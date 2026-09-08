package io.quarkusio;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LinkCrawlerTest extends BrowserTest {

    private static final int DEFAULT_MAX_PAGES = Integer.MAX_VALUE;
    private static final int DEFAULT_THREADS = 8;

    // --- Links that we know are wrong that we have in our docs to illustrate errors ---
    // renamed to security-oidc-code-flow-authentication; the /version/main/ match is a Roq migration issue
    private static final List<Pattern> DELIBERATE_ERRORS = List.of(
            Pattern.compile("^/version/([\\d.]+|main)/guides/security-openid-connect-web-authentication$"));

    // --- Known failures that need fixing ---
    // https://github.com/quarkusio/quarkusio.github.io/issues/1693
    private static final Set<String> KNOWN_FAILURES = Set.of(
            // sub-paths that don't exist as standalone pages,relative path issue related to Roq
            "/guides/building-native-image/getting-started-testing",
            "/guides/security-webauthn/all-config",
            "/guides/security-webauthn/security-authentication-mechanisms",
            // TODO: Roq migration issue — these redirects work on theJekyll site but not with Roq
            "/guides/hibernate-search-elasticsearch",
            "/guides/rest-client-multipart",
            "/guides/rest-client-reactive",
            "/guides/resteasy-reactive",
            "/guides/resteasy-reactive-migration",
            "/guides/security-openid-connect"
    );

    // --- Waiting for release ---
    private static final Pattern WAITING_FOR_RELEASE = Pattern.compile(
            // Generated config docs use a relative link:native-and-ssl.html which
            // resolves to e.g. /guides/native-and-ssl.html (404).
            // Fixed upstream in quarkusio/quarkus@43805585b1, expected in Quarkus 3.40.
            "^/guides/.*native-and-ssl\\.html$");

    // --- Will-not-backport: broken links in frozen versioned guide snapshots ---
    // Versioned guide snapshots are frozen copies of old releases. Broken
    // cross-references to guides that were renamed or removed can't be fixed
    // without backporting to old release branches, which we won't do.
    private static final List<Pattern> WILL_NOT_BACKPORT_PATTERNS = List.of(
            // file path links (link:src/main/resources/...) that aren't web pages
            Pattern.compile("^/version/[\\d.]+/guides/cassandra/src/"),
            // retired guide
            Pattern.compile("^/version/[\\d.]+/guides/deploying-to-openshift-S2I-howto$"),
            // renamed to opentelemetry-tracing
            Pattern.compile("^/version/[\\d.]+/guides/opentelemetry$"),
            // renamed to security-openid-connect-multitenancy-howto
            Pattern.compile("^/version/[\\d.]+/guides/security-openid-connect-multitenancy$"),
            // typo in cross-ref within the versioned snapshot
            Pattern.compile("^/version/[\\d.]+/guides/hibernate-search-stqndqlone-elasticsearch$"),
            // extensions search link in a frozen snapshot that resolves under /version/
            Pattern.compile("^/version/[\\d.]+/.*extensions/?\\?search-regex=kogito"),
            // native-and-ssl.html relative-link fix (Quarkus 3.40) won't be backported to snapshots
            Pattern.compile("^/version/[\\d.]+/guides/.*native-and-ssl\\.html$"),
            // cross-references to guides that were renamed/removed/retired after these snapshots
            Pattern.compile("^/version/[\\d.]+/guides/("
                    + "consul-config"
                    + "|micrometer"
                    + "|microprofile-health"
                    + "|microprofile-metrics"
                    + "|vault"
                    + "|security-authorization"
                    + "|security-authorization-web-endpoints-reference"
                    + "|security-authorization-of-web-endpoints-reference"
                    + "|registry\\.quarkus\\.io"
                    + "|registry\\.quarkus\\.io\\.maven\\.repo"
                    + "|security-oidc-bearer-token-authentication-concept"
                    + "|security-oidc-code-flow-authentication-concept"
                    + "|security-protect-service-applications-by-using-oidc-bearer-authentication-how-to"
                    + "|https:/quarkus\\.io/guides/dev-services"
                    + ")$")
    );

    // --- Known-broken fragment anchors (temporary, Roq regressions) ---
    // Some guides set :extension-status: but forget to include the
    // extension-status.adoc note block, so the generated status badge links to a
    // #extension-status-note anchor that is never rendered. The referring link is
    // emitted unconditionally by the extension-status treeprocessor, producing a
    // dead fragment on every affected guide (and each of its versioned snapshots,
    // including /version/main/), so it is excused everywhere.
    //
    // TODO: remove once https://github.com/quarkusio/quarkusio.github.io/pull/2892
    //       merges — it auto-injects the missing note block so the anchor exists.
    private static final Set<String> KNOWN_BROKEN_FRAGMENTS = Set.of(
            "extension-status-note");

    // --- Will-not-backport: fragment anchors fixed upstream on Quarkus main ---
    // The doc-reference cross-references were corrected on Quarkus main by
    // quarkusio/quarkus#55413 (merged 2026-07-13): doc-contribute-docs-howto.adoc
    // now links to the anchors that actually exist (#titles-headings, #categories,
    // #doc-structure), and those render fine. The old links below survive only in
    // the frozen numbered versioned snapshots and will not be backported, so they
    // are excused under /version/<number>/ only; the current guides stay strict.
    private static final Set<String> WILL_NOT_BACKPORT_FRAGMENTS = Set.of(
            "titles-and-headings",
            "document-attributes-and-variables",
            "document-structure");

    // --- Anchors that render fine today but are stale in frozen snapshots ---
    // These render correctly on the current guides (confirmed on both quarkus.io
    // and es.quarkus.io); only the frozen numbered versioned snapshots reference
    // the old ids. Excused under /version/<number>/ only, current guides stay
    // strict.
    private static final Set<String> STALE_IN_FROZEN_SNAPSHOTS = Set.of(
            "logging-adapters",
            "quarkus-vertx-http_quarkus-http-non-application-root-path");

    // --- #55413 anchors that Asciidoctor never emits (dead on all engines) ---
    // These anchors exist in the current guide source (added by
    // quarkusio/quarkus#55413) but were placed where Asciidoctor drops them, so
    // the fragments are dead everywhere — confirmed on both Roq (quarkus.io) and
    // Jekyll (es.quarkus.io), and across latest /guides/, /version/main/ and
    // snapshots:
    //   * [[s2i]] is stacked with [[openshift]] before the OpenShift section and
    //     only the last of two stacked block anchors survives, so #s2i is lost
    //     (both sites render id="openshift" but no id="s2i");
    //   * [[duration-note-anchor-...]] sits between a block title and a
    //     :no-duration-note: attribute entry + include::, and is not emitted at all
    //     (both sites: href= to it present, 0 matching id=).
    // TODO: needs an upstream quarkusio/quarkus fix (re-place the anchors); file an
    //       issue and reference it here, then remove once the anchors render.
    private static final Set<String> UNRENDERED_UPSTREAM_ANCHORS = Set.of(
            "s2i",
            "duration-note-anchor-quarkus-kubernetes_quarkus-kubernetes",
            "duration-note-anchor-quarkus-kubernetes_quarkus-knative",
            "duration-note-anchor-quarkus-kubernetes_quarkus-openshift",
            "duration-note-anchor-quarkus-kubernetes-client_quarkus-kubernetes-client");

    // --- Dead upstream anchors: broken on BOTH engines, everywhere ---
    // Each of these #fragment links points at an anchor id that Asciidoctor never
    // emits, on either the Roq (quarkus.io) or the jRuby/Jekyll (es.quarkus.io)
    // path, and on latest /guides/, /version/main/ and the numbered snapshots
    // alike. They are upstream content bugs in the guide sources
    // (quarkusio/quarkus, docs/src/main/asciidoc/) — a missing/renamed section
    // anchor or a stale referring link — not a site-generator issue, so they are
    // excused everywhere until fixed upstream. The full per-guide list is in
    // dead-upstream-anchors.md (to be filed as a quarkusio/quarkus issue).
    // TODO: file the upstream issue, reference it here, and drop each entry as the
    //       corresponding anchor starts rendering.
    private static final Set<String> DEAD_UPSTREAM_ANCHORS = Set.of(
            "analysis-configurer",
            "back-channel-logout",
            "basic-auth",
            "bean-reference-note-anchor",
            "build-analytics.quarkus-analytics-uri-base",
            "configuring-json-support",
            "coordination",
            "dev-mode",
            "devservices-configuration-free-databases",
            "duration-note-anchor-quarkus-knative-knative-config",
            "duration-note-anchor-quarkus-kubernetes-kubernetes-config",
            "duration-note-anchor-quarkus-mongodb-config-group-dev-services-build-time-config",
            "duration-note-anchor-quarkus-openshift-openshift-config",
            "duration-note-anchor-quarkus-rest-client-config_quarkus-rest-client",
            "embedded-roles",
            "embedded-users",
            "infinispan-annotations-api",
            "integration-with-github-and-other-oauth2-providers",
            "multi-module-maven",
            "multipart-support",
            "mutual-TLS-authentication",
            "proof-of-key-for-code-exchange-pkce",
            "quarkus-datasource_quarkus.datasource.db-version",
            "quarkus-vertx-http_quarkus.http.auth.session.encryption-key",
            "roles-properties",
            "security-identity-customization",
            "service-binding",
            "standard-security-annotations",
            "synthetic_beans",
            "syslog-configuration",
            "token-propagation",
            "users-properties",
            "webjar-locator-support");

    private static final Set<String> PRODUCTION_HOSTS = Set.of("quarkus.io", "www.quarkus.io");

    // Paths that should NOT be rewritten to localhost - they are served from different repos
    private static final Set<String> EXTERNAL_QUARKUSIO_PATHS = Set.of(
            "/extensions",
            "/benchmarks",
            "/quarkus-workshops",
            "/quarkus-workshop-langchain4j"
    );

    // URLs that should never be crawled. AsciiDoc auto-links URL literals in
    // prose (e.g. "https://quarkus.io/issuer" in the security-jwt guide) even
    // though they're not navigation links.
    private static final Set<String> DO_NOT_VISIT = Set.of(
            "https://quarkus.io/issuer",
            "/issuer"
    );

    private static final Pattern HREF_PATTERN = Pattern.compile(
            "<a\\s[^>]*?href\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern META_REFRESH_PATTERN = Pattern.compile(
            "<meta\\s[^>]*?(?:" +
                    "http-equiv\\s*=\\s*[\"']?refresh[\"']?[^>]*?content\\s*=\\s*[\"']?\\d+\\s*;\\s*url=([^\"'\\s>]+)" +
                    "|" +
                    "content\\s*=\\s*[\"']?\\d+\\s*;\\s*url=([^\"'\\s>]+)[^>]*?http-equiv\\s*=\\s*[\"']?refresh[\"']?" +
                    ")",
            Pattern.CASE_INSENSITIVE);

    private CrawlResults crawlResults;

    @BeforeEach
    @Override
    void createContext() {
    }

    @AfterEach
    @Override
    void closeContext() {
    }

    private synchronized CrawlResults getCrawlResults() throws InterruptedException {
        if (crawlResults != null) {
            return crawlResults;
        }
        crawlResults = runCrawl();
        return crawlResults;
    }

    @Test
    void crawlAndCheckLinks() throws InterruptedException {
        CrawlResults results = getCrawlResults();

        if (!results.brokenLinks.isEmpty()) {
            List<Map.Entry<String, BrokenLink>> unexpected = new ArrayList<>();
            int knownCount = 0;
            for (var entry : results.brokenLinks.entrySet()) {
                if (isKnownBrokenLink(entry.getKey())) {
                    knownCount++;
                } else {
                    unexpected.add(entry);
                }
            }
            if (knownCount > 0) {
                System.out.println("Skipped " + knownCount + " known broken link(s)");
            }
            if (!unexpected.isEmpty()) {
                unexpected.sort(Map.Entry.comparingByKey());
                fail("Found " + unexpected.size() + " broken link(s):\n" + buildLinkReport(unexpected));
            }
        }
    }

    private boolean isKnownBrokenLink(String url) {
        String path = normalizePath(url);

        if (isDeliberateError(path)) {
            return true;
        }

        if (KNOWN_FAILURES.contains(path)) {
            return true;
        }
        if (WAITING_FOR_RELEASE.matcher(path).find()) {
            return true;
        }
        for (Pattern p : WILL_NOT_BACKPORT_PATTERNS) {
            if (p.matcher(path).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDeliberateError(String path) {
        for (Pattern p : DELIBERATE_ERRORS) {
            if (p.matcher(path).find()) {
                return true;
            }
        }
        return false;
    }

    private static String stripHost(String url) {
        return url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url;
    }

    private static String normalizePath(String url) {
        String path = stripHost(url);
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Test
    void crawlAndCheckImages() throws InterruptedException {
        CrawlResults results = getCrawlResults();

        if (!results.brokenImages.isEmpty()) {
            List<Map.Entry<String, BrokenImage>> sorted = new ArrayList<>(results.brokenImages.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            fail("Found " + results.brokenImages.size() + " broken image(s):\n" + buildImageReport(sorted));
        }
    }

    @Test
    void crawlAndCheckFragmentAnchors() throws InterruptedException {
        CrawlResults results = getCrawlResults();

        if (!results.brokenFragments.isEmpty()) {
            List<Map.Entry<String, FragmentLink>> sorted = new ArrayList<>(results.brokenFragments.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            fail("Found " + results.brokenFragments.size() + " broken fragment anchor(s):\n" + buildFragmentReport(sorted));
        }
    }

    private CrawlResults runCrawl() throws InterruptedException {
        int maxPages = Integer.getInteger("test.crawl.max-pages", DEFAULT_MAX_PAGES);
        int threads = Integer.getInteger("test.crawl.threads", DEFAULT_THREADS);
        boolean checkInternal = Boolean.parseBoolean(System.getProperty("test.crawl.check-internal", "true"));
        boolean checkExternal = Boolean.parseBoolean(System.getProperty("test.crawl.check-external", "false"));
        List<String> excludePaths = parseExcludePaths(System.getProperty("test.crawl.exclude-paths", ""));
        List<String> changedPaths = parseExcludePaths(System.getProperty("test.crawl.changed-paths", ""));

        Set<String> visited = ConcurrentHashMap.newKeySet();
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        Map<String, BrokenLink> brokenLinks = new ConcurrentHashMap<>();
        Map<String, BrokenImage> brokenImages = new ConcurrentHashMap<>();
        Map<String, String> referrers = new ConcurrentHashMap<>();
        Set<String> checkedExternal = ConcurrentHashMap.newKeySet();
        Set<String> checkedImages = ConcurrentHashMap.newKeySet();
        Map<String, FragmentLink> fragmentLinks = new ConcurrentHashMap<>();
        // Element ids present on each crawled page, keyed by normalized URL.
        // Collected during the crawl so fragment anchors can be verified in
        // memory afterwards instead of re-navigating to every page.
        Map<String, Set<String>> pageIds = new ConcurrentHashMap<>();
        AtomicInteger crawledCount = new AtomicInteger();

        Set<String> seedUrls = ConcurrentHashMap.newKeySet();
        AtomicInteger pendingWork = new AtomicInteger();
        if (!changedPaths.isEmpty()) {
            System.out.println("Incremental mode: checking " + changedPaths.size() + " changed page(s)");
            for (String path : changedPaths) {
                String url = normalize(baseUrl + path);
                seedUrls.add(url);
                pendingWork.incrementAndGet();
                queue.add(url);
            }
        } else {
            pendingWork.incrementAndGet();
            queue.add(normalize(baseUrl + "/"));
        }

        var done = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try (Playwright pw = Playwright.create()) {
                    Browser br = pw.chromium().launch(
                            new BrowserType.LaunchOptions().setHeadless(true));
                    BrowserContext ctx = br.newContext();
                    ctx.route("**/*.{css,woff,woff2,ttf,eot}", Route::abort);
                    Page p = ctx.newPage();
                    p.setDefaultNavigationTimeout(60_000);

                    crawLoop(p, queue, visited, brokenLinks, brokenImages, referrers,
                            checkedExternal, checkedImages, fragmentLinks, pageIds, crawledCount, maxPages,
                            checkInternal, checkExternal, excludePaths, seedUrls,
                            pendingWork, done);

                    ctx.close();
                    br.close();
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(2000);
            if (pendingWork.get() <= 0) {
                done.set(true);
                break;
            }
        }
        executor.awaitTermination(30, TimeUnit.MINUTES);

        List<Map.Entry<String, BrokenLink>> unknownBroken = brokenLinks.entrySet().stream()
                .filter(e -> !isKnownBrokenLink(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        List<Map.Entry<String, BrokenLink>> deliberateErrors = brokenLinks.entrySet().stream()
                .filter(e -> isDeliberateError(normalizePath(e.getKey())))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        Map<String, FragmentLink> brokenFragments = verifyFragments(fragmentLinks, pageIds);

        long knownBroken = brokenLinks.size() - unknownBroken.size() - deliberateErrors.size();
        String summary = "Crawled " + crawledCount.get() + " internal pages"
                + (checkExternal ? ", checked " + checkedExternal.size() + " external links" : "")
                + ", checked " + checkedImages.size() + " unique images"
                + ", found " + unknownBroken.size() + " broken links"
                + (knownBroken > 0 ? " (+ " + knownBroken + " known excluded)" : "")
                + ", found " + brokenImages.size() + " broken images"
                + ", found " + brokenFragments.size() + " broken fragment anchors"
                + " (" + threads + " threads)";
        System.out.println(summary);

        try {
            Path summaryFile = Path.of("target", "crawl-summary.txt");
            Files.createDirectories(summaryFile.getParent());
            StringBuilder sb = new StringBuilder(summary).append("\n");

            int cap = 20;
            if (!unknownBroken.isEmpty()) {
                sb.append("\n**Broken links");
                if (unknownBroken.size() > cap) {
                    sb.append(" (first ").append(cap).append(" of ").append(unknownBroken.size()).append(")");
                }
                sb.append(":**\n");
                for (var entry : unknownBroken.subList(0, Math.min(cap, unknownBroken.size()))) {
                    BrokenLink link = entry.getValue();
                    sb.append("- `").append(link.status).append("` ").append(entry.getKey());
                    if (link.referrer != null) {
                        sb.append(" ← ").append(link.referrer);
                    }
                    sb.append("\n");
                }
            }

            if (!brokenImages.isEmpty()) {
                List<Map.Entry<String, BrokenImage>> sorted = new ArrayList<>(brokenImages.entrySet());
                sorted.sort(Map.Entry.comparingByKey());
                sb.append("\n**Broken images");
                if (sorted.size() > cap) {
                    sb.append(" (first ").append(cap).append(" of ").append(sorted.size()).append(")");
                }
                sb.append(":**\n");
                for (var entry : sorted.subList(0, Math.min(cap, sorted.size()))) {
                    BrokenImage img = entry.getValue();
                    sb.append("- `").append(img.status).append("` ").append(entry.getKey());
                    if (img.referrer != null) {
                        sb.append(" ← ").append(img.referrer);
                    }
                    sb.append("\n");
                }
            }

            if (!brokenFragments.isEmpty()) {
                List<Map.Entry<String, FragmentLink>> sorted = new ArrayList<>(brokenFragments.entrySet());
                sorted.sort(Map.Entry.comparingByKey());
                sb.append("\n**Broken fragment anchors");
                if (sorted.size() > cap) {
                    sb.append(" (first ").append(cap).append(" of ").append(sorted.size()).append(")");
                }
                sb.append(":**\n");
                for (var entry : sorted.subList(0, Math.min(cap, sorted.size()))) {
                    FragmentLink frag = entry.getValue();
                    sb.append("- `#").append(frag.fragment).append("` ").append(frag.url);
                    if (frag.referrer != null) {
                        sb.append(" ← ").append(frag.referrer);
                    }
                    sb.append("\n");
                }
            }

            Files.writeString(summaryFile, sb.toString());
        } catch (IOException ignored) {
        }

        return new CrawlResults(brokenLinks, brokenImages, brokenFragments);
    }

    private void crawLoop(Page p,
                           LinkedBlockingQueue<String> queue,
                           Set<String> visited,
                           Map<String, BrokenLink> brokenLinks,
                           Map<String, BrokenImage> brokenImages,
                           Map<String, String> referrers,
                           Set<String> checkedExternal,
                           Set<String> checkedImages,
                           Map<String, FragmentLink> fragmentLinks,
                           Map<String, Set<String>> pageIds,
                           AtomicInteger crawledCount,
                           int maxPages,
                           boolean checkInternal,
                           boolean checkExternal,
                           List<String> excludePaths,
                           Set<String> seedUrls,
                           AtomicInteger pendingWork,
                           AtomicBoolean done) {
        boolean incrementalMode = !seedUrls.isEmpty();
        while (!done.get()) {
            if (crawledCount.get() >= maxPages) {
                break;
            }
            String currentUrl;
            try {
                currentUrl = queue.poll(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (currentUrl == null) {
                continue;
            }

            try {
                String normalizedUrl = normalize(currentUrl);
                if (!visited.add(normalizedUrl)) {
                    continue;
                }
                crawledCount.incrementAndGet();

                Response response = navigateWithRetry(p, currentUrl);
                if (response == null) {
                    try {
                        if (!currentUrl.equals(normalize(p.url()))) {
                            continue;
                        }
                    } catch (Exception ignored) {
                    }
                    if (checkInternal) {
                        BrokenLink probe = probeWithHttp(currentUrl);
                        if (probe != null) {
                            brokenLinks.put(currentUrl, new BrokenLink(probe.status, probe.statusText, referrers.get(normalizedUrl)));
                        }
                    }
                    continue;
                }

                int status = response.status();
                if (status >= 400) {
                    if (checkInternal) {
                        brokenLinks.put(currentUrl, new BrokenLink(status, response.statusText(), referrers.get(normalizedUrl)));
                    }
                    continue;
                }

                if (incrementalMode && !seedUrls.contains(normalizedUrl)) {
                    continue;
                }

            // In incremental mode, only extract links from seed pages (the
            // changed pages). Non-seed pages are visited only to verify their
            // status — a depth-1 check from each changed page.
            if (incrementalMode && !seedUrls.contains(currentUrl)) {
                continue;
            }

                // Use the browser's actual URL (after redirects) as the base for
                // resolving relative URLs — e.g. /newsletter/18 redirects to
                // /newsletter/18/ and relative src="index_files/img.png" must
                // resolve against the trailing-slash form.
                String pageUrl = p.url();

                // Some pages (old guide stubs) have JS redirects to production.
                // If the browser navigated away from localhost, skip link
                // extraction so we don't queue production URLs.
                if (!pageUrl.startsWith(baseUrl)) {
                    continue;
                }

                List<String> hrefs;
                List<String> imageSrcs;
                try {
                    @SuppressWarnings("unchecked")
                    var linkResult = (List<String>) p.evaluate(
                            "() => [...document.querySelectorAll('a[href]')].map(a => a.getAttribute('href'))");
                    hrefs = linkResult;

                    @SuppressWarnings("unchecked")
                    var imageResult = (List<String>) p.evaluate("""
                            () => {
                              const srcs = new Set();
                              document.querySelectorAll('img[src]').forEach(img => srcs.add(img.getAttribute('src')));
                              document.querySelectorAll('img[srcset], source[srcset]').forEach(el => {
                                el.getAttribute('srcset').split(',').forEach(entry => {
                                  const url = entry.trim().split(/\\s+/)[0];
                                  if (url) srcs.add(url);
                                });
                              });
                              return [...srcs];
                            }""");
                    imageSrcs = imageResult;

                    // Capture every element id on the page while it is loaded, so
                    // fragment anchors can be verified in memory later without a
                    // second navigation pass.
                    @SuppressWarnings("unchecked")
                    var idResult = (List<String>) p.evaluate(
                            "() => [...document.querySelectorAll('[id]')].map(e => e.id)");
                    pageIds.put(normalizedUrl, new HashSet<>(idResult));
                } catch (PlaywrightException e) {
                    hrefs = extractLinksViaHttp(currentUrl);
                    if (hrefs == null) {
                        if (checkInternal) {
                            brokenLinks.put(currentUrl, new BrokenLink(0, e.getMessage(), referrers.get(normalizedUrl)));
                        }
                        continue;
                    }
                    imageSrcs = List.of();
                }

                for (String href : hrefs) {
                    if (href == null || href.isBlank()) {
                        continue;
                    }

                    ResolvedLink resolved = resolveLink(pageUrl, href);
                    if (resolved == null) {
                        continue;
                    }

                    if (resolved.internal) {
                        String normalized = normalize(resolved.url);
                        if (!visited.contains(normalized) && !isExcluded(normalized, excludePaths)) {
                            pendingWork.incrementAndGet();
                            queue.add(resolved.url);
                            referrers.putIfAbsent(normalized, currentUrl);
                        }
                        if (resolved.fragment != null) {
                            String key = normalized + "#" + resolved.fragment;
                            fragmentLinks.putIfAbsent(key,
                                    new FragmentLink(normalized, resolved.fragment, currentUrl));
                        }
                    } else if (checkExternal && checkedExternal.add(resolved.url)) {
                        BrokenLink result = checkExternalLink(resolved.url);
                        if (result != null) {
                            brokenLinks.put(resolved.url, new BrokenLink(result.status, result.statusText, currentUrl));
                        }
                    }
                }

                for (String src : imageSrcs) {
                    if (src == null || src.isBlank() || src.startsWith("data:")) {
                        continue;
                    }

                    String resolvedImage = resolveImageUrl(pageUrl, src);
                    if (resolvedImage == null || !resolvedImage.startsWith(baseUrl)) {
                        continue;
                    }

                    if (checkedImages.add(resolvedImage)) {
                        BrokenImage result = checkImageUrl(resolvedImage);
                        if (result != null) {
                            brokenImages.put(resolvedImage, new BrokenImage(result.status, result.statusText, currentUrl));
                        }
                    }
                }
            } finally {
                pendingWork.decrementAndGet();
            }
        }
    }

    private static Response navigateWithRetry(Page p, String url) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Response response = p.navigate(url,
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                return response;
            } catch (Exception e) {
                if (attempt == 0) {
                    continue;
                }
            }
        }
        return null;
    }

    private ResolvedLink resolveLink(String currentPageUrl, String href) {
        if (href.startsWith("mailto:") || href.startsWith("javascript:")
                || href.startsWith("tel:")
                || DO_NOT_VISIT.contains(href)) {
            return null;
        }

        if (href.equals("#")) {
            return null;
        }

        if (href.startsWith("#")) {
            String fragment = href.substring(1);
            if (fragment.isEmpty()) {
                return null;
            }
            return new ResolvedLink(currentPageUrl, true, fragment);
        }

        String resolved;
        boolean internal;
        if (href.startsWith("http://") || href.startsWith("https://")) {
            if (isLocalhostUrl(href)) {
                return null;
            }
            resolved = rewriteToLocal(href);
            internal = resolved.startsWith(baseUrl);
        } else {
            try {
                resolved = resolveRelativeUrl(currentPageUrl, href);
            } catch (Exception e) {
                return null;
            }
            if (resolved == null) {
                return null;
            }
            internal = resolved.startsWith(baseUrl);
        }

        String fragment = null;
        int fragmentIndex = resolved.indexOf('#');
        if (fragmentIndex >= 0) {
            fragment = resolved.substring(fragmentIndex + 1);
            if (fragment.isEmpty()) {
                fragment = null;
            }
            resolved = resolved.substring(0, fragmentIndex);
        }

        return new ResolvedLink(resolved, internal, fragment);
    }

    private static String resolveImageUrl(String currentPageUrl, String src) {
        if (src.startsWith("http://") || src.startsWith("https://")) {
            return src;
        }
        return resolveRelativeUrl(currentPageUrl, src);
    }

    private static String resolveRelativeUrl(String base, String relative) {
        try {
            URI resolved = URI.create(base).resolve(relative).normalize();
            String path = resolved.getPath();
            while (path.startsWith("/../")) {
                path = path.substring(3);
            }
            return new URI(resolved.getScheme(), resolved.getAuthority(), path,
                    resolved.getQuery(), resolved.getFragment()).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static BrokenImage checkImageUrl(String url) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try (HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();
                if (status >= 400) {
                    if (attempt == 0) {
                        continue;
                    }
                    return new BrokenImage(status, "HTTP " + status, null);
                }
                return null;
            } catch (Exception e) {
                if (attempt == 0) {
                    continue;
                }
                return new BrokenImage(0, e.getMessage(), null);
            }
        }
        return null;
    }

    private static BrokenLink checkExternalLink(String url) {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 400) {
                return new BrokenLink(status, "HTTP " + status, null);
            }
            return null;
        } catch (Exception e) {
            return new BrokenLink(0, e.getMessage(), null);
        }
    }

    private static List<String> extractLinksViaHttp(String url) {
        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return null;
            }
            List<String> hrefs = new ArrayList<>();
            Matcher m = HREF_PATTERN.matcher(response.body());
            while (m.find()) {
                hrefs.add(m.group(1));
            }
            return hrefs;
        } catch (Exception e) {
            return null;
        }
    }

    private BrokenLink probeWithHttp(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return new BrokenLink(0, "unsupported scheme", null);
        }

        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 400) {
                return new BrokenLink(status, "HTTP " + status, null);
            }

            String body = response.body();
            if (body != null) {
                Matcher m = META_REFRESH_PATTERN.matcher(body);
                if (m.find()) {
                    String target = m.group(1) != null ? m.group(1) : m.group(2);
                    target = target.strip();
                    target = rewriteToLocal(target);
                    if (target.startsWith(baseUrl)) {
                        BrokenLink targetCheck = checkUrlReachable(target, url);
                        if (targetCheck != null) {
                            return new BrokenLink(targetCheck.status,
                                    "meta-refresh target unreachable: " + target + " (" + targetCheck.statusText + ")", null);
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return new BrokenLink(0, e.getMessage(), null);
        }
    }

    private static BrokenLink checkUrlReachable(String url, String sourceUrl) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            try {
                url = URI.create(sourceUrl).resolve(url).toString();
            } catch (IllegalArgumentException e) {
                return new BrokenLink(0, "invalid redirect target: " + url, null);
            }
        }

        try (HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 400) {
                return new BrokenLink(status, "HTTP " + status, null);
            }
            return null;
        } catch (Exception e) {
            return new BrokenLink(0, e.getMessage(), null);
        }
    }

    private String rewriteToLocal(String target) {
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            return target;
        }
        try {
            URI targetUri = URI.create(target);
            String host = targetUri.getHost();
            if (host != null && PRODUCTION_HOSTS.contains(host)) {
                String path = targetUri.getPath();
                if (path != null && !path.isEmpty()) {
                    // Don't rewrite paths that are served from other repositories
                    for (String externalPath : EXTERNAL_QUARKUSIO_PATHS) {
                        if (path.equals(externalPath) || path.startsWith(externalPath + "/")) {
                            return target;  // Keep original production URL
                        }
                    }
                    return baseUrl + path;
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
        return target;
    }

    private static String buildLinkReport(List<Map.Entry<String, BrokenLink>> entries) {
        StringBuilder sb = new StringBuilder();
        for (var entry : entries) {
            BrokenLink link = entry.getValue();
            sb.append("  ").append(link.status).append(" ").append(entry.getKey());
            if (link.referrer != null) {
                sb.append("\n       linked from: ").append(link.referrer);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String buildImageReport(List<Map.Entry<String, BrokenImage>> entries) {
        StringBuilder sb = new StringBuilder();
        for (var entry : entries) {
            BrokenImage img = entry.getValue();
            sb.append("  ").append(img.status).append(" ").append(entry.getKey());
            if (img.referrer != null) {
                sb.append("\n       found on: ").append(img.referrer);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String buildFragmentReport(List<Map.Entry<String, FragmentLink>> entries) {
        StringBuilder sb = new StringBuilder();
        for (var entry : entries) {
            FragmentLink frag = entry.getValue();
            sb.append("  ").append(frag.url).append("#").append(frag.fragment);
            sb.append("\n       linked from: ").append(frag.referrer);
            sb.append("\n");
        }
        return sb.toString();
    }

    private Map<String, FragmentLink> verifyFragments(Map<String, FragmentLink> fragmentLinks,
            Map<String, Set<String>> pageIds) {
        if (fragmentLinks.isEmpty()) {
            return Map.of();
        }

        Map<String, FragmentLink> broken = new ConcurrentHashMap<>();
        int knownBroken = 0;

        List<Map.Entry<String, FragmentLink>> sorted = new ArrayList<>(fragmentLinks.entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        // Pages whose ids we did not capture during the crawl (e.g. incremental
        // mode, off-site redirects). These are verified on demand by navigating,
        // preserving the original coverage.
        Map<String, Set<String>> fallbackIds = new HashMap<>();
        Page p = null;
        BrowserContext ctx = null;

        for (var entry : sorted) {
            FragmentLink link = entry.getValue();

            // Skip query-style fragments (e.g. #q=spring) used by search.quarkus.io
            // See https://github.com/quarkusio/search.quarkus.io/pull/597
            if (link.fragment.contains("=")) {
                continue;
            }

            Set<String> ids = pageIds.get(link.url);
            if (ids == null) {
                // Not seen during the crawl — fetch it once and cache the ids.
                ids = fallbackIds.get(link.url);
                if (ids == null) {
                    if (p == null) {
                        ctx = browser.newContext();
                        ctx.route("**/*.{css,png,jpg,jpeg,gif,svg,ico,woff,woff2,ttf,eot}", Route::abort);
                        p = ctx.newPage();
                        p.setDefaultNavigationTimeout(30_000);
                    }
                    ids = fetchPageIds(p, link.url);
                    fallbackIds.put(link.url, ids);
                }
            }

            // A null id set means the page could not be loaded (4xx, redirect
            // off-site, etc.); match the previous behaviour and skip it.
            if (ids == FetchFailed.INSTANCE) {
                continue;
            }

            if (!ids.contains(link.fragment)) {
                if (isKnownBrokenFragment(link.url, link.fragment)) {
                    knownBroken++;
                    continue;
                }
                broken.put(entry.getKey(), link);
            }
        }

        if (ctx != null) {
            ctx.close();
        }

        System.out.println("Checked " + fragmentLinks.size() + " fragment anchors, found "
                + broken.size() + " broken"
                + (knownBroken > 0 ? " (+ " + knownBroken + " known excluded)" : ""));

        return broken;
    }

    /**
     * A fragment link is a known-broken anchor we tolerate so the check can gate
     * real regressions. Covers three cases:
     * <ul>
     *   <li>{@link #KNOWN_BROKEN_FRAGMENTS} (#2892) — the #extension-status-note
     *       anchor guides forget to include; excused everywhere;</li>
     *   <li>the {@code {summaryTableId}} Roq regression (#2962) — the config
     *       duration/memory note anchors are emitted with the literal attribute
     *       name instead of the resolved id;</li>
     *   <li>the {@link #UNRENDERED_UPSTREAM_ANCHORS} block anchors — present in
     *       source but never emitted by Asciidoctor (both engines), so dead
     *       everywhere;</li>
     *   <li>the {@link #DEAD_UPSTREAM_ANCHORS} — upstream guide content bugs
     *       (missing/renamed anchor or stale link) dead on both engines and
     *       everywhere, listed in dead-upstream-anchors.md;</li>
     *   <li>{@link #WILL_NOT_BACKPORT_FRAGMENTS} (quarkus#55413) — fixed on main but
     *       not backported, so only excused under a frozen {@code /version/<number>/}
     *       snapshot;</li>
     *   <li>{@link #STALE_IN_FROZEN_SNAPSHOTS} — render fine today, stale only in a
     *       frozen {@code /version/<number>/} snapshot.</li>
     * </ul>
     */
    private static boolean isKnownBrokenFragment(String url, String fragment) {
        if (KNOWN_BROKEN_FRAGMENTS.contains(fragment)) {
            return true;
        }
        // TODO: remove once https://github.com/quarkusio/quarkusio.github.io/issues/2962
        //       is fixed — Roq must expand {summaryTableId} in generated anchors.
        if (fragment.contains("{summaryTableId}")) {
            return true;
        }
        if (UNRENDERED_UPSTREAM_ANCHORS.contains(fragment)) {
            return true;
        }
        if (DEAD_UPSTREAM_ANCHORS.contains(fragment)) {
            return true;
        }
        if (WILL_NOT_BACKPORT_FRAGMENTS.contains(fragment)
                || STALE_IN_FROZEN_SNAPSHOTS.contains(fragment)) {
            return isFrozenVersionSnapshot(url);
        }
        return false;
    }

    /** True for a frozen numbered versioned snapshot ({@code /version/<number>/}), not main. */
    private static boolean isFrozenVersionSnapshot(String url) {
        return VERSION_SNAPSHOT_PATTERN.matcher(url).find();
    }

    private static final Pattern VERSION_SNAPSHOT_PATTERN =
            Pattern.compile("/version/[\\d.]+/");

    /** Sentinel id set used to remember pages that failed to load. */
    private static final class FetchFailed {
        static final Set<String> INSTANCE = Set.of();
    }

    private Set<String> fetchPageIds(Page p, String url) {
        try {
            Response response = p.navigate(url,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            if (response == null || response.status() >= 400) {
                return FetchFailed.INSTANCE;
            }
            @SuppressWarnings("unchecked")
            var idResult = (List<String>) p.evaluate(
                    "() => [...document.querySelectorAll('[id]')].map(e => e.id)");
            return new HashSet<>(idResult);
        } catch (Exception e) {
            return FetchFailed.INSTANCE;
        }
    }

    private static List<String> parseExcludePaths(String property) {
        if (property == null || property.isBlank()) {
            return List.of();
        }
        return Arrays.stream(property.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean isExcluded(String url, List<String> excludePaths) {
        String path = stripHost(url);
        for (String excluded : excludePaths) {
            if (path.contains(excluded)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalhostUrl(String href) {
        return href.startsWith("http://localhost") || href.startsWith("https://localhost");
    }

    private static String normalize(String url) {
        if (url.length() > 1 && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    record ResolvedLink(String url, boolean internal, String fragment) {
    }

    record FragmentLink(String url, String fragment, String referrer) {
    }

    record BrokenLink(int status, String statusText, String referrer) {
    }

    record BrokenImage(int status, String statusText, String referrer) {
    }

    record CrawlResults(Map<String, BrokenLink> brokenLinks, Map<String, BrokenImage> brokenImages,
                         Map<String, FragmentLink> brokenFragments) {
    }
}
