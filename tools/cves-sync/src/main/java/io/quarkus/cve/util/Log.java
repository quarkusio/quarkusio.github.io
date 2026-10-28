package io.quarkus.cve.util;

/**
 * Minimal leveled logger that writes to stderr (stdout stays clean for results/JSON).
 *
 * Levels:
 *   0 - quiet (default)
 *   1 - top-level stages and totals
 *   2 - per-batch progress, periodic counters, failures (one line each)
 *   3 - per-package / per-vulnerability activity
 *   4 - every HTTP request and response (method, URL, status, elapsed ms)
 *   5 - everything: HTTP headers and exception stack traces
 */
public final class Log {

    public static final int MIN = 0;
    public static final int MAX = 5;

    private static volatile int level = MIN;
    private static final long START = System.nanoTime();

    private Log() {}

    public static void setLevel(int newLevel) {
        level = Math.max(MIN, Math.min(MAX, newLevel));
    }

    public static int level() {
        return level;
    }

    public static boolean enabled(int lvl) {
        return level >= lvl;
    }

    /** Logs a formatted line to stderr if the current level is at least {@code lvl}. */
    public static void log(int lvl, String fmt, Object... args) {
        if (level < lvl) return;
        long ms = (System.nanoTime() - START) / 1_000_000;
        String msg = args.length == 0 ? fmt : String.format(fmt, args);
        System.err.printf("[%6.1fs][L%d] %s%n", ms / 1000.0, lvl, msg);
    }

    /** Always printed (any level): for conditions that make the results incomplete or stale. */
    public static void warn(String fmt, Object... args) {
        System.err.println("warning: " + (args.length == 0 ? fmt : String.format(fmt, args)));
    }

    /** Logs an exception: message at {@code lvl}, full stack trace at level 5. */
    public static void error(int lvl, String context, Throwable t) {
        if (level < lvl) return;
        log(lvl, "%s: %s: %s", context, t.getClass().getSimpleName(), t.getMessage());
        if (level >= MAX) t.printStackTrace(System.err);
    }
}
