package io.quarkusio.categories;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkiverse.roq.data.runtime.annotations.DataMapping;

/**
 * The shape of _data/componentcategories.yaml, bound by Roq at build time, and also written by the sync that keeps
 * the file current (tools/componentcategories-sync), which compiles this class.
 * <p>
 * It says which Quarkus platform extension categories each component of the platform belongs to, and whether it is
 * one of Quarkus's own or a third-party dependency. Components are named by {@link #componentId}.
 * {@code categories} is every category a component has, in the platform's order, with the platform's display names;
 * {@code platform} is the platform version the data was worked out from.
 */
@DataMapping("componentcategories")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComponentCategories(String platform, List<Category> categories, Map<String, Component> components) {

    /** The category used when nothing better is known. */
    public static final String MISCELLANEOUS = "miscellaneous";

    /** The category for libraries that are used all over the platform. */
    public static final String CORE = "core";

    private static final String QUARKUS_GROUP = "io.quarkus";

    public ComponentCategories {
        categories = categories == null ? List.of() : categories;
        components = components == null ? Map.of() : components;
    }

    /** Where a component comes from. */
    public enum Origin {
        /** One of Quarkus's own artifacts (group io.quarkus, or one under it such as io.quarkus.arc). */
        @JsonProperty("quarkus")
        QUARKUS,
        /** A library that Quarkus extensions use. */
        @JsonProperty("third-party")
        THIRD_PARTY;

        /** The origin of a component, from its {@link #componentId}. */
        public static Origin of(String componentId) {
            int colon = componentId.indexOf(':');
            return colon < 0 || componentId.substring(0, colon).startsWith(QUARKUS_GROUP + ".") ? QUARKUS : THIRD_PARTY;
        }
    }

    /**
     * One component. {@code categories} are platform category ids, never empty.
     * <p>
     * The sync knows which platform extensions bring each component in (the evidence for its categories) but does not
     * write them, since nothing uses them and they make up most of the file. If the evidence is ever wanted, it is the
     * {@code bringIn} set in the sync's CategoryAssignment.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Component(Origin origin, List<String> categories) {
    }

    /** A platform category: its id, and its display name as the platform gives it. */
    public record Category(String id, String name) {
    }

    /**
     * How a component is named in the data: {@code artifactId} for Quarkus's own artifacts (group {@code io.quarkus}),
     * {@code groupId:artifactId} for everything else.
     */
    public static String componentId(String groupId, String artifactId) {
        return QUARKUS_GROUP.equals(groupId) ? artifactId : groupId + ":" + artifactId;
    }
}
