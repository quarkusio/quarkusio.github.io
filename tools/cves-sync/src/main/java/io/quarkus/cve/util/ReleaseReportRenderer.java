package io.quarkus.cve.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.cve.model.ReleaseReport;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link ReleaseReport} as text. Minors and releases arrive newest first and are
 * rendered in that order. Each release shows what changed relative to the next-older entry;
 * the newest release of a line additionally lists everything still active, and the oldest
 * lists its full starting set.
 */
public final class ReleaseReportRenderer {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String CYAN   = "\u001B[36m";
    private static final String DIM    = "\u001B[2m";

    private ReleaseReportRenderer() {}

    public static String render(ReleaseReport report, OutputFormat format) throws JsonProcessingException {
        return switch (format) {
            case PLAINTEXT_ANSI_COLOR -> plaintext(report, true);
            case PLAINTEXT_NOCOLOR -> plaintext(report, false);
            case MARKDOWN -> markdown(report);
            case JSON -> json(report);
            case YAML -> yaml(report);
        };
    }

    // -----------------------------------------------------------------------
    // JSON / YAML
    // -----------------------------------------------------------------------

    private static String json(ReleaseReport report) throws JsonProcessingException {
        return new ObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(report);
    }

    private static final java.util.regex.Pattern DATE_FIELD_PATTERN = java.util.regex.Pattern.compile(
            "(?m)^(\\s*(?:release_date|eol_date|latest_patch_date|rhbq_eol|ibm_eol|herodevs_eol): )\"(\\d{4}-\\d{2}-\\d{2})\"$");

    abstract static class CvesYamlMixin {
        @JsonIgnore
        public ReleaseReport.Split<ReleaseReport.Change> introduced;
        @JsonIgnore
        public ReleaseReport.Split<ReleaseReport.Change> resolved;
    }

    abstract static class FindingYamlMixin {
        @JsonIgnore
        public String score;
    }

    private static String yaml(ReleaseReport report) throws JsonProcessingException {
        YAMLMapper mapper = new YAMLMapper(YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .build());
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        mapper.addMixIn(ReleaseReport.Cves.class, CvesYamlMixin.class);
        mapper.addMixIn(ReleaseReport.Finding.class, FindingYamlMixin.class);
        String yamlStr = mapper.writeValueAsString(report);
        return DATE_FIELD_PATTERN.matcher(yamlStr).replaceAll("$1$2");
    }

    // -----------------------------------------------------------------------
    // Plain text, with or without ANSI colour
    // -----------------------------------------------------------------------

    private static String plaintext(ReleaseReport report, boolean color) {
        Text t = new Text(color);
        t.out.append('\n').append(t.c(BOLD + CYAN, "Statistics")).append('\n');
        t.rule();
        for (String[] row : statisticsRows(report.statistics)) {
            t.out.append(String.format("  %-42s %s%n", row[0], row[1]));
        }
        t.rule();
        t.out.append('\n');
        t.out.append(t.c(DIM, "Advisories are today's knowledge applied to every release, so an older release can "
                + "show CVEs that were published long after it shipped.")).append('\n');
        t.out.append(t.c(DIM, "Newest first; each release shows what changed compared to the next-older one below it."))
                .append('\n');

        for (ReleaseReport.ReleaseGroup group : report.releaseGroups) {
            t.out.append('\n');
            t.rule();
            t.out.append(String.format("%s   (%d release%s)%n", t.c(BOLD + CYAN, "Quarkus " + group.releaseGroup),
                    group.releases.size(), group.releases.size() == 1 ? "" : "s"));
            t.rule();

            for (ReleaseReport.Release r : group.releases) {
                t.out.append(String.format("%n%s %s %s%n", t.c(BOLD, String.format("%-14s", r.version)),
                        t.c(DIM, String.format("%-16s", r.date != null ? r.date : "date unknown")),
                        t.c(BOLD, r.cves.active.count() + " active")));
                if (r.first) {
                    t.grouped("  active:", r.cves.active);
                    continue;
                }
                if (r.cves.resolved.count() == 0 && r.cves.introduced.count() == 0) {
                    t.out.append("  ").append(t.c(DIM, "no change in advisories")).append('\n');
                }
                t.changes("  resolved:", r.cves.resolved, GREEN);
                t.changes("  introduced:", r.cves.introduced, RED);
                if (r.mostRecent) t.grouped("  still active in " + r.version + ":", r.cves.active);
            }
        }
        t.out.append('\n');
        return t.out.toString();
    }

