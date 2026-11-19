package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A vendor's commercial support for a release, as given by its vendor block in the data: its own build with
 * dated support (Red Hat, IBM), and/or support with no end date once other support has ended (IBM, HeroDevs).
 *
 * @param releaseDate when the vendor released its build; null when not given, or the vendor has no build
 * @param eolDate the end of the vendor's support for its build; null when not given, or the vendor has no build
 * @param parentRelease the community release the vendor forked its build from, a micro (e.g. "3.20.1") or a minor;
 *        null when it is this release. Stored as a release rather than a date, since that is what is known;
 *        {@link Releases#branchPoint} turns it into a date for plotting.
 * @param openEnded the vendor supports the release with no end date once its other support has ended (IBM's
 *        open-ended support, HeroDevs' Never-Ending Support); when that starts depends on the release, see
 *        {@link Release#openEndedStart(EnterpriseOffering)}
 * @param micros the vendor's own builds of the release, newest first; empty when there are none
 */
public record EnterpriseOffering(LocalDate releaseDate, LocalDate eolDate, String parentRelease,
        boolean openEnded, List<Micro> micros) {

    public EnterpriseOffering {
        micros = micros == null ? List.of() : micros;
    }

    /** The vendor gives an end date for its support; without one there is no dated support to count or draw. */
    public boolean hasEndDate() {
        return eolDate != null;
    }

    /**
     * The vendor has a build of its own (Red Hat, IBM), unlike a vendor offering only open-ended support (HeroDevs).
     * For now this is the same as {@link #hasEndDate()}: every vendor build in the data has an end date. It could
     * become releaseDate != null || eolDate != null, so that a build whose end date isn't known yet still counts;
     * {@link Release#openEndedStart} would then need a start for open-ended support after a build with no end date.
     */
    public boolean hasRelease() {
        return hasEndDate();
    }

    /** The build is supported on d: it has an end date after d. */
    public boolean activeOn(LocalDate d) {
        return hasEndDate() && eolDate.isAfter(d);
    }

    /**
     * Same as {@link #activeOn(LocalDate)}, for templates: Roq gives them {@code now} as a LocalDateTime
     * (releases-table-band.html shows a vendor's badge with {@code release.rhbq.activeOn(now)??}).
     */
    public boolean activeOn(LocalDateTime now) {
        return activeOn(now.toLocalDate());
    }

}
