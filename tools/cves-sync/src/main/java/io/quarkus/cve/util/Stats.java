package io.quarkus.cve.util;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Per-run counters: HTTP requests actually sent, responses that were 304 Not Modified, and cache hits,
 * all per upstream provider, plus the elapsed time. A cache hit is a lookup answered from the local
 * cache without any request (a conditional request answered with 304 counts as an HTTP request and
 * as a 304, not as a cache hit).
 */
public final class Stats {

    public static final String OSV = "osv";
    public static final String GITHUB = "github";
    public static final String MAVEN_CENTRAL = "maven-central";
    public static final String QUARKUS_REGISTRY = "quarkus-registry";

    private static final String[] PROVIDERS = {OSV, GITHUB, MAVEN_CENTRAL, QUARKUS_REGISTRY};

    private static final Map<String, LongAdder> HTTP = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> NOT_MODIFIED = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> CACHE = new ConcurrentHashMap<>();
    private static volatile long startNanos = System.nanoTime();

    private Stats() {}

    /** Clears all counters and restarts the clock. */
    public static void reset() {
        HTTP.clear();
        NOT_MODIFIED.clear();
        CACHE.clear();
        startNanos = System.nanoTime();
    }

    public static void httpRequest(URI uri) {
        HTTP.computeIfAbsent(providerOf(uri), k -> new LongAdder()).increment();
    }

    public static void notModified(URI uri) {
        NOT_MODIFIED.computeIfAbsent(providerOf(uri), k -> new LongAdder()).increment();
    }

    public static void cacheHit(String provider) {
        CACHE.computeIfAbsent(provider, k -> new LongAdder()).increment();
    }

    public static long elapsedMillis() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Requests per provider; every known provider is present, even with 0. */
    public static Map<String, Long> httpRequests() {
        return snapshot(HTTP);
    }

    /** 304 Not Modified responses per provider. */
    public static Map<String, Long> notModified() {
        return snapshot(NOT_MODIFIED);
    }

    public static Map<String, Long> cacheHits() {
        return snapshot(CACHE);
    }

    static String providerOf(URI uri) {
        String host = uri != null && uri.getHost() != null ? uri.getHost() : "";
        if (host.endsWith("osv.dev")) return OSV;
        if (host.endsWith("github.com")) return GITHUB;
        if (host.endsWith("maven.org")) return MAVEN_CENTRAL;
        if (host.endsWith("quarkus.io")) return QUARKUS_REGISTRY;
        return host.isEmpty() ? "other" : host;
    }

    private static Map<String, Long> snapshot(Map<String, LongAdder> counters) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String p : PROVIDERS) out.put(p, counters.containsKey(p) ? counters.get(p).sum() : 0L);
        counters.forEach((k, v) -> out.putIfAbsent(k, v.sum()));
        return out;
    }
}
