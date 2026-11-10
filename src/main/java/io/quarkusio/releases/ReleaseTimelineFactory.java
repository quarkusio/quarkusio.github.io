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

    private final ReleaseCatalog catalog;

    @Inject
    public ReleaseTimelineFactory(ReleaseCatalog catalog) {
        this.catalog = catalog;
    }

    public List<LegendItem> legend() {
        List<LegendItem> items = new ArrayList<>(List.of(
                new LegendItem(RELEASE_KIND, "community", "Community maintenance",
                        "Solid bar: bug fixes and security patches from the Quarkus community."),
                new LegendItem(RELEASE_KIND,"enterprise", "Enterprise support",
                        "Bordered bar: paid support with an end date, from Red Hat and IBM."),
                new LegendItem(RELEASE_KIND,"nes", "Never-ending support",
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

    /** From the earliest year that still has support running, to the last support end. */
    public Timeline supportView(LocalDate today) {
        int startYear = catalog.all().stream()
                .filter(r -> !r.upcoming() && r.supportEnd() != null && r.supportEnd().isAfter(today))
                .map(r -> r.releaseDate().getYear())
                .min(Integer::compare).orElse(today.getYear());
        return build("support", startYear, today);
    }

    /** The same compact rows over the whole history, back to the first release. */
    public Timeline fullView(LocalDate today) {
        LocalDate first = catalog.all().stream().filter(r -> r.releaseDate() != null)
                .map(Release::releaseDate).min(LocalDate::compareTo).orElse(today);
        return build("all", first.getYear(), today);
    }

    private Timeline build(String id, int startYear, LocalDate today) {
        TimeScale scale = new TimeScale(LocalDate.of(startYear, 1, 1), LocalDate.of(endYear(today), 12, 31));
        GanttTimelineBuilder b = newBuilder(id, scale);
        List<Release> shown = catalog.all().stream()
                .filter(r -> !r.upcoming() && (r.lts() || r.hasVendorSupport() || r.hasNes()))
                .toList();
        // the non-LTS stream is the second row, after the first release row
        int streamAfter = Math.min(1, shown.size());
        if (streamAfter == 0) {
            addStreamLane(b, today);
        }
        for (int i = 0; i < shown.size(); i++) {
            addReleaseRow(b, shown.get(i), today);
            if (i == streamAfter - 1) {
                addStreamLane(b, today);
            }
        }
        return b.build();
    }

    /** The default length of an upcoming bar: the policy's release cadence. */
    private Period cadence() {
        return catalog.cadence();
    }

    /** Covers every support end and every visible upcoming bar, so planned releases are not cropped. */
    private int endYear(LocalDate today) {
        int supportEnds = catalog.all().stream().map(Release::supportEnd).filter(d -> d != null)
                .map(LocalDate::getYear).max(Integer::compare).orElse(today.getYear());
        int upcomingEnds = catalog.visibleUpcoming().stream()
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
     * Ends at the release's derived plannedEndDate. Without one, or if that is not after the start,
     * an LTS gets the policy's LTS duration and anything else the default bar length.
     */
    private LocalDate upcomingEnd(Release r, LocalDate today) {
        LocalDate start = upcomingStart(r, today);
        if (r.plannedEndDate() != null && r.plannedEndDate().isAfter(start)) {
            return r.plannedEndDate();
        }
        Period lts = catalog.policy() == null ? null : catalog.policy().ltsSupportPeriod();
        return start.plus(r.lts() && lts != null ? lts : cadence());
    }

    private void addUpcoming(LocalDate today, GanttTimelineBuilder.RowBuilder row) {
        boolean ltsLength = catalog.policy() != null && catalog.policy().ltsSupportPeriod() != null;
        for (Release r : catalog.visibleUpcoming()) {
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
        List<Release> stream = catalog.all().stream()
                .filter(r -> !r.upcoming() && !r.lts() && r.releaseDate() != null)
                .sorted(Comparator.comparing(Release::releaseDate))
                .toList();
        for (Release r : stream) {
            row.segment("community", r.releaseDate(), r.eolDate(), communityHref(r, today),
                    "Quarkus " + r.version(), communityLines(r), r.version());
        }
        addUpcoming(today, row);

        row.add();
    }

    private void addReleaseRow(GanttTimelineBuilder b, Release r, LocalDate today) {
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

        row.segment("community", r.releaseDate(), r.eolDate(), communityHref(r, today), name,
                communityLines(r), r.version());
        if (r.eolDate() != null) {
            if (later(r.rhbqEol(), r.eolDate())) {
                int lane = row.lane("Red Hat");
                row.segment("enterprise", "rhbq", lane, r.eolDate(), r.rhbqEol(), catalog.providerUrl("rhbq"),
                        name + ": Red Hat build of Quarkus",
                        List.of("Red Hat support until " + DATE.format(r.rhbqEol())), r.version());
            }
            if (r.ibmEol() != null) {
                int lane = row.lane("IBM");
                if (r.ibmEol().isAfter(r.eolDate())) {
                    row.segment("enterprise", "ibm", lane, r.eolDate(), r.ibmEol(), catalog.providerUrl("ibm"),
                            name + ": IBM Enterprise Build of Quarkus",
                            List.of("IBM support until " + DATE.format(r.ibmEol())), r.version());
                }
                LocalDate from = r.ibmEol().isAfter(r.eolDate()) ? r.ibmEol() : r.eolDate();
                row.segment("nes", "ibm", lane, from, null, catalog.providerUrl("ibm"),
                        name + ": IBM never-ending support",
                        List.of("IBM never-ending support after " + DATE.format(from), "No end date"), r.version());
            }
            if (r.hasNes()) {
                int lane = row.lane("HeroDevs");
                LocalDate from = r.supportEnd();
                row.segment("nes", "herodevs", lane, from, r.herodevsEnd(), catalog.providerUrl("herodevs"),
                        name + ": HeroDevs never-ending support",
                        List.of(r.herodevsEnd() == null
                                ? "HeroDevs never-ending support after " + DATE.format(from)
                                : "HeroDevs support until " + DATE.format(r.herodevsEnd()),
                                r.herodevsEnd() == null ? "No end date" : "Ends " + DATE.format(r.herodevsEnd())),
                        r.version());
            }
        }
        row.add();
    }

    private static boolean later(LocalDate d, LocalDate than) {
        return d != null && d.isAfter(than);
    }

    private String communityHref(Release r, LocalDate today) {
        if (r.eolDate() == null || r.eolDate().isAfter(today)) {
            return "https://code.quarkus.io/?S=" + r.version();
        }
        return r.link();
    }

    private String enterpriseUrl(Release r) {
        return catalog.providerUrl(r.rhbqEol() != null ? "rhbq" : "ibm");
    }

    private List<String> communityLines(Release r) {
        List<String> lines = new ArrayList<>();
        lines.add("Released " + DATE.format(r.releaseDate()));
        if (r.eolDate() == null) {
            lines.add("Community maintenance: current release");
        } else {
            lines.add("Community maintenance until " + DATE.format(r.eolDate()));
        }
        if (r.latestPatch() != null) {
            lines.add("Latest micro " + r.latestPatch()
                    + (r.latestPatchDate() == null ? "" : " (" + DATE.format(r.latestPatchDate()) + ")"));
        }
        return lines;
    }
}
