package io.quarkus.tools.migration.asciidoc;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry;

public class QuarkusExtensionRegistry implements ExtensionRegistry {

    @Override
    public void register(Asciidoctor asciidoctor) {
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(new TooltipInlineMacroProcessor())
                .inlineMacro(new EnvVarCopyButtonInlineMacroProcessor())
                .inlineMacro(new ConfigPropertyCopyButtonInlineMacroProcessor())
                .treeprocessor(new ConfigTableTreeprocessor())
                .postprocessor(new ConfigTablePostprocessor())
                .treeprocessor(new ExtensionStatusTreeprocessor());
    }
}
