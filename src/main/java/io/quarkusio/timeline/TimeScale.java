package io.quarkusio.timeline;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maps dates onto a 0..100 percentage axis. All strings use {@link Locale#ROOT}
 * so templates never print a decimal comma.
 */
public record TimeScale(LocalDate start, LocalDate end) {

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

    public boolean contains(LocalDate d) {
        return d != null && !d.isBefore(start) && !d.isAfter(end);
    }

    static String format(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    public record Tick(LocalDate date, String label, String pct) {
    }
}
