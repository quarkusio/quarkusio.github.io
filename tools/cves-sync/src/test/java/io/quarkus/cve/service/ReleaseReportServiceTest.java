package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.cve.model.UnifiedVuln;
import org.junit.jupiter.api.Test;

class ReleaseReportServiceTest {

    private static UnifiedVuln.Interval interval(String introduced, String fixed, String lastAffected) {
        UnifiedVuln.Interval iv = new UnifiedVuln.Interval();
        iv.introduced = introduced;
        iv.fixed = fixed;
        iv.lastAffected = lastAffected;
        return iv;
    }

    @Test
    void rangeIsWrittenLikeTheQuarkusRanges() {
        assertEquals(">=4.1.0 <4.1.100", ReleaseReportService.range(interval("4.1.0", "4.1.100", null)));
        assertEquals("<4.1.100", ReleaseReportService.range(interval("0", "4.1.100", null)));
        assertEquals("<4.1.100", ReleaseReportService.range(interval(null, "4.1.100", null)));
        assertEquals(">=2.0 <=2.5", ReleaseReportService.range(interval("2.0", null, "2.5")));
        assertEquals(">=2.0", ReleaseReportService.range(interval("2.0", null, null)));
        assertEquals("=2.5.0.Final", ReleaseReportService.range(interval("2.5.0.Final", null, "2.5.0.Final")));
        UnifiedVuln.Interval exclusive = interval("1.2.3", "1.2.5", null);
        exclusive.introducedExclusive = true;
        assertEquals(">1.2.3 <1.2.5", ReleaseReportService.range(exclusive));
        assertEquals("*", ReleaseReportService.range(interval(null, null, null)));
    }

    @Test
    void withdrawnOrRangeLessAdvisoriesDoNotSuppressPendingCves(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path releases = dir.resolve("releases.yaml");
        java.nio.file.Files.writeString(releases, """
                releases:
                  - version: "3.27"
                    micros:
                      - version: "3.27.0"
                        release_date: 2026-09-24
                        parent_release: "3.26.0"
                        bom_url: https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/3.27.0/quarkus-bom-3.27.0.pom
                """);

        UnifiedVuln withdrawn = new UnifiedVuln();
        withdrawn.id = "GHSA-withdrawn";
        withdrawn.cveId = "CVE-2026-1001";
        withdrawn.withdrawn = "2026-10-01T00:00:00Z";
        UnifiedVuln.AffectedPkg wp = new UnifiedVuln.AffectedPkg();
        wp.ecosystem = "Maven";
        wp.name = "io.quarkus:quarkus-vertx-http";
        wp.intervals.add(interval("0", "3.27.1", null));
        withdrawn.affected = java.util.List.of(wp);

        UnifiedVuln noRanges = new UnifiedVuln();
        noRanges.id = "GHSA-noranges";
        noRanges.cveId = "CVE-2026-1002";

        UnifiedVuln evaluable = new UnifiedVuln();
        evaluable.id = "GHSA-evaluable";
        evaluable.cveId = "CVE-2026-1003";
        UnifiedVuln.AffectedPkg ep = new UnifiedVuln.AffectedPkg();
        ep.ecosystem = "Maven";
        ep.name = "io.quarkus:quarkus-vertx-http";
        ep.intervals.add(interval("0", "3.26.0", null));
        evaluable.affected = java.util.List.of(ep);

        java.util.List<String> passedKnownIds = new java.util.ArrayList<>();
        ReleaseReportService service = new ReleaseReportService();
        service.bomService = new QuarkusBomService() {
            @Override
            public java.util.Map<String, String> managedVersions(String release, BomSource source, Runnable onDownload) {
                return java.util.Map.of("io.quarkus:quarkus-vertx-http", "3.27.0");
            }
        };
        service.artifactService = new QuarkusArtifactService() {
            @Override
            public java.util.List<UnifiedVuln> queryAll(java.util.List<String> packageNames, java.util.List<String> githubPackages) {
                return java.util.List.of(withdrawn, noRanges, evaluable);
            }
        };
        service.cveOrgService = new CveOrgService() {
            @Override
            public java.util.List<io.quarkus.cve.model.ReleaseReport.Pending> pending(
                    java.util.Set<String> managed, java.util.Collection<UnifiedVuln> known, int fromYear) {
                known.forEach(v -> passedKnownIds.add(v.cveId));
                return java.util.List.of();
            }
        };

        service.build(releases.toFile());
        assertEquals(java.util.List.of("CVE-2026-1003"), passedKnownIds,
                "only evaluable advisories with Maven ranges should suppress pending CVEs");
    }

