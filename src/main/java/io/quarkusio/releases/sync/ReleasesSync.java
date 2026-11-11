package io.quarkusio.releases.sync;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.logging.Log;
import io.quarkusio.releases.Releases;
import io.quarkusio.releases.sync.GitHubReleases.GitHubMilestone;
import io.quarkusio.releases.sync.GitHubReleases.GitHubRelease;

/**
 * Computes the updated {@link Releases} data from GitHub releases and the release planning wiki.
 * <p>
 * Pure logic, no I/O. Field ownership (which fields are automated and which are hand-edited) is described in
 * {@code README.md} in this package.
 */
public final class ReleasesSync {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
    private static final Pattern MILESTONE_VERSION = Pattern.compile("^(\\d+\\.\\d+)(?:\\s|$|\\s*-|\\s*\\.|$)");
    private static final Pattern PRE_RELEASE_VERSION = Pattern.compile("^(\\d+\\.\\d+)\\.\\d+\\.(CR|Alpha|Beta|RC)");

    /**
     * @param source "wiki", "milestone", "prerelease" or "cadence"
     * @param dueDate ISO date (or date-time), can be null
     */
    public record UpcomingRelease(String version, String source, String dueDate, boolean lts, boolean proposal) {

        UpcomingRelease(String version, String source, String dueDate) {
            this(version, source, dueDate, false, false);
        }
    }

    private ReleasesSync() {
    }

    // ---- Upcoming releases ----

    /**
     * Upcoming releases from the release planning wiki, which is authoritative for upcoming releases.
     */
    public static List<UpcomingRelease> wikiUpcoming(String page, List<GitHubRelease> githubReleases, LocalDate today) {
        Set<String> published = githubReleases.stream().map(r -> minorVersion(r.tagName())).collect(Collectors.toSet());
        Consumer<String> onUnparsable = version -> {
            // Old releases have no Platform date; only a release that isn't published yet matters
            if (!published.contains(version)) {
                Log.warnf("Release planning wiki lists %s but no Platform release date could be parsed; "
                        + "keeping any existing entry for it", version);
            }
        };
        return WikiReleasePlanning.upcoming(WikiReleasePlanning.parse(page, onUnparsable), today).stream()
                .map(r -> new UpcomingRelease(r.version(), "wiki", r.platformDate().toString(), r.lts(), r.proposal()))
                .toList();
    }

    /**
     * Fallback predictions, used when neither the wiki nor the data has upcoming releases.
     */
    public static List<UpcomingRelease> predictUpcoming(List<GitHubRelease> releases, List<GitHubRelease> preReleases,
            List<GitHubMilestone> milestones) {
        Set<String> existingVersions = releases.stream().map(r -> minorVersion(r.tagName())).collect(Collectors.toSet());
        List<UpcomingRelease> upcoming = new ArrayList<>();
        for (GitHubMilestone milestone : milestones) {
            String version = match(MILESTONE_VERSION, milestone.title());
            if (version != null && !existingVersions.contains(version)) {
                upcoming.add(new UpcomingRelease(version, "milestone", milestone.dueOn()));
            }
        }
        for (GitHubRelease preRelease : preReleases) {
            String version = match(PRE_RELEASE_VERSION, preRelease.tagName());
            if (version != null && !existingVersions.contains(version)) {
                upcoming.add(new UpcomingRelease(version, "prerelease", null));
            }
        }
        // next minor, based on cadence
        releases.stream().map(r -> minorVersion(r.tagName())).filter(ReleasesSync::isValidVersion)
                .max(ReleasesSync::compareVersions)
                .map(latest -> {
                    String[] parts = latest.split("\\.");
                    return parts[0] + "." + (Integer.parseInt(parts[1]) + 1);
                })
                .filter(next -> !existingVersions.contains(next))
                .ifPresent(next -> upcoming.add(new UpcomingRelease(next, "cadence", null)));

        // distinct by version, first source wins
        Map<String, UpcomingRelease> distinct = new LinkedHashMap<>();
        upcoming.forEach(u -> distinct.putIfAbsent(u.version(), u));
        return distinct.values().stream().sorted((a, b) -> compareVersions(b.version(), a.version())).toList();
    }

    // ---- Merge ----

