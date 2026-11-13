package io.quarkusio.categories.sync;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.quarkusio.categories.ComponentCategories;
import io.quarkusio.categories.ComponentCategories.Category;
import io.quarkusio.categories.ComponentCategories.Component;
import io.quarkusio.categories.ComponentCategories.Origin;
import io.quarkusio.categories.sync.PlatformDescriptor.Extension;
import io.quarkusio.categories.sync.PlatformDescriptor.Platform;

/**
 * Decides the platform categories of each component of the platform: every platform extension, and every artifact
 * that one of them brings in. The rules:
 * <ul>
 * <li>A component that is itself a platform extension has that extension's categories.</li>
 * <li>Anything else (a third-party library, or a Quarkus module that is not an extension) has the categories of the
 * platform extensions that bring it in, that is, whose dependencies include it.</li>
 * <li>A library that extensions of {@link #CORE_THRESHOLD} or more categories bring in is used all over the platform,
 * so it is core.</li>
 * <li>A component with no categories (an extension without any, or a library only such extensions bring in) is
 * miscellaneous.</li>
 * </ul>
 * Only the platform's categories are ever used, and the data lists those that some component has, in the platform's
 * order.
 */
public final class CategoryAssignment {

    /** A library brought in by extensions of this many categories or more is core. */
    public static final int CORE_THRESHOLD = 3;

    private CategoryAssignment() {
    }

    /**
     * @param platform the platform's extensions and categories
     * @param dependencies for each extension (by {@link Extension#id}), every artifact it depends on, directly or not,
     *        named the same way
     */
    public static ComponentCategories assign(Platform platform, Map<String, Set<String>> dependencies) {
        Map<String, Extension> extensions = new TreeMap<>();
        platform.extensions().forEach(e -> extensions.put(e.id(), e));

        Set<String> components = new TreeSet<>(extensions.keySet());
        dependencies.values().forEach(components::addAll);

        Map<String, Component> placed = new TreeMap<>();
        for (String component : components) {
            placed.put(component, place(component, extensions, dependencies));
        }
        Set<String> used = new TreeSet<>();
        placed.values().forEach(c -> used.addAll(c.categories()));
        List<Category> categories = platform.categories().stream().filter(c -> used.contains(c.id())).toList();
        return new ComponentCategories(platform.version(), categories, placed);
    }

    private static Component place(String component, Map<String, Extension> extensions,
            Map<String, Set<String>> dependencies) {
        Origin origin = Origin.of(component);

        Extension self = extensions.get(component);
        if (self != null) {
            return new Component(origin, orMiscellaneous(self.categories().stream().sorted().toList()));
        }

        // the extensions that bring the component in are the evidence for its categories; they are not written out
        // (see ComponentCategories.Component), but could be, from here
        Set<String> bringIn = new TreeSet<>();
        extensions.forEach((id, extension) -> {
            if (dependencies.getOrDefault(id, Set.of()).contains(component)) {
                bringIn.add(id);
            }
        });
        Set<String> categories = new TreeSet<>();
        bringIn.forEach(id -> categories.addAll(extensions.get(id).categories()));
        List<String> chosen = categories.size() >= CORE_THRESHOLD ? List.of(ComponentCategories.CORE)
                : orMiscellaneous(List.copyOf(categories));
        return new Component(origin, chosen);
    }

    private static List<String> orMiscellaneous(List<String> categories) {
        return categories.isEmpty() ? List.of(ComponentCategories.MISCELLANEOUS) : categories;
    }
}
