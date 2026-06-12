package io.quarkusio;

import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            "/extensions/",
            "/newsletter/",
            "/community/",
            "/events/",
            "/userstories/"
    })
    void criticalPageReturns200(String path) {
        Response response = page.navigate(baseUrl + path);
        assertNotNull(response, "No response for " + path);
        assertEquals(200, response.status(),
                "Expected 200 for " + path + " but got " + response.status());
    }

    @Test
    void notFoundPageReturns404() {
        Response response = page.navigate(baseUrl + "/this-path-does-not-exist-xyz/");
        assertNotNull(response, "No response for nonexistent path");
        assertEquals(404, response.status());
    }
}
