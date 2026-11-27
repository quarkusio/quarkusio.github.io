package io.quarkusio;

import io.quarkus.test.component.QuarkusComponentTestExtension;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Crawls an already served site, given by {@code -Dtest.url=...}. Skipped when
 * {@code test.url} is not set, in which case {@link LocalLinkCrawlerTest} crawls the
 * generated site.
 */
@EnabledIfSystemProperty(named = "test.url", matches = ".+")
@Tag("slow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RemoteLinkCrawlerTest {

    // The site is already running, so only the config mapping is needed from Quarkus. A component
    // test starts just the CDI container; @QuarkusTest would boot the whole site for no reason.
    // The extension is registered programmatically because @QuarkusComponentTest does not offer
    // useSystemConfigSources, and without it the -Dtest.crawl.* flags are silently ignored.
    @RegisterExtension
    static final QuarkusComponentTestExtension EXTENSION = QuarkusComponentTestExtension.builder()
            .useSystemConfigSources(true)
            .build();

    // The extension only creates a mapping for injection points on beans, not on the test class.
    @Singleton
    static class CrawlConfigHolder {
        @Inject
        CrawlConfig config;
    }

    @Inject
    CrawlConfigHolder configHolder;

    LinkCrawler crawler;

    @BeforeAll
    void startCrawler() {
        crawler = new LinkCrawler(System.getProperty("test.url"), configHolder.config);
    }

    @AfterAll
    void stopCrawler() {
        crawler.close();
    }

    @Test
    void crawlAndCheckLinks() throws InterruptedException {
        crawler.crawlAndCheckLinks();
    }

    @Test
    void crawlAndCheckImages() throws InterruptedException {
        crawler.crawlAndCheckImages();
    }

    @Test
    void crawlAndCheckFragmentAnchors() throws InterruptedException {
        crawler.crawlAndCheckFragmentAnchors();
    }
}
