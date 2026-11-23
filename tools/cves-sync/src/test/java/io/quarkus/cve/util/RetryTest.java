package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryTest {

    static WebApplicationException limited(int status, String retryAfter, String remaining) {
        Response.ResponseBuilder b = Response.status(status).header("X-RateLimit-Resource", "code_search");
        if (retryAfter != null) b.header("Retry-After", retryAfter);
        if (remaining != null) b.header("X-RateLimit-Remaining", remaining);
        return new WebApplicationException(b.build());
    }

    @Test
    void waitsAsLongAsRetryAfterSaysAndThenTriesAgain() {
        AtomicInteger calls = new AtomicInteger();
        long start = System.nanoTime();
        String result = Retry.call("test", () -> {
            if (calls.incrementAndGet() == 1) throw limited(429, "1", "10");
            return "ok";
        });
        long waitedMs = (System.nanoTime() - start) / 1_000_000;
        assertEquals("ok", result);
        assertEquals(2, calls.get());
        assertTrue(waitedMs >= 1_000, "waited " + waitedMs + " ms, Retry-After was 1 s");
    }

    @Test
    void aRateLimitOverTheLimitIsNotWaitedForButReportedWithWhatGitHubSaid() {
        AtomicInteger calls = new AtomicInteger();
        long start = System.nanoTime();
        Retry.RateLimitedException e = assertThrows(Retry.RateLimitedException.class, () -> Retry.call("test", 60_000, () -> {
            calls.incrementAndGet();
            throw limited(429, "736", "10");
        }));
        assertEquals(1, calls.get(), "no request while we know we are refused");
        assertTrue((System.nanoTime() - start) / 1_000_000 < 1_000, "gave up without sleeping");
        assertTrue(e.getMessage().contains("resource=code_search"), e.getMessage());
        assertTrue(e.getMessage().contains("retry-after=736"), e.getMessage());
    }

    @Test
    void a403WithNoSignOfARateLimitIsAnErrorAndIsNotRetried() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(WebApplicationException.class, () -> Retry.call("test", () -> {
            calls.incrementAndGet();
            throw limited(403, null, "10");
        }));
        assertEquals(1, calls.get());
    }
}
