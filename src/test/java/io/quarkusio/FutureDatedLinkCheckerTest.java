package io.quarkusio;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkusio.FutureDatedLinkChecker.Post;
import io.quarkusio.FutureDatedLinkChecker.Violation;

import static io.quarkusio.FutureDatedLinkChecker.findViolations;
import static org.junit.jupiter.api.Assertions.*;

class FutureDatedLinkCheckerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 10);
    private static final String GUARD = "{#if site.document('posts/2026/2026-11-17-deep-dive.adoc')}\n";

    @Test
    void blogPostsDoNotLinkToUnpublishedPosts() {
        List<Post> posts = FutureDatedLinkChecker.loadPosts(Path.of("content"));
        assertFalse(posts.isEmpty(), "No blog posts found under content/posts");

        List<Violation> violations = findViolations(posts, LocalDate.now(ZoneOffset.UTC));

        assertTrue(violations.isEmpty(), violations.size() + " link(s) to posts that are not published yet:\n"
                + violations.stream().map(Violation::toString).collect(Collectors.joining("\n\n")));
    }

    @Test
    void flagsPublishedPostLinkingToFuturePost() {
        List<Violation> violations = findViolations(List.of(
                intro("see the link:/blog/deep-dive/[companion post]."),
                deepDive()), TODAY);

        assertEquals(1, violations.size());
        assertFalse(violations.get(0).guardedWithoutQute());
        assertTrue(violations.get(0).toString().contains(GUARD.strip()),
                "The failure message should suggest the guard for the linked post");
    }

    @Test
    void flagsFuturePostLinkingToLaterFuturePost() {
        Post source = new Post("posts/2026/2026-11-12-teaser.adoc", "teaser", LocalDate.of(2026, 11, 12),
                "see https://quarkus.io/blog/deep-dive/");

        assertEquals(1, findViolations(List.of(source, deepDive()), TODAY).size());
    }

    @Test
    void allowsGuardedLinkInQutePost() {
        Post intro = intro(":qute:\n\n" + GUARD + "see the link:/blog/deep-dive/[companion post].\n{#else}\ncoming soon\n{/if}\n");

        assertEquals(List.of(), findViolations(List.of(intro, deepDive()), TODAY));
    }

    @Test
    void flagsGuardedLinkWithoutQute() {
        Post intro = intro(GUARD + "see the link:/blog/deep-dive/[companion post].\n{/if}\n");

        List<Violation> violations = findViolations(List.of(intro, deepDive()), TODAY);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).guardedWithoutQute());
    }

    @Test
    void flagsLinkInElseBranchOrAfterGuard() {
        Post intro = intro(":qute:\n\n" + GUARD + "ok\n{#else}\nsee /blog/deep-dive/\n{/if}\nand /blog/deep-dive/ again");

        assertEquals(2, findViolations(List.of(intro, deepDive()), TODAY).size());
    }

    @Test
    void flagsGuardOnWrongPath() {
        Post intro = intro(":qute:\n\n{#if site.document('posts/2026-11-17-deep-dive.adoc')}\nsee /blog/deep-dive/\n{/if}\n");

        assertEquals(1, findViolations(List.of(intro, deepDive()), TODAY).size());
    }

    @Test
    void allowsLinksToPublishedPostsAndFromLaterPosts() {
        Post published = new Post("posts/2026/2026-11-01-old.adoc", "old", LocalDate.of(2026, 11, 1), "");
        Post laterSource = new Post("posts/2026/2026-11-18-follow-up.adoc", "follow-up", LocalDate.of(2026, 11, 18),
                "see /blog/deep-dive/ and /blog/old/");

        assertEquals(List.of(), findViolations(List.of(intro("see /blog/old/"), published, laterSource, deepDive()), TODAY));
    }

    @Test
    void doesNotMatchSlugPrefixes() {
        assertEquals(List.of(), findViolations(List.of(intro("see /blog/deep-dive-part-two/"), deepDive()), TODAY));
    }

    private static Post intro(String content) {
        return new Post("posts/2026/2026-11-10-intro.adoc", "intro", TODAY, content);
    }

    private static Post deepDive() {
        return new Post("posts/2026/2026-11-17-deep-dive.adoc", "deep-dive", LocalDate.of(2026, 11, 17), "");
    }
}
