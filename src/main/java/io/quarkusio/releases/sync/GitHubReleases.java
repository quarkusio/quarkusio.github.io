package io.quarkusio.releases.sync;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

/**
 * Reads releases, milestones and the release planning wiki from GitHub.
 */
public class GitHubReleases {

    private static final String USER_AGENT = "Quarkus-Website-Release-Sync/1.0";
    private static final int MAX_ATTEMPTS = 3;

    public record GitHubRelease(String tagName, String publishedAt, boolean prerelease) {
    }

    public record GitHubMilestone(String title, String dueOn) {
    }

    public record FetchResult(List<GitHubRelease> releases, List<GitHubRelease> preReleases) {
    }

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String org;
    private final String repo;
    private final String token;

    /**
     * @param token the GitHub token, or null for anonymous access (lower rate limit)
     */
    public GitHubReleases(String org, String repo, String token) {
        this.org = org;
        this.repo = repo;
        this.token = token;
    }

    /**
     * All published (non-draft) releases, split between final and pre-releases.
     */
    public FetchResult fetchReleases() throws IOException, InterruptedException {
        List<GitHubRelease> releases = new ArrayList<>();
        List<GitHubRelease> preReleases = new ArrayList<>();
        String url = "https://api.github.com/repos/%s/%s/releases".formatted(org, repo);
        for (int page = 1; page <= 100; page++) {
            HttpResponse<String> response = send(api(url + "?page=" + page + "&per_page=100"));
            if (response.statusCode() == 403
                    && "0".equals(response.headers().firstValue("X-RateLimit-Remaining").orElse(null))) {
                String reset = response.headers().firstValue("X-RateLimit-Reset")
                        .map(s -> Instant.ofEpochSecond(Long.parseLong(s)).toString())
                        .orElse("unknown");
                throw new IOException("GitHub API rate limit exceeded. Resets at: " + reset);
            }
            if (response.statusCode() != 200) {
                throw new IOException("Failed to fetch releases (page %d): HTTP %d - %s"
                        .formatted(page, response.statusCode(), response.body()));
            }
            JsonNode array = mapper.readTree(response.body());
            if (array.isEmpty()) {
                return new FetchResult(releases, preReleases);
            }
            for (JsonNode json : array) {
                String tagName = json.path("tag_name").asText(null);
                String publishedAt = json.path("published_at").asText(null);
                if (tagName == null || publishedAt == null || json.path("draft").asBoolean(false)) {
                    continue;
                }
                boolean prerelease = json.path("prerelease").asBoolean(false);
                (prerelease ? preReleases : releases).add(new GitHubRelease(tagName, publishedAt, prerelease));
            }
            Log.debugf("Fetched page %d (%d final, %d pre-releases so far)", page, releases.size(), preReleases.size());
        }
        Log.warnf("Reached page limit (100), stopping pagination");
        return new FetchResult(releases, preReleases);
    }

    public List<GitHubMilestone> fetchOpenMilestones() throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/%s/%s/milestones?state=open&per_page=100".formatted(org, repo);
        HttpResponse<String> response = send(api(url));
        List<GitHubMilestone> milestones = new ArrayList<>();
        if (response.statusCode() != 200) {
            Log.warnf("Failed to fetch milestones: HTTP %d", response.statusCode());
            return milestones;
        }
        for (JsonNode json : mapper.readTree(response.body())) {
            milestones.add(new GitHubMilestone(json.path("title").asText(null),
                    json.path("due_on").isNull() ? null : json.path("due_on").asText(null)));
        }
        return milestones;
    }

    /**
     * @return the raw markdown of the release planning wiki, or null if it can't be read
     */
    public String fetchReleasePlanningWiki() throws IOException, InterruptedException {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(WikiReleasePlanning.WIKI_URL))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build());
        if (response.statusCode() != 200) {
            Log.warnf("Failed to fetch release planning wiki: HTTP %d", response.statusCode());
            return null;
        }
        return response.body();
    }

    private HttpRequest api(String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", USER_AGENT)
                .GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder.build();
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 500) {
                return response;
            }
            Log.warnf("GitHub returned HTTP %d for %s (attempt %d/%d)", response.statusCode(), request.uri(), attempt,
                    MAX_ATTEMPTS);
            Thread.sleep(2000L * attempt);
        }
        return response;
    }
}
