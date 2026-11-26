package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cve.model.CveOrgRecord;
import io.quarkus.cve.client.GitHubSearchClient;
import io.quarkus.cve.model.ReleaseReport;
import io.quarkus.cve.util.Cache;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CveOrgServiceTest {

    static final Set<String> MANAGED = Set.of(
            "io.quarkus:quarkus-vertx-http", "io.quarkus:quarkus-qute", "io.netty:netty-codec-http",
            "io.netty:netty-codec-http2", "io.quarkus.qute:qute-core", "org.jboss.resteasy:resteasy-core");

    static CveOrgRecord record(String json) throws Exception {
        return CveOrgRecord.from(new ObjectMapper().readTree(json));
    }

    static final String RED_HAT = """
            {"cveMetadata": {"cveId": "CVE-2026-87743", "state": "PUBLISHED", "datePublished": "2026-09-18T09:38:27.326Z"},
             "containers": {
               "cna": {
                 "title": "Quarkus-vertx-http: authorization bypass via path normalization discrepancy",
                 "metrics": [{"other": {"type": "Red Hat severity rating", "content": {"value": "Important"}}},
                             {"cvssV3_1": {"baseScore": 7.5, "baseSeverity": "HIGH"}}],
                 "affected": [
                   {"product": "Red Hat build of Quarkus 3.27.5.SP2", "packageName": "quarkus-vertx-http", "defaultStatus": "unaffected"},
                   {"product": "Red Hat Fuse 7", "packageName": "quarkus-vertx-http", "defaultStatus": "unknown"},
                   {"product": "Keycloak", "packageName": "rhbk/keycloak-rhel9"}]},
               "adp": [{"metrics": [{"other": {"type": "ssvc", "content": {}}}]}]}}
            """;

    @Test
    void readsTheFieldsTheReportNeeds() throws Exception {
        CveOrgRecord r = record(RED_HAT);
        assertEquals("CVE-2026-87743", r.cve());
        assertEquals("PUBLISHED", r.state());
        assertEquals("2026-09-18T09:38:27Z", r.published());
        assertEquals("HIGH", r.severity());
        assertEquals("Quarkus-vertx-http: authorization bypass via path normalization discrepancy", r.title());
        assertEquals(List.of("quarkus-vertx-http", "rhbk/keycloak-rhel9"), r.packages());
    }

    @Test
    void aBareArtifactIdIsMatchedToTheManagedArtifactWithThatId() throws Exception {
        assertEquals(List.of("io.quarkus:quarkus-vertx-http"), record(RED_HAT).managedArtifacts(MANAGED));
    }

    @Test
    void groupAndArtifactMayBeSeparatedByASlashOrAColon() throws Exception {
        CveOrgRecord r = record("""
                {"cveMetadata": {"cveId": "CVE-2026-1", "state": "PUBLISHED"},
                 "containers": {"cna": {"affected": [
                   {"packageName": "io.quarkus/quarkus-qute"}, {"packageName": "io.netty:netty-codec-http2"},
                   {"packageName": "io.quarkus/not-managed"}]}}}
                """);
        assertEquals(List.of("io.netty:netty-codec-http2", "io.quarkus:quarkus-qute"), r.managedArtifacts(MANAGED));
    }

    @Test
    void aGroupThatDoesNotMatchTheManagedOneIsNotAMatch() throws Exception {
        CveOrgRecord r = record("""
                {"cveMetadata": {"cveId": "CVE-2026-1", "state": "PUBLISHED"},
                 "containers": {"cna": {"affected": [{"packageName": "org.other/quarkus-qute"}]}}}
                """);
        assertTrue(r.managedArtifacts(MANAGED).isEmpty());
    }

    @Test
    void severityFallsBackToTheVendorRatingWhenThereIsNoCvss() throws Exception {
        CveOrgRecord r = record("""
                {"cveMetadata": {"cveId": "CVE-2026-1", "state": "PUBLISHED"},
                 "containers": {"cna": {"metrics": [{"other": {"content": {"value": "Moderate"}}}]}}}
                """);
        assertEquals("MEDIUM", r.severity());
    }

    @Test
    void titleFallsBackToTheEnglishDescription() throws Exception {
        CveOrgRecord r = record("""
                {"cveMetadata": {"cveId": "CVE-2026-1", "state": "PUBLISHED"},
                 "containers": {"cna": {"descriptions": [{"lang": "fr", "value": "non"}, {"lang": "en", "value": "yes"}]}}}
                """);
        assertEquals("yes", r.title());
    }

    @Test
    void onlyPublishedRecordsThatNameAManagedArtifactAreReported() throws Exception {
        CveOrgRecord good = record(RED_HAT);
        CveOrgRecord rejected = record("""
                {"cveMetadata": {"cveId": "CVE-2026-2", "state": "REJECTED"},
                 "containers": {"cna": {"affected": [{"packageName": "quarkus-qute"}]}}}
                """);
        CveOrgRecord elsewhere = record("""
                {"cveMetadata": {"cveId": "CVE-2026-3", "state": "PUBLISHED"},
                 "containers": {"cna": {"affected": [{"packageName": "jolokia-core"}]}}}
                """);
        List<ReleaseReport.Pending> out = CveOrgService.select(List.of(good, rejected, elsewhere), MANAGED, Set.of());
        assertEquals(1, out.size());
        assertEquals("CVE-2026-87743", out.get(0).id);
        assertEquals("HIGH", out.get(0).severity);
        assertEquals(List.of("io.quarkus:quarkus-vertx-http"), out.get(0).artifacts);
        assertEquals("https://www.cve.org/CVERecord?id=CVE-2026-87743", out.get(0).link);
    }

    @Test
    void aCveThatAnAdvisoryAlreadyCoversIsNotPending() throws Exception {
        assertTrue(CveOrgService.select(List.of(record(RED_HAT)), MANAGED, Set.of("CVE-2026-87743")).isEmpty());
    }

    @Test
    void newestFirstThenById() throws Exception {
        String t = """
                {"cveMetadata": {"cveId": "%s", "state": "PUBLISHED", "datePublished": "%s"},
                 "containers": {"cna": {"affected": [{"packageName": "quarkus-qute"}]}}}
                """;
        List<ReleaseReport.Pending> out = CveOrgService.select(List.of(
                record(t.formatted("CVE-2026-B", "2026-01-01T00:00:00Z")),
                record(t.formatted("CVE-2026-C", "2026-03-01T00:00:00Z")),
                record(t.formatted("CVE-2026-A", "2026-01-01T00:00:00Z"))), MANAGED, Set.of());
        assertEquals(List.of("CVE-2026-C", "CVE-2026-A", "CVE-2026-B"), out.stream().map(p -> p.id).toList());
    }

    // -- discovery ----------------------------------------------------------------------------------------

    @BeforeEach
    void noPacing() {
        CveOrgService.searchGapMs = 0;
    }

    @AfterEach
    void resetCache() {
        CveOrgService.searchGapMs = 7_000;
        Cache.configure(null, Cache.Mode.NORMAL);
    }

    static GitHubSearchClient.CodeSearchResult page(String... names) {
        GitHubSearchClient.CodeSearchResult r = new GitHubSearchClient.CodeSearchResult();
        r.items = new ArrayList<>();
        for (String n : names) {
            GitHubSearchClient.Item item = new GitHubSearchClient.Item();
            item.name = n;
            r.items.add(item);
        }
        return r;
    }

    @Test
    void yearComesFromTheCveId() {
        assertEquals(2026, CveOrgService.yearOf("CVE-2026-87743"));
        assertEquals(0, CveOrgService.yearOf("GHSA-xxxx-yyyy-zzzz1"));
        assertEquals(0, CveOrgService.yearOf("nonsense"));
    }

    @Test
    void oneSearchFindsTheCandidatesOfEveryYearAndTheOldOnesAreLeftOut(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        List<String> queries = new ArrayList<>();
        CveOrgService service = new CveOrgService();
        service.search = (q, perPage, p) -> {
            queries.add(q + " #" + p);
            return page("CVE-2022-1.json", "CVE-2023-2.json", "CVE-2026-3.json", "README.md");
        };
        assertEquals(List.of("CVE-2023-2", "CVE-2026-3"), service.candidates(2023));
        assertEquals(1, queries.size(), "all years are found with one search");
        assertEquals("quarkus repo:CVEProject/cvelistV5 path:cves #1", queries.get(0));

        // a second look, in the same run or the next, is answered from the cache
        assertEquals(List.of("CVE-2026-3"), service.candidates(2026));
        assertEquals(1, queries.size());
    }

    @Test
    void aFailedSearchWithNoListYieldsNothingAndCachesNothing(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        CveOrgService service = new CveOrgService();
        service.search = (q, perPage, p) -> {
            throw new IllegalStateException("boom");
        };
        // the run reports a warning, which makes the workflow refuse the output, and a failure is not remembered
        assertEquals(List.of(), service.candidates(2023));
        service.search = (q, perPage, p) -> page("CVE-2026-3.json");
        assertEquals(List.of("CVE-2026-3"), service.candidates(2023));
    }

    @Test
    void theWaitsSeenInCiFitTheLimitForCodeSearch() {
        // GitHub asked for up to 736 s on a refused code search; giving up at that would leave pending CVEs out
        assertTrue(CveOrgService.SEARCH_MAX_WAIT_MS >= 736_000);
    }

    // -- discovery through the index file -----------------------------------------------------------------

    static final java.time.Instant NOON = java.time.Instant.parse("2026-11-24T12:30:00Z");

    static CveOrgService indexed(Path dir, List<String> searched, java.util.Map<String, byte[]> zips) {
        Cache.configure(dir.resolve("cache"), Cache.Mode.NORMAL);
        CveOrgService service = new CveOrgService();
        service.clock = java.time.Clock.fixed(NOON, java.time.ZoneOffset.UTC);
        service.search = (q, perPage, p) -> {
            searched.add(q + " #" + p);
            return page("CVE-2026-1.json", "CVE-2026-2.json");
        };
        service.zips = (tag, asset) -> java.util.Optional.ofNullable(zips.get(asset));
        service.useIndex(dir.resolve("pending-cves.yaml"));
        return service;
    }

    @Test
    void withNoIndexFileItIsMadeWithCodeSearchAndIsWrittenOnlyByTheCaller(@TempDir Path dir) {
        List<String> searched = new ArrayList<>();
        CveOrgService service = indexed(dir, searched, java.util.Map.of());
        assertEquals(List.of("CVE-2026-1", "CVE-2026-2"), service.candidates(2020));
        assertEquals(1, searched.size());
        assertTrue(service.indexToWrite().isPresent());
        assertEquals(java.time.Instant.parse("2026-11-17T00:00:00Z"), service.indexToWrite().get().scanned());
        assertTrue(!java.nio.file.Files.exists(dir.resolve("pending-cves.yaml")), "the service does not write the file");
    }

    @Test
    void withAnIndexFileNoSearchIsMadeAndTheDeltaBringsItUpToDate(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), """
                scanned: "2026-11-23T00:00:00Z"
                pending:
                - id: "CVE-2026-1"
                  noticed: "2026-11-20"
                - id: "CVE-2026-2"
                  noticed: "2026-11-21"
                """);
        List<String> searched = new ArrayList<>();
        java.util.Map<String, byte[]> zips = new java.util.HashMap<>();
        zips.put("2026-11-23_delta_CVEs_at_end_of_day.zip", io.quarkus.cve.util.CveDeltaTest.zip(
                "d/CVE-2026-3.json", "Quarkus", "d/CVE-2026-2.json", "reworded"));
        zips.put("2026-11-24_delta_CVEs_at_1100Z.zip", io.quarkus.cve.util.CveDeltaTest.zip());
        CveOrgService service = indexed(dir, searched, zips);
        assertEquals(List.of("CVE-2026-1", "CVE-2026-3"), service.candidates(2020));
        assertEquals(0, searched.size(), "code search is not used when there is an index");
        assertEquals(java.time.Instant.parse("2026-11-24T11:00:00Z"), service.indexToWrite().get().scanned());
    }

    @Test
    void anIndexTooOldForTheDeltaIsMadeAgainWithCodeSearch(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), "scanned: \"2026-09-01T10:00:00Z\"\npending:\n- id: \"CVE-2026-9\"\n  noticed: \"2026-09-01\"\n");
        List<String> searched = new ArrayList<>();
        CveOrgService service = indexed(dir, searched, java.util.Map.of());
        assertEquals(List.of("CVE-2026-1", "CVE-2026-2"), service.candidates(2020));
        assertEquals(1, searched.size());
    }

    @Test
    void aDeltaThatCannotBeReadFailsTheCandidatesInsteadOfLeavingThemOutAndWritesNoIndex(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), "scanned: \"2026-11-22T00:00:00Z\"\npending:\n");
        CveOrgService service = indexed(dir, new ArrayList<>(), java.util.Map.of());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.candidates(2020));
        assertTrue(service.indexToWrite().isEmpty());
    }

    @Test
    void aFailedSearchToMakeTheIndexLeavesNoIndexToWrite(@TempDir Path dir) {
        CveOrgService service = indexed(dir, new ArrayList<>(), java.util.Map.of());
        service.search = (q, perPage, p) -> {
            throw new IllegalStateException("refused");
        };
        assertEquals(List.of(), service.candidates(2020));
        assertTrue(service.indexToWrite().isEmpty());
    }

    @Test
    void aRecordChangedInTheDeltaBypassesAFreshCacheEntry(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), """
                scanned: "2026-11-23T00:00:00Z"
                pending:
                - id: "CVE-2026-99"
                  noticed: "2026-11-23"
                """);
        java.util.Map<String, byte[]> zips = new java.util.HashMap<>();
        zips.put("2026-11-23_delta_CVEs_at_end_of_day.zip", io.quarkus.cve.util.CveDeltaTest.zip(
                "d/CVE-2026-99.json", "Quarkus revised with affected package"));
        zips.put("2026-11-24_delta_CVEs_at_1100Z.zip", io.quarkus.cve.util.CveDeltaTest.zip());

        CveOrgService service = indexed(dir, new ArrayList<>(), zips);
        // Simulate a cached record fetched < 24h ago before the CNA added affected packages
        Cache.write("cve-org", "CVE-2026-99", record("""
                {"cveMetadata": {"cveId": "CVE-2026-99", "state": "PUBLISHED", "datePublished": "2026-11-23T01:00:00Z"},
                 "containers": {"cna": {"title": "Initial stub", "affected": []}}}
                """), null);

        ObjectMapper mapper = new ObjectMapper();
        List<String> fetched = new ArrayList<>();
        service.cveOrg = id -> {
            fetched.add(id);
            try {
                return mapper.readTree("""
                        {"cveMetadata": {"cveId": "CVE-2026-99", "state": "PUBLISHED", "datePublished": "2026-11-23T01:00:00Z"},
                         "containers": {"cna": {"title": "Revised", "affected": [{"packageName": "quarkus-vertx-http"}]}}}
                        """);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };

        List<ReleaseReport.Pending> out = service.pending(MANAGED, List.of(), 2020);
        assertEquals(List.of("CVE-2026-99"), fetched, "delta-changed CVE must bypass the 24h cache");
        assertEquals(List.of("CVE-2026-99"), out.stream().map(p -> p.id).toList());
    }

    @Test
    void coveredAndUnmanagedCvesStayInTheIndexSoWithdrawnAdvisoriesAndNewReleasesFindThem(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), """
                scanned: "2026-11-24T11:00:00Z"
                pending:
                - id: "CVE-2026-87743"
                  noticed: "2026-11-20"
                - id: "CVE-2026-50"
                  noticed: "2026-11-21"
                """);
        java.util.Map<String, byte[]> zips = java.util.Map.of("2026-11-24_delta_CVEs_at_1100Z.zip", io.quarkus.cve.util.CveDeltaTest.zip());
        ObjectMapper mapper = new ObjectMapper();

        // Run 1: CVE-2026-87743 is covered by an advisory; CVE-2026-50 names an artifact no release manages yet
        CveOrgService run1 = indexed(dir, new ArrayList<>(), zips);
        run1.cveOrg = id -> {
            try {
                if ("CVE-2026-50".equals(id)) {
                    return mapper.readTree("""
                            {"cveMetadata": {"cveId": "CVE-2026-50", "state": "PUBLISHED"},
                             "containers": {"cna": {"title": "New ext", "affected": [{"packageName": "io.quarkus:quarkus-new-ext"}]}}}
                            """);
                }
                return mapper.readTree(RED_HAT);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        io.quarkus.cve.model.UnifiedVuln covered = new io.quarkus.cve.model.UnifiedVuln();
        covered.id = "GHSA-1111-2222-3333";
        covered.cveId = "CVE-2026-87743";

        assertTrue(run1.pending(MANAGED, List.of(covered), 2020).isEmpty());
        assertEquals(Set.of("CVE-2026-50", "CVE-2026-87743"), run1.indexToWrite().orElseThrow().ids());

        // Persist the index from Run 1 and run again after the advisory is withdrawn and a release adds quarkus-new-ext
        java.nio.file.Files.write(dir.resolve("pending-cves.yaml"), run1.indexToWrite().orElseThrow().bytes());
        CveOrgService run2 = indexed(dir, new ArrayList<>(), zips);
        run2.cveOrg = run1.cveOrg;
        Set<String> expandedManaged = new java.util.HashSet<>(MANAGED);
        expandedManaged.add("io.quarkus:quarkus-new-ext");

        List<ReleaseReport.Pending> out = run2.pending(expandedManaged, List.of(), 2020);
        assertEquals(Set.of("CVE-2026-50", "CVE-2026-87743"),
                new java.util.HashSet<>(out.stream().map(p -> p.id).toList()));
    }

    @Test
    void staleOrUnreadableCveOrgRecordsEmitWarnings(@TempDir Path dir) throws Exception {
        Cache.configure(dir, Cache.Mode.NORMAL);
        // Seed an expired cache entry (> 24h old) by writing a raw envelope with an old fetchedAt
        java.nio.file.Path recordDir = dir.resolve("cve-org");
        java.nio.file.Files.createDirectories(recordDir);
        java.nio.file.Files.writeString(recordDir.resolve("CVE-2026-87743.json"), """
                {"v":1,"key":"CVE-2026-87743","fetchedAt":"2026-11-01T00:00:00Z",
                 "data":{"cve":"CVE-2026-87743","state":"PUBLISHED","published":"2026-09-18T09:38:27Z",
                         "title":"Cached","severity":"HIGH","packages":["quarkus-vertx-http"]}}
                """);

        CveOrgService service = new CveOrgService();
        service.search = (q, perPage, p) -> page("CVE-2026-87743.json", "CVE-2026-99999.json");
        service.cveOrg = id -> {
            throw new IllegalStateException("cve.org down");
        };

        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
        java.io.PrintStream prev = System.err;
        System.setErr(new java.io.PrintStream(err, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            List<ReleaseReport.Pending> out = service.pending(MANAGED, List.of(), 2020);
            assertEquals(List.of("CVE-2026-87743"), out.stream().map(p -> p.id).toList());
        } finally {
            System.setErr(prev);
        }
        String stderr = err.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(stderr.contains("warning: cve.org: 1 candidate record(s) could not be refreshed; using older cached data"), stderr);
        assertTrue(stderr.contains("warning: cve.org: 1 candidate record(s) could not be read; pending CVEs may be missing"), stderr);
    }

    @Test
    void disputedOrRejectedRecordsAreExcludedAndFlagged(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), """
                scanned: "2026-11-23T00:00:00Z"
                pending:
                - id: "CVE-2026-77"
                  noticed: "2026-11-23"
                """);
        java.util.Map<String, byte[]> zips = java.util.Map.of(
                "2026-11-23_delta_CVEs_at_end_of_day.zip", io.quarkus.cve.util.CveDeltaTest.zip("d/CVE-2026-77.json", "Quarkus"),
                "2026-11-24_delta_CVEs_at_1100Z.zip", io.quarkus.cve.util.CveDeltaTest.zip());

        CveOrgService service = indexed(dir, new ArrayList<>(), zips);
        ObjectMapper mapper = new ObjectMapper();
        service.cveOrg = id -> {
            try {
                return mapper.readTree("""
                        {"cveMetadata": {"cveId": "CVE-2026-77", "state": "PUBLISHED"},
                         "containers": {"cna": {"tags": ["disputed"], "affected": [{"packageName": "quarkus-vertx-http"}]}}}
                        """);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };

        io.quarkus.cve.model.UnifiedVuln known = new io.quarkus.cve.model.UnifiedVuln();
        known.id = "GHSA-disputed";
        known.cveId = "CVE-2026-77";

        assertTrue(service.pending(MANAGED, List.of(known), 2020).isEmpty());
        assertEquals(Set.of("CVE-2026-77"), service.rejectedKnownCves());
    }

    @Test
    void rejectedKnownCveIsCaughtWhenRejectionRemovesQuarkusAndRememberedOnDay2(@TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("pending-cves.yaml"), """
                scanned: "2026-11-23T00:00:00Z"
                pending:
                - id: "CVE-2026-88"
                  noticed: "2026-11-20"
                """);
        // Day 1 delta: CVE-2026-88 rejected with generic text (no "quarkus"), plus a dependency CVE-2026-89 rejected
        java.util.Map<String, byte[]> day1Zips = java.util.Map.of(
                "2026-11-23_delta_CVEs_at_end_of_day.zip", io.quarkus.cve.util.CveDeltaTest.zip(
                        "d/CVE-2026-88.json", "This CVE ID has been rejected by its CNA.",
                        "d/CVE-2026-89.json", "Netty CVE rejected."),
                "2026-11-24_delta_CVEs_at_1100Z.zip", io.quarkus.cve.util.CveDeltaTest.zip());

        ObjectMapper mapper = new ObjectMapper();
        List<String> fetched = new ArrayList<>();
        io.quarkus.cve.client.CveOrgClient client = id -> {
            fetched.add(id);
            try {
                return mapper.readTree("{\"cveMetadata\": {\"cveId\": \"" + id + "\", \"state\": \"REJECTED\"}, \"containers\": {\"cna\": {}}}");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };

        io.quarkus.cve.model.UnifiedVuln k1 = new io.quarkus.cve.model.UnifiedVuln();
        k1.id = "GHSA-88";
        k1.cveId = "CVE-2026-88";
        io.quarkus.cve.model.UnifiedVuln k2 = new io.quarkus.cve.model.UnifiedVuln();
        k2.id = "GHSA-89";
        k2.cveId = "CVE-2026-89";

        CveOrgService day1 = indexed(dir, new ArrayList<>(), day1Zips);
        day1.cveOrg = client;
        day1.pending(MANAGED, List.of(k1, k2), 2020);
        assertEquals(Set.of("CVE-2026-88", "CVE-2026-89"), day1.rejectedKnownCves(),
                "Day 1 must catch rejected known CVEs even when 'quarkus' was removed or CVE is a dependency");
        java.nio.file.Files.write(dir.resolve("pending-cves.yaml"), day1.indexToWrite().orElseThrow().bytes());

        // Day 2: empty delta (neither CVE changed today) -> must still be remembered from cache with 0 HTTP calls
        fetched.clear();
        java.util.Map<String, byte[]> day2Zips = java.util.Map.of(
                "2026-11-24_delta_CVEs_at_1100Z.zip", io.quarkus.cve.util.CveDeltaTest.zip());
        CveOrgService day2 = indexed(dir, new ArrayList<>(), day2Zips);
        day2.cveOrg = client;
        day2.pending(MANAGED, List.of(k1, k2), 2020);
        assertEquals(Set.of("CVE-2026-88", "CVE-2026-89"), day2.rejectedKnownCves(),
                "Day 2 must remember rejected known CVEs from cache");
        assertTrue(fetched.isEmpty(), "Day 2 should use fresh cached cve-org records without HTTP calls");
    }

    @Test
    void descriptionFallbackMatchesBcp47EnglishTags() throws Exception {
        CveOrgRecord r = record("""
                {"cveMetadata": {"cveId": "CVE-2026-55", "state": "PUBLISHED"},
                 "containers": {"cna": {"descriptions": [{"lang": "en-US", "value": "US English description"}]}}}
                """);
        assertEquals("US English description", r.title());
    }

    @Test
    void cveOrg404DoesNotEmitFatalWarning(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        CveOrgService service = new CveOrgService();
        service.search = (q, perPage, p) -> page("CVE-2026-404.json");
        service.cveOrg = id -> {
            throw new jakarta.ws.rs.WebApplicationException(jakarta.ws.rs.core.Response.status(404).build());
        };

        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
        java.io.PrintStream prev = System.err;
        System.setErr(new java.io.PrintStream(err, true, java.nio.charset.StandardCharsets.UTF_8));
        try {
            assertTrue(service.pending(MANAGED, List.of(), 2020).isEmpty());
        } finally {
            System.setErr(prev);
        }
        String stderr = err.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(!stderr.contains("warning:"), "HTTP 404 at cve.org must not emit a fatal warning: " + stderr);
    }
}
