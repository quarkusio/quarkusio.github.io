# Micrometer Metrics

Micrometer provides an abstraction layer for metrics collection. It defines an API for basic meter types, like counters, gauges, timers, and distribution summaries, along with a `MeterRegistry` API that generalizes metrics collection and propagation for different backend monitoring systems.

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

> [!IMPORTANT]
> Micrometer is the recommended approach to metrics for Quarkus.

> [!NOTE]
> By default, the metrics are exposed on the main HTTP server. If you would like to surface metrics from a separate management port, see the [Managed interface](#management-interface) section.

## Micrometer and monitoring system extensions

Quarkus Micrometer extensions are structured in the same way as the Micrometer project. The `quarkus-micrometer` extension provides core Micrometer support and runtime integration. Other Quarkus and Quarkiverse extensions use the Quarkus Micrometer extension to provide support for other monitoring systems.

Quarkus extensions:

- micrometer
- micrometer-registry-prometheus
- micrometer-opentelemetry

[Quarkiverse extensions](https://github.com/quarkiverse/quarkus-micrometer-registry) (may be incomplete):

- micrometer-registry-azure-monitor
- micrometer-registry-datadog
- micrometer-registry-graphite
- micrometer-registry-influx
- micrometer-registry-jmx
- micrometer-registry-newrelic-telemetry
- micrometer-registry-otlp
- micrometer-registry-signalfx
- micrometer-registry-stackdriver
- micrometer-registry-statsd

To add support for Prometheus metrics to your application, for example, use the `micrometer-registry-prometheus` extension. It will bring in the Quarkus Micrometer extension and Micrometer core libraries as dependencies.

Add the extension to your project using following command (from your project directory):

**CLI**

```bash
quarkus extension add micrometer-registry-prometheus
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='micrometer-registry-prometheus'
```

**Gradle**

```bash
./gradlew addExtension --extensions='micrometer-registry-prometheus'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
```

And you're all set!

A similar process applies for other meter registry extensions. To use the Micrometer StackDriver MeterRegistry, for example, you would use the `quarkus-micrometer-registry-stackdriver` extension from the Quarkiverse:

**CLI**

```bash
quarkus extension add io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-stackdriver
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-stackdriver'
```

**Gradle**

```bash
./gradlew addExtension --extensions='io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-stackdriver'
```

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkiverse.micrometer.registry</groupId>
    <artifactId>quarkus-micrometer-registry-stackdriver</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-stackdriver")
```

### Other registry implementations

If the Micrometer registry you would like to use does not yet have an associated extension, use the `quarkus-micrometer` extension and bring in the Micrometer meter registry dependency directly:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer</artifactId>
</dependency>
<dependency>
    <groupId>com.acme</groupId>
    <artifactId>custom-micrometer-registry</artifactId>
    <version>...</version>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-micrometer")
implementation("com.acme:custom-micrometer-registry")
```

You will then need to specify your own provider to configure and initialize the MeterRegistry, as discussed in the next section.

### Send Micrometer metrics through OpenTelemetry

The [quarkus-micrometer-opentelemetry](/guides/telemetry-micrometer-to-opentelemetry/index.md) extension allows to use the Micrometer API and all existing automatic instrumentation available in Quarkus while sending all the telemetry with OpenTelemetry.

This is achieved with a Micrometer registry implemented with the OpenTelemetry SDK, allowing integration of Micrometer Metrics, OpenTelemetry Traces and Logs into a unified telemetry output using the OTLP protocol.

If you already have your Quarkus project configured, you can add the `quarkus-micrometer-opentelemetry` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add micrometer-opentelemetry
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='micrometer-opentelemetry'
```

**Gradle**

```bash
./gradlew addExtension --extensions='micrometer-opentelemetry'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-micrometer-opentelemetry</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-micrometer-opentelemetry")
```

### Create a customized MeterRegistry

Use a custom `@Produces` method to create and configure your own `MeterRegistry` if you need to.

The following example customizes the line format used for StatsD:

```java
@Produces
@Singleton // <1>
public StatsdMeterRegistry createStatsdMeterRegistry(StatsdConfig statsdConfig, Clock clock) { // <2>
    // define what to do with lines
    Consumer<String> lineLogger = line -> logger.info(line);

    // inject a configuration object, and then customize the line builder
    return StatsdMeterRegistry.builder(statsdConfig)
          .clock(clock)
          .lineSink(lineLogger)
          .build();
}
```

1. The method returns a `@Singleton`.
2. The method returns the specific type of `MeterRegistry`

This example corresponds to the following instructions in the Micrometer documentation: [Micrometer StatsD: Customizing the Metrics Sink](https://docs.micrometer.io/micrometer/reference/implementations/statsD.html#_customizing_the_metrics_sink)

Use MicroProfile Config to inject any configuration attributes you need to configure the registry. Most Micrometer registry extensions, like `quarkus-micrometer-registry-statsd`, provide registry-specific configuration objects that are integrated with the Quarkus configuration model. The [Quarkiverse GitHub Repository](https://github.com/quarkiverse/quarkus-micrometer-registry) can be a useful implementation reference.

## Create your own metrics

Metrics data is used in the aggregate to observe how data changes over time. This data is used for trend analysis, anomaly detection, and alerting. Data is stored by backend monitoring systems in time series databases, with new values appended to the end of the series.

> [!NOTE]
> Metrics are constructed lazily. You may not see any data for the metric you're looking for until you've performed an action that will create it, like visiting an endpoint.

### Naming conventions

Meter names should use dots to separate segments, `a.name.like.this`. Micrometer applies naming conventions to convert registered meter names to match the expectations of backend monitoring systems.

Given the following declaration of a timer: `registry.timer("http.server.requests")`, applied naming conventions will emit the following metrics for different monitoring systems:

- Prometheus: `http_server_requests_duration_seconds`
- Atlas: `httpServerRequests`
- Graphite: `http.server.requests`
- InfluxDB: `http_server_requests`

<a id="define-tags"></a>
### Define dimensions for aggregation

Metrics, single numerical measurements, often have additional data captured with them. This ancillary data is used to group or aggregate metrics for analysis. The Micrometer API refers to this dimensional data as tags, but you may see it referred to as "labels" or "attributes" in other documentation sources.

Micrometer is built primarily for backend monitoring systems that support dimensional data (metric names that are enriched with key/value pairs). For hierarchical systems that only support a flat metric name, Micrometer will flatten the set of key/value pairs (sorted by key) and add them to the name.

Tags can be specified when a meter is registered with a `MeterRegistry` or using a [Meter Filter](#meter-filter).

See the Micrometer documentation for additional advice on [tag naming](https://docs.micrometer.io/micrometer/reference/concepts/naming).

> [!IMPORTANT]
> Each unique combination of metric name and dimension produces a unique time series. Using an unbounded set of dimensional data can lead to a "cardinality explosion", an exponential increase in the creation of new time series.

### Obtain a reference to a MeterRegistry

To register meters, you need a reference to a `MeterRegistry`, which is configured and maintained by the Micrometer extension.

Use one of the following methods to obtain a reference to a `MeterRegistry`:

1. Use CDI Constructor injection:

   ```java
   package org.acme.micrometer;

   import io.micrometer.core.instrument.MeterRegistry;

   import jakarta.ws.rs.GET;
   import jakarta.ws.rs.Path;
   import jakarta.ws.rs.Produces;

   @Path("/example")
   @Produces("text/plain")
   public class ExampleResource {

       private final MeterRegistry registry;

       ExampleResource(MeterRegistry registry) {
           this.registry = registry;
       }
   }
   ```


1. Use a `MeterRegistry` member variable and use `@Inject`:

   ```java
       @Inject
       MeterRegistry registry;
   ```


1. Use the global `MeterRegistry`:

   ```java
       MeterRegistry registry = Metrics.globalRegistry;
   ```


### Gauges

Gauges measure a value that can increase or decrease over time, like the speedometer on a car. Gauges can be useful when monitoring the statistics for a cache or collection.

Gauge values are sampled rather than set; there is no record of how the value associated with a gauge may have changed between measurements.

Micrometer provides a few mechanisms for creating gauges:

1. Wrap construction of a collection to monitor its size:

   ```java
   List<String> list = registry.gaugeCollectionSize("fantastic.list", // <1>
           Tags.of("key", "value") // optional <2>
           new ArrayList<>());  // <3>
   ```

   1. Create a new gauge, `list.size`, using the dot-separated convention.
   2. Associate [tags](#define-tags) with the gauge. Gauge tag values are constant, and must be assigned at construction time.
   3. Construct the array list whose size should be observed.


1. Use a builder to create a Gauge that will call a function:

   ```java
   Gauge.builder("jvm.threads.peak", threadBean, ThreadMXBean::getPeakThreadCount) // <1>
       .baseUnit(BaseUnits.THREADS) // optional <2>
       .description("The peak live thread count...") // optional <3>
       .tags("key", "value") // optional <4>
       .register(registry); // <5>
   ```

   1. Create a new gauge called `jvm.threads.peak` that will call `getPeakThreadCount` on `threadBean`, an instance of `ThreadMXBean`
   2. Define the base unit, see [BaseUnits.java](https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/BaseUnits.java) for predefined values.
   3. Provide a description of the Gauge
   4. Associate [tags](#define-tags) with the gauge
   5. Register the Gauge with the MeterRegistry


See [Gauges](https://docs.micrometer.io/micrometer/reference/concepts/gauges) in the Micrometer documentation for more information and examples. Of note are two special cases: `TimeGauge` for measuring time, and a `MultiGauge` for reporting several criteria together.

> [!NOTE]
> Micrometer does not create strong references to the objects it observes by default. Depending on the registry, Micrometer either omits gauges that observe objects that have been garbage-collected entirely or uses `NaN` (not a number) as the observed value.

When should you use a gauge? Only use a gauge when you can't use something else. Gauges can be less straight-forward to use than other meters. If what you are measuring can be counted (because the value always increments), use a counter instead.

### Counters

Counters measure values that only increase. Use one of the methods below to create a counter.

1. Use a convenience method on the `MeterRegistry`:

   ```java
   registry.counter("example.prime.number", "type", "prime"); // <1><2>
   ```

   1. `example.prime.number` is the counter name.
   2. `type` is a dimensional tag with value `prime`.


1. Use `Counter.builder` to provide a description and units:

   ```java
   Counter.builder("count.me") // <1>
       .baseUnit("beans")            // optional <2>
       .description("a description") // optional <3>
       .tags("region", "test")       // optional <4>
       .register(registry);
   ```

   1. Create a new counter called `count.me`
   2. Define a custom base unit. See [BaseUnits.java](https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/BaseUnits.java) for predefined values.
   3. Provide a description for the counter
   4. Associate [tags](#define-tags) with the counter


1. [Annotate](#annotations) a method

   ```java
   @Counted(value = "counted.method", extraTags = { "extra", "annotated" }) // <1><2>
   void countThisMethod(){
       ...
   }
   ```

   1. A CDI interceptor will create and register a counter called `counted.method`
   2. The interceptor-created counter will have the "extra" dimension tag with value "annotated"


See [Counters](https://docs.micrometer.io/micrometer/reference/concepts/counters) in the Micrometer documentation for more information and examples, including the less common `FunctionCounter` that can be used to measure the result returned by an always increasing function.

When should you use a counter? Use a counter if you are doing something that can not be either timed or summarized. If you want to understand more about how a value is changing, a timer (when the base unit of measurement is time) or a distribution summary might be more appropriate.

### Summaries and Timers

Timers and distribution summaries in Micrometer are very similar. Both meters record data, and can capture additional histogram or percentile data. While distribution summaries can be use for arbitrary types of data, timers are optimized for measuring time and durations.

Timers and distribution summaries store at least three values internally:

- the aggregation of all recorded values as a sum
- the number of values that have been recorded (a counter)
- the highest value seen within a decaying time window (a gauge).

#### Create a distribution summary

Use a distribution summary to record a value, not time. Use one of the following methods to create a distribution summary.

1. Use a convenience method on the `MeterRegistry`:

   ```java
   registry.summary("bytes.written", "protocol", "http"); // <1><2>
   ```

   1. `bytes.written` is the summary name
   2. `protocol` is a dimensional tag with value `http`.


1. Use `DistributionSummary.builder` to provide a description and units:

   ```java
   DistributionSummary.builder("response.size") // <1>
       .baseUnit("bytes")            // optional <2>
       .description("a description") // optional <3>
       .tags("protocol", "http")     // optional <4>
       .register(registry);
   ```

   1. Create a new distribution summary called `response.size`
   2. Use `bytes` as a base unit. See [BaseUnits.java](https://github.com/micrometer-metrics/micrometer/blob/main/micrometer-core/src/main/java/io/micrometer/core/instrument/binder/BaseUnits.java) for predefined values.
   3. Provide a description for the distribution summary
   4. Associate [tags](#define-tags) with the distribution summary


#### Create a timer

Timers measure short-duration latencies and how often they occur. Negative values are not supported, and longer durations could cause an overflow of the total time (Long.MAX_VALUE nanoseconds (292.3 years)).

Use one of the following methods to construct a timer.

1. Use a convenience method on the `MeterRegistry`:

   ```java
   registry.timer("fabric.selection", "primary", "blue"); // <1><2>
   ```

   1. `fabric.selection` is the summary name
   2. `primary` is a dimensional tag with value `blue`.


1. Use `Timer.builder` to provide a description and units:

   ```java
   Timer.builder("my.timer")        // <1><2>
       .description("description ") // optional <3>
       .tags("region", "test")      // optional <4>
       .register(registry);
   ```

   1. Create a new timer called `my.timer`
   2. Timers measure time, and will convert it into the units required by the monitoring backend
   3. Provide a description for the distribution summary
   4. Associate [tags](#define-tags) with the timer


1. [Annotate](#annotations) a method

   ```java
   @Timed(value = "call", extraTags = {"region", "test"}) // <1><2>
   ```

   1. A CDI interceptor will create and register a timer called `call`
   2. The interceptor-created timer will have the "region" dimension tag with value "test"


#### Measure durations with Timers

Micrometer provides the following convenience mechanisms for recording durations.

1. Wrap the invocation of a `Runnable`:

   ```java
   timer.record(() -> noReturnValue());
   ```


1. Wrap the invocation of a `Callable`:

   ```java
   timer.recordCallable(() -> returnValue());
   ```


1. Create a wrapped `Runnable` for repeated invocation:

   ```java
   Runnable r = timer.wrap(() -> noReturnValue());
   ```


1. Create a wrapped `Callable` for repeated invocation:

   ```java
   Callable c = timer.wrap(() -> returnValue());
   ```


1. Use a `Sample` for more complex code paths:

   ```java
   Sample sample = Timer.start(registry); // <1>

   doStuff; // <2>

   sample.stop(registry.timer("my.timer", "response", response.status())); // <3>
   ```

   1. We create a sample, which records the start of the timer.
   2. The sample can be passed along as context
   3. We can choose the timer when the sample is stopped. This example uses a response status as a tag identifying the timer, which won't be known until processing is complete.


#### Histograms and percentiles

Both timers and distribution summaries can be configured to emit additional statistics, like histogram data, precomputed percentiles, or service level objective (SLO) boundaries. See [Timers](https://docs.micrometer.io/micrometer/reference/concepts/timers) and [Distribution Summaries](https://docs.micrometer.io/micrometer/reference/concepts/distribution-summaries) in the Micrometer documentation for more information and examples, including memory footprint estimation for both types.

> [!IMPORTANT]
> The count, sum, and histogram data associated with timers and distribution summaries can be re-aggregated across dimensions (or across a series of instances).
>
> Precomputed percentile values can not. Percentiles are unique to each dataset (the 90th percentile of this collection of measurements).

## Automatically generated metrics

### HTTP server

The Micrometer extension automatically times HTTP server requests. Following Prometheus naming conventions for timers, look for `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, and `http_server_requests_seconds_max`. Dimensional labels have been added for the requested uri, the HTTP method (GET, POST, etc.), the status code (200, 302, 404, etc.), and a more general outcome field.

You can disable measurement of HTTP endpoints using the `quarkus.micrometer.binder.http-server.ignore-patterns` property. This property accepts a comma-separated list of simple regex match patterns identifying URI paths that should be ignored. For example, setting `quarkus.micrometer.binder.http-server.ignore-patterns=/example/prime/[0-9]+` will ignore a request to `http://localhost:8080/example/prime/7919`. A request to `http://localhost:8080/example/gauge/7919` would still be measured.

The micrometer extension will make a best effort at representing URIs containing path parameters in templated form. Using examples from above, a request to `http://localhost:8080/example/prime/7919` should appear as an attribute of `http_server_requests_seconds_*` metrics with a value of `uri=/example/prime/{number}`.

Use the `quarkus.micrometer.binder.http-server.match-patterns` property if the correct URL can not be determined. This property accepts a comma-separated list defining an association between a simple regex match pattern and a replacement string. For example, setting `quarkus.micrometer.binder.http-server.match-patterns=/example/prime/[0-9]+=/example/{jellybeans}` would use the value `/example/{jellybeans}` for the uri attribute any time the requested uri matches `/example/prime/[0-9]+`.

By default, the  metrics are exported using the Prometheus format `application/openmetrics-text`, you can revert to the former format by specifying the `Accept` request header to `text/plain` (`curl -H "Accept: text/plain" localhost:8080/q/metrics/`).

### Netty memory management metrics

[Netty](https://netty.io/) memory management metrics are important because Quarkus is built on top of [Eclipse Vert.x](https://vertx.io/), which is a framework built on top of Netty to create reactive applications in Java. Even if you use imperative style programing in Quarkus, under the surface it is conveniently handled by Vert.x in a reactive way.

In short, Netty provides an *efficient* memory management system for asynchronous applications. It offers different types of memory allocation strategies, including **heap** (stored in the JVM heap) and direct memory (allocated outside the JVM memory), along with a **buffer pooling mechanism** to optimize allocations and reduce garbage collection (GC) overhead.

#### Memory Categories in Netty

Netty uses two primary memory categories:

1. **Heap Memory**
   - Allocated from the JVM heap (`byte[]`).
   - Managed by the garbage collector.
   - Slower for I/O operations because it requires copying data to/from direct memory before sending over a socket.

1. **Direct Memory**
   - Allocated outside the JVM heap using `ByteBuffer.allocateDirect()`.
   - Not managed by the garbage collector, reducing GC pressure.
   - Faster for I/O operations as it avoids unnecessary copying.

> [!NOTE]
> Using a very high value for -XX:MaxRAMPercentage (allocating most of the available RAM to the Java heap) is not recommended for Quarkus applications, as it’s important to leave room for Netty's native memory allocations.
>
> These Netty metrics help evaluate how much room is needed.

#### Pooling Mechanism

Netty uses **PooledByteBufAllocator** to reduce memory allocation overhead and fragmentation. It is based on a **jemalloc-style arena system** (see [jemalloc/jemalloc](https://github.com/jemalloc/jemalloc)), which includes:

- **Arenas**: Large memory blocks that manage allocations.
- **Chunks**: Memory segments within arenas (typically 16MB in size).
- **Pages**: Fixed-size portions of a chunk (usually 8KB).
- **Subpages**: Smaller allocations within a page for tiny objects.
- **Thread-local Caches**: Per-thread cache for frequently used small buffers, reducing contention.

#### Metrics for Netty Memory Management

The Micrometer metrics report key metrics from Netty's allocator, helping monitor memory usage, pinned memory, pooling efficiency, and cache behavior.

##### **1. Memory Usage Metrics (Every allocator)**

These metrics track the current memory usage by Netty allocators (in bytes)

| Metric Name | Description | Type | Tags |
| --- | --- | --- | --- |
| `allocator.memory.used` | Amount of memory currently used by the allocator | Gauge | `memory_type=[heap, direct]`, `name=allocator-name` |

##### **2. Pooled  Allocator - Pinned Memory Metrics**

Pinned memory refers to buffers that cannot be recycled due to active references, preventing reuse by the allocator. A *high* number may indicate some kind of leak (as reference are not released).

| Metric Name | Description | Type | Tags |
| --- | --- | --- | --- |
| `allocator.memory.pinned` | Amount of pinned memory that cannot be reused | Gauge | `memory_type=[heap, direct]`, `name=allocator-name` |

##### **3. Pooled Allocator - Arena Metrics**

Arenas (large size of memory) manage memory allocations efficiently by grouping chunks of memory. So this metric is the number of arenas allocated for a given allocator.

| Metric Name | Description | Type | Tags |
| --- | --- | --- | --- |
| `allocator.pooled.arenas` | Number of arenas handling memory allocation | Gauge | `memory_type=[heap, direct]` |

##### **4. Cache Size Metrics**

Netty caches small buffers within a given thread to avoid frequent memory allocations and improve performance. If the same thread needs memory, it will first check in its own cache. This is particularly efficient due to the event loop threading model (low number of threads). Note that the tiny caches are now merged into the small ones.

| Metric Name | Description | Type | Tags |
| --- | --- | --- | --- |
| `allocator.pooled.cache.size` | Size of the buffer cache | Gauge | `cache_type=[small, normal]` |

##### **5. Thread-Local Cache Metrics**

These metrics indicate the number of active thread-local caches used for memory pooling. It complements the cache size metrics (cache being the size, and this being the number of cache)

| Metric Name | Description | Type | Tags |
| --- | --- | --- | --- |
| `allocator.pooled.threadlocal.caches` | Number of thread-local caches for buffers | Gauge | None |

##### **6. Chunk Size Metrics**

Chunks are the primary unit of memory allocation in Netty's arenas.

| Metric Name | Description | Type | Tags |
| --- | --- | --- | --- |
| `allocator.pooled.chunk.size` | Size of a memory chunk in pooled allocator | Gauge | None |

#### Unpooled

This uses a utility class providing static methods to create new buffers directly and bypassing the pooling mechanism. The user must also release the allocated buffers.

> [!NOTE]
> It has been [reported](https://github.com/quarkusio/quarkus/issues/50538) that some libraries using the `UnpooledByteBufAllocator` might not release some heap buffers. This will not decrement the reported allocation even if memory is no longer reserved. This might lead to astronomical values in reported memory.

#### Debugging

If high allocation values are reported, there are 2 ways to determine whether it’s a real memory leak:

- By enabling [leak detection](https://netty.io/wiki/reference-counted-objects.html#leak-detection-levels), as documented by Netty. Example:

```bash
java -Dio.netty.leakDetection.level=advanced ...
```

- Inspect a memory dump from the application.

<a id="quarkus-extensions-using-micrometer"></a>
### List of Quarkus core extensions automatically instrumented with Micrometer

- [`quarkus-agroal`](https://quarkus.io/extensions/io.quarkus/quarkus-agroal)
- [`quarkus-cache`](https://quarkus.io/guides/cache)
- [`quarkus-caffeine`](https://quarkus.io/extensions/io.quarkus/quarkus-caffeine)
- [`quarkus-smallrye-fault-tolerance`](https://quarkus.io/guides/smallrye-fault-tolerance)
- [`quarkus-grpc`](https://quarkus.io/guides/grpc-getting-started)
- [`quarkus-hibernate-orm`](https://quarkus.io/guides/hibernate-orm)
- [`quarkus-jaxrs-client-reactive`](https://quarkus.io/extensions/io.quarkus/quarkus-jaxrs-client-reactive)
- [`quarkus-kubernetes`](https://quarkus.io/guides/deploying-to-kubernetes)
- [`jboss-logging`](https://quarkus.io/guides/logging) (logging API in Quarkus)
- [`quarkus-mongodb-client`](https://quarkus.io/guides/mongodb)
- [`quarkus-netty`](https://quarkus.io/extensions/io.quarkus/quarkus-netty)
- [`quarkus-redis-client`](https://quarkus.io/guides/redis)
- [`quarkus-rest`](https://quarkus.io/guides/rest)
- [`quarkus-resteasy-jackson`](https://quarkus.io/guides/resteasy)
- [`quarkus-resteasy-client`](https://quarkus.io/guides/resteasy-client)
- [`quarkus-scheduler`](https://quarkus.io/guides/scheduler)
- [`quarkus-smallrye-graphql`](https://quarkus.io/guides/smallrye-graphql)
- [`quarkus-messaging`](https://quarkus.io/extensions/io.quarkus/quarkus-messaging)
- 
  - AMQP 1.0
  - RabbitMQ
  - Kafka
  - Pulsar
  - JMS
  - MQTT
  - Camel Messaging
- [`quarkus-smallrye-stork`](https://quarkus.io/guides/stork-reference)
- [`quarkus-vertx`](https://quarkus.io/guides/vertx) (http requests)
- [`websockets-next`](/guides/websockets-next-reference/index.md)

## Customizing Micrometer

Quarkus provides a variety of way to customize Micrometer.

<a id="meter-filter"></a>
### Use `MeterFilter` to customize emitted tags and metrics

Micrometer uses `MeterFilter` instances to customize the metrics emitted by `MeterRegistry` instances. The Micrometer extension will detect `MeterFilter` CDI beans and use them when initializing `MeterRegistry` instances.

```java
@Singleton
public class CustomConfiguration {

    @ConfigProperty(name = "deployment.env")
    String deploymentEnv;

    /** Define common tags that apply only to a Prometheus Registry */
    @Produces
    @Singleton
    @MeterFilterConstraint(applyTo = PrometheusMeterRegistry.class)
    public MeterFilter configurePrometheusRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("registry", "prometheus")));
    }

    /** Define common tags that apply globally */
    @Produces
    @Singleton
    public MeterFilter configureAllRegistries() {
        return MeterFilter.commonTags(Arrays.asList(
                Tag.of("env", deploymentEnv)));
    }

    /** Enable histogram buckets for a specific timer */
    @Produces
    @Singleton
    public MeterFilter enableHistogram() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if(id.getName().startsWith("myservice")) {
                    return DistributionStatisticConfig.builder()
                        .percentiles(0.5, 0.95)     // median and 95th percentile, not aggregable
                        .percentilesHistogram(true) // histogram buckets (e.g. prometheus histogram_quantile)
                        .build()
                        .merge(config);
                }
                return config;
            }
        };
    }
}
```

In this example, a singleton CDI bean will produce two different `MeterFilter` beans. One will be applied only to Prometheus `MeterRegistry` instances (using the `@MeterFilterConstraint` qualifier), and another will be applied to all `MeterRegistry` instances. An application configuration property is also injected and used as a tag value. Additional examples of MeterFilters can be found in the [official documentation](https://docs.micrometer.io/micrometer/reference/concepts.html).

### Use `HttpServerMetricsTagsContributor` for server HTTP requests

By providing CDI beans that implement `io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor`, user code can contribute arbitrary tags based on the details of HTTP request and responses.

> [!CAUTION]
> When creating tags using this interface, it's important to limit the cardinality of the values, otherwise there is a risk of severely degrading the metrics system's capacity.

### Use `HttpClientMetricsTagsContributor` for client HTTP requests

By providing CDI beans that implement `io.quarkus.micrometer.runtime.HttpClientMetricsTagsContributor`, user code can contribute arbitrary tags based on the details of HTTP request

### Use `MeterRegistryCustomizer` for arbitrary customizations to meter registries

By providing CDI beans that implement `io.quarkus.micrometer.runtime.MeterRegistryCustomizer` user code has the change to change the configuration of any `MeterRegistry` that has been activated. Unless an implementation is annotated with `@io.quarkus.micrometer.runtime.MeterRegistryCustomizerConstraint`, the customization applies to all `MeterRegistry` instances.

<a id="annotations"></a>
## Does Micrometer support annotations?

Micrometer does define two annotations, `@Counted` and `@Timed`, that can be added to methods. The `@Timed` annotation will wrap the execution of a method and will emit the following tags in addition to any tags defined on the annotation itself: class, method, and exception (either "none" or the simple class name of a detected exception).

Parameters to `@Counted` and `@Timed` can be annotated with `@MeterTag` to dynamically assign meaningful tag values.

`MeterTag.resolver` can be used to extract a tag from a method parameter, by creating a bean implementing `io.micrometer.common.annotation.ValueResolver` and referring to this class: `@MeterTag(resolver=CustomResolver.class)`

`MeterTag.expression` is also supported, but you have to implement the evaluation of the expression by creating a bean implementing `io.micrometer.common.annotation.ValueExpressionResolver` that can evaluate expressions.

```java
enum Currency { USD, EUR }

@Singleton
class EnumOrdinalResolver implements ValueResolver {
    @Override
    public String resolve(Object parameter) {
        if(parameter instanceof Enum) {
            return String.valueOf(((Enum<?>) parameter).ordinal());
        }
        return null;
    }
}

@Singleton
public class MyExpressionResolver implements ValueExpressionResolver {
    @Override
    public String resolve(String expression, Object parameter) {
        return someParser.parse(expression).evaluate(parameter);
    }
}

// tags = type=with_enum, currency=${currency.toString()}
@Timed(value="time_something", extraTags = {"type", "with_enum"})
public Something calculateSomething(@MeterTag Currency currency) { ... }

// tags = type=with_enum, the_currency=${currency.toString()}
@Timed(value="time_something", extraTags = {"type", "with_enum"})
public Something calculateSomething(@MeterTag(key="the_currency") Currency currency) { ... }

// tags = type=with_enum, currency=${currency.ordinal()}
@Timed(value="time_something", extraTags = {"type", "with_enum"})
public Something calculateSomething(@MeterTag(resolver=EnumOrdinalResolver.class) Currency currency) { ... }

// tags = type=with_enum, currency=${currency.ordinal()}
@Timed(value="time_something", extraTags = {"type", "with_enum"})
public Something calculateSomething(@MeterTag(expression="currency.ordinal()") Currency currency) { ... }
```

> [!IMPORTANT]
> Provided tag values MUST BE of LOW-CARDINALITY. High-cardinality values can lead to performance and storage issues in your metrics backend (a "cardinality explosion"). Tag values should not use end-user data, since those could be high-cardinality.

Many methods, like REST endpoint methods or Vert.x Routes, are counted and timed by the micrometer extension out of the box.

<a id="management-interface"></a>
## Management interface

By default, the metrics are exposed on the main HTTP server.

You can expose them on a separate network interface and port by setting `quarkus.management.enabled=true` in your application configuration. Note that this property is a build-time property. The value cannot be overridden at runtime.

If you enable the management interface without customizing the management network interface and port, the metrics are exposed under: `http://0.0.0.0:9000/q/metrics`.

You can configure the path of each exposed format using:

```properties
quarkus.micrometer.export.json.enabled=true <1>
quarkus.micrometer.export.json.path=metrics/json
quarkus.micrometer.export.prometheus.path=metrics/prometheus
```

1. Enable JSON metrics

With such a configuration, the JSON metrics will be available from `http://0.0.0.0:9000/q/metrics/json`. The Prometheus metrics will be available from `http://0.0.0.0:9000/q/metrics/prometheus`.

Refer to the [management interface reference](/guides/management-interface-reference/index.md) for more information.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-enabled"></a> [`quarkus.micrometer.enabled`](#quarkus-micrometer_quarkus-micrometer-enabled) config_property_copy_button:+++quarkus.micrometer.enabled+++[]<br><br>Micrometer metrics support.<br><br>Micrometer metrics support is enabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-registry-enabled-default"></a> [`quarkus.micrometer.registry-enabled-default`](#quarkus-micrometer_quarkus-micrometer-registry-enabled-default) config_property_copy_button:+++quarkus.micrometer.registry-enabled-default+++[]<br><br>Micrometer MeterRegistry discovery.<br><br>Micrometer MeterRegistry implementations discovered on the classpath will be enabled automatically by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_REGISTRY_ENABLED_DEFAULT+++[] | boolean | `true` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-enabled-default"></a> [`quarkus.micrometer.binder-enabled-default`](#quarkus-micrometer_quarkus-micrometer-binder-enabled-default) config_property_copy_button:+++quarkus.micrometer.binder-enabled-default+++[]<br><br>Micrometer MeterBinder discovery. In other words, enables the automatic metrics instrumentation.<br><br>Micrometer MeterBinder implementations discovered on the classpath will be enabled automatically by default. In other words, automatic metrics instrumentation will be ON by default.<br><br>`quarkus.micrometer.binder.enable-all` overrides this property, meaning when this is set to `false`, and `enable-all` is true, discovery of all MeterBinder will still happen.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_ENABLED_DEFAULT+++[] | boolean | `true` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-http-additional-methods"></a> [`quarkus.micrometer.binder.http.additional-methods`](#quarkus-micrometer_quarkus-micrometer-binder-http-additional-methods) config_property_copy_button:+++quarkus.micrometer.binder.http.additional-methods+++[]<br><br>Comma-separated list of additional HTTP methods to allow in the {@code method} metric tag, beyond the standard RFC 9110/5789/9512 methods (GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH, QUERY).<br><br>Non-standard HTTP methods are replaced with {@code UNKNOWN} by default to prevent metrics cardinality explosion. Use this property to allow specific custom methods (e.g. WebDAV methods like {@code PROPFIND}).<br><br>The total number of methods to track cannot exceed 32.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_ADDITIONAL_METHODS+++[] | list of string |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-http-client-enabled"></a> [`quarkus.micrometer.binder.http-client.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-http-client-enabled) config_property_copy_button:+++quarkus.micrometer.binder.http-client.enabled+++[]<br><br>Outbound HTTP request metrics support.<br><br>Support for HTTP client metrics will be enabled if Micrometer support is enabled, the REST client feature is enabled, and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_CLIENT_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-http-server-enabled"></a> [`quarkus.micrometer.binder.http-server.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-http-server-enabled) config_property_copy_button:+++quarkus.micrometer.binder.http-server.enabled+++[]<br><br>Inbound HTTP metrics support.<br><br>Support for HTTP server metrics will be enabled if Micrometer support is enabled, an extension serving HTTP traffic is enabled, and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_SERVER_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-jvm"></a> [`quarkus.micrometer.binder.jvm`](#quarkus-micrometer_quarkus-micrometer-binder-jvm) config_property_copy_button:+++quarkus.micrometer.binder.jvm+++[]<br><br>Micrometer JVM metrics support.<br><br>Support for JVM metrics will be enabled if Micrometer support is enabled, and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_JVM+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-kafka-enabled"></a> [`quarkus.micrometer.binder.kafka.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-kafka-enabled) config_property_copy_button:+++quarkus.micrometer.binder.kafka.enabled+++[]<br><br>Kafka metrics support.<br><br>Support for Kafka metrics will be enabled if Micrometer support is enabled, the Kafka Consumer or Producer interface is on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_KAFKA_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-redis-enabled"></a> [`quarkus.micrometer.binder.redis.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-redis-enabled) config_property_copy_button:+++quarkus.micrometer.binder.redis.enabled+++[]<br><br>Redis client metrics support.<br><br>Support for Redis metrics will be enabled if Micrometer support is enabled, the Quarkus Redis client extension is on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_REDIS_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-stork-enabled"></a> [`quarkus.micrometer.binder.stork.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-stork-enabled) config_property_copy_button:+++quarkus.micrometer.binder.stork.enabled+++[]<br><br>Stork metrics support.<br><br>Support for Stork metrics will be enabled if Micrometer support is enabled, the Quarkus Stork extension is on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_STORK_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-grpc-server-enabled"></a> [`quarkus.micrometer.binder.grpc-server.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-grpc-server-enabled) config_property_copy_button:+++quarkus.micrometer.binder.grpc-server.enabled+++[]<br><br>gRPC Server metrics support.<br><br>Support for gRPC server metrics will be enabled if Micrometer support is enabled, the gRPC server interfaces are on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_GRPC_SERVER_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-grpc-client-enabled"></a> [`quarkus.micrometer.binder.grpc-client.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-grpc-client-enabled) config_property_copy_button:+++quarkus.micrometer.binder.grpc-client.enabled+++[]<br><br>gRPC Client metrics support.<br><br>Support for gRPC client metrics will be enabled if Micrometer support is enabled, the gRPC client interfaces are on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_GRPC_CLIENT_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-messaging-enabled"></a> [`quarkus.micrometer.binder.messaging.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-messaging-enabled) config_property_copy_button:+++quarkus.micrometer.binder.messaging.enabled+++[]<br><br>Kafka metrics support.<br><br>Support for Reactive Messaging metrics will be enabled if Micrometer support is enabled, MessageObservationCollector interface is on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_MESSAGING_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-virtual-threads-enabled"></a> [`quarkus.micrometer.binder.virtual-threads.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-virtual-threads-enabled) config_property_copy_button:+++quarkus.micrometer.binder.virtual-threads.enabled+++[]<br><br>Virtual Threads metrics support.<br><br>Support for virtual threads metrics will be enabled if Micrometer support is enabled, this value is set to `true` (default), the JVM supports virtual threads (Java 21{plus}) and the `quarkus.micrometer.binder-enabled-default` property is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_VIRTUAL_THREADS_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-virtual-threads-tags"></a> [`quarkus.micrometer.binder.virtual-threads.tags`](#quarkus-micrometer_quarkus-micrometer-binder-virtual-threads-tags) config_property_copy_button:+++quarkus.micrometer.binder.virtual-threads.tags+++[]<br><br>The tags to be added to the metrics. Empty by default. When set, tags are passed as: `key1=value1,key2=value2`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_VIRTUAL_THREADS_TAGS+++[] | list of string |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-system"></a> [`quarkus.micrometer.binder.system`](#quarkus-micrometer_quarkus-micrometer-binder-system) config_property_copy_button:+++quarkus.micrometer.binder.system+++[]<br><br>Micrometer System metrics support.<br><br>Support for System metrics will be enabled if Micrometer support is enabled, and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_SYSTEM+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-vertx-enabled"></a> [`quarkus.micrometer.binder.vertx.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-vertx-enabled) config_property_copy_button:+++quarkus.micrometer.binder.vertx.enabled+++[]<br><br>Vert.x metrics support.<br><br>Support for Vert.x metrics will be enabled if Micrometer support is enabled, Vert.x MetricsOptions is on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_VERTX_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-netty-enabled"></a> [`quarkus.micrometer.binder.netty.enabled`](#quarkus-micrometer_quarkus-micrometer-binder-netty-enabled) config_property_copy_button:+++quarkus.micrometer.binder.netty.enabled+++[]<br><br>Netty metrics support.<br><br>Support for Netty metrics will be enabled if Micrometer support is enabled, the Netty allocator classes are on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.binder-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_NETTY_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-binder-enable-all"></a> [`quarkus.micrometer.binder.enable-all`](#quarkus-micrometer_quarkus-micrometer-binder-enable-all) config_property_copy_button:+++quarkus.micrometer.binder.enable-all+++[]<br><br>Enable all binders. Activates all metrics regardless off their particular default.<br><br>This property has precedence over all `BinderConfig` binders. In other words, if the `quarkus.micrometer.binder.jvm` is set to `false` and `quarkus.micrometer.binder.enabled-all` is set to `true`, all JVM metrics will be enabled.<br><br>Also takes precedence over `quarkus.micrometer.binder-enabled-default`, if binder discover is disabled, discovery of all metrics will still happen.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_ENABLE_ALL+++[] | boolean | `false` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-json-enabled"></a> [`quarkus.micrometer.export.json.enabled`](#quarkus-micrometer_quarkus-micrometer-export-json-enabled) config_property_copy_button:+++quarkus.micrometer.export.json.enabled+++[]<br><br>Support for export to JSON format. Off by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_JSON_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-json-path"></a> [`quarkus.micrometer.export.json.path`](#quarkus-micrometer_quarkus-micrometer-export-json-path) config_property_copy_button:+++quarkus.micrometer.export.json.path+++[]<br><br>The path for the JSON metrics endpoint. The default value is `metrics`. By default, this value will be resolved as a path relative to `${quarkus.http.non-application-root-path}`. If the management interface is enabled, the value will be resolved as a path relative to `${quarkus.management.root-path}`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_JSON_PATH+++[] | string | `metrics` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-json-buffer-length"></a> [`quarkus.micrometer.export.json.buffer-length`](#quarkus-micrometer_quarkus-micrometer-export-json-buffer-length) config_property_copy_button:+++quarkus.micrometer.export.json.buffer-length+++[]<br><br>Statistics like max, percentiles, and histogram counts decay over time to give greater weight to recent samples. Samples are accumulated to such statistics in ring buffers which rotate after the expiry, with this buffer length.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_JSON_BUFFER_LENGTH+++[] | int | `3` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-json-expiry"></a> [`quarkus.micrometer.export.json.expiry`](#quarkus-micrometer_quarkus-micrometer-export-json-expiry) config_property_copy_button:+++quarkus.micrometer.export.json.expiry+++[]<br><br>Statistics like max, percentiles, and histogram counts decay over time to give greater weight to recent samples. Samples are accumulated to such statistics in ring buffers which rotate after this expiry, with a particular buffer length.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_JSON_EXPIRY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-micrometer_quarkus-micrometer) | `P3D` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-prometheus-enabled"></a> [`quarkus.micrometer.export.prometheus.enabled`](#quarkus-micrometer_quarkus-micrometer-export-prometheus-enabled) config_property_copy_button:+++quarkus.micrometer.export.prometheus.enabled+++[]<br><br>Support for export to Prometheus.<br><br>Support for Prometheus will be enabled if Micrometer support is enabled, the PrometheusMeterRegistry is on the classpath and either this value is true, or this value is unset and `quarkus.micrometer.registry-enabled-default` is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_PROMETHEUS_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-prometheus-path"></a> [`quarkus.micrometer.export.prometheus.path`](#quarkus-micrometer_quarkus-micrometer-export-prometheus-path) config_property_copy_button:+++quarkus.micrometer.export.prometheus.path+++[]<br><br>The path for the prometheus metrics endpoint (produces text/plain). The default value is `metrics` and is resolved relative to the non-application endpoint (`q`), e.g. `${quarkus.http.root-path}/${quarkus.http.non-application-root-path}/metrics`. If an absolute path is specified (`/metrics`), the prometheus endpoint will be served from the configured path.<br><br>If the management interface is enabled, the value will be resolved as a path relative to `${quarkus.management.root-path}` (`q` by default), e.g. `http://${quarkus.management.host}:${quarkus.management.port}/${quarkus.management.root-path}/metrics`. If an absolute path is specified (`/metrics`), the prometheus endpoint will be served from the configured path, e.g. `http://${quarkus.management.host}:${quarkus.management.port}/metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_PROMETHEUS_PATH+++[] | string | `metrics` |
| [lock] <a id="quarkus-micrometer_quarkus-micrometer-export-prometheus-default-registry"></a> [`quarkus.micrometer.export.prometheus.default-registry`](#quarkus-micrometer_quarkus-micrometer-export-prometheus-default-registry) config_property_copy_button:+++quarkus.micrometer.export.prometheus.default-registry+++[]<br><br>By default, this extension will create a Prometheus MeterRegistry instance.<br><br>Use this attribute to veto the creation of the default Prometheus MeterRegistry.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_PROMETHEUS_DEFAULT_REGISTRY+++[] | boolean | `true` |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-client-match-patterns"></a> [`quarkus.micrometer.binder.http-client.match-patterns`](#quarkus-micrometer_quarkus-micrometer-binder-http-client-match-patterns) config_property_copy_button:+++quarkus.micrometer.binder.http-client.match-patterns+++[]<br><br>Comma-separated list of regular expressions used to specify uri labels in http metrics.<br><br>Outbount HTTP client instrumentation will attempt to transform parameterized resource paths, `/item/123`, into a generic form, `/item/telemetry-micrometer`, to reduce the cardinality of uri label values.<br><br>Patterns specified here will take precedence over those computed values.<br><br>For example, if `/item/\\\\d+=/item/custom` or `/item/[0-9]+=/item/custom` is specified in this list, a request to a matching path (`/item/123`) will use the specified replacement value (`/item/custom`) as the value for the uri label. Note that backslashes must be double escaped as `\\\\`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_CLIENT_MATCH_PATTERNS+++[] | list of string |  |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-client-ignore-patterns"></a> [`quarkus.micrometer.binder.http-client.ignore-patterns`](#quarkus-micrometer_quarkus-micrometer-binder-http-client-ignore-patterns) config_property_copy_button:+++quarkus.micrometer.binder.http-client.ignore-patterns+++[]<br><br>Comma-separated list of regular expressions defining uri paths that should be ignored (not measured).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_CLIENT_IGNORE_PATTERNS+++[] | list of string |  |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-client-suppress4xx-errors"></a> [`quarkus.micrometer.binder.http-client.suppress4xx-errors`](#quarkus-micrometer_quarkus-micrometer-binder-http-client-suppress4xx-errors) config_property_copy_button:+++quarkus.micrometer.binder.http-client.suppress4xx-errors+++[]<br><br>Suppress 4xx errors from metrics collection for unmatched templates. This configuration exists to limit cardinality explosion from caller side errors. Does not apply to 404 errors.<br><br>Suppressing 4xx errors is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_CLIENT_SUPPRESS4XX_ERRORS+++[] | boolean | `false` |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-client-max-uri-tags"></a> [`quarkus.micrometer.binder.http-client.max-uri-tags`](#quarkus-micrometer_quarkus-micrometer-binder-http-client-max-uri-tags) config_property_copy_button:+++quarkus.micrometer.binder.http-client.max-uri-tags+++[]<br><br>Maximum number of unique URI tag values allowed. After the max number of tag values is reached, metrics with additional tag values are denied by filter.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_CLIENT_MAX_URI_TAGS+++[] | int | `100` |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-server-match-patterns"></a> [`quarkus.micrometer.binder.http-server.match-patterns`](#quarkus-micrometer_quarkus-micrometer-binder-http-server-match-patterns) config_property_copy_button:+++quarkus.micrometer.binder.http-server.match-patterns+++[]<br><br>Comma-separated list of regular expressions used to specify uri labels in http metrics.<br><br>Vertx instrumentation will attempt to transform parameterized resource paths, `/item/123`, into a generic form, `/item/telemetry-micrometer`, to reduce the cardinality of uri label values.<br><br>Patterns specified here will take precedence over those computed values.<br><br>For example, if `/item/\\\\d+=/item/custom` or `/item/[0-9]+=/item/custom` is specified in this list, a request to a matching path (`/item/123`) will use the specified replacement value (`/item/custom`) as the value for the uri label. Note that backslashes must be double escaped as `\\\\`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_SERVER_MATCH_PATTERNS+++[] | list of string |  |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-server-ignore-patterns"></a> [`quarkus.micrometer.binder.http-server.ignore-patterns`](#quarkus-micrometer_quarkus-micrometer-binder-http-server-ignore-patterns) config_property_copy_button:+++quarkus.micrometer.binder.http-server.ignore-patterns+++[]<br><br>Comma-separated list of regular expressions defining uri paths that should be ignored (not measured).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_SERVER_IGNORE_PATTERNS+++[] | list of string |  |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-server-suppress-non-application-uris"></a> [`quarkus.micrometer.binder.http-server.suppress-non-application-uris`](#quarkus-micrometer_quarkus-micrometer-binder-http-server-suppress-non-application-uris) config_property_copy_button:+++quarkus.micrometer.binder.http-server.suppress-non-application-uris+++[]<br><br>Suppress non-application uris from metrics collection. This will suppress all metrics for non-application endpoints using `${quarkus.http.root-path}/${quarkus.http.non-application-root-path}`.<br><br>Suppressing non-application uris is enabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_SERVER_SUPPRESS_NON_APPLICATION_URIS+++[] | boolean | `true` |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-server-suppress4xx-errors"></a> [`quarkus.micrometer.binder.http-server.suppress4xx-errors`](#quarkus-micrometer_quarkus-micrometer-binder-http-server-suppress4xx-errors) config_property_copy_button:+++quarkus.micrometer.binder.http-server.suppress4xx-errors+++[]<br><br>Suppress 4xx errors from metrics collection for unmatched templates. This configuration exists to limit cardinality explosion from caller side error. Does not apply to 404 errors.<br><br>Suppressing 4xx errors is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_SERVER_SUPPRESS4XX_ERRORS+++[] | boolean | `false` |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-http-server-max-uri-tags"></a> [`quarkus.micrometer.binder.http-server.max-uri-tags`](#quarkus-micrometer_quarkus-micrometer-binder-http-server-max-uri-tags) config_property_copy_button:+++quarkus.micrometer.binder.http-server.max-uri-tags+++[]<br><br>Maximum number of unique URI tag values allowed. After the max number of tag values is reached, metrics with additional tag values are denied by filter.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_HTTP_SERVER_MAX_URI_TAGS+++[] | int | `100` |
| <a id="quarkus-micrometer_quarkus-micrometer-export-prometheus-configuration-property-name"></a> [`quarkus.micrometer.export.prometheus."configuration-property-name"`](#quarkus-micrometer_quarkus-micrometer-export-prometheus-configuration-property-name) config_property_copy_button:+++quarkus.micrometer.export.prometheus."configuration-property-name"+++[]<br><br>Prometheus registry configuration properties.<br><br>A property source for configuration of the Prometheus MeterRegistry, see <https://micrometer.io/docs/registry/prometheus>.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_EXPORT_PROMETHEUS__CONFIGURATION_PROPERTY_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-vertx-match-patterns"></a> <mark>link:#quarkus-micrometer_quarkus-micrometer-binder-vertx-match-patterns\[`quarkus.micrometer.binder.vertx.match-patterns`\]</mark> config_property_copy_button:+++quarkus.micrometer.binder.vertx.match-patterns+++[]<br><br>*This property is deprecated: use `quarkus.micrometer.binder.http-server.match-patterns`.*<br><br>Comma-separated list of regular expressions used to specify uri labels in http metrics.<br><br>Vertx instrumentation will attempt to transform parameterized resource paths, `/item/123`, into a generic form, `/item/telemetry-micrometer`, to reduce the cardinality of uri label values.<br><br>Patterns specified here will take precedence over those computed values.<br><br>For example, if `/item/\\\\d+=/item/custom` or `/item/[0-9]+=/item/custom` is specified in this list, a request to a matching path (`/item/123`) will use the specified replacement value (`/item/custom`) as the value for the uri label. Note that backslashes must be double escaped as `\\\\`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_VERTX_MATCH_PATTERNS+++[] | list of string |  |
| <a id="quarkus-micrometer_quarkus-micrometer-binder-vertx-ignore-patterns"></a> <mark>link:#quarkus-micrometer_quarkus-micrometer-binder-vertx-ignore-patterns\[`quarkus.micrometer.binder.vertx.ignore-patterns`\]</mark> config_property_copy_button:+++quarkus.micrometer.binder.vertx.ignore-patterns+++[]<br><br>*This property is deprecated: use `quarkus.micrometer.binder.http-server.ignore-patterns`.*<br><br>Comma-separated list of regular expressions defining uri paths that should be ignored (not measured).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MICROMETER_BINDER_VERTX_IGNORE_PATTERNS+++[] | list of string |  |

<a id="duration-note-anchor-quarkus-micrometer_quarkus-micrometer"></a>

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
