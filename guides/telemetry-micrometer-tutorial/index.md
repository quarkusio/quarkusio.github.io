# Collect metrics using Micrometer

Create an application that uses the Micrometer metrics library to collect runtime, extension and application metrics and expose them as a Prometheus (OpenMetrics) endpoint.

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Solution

We recommend that you follow the instructions to create the application step by step, but you can skip right to the solution if you prefer. Either:

- Clone the git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or
- Download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `micrometer-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/micrometer-quickstart).

## Create the Maven project

Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:micrometer-quickstart \
    --extension='rest,micrometer-registry-prometheus' \
    --no-code
cd micrometer-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=micrometer-quickstart \
    -Dextensions='rest,micrometer-registry-prometheus' \
    -DnoCode
cd micrometer-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=micrometer-quickstart"`

This command generates a Maven project, that imports the `micrometer-registry-prometheus` extension as a dependency. This extension will load the core `micrometer` extension as well as additional library dependencies required to support prometheus.

> [!NOTE]
> To maintain backwards compatibility, the extension uses the Prometheus client v0.x from Micrometer 1.13+, instead of their default v1.x client.

## Create a REST endpoint

Let's first add a simple endpoint that calculates prime numbers.

```java

package org.acme.micrometer;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

@Path("/example")
@Produces("text/plain")
public class ExampleResource {

    @GET
    @Path("prime/{number}")
    public String checkIfPrime(@PathParam("number") long number) {
        if (number < 1) {
            return "Only natural numbers can be prime numbers.";
        }
        if (number == 1) {
            return number + " is not prime.";
        }
        if (number == 2 || number % 2 == 0) {
            return number + " is not prime.";
        }
        if (testPrimeNumber(number)) {
            return number + " is prime.";
        } else {
            return number + " is not prime.";
        }
    }

    protected boolean testPrimeNumber(long number) {
        for (int i = 3; i < Math.floor(Math.sqrt(number)) + 1; i = i + 2) {
            if (number % i == 0) {
                return false;
            }
        }
        return true;
    }
}
```

Start your application in dev mode:

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

### Review automatically generated metrics

The Micrometer extension automatically times HTTP server requests.

Let's use `curl` (or a browser) to visit our endpoint a few times:

```shell
curl http://localhost:8080/example/prime/256
curl http://localhost:8080/example/prime/7919
```

The Micrometer Prometheus MeterRegistry extension creates an endpoint we can use to observe collected metrics. Let's take a look at the metrics that have been collected:

```shell
curl http://localhost:8080/q/metrics
```

Look for `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, and `http_server_requests_seconds_max` in the output.

Dimensional labels are added for the request uri, the HTTP method (GET, POST, etc.), the status code (200, 302, 404, etc.), and a more general outcome field. You should find something like this:

```text
# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",outcome="SUCCESS",status="200",uri="/example/prime/{number}"} 2.0
http_server_requests_seconds_sum{method="GET",outcome="SUCCESS",status="200",uri="/example/prime/{number}"} 0.017385896
# HELP http_server_requests_seconds_max
# TYPE http_server_requests_seconds_max gauge
http_server_requests_seconds_max{method="GET",outcome="SUCCESS",status="200",uri="/example/prime/{number}"} 0.017385896
#
```

> [!NOTE]
> Metrics appear lazily, you often won't see any data for your endpoint until it is accessed.

By default, the  metrics are exported using the Prometheus format `application/openmetrics-text`, you can revert to the former format by specifying the `Accept` request header to `text/plain` (`curl -H "Accept: text/plain" localhost:8080/q/metrics/`).

## Inject the MeterRegistry

To register meters, you need a reference to the `MeterRegistry` that is configured and maintained by the Micrometer extension.

The `MeterRegistry` can be injected into your application as follows:

```java
    private final MeterRegistry registry;

    ExampleResource(MeterRegistry registry) {
        this.registry = registry;
    }
```

## Add a Counter

Counters are used to measure values that only increase.

Let's add a counter that tracks how often we test a number to see if it is prime. We'll add a dimensional label (also called an attribute or a tag) that will allow us to aggregate this counter value in different ways.

```java
    @GET
    @Path("prime/{number}")
    public String checkIfPrime(@PathParam("number") long number) {
        if (number < 1) {
            registry.counter("example.prime.number", "type", "not-natural") // <1>
                    .increment(); // <2>
            return "Only natural numbers can be prime numbers.";
        }
        if (number == 1) {
            registry.counter("example.prime.number", "type", "one") // <1>
                    .increment(); // <2>
            return number + " is not prime.";
        }
        if (number == 2 || number % 2 == 0) {
            registry.counter("example.prime.number", "type", "even") // <1>
                    .increment(); // <2>
            return number + " is not prime.";
        }
        if (testPrimeNumber(number)) {
            registry.counter("example.prime.number", "type", "prime") // <1>
                    .increment(); // <2>
            return number + " is prime.";
        } else {
            registry.counter("example.prime.number", "type", "not-prime") // <1>
                    .increment(); // <2>
            return number + " is not prime.";
        }
    }
