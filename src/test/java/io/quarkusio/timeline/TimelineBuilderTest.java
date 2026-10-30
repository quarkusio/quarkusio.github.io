package io.quarkusio.timeline;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class TimelineBuilderTest {

    final TimeScale scale = new TimeScale(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

    @Test
    void openEndedSegmentReachesRangeEnd() {
        var tl = new TimelineBuilder("t", scale).row("a", "a")
                .segment("community", LocalDate.of(2025, 1, 1), null, null, "t", List.of(), null).add().build();
        var seg = tl.rows().get(0).segments().get(0);
        assertTrue(seg.openEnded());
        double x = Double.parseDouble(seg.xPct());
        double w = Double.parseDouble(seg.wPct());
        assertEquals(100.0, x + w, 0.01);
    }

    @Test
    void clampsSegmentStartingBeforeRange() {
        var tl = new TimelineBuilder("t", scale).row("a", "a")
                .segment("community", LocalDate.of(2020, 1, 1), LocalDate.of(2024, 7, 1), null, "t", List.of(), null)
                .add().build();
        assertEquals("0.000", tl.rows().get(0).segments().get(0).xPct());
    }

    @Test
    void dropsRowsWithNoVisibleSegmentUnlessKept() {
        LocalDate old = LocalDate.of(2020, 1, 1);
        var dropped = new TimelineBuilder("t", scale).row("a", "a")
                .segment("community", old, old.plusDays(5), null, "t", List.of(), null).add().build();
        assertTrue(dropped.rows().isEmpty());
        var kept = new TimelineBuilder("t", scale).keepEmptyRows(true).row("a", "a")
                .segment("community", old, old.plusDays(5), null, "t", List.of(), null).add().build();
        assertEquals(1, kept.rows().size());
    }

    @Test
    void shortSegmentGetsMinimumWidth() {
        LocalDate d = LocalDate.of(2024, 6, 1);
        var tl = new TimelineBuilder("t", scale).row("a", "a")
                .segment("community", d, d, null, "t", List.of(), null).add().build();
        assertEquals("0.150", tl.rows().get(0).segments().get(0).wPct());
    }

    @Test
    void markersOutsideRangeAreDropped() {
        var tl = new TimelineBuilder("t", scale)
                .marker(LocalDate.of(2020, 1, 1), "old", "major")
                .marker(LocalDate.of(2024, 7, 1), "in", "major").build();
        assertEquals(1, tl.markers().size());
    }
}
