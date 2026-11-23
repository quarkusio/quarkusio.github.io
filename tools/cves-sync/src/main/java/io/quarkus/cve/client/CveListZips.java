package io.quarkus.cve.client;

import io.quarkus.cve.util.CveDelta;
import io.quarkus.cve.util.Retry;
import io.quarkus.cve.util.Stats;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Downloads the hourly delta zips of the {@code CVEProject/cvelistV5} repository, which are release assets.
 * They are public, so no token is sent (the download redirects to a storage host that has no use for one).
 */
public final class CveListZips implements CveDelta.Source {

    static final String BASE = "https://github.com/CVEProject/cvelistV5/releases/download/";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public Optional<byte[]> zip(String tag, String asset) {
        URI uri = URI.create(BASE + tag + "/" + asset);
        return Retry.call("cvelistV5 " + asset, () -> fetch(uri));
    }

    private Optional<byte[]> fetch(URI uri) {
        Stats.httpRequest(uri);
        try {
            HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "cve-util").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            int status = r.statusCode();
            if (status == 200) return Optional.of(r.body());
            if (status == 404) return Optional.empty();
            Stats.rejected(uri, status == 429 || status == 403);
            Response.ResponseBuilder b = Response.status(status);
            r.headers().firstValue("Retry-After").ifPresent(v -> b.header("Retry-After", v));
            r.headers().firstValue("X-RateLimit-Remaining").ifPresent(v -> b.header("X-RateLimit-Remaining", v));
            throw new WebApplicationException(b.build());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while downloading " + uri, ex);
        }
    }
}
