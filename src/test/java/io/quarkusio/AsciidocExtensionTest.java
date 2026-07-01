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
    void clickingChevronExpandsAndCollapsesConfigRow() {
        page.navigate(baseUrl + "/guides/redis-reference");
        page.waitForLoadState();

        Locator decoration = page.locator("tr.row-collapsible.row-collapsed .description-decoration").first();
        decoration.scrollIntoViewIfNeeded();
        Locator row = page.locator("tr.row-collapsible").first();

        decoration.click();

        assertTrue(row.locator(".description-expanded").count() > 0,
                "After clicking, description should have 'description-expanded'");
        assertTrue(row.locator(".fa-chevron-up").count() > 0,
                "After expanding, chevron should point up");

        row.locator(".description-decoration").click();

        assertTrue(row.locator(".description-collapsed").count() > 0,
                "After second click, description should be collapsed again");
        assertTrue(row.locator(".fa-chevron-down").count() > 0,
                "After collapsing, chevron should point down again");
    }

    @Test
    void searchFilterHidesNonMatchingConfigRows() {
        page.navigate(baseUrl + "/guides/redis-reference");
        page.waitForLoadState();

        Locator searchInput = page.locator("input[type='search'][placeholder='FILTER CONFIGURATION']").first();

        searchInput.click();
        searchInput.pressSequentially("quarkus.redis.hosts");
        page.waitForTimeout(500);

        int hiddenRows = page.locator("table.configuration-reference tr[style*='display: none']").count();
        assertTrue(hiddenRows > 0,
                "Expected some config rows to be hidden after filtering");

        Locator highlights = page.locator("span.configuration-highlight");
        assertTrue(highlights.count() > 0,
                "Expected highlighted text in matching rows");

        searchInput.fill("");
        page.waitForTimeout(500);

        int hiddenAfterClear = page.locator("table.configuration-reference tr[style*='display: none']").count();
        assertEquals(0, hiddenAfterClear,
                "All config rows should be visible again after clearing the search");
    }

    @Test
    void copyButtonShowsFeedbackOnClick() {
        page.navigate(baseUrl + "/guides/redis-reference");

        Locator copyButton = page.locator("button.btn-copy.inline-btn-copy").first();
        assertTrue(copyButton.count() > 0, "Expected at least one inline copy button");

        copyButton.click();

        assertTrue(copyButton.locator(".fa-check").count() > 0
                        || copyButton.getAttribute("class").contains("fa-check"),
                "After clicking, copy button should show a check icon");
        assertEquals("Copied!", copyButton.getAttribute("title"),
                "After clicking, copy button title should be 'Copied!'");
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
