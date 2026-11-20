# JLink packaging

The `jlink` extension produces a custom Java runtime image for a Quarkus application. The image contains only the JDK modules and application modules that are needed to run the application, resulting in a smaller, faster, and more self-contained distribution. This extension depends on the [Quarkus modularity extension](/guides/modularity/index.md) to compute the application's module graph.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Prerequisites

The `jlink` extension requires Java 25 or later. Earlier JDK versions have compatibility issues with the `jlink` tool that prevent reliable image generation.

## Usage

Add the `quarkus-jlink` extension to your project. When the extension is present, a `jlink` image is produced automatically as part of the normal Quarkus build. JAR packaging is disabled when `jlink` is active.

**CLI**

```bash
quarkus extension add quarkus-jlink
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-jlink'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-jlink'
```

After adding the extension, build the project normally:

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

The `jlink` image is written to the output directory (by default, `target/jlink-output/image`).

## Running the image

To run the application, execute the launcher script in the image's `bin` directory:

```bash
./target/jlink-output/image/bin/<launcher-name>
```

The launcher name currently defaults to `my-app`. A future release will derive the default from `quarkus.package.output-name`.

## Image layout

The output directory has the standard `jlink` image structure. The `bin/` directory contains the launcher script. The `lib/` directory contains the JDK runtime modules. Application modules that are not on the boot module path are placed in `lib/quarkus/` and are loaded dynamically on demand after startup by `smallrye-modules`. See the [modularity guide](/guides/modularity/index.md) for an explanation of boot modules and dynamic modules.

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-jlink_quarkus-jlink-enabled"></a> [`quarkus.jlink.enabled`](#quarkus-jlink_quarkus-jlink-enabled) config_property_copy_button:+++quarkus.jlink.enabled+++[]<br><br>Whether `jlink` image generation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-jlink_quarkus-jlink-min-heap-size"></a> [`quarkus.jlink.min-heap-size`](#quarkus-jlink_quarkus-jlink-min-heap-size) config_property_copy_button:+++quarkus.jlink.min-heap-size+++[]<br><br>The minimum heap size to configure for the image. If not given, no minimum heap size is specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_MIN_HEAP_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-jlink_quarkus-jlink) |  |
| [lock] <a id="quarkus-jlink_quarkus-jlink-max-heap-size"></a> [`quarkus.jlink.max-heap-size`](#quarkus-jlink_quarkus-jlink-max-heap-size) config_property_copy_button:+++quarkus.jlink.max-heap-size+++[]<br><br>The maximum heap size to configure for the image. If not given, no maximum heap size is specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_MAX_HEAP_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-jlink_quarkus-jlink) |  |
| [lock] <a id="quarkus-jlink_quarkus-jlink-image-path"></a> [`quarkus.jlink.image-path`](#quarkus-jlink_quarkus-jlink-image-path) config_property_copy_button:+++quarkus.jlink.image-path+++[]<br><br>The image output path. If relative, it will be resolved in terms of the packaging output path.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_IMAGE_PATH+++[] | path | `image` |
| [lock] <a id="quarkus-jlink_quarkus-jlink-launcher-name"></a> [`quarkus.jlink.launcher-name`](#quarkus-jlink_quarkus-jlink-launcher-name) config_property_copy_button:+++quarkus.jlink.launcher-name+++[]<br><br>The name of the launcher script generated in the image's `bin/` directory.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_LAUNCHER_NAME+++[] | string | `my-app` |
| [lock] <a id="quarkus-jlink_quarkus-jlink-output-directory"></a> [`quarkus.jlink.output-directory`](#quarkus-jlink_quarkus-jlink-output-directory) config_property_copy_button:+++quarkus.jlink.output-directory+++[]<br><br>The base output directory for the `jlink` build.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_OUTPUT_DIRECTORY+++[] | path | `target/jlink-output` |
| [lock] <a id="quarkus-jlink_quarkus-jlink-staging-directory"></a> [`quarkus.jlink.staging-directory`](#quarkus-jlink_quarkus-jlink-staging-directory) config_property_copy_button:+++quarkus.jlink.staging-directory+++[]<br><br>The path of the staging directory used during the `jlink` build process, relative to the output directory.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_JLINK_STAGING_DIRECTORY+++[] | path | `staging` |

<a id="memory-size-note-anchor-quarkus-jlink_quarkus-jlink"></a>

> [!NOTE]
> **About the MemorySize format**
>
> A size configuration option recognizes strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.
>
> If no suffix is given, assume bytes.

## CDS support

Support for Class Data Sharing (CDS) and AOT precompilation within jlink images is planned for a future release.

CDS currently only applies to modules on the boot module path. Dynamic modules loaded by `smallrye-modules` are not eligible for CDS precompilation at this time.
