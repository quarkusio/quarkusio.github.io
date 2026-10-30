package io.quarkus.cve.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST client for Maven Central, used to read the Quarkus platform BOM
 * ({@code io.quarkus.platform:quarkus-bom}): the list of platform releases and, per release,
 * the POM whose {@code dependencyManagement} lists every artifact that release manages.
 */
@RegisterRestClient(configKey = "maven-central")
@RegisterProvider(HttpLogFilter.class)
@Produces(MediaType.WILDCARD)
public interface MavenCentralClient {

    /** maven-metadata.xml listing every published platform release. */
    @GET
    @Path("/io/quarkus/platform/quarkus-bom/maven-metadata.xml")
    String bomMetadata();

    /** HTML directory listing of every release folder, with its upload date. */
    @GET
    @Path("/io/quarkus/platform/quarkus-bom/")
    String bomDirectory();

    /** The BOM POM for one platform release. Published releases are immutable. */
    @GET
    @Path("/io/quarkus/platform/quarkus-bom/{version}/quarkus-bom-{version}.pom")
    String bomPom(@PathParam("version") String version);
}
