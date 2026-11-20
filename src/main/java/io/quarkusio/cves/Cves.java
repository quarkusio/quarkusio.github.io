package io.quarkusio.cves;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/**
 * The part of _data/cves.yaml that the CVE plot reads, bound by Roq at build time. Advisories are keyed
 * by GHSA id. Dates stay strings (see {@code Releases}); {@link CveScatterFactory} parses them. The file only
 * carries advisories: release dates and lineage come from releases.yaml, see {@link Lineage}.
 */
@DataMapping("cves")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cves(Map<String, Advisory> advisories, List<Pending> pending) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Advisory(String cve, String severity, String published, String summary,
            List<Component> affectedComponents, AffectedReleases affectedReleases) {
    }

    /** One affected component; the data repeats it once per dependency version shipped. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Component(String component) {
    }

    /**
     * The releases an advisory affects: some listed one by one, the rest as pieces of the release tree. A piece is
     * a root and every release built on it, except the subtrees under its cuts (the first releases with the fix).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AffectedReleases(List<String> list, List<Piece> tree) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Piece(String root, List<String> cuts) {
    }

    /** A CVE that has been published but not yet assessed against Quarkus releases. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pending(String id, String severity, String published, String summary, List<String> artifacts,
            String link) {
    }
}
