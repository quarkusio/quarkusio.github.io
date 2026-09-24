package io.quarkusio;

import java.util.List;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainRawHtml;
import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static io.quarkusio.UnrenderedMarkupDetector.findUnresolvedPlaceholders;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorkshopsPageTest extends BrowserTest {

    public static final String WORKSHOPS_PATH = "/workshops/";

    @Test
    void workingGroupsPageListsAGoodNumber() {
        page.navigate(baseUrl + WORKSHOPS_PATH);
        int count = page.locator(".card .card-title").count();
        assertTrue(count >= 2,
                "Expected several workshops but found " + count);
    }

    @Test
    void workingGroupsPageDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + WORKSHOPS_PATH);
        assertDoesNotContainUnrenderedMarkup(page, "Workshops page");
    }

    @Test
    void workingGroupsPageDoesNotContainRawHtmlTags() {
        page.navigate(baseUrl + WORKSHOPS_PATH);
        assertDoesNotContainRawHtml(page, "Workshops page");
    }

    @Test
    void workingGroupsCardsDoNotContainUnresolvedPlaceholders() {
        page.navigate(baseUrl + WORKSHOPS_PATH);
        Locator cards = page.locator(".card");
        int count = cards.count();
        assertTrue(count >= 1, "Expected at least 1 card");
        for (int i = 0; i < count; i++) {
            String text = cards.nth(i).innerText().trim();
            List<String> findings = findUnresolvedPlaceholders(text);
            assertTrue(findings.isEmpty(),
                    "Card " + i + " contains unresolved placeholders: " + findings);
        }
    }

    @Test
    void workingGroupsCardsHaveDistinctTitles() {
        page.navigate(baseUrl + WORKSHOPS_PATH);
        Locator titles = page.locator(".card .card-title");
        int count = titles.count();
        assertTrue(count >= 2, "Expected several card titles");
        long distinctCount = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> titles.nth(i).innerText().trim())
                .distinct()
                .count();
        assertEquals(count, distinctCount,
                "Expected several distinct card titles but found " + distinctCount
                        + " (all cards having the same title suggests unresolved template data)");
    }
}
