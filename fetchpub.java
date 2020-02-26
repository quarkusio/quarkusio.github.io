//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0
//DEPS org.kohsuke:github-api:1.101
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9
//DEPS com.fasterxml.jackson.core:jackson-databind:2.9.9

import static java.lang.System.out;
import static java.util.Arrays.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kohsuke.github.*;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "fetchpub", mixinStandardHelpOptions = true, version = "fetchpub 0.1",
        description = "fetchpub made with jbang")
class fetchpub implements Callable<Integer> {

    @Parameters(index = "0", description = "Location of publications", defaultValue = "_data/publications.yaml")
    private File pubfile;

    public static void main(String... args) {
        int exitCode = new CommandLine(new fetchpub()).execute(args);
        System.exit(exitCode);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Publication {
        public String url;
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<Publication> publications = mapper.readValue(pubfile, new TypeReference<List<Publication>>(){});

        Set<String> urls = new HashSet<>();

        for(var pub:publications) {
            urls.add(pub.url);
        }

        
        GitHub github = GitHub.connectAnonymously();

        var ghRepo = github.getRepository("quarkusio/quarkus");

        var list = github.searchIssues().isOpen().q("repo:quarkusio/quarkusio.github.io").q("label:publication").list().asList();

        list.sort((GHIssue issue1, GHIssue issue2) -> Integer.compare(issue1.getNumber(), issue2.getNumber()));

        var included = new ArrayList<GHIssue>();
        for(GHIssue issue:list) {
            String snippet = issue.getBody().replace("```", "");
            Publication p = mapper.readValue(snippet, Publication[].class)[0];
            if(urls.contains(p.url)) {
                out.println("#" + issue.getNumber() + " already included");
            } else {
                out.println("#" + issue.getNumber()    );
                out.println(issue.getBody().replace("```", ""));
                included.add(issue);
            }
        }

        out.println();
        for(GHIssue issue:included) {
            out.println(issue.getTitle() + ", Fixes #" + issue.getNumber());
        }

        return 0;
    }
}