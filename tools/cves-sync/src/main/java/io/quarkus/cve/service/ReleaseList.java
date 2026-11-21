package io.quarkus.cve.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the list of releases from releases.yaml, which owns it: every community micro of every release
 * line, with its date, the release it was built from, and whether the platform has a BOM for it.
 * Vendor builds (the {@code rhbq} and {@code ibm} blocks) are not read.
 */
public final class ReleaseList {

    /**
     * @param version          e.g. {@code 3.39.1}
     * @param date             {@code yyyy-MM-dd}, or null when releases.yaml has none
     * @param parent           the release this one was built from, or null
     * @param hasPlatformBom   true when releases.yaml lists a platform BOM for it ({@code bom_url})
     */
    public record Micro(String version, String date, String parent, boolean hasPlatformBom) {}

    private ReleaseList() {}

    public static List<Micro> read(File file) throws IOException {
        JsonNode root = new YAMLMapper().readTree(file);
        List<Micro> out = new ArrayList<>();
        for (JsonNode release : root.path("releases")) {
            for (JsonNode micro : release.path("micros")) {
                String version = text(micro, "version");
                if (version == null) continue;
                out.add(new Micro(version, text(micro, "release_date"), text(micro, "parent_release"),
                        micro.hasNonNull("bom_url")));
            }
        }
        return out;
    }

    /**
     * The advisories are placed on the release tree, so releases.yaml must say which release each was built from.
     * Without {@code parent_release} every release would be a root of its own and no cut would mean anything.
     *
     * @throws IllegalStateException when there are releases and none has a parent
     */
    public static void requireLineage(List<Micro> micros) {
        if (!micros.isEmpty() && micros.stream().noneMatch(m -> m.parent() != null)) {
            throw new IllegalStateException("none of the " + micros.size() + " releases has a parent_release; "
                    + "releases.yaml needs the release lineage (tools/releases-sync, 'parent_release')");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
