package io.quarkus.cve.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The release tree: every release has one parent (the release it was built from), so releases form a
 * tree rather than a line. A release without BOM content is not scanned; its scanned descendants hang
 * off the nearest scanned ancestor instead.
 *
 * The parents are not worked out here: they are the {@code parent_release} of each micro in releases.yaml, which
 * src/main/java/io/quarkusio/releases/sync writes (its own {@code Lineage} class works them out from versions and git ancestry).
 */
public final class ReleaseTree {

    private final Map<String, String> scannedParent = new HashMap<>();
    private final Map<String, List<String>> children = new HashMap<>();

    /**
     * @param parentOf the parent release of each release, as recorded (null or absent for a root)
     * @param scanned  the releases that are part of the scan
     * @param order    the order children are listed in, normally by release date
     */
    public ReleaseTree(Map<String, String> parentOf, Set<String> scanned, Comparator<String> order) {
        for (String v : scanned) {
            String p = parentOf.get(v);
            Set<String> seen = new HashSet<>();
            while (p != null && !scanned.contains(p) && seen.add(p)) p = parentOf.get(p);
            if (p != null && !scanned.contains(p)) p = null; // cycle in the input: treat as a root
            scannedParent.put(v, p);
            if (p != null) children.computeIfAbsent(p, k -> new ArrayList<>()).add(v);
        }
        children.values().forEach(c -> c.sort(order));
    }

    /** The nearest scanned ancestor of a scanned release, or null when it is a root of the scan. */
    public String parent(String release) {
        return scannedParent.get(release);
    }

    /** The scanned releases built directly on this one. */
    public List<String> children(String release) {
        return children.getOrDefault(release, List.of());
    }
}
