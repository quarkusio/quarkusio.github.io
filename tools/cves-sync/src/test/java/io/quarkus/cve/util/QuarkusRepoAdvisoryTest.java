package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.cve.client.GitHubAdvisoryClient;
import io.quarkus.cve.model.UnifiedVuln;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuarkusRepoAdvisoryTest {

    @Test
    void parsesReversedOperatorAndBareExactVersions() {
        List<UnifiedVuln.Interval> le = QuarkusRepoAdvisory.parseRanges("=<3.38");
        assertEquals(1, le.size());
        assertNull(le.get(0).introduced);
        assertEquals("3.38", le.get(0).lastAffected);

        List<UnifiedVuln.Interval> bare = QuarkusRepoAdvisory.parseRanges("4.0.0.Beta1");
        assertEquals(1, bare.size());
        assertEquals("4.0.0.Beta1", bare.get(0).introduced);
        assertEquals("4.0.0.Beta1", bare.get(0).lastAffected);

        List<UnifiedVuln.Interval> multi = QuarkusRepoAdvisory.parseRanges("<=3.35.1.1. <=3.27.3.1, 3.38");
        assertEquals(3, multi.size());
        assertEquals("3.35.1.1", multi.get(0).lastAffected);
        assertEquals("3.27.3.1", multi.get(1).lastAffected);
        assertEquals("3.38", multi.get(2).introduced);
        assertEquals("3.38", multi.get(2).lastAffected);
    }

    @Test
    void multipleUnboundedUpperBoundsInferMinorLineLowerBounds() {
        GitHubAdvisoryClient.GitHubAdvisory a = new GitHubAdvisoryClient.GitHubAdvisory();
        a.ghsaId = "GHSA-9qp9-6p5x-rfh4";
        a.cveId = "CVE-2026-93432";
        a.state = "published";
        a.severity = "medium";
        a.vulnerabilities = List.of(
                vuln("io.quarkus.qute:qute-core", "<3.33.4"),
                vuln("io.quarkus.qute:qute-core", "<3.27.6"),
                vuln("io.quarkus.qute:qute-core", "<3.39.5"));

        UnifiedVuln u = QuarkusRepoAdvisory.toUnifiedVuln(a);
        assertNotNull(u);
        assertEquals(1, u.affected.size());
        List<UnifiedVuln.Interval> ivs = u.affected.get(0).intervals;
        assertEquals(3, ivs.size());

        // Find each interval by its fixed version
        UnifiedVuln.Interval i27 = ivs.stream().filter(i -> "3.27.6".equals(i.fixed)).findFirst().orElseThrow();
        UnifiedVuln.Interval i33 = ivs.stream().filter(i -> "3.33.4".equals(i.fixed)).findFirst().orElseThrow();
        UnifiedVuln.Interval i39 = ivs.stream().filter(i -> "3.39.5".equals(i.fixed)).findFirst().orElseThrow();

        assertNull(i27.introduced, "oldest affected line stays open below");
        assertEquals("3.28.0", i33.introduced, "3.33 line starts after 3.27 so fixed 3.27.6 is unaffected");
        assertEquals("3.34.0", i39.introduced, "3.39 line starts after 3.33 so fixed 3.33.4 is unaffected");
    }

    @Test
    void ignoresDraftOrWithdrawnRepoAdvisories() {
        GitHubAdvisoryClient.GitHubAdvisory draft = new GitHubAdvisoryClient.GitHubAdvisory();
        draft.ghsaId = "GHSA-draft";
        draft.state = "draft";
        assertNull(QuarkusRepoAdvisory.toUnifiedVuln(draft));

        GitHubAdvisoryClient.GitHubAdvisory withdrawn = new GitHubAdvisoryClient.GitHubAdvisory();
        withdrawn.ghsaId = "GHSA-withdrawn";
        withdrawn.state = "published";
        withdrawn.withdrawnAt = "2026-11-25T00:00:00Z";
        assertNull(QuarkusRepoAdvisory.toUnifiedVuln(withdrawn));
    }

    private static GitHubAdvisoryClient.GitHubVulnerability vuln(String pkgName, String range) {
        GitHubAdvisoryClient.GitHubVulnerability v = new GitHubAdvisoryClient.GitHubVulnerability();
        v.pkg = new GitHubAdvisoryClient.GitHubPackage();
        v.pkg.ecosystem = "maven";
        v.pkg.name = pkgName;
        v.vulnerableVersionRange = range;
        return v;
    }
}
