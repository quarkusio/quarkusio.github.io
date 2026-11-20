# Scheduling periodic tasks

Modern applications often need to run specific tasks periodically. In this guide, you learn how to schedule periodic tasks.

> [!TIP]
> If you need a clustered scheduler use the [Quartz extension](/guides/quartz/index.md).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this guide, we create a straightforward application accessible using HTTP to get the current value of a counter. This counter is periodically (every 10 seconds) incremented.

![Architecture](/guides/images/scheduling-task-architecture.png)

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `scheduler-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/scheduler-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:scheduler-quickstart \
    --extension='rest,scheduler' \
    --no-code
cd scheduler-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=scheduler-quickstart \
    -Dextensions='rest,scheduler' \
    -DnoCode
cd scheduler-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=scheduler-quickstart"`

It generates a new project including:

- a landing page accessible on `http://localhost:8080`
- example `Dockerfile` files for both `native` and `jvm` modes
- the application configuration file

The project also imports the Quarkus REST (formerly RESTEasy Reactive) and scheduler extensions.

If you already have your Quarkus project configured, you can add the `scheduler` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add scheduler
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='scheduler'
```

**Gradle**

```bash
./gradlew addExtension --extensions='scheduler'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-scheduler</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-scheduler")
```

<a id="standard-scheduling"></a>
## Creating a scheduled job

In the `org.acme.scheduler` package, create the `CounterBean` class, with the following content:

```java
package org.acme.scheduler;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.ScheduledExecution;

@ApplicationScoped              // <1>
public class CounterBean {

    private AtomicInteger counter = new AtomicInteger();

    public int get() {  // <2>
        return counter.get();
    }

    @Scheduled(every="10s")     // <3>
    void increment() {
        counter.incrementAndGet(); // <4>
    }

    @Scheduled(cron="0 15 10 * * ?") <5>
    void cronJob(ScheduledExecution execution) {
        counter.incrementAndGet();
        System.out.println(execution.getScheduledFireTime());
    }

    @Scheduled(cron = "{cron.expr}") <6>
    void cronJobWithExpressionInConfig() {
       counter.incrementAndGet();
       System.out.println("Cron expression configured in application.properties");
    }
}
```

1. Declare the bean in the *application* scope
2. The `get()` method allows retrieving the current value.
3. Use the `@Scheduled` annotation to instruct Quarkus to run this method every 10 seconds provided a worker thread is available (Quarkus is using 10 worker threads for the scheduler). If it is not available the method invocation should be re-scheduled by default i.e. it should be invoked as soon as possible. The invocation of the scheduled method does not depend on the status or result of the previous invocation.
4. The code is pretty straightforward. Every 10 seconds, the counter is incremented.
5. Define a job with a cron-like expression. The annotated method is executed at 10:15am every day.
6. Define a job with a cron-like expression `cron.expr` which is configurable in `application.properties`.

## Updating the application configuration file

Edit the `application.properties` file and add the `cron.expr` configuration:

```properties
# By default, the syntax used for cron expressions is based on Quartz - https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
# You can change the syntax using the following property:
# quarkus.scheduler.cron-type=unix
cron.expr=*/5 * * * * ?
```

## Creating the REST resource

Create the `CountResource` class as follows:

```java
package org.acme.scheduler;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/count")
public class CountResource {

