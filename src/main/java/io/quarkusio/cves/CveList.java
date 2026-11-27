package io.quarkusio.cves;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * View model for {@code /cves/all/}: every evaluated advisory and pending CVE from {@code _data/cves.yaml},
 * sorted newest-first by publication date, with stable lowercase anchor ids for both CVE and GHSA ids.
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
    public CveList(Cves cves) {
        List<Entry> built = build(cves);
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
        List<Entry> out = new ArrayList<>();
        if (cves == null) {
            return List.of();
        }
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

                List<String> releases = a.affectedReleases() != null && a.affectedReleases().list() != null
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
                        releases,
                        List.copyOf(releaseRanges),
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
    }
}
