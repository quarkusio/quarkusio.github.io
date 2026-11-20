# Command mode with Aesh

[Aesh](https://github.com/aeshell/aesh) is a Java library for building interactive command line applications. It supports option parsing, tab completion, command grouping, and an interactive shell (REPL) mode. For more details about the Aesh library itself, see the [Aesh documentation](https://aeshell.github.io/).

Quarkus provides support for using Aesh. This guide contains examples of the `aesh` extension usage.

> [!IMPORTANT]
> If you are not familiar with the Quarkus Command Mode, consider reading the [Command Mode reference guide](/guides/command-mode-reference/index.md) first.

## Extension

Once you have your Quarkus project configured you can add the `aesh` extension to your project by running the following command in your project base directory.

**CLI**

```bash
quarkus extension add aesh
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='aesh'
```

**Gradle**

```bash
./gradlew addExtension --extensions='aesh'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-aesh</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-aesh")
```

For optimal performance (especially in native mode), add the Aesh annotation processor to your Maven compiler plugin configuration. This generates compile-time command metadata, eliminating reflection-based annotation scanning at runtime:

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.aesh</groupId>
                <artifactId>aesh-processor</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

> [!NOTE]
> The annotation processor is optional. Without it, Aesh falls back to runtime reflection for command metadata, which works correctly but is slower at startup.

## Building a command line application

The Aesh extension supports two execution modes, controlled by the `quarkus.aesh.mode` build-time property. Console mode is what makes Aesh distinctive -- it provides an interactive shell (REPL) where users can type multiple commands with tab completion and command history. Runtime mode executes a single command and exits, like a standard CLI tool. In most cases, the mode is auto-detected based on your command annotations.

### Console mode (interactive shell)

Console mode starts an interactive shell (REPL) where users can type multiple commands. This is what differentiates Aesh from other CLI frameworks: users get an interactive session with tab completion, command history, and shared state across commands.

Console mode is automatically selected when there are multiple `@CommandDefinition` classes without `groupCommands`, when there are multiple independent commands, or when a remote transport (WebSocket or SSH) is present. You can also force it with `quarkus.aesh.mode=console`.

```java
package com.acme.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "greet", description = "Greet someone")
public class GreetCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", defaultValue = "World")
    private String name;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Hello " + name + "!");
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "calc", description = "Add two numbers")
class CalcCommand implements Command<CommandInvocation> {

    @Option(shortName = 'a', name = "a", defaultValue = "0")
    private int a;

    @Option(shortName = 'b', name = "b", defaultValue = "0")
    private int b;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Result: " + (a + b));
        return CommandResult.SUCCESS;
    }
}
```

With two commands, console mode is auto-detected. Without arguments, the interactive REPL starts:

```shell
$ java -jar myapp.jar
[quarkus]$ greet --name=Aesh
Hello Aesh!
[quarkus]$ calc -a 5 -b 3
Result: 8
[quarkus]$ exit
```

If command-line arguments are provided, the command is executed once and the application exits -- just like runtime mode:

```shell
$ java -jar myapp.jar greet --name=Aesh
Hello Aesh!
```

This allows console mode applications to be used both interactively and in scripts.

The prompt, exit command, and other console settings are configurable via `quarkus.aesh.*` properties.

#### Group commands in console mode (sub-command mode)

You can use `@CommandDefinition` with `groupCommands` in console mode to create command groups with sub-command mode. When a user types a group command without a sub-command, they enter an interactive context for that group:

```java
package com.acme.aesh;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;


@CommandDefinition(name = "task", description = "Task management",
        groupCommands = {AddTask.class, ListTasks.class}) // <1>
public class TaskGroup implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Sub-commands: add, list");
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "add", description = "Add a task") // <2>
class AddTask implements Command<CommandInvocation> {

    @Argument(description = "Task name", required = true)
    private String name;

    @Inject // <3>
    TaskService taskService;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        taskService.addTask(name);
        invocation.println("Added: " + name);
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "list", description = "List tasks")
class ListTasks implements Command<CommandInvocation> {

    @Inject
    TaskService taskService;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        taskService.getTasks().forEach(t -> invocation.println("  - " + t));
        return CommandResult.SUCCESS;
    }
}
```

1. Sub-commands listed in `groupCommands` are only accessible through their parent group -- they do not appear as top-level commands in the shell.
2. Sub-commands use `@CommandDefinition` without `groupCommands`. The extension automatically excludes them from top-level registration.
3. CDI injection works in sub-commands. The extension injects `@Inject` fields after Aesh creates the sub-command instances.

An interactive session using sub-command mode:

```shell
$ java -jar myapp.jar
[quarkus]$ task add groceries          # <1>
Added: groceries
[quarkus]$ task                        # <2>
task> add laundry                      # <3>
Added: laundry
task> list
  - groceries
  - laundry
task> exit                             # <4>
[quarkus]$ exit
```

1. Sub-commands can be called directly with their parent prefix.
2. Typing just the group command name enters sub-command mode.
3. Inside sub-command mode, type sub-commands directly without the parent prefix.
4. Type `exit` (or `..`) to leave sub-command mode and return to the main prompt.

> [!NOTE]
> Because `TaskService` is `@ApplicationScoped`, state is shared across all commands in the session. This is a key advantage of console mode over runtime mode, where each invocation is a separate process.

### Runtime mode (single command execution)

This is the default mode when there is a single `@CommandDefinition` or a `@CommandDefinition` with `groupCommands`. The application executes one command and exits, similar to standard CLI tools.

#### Simple application

A simple Aesh application with a single command can be created as follows:

```java
package com.acme.aesh;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;

@CommandDefinition(name = "hello", description = "Greet someone") // <1>
public class HelloCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", description = "Who to greet", defaultValue = "World")
    private String name;

    @Inject // <2>
    GreetingService greetingService;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println(greetingService.greet(name));
        return CommandResult.SUCCESS;
    }
}
```

1. If there is only one class annotated with `@CommandDefinition`, it is automatically used as the entry point of the command line application.
2. All command classes are registered as CDI beans. You can use `@Inject` to inject other beans.

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class GreetingService {
    String greet(String name) {
        return "Hello " + name + "!";
    }
}
```

> [!IMPORTANT]
> Beans annotated with `@CommandDefinition` should not use proxied scopes (e.g. do not use `@ApplicationScoped`) because Aesh sets field values directly via reflection. By default, the Aesh extension registers command classes with the `@Dependent` scope.

#### Grouped commands with subcommands

```java
package com.acme.aesh;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Option;
import org.aesh.command.option.ParentCommand;


@CommandDefinition(name = "cli", description = "CLI application",
        groupCommands = {RunCommand.class, InfoCommand.class}) // <1>
public class CliCommand implements Command<CommandInvocation> {

    @Option(shortName = 'v', name = "verbose", hasValue = false)
    private boolean verbose;

    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Use a subcommand: run, info");
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "run", description = "Run a task")
class RunCommand implements Command<CommandInvocation> {

    @ParentCommand // <2>
    private CliCommand parent;

    @Argument(description = "Task name")
    private String taskName;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        if (parent.isVerbose()) {
            invocation.println("[VERBOSE] Running...");
        }
        invocation.println("Running task: " + taskName);
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "info", description = "Show info")
class InfoCommand implements Command<CommandInvocation> {

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("CLI App v1.0");
        return CommandResult.SUCCESS;
    }
}
```

1. `groupCommands` lists the subcommands available under this parent command.
2. `@ParentCommand` injects the parent command, giving access to parent options like `--verbose`.

The application can then be invoked as:

```shell
$ java -jar myapp.jar run build
Running task: build

