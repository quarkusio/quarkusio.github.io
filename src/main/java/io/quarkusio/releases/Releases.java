package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/**
 * _data/releases.yaml, bound by Roq at build time, and the queries over it. Templates use it as
 * {@code cdi:releases}; template-facing methods take Roq's {@code now} (a LocalDateTime) as well as a LocalDate.
 * snake_case keys map to these components through quarkus.roq.jackson.property-naming-strategy.
 * <p>
 * Dates stay strings in {@link Entry} because Roq can't yet bind java.time types: its mappers lack the Jackson
 * java.time module, and the bytecode recorder can't record them. So {@link #all()} turns every entry into a typed
 * {@link Release} each time it is called, and every query pays for that conversion (a record can't hold the
 * converted list: all its components come from the data file). The cost is small for the site's ~100 entries.
 * TODO once https://github.com/quarkiverse/quarkus-roq/pull/1381 is released, declare the dates as LocalDate and
 * bind the releases straight to {@code List<Release>}: {@link Entry}, {@code Release.from} and the conversion, and
 * its performance penalty, go away.
 */
@DataMapping("releases")
public record Releases(Policy policy, SupportProviders supportProviders, List<EolSupport> eolSupport,
        List<Entry> releases) {

    static final int DEFAULT_MAX_UPCOMING = 1;
    static final Period DEFAULT_CADENCE = Period.ofWeeks(4);

    /** All releases, newest first, as listed in the data file. Converts every entry on each call, see above. */
    public List<Release> all() {
        return releases.stream().map(Release::from).toList();
    }

    /**
     * When an upcoming release is planned to stop being supported; null for a release that is out, one without a
     * planned date, or a non-LTS with no next release planned (the caller picks a default length).
     * Worked out from all the dated upcoming releases (not only the visible ones, so hiding later releases does not
     * change the ones that remain). A non-LTS is supported only until the next minor, so it ends where the next dated
     * upcoming release starts. An LTS runs for the policy's LTS support duration, clipped to that start but never to
     * less than the release cadence.
     */
    public LocalDate plannedEndDate(Release r) {
        if (!r.upcoming() || r.plannedDate() == null) {
            return null;
        }
        LocalDate start = r.plannedDate();
        LocalDate next = all().stream().filter(o -> o.upcoming() && o.plannedDate() != null)
                .map(Release::plannedDate).filter(d -> d.isAfter(start)).min(Comparator.naturalOrder()).orElse(null);
        Period ltsPeriod = policy == null ? null : policy.ltsSupportPeriod();
        if (!r.lts() || ltsPeriod == null) {
            return next;
        }
        LocalDate end = start.plus(ltsPeriod);
        if (next != null && next.isBefore(end)) {
            LocalDate minEnd = start.plus(cadence());
            end = next.isAfter(minEnd) ? next : minEnd;
        }
        return end;
    }

    /** The default length of an upcoming bar: the policy's release cadence. */
    public Period cadence() {
        Period p = policy == null ? null : policy.releaseCadencePeriod();
        return p == null ? DEFAULT_CADENCE : p;
    }

    /** Every upcoming release, soonest first; undated ones come last. */
    public List<Release> upcoming() {
        return all().stream().filter(Release::upcoming)
                .sorted(Comparator.comparing(Release::plannedDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Release::version))
                .toList();
    }

    /** The soonest upcoming releases, at most quarkusio.releases.max-upcoming. */
    public List<Release> visibleUpcoming() {
        return visibleUpcoming(maxUpcoming());
    }

    public List<Release> visibleUpcoming(int maxUpcoming) {
        return upcoming().stream().limit(maxUpcoming).toList();
    }

    /** All releases, but with only the visible upcoming ones; what the releases table lists. */
    public List<Release> listed() {
        return listed(maxUpcoming());
    }

    public List<Release> listed(int maxUpcoming) {
        List<Release> visible = visibleUpcoming(maxUpcoming);
        return all().stream().filter(r -> !r.upcoming() || visible.contains(r)).toList();
    }

    /** quarkusio.releases.max-upcoming (see {@link ReleasesConfig}); read here as Roq, not CDI, creates this bean. */
    private static int maxUpcoming() {
        return ConfigProvider.getConfig().getOptionalValue("quarkusio.releases.max-upcoming", Integer.class)
                .orElse(DEFAULT_MAX_UPCOMING);
    }

    /** Newest release that is out (not upcoming). */
    public Optional<Release> latest() {
        return all().stream().filter(r -> !r.upcoming()).findFirst();
    }

    /** Newest LTS whose community maintenance has not ended. */
    public Optional<Release> recommendedLts(LocalDate today) {
        return all().stream().filter(r -> r.lts() && r.communityActive(today)).findFirst();
    }

    /** Same as {@link #recommendedLts(LocalDate)}, but null when there is none, for templates. */
    public Release recommendedLtsOrNull(LocalDateTime now) {
        return recommendedLts(now.toLocalDate()).orElse(null);
    }

    public List<Release> supportedOn(LocalDate d) {
        return all().stream().filter(r -> r.isSupportedOn(d)).toList();
    }

    /** Releases whose community maintenance has ended, newest first. */
    public List<Release> endOfLife(LocalDate today) {
        return all().stream().filter(r -> r.ended(today)).toList();
    }

    public List<Release> endOfLife(LocalDateTime now) {
        return endOfLife(now.toLocalDate());
    }

    /** What the security page lists: the latest release's major, plus other supported LTS releases. */
    public SupportedVersions supportedVersions(LocalDate today) {
        Release latest = latest().orElseThrow();
        List<Release> lts = all().stream()
                .filter(r -> r.lts() && r.communityActive(today) && !r.version().equals(latest.version()))
                .toList();
        return new SupportedVersions(latest, latest.version().split("\\.")[0], lts);
    }

    public SupportedVersions supportedVersions(LocalDateTime now) {
        return supportedVersions(now.toLocalDate());
    }

    public record SupportedVersions(Release latest, String latestMajor, List<Release> otherLts) {
    }

    public Optional<Release> find(String version) {
        return all().stream().filter(r -> r.version().equals(version)).findFirst();
    }

    /** First release of each major version: 1.0, 2.0, 3.0, ... */
    public List<Release> majors() {
        return all().stream().filter(r -> !r.upcoming() && r.version().matches("\\d+\\.0")).toList();
    }

    /**
     * Where a vendor's build branched off the community line, for plotting: the release date of the release it
     * branched from (its branchedFromVersion), else of the release itself. Only minor versions can be resolved, as
     * the data has no dates for micro releases; an unknown version falls back to the release itself.
     * TODO resolve micro versions (e.g. "3.20.3", what vendors really fork from) once the data lists micro releases.
     */
    public LocalDate branchPoint(Release release, EnterpriseOffering vendor) {
        String version = vendor.branchedFromVersion();
        return version == null ? release.releaseDate()
                : find(version).map(Release::releaseDate).orElse(release.releaseDate());
    }

    /** Support provider URL by key: rhbq, ibm or herodevs. */
    public String providerUrl(String provider) {
        if (supportProviders == null) {
            return null;
        }
        Provider p = switch (provider) {
            case "rhbq" -> supportProviders.rhbq();
            case "ibm" -> supportProviders.ibm();
            case "herodevs" -> supportProviders.herodevs();
            default -> null;
        };
        return p == null ? null : p.url();
    }

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

    /**
     * A vendor block (rhbq, ibm, herodevs): dates are yyyy-MM-dd strings. branchedFrom is the community release the
     * vendor branched its build from, e.g. "3.20"; when absent it is the release the block belongs to. openEnded is true when the
     * vendor supports the release, with no end date, once its other support has ended. lastInMajor marks the vendor's last
     * minor of a major stream (3.x, 2.x), which is supported for longer than the others.
     */
    public record EnterpriseDates(String releaseDate, String eolDate, String branchedFrom, boolean openEnded,
            boolean lastInMajor) {
    }

    /**
     * One release as written in the YAML: dates are yyyy-MM-dd strings. branchedFrom is the version this release
     * was branched from: for an LTS, the non-LTS minor cut with it (not necessarily the one released just before).
     */
    public record Entry(String version, String releaseDate, String plannedDate, String eolDate, boolean lts,
            boolean upcoming, boolean proposal, String latestPatch, String latestPatchDate, String link,
            EnterpriseDates rhbq, EnterpriseDates ibm, EnterpriseDates herodevs, String branchedFrom) {
    }
}
