package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents one version range in an OSV affected entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvRange {

    @JsonProperty("type")
    public String type;

    @JsonProperty("events")
    public List<OsvVersion> events;

    public OsvRange() {}
}
