package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an OSV package identifier (ecosystem + name + optional purl).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvPackage {

    @JsonProperty("ecosystem")
    public String ecosystem;

    @JsonProperty("name")
    public String name;

    @JsonProperty("purl")
    public String purl;

    public OsvPackage() {}

    public OsvPackage(String ecosystem, String name) {
        this.ecosystem = ecosystem;
        this.name = name;
    }

    @Override
    public String toString() {
        return ecosystem + ":" + name;
    }
}
