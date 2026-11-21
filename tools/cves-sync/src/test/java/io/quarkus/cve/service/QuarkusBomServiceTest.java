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
}
