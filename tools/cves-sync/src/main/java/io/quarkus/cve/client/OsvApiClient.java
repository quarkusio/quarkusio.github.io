package io.quarkus.cve.client;

import io.quarkus.cve.model.OsvBatchQueryRequest;
import io.quarkus.cve.model.OsvBatchQueryResponse;
import io.quarkus.cve.model.OsvQueryRequest;
import io.quarkus.cve.model.OsvQueryResponse;
import io.quarkus.cve.model.OsvVulnerability;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST Client for the OSV (Open Source Vulnerability) API.
 * Base URL is configured via quarkus.rest-client.osv-api.url in application.properties.
 */
@RegisterRestClient(configKey = "osv-api")
@RegisterProvider(HttpLogFilter.class)
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OsvApiClient {

    /**
     * Fetch a single vulnerability by its OSV ID (e.g. GHSA-… or CVE-…).
     * GET /v1/vulns/{id}
     */
    @GET
    @Path("/vulns/{id}")
    OsvVulnerability getVulnerability(@PathParam("id") String id);

    /**
     * Query vulnerabilities affecting a package (optionally at a given version).
     * POST /v1/query
     */
    @POST
    @Path("/query")
    OsvQueryResponse query(OsvQueryRequest request);

    /**
     * Query vulnerabilities for multiple packages in one round-trip.
     * POST /v1/querybatch
     */
    @POST
    @Path("/querybatch")
    OsvBatchQueryResponse queryBatch(OsvBatchQueryRequest request);
}
