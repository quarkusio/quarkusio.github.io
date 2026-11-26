package io.quarkus.cve.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for published repository security advisories on {@code quarkusio/quarkus}.
 * Used as a backup source for Quarkus-published advisories that have not yet been reviewed
 * into GitHub's global advisory database or OSV.
 */
@RegisterRestClient(configKey = "github-api")
@RegisterProvider(GitHubAuthFilter.class)
@RegisterProvider(HttpLogFilter.class)
@Path("/repos/quarkusio/quarkus/security-advisories")
@Produces(MediaType.APPLICATION_JSON)
public interface QuarkusRepoAdvisoryClient {

    @GET
    Response listAdvisories(
            @QueryParam("state") String state,
            @QueryParam("per_page") int perPage,
            @QueryParam("after") String after,
            @HeaderParam("If-None-Match") String ifNoneMatch
    );
}