    private static final class Text {
        final StringBuilder out = new StringBuilder();
        final boolean color;

        Text(boolean color) {
            this.color = color;
        }

        String c(String code, String text) {
            return color ? code + text + RESET : text;
        }

        void rule() {
            out.append(c(DIM, "-".repeat(110))).append('\n');
        }

        void grouped(String title, ReleaseReport.Split<ReleaseReport.Finding> findings) {
            out.append(title).append('\n');
            if (findings.count() == 0) {
                out.append("    ").append(c(DIM, "none")).append('\n');
                return;
            }
            part("Core & extensions", findings.core);
            part("Dependencies", findings.dependencies);
        }

        private void part(String name, List<ReleaseReport.Finding> findings) {
            if (findings.isEmpty()) return;
            out.append("    ").append(c(BOLD, name + " (" + findings.size() + ")")).append('\n');
            for (ReleaseReport.Finding f : findings) out.append("      ").append(findingLine(f)).append('\n');
        }

        void changes(String title, ReleaseReport.Split<ReleaseReport.Change> changes, String colorCode) {
            if (changes.count() == 0) return;
            out.append(title).append('\n');
            changePart("Core & extensions", changes.core, colorCode);
            changePart("Dependencies", changes.dependencies, colorCode);
        }

        private void changePart(String name, List<ReleaseReport.Change> changes, String colorCode) {
            if (changes.isEmpty()) return;
            out.append("    ").append(c(BOLD, name + " (" + changes.size() + ")")).append('\n');
            for (ReleaseReport.Change ch : changes) {
                out.append("      ").append(c(colorCode, findingLine(ch.finding))).append('\n');
                out.append("        ").append(c(DIM, "because " + ch.cause)).append('\n');
            }
        }
    }

