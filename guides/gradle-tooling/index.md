# Quarkus and Gradle

Use Gradle to create a new project, add or remove extensions, launch development mode, debug your application, and build your application into a jar, native executable, or container-friendly executable. Import your project into your favorite IDE using Gradle project metadata.

<a id="project-creation"></a>
## Creating a new project

To scaffold a Gradle project you can either use the [Quarkus CLI](/guides/cli-tooling/index.md) or the Quarkus Maven plugin:

**CLI**

```bash
quarkus create app my-groupId:my-artifactId \
    --extensions=rest,rest-jackson \
    --gradle
```

*For more information about how to install the Quarkus CLI and use it, please refer to [the Quarkus CLI guide](/guides/cli-tooling/index.md).*

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=my-groupId \
    -DprojectArtifactId=my-artifactId \
    -Dextensions="rest,rest-jackson" \
    -DbuildTool=gradle
```

> [!NOTE]
> If you just launch `mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create` the Maven plugin asks for user inputs. You can disable this interactive mode (and use default values) by passing `-B` to the Maven command.

> [!TIP]
> If you prefer using the Kotlin DSL, use `gradle-kotlin-dsl` instead of `gradle`.

> [!NOTE]
> Quarkus project scaffolding automatically installs the Gradle wrapper (`./gradlew`) in your project.
>
> If you prefer to use a standalone Gradle installation, please use Gradle 9.7.0.

The project is generated in a directory named after the passed artifactId.

A pair of Dockerfiles for native and JVM modes are also generated in `src/main/docker`. Instructions to build the image and run the container are written in those Dockerfiles.

<a id="custom-test-configuration-profile"></a>
### Custom test configuration profile in JVM mode

By default, Quarkus tests in JVM mode are run using the `test` configuration profile. If you are not familiar with Quarkus configuration profiles, everything you need to know is explained in the [Configuration Profiles Documentation](/guides/config-reference/index.md#profiles).

It is however possible to use a custom configuration profile for your tests with the Gradle build configuration shown below. This can be useful if you need for example to run some tests using a specific database which is not your default testing database.

**Groovy DSL**

```groovy
test {
    systemProperty "quarkus.test.profile", "foo" <1>
}
```

1. The `foo` configuration profile will be used to run the tests.

**Kotlin DSL**

```kotlin
tasks.test {
    systemProperty("quarkus.test.profile", "foo") <1>
}
```

1. The `foo` configuration profile will be used to run the tests.

> [!WARNING]
> It is not possible to use a custom test configuration profile in native mode for now. Native tests are always run using the `prod` profile.

## Dealing with extensions

From inside a Quarkus project, you can obtain a list of the available extensions with:

**CLI**

```bash
quarkus extension
```

**Gradle**

```bash
./gradlew listExtensions
```

You can enable an extension using:

**CLI**

```bash
quarkus extension add hibernate-validator
```

**Gradle**

```bash
./gradlew addExtension --extensions='hibernate-validator'
```

Extensions are passed using a comma-separated list.

The extension name is the GAV name of the extension: e.g. `io.quarkus:quarkus-agroal`. But you can pass a partial name and Quarkus will do its best to find the right extension. For example, `agroal`, `Agroal` or `agro`  will expand to `io.quarkus:quarkus-agroal`. If no extension is found or if more than one extensions match, you will see a red check mark  ❌ in the command result.

```shell
$ ./gradlew addExtension --extensions="jdbc,agroal,non-exist-ent"
[...]
❌ Multiple extensions matching 'jdbc'
     * io.quarkus:quarkus-jdbc-h2
     * io.quarkus:quarkus-jdbc-mariadb
     * io.quarkus:quarkus-jdbc-postgresql
     Be more specific e.g using the exact name or the full gav.
✅ Adding extension io.quarkus:quarkus-agroal
❌ Cannot find a dependency matching 'non-exist-ent', maybe a typo?
[...]
```

You can install all extensions which match a globbing pattern:

**CLI**

```bash
quarkus extension add smallrye-*
```

**Gradle**

```bash
./gradlew addExtension --extensions='smallrye-*'
```

<a id="dev-mode"></a>
## Development mode

Quarkus comes with a built-in development mode. You can start it with:

**CLI**

```bash
quarkus dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

Note that if you run it this way the continuous testing experience will not be as nice, as gradle runs as a daemon Quarkus can't draw the 'pretty' test output so falls back to just logging the output.

You can then update the application sources, resources and configurations. The changes are automatically reflected in your running application. This is great to do development spanning UI and database as you see changes reflected immediately.