$ java -jar myapp.jar info
CLI App v1.0
```

#### Programmatic sub-commands with GroupCommand

Instead of declaring sub-commands in the `groupCommands` annotation attribute, you can implement the `GroupCommand` interface and override `getCommands()` to register sub-commands programmatically. This is useful when sub-commands need constructor arguments or when the set of sub-commands is determined at runtime:

```java
package com.acme.aesh;

import java.util.List;

import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommand;
import org.aesh.command.invocation.CommandInvocation;

@CommandDefinition(name = "tools", description = "Developer tools")
public class ToolsCommand implements GroupCommand<CommandInvocation> { // <1>

    @Inject
    StatusCommand statusSub; // <2>

    @Override
    public List<Command<CommandInvocation>> getCommands() { // <3>
        return List.of(statusSub);
    }

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Use a sub-command: status");
        return CommandResult.SUCCESS;
    }
}

@CommandDefinition(name = "status", description = "Show status")
class StatusCommand implements Command<CommandInvocation> {

    @Inject
    StatusService statusService; // <4>

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println(statusService.getStatus());
        return CommandResult.SUCCESS;
    }
}
```

1. Implement `GroupCommand` instead of plain `Command`. Do not use `groupCommands` in the annotation -- the sub-commands come from `getCommands()` instead.
2. Inject the sub-command via CDI so that `@Inject` fields in the sub-command are properly populated.
3. Return CDI-managed instances from `getCommands()`. Aesh uses these instances directly, preserving CDI injection.
4. CDI injection works in sub-commands because they are managed by CDI, not created with `new`.

> [!IMPORTANT]
> Sub-commands returned by `getCommands()` must be injected via `@Inject` (not created with `new`) if they need CDI services. Creating sub-commands with `new` bypasses CDI, and `@Inject` fields will not be populated.

## Custom option completers, converters, and validators

Aesh supports custom implementations for tab-completion, type conversion, option validation, and more. These implementations are CDI beans, so they can inject services:

```java
package com.acme.aesh;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Option;
import org.aesh.command.validator.OptionValidator;
import org.aesh.command.validator.OptionValidatorException;
import org.aesh.command.validator.ValidatorInvocation;

@CommandDefinition(name = "deploy", description = "Deploy to an environment")
public class DeployCommand implements Command<CommandInvocation> {

    @Option(name = "env", completer = EnvCompleter.class, validator = EnvValidator.class) // <1>
    private String environment;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        invocation.println("Deploying to " + environment);
        return CommandResult.SUCCESS;
    }
}

@Dependent // <2>
class EnvCompleter implements OptionCompleter<CompleterInvocation> {

    @Inject // <3>
    EnvironmentService envService;

    @Override
    public void complete(CompleterInvocation completerInvocation) {
        String input = completerInvocation.getGivenCompleteValue();
        for (String env : envService.getAvailableEnvironments()) {
            if (input == null || input.isEmpty() || env.startsWith(input)) {
                completerInvocation.addCompleterValue(env);
            }
        }
    }
}

@Dependent
class EnvValidator implements OptionValidator<ValidatorInvocation<String, ?>> {
    private static final List<String> VALID = List.of("dev", "staging", "prod");

    @Override
    public void validate(ValidatorInvocation<String, ?> validatorInvocation) throws OptionValidatorException {
        if (!VALID.contains(validatorInvocation.getValue())) {
            throw new OptionValidatorException("Invalid environment. Valid values: " + VALID);
        }
    }
}
```

1. Reference custom completers, validators, and converters via their class in annotation attributes.
2. CDI beans implementing aesh interfaces (`OptionCompleter`, `OptionValidator`, `Converter`, `CommandActivator`, etc.) are automatically kept by Arc even though they are only referenced from annotations.
3. Completers can inject CDI services. This is particularly useful in console mode, where a completer can offer context-aware suggestions based on application state (e.g., completing task names from a service).

## Mixins (reusable option groups)

Mixins allow you to define a set of options in a separate class and reuse them across multiple commands. Use the `@Mixin` annotation to include a mixin's options in a command:

```java
package com.acme.aesh;

import org.aesh.command.option.Mixin;
import org.aesh.command.option.Option;

public class OutputMixin { // <1>

    @Option(name = "format", shortName = 'f', defaultValue = "text")
    public String format;

    @Option(name = "verbose", shortName = 'v', hasValue = false)
    public boolean verbose;
}
```

1. The mixin class is a plain Java class with `@Option` fields. It does not need any annotations on the class itself.

Commands include the mixin with `@Mixin`:

```java
@CommandDefinition(name = "greet", description = "Greet someone")
public class GreetCommand implements Command<CommandInvocation> {

    @Option(shortName = 'n', name = "name", defaultValue = "World")
    private String name;

    @Mixin // <1>
    private OutputMixin output;

