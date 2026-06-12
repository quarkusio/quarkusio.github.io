package io.quarkusio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.ColorScheme;

public class ColorModeTest extends BrowserTest {

    @Override
    @BeforeEach
    void createContext() {
        // Pin to light so the "system" theme resolves deterministically
        // regardless of the OS dark-mode setting on the CI or dev machine.
        context = browser.newContext(
                new Browser.NewContextOptions().setColorScheme(ColorScheme.LIGHT));
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
    void themeToggleCyclesThemes() {
        page.navigate(baseUrl + "/");
        Locator toggle = page.locator("#theme-toggle");
        toggle.waitFor();

        // Initial state: no localStorage, defaults to "system"
        assertEquals("system", toggle.getAttribute("aria-label"));

        // Click once: system -> light
        toggle.click();
        assertEquals("light", toggle.getAttribute("aria-label"));
        assertFalse((Boolean) page.evaluate(
                "() => document.documentElement.classList.contains('dark')"),
                "Expected no 'dark' class on <html> in light mode");

        // Click again: light -> dark
        toggle.click();
        assertEquals("dark", toggle.getAttribute("aria-label"));
        assertTrue((Boolean) page.evaluate(
                "() => document.documentElement.classList.contains('dark')"),
                "Expected 'dark' class on <html> in dark mode");

        // Click again: dark -> system
        toggle.click();
        assertEquals("system", toggle.getAttribute("aria-label"));
    }

    @Test
    void themeSettingPersistsAcrossReload() {
        page.navigate(baseUrl + "/");
        Locator toggle = page.locator("#theme-toggle");
        toggle.waitFor();

        // Click twice to reach dark mode (system -> light -> dark)
        toggle.click();
        toggle.click();
        assertEquals("dark", toggle.getAttribute("aria-label"));
        assertTrue((Boolean) page.evaluate(
                "() => document.documentElement.classList.contains('dark')"),
                "Expected 'dark' class on <html> in dark mode");

        page.reload();
        toggle = page.locator("#theme-toggle");
        toggle.waitFor();

        assertTrue((Boolean) page.evaluate(
                "() => document.documentElement.classList.contains('dark')"),
                "Expected 'dark' class to persist after reload");
        assertEquals("dark", toggle.getAttribute("aria-label"));
    }
}