`quarkusDev` enables hot deployment with background compilation, which means that when you modify your Java files or your resource files and refresh your browser these changes will automatically take effect. This works too for resource files like the configuration property file. The act of refreshing the browser triggers a scan of the workspace, and if any changes are detected the Java files are compiled, and the application is redeployed, then your request is serviced by the redeployed application. If there are any issues with compilation or deployment an error page will let you know.

Hit `CTRL+C` to stop the application.

You can change the working directory the development environment runs on:

**Groovy DSL**

```groovy
quarkusDev {
    workingDirectory = rootProject.layout.projectDirectory.asFile
}
```

**Kotlin DSL**

```kotlin
tasks.quarkusDev {
    workingDirectory = rootProject.layout.projectDirectory.asFile
}
```

> [!TIP]
> By default, the `quarkusDev` task uses `compileJava` compiler options. These can be overridden by setting the `compilerArgs` property in the task.

> [!NOTE]
> By default, `quarkusDev` sets the debug host to `localhost` (for security reasons). If you need to change this, for example to enable debugging on all hosts, you can use the `-DdebugHost` option like so:
>
> **CLI**
>
> ```bash
> quarkus dev -DdebugHost=0.0.0.0
> ```
>
> **Gradle**
>
> ```bash
> ./gradlew --console=plain quarkusDev -DdebugHost=0.0.0.0
> ```

You also can add environment variables to the development environment:

**Groovy DSL**

```groovy
quarkusDev {
    environmentVariables = [FOO_VALUE: 'abc', BAR_VALUE: 'def']
}
```

**Kotlin DSL**

```kotlin
tasks.quarkusDev {
    environmentVariables.set(mapOf("FOO_VALUE" to "abc", "BAR_VALUE" to "def"))
}
```

The plugin also exposes a `quarkusDev` configuration. Using this configuration to declare a dependency will restrict the usage of that dependency to development mode. The `quarkusDev` configuration can be used as following:

**Groovy DSL**

```groovy
dependencies {
    quarkusDev 'io.quarkus:quarkus-jdbc-h2'
}
```

**Kotlin DSL**

```kotlin
dependencies {
    quarkusDev("io.quarkus:quarkus-jdbc-h2")
}
```

### Remote Development Mode

It is possible to use development mode remotely, so that you can run Quarkus in a container environment (such as OpenShift) and have changes made to your local files become immediately visible.

This allows you to develop in the same environment you will actually run your app in, and with access to the same services.

> [!WARNING]
> Do not use this in production. This should only be used in a development environment. You should not run production applications in dev mode.

To do this you must build a mutable application, using the `mutable-jar` format. Set the following properties in `application.properties`:

```properties
quarkus.package.jar.type=mutable-jar <1>
quarkus.live-reload.password=changeit <2>
quarkus.live-reload.url=http://my.cluster.host.com:8080 <3>
```

1. This tells Quarkus to use the mutable-jar format. Mutable applications also include the deployment time parts of Quarkus, so they take up a bit more disk space. If run normally they start just as fast and use the same memory as an immutable application, however they can also be started in dev mode.
2. The password that is used to secure communication between the remote side and the local side.
3. The URL that your app is going to be running in dev mode at. This is only needed on the local side, so you may want to leave it out of the properties file and specify it as a system property on the command line.

The `mutable-jar` is then built in the same way that a regular Quarkus jar is built, i.e. by issuing:

**CLI**

```bash
quarkus build
```

**Gradle**

```bash
./gradlew build
```

Before you start Quarkus on the remote host set the environment variable `QUARKUS_LAUNCH_DEVMODE=true`. If you are on bare metal you can set it via the `export QUARKUS_LAUNCH_DEVMODE=true` command and then run the application with the proper `java -jar ...` command to run the application.

If you plan on running the application via Docker, then you'll need to add `-e QUARKUS_LAUNCH_DEVMODE=true` to the `docker run` command. When the application starts you should now see the following line in the logs: `Profile dev activated. Live Coding activated`. You will also need to give the application the rights to update the deployment resources by adding `RUN chmod o+rw -R /deployments` after the `COPY` commands into your Dockerfile. For security reasons, this option should not be added to the production Dockerfile.

> [!NOTE]
> The remote side does not need to include Maven or any other development tools. The normal `fast-jar` Dockerfile that is generated with a new Quarkus application is all you need. If you are using bare metal launch the Quarkus runner jar, do not attempt to run normal dev mode.

Now you need to connect your local agent to the remote host, using the `remote-dev` command:

