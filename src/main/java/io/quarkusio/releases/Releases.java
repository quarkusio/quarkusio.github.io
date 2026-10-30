package io.quarkusio.releases;

import java.util.List;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/**
 * The shape of _data/releases.yaml, bound by Roq at build time. Dates stay strings here because
 * Quarkus' bytecode recorder cannot record java.time types; {@link ReleaseCatalog} turns each
 * {@link Entry} into a typed {@link Release}. Templates should use the catalog, not this bean.
 * snake_case keys map to these components through quarkus.roq.jackson.property-naming-strategy.
 */
@DataMapping("releases")
public record Releases(Policy policy, SupportProviders supportProviders, List<EolSupport> eolSupport,
        List<Entry> releases) {

    public record Policy(String ltsCadence, String ltsSupportDuration, String nonLtsSupportDuration,
            String ltsDescription, String eolDescription) {
    }

    public record SupportProviders(Provider rhbq, Provider ibm, Provider herodevs) {
    }

    public record Provider(String name, String url) {
    }

    public record EolSupport(String name, String url, String description) {
    }

    /** One release as written in the YAML: dates are yyyy-MM-dd strings, herodevs_eol may be "nes". */
    public record Entry(String version, String releaseDate, String eolDate, boolean lts,
            boolean upcoming, String latestPatch, String latestPatchDate, String link, String rhbqEol,
            String ibmEol, String herodevsEol) {
    }
}
