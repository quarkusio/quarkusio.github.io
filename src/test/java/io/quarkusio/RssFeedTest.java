package io.quarkusio;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

public class RssFeedTest extends BrowserTest {

    @Override
    @BeforeEach
    void createContext() {
    }

    @Override
    @AfterEach
    void closeContext() {
    }

    private String fetchFeed() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/feed.xml"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(),
                "Expected 200 for /feed.xml but got " + response.statusCode());
        String body = response.body();
        assertFalse(body.isEmpty(), "Feed response body should not be empty");
        return body;
    }

    private Document parseFeed(String body) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute("jdk.xml.maxGeneralEntitySizeLimit", 0);
        factory.setAttribute("jdk.xml.totalEntitySizeLimit", 0);
        return factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
    }

    @Test
    void feedIsValidXml() throws Exception {
        String body = fetchFeed();
        try {
            parseFeed(body);
        } catch (Exception e) {
            fail("Feed XML is not valid: " + e.getMessage());
        }
    }

    @Test
    void feedContainsEnoughPosts() throws Exception {
        Document doc = parseFeed(fetchFeed());

        int itemCount = doc.getElementsByTagName("item").getLength();
        int entryCount = doc.getElementsByTagName("entry").getLength();
        int feedCount = Math.max(itemCount, entryCount);

        assertTrue(feedCount >= 400,
                "Feed should contain at least 400 posts (issue #2421), but only has " + feedCount);
    }

    @Test
    void feedDescriptionsAreEscaped() throws Exception {
        Document doc = parseFeed(fetchFeed());
        NodeList descriptions = doc.getElementsByTagName("description");

        assertTrue(descriptions.getLength() > 0, "Feed should have description elements");

        for (int i = 0; i < descriptions.getLength(); i++) {
            Element desc = (Element) descriptions.item(i);
            NodeList children = desc.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                assertNotEquals(Node.ELEMENT_NODE, child.getNodeType(),
                        "Description #" + i + " contains unescaped HTML element <" + child.getNodeName()
                                + ">. HTML in descriptions must be entity-escaped.");
            }
        }
    }

    @Test
    void feedTitlesDoNotContainHtmlTags() throws Exception {
        Document doc = parseFeed(fetchFeed());
        NodeList titles = doc.getElementsByTagName("title");

        assertTrue(titles.getLength() > 0, "Feed should have title elements");

        for (int i = 0; i < titles.getLength(); i++) {
            String title = titles.item(i).getTextContent();
            String rawHtml = UnrenderedMarkupDetector.findRawHtmlTag(title);
            assertNull(rawHtml, "Title #" + i + " \"" + title + "\" contains raw HTML: " + rawHtml);
        }
    }

    @Test
    void feedTitlesDoNotContainUnrenderedMarkup() throws Exception {
        Document doc = parseFeed(fetchFeed());
        NodeList titles = doc.getElementsByTagName("title");

        for (int i = 0; i < titles.getLength(); i++) {
            String title = titles.item(i).getTextContent();
            List<String> findings = UnrenderedMarkupDetector.findUnrenderedMarkup(title);
            assertTrue(findings.isEmpty(),
                    "Title #" + i + " \"" + title + "\" contains unrendered markup: "
                            + String.join("; ", findings));
        }
    }
}
