package io.quarkusio;

import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainRawHtml;
import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class AuthorPageTest extends BrowserTest {

    @Test
    void authorIndexPageListsAuthors() {
        page.navigate(baseUrl + "/author/");
        int authorCount = page.locator(".authors a[href*='/author/']").count();
        assertTrue(authorCount >= 5,
                "Expected at least 5 authors but found " + authorCount);
    }

    @Test
    void authorPageShowsAuthorName() {
        page.navigate(baseUrl + "/blog/");
        var authorLink = page.locator(".byline a[href*='/author/']").first();
        String authorName = authorLink.textContent().trim();
        authorLink.click();

        page.waitForURL("**/author/**");
        var heading = page.locator(".byline").first();
        assertTrue(heading.textContent().contains(authorName),
                "Expected author page to show author name '" + authorName + "'");
    }

    @Test
    void authorPageListsPosts() {
        page.navigate(baseUrl + "/blog/");
        var authorLink = page.locator(".byline a[href*='/author/']").first();
        authorLink.click();

        page.waitForURL("**/author/**");
        int postCount = page.locator(".blog-list-item").count();
        assertTrue(postCount >= 1,
                "Expected at least 1 post on author page but found " + postCount);
    }

    @Test
    void authorIndexDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + "/author/");
        assertDoesNotContainUnrenderedMarkup(page, "Author index");
    }

    @Test
    void authorIndexDoesNotContainRawHtmlTags() {
        page.navigate(baseUrl + "/author/");
        assertDoesNotContainRawHtml(page, "Author index");
    }
}
