package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BranchLineageTest {

    private static BranchLineage.MergeBase mb(String sha, String date) {
        return new BranchLineage.MergeBase(sha, Instant.parse(date));
    }

    @Test
    void mostRecentMergeBaseWins() {
        // 3.40: split from 3.39's line in September, from 3.38's in July
        var r = BranchLineage.pick(Map.of("3.39", mb("e45", "2026-09-16T08:31:26Z"), "3.38", mb("a45", "2026-07-15T06:30:44Z")));
        assertEquals("3.39", r.version());
        assertFalse(r.ambiguous());
    }

    @Test
    void tiedMergeBasesPickNewestAndAreAmbiguous() {
        var r = BranchLineage.pick(Map.of("3.38", mb("x", "2026-06-25T15:11:24Z"), "3.39", mb("x", "2026-06-25T15:11:24Z")));
        assertEquals("3.39", r.version());
        assertTrue(r.ambiguous());
        assertEquals(List.of("3.39", "3.38"), r.candidates());
    }

    @Test
    void nothingToChooseFromGivesNothing() {
        var r = BranchLineage.pick(Map.of());
        assertNull(r.version());
        assertFalse(r.ambiguous());
    }

    @Test
    void minorsBelowStaysInTheMajorAndIsLimited() {
        var all = List.of("2.16", "3.0", "3.1", "3.2", "3.3", "3.4");
        assertEquals(List.of("3.3", "3.2", "3.1"), BranchLineage.minorsBelow("3.4", all));
        assertEquals(List.of("2.16"), BranchLineage.minorsBelow("2.17", all));
    }

}