    public static Releases sync(Releases existing, List<GitHubRelease> githubReleases, List<UpcomingRelease> predicted,
            List<UpcomingRelease> wikiUpcoming) {
        Map<String, List<GitHubRelease>> releasesByMinor = githubReleases.stream()
                .filter(r -> r.tagName() != null && isValidVersion(r.tagName()))
                .collect(Collectors.groupingBy(r -> minorVersion(r.tagName())));

        List<WorkingRelease> existingReleases = existing.releases() == null ? List.of()
                : existing.releases().stream().map(WorkingRelease::from).toList();
        Map<String, WorkingRelease> existingByVersion = existingReleases.stream()
                .collect(Collectors.toMap(r -> r.version, r -> r, (a, b) -> a));

        List<WorkingRelease> processed = new ArrayList<>();
        for (Map.Entry<String, List<GitHubRelease>> entry : releasesByMinor.entrySet()) {
            List<GitHubRelease> minorReleases = new ArrayList<>(entry.getValue());
            minorReleases.sort((a, b) -> compareVersions(b.tagName(), a.tagName()));
            processed.add(WorkingRelease.published(entry.getKey(), minorReleases.get(minorReleases.size() - 1),
                    minorReleases.get(0), existingByVersion.get(entry.getKey())));
        }

        // Upcoming releases from the planning wiki replace the existing entry for the same version
        List<UpcomingRelease> wikiPending = wikiUpcoming.stream()
                .filter(u -> !releasesByMinor.containsKey(u.version()))
                .toList();
        Set<String> wikiVersions = wikiPending.stream().map(UpcomingRelease::version).collect(Collectors.toSet());

        // Other existing upcoming releases are kept as manually curated (e.g. the wiki date couldn't be parsed)
        Set<String> manualUpcomingVersions = new HashSet<>();
        for (WorkingRelease release : existingReleases) {
            // Compare by minor version so a hand-edited "4.0.0" is covered by the wiki's "4.0"
            String minor = minorVersion(release.version);
            if (!Boolean.TRUE.equals(release.upcoming) || wikiVersions.contains(minor)) {
                continue;
            }
            if (releasesByMinor.containsKey(minor)) {
                Log.infof("Upcoming release %s is now published", release.version);
            } else {
                processed.add(release);
                manualUpcomingVersions.add(release.version);
                Log.infof("Preserving manually curated upcoming release: %s", release.version);
            }
        }

        for (UpcomingRelease wiki : wikiPending) {
            processed.add(WorkingRelease.upcoming(wiki));
            Log.infof("Upcoming release from wiki: %s (%s)", wiki.version(), wiki.dueDate());
        }

        // Nothing upcoming in the wiki or curated by hand: fall back to predicting the next release
        if (wikiPending.isEmpty() && manualUpcomingVersions.isEmpty()) {
            String latestReleased = processed.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.upcoming))
                    .map(r -> r.version)
                    .max(ReleasesSync::compareVersions)
                    .orElse(null);
            predicted.stream()
                    .filter(u -> latestReleased == null || compareVersions(u.version(), latestReleased) > 0)
                    .filter(u -> !releasesByMinor.containsKey(u.version()))
                    .min((a, b) -> compareVersions(a.version(), b.version()))
                    .ifPresent(next -> {
                        processed.add(WorkingRelease.upcoming(next));
                        Log.infof("Predicted upcoming release: %s (source: %s)", next.version(), next.source());
                    });
        }

        processed.sort((a, b) -> compareVersions(b.version, a.version));

        // Non-LTS releases are supported "until the next minor release":
        // when not set explicitly, their EOL date is the release date of the next minor.
        for (int i = 1; i < processed.size(); i++) {
            WorkingRelease release = processed.get(i);
            if (release.hasExplicitEolDate || Boolean.TRUE.equals(release.upcoming) || Boolean.TRUE.equals(release.lts)) {
                continue;
            }
            WorkingRelease next = processed.get(i - 1);
            LocalDate nextDate = next.releaseDate != null ? next.releaseDate : next.plannedDate;
            if (nextDate != null) {
                release.eolDate = nextDate;
            }
        }

        return new Releases(existing.policy(), existing.supportProviders(), existing.eolSupport(),
                processed.stream().map(WorkingRelease::toEntry).toList());
    }

    // ---- Versions ----

    static boolean isValidVersion(String version) {
        return VERSION_PATTERN.matcher(version).find();
    }

    static String minorVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.find() ? matcher.group(1) + "." + matcher.group(2) : version;
    }

    static int compareVersions(String v1, String v2) {
        return VERSION_ORDER.compare(v1, v2);
    }

    private static final Comparator<String> VERSION_ORDER = (v1, v2) -> {
        String[] parts1 = numericPart(v1).split("\\.");
        String[] parts2 = numericPart(v2).split("\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return Integer.compare(p1, p2);
            }
        }
        return 0;
    };

    private static String numericPart(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        return matcher.find() ? version.substring(matcher.start(), matcher.end()) : version;
    }

    private static String match(Pattern pattern, String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * A mutable release, while it's being synced.
     */
    private static final class WorkingRelease {
        String version;
        LocalDate releaseDate;
        LocalDate plannedDate;
        LocalDate eolDate;
        /**
         * Only set eol dates count as explicit: the bean can't tell an empty {@code eol_date:} from a missing one.
         */
        boolean hasExplicitEolDate;
        Boolean lts;
        Boolean upcoming;
        Boolean proposal;
        String latestPatch;
        LocalDate latestPatchDate;
        String link;
        Releases.ProviderDates rhbq;
        Releases.ProviderDates ibm;
        Releases.ProviderDates herodevs;

        static WorkingRelease from(Releases.Entry entry) {
            WorkingRelease release = new WorkingRelease();
            release.version = entry.version();
            release.releaseDate = date(entry.releaseDate());
            release.plannedDate = date(entry.plannedDate());
            release.eolDate = date(entry.eolDate());
            release.hasExplicitEolDate = entry.eolDate() != null;
            release.lts = entry.lts();
            release.upcoming = entry.upcoming();
            release.proposal = entry.proposal();
            release.latestPatch = entry.latestPatch();
            release.latestPatchDate = date(entry.latestPatchDate());
            release.link = entry.link();
            release.rhbq = entry.rhbq();
            release.ibm = entry.ibm();
            release.herodevs = entry.herodevs();
            return release;
        }

        static WorkingRelease published(String minorVersion, GitHubRelease earliest, GitHubRelease latest,
                WorkingRelease existing) {
            WorkingRelease release = new WorkingRelease();
            release.version = minorVersion;
            release.releaseDate = gitHubDate(earliest.publishedAt());
            release.latestPatch = latest.tagName();
            release.latestPatchDate = gitHubDate(latest.publishedAt());
            // Preserve manual fields from the existing release
            if (existing != null) {
                release.lts = existing.lts;
                release.eolDate = existing.eolDate;
                release.hasExplicitEolDate = existing.hasExplicitEolDate;
                release.link = existing.link;
                release.rhbq = existing.rhbq;
                release.ibm = existing.ibm;
                release.herodevs = existing.herodevs;
            } else {
                release.lts = false;
            }
            return release;
        }

        static WorkingRelease upcoming(UpcomingRelease upcoming) {
            WorkingRelease release = new WorkingRelease();
            release.version = upcoming.version();
            release.lts = upcoming.lts();
            release.upcoming = true;
            release.proposal = upcoming.proposal() ? true : null;
            if (upcoming.dueDate() != null) {
                try {
                    release.plannedDate = LocalDate.parse(upcoming.dueDate().split("T")[0]);
                } catch (RuntimeException e) {
                    Log.debugf("Failed to parse due date for upcoming release %s: %s", upcoming.version(), e.getMessage());
                }
            }
            return release;
        }

        Releases.Entry toEntry() {
            boolean isUpcoming = Boolean.TRUE.equals(upcoming);
            return new Releases.Entry(version, string(releaseDate), string(plannedDate), string(eolDate),
                    Boolean.TRUE.equals(lts), isUpcoming, isUpcoming && Boolean.TRUE.equals(proposal),
                    latestPatch, string(latestPatchDate), link, rhbq, ibm, herodevs);
        }

        private static LocalDate date(String value) {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        }

        private static String string(LocalDate date) {
            return date == null ? null : date.toString();
        }

        private static LocalDate gitHubDate(String isoDate) {
            return Instant.parse(isoDate).atOffset(ZoneOffset.UTC).toLocalDate();
        }
    }
}
