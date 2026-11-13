package io.quarkusio.charts.pie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A ring of slices, largest first, clockwise from the top, with a handle that can be dragged round it. The handle
 * snaps to a stop: the end of the last slice of a size. A stop's value is that size, so the slices from the top up
 * to the stop are exactly those with at least that value, which makes the ring a circular slider. Generic: slices
 * carry a free-form kind (CSS class pie-kind--{kind}) and data-* attributes, for the page script to use. Rendered
 * by partials/charts/pie.html, driven by public/assets/javascript/pie.js.
 */
public record PieChart(String id, List<Slice> slices, List<Stop> stops) {

    public static final int SIZE = 220;
    /** How many steps there are from the palest slice to the darkest. */
    public static final int SHADES = 5;
    public static final int OUTER = 92;
    public static final int INNER = 56;

    /** What the caller provides for each slice. */
    public record Input(String id, String label, int value, String kind, Map<String, String> data) {
    }

    /** shade is 0 for the fewest to SHADES - 1 for the most, see {@link #shade}. */
    public record Slice(String id, String label, int value, String kind, int shade, Map<String, String> data,
            String path, double start, double end) {

        public String title() {
            return label + ": " + value + (value == 1 ? " CVE" : " CVEs");
        }
    }

    /** angle is where the stop is, in degrees clockwise from the top. */
    public record Stop(int value, double angle) {

        public String angleText() {
            return fmt(angle);
        }

        /** For the script: "angle:value". */
        public String attr() {
            return fmt(angle) + ":" + value;
        }
    }

    /** Slices sorted by value, largest first; equal values keep the order they were given in. */
    public static PieChart of(String id, List<Input> inputs) {
        List<Input> sorted = inputs.stream().filter(i -> i.value() > 0)
                .sorted(Comparator.comparingInt((Input i) -> -i.value())).toList();
        double total = sorted.stream().mapToInt(Input::value).sum();
        int max = sorted.isEmpty() ? 1 : sorted.get(0).value();
        List<Slice> slices = new ArrayList<>();
        List<Stop> stops = new ArrayList<>();
        double angle = 0;
        for (int i = 0; i < sorted.size(); i++) {
            Input in = sorted.get(i);
            double end = i == sorted.size() - 1 ? 360 : angle + in.value() / total * 360;
            slices.add(new Slice(in.id(), in.label(), in.value(), in.kind(), shade(in.value(), max), in.data(),
                    path(angle, end), angle, end));
            if (i == sorted.size() - 1 || sorted.get(i + 1).value() != in.value()) {
                stops.add(new Stop(in.value(), end));
            }
            angle = end;
        }
        return new PieChart(id, slices, stops);
    }

    /**
     * Bigger slices are darker, on a log scale because a few slices are far bigger than the rest. The page picks
     * the colours, so it can keep every step distinguishable from the background.
     */
    static int shade(int value, int max) {
        if (max <= 1) {
            return 0;
        }
        return (int) Math.round(Math.log(value) / Math.log(max) * (SHADES - 1));
    }

    public int size() {
        return SIZE;
    }

    public int center() {
        return SIZE / 2;
    }

    public int outer() {
        return OUTER;
    }

    public int inner() {
        return INNER;
    }

    /** The handle is drawn pointing up, from the ring's inner edge to its outer edge, then rotated. */
    public int handleFrom() {
        return center() - INNER;
    }

    public int handleTo() {
        return center() - OUTER;
    }

    /** All the stops, for the script: "angle:value,angle:value". */
    public String stopsAttr() {
        return String.join(",", stops.stream().map(Stop::attr).toList());
    }

    /** Where the handle sits for a threshold: the last stop that still includes at least this value. */
    public Stop stopFor(int minValue) {
        Stop found = stops.isEmpty() ? new Stop(minValue, 360) : stops.get(0);
        for (Stop s : stops) {
            if (s.value() >= minValue) {
                found = s;
            }
        }
        return found;
    }

    private static String path(double from, double to) {
        // a slice cannot be a whole ring in one arc
        double end = Math.min(to, 359.99);
        String large = end - from > 180 ? "1" : "0";
        return "M" + point(OUTER, from) + "A" + OUTER + "," + OUTER + " 0 " + large + " 1 " + point(OUTER, end)
                + "L" + point(INNER, end) + "A" + INNER + "," + INNER + " 0 " + large + " 0 " + point(INNER, from) + "Z";
    }

    private static String point(int radius, double degrees) {
        double rad = Math.toRadians(degrees);
        int c = SIZE / 2;
        return fmt(c + radius * Math.sin(rad)) + "," + fmt(c - radius * Math.cos(rad));
    }

    static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
