// Patched version of io.quarkiverse.roq.plugin.asciidoc.common.deployment.AsciidocHeaderParser
// Fixes binary incompatibility with asciidoc-java 1.2.16:
//   Header.author() changed from Author to List<Author>
// See: https://github.com/yupiik/tools-maven-plugin/issues/92
// TODO: Remove with Roq 2.1.8
package io.quarkiverse.roq.plugin.asciidoc.common.deployment;

import static io.quarkiverse.roq.frontmatter.deployment.util.RoqFrontMatterTemplateUtils.stripFrontMatter;
import static io.quarkiverse.roq.frontmatter.runtime.RoqFrontMatterKeys.DESCRIPTION;
import static io.quarkiverse.roq.frontmatter.runtime.RoqFrontMatterKeys.ESCAPE;
import static io.quarkiverse.roq.frontmatter.runtime.RoqFrontMatterKeys.TITLE;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.quarkiverse.roq.frontmatter.deployment.items.scan.RoqFrontMatterHeaderParserBuildItem;
import io.quarkiverse.roq.frontmatter.deployment.items.scan.TemplateContext;
import io.vertx.core.json.JsonObject;
import io.yupiik.asciidoc.model.Author;
import io.yupiik.asciidoc.model.Header;
import io.yupiik.asciidoc.parser.Parser;
import io.yupiik.asciidoc.parser.internal.Reader;
import io.yupiik.asciidoc.parser.resolver.ContentResolver;
import io.yupiik.asciidoc.parser.resolver.RelativeContentResolver;

public class AsciidocHeaderParser {
    private static final Logger LOGGER = org.jboss.logging.Logger.getLogger(AsciidocHeaderParser.class);

    private static final String QUTE_KEY = "qute";

    // Pattern used to short-circuit more complex evaluation
    private static final Pattern SIMPLE_CONDITIONAL = Pattern
            .compile("ifn?(?:def|eval)");

    // Mirrors Asciidoctor's ConditionalDirectiveRx — one pattern for all conditional directives.
    // group 1: leading backslash (escaped directive, treat as literal text)
    // group 2: directive name (ifdef, ifndef, ifeval, endif)
    // group 3: target attributes (may be empty for endif/ifeval)
    // group 4: bracket content (null for empty brackets = block form, non-null = inline form)
    private static final Pattern CONDITIONAL_DIRECTIVE = Pattern
            .compile("^(\\\\)?(ifdef|ifndef|ifeval|endif)::(\\S*?)\\[(.+)?\\]\\s*$");

    // Matches attribute entries: :name: value, :!name: (unset), :name!: (unset)
    private static final Pattern ATTRIBUTE_ENTRY = Pattern.compile("^:(!?\\w[^:]*):(?:[ \\t]+(.*))?$");

    public static RoqFrontMatterHeaderParserBuildItem createBuildItem(boolean qute, Predicate<TemplateContext> isApplicable) {
        return new RoqFrontMatterHeaderParserBuildItem(isApplicable, templateContext -> {
            Parser parser = new Parser();
            String content = stripFrontMatter(templateContext.content());
            if (SIMPLE_CONDITIONAL.matcher(content).find()) {
                content = stripConditionalDirectives(content);
            }

            ContentResolver contentResolver = new PathContentResolver(templateContext.sourceFile().getParent());
            Header header = parser.parseHeader(new Reader(content.lines().toList()),
                    new Parser.ParserContext(contentResolver));
            final JsonObject pageData = toPageData(header);
            boolean escape = !qute;

            if (header.attributes().containsKey(QUTE_KEY)) {
                final String quteAttribute = header.attributes().get(QUTE_KEY);
                escape = !(quteAttribute.isEmpty() || Boolean.parseBoolean(quteAttribute));
            }

            if (!pageData.containsKey(ESCAPE)) {
                pageData.put(ESCAPE, escape);
            }
            return pageData;
        }, Function.identity(), 20);
    }

    public static JsonObject toPageData(Header header) {
        JsonObject pageData = new JsonObject();
        final Map<String, String> attributes = processAttributes(header.attributes());
        pageData.put("attributes", JsonObject.mapFrom(attributes));
        for (String key : attributes.keySet()) {
            if (key.startsWith("page-")) {
                final String value = attributes.get(key);
                pageData.put(key.substring(5), value.isEmpty() ? true : value);
            }
        }
        if (!header.title().isBlank()) {
            pageData.put(TITLE, header.title());
        }
        if (header.attributes().containsKey(DESCRIPTION) && !header.attributes().get(DESCRIPTION).isBlank()) {
            pageData.put(DESCRIPTION, header.attributes().get("description"));
        }
        if (header.author() != null && !header.author().isEmpty()
                && !header.author().get(0).name().isBlank()) {
            Author firstAuthor = header.author().get(0);
            pageData.put("author", firstAuthor.name());
            pageData.put("author-email", firstAuthor.mail());
        }
        if (!header.revision().number().isBlank()) {
            pageData.put("revision", new JsonObject()
                    .put("number", header.revision().number())
                    .put("date", header.revision().date())
                    .put("remark", header.revision().revmark()));
        }
        if (attributes.containsKey("description")) {
            pageData.put(DESCRIPTION, attributes.get("description"));
        }
        if (attributes.containsKey("image")) {
            pageData.put("image", attributes.get("image"));
        }
        return pageData;
    }

