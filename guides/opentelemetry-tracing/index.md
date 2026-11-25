# Using OpenTelemetry Tracing

This guide explains how your Quarkus application can utilize [OpenTelemetry](https://opentelemetry.io/) (OTel) to provide distributed tracing for interactive web applications.

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

> [!NOTE]
> - The [OpenTelemetry Guide](/guides/opentelemetry/index.md) is available with signal independent information about the OpenTelemetry extension.
> - If you search more information about OpenTelemetry Metrics, please refer to the [OpenTelemetry Metrics Guide](/guides/opentelemetry-metrics/index.md).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 21+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Docker and Docker Compose or [Podman](/guides/podman/index.md), and Docker Compose
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this guide, we create a straightforward REST application to demonstrate distributed tracing.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can skip right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `opentelemetry-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/opentelemetry-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:opentelemetry-quickstart \
    --extension='rest,quarkus-opentelemetry' \
    --no-code
cd opentelemetry-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:4.0.1:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=opentelemetry-quickstart \
    -Dextensions='rest,quarkus-opentelemetry' \
    -DnoCode
cd opentelemetry-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=opentelemetry-quickstart"`

This command generates the Maven project and imports the `quarkus-opentelemetry` extension, which includes the default OpenTelemetry support, and an OTLP span exporter for [OTLP](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/otlp.md).

If you already have your Quarkus project configured, you can add the `quarkus-opentelemetry` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add opentelemetry
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='opentelemetry'
```

**Gradle**

```bash
./gradlew addExtension --extensions='opentelemetry'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-opentelemetry</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-opentelemetry")
```

### Examine the Jakarta REST resource

Create a `src/main/java/org/acme/opentelemetry/TracedResource.java` file with the following content:

```java
package org.acme.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/hello")
public class TracedResource {

    private static final Logger LOG = Logger.getLogger(TracedResource.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("hello");
        return "hello";
    }
}
```

Notice that there is no tracing specific code included in the application. By default, requests sent to this endpoint will be traced without any required code changes.

### Create the configuration

By default, the exporters will send out data in batches, using the `http/protobuf` protocol and endpoint `http://localhost:4318`.

If you need to change any of the default property values, here is an example on how to configure the default OTLP Exporter within the application, using the `src/main/resources/application.properties` file:

```properties
quarkus.application.name=myservice // <1>
quarkus.otel.exporter.otlp.endpoint=http://localhost:4318 // <2>
quarkus.otel.exporter.otlp.headers=authorization=Bearer my_secret // <3>
quarkus.log.console.format=%d{HH:mm:ss} %-5p traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} [%c{2.}] (%t) %s%e%n  // <4>

# Alternative to the console log
quarkus.http.access-log.pattern="...traceId=%{X,traceId} spanId=%{X,spanId}" // <5>
```

1. All telemetry created from the application will include an OpenTelemetry `Resource` attribute indicating the telemetry was created by the `myservice` application. If not set, it will default to the artifact id.
2. OTLP endpoint to send the telemetry. If not set, it will default to `http://localhost:4318`.
3. Optional headers commonly used for authentication
4. Add tracing information into log messages.
5. You can also only put the trace info into the access log. In this case you must omit the info in the console log format.

We provide signal agnostic configurations for the connection related properties, meaning that you can use the same properties for both tracing and metrics when you set:

```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4318
```

If you need different configurations for each signal, you can use the specific properties:

```properties
quarkus.otel.exporter.otlp.traces.endpoint=http://trace-uri:4318 // <1>
quarkus.otel.exporter.otlp.metrics.endpoint=http://metrics-uri:4318 // <2>
quarkus.otel.exporter.otlp.logs.endpoint=http://logs-uri:4318 // <3>
```

1. The endpoint for the traces exporter.
2. The endpoint for the metrics exporter.
3. The endpoint for the logs exporter.

If you need your spans and logs to be exported directly as they finish (e.g. in a serverless environment / application), you can set this property to `true`. This replaces the default batching of data.

```properties
quarkus.otel.simple=true
```

## See data

Before starting the app, please set up the system to visualize the OpenTelemetry data. We have several options:

- Start an all-in-one Grafana OTel LGTM Dev Service for traces, logs and metrics
- Jaeger system just for traces
- Logging exporter

### Grafana OTel LGTM option

A Dev Service will receive your app's telemetry.

The Grafana-OTel-LGTM Dev Service will start automatically on Dev Mode and data will be automatically sent to it.

- Take a look at: [Getting Started with Grafana-OTel-LGTM](/guides/observability-devservices-lgtm/index.md).

This features a Quarkus Dev Service including a Grafana for visualizing data, Loki to store logs, Tempo to store traces and Prometheus to store metrics. Also provides an OTel collector to receive the data.

### Jaeger v2 to see traces option

Jaeger V2 is a tool to visualize spans, and it's based on OpenTelemetry collector. There is no need to install a separate collector. More details are available in [this blog post](https://medium.com/jaegertracing/towards-jaeger-v2-moar-opentelemetry-2f8239bee48e).

Start the OpenTelemetry Collector and Jaeger system via the following Docker command:

```shell
docker run -it \
  -p 16686:16686 \
  -p 4317:4317 \
  -p 4318:4318 \
jaegertracing/jaeger:latest
```

Where: |=== |Port | Purpose

|16686 |Jaeger UI

|4317 |OpenTelemetry collector. OTLP protobuf gRPC receiver.

|4318 |OpenTelemetry collector. OTLP protobuf HTTP receiver.

|  |
| --- |
| Other ports are available in the [Jaeger documentation](https://www.jaegertracing.io/docs/2.17/architecture/apis/#write-apis).<br><br>### Logging exporter<br><br>You can output all traces to the console by setting the exporter to `logging` in the `application.properties` file:<br><br>```properties<br>quarkus.otel.traces.exporter=logging<br>```<br><br>> [!NOTE]<br>> Usually there is no need to set the `quarkus.otel.traces.exporter` property. The default value is `cdi` and is managed by Quarkus.<br><br>This dependency must be added to the project:<br><br>```xml<br><dependency><br>    <groupId>io.opentelemetry</groupId><br>    <artifactId>opentelemetry-exporter-logging</artifactId><br></dependency><br>```<br><br>### Traces in the Dev UI<br><br>In dev mode, when the OpenTelemetry extension is present, an **Observability** dashboard appears in the Dev UI (`/q/dev-ui`) menu, offering a **Traces** card. It captures finished spans into a small in-memory ring buffer and renders them grouped by trace as a live waterfall, with no containers or Dev Services required. Add the card the first time you open the dashboard, or later with **Edit dashboard**, and drag it by its header to position it among the other cards. A waterfall benefits from room: the expand icon in the card's header gives it the whole dashboard area until you press **Escape**, and **Open** links through to the full-page view.<br><br>The capture is dev-mode only and does not affect production or native builds. |

|Property |Default |Description |`quarkus.dev-ui.observability.traces.enabled` |`true` |Enable/disable the Dev UI traces capture (dev mode only). |`quarkus.dev-ui.observability.traces.capacity` |`1000` |Maximum number of spans kept in the ring buffer; oldest are evicted first. |===

> [!NOTE]
> Dev mode defaults ratio-based samplers to 100%, so all spans are captured. Only sampled spans are recorded; a custom sampler that drops spans will limit what appears here. When the buffer is full, the oldest spans are evicted, which can leave the oldest trace partially shown.

## Run the application

### Start the application

Now we are ready to run our application. If using `application.properties` to configure the tracer:

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

or if configuring the OTLP endpoint via JVM arguments:

**CLI**

```bash
quarkus dev -Djvm.args="-Dquarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4318"
```

**Maven**

```bash
./mvnw quarkus:dev -Djvm.args="-Dquarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4318"
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev -Djvm.args="-Dquarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4318"
```

With the OpenTelemetry Collector, the Jaeger system and the application running, you can make a request to the provided endpoint:

```shell
$ curl http://localhost:8080/hello
hello
```

When the first request has been submitted, you will be able to see the tracing information in the logs:

```
10:49:02 INFO  traceId=, parentId=, spanId=, sampled= [io.quarkus] (main) Installed features: [cdi, opentelemetry, resteasy-client, resteasy, smallrye-context-propagation, vertx]
10:49:03 INFO  traceId=17ceb8429b9f25b0b879fa1503259456, parentId=3125c8bee75b7ad6, spanId=58ce77c86dd23457, sampled=true [or.ac.op.TracedResource] (executor-thread-1) hello
10:49:03 INFO  traceId=ad23acd6d9a4ed3d1de07866a52fa2df, parentId=, spanId=df13f5b45cf4d1e2, sampled=true [or.ac.op.TracedResource] (executor-thread-0) hello
```

Then visit the [Jaeger UI](http://localhost:16686) to see the tracing information.

Hit `CTRL+C` or type `q` to stop the application.

### JDBC

The [JDBC instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jdbc/library) bundled with this extension will add a span for each JDBC queries done by your application.

As it uses a dedicated JDBC datasource wrapper, you must enable telemetry for your datasource with the `quarkus.datasource.jdbc.telemetry` property, as in the following example:

```properties
# enable tracing
quarkus.datasource.jdbc.telemetry=true

# configure datasource
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydatabase
```

For more details check the [Datasource tracing](/guides/datasource/index.md#datasource-tracing) documentation.

## Additional configuration

Some use cases will require custom configuration of OpenTelemetry. These sections will outline what is necessary to properly configure it.

### ID Generator

The OpenTelemetry extension will use by default a random [ID Generator](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#id-generators) when creating the trace and span identifier.

Some vendor-specific protocols need a custom ID Generator, you can override the default one by creating a producer. The OpenTelemetry extension will detect the `IdGenerator` CDI bean and will use it when configuring the tracer producer.

```java
@Singleton
public class CustomConfiguration {

    /** Creates a custom IdGenerator for OpenTelemetry */
    @Produces
    @Singleton
    public IdGenerator idGenerator() {
        return AwsXrayIdGenerator.getInstance();
    }
}
```

<a id="propagators-reference"></a>
### Propagators

OpenTelemetry propagates cross-cutting concerns through [propagators](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/context/api-propagators.md) that will share an underlying `Context` for storing state and accessing data across the lifespan of a distributed transaction.

By default, the OpenTelemetry extension enables the [W3C Trace Context](https://www.w3.org/TR/trace-context/) and the [W3C Baggage](https://www.w3.org/TR/baggage/) propagators, you can however choose any of the supported OpenTelemetry propagators by setting the `propagators` config that is described in the [OpenTelemetry Configuration Reference](#configuration-reference).

#### Additional Propagators

- The `b3`, `b3multi`, `jaeger` and `ottrace` propagators will need the [trace-propagators](https://github.com/open-telemetry/opentelemetry-java/tree/main/extensions/trace-propagators) extension to be added as a dependency to your project.

**pom.xml**

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-extension-trace-propagators</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")
```

- The `xray` propagator will need the [aws](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/aws-xray-propagator) extension to be added as a dependency to your project.

**pom.xml**

```xml
<dependency>
    <groupId>io.opentelemetry.contrib</groupId>
    <artifactId>opentelemetry-aws-xray-propagator</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")
```

#### Customise Propagator

To customise the propagation header you can implement the `TextMapPropagatorCustomizer` interface. This can be used, as an example, to restrict propagation of OpenTelemetry trace headers and prevent potentially sensitive data to be sent to third party systems.

``\`java / **\* /** \* Meant to be implemented by a CDI bean that provides arbitrary customization for the TextMapPropagator \* that are to be registered with OpenTelemetry \*/ public interface TextMapPropagatorCustomizer {

TextMapPropagator customize(Context context);

interface Context { TextMapPropagator propagator();

ConfigProperties otelConfigProperties(); } } ``\`

### Resource

See the main [OpenTelemetry Guide resources](/guides/opentelemetry/index.md#resource) section.

#### End User attributes

When enabled, Quarkus adds OpenTelemetry End User attributes as Span attributes. Before you enable this feature, verify that Quarkus Security extension is present and configured. More information about the Quarkus Security can be found in the [Quarkus Security overview](/guides/security-overview/index.md).

The attributes are only added when authentication has already happened on a best-efforts basis. Whether the End User attributes are added as Span attributes depends on authentication and authorization configuration of your Quarkus application. If you create custom Spans prior to the authentication, Quarkus cannot add the End User attributes to them. Quarkus is only able to add the attributes to the Span that is current after the authentication has been finished. Another important consideration regarding custom Spans is active CDI request context that is used to propagate Quarkus `SecurityIdentity`. In principle, Quarkus is able to add the End User attributes when the CDI request context has been activated for you before the custom Spans are created.

```application.properties
quarkus.otel.traces.eusp.enabled=true <1>
quarkus.http.auth.proactive=true <2>
```

1. Enable the End User Attributes feature so that the `SecurityIdentity` principal and roles are added as Span attributes. The End User attributes are personally identifiable information, therefore make sure you want to export them before you enable this feature.
2. Optionally enable proactive authentication. The best possible results are achieved when proactive authentication is enabled because the authentication happens sooner. A good way to determine whether proactive authentication should be enabled in your Quarkus application is to read the Quarkus [Proactive authentication](/guides/security-proactive-authentication/index.md) guide.

> [!IMPORTANT]
> This feature is not supported when a custom [Jakarta REST SecurityContexts](/guides/security-customization/index.md#jaxrs-security-context) is used.

<a id="sampler"></a>
### Sampler

A [sampler](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#sampling) decides whether a trace should be discarded or forwarded, effectively managing noise and reducing overhead by limiting the number of collected traces sent to the collector.

Quarkus comes equipped with a [built-in sampler](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#built-in-samplers), and you also have the option to create your custom sampler.

To use the built-in sampler, you can configure it by setting the desired sampler parameters as detailed in the [OpenTelemetry Configuration Reference](#configuration-reference). As an example, you can configure the sampler to retain 50% of the traces:

```application.properties
# build time property only:
quarkus.otel.traces.sampler=traceidratio
# Runtime property:
quarkus.otel.traces.sampler.arg=0.5
```

> [!NOTE]
> As of Quarkus 4, the default sampling ratio changed from 100% (1.0d) to 10% (0.1d) to reduce overhead in production environments. In development and test modes, sampling defaults to 100% (1.0d) to ensure all traces are captured for debugging and for deterministic test assertions. This applies only when a ratio-based sampler (`parentbased_traceidratio` or `traceidratio`) is used; an explicit `quarkus.otel.traces.sampler.arg` still takes precedence. Note that `@QuarkusIntegrationTest` (native or JAR) applications are built in production mode, so they use the production default unless sampling is configured explicitly.

> [!TIP]
> An interesting use case for the sampler is to activate and deactivate tracing export at runtime, according to this example:
>
> ```application.properties
> # build time property only:
> quarkus.otel.traces.sampler=traceidratio
> # On. All traces are exported:
> quarkus.otel.traces.sampler.arg=1.0
> # Off. No traces are exported:
> quarkus.otel.traces.sampler.arg=0.0
> ```

If you need to use a custom sampler there are now 2 different ways:

#### Sampler CDI Producer

You can create a sampler CDI producer. The Quarkus OpenTelemetry extension will detect the `Sampler` CDI bean and will use it when configuring the Tracer.

```java
@Singleton
public class CustomConfiguration {

    /** Creates a custom sampler for OpenTelemetry */
    @Produces
    @Singleton
    public Sampler sampler() {
        return JaegerRemoteSampler.builder()
        .setServiceName("my-service")
        .build();
    }
}
```

#### OTel Sampler SPI

This will use the SPI hooks available with the OTel Autoconfiguration. You can create a simple Sampler class:

```java
public class CustomSPISampler implements Sampler {
    @Override
    public SamplingResult shouldSample(Context context,
            String s,
            String s1,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> list) {
        // Do some sampling here
        return Sampler.alwaysOn().shouldSample(context, s, s1, spanKind, attributes, list);
    }

    @Override
    public String getDescription() {
        return "custom-spi-sampler-description";
    }
}

```

Then a Sampler Provider:

```java
public class CustomSPISamplerProvider implements ConfigurableSamplerProvider {
    @Override
    public Sampler createSampler(ConfigProperties configProperties) {
        return new CustomSPISampler();
    }

    @Override
    public String getName() {
        return "custom-spi-sampler";
    }
}
```

Write the SPI loader text file at `resources/META-INF/services` with name `io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider` containing the full qualified name of the `CustomSPISamplerProvider` class.

Then activate on the configuration:

```properties
quarkus.otel.traces.sampler=custom-spi-sampler
```

As you can see, CDI is much simpler to work with.

## Additional instrumentation

Some Quarkus extensions will require additional code to ensure traces are propagated to subsequent execution. These sections will outline what is necessary to propagate traces across process boundaries.

The instrumentation documented in this section has been tested with Quarkus and works in both standard and native mode.

### CDI

Annotating a method in any CDI aware bean with the `io.opentelemetry.instrumentation.annotations.WithSpan` annotation will create a new Span and establish any required relationships with the current Trace context.

Annotating a method in any CDI aware bean with the `io.opentelemetry.instrumentation.annotations.AddingSpanAttributes` will not create a new span but will add annotated method parameters to attributes in the current span.

If a method is annotated by mistake with `@AddingSpanAttributes` and `@WithSpan` annotations, the `@WithSpan` annotation will take precedence.

Method parameters can be annotated with the `io.opentelemetry.instrumentation.annotations.SpanAttribute` annotation to indicate which method parameters should be part of the span. The parameter name can be customized as well.

Example:

```java
@ApplicationScoped
class SpanBean {
    @WithSpan
    void span() {

    }

    @WithSpan("name")
    void spanName() {

    }

    @WithSpan(kind = SERVER)
    void spanKind() {

    }

    @WithSpan
    void spanArgs(@SpanAttribute(value = "arg") String arg) {

    }

    @AddingSpanAttributes
    void addArgumentToExistingSpan(@SpanAttribute(value = "arg") String arg) {

    }
}
```

<a id="cdi-injections-reference"></a>
### Available OpenTelemetry CDI injections

As per MicroProfile Telemetry Tracing specification, Quarkus supports the CDI injections of the following classes:

- `io.opentelemetry.api.OpenTelemetry`
- `io.opentelemetry.api.trace.Tracer`
- `io.opentelemetry.api.trace.Span`
- `io.opentelemetry.api.baggage.Baggage`

You can inject these classes in any CDI enabled bean. For instance, the `Tracer` is particularly useful to start custom spans:

```java
@Inject
Tracer tracer;

...

public void tracedWork() {
    Span span = tracer.spanBuilder("My custom span")
        .setAttribute("attr", "attr.value")
        .setParent(Context.current().with(Span.current()))
        .setSpanKind(SpanKind.INTERNAL)
        .startSpan();

    // traced work

    span.end();
}
```

### Mutiny

Methods returning reactive types can also be annotated with `@WithSpan` and `@AddingSpanAttributes` to create a new span or add attributes to the current span.

If you need to create spans manually within a mutiny pipeline, use `wrapWithSpan` method from `io.quarkus.opentelemetry.runtime.tracing.mutiny.MutinyTracingHelper`.

Example. Assuming you have the following pipeline:

```java
Uni<String> uni = Uni.createFrom().item("hello")
        //start trace here
        .onItem().transform(item -> item + " world")
        .onItem().transform(item -> item + "!")
        //end trace here
        .subscribe().with(
                item -> System.out.println("Received: " + item),
                failure -> System.out.println("Failed with " + failure)
        );
```

wrap it like this:

```java
import static io.quarkus.opentelemetry.runtime.tracing.mutiny.MutinyTracingHelper.wrapWithSpan;
...
@Inject
Tracer tracer;
...
Context context = Context.current();
Uni<String> uni = Uni.createFrom().item("hello")
        .transformToUni(m -> wrapWithSpan(tracer, Optional.of(context), "my-span-name",
                                Uni.createFrom().item(m)
                                    .onItem().transform(item -> item + " world")
                                    .onItem().transform(item -> item + "!")
        ))
        .subscribe().with(
                item -> System.out.println("Received: " + item),
                failure -> System.out.println("Failed with " + failure)
        );

```

for multi-pipelines it works similarly:

```java
Multi.createFrom().items("Alice", "Bob", "Charlie")
        .transformToMultiAndConcatenate(m -> TracingHelper.withTrace("my-span-name",
                                Multi.createFrom().item(m)
                                    .onItem().transform(name -> "Hello " + name)
        ))
        .subscribe().with(
                item -> System.out.println("Received: " + item),
                failure -> System.out.println("Failed with " + failure)
        );
```

Instead of `transformToMultiAndConcatenate` you can use `transformToMultiAndMerge` if you don't care about the order of the items.

The `wrapWithSpan` utility method propagates the whole parent `Context` into the wrapped pipeline, not just the new span. Any `Baggage` present in the parent `Context` therefore remains available through `Baggage.current()` inside the pipeline. Passing `Optional.empty()` only detaches the trace parent to create a root span; a baggage already active in the surrounding context is still propagated.

### Quarkus Messaging - Kafka

When using the Quarkus Messaging extension for Kafka, we are able to propagate the span into the Kafka Record with:

```java
TracingMetadata tm = TracingMetadata.withPrevious(Context.current());
Message out = Message.of(...).withMetadata(tm);
```

The above creates a `TracingMetadata` object we can add to the `Message` being produced, which retrieves the OpenTelemetry `Context` to extract the current span for propagation.

### Quarkus Security Events

Quarkus supports exporting of the [Security events](/guides/security-customization/index.md#observe-security-events) as OpenTelemetry Span events.

```application.properties
quarkus.otel.security-events.enabled=true <1>
```

1. Export Quarkus Security events as OpenTelemetry Span events.

## Manual instrumentation

In cases were utilities or automatic instrumentation is not available, manual instrumentation can be used to generate spans, propagate the OpenTelemetry context and the Baggage.

> [!WARNING]
> Use Manual instrumentation if there are no other alternatives because it will require more maintenance work.

### Manual Span

On the following example we have a class with a method implementing a manual span with a custom attribute. The only business logic we are measuring there is the `return "Hello from Quarkus REST";` statement.

```java
@ApplicationScoped
public static class HelloSpan {

    private final Tracer tracer;

    // Instead of using @Inject on the tracer attribute
    public HelloSpan(Tracer tracer) {
        // The same as openTelemetry.getTracer("io.quarkus.opentelemetry");
        this.tracer = tracer;
    }

    public String helloManualSpan() {
        // Create a new span
        Span span = tracer.spanBuilder("HelloBean.helloManualSpan").startSpan();
        // Make sure span scope is closed
        try (Scope scope = span.makeCurrent()) { <1>
            // Add an attribute
            span.setAttribute("myAttributeName", "myValue");
            // Execute logic...
            return "Hello from Quarkus REST";
        } catch (Exception ignored) {
            // Store potential exceptions.
            span.recordException(ignored);
        } finally {
            // Whatever happens above, the span will be closed.
            span.end();
        }
        return "failover message";
    }
}
```

1. It is essential that all the logic the span is measuring be well delimited by a scope. The scope is Autocloseable.

### Manual Context Propagation

When you send messages or requests with clients that don't propagate the OpenTelemetry context you will have incomplete traces or broken traces, when the destination creates spans but because no context was propagated, the parent span is non-existent. This will cause more than one trace per request, which will cause problems.

Manual propagation can be implemented for those clients if there is a way to ship metadata containing the OpenTelemetry context with the message or request.

**This is an advanced subject**. For more details on context propagation please check the above section about [Propagators](#propagators-reference).

Next, we have an implementation example. The class contains a `MAP_SETTER`, a `MAP_GETTER` and 2 methods.

```java
@ApplicationScoped
public static class HelloPropagation {

    /**
     * How data is stored
     */
    private static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<Map<String, String>>() {
        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            carrier.put(key, value); <1>
        }
    };

    /**
     * How data is retrieved
     */
    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {

        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier == null ? emptyList() : carrier.keySet();
        }

        @Override
        public @Nullable String get(@Nullable Map<String, String> carrier, String key) {
            return carrier == null && key != null ? null : carrier.get(key); <2>
        }
    };

    private final Tracer tracer;
    private final OpenTelemetry openTelemetry;

    // Instead of using @Inject on the tracer attribute
    public HelloPropagation(Tracer tracer, OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        // The same as openTelemetry.getTracer("io.quarkus.opentelemetry");
        this.tracer = tracer;
    }

    public String createParentContext() { <3>
        // Create the first manual span
        Span parentSpan = tracer.spanBuilder("parent-span").startSpan();

        try (Scope ignored = parentSpan.makeCurrent()) {
            // inject into a temporary map and return the traceparent header value
            // This can be stored in any data structure containing metadata in the payload to ship.
            // As an example, for HTTP the headers, for JMS a message attribute
            Map<String, String> tempCarrier = new HashMap<>();

            // We will use The MAP_SETTER to place the header containing the OTel Context in the tempCarrier
            openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), tempCarrier, MAP_SETTER);

            // W3C traceparent header key is "traceparent"
            return tempCarrier.get("traceparent");
        } finally {
            parentSpan.end();
        }
    }

    public void receiveAndUseContext(String traceparent) { <4>
        // Rebuild a tempCarrier map that contains the "traceparent" header data
        Map<String, String> tempCarrier = new HashMap<>();
        tempCarrier.put("traceparent", traceparent);

        // Extract context from the tempCarrier
        Context extracted = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), tempCarrier, MAP_GETTER);

        // Optionally check whether a parent span was found:
        boolean hasParent = Span.fromContext(extracted).getSpanContext().isValid();

        // Start a child span with the extracted context as explicit parent
        Span child = tracer.spanBuilder("child-span")
                .setParent(extracted)
                .startSpan();

        try (Scope scope = child.makeCurrent()) {
            // Simulate work under child span
            logger.infov("Child span started. Extracted parent valid? {0}", hasParent);
        } finally {
            child.end();
        }
    }
```

1. The `MAP_SETTER` handles how to store data in the carrier map that will transport the context.
2. `MAP_GETTER` handles how to retrieve data from the carrier map transporting the context.
3. The `createParentContext()` method explains how to create an OpenTelemetry context. In this case we generate the default [W3C Trace Context](https://www.w3.org/TR/trace-context/) header: `traceparent`.
4. the `receiveAndUseContext()` method receives the contents of the `traceparent` header used to bootstrap a new OpenTelemetry context in the destination.

### Manual Baggage propagation

The [Baggage](https://opentelemetry.io/docs/languages/java/api/#baggage) can be used to send application specific data using the OpenTelemetry's propagation mechanism.

In Quarkus, as documented in the [Available OpenTelemetry CDI injections](#cdi-injections-reference), Baggage can be injected to unwrap data coming with inbound requests but in more complex scenarios were data needs to be sent away in the Baggage manual instrumentation is required.

**This is an advanced subject**.

Using the `HelloPropagation` class from before we can add the following 2 methods, `createBaggage()` and `receiveAndUseBaggage()`.

```java

        public Map<String, String> createBaggage(String withValue) {
            // Baggage can be used to send Application specific data using the OpenTelemetry propagation mechanism
            Baggage baggage = Baggage.builder()
                    .put("baggage_key", withValue, BaggageEntryMetadata.empty())
                    .build();
            Context context = baggage.storeInContext(Context.current());

            // We use this to store the header data to ship out.
            Map<String, String> tempCarrier = new HashMap<>();

            // We will use The MAP_SETTER to place the header containing the OTel Context in the tempCarrier
            try (Scope ignored = context.makeCurrent()) {
                openTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .inject(Context.current(), tempCarrier, MAP_SETTER);
            }
            return tempCarrier;
        }

        public String receiveAndUseBaggage(Map<String, String> headers) {
            // Extract context from the tempCarrier
            Context extracted = openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .extract(Context.current(), headers, MAP_GETTER);
            // Retrieve the baggage contents
            Baggage baggage = Baggage.fromContext(extracted);
            BaggageEntry baggageKey = baggage.asMap().get("baggage_key");
            return baggageKey != null ? baggageKey.getValue() : null;
        }
```

## Exporters

See the main [OpenTelemetry Guide exporters](/guides/opentelemetry/index.md#exporters) section.

<a id="quarkus-extensions-using-opentelemetry"></a>
## Quarkus core extensions instrumented with OpenTelemetry tracing

- [`quarkus-agroal`](https://quarkus.io/extensions/io.quarkus/quarkus-agroal)
- [`quarkus-grpc`](https://quarkus.io/guides/grpc-getting-started)
- [`quarkus-redis-client`](https://quarkus.io/guides/redis)
- [`quarkus-rest`](https://quarkus.io/guides/rest)
- [`quarkus-rest-client`](https://quarkus.io/guides/rest)
- [`quarkus-rest-client-jaxrs`](https://quarkus.io/extensions/io.quarkus/quarkus-rest-client-jaxrs)
- [`quarkus-resteasy`](https://quarkus.io/guides/resteasy)
- [`quarkus-resteasy-client`](https://quarkus.io/guides/resteasy-client)
- [`quarkus-scheduler`](https://quarkus.io/guides/scheduler)
- [`quarkus-signals`](https://quarkus.io/guides/signals)
- [`quarkus-smallrye-graphql`](https://quarkus.io/guides/smallrye-graphql)
- [`quarkus-mongodb-client`](https://quarkus.io/extensions/io.quarkus/quarkus-mongodb-client)
- [`quarkus-messaging`](https://quarkus.io/extensions/io.quarkus/quarkus-messaging)
- 
  - AMQP 1.0
  - RabbitMQ
  - Kafka
  - Pulsar
- [`quarkus-vertx`](https://quarkus.io/guides/vertx) (http requests)
- [`websockets-next`](/guides/websockets-next-reference/index.md)

### Disable parts of the automatic tracing

Automatic tracing instrumentation parts can be disabled by setting `quarkus.otel.instrument.*` properties to `false`.

Examples:

```properties
quarkus.otel.instrument.grpc=false
quarkus.otel.instrument.messaging=false
quarkus.otel.instrument.resteasy-client=false
quarkus.otel.instrument.rest=false
quarkus.otel.instrument.resteasy=false
```

<a id="disabling-traces-for-app-endpoints"></a>
### Disabling Specific Traces for Application Endpoints

You can use the `quarkus.otel.traces.suppress-application-uris` property to exclude specific endpoints from being traced.

#### Example Configuration

```properties
# application.properties
quarkus.otel.traces.suppress-application-uris=trace,ping,people*
```

This configuration will:

- Disable tracing for the `/trace` URI.
- Disable tracing for the `/ping` URI.
- Disable tracing for the `/people` URI and all subpaths, such as `/people/1` and `/people/1/cars`.

> [!NOTE]
> If you are using `quarkus.http.root-path`, ensure you include the root path in the configuration.

<a id="configuration-reference"></a>
## OpenTelemetry Configuration Reference

See the main [OpenTelemetry Guide configuration](/guides/opentelemetry/index.md#configuration-reference) reference.
