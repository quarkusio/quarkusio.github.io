package io.quarkus.tools.migration.asciidoc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

@Name("tooltip")
@ContentModel(ContentModel.RAW)
public class TooltipInlineMacroProcessor extends InlineMacroProcessor {

    @Override
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        String text = Objects.toString(attributes.get("text"), "");
        String html;
        if (text.isEmpty()) {
            html = "<code>" + target + "</code>";
        } else {
            html = """
                    <span class="asciidoc-tooltip-wrapper">\
                    <code>%s</code>\
                    <span class="asciidoc-tooltip">%s</span>\
                    </span>""".formatted(target, text);
        }
        return createPhraseNode(parent, "quoted", html, attributes, new HashMap<>(Map.of("subs", ":none")));
    }
}
