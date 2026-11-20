package io.quarkusio;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Settings for {@link LinkCrawlerTest}. The property names are part of the
 * contract with the CI workflows in this repo and in other repos, which pass
 * them as {@code -Dtest.crawl.*} flags, so they must not change.
 * <p>
 * List-valued properties are comma separated. They are optional rather than
 * defaulted because an empty string is treated as "not set" by the config
 * system; an absent value means an empty list.
 */
@ConfigMapping(prefix = "test.crawl")
public interface CrawlConfig {

    /** Stop after this many internal pages. */
    @WithDefault("2147483647")
    int maxPages();

    @WithDefault("16")
    int threads();

    @WithDefault("true")
    boolean checkInternal();

    @WithDefault("false")
    boolean checkExternal();

    /** Where to write the machine-readable results; nothing is written if unset. */
    Optional<File> resultsFile();

    /** Path prefixes which are not crawled. */
    Optional<List<String>> excludePaths();

    /** Substrings of external URLs which are not checked. */
    Optional<List<String>> excludeUrls();

    /** If set, only these pages are crawled (incremental mode). */
    Optional<List<String>> changedPaths();

    /** Paths to start crawling from, instead of the site root. */
    Optional<List<String>> startPaths();

    /** Path prefixes to include; everything is included if unset. */
    Optional<List<String>> includePaths();

    /** CSS selectors whose content is ignored when collecting links. */
    Optional<List<String>> excludeSelectors();

    /** Broken links in newsletters and blog posts older than this are informational only. */
    Optional<Integer> datedCutoffMonths();
}