    @Test
    void coreOnlyReleaseDoesNotCutPlatformComponentCveUntilPlatformBomFixesIt(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path releases = dir.resolve("releases.yaml");
        java.nio.file.Files.writeString(releases, """
                releases:
                  - version: "3.38"
                    micros:
                      - version: "3.38.1"
                        release_date: 2026-08-01
                        parent_release: "3.38.0"
                        bom_url: https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/3.38.1/quarkus-bom-3.38.1.pom
                  - version: "3.39"
                    micros:
                      - version: "3.39.0.CR1"
                        release_date: 2026-08-10
                        parent_release: "3.38.1"
                      - version: "3.39.0"
                        release_date: 2026-08-20
                        parent_release: "3.39.0.CR1"
                        bom_url: https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/3.39.0/quarkus-bom-3.39.0.pom
                      - version: "3.39.1"
                        release_date: 2026-08-30
                        parent_release: "3.39.0"
                        bom_url: https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/3.39.1/quarkus-bom-3.39.1.pom
                """);

        UnifiedVuln camel = new UnifiedVuln();
        camel.id = "GHSA-camel";
        camel.cveId = "CVE-2026-9001";
        UnifiedVuln.AffectedPkg cp = new UnifiedVuln.AffectedPkg();
        cp.ecosystem = "Maven";
        cp.name = "org.apache.camel.quarkus:camel-quarkus-core";
        cp.intervals.add(interval("3.38.0", "3.39.1", null));
        camel.affected = java.util.List.of(cp);

        ReleaseReportService service = new ReleaseReportService();
        service.bomService = new QuarkusBomService() {
            @Override
            public java.util.Map<String, String> managedVersions(String release, BomSource source, Runnable onDownload) {
                return switch (release) {
                    case "3.38.1" -> java.util.Map.of("org.apache.camel.quarkus:camel-quarkus-core", "3.38.1");
                    case "3.39.0.CR1" -> java.util.Map.of("io.quarkus:quarkus-core", "3.39.0.CR1"); // core-only: no camel
                    case "3.39.0" -> java.util.Map.of("org.apache.camel.quarkus:camel-quarkus-core", "3.39.0");
                    case "3.39.1" -> java.util.Map.of("org.apache.camel.quarkus:camel-quarkus-core", "3.39.1");
                    default -> java.util.Map.of();
                };
            }
        };
        service.artifactService = new QuarkusArtifactService() {
            @Override
            public java.util.List<UnifiedVuln> queryAll(java.util.List<String> packageNames, java.util.List<String> githubPackages) {
                return java.util.List.of(camel);
            }
        };
        service.cveOrgService = new CveOrgService() {
            @Override
            public java.util.List<io.quarkus.cve.model.ReleaseReport.Pending> pending(
                    java.util.Set<String> managed, java.util.Collection<UnifiedVuln> known, int fromYear) {
                return java.util.List.of();
            }
        };

        io.quarkus.cve.model.ReleaseReport report = service.build(releases.toFile());
        io.quarkus.cve.model.ReleaseReport.Advisory a = report.advisories.get("GHSA-camel");
        assertEquals(java.util.List.of("3.38.1", "3.39.0.CR1", "3.39.0"), a.affectedReleases.list);
        assertEquals(1, a.affectedReleases.tree.size());
        assertEquals("3.38.1", a.affectedReleases.tree.get(0).root());
        assertEquals(java.util.List.of("3.39.1"), a.affectedReleases.tree.get(0).cuts());
    }

