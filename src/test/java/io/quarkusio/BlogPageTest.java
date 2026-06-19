package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlogPageTest extends BrowserTest {

    @Test
    void blogPageHasAtLeastFivePosts() {
        page.navigate(baseUrl + "/blog/");
        int postCount = page.locator(".blog-list-item").count();
        assertTrue(postCount >= 5,
                "Expected at least 5 blog posts but found " + postCount);
    }

    @Test
    void blogPageHasOlderPostsLink() {
        page.navigate(baseUrl + "/blog/");
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
}