    @Override
    public CommandResult execute(CommandInvocation invocation) {
        String greeting = "Hello " + name + "!";
        if ("json".equals(output.format)) {
            invocation.println("{\"greeting\":\"" + greeting + "\"}");
        } else {
            invocation.println(greeting);
        }
        return CommandResult.SUCCESS;
    }
}
```

1. The mixin's options (`--format`, `--verbose`) are added to the command as if they were declared directly on it. Tab completion and help output include them automatically.

## Dynamic default values

You can provide dynamic default values for command options by implementing the `DefaultValueProvider` interface as a CDI bean. The extension automatically discovers and applies it to all commands:

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.aesh.command.DefaultValueProvider;
import org.aesh.command.impl.internal.ProcessedOption;

@ApplicationScoped // <1>
public class ConfigBasedDefaults implements DefaultValueProvider {

    @Inject
    AppConfig config;

    @Override
    public String defaultValue(ProcessedOption option) throws Exception {
        return config.getDefault(option.name()); // <2>
    }
}
```

1. Register as a CDI bean. The extension discovers it automatically -- no additional wiring needed.
2. Return `null` to use the annotation-declared default. Return a value to override it at runtime.

Per-command providers can also be specified via `@CommandDefinition(defaultValueProvider = MyProvider.class)`.

## Option features

### Allowed values

Restrict an option to a fixed set of values with `allowedValues`. Invalid values are rejected at parse time, and tab completion automatically suggests the allowed values:

```java
@Option(name = "env", allowedValues = {"dev", "staging", "prod"})
private String environment;
```

### Fallback values

Use `fallbackValue` for three-state options where the option can be omitted, specified without a value, or specified with an explicit value:

```java
@Option(name = "debug", fallbackValue = "5005", hasValue = true) // <1>
private String debugPort;
```

1. `--debug` alone uses the fallback value `"5005"`. `--debug=8000` uses the explicit value. Omitting the option entirely uses the `defaultValue` (if set).

## Customizing CLI settings

You can customize the underlying Aesh `SettingsBuilder` by implementing the `CliSettings` interface:

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.settings.SettingsBuilder;

import io.quarkus.aesh.runtime.CliSettings;

@ApplicationScoped
public class MyCliSettings implements CliSettings {

    @Override
    public void customize(SettingsBuilder<?, ?, ?, ?, ?, ?> builder) {
        builder.enableAlias(true)
                .persistHistory(true)
                .historySize(1000);
    }
}
```

Multiple `CliSettings` beans can be registered. They are applied in arbitrary order, so avoid conflicting settings across implementations.

### Custom CommandInvocation

You can use `CliSettings` to register a custom `CommandInvocationProvider` that wraps the default `CommandInvocation` with application-specific context. This is useful when commands need shared state or services that are not available through CDI injection:

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.CommandInvocationProvider;
import org.aesh.command.settings.SettingsBuilder;

import io.quarkus.aesh.runtime.CliSettings;

@ApplicationScoped
public class MyCliSettings implements CliSettings {

    @Inject
    MyAppContext appContext;

    @Override
    public void customize(SettingsBuilder<?> builder) {
        builder.commandInvocationProvider(new MyInvocationProvider(appContext));
    }
}
```

Commands can then declare the custom invocation type and access the additional context:

```java
@CommandDefinition(name = "status", description = "Show status")
public class StatusCommand implements Command<MyCommandInvocation> {

    @Override
    public CommandResult execute(MyCommandInvocation invocation) {
        invocation.println("Context: " + invocation.getAppContext().getName());
        return CommandResult.SUCCESS;
    }
}
```

The custom `CommandInvocationProvider` is applied in all execution modes: console REPL, console single-command execution (with arguments), and runtime mode.

Similarly, you can register custom `ConverterInvocationProvider` and `ValidatorInvocationProvider` implementations to pass additional context to converters and validators.

## Command execution listener

Implement `CommandExecutionListener` as a CDI bean to receive a callback after each command completes. This is useful for logging, metrics, or audit trails:

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;

import org.aesh.command.CommandExecutionListener;
import org.aesh.command.CommandResult;

@ApplicationScoped
public class CommandLogger implements CommandExecutionListener {

    @Override
    public void onCommandComplete(String commandLine, CommandResult result, long executionTimeMs) {
        System.out.println("Executed: " + commandLine + " in " + executionTimeMs + "ms");
    }
}
```

The listener fires in both console mode (after each REPL command) and when arguments are provided on the command line.

## Command not found handler

Implement `CommandNotFoundHandler` as a CDI bean to customize the error message when an unknown command is typed:

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.function.Consumer;

import org.aesh.command.CommandNotFoundHandler;

@ApplicationScoped
public class MyNotFoundHandler implements CommandNotFoundHandler {

    @Override
    public void handleCommandNotFound(String line, Consumer<String> output) {
        output.accept("Unknown command: " + line + ". Type 'help' for available commands.");
    }
}
```

## Build-time validation

The Aesh extension validates command configurations at build time. Invalid configurations are reported as deployment failures with descriptive error messages, so you can fix them before the application starts.

The following checks are performed:

- **Duplicate top-level command names** -- two or more top-level command classes with the same `@CommandDefinition(name = "...")` value.
- **Missing `@CommandDefinition` on group sub-commands** -- a class listed in `@CommandDefinition(groupCommands = {...})` that is not annotated with `@CommandDefinition`. \*

## Remote terminal access

The Aesh extension provides optional sub-extensions for remote terminal access via WebSocket and SSH. These allow users to interact with your CLI application from a browser or an SSH client while the application is running in console mode.

### WebSocket terminal

Add the `quarkus-aesh-websocket` dependency to expose a browser-based terminal:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-aesh-websocket</artifactId>
</dependency>
```

Once added, the terminal is accessible at `http://localhost:8080/aesh/index.html` when the application is running.

