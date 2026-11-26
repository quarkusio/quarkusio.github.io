package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PendingIndexTest {

    static final Instant NOW = Instant.parse("2026-11-23T15:20:00Z");
    static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    static CveDelta.Result delta(Instant last, Map<String, Boolean> changed) {
        return new CveDelta.Result(new TreeMap<>(changed), last, 1, 0, List.of());
    }

    @Test
    void aSeedIsDatedAWeekBackAtMidnightSoThatTheFirstDeltaCoversTheIndexingLagOfCodeSearch() {
        assertEquals(Instant.parse("2026-11-16T00:00:00Z"), PendingIndex.seed(List.of("CVE-2026-1"), NOW, TODAY).scanned());
    }

    @Test
    void twoListsAreTheSameWhenTheyHoldTheSameCvesWhateverTheirDates() {
        PendingIndex a = new PendingIndex(NOW, Map.of("CVE-2026-1", "2026-11-01"));
        assertTrue(a.sameCves(new PendingIndex(NOW.plusSeconds(86400), Map.of("CVE-2026-1", "2026-11-20"))));
        assertFalse(a.sameCves(new PendingIndex(NOW, Map.of("CVE-2026-2", "2026-11-01"))));
    }

    @Test
    void aSeedHoldsTheCvesAndTheDayItWasMade() {
        PendingIndex seed = PendingIndex.seed(List.of("CVE-2026-2", "CVE-2026-1"), NOW, TODAY);
        assertEquals(Set.of("CVE-2026-1", "CVE-2026-2"), seed.ids());
        assertEquals("2026-11-23", seed.entries().get(0).noticed());
    }

    @Test
    void theFileUsesBlockStyleOnlyAndIsReadBackAsItWasWritten(@TempDir Path dir) throws Exception {
        PendingIndex index = PendingIndex.seed(List.of("CVE-2026-2", "CVE-2026-1"), NOW, TODAY);
        String text = index.render();
        assertEquals("""
                scanned: "2026-11-16T00:00:00Z"
                pending:
                - id: "CVE-2026-1"
                  noticed: "2026-11-23"
                - id: "CVE-2026-2"
                  noticed: "2026-11-23"
                """, text);
        assertFalse(text.matches("(?s).*: [\\[{].*"), "po4a cannot read an inline list or map");
        Path f = dir.resolve("pending-cves.yaml");
        Files.write(f, index.bytes());
        PendingIndex back = PendingIndex.read(f).orElseThrow();
        assertEquals(index.render(), back.render());
    }

    @Test
    void anEmptyListIsAValidFile(@TempDir Path dir) throws Exception {
        PendingIndex empty = new PendingIndex(NOW, Map.of());
        assertEquals("scanned: \"2026-11-23T15:20:00Z\"\npending:\n", empty.render());
        Path f = dir.resolve("p.yaml");
        Files.write(f, empty.bytes());
        assertTrue(PendingIndex.read(f).orElseThrow().ids().isEmpty());
    }

    @Test
    void noFileMeansNoIndexButAFileThatCannotBeReadIsAnErrorNotAReasonToStartOver(@TempDir Path dir) throws Exception {
        assertTrue(PendingIndex.read(dir.resolve("none.yaml")).isEmpty());
        Path bad = dir.resolve("bad.yaml");
        Files.writeString(bad, "pending:\n- id: \"CVE-2026-1\"\n");
        assertThrows(IllegalStateException.class, () -> PendingIndex.read(bad));
        Files.writeString(bad, "scanned: \"2026-11-23T15:00:00Z\"\npending:\n- id: \"nonsense\"\n");
        assertThrows(IllegalStateException.class, () -> PendingIndex.read(bad));
    }

    @Test
    void anIndexOlderThanThirtyDaysIsNotUsable() {
        assertTrue(new PendingIndex(NOW.minusSeconds(29L * 86400), Map.of()).usable(NOW));
        assertFalse(new PendingIndex(NOW.minusSeconds(31L * 86400), Map.of()).usable(NOW));
    }

    @Test
    void aDeltaAddsNewCvesDropsThoseThatStoppedMentioningQuarkusAndKeepsTheDateFirstSeen() {
        PendingIndex before = new PendingIndex(Instant.parse("2026-11-20T10:00:00Z"),
                Map.of("CVE-2026-1", "2026-11-01", "CVE-2026-2", "2026-11-02"));
        PendingIndex after = before.apply(delta(Instant.parse("2026-11-23T14:00:00Z"),
                Map.of("CVE-2026-1", true, "CVE-2026-2", false, "CVE-2026-3", true, "CVE-2026-4", false)), TODAY);
        assertEquals(Set.of("CVE-2026-1", "CVE-2026-3"), after.ids());
        assertEquals("2026-11-01", after.entries().get(0).noticed());
        assertEquals("2026-11-23", after.entries().get(1).noticed());
        assertEquals(Instant.parse("2026-11-23T14:00:00Z"), after.scanned());
    }

    @Test
    void theTimeMovesOnEvenWhenNothingChangedButNeverBackwards() {
        PendingIndex before = new PendingIndex(Instant.parse("2026-11-23T10:00:00Z"), Map.of());
        assertEquals(Instant.parse("2026-11-23T14:00:00Z"),
                before.apply(delta(Instant.parse("2026-11-23T14:00:00Z"), Map.of()), TODAY).scanned());
        assertEquals(Instant.parse("2026-11-23T10:00:00Z"),
                before.apply(delta(Instant.parse("2026-11-23T08:00:00Z"), Map.of()), TODAY).scanned());
    }

}
