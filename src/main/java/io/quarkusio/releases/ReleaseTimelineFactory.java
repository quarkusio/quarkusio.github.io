package io.quarkusio.releases;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkusio.charts.timeline.LegendItem;
import io.quarkusio.charts.timeline.TimeScale;
import io.quarkusio.charts.timeline.Timeline;
import io.quarkusio.charts.timeline.gantt.GanttTimelineBuilder;

import static io.quarkusio.charts.timeline.LegendGroup.PROVIDER;
import static io.quarkusio.charts.timeline.LegendGroup.RELEASE_KIND;
import static io.quarkusio.releases.ReleaseKind.MAJOR;

/** Builds the two release support timelines shown on /releases/. */
@ApplicationScoped
@Named("releaseTimeline")
public class ReleaseTimelineFactory {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    /** Used for the default upcoming bar when the policy has no release_cadence. */

    private final Releases catalog;
    private final int maxUpcoming;

    @Inject
    public ReleaseTimelineFactory(Releases catalog, ReleasesConfig config) {
        this(catalog, config.maxUpcoming());
    }

    public ReleaseTimelineFactory(Releases catalog) {
        this(catalog, Releases.DEFAULT_MAX_UPCOMING);
    }

    public ReleaseTimelineFactory(Releases catalog, int maxUpcoming) {
        this.catalog = catalog;
        this.maxUpcoming = maxUpcoming;
    }

    public List<LegendItem> legend() {
        List<LegendItem> items = new ArrayList<>(List.of(
                new LegendItem(RELEASE_KIND, "community", "Community maintenance",
                        "Solid bar: bug fixes and security patches from the Quarkus community."),
                new LegendItem(RELEASE_KIND,"enterprise", "Enterprise support",
                        "Bordered bar: paid support with an end date, from Red Hat and IBM."),
                new LegendItem(RELEASE_KIND, "open-ended", "EOL Patches",
                        "Fading bar: paid patches with no end date, from IBM and HeroDevs.")));
        if (catalog.all().stream().anyMatch(Release::upcoming)) {
            items.add(new LegendItem(RELEASE_KIND, "upcoming", "Upcoming", "Planned release."));
        }
        items.add(new LegendItem(PROVIDER, "rhbq", "Red Hat", "Red Hat build of Quarkus"));
        items.add(new LegendItem(PROVIDER,"ibm", "IBM", "IBM Enterprise Build of Quarkus"));
        items.add(new LegendItem(PROVIDER,"herodevs", "HeroDevs", "HeroDevs Never-Ending Support"));
        return items;
    }

    /** Roq's {@code now} global is a LocalDateTime. */
    public Timeline supportView(LocalDateTime now) {
        return supportView(now.toLocalDate());
    }

    public Timeline fullView(LocalDateTime now) {
        return fullView(now.toLocalDate());
    }

    /**
     * Only releases with a support offering running on the build date. The axis starts at the beginning of the
     * month of the earliest bar that is drawn in full; open-ended support bars run off the left edge, so
     * they do not push it back.
     */
    public Timeline supportView(LocalDate today) {
        List<Release> shown = shown(today, true);
        LocalDate start = shown.stream().map(r -> earliestFullBar(r, today)).filter(d -> d != null)
                .min(LocalDate::compareTo).orElse(LocalDate.of(today.getYear(), 1, 1))
                .withDayOfMonth(1);
        return build("support", start, today, true, shown);
    }

    /** The same compact rows over the whole history, back to the first release. */
    public Timeline fullView(LocalDate today) {
        LocalDate first = catalog.all().stream().map(Release::releaseDate)
                .filter(localDate -> localDate!= null).min(LocalDate::compareTo).orElse(today);
        return build("all", LocalDate.of(first.getYear(), 1, 1), today, false, shown(today, false));
    }

    private List<Release> shown(LocalDate today, boolean activeOnly) {
        return catalog.all().stream()
                .filter(r -> !r.upcoming() && (r.lts() || r.hasVendorSupport() || r.hasOpenEndedSupport())
                        && (!activeOnly || r.supportActive(today)))
                .toList();
    }

