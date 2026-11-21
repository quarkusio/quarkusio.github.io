package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseListTest {

    @TempDir
    Path dir;

    @Test
    void readsTheCommunityMicrosAndWhetherThePlatformHasABom() throws IOException {
        File f = dir.resolve("releases.yaml").toFile();
        Files.writeString(f.toPath(), """
                releases:
                  - version: "3.39"
                    micros:
                      - version: "3.39.1"
                        release_date: 2026-08-26
                        parent_release: "3.39.0"
                        bom_url: https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/3.39.1/quarkus-bom-3.39.1.pom
                      - version: "3.39.0"
                        release_date: 2026-08-26
                        parent_release: "3.39.0.CR1"
                    rhbq:
                      micros:
                        - version: "3.39.1.redhat-00001"
                  - version: "4.3"
                    planned_date: 2027-03-31
                """);
        List<ReleaseList.Micro> micros = ReleaseList.read(f);
        assertEquals(2, micros.size());
        assertEquals(new ReleaseList.Micro("3.39.1", "2026-08-26", "3.39.0", true), micros.get(0));
        assertEquals(new ReleaseList.Micro("3.39.0", "2026-08-26", "3.39.0.CR1", false), micros.get(1));
        assertTrue(micros.get(0).hasPlatformBom());
        assertFalse(micros.get(1).hasPlatformBom());
    }

    @Test
    void aReleasesFileWithoutLineageIsRefused() {
        List<ReleaseList.Micro> flat = List.of(new ReleaseList.Micro("3.39.1", "2026-08-26", null, true));
        assertThrows(IllegalStateException.class, () -> ReleaseList.requireLineage(flat));
        ReleaseList.requireLineage(List.of(new ReleaseList.Micro("3.39.1", "2026-08-26", "3.39.0", true)));
        ReleaseList.requireLineage(List.of());
    }

    @Test
    void onlyTheReleasesBeforeTheFirstCoreBomMayBeMissingOne() {
        assertTrue(ReleaseReportService.predatesCoreBom("0.2.0"));
        assertTrue(ReleaseReportService.predatesCoreBom("0.10.0"));
        assertFalse(ReleaseReportService.predatesCoreBom("0.11.0"));
        assertFalse(ReleaseReportService.predatesCoreBom("1.0.0.Final"));
        assertFalse(ReleaseReportService.predatesCoreBom("3.39.0"));
    }

    @Test
    void releasesAreOrderedByDateThenVersion() {
        Map<String, ReleaseList.Micro> byVersion = Map.of(
                "3.39.1", new ReleaseList.Micro("3.39.1", "2026-08-26", "3.39.0", true),
                "3.39.0", new ReleaseList.Micro("3.39.0", "2026-08-26", "3.39.0.CR1", false),
                "3.38.3", new ReleaseList.Micro("3.38.3", "2026-08-19", "3.38.2", true));
        List<String> sorted = byVersion.keySet().stream()
                .sorted(ReleaseReportService.releaseOrder(byVersion)).toList();
        assertEquals(List.of("3.38.3", "3.39.0", "3.39.1"), sorted);
    }
}
