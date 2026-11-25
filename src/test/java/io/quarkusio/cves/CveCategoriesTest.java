package io.quarkusio.cves;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CveCategoriesTest {

    final CveCategories categories = new CveCategories();

    private void assertCategory(String category, String... artifacts) {
        for (String artifact : artifacts) {
            assertEquals(category, categories.category(artifact), artifact);
        }
    }

    @Test
    void libraryCategories() {
        assertCategory("Web", "io.undertow:undertow-core", "org.eclipse.jetty:jetty-server", "org.eclipse.jetty:jetty-servlets",
                "org.springframework:spring-web", "org.webjars:bootstrap", "org.webjars:swagger-ui");
        assertCategory("Security", "org.springframework.security:spring-security-core",
                "org.wildfly.security:wildfly-elytron");
        assertCategory("Serialization", "org.yaml:snakeyaml", "com.google.protobuf:protobuf-java");
        assertCategory("Data", "mysql:mysql-connector-java", "com.h2database:h2");
        assertCategory("Messaging", "org.apache.activemq:artemis-server", "org.apache.activemq:artemis-core-client");
        assertCategory("Observability", "io.opentelemetry.javaagent:opentelemetry-javaagent", "io.opentelemetry:opentelemetry-api");
        assertCategory("Core", "log4j:log4j", "org.apache.logging.log4j:log4j-core");
    }

    @Test
    void quarkusExtensionCategories() {
        assertCategory("Security", "quarkus-oidc", "quarkus-security-webauthn", "quarkus-smallrye-jwt",
                "quarkus-keycloak-authorization");
        assertCategory("Web", "quarkus-vertx-http", "quarkus-rest", "quarkus-rest-jsonb", "quarkus-undertow");
        assertCategory("Core", "quarkus-core", "quarkus-qute", "quarkus-vertx", "quarkus-cache");
        assertCategory("Data", "quarkus-hibernate-orm-rest-data-panache");
    }

    @Test
    void fullCoordinatesOfQuarkusArtifactsAreStillQuarkusOwn() {
        assertTrue(categories.isInternal("quarkus-oidc"));
        assertTrue(categories.isInternal("io.quarkus:quarkus-oidc"));
        assertTrue(categories.isInternal("io.quarkus.resteasy.reactive:resteasy-reactive"));
        assertFalse(categories.isInternal("io.netty:netty-codec-http"));
        assertEquals("Security", categories.category("io.quarkus:quarkus-oidc"));
        assertEquals("Web", categories.category("io.quarkus.resteasy.reactive:resteasy-reactive"));
        assertEquals(CveCategories.EXTERNAL, categories.origin("org.yaml:snakeyaml"));
    }

    @Test
    void projectIsGuessedFromTheNames() {
        assertEquals("jackson", categories.project("com.fasterxml.jackson.core:jackson-databind"));
        assertEquals("jackson", categories.project("com.fasterxml.jackson.core:jackson-core"));
        assertEquals("netty", categories.project("io.netty:netty-codec-http"));
        assertEquals("mariadb", categories.project("org.mariadb.jdbc:mariadb-java-client"));
        assertEquals("spring", categories.project("org.springframework.security:spring-security-core"));
        assertEquals("log4j", categories.project("org.apache.logging.log4j:log4j-core"));
        assertEquals("kafka", categories.project("org.apache.kafka:kafka_2.12"));
        assertEquals("bouncycastle", categories.project("org.bouncycastle:bcprov-jdk18on"));
        assertEquals("quarkus", categories.project("quarkus-vertx-http"));
        assertEquals("quarkus", categories.project("io.quarkus:quarkus-vertx-http"));
    }
}