    /**
     * In the support view, the community bar is only worth drawing while the community or a dated enterprise
     * offering (Red Hat, or IBM before its open-ended support) is still running. A release kept alive only by
     * open-ended support keeps its row, with just that bar.
     */
    private static boolean communityBarShown(Release r, LocalDate today, boolean activeOnly) {
        return !activeOnly || r.communityActive(today) || r.anyOfferingActiveOn(today);
    }

    /** Start of the earliest bar, branch or connector drawn for the release in the support view; never-ending bars are excluded. */
    private LocalDate earliestFullBar(Release r, LocalDate today) {
        LocalDate earliest = null;
        if (communityBarShown(r, today, true)) {
            earliest = earlier(earliest, r.releaseDate());
            Release from = catalog.find(r.branchedFromVersion()).orElse(null);
            if (r.lts() && from != null && !from.lts()) {
                earliest = earlier(earliest, from.releaseDate());
            }
        }
        if (r.eolDate() != null) {
            for (EnterpriseOffering vendor : new EnterpriseOffering[] { r.rhbq(), r.ibm() }) {
                if (enterpriseBar(r, vendor)) {
                    earliest = earlier(earliest, enterpriseStart(r, vendor));
                    earliest = earlier(earliest, catalog.branchPoint(r, vendor));
                }
            }
        }
        return earliest;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return a == null ? b : b == null ? a : b.isBefore(a) ? b : a;
    }

    private Timeline build(String id, LocalDate start, LocalDate today, boolean activeOnly, List<Release> shown) {
        TimeScale scale = new TimeScale(start, LocalDate.of(endYear(today), 12, 31));
        GanttTimelineBuilder b = newBuilder(id, scale);
        // the non-LTS stream is the top row, with the LTS rows below it
        addStreamLane(b, today);
        for (Release r : shown) {
            addReleaseRow(b, r, today, communityBarShown(r, today, activeOnly));
        }
        addBranches(b, shown, today, activeOnly);
        return b.build();
    }

    /**
     * Draws branched_from: an LTS from the non-LTS release before it (straight down from the left edge of
     * the stream bar it branched from, then into the LTS bar horizontally), and a non-LTS that follows a gap in the stream, where an LTS took the slot, from
     * that LTS (rising from the left of its bar, an S-curve and a rise into the gap, then into the non-LTS bar horizontally).
     * LTS releases that are on the chart are only connected while a support offering is running on the build date,
     * to keep it calm; the ones with no row run off the bottom.
     * Non-LTS releases with no gap between them are joined with a short horizontal line.
     */
    private void addBranches(GanttTimelineBuilder b, List<Release> shown, LocalDate today, boolean activeOnly) {
        for (Release r : catalog.all()) {
            if (r.upcoming() || !r.lts() || r.releaseDate() == null) {
                continue;
            }
            boolean onChart = shown.contains(r);
            if (onChart && (!r.supportActive(today) || !communityBarShown(r, today, activeOnly))) {
                continue;
            }
            // an LTS with no row of its own is drawn as lines vanishing off the bottom of the chart
            String row = onChart ? r.version() : GanttTimelineBuilder.BOTTOM;
            Release from = catalog.find(r.branchedFromVersion()).orElse(null);
            if (from != null && !from.lts() && from.releaseDate() != null) {
                // leaves the left edge of the release it branches from, drops, then turns to enter the LTS bar horizontally
                b.link("stream", row, from.releaseDate(), r.releaseDate());
            }
            for (Release next : catalog.all()) {
                if (!next.upcoming() && !next.lts() && next.releaseDate() != null
                        && r.version().equals(next.branchedFromVersion())) {
                    // rises from the left of the LTS bar, like the vendor connectors, and S-curves over to the non-LTS bar
                    b.rise(row, "stream", r.releaseDate(), next.releaseDate());
                }
            }
        }
        List<Release> stream = streamReleases();
        for (int i = 1; i < stream.size(); i++) {
            Release previous = stream.get(i - 1);
            if (previous.eolDate() != null && !previous.eolDate().isBefore(stream.get(i).releaseDate())) {
                b.chain("stream", stream.get(i).releaseDate());
            }
        }
    }

    private List<Release> streamReleases() {
        return catalog.all().stream()
                .filter(r -> !r.upcoming() && !r.lts() && r.releaseDate() != null)
                .sorted(Comparator.comparing(Release::releaseDate))
                .toList();
    }

