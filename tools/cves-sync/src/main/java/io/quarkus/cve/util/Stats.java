package io.quarkus.cve.util;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
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
    public static final String CVE_ORG = "cve-org";

    private static final String[] PROVIDERS = {OSV, GITHUB, MAVEN_CENTRAL, QUARKUS_REGISTRY, CVE_ORG};

    private static final Map<String, LongAdder> HTTP = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> NOT_MODIFIED = new ConcurrentHashMap<>();
    private static final Map<String, LongAdder> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Detail> DETAIL = new ConcurrentHashMap<>();
    private static volatile long startNanos = System.nanoTime();

    /** What was asked of one host for one purpose. */
    private static final class Detail {
        final LongAdder requests = new LongAdder();
        final LongAdder notModified = new LongAdder();
        final LongAdder rateLimited = new LongAdder();
        final LongAdder failed = new LongAdder();
    }

    private Stats() {}

    /** Clears all counters and restarts the clock. */
    public static void reset() {
        HTTP.clear();
        NOT_MODIFIED.clear();
        CACHE.clear();
        DETAIL.clear();
        startNanos = System.nanoTime();
    }

    public static void httpRequest(URI uri) {
        HTTP.computeIfAbsent(providerOf(uri), k -> new LongAdder()).increment();
        detail(uri).requests.increment();
    }

    public static void notModified(URI uri) {
        NOT_MODIFIED.computeIfAbsent(providerOf(uri), k -> new LongAdder()).increment();
        detail(uri).notModified.increment();
    }

    /** Counts a response that was refused: as rate limited if the host is limiting us, else as failed (other 4xx/5xx). */
    public static void rejected(URI uri, boolean rateLimited) {
        if (rateLimited) detail(uri).rateLimited.increment();
        else detail(uri).failed.increment();
    }

    private static Detail detail(URI uri) {
        String host = uri != null && uri.getHost() != null ? uri.getHost() : "?";
        return DETAIL.computeIfAbsent(host + "\t" + reasonOf(uri), k -> new Detail());
    }

    /** Why we ask this URL: the purpose of the request, from its path. */
    static String reasonOf(URI uri) {
        String path = uri != null && uri.getPath() != null ? uri.getPath() : "";
        String host = uri != null && uri.getHost() != null ? uri.getHost() : "";
        if (host.endsWith("osv.dev")) {
            if (path.endsWith("/querybatch")) return "which packages have vulnerabilities (batch)";
            if (path.endsWith("/query")) return "vulnerabilities of one package (paged)";
            if (path.contains("/vulns/")) return "vulnerability details";
        } else if (host.endsWith("github.com")) {
            if (path.startsWith("/search/code")) return "code search for CVE records";
            if (path.startsWith("/advisories")) return "advisories of packages (50 per request)";
        } else if (host.endsWith("maven.org")) {
            if (path.endsWith("maven-metadata.xml")) return "release list (metadata)";
            if (path.endsWith("/quarkus-bom/")) return "release list (directory)";
            if (path.contains("/io/quarkus/platform/")) return "platform BOM";
            if (path.contains("/io/quarkus/quarkus-bom/")) return "core BOM";
        } else if (host.endsWith("quarkus.io")) {
            if (path.endsWith("/extensions/all")) return "extension list";
        } else if (host.endsWith("mitre.org") || host.endsWith("cve.org")) {
            if (path.startsWith("/api/cve/")) return "CVE record";
        }
        return "other " + path;
    }

    /**
     * The run's requests per host and purpose, as lines for the log. Meant to be printed whatever the outcome
     * of the run, so that a run that failed on a rate limit shows what was asked and what was refused.
     */
    public static java.util.List<String> summaryLines() {
        Map<String, Detail> sorted = new TreeMap<>(DETAIL);
        java.util.List<String> out = new java.util.ArrayList<>();
        long total = 0, free = 0, limited = 0, failed = 0;
        String lastHost = null;
        out.add(String.format("HTTP requests by host and purpose (%s)", "requests / not modified (304) / rate limited (429) / failed"));
        for (Map.Entry<String, Detail> e : sorted.entrySet()) {
            String[] hp = e.getKey().split("\t", 2);
            Detail d = e.getValue();
            if (!hp[0].equals(lastHost)) {
                out.add("  " + hp[0]);
                lastHost = hp[0];
            }
            out.add(String.format("    %-45s %5d / %4d / %3d / %3d", hp[1], d.requests.sum(), d.notModified.sum(),
                    d.rateLimited.sum(), d.failed.sum()));
            total += d.requests.sum();
            free += d.notModified.sum();
            limited += d.rateLimited.sum();
            failed += d.failed.sum();
        }
        out.add(String.format("  total %d requests, %d not modified, %d rate limited, %d failed, in %.1fs",
                total, free, limited, failed, elapsedMillis() / 1000.0));
        Map<String, Long> hits = cacheHits();
        long cached = hits.values().stream().mapToLong(Long::longValue).sum();
        out.add("  answered from the cache without a request: " + cached
                + (cached == 0 ? "" : " " + hits.entrySet().stream().filter(h -> h.getValue() > 0)
                        .map(h -> h.getKey() + "=" + h.getValue()).collect(java.util.stream.Collectors.joining(", ", "(", ")"))));
        return out;
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
        if (host.endsWith("mitre.org") || host.endsWith("cve.org")) return CVE_ORG;
        return host.isEmpty() ? "other" : host;
    }

    private static Map<String, Long> snapshot(Map<String, LongAdder> counters) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String p : PROVIDERS) out.put(p, counters.containsKey(p) ? counters.get(p).sum() : 0L);
        counters.forEach((k, v) -> out.putIfAbsent(k, v.sum()));
        return out;
    }
}
