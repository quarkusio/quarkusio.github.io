///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus.platform:quarkus-bom:3.36.3@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-config-yaml
//DEPS org.yaml:snakeyaml:2.2

import io.quarkus.logging.Log;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "sync-workshops", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {

    @ConfigProperty(name = "GITHUB_TOKEN")
    String token;

    @ConfigProperty(name = "WORKSHOPS_INPUT", defaultValue = "_data/workshops.yaml")
    File inputFile;

    @ConfigProperty(name = "WORKSHOPS_OUTPUT", defaultValue = "_data/workshops.yaml")
    File outputFile;

    @CommandLine.Option(names = {"--dry-run"}, description = "Show what would be changed without writing files")
    boolean dryRun = false;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public Integer call() throws Exception {
        Log.infof("🚀 Syncing workshop repositories");
        Log.infof("📁 Input: %s", inputFile.getPath());
        Log.infof("📁 Output: %s", outputFile.getPath());

        if (dryRun) {
            Log.infof("🔍 DRY RUN MODE - No files will be modified");
        }

        try {
            // Load existing workshops data
            WorkshopsData existingData = loadExistingData();
            Log.infof("📋 Loaded existing data with %d workshops", existingData.workshops.size());

            // Fetch GitHub data for each repository
            List<Workshop> updatedWorkshops = new ArrayList<>();
            for (Workshop workshop : existingData.workshops) {
                Workshop updated = fetchRepoData(workshop);
                updatedWorkshops.add(updated);
            }

            existingData.workshops = updatedWorkshops;

            // Generate YAML output
            String yamlOutput = generateYaml(existingData);

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
                Log.infof("✅ Workshops data updated successfully!");
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

    private WorkshopsData loadExistingData() throws Exception {
        if (!inputFile.exists()) {
            throw new RuntimeException("Input file not found: " + inputFile.getPath() +
                "\nThe workshops.yaml file must exist.");
        }

        Yaml yaml = new Yaml(new LoaderOptions());
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            Map<String, Object> data = yaml.load(fis);
            return parseWorkshopsData(data);
        }
    }

    private WorkshopsData parseWorkshopsData(Map<String, Object> data) {
        WorkshopsData result = new WorkshopsData();

        result.headline = (String) data.get("headline");
        result.disclaimer = (String) data.get("disclaimer");

        List<Map<String, Object>> workshopsList = (List<Map<String, Object>>) data.getOrDefault("workshops", new ArrayList<>());
        for (Map<String, Object> workshopMap : workshopsList) {
            result.workshops.add(parseWorkshop(workshopMap));
        }

        return result;
    }

    private Workshop parseWorkshop(Map<String, Object> workshopMap) {
        Workshop workshop = new Workshop();

        // Preserve all existing fields
        workshop.url = (String) workshopMap.get("url");
        workshop.title = (String) workshopMap.get("title");
        workshop.thumbnail = (String) workshopMap.get("thumbnail");
        workshop.description = (String) workshopMap.get("description");
        workshop.src = (String) workshopMap.get("src");
        workshop.comment = (String) workshopMap.get("comment");

        Object archived = workshopMap.get("archived");
        if (archived instanceof Boolean) {
            workshop.archived = (Boolean) archived;
        }

        // Extract repo for GitHub API calls
        if (workshop.src != null && workshop.src.startsWith("https://github.com/")) {
            workshop.repo = workshop.src.substring("https://github.com/".length());
            if (workshop.repo.endsWith("/")) {
                workshop.repo = workshop.repo.substring(0, workshop.repo.length() - 1);
            }
        }

        workshop.stars = ((Number) workshopMap.getOrDefault("stars", 0)).intValue();

        Object lastUpdated = workshopMap.get("last_updated");
        if (lastUpdated != null) {
            workshop.lastUpdated = parseDate(lastUpdated);
        }

        return workshop;
    }

    private LocalDate parseDate(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate) return (LocalDate) dateObj;
        if (dateObj instanceof java.util.Date) {
            return ((java.util.Date) dateObj).toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (dateObj instanceof String) {
            String dateStr = (String) dateObj;
            if (dateStr.trim().isEmpty()) return null;
            return LocalDate.parse(dateStr);
        }
        return null;
    }

    private Workshop fetchRepoData(Workshop workshop) throws Exception {
        if (workshop.repo == null || workshop.repo.isEmpty()) {
            Log.warnf("⚠️  Skipping workshop with no repo");
            return workshop;
        }

        Log.infof("📊 Fetching data for: %s", workshop.repo);

        String url = String.format("https://api.github.com/repos/%s", workshop.repo);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Quarkus-Website-Workshops-Sync/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 403) {
            String rateLimitRemaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);
            String rateLimitReset = response.headers().firstValue("X-RateLimit-Reset").orElse(null);
            if ("0".equals(rateLimitRemaining)) {
                String resetTime = rateLimitReset != null
                    ? Instant.ofEpochSecond(Long.parseLong(rateLimitReset)).toString()
                    : "unknown";
                throw new RuntimeException(String.format(
                    "GitHub API rate limit exceeded. Resets at: %s", resetTime));
            }
        }

        if (response.statusCode() == 404) {
            Log.warnf("⚠️  Repository not found: %s", workshop.repo);
            return workshop;
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format(
                "Failed to fetch repo data for %s: HTTP %d - %s",
                workshop.repo, response.statusCode(), response.body()));
        }

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            JsonObject repoData = reader.readObject();

            // Update stars
            workshop.stars = repoData.getInt("stargazers_count", 0);

            // Update last modified date
            String pushedAt = repoData.getString("pushed_at", null);
            if (pushedAt != null) {
                workshop.lastUpdated = Instant.parse(pushedAt).atOffset(ZoneOffset.UTC).toLocalDate();
            }

            Log.infof("   ⭐ Stars: %d, Last updated: %s", workshop.stars, workshop.lastUpdated);
        }

        return workshop;
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
            File tempCurrent = File.createTempFile("workshops-current", ".yaml");
            File tempNew = File.createTempFile("workshops-new", ".yaml");

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
        }

        Log.infof("================================================================================");
        Log.infof("📊 Summary: %d lines would change", changedLines);
    }

    private static String toDateStr(LocalDate ld) {
        return ld.toString();
    }

    static class DateScalar {
        final String value;
        DateScalar(String value) { this.value = value; }
    }

    static class QuotedScalar {
        final String value;
        QuotedScalar(String value) { this.value = value; }
    }

    static class WorkshopsRepresenter extends Representer {
        WorkshopsRepresenter(DumperOptions options) {
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

    private String generateYaml(WorkshopsData data) throws Exception {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setWidth(120);

        Yaml yaml = new Yaml(new WorkshopsRepresenter(options), options);

        Map<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("headline", data.headline);
        dataMap.put("disclaimer", data.disclaimer);

        List<Map<String, Object>> workshopsList = new ArrayList<>();
        for (Workshop workshop : data.workshops) {
            workshopsList.add(workshopToMap(workshop));
        }
        dataMap.put("workshops", workshopsList);

        String output = yaml.dump(dataMap);

        // Add blank lines between workshop entries to match existing style
        output = output.replaceAll("\n(- url:)", "\n\n$1");

        return output;
    }

    private Map<String, Object> workshopToMap(Workshop workshop) {
        Map<String, Object> map = new LinkedHashMap<>();

        // Preserve field order from original
        if (workshop.url != null) {
            map.put("url", new QuotedScalar(workshop.url));
        }
        if (workshop.title != null) {
            map.put("title", new QuotedScalar(workshop.title));
        }
        if (workshop.thumbnail != null) {
            map.put("thumbnail", new QuotedScalar(workshop.thumbnail));
        }
        if (workshop.description != null) {
            // Description is unquoted in original
            map.put("description", workshop.description);
        }
        if (workshop.src != null) {
            // src is unquoted in original
            map.put("src", workshop.src);
        }
        map.put("stars", workshop.stars);

        if (workshop.lastUpdated != null) {
            map.put("last_updated", new DateScalar(toDateStr(workshop.lastUpdated)));
        }
        if (workshop.archived != null && workshop.archived) {
            map.put("archived", workshop.archived);
        }
        if (workshop.comment != null) {
            map.put("comment", workshop.comment);
        }

        return map;
    }

    // Data classes
    public static class WorkshopsData {
        public String headline;
        public String disclaimer;
        public List<Workshop> workshops = new ArrayList<>();
    }

    public static class Workshop {
        public String repo;  // Internal: extracted from src
        public String url;
        public String title;
        public String thumbnail;
        public String description;
        public String src;
        public int stars;
        public LocalDate lastUpdated;
        public Boolean archived;
        public String comment;
    }
}
