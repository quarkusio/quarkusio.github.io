package io.quarkusio.charts.scatter;

import java.util.List;
import java.util.Map;

/**
 * One horizontal row. Marks that overlap in time sit in different sub-lanes, so the row is as tall as it needs
 * to be. shortLabel (optional) replaces label on narrow screens. A row that is not shown is still in the page, so
 * the page script can bring it in, but it takes up no space until then. data is rendered as data-* attributes
 * on the row's label, for the page script.
 */
public record ScatterRow(String id, String label, String shortLabel, List<ScatterMark> marks, int lanes,
                         boolean shown, Map<String, String> data) {

    /** How many marks the row has, which the page script compares with the slider. */
    public int count() {
        return marks.size();
    }

    /** Vertical distance between sub-lanes, in pixels: bars are 1px apart. */
    public static final int LANE_PITCH = 7;
    /** The distance while the pointer is over the row, set by the page script, which spreads the lanes out. */
    public static final int HOVER_PITCH = 11;
    /** Height of the part of a bar from publication on, in pixels. */
    public static final int BAR_H = 6;
    /** Height of the paler part before publication, in pixels; centred on the bar. */
    public static final int PRE_H = 5;
    /** Space above the first and below the last lane. */
    public static final int PAD = 6;
    /** Marks closer than this many days in one lane would touch; they go in different lanes. */
    public static final int LANE_GAP_DAYS = 4;
    /** A cross is about this many days wide on each side of its date, so it is kept clear of the next mark. */
    public static final int CROSS_HALF_DAYS = 6;
    /** Smallest row height; matches a tl-h-* class in _timeline.scss. */
    public static final int MIN_HEIGHT = 24;

    /** Row height in pixels, always a tl-h-* class in _timeline.scss. */
    public int height() {
        return heightFor(lanes);
    }

    public static int heightFor(int lanes) {
        return Math.max(MIN_HEIGHT, 2 * PAD + lanes * LANE_PITCH);
    }

    /** Top of a bar in the given lane, centred within the row. */
    public static int barY(int lane, int rowHeight, int lanes) {
        return (rowHeight - lanes * LANE_PITCH) / 2 + lane * LANE_PITCH + (LANE_PITCH - BAR_H) / 2;
    }
}
