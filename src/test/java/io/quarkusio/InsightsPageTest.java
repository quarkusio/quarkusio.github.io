package io.quarkusio;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InsightsPageTest extends BrowserTest {

    @Test
    void insightsPageHasUpcomingSession() {
        page.navigate(baseUrl + "/insights/",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        var convertedTime = page.locator("#convertedTime");
        assertTrue(convertedTime.isVisible(), "Expected #convertedTime to be visible");

        String dataUtc = convertedTime.getAttribute("data-utc");
        assertNotNull(dataUtc, "Expected data-utc attribute on #convertedTime");
        assertFalse(dataUtc.isEmpty(), "Expected non-empty data-utc attribute on #convertedTime");
    }

    @Test
    void insightsPageHasScheduledSessions() {
        page.navigate(baseUrl + "/insights/",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        var scheduledTitles = page.locator(".card-static .title");
        int count = scheduledTitles.count();
        assertTrue(count >= 1, "Expected at least 1 scheduled session, but found " + count);
    }
}
