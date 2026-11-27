package io.quarkusio.releases.sync;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.versioning.ComparableVersion;

import io.quarkusio.releases.Releases;

/**
 * Works out the parent_release of each micro: the micro it was built from. Pure logic, no I/O, and no Quarkus (so it
 * can be tested with plain JUnit, see LineageTest).
 * <p>
 * Community micros of a minor form a main line (pre-releases, then X.Y.0, X.Y.1, ...) with side lines of four-part
 * patches (X.Y.Z.1, X.Y.Z.2, ...) off it. Git confirms this for finals: each tag is one [RELEASE] commit off its
 * branch, X.Y.Z+1 is built on X.Y.Z (not on X.Y.Z.1), and X.Y.Z.2 on X.Y.Z.1. So a final's parent follows from the
 * versions. A pre-release's doesn't always (see {@link #preReleaseParent}), so git is asked.
 * <p>
 * The first micro of a minor (usually its first candidate) has its parent in another minor, which the versions can't
 * tell: LTS releases branch off a non-LTS branch, others off main, and main stopped being the 3.x trunk after 3.37
 * (4.0 descends from 3.37.0.CR1). That needs git ancestry ({@link Ancestry}): the parent is the newest micro of the
 * nearest earlier minor that is contained in it.
 * <p>
 * Vendor builds carry the community micro they are based on in their version: 3.20.1.redhat-00003 is a build of
 * community 3.20.1, and 3.20.1.SP1-redhat-00001 a service pack on top of the vendor's own 3.20.1 build. Which vendor
 * fixes a rebased build carries forward can't be seen from outside, so a build of a new community micro has that micro
 * as its parent, and a service pack the vendor's previous build.
 */
public final class Lineage {

    /** How many earlier minors are searched for the parent of a minor's first micro. */
    public static final int CROSS_MINOR_WINDOW = 6;
    /** The most commits a pre-release's tag can be off its branch point, release fixes and version bumps included. */
    public static final int MAX_RELEASE_FIXES = 5;

    private static final Pattern COMMUNITY = Pattern
            .compile("^(\\d+\\.\\d+\\.\\d+)(?:\\.(\\d+))?(?:\\.(Final|(?:Alpha|Beta|CR|RC)\\d*))?$");
    private static final Pattern VENDOR = Pattern.compile("^(\\d+\\.\\d+\\.\\d+)(?:\\.(?:Final|SP(\\d+)))?[.-]redhat-(\\d+)$");

    /**
     * Git ancestry between release tags.
     */
    @FunctionalInterface
    public interface Ancestry {
        /**
         * @return true if everything in the release {@code earlier} is in {@code micro}, apart from release bookkeeping
         *         commits (version bumps)
         */
        boolean contains(String micro, String earlier) throws Exception;
    }

    /** Vendor builds by community base, then service pack, then build number. */
    public static final Comparator<String> VENDOR_ORDER = Comparator
            .comparing((String v) -> new ComparableVersion(vendorPart(v, 1)))
            .thenComparingInt(v -> Integer.parseInt(vendorPart(v, 2)))
            .thenComparingInt(v -> Integer.parseInt(vendorPart(v, 3)));

    private Lineage() {
    }

    // ---- Community micros ----

    /**
     * The parents of a minor's micros that follow from their versions alone: every micro except the first of the main
     * line, whose parent is in another minor (see {@link #firstMicro} and {@link #crossMinorParent}).
     *
     * @param micros the micros of one minor, in any order
     * @return version to parent version; micros whose parent is not known are left out
     */
    public static Map<String, String> communityParents(List<Releases.MicroEntry> micros) {
        List<String> versions = micros.stream().map(Releases.MicroEntry::version).sorted(Lineage::compare).toList();
        List<String> mainLine = versions.stream().filter(Lineage::isMainLine).toList();
        Map<String, String> parents = new HashMap<>();
        for (int i = 1; i < mainLine.size(); i++) {
            parents.put(mainLine.get(i), mainLine.get(i - 1));
        }
        for (int i = 0; i < versions.size(); i++) {
            String version = versions.get(i);
            Matcher m = COMMUNITY.matcher(version);
            if (!m.matches()) {
                // Not a version we understand: the one before it, if any
                if (i > 0) {
                    parents.put(version, versions.get(i - 1));
                }
                continue;
            }
            if (m.group(2) == null) {
                continue;
            }
            // A patch, X.Y.Z.N: the previous patch of X.Y.Z, else X.Y.Z itself
            String base = m.group(1);
            int patch = Integer.parseInt(m.group(2));
            String parent = null;
            for (int j = i - 1; j >= 0 && parent == null; j--) {
                Matcher earlier = COMMUNITY.matcher(versions.get(j));
                if (earlier.matches() && earlier.group(1).equals(base) && isFinal(earlier)
                        && (earlier.group(2) == null || Integer.parseInt(earlier.group(2)) < patch)) {
                    parent = versions.get(j);
                }
            }
            if (parent == null && i > 0) {
                parent = versions.get(i - 1);
            }
            if (parent != null) {
                parents.put(version, parent);
            }
        }
        return parents;
    }

