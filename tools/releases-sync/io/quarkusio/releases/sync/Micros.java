package io.quarkusio.releases.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.maven.artifact.versioning.ComparableVersion;

import io.quarkusio.releases.Releases;

/**
 * Builds the micros of each minor from two sources: the GitHub releases of quarkusio/quarkus, and the platform BOMs
 * ({@code io.quarkus.platform:quarkus-bom}) on Maven Central. Neither is complete. Some X.0 core releases are on
 * GitHub but never got a platform BOM, and most candidates (X.0.CR1) are pre-releases on GitHub. Every version
 * either source has is listed; when both have it, the earlier date wins.
 * <p>
 * Pure logic, no I/O, and no Quarkus (so it can be tested with plain JUnit, see MicrosTest).
 */
public final class Micros {

    static final String BOM_URL = "https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/%1$s/quarkus-bom-%1$s.pom";

    /**
     * A GitHub release (final or pre-release).
     *
     * @param publishedAt ISO date-time, can be null
     * @param url its page on GitHub, can be null
     */
    public record GitHubTag(String version, String publishedAt, String url) {
    }

    private Micros() {
    }

    public static String bomUrl(String version) {
        return BOM_URL.formatted(version);
    }

    /**
     * @param github every GitHub release, final and pre-release
     * @param boms platform BOM version to the date it was published to Maven Central
     * @return minor version (e.g. "3.27") to its micros, newest first
     */
    public static Map<String, List<Releases.MicroEntry>> byMinor(List<GitHubTag> github, Map<String, LocalDate> boms) {
        Map<String, GitHubTag> githubByVersion = new HashMap<>();
        for (GitHubTag release : github) {
            if (minorOf(release.version()) != null) {
                githubByVersion.put(release.version(), release);
            }
        }
        TreeSet<String> versions = new TreeSet<>((a, b) -> new ComparableVersion(b).compareTo(new ComparableVersion(a)));
        versions.addAll(githubByVersion.keySet());
        boms.keySet().stream().filter(v -> minorOf(v) != null).forEach(versions::add);

        Map<String, List<Releases.MicroEntry>> result = new LinkedHashMap<>();
        for (String version : versions) {
            GitHubTag release = githubByVersion.get(version);
            LocalDate githubDate = release == null || release.publishedAt() == null ? null : gitHubDate(release.publishedAt());
            LocalDate bomDate = boms.get(version);
            boolean hasBom = boms.containsKey(version);
            result.computeIfAbsent(minorOf(version), k -> new ArrayList<>())
                    .add(Releases.MicroEntry.of(version, string(earlier(githubDate, bomDate)),
                            release == null ? null : release.url(), hasBom ? bomUrl(version) : null));
        }
        return result;
    }

    /**
     * Platform BOM versions as already recorded in the data, for when Maven Central can't be read: each micro with a
     * BOM URL, and its date. That date is already the earlier of the two sources, so merging it again changes nothing.
     */
    public static Map<String, LocalDate> bomsFromExisting(Releases data) {
        Map<String, LocalDate> boms = new HashMap<>();
        if (data.releases() == null) {
            return boms;
        }
        for (Releases.Entry entry : data.releases()) {
            if (entry.micros() == null) {
                continue;
            }
            for (Releases.MicroEntry micro : entry.micros()) {
                if (micro.bomUrl() != null) {
                    boms.put(micro.version(), micro.releaseDate() == null ? null : LocalDate.parse(micro.releaseDate()));
                }
            }
        }
        return boms;
    }

    private static String minorOf(String version) {
        return version == null ? null : EnterpriseReleaseDates.minorOf(version);
    }

    static LocalDate gitHubDate(String isoDate) {
        return Instant.parse(isoDate).atOffset(ZoneOffset.UTC).toLocalDate();
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isBefore(b) ? a : b;
    }

    private static String string(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
