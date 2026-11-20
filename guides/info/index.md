# Application info endpoint

The `quarkus-info` extension exposes an HTTP endpoint at `/q/info` that returns information about the application: build metadata, git commit, Java version, and operating system.

## Adding the extension

Add the `quarkus-info` extension to your project:

**CLI**

```bash
quarkus extension add info
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='info'
```

**Gradle**

```bash
./gradlew addExtension --extensions='info'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-info</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-info")
```

Once added, start your application and access the info endpoint:

```shell
$ curl -s http://localhost:8080/q/info | jq
```

## Response structure

The endpoint returns a JSON object with up to four sections:

```json
{
  "os": {
    "name": "Mac OS X",
    "version": "15.5",
    "arch": "aarch64"
  },
  "java": {
    "version": "21.0.7",
    "vendor": "Eclipse Adoptium",
    "vendorVersion": "Temurin-21.0.7+6"
  },
  "build": {
    "group": "org.acme",
    "artifact": "getting-started",
    "version": "1.0.0-SNAPSHOT",
    "time": "2026-05-21T10:30:00Z",
    "quarkusVersion": "{quarkus-version}"
  },
  "git": {
    "branch": "main",
    "commit": {
      "id": "a1b2c3d",
      "time": "2026-05-20T14:22:00Z"
    }
  }
}
```

Each section can be individually enabled or disabled via configuration.

> [!NOTE]
> Git information is only available if the project is in a Git repository. If no `.git` directory is found, the `git` section is omitted.

## Git information modes

The `quarkus.info.git.mode` property controls how much git information is included.

**`standard` (default)**\
Branch name, abbreviated commit ID, and commit time.

**`full`**\
Adds remote URL, tags, full commit ID, commit message, author and committer details.

```properties
quarkus.info.git.mode=full
```

With `full` mode, the `git` section expands to:

```json
{
  "git": {
    "branch": "main",
    "remote": "https://github.com/acme/getting-started.git",
    "tags": [],
    "commit": {
      "id": {
        "full": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
        "abbrev": "a1b2c3d"
      },
      "time": "2026-05-20T14:22:00Z",
      "message": {
        "full": "Add greeting endpoint",
        "short": "Add greeting endpoint"
      },
      "user": {
        "name": "Jane Doe",
        "email": "jane@example.com"
      }
    }
  }
}
```

## Adding custom build properties

You can add custom key-value pairs to the `build` section:

```properties
quarkus.info.build.my-custom-key=my-value
quarkus.info.build.environment=staging
```

These will appear alongside the standard build properties:

```json
{
  "build": {
    "group": "org.acme",
    "artifact": "getting-started",
    "version": "1.0.0-SNAPSHOT",
    "time": "2026-05-21T10:30:00Z",
    "quarkusVersion": "{quarkus-version}",
    "my-custom-key": "my-value",
    "environment": "staging"
  }
}
```

## Custom info contributors

You can contribute your own sections to the info endpoint by implementing the `InfoContributor` interface and making it a CDI bean:

```java
package org.acme;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.info.runtime.spi.InfoContributor;

@ApplicationScoped
public class CustomInfoContributor implements InfoContributor {

    @Override
    public String name() {
        return "aws";
    }

    @Override
    public Map<String, Object> data() {
        return Map.of(
            "region", "eu-west-1",
            "instance", System.getenv().getOrDefault("HOSTNAME", "unknown")
        );
    }
}
```

This adds a `custom` section to the info endpoint response:

```json
{
  "aws": {
    "region": "eu-west-1",
    "instance": "app-7f8d9c-xk2lp"
  }
}
```

The `name()` method determines the key in the JSON output. The `data()` method is called once at startup, use it for values that don't change at runtime.

## Programmatic access

The extension also exposes injectable beans that you can use directly in your code:

```java
import io.quarkus.info.GitInfo;
import io.quarkus.info.BuildInfo;
import io.quarkus.info.JavaInfo;
import io.quarkus.info.OsInfo;

@Path("/version")
public class VersionResource {

    @Inject
    BuildInfo buildInfo;

    @Inject
    GitInfo gitInfo;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return buildInfo.version() + " (" + gitInfo.latestCommitId() + ")";
    }
}
```

The available beans are:

- `GitInfo`: `branch()`, `latestCommitId()`, `commitTime()`
- `BuildInfo`: `group()`, `artifact()`, `version()`, `time()`, `quarkusVersion()`
- `JavaInfo`: `version()`, `vendor()`, `vendorVersion()`
- `OsInfo`: `name()`, `version()`, `architecture()`

> [!NOTE]
> These beans are only available if the corresponding section is enabled in configuration.

## Management interface

If you use the [management interface](/guides/management-interface-reference/index.md), the info endpoint is automatically exposed on the management port instead of the main HTTP port:

```properties
quarkus.management.enabled=true
```

The info endpoint will then be available at `http://0.0.0.0:9000/q/info` instead of `http://localhost:8080/q/info`.

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-info_quarkus-info-enabled"></a> [`quarkus.info.enabled`](#quarkus-info_quarkus-info-enabled) config_property_copy_button:+++quarkus.info.enabled+++[]<br><br>Whether the info endpoint will be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-info_quarkus-info-path"></a> [`quarkus.info.path`](#quarkus-info_quarkus-info-path) config_property_copy_button:+++quarkus.info.path+++[]<br><br>The path under which the info endpoint will be located<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_PATH+++[] | string | `info` |
| [lock] <a id="quarkus-info_quarkus-info-git-enabled"></a> [`quarkus.info.git.enabled`](#quarkus-info_quarkus-info-git-enabled) config_property_copy_button:+++quarkus.info.git.enabled+++[]<br><br>Whether git info will be included in the info endpoint<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_GIT_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-info_quarkus-info-git-mode"></a> [`quarkus.info.git.mode`](#quarkus-info_quarkus-info-git-mode) config_property_copy_button:+++quarkus.info.git.mode+++[]<br><br>Controls how much information is present in the git section<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_GIT_MODE+++[] | `standard`, `full` | `standard` |
| [lock] <a id="quarkus-info_quarkus-info-build-enabled"></a> [`quarkus.info.build.enabled`](#quarkus-info_quarkus-info-build-enabled) config_property_copy_button:+++quarkus.info.build.enabled+++[]<br><br>Whether build info will be included in the info endpoint<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_BUILD_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-info_quarkus-info-build-quarkus-version"></a> [`quarkus.info.build.quarkus-version`](#quarkus-info_quarkus-info-build-quarkus-version) config_property_copy_button:+++quarkus.info.build.quarkus-version+++[]<br><br>Whether the Quarkus version should be displayed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_BUILD_QUARKUS_VERSION+++[] | boolean | `true` |
| [lock] <a id="quarkus-info_quarkus-info-build-property-key"></a> [`quarkus.info.build."property-key"`](#quarkus-info_quarkus-info-build-property-key) config_property_copy_button:+++quarkus.info.build."property-key"+++[]<br><br>Additional properties to be added to the build section<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_BUILD__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-info_quarkus-info-os-enabled"></a> [`quarkus.info.os.enabled`](#quarkus-info_quarkus-info-os-enabled) config_property_copy_button:+++quarkus.info.os.enabled+++[]<br><br>Whether os info will be included in the info endpoint<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_OS_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-info_quarkus-info-java-enabled"></a> [`quarkus.info.java.enabled`](#quarkus-info_quarkus-info-java-enabled) config_property_copy_button:+++quarkus.info.java.enabled+++[]<br><br>Whether java info will be included in the info endpoint<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFO_JAVA_ENABLED+++[] | boolean | `true` |
