package io.quarkusio;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SiteMetadataTest extends BrowserTest {

    private static final String HASH_TITLE_POST = "/blog/insights-2/";
    private static final String HASH_TITLE = "Quarkus Insights #2: quarkus:dev and Command mode";

    @Test
    void homepageTitleIsExact() {
        page.navigate(baseUrl + "/");
        assertEquals("Quarkus - Supersonic Subatomic Java", page.title());
    }

    @Test
    void shareLinksKeepTitlesWithSpecialCharactersIntact() {
        page.navigate(baseUrl + HASH_TITLE_POST);

        // A '#' left unencoded would start the URL fragment, cutting off the title and every parameter after it
        URI x = URI.create(page.locator("a.share-x").first().getAttribute("href"));
        assertNull(x.getRawFragment(), "X share link has a fragment, so the title was not encoded: " + x);
        Map<String, String> xParams = queryParams(x);
        assertEquals(HASH_TITLE, xParams.get("text"));
        assertTrue(xParams.getOrDefault("url", "").endsWith(HASH_TITLE_POST),
                "X share link lost the post URL: " + x);

        URI linkedIn = URI.create(page.locator("a.share-linkedin").first().getAttribute("href"));
        assertNull(linkedIn.getRawFragment(), "LinkedIn share link has a fragment: " + linkedIn);
        assertEquals(HASH_TITLE, queryParams(linkedIn).get("title"));

        String mail = page.locator("a.share-email").first().getAttribute("href");
        assertFalse(mail.contains("#"), "Email share link has an unencoded '#': " + mail);
        assertFalse(mail.contains(" "), "Email share link has an unencoded space: " + mail);
    }

    @Test
    void metaDescriptionKeepsQuotes() {
        page.navigate(baseUrl + "/blog/the-big-rename/");
        String expected = "previously included the word \"reactive\" in their names";
        assertTrue(page.locator("meta[name=description]").getAttribute("content").contains(expected),
                "meta description was cut short at a quote");
        assertTrue(page.locator("meta[property='og:description']").getAttribute("content").contains(expected),
                "og:description was cut short at a quote");
    }

    private static Map<String, String> queryParams(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(p -> p.split("=", 2))
                .collect(Collectors.toMap(
                        p -> p[0],
                        p -> p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "",
                        (a, b) -> a));
    }
}
