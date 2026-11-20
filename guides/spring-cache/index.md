# Quarkus extension for Spring Cache API

While users are encouraged to use [Quarkus annotations for caching](/guides/cache/index.md), Quarkus nevertheless provides a compatibility layer for Spring Cache annotations in the form of the `spring-cache` extension.

This guide explains how a Quarkus application can leverage the well known Spring Cache annotations to enable application data caching for their Spring beans.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- Some familiarity with the Spring DI extension

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:spring-cache-quickstart \
    --extension='rest,spring-di,spring-cache' \
    --no-code
cd spring-cache-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spring-cache-quickstart \
    -Dextensions='rest,spring-di,spring-cache' \
    -DnoCode
cd spring-cache-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=spring-cache-quickstart"`

This command generates a project which imports the `spring-cache` and `spring-di` extensions.

If you already have your Quarkus project configured, you can add the `spring-cache` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add spring-cache
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='spring-cache'
```

**Gradle**

```bash
./gradlew addExtension --extensions='spring-cache'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-cache</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spring-cache")
```

## Creating the REST API

Let's start by creating a service which will simulate an extremely slow call to an external meteorological service. Create `src/main/java/org/acme/spring/cache/WeatherForecastService.java` with the following content:

```java
package org.acme.spring.cache;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class WeatherForecastService {

    public String getDailyForecast(LocalDate date, String city) {
        try {
            Thread.sleep(2000L); // <1>
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return date.getDayOfWeek() + " will be " + getDailyResult(date.getDayOfMonth() % 4) + " in " + city;
    }

    private String getDailyResult(int dayOfMonthModuloFour) {
        switch (dayOfMonthModuloFour) {
            case 0:
                return "sunny";
            case 1:
                return "cloudy";
            case 2:
                return "chilly";
            case 3:
                return "rainy";
            default:
                throw new IllegalArgumentException();
        }
    }
}
```

1. This is where the slowness comes from.

We also need a class which contains the response sent to the users when they ask for the next three days weather forecast. Create `src/main/java/org/acme/spring/cache/WeatherForecast.java` this way:

```java
package org.acme.spring.cache;

import java.util.List;

public class WeatherForecast {

    private List<String> dailyForecasts;

    private long executionTimeInMs;

    public WeatherForecast(List<String> dailyForecasts, long executionTimeInMs) {
        this.dailyForecasts = dailyForecasts;
        this.executionTimeInMs = executionTimeInMs;
    }

    public List<String> getDailyForecasts() {
        return dailyForecasts;
    }

    public long getExecutionTimeInMs() {
        return executionTimeInMs;
    }
}
```

Now, we just need to create the `src/main/java/org/acme/spring/cache/WeatherForecastResource.java` class to use the service and response:

```java
package org.acme.spring.cache;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;

@Path("/weather")
public class WeatherForecastResource {

    @Inject
    WeatherForecastService service;

    @GET
    public WeatherForecast getForecast(@RestQuery String city, @RestQuery long daysInFuture) { // <1>
        long executionStart = System.currentTimeMillis();
        List<String> dailyForecasts = Arrays.asList(
                service.getDailyForecast(LocalDate.now().plusDays(daysInFuture), city),
                service.getDailyForecast(LocalDate.now().plusDays(daysInFuture + 1L), city),
                service.getDailyForecast(LocalDate.now().plusDays(daysInFuture + 2L), city)
        );
        long executionEnd = System.currentTimeMillis();
        return new WeatherForecast(dailyForecasts, executionEnd - executionStart);
    }
}
```

1. If the `daysInFuture` query parameter is omitted, the three days weather forecast will start from the current day. Otherwise, it will start from the current day plus the `daysInFuture` value.

We're all done! Let's check if everything's working.

First, run the application using:

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

Then, call `http://localhost:8080/weather?city=Raleigh` from a browser. After six long seconds, the application will answer something like this:

```json
{"dailyForecasts":["MONDAY will be cloudy in Raleigh","TUESDAY will be chilly in Raleigh","WEDNESDAY will be rainy in Raleigh"],"executionTimeInMs":6001}
```

> [!TIP]
> The response content may vary depending on the day you run the code.

You can try calling the same URL again and again, it will always take six seconds to answer.

## Enabling the cache

Now that your Quarkus application is up and running, let's tremendously improve its response time by caching the external meteorological service responses. Update the `WeatherForecastService` class as follows:

```java
package org.acme.cache;

import java.time.LocalDate;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class WeatherForecastService {

    @Cacheable("weather-cache") // <1>
    public String getDailyForecast(LocalDate date, String city) {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return date.getDayOfWeek() + " will be " + getDailyResult(date.getDayOfMonth() % 4) + " in " + city;
    }

    private String getDailyResult(int dayOfMonthModuloFour) {
        switch (dayOfMonthModuloFour) {
            case 0:
                return "sunny";
            case 1:
                return "cloudy";
            case 2:
                return "chilly";
            case 3:
                return "rainy";
            default:
                throw new IllegalArgumentException();
        }
    }
}
```

1. We only added this annotation (and the associated import of course).

Let's try to call `http://localhost:8080/weather?city=Raleigh` again. You're still waiting a long time before receiving an answer. This is normal since the server just restarted and the cache was empty.

Wait a second! The server restarted by itself after the `WeatherForecastService` update? Yes, this is one of Quarkus amazing features for developers called `live coding`.

Now that the cache was loaded during the previous call, try calling the same URL. This time, you should get a super fast answer with an `executionTimeInMs` value close to 0.

Let's see what happens if we start from one day in the future using the `http://localhost:8080/weather?city=Raleigh&daysInFuture=1` URL. You should get an answer two seconds later since two of the requested days were already loaded in the cache.

You can also try calling the same URL with a different city and see the cache in action again. The first call will take six seconds and the following ones will be answered immediately.

Congratulations! You just added application data caching to your Quarkus application with a single line of code!

## Supported features

Quarkus provides compatibility with the following Spring Cache annotations:

- `@Cacheable`
- `@CachePut`
- `@CacheEvict`

Note that in this first version of the Spring Cache annotations extension, not all features of these annotations are supported (with proper errors being logged when trying to use an unsupported feature). However, additional features are planned for future releases.

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus - Extension for Spring DI](/guides/spring-di/index.md)
- [Quarkus - Extension for Spring Web](/guides/spring-web/index.md)
- [Quarkus - Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus - Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus - Extension for Spring Data REST](/guides/spring-data-rest/index.md)
- [Quarkus - Reading properties from Spring Cloud Config Server](/guides/spring-cloud-config-client/index.md)
- [Quarkus - Extension for Spring Boot properties](/guides/spring-boot-properties/index.md)
- [Quarkus - Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Quarkus - Extension for Spring Transaction](/guides/spring-tx/index.md)
- [Testing Spring-style Applications in Quarkus](/guides/spring-test/index.md)
