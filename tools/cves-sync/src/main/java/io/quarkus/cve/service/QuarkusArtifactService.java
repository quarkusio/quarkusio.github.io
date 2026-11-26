package io.quarkus.cve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.cve.client.GitHubAdvisoryClient;
import io.quarkus.cve.client.OsvApiClient;
import io.quarkus.cve.client.QuarkusRegistryClient;
import io.quarkus.cve.client.QuarkusRepoAdvisoryClient;
import io.quarkus.cve.model.OsvBatchQueryRequest;
import io.quarkus.cve.model.OsvBatchQueryResponse;
import io.quarkus.cve.model.OsvQueryRequest;
import io.quarkus.cve.model.OsvQueryResponse;
import io.quarkus.cve.model.OsvVulnerability;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Cache;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.QuarkusRepoAdvisory;
import io.quarkus.cve.util.Retry;
import io.quarkus.cve.util.Stats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
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
    /** Defensive upper bound on pagination loops so a repeating server cursor can never loop forever. */
    private static final int MAX_PAGES = 100;
    /**
     * Packages per GitHub request ({@code affects} takes a comma separated list). One request per package
     * is ~1,000 requests for a scan, which a cold run cannot afford against the 1,000/hour that the
     * workflow's own token gets; 50 per request needs about 20.
     */
    private static final int GH_PACKAGES_PER_REQUEST = 50;

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

    @RestClient
    @Inject
    QuarkusRepoAdvisoryClient quarkusRepoAdvisoryClient;

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
            String key = canonicalKey(u);
            UnifiedVuln existing = merged.get(key);
            if (existing == null) {
                merged.put(key, u);
            } else {
                if (u.withdrawn != null && existing.withdrawn == null) existing.withdrawn = u.withdrawn;
                existing.fillCvssFrom(u);
                if (QuarkusBomService.mavenPackages(existing).isEmpty()
                        && !QuarkusBomService.mavenPackages(u).isEmpty()) {
                    existing.affected = u.affected;
                }
            }
        }

        // Step 3: Query GitHub Advisory API - catches CVEs not yet in OSV
        Log.log(1, "Step 3/3: querying GitHub Advisory for %d packages (token %s)",
                githubPackages.size(), githubTokenPresent() ? "present" : "absent: unauthenticated, 60 req/hour");
        GhRun gh = new GhRun();
        int ghNew = 0;
        // Sorted, so a batch is the same batch on every run and its cache entry (and ETag) can be reused
        List<String> sorted = githubPackages.stream().sorted().toList();
        int batches = (sorted.size() + GH_PACKAGES_PER_REQUEST - 1) / GH_PACKAGES_PER_REQUEST;
        for (int b = 0; b < batches; b++) {
            List<String> batch = sorted.subList(b * GH_PACKAGES_PER_REQUEST,
                    Math.min(sorted.size(), (b + 1) * GH_PACKAGES_PER_REQUEST));
            progress(b, batches, 5, "GitHub Advisory", batch.get(0) + " ...");
            List<GitHubAdvisoryClient.GitHubAdvisory> advisories = fetchGitHubAdvisories(batch, gh);
            int batchNew = 0;
            for (GitHubAdvisoryClient.GitHubAdvisory a : advisories) {
                UnifiedVuln u = UnifiedVuln.fromGitHub(a);
                String key = canonicalKey(u);
                if (a.withdrawnAt != null) {
                    merged.remove(key);
                    continue;
                }
                UnifiedVuln existing = merged.get(key);
                if (existing == null) {
                    merged.put(key, u);
                    ghNew++;
                    batchNew++;
                    Log.log(2, "GitHub-only advisory %s", u.id);
                } else {
                    existing.fillCvssFrom(u);
                    if (QuarkusBomService.mavenPackages(existing).isEmpty()
                            && !QuarkusBomService.mavenPackages(u).isEmpty()) {
                        existing.affected = u.affected;
                    }
                }
            }
            Log.log(3, "GitHub: batch %d/%d (%d packages) -> %d advisories (%d new, %d already in OSV)",
                    b + 1, batches, batch.size(), advisories.size(), batchNew, advisories.size() - batchNew);
        }
        Log.log(1, "GitHub Advisory: %d additional CVEs not in OSV "
                        + "(%d packages in %d requests: %d cached, %d unchanged (304), %d downloaded, %d stale/unavailable)",
                ghNew, sorted.size(), batches, gh.fresh, gh.notModified, gh.downloaded, gh.stale + gh.unavailable);
        if (gh.halted) {
            Log.warn("GitHub rate limit reached - %d request(s) of %d packages each were not made against GitHub%s. "
                            + "Set GITHUB_TOKEN for a 5,000 req/hour limit.",
                    gh.stale + gh.unavailable, GH_PACKAGES_PER_REQUEST,
                    gh.stale > 0 ? " (" + gh.stale + " used older cached data)" : "");
        } else if (gh.stale + gh.unavailable > 0) {
            Log.warn("GitHub Advisory query failed for %d request(s) of %d packages each%s",
                    gh.stale + gh.unavailable, GH_PACKAGES_PER_REQUEST,
                    gh.stale > 0 ? " (" + gh.stale + " used older cached data)" : "");
        }

        // Step 4 (backup): quarkusio/quarkus repository advisories for Quarkus-published CVEs not yet in OSV or GitHub Global
        if (!githubPackages.isEmpty() && quarkusRepoAdvisoryClient != null) {
            int repoAdded = mergeQuarkusRepoAdvisories(merged, gh);
            if (repoAdded > 0) {
                Log.log(1, "Quarkus repo advisories: %d backup advisories added (not yet in OSV/GitHub Global)", repoAdded);
            }
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

    /** Returns vulnerability id -> modified timestamp for every package given, following any OSV page tokens. */
    private Map<String, String> queryOsvIds(List<String> packageNames) {
        Map<String, String> seen = new LinkedHashMap<>();
        int total   = packageNames.size();
        int batches = (int) Math.ceil((double) total / OSV_BATCH_SIZE);
        int failed  = 0;

        for (int b = 0; b < batches; b++) {
            int from  = b * OSV_BATCH_SIZE;
            int to    = Math.min(from + OSV_BATCH_SIZE, total);
            List<String> chunk = packageNames.subList(from, to);

            Log.log(2, "Querying OSV batch %d/%d (%d packages)", b + 1, batches, chunk.size());

            List<OsvQueryRequest> reqs = chunk.stream()
                    .map(pkg -> OsvQueryRequest.forPackage("Maven", pkg))
                    .toList();

            int page = 1;
            while (!reqs.isEmpty()) {
                final List<OsvQueryRequest> current = reqs;
                final int p = page;
                try {
                    OsvBatchQueryResponse resp = Retry.call("OSV batch " + (b + 1) + (p > 1 ? " page " + p : ""),
                            () -> osvApiClient.queryBatch(new OsvBatchQueryRequest(current)));
                    List<OsvQueryRequest> nextReqs = new ArrayList<>();
                    if (resp != null && resp.results != null) {
                        for (int i = 0; i < resp.results.size(); i++) {
                            OsvQueryResponse r = resp.results.get(i);
                            collect(r, seen);
                            if (r != null && r.hasMore() && i < current.size()) {
                                OsvQueryRequest prev = current.get(i);
                                if (r.nextPageToken.equals(prev.pageToken) || p >= MAX_PAGES) {
                                    Log.warn("OSV batch pagination stopped for %s at page %d (repeating token or page limit)",
                                            prev.pkg.name, p);
                                } else {
                                    OsvQueryRequest next = OsvQueryRequest.forPackage(prev.pkg.ecosystem, prev.pkg.name);
                                    next.pageToken = r.nextPageToken;
                                    nextReqs.add(next);
                                }
                            }
                        }
                    }
                    reqs = nextReqs;
                    page++;
                } catch (Exception ex) {
                    // Fallback: individual paginated queries for the remaining requests
                    Log.error(1, "OSV batch " + (b + 1) + " failed, falling back to individual queries", ex);
                    int n = 0;
                    for (OsvQueryRequest req : current) {
                        Log.log(3, "OSV single query %d/%d", ++n, current.size());
                        try {
                            OsvQueryRequest single = req;
                            Set<String> seenTokens = new HashSet<>();
                            int singlePage = 1;
                            while (single != null) {
                                final OsvQueryRequest q = single;
                                OsvQueryResponse r = Retry.call("OSV query", () -> osvApiClient.query(q));
                                collect(r, seen);
                                if (r != null && r.hasMore()) {
                                    if (!seenTokens.add(r.nextPageToken) || singlePage >= MAX_PAGES) {
                                        Log.warn("OSV single-query pagination stopped for %s at page %d (repeating token or page limit)",
                                                req.pkg.name, singlePage);
                                        single = null;
                                    } else {
                                        single = OsvQueryRequest.forPackage(req.pkg.ecosystem, req.pkg.name);
                                        single.pageToken = r.nextPageToken;
                                        singlePage++;
                                    }
                                } else {
                                    single = null;
                                }
                            }
                        } catch (Exception e2) {
                            failed++;
                            Log.error(2, "OSV single query failed", e2);
                        }
                    }
                    break;
                }
            }
        }
        if (failed > 0) {
            Log.warn("could not query OSV for %d of %d package(s); advisories may be missing (use -vv for details)",
                    failed, total);
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
        if (stats.stale.get() > 0) {
            Log.warn("%d OSV record(s) changed upstream could not be reloaded; using older cached data (use -vv for details)",
                    stats.stale.get());
        }
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

    /** The advisories that affect any of {@code packages} (Maven {@code group:artifact}), in one logical request. */
    private List<GitHubAdvisoryClient.GitHubAdvisory> fetchGitHubAdvisories(List<String> packages, GhRun run) {
        String mavenCoord = String.join(",", packages);
        // The key names the exact set of packages, so a cached answer is never used for a different set
        String cacheKey = "batch-" + sha1(mavenCoord).substring(0, 16);
        Optional<Cache.Entry<List<GitHubAdvisoryClient.GitHubAdvisory>>> cached =
                Cache.read(NS_GITHUB, cacheKey, GH_TYPE);

        if (cached.isPresent() && cached.get().fresh(GITHUB_TTL)) {
            run.fresh++;
            Stats.cacheHit(Stats.GITHUB);
            Log.log(4, "GitHub %s: cache fresh (age %s), no request", cacheKey, cached.get().ageText());
            return cached.get().data();
        }
        if (run.halted) {
            return githubFallback(cacheKey, run);
        }

        // GitHub API: "sort=updated&direction=desc" makes the page-1 ETag change whenever any advisory
        // for this package changes, so a 304 on page 1 means the whole result is unchanged.
        String etag = cached.isPresent() ? cached.get().etag() : null;
        try {
            List<GitHubAdvisoryClient.GitHubAdvisory> all = new ArrayList<>();
            String newEtag = null;
            String after = null;
            Set<String> seenCursors = new HashSet<>();
            for (int page = 1; ; page++) {
                final int p = page;
                final String inm = page == 1 ? etag : null;
                final String cursor = after;
                Response r = Retry.call("GitHub " + cacheKey + " page " + p, () -> checked(
                        githubAdvisoryClient.listAdvisories("maven", mavenCoord, GH_PAGE_SIZE, cursor,
                                "updated", "desc", inm)));
                try {
                    noteRateLimit(r, run);
                    if (r.getStatus() == 304) {
                        List<GitHubAdvisoryClient.GitHubAdvisory> data =
                                cached.isPresent() ? cached.get().data() : List.of();
                        Cache.write(NS_GITHUB, cacheKey, data, etag); // restart the TTL clock
                        run.notModified++;
                        Log.log(4, "GitHub %s: 304 not modified", cacheKey);
                        return data;
                    }
                    List<GitHubAdvisoryClient.GitHubAdvisory> items = r.readEntity(GH_ENTITY);
                    if (page == 1) newEtag = r.getHeaderString("ETag");
                    if (items == null || items.isEmpty()) break;
                    all.addAll(items);
                    after = nextCursor(r.getHeaderString("Link"));
                    if (after == null || items.size() < GH_PAGE_SIZE) break;
                    if (!seenCursors.add(after) || page >= MAX_PAGES) {
                        Log.warn("GitHub Advisory pagination stopped for %s at page %d (repeating cursor or page limit)",
                                cacheKey, page);
                        break;
                    }
                } finally {
                    r.close();
                }
            }
            Cache.write(NS_GITHUB, cacheKey, all, newEtag);
            run.downloaded++;
            return all;
        } catch (Retry.RateLimitedException ex) {
            run.halted = true;
            Log.log(1, "GitHub rate limited: %s", ex.getMessage());
            return githubFallback(cacheKey, run);
        } catch (RuntimeException ex) {
            // Fall back to cached data if available; queryAll emits a warning so CI rejects incomplete/stale runs
            Log.error(1, "GitHub Advisory query failed for " + cacheKey, ex);
            return githubFallback(cacheKey, run);
        }
    }

    private int mergeQuarkusRepoAdvisories(Map<String, UnifiedVuln> merged, GhRun run) {
        Set<String> coveredCves = new HashSet<>();
        for (UnifiedVuln existing : merged.values()) {
            if (existing.withdrawn != null || QuarkusBomService.mavenPackages(existing).isEmpty()) continue;
            if (existing.cveId != null) coveredCves.add(existing.cveId);
            if (existing.aliases != null) {
                existing.aliases.stream().filter(a -> a != null && a.startsWith("CVE-")).forEach(coveredCves::add);
            }
        }
        int added = 0;
        for (GitHubAdvisoryClient.GitHubAdvisory a : fetchQuarkusRepoAdvisories(run)) {
            UnifiedVuln u = QuarkusRepoAdvisory.toUnifiedVuln(a);
            if (u == null) continue;
            String key = canonicalKey(u);
            UnifiedVuln existing = merged.get(key);
            if (existing != null) {
                // OSV / GitHub Global always take precedence once they have reviewed Maven ranges
                if (existing.withdrawn == null && QuarkusBomService.mavenPackages(existing).isEmpty()
                        && !QuarkusBomService.mavenPackages(u).isEmpty()) {
                    existing.affected = u.affected;
                    existing.fillCvssFrom(u);
                }
                continue;
            }
            if (u.cveId != null && coveredCves.contains(u.cveId)) continue;
            merged.put(key, u);
            if (u.cveId != null) coveredCves.add(u.cveId);
            added++;
            Log.log(2, "Quarkus repo-only advisory %s (%s)", u.id, u.cveId);
        }
        return added;
    }

    private List<GitHubAdvisoryClient.GitHubAdvisory> fetchQuarkusRepoAdvisories(GhRun run) {
        String cacheKey = "repo-quarkusio-quarkus";
        Optional<Cache.Entry<List<GitHubAdvisoryClient.GitHubAdvisory>>> cached =
                Cache.read(NS_GITHUB, cacheKey, GH_TYPE);
        if (cached.isPresent() && cached.get().fresh(GITHUB_TTL)) {
            Stats.cacheHit(Stats.GITHUB);
            return cached.get().data();
        }
        if (run.halted) {
            return repoAdvisoryFallback(cacheKey);
        }
        String etag = cached.isPresent() ? cached.get().etag() : null;
        try {
            List<GitHubAdvisoryClient.GitHubAdvisory> all = new ArrayList<>();
            String newEtag = null;
            String after = null;
            Set<String> seenCursors = new HashSet<>();
            for (int page = 1; ; page++) {
                final int p = page;
                final String inm = page == 1 ? etag : null;
                final String cursor = after;
                Response r = Retry.call("Quarkus repo advisories page " + p, () -> checked(
                        quarkusRepoAdvisoryClient.listAdvisories("published", GH_PAGE_SIZE, cursor, inm)));
                try {
                    noteRateLimit(r, run);
                    if (r.getStatus() == 304) {
                        List<GitHubAdvisoryClient.GitHubAdvisory> data =
                                cached.isPresent() ? cached.get().data() : List.of();
                        Cache.write(NS_GITHUB, cacheKey, data, etag);
                        return data;
                    }
                    List<GitHubAdvisoryClient.GitHubAdvisory> items = r.readEntity(GH_ENTITY);
                    if (page == 1) newEtag = r.getHeaderString("ETag");
                    if (items == null || items.isEmpty()) break;
                    all.addAll(items);
                    after = nextCursor(r.getHeaderString("Link"));
                    if (after == null || items.size() < GH_PAGE_SIZE) break;
                    if (!seenCursors.add(after) || page >= MAX_PAGES) {
                        Log.warn("Quarkus repo advisories pagination stopped at page %d (repeating cursor or page limit)", page);
                        break;
                    }
                } finally {
                    r.close();
                }
            }
            Cache.write(NS_GITHUB, cacheKey, all, newEtag);
            return all;
        } catch (Retry.RateLimitedException ex) {
            run.halted = true;
            Log.warn("GitHub rate limit reached while reading quarkusio/quarkus repository advisories");
            return repoAdvisoryFallback(cacheKey);
        } catch (RuntimeException ex) {
            Log.warn("could not read quarkusio/quarkus repository advisories (%s)", ex.getMessage());
            return repoAdvisoryFallback(cacheKey);
        }
    }

    private List<GitHubAdvisoryClient.GitHubAdvisory> repoAdvisoryFallback(String cacheKey) {
        Optional<Cache.Entry<List<GitHubAdvisoryClient.GitHubAdvisory>>> stale =
                Cache.readFallback(NS_GITHUB, cacheKey, GH_TYPE);
        return stale.isPresent() ? stale.get().data() : List.of();
    }

    private static final java.util.regex.Pattern NEXT_LINK =
            java.util.regex.Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
    private static final java.util.regex.Pattern AFTER_PARAM =
            java.util.regex.Pattern.compile("[?&]after=([^&]+)");

    /** Extracts the {@code after} cursor from GitHub's {@code Link: <...>; rel="next"} header, or null. */
    static String nextCursor(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) return null;
        java.util.regex.Matcher link = NEXT_LINK.matcher(linkHeader);
        if (!link.find()) return null;
        java.util.regex.Matcher after = AFTER_PARAM.matcher(link.group(1));
        return after.find() ? java.net.URLDecoder.decode(after.group(1), StandardCharsets.UTF_8) : null;
    }

    private static String sha1(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-1 is required of every JRE
        }
    }

    private List<GitHubAdvisoryClient.GitHubAdvisory> githubFallback(String cacheKey, GhRun run) {
        Optional<Cache.Entry<List<GitHubAdvisoryClient.GitHubAdvisory>>> stale =
                Cache.readFallback(NS_GITHUB, cacheKey, GH_TYPE);
        if (stale.isPresent()) {
            run.stale++;
            Log.log(3, "GitHub %s: using stale cache (age %s)", cacheKey, stale.get().ageText());
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

    static String canonicalKey(UnifiedVuln u) {
        if (u.id != null && u.id.startsWith("GHSA-")) return u.id;
        if (u.aliases != null) {
            for (String a : u.aliases) {
                if (a != null && a.startsWith("GHSA-")) return a;
            }
        }
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
