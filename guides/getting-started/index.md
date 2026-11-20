# Creating your first application

In this guide, you'll create a REST endpoint, see live coding in action, add a service with dependency injection, and write tests. You won't write any boilerplate, and you won't have to restart the app, not even once.

> [!TIP]
> **Already ran the [Quick Start](/get-started/)?** Skip to [Using injection](#using-injection).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it

> [!TIP]
> **Verify Maven is using the Java you expect**
>
> If you have multiple JDK's installed, it is not certain Maven will pick up the expected java and you could end up with unexpected results. You can verify which JDK Maven uses by running `mvn --version`.

## Bootstrapping the project

The easiest way to create a new Quarkus project is to open a terminal and run the following command:

**CLI**

```bash
quarkus create app org.acme:getting-started \
    --extension='rest'
cd getting-started
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=getting-started \
    -Dextensions='rest'
cd getting-started
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=getting-started"`

It generates the following in `./getting-started`:

- the Maven structure
- an `org.acme.GreetingResource` REST endpoint exposed on `/hello`
- an associated unit test
- a landing page that is accessible on `http://localhost:8080` after starting the application
- example `Dockerfile` files for both `native` and `jvm` modes in `src/main/docker`
- the application configuration file

Look at the generated `pom.xml`. It imports the Quarkus BOM (`quarkus-bom`), so you can omit the version of Quarkus dependencies in your project. It also uses the `quarkus-maven-plugin`, which is responsible for packaging the application and providing development mode.

### The REST endpoint

During the project creation, the `src/main/java/org/acme/GreetingResource.java` file has been created with the following content:

```java
package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
```

It's a very simple REST endpoint, returning "Hello from Quarkus REST" to requests on "/hello".

## Running the application

Now we are ready to run our application:

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

```shell
...
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
INFO  [io.quarkus] (Quarkus Main Thread) getting-started 1.0.0-SNAPSHOT on JVM (powered by Quarkus {quarkus-version}) started in 0.968s. Listening on: http://localhost:8080
INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, rest, smallrye-context-propagation, vertx]
```

Once started, you can request the provided endpoint:

```shell
$ curl -w "\n" http://localhost:8080/hello
Hello from Quarkus REST
```

Keep it running and enjoy the blazing fast hot-reload. If you really want shutdown the application, hit `CTRL+C`.

> [!TIP]
> **Automatically add newline with `curl -w "\n"`**
>
> We are using `curl -w "\n"` in this example to avoid your terminal printing a '%' or put both result and next command prompt on the same line.

## Live coding

`quarkus:dev` runs Quarkus in development mode. Edit your Java files or resource files, save, and refresh your browser, changes take effect immediately, no restart needed. If there are any issues with compilation or deployment an error page will let you know.

Try it: open `src/main/java/org/acme/GreetingResource.java`, change `"Hello from Quarkus REST"` to `"Hola from Quarkus"`, save, and refresh <http://localhost:8080/hello>. Then, switch back to `"Hello from Quarkus REST"`, refresh again... changed again!

While dev mode is running, open [the Dev UI](http://localhost:8080/q/dev-ui/), a dashboard where you can browse installed extensions, configuration, endpoints, and more, all live-updated as you code.

This will also listen for a debugger on port `5005`. If you want to wait for the debugger to attach before running you can pass `-Dsuspend` on the command line. If you don't want the debugger at all you can use `-Ddebug=false`.

<a id="using-injection"></a>
## Using injection

Dependency injection in Quarkus is based on ArC which is a CDI-based dependency injection solution tailored for Quarkus' architecture. If you're new to CDI then we recommend you to read the [Introduction to CDI](/guides/cdi/index.md) guide.

Quarkus only implements a subset of the CDI features and comes with non-standard features and specific APIS, you can learn more about it in the [Contexts and Dependency Injection guide](/guides/cdi-reference/index.md).

ArC comes as a dependency of `quarkus-rest` so you already have it handy.

Let's modify the application and add a companion bean. Create the `src/main/java/org/acme/GreetingService.java` file with the following content:

```java
package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String greeting(String name) {
        return "hello " + name;
    }

}
```

Edit the `GreetingResource` class to inject the `GreetingService` and create a new endpoint using it:

```java
package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @Inject
    GreetingService service;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/greeting/{name}")
    public String greeting(String name) {
        return service.greeting(name);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
```

If you stopped the application (keep in mind you don't have to do it, changes will be automatically deployed by our live reload feature), restart the application with:

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

Then check that the endpoint returns `hello quarkus` as expected:

```shell
$ curl -w "\n" http://localhost:8080/hello/greeting/quarkus
hello quarkus
```

## Testing

All right, so far so good, but wouldn't it be better with a few tests, just in case?

In the generated build file, you can see 2 test dependencies:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-junit")
testImplementation("io.rest-assured:rest-assured")
```

Quarkus supports [JUnit](https://docs.junit.org) tests.

Because of this, in the case of Maven, the version of the [Surefire Maven Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/) must be set, as the default version does not support JUnit:

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>${surefire-plugin.version}</version>
    <configuration>
       <argLine>--add-opens java.base/java.lang=ALL-UNNAMED</argLine>
       <systemPropertyVariables>
          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
          <maven.home>${maven.home}</maven.home>
       </systemPropertyVariables>
    </configuration>
</plugin>
```

We also set the `java.util.logging` system property to make sure tests will use the correct log manager and `maven.home` to ensure that custom configuration from `${maven.home}/conf/settings.xml` is applied (if any).

The generated project contains a simple test. Edit the `src/test/java/org/acme/GreetingResourceTest.java` to match the following content:

```java
package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest  // <1>
class GreetingResourceTest {

    @Test
    void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)    // <2>
             .body(is("Hello from Quarkus REST"));
    }

    @Test
    void testGreetingEndpoint() {
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

1. By using the `@QuarkusTest` annotation, you instruct JUnit to start the application before the tests.
2. Check the HTTP response status code and content

These tests use [RestAssured](https://rest-assured.io/), but feel free to use your favorite library.

You can run these using Maven:

```bash
./mvnw test
```

You can also run the test from your IDE directly (be sure you stopped the application first).

By default, tests will run on port `8081` so as not to conflict with the running application. We automatically configure RestAssured to use this port.

> [!TIP]
> If you want to use a different client you should use the `@TestHTTPResource` annotation to directly inject the URL of the tested application into a field on the test class. This field can be of the type `String`, `URL` or `URI`. This annotation can also be given a value for the test path. For example, if you want to test an endpoint mapped to `/hello`, you would just add the following to your `@QuarkusTest` test:
>
> ```java
> @TestHTTPResource("/hello")
> URL testUrl;
> ```

The test port can be controlled via the `quarkus.http.test-port` config property.

### Continuous testing

You can also have Quarkus run your tests automatically as you code. In the dev mode terminal, press `r`, and then Quarkus re-runs affected tests on every save, giving you instant feedback without leaving your editor. See the [Continuous Testing guide](/guides/continuous-testing/index.md) for more details.

## Packaging and run the application

The application is packaged using:

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

It produces the `quarkus-app` directory in `/target`, which contains the `quarkus-run.jar` file. This is a *fast-jar*, not an *über-jar*, the dependencies are copied into subdirectories of `quarkus-app/lib/`.

You can run the application using: `java -jar target/quarkus-app/quarkus-run.jar`.

> [!NOTE]
> If you want to deploy your application somewhere (typically in a container), you need to copy/deploy the whole `quarkus-app` directory.

> [!NOTE]
> Before running the application, don't forget to stop the hot reload mode (hit `CTRL+C`), or you will have a port conflict.

## What's next?

You now have a running Quarkus application with dependency injection and tests. Here's where to go next:

- [Your Second Quarkus Application](/guides/getting-started-dev-services/index.md) — add a database without installing one, using Dev Services
- [Building a Native Executable](/guides/building-native-image/index.md) — compile to a native binary and package it in a container
- [Getting Started with Quarkus and Kafka](/guides/kafka-getting-started/index.md) — build event-driven and streaming applications with Quarkus and Apache Kafka
- [Tooling Guide](/guides/tooling/index.md) — IDE setup, scaffolding, and development mode tips

### Solution

You can find the completed example in the [getting-started directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/getting-started) of the Quarkus quickstarts repository:

```bash
git clone https://github.com/quarkusio/quarkus-quickstarts.git
```
