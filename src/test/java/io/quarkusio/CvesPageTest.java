package io.quarkusio;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CvesPageTest extends BrowserTest {

    @Test
    void cvesTableHasKeyColumns() {
        page.navigate(baseUrl + "/cves/");
        String headerText = page.locator(".cves-main-table > thead").textContent();
        Locator headers = page.locator(".cves-main-table > thead > tr > th");
        assertTrue(headers.count() >= 7,
                "Expected 7 table columns but found " + headers.count());
        assertTrue(headerText.contains("Release"), "Table should have a Release column");
        assertTrue(headerText.contains("Released"), "Table should have a Released column");
        assertTrue(headerText.contains("Community Maintenance"), "Table should have Community Maintenance column");
        assertTrue(headerText.contains("Enterprise Support"), "Table should have Enterprise Support column");
        assertTrue(headerText.contains("EOL Patches Available"), "Table should have EOL Patches column");
        assertTrue(headerText.contains("Community Micro"), "Table should have Community Micro column");
        assertTrue(headerText.contains("CVEs"), "Table should have CVEs column");
    }

    @Test
    void cvesTableHasRows() {
        page.navigate(baseUrl + "/cves/");
        int rowCount = page.locator(".cves-main-table > tbody > tr.release-row").count();
        assertTrue(rowCount >= 5,
                "Expected at least 5 release rows but found " + rowCount);
    }

    @Test
    void filtersDefaultToChecked() {
        page.navigate(baseUrl + "/cves/");
        Locator eolCheckbox = page.locator("#hide-eol");
        assertTrue(eolCheckbox.isChecked(), "Hide EOL checkbox should be checked by default");

        Locator pastMicrosCheckbox = page.locator("#hide-past-micros");
        assertTrue(pastMicrosCheckbox.isChecked(), "Hide past micros checkbox should be checked by default");
    }

    @Test
    void pastMicrosAreHiddenByDefault() {
        page.navigate(baseUrl + "/cves/");
        Locator pastMicros = page.locator("tr.release-row[data-past-micro='true']");
        assertTrue(pastMicros.count() > 0, "There should be past micro rows in the table");
        for (int i = 0; i < pastMicros.count(); i++) {
            assertTrue(pastMicros.nth(i).evaluate("el => el.classList.contains('release-hidden')").equals(true),
                    "Past micro row should be hidden by default");
        }
    }

    @Test
    void uncheckingPastMicrosFilterRevealsThem() {
        page.navigate(baseUrl + "/cves/");
        Locator pastMicrosCheckbox = page.locator("#hide-past-micros");
        pastMicrosCheckbox.uncheck();

        // Active/non-EOL past micros should now be visible
        Locator pastMicrosNotEol = page.locator("tr.release-row[data-past-micro='true']:not(.release-hidden)");
        assertTrue(pastMicrosNotEol.count() > 0, "Unchecking past micros filter should make past micros visible");
    }

    @Test
    void twistieExpandsCveDetailsRow() {
        page.navigate(baseUrl + "/cves/");
        // Find the first visible release row with a twistie button
        Locator visibleRow = page.locator("tr.release-row:not(.release-hidden)").first();
        Locator btn = visibleRow.locator(".cve-twistie-btn");
        assertTrue(btn.count() >= 1, "Visible release row should have a twistie button");

        String detailsId = btn.getAttribute("aria-controls");
        assertNotNull(detailsId, "Button should have aria-controls");

        Locator detailsRow = page.locator("#" + detailsId);
        assertFalse(detailsRow.isVisible(), "Details row should be hidden initially");

        // Click twistie
        btn.click();
        assertEquals("true", btn.getAttribute("aria-expanded"), "Button should have aria-expanded='true'");
        assertTrue(detailsRow.isVisible(), "Details row should now be visible");

        // Click again to collapse
        btn.click();
        assertEquals("false", btn.getAttribute("aria-expanded"), "Button should have aria-expanded='false'");
        assertFalse(detailsRow.isVisible(), "Details row should be hidden again");
    }

    @Test
    void cvesTableHasFixedInColumn() {
        page.navigate(baseUrl + "/cves/");
        Locator visibleRow = page.locator("tr.release-row:not(.release-hidden)").first();
        Locator btn = visibleRow.locator(".cve-twistie-btn");
        btn.click();

        String detailsId = btn.getAttribute("aria-controls");
        Locator detailsRow = page.locator("#" + detailsId);
        assertTrue(detailsRow.isVisible());

        Locator fixedInHeader = detailsRow.locator(".cves-table thead th[data-sort='fixed']");
        assertTrue(fixedInHeader.count() >= 1, "CVE table should have a Fixed in column header");
        assertTrue(fixedInHeader.first().textContent().contains("Fixed in"));
    }

    @Test
    void cvesTableCanBeSorted() {
        page.navigate(baseUrl + "/cves/");
        // Find a visible row with at least 2 CVEs
        Locator rows = page.locator("tr.release-row:not(.release-hidden)");
        Locator targetBtn = null;
        for (int i = 0; i < rows.count(); i++) {
            Locator btn = rows.nth(i).locator(".cve-twistie-btn:not(.none)");
            if (btn.count() > 0) {
                targetBtn = btn;
                break;
            }
        }
        assertNotNull(targetBtn, "Should find a release row with CVEs");
        targetBtn.click();

        String detailsId = targetBtn.getAttribute("aria-controls");
        Locator detailsRow = page.locator("#" + detailsId);
        Locator cveTable = detailsRow.locator(".cves-table").first();

        // Default sort should be Severity
        Locator severityTh = cveTable.locator("thead th[data-sort='severity']");
        assertTrue(severityTh.evaluate("el => el.classList.contains('sort-active')").equals(true),
                "Severity column header should have sort-active by default");

        // Click Advisory header to sort by Advisory
        Locator advisoryTh = cveTable.locator("thead th[data-sort='advisory']");
        advisoryTh.click();
        assertTrue(advisoryTh.evaluate("el => el.classList.contains('sort-active')").equals(true),
                "Advisory column header should become sort-active after click");
        assertFalse(severityTh.evaluate("el => el.classList.contains('sort-active')").equals(true),
                "Severity column header should no longer be sort-active");

        // Click Published header to sort by Published
        Locator publishedTh = cveTable.locator("thead th[data-sort='published']");
        publishedTh.click();
        assertTrue(publishedTh.evaluate("el => el.classList.contains('sort-active')").equals(true),
                "Published column header should become sort-active after click");

        // Click Fixed in header to sort by Fixed in
        Locator fixedTh = cveTable.locator("thead th[data-sort='fixed']");
        fixedTh.click();
        assertTrue(fixedTh.evaluate("el => el.classList.contains('sort-active')").equals(true),
                "Fixed in column header should become sort-active after click");
    }
}
