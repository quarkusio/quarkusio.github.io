package io.quarkusio.charts.timeline.gantt;

/** A short horizontal joint at xPct, y pixels down the plot, drawn over the border between two adjoining bars. */
public record TimelineChain(String xPct, int y) {
}
