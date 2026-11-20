# Use virtual threads in REST applications

In this guide, we see how you can use virtual threads in a REST application. Because virtual threads are all about I/O, we will also use the REST client.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

The application built into this guide is quite simple. It calls a weather service for two cities (Valence, France, and Athens, Greece) and decides the best place based on the current temperature.

## Create the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:rest-virtual-threads \
    --extension='rest-jackson,quarkus-rest-client-jackson' \
    --no-code
cd rest-virtual-threads
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=rest-virtual-threads \
    -Dextensions='rest-jackson,quarkus-rest-client-jackson' \
    -DnoCode
cd rest-virtual-threads
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=rest-virtual-threads"`

This command generates a new project importing the Quarkus REST (formerly RESTEasy Reactive), REST client, and [Jackson](https://github.com/FasterXML/jackson) extensions, and in particular, adds the following dependencies:

**pom.xml**

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-rest-jackson")
implementation("io.quarkus:quarkus-rest-client-jackson")
```

> [!NOTE]
> You might wonder why we use *reactive* extensions. Virtual threads have limitations, and we can only integrate them properly when using the reactive extensions. No worries: your code will be written 100% in a synchronous / imperative style.
>
> Check the [virtual thread reference guide](/guides/virtual-threads/index.md#why-not) for details.

## Prepare the `pom.xml` file

We need to customize the `pom.xml` file to use virtual threads.

1. Locate the line with `<maven.compiler.release>17</maven.compiler.release>`, and replace it with:

```xml
    <maven.compiler.release>21</maven.compiler.release>
```

1. In the maven-surefire-plugin and maven-failsafe-plugin configurations, add the following `argLine` parameter:

> [!IMPORTANT]
> The `-Djdk.tracePinnedThreads` system property **only works on Java 21-23**. It has been removed in Java 24+. For Java 24+, use the JFR events or Quarkus testing extensions instead (see the [Testing virtual thread applications](/guides/virtual-threads/index.md#testing-virtual-thread-applications) section).

```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>${surefire-plugin.version}</version>
  <configuration>
    <systemPropertyVariables>
      <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
      <maven.home>${maven.home}</maven.home>
    </systemPropertyVariables>
    <argLine>-Djdk.tracePinnedThreads</argLine> <!-- Added line - only works on Java 21-23 -->
  </configuration>
</plugin>
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
          <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
          <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
          <maven.home>${maven.home}</maven.home>
        </systemPropertyVariables>
        <argLine>-Djdk.tracePinnedThreads</argLine> <!-- Added line - only works on Java 21-23 -->
      </configuration>
    </execution>
  </executions>
</plugin>
```

The `-Djdk.tracePinnedThreads` will detect pinned carrier threads while running tests on Java 21-23 (See [the virtual thread reference guide for details](/guides/virtual-threads/index.md#pinning)).

> [!IMPORTANT]
> **--enable-preview on Java 19 and 20**
>
> If you are using a Java 19 or 20, add the `--enable-preview` flag in the `argLine` and as `parameters` of the maven compiler plugin. Note that we strongly recommend Java 21.

## Create the weather client

This section is not about virtual threads. Because we need to do some I/O to demonstrate virtual threads usage, we need a client doing I/O operations. In addition, the REST client is virtual thread friendly: it does not pin and handle propagation correctly.

Create the `src/main/java/org/acme/WeatherService.java` class with the following content:

```java
package org.acme;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.GET;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

@RegisterRestClient(baseUri = "https://api.open-meteo.com/v1/forecast")
public interface WeatherService {

    @GET
    @ClientQueryParam(name = "current_weather", value = "true")
    WeatherResponse getWeather(@RestQuery double latitude, @RestQuery double longitude);


    record WeatherResponse(@JsonProperty("current_weather") Weather weather) {
        // represents the response
    }

    record Weather(double temperature, double windspeed) {
        // represents the inner object
    }
}
```

This class models the HTTP interaction with the weather service. Read more about the rest client in the dedicated [guide](/guides/rest-client/index.md).

## Create the HTTP endpoint

Now, create the `src/main/java/org/acme/TheBestPlaceToBeResource.java` class with the following content:

```java
package org.acme;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/")
public class TheBestPlaceToBeResource {

    static final double VALENCE_LATITUDE = 44.9;
    static final double VALENCE_LONGITUDE = 4.9;

    static final double ATHENS_LATITUDE = 37.9;
    static final double ATHENS_LONGITUDE = 23.7;

    @RestClient WeatherService service;

    @GET
    @RunOnVirtualThread // <1>
    public String getTheBestPlaceToBe() {
        var valence = service.getWeather(VALENCE_LATITUDE, VALENCE_LONGITUDE).weather().temperature();
        var athens = service.getWeather(ATHENS_LATITUDE, ATHENS_LONGITUDE).weather().temperature();

        // Advanced decision tree
        if (valence > athens && valence <= 35) {
            return "Valence! (" + Thread.currentThread() + ")";
        } else if (athens > 35) {
            return "Valence! (" + Thread.currentThread() + ")";
        } else {
            return "Athens (" + Thread.currentThread() + ")";
        }
    }
}
```

1. Instructs Quarkus to invoke this method on a virtual thread

## Run the application in dev mode

Make sure that you use OpenJDK and JVM versions supporting virtual thread and launch the *dev mode* with `./mvnw quarkus:dev`:

```shell
> java --version
openjdk 21 2023-09-19 LTS <1>
OpenJDK Runtime Environment Temurin-21+35 (build 21+35-LTS)
OpenJDK 64-Bit Server VM Temurin-21+35 (build 21+35-LTS, mixed mode)

> ./mvnw quarkus:dev <2>
```

1. Must be 19+, we recommend 21+
2. Launch the dev mode

Then, in a browser, open <http://localhost:8080>. You should get something like:

```text
Valence! (VirtualThread[#144]/runnable@ForkJoinPool-1-worker-6)
```

As you can see, the endpoint runs on a virtual thread.

It's essential to understand what happened behind the scene:

1. Quarkus creates the virtual thread to invoke your endpoint (because of the `@RunOnVirtualThread` annotation).
2. When the code invokes the rest client, the virtual thread is blocked, BUT the carrier thread is not blocked (that's the virtual thread *magic touch*).
3. Once the first invocation of the rest client completes, the virtual thread is rescheduled and continues its execution.
4. The second rest client invocation happens, and the virtual thread is blocked again (but not the carrier thread).
5. Finally, when the second invocation of the rest client completes, the virtual thread is rescheduled and continues its execution.
6. The method returns the result. The virtual thread terminates.
7. The result is captured by Quarkus and written in the HTTP response

## Verify pinning using tests

In the `pom.xml,` we added an `argLine` argument to the surefire and failsafe plugins:

```xml
<argLine>-Djdk.tracePinnedThreads</argLine> <!-- Only works on Java 21-23 -->
```

The `-Djdk.tracePinnedThreads` dumps the stack trace if a virtual thread cannot be *unmounted* smoothly (meaning that it blocks the carrier thread). That's what we call *pinning* (more info in [the virtual thread reference guide](/guides/virtual-threads/index.md#pinning)).

> [!IMPORTANT]
> This flag **only works on Java 21-23** and has been removed in Java 24+. For Java 24+, use the Quarkus `junit-virtual-threads` extension described in [the virtual thread reference guide](/guides/virtual-threads/index.md#testing-virtual-thread-applications), which uses JFR events to detect pinning across all Java versions.

> [!NOTE]
> Starting with Java 24, thanks to [JEP 491](https://openjdk.org/jeps/491), `synchronized` blocks no longer cause pinning. This means pinning is much less likely to occur on Java 24+, occurring only when native code calls back to Java code that performs blocking operations.

We recommend enabling pinning detection in tests. Thus, you can check that your application behaves correctly when using virtual threads. Just check your log after having run the test. If you see a stack trace (on Java 21-23) or pinning events (using the junit-virtual-threads extension), better check what's wrong. If your code (or one of your dependencies) pins, it might be better to use regular worker threads instead.

Create the `src/test/java/org/acme/TheBestPlaceToBeResourceTest.java` class with the following content:

```java
package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TheBestPlaceToBeResourceTest {

    @Test
    void verify() {
        RestAssured.get("/")
                .then()
                .statusCode(200);
    }

}
```

It is a straightforward test, but at least it will detect if our application is pinning. Run the test with either:

- `r` in dev mode (using continuous testing)
- `./mvnw test`

As you will see, it does not pin - no stack trace. It is because the REST client is implemented in a virtual-thread-friendly way.

The same approach can be used with integration tests.

## Conclusion

This guide shows how you can use virtual threads with Quarkus REST and the REST client. Learn more about virtual threads support on:

- [@RunOnVirtualThread in messaging applications](/guides/messaging-virtual-threads/index.md) (this guide covers Apache Kafka)
- [@RunOnVirtualThread in gRPC services](/guides/grpc-virtual-threads/index.md)
- [the virtual thread reference guide](/guides/virtual-threads/index.md) (include native compilation and containerization)
