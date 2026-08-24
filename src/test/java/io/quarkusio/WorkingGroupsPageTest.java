package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainRawHtml;
import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class WorkingGroupsPageTest extends BrowserTest {

    @Test
    void workingGroupsPageListsAtLeastFive() {
        page.navigate(baseUrl + "/working-groups/");
        int count = page.locator(".card .card-title").count();
        assertTrue(count >= 5,
                "Expected at least 5 working groups but found " + count);
    }

    @Test
    void workingGroupsPageDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + "/working-groups/");
        assertDoesNotContainUnrenderedMarkup(page, "Working groups page");
    }

    @Test
    void workingGroupsPageDoesNotContainRawHtmlTags() {
        page.navigate(baseUrl + "/working-groups/");
        assertDoesNotContainRawHtml(page, "Working groups page");
    }

    @Test
    void workingGroupsCardTitlesAreNotTemplateExpressions() {
        page.navigate(baseUrl + "/working-groups/");
        Locator titles = page.locator(".card .card-title");
        int count = titles.count();
        assertTrue(count >= 1, "Expected at least 1 card title");
        for (int i = 0; i < count; i++) {
            String title = titles.nth(i).innerText().trim();
            assertFalse(title.matches("\\{.*\\..*}"),
                    "Card title looks like an unresolved template expression: " + title);
            assertFalse(title.isEmpty(), "Card title should not be empty");
        }
    }

    @Test
    void workingGroupsCardsHaveDistinctTitles() {
        page.navigate(baseUrl + "/working-groups/");
        Locator titles = page.locator(".card .card-title");
        int count = titles.count();
        assertTrue(count >= 5, "Expected at least 5 card titles");
        long distinctCount = java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> titles.nth(i).innerText().trim())
                .distinct()
                .count();
        assertTrue(distinctCount >= 5,
                "Expected at least 5 distinct card titles but found " + distinctCount
                        + " (all cards having the same title suggests unresolved template data)");
    }
}
