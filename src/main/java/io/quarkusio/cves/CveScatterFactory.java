package io.quarkusio.cves;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkusio.charts.pie.PieChart;
import io.quarkusio.charts.scatter.ScatterMark;
import io.quarkusio.charts.scatter.ScatterPlot;
import io.quarkusio.charts.scatter.ScatterRow;
import io.quarkusio.charts.timeline.LegendItem;
import io.quarkusio.charts.timeline.TimeScale;
import io.quarkusio.releases.Releases;

import static io.quarkusio.charts.timeline.LegendGroup.MARK;
import static io.quarkusio.charts.timeline.LegendGroup.SEVERITY;

/**
 * Builds the CVE plots shown on /cves/: time on x, artifact on y, severity as colour. Each CVE is a bar from the
 * first release that contained it, through its publication, to the first release without it. CVEs not yet
 * assessed fade in before their publication. Every CVE without a fix runs to today and ends in a cross.
 */
@ApplicationScoped
@Named("cveScatter")
public class CveScatterFactory {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    /** The compact view keeps artifacts with at least this many CVEs. */
    static final int MIN_FOR_TOP_VIEW = 5;
    /** Narrowest bar, in percent of the axis. */
    static final double MIN_BAR_PCT = 0.5;
    /** A pending CVE fades in from this many days before its publication: we do not know when it first shipped. */
    static final int FADE_DAYS = 120;
    static final String QUARKUS_GROUP = "io.quarkus:";

    /**
     * introduced is null when no release in the data contained it; fixed is null while still open. sinceAtLeast is
     * set when it was already in the earliest release the data has, so it may go back further than introduced.
     */
    record Item(String artifact, Severity severity, LocalDate introduced, LocalDate published, LocalDate fixed,
            boolean pending, String cve, String ghsa, String summary, String link, boolean sinceAtLeast) {

        String id() {
            return cve != null ? cve : ghsa;
        }
    }

    private final List<Item> items;
    /** The oldest release line the data covers, e.g. "0.11", or null if there is none. */
    private final String fromVersion;

    private final CveCategories categories;

    CveScatterFactory(Cves data, Lineage lineage) {
        this(data, lineage, new CveCategories());
    }

    @Inject
    public CveScatterFactory(Cves data, Releases releases, CveCategories categories) {
        this(data, Lineage.of(releases), categories);
    }

    CveScatterFactory(Cves data, Lineage lineage, CveCategories categories) {
        this.categories = categories;
        this.items = items(data, lineage);
        String oldest = lineage.horizonRelease(scanned(data, lineage));
        this.fromVersion = oldest == null ? null : oldest.replaceFirst("^(\\d+\\.\\d+).*", "$1");
    }

    public List<LegendItem> legend() {
        List<LegendItem> legend = new ArrayList<>();
        for (Severity s : List.of(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW)) {
            legend.add(new LegendItem(SEVERITY, s.key(), s.label(), ""));
        }
        legend.add(new LegendItem(MARK, "span", "Fixed",
                "Bar from the first release that contained it to the first release without it. The paler part is before it was published."));
        if (items.stream().anyMatch(Item::sinceAtLeast)) {
            legend.add(new LegendItem(MARK, "since", "Since at least",
                    "Pale part ending in an arrow: it was already in the earliest release we have data for"
                            + (fromVersion == null ? "" : " (" + fromVersion + ")") + ", so it may go back further."));
        }
        legend.add(new LegendItem(MARK, "open", "Not fixed yet",
                "Bar ending in a cross at the today line: no release is without it yet. In the Discovery view, the cross takes the place of the dot."));
        legend.add(new LegendItem(MARK, "pending", "Pending",
                "Bar fading in from the left: published, but not yet assessed against Quarkus releases, so we do not know since when it affected them. Not fixed yet either, so it ends in a cross."));
        return legend;
    }

    /** Number of distinct CVEs plotted, published and pending. */
    public long advisoryCount() {
        return items.stream().map(Item::id).distinct().count();
    }

