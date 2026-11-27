package io.quarkusio.cves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CveListTest {

    @Test
    void combinesAdvisoriesAndPendingSortedNewestFirstWithLowercaseAnchors() {
        Cves.Advisory older = new Cves.Advisory(
                "CVE-2024-1597",
                "CRITICAL",
                "2024-02-21T23:33:43Z",
                "PostgreSQL SQL injection",
                List.of(new Cves.Component("org.postgresql:postgresql", "<42.2.28", null, List.of("42.2.5"))),
                new Cves.AffectedReleases(List.of("3.2.0", "3.2.1"), List.of(new Cves.Piece("3.2.0", List.of("3.2.2")))));

        Cves.Advisory ghsaOnly = new Cves.Advisory(
                null,
                "HIGH",
                "2025-05-10T10:00:00Z",
                "Quarkus core issue without CVE",
                List.of(new Cves.Component("io.quarkus:quarkus-core", "<3.20.1", true, List.of("3.20.0"))),
                new Cves.AffectedReleases(List.of("3.20.0"), List.of(new Cves.Piece("3.20.0", List.of("3.20.1")))));

        Cves.Pending newest = new Cves.Pending(
                "CVE-2026-127106",
                "MEDIUM",
                "2026-11-20T17:46:38Z",
                "Deeply nested JSON-B body",
                List.of("io.quarkus:quarkus-rest-jsonb"),
                "https://www.cve.org/CVERecord?id=CVE-2026-127106");

        CveList list = new CveList(new Cves(
                Map.of("GHSA-24rp-q3w6-vc56", older, "GHSA-aaaa-bbbb-cccc", ghsaOnly),
                List.of(newest),
                List.of()));

        assertEquals(3, list.totalCount());
        assertEquals(2, list.advisoryCount());
        assertEquals(1, list.pendingCount());
        assertEquals(2, list.coreCount());

        // Sorted newest first
        assertEquals("CVE-2026-127106", list.entries().get(0).primaryId());
        assertTrue(list.entries().get(0).pending());
        assertTrue(list.entries().get(0).core());
        assertEquals("cve-2026-127106", list.entries().get(0).anchorId());
        assertNull(list.entries().get(0).secondaryAnchorId());

        assertEquals("GHSA-aaaa-bbbb-cccc", list.entries().get(1).primaryId());
        assertEquals("ghsa-aaaa-bbbb-cccc", list.entries().get(1).anchorId());
        assertNull(list.entries().get(1).secondaryAnchorId());
        assertTrue(list.entries().get(1).core());

        assertEquals("CVE-2024-1597", list.entries().get(2).primaryId());
        assertEquals("cve-2024-1597", list.entries().get(2).anchorId());
        assertEquals("ghsa-24rp-q3w6-vc56", list.entries().get(2).secondaryAnchorId());
        assertFalse(list.entries().get(2).core());
        assertEquals("21 Feb 2024", list.entries().get(2).publishedDisplay());
        assertEquals("https://www.cve.org/CVERecord?id=CVE-2024-1597", list.entries().get(2).cveLink());
        assertEquals("https://github.com/advisories/GHSA-24rp-q3w6-vc56", list.entries().get(2).ghsaLink());
    }
}
