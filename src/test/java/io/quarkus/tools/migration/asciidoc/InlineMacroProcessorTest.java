package io.quarkus.tools.migration.asciidoc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InlineMacroProcessorTest {

    static Asciidoctor asciidoctor;

    @BeforeAll
    static void setup() {
        asciidoctor = Asciidoctor.Factory.create();
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(new TooltipInlineMacroProcessor())
                .inlineMacro(new EnvVarCopyButtonInlineMacroProcessor())
                .inlineMacro(new ConfigPropertyCopyButtonInlineMacroProcessor());
    }

    @AfterAll
    static void cleanup() {
        asciidoctor.close();
    }

    private String convert(String adoc) {
        return asciidoctor.convert(adoc, Options.builder().build());
    }

    @Nested
    class Tooltip {

        @Test
        void withDescription() {
            String html = convert("tooltip:MY_CONST[A description]");
            assertTrue(html.contains("<span class=\"asciidoc-tooltip-wrapper\">"));
            assertTrue(html.contains("<code>MY_CONST</code>"));
            assertTrue(html.contains("<span class=\"asciidoc-tooltip\">A description</span>"));
        }

        @Test
        void withoutDescription() {
            String html = convert("tooltip:MY_CONST[]");
            assertTrue(html.contains("<code>MY_CONST</code>"));
            assertFalse(html.contains("asciidoc-tooltip-wrapper"));
        }

        @Test
        void withSpecialCharacters() {
            String html = convert("tooltip:THIRD_CONSTANT[Test sentence with spaces]");
            assertTrue(html.contains("<span class=\"asciidoc-tooltip-wrapper\">"));
            assertTrue(html.contains("<code>THIRD_CONSTANT</code>"));
            assertTrue(html.contains("Test sentence with spaces"));
        }

        @Test
        void multipleInDocument() {
            String html = convert("tooltip:A[desc A] and tooltip:B[desc B]");
            assertTrue(html.contains("<code>A</code>"));
            assertTrue(html.contains("<code>B</code>"));
        }
    }

    @Nested
    class EnvVarCopyButton {

        @Test
        void producesCodeAndButton() {
            String html = convert("env_var_with_copy_button:QUARKUS_REDIS_HOSTS[]");
            assertTrue(html.contains("<code id=\"env-var-"));
            assertTrue(html.contains(">QUARKUS_REDIS_HOSTS</code>"));
            assertTrue(html.contains("data-clipboard-target=\"#env-var-"));
            assertTrue(html.contains("btn-copy"));
        }

        @Test
        void incrementsIds() {
            String html = convert(
                    "env_var_with_copy_button:FIRST[] and env_var_with_copy_button:SECOND[]");
            assertTrue(html.contains("env-var-1"));
            assertTrue(html.contains("env-var-2"));
        }

        @Test
        void buttonHasDoNotCollapseAttribute() {
            String html = convert("env_var_with_copy_button:VAR[]");
            assertTrue(html.contains("do-not-collapse=\"true\""));
        }
    }

    @Nested
    class ConfigPropertyCopyButton {

        @Test
        void producesButtonWithClipboardText() {
            String html = convert("config_property_copy_button:quarkus.redis.hosts[]");
            assertTrue(html.contains("data-clipboard-text='quarkus.redis.hosts'"));
            assertTrue(html.contains("btn-copy"));
            assertTrue(html.contains("fa-clipboard"));
        }

        @Test
        void buttonHasDoNotCollapseAttribute() {
            String html = convert("config_property_copy_button:quarkus.test[]");
            assertTrue(html.contains("do-not-collapse=\"true\""));
        }
    }
}
