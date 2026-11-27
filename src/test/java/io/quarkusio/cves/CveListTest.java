package io.quarkusio.cves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkusio.releases.Releases;
import java.time.LocalDate;
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

    @Test
    void collapsesSiblingEolRootsAndSplitsSupportedFromUnsupported() {
        Releases.Entry r021 = new Releases.Entry("0.21", "2019-08-01", null, "2019-09-01", false, false, false, null,
                null, null, null, null,
                List.of(new Releases.MicroEntry("0.21.0", "2019-08-01", null, "bom", false, false, false, null)));
        Releases.Entry r023 = new Releases.Entry("0.23", "2019-09-25", null, "2019-10-12", false, false, false, null,
                null, null, null, "0.21",
                List.of(new Releases.MicroEntry("0.23.0", "2019-09-25", null, "bom", false, false, false, "0.21.0")));
        Releases.Entry r024 = new Releases.Entry("0.24", "2019-10-12", null, "2019-10-16", false, false, false, null,
                null, null, null, "0.21",
                List.of(new Releases.MicroEntry("0.24.0", "2019-10-12", null, "bom", false, false, false, "0.21.0")));
        Releases.Entry r027 = new Releases.Entry("0.27", "2019-10-30", null, "2019-11-04", false, false, false, null,
                null, null, null, "0.21",
                List.of(new Releases.MicroEntry("0.27.0", "2019-10-30", null, "bom", false, false, false, "0.21.0")));
        Releases.Entry r213 = new Releases.Entry("2.13", "2022-09-28", null, "2023-06-01", true, false, false, null,
                null, null, null, "0.27",
                List.of(
                        new Releases.MicroEntry("2.13.7.Final", "2023-02-01", null, "bom", false, false, false, "2.13.6.Final"),
                        new Releases.MicroEntry("2.13.6.Final", "2023-01-01", null, "bom", false, false, false, "0.27.0")));
        Releases.Entry r327 = new Releases.Entry("3.27", "2025-09-10", null, "2026-09-10", true, false, false, null,
                null, null, null, "0.27",
                List.of(
                        new Releases.MicroEntry("3.27.4", "2026-05-04", null, "bom", false, false, false, "3.27.3"),
                        new Releases.MicroEntry("3.27.3", "2026-04-01", null, "bom", false, false, false, "0.27.0")));
        Releases.Entry r333 = new Releases.Entry("3.33", "2026-03-25", null, "2027-03-25", true, false, false, null,
                null, null, null, "3.27",
                List.of(
                        new Releases.MicroEntry("3.33.2", "2026-05-10", null, "bom", false, false, false, "3.33.1"),
                        new Releases.MicroEntry("3.33.1", "2026-04-15", null, "bom", false, false, false, "3.27.3")));

        Releases releases = new Releases(null, null, List.of(), List.of(r333, r327, r213, r027, r024, r023, r021));
        Cves.Advisory adv = new Cves.Advisory(
                "CVE-2026-39852",
                "HIGH",
                "2026-05-04T17:20:20Z",
                "Historical vulnerability",
                List.of(new Cves.Component("io.quarkus:quarkus-vertx-http", "<3.27.4", true, List.of("0.23.0"))),
                new Cves.AffectedReleases(
                        List.of("0.23.0", "0.24.0", "0.27.0", "2.13.7.Final", "3.27.3", "3.33.1"),
                        List.of(
                                new Cves.Piece("0.23.0", List.of()),
                                new Cves.Piece("0.24.0", List.of()),
                                new Cves.Piece("0.27.0", List.of("3.27.4", "3.33.2")),
                                new Cves.Piece("2.13.7.Final", List.of()))));

        CveList list = new CveList(
                new Cves(Map.of("GHSA-test-39852", adv), List.of(), List.of()),
                releases,
                LocalDate.of(2026, 11, 27));

        CveList.Entry entry = list.entries().get(0);
        // Supported branch 3.33 LTS is reported separately with its fix release
        assertEquals(1, entry.supportedImpacts().size());
        assertEquals("3.33 LTS", entry.supportedImpacts().get(0).branchLabel());
        assertEquals("3.33.1", entry.supportedImpacts().get(0).firstAffected());
        assertEquals(List.of("3.33.2"), entry.supportedImpacts().get(0).fixReleases());
        assertFalse(entry.supportedImpacts().get(0).stillAffected());

        // 0.23.0 and 0.24.0 collapsed into 0.27.0 -> 3.27.4, 3.33.2
        assertEquals(1, entry.unsupportedRanges().size());
        assertEquals("0.23.0", entry.unsupportedRanges().get(0).root());
        assertEquals(List.of("3.27.4", "3.33.2"), entry.unsupportedRanges().get(0).cuts());

        // Standalone EOL backport root 2.13.7.Final goes to eolUnfixedRoots, never "(no fix release yet)"
        assertEquals(List.of("2.13.7.Final"), entry.eolUnfixedRoots());

        // All 6 affected releases were released before 2026-05-04 disclosure, and fixes exist
        assertEquals(6, entry.preDisclosureCount());
        assertEquals(0, entry.postDisclosureCount());
        assertTrue(entry.fixedByDisclosure());
    }
}
