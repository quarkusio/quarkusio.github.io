package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SiteMetadataTest extends BrowserTest {

    @Test
    void pageTitleContainsQuarkus() {
        page.navigate(baseUrl + "/");
        String title = page.title();
        assertTrue(title.toLowerCase().contains("quarkus"),
                "Expected page title to contain 'Quarkus' but was: " + title);
    }

    @Test
    void metaDescriptionIsPresent() {
        page.navigate(baseUrl + "/");
        String description = page.locator("meta[name='description']").getAttribute("content");
        assertNotNull(description, "Expected a meta description tag");
        assertFalse(description.isBlank(), "Meta description should not be blank");
        assertTrue(description.toLowerCase().contains("quarkus"),
                "Expected meta description to mention Quarkus but was: " + description);
    }

    @Test
    void contentSecurityPolicyIsNonEmpty() {
        page.navigate(baseUrl + "/");
        String csp = page.locator("meta[http-equiv='Content-Security-Policy']").getAttribute("content");
        assertNotNull(csp, "Expected a Content-Security-Policy meta tag");
        assertTrue(csp.contains("script-src"),
                "CSP should contain a script-src directive but was: " + csp);
    }

    @Test
    void guidesPageTitleContainsQuarkus() {
        page.navigate(baseUrl + "/guides/");
        String title = page.title();
        assertTrue(title.toLowerCase().contains("quarkus"),
                "Expected guides page title to contain 'Quarkus' but was: " + title);
    }
}
