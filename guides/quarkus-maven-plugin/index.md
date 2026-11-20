# Quarkus Maven plugin

The Quarkus Maven Plugin builds the Quarkus applications, and provides helpers to launch dev mode or build native executables. For more information about how to use the Quarkus Maven Plugin, please refer to the [Maven Tooling guide](/guides/maven-tooling/index.md).

## Lifecycle

The Quarkus Maven Plugin provides a specific `quarkus` packaging that is used by default in the generated projects. This packaging should be used only for the Quarkus application itself, not for the library modules that should keep the default `jar` packaging.

This `quarkus` packaging defines a specific default lifecycle:

| Phase | Executions |
| --- | --- |
| `process-resources` | - `maven-resources-plugin:resources`<br>- `quarkus-maven-plugin:generate-code` |
| `compile` | - `maven-compiler-plugin:compile` |
| `process-test-resources` | - `maven-resources-plugin:testResources`<br>- `quarkus-maven-plugin:generate-code-tests` |
| `test-compile` | - `maven-compiler-plugin:testCompile` |
| `test` | - `maven-surefire-plugin:test` |
| `package` | - `quarkus-maven-plugin:build` |
| `post-integration-test` | - `quarkus-maven-plugin:native-image-agent` |

You can tweak the lifecycle either by not using the `quarkus` packaging or by configuring the default execution.

For instance, if you want to disable the `quarkus:build` goal for a specific module:

```xml
    <!-- ... -->
    <plugin>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-maven-plugin</artifactId>
        <version>${quarkus-plugin.version}</version>
        <extensions>true</extensions>
        <executions>
            <execution>
                <id>default-build</id>
                <phase>none</phase>
            </execution>
        </executions>
    <plugin>
    <!-- ... -->
```

## Discover Maven goals

Like most Maven plugins, the Quarkus Maven Plugin has a `help` goal that prints the description of the plugin, listing all available goals as well as their description. It is also possible to print out detailed information about a goal, all its parameters and their default values. For instance, to see the help for the `create` goal, run:

```shell
./mvnw quarkus:help -Ddetail -Dgoal=create
```

## Maven goals reference

Here is the list of all the Quarkus Maven Plugin goals:

### quarkus:add-extension

Allow adding an extension to an existing pom.xml file. Because you can add one or several extension in one go, there are 2 mojos: add-extensions and add-extension. Both supports the extension and extensions parameters.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-add-extension-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-add-extension-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extension-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extension-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extension-extension"></a> extension<br><br>For usability reason, this parameter allows adding a single extension. | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extension-extensions"></a> extensions<br><br>The list of extensions to be added. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-add-extension-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-add-extension-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-add-extension-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-add-extension-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-add-extension-session"></a> session | `MavenSession` | `${session}` |

### quarkus:add-extensions

Allow adding extensions to an existing pom.xml file. Because you can add one or several extension in one go, there are 2 mojos: add-extensions and add-extension. Both supports the extension and extensions parameters.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-add-extensions-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-add-extensions-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extensions-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extensions-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extensions-extension"></a> extension<br><br>For usability reason, this parameter allows adding a single extension. | `String` |  |
| <a id="quarkus-maven-plugin-goal-add-extensions-extensions"></a> extensions<br><br>The list of extensions to be added. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-add-extensions-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-add-extensions-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-add-extensions-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-add-extensions-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-add-extensions-session"></a> session | `MavenSession` | `${session}` |

### quarkus:analyze-call-tree

Analyze call tree of a method or a class based on an existing report produced by Substrate when using -H:+PrintAnalysisCallTree, and does a more meaningful analysis of what is causing a type to be retained.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-analyze-call-tree-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-analyze-call-tree-className"></a> className | `String` | `${class}` |
| <a id="quarkus-maven-plugin-goal-analyze-call-tree-methodName"></a> methodName | `String` | `${method}` |
| <a id="quarkus-maven-plugin-goal-analyze-call-tree-reportsDir"></a> reportsDir | `File` | `${project.build.directory}/reports` |

### quarkus:build

Builds the Quarkus application.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-build-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-build-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-build-attachRunnerAsMainArtifact"></a> attachRunnerAsMainArtifact<br><br>Whether to replace the original JAR with the Uber runner JAR as the main project artifact | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-build-attachSboms"></a> attachSboms<br><br>Whether to attach SBOMs generated for Uber JARs as project artifacts | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-build-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-build-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-build-buildDirectory"></a> buildDirectory | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-build-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-build-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-build-generatedSourcesDirectory"></a> generatedSourcesDirectory<br><br>The directory for generated source files. | `File` | `${project.build.directory}/generated-sources` |
| <a id="quarkus-maven-plugin-goal-build-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-build-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-build-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-build-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-build-pluginRepos"></a> pluginRepos<br><br>The project's remote repositories to use for the resolution of plugins and their dependencies. | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-build-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-build-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-build-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-build-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-build-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-build-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-build-quarkus.build.skip"></a> quarkus.build.skip<br><br>Skips the execution of this mojo | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-build-skipOriginalJarRename"></a> skipOriginalJarRename<br><br>When the building an Uber JAR, the default JAR is renamed by adding .original suffix. Enabling this property will disable the renaming of the original JAR. | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-build-systemProperties"></a> systemProperties<br><br>The list of system properties defined for the plugin. | `Map` |  |

### quarkus:build-enhanced-artifact

Builds enhanced artifacts from data generated by integration tests. Supports:

- AOT-enhanced container images (from Leyden AOT training)
- PGO-optimized native images (from GraalVM PGO profiling)

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-build-enhanced-artifact-session"></a> session | `MavenSession` | `${session}` |

### quarkus:create

This goal helps in setting up Quarkus Maven project with quarkus-maven-plugin, with sensible defaults

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-create-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-create-appConfig"></a> appConfig | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-platformArtifactId"></a> platformArtifactId<br><br>Artifact ID of the target platform BOM | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-platformGroupId"></a> platformGroupId<br><br>Group ID of the target platform BOM | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-platformVersion"></a> platformVersion<br><br>Version of the target platform BOM | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-buildTool"></a> buildTool | `String` | `MAVEN` |
| <a id="quarkus-maven-plugin-goal-create-className"></a> className<br><br>The className will define the generated class names when picking only one of those extensions REST, RESTEasy Classic and Spring-Web. If more than one of those extensions are picked, then only the package name part will be used as packageName More info: <https://github.com/quarkusio/quarkus/issues/14437> By default, the projectGroupId is used as package for generated classes (you can also use packageName to have them different). className | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-data"></a> data | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-example"></a> example | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extensions"></a> extensions | `Set` |  |
| <a id="quarkus-maven-plugin-goal-create-javaVersion"></a> javaVersion<br><br>Version of Java used to build the project. | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-noBuildToolWrapper"></a> noBuildToolWrapper<br><br>When true, do not include the Maven/Gradle wrapper | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-create-noCode"></a> noCode<br><br>When true, do not include any code in the generated Quarkus project. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-create-outputDirectory"></a> outputDirectory | `File` | `${basedir}` |
| <a id="quarkus-maven-plugin-goal-create-packageName"></a> packageName<br><br>Set the package name of the generated classes. If not set, projectGroupId will be used as packageName packageName | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-path"></a> path<br><br>The path will define the REST path of the generated code when picking only one of those extensions REST, RESTEasy Classic and Spring-Web. If more than one of those extensions are picked, this parameter will be ignored. More info: <https://github.com/quarkusio/quarkus/issues/14437> className | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-create-projectArtifactId"></a> projectArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-projectDescription"></a> projectDescription | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-projectGroupId"></a> projectGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-projectName"></a> projectName | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-projectVersion"></a> projectVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-create-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-create-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-create-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-create-stream"></a> stream<br><br>A target platform stream, for example: 3.15 or io.quarkus.platform:3.15. When specified, the extension registry client will be used to resolve the platform BOM corresponding to the given stream. | `String` |  |

### quarkus:create-extension

