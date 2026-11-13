package io.quarkus.cve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.cve.client.GitHubAdvisoryClient;
import io.quarkus.cve.client.OsvApiClient;
import io.quarkus.cve.client.QuarkusRegistryClient;
import io.quarkus.cve.model.OsvBatchQueryRequest;
import io.quarkus.cve.model.OsvBatchQueryResponse;
import io.quarkus.cve.model.OsvQueryRequest;
import io.quarkus.cve.model.OsvQueryResponse;
import io.quarkus.cve.model.OsvVulnerability;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Cache;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.Retry;
import io.quarkus.cve.util.Stats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Central service that:
 * 1. Discovers the Quarkus core + extension artifacts (io.quarkus and io.quarkus.* groups) from the
 *    Quarkus platform BOM on Maven Central ({@link QuarkusBomService}), cross-referenced with the
 *    Quarkus extension registry. Transitive dependencies are opt-in.
 * 2. Queries OSV in batches (Maven ecosystem) and deduplicates by GHSA/CVE ID. The batch response
 *    carries each vulnerability's {@code modified} timestamp, which is compared with the cached
 *    copy so that only new or changed records are downloaded.
 * 3. Queries the GitHub Advisory API for the same packages - catches CVEs that have been published
 *    to GitHub but not yet ingested by OSV. Uses a freshness TTL, then ETag revalidation (304),
 *    and stops politely when rate limited.
 * 4. Returns a merged, deduplicated list of {@link UnifiedVuln}.
 *
 * Every network call goes through {@link Retry}; on failure a stale cache entry is preferred
 * over no data.
 */
@ApplicationScoped
public class QuarkusArtifactService {

    /** OSV batch API maximum per request. */
    private static final int OSV_BATCH_SIZE = 100;

    /** GitHub Advisory API page size (max 100). */
    private static final int GH_PAGE_SIZE = 100;

    /** Concurrent OSV detail downloads. Deliberately small. GitHub is always sequential. */
    private static final int OSV_CONCURRENCY = 4;

    /** Registry list changes only when Quarkus ships new extensions. */
    private static final Duration REGISTRY_TTL = Duration.ofHours(6);

    /** Within this window a GitHub result is reused without any request; afterwards it is revalidated by ETag. */
    private static final Duration GITHUB_TTL = Duration.ofHours(1);

    /** For {@code get <id>} there is no modified marker to compare, so fall back to a short TTL. */
    private static final Duration OSV_SINGLE_TTL = Duration.ofHours(1);

    /** "osv-v2": records cached before OsvVulnerability kept database_specific lack it, so start fresh. */
    private static final String NS_OSV = "osv-v2";
    private static final String NS_GITHUB = "github";
    private static final String NS_REGISTRY = "registry";

    private static final TypeReference<OsvVulnerability> OSV_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRINGS_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<GitHubAdvisoryClient.GitHubAdvisory>> GH_TYPE = new TypeReference<>() {};
    private static final GenericType<List<GitHubAdvisoryClient.GitHubAdvisory>> GH_ENTITY = new GenericType<>() {};

    /**
     * Transitive dependency groupId:artifactId coordinates that Quarkus ships and
     * that OSV/GitHub tracks CVEs for independently of the quarkus-* artifacts.
     * These are pulled in by Quarkus but have their own CVE records.
     * Only scanned when requested (see {@link #discoverArtifacts(boolean)}).
     */
    private static final List<String> TRANSITIVE_PACKAGES = List.of(
        "io.netty:netty-codec-http",
        "io.netty:netty-codec-http2",
        "io.netty:netty-codec-haproxy",
        "io.netty:netty-codec-rtsp",
        "io.netty:netty-handler",
        "io.netty:netty-transport",
        "io.netty:netty-common",
        "com.github.luben:zstd-jni",
        "org.jboss.resteasy:resteasy-core",
        "org.hibernate.orm:hibernate-core",
        "io.smallrye:smallrye-jwt",
        "io.smallrye:smallrye-fault-tolerance",
        "io.smallrye.reactive:smallrye-mutiny-vertx-core",
        "org.freemarker:freemarker",
        "io.vertx:vertx-core",
        "io.vertx:vertx-web",
        "org.wildfly.security:wildfly-elytron-password-impl",
        "org.wildfly.security:wildfly-elytron-credential"
    );

