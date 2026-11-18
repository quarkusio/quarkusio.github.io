package io.quarkusio;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class SiteMetadataTest extends BrowserTest {

    @Test
    void homepageTitleIsExact() {
        page.navigate(baseUrl + "/");
        assertEquals("Quarkus - Supersonic Subatomic Java", page.title());
    }
}
