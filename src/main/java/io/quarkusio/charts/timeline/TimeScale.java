package io.quarkusio.charts.timeline;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps dates onto a 0..100 percentage axis. All strings use {@link Locale#ROOT}
 * so templates never print a decimal comma.
 */
public record TimeScale(LocalDate start, LocalDate end) {

    private static final DateTimeFormatter QUARTER_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TODAY_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public TimeScale {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start: " + start + " .. " + end);
        }
    }

    public double percent(LocalDate d) {
        double span = ChronoUnit.DAYS.between(start, end);
        double p = ChronoUnit.DAYS.between(start, d) / span * 100.0;
        return Math.max(0, Math.min(100, p));
    }

    public String pct(LocalDate d) {
        return format(percent(d));
    }

    /** Width of from..to in percent, never narrower than minPct. */
    public String widthPct(LocalDate from, LocalDate to, double minPct) {
        return format(Math.max(minPct, percent(to) - percent(from)));
    }

    public List<Tick> yearTicks() {
        List<Tick> ticks = new ArrayList<>();
        for (int y = start.getYear(); y <= end.getYear(); y++) {
            LocalDate d = LocalDate.of(y, 1, 1);
            if (contains(d)) {
                ticks.add(new Tick(d, String.valueOf(y), pct(d)));
            }
        }
        return ticks;
    }

    /** Every other calendar year, starting with the second, clipped to the range, for a subtle striped background. */
    public List<Stripe> yearStripes() {
        List<Stripe> stripes = new ArrayList<>();
        for (int y = start.getYear(); y <= end.getYear(); y++) {
            if ((y - start.getYear()) % 2 == 0) {
                continue;
            }
            LocalDate from = LocalDate.of(y, 1, 1);
            LocalDate to = LocalDate.of(y + 1, 1, 1).isAfter(end) ? end : LocalDate.of(y + 1, 1, 1);
            stripes.add(new Stripe(pct(from), widthPct(from, to, 0)));
        }
        return stripes;
    }

    /**
     * Year ticks for long ranges, otherwise one tick per quarter labelled with the month and year,
     * e.g. "Jan 2026". Ranges shorter than four years are the ones where years alone are too coarse.
     */
    public List<Tick> ticks() {
        if (ChronoUnit.YEARS.between(start, end) >= 4) {
            return yearTicks();
        }
        List<Tick> ticks = new ArrayList<>();
        LocalDate d = LocalDate.of(start.getYear(), ((start.getMonthValue() - 1) / 3) * 3 + 1, 1);
        for (; !d.isAfter(end); d = d.plusMonths(3)) {
            if (contains(d)) {
                ticks.add(new Tick(d, d.format(QUARTER_LABEL), pct(d)));
            }
        }
        return ticks;
    }

    /** E.g. "Today · 12 Nov 2026". */
    public static String todayLabel(LocalDate today) {
        return "Today · " + today.format(TODAY_LABEL);
    }

    public boolean contains(LocalDate d) {
        return d != null && !d.isBefore(start) && !d.isAfter(end);
    }

    static String format(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    public record Stripe(String xPct, String widthPct) {
    }

    public record Tick(LocalDate date, String label, String pct) {
    }
}
