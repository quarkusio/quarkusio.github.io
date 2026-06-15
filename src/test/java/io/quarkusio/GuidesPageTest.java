package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GuidesPageTest extends BrowserTest {

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
}
