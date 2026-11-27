package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.cve.model.ReleaseReport;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** CI runs po4a over every YAML file; see {@link Po4a}. These are the lines it refuses. */
class Po4aTest {

    @Test
    void aColonBeforeABracketOrBraceIsDropped() {
        assertEquals("Netty [codec-http2] Lack of Host Header Deduplication",
                Po4a.safe("Netty: [codec-http2] Lack of Host Header Deduplication"));
        assertEquals("Foo {bar} baz", Po4a.safe("Foo: {bar} baz"));
        assertEquals("a [b] c {d}", Po4a.safe("a: [b] c: {d}"));
    }

    @Test
    void newlinesAndTrailingBackslashesAreRemoved() {
        assertEquals("Path traversal via trailing", Po4a.safe("Path traversal\n  via trailing \\"));
        assertEquals("Netty [codec] issue", Po4a.safe("Netty:\r\n[codec] issue\\\\"));
    }

    @Test
    void otherTextIsLeftAlone() {
        assertEquals("Quarkus-vertx-http: authorization bypass", Po4a.safe("Quarkus-vertx-http: authorization bypass"));
        assertEquals("[Bzip2Decoder] Infinite Loop", Po4a.safe("[Bzip2Decoder] Infinite Loop"));
        assertNull(Po4a.safe(null));
    }

    @Test
    void theWrittenDataHasNoLineThatPo4aRefuses() throws Exception {
        ReleaseReport report = new ReleaseReport();
        ReleaseReport.Advisory a = new ReleaseReport.Advisory();
        a.summary = Po4a.safe("Netty: [SpdyHttpDecoder] ByteBuf Reference Leak on RST_STREAM");
        ReleaseReport.Component c = new ReleaseReport.Component();
        c.component = "io.netty:netty-codec";
        c.range = "<4.1.100";
        c.shipped = new ArrayList<>(List.of("4.1.90"));
        a.affectedComponents.add(c);
        a.affectedReleases.list.add("3.0.0");
        // a piece that runs to every tip of its branch has no cuts
        a.affectedReleases.tree.add(new TreeRanges.Piece("3.0.0", List.of()));
        a.affectedReleases.tree.add(new TreeRanges.Piece("3.2.0", List.of("3.2.9")));
        report.advisories.put("GHSA-xxxx", a);
        report.releases.add(new ReleaseReport.ReleaseSource("3.0.0", "maven://io.quarkus:quarkus-bom:3.0.0"));

        String yaml = withoutStatistics(ReleaseReportRenderer.render(report, OutputFormat.YAML));

        assertFalse(yaml.lines().anyMatch(l -> l.contains(": [") || l.contains(": {")), yaml);
        assertEquals(1, yaml.lines().filter(l -> l.trim().equals("cuts:")).count(), "only the piece that has cuts writes them:\n" + yaml);
        assertFalse(yaml.contains("pending"), "an empty list of pending CVEs is left out:\n" + yaml);
    }

    /** The statistics block is cut from the data file by the workflow, because it changes on every run. */
    private static String withoutStatistics(String yaml) {
        StringBuilder out = new StringBuilder();
        boolean skip = false;
        for (String line : yaml.split("\n")) {
            if (line.startsWith("statistics:")) skip = true;
            else if (skip && !line.isEmpty() && !line.startsWith(" ")) skip = false;
            if (!skip) out.append(line).append('\n');
        }
        return out.toString();
    }
}
