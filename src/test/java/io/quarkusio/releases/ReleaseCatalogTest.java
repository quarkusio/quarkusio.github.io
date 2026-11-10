package io.quarkusio.releases;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

class ReleaseCatalogTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    static final String DATA = """
            {
              "policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
              "support_providers": {"ibm": {"name": "IBM", "url": "https://ibm.example"}},
              "releases": [
                {"version": "4.0.0", "lts": false, "upcoming": true},
                {"version": "3.40", "release_date": "2026-09-30", "eol_date": "2027-09-30", "lts": true},
                {"version": "3.39", "release_date": "2026-08-26", "eol_date": "2026-09-30", "lts": false},
                {"version": "3.33", "release_date": "2026-03-25", "eol_date": "2027-03-25", "lts": true,
                 "rhbq": {"eol_date": "2028-06-30"}, "ibm": {"eol_date": "2028-06-30"}},
                {"version": "3.20", "release_date": "2025-03-26", "eol_date": "2026-03-28", "lts": true,
                 "rhbq": {"eol_date": "2027-06-30"}, "herodevs": {"eol_date": "nes"}},
                {"version": "2.16", "release_date": "2023-01-01", "eol_date": "2023-06-01", "lts": true,
                 "herodevs": {"eol_date": "2030-01-01"}}
              ]
            }
            """;

    static ReleaseCatalog catalogOf(String json, int maxUpcoming) {
        return new ReleaseCatalog(parse(json), maxUpcoming);
    }

    static ReleaseCatalog catalogOf(String json) {
        return new ReleaseCatalog(parse(json));
    }

    /** Binds like Roq does: a plain mapper, snake_case keys onto the Releases record. */
    static Releases parse(String json) {
        try {
            return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).readValue(json, Releases.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    final ReleaseCatalog catalog = catalogOf(DATA);

    @Test
    void bindsPolicyAndProviders() {
        assertEquals("12 months", catalog.policy().ltsSupportDuration().label());
        assertEquals("https://ibm.example", catalog.providers().ibm().url());
    }

    @Test
    void parsesDatesAndHeroDevsValues() {
        Release lts = catalog.all().get(1);
        assertEquals(LocalDate.of(2026, 9, 30), lts.releaseDate());
        assertNull(catalog.all().get(0).releaseDate());
        assertTrue(catalog.all().get(4).herodevsEol().isNes());
        assertEquals(LocalDate.of(2030, 1, 1), catalog.all().get(5).herodevsEol().date());
        assertFalse(catalog.all().get(1).hasNes());
    }

    @Test
    void listedKeepsOnlyTheNextUpcoming() {
        var c = catalogOf("""
                {"releases": [
                  {"version": "4.1", "lts": false, "upcoming": true, "planned_date": "2027-01-27"},
                  {"version": "4.0", "lts": false, "upcoming": true, "planned_date": "2026-11-25"},
                  {"version": "3.40", "lts": true, "release_date": "2026-09-30"}]}""");
        assertEquals(List.of("4.0", "3.40"), c.listed().stream().map(Release::version).toList());
    }

    @Test
    void maxUpcomingControlsHowManyAreListed() {
        String json = """
                {"releases": [
                  {"version": "4.1", "lts": false, "upcoming": true, "planned_date": "2027-01-27"},
                  {"version": "4.0", "lts": false, "upcoming": true, "planned_date": "2026-11-25"},
                  {"version": "3.40", "lts": true, "release_date": "2026-09-30"}]}""";
        assertEquals(List.of("3.40"), versions(catalogOf(json, 0).listed()));
        assertEquals(List.of("4.1", "4.0", "3.40"), versions(catalogOf(json, 2).listed()));
    }

    @Test
    void upcomingReleasesGetAPlannedEndDateFromTheNextOneEvenWhenHidden() {
        var c = catalogOf("""
                {"policy": {"lts_support_duration": {"amount": 12, "unit": "months"}},
                 "releases": [
                  {"version": "4.3", "lts": true, "upcoming": true, "planned_date": "2027-03-31"},
                  {"version": "4.1", "lts": false, "upcoming": true, "planned_date": "2027-01-27"},
                  {"version": "4.0", "lts": false, "upcoming": true, "planned_date": "2026-11-25"},
                  {"version": "3.40", "lts": true, "release_date": "2026-09-30"}]}""");
        assertEquals(java.time.LocalDate.of(2027, 1, 27), c.visibleUpcoming().get(0).plannedEndDate());
        var lts = c.upcoming().stream().filter(r -> r.version().equals("4.3")).findFirst().orElseThrow();
        assertEquals(java.time.LocalDate.of(2028, 3, 31), lts.plannedEndDate());
        assertEquals(null, c.latest().orElseThrow().plannedEndDate());
    }

    static List<String> versions(List<Release> releases) {
        return releases.stream().map(Release::version).toList();
    }

    @Test
    void latestSkipsUpcoming() {
        assertEquals("3.40", catalog.latest().orElseThrow().version());
    }

    @Test
    void recommendedLtsIsNewestLtsStillInCommunityMaintenance() {
        assertEquals("3.40", catalog.recommendedLts(TODAY).orElseThrow().version());
        assertNull(catalog.recommendedLtsOrNull(TODAY.atStartOfDay().plusYears(5)));
    }

    @Test
    void endOfLifeListsEndedReleasesNewestFirst() {
        List<String> versions = catalog.endOfLife(TODAY).stream().map(Release::version).toList();
        assertEquals(List.of("3.39", "3.20", "2.16"), versions);
    }

    @Test
    void supportedVersionsListOtherSupportedLts() {
        var supported = catalog.supportedVersions(TODAY);
        assertEquals("3", supported.latestMajor());
        assertEquals(List.of("3.33"), supported.otherLts().stream().map(Release::version).toList());
    }

    @Test
    void vendorSupportFlags() {
        Release r = catalog.all().get(3);
        assertTrue(r.rhbqActive(TODAY));
        assertTrue(r.ibmActive(TODAY));
        assertFalse(r.rhbqActive(LocalDate.of(2029, 1, 1)));
        assertTrue(r.communityActive(TODAY));
        assertFalse(r.communityActive(LocalDate.of(2030, 1, 1)));
        assertFalse(catalog.all().get(0).communityActive(TODAY), "upcoming is never active");
    }
}