    public static Map<String, String> processAttributes(Map<String, String> input) {
        Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey();

            if (key.startsWith("!")) {
                result.remove(key.substring(1));
            } else {
                result.put(key, entry.getValue());
            }
        }

        return result;
    }

    static String stripConditionalDirectives(String content) {
        List<String> result = new ArrayList<>();
        Deque<Boolean> includeStack = new ArrayDeque<>();
        Set<String> definedAttributes = new HashSet<>();

        for (String line : (Iterable<String>) content.lines()::iterator) {
            Matcher m = CONDITIONAL_DIRECTIVE.matcher(line);
            if (m.matches()) {
                if (m.group(1) != null) {
                    if (isIncluding(includeStack)) {
                        result.add(line.substring(1));
                    }
                    continue;
                }

                LOGGER.debugf("Stripping conditional directive from AsciiDoc header: '%s'", line);
                String directive = m.group(2);
                String target = m.group(3);
                String bracketContent = m.group(4);

                switch (directive) {
                    case "ifdef", "ifndef" -> {
                        if (bracketContent == null) {
                            boolean include = isIncluding(includeStack)
                                    && evaluateCondition(directive, target, definedAttributes);
                            includeStack.push(include);
                        } else {
                            if (isIncluding(includeStack)
                                    && evaluateCondition(directive, target, definedAttributes)) {
                                result.add(bracketContent);
                                trackAttribute(bracketContent, definedAttributes);
                            }
                        }
                    }
                    case "endif" -> {
                        if (!includeStack.isEmpty()) {
                            includeStack.pop();
                        }
                    }
                    case "ifeval" -> includeStack.push(isIncluding(includeStack));
                }
                continue;
            }

            if (isIncluding(includeStack)) {
                result.add(line);
                trackAttribute(line, definedAttributes);
            }
        }

        return String.join("\n", result);
    }

    private static boolean isIncluding(Deque<Boolean> includeStack) {
        return !includeStack.contains(false);
    }

    private static final Pattern COMMA = Pattern.compile(",");
    private static final Pattern PLUS = Pattern.compile("\\+");

    static boolean evaluateCondition(String directive, String attributes, Set<String> definedAttributes) {
        boolean defined;
        if (attributes.contains(",")) {
            defined = COMMA.splitAsStream(attributes)
                    .map(String::trim)
                    .anyMatch(definedAttributes::contains);
        } else if (attributes.contains("+")) {
            defined = PLUS.splitAsStream(attributes)
                    .map(String::trim)
                    .allMatch(definedAttributes::contains);
        } else {
            defined = definedAttributes.contains(attributes.trim());
        }

        return "ifdef".equals(directive) ? defined : !defined;
    }

    private static void trackAttribute(String line, Set<String> definedAttributes) {
        Matcher attr = ATTRIBUTE_ENTRY.matcher(line);
        if (attr.matches()) {
            String raw = attr.group(1);
            boolean unset = raw.startsWith("!") || raw.endsWith("!");
            String name = unset ? raw.replace("!", "") : raw;
            if (unset) {
                definedAttributes.remove(name);
            } else {
                definedAttributes.add(name);
            }
        }
    }

    static class PathContentResolver implements RelativeContentResolver {

        private final Path base;

        PathContentResolver(Path base) {
            this.base = base;
        }

        @Override
        public Optional<Resolved> resolve(Path parent, String ref, Charset encoding) {
            final var rel = Path.of(ref);
            if (rel.isAbsolute()) {
                return doRead(rel, encoding);
            }
            if (parent != null && parent.getParent() != null) {
                return doRead(parent.getParent().resolve(ref), encoding);
            }
            final var resolved = base.resolve(ref);
            return doRead(resolved, encoding);
        }

        private static Optional<Resolved> doRead(Path resolved, Charset encoding) {
            if (Files.isRegularFile(resolved)) {
                try {
                    return Optional.of(new Resolved(resolved, Files.readAllLines(resolved, encoding)));
                } catch (IOException e) {
                    throw new RuntimeException("Can't read '" + resolved + "'");
                }
            }
            LOGGER.warnf("Include file '%s' not found", resolved);
            return Optional.of(new Resolved(null, List.of()));
        }
    }

}