    /** The default length of an upcoming bar: the policy's release cadence. */
    private Period cadence() {
        return catalog.cadence();
    }

    /** Covers every support end and every visible upcoming bar, so planned releases are not cropped. */
    private int endYear(LocalDate today) {
        int supportEnds = catalog.all().stream().map(Release::supportEnd).filter(d -> d != null)
                .map(LocalDate::getYear).max(Integer::compare).orElse(today.getYear());
        int upcomingEnds = catalog.visibleUpcoming(maxUpcoming).stream()
                .map(r -> upcomingEnd(r, today).getYear())
                .max(Integer::compare).orElse(today.getYear());
        return Math.max(Math.max(supportEnds, upcomingEnds), today.getYear());
    }

    private GanttTimelineBuilder newBuilder(String id, TimeScale scale) {
        GanttTimelineBuilder b = new GanttTimelineBuilder(id, scale);
        for (Release major : catalog.majors()) {
            b.marker(major.releaseDate(), "Quarkus " + major.version().replaceAll("\\.0$", ""), MAJOR);
        }
        for (LegendItem item : legend()) {
            b.legend(item);
        }
        return b;
    }

    /**
     * Ends at the release's planned end date. Without one, or if that is not after the start,
     * an LTS gets the policy's LTS duration and anything else the default bar length.
     */
    private LocalDate upcomingEnd(Release r, LocalDate today) {
        LocalDate start = upcomingStart(r, today);
        LocalDate planned = catalog.plannedEndDate(r);
        if (planned != null && planned.isAfter(start)) {
            return planned;
        }
        Period lts = catalog.policy() == null ? null : catalog.policy().ltsSupportPeriod();
        return start.plus(r.lts() && lts != null ? lts : cadence());
    }

    private void addUpcoming(LocalDate today, GanttTimelineBuilder.RowBuilder row) {
        boolean ltsLength = catalog.policy() != null && catalog.policy().ltsSupportPeriod() != null;
        for (Release r : catalog.visibleUpcoming(maxUpcoming)) {
            List<String> lines = new ArrayList<>();
            lines.add(isPlanned(r, today) ? "Planned release " + DATE.format(r.plannedDate()) : "Upcoming release");
            if (r.lts() && ltsLength) {
                lines.add("LTS: community maintenance for " + catalog.policy().ltsSupportDuration().label());
            }
            if (r.lts()) {
                // data only: a visible LTS badge would mislabel the non-LTS stream row
                row.data("lts", "true");
            }
            row.segment("upcoming", upcomingStart(r, today), upcomingEnd(r, today), null, "Quarkus " + r.version() + " (upcoming)", lines, r.version());
        }
    }

    private static boolean isPlanned(Release r, LocalDate today) {
        return r.plannedDate() != null && r.plannedDate().isAfter(today);
    }

    private static LocalDate upcomingStart(Release r, LocalDate today) {
        return isPlanned(r, today) ? r.plannedDate() : today;
    }

    /** Non-LTS minors laid end to end; each is supported until the next one ships. */
    private void addStreamLane(GanttTimelineBuilder b, LocalDate today) {
        var row = b.row("stream", "Non-LTS stream").shortLabel("Non-LTS");
        for (Release r : streamReleases()) {
            row.segment("community", r.releaseDate(), r.eolDate(), communityHref(r, today),
                    "Quarkus " + r.version(), communityLines(r), r.version());
        }
        addUpcoming(today, row);

        row.add();
    }

