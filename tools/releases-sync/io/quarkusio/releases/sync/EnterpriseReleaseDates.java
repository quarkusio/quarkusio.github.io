package io.quarkusio.releases.sync;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
