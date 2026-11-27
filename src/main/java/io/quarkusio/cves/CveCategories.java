package io.quarkusio.cves;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Which part of Quarkus a component belongs to, and whether it is Quarkus's own or a library it uses.
 * <p>
 * TEMPORARY: hardcoded. Quarkus's own components ("quarkus-*") are internal; everything else, a Maven
 * groupId:artifactId, is external. A library gets the category of the Quarkus extensions that bring it in, in the
 * sense of the extension categories in the extension catalog. The first matching rule wins. To be replaced by the
 * real extension metadata.
 */
@ApplicationScoped
@Named("cveCategories")
public class CveCategories {

    public static final String INTERNAL = "internal";
    public static final String EXTERNAL = "external";
    public static final String OTHER = "Miscellaneous";

    /** Category names, in the order the page lists them. */
    static final List<String> NAMES = List.of("Core", "Web", "Data", "Messaging", "Security", "Serialization",
            "Observability", OTHER);

    /** Artifact id prefix (first match wins, so the specific ones come first) to category. */
    private static final Map<String, String> RULES = new LinkedHashMap<>();
    static {
        // Quarkus's own, by extension
        RULES.put("quarkus-oidc", "Security");
        RULES.put("quarkus-security", "Security");
        RULES.put("quarkus-smallrye-jwt", "Security");
        RULES.put("quarkus-keycloak", "Security");
        RULES.put("quarkus-csrf", "Security");
        RULES.put("quarkus-elytron", "Security");
        RULES.put("quarkus-vertx-http", "Web");
        RULES.put("quarkus-rest", "Web");
        RULES.put("quarkus-resteasy", "Web");
        RULES.put("quarkus-undertow", "Web");
        RULES.put("quarkus-websockets", "Web");
        RULES.put("quarkus-smallrye-graphql", "Web");
        RULES.put("quarkus-hibernate", "Data");
        RULES.put("quarkus-jdbc", "Data");
        RULES.put("quarkus-panache", "Data");
        RULES.put("quarkus-mongodb", "Data");
        RULES.put("quarkus-redis", "Data");
        RULES.put("quarkus-infinispan", "Data");
        RULES.put("quarkus-kafka", "Messaging");
        RULES.put("quarkus-amqp", "Messaging");
        RULES.put("quarkus-artemis", "Messaging");
        RULES.put("quarkus-messaging", "Messaging");
        RULES.put("quarkus-opentelemetry", "Observability");
        RULES.put("quarkus-micrometer", "Observability");
        RULES.put("quarkus-jackson", "Serialization");
        RULES.put("quarkus-jsonb", "Serialization");
        RULES.put("quarkus-jaxb", "Serialization");
        RULES.put("quarkus-", "Core");
        RULES.put("io.quarkus.resteasy.reactive:", "Web");
        RULES.put("io.quarkus.", "Core");
        // libraries
        RULES.put("io.netty:netty-codec-redis", "Data");
        RULES.put("io.netty:netty-codec-mqtt", "Messaging");
        RULES.put("io.netty:netty-codec-stomp", "Messaging");
        RULES.put("io.netty:netty-codec-smtp", "Messaging");
        RULES.put("io.netty:netty-codec-http", "Web");
        RULES.put("io.netty:netty-handler", "Web");
        RULES.put("io.netty:", "Core");
        RULES.put("io.vertx:", "Web");
        RULES.put("io.grpc:", "Web");
        RULES.put("io.undertow:", "Web");
        RULES.put("org.eclipse.jetty:", "Web");
        RULES.put("org.springframework.security:", "Security");
        RULES.put("org.springframework:spring-web", "Web");
        RULES.put("org.webjars:", "Web");
        RULES.put("org.jboss.resteasy:", "Web");
        RULES.put("org.asynchttpclient:", "Web");
        RULES.put("org.apache.httpcomponents.", "Web");
        RULES.put("com.fasterxml.jackson.", "Serialization");
        RULES.put("tools.jackson.", "Serialization");
        RULES.put("org.apache.avro:", "Serialization");
        RULES.put("org.yaml:", "Serialization");
        RULES.put("com.google.protobuf:", "Serialization");
        RULES.put("org.mariadb.", "Data");
        RULES.put("org.postgresql:", "Data");
        RULES.put("com.microsoft.sqlserver:", "Data");
        RULES.put("mysql:", "Data");
        RULES.put("com.h2database:", "Data");
        RULES.put("org.hibernate", "Data");
        RULES.put("org.mongodb:", "Data");
        RULES.put("org.infinispan", "Data");
        RULES.put("org.liquibase:", "Data");
        RULES.put("org.apache.kafka:", "Messaging");
        RULES.put("org.apache.qpid:", "Messaging");
        RULES.put("org.apache.activemq:", "Messaging");
        RULES.put("org.bouncycastle:", "Security");
        RULES.put("org.keycloak:", "Security");
        RULES.put("com.ongres.scram:", "Security");
        RULES.put("org.wildfly.security:", "Security");
        RULES.put("com.nimbusds:", "Security");
        RULES.put("org.bitbucket.b_c:", "Security");
        RULES.put("io.smallrye:smallrye-jwt", "Security");
        RULES.put("io.opentelemetry", "Observability");
        RULES.put("io.micrometer:", "Observability");
        RULES.put("org.apache.logging.log4j:", "Core");
        RULES.put("log4j:", "Core");
    }

    private static final String QUARKUS_GROUP = "io.quarkus:";
    /** Words that are no use as a project name. */
    private static final Set<String> GENERIC = Set.of("org", "com", "io", "net", "at", "java", "javax", "jakarta", "core",
            "jdbc", "client", "api", "impl", "common", "commons", "security", "sun");

    /** The categories the page offers. */
    public List<String> names() {
        return NAMES;
    }

    /** The chart data names Quarkus's own artifacts without their groupId, but the sync tool may write it in full. */
    private static String bare(String artifact) {
        return artifact.startsWith(QUARKUS_GROUP) ? artifact.substring(QUARKUS_GROUP.length()) : artifact;
    }

    public boolean isInternal(String artifact) {
        return bare(artifact).startsWith("quarkus-") || artifact.startsWith("io.quarkus.");
    }

    public String origin(String artifact) {
        return isInternal(artifact) ? INTERNAL : EXTERNAL;
    }

    public String category(String artifact) {
        String name = bare(artifact);
        for (Map.Entry<String, String> rule : RULES.entrySet()) {
            if (name.startsWith(rule.getKey())) {
                return rule.getValue();
            }
        }
        return OTHER;
    }

    /**
     * A guess at the upstream project, so that the components of one project sit together: the first word of the
     * artifact id when the groupId has it too (jackson-databind in com.fasterxml.jackson.core is jackson), otherwise the
     * last useful part of the groupId. Quarkus's own are all quarkus. TEMPORARY, like the categories.
     */
    public String project(String artifact) {
        if (isInternal(artifact)) {
            return "quarkus";
        }
        int colon = artifact.indexOf(':');
        String group = colon < 0 ? "" : artifact.substring(0, colon).toLowerCase(java.util.Locale.ROOT);
        String id = (colon < 0 ? artifact : artifact.substring(colon + 1)).toLowerCase(java.util.Locale.ROOT);
        String first = id.split("[-_.]")[0];
        if (first.length() >= 3 && !GENERIC.contains(first) && group.contains(first)) {
            return first;
        }
        String[] parts = group.split("\\.");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].isEmpty() && !GENERIC.contains(parts[i])) {
                return parts[i];
            }
        }
        return first.isEmpty() ? artifact : first;
    }
}
