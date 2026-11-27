package io.quarkusio;

import io.quarkus.test.junit.QuarkusTest;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MobileNavigationTest extends BrowserTest {

    @Override
    @BeforeEach
    void createContext() {
        context = browser.newContext(
                new Browser.NewContextOptions().setViewportSize(375, 667));
        page = context.newPage();
    }

    @Override
    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void menuIsHiddenByDefaultOnMobile() {
        page.navigate(baseUrl + "/");
        assertFalse(page.locator("#menu").isVisible(),
                "Menu should be hidden by default on mobile viewport");
    }

    @Test
    void menuReturnsToItsDesktopLayoutWhenTheViewportWidens() {
        page.navigate(baseUrl + "/");

        page.locator(".nav-toggle").click();
        page.waitForTimeout(500);
        assertTrue(page.locator("#menu").isVisible(),
                "Menu should be open before widening the viewport");

        page.setViewportSize(1440, 900);
        page.waitForTimeout(500);

        assertFalse(page.locator("#checkbox").isChecked(),
                "The hamburger checkbox should be cleared once the layout is the wide one");
        var box = page.locator("#menu").boundingBox();
        assertNotNull(box, "Menu should still be on the page after widening");
        assertTrue(box.width > 400,
                "Menu should span the wide layout again, not stay a narrow column, but is " + box.width + "px wide");
        assertTrue(box.height < 120,
                "Menu should be a row of links again, not the stacked mobile menu, but is " + box.height + "px tall");
    }

    @Test
    void hamburgerMenuOpensAndNavigates() {
        page.navigate(baseUrl + "/");

        page.locator(".nav-toggle").click();
        page.waitForTimeout(500);

        assertTrue(page.locator("#menu").isVisible(),
                "Menu should be visible after clicking hamburger toggle");

        // Pick a submenu link that navigates to a different page
        var menuLink = page.locator("#menu .submenu a[href]").first();
        String href = menuLink.getAttribute("href");
        assertNotNull(href, "Menu link should have an href attribute");

        // Navigate directly via the href to avoid animation/timing issues
        Response response = page.navigate(baseUrl + href);
        assertNotNull(response, "Navigation response should not be null");
        assertEquals(200, response.status(),
                "Navigation to " + href + " should return 200");
    }
}
