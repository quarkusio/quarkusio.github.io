package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

public class CveDeltaTest {

    public static byte[] zip(String... nameAndText) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream z = new ZipOutputStream(out)) {
                for (int i = 0; i < nameAndText.length; i += 2) {
                    z.putNextEntry(new ZipEntry(nameAndText[i]));
                    z.write(nameAndText[i + 1].getBytes(StandardCharsets.UTF_8));
                    z.closeEntry();
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Zips by asset name; anything else is not there. Remembers what was asked for. */
    static class Zips implements CveDelta.Source {
        final Map<String, byte[]> byAsset = new HashMap<>();
        final List<String> asked = new ArrayList<>();

        @Override
        public Optional<byte[]> zip(String tag, String asset) {
            asked.add(tag + " " + asset);
            return Optional.ofNullable(byAsset.get(asset));
        }

        Zips endOfDay(String day, String... records) {
            byAsset.put(day + "_delta_CVEs_at_end_of_day.zip", CveDeltaTest.zip(records));
            return this;
        }

        Zips hourly(String day, String hour, String... records) {
            byAsset.put(day + "_delta_CVEs_at_" + hour + "00Z.zip", CveDeltaTest.zip(records));
            return this;
        }
    }

    static final Instant NOON = Instant.parse("2026-11-24T12:30:00Z");

    @Test
    void aRecordIsWhetherItsTextMentionsQuarkusInAnyCase() {
        Map<String, Boolean> r = CveDelta.records(zip(
                "delta/cves/2026/1xxx/CVE-2026-1.json", "{\"title\": \"Quarkus-vertx-http bypass\"}",
                "delta/cves/2026/1xxx/CVE-2026-2.json", "{\"title\": \"Something else\"}",
                "delta/deltaLog.json", "quarkus is mentioned here but this is not a record",
                "delta/cves/2026/1xxx/CVE-2026-3.json", "{\"affected\": [{\"packageName\": \"io.quarkus/quarkus-qute\"}]}"),
                "test");
        assertEquals(Map.of("CVE-2026-1", true, "CVE-2026-2", false, "CVE-2026-3", true), r);
    }

    @Test
    void everyDaySinceTheLastScanIsReadFromItsEndOfDayZipAndTodayFromItsNewestHourlyOne() {
        Zips zips = new Zips()
                .endOfDay("2026-11-22", "a/CVE-2026-1.json", "quarkus")
                .endOfDay("2026-11-23", "a/CVE-2026-2.json", "quarkus")
                .hourly("2026-11-24", "12", "a/CVE-2026-3.json", "quarkus");
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-22T00:00:00Z"), NOON);
        assertEquals(Map.of("CVE-2026-1", true, "CVE-2026-2", true, "CVE-2026-3", true), r.mentionsQuarkus());
        assertEquals(List.of("2026-11-22 end of day", "2026-11-23 end of day", "2026-11-24 up to 1200Z"), r.read());
        assertEquals(Instant.parse("2026-11-24T12:00:00Z"), r.reached());
        assertEquals(List.of(
                "cve_2026-11-22_at_end_of_day 2026-11-22_delta_CVEs_at_end_of_day.zip",
                "cve_2026-11-23_at_end_of_day 2026-11-23_delta_CVEs_at_end_of_day.zip",
                "cve_2026-11-24_1200Z 2026-11-24_delta_CVEs_at_1200Z.zip"), zips.asked);
    }

    @Test
    void theLastHourOfADayIsOnlyInItsEndOfDayZipAndThatIsWhatIsRead() {
        Zips zips = new Zips()
                .hourly("2026-11-23", "23", "a/CVE-2026-1.json", "x")
                .endOfDay("2026-11-23", "a/CVE-2026-1.json", "x", "a/CVE-2026-2.json", "published at 23:30, quarkus")
                .hourly("2026-11-24", "01", "a/CVE-2026-3.json", "x");
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T23:00:00Z"), Instant.parse("2026-11-24T01:30:00Z"));
        assertTrue(r.mentionsQuarkus().get("CVE-2026-2"));
        assertEquals(Instant.parse("2026-11-24T01:00:00Z"), r.reached());
    }

    @Test
    void aMissingEndOfDayZipOfAFullDayFailsTheRun() {
        Zips zips = new Zips()
                .endOfDay("2026-11-23", "a/CVE-2026-2.json", "quarkus")
                .hourly("2026-11-24", "12", "a/CVE-2026-3.json", "x");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CveDelta.read(zips, Instant.parse("2026-11-21T00:00:00Z"), NOON));
        assertTrue(e.getMessage().contains("2026-11-21"), e.getMessage());
    }

    @Test
    void whenYesterdaysEndOfDayZipIsNotPublishedYetItsNewestHourlyOneIsReadAndTheScanStopsThere() {
        Zips zips = new Zips()
                .hourly("2026-11-23", "22", "a/CVE-2026-1.json", "quarkus")
                .hourly("2026-11-24", "00", "a/CVE-2026-9.json", "x");
        // 00:20 on the 24th, and the end-of-day zip of the 23rd is published at about 00:38
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T00:00:00Z"), Instant.parse("2026-11-24T00:20:00Z"));
        assertEquals(Map.of("CVE-2026-1", true), r.mentionsQuarkus());
        assertEquals(Instant.parse("2026-11-23T22:00:00Z"), r.reached());
        assertEquals(1, r.zips());
    }

    @Test
    void theNextRunReadsTheEndOfDayZipOfADayThatWasOnlyReadUpToAnHour() {
        Zips zips = new Zips().endOfDay("2026-11-23", "a/CVE-2026-2.json", "quarkus");
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T22:00:00Z"), Instant.parse("2026-11-24T01:00:00Z"));
        assertTrue(r.mentionsQuarkus().get("CVE-2026-2"));
        assertEquals(Instant.parse("2026-11-24T00:00:00Z"), r.reached());
    }

    @Test
    void todaysNewestHourlyZipIsTheNextOneDownWhenTheLatestIsNotPublishedAndAZipIsAlsoTriedUnderTheTagOfTheHourBefore() {
        Zips zips = new Zips()
                .endOfDay("2026-11-23", "a/CVE-2026-1.json", "x")
                .hourly("2026-11-24", "10", "a/CVE-2026-3.json", "quarkus");
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T00:00:00Z"), NOON);
        assertTrue(r.mentionsQuarkus().get("CVE-2026-3"));
        assertEquals(Instant.parse("2026-11-24T10:00:00Z"), r.reached());
        // the zip of 12:00 is looked for under its own tag and under the one of the hour before, then 11:00 the same way
        assertTrue(zips.asked.contains("cve_2026-11-24_1100Z 2026-11-24_delta_CVEs_at_1200Z.zip"), zips.asked.toString());
        assertTrue(zips.asked.contains("cve_2026-11-24_1000Z 2026-11-24_delta_CVEs_at_1100Z.zip"), zips.asked.toString());
    }

    @Test
    void theNewestRecordOfACveDecides() {
        Zips zips = new Zips()
                .endOfDay("2026-11-23", "a/CVE-2026-1.json", "quarkus", "a/CVE-2026-2.json", "other")
                .hourly("2026-11-24", "12", "a/CVE-2026-1.json", "no longer", "a/CVE-2026-2.json", "now quarkus");
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T00:00:00Z"), NOON);
        assertFalse(r.mentionsQuarkus().get("CVE-2026-1"));
        assertTrue(r.mentionsQuarkus().get("CVE-2026-2"));
    }

    @Test
    void noHourlyZipYetTodayIsNotAnErrorAndTheScanReachesMidnight() {
        Zips zips = new Zips().endOfDay("2026-11-23", "a/CVE-2026-1.json", "x");
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T00:00:00Z"), Instant.parse("2026-11-24T00:50:00Z"));
        assertEquals(Instant.parse("2026-11-24T00:00:00Z"), r.reached());
    }

    @Test
    void anHourInWhichNothingChangedHasAnEmptyZipAndThatIsNotAnError() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-24_delta_CVEs_at_1200Z.zip", zip());
        assertEquals(22, zip().length);
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-24T00:00:00Z"), NOON);
        assertTrue(r.mentionsQuarkus().isEmpty());
        assertEquals(1, r.zips());
    }

    @Test
    void aZipThatCannotBeReadIsAnError() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_end_of_day.zip", "not a zip".getBytes(StandardCharsets.UTF_8));
        // an error page or a truncated download must fail the run, not count as a day in which nothing changed
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CveDelta.read(zips, Instant.parse("2026-11-23T00:00:00Z"), NOON));
        assertTrue(e.getMessage().contains("not a zip"), e.getMessage());
    }
}
