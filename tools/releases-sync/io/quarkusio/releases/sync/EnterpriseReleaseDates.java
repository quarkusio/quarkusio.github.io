package io.quarkusio.releases.sync;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure helpers for working out when each Quarkus minor first shipped in the Red Hat and IBM enterprise
 * builds from their Maven repositories (see {@link MavenRepositories} for fetching). There is no official
 * feed of these dates, so a minor's release date is the publish date of the earliest micro build of its
 * quarkus-bom (the lowest micro number, e.g. 3.27.0). These are publication dates, not announced GA dates.
 */
public class EnterpriseReleaseDates {

    private static final Pattern VERSION = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern METADATA_VERSION = Pattern.compile("<version>([^<]+)</version>");
    // Apache-style listing row: <a href="3.27.0.redhat-00001/">...</a>   2025-11-26 12:17
    private static final Pattern LISTING_ROW = Pattern.compile(
            "<a href=\"(\\d[^\"/]*)/\">[^\\n]*?(\\d{4}-\\d{2}-\\d{2})\\s+\\d{2}:\\d{2}");

    /** Versions listed in a maven-metadata.xml. */
    public static List<String> parseMetadata(String xml) {
        List<String> versions = new ArrayList<>();
        Matcher m = METADATA_VERSION.matcher(xml);
        while (m.find()) {
            versions.add(m.group(1));
        }
        return versions;
    }

    /** Version directory name to its modification date, from an Apache-style directory listing. */
    public static Map<String, LocalDate> parseListing(String html) {
        Map<String, LocalDate> dates = new LinkedHashMap<>();
        Matcher m = LISTING_ROW.matcher(html);
        while (m.find()) {
            dates.put(m.group(1), LocalDate.parse(m.group(2)));
        }
        return dates;
    }

    /** "3.27.1.SP1-redhat-00001" is in minor "3.27", or null if it isn't a version. */
    public static String minorOf(String version) {
        Matcher m = VERSION.matcher(version);
        return m.find() ? m.group(1) + "." + m.group(2) : null;
    }

    /** For each minor, the versions with the lowest micro number (e.g. every build of 3.27.0). */
    public static Map<String, List<String>> earliestPerMinor(Collection<String> versions) {
        Map<String, List<String>> result = new TreeMap<>(Comparator.comparing(EnterpriseReleaseDates::minorKey));
        Map<String, Integer> lowestMicro = new LinkedHashMap<>();
        for (String version : versions) {
            String minor = minorOf(version);
            if (minor != null) {
                lowestMicro.merge(minor, micro(version), Math::min);
            }
        }
        for (String version : versions) {
            String minor = minorOf(version);
            if (minor != null && micro(version) == lowestMicro.get(minor)) {
                result.computeIfAbsent(minor, k -> new ArrayList<>()).add(version);
            }
        }
        return result;
    }

    private static int micro(String version) {
        Matcher m = VERSION.matcher(version);
        m.find();
        return Integer.parseInt(m.group(3));
    }

