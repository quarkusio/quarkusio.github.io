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
}
