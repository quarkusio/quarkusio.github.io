package io.quarkusio.charts.scatter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.quarkusio.charts.timeline.LegendItem;
import io.quarkusio.charts.timeline.TimeScale;
import io.quarkusio.charts.timeline.Timeline;

/** Dots on a time axis, one lane per row. Shares its geometry constants and scale with {@link Timeline}. */
public record ScatterPlot(String id, TimeScale scale, List<ScatterRow> rows, List<LegendItem> legend) {

    // The layout constants, handed to the page script, which lays the rows out again when marks are filtered.
    public int lanePitch() {
        return ScatterRow.LANE_PITCH;
    }

    public int hoverPitch() {
        return ScatterRow.HOVER_PITCH;
    }

    public int barH() {
        return ScatterRow.BAR_H;
    }

    public int preH() {
        return ScatterRow.PRE_H;
    }

    public int pad() {
        return ScatterRow.PAD;
    }

    public int minRowH() {
        return ScatterRow.MIN_HEIGHT;
    }

    public int axisH() {
        return Timeline.AXIS_H;
    }

    public int footH() {
        return Timeline.FOOT_H;
    }

    public int gapDays() {
        return ScatterRow.LANE_GAP_DAYS;
    }

    public int crossDays() {
        return ScatterRow.CROSS_HALF_DAYS;
    }

    /** Rows that are not shown take no space. */
    public int rowY(int index) {
        int y = Timeline.AXIS_H;
        for (int i = 0; i < index; i++) {
            if (rows.get(i).shown()) {
                y += rows.get(i).height();
            }
        }
        return y;
    }

    public int height() {
        return rowY(rows.size()) + Timeline.FOOT_H;
    }

    public String rangeStart() {
        return scale.start().toString();
    }

    public String rangeEnd() {
        return scale.end().toString();
    }

    public List<TimeScale.Tick> ticks() {
        return scale.ticks();
    }

    /** Distinct mark kinds, used to define one fade gradient each. */
    public List<String> kinds() {
        return rows.stream().flatMap(r -> r.marks().stream()).map(ScatterMark::kind).distinct().toList();
    }

    public List<TimeScale.Stripe> stripes() {
        return scale.yearStripes();
    }

    /** Build-time position of the today line, so it exists without JS. */
    public String todayPct(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        return scale.contains(today) ? scale.pct(today) : null;
    }

    public String todayLabel(LocalDateTime now) {
        return TimeScale.todayLabel(now.toLocalDate());
    }
}
