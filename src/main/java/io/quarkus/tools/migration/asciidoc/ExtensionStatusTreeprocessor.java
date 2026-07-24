package io.quarkus.tools.migration.asciidoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.Block;
import org.asciidoctor.ast.Document;
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

        return document;
    }
}
