package io.quarkus.cve.command;

import io.quarkus.cve.model.ReleaseReport;
import io.quarkus.cve.service.ReleaseReportService;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.OutputFormat;
import io.quarkus.cve.util.ReleaseReportRenderer;
import io.quarkus.cve.util.Stats;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/**
 * Release-centric view: for each Quarkus major.minor line, the path from its first to its last
 * release, showing which advisories are active, which each release resolved or introduced, and why.
 *
 * Examples:
 *   cve-util releases                       # 3.27 and onwards
 *   cve-util releases --from 3.20 --to 3.27
 *   cve-util releases --from 3.27 --to 3.27 --format markdown
 *   cve-util releases --format yaml --output cves.yaml
 *
 * Release groups and the releases within them are listed newest first.
 *
 * Exit status: 0 on success, 1 when the report could not be produced or written (with --output also
 * when there are no releases to report, so that no file is replaced by an empty result), 2 for a bad option.
 */
@Command(
    name = "releases",
    mixinStandardHelpOptions = true,
    description = "Show, per Quarkus major.minor line, which CVEs are active in each release and which release resolved them."
)
public class ReleasesCommand implements Callable<Integer> {

    @Inject
    ReleaseReportService reportService;

    @ParentCommand
    CveUtilCommand parent;

    @Spec
    CommandSpec spec;

    @Option(
        names = {"--from"},
        paramLabel = "<major.minor>",
        description = "First major.minor line to include (default: ${DEFAULT-VALUE})",
        defaultValue = "3.27"
    )
    String from;

    @Option(
        names = {"--to"},
        paramLabel = "<major.minor>",
        description = "Last major.minor line to include (default: the newest)"
    )
    String to;

    @Option(
        names = {"--format"},
        paramLabel = "<format>",
        description = "Output format: plaintext-ansi-color (default), plaintext-nocolor, markdown, json, yaml"
    )
    String format;

    @Option(
        names = {"-o", "--output"},
        paramLabel = "<file>",
        description = "Write the report to <file> instead of standard output. Always UTF-8 without a byte-order mark, "
                + "whatever the platform or shell, any --format. Written atomically: on failure an existing file is left as it was."
    )
    Path output;

    @Option(
        names = {"--releases-input"},
        paramLabel = "<file>",
        description = "Path to releases.yaml containing manual release metadata (default: _data/releases.yaml)",
        defaultValue = "_data/releases.yaml"
    )
    String releasesInput;

    @Option(
        names = {"--predict-upcoming"},
        negatable = true,
        description = "Predict upcoming releases from GitHub milestones and pre-releases (default: true)",
        defaultValue = "true"
    )
    boolean predictUpcoming = true;

    @Option(
        names = {"--releases-org"},
        description = "GitHub organization for releases (default: quarkusio)",
        defaultValue = "quarkusio"
    )
    String releasesOrg = "quarkusio";

    @Option(
        names = {"--releases-repo"},
        description = "GitHub repository for releases (default: quarkus)",
        defaultValue = "quarkus"
    )
    String releasesRepo = "quarkus";

    @Option(
        names = {"--json"},
        description = "Shorthand for --format json"
    )
    boolean json;

    private java.io.File resolveReleasesFile(String path) {
        if (path == null) path = "_data/releases.yaml";
        java.io.File f = new java.io.File(path);
        if (f.exists()) return f;
        java.io.File relParent = new java.io.File("../../" + path);
        if (relParent.exists()) return relParent;
        return f;
    }

    @Override
    public Integer call() {
        if (parent != null) parent.applyVerbosity();

        OutputFormat outputFormat;
        try {
            outputFormat = json ? OutputFormat.JSON : format == null ? OutputFormat.DEFAULT : OutputFormat.parse(format);
        } catch (IllegalArgumentException ex) {
            System.err.println("Error: " + ex.getMessage());
            return 2;
        }

        ReleaseReport report;
        try {
            report = reportService.build(from, to, resolveReleasesFile(releasesInput), releasesOrg, releasesRepo, predictUpcoming);
        } catch (IllegalArgumentException ex) {
            System.err.println("Error: " + ex.getMessage() + " (expected e.g. 3.27)");
            return 2;
        }

        fillStatistics(report);

        if (report.releaseGroups.isEmpty()) {
            String none = "No Quarkus releases found for " + from + (to != null ? " to " + to : " onwards") + ".";
            if (output != null) {
                System.err.println(none + " Not writing " + output + ".");
                return 1;
            }
            System.out.println(none);
            return 0;
        }

        // Always UTF-8, whatever the platform charset: advisory text is not ASCII, and a data file
        // (yaml / json) that is not valid UTF-8 cannot be parsed.
        byte[] bytes;
        try {
            bytes = (ReleaseReportRenderer.render(report, outputFormat) + System.lineSeparator())
                    .getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println(outputFormat.cliName + " rendering failed: " + ex.getMessage());
            return 1;
        }

        if (output == null) {
            System.out.write(bytes, 0, bytes.length);
            System.out.flush();
            return 0;
        }
        try {
            writeAtomically(output, bytes);
        } catch (IOException ex) {
            System.err.println("Error: could not write " + output + ": " + ex.getMessage());
            return 1;
        }
        Log.log(1, "Wrote %s report to %s (%d bytes)", outputFormat.cliName, output, bytes.length);
        return 0;
    }

    /** Temp file in the target directory + move, so an interrupted or failed run never leaves a half-written file. */
    private static void writeAtomically(Path target, byte[] bytes) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, ".tmp-", ".part");
        try {
            Files.write(tmp, bytes);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Scan time, arguments, duration and the request / cache counters, as of the end of the build. */
    private void fillStatistics(ReleaseReport report) {
        ReleaseReport.Statistics s = report.statistics;
        s.scanTime = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        s.arguments = List.copyOf(spec.root().commandLine().getParseResult().originalArgs());
        s.executionMillis = Stats.elapsedMillis();
        s.httpRequests = ReleaseReport.Counts.of(Stats.httpRequests());
        s.notModified = ReleaseReport.Counts.of(Stats.notModified());
        s.httpRequests.perProvider.forEach((provider, requests) -> {
            if (requests > 0) {
                long free = s.notModified.perProvider.getOrDefault(provider, 0L);
                s.chargedPercent.put(provider, Math.round((requests - free) * 1000.0 / requests) / 10.0);
            }
        });
        s.cacheHits = ReleaseReport.Counts.of(Stats.cacheHits());
    }
}