Creates the base of a Quarkus Extension <https://quarkus.io/guides/writing-extensions> in different layout depending on the options and environment.

Create in the quarkus-parent project directory (or the extensions parent dir)

It will:

- generate the new Quarkus extension in the extensions parent as a module (parent, runtime and deployment), with unit test and devmode test on option.
- On option, generate the new integration test in the integration tests parent as a module.
- add the dependencies to the bom/application/pom.xml.

Creating a Quarkiverse extension

When using -DgroupId=io.quarkiverse.\[featureId\], the new extension will use the Quarkiverse layout.

Creating a standalone extension

- generate the new Quarkus extension in the current directory (parent, runtime and deployment), with unit test and devmode test on option.
- On option, generate the new integration test module in the current directory.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-create-extension-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-create-extension-artifactId"></a> artifactId<br><br>Used to detect legacy command usage and display an error | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-basedir"></a> basedir<br><br>Directory where the changes should be performed.<br><br>Default: the current directory of the current Java process. | `File` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-extensionDescription"></a> extensionDescription<br><br>The extensionDescription of the runtime module.<br><br>This description is used on <https://code.quarkus.io/>. | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-extensionId"></a> extensionId<br><br>extensionId of this extension (REQUIRED).<br><br>It will be used to generate the different extension modules artifactIds (\[namespaceId\]\[extensionId\]-parent), runtime (\[namespaceId\]\[extensionId\]) and deployment (\[namespaceId\]\[extensionId\]-deployment). | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-extensionName"></a> extensionName<br><br>The extensionName of the runtime module. The extensionNames of the extension parent and deployment modules will be based on this name too.<br><br>Default: the formatted extensionId | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-groupId"></a> groupId<br><br>The groupId for the newly created Maven modules (REQUIRED - INHERITED IN QUARKUS-CORE). | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-javaVersion"></a> javaVersion<br><br>Version of Java used to build the project. | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-namespaceId"></a> namespaceId<br><br>A prefix common to all extension artifactIds in the current source tree.<br><br>Default: "quarkus-" in quarkus Quarkus Core and Quarkiverse else "" | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-namespaceName"></a> namespaceName<br><br>A prefix common to all extension names in the current source tree.<br><br>Default: "quarkus-" in Quarkus Core and Quarkiverse else "" | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-packageName"></a> packageName<br><br>Base package under which classes should be created in Runtime and Deployment modules.<br><br>Default: auto-generated out of groupId, namespaceId and extensionId | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-create-extension-quarkusBomArtifactId"></a> quarkusBomArtifactId<br><br>The artifactId of the Quarkus platform BOM.<br><br>Default: io.quarkus.devtools.commands.CreateExtension.DEFAULT_BOM_ARTIFACT_ID | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-quarkusBomGroupId"></a> quarkusBomGroupId<br><br>The groupId of the Quarkus platform BOM.<br><br>Default: io.quarkus.devtools.commands.CreateExtension.DEFAULT_BOM_GROUP_ID | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-quarkusBomVersion"></a> quarkusBomVersion<br><br>The version of the Quarkus platform BOM.<br><br>Default: io.quarkus.devtools.commands.CreateExtension.DEFAULT_BOM_VERSION | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-quarkusVersion"></a> quarkusVersion<br><br>Quarkus version the newly created extension should depend on (REQUIRED - INHERITED IN QUARKUS-CORE). | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-create-extension-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-create-extension-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-create-extension-version"></a> version<br><br>The version for the newly created Maven modules.<br><br>Default: automatic in Quarkus Core else io.quarkus.devtools.commands.CreateExtension.DEFAULT_VERSION | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-withCodestart"></a> withCodestart<br><br>Indicates whether to generate an extension codestart | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-withoutDevModeTest"></a> withoutDevModeTest<br><br>Indicates whether to generate a devmode test for the extension | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-withoutIntegrationTests"></a> withoutIntegrationTests<br><br>Indicates whether to generate an integration tests for the extension | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-withoutTests"></a> withoutTests<br><br>Indicates whether to generate any tests for the extension (same as -DwithoutUnitTest -DwithoutIntegrationTest -DwithoutDevModeTest) | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-create-extension-withoutUnitTest"></a> withoutUnitTest<br><br>Indicates whether to generate a unit test class for the extension | `boolean` |  |

### quarkus:create-jbang

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-create-jbang-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-create-jbang-platformArtifactId"></a> platformArtifactId<br><br>Artifact ID of the target platform BOM | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-jbang-platformGroupId"></a> platformGroupId<br><br>Group ID of the target platform BOM | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-jbang-platformVersion"></a> platformVersion<br><br>Version of the target platform BOM | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-jbang-extensions"></a> extensions | `Set` |  |
| <a id="quarkus-maven-plugin-goal-create-jbang-javaVersion"></a> javaVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-create-jbang-noJBangWrapper"></a> noJBangWrapper | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-create-jbang-outputDirectory"></a> outputDirectory | `File` | `${basedir}/jbang-with-quarkus` |
| <a id="quarkus-maven-plugin-goal-create-jbang-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-create-jbang-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |

### quarkus:dependency-list

Lists dependencies of a Quarkus application as resolved by the Quarkus bootstrap dependency resolver.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-dependency-list-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-dependency-list-appendOutput"></a> appendOutput<br><br>Whether to append outputs into the output file or overwrite it. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dependency-list-flags"></a> flags<br><br>Comma-separated list of dependency flag names from io.quarkus.maven.dependency.DependencyFlags indicating which dependencies should be listed. Only dependencies that have all the specified flags set will be included. Accepted flag names (case-insensitive, both constant names and text names are supported): OPTIONAL, DIRECT, RUNTIME_CP (or runtime-cp), DEPLOYMENT_CP (or deployment-cp), RUNTIME_EXTENSION_ARTIFACT (or runtime-extension-artifact), WORKSPACE_MODULE (or workspace-module), RELOADABLE, TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT (or top-level-runtime-extension-artifact), CLASSLOADER_PARENT_FIRST (or classloader-parent-first), CLASSLOADER_RUNNER_PARENT_FIRST (or classloader-runner-parent-first), CLASSLOADER_LESSER_PRIORITY (or classloader-lesser-priority), COMPILE_ONLY (or compile-only).<br><br>If not specified, all dependencies returned by io.quarkus.bootstrap.model.ApplicationModel.getDependencies() are listed. | `String` |  |
| <a id="quarkus-maven-plugin-goal-dependency-list-mode"></a> mode<br><br>Target launch mode corresponding to io.quarkus.runtime.LaunchMode for which the dependencies should be listed. io.quarkus.runtime.LaunchMode.NORMAL is the default. | `String` | `prod` |
| <a id="quarkus-maven-plugin-goal-dependency-list-outputFile"></a> outputFile<br><br>If specified, this parameter will cause the dependency list to be written to the path specified, instead of writing to the console. | `File` |  |
| <a id="quarkus-maven-plugin-goal-dependency-list-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-dependency-list-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-dependency-list-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-dependency-list-verbose"></a> verbose<br><br>Whether to include dependency scope and flag names in the output for each dependency. | `boolean` |  |

### quarkus:dependency-sbom

Quarkus application SBOM generator

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.format"></a> quarkus.dependency.sbom.format<br><br>CycloneDX BOM format. Allowed values are json and xml. The default is json. | `String` | `json` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.include-license-text"></a> quarkus.dependency.sbom.include-license-text<br><br>Whether to include license text in the generated SBOM. The default is false | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.include-quarkus-component-scope"></a> quarkus.dependency.sbom.include-quarkus-component-scope<br><br>Whether to include the quarkus:component:scope custom property on each component. The default is false. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.mode"></a> quarkus.dependency.sbom.mode<br><br>Target launch mode corresponding to io.quarkus.runtime.LaunchMode for which the SBOM should be built. io.quarkus.runtime.LaunchMode.NORMAL is the default. | `String` | `prod` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.output-file"></a> quarkus.dependency.sbom.output-file<br><br>File to store the SBOM in. If not configured, the SBOM will be stored in the ${project.build.directory} directory. | `File` |  |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.pretty-print"></a> quarkus.dependency.sbom.pretty-print<br><br>Whether to pretty-print the generated SBOM output. The default is false | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.runtime-only"></a> quarkus.dependency.sbom.runtime-only<br><br>Whether to limit application dependencies to only those that are included in the runtime | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.schema-version"></a> quarkus.dependency.sbom.schema-version<br><br>CycloneDX BOM schema version | `String` |  |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-dependency-sbom-quarkus.dependency.sbom.skip"></a> quarkus.dependency.sbom.skip<br><br>Whether to skip the execution of the goal | `boolean` | `false` |

