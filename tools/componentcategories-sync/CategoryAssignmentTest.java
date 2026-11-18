///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.36.3@pom
//DEPS io.quarkus:quarkus-bootstrap-maven-resolver
//DEPS io.quarkus:quarkus-devtools-registry-client
//DEPS org.junit.jupiter:junit-jupiter
//DEPS org.junit.platform:junit-platform-launcher
//SOURCES DataMapping.java
//SOURCES ../../src/main/java/io/quarkusio/categories/ComponentCategories.java
//SOURCES PlatformDescriptor.java
//SOURCES CategoryAssignment.java

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import io.quarkusio.categories.ComponentCategories;
import io.quarkusio.categories.ComponentCategories.Category;
import io.quarkusio.categories.ComponentCategories.Component;
import io.quarkusio.categories.ComponentCategories.Origin;
import io.quarkusio.categories.sync.CategoryAssignment;
import io.quarkusio.categories.sync.PlatformDescriptor.Extension;
import io.quarkusio.categories.sync.PlatformDescriptor.Platform;

/** Run from this directory with {@code jbang CategoryAssignmentTest.java}. */
public class CategoryAssignmentTest {

    private static Extension ext(String artifactId, String... categories) {
        return new Extension("io.quarkus", artifactId, "3.40.0", List.of(categories));
    }

    private static final List<Category> CATEGORIES = List.of(new Category("web", "Web"), new Category("data", "Data"),
            new Category("messaging", "Messaging"), new Category("core", "Core"), new Category("reactive", "Reactive"),
            new Category("observability", "Observability"), new Category("security", "Security"),
            new Category("ai", "Artificial Intelligence (AI)"), new Category("miscellaneous", "Miscellaneous"));

    private static final Platform PLATFORM = new Platform("3.40.0", CATEGORIES, List.of(
            ext("quarkus-vertx-http", "web", "reactive"),
            ext("quarkus-rest", "web", "reactive"),
            ext("quarkus-jdbc-mariadb", "data"),
            ext("quarkus-kafka-client", "messaging"),
            ext("quarkus-oidc", "security"),
            ext("quarkus-micrometer", "observability"),
            ext("quarkus-quiet")));

    private static final Map<String, Set<String>> DEPENDENCIES = Map.of(
            "quarkus-jdbc-mariadb", Set.of("org.mariadb.jdbc:mariadb-java-client", "io.netty:netty-buffer"),
            "quarkus-vertx-http", Set.of("io.vertx:vertx-web", "io.netty:netty-buffer", "quarkus-core"),
            "quarkus-rest", Set.of("io.vertx:vertx-web", "io.netty:netty-buffer"),
            "quarkus-kafka-client", Set.of("org.apache.kafka:kafka-clients", "io.netty:netty-buffer"),
            "quarkus-oidc", Set.of("io.netty:netty-buffer", "io.quarkus.arc:arc"));

    private static ComponentCategories assign() {
        return CategoryAssignment.assign(PLATFORM, DEPENDENCIES);
    }

    private static Component component(String id) {
        return assign().components().get(id);
    }

    @Test
    void aQuarkusExtensionHasItsOwnCategories() {
        Component c = component("quarkus-vertx-http");
        assertEquals(Origin.QUARKUS, c.origin());
        assertEquals(List.of("reactive", "web"), c.categories());
    }

    @Test
    void anArtifactInAGroupUnderIoQuarkusIsQuarkus() {
        assertEquals(Origin.QUARKUS, component("io.quarkus.arc:arc").origin());
        assertEquals(Origin.THIRD_PARTY, Origin.of("io.quarkusx:fake"), "Only io.quarkus and groups under it");
    }

    @Test
    void aLibraryHasTheCategoriesOfTheExtensionsThatBringItIn() {
        Component c = component("org.mariadb.jdbc:mariadb-java-client");
        assertEquals(Origin.THIRD_PARTY, c.origin());
        assertEquals(List.of("data"), c.categories());
    }

