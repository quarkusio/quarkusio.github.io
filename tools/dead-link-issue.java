///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 21+

//DEPS org.kohsuke:github-api:1.327
//DEPS info.picocli:picocli:4.2.0

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Command(name = "report", mixinStandardHelpOptions = true,
        description = "Raises and closes issues depending on the results of dead link checking")
class Report implements Runnable {

    private static final String EYECATCHER = "QuarkusWebsiteDeadLinkHelper";
    private static final int MAX_LINKS = 50;

    // Matched as substrings, so these also cover the triage/ prefixed variants
    private static final List<String> DO_NOT_REOPEN_LABELS = List.of("wontfix", "invalid");

    @Option(names = "--token", description = "Github token to use when calling the Github API")
    private String token;

    @Option(names = "--siteUrl", description = "Base url of the public site (e.g. https://quarkus.io)")
    private String siteUrl;

    @Option(names = "--testUrl", description = "Base url used during testing (e.g. http://localhost:8090)")
    private String testUrl = "http://localhost:8090";

    @Option(names = "--guideIssueRepo", description = "The repository where issues for guide pages should be raised (e.g. quarkusio/quarkus)")
    private String guideIssueRepo;

    @Option(names = "--siteIssueRepo", description = "The repository where issues for non-guide pages should be raised (e.g. quarkusio/quarkusio.github.io)")
    private String siteIssueRepo;

    @Option(names = "--dryRun", description = "Whether to go through with making changes to the live repo")
    private boolean dryRun;

    @Option(names = "--runId", description = "The ID of the Github Action run")
    private String runId;

    @Option(names = "--resultsFile", description = "Path to the JSON results file")
    private String resultsFile = "broken-links.json";