    /**
     * @return the oldest micro of the minor's main line (usually its first candidate), whose parent is in another
     *         minor; null if there are no micros
     */
    public static String firstMicro(List<Releases.MicroEntry> micros) {
        List<String> versions = micros.stream().map(Releases.MicroEntry::version).sorted(Lineage::compare).toList();
        return versions.stream().filter(Lineage::isMainLine).findFirst()
                .orElse(versions.isEmpty() ? null : versions.get(0));
    }

    /**
     * The parent of a pre-release, or of a minor's first micro. Usually a pre-release is built on the previous one, but
     * not always: 3.0's first alphas were each cut from a different 2.x line (Alpha2 from the 2.14.3.Final tag, Alpha3
     * and Alpha4 from main after 2.16.0.CR1), with the Jakarta transformation on top, and only from Alpha4 on were they
     * built on each other. So the previous pre-release is only the parent if git says it is contained; otherwise the
     * earlier minors are searched, as for a first micro.
     *
     * @param previous the previous micro of the minor's main line; null for the minor's first micro
     * @return the parent, or null if none is found
     */
    public static String preReleaseParent(String micro, String previous, List<List<Releases.MicroEntry>> earlierMinors,
            Ancestry ancestry) throws Exception {
        if (previous != null && ancestry.contains(micro, previous)) {
            return previous;
        }
        return crossMinorParent(micro, earlierMinors, ancestry);
    }

