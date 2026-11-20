# Command mode with Picocli

[Picocli](https://picocli.info/) is an open source tool for creating rich command line applications.

Quarkus provides support for using Picocli. This guide contains examples of `picocli` extension usage.

> [!IMPORTANT]
> If you are not familiar with the Quarkus Command Mode, consider reading the [Command Mode reference guide](/guides/command-mode-reference/index.md) first.

## Extension

Once you have your Quarkus project configured you can add the `picocli` extension to your project by running the following command in your project base directory.

**CLI**

```bash
quarkus extension add picocli
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='picocli'
```

**Gradle**

```bash
./gradlew addExtension --extensions='picocli'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-picocli</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-picocli")
```

## Building a command line application

### Simple application

A simple Picocli application with only one `Command` can be created as follows:

```java
package com.acme.picocli;

import picocli.CommandLine;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@CommandLine.Command // <1>
public class HelloCommand implements Runnable {

    @CommandLine.Option(names = {"-n", "--name"}, description = "Who will we greet?", defaultValue = "World")
    String name;

    private final GreetingService greetingService;

    public HelloCommand(GreetingService greetingService) { // <2>
        this.greetingService = greetingService;
    }

    @Override
    public void run() {
        greetingService.sayHello(name);
    }
}

@Dependent
class GreetingService {
    void sayHello(String name) {
        System.out.println("Hello " + name + "!");
    }
}
```

1. If there is only one class annotated with `picocli.CommandLine.Command`, it will be used automatically as the entry point of the command line application.
2. All classes annotated with `picocli.CommandLine.Command` are registered as CDI beans.

> [!IMPORTANT]
> Beans annotated with `@CommandLine.Command` should not use proxied scopes (e.g. do not use `@ApplicationScoped`) because Picocli will not be able to set field values in such beans. By default, this Picocli extension registers classes annotated with `@CommandLine.Command` with the `@Dependent` scope. If you need to use a proxied scope, then annotate the setters and not the fields, for example:

```java
@CommandLine.Command
@ApplicationScoped
public class EntryCommand {
    private String name;

    @CommandLine.Option(names = "-n")
    public void setName(String name) {
        this.name = name;
    }
}
```

### Command line application with multiple Commands

When multiple classes have the `picocli.CommandLine.Command` annotation, then one of them needs to be also annotated with `io.quarkus.picocli.runtime.annotations.TopCommand`. This can be overwritten with the `quarkus.picocli.top-command` property.

```java
package com.acme.picocli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(mixinStandardHelpOptions = true, subcommands = {HelloCommand.class, GoodByeCommand.class})
public class EntryCommand {
}

@CommandLine.Command(name = "hello", description = "Greet World!")
class HelloCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Hello World!");
    }
}

@CommandLine.Command(name = "goodbye", description = "Say goodbye to World!")
class GoodByeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Goodbye World!");
    }
}
```

### Customizing Picocli CommandLine instance

You can customize CommandLine classes used by the `picocli` extension by producing your own bean instance:

```java
package com.acme.picocli;

import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@TopCommand
@CommandLine.Command
public class EntryCommand implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        System.out.println("My name is: " + spec.name());
    }
}

@ApplicationScoped
class CustomConfiguration {

    @Produces
    CommandLine customCommandLine(PicocliCommandLineFactory factory) { // <1>
        return factory.create().setCommandName("CustomizedName");
    }
}
```

1. `PicocliCommandLineFactory` will create an instance of CommandLine with `TopCommand` and `CommandLine.IFactory` injected.

### Different entry command for each profile

It is possible to create different entry command for each profile, using `@IfBuildProfile`:

```java
@ApplicationScoped
public class Config {

    @Produces
    @TopCommand
    @IfBuildProfile("dev")
    public Object devCommand() {
        return DevCommand.class; // <1>
    }

    @Produces
    @TopCommand
    @IfBuildProfile("prod")
    public Object prodCommand() {
        return new ProdCommand("Configured by me!");
    }

}
```

1. You can return instance of `java.lang.Class` here. In such case `CommandLine` will try to instantiate this class using `CommandLine.IFactory`.

### Configure CDI Beans with parsed arguments

You can use `Event<CommandLine.ParseResult>` or just `CommandLine.ParseResult` to configure CDI beans based on arguments parsed by Picocli. This event will be generated in `QuarkusApplication` class created by this extension. If you are providing your own `@QuarkusMain` this event will not be raised. `CommandLine.ParseResult` is created from default `CommandLine` bean.

```java
@CommandLine.Command
public class EntryCommand implements Runnable {

    @CommandLine.Option(names = "-c", description = "JDBC connection string")
    String connectionString;

    @Inject
    DataSource dataSource;

    @Override
    public void run() {
        try (Connection c = dataSource.getConnection()) {
            // Do something
        } catch (SQLException throwables) {
            // Handle error
        }
    }
}

@ApplicationScoped
class DatasourceConfiguration {

    @Produces
    @ApplicationScoped // <1>
    DataSource dataSource(CommandLine.ParseResult parseResult) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(parseResult.matchedOption("c").getValue().toString());
        return ds;
    }
}
```

1. `@ApplicationScoped` used for lazy initialization

### Providing your own QuarkusMain

You can also provide your own application entry point annotated with `QuarkusMain` (as described in [Command Mode reference guide](/guides/command-mode-reference/index.md)).

```java
package com.acme.picocli;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;

import jakarta.inject.Inject;

@QuarkusMain
@CommandLine.Command(name = "demo", mixinStandardHelpOptions = true)
public class ExampleApp implements Runnable, QuarkusApplication {
    @Inject
    CommandLine.IFactory factory; // <1>

    @Override
    public void run() {
        // business logic
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }
}
```

1. Quarkus-compatible `CommandLine.IFactory` bean created by `picocli` extension.

## Development Mode

In the development mode, i.e. when running `mvn quarkus:dev`, the application is executed and restarted every time the `Space bar` key is pressed. You can also pass arguments to your command line app via the `quarkus.args` system property, e.g. `mvn quarkus:dev -Dquarkus.args='--help'` and `mvn quarkus:dev -Dquarkus.args='-c -w --val 1'`. For Gradle projects, arguments can be passed using `--quarkus-args`.

> [!NOTE]
> If you're creating a typical Quarkus application (e.g., HTTP-based services) that includes command-line functionality, you'll need to handle the application's lifecycle differently. In the `Runnable.run()` method of your command, make sure to use `Quarkus.waitForExit()` or `Quarkus.asyncExit()`. This will prevent the application from shutting down prematurely and ensure a proper shutdown process.

## Packaging your application

A Picocli command line application can be packaged in multiple formats (e.g. a JAR, a native executable) and can be published to various repositories (e.g. Homebrew, Chocolatey, SDKMAN!).

### As a jar

A Picocli command line application is a standard Quarkus application and as such can be published as a JAR in various packaging formats (e.g. fast-jar, uber-jar).

In the context of a command line application, building an uber-jar is more practical if you plan on publishing the JAR as is.

For more information about how to build an uber-jar, see our documentation:

- For [Maven](https://quarkus.io/guides/maven-tooling#uber-jar-maven)
- For [Gradle](https://quarkus.io/guides/gradle-tooling#building-uber-jars)

You can then execute the application by using the standard `java -jar your-application.jar` command.

Using plugins such as the [really-executable-jar-maven-plugin](https://github.com/brianm/really-executable-jars-maven-plugin) can be handy to simplify the execution of your command line application.

### As a native executable

You can also build a [native executable](https://quarkus.io/guides/building-native-image) but keep in mind that native executables are not portable and that you need one binary per supported platform.

### Publishing the application

Publishing your command line application to a repository makes it a lot easier to consume. Various application repositories are available depending on your requirements such as [SDKMAN!](https://sdkman.io/), [Homebrew](https://brew.sh/) for macOS, or [Chocolatey](https://chocolatey.org/) for Windows.

To publish to these repositories, we recommend the usage of [JReleaser](https://jreleaser.org/).

JReleaser adds executable wrappers around your JAR for your application to be easily executable.

### More information

You can also consult the [Picocli official documentation](https://picocli.info/#_packaging_your_application) for more general information about how to package Picocli applications.

## Kubernetes support

Once you have your command line application, you can also generate the resources necessary to install and use this application in Kubernetes by adding the `kubernetes` extension. To install the `kubernetes` extension, run the following command in your project base directory.

**CLI**

```bash
quarkus extension add kubernetes
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='kubernetes'
```

**Gradle**

```bash
./gradlew addExtension --extensions='kubernetes'
```

This will add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-kubernetes</artifactId>
</dependency>
```

And, next, build the application with:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

The Kubernetes extension will detect the presence of the Picocli extension and hence generate a [Job](https://kubernetes.io/docs/concepts/workloads/controllers/job/) resource instead of a [Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) resource in the `target/kubernetes/` directory.

> [!IMPORTANT]
> If you don't want to generate a Job resource, you can specify the resource you want to generate using the property `quarkus.kubernetes.deployment-kind`. For example, if you want to generate a Deployment resource, use `quarkus.kubernetes.deployment-kind=Deployment`.

Moreover, you can provide the arguments that will be used by the Kubernetes job via the property `quarkus.kubernetes.arguments`. For example, after adding the property `quarkus.kubernetes.arguments=A,B` and building your project, the following Job resource will be generated:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  labels:
    app.kubernetes.io/name: app
    app.kubernetes.io/version: 0.1-SNAPSHOT
  name: app
spec:
  completionMode: NonIndexed
  suspend: false
  template:
    metadata:
      labels:
        app.kubernetes.io/name: app
        app.kubernetes.io/version: 0.1-SNAPSHOT
    spec:
      containers:
        - args:
            - A
            - B
          env:
            - name: KUBERNETES_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          image: docker.io/user/app:0.1-SNAPSHOT
          imagePullPolicy: Always
          name: app
          ports:
            - containerPort: 8080
              name: http
              protocol: TCP
      restartPolicy: OnFailure
      terminationGracePeriodSeconds: 10

```

Finally, the Kubernetes job will be launched every time it is installed in Kubernetes. You can know more about how to run Kubernetes jobs in this [document](https://kubernetes.io/docs/concepts/workloads/controllers/job/#running-an-example-job).

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-picocli_quarkus-picocli-top-command"></a> [`quarkus.picocli.top-command`](#quarkus-picocli_quarkus-picocli-top-command) config_property_copy_button:+++quarkus.picocli.top-command+++[]<br><br>Name of bean annotated with `io.quarkus.picocli.runtime.annotations.TopCommand` or FQCN of class which will be used as entry point for Picocli CommandLine instance. This class needs to be annotated with `picocli.CommandLine.Command`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PICOCLI_TOP_COMMAND+++[] | string |  |