    private static String minorKey(String minor) {
        String[] parts = minor.split("\\.");
        return String.format("%05d.%05d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    /**
     * An enterprise build can't ship before the community release it is built from, so a provider
     * release_date earlier than the community one means the data (or a scraped date) is wrong.
     * Same day is fine. Returns the problem, or empty if the dates are consistent or either is unknown.
     */
    public static Optional<String> checkNotBeforeCommunity(String provider, String version,
                                                           LocalDate community, LocalDate enterprise) {
        if (community == null || enterprise == null || !enterprise.isBefore(community)) {
            return Optional.empty();
        }
        return Optional.of(provider + " release_date " + enterprise + " for " + version
                + " is before the community release date " + community);
    }

    /** Minor to the earliest date among the lowest-micro versions of that minor. */
    public static Map<String, LocalDate> earliestDates(Map<String, LocalDate> versionDates) {
        Map<String, LocalDate> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : earliestPerMinor(versionDates.keySet()).entrySet()) {
            e.getValue().stream().map(versionDates::get).min(Comparator.naturalOrder())
                    .ifPresent(date -> result.put(e.getKey(), date));
        }
        return result;
    }

    /**
     * A vendor's build of a minor: when it was released, and whether it is the vendor's last minor of its
     * major stream (3.x, 2.x), which the vendor supports for longer than the others.
     */
    public record Build(LocalDate releaseDate, boolean lastInMajor) {
    }

    /**
     * The minors to mark as last in their major stream: for each major the vendor has since moved past (it
     * has released a newer major) and where no minor is marked yet, the newest minor. A stream that already
     * has a marked minor is left alone, so a hand-edited mark is respected.
     */
    public static Set<String> newlyLastInMajor(Map<String, Build> builds) {
        int newestMajor = builds.keySet().stream().mapToInt(EnterpriseReleaseDates::major).max().orElse(0);
        Set<String> result = new LinkedHashSet<>();
        for (int major : builds.keySet().stream().mapToInt(EnterpriseReleaseDates::major).distinct().toArray()) {
            if (major >= newestMajor || stream(builds, major).anyMatch(v -> builds.get(v).lastInMajor())) {
                continue;
            }
            stream(builds, major).reduce((a, b) -> b).ifPresent(result::add);
        }
        return result;
    }

    /**
     * The end of a vendor's support for a minor, by the rules of the Red Hat and IBM support policies. There is
     * one end date; full support and maintenance aren't distinguished.
     * <ul>
     * <li>Red Hat: the latest minor is fully supported, and the previous one goes into maintenance for six months
     * from the release of its successor. So a minor that is not the last of its major stream ends six months after
     * the next minor of the stream is released. Until there is a next minor, one year after the minor's own
     * release is a placeholder (the usual gap between minors is six months).</li>
     * <li>IBM: a minor that is not the last of its major stream ends one year after its own release.</li>
     * <li>The last minor of a stream is supported for a period counted from the vendor's first release in that
     * stream, not from its own release: 3 years for Red Hat, 5 years for IBM.</li>
     * <li>Red Hat's full support starts with a new major and ends when the next major is released; the previous
     * major then stays in maintenance for at least six months. So once the vendor has released the next major, the
     * last minor's end is no earlier than six months after that release. (This is applied to IBM too.)</li>
     * </ul>
     * Which minor is last is the {@link Build#lastInMajor} flag; see {@link #newlyLastInMajor}. Before the vendor
     * has released the next major its date can't be known, so Red Hat's last minor has no computed date and
     * keeps a hand-edited provisional one.
     *
     * @param provider "rhbq" or "ibm"
     * @param version a minor, e.g. "3.27"
     * @param builds the vendor's builds by minor
     * @return the end date, or empty if it can't be computed (unknown version, or Red Hat's last minor before
     *         the next major)
     */
    public static Optional<LocalDate> computeEolDate(String provider, String version, Map<String, Build> builds) {
        Build build = builds.get(version);
        if (build == null) {
            return Optional.empty();
        }
        if (!build.lastInMajor()) {
            if (provider.equals("rhbq")) {
                Optional<LocalDate> next = nextMinorRelease(builds, version);
                if (next.isPresent()) {
                    return next.map(date -> date.plusMonths(6));
                }
            }
            return Optional.of(build.releaseDate().plusYears(1));
        }
        Optional<LocalDate> nextMajor = nextMajorStart(builds, major(version));
        if (provider.equals("rhbq") && nextMajor.isEmpty()) {
            return Optional.empty();
        }
        LocalDate first = stream(builds, major(version)).map(v -> builds.get(v).releaseDate())
                .min(Comparator.naturalOrder()).get();
        LocalDate eol = first.plusYears(provider.equals("ibm") ? 5 : 3);
        LocalDate minimum = nextMajor.map(start -> start.plusMonths(6)).orElse(eol);
        return Optional.of(eol.isBefore(minimum) ? minimum : eol);
    }

    /**
     * The end date to record for a minor: what {@link #computeEolDate} says, reconciled with the date already
     * recorded, if any.
     * <ul>
     * <li>No date recorded: the computed one.</li>
     * <li>Nothing computed (Red Hat's last minor before the next major): the recorded one, a hand-edited
     * placeholder.</li>
     * <li>A Red Hat minor that has a successor in its stream: the computed date replaces the recorded one, since
     * the successor's release fixes it.</li>
     * <li>A last minor whose next major has been released: the computed date, but never earlier than the one
     * recorded. Red Hat publishes the end of a stream (and has extended it before), so the date it published must
     * not be shortened by a computation; it can only be lengthened if the next major's release pushes the six months
     * beyond it.</li>
     * <li>Anything else: the recorded date, which is kept.</li>
     * </ul>
     *
     * @param existing the date already recorded, or null
     */
    public static Optional<LocalDate> eolDateToRecord(String provider, String version, Map<String, Build> builds,
                                                      LocalDate existing) {
        Optional<LocalDate> computed = computeEolDate(provider, version, builds);
        if (existing == null || computed.isEmpty()) {
            return computed.or(() -> Optional.ofNullable(existing));
        }
        Build build = builds.get(version);
        if (build.lastInMajor() && nextMajorStart(builds, major(version)).isPresent()) {
            return Optional.of(computed.get().isBefore(existing) ? existing : computed.get());
        }
        if (!build.lastInMajor() && provider.equals("rhbq") && nextMinorRelease(builds, version).isPresent()) {
            return computed;
        }
        return Optional.of(existing);
    }

    /** The vendor's release date of the next minor in the same major, if it has one. */
    private static Optional<LocalDate> nextMinorRelease(Map<String, Build> builds, String version) {
        return stream(builds, major(version))
                .filter(v -> minorKey(v).compareTo(minorKey(version)) > 0)
                .findFirst().map(v -> builds.get(v).releaseDate());
    }

    /** The vendor's first release in the next major after this one, if it has one: when this major's full support ends. */
    private static Optional<LocalDate> nextMajorStart(Map<String, Build> builds, int major) {
        return builds.keySet().stream().mapToInt(EnterpriseReleaseDates::major).filter(m -> m > major).min()
                .stream().boxed().findFirst()
                .flatMap(next -> stream(builds, next).map(v -> builds.get(v).releaseDate())
                        .min(Comparator.naturalOrder()));
    }

    /** Describes how an existing eol_date differs from the computed one, or empty if it matches or can't be checked. */
    public static Optional<String> checkEolDate(String provider, String version, Map<String, Build> builds,
                                                LocalDate actual) {
        Optional<LocalDate> expected = computeEolDate(provider, version, builds);
        if (actual == null || expected.isEmpty() || expected.get().equals(actual)) {
            return Optional.empty();
        }
        // A last minor may run longer than the minimum its next major's release sets: it is only a floor
        if (builds.get(version).lastInMajor() && nextMajorStart(builds, major(version)).isPresent()
                && actual.isAfter(expected.get())) {
            return Optional.empty();
        }
        return Optional.of(provider + " eol_date " + actual + " for " + version + " should be "
                + expected.get() + " by the support policy");
    }

    /** The minors of a major, oldest first. */
    private static java.util.stream.Stream<String> stream(Map<String, Build> builds, int major) {
        return builds.keySet().stream().filter(v -> major(v) == major)
                .sorted(Comparator.comparing(EnterpriseReleaseDates::minorKey));
    }

    private static int major(String minor) {
        return Integer.parseInt(minor.substring(0, minor.indexOf('.')));
    }
}
