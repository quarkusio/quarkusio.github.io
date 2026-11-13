package io.quarkus.cve.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * What the report needs from a cve.org record (CVE JSON 5): the id, state, publication date, title,
 * severity and the package names its CNA container lists as affected. Full records can list hundreds of
 * product builds, so only this is cached.
 *
 * cve.org is the root of CVE data, but it says which <em>products</em> are affected, not which Maven
 * versions: a record like Red Hat's names {@code io.quarkus/quarkus-vertx-http} and lists its own builds as
 * unaffected, with no upstream range. Version ranges can therefore only come from OSV and GitHub, and a
 * record found here without them is reported as pending.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CveOrgRecord(String cve, String state, String published, String title, String severity,
        List<String> packages) {

    /** Reads the parts of a record this tool uses. */
    public static CveOrgRecord from(JsonNode record) {
        JsonNode meta = record.path("cveMetadata");
        JsonNode cna = record.path("containers").path("cna");

        Set<String> packages = new TreeSet<>();
        for (JsonNode affected : cna.path("affected")) {
            String name = affected.path("packageName").asText("");
            if (!name.isBlank()) packages.add(name.trim());
        }
        String title = cna.path("title").asText("");
        if (title.isBlank()) {
            for (JsonNode d : cna.path("descriptions")) {
                if ("en".equals(d.path("lang").asText())) {
                    title = d.path("value").asText("");
                    break;
                }
            }
        }
        List<JsonNode> metrics = new ArrayList<>();
        cna.path("metrics").forEach(metrics::add);
        for (JsonNode adp : record.path("containers").path("adp")) adp.path("metrics").forEach(metrics::add);

        return new CveOrgRecord(meta.path("cveId").asText(null), meta.path("state").asText(null),
                published(meta.path("datePublished").asText(null)), title, severity(metrics), List.copyOf(packages));
    }

    /** Dropping the fraction of a second keeps the same form as the GitHub and OSV dates in the report. */
    static String published(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return Instant.parse(date).truncatedTo(ChronoUnit.SECONDS).toString();
        } catch (RuntimeException ex) {
            return date;
        }
    }

    /** CVSS base severity when a CVSS metric exists, otherwise Red Hat's own rating; null if neither. */
    static String severity(List<JsonNode> metrics) {
        for (String key : List.of("cvssV4_0", "cvssV3_1", "cvssV3_0")) {
            for (JsonNode m : metrics) {
                String s = m.path(key).path("baseSeverity").asText("");
                if (!s.isBlank() && !"NONE".equalsIgnoreCase(s)) return s.toUpperCase(Locale.ROOT);
            }
        }
        for (JsonNode m : metrics) {
            String rating = m.path("other").path("content").path("value").asText("").toLowerCase(Locale.ROOT);
            switch (rating) {
                case "critical": return "CRITICAL";
                case "important": return "HIGH";
                case "moderate": return "MEDIUM";
                case "low": return "LOW";
                default: break;
            }
        }
        return null;
    }

    /**
     * The managed artifacts ({@code group:artifact}) this record names. A name written
     * {@code group:artifact} or {@code group/artifact} must be managed as is; a bare artifact id, as Red Hat
     * often writes it, is matched to the managed artifacts with that id.
     */
    public List<String> managedArtifacts(Set<String> managed) {
        Map<String, Set<String>> byArtifactId = new HashMap<>();
        for (String ga : managed) {
            byArtifactId.computeIfAbsent(ga.substring(ga.indexOf(':') + 1), k -> new TreeSet<>()).add(ga);
        }
        Set<String> out = new LinkedHashSet<>();
        for (String name : packages == null ? List.<String>of() : packages) {
            String p = name.replace('/', ':');
            if (p.contains(":")) {
                if (managed.contains(p)) out.add(p);
            } else {
                out.addAll(byArtifactId.getOrDefault(p, Set.of()));
            }
        }
        return List.copyOf(new TreeSet<>(out));
    }
}
