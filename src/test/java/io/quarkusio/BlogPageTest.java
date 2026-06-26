package io.quarkusio;

import java.util.List;

import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainRawHtml;
import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static io.quarkusio.UnrenderedMarkupDetector.findUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class BlogPageTest extends BrowserTest {

    private static final String BLOG_PATH = "/blog/";
    private static final int POSTS_TO_CHECK = 3;

    @Test
    void blogPageTitleIsExact() {
        page.navigate(baseUrl + "/blog/");
        assertEquals("Blog - Quarkus", page.title());
    }

    @Test
    void blogPageHasAtLeastFivePosts() {
        page.navigate(baseUrl + BLOG_PATH);
        int postCount = page.locator(".blog-list-item").count();
        assertTrue(postCount >= 5,
                "Expected at least 5 blog posts but found " + postCount);
    }

    @Test
    void blogPageHasOlderPostsLink() {
        page.navigate(baseUrl + BLOG_PATH);
        int linkCount = page.locator("a:has-text('Older Posts')").count();
        assertTrue(linkCount > 0,
                "Expected an 'Older Posts' link on the blog page");
    }

    @Test
    void blogPageTwoHasContentAndNavigationLinks() {
        page.navigate(baseUrl + "/blog/");
        page.locator("a:has-text('Older Posts')").first().click();
        page.waitForLoadState();

        int postCount = page.locator(".blog-list-item").count();
        assertTrue(postCount >= 5,
                "Expected at least 5 blog posts on page 2 but found " + postCount);

        int olderCount = page.locator("a:has-text('Older Posts')").count();
        assertTrue(olderCount > 0,
                "Expected an 'Older Posts' link on page 2");

        int newerCount = page.locator("a:has-text('Newer Posts')").count();
        assertTrue(newerCount > 0,
                "Expected a 'Newer Posts' link on page 2");
    }

    @Test
    void blogIndexAuthorLinkNavigatesToAuthorPage() {
        page.navigate(baseUrl + BLOG_PATH);
        var authorLink = page.locator(".byline a[href*='/author/']").first();
        String authorName = authorLink.textContent().trim();
        assertFalse(authorName.isEmpty(), "Expected author link to have text");

        authorLink.click();
        page.waitForURL("**/author/**");

        var heading = page.locator(".byline").first();
        assertTrue(heading.textContent().contains(authorName),
                "Expected author page to contain author name '" + authorName + "'");
    }

    @Test
    void blogPostAuthorLinkNavigatesToAuthorPage() {
        page.navigate(baseUrl + BLOG_PATH);
        var postLink = page.locator(".blog-list-item .post-title a").first();
        postLink.click();

        var authorLink = page.locator(".byline a[href*='/author/']").first();
        String authorName = authorLink.textContent().trim();
        assertFalse(authorName.isEmpty(), "Expected author link to have text");

        authorLink.click();
        page.waitForURL("**/author/**");

        var heading = page.locator(".byline").first();
        assertTrue(heading.textContent().contains(authorName),
                "Expected author page to contain author name '" + authorName + "'");
    }

    @Test
    void blogPostLinksUseBlogUrl() {
        page.navigate(baseUrl + BLOG_PATH);
        var postLinks = page.locator(".blog-list-item .post-title a");
        int count = Math.min(postLinks.count(), POSTS_TO_CHECK);
        assertTrue(count > 0, "Expected at least one blog post link");

        for (int i = 0; i < count; i++) {
            String href = postLinks.nth(i).getAttribute("href");
            assertNotNull(href, "Post link should have an href");
            assertTrue(href.contains(BLOG_PATH),
                    "Blog post link should contain " + BLOG_PATH + ": " + href);
        }
    }

    @Test
    void multiAuthorPostShowsAllAuthors() {
        page.navigate(baseUrl + BLOG_PATH);

        String postHref = null;
        int indexAuthorCount = 0;

        while (postHref == null) {
            var posts = page.locator(".blog-list-item");
            int postCount = posts.count();
            for (int i = 0; i < postCount; i++) {
                var post = posts.nth(i);
                int authorCount = post.locator(".byline a[href*='/author/']").count();
                if (authorCount > 1) {
                    postHref = post.locator(".post-title a").getAttribute("href");
                    indexAuthorCount = authorCount;
                    break;
                }
            }
            if (postHref == null) {
                var olderPosts = page.locator("a:has-text('Older Posts')");
                assertFalse(olderPosts.count() == 0,
                        "Could not find any multi-author blog post");
                olderPosts.click();
                page.waitForLoadState();
            }
        }

        page.navigate(postHref.startsWith("http") ? postHref : baseUrl + postHref);
        int postPageAuthorCount = page.locator(".byline a[href*='/author/']").count();
        assertEquals(indexAuthorCount, postPageAuthorCount,
                "Post page should show the same number of authors as the blog index");
        assertTrue(postPageAuthorCount > 1,
                "Expected multiple authors on post page but found " + postPageAuthorCount);
    }

    @Test
    void blogIndexDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + BLOG_PATH);
        assertDoesNotContainUnrenderedMarkup(page, "Blog index");
    }

    @Test
    void blogIndexDoesNotContainRawHtmlTags() {
        page.navigate(baseUrl + BLOG_PATH);
        assertDoesNotContainRawHtml(page, "Blog index");
    }

    @Test
    void blogPostsDoNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + BLOG_PATH);
        var postLinks = page.locator(".blog-list-item .post-title a");
        int count = Math.min(postLinks.count(), POSTS_TO_CHECK);
        assertTrue(count > 0, "Expected at least one blog post link");

        for (int i = 0; i < count; i++) {
            String href = postLinks.nth(i).getAttribute("href");
            String title = postLinks.nth(i).textContent().trim();

            page.navigate(href.startsWith("http") ? href : baseUrl + href);

            String bodyText = page.locator(".doc-content").first().textContent();
            List<String> findings = findUnrenderedMarkup(bodyText);
            assertTrue(findings.isEmpty(),
                    () -> "\"" + title + "\" (" + href + ") contains unrendered markup: " + String.join("; ", findings));

            page.navigate(baseUrl + BLOG_PATH);
        }
    }
}