```bash
./gradlew quarkusRemoteDev -Dquarkus.live-reload.url=http://my-remote-host:8080
```

Now every time you refresh the browser you should see any changes you have made locally immediately visible in the remote app.

All the config options are shown below:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-core_quarkus-live-reload-enabled"></a> [`quarkus.live-reload.enabled`](#quarkus-core_quarkus-live-reload-enabled) config_property_copy_button:+++quarkus.live-reload.enabled+++[]<br><br>Whether the live-reload feature should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-live-reload-instrumentation"></a> [`quarkus.live-reload.instrumentation`](#quarkus-core_quarkus-live-reload-instrumentation) config_property_copy_button:+++quarkus.live-reload.instrumentation+++[]<br><br>Whether Quarkus should enable its ability to not do a full restart when changes to classes are compatible with JVM instrumentation.<br><br>If this is set to true, Quarkus will perform class redefinition when possible.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_INSTRUMENTATION+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-live-reload-watched-resources"></a> [`quarkus.live-reload.watched-resources`](#quarkus-core_quarkus-live-reload-watched-resources) config_property_copy_button:+++quarkus.live-reload.watched-resources+++[]<br><br>The names of additional resource files to watch for changes, triggering a reload on change. Directories are *not* supported.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_WATCHED_RESOURCES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-live-reload-password"></a> [`quarkus.live-reload.password`](#quarkus-core_quarkus-live-reload-password) config_property_copy_button:+++quarkus.live-reload.password+++[]<br><br>Password used to use to connect to the remote dev-mode application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_PASSWORD+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-live-reload-url"></a> [`quarkus.live-reload.url`](#quarkus-core_quarkus-live-reload-url) config_property_copy_button:+++quarkus.live-reload.url+++[]<br><br>URL used to use to connect to the remote dev-mode application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_URL+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-live-reload-connect-timeout"></a> [`quarkus.live-reload.connect-timeout`](#quarkus-core_quarkus-live-reload-connect-timeout) config_property_copy_button:+++quarkus.live-reload.connect-timeout+++[]<br><br>The amount of time to wait for a remote dev connect or reconnect<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-core_quarkus-live-reload) | `30S` |
| [lock] <a id="quarkus-core_quarkus-live-reload-retry-interval"></a> [`quarkus.live-reload.retry-interval`](#quarkus-core_quarkus-live-reload-retry-interval) config_property_copy_button:+++quarkus.live-reload.retry-interval+++[]<br><br>The amount of time to wait between attempts when connecting to the server side of remote dev<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_RETRY_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-core_quarkus-live-reload) | `2S` |
| [lock] <a id="quarkus-core_quarkus-live-reload-retry-max-attempts"></a> [`quarkus.live-reload.retry-max-attempts`](#quarkus-core_quarkus-live-reload-retry-max-attempts) config_property_copy_button:+++quarkus.live-reload.retry-max-attempts+++[]<br><br>The maximum number of attempts when connecting to the server side of remote dev<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIVE_RELOAD_RETRY_MAX_ATTEMPTS+++[] | int | `10` |

<a id="duration-note-anchor-quarkus-core_quarkus-live-reload"></a>

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

### Extension provided Dev mode Java options

Some extensions may provide pre-configured Java options that should be added to the command line launching an application in Dev mode.

Let's suppose there are couple of extensions `quarkus-blue` and `quarkus-red` in an application that provide Java options for Dev mode. The logs may look something like this

```bash
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] Adding JVM options from org.acme:quarkus-blue::jar
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   enable-native-access: [ALL-UNNAMED]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   add-modules: [jdk.incubator.vector]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   enable-preview: []
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] Adding JVM options from org.acme:quarkus-red::jar
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   add-opens: [java.base/java.io=ALL-UNNAMED, java.base/java.nio=ALL-UNNAMED]
[INFO] [org.gradle.process.internal.DefaultExecHandle] Starting process 'command '/home/<username>/jdk/bin/java''. Working directory: /home/<username>/gradle-app/build/classes/java/main Command: /home/<username>/jdk/bin/java -Dquarkus.console.basic=true -Dio.quarkus.force-color-support=true -javaagent:/home/<username>/.m2/repository/io/quarkus/quarkus-class-change-agent/{quarkus-version}/quarkus-class-change-agent-{quarkus-version}.jar -Dquarkus-internal.serialized-app-model.path=/home/<username>/gradle-app/build/tmp/quarkusDev/quarkus-app-model.dat -Dquarkus-internal-test.serialized-app-model.path=/home/<username>/gradle-app/build/tmp/quarkusDev/quarkus-app-test-model.dat -XX:TieredStopAtLevel=1 -agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=y,suspend=n --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --enable-native-access=ALL-UNNAMED --add-modules=jdk.incubator.vector --enable-preview -Djava.util.logging.manager=org.jboss.logmanager.LogManager -jar /home/<username>/gradle-app/build/gradle-app-dev.jar
```

A user may choose to disable all the Java options provided by extensions by configuring `disableAll` parameter such as

**Groovy DSL**

```groovy
quarkusDev {
    extensionJvmOptions{
        disableAll = true
    }
}
```

**Kotlin DSL**

```kotlin
tasks.quarkusDev {
    extensionJvmOptions{
        setDisableAll(true)
    }
}
```

Or disable Java options provided by specific extensions by configuring Maven coordinates patterns, such as

**Groovy DSL**

```groovy
quarkusDev {
    extensionJvmOptions{
        disableFor = ["org.acme:quarkus-red"]
    }
}
```

**Kotlin DSL**

```kotlin
tasks.quarkusDev {
    extensionJvmOptions{
        setDisableFor(mutableListOf("org.acme:quarkus-red"))
    }
}
```

With this configuration the logs will look like

```bash
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] Adding JVM options from org.acme:quarkus-blue::jar
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   enable-native-access: [ALL-UNNAMED]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   add-modules: [jdk.incubator.vector]
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder]   enable-preview: []
[DEBUG] [io.quarkus.deployment.dev.DevModeCommandLineBuilder] Skipped JVM options from org.acme:quarkus-red::jar
[INFO] [org.gradle.process.internal.DefaultExecHandle] Starting process 'command '/home/<username>/jdk/bin/java''. Working directory: /home/<username>/gradle-app/build/classes/java/main Command: /home/<username>/jdk/bin/java -Dquarkus.console.basic=true -Dio.quarkus.force-color-support=true -javaagent:/home/<username>/.m2/repository/io/quarkus/quarkus-class-change-agent/{quarkus-version}/quarkus-class-change-agent-{quarkus-version}.jar -Dquarkus-internal.serialized-app-model.path=/home/<username>/gradle-app/build/tmp/quarkusDev/quarkus-app-model.dat -Dquarkus-internal-test.serialized-app-model.path=/home/<username>/gradle-app/build/tmp/quarkusDev/quarkus-app-test-model.dat -XX:TieredStopAtLevel=1 -agentlib:jdwp=transport=dt_socket,address=localhost:5005,server=y,suspend=n --enable-native-access=ALL-UNNAMED --add-modules=jdk.incubator.vector --enable-preview -Djava.util.logging.manager=org.jboss.logmanager.LogManager -jar /home/<username>/gradle-app/build/gradle-app-kotlin-dev.jar
```

## Debugging

In development mode, Quarkus starts by default with debug mode enabled, listening to port `5005` without suspending the JVM.

This behavior can be changed by giving the `debug` system property one of the following values:

- `false` - the JVM will start with debug mode disabled
- `true` - The JVM is started in debug mode and will be listening on port `5005`
- `client` - the JVM will start in client mode and attempt to connect to `localhost:5005`
- `{port}` - The JVM is started in debug mode and will be listening on `{port}`

An additional system property `suspend` can be used to suspend the JVM, when launched in debug mode. `suspend` supports the following values:

- `y` or `true` - The debug mode JVM launch is suspended
- `n` or `false` - The debug mode JVM is started without suspending

> [!TIP]
> You can also run a Quarkus application in debug mode with a suspended JVM using:
>
> **CLI**
>
> ```bash
> quarkus dev -Dsuspend -Ddebug
> ```
>
> **Gradle**
>
> ```bash
> ./gradlew --console=plain quarkusDev -Dsuspend -Ddebug
> ```
>
> Then, attach your debugger to `localhost:5005`.

## Import in your IDE

Once you have a [project generated](#project-creation), you can import it in your favorite IDE. The only requirement is the ability to import a Gradle project.

**Eclipse**

In Eclipse, click on: `File -> Import`. In the wizard, select: `Gradle -> Existing Gradle Project`. On the next screen, select the root location of the project. The next screen list the found modules; select the generated project and click on `Finish`. Done!

In a separated terminal, run:

**CLI**

```bash
quarkus dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

and enjoy a highly productive environment.

**IntelliJ IDEA**

In IntelliJ IDEA:

1. From inside IntelliJ IDEA select `File -> New -> Project From Existing Sources...` or, if you are on the welcome dialog, select `Import project`.
2. Select the project root
3. Select `Import project from external model` and `Gradle`
4. Next a few times (review the different options if needed)
5. On the last screen click on Finish

In a separated terminal or in the embedded terminal, run:

**CLI**

```bash
quarkus dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

Enjoy!

**Apache NetBeans**

In NetBeans:

1. Select `File -> Open Project`
2. Select the project root
3. Click on `Open Project`

In a separated terminal or the embedded terminal, go to the project root and run:

**CLI**

```bash
quarkus dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

Enjoy!

**Visual Studio Code**

Open the project directory in VS Code. If you have installed the Java Extension Pack (grouping a set of Java extensions), the project is loaded as a Gradle project.

## Downloading dependencies for offline development and testing

Quarkus extension dependencies are divided into the runtime extension dependencies that end up on the application runtime classpath and the deployment (or build time) extension dependencies that are resolved by Quarkus only at application build time to create the build classpath. Application developers are expected to express dependencies only on the runtime artifacts of Quarkus extensions.

To enable the use-case of building and testing a Quarkus application offline, the plugin includes the `quarkusGoOffline` task that could be called from the command line like this:

```bash
./gradlew quarkusGoOffline
```

This task will resolve all the runtime, build time, test and dev mode dependencies of the application to the Gradle cache. Once executed, you will be able to safely run quarkus task with `--offline` flag.

## Building a native executable

Native executables make Quarkus applications ideal for containers and serverless workloads.

Make sure to have `GRAALVM_HOME` configured and pointing to the latest release of GraalVM for JDK 21.

Create a native executable using:

**CLI**

```bash
quarkus build --native
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

A native executable will be present in `build/`.

Native related properties can either be added in `application.properties` file, as command line arguments or in the `quarkusBuild` task. Configuring the `quarkusBuild` task can be done as following:

**Groovy DSL**

```groovy
quarkusBuild {
    nativeArgs {
        containerBuild = true <1>
        builderImage = "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21" <2>
    }
}
```

1. Set `quarkus.native.container-build` property to `true`
2. Set `quarkus.native.builder-image` property to `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21`

**Kotlin DSL**

```kotlin
tasks.quarkusBuild {
    nativeArgs {
        "container-build" to true <1>
        "builder-image" to "quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21" <2>
    }
}
```

1. Set `quarkus.native.container-build` property to `true`
2. Set `quarkus.native.builder-image` property to `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21`

> [!WARNING]
> When using the Gradle Groovy DSL, property keys must follow lower camel case notation. e.g. `container-build` is not valid, and should be replaced by `containerBuild`. This limitation does not apply to the Gradle Kotlin DSL.

### Build a container friendly executable

The native executable will be specific to your operating system. To create an executable that will run in a container, use the following:

**CLI**

```bash
quarkus build --native --no-tests -Dquarkus.native.container-build=true
# The --no-tests flag is required only on Windows and macOS.
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

The produced executable will be a 64-bit Linux executable, so depending on your operating system it may no longer be runnable. However, it's not an issue as we are going to copy it to a Docker container. Note that in this case the build itself runs in a Docker container too, so you don't need to have GraalVM installed locally.

> [!TIP]
> By default, the native executable will be generated using the `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21` Docker image.
>
> If you want to build a native executable with a different Docker image (for instance to use a different GraalVM version), use the `-Dquarkus.native.builder-image=<image name>` build argument.
>
> The list of the available Docker images can be found on [quay.io](https://quay.io/repository/quarkus/ubi9-quarkus-mandrel-builder-image?tab=tags). Be aware that a given Quarkus version might not be compatible with all the images available.
>
> Note also that starting Quarkus 3.19, the default *builder* images are based on UBI 9. To use the previous UBI 8 based images, you can use the pick an image from the [quay.io repository](https://quay.io/repository/quarkus/ubi-quarkus-mandrel-builder-image?tab=tags).

## Running native tests

Run the native tests using:

```bash
./gradlew testNative
```

This task depends on `quarkusBuild`, so it will generate the native image before running the native tests.

> [!NOTE]
> By default, the `native-test` source set is based on `main` and `test` source sets. It is possible to add an extra source set. For example, if your integration tests are located in an `integrationTest` source set, you can specify it as:
>
> **Groovy DSL**
>
> ```groovy
> quarkus {
>     sourceSets {
>         extraNativeTest = sourceSets.integrationTest
>     }
> }
> ```
>
> **Kotlin DSL**
>
> ```kotlin
> quarkus {
>     sourceSets {
>         setExtraNativeTest(sourceSets["integrationTest"])
>     }
> }
> ```

## Running integration tests

Quarkus integration tests (annotated with `@QuarkusIntegrationTest`) will run on the artifact produced by Quarkus. Those tests can be placed in a `src/integrationTest/java` directory and executed using:

```bash
./gradlew quarkusIntTest
```

This task depends on `quarkusBuild`, so it will generate the artifact before running the integration tests.

## Using fast-jar

`fast-jar` is now the default quarkus package type. The result of `./gradlew build` command is a new directory under `build` named `quarkus-app`.

You can run the application using: `java -jar build/quarkus-app/quarkus-run.jar`.

> [!WARNING]
> In order to successfully run the produced jar, you need to have the entire contents of the `quarkus-app` directory. If any of the files are missing, the application will not start or might not function correctly.

> [!TIP]
> The `fast-jar` packaging results in creating an artifact that starts a little faster and consumes slightly less memory than a legacy Quarkus jar because it has indexed information about which dependency jar contains classes and resources. It can thus avoid the lookup into potentially every jar on the classpath that the legacy jar necessitates, when loading a class or resource.

> [!TIP]
> You can reduce the size of the produced JAR by enabling experimental tree-shaking, which removes unreachable classes from runtime dependencies. See [Tree-shaking JAR dependencies](/guides/jar-tree-shaking/index.md) for details.

## Building Uber-Jars

Quarkus Gradle plugin supports the generation of Uber-Jars by specifying a `quarkus.package.jar.type` argument as follows:

**CLI**

```bash
quarkus build
quarkus deploy openshift
```

**Gradle**

```bash
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

When building an Uber-Jar you can specify entries that you want to exclude from the generated jar by using the `--ignored-entry` argument:

```bash
./gradlew quarkusBuild -Dquarkus.package.jar.type=uber-jar --ignored-entry=META-INF/file1.txt
```

The entries are relative to the root of the generated Uber-Jar. You can specify multiple entries by adding extra `--ignored-entry` arguments.

> [!TIP]
> You can reduce the size of the produced Uber-Jar by enabling experimental tree-shaking, which removes unreachable classes from runtime dependencies. See [Tree-shaking JAR dependencies](/guides/jar-tree-shaking/index.md) for details.

<a id="multi-module-gradle"></a>
### Working with multi-module projects

By default, Quarkus will not discover CDI beans inside another module.

The best way to enable CDI bean discovery for a module in a multi-module project would be to include a `META-INF/beans.xml` file, unless it is the main application module already configured with the `io.quarkus` Gradle plugin, in which case it will be indexed automatically.

Alternatively, there is some unofficial [Gradle Jandex plugins](https://plugins.gradle.org/search?term=jandex) that can be used instead of the `META-INF/beans.xml` file.

More information on this topic can be found on the [Bean Discovery](/guides/cdi-reference/index.md#bean_discovery) section of the CDI guide.

## Publishing your application

In order to make sure the right dependency versions are being used by Gradle, the BOM is declared as an `enforcedPlatform` in your build file. By default, the `maven-publish` plugin will prevent you from publishing your application due to this `enforcedPlatform`. This validation can be skipped by adding the following configuration in your build file:

**Groovy DSL**

```groovy
tasks.withType(GenerateModuleMetadata).configureEach {
    suppressedValidationErrors.add('enforced-platform')
}
```

**Kotlin DSL**

```kotlin
tasks.withType<GenerateModuleMetadata>().configureEach {
    suppressedValidationErrors.add("enforced-platform")
}
```

## Configuring Quarkus builds

There are multiple configuration sources that influence Quarkus builds, mentioned in the order of their priority. The Quarkus build uses the `prod` configuration profile:

1. System properties (for example `./gradlew -Dquarkus.package.jar.type=fast-jar ...`)
2. System environment (for example `QUARKUS_PACKAGE_JAR_TYPE=fast-jar ./gradlew ...`)
3. Configuration via Gradle project properties (for example `./gradlew -Pquarkus.package.jar.type=fast-jar`)
4. Configuration from a project's `application.properties`, `application.yaml` and `application.yml` files, as well as a project's `application-prod.properties`, `application-prod.yaml` and `application-prod.yml` files

> [!WARNING]
> The above priorities have changed in Quarkus plugin starting with 3.0. Older versions of the Quarkus Gradle plugin preferred `application.properties` over settings in the Gradle build.

> [!NOTE]
> The Quarkus Gradle plugin uses the "standard" Quarkus mechanisms to load and parse configurations. Support for `application.(yaml|yml)` has been added in Quarkus 3.0 in addition to `application.properties`. Also new in 3.0 is that all mechanisms available via SmallRye Config, are implicitly also now available for the Quarkus Gradle plugin.

> [!TIP]
> Use the `quarkusShowEffectiveConfig` task to show the effective configuration options used for a Quarkus build. If you specify the `--save-config-properties` command line option, the configuration properties are also store in the file `build/<final-name>.quarkus-build.properties`.

### Gradle caching / task inputs

By default, system properties starting with `quarkus.` and environment variables, including those from `~/.env`, starting  with `QUARKUS_`, are considered as inputs for the Gradle tasks. This means that only changes to those system properties or environment variables will cause Gradle's up-to-date to trigger a rebuild. Changes to other system properties or environment variables do not change Quarkus' Gradle task inputs and do not trigger an unnecessary rebuild.

Configuration properties specified via `quarkus.quarkusBuildProperties` or via the Quarkus `application.*` configuration files are all considered as Gradle task inputs, in other words: every change in these files causes a rebuild.

If your Quarkus build references system properties that do not start with `quarkus.` (or environment variables that do not start with `QUARKUS_`), you must reference those via the Quarkus build extension. For example, if your `application.properties` file references an environment variable like this:

greeting.message=${FOO_MESSAGE:Hello!}

it must be explicitly declared as "caching relevant":

quarkus { cachingRelevantProperties.add("FOO_MESSAGE") // Note: `cachingRelevantProperties` accepts regular expressions }

## Build workers

Quarkus application builds are ran in isolated processes using Gradle's worker API. This includes the Quarkus application build and Quarkus code generation. This is necessary to properly pass the configuration from the `quarkus` extension and Gradle project properties to Quarkus' code generator / application builder.

The JVM settings of the processes performing the code generation and/or Quarkus build can be configured as follows. See [JavaForkOptions](https://docs.gradle.org/current/javadoc/org/gradle/process/JavaForkOptions.html) for details.

**Groovy DSL**

```groovy
plugins {
    id 'java'
    id 'io.quarkus'
}

quarkus {
    buildForkOptions {
        maxHeapSize = '2g'
    }
    codeGenForkOptions {
        maxHeapSize = '128m'
    }
}
```

**Kotlin DSL**

```kotlin
plugins {
    java
    id("io.quarkus")
}

quarkus {
    buildForkOptions {
        maxHeapSize = "2g"
    }
    codeGenForkOptions {
        maxHeapSize = "128m"
    }
}
```

## Cached build artifacts

[Gradle's build cache](https://docs.gradle.org/current/userguide/build_cache.html) is a very efficient mechanism to improve the overall build runtime, by reusing previously generated outputs (see [Incremental build](https://docs.gradle.org/current/userguide/incremental_build.html) for technical details).

The Quarkus plugin leverages the Gradle mechanisms of up-to-date checks and the build cache. The build cache can be local or local *plus* a remote cache server or, if configured in CI environments, remote in the sense of retrieving and storing the whole cache as an artifact, for example using [Gradle's GitHub action](https://github.com/gradle/gradle-build-action) or directly/manually GitHub's [GitHub's cache action](https://github.com/actions/cache).

The Quarkus Gradle plugin cares about *what* is cached *in which environment* (CI or local development). Big artifacts like uber-jars and native binaries are not cached in CI, but are cached in local development environments.

> [!TIP]
> The Quarkus Gradle plugin detects a *CI environment*, if the `CI` environment variable is present.

How the various Quarkus package types are cached in non-CI and CI environments is described in the following table. Note that even if a task's output is not *cached*, the *up-to-date* checks still apply.

> [!NOTE]
> The Quarkus application build is split across three tasks. The `quarkusBuild` task is responsible to *provide* a built Quarkus application. The tasks `quarkusDependenciesBuild` and `quarkusAppPartsBuild` are considered internal tasks (may change at any time w/o prior notice). See below for details.

| Quarkus package type | Notes | Caching (non-CI) | Caching in CI |
| --- | --- | --- | --- |
| `fast-jar`, `jar` | Dependency jars are stored unmodified as individual files in the `quarkus-app/lib/` directory. All other files in the `quarkus-app/` directory are generated.<br><br>The `quarkusAppPartsBuild` task builds `fast-jar` package-type applications and allows caching of the *generated* pieces, which is everything except the dependencies in the `quarkus-app/lib/` directory. The `quarkusDependenciesBuild` task is used to collect the dependencies via Gradle mechanisms. The `quarkusBuild` task then assembles the outputs of the `quarkusAppPartsBuild` and `quarkusDependenciesBuild` tasks.<br><br>Note: `fast-jar` (or `-jar`) is the default if no package type has been explicitly configured. | ✅ | ✅ |
| `mutable-jar`, `uber-jar`, `native`, `native-sources` | The `quarkusBuild` task is responsible for building the Quarkus application.<br><br>The `quarkusAppPartsBuild` and `quarkusDependenciesBuild` tasks do nothing for `uber-jar`s. | ✅ | ❌ |
| `legacy-jar`, `legacy` | `legacy-jar` builds work similar to `fast-jar` builds, except that the directory structure is different and the `modified-*.jar` files are considered as generated. | ✅ | ✅ |

> [!NOTE]
> In a local development environment, the cost (think: time) of storing (and retrieving) even bigger cache artifacts is lower than the cost of re-building a Quarkus application. This means, that The Quarkus Gradle plugin allows caching even potentially big artifacts like uber-jars or native binaries in non-CI environments. In CI environments, which run builds against varying states of a code base (think: running CI against every commit on a main branch), adding each built (and big) artifact to the build cache would let the build cache become unnecessarily big, which becomes a problem for example in GitHub, where the total amount of cached artifacts is limited to 10 GB.

> [!NOTE]
> Background information: There are two related mechanisms in Gradle at play to improve build performance:
>
> - Up-to-date checks allow a task's execution to be skipped, if the *inputs* and *outputs* of the tasks did not change. For example, consider a Java compile tasks: if the library dependencies and the source files (the *inputs*) did not change and the compiled class files (the *outputs*) are still available, compilation can be skipped.
> - The build cache can store the outputs of (cacheable) tasks in the local build cache. The *output* of a task can be restored from the cache.
>
> The benefits of up-to-date checks and the interaction of the build cache come with the cost of modeling the *inputs* and *outputs*. Inputs are not only files or directories, but also the Java version used during the build, the operating system, the working directory, configuration options, and so on. So everything that influences the output of a task action must be declared as an input of the task.

## Gradle configuration cache

The Quarkus Gradle plugin works with builds that have the [Gradle's configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html) enabled, but the configuration cache is disabled for some of the Quarkus tasks. This means, that the Quarkus plugin does not break such Gradle builds. The current state of compatibility is shown in the following table:

| Quarkus task | Configuration Cache Compatibility |
| --- | --- |
| `quarkusGenerateCode` | ✅ |
| `quarkusGenerateCodeDev` | ✅ |
| `quarkusGenerateCodeTests` | ✅ |
| `quarkusDependenciesBuild` | ✅ |
| `quarkusAppPartsBuild` | ✅ |
| `quarkusShowEffectiveConfig` | ✅ |
| `quarkusBuild` | ✅ |
| `imageBuild` | ✅ |
| `imagePush` | ✅ |
| `quarkusDev` | ❌ |
| `quarkusRun` | ❌ |
| `quarkusRemoteDev` | ❌ |
| `quarkusTest` | ❌ |
| `quarkusGoOffline` | ❌ |
| `quarkusInfo` | ❌ |
| `quarkusUpdate` | ❌ |
| `deploy` | ❌ |
| `listExtensions` | ❌ |
| `listCategories` | ❌ |
| `listPlatforms` | ❌ |
| `addExtension` | ❌ |
| `removeExtension` | ❌ |

## Dependency resolver options

### Quarkus component variants

`disableQuarkusComponentVariants` project property was introduced in Quarkus 3.25.1 to control whether Quarkus component variants should be added to certain dependencies of a project to enable [Quarkus conditional](/guides/conditional-extension-dependencies/index.md) and build time (deployment) dependencies. Quarkus component variants are enabled by default since 3.25.1.

The approach to enable Quarkus conditional and extension build time (deployment) dependencies has changed in Quarkus 3.25.1 due to a few issues with the previous implementation. Specifically, the previous implementation would not apply relevant dependency exclusions to enabled conditional and deployment dependencies. It would also appear to leak devmode-only dependencies into non-devmode classpaths (such as test and prod) in some cases. These issues couldn't be fixed in a reasonable way in the previous implementation, which is why the Quarkus dependency resolution had to be re-implemented based on a different approach using [Gradle component variants](https://docs.gradle.org/current/userguide/variant_aware_resolution.html).

The previous implementation still remains available for now in case the new one appears to introduce not yet detected regressions and can be enabled by setting `disableQuarkusComponentVariants` project property to `true`.
