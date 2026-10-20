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

        String firstScheduled = scheduledTitles.first().innerText();
        assertTrue(firstScheduled.startsWith("Episode #264:"),
                "Expected first scheduled title to start with 'Episode #264:', but was: " + firstScheduled);
        assertFalse(firstScheduled.contains("{item.episode}"), "Expected episode number to be interpolated");
        String firstScheduled = scheduledTitles.first().innerText();
        assertTrue(firstScheduled.startsWith("Episode #"),
                "Expected first scheduled title to start with 'Episode #, but was: " + firstScheduled);
        assertFalse(firstScheduled.contains("{item.episode}"), "Expected episode number to be interpolated");

    @Test
    void insightsPageHasArchivedEpisodesTable() {
        page.navigate(baseUrl + "/insights/",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        var table = page.locator("table.tableblock");
        assertTrue(table.isVisible(), "Expected archived episodes table to be visible");

        var headers = table.locator("thead th");
        assertEquals(4, headers.count(), "Expected 4 table headers");
        assertEquals("Episode", headers.nth(0).innerText().trim());
        assertEquals("Title", headers.nth(1).innerText().trim());
        assertEquals("Guest(s)", headers.nth(2).innerText().trim());
        assertEquals("Streaming Date", headers.nth(3).innerText().trim());

        var rows = table.locator("tbody tr");
        assertTrue(rows.count() >= 1, "Expected at least 1 row in archived episodes table");

        var firstRowEpisode = rows.first().locator("td").first().innerText().trim();
    }
}
