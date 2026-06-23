package io.quarkusio;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnrenderedMarkupDetector {

    private record MarkupPattern(Pattern pattern, String description) {
    }

    private static final List<MarkupPattern> PATTERNS = List.of(
            // AsciiDoc headings: == Heading or === Heading at start of line
            new MarkupPattern(Pattern.compile("(?m)^={2,6}\\s+\\S"), "AsciiDoc heading"),
            // AsciiDoc code block fence
            new MarkupPattern(Pattern.compile("(?m)^----\\s*$"), "AsciiDoc code fence (----)"),
            // AsciiDoc source block annotation
            new MarkupPattern(Pattern.compile("\\[source,[a-z]+]"), "AsciiDoc [source,...] block"),
            // AsciiDoc image macro
            new MarkupPattern(Pattern.compile("image::[^\\[]+\\["), "AsciiDoc image:: macro"),
            // AsciiDoc link syntax: https://url[link text]
            new MarkupPattern(Pattern.compile("https?://[^\\s\\[]+\\[[^]]+]"), "AsciiDoc link (url[text])"),
            // AsciiDoc admonition markers
            new MarkupPattern(Pattern.compile("(?m)^\\[(NOTE|TIP|WARNING|IMPORTANT|CAUTION)]\\s*$"), "AsciiDoc admonition marker"),
            // AsciiDoc attribute definition
            new MarkupPattern(Pattern.compile("(?m)^:[a-zA-Z][a-zA-Z0-9_-]+:"), "AsciiDoc attribute definition"),

            // Markdown headings: ## Heading or ### Heading
            new MarkupPattern(Pattern.compile("(?m)^#{2,6}\\s+\\S"), "Markdown heading"),
            // Markdown links: [text](url)
            new MarkupPattern(Pattern.compile("\\[[^]]+]\\(https?://[^)]+\\)"), "Markdown link ([text](url))"),
            // Markdown image: ![alt](url)
            new MarkupPattern(Pattern.compile("!\\[[^]]*]\\("), "Markdown image (![alt](url))"),
            // Markdown code fence
            new MarkupPattern(Pattern.compile("(?m)^```[a-z]*\\s*$"), "Markdown code fence (```)"),

            // Template block tags: {% if ... %} or {%- ... -%}
            new MarkupPattern(Pattern.compile("\\{%-?\\s+\\w"), "Template tag ({% ... %})"),
            // Template output tags: {{ variable }}
            new MarkupPattern(Pattern.compile("\\{\\{\\s*\\w"), "Template output ({{ ... }})"),
            // Template error messages
            new MarkupPattern(Pattern.compile("(?i)(liquid|template) (error|syntax error|warning)"), "Template error message"),
            // YAML front matter: --- at very start of visible text
            new MarkupPattern(Pattern.compile("\\A---\\s*\\n"), "YAML front matter (---)")
    );

    private static final Pattern HTML_TAG = Pattern.compile("</?[a-zA-Z][a-zA-Z0-9]*[\\s>/]");

    public static String findRawHtmlTag(String text) {
        Matcher matcher = HTML_TAG.matcher(text);
        if (matcher.find()) {
            int start = Math.max(0, matcher.start() - 20);
            int end = Math.min(text.length(), matcher.end() + 20);
            String context = text.substring(start, end).replace("\n", "\\n");
            return "Raw HTML tag \"" + matcher.group() + "\" in context: \"..." + context + "...\"";
        }
        return null;
    }

    public static void assertDoesNotContainRawHtml(String text, String pageDescription) {
        assertNull(findRawHtmlTag(text), pageDescription + " contains raw HTML");
    }

    public static void assertDoesNotContainRawHtml(Locator locator, String pageDescription) {
        assertDoesNotContainRawHtml(locator.innerText(), pageDescription);
    }

    public static void assertDoesNotContainRawHtml(Page page, String pageDescription) {
        assertDoesNotContainRawHtml(page.locator("body"), pageDescription);
    }

    public static void assertDoesNotContainUnrenderedMarkup(String text, String pageDescription) {
        List<String> findings = findUnrenderedMarkup(text);
        assertTrue(findings.isEmpty(),
                pageDescription + " contains unrendered markup: " + String.join("; ", findings));
    }

    public static void assertDoesNotContainUnrenderedMarkup(Locator locator, String pageDescription) {
        assertDoesNotContainUnrenderedMarkup(locator.innerText(), pageDescription);
    }

    public static void assertDoesNotContainUnrenderedMarkup(Page page, String pageDescription) {
        assertDoesNotContainUnrenderedMarkup(page.locator("body"), pageDescription);
    }

    public static List<String> findUnrenderedMarkup(String text) {
        List<String> findings = new ArrayList<>();
        for (MarkupPattern mp : PATTERNS) {
            var matcher = mp.pattern.matcher(text);
            if (matcher.find()) {
                String match = matcher.group();
                if (match.length() > 60) {
                    match = match.substring(0, 57) + "...";
                }
                findings.add(mp.description + ": \"" + match + "\"");
            }
        }
        return findings;
    }
}