    @Inject
    CounterBean counter;            // <1>


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "count: " + counter.get();  // <2>
    }
}
```

1. Inject the `CounterBean`
2. Send back the current counter value

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

In another terminal, run `curl localhost:8080/count` to check the counter value. After a few seconds, re-run `curl localhost:8080/count` to verify the counter has been incremented.

Observe the console to verify that the message `Cron expression configured in application.properties` has been displayed indicating that the cron job using an expression configured in `application.properties` has been triggered.

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

And executed with `java -jar target/quarkus-app/quarkus-run.jar`.

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

<a id="scheduler-configuration-reference"></a>
## Scheduler Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-scheduler_quarkus-scheduler-cron-type"></a> [`quarkus.scheduler.cron-type`](#quarkus-scheduler_quarkus-scheduler-cron-type) config_property_copy_button:+++quarkus.scheduler.cron-type+++[]<br><br>The syntax used in CRON expressions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_CRON_TYPE+++[] | `cron4j`, `quartz`, `unix`, `spring`, `spring53` | `quartz` |
| [lock] <a id="quarkus-scheduler_quarkus-scheduler-metrics-enabled"></a> [`quarkus.scheduler.metrics.enabled`](#quarkus-scheduler_quarkus-scheduler-metrics-enabled) config_property_copy_button:+++quarkus.scheduler.metrics.enabled+++[]<br><br>Scheduled task metrics will be enabled if a metrics extension is present and this value is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_METRICS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-scheduler_quarkus-scheduler-tracing-enabled"></a> [`quarkus.scheduler.tracing.enabled`](#quarkus-scheduler_quarkus-scheduler-tracing-enabled) config_property_copy_button:+++quarkus.scheduler.tracing.enabled+++[]<br><br>Controls whether tracing is enabled. If set to true and the OpenTelemetry extension is present, tracing will be enabled, creating automatic Spans for each scheduled task.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_TRACING_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-scheduler_quarkus-scheduler-use-composite-scheduler"></a> [`quarkus.scheduler.use-composite-scheduler`](#quarkus-scheduler_quarkus-scheduler-use-composite-scheduler) config_property_copy_button:+++quarkus.scheduler.use-composite-scheduler+++[]<br><br>By default, only one `Scheduler` implementation is used. If set to `true` then a composite `Scheduler` that delegates to all running implementations is used.<br><br>Scheduler implementations will be started depending on the value of `quarkus.scheduler.start-mode`, i.e. the scheduler is not started unless a relevant `io.quarkus.scheduler.Scheduled` business method is found.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_USE_COMPOSITE_SCHEDULER+++[] | boolean | `false` |
| <a id="quarkus-scheduler_quarkus-scheduler-enabled"></a> [`quarkus.scheduler.enabled`](#quarkus-scheduler_quarkus-scheduler-enabled) config_property_copy_button:+++quarkus.scheduler.enabled+++[]<br><br>If schedulers are enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-scheduler_quarkus-scheduler-overdue-grace-period"></a> [`quarkus.scheduler.overdue-grace-period`](#quarkus-scheduler_quarkus-scheduler-overdue-grace-period) config_property_copy_button:+++quarkus.scheduler.overdue-grace-period+++[]<br><br>Scheduled task will be flagged as overdue if next execution time is exceeded by this period.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_OVERDUE_GRACE_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-scheduler_quarkus-scheduler) | `1S` |
| <a id="quarkus-scheduler_quarkus-scheduler-start-mode"></a> [`quarkus.scheduler.start-mode`](#quarkus-scheduler_quarkus-scheduler-start-mode) config_property_copy_button:+++quarkus.scheduler.start-mode+++[]<br><br>Scheduler can be started in different modes. By default, the scheduler is not started unless a `io.quarkus.scheduler.Scheduled` business method is found.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SCHEDULER_START_MODE+++[] | tooltip:normal[The scheduler is not started unless a `io.quarkus.scheduler.Scheduled` business method is found.], tooltip:forced[The scheduler will be started even if no scheduled business methods are found.  This is necessary for "pure" programmatic scheduling.], tooltip:halted[Just like the `forced` mode but the scheduler will not start triggering jobs until `Scheduler++#++resume()` is called.  This can be useful to run some initialization logic that needs to be performed before the scheduler starts.] | tooltip:normal[The scheduler is not started unless a {@link io.quarkus.scheduler.Scheduled} business method is found.] |

<a id="duration-note-anchor-quarkus-scheduler_quarkus-scheduler"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.
