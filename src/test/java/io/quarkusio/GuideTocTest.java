package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    /**
     * The TOC depth must follow each guide's AsciiDoc {@code :toclevels:} attribute, as the Jekyll
     * site's {@code tocify_asciidoc} filter did. {@code data-level} counts heading tags from
     * {@code h1} = 0, so a guide showing sections down to {@code h5} has a maximum of 4.
     */
    @ParameterizedTest
    @CsvSource({
            "/guides/logging, 4",                                // :toclevels: 4, has h5 sections
            "/guides/qute-reference, 3",                         // :toclevels: 3
            "/guides/getting-started, 2",                        // no :toclevels:, Asciidoctor defaults to 2
            "/guides/cdi-integration, 2",                        // :toclevels: 2
            "/guides/security-oidc-code-flow-authentication, 3", // :toclevels: 4, but no section is deeper than h4
            "/version/main/guides/logging, 4"                    // the versioned copy resolves levels too
    })
    void tocDepthFollowsAsciidocTocLevels(String path, int expectedMaxLevel) {
        assumeTocPluginInstalled();
        page.navigate(baseUrl + path);
        assertTrue(page.locator("nav.roq-toc li[data-level='" + expectedMaxLevel + "']").count() > 0,
                path + ": expected TOC entries at data-level " + expectedMaxLevel);
        assertEquals(0, page.locator("nav.roq-toc li[data-level='" + (expectedMaxLevel + 1) + "']").count(),
                path + ": expected no TOC entries below data-level " + expectedMaxLevel);
    }

    /**
     * Asciidoctor prefixes section titles with their number only when the document asks for it, so the
     * TOC inherits the numbering rather than deciding it.
     */
    @ParameterizedTest
    @CsvSource({
            "/guides/qute-reference, true",                          // :sectnums:
            "/guides/getting-started, true",                         // :sectnums:
            "/guides/cdi-integration, true",                         // :numbered:, the legacy alias
            "/guides/security-oidc-code-flow-authentication, false"  // neither
    })
    void tocEntriesAreNumberedOnlyWhenTheGuideAsksForIt(String path, boolean numbered) {
        assumeTocPluginInstalled();
        page.navigate(baseUrl + path);
        String firstEntry = page.locator("nav.roq-toc > ul > li > a").first().textContent().trim();
        assertEquals(numbered, firstEntry.matches("^\\d+\\. .*"),
                path + ": first TOC entry was '" + firstEntry + "'");
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