    private void addReleaseRow(GanttTimelineBuilder b, Release r, LocalDate today, boolean communityBar) {
        var row = b.row(r.version(), r.version())
                .href(r.link())
                .data("version", r.version());
        if (r.lts()) {
            row.badge("LTS").data("lts", "true");
        }
        if (r.eolDate() != null) {
            row.data("eol", r.eolDate().toString());
        }
        if (r.vendorEnd() != null && r.vendorEnd().isAfter(today)) {
            row.data("supported", "true");
        }
        String name = "Quarkus " + r.version();

        if (communityBar) {
            row.segment("community", r.releaseDate(), r.eolDate(), communityHref(r, today), name,
                    communityLines(r), r.version());
        }
        if (r.eolDate() != null) {
            if (enterpriseBar(r, r.rhbq())) {
                int lane = row.lane("Red Hat");
                LocalDate from = enterpriseStart(r, r.rhbq());
                row.segment("enterprise", "rhbq", lane, from, r.rhbq().eolDate(), catalog.providerUrl("rhbq"),
                        name + ": Red Hat build of Quarkus",
                        enterpriseLines("Red Hat", r.rhbq().releaseDate(), r.rhbq().eolDate()), r.version());
                row.branch(lane, catalog.branchPoint(r, r.rhbq()));
            }
            if (r.ibm() != null && r.ibm().hasEndDate()) {
                int lane = row.lane("IBM");
                if (enterpriseBar(r, r.ibm())) {
                    LocalDate from = enterpriseStart(r, r.ibm());
                    row.segment("enterprise", "ibm", lane, from, r.ibm().eolDate(), catalog.providerUrl("ibm"),
                            name + ": IBM Enterprise Build of Quarkus",
                            enterpriseLines("IBM", r.ibm().releaseDate(), r.ibm().eolDate()), r.version());
                    row.branch(lane, catalog.branchPoint(r, r.ibm()));
                }
                if (r.ibm().openEnded()) {
                    openEndedSegment(row, lane, "ibm", "IBM open-ended support", r.openEndedStart(r.ibm()), name,
                            r.version());
                }
            }
            if (r.herodevs() != null && r.herodevs().openEnded()) {
                openEndedSegment(row, row.lane("HeroDevs"), "herodevs", "HeroDevs never-ending support",
                        r.openEndedStart(r.herodevs()), name, r.version());
            }
        }
        row.add();
    }

    /** Draws a bar for support with no end date, e.g. "IBM open-ended support", starting on from. */
    private void openEndedSegment(GanttTimelineBuilder.RowBuilder row, int lane, String provider, String description,
            LocalDate from, String name, String version) {
        row.segment("open-ended", provider, lane, from, null, catalog.providerUrl(provider), name + ": " + description,
                List.of(description + " after " + DATE.format(from), "No end date"), version);
    }

    /**
     * Whether a vendor's bar adds anything to the community bar above it. With a vendor release date the bar
     * runs from that date to the vendor's end, layered under the community bar, and is dropped only when it
     * would be identical to it (same start and end). Without one, as before, the bar only covers the time
     * after community maintenance and is dropped unless the vendor supports the release for longer.
     */
    private static boolean enterpriseBar(Release r, EnterpriseOffering vendor) {
        if (vendor == null || !vendor.hasEndDate()) {
            return false;
        }
        if (vendor.releaseDate() == null) {
            return vendor.eolDate().isAfter(r.eolDate());
        }
        return !(vendor.releaseDate().equals(r.releaseDate()) && vendor.eolDate().equals(r.eolDate()));
    }

    private static LocalDate enterpriseStart(Release r, EnterpriseOffering vendor) {
        return vendor.releaseDate() != null ? vendor.releaseDate() : r.eolDate();
    }

    private static List<String> enterpriseLines(String vendor, LocalDate release, LocalDate end) {
        List<String> lines = new ArrayList<>();
        if (release != null) {
            lines.add(vendor + " release " + DATE.format(release));
        }
        lines.add(vendor + " support until " + DATE.format(end));
        return lines;
    }

    private String communityHref(Release r, LocalDate today) {
        if (r.eolDate() == null || r.eolDate().isAfter(today)) {
            return "https://code.quarkus.io/?S=" + r.version();
        }
        return r.link();
    }

    private List<String> communityLines(Release r) {
        List<String> lines = new ArrayList<>();
        lines.add("Released " + DATE.format(r.releaseDate()));
        if (r.eolDate() == null) {
            lines.add("Community maintenance: current release");
        } else {
            lines.add("Community maintenance until " + DATE.format(r.eolDate()));
        }
        Micro latest = r.latestMicro();
        if (latest != null) {
            lines.add("Latest micro " + latest.version()
                    + (latest.releaseDate() == null ? "" : " (" + DATE.format(latest.releaseDate()) + ")"));
        }
        return lines;
    }
}
