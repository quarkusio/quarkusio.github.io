package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GuidesDataBindingTest extends BrowserTest {

    @Test
    void guidesPageRendersGuideListings() {
        page.navigate(baseUrl + "/guides/");
        int guideCount = page.locator("a[href*='/guides/']").count();
        assertTrue(guideCount >= 50,
                "Expected at least 50 guide links but found " + guideCount);
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
}
