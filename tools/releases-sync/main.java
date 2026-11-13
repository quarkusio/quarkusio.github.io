///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus.platform:quarkus-bom:3.36.3@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-config-yaml
//DEPS io.quarkus:quarkus-rest-client
//DEPS io.quarkus:quarkus-smallrye-fault-tolerance
//Q:CONFIG quarkus.rest-client.connect-timeout=10000
//Q:CONFIG quarkus.rest-client.read-timeout=30000
//DEPS org.yaml:snakeyaml:2.2
//DEPS org.apache.maven:maven-artifact:3.9.9
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml
//SOURCES io/quarkiverse/roq/data/runtime/annotations/DataMapping.java
//SOURCES ../../src/main/java/io/quarkusio/releases/Releases.java
//SOURCES ../../src/main/java/io/quarkusio/releases/Release.java
//SOURCES ../../src/main/java/io/quarkusio/releases/Micro.java
//SOURCES ../../src/main/java/io/quarkusio/releases/EnterpriseOffering.java
//SOURCES io/quarkusio/releases/sync/WikiReleasePlanning.java
//SOURCES io/quarkusio/releases/sync/EnterpriseReleaseDates.java
//SOURCES io/quarkusio/releases/sync/MavenRepositories.java
//SOURCES io/quarkusio/releases/sync/BranchLineage.java
//SOURCES io/quarkusio/releases/sync/GitHubReleases.java
//SOURCES io/quarkusio/releases/sync/ReleasesSync.java

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import io.quarkusio.releases.Releases;
import io.quarkusio.releases.sync.GitHubReleases;
import io.quarkusio.releases.sync.GitHubReleases.FetchResult;
import io.quarkusio.releases.sync.GitHubReleases.GitHubMilestone;
import io.quarkusio.releases.sync.MavenRepositories;
import io.quarkusio.releases.sync.ReleasesSync;
import io.quarkusio.releases.sync.ReleasesSync.UpcomingRelease;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Keeps _data/releases.yaml current. The sync itself is in ReleasesSync (merge logic) and GitHubReleases (API);
 * this script reads and writes the file.
 */
