package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.cve.client.OsvApiClient;
import io.quarkus.cve.model.OsvBatchQueryRequest;
import io.quarkus.cve.model.OsvBatchQueryResponse;
import io.quarkus.cve.model.OsvQueryRequest;
import io.quarkus.cve.model.OsvQueryResponse;
import io.quarkus.cve.model.OsvVulnerability;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Cache;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuarkusArtifactServiceTest {

    @AfterEach
    void resetCache() {
        Cache.configure(null, Cache.Mode.NORMAL);
    }

    private static String captureStderr(Runnable action) {
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream prev = System.err;
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setErr(prev);
        }
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void osvQueryFailureEmitsWarning(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                throw new IllegalStateException("osv down");
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                throw new IllegalStateException("osv down");
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                throw new IllegalStateException("osv down");
            }
        };

        String stderr = captureStderr(() -> service.queryAll(List.of("io.quarkus:quarkus-core"), List.of()));
        assertTrue(stderr.contains("warning: could not query OSV for 1 of 1 package(s)"), stderr);
    }

    @Test
    void osvStaleDetailFallbackEmitsWarning(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        OsvVulnerability cached = new OsvVulnerability();
        cached.id = "GHSA-stale-1";
        cached.modified = "2026-11-20T00:00:00Z";
        cached.summary = "Old summary";
        Cache.write("osv-v2", "GHSA-stale-1", cached, null);

        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                throw new IllegalStateException("osv detail down");
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                return new OsvQueryResponse();
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                OsvVulnerability head = new OsvVulnerability();
                head.id = "GHSA-stale-1";
                head.modified = "2026-11-25T00:00:00Z"; // newer than cached!
                OsvQueryResponse qr = new OsvQueryResponse();
                qr.vulns = List.of(head);
                OsvBatchQueryResponse br = new OsvBatchQueryResponse();
                br.results = List.of(qr);
                return br;
            }
        };

        String stderr = captureStderr(() -> {
            List<UnifiedVuln> out = service.queryAll(List.of("io.quarkus:quarkus-core"), List.of());
            assertEquals(1, out.size());
        });
        assertTrue(stderr.contains("warning: 1 OSV record(s) changed upstream could not be reloaded; using older cached data"),
                stderr);
    }

    @Test
    void githubNonRateLimitFailureEmitsWarningEvenWithStaleFallback(@TempDir Path dir) throws Exception {
        Cache.configure(dir, Cache.Mode.NORMAL);
        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                return null;
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                return new OsvQueryResponse();
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                OsvBatchQueryResponse br = new OsvBatchQueryResponse();
                br.results = List.of();
                return br;
            }
        };
        service.githubAdvisoryClient = (eco, pkg, perPage, page, sort, direction, inm) -> {
            throw new IllegalStateException("github 502");
        };

        // Case 1: uncached failure
        String uncachedErr = captureStderr(() -> service.queryAll(List.of(), List.of("io.quarkus:quarkus-core")));
        assertTrue(uncachedErr.contains("warning: GitHub Advisory query failed for 1 request(s)"), uncachedErr);

        // Case 2: expired cached entry used as fallback
        Path ghDir = dir.resolve("github");
        java.nio.file.Files.createDirectories(ghDir);
        // batch key for single package "io.quarkus:quarkus-core": sha1("io.quarkus:quarkus-core")[0..16] = b47e93f65c40fb4f
        String sha = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-1")
                        .digest("io.quarkus:quarkus-core".getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        java.nio.file.Files.writeString(ghDir.resolve("batch-" + sha + ".json"), """
                {"v":1,"fetchedAt":"2026-11-01T00:00:00Z","data":[]}
                """, StandardCharsets.UTF_8);

        String staleErr = captureStderr(() -> service.queryAll(List.of(), List.of("io.quarkus:quarkus-core")));
        assertTrue(staleErr.contains("warning: GitHub Advisory query failed for 1 request(s) of 50 packages each (1 used older cached data)"),
                staleErr);
    }

    @Test
    void osvExactVersionsWithoutRangesBecomeEvaluableIntervals() {
        OsvVulnerability v = new OsvVulnerability();
        v.id = "GHSA-h63j-xqx6-w58r";
        v.aliases = List.of("CVE-2023-51079");
        io.quarkus.cve.model.OsvAffected aff = new io.quarkus.cve.model.OsvAffected();
        io.quarkus.cve.model.OsvPackage pkg = new io.quarkus.cve.model.OsvPackage();
        pkg.ecosystem = "Maven";
        pkg.name = "org.mvel:mvel2";
        aff.pkg = pkg;
        aff.versions = List.of("2.5.0.Final");
        v.affected = List.of(aff);

        UnifiedVuln u = UnifiedVuln.fromOsv(v);
        List<UnifiedVuln.AffectedPkg> maven = QuarkusBomService.mavenPackages(u);
        assertEquals(1, maven.size());
        assertEquals(1, maven.get(0).intervals.size());
        assertEquals("2.5.0.Final", maven.get(0).intervals.get(0).introduced);
        assertEquals("2.5.0.Final", maven.get(0).intervals.get(0).lastAffected);
        assertEquals("=2.5.0.Final", ReleaseReportService.range(maven.get(0).intervals.get(0)));
    }

    @Test
    void nonGhsaOsvIdPreservesCveIdAndDeduplicatesAgainstGhsaAlias() {
        OsvVulnerability v = new OsvVulnerability();
        v.id = "CVE-2026-4242";
        v.aliases = List.of("GHSA-aaaa-bbbb-cccc");

        UnifiedVuln fromOsv = UnifiedVuln.fromOsv(v);
        assertEquals("CVE-2026-4242", fromOsv.cveId);
        assertEquals("GHSA-aaaa-bbbb-cccc", QuarkusArtifactService.canonicalKey(fromOsv));
    }

    @Test
    void osvBatchPaginationFollowsNextPageTokens(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = id;
                v.modified = "2026-11-25T00:00:00Z";
                return v;
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                return new OsvQueryResponse();
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                OsvQueryRequest first = request.queries.get(0);
                OsvQueryResponse qr = new OsvQueryResponse();
                OsvVulnerability v = new OsvVulnerability();
                v.modified = "2026-11-25T00:00:00Z";
                if (first.pageToken == null) {
                    v.id = "GHSA-page-1";
                    qr.vulns = List.of(v);
                    qr.nextPageToken = "tok-2";
                } else {
                    assertEquals("tok-2", first.pageToken);
                    v.id = "GHSA-page-2";
                    qr.vulns = List.of(v);
                }
                OsvBatchQueryResponse br = new OsvBatchQueryResponse();
                br.results = List.of(qr);
                return br;
            }
        };

        List<UnifiedVuln> out = service.queryAll(List.of("io.quarkus:quarkus-core"), List.of());
        assertEquals(List.of("GHSA-page-1", "GHSA-page-2"), out.stream().map(v -> v.id).toList());
    }

    @Test
    void githubAdvisoryFollowsCursorLinkAndWithdrawnRemovesOsvEntry(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = "GHSA-withdrawn-in-gh";
                v.modified = "2026-11-25T00:00:00Z";
                return v;
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                return new OsvQueryResponse();
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = "GHSA-withdrawn-in-gh";
                v.modified = "2026-11-25T00:00:00Z";
                OsvQueryResponse qr = new OsvQueryResponse();
                qr.vulns = List.of(v);
                OsvBatchQueryResponse br = new OsvBatchQueryResponse();
                br.results = List.of(qr);
                return br;
            }
        };

        List<String> seenAfter = new java.util.ArrayList<>();
        service.githubAdvisoryClient = (eco, pkg, perPage, after, sort, direction, inm) -> {
            seenAfter.add(after);
            if (after == null) {
                List<io.quarkus.cve.client.GitHubAdvisoryClient.GitHubAdvisory> page1 = new java.util.ArrayList<>();
                for (int i = 0; i < 100; i++) {
                    var a = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubAdvisory();
                    a.ghsaId = "GHSA-p1-" + i;
                    page1.add(a);
                }
                return jakarta.ws.rs.core.Response.ok(page1)
                        .header("Link", "<https://api.github.com/advisories?after=cursor2&per_page=100>; rel=\"next\"")
                        .build();
            }
            var withdrawn = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubAdvisory();
            withdrawn.ghsaId = "GHSA-withdrawn-in-gh";
            withdrawn.withdrawnAt = "2026-11-25T01:00:00Z";
            var page2Only = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubAdvisory();
            page2Only.ghsaId = "GHSA-page2-only";
            return jakarta.ws.rs.core.Response.ok(List.of(withdrawn, page2Only)).build();
        };

        List<UnifiedVuln> out = service.queryAll(List.of("io.quarkus:quarkus-core"), List.of("io.quarkus:quarkus-core"));
        assertEquals(java.util.Arrays.asList(null, "cursor2"), seenAfter);
        assertTrue(out.stream().noneMatch(v -> "GHSA-withdrawn-in-gh".equals(v.id)),
                "GitHub withdrawn advisory must evict un-synced OSV entry");
        assertTrue(out.stream().anyMatch(v -> "GHSA-page2-only".equals(v.id)));
    }

    @Test
    void repeatingOsvPageTokenBreaksLoopWithWarning(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = id;
                v.modified = "2026-11-25T00:00:00Z";
                return v;
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                return new OsvQueryResponse();
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = "GHSA-loop";
                v.modified = "2026-11-25T00:00:00Z";
                OsvQueryResponse qr = new OsvQueryResponse();
                qr.vulns = List.of(v);
                qr.nextPageToken = "stuck-token";
                OsvBatchQueryResponse br = new OsvBatchQueryResponse();
                br.results = List.of(qr);
                return br;
            }
        };

        String err = captureStderr(() -> service.queryAll(List.of("io.quarkus:quarkus-core"), List.of()));
        assertTrue(err.contains("warning: OSV batch pagination stopped"), err);
    }

    @Test
    void quarkusRepoAdvisoriesActAsBackupAndYieldToOsv(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        QuarkusArtifactService service = new QuarkusArtifactService();
        service.osvApiClient = new OsvApiClient() {
            @Override
            public OsvVulnerability getVulnerability(String id) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = "GHSA-reviewed-in-osv";
                v.aliases = List.of("CVE-2026-1000");
                v.modified = "2026-11-25T00:00:00Z";
                io.quarkus.cve.model.OsvAffected aff = new io.quarkus.cve.model.OsvAffected();
                aff.pkg = new io.quarkus.cve.model.OsvPackage("Maven", "io.quarkus:quarkus-oidc");
                io.quarkus.cve.model.OsvRange r = new io.quarkus.cve.model.OsvRange();
                r.type = "ECOSYSTEM";
                io.quarkus.cve.model.OsvVersion e1 = new io.quarkus.cve.model.OsvVersion();
                e1.introduced = "3.0.0";
                io.quarkus.cve.model.OsvVersion e2 = new io.quarkus.cve.model.OsvVersion();
                e2.fixed = "3.39.2";
                r.events = List.of(e1, e2);
                aff.ranges = List.of(r);
                v.affected = List.of(aff);
                return v;
            }

            @Override
            public OsvQueryResponse query(OsvQueryRequest request) {
                return new OsvQueryResponse();
            }

            @Override
            public OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request) {
                OsvVulnerability v = new OsvVulnerability();
                v.id = "GHSA-reviewed-in-osv";
                v.modified = "2026-11-25T00:00:00Z";
                OsvQueryResponse qr = new OsvQueryResponse();
                qr.vulns = List.of(v);
                OsvBatchQueryResponse br = new OsvBatchQueryResponse();
                br.results = List.of(qr);
                return br;
            }
        };
        service.githubAdvisoryClient = (eco, pkg, perPage, after, sort, dir2, inm) ->
                jakarta.ws.rs.core.Response.ok(List.of()).build();
        service.quarkusRepoAdvisoryClient = (state, perPage, after, inm) -> {
            var alreadyInOsv = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubAdvisory();
            alreadyInOsv.ghsaId = "GHSA-reviewed-in-osv";
            alreadyInOsv.cveId = "CVE-2026-1000";
            alreadyInOsv.state = "published";
            var v1 = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubVulnerability();
            v1.pkg = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubPackage();
            v1.pkg.ecosystem = "maven";
            v1.pkg.name = "io.quarkus:quarkus-oidc";
            v1.vulnerableVersionRange = "<9.9.9";
            alreadyInOsv.vulnerabilities = List.of(v1);

            var repoOnly = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubAdvisory();
            repoOnly.ghsaId = "GHSA-repo-only";
            repoOnly.cveId = "CVE-2026-19625";
            repoOnly.state = "published";
            var v2 = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubVulnerability();
            v2.pkg = new io.quarkus.cve.client.GitHubAdvisoryClient.GitHubPackage();
            v2.pkg.ecosystem = "maven";
            v2.pkg.name = "io.quarkus:quarkus-oidc";
            v2.vulnerableVersionRange = "<3.39.2";
            repoOnly.vulnerabilities = List.of(v2);

            return jakarta.ws.rs.core.Response.ok(List.of(alreadyInOsv, repoOnly)).build();
        };

        List<UnifiedVuln> out = service.queryAll(List.of("io.quarkus:quarkus-oidc"), List.of("io.quarkus:quarkus-oidc"));
        assertEquals(2, out.size());
        UnifiedVuln osv = out.stream().filter(v -> "GHSA-reviewed-in-osv".equals(v.id)).findFirst().orElseThrow();
        assertEquals("3.0.0", osv.affected.get(0).intervals.get(0).introduced, "OSV reviewed range must win over repo advisory");
        UnifiedVuln repo = out.stream().filter(v -> "GHSA-repo-only".equals(v.id)).findFirst().orElseThrow();
        assertEquals("3.39.2", repo.affected.get(0).intervals.get(0).fixed);
    }
}