    @Test
    void coreOnlyInheritanceUsesTreeOrderEvenWhenChildDatePrecedesParent(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path releases = dir.resolve("releases.yaml");
        java.nio.file.Files.writeString(releases, """
                releases:
                  - version: "3.38"
                    micros:
                      - version: "3.38.1"
                        release_date: "2026-08-10"
                        bom_url: "https://example/3.38.1"
                  - version: "3.39"
                    micros:
                      - version: "3.39.0.CR1"
                        release_date: "2026-08-05"
                        parent_release: "3.38.1"
                """);

        UnifiedVuln camel = new UnifiedVuln();
        camel.id = "GHSA-camel";
        camel.cveId = "CVE-2026-9002";
        UnifiedVuln.AffectedPkg cp = new UnifiedVuln.AffectedPkg();
        cp.ecosystem = "Maven";
        cp.name = "org.apache.camel.quarkus:camel-quarkus-core";
        cp.intervals.add(interval("3.38.0", "3.39.1", null));
        camel.affected = java.util.List.of(cp);

        ReleaseReportService service = new ReleaseReportService();
        service.bomService = new QuarkusBomService() {
            @Override
            public java.util.Map<String, String> managedVersions(String release, BomSource source, Runnable onDownload) {
                return "3.38.1".equals(release)
                        ? java.util.Map.of("org.apache.camel.quarkus:camel-quarkus-core", "3.38.1")
                        : java.util.Map.of("io.quarkus:quarkus-core", "3.39.0.CR1");
            }
        };
        service.artifactService = new QuarkusArtifactService() {
            @Override
            public java.util.List<UnifiedVuln> queryAll(java.util.List<String> packageNames, java.util.List<String> githubPackages) {
                return java.util.List.of(camel);
            }
        };
        service.cveOrgService = new CveOrgService() {
            @Override
            public java.util.List<io.quarkus.cve.model.ReleaseReport.Pending> pending(
                    java.util.Set<String> managed, java.util.Collection<UnifiedVuln> known, int fromYear) {
                return java.util.List.of();
            }
        };

        io.quarkus.cve.model.ReleaseReport report = service.build(releases.toFile());
        assertEquals(java.util.List.of("3.39.0.CR1", "3.38.1"), report.advisories.get("GHSA-camel").affectedReleases.list);
    }

    @Test
    void fatalPendingIndexIllegalStateExceptionPropagates(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path releases = dir.resolve("releases.yaml");
        java.nio.file.Files.writeString(releases, """
                releases:
                  - version: "3.38"
                    micros:
                      - version: "3.38.0"
                        release_date: "2026-08-01"
                        bom_url: "https://example/3.38.0"
                      - version: "3.38.1"
                        release_date: "2026-08-10"
                        parent_release: "3.38.0"
                        bom_url: "https://example/3.38.1"
                """);

        ReleaseReportService service = new ReleaseReportService();
        service.bomService = new QuarkusBomService() {
            @Override
            public java.util.Map<String, String> managedVersions(String release, BomSource source, Runnable onDownload) {
                return java.util.Map.of("io.quarkus:quarkus-core", release);
            }
        };
        service.artifactService = new QuarkusArtifactService() {
            @Override
            public java.util.List<UnifiedVuln> queryAll(java.util.List<String> packageNames, java.util.List<String> githubPackages) {
                return java.util.List.of();
            }
        };
        service.cveOrgService = new CveOrgService() {
            @Override
            public java.util.List<io.quarkus.cve.model.ReleaseReport.Pending> pending(
                    java.util.Set<String> managed, java.util.Collection<UnifiedVuln> known, int fromYear) {
                throw new IllegalStateException("missing end-of-day zip");
            }
        };

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.build(releases.toFile()));
    }

    @Test
    void mergeByCveCombinesDuplicateGhsasCaseInsensitively() {
        UnifiedVuln a = new UnifiedVuln();
        a.id = "GHSA-bbbb-0000-0001";
        a.cveId = "CVE-2022-0225";
        a.severity = "HIGH";
        a.published = "2022-05-01T00:00:00Z";
        a.summary = "Keycloak XSS";
        UnifiedVuln.AffectedPkg pa = new UnifiedVuln.AffectedPkg();
        pa.ecosystem = "maven";
        pa.name = "org.keycloak:keycloak-core";
        pa.intervals.add(interval("0", "20.0.0", null));
        a.affected = java.util.List.of(pa);

        UnifiedVuln b = new UnifiedVuln();
        b.id = "GHSA-aaaa-0000-0001";
        b.cveId = "CVE-2022-0225";
        b.severity = "MEDIUM";
        b.published = "2022-06-01T00:00:00Z";
        b.summary = "";
        UnifiedVuln.AffectedPkg pb = new UnifiedVuln.AffectedPkg();
        pb.ecosystem = "Maven";
        pb.name = "org.keycloak:keycloak-core";
        pb.intervals.add(interval("0", null, "16.1.0"));
        b.affected = java.util.List.of(pb);

        java.util.List<UnifiedVuln> merged = ReleaseReportService.mergeByCve(java.util.List.of(a, b));
        assertEquals(1, merged.size());
        UnifiedVuln m = merged.get(0);
        assertEquals("GHSA-aaaa-0000-0001", m.id);
        assertEquals("HIGH", m.severity);
        assertEquals("2022-05-01T00:00:00Z", m.published);
        assertEquals("Keycloak XSS", m.summary);
        assertEquals(1, m.affected.size(), "maven and Maven must merge into one AffectedPkg");
        assertEquals(2, m.affected.get(0).intervals.size());
        org.junit.jupiter.api.Assertions.assertTrue(m.aliases.contains("GHSA-bbbb-0000-0001"));
    }
}