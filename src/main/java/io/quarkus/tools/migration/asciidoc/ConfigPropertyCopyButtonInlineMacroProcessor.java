package io.quarkus.tools.migration.asciidoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

@Name("config_property_copy_button")
public class ConfigPropertyCopyButtonInlineMacroProcessor extends InlineMacroProcessor {

    @Override
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        String html = """
                <button class="btn-copy fa fa-clipboard inline-btn-copy"\
                 data-clipboard-action="copy"\
                 data-clipboard-text='%s'\
                 title="Copy to clipboard"\
                 do-not-collapse="true"></button>""".formatted(target);
        return createPhraseNode(parent, "quoted", html, attributes, new HashMap<>(Map.of("subs", ":none")));
    }
}
