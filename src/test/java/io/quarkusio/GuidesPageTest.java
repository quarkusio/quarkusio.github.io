package io.quarkusio;

import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class GuidesPageTest extends BrowserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageRendersWithSubstantialContent(String path) {
        page.navigate(baseUrl + path);
        int guideCount = page.locator(".docslist h4 a[href*='/guides/']").count();
        assertTrue(guideCount >= 50,
                path + ": Expected at least 50 guides but found " + guideCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageHasSearchInput(String path) {
        page.navigate(baseUrl + path);
        int searchCount = page.locator("input[type='search'], input[placeholder*='ilter'], input[placeholder*='earch'], .search input").count();
        assertTrue(searchCount > 0,
                path + ": Expected a search/filter input on the guides page");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageHasCategorySections(String path) {
        page.navigate(baseUrl + path);
        int sectionCount = page.locator(".doclist-header").count();
        assertTrue(sectionCount >= 3,
                path + ": Expected at least 3 guide category sections but found " + sectionCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageHasVersionSelector(String path) {
        page.navigate(baseUrl + path);
        int versionCount = page.locator(".pulldown.version, select:has(option), [class*='version']").count();
        assertTrue(versionCount > 0,
                path + ": Expected a version selector on the guides page");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageHasCategoryFilter(String path) {
        page.navigate(baseUrl + path);
        int categoryCount = page.locator(".pulldown.category, [class*='category']").count();
        assertTrue(categoryCount > 0,
                path + ": Expected a category filter on the guides page");
    }

    @Test
    void guidesPageTitleIsExact() {
        page.navigate(baseUrl + "/guides/");
        assertEquals("Guides - Latest - Quarkus", page.title());
    }

    @Test
    void guidesPageDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + "/guides/");
        assertDoesNotContainUnrenderedMarkup(page, "Guides page");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/getting-started",
            "/guides/rest",
            "/guides/hibernate-orm",
            "/guides/cdi",
            "/guides/getting-started-testing",
            "/guides/security-overview",
            "/version/main/guides/getting-started",
            "/version/main/guides/rest"
    })
    void specificGuideReturns200(String path) {
        Response response = page.navigate(baseUrl + path);
        assertNotNull(response, "No response for " + path);
        assertEquals(200, response.status(),
                "Expected 200 for " + path + " but got " + response.status());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/getting-started",
            "/guides/rest",
            "/guides/hibernate-orm",
            "/version/main/guides/getting-started"
    })
    void specificGuideHasContent(String path) {
        page.navigate(baseUrl + path);
        String title = page.title();
        assertFalse(title == null || title.isBlank(),
                path + ": Guide page title should not be blank");
        assertTrue(title.toLowerCase().contains("quarkus"),
                path + ": Guide page title should contain 'Quarkus', got: " + title);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/getting-started",
            "/guides/rest",
            "/version/main/guides/getting-started"
    })
    void specificGuideDoesNotContainUnrenderedMarkup(String path) {
        page.navigate(baseUrl + path);
        assertDoesNotContainUnrenderedMarkup(page, path);
    }
}
