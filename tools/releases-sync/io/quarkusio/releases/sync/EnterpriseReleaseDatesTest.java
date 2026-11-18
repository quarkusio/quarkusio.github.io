///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.jupiter:junit-jupiter:5.11.4
//DEPS org.junit.platform:junit-platform-launcher:1.11.4
//SOURCES EnterpriseReleaseDates.java

package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/**
 * Run from this directory with {@code jbang EnterpriseReleaseDatesTest.java}.
 * Uses made-up repository listings in test/ (fake versions 98.x and fake dates).
 */
public class EnterpriseReleaseDatesTest {

    private static EnterpriseReleaseDates.Build build(int y, int m, int d, boolean last) {
        return new EnterpriseReleaseDates.Build(LocalDate.of(y, m, d), last);
    }

    @Test
    void eolDateFollowsSupportPolicy() {
        Map<String, EnterpriseReleaseDates.Build> builds = Map.of(
                "97.1", build(2080, 1, 10, true), "98.1", build(2080, 6, 1, false),
                "98.2", build(2081, 2, 1, false), "98.5", build(2082, 3, 5, true));
        // not the last of its stream: a year from its own release
        assertEquals(LocalDate.of(2081, 6, 1), EnterpriseReleaseDates.computeEolDate("ibm", "98.1", builds).get());
        // Red Hat: six months after the next minor of the stream (98.5, 2082-03-05), not a year from its own release
        assertEquals(LocalDate.of(2082, 9, 5), EnterpriseReleaseDates.computeEolDate("rhbq", "98.2", builds).get());
        assertEquals(LocalDate.of(2081, 8, 1), EnterpriseReleaseDates.computeEolDate("rhbq", "98.1", builds).get());
        // with no next minor yet, a year from its own release is a placeholder
        assertEquals(LocalDate.of(2081, 2, 1), EnterpriseReleaseDates.computeEolDate("rhbq", "98.9",
                Map.of("98.9", build(2080, 2, 1, false))).get());
        // the last of its stream: 5 years from IBM's first release in that stream
        assertEquals(LocalDate.of(2085, 6, 1), EnterpriseReleaseDates.computeEolDate("ibm", "98.5", builds).get());
        assertEquals(LocalDate.of(2085, 1, 10), EnterpriseReleaseDates.computeEolDate("ibm", "97.1", builds).get());
        // Red Hat's last minor can't be computed until the next major has a build; unknown versions neither
        assertTrue(EnterpriseReleaseDates.computeEolDate("rhbq", "98.5", builds).isEmpty());
        assertTrue(EnterpriseReleaseDates.computeEolDate("ibm", "98.9", builds).isEmpty());
    }

    @Test
    void fullSupportEndsWhenTheNextMajorIsReleasedAndMaintenanceLastsSixMonthsMore() {
        // 98's first Red Hat release is 2080-01-10, so 3 years is 2083-01-10
        // The next major arrives long after that: its release plus six months is the end
        Map<String, EnterpriseReleaseDates.Build> late = Map.of(
                "98.1", build(2080, 1, 10, false), "98.5", build(2081, 1, 1, true), "99.0", build(2083, 8, 31, false));
        assertEquals(LocalDate.of(2084, 2, 29), EnterpriseReleaseDates.computeEolDate("rhbq", "98.5", late).get());
        // Arriving early: the 3 year life from the stream's start still holds
        Map<String, EnterpriseReleaseDates.Build> early = Map.of(
                "98.1", build(2080, 1, 10, false), "98.5", build(2081, 1, 1, true), "99.0", build(2081, 6, 1, false));
        assertEquals(LocalDate.of(2083, 1, 10), EnterpriseReleaseDates.computeEolDate("rhbq", "98.5", early).get());
        // Exactly six months after the next major is as late as the minimum allows
        Map<String, EnterpriseReleaseDates.Build> edge = Map.of(
                "98.1", build(2080, 1, 10, false), "98.5", build(2081, 1, 1, true), "99.0", build(2082, 7, 10, false));
        assertEquals(LocalDate.of(2083, 1, 10), EnterpriseReleaseDates.computeEolDate("rhbq", "98.5", edge).get());
        // The next major is the first one after this, not the newest
        Map<String, EnterpriseReleaseDates.Build> skipped = Map.of(
                "98.1", build(2080, 1, 10, false), "98.5", build(2081, 1, 1, true),
                "99.0", build(2083, 8, 31, false), "100.0", build(2090, 1, 1, false));
        assertEquals(LocalDate.of(2084, 2, 29), EnterpriseReleaseDates.computeEolDate("rhbq", "98.5", skipped).get());
        // The same minimum applies to IBM, whose 5 years usually exceeds it
        assertEquals(LocalDate.of(2085, 1, 10), EnterpriseReleaseDates.computeEolDate("ibm", "98.5", early).get());
        Map<String, EnterpriseReleaseDates.Build> ibmLate = Map.of(
                "98.1", build(2080, 1, 10, true), "99.0", build(2085, 1, 1, false));
        assertEquals(LocalDate.of(2085, 7, 1), EnterpriseReleaseDates.computeEolDate("ibm", "98.1", ibmLate).get());
    }

