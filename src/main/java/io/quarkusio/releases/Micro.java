package io.quarkusio.releases;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * One micro release of a minor, e.g. 3.27.6.1 of 3.27, or a pre-release such as 3.28.0.CR1. The release date is null
 * when unknown. githubUrl is its GitHub release, bomUrl its platform BOM on Maven Central; either can be null.
 * candidate, beta and alpha come from the data (the releases sync sets them from the version, see
 * {@link #isCandidate(String)}); at most one is true, and none for a final release. parentRelease is the version of
 * the micro it was built from (see {@link Releases.MicroEntry}); null when unknown. Vendor builds are Micros too,
 * listed in their {@link EnterpriseOffering#micros()}.
 */
public record Micro(String version, LocalDate releaseDate, String githubUrl, String bomUrl, boolean candidate,
        boolean beta, boolean alpha, String parentRelease) {

    private static final Pattern CANDIDATE = Pattern.compile("(?i)[.-](cr|rc)\\d*$");
    private static final Pattern BETA = Pattern.compile("(?i)[.-]beta\\d*$");
    private static final Pattern ALPHA = Pattern.compile("(?i)[.-]alpha\\d*$");

    /** A candidate, beta or alpha, rather than a final release. */
    public boolean preRelease() {
        return candidate || beta || alpha;
    }

    /** A final release: not a candidate, beta or alpha. */
    public boolean isFinal() {
        return !preRelease();
    }

    /** A release candidate, e.g. 3.28.0.CR1 (Quarkus uses CR; RC is accepted too). */
    public static boolean isCandidate(String version) {
        return version != null && CANDIDATE.matcher(version).find();
    }

    /** E.g. 4.0.0.Beta1. */
    public static boolean isBeta(String version) {
        return version != null && BETA.matcher(version).find();
    }

    /** E.g. 3.0.0.Alpha3. */
    public static boolean isAlpha(String version) {
        return version != null && ALPHA.matcher(version).find();
    }

    /** Judged by the version alone, as GitHub marks some pre-releases (e.g. 3.5.0.CR1) as final. */
    public static boolean isPreRelease(String version) {
        return isCandidate(version) || isBeta(version) || isAlpha(version);
    }
}
