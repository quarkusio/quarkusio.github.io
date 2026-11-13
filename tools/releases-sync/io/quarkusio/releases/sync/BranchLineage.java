package io.quarkusio.releases.sync;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Works out which non-LTS minor an LTS was branched from. Release order is not a reliable signal: the LTS policy has
 * changed over time (n-1 used to be the parent; now an LTS and the next non-LTS are released concurrently, so 3.20 and
 * 3.21 came out the same day), and main stopped being the trunk for the 3.x branches from 3.38 on. The signal is git
 * ancestry: the parent is the candidate branch with the most recent merge-base with the LTS branch.
 */
public final class BranchLineage {

    /** How many minors below an LTS are checked. */
    public static final int WINDOW = 3;

    /** The merge-base of two branches: the commit and when it was made. */
    public record MergeBase(String sha, Instant date) {
    }

    /**
     * @param version the chosen non-LTS minor, or null when there is nothing to choose from
     * @param candidates the minors tied for the most recent merge-base, newest version first (one unless ambiguous)
     */
    public record Result(String version, List<String> candidates) {
        public boolean ambiguous() {
            return candidates.size() > 1;
        }
    }

    private BranchLineage() {
    }

    /**
     * @param mergeBases for each candidate non-LTS minor below the LTS, its merge-base with the LTS branch; candidates
     *        whose branch doesn't exist are left out
     */
    public static Result pick(Map<String, MergeBase> mergeBases) {
        Instant latest = mergeBases.values().stream().map(MergeBase::date).max(Comparator.naturalOrder()).orElse(null);
        List<String> tied = new ArrayList<>();
        if (latest != null) {
            mergeBases.forEach((v, mb) -> {
                if (mb.date().equals(latest)) {
                    tied.add(v);
                }
            });
        }
        tied.sort(Comparator.comparing(BranchLineage::sortKey).reversed());
        return new Result(tied.isEmpty() ? null : tied.get(0), tied);
    }

    /** Up to {@link #WINDOW} minors of the same major directly below the version, newest first. */
    public static List<String> minorsBelow(String version, List<String> all) {
        return all.stream()
                .filter(v -> major(v) == major(version) && compare(v, version) < 0)
                .sorted(Comparator.comparing(BranchLineage::sortKey).reversed())
                .limit(WINDOW)
                .toList();
    }

    public static int major(String v) {
        return Integer.parseInt(v.split("\\.")[0]);
    }

    static int compare(String a, String b) {
        return sortKey(a).compareTo(sortKey(b));
    }

    private static Long sortKey(String v) {
        String[] p = v.split("\\.");
        return Long.parseLong(p[0]) * 100000 + Long.parseLong(p[1]);
    }
}
