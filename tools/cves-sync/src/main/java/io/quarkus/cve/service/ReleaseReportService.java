package io.quarkus.cve.service;

import io.quarkus.cve.model.ReleaseContent;
import io.quarkus.cve.model.ReleaseReport;
import io.quarkus.cve.model.ReleaseReport.Advisory;
import io.quarkus.cve.model.ReleaseReport.Component;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.service.QuarkusBomService.BomSource;
import io.quarkus.cve.util.ReleaseTree;
import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.Po4a;
import io.quarkus.cve.util.TreeRanges;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
 * Builds the advisory report. releases.yaml owns the list of releases, their dates and which release each
 * was built from. For every release the BOM says which version of every component (Quarkus core and
 * extensions, and dependencies) it manages: the platform BOM where releases.yaml lists one, otherwise the
 * core BOM. An advisory affects a release when one of the components it names is managed at a version inside
 * an affected range.
 *
 * Advisories are today's knowledge applied to every release: an advisory published last month affects a
 * release shipped years ago if that release carries an affected version.
 */
@ApplicationScoped
public class ReleaseReportService {

    @Inject
    QuarkusBomService bomService;

    @Inject
    QuarkusArtifactService artifactService;

    @Inject
    CveOrgService cveOrgService;

    /** One component of an advisory as managed by one release. */
    private record Hit(String component, String version, String range) {}

    /**
     * @param releasesInput releases.yaml
     * @throws IOException when releases.yaml cannot be read
     */
    public ReleaseReport build(File releasesInput) throws IOException {
        ReleaseReport report = new ReleaseReport();

        // 1. The releases, and what each ships (cached; the first run downloads the BOMs).
        List<ReleaseList.Micro> micros = ReleaseList.read(releasesInput);
        ReleaseList.requireLineage(micros);
        Map<String, ReleaseList.Micro> byVersion = new HashMap<>();
        micros.forEach(m -> byVersion.put(m.version(), m));
        Comparator<String> order = releaseOrder(byVersion);

        Map<String, ReleaseContent> contents = readContents(micros);
        report.statistics.releasesScanned = contents.size();
        report.statistics.releasesWithoutContent = micros.size() - contents.size();
        if (contents.isEmpty()) return report;

        List<String> scanned = new ArrayList<>(contents.keySet());
        scanned.sort(order);
        for (String v : scanned) report.releases.add(new ReleaseReport.ReleaseSource(v, contents.get(v).source()));

        Map<String, String> parents = new HashMap<>();
        micros.forEach(m -> parents.put(m.version(), m.parent()));
        ReleaseTree tree = new ReleaseTree(parents, contents.keySet(), order);

        // 2. Every advisory for every component any of these releases manages. OSV for all of them;
        //    GitHub (one request per package) only for the Quarkus artifacts.
        TreeSet<String> universe = new TreeSet<>();
        contents.values().forEach(c -> universe.addAll(c.managed().keySet()));
        List<String> quarkusPackages = universe.stream().filter(ReleaseContent::isCore).toList();
        Log.log(1, "Scanning %d managed components (%d Quarkus) for advisories", universe.size(), quarkusPackages.size());
        List<UnifiedVuln> all = artifactService.queryAll(new ArrayList<>(universe), quarkusPackages);

        List<UnifiedVuln> evaluable = new ArrayList<>();
        for (UnifiedVuln v : all) {
            if (v.withdrawn != null) continue;
            if (QuarkusBomService.mavenPackages(v).isEmpty()) report.statistics.advisoriesNotEvaluable++;
            else evaluable.add(v);
        }
        Log.log(1, "%d advisories evaluated, %d without usable version ranges", evaluable.size(), report.statistics.advisoriesNotEvaluable);

        // CVEs cve.org knows that name a managed component but that no advisory above has ranges for.
        report.pending = pending(universe, all, byVersion.get(scanned.get(0)).date());
        report.statistics.advisoriesPending = report.pending.size();
        Log.log(1, "%d CVEs without version ranges yet (pending)", report.pending.size());

        // 3. Which releases each advisory affects, and through which components.
        Map<String, ComparableVersion> parsed = new HashMap<>();
        for (UnifiedVuln v : evaluable) {
            Advisory a = advisory(v, scanned, contents, tree, order, parsed);
            if (a != null) report.advisories.put(v.id, a);
        }
        report.statistics.advisoriesEvaluated = evaluable.size();
        Log.log(1, "%d of those affect at least one release", report.advisories.size());
        return report;
    }

