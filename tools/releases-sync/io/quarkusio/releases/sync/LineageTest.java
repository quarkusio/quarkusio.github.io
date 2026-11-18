///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.junit.jupiter:junit-jupiter:5.11.4
//DEPS org.junit.platform:junit-platform-launcher:1.11.4
//DEPS org.apache.maven:maven-artifact:3.9.9
//DEPS org.eclipse.microprofile.config:microprofile-config-api:3.1
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/Releases.java
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/Release.java
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/Micro.java
//SOURCES ../../../../../../src/main/java/io/quarkusio/releases/EnterpriseOffering.java
//SOURCES ../../../../io/quarkiverse/roq/data/runtime/annotations/DataMapping.java
//SOURCES Lineage.java

package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import io.quarkusio.releases.Releases;

/**
 * Run from tools/releases-sync with {@code jbang io/quarkusio/releases/sync/LineageTest.java}.
 * Uses made-up versions (98.x and 99.x).
 */
public class LineageTest {

    static List<Releases.MicroEntry> micros(String... versions) {
        return Stream.of(versions).map(v -> Releases.MicroEntry.of(v, null, "https://github.com/x/" + v, null)).toList();
    }

    @Test
    void mainLineMicrosFollowEachOther() {
        Map<String, String> parents = Lineage.communityParents(micros("98.1.1", "98.1.0", "98.1.0.CR2", "98.1.0.CR1"));
        assertEquals(Map.of("98.1.1", "98.1.0", "98.1.0", "98.1.0.CR2", "98.1.0.CR2", "98.1.0.CR1"), parents);
        assertEquals("98.1.0.CR1", Lineage.firstMicro(micros("98.1.1", "98.1.0", "98.1.0.CR2", "98.1.0.CR1")));
    }

    @Test
    void patchesAreASideLine() {
        Map<String, String> parents = Lineage.communityParents(
                micros("98.1.2", "98.1.1.2", "98.1.1.1", "98.1.1", "98.1.0"));
        // 98.1.2 is built on 98.1.1, not on its patches
        assertEquals("98.1.1", parents.get("98.1.2"));
        assertEquals("98.1.1", parents.get("98.1.1.1"));
        assertEquals("98.1.1.1", parents.get("98.1.1.2"));
        assertEquals("98.1.0", parents.get("98.1.1"));
    }

    @Test
    void finalSuffixAsIn1xAnd2x() {
        Map<String, String> parents = Lineage.communityParents(
                micros("98.1.1.Final", "98.1.0.Final", "98.1.0.CR1"));
        assertEquals(Map.of("98.1.1.Final", "98.1.0.Final", "98.1.0.Final", "98.1.0.CR1"), parents);
    }

    @Test
    void firstMicroOfAMinorDescendsFromTheNewestContainedMicroOfTheNearestMinor() throws Exception {
        // 99.0 branched off 98.2 after 98.2.3; 98.1 is on another line
        Set<String> contained = Set.of("98.2.0.CR1", "98.2.0", "98.2.1", "98.2.2", "98.2.3");
        List<String> asked = new ArrayList<>();
        Lineage.Ancestry ancestry = (micro, earlier) -> {
            asked.add(earlier);
            return contained.contains(earlier);
        };
        List<List<Releases.MicroEntry>> earlier = List.of(
                micros("98.2.5", "98.2.4", "98.2.3.1", "98.2.3", "98.2.2", "98.2.1", "98.2.0", "98.2.0.CR1"),
                micros("98.1.0"));
        assertEquals("98.2.3", Lineage.crossMinorParent("99.0.0.CR1", earlier, ancestry));
        // a binary search, not a lookup per micro
        assertEquals(true, asked.size() <= 4);
    }

    @Test
    void skipsMinorsItDoesNotDescendFrom() throws Exception {
        Lineage.Ancestry ancestry = (micro, earlier) -> earlier.startsWith("98.1.");
        List<List<Releases.MicroEntry>> earlier = List.of(micros("98.3.0", "98.3.0.CR1"),
                micros("98.2.0", "98.2.0.CR1"), micros("98.1.0", "98.1.0.CR1"));
        assertEquals("98.1.0", Lineage.crossMinorParent("99.0.0.Beta1", earlier, ancestry));
        assertNull(Lineage.crossMinorParent("99.0.0.Beta1", earlier, (micro, e) -> false));
    }

    @Test
    void earlyPreReleasesOnAnotherLineDoNotHideTheMinor() throws Exception {
        // 99.1 grew from 99.0.0.CR1, but 99.0's alphas were on another line
        Set<String> contained = Set.of("99.0.0.CR1", "99.0.0.Beta1");
        List<List<Releases.MicroEntry>> earlier = List.of(
                micros("99.0.1", "99.0.0", "99.0.0.CR1", "99.0.0.Beta1", "99.0.0.Alpha2", "99.0.0.Alpha1"));
        assertEquals("99.0.0.CR1", Lineage.crossMinorParent("99.1.0.CR1", earlier, (m, e) -> contained.contains(e)));
    }

