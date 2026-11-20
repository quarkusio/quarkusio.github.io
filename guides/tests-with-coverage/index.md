# Measuring the coverage of your tests

Learn how to measure the test coverage of your application. This guide covers:

- Measuring the coverage of your Unit Tests
- Measuring the coverage of your Integration Tests
- Separating the execution of your Unit Tests and Integration Tests
- Consolidating the coverage for all your tests

Please note that code coverage is not supported in native mode.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- Having completed the [Testing your application guide](/guides/getting-started-testing/index.md)

## Architecture

The application built in this guide is just a Jakarta REST endpoint (hello world) that relies on dependency injection to use a service. The service will be tested with JUnit and the endpoint will be annotated via a `@QuarkusTest` annotation.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example. Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `tests-with-coverage-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/tests-with-coverage-quickstart).

## Starting from a simple project and two tests

Let's start from an empty application created with the Quarkus Maven plugin:

**CLI**

```bash
quarkus create app org.acme:tests-with-coverage-quickstart \
    --extension='rest' \
    --no-code
cd tests-with-coverage-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=tests-with-coverage-quickstart \
    -Dextensions='rest' \
    -DnoCode
cd tests-with-coverage-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=tests-with-coverage-quickstart"`

Now we'll be adding all the elements necessary to have an application that is properly covered with tests.

First, a Jakarta REST resource serving a hello endpoint:

```java
package org.acme.testcoverage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    private final GreetingService service;

    @Inject
    public GreetingResource(GreetingService service) {
        this.service = service;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/{name}")
    public String greeting(String name) {
        return service.greeting(name);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
```

This endpoint uses a greeting service:

```java
package org.acme.testcoverage;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greeting(String name) {
        return "hello " + name;
    }

}
```

The project will also need a test:

```java
package org.acme.testcoverage;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("hello"));
    }

    @Test
    public void testGreetingEndpoint() {
        String uuid = UUID.randomUUID().toString();
        given()
          .pathParam("name", uuid)
          .when().get("/hello/greeting/{name}")
          .then()
            .statusCode(200)
            .body(is("hello " + uuid));
    }
}
```

## Setting up JaCoCo

Now we need to add JaCoCo to our project. To do this we need to add the following to the build file:

**pom.xml**

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jacoco</artifactId>
  <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-jacoco")
```

This Quarkus extension takes care of everything that would usually be done via the JaCoCo Maven plugin, so no additional config is required.

> [!WARNING]
> Using both the extension and the plugin requires special configuration, if you add both you will get lots of errors about classes already being instrumented. The configuration needed is detailed below.

## Working with multi-module projects

Up until `3.2`, `data-file` and `report-location` were always relative to the module's build output directory, which prevented from working with multi-module projects where you want to aggregate all coverages into a single parent directory. Starting in `3.3`, specifying a `data-file` or `report-location` will assume the path as is. Here is an example on how to set up the `surefire` plugin:

```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <systemPropertyVariables>
      <quarkus.jacoco.data-file>${maven.multiModuleProjectDirectory}/target/jacoco.exec</quarkus.jacoco.data-file>
      <quarkus.jacoco.reuse-data-file>true</quarkus.jacoco.reuse-data-file>
      <quarkus.jacoco.report-location>${maven.multiModuleProjectDirectory}/target/coverage</quarkus.jacoco.report-location>
    </systemPropertyVariables>
  </configuration>
