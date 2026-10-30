package io.quarkusio.releases;

import java.time.Period;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        private static final Pattern DURATION = Pattern.compile("\\s*(\\d+)\\s+(month|year)s?\\s*", Pattern.CASE_INSENSITIVE);

        /** ltsSupportDuration as a Period (e.g. "12 months"), or null if it is not of that form. */
        public Period ltsSupportPeriod() {
            Matcher m = ltsSupportDuration == null ? null : DURATION.matcher(ltsSupportDuration);
            if (m == null || !m.matches()) {
                return null;
            }
            int n = Integer.parseInt(m.group(1));
            return m.group(2).equalsIgnoreCase("year") ? Period.ofYears(n) : Period.ofMonths(n);
        }
    }

    public record SupportProviders(Provider rhbq, Provider ibm, Provider herodevs) {
    }

    public record Provider(String name, String url) {
    }

    public record EolSupport(String name, String url, String description) {
    }

    /** One release as written in the YAML: dates are yyyy-MM-dd strings, herodevs_eol may be "nes". */
    public record Entry(String version, String releaseDate, String plannedDate, String eolDate, boolean lts,
            boolean upcoming, String latestPatch, String latestPatchDate, String link, String rhbqEol,
            String ibmEol, String herodevsEol) {
    }
}
