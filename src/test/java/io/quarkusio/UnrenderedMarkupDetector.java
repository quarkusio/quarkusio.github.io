package io.quarkusio;

import java.util.ArrayList;
import java.util.List;
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
            new MarkupPattern(Pattern.compile("(?m)^```[a-z]*\\s*$"), "Markdown code fence (```)")
    );

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
