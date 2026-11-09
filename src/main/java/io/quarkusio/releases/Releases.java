package io.quarkusio.releases;

import java.time.Period;
import java.util.List;
import java.util.Locale;

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

    public record Policy(SupportPeriod ltsCadence, SupportPeriod ltsSupportDuration, String nonLtsSupportDuration, SupportPeriod releaseCadence,
            String ltsDescription, String eolDescription) {

        public Policy {
            // Ad-hoc variable replacement, for this field only
            ltsDescription = ltsDescription == null ? null
                    : ltsDescription.replace("{lts_cadence}", ltsCadence == null ? "" : ltsCadence.label())
                            .replace("{lts_support_duration}", ltsSupportDuration == null ? "" : ltsSupportDuration.label())
                            .replace("{release_cadence}", releaseCadence == null ? "" : releaseCadence.label());
        }

        /** The gap between LTS releases, or null when not set. */
        public Period ltsCadencePeriod() {
            return ltsCadence == null ? null : ltsCadence.period();
        }

        /** The LTS support length as a Period, or null when not set. */
        public Period ltsSupportPeriod() {
            return ltsSupportDuration == null ? null : ltsSupportDuration.period();
        }

        /** The gap between minor releases, or null when not set. */
        public Period releaseCadencePeriod() {
            return releaseCadence == null ? null : releaseCadence.period();
        }
    }

    /** A length of time in whole weeks or months, e.g. {@code {amount: 4, unit: weeks}}. */
    public record SupportPeriod(int amount, String unit) {

        public SupportPeriod {
            unit = unit == null ? "months" : unit.toLowerCase(Locale.ROOT);
            if (!unit.equals("weeks") && !unit.equals("months")) {
                throw new IllegalArgumentException("Unsupported support period unit '" + unit + "', use weeks or months");
            }
        }

        public Period period() {
            return unit.equals("weeks") ? Period.ofWeeks(amount) : Period.ofMonths(amount);
        }

        @Override
        public String toString() {
            return label();
        }

        /** E.g. "12 months", "1 month". */
        public String label() {
            String singular = unit.substring(0, unit.length() - 1);
            return amount + " " + (amount == 1 ? singular : unit);
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
            boolean upcoming, boolean proposal, String latestPatch, String latestPatchDate, String link,
            String rhbqEol, String ibmEol, String herodevsEol) {
    }
}
