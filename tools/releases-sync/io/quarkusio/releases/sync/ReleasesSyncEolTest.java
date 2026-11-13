///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.eclipse.microprofile.config:microprofile-config-api:3.1
//DEPS org.apache.maven:maven-artifact:3.9.9
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.2
//DEPS org.junit.jupiter:junit-jupiter:5.11.4
//DEPS org.junit.platform:junit-platform-launcher:1.11.4
//SOURCES ../../../../test/stubs/io/quarkus/logging/Log.java
//SOURCES ../../../quarkiverse/roq/data/runtime/annotations/DataMapping.java
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/Releases.java
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/Release.java
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/EnterpriseOffering.java
//SOURCES WikiReleasePlanning.java
//SOURCES EnterpriseReleaseDates.java
//SOURCES BranchLineage.java
//SOURCES GitHubReleases.java
//SOURCES ReleasesSync.java

package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import io.quarkusio.releases.Releases;

/**
 * Run from the tools/releases-sync directory with {@code jbang io/quarkusio/releases/sync/ReleasesSyncEolTest.java}.
 * Logging is stubbed (test/stubs), so it needs no Quarkus application. Uses made-up releases (fake versions 98.x, 99.x and fake dates).
 */
public class ReleasesSyncEolTest {

    private static Releases.EnterpriseDates dates(String release, String eol, boolean last) {
        return new Releases.EnterpriseDates(release, eol, null, false, last);
    }

    private static Releases.Entry entry(String version, Releases.EnterpriseDates rhbq, Releases.EnterpriseDates ibm) {
        return new Releases.Entry(version, "2080-01-01", null, null, true, false, false, null, null, null, rhbq, ibm,
                null, null);
    }

    private static Releases releases(Releases.Entry... entries) {
        return new Releases(null, null, List.of(), List.of(entries));
    }

    private static Releases.Entry find(Releases data, String version) {
        return data.releases().stream().filter(e -> e.version().equals(version)).findFirst().get();
    }

    @Test
    void fillsMissingEndDatesAndKeepsExistingOnesWhereNothingLaterFixesThem() {
        Releases data = releases(
                entry("98.1", dates("2080-02-01", null, false), dates("2080-03-01", null, false)),
                entry("98.2", dates("2081-02-01", "2090-01-01", false), null));
        Releases result = ReleasesSync.applyEnterpriseEolDates(data);
        // Red Hat: six months after the next minor (98.2, 2081-02-01); IBM: a year from its own release
        assertEquals("2081-08-01", find(result, "98.1").rhbq().eolDate());
        assertEquals("2081-03-01", find(result, "98.1").ibm().eolDate());
        // an existing date is not overwritten, even when it disagrees (that is only warned about)
        assertEquals("2090-01-01", find(result, "98.2").rhbq().eolDate());
    }

    @Test
    void marksTheLastMinorOnceANewMajorArrivesAndEndsItSixMonthsAfterThatMajor() {
        Releases data = releases(
                entry("99.0", dates("2083-08-31", null, false), null),
                entry("98.5", dates("2081-01-01", "2082-06-30", false), null),
                entry("98.1", dates("2080-01-10", "2081-01-10", false), null));
        Releases result = ReleasesSync.applyEnterpriseEolDates(data);
        assertTrue(find(result, "98.5").rhbq().lastInMajor());
        assertFalse(find(result, "98.1").rhbq().lastInMajor());
        assertFalse(find(result, "99.0").rhbq().lastInMajor());
        // the hand-edited placeholder is replaced: 99.0 arrived after the 3 years from 98.1 ended
        assertEquals("2084-02-29", find(result, "98.5").rhbq().eolDate());
        // 98.1 isn't last: six months after the next minor, 98.5 (2081-01-01), replacing the recorded date
        assertEquals("2081-07-01", find(result, "98.1").rhbq().eolDate());
        // the newest major has no successor yet: its minor isn't marked, so it gets the one year of any other
        assertEquals("2084-08-31", find(result, "99.0").rhbq().eolDate());
    }

    @Test
    void redHatLastMinorKeepsItsPlaceholderUntilTheNextMajorArrives() {
        Releases data = releases(
                entry("98.5", dates("2081-01-01", "2082-06-30", true), dates("2081-02-01", null, true)),
                entry("98.1", dates("2080-01-10", "2081-01-10", false), dates("2080-02-01", null, false)));
        Releases result = ReleasesSync.applyEnterpriseEolDates(data);
        assertEquals("2082-06-30", find(result, "98.5").rhbq().eolDate());
        // IBM's can be computed without waiting: 5 years from its first release in the stream
        assertEquals("2085-02-01", find(result, "98.5").ibm().eolDate());
    }

    @Test
    void aPublishedDateLaterThanTheFloorIsNotShortened() {
        // 99.0 arrived early enough that the six months end well before the recorded date
        Releases data = releases(
                entry("99.0", dates("2081-06-01", null, false), null),
                entry("98.5", dates("2081-01-01", "2090-09-30", true), null),
                entry("98.1", dates("2080-01-10", null, false), null));
        Releases result = ReleasesSync.applyEnterpriseEolDates(data);
        assertEquals("2090-09-30", find(result, "98.5").rhbq().eolDate());
        // but one that is too early is lengthened to six months after 99.0
        Releases early = releases(
                entry("99.0", dates("2083-08-31", null, false), null),
                entry("98.5", dates("2081-01-01", "2082-09-30", true), null),
                entry("98.1", dates("2080-01-10", null, false), null));
        assertEquals("2084-02-29", find(ReleasesSync.applyEnterpriseEolDates(early), "98.5").rhbq().eolDate());
    }

    @Test
    void runningTwiceChangesNothing() {
        Releases data = releases(
                entry("99.0", dates("2083-08-31", null, false), null),
                entry("98.5", dates("2081-01-01", "2082-06-30", false), null),
                entry("98.1", dates("2080-01-10", null, false), null));
        Releases once = ReleasesSync.applyEnterpriseEolDates(data);
        assertEquals(once, ReleasesSync.applyEnterpriseEolDates(once));
    }

    public static void main(String[] args) {
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(
                LauncherDiscoveryRequestBuilder.request().selectors(selectClass(ReleasesSyncEolTest.class)).build(),
                listener);
        listener.getSummary().printTo(new PrintWriter(System.out));
        listener.getSummary().printFailuresTo(new PrintWriter(System.out), 15);
        System.exit(listener.getSummary().getTotalFailureCount() == 0 ? 0 : 1);
    }
}
