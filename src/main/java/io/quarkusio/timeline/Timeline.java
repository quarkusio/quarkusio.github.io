package io.quarkusio.timeline;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Timeline(String id, TimeScale scale, List<TimelineRow> rows,
        List<TimelineMarker> markers, List<LegendItem> legend) {

    public static final int AXIS_H = 36;
    /** Band below the last row, where the today label sits. */
    public static final int FOOT_H = 26;
    /** Visible height of main-lane bars (e.g. community). */
    public static final int MAIN_H = 12;
    /** Height of extra-lane bars including their border; always less than MAIN_H, and 2 * LANE_H > MAIN_H. */
    public static final int LANE_H = 8;
    public static final int LANE_GAP = 2;
    /** Main bars are drawn this much taller, because their background-coloured stroke eats it. */
    public static final int MAIN_STROKE = 2;
    public static final int ROW_PAD = 12;

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
        return rows.stream().flatMap(r -> r.segments().stream()).map(TimelineSegment::variant)
                .filter(v -> v != null).distinct().toList();
    }

    public String rangeStart() {
        return scale.start().toString();
    }

    public String rangeEnd() {
        return scale.end().toString();
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
