package io.quarkusio.releases;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.quarkusio.charts.timeline.Timeline;
import io.quarkusio.charts.timeline.gantt.TimelineRow;
import io.quarkusio.charts.timeline.gantt.GanttTimelineSegment;

class ReleaseTimelineFactoryTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    static final String YAML_AS_JSON = """
            {
              "support_providers": {
                "rhbq": {"url": "https://rh.example"},
                "ibm": {"url": "https://ibm.example"},
                "herodevs": {"url": "https://hd.example"}
              },
              "releases": [
                {"version": "4.0.0", "lts": false, "upcoming": true},
                {"version": "3.40", "release_date": "2026-09-30", "eol_date": "2027-09-30", "lts": true,
                 "latest_patch": "3.40.1.1", "latest_patch_date": "2026-10-19", "link": "/blog/3-40/"},
                {"version": "3.39", "release_date": "2026-08-26", "eol_date": "2026-09-30", "lts": false},
                {"version": "3.38", "release_date": "2026-07-22", "eol_date": "2026-08-26", "lts": false},
                {"version": "3.33", "release_date": "2026-03-25", "eol_date": "2027-03-25", "lts": true,
                 "rhbq": {"eol_date": "2028-06-30"}, "ibm": {"eol_date": "2028-06-30"}},
                {"version": "3.20", "release_date": "2025-03-26", "eol_date": "2026-03-28", "lts": true,
                 "rhbq": {"eol_date": "2027-06-30"}, "herodevs": {"eol_date": "nes"}},
                {"version": "3.50", "release_date": "2026-10-01", "lts": false},
                {"version": "2.16", "release_date": "2023-01-01", "eol_date": "2023-06-01", "lts": true,
                 "rhbq": {"eol_date": "2023-07-01"}},
                {"version": "0.2", "release_date": "2019-03-01", "eol_date": "2019-03-08", "lts": false}
              ]
            }
            """;

    static final ObjectMapper MAPPER = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    public static final int WEEKS_TO_ADD = 4;

    /** Binds like Roq does (plain mapper, snake_case naming strategy): snake_case keys onto the Releases record. */
    static ReleaseCatalog catalogOf(String json) {
        try {
            return new ReleaseCatalog(MAPPER.readValue(json, Releases.class));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    final ReleaseCatalog catalog = catalogOf(YAML_AS_JSON);
    final ReleaseTimelineFactory factory = new ReleaseTimelineFactory(catalog);

    static List<String> kinds(TimelineRow row) {
        return row.segments().stream().map(GanttTimelineSegment::kind).collect(Collectors.toList());
    }

    static TimelineRow row(Timeline t, String id) {
        return t.rows().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void supportViewRowOrder() {
        Timeline t = factory.supportView(TODAY);
        List<String> ids = t.rows().stream().map(TimelineRow::id).toList();
        assertEquals(List.of("3.40", "stream", "3.33", "3.20"), ids);
        assertEquals(LocalDate.of(2025, 1, 1), t.scale().start());
        assertEquals(LocalDate.of(2028, 12, 31), t.scale().end());
    }

    @Test
    void segmentKindsPerRow() {
        Timeline t = factory.supportView(TODAY);
        assertEquals(List.of("community"), kinds(row(t, "3.40")));
        // community, Red Hat, then IBM enterprise followed by IBM never-ending
        assertEquals(List.of("community", "enterprise", "enterprise", "nes"), kinds(row(t, "3.33")));
        assertEquals(List.of("Red Hat", "IBM"), row(t, "3.33").lanes());
        GanttTimelineSegment ibmNes = row(t, "3.33").segments().get(3);
        assertTrue(ibmNes.openEnded());
        assertEquals("ibm", ibmNes.variant());
        assertEquals(LocalDate.of(2028, 6, 30), ibmNes.start());
        // 3.20: community, Red Hat, HeroDevs never-ending
        assertEquals(List.of("community", "enterprise", "nes"), kinds(row(t, "3.20")));
        assertEquals("herodevs", row(t, "3.20").segments().get(2).variant());
        assertTrue(row(t, "3.20").segments().get(2).openEnded());
    }

    @Test
    void upcomingRowIsOneDashedSegmentAtEnd() {
        TimelineRow r = row(factory.supportView(TODAY), "stream");
        assertEquals("upcoming", kinds(r).getLast());
        assertEquals(TODAY.plusWeeks(WEEKS_TO_ADD), r.segments().getLast().end());
    }

    @Test
    void streamLaneTilesWithoutGaps() {
        TimelineRow stream = row(factory.supportView(TODAY), "stream");
        List<GanttTimelineSegment> segs = stream.segments();
        assertEquals(WEEKS_TO_ADD, segs.size());
        // 3.38 -> 3.39 tile exactly; 3.39 ends at the LTS 3.40, 3.50 starts later
        assertEquals(segs.get(0).end(), segs.get(1).start());
        assertTrue(segs.get(2).openEnded());
    }

    @Test
    void enterpriseSegmentOnlyWhenLaterThanCommunity() {
        var catalog = catalogOf("""
                {"releases": [{"version": "3.1", "release_date": "2026-01-01", "eol_date": "2027-01-01",
                  "lts": true, "rhbq": {"eol_date": "2026-12-01"}}]}""");
        Timeline t = new ReleaseTimelineFactory(catalog).supportView(TODAY);
        assertEquals(List.of("community"), kinds(row(t, "3.1")));
    }

    @Test
    void fullViewCoversWholeHistoryWithCompactRows() {
        Timeline t = factory.fullView(TODAY);
        List<String> ids = t.rows().stream().map(TimelineRow::id).toList();
        assertEquals(List.of("3.40", "stream", "3.33", "3.20", "2.16"), ids);
        assertEquals(LocalDate.of(2019, 1, 1), t.scale().start());
        // 0.2 is a non-LTS minor, so it lives in the stream lane
        assertEquals(LocalDate.of(2019, 3, 1), row(t, "stream").segments().get(0).start());
        assertEquals("true", row(t, "3.40").data().get("lts"));
    }

    @Test
    void tooltipLinesCarryDates() {
        GanttTimelineSegment seg = row(factory.supportView(TODAY), "3.33").segments().get(0);
        assertTrue(seg.tooltip().contains("Released 25 Mar 2026"));
        assertTrue(row(factory.supportView(TODAY), "3.33").segments().get(1).tooltip()
                .contains("Red Hat support until 30 Jun 2028"));
    }

    @Test
    void catalogDerivedValues() {
        Release r = catalog.find("3.20").orElseThrow();
        assertTrue(r.herodevsNes());
        assertEquals(LocalDate.of(2027, 6, 30), r.supportEnd());
        assertEquals(Release.Status.ENTERPRISE_ONLY, r.status(TODAY));
        assertEquals(Release.Status.EOL, catalog.find("2.16").orElseThrow().status(TODAY));
        assertEquals("3.40", catalog.latest().map(Release::version).orElseThrow());
        assertEquals("3.40", catalog.recommendedLts(TODAY).map(Release::version).orElseThrow());
        assertEquals(Release.Status.UPCOMING, catalog.find("4.0.0").orElseThrow().status(TODAY));
    }

    @Test
    void upcomingStartsOnPlannedDateWhenKnown() {
        var catalog = catalogOf("""
                {"releases": [{"version": "4.0.0", "lts": false, "upcoming": true, "planned_date": "2026-11-25"}]}""");
        GanttTimelineSegment seg = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream")
                .segments().get(0);
        assertEquals(LocalDate.of(2026, 11, 25), seg.start());
        assertTrue(seg.tooltip().contains("25 Nov 2026"));
    }

    @Test
    void upcomingLegendAndRowOnlyWhenPresent() {
        var catalog = catalogOf("""
                {"releases": [{"version": "3.1", "release_date": "2026-01-01", "eol_date": "2027-01-01", "lts": true}]}""");
        var factory = new ReleaseTimelineFactory(catalog);
        assertTrue(factory.legend().stream().noneMatch(i -> i.key().equals("upcoming")));
        assertTrue(factory.supportView(TODAY).rows().stream().noneMatch(r -> r.id().startsWith("upcoming")));
        assertTrue(this.factory.legend().stream().anyMatch(i -> i.key().equals("upcoming")));
    }

    @Test
    void streamLaneHasShortLabelForNarrowScreens() {
        TimelineRow stream = row(factory.supportView(TODAY), "stream");
        assertEquals("Non-LTS stream", stream.label());
        assertEquals("Non-LTS", stream.shortLabel());
    }

    @Test
    void upcomingLtsGetsLtsLengthBarAndDataButNoBadge() {
        var catalog = catalogOf("""
                {"policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
                 "releases": [
                   {"version": "4.0.0", "lts": true, "upcoming": true, "planned_date": "2026-11-25"},
                   {"version": "3.40", "release_date": "2026-09-30", "eol_date": "2027-09-30", "lts": true}]}""");
        TimelineRow r = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream");
        assertTrue(r.badges().isEmpty());
        assertEquals("true", r.data().get("lts"));
        GanttTimelineSegment seg = r.segments().get(0);
        assertEquals(LocalDate.of(2026, 11, 25), seg.start());
        assertEquals(LocalDate.of(2027, 11, 25), seg.end());
        assertTrue(seg.tooltip().contains("12 months"));
    }

    @Test
    void upcomingNonLtsKeepsShortPlaceholderAndNoBadge() {
        TimelineRow r = row(factory.supportView(TODAY), "stream");
        assertTrue(r.badges().isEmpty());
        assertEquals(r.segments().getLast().start().plusWeeks(WEEKS_TO_ADD), r.segments().getLast().end());
    }

    @Test
    void nonLtsUpcomingBarEndsWhereTheNextUpcomingStarts() {
        var catalog = catalogOf("""
                {"policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
                 "releases": [
                   {"version": "4.2", "lts": false, "upcoming": true, "planned_date": "2027-02-24"},
                   {"version": "4.1", "lts": false, "upcoming": true, "planned_date": "2027-01-27"},
                   {"version": "4.0", "lts": false, "upcoming": true, "planned_date": "2026-11-25"}]}""");
        List<GanttTimelineSegment> segs = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream").segments();
        assertEquals(3, segs.size());
        // 4.0 -> 4.1 -> 4.2 tile exactly; only the last keeps the default length
        assertEquals(LocalDate.of(2026, 11, 25), segs.get(0).start());
        assertEquals(LocalDate.of(2027, 1, 27), segs.get(0).end());
        assertEquals(LocalDate.of(2027, 1, 27), segs.get(1).start());
        assertEquals(LocalDate.of(2027, 2, 24), segs.get(1).end());
        assertEquals(LocalDate.of(2027, 2, 24).plusWeeks(WEEKS_TO_ADD), segs.get(2).end());
    }

    @Test
    void upcomingLtsKeepsLtsLengthAndShortensTheNonLtsBeforeIt() {
        var catalog = catalogOf("""
                {"policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
                 "releases": [
                   {"version": "4.3", "lts": true, "upcoming": true, "planned_date": "2027-03-31"},
                   {"version": "4.2", "lts": false, "upcoming": true, "planned_date": "2027-02-24"}]}""");
        List<GanttTimelineSegment> segs = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream").segments();
        assertEquals(LocalDate.of(2027, 3, 31), segs.get(0).end());
        assertEquals(LocalDate.of(2027, 3, 31), segs.get(1).start());
        assertEquals(LocalDate.of(2028, 3, 31), segs.get(1).end());
    }

    @Test
    void upcomingLtsIsClippedByALaterUpcomingButKeepsAtLeastTheDefaultLength() {
        var catalog = catalogOf("""
                {"policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
                 "releases": [
                   {"version": "4.5", "lts": false, "upcoming": true, "planned_date": "2027-04-20"},
                   {"version": "4.4", "lts": false, "upcoming": true, "planned_date": "2027-04-01"},
                   {"version": "4.3", "lts": true, "upcoming": true, "planned_date": "2027-03-31"}]}""");
        List<GanttTimelineSegment> segs = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream").segments();
        // 4.4 starts only a day after the LTS: clipping stops at the default bar length
        assertEquals(LocalDate.of(2027, 3, 31), segs.get(0).start());
        assertEquals(LocalDate.of(2027, 3, 31).plusWeeks(WEEKS_TO_ADD), segs.get(0).end());

        var roomy = catalogOf("""
                {"policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
                 "releases": [
                   {"version": "4.4", "lts": false, "upcoming": true, "planned_date": "2027-06-30"},
                   {"version": "4.3", "lts": true, "upcoming": true, "planned_date": "2027-03-31"}]}""");
        List<GanttTimelineSegment> clipped = row(new ReleaseTimelineFactory(roomy).supportView(TODAY), "stream").segments();
        assertEquals(LocalDate.of(2027, 6, 30), clipped.get(0).end());
    }

    @Test
    void upcomingWithoutDistinctStartsFallsBackToDefaultLength() {
        var catalog = catalogOf("""
                {"releases": [
                   {"version": "4.1", "lts": false, "upcoming": true},
                   {"version": "4.0", "lts": false, "upcoming": true}]}""");
        List<GanttTimelineSegment> segs = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream").segments();
        // Neither has a planned date, so both start today and there is no later start to end at
        assertEquals(TODAY.plusWeeks(WEEKS_TO_ADD), segs.get(0).end());
        assertEquals(TODAY.plusWeeks(WEEKS_TO_ADD), segs.get(1).end());
    }

    @Test
    void supportPeriods() {
        var months = new Releases.SupportPeriod(12, "months");
        var weeks = new Releases.SupportPeriod(4, "Weeks");
        assertEquals(java.time.Period.ofMonths(12), months.period());
        assertEquals(java.time.Period.ofWeeks(4), weeks.period());
        assertEquals("12 months", months.label());
        assertEquals("1 month", new Releases.SupportPeriod(1, "months").label());
        assertEquals("4 weeks", weeks.label());
        assertThrows(IllegalArgumentException.class, () -> new Releases.SupportPeriod(1, "years"));
        assertNull(new Releases.Policy(null, null, null, null, null, null).ltsSupportPeriod());
        assertNull(new Releases.Policy(null, null, null, null, null, null).releaseCadencePeriod());
        assertNull(new Releases.Policy(null, null, null, null, null, null).ltsCadencePeriod());
    }

    @Test
    void ltsDescriptionFillsInPolicyValues() {
        var policy = new Releases.Policy(new Releases.SupportPeriod(6, "months"), new Releases.SupportPeriod(12, "months"),
                null, new Releases.SupportPeriod(4, "weeks"),
                "LTS every {lts_cadence} gets {lts_support_duration}, minors every {release_cadence}.", null);
        assertEquals("LTS every 6 months gets 12 months, minors every 4 weeks.", policy.ltsDescription());
    }

    @Test
    void releaseCadenceSizesTheDefaultUpcomingBar() {
        var catalog = catalogOf("""
                {"policy": {"release_cadence": {"amount": 2, "unit": "months"}},
                 "releases": [{"version": "4.0", "lts": false, "upcoming": true, "planned_date": "2026-11-25"}]}""");
        GanttTimelineSegment seg = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "stream").segments().get(0);
        assertEquals(LocalDate.of(2027, 1, 25), seg.end());
    }
}
