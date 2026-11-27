package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkusio.releases.Releases;

/**

 * The release times are the real ones of 2.11.0.Final, which GitHub published 399 days after tagging it; the data
 * checks use made-up releases (fake versions 98.x and fake dates).
 */
public class ReleaseDatesTest {

    @Test
    void aReleasePublishedLongAfterItsTagIsDatedFromTheTag() {
        assertEquals("2022-07-20T08:48:23Z",
                GitHubReleases.releaseTime("2022-07-20T08:48:23Z", "2023-08-23T17:22:02Z", "2.11.0.Final"));
    }

    @Test
    void aNormalDelayBetweenTagAndPublishingKeepsThePublishedDate() {
        // a week is routine (the tag commit comes first); even the longest real one, 23 days, is kept
        assertEquals("2026-09-30T10:00:00Z",
                GitHubReleases.releaseTime("2026-09-23T10:00:00Z", "2026-09-30T10:00:00Z", "98.0.0"));
        assertEquals("2021-11-03T10:00:00Z",
                GitHubReleases.releaseTime("2021-10-11T10:00:00Z", "2021-11-03T10:00:00Z", "98.0.0.CR1"));
    }

    @Test
    void aPublishedDateEarlierThanTheTagIsKept() {
        assertEquals("2026-09-20T10:00:00Z",
                GitHubReleases.releaseTime("2026-09-23T10:00:00Z", "2026-09-20T10:00:00Z", "98.0.0"));
    }

    @Test
    void missingTimesAreTolerated() {
        assertEquals("2026-09-30T10:00:00Z", GitHubReleases.releaseTime(null, "2026-09-30T10:00:00Z", "98.0.0"));
        assertNull(GitHubReleases.releaseTime("2026-09-30T10:00:00Z", null, "98.0.0"));
    }

    private static Releases.MicroEntry micro(String version, String date, String parent) {
        return Releases.MicroEntry.of(version, date, null, null).withParentRelease(parent);
    }

    private static Releases data(Releases.MicroEntry... micros) {
        return new Releases(null, null, List.of(), List.of(new Releases.Entry("98.1", "2080-01-01", null, null, true,
                false, false, null, null, null, null, null, List.of(micros))));
    }

    @Test
    void aMicroDatedBeforeItsParentIsReported() {
        List<String> problems = ReleasesSync.microsBeforeTheirParents(data(
                micro("98.1.1", "2080-02-10", "98.1.0"),
                micro("98.1.0", "2081-03-01", "98.1.0.CR1"),
                micro("98.1.0.CR1", "2080-01-20", null)));
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith("98.1.1 (2080-02-10) is dated before its parent_release 98.1.0 (2081-03-01)"),
                problems.get(0));
    }

    @Test
    void consistentDatesAndUnlistedParentsAreNotReported() {
        assertEquals(List.of(), ReleasesSync.microsBeforeTheirParents(data(
                micro("98.1.2", "2080-02-10", "98.1.1"),
                micro("98.1.1", "2080-02-10", "98.1.0"),
                micro("98.1.0", "2080-01-01", "97.9.9"))));
    }

}
