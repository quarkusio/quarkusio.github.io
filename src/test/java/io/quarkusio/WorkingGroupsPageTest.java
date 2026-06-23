package io.quarkusio;

import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainRawHtml;
import static io.quarkusio.UnrenderedMarkupDetector.assertDoesNotContainUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

public class WorkingGroupsPageTest extends BrowserTest {

    @Test
    void workingGroupsPageListsAtLeastFive() {
        page.navigate(baseUrl + "/working-groups/");
        int count = page.locator(".card .card-title").count();
        assertTrue(count >= 5,
                "Expected at least 5 working groups but found " + count);
    }

    @Test
    void workingGroupsPageDoesNotContainUnrenderedMarkup() {
        page.navigate(baseUrl + "/working-groups/");
        assertDoesNotContainUnrenderedMarkup(page, "Working groups page");
    }

    @Test
    void workingGroupsPageDoesNotContainRawHtmlTags() {
        page.navigate(baseUrl + "/working-groups/");
        assertDoesNotContainRawHtml(page, "Working groups page");
    }
}