@CommandLine.Command(name = "sync-releases", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {

    @ConfigProperty(name = "GITHUB_TOKEN")
    Optional<String> token;

    @Inject
    MavenRepositories mavenRepositories;

    @ConfigProperty(name = "RELEASES_ORG", defaultValue = "quarkusio")
    String org;

    @ConfigProperty(name = "RELEASES_REPO", defaultValue = "quarkus")
    String repo;

    @ConfigProperty(name = "RELEASES_INPUT", defaultValue = "_data/releases.yaml")
    File inputFile;

    @ConfigProperty(name = "RELEASES_OUTPUT", defaultValue = "_data/releases.yaml")
    File outputFile;

    @CommandLine.Option(names = {"--dry-run"}, description = "Show what would be changed without writing files")
    boolean dryRun = false;

    @CommandLine.Option(names = {"--no-predict-upcoming"}, negatable = true, description = "Predict and add upcoming releases based on milestones and patterns")
    boolean predictUpcoming = true;

    @Override
    public Integer call() throws Exception {
        Log.infof("🚀 Syncing releases for %s/%s", org, repo);
        Log.infof("📁 Input: %s", inputFile.getPath());
        Log.infof("📁 Output: %s", outputFile.getPath());

        if (dryRun) {
            Log.infof("🔍 DRY RUN MODE - No files will be modified");
        }

        try {
            // Load existing releases data
            Releases existingData = loadExistingData();
            Log.infof("📋 Loaded existing data with %d releases", existingData.releases().size());

            if (token.isEmpty()) {
                Log.warnf("GITHUB_TOKEN is not set, using anonymous GitHub API access (lower rate limit)");
            }
            GitHubReleases github = new GitHubReleases(org, repo, token.orElse(null));

            // Fetch GitHub releases (both final and pre-releases in one pass)
            FetchResult fetchResult = github.fetchReleases();
            Log.infof("Fetched %d releases from GitHub (%d pre-releases)",
                     fetchResult.releases().size(), fetchResult.preReleases().size());

            // Fetch upcoming release predictions if enabled
            List<UpcomingRelease> upcomingReleases = new ArrayList<>();
            if (predictUpcoming) {
                try {
                    upcomingReleases = ReleasesSync.predictUpcoming(fetchResult.releases(), fetchResult.preReleases(),
                            milestones(github));
                    Log.infof("Predicted %d upcoming releases", upcomingReleases.size());
                } catch (Exception e) {
                    Log.warnf("Upcoming release prediction failed: %s", e.getMessage());
                    Log.infof("Will use '[version tbd]' as fallback");
                }
            }

            // The release planning wiki is authoritative for upcoming releases
            List<UpcomingRelease> wikiUpcoming = predictUpcoming ? wikiUpcoming(github, fetchResult) : List.of();

            // Process and merge data
            Releases updatedData = ReleasesSync.sync(existingData, fetchResult.releases(), upcomingReleases,
                    wikiUpcoming, github::fetchMergeBase);
            updatedData = ReleasesSync.applyEnterpriseReleaseDates(updatedData, "rhbq", mavenRepositories::rhbqDates);
            updatedData = ReleasesSync.applyEnterpriseReleaseDates(updatedData, "ibm", mavenRepositories::ibmDates);
            ReleasesSync.validateEnterpriseReleaseDates(updatedData);
            Log.infof("🔄 Processed into %d release entries", updatedData.releases().size());

            // Generate YAML output
            String yamlOutput = generateYaml(updatedData);

            if (dryRun) {
                showDiff(yamlOutput);
                Log.infof("✅ Dry run completed successfully");
            } else {
                // Create backup
                if (inputFile.exists()) {
                    File backup = new File(inputFile.getPath() + ".bak");
                    Files.copy(inputFile.toPath(), backup.toPath(),
                              java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Log.infof("💾 Created backup: %s", backup.getPath());
                }

                // Write output
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(yamlOutput);
                }
                Log.infof("✅ Release data updated successfully!");
                Log.infof("📁 Output written to: %s", outputFile.getPath());
            }

            return 0;

        } catch (Exception e) {
            Log.errorf("❌ Error: %s", e.getMessage());
            if (Log.isDebugEnabled()) {
                Log.debug("Stack trace:", e);
            }
            return 1;
        }
    }

    private Releases loadExistingData() throws Exception {
        if (!inputFile.exists()) {
            throw new RuntimeException("Input file not found: " + inputFile.getPath() +
                "\nThe releases.yaml file must exist with policy, support_providers, and eol_support sections.");
        }
        // Same mapping as the website's Releases data bean
        YAMLMapper mapper = YAMLMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        return mapper.readValue(inputFile, Releases.class);
    }

    private static List<GitHubMilestone> milestones(GitHubReleases github) {
        try {
            return github.fetchOpenMilestones();
        } catch (Exception e) {
            Log.warnf("Failed to fetch milestones: %s", e.getMessage());
            return List.of();
        }
    }

    private static List<UpcomingRelease> wikiUpcoming(GitHubReleases github, FetchResult fetched) {
        try {
            String page = github.fetchReleasePlanningWiki();
            if (page == null) {
                return List.of();
            }
            List<UpcomingRelease> upcoming = ReleasesSync.wikiUpcoming(page, fetched.releases(),
                    LocalDate.now(ZoneOffset.UTC));
            Log.infof("Found %d upcoming releases in the release planning wiki", upcoming.size());
            return upcoming;
        } catch (Exception e) {
            Log.warnf("Failed to read release planning wiki: %s", e.getMessage());
            return List.of();
        }
    }

    private void showDiff(String newContent) {
        try {
            if (!inputFile.exists()) {
                Log.infof("📄 NEW FILE WOULD BE CREATED:");
                Log.infof("================================================================================");
                System.out.println(newContent);
                Log.infof("================================================================================");
                return;
            }

            // Read current file content
            String currentContent = Files.readString(inputFile.toPath());

            // Check if there are any differences
            if (currentContent.equals(newContent)) {
                Log.infof("✅ NO CHANGES - file is already up to date");
                return;
            }

            // Try to use system diff command for better output
            if (trySystemDiff(currentContent, newContent)) {
                return;
            }

            // Fallback to simple line-by-line comparison
            showSimpleDiff(currentContent, newContent);

        } catch (Exception e) {
            Log.warnf("⚠️  Could not generate diff: %s", e.getMessage());
            Log.infof("📄 FULL NEW CONTENT:");
            Log.infof("================================================================================");
            System.out.println(newContent);
            Log.infof("================================================================================");
        }
    }

    private boolean trySystemDiff(String currentContent, String newContent) {
        try {
            // Create temporary files
            File tempCurrent = File.createTempFile("releases-current", ".yaml");
            File tempNew = File.createTempFile("releases-new", ".yaml");

            Files.writeString(tempCurrent.toPath(), currentContent);
            Files.writeString(tempNew.toPath(), newContent);

            // Try to run diff command
            Process process = new ProcessBuilder("diff", "-u",
                "--label=" + inputFile.getPath() + " (current)",
                "--label=" + inputFile.getPath() + " (updated)",
                tempCurrent.getAbsolutePath(),
                tempNew.getAbsolutePath())
                .start();

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (finished) {
                String diffOutput = new String(process.getInputStream().readAllBytes());

                if (!diffOutput.trim().isEmpty()) {
                    Log.infof("📋 CHANGES TO BE MADE:");
                    Log.infof("================================================================================");
                    System.out.println(diffOutput);
                    Log.infof("================================================================================");

                    // Count changed lines
                    long changedLines = diffOutput.lines()
                        .filter(line -> line.startsWith("+") || line.startsWith("-"))
                        .filter(line -> !line.startsWith("+++") && !line.startsWith("---"))
                        .count();
                    Log.infof("📊 Summary: %d lines would change", changedLines);
                }

                // Clean up temp files
                tempCurrent.delete();
                tempNew.delete();
                return true;
            }

        } catch (Exception e) {
            Log.debugf("System diff not available or failed: %s", e.getMessage());
        }

        return false;
    }

    private void showSimpleDiff(String currentContent, String newContent) {
        List<String> currentLines = currentContent.lines().collect(Collectors.toList());
        List<String> newLines = newContent.lines().collect(Collectors.toList());

        Log.infof("📋 CHANGES TO BE MADE:");
        Log.infof("================================================================================");

        System.out.println("--- " + inputFile.getPath() + " (current)");
        System.out.println("+++ " + inputFile.getPath() + " (updated)");

        int maxLines = Math.max(currentLines.size(), newLines.size());
        int changedLines = 0;

        for (int i = 0; i < maxLines; i++) {
            String currentLine = i < currentLines.size() ? currentLines.get(i) : null;
            String newLine = i < newLines.size() ? newLines.get(i) : null;

            if (currentLine == null) {
                System.out.println("+" + newLine);
                changedLines++;
            } else if (newLine == null) {
                System.out.println("-" + currentLine);
                changedLines++;
            } else if (!currentLine.equals(newLine)) {
                System.out.println("-" + currentLine);
                System.out.println("+" + newLine);
                changedLines += 2;
            }
            // Skip unchanged lines in simple diff to keep it concise
        }

        Log.infof("================================================================================");
        Log.infof("📊 Summary: %d lines would change", changedLines);
    }


    static class DateScalar {
        final String value;
        DateScalar(String value) { this.value = value; }
    }

    static class QuotedScalar {
        final String value;
        QuotedScalar(String value) { this.value = value; }
    }

    static class ReleasesRepresenter extends Representer {
        ReleasesRepresenter(DumperOptions options) {
            super(options);
            this.nullRepresenter = new Represent() {
                @Override
                public Node representData(Object data) {
                    return representScalar(Tag.NULL, "", DumperOptions.ScalarStyle.PLAIN);
                }
            };
            this.representers.put(DateScalar.class, new Represent() {
                @Override
                public Node representData(Object data) {
                    return representScalar(new Tag("tag:yaml.org,2002:timestamp"), ((DateScalar) data).value, DumperOptions.ScalarStyle.PLAIN);
                }
            });
            this.representers.put(QuotedScalar.class, new Represent() {
                @Override
                public Node representData(Object data) {
                    return representScalar(Tag.STR, ((QuotedScalar) data).value, DumperOptions.ScalarStyle.DOUBLE_QUOTED);
                }
            });
        }
    }

    private String generateYaml(Releases data) throws Exception {
        // Preserve everything before "releases:" from the original file
        String original = Files.readString(inputFile.toPath());
        int relIdx = original.indexOf("\nreleases:\n");
        String header = relIdx >= 0 ? original.substring(0, relIdx + 1) : "";

        // Generate only the releases section
        DumperOptions relOpts = new DumperOptions();
        relOpts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        relOpts.setIndent(4);
        relOpts.setIndicatorIndent(2);
        relOpts.setWidth(120);

        Yaml relYaml = new Yaml(new ReleasesRepresenter(relOpts), relOpts);

        List<Map<String, Object>> releasesList = new ArrayList<>();
        for (Releases.Entry release : data.releases()) {
            releasesList.add(releaseToMap(release));
        }
        Map<String, Object> relMap = new LinkedHashMap<>();
        relMap.put("releases", releasesList);

        // Add blank lines between release entries to match existing style
        String relSection = relYaml.dump(relMap);
        relSection = relSection.replaceAll("\n(  - version:)", "\n\n$1");

        return header + relSection;
    }

    private Map<String, Object> releaseToMap(Releases.Entry release) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", new QuotedScalar(release.version()));

        if (release.releaseDate() != null) {
            map.put("release_date", new DateScalar(release.releaseDate()));
        }

        if (release.plannedDate() != null) {
            map.put("planned_date", new DateScalar(release.plannedDate()));
        }

        if (release.eolDate() != null) {
            map.put("eol_date", new DateScalar(release.eolDate()));
        }

        map.put("lts", release.lts());

        if (release.upcoming()) {
            map.put("upcoming", true);
            if (release.proposal()) {
                map.put("proposal", true);
            }
        }

        if (release.link() != null) {
            map.put("link", release.link());
        }

        if (release.branchedFrom() != null) {
            map.put("branched_from", release.branchedFrom());
        }

        putProvider(map, "rhbq", release.rhbq());
        putProvider(map, "ibm", release.ibm());
        putProvider(map, "herodevs", release.herodevs());

        // Last, as it is long. Block mappings, not one-line {...} flow mappings: pt.quarkus.io translates
        // this file with po4a, whose YAML::Tiny parser rejects inline lists and dictionaries
        if (release.micros() != null && !release.micros().isEmpty()) {
            List<Map<String, Object>> micros = new ArrayList<>();
            for (Releases.MicroEntry micro : release.micros()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("version", new QuotedScalar(micro.version()));
                if (micro.releaseDate() != null) {
                    m.put("release_date", new DateScalar(micro.releaseDate()));
                }
                micros.add(m);
            }
            map.put("micros", micros);
        }

        return map;
    }

    private static void putProvider(Map<String, Object> map, String name, Releases.EnterpriseDates dates) {
        if (dates == null) {
            return;
        }
        Map<String, Object> block = new LinkedHashMap<>();
        putProviderValue(block, "release_date", dates.releaseDate());
        putProviderValue(block, "eol_date", dates.eolDate());
        if (dates.branchedFrom() != null) {
            block.put("branched_from", dates.branchedFrom());
        }
        if (dates.openEnded()) {
            block.put("open_ended", true);
        }
        map.put(name, block);
    }

    private static void putProviderValue(Map<String, Object> block, String key, String value) {
        if (value == null) {
            return;
        }
        // Dates are written as plain YAML dates
        try {
            block.put(key, new DateScalar(LocalDate.parse(value).toString()));
        } catch (Exception e) {
            block.put(key, value);
        }
    }
}
