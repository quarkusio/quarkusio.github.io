package io.quarkusio.charts.timeline.gantt;

/**
 * A connector between rows or within a row, in plot coordinates. path is SVG path data in a box that starts at
 * xPct and is widthPct wide, with x running 0..100 across it and y in pixels.
 */
public record TimelineLink(String xPct, String widthPct, String path) {
}
