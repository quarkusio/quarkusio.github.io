package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BlogPageTest extends BrowserTest {

    @Test
    void blogPageTitleIsExact() {
        page.navigate(baseUrl + "/blog/");
        assertEquals("Blog - Quarkus", page.title());
    }

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
}
