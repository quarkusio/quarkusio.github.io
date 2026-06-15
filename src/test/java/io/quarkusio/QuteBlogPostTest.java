package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuteBlogPostTest extends BrowserTest {

    @Test
    void qutePostRendersCodeBlocks() {
        page.navigate(baseUrl + "/blog/qute/");
        int codeCount = page.locator("pre, code").count();
        assertTrue(codeCount >= 5,
                "Expected at least 5 code blocks in Qute blog post but found " + codeCount);
    }

    @Test
    void qutePostDoesNotShowRawTemplateErrors() {
        page.navigate(baseUrl + "/blog/qute/");
        String body = page.locator("body").innerText();
        assertFalse(body.contains("Liquid error"),
                "Page contains Liquid/template error output");
        assertFalse(body.contains("Qute error"),
                "Page contains Qute error output");
    }
}
