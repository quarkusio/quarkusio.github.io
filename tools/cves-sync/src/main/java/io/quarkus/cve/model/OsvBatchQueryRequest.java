package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request body for POST /v1/querybatch — query multiple packages at once.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvBatchQueryRequest {

    @JsonProperty("queries")
    public List<OsvQueryRequest> queries;

    public OsvBatchQueryRequest() {}

    public OsvBatchQueryRequest(List<OsvQueryRequest> queries) {
        this.queries = queries;
    }
}