</plugin
```

> [!WARNING]
> If you need to configure the `argLine` property of the Surefire plugin (e.g. for setting memory parameters), you need to use [Maven late property evaluation^](https://maven.apache.org/surefire/maven-surefire-plugin/faq.html#late-property-evaluation), otherwise the Jacoco agent will not be correctly added, and regular JUnit tests and Quarkus `ComponentTest` will not get covered. Example: `<argLine>@{argLine} -your -extra -arguments</argLine>`.

## Running the tests with coverage

Run `mvn verify`, the tests will be run and the results will end up in `target/jacoco-reports`. This is all that is needed, the `quarkus-jacoco` extension allows JaCoCo to just work out of the box.

There are some config options that affect this:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-enabled"></a> [`quarkus.jacoco.enabled`](#quarkus-jacoco_quarkus-jacoco-enabled) config_property_copy_button:+++quarkus.jacoco.enabled+++[]<br><br>Whether or not the Jacoco extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-data-file"></a> [`quarkus.jacoco.data-file`](#quarkus-jacoco_quarkus-jacoco-data-file) config_property_copy_button:+++quarkus.jacoco.data-file+++[]<br><br>The Jacoco data file. The path can be relative (to the project/module root) or absolute.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_DATA_FILE+++[] | string | `target/jacoco-quarkus.exec` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-reuse-data-file"></a> [`quarkus.jacoco.reuse-data-file`](#quarkus-jacoco_quarkus-jacoco-reuse-data-file) config_property_copy_button:+++quarkus.jacoco.reuse-data-file+++[]<br><br>Whether to reuse (`true`) or delete (`false`) the Jacoco data file on each run.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_REUSE_DATA_FILE+++[] | boolean | `false` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-report"></a> [`quarkus.jacoco.report`](#quarkus-jacoco_quarkus-jacoco-report) config_property_copy_button:+++quarkus.jacoco.report+++[]<br><br>If Quarkus should generate the Jacoco report<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_REPORT+++[] | boolean | `true` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-output-encoding"></a> [`quarkus.jacoco.output-encoding`](#quarkus-jacoco_quarkus-jacoco-output-encoding) config_property_copy_button:+++quarkus.jacoco.output-encoding+++[]<br><br>Encoding of the generated reports.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_OUTPUT_ENCODING+++[] | string | `UTF-8` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-title"></a> [`quarkus.jacoco.title`](#quarkus-jacoco_quarkus-jacoco-title) config_property_copy_button:+++quarkus.jacoco.title+++[]<br><br>Name of the root node HTML report pages.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_TITLE+++[] | string | `${quarkus.application.name}` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-footer"></a> [`quarkus.jacoco.footer`](#quarkus-jacoco_quarkus-jacoco-footer) config_property_copy_button:+++quarkus.jacoco.footer+++[]<br><br>Footer text used in HTML report pages.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_FOOTER+++[] | string |  |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-source-encoding"></a> [`quarkus.jacoco.source-encoding`](#quarkus-jacoco_quarkus-jacoco-source-encoding) config_property_copy_button:+++quarkus.jacoco.source-encoding+++[]<br><br>Encoding of the source files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_SOURCE_ENCODING+++[] | string | `UTF-8` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-includes"></a> [`quarkus.jacoco.includes`](#quarkus-jacoco_quarkus-jacoco-includes) config_property_copy_button:+++quarkus.jacoco.includes+++[]<br><br>A list of class files to include in the report. May use wildcard characters (++\*++ and ?). When not specified everything will be included.<br><br>For instance:<br><br>\* `**/fo/**/*` targets all classes under fo and sub packages \* `**/bar/*` targets all classes directly under bar \* `**/*BAR*.class` targets classes that contain BAR in their name regardless of path<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_INCLUDES+++[] | list of string | `**` |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-excludes"></a> [`quarkus.jacoco.excludes`](#quarkus-jacoco_quarkus-jacoco-excludes) config_property_copy_button:+++quarkus.jacoco.excludes+++[]<br><br>A list of class files to exclude from the report. May use wildcard characters (++\*++ and ?). When not specified nothing will be excluded.<br><br>For instance:<br><br>\* `**/fo/**/*` targets all classes under fo and sub packages \* `**/bar/*` targets all classes directly under bar \* `**/*BAR*.class` targets classes that contain BAR in their name regardless of path<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_EXCLUDES+++[] | list of string |  |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-report-location"></a> [`quarkus.jacoco.report-location`](#quarkus-jacoco_quarkus-jacoco-report-location) config_property_copy_button:+++quarkus.jacoco.report-location+++[]<br><br>The location of the report files. The path can be relative (to the project/module root) or absolute.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_REPORT_LOCATION+++[] | string | `target/jacoco-report` |
| <a id="quarkus-jacoco_section_quarkus-jacoco-instrument-artifacts"></a> [Artifacts that should be instrumented](#quarkus-jacoco_section_quarkus-jacoco-instrument-artifacts) | Type | Default |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-instrument-artifacts-dependency-name-group-id"></a> [`quarkus.jacoco.instrument-artifacts."dependency-name".group-id`](#quarkus-jacoco_quarkus-jacoco-instrument-artifacts-dependency-name-group-id) config_property_copy_button:+++quarkus.jacoco.instrument-artifacts."dependency-name".group-id+++[]<br><br>The maven groupId of the artifact.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_INSTRUMENT_ARTIFACTS__DEPENDENCY_NAME__GROUP_ID+++[] | string | required [exclamation circle] |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-instrument-artifacts-dependency-name-artifact-id"></a> [`quarkus.jacoco.instrument-artifacts."dependency-name".artifact-id`](#quarkus-jacoco_quarkus-jacoco-instrument-artifacts-dependency-name-artifact-id) config_property_copy_button:+++quarkus.jacoco.instrument-artifacts."dependency-name".artifact-id+++[]<br><br>The maven artifactId of the artifact (optional).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_INSTRUMENT_ARTIFACTS__DEPENDENCY_NAME__ARTIFACT_ID+++[] | string |  |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-instrument-artifacts-dependency-name-classifier"></a> [`quarkus.jacoco.instrument-artifacts."dependency-name".classifier`](#quarkus-jacoco_quarkus-jacoco-instrument-artifacts-dependency-name-classifier) config_property_copy_button:+++quarkus.jacoco.instrument-artifacts."dependency-name".classifier+++[]<br><br>The maven classifier of the artifact (optional).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_INSTRUMENT_ARTIFACTS__DEPENDENCY_NAME__CLASSIFIER+++[] | string |  |
| [lock] <a id="quarkus-jacoco_quarkus-jacoco-aggregate-report-data"></a> [`quarkus.jacoco.aggregate-report-data`](#quarkus-jacoco_quarkus-jacoco-aggregate-report-data) config_property_copy_button:+++quarkus.jacoco.aggregate-report-data+++[]<br><br>If set to `true` then the report data (source directories and class files) are aggregated so that a single report can be generated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JACOCO_AGGREGATE_REPORT_DATA+++[] | boolean | `false` |

> [!TIP]
> When working with a multi-module project, then for code coverage to work properly, the upstream modules need to be properly [indexed](/guides/cdi-reference/index.md#bean_discovery).

## Coverage for tests not using @QuarkusTest

The Quarkus automatic JaCoCo config will only work for tests that are annotated with `@QuarkusTest`. If you want to check the coverage of other tests as well then you will need to fall back to the JaCoCo maven plugin.

In addition to including the `quarkus-jacoco` extension in your `pom.xml` you will need the following config:

**pom.xml**

```xml
<project>
    <build>
        <plugins>
            ...
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <executions>
                   <execution>
                      <id>default-prepare-agent</id>
                      <goals>
                           <goal>prepare-agent</goal>
                      </goals>
                      <configuration>
                        <exclClassLoaders>*QuarkusClassLoader</exclClassLoaders> <1>
                        <destFile>${project.build.directory}/jacoco-quarkus.exec</destFile>
                        <append>true</append>
                      </configuration>
                   </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

