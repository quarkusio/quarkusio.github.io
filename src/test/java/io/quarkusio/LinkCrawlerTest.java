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

import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LinkCrawlerTest extends BrowserTest {

    private static final int DEFAULT_MAX_PAGES = Integer.MAX_VALUE;
    private static final int DEFAULT_THREADS = 16;

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
            List<Map.Entry<String, BrokenLink>> sorted = new ArrayList<>(results.brokenLinks.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            fail("Found " + results.brokenLinks.size() + " broken link(s):\n" + buildLinkReport(sorted));
        }
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

    private CrawlResults runCrawl() throws InterruptedException {
        int maxPages = Integer.getInteger("test.crawl.max-pages", DEFAULT_MAX_PAGES);
        int threads = Integer.getInteger("test.crawl.threads", DEFAULT_THREADS);
        boolean checkInternal = Boolean.parseBoolean(System.getProperty("test.crawl.check-internal", "true"));
        boolean checkExternal = Boolean.parseBoolean(System.getProperty("test.crawl.check-external", "false"));
        String resultsFile = System.getProperty("test.crawl.results-file", "");
        List<String> excludePaths = parseExcludePaths(System.getProperty("test.crawl.exclude-paths", ""));
        List<String> excludeUrls = parseExcludePaths(System.getProperty("test.crawl.exclude-urls", ""));
        List<String> changedPaths = parseExcludePaths(System.getProperty("test.crawl.changed-paths", ""));

        Set<String> visited = ConcurrentHashMap.newKeySet();
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        Map<String, BrokenLink> brokenLinks = new ConcurrentHashMap<>();
        Map<String, BrokenImage> brokenImages = new ConcurrentHashMap<>();
        Map<String, String> referrers = new ConcurrentHashMap<>();
        Map<String, String> pendingExternal = new ConcurrentHashMap<>();
        Set<String> checkedImages = ConcurrentHashMap.newKeySet();
        AtomicInteger crawledCount = new AtomicInteger();

        Set<String> seedUrls = ConcurrentHashMap.newKeySet();
        if (!changedPaths.isEmpty()) {
            System.out.println("Incremental mode: checking " + changedPaths.size() + " changed page(s)");
            for (String path : changedPaths) {
                String url = normalize(baseUrl + path);
                seedUrls.add(url);
                queue.add(url);
            }
        } else {
            queue.add(normalize(baseUrl + "/"));
        }

        // Phase 1: crawl internal pages with Playwright, collecting external URLs
        ExecutorService crawlExecutor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            crawlExecutor.submit(() -> {
                try (Playwright pw = Playwright.create()) {
                    Browser br = pw.chromium().launch(
                            new BrowserType.LaunchOptions().setHeadless(true));
                    BrowserContext ctx = br.newContext();
                    ctx.route("**/*.{css,woff,woff2,ttf,eot}", Route::abort);
                    Page p = ctx.newPage();
                    p.setDefaultNavigationTimeout(60_000);

                    crawLoop(p, queue, visited, brokenLinks, brokenImages, referrers,
                            pendingExternal, checkedImages, crawledCount, maxPages,
                            checkInternal, checkExternal, excludePaths, excludeUrls, seedUrls);

                    ctx.close();
                    br.close();
                }
            });
        }

        crawlExecutor.shutdown();
        crawlExecutor.awaitTermination(3, TimeUnit.HOURS);

        System.out.println("Phase 1: crawled " + crawledCount.get() + " internal pages"
                + ", checked " + checkedImages.size() + " unique images"
                + ", found " + brokenLinks.size() + " broken internal links"
                + ", found " + brokenImages.size() + " broken images"
                + " (" + threads + " threads)");

        // Phase 2: check external links with plain HTTP threads
        if (checkExternal && !pendingExternal.isEmpty()) {
            int total = pendingExternal.size();
            System.out.println("Phase 2: checking " + total + " external links...");
            AtomicInteger checkedCount = new AtomicInteger();
            ExecutorService externalExecutor = Executors.newFixedThreadPool(threads);
            for (var entry : pendingExternal.entrySet()) {
                externalExecutor.submit(() -> {
                    BrokenLink result = checkExternalLink(entry.getKey());
                    int done = checkedCount.incrementAndGet();
                    if (done % 500 == 0) {
                        System.out.println("  checked " + done + "/" + total + " external links...");
                    }
                    if (result != null) {
                        brokenLinks.put(entry.getKey(), new BrokenLink(result.status, result.statusText, entry.getValue()));
                    }
                });
            }
            externalExecutor.shutdown();
            externalExecutor.awaitTermination(3, TimeUnit.HOURS);

            System.out.println("Phase 2: checked " + total + " external links"
                    + ", found " + (brokenLinks.size()) + " total broken links");
        }

        Map<String, BrokenLink> rateLimited = new ConcurrentHashMap<>();
        Map<String, BrokenLink> reallyBroken = new ConcurrentHashMap<>();
        brokenLinks.forEach((url, link) -> {
            if (link.status == 429) {
                rateLimited.put(url, link);
            } else {
                reallyBroken.put(url, link);
            }
        });

        if (!resultsFile.isEmpty()) {
            writeResultsJson(reallyBroken, resultsFile);
        }

        if (!rateLimited.isEmpty()) {
            List<Map.Entry<String, BrokenLink>> sorted = new ArrayList<>(rateLimited.entrySet());
            sorted.sort(Map.Entry.comparingByKey());
            System.out.println("WARNING: " + rateLimited.size()
                    + " link(s) could not be checked due to rate limiting (429):\n"
                    + buildLinkReport(sorted));
        }

        return new CrawlResults(reallyBroken, brokenImages);
    }

    private void crawLoop(Page p,
                           ConcurrentLinkedQueue<String> queue,
                           Set<String> visited,
                           Map<String, BrokenLink> brokenLinks,
                           Map<String, BrokenImage> brokenImages,
                           Map<String, String> referrers,
                           Map<String, String> pendingExternal,
                           Set<String> checkedImages,
                           AtomicInteger crawledCount,
                           int maxPages,
                           boolean checkInternal,
                           boolean checkExternal,
                           List<String> excludePaths,
                           List<String> excludeUrls,
                           Set<String> seedUrls) {
        boolean incrementalMode = !seedUrls.isEmpty();
        int idlePolls = 0;
        while (idlePolls < 10) {
            if (crawledCount.get() >= maxPages) {
                break;
            }

            String currentUrl = queue.poll();
            if (currentUrl == null) {
                idlePolls++;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            idlePolls = 0;

            if (!visited.add(currentUrl)) {
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
                        brokenLinks.put(currentUrl, new BrokenLink(probe.status, probe.statusText, referrers.get(currentUrl)));
                    }
                }
                continue;
            }

            int status = response.status();
            if (status >= 400) {
                if (checkInternal) {
                    brokenLinks.put(currentUrl, new BrokenLink(status, response.statusText(), referrers.get(currentUrl)));
                }
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

            List<String> hrefs;
            List<String> imageSrcs;
            try {
                @SuppressWarnings("unchecked")
                var linkResult = (List<String>) p.evaluate(
                        "() => [...document.querySelectorAll('a[href]')].filter(a => {"
                        + "  try {"
                        + "    const block = a.closest('p,li,td,dd,div') || a.parentElement;"
                        + "    if (!block) return true;"
                        + "    const r = document.createRange();"
                        + "    r.setStart(block, 0);"
                        + "    r.setEndBefore(a);"
                        + "    return !r.toString().toLowerCase().includes('such as');"
                        + "  } catch(e) { return true; }"
                        + "}).map(a => a.getAttribute('href'))");
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
            } catch (PlaywrightException e) {
                if (checkInternal) {
                    BrokenLink probe = probeWithHttp(currentUrl);
                    if (probe != null) {
                        brokenLinks.put(currentUrl, new BrokenLink(probe.status, probe.statusText, referrers.get(currentUrl)));
                    }
                }
                continue;
            }

            for (String href : hrefs) {
                if (href == null || href.isBlank()) {
                    continue;
                }

                // Rewrite production URLs to localhost for testing
                href = rewriteToLocal(href);

                ResolvedLink resolved = resolveLink(pageUrl, href);
                if (resolved == null) {
                    continue;
                }

                if (resolved.internal) {
                    String normalized = normalize(resolved.url);
                    if (!visited.contains(normalized) && !isExcludedPath(normalized, excludePaths)) {
                        queue.add(normalized);
                        referrers.putIfAbsent(normalized, currentUrl);
                    }
                } else if (checkExternal && !isExcludedUrl(resolved.url, excludeUrls)
                        && !isOnDoNotCheckList(resolved.url)) {
                    pendingExternal.putIfAbsent(resolved.url, currentUrl);
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
                || href.startsWith("tel:") || href.startsWith("#")) {
            return null;
        }

        String resolved;
        boolean internal;
        if (href.startsWith("http://") || href.startsWith("https://")) {
            if (isLocalhostUrl(href) || isExampleUrl(href)) {
                return null;
            }
            resolved = href;
            internal = href.startsWith(baseUrl);
        } else {
            try {
                resolved = resolveRelativeUrl(currentPageUrl, href);
            } catch (Exception e) {
                return null;
            }
            if (resolved == null) {
                return null;
            }
            internal = true;
        }

        int fragmentIndex = resolved.indexOf('#');
        if (fragmentIndex >= 0) {
            resolved = resolved.substring(0, fragmentIndex);
        }

        return new ResolvedLink(resolved, internal);
    }

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; QuarkusLinkChecker/1.0; +https://quarkus.io)";

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final HttpClient NO_REDIRECT_HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ConcurrentHashMap<String, Semaphore> DOMAIN_SEMAPHORES = new ConcurrentHashMap<>();

    private static Semaphore domainSemaphore(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) host = url;
            return DOMAIN_SEMAPHORES.computeIfAbsent(host.toLowerCase(), k -> new Semaphore(2));
        } catch (IllegalArgumentException e) {
            return DOMAIN_SEMAPHORES.computeIfAbsent(url, k -> new Semaphore(1));
        }
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
        Semaphore sem = domainSemaphore(url);
        try {
            sem.acquire();
            try {
                return doCheckExternalLink(url);
            } finally {
                sem.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new BrokenLink(0, "interrupted", null);
        }
    }

    private static BrokenLink doCheckExternalLink(String url) {
        try {
            HttpRequest headRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", USER_AGENT)
                    .timeout(Duration.ofSeconds(15))
                    .build();
            int status = sendWithRetry(headRequest);

            // Some sites reject HEAD but accept GET — fall back
            if (status >= 400 && status < 500) {
                HttpRequest getRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(15))
                        .build();
                status = sendWithRetry(getRequest);
            }

            // Some sites block bot user-agents — retry without custom UA
            // and without following redirects (some sites redirect bots to challenge pages)
            if (status == 401 || status == 403) {
                HttpRequest retryRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .timeout(Duration.ofSeconds(15))
                        .build();
                int retryStatus = NO_REDIRECT_HTTP_CLIENT.send(retryRequest, HttpResponse.BodyHandlers.discarding()).statusCode();
                if (retryStatus < 400) {
                    status = retryStatus;
                }
            }

            if (status == 403 && hostMatches(url, FORBIDDEN_OK_HOSTS)) {
                return null;
            }
            if (status == 405 && hostMatches(url, METHOD_NOT_ALLOWED_OK_HOSTS)) {
                return null;
            }

            if (status >= 400) {
                return new BrokenLink(status, "HTTP " + status, null);
            }
            return null;
        } catch (Exception e) {
            return new BrokenLink(0, e.getMessage(), null);
        }
    }

    private static boolean hostMatches(String url, List<String> hosts) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return false;
            }
            String hostLower = host.toLowerCase();
            for (String h : hosts) {
                if (hostLower.endsWith(h)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return false;
    }

    private static int sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        int status = SHARED_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        Duration delay = Duration.ofSeconds(65);
        Duration maxDelay = Duration.ofMinutes(5);
        for (int attempt = 0; attempt < 3 && (status == 429 || status >= 500); attempt++) {
            Thread.sleep(delay.toMillis());
            status = SHARED_HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            delay = delay.multipliedBy(2);
            if (delay.compareTo(maxDelay) > 0) delay = maxDelay;
        }
        return status;
    }

    private static final Pattern META_REFRESH_PATTERN = Pattern.compile(
            "<meta\\s[^>]*?(?:" +
                    "http-equiv\\s*=\\s*[\"']?refresh[\"']?[^>]*?content\\s*=\\s*[\"']?\\d+\\s*;\\s*url=([^\"'\\s>]+)" +
                    "|" +
                    "content\\s*=\\s*[\"']?\\d+\\s*;\\s*url=([^\"'\\s>]+)[^>]*?http-equiv\\s*=\\s*[\"']?refresh[\"']?" +
                    ")",
            Pattern.CASE_INSENSITIVE);

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

    private static final Set<String> PRODUCTION_HOSTS = Set.of("quarkus.io", "www.quarkus.io");

    // Paths that should NOT be rewritten to localhost - they are served from different repos
    private static final Set<String> EXTERNAL_QUARKUSIO_PATHS = Set.of(
            "/extensions",     // Extensions registry (separate service)
            "/benchmarks"      // Benchmark results (separate service)
    );

    private String rewriteToLocal(String target) {
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            return target;
        }
        try {
            URI targetUri = URI.create(target);
            if (PRODUCTION_HOSTS.contains(targetUri.getHost())) {
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

    private static List<String> parseExcludePaths(String property) {
        if (property == null || property.isBlank()) {
            return List.of();
        }
        return Arrays.stream(property.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private boolean isExcludedPath(String url, List<String> excludePaths) {
        String path = url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url;
        for (String excluded : excludePaths) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        if (isOldNewsletter(path)) {
            return true;
        }
        return false;
    }

    // Newsletters are monthly link roundups that naturally accumulate dead external
    // links over time. Checking old issues generates noise without actionable fixes,
    // since no one wants to invest time editing historical newsletter archives.
    private static final Pattern NEWSLETTER_PATTERN = Pattern.compile("^/newsletter/(\\d+)");
    private static final int NEWSLETTER_EPOCH_YEAR = 2020;
    private static final int NEWSLETTER_EPOCH_MONTH = 10; // October 2020 = issue #1
    private static final int NEWSLETTER_MAX_AGE_MONTHS = 6;

    private static boolean isOldNewsletter(String path) {
        Matcher m = NEWSLETTER_PATTERN.matcher(path);
        if (m.find()) {
            int issue = Integer.parseInt(m.group(1));
            java.time.LocalDate now = java.time.LocalDate.now();
            int monthsSinceEpoch = (now.getYear() - NEWSLETTER_EPOCH_YEAR) * 12
                    + now.getMonthValue() - NEWSLETTER_EPOCH_MONTH;
            return issue <= monthsSinceEpoch - NEWSLETTER_MAX_AGE_MONTHS;
        }
        return false;
    }

    private static boolean isExcludedUrl(String url, List<String> excludeUrls) {
        for (String excluded : excludeUrls) {
            if (url.contains(excluded)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOnDoNotCheckList(String url) {
        for (String domain : DO_NOT_CHECK) {
            if (url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalhostUrl(String href) {
        return href.startsWith("http://localhost") || href.startsWith("https://localhost")
                || href.startsWith("http://0.0.0.0") || href.startsWith("http://127.0.0.1")
                || href.startsWith("https://0.0.0.0") || href.startsWith("https://127.0.0.1");
    }

    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\$\\{|\\{[a-zA-Z]");

    private static final List<String> EXAMPLE_HOSTS = List.of(
            "example.com", "example.org", "example.net", "1.2.3.4",
            "your-domain", "your-dns", "your-ngrok",
            "application.com", "service.example",
            "SERVER_HOST", "SERVER_PORT",
            "myservice.com", "my-service.com", "openshift-helloworld",
            "quarkus-auth0", "stage.code.quarkus.io",
            "nip.io", "cluster.local", "ocp.host"
    );

    // Sites that return 403 Forbidden for bot requests but 404 for genuinely dead links
    private static final List<String> FORBIDDEN_OK_HOSTS = List.of(
            "medium.com"
    );

    // Sites that return 405 Method Not Allowed for bot requests but 404 for genuinely dead links
    private static final List<String> METHOD_NOT_ALLOWED_OK_HOSTS = List.of(
            "infoq.com"
    );

    private static final List<String> DO_NOT_CHECK = List.of(
            "linkedin.com",
            "pexels.com",
            "docs.gitlab.com",
            "search.maven.org",
            "linux.die.net",
            "uber.com/blog",
            "rfc-editor.org",
            "netflixtechblog.com",
            // Localized sites 404 for untranslated pages — tracked in #2695
            "cn.quarkus.io/blog",
            "es.quarkus.io/blog",
            "ja.quarkus.io/blog",
            "pt.quarkus.io/blog"
    );

    private static boolean isExampleUrl(String url) {
        if (TEMPLATE_VAR_PATTERN.matcher(url).find()) {
            return true;
        }
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return true;
            }
            String hostLower = host.toLowerCase();
            for (String example : EXAMPLE_HOSTS) {
                if (hostLower.contains(example.toLowerCase())) {
                    return true;
                }
            }
        } catch (IllegalArgumentException e) {
            return true;
        }
        return false;
    }

    private static String normalize(String url) {
        if (url.length() > 1 && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static void writeResultsJson(Map<String, BrokenLink> broken, String path) {
        List<Map.Entry<String, BrokenLink>> sorted = new ArrayList<>(broken.entrySet());
        sorted.sort(Map.Entry.comparingByKey());

        List<Map<String, Object>> results = sorted.stream()
                .map(entry -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("url", entry.getKey());
                    map.put("status", entry.getValue().status());
                    map.put("statusText", entry.getValue().statusText());
                    map.put("referrer", entry.getValue().referrer());
                    return map;
                })
                .toList();

        try {
            Path filePath = Path.of(path);
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), results);
            System.out.println("Wrote results to " + path);
        } catch (IOException e) {
            System.err.println("Warning: could not write results file: " + e.getMessage());
        }
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

    record ResolvedLink(String url, boolean internal) {
    }

    record BrokenLink(int status, String statusText, String referrer) {
    }

    record BrokenImage(int status, String statusText, String referrer) {
    }

    record CrawlResults(Map<String, BrokenLink> brokenLinks, Map<String, BrokenImage> brokenImages) {
    }
}
