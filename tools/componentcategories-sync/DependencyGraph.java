package io.quarkusio.categories.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkusio.categories.ComponentCategories;
import io.quarkusio.categories.sync.PlatformDescriptor.Extension;

/**
 * What each platform extension depends on, from the Maven dependency graph of its runtime artifact (compile and
 * runtime scopes; optional dependencies are not followed). Resolving uses the local repository and Maven Central,
 * like Quarkus does, so a first run downloads many POMs.
 */
public final class DependencyGraph {

    private static final int THREADS = 8;

    private DependencyGraph() {
    }

    /**
     * @return for each extension (by {@link Extension#id}) every artifact in its dependency graph, named the same way.
     *         An extension that cannot be resolved is reported to {@code problem} and has no dependencies.
     */
    public static Map<String, Set<String>> of(MavenArtifactResolver resolver, List<Extension> extensions,
            Consumer<String> problem) throws Exception {
        Map<String, Set<String>> graph = new ConcurrentHashMap<>();
        // the time is in downloading POMs, so several extensions at once
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<?>> done = new ArrayList<>();
            for (Extension e : extensions) {
                done.add(pool.submit(() -> {
                    Artifact root = new DefaultArtifact(e.groupId(), e.artifactId(), "jar", e.version());
                    try {
                        graph.put(e.id(), artifacts(resolver.collectDependencies(root, List.of()).getRoot()));
                    } catch (Exception ex) {
                        problem.accept("Could not resolve " + root + ": " + rootCause(ex).getMessage());
                        graph.put(e.id(), Set.of());
                    }
                }));
            }
            for (Future<?> f : done) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }
        return new TreeMap<>(graph);
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }

    /** Every artifact under the root, which is not included, as a {@link ComponentCategories#componentId}. */
    static Set<String> artifacts(DependencyNode root) {
        PreorderNodeListGenerator nodes = new PreorderNodeListGenerator();
        root.accept(nodes);
        Set<String> found = new TreeSet<>();
        nodes.getNodes().stream().filter(n -> n != root && n.getArtifact() != null)
                .forEach(n -> found.add(ComponentCategories.componentId(n.getArtifact().getGroupId(),
                        n.getArtifact().getArtifactId())));
        return found;
    }
}