    @Override
    public void run() {
        try {
            final GitHub github = new GitHubBuilder().withOAuthToken(token)
                    .build();

            // The crawler only writes the results file once it has finished, so a missing
            // file means it timed out or crashed. The crawl step is continue-on-error, so
            // we get here anyway — and an empty link list would close every open issue as
            // fixed. Bail out instead, and let the job go red to flag the broken crawl.
            Path filePath = Path.of(resultsFile);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("No results file at " + filePath.toAbsolutePath()
                        + ". The crawler did not finish, so its findings are unknown."
                        + " No issues will be created, reopened or closed.");
            }

            List<DeadLink> links = readResultsFile().stream()
                    .filter(link -> link.status() != 429)
                    .toList();

            if (links.size() > MAX_LINKS) {
                throw new RuntimeException("There were " + links.size() +
                        " dead links, which exceeds the threshold of " + MAX_LINKS +
                        " and seems implausible.");
            }

            System.out.println("Processing " + links.size() + " dead links.");
            links.forEach(link -> processDeadLink(github, link));

            closeResolvedIssues(github, links);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void closeResolvedIssues(GitHub github, List<DeadLink> links) throws IOException {
        closeResolvedIssuesInRepo(github, links, guideIssueRepo);
        closeResolvedIssuesInRepo(github, links, siteIssueRepo);
    }

    private void closeResolvedIssuesInRepo(GitHub github, List<DeadLink> links, String repo) throws IOException {
        String term = String.format("is:issue is:open %s in:body repo:%s", EYECATCHER, repo);
        PagedSearchIterable<GHIssue> answer = github.searchIssues()
                .q(term)
                .list();

        PagedIterator<GHIssue> iterator = answer.iterator();

        while (iterator.hasNext()) {
            GHIssue issue = iterator.next();
            String title = issue.getTitle();

            DeadLink matchingLink = links.stream()
                    .filter(link -> title.contains(link.url()))
                    .findAny()
                    .orElse(null);

            if (matchingLink == null) {
                final String comment = String.format(
                        "Build fixed:\n* Link to latest CI run: https://github.com/%s/actions/runs/%s",
                        siteIssueRepo, runId);

                if (!dryRun) {
                    issue.comment(comment);
                    issue.close();
                } else {
                    System.out.println(
                            String.format("Dry run: would close issue %s", issue.getHtmlUrl()));
                    System.out.println("Comment would be: " + comment);
                }
            } else {
                System.out.println(
                        String.format("Keeping %s open as it is still broken. The dead link is %s",
                                issue.getHtmlUrl(), matchingLink.url()));
            }
        }
    }

    private void processDeadLink(GitHub github, DeadLink link) {
        try {
            System.out.println(String.format("Found a dead link: %s (status %d on %s)",
                    link.url, link.status, getPublicReferrer(link)));

            String targetRepo = getTargetRepo(link);
            final GHRepository repository = github.getRepository(targetRepo);

            GHIssue openIssue = findIssue(github, targetRepo, link, "open", false);
            if (openIssue != null) {
                System.out.println(
                        String.format("Found an issue already covering this dead link: %s: %s",
                                openIssue.getNumber(), openIssue.getTitle()));
                return;
            }

            // The link was dead before and the issue was closed, either because it got fixed
            // and has broken again, or because it was closed without being fixed. Reopening
            // keeps the history in one place instead of filing a near-duplicate.
            GHIssue closedIssue = findIssue(github, targetRepo, link, "closed", true);
            if (closedIssue != null) {
                reopenIssue(closedIssue, link);
                return;
            }

            String title = String.format("Dead link: %s", link.url);
            String body = String.format("""
                    Dead link: %s

                    Status: %d (%s)
                    The problem link was found on %s

                    This issue was auto-created by the dead link checker.

                    ### What to do

                    - **If the link is genuinely broken:** fix or remove it in the source page.
                    - **If the link works in a browser but is flagged here:** the site may be blocking automated requests. \
                    Add it to the `DO_NOT_CHECK` list in [`LinkCrawlerTest.java`](https://github.com/quarkusio/quarkusio.github.io/blob/develop/src/test/java/io/quarkusio/LinkCrawlerTest.java) \
                    or to `exclude-urls` in the [`check-external-links.yml`](https://github.com/quarkusio/quarkusio.github.io/blob/develop/.github/workflows/check-external-links.yml) workflow. \
                    Closing this issue with a `%s` label will stop it being reopened on the next run.

                     --- %s --- Do not remove this line or the dead link helper will not be able to manage this issue
                     """, link.url, link.status, link.statusText, getPublicReferrer(link),
                    DO_NOT_REOPEN_LABELS.get(0), EYECATCHER);

            if (!dryRun) {
                var issueBuilder = repository.createIssue(title)
                        .body(body)
                        .label("dead-link");
                if (targetRepo.equals(guideIssueRepo)) {
                    issueBuilder.label("area/documentation");
                }
                GHIssue issue = issueBuilder.create();
                System.out.println(String.format("Created issue: %s", issue.getHtmlUrl()));
            } else {
                System.out.println(
                        String.format("Dry run: NOT creating issue in %s:\n %s\n%s", targetRepo, title, body));
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Github's issue search tokenises punctuation, so a quoted url can match an issue for a
    // different link. Confirm the title really does hold the url before acting on the match.
    // Only issues carrying the eyecatcher are ours to reopen; a human-filed issue about the
    // same link still counts for deduplication, which is why the check is optional.
    private GHIssue findIssue(GitHub github, String repo, DeadLink link, String state, boolean mustBeOurs)
            throws IOException {
        String term = String.format("is:issue is:%s \"%s\" in:title repo:%s", state, link.url(), repo);
        PagedSearchIterable<GHIssue> matches = github.searchIssues()
                .q(term)
                .list();

        for (GHIssue issue : matches) {
            if (!issue.getTitle().contains(link.url())) {
                continue;
            }
            if (mustBeOurs && (issue.getBody() == null || !issue.getBody().contains(EYECATCHER))) {
                continue;
            }
            return issue;
        }
        return null;
    }

    private void reopenIssue(GHIssue issue, DeadLink link) throws IOException {
        String blockingLabel = reopenBlockedBy(issue);
        if (blockingLabel != null) {
            System.out.println(
                    String.format("Not reopening %s for %s: it is labelled %s",
                            issue.getHtmlUrl(), link.url(), blockingLabel));
            return;
        }

        final String comment = String.format("""
                This link is dead again, with status %d (%s).
                * Link to latest CI run: https://github.com/%s/actions/runs/%s""",
                link.status(), link.statusText(), siteIssueRepo, runId);

        if (!dryRun) {
            issue.reopen();
            issue.comment(comment);
            System.out.println(String.format("Reopened issue: %s", issue.getHtmlUrl()));
        } else {
            System.out.println(String.format("Dry run: would reopen issue %s", issue.getHtmlUrl()));
            System.out.println("Comment would be: " + comment);
        }
    }

    // An issue closed as a false positive or as won't-fix is a deliberate decision, so
    // reopening it every run would just recreate the noise someone closed it to stop.
    private String reopenBlockedBy(GHIssue issue) {
        return issue.getLabels().stream()
                .map(label -> label.getName().toLowerCase())
                .filter(name -> DO_NOT_REOPEN_LABELS.stream().anyMatch(name::contains))
                .findFirst()
                .orElse(null);
    }

    private String getTargetRepo(DeadLink link) {
        if (link.referrer != null) {
            String path = extractPath(link.referrer);
            if (path.startsWith("/guides/") || path.startsWith("/version/")) {
                return guideIssueRepo;
            }
        }
        return siteIssueRepo;
    }

    private String getPublicReferrer(DeadLink link) {
        if (link.referrer == null) {
            return "(unknown page)";
        }
        return link.referrer.replace(testUrl, siteUrl);
    }

    private static String extractPath(String url) {
        try {
            return URI.create(url).getPath();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    private List<DeadLink> readResultsFile() throws IOException {
        String content = Files.readString(Path.of(resultsFile));
        ObjectMapper mapper = new ObjectMapper();

        // Try to parse as the new structured format first
        try {
            var root = mapper.readTree(content);
            if (root.has("reportable")) {
                // New format with separate categories - only process reportable links
                return mapper.readValue(
                    root.get("reportable").toString(),
                    new TypeReference<List<DeadLink>>() {}
                );
            }
        } catch (Exception e) {
            // Fall through to old format
        }

        // Old format - direct list of links
        return mapper.readValue(content, new TypeReference<List<DeadLink>>() {
        });
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new Report()).execute(args);
        System.exit(exitCode);
    }

    record DeadLink(
            String url,
            int status,
            String statusText,
            String referrer) {
    }
}
