package io.quarkusio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TagTest extends BrowserTest {

    private static final String BLOG_PATH = "/blog/";

    @Test
    void blogIndexShowsTagsOnPosts() {
        page.navigate(baseUrl + BLOG_PATH);
        int taggedPosts = page.locator(".blog-list-item .tags").count();
        assertTrue(taggedPosts > 0,
                "Expected at least one post on the blog index to display tags");
    }

    @Test
    void blogIndexTagLinksPointToTagArchive() {
        page.navigate(baseUrl + BLOG_PATH);
        var tagLinks = page.locator(".blog-list-item .tags a");
        int count = tagLinks.count();
        assertTrue(count > 0, "Expected at least one tag link on the blog index");

        for (int i = 0; i < count; i++) {
            String href = tagLinks.nth(i).getAttribute("href");
            assertNotNull(href, "Tag link should have an href");
            assertTrue(href.contains("/blog/tag/"),
                    "Tag link should point to /blog/tag/: " + href);
        }
    }

    @Test
    void blogIndexTagLinksDoNotContainCommas() {
        page.navigate(baseUrl + BLOG_PATH);
        var tagLinks = page.locator(".blog-list-item .tags a");
        int count = tagLinks.count();
        assertTrue(count > 0, "Expected at least one tag link");

        for (int i = 0; i < count; i++) {
            var link = tagLinks.nth(i);
            String href = link.getAttribute("href");
            assertNotNull(href, "Tag link should have an href");
            assertFalse(href.contains(","),
                    "Tag URL should not contain commas: " + href);
            String text = link.textContent();
            assertFalse(text.contains(","),
                    "Tag display text should not contain commas: " + text);
        }
    }

    @Test
    void tagSidebarExistsOnBlogIndex() {
        page.navigate(baseUrl + BLOG_PATH);
        var tagsLabel = page.locator(".tags-label");
        assertTrue(tagsLabel.count() > 0,
                "Expected a 'Tags' label in the sidebar");
        assertEquals("Tags", tagsLabel.first().textContent().trim());

        var sidebarTagLinks = page.locator(".tags-label").locator("xpath=..").locator("a[href*='/blog/tag/']");
        assertTrue(sidebarTagLinks.count() > 0,
                "Expected tag links in the sidebar");
    }

    @Test
    void clickingTagNavigatesToTagArchivePage() {
        page.navigate(baseUrl + BLOG_PATH);
        var tagLink = page.locator(".blog-list-item .tags a").first();
        String tagText = tagLink.textContent().trim().replace("#", "");
        tagLink.click();
        page.waitForURL("**/blog/tag/**");

        var heading = page.locator("h2.title");
        assertTrue(heading.count() > 0, "Tag archive page should have an h2 heading");
        String headingText = heading.first().textContent();
        assertTrue(headingText.toLowerCase().contains(tagText.toLowerCase()),
                "Tag archive heading '" + headingText + "' should contain the tag '" + tagText + "'");

        int postCount = page.locator(".blog-list-item").count();
        assertTrue(postCount > 0,
                "Tag archive page should list at least one post");

        var tagsLabel = page.locator(".tags-label");
        assertTrue(tagsLabel.count() > 0,
                "Tag archive page should have a tag sidebar");
    }

    @Test
    void blogPostPageShowsTagsWithLinks() {
        page.navigate(baseUrl + BLOG_PATH);
        var postLink = page.locator(".blog-list-item .post-title a").first();
        postLink.click();
        page.waitForLoadState();

        var tags = page.locator(".post-date .tags a");
        int tagCount = tags.count();
        assertTrue(tagCount > 0,
                "Blog post page should display tags with links");

        for (int i = 0; i < tagCount; i++) {
            String href = tags.nth(i).getAttribute("href");
            assertTrue(href.contains("/blog/tag/"),
                    "Post page tag link should point to /blog/tag/: " + href);
        }
    }

    @Test
    void blogPostTagLinksNavigateToWorkingPages() {
        page.navigate(baseUrl + BLOG_PATH);
        var postLink = page.locator(".blog-list-item .post-title a").first();
        postLink.click();
        page.waitForLoadState();

        var tagLinks = page.locator(".post-date .tags a");
        assertTrue(tagLinks.count() > 0,
                "Blog post page should display at least one tag link");
        String href = tagLinks.first().getAttribute("href");
        assertNotNull(href, "Tag link should have an href");

        page.navigate(href.startsWith("http") ? href : baseUrl + href);
        var heading = page.locator("h2.title");
        assertTrue(heading.count() > 0,
                "Navigating to tag link should reach a page with a heading, not a 404");
        assertTrue(heading.first().textContent().contains("Tagged posts"),
                "Tag page heading should say 'Tagged posts'");
    }

    @Test
    void blogSidebarTagsHaveNoDuplicates() {
        page.navigate(baseUrl + BLOG_PATH);
        var sidebarTagLinks = page.locator(".tags-label").locator("xpath=..").locator("a[href*='/blog/tag/']");
        int count = sidebarTagLinks.count();
        assertTrue(count > 0, "Expected at least one tag in the sidebar");

        List<String> tagTexts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tagTexts.add(sidebarTagLinks.nth(i).textContent().trim().toLowerCase());
        }

        List<String> duplicates = tagTexts.stream()
                .filter(t -> Collections.frequency(tagTexts, t) > 1)
                .distinct()
                .toList();

        assertTrue(duplicates.isEmpty(),
                "Sidebar contains duplicate tags (case-insensitive): " + duplicates);
    }

    @Test
    void tagDisplayTextStartsWithHash() {
        page.navigate(baseUrl + BLOG_PATH);
        var tagLinks = page.locator(".blog-list-item .tags a");
        int count = Math.min(tagLinks.count(), 10);
        assertTrue(count > 0, "Expected at least one tag link");

        for (int i = 0; i < count; i++) {
            String text = tagLinks.nth(i).textContent().trim();
            assertTrue(text.startsWith("#"),
                    "Tag display text should start with '#': " + text);
        }
    }
}
