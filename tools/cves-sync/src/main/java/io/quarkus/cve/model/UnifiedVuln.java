package io.quarkus.cve.model;

import io.quarkus.cve.client.GitHubAdvisoryClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified vulnerability record that can be populated from either OSV or
 * a GitHub Security Advisory. Commands work exclusively with this type.
 */
public class UnifiedVuln {

    public String id;           // GHSA-… or CVE-… (canonical ID)
    public String cveId;        // CVE alias if known
    public String summary;
    public String details;
    public String published;
    public String modified;
    public String withdrawn;
    public String severity;     // "CRITICAL" / "HIGH" / "MEDIUM" / "LOW"
    public String cvssScore;    // e.g. "8.1" or full CVSS vector
    public String cvssVector;
    public List<String> aliases;
    public List<AffectedPkg> affected;
    public List<String> references;

    /** Source database this record came from. */
    public enum Source { OSV, GITHUB }
    public Source source = Source.OSV;

    /**
     * Quarkus platform releases affected, as compact ranges (e.g. "3.0.0 - 3.2.5"), oldest first.
     * Null = not computed; empty = no known platform release is affected.
     */
    public List<String> affectedReleases;

    public static class AffectedPkg {
        public String ecosystem;
        public String name;
        public String introducedVersion;
        public String fixedVersion;
        public String lastAffectedVersion;
        /** Every affected version interval; the three fields above keep only the last one seen. */
        public List<Interval> intervals = new ArrayList<>();
    }

    /** Affected when {@code introduced <= v < fixed} (or {@code v <= lastAffected}); null bound = open. */
    public static class Interval {
        public String introduced;
        public String fixed;
        public String lastAffected;
    }

    // -----------------------------------------------------------------------
    // Factory: from OsvVulnerability
    // -----------------------------------------------------------------------

    public static UnifiedVuln fromOsv(OsvVulnerability v) {
        UnifiedVuln u = new UnifiedVuln();
        u.source    = Source.OSV;
        u.id        = v.id;
        u.summary   = v.summary;
        u.details   = v.details;
        u.published = v.published;
        u.modified  = v.modified;
        u.withdrawn = v.withdrawn;
        u.aliases   = v.aliases;

        if (v.aliases != null) {
            u.cveId = v.aliases.stream()
                    .filter(a -> a != null && a.startsWith("CVE-"))
                    .findFirst().orElse(null);
        }

        if (v.severity != null && !v.severity.isEmpty()) {
            u.cvssVector = v.severity.get(0).score;
            u.cvssScore  = extractNumericScore(v.severity.get(0).score);
            u.severity   = scoreToLabel(u.cvssScore);
        }
        // OSV usually gives a CVSS vector rather than a numeric score, so the label above is
        // UNKNOWN; fall back to the qualitative severity GitHub attaches to the record.
        if (u.severity == null || "UNKNOWN".equals(u.severity)) {
            String label = qualitativeSeverity(v.databaseSpecific);
            if (label != null) u.severity = label;
        }

        if (v.references != null) {
            u.references = v.references.stream()
                    .map(r -> r.url)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();
        }

        if (v.affected != null) {
            u.affected = v.affected.stream()
                    .filter(a -> a.pkg != null)
                    .map(a -> {
                        AffectedPkg p = new AffectedPkg();
                        p.ecosystem = a.pkg.ecosystem;
                        p.name      = a.pkg.name;
                        if (a.ranges != null) {
                            for (OsvRange r : a.ranges) {
                                if (!"GIT".equals(r.type)) p.intervals.addAll(intervalsOf(r.events));
                                if (r.events != null) {
                                    for (OsvVersion ev : r.events) {
                                        if (ev.introduced     != null) p.introducedVersion    = ev.introduced;
                                        if (ev.fixed          != null) p.fixedVersion         = ev.fixed;
                                        if (ev.lastAffected   != null) p.lastAffectedVersion  = ev.lastAffected;
                                    }
                                }
                            }
                        }
                        return p;
                    })
                    .toList();
        }

        return u;
    }

    // -----------------------------------------------------------------------
    // Factory: from GitHubAdvisory
    // -----------------------------------------------------------------------

