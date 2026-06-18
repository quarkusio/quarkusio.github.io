package io.quarkusio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorkingGroupsPageTest extends BrowserTest {

    @Test
    void workingGroupsPageListsAtLeastFive() {
        page.navigate(baseUrl + "/working-groups/");
        int count = page.locator(".card .card-title").count();
        assertTrue(count >= 5,
                "Expected at least 5 working groups but found " + count);
    }
}
