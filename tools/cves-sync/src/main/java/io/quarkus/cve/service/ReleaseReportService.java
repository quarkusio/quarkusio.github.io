package io.quarkus.cve.service;

import io.quarkus.cve.model.ReleaseReport;
import io.quarkus.cve.model.ReleaseReport.Change;
import io.quarkus.cve.model.ReleaseReport.Finding;
import io.quarkus.cve.model.ReleaseReport.Release;
import io.quarkus.cve.model.ReleaseReport.ReleaseGroup;
import io.quarkus.cve.model.ReleaseReport.Split;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.util.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Builds the release-centric report. The Quarkus platform BOM of each release says which version of
 * every artifact (core, extensions and dependencies) that release ships; an advisory is active in a
 * release when one of its affected artifacts is managed at a version inside an affected range.
 * Comparing consecutive releases of a major.minor line shows what each release fixed or introduced,
 * and the artifact whose version changed explains why.
 *
 * Advisories are today's knowledge applied to every release: an advisory published last month is
 * shown as active in a release shipped years ago if that release carries an affected version.
 */
@ApplicationScoped
public class ReleaseReportService {

    private static final int MAX_CAUSE_GROUPS = 3;

    @Inject
    QuarkusBomService bomService;

    @Inject
    QuarkusArtifactService artifactService;

    @Inject
    GitHubReleaseSyncService releaseSyncService;

    /** One affected artifact of an advisory as managed by one release. */
    private record Hit(String artifact, String version, String fixed) {}

    /**
     * @param fromMinor first major.minor line to include, e.g. "3.27"
     * @param toMinor   last major.minor line to include, or null for everything newer
     * @throws IllegalArgumentException if a minor is not of the form major.minor
     */
    public ReleaseReport build(String fromMinor, String toMinor) {
        return build(fromMinor, toMinor, new java.io.File("_data/releases.yaml"), "quarkusio", "quarkus", true);
    }