    /** Cached Quarkus core + extension artifact list (populated lazily on first call). */
    private volatile List<String> cachedQuarkusArtifacts;

    @Inject
    QuarkusBomService bomService;

    @RestClient
    @Inject
    OsvApiClient osvApiClient;

    @RestClient
    @Inject
    QuarkusRegistryClient registryClient;

    @RestClient
    @Inject
    GitHubAdvisoryClient githubAdvisoryClient;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Returns the Quarkus core + extension artifact coordinates (groups {@code io.quarkus} and
     * {@code io.quarkus.*}), and, when {@code includeTransitive} is set, the known transitive
     * dependency packages as well.
     *
     * The list is the union of every sampled platform BOM release and the extension registry, so it
     * also covers artifacts that existed in older Quarkus releases. The Quarkus part is cached for
     * the lifetime of the process (and on disk, see {@link QuarkusBomService} and {@link #REGISTRY_TTL}).
     */
    public List<String> discoverArtifacts(boolean includeTransitive) {
        List<String> quarkus = discoverQuarkusArtifacts();
        if (!includeTransitive) return quarkus;

        List<String> all = new ArrayList<>(quarkus);
        for (String t : TRANSITIVE_PACKAGES) {
            if (!all.contains(t)) all.add(t);
        }
        Log.log(1, "Discovered %d Quarkus artifacts + %d transitive = %d total",
                quarkus.size(), all.size() - quarkus.size(), all.size());
        return all;
    }

    /**
     * Queries OSV + GitHub Advisory API for all given Maven packages.
     * Returns a deduplicated, merged list of {@link UnifiedVuln}.
     */
    public List<UnifiedVuln> queryAll(List<String> packageNames) {
        return queryAll(packageNames, packageNames);
    }

    /**
     * Like {@link #queryAll(List)}, but GitHub is only asked about {@code githubPackages}. GitHub costs
     * one request per package and is rate limited, so wide scans (e.g. every dependency a BOM manages)
     * can use OSV for everything and GitHub just for the packages where it adds the most.
     */
    public List<UnifiedVuln> queryAll(List<String> packageNames, List<String> githubPackages) {
        Map<String, UnifiedVuln> merged = new LinkedHashMap<>();

        // Step 1: Collect vulnerability IDs (+ modified timestamps) from OSV batch queries
        Log.log(1, "Step 1/3: querying OSV for %d packages", packageNames.size());
        Map<String, String> osvIds = queryOsvIds(packageNames);
        Log.log(1, "OSV: %d unique vulnerability IDs found", osvIds.size());

        // Step 2: Load full OSV details - from cache when unchanged, otherwise downloaded
        Log.log(1, "Step 2/3: loading %d OSV vulnerability details", osvIds.size());
        for (OsvVulnerability v : loadOsvDetails(osvIds).values()) {
            UnifiedVuln u = UnifiedVuln.fromOsv(v);
            merged.put(canonicalKey(u), u);
        }

        // Step 3: Query GitHub Advisory API - catches CVEs not yet in OSV
        Log.log(1, "Step 3/3: querying GitHub Advisory for %d packages (token %s)",
                githubPackages.size(), githubTokenPresent() ? "present" : "absent: unauthenticated, 60 req/hour");
        GhRun gh = new GhRun();
        int ghNew = 0;
        int total = githubPackages.size();
        for (int i = 0; i < total; i++) {
            String pkg = githubPackages.get(i);
            progress(i, total, 25, "GitHub Advisory", pkg);
            List<GitHubAdvisoryClient.GitHubAdvisory> advisories = fetchGitHubAdvisories(pkg, gh);
            int pkgNew = 0;
            for (GitHubAdvisoryClient.GitHubAdvisory a : advisories) {
                if (a.withdrawnAt != null) continue; // skip withdrawn
                UnifiedVuln u = UnifiedVuln.fromGitHub(a);
                String key = canonicalKey(u);
                UnifiedVuln existing = merged.get(key);
                if (existing == null) {
                    merged.put(key, u);
                    ghNew++;
                    pkgNew++;
                    Log.log(2, "GitHub-only advisory %s (%s)", u.id, pkg);
                } else {
                    existing.fillCvssFrom(u);
                }
            }
            Log.log(3, "GitHub: %s -> %d advisories (%d new, %d already in OSV)",
                    pkg, advisories.size(), pkgNew, advisories.size() - pkgNew);
        }
        Log.log(1, "GitHub Advisory: %d additional CVEs not in OSV "
                        + "(%d packages: %d cached, %d unchanged (304), %d downloaded, %d stale/unavailable)",
                ghNew, total, gh.fresh, gh.notModified, gh.downloaded, gh.stale + gh.unavailable);
        if (gh.halted) {
            Log.warn("GitHub rate limit reached - %d package(s) were not checked against GitHub%s. "
                            + "Set GITHUB_TOKEN for a 5,000 req/hour limit.",
                    gh.unavailable, gh.stale > 0 ? " (" + gh.stale + " used older cached data)" : "");
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * Loads full OSV details for IDs found by a package query (used by ListCommand --package).
     * The map is id -> modified timestamp as reported by the query response.
     */
    public List<UnifiedVuln> fetchOsvDetails(Map<String, String> idToModified) {
        Log.log(1, "Loading details for %d vulnerabilities", idToModified.size());
        List<UnifiedVuln> results = new ArrayList<>();
        for (OsvVulnerability v : loadOsvDetails(idToModified).values()) {
            results.add(UnifiedVuln.fromOsv(v));
        }
        return results;
    }

    /** Fills {@link UnifiedVuln#affectedReleases} from the Quarkus platform BOMs. */
    public void annotateAffectedReleases(List<UnifiedVuln> vulns) {
        bomService.annotateAffectedReleases(vulns);
    }

    /**
     * Fetches one OSV record (used by GetCommand). There is no modified marker to compare here,
     * so a cached copy is reused for {@link #OSV_SINGLE_TTL}. Returns null if the ID is unknown;
     * throws if the network fails and no cached copy exists.
     */
    public OsvVulnerability getOsvVulnerability(String id) {
        Optional<Cache.Entry<OsvVulnerability>> cached = Cache.read(NS_OSV, id, OSV_TYPE);
        if (cached.isPresent() && cached.get().fresh(OSV_SINGLE_TTL)) {
            Stats.cacheHit(Stats.OSV);
            Log.log(2, "OSV %s: cache hit (age %s)", id, cached.get().ageText());
            return cached.get().data();
        }
        try {
            OsvVulnerability v = Retry.call("OSV " + id, () -> osvApiClient.getVulnerability(id));
            if (v != null) Cache.write(NS_OSV, id, v, null);
            return v;
        } catch (RuntimeException ex) {
            Optional<Cache.Entry<OsvVulnerability>> stale = Cache.readFallback(NS_OSV, id, OSV_TYPE);
            if (stale.isPresent()) {
                Log.warn("could not reach OSV for %s (%s); showing cached copy from %s ago",
                        id, ex.getMessage(), stale.get().ageText());
                return stale.get().data();
            }
            throw ex;
        }
    }

    /** Level 2: every {@code every}th item (and the last); level 3: every item. */
    private static void progress(int index, int total, int every, String what, String item) {
        int n = index + 1;
        if (Log.enabled(3)) {
            Log.log(3, "%s %d/%d: %s", what, n, total, item);
        } else if (n % every == 0 || n == total) {
            Log.log(2, "%s %d/%d", what, n, total);
        }
    }

    private boolean githubTokenPresent() {
        return org.eclipse.microprofile.config.ConfigProvider.getConfig()
                .getOptionalValue("github.token", String.class)
                .filter(t -> !t.isBlank()).isPresent();
    }

    // -----------------------------------------------------------------------
    // OSV querying
    // -----------------------------------------------------------------------

    /** Returns vulnerability id -> modified timestamp for every package given. */
    private Map<String, String> queryOsvIds(List<String> packageNames) {
        Map<String, String> seen = new LinkedHashMap<>();
        int total   = packageNames.size();
        int batches = (int) Math.ceil((double) total / OSV_BATCH_SIZE);

        for (int b = 0; b < batches; b++) {
            int from  = b * OSV_BATCH_SIZE;
            int to    = Math.min(from + OSV_BATCH_SIZE, total);
            List<String> chunk = packageNames.subList(from, to);

            Log.log(2, "Querying OSV batch %d/%d (%d packages)", b + 1, batches, chunk.size());

            List<OsvQueryRequest> reqs = chunk.stream()
                    .map(pkg -> OsvQueryRequest.forPackage("Maven", pkg))
                    .toList();

            try {
                OsvBatchQueryResponse resp = Retry.call("OSV batch " + (b + 1),
                        () -> osvApiClient.queryBatch(new OsvBatchQueryRequest(reqs)));
                if (resp.results != null) {
                    for (OsvQueryResponse r : resp.results) {
                        collect(r, seen);
                    }
                }
            } catch (Exception ex) {
                // Fallback: individual queries
                Log.error(1, "OSV batch " + (b + 1) + " failed, falling back to individual queries", ex);
                int n = 0;
                for (OsvQueryRequest req : reqs) {
                    Log.log(3, "OSV single query %d/%d", ++n, reqs.size());
                    try {
                        collect(Retry.call("OSV query", () -> osvApiClient.query(req)), seen);
                    } catch (Exception e2) {
                        Log.error(2, "OSV single query failed", e2);
                    }
                }
            }
        }
        return seen;
    }

    private static void collect(OsvQueryResponse r, Map<String, String> into) {
        if (r == null || r.vulns == null) return;
        for (OsvVulnerability v : r.vulns) {
            if (v.id != null) into.put(v.id, v.modified);
        }
    }

    /** Counters shared by the OSV download workers. */
    private static final class OsvStats {
        final AtomicInteger hit = new AtomicInteger();
        final AtomicInteger downloaded = new AtomicInteger();
        final AtomicInteger stale = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
        final AtomicInteger done = new AtomicInteger();
    }

    /**
     * Returns full records for the given ids. A cached record is reused when its {@code modified}
     * equals the one just reported by OSV; everything else is downloaded (bounded concurrency).
     */
    private Map<String, OsvVulnerability> loadOsvDetails(Map<String, String> idToModified) {
        Map<String, OsvVulnerability> out = new LinkedHashMap<>();
        int total = idToModified.size();
        if (total == 0) return out;

        OsvStats stats = new OsvStats();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(OSV_CONCURRENCY, total));
        try {
            Map<String, Future<OsvVulnerability>> futures = new LinkedHashMap<>();
            idToModified.forEach((id, modified) ->
                    futures.put(id, pool.submit(() -> loadOsv(id, modified, stats, total))));
            for (Map.Entry<String, Future<OsvVulnerability>> e : futures.entrySet()) {
                try {
                    OsvVulnerability v = e.getValue().get();
                    if (v != null) out.put(e.getKey(), v);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException ee) {
                    stats.failed.incrementAndGet();
                    Log.error(2, "OSV detail task failed for " + e.getKey(), ee.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }
        Log.log(1, "OSV details: %d from cache, %d downloaded, %d stale, %d failed",
                stats.hit.get(), stats.downloaded.get(), stats.stale.get(), stats.failed.get());
        if (stats.failed.get() > 0) {
            Log.warn("%d OSV record(s) could not be loaded and are missing from the results (use -vv for details)",
                    stats.failed.get());
        }
        return out;
    }

    /**
     * OSV reports the same instant at different precisions depending on the endpoint
     * (querybatch: microseconds; vulns/{id} and query: nanoseconds), so a string compare would
     * wrongly treat unchanged records as modified. Compare as instants at microsecond precision.
     */
    static boolean sameModified(String a, String b) {
        if (a == null || b == null) return false;
        try {
            return Instant.parse(a).truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                    .equals(Instant.parse(b).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        } catch (java.time.format.DateTimeParseException ex) {
            return a.equals(b);
        }
    }

    private OsvVulnerability loadOsv(String id, String modified, OsvStats stats, int total) {
        try {
            Optional<Cache.Entry<OsvVulnerability>> cached = Cache.read(NS_OSV, id, OSV_TYPE);
            if (cached.isPresent() && sameModified(modified, cached.get().data().modified)) {
                stats.hit.incrementAndGet();
                Stats.cacheHit(Stats.OSV);
                Log.log(3, "OSV %s: cache hit (modified %s)", id, modified);
                return cached.get().data();
            }
            try {
                OsvVulnerability v = Retry.call("OSV " + id, () -> osvApiClient.getVulnerability(id));
                if (v == null) {
                    stats.failed.incrementAndGet();
                    return null;
                }
                Cache.write(NS_OSV, id, v, null);
                stats.downloaded.incrementAndGet();
                Log.log(3, "OSV %s: downloaded%s", id, cached.isPresent() ? " (changed upstream)" : "");
                return v;
            } catch (RuntimeException ex) {
                Log.error(2, "OSV detail fetch failed for " + id, ex);
                Optional<Cache.Entry<OsvVulnerability>> stale = Cache.readFallback(NS_OSV, id, OSV_TYPE);
                if (stale.isPresent()) {
                    stats.stale.incrementAndGet();
                    return stale.get().data();
                }
                stats.failed.incrementAndGet();
                return null;
            }
        } finally {
            int n = stats.done.incrementAndGet();
            if (!Log.enabled(3) && (n % 25 == 0 || n == total)) {
                Log.log(2, "OSV detail %d/%d", n, total);
            }
        }
    }

    // -----------------------------------------------------------------------
    // GitHub Advisory querying
    // -----------------------------------------------------------------------

    /** Per-scan GitHub state: counters, and whether we have been told to stop. */
    private static final class GhRun {
        int fresh;        // served from cache within TTL, no request
        int notModified;  // revalidated with ETag, got 304
        int downloaded;   // full download
        int stale;        // network failed/skipped, used older cache
        int unavailable;  // network failed/skipped, nothing cached
        boolean halted;   // rate limit hit: stop calling GitHub for the rest of this run
    }

    private List<GitHubAdvisoryClient.GitHubAdvisory> fetchGitHubAdvisories(String mavenCoord, GhRun run) {
        Optional<Cache.Entry<List<GitHubAdvisoryClient.GitHubAdvisory>>> cached =
                Cache.read(NS_GITHUB, mavenCoord, GH_TYPE);

        if (cached.isPresent() && cached.get().fresh(GITHUB_TTL)) {
            run.fresh++;
            Stats.cacheHit(Stats.GITHUB);
            Log.log(4, "GitHub %s: cache fresh (age %s), no request", mavenCoord, cached.get().ageText());
            return cached.get().data();
        }
        if (run.halted) {
            return githubFallback(mavenCoord, run);
        }

        // GitHub API: "sort=updated&direction=desc" makes the page-1 ETag change whenever any advisory
        // for this package changes, so a 304 on page 1 means the whole result is unchanged.
        String etag = cached.isPresent() ? cached.get().etag() : null;
        try {
            List<GitHubAdvisoryClient.GitHubAdvisory> all = new ArrayList<>();
            String newEtag = null;
            for (int page = 1; ; page++) {
                final int p = page;
                final String inm = page == 1 ? etag : null;
                Response r = Retry.call("GitHub " + mavenCoord + " page " + p, () -> checked(
                        githubAdvisoryClient.listAdvisories("maven", mavenCoord, GH_PAGE_SIZE, p,
                                "updated", "desc", inm)));
                try {
                    noteRateLimit(r, run);
                    if (r.getStatus() == 304) {
                        List<GitHubAdvisoryClient.GitHubAdvisory> data =
                                cached.isPresent() ? cached.get().data() : List.of();
                        Cache.write(NS_GITHUB, mavenCoord, data, etag); // restart the TTL clock
                        run.notModified++;
                        Log.log(4, "GitHub %s: 304 not modified", mavenCoord);
                        return data;
                    }
                    List<GitHubAdvisoryClient.GitHubAdvisory> items = r.readEntity(GH_ENTITY);
                    if (page == 1) newEtag = r.getHeaderString("ETag");
                    if (items == null || items.isEmpty()) break;
                    all.addAll(items);
                    if (items.size() < GH_PAGE_SIZE) break;
                } finally {
                    r.close();
                }
            }
            Cache.write(NS_GITHUB, mavenCoord, all, newEtag);
            run.downloaded++;
            return all;
        } catch (Retry.RateLimitedException ex) {
            run.halted = true;
            Log.log(1, "GitHub rate limited: %s", ex.getMessage());
            return githubFallback(mavenCoord, run);
        } catch (RuntimeException ex) {
            // GitHub Advisory API is optional - don't fail the scan if it is unavailable
            Log.error(1, "GitHub Advisory query failed for " + mavenCoord, ex);
            return githubFallback(mavenCoord, run);
        }
    }

    private List<GitHubAdvisoryClient.GitHubAdvisory> githubFallback(String mavenCoord, GhRun run) {
        Optional<Cache.Entry<List<GitHubAdvisoryClient.GitHubAdvisory>>> stale =
                Cache.readFallback(NS_GITHUB, mavenCoord, GH_TYPE);
        if (stale.isPresent()) {
            run.stale++;
            Log.log(3, "GitHub %s: using stale cache (age %s)", mavenCoord, stale.get().ageText());
            return stale.get().data();
        }
        run.unavailable++;
        return List.of();
    }

    /** Some REST client configurations return error responses instead of throwing; make them throw. */
    private static Response checked(Response r) {
        if (r.getStatus() >= 400) {
            throw new WebApplicationException(r);
        }
        return r;
    }

    private static void noteRateLimit(Response r, GhRun run) {
        String remaining = r.getHeaderString("X-RateLimit-Remaining");
        if (remaining == null) return;
        Log.log(4, "GitHub rate limit: %s/%s remaining", remaining, r.getHeaderString("X-RateLimit-Limit"));
        if ("0".equals(remaining.trim()) && !run.halted) {
            run.halted = true;
            String reset = r.getHeaderString("X-RateLimit-Reset");
            String when = "";
            try {
                if (reset != null) when = " (resets at " + Instant.ofEpochSecond(Long.parseLong(reset.trim())) + ")";
            } catch (NumberFormatException ignored) { /* cosmetic only */ }
            Log.log(1, "GitHub rate limit exhausted%s; remaining packages will use cached data only", when);
        }
    }

    // -----------------------------------------------------------------------
    // Artifact discovery
    // -----------------------------------------------------------------------

    /** BOM artifacts (all sampled releases) plus registry artifacts; built-in list only if both are empty. */
    private List<String> discoverQuarkusArtifacts() {
        List<String> cachedList = cachedQuarkusArtifacts;
        if (cachedList != null) return cachedList;

        Log.log(1, "Discovering Quarkus artifacts from the platform BOM...");
        List<String> bom = bomService.quarkusArtifacts();
        List<String> registry = registryArtifacts();

        TreeSet<String> union = new TreeSet<>(bom);
        union.addAll(registry);
        if (union.isEmpty()) {
            Log.warn("using built-in artifact list (%d artifacts only)", fallbackArtifacts().size());
            union.addAll(fallbackArtifacts());
        } else {
            Set<String> bomSet = new HashSet<>(bom);
            long registryOnly = registry.stream().filter(a -> !bomSet.contains(a)).count();
            Log.log(1, "Quarkus artifacts: %d from BOM, %d from registry (%d only in registry) = %d total",
                    bom.size(), registry.size(), registryOnly, union.size());
        }
        cachedQuarkusArtifacts = new ArrayList<>(union);
        return cachedQuarkusArtifacts;
    }

    /** Quarkus core / extension artifacts known to the extension registry. Empty if unavailable. */
    private List<String> registryArtifacts() {
        Optional<Cache.Entry<List<String>>> cached = Cache.read(NS_REGISTRY, "quarkus-extensions", STRINGS_TYPE);
        if (cached.isPresent() && cached.get().fresh(REGISTRY_TTL)) {
            Stats.cacheHit(Stats.QUARKUS_REGISTRY);
            Log.log(2, "Registry: using cached list (%d artifacts, age %s)",
                    cached.get().data().size(), cached.get().ageText());
            return cached.get().data();
        }
        try {
            QuarkusRegistryClient.ExtensionCatalog catalog =
                    Retry.call("Quarkus registry", () -> registryClient.getAllExtensions());
            if (catalog != null && catalog.extensions != null) {
                List<String> found = catalog.extensions.stream()
                        .filter(e -> e.artifact != null)
                        .map(e -> e.artifact.split(":"))
                        .filter(p -> p.length >= 2 && QuarkusBomService.isQuarkusGroup(p[0]))
                        .map(p -> p[0] + ":" + p[1])
                        .distinct()
                        .sorted()
                        .toList();
                if (!found.isEmpty()) {
                    Cache.write(NS_REGISTRY, "quarkus-extensions", found, null);
                    return found;
                }
            }
        } catch (RuntimeException ex) {
            Log.warn("could not reach Quarkus registry (%s)", ex.getMessage());
        }
        Optional<Cache.Entry<List<String>>> stale =
                Cache.readFallback(NS_REGISTRY, "quarkus-extensions", STRINGS_TYPE);
        if (stale.isPresent()) {
            Log.warn("using cached Quarkus registry list from %s ago", stale.get().ageText());
            return stale.get().data();
        }
        return List.of();
    }

    // -----------------------------------------------------------------------
    // Deduplication key: prefer GHSA ID, fall back to CVE ID, then raw ID
    // -----------------------------------------------------------------------

    private static String canonicalKey(UnifiedVuln u) {
        if (u.id != null && u.id.startsWith("GHSA-")) return u.id;
        if (u.cveId != null) return u.cveId;
        return u.id != null ? u.id : "UNKNOWN";
    }

    // -----------------------------------------------------------------------
    // Fallback artifact list
    // -----------------------------------------------------------------------

    private static List<String> fallbackArtifacts() {
        return List.of(
            "io.quarkus:quarkus-cache",
            "io.quarkus:quarkus-core",
            "io.quarkus:quarkus-csrf-reactive",
            "io.quarkus:quarkus-hibernate-orm",
            "io.quarkus:quarkus-keycloak-authorization",
            "io.quarkus:quarkus-oidc",
            "io.quarkus:quarkus-rest",
            "io.quarkus:quarkus-resteasy",
            "io.quarkus:quarkus-resteasy-reactive-common",
            "io.quarkus:quarkus-security",
            "io.quarkus:quarkus-smallrye-graphql-client",
            "io.quarkus:quarkus-smallrye-jwt",
            "io.quarkus:quarkus-undertow",
            "io.quarkus:quarkus-vertx",
            "io.quarkus:quarkus-vertx-http",
            "io.quarkus:quarkus-websockets-next"
        );
    }
}
