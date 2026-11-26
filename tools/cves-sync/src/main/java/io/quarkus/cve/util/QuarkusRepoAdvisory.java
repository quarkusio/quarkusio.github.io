package io.quarkus.cve.util;

import io.quarkus.cve.client.GitHubAdvisoryClient;
import io.quarkus.cve.model.UnifiedVuln;
import io.quarkus.cve.service.QuarkusBomService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps repository-level security advisories from {@code quarkusio/quarkus} into {@link UnifiedVuln}
 * records for use as a backup when OSV and GitHub's global advisory database do not yet have reviewed
 * ranges for a Quarkus-published advisory.
 *
 * Maintainer-authored repository advisories often use looser range syntax than reviewed global advisories:
 * <ul>
 *   <li>{@code =<3.38} (reversed {@code <=} operator)</li>
 *   <li>bare exact versions such as {@code 4.0.0.Beta1} without an {@code =} prefix</li>
 *   <li>multiple comma-separated upper bounds in one string ({@code <3.20.6, <3.27.3, <3.33.1}) or
 *       separate entries for the same package ({@code <3.27.6}, {@code <3.33.4}, {@code <3.39.5})</li>
 * </ul>
 * When a package has multiple unbounded-below upper bounds across distinct {@code major.minor} lines,
 * each higher line is bounded below at the next minor after the previous fixed line (e.g. {@code <3.27.6}
 * followed by {@code <3.33.4} becomes {@code <3.27.6} and {@code >=3.28.0 <3.33.4}) so that fixed
 * earlier LTS releases are not falsely swallowed by a later branch's upper bound.
 */
public final class QuarkusRepoAdvisory {

    private static final Pattern MAJOR_MINOR = Pattern.compile("^(\\d+)\\.(\\d+)(?:\\..*|$)");
    /** Fixes maintainer typos such as {@code <=3.35.1.1. <=3.27.3.1} where a period replaced a comma. */
    private static final Pattern DOT_BEFORE_OP = Pattern.compile("\\.\\s+(?=[<>]=?|=<|=)");

    private QuarkusRepoAdvisory() {}

    /** Converts a published repository advisory, or returns null if it is not published / is withdrawn. */
    public static UnifiedVuln toUnifiedVuln(GitHubAdvisoryClient.GitHubAdvisory a) {
        if (a == null || a.withdrawnAt != null) return null;
        if (a.state != null && !"published".equalsIgnoreCase(a.state)) return null;

        UnifiedVuln u = UnifiedVuln.fromGitHub(a);
        if (a.vulnerabilities == null || a.vulnerabilities.isEmpty()) {
            return u;
        }

        Map<String, UnifiedVuln.AffectedPkg> byPkg = new LinkedHashMap<>();
        for (GitHubAdvisoryClient.GitHubVulnerability v : a.vulnerabilities) {
            if (v.pkg == null || v.pkg.name == null || v.pkg.name.isBlank()) continue;
            String eco = v.pkg.ecosystem == null ? "" : v.pkg.ecosystem;
            String key = eco.toLowerCase(Locale.ROOT) + "|" + v.pkg.name.trim();
            UnifiedVuln.AffectedPkg pkg = byPkg.computeIfAbsent(key, k -> {
                UnifiedVuln.AffectedPkg p = new UnifiedVuln.AffectedPkg();
                p.ecosystem = eco;
                p.name = v.pkg.name.trim();
                return p;
            });
            List<UnifiedVuln.Interval> parsed = parseRanges(v.vulnerableVersionRange);
            if (parsed.isEmpty() && v.firstPatchedVersion != null && !v.firstPatchedVersion.isBlank()) {
                UnifiedVuln.Interval iv = new UnifiedVuln.Interval();
                iv.fixed = v.firstPatchedVersion.trim();
                parsed = List.of(iv);
            }
            pkg.intervals.addAll(parsed);
        }

        for (UnifiedVuln.AffectedPkg pkg : byPkg.values()) {
            inferMinorLineLowerBounds(pkg.intervals);
        }
        u.affected = new ArrayList<>(byPkg.values());
        return u;
    }

