package io.quarkus.cve.client;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;

/**
 * Attaches a {@code Authorization: Bearer <token>} header to every GitHub
 * Advisory API request when GITHUB_TOKEN is set.
 *
 * Without a token: 60 requests/hour (unauthenticated).
 * With a token:  5,000 requests/hour (authenticated).
 *
 * Set the token in .env (which is git-ignored):
 *   GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx
 *
 * Or export it in your shell before running:
 *   export GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxx
 *   java -jar target/quarkus-app/quarkus-run.jar list
 */
@Provider
public class GitHubAuthFilter implements ClientRequestFilter {

    /** Empty when GITHUB_TOKEN is not set. */
    @Inject
    @ConfigProperty(name = "github.token")
    java.util.Optional<String> token;

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        token.filter(t -> !t.isBlank())
             .ifPresent(t -> ctx.getHeaders().putSingle("Authorization", "Bearer " + t));
    }
}
