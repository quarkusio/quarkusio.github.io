package io.quarkus.cve.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for the GitHub Security Advisory API.
 * Used to supplement OSV for CVEs that have GHSA records but haven't been
 * ingested into OSV's Maven ecosystem yet.
 *
 * Docs: https://docs.github.com/en/rest/security-advisories/global-advisories
 */
@RegisterRestClient(configKey = "github-api")
@RegisterProvider(GitHubAuthFilter.class)
@RegisterProvider(HttpLogFilter.class)
@Path("/advisories")
@Produces(MediaType.APPLICATION_JSON)
public interface GitHubAdvisoryClient {

    /**
     * List global security advisories for a given Maven package name.
     * Returns up to 100 per page; use page parameter for pagination.
     * Callers should pass sort=updated, direction=desc so that any modification to any advisory
     * changes page 1 - this makes the page-1 ETag a valid change marker for the whole result.
     *
     * Returns the raw {@link Response} so callers can read ETag / rate-limit headers and handle
     * {@code 304 Not Modified}. Pass the previously seen ETag as {@code ifNoneMatch} (null to omit);
     * read the body with {@code readEntity(new GenericType<List<GitHubAdvisory>>() {})}.
     */
    @GET
    Response listAdvisories(
            @QueryParam("ecosystem") String ecosystem,
            @QueryParam("affects") String packageName,
            @QueryParam("per_page") int perPage,
            @QueryParam("page") int page,
            @QueryParam("sort") String sort,
            @QueryParam("direction") String direction,
            @HeaderParam("If-None-Match") String ifNoneMatch
    );

    @JsonIgnoreProperties(ignoreUnknown = true)
    class GitHubAdvisory {

        @JsonProperty("ghsa_id")
        public String ghsaId;

        @JsonProperty("cve_id")
        public String cveId;

        @JsonProperty("summary")
        public String summary;

        @JsonProperty("description")
        public String description;

        @JsonProperty("severity")
        public String severity;   // "critical", "high", "medium", "low"

        @JsonProperty("published_at")
        public String publishedAt;

        @JsonProperty("updated_at")
        public String updatedAt;

        @JsonProperty("withdrawn_at")
        public String withdrawnAt;

        @JsonProperty("vulnerabilities")
        public List<GitHubVulnerability> vulnerabilities;

        @JsonProperty("references")
        public List<String> references;

        @JsonProperty("cvss_severities")
        public CvssSeverities cvssSeverities;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class GitHubVulnerability {

        @JsonProperty("package")
        public GitHubPackage pkg;

        @JsonProperty("first_patched_version")
        public String firstPatchedVersion;

        @JsonProperty("vulnerable_version_range")
        public String vulnerableVersionRange;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class GitHubPackage {
        @JsonProperty("ecosystem")
        public String ecosystem;

        @JsonProperty("name")
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CvssSeverities {
        @JsonProperty("cvss_v3")
        public CvssScore cvssV3;

        @JsonProperty("cvss_v4")
        public CvssScore cvssV4;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CvssScore {
        @JsonProperty("vector_string")
        public String vectorString;

        @JsonProperty("score")
        public Double score;
    }
}
