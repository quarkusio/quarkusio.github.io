package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class GuideTocTest extends BrowserTest {

    private void assumeTocPluginInstalled() {
        page.navigate(baseUrl + "/guides/getting-started");
        assumeTrue(page.locator("nav.roq-toc").count() > 0,
                "TOC plugin not installed — skipping TOC tests");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/guides/getting-started",
            "/guides/rest",
            "/guides/cdi",
            "/version/main/guides/getting-started"
    })
    void guideHasTocWithMultipleEntries(String path) {
        assumeTocPluginInstalled();
        page.navigate(baseUrl + path);
        Locator tocItems = page.locator("nav.roq-toc li");
        assertTrue(tocItems.count() >= 3,
                path + ": Expected at least 3 TOC entries but found " + tocItems.count());
    }

    @Test
    void tocLinksPointToAnchorsOnPage() {
        assumeTocPluginInstalled();
        page.navigate(baseUrl + "/guides/getting-started");
        Locator tocLinks = page.locator("nav.roq-toc a[href^='#']");
        assertTrue(tocLinks.count() > 0, "Expected TOC links with anchor hrefs");

        for (int i = 0; i < Math.min(tocLinks.count(), 10); i++) {
            String href = tocLinks.nth(i).getAttribute("href");
            String id = href.substring(1);
            int targetCount = page.locator("[id='" + id + "']").count();
            assertTrue(targetCount > 0,
                    "TOC link " + href + " does not point to an element on the page");
        }
    }

    @Test
    void tocHasNestedStructure() {
        assumeTocPluginInstalled();
        page.navigate(baseUrl + "/guides/cdi");
        Locator nestedLists = page.locator("nav.roq-toc ul ul");
        assertTrue(nestedLists.count() > 0,
                "Expected nested <ul> elements in the TOC for a guide with sub-sections");
    }

    @Test
    void tocIsInsideStickyWrapper() {
        assumeTocPluginInstalled();
        page.navigate(baseUrl + "/guides/getting-started");
        Locator toc = page.locator(".toc nav.roq-toc");
        assertTrue(toc.count() > 0,
                "TOC nav should be inside the .toc wrapper div");
    }

    @Test
    void tocIsHiddenWhenEmpty() {
        page.navigate(baseUrl + "/guides/getting-started");
        Locator tocWrapper = page.locator(".toc");
        if (tocWrapper.count() > 0 && page.locator("nav.roq-toc").count() == 0) {
            assertFalse(tocWrapper.first().isVisible(),
                    "Empty .toc wrapper should be hidden by CSS");
        }
    }
}
