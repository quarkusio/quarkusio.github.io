package io.quarkusio;

import com.microsoft.playwright.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NewsletterPageTest extends BrowserTest {

    @Test
    void newsletterIndexHasSubstantialEditions() {
        page.navigate(baseUrl + "/newsletter/");
        int cardCount = page.locator(".card").count();
        assertTrue(cardCount >= 20,
                "Expected at least 20 newsletter editions but found " + cardCount);
    }

    @Test
    void newsletterEditionsLinkToValidPaths() {
        page.navigate(baseUrl + "/newsletter/");
        int linkCount = page.locator(".card a[href*='/newsletter/']").count();
        assertTrue(linkCount >= 10,
                "Expected at least 10 newsletter links but found " + linkCount);
    }

    @Test
    void newsletterEditionShowsTitle() {
        page.navigate(baseUrl + "/newsletter/");
        int titleCount = page.locator(".card .title").count();
        assertTrue(titleCount >= 20,
                "Expected newsletter cards to have titles but found " + titleCount);
    }

    @Test
    void newsletterEditionPageLoadsSuccessfully() {
        Response response = page.navigate(baseUrl + "/newsletter/69/");
        assertNotNull(response, "Expected a response from /newsletter/69/");
        assertEquals(200, response.status(),
                "Expected HTTP 200 for /newsletter/69/ but got " + response.status());
    }

    @Test
    void newsletterEditionPageHasTitle() {
        page.navigate(baseUrl + "/newsletter/69/");
        String title = page.title();
        assertNotNull(title, "Expected page to have a title");
        assertTrue(title.toLowerCase().contains("newsletter"),
                "Expected page title to contain 'newsletter' but was: " + title);
    }

    @Test
    void newsletterEditionPageHasContent() {
        page.navigate(baseUrl + "/newsletter/69/");
        String bodyText = page.locator("body").textContent();
        assertTrue(bodyText.length() > 500,
                "Expected newsletter edition page to have substantial content but body was only " + bodyText.length() + " chars");
    }

    @Test
    void olderNewsletterEditionPageLoads() {
        Response response = page.navigate(baseUrl + "/newsletter/38/");
        assertNotNull(response, "Expected a response from /newsletter/38/");
        assertEquals(200, response.status(),
                "Expected HTTP 200 for /newsletter/38/ but got " + response.status());
        String bodyText = page.locator("body").textContent();
        assertTrue(bodyText.length() > 500,
                "Expected older newsletter edition to have substantial content");
    }

    @Test
    void newsletterWithBibqueryRendersPublicationCards() {
        page.navigate(baseUrl + "/blog/quarkus-newsletter-13/");
        int cardCount = page.locator(".card").count();
        assertTrue(cardCount >= 10,
                "Expected at least 10 publication cards from where_exp filtering but found " + cardCount);
    }

    @Test
    void recentBlogNewsletterPostLoadsSuccessfully() {
        Response response = page.navigate(baseUrl + "/blog/quarkus-newsletter-68/");
        assertNotNull(response, "Expected a response from /blog/quarkus-newsletter-68/");
        assertEquals(200, response.status(),
                "Expected HTTP 200 for /blog/quarkus-newsletter-68/ but got " + response.status());
    }

    @Test
    void recentBlogNewsletterPostHasContent() {
        page.navigate(baseUrl + "/blog/quarkus-newsletter-68/");
        String bodyText = page.locator(".doc-content").first().textContent();
        assertTrue(bodyText.length() > 200,
                "Expected blog newsletter post to have substantial content but found only " + bodyText.length() + " chars");
    }

    @Test
    void recentBlogNewsletterPostHasNewsletterTitle() {
        page.navigate(baseUrl + "/blog/quarkus-newsletter-68/");
        String title = page.title();
        assertTrue(title.contains("Newsletter"),
                "Expected blog post title to contain 'Newsletter' but was: " + title);
    }
}
