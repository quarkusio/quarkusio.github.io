package io.quarkusio;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Nested;
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

    // These tests exercise behaviour that depends on the custom Liquid filters in
    // _plugins/strings.rb (startswith, endswith). If that plugin is not loaded during
    // the site build, titles and back-links will be wrong and these tests will fail.
    @Nested
    class StringsFilterTests {

        // Ideally we'd test with a numeric version (e.g. 3.17), but the CI build
        // profile excludes numbered version guides, so we use "main" instead.
        private static final String VERSION = "main";

        @Test
        void pageTitleWithoutQuarkusGetsSuffix() {
            page.navigate(baseUrl + "/guides/redis-reference");
            String title = page.title();
            assertTrue(title.endsWith(" - Quarkus"),
                    "A guide whose title does not contain 'Quarkus' should get ' - Quarkus' suffix, but was: " + title);
        }

        @Test
        void versionedGuideTitleIncludesVersion() {
            page.navigate(baseUrl + "/version/" + VERSION + "/guides/redis-reference");
            String title = page.title();
            assertTrue(title.contains(VERSION),
                    "Versioned guide title should include the version, but was: " + title);
        }

        @Test
        void nonVersionedGuideTitleDoesNotIncludeVersionSuffix() {
            page.navigate(baseUrl + "/guides/redis-reference");
            String title = page.title();
            assertFalse(title.matches(".*\\d+\\.\\d+ - Quarkus$"),
                    "Non-versioned guide title should not have a version suffix, but was: " + title);
        }

        @Test
        void versionedGuideBackLinkPointsToVersionedIndex() {
            page.navigate(baseUrl + "/version/" + VERSION + "/guides/redis-reference");
            Locator backLink = page.locator("a.returnlink");
            assertTrue(backLink.count() > 0, "Expected a 'Back to Guides' link");
            String href = backLink.first().getAttribute("href");
            assertTrue(href.contains("/version/" + VERSION + "/guides/"),
                    "Versioned guide 'Back to Guides' should link to versioned index, but was: " + href);
        }

        @Test
        void nonVersionedGuideBackLinkPointsToLatestIndex() {
            page.navigate(baseUrl + "/guides/redis-reference");
            Locator backLink = page.locator("a.returnlink");
            assertTrue(backLink.count() > 0, "Expected a 'Back to Guides' link");
            String href = backLink.first().getAttribute("href");
            assertFalse(href.contains("/version/"),
                    "Non-versioned guide 'Back to Guides' should not link to a versioned index, but was: " + href);
        }

        @Test
        void internalGuideLinksDoNotOpenInNewTab() {
            page.navigate(baseUrl + "/guides/");
            Locator guideLinks = page.locator("qs-guide a[href^='/']");
            assertTrue(guideLinks.count() > 0, "Expected at least one internal guide link");

            for (int i = 0; i < Math.min(guideLinks.count(), 10); i++) {
                String target = guideLinks.nth(i).getAttribute("target");
                String href = guideLinks.nth(i).getAttribute("href");
                assertNull(target,
                        "Internal guide link should not have target attribute, but " + href + " has target='" + target + "'");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesSearchFiltersResults(String path) {
        page.navigate(baseUrl + path);

        Locator searchInput = page.locator("input[type='search']");
        searchInput.waitFor();

        int initialCount = page.locator("qs-guide h4 a").count();
        assertTrue(initialCount >= 50,
                path + ": Expected at least 50 guides before filtering but found " + initialCount);

        searchInput.fill("hibernate");

        // Wait for filtering: either the web component replaces content with search hits,
        // or the local fallback hides non-matching guides
        page.waitForCondition(() -> {
            boolean hasSearchHits = page.locator("[aria-label='Search Hits']").count() > 0;
            boolean guidesFiltered = page.locator("qs-guide h4 a").count() < initialCount;
            return hasSearchHits || guidesFiltered;
        });

        int filteredCount;
        if (page.locator("[aria-label='Search Hits']").count() > 0) {
            filteredCount = page.locator("[aria-label='Search Hits'] .qs-guide").count();
        } else {
            filteredCount = page.locator("qs-guide h4 a").count();
        }

        assertTrue(filteredCount > 0,
                path + ": Expected some guides to match 'hibernate'");
        assertTrue(filteredCount < initialCount,
                path + ": Expected fewer guides after filtering, but got " + filteredCount + " (was " + initialCount + ")");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/getting-started",
            "/guides/rest",
            "/guides/cdi"
    })
    void guideAdmonitionsHaveFontIcons(String path) {
        page.navigate(baseUrl + path);
        int admonitionCount = page.locator(".admonitionblock").count();
        assertTrue(admonitionCount > 0,
                path + ": Expected admonition blocks (NOTE, TIP, WARNING, etc.)");
        int iconCount = page.locator(".admonitionblock td.icon i[class*='icon-']").count();
        assertTrue(iconCount > 0,
                path + ": Found " + admonitionCount + " admonition blocks but no font-based icon elements. "
                        + "Check that the AsciiDoc :icons: font attribute is set.");
    }

    @Test
    void guidesIndexWebComponentsAreRegistered() {
        page.navigate(baseUrl + "/guides/");
        page.waitForLoadState();
        Boolean defined = (Boolean) page.evaluate(
                "() => customElements.get('qs-guide') !== undefined");
        assertTrue(defined,
                "The qs-guide custom element is not registered. "
                        + "The search-wc.js script that renders guide type icons may not be loaded.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageSearchFieldIsBlankOnLoad(String path) {
        page.navigate(baseUrl + path);

        // Find the search input in the "By Category" filter bar
        Locator searchInput = page.locator("input[type='search'][name='q']");
        searchInput.waitFor();

        String searchValue = searchInput.inputValue();
        assertEquals("", searchValue,
                path + ": Expected search field to be blank on page load but found: '" + searchValue + "'");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageDoesNotContainNotFoundText(String path) {
        page.navigate(baseUrl + path);

        // Check that "NOT_FOUND" does not appear in the visible page content
        String pageContent = page.content();
        int notFoundCount = countOccurrences(pageContent, "NOT_FOUND");
        assertEquals(0, notFoundCount,
                path + ": Found " + notFoundCount + " occurrences of 'NOT_FOUND' text. "
                        + "This typically indicates missing data file entries (e.g., index-docs.texts.all_categories)");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guidesPageCategoryDropdownDefaultOptionIsNotNotFound(String path) {
        page.navigate(baseUrl + path);

        // Check the default option in the category dropdown under "By Category" label
        Locator categorySelect = page.locator("select[name='categories']");
        categorySelect.waitFor();

        Locator defaultOption = categorySelect.locator("option[value='']");
        String optionText = defaultOption.textContent();

        assertFalse(optionText.contains("NOT_FOUND"),
                path + ": Category dropdown default option contains 'NOT_FOUND' (was: '" + optionText + "'). "
                        + "This indicates a missing translation in _data/index-docs.yaml (texts.all_categories)");
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guideElementsHaveValidStatusAttribute(String path) {
        page.navigate(baseUrl + path);

        Locator guides = page.locator("qs-guide[status]");
        int guideCount = guides.count();
        assertTrue(guideCount > 0, path + ": Expected qs-guide elements with status attribute");

        // Check all guides - status should be empty string or a valid value, never NOT_FOUND
        for (int i = 0; i < guideCount; i++) {
            String status = guides.nth(i).getAttribute("status");
            String title = guides.nth(i).getAttribute("title");

            assertNotNull(status, path + ": Guide '" + title + "' has null status attribute");
            assertFalse(status.equals("NOT_FOUND"),
                    path + ": Guide '" + title + "' has status='NOT_FOUND'. "
                            + "The guide index should provide a status field (e.g., 'stable', 'preview', '') "
                            + "or the template should use a default value (empty string) instead of NOT_FOUND.");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guideElementsHaveValidKeywordsAttribute(String path) {
        page.navigate(baseUrl + path);

        Locator guides = page.locator("qs-guide[keywords]");
        int guideCount = guides.count();
        assertTrue(guideCount > 0, path + ": Expected qs-guide elements with keywords attribute");

        // Check all guides - keywords should be empty string or valid comma-separated topics
        for (int i = 0; i < guideCount; i++) {
            String keywords = guides.nth(i).getAttribute("keywords");
            String title = guides.nth(i).getAttribute("title");

            assertNotNull(keywords, path + ": Guide '" + title + "' has null keywords attribute");
            assertFalse(keywords.equals("NOT_FOUND"),
                    path + ": Guide '" + title + "' has keywords='NOT_FOUND'. "
                            + "The guide index should map :topics: frontmatter to the keywords field, "
                            + "or the template should use a default empty string instead of NOT_FOUND.");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void guideElementsHaveValidOriginAttribute(String path) {
        page.navigate(baseUrl + path);

        Locator guides = page.locator("qs-guide[origin]");
        int guideCount = guides.count();
        assertTrue(guideCount > 0, path + ": Expected qs-guide elements with origin attribute");

        // Check all guides - origin should be empty string or valid value like 'quarkus', 'quarkiverse'
        for (int i = 0; i < guideCount; i++) {
            String origin = guides.nth(i).getAttribute("origin");
            String title = guides.nth(i).getAttribute("title");

            assertNotNull(origin, path + ": Guide '" + title + "' has null origin attribute");
            assertFalse(origin.equals("NOT_FOUND"),
                    path + ": Guide '" + title + "' has origin='NOT_FOUND'. "
                            + "The guide index should provide an origin field (e.g., 'quarkus', 'quarkiverse', '') "
                            + "or the template should use a default value (empty string) instead of NOT_FOUND.");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/",
            "/version/main/guides/"
    })
    void knownGuideHasExpectedAttributes(String path) {
        page.navigate(baseUrl + path);

        // Look for "Getting Started" guide - a stable guide that should always exist
        // Don't assume order, use attribute selector
        Locator gettingStartedGuide = page.locator("qs-guide[url*='getting-started']").first();

        assertTrue(gettingStartedGuide.count() > 0,
                path + ": Expected to find a guide with URL containing 'getting-started'");

        String title = gettingStartedGuide.getAttribute("title");
        String status = gettingStartedGuide.getAttribute("status");
        String keywords = gettingStartedGuide.getAttribute("keywords");
        String origin = gettingStartedGuide.getAttribute("origin");
        String categories = gettingStartedGuide.getAttribute("categories");

        // Verify the guide exists and has a title
        assertNotNull(title, path + ": Getting Started guide should have a title");
        assertTrue(title.toLowerCase().contains("getting") || title.toLowerCase().contains("first"),
                path + ": Expected 'Getting Started' or 'First Application' in title, got: " + title);

        // Verify attributes are not NOT_FOUND (should be empty string or valid values)
        assertFalse("NOT_FOUND".equals(status),
                path + ": Getting Started guide has status='NOT_FOUND'");
        assertFalse("NOT_FOUND".equals(keywords),
                path + ": Getting Started guide has keywords='NOT_FOUND'");
        assertFalse("NOT_FOUND".equals(origin),
                path + ": Getting Started guide has origin='NOT_FOUND'");

        // Verify categories is populated (this comes from AsciiDoc :categories: frontmatter)
        assertNotNull(categories, path + ": Getting Started guide should have categories");
        assertTrue(categories.contains("getting-started"),
                path + ": Getting Started guide categories should contain 'getting-started', got: " + categories);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/getting-started",
            "/guides/rest",
            "/guides/cdi"
    })
    void guideTitleIsNotDuplicated(String path) {
        page.navigate(baseUrl + path);
        int h1Count = page.locator(".guide h1").count();
        assertEquals(1, h1Count,
                path + ": Expected exactly one h1 in the guide content area, but found " + h1Count
                        + ". The title may be rendered by both the layout and the AsciiDoc content.");
    }

    // The tests below verify rendered output that depends on _plugins/regex_filter.rb (the
    // replace_regex Liquid filter). They will break if the regex filter plugin is not working properly.

    @Test
    void latestGuideVersionDropdownShowsShortVersionNumber() {
        page.navigate(baseUrl + "/guides/getting-started");

        Locator selectedOption = page.locator("#guide-version-dropdown option[selected]");
        assertTrue(selectedOption.count() > 0,
                "Expected a selected option in the version dropdown");

        String text = selectedOption.first().textContent().trim();
        assertFalse(text.contains(".Final"),
                "Version dropdown should show trimmed version (not '.Final'), got: " + text);
        assertTrue(text.toLowerCase().contains("latest"),
                "Version dropdown for default guide should show 'Latest', got: " + text);
    }

    @Test
    void versionedGuideCanonicalUrlOmitsVersionPrefix() {
        page.navigate(baseUrl + "/version/main/guides/getting-started");

        Locator canonical = page.locator("link[rel='canonical']");
        assertTrue(canonical.count() > 0, "Expected a canonical link element");

        String href = canonical.first().getAttribute("href");
        assertNotNull(href, "Canonical link should have an href");
        assertFalse(href.contains("/version/main/"),
                "Canonical URL should not contain '/version/main/', got: " + href);
        assertTrue(href.contains("/guides/getting-started"),
                "Canonical URL should contain the guide path, got: " + href);
    }

    @Test
    void versionedGuideDropdownHasCorrectVersionSelected() {
        page.navigate(baseUrl + "/version/main/guides/getting-started");

        Locator selectedOption = page.locator("#guide-version-dropdown option[selected]");
        assertTrue(selectedOption.count() > 0,
                "Expected a selected option in the version dropdown");

        String selectedValue = selectedOption.first().getAttribute("value");
        assertEquals("main", selectedValue,
                "Version dropdown should have 'main' selected");
    }

    @Test
    void versionedGuideTitleIncludesVersionSuffix() {
        page.navigate(baseUrl + "/version/main/guides/getting-started");

        String title = page.title();
        assertTrue(title.contains("- main"),
                "Versioned guide title should include '- main' suffix, got: " + title);
    }

    @Test
    void nonVersionedGuideTitleOmitsVersionSuffix() {
        page.navigate(baseUrl + "/guides/getting-started");

        String title = page.title();
        assertFalse(title.contains("- main"),
                "Non-versioned guide title should not include '- main', got: " + title);
        assertFalse(title.contains("- latest"),
                "Non-versioned guide title should not include '- latest', got: " + title);
    }
}
