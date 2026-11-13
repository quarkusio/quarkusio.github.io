package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * One entry of _data/releases.yaml. Dates are null when unknown or open-ended.
 * An upcoming release has no release date, only an optional planned date.
 * branchedFromVersion is a release version (e.g. "3.39"); the dates of the vendors' own branch points are
 * {@link EnterpriseOffering#branchedFromVersion()}, resolved to dates by {@link Releases#branchPoint}. rhbq, ibm and herodevs are null without a block for that vendor.
 * Values which aren't in the data are computed: when open-ended support starts (here), and the planned end of an
 * upcoming release ({@link Releases#plannedEndDate(Release)}), and the latest micro ({@link #latestMicro()}).
 * micros are the published micro releases, newest first; empty for an upcoming release.
 */
public record Release(String version, LocalDate releaseDate, LocalDate plannedDate, LocalDate eolDate,
        boolean lts, boolean upcoming, String link,
        EnterpriseOffering rhbq, EnterpriseOffering ibm, EnterpriseOffering herodevs, String branchedFromVersion,
        List<Micro> micros) {

    public static Release from(Releases.Entry e) {
        List<Micro> micros = e.micros() == null ? List.of()
                : e.micros().stream().map(m -> new Micro(m.version(), date(m.releaseDate()))).toList();
        return new Release(e.version(), date(e.releaseDate()), date(e.plannedDate()), date(e.eolDate()),
                e.lts(), e.upcoming(), e.link(),
                enterprise(e.rhbq()), enterprise(e.ibm()), enterprise(e.herodevs()), e.branchedFrom(), micros);
    }

    /**
     * The most recent micro release (the first in {@link #micros()}, which the data lists newest first), or null if
     * there are none.
     */
    public Micro latestMicro() {
        return micros.isEmpty() ? null : micros.getFirst();
    }

    private static EnterpriseOffering enterprise(Releases.EnterpriseDates dates) {
        return dates == null ? null
                : new EnterpriseOffering(date(dates.releaseDate()), date(dates.eolDate()), dates.branchedFrom(),
                        dates.openEnded());
    }

    /**
     * Turns a yyyy-MM-dd string from the data into a date.
     * TODO remove once Roq can bind java.time types (https://github.com/quarkiverse/quarkus-roq/pull/1381): the {@link Releases} records
     * can then declare LocalDate, and from and enterprise pass the dates straight through.
     */
    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    public enum Status {
        UPCOMING, ACTIVE, COMMUNITY, ENTERPRISE_ONLY, OPEN_ENDED_ONLY, EOL
    }

    /** End of community maintenance; null when open-ended. */
    public LocalDate communityEnd() {
        return eolDate;
    }

    /** Latest end of Red Hat or IBM support, or null if neither applies. */
    public LocalDate vendorEnd() {
        return later(rhbq == null ? null : rhbq.eolDate(), ibm == null ? null : ibm.eolDate());
    }

    /** End of enterprise support, only when it extends past community maintenance. */
    public LocalDate enterpriseEnd() {
        LocalDate vendor = vendorEnd();
        if (vendor == null || eolDate == null || !vendor.isAfter(eolDate)) {
            return null;
        }
        return vendor;
    }

    /** Last day of any dated support (community or enterprise); null if open-ended or unknown. */
    public LocalDate supportEnd() {
        if (eolDate == null) {
            return null;
        }
        return later(eolDate, vendorEnd());
    }

    /** Any support offering is running on d: community, Red Hat, IBM or HeroDevs. */
    public boolean supportActive(LocalDate d) {
        return communityActive(d) || anyOfferingActiveOn(d) || hasOpenEndedSupport();
    }

    public boolean hasVendorSupport() {
        return (rhbq != null && rhbq.hasEndDate()) || (ibm != null && ibm.hasEndDate());
    }

    /** Community maintenance is running: not upcoming, and either open-ended or ending after d. */
    public boolean communityActive(LocalDate d) {
        return !upcoming && (eolDate == null || eolDate.isAfter(d));
    }

    public boolean communityActive(LocalDateTime now) {
        return communityActive(now.toLocalDate());
    }

    /**
     * When a vendor's support with no end date starts: when its own dated support ends, or, for a vendor with no build
     * of its own (HeroDevs), once all dated vendor support has ended; never before community support ends.
     */
    public LocalDate openEndedStart(EnterpriseOffering vendor) {
        return later(eolDate, vendor.hasRelease() ? vendor.eolDate() : vendorEnd());
    }

    /** Support with no end date, IBM's open-ended support or HeroDevs' Never-Ending Support, is offered. */
    public boolean hasOpenEndedSupport() {
        return (ibm != null && ibm.openEnded()) || (herodevs != null && herodevs.openEnded());
    }

    /**
     * Some vendor's dated support for the release is running on d. Called from Java with a date; templates use the
     * {@link #anyOfferingActiveOn(LocalDateTime)} overload.
     */
    public boolean anyOfferingActiveOn(LocalDate d) {
        return vendors().anyMatch(vendor -> vendor.activeOn(d));
    }

    /**
     * Same as {@link #anyOfferingActiveOn(LocalDate)}, for templates: Roq gives them {@code now} as a LocalDateTime
     * (releases-table-band.html marks supported rows with it).
     */
    public boolean anyOfferingActiveOn(LocalDateTime now) {
        return anyOfferingActiveOn(now.toLocalDate());
    }

    /** The vendor offerings the release has. */
    private Stream<EnterpriseOffering> vendors() {
        return Stream.of(rhbq, ibm, herodevs).filter(Objects::nonNull);
    }

    /** Community maintenance has ended: has an end date that is not after d. */
    public boolean ended(LocalDate d) {
        return eolDate != null && !eolDate.isAfter(d);
    }

    public boolean ended(LocalDateTime now) {
        return ended(now.toLocalDate());
    }

    public boolean isSupportedOn(LocalDate d) {
        return status(d) != Status.EOL && status(d) != Status.UPCOMING && status(d) != Status.OPEN_ENDED_ONLY;
    }

    public Status status(LocalDate d) {
        Objects.requireNonNull(d);
        if (upcoming) {
            return Status.UPCOMING;
        }
        if (eolDate == null) {
            return Status.ACTIVE;
        }
        if (eolDate.isAfter(d)) {
            return Status.COMMUNITY;
        }
        LocalDate vendor = vendorEnd();
        if (vendor != null && vendor.isAfter(d)) {
            return Status.ENTERPRISE_ONLY;
        }
        if (hasOpenEndedSupport()) {
            return Status.OPEN_ENDED_ONLY;
        }
        return Status.EOL;
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }
}
