package io.quarkus.cve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.cve.client.CveOrgClient;
import io.quarkus.cve.client.GitHubSearchClient;
import io.quarkus.cve.model.CveOrgRecord;
import io.quarkus.cve.model.ReleaseReport;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Cache;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.Retry;
import io.quarkus.cve.util.Stats;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.time.Duration;
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

    /**
     * @param managed   every {@code group:artifact} any scanned release manages
     * @param known     the advisories that have version ranges; their CVEs are not pending
     * @param fromYear  oldest year of CVE ids to look at
     */
    public List<ReleaseReport.Pending> pending(Set<String> managed, Collection<UnifiedVuln> known, int fromYear) {
        List<CveOrgRecord> records = new ArrayList<>();
        int skipped = 0;
        Set<String> alreadyKnown = knownCves(known);
        for (String id : candidates(fromYear)) {
            if (alreadyKnown.contains(id)) continue;
            Optional<CveOrgRecord> r = record(id);
            if (r.isPresent()) records.add(r.get());
            else skipped++;
        }
        if (skipped > 0) Log.log(1, "cve.org: %d candidate records could not be read", skipped);
        return select(records, managed, alreadyKnown);
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
            if (r.cve() == null || !"PUBLISHED".equals(r.state()) || alreadyKnown.contains(r.cve())) continue;
            List<String> artifacts = r.managedArtifacts(managed);
            if (artifacts.isEmpty()) continue;
            ReleaseReport.Pending p = new ReleaseReport.Pending();
            p.id = r.cve();
            p.severity = r.severity();
            p.published = r.published();
            p.summary = r.title();
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
        Set<String> ids = new TreeSet<>();
        int thisYear = java.time.Year.now().getValue();
        for (int year = fromYear; year <= thisYear; year++) ids.addAll(candidatesOf(year));
        return List.copyOf(ids);
    }

    private List<String> candidatesOf(int year) {
        String key = "quarkus-" + year;
        Optional<Cache.Entry<List<String>>> cached = Cache.read(NS_INDEX, key, IDS_TYPE);
        if (cached.isPresent() && cached.get().fresh(INDEX_TTL)) {
            Stats.cacheHit(Stats.GITHUB);
            return cached.get().data();
        }
        try {
            Set<String> ids = new TreeSet<>();
            String query = "quarkus repo:" + MIRROR + " path:cves/" + year;
            for (int page = 1; page <= MAX_PAGES; page++) {
                final int p = page;
                GitHubSearchClient.CodeSearchResult result = Retry.call("GitHub code search " + year + " page " + p,
                        () -> search.search(query, PAGE_SIZE, p));
                if (result == null || result.items == null || result.items.isEmpty()) break;
                for (GitHubSearchClient.Item item : result.items) {
                    if (item.name != null && item.name.startsWith("CVE-") && item.name.endsWith(".json")) {
                        ids.add(item.name.substring(0, item.name.length() - ".json".length()));
                    }
                }
                if (result.items.size() < PAGE_SIZE) break;
                if (page == MAX_PAGES) Log.warn("cve.org: more than %d CVE records of %d mention Quarkus; the list is cut off",
                        MAX_PAGES * PAGE_SIZE, year);
            }
            List<String> list = List.copyOf(ids);
            Cache.write(NS_INDEX, key, list, null);
            return list;
        } catch (RuntimeException ex) {
            Optional<Cache.Entry<List<String>>> stale = Cache.readFallback(NS_INDEX, key, IDS_TYPE);
            if (stale.isPresent()) {
                Log.log(1, "cve.org: GitHub code search for %d failed (%s); using the list from %s ago",
                        year, ex.getMessage(), stale.get().ageText());
                return stale.get().data();
            }
            Log.warn("could not search the CVE list for %d (%s); pending CVEs from that year are missing", year, ex.getMessage());
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // Records at cve.org
    // -----------------------------------------------------------------------

    Optional<CveOrgRecord> record(String id) {
        Optional<Cache.Entry<CveOrgRecord>> cached = Cache.read(NS_RECORD, id, RECORD_TYPE);
        if (cached.isPresent() && cached.get().fresh(RECORD_TTL)) {
            Stats.cacheHit(Stats.CVE_ORG);
            return Optional.of(cached.get().data());
        }
        try {
            CveOrgRecord r = CveOrgRecord.from(Retry.call("cve.org " + id, () -> cveOrg.getRecord(id)));
            Cache.write(NS_RECORD, id, r, null);
            return Optional.of(r);
        } catch (RuntimeException ex) {
            int status = ex instanceof WebApplicationException w ? w.getResponse().getStatus() : -1;
            Optional<Cache.Entry<CveOrgRecord>> stale = Cache.readFallback(NS_RECORD, id, RECORD_TYPE);
            if (stale.isPresent()) {
                Log.log(3, "cve.org %s: using stale cache (%s)", id, stale.get().ageText());
                return Optional.of(stale.get().data());
            }
            Log.log(1, "cve.org %s: not available (%s)", id, status != -1 ? "HTTP " + status : ex.getMessage());
            return Optional.empty();
        }
    }
}
