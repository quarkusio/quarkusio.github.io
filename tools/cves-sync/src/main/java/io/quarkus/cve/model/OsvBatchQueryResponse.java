package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from POST /v1/querybatch.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvBatchQueryResponse {

    @JsonProperty("results")
    public List<OsvQueryResponse> results;

    public OsvBatchQueryResponse() {}
}
