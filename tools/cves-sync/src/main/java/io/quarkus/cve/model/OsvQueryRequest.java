package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for POST /v1/query (single package query).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsvQueryRequest {

    @JsonProperty("package")
    public OsvPackage pkg;

    @JsonProperty("version")
    public String version;

    @JsonProperty("page_token")
    public String pageToken;

    public OsvQueryRequest() {}

    public static OsvQueryRequest forPackage(String ecosystem, String name) {
        OsvQueryRequest req = new OsvQueryRequest();
        req.pkg = new OsvPackage(ecosystem, name);
        return req;
    }

    public static OsvQueryRequest forPackageVersion(String ecosystem, String name, String version) {
        OsvQueryRequest req = new OsvQueryRequest();
        req.pkg = new OsvPackage(ecosystem, name);
        req.version = version;
        return req;
    }
}
