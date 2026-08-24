package io.quarkusio;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AllConfigPageTest extends BrowserTest {

    @Test
    void allConfigPageReturns200() {
        Response response = page.navigate(baseUrl + "/guides/all-config");
        assertNotNull(response, "No response for /guides/all-config");
        assertEquals(200, response.status(),
                "Expected 200 for /guides/all-config but got " + response.status());
    }

    @Test
    void allConfigPageHasSubstantialConfigTableContent() {
        page.navigate(baseUrl + "/guides/all-config");

        Locator tables = page.locator("table.configuration-reference");
        assertTrue(tables.count() > 0,
                "Expected at least one configuration-reference table on /guides/all-config. "
                        + "The include of quarkus-all-config.adoc may not have resolved "
                        + "(check that the generated-dir attribute points to a valid path).");

        int rowCount = page.locator("table.configuration-reference tr").count();
        assertTrue(rowCount >= 10,
                "Expected substantial config table content on /guides/all-config but found only "
                        + rowCount + " row(s). The config table content may be missing.");
    }
}
