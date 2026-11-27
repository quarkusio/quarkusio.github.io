package io.quarkusio.cves;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.quarkusio.releases.Micro;
import io.quarkusio.releases.Release;
import io.quarkusio.releases.Releases;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * View model for {@code /cves/all/}: every evaluated advisory and pending CVE from {@code _data/cves.yaml},
 * sorted newest-first by publication date, with stable lowercase anchor ids for both CVE and GHSA ids,
 * supported-vs-unsupported release breakdown, and collapsed EOL ranges.
 */
@ApplicationScoped
@Named("cveList")
public class CveList {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final List<Entry> entries;
    private final int advisoryCount;
    private final int pendingCount;
    private final int coreCount;

    @Inject
    public CveList(Cves cves, Releases releases) {
        this(cves, releases, LocalDate.now());
    }

    CveList(Cves cves) {
        this(cves, null, LocalDate.now());
    }

    CveList(Cves cves, Releases releases, LocalDate today) {
        List<Entry> built = build(cves, releases, today);
        this.entries = built;
        int adv = 0;
        int pend = 0;
        int core = 0;
        for (Entry e : built) {
            if (e.pending()) {
                pend++;
            } else {
                adv++;
            }
            if (e.core()) {
                core++;
            }
        }
        this.advisoryCount = adv;
        this.pendingCount = pend;
        this.coreCount = core;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int totalCount() {
        return entries.size();
    }

    public int advisoryCount() {
        return advisoryCount;
    }

    public int pendingCount() {
        return pendingCount;
    }

    public int coreCount() {
        return coreCount;
    }

    static List<Entry> build(Cves cves) {
        return build(cves, null, LocalDate.now());
    }

    static List<Entry> build(Cves cves, Releases releases, LocalDate today) {
        List<Entry> out = new ArrayList<>();
        if (cves == null) {
            return List.of();
        }
        ReleaseIndex relIndex = ReleaseIndex.from(releases, today);

        if (cves.advisories() != null) {
            for (Map.Entry<String, Cves.Advisory> e : cves.advisories().entrySet()) {
                String ghsaId = e.getKey();
                Cves.Advisory a = e.getValue();
                if (a == null) {
                    continue;
                }
                String cveId = blankToNull(a.cve());
                String primaryId = cveId != null ? cveId : ghsaId;
                String anchorId = primaryId.toLowerCase(Locale.ROOT);
                String secondaryAnchorId = cveId != null && ghsaId != null && !cveId.equalsIgnoreCase(ghsaId)
                        ? ghsaId.toLowerCase(Locale.ROOT)
                        : null;

                List<ComponentDetail> components = new ArrayList<>();
                boolean core = false;
                if (a.affectedComponents() != null) {
                    for (Cves.Component c : a.affectedComponents()) {
                        if (c == null || c.component() == null) {
                            continue;
                        }
                        boolean isCore = Boolean.TRUE.equals(c.core()) || isQuarkusCoordinate(c.component());
                        if (isCore) {
                            core = true;
                        }
                        List<String> shipped = c.shipped() != null ? List.copyOf(c.shipped()) : List.of();
                        components.add(new ComponentDetail(c.component(), blankToNull(c.range()), isCore, shipped));
                    }
                }

                List<String> releasesList = a.affectedReleases() != null && a.affectedReleases().list() != null
                        ? List.copyOf(a.affectedReleases().list())
                        : List.of();
                List<ReleaseRange> releaseRanges = new ArrayList<>();
                if (a.affectedReleases() != null && a.affectedReleases().tree() != null) {
                    for (Cves.Piece piece : a.affectedReleases().tree()) {
                        if (piece == null || piece.root() == null) {
                            continue;
                        }
                        List<String> cuts = piece.cuts() != null ? List.copyOf(piece.cuts()) : List.of();
                        releaseRanges.add(new ReleaseRange(piece.root(), cuts));
                    }
                }

                Severity sev = Severity.parse(a.severity());
                LocalDate publishedDate = parseDate(a.published());
                ImpactSummary impact = relIndex.summarize(releasesList, releaseRanges, publishedDate);

                out.add(new Entry(
                        primaryId,
                        cveId,
                        ghsaId,
                        anchorId,
                        secondaryAnchorId,
                        sev != null ? sev.label() : "Unknown",
                        sev != null ? sev.key() : "unknown",
                        publishedDate != null ? publishedDate.toString() : "",
                        publishedDate != null ? DISPLAY_DATE.format(publishedDate) : "—",
                        a.published() != null ? a.published() : "",
                        a.summary() != null ? a.summary() : "",
                        false,
                        core,
                        List.copyOf(components),
                        releasesList,
                        List.copyOf(releaseRanges),
                        impact.supportedImpacts(),
                        impact.unsupportedRanges(),
                        impact.eolUnfixedRoots(),
                        impact.preDisclosureCount(),
                        impact.postDisclosureCount(),
                        impact.fixedByDisclosure(),
                        cveId != null ? "https://www.cve.org/CVERecord?id=" + cveId : null,
                        ghsaId != null && ghsaId.startsWith("GHSA-") ? "https://github.com/advisories/" + ghsaId : null));
            }
        }
        if (cves.pending() != null) {
            for (Cves.Pending p : cves.pending()) {
                if (p == null || p.id() == null || p.id().isBlank()) {
                    continue;
                }
                String cveId = p.id().trim();
                List<ComponentDetail> components = new ArrayList<>();
                boolean core = false;
                if (p.artifacts() != null) {
                    for (String art : p.artifacts()) {
                        if (art == null || art.isBlank()) {
                            continue;
                        }
                        boolean isCore = isQuarkusCoordinate(art);
                        if (isCore) {
                            core = true;
                        }
                        components.add(new ComponentDetail(art, null, isCore, List.of()));
                    }
                }
                Severity sev = Severity.parse(p.severity());
                LocalDate publishedDate = parseDate(p.published());
                String cveLink = blankToNull(p.link()) != null ? p.link() : "https://www.cve.org/CVERecord?id=" + cveId;
                out.add(new Entry(
                        cveId,
                        cveId,
                        null,
                        cveId.toLowerCase(Locale.ROOT),
                        null,
                        sev != null ? sev.label() : "Unknown",
                        sev != null ? sev.key() : "unknown",
                        publishedDate != null ? publishedDate.toString() : "",
                        publishedDate != null ? DISPLAY_DATE.format(publishedDate) : "—",
                        p.published() != null ? p.published() : "",
                        p.summary() != null ? p.summary() : "",
                        true,
                        core,
                        List.copyOf(components),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        0,
                        0,
                        false,
                        cveLink,
                        null));
            }
        }
        out.sort(Comparator.comparing(Entry::publishedSortKey).reversed()
                .thenComparing(Comparator.comparing(Entry::primaryId).reversed()));
        return List.copyOf(out);
    }

    static boolean isQuarkusCoordinate(String ga) {
        if (ga == null) {
            return false;
        }
        int colon = ga.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        String group = ga.substring(0, colon);
        if ("io.quarkus.platform".equals(group)) {
            return false;
        }
        return "io.quarkus".equals(group) || group.startsWith("io.quarkus.");
    }

    static LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso.trim()).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (RuntimeException ignored) {
            if (iso.length() >= 10) {
                try {
                    return LocalDate.parse(iso.substring(0, 10));
                } catch (RuntimeException ignored2) {
                    return null;
                }
            }
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String versionKey(String v) {
        if (v == null) {
            return "";
        }
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

    private record ImpactSummary(
            List<SupportedBranchImpact> supportedImpacts,
            List<ReleaseRange> unsupportedRanges,
            List<String> eolUnfixedRoots,
            int preDisclosureCount,
            int postDisclosureCount,
            boolean fixedByDisclosure) {
    }

    private static final class ReleaseIndex {
        private final boolean hasMetadata;
        private final List<Release> supportedReleases;
        private final Set<String> supportedMinors;
        private final Map<String, String> minorByVersion;
        private final Map<String, String> parentByVersion;
        private final Map<String, List<String>> childrenByVersion;
        private final Map<String, LocalDate> dateByVersion;

        private ReleaseIndex(
                boolean hasMetadata,
                List<Release> supportedReleases,
                Set<String> supportedMinors,
                Map<String, String> minorByVersion,
                Map<String, String> parentByVersion,
                Map<String, List<String>> childrenByVersion,
                Map<String, LocalDate> dateByVersion) {
            this.hasMetadata = hasMetadata;
            this.supportedReleases = supportedReleases;
            this.supportedMinors = supportedMinors;
            this.minorByVersion = minorByVersion;
            this.parentByVersion = parentByVersion;
            this.childrenByVersion = childrenByVersion;
            this.dateByVersion = dateByVersion;
        }

        static ReleaseIndex from(Releases releases, LocalDate today) {
            if (releases == null || releases.releases() == null || releases.releases().isEmpty()) {
                return new ReleaseIndex(false, List.of(), Set.of(), Map.of(), Map.of(), Map.of(), Map.of());
            }
            List<Release> supported = new ArrayList<>();
            Set<String> supportedMinors = new HashSet<>();
            Map<String, String> minorByVersion = new HashMap<>();
            Map<String, String> parentByVersion = new HashMap<>();
            Map<String, List<String>> childrenByVersion = new HashMap<>();
            Map<String, LocalDate> dateByVersion = new HashMap<>();

            for (Release r : releases.all()) {
                if (r.upcoming() || r.micros() == null || r.micros().isEmpty()) {
                    continue;
                }
                if (r.communityActive(today)) {
                    supported.add(r);
                    supportedMinors.add(r.version());
                }
                for (Micro m : r.micros()) {
                    if (m.version() == null) {
                        continue;
                    }
                    minorByVersion.put(m.version(), r.version());
                    if (m.releaseDate() != null) {
                        dateByVersion.put(m.version(), m.releaseDate());
                    }
                    if (m.parentRelease() != null) {
                        parentByVersion.put(m.version(), m.parentRelease());
                        childrenByVersion.computeIfAbsent(m.parentRelease(), k -> new ArrayList<>()).add(m.version());
                    }
                }
            }
            return new ReleaseIndex(true, List.copyOf(supported), Set.copyOf(supportedMinors),
                    minorByVersion, parentByVersion, childrenByVersion, dateByVersion);
        }

        ImpactSummary summarize(List<String> affectedList, List<ReleaseRange> rawRanges, LocalDate publishedDate) {
            if (!hasMetadata) {
                return new ImpactSummary(List.of(), List.copyOf(rawRanges), List.of(), 0, 0, false);
            }
            Set<String> affectedSet = new HashSet<>(affectedList);
            Set<String> allCuts = new LinkedHashSet<>();
            for (ReleaseRange rr : rawRanges) {
                allCuts.addAll(rr.cuts());
            }

            int preCount = 0;
            int postCount = 0;
            if (publishedDate != null) {
                for (String ver : affectedList) {
                    LocalDate relDate = dateByVersion.get(ver);
                    if (relDate != null && relDate.isBefore(publishedDate)) {
                        preCount++;
                    } else {
                        postCount++;
                    }
                }
            }
            boolean fixedByDisclosure = publishedDate != null && preCount > 0 && postCount == 0 && !allCuts.isEmpty();

            // 1. Supported branches (community-active minors: latest + active LTS)
            List<SupportedBranchImpact> supportedImpacts = new ArrayList<>();
            for (Release r : supportedReleases) {
                List<String> affectedOnBranch = new ArrayList<>();
                // r.micros() is newest first; collect ascending
                for (int i = r.micros().size() - 1; i >= 0; i--) {
                    String ver = r.micros().get(i).version();
                    if (affectedSet.contains(ver)) {
                        affectedOnBranch.add(ver);
                    }
                }
                if (affectedOnBranch.isEmpty()) {
                    continue;
                }
                List<String> branchFixes = allCuts.stream()
                        .filter(c -> r.version().equals(minorByVersion.get(c)))
                        .sorted(Comparator.comparing(CveList::versionKey))
                        .toList();
                Micro latestFinal = r.latestMicro();
                String tipVersion = latestFinal != null ? latestFinal.version() : r.micros().get(0).version();
                boolean stillAffected = affectedSet.contains(tipVersion);
                supportedImpacts.add(new SupportedBranchImpact(
                        r.version(),
                        r.lts(),
                        affectedOnBranch.get(0),
                        branchFixes,
                        stillAffected));
            }

            // 2. Collapse EOL-only dead-end roots and build unsupported/historical ranges
            List<ReleaseRange> bounded = new ArrayList<>();
            List<ReleaseRange> activeUnfixed = new ArrayList<>();
            List<ReleaseRange> eolDeadEnds = new ArrayList<>();

            for (ReleaseRange rr : rawRanges) {
                boolean touchesUnsupported = pieceTouchesUnsupported(rr, affectedSet);
                if (!touchesUnsupported) {
                    // Purely on currently supported branch(es) — already shown in Supported section
                    continue;
                }
                if (!rr.cuts().isEmpty()) {
                    List<String> sortedCuts = rr.cuts().stream()
                            .sorted(Comparator.comparing(CveList::versionKey))
                            .toList();
                    bounded.add(new ReleaseRange(rr.root(), sortedCuts));
                } else if (pieceTouchesSupported(rr, affectedSet)) {
                    activeUnfixed.add(rr);
                } else {
                    eolDeadEnds.add(rr);
                }
            }

            // Merge EOL-only sibling roots that share a parent_release with a bounded range
            // (e.g. 0.23.0, 0.24.0, 0.25.0, 0.26.0 sharing parent 0.21.0 with 0.27.0 -> fixed in 3.20.6.1...)
            Map<String, Integer> boundedIndexByParent = new HashMap<>();
            for (int i = 0; i < bounded.size(); i++) {
                String p = parentByVersion.get(bounded.get(i).root());
                if (p != null) {
                    boundedIndexByParent.putIfAbsent(p, i);
                }
            }

            List<String> standaloneEolRoots = new ArrayList<>();
            for (ReleaseRange eol : eolDeadEnds) {
                String p = parentByVersion.get(eol.root());
                Integer targetIdx = p != null ? boundedIndexByParent.get(p) : null;
                if (targetIdx != null) {
                    ReleaseRange cur = bounded.get(targetIdx);
                    if (versionKey(eol.root()).compareTo(versionKey(cur.root())) < 0) {
                        bounded.set(targetIdx, new ReleaseRange(eol.root(), cur.cuts()));
                    }
                } else {
                    standaloneEolRoots.add(eol.root());
                }
            }

            List<ReleaseRange> unsupportedRanges = new ArrayList<>(bounded.size() + activeUnfixed.size());
            unsupportedRanges.addAll(bounded);
            unsupportedRanges.addAll(activeUnfixed);
            unsupportedRanges.sort(Comparator.comparing(r -> versionKey(r.root())));

            standaloneEolRoots.sort(Comparator.comparing(CveList::versionKey));

            return new ImpactSummary(
                    List.copyOf(supportedImpacts),
                    List.copyOf(unsupportedRanges),
                    List.copyOf(standaloneEolRoots),
                    preCount,
                    postCount,
                    fixedByDisclosure);
        }

        private boolean pieceTouchesSupported(ReleaseRange rr, Set<String> affectedSet) {
            return pieceTouches(rr, affectedSet, true);
        }

        private boolean pieceTouchesUnsupported(ReleaseRange rr, Set<String> affectedSet) {
            return pieceTouches(rr, affectedSet, false);
        }

        private boolean pieceTouches(ReleaseRange rr, Set<String> affectedSet, boolean wantSupported) {
            Set<String> cuts = new HashSet<>(rr.cuts());
            Set<String> visited = new HashSet<>();
            List<String> stack = new ArrayList<>();
            stack.add(rr.root());
            while (!stack.isEmpty()) {
                String v = stack.remove(stack.size() - 1);
                if (cuts.contains(v) || !visited.add(v)) {
                    continue;
                }
                if (affectedSet.contains(v)) {
                    String minor = minorByVersion.get(v);
                    boolean isSup = minor != null && supportedMinors.contains(minor);
                    if (isSup == wantSupported) {
                        return true;
                    }
                }
                for (String child : childrenByVersion.getOrDefault(v, List.of())) {
                    if (!cuts.contains(child)) {
                        stack.add(child);
                    }
                }
            }
            return false;
        }
    }

    public record ComponentDetail(String component, String range, boolean core, List<String> shipped) {
        public String shippedSummary() {
            return String.join(", ", shipped);
        }
    }

    public record ReleaseRange(String root, List<String> cuts) {
        public String cutsSummary() {
            return String.join(", ", cuts);
        }
    }

    public record SupportedBranchImpact(
            String branch,
            boolean lts,
            String firstAffected,
            List<String> fixReleases,
            boolean stillAffected) {

        public String branchLabel() {
            return lts ? branch + " LTS" : branch;
        }

        public String fixReleasesSummary() {
            return String.join(", ", fixReleases);
        }
    }

    public record Entry(
            String primaryId,
            String cveId,
            String ghsaId,
            String anchorId,
            String secondaryAnchorId,
            String severityLabel,
            String severityKey,
            String publishedIso,
            String publishedDisplay,
            String publishedSortKey,
            String summary,
            boolean pending,
            boolean core,
            List<ComponentDetail> components,
            List<String> affectedReleases,
            List<ReleaseRange> releaseRanges,
            List<SupportedBranchImpact> supportedImpacts,
            List<ReleaseRange> unsupportedRanges,
            List<String> eolUnfixedRoots,
            int preDisclosureCount,
            int postDisclosureCount,
            boolean fixedByDisclosure,
            String cveLink,
            String ghsaLink) {

        public String allAnchorIds() {
            return secondaryAnchorId != null ? anchorId + " " + secondaryAnchorId : anchorId;
        }

        public String searchText() {
            StringBuilder sb = new StringBuilder();
            sb.append(primaryId);
            if (ghsaId != null && !ghsaId.equals(primaryId)) {
                sb.append(' ').append(ghsaId);
            }
            if (!summary.isEmpty()) {
                sb.append(' ').append(summary);
            }
            for (ComponentDetail c : components) {
                sb.append(' ').append(c.component());
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        }

        public String affectedReleasesSummary() {
            return String.join(", ", affectedReleases);
        }

        public String eolUnfixedRootsSummary() {
            return String.join(", ", eolUnfixedRoots);
        }

        public boolean hasUnsupportedImpact() {
            return !unsupportedRanges.isEmpty() || !eolUnfixedRoots.isEmpty();
        }
    }
}
