package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityPageTest extends BrowserTest {

    @Test
    void supportedVersionsTableIsPresent() {
        page.navigate(baseUrl + "/security/");
        Locator table = page.locator("table.tableblock");
        assertTrue(table.isVisible(), "Supported versions table should be visible");

        Locator headers = table.locator("thead th");
        assertEquals(2, headers.count(), "Table should have 2 columns");
        assertTrue(headers.nth(0).textContent().contains("Version"), "First column should be Version");
        assertTrue(headers.nth(1).textContent().contains("Supported"), "Second column should be Supported");
    }

    @Test
    void supportedVersionsTableShowsLatestRelease() {
        page.navigate(baseUrl + "/security/");
        Locator rows = page.locator("table.tableblock tbody tr");
        assertTrue(rows.count() >= 4,
                "Expected at least 4 rows (latest, LTS, older, pre-3.0) but found " + rows.count());

        String firstRowText = rows.first().textContent();
        assertTrue(firstRowText.matches("(?s).*Latest \\d+\\.x.*"),
                "First row should show 'Latest <number>.x' but was: " + firstRowText);
        assertTrue(firstRowText.contains("✅"), "Latest release should be marked as supported");
    }

    @Test
    void supportedVersionsTableShowsLtsVersions() {
        page.navigate(baseUrl + "/security/");
        Locator cells = page.locator("table.tableblock tbody td");
        boolean foundLts = false;
        for (int i = 0; i < cells.count(); i++) {
            if (cells.nth(i).textContent().contains("LTS")) {
                foundLts = true;
                break;
            }
        }
        assertTrue(foundLts, "Table should contain at least one LTS version row");
    }

    @Test
    void supportedVersionsTableShowsUnsupportedRows() {
        page.navigate(baseUrl + "/security/");
        Locator rows = page.locator("table.tableblock tbody tr");
        String lastRowText = rows.last().textContent();
        assertTrue(lastRowText.contains("❌"), "Last row should be marked as unsupported");
    }

    @Test
    void pageLinksToEol() {
        page.navigate(baseUrl + "/security/");
        Locator eolLink = page.locator("a[href*='/eol/']");
        assertTrue(eolLink.count() >= 1, "Page should link to the EOL page");
    }
}
