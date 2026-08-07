package io.quarkus.tools.migration.asciidoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.Treeprocessor;

public class ExtensionStatusTreeprocessor extends Treeprocessor {

    private static final Map<String, String> TOOLTIPS = Map.of(
            "experimental", "This extension requests early feedback to mature the idea",
            "preview", "This extension's backward compatibility and presence in the ecosystem is not guaranteed",
            "stable",
            "This extension's backward compatibility and presence in the ecosystem are taken very seriously",
            "deprecated", "This extension is likely to be replaced or removed in a future version");

    @Override
    public Document process(Document document) {
        Object statusObj = document.getAttribute("extension-status");
        if (statusObj == null) {
            return document;
        }

        String status = statusObj.toString().trim();
        if (status.isEmpty()) {
            return document;
        }

        String tooltip = TOOLTIPS.getOrDefault(status, "");
        String labelHtml = """
                <a class="status-label status-%1$s"\
                 title="%2$s"\
                 href="#extension-status-note">%1$s</a>""".formatted(status, tooltip);

        Block labelBlock = createBlock(document, "pass", labelHtml, new HashMap<>());
        document.getBlocks().add(0, labelBlock);

        // Inject the extension-status note if the guide has not already included
        // _includes/extension-status.adoc (which provides the [[extension-status-note]] anchor).
        List<StructuralNode> existing = document.findBy(Map.of("id", "extension-status-note"));
        if (existing == null || existing.isEmpty()) {
            // Only inject when the caller explicitly set a base dir. Options.BASEDIR is absent
            // when no base dir was provided (Asciidoctor does not default it in the options map).
            Object baseDirOpt = document.getOptions().get(Options.BASEDIR);
            if (baseDirOpt != null) {
                injectNoteFromIncludeFile(document, status, Paths.get(baseDirOpt.toString()));
            }
        }

        return document;
    }

    /**
     * Reads {@code _includes/extension-status.adoc} from {@code baseDir},
     * converts it to HTML using a fresh Asciidoctor instance (avoiding re-entrancy
     * with the outer treeprocessor), and inserts the result as a pass block at position 1.
     *
     * <p>The note content lives in exactly one place on disk ({@code _includes/extension-status.adoc});
     * this method is the DRY bridge between the treeprocessor and that file.</p>
     */
    private void injectNoteFromIncludeFile(Document document, String status, Path baseDir) {
        Path noteFile = baseDir.resolve("_includes").resolve("extension-status.adoc");
        if (!Files.isRegularFile(noteFile)) {
            return;
        }

        try {
            String noteSource = Files.readString(noteFile);
            // Prepend the anchor before converting to ensure it's included in the output.
            // This allows the include file to remain unchanged in the guides repo.
            String noteSourceWithAnchor = "[[extension-status-note]]\n" + noteSource;

            // Use a fresh Asciidoctor instance without the extension registry so that
            // converting the note file does not re-trigger this treeprocessor.
            // We convert to an HTML string and wrap it in a pass block — no cross-runtime
            // AST nodes are injected into the outer document.
            String noteHtml;
            try (Asciidoctor inner = Asciidoctor.Factory.create()) {
                noteHtml = inner.convert(noteSourceWithAnchor,
                        Options.builder()
                                .attributes(Attributes.builder()
                                        .attribute("extension-status", status)
                                        .build())
                                .build());
            }
            Block noteBlock = createBlock(document, "pass", noteHtml, new HashMap<>());
            document.getBlocks().add(1, noteBlock);
        } catch (IOException e) {
            // Non-fatal: fall back to just the label with a broken anchor
        }
    }
}
