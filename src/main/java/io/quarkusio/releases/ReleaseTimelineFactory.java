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

import io.quarkusio.timeline.LegendItem;
import io.quarkusio.timeline.TimeScale;
import io.quarkusio.timeline.Timeline;
import io.quarkusio.timeline.TimelineBuilder;

/** Builds the two release support timelines shown on /releases/. */
@ApplicationScoped
@Named("releaseTimeline")
public class ReleaseTimelineFactory {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    static final int UPCOMING_WEEKS = 6;

    private final ReleaseCatalog catalog;

    @Inject
    public ReleaseTimelineFactory(ReleaseCatalog catalog) {
        this.catalog = catalog;
    }

    public List<LegendItem> legend() {
        List<LegendItem> items = new ArrayList<>(List.of(
                new LegendItem("community", "Community maintenance",
                        "Solid bar: bug fixes and security patches from the Quarkus community."),
                new LegendItem("enterprise", "Enterprise support",
                        "Bordered bar: paid support with an end date, from Red Hat and IBM."),
                new LegendItem("nes", "Never-ending support",
                        "Fading bar: paid patches with no end date, from IBM and HeroDevs.")));
        if (catalog.all().stream().anyMatch(Release::upcoming)) {
            items.add(new LegendItem("upcoming", "Upcoming", "Planned release."));
        }
        items.add(new LegendItem("prov-rhbq", "Red Hat", "Red Hat build of Quarkus"));
        items.add(new LegendItem("prov-ibm", "IBM", "IBM Enterprise Build of Quarkus"));
        items.add(new LegendItem("prov-herodevs", "HeroDevs", "HeroDevs Never-Ending Support"));
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
        TimelineBuilder b = newBuilder(id, scale);
        addUpcoming(b, today);
        addStreamLane(b, today);
        for (Release r : catalog.all()) {
            if (!r.upcoming() && (r.lts() || r.hasVendorSupport() || r.hasNes())) {
                addReleaseRow(b, r, today);
            }
        }
        return b.build();
    }

    private int endYear(LocalDate today) {
        return catalog.all().stream().map(Release::supportEnd).filter(d -> d != null)
                .map(LocalDate::getYear).max(Integer::compare).map(y -> Math.max(y, today.getYear()))
                .orElse(today.getYear());
    }

    private TimelineBuilder newBuilder(String id, TimeScale scale) {
        TimelineBuilder b = new TimelineBuilder(id, scale);
        for (Release major : catalog.majors()) {
            b.marker(major.releaseDate(), "Quarkus " + major.version().replaceAll("\\.0$", ""), "major");
        }
        for (LegendItem item : legend()) {
            b.legend(item.kind(), item.label(), item.description());
        }
        return b;
    }

    /**
     * Only drawn for releases flagged upcoming. Starts on the planned_date when the data has one in
     * the future, otherwise today. An LTS runs for the policy's LTS support duration (12 months);
     * anything else is a short placeholder, since a non-LTS lasts only until the next minor.
     */
    private void addUpcoming(TimelineBuilder b, LocalDate today) {
        Period lts = catalog.policy() == null ? null : catalog.policy().ltsSupportPeriod();
        for (Release r : catalog.all()) {
            if (!r.upcoming()) {
                continue;
            }
            boolean planned = r.plannedDate() != null && r.plannedDate().isAfter(today);
            LocalDate start = planned ? r.plannedDate() : today;
            boolean ltsLength = r.lts() && lts != null;
            LocalDate end = ltsLength ? start.plus(lts) : start.plusWeeks(UPCOMING_WEEKS);
            List<String> lines = new ArrayList<>();
            lines.add(planned ? "Planned release " + DATE.format(r.plannedDate()) : "Upcoming release");
            if (ltsLength) {
                lines.add("LTS: community maintenance for " + catalog.policy().ltsSupportDuration());
            }
            var row = b.row("upcoming-" + r.version(), r.version()).data("version", r.version());
            if (r.lts()) {
                row.badge("LTS").data("lts", "true");
            }
            row.segment("upcoming", start, end, null, "Quarkus " + r.version() + " (upcoming)", lines, r.version())
                    .add();
        }
    }

    /** Non-LTS minors laid end to end; each is supported until the next one ships. */
    private void addStreamLane(TimelineBuilder b, LocalDate today) {
        var row = b.row("stream", "Non-LTS releases").shortLabel("Non-LTS");
        List<Release> stream = catalog.all().stream()
                .filter(r -> !r.upcoming() && !r.lts() && r.releaseDate() != null)
                .sorted(Comparator.comparing(Release::releaseDate))
                .toList();
        for (Release r : stream) {
            row.segment("community", r.releaseDate(), r.eolDate(), communityHref(r, today),
                    "Quarkus " + r.version(), communityLines(r), r.version());
        }
        row.add();
    }

    private void addReleaseRow(TimelineBuilder b, Release r, LocalDate today) {
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
