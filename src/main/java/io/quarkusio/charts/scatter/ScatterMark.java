package io.quarkusio.charts.scatter;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One mark in a row: a bar from start to end. When sinceAtLeast is set, the bar starts at the earliest release we have
 * data for, so it may really go back further, and it is drawn with an arrow at its start. A pending mark is also terminated by a cross at its end, because
 * we do not know when it will be resolved. A bar has two parts: from start to found
 * (before anyone knew about it, drawn lighter) and from found to end. preWPct, postXPct and postWPct are the
 * widths and position of those two parts in percent of the axis. kind is a free string, rendered as
 * CSS class tl-sev--{kind}. open marks a bar whose end is not known yet. lane is the sub-lane in the row (0 is
 * the top), y is the bar's top and cy the mark's vertical centre, both in pixels, set when the row is built.

 * A dot mark is a single point on the start date: a filled circle, or a cross if open. Its parts have no width.
 * details is rendered as the tooltip lines.
 */
public record ScatterMark(String kind, boolean open, boolean pending, boolean sinceAtLeast, boolean dot, int lane, LocalDate start, LocalDate found, LocalDate end,
                          String xPct, String preWPct, String postXPct, String postWPct, String endXPct, int y, int cy, String href, String title, List<String> details) {

    /** Height of the part from publication on, in pixels. */
    public int barH() {
        return ScatterRow.BAR_H;
    }

    /** Top of the paler part: it is thinner than the rest, so it is centred on it. */
    public String preY() {
        return String.format(java.util.Locale.ROOT, "%.1f", y + (ScatterRow.BAR_H - ScatterRow.PRE_H) / 2.0);
    }

    /** Height of the paler part before publication, in pixels. */
    public int preH() {
        return ScatterRow.PRE_H;
    }

    /** The clickable area of a bar is as tall as its lane, so it is easy to hit and never overlaps the next lane. */
    public int hitY() {
        return y - (ScatterRow.LANE_PITCH - ScatterRow.BAR_H) / 2;
    }

    public int hitH() {
        return ScatterRow.LANE_PITCH;
    }

    /** Width of the whole bar, both parts, in percent. */
    public String wPct() {
        return String.format(java.util.Locale.ROOT, "%.3f", Double.parseDouble(preWPct) + Double.parseDouble(postWPct));
    }

    public boolean hasPre() {
        return Double.parseDouble(preWPct) > 0;
    }

    /** Lines joined for the native SVG title. */
    public String summary() {
        return String.join("; ", details);
    }

    /** Lines joined for the data-tooltip attribute; the timeline script splits on the bar character. */
    public String tooltip() {
        return details.stream().map(d -> d.replace('|', '/')).collect(Collectors.joining("|"));
    }
}
