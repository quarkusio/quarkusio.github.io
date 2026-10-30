package io.quarkusio.releases;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import io.quarkusio.timeline.Timeline;
import io.quarkusio.timeline.TimelineRow;
import io.quarkusio.timeline.TimelineSegment;

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
                 "rhbq_eol": "2028-06-30", "ibm_eol": "2028-06-30"},
                {"version": "3.20", "release_date": "2025-03-26", "eol_date": "2026-03-28", "lts": true,
                 "rhbq_eol": "2027-06-30", "herodevs_eol": "nes"},
                {"version": "3.50", "release_date": "2026-10-01", "lts": false},
                {"version": "2.16", "release_date": "2023-01-01", "eol_date": "2023-06-01", "lts": true,
                 "rhbq_eol": "2023-07-01"},
                {"version": "0.2", "release_date": "2019-03-01", "eol_date": "2019-03-08", "lts": false}
              ]
            }
            """;

    static final ObjectMapper MAPPER = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

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
        return row.segments().stream().map(TimelineSegment::kind).collect(Collectors.toList());
    }

    static TimelineRow row(Timeline t, String id) {
        return t.rows().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void supportViewRowOrder() {
        Timeline t = factory.supportView(TODAY);
        List<String> ids = t.rows().stream().map(TimelineRow::id).toList();
        assertEquals(List.of("upcoming-4.0.0", "stream", "3.40", "3.33", "3.20"), ids);
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
        TimelineSegment ibmNes = row(t, "3.33").segments().get(3);
        assertTrue(ibmNes.openEnded());
        assertEquals("ibm", ibmNes.variant());
        assertEquals(LocalDate.of(2028, 6, 30), ibmNes.start());
        // 3.20: community, Red Hat, HeroDevs never-ending
        assertEquals(List.of("community", "enterprise", "nes"), kinds(row(t, "3.20")));
        assertEquals("herodevs", row(t, "3.20").segments().get(2).variant());
        assertTrue(row(t, "3.20").segments().get(2).openEnded());
    }

    @Test
    void upcomingRowIsOneDashedSegment() {
        TimelineRow r = row(factory.supportView(TODAY), "upcoming-4.0.0");
        assertEquals(List.of("upcoming"), kinds(r));
        assertEquals(TODAY.plusWeeks(6), r.segments().get(0).end());
    }

    @Test
    void streamLaneTilesWithoutGaps() {
        TimelineRow stream = row(factory.supportView(TODAY), "stream");
        List<TimelineSegment> segs = stream.segments();
        assertEquals(3, segs.size());
        // 3.38 -> 3.39 tile exactly; 3.39 ends at the LTS 3.40, 3.50 starts later
        assertEquals(segs.get(0).end(), segs.get(1).start());
        assertTrue(segs.get(2).openEnded());
    }

    @Test
    void enterpriseSegmentOnlyWhenLaterThanCommunity() {
        var catalog = catalogOf("""
                {"releases": [{"version": "3.1", "release_date": "2026-01-01", "eol_date": "2027-01-01",
                  "lts": true, "rhbq_eol": "2026-12-01"}]}""");
        Timeline t = new ReleaseTimelineFactory(catalog).supportView(TODAY);
        assertEquals(List.of("community"), kinds(row(t, "3.1")));
    }

    @Test
    void fullViewCoversWholeHistoryWithCompactRows() {
        Timeline t = factory.fullView(TODAY);
        List<String> ids = t.rows().stream().map(TimelineRow::id).toList();
        assertEquals(List.of("upcoming-4.0.0", "stream", "3.40", "3.33", "3.20", "2.16"), ids);
        assertEquals(LocalDate.of(2019, 1, 1), t.scale().start());
        // 0.2 is a non-LTS minor, so it lives in the stream lane
        assertEquals(LocalDate.of(2019, 3, 1), row(t, "stream").segments().get(0).start());
        assertEquals("true", row(t, "3.40").data().get("lts"));
    }

    @Test
    void tooltipLinesCarryDates() {
        TimelineSegment seg = row(factory.supportView(TODAY), "3.33").segments().get(0);
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
        TimelineSegment seg = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "upcoming-4.0.0")
                .segments().get(0);
        assertEquals(LocalDate.of(2026, 11, 25), seg.start());
        assertTrue(seg.tooltip().contains("25 Nov 2026"));
    }

    @Test
    void upcomingLegendAndRowOnlyWhenPresent() {
        var catalog = catalogOf("""
                {"releases": [{"version": "3.1", "release_date": "2026-01-01", "eol_date": "2027-01-01", "lts": true}]}""");
        var factory = new ReleaseTimelineFactory(catalog);
        assertTrue(factory.legend().stream().noneMatch(i -> i.kind().equals("upcoming")));
        assertTrue(factory.supportView(TODAY).rows().stream().noneMatch(r -> r.id().startsWith("upcoming")));
        assertTrue(this.factory.legend().stream().anyMatch(i -> i.kind().equals("upcoming")));
    }

    @Test
    void streamLaneHasShortLabelForNarrowScreens() {
        TimelineRow stream = row(factory.supportView(TODAY), "stream");
        assertEquals("Non-LTS releases", stream.label());
        assertEquals("Non-LTS", stream.shortLabel());
    }

    @Test
    void upcomingLtsGetsBadgeAndLtsLengthBar() {
        var catalog = catalogOf("""
                {"policy": {"lts_support_duration": "12 months"},
                 "releases": [
                   {"version": "4.0.0", "lts": true, "upcoming": true, "planned_date": "2026-11-25"},
                   {"version": "3.40", "release_date": "2026-09-30", "eol_date": "2027-09-30", "lts": true}]}""");
        TimelineRow r = row(new ReleaseTimelineFactory(catalog).supportView(TODAY), "upcoming-4.0.0");
        assertEquals(List.of("LTS"), r.badges());
        assertEquals("true", r.data().get("lts"));
        TimelineSegment seg = r.segments().get(0);
        assertEquals(LocalDate.of(2026, 11, 25), seg.start());
        assertEquals(LocalDate.of(2027, 11, 25), seg.end());
        assertTrue(seg.tooltip().contains("12 months"));
    }

    @Test
    void upcomingNonLtsKeepsShortPlaceholderAndNoBadge() {
        TimelineRow r = row(factory.supportView(TODAY), "upcoming-4.0.0");
        assertTrue(r.badges().isEmpty());
        assertEquals(r.segments().get(0).start().plusWeeks(6), r.segments().get(0).end());
    }

    @Test
    void policyDurationParsing() {
        assertEquals(java.time.Period.ofMonths(12), new Releases.Policy(null, "12 months", null, null, null).ltsSupportPeriod());
        assertEquals(java.time.Period.ofYears(1), new Releases.Policy(null, "1 year", null, null, null).ltsSupportPeriod());
        assertNull(new Releases.Policy(null, "until the next minor", null, null, null).ltsSupportPeriod());
        assertNull(new Releases.Policy(null, null, null, null, null).ltsSupportPeriod());
    }
}
