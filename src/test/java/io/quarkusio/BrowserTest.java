package io.quarkusio;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.util.Set;

/**
 * Base for tests that drive a browser against the live app.
 * <p>
 * It would be nicer to use {@code @WithPlaywright} and {@code @InjectPlaywright} from quarkus-playwright,
 * but {@code @WithPlaywright} makes Quarkus build and start a separate application for every test class that
 * carries it, which runs out of memory with the default heap once there are a couple of dozen classes.
 * See https://github.com/quarkiverse/quarkus-playwright/issues/250 and
 * https://github.com/quarkusio/quarkus/pull/56443
 * <p>
 * TODO: use {@code @WithPlaywright} and {@code @InjectPlaywright} instead of this class once on Quarkus 3.40.2
 */
public abstract class BrowserTest {

    /**
     * Third-party hosts the site cannot work without: jQuery, which the site scripts (mobile navigation, for
     * example) depend on, the icon CSS, without which icon-only buttons have no size and cannot be clicked,
     * and the search service the guides search queries (it is only called once someone types, so it does not
     * hold up page loads, and the search falls back to filtering locally when it does not answer).
     */
    private static final Set<String> ALLOWED_EXTERNAL_HOSTS = Set.of("ajax.googleapis.com", "use.fontawesome.com",
            "search.quarkus.io");

    static Playwright playwright;
    static Browser browser;
    static String baseUrl;

    @TestHTTPResource
    String siteUrl;

    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void resolveBaseUrl() {
        // the injected URL ends with a slash, and the server rejects "//"
        baseUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        // The pages pull in fonts, icon CSS, comment widgets and so on from third parties, and a navigation
        // waits for all of them to load. Tests only care about our own site, so a slow or unreachable
        // third party must not turn into a 30s navigation timeout.
        context.route(BrowserTest::isExternal, route -> {
            URI url = URI.create(route.request().url());
            // the icon fonts are not needed, the icons just render as fallback glyphs
            if (ALLOWED_EXTERNAL_HOSTS.contains(url.getHost()) && !url.getPath().contains("/webfonts/")) {
                route.resume();
            } else {
                route.abort();
            }
        });
        page = context.newPage();
    }

    private static boolean isExternal(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && !host.equals("localhost") && !host.equals("127.0.0.1");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }
}
