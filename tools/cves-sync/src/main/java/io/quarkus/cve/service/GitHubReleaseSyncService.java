package io.quarkus.cve.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.cve.util.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GitHubReleaseSyncService {

    @Inject
    @ConfigProperty(name = "github.token")
    Optional<String> configToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    public static class MinorReleaseInfo {
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
        public Object herodevsEol; // LocalDate or String like "nes"
        public Boolean upcoming;
    }

    public static class ReleaseSyncResult {
        public Map<String, Object> policy = new LinkedHashMap<>();
        public Map<String, Object> supportProviders = new LinkedHashMap<>();
        public List<Map<String, Object>> eolSupport = new ArrayList<>();
        public List<MinorReleaseInfo> releases = new ArrayList<>();
        public Map<String, MinorReleaseInfo> releasesByMinor = new LinkedHashMap<>();
    }

    public ReleaseSyncResult sync(File inputFile, String org, String repo, boolean predictUpcoming) {
        ReleaseSyncResult result = new ReleaseSyncResult();

        // 1. Load manual / existing metadata from inputFile (releases.yaml)
        ExistingData existingData = loadExistingData(inputFile);
        result.policy = existingData.policy;
        result.supportProviders = existingData.supportProviders;
        result.eolSupport = existingData.eolSupport;

        String token = configToken.filter(t -> !t.isBlank())
                .orElse(System.getenv("GITHUB_TOKEN"));

        // 2. Fetch GitHub releases
        FetchReleasesResult fetchResult = null;
        try {
            fetchResult = fetchAllGitHubReleases(org, repo, token);
            Log.log(1, "GitHub: fetched %d releases (%d pre-releases) from %s/%s",
                    fetchResult.releases.size(), fetchResult.preReleases.size(), org, repo);
        } catch (Exception e) {
            Log.warn("GitHub releases fetch failed (%s). Using existing release metadata from %s.",
                    e.getMessage(), inputFile != null ? inputFile.getPath() : "releases.yaml");
        }

        // If GitHub fetch failed, use existingData directly
        if (fetchResult == null || fetchResult.releases.isEmpty()) {
            result.releases = existingData.releases;
            for (MinorReleaseInfo r : result.releases) {
                result.releasesByMinor.put(getMinorVersion(r.version), r);
            }
            return result;
        }

        // 3. Upcoming release prediction if enabled
        List<UpcomingRelease> upcomingReleases = new ArrayList<>();
        if (predictUpcoming) {
            try {
                upcomingReleases = predictUpcomingReleases(org, repo, token, fetchResult.releases, fetchResult.preReleases);
                Log.log(2, "GitHub: predicted %d upcoming releases", upcomingReleases.size());
            } catch (Exception e) {
                Log.warn("Upcoming release prediction failed: %s", e.getMessage());
            }
        }

        // 4. Process and merge GitHub data with manual data
        result.releases = processReleases(existingData, fetchResult.releases, upcomingReleases);
        for (MinorReleaseInfo r : result.releases) {
            result.releasesByMinor.put(getMinorVersion(r.version), r);
        }

        return result;
    }

    private static class ExistingData {
        Map<String, Object> policy = new LinkedHashMap<>();
        Map<String, Object> supportProviders = new LinkedHashMap<>();
        List<Map<String, Object>> eolSupport = new ArrayList<>();
        List<MinorReleaseInfo> releases = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private ExistingData loadExistingData(File inputFile) {
        ExistingData data = new ExistingData();
        if (inputFile == null || !inputFile.exists()) {
            Log.warn("Releases input file does not exist: %s", inputFile != null ? inputFile.getPath() : "null");
            return data;
        }

        try {
            YAMLMapper yamlMapper = new YAMLMapper();
            Map<String, Object> raw = yamlMapper.readValue(inputFile, Map.class);
            if (raw == null) return data;

            data.policy = (Map<String, Object>) raw.getOrDefault("policy", new LinkedHashMap<>());
            data.supportProviders = (Map<String, Object>) raw.getOrDefault("support_providers", new LinkedHashMap<>());
            data.eolSupport = (List<Map<String, Object>>) raw.getOrDefault("eol_support", new ArrayList<>());

            List<Map<String, Object>> relList = (List<Map<String, Object>>) raw.getOrDefault("releases", new ArrayList<>());
            for (Map<String, Object> m : relList) {
                MinorReleaseInfo info = new MinorReleaseInfo();
                info.version = (String) m.get("version");
                info.lts = (Boolean) m.getOrDefault("lts", false);
                info.upcoming = (Boolean) m.get("upcoming");
                info.latestPatch = (String) m.get("latest_patch");
                info.link = (String) m.get("link");
                info.releaseDate = parseDate(m.get("release_date"));
                info.eolDate = parseDate(m.get("eol_date"));
                info.hasExplicitEolDate = m.containsKey("eol_date");
                info.latestPatchDate = parseDate(m.get("latest_patch_date"));
                info.rhbqEol = parseDate(m.get("rhbq_eol"));
                info.ibmEol = parseDate(m.get("ibm_eol"));

                Object h = m.get("herodevs_eol");
                if (h instanceof String str) {
                    try {
                        info.herodevsEol = LocalDate.parse(str);
                    } catch (Exception ex) {
                        info.herodevsEol = str;
                    }
                } else {
                    info.herodevsEol = parseDate(h);
                }
                data.releases.add(info);
            }
        } catch (Exception e) {
            Log.warn("Failed to load existing releases from %s: %s", inputFile.getPath(), e.getMessage());
        }

        return data;
    }

    private LocalDate parseDate(Object dateObj) {
        if (dateObj == null) return null;
        if (dateObj instanceof LocalDate ld) return ld;
        if (dateObj instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (dateObj instanceof String s) {
            String str = s.trim();
            if (str.isEmpty()) return null;
            try {
                return LocalDate.parse(str.contains("T") ? str.split("T")[0] : str.split(" ")[0]);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static class GitHubRelease {
        String tagName;
        String publishedAt;
        boolean prerelease;
        boolean draft;
    }

    private static class FetchReleasesResult {
        List<GitHubRelease> releases = new ArrayList<>();
        List<GitHubRelease> preReleases = new ArrayList<>();
    }

    private FetchReleasesResult fetchAllGitHubReleases(String org, String repo, String token) throws Exception {
        FetchReleasesResult result = new FetchReleasesResult();
        String url = String.format("https://api.github.com/repos/%s/%s/releases", org, repo);

        int page = 1;
        while (true) {
            String requestUrl = url + "?page=" + page + "&per_page=100";
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Quarkus-Website-CVE-Sync/1.0")
                    .GET();

            if (token != null && !token.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                String resetTime = response.headers().firstValue("X-RateLimit-Reset").orElse("unknown");
                Log.warn("GitHub API rate limit reached (reset: %s).", resetTime);
                break;
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException(String.format("GitHub API returned HTTP %d: %s", response.statusCode(), response.body()));
            }

            JsonNode array = jsonMapper.readTree(response.body());
            if (!array.isArray() || array.isEmpty()) {
                break;
            }

            for (JsonNode item : array) {
                GitHubRelease rel = new GitHubRelease();
                rel.tagName = item.path("tag_name").asText(null);
                rel.publishedAt = item.path("published_at").asText(null);
                rel.prerelease = item.path("prerelease").asBoolean(false);
                rel.draft = item.path("draft").asBoolean(false);

                if (rel.tagName != null && rel.publishedAt != null && !rel.draft) {
                    if (rel.prerelease) {
                        result.preReleases.add(rel);
                    } else {
                        result.releases.add(rel);
                    }
                }
            }

            page++;
            if (page > 100) break;
        }

        return result;
    }

    private static class UpcomingRelease {
        String version;
        String source;
        String dueDate;

        UpcomingRelease(String version, String source, String dueDate) {
            this.version = version;
            this.source = source;
            this.dueDate = dueDate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UpcomingRelease other)) return false;
            return Objects.equals(version, other.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version);
        }
    }

    private List<UpcomingRelease> predictUpcomingReleases(String org, String repo, String token,
                                                          List<GitHubRelease> existingReleases,
                                                          List<GitHubRelease> preReleases) {
        List<UpcomingRelease> upcoming = new ArrayList<>();
        Set<String> existingVersions = existingReleases.stream()
                .map(r -> getMinorVersion(r.tagName))
                .collect(Collectors.toSet());

        // Milestones
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/milestones?state=open&per_page=100", org, repo);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Quarkus-Website-CVE-Sync/1.0")
                    .GET();
            if (token != null && !token.isBlank()) reqBuilder.header("Authorization", "Bearer " + token);

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode array = jsonMapper.readTree(resp.body());
                if (array.isArray()) {
                    for (JsonNode m : array) {
                        String title = m.path("title").asText(null);
                        String dueOn = m.path("due_on").asText(null);
                        String v = extractVersionFromMilestone(title);
                        if (v != null && !existingVersions.contains(v)) {
                            upcoming.add(new UpcomingRelease(v, "milestone", dueOn));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.log(3, "Milestone prediction error: %s", e.getMessage());
        }

        // Pre-releases
        for (GitHubRelease pr : preReleases) {
            if (pr.tagName != null) {
                String base = extractBaseVersionFromPreRelease(pr.tagName);
                if (base != null && !existingVersions.contains(base)) {
                    upcoming.add(new UpcomingRelease(base, "prerelease", null));
                }
            }
        }

        return upcoming.stream().distinct().sorted((a, b) -> compareVersions(b.version, a.version)).toList();
    }

    private String extractVersionFromMilestone(String title) {
        if (title == null) return null;
        Pattern p = Pattern.compile("^(\\d+\\.\\d+)(?:\\s|$|\\s*-|\\s*\\.|$)");
        Matcher m = p.matcher(title);
        return m.find() ? m.group(1) : null;
    }

    private String extractBaseVersionFromPreRelease(String tagName) {
        if (tagName == null) return null;
        Pattern p = Pattern.compile("^(\\d+\\.\\d+)\\.\\d+\\.(CR|Alpha|Beta|RC)");
        Matcher m = p.matcher(tagName);
        return m.find() ? m.group(1) : null;
    }

    private List<MinorReleaseInfo> processReleases(ExistingData existingData,
                                                   List<GitHubRelease> githubReleases,
                                                   List<UpcomingRelease> upcomingReleases) {
        Map<String, List<GitHubRelease>> byMinor = githubReleases.stream()
                .filter(r -> r.tagName != null && isValidVersion(r.tagName))
                .collect(Collectors.groupingBy(r -> getMinorVersion(r.tagName)));

        Map<String, MinorReleaseInfo> existingMap = existingData.releases.stream()
                .collect(Collectors.toMap(r -> r.version, r -> r, (a, b) -> a));

        List<MinorReleaseInfo> processed = new ArrayList<>();

        for (Map.Entry<String, List<GitHubRelease>> e : byMinor.entrySet()) {
            String minor = e.getKey();
            List<GitHubRelease> releases = e.getValue();
            releases.sort((a, b) -> compareVersions(b.tagName, a.tagName));

            GitHubRelease latest = releases.get(0);
            GitHubRelease earliest = releases.get(releases.size() - 1);

            MinorReleaseInfo existing = existingMap.get(minor);
            MinorReleaseInfo info = new MinorReleaseInfo();
            info.version = minor;
            info.releaseDate = parseDate(earliest.publishedAt);
            info.latestPatch = latest.tagName;
            info.latestPatchDate = parseDate(latest.publishedAt);
            info.upcoming = null;

            if (existing != null) {
                info.lts = existing.lts;
                info.eolDate = existing.eolDate;
                info.hasExplicitEolDate = existing.hasExplicitEolDate;
                info.link = existing.link;
                info.rhbqEol = existing.rhbqEol;
                info.ibmEol = existing.ibmEol;
                info.herodevsEol = existing.herodevsEol;
            } else {
                info.lts = false;
            }
            processed.add(info);
        }

        // Add / preserve upcoming releases
        Set<String> manualUpcoming = new HashSet<>();
        for (MinorReleaseInfo existing : existingData.releases) {
            if (Boolean.TRUE.equals(existing.upcoming)) {
                if (!byMinor.containsKey(existing.version)) {
                    processed.add(existing);
                    manualUpcoming.add(existing.version);
                }
            }
        }

        String latestReleased = processed.stream()
                .filter(r -> !Boolean.TRUE.equals(r.upcoming))
                .map(r -> r.version)
                .max(this::compareVersions)
                .orElse(null);

        if (manualUpcoming.isEmpty() && !upcomingReleases.isEmpty()) {
            UpcomingRelease nextUpcoming = upcomingReleases.stream()
                    .filter(u -> latestReleased == null || compareVersions(u.version, latestReleased) > 0)
                    .filter(u -> !byMinor.containsKey(u.version))
                    .min((a, b) -> compareVersions(a.version, b.version))
                    .orElse(null);

            if (nextUpcoming != null) {
                MinorReleaseInfo up = new MinorReleaseInfo();
                up.version = nextUpcoming.version;
                up.lts = false;
                up.upcoming = true;
                if (nextUpcoming.dueDate != null) {
                    try {
                        up.releaseDate = LocalDate.parse(nextUpcoming.dueDate.split("T")[0]);
                    } catch (Exception ignored) {}
                }
                processed.add(up);
            }
        }

        // Sort latest first
        processed.sort((a, b) -> compareVersions(b.version, a.version));

        // Compute EOL dates for non-LTS releases without explicit EOL
        for (int i = 0; i < processed.size(); i++) {
            MinorReleaseInfo r = processed.get(i);
            if (r.hasExplicitEolDate || Boolean.TRUE.equals(r.upcoming) || Boolean.TRUE.equals(r.lts)) {
                continue;
            }
            if (i > 0) {
                MinorReleaseInfo next = processed.get(i - 1);
                if (next.releaseDate != null) {
                    r.eolDate = next.releaseDate;
                }
            }
        }

        return processed;
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
}
