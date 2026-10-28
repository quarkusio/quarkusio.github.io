package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One affected entry in an OSV vulnerability: a package + version ranges.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvAffected {

    @JsonProperty("package")
    public OsvPackage pkg;

    @JsonProperty("ranges")
    public List<OsvRange> ranges;

    @JsonProperty("versions")
    public List<String> versions;

    public OsvAffected() {}
}
