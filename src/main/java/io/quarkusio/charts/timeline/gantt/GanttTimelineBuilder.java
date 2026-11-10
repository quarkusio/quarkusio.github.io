package io.quarkusio.charts.timeline.gantt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkusio.charts.timeline.LegendItem;
import io.quarkusio.charts.timeline.TimeScale;
import io.quarkusio.charts.timeline.Timeline;
import io.quarkusio.charts.timeline.TimelineMarker;
import io.quarkusio.releases.ReleaseKind;

/** Fluent builder that clamps segments to the scale and precomputes percentages. */
public final class GanttTimelineBuilder {

    /** Minimum visible bar width, as percent of the range. */
    public static final double MIN_WIDTH_PCT = 0.15;

    private final String id;
    private final TimeScale scale;
    private final List<TimelineRow> rows = new ArrayList<>();
    private final List<TimelineMarker> markers = new ArrayList<>();
    private final List<LegendItem> legend = new ArrayList<>();
    private final List<PendingLink> links = new ArrayList<>();
    private final List<PendingChain> chains = new ArrayList<>();
    private boolean keepEmptyRows;

    public GanttTimelineBuilder(String id, TimeScale scale) {
        this.id = id;
        this.scale = scale;
    }

    public GanttTimelineBuilder keepEmptyRows(boolean keep) {
        this.keepEmptyRows = keep;
        return this;
    }

    public GanttTimelineBuilder marker(LocalDate date, String label, ReleaseKind kind) {
        if (scale.contains(date)) {
            markers.add(new TimelineMarker(date, label, kind, scale.pct(date)));
        }
        return this;
    }

    public void legend(LegendItem item) {
        legend.add(item);
    }

    private record PendingLink(String fromRow, String toRow, LocalDate fromDate, LocalDate toDate, boolean rise) {
    }

    private record PendingChain(String row, LocalDate at) {
    }

    /** A straight joint on the main bar of a row, bridging the gap where two bars meet. */
    public GanttTimelineBuilder chain(String row, LocalDate at) {
        chains.add(new PendingChain(row, at));
        return this;
    }

    /**
     * Connects the main bar of one row, at fromDate, to the start of the main bar of another, at toDate: it
     * runs straight up or down from the first, then turns to enter the second horizontally from the left.
     * Resolved when the timeline is built; ignored if either row is missing.
     */
    public GanttTimelineBuilder link(String fromRow, String toRow, LocalDate fromDate, LocalDate toDate) {
        links.add(new PendingLink(fromRow, toRow, fromDate, toDate, false));
        return this;
    }

    /**
     * Like {@link #link}, for a connector that rises from the left of one bar to the start of a bar in a row
     * above: it rises, S-curves across and rises again into the gap, then turns to enter the bar horizontally.
     */
    public GanttTimelineBuilder rise(String fromRow, String toRow, LocalDate fromDate, LocalDate toDate) {
        links.add(new PendingLink(fromRow, toRow, fromDate, toDate, true));
        return this;
    }

    /** Row id for a link end below the last row: the line just runs off the bottom of the chart. */
    public static final String BOTTOM = "#bottom";

