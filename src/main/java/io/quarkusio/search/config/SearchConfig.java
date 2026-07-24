package io.quarkusio.search.config;

import io.quarkus.tools.migration.search.ScriptMode;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkusio.search")
public interface SearchConfig {

    /** The URL of the remote search service */
    @WithDefault("https://search.quarkus.io")
    String host();

    /** The path to the search script (relative from the search.host) */
    @WithDefault("/static/bundle/app.js")
    String scriptPath();

    /** The script mode is direct or cached. Cached means the search script is copied from the remote service. */
    @WithDefault("cached")
    ScriptMode scriptMode();

    /** Where to copy the search script file for cached mode */
    @WithDefault("assets/javascript/search-wc.js")
    String cachedScriptFile();

    /**
     * The amount of time before we give up on a pending remote search and fall back to Javascript search.
     * The search service itself is reasonably fast on a decent machine (with curl: ~100ms median, ~150ms 90th percentile).
     * but it's slower on prod machines (with curl: ~200ms median, ~400ms 90th percentile),
     * and prod network overhead makes it even worse (with curl from France: ~750ms median, ~1000ms 90th percentile),
     * so we need a high timeout with some margin for varying network latency depending on browser location.
     * Such a high timeout is acceptable since Javascript search is almost instantaneous,
     * so when we hit the timeout the user won't experience much more delay.
     * See also https://docs.google.com/spreadsheets/d/1w0tSfL-MKFArSrB-L8IX1pxmrhWmQyNgNwQRloG3cLk/edit#gid=456110917
     */
    @WithDefault("1500")
    int initialTimeout();

    /**
     * When fetching more pages, a timeout would be very bad for UX
     * since we would reset the whole page using Javascript --
     * while the user is reading through it!
     * This we set a higher timeout value, to make timeouts less likely.
     */
    @WithDefault("2500")
    int moreTimeout();

    /**
     * The minimum number of characters before we run a full search.
     * Below this:
     * - if another filter is selected (e.g. categories), we run Javascript search
     * - otherwise, we don't run search and just display all guides
     */
    @WithDefault("2")
    int minChars();
}
