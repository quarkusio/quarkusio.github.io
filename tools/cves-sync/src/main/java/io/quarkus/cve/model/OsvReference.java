package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A reference (URL + type) attached to an OSV vulnerability.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvReference {

    @JsonProperty("type")
    public String type;

    @JsonProperty("url")
    public String url;

    public OsvReference() {}
}
