# Quarkus extension for Spring DI API

While users are encouraged to use CDI annotations for injection, Quarkus provides a compatibility layer for Spring dependency injection in the form of the `spring-di` extension.

This guide explains how a Quarkus application can leverage the well known Dependency Injection annotations included in the Spring Framework.

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

The solution is located in the `spring-di-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/spring-di-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:spring-di-quickstart \
    --extension='rest,spring-di' \
    --no-code
cd spring-di-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spring-di-quickstart \
    -Dextensions='rest,spring-di' \
    -DnoCode
cd spring-di-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=spring-di-quickstart"`

This command generates a project which imports the `spring-di` extension.

If you already have your Quarkus project configured, you can add the `spring-di` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add spring-di
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='spring-di'
```

**Gradle**

```bash
./gradlew addExtension --extensions='spring-di'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-di</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spring-di")
```

## Add beans using Spring annotations

Let's proceed to create some beans using various Spring annotations.

First we will create a `StringFunction` interface that some of our beans will implement and which will be injected into another bean later on. Create a `src/main/java/org/acme/spring/di/StringFunction.java` file and set the following content:

```java
package org.acme.spring.di;

import java.util.function.Function;

public interface StringFunction extends Function<String, String> {

}
```

With the interface in place, we will add an `AppConfiguration` class which will use the Spring's Java Config style for defining a bean. It will be used to create a `StringFunction` bean that will capitalize the text passed as parameter. Create a `src/main/java/org/acme/spring/di/AppConfiguration.java` file with the following content:

```java
package org.acme.spring.di;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean(name = "capitalizeFunction")
    public StringFunction capitalizer() {
        return String::toUpperCase;
    }
}
```

As a Spring developer, you might be tempted to add the `@ComponentScan` annotation in order to define specific packages to scan for additional beans. Do note that `@ComponentScan` is entirely unnecessary since Quarkus performs [bean discovery](/guides/cdi-reference/index.md#bean_discovery) only in `annotated` mode with no visibility boundaries. Moreover, note that the bean discovery in Quarkus happens at build time. In the same vein, Quarkus does not support the Spring `@Import` annotation.

Now we define another bean that will implement `StringFunction` using Spring's stereotype annotation style. This bean will effectively be a no-op bean that simply returns the input as is. Create a `src/main/java/org/acme/spring/di/NoOpSingleStringFunction.java` file and set the following content:

```java
package org.acme.spring.di;

import org.springframework.stereotype.Component;

@Component("noopFunction")
public class NoOpSingleStringFunction implements StringFunction {

    @Override
    public String apply(String s) {
        return s;
    }
}
```

Quarkus also provides support for injecting configuration values using Spring's `@Value` annotation. To see that in action, first edit the `src/main/resources/application.properties` with the following content:

```properties
# Your configuration properties
greeting.message = hello
```

Next create a new Spring bean in `src/main/java/org/acme/spring/di/MessageProducer.java` with the following content:

```java
package org.acme.spring.di;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

    @Value("${greeting.message}")
    String message;

    public String getPrefix() {
        return message;
    }
}
```

The final bean we will create ties together all the previous beans. Create a `src/main/java/org/acme/spring/di/GreeterBean.java` file and copy the following content:

```java
package org.acme.spring.di;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GreeterBean {

    private final MessageProducer messageProducer;

    @Autowired
    @Qualifier("noopFunction")
    StringFunction noopStringFunction;

    @Autowired
    @Qualifier("capitalizeFunction")
    StringFunction capitalizerStringFunction;

    @Value("${greeting.suffix:!}")
    String suffix;

    public GreeterBean(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public String greet(String name) {
        final String initialValue = messageProducer.getPrefix() + " " + name + suffix;
        return noopStringFunction.andThen(capitalizerStringFunction).apply(initialValue);
    }
}
```

In the code above, we see that both field injection and constructor injection are being used (note that constructor injection does not need the `@Autowired` annotation since there is a single constructor). Furthermore, the `@Value` annotation on `suffix` has also a default value defined, which in this case will be used since we have not defined `greeting.suffix` in `application.properties`.

### Create the Jakarta REST resource

Create the `src/main/java/org/acme/spring/di/GreeterResource.java` file with the following content:

```java
package org.acme.spring.di;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/greeting")
public class GreeterResource {

    @Autowired
    GreeterBean greeterBean;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return greeterBean.greet("world");
    }
}
```

## Update the test

We also need to update the functional test to reflect the changes made to the endpoint. Edit the `src/test/java/org/acme/spring/di/GreetingResourceTest.java` file and change the content of the `testHelloEndpoint` method to:

```java
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
                .body(is("HELLO WORLD!"));
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

