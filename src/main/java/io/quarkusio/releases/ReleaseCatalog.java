package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final Releases data;
    private final List<Release> releases;

    @Inject
    public ReleaseCatalog(Releases data) {
        this.data = data;
        this.releases = data.releases().stream().map(Release::from).toList();
    }

    public Releases.Policy policy() {
        return data.policy();
    }

    public Releases.SupportProviders providers() {
        return data.supportProviders();
    }

    /** All releases, newest first, as listed in the data file. */
    public List<Release> all() {
        return releases;
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
