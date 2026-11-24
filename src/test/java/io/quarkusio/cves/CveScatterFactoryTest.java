package io.quarkusio.cves;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkusio.charts.scatter.ScatterMark;
import io.quarkusio.charts.scatter.ScatterPlot;
import io.quarkusio.charts.scatter.ScatterRow;
import org.junit.jupiter.api.Test;

class CveScatterFactoryTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    static Cves.Advisory adv(String cve, String severity, String published, String... artifacts) {
        return new Cves.Advisory(cve, severity, published, "Summary of " + cve,
                java.util.Arrays.stream(artifacts).map(Cves.Component::new).toList(), null);
    }

    /** A release line: each entry is a release date followed by the advisories still affecting it. */
    record Line(List<String[]> releases) {
    }

    static Line line(String[]... releases) {
        return new Line(List.of(releases));
    }

    static String[] rel(String... dateAndIds) {
        return dateAndIds;
    }

    /**
     * Builds the release tree from the lines (each release is built on the one before it in its line), and lists
     * on every advisory the releases whose entry names it.
     */
    static CveScatterFactory factory(Map<String, Cves.Advisory> advisories, List<Cves.Pending> pending,
            Line... lines) {
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        Map<String, String> parents = new LinkedHashMap<>();
        Map<String, List<String>> affected = new LinkedHashMap<>();
        for (int l = 0; l < lines.length; l++) {
            String previous = null;
            for (int r = 0; r < lines[l].releases().size(); r++) {
                String[] entry = lines[l].releases().get(r);
                String version = "line" + l + "." + r;
                dates.put(version, LocalDate.parse(entry[0]));
                if (previous != null) {
                    parents.put(version, previous);
                }
                previous = version;
                for (int k = 1; k < entry.length; k++) {
                    affected.computeIfAbsent(entry[k], x -> new ArrayList<>()).add(version);
                }
            }
        }
        Map<String, Cves.Advisory> withReleases = new LinkedHashMap<>();
        advisories.forEach((ghsa, a) -> withReleases.put(ghsa,
                new Cves.Advisory(a.cve(), a.severity(), a.published(), a.summary(), a.affectedComponents(),
                        new Cves.AffectedReleases(affected.getOrDefault(ghsa, List.of()), null))));
        return new CveScatterFactory(new Cves(withReleases, pending, null), new Lineage(dates, parents));
    }

    static CveScatterFactory factory(Map<String, Cves.Advisory> advisories) {
        return factory(advisories, List.of());
    }

    static List<ScatterMark> marks(CveScatterFactory f) {
        return f.build("all", TODAY, 1).rows().get(0).marks();
    }

    @Test
    void dotViewPutsEachCveAtItsPublicationDateAndKeepsOpenOnesCrossed() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"),
                "GHSA-2", adv("CVE-2", "LOW", "2026-04-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-01-05", "GHSA-1"), rel("2026-03-20", "GHSA-2"), rel("2026-05-01", "GHSA-2")));
        ScatterPlot dots = f.build("d", TODAY, 1, true);
        Map<String, ScatterMark> byCve = new java.util.HashMap<>();
        dots.rows().get(0).marks().forEach(mk -> byCve.put(mk.title().split(" ")[0], mk));
        ScatterMark fixed = byCve.get("CVE-1");
        assertTrue(fixed.dot());
        assertFalse(fixed.open());
        assertEquals(LocalDate.of(2026, 3, 10), fixed.start());
        assertEquals(fixed.start(), fixed.found());
        assertEquals(fixed.start(), fixed.end());
        assertFalse(fixed.sinceAtLeast(), "No arrow on a dot");
        assertTrue(byCve.get("CVE-2").open());
        // the time span starts at the earliest publication, not at the first release that had the CVE
        assertEquals(LocalDate.of(2026, 2, 1), dots.scale().start());
    }

    @Test
    void responsivenessViewRunsOnlyFromPublicationToTheFix() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"),
                "GHSA-2", adv("CVE-2", "LOW", "2026-04-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-01-05", "GHSA-1"), rel("2026-03-20", "GHSA-2"), rel("2026-05-01", "GHSA-2")));
        ScatterPlot plot = f.build("r", TODAY, 1, CveScatterFactory.Mode.RESPONSIVENESS);
        Map<String, ScatterMark> byCve = new java.util.HashMap<>();
        plot.rows().get(0).marks().forEach(mk -> byCve.put(mk.title().split(" ")[0], mk));
        ScatterMark fixed = byCve.get("CVE-1");
        assertFalse(fixed.dot());
        assertEquals(LocalDate.of(2026, 3, 10), fixed.start(), "Starts at publication, not at the first release with it");
        assertEquals(fixed.start(), fixed.found());
        assertEquals("0.000", fixed.preWPct(), "Nothing before publication");
        assertFalse(fixed.sinceAtLeast());
        assertFalse(fixed.open());
        assertTrue(byCve.get("CVE-2").open(), "Not fixed yet still ends in a cross");
        assertEquals(LocalDate.of(2026, 2, 1), plot.scale().start(), "The span starts at the earliest publication");
        assertTrue(f.build("e", TODAY, 1).rows().get(0).marks().stream().anyMatch(mk -> mk.start().isBefore(mk.found())),
                "The exposure view still has the time before publication");
    }

    @Test
    void barRunsFromFirstReleaseThroughPublicationToFirstReleaseWithoutIt() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-01-05", "GHSA-1"), rel("2026-02-05", "GHSA-1"), rel("2026-03-05", "GHSA-1"),
                        rel("2026-03-20")));
        ScatterMark mark = marks(f).get(0);
        assertFalse(mark.pending());
        assertEquals(LocalDate.of(2026, 1, 5), mark.start());
        assertEquals(LocalDate.of(2026, 3, 10), mark.found());
        assertEquals(LocalDate.of(2026, 3, 20), mark.end());
        assertFalse(mark.open());
        assertTrue(mark.hasPre(), "the part before publication is drawn separately");
    }

    @Test
    void fixInAnyLineCounts_earliestWins() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-03-01", "GHSA-1"), rel("2026-04-20")),
                line(rel("2026-03-02", "GHSA-1"), rel("2026-03-25")));
        assertEquals(LocalDate.of(2026, 3, 25), marks(f).get(0).end());
    }

    @Test
    void releaseBeforePublicationIsNotTheFix() {
        // a release that dropped it before it was published does not count; the next one that does, does
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-03-01", "GHSA-1"), rel("2026-03-05"), rel("2026-03-15", "GHSA-1"), rel("2026-03-30")));
        assertEquals(LocalDate.of(2026, 3, 30), marks(f).get(0).end());
    }

    @Test
    void notYetFixedIsOpenAndRunsToToday() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-10-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(), line(rel("2026-10-01", "GHSA-1"), rel("2026-11-01", "GHSA-1")));
        ScatterMark mark = marks(f).get(0);
        assertTrue(mark.open());
        assertEquals(TODAY, mark.end());
        assertTrue(mark.details().stream().anyMatch(d -> d.startsWith("Not fixed yet")));
    }

    @Test
    void releasedAfterPublicationHasNoPaleStart() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(), line(rel("2026-04-01", "GHSA-1"), rel("2026-04-10")));
        ScatterMark mark = marks(f).get(0);
        assertEquals(LocalDate.of(2026, 3, 10), mark.start());
        assertFalse(mark.hasPre());
    }

    @Test
    void pendingRunsFromPublicationToTodayAndEndsInACross() {
        var pending = List.of(new Cves.Pending("CVE-9", "MEDIUM", "2026-11-03T04:16:03Z", "Pending one",
                List.of("io.quarkus:quarkus-vertx-http"), "https://www.cve.org/CVERecord?id=CVE-9"));
        var f = factory(Map.of(), pending);
        ScatterRow row = f.build("all", TODAY, 1).rows().get(0);
        assertEquals("quarkus-vertx-http", row.id(), "the io.quarkus group is dropped, as in the advisories");
        ScatterMark mark = row.marks().get(0);
        assertTrue(mark.pending());
        assertTrue(mark.open());
        assertEquals(TODAY, mark.end());
        assertTrue(mark.hasPre(), "a fade stands for not knowing when it first shipped");
        assertEquals(LocalDate.of(2026, 11, 3).minusDays(CveScatterFactory.FADE_DAYS), mark.start());
        assertEquals(LocalDate.of(2026, 11, 3), mark.found());
        assertEquals("medium", mark.kind());
        assertEquals(1, f.pendingCount());
    }

    @Test
    void pendingMarksOverlappingInTimeGetSeparateLanes() {
        var pending = List.of(
                new Cves.Pending("CVE-8", "HIGH", "2026-11-03T04:16:03Z", "One", List.of("a:one"), null),
                new Cves.Pending("CVE-9", "LOW", "2026-11-05T04:16:03Z", "Two", List.of("a:one"), null));
        ScatterRow row = factory(Map.of(), pending).build("all", TODAY, 1).rows().get(0);
        assertEquals(2, row.lanes());
        assertEquals("https://www.cve.org/CVERecord?id=CVE-8", row.marks().get(0).href());
    }

    @Test
    void aPendingFadeIsKeptClearOfEarlierBarsInTheSameLane() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-09-01T10:00:00Z", "a:one"));
        var pending = List.of(new Cves.Pending("CVE-9", "LOW", "2026-11-03T04:16:03Z", "P", List.of("a:one"), null));
        // the fixed bar ends 2026-09-10, which is within the fade's 120 days before 2026-11-03
        var f = factory(m, pending, line(rel("2026-08-20", "GHSA-1"), rel("2026-09-10")));
        assertEquals(2, f.build("all", TODAY, 1).rows().get(0).lanes());
    }

    @Test
    void paleStartsOfOverlappingCvesAreInLanesOfTheirOwn() {
        // all in Quarkus since January, published and fixed at different times: the bars, pale parts included,
        // overlap from January on, so each needs its own lane
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", "LOW", "2026-03-10T10:00:00Z", "a:one"));
        m.put("GHSA-2", adv("CVE-2", "HIGH", "2026-05-10T10:00:00Z", "a:one"));
        m.put("GHSA-3", adv("CVE-3", "HIGH", "2026-07-10T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-01-05", "GHSA-1", "GHSA-2", "GHSA-3"), rel("2026-03-20", "GHSA-2", "GHSA-3"),
                        rel("2026-05-20", "GHSA-3"), rel("2026-07-20")));
        ScatterRow row = f.build("all", TODAY, 1).rows().get(0);
        assertEquals(3, row.lanes());
        assertEquals(3, row.marks().stream().map(ScatterMark::lane).distinct().count());
        assertTrue(row.marks().stream().allMatch(ScatterMark::hasPre));
    }

    @Test
    void overlappingMarksOfOneArtifactGetTheirOwnLanes() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", "LOW", "2026-03-10T10:00:00Z", "a:one"));
        m.put("GHSA-2", adv("CVE-2", "CRITICAL", "2026-03-12T10:00:00Z", "a:one"));
        m.put("GHSA-3", adv("CVE-3", "HIGH", "2026-08-01T10:00:00Z", "a:one"));
        var f = factory(m, List.of(),
                line(rel("2026-03-01", "GHSA-1", "GHSA-2"), rel("2026-04-01"), rel("2026-07-20", "GHSA-3"),
                        rel("2026-08-20")));
        ScatterRow row = f.build("all", TODAY, 1).rows().get(0);
        assertEquals(2, row.lanes());
        List<ScatterMark> marks = row.marks();
        assertNotEquals(marks.get(0).lane(), marks.get(1).lane());
        assertEquals(0, marks.get(2).lane(), "a later, separate mark reuses the first lane");
        assertEquals(ScatterRow.heightFor(row.lanes()), row.height());
        assertNotEquals(marks.get(0).y(), marks.get(1).y());
    }

    // ---- "since at least": the data only goes back so far

    @Test
    void aCveAlreadyInTheEarliestReleaseInTheDataIsFlaggedAsSinceAtLeast() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:old"));
        m.put("GHSA-2", adv("CVE-2", "HIGH", "2026-06-10T10:00:00Z", "a:new"));
        var f = factory(m, List.of(),
                line(rel("2026-01-05", "GHSA-1"), rel("2026-02-05", "GHSA-1", "GHSA-2"), rel("2026-03-20"),
                        rel("2026-07-01")));
        Map<String, ScatterMark> byTitle = new LinkedHashMap<>();
        f.build("exposure", TODAY, 1).rows().forEach(r -> r.marks().forEach(mk -> byTitle.put(mk.title().split(" ")[0], mk)));
        assertTrue(byTitle.get("CVE-1").sinceAtLeast(), "in the very first release we have");
        assertFalse(byTitle.get("CVE-2").sinceAtLeast(), "it arrived in a later release, so that is when it started");
        assertTrue(byTitle.get("CVE-1").details().stream()
                .anyMatch(d -> d.startsWith("In releases since at least 05 Jan 2026")), "" + byTitle.get("CVE-1").details());
        assertTrue(byTitle.get("CVE-2").details().stream().anyMatch(d -> d.startsWith("In releases since 05 Feb 2026")));
    }

    @Test
    void theEarliestReleaseIsWhicheverTheDataStartsAtSoGoingBackMovesTheFlag() {
        // the data now also covers an older line, back to October 2025: only what was in that is flagged
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        m.put("GHSA-2", adv("CVE-2", "HIGH", "2026-04-10T10:00:00Z", "a:two"));
        var newer = line(rel("2026-01-05", "GHSA-1", "GHSA-2"), rel("2026-04-20"));
        var older = line(rel("2025-10-01", "GHSA-1"), rel("2026-01-02", "GHSA-1"), rel("2026-03-25"));

        Map<String, Boolean> before = new LinkedHashMap<>();
        factory(m, List.of(), newer).build("exposure", TODAY, 1).rows()
                .forEach(r -> r.marks().forEach(mk -> before.put(mk.title().split(" ")[0], mk.sinceAtLeast())));
        assertEquals(Map.of("CVE-1", true, "CVE-2", true), before);

        Map<String, Boolean> after = new LinkedHashMap<>();
        factory(m, List.of(), newer, older).build("exposure", TODAY, 1).rows()
                .forEach(r -> r.marks().forEach(mk -> after.put(mk.title().split(" ")[0], mk.sinceAtLeast())));
        assertEquals(Map.of("CVE-1", true, "CVE-2", false), after,
                "CVE-2 only arrived in the newer line, so it is no longer at the edge of what we know");
    }

    @Test
    void pendingAndUnpaleCvesAreNeverFlagged() {
        var pending = List.of(new Cves.Pending("CVE-9", "LOW", "2026-11-03T04:16:03Z", "P", List.of("a:one"), null));
        var f = factory(Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-01-02T10:00:00Z", "a:one")), pending,
                line(rel("2026-01-05", "GHSA-1"), rel("2026-02-01")));
        // CVE-1 was published before the earliest release, so it has no paler part to put an arrow on
        f.build("exposure", TODAY, 1).rows().forEach(r -> r.marks().forEach(mk -> assertFalse(mk.sinceAtLeast(), mk.title())));
    }

    @Test
    void theLegendOnlyExplainsTheArrowWhenSomethingHasOne() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        var with = factory(m, List.of(), line(rel("2026-01-05", "GHSA-1"), rel("2026-03-20")));
        assertTrue(with.legend().stream().anyMatch(l -> l.key().equals("since")));
        assertTrue(factory(m).legend().stream().noneMatch(l -> l.key().equals("since")), "nothing to explain");
    }

    @Test
    void aSingleLaneRowKeepsTheMinimumHeight() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        assertEquals(ScatterRow.MIN_HEIGHT, factory(m).build("all", TODAY, 1).rows().get(0).height());
    }

    @Test
    void oneRowPerArtifactMostCvesFirst() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", "HIGH", "2026-01-10T10:00:00Z", "a:one", "a:two"));
        m.put("GHSA-2", adv("CVE-2", "LOW", "2026-03-10T10:00:00Z", "a:two"));
        ScatterPlot plot = factory(m).build("all", TODAY, 1);
        assertEquals(List.of("a:two", "a:one"), plot.rows().stream().map(ScatterRow::id).toList());
        assertEquals(2, plot.rows().get(0).marks().size());
    }

    @Test
    void artifactListedOncePerAdvisory() {
        // the data repeats an artifact once per dependency version
        Map<String, Cves.Advisory> m = Map.of("GHSA-1",
                adv("CVE-1", "HIGH", "2026-01-10T10:00:00Z", "a:one", "a:one", "a:one"));
        assertEquals(1, marks(factory(m)).size());
    }

    @Test
    void artifactsWithFewCvesAreInThePageButNotShownAndTakeNoSpace() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            m.put("GHSA-" + i, adv("CVE-" + i, "HIGH", "2026-0" + (i + 1) + "-10T10:00:00Z", "a:busy"));
        }
        m.put("GHSA-x", adv("CVE-x", "HIGH", "2026-05-10T10:00:00Z", "a:quiet"));
        var f = factory(m);
        ScatterPlot some = f.build("exposure", TODAY, 3);
        assertEquals(List.of("a:busy", "a:quiet"), some.rows().stream().map(ScatterRow::id).toList(),
                "the slider needs every row in the page");
        assertEquals(List.of(true, false), some.rows().stream().map(ScatterRow::shown).toList());
        assertEquals(3, some.rows().get(0).count());

        ScatterPlot all = f.build("exposure", TODAY, 1);
        assertTrue(all.rows().stream().allMatch(ScatterRow::shown));
        assertEquals(all.rowY(1) + all.rows().get(1).height(), all.rowY(2));
        assertEquals(some.rowY(1), some.rowY(2), "a row that is not shown takes no space");
        assertTrue(some.height() < all.height());
    }

    @Test
    void theSliderStartsAtSixOrMoreCves() {
        assertEquals(6, CveScatterFactory.MIN_FOR_TOP_VIEW);
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            m.put("GHSA-a" + i, adv("CVE-a" + i, "HIGH", "2026-0" + (i + 1) + "-10T10:00:00Z", "a:many"));
        }
        for (int i = 0; i < 4; i++) {
            m.put("GHSA-b" + i, adv("CVE-b" + i, "HIGH", "2026-0" + (i + 1) + "-10T10:00:00Z", "a:four"));
        }
        var f = factory(m);
        assertEquals(7, f.maxCount());
        assertEquals(6, f.defaultMin());
        ScatterPlot plot = f.plot(TODAY.atStartOfDay());
        assertEquals(List.of("a:many", "a:four"), plot.rows().stream().map(ScatterRow::id).toList());
        assertEquals(List.of(true, false), plot.rows().stream().map(ScatterRow::shown).toList());
    }

    @Test
    void theSliderStartIsNeverBeyondItsTopForSmallData() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2026-03-10T10:00:00Z", "a:one"));
        var f = factory(m);
        assertEquals(1, f.maxCount());
        assertEquals(1, f.defaultMin());
        assertTrue(f.plot(TODAY.atStartOfDay()).rows().get(0).shown());
        assertEquals(1, factory(Map.of()).maxCount(), "no data still gives a usable slider");
    }

    @Test
    void ignoresAdvisoriesWithoutUsableSeverityOrDate() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", null, "2026-01-10T10:00:00Z", "a:one"));
        m.put("GHSA-2", adv("CVE-2", "HIGH", null, "a:one"));
        m.put("GHSA-3", adv("CVE-3", "WEIRD", "2026-01-10T10:00:00Z", "a:one"));
        assertEquals(0, factory(m).advisoryCount());
    }

    @Test
    void linksToCveRecordOrFallsBackToGithubAdvisory() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        m.put("GHSA-1", adv("CVE-1", "HIGH", "2026-01-10T10:00:00Z", "a:one"));
        m.put("GHSA-2", adv(null, "HIGH", "2026-06-10T10:00:00Z", "a:one"));
        List<ScatterMark> marks = marks(factory(m));
        assertEquals("https://www.cve.org/CVERecord?id=CVE-1", marks.get(0).href());
        assertEquals("https://github.com/advisories/GHSA-2", marks.get(1).href());
    }

    @Test
    void scaleStartsTheMonthBeforeTheEarliestBarAndEndsTheMonthAfterNext() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1", adv("CVE-1", "HIGH", "2025-03-17T10:00:00Z", "a:one"));
        var f = factory(m, List.of(), line(rel("2025-02-20", "GHSA-1"), rel("2025-04-01")));
        ScatterPlot plot = f.build("all", TODAY, 1);
        assertEquals(LocalDate.of(2025, 1, 1), plot.scale().start());
        assertEquals(LocalDate.of(2027, 1, 31), plot.scale().end());
    }

    @Test
    void theSpanIsThatOfTheRowsShown() {
        Map<String, Cves.Advisory> m = new LinkedHashMap<>();
        // the busy artifact only starts in mid 2026; a quiet one, shown only once the slider is down, goes back to 2024
        for (int i = 0; i < 3; i++) {
            m.put("GHSA-" + i, adv("CVE-" + i, "HIGH", "2026-0" + (i + 5) + "-10T10:00:00Z", "a:busy"));
        }
        m.put("GHSA-old", adv("CVE-old", "LOW", "2024-10-08T10:00:00Z", "a:quiet"));
        var f = factory(m);
        ScatterPlot some = f.build("exposure", TODAY, 3);
        ScatterPlot all = f.build("exposure", TODAY, 1);
        assertEquals(LocalDate.of(2024, 9, 1), all.scale().start());
        assertEquals(LocalDate.of(2026, 4, 1), some.scale().start(), "a month before the earliest bar shown");
        assertEquals(all.scale().end(), some.scale().end());
    }

    @Test
    void tooltipNeverContainsTheLineSeparator() {
        Map<String, Cves.Advisory> m = Map.of("GHSA-1",
                new Cves.Advisory("CVE-1", "HIGH", "2026-01-10T10:00:00Z", "a | b", List.of(new Cves.Component("a:one")), null));
        ScatterMark mark = marks(factory(m)).get(0);
        assertEquals(mark.details().size(), mark.tooltip().split("\\|").length);
    }

    @Test
    void theDataBindsWithRoqsSnakeCaseMapper() throws Exception {
        // Roq reads data files with a snake_case mapper, but cves.yaml is camelCase: without explicit names every
        // advisory loses its components and releases, and the page silently shows only the pending CVEs
        String json = """
                {"advisories": {"GHSA-1": {"cve": "CVE-1", "severity": "HIGH", "published": "2026-03-10T10:00:00Z",
                  "summary": "S", "affectedComponents": [{"component": "a:one", "range": "<1"}],
                  "affectedReleases": {"list": ["v1"], "tree": [{"root": "v1", "cuts": ["v2"]}]}}},
                 "releases": [{"version": "v1"}]}""";
        Cves cves = new com.fasterxml.jackson.databind.ObjectMapper()
                .setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE)
                .readValue(json, Cves.class);
        Cves.Advisory a = cves.advisories().get("GHSA-1");
        assertEquals("a:one", a.affectedComponents().get(0).component());
        assertEquals(List.of("v1"), a.affectedReleases().list());
        assertEquals("v2", a.affectedReleases().tree().get(0).cuts().get(0));
        assertEquals("v1", cves.releases().get(0).version());
    }

    @Test
    void theEarliestScannedReleaseBoundsSinceNotTheEarliestReleaseInTheLineage() {
        // releases.yaml also lists releases too old to scan; they must not move the edge of what we know
        Map<String, LocalDate> dates = Map.of("old", LocalDate.of(2018, 12, 1), "v1", LocalDate.of(2019, 3, 6), "v2",
                LocalDate.of(2019, 4, 1));
        Map<String, String> parents = Map.of("v1", "old", "v2", "v1");
        var adv = new Cves.Advisory("CVE-1", "HIGH", "2020-01-01T00:00:00Z", "S", List.of(new Cves.Component("a:one")),
                new Cves.AffectedReleases(List.of("v1"), null));
        var f = new CveScatterFactory(
                new Cves(Map.of("GHSA-1", adv), List.of(), List.of(new Cves.Scanned("v1"), new Cves.Scanned("v2"))),
                new Lineage(dates, parents));
        ScatterMark mark = marks(f).get(0);
        assertTrue(mark.sinceAtLeast());
        assertEquals(LocalDate.of(2019, 3, 6), mark.start());
        assertTrue(f.legend().stream().anyMatch(l -> l.key().equals("since")), "the arrow is explained");
    }
}
