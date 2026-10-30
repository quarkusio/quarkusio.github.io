package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One entry of _data/releases.yaml. Dates are null when unknown or open-ended.
 * An upcoming release has no release date, only an optional planned date.
 */
public record Release(String version, LocalDate releaseDate, LocalDate plannedDate, LocalDate eolDate,
        boolean lts, boolean upcoming, String latestPatch, LocalDate latestPatchDate, String link,
        LocalDate rhbqEol, LocalDate ibmEol, HeroDevsEol herodevsEol) {

    public static Release from(Releases.Entry e) {
        return new Release(e.version(), date(e.releaseDate()), date(e.plannedDate()), date(e.eolDate()),
                e.lts(), e.upcoming(), e.latestPatch(), date(e.latestPatchDate()), e.link(),
                date(e.rhbqEol()), date(e.ibmEol()),
                e.herodevsEol() == null ? null : HeroDevsEol.parse(e.herodevsEol()));
    }

    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    public enum Status {
        UPCOMING, ACTIVE, COMMUNITY, ENTERPRISE_ONLY, NES_ONLY, EOL
    }

    /** End of community maintenance; null when open-ended. */
    public LocalDate communityEnd() {
        return eolDate;
    }

    /** Latest end of Red Hat or IBM support, or null if neither applies. */
    public LocalDate vendorEnd() {
        return later(rhbqEol, ibmEol);
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

    public boolean hasVendorSupport() {
        return rhbqEol != null || ibmEol != null;
    }

    public boolean hasNes() {
        return herodevsEol != null;
    }

    /** True for "nes": never-ending support with no end date. */
    public boolean herodevsNes() {
        return herodevsEol != null && herodevsEol.isNes();
    }

    /** End of dated HeroDevs support, or null when none or never-ending. */
    public LocalDate herodevsEnd() {
        return herodevsEol == null ? null : herodevsEol.date();
    }

    /** Community maintenance is running: not upcoming, and either open-ended or ending after d. */
    public boolean communityActive(LocalDate d) {
        return !upcoming && (eolDate == null || eolDate.isAfter(d));
    }

    public boolean communityActive(LocalDateTime now) {
        return communityActive(now.toLocalDate());
    }

    public boolean rhbqActive(LocalDate d) {
        return rhbqEol != null && rhbqEol.isAfter(d);
    }

    public boolean rhbqActive(LocalDateTime now) {
        return rhbqActive(now.toLocalDate());
    }

    public boolean ibmActive(LocalDate d) {
        return ibmEol != null && ibmEol.isAfter(d);
    }

    public boolean ibmActive(LocalDateTime now) {
        return ibmActive(now.toLocalDate());
    }

    /** Community maintenance has ended: has an end date that is not after d. */
    public boolean ended(LocalDate d) {
        return eolDate != null && !eolDate.isAfter(d);
    }

    public boolean ended(LocalDateTime now) {
        return ended(now.toLocalDate());
    }

    public boolean isSupportedOn(LocalDate d) {
        return status(d) != Status.EOL && status(d) != Status.UPCOMING && status(d) != Status.NES_ONLY;
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
        if (herodevsNes() || (herodevsEnd() != null && herodevsEnd().isAfter(d))) {
            return Status.NES_ONLY;
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
