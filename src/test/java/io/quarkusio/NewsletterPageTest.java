package io.quarkusio;

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
    void newsletterWithBibqueryRendersPublicationCards() {
        page.navigate(baseUrl + "/blog/quarkus-newsletter-13/");
        int cardCount = page.locator(".card").count();
        assertTrue(cardCount >= 10,
                "Expected at least 10 publication cards from where_exp filtering but found " + cardCount);
    }
}
