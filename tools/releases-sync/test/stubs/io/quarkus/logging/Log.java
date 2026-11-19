package io.quarkus.logging;

/**
 * A stand-in for Quarkus' Log, for tests that run the sync logic with plain jbang. The real one only works inside
 * a Quarkus application, and jbang's Quarkus integration then starts an application instead of running the test's main.
 * Messages are discarded.
 */
public final class Log {

    private Log() {
    }

    public static void debugf(String format, Object... params) {
    }

    public static void infof(String format, Object... params) {
    }

    public static void warnf(String format, Object... params) {
    }

    public static void errorf(String format, Object... params) {
    }
}
