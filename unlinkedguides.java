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
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "UnlinkedGuides", mixinStandardHelpOptions = true, version = "UnlinkedGuides 0.1",
        description = "UnlinkedGuides made with jbang")
class unlinkedguides implements Callable<Integer> {

    @Parameters(index = "0", description = "The version of guides", defaultValue = "latest")
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
        String guidesPath = "./_data/guides-" + version + ".yaml";
        Categories cats = yaml.loadAs(new FileReader(guidesPath), Categories.class);


        System.out.println("Unlinked guides in " + guidesPath);

        List<String> guides = cats.categories.stream()
                .flatMap(c -> c.guides.stream())
                .map(g -> g.url.replace("/guides/", ""))
                .collect(Collectors.toList());

        Files.find(Path.of(getGuidesPath(version)), 1, (p, a) -> p.toString().endsWith(".adoc"))
                .map(p -> p.getFileName().toString().replace(".adoc", ""))
                .filter(p -> !guides.contains(p))
                .forEach(System.out::println);

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
