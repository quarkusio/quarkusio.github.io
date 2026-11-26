package io.quarkus.cve.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class QuarkusBomServiceTest {

    @Test
    void coreBomsBeforeTwoDotZeroResolveTheirVersionProperties() {
        String pom = """
                <project>
                  <parent><groupId>io.quarkus</groupId><artifactId>quarkus-parent</artifactId><version>1.0.0.Final</version></parent>
                  <artifactId>quarkus-bom</artifactId>
                  <properties><netty.version>4.1.42.Final</netty.version></properties>
                  <dependencyManagement><dependencies>
                    <dependency><groupId>io.netty</groupId><artifactId>netty-codec</artifactId><version>${netty.version}</version></dependency>
                    <dependency><groupId>io.quarkus</groupId><artifactId>quarkus-core</artifactId><version>${project.version}</version></dependency>
                    <dependency><groupId>org.example</groupId><artifactId>spelled-out</artifactId><version>1.2.3</version></dependency>
                    <dependency><groupId>org.example</groupId><artifactId>undefined</artifactId><version>${nowhere.version}</version></dependency>
                  </dependencies></dependencyManagement>
                </project>
                """;
        assertEquals(List.of(
                "io.netty:netty-codec:4.1.42.Final",
                "io.quarkus:quarkus-core:1.0.0.Final",
                "org.example:spelled-out:1.2.3"), QuarkusBomService.parseManaged(pom));
    }

    @Test
    void aPropertyThatNamesAnotherPropertyIsResolvedThrough() {
        Map<String, String> props = Map.of("a", "${b}", "b", "1.0");
        assertEquals("1.0", QuarkusBomService.resolve("${a}", props));
        assertEquals("x-1.0", QuarkusBomService.resolve("x-${a}", props));
        assertNull(QuarkusBomService.resolve("${missing}", props));
        assertNull(QuarkusBomService.resolve("${loop}", Map.of("loop", "${loop}")));
    }

    @Test
    void theReleaseSourceNamesTheBomItWasReadFrom() {
        assertEquals("maven://io.quarkus.platform:quarkus-bom:3.39.1",
                QuarkusBomService.BomSource.PLATFORM.id("3.39.1"));
        assertEquals("maven://io.quarkus:quarkus-bom:3.39.0",
                QuarkusBomService.BomSource.CORE.id("3.39.0"));
    }

    @Test
    void strictGreaterThanExcludesTheBoundaryVersion() {
        io.quarkus.cve.model.UnifiedVuln.AffectedPkg pkg = new io.quarkus.cve.model.UnifiedVuln.AffectedPkg();
        pkg.ecosystem = "Maven";
        pkg.name = "io.quarkus:quarkus-core";
        pkg.intervals.add(io.quarkus.cve.model.UnifiedVuln.parseGitHubRange("> 3.15.1, < 3.15.4"));

        Map<String, org.apache.maven.artifact.versioning.ComparableVersion> parsed = new java.util.HashMap<>();
        assertNull(QuarkusBomService.matching("3.15.1", pkg, parsed));
        assertNull(QuarkusBomService.matching("3.15.1.CR1", pkg, parsed));
        org.junit.jupiter.api.Assertions.assertNotNull(QuarkusBomService.matching("3.15.2", pkg, parsed));
        assertNull(QuarkusBomService.matching("3.15.4", pkg, parsed));
    }

    @Test
    void importedBomsAndParentPropertiesAreExpandedRecursively() {
        String rootBom = """
                <project>
                  <groupId>io.quarkus</groupId>
                  <artifactId>quarkus-bom</artifactId>
                  <version>1.9.2.Final</version>
                  <properties><jackson.bom.version>2.11.3</jackson.bom.version></properties>
                  <dependencyManagement><dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-bom</artifactId>
                      <version>${jackson.bom.version}</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-databind</artifactId>
                      <version>2.11.3.1</version>
                    </dependency>
                  </dependencies></dependencyManagement>
                </project>
                """;
        String jacksonBom = """
                <project>
                  <parent>
                    <groupId>com.fasterxml.jackson</groupId>
                    <artifactId>jackson-parent</artifactId>
                    <version>2.11</version>
                  </parent>
                  <artifactId>jackson-bom</artifactId>
                  <version>2.11.3</version>
                  <dependencyManagement><dependencies>
                    <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-core</artifactId><version>${jackson.core.version}</version></dependency>
                    <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.11.3</version></dependency>
                  </dependencies></dependencyManagement>
                </project>
                """;
        String jacksonParent = """
                <project>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-parent</artifactId>
                  <version>2.11</version>
                  <properties><jackson.core.version>2.11.3</jackson.core.version></properties>
                </project>
                """;

        QuarkusBomService.PomFetcher fetcher = (g, a, v) -> switch (g + ":" + a + ":" + v) {
            case "com.fasterxml.jackson:jackson-bom:2.11.3" -> jacksonBom;
            case "com.fasterxml.jackson:jackson-parent:2.11" -> jacksonParent;
            default -> throw new IllegalStateException("Unexpected POM: " + g + ":" + a + ":" + v);
        };

        List<String> managed = QuarkusBomService.parseManaged(rootBom, fetcher);
        // Imported BOM itself is kept, outer BOM override (2.11.3.1) wins over imported BOM (2.11.3), and parent property resolves jackson-core
        assertEquals(List.of(
                "com.fasterxml.jackson:jackson-bom:2.11.3",
                "com.fasterxml.jackson.core:jackson-databind:2.11.3.1",
                "com.fasterxml.jackson.core:jackson-core:2.11.3"), managed);
    }

    @Test
    void failedImportedOrParentPomMarksResultIncompleteAndSkipsCache(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        io.quarkus.cve.util.Cache.configure(dir, io.quarkus.cve.util.Cache.Mode.NORMAL);
        String bomWithBrokenImport = """
                <project>
                  <groupId>io.quarkus</groupId>
                  <artifactId>quarkus-bom</artifactId>
                  <version>1.9.2.Final</version>
                  <dependencyManagement><dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-bom</artifactId>
                      <version>2.11.3</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                    <dependency>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-core</artifactId>
                      <version>1.9.2.Final</version>
                    </dependency>
                  </dependencies></dependencyManagement>
                </project>
                """;
        QuarkusBomService service = new QuarkusBomService();
        service.mavenCentral = new io.quarkus.cve.client.MavenCentralClient() {
            @Override public String bomMetadata() { return ""; }
            @Override public String bomDirectory() { return ""; }
            @Override public String bomPom(String version) { return ""; }
            @Override public String coreBomPom(String version) { return bomWithBrokenImport; }
            @Override public String pom(String path) { throw new IllegalStateException("Maven Central 503"); }
        };

        java.util.Map<String, String> managed = service.managedVersions("1.9.2.Final", QuarkusBomService.BomSource.CORE, null);
        assertEquals(java.util.Map.of(
                "com.fasterxml.jackson:jackson-bom", "2.11.3",
                "io.quarkus:quarkus-core", "1.9.2.Final"), managed);
        org.junit.jupiter.api.Assertions.assertTrue(io.quarkus.cve.util.Cache.read("bom",
                QuarkusBomService.BomSource.CORE.cachePrefix + "1.9.2.Final",
                new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}).isEmpty(),
                "incomplete BOM must not be persisted into immutable cache");
    }

    @Test
    void crVersionDoesNotMatchLaterCrIntroducedBound() {
        io.quarkus.cve.model.UnifiedVuln.AffectedPkg p = new io.quarkus.cve.model.UnifiedVuln.AffectedPkg();
        io.quarkus.cve.model.UnifiedVuln.Interval iv = new io.quarkus.cve.model.UnifiedVuln.Interval();
        iv.introduced = "3.30.0.CR2";
        iv.fixed = "3.30.1";
        p.intervals.add(iv);

        java.util.Map<String, org.apache.maven.artifact.versioning.ComparableVersion> parsed = new java.util.HashMap<>();
        org.junit.jupiter.api.Assertions.assertNull(QuarkusBomService.matching("3.30.0.CR1", p, parsed));
        org.junit.jupiter.api.Assertions.assertNotNull(QuarkusBomService.matching("3.30.0.CR2", p, parsed));
        org.junit.jupiter.api.Assertions.assertNotNull(QuarkusBomService.matching("3.30.0", p, parsed));
    }
}
