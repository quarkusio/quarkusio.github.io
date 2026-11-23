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
    }

    static final Instant NOON = Instant.parse("2026-11-23T12:00:00Z");

    @Test
    void aRecordIsWhetherItsTextMentionsQuarkusInAnyCase() {
        Map<String, Boolean> r = CveDelta.records(zip(
                "delta/cves/2026/1xxx/CVE-2026-1.json", "{\"title\": \"Quarkus-vertx-http bypass\"}",
                "delta/cves/2026/1xxx/CVE-2026-2.json", "{\"title\": \"Something else\"}",
                "delta/deltaLog.json", "quarkus is mentioned here but this is not a record",
                "delta/cves/2026/1xxx/CVE-2026-3.json", "{\"affected\": [{\"packageName\": \"io.quarkus/quarkus-qute\"}]}"),
                NOON);
        assertEquals(Map.of("CVE-2026-1", true, "CVE-2026-2", false, "CVE-2026-3", true), r);
    }

    @Test
    void zipsAreReadFromTwoHoursBeforeTheLastScanToNowAndNamedLikeTheRepositoryNamesThem() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1100Z.zip", zip("a/CVE-2026-1.json", "x"));
        CveDelta.read(zips, Instant.parse("2026-11-23T09:30:00Z"), Instant.parse("2026-11-23T11:20:00Z"));
        assertEquals(List.of(
                "cve_2026-11-23_0700Z 2026-11-23_delta_CVEs_at_0700Z.zip",
                "cve_2026-11-23_0800Z 2026-11-23_delta_CVEs_at_0800Z.zip",
                "cve_2026-11-23_0900Z 2026-11-23_delta_CVEs_at_0900Z.zip",
                "cve_2026-11-23_1000Z 2026-11-23_delta_CVEs_at_1000Z.zip",
                "cve_2026-11-23_1100Z 2026-11-23_delta_CVEs_at_1100Z.zip"), zips.asked.subList(0, 5));
    }

    @Test
    void theNewestRecordOfACveDecides() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1000Z.zip", zip("a/CVE-2026-1.json", "quarkus", "a/CVE-2026-2.json", "other"));
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1100Z.zip", zip("a/CVE-2026-1.json", "no longer", "a/CVE-2026-2.json", "now quarkus"));
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T10:00:00Z"), NOON);
        assertFalse(r.mentionsQuarkus().get("CVE-2026-1"));
        assertTrue(r.mentionsQuarkus().get("CVE-2026-2"));
        assertEquals(Instant.parse("2026-11-23T11:00:00Z"), r.lastZip());
        assertEquals(2, r.zips());
    }

    @Test
    void anHourWithoutAZipIsSkippedBecauseTheNextOneCoversIt() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_0800Z.zip", zip("a/CVE-2026-1.json", "quarkus"));
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1000Z.zip", zip("a/CVE-2026-2.json", "quarkus"));
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T08:00:00Z"), Instant.parse("2026-11-23T10:30:00Z"));
        assertEquals(Map.of("CVE-2026-1", true, "CVE-2026-2", true), r.mentionsQuarkus());
        assertEquals(List.of(Instant.parse("2026-11-23T06:00:00Z"), Instant.parse("2026-11-23T07:00:00Z"),
                Instant.parse("2026-11-23T09:00:00Z")), r.missing());
        assertEquals(Instant.parse("2026-11-23T10:00:00Z"), r.lastZip());
    }

    @Test
    void theReachedTimeStopsAtTheLastZipThatWasFoundAndNeverMovesBack() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_0900Z.zip", zip("a/CVE-2026-1.json", "x"));
        Instant scanned = Instant.parse("2026-11-23T10:00:00Z");
        // the zips of 10:00 and 11:00 are not published yet: the point reached is not moved to now
        assertEquals(scanned, CveDelta.read(zips, scanned, NOON).lastZip());
    }

    @Test
    void noZipAtAllOverSeveralHoursIsAnErrorNotAnEmptyDelta() {
        Zips zips = new Zips();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CveDelta.read(zips, Instant.parse("2026-11-22T12:00:00Z"), NOON));
        assertTrue(e.getMessage().contains("no cvelistV5 delta zip"), e.getMessage());
    }

    @Test
    void anHourInWhichNothingChangedHasAnEmptyZipAndThatIsNotAnError() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1100Z.zip", zip());
        assertEquals(22, zip().length);
        CveDelta.Result r = CveDelta.read(zips, Instant.parse("2026-11-23T11:00:00Z"), NOON);
        assertTrue(r.mentionsQuarkus().isEmpty());
        assertEquals(1, r.zips());
    }

    @Test
    void aZipThatCannotBeReadIsAnError() {
        Zips zips = new Zips();
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1100Z.zip", "not a zip".getBytes(StandardCharsets.UTF_8));
        zips.byAsset.put("2026-11-23_delta_CVEs_at_1000Z.zip", zip("a/CVE-2026-1.json", "quarkus"));
        // an error page or a truncated download must fail the run, not count as an hour in which nothing changed
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CveDelta.read(zips, Instant.parse("2026-11-23T10:00:00Z"), NOON));
        assertTrue(e.getMessage().contains("not a zip"), e.getMessage());
    }
}
