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
        setSlider("cve-min-count", 1);
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
        setSlider("cve-min-count", 1);
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
        setSlider("cve-min-count", 1);
        page.locator("#cve-unresolved").check();
        page.locator("[data-timeline-toggle=discovered]").click();
        Object everyMarkOpen = page.locator("#tl-discovered .tl-mark:not([display=none])")
                .evaluateAll("marks => marks.length > 0 && marks.every(m => m.classList.contains('tl-mark--open'))");
        assertEquals(Boolean.TRUE, everyMarkOpen);
        assertEquals(page.locator("#tl-exposure .tl-row:not([display=none])").count(),
                page.locator("#tl-discovered .tl-row:not([display=none])").count());
    }

    // ---- the ring

    @Test
    void ringHasOneSliceForEachComponentLargestFirst() {
        page.navigate(baseUrl + "/cves/");
        int slices = page.locator(".pie-slice").count();
        assertEquals(page.locator("#tl-exposure .tl-labels li").count(), slices);
        Object descending = page.locator(".pie-slice").evaluateAll(
                "els => els.map(e => +e.dataset.value).every((v, i, a) => i === 0 || a[i - 1] >= v)");
        assertEquals(Boolean.TRUE, descending);
    }

    @Test
    void draggingTheHandleRoundTheRingWorksLikeTheSlider() {
        page.navigate(baseUrl + "/cves/");
        assertTrue(page.locator(".pie-handle").isVisible());
        page.locator(".pie-svg").scrollIntoViewIfNeeded();
        var box = page.locator(".pie-svg").boundingBox();
        double cx = box.x + box.width / 2, cy = box.y + box.height / 2, r = box.width * 0.33;
        int before = visibleRows();
        // a quarter of the way round, clockwise from the top: the biggest components only
        page.mouse().move(cx, cy - r);
        page.mouse().down();
        page.mouse().move(cx + r, cy, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(5));
        page.mouse().up();
        assertTrue(visibleRows() < before, "Fewer components with the handle part way round: " + before + " -> " + visibleRows() + ", min " + page.locator("#cve-min-count").inputValue() + ", handle " + page.locator(".pie-handle").getAttribute("transform") + ", stops " + page.locator(".pie").getAttribute("data-stops"));
        assertTrue(allCountsAtLeast(Integer.parseInt(page.locator("#cve-min-count").inputValue())));
        // all the way round shows everything
        page.mouse().move(cx + r, cy);
        page.mouse().down();
        page.mouse().move(cx - 1, cy - r, new com.microsoft.playwright.Mouse.MoveOptions().setSteps(10));
        page.mouse().up();
        assertEquals("1", page.locator("#cve-min-count").inputValue());
    }

    @Test
    void unresolvedOnlyPutsTheHandleBackToEverythingAndUntickingRestoresIt() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 9);
        String handleBefore = page.locator(".pie-handle").getAttribute("transform");
        page.locator("#cve-unresolved").check();
        assertEquals("1", page.locator("#cve-min-count").inputValue());
        assertNotEquals(handleBefore, page.locator(".pie-handle").getAttribute("transform"));
        page.locator("#cve-unresolved").uncheck();
        assertEquals("9", page.locator("#cve-min-count").inputValue());
        assertEquals(handleBefore, page.locator(".pie-handle").getAttribute("transform"));
    }

    @Test
    void switchingOffThirdPartyDependenciesLeavesOnlyQuarkusLibrariesLitAndFiltersBothPlots() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
        assertTrue(page.locator("[data-cve-origin=external]").isChecked(), "All switches start on");
        page.locator("[data-cve-origin=external]").uncheck();
        int lit = page.locator(".pie-slice:not(.is-off)").count();
        assertTrue(lit >= 1 && lit < page.locator(".pie-slice").count());
        assertEquals(lit, page.locator(".pie-slice[data-origin=internal]").count());
        assertEquals(lit, page.locator("#tl-exposure .tl-labels li:not([hidden])").count());
        assertEquals(lit, page.locator("#tl-discovered .tl-labels li:not([hidden])").count());
        page.locator("[data-cve-origin=external]").check();
        assertEquals(0, page.locator(".pie-slice.is-off").count());
    }

    @Test
    void selectNoneAndSelectAllWorkOnEachSetOfSwitches() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
        page.locator("[data-cve-set=category][data-cve-select=none]").click();
        assertEquals(0, page.locator("[data-cve-category]:checked").count());
        assertEquals(page.locator(".pie-slice").count(), page.locator(".pie-slice.is-off").count());
        assertEquals(0, page.locator("#tl-exposure .tl-labels li:not([hidden])").count());
        // the other set is not touched
        assertEquals(2, page.locator("[data-cve-origin]:checked").count());
        page.locator("[data-cve-set=category][data-cve-select=all]").click();
        assertEquals(page.locator("[data-cve-category]").count(), page.locator("[data-cve-category]:checked").count());
        assertEquals(0, page.locator(".pie-slice.is-off").count());
        page.locator("[data-cve-set=origin][data-cve-select=none]").click();
        assertEquals(0, page.locator("[data-cve-origin]:checked").count());
        assertEquals(page.locator("[data-cve-category]").count(), page.locator("[data-cve-category]:checked").count());
    }

    @Test
    void aCategorySwitchedOnAloneLightsUpOnlyItsComponents() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
        page.locator("[data-cve-set=category][data-cve-select=none]").click();
        page.locator("[data-cve-category=Security]").check();
        int lit = page.locator(".pie-slice:not(.is-off)").count();
        assertTrue(lit >= 1 && lit < page.locator(".pie-slice").count());
        assertEquals(lit, page.locator(".pie-slice[data-category=Security]").count());
        assertEquals(lit, page.locator("#tl-exposure .tl-labels li:not([hidden])").count());
    }

    @Test
    void hoveringASwitchLightsUpItsComponentsInTheRingEvenWhenItIsOff() {
        page.navigate(baseUrl + "/cves/");
        page.locator("[data-cve-set=category][data-cve-select=none]").click();
        Locator security = page.locator(".cve-switch:has([data-cve-category=Security])");
        security.scrollIntoViewIfNeeded();
        security.hover();
        int lit = page.locator(".pie-slice.is-hover").count();
        assertTrue(lit >= 1);
        assertEquals(page.locator(".pie-slice[data-category=Security]").count(), lit);
        assertEquals(lit, page.locator(".pie-slice.is-hover[data-category=Security]").count());
        assertTrue(page.locator(".pie.is-previewing").count() == 1);
        // the same goes for the origin switches
        page.locator(".cve-switch:has([data-cve-origin=internal])").hover();
        assertEquals(page.locator(".pie-slice[data-origin=internal]").count(), page.locator(".pie-slice.is-hover").count());
        page.mouse().move(0, 0);
        assertEquals(0, page.locator(".pie-slice.is-hover").count());
        assertEquals(0, page.locator(".pie.is-previewing").count());
    }

    @Test
    void unresolvedOnlyIsASwitch() {
        page.navigate(baseUrl + "/cves/");
        assertEquals("switch", page.locator("#cve-unresolved").getAttribute("role"));
        assertFalse(page.locator("#cve-unresolved").isChecked());
    }

    @Test
    void sliceColoursFollowOriginAndGetDarkerWithMoreCves() {
        page.navigate(baseUrl + "/cves/");
        assertEquals("Quarkus libraries", page.locator(".cve-switch:has([data-cve-origin=internal])").textContent().trim());
        assertEquals("Third-party dependencies", page.locator(".cve-switch:has([data-cve-origin=external])").textContent().trim());
        // the biggest slice is the darkest and the smallest the palest
        assertTrue(page.locator(".pie-slice").first().getAttribute("class").contains("pie-shade--4"));
        assertTrue(page.locator(".pie-slice").last().getAttribute("class").contains("pie-shade--0"));
        String internal = page.locator(".pie-slice[data-origin=internal]").first()
                .evaluate("e => getComputedStyle(e).getPropertyValue('--pie-base').trim()").toString();
        String external = page.locator(".pie-slice[data-origin=external]").first()
                .evaluate("e => getComputedStyle(e).getPropertyValue('--pie-base').trim()").toString();
        assertNotEquals(internal, external);
        assertEquals("#8c8c8c", external);
    }

    @Test
    void tooltipStaysInsideTheWindowEvenForAMarkNearTheBottom() {
        page.setViewportSize(1280, 500);
        page.navigate(baseUrl + "/cves/");
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
        String href = page.locator(MARKS).first().getAttribute("href");
        assertTrue(href.startsWith("https://www.cve.org/") || href.startsWith("https://github.com/advisories/"), href);
    }

    @Test
    void overlappingCvesOfOneArtifactAreInSeparateLanes() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
        assertTrue(page.locator("#tl-exposure .tl-labels li.tl-h-24").count() < page.locator("#tl-exposure .tl-labels li").count(),
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
            setSlider("cve-min-count", 1); // moves the axis, so everything is placed again
        }
        // and not every bar has one: those that arrived in a later release start where they started
        assertTrue(page.locator("#tl-exposure .tl-mark:not(:has(.tl-more)):has(.tl-mark-pre)").count() >= 1);
    }

    @Test
    void tooltipSaysSinceAtLeastForThoseBars() {
        page.navigate(baseUrl + "/cves/");
        Locator mark = page.locator(MARKS + ":has(.tl-more)").first();
        mark.scrollIntoViewIfNeeded();
        hoverSteadily(mark.locator("rect.tl-hit"));
        String text = page.locator(".tl-tooltip").textContent();
        assertTrue(text.contains("since at least") && text.contains("the earliest we have data for"), text);
        assertTrue(text.contains("(3.27)") || text.matches(".*\\(\\d+\\.\\d+\\).*"), "It names the oldest release line: " + text);
    }

    // ---- the legend

    @Test
    void severityColoursAndBarKindsAreInSeparateLegendRows() {
        page.navigate(baseUrl + "/cves/");
        Locator colours = page.locator(".tl-legend[aria-label='Severity colours']");
        assertEquals(4, colours.locator("li").count(), "One entry per severity");
        assertEquals(0, colours.locator(".tl-swatch--span, .tl-swatch--open, .tl-swatch--pending").count(),
                "Bar kinds do not belong in the colour row");
        Locator kinds = page.locator(".tl-legend:not([aria-label])");
        assertEquals(4, kinds.locator("li").count(), "Fixed, since at least, not fixed yet and pending");
        assertEquals(1, kinds.locator(".tl-swatch--since").count(), "The arrow is explained in the bar kinds row");
        assertEquals(0, kinds.locator(".tl-swatch--critical, .tl-swatch--high, .tl-swatch--medium, .tl-swatch--low").count());
        assertTrue(kinds.locator(".tl-legend-desc").first().textContent().length() > 10, "Bar kinds have descriptions");
        assertFalse(page.locator(".tl-legend--providers").isVisible(), "No provider row on this page");
    }

    @Test
    void severityLegendSwatchesHaveDistinctColours() {
        page.navigate(baseUrl + "/cves/");
        Object count = page.evaluate("() => new Set(['critical','high','medium','low'].map(s => "
                + "getComputedStyle(document.querySelector('.tl-legend .tl-swatch--' + s)).backgroundColor)).size");
        assertEquals(4, ((Number) count).intValue(), "Each severity needs its own colour in the legend");
    }

    // ---- hovering

    @Test
    void hoveringAMarkShowsTheCveInATooltip() {
        page.navigate(baseUrl + "/cves/");
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
        Locator mark = page.locator(MARKS + ":not(.tl-mark--pending):has(.tl-mark-pre)").first();
        mark.scrollIntoViewIfNeeded();
        hoverSteadily(mark.locator(".tl-mark-pre"));
        assertTrue(page.locator(".tl-tooltip").isVisible(), "Tooltip should show over the pale part");
        assertTrue(page.locator(".tl-tooltip").textContent().contains(mark.getAttribute("data-tooltip-title")));
    }

    @Test
    void tooltipKeepsLongUnbrokenTextInsideItsBox() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
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
    void lanesAreTightByDefaultAndSpreadOutWhileHoveringAComponent() {
        page.navigate(baseUrl + "/cves/");
        Locator first = page.locator(ROWS).first();
        double tight = plotHeight();
        assertEquals(7, laneSpacing(), "Neighbouring lanes are one bar height plus 1px apart");

        first.scrollIntoViewIfNeeded();
        var label = page.locator("#tl-exposure .tl-labels li:not([hidden])").first().boundingBox();
        page.mouse().move(label.x + 5, label.y + label.height / 2);
        page.waitForTimeout(100);
        assertTrue(plotHeight() > tight, "Hovering a component should spread its lanes out");
        assertEquals(11, laneSpacing());

        // between the bars of the hovered row still counts as hovering it
        var plotBox = first.boundingBox();
        page.mouse().move(plotBox.x + plotBox.width / 2, plotBox.y + 1);
        page.waitForTimeout(100);
        assertTrue(plotHeight() > tight, "The row stays spread out while the pointer is within it");

        page.mouse().move(0, 0);
        page.waitForTimeout(150);
        assertEquals(tight, plotHeight(), 0.5, "Lanes close again when the pointer leaves");
    }

    // ---- the sliders

    @Test
    void sliderStartsAtFiveAndShowsMoreComponentsWhenMovedDown() {
        page.navigate(baseUrl + "/cves/");
        assertEquals("5", page.locator("#cve-min-count").inputValue());
        assertTrue(page.locator("#cve-min-count-value").textContent().contains("at least 5 CVEs"));
        int five = visibleRows();
        assertTrue(five > 0);
        assertTrue(allCountsAtLeast(5), "Only components with at least 5 CVEs are shown at first");

        setSlider("cve-min-count", 1);
        assertTrue(visibleRows() > five, "Moving the slider down brings in more components");
        assertEquals("at least 1 CVE", page.locator("#cve-min-count-value").textContent());

        setSlider("cve-min-count", maxCount());
        assertTrue(visibleRows() < five, "Moving it all the way up leaves just the busiest");
        assertTrue(visibleRows() >= 1);
    }

    @Test
    void sliderNamesAndHighlightsTheComponentsItBringsIn() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 4);
        String status = page.locator(".cve-slider-status").textContent();
        assertTrue(status.startsWith("Bringing in "), status);
        // every component that came in has exactly 4 CVEs, and is named and highlighted
        Locator entering = page.locator("#tl-exposure .tl-labels li[data-count='4']:not([hidden])");
        assertTrue(entering.count() >= 1);
        for (int i = 0; i < entering.count(); i++) {
            String name = entering.nth(i).getAttribute("data-row");
            if (i < 3) {
                assertTrue(status.contains(name + " (4)"), "Status should name " + name + ": " + status);
            }
            assertTrue(entering.nth(i).getAttribute("class").contains("is-new"), name + " should be highlighted");
        }
        assertEquals(0, page.locator("#tl-exposure .tl-labels li.is-new:not([data-count='4'])").count(),
                "Only what was brought in is highlighted");

        setSlider("cve-min-count", 5);
        assertTrue(page.locator(".cve-slider-status").textContent().startsWith("Taking out "),
                page.locator(".cve-slider-status").textContent());
    }

    @Test
    void timeSpanFollowsTheComponentsShown() {
        page.navigate(baseUrl + "/cves/");
        String five = page.locator("#tl-exposure").getAttribute("data-range-start");
        String firstTick = page.locator("#tl-exposure .tl-grid text").first().textContent();
        setSlider("cve-min-count", 1);
        String one = page.locator("#tl-exposure").getAttribute("data-range-start");
        assertTrue(one.compareTo(five) < 0, "Bringing in older CVEs should start the axis earlier: " + one + " vs " + five);
        assertNotEquals(firstTick, page.locator("#tl-exposure .tl-grid text").first().textContent(), "The axis labels follow the span");
        assertEquals(page.locator("#tl-exposure").getAttribute("data-range-end").substring(0, 4),
                page.locator("#tl-exposure .tl-grid text").last().textContent().replaceAll(".* ", "").substring(0, 4).replace("2027", "2027"),
                "The end of the axis does not move");
        setSlider("cve-min-count", 5);
        assertEquals(five, page.locator("#tl-exposure").getAttribute("data-range-start"), "and it goes back again");
    }

    @Test
    void severitySliderShowsThatSeverityAndAboveAndItsTrackIsColoured() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
        int all = visibleMarks();
        int rows = visibleRows();
        double height = plotHeight();
        assertEquals("All severities", page.locator("#cve-min-severity-value").textContent());

        setSlider("cve-min-severity", 3); // high and above
        assertEquals("High and above", page.locator("#cve-min-severity-value").textContent());
        int high = visibleMarks();
        assertTrue(high > 0 && high < all);
        assertEquals(0, page.locator(MARKS + ".tl-sev--medium, " + MARKS + ".tl-sev--low").count());
        assertTrue(page.locator(MARKS + ".tl-sev--high").count() > 0);

        setSlider("cve-min-severity", 4); // critical only
        assertEquals("Critical only", page.locator("#cve-min-severity-value").textContent());
        int critical = visibleMarks();
        assertTrue(critical > 0 && critical < high);
        assertEquals(critical, page.locator(MARKS + ".tl-sev--critical").count(), "Only critical marks are left");
        assertTrue(visibleRows() < rows, "Rows with no critical CVE are hidden");
        assertTrue(plotHeight() < height, "The plot shrinks, rather than leaving gaps");
        assertEquals("100%", page.locator("#cve-min-severity").evaluate("e => e.style.getPropertyValue('--p')"),
                "The greyed-out part of the track covers everything below the thumb");

        setSlider("cve-min-severity", 1);
        assertEquals(all, visibleMarks());
        assertEquals(rows, visibleRows());
        assertEquals(height, plotHeight(), 0.5, "Putting the slider back restores the original layout");
    }

    @Test
    void severitySliderTrackUsesTheSeverityColours() {
        page.navigate(baseUrl + "/cves/");
        String background = (String) page.locator("#cve-min-severity").evaluate("e => getComputedStyle(e).backgroundImage");
        for (String colour : new String[] { "rgb(179, 32, 42)", "rgb(241, 143, 1)", "rgb(240, 202, 77)", "rgb(91, 143, 201)" }) {
            assertTrue(background.contains(colour), "The track should have " + colour + " in it: " + background);
        }
    }

    @Test
    void unresolvedOnlyKeepsJustTheCrossedBars() {
        page.navigate(baseUrl + "/cves/");
        setSlider("cve-min-count", 1);
        int all = visibleMarks();
        page.locator("#cve-unresolved").check();
        int unresolved = visibleMarks();
        assertTrue(unresolved > 0 && unresolved < all);
        assertEquals(unresolved, page.locator(MARKS + " .tl-cross").count(), "Every unresolved mark ends in a cross");
        assertTrue(page.locator(".cve-slider-status").textContent().contains(" of "), "The count should be shown");
    }

    // ---- no script

    @Test
    void theControlsAreHiddenWithoutScriptAndThePageIsLaidOutForTheSlidersStart() {
        // what the server renders must be what the script would lay out, or the page jumps when it loads
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
            assertEquals(serverStart, page.locator("#tl-exposure").getAttribute("data-range-start"));
            assertEquals(serverHeight, page.locator("#tl-exposure .tl-plot").getAttribute("height"), "plot height");
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

    private int maxCount() {
        return Integer.parseInt(page.locator("#cve-min-count").getAttribute("max"));
    }

    private boolean allCountsAtLeast(int count) {
        return page.locator("#tl-exposure .tl-labels li:not([hidden])").evaluateAll(
                "(items, min) => items.every(li => parseInt(li.getAttribute('data-count'), 10) >= min)", count) == Boolean.TRUE;
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
