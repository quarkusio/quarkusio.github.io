package io.quarkusio.categories;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import io.quarkusio.categories.ComponentCategories.Category;
import io.quarkusio.categories.ComponentCategories.Origin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the real data file, read by Roq as the site reads it. When one fails, run tools/componentcategories-sync
 * (see its README).
 */
@QuarkusTest
class ComponentCategoriesDataTest {

    @Inject
    ComponentCategories data;

    private Set<String> categoryIds() {
        return data.categories().stream().map(Category::id).collect(Collectors.toSet());
    }

    @Test
    void theDataIsForAPlatformAndHasComponents() {
        assertNotNull(data.platform());
        assertFalse(data.components().isEmpty());
    }

    @Test
    void everyCategoryHasANameFromThePlatform() {
        data.categories().forEach(c -> assertFalse(c.name() == null || c.name().isBlank(), c.id() + " has no name"));
        assertEquals(data.categories().size(), categoryIds().size(), "Categories are listed once each");
    }

    @Test
    void everyComponentCategoryIsOneOfTheListedCategories() {
        data.components().forEach((id, c) -> {
            assertFalse(c.categories().isEmpty(), id + " has no category");
            assertTrue(categoryIds().containsAll(c.categories()), id + " has a category outside " + categoryIds());
        });
        assertTrue(categoryIds().contains(ComponentCategories.CORE));
    }

    @Test
    void originFollowsTheNameQuarkusOwnComponentsHaveNoGroup() {
        data.components().forEach((id, c) -> assertEquals(Origin.of(id), c.origin(), id));
    }

    /**
     * The sync makes anything that extensions of 3 or more categories bring in core, and nothing else. Third-party
     * components are (almost all) libraries, so have at most two categories.
     */
    @Test
    void aThirdPartyComponentHasAtMostTwoCategories() {
        data.components().forEach((id, c) -> {
            if (c.origin() == Origin.THIRD_PARTY) {
                assertTrue(c.categories().size() <= 2, id + " should be core: " + c.categories());
            }
        });
    }

    @Test
    void artifactsInGroupsUnderIoQuarkusAreQuarkus() {
        data.components().keySet().stream().filter(id -> id.startsWith("io.quarkus."))
                .forEach(id -> assertEquals(Origin.QUARKUS, data.components().get(id).origin(), id));
    }
}
