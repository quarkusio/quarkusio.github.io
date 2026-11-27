package io.quarkusio;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class CvesPageTest extends BrowserTest {

    static final String ROWS = "#tl-exposure .tl-row:not([display=none])";
    static final String MARKS = ROWS + " a.tl-mark:not([display=none])";

    // ---- the plot

    @Test
    void plotHasMarksColouredBySeverity() {
        page.navigate(baseUrl + "/cves/");
        assertTrue(page.locator("#tl-exposure .tl-mark").count() >= 10, "Expected many CVE marks");
        for (String sev : new String[] { "critical", "high", "medium", "low" }) {
            assertTrue(page.locator("#tl-exposure .tl-mark.tl-sev--" + sev).count() >= 1, "Expected a " + sev + " mark");
        }
    }

    @Test
    void fixedCvesAreBarsAndUnfixedOnesEndInACrossAndPendingOnesFadeIn() {
        page.navigate(baseUrl + "/cves/");
        openAll();
        assertTrue(page.locator("#tl-exposure .tl-mark .tl-mark-post").count() >= 10, "Expected bars");
        int crosses = page.locator("#tl-exposure .tl-cross").count();
        assertTrue(crosses >= 1, "Expected a cross on CVEs that are not fixed yet");
        assertEquals(crosses, page.locator("#tl-exposure .tl-mark--open .tl-cross").count(), "Only unfixed CVEs end in a cross");
        int fades = page.locator("#tl-exposure .tl-mark--pending .tl-mark-fade").count();
        assertTrue(fades >= 1, "Expected pending CVEs to fade in");
        assertEquals(fades, page.locator("#tl-exposure .tl-mark-fade").count(), "Only pending CVEs fade in");
        assertTrue(crosses > fades, "Unfixed CVEs that were assessed also end in a cross");
    }

    // ---- the views

    @Test
    void exposureIsShownFirstAndTheToggleFlipsToDiscovered() {
        page.navigate(baseUrl + "/cves/");
        assertTrue(page.locator("#tl-exposure").isVisible());
        assertFalse(page.locator("#tl-discovered").isVisible());
        page.locator("[data-timeline-toggle=discovered]").click();
        assertFalse(page.locator("#tl-exposure").isVisible());
        assertTrue(page.locator("#tl-discovered").isVisible());
        assertEquals("true", page.locator("[data-timeline-toggle=discovered]").getAttribute("aria-pressed"));
        assertEquals("false", page.locator("[data-timeline-toggle=exposure]").getAttribute("aria-pressed"));
    }

    @Test
    void discoveredViewHasDotsAndCrossesForUnresolvedOnes() {
        page.navigate(baseUrl + "/cves/");
        openAll();
        assertEquals(0, page.locator("#tl-discovered .tl-mark-post").count(), "No bars in the discovery view");
        assertTrue(page.locator("#tl-discovered .tl-mark:not(.tl-mark--open) circle.tl-dot").count() >= 10);
        int crosses = page.locator("#tl-discovered .tl-cross").count();
        assertTrue(crosses >= 1);
        assertEquals(crosses, page.locator("#tl-discovered .tl-mark--open .tl-cross").count());
        assertEquals(crosses, page.locator("#tl-exposure .tl-cross").count(), "Same unresolved CVEs in both views");
    }

    @Test
    void filtersApplyToBothViews() {
        page.navigate(baseUrl + "/cves/");
        allTime();
        openAll();
        setChip("status", "fixed", false);
        page.locator("[data-timeline-toggle=discovered]").click();
        Object everyMarkOpen = page.locator("#tl-discovered .tl-mark:not([display=none])")
                .evaluateAll("marks => marks.length > 0 && marks.every(m => m.classList.contains('tl-mark--open'))");
        assertEquals(Boolean.TRUE, everyMarkOpen);
        assertEquals(page.locator("#tl-exposure .tl-row:not([display=none])").count(),
                page.locator("#tl-discovered .tl-row:not([display=none])").count());
    }

    // ---- the ring

    @Test
    void tooltipStaysInsideTheWindowEvenForAMarkNearTheBottom() {
        page.setViewportSize(1280, 500);
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(MARKS).first();
        mark.scrollIntoViewIfNeeded();
        page.evaluate("() => window.scrollTo(0, document.body.scrollHeight)");
        Locator last = page.locator(MARKS).last();
        last.scrollIntoViewIfNeeded();
        var box = last.boundingBox();
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        Locator tip = page.locator(".tl-tooltip");
        tip.waitFor();
        var t = tip.boundingBox();
        assertTrue(t.y >= 0 && t.y + t.height <= 500, "Tooltip must be inside the window: " + t.y + " + " + t.height);
    }

    @Test
    void marksLinkToTheAdvisory() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        String href = page.locator(MARKS).first().getAttribute("href");
        assertTrue(href.startsWith("https://www.cve.org/") || href.startsWith("https://github.com/advisories/"), href);
    }

    @Test
    void overlappingCvesOfOneArtifactAreInSeparateLanes() {
        page.navigate(baseUrl + "/cves/");
        openAll();
        assertTrue(page.locator("#tl-exposure .tl-labels li.tl-h-18").count() < page.locator("#tl-exposure .tl-labels li").count(),
                "Artifacts with overlapping CVEs should have taller rows");
        // within a row, no two CVEs may overlap, whatever their parts
        Object overlaps = page.evaluate("() => {"
                + "const bad = [];"
                + "document.querySelectorAll('#tl-exposure .tl-row:not([display=none])').forEach(row => {"
                + "  const parts = [];"
                + "  row.querySelectorAll('a.tl-mark').forEach((m, n) => m.querySelectorAll('.tl-mark-pre, .tl-mark-post, .tl-cross')"
                + "    .forEach(e => parts.push({n, box: e.getBoundingClientRect()})));"
                + "  for (let i = 0; i < parts.length; i++) for (let j = i + 1; j < parts.length; j++) {"
                + "    if (parts[i].n === parts[j].n) continue;"
                + "    const a = parts[i].box, b = parts[j].box;"
                + "    if (a.left < b.right - 1 && b.left < a.right - 1 && a.top < b.bottom - 1 && b.top < a.bottom - 1) bad.push(row.dataset.timelineRow);"
                + "  }"
                + "});"
                + "return bad.length;"
                + "}");
        assertEquals(0, ((Number) overlaps).intValue(), "Marks overlap within a row");
    }

    // ---- "since at least"

    @Test
    void barsThatMayGoBackFurtherThanOurDataHaveAnArrowAndALegendEntry() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        assertTrue(page.locator("#tl-exposure .tl-mark .tl-more").count() >= 1, "Expected arrows at the start of some bars");
        assertEquals(1, page.locator(".tl-legend .tl-swatch--since").count(), "The arrow needs a legend entry");
        assertTrue(page.locator(".tl-legend .tl-swatch--since").locator("xpath=..").textContent().contains("earliest release"));
        // the arrow sits at the left end of its bar, and follows it when the span changes
        for (int pass = 0; pass < 2; pass++) {
            Locator mark = page.locator(MARKS + ":has(.tl-more)").first();
            mark.scrollIntoViewIfNeeded();
            var arrow = mark.locator(".tl-more").boundingBox();
            var pale = mark.locator(".tl-mark-pre").boundingBox();
            assertTrue(Math.abs((arrow.x + arrow.width) - pale.x) <= 3, "Arrow should end where the bar starts: "
                    + (arrow.x + arrow.width) + " vs " + pale.x);
            assertTrue(Math.abs((arrow.y + arrow.height / 2) - (pale.y + pale.height / 2)) <= 4, "Arrow should be level with its bar");
            openAll(); // moves the axis, so everything is placed again
        }
        // and not every bar has one: those that arrived in a later release start where they started
        assertTrue(page.locator("#tl-exposure .tl-mark:not(:has(.tl-more)):has(.tl-mark-pre)").count() >= 1);
    }

    @Test
    void tooltipSaysSinceAtLeastForThoseBars() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(MARKS + ":has(.tl-more)").first();
        mark.scrollIntoViewIfNeeded();
        hoverSteadily(mark.locator("rect.tl-hit"));
        String text = page.locator(".tl-tooltip").textContent();
        assertTrue(text.contains("since at least") && text.contains("the earliest we have data for"), text);
        assertTrue(text.contains("(3.27)") || text.matches(".*\\(\\d+\\.\\d+\\).*"), "It names the oldest release line: " + text);
    }

    // ---- the legend

    @Test
    void theLegendExplainsTheBarKindsAndTheSeverityChipsAreTheKeyToTheColours() {
        page.navigate(baseUrl + "/cves/");
        assertEquals(0, page.locator(".tl-legend[aria-label='Severity colours']").count(), "No separate severity legend row");
        Locator kinds = page.locator(".tl-legend:not([aria-label])");
        assertEquals(4, kinds.locator("li").count(), "Fixed, since at least, not fixed yet and pending");
        assertEquals(1, kinds.locator(".tl-swatch--since").count(), "The arrow is explained in the bar kinds row");
        assertTrue(kinds.locator(".tl-legend-desc").first().textContent().length() > 10, "Bar kinds have descriptions");
        assertFalse(page.locator(".tl-legend--providers").isVisible(), "No provider row on this page");
        Object count = page.evaluate("() => new Set(['critical','high','medium','low'].map(s => "
                + "getComputedStyle(document.querySelector('.cve-chip--' + s + ' .cve-chip-swatch')).borderTopColor)).size");
        assertEquals(4, ((Number) count).intValue(), "Each severity chip needs its own colour");
    }

    // ---- hovering

    @Test
    void hoveringAMarkShowsTheCveInATooltip() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(MARKS).first();
        mark.scrollIntoViewIfNeeded();
        String title = mark.getAttribute("data-tooltip-title");
        assertTrue(title.startsWith("CVE-") || title.startsWith("GHSA-"), title);
        hoverSteadily(mark.locator("rect.tl-hit"));
        assertTrue(page.locator(".tl-tooltip").isVisible());
        assertTrue(page.locator(".tl-tooltip").textContent().contains(title));
    }

    @Test
    void hoveringBesideTheCrossStillShowsTheTooltip() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(ROWS + " a.tl-mark--pending:not([display=none])").first();
        mark.scrollIntoViewIfNeeded();
        var box = mark.locator(".tl-cross").boundingBox();
        // beside the two thin lines of the cross, where only the hit area can catch the pointer
        page.mouse().move(box.x + box.width / 2 + 3, box.y + box.height / 2);
        assertTrue(page.locator(".tl-tooltip").isVisible(), "Tooltip should show near the cross");
        assertTrue(page.locator(".tl-tooltip").textContent().contains(mark.getAttribute("data-tooltip-title")));
    }

    @Test
    void hoveringAPendingBarShowsThatItIsPending() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(ROWS + " a.tl-mark--pending:not([display=none])").first();
        mark.scrollIntoViewIfNeeded();
        for (String part : new String[] { ".tl-mark-fade", ".tl-mark-post" }) {
            hoverSteadily(mark.locator(part));
            assertTrue(page.locator(".tl-tooltip").isVisible(), "Tooltip should show over " + part);
            assertTrue(page.locator(".tl-tooltip").textContent().contains("Pending"), part);
            page.mouse().move(0, 0);
        }
    }

    @Test
    void hoveringThePalePartShowsTheTooltipToo() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(MARKS + ":not(.tl-mark--pending):has(.tl-mark-pre)").first();
        mark.scrollIntoViewIfNeeded();
        hoverSteadily(mark.locator(".tl-mark-pre"));
        assertTrue(page.locator(".tl-tooltip").isVisible(), "Tooltip should show over the pale part");
        assertTrue(page.locator(".tl-tooltip").textContent().contains(mark.getAttribute("data-tooltip-title")));
    }

    @Test
    void tooltipKeepsLongUnbrokenTextInsideItsBox() {
        page.navigate(baseUrl + "/cves/");
        openAll();
        // its summary has a long method name with no spaces in it
        Locator mark = page.locator(MARKS + "[data-tooltip-title^='CVE-2026-89425']").first();
        mark.scrollIntoViewIfNeeded();
        hoverSteadily(mark.locator("rect.tl-hit"));
        assertTrue(page.locator(".tl-tooltip").isVisible());
        assertTrue(page.locator(".tl-tooltip").textContent().startsWith("CVE-2026-89425"),
                "Should be showing the CVE under test: " + page.locator(".tl-tooltip").textContent());
        Object overflowing = page.evaluate("() => { const tip = document.querySelector('.tl-tooltip');"
                + " return [tip, ...tip.querySelectorAll('div, strong')].filter(e => e.scrollWidth > e.clientWidth + 1).length; }");
        assertEquals(0, ((Number) overflowing).intValue(), "No line of the tooltip may be wider than the tooltip");
    }

    @Test
    void hoveringAComponentDoesNotMoveAnythingOnThePage() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator first = page.locator(ROWS).first();
        first.scrollIntoViewIfNeeded();
        double height = plotHeight();
        assertEquals(7, laneSpacing(), "Neighbouring lanes are one bar height plus 1px apart");

        var label = page.locator("#tl-exposure .tl-labels li:not([hidden])").first().boundingBox();
        page.mouse().move(label.x + 5, label.y + label.height / 2);
        page.waitForTimeout(150);
        assertEquals(height, plotHeight(), 0.5, "Hovering a component must not change the layout");
        assertEquals(7, laneSpacing());

        var plotBox = first.boundingBox();
        page.mouse().move(plotBox.x + plotBox.width / 2, plotBox.y + 1);
        page.waitForTimeout(150);
        assertEquals(height, plotHeight(), 0.5);
    }

    @Test
    void theTimePeriodStartsAtAYearAndThePresetsAndThePlotFollowIt() {
        page.navigate(baseUrl + "/cves/");
        assertEquals("true", page.locator("[data-cve-preset='12']").getAttribute("aria-pressed"));
        String readout = page.locator("#cve-brush-readout").textContent();
        assertTrue(readout.matches("[A-Z][a-z]{2} \\d{4} – [A-Z][a-z]{2} \\d{4} · \\d+ CVEs?"), readout);
        openAllHigh();
        String recent = page.locator("#tl-exposure").getAttribute("data-range-start");
        int recentMarks = visibleMarks();
        assertTrue(recent.compareTo(java.time.LocalDate.now().minusMonths(13).toString()) >= 0,
                "The axis starts no earlier than the period: " + recent);

        allTime();
        assertEquals("true", page.locator("[data-cve-preset='0']").getAttribute("aria-pressed"));
        assertEquals("All time", page.locator("#cve-brush-readout").textContent().split(" · ")[0]);
        assertTrue(page.locator("#tl-exposure").getAttribute("data-range-start").compareTo(recent) < 0, "All time starts earlier");
        assertTrue(visibleMarks() > recentMarks, "Looking further back brings in more CVEs");
        assertTrue(page.locator(".cve-slider-status").textContent().contains("All time"));

        preset(3);
        assertTrue(page.locator(".cve-slider-status").textContent().contains(" – "));
        assertTrue(visibleMarks() <= recentMarks);
        preset(36);
        assertTrue(visibleMarks() >= recentMarks);
    }

    @Test
    void theStripIsLinedUpWithThePlotAreaAndHasAColumnForEachMonth() {
        page.navigate(baseUrl + "/cves/");
        var strip = page.locator(".cve-brush-svg").boundingBox();
        var plot = page.locator("#tl-exposure .tl-plot").boundingBox();
        assertEquals(plot.x, strip.x, 1.5, "Left edges line up");
        assertEquals(plot.width, strip.width, 1.5, "Widths line up");
        assertTrue(strip.y < plot.y, "The strip is above the plot");
        assertTrue(page.locator(".cve-brush-col").count() >= 24, "A column for each month with CVEs");
        assertTrue(page.locator(".cve-brush-col rect.is-open").count() >= 1, "Not fixed ones are stacked on");
        assertTrue(page.locator(".cve-brush-axis text").count() >= 5, "Years along the top");
    }

    @Test
    void draggingTheWindowMovesThePeriodAndTheHandlesResizeIt() {
        page.navigate(baseUrl + "/cves/");
        allTime();
        preset(12);
        String before = page.locator("#cve-brush-readout").textContent();
        var strip = page.locator(".cve-brush-svg").boundingBox();
        var window = page.locator(".cve-brush-window").boundingBox();

        // move the window left by about a quarter of the strip
        page.mouse().move(window.x + window.width / 2, strip.y + strip.height / 2);
        page.mouse().down();
        page.mouse().move(window.x + window.width / 2 - strip.width / 4, strip.y + strip.height / 2, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(5));
        page.mouse().up();
        page.waitForTimeout(100);
        String moved = page.locator("#cve-brush-readout").textContent();
        assertNotEquals(before, moved);
        assertFalse(moved.contains("Nov 2026") && moved.startsWith("Nov 2025"), moved);
        assertEquals("false", page.locator("[data-cve-preset='12']").getAttribute("aria-pressed"), "No longer a preset");
        assertTrue(query().contains("from="), query());
        String period = moved.split(" · ")[0];

        // drag the left handle to the left to make the period longer
        var handle = page.locator(".cve-brush-handle rect").first().boundingBox();
        page.mouse().move(handle.x + handle.width / 2, strip.y + strip.height / 2);
        page.mouse().down();
        page.mouse().move(handle.x - strip.width / 4, strip.y + strip.height / 2, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(5));
        page.mouse().up();
        page.waitForTimeout(100);
        assertNotEquals(period, page.locator("#cve-brush-readout").textContent().split(" · ")[0], "The period is longer");
        assertTrue(page.locator("#cve-brush-readout").textContent().split(" · ")[0].endsWith(period.split(" – ")[1]), "The end stayed");

        // dragging on empty strip makes a new window
        page.mouse().move(strip.x + strip.width * 0.05, strip.y + strip.height / 2);
        page.mouse().down();
        page.mouse().move(strip.x + strip.width * 0.15, strip.y + strip.height / 2, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(5));
        page.mouse().up();
        page.waitForTimeout(100);
        assertTrue(page.locator("#cve-brush-readout").textContent().contains(" – "));
        assertTrue(query().contains("to="), "The period no longer ends now: " + query());
        assertFalse(page.locator("#tl-exposure .tl-today-tag").isVisible(), "Today is not in the period");
    }

    @Test
    void theRangeInputsSetThePeriodForTheKeyboard() {
        page.navigate(baseUrl + "/cves/");
        int last = Integer.parseInt(page.locator("#cve-to").inputValue());
        assertEquals(last, Integer.parseInt(page.locator("#cve-to").getAttribute("max")));
        assertEquals(0, Integer.parseInt(page.locator("#cve-from").getAttribute("min")));
        assertEquals(last - 11, Integer.parseInt(page.locator("#cve-from").inputValue()), "A year");
        assertTrue(page.locator("#cve-from").getAttribute("aria-valuetext").matches("[A-Z][a-z]+ \\d{4}"));

        page.locator("#cve-from").focus();
        page.keyboard().press("ArrowLeft");
        assertEquals(last - 12, Integer.parseInt(page.locator("#cve-from").inputValue()));
        page.locator("#cve-to").focus();
        page.keyboard().press("ArrowLeft");
        assertEquals(last - 1, Integer.parseInt(page.locator("#cve-to").inputValue()));
        assertTrue(query().contains("to="));
        setSlider("cve-from", last); // past the end: the end follows
        assertEquals(last, Integer.parseInt(page.locator("#cve-to").inputValue()));
    }

    @Test
    void thePeriodFiltersTheDiscoveryViewToo() {
        page.navigate(baseUrl + "/cves/");
        openAll();
        page.locator("[data-timeline-toggle=discovered]").click();
        int recent = page.locator("#tl-discovered .tl-mark:not([display=none])").count();
        allTime();
        assertTrue(page.locator("#tl-discovered .tl-mark:not([display=none])").count() > recent);
    }

    @Test
    void theTooltipStaysWhileThePointerIsOnIt() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        Locator mark = page.locator(ROWS).first().locator("a.tl-mark:not([display=none])").first();
        mark.scrollIntoViewIfNeeded();
        hoverSteadily(mark.locator("rect.tl-hit"));
        Locator tip = page.locator(".tl-tooltip");
        assertTrue(tip.isVisible());
        var box = tip.boundingBox();
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        page.waitForTimeout(500);
        assertTrue(tip.isVisible(), "The tooltip stays while the pointer is on it, so its text can be copied");
        page.mouse().move(0, 0);
        page.waitForTimeout(500);
        assertTrue(tip.isHidden(), "It goes when the pointer leaves it");
    }

    @Test
    void responsivenessTabSitsBetweenExposureAndDiscoveryAndHasNoTimeBeforePublication() {
        page.navigate(baseUrl + "/cves/");
        var tabs = page.locator(".tl-toolbar .tl-toggle").allTextContents();
        assertEquals(java.util.List.of("Exposure", "Responsiveness", "Discovery"), tabs);

        allSeverities();
        openAll();
        allTime();
        page.locator("[data-timeline-toggle=responsiveness]").click();
        assertTrue(page.locator("#tl-responsiveness").isVisible());
        assertTrue(page.locator("#tl-exposure").isHidden());
        assertEquals(0, page.locator("#tl-responsiveness .tl-mark:not([display=none]) .tl-mark-pre").count(),
                "No paler part before publication");
        assertEquals(0, page.locator("#tl-responsiveness .tl-more").count(), "No arrows");
        assertTrue(page.locator("#tl-responsiveness .tl-mark:not([display=none])").count() > 0);
        assertTrue(query().contains("view=responsiveness"), query());

        // the filters apply to it too
        int all = page.locator("#tl-responsiveness .tl-mark:not([display=none])").count();
        onlySeverity("critical");
        assertTrue(page.locator("#tl-responsiveness .tl-mark:not([display=none])").count() < all);
        allSeverities();
        preset(3);
        assertTrue(page.locator("#tl-responsiveness .tl-mark:not([display=none])").count() < all);
    }

    @Test
    void theResponsivenessViewCanBeOpenedFromTheAddress() {
        page.navigate(baseUrl + "/cves/?view=responsiveness");
        assertTrue(page.locator("#tl-responsiveness").isVisible());
        assertEquals("true", page.locator("[data-timeline-toggle=responsiveness]").getAttribute("aria-pressed"));
    }

    // ---- the categories

    private static final String CATS = "#tl-exposure .tl-cat:not([hidden])";

    @Test
    void everythingStartsCollapsedInOneRowPerCategory() {
        page.navigate(baseUrl + "/cves/");
        int categories = page.locator(CATS).count();
        assertTrue(categories >= 2 && categories <= 8, "One row per category: " + categories);
        assertEquals(0, page.locator("#tl-exposure .tl-component:not([hidden])").count(), "No components until one is opened");
        assertEquals(0, visibleRows());
        assertEquals(categories, page.locator(CATS + " button[aria-expanded=false]").count());
        assertEquals(0, page.locator("#tl-exposure .tl-fold:not([hidden])").count());
    }

    @Test
    void aCategoryRowShowsItsCountOpenCountAndMedian() {
        page.navigate(baseUrl + "/cves/");
        String meta = page.locator(CATS + " .cve-cat-meta").first().textContent();
        assertTrue(meta.matches("\\d+ CVEs? · \\d+ open"), meta);
        String median = page.locator("#tl-exposure .tl-cat-median:visible").first().textContent();
        assertTrue(median.matches("median (\\d+ d|—)"), median);
        for (Locator open : page.locator(CATS + " .cve-cat-open.has-open").all()) {
            assertFalse(open.textContent().startsWith("0 "), "Only a non-zero open count is highlighted");
        }
    }

    @Test
    void everyViewHasAStripOfCvesPerMonthInEachCategory() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        allTime();
        for (String view : new String[] { "exposure", "responsiveness", "discovered" }) {
            page.locator("[data-timeline-toggle=" + view + "]").click();
            assertTrue(page.locator("#tl-" + view + " .tl-cat-row:not([display=none]) .tl-cat-col").count() >= 5, view);
            assertEquals(page.locator("#tl-" + view + " .tl-cat:not([hidden])").count(),
                    page.locator("#tl-" + view + " .tl-cat-row:not(.tl-fold-row):not([display=none])").count(), view);
        }
        // the crosses are the CVEs that are not fixed yet
        page.locator("[data-timeline-toggle=discovered]").click();
        assertTrue(page.locator("#tl-discovered .tl-cat-cross").count() >= 1);
    }

    @Test
    void theStatusAndOriginRingsAreOnTheSameLine() {
        page.navigate(baseUrl + "/cves/");
        var status = page.locator("[data-cve-donut=status]").boundingBox();
        var origin = page.locator("[data-cve-donut=origin]").boundingBox();
        assertEquals(status.y, origin.y, 2, "Side by side");
        assertTrue(origin.x > status.x);
    }

    @Test
    void theStripIsBlackWhileACategoryIsClosedAndFainterOnceItIsOpenNeverBlue() {
        page.navigate(baseUrl + "/cves/");
        String text = (String) page.evaluate("() => getComputedStyle(document.body).color");
        String closed = (String) page.locator("#tl-exposure .tl-cat-col").first().evaluate("e => getComputedStyle(e).fill");
        assertEquals(text, closed, "The same as the text, which is black on the light theme");
        double closedOpacity = Double.parseDouble((String) page.locator("#tl-exposure .tl-cat-col").first()
                .evaluate("e => getComputedStyle(e).fillOpacity"));
        page.locator(CATS + " button").first().click();
        Locator open = page.locator("#tl-exposure .tl-cat-row.is-open .tl-cat-col").first();
        assertEquals(text, open.evaluate("e => getComputedStyle(e).fill"), "Still not blue");
        assertTrue(Double.parseDouble((String) open.evaluate("e => getComputedStyle(e).fillOpacity")) < closedOpacity,
                "Fainter while the category shows its components");
        assertEquals(closedOpacity, Double.parseDouble((String) page.locator("#tl-exposure .tl-cat-row:not(.is-open) .tl-cat-col").first()
                .evaluate("e => getComputedStyle(e).fillOpacity")), "The others stay as they were");
    }

    @Test
    void clickingACategoryOpensItsComponentsAndPutsItInTheAddress() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        Locator first = page.locator(CATS + " button").first();
        String name = page.locator(CATS + " .cve-cat-name").first().textContent();
        first.click();
        assertEquals("true", first.getAttribute("aria-expanded"));
        assertTrue(page.locator("#tl-exposure .tl-component:not([hidden])").count() >= 1);
        assertTrue(visibleRows() >= 1);
        assertTrue(page.locator("#tl-exposure .tl-component:not([hidden])").first().getAttribute("data-category").equals(name));
        assertTrue(query().contains("open=" + name), query());
        first.click();
        assertEquals(0, page.locator("#tl-exposure .tl-component:not([hidden])").count());
        assertFalse(query().contains("open="), "Collapsed again, no category in the address");
    }

    @Test
    void expandAllAndCollapseAll() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        openAll();
        int categories = page.locator(CATS).count();
        assertEquals(categories, page.locator(CATS + " button[aria-expanded=true]").count());
        assertTrue(page.locator("#tl-exposure .tl-component:not([hidden])").count() > categories);
        page.locator("[data-cve-expand=none]").click();
        assertEquals(0, page.locator("#tl-exposure .tl-component:not([hidden])").count());
    }

    @Test
    void componentsWithOneCveAreFoldedIntoALineThatOpens() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        allTime();
        for (Locator button : page.locator(CATS + " button").all()) {
            button.click();
        }
        int folds = page.locator("#tl-exposure .tl-fold:not([hidden])").count();
        assertTrue(folds >= 1, "Some category has components with one CVE");
        String text = page.locator("#tl-exposure .tl-fold:not([hidden])").first().textContent();
        assertTrue(text.matches(".*\\+ \\d+ components? with 1 CVE"), text);
        int before = page.locator("#tl-exposure .tl-component:not([hidden])").count();
        page.locator("#tl-exposure .tl-fold:not([hidden]) button").first().click();
        assertTrue(page.locator("#tl-exposure .tl-component:not([hidden])").count() > before);
    }

    @Test
    void componentsAreGroupedByProjectWithOpenOnesFirst() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        allTime();
        openAll();
        Object ok = page.locator("#tl-exposure .tl-labels li:not([hidden])").evaluateAll("""
                items => {
                  let seen = new Set(), last = null, folded = false;
                  for (const li of items) {
                    if (li.classList.contains('tl-cat')) { seen = new Set(); last = null; folded = false; continue; }
                    if (li.classList.contains('tl-fold')) { folded = true; continue; }
                    if (folded) continue; // the components with one CVE come after their line
                    const project = li.getAttribute('data-project');
                    if (project !== last) { if (seen.has(project)) return false; seen.add(project); last = project; }
                  }
                  return true;
                }""");
        assertEquals(Boolean.TRUE, ok, "The components of one project sit together");
    }

    @Test
    void theFiltersChangeTheStripAndTheCounts() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        allTime();
        int all = page.locator(CATS).count();
        String before = page.locator(CATS + " .cve-cat-meta").allTextContents().toString();
        onlySeverity("critical");
        assertTrue(page.locator(CATS).count() <= all);
        assertNotEquals(before, page.locator(CATS + " .cve-cat-meta").allTextContents().toString());
    }

    // ---- the address

    @Test
    void theControlsAreKeptInTheAddressAndRestoredFromIt() {
        page.navigate(baseUrl + "/cves/");
        assertEquals("", query(), "The starting state needs no parameters");

        setChip("severity", "low", true);
        preset(36);
        setChip("origin", "external", false);
        setChip("status", "pending", false);
        String name = page.locator(CATS + " .cve-cat-name").first().textContent();
        page.locator(CATS + " button").first().click();
        page.locator("[data-timeline-toggle=discovered]").click();
        String query = query();
        for (String expected : new String[] { "severity=critical&severity=high&severity=low", "from=" + java.time.LocalDate.now().minusMonths(35).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")), "origin=internal", "status=fixed&status=open",
                "open=" + name, "view=discovered" }) {
            assertTrue(query.contains(expected), expected + " in " + query);
        }

        String url = page.url();
        int marks = page.locator("#tl-discovered .tl-mark:not([display=none])").count();
        page.navigate("about:blank");
        page.navigate(url);
        assertTrue(chip("severity", "low").isChecked());
        assertFalse(chip("severity", "medium").isChecked());
        assertEquals("true", page.locator("[data-cve-preset='36']").getAttribute("aria-pressed"));
        assertFalse(chip("origin", "external").isChecked());
        assertTrue(chip("origin", "internal").isChecked());
        assertFalse(chip("status", "pending").isChecked());
        assertTrue(page.locator("#tl-discovered").isVisible(), "The view is restored");
        assertEquals("true", page.locator("#tl-discovered .tl-cat:not([hidden]) button").first().getAttribute("aria-expanded"));
        assertEquals(marks, page.locator("#tl-discovered .tl-mark:not([display=none])").count(), "The plot is laid out for it");

        setChip("severity", "low", false);
        preset(12);
        setChip("origin", "external", true);
        setChip("status", "pending", true);
        page.locator("#tl-discovered .tl-cat:not([hidden]) button").first().click();
        page.locator("[data-timeline-toggle=exposure]").click();
        assertEquals("", query(), "Back at the starting state, the parameters are gone");
    }

    @Test
    void nothingOnInAFilterSurvivesAReloadAndMeansNothingShown() {
        page.navigate(baseUrl + "/cves/?status=&origin=internal");
        assertEquals(0, page.locator("[data-cve-status]:checked").count(), "An empty parameter means none");
        assertEquals(0, page.locator(CATS).count());
        allSeverities();
        allTime();
        setChip("status", "fixed", true);
        assertTrue(page.locator(CATS).count() >= 1);
    }

    @Test
    void nonsenseInTheAddressIsIgnored() {
        page.navigate(baseUrl + "/cves/?minCves=3&category=Web&severity=purple&status=nope&from=banana&to=2999-99&back=-3&view=nope&open=Nonsense");
        assertEquals(0, page.locator("[data-cve-severity]:checked").count(), "Nothing named is on");
        assertEquals("true", page.locator("[data-cve-preset='12']").getAttribute("aria-pressed"));
        assertTrue(page.locator("#tl-exposure").isVisible());
        assertEquals(0, page.locator("#tl-exposure .tl-component:not([hidden])").count());
    }

    // ---- the sliders and the switches

    @Test
    void severityChipsAreCheckboxesThatStartAtHighAndAbove() {
        page.navigate(baseUrl + "/cves/");
        for (String sev : new String[] { "critical", "high", "medium", "low" }) {
            assertEquals("checkbox", chip("severity", sev).getAttribute("type"));
        }
        assertTrue(chip("severity", "critical").isChecked());
        assertTrue(chip("severity", "high").isChecked());
        assertFalse(chip("severity", "medium").isChecked());
        assertFalse(chip("severity", "low").isChecked());
        String count = page.locator(".cve-chip--medium .cve-chip-count").textContent();
        assertTrue(count.matches("\\d+") && Integer.parseInt(count) > 0, "An off chip still says what it would add: " + count);
    }

    @Test
    void theSeverityShortcutsAndChipsFilterThePlot() {
        page.navigate(baseUrl + "/cves/");
        allTime();
        openAll();
        int high = visibleMarks();
        int rows = visibleRows();
        double height = plotHeight();
        assertTrue(high > 0);
        assertEquals(0, page.locator(MARKS + ".tl-sev--medium, " + MARKS + ".tl-sev--low").count());

        allSeverities();
        int all = visibleMarks();
        assertTrue(all > high);
        assertTrue(chip("severity", "low").isChecked());

        onlySeverity("critical");
        int critical = visibleMarks();
        assertTrue(critical > 0 && critical < high);
        assertEquals(critical, page.locator(MARKS + ".tl-sev--critical").count(), "Only critical marks are left");
        assertTrue(visibleRows() < rows, "Rows with no critical CVE are hidden");
        assertTrue(plotHeight() < height, "The plot shrinks, rather than leaving gaps");

        // not in a run, as the slider could not: medium and critical, but not high
        setChip("severity", "medium", true);
        assertEquals(0, page.locator(MARKS + ".tl-sev--high").count());
        assertTrue(page.locator(MARKS + ".tl-sev--medium").count() > 0);

        page.locator("[data-cve-severity-shortcut=high]").click();
        assertTrue(chip("severity", "high").isChecked());
        assertFalse(chip("severity", "medium").isChecked());
        assertEquals(high, visibleMarks());
        assertEquals(rows, visibleRows());
        assertEquals(height, plotHeight(), 0.5, "The shortcut restores the layout");
    }

    @Test
    void countsOnTheChipsFollowTheOtherFilters() {
        page.navigate(baseUrl + "/cves/");
        allTime();
        int critical = count("severity", "critical");
        int fixed = count("status", "fixed");
        int external = count("origin", "external");
        assertTrue(critical > 0 && fixed > 0 && external > 0);

        // a filter does not change its own counts, but changes the others'
        setChip("status", "fixed", false);
        assertEquals(fixed, count("status", "fixed"), "An off chip says what it would add");
        assertTrue(count("severity", "critical") <= critical);
        setChip("status", "fixed", true);
        assertEquals(critical, count("severity", "critical"));

        preset(3);
        assertTrue(count("status", "fixed") < fixed, "The time period changes the counts");
        assertEquals(count("origin", "internal") + count("origin", "external") >= count("status", "open"), true);
    }

    @Test
    void statusAndOriginHaveARingWithASliceForEachChipThatActsAsASwitch() {
        page.navigate(baseUrl + "/cves/");
        allSeverities();
        allTime();
        assertEquals(3, page.locator("[data-cve-donut=status] .cve-slice").count());
        assertEquals(2, page.locator("[data-cve-donut=origin] .cve-slice").count());
        assertTrue(page.locator("[data-cve-donut=status] .cve-slice--fixed").getAttribute("d").startsWith("M"), "The biggest slice is drawn");
        assertEquals(0, page.locator("[data-cve-donut=status] .cve-slice.is-off").count());

        page.locator("[data-cve-donut=origin] .cve-slice--external").dispatchEvent("click");
        assertFalse(chip("origin", "external").isChecked(), "Clicking a slice turns that value off");
        assertEquals(1, page.locator("[data-cve-donut=origin] .cve-slice.is-off").count());
        assertEquals(0, page.locator(CATS + "[data-origin=external]").count());
        chip("origin", "external").evaluate("e => e.closest('label').click()");
        assertEquals(0, page.locator("[data-cve-donut=origin] .cve-slice.is-off").count());
    }

    @Test
    void notFixedYetKeepsJustTheCrossedBars() {
        page.navigate(baseUrl + "/cves/");
        showEverything();
        int all = visibleMarks();
        setChip("status", "fixed", false);
        int unresolved = visibleMarks();
        assertTrue(unresolved > 0 && unresolved < all);
        assertEquals(unresolved, page.locator(MARKS + " .tl-cross").count(), "Every unresolved mark ends in a cross");
        assertTrue(page.locator(".cve-slider-status").textContent().contains(" of "), "The count should be shown");
        setChip("status", "pending", false);
        assertTrue(visibleMarks() < unresolved, "Awaiting assessment is a status of its own");
        assertEquals(0, page.locator(MARKS + ".tl-mark--pending").count());
    }

    // ---- no script

    @Test
    void theControlsAreHiddenWithoutScriptAndTheRowsAreLaidOutAsTheScriptWould() {
        // what the server renders for each component must be what the script would lay out
        try (BrowserContext plain = browser.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false))) {
            Page server = plain.newPage();
            server.navigate(baseUrl + "/cves/");
            assertTrue(server.locator(".cve-filters").getAttribute("hidden") != null, "Controls do nothing without script");
            String serverStart = server.locator("#tl-exposure").getAttribute("data-range-start");
            String serverHeight = server.locator("#tl-exposure .tl-plot").getAttribute("height");
            int rows = server.locator(ROWS).count();
            String[] serverY = new String[Math.min(rows, 6)];
            String[] serverX = new String[serverY.length];
            for (int i = 0; i < serverY.length; i++) {
                Locator post = server.locator(ROWS).nth(i).locator(".tl-mark-post").first();
                serverY[i] = post.getAttribute("y");
                serverX[i] = post.getAttribute("x");
            }
            String serverTicks = String.join(",", server.locator("#tl-exposure .tl-grid text").allTextContents());

            page.navigate(baseUrl + "/cves/");
            showEverything(); // the server lays out all of the data; the script does the same for the same controls
            assertEquals(serverStart, page.locator("#tl-exposure").getAttribute("data-range-start"));
            assertTrue(Double.parseDouble(page.locator("#tl-exposure .tl-plot").getAttribute("height")) > Double.parseDouble(serverHeight),
                    "the script adds the category rows to the height");
            assertEquals(rows, visibleRows(), "rows shown");
            assertEquals(serverTicks, String.join(",", page.locator("#tl-exposure .tl-grid text").allTextContents()), "axis labels");
            for (int i = 0; i < serverY.length; i++) {
                Locator post = page.locator(ROWS).nth(i).locator(".tl-mark-post").first();
                assertEquals(serverY[i], post.getAttribute("y"), "y of the first bar in row " + i);
                assertEquals(serverX[i], post.getAttribute("x"), "x of the first bar in row " + i);
            }
        }
    }

    // ---- helpers

    /** Sets a slider and sends the event the script listens for, as dragging it would. */
    private void setSlider(String id, int value) {
        page.evaluate("([id, value]) => { const e = document.getElementById(id); e.value = value;"
                + " e.dispatchEvent(new Event('input', { bubbles: true })); }", new Object[] { id, value });
    }

    private String query() {
        return (String) page.evaluate("() => window.location.search.replace(/^\\?/, '')");
    }

    /** Opens every category, and the lines that fold the components with one CVE. */
    private void openAll() {
        page.locator("[data-cve-expand=all]").click();
    }

    /** Every severity, the whole history, and every category open: what the page used to start with. */
    private void showEverything() {
        allSeverities();
        allTime();
        openAll();
    }

    private void allSeverities() {
        page.locator("[data-cve-severity-shortcut=all]").click();
    }

    private Locator chip(String filter, String value) {
        return page.locator("[data-cve-" + filter + "=" + value + "]");
    }

    /** Turns a chip on or off as a click on it would, if it is not already as wanted. */
    private void setChip(String filter, String value, boolean on) {
        if (chip(filter, value).isChecked() != on) {
            chip(filter, value).evaluate("e => e.closest('label').click()");
        }
    }

    /** Only the one severity. */
    private void onlySeverity(String severity) {
        for (String sev : new String[] { "critical", "high", "medium", "low" }) {
            setChip("severity", sev, sev.equals(severity));
        }
    }

    private int count(String filter, String value) {
        return Integer.parseInt(page.locator(".cve-set:has([data-cve-" + filter + "=" + value + "]) label:has([data-cve-"
                + filter + "=" + value + "]) .cve-chip-count").textContent());
    }

    private void allTime() {
        preset(0);
    }

    /** A preset of the time period: that many months to now, or 0 for all of it. */
    private void preset(int months) {
        page.locator("[data-cve-preset='" + months + "']").click();
    }

    /** Every category open, as the chart is mostly looked at. */
    private void openAllHigh() {
        openAll();
    }

    /** The first hover spreads the row's lanes out, which moves the bars, so aim again once it has. */
    private void hoverSteadily(Locator target) {
        var box = target.boundingBox();
        page.mouse().move(box.x + box.width / 2, box.y + box.height / 2);
        page.waitForTimeout(150);
        box = target.boundingBox();
        page.mouse().move(box.x + box.width / 2 + 1, box.y + box.height / 2);
    }

    private int visibleMarks() {
        return page.locator(MARKS).count();
    }

    private int visibleRows() {
        return page.locator(ROWS).count();
    }

    private double plotHeight() {
        return ((Number) page.evaluate("() => document.querySelector('#tl-exposure .tl-plot').getBoundingClientRect().height")).doubleValue();
    }

    /** Vertical distance between the neighbouring lanes of the first row. */
    private int laneSpacing() {
        Object spacing = page.evaluate("() => { const ys = [...new Set([...document.querySelector('#tl-exposure .tl-row:not([display=none])')"
                + ".querySelectorAll('.tl-mark-post')].map(e => +e.getAttribute('y')))].sort((a, b) => a - b);"
                + " return ys[1] - ys[0]; }");
        return ((Number) spacing).intValue();
    }
}
