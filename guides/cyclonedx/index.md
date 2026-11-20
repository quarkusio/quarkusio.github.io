# Generating and using CycloneDX SBOMs

An SBOM (Software Bill of Materials) is a manifest that describes what a given software distribution consists of in terms of components. In addition to that, it may include a lot more information such as relationships between those components, licenses, provenance, etc. SBOMs are typically used by software security and software supply chain risk management tools to perform vulnerability and compliance related analysis.

This guide covers:

- [Dependency SBOMs](#dependency-sboms) — generating SBOMs that manifest application dependencies before a build
- [Embedded Dependency SBOMs](#embedded-dependency-sboms) — embedding dependency SBOMs into applications as classpath resources
- [SBOM REST Endpoint](#sbom-endpoint) — exposing embedded dependency SBOMs via a REST endpoint
- [Distribution SBOMs](#distribution-sboms) — generating SBOMs that manifest the final application distribution after a build
- [Vulnerability scanning](#vulnerability-scanning) — using generated SBOMs with tools such as Grype to identify known vulnerabilities
- [Serial number](#serial-number) — how the SBOM serial number is generated and how to make it reproducible
- [Product attribution](#product-attribution) — representing platform-member products (offerings) as SBOM components
- [Extension SBOM contributions](#extension-sbom-contributions) — contributing additional components to the application SBOM from Quarkus extensions

All SBOM generation in this guide follows the [CycloneDX](https://cyclonedx.org/) specification.

## Why Quarkus-specific tooling?

While Quarkus integrates with build tools such as [Maven](https://maven.apache.org/) and [Gradle](https://gradle.org/), it could itself be categorized as a build tool with its own component and dependency model, build steps, and build outcomes. One of the essential component types of a Quarkus application is a Quarkus extension, which consists of runtime and build time artifacts, and their dependencies.

To properly resolve Quarkus extension and other application dependencies, Quarkus uses its own dependency resolver, which is implemented on top of the dependency resolver provided by the underlying build tool: Maven or Gradle.

As a consequence, in the case of Maven, for example, the results of `dependency:tree` will not include all the dependencies Quarkus will use to build an application. A similar issue will affect other dependency analysis tools that assume a project adheres to the standard Maven dependency model: they will not be able to capture the effective Quarkus application dependency graph. Unfortunately, that includes the implementation of the [CycloneDX Maven plugin](https://github.com/CycloneDX/cyclonedx-maven-plugin).

In addition to manifesting dependencies (the input to a build), there is also an outcome of the build that is the final distribution of an application. Users of an application may request an SBOM manifesting not only the dependencies (the input to a build) but also the final distribution (the outcome of the build) before they agree to deploy the application. Quarkus allows application developers to choose various packaging types for their applications, some of which are Quarkus-specific. Providing certain Quarkus-specific details about components included in a distribution may help better evaluate the impact of potential security-related issues.

<a id="dependency-sboms"></a>
## Dependency SBOMs

Dependency SBOMs manifest the dependencies of an application before it is built. In other words, they describe the input to a build. These SBOMs can be used to perform vulnerability and compliance related analysis before building applications.

### Maven Dependency SBOMs

For Quarkus Maven projects, dependency SBOMs can be generated with the `quarkus:dependency-sbom` goal. The output of the goal will be saved in a `target/<artifactId>-<version>-dependency-cyclonedx.json` file (which can be changed by setting the `outputFile` goal parameter or the `quarkus.dependency.sbom.output-file` property). The complete Quarkus build and runtime dependency graphs will be recorded in the [CycloneDX](https://cyclonedx.org/) `JSON` format.

`XML` format can be requested by setting the `format` goal parameter (or `quarkus.dependency.sbom.format` property) to `xml`.

Development/build-time dependencies will have their CycloneDX `scope` set to `excluded` (see [Build time dependencies](#build-time-dependencies)), while runtime dependencies omit the `scope` field (defaulting to `required` per the specification).

Optionally, the `quarkus:component:scope` custom property can be included on each component by setting `includeQuarkusComponentScope` to `true` (or `-Dquarkus.dependency.sbom.include-quarkus-component-scope=true`). When enabled, each component will carry a property indicating whether it is a `runtime` or `development` dependency:

```json
        {
          "name" : "quarkus:component:scope",
          "value" : "runtime"
        }
```

The complete set of parameters and their description can be obtained by executing `mvn help:describe -Dcmd=quarkus:dependency-sbom -Ddetail`.

#### Bootstrap Modes

By default, `quarkus:dependency-sbom` captures the dependencies of a production build. Quarkus supports three application bootstrap modes: normal (production), test, and dev. In all three modes, an application may have different dependency graphs.

The `mode` parameter can be used to indicate which dependency graph should be recorded. If the `mode` is set to `test` or `dev`, the target file name will become `target/<artifactId>-<version>-<mode>-dependency-cyclonedx.json`.

### Gradle Dependency SBOMs

Unlike Maven, the [Gradle CycloneDX plugin implementation](https://github.com/CycloneDX/cyclonedx-gradle-plugin) can be used in Quarkus projects to generate dependency SBOMs, since the implementation manifests dependency configurations registered by configured plugins.

Please refer to the [Gradle CycloneDX plugin](https://github.com/CycloneDX/cyclonedx-gradle-plugin) documentation for its configuration options. Here is a list of Quarkus dependency configurations that would be relevant for manifesting:

- `quarkusProdRuntimeClasspathConfiguration` - Quarkus application production runtime dependencies;
- `quarkusProdRuntimeClasspathConfigurationDeployment` - Quarkus application production runtime and build time dependencies;
- `quarkusTestRuntimeClasspathConfiguration` - Quarkus application test runtime dependencies;
- `quarkusTestRuntimeClasspathConfigurationDeployment` - Quarkus application test runtime and build time dependencies;
- `quarkusDevRuntimeClasspathConfiguration` - Quarkus application dev mode runtime dependencies;
- `quarkusDevRuntimeClasspathConfigurationDeployment` - Quarkus application dev mode runtime and build time dependencies.

For example, to generate an SBOM for the production runtime dependencies only:

```groovy
cyclonedxBom {
    includeConfigs = ["quarkusProdRuntimeClasspathConfiguration"]
}
```

Given that the plugin is not aware of how Quarkus uses these dependencies, it will not be able to set the CycloneDX `scope` field or the `quarkus:component:scope` property for components. On the other hand, the requested configuration name can be used to indicate which scope to target.

<a id="embedded-dependency-sboms"></a>
## Embedded Dependency SBOMs

In addition to generating dependency SBOMs as separate files, Quarkus can embed a dependency SBOM directly into the built application as a classpath resource. This allows the application to carry its own bill of materials at runtime, which can be useful for runtime auditing or to expose via a REST endpoint (see [SBOM REST Endpoint](#sbom-endpoint)).

To embed a dependency SBOM in the application:

```properties
quarkus.cyclonedx.embedded.enabled=true
```

The SBOM will be embedded as a classpath resource at `META-INF/sbom/dependency.cdx.json` by default. The resource name can be customized:

```properties
quarkus.cyclonedx.embedded.resource-name=META-INF/custom-sbom.json
```

The format is determined by the resource name extension: `.json` for JSON, `.xml` for XML.

> [!NOTE]
> Embedded dependency SBOMs are also available when running in dev mode (`quarkus dev`).

<a id="sbom-endpoint"></a>
## SBOM REST Endpoint

The embedded dependency SBOM can be exposed through a REST endpoint, making it accessible to external tools and scanners at runtime. To enable the endpoint:

```properties
quarkus.cyclonedx.endpoint.enabled=true
```

The embedded SBOM will be served at `/.well-known/sbom` with the `application/vnd.cyclonedx+json` content type (or `application/vnd.cyclonedx+xml` for XML SBOMs). The path can be customized using the `quarkus.cyclonedx.endpoint.path` property.

Enabling the endpoint will automatically trigger SBOM embedding, even if `quarkus.cyclonedx.embedded.enabled` is not explicitly set to `true`.

The `quarkus.cyclonedx.embedded.compress` option controls whether the response is GZIP-compressed:

- if set to `true`, the SBOM is always served with `Content-Encoding: gzip`;
- if set to `false`, the SBOM is always served uncompressed;
- if not set (the default), compression is negotiated from the request's `Accept-Encoding` header, so the SBOM is served with `Content-Encoding: gzip` only when the client accepts `gzip`.

HTTP clients handle the compressed response transparently.

> [!NOTE]
> The `quarkus.cyclonedx.endpoint.*` configuration options are only available when `quarkus-vertx-http` is present as a direct or transitive dependency of the application, which is the case for most web applications (e.g., those using `quarkus-rest`).

> [!CAUTION]
> Exposing an SBOM endpoint allows anyone with access to it to perform composition analysis of the application, potentially revealing dependency versions with known vulnerabilities. The endpoint should be restricted to approved consumers only by applying appropriate authentication and authorization policies.

For an example of scanning a running application through the endpoint, see [Scanning via the SBOM Endpoint](#scanning-via-endpoint).

The SBOM endpoint is also available when running in dev mode (`quarkus dev`), which can be useful for scanning during development.

### Management Interface

When the [management interface](/guides/management-interface-reference/index.md) is enabled, the SBOM endpoint is automatically served on the management port (default `9000`) instead of the main application port:

```properties
quarkus.management.enabled=true
```

The SBOM will then be accessible at `http://localhost:9000/.well-known/sbom`. Authentication and authorization policies can be applied to the management interface to restrict access — see [Management Interface](/guides/management-interface-reference/index.md) for details.

<a id="distribution-sboms"></a>
## Distribution SBOMs

Distribution SBOMs manifest the outcomes of Quarkus builds — the final application distributions. Unlike dependency SBOMs, which describe the input to a build, distribution SBOMs describe what was actually produced.

During the build and package assembly process, Quarkus captures details about the produced distribution and allows an SBOM generator to record that information in an SBOM format.

To generate CycloneDX distribution SBOMs, add `io.quarkus:quarkus-cyclonedx` as a project dependency. It will generate SBOMs every time an application is built. SBOMs will be saved in the project's build output directory under `<executable-name>-cyclonedx.<format>` name, where

- `<executable-name>` is the base file name (without the extension) of the executable that launches the application;
- `<format>` is either `json` (the default) or `xml`, which can be configured using the `quarkus.cyclonedx.format` property. If both formats are desired, `quarkus.cyclonedx.format` can be set to `all`.

By default, generated JSON SBOMs are not pretty-printed. To enable pretty-printing:

```properties
quarkus.cyclonedx.pretty-print=true
```

### Libraries-only mode

By default, the generated SBOM includes all components of the application distribution, including generic file components such as non-Maven JARs and other generated files. To include only components identified as libraries (i.e., those with a recognized package type such as Maven or npm), use:

```properties
quarkus.cyclonedx.libraries-only=true
```

When enabled, components that would be classified as `file` type in CycloneDX (generic PURL components with a file system path or distribution path) are excluded from the generated SBOM along with their dependency relationships.

### Runtime-only mode

By default, the generated SBOM includes both runtime and development/build-time dependencies. To include only runtime dependencies, use:

```properties
quarkus.cyclonedx.runtime-only=true
```

When enabled, development/build-time dependencies (those with CycloneDX `scope` set to `excluded`) are excluded entirely from the generated SBOM along with their dependency relationships. This can be useful when the SBOM is intended for runtime vulnerability scanning and build-time components are not relevant.

### CycloneDX schema version

By default, the SBOM is generated using the latest CycloneDX schema version supported by the integrated library. A specific version can be requested:

```properties
quarkus.cyclonedx.schema-version=1.5
```

### License text

License text is not included in generated SBOMs by default. To include the full text of resolved licenses:

```properties
quarkus.cyclonedx.include-license-text=true
```

### Pedigree

Pedigree is a way to provide information that patches or modifications have been applied to a component.

An application developer may instruct Quarkus to remove some resources or classes from specific dependencies to exclude them from the application classpath. Manipulating the original content of an artifact will change its digest, which may get highlighted as suspicious by the tools comparing original component digest to the one found in the distribution.

In such cases, when Quarkus generates an SBOM, it will add a pedigree note for a modified artifact documenting what content was removed from the artifact.

For example, if an application developer decided to remove certain classpath resources from a dependency, such as

```properties
quarkus.class-loading.removed-resources."jakarta.transaction\:jakarta.transaction-api"=META-INF/NOTICE.md,jakarta/transaction/package.html
```

The resulting SBOM will include

```json
      "purl" : "pkg:maven/jakarta.transaction/jakarta.transaction-api@2.0.1?type=jar",
      "pedigree" : {
        "notes" : "Removed META-INF/NOTICE.md,jakarta/transaction/package.html"
      },
```

### Fast JAR

Fast JAR packaging uses a Quarkus-specific filesystem directory layout that contains files generated by Quarkus and Maven artifacts that are runtime dependencies of an application.

SBOMs for Fast JAR packaging type will use the executable JAR file as their main component and record both runtime and build time Quarkus application dependencies.

#### Runtime Components

Every file in the resulting Fast JAR distribution will appear in the SBOM with an `evidence.occurrences.location` field pointing to the location of the component in the application distribution directory. Runtime components do not set the CycloneDX `scope` field, which defaults to `required` per the specification. For example:

```json
      "purl" : "pkg:maven/org.jboss.slf4j/slf4j-jboss-logmanager@2.0.0.Final?type=jar",
      "evidence" : {
        "occurrences" : [
          {
            "location" : "lib/main/org.jboss.slf4j.slf4j-jboss-logmanager-2.0.0.Final.jar"
          }
        ]
      }
```

> [!NOTE]
> `evidence.occurrences.location` was introduced in CycloneDX schema version 1.5, for older versions the location will be indicated using the `quarkus:component:location` property.

<a id="build-time-dependencies"></a>
#### Build time dependencies

Build time dependencies will have their CycloneDX `scope` set to `excluded`:

```json
      "purl" : "pkg:maven/org.apache.httpcomponents/httpclient@4.5.14?type=jar",
      "scope" : "excluded"
```

The `excluded` scope indicates that these components are not reachable within a call graph at runtime, as defined by the CycloneDX specification. Runtime components omit the `scope` field, which defaults to `required` per the specification.

They will not include `evidence.occurrences.location` since they will not be found in the distribution.

### Uber JAR

SBOMs for Uber JARs will use the Uber JAR Maven artifact as their main component.

Since an Uber JAR is published as a Maven artifact itself, SBOMs generated for Uber JARs will also be automatically published as Maven artifacts. This can be disabled by setting the `attachSboms` parameter of the `quarkus:build` goal to `false`:

```xml
<plugin>
    <groupId>${quarkus.platform.group-id}</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <configuration>
        <attachSboms>false</attachSboms>
    </configuration>
</plugin>
```

Gradle users will have to explicitly configure a publishing plugin to deploy SBOMs as Maven artifacts.

Runtime components in an SBOM generated for an Uber JAR will not include `evidence.occurrences.location` since their content is merged in a single JAR file.

### Native image

SBOMs for native images will use the native executable file as their main component.

Since native executables are not currently attached to projects as Maven artifacts, their SBOMs will not be attached as Maven artifacts either.

As in the case of an Uber JAR, runtime components in an SBOM generated for a native executable will not include `evidence.occurrences.location` since their corresponding code and resources are included in a single native executable file.

#### Embedded SBOM in native executables

When [SBOM embedding](#embedded-dependency-sboms) is enabled, the SBOM is also embedded directly into the native executable as global symbols (`sbom` and `sbom_length`), following the [GraalVM SBOM specification](https://www.graalvm.org/jdk25/security-guide/native-image/sbom/). This allows tools such as `syft` to extract the SBOM from the binary:

```bash
podman run --rm -v $(pwd):/tmp:z anchore/syft scan myapp -o cyclonedx-json
```

> [!NOTE]
> The SBOM is always GZIP-compressed before being embedded as a global symbol, as required by the GraalVM SBOM specification and regardless of the `quarkus.cyclonedx.embedded.compress` setting. Since the classpath resource is always stored uncompressed, Quarkus compresses it at native image build time.

### Mutable JAR

Mutable JAR distribution is similar to the Fast JAR one except it also includes build time dependencies to support re-augmentation (re-building) of an application.

SBOMs generated for Mutable JAR distributions will also record locations of components that will be used during re-augmentation process using `evidence.occurrences.location` but keeping their CycloneDX `scope` set to `excluded`. For example:

```json
      "purl" : "pkg:maven/org.apache.httpcomponents/httpcore@4.4.16?type=jar",
      "scope" : "excluded",
      "evidence" : {
        "occurrences" : [
          {
            "location" : "lib/deployment/org.apache.httpcomponents.httpcore-4.4.16.jar"
          }
        ]
      }
```

<a id="vulnerability-scanning"></a>
## Vulnerability Scanning with SBOMs

SBOMs can be fed into vulnerability scanners such as [Grype](https://github.com/anchore/grype) to identify known vulnerabilities in application dependencies. This section demonstrates how to use Quarkus-generated SBOMs with Grype.

### Sample Project Setup

Create a sample project with a REST endpoint, CycloneDX SBOM generation, and `org.codehaus.plexus:plexus-utils:3.6.0` — a dependency with a known security vulnerability added here on purpose for demonstration:

**CLI**

```bash
quarkus create app org.acme:my-rest-app \
    --extension='rest,cyclonedx,org.codehaus.plexus:plexus-utils:3.6.0'
cd my-rest-app
```

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=my-rest-app \
    -Dextensions='rest,cyclonedx,org.codehaus.plexus:plexus-utils:3.6.0'
cd my-rest-app
```

**Gradle**

```bash
quarkus create app org.acme:my-rest-app \
    --extension='rest,cyclonedx,org.codehaus.plexus:plexus-utils:3.6.0' \
    --gradle
cd my-rest-app
```

### Scanning Distribution SBOMs

Build the application:

**Maven**

```bash
./mvnw package
```

**Gradle**

```bash
./gradlew build
```

Scan the generated distribution SBOM:

**Maven**

```bash
grype target/quarkus-run-cyclonedx.json
```

**Gradle**

```bash
grype build/quarkus-run-cyclonedx.json
```

Grype should report the known vulnerability in `plexus-utils:3.6.0`:

```
NAME          INSTALLED  FIXED IN  TYPE          VULNERABILITY         SEVERITY  EPSS        RISK
plexus-utils  3.6.0      3.6.1     java-archive  GHSA-6fmv-xxpf-w3cw   High      0.2% (47th) 0.2
```

<a id="scanning-via-endpoint"></a>
### Scanning via the SBOM Endpoint

With the SBOM endpoint enabled, a running application can be scanned without access to the build output. This works regardless of the packaging type, including JARs and native executables.

Enable the endpoint by adding the following property to `src/main/resources/application.properties`:

```properties
quarkus.cyclonedx.endpoint.enabled=true
```

This can also be done from the command line:

**Linux / macOS**

```bash
echo 'quarkus.cyclonedx.endpoint.enabled=true' >> src/main/resources/application.properties
```

**Windows (PowerShell)**

```powershell
Add-Content src\main\resources\application.properties 'quarkus.cyclonedx.endpoint.enabled=true'
```

Build the application:

**Maven**

```bash
./mvnw package
```

**Gradle**

```bash
./gradlew build
```

Start the application:

**Maven**

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

**Gradle**

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

In another terminal, scan the running application:

```bash
curl -s http://localhost:8080/.well-known/sbom | grype
```

The output should be the same as when scanning the distribution SBOM:

```
NAME          INSTALLED  FIXED IN  TYPE          VULNERABILITY        SEVERITY  EPSS        RISK
plexus-utils  3.6.0      3.6.1     java-archive  GHSA-6fmv-xxpf-w3cw  High      0.2% (47th) 0.2
```

This approach is useful for scanning applications that are already deployed, for example as part of a periodic audit or a CI/CD pipeline that verifies running instances.

<a id="serial-number"></a>
## Serial Number

Every generated SBOM includes a `serialNumber` field in `urn:uuid:` format, as defined by the CycloneDX specification. The serial number is derived from the BOM's content hash, which covers metadata, components, dependency relationships, and other structural fields.

When `project.build.outputTimestamp` is configured in the Maven project (the standard property for [reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html)), the SBOM metadata timestamp is set to that value, making the entire BOM — including its serial number — fully deterministic across identical builds. Without a reproducible timestamp, the metadata timestamp defaults to the current time, meaning the serial number will vary between builds even if the dependencies have not changed.

<a id="product-attribution"></a>
## Product Attribution

Quarkus platforms (such as the Quarkus platform BOM or vendor-provided platforms) may aggregate extensions that belong to commercially supported products, also referred to as *offerings*. When an application is built on such a platform, product attribution represents each supported product as a dedicated component in the generated SBOM and links the application artifacts that belong to that product to it. This lets downstream tools see which parts of an application are covered by a given product and match them against product-level vulnerability and support information, for example by [CPE](https://nvd.nist.gov/products/cpe).

Product attribution is enabled by default and applies to every SBOM that includes the core application contribution (dependency, embedded, and distribution SBOMs). It can be disabled with:

```properties
quarkus.cyclonedx.product-attribution=false
```

### How it works

Attribution is driven entirely by metadata the platform provides; an application does not configure products directly. Each imported platform member that declares a product identity (at minimum a CPE) yields, in the generated SBOM, a single product component that:

- has the type `framework` (unless the platform specifies a different type), carries the product's CPE, and has its CycloneDX `scope` set to `excluded`, since the product itself is not a shipped artifact;
- is marked top-level, so it appears as a direct dependency of the main application component;
- *provides* the artifacts attributed to it — the application's runtime extension artifacts that belong to the product, together with their dependencies. These are recorded as CycloneDX 1.6 `dependency.provides` relationships (the product provides/implements those artifacts). On schema versions older than 1.6, where `provides` is not available, the same artifacts are recorded as `dependsOn` relationships instead.

Platform members that declare the **same** product PURL and CPE are merged into a single product component whose attributed artifacts are the union of the members' contributions. Members with different product PURLs remain separate components even if they share a CPE.

Only artifacts that are actually present in the application and match the coordinates the platform attributed to the product are linked. If the application overrides the version of an attributed dependency, that dependency is not attributed to the product.

> [!NOTE]
> A product component is emitted whenever a platform member declares a CPE, even if none of its attributed artifacts end up present in the application.

## SBOM Component Properties

| Name | Value range | Description |
| --- | --- | --- |
| `quarkus:component:scope` | `runtime` or `development` | Indicates whether a component is a runtime or a build/development time dependency of an application. Disabled by default; enable with `quarkus.cyclonedx.include-quarkus-component-scope=true`. Development components always have their CycloneDX `scope` set to `excluded` regardless of this setting. |
| `quarkus:component:location` | String representing a file system path using `/` as a path element | Used in SBOMs with schema versions 1.4 or older. Starting from schema 1.5, `evidence.occurrences.location` is used instead. This property is used only if a component is found in the distribution. The value is a relative path to a file pointing to the location of a component in a distribution using `/` as a path element separator. |

<a id="extension-sbom-contributions"></a>
## Extension SBOM Contributions

By default, Quarkus generates SBOMs from the application's Maven dependency model and the packaging output. Extensions that manage additional software components — for example, npm packages or other non-Maven artifacts — can contribute those components to the application SBOM by producing `SbomContributionBuildItem` instances from their `@BuildStep` methods.

Extension contributions are merged with the core application contribution by the SBOM generator. Components marked as top-level will appear as direct dependencies of the main application component in the generated SBOM.

### API Overview

Extension SBOM contributions are built from the following classes:

- `io.quarkus.sbom.Purl` — identifies a software component using the [Package URL](https://github.com/package-url/purl-spec) specification. Factory methods are provided for Maven, npm, and generic component types.
- `io.quarkus.sbom.ComponentDescriptor` — describes a software component with its PURL, scope, integrity hash, and other metadata.
- `io.quarkus.sbom.ComponentDependencies` — describes the dependency relationships of a component, referencing other components by their bom-ref.
- `io.quarkus.sbom.SbomContribution` — groups component descriptors and their dependency relationships into a single contribution.
- `io.quarkus.deployment.sbom.SbomContributionBuildItem` — a `MultiBuildItem` that carries an `SbomContribution` to the SBOM generator.

### Example

The following example shows a `@BuildStep` method that contributes npm packages to the application SBOM:

```java
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.sbom.SbomContributionBuildItem;
import io.quarkus.sbom.ComponentDependencies;
import io.quarkus.sbom.ComponentDescriptor;
import io.quarkus.sbom.Purl;
import io.quarkus.sbom.SbomContribution;

@BuildStep
SbomContributionBuildItem contributeNpmComponents() {

    // Describe an npm package
    ComponentDescriptor react = ComponentDescriptor.builder()
            .setPurl(Purl.npm(null, "react", "18.2.0"))
            .setScope(ComponentDescriptor.SCOPE_RUNTIME)
            .setIntegrity("sha512-...")  // SRI hash from package-lock.json
            .setTopLevel(true)           // direct dependency of the application
            .build();

    // Describe a transitive npm dependency
    ComponentDescriptor loosEnvify = ComponentDescriptor.builder()
            .setPurl(Purl.npm(null, "loose-envify", "1.4.0"))
            .setScope(ComponentDescriptor.SCOPE_RUNTIME)
            .build();

    // Describe the dependency relationship: react depends on loose-envify
    ComponentDependencies reactDeps = ComponentDependencies.builder()
            .setBomRef(react.getBomRef())
            .addDependsOn(loosEnvify.getBomRef())
            .build();

    // Build the contribution
    SbomContribution contribution = SbomContribution.builder()
            .addComponent(react)
            .addComponent(loosEnvify)
            .addDependency(reactDeps)
            .build();

    return new SbomContributionBuildItem(contribution);
}
```

### Key Points

- Extensions must not designate a main component — only the core application contribution does that.
- Components from any package ecosystem can be contributed using the appropriate `Purl` type (e.g., `Purl.npm(...)`, `Purl.maven(...)`, `Purl.generic(...)`, or `Purl.of(type, ...)` for other ecosystems).
- Set `topLevel(true)` on components that are direct dependencies of the application (e.g., packages declared in `package.json`). Transitive dependencies should not be marked as top-level.
- The `scope` field classifies components as `runtime` or `development` dependencies. Development components have their CycloneDX `scope` set to `excluded`. When `quarkus.cyclonedx.include-quarkus-component-scope` is enabled, the scope is also recorded as the `quarkus:component:scope` custom property.
- The `integrity` field can carry an SRI-format hash (e.g., from lock files) for component verification.
- Dependency relationships are optional. If you only need to list components without expressing their dependency graph, use `SbomContribution.ofComponents(...)`.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-enabled"></a> [`quarkus.cyclonedx.enabled`](#quarkus-cyclonedx_quarkus-cyclonedx-enabled) config_property_copy_button:+++quarkus.cyclonedx.enabled+++[]<br><br>Whether to CycloneDX SBOM generation is enabled. If this option is false, the rest of the configuration will be ignored.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-format"></a> [`quarkus.cyclonedx.format`](#quarkus-cyclonedx_quarkus-cyclonedx-format) config_property_copy_button:+++quarkus.cyclonedx.format+++[]<br><br>SBOM file format. Supported formats are ++{++code json++}++ and ++{++code xml++}++. The default format is JSON. If both are desired then `all` could be used as the value of this option.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_FORMAT+++[] | string | `json` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-schema-version"></a> [`quarkus.cyclonedx.schema-version`](#quarkus-cyclonedx_quarkus-cyclonedx-schema-version) config_property_copy_button:+++quarkus.cyclonedx.schema-version+++[]<br><br>CycloneDX specification version. The default value be the latest supported by the integrated CycloneDX library.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_SCHEMA_VERSION+++[] | string |  |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-include-license-text"></a> [`quarkus.cyclonedx.include-license-text`](#quarkus-cyclonedx_quarkus-cyclonedx-include-license-text) config_property_copy_button:+++quarkus.cyclonedx.include-license-text+++[]<br><br>Whether to include the license text into generated SBOMs.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_INCLUDE_LICENSE_TEXT+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-pretty-print"></a> [`quarkus.cyclonedx.pretty-print`](#quarkus-cyclonedx_quarkus-cyclonedx-pretty-print) config_property_copy_button:+++quarkus.cyclonedx.pretty-print+++[]<br><br>Whether to pretty-print the generated SBOM output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_PRETTY_PRINT+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-libraries-only"></a> [`quarkus.cyclonedx.libraries-only`](#quarkus-cyclonedx_quarkus-cyclonedx-libraries-only) config_property_copy_button:+++quarkus.cyclonedx.libraries-only+++[]<br><br>Whether to include only library components in generated SBOMs, excluding generic file components such as non-Maven JARs and other files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_LIBRARIES_ONLY+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-runtime-only"></a> [`quarkus.cyclonedx.runtime-only`](#quarkus-cyclonedx_quarkus-cyclonedx-runtime-only) config_property_copy_button:+++quarkus.cyclonedx.runtime-only+++[]<br><br>Whether to include only runtime dependencies in generated SBOMs, excluding development/build-time dependencies entirely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_RUNTIME_ONLY+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-include-quarkus-component-scope"></a> [`quarkus.cyclonedx.include-quarkus-component-scope`](#quarkus-cyclonedx_quarkus-cyclonedx-include-quarkus-component-scope) config_property_copy_button:+++quarkus.cyclonedx.include-quarkus-component-scope+++[]<br><br>Whether to include the `quarkus:component:scope` custom property on each component in generated SBOMs. This property indicates whether a component is a `runtime` or `development` dependency. Since development dependencies are now also marked with the standard CycloneDX `scope` set to `excluded`, the custom property is redundant for most consumers and is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_INCLUDE_QUARKUS_COMPONENT_SCOPE+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-product-attribution"></a> [`quarkus.cyclonedx.product-attribution`](#quarkus-cyclonedx_quarkus-cyclonedx-product-attribution) config_property_copy_button:+++quarkus.cyclonedx.product-attribution+++[]<br><br>When Quarkus platform members include product information (a CPE and the extensions/artifacts bound to an offering) and this option is enabled, each product is represented in the SBOM as a top-level component of type `framework` that `provides` the artifacts attributed to it (CycloneDX 1.6 `dependency.provides`; recorded as `dependsOn` on older schema versions).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_PRODUCT_ATTRIBUTION+++[] | boolean | `true` |
| <a id="quarkus-cyclonedx_section_quarkus-cyclonedx-embedded"></a> [Embedded dependency SBOM configuration](#quarkus-cyclonedx_section_quarkus-cyclonedx-embedded) | Type | Default |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-embedded-enabled"></a> [`quarkus.cyclonedx.embedded.enabled`](#quarkus-cyclonedx_quarkus-cyclonedx-embedded-enabled) config_property_copy_button:+++quarkus.cyclonedx.embedded.enabled+++[]<br><br>Whether a dependency SBOM should be embedded in the final application.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_EMBEDDED_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-embedded-resource-name"></a> [`quarkus.cyclonedx.embedded.resource-name`](#quarkus-cyclonedx_quarkus-cyclonedx-embedded-resource-name) config_property_copy_button:+++quarkus.cyclonedx.embedded.resource-name+++[]<br><br>Resource name for the embedded dependency SBOM. The SBOM is always stored uncompressed under this exact name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_EMBEDDED_RESOURCE_NAME+++[] | string | `META-INF/sbom/dependency.cdx.json` |
| [lock] <a id="quarkus-cyclonedx_quarkus-cyclonedx-embedded-compress"></a> [`quarkus.cyclonedx.embedded.compress`](#quarkus-cyclonedx_quarkus-cyclonedx-embedded-compress) config_property_copy_button:+++quarkus.cyclonedx.embedded.compress+++[]<br><br>Controls whether the embedded SBOM is served GZIP-compressed through the *endpoint*. This option does not affect how the SBOM is stored in the application: the embedded SBOM resource is always stored uncompressed.<br><br>\* if set to `true`, the endpoint always serves the SBOM compressed with `Content-Encoding: gzip`; \* if set to `false`, the endpoint always serves the SBOM uncompressed; \* if not set, the endpoint negotiates the encoding based on the request's `Accept-Encoding` header, serving the SBOM compressed only when the client accepts `gzip`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_EMBEDDED_COMPRESS+++[] | boolean |  |

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-cyclonedx-endpoint_quarkus-cyclonedx-endpoint-enabled"></a> [`quarkus.cyclonedx.endpoint.enabled`](#quarkus-cyclonedx-endpoint_quarkus-cyclonedx-endpoint-enabled) config_property_copy_button:+++quarkus.cyclonedx.endpoint.enabled+++[]<br><br>Whether the embedded SBOM should be exposed through a REST endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_ENDPOINT_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-cyclonedx-endpoint_quarkus-cyclonedx-endpoint-path"></a> [`quarkus.cyclonedx.endpoint.path`](#quarkus-cyclonedx-endpoint_quarkus-cyclonedx-endpoint-path) config_property_copy_button:+++quarkus.cyclonedx.endpoint.path+++[]<br><br>REST endpoint path that will provide an SBOM<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CYCLONEDX_ENDPOINT_PATH+++[] | string | `/.well-known/sbom` |
