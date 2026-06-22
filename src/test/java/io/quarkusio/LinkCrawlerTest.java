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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LinkCrawlerTest extends BrowserTest {

    private static final int DEFAULT_MAX_PAGES = Integer.MAX_VALUE;
    private static final int DEFAULT_THREADS = 8;

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
        List<String> excludePaths = parseExcludePaths(System.getProperty("test.crawl.exclude-paths", ""));
        List<String> changedPaths = parseExcludePaths(System.getProperty("test.crawl.changed-paths", ""));

        Set<String> visited = ConcurrentHashMap.newKeySet();
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        Map<String, BrokenLink> brokenLinks = new ConcurrentHashMap<>();
        Map<String, BrokenImage> brokenImages = new ConcurrentHashMap<>();
        Map<String, String> referrers = new ConcurrentHashMap<>();
        Set<String> checkedExternal = ConcurrentHashMap.newKeySet();
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
                            checkedExternal, checkedImages, crawledCount, maxPages,
                            checkInternal, checkExternal, excludePaths, seedUrls);

                    ctx.close();
                    br.close();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);

        System.out.println("Crawled " + crawledCount.get() + " internal pages"
                + (checkExternal ? ", checked " + checkedExternal.size() + " external links" : "")
                + ", checked " + checkedImages.size() + " unique images"
                + ", found " + brokenLinks.size() + " broken links"
                + ", found " + brokenImages.size() + " broken images"
                + " (" + threads + " threads)");

        return new CrawlResults(brokenLinks, brokenImages);
    }

    private void crawLoop(Page p,
                           ConcurrentLinkedQueue<String> queue,
                           Set<String> visited,
                           Map<String, BrokenLink> brokenLinks,
                           Map<String, BrokenImage> brokenImages,
                           Map<String, String> referrers,
                           Set<String> checkedExternal,
                           Set<String> checkedImages,
                           AtomicInteger crawledCount,
                           int maxPages,
                           boolean checkInternal,
                           boolean checkExternal,
                           List<String> excludePaths,
                           Set<String> seedUrls) {
        boolean incrementalMode = !seedUrls.isEmpty();
        int idlePolls = 0;
        while (idlePolls < 3) {
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

                ResolvedLink resolved = resolveLink(pageUrl, href);
                if (resolved == null) {
                    continue;
                }

                if (resolved.internal) {
                    String normalized = normalize(resolved.url);
                    if (!visited.contains(normalized) && !isExcluded(normalized, excludePaths)) {
                        queue.add(normalized);
                        referrers.putIfAbsent(normalized, currentUrl);
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
            if (isLocalhostUrl(href)) {
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

    private String rewriteToLocal(String target) {
        if (!target.startsWith("http://") && !target.startsWith("https://")) {
            return target;
        }
        try {
            URI targetUri = URI.create(target);
            if (PRODUCTION_HOSTS.contains(targetUri.getHost())) {
                String path = targetUri.getPath();
                if (path != null && !path.isEmpty()) {
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

    private boolean isExcluded(String url, List<String> excludePaths) {
        String path = url.startsWith(baseUrl) ? url.substring(baseUrl.length()) : url;
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
