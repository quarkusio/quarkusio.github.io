package io.quarkus.cve.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Polite retry for upstream calls: exponential backoff with jitter, honours
 * {@code Retry-After} and GitHub's {@code X-RateLimit-Reset}, and gives up immediately on
 * errors that retrying cannot fix (4xx, malformed JSON) so we never hammer a struggling service.
 */
public final class Retry {

    private static final int MAX_ATTEMPTS = 4;
    private static final long BASE_DELAY_MS = 1_000;
    /** If an upstream asks us to wait longer than this, stop instead of sleeping. */
    private static final long MAX_WAIT_MS = 60_000;
    private static final Set<Integer> RETRYABLE_STATUS = Set.of(500, 502, 503, 504);

    /** Thrown when the upstream is rate limiting us for longer than we are willing to wait. */
    public static class RateLimitedException extends RuntimeException {
        public final Instant resetAt;

        RateLimitedException(String message, Instant resetAt, Throwable cause) {
            super(message, cause);
            this.resetAt = resetAt;
        }
    }

    private Retry() {}

    public static <T> T call(String what, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                Response r = e instanceof WebApplicationException w ? w.getResponse() : null;
                int status = r != null ? r.getStatus() : -1;
                boolean limited = status == 429 || (status == 403 && looksRateLimited(r));

                long waitMs;
                if (limited) {
                    waitMs = rateLimitWaitMs(r, attempt);
                    if (waitMs > MAX_WAIT_MS) {
                        Instant reset = Instant.now().plusMillis(waitMs);
                        throw new RateLimitedException(what + ": rate limited, resets at " + reset, reset, e);
                    }
                } else if (RETRYABLE_STATUS.contains(status) || (status == -1 && isTransient(e))) {
                    long ra = retryAfterMs(r);
                    waitMs = ra >= 0 ? ra : backoffMs(attempt);
                } else {
                    throw e;
                }

                if (attempt >= MAX_ATTEMPTS) {
                    if (limited) {
                        throw new RateLimitedException(what + ": still rate limited after " + attempt + " attempts",
                                Instant.now().plusMillis(waitMs), e);
                    }
                    throw e;
                }
                Log.log(1, "%s: %s; retrying in %.1fs (attempt %d/%d)",
                        what, describe(e, status), waitMs / 1000.0, attempt + 1, MAX_ATTEMPTS);
                sleep(waitMs);
            }
        }
    }

    private static boolean looksRateLimited(Response r) {
        return r != null && ("0".equals(r.getHeaderString("X-RateLimit-Remaining"))
                || r.getHeaderString("Retry-After") != null);
    }

    private static long rateLimitWaitMs(Response r, int attempt) {
        long ra = retryAfterMs(r);
        if (ra >= 0) return ra;
        if (r != null) {
            try {
                String reset = r.getHeaderString("X-RateLimit-Reset");
                if (reset != null) return Math.max(0, Long.parseLong(reset.trim()) * 1000 - System.currentTimeMillis()) + 1_000;
            } catch (NumberFormatException ignored) { /* fall through */ }
        }
        return backoffMs(attempt);
    }

    /** Retry-After as seconds; the HTTP-date form is ignored. -1 if absent. */
    private static long retryAfterMs(Response r) {
        if (r == null) return -1;
        String v = r.getHeaderString("Retry-After");
        if (v == null) return -1;
        try {
            return Math.max(0, Long.parseLong(v.trim())) * 1000;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static long backoffMs(int attempt) {
        return BASE_DELAY_MS * (1L << (attempt - 1)) + ThreadLocalRandom.current().nextLong(250);
    }

    /** Network-level failures (connect/read timeouts, resets) are transient; bad JSON is not. */
    private static boolean isTransient(Throwable t) {
        boolean io = false;
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof JsonProcessingException) return false;
            if (c instanceof IOException || c instanceof TimeoutException
                    || c.getClass().getSimpleName().contains("Timeout")) {
                io = true;
            }
        }
        return io;
    }

    private static String describe(Throwable e, int status) {
        return status != -1 ? "HTTP " + status : e.getClass().getSimpleName() + ": " + e.getMessage();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", ie);
        }
    }
}
