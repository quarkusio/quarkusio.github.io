package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReleasesPageTest extends BrowserTest {

    @Test
    void releasesTableHasKeyColumns() {
        page.navigate(baseUrl + "/releases/");
        String headerText = page.locator(".releases-table thead").textContent();
        Locator headers = page.locator(".releases-table thead th");
        assertTrue(headers.count() >= 4,
                "Expected at least 4 table columns but found " + headers.count());
        assertTrue(headerText.contains("Release"), "Table should have a Release column");
        assertTrue(headerText.contains("Support"), "Table should have a support-related column");
    }

    @Test
    void releasesTableHasSubstantialContent() {
        page.navigate(baseUrl + "/releases/");
        int rowCount = page.locator(".releases-table tbody tr.release-row").count();
        assertTrue(rowCount >= 10,
                "Expected at least 10 release rows but found " + rowCount);
    }

    @Test
    void ltsCalloutIsPresent() {
        page.navigate(baseUrl + "/releases/");
        Locator callout = page.locator(".lts-callout");
        assertTrue(callout.isVisible(), "LTS callout should be visible");
    }

    @Test
    void ltsCalloutHasActionableLink() {
        page.navigate(baseUrl + "/releases/");
        Locator links = page.locator(".lts-callout a");
        assertTrue(links.count() >= 1,
                "LTS callout should contain at least one link");
        String href = links.first().getAttribute("href");
        assertNotNull(href, "LTS callout link should have an href");
        assertFalse(href.isBlank(), "LTS callout link href should not be blank");
    }

    @Test
    void releaseTableHasLtsBadges() {
        page.navigate(baseUrl + "/releases/");
        int ltsCount = page.locator(".release-badge.lts").count();
        assertTrue(ltsCount >= 2,
                "Expected at least 2 LTS badges but found " + ltsCount);
    }

    @Test
    void releaseTableHasUpcomingRelease() {
        page.navigate(baseUrl + "/releases/");
        Locator upcoming = page.locator("tr.release-upcoming");
        assertTrue(upcoming.count() >= 1,
                "Expected at least one upcoming release row");
        Locator badge = upcoming.locator(".release-status-badge.upcoming");
        assertTrue(badge.count() >= 1,
                "Upcoming release should have an upcoming status badge");
    }

    @Test
    void enterpriseSupportColumnHasBadges() {
        page.navigate(baseUrl + "/releases/");
        int rhbqCount = page.locator(".releases-table .release-support-badge.rhbq").count();
        assertTrue(rhbqCount >= 1,
                "Expected at least one RHBQ badge but found " + rhbqCount);
    }

    @Test
    void eolFilterDefaultsToChecked() {
        page.navigate(baseUrl + "/releases/");
        Locator checkbox = page.locator("#hide-eol");
        assertTrue(checkbox.isChecked(),
                "Hide EOL checkbox should be checked by default");
    }

    @Test
    void eolFilterHidesUnsupportedReleases() {
        page.navigate(baseUrl + "/releases/");
        Locator hidden = page.locator("tr.release-row.release-hidden");
        assertTrue(hidden.count() > 0,
                "Some EOL releases should be hidden by default");
    }

    @Test
    void eolFilterDoesNotHideSupportedReleases() {
        page.navigate(baseUrl + "/releases/");
        Locator supportedRows = page.locator("tr.release-row[data-supported='true']");
        for (int i = 0; i < supportedRows.count(); i++) {
            assertFalse(supportedRows.nth(i).evaluate("el => el.classList.contains('release-hidden')").equals(true),
                    "Rows with enterprise support should not be hidden by the EOL filter");
        }
    }

    @Test
    void uncheckingEolFilterShowsAllReleases() {
        page.navigate(baseUrl + "/releases/");
        Locator checkbox = page.locator("#hide-eol");
        checkbox.uncheck();
        int hidden = page.locator("tr.release-row.release-hidden").count();
        assertEquals(0, hidden,
                "No releases should be hidden after unchecking the filter");
    }

    @Test
    void eolFilterHidesRowsWithExpiredEnterpriseSupport() {
        page.navigate(baseUrl + "/releases/");
        Locator eolWithoutSupport = page.locator("tr.release-row.release-hidden:not([data-supported])");
        assertTrue(eolWithoutSupport.count() > 0,
                "Releases past both community and enterprise EOL should be hidden by default");
        for (int i = 0; i < eolWithoutSupport.count(); i++) {
            String eol = eolWithoutSupport.nth(i).getAttribute("data-eol");
            assertNotNull(eol, "Hidden row should have a data-eol attribute");
            assertFalse(eolWithoutSupport.nth(i).isVisible(),
                    "Row with expired enterprise support (eol=" + eol + ") should not be visible");
        }
    }

    @Test
    void enterpriseSupportHeaderLinksToSupportPage() {
        page.navigate(baseUrl + "/releases/");
        Locator link = page.locator(".releases-table thead a[href*='/support/']");
        assertTrue(link.count() >= 1,
                "Enterprise Support header should link to the support page");
    }

    // The timeline tests check structure and invariants, not specific versions or dates,
    // so they keep passing as releases.yaml moves on.

    @Test
    void timelineSupportViewIsRendered() {
        page.navigate(baseUrl + "/releases/");
        assertEquals(1, page.locator(".tl[data-view=support] svg.tl-plot").count());
        assertTrue(page.locator("#tl-support .tl-row").count() >= 2,
                "Support view should have at least the stream lane and one release row");
        assertTrue(page.locator("#tl-support .tl-seg--community").count() >= 1);
    }

    @Test
    void timelineLtsRowsHaveCommunitySegment() {
        page.navigate(baseUrl + "/releases/");
        Locator lts = page.locator("#tl-support .tl-row[data-lts]");
        assertTrue(lts.count() >= 1);
        for (int i = 0; i < lts.count(); i++) {
            assertTrue(lts.nth(i).locator(".tl-seg--community").count() >= 1,
                    "LTS row should have a community segment");
        }
    }

    @Test
    void timelineFullViewIsCompactButReachesFurtherBack() {
        page.navigate(baseUrl + "/releases/");
        int supportStart = Integer.parseInt(page.locator("#tl-support").getAttribute("data-range-start").substring(0, 4));
        int allStart = Integer.parseInt(page.locator("#tl-all").getAttribute("data-range-start").substring(0, 4));
        assertTrue(allStart <= supportStart);
        int rows = page.locator("#tl-all .tl-row").count();
        int tableRows = page.locator("tr.release-row").count();
        assertTrue(rows < tableRows, "Full view (" + rows + " rows) should be more compact than one row per release ("
                + tableRows + ")");
    }

    @Test
    void timelineTodayLineIsPositioned() {
        page.navigate(baseUrl + "/releases/");
        String x1 = page.locator("#tl-support .tl-today").getAttribute("x1");
        assertNotNull(x1);
        assertTrue(x1.endsWith("%"));
        Double.parseDouble(x1.substring(0, x1.length() - 1));
    }

    @Test
    void timelineTooltipShowsOnHoverAndHighlightsTableRow() {
        page.navigate(baseUrl + "/releases/");
        Locator seg = page.locator("#tl-support .tl-row[data-lts] .tl-seg--community").first();
        String version = seg.getAttribute("data-version");
        seg.hover();
        Locator tooltip = page.locator(".tl-tooltip");
        assertTrue(tooltip.isVisible());
        assertTrue(tooltip.textContent().contains("Released"));
        assertEquals(1, page.locator("tr.release-row[data-version='" + version + "'].is-highlighted").count(),
                "Matching table row should be highlighted");
    }

    @Test
    void timelineToggleSwitchesViews() {
        page.navigate(baseUrl + "/releases/");
        page.locator("[data-timeline-toggle=all]").click();
        assertTrue(page.locator("#tl-all").isVisible());
        assertFalse(page.locator("#tl-support").isVisible());
        assertEquals("true", page.locator("[data-timeline-toggle=all]").getAttribute("aria-pressed"));
    }

    @Test
    void timelineHasNoHorizontalPageScroll() {
        page.setViewportSize(400, 800);
        page.navigate(baseUrl + "/releases/");
        Object overflow = page.evaluate("() => document.documentElement.scrollWidth > document.documentElement.clientWidth");
        assertEquals(false, overflow);
    }

    @Test
    void timelineHasNoNativeTooltipWhenJsRuns() {
        page.navigate(baseUrl + "/releases/");
        assertEquals(0, page.locator(".tl .tl-seg > title").count());
        String label = page.locator("#tl-support .tl-seg").first().getAttribute("aria-label");
        assertTrue(label != null && !label.isBlank(), "Segments should keep an aria-label");
    }

    @Test
    void upcomingReleaseShowsPlannedDateOrDashInTable() {
        page.navigate(baseUrl + "/releases/");
        Locator upcoming = page.locator("tr.release-upcoming");
        org.junit.jupiter.api.Assumptions.assumeTrue(upcoming.count() > 0, "No upcoming release in the data");
        String text = upcoming.first().locator("td").nth(1).textContent().trim();
        assertTrue(text.matches("—|\\d{2} [A-Z][a-z]{2} \\d{4}\\s*\\(planned\\)"), "Unexpected released cell: " + text);
    }
}
