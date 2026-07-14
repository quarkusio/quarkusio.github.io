///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus.platform:quarkus-bom:3.36.3@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-config-yaml
//DEPS org.yaml:snakeyaml:2.2

import io.quarkus.logging.Log;
import jakarta.inject.Inject;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CommandLine.Command(name = "sync-releases", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {

    @ConfigProperty(name = "GITHUB_TOKEN")
    String token;

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

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

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
            ReleaseData existingData = loadExistingData();
            Log.infof("📋 Loaded existing data with %d releases", existingData.releases.size());

            // Fetch GitHub releases (both final and pre-releases in one pass)
            FetchReleasesResult fetchResult = fetchAllGitHubReleases();
            List<GitHubRelease> githubReleases = fetchResult.releases;
            Log.infof("Fetched %d releases from GitHub (%d pre-releases)",
                     githubReleases.size(), fetchResult.preReleases.size());

            // Fetch upcoming release predictions if enabled
            List<UpcomingRelease> upcomingReleases = new ArrayList<>();
            if (predictUpcoming) {
                try {
                    upcomingReleases = predictUpcomingReleases(githubReleases, fetchResult.preReleases);
                    Log.infof("Predicted %d upcoming releases", upcomingReleases.size());
                } catch (Exception e) {
                    Log.warnf("Upcoming release prediction failed: %s", e.getMessage());
                    Log.infof("Will use '[version tbd]' as fallback");
                }
            }

            // Process and merge data
            ReleaseData updatedData = processReleases(existingData, githubReleases, upcomingReleases);
            Log.infof("🔄 Processed into %d release entries", updatedData.releases.size());

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

    private ReleaseData loadExistingData() throws Exception {
        if (!inputFile.exists()) {
            throw new RuntimeException("Input file not found: " + inputFile.getPath() +
                "\nThe releases.yaml file must exist with policy, support_providers, and eol_support sections.");
        }

        Yaml yaml = new Yaml(new LoaderOptions());
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            Map<String, Object> data = yaml.load(fis);
            return parseReleaseData(data);
        }
    }

    private ReleaseData parseReleaseData(Map<String, Object> data) {
        ReleaseData result = new ReleaseData();

        result.policy = (Map<String, Object>) data.getOrDefault("policy", new LinkedHashMap<>());
        result.supportProviders = (Map<String, Object>) data.getOrDefault("support_providers", new LinkedHashMap<>());
        result.eolSupport = (List<Map<String, Object>>) data.getOrDefault("eol_support", new ArrayList<>());

        List<Map<String, Object>> releaseList = (List<Map<String, Object>>) data.getOrDefault("releases", new ArrayList<>());
        for (Map<String, Object> releaseMap : releaseList) {
            result.releases.add(parseRelease(releaseMap));
        }

        return result;
    }

    private Release parseRelease(Map<String, Object> releaseMap) {
        Release release = new Release();
        release.version = (String) releaseMap.get("version");
        release.lts = (Boolean) releaseMap.getOrDefault("lts", false);
        release.upcoming = (Boolean) releaseMap.get("upcoming");
        release.latestPatch = (String) releaseMap.get("latest_patch");
        release.link = (String) releaseMap.get("link");

        // Parse dates
        release.releaseDate = parseDate(releaseMap.get("release_date"));
        release.eolDate = parseDate(releaseMap.get("eol_date"));
        release.hasExplicitEolDate = releaseMap.containsKey("eol_date");
        release.latestPatchDate = parseDate(releaseMap.get("latest_patch_date"));
        release.rhbqEol = parseDate(releaseMap.get("rhbq_eol"));
        release.ibmEol = parseDate(releaseMap.get("ibm_eol"));

        return release;
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

    private static class FetchReleasesResult {
        List<GitHubRelease> releases = new ArrayList<>();
        List<GitHubRelease> preReleases = new ArrayList<>();
    }

    private FetchReleasesResult fetchAllGitHubReleases() throws Exception {
        FetchReleasesResult result = new FetchReleasesResult();
        String url = String.format("https://api.github.com/repos/%s/%s/releases", org, repo);

        int page = 1;
        while (true) {
            String requestUrl = url + "?page=" + page + "&per_page=100";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Quarkus-Website-Release-Sync/1.0")
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

            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format(
                    "Failed to fetch releases (page %d): HTTP %d - %s",
                    page, response.statusCode(), response.body()));
            }

            List<GitHubRelease> pageReleases = parseGitHubReleases(response.body());

            if (pageReleases.isEmpty()) {
                break;
            }

            for (GitHubRelease release : pageReleases) {
                if (release.draft) {
                    continue;
                }
                if (release.prerelease) {
                    result.preReleases.add(release);
                } else {
                    result.releases.add(release);
                }
            }

            Log.debugf("Fetched page %d: %d releases (%d final, %d pre-releases so far)",
                      page, pageReleases.size(), result.releases.size(), result.preReleases.size());

            page++;

            if (page > 100) {
                Log.warnf("Reached page limit (100), stopping pagination");
                break;
            }
        }

        return result;
    }

    private List<GitHubRelease> fetchGitHubReleases() throws Exception {
        return fetchAllGitHubReleases().releases;
    }

    private List<GitHubRelease> parseGitHubReleases(String jsonResponse) {
        List<GitHubRelease> releases = new ArrayList<>();

        try (JsonReader reader = Json.createReader(new StringReader(jsonResponse))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value instanceof JsonObject) {
                    GitHubRelease release = parseGitHubRelease((JsonObject) value);
                    if (release != null) {
                        releases.add(release);
                    }
                }
            }
        }

        return releases;
    }

    private GitHubRelease parseGitHubRelease(JsonObject json) {
        GitHubRelease release = new GitHubRelease();

        release.tagName = json.getString("tag_name", null);
        release.publishedAt = json.getString("published_at", null);
        release.prerelease = json.getBoolean("prerelease", false);
        release.draft = json.getBoolean("draft", false);

        if (release.tagName != null && release.publishedAt != null) {
            return release;
        }
        return null;
    }

    private List<UpcomingRelease> predictUpcomingReleases(List<GitHubRelease> existingReleases,
                                                          List<GitHubRelease> preReleases) {
        List<UpcomingRelease> upcoming = new ArrayList<>();

        // Get existing release versions to avoid conflicts
        Set<String> existingVersions = existingReleases.stream()
                .map(r -> getMinorVersion(r.tagName))
                .collect(Collectors.toSet());

        Log.infof("Checking for upcoming releases (existing versions: %d)", existingVersions.size());

        // Method 1: Check GitHub milestones for upcoming releases
        try {
            List<UpcomingRelease> milestoneReleases = predictFromMilestones(existingVersions);
            upcoming.addAll(milestoneReleases);
            Log.infof("Found %d upcoming releases from milestones", milestoneReleases.size());
        } catch (Exception e) {
            Log.warnf("Milestone prediction failed: %s", e.getMessage());
        }

        // Method 2: Check pre-release tags (CR, Alpha, Beta) for upcoming final releases
        try {
            List<UpcomingRelease> preReleaseUpcoming = predictFromPreReleases(preReleases, existingVersions);
            upcoming.addAll(preReleaseUpcoming);
            Log.infof("Found %d upcoming releases from pre-releases", preReleaseUpcoming.size());
        } catch (Exception e) {
            Log.warnf("Pre-release prediction failed: %s", e.getMessage());
        }

        // Method 3: Predict next minor release based on cadence pattern
        try {
            List<UpcomingRelease> cadenceReleases = predictFromCadence(existingReleases, existingVersions);
            upcoming.addAll(cadenceReleases);
            Log.infof("Found %d upcoming releases from cadence", cadenceReleases.size());
        } catch (Exception e) {
            Log.warnf("Cadence prediction failed: %s", e.getMessage());
        }

        // Remove duplicates and sort
        List<UpcomingRelease> result = upcoming.stream()
                .distinct()
                .sorted((a, b) -> compareVersions(b.version, a.version))
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            Log.warnf("No upcoming releases predicted from any source");
        } else {
            Log.infof("Total predicted upcoming releases: %d", result.size());
        }

        return result;
    }

    private List<UpcomingRelease> predictFromMilestones(Set<String> existingVersions) {
        List<UpcomingRelease> upcoming = new ArrayList<>();

        try {
            String url = String.format("https://api.github.com/repos/%s/%s/milestones", org, repo);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "?state=open&per_page=100"))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Quarkus-Website-Release-Sync/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<GitHubMilestone> milestones = parseMilestones(response.body());

                for (GitHubMilestone milestone : milestones) {
                    String version = extractVersionFromMilestone(milestone.title);
                    if (version != null && !existingVersions.contains(version)) {
                        upcoming.add(new UpcomingRelease(version, "milestone", milestone.dueOn));
                        Log.infof("🎯 Found milestone-based upcoming release: %s (due: %s)", version, milestone.dueOn);
                    }
                }
            }
        } catch (Exception e) {
            Log.warnf("Failed to fetch milestones: %s", e.getMessage());
        }

        return upcoming;
    }

    private List<UpcomingRelease> predictFromPreReleases(List<GitHubRelease> preReleases,
                                                         Set<String> existingVersions) {
        List<UpcomingRelease> upcoming = new ArrayList<>();

        for (GitHubRelease release : preReleases) {
            if (release.tagName != null) {
                String baseVersion = extractBaseVersionFromPreRelease(release.tagName);
                if (baseVersion != null && !existingVersions.contains(baseVersion)) {
                    upcoming.add(new UpcomingRelease(baseVersion, "prerelease", null));
                    Log.infof("Found pre-release indicating upcoming: %s (from %s)", baseVersion, release.tagName);
                }
            }
        }

        return upcoming;
    }

    private List<UpcomingRelease> predictFromCadence(List<GitHubRelease> existingReleases, Set<String> existingVersions) {
        List<UpcomingRelease> upcoming = new ArrayList<>();

        // Find the latest minor version
        String latestMinor = existingReleases.stream()
                .map(r -> getMinorVersion(r.tagName))
                .filter(this::isValidVersion)
                .max((a, b) -> compareVersions(a, b))
                .orElse(null);

        if (latestMinor != null) {
            try {
                // Parse current version
                String[] parts = latestMinor.split("\\.");
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);

                // Predict next minor version
                String nextMinor = major + "." + (minor + 1);

                if (!existingVersions.contains(nextMinor)) {
                    upcoming.add(new UpcomingRelease(nextMinor, "cadence", null));
                    Log.infof("📅 Predicted next minor release based on cadence: %s", nextMinor);
                }
            } catch (Exception e) {
                Log.debugf("Failed to parse version for cadence prediction: %s", e.getMessage());
            }
        }

        return upcoming;
    }

    private String extractVersionFromMilestone(String title) {
        if (title == null) return null;

        // Common milestone patterns:
        // "3.38 - 3.x" -> "3.38"
        // "3.38" -> "3.38"
        // "4.0 - main" -> "4.0"

        Pattern pattern = Pattern.compile("^(\\d+\\.\\d+)(?:\\s|$|\\s*-|\\s*\\.|$)");
        Matcher matcher = pattern.matcher(title);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String extractBaseVersionFromPreRelease(String tagName) {
        if (tagName == null) return null;

        // Pre-release patterns:
        // "3.38.0.CR1" -> "3.38"
        // "3.38.0.Alpha1" -> "3.38"
        // "3.38.0.Beta1" -> "3.38"

        Pattern pattern = Pattern.compile("^(\\d+\\.\\d+)\\.\\d+\\.(CR|Alpha|Beta|RC)");
        Matcher matcher = pattern.matcher(tagName);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private List<GitHubMilestone> parseMilestones(String jsonResponse) {
        List<GitHubMilestone> milestones = new ArrayList<>();

        try (JsonReader reader = Json.createReader(new StringReader(jsonResponse))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                if (value instanceof JsonObject) {
                    GitHubMilestone milestone = parseMilestone((JsonObject) value);
                    if (milestone != null) {
                        milestones.add(milestone);
                    }
                }
            }
        } catch (Exception e) {
            Log.debugf("Failed to parse milestones: %s", e.getMessage());
        }

        return milestones;
    }

    private GitHubMilestone parseMilestone(JsonObject json) {
        GitHubMilestone milestone = new GitHubMilestone();
        milestone.title = json.getString("title", null);
        milestone.dueOn = json.isNull("due_on") ? null : json.getString("due_on", null);
        return milestone;
    }

    private ReleaseData processReleases(ReleaseData existingData, List<GitHubRelease> githubReleases, List<UpcomingRelease> upcomingReleases) {
        // Group GitHub releases by minor version
        Map<String, List<GitHubRelease>> releasesByMinor = githubReleases.stream()
                .filter(r -> r.tagName != null && isValidVersion(r.tagName))
                .collect(Collectors.groupingBy(r -> getMinorVersion(r.tagName)));

        // Create map of existing releases for easy lookup
        Map<String, Release> existingReleases = existingData.releases.stream()
                .collect(Collectors.toMap(r -> r.version, r -> r));

        // Process each minor version group
        List<Release> processedReleases = new ArrayList<>();

        for (Map.Entry<String, List<GitHubRelease>> entry : releasesByMinor.entrySet()) {
            String minorVersion = entry.getKey();
            List<GitHubRelease> minorReleases = entry.getValue();

            // Sort releases by semantic version (latest first)
            minorReleases.sort((a, b) -> compareVersions(b.tagName, a.tagName));

            GitHubRelease latest = minorReleases.get(0);
            GitHubRelease earliest = minorReleases.get(minorReleases.size() - 1);

            Release existingRelease = existingReleases.get(minorVersion);
            Release processedRelease = createProcessedRelease(minorVersion, earliest, latest, existingRelease);

            processedReleases.add(processedRelease);
        }

        // Handle existing upcoming releases with smart logic
        Set<String> manualUpcomingVersions = new HashSet<>();
        for (Release existing : existingData.releases) {
            if (Boolean.TRUE.equals(existing.upcoming)) {
                if (releasesByMinor.containsKey(existing.version)) {
                    // Upcoming release now exists - auto-update it to actual release
                    Log.infof("🔄 Auto-updating upcoming release %s to actual release (now published)", existing.version);
                    // The actual release is already processed above, so we don't add the upcoming version
                } else {
                    // Keep existing upcoming release and mark as manually curated
                    processedReleases.add(existing);
                    manualUpcomingVersions.add(existing.version);
                    Log.infof("✋ Preserving manually curated upcoming release: %s", existing.version);
                }
            }
        }

        // Find latest released version to filter out stale predictions
        String latestReleasedVersion = processedReleases.stream()
                .filter(r -> !Boolean.TRUE.equals(r.upcoming))
                .map(r -> r.version)
                .max(this::compareVersions)
                .orElse(null);

        // Add the single next upcoming release if no manual upcoming releases exist
        if (manualUpcomingVersions.isEmpty()) {
            UpcomingRelease nextUpcoming = upcomingReleases.stream()
                    .filter(u -> latestReleasedVersion == null || compareVersions(u.version, latestReleasedVersion) > 0)
                    .filter(u -> !releasesByMinor.containsKey(u.version))
                    .min((a, b) -> compareVersions(a.version, b.version))
                    .orElse(null);

            if (nextUpcoming != null) {
                Release upcomingRelease = createUpcomingRelease(nextUpcoming);
                processedReleases.add(upcomingRelease);
                Log.infof("🆕 Added predicted upcoming release: %s (source: %s)", nextUpcoming.version, nextUpcoming.source);
            }
        } else {
            Log.infof("📝 Skipping prediction - manual upcoming releases found: %s", manualUpcomingVersions);
        }

        // Sort all releases by version (latest first)
        processedReleases.sort((a, b) -> compareVersions(b.version, a.version));

        // Compute EOL dates for non-LTS releases that don't have one explicitly set.
        // Policy: non-LTS releases are supported "until the next minor release",
        // so EOL date = release date of the next higher minor version.
        for (int i = 0; i < processedReleases.size(); i++) {
            Release release = processedReleases.get(i);
            if (release.hasExplicitEolDate || Boolean.TRUE.equals(release.upcoming)) {
                continue;
            }
            if (Boolean.TRUE.equals(release.lts)) {
                continue;
            }
            // Find the next higher version (previous index since sorted latest-first)
            if (i > 0) {
                Release nextRelease = processedReleases.get(i - 1);
                if (nextRelease.releaseDate != null) {
                    release.eolDate = nextRelease.releaseDate;
                }
            }
        }

        // Build result
        ReleaseData result = new ReleaseData();
        result.policy = existingData.policy;
        result.supportProviders = existingData.supportProviders;
        result.eolSupport = existingData.eolSupport;
        result.releases = processedReleases;

        return result;
    }

    private boolean isValidVersion(String version) {
        return VERSION_PATTERN.matcher(version).find();
    }

    private String getMinorVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2);
        }
        return version;
    }

    private int compareVersions(String v1, String v2) {
        Matcher m1 = VERSION_PATTERN.matcher(v1);
        Matcher m2 = VERSION_PATTERN.matcher(v2);
        String clean1 = m1.find() ? v1.substring(m1.start(), m1.end()) : v1;
        String clean2 = m2.find() ? v2.substring(m2.start(), m2.end()) : v2;

        String[] parts1 = clean1.split("\\.");
        String[] parts2 = clean2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (part1 != part2) {
                return Integer.compare(part1, part2);
            }
        }
        return 0;
    }

    private Release createProcessedRelease(String minorVersion, GitHubRelease earliest, GitHubRelease latest, Release existing) {
        Release release = new Release();
        release.version = minorVersion;

        // Set release date from earliest release
        release.releaseDate = parseGitHubDate(earliest.publishedAt);

        // Set latest patch info
        release.latestPatch = latest.tagName;
        release.latestPatchDate = parseGitHubDate(latest.publishedAt);

        // Clear upcoming flag since we have real GitHub data
        release.upcoming = null;

        // Preserve manual fields from existing release
        if (existing != null) {
            release.lts = existing.lts;
            release.eolDate = existing.eolDate;
            release.hasExplicitEolDate = existing.hasExplicitEolDate;
            release.link = existing.link;
            release.rhbqEol = existing.rhbqEol;
            release.ibmEol = existing.ibmEol;
        } else {
            release.lts = false;
        }

        return release;
    }

    private Release createUpcomingRelease(UpcomingRelease upcoming) {
        Release release = new Release();
        release.version = upcoming.version;
        release.lts = false;  // Default to non-LTS for upcoming releases
        release.upcoming = true;

        // Set estimated release date if available
        if (upcoming.dueDate != null) {
            try {
                release.releaseDate = LocalDate.parse(upcoming.dueDate.split("T")[0]);
            } catch (Exception e) {
                Log.debugf("Failed to parse due date for upcoming release %s: %s", upcoming.version, e.getMessage());
            }
        }

        return release;
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


    private LocalDate parseGitHubDate(String isoDate) {
        return Instant.parse(isoDate).atOffset(ZoneOffset.UTC).toLocalDate();
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

    private String generateYaml(ReleaseData data) throws Exception {
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
        for (Release release : data.releases) {
            releasesList.add(releaseToMap(release));
        }
        Map<String, Object> relMap = new LinkedHashMap<>();
        relMap.put("releases", releasesList);

        // Add blank lines between release entries to match existing style
        String relSection = relYaml.dump(relMap);
        relSection = relSection.replaceAll("\n(  - version:)", "\n\n$1");

        return header + relSection;
    }

    private Map<String, Object> releaseToMap(Release release) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", new QuotedScalar(release.version));

        if (release.releaseDate != null) {
            map.put("release_date", new DateScalar(toDateStr(release.releaseDate)));
        }

        if (release.eolDate != null) {
            map.put("eol_date", new DateScalar(toDateStr(release.eolDate)));
        } else if (release.hasExplicitEolDate) {
            map.put("eol_date", null);
        }

        map.put("lts", release.lts);

        if (Boolean.TRUE.equals(release.upcoming)) {
            map.put("upcoming", true);
        }

        if (release.latestPatch != null) {
            map.put("latest_patch", new QuotedScalar(release.latestPatch));
        }

        if (release.latestPatchDate != null) {
            map.put("latest_patch_date", new DateScalar(toDateStr(release.latestPatchDate)));
        }

        if (release.link != null) {
            map.put("link", release.link);
        }

        if (release.rhbqEol != null) {
            map.put("rhbq_eol", new DateScalar(toDateStr(release.rhbqEol)));
        }

        if (release.ibmEol != null) {
            map.put("ibm_eol", new DateScalar(toDateStr(release.ibmEol)));
        }

        return map;
    }

    // Data classes
    public static class ReleaseData {
        public Map<String, Object> policy = new LinkedHashMap<>();
        public Map<String, Object> supportProviders = new LinkedHashMap<>();
        public List<Map<String, Object>> eolSupport = new ArrayList<>();
        public List<Release> releases = new ArrayList<>();
    }

    public static class Release {
        public String version;
        public LocalDate releaseDate;
        public LocalDate eolDate;
        public boolean hasExplicitEolDate;
        public Boolean lts;
        public String latestPatch;
        public LocalDate latestPatchDate;
        public String link;
        public LocalDate rhbqEol;
        public LocalDate ibmEol;
        public Boolean upcoming;
    }

    public static class GitHubRelease {
        public String tagName;
        public String publishedAt;
        public boolean prerelease;
        public boolean draft;
    }

    public static class UpcomingRelease {
        public String version;
        public String source;  // "milestone", "prerelease", "cadence"
        public String dueDate; // ISO date string, can be null

        public UpcomingRelease(String version, String source, String dueDate) {
            this.version = version;
            this.source = source;
            this.dueDate = dueDate;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof UpcomingRelease)) return false;
            UpcomingRelease other = (UpcomingRelease) obj;
            return Objects.equals(version, other.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version);
        }
    }

    public static class GitHubMilestone {
        public String title;
        public String dueOn;
    }
}