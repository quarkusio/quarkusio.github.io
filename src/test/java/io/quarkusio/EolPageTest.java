package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EolPageTest extends BrowserTest {

    @Test
    void eolPageHasConsequenceSections() {
        page.navigate(baseUrl + "/eol/");
        int consequenceCount = page.locator(".eol-consequence").count();
        assertTrue(consequenceCount >= 2,
                "Expected at least 2 EOL consequence sections but found " + consequenceCount);
    }

    @Test
    void eolPageMentionsLts() {
        page.navigate(baseUrl + "/eol/");
        String text = page.locator(".eol-band").textContent();
        assertTrue(text.contains("LTS"),
                "EOL page should mention LTS releases");
    }

    @Test
    void eolPageHasEolTable() {
        page.navigate(baseUrl + "/eol/");
        Locator table = page.locator(".eol-table-band .releases-table");
        assertTrue(table.isVisible(), "EOL table should be visible");
        int rowCount = page.locator(".eol-table-band .releases-table tbody tr").count();
        assertTrue(rowCount >= 5,
                "Expected at least 5 EOL releases in the table but found " + rowCount);
    }

    @Test
    void eolPageHasCommercialSupportSection() {
        page.navigate(baseUrl + "/eol/");
        Locator providers = page.locator(".eol-provider");
        assertTrue(providers.count() >= 2,
                "Expected at least two EOL support providers but found " + providers.count());
    }

    @Test
    void eolPageHasViewAllReleasesButton() {
        page.navigate(baseUrl + "/eol/");
        Locator cta = page.locator(".eol-cta a[href*='/releases/']");
        assertTrue(cta.count() >= 1,
                "Expected a 'View all releases' button linking to /releases/");
    }
}
