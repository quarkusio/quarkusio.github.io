package io.quarkusio.cves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkusio.releases.Releases;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CveTreeMapTest {

    @Test
    void branchesPeelOffLeftToRightAndMapInterchangesAndAdvisories() {
        Releases.Entry r326 = new Releases.Entry(
                "3.26", "2025-08-01", null, "2025-10-01", false, false, false, null,
                null, null, null, null,
                List.of(
                        new Releases.MicroEntry("3.26.3", "2025-09-12", null, "bom", false, false, false, "3.26.2"),
                        new Releases.MicroEntry("3.26.2", "2025-09-01", null, "bom", false, false, false, "3.26.1"),
                        new Releases.MicroEntry("3.26.1", "2025-08-15", null, "bom", false, false, false, "3.26.0"),
                        new Releases.MicroEntry("3.26.0", "2025-08-01", null, "bom", false, false, false, null)));
        Releases.Entry r327 = new Releases.Entry(
                "3.27", "2025-09-10", null, "2026-09-10", true, false, false, null,
                null, null, null, "3.26",
                List.of(
                        new Releases.MicroEntry("3.27.1", "2025-09-25", null, "bom", false, false, false, "3.27.0"),
                        new Releases.MicroEntry("3.27.0", "2025-09-10", null, "bom", false, false, false, "3.26.2")));
        Releases.Entry r328 = new Releases.Entry(
                "3.28", "2025-10-01", null, "2025-11-01", false, false, false, null,
                null, null, null, "3.26",
                List.of(
                        new Releases.MicroEntry("3.28.1", "2025-10-15", null, "bom", false, false, false, "3.28.0"),
                        new Releases.MicroEntry("3.28.0", "2025-10-01", null, "bom", false, false, false, "3.26.0")));

        Releases releases = new Releases(null, null, List.of(), List.of(r328, r327, r326));
        Cves.Advisory adv = new Cves.Advisory(
                "CVE-2025-9999",
                "HIGH",
                "2025-09-12T00:00:00Z",
                "Test CVE",
                List.of(new Cves.Component("io.quarkus:quarkus-core", "<3.27.1", true, List.of("3.26.1", "3.26.2", "3.27.0"))),
                new Cves.AffectedReleases(
                        List.of("3.26.1", "3.26.2", "3.27.0"),
                        List.of(new Cves.Piece("3.26.1", List.of("3.27.1")))));
        Cves cves = new Cves(
                Map.of("GHSA-test-0001-0001", adv),
                List.of(),
                List.of(
                        new Cves.Scanned("3.26.0"),
                        new Cves.Scanned("3.26.1"),
                        new Cves.Scanned("3.26.2"),
                        new Cves.Scanned("3.26.3"),
                        new Cves.Scanned("3.27.0"),
                        new Cves.Scanned("3.27.1"),
                        new Cves.Scanned("3.28.0"),
                        new Cves.Scanned("3.28.1")));

        CveTreeMap map = new CveTreeMap(releases, cves, new CveList(cves));
        CveTreeMap.TreePayload payload = map.payload();

        assertEquals(8, payload.stations().size());
        assertEquals(3, payload.branches().size());
        assertEquals(1, payload.advisories().size());

        Map<String, CveTreeMap.Station> byVer = payload.stations().stream()
                .collect(Collectors.toMap(CveTreeMap.Station::version, s -> s));

        // Every child sits strictly to the right of its parent (earliest on the left)
        for (CveTreeMap.Edge edge : payload.edges()) {
            assertTrue(byVer.get(edge.to()).col() > byVer.get(edge.from()).col(),
                    "Child " + edge.to() + " must be right of parent " + edge.from());
        }

        // 3.26.2 is an interchange where 3.26.3 and 3.27 LTS diverge onto different lanes
        assertTrue(byVer.get("3.26.2").interchange());
        assertNotEquals(byVer.get("3.26.3").lane(), byVer.get("3.27.0").lane());
        assertTrue(byVer.get("3.27.0").lts());

        // Zero-crossing invariant: no vertical drop from (fromCol, fromLane) -> toLane may cross any
        // horizontal edge spanning across fromCol on an intermediate lane.
        for (CveTreeMap.Edge drop : payload.edges()) {
            CveTreeMap.Station from = byVer.get(drop.from());
            CveTreeMap.Station to = byVer.get(drop.to());
            if (from.lane() == to.lane()) continue;
            int minL = Math.min(from.lane(), to.lane());
            int maxL = Math.max(from.lane(), to.lane());
            for (CveTreeMap.Edge horiz : payload.edges()) {
                CveTreeMap.Station hf = byVer.get(horiz.from());
                CveTreeMap.Station ht = byVer.get(horiz.to());
                if (hf.lane() == ht.lane() && hf.lane() > minL && hf.lane() < maxL) {
                    assertTrue(ht.col() <= from.col() || hf.col() > from.col(),
                            "Drop " + drop.from() + "->" + drop.to() + " crosses " + horiz.from() + "->" + horiz.to());
                }
            }
        }

        CveTreeMap.AdvisoryOverlay overlay = payload.advisories().get(0);
        assertEquals(List.of("3.26.1"), overlay.roots());
        assertEquals(List.of("3.27.1"), overlay.cuts());
    }
}
