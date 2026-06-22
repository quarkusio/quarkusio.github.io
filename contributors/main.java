///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus.platform:quarkus-bom:3.12.1@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-smallrye-graphql-client
//DEPS io.quarkus:quarkus-qute
//DEPS io.quarkus:quarkus-config-yaml
//DEPS org.yaml:snakeyaml:2.2
//FILES templates/=templates/*
//FILES application.yaml

import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandLine.Command(name = "generate-contributors", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {

    @ConfigProperty(name = "github.token")
    String token;

    @ConfigProperty(name = "contributors.org", defaultValue = "quarkusio")
    String org;

    @ConfigProperty(name = "contributors.repo", defaultValue = "quarkus")
    String repo;

    @ConfigProperty(name = "contributors.months", defaultValue = "6")
    int months;

    @ConfigProperty(name = "contributors.output", defaultValue = "contributors.yaml")
    File out;

    @ConfigProperty(name = "contributors.opt-in-url",
            defaultValue = "https://raw.githubusercontent.com/quarkusio/quarkus-extension-catalog/main/named-contributing-orgs-opt-in.yml")
    String optInUrl;

    @GraphQLClient("github")
    DynamicGraphQLClient githubClient;

    @Inject
    @Location("contributors.yaml.template")
    Template yaml;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, String> companyCache = new HashMap<>();

    @Override
    public Integer call() throws Exception {
        Log.infof("Fetching contributor data for %s/%s over the last %d months", org, repo, months);

        Set<String> optInNames = fetchOptInList();
        Log.infof("Loaded %d opted-in organization names", optInNames.size());

        List<RawCommit> commits = fetchCommitHistory();
        Log.infof("Fetched %d commits (after filtering merges and bots)", commits.size());

        Map<String, ContributorData> byLogin = aggregateByContributor(commits);
        Log.infof("Found %d unique contributors", byLogin.size());

        List<CompanyData> companies = aggregateByCompany(byLogin.values(), optInNames);

        // To include individual contributors in the output, pass them to the template as "contributors"
        // (also restore the contributors section in contributors.yaml.template)

        int totalCommits = byLogin.values().stream().mapToInt(ContributorData::contributions).sum();

        for (CompanyData c : companies) {
            c.percentage = Math.round(c.contributions * 1000.0 / totalCommits) / 10.0;
        }

        String generated = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String output = yaml
                .data("generated", generated)
                .data("periodMonths", months)
                .data("totalCommits", totalCommits)
                .data("companies", companies)
                .render();

        Files.writeString(out.toPath(), output);
        Log.infof("Wrote contributor data to %s", out.getPath());

        return 0;
    }

    // --- GitHub GraphQL: fetch commit history with pagination ---

    List<RawCommit> fetchCommitHistory() throws ExecutionException, InterruptedException {
        List<RawCommit> result = new ArrayList<>();
        String since = Instant.now().minus((long) (months * 30.5), ChronoUnit.DAYS)
                .atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            String afterClause = cursor != null ? ", after: \"" + cursor + "\"" : "";

            String query = """
                    query {
                        repository(owner: "%s", name: "%s") {
                            defaultBranchRef {
                                target {
                                    ... on Commit {
                                        history(since: "%s"%s, first: 100) {
                                            pageInfo {
                                                hasNextPage
                                                endCursor
                                            }
                                            edges {
                                                node {
                                                    ... on Commit {
                                                        parents(first: 1) {
                                                            totalCount
                                                        }
                                                        author {
                                                            user {
                                                                login
                                                                name
                                                                company
                                                                url
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    """.formatted(org, repo, since, afterClause);

            Response response = githubClient.executeSync(query);

            if (response.hasError()) {
                Log.errorf("GraphQL errors: %s", response.getErrors());
                break;
            }

            JsonObject history = response.getData()
                    .getJsonObject("repository")
                    .getJsonObject("defaultBranchRef")
                    .getJsonObject("target")
                    .getJsonObject("history");

            JsonObject pageInfo = history.getJsonObject("pageInfo");
            hasNextPage = pageInfo.getBoolean("hasNextPage");
            cursor = pageInfo.getString("endCursor");

            JsonArray edges = history.getJsonArray("edges");
            for (JsonValue edge : edges) {
                JsonObject node = edge.asJsonObject().getJsonObject("node");

                int parentCount = node.getJsonObject("parents").getInt("totalCount");
                if (parentCount > 1) {
                    continue;
                }

                JsonObject author = node.getJsonObject("author");
                if (author.isNull("user")) {
                    continue;
                }
                JsonObject user = author.getJsonObject("user");

                String login = user.getString("login", null);
                if (login == null || isBot(login)) {
                    continue;
                }

                String name = user.getString("name", login);
                String company = user.getString("company", null);
                if (company != null && company.isBlank()) {
                    company = null;
                }
                String url = user.getString("url", null);

                result.add(new RawCommit(login, name, company, url));
            }

            Log.infof("Fetched %d commits so far (hasNextPage=%b)", result.size(), hasNextPage);
        }

        return result;
    }

    // --- Company name resolution and normalization (ported from sponsorFinder.js) ---

    String resolveAndNormalizeCompanyName(String company) {
        if (company == null) {
            return null;
        }

        if (company.startsWith("@")) {
            String[] parts = company.split("[@ ]");
            // First element is empty (before @), take second
            String login = parts.length > 1 ? parts[1].replace(",", "").trim() : null;
            if (login != null && !login.isBlank()) {
                return normalizeCompanyName(resolveCompanyFromGitHubLogin(login));
            }
            return null;
        }

        return normalizeCompanyName(company);
    }

    static final Pattern PARENTHESIS_PATTERN = Pattern.compile("(.*?)\\s+\\(.*\\)");
    static final Pattern AT_PATTERN = Pattern.compile("(.*?)( - )?@.*");
    static final Pattern BY_PATTERN = Pattern.compile("(.*?)\\s+by\\s+.*");

    static String normalizeCompanyName(String company) {
        if (company == null || company.isBlank()) {
            return null;
        }

        // If multiple companies listed, take the first
        String name = company.split("(,|\\band\\b|&)")[0];

        name = name
                .replace(", Inc.", "")
                .replace(", Inc", "")
                .replace(" GmbH", "");

        Matcher m = PARENTHESIS_PATTERN.matcher(name);
        if (m.matches()) {
            name = m.group(1);
        }

        m = AT_PATTERN.matcher(name);
        if (m.matches()) {
            name = m.group(1);
        }

        m = BY_PATTERN.matcher(name);
        if (m.matches()) {
            name = m.group(1);
        }

        name = name
                .replace("https://www.redhat.com/", "Red Hat")
                .replace("http://www.redhat.com/", "Red Hat");

        name = name.replace(",", "").trim();

        name = name.replace("International Business Machines", "IBM");
        name = name.replace("JBoss", "Red Hat");

        if (name.startsWith("Red Hat")) {
            name = "Red Hat & IBM";
        } else if (name.startsWith("IBM")) {
            name = "Red Hat & IBM";
        }

        return name;
    }

    String resolveCompanyFromGitHubLogin(String login) {
        if (companyCache.containsKey(login)) {
            return companyCache.get(login);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/users/" + login))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Simple JSON parsing — extract "name" field
                String body = response.body();
                String name = extractJsonStringField(body, "name");
                if (name == null || name.isBlank()) {
                    name = login;
                }
                companyCache.put(login, name);
                return name;
            }
        } catch (Exception e) {
            Log.warnf("Failed to resolve GitHub login %s: %s", login, e.getMessage());
        }

        companyCache.put(login, login);
        return login;
    }

    static String extractJsonStringField(String json, String field) {
        // Minimal JSON field extraction to avoid adding a JSON parsing dependency for REST responses
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    // --- Bot filtering ---

    static boolean isBot(String login) {
        return login.contains("[bot]")
                || "actions-user".equals(login)
                || "quarkiversebot".equals(login);
    }

    // --- Aggregation ---

    Map<String, ContributorData> aggregateByContributor(List<RawCommit> commits) {
        Map<String, ContributorData> byLogin = new HashMap<>();

        for (RawCommit commit : commits) {
            String resolvedCompany = resolveAndNormalizeCompanyName(commit.company);

            ContributorData existing = byLogin.get(commit.login);
            if (existing != null) {
                existing.contributions++;
            } else {
                byLogin.put(commit.login, new ContributorData(
                        commit.login,
                        commit.name != null ? commit.name : commit.login,
                        1,
                        resolvedCompany != null ? resolvedCompany : "Community"
                ));
            }
        }

        return byLogin;
    }

    List<CompanyData> aggregateByCompany(java.util.Collection<ContributorData> contributors, Set<String> optInNames) {
        Map<String, CompanyData> byCompany = new HashMap<>();

        for (ContributorData c : contributors) {
            String companyName = isOptedIn(c.company, optInNames) ? c.company : "Community";

            CompanyData existing = byCompany.get(companyName);
            if (existing != null) {
                existing.contributions += c.contributions;
                existing.contributorCount++;
            } else {
                byCompany.put(companyName, new CompanyData(companyName, c.contributions, 1, 0));
            }
        }

        List<CompanyData> result = new ArrayList<>(byCompany.values());
        result.sort(Comparator.comparingInt(CompanyData::contributions).reversed());
        return result;
    }

    // --- Opt-in list ---

    @SuppressWarnings("unchecked")
    Set<String> fetchOptInList() {
        Set<String> names = new HashSet<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(optInUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Yaml yamlParser = new Yaml();
                Map<String, Object> data = yamlParser.load(response.body());

                if (data.containsKey("named-sponsors")) {
                    Object sponsors = data.get("named-sponsors");
                    if (sponsors instanceof List) {
                        ((List<String>) sponsors).forEach(s -> names.add(normalizeCompanyName(s)));
                    }
                }

                if (data.containsKey("named-contributing-orgs")) {
                    Object orgs = data.get("named-contributing-orgs");
                    if (orgs instanceof List) {
                        ((List<String>) orgs).forEach(s -> names.add(normalizeCompanyName(s)));
                    }
                }
            } else {
                Log.warnf("Failed to fetch opt-in list (HTTP %d), all companies will appear as Community", response.statusCode());
            }
        } catch (Exception e) {
            Log.warnf("Failed to fetch opt-in list: %s. All companies will appear as Community", e.getMessage());
        }

        // Community is always allowed
        names.add("Community");

        return names;
    }

    static boolean isOptedIn(String companyName, Set<String> optInNames) {
        return companyName != null && optInNames.contains(companyName);
    }

    // --- Data types ---

    record RawCommit(String login, String name, String company, String url) {
    }

    static final class ContributorData {
        final String login;
        final String name;
        int contributions;
        String company;

        ContributorData(String login, String name, int contributions, String company) {
            this.login = login;
            this.name = name;
            this.contributions = contributions;
            this.company = company;
        }

        public String login() { return login; }
        public String name() { return name; }
        public int contributions() { return contributions; }
        public String company() { return company; }
    }

    static final class CompanyData {
        final String name;
        int contributions;
        int contributorCount;
        double percentage;

        CompanyData(String name, int contributions, int contributorCount, double percentage) {
            this.name = name;
            this.contributions = contributions;
            this.contributorCount = contributorCount;
            this.percentage = percentage;
        }

        public String name() { return name; }
        public int contributions() { return contributions; }
        public int contributorCount() { return contributorCount; }
        public double percentage() { return percentage; }
    }
}
