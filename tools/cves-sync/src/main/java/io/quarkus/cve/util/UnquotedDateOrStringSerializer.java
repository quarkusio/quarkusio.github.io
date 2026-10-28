package io.quarkus.cve.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Serializes fields like herodevs_eol that can be either a date string (e.g. 2026-10-31)
 * or a symbolic string like "nes". If it matches YYYY-MM-DD, writes it unquoted; otherwise writes as string.
 */
public class UnquotedDateOrStringSerializer extends JsonSerializer<Object> {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value instanceof String str && DATE_PATTERN.matcher(str).matches()) {
            gen.writeRawValue(str);
        } else {
            gen.writeObject(value);
        }
    }
}
