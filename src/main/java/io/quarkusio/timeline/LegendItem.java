package io.quarkusio.timeline;

/** kind prefixed with "prov-" marks a provider colour key, rendered on its own row. */
public record LegendItem(String kind, String label, String description) {

    public boolean provider() {
        return kind.startsWith("prov-");
    }
}
