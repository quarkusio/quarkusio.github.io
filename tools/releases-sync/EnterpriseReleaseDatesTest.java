///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.jupiter:junit-jupiter:5.11.4
//DEPS org.junit.platform:junit-platform-launcher:1.11.4
//SOURCES EnterpriseReleaseDates.java

import static org.junit.jupiter.api.Assertions.assertEquals;
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
