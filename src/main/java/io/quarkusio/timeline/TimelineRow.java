package io.quarkusio.timeline;

import java.util.List;
import java.util.Map;

/**
 * A row has one main lane plus optional extra lanes, e.g. one per provider. Extra-lane bars
 * have a fixed height, are stacked next to the main bar when their date ranges overlap, and
 * share a slot otherwise. height is the row height in pixels. shortLabel (optional) replaces
 * label on narrow screens. data is rendered as data-*
 * attributes on the row.
 */
public record TimelineRow(String id, String label, List<String> badges, String href,
        List<TimelineSegment> segments, Map<String, String> data, List<String> lanes, int height, String shortLabel) {
}
