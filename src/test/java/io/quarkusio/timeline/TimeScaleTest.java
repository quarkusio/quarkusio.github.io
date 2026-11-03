package io.quarkusio.timeline;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class TimeScaleTest {

    final TimeScale scale = new TimeScale(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

    @Test
    void percentAtBoundsAndClamped() {
        assertEquals(0, scale.percent(scale.start()));
        assertEquals(100, scale.percent(scale.end()));
        assertEquals(0, scale.percent(LocalDate.of(2020, 1, 1)));
        assertEquals(100, scale.percent(LocalDate.of(2030, 1, 1)));
    }

    @Test
    void formatsWithRootLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.FRANCE);
        try {
            assertEquals("50.000", new TimeScale(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3)).pct(LocalDate.of(2024, 1, 2)));
            assertFalse(scale.widthPct(scale.start(), scale.end(), 0.15).contains(","));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void widthHonoursMinimum() {
        LocalDate d = LocalDate.of(2024, 6, 1);
        assertEquals("0.150", scale.widthPct(d, d, 0.15));
    }

    @Test
    void yearTicksInsideRange() {
        var ticks = scale.yearTicks();
        assertEquals(2, ticks.size());
        assertEquals("2024", ticks.get(0).label());
        assertEquals("0.000", ticks.get(0).pct());
        assertEquals("2025", ticks.get(1).label());
    }

    @Test
    void ticksSkipYearStartedBeforeRange() {
        var s = new TimeScale(LocalDate.of(2024, 3, 1), LocalDate.of(2025, 12, 31));
        assertEquals(1, s.yearTicks().size());
    }

    @Test
    void rejectsEmptyRange() {
        LocalDate d = LocalDate.of(2024, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> new TimeScale(d, d));
    }
}
