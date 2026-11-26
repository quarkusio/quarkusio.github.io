package io.quarkus.cve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.cve.client.CveListZips;
import io.quarkus.cve.client.CveOrgClient;
import io.quarkus.cve.client.GitHubSearchClient;
import io.quarkus.cve.model.CveOrgRecord;
import io.quarkus.cve.model.ReleaseReport;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Cache;
import io.quarkus.cve.util.CveDelta;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.PendingIndex;
import io.quarkus.cve.util.Po4a;
import io.quarkus.cve.util.Retry;
import io.quarkus.cve.util.Stats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Finds published CVEs that concern an artifact the Quarkus BOM manages but that neither OSV nor GitHub
 * has version ranges for yet, which is the case for a new CVE until GitHub reviews it.
 *
 * cve.org is the root of CVE data, but only CNAs can list or search it. Its records are mirrored in the
 * {@code CVEProject/cvelistV5} repository, so GitHub code search over that repository finds the records
 * that mention Quarkus (this is the only use of the mirror), and each record is then read from cve.org.
 * Neither gives upstream version ranges: a record names the affected artifacts and lists a vendor's own
 * builds, so such a CVE is reported as pending and the ranges come once an advisory with version ranges
 * exists, derived from the managed artifact versions like every other one.
 *
 * Code search is limited to 10 requests a minute, and GitHub refuses it for a long time to a CI run, so with an
 * index file ({@link #useIndex}) it is only used to create that file. A run then reads what changed in the CVE
 * list since the index was made, from the delta zips of the same repository ({@link CveDelta}), which
 * needs no API. See {@link PendingIndex}.
 *
 * Lookups are cached: the candidate list for {@link #INDEX_TTL}, each record for {@link #RECORD_TTL}.
 */
@ApplicationScoped
public class CveOrgService {

    static final Duration INDEX_TTL = Duration.ofHours(6);
    static final Duration RECORD_TTL = Duration.ofHours(24);
    private static final String NS_INDEX = "cve-org-index";
    private static final String NS_RECORD = "cve-org";
    private static final String MIRROR = "CVEProject/cvelistV5";
    private static final int PAGE_SIZE = 100;
    /** GitHub code search allows 10 requests a minute; keep the requests this far apart to stay under it. */
    static long searchGapMs = 7_000;
    /**
     * When code search refuses a request GitHub says how long to wait, and the waits seen in CI run up to 736
     * seconds. The list of pending CVEs is not worth a run without it, so wait that long (the workflow has a
     * timeout to bound this) rather than give up and report a run with missing data.
     */
    static final long SEARCH_MAX_WAIT_MS = 15 * 60_000;
    private static long lastSearchAt;
    /** GitHub returns at most 1000 results for a search. */
    private static final int MAX_PAGES = 10;
    private static final TypeReference<List<String>> IDS_TYPE = new TypeReference<>() {};
    private static final TypeReference<CveOrgRecord> RECORD_TYPE = new TypeReference<>() {};

    @Inject
    @RestClient
    GitHubSearchClient search;

    @Inject
    @RestClient
    CveOrgClient cveOrg;

    /** Where the delta zips come from. */
    CveDelta.Source zips = new CveListZips();

    Clock clock = Clock.systemUTC();

    private Path indexFile;
    private PendingIndex index;
    private Set<String> changedInDelta = Set.of();
    private Set<String> rejectedKnownCves = Set.of();

    /**
     * Keeps the list of candidate CVEs in this file (see {@link PendingIndex}). The file is read here and
     * not written: {@link #indexToWrite()} is the file to write once the whole run has succeeded.
     */
    public void useIndex(Path file) {
        this.indexFile = file;
        this.index = null;
        this.changedInDelta = Set.of();
        this.rejectedKnownCves = Set.of();
    }

    /** CVEs that are already covered by an advisory yet reported {@code REJECTED} or {@code disputed} on cve.org. */
    public Set<String> rejectedKnownCves() {
        return rejectedKnownCves;
    }

    /** The index as this run left it, to be written when the run succeeded; empty if there is none to write. */
    public Optional<PendingIndex> indexToWrite() {
        return Optional.ofNullable(index);
    }

    /**
     * @param managed   every {@code group:artifact} any scanned release manages
     * @param known     the advisories that have version ranges; their CVEs are not pending
     * @param fromYear  oldest year of CVE ids to look at
     */
    public List<ReleaseReport.Pending> pending(Set<String> managed, Collection<UnifiedVuln> known, int fromYear) {
        List<CveOrgRecord> records = new ArrayList<>();
        int skipped = 0;
        int stale = 0;
        Set<String> alreadyKnown = knownCves(known);
        Set<String> rejected = new HashSet<>();
        List<String> candidates = candidates(fromYear);
        for (String id : candidates) {
            if (alreadyKnown.contains(id)) continue;
            RecordResult r = record(id, changedInDelta.contains(id));
            if (r.record().isPresent()) {
                records.add(r.record().get());
                if (r.stale()) stale++;
            } else if (!r.notFound()) {
                skipped++;
            }
        }
        // Check already-known CVEs: force-refresh any that appeared in today's delta (even if rejection
        // removed "quarkus" or the CVE is for a third-party dependency), and revalidate any previously
        // persisted rejected/disputed CVEs from PendingIndex (independent of cache warmth).
        Set<String> persistedRejected = index != null ? index.rejected() : Set.of();
        for (String id : alreadyKnown) {
            if (changedInDelta.contains(id)) {
                RecordResult r = record(id, true);
                if (r.record().isPresent()) {
                    if (isRejectedOrDisputed(r.record().get())) rejected.add(id);
                    if (r.stale()) stale++;
                } else if (!r.notFound()) {
                    skipped++;
                }
            } else if (persistedRejected.contains(id)) {
                RecordResult r = record(id, false);
                if (r.record().isPresent()) {
                    if (isRejectedOrDisputed(r.record().get())) rejected.add(id);
                    if (r.stale()) stale++;
                } else if (!r.notFound()) {
                    skipped++;
                }
            }
        }
        rejectedKnownCves = Set.copyOf(rejected);
        if (index != null) {
            index = index.withRejected(rejected);
        }
        if (stale > 0) {
            Log.warn("cve.org: %d candidate record(s) could not be refreshed; using older cached data", stale);
        }
        if (skipped > 0) {
            Log.warn("cve.org: %d candidate record(s) could not be read; pending CVEs may be missing", skipped);
        }
        List<ReleaseReport.Pending> pending = select(records, managed, alreadyKnown);
        if (index != null) {
            Log.log(1, "cvelistV5: %d of the %d indexed CVEs are pending", pending.size(), index.ids().size());
        }
        return pending;
    }

    private static boolean isRejectedOrDisputed(CveOrgRecord r) {
        return !"PUBLISHED".equals(r.state()) || r.disputed();
    }

    /** The CVE ids that an evaluated advisory already covers, by its own id, its CVE and its aliases. */
    static Set<String> knownCves(Collection<UnifiedVuln> known) {
        Set<String> out = new HashSet<>();
        for (UnifiedVuln v : known) {
            if (v.cveId != null) out.add(v.cveId);
            if (v.id != null && v.id.startsWith("CVE-")) out.add(v.id);
            if (v.aliases != null) v.aliases.stream().filter(a -> a.startsWith("CVE-")).forEach(out::add);
        }
        return out;
    }

    /** The records worth reporting: published, naming a managed artifact, and not covered by a known advisory. */
    static List<ReleaseReport.Pending> select(List<CveOrgRecord> records, Set<String> managed, Set<String> alreadyKnown) {
        List<ReleaseReport.Pending> out = new ArrayList<>();
        for (CveOrgRecord r : records) {
            if (r.cve() == null || !"PUBLISHED".equals(r.state()) || r.disputed() || alreadyKnown.contains(r.cve())) continue;
            List<String> artifacts = r.managedArtifacts(managed);
            if (artifacts.isEmpty()) continue;
            ReleaseReport.Pending p = new ReleaseReport.Pending();
            p.id = r.cve();
            p.severity = r.severity();
            p.published = r.published();
            p.summary = Po4a.safe(r.title());
            p.artifacts = artifacts;
            p.link = "https://www.cve.org/CVERecord?id=" + r.cve();
            out.add(p);
        }
        out.sort(Comparator.comparing((ReleaseReport.Pending p) -> p.published == null ? "" : p.published).reversed()
                .thenComparing(p -> p.id));
        return out;
    }

    // -----------------------------------------------------------------------
    // Discovery through the cvelistV5 mirror
    // -----------------------------------------------------------------------

    /** CVE ids of every record from {@code fromYear} on whose text mentions Quarkus. */
    List<String> candidates(int fromYear) {
        List<String> ids = new ArrayList<>();
        for (String id : candidatesOfAllYears()) {
            if (yearOf(id) >= fromYear) ids.add(id);
        }
        return ids;
    }

    /** The year in {@code CVE-2026-12345}; 0 if the id is not of that form. */
    static int yearOf(String id) {
        String[] parts = id.split("-");
        if (parts.length != 3) return 0;
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * One search for all years, not one per year: code search is limited to 10 requests a minute and the whole
     * list is a couple of pages, so this takes 2 requests where a search per year took 8 or more.
     */
    private List<String> candidatesOfAllYears() {
        changedInDelta = Set.of();
        if (indexFile != null) return indexedCandidates();
        String key = "quarkus";
        Optional<Cache.Entry<List<String>>> cached = Cache.read(NS_INDEX, key, IDS_TYPE);
        if (cached.isPresent() && cached.get().fresh(INDEX_TTL)) {
            Stats.cacheHit(Stats.GITHUB);
            return cached.get().data();
        }
        try {
            List<String> list = searchAllYears();
            Cache.write(NS_INDEX, key, list, null);
            return list;
        } catch (RuntimeException ex) {
            Optional<Cache.Entry<List<String>>> stale = Cache.readFallback(NS_INDEX, key, IDS_TYPE);
            if (stale.isPresent()) {
                Log.warn("cve.org: GitHub code search failed (%s); using cached list from %s ago",
                        ex.getMessage(), stale.get().ageText());
                return stale.get().data();
            }
            Log.warn("could not search the CVE list (%s); pending CVEs are missing", ex.getMessage());
            return List.of();
        }
    }

    /** Every CVE id whose record mentions Quarkus, by GitHub code search. Throws if the search cannot be completed. */
    private List<String> searchAllYears() {
        Set<String> ids = new TreeSet<>();
        String query = "quarkus repo:" + MIRROR + " path:cves";
        for (int page = 1; page <= MAX_PAGES; page++) {
            final int p = page;
            paceSearch();
            GitHubSearchClient.CodeSearchResult result = Retry.call("GitHub code search page " + p, SEARCH_MAX_WAIT_MS,
                    () -> search.search(query, PAGE_SIZE, p));
            if (result == null || result.items == null || result.items.isEmpty()) break;
            for (GitHubSearchClient.Item item : result.items) {
                if (item.name != null && item.name.startsWith("CVE-") && item.name.endsWith(".json")) {
                    ids.add(item.name.substring(0, item.name.length() - ".json".length()));
                }
            }
            if (result.items.size() < PAGE_SIZE) break;
            if (page == MAX_PAGES) Log.warn("cve.org: more than %d CVE records mention Quarkus; the list is cut off",
                    MAX_PAGES * PAGE_SIZE);
        }
        return List.copyOf(ids);
    }

    /**
     * The candidates from the index file, brought up to date with what changed in the CVE list since it was made.
     * With no file, or one too old for that, a new one is made with code search. A failure leaves the file as it
     * was and the run reports it (an empty list and a warning, which the workflow does not accept).
     */
    private List<String> indexedCandidates() {
        Instant now = clock.instant();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Optional<PendingIndex> existing = PendingIndex.read(indexFile);
        if (existing.isPresent() && existing.get().usable(now)) {
            PendingIndex before = existing.get();
            CveDelta.Result delta = CveDelta.read(zips, before.scanned(), now);
            changedInDelta = Set.copyOf(delta.mentionsQuarkus().keySet());
            index = before.apply(delta, today);
            Log.log(1, "cvelistV5: read %d zips (%.1f MB: %s), %d CVEs changed, %d mention Quarkus",
                    delta.zips(), delta.bytes() / 1e6, String.join(", ", delta.read()), delta.mentionsQuarkus().size(),
                    delta.mentionsQuarkus().values().stream().filter(b -> b).count());
            Log.log(1, "cvelistV5: %d candidate CVEs (%d before), scanned up to %s", index.ids().size(),
                    before.ids().size(), index.scanned());
            return List.copyOf(index.ids());
        }
        if (existing.isPresent()) {
            Log.log(1, "cvelistV5: %s was last scanned %s, more than %d days ago; making a new list with code search",
                    indexFile, existing.get().scanned(), PendingIndex.MAX_AGE.toDays());
        } else {
            Log.log(1, "cvelistV5: no %s; making it with code search", indexFile);
        }
        try {
            List<String> ids = searchAllYears();
            index = PendingIndex.seed(ids, now, today);
            return ids;
        } catch (RuntimeException ex) {
            Log.warn("could not search the CVE list (%s); pending CVEs are missing", ex.getMessage());
            return List.of();
        }
    }

    private static synchronized void paceSearch() {
        long wait = lastSearchAt + searchGapMs - System.currentTimeMillis();
        if (lastSearchAt != 0 && wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastSearchAt = System.currentTimeMillis();
    }

    // -----------------------------------------------------------------------
    // Records at cve.org
    // -----------------------------------------------------------------------

    record RecordResult(Optional<CveOrgRecord> record, boolean stale, boolean notFound) {}

    Optional<CveOrgRecord> record(String id) {
        return record(id, false).record();
    }

    RecordResult record(String id, boolean forceRefresh) {
        Optional<Cache.Entry<CveOrgRecord>> cached = forceRefresh
                ? Optional.empty()
                : Cache.read(NS_RECORD, id, RECORD_TYPE);
        if (cached.isPresent() && cached.get().fresh(RECORD_TTL)) {
            Stats.cacheHit(Stats.CVE_ORG);
            return new RecordResult(Optional.of(cached.get().data()), false, false);
        }
        try {
            CveOrgRecord r = CveOrgRecord.from(Retry.call("cve.org " + id, () -> cveOrg.getRecord(id)));
            Cache.write(NS_RECORD, id, r, null);
            return new RecordResult(Optional.of(r), false, false);
        } catch (RuntimeException ex) {
            int status = ex instanceof WebApplicationException w ? w.getResponse().getStatus() : -1;
            if (status == 404) {
                Log.log(1, "cve.org %s: HTTP 404 (not found)", id);
                return new RecordResult(Optional.empty(), false, true);
            }
            Optional<Cache.Entry<CveOrgRecord>> stale = Cache.readFallback(NS_RECORD, id, RECORD_TYPE);
            if (stale.isPresent()) {
                Log.log(3, "cve.org %s: using stale cache (%s)", id, stale.get().ageText());
                return new RecordResult(Optional.of(stale.get().data()), true, false);
            }
            Log.log(1, "cve.org %s: not available (%s)", id, status != -1 ? "HTTP " + status : ex.getMessage());
            return new RecordResult(Optional.empty(), false, false);
        }
    }
}
