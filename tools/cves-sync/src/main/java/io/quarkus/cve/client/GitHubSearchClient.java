package io.quarkus.cve.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * GitHub code search, used to find which CVE records mention Quarkus. cve.org is the root of CVE data
 * but its list and search endpoints are only open to CVE Numbering Authorities, while its records are
 * mirrored in the {@code CVEProject/cvelistV5} repository, which is searchable here. Shares the base URL,
 * headers and token of the advisory client.
 *
 * Code search has its own, much lower, rate limit than the rest of the API.
 */
@RegisterRestClient(configKey = "github-api")
@RegisterProvider(GitHubAuthFilter.class)
@RegisterProvider(HttpLogFilter.class)
@Path("/search/code")
@Produces(MediaType.APPLICATION_JSON)
public interface GitHubSearchClient {

    @GET
    CodeSearchResult search(
            @QueryParam("q") String query,
            @QueryParam("per_page") int perPage,
            @QueryParam("page") int page);

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CodeSearchResult {
        @JsonProperty("total_count")
        public int totalCount;

        @JsonProperty("incomplete_results")
        public boolean incompleteResults;

        public List<Item> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Item {
        /** File name, e.g. {@code CVE-2026-87743.json}. */
        public String name;
    }
}
