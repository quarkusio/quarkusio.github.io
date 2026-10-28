package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CVSS severity info attached to an OSV vulnerability.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvSeverity {

    @JsonProperty("type")
    public String type;

    @JsonProperty("score")
    public String score;

    public OsvSeverity() {}
}
