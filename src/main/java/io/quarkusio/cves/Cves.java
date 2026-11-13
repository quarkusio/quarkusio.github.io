package io.quarkusio.cves;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/**
 * The part of _data/cves.yaml that the CVE plot reads, bound by Roq at build time. Advisories are keyed
 * by GHSA id. Dates stay strings (see {@code Releases}); {@link CveScatterFactory} parses them.
 * {@code releaseGroups} is camelCase in the file, so it is named explicitly. {@code from} is the oldest release line
 * the data covers, e.g. "3.27"; nothing can be said about earlier releases.
 */
@DataMapping("cves")
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cves(Map<String, Advisory> advisories, List<Pending> pending,
        @JsonProperty("releaseGroups") List<ReleaseGroup> releaseGroups, String from) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Advisory(String cve, String severity, String published, String summary, List<Affect> affects) {
    }

    /** One affected artifact; the data repeats it once per dependency version. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Affect(String artifact) {
    }

    /** A CVE that has been published but not yet assessed against Quarkus releases. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pending(String id, String severity, String published, String summary, List<String> artifacts,
            String link) {
    }

    /** One minor release line, e.g. 3.33, with each of its releases. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseGroup(List<Release> releases) {
    }

    /** A release and the advisories that still affected it (by GHSA id). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Release(String releaseDate, ReleaseCves cves) {

        public java.util.Set<String> active() {
            java.util.Set<String> ids = new java.util.HashSet<>();
            if (cves != null && cves.active() != null) {
                if (cves.active().core() != null) {
                    ids.addAll(cves.active().core());
                }
                if (cves.active().dependencies() != null) {
                    ids.addAll(cves.active().dependencies());
                }
            }
            return ids;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReleaseCves(Active active) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Active(List<String> core, List<String> dependencies) {
    }
}
