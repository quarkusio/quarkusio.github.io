package io.quarkusio.cves;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Artifact id prefix (first match wins) to category. */
    private static final Map<String, String> RULES = new LinkedHashMap<>();
    static {
        // Quarkus's own
        RULES.put("quarkus-vertx-http", "Web");
        RULES.put("quarkus-rest", "Web");
        RULES.put("quarkus-", "Core");
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
        RULES.put("org.jboss.resteasy:", "Web");
        RULES.put("org.asynchttpclient:", "Web");
        RULES.put("org.apache.httpcomponents.", "Web");
        RULES.put("com.fasterxml.jackson.", "Serialization");
        RULES.put("tools.jackson.", "Serialization");
        RULES.put("org.apache.avro:", "Serialization");
        RULES.put("org.mariadb.", "Data");
        RULES.put("org.postgresql:", "Data");
        RULES.put("com.microsoft.sqlserver:", "Data");
        RULES.put("org.hibernate.", "Data");
        RULES.put("org.apache.kafka:", "Messaging");
        RULES.put("org.apache.qpid:", "Messaging");
        RULES.put("org.bouncycastle:", "Security");
        RULES.put("org.keycloak:", "Security");
        RULES.put("com.ongres.scram:", "Security");
        RULES.put("io.opentelemetry:", "Observability");
        RULES.put("io.micrometer:", "Observability");
        RULES.put("org.apache.logging.log4j:", "Core");
    }

    /** The categories the page offers. */
    public List<String> names() {
        return NAMES;
    }

    public boolean isInternal(String artifact) {
        return artifact.startsWith("quarkus-");
    }

    public String origin(String artifact) {
        return isInternal(artifact) ? INTERNAL : EXTERNAL;
    }

    public String category(String artifact) {
        for (Map.Entry<String, String> rule : RULES.entrySet()) {
            if (artifact.startsWith(rule.getKey())) {
                return rule.getValue();
            }
        }
        return OTHER;
    }
}
