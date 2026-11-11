import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.logging.Log;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Reads release dates from the Red Hat and IBM Maven repositories. These are shared public servers (IBM's is
 * rate limited), so the clients retry transient failures with exponential backoff and then trip a circuit
 * breaker, which makes every further call for the rest of the run fail fast instead of hammering the server.
 * Callers only ask for the minors that still need a date, so a normal run makes no requests at all.
 */
@ApplicationScoped
public class MavenRepositories {

    private static final long PAUSE_BETWEEN_REQUESTS_MS = 1_000;

    @RegisterRestClient(configKey = "rhbq-maven", baseUri = "https://maven.repository.redhat.com/ga/com/redhat/quarkus/platform/quarkus-bom")
    @Retry(maxRetries = 2, delay = 5, delayUnit = ChronoUnit.SECONDS, retryOn = {WebApplicationException.class, ProcessingException.class}, abortOn = NotFoundException.class)
    @ExponentialBackoff
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0, delay = 1, delayUnit = ChronoUnit.HOURS, failOn = {WebApplicationException.class, ProcessingException.class}, skipOn = NotFoundException.class)
    public interface Rhbq {
        @GET
        @Path("maven-metadata.xml")
        String versions();

        @HEAD
        @Path("{version}/quarkus-bom-{version}.pom")
        Response pom(@PathParam("version") String version);
    }

    @RegisterRestClient(configKey = "ibm-maven", baseUri = "https://public.dhe.ibm.com/ibmdl/export/pub/software/appcafe/maven-repository/io/quarkus/quarkus-bom")
    @Retry(maxRetries = 2, delay = 5, delayUnit = ChronoUnit.SECONDS, retryOn = {WebApplicationException.class, ProcessingException.class}, abortOn = NotFoundException.class)
    @ExponentialBackoff
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0, delay = 1, delayUnit = ChronoUnit.HOURS, failOn = {WebApplicationException.class, ProcessingException.class}, skipOn = NotFoundException.class)
    public interface Ibm {
        @GET
        @Path("/")
        String listing();
    }

    @Inject
    @RestClient
    Rhbq rhbq;

    @Inject
    @RestClient
    Ibm ibm;

    /** Minor to the first date a build of it was published to the Red Hat repository, for the wanted minors only. */
    public Map<String, LocalDate> rhbqDates(Set<String> wanted) {
        Map<String, LocalDate> result = new LinkedHashMap<>();
        if (wanted.isEmpty()) {
            return result;
        }
        try {
            var earliest = EnterpriseReleaseDates.earliestPerMinor(EnterpriseReleaseDates.parseMetadata(rhbq.versions()));
            for (var minor : earliest.entrySet()) {
                if (!wanted.contains(minor.getKey())) {
                    continue;
                }
                for (String version : minor.getValue()) {
                    Thread.sleep(PAUSE_BETWEEN_REQUESTS_MS);
                    LocalDate date = rhbq.pom(version).getLastModified().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                    result.merge(minor.getKey(), date, (a, b) -> a.isBefore(b) ? a : b);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            Log.warnf("Stopped reading the Red Hat Maven repository: %s", e.toString());
        }
        return result;
    }

    /** Minor to the first date a build of it was published to the IBM repository, for the wanted minors only. */
    public Map<String, LocalDate> ibmDates(Set<String> wanted) {
        if (wanted.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, LocalDate> result = new LinkedHashMap<>(
                    EnterpriseReleaseDates.earliestDates(EnterpriseReleaseDates.parseListing(ibm.listing())));
            result.keySet().retainAll(wanted);
            return result;
        } catch (RuntimeException e) {
            Log.warnf("Stopped reading the IBM Maven repository: %s", e.toString());
            return Map.of();
        }
    }
}
