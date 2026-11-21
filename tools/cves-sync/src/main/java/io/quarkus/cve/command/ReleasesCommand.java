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
 * Advisory view of every release in releases.yaml: which advisories affect which releases, through which
 * components, as a flat list and as ranges over the release tree. releases.yaml owns the list of releases,
 * their dates and lineage; the BOM of each release (platform where releases.yaml lists one, else core)
 * says what it ships.
 *
 * Examples:
 *   cve-util releases
 *   cve-util releases --releases-input _data/releases.yaml --format yaml --output _data/cves.yaml
 *
 * Exit status: 0 on success, 1 when the report could not be produced or written (with --output also
 * when there are no releases to report, so that no file is replaced by an empty result), 2 for a bad option.
 */
@Command(
    name = "releases",
    mixinStandardHelpOptions = true,
    description = "Show which advisories affect which Quarkus releases."
)
public class ReleasesCommand implements Callable<Integer> {

    @Inject
    ReleaseReportService reportService;

    @ParentCommand
    CveUtilCommand parent;

    @Spec
    CommandSpec spec;

    @Option(
        names = {"--format"},
        paramLabel = "<format>",
        description = "Output format: plaintext (default), json, yaml"
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
        description = "Path to releases.yaml, which lists the releases (default: _data/releases.yaml)",
        defaultValue = "_data/releases.yaml"
    )
    String releasesInput;

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
            report = reportService.build(resolveReleasesFile(releasesInput));
        } catch (IllegalStateException ex) {
            System.err.println("Error: " + ex.getMessage());
            return 1;
        } catch (IOException ex) {
            System.err.println("Error: could not read " + releasesInput + ": " + ex.getMessage());
            return 1;
        }

        fillStatistics(report);

        if (report.releases.isEmpty()) {
            String none = "No Quarkus releases found in " + releasesInput + ".";
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
        } catch (IllegalStateException ex) {
            System.err.println("Error: " + ex.getMessage());
            return 1;
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
