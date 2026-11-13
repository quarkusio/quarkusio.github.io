package io.quarkus.cve.util;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Output formats of the {@code releases} command. */
public enum OutputFormat {
    PLAINTEXT_ANSI_COLOR("plaintext-ansi-color"),
    PLAINTEXT_NOCOLOR("plaintext-nocolor"),
    MARKDOWN("markdown"),
    JSON("json"),
    YAML("yaml");

    public static final OutputFormat DEFAULT = PLAINTEXT_ANSI_COLOR;

    public final String cliName;

    OutputFormat(String cliName) {
        this.cliName = cliName;
    }

    /** The names accepted on the command line, comma separated, for help and error messages. */
    public static String names() {
        return Arrays.stream(values()).map(f -> f.cliName).collect(Collectors.joining(", "));
    }

    /** Case-insensitive lookup by CLI name. */
    public static OutputFormat parse(String text) {
        String wanted = text == null ? "" : text.trim();
        for (OutputFormat f : values()) {
            if (f.cliName.equalsIgnoreCase(wanted)) return f;
        }
        throw new IllegalArgumentException("unknown format '" + text + "' (expected one of: " + names() + ")");
    }
}
