package io.quarkusio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class RedirectTest extends BrowserTest {

    private static final Pattern META_REFRESH = Pattern.compile(
            "<meta\\s+http-equiv=['\"]refresh['\"]\\s+content=['\"]0;\\s*url='?([^'\"]+)'?['\"]",
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

    // Renamed guides keep redirects from their old names, because extension metadata
    // in released Quarkus versions and external pages still link to the old names.
    @ParameterizedTest
    @CsvSource({
            "/guides/hibernate-search-elasticsearch/, /guides/hibernate-search-orm-elasticsearch",
            "/guides/kafka-reactive-getting-started/, /guides/kafka-getting-started",
            "/guides/rest-client-multipart/, /guides/resteasy-client-multipart",
            "/guides/rest-client-reactive/, /guides/rest-client",
            "/guides/resteasy-reactive/, /guides/rest",
            "/guides/resteasy-reactive-migration/, /guides/rest-migration",
            "/guides/resteasy-reactive-virtual-threads/, /guides/rest-virtual-threads",
            "/guides/security-architecture-concept/, /guides/security-architecture",
            "/guides/security-authentication-mechanisms-concept/, /guides/security-authentication-mechanisms",
            "/guides/security-authorization-of-web-endpoints-reference/, /guides/security-authorize-web-endpoints-reference",
            "/guides/security-basic-authentication-concept/, /guides/security-basic-authentication",
            "/guides/security-identity-providers-concept/, /guides/security-identity-providers",
            "/guides/security-jpa-concept/, /guides/security-jpa",
            "/guides/security-oidc-bearer-token-authentication-concept/, /guides/security-oidc-bearer-token-authentication",
            "/guides/security-oidc-code-flow-authentication-concept/, /guides/security-oidc-code-flow-authentication",
            "/guides/security-openid-connect/, /guides/security-oidc-bearer-token-authentication-tutorial",
            "/guides/security-openid-connect-web-authentication/, /guides/security-oidc-code-flow-authentication-concept",
            "/guides/security-overview-concept/, /guides/security-overview",
            "/guides/security-proactive-authentication-concept/, /guides/security-proactive-authentication",
            "/guides/security-vulnerability-detection-concept/, /guides/security-vulnerability-detection",
            "/guides/security-webauthn-concept/, /guides/security-webauthn",
            "/guides/update-to-quarkus-3/, /guides/update-quarkus"
    })
    void renamedGuidesRedirectToTheirNewNames(String oldPath, String newPath) throws Exception {
        String target = getMetaRefreshUrl(oldPath);
        assertTrue(target.endsWith(newPath),
                "Expected " + oldPath + " to redirect to " + newPath + " but target is: " + target);
    }
}
