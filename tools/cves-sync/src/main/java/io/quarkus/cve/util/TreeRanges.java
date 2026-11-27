package io.quarkus.cve.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes a set of affected releases as ranges over the release tree: a {@link Piece} is a root and the
 * cuts below it. It reads as "the root and every descendant of it, except the subtrees under a cut", where
 * a cut is the first release on a branch that is no longer affected.
 *
 * An advisory has a list of pieces rather than one, because a release can be cured and then be affected
 * again further down, and because a scan can start in the middle of a line.
 */
public final class TreeRanges {

    /**
     * One connected piece of the affected set: its root and the first unaffected release on each branch.
     * {@code cuts} is left out of the data file when it is empty (the piece then runs to every tip of its branch),
     * because po4a cannot read the one line {@code cuts: []} that would be written.
     */
    @JsonPropertyOrder({"root", "cuts"})
    public record Piece(String root, @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> cuts) {}

    private TreeRanges() {}

    /** The pieces of {@code affected}, ordered by {@code order} (roots, and the cuts of each root). */
    public static List<Piece> compute(Set<String> affected, ReleaseTree tree, Comparator<String> order) {
        List<String> roots = new ArrayList<>();
        for (String v : affected) {
            String p = tree.parent(v);
            if (p == null || !affected.contains(p)) roots.add(v);
        }
        roots.sort(order);

        List<Piece> pieces = new ArrayList<>();
        for (String root : roots) {
            List<String> cuts = new ArrayList<>();
            List<String> stack = new ArrayList<>(List.of(root));
            while (!stack.isEmpty()) {
                String x = stack.remove(stack.size() - 1);
                for (String child : tree.children(x)) {
                    if (affected.contains(child)) stack.add(child);
                    else cuts.add(child);
                }
            }
            cuts.sort(order);
            pieces.add(new Piece(root, cuts));
        }
        return pieces;
    }

    /** The releases the pieces describe; {@code compute} followed by {@code expand} gives the set back. */
    public static Set<String> expand(List<Piece> pieces, ReleaseTree tree) {
        Set<String> out = new HashSet<>();
        for (Piece piece : pieces) {
            Set<String> cuts = new HashSet<>(piece.cuts());
            List<String> stack = new ArrayList<>(List.of(piece.root()));
            while (!stack.isEmpty()) {
                String x = stack.remove(stack.size() - 1);
                if (cuts.contains(x) || !out.add(x)) continue;
                stack.addAll(tree.children(x));
            }
        }
        return out;
    }
}
