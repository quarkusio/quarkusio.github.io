package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Queries over the typed {@link Releases} data. Template-facing methods take Roq's
 * {@code now} (a LocalDateTime) as well as a LocalDate.
 */
@ApplicationScoped
@Named("releaseCatalog")
public class ReleaseCatalog {

    static final int DEFAULT_MAX_UPCOMING = 1;
    static final Period DEFAULT_CADENCE = Period.ofWeeks(4);

    private final Releases data;
    private final List<Release> releases;
    private final int maxUpcoming;

    @Inject
    public ReleaseCatalog(Releases data, ReleasesConfig config) {
        this(data, config.maxUpcoming());
    }

    public ReleaseCatalog(Releases data) {
        this(data, DEFAULT_MAX_UPCOMING);
    }

    public ReleaseCatalog(Releases data, int maxUpcoming) {
        this.data = data;
        this.maxUpcoming = maxUpcoming;
        this.releases = enrichUpcoming(data.releases().stream().map(Release::from).toList(), data.policy());
    }

    public Releases.Policy policy() {
        return data.policy();
    }

    public Releases.SupportProviders providers() {
        return data.supportProviders();
    }

    /**
     * Derives plannedEndDate for the upcoming releases that have a planned date, from all of them
     * (not only the visible ones, so hiding later releases does not change the ones that remain).
     * A non-LTS is supported only until the next minor, so it ends where the next dated upcoming release starts.
     * An LTS runs for the policy's LTS support duration, clipped to that start but never to less than
     * the release cadence. Without a next release a non-LTS has no end date, and the caller picks a default length.
     */
    private static List<Release> enrichUpcoming(List<Release> all, Releases.Policy policy) {
        Period ltsPeriod = policy == null ? null : policy.ltsSupportPeriod();
        Period cadence = cadence(policy);
        List<Release> dated = all.stream().filter(r -> r.upcoming() && r.plannedDate() != null)
                .sorted(Comparator.comparing(Release::plannedDate).thenComparing(Release::version))
                .toList();
        return all.stream().map(r -> {
            if (!dated.contains(r)) {
                return r;
            }
            LocalDate start = r.plannedDate();
            LocalDate next = dated.stream().map(Release::plannedDate).filter(d -> d.isAfter(start))
                    .findFirst().orElse(null);
            LocalDate end = next;
            if (r.lts() && ltsPeriod != null) {
                end = start.plus(ltsPeriod);
                if (next != null && next.isBefore(end)) {
                    LocalDate minEnd = start.plus(cadence);
                    end = next.isAfter(minEnd) ? next : minEnd;
                }
            }
            return r.withPlannedEndDate(end);
        }).toList();
    }

    /** The default length of an upcoming bar: the policy's release cadence. */
    public Period cadence() {
        return cadence(data.policy());
    }

    private static Period cadence(Releases.Policy policy) {
        Period p = policy == null ? null : policy.releaseCadencePeriod();
        return p == null ? DEFAULT_CADENCE : p;
    }

    /** All releases, newest first, as listed in the data file. */
    public List<Release> all() {
        return releases;
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
        return upcoming().stream().limit(maxUpcoming).toList();
    }

    /** All releases, but with only the visible upcoming ones; what the releases table lists. */
    public List<Release> listed() {
        List<Release> visible = visibleUpcoming();
        return all().stream().filter(r -> !r.upcoming() || visible.contains(r)).toList();
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

    /** Support provider URL by key: rhbq, ibm or herodevs. */
    public String providerUrl(String provider) {
        Releases.SupportProviders providers = data.supportProviders();
        if (providers == null) {
            return null;
        }
        Releases.Provider p = switch (provider) {
            case "rhbq" -> providers.rhbq();
            case "ibm" -> providers.ibm();
            case "herodevs" -> providers.herodevs();
            default -> null;
        };
        return p == null ? null : p.url();
    }
}