    private int indexOf(String rowId) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(rowId)) {
                return i;
            }
        }
        return -1;
    }

    private int rowTop(int index) {
        int y = Timeline.AXIS_H;
        for (int i = 0; i < index; i++) {
            y += rows.get(i).height();
        }
        return y;
    }

    /** The bottom edge of the plot, in plot coordinates. */
    private int bottomY() {
        return rowTop(rows.size()) + Timeline.FOOT_H;
    }

    /** Vertical middle of a row's main bar, in plot coordinates. */
    private int mainMidY(int index) {
        int top = rowTop(index);
        TimelineRow row = rows.get(index);
        // a row with no main bar (only lanes) is aimed at in its middle
        return row.segments().stream().filter(sg -> sg.lane() == 0).findFirst()
                .map(main -> top + main.y() + main.h() / 2)
                .orElse(top + row.height() / 2);
    }

    /** Smallest width of a connector box, so a straight vertical one still renders. */
    private static final double MIN_LINK_WIDTH_PCT = 0.01;
    /** Height of the S-curve in a rising connector, and the length of its last rise before it turns, in pixels. */
    private static final int RISE_S_HEIGHT = 26;
    private static final int RISE_TURN = 8;

    /** How far from the bar a dropped connector turns, in pixels: small, so it runs nearly down to the bar first. */
    private static final int DROP_RUN = 6;

    /** Straight down (or up) from y1, then a quick turn so it enters the bar at y2 horizontally. */
    private static String joinPath(int y1, int y2, int run) {
        run = Math.min(run, Math.abs(y2 - y1));
        int turn = y2 - Integer.signum(y2 - y1) * run;
        return "M0," + y1 + " V" + turn + " C0," + y2 + " 60," + y2 + " 100," + y2;
    }

    /** Rises straight up, S-curves across just below y2 and rises into the gap, then turns into the horizontal at y2. */
    private static String risePath(int y1, int y2) {
        int rise = Math.abs(y1 - y2);
        int from = y2 + Math.min(RISE_S_HEIGHT, rise / 2);
        int into = y2 + Math.min(RISE_TURN, rise / 4);
        int mid = (from + into) / 2;
        return "M0," + y1 + " V" + from + " C0," + mid + " 50," + mid + " 50," + into
                + " C50," + y2 + " 70," + y2 + " 100," + y2;
    }

    public RowBuilder row(String id, String label) {
        return new RowBuilder(id, label);
    }

    public Timeline build() {
        List<TimelineLink> resolved = new ArrayList<>();
        for (PendingLink l : links) {
            boolean fromBottom = l.fromRow().equals(BOTTOM);
            boolean toBottom = l.toRow().equals(BOTTOM);
            int from = fromBottom ? rows.size() : indexOf(l.fromRow());
            int to = toBottom ? rows.size() : indexOf(l.toRow());
            if (from < 0 || to < 0 || l.toDate().isBefore(scale.start()) || l.fromDate().isAfter(scale.end())
                    || (fromBottom && !scale.contains(l.toDate()))
                    || (toBottom && !scale.contains(l.fromDate()))) {
                continue;
            }
            LocalDate at = l.fromDate().isBefore(scale.start()) ? scale.start() : l.fromDate();
            LocalDate end = l.toDate().isBefore(scale.start()) ? scale.start() : l.toDate();
            int y1 = fromBottom ? bottomY() : mainMidY(from);
            int y2 = toBottom ? bottomY() : mainMidY(to);
            // a line running off the bottom is straight; there is nothing there to line up with
            String path = toBottom ? "M0," + y1 + " V" + y2
                    : l.rise() ? risePath(y1, y2) : joinPath(y1, y2, DROP_RUN);
            resolved.add(new TimelineLink(scale.pct(at), scale.widthPct(at, end, MIN_LINK_WIDTH_PCT), path));
        }
        List<TimelineChain> joints = new ArrayList<>();
        for (PendingChain c : chains) {
            int row = indexOf(c.row());
            if (row >= 0 && scale.contains(c.at())) {
                joints.add(new TimelineChain(scale.pct(c.at()), mainMidY(row)));
            }
        }
        return new Timeline(id, scale, List.copyOf(rows), List.copyOf(markers), List.copyOf(legend),
                List.copyOf(resolved), List.copyOf(joints));
    }


    public final class RowBuilder {
        private final String id;
        private final String label;
        private final List<String> badges = new ArrayList<>();
        private final List<GanttTimelineSegment> segments = new ArrayList<>();
        private final Map<String, String> data = new LinkedHashMap<>();
        private final List<String> lanes = new ArrayList<>();
        private final List<LocalDate> branchFrom = new ArrayList<>();
        private final List<Integer> branchLane = new ArrayList<>();
        private String href;
        private String shortLabel;
        private RowBuilder(String id, String label) {
            this.id = id;
            this.label = label;
        }

        /** Label to show instead of the full one on narrow screens. */
        public RowBuilder shortLabel(String shortLabel) {
            this.shortLabel = shortLabel;
            return this;
        }

        public RowBuilder badge(String badge) {
            badges.add(badge);
            return this;
        }

        public RowBuilder href(String href) {
            this.href = href;
            return this;
        }

        public RowBuilder data(String key, String value) {
            data.put(key, value);
            return this;
        }

        /** Connects the main bar, at the given date, to the start of the bar in the lane. */
        public RowBuilder branch(int lane, LocalDate from) {
            branchLane.add(lane);
            branchFrom.add(from);
            return this;
        }

        /** Adds a lane below the main one and returns its index. */
        public int lane(String label) {
            lanes.add(label);
            return lanes.size();
        }

        public RowBuilder segment(String kind, LocalDate start, LocalDate end, String href, String title,
                List<String> details, String ref) {
            return segment(kind, null, 0, start, end, href, title, details, ref);
        }

        /** A null end means open-ended: the segment runs to the end of the range. */
        public RowBuilder segment(String kind, String variant, int lane, LocalDate start, LocalDate end,
                String href, String title, List<String> details, String ref) {
            boolean open = end == null;
            LocalDate to = open ? scale.end() : end;
            if (to.isBefore(scale.start()) || start.isAfter(scale.end())) {
                return this; // entirely outside the visible range
            }
            LocalDate from = start.isBefore(scale.start()) ? scale.start() : start;
            if (to.isAfter(scale.end())) {
                to = scale.end();
            }
            segments.add(new GanttTimelineSegment(kind, variant, lane, start, end, open, href, title,
                    List.copyOf(details), ref, scale.pct(from), scale.widthPct(from, to, MIN_WIDTH_PCT), 0, 0));
            return this;
        }

        /** Packs extra lanes into slots: lanes whose date ranges do not overlap share one. */
        private int[] packSlots() {
            LocalDate[] from = new LocalDate[lanes.size() + 1];
            LocalDate[] to = new LocalDate[lanes.size() + 1];
            for (GanttTimelineSegment sg : segments) {
                LocalDate end = sg.end() == null ? LocalDate.MAX : sg.end();
                int l = sg.lane();
                from[l] = from[l] == null || sg.start().isBefore(from[l]) ? sg.start() : from[l];
                to[l] = to[l] == null || end.isAfter(to[l]) ? end : to[l];
            }
            int[] slot = new int[lanes.size() + 1];
            List<LocalDate> slotEnd = new ArrayList<>();
            for (int lane = 1; lane <= lanes.size(); lane++) {
                if (from[lane] == null) {
                    continue;
                }
                int s = 0;
                while (s < slotEnd.size() && from[lane].isBefore(slotEnd.get(s))) {
                    s++;
                }
                if (s == slotEnd.size()) {
                    slotEnd.add(to[lane]);
                } else {
                    slotEnd.set(s, to[lane]);
                }
                slot[lane] = s + 1;
            }
            return slot;
        }

        /** True if any lane segment is in the same time span as a main-lane one. */
        private boolean lanesOverlapMain() {
            for (GanttTimelineSegment lane : segments) {
                if (lane.lane() == 0) {
                    continue;
                }
                for (GanttTimelineSegment main : segments) {
                    if (main.lane() == 0 && overlaps(lane, main)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean overlaps(GanttTimelineSegment a, GanttTimelineSegment b) {
            LocalDate aEnd = a.end() == null ? LocalDate.MAX : a.end();
            LocalDate bEnd = b.end() == null ? LocalDate.MAX : b.end();
            return a.start().isBefore(bEnd) && b.start().isBefore(aEnd);
        }

        public GanttTimelineBuilder add() {
            if (!segments.isEmpty() || keepEmptyRows) {
                int[] slot = packSlots();
                int n = java.util.Arrays.stream(slot).max().orElse(0);
                int laneH = Timeline.MAIN_H;
                // Lanes that overlap the main bar in time are stacked above it, so the main bar is the lowest.
                // Otherwise they share the main bar's band, centred on it.
                boolean above = n > 0 && lanesOverlapMain();
                int gap = Timeline.LANE_GAP;
                int stack = n == 0 ? 0 : n * laneH + (n - 1) * gap;
                int band = above ? stack + gap + Timeline.MAIN_H : Math.max(Timeline.MAIN_H, stack);
                int top = Timeline.ROW_PAD / 2;
                int mainY = above ? top + stack + gap : top + (band - Timeline.MAIN_H) / 2;
                int lanesTop = top + (band - (above ? stack + gap + Timeline.MAIN_H : stack)) / 2;
                int laneStep = laneH + gap;
                List<GanttTimelineSegment> placed = segments.stream().map(sg -> {
                    int y;
                    int h;
                    if (sg.lane() == 0) {
                        h = Timeline.MAIN_H;
                        y = mainY;
                    } else {
                        h = laneH;
                        y = lanesTop + (slot[sg.lane()] - 1) * laneStep;
                    }
                    return new GanttTimelineSegment(sg.kind(), sg.variant(), sg.lane(), sg.start(), sg.end(),
                            sg.openEnded(), sg.href(), sg.title(), sg.details(), sg.ref(), sg.xPct(), sg.wPct(), y, h);
                }).toList();
                List<TimelineLink> branches = new ArrayList<>();
                for (int i = 0; i < branchLane.size(); i++) {
                    int lane = branchLane.get(i);
                    GanttTimelineSegment target = placed.stream().filter(sg -> sg.lane() == lane).findFirst().orElse(null);
                    if (target == null) {
                        continue;
                    }
                    int laneMid = target.y() + target.h() / 2;
                    // start at the middle of the main bar: the connectors are drawn under the bars, so they
                    // show only in the gaps and read as one bracket down through the vendor lanes
                    int fromY = mainY + Timeline.MAIN_H / 2;
                    LocalDate at = branchFrom.get(i).isBefore(scale.start()) ? scale.start() : branchFrom.get(i);
                    LocalDate start = target.start().isBefore(scale.start()) ? scale.start() : target.start();
                    branches.add(new TimelineLink(scale.pct(at), scale.widthPct(at, start, MIN_LINK_WIDTH_PCT),
                            joinPath(fromY, laneMid, 8)));
                }
                rows.add(new TimelineRow(id, label, List.copyOf(badges), href, placed,
                        java.util.Collections.unmodifiableMap(data), List.copyOf(lanes), band + Timeline.ROW_PAD,
                        shortLabel, List.copyOf(branches)));
            }
            return GanttTimelineBuilder.this;
        }
    }
}
