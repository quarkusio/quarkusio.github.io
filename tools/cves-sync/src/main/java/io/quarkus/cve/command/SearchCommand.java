package io.quarkus.cve.command;

import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.service.QuarkusArtifactService;
import io.quarkus.cve.util.Printer;
import jakarta.inject.Inject;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Search OSV + GitHub Advisory for Quarkus vulnerabilities by keyword or severity.
 *
 * Examples:
 *   cve-util search --keyword "injection"
 *   cve-util search --keyword "bypass" --severity HIGH
 *   cve-util search --packages io.quarkus:quarkus-core,io.quarkus:quarkus-oidc
 */
@Command(
    name = "search",
    mixinStandardHelpOptions = true,
    description = "Search OSV and GitHub Advisory for Quarkus vulnerabilities by keyword or severity."
)
public class SearchCommand implements Runnable {

    @Inject
    QuarkusArtifactService artifactService;

    @ParentCommand
    CveUtilCommand parent;

    @Option(
        names = {"-k", "--keyword"},
        description = "Filter results where summary or details contain this keyword (case-insensitive)"
    )
    String keyword;

    @Option(
        names = {"-s", "--severity"},
        description = "Filter by minimum severity: LOW, MEDIUM, HIGH, CRITICAL"
    )
    String severity;

    @Option(
        names = {"-p", "--packages"},
        description = "Comma-separated list of groupId:artifactId packages (default: all Quarkus artifacts)"
    )
    String packages;

    @Option(
        names = {"--include-transitive"},
        description = "Also scan key transitive dependencies (netty, vertx, ...), not just Quarkus core and extensions"
    )
    boolean includeTransitive;

    @Override
    public void run() {
        if (parent != null) parent.applyVerbosity();

        List<String> targets = packages != null
                ? List.of(packages.split(","))
                : artifactService.discoverArtifacts(includeTransitive);

        System.err.printf("Scanning %d artifacts against OSV + GitHub...%n", targets.size());
        List<UnifiedVuln> results = artifactService.queryAll(targets);

        // Apply keyword filter
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            results = results.stream()
                    .filter(v -> contains(v.summary, kw) || contains(v.details, kw)
                            || contains(v.id, kw) || contains(v.cveId, kw)
                            || (v.aliases != null && v.aliases.stream().anyMatch(a -> contains(a, kw))))
                    .toList();
        }

        // Apply severity filter
        if (severity != null && !severity.isBlank()) {
            String min = severity.toUpperCase();
            int minOrdinal = severityOrdinal(min);
            results = results.stream()
                    .filter(v -> severityOrdinal(v.severity) >= minOrdinal)
                    .toList();
        }

        if (results.isEmpty()) {
            System.out.println("No matching vulnerabilities found.");
            return;
        }

        artifactService.annotateAffectedReleases(results);

        System.out.printf("%nFound %d match%s:%n%n",
                results.size(), results.size() == 1 ? "" : "es");
        Printer.printTableHeader();
        for (UnifiedVuln v : results) {
            Printer.printSummaryLine(v);
        }
        System.out.printf("%n  Tip: run  cve-util get <ID>  for full details on any entry.%n%n");
    }

    private static boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase().contains(keyword);
    }

    private static int severityOrdinal(String label) {
        if (label == null) return 0;
        return switch (label.toUpperCase()) {
            case "CRITICAL" -> 4;
            case "HIGH"     -> 3;
            case "MEDIUM"   -> 2;
            case "LOW"      -> 1;
            default         -> 0;
        };
    }
}
