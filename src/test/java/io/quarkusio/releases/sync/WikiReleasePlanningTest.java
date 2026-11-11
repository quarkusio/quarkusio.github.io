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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Uses the made-up wiki-style page in src/test/resources/releases-sync/Release-Planning.md (fake versions and dates).
 */
public class WikiReleasePlanningTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private static List<WikiReleasePlanning.PlannedRelease> parseFixture() throws Exception {
        return WikiReleasePlanning.parse(Files.readString(Path.of("src/test/resources/releases-sync/Release-Planning.md")));
    }

    @Test
    void parsesUpcomingReleases() throws Exception {
        List<WikiReleasePlanning.PlannedRelease> upcoming = WikiReleasePlanning.upcoming(parseFixture(), TODAY);

        assertEquals(List.of("99.3", "99.2", "99.1", "99.0"),
                upcoming.stream().map(WikiReleasePlanning.PlannedRelease::version).toList());

        Map<String, WikiReleasePlanning.PlannedRelease> byVersion = upcoming.stream()
                .collect(Collectors.toMap(WikiReleasePlanning.PlannedRelease::version, Function.identity()));
        assertEquals(LocalDate.of(2099, 3, 31), byVersion.get("99.3").platformDate());
        assertTrue(byVersion.get("99.3").lts());
        assertTrue(byVersion.get("99.3").proposal());
        assertEquals(LocalDate.of(2099, 2, 24), byVersion.get("99.2").platformDate());
        assertFalse(byVersion.get("99.2").lts());
        // 99.0 is confirmed (no "(Proposal)") and has a trailing note on its CR1 line
        assertEquals(LocalDate.of(2098, 11, 25), byVersion.get("99.0").platformDate());
        assertFalse(byVersion.get("99.0").proposal());
    }

    @Test
    void ignoresBetaHeadingsAndNonReleaseHeadings() throws Exception {
        List<String> versions = parseFixture().stream().map(WikiReleasePlanning.PlannedRelease::version).toList();
        // "99.0 Beta1", "99.0 Beta2" and "As usual, no feature release..." must not produce entries
        assertEquals(1, versions.stream().filter("99.0"::equals).count());
    }

    @Test
    void releasesBeforeTodayAreNotUpcoming() throws Exception {
        List<WikiReleasePlanning.PlannedRelease> upcoming = WikiReleasePlanning.upcoming(parseFixture(), TODAY);
        assertTrue(upcoming.stream().noneMatch(r -> r.version().equals("98.40")));
        // Releasing today still counts as upcoming
        assertTrue(WikiReleasePlanning.upcoming(parseFixture(), LocalDate.of(2098, 11, 25)).stream()
                .anyMatch(r -> r.version().equals("99.0")));
        assertFalse(WikiReleasePlanning.upcoming(parseFixture(), LocalDate.of(2098, 11, 26)).stream()
                .anyMatch(r -> r.version().equals("99.0")));
    }

    @Test
    void parsesDateVariants() {
        assertEquals(LocalDate.of(2027, 3, 17), WikiReleasePlanning.parseDate("March 17th 2027"));
        assertEquals(LocalDate.of(2026, 7, 22), WikiReleasePlanning.parseDate("July 22th 2026")); // wiki typo
        assertEquals(LocalDate.of(2026, 4, 15), WikiReleasePlanning.parseDate("Apr 15th 2026"));
        assertEquals(LocalDate.of(2026, 6, 2), WikiReleasePlanning.parseDate("June 2nd, 2026 (delayed)"));
        assertNull(WikiReleasePlanning.parseDate("sometime next year"));
        assertNull(WikiReleasePlanning.parseDate("February 31st 2026"));
    }

    @Test
    void reportsReleasesWithoutAParsableDate() {
        String page = """
                ## 99.2

                - Full Platform release (and official announcement) on sometime in February

                ## 99.1

                - Full Platform release (and official announcement) on January 27th 2099

                ## 99.0 Beta1

                - Full Platform release (and official announcement) on soon
                """;
        List<String> skipped = new java.util.ArrayList<>();
        List<WikiReleasePlanning.PlannedRelease> releases = WikiReleasePlanning.parse(page, skipped::add);

        assertEquals(List.of("99.1"), releases.stream().map(WikiReleasePlanning.PlannedRelease::version).toList());
        assertEquals(List.of("99.2"), skipped);
    }

    @Test
    void emptyOrGarbageInputYieldsNothing() {
        assertTrue(WikiReleasePlanning.parse("").isEmpty());
        assertTrue(WikiReleasePlanning.parse("## 99.9\n\n- Core Final on March 1st 2099\n").isEmpty());
    }
}
