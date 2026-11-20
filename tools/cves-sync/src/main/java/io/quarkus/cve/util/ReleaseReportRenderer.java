package io.quarkus.cve.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.cve.model.ReleaseReport;
import java.util.Map;

/** Renders a {@link ReleaseReport} as YAML (the data file the website reads), JSON, or a short text summary. */
public final class ReleaseReportRenderer {

    private ReleaseReportRenderer() {}

    public static String render(ReleaseReport report, OutputFormat format) throws JsonProcessingException {
        return switch (format) {
            case PLAINTEXT -> plaintext(report);
            case JSON -> json(report);
            case YAML -> yaml(report);
        };
    }

    private static String json(ReleaseReport report) throws JsonProcessingException {
        return new ObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(report);
    }

    private static String yaml(ReleaseReport report) throws JsonProcessingException {
        YAMLMapper mapper = new YAMLMapper(YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                // Long strings stay on one line: po4a (pt.quarkus.io) cannot read the folded "...\" form
                .disable(YAMLGenerator.Feature.SPLIT_LINES)
                .build());
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsString(report);
    }

    private static String plaintext(ReleaseReport report) {
        StringBuilder out = new StringBuilder();
        ReleaseReport.Statistics s = report.statistics;
        out.append(String.format("%d releases scanned, %d without a BOM%n", s.releasesScanned, s.releasesWithoutContent));
        out.append(String.format("%d advisories evaluated, %d affect a release, %d pending%n",
                s.advisoriesEvaluated, report.advisories.size(), report.pending.size()));
        for (Map.Entry<String, ReleaseReport.Advisory> e : report.advisories.entrySet()) {
            ReleaseReport.Advisory a = e.getValue();
            out.append(String.format("%n%s %s %s%n  %s%n", e.getKey(), a.cve == null ? "-" : a.cve,
                    a.severity == null ? "-" : a.severity, a.summary == null ? "" : a.summary));
            for (ReleaseReport.Component c : a.affectedComponents) {
                out.append(String.format("  %s %s  ships %s%n", c.component, c.range, String.join(", ", c.shipped)));
            }
            out.append(String.format("  %d releases:", a.affectedReleases.list.size()));
            for (TreeRanges.Piece piece : a.affectedReleases.tree) out.append(' ').append(piece.root()).append(piece.cuts());
            out.append('\n');
        }
        return out.toString();
    }
}
