package io.quarkus.cve.util;

/**
 * pt.quarkus.io translates the data files with po4a, and CI runs po4a over every YAML file. Its YAML parser takes a line
 * with {@code : [} or {@code : {} for an inline list or map, even inside a quoted string, and refuses the file.
 */
public final class Po4a {

    private Po4a() {}

    /**
     * Free text (an advisory title) as the data file can hold it: {@code Netty: [codec-http2] Lack of ...} is written
     * as {@code Netty [codec-http2] Lack of ...}.
     */
    public static String safe(String text) {
        if (text == null) return null;
        String s = text;
        while (s.contains(": [") || s.contains(": {")) {
            s = s.replace(": [", " [").replace(": {", " {");
        }
        return s;
    }
}
