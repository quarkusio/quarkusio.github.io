package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.cve.model.UnifiedVuln;
import org.junit.jupiter.api.Test;

class ReleaseReportServiceTest {

    private static UnifiedVuln.Interval interval(String introduced, String fixed, String lastAffected) {
        UnifiedVuln.Interval iv = new UnifiedVuln.Interval();
        iv.introduced = introduced;
        iv.fixed = fixed;
        iv.lastAffected = lastAffected;
        return iv;
    }

    @Test
    void rangeIsWrittenLikeTheQuarkusRanges() {
        assertEquals(">=4.1.0 <4.1.100", ReleaseReportService.range(interval("4.1.0", "4.1.100", null)));
        assertEquals("<4.1.100", ReleaseReportService.range(interval("0", "4.1.100", null)));
        assertEquals("<4.1.100", ReleaseReportService.range(interval(null, "4.1.100", null)));
        assertEquals(">=2.0 <=2.5", ReleaseReportService.range(interval("2.0", null, "2.5")));
        assertEquals(">=2.0", ReleaseReportService.range(interval("2.0", null, null)));
        assertEquals("*", ReleaseReportService.range(interval(null, null, null)));
    }
}