The result should be: `HELLO WORLD!`.

## Run the application as a native

You can of course create a native image using instructions similar to [this](/guides/building-native-image/index.md) guide.

## Important Technical Note

Please note that the Spring support in Quarkus does not start a Spring Application Context nor are any Spring infrastructure classes run. Spring classes and annotations are only used for reading metadata and / or are used as user code method return types or parameter types. What that means for end users, is that adding arbitrary Spring libraries will not have any effect. Moreover, Spring infrastructure classes (like `org.springframework.beans.factory.config.BeanPostProcessor` , `org.springframework.context.ApplicationContext` for example) will not be executed. Regarding the dependency injection in particular, Quarkus uses a Dependency Injection mechanism (called ArC) based on the [Jakarta Contexts and Dependency Injection 4.1](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html) specification. If you want to learn more about it, we recommend you to read the [Quarkus introduction to CDI](/guides/cdi/index.md) and the [CDI reference guide](/guides/cdi-reference/index.md#arc-configuration-reference) The various Spring Boot test features are not supported by Quarkus. For testing purposes, please, check the [Quarkus testing guide](/guides/getting-started-testing/index.md).

Some known limitations:

- In case of ambiguity, Spring uses a fallback match of the bean name against the injection point field name or parameter name. This is not supported, thus `@Named` annotation needs to be used to explicitly solve any ambiguity.
- Injecting all beans of a particular type is limited to `List<Bean>`. Injecting `Set<Bean>` or `Map<String, Bean>` is not supported.
- Optional injection using `@Autowired(required=false)` is not supported. Use `javax.enterprise.inject.Instance` and then test `Instance.isResolvable()`.
- `@Conditional` is ignored, since dependency injection gets resolved at build time. An alternative is to use [conditional build profiles](/guides/cdi-reference/index.md#enable_build_profile).

## Conversion Table

The following table shows how Spring DI annotations can be converted to CDI and / or MicroProfile annotations.

| Spring | CDI / MicroProfile | Comments |
| --- | --- | --- |
| @Autowired | @Inject | If the type is `java.util.List`, the `io.quarkus.arc.All` qualifier is added. |
| @Qualifier | @Named |  |
| @Value | @ConfigProperty | @ConfigProperty doesn't support an expression language the way @Value does, but makes the typical use cases much easier to handle |
| @Component | @Singleton | By default Spring stereotype annotations are singleton beans |
| @Service | @Singleton | By default Spring stereotype annotations are singleton beans |
| @Repository | @Singleton | By default Spring stereotype annotations are singleton beans |
| @Configuration | @ApplicationScoped | In CDI a producer bean isn't limited to the application scope, it could just as well be @Singleton or @Dependent |
| @Bean | @Produces |  |
| @Scope |  | Doesn't have a one-to-one mapping to a CDI annotation. Depending on the value of @Scope, one of the @Singleton, @ApplicationScoped, @SessionScoped, @RequestScoped, @Dependent could be used |
| @ComponentScan |  | Doesn't have a one-to-one mapping to a CDI annotation. It is not used in Quarkus because Quarkus does all classpath scanning at build time. |
| @Import |  | Doesn't have a one-to-one mapping to a CDI annotation. |

## Spring DI Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-spring-di_quarkus-spring-di-enabled"></a> [`quarkus.spring-di.enabled`](#quarkus-spring-di_quarkus-spring-di-enabled) config_property_copy_button:+++quarkus.spring-di.enabled+++[]<br><br>Whether Spring DI is enabled ++**++during the build++**++.<br><br>Turning this setting off will result in Quarkus completely ignoring beans annotated with Spring annotations<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SPRING_DI_ENABLED+++[] | boolean | `true` |

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus - Extension for Spring Web](/guides/spring-web/index.md)
- [Quarkus - Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus - Extension for Spring Data REST](/guides/spring-data-rest/index.md)
- [Quarkus - Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus - Reading properties from Spring Cloud Config Server](/guides/spring-cloud-config-client/index.md)
- [Quarkus - Extension for Spring Boot properties](/guides/spring-boot-properties/index.md)
- [Quarkus - Extension for Spring Cache](/guides/spring-cache/index.md)
- [Quarkus - Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Quarkus - Extension for Spring Transaction](/guides/spring-tx/index.md)
- [Testing Spring-style Applications in Quarkus](/guides/spring-test/index.md)
