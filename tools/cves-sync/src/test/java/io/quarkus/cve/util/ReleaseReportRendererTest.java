package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.cve.model.ReleaseReport;
import org.junit.jupiter.api.Test;

/**
 * pt.quarkus.io translates the data files with po4a, whose YAML::Tiny parser cannot read a long string folded across
 * lines with a trailing backslash. CI runs po4a over every YAML file, so a file written that way fails the build.
 */
class ReleaseReportRendererTest {

    @Test
    void longSummariesStayOnOneLine() throws Exception {
        ReleaseReport report = new ReleaseReport();
        ReleaseReport.Pending p = new ReleaseReport.Pending();
        p.id = "CVE-2026-0001";
        p.summary = "Netty: [Bzip2Decoder] Infinite Loop in RLE State Machine Leads to Event-Loop starvation and a "
                + "denial of service when a crafted stream is decoded by a server that is listening on the network";
        report.pending.add(p);

        String yaml = ReleaseReportRenderer.render(report, OutputFormat.YAML);

        assertTrue(yaml.contains(p.summary), "the summary should be written whole, on one line");
        assertFalse(yaml.lines().anyMatch(l -> l.endsWith("\\")), "no line may be folded with a trailing backslash");
    }
}
