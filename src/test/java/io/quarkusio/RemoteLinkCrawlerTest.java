package io.quarkusio;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Crawls an already served site, given by {@code -Dtest.url=...}. Skipped when
 * {@code test.url} is not set, in which case {@link LocalLinkCrawlerTest} crawls the
 * generated site.
 */
@EnabledIfSystemProperty(named = "test.url", matches = ".+")
@Tag("slow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RemoteLinkCrawlerTest {

    LinkCrawler crawler;

    @BeforeAll
    void startCrawler() {
        crawler = new LinkCrawler(System.getProperty("test.url"));
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
