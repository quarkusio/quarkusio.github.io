package io.quarkusio;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;

public class SitemapTest extends BrowserTest {

    @Override
    @BeforeEach
    void createContext() {
    }

    @Override
    @AfterEach
    void closeContext() {
    }

    private HttpResponse<String> fetchSitemap() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/sitemap.xml"))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private Document parseSitemap() throws IOException, InterruptedException, ParserConfigurationException, SAXException {
        HttpResponse<String> response = fetchSitemap();
        String body = response.body();
        assertFalse(body.isEmpty(), "Sitemap response body should not be empty");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
    }

    @Test
    void sitemapReturns200() throws IOException, InterruptedException {
        HttpResponse<String> response = fetchSitemap();
        assertEquals(200, response.statusCode(),
                "Expected 200 for /sitemap.xml but got " + response.statusCode());
    }

    @Test
    void sitemapIsValidXml()  {
        try {
            parseSitemap();
        } catch (Exception e) {
            fail("Sitemap XML is not valid: " + e.getMessage());
        }
    }

    @Test
    void sitemapHasUrlsetRoot() throws IOException, ParserConfigurationException, InterruptedException, SAXException {
        Document doc = parseSitemap();
        Element root = doc.getDocumentElement();
        assertEquals("urlset", root.getLocalName(),
                "Sitemap root element should be <urlset>");
    }

    @Test
    void sitemapContainsUrls() throws IOException, ParserConfigurationException, InterruptedException, SAXException {
        Document doc = parseSitemap();
        NodeList urls = doc.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "url");
        assertTrue(urls.getLength() > 10,
                "Sitemap should contain at least 10 URLs, but only has " + urls.getLength());
    }

    @Test
    void allUrlsHaveLocElements() throws IOException, ParserConfigurationException, InterruptedException, SAXException {
        Document doc = parseSitemap();
        NodeList urls = doc.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "url");

        for (int i = 0; i < urls.getLength(); i++) {
            Element url = (Element) urls.item(i);
            NodeList locs = url.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
            assertTrue(locs.getLength() > 0,
                    "Every <url> element must have a <loc> child (missing at index " + i + ")");
            String loc = locs.item(0).getTextContent().trim();
            assertFalse(loc.isEmpty(),
                    "<loc> must not be empty (at index " + i + ")");
            assertTrue(loc.startsWith("http"),
                    "<loc> should be an absolute URL but was: " + loc);
        }
    }

    @Test
    void sitemapIncludesHomePage() throws IOException, ParserConfigurationException, InterruptedException, SAXException {
        Document doc = parseSitemap();
        NodeList locs = doc.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");

        boolean foundHome = false;
        for (int i = 0; i < locs.getLength(); i++) {
            String loc = locs.item(i).getTextContent().trim();
            if (loc.endsWith("/")) {
                foundHome = true;
                break;
            }
        }
        assertTrue(foundHome, "Sitemap should include the home page URL (ending with /)");
    }
}
