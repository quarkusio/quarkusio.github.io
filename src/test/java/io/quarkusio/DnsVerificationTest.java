package io.quarkusio;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DnsVerificationTest extends BrowserTest {

    @Override
    @BeforeEach
    void createContext() {
    }

    @Override
    @AfterEach
    void closeContext() {
    }

    @Test
    void atprotoDidReturns200() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/.well-known/atproto-did"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(),
                    "Expected 200 for /.well-known/atproto-did but got " + response.statusCode());
        }
    }

    @Test
    void atprotoDidContainsValidDid() throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/.well-known/atproto-did"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            assertFalse(body.isEmpty(), "atproto-did should not be empty");
            assertTrue(body.startsWith("did:"), "atproto-did should start with 'did:'");
        }
    }
}
