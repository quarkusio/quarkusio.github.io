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

    /** Builds the advisory report. */

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
        int beforeMerge = evaluable.size();
        evaluable = mergeByCve(evaluable);
        if (evaluable.size() < beforeMerge) {
            Log.log(1, "%d advisories merged into others that name the same CVE", beforeMerge - evaluable.size());
        }
        Log.log(1, "%d advisories evaluated, %d without usable version ranges", evaluable.size(), report.statistics.advisoriesNotEvaluable);

        // CVEs cve.org knows that name a managed component but that no advisory above has ranges for.
        report.pending = pending(universe, evaluable, byVersion.get(scanned.get(0)).date());
        Set<String> rejectedKnown = cveOrgService.rejectedKnownCves();
        report.statistics.advisoriesPending = report.pending.size();
        Log.log(1, "%d CVEs without version ranges yet (pending)", report.pending.size());

        // 3. Which releases each advisory affects, and through which components.
        List<String> topo = topologicalOrder(scanned, tree);
        Map<String, ComparableVersion> parsed = new HashMap<>();
        int evaluatedCount = 0;
        for (UnifiedVuln v : evaluable) {
            if (isRejected(v, rejectedKnown)) continue;
            evaluatedCount++;
            Advisory a = advisory(v, scanned, topo, contents, byVersion, tree, order, parsed);
            if (a != null) report.advisories.put(v.id, a);
        }
        report.statistics.advisoriesEvaluated = evaluatedCount;
        Log.log(1, "%d of those affect at least one release", report.advisories.size());
        return report;
    }

    /**
     * GitHub sometimes keeps two GHSAs for one CVE (each lists the other as an alias), which would list the CVE twice.
     * Advisories that share a CVE become one: the lowest id, with every affected interval of either (a release is
     * affected if any of them says so), the highest severity and the earliest publication date.
     */
    static List<UnifiedVuln> mergeByCve(List<UnifiedVuln> vulns) {
        List<UnifiedVuln> out = new ArrayList<>();
        Map<String, List<UnifiedVuln>> byCve = new LinkedHashMap<>();
        for (UnifiedVuln v : vulns) {
            if (v.cveId == null) out.add(v);
            else byCve.computeIfAbsent(v.cveId, k -> new ArrayList<>()).add(v);
        }
        for (List<UnifiedVuln> group : byCve.values()) {
            if (group.size() == 1) {
                out.add(group.get(0));
                continue;
            }
            group.sort(Comparator.comparing((UnifiedVuln u) -> u.id == null ? "" : u.id));
            UnifiedVuln primary = group.get(0);
            UnifiedVuln m = new UnifiedVuln();
            m.id = primary.id;
            m.cveId = primary.cveId;
            m.source = primary.source;
            m.summary = primary.summary;
            m.details = primary.details;
            m.modified = primary.modified;
            m.references = primary.references;
            m.severity = primary.severity;
            m.published = primary.published;
            Set<String> aliases = new TreeSet<>();
            Map<String, UnifiedVuln.AffectedPkg> pkgs = new LinkedHashMap<>();
            for (UnifiedVuln u : group) {
                if (u.id != null && !u.id.equals(m.id)) aliases.add(u.id);
                if (u.aliases != null) {
                    for (String a : u.aliases) if (a != null && !a.equals(m.id)) aliases.add(a);
                }
                if ((m.summary == null || m.summary.isBlank()) && u.summary != null && !u.summary.isBlank()) {
                    m.summary = u.summary;
                }
                if (u.published != null && (m.published == null || u.published.compareTo(m.published) < 0)) m.published = u.published;
                if (severityRank(u.severity) > severityRank(m.severity)) m.severity = u.severity;
                m.fillCvssFrom(u);
                if (u.affected == null) continue;
                for (UnifiedVuln.AffectedPkg p : u.affected) {
                    String ecoKey = (p.ecosystem == null ? "" : p.ecosystem.toLowerCase(java.util.Locale.ROOT)) + "|" + p.name;
                    UnifiedVuln.AffectedPkg t = pkgs.computeIfAbsent(ecoKey, k -> {
                        UnifiedVuln.AffectedPkg c = new UnifiedVuln.AffectedPkg();
                        c.ecosystem = p.ecosystem;
                        c.name = p.name;
                        return c;
                    });
                    for (UnifiedVuln.Interval iv : p.intervals) {
                        if (t.intervals.stream().noneMatch(x -> sameInterval(x, iv))) t.intervals.add(iv);
                    }
                }
            }
            m.aliases = new ArrayList<>(aliases);
            m.affected = new ArrayList<>(pkgs.values());
            out.add(m);
        }
        return out;
    }

    private static boolean sameInterval(UnifiedVuln.Interval a, UnifiedVuln.Interval b) {
        return java.util.Objects.equals(a.introduced, b.introduced) && a.introducedExclusive == b.introducedExclusive
                && java.util.Objects.equals(a.fixed, b.fixed) && java.util.Objects.equals(a.lastAffected, b.lastAffected);
    }

    private static int severityRank(String s) {
        if (s == null) return 0;
        return switch (s) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private static boolean isRejected(UnifiedVuln v, Set<String> rejected) {
        if (rejected.isEmpty()) return false;
        if (v.cveId != null && rejected.contains(v.cveId)) return true;
        if (v.id != null && rejected.contains(v.id)) return true;
        return v.aliases != null && v.aliases.stream().anyMatch(rejected::contains);
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
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            Log.warn("could not look up pending CVEs at cve.org (%s)", ex.getMessage());
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // One advisory
    // -----------------------------------------------------------------------

    /**
     * The advisory as it applies to the scanned releases, or null when none of them is affected.
     * A core-only release ({@code !hasPlatformBom}) cannot end a platform-component CVE's lifespan:
     * when its scanned parent is affected through a package absent from the core BOM, the release
     * stays affected until a descendant platform BOM ships a non-affected version.
     */
    /** Parents before children, so core-only inheritance never depends on release_date ordering. */
    static List<String> topologicalOrder(List<String> scanned, ReleaseTree tree) {
        List<String> out = new ArrayList<>(scanned.size());
        Set<String> visited = new HashSet<>();
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        for (String r : scanned) {
            if (tree.parent(r) == null && visited.add(r)) queue.add(r);
        }
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            out.add(cur);
            for (String child : tree.children(cur)) {
                if (visited.add(child)) queue.add(child);
            }
        }
        for (String r : scanned) {
            if (visited.add(r)) out.add(r);
        }
        return out;
    }

    private static Advisory advisory(UnifiedVuln v, List<String> scanned, List<String> topo, Map<String, ReleaseContent> contents,
                                     Map<String, ReleaseList.Micro> byVersion, ReleaseTree tree,
                                     Comparator<String> order, Map<String, ComparableVersion> parsed) {
        Set<String> affected = new HashSet<>();
        Map<String, Set<String>> affectedPkgsByRelease = new HashMap<>();
        Map<String, Component> components = new LinkedHashMap<>();
        Map<String, Set<String>> shipped = new HashMap<>();
        List<UnifiedVuln.AffectedPkg> mavenPkgs = QuarkusBomService.mavenPackages(v);
        for (String release : topo) {
            Map<String, String> managed = contents.get(release).managed();
            ReleaseList.Micro micro = byVersion.get(release);
            boolean coreOnly = micro != null && !micro.hasPlatformBom();
            Set<String> parentPkgs = coreOnly && tree.parent(release) != null
                    ? affectedPkgsByRelease.getOrDefault(tree.parent(release), Set.of())
                    : Set.of();

            Set<String> releasePkgs = new HashSet<>();
            for (UnifiedVuln.AffectedPkg p : mavenPkgs) {
                String version = managed.get(p.name);
                if (version != null) {
                    UnifiedVuln.Interval iv = QuarkusBomService.matching(version, p, parsed);
                    if (iv != null) {
                        affected.add(release);
                        releasePkgs.add(p.name);
                        String key = p.name + " " + range(iv);
                        components.computeIfAbsent(key, k -> {
                            Component c = new Component();
                            c.component = p.name;
                            c.range = range(iv);
                            c.core = ReleaseContent.isCore(p.name) ? Boolean.TRUE : null;
                            return c;
                        });
                        shipped.computeIfAbsent(key, k -> new TreeSet<>(QuarkusBomService::compareVersions)).add(version);
                    }
                } else if (coreOnly && parentPkgs.contains(p.name)) {
                    affected.add(release);
                    releasePkgs.add(p.name);
                }
            }
            if (!releasePkgs.isEmpty()) affectedPkgsByRelease.put(release, releasePkgs);
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

    /**
     * The affected interval of one package as the advisory states it, in the notation of the Quarkus ranges:
     * {@code >=4.1.0 <4.1.100}, {@code <4.1.100} when nothing is said about where it starts, {@code <=2.0} for a
     * last affected version.
     */
    static String range(UnifiedVuln.Interval iv) {
        if (iv.introduced != null && !iv.introducedExclusive && iv.fixed == null
                && iv.introduced.equals(iv.lastAffected)) {
            return "=" + iv.introduced;
        }
        List<String> parts = new ArrayList<>();
        if (iv.introduced != null && (iv.introducedExclusive || !iv.introduced.equals("0"))) {
            parts.add((iv.introducedExclusive ? ">" : ">=") + iv.introduced);
        }
        if (iv.fixed != null) parts.add("<" + iv.fixed);
        else if (iv.lastAffected != null) parts.add("<=" + iv.lastAffected);
        return parts.isEmpty() ? "*" : String.join(" ", parts);
    }
}