    /** Label / value pairs of the statistics block, shared by the plain text and markdown renderers. */
    private static List<String[]> statisticsRows(ReleaseReport.Statistics s) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Scan time", s.scanTime});
        rows.add(new String[] {"Arguments", s.arguments.isEmpty() ? "(none)" : String.join(" ", s.arguments)});
        rows.add(new String[] {"Execution time", String.format("%.1fs", s.executionMillis / 1000.0)});
        rows.add(new String[] {"Advisories evaluated", String.valueOf(s.advisoriesEvaluated)});
        rows.add(new String[] {"Advisories not evaluable", s.advisoriesNotEvaluable
                + " (no usable version ranges, not placed on any release)"});
        rows.add(new String[] {"HTTP requests: total", String.valueOf(s.httpRequests.total)});
        s.httpRequests.perProvider.forEach((p, n) -> rows.add(new String[] {"HTTP requests: " + p, String.valueOf(n)}));
        rows.add(new String[] {"HTTP 304 not modified: total", String.valueOf(s.notModified.total)});
        s.notModified.perProvider.forEach((p, n) -> rows.add(new String[] {"HTTP 304 not modified: " + p, String.valueOf(n)}));
        s.chargedPercent.forEach((p, pct) -> {
            long requests = s.httpRequests.perProvider.get(p);
            long charged = requests - s.notModified.perProvider.getOrDefault(p, 0L);
            rows.add(new String[] {"Charged requests: " + p, charged + " of " + requests + " (" + pct + "%)"});
        });
        rows.add(new String[] {"Cache hits: total", String.valueOf(s.cacheHits.total)});
        s.cacheHits.perProvider.forEach((p, n) -> rows.add(new String[] {"Cache hits: " + p, String.valueOf(n)}));
        return rows;
    }

    private static String findingLine(ReleaseReport.Finding f) {
        String label = f.severity != null ? f.severity : "UNKNOWN";
        String cve = f.cve != null ? f.cve : "";
        String summary = f.summary != null ? f.summary.replaceAll("\\s+", " ").trim() : "";
        if (summary.length() > 60) summary = summary.substring(0, 57) + "...";
        return String.format("%-19s %-26s %-8s %-4s %s", f.id, cve, label, f.score != null ? f.score : "", summary);
    }

    // -----------------------------------------------------------------------
    // Markdown
    // -----------------------------------------------------------------------

    private static String markdown(ReleaseReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# Quarkus releases and CVEs\n\n");
        md.append("## Statistics\n\n| | |\n|---|---|\n");
        for (String[] row : statisticsRows(report.statistics)) {
            md.append("| ").append(mdCell(row[0])).append(" | ").append(mdCell(row[1])).append(" |\n");
        }
        md.append("\nAdvisories are today's knowledge applied to every release, so an older release can show CVEs "
                + "that were published long after it shipped. Newest first; each release shows what changed "
                + "compared to the next-older one below it.\n");

        for (ReleaseReport.ReleaseGroup group : report.releaseGroups) {
            md.append("\n## Quarkus ").append(group.releaseGroup).append("\n\n");
            md.append(group.releases.size()).append(" release").append(group.releases.size() == 1 ? "" : "s").append("\n");

            for (ReleaseReport.Release r : group.releases) {
                md.append("\n### ").append(r.version).append("\n\n");
                md.append("Released ").append(r.date != null ? r.date : "(date unknown)").append(" - **")
                        .append(r.cves.active.count()).append(" active**\n");
                if (r.first) {
                    mdFindings(md, "Active", r.cves.active);
                    continue;
                }
                if (r.cves.resolved.count() == 0 && r.cves.introduced.count() == 0) {
                    md.append("\nNo change in advisories.\n");
                }
                mdChanges(md, "Resolved", r.cves.resolved);
                mdChanges(md, "Introduced", r.cves.introduced);
                if (r.mostRecent) mdFindings(md, "Still active in " + r.version, r.cves.active);
            }
        }
        return md.toString();
    }

    private static void mdFindings(StringBuilder md, String title, ReleaseReport.Split<ReleaseReport.Finding> findings) {
        md.append("\n#### ").append(title).append("\n");
        if (findings.count() == 0) {
            md.append("\nNone.\n");
            return;
        }
        mdFindingTable(md, "Core & extensions", findings.core);
        mdFindingTable(md, "Dependencies", findings.dependencies);
    }

    private static void mdFindingTable(StringBuilder md, String name, List<ReleaseReport.Finding> findings) {
        if (findings.isEmpty()) return;
        md.append("\n**").append(name).append(" (").append(findings.size()).append(")**\n\n");
        md.append("| ID | CVE | Severity | Score | Summary |\n|---|---|---|---|---|\n");
        for (ReleaseReport.Finding f : findings) md.append(mdRow(f)).append(" |\n");
    }

    private static void mdChanges(StringBuilder md, String title, ReleaseReport.Split<ReleaseReport.Change> changes) {
        if (changes.count() == 0) return;
        md.append("\n#### ").append(title).append("\n");
        mdChangeTable(md, "Core & extensions", changes.core);
        mdChangeTable(md, "Dependencies", changes.dependencies);
    }

    private static void mdChangeTable(StringBuilder md, String name, List<ReleaseReport.Change> changes) {
        if (changes.isEmpty()) return;
        md.append("\n**").append(name).append(" (").append(changes.size()).append(")**\n\n");
        md.append("| ID | CVE | Severity | Score | Summary | Because |\n|---|---|---|---|---|---|\n");
        for (ReleaseReport.Change ch : changes) {
            md.append(mdRow(ch.finding)).append(" | ").append(mdCell(ch.cause)).append(" |\n");
        }
    }

    /** A table row without the closing pipe, so callers can append more cells. */
    private static String mdRow(ReleaseReport.Finding f) {
        return "| " + mdCell(f.id) + " | " + mdCell(f.cve) + " | " + mdCell(f.severity != null ? f.severity : "UNKNOWN")
                + " | " + mdCell(f.score) + " | " + mdCell(f.summary);
    }

    private static String mdCell(String s) {
        if (s == null) return "";
        return s.replace("\r", " ").replace("\n", " ").replace("|", "\\|").trim();
    }
}
