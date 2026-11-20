# Building a native executable

This guide covers:

- Compiling the application to a native executable
- Packaging the native executable in a container
- Debugging native executable

## Do you need a native executable?

Quarkus on the JVM is already fast, sub-second startup, low memory footprint, and optimized throughput. For many applications, JVM mode is the right choice.

Native executables shine when:

- **Startup time is critical**: serverless/FaaS, CLI tools, or scale-to-zero environments where cold starts matter.
- **Memory is constrained**: dense container deployments or edge devices with tight RAM budgets.
- **You want the smallest possible container image**: native binaries produce images under 50MB.

JVM mode is typically better when:

- **Peak throughput matters most**: the JVM's JIT compiler optimizes hot paths at runtime, which can outperform native on long-running workloads.
- **Build time is a constraint**: native compilation takes minutes, JVM builds take seconds.
- **You rely heavily on reflection or dynamic class loading**: these require explicit configuration for native images.

> [!TIP]
> Start with JVM mode. Move to native when you have a concrete need. Both modes use the same code and the same Quarkus optimizations: native just changes how the binary is produced.

<a id="comparing-jvm-aot-native"></a>
### Comparing JVM, Leyden AOT, and native modes

Native is one of three runtime options Quarkus supports. JVM fast-jar, JVM with [Project Leyden AOT caching](/guides/aot/index.md) (JDK 24+), and native (Mandrel) each trade cold-start speed, peak throughput, memory footprint, and build cost differently. The table below compares them. Pick the mode that matches your workload's most important constraint.

| Mode | Cold start | Time to first request | Peak throughput | Memory (RSS) | Container image size | Build cost | When to choose |
| --- | --- | --- | --- | --- | --- | --- | --- |
| JVM fast-jar | \~0.4 s (small REST) to \~3 s (large CRUD) | 4,417 ms | 13,265 tps (baseline) | 304 MiB | 517 MB | \~30 s build; no special workflow | Long-running services, throughput-critical workloads; teams on pre-JDK-24 runtimes |
| [JVM + Leyden AOT cache](/guides/aot/index.md) | \~80 ms (small) to \~900 ms (large) | 1,859 ms | 12,389 tps (\~7% below JVM) | 240 MiB | 715 MB (AOT cache adds \~198 MB) | Standard build plus a training run on a representative workload | Cold-start-sensitive but not millisecond-critical; requires JDK 24+; keeps full JVM tooling (debuggers, profilers, JFR) |
| Native (Mandrel) | \~17 ms (small) to \~240 ms (large) | 581 ms | 5,411 tps (\~59% below JVM) | 95 MiB | 244 MB | 3-10 min build; 4-8 GB build-host RAM | Extreme cold start (serverless, scale-to-zero); edge deployments; high-density container hosts where memory is the binding constraint |

Time to first request, peak throughput, and memory (RSS) come from the [2026-04-21 perf-lab tuned benchmark](https://raw.githubusercontent.com/quarkusio/benchmarks/main/images/spring-quarkus-perf-comparison/2026-04-21_09-26-13/metrics-tuned-composite-for-quarkus-light.svg) (Quarkus 3.34.3, JDK 25.0.2, GraalVM 25.0.2-graalce, 4 CPUs, `-Xmx512m`). Cold-start ranges and container image sizes come from the [Leyden integration benchmarks](https://quarkus.io/blog/leyden-2/) and the [Mar 2026 performance post](https://quarkus.io/blog/new-benchmarks/). For the latest numbers, see the [Quarkus benchmarks chart reference](https://github.com/quarkusio/benchmarks#chart-reference).

For the Leyden AOT cache path, see [the AOT caching guide](/guides/aot/index.md). If native is the right fit for your workload, read on. The next sections walk you through prerequisites, compilation, testing, and container packaging.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm)

