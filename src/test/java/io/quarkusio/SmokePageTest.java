package io.quarkusio;

import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainRawHtml;
import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class SmokePageTest extends BrowserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/about/",
            "/blog/",
            "/guides/",
            "/get-started/",
            "/support/",
            "/newsletter/",
            "/community/",
            "/events/",
            "/userstories/",
            "/releases/",
            "/eol/",
            "/security/"
    })
    void criticalPageReturns200(String path) {
        Response response = page.navigate(baseUrl + path);
        assertNotNull(response, "No response for " + path);
        assertEquals(200, response.status(),
                "Expected 200 for " + path + " but got " + response.status());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/about/",
            "/blog/",
            "/guides/",
            "/get-started/",
            "/support/"
    })
    void criticalPageHasValidTitle(String path) {
        page.navigate(baseUrl + path);
        String title = page.title();
        assertFalse(title == null || title.isBlank(),
                "Page title should not be null or empty for " + path);
        String lowerTitle = title.toLowerCase();
        assertFalse(lowerTitle.contains("undefined"),
                "Page title should not contain 'undefined' for " + path + ", got: " + title);
        assertFalse(lowerTitle.contains("null"),
                "Page title should not contain 'null' for " + path + ", got: " + title);
        assertFalse(lowerTitle.contains("error"),
                "Page title should not contain 'error' for " + path + ", got: " + title);
        assertFalse(lowerTitle.contains("exception"),
                "Page title should not contain 'Exception' for " + path + ", got: " + title);
    }

    @Test
    void homepageDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + "/");
        assertDoesNotContainUnrenderedMarkup(page, "Homepage");
    }

    @Test
    void homepageDoesNotContainRawHtmlTags() {
        page.navigate(baseUrl + "/");
        assertDoesNotContainRawHtml(page, "Homepage");
    }

    @Test
    void notFoundPageReturns404() {
        Response response = page.navigate(baseUrl + "/this-path-does-not-exist-xyz/");
        assertNotNull(response, "No response for nonexistent path");
        assertEquals(404, response.status());
    }
}
