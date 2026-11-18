package io.quarkusio.releases.sync;

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
 * Reads release dates and builds from the Red Hat and IBM Maven repositories. These are shared public servers (IBM's is
 * rate limited), so the clients retry transient failures with exponential backoff and then trip a circuit
 * breaker, which makes every further call for the rest of the run fail fast instead of hammering the server.
 * Callers only ask for the minors that still need a date; listing the builds takes one request per repository, and
 * the Red Hat repository one more for each new build (to date it).
 */
@ApplicationScoped
public class MavenRepositories {

    private static final long PAUSE_BETWEEN_REQUESTS_MS = 1_000;
    private static final String RHBQ_URL = "https://maven.repository.redhat.com/ga/com/redhat/quarkus/platform/quarkus-bom";
    private static final String IBM_URL = "https://public.dhe.ibm.com/ibmdl/export/pub/software/appcafe/maven-repository/io/quarkus/quarkus-bom";

    @RegisterRestClient(configKey = "rhbq-maven", baseUri = RHBQ_URL)
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

    @RegisterRestClient(configKey = "ibm-maven", baseUri = IBM_URL)
    @Retry(maxRetries = 2, delay = 5, delayUnit = ChronoUnit.SECONDS, retryOn = {WebApplicationException.class, ProcessingException.class}, abortOn = NotFoundException.class)
    @ExponentialBackoff
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0, delay = 1, delayUnit = ChronoUnit.HOURS, failOn = {WebApplicationException.class, ProcessingException.class}, skipOn = NotFoundException.class)
    public interface Ibm {
        @GET
        @Path("/")
        String listing();
    }

    @RegisterRestClient(configKey = "maven-central", baseUri = "https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom")
    @Retry(maxRetries = 2, delay = 5, delayUnit = ChronoUnit.SECONDS, retryOn = {WebApplicationException.class, ProcessingException.class}, abortOn = NotFoundException.class)
    @ExponentialBackoff
    @CircuitBreaker(requestVolumeThreshold = 3, failureRatio = 1.0, delay = 1, delayUnit = ChronoUnit.HOURS, failOn = {WebApplicationException.class, ProcessingException.class}, skipOn = NotFoundException.class)
    public interface Central {
        /** The directory listing of every platform BOM version, with its upload date. */
        @GET
        @Path("/")
        String listing();
    }

    @Inject
    @RestClient
    Rhbq rhbq;

    @Inject
    @RestClient
    Central central;

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

    /**
     * Every community platform BOM version on Maven Central, to the date it was published there (one request).
     *
     * @return null if Maven Central can't be read, or lists nothing
     */
    public Map<String, LocalDate> platformBomDates() {
        try {
            Map<String, LocalDate> dates = EnterpriseReleaseDates.parseListing(central.listing());
            if (dates.isEmpty()) {
                Log.warnf("Maven Central lists no platform BOMs");
                return null;
            }
            return dates;
        } catch (RuntimeException e) {
            Log.warnf("Could not read the platform BOMs from Maven Central: %s", e.toString());
            return null;
        }
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

    public static String rhbqBomUrl(String version) {
        return "%1$s/%2$s/quarkus-bom-%2$s.pom".formatted(RHBQ_URL, version);
    }

    public static String ibmBomUrl(String version) {
        return "%1$s/%2$s/quarkus-bom-%2$s.pom".formatted(IBM_URL, version);
    }

    /**
     * The latest build of each version in the Red Hat repository (see {@link Lineage#latestBuilds}), to the date it was
     * published. Only builds not in {@code known} are dated, one request each; a build that can't be dated (the
     * server stopped answering) maps to null, and is dated by a later run.
     *
     * @param known build version to its date, as already in the data
     * @return null if the repository can't be read
     */
    public Map<String, LocalDate> rhbqBuilds(Map<String, LocalDate> known) {
        java.util.List<String> builds;
        try {
            builds = Lineage.latestBuilds(EnterpriseReleaseDates.parseMetadata(rhbq.versions()));
        } catch (RuntimeException e) {
            Log.warnf("Could not list the builds in the Red Hat Maven repository: %s", e.toString());
            return null;
        }
        Map<String, LocalDate> result = new LinkedHashMap<>();
        boolean reachable = true;
        for (String version : builds) {
            LocalDate date = known.get(version);
            if (date == null && reachable) {
                try {
                    Thread.sleep(PAUSE_BETWEEN_REQUESTS_MS);
                    date = rhbq.pom(version).getLastModified().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    reachable = false;
                } catch (RuntimeException e) {
                    Log.warnf("Stopped dating builds in the Red Hat Maven repository: %s", e.toString());
                    reachable = false;
                }
            }
            result.put(version, date);
        }
        return result;
    }

    /**
     * The latest build of each version in the IBM repository (see {@link Lineage#latestBuilds}), to the date it was
     * published (one request, the directory listing has the dates).
     *
     * @return null if the repository can't be read
     */
    public Map<String, LocalDate> ibmBuilds() {
        try {
            Map<String, LocalDate> listed = EnterpriseReleaseDates.parseListing(ibm.listing());
            Map<String, LocalDate> result = new LinkedHashMap<>();
            for (String version : Lineage.latestBuilds(listed.keySet())) {
                result.put(version, listed.get(version));
            }
            return result;
        } catch (RuntimeException e) {
            Log.warnf("Could not list the builds in the IBM Maven repository: %s", e.toString());
            return null;
        }
    }
}
