// Patched version of io.quarkiverse.roq.plugin.asciidoctorj.runtime.AsciidocJInclude
// Fixes https://github.com/quarkiverse/quarkus-roq/issues/1178
// TODO: Remove once Roq ships the fix and we upgrade
package io.quarkiverse.roq.plugin.asciidoctorj.runtime;

import static io.quarkiverse.roq.plugin.asciidoctorj.runtime.AsciidoctorJConverter.ROOTDIR;
import static org.asciidoctor.Options.BASEDIR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.IncludeProcessor;
import org.asciidoctor.extension.PreprocessorReader;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;

import io.quarkiverse.tools.stringpaths.StringPaths;

public class AsciidocJInclude extends IncludeProcessor {
    private static final Pattern URL_PREFIX_PATTERN = Pattern.compile("^((https?|file|ftp|irc)://|mailto:)");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(=+)(\\s+.*)$");
    private static final Pattern TAG_START_PATTERN = Pattern.compile("^\\s*(?://|#|--|;;|<!--)\\s*tag::(\\S+?)\\[\\]");
    private static final Pattern TAG_END_PATTERN = Pattern.compile("^\\s*(?://|#|--|;;|<!--)\\s*end::(\\S+?)\\[\\]");

    public AsciidocJInclude() {
    }

    @Override
    public boolean handles(String target) {
        return !URL_PREFIX_PATTERN.matcher(target).find();
    }

    @Override
    public void process(Document document, PreprocessorReader reader, String target, Map<String, Object> attributes) {
        long safeLevel = (Long) document.getOptions().get("safe");
        if (safeLevel >= SafeMode.SECURE.getLevel()) {
            throw new SecurityException("File includes are not allowed in SECURE mode.");
        }

        final String dir = reader.getDir();
        Charset charset = Charset.forName((String) document.getAttributes().getOrDefault("encoding", "UTF-8"));
        final Path baseDir = Path.of(document.getOptions().getOrDefault(BASEDIR, "").toString());
        final Path rootDir = Path.of(document.getOptions().getOrDefault(ROOTDIR, "").toString());
        Path p = Path.of(target);
        Path targetPath = (p.isAbsolute() ? p : baseDir.resolve(dir).resolve(target)).normalize();

        if (safeLevel >= SafeMode.SAFE.getLevel()) {
            if (!targetPath.startsWith(rootDir.normalize())) {
                throw new SecurityException("Include path is outside the root dir ('%s'): '%s'".formatted(rootDir, targetPath));
            }
        }

        String resourcePath = StringPaths.toUnixPath(targetPath.toString());
        try (InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (resource != null) {
                pushInclude(reader, new String(resource.readAllBytes(), charset), target, attributes);
                return;
            }
        } catch (IOException e) {
            log(new LogRecord(Severity.ERROR, "Can't read '" + target + "'"));
            return;
        }

        if (!Files.isRegularFile(targetPath)) {
            log(new LogRecord(Severity.ERROR, "Include file not found '" + target + "'"));
            return;
        }
        try {
            pushInclude(reader, Files.readString(targetPath, charset), target, attributes);
        } catch (IOException e) {
            log(new LogRecord(Severity.ERROR, "Can't read '" + target + "'"));
        }

    }

    private static void pushInclude(PreprocessorReader reader, String content, String target, Map<String, Object> attributes)
            throws IOException {
        final String processedContent = String.join("\n", processInclude(content, attributes));
        reader.pushInclude(
                processedContent,
                target,
                target,
                1,
                attributes);
    }

    static List<String> processInclude(String content, Map<String, Object> attrs) throws IOException {
        List<String> lines = content.lines().toList();

        if (attrs.containsKey("tag") && attrs.get("tag") instanceof String) {
            lines = extractSingleTag(lines, (String) attrs.get("tag"));
        } else if (attrs.containsKey("tags") && attrs.get("tags") instanceof String) {
            String tagsVal = (String) attrs.get("tags");
            lines = extractTags(lines, tagsVal);
        }

        if (attrs.containsKey("lines") && attrs.get("lines") instanceof String) {
            lines = extractLines(lines, (String) attrs.get("lines"));
        }

        if (attrs.containsKey("indent") && attrs.get("indent") instanceof String) {
            int indent = Integer.parseInt((String) attrs.get("indent"));
            String pad = " ".repeat(indent);
            lines = lines.stream().map(line -> pad + line).toList();
        }

        return lines;
    }

    // --- Fixed: split on ; or , and support ! negation ---

    private static List<String> extractTags(List<String> lines, String tagsValue) {
        List<String> includeTags = new ArrayList<>();
        List<String> excludeTags = new ArrayList<>();

        for (String raw : tagsValue.split("[;,]")) {
            String tag = raw.trim();
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.startsWith("!")) {
                excludeTags.add(tag.substring(1));
            } else {
                includeTags.add(tag);
            }
        }

        Set<String> activeTagStack = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            Matcher startMatcher = TAG_START_PATTERN.matcher(line);
            if (startMatcher.find()) {
                activeTagStack.add(startMatcher.group(1));
                continue;
            }
            Matcher endMatcher = TAG_END_PATTERN.matcher(line);
            if (endMatcher.find()) {
                activeTagStack.remove(endMatcher.group(1));
                continue;
            }

            boolean included = includeTags.isEmpty()
                    || activeTagStack.stream().anyMatch(includeTags::contains);
            boolean excluded = activeTagStack.stream().anyMatch(excludeTags::contains);

            if (included && !excluded) {
                result.add(line);
            }
        }

        return result;
    }

    private static List<String> extractSingleTag(List<String> lines, String tag) {
        List<String> result = new ArrayList<>();
        boolean inTag = false;
        for (String line : lines) {
            if (!inTag) {
                Matcher m = TAG_START_PATTERN.matcher(line);
                if (m.find() && m.group(1).equals(tag)) {
                    inTag = true;
                    continue;
                }
            } else {
                Matcher m = TAG_END_PATTERN.matcher(line);
                if (m.find() && m.group(1).equals(tag)) {
                    break;
                }
            }
            if (inTag) {
                result.add(line);
            }
        }
        return result;
    }

    private static List<String> extractLines(List<String> lines, String lineRanges) {
        Set<Integer> indices = new LinkedHashSet<>();
        for (String part : lineRanges.split("[;,]")) {
            part = part.trim();
            if (part.matches("\\d+")) {
                indices.add(Integer.parseInt(part) - 1);
            } else if (part.matches("\\d+\\.\\.\\d+")) {
                String[] range = part.split("\\.\\.");
                int start = Integer.parseInt(range[0]) - 1;
                int end = Integer.parseInt(range[1]) - 1;
                for (int i = start; i <= end; i++) {
                    indices.add(i);
                }
            }
        }
        List<String> result = new ArrayList<>();
        for (int idx : indices) {
            if (idx >= 0 && idx < lines.size()) {
                result.add(lines.get(idx));
            }
        }
        return result;
    }

    private static List<String> applyLevelOffset(List<String> lines, String leveloffset) {
        int offset = Integer.parseInt(leveloffset);
        List<String> result = new ArrayList<>();
        Pattern heading = HEADING_PATTERN;
        for (String line : lines) {
            Matcher m = heading.matcher(line);
            if (m.matches()) {
                int level = m.group(1).length() + offset;
                level = Math.max(1, level);
                String newLine = "=".repeat(level) + m.group(2);
                result.add(newLine);
            } else {
                result.add(line);
            }
        }
        return result;
    }

}
