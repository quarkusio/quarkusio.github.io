package io.quarkus.tools.migration.asciidoc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExtensionStatusTreeprocessorTest {

    static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry()
                .treeprocessor(new ExtensionStatusTreeprocessor());
    }

    @AfterAll
    static void cleanup() {
        asciidoctor.close();
    }

    private String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder().build());
    }

    @ParameterizedTest
    @CsvSource({
            "stable, status-stable",
            "preview, status-preview",
            "experimental, status-experimental",
            "deprecated, status-deprecated"
    })
    void insertsStatusLabel(String status, String cssClass) {
        String html = convert(":extension-status: " + status + "\n\n== My Guide\n\nSome content.");
        assertTrue(html.contains("class=\"status-label " + cssClass + "\""));
        assertTrue(html.contains(">" + status + "</a>"));
        assertTrue(html.contains("href=\"#extension-status-note\""));
    }

    @Test
    void stableHasCorrectTooltip() {
        String html = convert(":extension-status: stable\n\n== Guide\n\nContent.");
        assertTrue(html.contains("title=\"This extension's backward compatibility"));
    }

    @Test
    void previewHasCorrectTooltip() {
        String html = convert(":extension-status: preview\n\n== Guide\n\nContent.");
        assertTrue(html.contains("title=\"This extension's backward compatibility and presence in the ecosystem is not guaranteed\""));
    }

    @Test
    void noStatusAttributeLeavesDocumentUnchanged() {
        String html = convert("== Guide\n\nContent.");
        assertFalse(html.contains("status-label"));
    }

    @Test
    void labelAppearsBeforeContent() {
        String html = convert(":extension-status: stable\n\n== Guide\n\nContent.");
        int labelIndex = html.indexOf("status-label");
        int contentIndex = html.indexOf("Guide");
        assertTrue(labelIndex < contentIndex);
    }
}
