package io.quarkusio.cves;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkusio.releases.Micro;
import io.quarkusio.releases.Release;
import io.quarkusio.releases.Releases;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Builds the London Underground / Tube-style release branch map data for {@code /cves/tree/}.
 * Earliest releases sit on the left and minor/LTS release branches peel off along horizontal tracks,
 * with every evaluated advisory's affected releases, roots, and fix ("cut") stations mapped onto the tree.
 */
@ApplicationScoped
@Named("cveTree")
public class CveTreeMap {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TreePayload payload;
    private final String json;
    private final int releaseCount;
    private final int branchCount;
    private final int advisoryCount;

    @Inject
    public CveTreeMap(Releases releases, Cves cves, CveList cveList) {
        this.payload = build(releases, cves, cveList);
        this.releaseCount = payload.stations().size();
        this.branchCount = payload.branches().size();
        this.advisoryCount = payload.advisories().size();
        this.json = serialize(payload);
    }

    public String json() {
        return json;
    }

    public int releaseCount() {
        return releaseCount;
    }

    public int branchCount() {
        return branchCount;
    }

    public int advisoryCount() {
        return advisoryCount;
    }

    TreePayload payload() {
        return payload;
    }

    private static String serialize(TreePayload payload) {
        try {
            // Escape "</" so the JSON can never prematurely close a <script type="application/json"> tag
            return MAPPER.writeValueAsString(payload).replace("</", "<\\/");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize CVE tree map payload", e);
        }
    }

    static TreePayload build(Releases releases, Cves cves, CveList cveList) {
        if (releases == null || cves == null) {
            return new TreePayload(List.of(), List.of(), List.of(), List.of());
        }

        Set<String> scanned = new HashSet<>();
        if (cves.releases() != null) {
            for (Cves.Scanned s : cves.releases()) {
                if (s != null && s.version() != null) {
                    scanned.add(s.version());
                }
            }
        }

        // Collect all community micros from releases.yaml
        record RawMicro(String version, String minor, boolean lts, LocalDate date, String parent, boolean preRelease) {
        }

        Map<String, RawMicro> byVersion = new LinkedHashMap<>();
        Map<String, Boolean> ltsByMinor = new HashMap<>();
        for (Release r : releases.all()) {
            if (r.upcoming() || r.micros() == null || r.micros().isEmpty()) {
                continue;
            }
            ltsByMinor.put(r.version(), r.lts());
            for (Micro m : r.micros()) {
                if (m.version() == null) {
                    continue;
                }
                if (!scanned.isEmpty() && !scanned.contains(m.version())) {
                    continue;
                }
                byVersion.put(m.version(), new RawMicro(
                        m.version(), r.version(), r.lts(), m.releaseDate(), m.parentRelease(), m.preRelease()));
            }
        }

        if (byVersion.isEmpty()) {
            return new TreePayload(List.of(), List.of(), List.of(), List.of());
        }

        // Resolve nearest included ancestor for each micro
        Map<String, String> parentOf = new HashMap<>();
        Map<String, List<String>> childrenOf = new HashMap<>();
        for (RawMicro m : byVersion.values()) {
            String p = m.parent();
            Set<String> seen = new HashSet<>();
            while (p != null && !byVersion.containsKey(p) && seen.add(p)) {
                p = null; // non-scanned ancestors are skipped cleanly
            }
            if (p != null && !byVersion.containsKey(p)) {
                p = null;
            }
            parentOf.put(m.version(), p);
            if (p != null) {
                childrenOf.computeIfAbsent(p, k -> new ArrayList<>()).add(m.version());
            }
        }

        Comparator<String> chronoOrder = Comparator
                .comparing((String v) -> {
                    LocalDate d = byVersion.get(v).date();
                    return d == null ? LocalDate.MIN : d;
                })
                .thenComparing(CveTreeMap::versionKey);

        childrenOf.values().forEach(list -> list.sort(chronoOrder));

        // Topological ordering (parents always before children)
        List<String> topo = new ArrayList<>(byVersion.size());
        Set<String> visited = new HashSet<>();
        List<String> roots = byVersion.keySet().stream()
                .filter(v -> parentOf.get(v) == null)
                .sorted(chronoOrder)
                .toList();
        ArrayDeque<String> queue = new ArrayDeque<>(roots);
        visited.addAll(roots);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            topo.add(cur);
            for (String child : childrenOf.getOrDefault(cur, List.of())) {
                if (visited.add(child)) {
                    queue.add(child);
                }
            }
        }
        byVersion.keySet().stream().sorted(chronoOrder).filter(visited::add).forEach(topo::add);

        // Compute subtree sizes so the main trunk (lane 0) follows the coherent mainline
        // (and each minor branch continues straight along its own minor line).
        Map<String, Integer> subtreeSize = new HashMap<>();
        for (int i = topo.size() - 1; i >= 0; i--) {
            String v = topo.get(i);
            int size = 1;
            for (String child : childrenOf.getOrDefault(v, List.of())) {
                size += subtreeSize.getOrDefault(child, 1);
            }
            subtreeSize.put(v, size);
        }

