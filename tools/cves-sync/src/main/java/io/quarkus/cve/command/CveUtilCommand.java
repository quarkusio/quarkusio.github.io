package io.quarkus.cve.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Root command for the cve-util CLI.
 *
 * Usage examples:
 *   cve-util list
 *   cve-util list --ecosystem Maven --package io.quarkus:quarkus-core
 *   cve-util get GHSA-xxxx-xxxx-xxxx
 *   cve-util search --keyword "deserialization"
 */
@Command(
    name = "cve-util",
    mixinStandardHelpOptions = true,
    version = "cve-util 1.0.0",
    description = "Query the OSV (https://osv.dev) database for Quarkus CVEs.",
    subcommands = {
        ListCommand.class,
        GetCommand.class,
        SearchCommand.class,
        ReleasesCommand.class
    }
)
public class CveUtilCommand implements Runnable {

    @Option(names = {"--no-cache"}, scope = picocli.CommandLine.ScopeType.INHERIT,
            description = "Do not read or write the local cache (every request goes upstream)")
    boolean noCache;

    @Option(names = {"--refresh"}, scope = picocli.CommandLine.ScopeType.INHERIT,
            description = "Ignore cached data and re-download everything, then update the cache")
    boolean refresh;

    @Option(names = {"--cache-dir"}, paramLabel = "<dir>", scope = picocli.CommandLine.ScopeType.INHERIT,
            description = "Cache directory (default: $CVE_UTIL_CACHE_DIR, else %%LOCALAPPDATA%%\\cve-util\\cache "
                    + "on Windows, else $XDG_CACHE_HOME/cve-util or ~/.cache/cve-util)")
    java.nio.file.Path cacheDir;

    @Option(names = {"-v", "--verbose"}, scope = picocli.CommandLine.ScopeType.INHERIT,
            description = "Increase verbosity; repeat for more detail (-v = level 1 ... -vvvvv = level 5)")
    boolean[] verbose = new boolean[0];

    @Option(names = {"--verbosity"}, paramLabel = "<0-5>", scope = picocli.CommandLine.ScopeType.INHERIT,
            description = "Set verbosity level explicitly: 0=quiet (default), 1=stages, 2=batches/progress, "
                    + "3=per-package/vuln, 4=HTTP requests, 5=headers + stack traces")
    Integer verbosity;

    /** Applies -v / --verbosity and the cache options globally. Call first in each command. */
    public void applyVerbosity() {
        int level = verbosity != null ? verbosity : verbose.length;
        io.quarkus.cve.util.Stats.reset();
        io.quarkus.cve.util.Log.setLevel(level);
        io.quarkus.cve.util.Cache.configure(cacheDir,
                noCache ? io.quarkus.cve.util.Cache.Mode.OFF
                        : refresh ? io.quarkus.cve.util.Cache.Mode.REFRESH
                        : io.quarkus.cve.util.Cache.Mode.NORMAL);
    }

    @Override
    public void run() {
        // Print help when called with no subcommand
        new picocli.CommandLine(this).usage(System.out);
    }
}
