//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0
//DEPS org.kohsuke:github-api:1.115
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.9
//DEPS com.fasterxml.jackson.core:jackson-databind:2.9.9

import static java.lang.System.out;
import static java.util.Arrays.*;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.kohsuke.github.*;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "fetchpub", mixinStandardHelpOptions = true, version = "fetchpub 0.1", description = "fetchpub made with jbang")
class fetchpub implements Callable<Integer> {

    @CommandLine.Option(names = "--close-existing", defaultValue = "false")

    boolean closeExisting;

    @CommandLine.Option(names = "--token")
    String token;

    @CommandLine.Option(names = "--verbose")
    boolean verbose;

    @Parameters(index = "0", description = "Location of publications", defaultValue = "_data/publications.yaml")
    private File pubfile;

    public static void main(String... args) {
        int exitCode = new CommandLine(new fetchpub()).execute(args);
        System.exit(exitCode);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static public class Publication {

        public String url;

        Map<String, Object> unknownFields = new HashMap<>();

        // Capture all other fields that Jackson do not match other members
        @JsonAnyGetter
        public Map<String, Object> otherFields() {
            return unknownFields;
        }

        @JsonAnySetter
        public void setOtherField(String name, Object value) {
            unknownFields.put(name, value);
        }
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        List<Publication> publications = mapper.readValue(pubfile, new TypeReference<List<Publication>>() {
        });

        Set<String> urls = new HashSet<>();
        Set<String> sources = new HashSet<>();

        for (var pub : publications) {
            urls.add(pub.url);
            sources.add((String)pub.otherFields().get("source"));
        }

        GitHub github;

        if (token == null) {
            github = GitHub.connectAnonymously();
        } else {
            out.println("Logging in using token...");
            github = GitHub.connectUsingOAuth(token);
        }

        var ghRepo = github.getRepository("quarkusio/quarkus");

        var list = github.searchIssues().isOpen().q("repo:quarkusio/quarkusio.github.io").q("label:publication").q("is:issue").list()
                .asList();

        System.out.println("Found " + list.size() + " issues with label 'publication'");

        Set<String> duplicateSources = new HashSet<>();

        list = new ArrayList(list);
        list.sort((GHIssue issue1, GHIssue issue2) -> Integer.compare(issue1.getNumber(), issue2.getNumber()));

        var included = new ArrayList<GHIssue>();
        for (GHIssue issue : list) {
            String snippet = issue.getBody().replace("```", "");
            try {
                Publication p = mapper.readValue(snippet, Publication[].class)[0];
                p.setOtherField("source", issue.getHtmlUrl());
                if (urls.contains(p.url)) {
                    if (closeExisting) {

                        out.println("Closing issue #" + issue.getNumber() + " as already included.");
                        issue.comment("Closed by fetchpub as detected to already be included.");

                        issue.close();

                    } else {
                        out.println("#" + issue.getNumber()
                                + " already included. Use --close-existing if should be closed.");

                    }
                } else if(!sources.contains(issue.getHtmlUrl().toString())) {
                    out.println("#" + issue.getNumber());
                    out.println(issue.getBody().replace("```", ""));
                    included.add(issue);
                    publications.add(p);
                } else {
                    out.println("Issue #" + issue.getNumber() + " already sourced - ignoring.");
                    duplicateSources.add(issue.getHtmlUrl().toString());
                }
            } catch (JsonMappingException me) {
                System.err.println("Skipping " + issue.getNumber() + " as error reading " + issue.getHtmlUrl());
                System.err.println(me.getMessage());
                if(verbose) {
                    me.printStackTrace();
                }
                //System.exit(-1);
            }
        }

        out.println();
        for (GHIssue issue : included) {
            out.println(issue.getTitle() + ", Fixes #" + issue.getNumber());
        }

        
        out.println("Writing back to " + pubfile);
        FileOutputStream fos = new FileOutputStream(pubfile);
        SequenceWriter sw = mapper.writerWithDefaultPrettyPrinter().writeValues(fos);
        sw.write(publications);

        // Set to store the duplicate elements 
        Set<String> items = new HashSet<>(); 
  
        var duplicates = publications.stream()
            .filter(n -> !items.add(n.url)) 
            .collect(Collectors.toSet()); 

        System.out.println("Duplicate urls:" + duplicates.size());
        duplicates.forEach(p -> out.println("Duplicate: " + p.url));
 
        System.out.println("Already sourced:" + duplicateSources.size());
        duplicateSources.forEach(p -> out.println("Duplicate Sourced: " + p));
 
        return 0;
    }
}