        // Horizontal-compacted planar trunk-first layout:
        // - Lane 0 is the coherent main trunk along the top (earliest on the left, continuing along the mainline).
        // - Whenever a node has side branches peeling off below it, the next node on the parent's lane shifts right
        //   past the end of those side branches (+2 column gap for the branch terminus pill).
        // - That lets subsequent branches reuse lane 1, lane 2, etc., keeping total height to just the tree's
        //   branching depth (~3-4 lanes) with mathematically zero line crossings.
        Map<String, Integer> colOf = new HashMap<>();
        Map<String, Integer> laneOf = new HashMap<>();
        int nextRootCol = 0;

        for (String root : roots) {
            Box box = layoutSubtree(root, nextRootCol, 0, true, childrenOf, subtreeSize, colOf, laneOf);
            nextRootCol = box.maxCol() + 3;
        }
        // Any disconnected node (defensive)
        for (String v : topo) {
            if (!laneOf.containsKey(v)) {
                Box box = layoutSubtree(v, nextRootCol, 0, false, childrenOf, subtreeSize, colOf, laneOf);
                nextRootCol = box.maxCol() + 3;
            }
        }

        // Identify interchange stations (a release that has at least one child on a different lane)
        Set<String> interchanges = new HashSet<>();
        for (String v : topo) {
            int myLane = laneOf.get(v);
            for (String child : childrenOf.getOrDefault(v, List.of())) {
                if (laneOf.get(child) != myLane) {
                    interchanges.add(v);
                    break;
                }
            }
        }

        // Group stations by lane/minor for branch metadata & Tube line colors
        Map<String, List<String>> microsByMinor = new LinkedHashMap<>();
        for (String v : topo) {
            microsByMinor.computeIfAbsent(byVersion.get(v).minor(), k -> new ArrayList<>()).add(v);
        }
        List<String> minorOrder = new ArrayList<>(microsByMinor.keySet());
        minorOrder.sort(Comparator
                .comparingInt((String minor) -> colOf.get(microsByMinor.get(minor).get(0)))
                .thenComparing(CveTreeMap::versionKey));

        List<Branch> branches = new ArrayList<>(minorOrder.size());
        int colorIndex = 0;
        for (String minor : minorOrder) {
            List<String> ms = microsByMinor.get(minor);
            int startCol = ms.stream().mapToInt(colOf::get).min().orElse(0);
            int endCol = ms.stream().mapToInt(colOf::get).max().orElse(0);
            int branchLane = laneOf.get(ms.get(ms.size() - 1));
            branches.add(new Branch(
                    minor,
                    Boolean.TRUE.equals(ltsByMinor.get(minor)),
                    branchLane,
                    startCol,
                    endCol,
                    colorIndex++ % 8,
                    majorOf(minor)));
        }

        // Build station and edge lists
        List<Station> stations = new ArrayList<>(topo.size());
        List<Edge> edges = new ArrayList<>(topo.size());
        for (String v : topo) {
            RawMicro m = byVersion.get(v);
            int col = colOf.get(v);
            int lane = laneOf.get(v);
            String parent = parentOf.get(v);
            stations.add(new Station(
                    v,
                    m.minor(),
                    majorOf(m.minor()),
                    m.lts(),
                    m.preRelease(),
                    interchanges.contains(v),
                    m.date() != null ? m.date().toString() : "",
                    parent,
                    col,
                    lane));
            if (parent != null) {
                edges.add(new Edge(parent, v, laneOf.get(parent) != lane, m.minor()));
            }
        }

        // Build compact advisory list for client-side CVE overlay & station inspection
        List<AdvisoryOverlay> advisories = new ArrayList<>();
        if (cveList != null) {
            for (CveList.Entry e : cveList.entries()) {
                if (e.pending() || e.affectedReleases().isEmpty()) {
                    continue;
                }
                List<String> rootsList = new ArrayList<>();
                List<String> cutsList = new ArrayList<>();
                for (CveList.ReleaseRange rr : e.releaseRanges()) {
                    if (byVersion.containsKey(rr.root())) {
                        rootsList.add(rr.root());
                    }
                    for (String cut : rr.cuts()) {
                        if (byVersion.containsKey(cut)) {
                            cutsList.add(cut);
                        }
                    }
                }
                List<String> affectedInTree = e.affectedReleases().stream()
                        .filter(byVersion::containsKey)
                        .toList();
                if (affectedInTree.isEmpty()) {
                    continue;
                }
                List<String> comps = e.components().stream()
                        .map(CveList.ComponentDetail::component)
                        .distinct()
                        .toList();
                advisories.add(new AdvisoryOverlay(
                        e.primaryId(),
                        e.ghsaId(),
                        e.anchorId(),
                        e.secondaryAnchorId(),
                        e.severityKey(),
                        e.severityLabel(),
                        e.publishedIso(),
                        e.publishedDisplay(),
                        e.summary(),
                        e.core(),
                        comps,
                        affectedInTree,
                        rootsList,
                        cutsList));
            }
        }

