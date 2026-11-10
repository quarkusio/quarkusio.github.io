package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cve.model.CveOrgRecord;
import io.quarkus.cve.model.ReleaseReport;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

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
}
