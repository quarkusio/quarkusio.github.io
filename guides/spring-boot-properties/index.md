# Accessing application properties with Spring Boot properties API

If you prefer to use Spring Boot `@ConfigurationProperties` annotated class to access application properties instead of [`@ConfigMapping`](/guides/config-mappings/index.md#config-mappings) or a MicroProfile `@ConfigProperty` approach, you can do that with this extension.

Spring Boot `@ConfigurationProperties` classes are bound with the same mapping support as [`@ConfigMapping`](/guides/config-mappings/index.md), so nested classes, `Optional`, `List` and `Map` fields are supported.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 21+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `spring-boot-properties-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/spring-boot-properties-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:spring-boot-properties-quickstart \
    --extension='rest,spring-boot-properties' \
    --no-code
cd spring-boot-properties-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:4.0.1:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spring-boot-properties-quickstart \
    -Dextensions='rest,spring-boot-properties' \
    -DnoCode
cd spring-boot-properties-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=spring-boot-properties-quickstart"`

This command generates a project and imports the `spring-boot-properties` extension.

If you already have your Quarkus project configured, you can add the `spring-boot-properties` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add spring-boot-properties
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='spring-boot-properties'
```

**Gradle**

```bash
./gradlew addExtension --extensions='spring-boot-properties'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-boot-properties</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spring-boot-properties")
```

## GreetingController

First, create a `GreetingResource` Jakarta REST resource in the `src/main/java/org/acme/spring/boot/properties/GreetingResource.java` file that looks like:

```java
package org.acme.spring.boot.properties;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
```

## Injecting properties

Create a new class `src/main/java/org/acme/spring/boot/properties/GreetingProperties.java` with a message field:

```java
package org.acme.spring.boot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("greeting")
public class GreetingProperties {

    public String text;
}
```

Here `text` field is public, but it could also be a private field with a getter. As in Spring Boot, the annotated type must be a class, interfaces are not bound. Because `text` does not have a default value it is considered required and unless it is defined in a configuration file (`application.properties` by default)  your application will fail to start. Define this property in your `src/main/resources/application.properties` file:

```properties
# Your configuration properties
greeting.text=hello
```

Now modify `GreetingResource` to start using the `GreetingProperties`:

```java
package org.acme.spring.boot.properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @Inject
    GreetingProperties properties;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return properties.text;
    }
}
```

Run the tests to verify that application still functions correctly.

## Package and run the application

Run the application in dev mode with:

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

Changing the configuration file is immediately reflected.

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

And executed using `java -jar target/quarkus-app/quarkus-run.jar`.

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

## Default values

Now let's add a suffix for a greeting for which we'll set a default value.

Properties with default values can be configured in a configuration file just like any other property. However, the default value will be used if the property was not defined in a configuration file.

Go ahead and add the new field to the `GreetingProperties` class:

```java
package org.acme.spring.boot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("greeting")
public class GreetingProperties {

    public String text;

    public String suffix = "!";
}
```

And update the `GreetingResource` and its test `GreetingResourceTest`:

```java
package org.acme.spring.boot.properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @Inject
    GreetingProperties properties;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return properties.text + properties.suffix;
    }
}
```

```java
package org.acme.spring.boot.properties;

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
             .body(is("hello!"));
    }
}
```

Run the tests to verify the change.

## Optional values

Properties with optional values are the middle-ground between standard and properties with default values. While a missing property in a configuration file will not cause your application to fail, it will nevertheless not have a value set. We use `java.util.Optional` type to define such properties.

Add an optional `name` property to the `GreetingProperties`:

```java
package org.acme.spring.boot.properties;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("greeting")
public class GreetingProperties {

    public String text;

    public String suffix = "!";

    public Optional<String> name;
}
```

And update the `GreetingResource` and its test `GreetingResourceTest`:

```java
package org.acme.spring.boot.properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @Inject
    GreetingProperties properties;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return properties.text + ", " + properties.name.orElse("You") + properties.suffix;
    }
}
```

```java
package org.acme.spring.boot.properties;

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
             .body(is("hello, You!"));
    }
}
```

Run the tests to verify the change.

## Grouping properties

Now we have three properties in our `GreetingProperties` class. While `name` could be considered more of a runtime property (and maybe could be passed as an HTTP query parameter in the future), `text` and `suffix` are used to define a message template. Let's group these two properties in a separate inner class:

```java
package org.acme.spring.boot.properties;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("greeting")
public class GreetingProperties {

    public Message message;

    public Optional<String> name;

    public static class Message {

        public String text;

        public String suffix = "!";
    }
}
```

Here `Message` properties class is defined as an inner class, but it could also be a top level class.

Having such property groups brings more structure to your configuration. This is especially useful when then number of properties grows.

Because of the additional class, our property names have changed. Let's update the properties file and the `GreetingResource` class.

```properties
# Your configuration properties
greeting.message.text=hello
```

```java
package org.acme.spring.boot.properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreetingResource {

    @Inject
    GreetingProperties properties;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return properties.message.text + ", " + properties.name.orElse("You") + properties.message.suffix;
    }
}
```

## Naming strategy

By default, a field name is mapped to its configuration property name in kebab-case, so a `maxAttempts` field is read from `greeting.max-attempts`. Annotate the class with `@io.smallrye.config.ConfigMapping` to use a different `namingStrategy`:

```java
package org.acme.spring.boot.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMapping.NamingStrategy;

@ConfigurationProperties("greeting")
@ConfigMapping(namingStrategy = NamingStrategy.VERBATIM)
public class GreetingProperties {

    public Message message;

    public String greetingName; // <1>

    public static class Message { // <2>

        public String text;

        public String textSuffix = "!";
    }
}
```

1. Read from `greeting.greetingName`.
2. A nested class inherits the naming strategy of its enclosing class, so `textSuffix` is read from `greeting.message.textSuffix`. A nested class can declare its own `@ConfigMapping(namingStrategy = ...)` to override it. A property class that is not enclosed by the annotated class is mapped in kebab-case, unless it declares a naming strategy itself.

A field annotated with the Spring Boot `@Name` annotation is mapped to the given name, regardless of the naming strategy.

## Unknown properties

Configuration properties under the class prefix that do not map to any field are ignored. Set `ignoreUnknownFields = false` to fail the application startup instead:

```java
@ConfigurationProperties(value = "greeting", ignoreUnknownFields = false)
public class GreetingProperties {
    // ...
}
```

> [!NOTE]
> Unknown properties are only reported when `quarkus.config.mapping.validate-unknown` is enabled, which is the default.

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus - Extension for Spring DI](/guides/spring-di/index.md)
- [Quarkus - Extension for Spring Web](/guides/spring-web/index.md)
- [Quarkus - Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus - Extension for Spring Data REST](/guides/spring-data-rest/index.md)
- [Quarkus - Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus - Reading properties from Spring Cloud Config Server](/guides/spring-cloud-config-client/index.md)
- [Quarkus - Extension for Spring Cache](/guides/spring-cache/index.md)
- [Quarkus - Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Quarkus - Extension for Spring Transaction](/guides/spring-tx/index.md)
- [Testing Spring-style Applications in Quarkus](/guides/spring-test/index.md)
