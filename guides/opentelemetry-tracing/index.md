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
- JDK 17+ installed with `JAVA_HOME` configured appropriately
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
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=opentelemetry-quickstart \
    -Dextensions='rest,quarkus-opentelemetry' \
    -DnoCode
cd opentelemetry-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=opentelemetry-quickstart"`

This command generates the Maven project and imports the `quarkus-opentelemetry` extension, which includes the default OpenTelemetry support, and a gRPC span exporter for [OTLP](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/otlp.md).

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

By default, the exporters will send out data in batches, using the gRPC protocol and endpoint `http://localhost:4317`.

If you need to change any of the default property values, here is an example on how to configure the default OTLP gRPC Exporter within the application, using the `src/main/resources/application.properties` file:

```properties
quarkus.application.name=myservice // <1>
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317 // <2>
quarkus.otel.exporter.otlp.headers=authorization=Bearer my_secret // <3>
quarkus.log.console.format=%d{HH:mm:ss} %-5p traceId=%X{traceId}, parentId=%X{parentId}, spanId=%X{spanId}, sampled=%X{sampled} [%c{2.}] (%t) %s%e%n  // <4>

# Alternative to the console log
quarkus.http.access-log.pattern="...traceId=%{X,traceId} spanId=%{X,spanId}" // <5>
```

1. All telemetry created from the application will include an OpenTelemetry `Resource` attribute indicating the telemetry was created by the `myservice` application. If not set, it will default to the artifact id.
2. gRPC endpoint to send the telemetry. If not set, it will default to `http://localhost:4317`.
3. Optional gRPC headers commonly used for authentication
4. Add tracing information into log messages.
5. You can also only put the trace info into the access log. In this case you must omit the info in the console log format.

We provide signal agnostic configurations for the connection related properties, meaning that you can use the same properties for both tracing and metrics when you set:

```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
```

If you need different configurations for each signal, you can use the specific properties:

