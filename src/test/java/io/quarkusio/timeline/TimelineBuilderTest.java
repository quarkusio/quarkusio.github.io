package io.quarkusio.timeline;

import static io.quarkusio.releases.ReleaseKind.MAJOR;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import io.quarkusio.charts.timeline.TimeScale;
import io.quarkusio.charts.timeline.Timeline;
import io.quarkusio.charts.timeline.gantt.GanttTimelineBuilder;
import io.quarkusio.releases.ReleaseKind;
import org.junit.jupiter.api.Test;

class TimelineBuilderTest {

    final TimeScale scale = new TimeScale(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

    @Test
    void openEndedSegmentReachesRangeEnd() {
        var tl = new GanttTimelineBuilder("t", scale).row("a", "a")
                .segment("community", LocalDate.of(2025, 1, 1), null, null, "t", List.of(), null).add().build();
        var seg = tl.rows().get(0).segments().get(0);
        assertTrue(seg.openEnded());
        double x = Double.parseDouble(seg.xPct());
        double w = Double.parseDouble(seg.wPct());
        assertEquals(100.0, x + w, 0.01);
    }

    @Test
    void clampsSegmentStartingBeforeRange() {
        var tl = new GanttTimelineBuilder("t", scale).row("a", "a")
                .segment("community", LocalDate.of(2020, 1, 1), LocalDate.of(2024, 7, 1), null, "t", List.of(), null)
                .add().build();
        assertEquals("0.000", tl.rows().get(0).segments().get(0).xPct());
    }

    @Test
    void dropsRowsWithNoVisibleSegmentUnlessKept() {
        LocalDate old = LocalDate.of(2020, 1, 1);
        var dropped = new GanttTimelineBuilder("t", scale).row("a", "a")
                .segment("community", old, old.plusDays(5), null, "t", List.of(), null).add().build();
        assertTrue(dropped.rows().isEmpty());
        var kept = new GanttTimelineBuilder("t", scale).keepEmptyRows(true).row("a", "a")
                .segment("community", old, old.plusDays(5), null, "t", List.of(), null).add().build();
        assertEquals(1, kept.rows().size());
    }

    @Test
    void shortSegmentGetsMinimumWidth() {
        LocalDate d = LocalDate.of(2024, 6, 1);
        var tl = new GanttTimelineBuilder("t", scale).row("a", "a")
                .segment("community", d, d, null, "t", List.of(), null).add().build();
        assertEquals("0.150", tl.rows().get(0).segments().get(0).wPct());
    }

    @Test
    void markersOutsideRangeAreDropped() {
        var tl = new GanttTimelineBuilder("t", scale)
                .marker(LocalDate.of(2020, 1, 1), "old", MAJOR)
                .marker(LocalDate.of(2024, 7, 1), "in", MAJOR).build();
        assertEquals(1, tl.markers().size());
    }

    @Test
    void lanesOverlappingMainBarAreStackedAboveIt() {
        var row = new GanttTimelineBuilder("t", scale).row("a", "a");
        int redHat = row.lane("Red Hat");
        int ibm = row.lane("IBM");
        var tl = row
                .segment("community", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null, "c", List.of(), null)
                .segment("enterprise", "rhbq", redHat, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1), null, "r", List.of(), null)
                .segment("enterprise", "ibm", ibm, LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 1), null, "i", List.of(), null)
                .add().build();
        var r = tl.rows().get(0);
        assertEquals(Timeline.ROW_PAD + 3 * Timeline.MAIN_H + 2 * Timeline.LANE_GAP, r.height());
        var main = r.segments().get(0);
        var rhbq = r.segments().get(1);
        var ibmSeg = r.segments().get(2);
        assertEquals(rhbq.y() + Timeline.MAIN_H + Timeline.LANE_GAP, ibmSeg.y());
        assertEquals(ibmSeg.y() + Timeline.MAIN_H + Timeline.LANE_GAP, main.y());
    }

    @Test
    void aSingleOverlappingLaneSitsAboveTheMainBar() {
        var row = new GanttTimelineBuilder("t", scale).row("a", "a");
        int redHat = row.lane("Red Hat");
        var tl = row
                .segment("community", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null, "c", List.of(), null)
                .segment("enterprise", "rhbq", redHat, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1), null, "r", List.of(), null)
                .add().build();
        var r = tl.rows().get(0);
        assertEquals(Timeline.ROW_PAD + 2 * Timeline.MAIN_H + Timeline.LANE_GAP, r.height());
        assertTrue(r.segments().get(1).y() < r.segments().get(0).y());
    }

    @Test
    void laneAfterMainBarStaysBesideIt() {
        var row = new GanttTimelineBuilder("t", scale).row("a", "a");
        int lane = row.lane("Red Hat");
        var tl = row
                .segment("community", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 1), null, "c", List.of(), null)
                .segment("enterprise", "rhbq", lane, LocalDate.of(2024, 6, 1), LocalDate.of(2025, 6, 1), null, "e", List.of(), null)
                .add().build();
        assertEquals(Timeline.ROW_PAD + Timeline.MAIN_H, tl.rows().get(0).height());
    }

    @Test
    void offScreenLinksAreFilteredOut() {
        var builder = new GanttTimelineBuilder("t", scale).keepEmptyRows(true);
        builder.row("r1", "r1").add();
        builder.row("r2", "r2").add();
        // entirely in the past
        builder.link("r1", "r2", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        // entirely in the future
        builder.link("r1", "r2", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 1, 1));
        // within range
        builder.link("r1", "r2", LocalDate.of(2024, 6, 1), LocalDate.of(2025, 1, 1));

        Timeline tl = builder.build();
        assertEquals(1, tl.links().size());
    }
}
