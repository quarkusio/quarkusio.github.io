package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StatsTest {

    @AfterEach
    void clean() {
        Stats.reset();
    }

    @Test
    void requestsAreGroupedByHostAndPurpose() {
        Stats.reset();
        Stats.httpRequest(URI.create("https://api.github.com/search/code?q=quarkus&page=1"));
        Stats.httpRequest(URI.create("https://api.github.com/search/code?q=quarkus&page=2"));
        Stats.rejected(URI.create("https://api.github.com/search/code?q=quarkus&page=2"), true);
        Stats.httpRequest(URI.create("https://api.github.com/advisories?affects=a,b"));
        Stats.notModified(URI.create("https://api.github.com/advisories?affects=a,b"));
        Stats.httpRequest(URI.create("https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/3.1.0/quarkus-bom-3.1.0.pom"));
        Stats.httpRequest(URI.create("https://api.osv.dev/v1/vulns/GHSA-1"));
        Stats.cacheHit(Stats.OSV);

        List<String> lines = Stats.summaryLines();
        String all = String.join("\n", lines);
        assertTrue(all.contains("api.github.com"), all);
        assertTrue(lines.stream().anyMatch(l -> l.contains("code search for CVE records") && l.contains("2 /    0 /   1 /   0")), all);
        assertTrue(lines.stream().anyMatch(l -> l.contains("advisories of packages") && l.contains("1 /    1 /   0 /   0")), all);
        assertTrue(all.contains("platform BOM"), all);
        assertTrue(all.contains("vulnerability details"), all);
        assertTrue(all.contains("total 5 requests, 1 not modified, 1 rate limited, 0 failed"), all);
        assertTrue(all.contains("answered from the cache without a request: 1 (osv=1)"), all);
    }

    @Test
    void aRunThatAskedForNothingSaysSo() {
        Stats.reset();
        assertEquals("  answered from the cache without a request: 0", Stats.summaryLines().get(Stats.summaryLines().size() - 1));
    }
}
