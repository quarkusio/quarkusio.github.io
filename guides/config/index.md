# Configuring your application

> [!IMPORTANT]
> The content of this guide has been revised and split into additional topics. Please check the [Additional Information](#additional-information) section.

Hardcoded values in your code are a *no-go* (even if we all did it at some point ;-)). In this guide, we will learn how to configure a Quarkus application.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `config-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/config-quickstart).

## Create the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:config-quickstart \
    --extension='rest' \
    --no-code
cd config-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=config-quickstart \
    -Dextensions='rest' \
    -DnoCode
cd config-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=config-quickstart"`

It generates:

- the Maven structure
- a landing page accessible on `http://localhost:8080`
- example `Dockerfile` files for both `native` and `jvm` modes
- the application configuration file

## Create the configuration

A Quarkus application uses the [SmallRye Config](https://github.com/smallrye/smallrye-config) API to provide all mechanisms related with configuration.

By default, Quarkus reads configuration properties from [several sources](/guides/config-reference/index.md#configuration-sources). For the purpose of this guide, we will use an application configuration file located in `src/main/resources/application.properties`.

Edit the file with the following content:

**application.properties**

```properties
# Your configuration properties
greeting.message=hello
greeting.name=quarkus
```

## Create a REST resource

Create the `org.acme.config.GreetingResource` REST resource with the following content:

```java
package org.acme.config;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello RESTEasy";
    }
}
```

## Inject the configuration

Quarkus uses [MicroProfile Config](https://microprofile.io/specifications/config/) annotations to inject the configuration properties in the application.

```java
@ConfigProperty(name = "greeting.message") <1>
String message;
```

1. You can use `@Inject @ConfigProperty` or just `@ConfigProperty`. The `@Inject` annotation is not necessary for members annotated with `@ConfigProperty`.

> [!NOTE]
> If the application attempts to inject a configuration property that is not set, an error is thrown.

Edit the `org.acme.config.GreetingResource`, and introduce the following configuration properties:

```java
@ConfigProperty(name = "greeting.message") <1>
String message;

@ConfigProperty(name = "greeting.suffix", defaultValue="!") <2>
String suffix;

@ConfigProperty(name = "greeting.name")
Optional<String> name; <3>
```

1. If you do not provide a value for this property, the application startup fails with `jakarta.enterprise.inject.spi.DeploymentException: No config value of type [class java.lang.String] exists for: greeting.message`.
2. The default value is injected if the configuration does not provide a value for `greeting.suffix`.
3. This property is optional - an empty `Optional` is injected if the configuration does not provide a value for `greeting.name`.

Now, modify the `hello` method to use the injected properties:

```java
@GET
@Produces(MediaType.TEXT_PLAIN)
public String hello() {
    return message + " " + name.orElse("world") + suffix;
}
```

> [!TIP]
> Use `@io.smallrye.config.ConfigMapping` annotation to group multiple configurations in a single interface. Please, check the [Config Mappings](https://smallrye.io/smallrye-config/Main/config/mappings/) documentation.

<a id="secrets-in-environment-properties"></a>
## Store secrets in an environment properties file

A secret (such as a password, a personal access token or an API key) must not end up in version control for security reasons. One way is to store them in a local environment properties (`.env`) file:

1. Store the secret in the `.env` file in the project root directory.

   .The .env file

   ```properties
   foo.api-key=ThisIsSecret
   ```


1. Add the `.env` file to `.gitignore`.

`mvn quarkus:dev` automatically picks up the properties in the `.env` file, similar to those in the `application.properties` file.

## Update the test

We also need to update the functional test to reflect the changes made to the endpoint. Create the `src/test/java/org/acme/config/GreetingResourceTest.java` file with the following content:

```java
package org.acme.config;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/greeting")
          .then()
             .statusCode(200)
             .body(is("hello quarkus!")); // Modified line
    }

}
```

## Package and run the application

Run the application with:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

Open your browser to <http://localhost:8080/greeting>.

Changing the configuration file is immediately reflected. You can add the `greeting.suffix`, remove the other properties, change the values, etc.

As usual, the application can be packaged using:

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

and executed using `java -jar target/quarkus-app/quarkus-run.jar`.

You can also generate the native executable with:

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

## Programmatically access the configuration

The `org.eclipse.microprofile.config.ConfigProvider.getConfig()` API allows access to the Config API programmatically. This API is mostly useful in situations where CDI injection is not available.

```java
String databaseName = ConfigProvider.getConfig().getValue("database.name", String.class);
Optional<String> maybeDatabaseName = ConfigProvider.getConfig().getOptionalValue("database.name", String.class);
```

## Configuring Quarkus

Quarkus itself is configured via the same mechanism as your application. Quarkus reserves the `quarkus.` namespace for its own configuration. For example to configure the HTTP server port you can set `quarkus.http.port` in `application.properties`. All the Quarkus configuration properties are [documented and searchable](/guides/all-config/index.md).

> [!IMPORTANT]
> As mentioned above, properties prefixed with `quarkus.` are effectively reserved for configuring Quarkus itself and its extensions. Therefore, the `quarkus.` prefix should **never** be used for application specific properties.

### Build Time configuration

Some Quarkus configurations only take effect during build time, meaning is not possible to change them at runtime. These configurations are still available at runtime but as read-only and have no effect in Quarkus behaviour. A change to any of these configurations requires a rebuild of the application itself to reflect changes of such properties.

> [!TIP]
> The properties fixed at build time are marked with a lock icon ([lock]) in the [list of all configuration options](/guides/all-config/index.md).

However, some extensions do define properties *overridable at runtime*. A simple example is the database URL, username and password which is only known specifically in your target environment, so they can be set and influence the application behaviour at runtime.

<a id="additional-information"></a>
## Additional Information

- [Configuration Reference Guide](/guides/config-reference/index.md)
- [YAML ConfigSource Extension](/guides/config-yaml/index.md)
- [HashiCorp Vault ConfigSource Extension](https://github.com/quarkiverse/quarkus-vault)
- [Consul ConfigSource Extension](https://github.com/quarkiverse/quarkus-config-extensions)
- [Spring Cloud ConfigSource Extension](/guides/spring-cloud-config-client/index.md)
- [Mapping configuration to objects](/guides/config-mappings/index.md)
- [Extending configuration support](/guides/config-extending-support/index.md)

Quarkus relies on [SmallRye Config](https://github.com/smallrye/smallrye-config/) and inherits its features:

- Additional ``ConfigSource``s
- Additional ``Converter``s
- Indexed properties
- Parent profile
- Interceptors for configuration value resolution
- Relocate configuration properties
- Fallback configuration properties
- Logging
- Hide secrets

For more information, please check the [SmallRye Config documentation](https://smallrye.io/smallrye-config/Main/).