        return new TreePayload(branches, stations, edges, advisories);
    }

    private record Box(int maxCol, int maxLane) {
    }

    /**
     * Lays out a branch starting at {@code (startCol, lane)}. For each node along the branch, any side
     * branches are laid out directly below {@code lane} first, and the next node on {@code lane} shifts
     * right past their rightmost column (+2 gap for branch pills). Every subsequent branch on {@code lane}
     * can therefore reuse {@code lane + 1} with zero crossings.
     */
    private static Box layoutSubtree(
            String start,
            int startCol,
            int lane,
            boolean isMainTrunk,
            Map<String, List<String>> childrenOf,
            Map<String, Integer> subtreeSize,
            Map<String, Integer> colOf,
            Map<String, Integer> laneOf) {
        int col = startCol;
        int maxCol = startCol;
        int maxLane = lane;
        String cur = start;

        while (cur != null) {
            colOf.put(cur, col);
            laneOf.put(cur, lane);
            maxCol = Math.max(maxCol, col);

            List<String> children = childrenOf.getOrDefault(cur, List.of());
            if (children.isEmpty()) {
                break;
            }
            String straight = pickStraightChild(cur, children, isMainTrunk, subtreeSize);
            List<String> sideBranches = new ArrayList<>();
            for (String child : children) {
                if (!child.equals(straight)) {
                    sideBranches.add(child);
                }
            }
            // Place smaller side branches closer to the parent lane
            sideBranches.sort(Comparator.comparingInt((String c) -> subtreeSize.getOrDefault(c, 1)));

            int childLane = lane + 1;
            int sideMaxCol = col;
            for (String branch : sideBranches) {
                Box sub = layoutSubtree(branch, col + 1, childLane, false, childrenOf, subtreeSize, colOf, laneOf);
                sideMaxCol = Math.max(sideMaxCol, sub.maxCol());
                maxCol = Math.max(maxCol, sub.maxCol());
                maxLane = Math.max(maxLane, sub.maxLane());
                childLane = sub.maxLane() + 1;
            }

            // Shift the next station on this branch right to clear the side branches below it
            col = sideBranches.isEmpty() ? col + 1 : sideMaxCol + 2;
            cur = straight;
        }
        return new Box(maxCol, maxLane);
    }

    /**
     * Picks which child continues straight along the current lane:
     * - On the main trunk (lane 0), follow the child with the largest subtree (the continuing mainline).
     * - On a release branch, prefer continuing along the same minor line; otherwise follow the largest subtree.
     */
    private static String pickStraightChild(String parent, List<String> children, boolean isMainTrunk,
                                            Map<String, Integer> subtreeSize) {
        if (children.size() == 1) {
            return children.get(0);
        }
        if (!isMainTrunk) {
            String parentMinor = minorOfVersion(parent);
            for (String child : children) {
                if (parentMinor.equals(minorOfVersion(child))) {
                    return child;
                }
            }
        }
        return children.stream()
                .max(Comparator.comparingInt((String c) -> subtreeSize.getOrDefault(c, 1))
                        .thenComparing(CveTreeMap::versionKey))
                .orElse(children.get(0));
    }

    private static String minorOfVersion(String v) {
        String[] p = v.split("\\.");
        return p.length >= 2 ? p[0] + "." + p[1] : v;
    }

    private static int majorOf(String minor) {
        int dot = minor.indexOf('.');
        try {
            return Integer.parseInt(dot > 0 ? minor.substring(0, dot) : minor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Numeric-aware sort key for versions like "3.9.0" vs "3.10.0". */
    private static String versionKey(String v) {
        String[] parts = v.split("[.-]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.matches("\\d+")) {
                sb.append(String.format(Locale.ROOT, "%05d.", Integer.parseInt(p)));
            } else {
                sb.append(p.toLowerCase(Locale.ROOT)).append('.');
            }
        }
        return sb.toString();
    }

    public record TreePayload(
            List<Branch> branches,
            List<Station> stations,
            List<Edge> edges,
            List<AdvisoryOverlay> advisories) {
    }

    public record Branch(String minor, boolean lts, int lane, int startCol, int endCol, int colorIndex, int major) {
    }

    public record Station(
            String version,
            String minor,
            int major,
            boolean lts,
            boolean preRelease,
            boolean interchange,
            String date,
            String parent,
            int col,
            int lane) {
    }

    public record Edge(String from, String to, boolean branchFork, String minor) {
    }

    public record AdvisoryOverlay(
            String id,
            String ghsa,
            String anchor,
            String ghsaAnchor,
            String severity,
            String severityLabel,
            String published,
            String publishedDisplay,
            String summary,
            boolean core,
            List<String> components,
            List<String> affected,
            List<String> roots,
            List<String> cuts) {
    }
}
