package io.quarkus.cve.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.cve.util.Stats;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(GitHubAuthFilterTest.Profile.class)
class GitHubAuthFilterTest {

    private static final String TOKEN = "test-gh-token";

    @Inject
    @RestClient
    GitHubAdvisoryClient githubAdvisory;

    @Inject
    @RestClient
    GitHubSearchClient githubSearch;

    @Inject
    @RestClient
    OsvApiClient osv;

    @Inject
    @RestClient
    MavenCentralClient mavenCentral;

    @Inject
    @RestClient
    CveOrgClient cveOrg;

    @Inject
    @RestClient
    QuarkusRegistryClient quarkusRegistry;

    Map<String, Optional<String>> receivedAuth;

    @AfterEach
    void cleanStats() {
        Stats.reset();
    }

    @Test
    void tokenIsSentOnlyToGitHubClients() {
        githubAdvisory.listAdvisories("maven", "io.quarkus:quarkus-core", 1, null, "updated", "desc", null).close();
        githubSearch.search("quarkus", 1, 1);
        osv.getVulnerability("GHSA-test");
        mavenCentral.bomMetadata();
        cveOrg.getRecord("CVE-2026-0001");
        quarkusRegistry.getAllExtensions();

        assertEquals(Optional.of("Bearer " + TOKEN), receivedAuth.get("/advisories"));
        assertEquals(Optional.of("Bearer " + TOKEN), receivedAuth.get("/search/code"));
        assertEquals(Optional.empty(), receivedAuth.get("/v1/vulns/GHSA-test"));
        assertEquals(Optional.empty(), receivedAuth.get("/io/quarkus/platform/quarkus-bom/maven-metadata.xml"));
        assertEquals(Optional.empty(), receivedAuth.get("/api/cve/CVE-2026-0001"));
        assertEquals(Optional.empty(), receivedAuth.get("/client/extensions/all"));
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("github.token", TOKEN);
        }

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(MockUpstream.class));
        }
    }

    public static class MockUpstream implements QuarkusTestResourceLifecycleManager {
        private final Map<String, Optional<String>> receivedAuth = new ConcurrentHashMap<>();
        private HttpServer server;

        @Override
        public Map<String, String> start() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                receivedAuth.put(path, Optional.ofNullable(exchange.getRequestHeaders().getFirst("Authorization")));
                byte[] body = ("/advisories".equals(path) ? "[]" : "{}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            return Map.of(
                    "quarkus.rest-client.github-api.url", url,
                    "quarkus.rest-client.osv-api.url", url,
                    "quarkus.rest-client.maven-central.url", url,
                    "quarkus.rest-client.cve-org.url", url,
                    "quarkus.rest-client.quarkus-registry.url", url);
        }

        @Override
        public void inject(TestInjector testInjector) {
            testInjector.injectIntoFields(receivedAuth, f -> "receivedAuth".equals(f.getName()));
        }

        @Override
        public void stop() {
            if (server != null) {
                server.stop(0);
            }
        }
    }
}