The built-in terminal page uses [xterm.js](https://xtermjs.org/), which is bundled with the extension. No external CDN or internet access is required.

The WebSocket endpoint is registered at `/aesh/terminal` by default. You can change the endpoint path:

```properties
quarkus.aesh.websocket.path=/custom/terminal
```

> [!NOTE]
> The built-in HTML page connects to the default path. When using a custom path, access the page with a `path` query parameter: `http://localhost:8080/aesh/index.html?path=/custom/terminal`.

To disable the WebSocket endpoint:

```properties
quarkus.aesh.websocket.enabled=false
```

#### WebSocket authentication

The WebSocket terminal endpoint has no authentication by default. You can secure it by requiring an authenticated user or restricting access to specific roles. Both options require a Quarkus Security extension to be present (e.g., `quarkus-elytron-security-properties-file`).

To require any authenticated user:

```properties
quarkus.aesh.websocket.authenticated=true
```

To restrict access to specific roles:

```properties
quarkus.aesh.websocket.roles-allowed=admin,operator
```

When `roles-allowed` is set, `authenticated` is ignored since role-based access implies authentication.

> [!NOTE]
> The extension will log a warning at build time if the WebSocket terminal is enabled in production without authentication configured.

### SSH terminal

Add the `quarkus-aesh-ssh` dependency to expose an SSH server:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-aesh-ssh</artifactId>
</dependency>
```

Connect to the terminal with any SSH client:

```shell
ssh -p 2222 localhost
```

Configuration properties:

```properties
# SSH server port (default: 2222)
quarkus.aesh.ssh.port=2222

# SSH server bind address (default: localhost)
quarkus.aesh.ssh.host=localhost

# Path to an OpenSSH authorized_keys file for public key authentication
# (recommended for production)
quarkus.aesh.ssh.authorized-keys-file=/path/to/authorized_keys

# Password for SSH authentication (default: any password accepted)
# For production, use an environment variable instead of plaintext:
# quarkus.aesh.ssh.password=${SSH_PASSWORD}
quarkus.aesh.ssh.password=mysecret

# Path to the host key file (default: hostkey.ser)
quarkus.aesh.ssh.host-key-file=hostkey.ser

# Enable or disable the SSH server at build time (default: true)
quarkus.aesh.ssh.enabled=true
```

Password and public key authentication can be used simultaneously. When both are configured, clients can authenticate with either method. If neither is configured, any password is accepted.

> [!NOTE]
> The extension will log a warning at startup if the SSH server is running without any authentication configured (no password, no authorized-keys-file).

### Connection management

Both SSH and WebSocket transports support limiting concurrent sessions and closing idle connections.

```properties
# Maximum concurrent SSH sessions (default: no limit)
quarkus.aesh.ssh.max-connections=10

# Close idle SSH sessions after a duration (default: disabled)
quarkus.aesh.ssh.idle-timeout=30m

# Maximum concurrent WebSocket sessions (default: no limit)
quarkus.aesh.websocket.max-connections=10

# Close idle WebSocket sessions after a duration (default: disabled)
quarkus.aesh.websocket.idle-timeout=30m
```

When `max-connections` is set, new connections beyond the limit are rejected immediately. When `idle-timeout` is set, sessions with no input activity for the specified duration are closed automatically.

### Session events

The extension fires CDI events when remote sessions open and close. Use these to monitor session lifecycle, log access, or perform cleanup:

```java
package com.acme.aesh;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

import io.quarkus.aesh.runtime.AeshSessionEvent;
import io.quarkus.aesh.runtime.SessionClosed;
import io.quarkus.aesh.runtime.SessionOpened;

@ApplicationScoped
public class SessionLogger {

    void onOpen(@ObservesAsync @SessionOpened AeshSessionEvent event) { // <1>
        System.out.println("Session opened: " + event.sessionId()
                + " via " + event.transport());
    }

    void onClose(@ObservesAsync @SessionClosed AeshSessionEvent event) {
        System.out.println("Session closed: " + event.sessionId()
                + " at " + event.timestamp());
    }
}
```

1. Events are fired asynchronously. Use `@ObservesAsync` (not `@Observes`) to receive them.

The `AeshSessionEvent` payload provides:

- `sessionId()` -- unique identifier for the session
- `transport()` -- `"ssh"` or `"websocket"`
- `timestamp()` -- when the event occurred

Events are fired for both SSH and WebSocket sessions.

### Health checks

When the `quarkus-smallrye-health` extension is present, readiness health checks are automatically registered for SSH and WebSocket transports. The health endpoints report whether each transport is running and the number of active connections.

To disable the health checks:

```properties
quarkus.aesh.ssh.health.enabled=false
quarkus.aesh.websocket.health.enabled=false
```

### Local console behavior with remote transports

When a remote transport extension (`quarkus-aesh-websocket` or `quarkus-aesh-ssh`) is present, the local console (stdin/stdout) is **not started by default**. This means the application starts as a normal Quarkus server, and CLI access is available only through the remote transport.

This auto-detection prevents the local console from blocking the terminal when the application is intended to run as a server with remote CLI access.

You can override this behavior with the `quarkus.aesh.start-console` property:

```properties
# Force start the local console alongside remote transports
quarkus.aesh.start-console=true

# Disable the local console even without remote transports
# (useful when embedding commands in a server application)
quarkus.aesh.start-console=false
```

The auto-detection logic:

- No remote transport present -> local console **starts** (default CLI application behavior)
- Remote transport present -> local console **skipped** (server mode with remote CLI access)
- `quarkus.aesh.start-console` explicitly set -> overrides auto-detection in either direction

### Security considerations

> [!WARNING]
> The WebSocket and SSH terminals provide remote access to your application's CLI. In production, always configure authentication:

- **SSH**: Use `quarkus.aesh.ssh.authorized-keys-file` for public key authentication (recommended), or set `quarkus.aesh.ssh.password` for password authentication (use `${SSH_PASSWORD}` to avoid storing the password in plaintext). Both methods can be used simultaneously. Without either, any password is accepted.
- **WebSocket**: Set `quarkus.aesh.websocket.authenticated=true` or `quarkus.aesh.websocket.roles-allowed`  to require authentication. This requires a Quarkus Security extension.

Additionally, restrict network access to these endpoints using firewalls or bind addresses. Use `idle-timeout` to automatically close abandoned sessions and `max-connections` to limit concurrent sessions and prevent resource exhaustion. Health checks (when `quarkus-smallrye-health` is present) provide visibility into the number of active sessions.

> [!NOTE]
> WebSocket connections are not protected by the browser's same-origin policy. If the WebSocket terminal is exposed on a network, consider configuring CORS via the `quarkus.http.cors` settings to restrict which origins can connect.

## Providing your own QuarkusMain

You can also provide your own application entry point annotated with `@QuarkusMain` (as described in [Command Mode reference guide](/guides/command-mode-reference/index.md)). When a `@QuarkusMain` is present, the Aesh extension will not register its own runner.

```java
package com.acme.aesh;

import jakarta.inject.Inject;

import org.aesh.AeshRuntimeRunner;
import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.aesh.command.invocation.CommandInvocation;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class CustomMain implements QuarkusApplication {

    @Inject
    AeshRuntimeRunner runner;

    @Override
    public int run(String... args) throws Exception {
        runner.args(args).execute();
        return 0;
    }
}
```

## Development Mode

In development mode (`mvn quarkus:dev`), the Quarkus dev console takes control of the terminal for its interactive features (live reload, test output, etc.). This means the local terminal is not available for aesh CLI interaction.

To test your CLI commands during development, you have the following options:

- **WebSocket terminal**: Add the `quarkus-aesh-websocket` dependency and access the CLI via browser at `http://localhost:8080/aesh/index.html`. The dev console stays on the local terminal while the aesh CLI runs in the browser.
- **Pass arguments**: Use `mvn quarkus:dev -Dquarkus.args='--help'` or `mvn quarkus:dev -Dquarkus.args='-n Quarkus'` to pass arguments directly. The application is re-executed every time the `Space bar` key is pressed. For Gradle projects, use `--quarkus-args`.
- **Build and run**: Build the application with `mvn package -DskipTests` and run it directly with `java -jar target/quarkus-app/quarkus-run.jar <command> [options]`.

### Dev UI

The Aesh extension provides Dev UI pages accessible at `http://localhost:8080/q/dev-ui` when running in development mode. The Aesh card in the Dev UI offers up to three pages depending on which sub-extensions are present.

#### Commands page

Always available. Shows a table of all discovered CLI commands with their name, description, type (Group or regular), class name, and sub-commands for group commands. The resolved execution mode (console or runtime) is displayed at the top. A search field allows filtering commands by name, description, or class name.

#### Sessions page

Available when a remote transport extension (`quarkus-aesh-websocket` or `quarkus-aesh-ssh`) is present. Displays transport cards showing the status, active session count, and maximum session limit for each transport. Below the cards, a live event log shows session opened and closed events in real-time as users connect and disconnect.

#### Terminal page

Available when the `quarkus-aesh-websocket` extension is present. Embeds an interactive terminal directly in the Dev UI using [xterm.js](https://xtermjs.org/). Click **Connect** to open a WebSocket connection to the terminal endpoint and interact with your CLI commands from the browser. The terminal supports the same features as the standalone terminal page (color, resize, unicode).

## Testing

Aesh commands can be tested with `@QuarkusMainTest` from the [Command Mode reference guide](/guides/command-mode-reference/index.md). The approach differs depending on the execution mode.

### Testing runtime mode commands

Runtime mode commands execute once and exit. Use `@Launch` to run them with specific arguments and assert on the output:

```java
@QuarkusMainTest
public class CliTest {

    @Test
    @Launch({"--name=Alice"})
    void testGreet(LaunchResult result) {
        assertThat(result.getOutput()).contains("Hello Alice!");
        assertThat(result.exitCode()).isEqualTo(0);
    }

    @Test
    void testMultipleLaunches(QuarkusMainLauncher launcher) {
        LaunchResult first = launcher.launch("--name=Alice");
        assertThat(first.getOutput()).contains("Hello Alice!");

        LaunchResult second = launcher.launch("--name=Bob");
        assertThat(second.getOutput()).contains("Hello Bob!");
    }
}
```

### Testing console mode commands (REPL)

Console mode commands run in an interactive shell. Use `AeshLauncher` to send commands and assert on their output:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-aesh</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@QuarkusMainTest
public class ReplTest {

    @Test
    void testInteractiveSession(AeshLauncher launcher) {
        launcher.execute("greet --name=Alice");
        assertThat(launcher.getCommandOutput()).isEqualTo("Hello Alice!\n");

        launcher.execute("calc -a 5 -b 3");
        assertThat(launcher.getCommandOutput()).isEqualTo("Result: 8\n");
    }
}
```

The REPL is auto-launched on the first `execute()` call and auto-closed after each test method. No explicit `launch()` or `exit()` calls are needed for typical tests.

#### Clean command output

`getCommandOutput()` returns only what the command wrote via `invocation.println()` -- no prompt, no command echo, no ANSI sequences. This enables exact assertions with `isEqualTo()` instead of fuzzy `contains()` matching.

`getOutput()` accumulates command output across multiple `execute()` calls within a test. Use `resetOutput()` to clear the accumulator between test phases:

```java
@Test
void testAccumulatedOutput(AeshLauncher launcher) {
    launcher.execute("add-item apple");
    launcher.execute("add-item banana");

    assertThat(launcher.getOutput()).isEqualTo("Added: apple\nAdded: banana\n");

    launcher.resetOutput();
    launcher.execute("list-items");
    assertThat(launcher.getCommandOutput()).contains("apple", "banana");
}
```

#### Exit code assertions

`execute()` asserts that the command returns `CommandResult.SUCCESS` (exit code 0) by default. If a command is expected to fail, pass the expected `CommandResult`:

```java
@Test
void testFailingCommand(AeshLauncher launcher) {
    // Passes — expected to fail
    launcher.execute("invalid-command --bad", CommandResult.FAILURE);
    assertThat(launcher.getLastExitCode()).isEqualTo(1);
    assertThat(launcher.getLastError()).isNotNull();
}
```

For more control (custom timeout, pre-canned input), use `ExecuteOptions`:

```java
// Custom timeout
launcher.execute("slow-command", ExecuteOptions.defaults().timeout(Duration.ofMinutes(2)));

// Expected failure with custom timeout
launcher.execute("risky-command",
        ExecuteOptions.expecting(CommandResult.FAILURE).timeout(Duration.ofSeconds(5)));
```

#### Error capture

When a command throws an exception, it is captured and available via `getLastError()`:

```java
@Test
void testErrorCapture(AeshLauncher launcher) {
    launcher.execute("fail --message=boom", CommandResult.FAILURE);
    assertThat(launcher.getLastError().getMessage()).contains("boom");
    assertThat(launcher.getErrorOutput()).contains("boom");
}
```

Errors are cleared between commands -- a successful command after a failing one resets `getLastError()` to `null`.

#### Quoting arguments

Arguments containing spaces or special characters should be quoted the same way as in a terminal:

```java
launcher.execute("echo --text 'hello world'");
assertThat(launcher.getCommandOutput()).isEqualTo("hello world\n");
```

#### Interactive input

For commands that prompt for input during execution (e.g., confirmation prompts), use `ExecuteOptions.input()` to provide pre-canned responses:

```java
// Command calls invocation.inputLine() — response is provided automatically
launcher.execute("confirm --action=deploy", ExecuteOptions.defaults().input("y"));
assertThat(launcher.getCommandOutput()).contains("Confirmed: deploy");

// Multiple prompts
launcher.execute("setup", ExecuteOptions.defaults().input("admin", "password123", "y"));
```

For more complex interactive scenarios, `sendInput()` provides low-level access to the REPL's stdin:

#### Pipeline commands

Aesh supports Unix-style pipe operators (`cmd1 | cmd2`). When testing pipeline commands, `getStageResults()` returns per-stage outcomes so you can inspect each stage independently -- including upstream failures that Unix semantics would otherwise hide (only the last stage's exit code is returned).

```java
@Test
void testPipelineStages(AeshLauncher launcher) {
    launcher.execute("echo --text hello | upper");

    // Terminal stage output
    assertThat(launcher.getCommandOutput()).isEqualTo("HELLO\n");

    // Per-stage results
    List<StageResult> stages = launcher.getStageResults();
    assertThat(stages).hasSize(2);

    assertThat(stages.get(0).commandName()).isEqualTo("EchoCommand");
    assertThat(stages.get(0).isSuccess()).isTrue();

    assertThat(stages.get(1).commandName()).isEqualTo("UpperCommand");
    assertThat(stages.get(1).isSuccess()).isTrue();
}
```

For non-pipeline (single) commands, `getStageResults()` returns an empty list.

Upstream failures are visible even when the terminal stage succeeds:

```java
@Test
void testUpstreamFailure(AeshLauncher launcher) {
    // failpipe throws after writing partial output — terminal still succeeds
    launcher.execute("failpipe | upper");

    StageResult upstream = launcher.getStageResults().get(0);
    assertThat(upstream.isSuccess()).isFalse();
    assertThat(upstream.errorMessage()).contains("upstream boom");

    StageResult terminal = launcher.getStageResults().get(1);
    assertThat(terminal.isSuccess()).isTrue();
}
```

Each `StageResult` carries:

- `commandName()` -- the command's simple class name
- `stageIndex()` / `stageCount()` -- position in the pipeline
- `exitCode()` / `isSuccess()` -- the stage's result
- `errorMessage()` / `errorClass()` -- error details if the stage failed
- `durationMs()` -- wall-clock execution time

#### AeshLauncher API summary

- `execute(String command)` -- executes and asserts `SUCCESS`
- `execute(String command, CommandResult expected)` -- executes and asserts the given result
- `execute(String command, ExecuteOptions options)` -- full control (timeout, expected result, input)
- `sendInput(String input)` -- send raw input for interactive commands
- `getCommandOutput()` -- clean output from the last command
- `getOutput()` / `resetOutput()` -- accumulated command output
- `getStageResults()` -- per-stage results for pipeline commands (empty for single commands)
- `getLastExitCode()` -- exit code from the last command
- `getLastError()` / `getErrorOutput()` -- exception from the last failed command
- `isRunning()` / `waitForExit(Duration)` -- REPL lifecycle state
- `getLaunchResult()` -- launch result after REPL exits
- `launch(String... args)` -- explicit launch with initial arguments (optional)
- `exit()` -- explicit shutdown (optional, auto-closed after each test)

> [!NOTE]
> The exit command must be enabled for `AeshLauncher` to shut down cleanly. Set `quarkus.aesh.add-exit-command=true` in `application.properties` (or `src/test/resources/application.properties`).

## Packaging your application

An Aesh command line application is a standard Quarkus application and can be packaged as a JAR or a native executable.

### As a jar

Building an uber-jar is practical if you plan on distributing the JAR directly:

```shell
mvn package -Dquarkus.package.jar.type=uber-jar
java -jar target/myapp-runner.jar --name=World
```

### As a native executable

You can build a [native executable](https://quarkus.io/guides/building-native-image) for faster startup and lower memory usage:

```shell
mvn package -Dnative
./target/myapp-runner --name=World
```

The Aesh extension automatically registers command classes and their option types for reflection in native builds.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-aesh_quarkus-aesh-mode"></a> [`quarkus.aesh.mode`](#quarkus-aesh_quarkus-aesh-mode) config_property_copy_button:+++quarkus.aesh.mode+++[]<br><br>The execution mode for the Aesh extension.<br><br>\* `auto` - Automatically detect the mode based on annotations. Uses runtime mode for single command or group commands, console mode for multiple commands. \* `runtime` - Use AeshRuntimeRunner for single command execution (like picocli). \* `console` - Use AeshConsoleRunner for interactive shell mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_MODE+++[] | `auto`, `runtime`, `console` | `auto` |
| [lock] <a id="quarkus-aesh_quarkus-aesh-start-console"></a> [`quarkus.aesh.start-console`](#quarkus-aesh_quarkus-aesh-start-console) config_property_copy_button:+++quarkus.aesh.start-console+++[]<br><br>Whether to start the local console (stdin/stdout) for interactive CLI access.<br><br>When not set, auto-detected: the local console is started if no remote transport (SSH, WebSocket) is present, and skipped if a remote transport is present.<br><br>Set to `true` to force starting the local console alongside remote transports. Set to `false` to disable the local console even without remote transports (useful when embedding commands in a server application).<br><br>When running in dev mode with the local console enabled, set `quarkus.console.enabled=false` in `application.properties` to prevent the Quarkus dev console from competing for stdin.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_START_CONSOLE+++[] | boolean |  |
| <a id="quarkus-aesh_quarkus-aesh-top-command"></a> [`quarkus.aesh.top-command`](#quarkus-aesh_quarkus-aesh-top-command) config_property_copy_button:+++quarkus.aesh.top-command+++[]<br><br>Fully qualified class name of the top command to use in runtime mode. or FQCN of class which will be used as entry point for Aesh CommandRunner instance. This class needs to be annotated with `org.aesh.command.Command`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_TOP_COMMAND+++[] | string |  |
| <a id="quarkus-aesh_quarkus-aesh-prompt"></a> [`quarkus.aesh.prompt`](#quarkus-aesh_quarkus-aesh-prompt) config_property_copy_button:+++quarkus.aesh.prompt+++[]<br><br>The prompt to display in console mode. Only used when mode is set to 'console' or auto-detected as console.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_PROMPT+++[] | string | `[quarkus]$ ` |
| <a id="quarkus-aesh_quarkus-aesh-add-exit-command"></a> [`quarkus.aesh.add-exit-command`](#quarkus-aesh_quarkus-aesh-add-exit-command) config_property_copy_button:+++quarkus.aesh.add-exit-command+++[]<br><br>Whether to add a built-in 'exit' command in console mode. Only used when mode is set to 'console' or auto-detected as console.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_ADD_EXIT_COMMAND+++[] | boolean | `true` |
| <a id="quarkus-aesh_quarkus-aesh-enable-alias"></a> [`quarkus.aesh.enable-alias`](#quarkus-aesh_quarkus-aesh-enable-alias) config_property_copy_button:+++quarkus.aesh.enable-alias+++[]<br><br>Enable command aliasing. When enabled, users can create aliases for commands.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_ENABLE_ALIAS+++[] | boolean | `false` |
| <a id="quarkus-aesh_quarkus-aesh-enable-export"></a> [`quarkus.aesh.enable-export`](#quarkus-aesh_quarkus-aesh-enable-export) config_property_copy_button:+++quarkus.aesh.enable-export+++[]<br><br>Enable the export command. When enabled, the 'export' command is available for setting environment variables.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_ENABLE_EXPORT+++[] | boolean | `false` |
| <a id="quarkus-aesh_quarkus-aesh-enable-man"></a> [`quarkus.aesh.enable-man`](#quarkus-aesh_quarkus-aesh-enable-man) config_property_copy_button:+++quarkus.aesh.enable-man+++[]<br><br>Enable man page support. When enabled, the 'man' command is available for viewing command documentation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_ENABLE_MAN+++[] | boolean | `false` |
| <a id="quarkus-aesh_quarkus-aesh-persist-history"></a> [`quarkus.aesh.persist-history`](#quarkus-aesh_quarkus-aesh-persist-history) config_property_copy_button:+++quarkus.aesh.persist-history+++[]<br><br>Enable command history persistence. When enabled, command history is saved to a file between sessions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_PERSIST_HISTORY+++[] | boolean | `false` |
| <a id="quarkus-aesh_quarkus-aesh-history-file"></a> [`quarkus.aesh.history-file`](#quarkus-aesh_quarkus-aesh-history-file) config_property_copy_button:+++quarkus.aesh.history-file+++[]<br><br>Path to the history file. Only used when persistHistory is enabled. If not specified, defaults to ".aesh_history" in the user's home directory.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_HISTORY_FILE+++[] | string |  |
| <a id="quarkus-aesh_quarkus-aesh-history-size"></a> [`quarkus.aesh.history-size`](#quarkus-aesh_quarkus-aesh-history-size) config_property_copy_button:+++quarkus.aesh.history-size+++[]<br><br>Maximum number of history entries to keep.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_HISTORY_SIZE+++[] | int | `500` |
| <a id="quarkus-aesh_quarkus-aesh-logging"></a> [`quarkus.aesh.logging`](#quarkus-aesh_quarkus-aesh-logging) config_property_copy_button:+++quarkus.aesh.logging+++[]<br><br>Enable logging of aesh internal operations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_LOGGING+++[] | boolean | `false` |
| <a id="quarkus-aesh_quarkus-aesh-sub-command-mode-enabled"></a> [`quarkus.aesh.sub-command-mode.enabled`](#quarkus-aesh_quarkus-aesh-sub-command-mode-enabled) config_property_copy_button:+++quarkus.aesh.sub-command-mode.enabled+++[]<br><br>Enable sub-command mode. When enabled, typing a group command without a subcommand enters an interactive context.<br><br>Example:<br><br>```<br>[quarkus]$ module --name mymodule<br>Entering module mode.<br>module[mymodule]> tag add v1.0<br>module[mymodule]> exit<br>[quarkus]$<br>```<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SUB_COMMAND_MODE_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-aesh_quarkus-aesh-sub-command-mode-exit-command"></a> [`quarkus.aesh.sub-command-mode.exit-command`](#quarkus-aesh_quarkus-aesh-sub-command-mode-exit-command) config_property_copy_button:+++quarkus.aesh.sub-command-mode.exit-command+++[]<br><br>Command to exit sub-command mode and return to the parent context.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SUB_COMMAND_MODE_EXIT_COMMAND+++[] | string | `exit` |
| <a id="quarkus-aesh_quarkus-aesh-sub-command-mode-alternative-exit-command"></a> [`quarkus.aesh.sub-command-mode.alternative-exit-command`](#quarkus-aesh_quarkus-aesh-sub-command-mode-alternative-exit-command) config_property_copy_button:+++quarkus.aesh.sub-command-mode.alternative-exit-command+++[]<br><br>Alternative command to exit sub-command mode (e.g., ".."). Set to empty string to disable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SUB_COMMAND_MODE_ALTERNATIVE_EXIT_COMMAND+++[] | string | `..` |
| <a id="quarkus-aesh_quarkus-aesh-sub-command-mode-context-separator"></a> [`quarkus.aesh.sub-command-mode.context-separator`](#quarkus-aesh_quarkus-aesh-sub-command-mode-context-separator) config_property_copy_button:+++quarkus.aesh.sub-command-mode.context-separator+++[]<br><br>Separator used for nested context paths in the prompt. For example, with separator ":", nested contexts appear as "module:project>"<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SUB_COMMAND_MODE_CONTEXT_SEPARATOR+++[] | string | `:` |
| <a id="quarkus-aesh_quarkus-aesh-sub-command-mode-show-context-on-entry"></a> [`quarkus.aesh.sub-command-mode.show-context-on-entry`](#quarkus-aesh_quarkus-aesh-sub-command-mode-show-context-on-entry) config_property_copy_button:+++quarkus.aesh.sub-command-mode.show-context-on-entry+++[]<br><br>Show option/argument values when entering sub-command mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SUB_COMMAND_MODE_SHOW_CONTEXT_ON_ENTRY+++[] | boolean | `true` |
| <a id="quarkus-aesh_quarkus-aesh-sub-command-mode-show-argument-in-prompt"></a> [`quarkus.aesh.sub-command-mode.show-argument-in-prompt`](#quarkus-aesh_quarkus-aesh-sub-command-mode-show-argument-in-prompt) config_property_copy_button:+++quarkus.aesh.sub-command-mode.show-argument-in-prompt+++[]<br><br>Show the primary argument value in the prompt. For example, "module++\[++myapp++\]++>" vs "module>"<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SUB_COMMAND_MODE_SHOW_ARGUMENT_IN_PROMPT+++[] | boolean | `true` |

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-enabled"></a> [`quarkus.aesh.websocket.enabled`](#quarkus-aesh-websocket_quarkus-aesh-websocket-enabled) config_property_copy_button:+++quarkus.aesh.websocket.enabled+++[]<br><br>Whether the WebSocket terminal endpoint is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-roles-allowed"></a> [`quarkus.aesh.websocket.roles-allowed`](#quarkus-aesh-websocket_quarkus-aesh-websocket-roles-allowed) config_property_copy_button:+++quarkus.aesh.websocket.roles-allowed+++[]<br><br>Comma-separated list of roles required to access the WebSocket terminal. Requires a Quarkus Security extension (e.g. quarkus-elytron-security-properties-file).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_ROLES_ALLOWED+++[] | list of string |  |
| [lock] <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-authenticated"></a> [`quarkus.aesh.websocket.authenticated`](#quarkus-aesh-websocket_quarkus-aesh-websocket-authenticated) config_property_copy_button:+++quarkus.aesh.websocket.authenticated+++[]<br><br>Whether the WebSocket terminal requires an authenticated user (any role). Ignored if roles-allowed is set. Requires a Quarkus Security extension.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_AUTHENTICATED+++[] | boolean | `false` |
| [lock] <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-path"></a> [`quarkus.aesh.websocket.path`](#quarkus-aesh-websocket_quarkus-aesh-websocket-path) config_property_copy_button:+++quarkus.aesh.websocket.path+++[]<br><br>The path for the WebSocket terminal endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_PATH+++[] | string | `/aesh/terminal` |
| [lock] <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-health-enabled"></a> [`quarkus.aesh.websocket.health.enabled`](#quarkus-aesh-websocket_quarkus-aesh-websocket-health-enabled) config_property_copy_button:+++quarkus.aesh.websocket.health.enabled+++[]<br><br>Whether the health check is published when the smallrye-health extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_HEALTH_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-max-connections"></a> [`quarkus.aesh.websocket.max-connections`](#quarkus-aesh-websocket_quarkus-aesh-websocket-max-connections) config_property_copy_button:+++quarkus.aesh.websocket.max-connections+++[]<br><br>Maximum number of concurrent WebSocket terminal sessions. Zero means no limit.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_MAX_CONNECTIONS+++[] | int | `0` |
| <a id="quarkus-aesh-websocket_quarkus-aesh-websocket-idle-timeout"></a> [`quarkus.aesh.websocket.idle-timeout`](#quarkus-aesh-websocket_quarkus-aesh-websocket-idle-timeout) config_property_copy_button:+++quarkus.aesh.websocket.idle-timeout+++[]<br><br>Idle timeout for WebSocket sessions. If a session has no input activity for this duration, it will be closed. If not set, sessions can remain idle indefinitely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_WEBSOCKET_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-aesh-websocket_quarkus-aesh) |  |

<a id="duration-note-anchor-quarkus-aesh-websocket_quarkus-aesh"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-enabled"></a> [`quarkus.aesh.ssh.enabled`](#quarkus-aesh-ssh_quarkus-aesh-ssh-enabled) config_property_copy_button:+++quarkus.aesh.ssh.enabled+++[]<br><br>Whether the SSH terminal server is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-health-enabled"></a> [`quarkus.aesh.ssh.health.enabled`](#quarkus-aesh-ssh_quarkus-aesh-ssh-health-enabled) config_property_copy_button:+++quarkus.aesh.ssh.health.enabled+++[]<br><br>Whether the health check is published when the smallrye-health extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_HEALTH_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-port"></a> [`quarkus.aesh.ssh.port`](#quarkus-aesh-ssh_quarkus-aesh-ssh-port) config_property_copy_button:+++quarkus.aesh.ssh.port+++[]<br><br>SSH server port.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_PORT+++[] | int | `2222` |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-host"></a> [`quarkus.aesh.ssh.host`](#quarkus-aesh-ssh_quarkus-aesh-ssh-host) config_property_copy_button:+++quarkus.aesh.ssh.host+++[]<br><br>SSH server bind address.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_HOST+++[] | string | `localhost` |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-host-key-file"></a> [`quarkus.aesh.ssh.host-key-file`](#quarkus-aesh-ssh_quarkus-aesh-ssh-host-key-file) config_property_copy_button:+++quarkus.aesh.ssh.host-key-file+++[]<br><br>Path to the host key file. If the file does not exist, a new RSA key pair will be generated and saved to this path.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_HOST_KEY_FILE+++[] | string | `hostkey.ser` |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-password"></a> [`quarkus.aesh.ssh.password`](#quarkus-aesh-ssh_quarkus-aesh-ssh-password) config_property_copy_button:+++quarkus.aesh.ssh.password+++[]<br><br>Password for SSH authentication. If not set, any password is accepted (suitable for development only).<br><br>For production, use public key authentication via `authorized-keys-file` instead. If password authentication is needed, avoid storing the password in plaintext in `application.properties`. Instead, reference an environment variable:<br><br>```<br>quarkus.aesh.ssh.password=${SSH_PASSWORD}<br>```<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_PASSWORD+++[] | string |  |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-authorized-keys-file"></a> [`quarkus.aesh.ssh.authorized-keys-file`](#quarkus-aesh-ssh_quarkus-aesh-ssh-authorized-keys-file) config_property_copy_button:+++quarkus.aesh.ssh.authorized-keys-file+++[]<br><br>Path to an OpenSSH-format authorized_keys file for public key authentication. This is the recommended authentication method for production environments. When set, clients can authenticate with a private key whose public key is listed in this file. Can be used alongside password authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_AUTHORIZED_KEYS_FILE+++[] | string |  |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-max-connections"></a> [`quarkus.aesh.ssh.max-connections`](#quarkus-aesh-ssh_quarkus-aesh-ssh-max-connections) config_property_copy_button:+++quarkus.aesh.ssh.max-connections+++[]<br><br>Maximum number of concurrent SSH sessions. Zero means no limit.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_MAX_CONNECTIONS+++[] | int | `0` |
| <a id="quarkus-aesh-ssh_quarkus-aesh-ssh-idle-timeout"></a> [`quarkus.aesh.ssh.idle-timeout`](#quarkus-aesh-ssh_quarkus-aesh-ssh-idle-timeout) config_property_copy_button:+++quarkus.aesh.ssh.idle-timeout+++[]<br><br>Idle timeout for SSH sessions. If a session has no input activity for this duration, it will be closed. If not set, sessions can remain idle indefinitely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AESH_SSH_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-aesh-ssh_quarkus-aesh) |  |

<a id="duration-note-anchor-quarkus-aesh-ssh_quarkus-aesh"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.