    public ReleaseReport build(String fromMinor, String toMinor, java.io.File releasesInput, String releasesOrg, String releasesRepo, boolean predictUpcoming) {
        int[] from = parseMinor(fromMinor);
        int[] to = toMinor == null ? null : parseMinor(toMinor);

        ReleaseReport report = new ReleaseReport();
        report.from = fromMinor;
        report.to = toMinor;

        GitHubReleaseSyncService.ReleaseSyncResult syncResult =
                releaseSyncService.sync(releasesInput, releasesOrg, releasesRepo, predictUpcoming);
        report.policy = syncResult.policy;
        report.supportProviders = syncResult.supportProviders;
        report.eolSupport = syncResult.eolSupport;

        // 1. Releases in range, grouped by major.minor, oldest first.
        Map<String, List<String>> byMinor = new LinkedHashMap<>();
        for (String release : bomService.sortedReleases()) {
            int[] m = minorOf(release);
            if (m == null || compare(m, from) < 0 || (to != null && compare(m, to) > 0)) continue;
            byMinor.computeIfAbsent(m[0] + "." + m[1], k -> new ArrayList<>()).add(release);
        }
        if (byMinor.isEmpty()) return report;

        // 2. What each release manages (cached; first run downloads the BOMs).
        Map<String, Map<String, String>> managed = new HashMap<>();
        int total = 0;
        for (List<String> releases : byMinor.values()) total += releases.size();
        final int totalReleases = total;
        boolean[] announced = {false};
        int unreadable = 0;
        Log.log(1, "Reading %d Quarkus platform BOMs for %s", total, byMinor.keySet());
        for (List<String> releases : byMinor.values()) {
            for (String release : releases) {
                Map<String, String> m = bomService.managedVersions(release, () -> {
                    if (!announced[0]) {
                        announced[0] = true;
                        System.err.printf("Downloading Quarkus platform BOMs not yet cached "
                                + "(one-time, up to %d releases)...%n", totalReleases);
                    }
                });
                if (m == null) unreadable++;
                else managed.put(release, m);
            }
        }
        if (unreadable > 0) {
            Log.warn("could not read %d of %d Quarkus BOM releases; they are left out of the report", unreadable, total);
        }

        // 3. Every advisory for every artifact any of these releases manages. OSV for all of them;
        //    GitHub (one request per package) only for the Quarkus artifacts.
        TreeSet<String> universe = new TreeSet<>();
        managed.values().forEach(m -> universe.addAll(m.keySet()));
        List<String> quarkusPackages = universe.stream().filter(QuarkusBomService::isQuarkusArtifact).toList();
        Log.log(1, "Scanning %d managed artifacts (%d Quarkus) for advisories", universe.size(), quarkusPackages.size());
        List<UnifiedVuln> all = artifactService.queryAll(new ArrayList<>(universe), quarkusPackages);

        List<UnifiedVuln> evaluable = new ArrayList<>();
        for (UnifiedVuln v : all) {
            if (v.withdrawn != null) continue;
            if (QuarkusBomService.mavenPackages(v).isEmpty()) report.statistics.advisoriesNotEvaluable++;
            else evaluable.add(v);
        }
        report.statistics.advisoriesEvaluated = evaluable.size();
        Log.log(1, "%d advisories evaluated, %d without usable version ranges", evaluable.size(), report.statistics.advisoriesNotEvaluable);

        // 4. Active set per release, and the delta against the previous release of the same line.
        Map<String, ComparableVersion> parsed = new HashMap<>();
        Map<String, String> dates = bomService.releaseDates();
        for (Map.Entry<String, List<String>> line : byMinor.entrySet()) {
            ReleaseGroup group = new ReleaseGroup();
            group.releaseGroup = line.getKey();
            Map<UnifiedVuln, Map<String, Hit>> prevActive = null;
            String prevRelease = null;
            for (String release : line.getValue()) {
                Map<String, String> current = managed.get(release);
                if (current == null) continue;

                Map<UnifiedVuln, Map<String, Hit>> active = activeIn(current, evaluable, parsed);
                Release r = new Release();
                r.version = release;
                r.date = dates.get(release);
                r.first = prevActive == null;
                for (Map.Entry<UnifiedVuln, Map<String, Hit>> e : active.entrySet()) {
                    r.cves.active.add(isCore(e.getValue()), finding(e.getKey(), e.getValue()));
                }
                if (prevActive != null) {
                    Map<String, String> previous = managed.get(prevRelease);
                    for (Map.Entry<UnifiedVuln, Map<String, Hit>> e : active.entrySet()) {
                        if (prevActive.containsKey(e.getKey())) continue;
                        Change c = new Change();
                        c.finding = finding(e.getKey(), e.getValue());
                        c.cause = cause(e.getValue(), previous, current, false);
                        r.cves.introduced.add(isCore(e.getValue()), c);
                    }
                    for (Map.Entry<UnifiedVuln, Map<String, Hit>> e : prevActive.entrySet()) {
                        if (active.containsKey(e.getKey())) continue;
                        Change c = new Change();
                        c.finding = finding(e.getKey(), e.getValue());
                        c.cause = cause(e.getValue(), previous, current, true);
                        r.cves.resolved.add(isCore(e.getValue()), c);
                    }
                }
                sort(r.cves.active);
                sortChanges(r.cves.introduced);
                sortChanges(r.cves.resolved);
                group.releases.add(r);
                prevActive = active;
                prevRelease = release;
            }
            if (!group.releases.isEmpty()) report.releaseGroups.add(group);
        }

        // Determine for each active finding which future Quarkus release fixed it (no longer applies)
        resolveFixedVersions(report.releaseGroups);

        // The deltas above need oldest-first order; the report is presented newest first.
        for (ReleaseGroup g : report.releaseGroups) {
            Collections.reverse(g.releases);
            g.releases.get(0).mostRecent = true;
        }
        Collections.reverse(report.releaseGroups);

        // Merge release metadata from syncResult into releaseGroups
        for (ReleaseGroup g : report.releaseGroups) {
            GitHubReleaseSyncService.MinorReleaseInfo info = syncResult.releasesByMinor.get(g.releaseGroup);
            if (info != null) {
                g.version = info.version != null ? info.version : g.releaseGroup;
                g.releaseDate = info.releaseDate != null ? info.releaseDate.toString() : null;
                g.eolDate = info.eolDate != null ? info.eolDate.toString() : null;
                g.lts = info.lts;
                g.upcoming = info.upcoming;
                g.latestPatch = info.latestPatch;
                g.latestPatchDate = info.latestPatchDate != null ? info.latestPatchDate.toString() : null;
                g.link = info.link;
                g.rhbqEol = info.rhbqEol != null ? info.rhbqEol.toString() : null;
                g.ibmEol = info.ibmEol != null ? info.ibmEol.toString() : null;
                g.herodevsEol = info.herodevsEol != null ? (info.herodevsEol instanceof java.time.LocalDate ld ? ld.toString() : info.herodevsEol) : null;
            } else {
                g.version = g.releaseGroup;
            }
            for (Release r : g.releases) {
                r.releaseDate = r.date != null ? (r.date.contains(" ") ? r.date.split(" ")[0] : r.date) : null;
                if (r.mostRecent && g.link != null && r.link == null) {
                    r.link = g.link;
                }
            }
        }

        // Add any upcoming releases from syncResult not already in releaseGroups
        for (GitHubReleaseSyncService.MinorReleaseInfo up : syncResult.releases) {
            if (Boolean.TRUE.equals(up.upcoming)) {
                boolean alreadyPresent = report.releaseGroups.stream()
                        .anyMatch(rg -> rg.releaseGroup.equals(up.version) || (rg.version != null && rg.version.equals(up.version)));
                if (!alreadyPresent) {
                    ReleaseGroup upcomingGroup = new ReleaseGroup();
                    upcomingGroup.releaseGroup = up.version;
                    upcomingGroup.version = up.version;
                    upcomingGroup.upcoming = true;
                    upcomingGroup.lts = Boolean.TRUE.equals(up.lts);
                    upcomingGroup.releaseDate = up.releaseDate != null ? up.releaseDate.toString() : null;
                    upcomingGroup.link = up.link;
                    report.releaseGroups.add(0, upcomingGroup);
                }
            }
        }

        return report;
    }

