package io.quarkusio.cves;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkusio.releases.Micro;
import io.quarkusio.releases.Release;
import io.quarkusio.releases.Releases;

/**
 * Release dates and the release tree (each micro's parent), from releases.yaml, which owns both. cves.yaml names
 * releases by version only, so this is what turns "the pieces of the tree" into dates.
 */
record Lineage(Map<String, LocalDate> dates, Map<String, String> parents) {

    static Lineage of(Releases releases) {
        Map<String, LocalDate> dates = new HashMap<>();
        Map<String, String> parents = new HashMap<>();
        if (releases != null) {
            for (Release r : releases.all()) {
                for (Micro m : r.micros()) {
                    if (m.releaseDate() != null) {
                        dates.put(m.version(), m.releaseDate());
                    }
                    if (m.parentRelease() != null) {
                        parents.put(m.version(), m.parentRelease());
                    }
                }
            }
        }
        return new Lineage(dates, parents);
    }

    static final Lineage EMPTY = new Lineage(Map.of(), Map.of());

    LocalDate date(String version) {
        return dates.get(version);
    }

    /**
     * The oldest of the given releases that has a date: nothing is known about before it. Pass the releases that were
     * scanned, not every release in releases.yaml, which also lists releases too old to have a BOM to scan.
     */
    String horizonRelease(Collection<String> scanned) {
        return scanned.stream().filter(dates::containsKey).min(java.util.Comparator.comparing(dates::get)).orElse(null);
    }

    LocalDate horizon(Collection<String> scanned) {
        String release = horizonRelease(scanned);
        return release == null ? null : dates.get(release);
    }

    private Map<String, List<String>> children() {
        Map<String, List<String>> children = new HashMap<>();
        parents.forEach((child, parent) -> children.computeIfAbsent(parent, k -> new ArrayList<>()).add(child));
        return children;
    }

    /** What an advisory affects: the listed releases, and each piece of the tree expanded to its releases. */
    Set<String> affected(Cves.AffectedReleases a) {
        Set<String> out = new HashSet<>();
        if (a == null) {
            return out;
        }
        if (a.list() != null) {
            out.addAll(a.list());
        }
        if (a.tree() != null) {
            Map<String, List<String>> children = children();
            for (Cves.Piece p : a.tree()) {
                if (p.root() == null) {
                    continue;
                }
                Set<String> cuts = p.cuts() == null ? Set.of() : new HashSet<>(p.cuts());
                ArrayDeque<String> todo = new ArrayDeque<>(List.of(p.root()));
                while (!todo.isEmpty()) {
                    String v = todo.pop();
                    if (cuts.contains(v) || !out.add(v)) {
                        continue;
                    }
                    todo.addAll(children.getOrDefault(v, List.of()));
                }
            }
        }
        return out;
    }

    /** The first release, on or after {@code from}, built on an affected release but not itself affected. */
    LocalDate firstFixAtOrAfter(Collection<String> affected, LocalDate from) {
        Set<String> set = new HashSet<>(affected);
        LocalDate best = null;
        for (Map.Entry<String, String> e : parents.entrySet()) {
            LocalDate d = dates.get(e.getKey());
            if (d != null && !set.contains(e.getKey()) && set.contains(e.getValue()) && !d.isBefore(from)
                    && (best == null || d.isBefore(best))) {
                best = d;
            }
        }
        return best;
    }

    /** The newest release we know of. */
    String newest() {
        return dates.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }
}
