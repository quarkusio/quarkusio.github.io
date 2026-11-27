package io.quarkusio.cves;

import java.util.Locale;

/** Declared lowest first. */
public enum Severity {

    LOW("Low"), MEDIUM("Medium"), HIGH("High"), CRITICAL("Critical");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** The CSS suffix, e.g. "critical". */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Null when the value is missing or not one of the four known levels. */
    public static Severity parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
