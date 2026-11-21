package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from POST /v1/query — a paginated list of vulnerability summaries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvQueryResponse {

    @JsonProperty("vulns")
    public List<OsvVulnerability> vulns;

    @JsonProperty("next_page_token")
    public String nextPageToken;

    public OsvQueryResponse() {}

    public boolean hasMore() {
        return nextPageToken != null && !nextPageToken.isBlank();
    }
}
