///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.quarkus.platform:quarkus-bom:3.36.3@pom
//DEPS io.quarkus:quarkus-bootstrap-maven-resolver
//DEPS io.quarkus:quarkus-devtools-registry-client
//DEPS org.junit.jupiter:junit-jupiter
//DEPS org.junit.platform:junit-platform-launcher
//SOURCES DataMapping.java
//SOURCES ../../src/main/java/io/quarkusio/categories/ComponentCategories.java
//SOURCES PlatformDescriptor.java

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkusio.categories.ComponentCategories.Category;
import io.quarkusio.categories.sync.PlatformDescriptor;
import io.quarkusio.categories.sync.PlatformDescriptor.Extension;
import io.quarkusio.categories.sync.PlatformDescriptor.Platform;

/**
 * The descriptor reading is tested against {@code test/platform-descriptor.json}, a made-up platform descriptor in the
 * platform's shape (fake extensions, a fake version 99.0.0). Run from this directory with
 * {@code jbang PlatformDescriptorTest.java}.
 */
public class PlatformDescriptorTest {

    private static Platform platform() throws Exception {
        return PlatformDescriptor.platform(ExtensionCatalog.fromFile(Path.of("test/platform-descriptor.json")));
    }

    private static Extension extension(String id) throws Exception {
        return platform().extensions().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void thePlatformVersionIsTheBoms() throws Exception {
        assertEquals("99.0.0", platform().version());
    }

    @Test
    void theCategoriesAreThePlatformsWithTheirNamesInThePlatformsOrder() throws Exception {
        assertEquals(List.of(new Category("web", "Web"), new Category("core", "Core"), new Category("reactive", "Reactive"),
                new Category("alt-languages", "Alternative languages"), new Category("miscellaneous", "Miscellaneous")),
                platform().categories());
    }

    @Test
    void extensionsAreNamedAsTheDataNamesThem() throws Exception {
        assertEquals(List.of("org.example:fake-kotlin", "quarkus-core", "quarkus-fake-bare", "quarkus-fake-web"),
                platform().extensions().stream().map(Extension::id).toList());
    }

    @Test
    void anExtensionKeepsOnlyThePlatformsCategoriesInThePlatformsOrderWithoutDuplicates() throws Exception {
        Extension web = extension("quarkus-fake-web");
        assertEquals(List.of("web", "reactive"), web.categories());
        assertEquals("99.0.0", web.version());
    }

    @Test
    void anExtensionWithoutMetadataHasNoCategories() throws Exception {
        assertEquals(List.of(), extension("quarkus-fake-bare").categories());
    }

    public static void main(String[] args) {
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(
                LauncherDiscoveryRequestBuilder.request().selectors(selectClass(PlatformDescriptorTest.class)).build(), listener);
        listener.getSummary().printTo(new PrintWriter(System.out));
        listener.getSummary().printFailuresTo(new PrintWriter(System.out));
        System.exit(listener.getSummary().getTotalFailureCount() == 0 ? 0 : 1);
    }
}
