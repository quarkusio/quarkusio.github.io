package io.quarkusio;

import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CvesAllPageTest extends BrowserTest {

    @Test
    void cvesTableHasKeyColumnsAndSubstantialContent() {
        page.navigate(baseUrl + "/cves/all/");
        String headerText = page.locator(".cves-table thead").textContent();
        assertTrue(headerText.contains("ID"));
        assertTrue(headerText.contains("Published"));
        assertTrue(headerText.contains("Severity"));
        assertTrue(headerText.contains("Summary"));

        int rowCount = page.locator(".cves-table tbody tr.cve-row").count();
        assertTrue(rowCount >= 100, "Expected at least 100 CVE rows but found " + rowCount);
    }

    @Test
    void rowsAreSortedNewestFirstByPublishedDate() {
        page.navigate(baseUrl + "/cves/all/");
        Locator rows = page.locator(".cves-table tbody tr.cve-row");
        String firstId = rows.nth(0).getAttribute("id");
        assertNotNull(firstId);
        // Check first 10 rows have non-increasing publication dates (via detail panel date or ISO)
        String prev = null;
        int sample = Math.min(10, rows.count());
        for (int i = 0; i < sample; i++) {
            String detailId = rows.nth(i).getAttribute("data-detail-id");
            Locator dateEl = page.locator("#" + detailId + " .cve-detail-date");
            if (dateEl.count() == 0) continue;
            String iso = dateEl.textContent().replace("Published", "").trim();
            if (prev != null) {
                assertTrue(prev.compareTo(iso) >= 0, "Expected descending date order but " + prev + " < " + iso);
            }
            prev = iso;
        }
    }

    @Test
    void clickingRowExpandsAndCollapsesDetails() {
        page.navigate(baseUrl + "/cves/all/");
        Locator firstRow = page.locator(".cves-table tbody tr.cve-row:not(.cve-hidden)").first();
        String detailId = firstRow.getAttribute("data-detail-id");
        Locator detail = page.locator("#" + detailId);

        assertFalse(detail.isVisible(), "Detail row should be collapsed by default");
        firstRow.click();
        assertTrue(detail.isVisible(), "Detail row should expand on click");
        assertEquals("true", firstRow.locator(".cve-expand-btn").getAttribute("aria-expanded"));

        firstRow.click();
        assertFalse(detail.isVisible(), "Detail row should collapse on second click");
    }

    @Test
    void pendingCvesAreHiddenByDefaultAndCanBeShown() {
        page.navigate(baseUrl + "/cves/all/");
        Locator hidePending = page.locator("#cve-hide-pending");
        assertTrue(hidePending.isChecked(), "Hide pending should be checked by default");

        int totalPending = page.locator(".cves-table tbody tr.cve-row.cve-pending").count();
        int visiblePending = page.locator(".cves-table tbody tr.cve-row.cve-pending:not(.cve-hidden)").count();
        assertEquals(0, visiblePending, "Pending rows should be hidden by default");

        hidePending.uncheck();
        int shownPending = page.locator(".cves-table tbody tr.cve-row.cve-pending:not(.cve-hidden)").count();
        assertEquals(totalPending, shownPending, "Unchecking hide pending should reveal all pending rows");
    }

    @Test
    void softLinkByCveAndGhsaHashExpandsAndHighlightsRow() {
        // Test case-insensitive CVE hash
        page.navigate(baseUrl + "/cves/all/#CVE-2024-1597");
        Locator row = page.locator("tr.cve-row#cve-2024-1597");
        assertEquals(1, row.count());
        assertTrue(row.evaluate("el => el.classList.contains('is-highlighted')").equals(true),
                "Target row should be highlighted when linked by CVE hash");
        assertTrue(page.locator("#details-cve-2024-1597").isVisible(),
                "Target detail row should auto-expand when linked by CVE hash");

        // Test soft link by GHSA hash for the same entry
        page.navigate(baseUrl + "/cves/all/#ghsa-24rp-q3w6-vc56");
        assertTrue(row.evaluate("el => el.classList.contains('is-highlighted')").equals(true),
                "Target row should be highlighted when linked by GHSA hash");
        assertTrue(page.locator("#details-cve-2024-1597").isVisible(),
                "Target detail row should auto-expand when linked by GHSA hash");
    }

    @Test
    void quarkusOnlyFilterReducesVisibleRows() {
        page.navigate(baseUrl + "/cves/all/");
        int allCount = page.locator(".cves-table tbody tr.cve-row:not(.cve-hidden)").count();
        page.locator("#cve-core-only").check();
        int coreCount = page.locator(".cves-table tbody tr.cve-row:not(.cve-hidden)").count();
        assertTrue(coreCount > 0 && coreCount < allCount,
                "Expected Quarkus-only filter to narrow rows (" + coreCount + " vs " + allCount + ")");
    }

    @Test
    void showImpactOnBranchMapSummonsModalWithHighlightedNodes() {
        page.navigate(baseUrl + "/cves/all/#CVE-2024-1597");
        Locator btn = page.locator("#details-cve-2024-1597 .cve-show-tree-btn");
        assertTrue(btn.isVisible(), "Show impact on branch map button should be visible in expanded CVE details");

        btn.click();
        Locator dialog = page.locator("#cve-tree-dialog");
        assertTrue(dialog.isVisible(), "Clicking Show impact on branch map should open the tree modal");
        Locator banner = page.locator("#cve-modal-banner");
        assertTrue(banner.isVisible(), "Modal should show the selected advisory banner");
        assertTrue(banner.textContent().contains("CVE-2024-1597"));

        int stations = page.locator("#cve-modal-svg .tube-station").count();
        int highlightedEdges = page.locator("#cve-modal-svg .tube-edge--affected, #cve-modal-svg .tube-edge--affected-pre").count();
        assertTrue(stations >= 400, "Modal should render full single-root tree, found " + stations);
        assertTrue(highlightedEdges >= 1, "Modal should highlight affected track edges");
        assertEquals(1, page.locator("#cve-modal-svg .tube-disclosure-marker").count(),
                "Modal should display the CVE disclosure marker line");

        page.locator("#cve-tree-dialog-close").click();
        assertFalse(dialog.isVisible(), "Clicking close should dismiss the tree modal");
    }

    @Test
    void pageHasNoHorizontalOverflowOnMobile() {
        page.setViewportSize(400, 800);
        page.navigate(baseUrl + "/cves/all/");
        Object overflow = page.evaluate("() => document.documentElement.scrollWidth > document.documentElement.clientWidth");
        assertEquals(false, overflow);
    }
}
