package io.quarkus.cve.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The CVE records that changed since a point in time, read from the delta zips that the
 * {@code CVEProject/cvelistV5} repository publishes as release assets. A zip holds the full records of the CVEs
 * that changed, so there is nothing to look up per CVE, and downloading them needs neither an API nor a token. Each
 * record is checked for the word "quarkus".
 *
 * The zips are <em>cumulative over a day</em>: the one of hour H holds every record changed since 00:00 UTC until H,
 * so a later zip of the day holds all of an earlier one. What was changed in the last hour of a day (23:00 to 24:00)
 * is in no hourly zip of that day, nor in one of the next day, but only in the {@code end_of_day} zip, which is
 * published shortly after midnight. Reading therefore means:
 * <ul>
 *   <li>the end-of-day zip of every day from the day of the last scan up to yesterday, each of which holds the whole
 *       day. {@code cvelistV5} occasionally skips an {@code end_of_day} release (~1–2 days/year) and does not backfill
 *       it; for a past day older than yesterday, if {@code end_of_day} is missing we fall back to a late hourly zip
 *       ({@code >= 2200Z}) and advance past the day so nightly runs are not permanently blocked. If even that is
 *       missing, the run fails;</li>
 *   <li>for today only the newest hourly zip there is. If yesterday's end-of-day zip is not published yet, its newest
 *       hourly zip is read instead, and the scan only reaches that hour, so that the next run reads the end-of-day zip.</li>
 * </ul>
 * Some hours have no zip, or have one under the tag of the hour before; both are tried, and a missing hourly zip only
 * means that the newest one that exists is read.
 */
public final class CveDelta {

    /** Where the zips come from. Empty when there is no such zip. */
    public interface Source {
        Optional<byte[]> zip(String tag, String asset);
    }

    /**
     * @param mentionsQuarkus every CVE seen, and whether its latest record mentions Quarkus
     * @param reached         up to when the CVE list was read: the next run reads from the day of this time
     * @param zips            how many zips were read
     * @param bytes           their total size
     * @param read            what was read, for the log
     */
    public record Result(Map<String, Boolean> mentionsQuarkus, Instant reached, int zips, long bytes, List<String> read) {}

    /** Earliest hourly zip accepted as a fallback when an older past day has no end-of-day zip (at most 2 hours lost). */
    static final int PAST_DAY_MIN_HOUR = 22;

    private CveDelta() {}

    /**
     * Reads from the day of {@code scanned} up to {@code now}, oldest first, so that when a CVE is in several zips the
     * newest record decides.
     *
     * @throws IllegalStateException when a zip cannot be read, or a past day is missing both its end-of-day zip and
     *     any late hourly zip ({@code >= 2200Z}); the caller must not take a partial read for a complete one
     */
    public static Result read(Source source, Instant scanned, Instant now) {
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        Map<String, Boolean> seen = new LinkedHashMap<>();
        List<String> read = new ArrayList<>();
        Instant reached = scanned;
        long bytes = 0;
        boolean closed = true;

        for (LocalDate day = scanned.atZone(ZoneOffset.UTC).toLocalDate(); day.isBefore(today); day = day.plusDays(1)) {
            Optional<byte[]> zip = source.zip("cve_" + day + "_at_end_of_day", day + "_delta_CVEs_at_end_of_day.zip");
            if (zip.isPresent()) {
                bytes += zip.get().length;
                seen.putAll(records(zip.get(), day + " end of day"));
                read.add(day + " end of day");
                reached = later(reached, day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
                continue;
            }
            int minHour = day.equals(yesterday) ? 1 : PAST_DAY_MIN_HOUR;
            Optional<Hourly> hourly = newestHourly(source, day, 23, minHour);
            if (hourly.isEmpty()) {
                throw new IllegalStateException("the cvelistV5 end-of-day zip of " + day + " is not available and no late hourly zip (>="
                        + String.format(Locale.ROOT, "%02d00Z", minHour) + ") exists, so the changes of that day cannot be trusted");
            }
            bytes += hourly.get().zip().length;
            seen.putAll(records(hourly.get().zip(), day + " " + hourly.get().label()));
            if (day.equals(yesterday)) {
                read.add(day + " up to " + hourly.get().label() + " (end of day not published yet)");
                reached = later(reached, hourly.get().at());
                closed = false;
            } else {
                // cvelistV5 occasionally omits an end_of_day release and never backfills it; advance past the day using
                // its late hourly zip (>= 2200Z) so subsequent nightly runs are not permanently blocked.
                Log.log(1, "cvelistV5: no end-of-day zip for %s; using %s hourly zip and advancing past %s",
                        day, hourly.get().label(), day);
                read.add(day + " up to " + hourly.get().label() + " (end of day missing)");
                reached = later(reached, day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
            }
        }

        // Today, unless yesterday is still open: reaching today's hour would skip the end of yesterday
        if (closed) {
            Optional<Hourly> hourly = newestHourly(source, today, now.atZone(ZoneOffset.UTC).getHour(), 1);
            if (hourly.isPresent()) {
                bytes += hourly.get().zip().length;
                seen.putAll(records(hourly.get().zip(), today + " " + hourly.get().label()));
                read.add(today + " up to " + hourly.get().label());
                reached = later(reached, hourly.get().at());
            } else {
                reached = later(reached, today.atStartOfDay().toInstant(ZoneOffset.UTC));
            }
        }
        return new Result(seen, reached, read.size(), bytes, read);
    }

    private static Instant later(Instant a, Instant b) {
        return b.isAfter(a) ? b : a;
    }

    private record Hourly(byte[] zip, String label, Instant at) {}

    /**
     * The newest hourly zip of the day from {@code fromHour} down to {@code minHour} (the 00:00 zip has nothing of the day),
     * which holds the whole day up to its hour. A zip is sometimes published under the tag of the hour before.
     */
    private static Optional<Hourly> newestHourly(Source source, LocalDate day, int fromHour, int minHour) {
        for (int hour = fromHour; hour >= minHour; hour--) {
            String at = String.format(Locale.ROOT, "%02d00Z", hour);
            String asset = day + "_delta_CVEs_at_" + at + ".zip";
            Optional<byte[]> zip = source.zip("cve_" + day + "_" + at, asset);
            if (zip.isEmpty()) zip = source.zip("cve_" + day + "_" + String.format(Locale.ROOT, "%02d00Z", hour - 1), asset);
            if (zip.isPresent()) {
                return Optional.of(new Hourly(zip.get(), at, day.atStartOfDay().plusHours(hour).toInstant(ZoneOffset.UTC)));
            }
            Log.log(2, "cvelistV5: no delta zip for %s %s", day, at);
        }
        return Optional.empty();
    }

    /** The CVE records in one zip, by id, each with whether its text mentions Quarkus. */
    static Map<String, Boolean> records(byte[] zip, String what) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        // ZipInputStream takes any other content (an error page, say) for a zip with nothing in it. An hour in which
        // nothing changed is a real zip with nothing in it: just the 22 bytes of the end-of-archive record.
        if (!isZip(zip)) throw new IllegalStateException("the cvelistV5 delta zip of " + what + " is not a zip file");
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
            throw new IllegalStateException("could not read the cvelistV5 delta zip of " + what + ": " + ex.getMessage(), ex);
        }
        return out;
    }

    /** Starts with the signature of a local file header (a zip with files) or of the end-of-archive record (an empty one). */
    static boolean isZip(byte[] b) {
        return b.length >= 4 && b[0] == 'P' && b[1] == 'K' && ((b[2] == 3 && b[3] == 4) || (b[2] == 5 && b[3] == 6));
    }
}
