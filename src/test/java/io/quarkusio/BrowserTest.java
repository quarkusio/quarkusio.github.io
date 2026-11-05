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
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }
}
