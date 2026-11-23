package io.quarkus.cve.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The CVE records that changed since a point in time, read from the hourly delta zips that the
 * {@code CVEProject/cvelistV5} repository publishes as release assets. A zip holds the full records of the CVEs
 * that changed in the hour, so there is nothing to look up per CVE, and downloading them needs neither an API
 * nor a token. Each record is checked for the word "quarkus".
 *
 * Not every hour has a zip under its own name: a zip can be published under the tag of the hour before, and some
 * hours have none. A zip holds what changed since the previous one that was published, so an hour without a zip
 * is covered by the next one that exists. What is read is therefore every zip that exists, and the point reached
 * is the hour of the last one found.
 */
public final class CveDelta {

    /** Reads are started this long before the point reached by the last run, in case a zip was still being filled. */
    static final Duration OVERLAP = Duration.ofHours(2);
    /** If no zip at all is found over this many hours, the source is not working and the run must not go on. */
    static final int MIN_HOURS_EXPECTING_A_ZIP = 4;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    /** Where the zips come from. Empty when there is no such zip (not an error: see the class comment). */
    public interface Source {
        Optional<byte[]> zip(String tag, String asset);
    }

    /**
     * @param mentionsQuarkus every CVE seen, and whether its latest record mentions Quarkus
     * @param lastZip         the hour of the last zip read; where the next read starts, less the overlap
     * @param zips            how many zips were read
     * @param bytes           their total size
     * @param missing         the hours that have no zip
     */
    public record Result(Map<String, Boolean> mentionsQuarkus, Instant lastZip, int zips, long bytes, List<Instant> missing) {}

    private CveDelta() {}

    /**
     * Reads every zip from a little before {@code scanned} up to {@code now}, oldest first, so that when a CVE is in
     * several zips the newest record decides.
     *
     * @throws IllegalStateException when a zip cannot be read, or none was found over a long enough time; the caller
     *     must not take a partial read for a complete one
     */
    public static Result read(Source source, Instant scanned, Instant now) {
        Instant first = scanned.minus(OVERLAP).truncatedTo(ChronoUnit.HOURS);
        Instant last = now.truncatedTo(ChronoUnit.HOURS);
        Map<String, Boolean> seen = new LinkedHashMap<>();
        List<Instant> missing = new ArrayList<>();
        Instant reached = scanned;
        int zips = 0;
        long bytes = 0;
        int hours = 0;
        for (Instant hour = first; !hour.isAfter(last); hour = hour.plus(1, ChronoUnit.HOURS)) {
            hours++;
            String day = DAY.format(hour);
            String at = String.format(Locale.ROOT, "%02d00Z", hour.atZone(ZoneOffset.UTC).getHour());
            Optional<byte[]> zip = source.zip("cve_" + day + "_" + at, day + "_delta_CVEs_at_" + at + ".zip");
            if (zip.isEmpty()) {
                missing.add(hour);
                Log.log(2, "cvelistV5: no delta zip for %s", hour);
                continue;
            }
            zips++;
            bytes += zip.get().length;
            seen.putAll(records(zip.get(), hour));
            if (hour.isAfter(reached)) reached = hour;
        }
        if (zips == 0 && hours >= MIN_HOURS_EXPECTING_A_ZIP) {
            throw new IllegalStateException("no cvelistV5 delta zip was found for the " + hours + " hours from " + first
                    + " to " + last + "; the list of changed CVEs cannot be trusted");
        }
        return new Result(seen, reached, zips, bytes, missing);
    }

    /** The CVE records in one zip, by id, each with whether its text mentions Quarkus. */
    static Map<String, Boolean> records(byte[] zip, Instant hour) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        // ZipInputStream takes any other content (an error page, say) for a zip with nothing in it. An hour in which
        // nothing changed is a real zip with nothing in it: just the 22 bytes of the end-of-archive record.
        if (!isZip(zip)) throw new IllegalStateException("the cvelistV5 delta zip of " + hour + " is not a zip file");
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                String name = e.getName();
                String base = name.substring(name.lastIndexOf('/') + 1);
                if (e.isDirectory() || !base.startsWith("CVE-") || !base.endsWith(".json")) continue;
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                out.put(base.substring(0, base.length() - ".json".length()),
                        text.toLowerCase(Locale.ROOT).contains("quarkus"));
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("could not read the cvelistV5 delta zip of " + hour + ": " + ex.getMessage(), ex);
        }
        return out;
    }

    /** Starts with the signature of a local file header (a zip with files) or of the end-of-archive record (an empty one). */
    static boolean isZip(byte[] b) {
        return b.length >= 4 && b[0] == 'P' && b[1] == 'K' && ((b[2] == 3 && b[3] == 4) || (b[2] == 5 && b[3] == 6));
    }
}
