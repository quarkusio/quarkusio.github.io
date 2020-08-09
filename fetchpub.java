//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0
//DEPS org.kohsuke:github-api:1.115
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9
//DEPS com.fasterxml.jackson.core:jackson-databind:2.9.9

import static java.lang.System.out;
import static java.util.Arrays.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
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

    @CommandLine.Option(names="--close-existing", defaultValue = "false")
    boolean closeExisting;

    @CommandLine.Option(names="--token")
    String token;

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

        
        GitHub github;

        if(token == null) {
            github = GitHub.connectAnonymously();
        } else {
            out.println("Logging in using token...");
            github = GitHub.connectUsingOAuth(token);
        }

        var ghRepo = github.getRepository("quarkusio/quarkus");

        var list = github.searchIssues().isOpen().q("repo:quarkusio/quarkusio.github.io").q("label:publication").list().asList();

        list = new ArrayList(list);
        list.sort((GHIssue issue1, GHIssue issue2) -> Integer.compare(issue1.getNumber(), issue2.getNumber()));

        var included = new ArrayList<GHIssue>();
        for(GHIssue issue:list) {
            String snippet = issue.getBody().replace("```", "");
            try {
            Publication p = mapper.readValue(snippet, Publication[].class)[0];
            
            if(urls.contains(p.url)) {
                if(closeExisting) {
                    out.println("Closing issue #" + issue.getNumber() + " as already included.");
                    issue.comment("Closed by fetchpub as detected to already be included.");
                } else {
                    out.println("#" + issue.getNumber() + " already included. Use --close-existing if should be closed.");
                }
            } else {
                out.println("#" + issue.getNumber()    );
                out.println(issue.getBody().replace("```", ""));
                included.add(issue);
            }
        } catch (JsonMappingException me) {
            System.err.println("error reading " + issue.getHtmlUrl());
            me.printStackTrace();
            System.exit(-1);
        }
        }

        out.println();
        for(GHIssue issue:included) {
            out.println(issue.getTitle() + ", Fixes #" + issue.getNumber());
        }

        return 0;
    }
}