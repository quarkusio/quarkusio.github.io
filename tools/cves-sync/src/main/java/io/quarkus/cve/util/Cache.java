package io.quarkus.cve.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Small on-disk JSON cache that lets us avoid hitting upstream APIs needlessly.
 *
 * Layout: {@code <dir>/<namespace>/<sanitised-key>.json}, each file an envelope:
 * {@code {"v":1,"fetchedAt":"...","etag":"...","data":{...}}}.
 *
 * Writes are atomic (temp file + move) so an interrupted run never leaves a half-written
 * entry; unreadable or wrong-version entries are treated as a miss.
 */
public final class Cache {

    /** NORMAL: read and write. REFRESH: skip reads (re-fetch everything) but still write. OFF: no reads or writes. */
    public enum Mode { NORMAL, REFRESH, OFF }

    /** A cached value plus the metadata needed to decide whether it is still usable. */
    public record Entry<T>(T data, Instant fetchedAt, String etag) {
        public Duration age() {
            return Duration.between(fetchedAt, Instant.now());
        }

        public boolean fresh(Duration ttl) {
            return age().compareTo(ttl) < 0;
        }

        public String ageText() {
            long s = Math.max(0, age().getSeconds());
            if (s < 120) return s + "s";
            if (s < 7200) return (s / 60) + "m";
            return (s / 3600) + "h";
        }
    }

    private static final int SCHEMA = 1;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    private static volatile Path dir = defaultDir();
    private static volatile Mode mode = Mode.NORMAL;

    private Cache() {}

    public static void configure(Path cacheDir, Mode newMode) {
        dir = cacheDir != null ? cacheDir : defaultDir();
        mode = newMode;
        Log.log(2, "Cache: mode=%s dir=%s", mode, dir);
    }

    /** Order: CVE_UTIL_CACHE_DIR, then %LOCALAPPDATA% (Windows) / XDG_CACHE_HOME / ~/.cache. */
    public static Path defaultDir() {
        String env = System.getenv("CVE_UTIL_CACHE_DIR");
        if (env != null && !env.isBlank()) return Path.of(env);
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null && !local.isBlank()) return Path.of(local, "cve-util", "cache");
        }
        String xdg = System.getenv("XDG_CACHE_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg, "cve-util");
        return Path.of(System.getProperty("user.home"), ".cache", "cve-util");
    }

    /** Reads an entry of any age. Empty unless mode is NORMAL. Callers decide freshness. */
    public static <T> Optional<Entry<T>> read(String namespace, String key, TypeReference<T> type) {
        if (mode != Mode.NORMAL) return Optional.empty();
        return load(namespace, key, type);
    }

    /** Reads an entry to use as a last resort when the network fails. Honoured in NORMAL and REFRESH, not OFF. */
    public static <T> Optional<Entry<T>> readFallback(String namespace, String key, TypeReference<T> type) {
        if (mode == Mode.OFF) return Optional.empty();
        return load(namespace, key, type);
    }

    public static void write(String namespace, String key, Object data, String etag) {
        if (mode == Mode.OFF) return;
        Path tmp = null;
        try {
            Path file = file(namespace, key);
            Files.createDirectories(file.getParent());
            ObjectNode envelope = MAPPER.createObjectNode();
            envelope.put("v", SCHEMA);
            envelope.put("fetchedAt", Instant.now().toString());
            if (etag != null) envelope.put("etag", etag);
            envelope.set("data", MAPPER.valueToTree(data));

            tmp = Files.createTempFile(file.getParent(), ".tmp-", ".part");
            MAPPER.writeValue(tmp.toFile(), envelope);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            Log.log(5, "Cache write %s/%s", namespace, key);
        } catch (IOException | RuntimeException ex) {
            Log.error(2, "Cache write failed for " + namespace + "/" + key, ex);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) { /* best effort */ }
            }
        }
    }

    private static <T> Optional<Entry<T>> load(String namespace, String key, TypeReference<T> type) {
        Path file = file(namespace, key);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            JsonNode envelope = MAPPER.readTree(file.toFile());
            if (envelope.path("v").asInt() != SCHEMA || !envelope.hasNonNull("data")) {
                Log.log(2, "Cache entry %s/%s has unexpected format, ignoring", namespace, key);
                return Optional.empty();
            }
            T data = MAPPER.readerFor(type).readValue(envelope.get("data"));
            Instant fetchedAt = Instant.parse(envelope.path("fetchedAt").asText());
            String etag = envelope.hasNonNull("etag") ? envelope.get("etag").asText() : null;
            return Optional.of(new Entry<>(data, fetchedAt, etag));
        } catch (IOException | RuntimeException ex) {
            Log.error(2, "Cache read failed for " + namespace + "/" + key + " (treating as miss)", ex);
            return Optional.empty();
        }
    }

    /** Keys such as "io.quarkus:quarkus-core" contain characters that are illegal in file names. */
    private static Path file(String namespace, String key) {
        String safe = key.replaceAll("[^A-Za-z0-9._-]", "_");
        return dir.resolve(namespace).resolve(safe + ".json");
    }
}
