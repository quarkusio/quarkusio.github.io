package io.quarkusio;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HreflangTest extends BrowserTest {

    @SuppressWarnings("unchecked")
    private List<String> hreflangHrefs(String path, String lang) {
        page.navigate(baseUrl + path);
        return (List<String>) page.locator("link[rel='alternate'][hreflang='" + lang + "']")
                .evaluateAll("els => els.map(e => e.getAttribute('href'))");
    }

    private void assertAlternate(String path, String lang, String expected) {
        assertEquals(List.of(expected), hreflangHrefs(path, lang),
                "hreflang=" + lang + " alternate for " + path);
    }

    @Test
    void homePageAlternatesHaveNoDoubledSlash() {
        assertAlternate("/", "en", "https://quarkus.io/");
        assertAlternate("/", "pt-br", "https://pt.quarkus.io/");
    }

    @Test
    void subPageAlternatesKeepPathAndTrailingSlash() {
        assertAlternate("/blog/", "en", "https://quarkus.io/blog/");
        assertAlternate("/blog/", "pt-br", "https://pt.quarkus.io/blog/");
        assertAlternate("/blog/", "es", "https://es.quarkus.io/blog/");
    }

    @Test
    void alternatesAreWellFormed() {
        page.navigate(baseUrl + "/about/");
        @SuppressWarnings("unchecked")
        List<String> hrefs = (List<String>) page.locator("link[rel='alternate'][hreflang]")
                .evaluateAll("els => els.map(e => e.getAttribute('href'))");
        assertFalse(hrefs.isEmpty(), "Expected hreflang alternates");
        for (String href : hrefs) {
            assertTrue(href.matches("https://([a-z]+\\.)?quarkus\\.io/(about/)?"),
                    "Malformed hreflang href: " + href);
            assertFalse(href.substring("https://".length()).contains("//"), "Doubled slash in: " + href);
        }
    }
}
