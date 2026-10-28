package io.quarkus.cve.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * Serializes date strings as raw unquoted scalars in YAML (e.g. 2026-09-30 instead of "2026-09-30")
 * so that YAML parsers (like SnakeYAML in Roq) load them as timestamps/Dates rather than Strings.
 */
public class UnquotedDateSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeRawValue(value);
        }
    }
}