    public long pendingCount() {
        return items.stream().filter(Item::pending).map(Item::id).distinct().count();
    }

    public long artifactCount() {
        return items.stream().map(Item::artifact).distinct().count();
    }

    /** The fewest CVEs an artifact needs to be shown, before the slider is moved. */
    public int minForTopView() {
        return MIN_FOR_TOP_VIEW;
    }

    /** The most CVEs any artifact has: the top of the slider. */
    public int maxCount() {
        return Math.max(1, items.stream().collect(java.util.stream.Collectors.groupingBy(Item::artifact,
                java.util.stream.Collectors.counting())).values().stream().mapToInt(Long::intValue).max().orElse(1));
    }

    /** Where the slider starts: never beyond its top, for small data. */
    public int defaultMin() {
        return Math.min(MIN_FOR_TOP_VIEW, maxCount());
    }

    /** Components by number of CVEs, as a ring with a handle that works like the slider. */
    public PieChart pie() {
        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(Item::artifact, Collectors.counting()));
        return PieChart.of("components", counts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new PieChart.Input(e.getKey(), e.getKey(), e.getValue().intValue(),
                        categories.origin(e.getKey()),
                        Map.of("origin", categories.origin(e.getKey()), "category", categories.category(e.getKey()))))
                .toList());
    }

    /** The categories that have a component, for the buttons that pick one. */
    public List<String> categoryNames() {
        Set<String> used = items.stream().map(i -> categories.category(i.artifact())).collect(Collectors.toSet());
        return categories.names().stream().filter(used::contains).toList();
    }

    /** The exposure view: every artifact, with those that have few CVEs hidden until the slider brings them in. */
    public ScatterPlot plot(LocalDateTime now) {
        return build("exposure", now.toLocalDate(), defaultMin());
    }

    /** The discovery view: the same rows, with a dot (or a cross, if not fixed yet) on each publication date. */
    public ScatterPlot dotPlot(LocalDateTime now) {
        return build("discovered", now.toLocalDate(), defaultMin(), true);
    }

    ScatterPlot build(String id, LocalDate today, int minShown) {
        return build(id, today, minShown, false);
    }

    /** With dots, a mark is a point on the publication date instead of a bar from first release to fix. */
    ScatterPlot build(String id, LocalDate today, int minShown, boolean dots) {
        Map<String, List<Item>> byArtifact = new LinkedHashMap<>();
        items.forEach(i -> byArtifact.computeIfAbsent(i.artifact(), k -> new ArrayList<>()).add(i));
        List<Map.Entry<String, List<Item>>> all = byArtifact.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, List<Item>>>comparingInt(e -> -e.getValue().size())
                        .thenComparing(Map.Entry::getKey))
                .toList();
        List<Map.Entry<String, List<Item>>> shown = all.stream().filter(e -> e.getValue().size() >= minShown).toList();

        // The span is set by what is shown, not by the data as a whole: from the start of the month before
        // the earliest start of a bar (the paler part included) to the end of the month after next, so the
        // "today" tag, which is centred on today's line, is not clipped by the right edge. The page script
        // does the same when the slider brings in more artifacts.
        LocalDate first = shown.stream().flatMap(e -> e.getValue().stream()).map(i -> barStart(i, dots))
                .min(LocalDate::compareTo).orElse(today);
        LocalDate end = today.plusMonths(2);
        TimeScale scale = new TimeScale(first.minusMonths(1).withDayOfMonth(1), end.withDayOfMonth(end.lengthOfMonth()));

        List<ScatterRow> rows = all.stream()
                .map(e -> row(e.getKey(), e.getValue(), scale, today, e.getValue().size() >= minShown, dots)).toList();
        return new ScatterPlot(id, scale, rows, legend());
    }

    private record Placed(Item item, LocalDate from, LocalDate to) {
    }

    private ScatterRow row(String artifact, List<Item> artifactItems, TimeScale scale, LocalDate today,
            boolean shown, boolean dots) {
        List<Placed> placed = artifactItems.stream().map(i -> place(i, today, dots))
                .sorted(Comparator.comparing(Placed::from).thenComparing(p -> p.item().published())
                        .thenComparing(p -> p.item().id()))
                .toList();

        // first fit: the first sub-lane that is free by the time this mark starts
        List<LocalDate> laneEnds = new ArrayList<>();
        int[] laneOf = new int[placed.size()];
        for (int i = 0; i < placed.size(); i++) {
            Placed p = placed.get(i);
            int lane = -1;
            for (int l = 0; l < laneEnds.size(); l++) {
                if (laneEnds.get(l).plusDays(ScatterRow.LANE_GAP_DAYS).isBefore(p.from())) {
                    lane = l;
                    break;
                }
            }
            if (lane < 0) {
                laneEnds.add(p.to());
                lane = laneEnds.size() - 1;
            } else {
                laneEnds.set(lane, p.to());
            }
            laneOf[i] = lane;
        }

        int lanes = laneEnds.size();
        int height = ScatterRow.heightFor(lanes);
        List<ScatterMark> marks = new ArrayList<>();
        for (int i = 0; i < placed.size(); i++) {
            marks.add(mark(placed.get(i), laneOf[i], lanes, height, scale, today, dots));
        }
        int colon = artifact.indexOf(':');
        String shortLabel = colon < 0 ? artifact : artifact.substring(colon + 1);
        return new ScatterRow(artifact, artifact, shortLabel, marks, lanes, shown,
                Map.of("origin", categories.origin(artifact), "category", categories.category(artifact)));
    }

    /** The span of time a mark occupies, for packing into sub-lanes. */
    private static Placed place(Item i, LocalDate today, boolean dots) {
        if (dots) {
            // a dot or cross is centred on its date and about as wide as a cross
            return new Placed(i, i.published().minusDays(ScatterRow.CROSS_HALF_DAYS),
                    i.published().plusDays(ScatterRow.CROSS_HALF_DAYS));
        }
        // the cross at the end of an open bar sticks out past it
        return new Placed(i, barStart(i, false), barEnd(i, today).plusDays(i.fixed() == null ? ScatterRow.CROSS_HALF_DAYS : 0));
    }

    private static LocalDate barStart(Item i, boolean dots) {
        if (dots) {
            return i.published();
        }
        if (i.pending()) {
            return i.published().minusDays(FADE_DAYS);
        }
        return i.introduced() == null || i.introduced().isAfter(i.published()) ? i.published() : i.introduced();
    }

    private static LocalDate barEnd(Item i, LocalDate today) {
        return i.fixed() == null ? today : i.fixed();
    }

    private ScatterMark mark(Placed p, int lane, int lanes, int height, TimeScale scale, LocalDate today,
            boolean dots) {
        Item i = p.item();
        int y = ScatterRow.barY(lane, height, lanes);
        int cy = y + ScatterRow.BAR_H / 2;
        String title = i.id() + " · " + i.severity().label();
        LocalDate start = barStart(i, dots);
        LocalDate end = dots ? i.published() : barEnd(i, today);
        double sx = scale.percent(start);
        double fx = scale.percent(i.published());
        double ex = scale.percent(end);
        double pre = Math.max(0, fx - sx);
        double post = Math.max(MIN_BAR_PCT, ex - Math.max(fx, sx));

        List<String> details = new ArrayList<>();
        details.add(nonNull(i.summary()));
        details.add(i.artifact());
        if (!dots && !i.pending() && start.isBefore(i.published())) {
            details.add(i.sinceAtLeast()
                    ? "In releases since at least " + DATE.format(start) + ", the earliest we have data for"
                            + (fromVersion == null ? "" : " (" + fromVersion + ")") + ", "
                            + days(start, i.published()) + " before it was published"
                    : "In releases since " + DATE.format(start) + ", " + days(start, i.published())
                            + " before it was published");
        }
        details.add("Published " + DATE.format(i.published()));
        if (i.pending()) {
            details.add("Pending: not yet assessed against Quarkus releases, open for " + days(i.published(), today));
        } else if (i.fixed() == null) {
            details.add("Not fixed yet: open for " + days(i.published(), today));
        } else {
            details.add("First release without it: " + DATE.format(i.fixed()) + ", " + days(i.published(), i.fixed())
                    + " after it was published");
        }
        return new ScatterMark(i.severity().key(), i.fixed() == null, i.pending(), i.sinceAtLeast() && !dots, dots, lane, start,
                i.published(), end,
                fmt(sx), fmt(pre), fmt(sx + pre), fmt(post), fmt(ex), y, cy, i.link(), title, details);
    }

    private static String days(LocalDate from, LocalDate to) {
        long d = Math.max(0, ChronoUnit.DAYS.between(from, to));
        return d + (d == 1 ? " day" : " days");
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static String normalise(String artifact) {
        return artifact.startsWith(QUARKUS_GROUP) ? artifact.substring(QUARKUS_GROUP.length()) : artifact;
    }

    private static String link(String cve, String ghsa) {
        return cve != null ? "https://www.cve.org/CVERecord?id=" + cve : "https://github.com/advisories/" + ghsa;
    }

    private static LocalDate date(String s) {
        return s == null || s.length() < 10 ? null : LocalDate.parse(s.substring(0, 10));
    }

    /** The releases that were scanned for advisories; every release we have dates for when the data does not say. */
    private static java.util.Collection<String> scanned(Cves data, Lineage lineage) {
        return data == null || data.releases() == null ? lineage.dates().keySet()
                : data.releases().stream().map(Cves.Scanned::version).toList();
    }

    /**
     * One item per CVE per affected artifact (the data lists a component once per dependency version).
     * <p>
     * A CVE was in Quarkus from the earliest affected release. It was fixed in the first release, on or after its
     * publication, that is built on an affected release but is not affected itself. One that the newest release
     * still has is open. The earliest release we know of bounds "since".
     */
    static List<Item> items(Cves data, Lineage lineage) {
        List<Item> out = new ArrayList<>();
        if (data == null) {
            return out;
        }
        LocalDate horizon = lineage.horizon(scanned(data, lineage));
        String newest = lineage.newest();
        if (data.advisories() != null) {
            data.advisories().forEach((ghsa, a) -> {
                Severity sev = Severity.parse(a.severity());
                LocalDate published = date(a.published());
                if (sev == null || published == null || a.affectedComponents() == null) {
                    return;
                }
                Set<String> affected = lineage.affected(a.affectedReleases());
                LocalDate since = affected.stream().map(lineage::date).filter(Objects::nonNull)
                        .min(LocalDate::compareTo).orElse(null);
                LocalDate fixed = null;
                if (!affected.contains(newest)) {
                    fixed = lineage.firstFixAtOrAfter(affected, published);
                    if (fixed == null) {
                        fixed = published; // gone from every release before it was published
                    }
                }
                LocalDate fixedAt = fixed;
                boolean sinceAtLeast = since != null && since.equals(horizon) && since.isBefore(published);
                a.affectedComponents().stream().map(Cves.Component::component).filter(Objects::nonNull)
                        .map(CveScatterFactory::normalise).distinct()
                        .forEach(artifact -> out.add(new Item(artifact, sev, since, published, fixedAt,
                                false, a.cve(), ghsa, a.summary(), link(a.cve(), ghsa), sinceAtLeast)));
            });
        }
        if (data.pending() != null) {
            for (Cves.Pending p : data.pending()) {
                Severity sev = Severity.parse(p.severity());
                LocalDate published = date(p.published());
                if (sev == null || published == null || p.artifacts() == null) {
                    continue;
                }
                String link = p.link() != null ? p.link() : link(p.id(), null);
                p.artifacts().stream().filter(x -> x != null).map(CveScatterFactory::normalise).distinct()
                        .forEach(artifact -> out.add(new Item(artifact, sev, null, published, null, true, p.id(),
                                null, p.summary(), link, false)));
            }
        }
        return out;
    }
}
