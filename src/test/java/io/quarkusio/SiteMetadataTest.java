package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SiteMetadataTest extends BrowserTest {

    @Test
    void homepageTitleIsExact() {
        page.navigate(baseUrl + "/");
        assertEquals("Quarkus - Supersonic Subatomic Java", page.title());
    }
}
