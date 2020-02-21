//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.2.0
//DEPS org.kohsuke:github-api:1.101

import static java.lang.System.out;
import static java.util.Arrays.*;
import org.kohsuke.github.*;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "fetchpub", mixinStandardHelpOptions = true, version = "fetchpub 0.1",
        description = "fetchpub made with jbang")
class fetchpub implements Callable<Integer> {

    @Parameters(index = "0", description = "The greeting to print", defaultValue = "World!")
    private String greeting;

    public static void main(String... args) {
        int exitCode = new CommandLine(new fetchpub()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception { // your business logic goes here...
        GitHub github = GitHub.connectAnonymously();

        var ghRepo = github.getRepository("quarkusio/quarkus");

        var list = github.searchIssues().isOpen().q("repo:quarkusio/quarkusio.github.io").q("label:publication").list().asList();

        for(GHIssue issue:list) {
            out.println(issue.getBody().replace("```", ""));
        }

        out.println();
        for(GHIssue issue:list) {
            out.println(issue.getTitle() + ", Fixes #" + issue.getNumber());
        }

        return 0;
    }
}
