package io.quarkus.tools.migration.asciidoc;

import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.asciidoctor.extension.Name;

@Name("env_var_with_copy_button")
public class EnvVarCopyButtonInlineMacroProcessor extends InlineMacroProcessor {

    @Override
    public PhraseNode process(StructuralNode parent, String target, Map<String, Object> attributes) {
        int id = parent.getDocument().getAndIncrementCounter("env-var-id");
        String html = """
                <code id="env-var-%1$d">%2$s</code>\
                <button class="btn-copy fa fa-clipboard inline-btn-copy"\
                 data-clipboard-action="copy"\
                 data-clipboard-target="#env-var-%1$d"\
                 title="Copy to clipboard"\
                 do-not-collapse="true"></button>""".formatted(id, target);
        return createPhraseNode(parent, "quoted", html, attributes, new HashMap<>(Map.of("subs", ":none")));
    }
}
