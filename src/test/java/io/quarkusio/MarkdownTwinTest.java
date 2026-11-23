package io.quarkusio;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The markdown-twin plugin publishes a Markdown copy of each page for AI agents, and the head of the page links to it.
 */
@QuarkusTest
public class MarkdownTwinTest extends BrowserTest {

    @Override
    @BeforeEach
    void createContext() {
    }

    @Override
    @AfterEach
    void closeContext() {
    }

    private HttpResponse<String> fetch(String path) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void redirectPageHasNoTwin() throws IOException, InterruptedException {
        assertEquals(200, fetch("/awards/").statusCode());
        assertEquals(404, fetch("/awards/index.md").statusCode());
    }

    @Test
    void notFoundPageHasNoTwin() throws IOException, InterruptedException {
        HttpResponse<String> page = fetch("/404.html");
        assertEquals(200, page.statusCode());
        assertFalse(page.body().contains("type=\"text/markdown\""),
                "The 404 page should not link to a twin");
        assertEquals(404, fetch("/404.html.md").statusCode());
    }

    @Test
    void guideHasMarkdownTwin() throws IOException, InterruptedException {
        HttpResponse<String> twin = fetch("/guides/cdi/index.md");
        assertEquals(200, twin.statusCode());
        assertTrue(twin.body().startsWith("# Introduction to Contexts and Dependency Injection (CDI)"),
                "The twin should start with the guide title as its heading");
        assertFalse(twin.body().contains("<div"), "The twin should be Markdown, not the rendered page");
    }

    @Test
    void guidePageLinksToItsTwin() throws IOException, InterruptedException {
        HttpResponse<String> page = fetch("/guides/cdi/");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains(
                "<link rel=\"alternate\" type=\"text/markdown\" href=\"https://quarkus.io/guides/cdi/index.md\" />"),
                "The guide page should link to its Markdown twin");
    }

    @Test
    void versionedGuideHasNoTwin() throws IOException, InterruptedException {
        HttpResponse<String> page = fetch("/version/main/guides/cdi/");
        assertEquals(200, page.statusCode());
        assertFalse(page.body().contains("type=\"text/markdown\""),
                "A versioned guide under /version/ should not link to a twin");
        assertEquals(404, fetch("/version/main/guides/cdi/index.md").statusCode());
    }
}