1. This config tells it to ignore `@QuarkusTest` related classes, as they are loaded by `QuarkusClassLoader`

**build.gradle**

```gradle
plugins {
    id 'jacoco' <1>
}

test {
    finalizedBy jacocoTestReport
    jacoco {
        excludeClassLoaders = ["*QuarkusClassLoader"] <2>
        destinationFile = layout.buildDirectory.file("jacoco-quarkus.exec").get().asFile <2>
    }
    jacocoTestReport.enabled = false <3>
}
```

1. Add the `jacoco` gradle plugin
2. This config tells it to ignore `@QuarkusTest` related classes, as they are loaded by `QuarkusClassLoader`
3. Set this config to `false` if you are also using the `quarkus-jacoco` extension and have at least one `@QuarkusTest`.  The default `jacocoTestReport` task can be skipped since `quarkus-jacoco` will generate the combined report of regular unit tests and `@QuarkusTest` classes since the execution data is recorded in the same file.

> [!WARNING]
> This config will only work if at least one `@QuarkusTest` is being run. If you are not using `@QuarkusTest` then you can simply use the JaCoCo plugin in the standard manner with no additional config.

### Coverage for Integration Tests

To get code coverage data from integration tests, the following requirements need to be met:

- The built artifact is a jar (and not a container or native binary).
- JaCoCo needs to be configured in your build tool.
- The application must have been built with `quarkus.package.write-transformed-bytecode-to-build-output` set to `true`

