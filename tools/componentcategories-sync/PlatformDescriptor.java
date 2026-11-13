package io.quarkusio.categories.sync;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.artifact.DefaultArtifact;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.ExtensionCatalogResolver;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.util.PlatformArtifacts;
import io.quarkusio.categories.ComponentCategories;
import io.quarkusio.categories.ComponentCategories.Category;

/**
 * The extensions of the Quarkus platform, with their categories, from the platform's own descriptor
 * ({@code io.quarkus.platform:quarkus-bom-quarkus-platform-descriptor}), read with Quarkus's registry client as the
 * Quarkus tooling and code.quarkus.io read it.
 * <p>
 * The descriptor has only the platform BOM's own extensions (not those of member BOMs such as Camel, or of the
 * registry's non-platform list), and the platform's categories with their display names, in the platform's order.
 */
public final class PlatformDescriptor {

    public static final String PLATFORM_GROUP_ID = "io.quarkus.platform";
    public static final String PLATFORM_BOM = "quarkus-bom";

    /** A platform extension: its runtime artifact and categories (ids, in the platform's order). */
    public record Extension(String groupId, String artifactId, String version, List<String> categories) {

        /** The extension as a component of the data, see {@link ComponentCategories#componentId}. */
        public String id() {
            return ComponentCategories.componentId(groupId, artifactId);
        }
    }

    /** The platform's version, categories (in the platform's order) and extensions. */
    public record Platform(String version, List<Category> categories, List<Extension> extensions) {
    }

    private PlatformDescriptor() {
    }

    /** The version of the platform that the Quarkus registry recommends, as {@code quarkus create} would use. */
    public static String recommendedVersion(MavenArtifactResolver resolver) throws Exception {
        Collection<ArtifactCoords> boms = ExtensionCatalogResolver.builder().artifactResolver(resolver).build()
                .resolvePlatformCatalog().getRecommendedPlatform().getRecommendedStream().getRecommendedRelease()
                .getMemberBoms();
        return boms.stream()
                .filter(b -> b.getGroupId().equals(PLATFORM_GROUP_ID) && b.getArtifactId().equals(PLATFORM_BOM))
                .map(ArtifactCoords::getVersion).findFirst()
                .orElseThrow(() -> new IllegalStateException("The recommended platform release has no " + PLATFORM_BOM + ": " + boms));
    }

    /** Downloads the descriptor of the given platform version. */
    public static ExtensionCatalog resolve(MavenArtifactResolver resolver, String version) throws Exception {
        ArtifactCoords descriptor = PlatformArtifacts.getCatalogArtifactForBom(PLATFORM_GROUP_ID, PLATFORM_BOM, version);
        Path json = resolver.resolve(new DefaultArtifact(descriptor.getGroupId(), descriptor.getArtifactId(),
                descriptor.getClassifier(), descriptor.getType(), descriptor.getVersion())).getArtifact().getFile().toPath();
        return ExtensionCatalog.fromFile(json);
    }

    /**
     * The platform as this tool uses it. An extension keeps only the categories that the platform defines (in the
     * platform's order); the platform version is the descriptor's.
     */
    public static Platform platform(ExtensionCatalog catalog) {
        List<Category> categories = catalog.getCategories().stream().map(c -> new Category(c.getId(), c.getName()))
                .toList();
        Map<String, Integer> order = new HashMap<>();
        categories.forEach(c -> order.putIfAbsent(c.id(), order.size()));
        List<Extension> extensions = catalog.getExtensions().stream().map(e -> {
            ArtifactCoords a = e.getArtifact();
            List<String> own = categoriesOf(e.getMetadata()).stream().filter(order::containsKey).distinct()
                    .sorted(Comparator.comparing(order::get)).toList();
            return new Extension(a.getGroupId(), a.getArtifactId(), a.getVersion(), own);
        }).sorted(Comparator.comparing(Extension::id)).toList();
        return new Platform(catalog.getBom().getVersion(), categories, extensions);
    }

    private static List<String> categoriesOf(Map<String, Object> metadata) {
        Object categories = metadata == null ? null : metadata.get("categories");
        return categories instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
    }
}