    @Test
    void aLibraryBroughtInByTwoCategoriesHasBoth() {
        Component c = component("io.vertx:vertx-web");
        assertEquals(List.of("reactive", "web"), c.categories());
    }

    @Test
    void aLibraryBroughtInByThreeOrMoreCategoriesIsCore() {
        // netty-buffer: web, reactive, data, messaging, security
        Component c = component("io.netty:netty-buffer");
        assertEquals(List.of(ComponentCategories.CORE), c.categories());
    }

    @Test
    void theThresholdIsThreeCategoriesNotTwo() {
        assertEquals(3, CategoryAssignment.CORE_THRESHOLD);
        // two categories (web, reactive) are not enough
        assertEquals(List.of("reactive", "web"), component("io.vertx:vertx-web").categories());
        Platform three = new Platform("3.40.0", CATEGORIES, List.of(ext("quarkus-a", "web"), ext("quarkus-b", "data"), ext("quarkus-c", "security")));
        Map<String, Set<String>> deps = Map.of("quarkus-a", Set.of("x:y"), "quarkus-b", Set.of("x:y"), "quarkus-c", Set.of("x:y"));
        assertEquals(List.of("core"), CategoryAssignment.assign(three, deps).components().get("x:y").categories());
    }

    @Test
    void theComponentsAreTheExtensionsAndEverythingTheyBringIn() {
        assertEquals(Set.of("quarkus-vertx-http", "quarkus-rest", "quarkus-jdbc-mariadb", "quarkus-kafka-client", "quarkus-oidc",
                "quarkus-micrometer", "quarkus-quiet", "quarkus-core", "io.vertx:vertx-web", "io.netty:netty-buffer",
                "org.mariadb.jdbc:mariadb-java-client", "org.apache.kafka:kafka-clients", "io.quarkus.arc:arc"), assign().components().keySet());
    }

    @Test
    void aLibraryOnlyExtensionsWithoutCategoriesBringInIsMiscellaneous() {
        Platform quiet = new Platform("3.40.0", CATEGORIES, List.of(ext("quarkus-quiet")));
        Component c = CategoryAssignment.assign(quiet, Map.of("quarkus-quiet", Set.of("com.example:lonely")))
                .components().get("com.example:lonely");
        assertEquals(List.of(ComponentCategories.MISCELLANEOUS), c.categories());
    }

    @Test
    void anExtensionWithoutCategoriesIsMiscellaneous() {
        assertEquals(List.of(ComponentCategories.MISCELLANEOUS),
                component("quarkus-quiet").categories());
    }

    @Test
    void aQuarkusModuleThatIsNotAnExtensionIsPlacedLikeALibrary() {
        // quarkus-core is not in the catalog, but quarkus-vertx-http depends on it
        Component c = component("quarkus-core");
        assertEquals(Origin.QUARKUS, c.origin());
        assertEquals(List.of("reactive", "web"), c.categories());
    }

    @Test
    void theCategoriesAreThoseSomeComponentHasWithNamesInThePlatformsOrder() {
        assertEquals(List.of(new Category("web", "Web"), new Category("data", "Data"), new Category("messaging", "Messaging"),
                new Category("core", "Core"), new Category("reactive", "Reactive"), new Category("observability", "Observability"),
                new Category("security", "Security"), new Category("miscellaneous", "Miscellaneous")),
                assign().categories());
    }

    @Test
    void componentsAreInAlphabeticalOrderSoTheFileIsStable() {
        List<String> ids = List.copyOf(assign().components().keySet());
        assertEquals(ids.stream().sorted().toList(), ids);
    }

    public static void main(String[] args) {
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(
                LauncherDiscoveryRequestBuilder.request().selectors(selectClass(CategoryAssignmentTest.class)).build(), listener);
        listener.getSummary().printTo(new PrintWriter(System.out));
        listener.getSummary().printFailuresTo(new PrintWriter(System.out));
        System.exit(listener.getSummary().getTotalFailureCount() == 0 ? 0 : 1);
    }
}