    @Test
    void recordedEndDateIsReconciledWithTheComputedOne() {
        LocalDate longer = LocalDate.of(2999, 1, 1);
        LocalDate shorter = LocalDate.of(2001, 1, 1);
        Map<String, EnterpriseReleaseDates.Build> stream = Map.of(
                "98.1", build(2080, 1, 10, false), "98.2", build(2080, 8, 1, false), "98.5", build(2081, 1, 1, true));

        // nothing recorded: the computed date
        assertEquals(LocalDate.of(2081, 2, 1), EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.1", stream, null).get());
        // a Red Hat minor with a successor: the successor's release fixes the date, longer or shorter than recorded
        assertEquals(LocalDate.of(2081, 2, 1), EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.1", stream, longer).get());
        assertEquals(LocalDate.of(2081, 2, 1), EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.1", stream, shorter).get());
        // Red Hat's last minor before the next major: nothing computed, so the placeholder stays; none, none
        assertEquals(longer, EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.5", stream, longer).get());
        assertTrue(EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.5", stream, null).isEmpty());
        // the newest minor has no successor yet: its recorded date is kept
        Map<String, EnterpriseReleaseDates.Build> newest = Map.of("98.1", build(2080, 1, 10, false));
        assertEquals(shorter, EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.1", newest, shorter).get());
        // IBM's minors aren't fixed by a successor: the recorded date is kept
        assertEquals(longer, EnterpriseReleaseDates.eolDateToRecord("ibm", "98.1", stream, longer).get());

        // The next major arrives: 98.5 ends no earlier than six months after it (2083-08-31 + 6 months)
        Map<String, EnterpriseReleaseDates.Build> withNextMajor = Map.of(
                "98.1", build(2080, 1, 10, false), "98.5", build(2081, 1, 1, true), "99.0", build(2083, 8, 31, false));
        LocalDate floor = LocalDate.of(2084, 2, 29);
        assertEquals(floor, EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.5", withNextMajor, null).get());
        // a placeholder that is too early is lengthened to the floor
        assertEquals(floor, EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.5", withNextMajor, shorter).get());
        // a date Red Hat published that is already later is never shortened
        assertEquals(longer, EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.5", withNextMajor, longer).get());
        assertEquals(floor, EnterpriseReleaseDates.eolDateToRecord("rhbq", "98.5", withNextMajor, floor).get());
        // and a longer recorded date isn't reported as a disagreement
        assertTrue(EnterpriseReleaseDates.checkEolDate("rhbq", "98.5", withNextMajor, longer).isEmpty());
        assertFalse(EnterpriseReleaseDates.checkEolDate("rhbq", "98.5", withNextMajor, shorter).isEmpty());
    }

    @Test
    void checkEolDateFlagsDisagreement() {
        Map<String, EnterpriseReleaseDates.Build> builds = Map.of(
                "98.1", build(2080, 6, 1, false), "98.2", build(2081, 2, 1, true));
        assertTrue(EnterpriseReleaseDates.checkEolDate("ibm", "98.1", builds, LocalDate.of(2081, 6, 1)).isEmpty());
        assertEquals("ibm eol_date 2083-01-01 for 98.1 should be 2081-06-01 by the support policy",
                EnterpriseReleaseDates.checkEolDate("ibm", "98.1", builds, LocalDate.of(2083, 1, 1)).get());
        assertTrue(EnterpriseReleaseDates.checkEolDate("rhbq", "98.2", builds, LocalDate.of(2083, 1, 1)).isEmpty());
    }

    @Test
    void lastInMajorIsMarkedOnceANewerMajorArrives() {
        // 98 has no newer major yet: nothing to mark
        assertTrue(EnterpriseReleaseDates.newlyLastInMajor(Map.of(
                "98.1", build(2080, 6, 1, false), "98.9", build(2081, 1, 1, false))).isEmpty());
        // 99 has arrived: the newest 98 minor is last (numerically: 98.10 is newer than 98.9)
        assertEquals(java.util.Set.of("98.10"), EnterpriseReleaseDates.newlyLastInMajor(Map.of(
                "98.9", build(2080, 6, 1, false), "98.10", build(2081, 1, 1, false), "99.0", build(2082, 1, 1, false))));
        // a stream with a hand-marked minor is left alone, even if it is not the newest
        assertTrue(EnterpriseReleaseDates.newlyLastInMajor(Map.of(
                "98.9", build(2080, 6, 1, true), "98.10", build(2081, 1, 1, false), "99.0", build(2082, 1, 1, false))).isEmpty());
    }

    @Test
    void minorOfIgnoresSuffixes() {
        assertEquals("3.27", EnterpriseReleaseDates.minorOf("3.27.1.SP1-redhat-00001"));
        assertEquals("2.13", EnterpriseReleaseDates.minorOf("2.13.5.Final-redhat-00002"));
        assertNull(EnterpriseReleaseDates.minorOf("latest"));
    }

    @Test
    void parsesIbmListingAndPicksEarliestBuildOfLowestMicro() throws Exception {
        Map<String, LocalDate> listing = EnterpriseReleaseDates.parseListing(
                Files.readString(Path.of("test/ibm-listing.html")));
        assertEquals(6, listing.size());
        Map<String, LocalDate> minors = EnterpriseReleaseDates.earliestDates(listing);
        assertEquals(LocalDate.of(2090, 1, 5), minors.get("98.1"));
        // 98.2 has no .0: the lowest micro is 1, and the earlier of its two builds wins
        assertEquals(LocalDate.of(2090, 4, 18), minors.get("98.2"));
        // 98.10 sorts after 98.2 numerically, not alphabetically
        assertEquals(List.of("98.1", "98.2", "98.10"), List.copyOf(minors.keySet()));
    }

    @Test
    void groupsMetadataVersionsByLowestMicro() throws Exception {
        List<String> versions = EnterpriseReleaseDates.parseMetadata(Files.readString(Path.of("test/maven-metadata.xml")));
        Map<String, List<String>> earliest = EnterpriseReleaseDates.earliestPerMinor(versions);
        assertEquals(List.of("98.1.0.SP1-redhat-00001", "98.1.0.redhat-00002"), earliest.get("98.1"));
        assertEquals(List.of("98.2.1.redhat-00002"), earliest.get("98.2"));
    }

    @Test
    void enterpriseDateMustNotBeBeforeCommunityDate() {
        LocalDate community = LocalDate.of(2090, 4, 10);
        assertTrue(EnterpriseReleaseDates.checkNotBeforeCommunity("rhbq", "98.2", community, LocalDate.of(2090, 4, 9)).isPresent());
        assertTrue(EnterpriseReleaseDates.checkNotBeforeCommunity("rhbq", "98.2", community, community).isEmpty());
        assertTrue(EnterpriseReleaseDates.checkNotBeforeCommunity("ibm", "98.2", community, LocalDate.of(2090, 5, 1)).isEmpty());
        // unknown dates can't be checked
        assertTrue(EnterpriseReleaseDates.checkNotBeforeCommunity("ibm", "98.2", null, LocalDate.of(2090, 5, 1)).isEmpty());
        assertTrue(EnterpriseReleaseDates.checkNotBeforeCommunity("ibm", "98.2", community, null).isEmpty());
        assertEquals("rhbq release_date 2090-04-09 for 98.2 is before the community release date 2090-04-10",
                EnterpriseReleaseDates.checkNotBeforeCommunity("rhbq", "98.2", community, LocalDate.of(2090, 4, 9)).get());
    }

    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(EnterpriseReleaseDatesTest.class)).build();
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);
        var summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.printFailuresTo(new PrintWriter(System.out));
        System.exit(summary.getTotalFailureCount() == 0 ? 0 : 1);
    }
}
