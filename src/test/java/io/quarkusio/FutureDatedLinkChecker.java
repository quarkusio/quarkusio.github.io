package io.quarkusio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds links from blog posts to posts that will not be published yet when the linking post is.
 * <p>
 * Production hides future-dated posts until their date, but PR builds show them (SITE_FUTURE=true),
 * so the link crawler cannot catch these. A link is fine if the target is already published, if the
 * linking post is dated on or after the target, or if it is wrapped in a
 * {@code {#if site.document('posts/...')}} guard in a post that Qute processes.
 */
public class FutureDatedLinkChecker {

    private static final Pattern POST_FILE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})-(.+)\\.(adoc|asciidoc|md)$");
    private static final Pattern FRONT_MATTER = Pattern.compile("\\A---\\s*\\n(.*?)\\n---\\s*\\n", Pattern.DOTALL);
    private static final Pattern FRONT_MATTER_DATE = Pattern.compile("(?m)^date:\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern BLOG_LINK = Pattern.compile("(?:https?://(?:www\\.)?quarkus\\.io)?/blog/([A-Za-z0-9._-]+?)(?=[/\\[)\"'#\\s]|$)");
    private static final Pattern GUARD_START = Pattern.compile("\\{#if\\s+site\\.document\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)\\s*}");
    private static final Pattern GUARD_END = Pattern.compile("\\{#else}|\\{/if}");
    private static final Pattern QUTE_ATTRIBUTE = Pattern.compile("(?m)^:qute:\\s*$");

    public record Post(String sourcePath, String slug, LocalDate date, String content) {

        boolean isAsciidoc() {
            return !sourcePath.endsWith(".md");
        }
    }

    public record Violation(Post source, Post target, boolean guardedWithoutQute) {

        @Override
        public String toString() {
            if (guardedWithoutQute) {
                return "content/" + source.sourcePath() + " guards its link to /blog/" + target.slug()
                        + "/ with site.document(...), but the guard is ignored because the post does not enable Qute."
                        + " Add a ':qute:' line after ':imagesdir:' in the AsciiDoc header.";
            }
            return "content/" + source.sourcePath() + " (" + source.date() + ") links to /blog/" + target.slug()
                    + "/, which is not published until " + target.date() + ", so the link will 404 until then."
                    + " Show the link only once the target exists:\n"
                    + "    :qute:   <- in the AsciiDoc header (not needed for Markdown)\n"
                    + "    {#if site.document('" + target.sourcePath() + "')}\n"
                    + "    ...the sentence with the link...\n"
                    + "    {#else}\n"
                    + "    ...a version without the link, e.g. 'A follow-up post next week will cover ...'\n"
                    + "    {/if}\n"
                    + "  See 'Linking to a Future-Dated Post' in .agents/skills/building-blog/SKILL.md.";
        }
    }

    public static List<Post> loadPosts(Path contentDir) {
        Path postsDir = contentDir.resolve("posts");
        try (Stream<Path> files = Files.walk(postsDir)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> loadPost(contentDir, file))
                    .filter(post -> post != null)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Post loadPost(Path contentDir, Path file) {
        Matcher name = POST_FILE.matcher(file.getFileName().toString());
        if (!name.matches()) {
            return null;
        }
        try {
            String content = Files.readString(file);
            String sourcePath = contentDir.relativize(file).toString().replace('\\', '/');
            return new Post(sourcePath, name.group(2), postDate(content, name.group(1)), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Roq uses the front matter date when there is one, and the file name date otherwise
    private static LocalDate postDate(String content, String fileNameDate) {
        Matcher frontMatter = FRONT_MATTER.matcher(content);
        if (frontMatter.find()) {
            Matcher date = FRONT_MATTER_DATE.matcher(frontMatter.group(1));
            if (date.find()) {
                try {
                    return LocalDate.parse(date.group(1));
                } catch (DateTimeParseException e) {
                    // fall back to the file name
                }
            }
        }
        return LocalDate.parse(fileNameDate);
    }

    public static List<Violation> findViolations(List<Post> posts, LocalDate today) {
        Map<String, Post> futurePostsBySlug = new HashMap<>();
        for (Post post : posts) {
            if (post.date().isAfter(today)) {
                futurePostsBySlug.put(post.slug(), post);
            }
        }

        List<Violation> violations = new ArrayList<>();
        for (Post source : posts) {
            Matcher link = BLOG_LINK.matcher(source.content());
            while (link.find()) {
                Post target = futurePostsBySlug.get(link.group(1));
                if (target == null || !target.date().isAfter(source.date())) {
                    continue;
                }
                if (!isGuarded(source.content(), link.start(), target.sourcePath())) {
                    violations.add(new Violation(source, target, false));
                } else if (source.isAsciidoc() && !QUTE_ATTRIBUTE.matcher(source.content()).find()) {
                    violations.add(new Violation(source, target, true));
                }
            }
        }
        return violations;
    }

    private static boolean isGuarded(String content, int linkOffset, String targetSourcePath) {
        Matcher guard = GUARD_START.matcher(content);
        while (guard.find() && guard.end() <= linkOffset) {
            if (!guard.group(1).equals(targetSourcePath)) {
                continue;
            }
            Matcher end = GUARD_END.matcher(content);
            if (!end.find(guard.end()) || end.start() > linkOffset) {
                return true;
            }
        }
        return false;
    }
}
