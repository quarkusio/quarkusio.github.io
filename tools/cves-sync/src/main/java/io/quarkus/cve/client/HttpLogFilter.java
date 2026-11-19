package io.quarkus.cve.client;

import io.quarkus.cve.util.Log;
import io.quarkus.cve.util.Stats;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import java.io.IOException;

/**
 * Counts every REST client request (all clients) for the run statistics, and logs request/response
 * for verbosity levels 4 and 5.
 * Level 4: method, URL, status, elapsed time. Level 5: also headers.
 */
public class HttpLogFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final String START_PROP = "cve-util.http.start";

    @Override
    public void filter(ClientRequestContext req) throws IOException {
        Stats.httpRequest(req.getUri());
        if (!Log.enabled(4)) return;
        req.setProperty(START_PROP, System.nanoTime());
        Log.log(4, "HTTP -> %s %s", req.getMethod(), req.getUri());
        if (Log.enabled(5)) {
            req.getStringHeaders().forEach((k, v) ->
                    Log.log(5, "     req header %s: %s", k, "Authorization".equalsIgnoreCase(k) ? "***" : v));
        }
    }

    @Override
    public void filter(ClientRequestContext req, ClientResponseContext resp) throws IOException {
        if (resp.getStatus() == 304) Stats.notModified(req.getUri());
        if (!Log.enabled(4)) return;
        Object start = req.getProperty(START_PROP);
        long ms = start instanceof Long s ? (System.nanoTime() - s) / 1_000_000 : -1;
        Log.log(4, "HTTP <- %d %s %s (%d ms)", resp.getStatus(), req.getMethod(), req.getUri(), ms);
        if (Log.enabled(5)) {
            resp.getHeaders().forEach((k, v) -> Log.log(5, "     resp header %s: %s", k, v));
        }
    }
}
