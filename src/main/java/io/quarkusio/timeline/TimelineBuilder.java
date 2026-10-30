package io.quarkusio.timeline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fluent builder that clamps segments to the scale and precomputes percentages. */
public final class TimelineBuilder {

    /** Minimum visible bar width, as percent of the range. */
    public static final double MIN_WIDTH_PCT = 0.15;

    private final String id;
    private final TimeScale scale;
    private final List<TimelineRow> rows = new ArrayList<>();
    private final List<TimelineMarker> markers = new ArrayList<>();
    private final List<LegendItem> legend = new ArrayList<>();
    private boolean keepEmptyRows;

    public TimelineBuilder(String id, TimeScale scale) {
        this.id = id;
        this.scale = scale;
    }

    public TimelineBuilder keepEmptyRows(boolean keep) {
        this.keepEmptyRows = keep;
        return this;
    }

    public TimelineBuilder marker(LocalDate date, String label, String kind) {
        if (scale.contains(date)) {
            markers.add(new TimelineMarker(date, label, kind, scale.pct(date)));
        }
        return this;
    }

    public TimelineBuilder legend(String kind, String label, String description) {
        legend.add(new LegendItem(kind, label, description));
        return this;
    }

    public RowBuilder row(String id, String label) {
        return new RowBuilder(id, label);
    }

    public Timeline build() {
        return new Timeline(id, scale, List.copyOf(rows), List.copyOf(markers), List.copyOf(legend));
    }

    public final class RowBuilder {
        private final String id;
        private final String label;
        private final List<String> badges = new ArrayList<>();
        private final List<TimelineSegment> segments = new ArrayList<>();
        private final Map<String, String> data = new LinkedHashMap<>();
        private final List<String> lanes = new ArrayList<>();
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
            segments.add(new TimelineSegment(kind, variant, lane, start, end, open, href, title,
                    List.copyOf(details), ref, scale.pct(from), scale.widthPct(from, to, MIN_WIDTH_PCT), 0, 0));
            return this;
        }

        /** Packs extra lanes into slots: lanes whose date ranges do not overlap share one. */
        private int[] packSlots() {
            LocalDate[] from = new LocalDate[lanes.size() + 1];
            LocalDate[] to = new LocalDate[lanes.size() + 1];
            for (TimelineSegment sg : segments) {
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

        public TimelineBuilder add() {
            if (!segments.isEmpty() || keepEmptyRows) {
                int[] slot = packSlots();
                int n = java.util.Arrays.stream(slot).max().orElse(0);
                int stack = n == 0 ? 0 : n * Timeline.LANE_H + (n - 1) * Timeline.LANE_GAP;
                int band = Math.max(Timeline.MAIN_H, stack);
                int top = Timeline.ROW_PAD / 2;
                List<TimelineSegment> placed = segments.stream().map(sg -> {
                    int y;
                    int h;
                    if (sg.lane() == 0) {
                        h = Timeline.MAIN_H + Timeline.MAIN_STROKE;
                        y = top + (band - Timeline.MAIN_H) / 2 - Timeline.MAIN_STROKE / 2;
                    } else {
                        h = Timeline.LANE_H;
                        y = top + (band - stack) / 2 + (slot[sg.lane()] - 1) * (Timeline.LANE_H + Timeline.LANE_GAP);
                    }
                    return new TimelineSegment(sg.kind(), sg.variant(), sg.lane(), sg.start(), sg.end(),
                            sg.openEnded(), sg.href(), sg.title(), sg.details(), sg.ref(), sg.xPct(), sg.wPct(), y, h);
                }).toList();
                rows.add(new TimelineRow(id, label, List.copyOf(badges), href, placed,
                        java.util.Collections.unmodifiableMap(data), List.copyOf(lanes), band + Timeline.ROW_PAD, shortLabel));
            }
            return TimelineBuilder.this;
        }
    }
}