    /** Every release's managed components, from the platform BOM where there is one, else the core BOM. */
    private Map<String, ReleaseContent> readContents(List<ReleaseList.Micro> micros) {
        Map<String, ReleaseContent> out = new LinkedHashMap<>();
        boolean[] announced = {false};
        Runnable onDownload = () -> {
            if (!announced[0]) {
                announced[0] = true;
                System.err.printf("Downloading Quarkus BOMs not yet cached (one-time, up to %d releases)...%n", micros.size());
            }
        };
        Log.log(1, "Reading the BOMs of %d releases", micros.size());
        int done = 0;
        for (ReleaseList.Micro m : micros) {
            Log.log(3, "BOM %d/%d: %s", ++done, micros.size(), m.version());
            BomSource source = m.hasPlatformBom() ? BomSource.PLATFORM : BomSource.CORE;
            Map<String, String> managed = bomService.managedVersions(m.version(), source, onDownload);
            if (managed == null) {
                if (predatesCoreBom(m.version())) {
                    Log.log(1, "no %s for %s; it is left out", source.coordinate, m.version());
                } else {
                    Log.warn("could not read %s for %s; it is left out and the report is incomplete",
                            source.coordinate, m.version());
                }
                continue;
            }
            out.put(m.version(), new ReleaseContent(m.version(), source.id(m.version()), managed));
        }
        return out;
    }

    /** The core BOM was first published at 0.11.0; the 0.2 to 0.10 releases have no BOM to read. */
    static boolean predatesCoreBom(String version) {
        String[] p = version.split("\\.");
        try {
            return p.length >= 2 && Integer.parseInt(p[0]) == 0 && Integer.parseInt(p[1]) < 11;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /** Release date, then version, so the order is stable and does not depend on the order releases.yaml lists them in. */
    static Comparator<String> releaseOrder(Map<String, ReleaseList.Micro> byVersion) {
        return Comparator
                .comparing((String v) -> {
                    ReleaseList.Micro m = byVersion.get(v);
                    return m == null || m.date() == null ? "" : m.date();
                })
                .thenComparing(ComparableVersion::new);
    }

    /** Pending CVEs from cve.org, from the year of the oldest scanned release. A failure only leaves the list short. */
    private List<ReleaseReport.Pending> pending(Set<String> managed, List<UnifiedVuln> known, String oldestDate) {
        int year = oldestDate != null && oldestDate.length() >= 4
                ? Integer.parseInt(oldestDate.substring(0, 4)) : java.time.Year.now().getValue() - 1;
        try {
            return cveOrgService.pending(managed, known, year);
        } catch (RuntimeException ex) {
            Log.warn("could not look up pending CVEs at cve.org (%s)", ex.getMessage());
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // One advisory
    // -----------------------------------------------------------------------

    /** The advisory as it applies to the scanned releases, or null when none of them is affected. */
    private static Advisory advisory(UnifiedVuln v, List<String> scanned, Map<String, ReleaseContent> contents,
                                     ReleaseTree tree, Comparator<String> order, Map<String, ComparableVersion> parsed) {
        Set<String> affected = new HashSet<>();
        Map<String, Component> components = new LinkedHashMap<>();
        Map<String, Set<String>> shipped = new HashMap<>();
        for (String release : scanned) {
            Map<String, String> managed = contents.get(release).managed();
            for (Hit h : hits(managed, v, parsed)) {
                affected.add(release);
                String key = h.component() + " " + h.range();
                components.computeIfAbsent(key, k -> {
                    Component c = new Component();
                    c.component = h.component();
                    c.range = h.range();
                    c.core = ReleaseContent.isCore(h.component()) ? Boolean.TRUE : null;
                    return c;
                });
                shipped.computeIfAbsent(key, k -> new TreeSet<>(QuarkusBomService::compareVersions)).add(h.version());
            }
        }
        if (affected.isEmpty()) return null;

        Advisory a = new Advisory();
        a.cve = v.cveId;
        a.severity = v.severity;
        a.published = v.published;
        a.summary = Po4a.safe(v.summary);
        components.forEach((key, c) -> {
            c.shipped.addAll(shipped.get(key));
            a.affectedComponents.add(c);
        });
        a.affectedComponents.sort(Comparator.comparing((Component c) -> c.component).thenComparing(c -> c.range));

        a.affectedReleases.list = scanned.stream().filter(affected::contains).toList();
        a.affectedReleases.tree = TreeRanges.compute(affected, tree, order);
        return a;
    }

    /** The components of {@code v} that {@code managed} puts inside an affected range. */
    private static List<Hit> hits(Map<String, String> managed, UnifiedVuln v, Map<String, ComparableVersion> parsed) {
        List<Hit> hits = new ArrayList<>();
        for (UnifiedVuln.AffectedPkg p : QuarkusBomService.mavenPackages(v)) {
            String version = managed.get(p.name);
            if (version == null) continue;
            UnifiedVuln.Interval iv = QuarkusBomService.matching(version, p, parsed);
            if (iv != null) hits.add(new Hit(p.name, version, range(iv)));
        }
        return hits;
    }

    /**
     * The affected interval of one package as the advisory states it, in the notation of the Quarkus ranges:
     * {@code >=4.1.0 <4.1.100}, {@code <4.1.100} when nothing is said about where it starts, {@code <=2.0} for a
     * last affected version.
     */
    static String range(UnifiedVuln.Interval iv) {
        List<String> parts = new ArrayList<>();
        if (iv.introduced != null && !iv.introduced.equals("0")) parts.add(">=" + iv.introduced);
        if (iv.fixed != null) parts.add("<" + iv.fixed);
        else if (iv.lastAffected != null) parts.add("<=" + iv.lastAffected);
        return parts.isEmpty() ? "*" : String.join(" ", parts);
    }
}