- A [working C development environment](#configuring-c-development)
- The code of the application developed in the [Getting Started Guide](/guides/getting-started/index.md).

<a id="configuring-c-development"></a>

> [!NOTE]
> **Supporting native compilation in C**
>
> What does having a working C developer environment mean?
>
> - On Linux, you will need GCC, and the glibc and zlib headers. Examples for common distributions:
>
>   ```bash
>   # dnf (rpm-based)
>   sudo dnf install gcc glibc-devel zlib-devel libstdc++-static
>   # Debian-based distributions:
>   sudo apt-get install build-essential libz-dev zlib1g-dev
>   # Arch Linux
>   sudo pacman -S freetype2 gcc glibc lib32-gcc-libs zlib
>   ```
>
> - XCode provides the required dependencies on macOS:
>
>   ```bash
>   xcode-select --install
>   ```
>
> - On Windows, you will need to install the [Visual Studio 2022 Visual C++ Build Tools](https://aka.ms/vs/17/release/vs_buildtools.exe)

### Background

Building a native executable requires using a distribution of GraalVM. There are three distributions: Oracle GraalVM Community Edition (CE), Oracle GraalVM Enterprise Edition (EE) and Mandrel. The differences between the Oracle and Mandrel distributions are as follows:

- Mandrel is a downstream distribution of the Oracle GraalVM CE. Mandrel's main goal is to provide a way to build native executables specifically designed to support Quarkus.

- Mandrel releases are built from a code base derived from the upstream Oracle GraalVM CE code base, with only minor changes but some significant exclusions that are not necessary for Quarkus native apps. They support the same capabilities to build native executables as Oracle GraalVM CE, with no significant changes to functionality. Notably, they do not include support for polyglot programming. The reason for these exclusions is to provide a better level of support for the majority of Quarkus users. These exclusions also mean Mandrel offers a considerable reduction in its distribution size when compared with Oracle GraalVM CE/EE.

- Mandrel is built slightly differently to Oracle GraalVM CE, using the standard OpenJDK project. This means that it does not profit from a few small enhancements that Oracle have added to the version of OpenJDK used to build their own GraalVM downloads. These enhancements are omitted because upstream OpenJDK does not manage them, and cannot vouch for. This is particularly important when it comes to conformance and security.

- Mandrel is recommended for building native executables that target Linux containerized environments. This means that Mandrel users are encouraged to use containers to build their native executables. If you are building native executables for macOS on amd64/x86, you should consider using Oracle GraalVM instead, because Mandrel does not currently target this platform. Building native executables directly on bare metal Linux, macOS (on M processors), or Windows is possible, with details available in the [Mandrel README](https://github.com/graalvm/mandrel/blob/default/README.md) and [Mandrel releases](https://github.com/graalvm/mandrel/releases).

<a id="configuring-graalvm"></a>
### Configuring GraalVM

> [!TIP]
> This step is only required for generating native executables targeting non-Linux operating systems. For generating native executables targeting Linux, you can optionally skip this section and [use a builder image](#container-runtime) instead.

> [!TIP]
> If you cannot install GraalVM, you can use a multi-stage Docker build to run Maven inside a Docker container that embeds GraalVM. There is an explanation of how to do this at [the end of this guide](#multistage-docker).

GraalVM for JDK 21 is required.

1. Install GraalVM if you haven't already. You have a few options for this: \*\* Download the appropriate archive from <https://github.com/graalvm/mandrel/releases> or <https://github.com/graalvm/graalvm-ce-builds/releases>, and unpack it like you would any other JDK. \*\* Use platform-specific installer tools like [sdkman](https://sdkman.io/jdks#graalce), [homebrew](https://github.com/graalvm/homebrew-tap), or [scoop](https://github.com/ScoopInstaller/Java). We recommend the *community edition* of GraalVM. For example, install it with `sdk install java 21-graalce`.
2. Configure the runtime environment. Set `GRAALVM_HOME` environment variable to the GraalVM installation directory, for example:

   ```bash
   export GRAALVM_HOME=$HOME/Development/mandrel/
   ```

   On macOS (amd64/x86 based Macs not supported), point the variable to the `Home` sub-directory:

   ```bash
   export GRAALVM_HOME=$HOME/Development/graalvm/Contents/Home/
   ```

   On Windows, you will have to go through the Control Panel to set your environment variables.

   > [!TIP]
   > Installing via scoop will do this for you.

3. (Optional) Set the `JAVA_HOME` environment variable to the GraalVM installation directory.

   ```bash
   export JAVA_HOME=${GRAALVM_HOME}
   ```

4. (Optional) Add the GraalVM `bin` directory to the path

   ```bash
   export PATH=${GRAALVM_HOME}/bin:$PATH
   ```


<a id="graal-and-macos"></a>

> [!NOTE]
> **Issues using GraalVM with macOS**
>
> GraalVM binaries are not (yet) notarized for macOS as reported in this [GraalVM issue](https://github.com/oracle/graal/issues/1724). This means that you may see the following error when using `native-image`:
>
> ```bash
> “native-image” cannot be opened because the developer cannot be verified
> ```
>
> Use the following command to recursively delete the `com.apple.quarantine` extended attribute on the GraalVM install directory as a workaround:
>
> ---
>
> xattr -r -d com.apple.quarantine ${GRAALVM_HOME}/../..
>
> ---

## Solution

We recommend that you follow the instructions in the next sections and package the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `getting-started` directory.

## Producing a native executable

The native executable for our application will contain the application code, required libraries, Java APIs, and a reduced version of a VM. The smaller VM base improves the startup time of the application and produces a minimal disk footprint.

![Creating a native executable](/guides/images/native-executable-process.png)

If you have generated the application from the previous tutorial, you can find in the `pom.xml` the following Maven profile section:

```xml
<profiles>
    <profile>
        <id>native</id>
        <activation>
            <property>
                <name>native</name>
            </property>
        </activation>
        <properties>
            <skipITs>false</skipITs>
            <quarkus.native.enabled>true</quarkus.native.enabled>
        </properties>
    </profile>
</profiles>
```

> [!TIP]
> You can provide custom options for the `native-image` command using the `quarkus.native.additional-build-args` and `quarkus.native.additional-build-args-append` properties. Multiple options may be separated by a comma.
>
> By convention `quarkus.native.additional-build-args-append` is meant to be defined at the command line (e.g. `-Dquarkus.native.additional-build-args-append=--verbose`), while `quarkus.native.additional-build-args` may be defined either at the command line or in your `application.properties`. Note that, any arguments included in `quarkus.native.additional-build-args-append` may override those included in `quarkus.native.additional-build-args`.
>
> You can find more information about how to configure the native image building process in the [\[configuration-reference\]](#configuration-reference) section below.

We use a profile because, you will see very soon, packaging the native executable takes a *few* minutes. You could just pass -Dquarkus.native.enabled=true as a property on the command line, however it is better to use a profile as this allows native image tests to also be run.

Create a native executable using:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

<a id="graal-and-windows"></a>

> [!NOTE]
> **Issues with packaging on Windows**
>
> The Microsoft Native Tools for Visual Studio must first be initialized before packaging. You can do this by starting the `x64 Native Tools Command Prompt` that was installed with the Visual Studio Build Tools. At the `x64 Native Tools Command Prompt`, you can navigate to your project folder and run `./mvnw package -Dnative`.
>
> Another solution is to write a script to do this for you:
>
> ```bash
> cmd /c 'call "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat" && mvn package -Dnative'
> ```

In addition to the regular files, the build also produces `target/getting-started-1.0.0-SNAPSHOT-runner`. You can run it using: `./target/getting-started-1.0.0-SNAPSHOT-runner`.

<a id="graal-package-preview"></a>

> [!NOTE]
> **Java preview features**
>
> Java code that relies on preview features requires special attention. To produce a native executable, this means that the `--enable-preview` flag needs to be passed to the underlying native image invocation. You can do so by prepending the flag with `-J` and passing it as additional native build argument: `-Dquarkus.native.additional-build-args=-J--enable-preview`.

### Build fully static native executables

> [!IMPORTANT]
> Fully static native executables support is experimental.

On Linux it's possible to package a native executable that doesn't depend on any system shared library. There are [some system requirements](https://www.graalvm.org/jdk21/reference-manual/native-image/guides/build-static-executables/#prerequisites-and-preparation) to be fulfilled and additional build arguments to be used along with the `native-image` invocation, a minimum is `-Dquarkus.native.additional-build-args="--static","--libc=musl"`.

Compiling fully static binaries is done by statically linking [musl](https://musl.libc.org/) instead of `glibc` and should not be used in production without rigorous testing.

## Testing the native executable

Producing a native executable can lead to a few issues, and so it's also a good idea to run some tests against the application running in the native file. The reasoning is explained in the [Testing Guide](/guides/getting-started-testing/index.md#quarkus-integration-test).

To see the `GreetingResourceIT` run against the native executable, use `./mvnw verify -Dnative`:

```shell
$ ./mvnw verify -Dnative
...
Finished generating 'getting-started-1.0.0-SNAPSHOT-runner' in 22.0s.
[INFO] [io.quarkus.deployment.pkg.steps.NativeImageBuildRunner] docker run --env LANG=C --rm --user 1000:1000 -v /home/zakkak/code/quarkus-quickstarts/getting-started/target/getting-started-1.0.0-SNAPSHOT-native-image-source-jar:/project:z --entrypoint /bin/bash quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 -c objcopy --strip-debug getting-started-1.0.0-SNAPSHOT-runner
[INFO] [io.quarkus.deployment.QuarkusAugmentor] Quarkus augmentation completed in 70686ms
[INFO]
[INFO] --- maven-failsafe-plugin:3.0.0-M7:integration-test (default) @ getting-started ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO]
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.acme.getting.started.GreetingResourceIT
Executing "/home/zakkak/code/quarkus-quickstarts/getting-started/target/getting-started-1.0.0-SNAPSHOT-runner -Dquarkus.http.port=8081 -Dquarkus.http.ssl-port=8444 -Dquarkus.log.file.path=/home/zakkak/code/quarkus-quickstarts/getting-started/target/quarkus.log -Dquarkus.log.file.enable=true -Dquarkus.log.category."io.quarkus".level=INFO"
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2023-05-05 10:55:52,068 INFO  [io.quarkus] (main) getting-started 1.0.0-SNAPSHOT native (powered by Quarkus 3.0.2.Final) started in 0.009s. Listening on: http://0.0.0.0:8081
2023-05-05 10:55:52,069 INFO  [io.quarkus] (main) Profile prod activated.
2023-05-05 10:55:52,069 INFO  [io.quarkus] (main) Installed features: [cdi, rest, smallrye-context-propagation, vertx]
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.99 s - in org.acme.getting.started.GreetingResourceIT
...
```

> [!TIP]
> By default, Quarkus waits for 60 seconds for the native image to start before automatically failing the native tests. This duration can be changed using the `quarkus.test.wait-time` system property. For example, to increase the duration to 300 seconds, use: `./mvnw verify -Dnative -Dquarkus.test.wait-time=300`.

> [!WARNING]
> This procedure was formerly accomplished using the `@NativeImageTest` annotation. `@NativeImageTest` was replaced by `@QuarkusIntegrationTest` which provides a superset of the testing capabilities of `@NativeImageTest`. More information about `@QuarkusIntegrationTest` can be found in the [Testing Guide](/guides/getting-started-testing/index.md#quarkus-integration-test).

### Profiles

By default, integration tests both **build** and **run** the native executable using the `prod` profile.

You can override the profile the executable **runs** with during the test using the `quarkus.test.integration-test-profile` property. Either by adding it to `application.properties` or by appending it to the command line: `./mvnw verify -Dnative -Dquarkus.test.integration-test-profile=test`. Your `%test.` prefixed properties will be used at the test runtime.

You can override the profile the executable is **built** with and **runs** with using the `quarkus.profile=test` property, e.g. `./mvnw clean verify -Dnative -Dquarkus.profile=test`. This might come handy if there are test specific resources to be processed, such as importing test data into the database.

```properties
quarkus.native.resources.includes=version.txt
%test.quarkus.native.resources.includes=version.txt,import-dev.sql
%test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
%test.quarkus.hibernate-orm.sql-load-script=import-dev.sql
```

With the aforementioned example in your `application.properties`, your Hibernate ORM managed database will be populated with test data both during the JVM mode test run and during the native mode test run. The production executable will contain only the `version.txt` resource, no superfluous test data.

> [!WARNING]
> The executable built with `-Dquarkus.profile=test` is not suitable for production deployment. It contains your test resources files and settings. Once the testing is done, the executable would have to be built again, using the default, `prod` profile.
>
> Alternatively, if you need to specify specific properties when running tests against the native executable built using the `prod` profile, an option is to put those properties in file `src/test/resources/application-nativeit.yaml`, and refer to it from the `failsafe` plugin configuration using the `QUARKUS_CONFIG_LOCATIONS` environment variable. For instance:
>
> ```xml
> <plugin>
>   <artifactId>maven-failsafe-plugin</artifactId>
>   <version>${surefire-plugin.version}</version>
>   <executions>
>     <execution>
>       <goals>
>         <goal>integration-test</goal>
>         <goal>verify</goal>
>       </goals>
>       <configuration>
>         <systemPropertyVariables>
>           <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
>           <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
>           <maven.home>${maven.home}</maven.home>
>         </systemPropertyVariables>
>         <environmentVariables>
>           <QUARKUS_CONFIG_LOCATIONS>./src/test/resources/application-nativeit.yaml</QUARKUS_CONFIG_LOCATIONS>
>         </environmentVariables>
>       </configuration>
>     </execution>
>   </executions>
> </plugin>
> ```

### Java preview features

<a id="graal-test-preview"></a>

> [!NOTE]
> **Java preview features**
>
> Java code that relies on preview features requires special attention. To test a native executable, this means that the `--enable-preview` flag needs to be passed to the Surefire plugin. Adding `<argLine>--enable-preview</argLine>` to its `configuration` section is one way to do so.

### Excluding tests when running as a native executable

When running tests this way, the only things that actually run natively are your application endpoints, which you can only test via HTTP calls. Your test code does not actually run natively, so if you are testing code that does not call your HTTP endpoints, it's probably not a good idea to run them as part of native tests.

If you share your test class between JVM and native executions like we advise above, you can mark certain tests with the `@DisabledOnIntegrationTest` annotation in order to skip them when testing against a native image.

> [!NOTE]
> Using `@DisabledOnIntegrationTest` will also disable the test in all integration test instances, including testing the application in JVM mode, in a container image, and native image.

### Testing an existing native executable

It is also possible to re-run the tests against a native executable that has already been built. To do this run `./mvnw test-compile failsafe:integration-test -Dnative`. This will discover the existing native image and run the tests against it using failsafe.

If the process cannot find the native image for some reason, or you want to test a native image that is no longer in the target directory you can specify the executable with the `-Dnative.image.path=` system property.

<a id="container-runtime"></a>
## Creating a Linux executable without GraalVM installed

> [!IMPORTANT]
> Before going further, be sure to have a working container runtime (Docker, podman) environment. If you use Docker on Windows you should share your project's drive at Docker Desktop file share settings and restart Docker Desktop.

Quite often one only needs to create a native Linux executable for their Quarkus application (for example in order to run in a containerized environment) and would like to avoid the trouble of installing the proper GraalVM version in order to accomplish this task (for example, in CI environments it's common practice to install as little software as possible).

To this end, Quarkus provides a very convenient way of creating a native Linux executable by leveraging a container runtime such as Docker or podman. The easiest way of accomplishing this task is to execute:

**CLI**

```bash
quarkus build --native --no-tests -Dquarkus.native.container-build=true
# The --no-tests flag is required only on Windows and macOS.
```

**Maven**

```bash
./mvnw install -Dnative -DskipTests -Dquarkus.native.container-build=true
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

> [!TIP]
> By default, Quarkus automatically detects the container runtime. If you want to explicitly select the container runtime, you can do it with:
>
> For Docker:
>
> **CLI**
>
> ```bash
> quarkus build --native -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=docker
> ```
>
> **Maven**
>
> ```bash
> ./mvnw install -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=docker
> ```
>
> **Gradle**
>
> ```bash
> ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=docker
> ```
>
> For podman:
>
> **CLI**
>
> ```bash
> quarkus build --native -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman
> ```
>
> **Maven**
>
> ```bash
> ./mvnw install -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman
> ```
>
> **Gradle**
>
> ```bash
> ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman
> ```
>
> These are regular Quarkus config properties, so if you always want to build in a container it is recommended you add these to your `application.properties` in order to avoid specifying them every time.

Executable built that way with the container runtime will be a 64-bit Linux executable, so depending on your operating system, it may no longer be runnable.

> [!IMPORTANT]
> Starting with Quarkus 3.19+, the *builder* image used to build the native executable is based on UBI 9. It means that the native executable produced by the container build will be based on UBI 9 as well. So, if you plan to build a container, make sure that the base image in your `Dockerfile` is compatible with UBI 9. The native executable will not run on UBI 8 base images.
>
> You can configure the builder image used for the container build by setting the `quarkus.native.builder-image` property. For example, to switch back to an UBI8 *builder image* you can use:
>
> `quarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21`
>
> You can see the available tags for UBI8 [here](https://quay.io/repository/quarkus/ubi-quarkus-mandrel-builder-image?tab=tags) and for UBI9 [here (UBI 9)](https://quay.io/repository/quarkus/ubi9-quarkus-mandrel-builder-image?tab=tags))

<a id="tip-quarkus-native-remote-container-build"></a>

> [!TIP]
> If you see the following invalid path error for your application JAR when trying to create a native executable using a container build, even though your JAR was built successfully, you're most likely using a remote daemon for your container runtime.
>
> ```
> Error: Invalid Path entry getting-started-1.0.0-SNAPSHOT-runner.jar
> Caused by: java.nio.file.NoSuchFileException: /project/getting-started-1.0.0-SNAPSHOT-runner.jar
> ```
>
> In this case, use the parameter `-Dquarkus.native.remote-container-build=true` instead of `-Dquarkus.native.container-build=true`.
>
> The reason for this is that the local build driver invoked through `-Dquarkus.native.container-build=true` uses volume mounts to make the JAR available in the build container, but volume mounts do not work with remote daemons. The remote container build driver copies the necessary files instead of mounting them. Note that even though the remote driver also works with local daemons, the local driver should be preferred in the local case because mounting is usually more performant than copying.

> [!TIP]
> Building with GraalVM instead of Mandrel requires a custom builder image parameter to be passed additionally:
>
> **CLI**
>
> ```bash
> quarkus build --native -Dquarkus.native.container-build=true -Dquarkus.native.builder-image=graalvm
> ```
>
> **Maven**
>
> ```bash
> ./mvnw install -Dnative -Dquarkus.native.container-build=true -Dquarkus.native.builder-image=graalvm
> ```
>
> **Gradle**
>
> ```bash
> ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true -Dquarkus.native.builder-image=graalvm
> ```
>
> Please note that the above command points to a floating tag. It is highly recommended to use the floating tag, so that your builder image remains up-to-date and secure. If you absolutely must, you may hard-code to a specific tag (see [here (UBI 8)](https://quay.io/repository/quarkus/ubi-quarkus-mandrel-builder-image?tab=tags) and  [here (UBI 9)](https://quay.io/repository/quarkus/ubi9-quarkus-mandrel-builder-image?tab=tags) for available tags), but be aware that you won't get security updates that way and it's unsupported.

## Creating a container

### Using the container-image extensions

By far the easiest way to create a container-image from your Quarkus application is to leverage one of the container-image extensions.

If one of those extensions is present, then creating a container image for the native executable is essentially a matter of executing a single command:

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true -Dquarkus.container-image.build=true
```

- `quarkus.native.container-build=true` allows for creating a Linux executable without GraalVM being installed (and is only necessary if you don't have GraalVM installed locally or your local operating system is not Linux)

> [!NOTE]
> If you're running a remote Docker daemon, you need to replace `quarkus.native.container-build=true` with `quarkus.native.remote-container-build=true`.
>
> See [Creating a Linux executable without GraalVM installed](#tip-quarkus-native-remote-container-build) for more details.

- `quarkus.container-image.build=true` instructs Quarkus to create a container-image using the final application artifact (which is the native executable in this case)

See the [Container Image guide](/guides/container-image/index.md) for more details.

### Manually using the micro base image

You can run the application in a container using the JAR produced by the Quarkus Maven Plugin. However, in this section, we focus on creating a container image using the produced native executable.

![Containerization Process](/guides/images/containerization-process.png)

When using a local GraalVM installation, the native executable targets your local operating system (Linux, macOS, Windows etc). However, as a container may not use the same *executable* format as the one produced by your operating system, we will instruct the Maven build to produce an executable by leveraging a container runtime (as described in [this section](#container-runtime)):

The produced executable will be a 64-bit Linux executable, so depending on your operating system, it may no longer be runnable. However, it's not an issue as we are going to copy it to a container. The project generation has provided a `Dockerfile.native-micro` in the `src/main/docker` directory with the following content:

```dockerfile
FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --chown=1001:root --chmod=755 target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

> [!NOTE]
> **Quarkus Micro Image?**
>
> The Quarkus Micro Image is a small container image providing the right set of dependencies to run your native application. It is based on [UBI Micro](https://catalog.redhat.com/software/containers/ubi9-micro/61832b36dd607bfc82e66399?container-tabs=overview). This base image has been tailored to work perfectly in containers.
>
> You can read more about UBI images on:
>
> - [Introduction to Universal Base Image](https://www.redhat.com/en/blog/introducing-red-hat-universal-base-image)
> - [Red Hat Universal Base Image 9](https://catalog.redhat.com/software/containers/ubi9/ubi/615bcf606feffc5384e8452e)
>
> UBI images can be used without any limitations.
>
> [This page](/guides/quarkus-runtime-base-image/index.md) explains how to extend the `quarkus-micro` image when your application has specific requirements.

Then, if you didn't delete the generated native executable, you can build the docker image with:

```bash
docker build -f src/main/docker/Dockerfile.native-micro -t quarkus-quickstart/getting-started .
```

And finally, run it with:

```bash
docker run -i --rm -p 8080:8080 quarkus-quickstart/getting-started
```

### Manually using the minimal base image

The project generation has also provided a `Dockerfile.native` in the `src/main/docker` directory with the following content:

```dockerfile
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.6
WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --chown=1001:root --chmod=0755 target/*-runner /work/application

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

The UBI minimal image is bigger than the micro one mentioned above. It contains more utilities such as the `microdnf` package manager.

<a id="multistage-docker"></a>
### Using a multi-stage Docker build

The previous section showed you how to build a native executable using Maven or Gradle, but it requires you to have created the native executable first. In addition, this native executable must be a Linux 64 bits executable.

You may want to build the native executable directly in a container without having a final container containing the build tools. That approach is possible with a multi-stage Docker build:

1. The first stage builds the native executable using Maven or Gradle
2. The second stage is a minimal image copying the produced native executable

> [!WARNING]
> Before building a container image from the Dockerfiles shown below, you need to update the default `.dockerignore` file, as it filters everything except the `target` directory. In order to build inside a container, you need to copy the `src` directory. Thus, edit your `.dockerignore` and remove the `*` line.

Such a multi-stage build can be achieved as follows:

Sample Dockerfile for building with Maven:

```dockerfile
## Stage 1 : build with maven builder image with native capabilities
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 AS build
COPY --chown=quarkus:quarkus --chmod=0755 mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/
USER quarkus
WORKDIR /code
RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline
COPY src /code/src
RUN ./mvnw package -Dnative

## Stage 2 : create the docker final image
FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/
COPY --from=build /code/target/*-runner /work/application

# set up permissions for user `1001`
RUN chmod 775 /work /work/application \
  && chown -R 1001 /work \
  && chmod -R "g+rwX" /work \
  && chown -R 1001:root /work

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

> [!NOTE]
> This multi-stage Docker build copies the Maven wrapper from the host machine. The Maven wrapper (or the Gradle wrapper) is a convenient way to provide a specific version of Maven/Gradle. It avoids having to create a base image with Maven and Gradle. To provision the Maven Wrapper in your project, use: `mvn wrapper:wrapper`.

Save this file in `src/main/docker/Dockerfile.multistage` as it is not included in the getting started quickstart.

Sample Dockerfile for building with Gradle:

```dockerfile
## Stage 1 : build with maven builder image with native capabilities
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 AS build
USER root
RUN microdnf install findutils -y
COPY --chown=quarkus:quarkus gradlew /code/gradlew
COPY --chown=quarkus:quarkus gradle /code/gradle
COPY --chown=quarkus:quarkus build.gradle /code/
COPY --chown=quarkus:quarkus settings.gradle /code/
COPY --chown=quarkus:quarkus gradle.properties /code/
USER quarkus
WORKDIR /code
COPY src /code/src
RUN ./gradlew build -Dquarkus.native.enabled=true

## Stage 2 : create the docker final image
FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/
COPY --from=build /code/build/*-runner /work/application
RUN chmod 775 /work
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

If you are using Gradle in your project, you can use this sample Dockerfile.  Save it in `src/main/docker/Dockerfile.multistage`.

```bash
docker build -f src/main/docker/Dockerfile.multistage -t quarkus-quickstart/getting-started .
```

And, finally, run it with:

```bash
docker run -i --rm -p 8080:8080 quarkus-quickstart/getting-started
```

> [!TIP]
> If you need SSL support in your native executable, you can easily include the necessary libraries in your Docker image.
>
> Please see [our Using SSL With Native Executables guide](/guides/native-and-ssl/index.md#working-with-containers) for more information.

> [!NOTE]
> To use GraalVM CE instead of Mandrel, update the `FROM` clause to: `FROM quay.io/quarkus/ubi9-quarkus-graalvmce-builder-image:jdk-21 AS build`.

### Using a Distroless base image

> [!IMPORTANT]
> Distroless image support is experimental.

If you are looking for small container images, the [distroless](https://github.com/GoogleContainerTools/distroless) approach reduces the size of the base layer. The idea behind *distroless* is the usage of a single and minimal base image containing all the requirements, and sometimes even the application itself.

Quarkus provides a distroless base image that you can use in your `Dockerfile`. You only need to copy your application, and you are done:

```dockerfile
FROM quay.io/quarkus/quarkus-distroless-image:2.0
COPY target/*-runner /application

EXPOSE 8080
USER nonroot

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

Quarkus provides the `quay.io/quarkus/quarkus-distroless-image:2.0` image. It contains the required packages to run a native executable and is only **9Mb**. Just add your application on top of this image, and you will get a tiny container image.

Distroless images should not be used in production without rigorous testing.

### Build a container image from scratch

> [!IMPORTANT]
> Scratch image support is experimental.

Building fully statically linked binaries enables the usage of a [scratch image](https://hub.docker.com/_/scratch) containing solely the resulting native executable.

Sample multistage Dockerfile for building an image from `scratch`:

```dockerfile
## Stage 1 : build with maven builder image with native capabilities
FROM quay.io/quarkus/ubi9-quarkus-graalvmce-builder-image:jdk-21 AS build
USER root
RUN microdnf install make gcc -y
COPY --chown=quarkus:quarkus mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/
RUN mkdir /musl && \
    curl -L -o musl.tar.gz https://more.musl.cc/11.2.1/x86_64-linux-musl/x86_64-linux-musl-native.tgz && \
    tar -xvzf musl.tar.gz -C /musl --strip-components 1 && \
    curl -L -o zlib.tar.gz https://github.com/madler/zlib/releases/download/v1.2.13/zlib-1.2.13.tar.gz && \
    mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && \
    cd zlib && ./configure --static --prefix=/musl && \
    make && make install && \
    cd .. && rm -rf zlib && rm -f zlib.tar.gz && rm -f musl.tar.gz
ENV PATH="/musl/bin:${PATH}"
USER quarkus
WORKDIR /code
RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline
COPY src /code/src
RUN ./mvnw package -Dnative -DskipTests -Dquarkus.native.additional-build-args="--static","--libc=musl"

## Stage 2 : create the final image
FROM scratch
COPY --from=build /code/target/*-runner /application
EXPOSE 8080
ENTRYPOINT [ "/application" ]
```

Scratch images should not be used in production without rigorous testing.

> [!NOTE]
> The versions of musl and zlib may need to be updated to meet the native-image executable requirements (and UPX if you use native image compression).

### Compress native images

Quarkus can compress the produced native executable using UPX. More details on [UPX Compression documentation](/guides/upx/index.md).

### Separating Java and native image compilation

In certain circumstances, you may want to build the native image in a separate step. For example, in a CI/CD pipeline, you may want to have one step to generate the source that will be used for the native image generation and another step to use these sources to actually build the native executable. For this use case, you can set the additional flag `quarkus.native.sources-only=true`. This will execute the java compilation as if you had started native compilation (`-Dnative`), but stops before triggering the actual call to GraalVM's `native-image`.

```bash
$ ./mvnw clean package -Dnative -Dquarkus.native.sources-only=true
```

After compilation has finished, you find the build artifact in `target/native-sources`:

```bash
$ cd target/native-sources
$ ls
getting-started-1.0.0-SNAPSHOT-runner.jar  graalvm.version  lib  native-image.args
```

From the output above one can see that, in addition to the produced jar file and the associated lib directory, a text file named `native-image.args` was created. This file holds all parameters (including the name of the JAR to compile) to pass along to GraalVM's `native-image` command. A text file named `graalvm.version` was also created and holds the GraalVM version that should be used. If you have GraalVM installed and it matches this version, you can start the native compilation by executing:

```bash
$ cd target/native-sources
$ native-image $(cat native-image.args)
...
$ ls
native-image.args
getting-started-1.0.0-SNAPSHOT-runner
getting-started-1.0.0-SNAPSHOT-runner.build_artifacts.txt
getting-started-1.0.0-SNAPSHOT-runner.jar
```

The process for Gradle is analogous.

Running the build process in a container is also possible:

```bash
$ ./mvnw clean package -Dquarkus.native.enabled=true -Dquarkus.native.sources-only=true -Dquarkus.native.container-build=true
```

`-Dquarkus.native.container-build=true` will produce an additional text file named `native-builder.image` holding the docker image name to be used to build the native image.

```bash
cd target/native-sources
docker run \
  -it \
  --user $(id -ur):$(id -gr) \
  --rm \
  -v $(pwd):/work \# <1>
  -w /work \# <2>
  --entrypoint /bin/sh \
  $(cat native-builder.image) \# <3>
  -c "native-image $(cat native-image.args) -J-Xmx4g"# <4>
```

1. Mount the host's directory `target/native-image` to the container's `/work`. Thus, the generated binary will also be written to this directory.
2. Switch the working directory to `/work`, which we have mounted in \<1>.
3. Use the docker image from the file `native-builder.image`.
4. Call `native-image` with the content of file `native-image.args` as arguments. We also supply an additional argument to limit the process's maximum memory to 4 Gigabytes (this may vary depending on the project being built and the machine building it).

> [!WARNING]
> If you are running on a Windows machine, please keep in mind that the binary was created within a Linux docker container. Hence, the binary will not be executable on the host Windows machine.

A high level overview of what the various steps of a CI/CD pipeline would look is the following:

1. Register the output of the step executing `./mvnw ...` command (i.e. directory `target/native-image`) as a build artifact,
2. Require this artifact in the step executing the `native-image ...` command, and
3. Register the output of the step executing the `native-image ...` command (i.e. files matching `target/*runner`) as build artifact.

The environment executing step `1` only needs Java and Maven (or Gradle) installed, while the environment executing step `3` only needs a GraalVM installation (including the `native-image` feature).

Depending on what the final desired output of the CI/CD pipeline is, the generated binary might then be used to create a container image.

## Debugging native executable

Native executables can be debugged using tools such as `gdb`. For this to be possible native executables need to be generated with debug symbols.

> [!NOTE]
> Debug symbol generation is only supported on Linux. Windows support is still under development, while macOS is not supported.

To generate debug symbols, add `-Dquarkus.native.debug.enabled=true` flag when generating the native executable. You will find the debug symbols for the native executable in a `.debug` file next to the native executable.

> [!NOTE]
> The generation of the `.debug` file depends on `objcopy`. As a result, when using a local GraalVM installation on common Linux distributions, you will need to install the `binutils` package:
>
> ```bash
> # dnf (rpm-based)
> sudo dnf install binutils
> # Debian-based distributions
> sudo apt-get install binutils
> ```
>
> When `objcopy` is not available, debug symbols are embedded in the executable.

Aside from debug symbols, setting `-Dquarkus.native.debug.enabled=true` flag generates a cache of source files for any JDK runtime classes, GraalVM classes, and application classes resolved during native executable generation. This source cache is useful for native debugging tools, to establish the link between the symbols and the matching source code. It provides a convenient way of making just the necessary sources available to the debugger/IDE when debugging a native executable.

Sources for third party jar dependencies, including Quarkus source code, are not added to the source cache by default. To include those, make sure you invoke `mvn dependency:sources` first. This step is required in order to pull the sources for these dependencies, and get them included in the source cache.

The source cache is located in the `target/sources` folder.

> [!TIP]
> If running `gdb` from a different directory than `target`, then the sources can be loaded by running:
>
> ```bash
> directory path/to/target
> ```
>
> in the `gdb` prompt.
>
> Or start `gdb` with:
>
> ```bash
> gdb -ex 'directory path/to/target' path/to/target/{project.name}-{project.version}-runner
> ```
>
> e.g.,
>
> ```bash
> gdb -ex 'directory ./target' ./target/getting-started-1.0.0-SNAPSHOT-runner
> ```

For a more detailed guide about debugging native images please refer to the [Native Reference Guide](/guides/native-reference/index.md).

## Using Monitoring Options

Monitoring options such as JDK flight recorder, jvmstat, heap dumps, NMT (starting with Mandrel 24.1 for JDK 23), and remote JMX can be added to the native executable build. Simply supply a comma separated list of the monitoring options you wish to include at build time.

```bash
-Dquarkus.native.monitoring=<comma separated list of options>
```

| Monitoring Option | Description | Availability As Of |
| --- | --- | --- |
| jcmd | Include JCMD support | GraalVM CE for JDK 24 Mandrel 24.2 |
| jfr | Include JDK Flight Recorder support | GraalVM CE 21.3 Mandrel 21.3 |
| jvmstat | Adds jvmstat support | GraalVM 22.3, GraalVM CE 17.0.7 Mandrel 22.3 Mandrel 23.0 (17.0.7) |
| heapdump | Adds support for generating heap dumps | GraalVM 22.3, GraalVM CE 17.0.7 Mandrel 22.3 Mandrel 23.0 (17.0.7) |
| jmxclient | Adds support for connections to JMX servers. | GraalVM for JDK 17/20 Mandrel 23.0 |
| jmxserver | Adds support for accepting connections from JMX clients. | GraalVM for JDK 17/20 Mandrel 23.0 (17.0.7) |
| nmt | Adds support for native memory tracking. | GraalVM for JDK 23 Mandrel 24.1 |
| threaddump | Adds support for dumping thread stacks on SIGQUIT/SIGBREAK. | GraalVM for JDK 22 Mandrel 24.0 |
| none | Disables support for all monitoring options that would be enabled by default in Quarkus | Pseudo option used by Quarkus |
| all | Adds all monitoring options. | GraalVM 22.3, GraalVM CE 17.0.7 Mandrel 22.3 Mandrel 23.0 (17.0.7) |

Please see the Quarkus Native Reference Guide for more detailed information on these monitoring options.

## Creating a Native Image Bundle

A native image bundle containing everything required to build a native image can be generated. This is useful to inspect the build inputs and configuration.

The generation of the bundle can be activated by setting any of the following parameters:

| Parameter | Description | Default |
| --- | --- | --- |
| quarkus.native.bundle.enabled | Enables the native-image bundle generation | `false` |
| quarkus.native.bundle.dry-run | Generates the bundle without building the native executable | `false` |
| quarkus.native.bundle.name | Sets the name of the bundle |  |

To use this feature, run:

```bash
./mvnw package -Dnative -Dquarkus.native.bundle.enabled=true
```

This command will generate all the files and configuration needed for the native image build in `target/{project.name}-{project.version}-runner.nib`.

> [!NOTE]
> More details on the GraalVM native-image bundle are available in the [GraalVM documentation](https://www.graalvm.org/jdk21/reference-manual/native-image/overview/Bundles).

## Configuring the Native Executable

There are a lot of different configuration options that can affect how the native executable is generated. These are provided in `application.properties` the same as any other config property.

The properties are shown below:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-core_quarkus-native-enabled"></a> [`quarkus.native.enabled`](#quarkus-core_quarkus-native-enabled) config_property_copy_button:+++quarkus.native.enabled+++[]<br><br>Set to enable native-image building using GraalVM.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-bundle-enabled"></a> [`quarkus.native.bundle.enabled`](#quarkus-core_quarkus-native-bundle-enabled) config_property_copy_button:+++quarkus.native.bundle.enabled+++[]<br><br>Set to enable native-image bundle generation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_BUNDLE_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-core_quarkus-native-bundle-dry-run"></a> [`quarkus.native.bundle.dry-run`](#quarkus-core_quarkus-native-bundle-dry-run) config_property_copy_button:+++quarkus.native.bundle.dry-run+++[]<br><br>Generates the native-image bundle through a dry-run build, skipping the actual native-image build.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_BUNDLE_DRY_RUN+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-bundle-name"></a> [`quarkus.native.bundle.name`](#quarkus-core_quarkus-native-bundle-name) config_property_copy_button:+++quarkus.native.bundle.name+++[]<br><br>Set to define the native-image bundle name. If not set the default name will match the native-executable's name suffixed by `.nib`. i.e. `{project.name}-{project.version}-runner.nib`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_BUNDLE_NAME+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-native-sources-only"></a> [`quarkus.native.sources-only`](#quarkus-core_quarkus-native-sources-only) config_property_copy_button:+++quarkus.native.sources-only+++[]<br><br>Set to prevent the native-image process from actually building the native image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_SOURCES_ONLY+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-additional-build-args"></a> [`quarkus.native.additional-build-args`](#quarkus-core_quarkus-native-additional-build-args) config_property_copy_button:+++quarkus.native.additional-build-args+++[]<br><br>Comma-separated, additional arguments to pass to the build process. If an argument includes the `,` symbol, it needs to be escaped, e.g. `\\,`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ADDITIONAL_BUILD_ARGS+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-native-additional-build-args-append"></a> [`quarkus.native.additional-build-args-append`](#quarkus-core_quarkus-native-additional-build-args-append) config_property_copy_button:+++quarkus.native.additional-build-args-append+++[]<br><br>Comma-separated, additional arguments to pass to the build process. The arguments are appended to those provided through `additional-build-args()`, as a result they may override those passed through `additional-build-args()`. By convention, this is meant to be set on the command-line, while `additional-build-args()` should be preferred for use in properties files. If an argument includes the `,` symbol, it needs to be escaped, e.g. `\\,`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ADDITIONAL_BUILD_ARGS_APPEND+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-native-enable-http-url-handler"></a> [`quarkus.native.enable-http-url-handler`](#quarkus-core_quarkus-native-enable-http-url-handler) config_property_copy_button:+++quarkus.native.enable-http-url-handler+++[]<br><br>If the HTTP url handler should be enabled, allowing you to do URL.openConnection() for HTTP URLs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_HTTP_URL_HANDLER+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-native-enable-https-url-handler"></a> [`quarkus.native.enable-https-url-handler`](#quarkus-core_quarkus-native-enable-https-url-handler) config_property_copy_button:+++quarkus.native.enable-https-url-handler+++[]<br><br>If the HTTPS url handler should be enabled, allowing you to do URL.openConnection() for HTTPS URLs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_HTTPS_URL_HANDLER+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-headless"></a> [`quarkus.native.headless`](#quarkus-core_quarkus-native-headless) config_property_copy_button:+++quarkus.native.headless+++[]<br><br>The default value for java.awt.headless JVM option. Switching this option affects linking of awt libraries.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_HEADLESS+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-native-file-encoding"></a> [`quarkus.native.file-encoding`](#quarkus-core_quarkus-native-file-encoding) config_property_copy_button:+++quarkus.native.file-encoding+++[]<br><br>Defines the file encoding as in `-Dfile.encoding=...`.<br><br>Native image runtime uses the host's (i.e. build time) value of `file.encoding` system property. We intentionally default this to UTF-8 to avoid platform specific defaults to be picked up which can then result in inconsistent behavior in the generated native executable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_FILE_ENCODING+++[] | string | `UTF-8` |
| [lock] <a id="quarkus-core_quarkus-native-add-all-charsets"></a> [`quarkus.native.add-all-charsets`](#quarkus-core_quarkus-native-add-all-charsets) config_property_copy_button:+++quarkus.native.add-all-charsets+++[]<br><br>If all character sets should be added to the native executable.<br><br>Note that some extensions (e.g. the Oracle JDBC driver) also take this setting into account to enable support for all charsets at the extension level.<br><br>This increases image size.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ADD_ALL_CHARSETS+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-graalvm-home"></a> [`quarkus.native.graalvm-home`](#quarkus-core_quarkus-native-graalvm-home) config_property_copy_button:+++quarkus.native.graalvm-home+++[]<br><br>The location of the Graal distribution<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_GRAALVM_HOME+++[] | string | `${GRAALVM_HOME:}` |
| [lock] <a id="quarkus-core_quarkus-native-java-home"></a> [`quarkus.native.java-home`](#quarkus-core_quarkus-native-java-home) config_property_copy_button:+++quarkus.native.java-home+++[]<br><br>The location of the JDK<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_JAVA_HOME+++[] | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | `${java.home}` |
| [lock] <a id="quarkus-core_quarkus-native-native-image-xmx"></a> [`quarkus.native.native-image-xmx`](#quarkus-core_quarkus-native-native-image-xmx) config_property_copy_button:+++quarkus.native.native-image-xmx+++[]<br><br>The maximum Java heap to be used during the native image generation<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_NATIVE_IMAGE_XMX+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-native-debug-build-process"></a> [`quarkus.native.debug-build-process`](#quarkus-core_quarkus-native-debug-build-process) config_property_copy_button:+++quarkus.native.debug-build-process+++[]<br><br>If the native image build should wait for a debugger to be attached before running. This is an advanced option and is generally only intended for those familiar with GraalVM internals<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_DEBUG_BUILD_PROCESS+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-publish-debug-build-process-port"></a> [`quarkus.native.publish-debug-build-process-port`](#quarkus-core_quarkus-native-publish-debug-build-process-port) config_property_copy_button:+++quarkus.native.publish-debug-build-process-port+++[]<br><br>If the debug port should be published when building with docker and debug-build-process is true<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_PUBLISH_DEBUG_BUILD_PROCESS_PORT+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-native-enable-isolates"></a> [`quarkus.native.enable-isolates`](#quarkus-core_quarkus-native-enable-isolates) config_property_copy_button:+++quarkus.native.enable-isolates+++[]<br><br>If isolates should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_ISOLATES+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-native-enable-fallback-images"></a> [`quarkus.native.enable-fallback-images`](#quarkus-core_quarkus-native-enable-fallback-images) config_property_copy_button:+++quarkus.native.enable-fallback-images+++[]<br><br>If a JVM based 'fallback image' should be created if native image fails. This is not recommended, as this is functionally the same as just running the application in a JVM<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_FALLBACK_IMAGES+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-auto-service-loader-registration"></a> [`quarkus.native.auto-service-loader-registration`](#quarkus-core_quarkus-native-auto-service-loader-registration) config_property_copy_button:+++quarkus.native.auto-service-loader-registration+++[]<br><br>If all META-INF/services entries should be automatically registered<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_AUTO_SERVICE_LOADER_REGISTRATION+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-dump-proxies"></a> [`quarkus.native.dump-proxies`](#quarkus-core_quarkus-native-dump-proxies) config_property_copy_button:+++quarkus.native.dump-proxies+++[]<br><br>If the bytecode of all proxies should be dumped for inspection<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_DUMP_PROXIES+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-container-build"></a> [`quarkus.native.container-build`](#quarkus-core_quarkus-native-container-build) config_property_copy_button:+++quarkus.native.container-build+++[]<br><br>If this build should be done using a container runtime. Unless container-runtime is also set, docker will be used by default. If docker is not available or is an alias to podman, podman will be used instead as the default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_CONTAINER_BUILD+++[] | boolean |  |
| [lock] <a id="quarkus-core_quarkus-native-pie"></a> [`quarkus.native.pie`](#quarkus-core_quarkus-native-pie) config_property_copy_button:+++quarkus.native.pie+++[]<br><br>Explicit configuration option to generate a native Position Independent Executable (PIE) for Linux. If the system supports PIE generation, the default behaviour is to disable it for [performance reasons](https://www.redhat.com/en/blog/position-independent-executable-pie-performance). However, some systems can only run position-independent executables, so this option enables the generation of such native executables.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_PIE+++[] | boolean |  |
| [lock] <a id="quarkus-core_quarkus-native-march"></a> [`quarkus.native.march`](#quarkus-core_quarkus-native-march) config_property_copy_button:+++quarkus.native.march+++[]<br><br>Generate instructions for a specific machine type. Defaults to `x86-64-v3` on AMD64 and `armv8-a` on AArch64. Use `compatibility` for best compatibility, or `native` for best performance if a native executable is deployed on the same machine or on a machine with the same CPU features. A list of all available machine types is available by executing `native-image -march=list`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_MARCH+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-native-remote-container-build"></a> [`quarkus.native.remote-container-build`](#quarkus-core_quarkus-native-remote-container-build) config_property_copy_button:+++quarkus.native.remote-container-build+++[]<br><br>If this build is done using a remote docker daemon.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_REMOTE_CONTAINER_BUILD+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-builder-image"></a> [`quarkus.native.builder-image`](#quarkus-core_quarkus-native-builder-image) config_property_copy_button:+++quarkus.native.builder-image+++[]<br><br>The docker image to use to do the image build. It can be one of `graalvm`, `mandrel`, or the full image path, e.g. `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21`.<br><br>**Note:** Builder images are available using UBI 8 and UBI 9 base images, for example:<br><br>\* UBI 8: `quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21` (UBI 8) \* UBI 9: `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21` (UBI 9)<br><br>You need to be aware that if you use a builder image using UBI9 and you plan to build a container, you must ensure that the base image used in the container is also UBI9.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_BUILDER_IMAGE+++[] | string | `mandrel` |
| [lock] <a id="quarkus-core_quarkus-native-builder-image-pull"></a> [`quarkus.native.builder-image.pull`](#quarkus-core_quarkus-native-builder-image-pull) config_property_copy_button:+++quarkus.native.builder-image.pull+++[]<br><br>The strategy for pulling the builder image during the build.<br><br>Defaults to 'always', which will always pull the most up-to-date image; useful to keep up with fixes when a (floating) tag is updated.<br><br>Use 'missing' to only pull if there is no image locally; useful on development environments where building with out-of-date images is acceptable and bandwidth may be limited.<br><br>Use 'never' to fail the build if there is no image locally.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_BUILDER_IMAGE_PULL+++[] | tooltip:always[Always pull the most recent image.], tooltip:missing[Only pull the image if it's missing locally.], tooltip:never[Never pull any image; fail if the image is missing locally.] | tooltip:always[Always pull the most recent image.] |
| [lock] <a id="quarkus-core_quarkus-native-container-runtime"></a> [`quarkus.native.container-runtime`](#quarkus-core_quarkus-native-container-runtime) config_property_copy_button:+++quarkus.native.container-runtime+++[]<br><br>The container runtime (e.g. docker) that is used to do an image based build. If this is set then a container build is always done.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_CONTAINER_RUNTIME+++[] | `docker`, `docker-rootless`, `wsl`, `wsl-rootless`, `podman`, `podman-rootless`, `unavailable` |  |
| [lock] <a id="quarkus-core_quarkus-native-container-runtime-options"></a> [`quarkus.native.container-runtime-options`](#quarkus-core_quarkus-native-container-runtime-options) config_property_copy_button:+++quarkus.native.container-runtime-options+++[]<br><br>Options to pass to the container runtime<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_CONTAINER_RUNTIME_OPTIONS+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-native-enable-vm-inspection"></a> <mark>link:#quarkus-core_quarkus-native-enable-vm-inspection\[`quarkus.native.enable-vm-inspection`\]</mark> config_property_copy_button:+++quarkus.native.enable-vm-inspection+++[]<br><br>*This property is deprecated: Use `quarkus.native.monitoring` instead.*<br><br>If the resulting image should allow VM introspection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_VM_INSPECTION+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-monitoring"></a> [`quarkus.native.monitoring`](#quarkus-core_quarkus-native-monitoring) config_property_copy_button:+++quarkus.native.monitoring+++[]<br><br>Enable monitoring various monitoring options. The value should be comma separated.<br><br>\* `jfr` for JDK flight recorder support \* `jcmd` for JCMD support \* `jvmstat` for JVMStat support \* `heapdump` for heapdump support \* `jmxclient` for JMX client support (experimental) \* `jmxserver` for JMX server support (experimental) \* `nmt` for native memory tracking support \* `threaddump` for thread dumping on SIGBREAK/SIGQUIT support \* `all` for all monitoring features \* `none` for explicitly turning off all monitoring features<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_MONITORING+++[] | list of tooltip:heapdump[Heapdump support.], tooltip:jcmd[JCMD support.], tooltip:jvmstat[JVMStat support.], tooltip:jfr[JDK flight recorder support.], tooltip:jmxserver[JMX server support (experimental).], tooltip:jmxclient[JMX client support (experimental).], tooltip:nmt[Native memory tracking support.], tooltip:threaddump[Thread dumping support.], tooltip:all[All monitoring features.], tooltip:none[Explicitly turns off all monitoring features.] |  |
| [lock] <a id="quarkus-core_quarkus-native-enable-reports"></a> [`quarkus.native.enable-reports`](#quarkus-core_quarkus-native-enable-reports) config_property_copy_button:+++quarkus.native.enable-reports+++[]<br><br>If the reports on call paths and included packages/classes/methods should be generated<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_REPORTS+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-report-exception-stack-traces"></a> [`quarkus.native.report-exception-stack-traces`](#quarkus-core_quarkus-native-report-exception-stack-traces) config_property_copy_button:+++quarkus.native.report-exception-stack-traces+++[]<br><br>If exceptions should be reported with a full stack trace<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_REPORT_EXCEPTION_STACK_TRACES+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-native-report-errors-at-runtime"></a> [`quarkus.native.report-errors-at-runtime`](#quarkus-core_quarkus-native-report-errors-at-runtime) config_property_copy_button:+++quarkus.native.report-errors-at-runtime+++[]<br><br>If errors should be reported at runtime. This is a more relaxed setting, however it is not recommended as it means your application may fail at runtime if an unsupported feature is used by accident.<br><br>Note that the use of this flag may result in build time failures due to `ClassNotFoundException`s. Reason most likely being that the Quarkus extension already optimized it away or do not actually need it. In such cases you should explicitly add the corresponding dependency providing the missing classes as a dependency to your project.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_REPORT_ERRORS_AT_RUNTIME+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-reuse-existing"></a> [`quarkus.native.reuse-existing`](#quarkus-core_quarkus-native-reuse-existing) config_property_copy_button:+++quarkus.native.reuse-existing+++[]<br><br>Don't build a native image if it already exists.<br><br>This is useful if you have already built an image and you want to use Quarkus to deploy it somewhere.<br><br>Note that this is not able to detect if the existing image is outdated, if you have modified source or config and want a new image you must not use this flag.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_REUSE_EXISTING+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-resources-includes"></a> [`quarkus.native.resources.includes`](#quarkus-core_quarkus-native-resources-includes) config_property_copy_button:+++quarkus.native.resources.includes+++[]<br><br>A comma separated list of globs to match resource paths that should be added to the native image.<br><br>Use slash (`/`) as a path separator on all platforms. Globs must not start with slash.<br><br>By default, no resources are included.<br><br>Example: Given that you have `src/main/resources/ignored.png` and `src/main/resources/foo/selected.png` in your source tree and one of your dependency JARs contains `bar/some.txt` file, with the following configuration<br><br>```<br>quarkus.native.resources.includes = foo/**,bar/**/*.txt<br>```<br><br>the files `src/main/resources/foo/selected.png` and `bar/some.txt` will be included in the native image, while `src/main/resources/ignored.png` will not be included.<br><br>Supported glob features<br><br>!=== !Feature !Description<br><br>!`*` !Matches a (possibly empty) sequence of characters that does not contain slash (`/`)<br><br>!`**` !Matches a (possibly empty) sequence of characters that may contain slash (`/`)<br><br>!`?` !Matches one character, but not slash<br><br>!`[abc]` !Matches one character given in the bracket, but not slash<br><br>!`[a-z]` !Matches one character from the range given in the bracket, but not slash<br><br>!`[\!abc]` !Matches one character not named in the bracket; does not match slash<br><br>!`[a-z]` !Matches one character outside the range given in the bracket; does not match slash<br><br>!`{one,two,three}` !Matches any of the alternating tokens separated by comma; the tokens may contain wildcards, nested alternations and ranges<br><br>!`\` !The escape character !===<br><br>Note that there are three levels of escaping when passing this option via `application.properties`:<br><br>. `application.properties` parser . MicroProfile Config list converter that splits the comma separated list . Glob parser<br><br>All three levels use backslash (`\`) as the escaping character. So you need to use an appropriate number of backslashes depending on which level you want to escape.<br><br>Note that Quarkus extensions typically include the resources they require by themselves. This option is useful in situations when the built-in functionality is not sufficient.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_RESOURCES_INCLUDES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-native-resources-excludes"></a> <mark>link:#quarkus-core_quarkus-native-resources-excludes\[`quarkus.native.resources.excludes`\]</mark> config_property_copy_button:+++quarkus.native.resources.excludes+++[]<br><br>*This property is deprecated since `3.29`: Excluding resources is not supported in the new reachability-metadata.json file used with Mandrel/GraalVM 25.0 and onwards. Quarkus plans to adopt the use of reachability-metadata.json for Mandrel/GraalVM 23.1 for JDK 21 as well (see <https://github.com/quarkusio/quarkus/issues/41016>).*<br><br>A comma separated list of globs to match resource paths that should **not** be added to the native image.<br><br>Use slash (`/`) as a path separator on all platforms. Globs must not start with slash.<br><br>Please refer to `includes` for details about the glob syntax.<br><br>By default, no resources are excluded.<br><br>Example: Given that you have `src/main/resources/red.png` and `src/main/resources/foo/green.png` in your source tree and one of your dependency JARs contains `bar/blue.png` file, with the following configuration<br><br>```<br>quarkus.native.resources.includes = **/*.png<br>quarkus.native.resources.excludes = foo/**,**/green.png<br>```<br><br>the resource `red.png` will be available in the native image while the resources `foo/green.png` and `bar/blue.png` will not be available in the native image.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_RESOURCES_EXCLUDES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-native-debug-enabled"></a> [`quarkus.native.debug.enabled`](#quarkus-core_quarkus-native-debug-enabled) config_property_copy_button:+++quarkus.native.debug.enabled+++[]<br><br>If debug is enabled and debug symbols are generated. The symbols will be generated in a separate .debug file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_DEBUG_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-enable-dashboard-dump"></a> [`quarkus.native.enable-dashboard-dump`](#quarkus-core_quarkus-native-enable-dashboard-dump) config_property_copy_button:+++quarkus.native.enable-dashboard-dump+++[]<br><br>Generate the report files for GraalVM Dashboard.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_ENABLE_DASHBOARD_DUMP+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-include-reasons-in-config-files"></a> [`quarkus.native.include-reasons-in-config-files`](#quarkus-core_quarkus-native-include-reasons-in-config-files) config_property_copy_button:+++quarkus.native.include-reasons-in-config-files+++[]<br><br>Include a reasons entries in the generated json configuration files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_INCLUDE_REASONS_IN_CONFIG_FILES+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-compression-enabled"></a> [`quarkus.native.compression.enabled`](#quarkus-core_quarkus-native-compression-enabled) config_property_copy_button:+++quarkus.native.compression.enabled+++[]<br><br>Whether compression should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_COMPRESSION_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-native-compression-container-build"></a> [`quarkus.native.compression.container-build`](#quarkus-core_quarkus-native-compression-container-build) config_property_copy_button:+++quarkus.native.compression.container-build+++[]<br><br>Whether the compression should be executed within a container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_COMPRESSION_CONTAINER_BUILD+++[] | boolean |  |
| [lock] <a id="quarkus-core_quarkus-native-compression-container-image"></a> [`quarkus.native.compression.container-image`](#quarkus-core_quarkus-native-compression-container-image) config_property_copy_button:+++quarkus.native.compression.container-image+++[]<br><br>The image used for compression. Defaults to `quarkus.native.builder-image` if not set.<br><br>Setting this variable will automatically activate<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_COMPRESSION_CONTAINER_IMAGE+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-native-compression-level"></a> [`quarkus.native.compression.level`](#quarkus-core_quarkus-native-compression-level) config_property_copy_button:+++quarkus.native.compression.level+++[]<br><br>The compression level in ++\[++1, 10++\]++. 10 means *best*.<br><br>Higher compression level requires more time to compress the executable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_COMPRESSION_LEVEL+++[] | int |  |
| [lock] <a id="quarkus-core_quarkus-native-compression-additional-args"></a> [`quarkus.native.compression.additional-args`](#quarkus-core_quarkus-native-compression-additional-args) config_property_copy_button:+++quarkus.native.compression.additional-args+++[]<br><br>Allows passing extra arguments to the UPX command line (like --brute). The arguments are comma-separated.<br><br>The exhaustive list of parameters can be found in <https://github.com/upx/upx/blob/devel/doc/upx.pod>.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_COMPRESSION_ADDITIONAL_ARGS+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-native-agent-configuration-apply"></a> [`quarkus.native.agent-configuration-apply`](#quarkus-core_quarkus-native-agent-configuration-apply) config_property_copy_button:+++quarkus.native.agent-configuration-apply+++[]<br><br>Configuration files generated by the Quarkus build, using native image agent, are informative by default. In other words, the generated configuration files are presented in the build log but are not applied. When this option is set to true, generated configuration files are applied to the native executable building process.<br><br>Enabling this option should be done with care, because it can make native image configuration and/or behaviour dependant on other non-obvious factors. For example, if the native image agent generated configuration was generated from running JVM unit tests, disabling test(s) can result in a different native image configuration being generated, which in turn can misconfigure the native executable or affect its behaviour in unintended ways.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_AGENT_CONFIGURATION_APPLY+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-native-pgo-enabled"></a> [`quarkus.native.pgo.enabled`](#quarkus-core_quarkus-native-pgo-enabled) config_property_copy_button:+++quarkus.native.pgo.enabled+++[]<br><br>Enable Profile-Guided Optimization for native images.<br><br>Requires Oracle GraalVM. When enabled, the native build produces an instrumented binary. Running `@QuarkusIntegrationTest` generates a `.iprof` profile, and a post-integration-test Maven goal rebuilds with that profile to produce an optimized native binary.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_NATIVE_PGO_ENABLED+++[] | boolean | `false` |

## What's next?

This guide covered the creation of a native (binary) executable for your application. It provides an application exhibiting a swift startup time and consuming less memory. However, there is much more.

We recommend continuing the journey with the [deployment to Kubernetes and OpenShift](/guides/deploying-to-kubernetes/index.md).
