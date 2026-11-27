package io.quarkus.cve.util;

import io.quarkus.cve.model.UnifiedVuln;

/**
 * Static helpers for pretty-printing UnifiedVuln records to the terminal.
 */
public final class Printer {

    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String GREEN  = "\u001B[32m";
    private static final String DIM    = "\u001B[2m";
    private static final String MAGENTA = "\u001B[35m";

    private Printer() {}

    // -----------------------------------------------------------------------
    // Summary line (list view)
    // -----------------------------------------------------------------------

    public static void printSummaryLine(UnifiedVuln v) {
        String id       = pad(v.id, 22);
        String cve      = v.cveId != null ? v.cveId : "";
        String score    = formatScore(v);
        String label    = v.severity != null ? v.severity : "UNKNOWN";
        String summary  = v.summary != null ? v.summary : "";
        if (summary.length() > 50) summary = summary.substring(0, 47) + "...";
        String date     = v.published != null && v.published.length() >= 10 ? v.published.substring(0, 10) : "";

        String srcTag = DIM + (v.source == UnifiedVuln.Source.GITHUB ? "[GH] " : "[OSV]") + RESET;

        // widths match printTableHeader: ID 22, SRC 5, SEVERITY 8, SCORE 6, PUBLISHED 10
        System.out.printf("%s%-22s%s  %s  %s%-8s%s  %s%-6s%s  %s%-10s%s  %s%s%s%n",
                BOLD + CYAN, id, RESET,
                srcTag,
                severityColor(label), label, RESET,
                DIM, score, RESET,
                DIM, date, RESET,
                DIM, summary, RESET);

        if (cve != null && !cve.isBlank()) {
            System.out.printf("  %s-> %s%s%n", DIM, cve, RESET);
        }
        if (v.affectedReleases != null) {
            System.out.printf("  %s   Quarkus: %s%s%n", DIM, releasesText(v, 100), RESET);
        }
    }

    /** Affected releases for display, cut off at {@code max} chars (the full list is in {@code get}). */
    private static String releasesText(UnifiedVuln v, int max) {
        if (v.affectedReleases.isEmpty()) return "no known release affected";
        String text = String.join(", ", v.affectedReleases);
        return text.length() <= max ? text : text.substring(0, text.lastIndexOf(", ", max - 4) + 1) + " ...";
    }

    // -----------------------------------------------------------------------
    // Full detail (get view)
    // -----------------------------------------------------------------------

    public static void printDetail(UnifiedVuln v) {
        line();
        header("ID",        v.id);
        if (v.cveId != null) header("CVE",    v.cveId);
        header("Published", v.published);
        header("Modified",  v.modified);
        if (v.withdrawn != null) header(RED + "Withdrawn", v.withdrawn + RESET);
        if (v.aliases != null && !v.aliases.isEmpty()) {
            header("Aliases", String.join(", ", v.aliases));
        }
        header("Source",  v.source != null ? v.source.name() : "");
        line();

        if (v.summary != null) {
            System.out.println(BOLD + v.summary + RESET);
            System.out.println();
        }

        if (v.severity != null || v.cvssScore != null) {
            System.out.println(BOLD + "Severity:" + RESET);
            String label = v.severity != null ? v.severity : "UNKNOWN";
            String score = v.cvssScore != null ? v.cvssScore : "";
            String vec   = v.cvssVector != null && !v.cvssVector.equals(v.cvssScore)
                    ? "  (" + v.cvssVector + ")" : "";
            System.out.printf("  %s%s%s  %s%s%s%n",
                    severityColor(label) + BOLD, label, RESET,
                    DIM, score + vec, RESET);
            System.out.println();
        }

        if (v.affectedReleases != null) {
            System.out.println(BOLD + "Affected Quarkus releases:" + RESET);
            System.out.println("  " + (v.affectedReleases.isEmpty()
                    ? "no known platform release is affected"
                    : String.join(", ", v.affectedReleases)));
            System.out.println();
        }

        if (v.details != null && !v.details.isBlank()) {
            System.out.println(BOLD + "Details:" + RESET);
            System.out.println(wrapText(v.details, 100));
            System.out.println();
        }

        if (v.affected != null && !v.affected.isEmpty()) {
            System.out.println(BOLD + "Affected packages:" + RESET);
            for (UnifiedVuln.AffectedPkg a : v.affected) {
                System.out.println("  " + CYAN + a.ecosystem + ":" + a.name + RESET);
                if (a.introducedVersion  != null) System.out.println("      introduced:    " + a.introducedVersion);
                if (a.fixedVersion       != null) System.out.println("      " + GREEN + "fixed:         " + a.fixedVersion + RESET);
                if (a.lastAffectedVersion != null) System.out.println("      " + YELLOW + "last_affected: " + a.lastAffectedVersion + RESET);
            }
            System.out.println();
        }

        if (v.references != null && !v.references.isEmpty()) {
            System.out.println(BOLD + "References:" + RESET);
            v.references.stream().distinct().forEach(r ->
                    System.out.println("  " + r));
        }
        line();
    }

    // -----------------------------------------------------------------------
    // Table header
    // -----------------------------------------------------------------------

    public static void printTableHeader() {
        System.out.printf("%s%-22s  %-5s  %-8s  %-6s  %-10s  %s%s%n",
                BOLD, "ID", "SRC", "SEVERITY", "SCORE", "PUBLISHED", "SUMMARY", RESET);
        System.out.println("-".repeat(112));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static void header(String key, String value) {
        if (value != null) System.out.printf("%-14s %s%n", key + ":", value);
    }

    private static void line() {
        System.out.println(DIM + "-".repeat(110) + RESET);
    }

    private static String pad(String s, int width) {
        if (s == null) return " ".repeat(width);
        return s.length() >= width ? s : s + " ".repeat(width - s.length());
    }

    private static String formatScore(UnifiedVuln v) {
        if (v.cvssScore != null && !v.cvssScore.isBlank()) return v.cvssScore;
        return "";
    }

    private static String severityColor(String label) {
        if (label == null) return "";
        return switch (label.toUpperCase()) {
            case "CRITICAL" -> RED + BOLD;
            case "HIGH"     -> RED;
            case "MEDIUM"   -> YELLOW;
            case "LOW"      -> GREEN;
            default         -> DIM;
        };
    }

    private static String wrapText(String text, int width) {
        StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.length() <= width) {
                sb.append(line).append("\n");
            } else {
                int pos = 0;
                while (pos < line.length()) {
                    int end = Math.min(pos + width, line.length());
                    if (end < line.length()) {
                        int space = line.lastIndexOf(' ', end);
                        if (space > pos) end = space + 1;
                    }
                    sb.append(line, pos, end).append("\n");
                    pos = end;
                }
            }
        }
        return sb.toString().stripTrailing();
    }
}
