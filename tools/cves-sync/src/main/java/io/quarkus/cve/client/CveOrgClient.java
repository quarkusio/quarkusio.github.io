package io.quarkus.cve.client;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for the CVE Services API at cve.org, the root of CVE data. Only the single-record
 * endpoint is open without credentials.
 */
@RegisterRestClient(configKey = "cve-org")
@RegisterProvider(HttpLogFilter.class)
@Path("/api/cve")
@Produces(MediaType.APPLICATION_JSON)
public interface CveOrgClient {

    /** The CVE JSON 5 record. */
    @GET
    @Path("/{id}")
    JsonNode getRecord(@PathParam("id") String id);
}
