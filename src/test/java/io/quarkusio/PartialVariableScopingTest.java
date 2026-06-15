package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PartialVariableScopingTest extends BrowserTest {

    @Test
    void contentSecurityPolicyHasDirectives() {
        page.navigate(baseUrl + "/");
        String csp = page.locator("meta[http-equiv='Content-Security-Policy']").getAttribute("content");
        assertNotNull(csp, "Expected a Content-Security-Policy meta tag");
        assertTrue(csp.contains("script-src"),
                "CSP should contain a script-src directive but was: " + csp);
    }

    @Test
    void contentSecurityPolicyIsNotEmpty() {
        page.navigate(baseUrl + "/");
        String csp = page.locator("meta[http-equiv='Content-Security-Policy']").getAttribute("content");
        assertNotNull(csp, "Expected a Content-Security-Policy meta tag");
        assertTrue(csp.trim().length() > 20,
                "CSP meta tag appears to be empty or truncated: " + csp);
    }
}
