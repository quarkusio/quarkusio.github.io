package io.quarkusio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class RedirectTest extends BrowserTest {

    private static final Pattern META_REFRESH = Pattern.compile(
            "<meta\\s+http-equiv=['\"]refresh['\"]\\s+content=['\"]0;\\s*url=([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE);

    @Override
    @BeforeEach
    void createContext() {
    }

    @Override
    @AfterEach
    void closeContext() {
    }

    private String getMetaRefreshUrl(String path) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String html = response.body();
        Matcher m = META_REFRESH.matcher(html);
        assertTrue(m.find(), "Expected a meta refresh tag on " + path + " but page content was: " + html);
        return m.group(1);
    }

    @Test
    void continuumRedirectsToVersatility() throws Exception {
        String target = getMetaRefreshUrl("/continuum/");
        assertTrue(target.contains("/versatility"),
                "Expected /continuum/ to redirect to /versatility/ but target is: " + target);
    }

    @Test
    void communityRedirectsToGitHubDiscussions() throws Exception {
        String target = getMetaRefreshUrl("/community/");
        assertTrue(target.contains("github.com/quarkusio/quarkus/discussions"),
                "Expected /community/ to redirect to GitHub Discussions but target is: " + target);
    }

    @Test
    void publicationsRedirectsToNewsletter() throws Exception {
        String target = getMetaRefreshUrl("/publications/");
        assertTrue(target.contains("/newsletter"),
                "Expected /publications/ to redirect to /newsletter/ but target is: " + target);
    }

    @Test
    void migrateToQuarkusRedirectsToSpringMigrate() throws Exception {
        String target = getMetaRefreshUrl("/migrate-to-quarkus/");
        assertTrue(target.contains("/spring/migrate"),
                "Expected /migrate-to-quarkus/ to redirect to /spring/migrate but target is: " + target);
    }

    @Test
    void githubRedirectsToQuarkusRepo() throws Exception {
        String target = getMetaRefreshUrl("/github/");
        assertTrue(target.contains("github.com/quarkusio/quarkus"),
                "Expected /github/ to redirect to GitHub repo but target is: " + target);
    }
}