    @Test
    void aPreReleaseNotBuiltOnThePreviousOneDescendsFromAnEarlierMinor() throws Exception {
        // 99.0.0.Alpha2 was cut from the 98.2.1 tag, not from Alpha1; Alpha3 was built on Alpha2
        Set<String> inAlpha2 = Set.of("98.2.0", "98.2.1");
        Set<String> inAlpha3 = Set.of("98.2.0", "98.2.1", "99.0.0.Alpha2");
        Lineage.Ancestry ancestry = (micro, earlier) -> (micro.endsWith("Alpha2") ? inAlpha2 : inAlpha3)
                .contains(earlier);
        List<List<Releases.MicroEntry>> earlier = List.of(micros("98.2.2", "98.2.1", "98.2.0"));
        assertEquals("98.2.1", Lineage.preReleaseParent("99.0.0.Alpha2", "99.0.0.Alpha1", earlier, ancestry));
        assertEquals("99.0.0.Alpha2", Lineage.preReleaseParent("99.0.0.Alpha3", "99.0.0.Alpha2", earlier, ancestry));
    }

    @Test
    void containedIgnoresVersionBumpsAndAPreReleasesLastMinuteFixes() {
        assertEquals(true, Lineage.contained("98.1.4", 0, List.of()));
        assertEquals(true, Lineage.contained("98.1.4", 1, List.of("[RELEASE] - Bump version to 98.1.4")));
        // a later final: backports
        assertEquals(false, Lineage.contained("98.1.5", 3, List.of("Fix a", "Fix b", "[RELEASE] - Bump version to 98.1.5")));
        // a candidate with fixes made on its branch just before tagging
        assertEquals(true, Lineage.contained("98.2.0.CR1", 3,
                List.of("Disable failing tests", "Skip deployment for CR1", "[RELEASE] - Bump version to 98.2.0.CR1")));
        assertEquals(false, Lineage.contained("98.2.0.CR1", 6, List.of("a", "b", "c", "d", "e", "[RELEASE] x")));
        assertEquals(false, Lineage.contained("98.2.0.CR1", 2, List.of("[RELEASE] x", "Fix after")));
        // too many to list
        assertEquals(false, Lineage.contained("98.2.0.CR1", 400, List.of()));
    }

    @Test
    void onlyMicrosWithATagAreCompared() throws Exception {
        List<Releases.MicroEntry> bomOnly = List.of(Releases.MicroEntry.of("98.2.1", null, null, "bom"),
                Releases.MicroEntry.of("98.2.0", null, "https://github.com/x/98.2.0", null));
        assertEquals("98.2.0", Lineage.crossMinorParent("99.0.0.CR1", List.of(bomOnly), (micro, e) -> true));
    }

    @Test
    void vendorBuildsDescendFromTheCommunityMicroOrTheirPreviousServicePack() {
        Set<String> community = Set.of("98.1.0", "98.1.1", "97.2.3.Final");
        List<Releases.MicroEntry> builds = micros("98.1.1.SP2-redhat-00001", "98.1.1.SP1-redhat-00003",
                "98.1.1.redhat-00002", "98.1.0.SP1-redhat-00001", "98.1.0.redhat-00004");
        Map<String, String> parents = Lineage.vendorParents(builds, community);
        assertEquals("98.1.0", parents.get("98.1.0.redhat-00004"));
        assertEquals("98.1.0.redhat-00004", parents.get("98.1.0.SP1-redhat-00001"));
        // a rebase onto a new community micro
        assertEquals("98.1.1", parents.get("98.1.1.redhat-00002"));
        assertEquals("98.1.1.redhat-00002", parents.get("98.1.1.SP1-redhat-00003"));
        assertEquals("98.1.1.SP1-redhat-00003", parents.get("98.1.1.SP2-redhat-00001"));

        // a service pack whose base build isn't listed: the community micro
        assertEquals("98.1.1", Lineage.vendorParents(micros("98.1.1.SP1-redhat-00001"), community)
                .get("98.1.1.SP1-redhat-00001"));
        // 1.x and 2.x naming
        assertEquals("97.2.3.Final", Lineage.communityBase("97.2.3.Final-redhat-00013", community));
        assertNull(Lineage.communityBase("98.1.9.redhat-00001", community));
    }

    @Test
    void keepsTheLatestBuildOfEachVersion() {
        assertEquals(List.of("98.1.1.SP1-redhat-00003", "98.1.1.redhat-00002", "98.1.0.SP1-redhat-00001",
                "98.1.0.redhat-00009"),
                Lineage.latestBuilds(List.of("98.1.0.redhat-00004", "98.1.0.redhat-00009", "98.1.0.SP1-redhat-00001",
                        "98.1.1.redhat-00002", "98.1.1.SP1-redhat-00002", "98.1.1.SP1-redhat-00003", "not-a-build")));
    }

    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(LineageTest.class)).build();
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);
        var summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.printFailuresTo(new PrintWriter(System.out));
        System.exit(summary.getTotalFailureCount() == 0 ? 0 : 1);
    }
}
