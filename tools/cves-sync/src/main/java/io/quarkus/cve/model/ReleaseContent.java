package io.quarkus.cve.model;

import io.quarkus.cve.service.QuarkusBomService;
import java.util.Map;

/**
 * What one release ships, as far as versions go: the components its source manages and the version of each.
 * "Managed" is not "on the classpath": a BOM says which version an application gets if it uses the component.
 *
 * @param version the Quarkus release, as in releases.yaml
 * @param source  where the content was read from, e.g. {@code maven://io.quarkus.platform:quarkus-bom:3.39.1}
 * @param managed {@code groupId:artifactId -> version}
 */
public record ReleaseContent(String version, String source, Map<String, String> managed) {

    /** True for Quarkus core and its extensions, false for the dependencies they bring in. */
    public static boolean isCore(String groupArtifact) {
        return QuarkusBomService.isQuarkusArtifact(groupArtifact);
    }
}