    public static UnifiedVuln fromGitHub(GitHubAdvisoryClient.GitHubAdvisory a) {
        UnifiedVuln u = new UnifiedVuln();
        u.source    = Source.GITHUB;
        u.id        = a.ghsaId != null ? a.ghsaId : a.cveId;
        u.cveId     = a.cveId;
        u.summary   = a.summary;
        u.details   = a.description;
        u.published = a.publishedAt;
        u.modified  = a.updatedAt;
        u.withdrawn = a.withdrawnAt;

        if (a.cveId != null) {
            u.aliases = List.of(a.cveId);
        }

        // CVSS score
        if (a.cvssSeverities != null) {
            // GitHub sends an empty v4 placeholder (null vector, score 0.0) when there is no v4 rating.
            GitHubAdvisoryClient.CvssScore best = isRated(a.cvssSeverities.cvssV4)
                    ? a.cvssSeverities.cvssV4 : a.cvssSeverities.cvssV3;
            if (isRated(best)) {
                u.cvssVector = best.vectorString;
                u.cvssScore  = best.score != null ? String.valueOf(best.score) : null;
            }
        }
        // Fallback to qualitative label if no CVSS
        u.severity = a.severity != null ? a.severity.toUpperCase() : scoreToLabel(u.cvssScore);

        u.references = a.references;

        if (a.vulnerabilities != null) {
            u.affected = a.vulnerabilities.stream()
                    .filter(v -> v.pkg != null)
                    .map(v -> {
                        AffectedPkg p = new AffectedPkg();
                        p.ecosystem          = v.pkg.ecosystem;
                        p.name               = v.pkg.name;
                        p.fixedVersion       = v.firstPatchedVersion;
                        p.introducedVersion  = parseIntroduced(v.vulnerableVersionRange);
                        Interval iv = parseGitHubRange(v.vulnerableVersionRange);
                        if (iv != null) p.intervals.add(iv);
                        return p;
                    })
                    .toList();
        }

        return u;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** OSV events, in order: each "introduced" opens an interval, "fixed" / "last_affected" closes it. */
    private static List<Interval> intervalsOf(List<OsvVersion> events) {
        List<Interval> out = new ArrayList<>();
        if (events == null) return out;
        Interval cur = null;
        for (OsvVersion ev : events) {
            if (ev.introduced != null) {
                if (cur != null) out.add(cur);
                cur = new Interval();
                cur.introduced = ev.introduced;
            } else if (ev.fixed != null || ev.lastAffected != null) {
                if (cur == null) cur = new Interval();
                cur.fixed = ev.fixed;
                cur.lastAffected = ev.lastAffected;
                out.add(cur);
                cur = null;
            }
        }
        if (cur != null) out.add(cur);
        return out;
    }

    /**
     * GitHub range such as ">= 3.0.0, < 3.2.6.Final", "< 2.16.11" or "= 1.2". A strict ">" is read
     * as ">=" (rare; slightly over-inclusive). Returns null if nothing could be read.
     */
    static Interval parseGitHubRange(String range) {
        if (range == null || range.isBlank()) return null;
        Interval iv = new Interval();
        boolean any = false;
        for (String part : range.split(",")) {
            String p = part.trim();
            if (p.startsWith(">=")) { iv.introduced = p.substring(2).trim(); any = true; }
            else if (p.startsWith(">")) { iv.introduced = p.substring(1).trim(); any = true; }
            else if (p.startsWith("<=")) { iv.lastAffected = p.substring(2).trim(); any = true; }
            else if (p.startsWith("<")) { iv.fixed = p.substring(1).trim(); any = true; }
            else if (p.startsWith("=")) {
                iv.introduced = p.substring(1).trim();
                iv.lastAffected = iv.introduced;
                any = true;
            }
        }
        return any ? iv : null;
    }

    private static boolean isRated(GitHubAdvisoryClient.CvssScore s) {
        return s != null && s.vectorString != null && s.score != null;
    }

    /**
     * OSV only provides a CVSS vector, not the computed score. When the same advisory was also
     * seen on GitHub, which does publish the score, take score and vector from there.
     */
    public void fillCvssFrom(UnifiedVuln other) {
        if (cvssScore != null || other.cvssScore == null) return;
        cvssScore  = other.cvssScore;
        cvssVector = other.cvssVector;
    }

    private static String extractNumericScore(String cvssVector) {
        if (cvssVector == null) return null;
        // Plain numeric score (e.g. "8.1")
        try {
            Double.parseDouble(cvssVector);
            return cvssVector;
        } catch (NumberFormatException ignored) {}
        // CVSS vector string — not a bare score, return null (will use label)
        return null;
    }

    /** GitHub's {@code database_specific.severity}: LOW / MODERATE / HIGH / CRITICAL. MODERATE -> MEDIUM. */
    private static String qualitativeSeverity(java.util.Map<String, Object> databaseSpecific) {
        if (databaseSpecific == null || !(databaseSpecific.get("severity") instanceof String s)) return null;
        String label = s.trim().toUpperCase();
        if (label.equals("MODERATE")) return "MEDIUM";
        return switch (label) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> label;
            default -> null;
        };
    }

    public static String scoreToLabel(String score) {
        if (score == null) return "UNKNOWN";
        try {
            double v = Double.parseDouble(score);
            if (v >= 9.0) return "CRITICAL";
            if (v >= 7.0) return "HIGH";
            if (v >= 4.0) return "MEDIUM";
            return "LOW";
        } catch (NumberFormatException e) {
            return "UNKNOWN";
        }
    }

    private static String parseIntroduced(String range) {
        if (range == null) return null;
        // e.g. ">= 1.0, < 2.0" → "1.0"
        int idx = range.indexOf(">=");
        if (idx >= 0) {
            String sub = range.substring(idx + 2).trim();
            return sub.split("[,\\s]")[0].trim();
        }
        idx = range.indexOf(">");
        if (idx >= 0) {
            return range.substring(idx + 1).trim().split("[,\\s]")[0].trim();
        }
        return null;
    }
}
