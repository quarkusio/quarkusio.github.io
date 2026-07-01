package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HTML output produced by _plugins/asciidoctor-extension.rb.
 */
public class AsciidocExtensionTest extends BrowserTest {

    @ParameterizedTest
    @CsvSource({
            "/guides/redis-reference, stable",
            "/guides/cache-redis-reference, preview"
    })
    void guideWithExtensionStatusShowsLabel(String path, String status) {
        page.navigate(baseUrl + path);

        Locator label = page.locator("a.status-label.status-" + status);
        assertTrue(label.count() > 0,
                path + ": Expected a status label with class 'status-" + status + "'");
        assertEquals(status, label.first().textContent().trim(),
                path + ": Status label text should be '" + status + "'");

        String href = label.first().getAttribute("href");
        assertNotNull(href, path + ": Status label should have an href");
        assertTrue(href.contains("extension-status"),
                path + ": Status label href should reference extension-status, got: " + href);
    }

    @Test
    void configPropertyHasCopyButton() {
        page.navigate(baseUrl + "/guides/redis-reference");

        Locator copyButtons = page.locator("button.btn-copy.inline-btn-copy[data-clipboard-text]");
        assertTrue(copyButtons.count() > 0,
                "Expected at least one config property copy button with data-clipboard-text");

        String clipboardText = copyButtons.first().getAttribute("data-clipboard-text");
        assertNotNull(clipboardText, "data-clipboard-text should not be null");
        assertFalse(clipboardText.isBlank(), "data-clipboard-text should contain a property name");
    }

    @Test
    void envVarHasCopyButton() {
        page.navigate(baseUrl + "/guides/redis-reference");

        Locator envVarCodes = page.locator("code[id^='env-var-']");
        assertTrue(envVarCodes.count() > 0,
                "Expected at least one env var code element with id starting with 'env-var-'");

        Locator copyButtons = page.locator("button.btn-copy.inline-btn-copy[data-clipboard-target^='#env-var-']");
        assertTrue(copyButtons.count() > 0,
                "Expected at least one copy button targeting an env var element");
    }

    @Test
    void configTableHasCollapsibleRows() {
        page.navigate(baseUrl + "/guides/redis-reference");

        Locator table = page.locator("table.configuration-reference-all-rows");
        assertTrue(table.count() > 0,
                "Expected at least one configuration-reference table with configuration-reference-all-rows class");

        Locator collapsedRows = page.locator("tr.row-collapsible.row-collapsed");
        assertTrue(collapsedRows.count() > 0,
                "Expected at least one collapsed config row");

        Locator showMore = page.locator(".description-decoration .fa-chevron-down");
        assertTrue(showMore.count() > 0,
                "Expected at least one 'Show more' chevron icon in collapsed descriptions");
    }

    @Test
    void configTableHasSearchInput() {
        page.navigate(baseUrl + "/guides/redis-reference");

        Locator searchInput = page.locator("input[type='search'][placeholder='FILTER CONFIGURATION']");
        assertTrue(searchInput.count() > 0,
                "Expected a search input with placeholder 'FILTER CONFIGURATION' for the config table");
    }

    @Test
    void tooltipMacroRendersTooltipSpan() {
        page.navigate(baseUrl + "/guides/security-oidc-configuration-properties-reference");

        Locator tooltipWrappers = page.locator("span.asciidoc-tooltip-wrapper");
        assertTrue(tooltipWrappers.count() > 0,
                "Expected at least one tooltip wrapper span");

        Locator firstWrapper = tooltipWrappers.first();
        assertTrue(firstWrapper.locator("code").count() > 0,
                "Tooltip wrapper should contain a <code> element");
        assertTrue(firstWrapper.locator("span.asciidoc-tooltip").count() > 0,
                "Tooltip wrapper should contain a span with class 'asciidoc-tooltip'");
    }
}
