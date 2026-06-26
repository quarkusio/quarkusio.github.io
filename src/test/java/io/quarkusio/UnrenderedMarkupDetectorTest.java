package io.quarkusio;

import java.util.List;

import org.junit.jupiter.api.Test;

import static io.quarkusio.UnrenderedMarkupDetector.findUnrenderedMarkup;
import static org.junit.jupiter.api.Assertions.*;

class UnrenderedMarkupDetectorTest {

    @Test
    void detectsAsciidocHeadings() {
        assertDetects("== My Heading\nSome text", "AsciiDoc heading");
        assertDetects("=== Sub Heading\ntext", "AsciiDoc heading");
    }

    @Test
    void detectsAsciidocCodeFences() {
        assertDetects("some text\n----\ncode\n----\n", "AsciiDoc code fence");
    }

    @Test
    void detectsAsciidocSourceBlocks() {
        assertDetects("before\n[source,java]\n----\ncode\n----", "AsciiDoc [source,...] block");
    }

    @Test
    void detectsAsciidocImageMacros() {
        assertDetects("see image::screenshot.png[A screenshot] for details", "AsciiDoc image:: macro");
    }

    @Test
    void detectsAsciidocLinks() {
        assertDetects("Visit https://example.com[Example Site] for more", "AsciiDoc link");
    }

    @Test
    void detectsAsciidocAdmonitions() {
        assertDetects("text\n[NOTE]\n====\nBe careful\n====", "AsciiDoc admonition");
    }

    @Test
    void detectsAsciidocAttributes() {
        assertDetects(":imagesdir: /assets/images\nSome text", "AsciiDoc attribute");
    }

    @Test
    void detectsMarkdownHeadings() {
        assertDetects("## My Heading\nSome text", "Markdown heading");
        assertDetects("### Sub Heading\ntext", "Markdown heading");
    }

    @Test
    void detectsMarkdownLinks() {
        assertDetects("Click [here](https://example.com) to continue", "Markdown link");
    }

    @Test
    void detectsMarkdownImages() {
        assertDetects("See ![alt text](https://example.com/img.png)", "Markdown image");
    }

    @Test
    void detectsMarkdownCodeFences() {
        assertDetects("text\n```java\ncode\n```\n", "Markdown code fence");
    }

    @Test
    void detectsTemplateTags() {
        assertDetects("before {% if page.layout %} after", "Template tag");
        assertDetects("before {%- for item in list -%} after", "Template tag");
    }

    @Test
    void detectsTemplateOutput() {
        assertDetects("Hello {{ user.name }} welcome", "Template output");
    }

    @Test
    void detectsTemplateErrors() {
        assertDetects("Liquid error: undefined variable", "Template error");
        assertDetects("template syntax error in tag", "Template error");
    }

    @Test
    void detectsYamlFrontMatter() {
        assertDetects("---\ntitle: My Page\n---\nContent", "YAML front matter");
    }

    @Test
    void doesNotFlagYamlFrontMatterMidText() {
        List<String> findings = findUnrenderedMarkup(
                "Some text before\n---\nsome text after");
        assertTrue(findings.stream().noneMatch(f -> f.contains("YAML front matter")),
                "Should not flag --- in the middle of text but got: " + findings);
    }

    @Test
    void doesNotFlagCleanHtml() {
        List<String> findings = findUnrenderedMarkup(
                "This is a normal blog post with headings and paragraphs. "
                        + "It mentions Java and Quarkus. "
                        + "Visit our website for more details.");
        assertTrue(findings.isEmpty(), "Clean HTML should produce no findings but got: " + findings);
    }

    private void assertDetects(String input, String expectedFragment) {
        List<String> findings = findUnrenderedMarkup(input);
        assertFalse(findings.isEmpty(), "Expected to detect markup in: " + input);
        assertTrue(findings.stream().anyMatch(f -> f.contains(expectedFragment)),
                "Expected finding containing '" + expectedFragment + "' but got: " + findings);
    }
}
