///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.yaml:snakeyaml:1.29

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "UnlinkedGuides", mixinStandardHelpOptions = true, version = "UnlinkedGuides 0.1",
        description = "UnlinkedGuides made with jbang")
class unlinkedguides implements Callable<Integer> {

    private static final String LATEST = "latest";

    private static final Set<String> IGNORED_ENTRIES = Set.of(
        "0-glossary",
        "platform-include",
        "quarkus-intro",
        "duration-format-note",
        "status-include",
        "quarkus-blaze-persistence",
        "attributes",
        "attributes-local",
        "smallrye-kafka-incoming",
        "smallrye-kafka-outgoing",
        "faq",
        "building-substrate-howto",
        "README",
        "gradle-config",
        "amazon-credentials",
        "kogito-dev-services-build-time-config"
    );

    @Parameters(index = "0", description = "The version of guides", defaultValue = LATEST)
    private String version;

    public static void main(String... args) {
        int exitCode = new CommandLine(new unlinkedguides()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml(representer);
        yaml.setBeanAccess(BeanAccess.FIELD);
        String guidesPath = "_data/guides-" + version + ".yaml";
        Categories cats = yaml.loadAs(new FileReader(guidesPath), Categories.class);


        System.out.println("Guides not linked in " + guidesPath + ", please add entries there:\n");

        List<String> guides = cats.categories.stream()
                .flatMap(c -> c.guides.stream())
                .map(g -> g.url.replace("/guides/", ""))
                .collect(Collectors.toList());

        Files.find(Path.of(getGuidesPath(version)), 1, (p, a) -> p.toString().endsWith(".adoc"))
                .map(p -> p.getFileName().toString().replace(".adoc", ""))
                .filter(p -> !guides.contains(p))
                .filter(p -> !IGNORED_ENTRIES.contains(p))
                .map(p -> "- " + p + "\n  › https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/" + p + ".adoc")
                .sorted()
                .forEach(System.out::println);

        if (LATEST.equals(version)) {
            System.out.println("\nCheck that they have not been already added to _data/guides-main.yaml. If so just copy the entries to guides-latest.yaml.");
            System.out.println("Also please keep guides-main.yaml in sync.\n");
        }

        return 0;
    }

    String getGuidesPath(String version) {
        if (version.equals("latest")) {
            return "./_guides";
        } else {
            return "./_versions/" + version + "/guides";
        }
    }
}

class Categories {
    List<Category> categories;
}

class Category {
    List<Guide> guides;
    String category;
    // String catId;
}

class Guide {
    String title;
    String url;
    String description;
    String keywords;
}