```

1. Find or create a counter called `example.prime.number` that has a `type` label with the specified value.
2. Increment that counter.

### Review collected metrics

If you did not leave Quarkus running in dev mode, start it again:

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

Try the following sequence and look for `example_prime_number_total` in the plain text output.

Note that the `_total` suffix is added when Micrometer applies Prometheus naming conventions to `example.prime.number`, the originally specified counter name.

```shell
curl http://localhost:8080/example/prime/-1
curl http://localhost:8080/example/prime/0
curl http://localhost:8080/example/prime/1
curl http://localhost:8080/example/prime/2
curl http://localhost:8080/example/prime/3
curl http://localhost:8080/example/prime/15
curl http://localhost:8080/q/metrics
```

Notice that there is one measured value for each unique combination of `example_prime_number_total` and `type` value.

Looking at the dimensional data produced by this counter, you can count:

- how often a negative number was checked: `type="not-natural"`
- how often the number one was checked: `type="one"`
- how often an even number was checked: `type="even"`
- how often a prime number was checked: `type="prime"`
- how often a non-prime number was checked: `type="not-prime"`

You can also count how often a number was checked (generally) by aggregating all of these values together.

## Add a Timer

Timers are a specialized abstraction for measuring duration. Let's add a timer to measure how long it takes to determine if a number is prime.

```java
    @GET
    @Path("prime/{number}")
    public String checkIfPrime(@PathParam("number") long number) {
        if (number < 1) {
            registry.counter("example.prime.number", "type", "not-natural") // <1>
                    .increment(); // <2>
            return "Only natural numbers can be prime numbers.";
        }
        if (number == 1) {
            registry.counter("example.prime.number", "type", "one") // <1>
                    .increment(); // <2>
            return number + " is not prime.";
        }
        if (number == 2 || number % 2 == 0) {
            registry.counter("example.prime.number", "type", "even") // <1>
                    .increment(); // <2>
            return number + " is not prime.";
        }
        if (timedTestPrimeNumber(number)) { // <3>
            registry.counter("example.prime.number", "type", "prime") // <1>
                    .increment(); // <2>
            return number + " is prime.";
        } else {
            registry.counter("example.prime.number", "type", "not-prime") // <1>
                    .increment(); // <2>
            return number + " is not prime.";
        }
    }

    protected boolean timedTestPrimeNumber(long number) {
        Timer.Sample sample = Timer.start(registry); // <4>
        boolean result = testPrimeNumber(number); // <5>
        sample.stop(registry.timer("example.prime.number.test", "prime", result + "")); // <6>
        return result;
    }
```

1. Find or create a counter called `example.prime.number` that has a `type` label with the specified value.
2. Increment that counter.
3. Call a method that wraps the original `testPrimeNumber` method.
4. Create a `Timer.Sample` that tracks the start time
5. Call the method to be timed and store the boolean result
6. Find or create a `Timer` using the specified id and a `prime` label with the result value, and record the duration captured by the `Timer.Sample`.

### Review collected metrics

If you did not leave Quarkus running in dev mode, start it again:

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

Micrometer will apply Prometheus conventions when emitting metrics for this timer. Specifically, measured durations are converted into seconds and this unit is included in the metric name.

Try the following sequence and look for the following entries in the plain text output:

- `example_prime_number_test_seconds_count` -- how many times the method was called
- `example_prime_number_test_seconds_sum` -- the total duration of all method calls
- `example_prime_number_test_seconds_max` -- the maximum observed duration within a decaying interval. This value will return to 0 if the method is not invoked frequently.

```shell
curl http://localhost:8080/example/prime/256
curl http://localhost:8080/q/metrics
curl http://localhost:8080/example/prime/7919
curl http://localhost:8080/q/metrics
```

Looking at the dimensional data produced by this counter, you can use the sum and the count to calculate how long (on average) it takes to determine if a number is prime. Using the dimensional label, you might be able to understand if there is a significant difference in duration for numbers that are prime when compared with numbers that are not.

## Add a Gauge

Gauges measure a value that can increase or decrease over time, like the speedometer on a car. The value of a gauge is not accumulated, it is observed at collection time. Use a gauge to observe the size of a collection, or the value returned from a function.

```java
    private final LinkedList<Long> list = new LinkedList<>(); // <1>

    ExampleResource(MeterRegistry registry) {
        this.registry = registry;
        registry.gaugeCollectionSize("example.list.size", Tags.empty(), list); // <2>
    }

    @GET
    @Path("gauge/{number}")
    public Long checkListSize(@PathParam("number") long number) { // <3>
        if (number == 2 || number % 2 == 0) {
            // add even numbers to the list
            list.add(number);
        } else {
            // remove items from the list for odd numbers
            try {
                number = list.removeFirst();
            } catch (NoSuchElementException nse) {
                number = 0;
            }
        }
        return number;
    }
```

1. Define list that will hold arbitrary numbers.
2. Register a gauge that will track the size of the list.
3. Create a REST endpoint to populate the list. Even numbers are added to the list, and odd numbers remove an element from the list.

### Review collected metrics

If you did not leave Quarkus running in dev mode, start it again:

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

Then try the following sequence and look for `example_list_size` in the plain text output:

```shell
curl http://localhost:8080/example/gauge/1
curl http://localhost:8080/example/gauge/2
curl http://localhost:8080/example/gauge/4
curl http://localhost:8080/q/metrics
curl http://localhost:8080/example/gauge/6
curl http://localhost:8080/example/gauge/5
curl http://localhost:8080/example/gauge/7
curl http://localhost:8080/q/metrics
```

## Summary

Congratulations!

You have created a project that uses the Micrometer and Prometheus Meter Registry extensions to collect metrics. You've observed some of the metrics that Quarkus captures automatically, and have added a `Counter` and `Timer` that are unique to the application. You've also added dimensional labels to metrics, and have observed how those labels shape the data emitted by the prometheus endpoint.
