package io.quarkus.cve.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TreeRangesTest {

    // a - b - c - d          d is a leaf
    //      \
    //       e - f            a branch off b
    // x - y                  a second root, y has no BOM neighbour
    private static final Map<String, String> PARENTS = new HashMap<>();
    static {
        PARENTS.put("a", null);
        PARENTS.put("b", "a");
        PARENTS.put("c", "b");
        PARENTS.put("d", "c");
        PARENTS.put("e", "b");
        PARENTS.put("f", "e");
        PARENTS.put("x", null);
        PARENTS.put("y", "x");
    }

    private static final Comparator<String> ORDER = Comparator.naturalOrder();
    private static final ReleaseTree TREE = new ReleaseTree(PARENTS, PARENTS.keySet(), ORDER);

    private static List<Object> piece(TreeRanges.Piece p) {
        return List.of(p.root(), p.cuts());
    }

    @Test
    void aWholeSubtreeIsOneRootWithNoCuts() {
        List<TreeRanges.Piece> pieces = TreeRanges.compute(Set.of("b", "c", "d", "e", "f"), TREE, ORDER);
        assertEquals(List.of(List.of("b", List.of())), pieces.stream().map(TreeRangesTest::piece).toList());
    }

    @Test
    void aCutIsTheFirstUnaffectedReleaseOnEachBranch() {
        List<TreeRanges.Piece> pieces = TreeRanges.compute(Set.of("a", "b", "c", "e"), TREE, ORDER);
        assertEquals(List.of(List.of("a", List.of("d", "f"))), pieces.stream().map(TreeRangesTest::piece).toList());
    }

    @Test
    void aReleaseCuredAndThenAffectedAgainIsTwoPieces() {
        List<TreeRanges.Piece> pieces = TreeRanges.compute(Set.of("a", "b", "d"), TREE, ORDER);
        assertEquals(List.of(List.of("a", List.of("c", "e")), List.of("d", List.of())),
                pieces.stream().map(TreeRangesTest::piece).toList());
    }

    @Test
    void separateTreesGiveSeparateRoots() {
        List<TreeRanges.Piece> pieces = TreeRanges.compute(Set.of("c", "d", "y"), TREE, ORDER);
        assertEquals(List.of(List.of("c", List.of()), List.of("y", List.of())),
                pieces.stream().map(TreeRangesTest::piece).toList());
    }

    @Test
    void expandingGivesTheAffectedSetBack() {
        for (Set<String> affected : List.of(
                Set.of("a", "b", "c", "e"), Set.of("a", "b", "d"), Set.of("c", "d", "y"),
                Set.of("a", "b", "c", "d", "e", "f", "x", "y"), Set.of("f"))) {
            List<TreeRanges.Piece> pieces = TreeRanges.compute(affected, TREE, ORDER);
            assertEquals(affected, TreeRanges.expand(pieces, TREE), "affected " + affected);
        }
    }

    @Test
    void aReleaseWithoutContentIsSkippedWhenFindingTheParent() {
        // c is not scanned, so d hangs off b
        ReleaseTree l = new ReleaseTree(PARENTS, Set.of("a", "b", "d"), ORDER);
        assertEquals("b", l.parent("d"));
        assertNull(l.parent("a"));
        assertEquals(List.of("d"), l.children("b"));
    }

    @Test
    void aCycleInTheInputDoesNotLoopForever() {
        Map<String, String> cyclic = new HashMap<>();
        cyclic.put("p", "q");
        cyclic.put("q", "p");
        cyclic.put("r", "p");
        ReleaseTree l = new ReleaseTree(cyclic, Set.of("r"), ORDER);
        assertNull(l.parent("r"));
    }
}