    /**
     * Parses a maintainer-authored range string into one or more intervals.
     * Supports standard {@code >=, >, <=, <, =}, reversed {@code =<}, bare exact versions,
     * and comma-separated multi-branch upper bounds.
     */
    static List<UnifiedVuln.Interval> parseRanges(String range) {
        List<UnifiedVuln.Interval> out = new ArrayList<>();
        if (range == null || range.isBlank()) return out;

        String normalized = DOT_BEFORE_OP.matcher(range.trim()).replaceAll(", ");
        UnifiedVuln.Interval cur = null;
        for (String raw : normalized.split(",")) {
            String p = raw.trim();
            if (p.isEmpty()) continue;

            if (p.startsWith(">=")) {
                String ver = p.substring(2).trim();
                if (ver.isEmpty()) continue;
                if (cur != null && (cur.introduced != null || cur.fixed != null || cur.lastAffected != null)) {
                    out.add(cur);
                    cur = null;
                }
                if (cur == null) cur = new UnifiedVuln.Interval();
                cur.introduced = ver;
                cur.introducedExclusive = false;
            } else if (p.startsWith(">")) {
                String ver = p.substring(1).trim();
                if (ver.isEmpty()) continue;
                if (cur != null && (cur.introduced != null || cur.fixed != null || cur.lastAffected != null)) {
                    out.add(cur);
                    cur = null;
                }
                if (cur == null) cur = new UnifiedVuln.Interval();
                cur.introduced = ver;
                cur.introducedExclusive = true;
            } else if (p.startsWith("<=") || p.startsWith("=<")) {
                String ver = p.substring(2).trim();
                if (ver.isEmpty()) continue;
                if (cur != null && (cur.fixed != null || cur.lastAffected != null)) {
                    out.add(cur);
                    cur = null;
                }
                if (cur == null) cur = new UnifiedVuln.Interval();
                cur.lastAffected = ver;
                out.add(cur);
                cur = null;
            } else if (p.startsWith("<")) {
                String ver = p.substring(1).trim();
                if (ver.isEmpty()) continue;
                if (cur != null && (cur.fixed != null || cur.lastAffected != null)) {
                    out.add(cur);
                    cur = null;
                }
                if (cur == null) cur = new UnifiedVuln.Interval();
                cur.fixed = ver;
                out.add(cur);
                cur = null;
            } else if (p.startsWith("=")) {
                String ver = p.substring(1).trim();
                if (ver.isEmpty()) continue;
                if (cur != null) {
                    out.add(cur);
                    cur = null;
                }
                UnifiedVuln.Interval exact = new UnifiedVuln.Interval();
                exact.introduced = ver;
                exact.lastAffected = ver;
                out.add(exact);
            } else if (Character.isDigit(p.charAt(0))) {
                // Bare version such as "4.0.0.Beta1" -> exact version match
                if (cur != null) {
                    out.add(cur);
                    cur = null;
                }
                UnifiedVuln.Interval exact = new UnifiedVuln.Interval();
                exact.introduced = p;
                exact.lastAffected = p;
                out.add(exact);
            }
        }
        if (cur != null) out.add(cur);
        return out;
    }

    /**
     * When a package has multiple unbounded-below upper bounds across different minor lines
     * (e.g. {@code <3.27.6}, {@code <3.33.4}, {@code <3.39.5}), bound each higher line below
     * at the next minor after the previous line so fixed earlier releases are not marked affected.
     */
    static void inferMinorLineLowerBounds(List<UnifiedVuln.Interval> intervals) {
        List<UnifiedVuln.Interval> unbounded = new ArrayList<>();
        for (UnifiedVuln.Interval iv : intervals) {
            boolean noLower = iv.introduced == null || (!iv.introducedExclusive && "0".equals(iv.introduced));
            if (noLower && upperBound(iv) != null) {
                unbounded.add(iv);
            }
        }
        if (unbounded.size() < 2) return;

        unbounded.sort(Comparator.comparing(QuarkusRepoAdvisory::upperBound, QuarkusBomService::compareVersions));
        for (int i = 1; i < unbounded.size(); i++) {
            String prevUpper = upperBound(unbounded.get(i - 1));
            String curUpper = upperBound(unbounded.get(i));
            String nextMinor = nextMinorVersion(prevUpper);
            if (nextMinor != null && QuarkusBomService.compareVersions(nextMinor, curUpper) < 0) {
                unbounded.get(i).introduced = nextMinor;
                unbounded.get(i).introducedExclusive = false;
            }
        }
    }

    private static String upperBound(UnifiedVuln.Interval iv) {
        return iv.fixed != null ? iv.fixed : iv.lastAffected;
    }

    /** {@code 3.27.6 -> 3.28.0}; null if the version does not start with {@code major.minor}. */
    static String nextMinorVersion(String version) {
        if (version == null) return null;
        Matcher m = MAJOR_MINOR.matcher(version);
        if (!m.matches()) return null;
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        return major + "." + (minor + 1) + ".0";
    }
}
