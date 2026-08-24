package io.quarkusio;

import org.asciidoctor.*;
import org.asciidoctor.ast.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AsciidocConversionTest {

    private static Document loadDoc(String input) {
        Asciidoctor asciidoctor = Asciidoctor.Factory.create();
        return asciidoctor.load(input, Options.builder()
                .safe(SafeMode.SAFE)
                .backend("html5")
                .attributes(Attributes.builder()
                        .attribute("noheader@", "")
                        .attribute("icons", "font")
                        .build())
                .build());
    }

    @Test
    void searchableConfigTableWithDescriptionBlockRenders() {
        String input = """
                = All configuration options

                [.configuration-legend]
                icon:lock[title=Fixed at build time] Configuration property fixed at build time

                [.configuration-reference.searchable, cols="80,.^10,.^10"]
                |===

                h|Property
                h|Type
                h|Default

                a|icon:lock[title=Fixed at build time] `quarkus.test.prop`

                [.description]
                --
                A test property description that is long enough to be collapsible.
                It has multiple lines of content to trigger the collapsible behavior.
                --
                |string
                |`test`

                |===
                """;

        String result = loadDoc(input).convert();

        assertTrue(result.contains("<table"),
                "A searchable configuration-reference table with .description blocks "
                        + "should render. This is the pattern used by /guides/all-config.");
        assertTrue(result.contains("quarkus.test.prop"),
                "Config property content should be present in the rendered table");
    }

    @Test
    void singleLineDescriptionIsNotCollapsible() {
        String input = """
                = Test

                [.configuration-reference.searchable, cols="80,.^10,.^10"]
                |===

                h|Property
                h|Type
                h|Default

                a|`quarkus.test.timeout`

                [.description]
                --
                Environment variable: `QUARKUS_TEST_TIMEOUT`
                --
                |int
                |`10`

                |===
                """;

        String result = loadDoc(input).convert();

        assertTrue(result.contains("<table"),
                "Table with non-collapsible description should render");
        assertTrue(result.contains("conf-non-collapsible-desc-"),
                "Non-collapsible description should get a non-collapsible ID");
        assertFalse(result.contains("description-decoration"),
                "Non-collapsible description should not get a chevron decoration");
    }
}
