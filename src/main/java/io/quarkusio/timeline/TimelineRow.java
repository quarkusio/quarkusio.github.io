package io.quarkusio.timeline;

import java.util.List;
import java.util.Map;

/**
 * A row has one main lane plus optional extra lanes, e.g. one per provider. Extra-lane bars
 * are as tall as the main bar, are stacked next to it when their date ranges overlap, and
 * share a slot otherwise. Lanes that overlap the main bar in time are painted behind it. height is the row height in pixels. shortLabel (optional) replaces
 * label on narrow screens. data is rendered as data-*
 * attributes on the row.
 */
public record TimelineRow(String id, String label, List<String> badges, String href,
        List<TimelineSegment> segments, Map<String, String> data, List<String> lanes, int height, String shortLabel) {

    /** Segments in SVG paint order: extra lanes first, so the main-lane bar is drawn on top of them. */
    public List<TimelineSegment> paintOrder() {
        return java.util.stream.Stream.concat(
                segments.stream().filter(s -> s.lane() > 0),
                segments.stream().filter(s -> s.lane() == 0)).toList();
    }
}