    /**
     * The parent of a minor's first micro: the newest micro, of the nearest earlier minor which has any, that the first
     * micro contains. A minor's finals (X.Y.0, X.Y.1, ...) are one line, so the ones it contains are the oldest, and a
     * binary search finds the newest: about log2(n) + 1 lookups. If it doesn't contain the minor's first final (it was
     * branched from main around the minor's candidates), its pre-releases are tried, newest first. Pre-releases aren't
     * always on one line (3.0's alphas weren't on the line 3.1 grew from), so they're not binary searched.
     *
     * @param earlierMinors the micros of each earlier minor, nearest first; only main-line micros with a GitHub release
     *        (so a tag) are used. At most {@link #CROSS_MINOR_WINDOW} minors are searched.
     * @return the parent, or null if none of the minors searched has a micro the first micro contains
     */
    public static String crossMinorParent(String firstMicro, List<List<Releases.MicroEntry>> earlierMinors,
            Ancestry ancestry) throws Exception {
        int searched = 0;
        for (List<Releases.MicroEntry> minor : earlierMinors) {
            List<String> mainLine = minor.stream().filter(m -> m.githubUrl() != null)
                    .map(Releases.MicroEntry::version).filter(Lineage::isMainLine).sorted(Lineage::compare).toList();
            if (mainLine.isEmpty()) {
                continue;
            }
            if (++searched > CROSS_MINOR_WINDOW) {
                return null;
            }
            List<String> finals = mainLine.stream().filter(v -> !isPreRelease(v)).toList();
            if (!finals.isEmpty() && ancestry.contains(firstMicro, finals.get(0))) {
                int low = 0; // contained
                int high = finals.size(); // first not contained, as far as is known
                while (high - low > 1) {
                    int mid = (low + high) >>> 1;
                    if (ancestry.contains(firstMicro, finals.get(mid))) {
                        low = mid;
                    } else {
                        high = mid;
                    }
                }
                return finals.get(low);
            }
            List<String> preReleases = mainLine.stream().filter(Lineage::isPreRelease).toList();
            for (int i = preReleases.size() - 1; i >= 0; i--) {
                if (ancestry.contains(firstMicro, preReleases.get(i))) {
                    return preReleases.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Whether a release is contained in a later one, from the commits of the release that the later one doesn't have
     * (oldest first). Each release tag sits on "[RELEASE] ..." version bump commits off its branch, which don't count.
     * Older candidates (1.x, 2.x) also got a few fixes on their branch just before tagging ("Disable failing Gradle
     * tests", "Skip quarkus-cli deployment for CR1"), so a pre-release whose last extra commit is its version bump, with
     * at most {@link #MAX_RELEASE_FIXES} commits in all, counts as contained too. Finals don't get that leeway: a
     * micro after the one a branch was cut from differs by a few backports, which look just the same.
     *
     * @param extra how many commits of the release the later one doesn't have
     * @param messages the first line of each of those commits, oldest first, when there are few enough to list
     */
    public static boolean contained(String release, int extra, List<String> messages) {
        if (extra == 0) {
            return true;
        }
        if (messages.size() < extra) {
            return false;
        }
        if (messages.stream().allMatch(m -> m.startsWith("[RELEASE]"))) {
            return true;
        }
        return isPreRelease(release) && extra <= MAX_RELEASE_FIXES
                && messages.get(messages.size() - 1).startsWith("[RELEASE]");
    }

    // ---- Vendor builds ----

    /**
     * The parents of a vendor's builds of one minor: a service pack (X.Y.Z.SPn) has the vendor's previous build of
     * X.Y.Z as its parent, and any other build the community micro X.Y.Z (or X.Y.Z.Final, as 1.x and 2.x named them).
     *
     * @param builds the vendor's builds of one minor, in any order
     * @param community every community micro version
     * @return version to parent version; builds whose parent is not known are left out
     */
    public static Map<String, String> vendorParents(List<Releases.MicroEntry> builds, Set<String> community) {
        Map<String, String> parents = new HashMap<>();
        for (Releases.MicroEntry build : builds) {
            Matcher m = VENDOR.matcher(build.version());
            if (!m.matches()) {
                continue;
            }
            String base = m.group(1);
            int sp = servicePack(m);
            String parent = null;
            if (sp > 0) {
                // The vendor's build of X.Y.Z with the highest lower service pack
                parent = builds.stream().map(Releases.MicroEntry::version)
                        .filter(v -> {
                            Matcher o = VENDOR.matcher(v);
                            return o.matches() && o.group(1).equals(base) && servicePack(o) < sp;
                        })
                        .max(VENDOR_ORDER)
                        .orElse(null);
            }
            if (parent == null) {
                parent = communityBase(build.version(), community);
            }
            if (parent != null) {
                parents.put(build.version(), parent);
            }
        }
        return parents;
    }

    /**
     * @return the community micro a vendor build is based on (3.20.1.SP1-redhat-00001 is based on 3.20.1), or null if
     *         the version isn't a vendor build or the community has no such micro
     */
    public static String communityBase(String vendorVersion, Set<String> community) {
        Matcher m = VENDOR.matcher(vendorVersion);
        if (!m.matches()) {
            return null;
        }
        String base = m.group(1);
        return community.contains(base) ? base : community.contains(base + ".Final") ? base + ".Final" : null;
    }

    /**
     * The vendors rebuild a version until it ships (3.27.4.SP3-redhat-00002, then -00003): keeps only the latest build
     * of each, the one that shipped.
     *
     * @return the latest build of each version, newest version first
     */
    public static List<String> latestBuilds(Iterable<String> versions) {
        Map<String, String> latest = new HashMap<>();
        for (String version : versions) {
            Matcher m = VENDOR.matcher(version);
            if (!m.matches()) {
                continue;
            }
            String key = m.group(1) + "/" + servicePack(m);
            latest.merge(key, version, (a, b) -> buildNumber(a) >= buildNumber(b) ? a : b);
        }
        List<String> result = new ArrayList<>(latest.values());
        result.sort(VENDOR_ORDER.reversed());
        return result;
    }

    // ---- Versions ----

    private static boolean isMainLine(String version) {
        Matcher m = COMMUNITY.matcher(version);
        return m.matches() && m.group(2) == null;
    }

    private static boolean isPreRelease(String version) {
        Matcher m = COMMUNITY.matcher(version);
        return m.matches() && m.group(3) != null && !m.group(3).equals("Final");
    }

    private static boolean isFinal(Matcher m) {
        return m.group(3) == null || m.group(3).equals("Final");
    }

    private static int servicePack(Matcher vendor) {
        return vendor.group(2) == null ? 0 : Integer.parseInt(vendor.group(2));
    }

    /** A part of a vendor build version (1 base, 2 service pack, 3 build number); "0" for a non-vendor version. */
    private static String vendorPart(String version, int group) {
        Matcher m = VENDOR.matcher(version);
        if (!m.matches()) {
            return "0";
        }
        String part = group == 2 ? String.valueOf(servicePack(m)) : m.group(group);
        return part;
    }

    private static int buildNumber(String vendorVersion) {
        Matcher m = VENDOR.matcher(vendorVersion);
        return m.matches() ? Integer.parseInt(m.group(3)) : -1;
    }

    static int compare(String a, String b) {
        return new ComparableVersion(a).compareTo(new ComparableVersion(b));
    }
}
