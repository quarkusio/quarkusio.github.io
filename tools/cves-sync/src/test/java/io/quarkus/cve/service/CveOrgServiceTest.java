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
}