### quarkus:dependency-tree

Displays Quarkus application build dependency tree including the deployment ones.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-dependency-tree-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-dependency-tree-appendOutput"></a> appendOutput<br><br>Whether to append outputs into the output file or overwrite it. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dependency-tree-graph"></a> graph<br><br>INCUBATING option, enabled with -Dquarkus.bootstrap.incubating-model-resolver system or project property. Whether to log all dependencies of each dependency node in a tree, adding \[+\] suffix to those whose dependencies are not expanded. | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-dependency-tree-mode"></a> mode<br><br>Target launch mode corresponding to io.quarkus.runtime.LaunchMode for which the dependency tree should be built. io.quarkus.runtime.LaunchMode.NORMAL is the default. | `String` | `prod` |
| <a id="quarkus-maven-plugin-goal-dependency-tree-outputFile"></a> outputFile<br><br>If specified, this parameter will cause the dependency tree to be written to the path specified, instead of writing to the console. | `File` |  |
| <a id="quarkus-maven-plugin-goal-dependency-tree-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-dependency-tree-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-dependency-tree-runtimeOnly"></a> runtimeOnly<br><br>Whether to log only the runtime dependencies of the Quarkus application | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-dependency-tree-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-dependency-tree-verbose"></a> verbose<br><br>INCUBATING option, enabled with -Dquarkus.bootstrap.incubating-model-resolver system or project property. Whether to log dependency properties, such as on which classpath they belong, whether they are hot-reloadable in dev mode, etc. | `boolean` |  |

### quarkus:deploy

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-deploy-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-deploy-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-deploy-attachRunnerAsMainArtifact"></a> attachRunnerAsMainArtifact<br><br>Whether to replace the original JAR with the Uber runner JAR as the main project artifact | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-deploy-attachSboms"></a> attachSboms<br><br>Whether to attach SBOMs generated for Uber JARs as project artifacts | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-deploy-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-deploy-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-deploy-buildDirectory"></a> buildDirectory | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-deploy-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-deploy-quarkus.deployment.dry-run"></a> quarkus.deployment.dry-run | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-deploy-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-deploy-generatedSourcesDirectory"></a> generatedSourcesDirectory<br><br>The directory for generated source files. | `File` | `${project.build.directory}/generated-sources` |
| <a id="quarkus-maven-plugin-goal-deploy-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-deploy-quarkus.container-image.build"></a> quarkus.container-image.build | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-deploy-quarkus.container-image.builder"></a> quarkus.container-image.builder | `String` |  |
| <a id="quarkus-maven-plugin-goal-deploy-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-deploy-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-deploy-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-deploy-pluginRepos"></a> pluginRepos<br><br>The project's remote repositories to use for the resolution of plugins and their dependencies. | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-deploy-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-deploy-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-deploy-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-deploy-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-deploy-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-deploy-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-deploy-quarkus.build.skip"></a> quarkus.build.skip<br><br>Skips the execution of this mojo | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-deploy-skipOriginalJarRename"></a> skipOriginalJarRename<br><br>When the building an Uber JAR, the default JAR is renamed by adding .original suffix. Enabling this property will disable the renaming of the original JAR. | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-deploy-systemProperties"></a> systemProperties<br><br>The list of system properties defined for the plugin. | `Map` |  |

### quarkus:dev

