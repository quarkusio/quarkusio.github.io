package io.quarkus.cve.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Collapses the releases an advisory is active in into version ranges, in the notation vulnerability
 * directories use: {@code >=3.27.0.CR1 <3.27.6.1}. Each unbroken run of affected releases, in version
 * order, becomes one range that starts at its first release and ends before the first later release
 * that no longer has the advisory (so the upper bound is also the release that fixed it). A run that
 * reaches the newest release has no upper bound.
 *
 * Only the releases that were scanned are known, so the first range starts no earlier than the oldest
 * scanned release; that is a limit of the scan, not a claim about when the problem was introduced.
 */
public final class AdvisoryRanges {

    private AdvisoryRanges() {}

    /**
     * @param ascending  every scanned release version, oldest first
     * @param activeById for each advisory id, the release versions it is active in
     * @return advisory id (sorted) to its ranges, oldest first
     */
    public static Map<String, List<String>> compute(List<String> ascending, Map<String, ? extends Set<String>> activeById) {
        Map<String, List<String>> out = new TreeMap<>();
        activeById.forEach((id, active) -> {
            List<String> ranges = new ArrayList<>();
            String start = null;
            for (String version : ascending) {
                if (active.contains(version)) {
                    if (start == null) start = version;
                } else if (start != null) {
                    ranges.add(">=" + start + " <" + version);
                    start = null;
                }
            }
            if (start != null) ranges.add(">=" + start);
            out.put(id, ranges);
        });
        return out;
    }
}
