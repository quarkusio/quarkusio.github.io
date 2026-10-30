package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Version specifier used inside an OSV affected range event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvVersion {

    @JsonProperty("introduced")
    public String introduced;

    @JsonProperty("fixed")
    public String fixed;

    @JsonProperty("last_affected")
    public String lastAffected;

    public OsvVersion() {}
}