The dev mojo, that runs a quarkus app in a forked process. A background compilation process is launched and any changes are automatically reflected in your running application. You can use this dev mode in a remote container environment with remote-dev.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-dev-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-dev-argsString"></a> argsString | `String` | `${quarkus.args}` |
| <a id="quarkus-maven-plugin-goal-dev-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-dev-compilerArgs"></a> compilerArgs<br><br>Additional parameters to pass to javac when recompiling changed source files. | `List` |  |
| <a id="quarkus-maven-plugin-goal-dev-compilerOptions"></a> compilerOptions<br><br>Additional compiler arguments | `List` |  |
| <a id="quarkus-maven-plugin-goal-dev-copySurefireVariables"></a> copySurefireVariables<br><br>When enabled, the \<environmentVariables> and \<systemPropertyVariables> elements of the Maven Surefire plugin are copied to environment variables and system properties defined by this plugin. Note that no other Surefire configuration is used (notably \<systemProperties>), only the 2 elements mentioned above. This plugin's \<environmentVariables> and \<systemProperties> have priority, so duplicate keys are not copied.<br><br>Since environment variables and system properties are global to the entire process, this also affects dev mode (when executed as quarkus:dev). Because of that, this copying action is disabled by default and requires opt-in. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-dev-debug"></a> debug<br><br>If this server should be started in debug mode. The default is to start in debug mode and listen on port 5005. Whether the JVM is suspended waiting for a debugger to be attached, depends on the value of suspend. debug supports the following options: Value Effect false The JVM is not started in debug mode true The JVM is started in debug mode and will be listening on debugHost:debugPort client The JVM is started in client mode, and will attempt to connect to debugHost:debugPort {port} The JVM is started in debug mode and will be listening on debugHost:{port}. By default, debugHost has the value "localhost", and debugPort is 5005. | `String` | `${debug}` |
| <a id="quarkus-maven-plugin-goal-dev-debugHost"></a> debugHost | `String` | `${debugHost}` |
| <a id="quarkus-maven-plugin-goal-dev-debugPort"></a> debugPort | `String` | `${debugPort}` |
| <a id="quarkus-maven-plugin-goal-dev-deleteDevJar"></a> deleteDevJar | `boolean` | `TRUE` |
| <a id="quarkus-maven-plugin-goal-dev-enforceBuildGoal"></a> enforceBuildGoal<br><br>Whether to enforce the quarkus-maven-plugin build goal to be configured. By default, a missing build goal is considered an inconsistency (although the build goal is not required technically). In this case a warning will be logged and the application will not be started. | `boolean` | `${quarkus.enforceBuildGoal}` |
| <a id="quarkus-maven-plugin-goal-dev-environmentVariables"></a> environmentVariables | `Map` |  |
| <a id="quarkus-maven-plugin-goal-dev-extensionJvmOptions"></a> extensionJvmOptions<br><br>Extension dev mode JVM option filter configuration. Allows disabling all JVM options configured by extensions, for example \<extensionJvmOptions> \<!-- disable JVM options from all the extensions --> \<disableAll>true\</disableAll> \</extensionJvmOptions>\* or specifying a groupId:artifactId:classifier artifact pattern to disable options provided by the matching subset of extensions, for example \<extensionJvmOptions> \<disableFor> \<!-- disable JVM options from all the extensions with groupId org.acme --> \<extension>org.acme\</extension> \<!-- disable JVM options configured by io.quarkiverse:quarkus-magic --> \<extension>io.quarkiverse:quarkus-magic\</extension> \</disableFor> \</extensionJvmOptions> | `ExtensionDevModeJvmOptionFilter` |  |
| <a id="quarkus-maven-plugin-goal-dev-forceC2"></a> forceC2<br><br>This value is intended to be set to true when we want to require C2 compilation instead of preventing it from ever kicking in. Setting this will likely have a small negative effect on startup time and should only be done when it absolutely makes sense. | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-dev-jvmArgs"></a> jvmArgs<br><br>Allows configuring arbitrary JVM arguments. Multiple arguments can be specified by delimiting them with a space character. | `String` | `${jvm.args}` |
| <a id="quarkus-maven-plugin-goal-dev-modules"></a> modules<br><br>Allows configuring the modules to add to the application. The listed modules will be added using: --add-modules m1,m2.... | `List` | `${add-modules}` |
| <a id="quarkus-maven-plugin-goal-dev-mojoExecution"></a> mojoExecution | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-dev-noDeps"></a> noDeps<br><br>Whether changes in the projects that appear to be dependencies of the project containing the application to be launched should trigger hot-reload. By default, they do. | `boolean` | `${noDeps}` |
| <a id="quarkus-maven-plugin-goal-dev-openJavaLang"></a> openJavaLang | `boolean` | `${open-lang-package}` |
| <a id="quarkus-maven-plugin-goal-dev-outputDirectory"></a> outputDirectory<br><br>The directory for compiled classes. | `File` (required) | `${project.build.outputDirectory}` |
| <a id="quarkus-maven-plugin-goal-dev-pluginRepos"></a> pluginRepos | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-dev-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-dev-release"></a> release<br><br>The --release argument to javac. | `String` | `${maven.compiler.release}` |
| <a id="quarkus-maven-plugin-goal-dev-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-dev-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-dev-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-dev-skipPlugins"></a> skipPlugins<br><br>A comma-separated list of Maven plugin keys in groupId:artifactId format (for example org.codehaus.mojo:flatten-maven-plugin and/or goal prefixes, (for example flatten) that should be skipped when quarkus:dev identifies Maven plugin goals that should be executed before the application is launched in dev mode. Only the flatten Maven plugin is skipped by default. | `Set` | `org.codehaus.mojo:flatten-maven-plugin` |
| <a id="quarkus-maven-plugin-goal-dev-source"></a> source<br><br>The -source argument to javac. | `String` | `${maven.compiler.source}` |
| <a id="quarkus-maven-plugin-goal-dev-sourceDir"></a> sourceDir | `File` | `${project.build.sourceDirectory}` |
| <a id="quarkus-maven-plugin-goal-dev-suspend"></a> suspend<br><br>Whether the JVM launch, in debug mode, should be suspended. This parameter is only relevant when the JVM is launched in debug mode. This parameter supports the following values (all the allowed values are case-insensitive): Value Effect y or true The debug mode JVM launch is suspended n or false The debug mode JVM is started without suspending | `String` | `${suspend}` |
| <a id="quarkus-maven-plugin-goal-dev-systemProperties"></a> systemProperties | `Map` |  |
| <a id="quarkus-maven-plugin-goal-dev-target"></a> target<br><br>The -target argument to javac. | `String` | `${maven.compiler.target}` |
| <a id="quarkus-maven-plugin-goal-dev-test"></a> test<br><br>Selects given test(s) for continuous testing. This is an alternative to quarkus.test.include-pattern and quarkus.test.exclude-pattern; if set, the quarkus.test.\[include\|exclude\]-pattern configuration is ignored. The format of this configuration property is the same as the Maven Surefire -Dtest=... format \<<https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html><mark>test>. Specifically: it is a comma (,) separated list of globs of class file paths and/or method names. Each glob can potentially be prefixed with an exclamation mark (!), which makes it an exclusion filter instead of an inclusion filter. Exclusions have higher priority than inclusions. The class file path glob is separated from the method name glob by the hash sign (</mark>) and multiple method name globs may be present, separated by the plus sign (+).<br><br>For example:<br><br>- Basic\*: all classes starting with Basic<br>- ???Test: all classes named with 3 arbitrary characters followed by Test<br>- !Unstable\*: all classes except classes starting with Unstable<br>- pkg/\*\*/Ci\*leTest: all classes in the package pkg and subpackages, starting with Ci and ending with leTest<br>- \*Test#test\*One+testTwo?????: all classes ending with Test, and in them, only methods starting with test and ending with One, or starting with testTwo and followed by 5 arbitrary characters<br>- #fast\*+slowTest: all classes, and in them, only methods starting with fast or methods named slowTest Note that the syntax %regex\[...\] and %ant\[...\] is NOT supported. | `String` |  |
| <a id="quarkus-maven-plugin-goal-dev-quarkus.warnIfBuildGoalMissing"></a> quarkus.warnIfBuildGoalMissing | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-dev-watchedFiles"></a> watchedFiles<br><br>Optional list of files to watch for changes that trigger a hot reload in dev mode. This is useful for extensions developers that can set this property to their extension's artifacts in their local repository. | `List` |  |
| <a id="quarkus-maven-plugin-goal-dev-workingDir"></a> workingDir | `File` |  |

### quarkus:generate-code

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-generate-code-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-generate-code-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-generate-code-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-generate-code-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-launchMode"></a> launchMode<br><br>Application launch mode for which to generate the source code. | `String` | `NORMAL` |
| <a id="quarkus-maven-plugin-goal-generate-code-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-generate-code-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-generate-code-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-generate-code-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-generate-code-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-generate-code-quarkus.generate-code.skip"></a> quarkus.generate-code.skip<br><br>Skip the execution of this mojo | `boolean` | `false` |

### quarkus:generate-code-tests

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-launchMode"></a> launchMode<br><br>Application launch mode for which to generate the source code. | `String` | `NORMAL` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-quarkus.generate-code.serialize-test-model"></a> quarkus.generate-code.serialize-test-model<br><br>A switch that enables or disables serialization of an io.quarkus.bootstrap.model.ApplicationModel to a file for tests. Deserializing an application model when bootstrapping Quarkus tests has a performance advantage in that the tests will not have to initialize a Maven resolver and re-resolve the application model, which may save, depending on a project, \~80-95% of time on io.quarkus.bootstrap.model.ApplicationModel resolution. Serialization of the test model is enabled by default. | `boolean` | `true` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-generate-code-tests-quarkus.generate-code.skip"></a> quarkus.generate-code.skip<br><br>Skip the execution of this mojo | `boolean` | `false` |

### quarkus:go-offline

This goal downloads all the Maven artifact dependencies required to build, run, test and launch the application dev mode.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-go-offline-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-go-offline-mode"></a> mode<br><br>Target launch mode corresponding to io.quarkus.runtime.LaunchMode for which the dependencies should be resolved. all is the default, since it includes both provided and test dependency scopes and also the dependencies required by dev mode. | `String` | `all` |
| <a id="quarkus-maven-plugin-goal-go-offline-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-go-offline-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-go-offline-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-go-offline-session"></a> session | `MavenSession` | `${session}` |

### quarkus:help

Display help information on quarkus-maven-plugin. Call mvn quarkus:help -Ddetail=true -Dgoal=\<goal-name> to display parameter details.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-help-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-help-detail"></a> detail<br><br>If true, display all settable properties for each goal. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-help-goal"></a> goal<br><br>The name of the goal for which to show help. If unspecified, all goals will be displayed. | `String` |  |
| <a id="quarkus-maven-plugin-goal-help-indentSize"></a> indentSize<br><br>The number of spaces per indentation level, should be positive. | `int` | `2` |
| <a id="quarkus-maven-plugin-goal-help-lineLength"></a> lineLength<br><br>The maximum length of a display line, should be positive. | `int` | `80` |

### quarkus:image-build

Builds a container image.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-image-build-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-image-build-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-image-build-attachRunnerAsMainArtifact"></a> attachRunnerAsMainArtifact<br><br>Whether to replace the original JAR with the Uber runner JAR as the main project artifact | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-build-attachSboms"></a> attachSboms<br><br>Whether to attach SBOMs generated for Uber JARs as project artifacts | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-build-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-image-build-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-image-build-buildDirectory"></a> buildDirectory | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-image-build-quarkus.container-image.builder"></a> quarkus.container-image.builder | `String` |  |
| <a id="quarkus-maven-plugin-goal-image-build-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-build-quarkus.container-image.dry-run"></a> quarkus.container-image.dry-run | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-build-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-image-build-generatedSourcesDirectory"></a> generatedSourcesDirectory<br><br>The directory for generated source files. | `File` | `${project.build.directory}/generated-sources` |
| <a id="quarkus-maven-plugin-goal-image-build-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-image-build-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-image-build-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-image-build-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-image-build-pluginRepos"></a> pluginRepos<br><br>The project's remote repositories to use for the resolution of plugins and their dependencies. | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-image-build-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-image-build-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-image-build-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-image-build-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-image-build-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-image-build-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-image-build-quarkus.build.skip"></a> quarkus.build.skip<br><br>Skips the execution of this mojo | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-image-build-skipOriginalJarRename"></a> skipOriginalJarRename<br><br>When the building an Uber JAR, the default JAR is renamed by adding .original suffix. Enabling this property will disable the renaming of the original JAR. | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-build-systemProperties"></a> systemProperties<br><br>The list of system properties defined for the plugin. | `Map` |  |

### quarkus:image-push

Pushes a container image.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-image-push-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-image-push-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-image-push-attachRunnerAsMainArtifact"></a> attachRunnerAsMainArtifact<br><br>Whether to replace the original JAR with the Uber runner JAR as the main project artifact | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-push-attachSboms"></a> attachSboms<br><br>Whether to attach SBOMs generated for Uber JARs as project artifacts | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-push-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-image-push-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-image-push-buildDirectory"></a> buildDirectory | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-image-push-quarkus.container-image.builder"></a> quarkus.container-image.builder | `String` |  |
| <a id="quarkus-maven-plugin-goal-image-push-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-push-quarkus.container-image.dry-run"></a> quarkus.container-image.dry-run | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-push-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-image-push-generatedSourcesDirectory"></a> generatedSourcesDirectory<br><br>The directory for generated source files. | `File` | `${project.build.directory}/generated-sources` |
| <a id="quarkus-maven-plugin-goal-image-push-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-image-push-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-image-push-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-image-push-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-image-push-pluginRepos"></a> pluginRepos<br><br>The project's remote repositories to use for the resolution of plugins and their dependencies. | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-image-push-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-image-push-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-image-push-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-image-push-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-image-push-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-image-push-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-image-push-quarkus.build.skip"></a> quarkus.build.skip<br><br>Skips the execution of this mojo | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-image-push-skipOriginalJarRename"></a> skipOriginalJarRename<br><br>When the building an Uber JAR, the default JAR is renamed by adding .original suffix. Enabling this property will disable the renaming of the original JAR. | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-image-push-systemProperties"></a> systemProperties<br><br>The list of system properties defined for the plugin. | `Map` |  |

### quarkus:info

Log Quarkus-specific project information, such as imported Quarkus platform BOMs, Quarkus extensions found among the project dependencies, etc.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-info-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-info-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-info-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-info-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-info-perModule"></a> perModule<br><br>If true, the information will be logged per each relevant module of the project instead of an overall summary | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-info-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-info-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-info-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-info-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-info-session"></a> session | `MavenSession` | `${session}` |

### quarkus:list-categories

List extension categories, which a user can use to filter extensions.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-list-categories-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-list-categories-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-categories-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-categories-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-categories-format"></a> format<br><br>Select the output format among 'name' (display the name only) and 'full' (includes a verbose name and a description). | `String` | `concise` |
| <a id="quarkus-maven-plugin-goal-list-categories-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-list-categories-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-list-categories-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-list-categories-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-list-categories-session"></a> session | `MavenSession` | `${session}` |

### quarkus:list-extensions

List the available extensions. You can add one or several extensions in one go, with the 2 following mojos: add-extensions and add-extension. You can list all extension or just installable. Choose between 3 output formats: name, concise and full.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-list-extensions-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-list-extensions-all"></a> all<br><br>List all extensions or just the installable. | `boolean` | `true` |
| <a id="quarkus-maven-plugin-goal-list-extensions-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-extensions-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-extensions-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-extensions-category"></a> category<br><br>Only list extensions from given category. | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-extensions-format"></a> format<br><br>Select the output format among 'id' (display the artifactId only), 'concise' (display name and artifactId), 'origins' (display extension catalog IDs providing extension information), 'support-scope' (support scope associated with each extension, if any) and 'full' (concise format and version related columns). | `String` | `concise` |
| <a id="quarkus-maven-plugin-goal-list-extensions-installed"></a> installed<br><br>List the already installed extensions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-list-extensions-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-list-extensions-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-list-extensions-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-list-extensions-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-list-extensions-searchPattern"></a> searchPattern<br><br>Search filter on extension list. The format is based on Java Pattern. | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-extensions-session"></a> session | `MavenSession` | `${session}` |

### quarkus:list-platforms

List imported and optionally other platforms available for the project.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-list-platforms-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-list-platforms-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-platforms-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-platforms-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-list-platforms-installed"></a> installed<br><br>List the already installed extensions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-list-platforms-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-list-platforms-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-list-platforms-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-list-platforms-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-list-platforms-session"></a> session | `MavenSession` | `${session}` |

### quarkus:native-image-agent

Post-processes native image agent generated configuration to trim any unnecessary configuration.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-native-image-agent-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-native-image-agent-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-native-image-agent-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-native-image-agent-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-native-image-agent-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-native-image-agent-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-native-image-agent-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-native-image-agent-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-native-image-agent-session"></a> session | `MavenSession` | `${session}` |

### quarkus:prepare

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-prepare-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-prepare-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-prepare-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-prepare-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-prepare-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-prepare-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-prepare-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-prepare-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-prepare-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-prepare-launchMode"></a> launchMode<br><br>Application launch mode for which to generate the source code. | `String` | `NORMAL` |
| <a id="quarkus-maven-plugin-goal-prepare-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-prepare-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-prepare-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-prepare-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-prepare-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-prepare-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-prepare-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-prepare-quarkus.generate-code.skip"></a> quarkus.generate-code.skip<br><br>Skip the execution of this mojo | `boolean` | `false` |

### quarkus:prepare-tests

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-prepare-tests-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-prepare-tests-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-launchMode"></a> launchMode<br><br>Application launch mode for which to generate the source code. | `String` | `NORMAL` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-prepare-tests-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-quarkus.generate-code.serialize-test-model"></a> quarkus.generate-code.serialize-test-model<br><br>A switch that enables or disables serialization of an io.quarkus.bootstrap.model.ApplicationModel to a file for tests. Deserializing an application model when bootstrapping Quarkus tests has a performance advantage in that the tests will not have to initialize a Maven resolver and re-resolve the application model, which may save, depending on a project, \~80-95% of time on io.quarkus.bootstrap.model.ApplicationModel resolution. Serialization of the test model is enabled by default. | `boolean` | `true` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-prepare-tests-quarkus.generate-code.skip"></a> quarkus.generate-code.skip<br><br>Skip the execution of this mojo | `boolean` | `false` |

### quarkus:remote-dev

The dev mojo, that connects to a remote host.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-remote-dev-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-remote-dev-argsString"></a> argsString | `String` | `${quarkus.args}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-compilerArgs"></a> compilerArgs<br><br>Additional parameters to pass to javac when recompiling changed source files. | `List` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-compilerOptions"></a> compilerOptions<br><br>Additional compiler arguments | `List` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-copySurefireVariables"></a> copySurefireVariables<br><br>When enabled, the \<environmentVariables> and \<systemPropertyVariables> elements of the Maven Surefire plugin are copied to environment variables and system properties defined by this plugin. Note that no other Surefire configuration is used (notably \<systemProperties>), only the 2 elements mentioned above. This plugin's \<environmentVariables> and \<systemProperties> have priority, so duplicate keys are not copied.<br><br>Since environment variables and system properties are global to the entire process, this also affects dev mode (when executed as quarkus:dev). Because of that, this copying action is disabled by default and requires opt-in. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-remote-dev-debug"></a> debug<br><br>If this server should be started in debug mode. The default is to start in debug mode and listen on port 5005. Whether the JVM is suspended waiting for a debugger to be attached, depends on the value of suspend. debug supports the following options: Value Effect false The JVM is not started in debug mode true The JVM is started in debug mode and will be listening on debugHost:debugPort client The JVM is started in client mode, and will attempt to connect to debugHost:debugPort {port} The JVM is started in debug mode and will be listening on debugHost:{port}. By default, debugHost has the value "localhost", and debugPort is 5005. | `String` | `${debug}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-debugHost"></a> debugHost | `String` | `${debugHost}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-debugPort"></a> debugPort | `String` | `${debugPort}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-deleteDevJar"></a> deleteDevJar | `boolean` | `TRUE` |
| <a id="quarkus-maven-plugin-goal-remote-dev-enforceBuildGoal"></a> enforceBuildGoal<br><br>Whether to enforce the quarkus-maven-plugin build goal to be configured. By default, a missing build goal is considered an inconsistency (although the build goal is not required technically). In this case a warning will be logged and the application will not be started. | `boolean` | `${quarkus.enforceBuildGoal}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-environmentVariables"></a> environmentVariables | `Map` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-extensionJvmOptions"></a> extensionJvmOptions<br><br>Extension dev mode JVM option filter configuration. Allows disabling all JVM options configured by extensions, for example \<extensionJvmOptions> \<!-- disable JVM options from all the extensions --> \<disableAll>true\</disableAll> \</extensionJvmOptions>\* or specifying a groupId:artifactId:classifier artifact pattern to disable options provided by the matching subset of extensions, for example \<extensionJvmOptions> \<disableFor> \<!-- disable JVM options from all the extensions with groupId org.acme --> \<extension>org.acme\</extension> \<!-- disable JVM options configured by io.quarkiverse:quarkus-magic --> \<extension>io.quarkiverse:quarkus-magic\</extension> \</disableFor> \</extensionJvmOptions> | `ExtensionDevModeJvmOptionFilter` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-forceC2"></a> forceC2<br><br>This value is intended to be set to true when we want to require C2 compilation instead of preventing it from ever kicking in. Setting this will likely have a small negative effect on startup time and should only be done when it absolutely makes sense. | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-jvmArgs"></a> jvmArgs<br><br>Allows configuring arbitrary JVM arguments. Multiple arguments can be specified by delimiting them with a space character. | `String` | `${jvm.args}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-modules"></a> modules<br><br>Allows configuring the modules to add to the application. The listed modules will be added using: --add-modules m1,m2.... | `List` | `${add-modules}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-mojoExecution"></a> mojoExecution | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-noDeps"></a> noDeps<br><br>Whether changes in the projects that appear to be dependencies of the project containing the application to be launched should trigger hot-reload. By default, they do. | `boolean` | `${noDeps}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-openJavaLang"></a> openJavaLang | `boolean` | `${open-lang-package}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-outputDirectory"></a> outputDirectory<br><br>The directory for compiled classes. | `File` (required) | `${project.build.outputDirectory}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-pluginRepos"></a> pluginRepos | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-release"></a> release<br><br>The --release argument to javac. | `String` | `${maven.compiler.release}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-skipPlugins"></a> skipPlugins<br><br>A comma-separated list of Maven plugin keys in groupId:artifactId format (for example org.codehaus.mojo:flatten-maven-plugin and/or goal prefixes, (for example flatten) that should be skipped when quarkus:dev identifies Maven plugin goals that should be executed before the application is launched in dev mode. Only the flatten Maven plugin is skipped by default. | `Set` | `org.codehaus.mojo:flatten-maven-plugin` |
| <a id="quarkus-maven-plugin-goal-remote-dev-source"></a> source<br><br>The -source argument to javac. | `String` | `${maven.compiler.source}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-sourceDir"></a> sourceDir | `File` | `${project.build.sourceDirectory}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-suspend"></a> suspend<br><br>Whether the JVM launch, in debug mode, should be suspended. This parameter is only relevant when the JVM is launched in debug mode. This parameter supports the following values (all the allowed values are case-insensitive): Value Effect y or true The debug mode JVM launch is suspended n or false The debug mode JVM is started without suspending | `String` | `${suspend}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-systemProperties"></a> systemProperties | `Map` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-target"></a> target<br><br>The -target argument to javac. | `String` | `${maven.compiler.target}` |
| <a id="quarkus-maven-plugin-goal-remote-dev-test"></a> test<br><br>Selects given test(s) for continuous testing. This is an alternative to quarkus.test.include-pattern and quarkus.test.exclude-pattern; if set, the quarkus.test.\[include\|exclude\]-pattern configuration is ignored. The format of this configuration property is the same as the Maven Surefire -Dtest=... format \<<https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html><mark>test>. Specifically: it is a comma (,) separated list of globs of class file paths and/or method names. Each glob can potentially be prefixed with an exclamation mark (!), which makes it an exclusion filter instead of an inclusion filter. Exclusions have higher priority than inclusions. The class file path glob is separated from the method name glob by the hash sign (</mark>) and multiple method name globs may be present, separated by the plus sign (+).<br><br>For example:<br><br>- Basic\*: all classes starting with Basic<br>- ???Test: all classes named with 3 arbitrary characters followed by Test<br>- !Unstable\*: all classes except classes starting with Unstable<br>- pkg/\*\*/Ci\*leTest: all classes in the package pkg and subpackages, starting with Ci and ending with leTest<br>- \*Test#test\*One+testTwo?????: all classes ending with Test, and in them, only methods starting with test and ending with One, or starting with testTwo and followed by 5 arbitrary characters<br>- #fast\*+slowTest: all classes, and in them, only methods starting with fast or methods named slowTest Note that the syntax %regex\[...\] and %ant\[...\] is NOT supported. | `String` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-quarkus.warnIfBuildGoalMissing"></a> quarkus.warnIfBuildGoalMissing | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-watchedFiles"></a> watchedFiles<br><br>Optional list of files to watch for changes that trigger a hot reload in dev mode. This is useful for extensions developers that can set this property to their extension's artifacts in their local repository. | `List` |  |
| <a id="quarkus-maven-plugin-goal-remote-dev-workingDir"></a> workingDir | `File` |  |

### quarkus:remove-extension

Allow removing an extension from an existing pom.xml file. Because you can remove one or several extension in one go, there are 2 mojos: remove-extensions and remove-extension. Both supports the extension and extensions parameters.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-remove-extension-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-remove-extension-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extension-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extension-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extension-extension"></a> extension<br><br>For usability reason, this parameter allows removing a single extension. | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extension-extensions"></a> extensions<br><br>The list of extensions to be removed. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-remove-extension-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-remove-extension-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-remove-extension-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-remove-extension-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-remove-extension-session"></a> session | `MavenSession` | `${session}` |

### quarkus:remove-extensions

Allow removing extensions to an existing pom.xml file. Because you can remove one or several extension in one go, there are 2 mojos: remove-extensions and remove-extension. Both supports the extension and extensions parameters.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-remove-extensions-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-remove-extensions-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extensions-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extensions-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extensions-extension"></a> extension<br><br>For usability reason, this parameter allows removing a single extension. | `String` |  |
| <a id="quarkus-maven-plugin-goal-remove-extensions-extensions"></a> extensions<br><br>The list of extensions to be removed. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-remove-extensions-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-remove-extensions-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-remove-extensions-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-remove-extensions-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-remove-extensions-session"></a> session | `MavenSession` | `${session}` |

### quarkus:run

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-run-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-run-additionalSystemProperties"></a> additionalSystemProperties<br><br>Additional system properties meant to be passed on the command line in a formal like -DsysProps=prop1=val1,prop2=val2 | `String` | `${sysProps}` |
| <a id="quarkus-maven-plugin-goal-run-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-run-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-run-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-run-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-run-environmentVariables"></a> environmentVariables<br><br>The list of environment variables with which the process will be launched. To be specified in a format like -DenvVars=VAR1=val1,VAR2=val2 | `String` | `${envVars}` |
| <a id="quarkus-maven-plugin-goal-run-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-run-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-run-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-run-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-run-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-run-programArguments"></a> programArguments<br><br>The list of program arguments with which the process will be launched. To be specified in a format like -Dargs=1,2,k=v | `String` | `${args}` |
| <a id="quarkus-maven-plugin-goal-run-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-run-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-run-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-run-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-run-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-run-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-run-systemProperties"></a> systemProperties<br><br>The list of system properties defined for the plugin. | `Map` |  |

### quarkus:test

The test mojo, that starts continuous testing outside of dev mode

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-test-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-test-argsString"></a> argsString | `String` | `${quarkus.args}` |
| <a id="quarkus-maven-plugin-goal-test-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-test-compilerArgs"></a> compilerArgs<br><br>Additional parameters to pass to javac when recompiling changed source files. | `List` |  |
| <a id="quarkus-maven-plugin-goal-test-compilerOptions"></a> compilerOptions<br><br>Additional compiler arguments | `List` |  |
| <a id="quarkus-maven-plugin-goal-test-copySurefireVariables"></a> copySurefireVariables<br><br>When enabled, the \<environmentVariables> and \<systemPropertyVariables> elements of the Maven Surefire plugin are copied to environment variables and system properties defined by this plugin. Note that no other Surefire configuration is used (notably \<systemProperties>), only the 2 elements mentioned above. This plugin's \<environmentVariables> and \<systemProperties> have priority, so duplicate keys are not copied.<br><br>Since environment variables and system properties are global to the entire process, this also affects dev mode (when executed as quarkus:dev). Because of that, this copying action is disabled by default and requires opt-in. | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-test-debug"></a> debug<br><br>If this server should be started in debug mode. The default is to start in debug mode and listen on port 5005. Whether the JVM is suspended waiting for a debugger to be attached, depends on the value of suspend. debug supports the following options: Value Effect false The JVM is not started in debug mode true The JVM is started in debug mode and will be listening on debugHost:debugPort client The JVM is started in client mode, and will attempt to connect to debugHost:debugPort {port} The JVM is started in debug mode and will be listening on debugHost:{port}. By default, debugHost has the value "localhost", and debugPort is 5005. | `String` | `${debug}` |
| <a id="quarkus-maven-plugin-goal-test-debugHost"></a> debugHost | `String` | `${debugHost}` |
| <a id="quarkus-maven-plugin-goal-test-debugPort"></a> debugPort | `String` | `${debugPort}` |
| <a id="quarkus-maven-plugin-goal-test-deleteDevJar"></a> deleteDevJar | `boolean` | `TRUE` |
| <a id="quarkus-maven-plugin-goal-test-enforceBuildGoal"></a> enforceBuildGoal<br><br>Whether to enforce the quarkus-maven-plugin build goal to be configured. By default, a missing build goal is considered an inconsistency (although the build goal is not required technically). In this case a warning will be logged and the application will not be started. | `boolean` | `${quarkus.enforceBuildGoal}` |
| <a id="quarkus-maven-plugin-goal-test-environmentVariables"></a> environmentVariables | `Map` |  |
| <a id="quarkus-maven-plugin-goal-test-extensionJvmOptions"></a> extensionJvmOptions<br><br>Extension dev mode JVM option filter configuration. Allows disabling all JVM options configured by extensions, for example \<extensionJvmOptions> \<!-- disable JVM options from all the extensions --> \<disableAll>true\</disableAll> \</extensionJvmOptions>\* or specifying a groupId:artifactId:classifier artifact pattern to disable options provided by the matching subset of extensions, for example \<extensionJvmOptions> \<disableFor> \<!-- disable JVM options from all the extensions with groupId org.acme --> \<extension>org.acme\</extension> \<!-- disable JVM options configured by io.quarkiverse:quarkus-magic --> \<extension>io.quarkiverse:quarkus-magic\</extension> \</disableFor> \</extensionJvmOptions> | `ExtensionDevModeJvmOptionFilter` |  |
| <a id="quarkus-maven-plugin-goal-test-forceC2"></a> forceC2<br><br>This value is intended to be set to true when we want to require C2 compilation instead of preventing it from ever kicking in. Setting this will likely have a small negative effect on startup time and should only be done when it absolutely makes sense. | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-test-jvmArgs"></a> jvmArgs<br><br>Allows configuring arbitrary JVM arguments. Multiple arguments can be specified by delimiting them with a space character. | `String` | `${jvm.args}` |
| <a id="quarkus-maven-plugin-goal-test-modules"></a> modules<br><br>Allows configuring the modules to add to the application. The listed modules will be added using: --add-modules m1,m2.... | `List` | `${add-modules}` |
| <a id="quarkus-maven-plugin-goal-test-mojoExecution"></a> mojoExecution | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-test-noDeps"></a> noDeps<br><br>Whether changes in the projects that appear to be dependencies of the project containing the application to be launched should trigger hot-reload. By default, they do. | `boolean` | `${noDeps}` |
| <a id="quarkus-maven-plugin-goal-test-openJavaLang"></a> openJavaLang | `boolean` | `${open-lang-package}` |
| <a id="quarkus-maven-plugin-goal-test-outputDirectory"></a> outputDirectory<br><br>The directory for compiled classes. | `File` (required) | `${project.build.outputDirectory}` |
| <a id="quarkus-maven-plugin-goal-test-pluginRepos"></a> pluginRepos | `List` (required) | `${project.remotePluginRepositories}` |
| <a id="quarkus-maven-plugin-goal-test-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-test-release"></a> release<br><br>The --release argument to javac. | `String` | `${maven.compiler.release}` |
| <a id="quarkus-maven-plugin-goal-test-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-test-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-test-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-test-skipPlugins"></a> skipPlugins<br><br>A comma-separated list of Maven plugin keys in groupId:artifactId format (for example org.codehaus.mojo:flatten-maven-plugin and/or goal prefixes, (for example flatten) that should be skipped when quarkus:dev identifies Maven plugin goals that should be executed before the application is launched in dev mode. Only the flatten Maven plugin is skipped by default. | `Set` | `org.codehaus.mojo:flatten-maven-plugin` |
| <a id="quarkus-maven-plugin-goal-test-source"></a> source<br><br>The -source argument to javac. | `String` | `${maven.compiler.source}` |
| <a id="quarkus-maven-plugin-goal-test-sourceDir"></a> sourceDir | `File` | `${project.build.sourceDirectory}` |
| <a id="quarkus-maven-plugin-goal-test-suspend"></a> suspend<br><br>Whether the JVM launch, in debug mode, should be suspended. This parameter is only relevant when the JVM is launched in debug mode. This parameter supports the following values (all the allowed values are case-insensitive): Value Effect y or true The debug mode JVM launch is suspended n or false The debug mode JVM is started without suspending | `String` | `${suspend}` |
| <a id="quarkus-maven-plugin-goal-test-systemProperties"></a> systemProperties | `Map` |  |
| <a id="quarkus-maven-plugin-goal-test-target"></a> target<br><br>The -target argument to javac. | `String` | `${maven.compiler.target}` |
| <a id="quarkus-maven-plugin-goal-test-test"></a> test<br><br>Selects given test(s) for continuous testing. This is an alternative to quarkus.test.include-pattern and quarkus.test.exclude-pattern; if set, the quarkus.test.\[include\|exclude\]-pattern configuration is ignored. The format of this configuration property is the same as the Maven Surefire -Dtest=... format \<<https://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html><mark>test>. Specifically: it is a comma (,) separated list of globs of class file paths and/or method names. Each glob can potentially be prefixed with an exclamation mark (!), which makes it an exclusion filter instead of an inclusion filter. Exclusions have higher priority than inclusions. The class file path glob is separated from the method name glob by the hash sign (</mark>) and multiple method name globs may be present, separated by the plus sign (+).<br><br>For example:<br><br>- Basic\*: all classes starting with Basic<br>- ???Test: all classes named with 3 arbitrary characters followed by Test<br>- !Unstable\*: all classes except classes starting with Unstable<br>- pkg/\*\*/Ci\*leTest: all classes in the package pkg and subpackages, starting with Ci and ending with leTest<br>- \*Test#test\*One+testTwo?????: all classes ending with Test, and in them, only methods starting with test and ending with One, or starting with testTwo and followed by 5 arbitrary characters<br>- #fast\*+slowTest: all classes, and in them, only methods starting with fast or methods named slowTest Note that the syntax %regex\[...\] and %ant\[...\] is NOT supported. | `String` |  |
| <a id="quarkus-maven-plugin-goal-test-quarkus.warnIfBuildGoalMissing"></a> quarkus.warnIfBuildGoalMissing | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-test-watchedFiles"></a> watchedFiles<br><br>Optional list of files to watch for changes that trigger a hot reload in dev mode. This is useful for extensions developers that can set this property to their extension's artifacts in their local repository. | `List` |  |
| <a id="quarkus-maven-plugin-goal-test-workingDir"></a> workingDir | `File` |  |

### quarkus:track-config-changes

Maven goal that is executed before the BuildMojo. The goal looks for a file that contains build time configuration options read during the previous build. If that file exists, the goal will check whether the configuration options used during the previous build have changed in the current configuration and will persist their current values to another file, so that both configuration files could be compared by tools caching build goal outcomes to check whether the previous outcome of the BuildMojo needs to be rebuilt.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-track-config-changes-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-track-config-changes-appArtifact"></a> appArtifact<br><br>Coordinates of the Maven artifact containing the original Java application to build the native image for. If not provided, the current project is assumed to be the original Java application. The coordinates are expected to be expressed in the following format:<br><br>groupId:artifactId:classifier:type:version<br><br>With the classifier, type and version being optional.<br><br>If the type is missing, the artifact is assumed to be of type JAR.<br><br>If the version is missing, the artifact is going to be looked up among the project dependencies using the provided coordinates.<br><br>However, if the expression consists of only three parts, it is assumed to be groupId:artifactId:version.<br><br>If the expression consists of only four parts, it is assumed to be groupId:artifactId:classifier:type. | `String` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-bootstrapId"></a> bootstrapId<br><br>Application bootstrap provider ID. This parameter is not supposed to be configured by the user. To be able to re-use an application bootstrapped in one phase in a later phase, there needs to be a way to identify the correct instance of the bootstrapped application (in case there are more than one) in each Mojo. A bootstrap ID serves this purpose. This parameter is set in DevMojo invoking generate-code and generate-code-tests goals. If this parameter is not configured, a Mojo execution ID will be used as the bootstrap ID. | `String` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-buildDir"></a> buildDir | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkusCloseBootstrappedApp"></a> quarkusCloseBootstrappedApp<br><br>Whether to close the bootstrapped applications after the execution | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.track-config-changes.dependencies-file"></a> quarkus.track-config-changes.dependencies-file<br><br>Dependency dump file | `File` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.track-config-changes.dump-current-when-recorded-unavailable"></a> quarkus.track-config-changes.dump-current-when-recorded-unavailable<br><br>Whether to dump the current build configuration in case the configuration from the previous build isn't found | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.track-config-changes.dump-dependencies"></a> quarkus.track-config-changes.dump-dependencies<br><br>Whether to dump Quarkus application dependencies along with their checksums | `boolean` | `true` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-finalName"></a> finalName | `String` | `${project.build.finalName}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-ignoredEntries"></a> ignoredEntries<br><br>When building an uber-jar, this array specifies entries that should be excluded from the final jar. The entries are relative to the root of the file. An example of this configuration could be: \<configuration> \<uberJar>true\</uberJar> \<ignoredEntries> \<ignoredEntry>META-INF/BC2048KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC2048KE.DSA\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.SF\</ignoredEntry> \<ignoredEntry>META-INF/BC1024KE.DSA\</ignoredEntry> \</ignoredEntries> \</configuration> | `String[]` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-manifestEntries"></a> manifestEntries<br><br>The list of main manifest attributes | `Map` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-manifestSections"></a> manifestSections<br><br>The list of manifest sections | `List` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-launchMode"></a> launchMode | `String` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-mojoExecution"></a> mojoExecution<br><br>The context of the execution of the plugin. | `MojoExecution` (required) | `${mojoExecution}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.track-config-changes.outputDirectory"></a> quarkus.track-config-changes.outputDirectory | `File` | `${project.build.directory}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.track-config-changes.outputFile"></a> quarkus.track-config-changes.outputFile | `File` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-project"></a> project | `MavenProject` (required) | `${project}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-properties"></a> properties<br><br>The properties of the plugin. | `Map` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.recorded-build-config.directory"></a> quarkus.recorded-build-config.directory | `File` | `${basedir}/.quarkus` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.recorded-build-config.file"></a> quarkus.recorded-build-config.file | `String` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-reloadPoms"></a> reloadPoms<br><br>POM files from the workspace that should be reloaded from the disk instead of taken from the Maven reactor. This parameter is not supposed to be configured by a user. | `Set` |  |
| <a id="quarkus-maven-plugin-goal-track-config-changes-repoSession"></a> repoSession<br><br>The current repository/network configuration of Maven. | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-repos"></a> repos<br><br>The project's remote repositories to use for the resolution of artifacts and their dependencies. | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-track-config-changes-quarkus.track-config-changes.skip"></a> quarkus.track-config-changes.skip<br><br>Skip the execution of this mojo | `boolean` | `false` |

### quarkus:update

Suggest project updates and create a recipe with the possibility to apply it.

|  |  |  |
| --- | --- | --- |
| <a id="quarkus-maven-plugin-goal-update-parameter-table"></a> Parameter | Type | Default value |
| <a id="quarkus-maven-plugin-goal-update-bomArtifactId"></a> bomArtifactId | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-bomGroupId"></a> bomGroupId | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-bomVersion"></a> bomVersion | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-noRewrite"></a> noRewrite<br><br>Disable the rewrite feature. Deprecated: use -Drewrite=false instead | `Boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-update-perModule"></a> perModule<br><br>Deprecated: this option was unused | `boolean` |  |
| <a id="quarkus-maven-plugin-goal-update-platformVersion"></a> platformVersion<br><br>Version of the target platform (e.g: 2.0.0.Final) You may instead use stream to target the latest version of a specific platform stream. | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-project"></a> project | `MavenProject` | `${project}` |
| <a id="quarkus-maven-plugin-goal-update-refresh"></a> refresh<br><br>Whether to refresh the local extension catalog cache before searching for the recommended Quarkus extension versions | `boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-update-repoSession"></a> repoSession | `RepositorySystemSession` | `${repositorySystemSession}` |
| <a id="quarkus-maven-plugin-goal-update-repos"></a> repos | `List` (required) | `${project.remoteProjectRepositories}` |
| <a id="quarkus-maven-plugin-goal-update-rewrite"></a> rewrite<br><br>Run the suggested update recipe for this project. | `Boolean` |  |
| <a id="quarkus-maven-plugin-goal-update-additionalUpdateRecipes"></a> additionalUpdateRecipes<br><br>Specify a list of additional artifacts (GAV) containing rewrite recipes | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-rewriteDryRun"></a> rewriteDryRun<br><br>Do a dry run the suggested update recipe for this project. | `Boolean` | `false` |
| <a id="quarkus-maven-plugin-goal-update-rewritePluginVersion"></a> rewritePluginVersion<br><br>The OpenRewrite plugin version | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-quarkusUpdateRecipes"></a> quarkusUpdateRecipes<br><br>Use custom io.quarkus:quarkus-update-recipes:LATEST coords (GAV) or just provide the version. This artifact should contain the base Quarkus update recipes to update a project. | `String` |  |
| <a id="quarkus-maven-plugin-goal-update-session"></a> session | `MavenSession` | `${session}` |
| <a id="quarkus-maven-plugin-goal-update-stream"></a> stream<br><br>Target stream (e.g: 2.0) | `String` |  |
