package io.quarkusio;

import io.quarkiverse.roq.testing.RoqAndRoll;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.startsWith;

/**
 * Checks the files the generator writes and how a static server serves them. The pages
 * themselves are rendered and inspected in {@link CorePageSenseTest}.
 * <p>
 * This should be the only {@code @RoqAndRoll} class in the normal test run: each one generates the whole site
 * again in the same JVM, and any more than one OOMs.
 */
@QuarkusTest
@RoqAndRoll
@Tag("fullsite")
public class SmokePageTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/blog/",
            "/guides/",
            "/get-started/",
            "/releases/"
    })
    void criticalPageIsGenerated(String path) {
        get(path).then().statusCode(200).contentType(containsString("text/html"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/feed.xml",
            "/sitemap.xml",
            "/llms.txt",
            "/robots.txt",
            "/favicon.ico"
    })
    void nonPageFileIsGenerated(String path) {
        get(path).then().statusCode(200).body(not(emptyString()));
    }

    // The Bluesky domain verification file, copied from public/
    @Test
    void atprotoDidIsGenerated() {
        get("/.well-known/atproto-did").then().statusCode(200).body(startsWith("did:"));
    }

    @Test
    void notFoundPageIsGenerated() {
        get("/404.html").then().statusCode(200).body(containsString("You have found nowhere"));
    }

    @Test
    void unknownPathReturns404() {
        get("/this-path-does-not-exist-xyz/").then().statusCode(404);
    }
}
