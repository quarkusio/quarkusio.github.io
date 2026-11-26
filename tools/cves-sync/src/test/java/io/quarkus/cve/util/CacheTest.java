package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheTest {

    private static final TypeReference<String> STRING_TYPE = new TypeReference<>() {};

    @AfterEach
    void reset() {
        Cache.configure(null, Cache.Mode.NORMAL);
    }

    @Test
    void distinctKeysWithPunctuationDoNotCollide(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);

        assertNotEquals(Cache.file("ns", "io.quarkus:foo"), Cache.file("ns", "io.quarkus_foo"));
        assertNotEquals(Cache.file("ns", "io.quarkus:foo"), Cache.file("ns", "io.quarkus/foo"));

        Cache.write("ns", "io.quarkus:foo", "colon-value", "\"etag-colon\"");
        Cache.write("ns", "io.quarkus_foo", "underscore-value", "\"etag-underscore\"");
        Cache.write("ns", "io.quarkus/foo", "slash-value", "\"etag-slash\"");

        Cache.Entry<String> colon = Cache.read("ns", "io.quarkus:foo", STRING_TYPE).orElseThrow();
        Cache.Entry<String> underscore = Cache.read("ns", "io.quarkus_foo", STRING_TYPE).orElseThrow();
        Cache.Entry<String> slash = Cache.read("ns", "io.quarkus/foo", STRING_TYPE).orElseThrow();

        assertEquals("colon-value", colon.data());
        assertEquals("\"etag-colon\"", colon.etag());
        assertEquals("underscore-value", underscore.data());
        assertEquals("\"etag-underscore\"", underscore.etag());
        assertEquals("slash-value", slash.data());
        assertEquals("\"etag-slash\"", slash.etag());
    }

    @Test
    void dotAndEmptyKeysStayInsideTheNamespaceDirectory(@TempDir Path dir) {
        Cache.configure(dir, Cache.Mode.NORMAL);
        for (String key : new String[] {"", ".", ".."}) {
            Path f = Cache.file("ns", key).normalize();
            assertEquals(dir.resolve("ns").normalize(), f.getParent());
            Cache.write("ns", key, "v-" + key, null);
            assertEquals("v-" + key, Cache.read("ns", key, STRING_TYPE).orElseThrow().data());
        }
    }

    @Test
    void mismatchedEnvelopeKeyIsTreatedAsAMiss(@TempDir Path dir) throws Exception {
        Cache.configure(dir, Cache.Mode.NORMAL);
        Path f = Cache.file("ns", "expected-key");
        Files.createDirectories(f.getParent());
        Files.writeString(f, """
                {"v":1,"key":"other-key","fetchedAt":"2026-11-25T00:00:00Z","data":"wrong"}
                """, StandardCharsets.UTF_8);

        assertTrue(Cache.read("ns", "expected-key", STRING_TYPE).isEmpty());
        assertTrue(Cache.readFallback("ns", "expected-key", STRING_TYPE).isEmpty());
    }
}

