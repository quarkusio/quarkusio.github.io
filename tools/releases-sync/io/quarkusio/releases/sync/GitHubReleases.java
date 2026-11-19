package io.quarkusio.releases.sync;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

/**
 * Reads releases, milestones, branch merge-bases, tag ancestry and the release planning wiki from GitHub.
 */
public class GitHubReleases {

    private static final String USER_AGENT = "Quarkus-Website-Release-Sync/1.0";
    private static final int MAX_ATTEMPTS = 3;
    /** More commits than this between two tags can't all be release bookkeeping, so aren't listed. */
    private static final int MAX_RELEASE_COMMITS = 10;

    /**
     * A GitHub release published more than this long after its tag commit wasn't published when it came out. Normal
     * releases are published within days of the tag commit (the longest of 529 quarkusio/quarkus releases was 23
     * days, a 3.0 candidate); 2.11.0.Final was published 399 days late, which made it look newer than 2.11.1.Final.
     */
    static final Duration MAX_PUBLISH_DELAY = Duration.ofDays(60);

    /**
     * @param publishedAt when the release came out, as an ISO date-time: GitHub's published_at, except when that is
     *        implausibly late (see {@link #releaseTime}); can be null
     * @param htmlUrl the release's page on GitHub, can be null
     */
    public record GitHubRelease(String tagName, String publishedAt, boolean prerelease, String htmlUrl) {

        public GitHubRelease(String tagName, String publishedAt, boolean prerelease) {
            this(tagName, publishedAt, prerelease, null);
        }
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
                String publishedAt = releaseTime(json.path("created_at").asText(null),
                        json.path("published_at").asText(null), tagName);
                if (tagName == null || publishedAt == null || json.path("draft").asBoolean(false)) {
                    continue;
                }
                boolean prerelease = json.path("prerelease").asBoolean(false);
                (prerelease ? preReleases : releases).add(
                        new GitHubRelease(tagName, publishedAt, prerelease, json.path("html_url").asText(null)));
            }
            Log.debugf("Fetched page %d (%d final, %d pre-releases so far)", page, releases.size(), preReleases.size());
        }
        Log.warnf("Reached page limit (100), stopping pagination");
        return new FetchResult(releases, preReleases);
    }

    /**
     * When a release came out. That is normally when GitHub says it was published, but a release can be created at
     * the time of its tag and only published long afterwards (2.11.0.Final: tagged 2022-07-20, published
     * 2023-08-23). GitHub's created_at is the date of the tagged commit, so when the two are more than
     * {@link #MAX_PUBLISH_DELAY} apart, that is the date used. A smaller difference is normal (the tag commit comes
     * a few days before the release), and using the earlier of the two always would move those dates too.
     *
     * @param createdAt the release's created_at, can be null
     * @param publishedAt the release's published_at, can be null
     * @return an ISO date-time, null if publishedAt is
     */
    static String releaseTime(String createdAt, String publishedAt, String tagName) {
        if (createdAt == null || publishedAt == null) {
            return publishedAt;
        }
        if (Duration.between(Instant.parse(createdAt), Instant.parse(publishedAt)).compareTo(MAX_PUBLISH_DELAY) > 0) {
            Log.warnf("GitHub release %s was published (%s) long after its tag commit (%s); using the commit date",
                    tagName, publishedAt, createdAt);
            return createdAt;
        }
        return publishedAt;
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
     * @return the merge-base of two release branches, or null if either doesn't exist
     */
    public BranchLineage.MergeBase fetchMergeBase(String base, String head) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/%s/%s/compare/%s...%s".formatted(org, repo, base, head);
        HttpResponse<String> response = send(api(url));
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " comparing " + base + " with " + head);
        }
        JsonNode commit = mapper.readTree(response.body()).get("merge_base_commit");
        if (commit == null || commit.isNull()) {
            return null;
        }
        return new BranchLineage.MergeBase(commit.path("sha").asText(),
                Instant.parse(commit.path("commit").path("committer").path("date").asText()));
    }

    /**
     * Whether release tag {@code earlier} is contained in release tag {@code micro}, judged by
     * {@link Lineage#contained} from the commits of {@code earlier} that {@code micro} doesn't have.
     *
     * @return false if either tag doesn't exist
     */
    public boolean contains(String micro, String earlier) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/%s/%s/compare/%s...%s".formatted(org, repo, micro, earlier);
        HttpResponse<String> response = send(api(url + "?per_page=" + MAX_RELEASE_COMMITS));
        if (response.statusCode() == 404) {
            return false;
        }
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " comparing " + micro + " with " + earlier);
        }
        JsonNode json = mapper.readTree(response.body());
        // ahead_by: commits in earlier that aren't in micro; listed oldest first
        int extra = json.path("ahead_by").asInt();
        List<String> messages = new ArrayList<>();
        if (extra <= MAX_RELEASE_COMMITS) {
            for (JsonNode commit : json.path("commits")) {
                messages.add(commit.path("commit").path("message").asText("").lines().findFirst().orElse(""));
            }
        }
        return Lineage.contained(earlier, extra, messages);
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
