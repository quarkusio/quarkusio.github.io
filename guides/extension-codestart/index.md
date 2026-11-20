# Extension codestart

This guide explains how to create and configure a Quarkus Codestart for an extension.

## Description

"Extension Codestarts" is the name we give to our Quarkus extension "getting started" code generation system. It aims to provide a personalized getting started experience with Quarkus. A Quarkus extension is able to provide one or more well-defined codestarts which will contain the resources and code required/recommended to start using that particular extension.

Extension codestarts are applied by default when using the Quarkus tooling (if the chosen extensions contain any):

- [code.quarkus.io](https://code.quarkus.io) (find the extensions tagged with \[code\])
- The Quarkus Maven plugin:

  ```bash
  mvn io.quarkus.platform:quarkus-maven-plugin:create
  ```


- The Quarkus CLI:

  ```bash
  quarkus create app
  ```


## How it works

When starting a project, you choose the language, the build tool, the framework, then you add Dockerfiles, CI, dependencies and code.

Codestarts are working the same way when contributing to the generation of a project, they are split in two categories:

**The "Base" codestarts (you choose a combination of those):**

- project: The project skeleton (e.g. a Quarkus project)
- buildtool: The build tool (e.g. Maven, Gradle, Gradle with Kotlin DSL)
- language: The coding language (e.g. Java, Kotlin, Scala)
- config: The config type (e.g. yaml, properties)

**Extra codestarts (as much as wanted, added on top of the base ones):**

- tooling: Anything that can be added to improve the project (e.g. Dockerfiles, GitHub Actions workflows)
- code: Any Quarkus extension can provide starter code. The user can decide to activate it or not.

Each codestart consists of:

1. A codestart unique name, ie `my-codestart`
2. A directory for the codestart files, ie `my-codestart/`
3. A `codestart.yml` file
4. Optionally some templates that are following a common structure and naming conventions

## Where are the Quarkus Extension Codestarts located

- In the Quarkus core repository, the extension codestarts are all in the same [module](https://github.com/quarkusio/quarkus/tree/main/devtools/project-core-extension-codestarts/src/main/resources/codestarts/quarkus/extension-codestarts).

- Quarkus REST (formerly RESTEasy Reactive), RESTEasy and Spring Web extension codestarts are also part of [the same module](https://github.com/quarkusio/quarkus/tree/main/devtools/project-core-extension-codestarts/src/main/resources/codestarts/quarkus/extension-codestarts).

- For other extensions, the codestart will typically be located in the runtime module (with special instruction in the `pom.xml` to generate a separate codestart artifact).

## Base codestarts

The [base codestarts](https://github.com/quarkusio/quarkus/tree/main/independent-projects/tools/base-codestarts/src/main/resources/codestarts/quarkus) contains templates to create project, buildtool, languages, config and tooling files.

In addition, Quarkus also provides the following ways to initialize a new extension project with a Codestart:

**CLI**

To create a new extension with a Codestart skeleton provide the `--codestart` flag to the `create extension` command:

```bash
quarkus create extension --codestart org.acme:greeting-extension
```

```shell
Detected layout type is 'standalone'
Generated runtime artifactId is 'greeting-extension'


applying codestarts...
📚  java
🔨  maven
📦  quarkus-extension
🚀  devmode-test
🚀  extension-base
🚀  extension-codestart
🚀  integration-tests
🚀  unit-test

-----------
 👍  extension has been successfully generated in:
--> /Users/.../greeting-extension
-----------
Navigate into this directory and get started: quarkus build
```

*For more information about how to install the Quarkus CLI and use it, please refer to [the Quarkus CLI guide](/guides/cli-tooling/index.md).*

**Maven**

Quarkus provides the [`create-extension` Maven Mojo](/guides/building-my-first-extension/index.md#maven-setup) to initialize an extension project.

To generate a new extension with a Codestart skeleton provide the `-DwithCodestart` flag to this Mojo:

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create-extension -N \
    -DgroupId=org.acme \
    -DextensionId=greeting-extension \
    -DwithCodestart
```

```shell
[INFO] --- quarkus-maven-plugin:3.40.1.2:create-extension (default-cli) @ standalone-pom ---

Detected layout type is 'standalone'
Generated runtime artifactId is 'greeting-extension'


applying codestarts...
📚  java
🔨  maven
📦  quarkus-extension
🚀  devmode-test
🚀  extension-base
🚀  extension-codestart
🚀  integration-tests
🚀  unit-test

-----------
 👍  extension has been successfully generated in:
--> /Users/.../greeting-extension
-----------
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.638 s
[INFO] Finished at: 2022-10-24T21:27:51+02:00
[INFO] ------------------------------------------------------------------------
```

## Writing an Extension Codestart

Here is a step-by-step guide to write an extension codestart. You may also watch the [Quarkus Insight #99](https://www.youtube.com/watch?v=lLyVDqVK8cE) with a live-coding session.

As was mentioned previously, the base project files (pom.xml, Dockerfiles, ...) are already generated by the base codestarts provided by the Quarkus core. Thanks to this, we can only focus on the starter code specific to the extension.

Let's take `io.quarkiverse.aloha:quarkus-aloha` as an example extension GAV (don't look for this extension, it doesn't exist).

### The code

A Codestart is a template for scaffolding new project.

In this tutorial a Codestart project is created from a Quarkus project and adding the needed templates.

Therefore, go to [code.quarkus.io](https://code.quarkus.io), create a new project with the aloha extension and `org.acme` as Group (i.e [org.acme placeholder for package name](#org-acme-package)). Prepare a nice starter. It should not include any business logic, instead, it should contain some stub data/hello world that compiles and gives an overview of how to use the extension. The idea is to bring code that is the most common starting point for the extension.

Happy with the code? Let's make a Codestart out of it.

### The Codestart (Quarkiverse or Standalone extensions)

In your extension:

- Create the `runtime/src/main/codestarts/quarkus/aloha-codestart` directory

- Move the `src/main/java` from your generated project to `runtime/src/main/codestarts/quarkus/aloha-codestart/java/src/main/java`

- (Optional) Move the config using this convention: [application config application.yml](#app-config).

- Create a [codestart.yml](#codestart-yml) file in `runtime/src/main/codestarts/quarkus/aloha-codestart`:

  ```yaml
  name: aloha-codestart
  ref: aloha
  type: code
  tags: extension-codestart
  metadata:
    title: Aloha
    description: Start to code with the Aloha extension.
    related-guide-section: https://docs.quarkiverse.io/quarkus-aloha/dev/
    path: /aloha # (optional) for web extensions providing HTTP resources
  ```


- Add the Maven build plugin configuration in `runtime/pom.xml` (to generate the codestart artifact: `/target/quarkus-aloha-VERSION-codestarts.jar`):

  ```xml
        <plugin>
          <artifactId>maven-jar-plugin</artifactId>
          <executions>
            <execution>
              <id>generate-codestart-jar</id>
              <phase>generate-resources</phase>
              <goals>
                <goal>jar</goal>
              </goals>
              <configuration>
                <classesDirectory>${project.basedir}/src/main</classesDirectory>
                <includes>
                  <include>codestarts/**</include>
                </includes>
                <classifier>codestarts</classifier>
                <skipIfEmpty>true</skipIfEmpty>
              </configuration>
            </execution>
          </executions>
        </plugin>
  ```


- Add the codestart binding in the extension metadata `runtime/src/main/resources/META-INF/quarkus-extension.yaml`. **Without this, your codestart won't be added when your extension is picked**:

  ```yaml
  name: ...
  description: ...
  metadata:
    ...
    codestart:
      name: "aloha"
      languages:
      - "java"
      artifact: "io.quarkiverse.aloha:quarkus-aloha:codestarts:jar:${project.version}"
  ```


- Add the readme [README.md](#readme-md) section template in `base/README.tpl.qute.md`:

  ```html
  {#include readme-header /}
  ```


- Run `mvn clean install` in the extension root (or just `runtime`).

- Now we can check that the codestart actually works by creating a project consuming our extension (make sure the snapshot version is correct):

  ```bash
  quarkus create app aloha-app -x=io.quarkiverse.aloha:quarkus-aloha:999-SNAPSHOT

  ...
  applying codestarts...
  📚  java
  🔨  maven
  📦  quarkus
  📝  config-properties
  🔧  dockerfiles
  🔧  maven-wrapper
  🚀  aloha-codestart <<<<<<<<<<<<<<<<
  ...
  ```


### Testing

- Add this dependency to the `integration-tests`:

  ```xml
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-devtools-testing</artifactId>
    <scope>test</scope>
  </dependency>
  ```


- Create a `AlohaCodestartTest` in the `integration-tests`:

  ```java

  public class AlohaCodestartTest {

      @RegisterExtension
      public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
              .languages(Language.JAVA)
              .setupStandaloneExtensionTest("io.quarkiverse.aloha:quarkus-aloha")
              .build();

      @Test
      void testContent() throws Throwable {
          codestartTest.checkGeneratedSource("org.acme.AlohaResource");
      }

      @Test
      void buildAllProjects() throws Throwable {
          codestartTest.buildAllProjects();
      }
  }
  ```


### Going further

- If the extension provides some web resources, add the `base/src/main/resources/META-INF/resources/index.entry.qute.html` template ([index.html and web extension codestarts](#index-html)).

- Add another language (it is recommended to provide Java and Kotlin).

- You may add some other resources (in the `./base` directory if they are not language-specific).

### Extensions codestarts in Quarkus Core

- The codestarts are all grouped in a [specific module](https://github.com/quarkusio/quarkus/tree/main/devtools/project-core-extension-codestarts/src/main/resources/codestarts/quarkus/extension-codestarts).
- No extra Maven configuration is needed.
- The [extension metadata](https://github.com/quarkusio/quarkus/blob/main/extensions/resteasy-reactive/rest-qute/runtime/src/main/resources/META-INF/quarkus-extension.yaml#L18) references the artifact containing all the core codestarts.
- The tests are also [grouped](https://github.com/quarkusio/quarkus/tree/main/integration-tests/devtools/src/test/java/io/quarkus/devtools/codestarts/quarkus). You don't need to test the build as there is a specific grouped [test](https://github.com/quarkusio/quarkus/blob/main/integration-tests/devtools/src/test/java/io/quarkus/devtools/codestarts/quarkus/QuarkusCodestartBuildIT.java) for it. e.g.:

  ```java
  public class ConfigYamlCodestartTest {

      @RegisterExtension
      public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
              .codestarts("config-yaml")
              .languages(JAVA, KOTLIN)
              .build();

      @Test
      void testContent() throws Throwable {
          codestartTest.checkGeneratedSource("org.acme.GreetingConfig");
          codestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "src/main/resources/application.yml");
      }

      @Test
      @EnabledIfSystemProperty(named = "build-projects", matches = "true")
      void buildAllProjectsForLocalUse() throws Throwable {
          codestartTest.buildAllProjects();
      }

  }
  ```


## Specific topics

<a id="org-acme-package"></a>
### `org.acme` placeholder for package name

You have to use `org.acme` as the package name in your extension codestart sources. In the generated project, the user specified package (or Group) will be used (and automatically replace `org.acme`).

The package will be automatically replaced in all the source files (.java, .kt, .scala). The package directory will also be automatically adjusted. If for some reason, another type of file needs the user package name then you should use a [Templates (Qute)](#qute-templates) for it and the `{project.package-name}` data placeholder ([find an example in the grpc proto file](https://github.com/quarkusio/quarkus/blob/main/devtools/project-core-extension-codestarts/src/main/resources/codestarts/quarkus/extension-codestarts/grpc-codestart/base/src/main/proto/hello.tpl.qute.proto#L4)).

<a id="codestart-yml"></a>
### codestart.yml

```yaml
# codestart unique name
name: resteasy-example
# codestart reference, use the extension id
ref: resteasy
# use 'code' (other types are for base codestarts)
type: code
# use 'extension-codestart'
tags: extension-codestart
# public metadata for this example (accessible as data in the templates e.g. {title})
metadata:
  title: RESTEasy Jakarta REST example
  description: Rest is easy peasy with this Hello World RESTEasy resource.
  related-guide-section: https://quarkus.io/guides/getting-started#the-jax-rs-resources
  # (optional) use this in web extensions with a specific path (and also add the index page)
  path: /some-path
```

<a id="directory-structure"></a>
### Directory Structure

> [!NOTE]
> `codestart.yml` is the only required file.

- `codestart.yml` must be at the root of the codestart
- `./base` contains all the files that will be processed independently of the specified language
- `./[java/kotlin/scala]` contains all the files that will be processed only if the specified language has been selected (overriding base)

### Dynamic Config Keys in Codestart

```bash
gen-info.time = generation time (in milliseconds)
input.selected-extensions[].name|description|guide = list of selected extensions with info
input.selected-extensions-ga = Set of Strings containing the list of extensions groupId:artifactId, useful for dynamic codestarts depending on selected extensions
input.provided-code[].name|tags|title|description|related-guide: list of selected codestarts with info
```

### Static Config Keys in Codestart

```properties
quarkus.platform.group-id = BOM groupId
quarkus.platform.artifact-id = BOM artifactId
quarkus.platform.version = BOM version
project.group-id = Project groupId
project.artifact-id = Project artifactId
project.version = Project version
project.name = Project name (if specified)
project.description = Project description (if specified)
project.package-name = Project package name
quarkus.maven-plugin.group-id = Quarkus Maven plugin groupId
quarkus.maven-plugin.artifact-id = Quarkus Maven plugin artifactId
quarkus.maven-plugin.version = Quarkus Maven plugin version
quarkus.gradle-plugin.id = Quarkus Gradle pluginId
quarkus.gradle-plugin.version = Quarkus Gradle plugin version
quarkus.version = Quarkus version
java.version = Java version
kotlin.version = Kotlin version
scala.version = Scala version
scala-maven-plugin.version = Scala Maven plugin version
maven-compiler-plugin.version = Maven compiler plugin version
maven-surefire-plugin.version = Maven Surefire plugin version
```

### Naming Convention for files

- `.tpl.qute` will be processed with Qute and can use data (`.tpl.qute` will be removed from the output file name).
- certain common files, such as `readme.md`, `src/main/resources/application.yml`, `src/main/resources/META-INF/resources/index.html` are generated from the collected fragments found in the selected codestarts for the project
- other files are copied.

<a id="qute-templates"></a>
### Templates (Qute)

Codestarts may use Qute templates `MyClass.tpl.qute.java` for dynamic rendering.

Those templates are able to use data which contains:

- The `data` (and public `metadata`) of the codestart to generate (specified in the `codestart.yml`)
- A merge of the `shared-data` from the all the codestarts used to generate the project
- The user input
- Some dynamically generated data (e.g. `dependencies` and `test-dependencies`)

<a id="readme-md"></a>
### README.md

You may add a `README.md` or `README.tpl.qute.md` in the `base` directory, it will be appended to the others. So just add the info relative to your extension codestart.

base/README.tpl.qute.md

```html
{#include readme-header /}

[Optionally, Here you may add information about how to use the example, settings, ...]
```

> [!NOTE]
> The `{#include readme-header /}` will use a template located in the Quarkus project codestart which displays standard info from the `codestart.yml` metadata.

<a id="app-config"></a>
### application config application.yml

As a convention, you should always provide the Quarkus configuration as a yaml file (`base/src/main/resources/application.yml`).

It is going to be:

- merged with the other extension codestarts configs
- automatically converted to the selected config type (yaml or properties) at generation time depending on the selected extensions

<a id="index-html"></a>
### index.html and web extension codestarts

Extension codestarts may provide a snippet for the generated index.html by adding this file:

base/src/main/resources/META-INF/resources/index.entry.qute.html:

```html
{#include index-entry /}
```

> [!NOTE]
> The `{#include index-entry /}` will use a template located in the Quarkus project codestart which displays standard info from the `codestart.yml` metadata.

<a id="integration-test"></a>
### Integration test

An extension is available to help test extension codestarts `QuarkusCodestartTest`:

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-devtools-testing</artifactId>
  <scope>test</scope>
</dependency>
```

It provides a way to test:

- the generated project content (with immutable mocked data) using snapshot testing
- the generated project build/run (with real data) with helpers to run the build

> [!NOTE]
> Before all the tests, the extension will generate Quarkus projects in the specified languages with the given codestart using mocked data and real data. You can find those generated projects in the `target/quarkus-codestart-test` directory. You can open the `real-data` ones in your IDE or play with them using the terminal. **The real data is the easiest way to iterate on your extension codestart development.**

The extension provides helpers to test that the projects build `buildAllProjects` or just a specific language project `buildProject(Language language)`. It also provides helpers to test the content with [Snapshot testing](#snapshot-testing).

The [ConfigYamlCodestartTest](https://github.com/quarkusio/quarkus/blob/main/integration-tests/devtools/src/test/java/io/quarkus/devtools/codestarts/quarkus/ConfigYamlCodestartTest.java) is a good example in Quarkus core.

<a id="snapshot-testing"></a>
#### Snapshot testing

Snapshot testing is a way to make sure the content generated by a test doesn't change from one revision to another, i.e. between commits. That means, the generated content for each commit needs to be immutable and deterministic (this is the reason for using mocked data). To be able to perform such checks, we auto-generate snapshots of the generated content  and commit them as the references of the expected output for subsequent test runs. When the templates change, we also commit the induced snapshots changes. This way, during the review, we can make sure the applied code changes have the expected effects on the generated output.

The extension provides helpers to check the content:

- `checkGeneratedSource()` validate a class against the snapshots for all languages (or a specific one).
- `checkGeneratedTestSource()` validate a test class against the snapshots for all languages (or a specific one).
- `assertThatGeneratedFileMatchSnapshot()` check a project file against the snapshot.
- You can use `AbstractPathAssert.satisfies(checkContains("some content"))` or any Path assert on the return of the methods above to also check the file contains a specific content.
- `assertThatGeneratedTreeMatchSnapshots()`  lets you compare the project file structure (tree) for a specific language against its snapshot.

> [!NOTE]
> In order to first generate or update existing snapshots files on your local filesystem, you need to add `-Dsnap` when running the tests locally while developing the codestart. They need to be added as part of the commit, else the tests will not pass on the CI.

### Writing tips

- Your extension codestart must/should be independent of buildtool and dockerfiles.
- Extension codestarts should be able to work alongside each other without interference (in combination).
- Make sure your class names are unique across all extension codestarts.
- Only use `org.acme` as package name.
- Use a unique path `/[unique]` for your REST paths
- Write the config in yml `src/main/resources/application.yml`.

  It is going to be merged with the other codestarts config and automatically converted to the selected config type (yaml or properties).

- You can start with java and add kotlin later in another PR (create an issue so you don't forget).
- If you have a question, ping me @ia3andy on <https://quarkusio.zulipchat.com/>.

### Platform codestarts data

This chapter is relevant for [Quarkus platform](https://quarkus.io/guides/platform) developers who want to provide codestart data in the [platform metadata](https://quarkus.io/guides/platform#platform-descriptor).

While typically codestart data is configured in [codestart.yml](#codestart-yml) files, platform developers may also provide codestart data in a platform descriptor with the purpose of customizing certain values.

For example, given a `codestart.yml` such as

```yaml
name: quarkus-magic-codestart
ref: quarkus-magic
type: code
tags: extension-codestart
metadata:
  title: Quarkus Magic
  description: Quarkus magic
language:
  base:
    data:
      magic:
        source: codestart.yml
```

The value of `magic.source` can be customized in a platform descriptor like this

```yaml
{
  "id" : "org.acme.platform:acme-bom-quarkus-platform-descriptor:7.0.7:json:7.0.7",
  "platform" : true,
  "bom" : "org.acme.platform:acme-magic-bom::pom:7.0.7",
  "metadata" : {
    "project" : { <1>
      "codestart-data" : { <2>
        "quarkus-magic-codestart" : { <3>
          "magic" : {
            "source" : "acme-platform"
          }
        }
      }
    },
...
```

1. `project` groups metadata that is relevant for project creation tools
2. `codestart-data` is a source of data for various codestarts
3. a name of the codestart to which the data nested under it should be passed

## The generator sources

- [Codestart generator](https://github.com/quarkusio/quarkus/tree/main/independent-projects/tools/codestarts)
- [Quarkus implementation of the Codestart generator](https://github.com/quarkusio/quarkus/tree/main/independent-projects/tools/devtools-common/src/main/java/io/quarkus/devtools/codestarts/quarkus)

## Issues and Feature requests

<https://github.com/quarkusio/quarkus/labels/area%2Fcodestarts>
