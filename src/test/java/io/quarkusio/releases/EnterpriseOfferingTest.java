package io.quarkusio.releases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class EnterpriseOfferingTest {

    private static final LocalDate COMMUNITY = LocalDate.of(2025, 3, 26);

    private static Release release(Releases.EnterpriseDates rhbq, Releases.EnterpriseDates ibm,
            Releases.EnterpriseDates herodevs) {
        return Release.from(new Releases.Entry("3.20", "2025-03-26", null, "2025-09-24", true, false, false, null, rhbq,
                ibm, herodevs, null, null));
    }

    @Test
    void branchPointIsTheReleaseDateOfTheReleaseBranchedFrom() {
        Releases data = ReleasesTest.parse("""
                {"releases": [
                  {"version": "3.21", "release_date": "2025-03-26"},
                  {"version": "3.20", "release_date": "2025-03-26", "lts": true,
                   "rhbq": {"eol_date": "2027-06-30"},
                   "ibm": {"eol_date": "2027-06-30", "branched_from": "3.19"}},
                  {"version": "3.19", "release_date": "2025-02-26"}
                ]}""");
        Release lts = data.find("3.20").orElseThrow();
        // without branched_from, the release itself
        assertEquals(COMMUNITY, data.branchPoint(lts, lts.rhbq()));
        assertEquals(LocalDate.of(2025, 2, 26), data.branchPoint(lts, lts.ibm()));
        // a micro, or an unknown version, can't be dated yet: the release itself
        assertEquals(COMMUNITY, data.branchPoint(lts, new EnterpriseOffering(null, null, "3.20.3", false)));
    }

    @Test
    void enterpriseSupportNeedsAnEndDateAndStopsOnIt() {
        LocalDate end = LocalDate.of(2027, 6, 30);
        var vendor = new EnterpriseOffering(null, end, null, false);
        assertTrue(vendor.activeOn(end.minusDays(1)));
        assertFalse(vendor.activeOn(end));
        assertFalse(new EnterpriseOffering(COMMUNITY, null, null, false).activeOn(COMMUNITY));
    }

    @Test
    void ibmOpenEndedSupportComesFromTheData() {
        Release r = release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, true), null);
        assertTrue(r.ibm().openEnded());
        assertFalse(release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, false), null).ibm().openEnded());
        // starts when IBM's own support ends, or the community's if later
        assertEquals(LocalDate.of(2027, 6, 30), r.openEndedStart(r.ibm()));
    }

    @Test
    void openEndedSupportWithoutABuildStartsWhenAllOtherSupportEnds() {
        Release r = release(new Releases.EnterpriseDates(null, "2028-06-30", null, false),
                new Releases.EnterpriseDates(null, "2027-06-30", null, true), new Releases.EnterpriseDates(null, null, null, true));
        assertTrue(r.herodevs().openEnded());
        assertEquals(LocalDate.of(2028, 6, 30), r.openEndedStart(r.herodevs()));
        assertNull(release(null, null, null).herodevs());
    }

    @Test
    void openEndedSupportCountsOnceEnterpriseSupportEnds() {
        Release r = release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, true), null);
        assertEquals(Release.Status.ENTERPRISE_ONLY, r.status(LocalDate.of(2026, 1, 1)));
        assertEquals(Release.Status.OPEN_ENDED_ONLY, r.status(LocalDate.of(2028, 1, 1)));
        assertTrue(r.supportActive(LocalDate.of(2040, 1, 1)));
        assertFalse(release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, false), null)
                .supportActive(LocalDate.of(2028, 1, 1)));
    }
}
