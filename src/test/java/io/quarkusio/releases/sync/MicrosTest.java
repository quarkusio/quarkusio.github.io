package io.quarkusio.releases.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkusio.releases.Releases;
import io.quarkusio.releases.sync.Micros.GitHubTag;

/**

 * Uses a made-up Maven Central listing in test/ (fake versions 98.x and 99.x and fake dates).
 */
public class MicrosTest {

    static GitHubTag gh(String tag, String publishedAt) {
        return new GitHubTag(tag, publishedAt, "https://github.com/example/releases/tag/" + tag);
    }

    static Map<String, LocalDate> listing() throws Exception {
        return EnterpriseReleaseDates.parseListing(Files.readString(Path.of("src/test/resources/releases-sync/central-listing.html")));
    }

    @Test
    void parsesMavenCentralListing() throws Exception {
        assertEquals(Map.of("98.1.0.CR1", LocalDate.of(2090, 1, 1), "98.1.1", LocalDate.of(2090, 1, 20),
                "98.2.0", LocalDate.of(2090, 2, 3), "99.0.0.Beta1", LocalDate.of(2090, 3, 1)), listing());
    }

    @Test
    void listsEveryVersionFromEitherSourceNewestFirst() throws Exception {
        List<GitHubTag> github = List.of(
                gh("98.1.0.CR1", "2090-01-01T12:00:00Z"),
                gh("98.1.0", "2090-01-14T12:00:00Z"), // core release with no platform BOM
                gh("98.1.1", "2090-01-21T08:00:00Z"),
                gh("98.2.0", "2090-02-03T23:00:00Z"));
        Map<String, List<Releases.MicroEntry>> micros = Micros.byMinor(github, listing());

        assertEquals(List.of("98.1.1", "98.1.0", "98.1.0.CR1"),
                micros.get("98.1").stream().map(Releases.MicroEntry::version).toList());
        // only on Maven Central
        Releases.MicroEntry beta = micros.get("99.0").getFirst();
        assertEquals("99.0.0.Beta1", beta.version());
        assertTrue(beta.beta());
        assertFalse(beta.candidate());
        assertNull(beta.githubUrl());
        assertEquals(Micros.bomUrl("99.0.0.Beta1"), beta.bomUrl());
        // only on GitHub
        Releases.MicroEntry noBom = micros.get("98.1").get(1);
        assertEquals("https://github.com/example/releases/tag/98.1.0", noBom.githubUrl());
        assertNull(noBom.bomUrl());
        assertFalse(noBom.candidate() || noBom.beta() || noBom.alpha());
        Releases.MicroEntry candidate = micros.get("98.1").get(2);
        assertTrue(candidate.candidate());
        assertFalse(candidate.beta() || candidate.alpha());
    }

    @Test
    void takesTheEarlierDate() throws Exception {
        List<GitHubTag> github = List.of(
                gh("98.1.1", "2090-01-21T08:00:00Z"), // a day after the BOM
                gh("98.1.0.CR1", "2089-12-31T23:59:00Z")); // a day before the BOM
        Map<String, List<Releases.MicroEntry>> micros = Micros.byMinor(github, listing());
        assertEquals("2090-01-20", micros.get("98.1").get(0).releaseDate());
        assertEquals("2089-12-31", micros.get("98.1").get(1).releaseDate());
        assertEquals("2090-02-03", micros.get("98.2").get(0).releaseDate());
    }

    @Test
    void bomUrlPointsAtThePlatformBomPom() {
        assertEquals("https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/98.1.0.CR1/quarkus-bom-98.1.0.CR1.pom",
                Micros.bomUrl("98.1.0.CR1"));
    }

    @Test
    void existingDataStandsInForMavenCentral() {
        Releases data = new Releases(null, null, null, List.of(new Releases.Entry("98.1", "2090-01-14", null, null,
                false, false, false, null, null, null, null, null, List.of(
                        Releases.MicroEntry.of("98.1.1", "2090-01-20", "https://github.com/x", Micros.bomUrl("98.1.1")),
                        Releases.MicroEntry.of("98.1.0", "2090-01-14", "https://github.com/y", null)))));
        assertEquals(Map.of("98.1.1", LocalDate.of(2090, 1, 20)), Micros.bomsFromExisting(data));
    }

}
