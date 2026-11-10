package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdvisoryRangesTest {

    static final List<String> RELEASES = List.of("3.27.0.CR1", "3.27.0", "3.27.1", "3.28.0", "3.28.1", "3.29.0");

    @Test
    void anUnbrokenRunBecomesOneRangeEndingAtTheFixingRelease() {
        var ranges = AdvisoryRanges.compute(RELEASES, Map.of("A", Set.of("3.27.0.CR1", "3.27.0", "3.27.1")));
        assertEquals(List.of(">=3.27.0.CR1 <3.28.0"), ranges.get("A"));
    }

    @Test
    void aGapSplitsTheRunAndAnAdvisoryThatReturnsGetsASecondRange() {
        var ranges = AdvisoryRanges.compute(RELEASES, Map.of("A", Set.of("3.27.0.CR1", "3.27.0", "3.28.0", "3.28.1")));
        assertEquals(List.of(">=3.27.0.CR1 <3.27.1", ">=3.28.0 <3.29.0"), ranges.get("A"));
    }

    @Test
    void aRunReachingTheNewestReleaseHasNoUpperBound() {
        var ranges = AdvisoryRanges.compute(RELEASES, Map.of("A", Set.of("3.28.1", "3.29.0")));
        assertEquals(List.of(">=3.28.1"), ranges.get("A"));
    }

    @Test
    void versionsAreOrderedByTheGivenListNotAlphabetically() {
        var ranges = AdvisoryRanges.compute(List.of("3.9.0", "3.10.0", "3.11.0"), Map.of("A", Set.of("3.9.0", "3.10.0")));
        assertEquals(List.of(">=3.9.0 <3.11.0"), ranges.get("A"));
    }

    @Test
    void resultIsSortedByAdvisoryId() {
        var ranges = AdvisoryRanges.compute(RELEASES, Map.of("B", Set.of("3.29.0"), "A", Set.of("3.29.0")));
        assertEquals(List.of("A", "B"), List.copyOf(ranges.keySet()));
    }
}
