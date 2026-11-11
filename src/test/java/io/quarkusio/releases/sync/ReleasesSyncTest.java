package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkusio.releases.Releases;
import io.quarkusio.releases.Releases.Entry;
import io.quarkusio.releases.sync.GitHubReleases.GitHubRelease;
import io.quarkusio.releases.sync.ReleasesSync.UpcomingRelease;

class ReleasesSyncTest {

    private static final Releases.ProviderDates RHBQ = new Releases.ProviderDates(null, "2099-06-30");

    private static Releases existing() {
        return new Releases(null, null, List.of(), List.of(
                new Entry("99.2", null, "2099-03-01", null, false, true, false, null, null, null, null, null, null),
                new Entry("99.1", "2099-02-01", null, null, true, false, false, "99.1.0", "2099-02-01",
                        "/blog/99-1/", RHBQ, null, null),
                new Entry("99.0", "2099-01-01", null, "2099-12-31", false, false, false, "99.0.1", "2099-01-10",
                        null, null, null, null)));
    }

    private static GitHubRelease release(String tag, String date) {
        return new GitHubRelease(tag, date + "T10:00:00Z", false);
    }

    private static Map<String, Entry> byVersion(Releases releases) {
        return releases.releases().stream().collect(Collectors.toMap(Entry::version, Function.identity()));
    }

    @Test
    void publishedUpcomingReleaseKeepsManualFieldsAndGetsPatches() {
        Releases synced = ReleasesSync.sync(existing(), List.of(
                release("99.2.0", "2099-03-02"), release("99.2.1", "2099-03-10"),
                release("99.1.0", "2099-02-01"), release("99.1.1", "2099-02-15"),
                release("99.0.0", "2099-01-01"), release("99.0.1", "2099-01-10")),
                List.of(), List.of());

        assertEquals(List.of("99.2", "99.1", "99.0"), synced.releases().stream().map(Entry::version).toList());
        Map<String, Entry> releases = byVersion(synced);

        Entry published = releases.get("99.2");
        assertFalse(published.upcoming());
        assertNull(published.plannedDate());
        assertEquals("2099-03-02", published.releaseDate());
        assertEquals("99.2.1", published.latestPatch());
        assertEquals("2099-03-10", published.latestPatchDate());

        Entry lts = releases.get("99.1");
        assertEquals("99.1.1", lts.latestPatch());
        assertEquals("/blog/99-1/", lts.link());
        assertEquals(RHBQ, lts.rhbq());
        assertTrue(lts.lts());
        assertNull(lts.eolDate(), "LTS releases have no computed eol date");

        assertEquals("2099-12-31", releases.get("99.0").eolDate(), "explicit eol dates are kept");
    }

    @Test
    void nonLtsEolIsTheNextMinorReleaseDate() {
        Releases data = new Releases(null, null, List.of(), List.of(
                new Entry("99.0", "2099-01-01", null, null, false, false, false, "99.0.0", "2099-01-01",
                        null, null, null, null)));
        Releases synced = ReleasesSync.sync(data,
                List.of(release("99.1.0", "2099-02-01"), release("99.0.0", "2099-01-01")), List.of(), List.of());
        assertEquals("2099-02-01", byVersion(synced).get("99.0").eolDate());
    }

    @Test
    void wikiReplacesUpcomingReleases() {
        Releases synced = ReleasesSync.sync(existing(),
                List.of(release("99.1.0", "2099-02-01"), release("99.0.0", "2099-01-01")), List.of(),
                List.of(new UpcomingRelease("99.2", "wiki", "2099-03-04", false, false),
                        new UpcomingRelease("99.3", "wiki", "2099-04-01", true, true)));
        Map<String, Entry> releases = byVersion(synced);
        assertEquals("2099-03-04", releases.get("99.2").plannedDate());
        Entry proposal = releases.get("99.3");
        assertTrue(proposal.upcoming());
        assertTrue(proposal.proposal());
        assertTrue(proposal.lts());
    }

    @Test
    void predictsNextMinorWhenNothingIsUpcoming() {
        Releases data = new Releases(null, null, List.of(), List.of());
        List<GitHubRelease> published = List.of(release("99.1.0", "2099-02-01"));
        Releases synced = ReleasesSync.sync(data, published,
                ReleasesSync.predictUpcoming(published, List.of(), List.of()), List.of());
        assertEquals(List.of("99.2", "99.1"), synced.releases().stream().map(Entry::version).toList());
        assertTrue(byVersion(synced).get("99.2").upcoming());
    }
}
