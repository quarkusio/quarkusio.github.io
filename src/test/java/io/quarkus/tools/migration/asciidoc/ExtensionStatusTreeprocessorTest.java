package io.quarkus.tools.migration.asciidoc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

    private String convertWithBaseDir(String adoc, Path baseDir) {
        return asciidoctor.convert(adoc, Options.builder()
                .baseDir(baseDir.toFile())
                .build());
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

    // --- Note injection tests (require an _includes/extension-status.adoc on disk) ---

    @Test
    void injectsNoteWhenIncludeFilePresent(@TempDir Path baseDir) throws IOException {
        writeExtensionStatusInclude(baseDir);
        String html = convertWithBaseDir(":extension-status: experimental\n\n== Guide\n\nContent.", baseDir);
        assertTrue(html.contains("id=\"extension-status-note\""),
                "Should inject the extension-status-note anchor from the include file");
        assertTrue(html.contains("experimental"),
                "Note should mention the status");
    }

    @Test
    void noteAppearsAfterLabel(@TempDir Path baseDir) throws IOException {
        writeExtensionStatusInclude(baseDir);
        String html = convertWithBaseDir(":extension-status: preview\n\n== Guide\n\nContent.", baseDir);
        int labelIndex = html.indexOf("status-label");
        int noteIndex = html.indexOf("extension-status-note");
        assertTrue(labelIndex >= 0, "Label should be present");
        assertTrue(noteIndex >= 0, "Note should be present");
        assertTrue(labelIndex < noteIndex, "Label should appear before the note");
    }

    @Test
    void doesNotDuplicateNoteWhenAlreadyPresent(@TempDir Path baseDir) throws IOException {
        writeExtensionStatusInclude(baseDir);
        // Simulate a guide that manually includes the file: embed the anchor inline
        String adoc = ":extension-status: stable\n\n== Guide\n\nContent.\n\n" +
                "[[extension-status-note]]\nNOTE: Already included.";
        String html = convertWithBaseDir(adoc, baseDir);
        // The anchor id should appear exactly once — the treeprocessor must not inject a second copy.
        // (The label's href="#extension-status-note" also contains the string but is not the id attribute.)
        int count = 0;
        int idx = 0;
        String marker = "id=\"extension-status-note\"";
        while ((idx = html.indexOf(marker, idx)) != -1) {
            count++;
            idx += marker.length();
        }
        assertTrue(count == 1, "extension-status-note anchor id should appear exactly once, found: " + count);
    }

    @Test
    void noNoteInjectedWhenIncludeFileMissing() {
        // No baseDir set — the note anchor (id=) should not be injected.
        // The label's href="#extension-status-note" is always present; we only check for the anchor id.
        String html = convert(":extension-status: deprecated\n\n== Guide\n\nContent.");
        assertFalse(html.contains("id=\"extension-status-note\""),
                "Without the include file on disk, no note anchor id should be injected");
    }

    /**
     * Writes the real extension-status.adoc content into {@code baseDir/_includes/}.
     * The content matches what's in the guides repo (no [[extension-status-note]] anchor).
     * The treeprocessor injects the anchor programmatically when converting.
     */
    private static void writeExtensionStatusInclude(Path baseDir) throws IOException {
        Path includesDir = baseDir.resolve("_includes");
        Files.createDirectories(includesDir);
        Files.writeString(includesDir.resolve("extension-status.adoc"), """
                ifdef::extension-status[]
                [NOTE]
                ====
                This technology is considered {extension-status}.

                ifeval::["\\{extension-status}" == "experimental"]
                In _experimental_ mode, early feedback is requested to mature the idea.
                There is no guarantee of stability nor long term presence in the platform until the solution matures.
                Feedback is welcome on our https://groups.google.com/d/forum/quarkus-dev[mailing list] or as issues in our https://github.com/quarkusio/quarkus/issues[GitHub issue tracker].
                endif::[]
                ifeval::["\\{extension-status}" == "preview"]
                In _preview_, backward compatibility and presence in the ecosystem is not guaranteed.
                Specific improvements might require changing configuration or APIs, and plans to become _stable_ are under way.
                Feedback is welcome on our https://groups.google.com/d/forum/quarkus-dev[mailing list] or as issues in our https://github.com/quarkusio/quarkus/issues[GitHub issue tracker].
                endif::[]
                ifeval::["\\{extension-status}" == "stable"]
                Being _stable_, backward compatibility and presence in the ecosystem are taken very seriously.
                endif::[]
                ifeval::["\\{extension-status}" == "deprecated"]
                Being _deprecated_ means that this extension is likely to be replaced or removed in a future version of Quarkus.
                endif::[]

                For a full list of possible statuses, check our https://quarkus.io/faq/#what-are-the-extension-statuses[FAQ entry].
                ====
                endif::extension-status[]
                """);
    }
}
