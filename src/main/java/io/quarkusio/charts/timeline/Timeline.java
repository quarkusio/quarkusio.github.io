package io.quarkusio.charts.timeline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.quarkusio.charts.timeline.gantt.GanttTimelineSegment;
import io.quarkusio.charts.timeline.gantt.TimelineChain;
import io.quarkusio.charts.timeline.gantt.TimelineLink;
import io.quarkusio.charts.timeline.gantt.TimelineRow;

public record Timeline(String id, TimeScale scale, List<TimelineRow> rows,
        List<TimelineMarker> markers, List<LegendItem> legend, List<TimelineLink> links,
        List<TimelineChain> chains) {

    public static final int AXIS_H = 36;
    /** Band below the last row, where the today label sits. */
    public static final int FOOT_H = 26;
    /** Visible height of main-lane bars (e.g. community). */
    public static final int MAIN_H = 12;
    public static final int LANE_GAP = 6;
    public static final int ROW_PAD = 24;

    /** Pixel offset of the row at index, below the axis band. */
    public int rowY(int index) {
        int y = AXIS_H;
        for (int i = 0; i < index; i++) {
            y += rows.get(i).height();
        }
        return y;
    }

    /** Total plot height in pixels. */
    public int height() {
        return rowY(rows.size()) + FOOT_H;
    }

    /** Distinct segment variants, used to define one pattern and gradient each. */
    public List<String> variants() {
        return rows.stream().flatMap(r -> r.segments().stream()).map(GanttTimelineSegment::variant)
                .filter(v -> v != null).distinct().toList();
    }

    public String rangeStart() {
        return scale.start().toString();
    }

    public String rangeEnd() {
        return scale.end().toString();
    }

    public List<TimeScale.Stripe> stripes() {
        return scale.yearStripes();
    }

    public List<TimeScale.Tick> ticks() {
        return scale.yearTicks();
    }

    /** Build-time position of the today line, so it exists without JS. */
    public String todayPct(LocalDate today) {
        return scale.contains(today) ? scale.pct(today) : null;
    }

    public String todayLabel(LocalDateTime now) {
        return "Today · " + now.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.ENGLISH));
    }

    public String todayPct(LocalDateTime now) {
        return todayPct(now.toLocalDate());
    }
}
