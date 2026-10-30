package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One entry of _data/releases.yaml with real dates. Dates are null when unknown or open-ended;
 * an upcoming release has no dates at all.
 */
public record Release(String version, LocalDate releaseDate, LocalDate eolDate, boolean lts, boolean upcoming,
        String latestPatch, LocalDate latestPatchDate, String link,
        LocalDate rhbqEol, LocalDate ibmEol, HeroDevsEol herodevsEol) {

    public static Release from(Releases.Entry e) {
        return new Release(e.version(), date(e.releaseDate()), date(e.eolDate()), e.lts(), e.upcoming(),
                e.latestPatch(), date(e.latestPatchDate()), e.link(), date(e.rhbqEol()), date(e.ibmEol()),
                e.herodevsEol() == null ? null : HeroDevsEol.parse(e.herodevsEol()));
    }

    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    /** HeroDevs provides never-ending or dated support for this release. */
    public boolean hasNes() {
        return herodevsEol != null;
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
}