> [!WARNING]
> Setting `quarkus.package.write-transformed-bytecode-to-build-output=true` should be done with caution and only if subsequent builds are done in a clean environment - i.e. the build tool's output directory has been completely cleaned.

In the `pom.xml`, you can add the following plugin configuration for JaCoCo. This will append integration test data into the same destination file as unit tests, re-build the JaCoCo report after the integration tests are complete, and thus produce a comprehensive code-coverage report.

```xml
<build>
    ...
    <plugins>
        ...
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>${jacoco.version}</version>
            <executions>
                ... <1>

                <execution>
                    <id>default-prepare-agent-integration</id>
                    <goals>
                        <goal>prepare-agent-integration</goal>
                    </goals>
                    <configuration>
                        <destFile>${project.build.directory}/jacoco-quarkus.exec</destFile>
                        <append>true</append>
                    </configuration>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>post-integration-test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                    <configuration>
                        <dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>
                        <outputDirectory>${project.build.directory}/jacoco-report</outputDirectory>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        ...
    </plugins>
    ...
</build>
```

1. All executions should be in the same `<plugin>` definition so make sure you concatenate all of them.

In order to run the integration tests as a jar with the JaCoCo agent, add the following to your `pom.xml`.

```xml
<build>
    ...
    <plugins>
        ...
        <plugin>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>${surefire-plugin.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                    <configuration>
                        <systemPropertyVariables>
                            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                            <maven.home>${maven.home}</maven.home>
                            <quarkus.test.arg-line>${argLine}</quarkus.test.arg-line>
                        </systemPropertyVariables>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        ...
    </plugins>
    ...
</build>

```

> [!WARNING]
> Sharing the same value for `quarkus.test.arg-line` might break integration test runs that test different types of Quarkus artifacts. In such cases, the use of Maven profiles is advised.

## Setting coverage thresholds

You can set thresholds for code coverage using the JaCoCo Maven plugin. Note the element `<dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>`. You must set it matching your choice for `quarkus.jacoco.data-file`.

**pom.xml**

```xml
<build>
    ...
    <plugins>
        ...
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>${jacoco.version}</version>
            <executions>
                ... <1>

                <execution>
                    <id>jacoco-check</id>
                    <goals>
                        <goal>check</goal>
                    </goals>
                    <phase>post-integration-test</phase>
                    <configuration>
                        <dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>
                        <rules>
                            <rule>
                                <element>BUNDLE</element>
                                <limits>
                                    <limit>
                                        <counter>LINE</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.8</minimum>
                                    </limit>
                                    <limit>
                                        <counter>BRANCH</counter>
                                        <value>COVEREDRATIO</value>
                                        <minimum>0.72</minimum>
                                    </limit>
                                </limits>
                            </rule>
                        </rules>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        ...
    </plugins>
    ...
</build>
```

1. All executions should be in the same `<plugin>` definition so make sure you concatenate all of them.

**build.gradle**

```gradle
jacocoTestCoverageVerification {
    executionData.setFrom("$project.buildDir/jacoco-quarkus.exec")
    violationRules {
        rule {
            limit {
                counter = 'INSTRUCTION'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 0.72
            }
        }
    }
}
check.dependsOn jacocoTestCoverageVerification
```

Excluding classes from the verification task can be configured as following:

```gradle
jacocoTestCoverageVerification {
    afterEvaluate { <1>
        classDirectories.setFrom(files(classDirectories.files.collect { <2>
            fileTree(dir: it, exclude: [
                    "org/example/package/**/*" <3>
            ])
        }))
    }
}
```

1. `classDirectories` needs to be read after evaluation phase in Gradle
2. Currently, there is a bug in Gradle JaCoCo which requires the `excludes` to be specified in this manner - <https://github.com/gradle/gradle/issues/14760>.  Once this issue is fixed, excludes
3. Exclude all classes in `org/example/package` package

## Conclusion

You now have all the information you need to study the coverage of your tests! But remember, some code that is not covered is certainly not well tested. But some code that is covered is not necessarily **well** tested. Make sure to write good tests!
