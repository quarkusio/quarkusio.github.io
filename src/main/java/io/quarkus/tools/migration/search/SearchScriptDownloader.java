package io.quarkus.tools.migration.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import io.quarkiverse.roq.generator.runtime.RoqSelection;
import io.quarkiverse.roq.generator.runtime.SelectedPath;
import io.quarkus.runtime.Startup;
import io.quarkusio.search.config.SearchConfig;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

@ApplicationScoped
@Startup
public class SearchScriptDownloader {

    private static final Logger LOG = Logger.getLogger(SearchScriptDownloader.class);

    // path is the URL the SSG generator fetches via HTTP from the running server
    //  (the Vert.x route). outputPath is where it writes the response in target/roq/.
    //  They're similar because the route is mounted at the same logical location the
    //  file should end up — one has a leading / (URL), the other doesn't (relative
    //  file path)
    private static final String SCRIPT_PATH = "/assets/javascript/search-wc.js";
    public static final String OUTPUT_PATH = "assets/javascript/search-wc.js";

    @Inject
    @Named("searchConfig")
    SearchConfig searchConfig;

    private volatile String scriptContent;

    @PostConstruct
    void downloadScript() {
        if (searchConfig.scriptMode() != ScriptMode.DIRECT) {
            String scriptUrl = searchConfig.host() + searchConfig.scriptPath();
            try (InputStream in = URI.create(scriptUrl).toURL().openStream()) {
                scriptContent = new String(in.readAllBytes())
                        .replaceAll("//# sourceMappingURL=.*\\.map", "");
                LOG.infof("Downloaded search script from %s (%d bytes)", scriptUrl, scriptContent.length());
            } catch (IOException e) {
                LOG.errorf(e, "Failed to download search script from %s", scriptUrl);
            }
        }
    }

    void registerRoute(@Observes Router router) {
        if (scriptContent != null) {
            router.route(SCRIPT_PATH).method(HttpMethod.GET).handler(rc -> rc.response()
                            .putHeader("Content-Type", "application/javascript")
                            .end(scriptContent));
        }
    }

    @Produces
    @Singleton
    RoqSelection searchScriptSelection() {
        if (searchConfig.scriptMode() != ScriptMode.DIRECT && scriptContent != null) {
            return new RoqSelection(List.of(
                    SelectedPath.builder()
                            .path(SCRIPT_PATH)
                            .outputPath(OUTPUT_PATH)
                            .build()));
        }
        return new RoqSelection(List.of());
    }
}

