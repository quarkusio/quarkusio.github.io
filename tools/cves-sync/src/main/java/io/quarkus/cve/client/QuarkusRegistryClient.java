package io.quarkus.cve.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

/**
 * REST client for the Quarkus Extension Registry — used to discover
 * all first-party io.quarkus:* artifact coordinates dynamically.
 */
@RegisterRestClient(configKey = "quarkus-registry")
@RegisterProvider(HttpLogFilter.class)
@Path("/client")
@Produces(MediaType.APPLICATION_JSON)
public interface QuarkusRegistryClient {

    @GET
    @Path("/extensions/all")
    ExtensionCatalog getAllExtensions();

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ExtensionCatalog {
        @JsonProperty("extensions")
        public List<ExtensionEntry> extensions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ExtensionEntry {
        /** Maven GAV string: groupId:artifactId:version */
        @JsonProperty("artifact")
        public String artifact;
    }
}