    // -----------------------------------------------------------------------
    // Matching
    // -----------------------------------------------------------------------

    private static Map<UnifiedVuln, Map<String, Hit>> activeIn(
            Map<String, String> managed, List<UnifiedVuln> vulns, Map<String, ComparableVersion> parsed) {
        Map<UnifiedVuln, Map<String, Hit>> active = new LinkedHashMap<>();
        for (UnifiedVuln v : vulns) {
            Map<String, Hit> hits = null;
            for (UnifiedVuln.AffectedPkg p : QuarkusBomService.mavenPackages(v)) {
                String version = managed.get(p.name);
                if (version == null) continue;
                UnifiedVuln.Interval iv = QuarkusBomService.matching(version, p, parsed);
                if (iv == null) continue;
                if (hits == null) hits = new LinkedHashMap<>();
                hits.putIfAbsent(p.name, new Hit(p.name, version, iv.fixed));
            }
            if (hits != null) active.put(v, hits);
        }
        return active;
    }

    /** True when any artifact that puts the release in range is a Quarkus core / extension artifact. */
    private static boolean isCore(Map<String, Hit> hits) {
        return hits.keySet().stream().anyMatch(QuarkusBomService::isQuarkusArtifact);
    }

    private static Finding finding(UnifiedVuln v, Map<String, Hit> hits) {
        Finding f = new Finding();
        f.id = v.id;
        f.cve = v.cveId;
        f.severity = v.severity;
        f.score = v.cvssScore;
        f.published = v.published;
        f.summary = v.summary;
        for (Hit h : hits.values()) f.affects.add(shortName(h.artifact()) + " " + h.version());
        return f;
    }

    // -----------------------------------------------------------------------
    // Cause
    // -----------------------------------------------------------------------

    /**
     * Describes how the artifacts of {@code hits} differ between the two releases, e.g.
     * "quarkus-vertx-http (+3 more) 3.27.3 -> 3.27.3.1". For a resolved advisory the hits are the ones
     * from the older release, and the version the advisory says it was fixed in is appended.
     */
    private static String cause(Map<String, Hit> hits, Map<String, String> previous, Map<String, String> current,
                                boolean resolved) {
        Map<String, List<String>> byChange = new LinkedHashMap<>();
        for (Hit h : hits.values()) {
            String before = previous.get(h.artifact());
            String after = current.get(h.artifact());
            String change;
            if (before == null) change = "added to the BOM at " + after;
            else if (after == null) change = removedOrRelocated(h.artifact(), before, current);
            else change = before + " -> " + after;
            byChange.computeIfAbsent(change, k -> new ArrayList<>()).add(h.artifact());
        }

        List<String> parts = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String, List<String>> e : byChange.entrySet()) {
            if (shown++ == MAX_CAUSE_GROUPS) {
                parts.add("+" + (byChange.size() - MAX_CAUSE_GROUPS) + " more changes");
                break;
            }
            List<String> names = e.getValue();
            String who = shortName(names.get(0)) + (names.size() > 1 ? " (+" + (names.size() - 1) + " more)" : "");
            parts.add(who + " " + e.getKey());
        }
        String text = String.join("; ", parts);

