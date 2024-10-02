//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//JAVAC_OPTIONS -parameters
//DEPS io.quarkus.platform:quarkus-bom:3.12.1@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-smallrye-graphql-client
//DEPS io.quarkus:quarkus-qute
//DEPS org.commonmark:commonmark:0.22.0
//DEPS io.quarkus:quarkus-config-yaml
//FILES templates/=templates/*
//FILES application.yaml

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import picocli.CommandLine;

@CommandLine.Command(name = "generate-page", mixinStandardHelpOptions = true)
public class main implements Callable<Integer> {

    @ConfigProperty(name = "github.token")
    String token;

    @ConfigProperty(name = "working-groups.organizations", defaultValue = "quarkusio,quarkiverse")
    List<String> organizations;

    @ConfigProperty(name = "working-groups.project-prefix", defaultValue = "WG -")
    String prefix;

    @ConfigProperty(name = "working-groups.output", defaultValue = "wg.yaml")
    File out;

    @GraphQLClient("github")
    DynamicGraphQLClient githubClient;

    @Inject
    @Location("wg.yaml.template")
    Template yaml;

    @Override
    public Integer call() throws Exception {
        Log.infof("Looking for working group projects in the following organizations: %s", organizations);
        Log.infof("Working group projects should start with the following prefix: `%s`", prefix);

        List<Board> boards = new ArrayList<>();
        for (String org : organizations) {
            boards.addAll(getAllProjectsForOrganization(org));
        }

        Log.infof("Found %d working group projects", boards.size());
        boards.sort(Comparator.comparing(Board::updateDate).reversed());

        Files.writeString(out.toPath(), yaml.data("boards", boards).render());

        return 0;
    }

    public List<Board> getAllProjectsForOrganization(String org) throws ExecutionException, InterruptedException {
        List<Board> boards = new ArrayList<>();
        Map<String, Object> variables = Map.of("organization", org, "first", 100);
        Response response = githubClient.executeSync("""
                 query($organization: String!, $first: Int!){
                     organization(login: $organization){
                       projectsV2(first: $first) {
                       nodes {
                         id
                         title
                         shortDescription
                         readme
                         url
                         updatedAt
                         statusUpdates(first: $first) {
                           nodes {
                             id
                             body
                             bodyHTML
                             status
                             createdAt
                           }
                           pageInfo {
                             endCursor
                             hasNextPage
                           }
                         }
                       }
                       pageInfo {
                         endCursor
                         hasNextPage
                       }
                     }
                     }
                   }
                """, variables);


        System.out.println("Response: " + response);        
        System.out.println("Errors: " + response.getErrors());
        System.out.println("Data:\n" + response.getData());        
        JsonArray array = response.getData().getJsonObject("organization").getJsonObject("projectsV2")
                .getJsonArray("nodes");
        for (JsonValue value : array) {
            var json = value.asJsonObject();
            var t = json.getString("title");
            if (t.startsWith(prefix)) {
                var title = t.substring(prefix.length() + 1).trim();
                var shortDescription = json.getString("shortDescription");
                var longDescription = json.getString("readme");
                var id = json.getString("id");
                var url = json.getString("url");
                var updatedAt = json.getString("updatedAt");
                OffsetDateTime updatedAtDate = OffsetDateTime.parse(updatedAt);
                var updateDate = updatedAtDate.toInstant();

                // Parse the updates
                List<Update> updates = new ArrayList<>();
                var jsonForStatusUpdates = json.getJsonObject("statusUpdates");
                for (JsonValue n : jsonForStatusUpdates.getJsonArray("nodes")) {
                    var obj = n.asJsonObject();
                    var update = new Update(obj.getString("id"), obj.getString("body"), obj.getString("bodyHTML"),
                            obj.getString("status"), OffsetDateTime.parse(obj.getString("createdAt")).toInstant());
                    updates.add(update);
                }

                boards.add(new Board(id, org, title, shortDescription, longDescription, url, updateDate, updates));
            }
        }
        return boards;
    }

    record Board(
            String id,
            String org,
            String title,
            String shortDescription,
            String longDescription,
            String url,
            Instant updateDate,
            List<Update> statusUpdates) {

        public enum Status {
            INACTIVE,
            ON_TRACK,
            AT_RISK,
            OFF_TRACK,
            COMPLETE,
            STALED
        }

        public String getLastActivityDate() {
            LocalDateTime dateTime = LocalDateTime.ofInstant(updateDate, ZoneId.of("UTC"));

            // Define the formatter
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Format the LocalDateTime
            return dateTime.format(formatter);
        }

        public Update getLastUpdate() {
            if (statusUpdates.isEmpty()) {
                return null;
            }

            statusUpdates.sort(Comparator.comparing(Update::updateAt).reversed());
            return statusUpdates.getFirst();
        }

        public String getReadme() {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(longDescription());
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            return renderer.render(document);
        }

        public String getIndentedReadme() {
            String readme = getReadme();
            return readme.replaceAll("\n", "\n        ").trim();
        }

        public String getIndentedLastUpdate() {
            String lastUpdateBody = getLastUpdate().body();
            return lastUpdateBody.replaceAll("\n", "\n        ").trim();
        }

        public boolean isCompleted() {
            var last = getLastUpdate();
            return last != null && last.status().equals("COMPLETE");
        }

        public Status getStatus() {
            if (statusUpdates.isEmpty()) {
                return Status.INACTIVE;
            }

            statusUpdates.sort(Comparator.comparing(Update::updateAt).reversed());
            Update update = statusUpdates.getFirst();

            // Is it completed?
            if (update.status().equals("COMPLETE")) {
                return Status.COMPLETE;
            }

            // Is it inactive?
            if (update.status().equals("INACTIVE")) {
                return Status.INACTIVE;
            }

            // Is it staled?
            // Months is an unsupported unit, so using days
            if (update.updateAt().isBefore(Instant.now().minus(60, ChronoUnit.DAYS))) {
                return Status.STALED;
            }

            if (update.status().equals("ON_TRACK")) {
                return Status.ON_TRACK;
            }

            if (update.status().equals("AT_RISK")) {
                return Status.AT_RISK;
            }

            if (update.status().equals("OFF_TRACK")) {
                return Status.OFF_TRACK;
            }

            Log.warn("Unable to determine status of working group " + url + ", using INACTIVE as default");
            return Status.INACTIVE;

        }

        public String getBadgeClass() {
            return switch (getStatus()) {
                case INACTIVE -> "text-bg-secondary";
                case ON_TRACK -> "text-bg-success";
                case AT_RISK, STALED -> "text-bg-warning";
                case OFF_TRACK -> "text-bg-danger";
                case COMPLETE -> "text-bg-info";
            };
        }

        public String getBadgeText() {
            return getStatus().name().toLowerCase().replace("_", " ");
        }

    }

    record Update(String id, String body, String bodyHtml, String status, Instant updateAt) {

    }

}
