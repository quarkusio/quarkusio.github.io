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
    void branchPointIsTheReleaseDateOfTheParentRelease() {
        Releases data = ReleasesTest.parse("""
                {"releases": [
                  {"version": "3.21", "release_date": "2025-03-26"},
                  {"version": "3.20", "release_date": "2025-03-26", "lts": true,
                   "micros": [{"version": "3.20.3", "release_date": "2025-05-28", "parent_release": "3.20.2"}],
                   "rhbq": {"eol_date": "2027-06-30",
                            "micros": [{"version": "3.20.3.redhat-00001", "release_date": "2025-06-10",
                                        "parent_release": "3.20.3"}]},
                   "ibm": {"eol_date": "2027-06-30", "parent_release": "3.19"}},
                  {"version": "3.19", "release_date": "2025-02-26"}
                ]}""");
        Release lts = data.find("3.20").orElseThrow();
        // without parent_release, the release itself
        assertEquals(COMMUNITY, data.branchPoint(lts, lts.rhbq()));
        // a minor
        assertEquals(LocalDate.of(2025, 2, 26), data.branchPoint(lts, lts.ibm()));
        // a micro
        assertEquals(LocalDate.of(2025, 5, 28), data.branchPoint(lts, new EnterpriseOffering(null, null, "3.20.3", false, null)));
        // an unknown version: the release itself
        assertEquals(COMMUNITY, data.branchPoint(lts, new EnterpriseOffering(null, null, "3.20.9", false, null)));
    }

    @Test
    void microsAndVendorMicrosKeepTheirParentRelease() {
        Releases data = ReleasesTest.parse("""
                {"releases": [
                  {"version": "3.20", "release_date": "2025-03-26",
                   "micros": [{"version": "3.20.3", "release_date": "2025-05-28", "parent_release": "3.20.2"}],
                   "rhbq": {"micros": [{"version": "3.20.3.SP1-redhat-00001", "parent_release": "3.20.3.redhat-00001"}]}}
                ]}""");
        Release r = data.find("3.20").orElseThrow();
        assertEquals("3.20.2", r.micros().get(0).parentRelease());
        assertEquals("3.20.3.redhat-00001", r.rhbq().micros().get(0).parentRelease());
        assertTrue(release(null, null, null).micros().isEmpty());
        assertTrue(new EnterpriseOffering(null, null, null, false, null).micros().isEmpty());
    }

    @Test
    void enterpriseSupportNeedsAnEndDateAndStopsOnIt() {
        LocalDate end = LocalDate.of(2027, 6, 30);
        var vendor = new EnterpriseOffering(null, end, null, false, null);
        assertTrue(vendor.activeOn(end.minusDays(1)));
        assertFalse(vendor.activeOn(end));
        assertFalse(new EnterpriseOffering(COMMUNITY, null, null, false, null).activeOn(COMMUNITY));
    }

    @Test
    void ibmOpenEndedSupportComesFromTheData() {
        Release r = release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, true, false, null), null);
        assertTrue(r.ibm().openEnded());
        assertFalse(release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, false, false, null), null).ibm().openEnded());
        // starts when IBM's own support ends, or the community's if later
        assertEquals(LocalDate.of(2027, 6, 30), r.openEndedStart(r.ibm()));
    }

    @Test
    void openEndedSupportWithoutABuildStartsWhenAllOtherSupportEnds() {
        Release r = release(new Releases.EnterpriseDates(null, "2028-06-30", null, false, false, null),
                new Releases.EnterpriseDates(null, "2027-06-30", null, true, false, null), new Releases.EnterpriseDates(null, null, null, true, false, null));
        assertTrue(r.herodevs().openEnded());
        assertEquals(LocalDate.of(2028, 6, 30), r.openEndedStart(r.herodevs()));
        assertNull(release(null, null, null).herodevs());
    }

    @Test
    void openEndedSupportCountsOnceEnterpriseSupportEnds() {
        Release r = release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, true, false, null), null);
        assertEquals(Release.Status.ENTERPRISE_ONLY, r.status(LocalDate.of(2026, 1, 1)));
        assertEquals(Release.Status.OPEN_ENDED_ONLY, r.status(LocalDate.of(2028, 1, 1)));
        assertTrue(r.supportActive(LocalDate.of(2040, 1, 1)));
        assertFalse(release(null, new Releases.EnterpriseDates(null, "2027-06-30", null, false, false, null), null)
                .supportActive(LocalDate.of(2028, 1, 1)));
    }
}