```properties
quarkus.otel.exporter.otlp.traces.endpoint=http://trace-uri:4317 // <1>
quarkus.otel.exporter.otlp.metrics.endpoint=http://metrics-uri:4317 // <2>
quarkus.otel.exporter.otlp.logs.endpoint=http://logs-uri:4317 // <3>
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
| Other ports are available in the [Jaeger documentation](https://www.jaegertracing.io/docs/2.17/architecture/apis/#write-apis).<br><br>### Logging exporter<br><br>You can output all traces to the console by setting the exporter to `logging` in the `application.properties` file:<br><br>```properties<br>quarkus.otel.traces.exporter=logging<br>```<br><br>> [!NOTE]<br>> Usually there is no need to set the `quarkus.otel.traces.exporter` property. The default value is `cdi` and is managed by Quarkus.<br><br>This dependency must be added to the project:<br><br>```xml<br><dependency><br>    <groupId>io.opentelemetry</groupId><br>    <artifactId>opentelemetry-exporter-logging</artifactId><br></dependency><br>```<br><br>## Run the application<br><br>### Start the application<br><br>Now we are ready to run our application. If using `application.properties` to configure the tracer:<br><br>**CLI**<br><br>```bash<br>quarkus dev<br>```<br><br>**Maven**<br><br>```bash<br>./mvnw quarkus:dev<br>```<br><br>**Gradle**<br><br>```bash<br>./gradlew --console=plain quarkusDev<br>```<br><br>or if configuring the OTLP gRPC endpoint via JVM arguments:<br><br>**CLI**<br><br>```bash<br>quarkus dev -Djvm.args="-Dquarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4317"<br>```<br><br>**Maven**<br><br>```bash<br>./mvnw quarkus:dev -Djvm.args="-Dquarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4317"<br>```<br><br>**Gradle**<br><br>```bash<br>./gradlew --console=plain quarkusDev -Djvm.args="-Dquarkus.otel.exporter.otlp.traces.endpoint=http://localhost:4317"<br>```<br><br>With the OpenTelemetry Collector, the Jaeger system and the application running, you can make a request to the provided endpoint:<br><br>```shell<br>$ curl http://localhost:8080/hello<br>hello<br>```<br><br>When the first request has been submitted, you will be able to see the tracing information in the logs:<br><br>```<br>10:49:02 INFO  traceId=, parentId=, spanId=, sampled= [io.quarkus] (main) Installed features: [cdi, opentelemetry, resteasy-client, resteasy, smallrye-context-propagation, vertx]<br>10:49:03 INFO  traceId=17ceb8429b9f25b0b879fa1503259456, parentId=3125c8bee75b7ad6, spanId=58ce77c86dd23457, sampled=true [or.ac.op.TracedResource] (executor-thread-1) hello<br>10:49:03 INFO  traceId=ad23acd6d9a4ed3d1de07866a52fa2df, parentId=, spanId=df13f5b45cf4d1e2, sampled=true [or.ac.op.TracedResource] (executor-thread-0) hello<br>```<br><br>Then visit the [Jaeger UI](http://localhost:16686) to see the tracing information.<br><br>Hit `CTRL+C` or type `q` to stop the application.<br><br>### JDBC<br><br>The [JDBC instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/jdbc/library) bundled with this extension will add a span for each JDBC queries done by your application.<br><br>As it uses a dedicated JDBC datasource wrapper, you must enable telemetry for your datasource with the `quarkus.datasource.jdbc.telemetry` property, as in the following example:<br><br>```properties<br># enable tracing<br>quarkus.datasource.jdbc.telemetry=true<br><br># configure datasource<br>quarkus.datasource.db-kind=postgresql<br>quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydatabase<br>```<br><br>For more details check the [Datasource tracing](/guides/datasource/index.md#datasource-tracing) documentation.<br><br>## Additional configuration<br><br>Some use cases will require custom configuration of OpenTelemetry. These sections will outline what is necessary to properly configure it.<br><br>### ID Generator<br><br>The OpenTelemetry extension will use by default a random [ID Generator](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#id-generators) when creating the trace and span identifier.<br><br>Some vendor-specific protocols need a custom ID Generator, you can override the default one by creating a producer. The OpenTelemetry extension will detect the `IdGenerator` CDI bean and will use it when configuring the tracer producer.<br><br>```java<br>@Singleton<br>public class CustomConfiguration {<br><br>    /** Creates a custom IdGenerator for OpenTelemetry */<br>    @Produces<br>    @Singleton<br>    public IdGenerator idGenerator() {<br>        return AwsXrayIdGenerator.getInstance();<br>    }<br>}<br>```<br><br><a id="propagators-reference"></a><br>### Propagators<br><br>OpenTelemetry propagates cross-cutting concerns through [propagators](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/context/api-propagators.md) that will share an underlying `Context` for storing state and accessing data across the lifespan of a distributed transaction.<br><br>By default, the OpenTelemetry extension enables the [W3C Trace Context](https://www.w3.org/TR/trace-context/) and the [W3C Baggage](https://www.w3.org/TR/baggage/) propagators, you can however choose any of the supported OpenTelemetry propagators by setting the `propagators` config that is described in the [OpenTelemetry Configuration Reference](#configuration-reference).<br><br>#### Additional Propagators<br><br>- The `b3`, `b3multi`, `jaeger` and `ottrace` propagators will need the [trace-propagators](https://github.com/open-telemetry/opentelemetry-java/tree/main/extensions/trace-propagators) extension to be added as a dependency to your project.<br><br>**pom.xml**<br><br>```xml<br><dependency><br>    <groupId>io.opentelemetry</groupId><br>    <artifactId>opentelemetry-extension-trace-propagators</artifactId><br></dependency><br>```<br><br>**build.gradle**<br><br>```gradle<br>implementation("io.opentelemetry:opentelemetry-extension-trace-propagators")<br>```<br><br>- The `xray` propagator will need the [aws](https://github.com/open-telemetry/opentelemetry-java-contrib/tree/main/aws-xray-propagator) extension to be added as a dependency to your project.<br><br>**pom.xml**<br><br>```xml<br><dependency><br>    <groupId>io.opentelemetry.contrib</groupId><br>    <artifactId>opentelemetry-aws-xray-propagator</artifactId><br></dependency><br>```<br><br>**build.gradle**<br><br>```gradle<br>implementation("io.opentelemetry.contrib:opentelemetry-aws-xray-propagator")<br>```<br><br>#### Customise Propagator<br><br>To customise the propagation header you can implement the `TextMapPropagatorCustomizer` interface. This can be used, as an example, to restrict propagation of OpenTelemetry trace headers and prevent potentially sensitive data to be sent to third party systems.<br><br>``\`java / **\* /** \* Meant to be implemented by a CDI bean that provides arbitrary customization for the TextMapPropagator \* that are to be registered with OpenTelemetry \*/ public interface TextMapPropagatorCustomizer {<br><br>TextMapPropagator customize(Context context);<br><br>interface Context { TextMapPropagator propagator();<br><br>ConfigProperties otelConfigProperties(); } } ``\`<br><br>### Resource<br><br>See the main [OpenTelemetry Guide resources](/guides/opentelemetry/index.md#resource) section.<br><br>#### End User attributes<br><br>When enabled, Quarkus adds OpenTelemetry End User attributes as Span attributes. Before you enable this feature, verify that Quarkus Security extension is present and configured. More information about the Quarkus Security can be found in the [Quarkus Security overview](/guides/security-overview/index.md).<br><br>The attributes are only added when authentication has already happened on a best-efforts basis. Whether the End User attributes are added as Span attributes depends on authentication and authorization configuration of your Quarkus application. If you create custom Spans prior to the authentication, Quarkus cannot add the End User attributes to them. Quarkus is only able to add the attributes to the Span that is current after the authentication has been finished. Another important consideration regarding custom Spans is active CDI request context that is used to propagate Quarkus `SecurityIdentity`. In principle, Quarkus is able to add the End User attributes when the CDI request context has been activated for you before the custom Spans are created.<br><br>```application.properties<br>quarkus.otel.traces.eusp.enabled=true <1><br>quarkus.http.auth.proactive=true <2><br>```<br><br>1. Enable the End User Attributes feature so that the `SecurityIdentity` principal and roles are added as Span attributes. The End User attributes are personally identifiable information, therefore make sure you want to export them before you enable this feature.<br>2. Optionally enable proactive authentication. The best possible results are achieved when proactive authentication is enabled because the authentication happens sooner. A good way to determine whether proactive authentication should be enabled in your Quarkus application is to read the Quarkus [Proactive authentication](/guides/security-proactive-authentication/index.md) guide.<br><br>> [!IMPORTANT]<br>> This feature is not supported when a custom [Jakarta REST SecurityContexts](/guides/security-customization/index.md#jaxrs-security-context) is used.<br><br><a id="sampler"></a><br>### Sampler<br><br>A [sampler](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#sampling) decides whether a trace should be discarded or forwarded, effectively managing noise and reducing overhead by limiting the number of collected traces sent to the collector.<br><br>Quarkus comes equipped with a [built-in sampler](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#built-in-samplers), and you also have the option to create your custom sampler.<br><br>To use the built-in sampler, you can configure it by setting the desired sampler parameters as detailed in the [OpenTelemetry Configuration Reference](#configuration-reference). As an example, you can configure the sampler to retain 50% of the traces:<br><br>```application.properties<br># build time property only:<br>quarkus.otel.traces.sampler=traceidratio<br># Runtime property:<br>quarkus.otel.traces.sampler.arg=0.5<br>```<br><br>> [!TIP]<br>> An interesting use case for the sampler is to activate and deactivate tracing export at runtime, according to this example:<br>><br>> ```application.properties<br>> # build time property only:<br>> quarkus.otel.traces.sampler=traceidratio<br>> # On (default). All traces are exported:<br>> quarkus.otel.traces.sampler.arg=1.0<br>> # Off. No traces are exported:<br>> quarkus.otel.traces.sampler.arg=0.0<br>> ```<br><br>If you need to use a custom sampler there are now 2 different ways:<br><br>#### Sampler CDI Producer<br><br>You can create a sampler CDI producer. The Quarkus OpenTelemetry extension will detect the `Sampler` CDI bean and will use it when configuring the Tracer.<br><br>```java<br>@Singleton<br>public class CustomConfiguration {<br><br>    /** Creates a custom sampler for OpenTelemetry */<br>    @Produces<br>    @Singleton<br>    public Sampler sampler() {<br>        return JaegerRemoteSampler.builder()<br>        .setServiceName("my-service")<br>        .build();<br>    }<br>}<br>```<br><br>#### OTel Sampler SPI<br><br>This will use the SPI hooks available with the OTel Autoconfiguration. You can create a simple Sampler class:<br><br>```java<br>public class CustomSPISampler implements Sampler {<br>    @Override<br>    public SamplingResult shouldSample(Context context,<br>            String s,<br>            String s1,<br>            SpanKind spanKind,<br>            Attributes attributes,<br>            List<LinkData> list) {<br>        // Do some sampling here<br>        return Sampler.alwaysOn().shouldSample(context, s, s1, spanKind, attributes, list);<br>    }<br><br>    @Override<br>    public String getDescription() {<br>        return "custom-spi-sampler-description";<br>    }<br>}<br><br>```<br><br>Then a Sampler Provider:<br><br>```java<br>public class CustomSPISamplerProvider implements ConfigurableSamplerProvider {<br>    @Override<br>    public Sampler createSampler(ConfigProperties configProperties) {<br>        return new CustomSPISampler();<br>    }<br><br>    @Override<br>    public String getName() {<br>        return "custom-spi-sampler";<br>    }<br>}<br>```<br><br>Write the SPI loader text file at `resources/META-INF/services` with name `io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider` containing the full qualified name of the `CustomSPISamplerProvider` class.<br><br>Then activate on the configuration:<br><br>```properties<br>quarkus.otel.traces.sampler=custom-spi-sampler<br>```<br><br>As you can see, CDI is much simpler to work with.<br><br>## Additional instrumentation<br><br>Some Quarkus extensions will require additional code to ensure traces are propagated to subsequent execution. These sections will outline what is necessary to propagate traces across process boundaries.<br><br>The instrumentation documented in this section has been tested with Quarkus and works in both standard and native mode.<br><br>### CDI<br><br>Annotating a method in any CDI aware bean with the `io.opentelemetry.instrumentation.annotations.WithSpan` annotation will create a new Span and establish any required relationships with the current Trace context.<br><br>Annotating a method in any CDI aware bean with the `io.opentelemetry.instrumentation.annotations.AddingSpanAttributes` will not create a new span but will add annotated method parameters to attributes in the current span.<br><br>If a method is annotated by mistake with `@AddingSpanAttributes` and `@WithSpan` annotations, the `@WithSpan` annotation will take precedence.<br><br>Method parameters can be annotated with the `io.opentelemetry.instrumentation.annotations.SpanAttribute` annotation to indicate which method parameters should be part of the span. The parameter name can be customized as well.<br><br>Example:<br><br>```java<br>@ApplicationScoped<br>class SpanBean {<br>    @WithSpan<br>    void span() {<br><br>    }<br><br>    @WithSpan("name")<br>    void spanName() {<br><br>    }<br><br>    @WithSpan(kind = SERVER)<br>    void spanKind() {<br><br>    }<br><br>    @WithSpan<br>    void spanArgs(@SpanAttribute(value = "arg") String arg) {<br><br>    }<br><br>    @AddingSpanAttributes<br>    void addArgumentToExistingSpan(@SpanAttribute(value = "arg") String arg) {<br><br>    }<br>}<br>```<br><br><a id="cdi-injections-reference"></a><br>### Available OpenTelemetry CDI injections<br><br>As per MicroProfile Telemetry Tracing specification, Quarkus supports the CDI injections of the following classes:<br><br>- `io.opentelemetry.api.OpenTelemetry`<br>- `io.opentelemetry.api.trace.Tracer`<br>- `io.opentelemetry.api.trace.Span`<br>- `io.opentelemetry.api.baggage.Baggage`<br><br>You can inject these classes in any CDI enabled bean. For instance, the `Tracer` is particularly useful to start custom spans:<br><br>```java<br>@Inject<br>Tracer tracer;<br><br>...<br><br>public void tracedWork() {<br>    Span span = tracer.spanBuilder("My custom span")<br>        .setAttribute("attr", "attr.value")<br>        .setParent(Context.current().with(Span.current()))<br>        .setSpanKind(SpanKind.INTERNAL)<br>        .startSpan();<br><br>    // traced work<br><br>    span.end();<br>}<br>```<br><br>### Mutiny<br><br>Methods returning reactive types can also be annotated with `@WithSpan` and `@AddingSpanAttributes` to create a new span or add attributes to the current span.<br><br>If you need to create spans manually within a mutiny pipeline, use `wrapWithSpan` method from `io.quarkus.opentelemetry.runtime.tracing.mutiny.MutinyTracingHelper`.<br><br>Example. Assuming you have the following pipeline:<br><br>```java<br>Uni<String> uni = Uni.createFrom().item("hello")<br>        //start trace here<br>        .onItem().transform(item -> item + " world")<br>        .onItem().transform(item -> item + "!")<br>        //end trace here<br>        .subscribe().with(<br>                item -> System.out.println("Received: " + item),<br>                failure -> System.out.println("Failed with " + failure)<br>        );<br>```<br><br>wrap it like this:<br><br>```java<br>import static io.quarkus.opentelemetry.runtime.tracing.mutiny.MutinyTracingHelper.wrapWithSpan;<br>...<br>@Inject<br>Tracer tracer;<br>...<br>Context context = Context.current();<br>Uni<String> uni = Uni.createFrom().item("hello")<br>        .transformToUni(m -> wrapWithSpan(tracer, Optional.of(context), "my-span-name",<br>                                Uni.createFrom().item(m)<br>                                    .onItem().transform(item -> item + " world")<br>                                    .onItem().transform(item -> item + "!")<br>        ))<br>        .subscribe().with(<br>                item -> System.out.println("Received: " + item),<br>                failure -> System.out.println("Failed with " + failure)<br>        );<br><br>```<br><br>for multi-pipelines it works similarly:<br><br>```java<br>Multi.createFrom().items("Alice", "Bob", "Charlie")<br>        .transformToMultiAndConcatenate(m -> TracingHelper.withTrace("my-span-name",<br>                                Multi.createFrom().item(m)<br>                                    .onItem().transform(name -> "Hello " + name)<br>        ))<br>        .subscribe().with(<br>                item -> System.out.println("Received: " + item),<br>                failure -> System.out.println("Failed with " + failure)<br>        );<br>```<br><br>Instead of `transformToMultiAndConcatenate` you can use `transformToMultiAndMerge` if you don't care about the order of the items.<br><br>The `wrapWithSpan` utility method propagates the whole parent `Context` into the wrapped pipeline, not just the new span. Any `Baggage` present in the parent `Context` therefore remains available through `Baggage.current()` inside the pipeline. Passing `Optional.empty()` only detaches the trace parent to create a root span; a baggage already active in the surrounding context is still propagated.<br><br>### Quarkus Messaging - Kafka<br><br>When using the Quarkus Messaging extension for Kafka, we are able to propagate the span into the Kafka Record with:<br><br>```java<br>TracingMetadata tm = TracingMetadata.withPrevious(Context.current());<br>Message out = Message.of(...).withMetadata(tm);<br>```<br><br>The above creates a `TracingMetadata` object we can add to the `Message` being produced, which retrieves the OpenTelemetry `Context` to extract the current span for propagation.<br><br>### Quarkus Security Events<br><br>Quarkus supports exporting of the [Security events](/guides/security-customization/index.md#observe-security-events) as OpenTelemetry Span events.<br><br>```application.properties<br>quarkus.otel.security-events.enabled=true <1><br>```<br><br>1. Export Quarkus Security events as OpenTelemetry Span events.<br><br>## Manual instrumentation<br><br>In cases were utilities or automatic instrumentation is not available, manual instrumentation can be used to generate spans, propagate the OpenTelemetry context and the Baggage.<br><br>> [!WARNING]<br>> Use Manual instrumentation if there are no other alternatives because it will require more maintenance work.<br><br>### Manual Span<br><br>On the following example we have a class with a method implementing a manual span with a custom attribute. The only business logic we are measuring there is the `return "Hello from Quarkus REST";` statement.<br><br>```java<br>@ApplicationScoped<br>public static class HelloSpan {<br><br>    private final Tracer tracer;<br><br>    // Instead of using @Inject on the tracer attribute<br>    public HelloSpan(Tracer tracer) {<br>        // The same as openTelemetry.getTracer("io.quarkus.opentelemetry");<br>        this.tracer = tracer;<br>    }<br><br>    public String helloManualSpan() {<br>        // Create a new span<br>        Span span = tracer.spanBuilder("HelloBean.helloManualSpan").startSpan();<br>        // Make sure span scope is closed<br>        try (Scope scope = span.makeCurrent()) { <1><br>            // Add an attribute<br>            span.setAttribute("myAttributeName", "myValue");<br>            // Execute logic...<br>            return "Hello from Quarkus REST";<br>        } catch (Exception ignored) {<br>            // Store potential exceptions.<br>            span.recordException(ignored);<br>        } finally {<br>            // Whatever happens above, the span will be closed.<br>            span.end();<br>        }<br>        return "failover message";<br>    }<br>}<br>```<br><br>1. It is essential that all the logic the span is measuring be well delimited by a scope. The scope is Autocloseable.<br><br>### Manual Context Propagation<br><br>When you send messages or requests with clients that don't propagate the OpenTelemetry context you will have incomplete traces or broken traces, when the destination creates spans but because no context was propagated, the parent span is non-existent. This will cause more than one trace per request, which will cause problems.<br><br>Manual propagation can be implemented for those clients if there is a way to ship metadata containing the OpenTelemetry context with the message or request.<br><br>**This is an advanced subject**. For more details on context propagation please check the above section about [Propagators](#propagators-reference).<br><br>Next, we have an implementation example. The class contains a `MAP_SETTER`, a `MAP_GETTER` and 2 methods.<br><br>```java<br>@ApplicationScoped<br>public static class HelloPropagation {<br><br>    /**<br>     * How data is stored<br>     */<br>    private static final TextMapSetter<Map<String, String>> MAP_SETTER = new TextMapSetter<Map<String, String>>() {<br>        @Override<br>        public void set(@Nullable Map<String, String> carrier, String key, String value) {<br>            carrier.put(key, value); <1><br>        }<br>    };<br><br>    /**<br>     * How data is retrieved<br>     */<br>    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {<br><br>        @Override<br>        public Iterable<String> keys(Map<String, String> carrier) {<br>            return carrier == null ? emptyList() : carrier.keySet();<br>        }<br><br>        @Override<br>        public @Nullable String get(@Nullable Map<String, String> carrier, String key) {<br>            return carrier == null && key != null ? null : carrier.get(key); <2><br>        }<br>    };<br><br>    private final Tracer tracer;<br>    private final OpenTelemetry openTelemetry;<br><br>    // Instead of using @Inject on the tracer attribute<br>    public HelloPropagation(Tracer tracer, OpenTelemetry openTelemetry) {<br>        this.openTelemetry = openTelemetry;<br>        // The same as openTelemetry.getTracer("io.quarkus.opentelemetry");<br>        this.tracer = tracer;<br>    }<br><br>    public String createParentContext() { <3><br>        // Create the first manual span<br>        Span parentSpan = tracer.spanBuilder("parent-span").startSpan();<br><br>        try (Scope ignored = parentSpan.makeCurrent()) {<br>            // inject into a temporary map and return the traceparent header value<br>            // This can be stored in any data structure containing metadata in the payload to ship.<br>            // As an example, for HTTP the headers, for JMS a message attribute<br>            Map<String, String> tempCarrier = new HashMap<>();<br><br>            // We will use The MAP_SETTER to place the header containing the OTel Context in the tempCarrier<br>            openTelemetry.getPropagators()<br>                    .getTextMapPropagator()<br>                    .inject(Context.current(), tempCarrier, MAP_SETTER);<br><br>            // W3C traceparent header key is "traceparent"<br>            return tempCarrier.get("traceparent");<br>        } finally {<br>            parentSpan.end();<br>        }<br>    }<br><br>    public void receiveAndUseContext(String traceparent) { <4><br>        // Rebuild a tempCarrier map that contains the "traceparent" header data<br>        Map<String, String> tempCarrier = new HashMap<>();<br>        tempCarrier.put("traceparent", traceparent);<br><br>        // Extract context from the tempCarrier<br>        Context extracted = openTelemetry.getPropagators()<br>                .getTextMapPropagator()<br>                .extract(Context.current(), tempCarrier, MAP_GETTER);<br><br>        // Optionally check whether a parent span was found:<br>        boolean hasParent = Span.fromContext(extracted).getSpanContext().isValid();<br><br>        // Start a child span with the extracted context as explicit parent<br>        Span child = tracer.spanBuilder("child-span")<br>                .setParent(extracted)<br>                .startSpan();<br><br>        try (Scope scope = child.makeCurrent()) {<br>            // Simulate work under child span<br>            logger.infov("Child span started. Extracted parent valid? {0}", hasParent);<br>        } finally {<br>            child.end();<br>        }<br>    }<br>```<br><br>1. The `MAP_SETTER` handles how to store data in the carrier map that will transport the context.<br>2. `MAP_GETTER` handles how to retrieve data from the carrier map transporting the context.<br>3. The `createParentContext()` method explains how to create an OpenTelemetry context. In this case we generate the default [W3C Trace Context](https://www.w3.org/TR/trace-context/) header: `traceparent`.<br>4. the `receiveAndUseContext()` method receives the contents of the `traceparent` header used to bootstrap a new OpenTelemetry context in the destination.<br><br>### Manual Baggage propagation<br><br>The [Baggage](https://opentelemetry.io/docs/languages/java/api/#baggage) can be used to send application specific data using the OpenTelemetry's propagation mechanism.<br><br>In Quarkus, as documented in the [Available OpenTelemetry CDI injections](#cdi-injections-reference), Baggage can be injected to unwrap data coming with inbound requests but in more complex scenarios were data needs to be sent away in the Baggage manual instrumentation is required.<br><br>**This is an advanced subject**.<br><br>Using the `HelloPropagation` class from before we can add the following 2 methods, `createBaggage()` and `receiveAndUseBaggage()`.<br><br>```java<br><br>        public Map<String, String> createBaggage(String withValue) {<br>            // Baggage can be used to send Application specific data using the OpenTelemetry propagation mechanism<br>            Baggage baggage = Baggage.builder()<br>                    .put("baggage_key", withValue, BaggageEntryMetadata.empty())<br>                    .build();<br>            Context context = baggage.storeInContext(Context.current());<br><br>            // We use this to store the header data to ship out.<br>            Map<String, String> tempCarrier = new HashMap<>();<br><br>            // We will use The MAP_SETTER to place the header containing the OTel Context in the tempCarrier<br>            try (Scope ignored = context.makeCurrent()) {<br>                openTelemetry.getPropagators()<br>                        .getTextMapPropagator()<br>                        .inject(Context.current(), tempCarrier, MAP_SETTER);<br>            }<br>            return tempCarrier;<br>        }<br><br>        public String receiveAndUseBaggage(Map<String, String> headers) {<br>            // Extract context from the tempCarrier<br>            Context extracted = openTelemetry.getPropagators()<br>                    .getTextMapPropagator()<br>                    .extract(Context.current(), headers, MAP_GETTER);<br>            // Retrieve the baggage contents<br>            Baggage baggage = Baggage.fromContext(extracted);<br>            BaggageEntry baggageKey = baggage.asMap().get("baggage_key");<br>            return baggageKey != null ? baggageKey.getValue() : null;<br>        }<br>```<br><br>## Exporters<br><br>See the main [OpenTelemetry Guide exporters](/guides/opentelemetry/index.md#exporters) section.<br><br><a id="quarkus-extensions-using-opentelemetry"></a><br>## Quarkus core extensions instrumented with OpenTelemetry tracing<br><br>- [`quarkus-agroal`](https://quarkus.io/extensions/io.quarkus/quarkus-agroal)<br>- [`quarkus-grpc`](https://quarkus.io/guides/grpc-getting-started)<br>- [`quarkus-redis-client`](https://quarkus.io/guides/redis)<br>- [`quarkus-rest`](https://quarkus.io/guides/rest)<br>- [`quarkus-rest-client`](https://quarkus.io/guides/rest)<br>- [`quarkus-rest-client-jaxrs`](https://quarkus.io/extensions/io.quarkus/quarkus-rest-client-jaxrs)<br>- [`quarkus-resteasy`](https://quarkus.io/guides/resteasy)<br>- [`quarkus-resteasy-client`](https://quarkus.io/guides/resteasy-client)<br>- [`quarkus-scheduler`](https://quarkus.io/guides/scheduler)<br>- [`quarkus-smallrye-graphql`](https://quarkus.io/guides/smallrye-graphql)<br>- [`quarkus-mongodb-client`](https://quarkus.io/extensions/io.quarkus/quarkus-mongodb-client)<br>- [`quarkus-messaging`](https://quarkus.io/extensions/io.quarkus/quarkus-messaging)<br>- <br>  - AMQP 1.0<br>  - RabbitMQ<br>  - Kafka<br>  - Pulsar<br>- [`quarkus-vertx`](https://quarkus.io/guides/vertx) (http requests)<br>- [`websockets-next`](/guides/websockets-next-reference/index.md)<br><br>### Disable parts of the automatic tracing<br><br>Automatic tracing instrumentation parts can be disabled by setting `quarkus.otel.instrument.*` properties to `false`.<br><br>Examples:<br><br>```properties<br>quarkus.otel.instrument.grpc=false<br>quarkus.otel.instrument.messaging=false<br>quarkus.otel.instrument.resteasy-client=false<br>quarkus.otel.instrument.rest=false<br>quarkus.otel.instrument.resteasy=false<br>```<br><br><a id="disabling-traces-for-app-endpoints"></a><br>### Disabling Specific Traces for Application Endpoints<br><br>You can use the `quarkus.otel.traces.suppress-application-uris` property to exclude specific endpoints from being traced.<br><br>#### Example Configuration<br><br>```properties<br># application.properties<br>quarkus.otel.traces.suppress-application-uris=trace,ping,people*<br>```<br><br>This configuration will:<br><br>- Disable tracing for the `/trace` URI.<br>- Disable tracing for the `/ping` URI.<br>- Disable tracing for the `/people` URI and all subpaths, such as `/people/1` and `/people/1/cars`.<br><br>> [!NOTE]<br>> If you are using `quarkus.http.root-path`, ensure you include the root path in the configuration.<br><br><a id="configuration-reference"></a><br>## OpenTelemetry Configuration Reference<br><br>See the main [OpenTelemetry Guide configuration](/guides/opentelemetry/index.md#configuration-reference) reference. |
