package io.quarkusio.cves;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/**
 * Bound representation of {@code _data/cves.yaml}, read by Roq at build time.
 * Advisories are keyed by GHSA id. {@code affectedComponents} and {@code affectedReleases} are camelCase
 * in the file, while Roq reads data with a snake_case mapper, so they are named explicitly.
 */
@DataMapping("cves")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cves(Map<String, Advisory> advisories, List<Pending> pending, List<Scanned> releases) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Scanned(String version) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Advisory(String cve, String severity, String published, String summary,
            @JsonProperty("affectedComponents") List<Component> affectedComponents,
            @JsonProperty("affectedReleases") AffectedReleases affectedReleases) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Component(String component, String range, Boolean core, List<String> shipped) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AffectedReleases(List<String> list, List<Piece> tree) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Piece(String root, List<String> cuts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pending(String id, String severity, String published, String summary, List<String> artifacts,
            String link) {
    }
}
