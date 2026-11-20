package io.quarkus.cve.command;

import io.quarkus.cve.model.OsvVulnerability;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.service.QuarkusArtifactService;
import io.quarkus.cve.util.Printer;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Fetch and display full detail for a single vulnerability by ID.
 *
 * Examples:
 *   cve-util get GHSA-xxxx-xxxx-xxxx
 *   cve-util get CVE-2024-1234
 */
@Command(
    name = "get",
    mixinStandardHelpOptions = true,
    description = "Show full details for a vulnerability by its OSV/CVE/GHSA ID."
)
public class GetCommand implements Runnable {

    @Inject
    QuarkusArtifactService artifactService;

    @ParentCommand
    CveUtilCommand parent;

    @Parameters(
        index = "0",
        description = "Vulnerability ID — GHSA (e.g. GHSA-xxxx-xxxx-xxxx) or CVE (e.g. CVE-2024-1234)."
    )
    String id;

    @Option(
        names = {"--json"},
        description = "Print raw JSON instead of formatted output"
    )
    boolean json;

    @Override
    public void run() {
        if (parent != null) parent.applyVerbosity();
        OsvVulnerability raw;
        try {
            raw = artifactService.getOsvVulnerability(id);
        } catch (Exception ex) {
            System.err.println("Error fetching " + id + ": " + ex.getMessage());
            return;
        }

        if (raw == null) {
            System.err.println("Vulnerability not found: " + id);
            return;
        }

        UnifiedVuln v = UnifiedVuln.fromOsv(raw);
        artifactService.annotateAffectedReleases(java.util.List.of(v));

        if (json) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                        new com.fasterxml.jackson.databind.ObjectMapper()
                                .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                System.out.println(mapper.writeValueAsString(v));
            } catch (Exception ex) {
                System.err.println("JSON serialisation failed: " + ex.getMessage());
            }
        } else {
            Printer.printDetail(v);
        }
    }
}
