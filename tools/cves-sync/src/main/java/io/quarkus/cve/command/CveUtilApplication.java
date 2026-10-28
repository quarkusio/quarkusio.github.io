package io.quarkus.cve.command;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Main Quarkus entry point — delegates to picocli's CommandLine.
 */
@QuarkusMain
public class CveUtilApplication implements QuarkusApplication {

    @Inject
    IFactory factory;

    @Override
    public int run(String... args) {
        return new CommandLine(new CveUtilCommand(), factory).execute(args);
    }
}