        if (resolved) {
            TreeSet<String> fixed = new TreeSet<>(QuarkusBomService::compareVersions);
            for (Hit h : hits.values()) if (h.fixed() != null) fixed.add(h.fixed());
            if (!fixed.isEmpty()) text += "  [advisory fixed in " + String.join(", ", fixed) + "]";
        }
        return text;
    }

    /**
     * An artifact that left the BOM may simply have moved to another groupId (org.lz4:lz4-java became
     * at.yawk.lz4:lz4-java). Same artifactId under a different group in the newer BOM is reported as a relocation.
     */
    private static String removedOrRelocated(String artifact, String before, Map<String, String> current) {
        String artifactId = artifact.substring(artifact.indexOf(':') + 1);
        for (Map.Entry<String, String> e : current.entrySet()) {
            String key = e.getKey();
            if (!key.equals(artifact) && key.endsWith(":" + artifactId)) {
                return "relocated, was " + before + ", now " + key + " " + e.getValue();
            }
        }
        return "removed from the BOM (was " + before + ")";
    }

    private static String shortName(String artifact) {
        return artifact.startsWith("io.quarkus:") ? artifact.substring("io.quarkus:".length()) : artifact;
    }

    // -----------------------------------------------------------------------
    // Ordering and versions
    // -----------------------------------------------------------------------

    /**
     * Determines for each active finding which future Quarkus release fixed it.
     * Iterates releases in ascending version order (oldest first). For each active
     * advisory, looks forward through subsequent releases until the advisory no longer
     * applies, recording that release version in {@code f.fixedIn}.
     */
    private static void resolveFixedVersions(List<ReleaseGroup> oldestFirstGroups) {
        List<Release> allReleases = new ArrayList<>();
        for (ReleaseGroup g : oldestFirstGroups) {
            allReleases.addAll(g.releases);
        }

        List<Set<String>> activeIdsPerRelease = new ArrayList<>(allReleases.size());
        for (Release r : allReleases) {
            Set<String> activeIds = new HashSet<>();
            for (Finding f : r.cves.active.core) {
                if (f.id != null) activeIds.add(f.id);
            }
            for (Finding f : r.cves.active.dependencies) {
                if (f.id != null) activeIds.add(f.id);
            }
            activeIdsPerRelease.add(activeIds);
        }

        int n = allReleases.size();
        for (int i = 0; i < n; i++) {
            Release r = allReleases.get(i);
            List<Finding> findings = new ArrayList<>(r.cves.active.core.size() + r.cves.active.dependencies.size());
            findings.addAll(r.cves.active.core);
            findings.addAll(r.cves.active.dependencies);

            for (Finding f : findings) {
                if (f.id == null) continue;
                String fixedInVersion = null;
                for (int j = i + 1; j < n; j++) {
                    if (!activeIdsPerRelease.get(j).contains(f.id)) {
                        fixedInVersion = allReleases.get(j).version;
                        break;
                    }
                }
                f.fixedIn = fixedInVersion;
            }
        }
    }

    private static void sort(Split<Finding> split) {
        sort(split.core);
        sort(split.dependencies);
    }

    private static void sort(List<Finding> findings) {
        findings.sort(Comparator.comparingInt((Finding f) -> -severityRank(f.severity))
                .thenComparing((Finding f) -> f.published, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(f -> f.id == null ? "" : f.id));
    }

    private static void sortChanges(Split<Change> split) {
        sortChanges(split.core);
        sortChanges(split.dependencies);
    }

    private static void sortChanges(List<Change> changes) {
        changes.sort(Comparator.comparingInt((Change c) -> -severityRank(c.finding.severity))
                .thenComparing((Change c) -> c.finding.published, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(c -> c.finding.id == null ? "" : c.finding.id));
    }

    private static int severityRank(String severity) {
        if (severity == null) return 0;
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    /** "3.27" or "3.27.5" -> {3, 27}. */
    static int[] parseMinor(String text) {
        int[] m = text == null ? null : minorOf(text.trim());
        if (m == null) throw new IllegalArgumentException("not a major.minor version: " + text);
        return m;
    }

    /** First two numeric segments of a release version, or null if it does not start with major.minor. */
    static int[] minorOf(String version) {
        String[] p = version.split("\\.");
        if (p.length < 2) return null;
        try {
            return new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1])};
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static int compare(int[] a, int[] b) {
        return a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]);
    }
}
