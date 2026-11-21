package io.quarkus.cve;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The workflow runs the tool with {@code jbang run.java}, which compiles only the files {@code run.java} lists under
 * {@code //SOURCES}, while Maven compiles everything. A class that is not listed passes the tests and then fails to
 * compile in the workflow.
 */
class RunJavaSourcesTest {

    @Test
    void everySourceFileIsListedInRunJava() throws IOException {
        Path tool = Path.of("").toAbsolutePath();
        List<String> lines = Files.readAllLines(tool.resolve("run.java"));
        TreeSet<String> listed = new TreeSet<>();
        for (String line : lines) {
            if (line.startsWith("//SOURCES ")) listed.add(line.substring("//SOURCES ".length()).trim());
        }

        TreeSet<String> actual = new TreeSet<>();
        try (Stream<Path> files = Files.walk(tool.resolve("src/main/java"))) {
            files.filter(f -> f.toString().endsWith(".java"))
                    .forEach(f -> actual.add(tool.relativize(f).toString().replace('\\', '/')));
        }

        assertEquals(actual, listed, "run.java //SOURCES must list exactly the files under src/main/java");
    }
}
