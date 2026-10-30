package io.quarkusio.timeline;

import java.time.LocalDate;
import java.util.List;

/**
 * kind is a free string, rendered as CSS class tl-seg--{kind}. variant (optional) picks a
 * colour, rendered as tl-prov--{variant}. lane is 0 for the main lane; y and h are the bar geometry in pixels, set when the row is built. ref optionally ties
 * the segment to an item elsewhere on the page (rendered as data-version).
 */
public record TimelineSegment(String kind, String variant, int lane, LocalDate start, LocalDate end,
        boolean openEnded, String href, String title, List<String> details, String ref,
        String xPct, String wPct, int y, int h) {

    /** Lines joined for the native SVG <title>. */
    public String summary() {
        return String.join("; ", details);
    }

    /** Lines joined for the data-tooltip attribute. */
    public String tooltip() {
        return String.join("|", details);
    }
}
