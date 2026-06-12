package io.quarkusio;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
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

    @Test
    void feedReturns200() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/feed.xml"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(),
                "Expected 200 for /feed.xml but got " + response.statusCode());
    }

    @Test
    void feedIsValidXml() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/feed.xml"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        assertFalse(body.isEmpty(), "Feed response body should not be empty");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
        } catch (Exception e) {
            fail("Feed XML is not valid: " + e.getMessage());
        }
    }

    @Test
    void feedContainsEnoughPosts() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/feed.xml"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));

        int itemCount = doc.getElementsByTagName("item").getLength();
        int entryCount = doc.getElementsByTagName("entry").getLength();
        int feedCount = Math.max(itemCount, entryCount);

        assertTrue(feedCount >= 400,
                "Feed should contain at least 400 posts (issue #2421), but only has " + feedCount);
    }
}
