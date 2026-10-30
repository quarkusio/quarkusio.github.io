package io.quarkus.cve.command;

import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.service.QuarkusArtifactService;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.Printer;
import io.quarkus.cve.util.Retry;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.quarkus.cve.client.OsvApiClient;
import io.quarkus.cve.model.OsvQueryRequest;
import io.quarkus.cve.model.OsvQueryResponse;
import io.quarkus.cve.model.OsvVulnerability;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * List vulnerabilities for Quarkus packages from OSV and GitHub Advisory.
 *
 * Examples:
 *   cve-util list                                           # Quarkus core + extensions (io.quarkus.*)
 *   cve-util list --include-transitive                     # ... plus key transitive deps
 *   cve-util list --package io.quarkus:quarkus-core        # single artifact
 *   cve-util list --package io.quarkus:quarkus-core --version 3.2.0
 */
@Command(
    name = "list",
    mixinStandardHelpOptions = true,
    description = "List CVEs/vulnerabilities for Quarkus packages from OSV and GitHub Advisory."
)
public class ListCommand implements Runnable {

    @RestClient
    @Inject
    OsvApiClient osvApiClient;

    @Inject
    QuarkusArtifactService artifactService;

    @ParentCommand
    CveUtilCommand parent;

    @Option(
        names = {"-p", "--package"},
        description = "Single Maven groupId:artifactId to query instead of all Quarkus packages"
    )
    String packageName;

    @Option(
        names = {"-e", "--ecosystem"},
        description = "OSV ecosystem (default: Maven)",
        defaultValue = "Maven"
    )
    String ecosystem;

    @Option(
        names = {"--version"},
        description = "Filter vulnerabilities to a specific package version"
    )
    String version;

    @Option(
        names = {"--include-transitive"},
        description = "Also scan key transitive dependencies (netty, vertx, ...), not just Quarkus core and extensions"
    )
    boolean includeTransitive;

    @Option(
        names = {"--json"},
        description = "Output raw JSON (one vulnerability per line)"
    )
    boolean json;

    @Override
    public void run() {
        if (parent != null) parent.applyVerbosity();
        List<UnifiedVuln> vulns;

        if (packageName != null) {
            vulns = querySinglePackage(packageName, version);
        } else {
            List<String> artifacts = artifactService.discoverArtifacts(includeTransitive);
            System.err.printf("Scanning %d artifacts (Quarkus core + extensions%s) against OSV + GitHub...%n",
                    artifacts.size(), includeTransitive ? " + key transitive deps" : "");
            vulns = artifactService.queryAll(artifacts);
        }

        if (vulns.isEmpty()) {
            System.out.println("No vulnerabilities found.");
            return;
        }

        artifactService.annotateAffectedReleases(vulns);

        if (json) {
            for (UnifiedVuln v : vulns) {
                String releases = v.affectedReleases == null ? "null"
                        : v.affectedReleases.stream().map(r -> "\"" + r + "\"")
                                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
                System.out.printf("{\"id\":\"%s\",\"cve\":\"%s\",\"severity\":\"%s\",\"summary\":\"%s\",\"published\":\"%s\",\"affectedReleases\":%s}%n",
                        safe(v.id), safe(v.cveId), safe(v.severity),
                        v.summary != null ? v.summary.replace("\"", "\\\"") : "",
                        safe(v.published), releases);
            }
        } else {
            long ghCount = vulns.stream().filter(v -> v.source == UnifiedVuln.Source.GITHUB).count();
            System.out.printf("%nFound %d vulnerabilit%s (%d from OSV, %d from GitHub Advisory):%n%n",
                    vulns.size(), vulns.size() == 1 ? "y" : "ies",
                    vulns.size() - ghCount, ghCount);
            Printer.printTableHeader();
            for (UnifiedVuln v : vulns) {
                Printer.printSummaryLine(v);
            }
            System.out.printf("%n  Tip: run  cve-util get <ID>  for full details on any entry.%n%n");
        }
    }

    private List<UnifiedVuln> querySinglePackage(String pkg, String ver) {
        OsvQueryRequest req = ver != null
                ? OsvQueryRequest.forPackageVersion(ecosystem, pkg, ver)
                : OsvQueryRequest.forPackage(ecosystem, pkg);

        // id -> modified, so cached details can be reused when unchanged
        Map<String, String> deduped = new LinkedHashMap<>();
        String pageToken = null;
        Log.log(1, "Querying OSV for %s%s", pkg, ver != null ? " @ " + ver : "");
        do {
            req.pageToken = pageToken;
            try {
                Log.log(2, "OSV query page token=%s", pageToken);
                OsvQueryResponse page = Retry.call("OSV query " + pkg, () -> osvApiClient.query(req));
                if (page.vulns != null) {
                    for (OsvVulnerability v : page.vulns) {
                        if (v.id != null) deduped.put(v.id, v.modified);
                    }
                }
                pageToken = page.nextPageToken;
            } catch (Exception ex) {
                System.err.println("Error querying " + pkg + ": " + ex.getMessage());
                break;
            }
        } while (pageToken != null && !pageToken.isBlank());

        Log.log(1, "OSV: %d vulnerability IDs for %s", deduped.size(), pkg);
        return artifactService.fetchOsvDetails(deduped);
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
