package io.quarkusio;

import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class GuidesPageTest extends BrowserTest {

    @Test
    void guidesPageTitleIsExact() {
        page.navigate(baseUrl + "/guides/");
        assertEquals("Guides - Latest - Quarkus", page.title());
    }

    @Test
    void guidesPageRendersWithSubstantialContent() {
        page.navigate(baseUrl + "/guides/");
        int guideCount = page.locator(".docslist h4 a[href*='/guides/']").count();
        assertTrue(guideCount >= 50,
                "Expected at least 50 guides but found " + guideCount);
    }

    @Test
    void guidesPageHasSearchInput() {
        page.navigate(baseUrl + "/guides/");
        int searchCount = page.locator("input[type='search'], input[placeholder*='ilter'], input[placeholder*='earch'], .search input").count();
        assertTrue(searchCount > 0,
                "Expected a search/filter input on the guides page");
    }

    @Test
    void guidesPageHasCategorySections() {
        page.navigate(baseUrl + "/guides/");
        int sectionCount = page.locator(".doclist-header").count();
        assertTrue(sectionCount >= 3,
                "Expected at least 3 guide category sections but found " + sectionCount);
    }

    @Test
    void guidesPageHasVersionSelector() {
        page.navigate(baseUrl + "/guides/");
        int versionCount = page.locator(".pulldown.version, select:has(option), [class*='version']").count();
        assertTrue(versionCount > 0,
                "Expected a version selector on the guides page");
    }

    @Test
    void guidesPageHasCategoryFilter() {
        page.navigate(baseUrl + "/guides/");
        int categoryCount = page.locator(".pulldown.category, [class*='category']").count();
        assertTrue(categoryCount > 0,
                "Expected a category filter on the guides page");
    }

    @Test
    void guidesPageDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + "/guides/");
        assertDoesNotContainUnrenderedMarkup(page, "Guides page");
    }
}
