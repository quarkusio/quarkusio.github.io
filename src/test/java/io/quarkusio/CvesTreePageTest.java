package io.quarkusio;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CvesTreePageTest extends BrowserTest {

    @Test
    void treeMapRendersFullSingleRootTree() {
        page.navigate(baseUrl + "/cves/tree/");
        int stations = page.locator("#cve-tree-svg .tube-station").count();
        int edges = page.locator("#cve-tree-svg .tube-edge").count();
        int pills = page.locator("#cve-tree-svg .tube-branch-pill").count();

        assertTrue(stations >= 400, "Expected full release tree (>= 400 stations) but found " + stations);
        assertTrue(edges >= 400, "Expected at least 400 branch edges but found " + edges);
        assertTrue(pills >= 10, "Expected branch pills but found " + pills);
    }

    @Test
    void fitAllZoomsOutToEntireTree() {
        page.navigate(baseUrl + "/cves/tree/");
        page.locator("#cve-tree-fit-all").click();
        String zoomText = page.locator("#cve-tree-zoom-reset").textContent().trim();
        assertTrue(zoomText.endsWith("%"), "Expected zoom percentage label, got: " + zoomText);
        int pct = Integer.parseInt(zoomText.replace("%", ""));
        assertTrue(pct >= 8 && pct < 100, "Fit-all should zoom out below 100%, got " + pct + "%");
    }

    @Test
    void clickingStationOpensInspectorWithAdvisories() {
        page.navigate(baseUrl + "/cves/tree/");
        Locator inspector = page.locator("#cve-tree-inspector");
        assertFalse(inspector.isVisible());

        page.locator("#cve-tree-svg .tube-station").first().click();
        assertTrue(inspector.isVisible(), "Clicking a station should open the station inspector");
        assertTrue(inspector.textContent().contains("Quarkus "));
    }

    @Test
    void hashDeepLinkTracesCveOnBranchMap() {
        page.navigate(baseUrl + "/cves/tree/#CVE-2024-1597");
        Locator banner = page.locator("#cve-tree-banner");
        assertTrue(banner.isVisible(), "Tracing a CVE by hash should show the advisory banner");
        assertTrue(banner.textContent().contains("CVE-2024-1597"));
        assertTrue(page.locator("#cve-tree-fit-affected").isVisible(),
                "Fit affected button should appear when tracing an advisory");

        int affected = page.locator("#cve-tree-svg .tube-station--affected, #cve-tree-svg .tube-station--root").count();
        int highlightedEdges = page.locator("#cve-tree-svg .tube-edge--affected, #cve-tree-svg .tube-edge--affected-pre").count();
        assertTrue(affected >= 1, "Expected affected stations to be highlighted on the map");
        assertTrue(highlightedEdges >= 1, "Expected affected track edges to be highlighted on the map");
        assertEquals(1, page.locator("#cve-tree-svg .tube-disclosure-marker").count(),
                "Expected CVE published disclosure marker line on the branch map");
    }

    @Test
    void pageHasNoHorizontalPageOverflowOnMobile() {
        page.setViewportSize(400, 800);
        page.navigate(baseUrl + "/cves/tree/");
        Object overflow = page.evaluate("() => document.documentElement.scrollWidth > document.documentElement.clientWidth");
        assertEquals(false, overflow);
    }
}
