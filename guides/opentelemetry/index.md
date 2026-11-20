# Using OpenTelemetry

This guide explains how your Quarkus application can utilize [OpenTelemetry](https://opentelemetry.io/) (OTel) to provide Observability for interactive web applications.

On this page we show the signal independent features of the extension.

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

> [!NOTE]
> - The old OpenTelemetry guide has been split into this generic guide, the [OpenTelemetry Tracing Guide](/guides/opentelemetry-tracing/index.md), the new [OpenTelemetry Metrics Guide](/guides/opentelemetry-metrics/index.md) and the [OpenTelemetry Logging Guide](/guides/opentelemetry-logging/index.md).
> - The use of **the [OpenTelemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/) is not needed nor recommended**. Quarkus Extensions and the libraries they provide, are directly instrumented. That agent doesn't work with native mode.

## Introduction

[OpenTelemetry](https://opentelemetry.io/docs/what-is-opentelemetry/) is an Observability framework and toolkit designed to create and manage telemetry data such as traces, metrics, and logs. Crucially, OpenTelemetry is vendor- and tool-agnostic.

Quarkus provides manual and automatic instrumentation for tracing and manual instrumentation capabilities for metrics.

This will allow Quarkus based applications to be observable by tools and services supporting OpenTelemetry.

> [!NOTE]
> Automatic metrics instrumentation in Quarkus is done by the [Quarkus Micrometer extension](/guides/telemetry-micrometer/index.md).
>
> The [quarkus-micrometer-opentelemetry](/guides/telemetry-micrometer-to-opentelemetry/index.md) extension enables the use and export of Micrometer metrics via OpenTelemetry.

Quarkus supports the OpenTelemetry Autoconfiguration. The configurations match what you can see at [OpenTelemetry SDK Autoconfigure](https://opentelemetry.io/docs/languages/java/configuration/) with the `quarkus.*` prefix.

This guide provides a crosscutting explanation of the OpenTelemetry extension and how to use it. If you need details about any particular signal (tracing or metrics), please refer to the signal specific guide.

With the introduction of OpenTelemetry Metrics, the original, single page guide had to be split according to signal types, as follows:

### [OpenTelemetry Tracing Guide](/guides/opentelemetry-tracing/index.md)

The tracing functionality is supported and **on** by default.

### [OpenTelemetry Metrics Guide](/guides/opentelemetry-metrics/index.md)

#### Enable Metrics

The metrics functionality is tech preview and **off** by default. You will need to activate it by setting:

```properties
quarkus.otel.metrics.enabled=true
```

At build time on your `application.properties` file.

### [OpenTelemetry Logging Guide](/guides/opentelemetry-logging/index.md)

#### Enable Logs

The logging functionality is tech preview and **off** by default. You will need to activate it by setting:

```properties
quarkus.otel.logs.enabled=true
```

At build time on your `application.properties` file.

## Using the extension

If you already have your Quarkus project, you can add the `quarkus-opentelemetry` extension to it by running the following command in your project base directory:

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

### Disable all or parts of the OpenTelemetry extension

Once you add the dependency, the extension will generate tracing data by default. To enable metrics or disable the OpenTelemetry extension globally or partially these are the properties to use (they are extracted from the config reference below):

| Affected Signal | Property name | Default value | Description |
| --- | --- | --- | --- |
| All | `quarkus.otel.enabled` | true | If false, disable the OpenTelemetry usage at **build** time. |
| All | `quarkus.otel.sdk.disabled` | false | Comes from the OpenTelemetry autoconfiguration. If true, will disable the OpenTelemetry SDK usage at **runtime**. |
| All output | `quarkus.otel.exporter.otlp.enabled` | true | If false, will disable Quarkus default OTLP exporters at **build** time.<br><br>Telemetry will be generated, contexts will be propagated but no telemetry will be sent out. |
| Traces | `quarkus.otel.traces.enabled` | true | If false, disable the OpenTelemetry tracing usage at **build** time. |
| Traces output | `quarkus.otel.traces.exporter` | cdi | List of exporters to be used for tracing, separated by commas. Has one of the values from *ExporterType*: `otlp`, `cdi`, `none`. This is a **build** time property and setting it to `none` will disable tracing data output. |
| Metrics | `quarkus.otel.metrics.enabled` | false | Metrics are disabled by default at **build** time because they are tech preview. |
| Metrics output | `quarkus.otel.metrics.exporter` | cdi | List of exporters to be used for metrics, separated by commas. Has one of the values from *ExporterType*: `otlp`, `cdi`, `none`. This is a **build** time property and setting it to `none` will disable metrics data output. |
| Logs | `quarkus.otel.logs.enabled` | false | Logs are disabled by default at **build** time because they are tech preview. |
| Logs output | `quarkus.otel.logs.exporter` | cdi | List of exporters to be used for logs, separated by commas. Has one of the values from *ExporterType*: `otlp`, `cdi`, `none`. This is a **build** time property and setting it to `none` will disable logs data output. |
| Logs output | `quarkus.otel.logs.handler.enabled` | true | If false, disable the OpenTelemetry logs handler at **runtime**. This removes the bridge between the Quarkus logging system (JBoss LogManager) and OpenTelemetry logs. |

If you need to enable or disable the exporter at runtime, you can use the [sampler](/guides/opentelemetry-tracing/index.md#sampler) because it has the ability to filter out all the spans if needed.

Particular instrumentation components can be disabled in tracing, like ignore client requests but keep server ones. For more details, please check the [OpenTelemetry Tracing Guide](/guides/opentelemetry-tracing/index.md).

### Resource

A [resource](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/overview.md#resources) is a representation of the entity that is producing telemetry, it adds attributes to the exported trace or metric to characterize who is producing the telemetry. Quarkus follows the [resources auto-configuration](https://opentelemetry.io/docs/languages/java/configuration/#resources) specified by the Java OpenTelemetry SDK.

#### Default

The following attributes are added by default to resources.

| Attribute name | Content example | Origin |
| --- | --- | --- |
| service.name | "opentelemetry-quickstart" | Value comes from the artifactId, from the `quarkus.application.name` property or from `quarkus.otel.resource.attributes=service.name=cart` property. |
| host.name | "myHost" | Resolved at startup |
| service.version | "1.0-SNAPSHOT" | Resolved at build time from the artifact version |
| telemetry.sdk.language | "java" | Static value |
| telemetry.sdk.name | "opentelemetry" | Resolved at build time |
| telemetry.sdk.version | "1.32.0" | Resolved at build time |
| webengine.name | "Quarkus" | Static value |
| webengine.version | "999-SNAPSHOT" | Quarkus version resolved at build time |

#### Using configuration

You can add additional attributes by setting the `quarkus.otel.resource.attributes` config property that is described in the [OpenTelemetry Configuration Reference](#configuration-reference). Since this property can be overridden at runtime, the OpenTelemetry extension will pick up its value following the order of precedence that is described in the [Quarkus Configuration Reference](/guides/config-reference/index.md#configuration-sources).

```properties
quarkus.otel.resource.attributes=deployment.environment=dev,service.name=cart,service.namespace=shopping
```

This will add the attributes for `deployment.environment`, `service.name` and `service.namespace` to the resource and be included in traces and metrics.

#### Using CDI beans

If by any means you need to use a custom resource or one that is provided by one of the [OpenTelemetry SDK Extensions](https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions) you can create multiple resource producers. The OpenTelemetry extension will detect the `Resource` CDI beans and will merge them when configuring the OTel SDK.

```java
@ApplicationScoped
public class CustomConfiguration {

    @Produces
    @ApplicationScoped
    public Resource osResource() {
        return OsResource.get();
    }

    @Produces
    @ApplicationScoped
    public Resource ecsResource() {
        return EcsResource.get();
    }
}
```

#### Kubernetes

When running the application in Kubernetes, the [OpenTelemetry Kubernetes resource attributes](https://opentelemetry.io/docs/specs/semconv/resource/k8s/) can be populated in a couple of ways.

##### With the Quarkus Kubernetes extension

When the `quarkus-kubernetes` extension is present, Quarkus automatically adds the following environment variables to the generated Kubernetes manifests:

| Attribute name | Content example | Environment variable | Source |
| --- | --- | --- | --- |
| k8s.namespace.name | "my-namespace" | `QUARKUS_OTEL_K8S_RESOURCE_NAMESPACE` | Kubernetes Downward API: `metadata.namespace`. Falls back to reading the service account namespace file at `/var/run/secrets/kubernetes.io/serviceaccount/namespace`. |
| k8s.pod.name | "my-app-7d6f8b5c9-x2k4m" | `QUARKUS_OTEL_K8S_RESOURCE_POD_NAME` | Kubernetes Downward API: `metadata.name`. |
| k8s.pod.uid | "f4b5c6d7-1234-5678-9abc-def012345678" | `QUARKUS_OTEL_K8S_RESOURCE_POD_UID` | Kubernetes Downward API: `metadata.uid`. |
| k8s.node.name | "node-1" | `QUARKUS_OTEL_K8S_RESOURCE_NODE_NAME` | Kubernetes Downward API: `spec.nodeName`. |
| k8s.container.name | "my-app" | `QUARKUS_OTEL_K8S_RESOURCE_CONTAINER_NAME` | Set to the application name at build time. |
| k8s.deployment.name | "my-app" | `QUARKUS_OTEL_K8S_RESOURCE_DEPLOYMENT_NAME` | Set to `quarkus.kubernetes.name` (or the application name if not configured) at build time. |

The `k8s.cluster.name` attribute is not set automatically because the cluster name is not available through pod metadata. You can set it manually using `quarkus.otel.resource.attributes=k8s.cluster.name=my-cluster` or by setting the `QUARKUS_OTEL_K8S_RESOURCE_CLUSTER_NAME` environment variable.

##### Without the Quarkus Kubernetes extension

If the `quarkus-kubernetes` extension is **not** used, there's the need to manually set the environment variables defined in the previous section. The `quarkus-opentelemetry` extension will then be able to populate the Kubernetes resource attributes. This is a partial sample of a deployment manifest setting up the required env. vars.:

```yaml
spec:
  containers:
    - name: my-app
      env:
        - name: QUARKUS_OTEL_K8S_RESOURCE_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: QUARKUS_OTEL_K8S_RESOURCE_POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: QUARKUS_OTEL_K8S_RESOURCE_POD_UID
          valueFrom:
            fieldRef:
              fieldPath: metadata.uid
        - name: QUARKUS_OTEL_K8S_RESOURCE_NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        - name: QUARKUS_OTEL_K8S_RESOURCE_CONTAINER_NAME
          value: my-app
        - name: QUARKUS_OTEL_K8S_RESOURCE_DEPLOYMENT_NAME
          value: my-app
        - name: QUARKUS_OTEL_K8S_RESOURCE_CLUSTER_NAME
          value: production
```

### Semantic conventions

OpenTelemetry provides a set of [semantic conventions](https://opentelemetry.io/docs/specs/semconv/http/http-spans/) to standardize the data collected by the instrumentation.

When creating manual instrumentation, while naming metrics or attributes you should follow those conventions and not create new names to represent existing conventions. This will make data correlation easier to perform across services.

## Exporters

### The Default

The Quarkus OpenTelemetry extension uses its own signal exporters built on top of Vert.x for optimal performance and maintainability. All **Quarkus built in exporters use the OTLP protocol** through a couple of data senders, using `grpc` (the default) and `http/protobuf`.

The active exporter is automatically wired by CDI, that's why the `quarkus.otel.traces.exporter`, `quarkus.otel.metrics.exporter` and `quarkus.otel.logs.exporter` properties default value is `cdi`. This is not because of the protocol being used in the data transfer but because of how the exporters are wired.

CDI (Context Dependency Injection) will manage the exporters to use, according to the selected protocol or when applications implement their own CDI exporter, like in tests.

The `quarkus.otel.exporter.otlp.protocol` property instructs Quarkus to switch the senders and defaults to `grpc` but `http/protobuf` can also be used.

> [!NOTE]
> If you change the protocol, you also need to change the port in the endpoint. The default port for `grpc` is `4317` and for `http/protobuf` is `4318`.

The `quarkus.otel.exporter.otlp.memory-mode` property configures the memory allocation strategy for OTLP exporters. The default value `immutable-data` creates new serialization objects for each export. Setting it to `reusable-data` enables object pooling to reduce garbage collection pressure, which can be beneficial in memory-constrained environments.

### Using CDI to produce a test exporter

Leaving the default as CDI is particularly useful for tests. In the following example a Span exporter class is wired with CDI and then the telemetry can be used in test code.

Creating a custom `SpanExporter` bean:

```java
    @ApplicationScoped
    static class InMemorySpanExporterProducer {
        @Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }
```

Where `InMemorySpanExporter` is a class from the OpenTelemetry test utilities dependency:

**pom.xml**

```xml
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk-testing</artifactId>
        <scope>test</scope>
    </dependency>
```

**build.gradle**

```gradle
implementation("io.opentelemetry:opentelemetry-sdk-testing")
```

The bean of that class can be injected to access the telemetry data. This is an example to obtain the spans:

```java
    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    //...

    List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
```

If this is used in an integration test, you should access the class from inside the running process and not from the test class. A viable option could be to expose that data through a REST endpoint method:

```java
    @GET
    @Path("/export")
    public List<SpanData> exportTraces() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export")) <1>
                .collect(Collectors.toList());
    }
```

1. This excludes calls to the export endpoint itself.

For more details please take a look at the [ExporterResource](https://github.com/quarkusio/quarkus/blob/main/integration-tests/opentelemetry/src/main/java/io/quarkus/it/opentelemetry/ExporterResource.java) in the Quarkus integration tests.

### The OpenTelemetry OTLP exporter

This is currently not supported in Quarkus. Configuration example for traces: `quarkus.otel.tracing.exporter=otlp`.

However, it's also not needed because Quarkus own default exporters will send data using the OTLP protocol.

### On Quarkiverse

Additional exporters will be available in the Quarkiverse [quarkus-opentelemetry-exporter](https://docs.quarkiverse.io/quarkus-opentelemetry-exporter/dev/index.html) project.

Currently, are available the following exporters (may be outdated) for:

- Legacy Jaeger
- Microsoft Azure
- Google Cloud

Also on Quarkiverse, the [Quarkus AWS SDK has integration with OpenTelemetry](https://docs.quarkiverse.io/quarkus-amazon-services/dev/opentelemetry.html).

<a id="logging-exporter-for-debugging"></a>
### Logging exporter (for debugging)

You can output all metrics to the console, for debugging/development purposes.

> [!IMPORTANT]
> Don't use this in production.

You will need to add the following dependency to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-logging</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.opentelemetry:opentelemetry-exporter-logging")
```

Then, setting the exporter to `logging` in the `application.properties` file:

```properties
quarkus.otel.metrics.exporter=logging <1>
quarkus.otel.metric.export.interval=10000ms <2>
quarkus.otel.traces.exporter=logging <3>
```

1. Set the metrics exporter to `logging`. Normally you don't need to set this. The default is `cdi`.
2. Set the interval to export the metrics. The default is `1m`, which is too long for debugging.
3. Set the traces exporter to `logging`. Normally you don't need to set this. The default is `cdi`.

## Visualizing the data

A Dev Service can receive your app's telemetry.

The Grafana-OTel-LGTM Dev Service will start automatically on Dev Mode and data will be automatically sent to it.

- Take a look at: [Getting Started with Grafana-OTel-LGTM](/guides/observability-devservices-lgtm/index.md).

Grafana is used to visualize data, Loki to store logs, Tempo to store traces and Prometheus to store metrics. Also provides and OTel collector to receive the data.

This provides an easy way to visualize all OpenTelemetry data generated by the application.

You can also use the [logging exporter](#logging-exporter-for-debugging) to output all traces and metrics to the console.

## Dev Services for OpenTelemetry with the LGTM stack

The OpenTelemetry extension provides a Dev Service that automatically starts a Grafana OTel LGTM stack when the application is started in development mode. This allows you to easily visualize the telemetry data generated by your application without needing to set up an external observability stack.

### Enabling / Disabling Dev Services for OpenTelemetry

Dev Services for OpenTelemetry is enabled by default. You can disable it by setting the `quarkus.observability.lgtm.enabled=false` or `quarkus.observability.dev-resources=false` properties in your configuration.

### Full Dev Services for OpenTelemetry reference

The Observability Dev Services guide provides a full reference for all the properties related to Dev Services for OpenTelemetry with the LGTM stack. You can find it at [Observability Dev Services with Grafana OTel LGTM](/guides/observability-devservices-lgtm/index.md).

<a id="configuration-reference"></a>
## OpenTelemetry Configuration Reference

Quarkus supports the OpenTelemetry Autoconfiguration for Traces. The configurations match what you can see at [OpenTelemetry SDK Autoconfigure](https://opentelemetry.io/docs/languages/java/configuration/) adding the usual `quarkus.*` prefix.

Quarkus OpenTelemetry configuration properties now have the `quarkus.otel.*` prefix.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-enabled"></a> [`quarkus.otel.enabled`](#quarkus-opentelemetry_quarkus-otel-enabled) config_property_copy_button:+++quarkus.otel.enabled+++[]<br><br>If false, disable the OpenTelemetry usage at build time. All other Otel properties will be ignored at runtime.<br><br>Will pick up value from legacy property quarkus.opentelemetry.enabled<br><br>Defaults to `true`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-simple"></a> [`quarkus.otel.simple`](#quarkus-opentelemetry_quarkus-otel-simple) config_property_copy_button:+++quarkus.otel.simple+++[]<br><br>Should we use simple processor for spans and log records. This will disable batch processing and the exporter will send telemetry data right away. This is recommended for serverless applications.<br><br>Defaults to `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SIMPLE+++[] | boolean | `false` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-traces-enabled"></a> [`quarkus.otel.traces.enabled`](#quarkus-opentelemetry_quarkus-otel-traces-enabled) config_property_copy_button:+++quarkus.otel.traces.enabled+++[]<br><br>Enable tracing with OpenTelemetry.<br><br>This property is not available in the Open Telemetry SDK. It's Quarkus specific.<br><br>Support for OpenTelemetry Tracing will be enabled if this value is true. The OpenTelemetry SDK ( `io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig#enabled()` ) is enabled by default and if disabled, OpenTelemetry Tracing will also be disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-traces-exporter"></a> [`quarkus.otel.traces.exporter`](#quarkus-opentelemetry_quarkus-otel-traces-exporter) config_property_copy_button:+++quarkus.otel.traces.exporter+++[]<br><br>List of exporters supported by Quarkus.<br><br>List of exporters to be used for tracing, separated by commas. Has one of the values on `ExporterType` `otlp`, `cdi`, `none` or the full qualified name of a class implementing `io.opentelemetry.sdk.trace.export.SpanExporter`<br><br>Default on Quarkus is `ExporterType.Constants#CDI_VALUE`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_EXPORTER+++[] | list of string | `cdi` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-traces-sampler"></a> [`quarkus.otel.traces.sampler`](#quarkus-opentelemetry_quarkus-otel-traces-sampler) config_property_copy_button:+++quarkus.otel.traces.sampler+++[]<br><br>The sampler to use for tracing.<br><br>Has one of the values on `SamplerType` `always_on`, `always_off`, `traceidratio`, `parentbased_always_on`, `parentbased_always_off`, `parentbased_traceidratio` or the Sampler SPI name. This will use the OTel SPI hooks for the `io.opentelemetry.sdk.trace.samplers.Sampler` implementation set in the provider: `io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider`.<br><br>Fallbacks to the legacy property `quarkus.opentelemetry.tracer.sampler.sampler.name` or defaults to `SamplerType.Constants#PARENT_BASED_ALWAYS_ON`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_SAMPLER+++[] | string | `parentbased_always_on` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-traces-eusp-enabled"></a> [`quarkus.otel.traces.eusp.enabled`](#quarkus-opentelemetry_quarkus-otel-traces-eusp-enabled) config_property_copy_button:+++quarkus.otel.traces.eusp.enabled+++[]<br><br>If OpenTelemetry End User attributes should be added as Span attributes on a best-efforts basis.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_EUSP_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-metrics-enabled"></a> [`quarkus.otel.metrics.enabled`](#quarkus-opentelemetry_quarkus-otel-metrics-enabled) config_property_copy_button:+++quarkus.otel.metrics.enabled+++[]<br><br>Enable metrics with OpenTelemetry.<br><br>This property is not available in the Open Telemetry SDK. It's Quarkus specific.<br><br>Support for OpenTelemetry Metrics will be enabled if this value is true. The OpenTelemetry SDK ( `io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig#enabled()` ) is enabled by default and if disabled, OpenTelemetry Metrics will also be disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_METRICS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-metrics-exporter"></a> [`quarkus.otel.metrics.exporter`](#quarkus-opentelemetry_quarkus-otel-metrics-exporter) config_property_copy_button:+++quarkus.otel.metrics.exporter+++[]<br><br>The Metrics exporter to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_METRICS_EXPORTER+++[] | list of string | `cdi` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-logs-enabled"></a> [`quarkus.otel.logs.enabled`](#quarkus-opentelemetry_quarkus-otel-logs-enabled) config_property_copy_button:+++quarkus.otel.logs.enabled+++[]<br><br>Enable logs with OpenTelemetry.<br><br>This property is not available in the Open Telemetry SDK. It's Quarkus specific.<br><br>Support for OpenTelemetry Logs will be enabled if this value is true. The OpenTelemetry SDK ( `io.quarkus.opentelemetry.runtime.config.build.OTelBuildConfig#enabled()` ) is enabled by default and if disabled, OpenTelemetry Logs will also be disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_LOGS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-logs-exporter"></a> [`quarkus.otel.logs.exporter`](#quarkus-opentelemetry_quarkus-otel-logs-exporter) config_property_copy_button:+++quarkus.otel.logs.exporter+++[]<br><br>The Logs exporter to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_LOGS_EXPORTER+++[] | list of string | `cdi` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-propagators"></a> [`quarkus.otel.propagators`](#quarkus-opentelemetry_quarkus-otel-propagators) config_property_copy_button:+++quarkus.otel.propagators+++[]<br><br>The propagators to be used. Use a comma-separated list for multiple propagators.<br><br>Has values from `PropagatorType` or the full qualified name of a class implementing `io.opentelemetry.context.propagation.TextMapPropagator`.<br><br>Default is `PropagatorType.Constants#TRACE_CONTEXT`,`PropagatorType.Constants#BAGGAGE` (W3C).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_PROPAGATORS+++[] | list of string | `tracecontext`, `baggage` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-instrument-grpc"></a> [`quarkus.otel.instrument.grpc`](#quarkus-opentelemetry_quarkus-otel-instrument-grpc) config_property_copy_button:+++quarkus.otel.instrument.grpc+++[]<br><br>Enables instrumentation for gRPC.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_GRPC+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-instrument-messaging"></a> [`quarkus.otel.instrument.messaging`](#quarkus-opentelemetry_quarkus-otel-instrument-messaging) config_property_copy_button:+++quarkus.otel.instrument.messaging+++[]<br><br>Enables instrumentation for Messaging.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_MESSAGING+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-instrument-resteasy-client"></a> [`quarkus.otel.instrument.resteasy-client`](#quarkus-opentelemetry_quarkus-otel-instrument-resteasy-client) config_property_copy_button:+++quarkus.otel.instrument.resteasy-client+++[]<br><br>Enables instrumentation for REST Client backed by RESTEasy Classic.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_RESTEASY_CLIENT+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-instrument-rest"></a> [`quarkus.otel.instrument.rest`](#quarkus-opentelemetry_quarkus-otel-instrument-rest) config_property_copy_button:+++quarkus.otel.instrument.rest+++[]<br><br>Enables instrumentation for Quarkus REST.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_REST+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-instrument-resteasy"></a> [`quarkus.otel.instrument.resteasy`](#quarkus-opentelemetry_quarkus-otel-instrument-resteasy) config_property_copy_button:+++quarkus.otel.instrument.resteasy+++[]<br><br>Enables instrumentation for RESTEasy Classic.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_RESTEASY+++[] | boolean | `true` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-security-events-enabled"></a> [`quarkus.otel.security-events.enabled`](#quarkus-opentelemetry_quarkus-otel-security-events-enabled) config_property_copy_button:+++quarkus.otel.security-events.enabled+++[]<br><br>Whether exporting of the security events is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SECURITY_EVENTS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-security-events-event-types"></a> [`quarkus.otel.security-events.event-types`](#quarkus-opentelemetry_quarkus-otel-security-events-event-types) config_property_copy_button:+++quarkus.otel.security-events.event-types+++[]<br><br>Selects security event types.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SECURITY_EVENTS_EVENT_TYPES+++[] | list of tooltip:all[All the security events.], tooltip:authentication-success[Authentication success event.], tooltip:authentication-failure[Authentication failure event.], tooltip:authorization-success[Authorization success event.], tooltip:authorization-failure[Authorization failure event.], tooltip:other[Any other security event. For example the OpenId Connect security event belongs here.] | `all` |
| [lock] <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-enabled"></a> [`quarkus.otel.exporter.otlp.enabled`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-enabled) config_property_copy_button:+++quarkus.otel.exporter.otlp.enabled+++[]<br><br>Will disable the Quarkus managed OpenTelemetry exporters. No telemetry will be sent output if set to `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-sdk-disabled"></a> [`quarkus.otel.sdk.disabled`](#quarkus-opentelemetry_quarkus-otel-sdk-disabled) config_property_copy_button:+++quarkus.otel.sdk.disabled+++[]<br><br>If true, disable the OpenTelemetry SDK. Runtime configuration.<br><br>Defaults to `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SDK_DISABLED+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-traces-suppress-non-application-uris"></a> [`quarkus.otel.traces.suppress-non-application-uris`](#quarkus-opentelemetry_quarkus-otel-traces-suppress-non-application-uris) config_property_copy_button:+++quarkus.otel.traces.suppress-non-application-uris+++[]<br><br>Suppress non-application uris from trace collection. This will suppress tracing of `/q` endpoints.<br><br>Providing a custom `io.opentelemetry.sdk.trace.samplers.Sampler` CDI Bean will ignore this setting.<br><br>This is a Quarkus specific property. Suppressing non-application uris is enabled by default.<br><br>Fallbacks to the legacy property `quarkus.opentelemetry.tracer.suppress-non-application-uris` or defaults to `true`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_SUPPRESS_NON_APPLICATION_URIS+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-traces-suppress-application-uris"></a> [`quarkus.otel.traces.suppress-application-uris`](#quarkus-opentelemetry_quarkus-otel-traces-suppress-application-uris) config_property_copy_button:+++quarkus.otel.traces.suppress-application-uris+++[]<br><br>Comma-separated, suppress application uris from trace collection.<br><br>This will suppress all uris set by this property.<br><br>If you are using `quarkus.http.root-path`, you need to consider it when setting your uris, in other words, you need to configure it using the root-path if necessary.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_SUPPRESS_APPLICATION_URIS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-traces-include-static-resources"></a> [`quarkus.otel.traces.include-static-resources`](#quarkus-opentelemetry_quarkus-otel-traces-include-static-resources) config_property_copy_button:+++quarkus.otel.traces.include-static-resources+++[]<br><br>Include static resources from trace collection.<br><br>This is a Quarkus specific property. Include static resources is disabled by default. Providing a custom `io.opentelemetry.sdk.trace.samplers.Sampler` CDI Bean will ignore this setting.<br><br>Fallbacks to the legacy property `quarkus.opentelemetry.tracer.include-static-resources` or defaults to `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_INCLUDE_STATIC_RESOURCES+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-traces-sampler-arg"></a> [`quarkus.otel.traces.sampler.arg`](#quarkus-opentelemetry_quarkus-otel-traces-sampler-arg) config_property_copy_button:+++quarkus.otel.traces.sampler.arg+++[]<br><br>Sampler argument. Depends on the `quarkus.otel.traces.sampler` property. Fallbacks to the legacy property `quarkus.opentelemetry.tracer.sampler.ratio`.<br><br>When setting the stock sampler to `traceidratio` or `parentbased_traceidratio` you need to set a `double` compatible value between `0.0d` and `1.0d`, like `0.01d` or `0.5d`. It is kept as a `String` to allow the flexible customisation of alternative samplers.<br><br>Defaults to `1.0d`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_TRACES_SAMPLER_ARG+++[] | string | `1.0d` |
| <a id="quarkus-opentelemetry_quarkus-otel-metric-export-interval"></a> [`quarkus.otel.metric.export.interval`](#quarkus-opentelemetry_quarkus-otel-metric-export-interval) config_property_copy_button:+++quarkus.otel.metric.export.interval+++[]<br><br>The interval, between the start of two metric export attempts.<br><br>Default is 1min.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_METRIC_EXPORT_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `60S` |
| <a id="quarkus-opentelemetry_quarkus-otel-logs-handler-enabled"></a> [`quarkus.otel.logs.handler.enabled`](#quarkus-opentelemetry_quarkus-otel-logs-handler-enabled) config_property_copy_button:+++quarkus.otel.logs.handler.enabled+++[]<br><br>Determine whether to enable the OpenTelemetry logging handler.<br><br>This is a Quarkus specific property. The OpenTelemetry logging handler is enabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_LOGS_HANDLER_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-logs-level"></a> [`quarkus.otel.logs.level`](#quarkus-opentelemetry_quarkus-otel-logs-level) config_property_copy_button:+++quarkus.otel.logs.level+++[]<br><br>The log level to use for the OpenTelemetry logging handler.<br><br>This is a Quarkus specific property. The default log level is ALL.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_LOGS_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-opentelemetry_quarkus-otel-attribute-value-length-limit"></a> [`quarkus.otel.attribute.value.length.limit`](#quarkus-opentelemetry_quarkus-otel-attribute-value-length-limit) config_property_copy_button:+++quarkus.otel.attribute.value.length.limit+++[]<br><br>The maximum length of attribute values. Applies to spans and logs.<br><br>By default, there is no limit.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-attribute-count-limit"></a> [`quarkus.otel.attribute.count.limit`](#quarkus-opentelemetry_quarkus-otel-attribute-count-limit) config_property_copy_button:+++quarkus.otel.attribute.count.limit+++[]<br><br>The maximum number of attributes. Applies to spans, span events, span links, and logs.<br><br>Default is `128`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_ATTRIBUTE_COUNT_LIMIT+++[] | int | `128` |
| <a id="quarkus-opentelemetry_quarkus-otel-span-attribute-value-length-limit"></a> [`quarkus.otel.span.attribute.value.length.limit`](#quarkus-opentelemetry_quarkus-otel-span-attribute-value-length-limit) config_property_copy_button:+++quarkus.otel.span.attribute.value.length.limit+++[]<br><br>The maximum length of span attribute values. Takes precedence over `otel.attribute.value.length.limit`.<br><br>By default, there is no limit.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT+++[] | int |  |
| <a id="quarkus-opentelemetry_quarkus-otel-span-attribute-count-limit"></a> [`quarkus.otel.span.attribute.count.limit`](#quarkus-opentelemetry_quarkus-otel-span-attribute-count-limit) config_property_copy_button:+++quarkus.otel.span.attribute.count.limit+++[]<br><br>The maximum number of attributes per span. Takes precedence over `otel.attribute.count.limit`.<br><br>Default is `128`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT+++[] | int | `128` |
| <a id="quarkus-opentelemetry_quarkus-otel-span-event-count-limit"></a> [`quarkus.otel.span.event.count.limit`](#quarkus-opentelemetry_quarkus-otel-span-event-count-limit) config_property_copy_button:+++quarkus.otel.span.event.count.limit+++[]<br><br>The maximum number of events per span.<br><br>Default is `128`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SPAN_EVENT_COUNT_LIMIT+++[] | int | `128` |
| <a id="quarkus-opentelemetry_quarkus-otel-span-link-count-limit"></a> [`quarkus.otel.span.link.count.limit`](#quarkus-opentelemetry_quarkus-otel-span-link-count-limit) config_property_copy_button:+++quarkus.otel.span.link.count.limit+++[]<br><br>The maximum number of links per span.<br><br>Default is `128`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SPAN_LINK_COUNT_LIMIT+++[] | int | `128` |
| <a id="quarkus-opentelemetry_quarkus-otel-bsp-schedule-delay"></a> [`quarkus.otel.bsp.schedule.delay`](#quarkus-opentelemetry_quarkus-otel-bsp-schedule-delay) config_property_copy_button:+++quarkus.otel.bsp.schedule.delay+++[]<br><br>The Batch Span Processor interval `Duration` between two consecutive exports.<br><br>Default is `5s`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BSP_SCHEDULE_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `5S` |
| <a id="quarkus-opentelemetry_quarkus-otel-bsp-max-queue-size"></a> [`quarkus.otel.bsp.max.queue.size`](#quarkus-opentelemetry_quarkus-otel-bsp-max-queue-size) config_property_copy_button:+++quarkus.otel.bsp.max.queue.size+++[]<br><br>The Batch Span Processor maximum queue size.<br><br>Default is `2048`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BSP_MAX_QUEUE_SIZE+++[] | int | `2048` |
| <a id="quarkus-opentelemetry_quarkus-otel-bsp-max-export-batch-size"></a> [`quarkus.otel.bsp.max.export.batch.size`](#quarkus-opentelemetry_quarkus-otel-bsp-max-export-batch-size) config_property_copy_button:+++quarkus.otel.bsp.max.export.batch.size+++[]<br><br>The Batch Span Processor maximum batch size.<br><br>Default is `512`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BSP_MAX_EXPORT_BATCH_SIZE+++[] | int | `512` |
| <a id="quarkus-opentelemetry_quarkus-otel-bsp-export-timeout"></a> [`quarkus.otel.bsp.export.timeout`](#quarkus-opentelemetry_quarkus-otel-bsp-export-timeout) config_property_copy_button:+++quarkus.otel.bsp.export.timeout+++[]<br><br>The Batch Span Processor maximum allowed time `Duration` to export data.<br><br>Default is `30s`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BSP_EXPORT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `30S` |
| <a id="quarkus-opentelemetry_quarkus-otel-blrp-schedule-delay"></a> [`quarkus.otel.blrp.schedule.delay`](#quarkus-opentelemetry_quarkus-otel-blrp-schedule-delay) config_property_copy_button:+++quarkus.otel.blrp.schedule.delay+++[]<br><br>The Batch Log Record Processor interval `Duration` between two consecutive exports.<br><br>Default is `1S`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BLRP_SCHEDULE_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `1S` |
| <a id="quarkus-opentelemetry_quarkus-otel-blrp-max-queue-size"></a> [`quarkus.otel.blrp.max.queue.size`](#quarkus-opentelemetry_quarkus-otel-blrp-max-queue-size) config_property_copy_button:+++quarkus.otel.blrp.max.queue.size+++[]<br><br>The Batch Log Record Processor maximum queue size.<br><br>Default is `2048`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BLRP_MAX_QUEUE_SIZE+++[] | int | `2048` |
| <a id="quarkus-opentelemetry_quarkus-otel-blrp-max-export-batch-size"></a> [`quarkus.otel.blrp.max.export.batch.size`](#quarkus-opentelemetry_quarkus-otel-blrp-max-export-batch-size) config_property_copy_button:+++quarkus.otel.blrp.max.export.batch.size+++[]<br><br>The Batch Log Record Processor maximum batch size.<br><br>Default is `512`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BLRP_MAX_EXPORT_BATCH_SIZE+++[] | int | `512` |
| <a id="quarkus-opentelemetry_quarkus-otel-blrp-export-timeout"></a> [`quarkus.otel.blrp.export.timeout`](#quarkus-opentelemetry_quarkus-otel-blrp-export-timeout) config_property_copy_button:+++quarkus.otel.blrp.export.timeout+++[]<br><br>The Batch Log Record Processor maximum allowed time `Duration` to export data.<br><br>Default is `30s`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_BLRP_EXPORT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `30S` |
| <a id="quarkus-opentelemetry_quarkus-otel-resource-attributes"></a> [`quarkus.otel.resource.attributes`](#quarkus-opentelemetry_quarkus-otel-resource-attributes) config_property_copy_button:+++quarkus.otel.resource.attributes+++[]<br><br>Specify resource attributes in the following format: `key1=val1,key2=val2,key3=val3`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_RESOURCE_ATTRIBUTES+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-service-name"></a> [`quarkus.otel.service.name`](#quarkus-opentelemetry_quarkus-otel-service-name) config_property_copy_button:+++quarkus.otel.service.name+++[]<br><br>Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes and from quarkus.application.name.<br><br>Defaults to `quarkus.application.name`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_SERVICE_NAME+++[] | string | `${quarkus.application.name:unset}` |
| <a id="quarkus-opentelemetry_quarkus-otel-experimental-resource-disabled-keys"></a> [`quarkus.otel.experimental.resource.disabled-keys`](#quarkus-opentelemetry_quarkus-otel-experimental-resource-disabled-keys) config_property_copy_button:+++quarkus.otel.experimental.resource.disabled-keys+++[]<br><br>Specify resource attribute keys that are filtered.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPERIMENTAL_RESOURCE_DISABLED_KEYS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-experimental-shutdown-wait-time"></a> [`quarkus.otel.experimental.shutdown-wait-time`](#quarkus-opentelemetry_quarkus-otel-experimental-shutdown-wait-time) config_property_copy_button:+++quarkus.otel.experimental.shutdown-wait-time+++[]<br><br>The maximum amount of time Quarkus will wait for the OpenTelemetry SDK to flush unsent spans and shutdown.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPERIMENTAL_SHUTDOWN_WAIT_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `2S` |
| <a id="quarkus-opentelemetry_quarkus-otel-instrument-vertx-http"></a> [`quarkus.otel.instrument.vertx-http`](#quarkus-opentelemetry_quarkus-otel-instrument-vertx-http) config_property_copy_button:+++quarkus.otel.instrument.vertx-http+++[]<br><br>Enables instrumentation for Vert.x HTTP.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_VERTX_HTTP+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-instrument-vertx-event-bus"></a> [`quarkus.otel.instrument.vertx-event-bus`](#quarkus-opentelemetry_quarkus-otel-instrument-vertx-event-bus) config_property_copy_button:+++quarkus.otel.instrument.vertx-event-bus+++[]<br><br>Enables instrumentation for Vert.x Event Bus.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_VERTX_EVENT_BUS+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-instrument-vertx-sql-client"></a> [`quarkus.otel.instrument.vertx-sql-client`](#quarkus-opentelemetry_quarkus-otel-instrument-vertx-sql-client) config_property_copy_button:+++quarkus.otel.instrument.vertx-sql-client+++[]<br><br>Enables instrumentation for Vert.x SQL Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_VERTX_SQL_CLIENT+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-instrument-vertx-redis-client"></a> [`quarkus.otel.instrument.vertx-redis-client`](#quarkus-opentelemetry_quarkus-otel-instrument-vertx-redis-client) config_property_copy_button:+++quarkus.otel.instrument.vertx-redis-client+++[]<br><br>Enables instrumentation for Vert.x Redis Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_VERTX_REDIS_CLIENT+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-instrument-jvm-metrics"></a> [`quarkus.otel.instrument.jvm-metrics`](#quarkus-opentelemetry_quarkus-otel-instrument-jvm-metrics) config_property_copy_button:+++quarkus.otel.instrument.jvm-metrics+++[]<br><br>Enables instrumentation for JVM Metrics.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_JVM_METRICS+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-instrument-http-server-metrics"></a> [`quarkus.otel.instrument.http-server-metrics`](#quarkus-opentelemetry_quarkus-otel-instrument-http-server-metrics) config_property_copy_button:+++quarkus.otel.instrument.http-server-metrics+++[]<br><br>Enables instrumentation for HTTP Server Metrics.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_INSTRUMENT_HTTP_SERVER_METRICS+++[] | boolean | `true` |
| <a id="quarkus-opentelemetry_quarkus-otel-experimental-sdk-telemetry-version"></a> [`quarkus.otel.experimental.sdk.telemetry.version`](#quarkus-opentelemetry_quarkus-otel-experimental-sdk-telemetry-version) config_property_copy_button:+++quarkus.otel.experimental.sdk.telemetry.version+++[]<br><br>Controls the OpenTelemetry SDK internal telemetry version.<br><br>When set to `latest`, the SDK emits self-diagnostics metrics for spans (`otel.sdk.span.live`, `otel.sdk.span.started`) in addition to metric reader diagnostics (`otel.sdk.metric_reader.collection.duration`). When set to `legacy`, only metric reader diagnostics are emitted.<br><br>Defaults to `latest`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPERIMENTAL_SDK_TELEMETRY_VERSION+++[] | string | `latest` |
| <a id="quarkus-opentelemetry_quarkus-otel-mp-compatibility"></a> [`quarkus.otel.mp.compatibility`](#quarkus-opentelemetry_quarkus-otel-mp-compatibility) config_property_copy_button:+++quarkus.otel.mp.compatibility+++[]<br><br>Prioritize OpenTelemetry configuration `otel.` on top of Quarkus OpenTelemetry configuration `quarkus.otel`.<br><br>By default, Quarkus configuration has priority over OpenTelemetry configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_MP_COMPATIBILITY+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-endpoint"></a> [`quarkus.otel.exporter.otlp.endpoint`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-endpoint) config_property_copy_button:+++quarkus.otel.exporter.otlp.endpoint+++[]<br><br>Sets the OTLP endpoint to send telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..endpoint` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics) and the default port will be `OtlpExporterRuntimeConfig#DEFAULT_HTTP_BASE_URI`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT+++[] | string | `http://localhost:4317/` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-headers"></a> [`quarkus.otel.exporter.otlp.headers`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-headers) config_property_copy_button:+++quarkus.otel.exporter.otlp.headers+++[]<br><br>Key-value pairs to be used as headers associated with exporter requests. The format is similar to the `OTEL_EXPORTER_OTLP_HEADERS` environment variable, a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..headers` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_HEADERS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-compression"></a> [`quarkus.otel.exporter.otlp.compression`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-compression) config_property_copy_button:+++quarkus.otel.exporter.otlp.compression+++[]<br><br>Sets the method used to compress payloads. If unset, compression is disabled. Currently supported compression methods include `gzip` and `none`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..compression` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_COMPRESSION+++[] | `gzip`, `none` |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-timeout"></a> [`quarkus.otel.exporter.otlp.timeout`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-timeout) config_property_copy_button:+++quarkus.otel.exporter.otlp.timeout+++[]<br><br>Sets the maximum time to wait for the collector to process an exported batch of telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS`s.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..timeout` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `10s` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-protocol"></a> [`quarkus.otel.exporter.otlp.protocol`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-protocol) config_property_copy_button:+++quarkus.otel.exporter.otlp.protocol+++[]<br><br>OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server. Depending on the exporter, the available protocols will be different.<br><br>Currently, only `grpc` and `http/protobuf` are allowed.<br><br>Please mind that changing the protocol requires changing the port in the endpoint as well.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..protocol` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_PROTOCOL+++[] | string | `grpc` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-key-cert-keys"></a> [`quarkus.otel.exporter.otlp.key-cert.keys`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-key-cert-keys) config_property_copy_button:+++quarkus.otel.exporter.otlp.key-cert.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_KEY_CERT_KEYS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-key-cert-certs"></a> [`quarkus.otel.exporter.otlp.key-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-key-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.key-cert.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_KEY_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-trust-cert-certs"></a> [`quarkus.otel.exporter.otlp.trust-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-trust-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.trust-cert.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRUST_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-tls-configuration-name"></a> [`quarkus.otel.exporter.otlp.tls-configuration-name`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-tls-configuration-name) config_property_copy_button:+++quarkus.otel.exporter.otlp.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If not set and the default TLS configuration is configured (`quarkus.tls.*`) then that will be used. If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..tls-configuration-name` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-enabled"></a> [`quarkus.otel.exporter.otlp.proxy-options.enabled`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-enabled) config_property_copy_button:+++quarkus.otel.exporter.otlp.proxy-options.enabled+++[]<br><br>If proxy connection must be used.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.enabled` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_PROXY_OPTIONS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-username"></a> [`quarkus.otel.exporter.otlp.proxy-options.username`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-username) config_property_copy_button:+++quarkus.otel.exporter.otlp.proxy-options.username+++[]<br><br>Set proxy username.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.username` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_PROXY_OPTIONS_USERNAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-password"></a> [`quarkus.otel.exporter.otlp.proxy-options.password`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-password) config_property_copy_button:+++quarkus.otel.exporter.otlp.proxy-options.password+++[]<br><br>Set proxy password.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.password` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_PROXY_OPTIONS_PASSWORD+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-port"></a> [`quarkus.otel.exporter.otlp.proxy-options.port`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-port) config_property_copy_button:+++quarkus.otel.exporter.otlp.proxy-options.port+++[]<br><br>Set proxy port.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.port` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_PROXY_OPTIONS_PORT+++[] | int |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-host"></a> [`quarkus.otel.exporter.otlp.proxy-options.host`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-proxy-options-host) config_property_copy_button:+++quarkus.otel.exporter.otlp.proxy-options.host+++[]<br><br>Set proxy host.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.host` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_PROXY_OPTIONS_HOST+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-endpoint"></a> [`quarkus.otel.exporter.otlp.traces.endpoint`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-endpoint) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.endpoint+++[]<br><br>Sets the OTLP endpoint to send telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..endpoint` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics) and the default port will be `OtlpExporterRuntimeConfig#DEFAULT_HTTP_BASE_URI`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT+++[] | string | `http://localhost:4317/` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-headers"></a> [`quarkus.otel.exporter.otlp.traces.headers`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-headers) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.headers+++[]<br><br>Key-value pairs to be used as headers associated with exporter requests. The format is similar to the `OTEL_EXPORTER_OTLP_HEADERS` environment variable, a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..headers` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_HEADERS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-compression"></a> [`quarkus.otel.exporter.otlp.traces.compression`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-compression) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.compression+++[]<br><br>Sets the method used to compress payloads. If unset, compression is disabled. Currently supported compression methods include `gzip` and `none`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..compression` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_COMPRESSION+++[] | `gzip`, `none` |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-timeout"></a> [`quarkus.otel.exporter.otlp.traces.timeout`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-timeout) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.timeout+++[]<br><br>Sets the maximum time to wait for the collector to process an exported batch of telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS`s.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..timeout` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `10s` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-protocol"></a> [`quarkus.otel.exporter.otlp.traces.protocol`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-protocol) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.protocol+++[]<br><br>OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server. Depending on the exporter, the available protocols will be different.<br><br>Currently, only `grpc` and `http/protobuf` are allowed.<br><br>Please mind that changing the protocol requires changing the port in the endpoint as well.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..protocol` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROTOCOL+++[] | string | `grpc` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-key-cert-keys"></a> [`quarkus.otel.exporter.otlp.traces.key-cert.keys`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-key-cert-keys) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.key-cert.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_KEY_CERT_KEYS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-key-cert-certs"></a> [`quarkus.otel.exporter.otlp.traces.key-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-key-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.key-cert.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_KEY_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-trust-cert-certs"></a> [`quarkus.otel.exporter.otlp.traces.trust-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-trust-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.trust-cert.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_TRUST_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-tls-configuration-name"></a> [`quarkus.otel.exporter.otlp.traces.tls-configuration-name`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-tls-configuration-name) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If not set and the default TLS configuration is configured (`quarkus.tls.*`) then that will be used. If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..tls-configuration-name` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-enabled"></a> [`quarkus.otel.exporter.otlp.traces.proxy-options.enabled`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-enabled) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.proxy-options.enabled+++[]<br><br>If proxy connection must be used.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.enabled` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROXY_OPTIONS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-username"></a> [`quarkus.otel.exporter.otlp.traces.proxy-options.username`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-username) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.proxy-options.username+++[]<br><br>Set proxy username.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.username` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROXY_OPTIONS_USERNAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-password"></a> [`quarkus.otel.exporter.otlp.traces.proxy-options.password`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-password) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.proxy-options.password+++[]<br><br>Set proxy password.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.password` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROXY_OPTIONS_PASSWORD+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-port"></a> [`quarkus.otel.exporter.otlp.traces.proxy-options.port`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-port) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.proxy-options.port+++[]<br><br>Set proxy port.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.port` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROXY_OPTIONS_PORT+++[] | int |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-host"></a> [`quarkus.otel.exporter.otlp.traces.proxy-options.host`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-traces-proxy-options-host) config_property_copy_button:+++quarkus.otel.exporter.otlp.traces.proxy-options.host+++[]<br><br>Set proxy host.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.host` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROXY_OPTIONS_HOST+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-endpoint"></a> [`quarkus.otel.exporter.otlp.metrics.endpoint`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-endpoint) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.endpoint+++[]<br><br>Sets the OTLP endpoint to send telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..endpoint` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics) and the default port will be `OtlpExporterRuntimeConfig#DEFAULT_HTTP_BASE_URI`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_ENDPOINT+++[] | string | `http://localhost:4317/` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-headers"></a> [`quarkus.otel.exporter.otlp.metrics.headers`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-headers) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.headers+++[]<br><br>Key-value pairs to be used as headers associated with exporter requests. The format is similar to the `OTEL_EXPORTER_OTLP_HEADERS` environment variable, a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..headers` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_HEADERS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-compression"></a> [`quarkus.otel.exporter.otlp.metrics.compression`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-compression) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.compression+++[]<br><br>Sets the method used to compress payloads. If unset, compression is disabled. Currently supported compression methods include `gzip` and `none`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..compression` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_COMPRESSION+++[] | `gzip`, `none` |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-timeout"></a> [`quarkus.otel.exporter.otlp.metrics.timeout`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-timeout) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.timeout+++[]<br><br>Sets the maximum time to wait for the collector to process an exported batch of telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS`s.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..timeout` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `10s` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-protocol"></a> [`quarkus.otel.exporter.otlp.metrics.protocol`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-protocol) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.protocol+++[]<br><br>OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server. Depending on the exporter, the available protocols will be different.<br><br>Currently, only `grpc` and `http/protobuf` are allowed.<br><br>Please mind that changing the protocol requires changing the port in the endpoint as well.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..protocol` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_PROTOCOL+++[] | string | `grpc` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-key-cert-keys"></a> [`quarkus.otel.exporter.otlp.metrics.key-cert.keys`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-key-cert-keys) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.key-cert.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_KEY_CERT_KEYS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-key-cert-certs"></a> [`quarkus.otel.exporter.otlp.metrics.key-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-key-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.key-cert.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_KEY_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-trust-cert-certs"></a> [`quarkus.otel.exporter.otlp.metrics.trust-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-trust-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.trust-cert.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_TRUST_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-tls-configuration-name"></a> [`quarkus.otel.exporter.otlp.metrics.tls-configuration-name`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-tls-configuration-name) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If not set and the default TLS configuration is configured (`quarkus.tls.*`) then that will be used. If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..tls-configuration-name` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-enabled"></a> [`quarkus.otel.exporter.otlp.metrics.proxy-options.enabled`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-enabled) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.proxy-options.enabled+++[]<br><br>If proxy connection must be used.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.enabled` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_PROXY_OPTIONS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-username"></a> [`quarkus.otel.exporter.otlp.metrics.proxy-options.username`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-username) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.proxy-options.username+++[]<br><br>Set proxy username.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.username` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_PROXY_OPTIONS_USERNAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-password"></a> [`quarkus.otel.exporter.otlp.metrics.proxy-options.password`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-password) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.proxy-options.password+++[]<br><br>Set proxy password.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.password` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_PROXY_OPTIONS_PASSWORD+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-port"></a> [`quarkus.otel.exporter.otlp.metrics.proxy-options.port`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-port) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.proxy-options.port+++[]<br><br>Set proxy port.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.port` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_PROXY_OPTIONS_PORT+++[] | int |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-host"></a> [`quarkus.otel.exporter.otlp.metrics.proxy-options.host`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-proxy-options-host) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.proxy-options.host+++[]<br><br>Set proxy host.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.host` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_PROXY_OPTIONS_HOST+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-temporality-preference"></a> [`quarkus.otel.exporter.otlp.metrics.temporality-preference`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-temporality-preference) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.temporality-preference+++[]<br><br>The preferred output aggregation temporality. Options include DELTA, LOWMEMORY, and CUMULATIVE.<br><br>If CUMULATIVE, all instruments will have cumulative temporality. If DELTA, counter (sync and async) and histograms will be delta, up down counters (sync and async) will be cumulative. If LOWMEMORY, sync counter and histograms will be delta, async counter and up down counters (sync and async) will be cumulative.<br><br>Default is CUMULATIVE.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE+++[] | string | `cumulative` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-default-histogram-aggregation"></a> [`quarkus.otel.exporter.otlp.metrics.default-histogram-aggregation`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-metrics-default-histogram-aggregation) config_property_copy_button:+++quarkus.otel.exporter.otlp.metrics.default-histogram-aggregation+++[]<br><br>The preferred default histogram aggregation.<br><br>Options include BASE2_EXPONENTIAL_BUCKET_HISTOGRAM and EXPLICIT_BUCKET_HISTOGRAM.<br><br>Default is EXPLICIT_BUCKET_HISTOGRAM.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION+++[] | string | `explicit_bucket_histogram` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-endpoint"></a> [`quarkus.otel.exporter.otlp.logs.endpoint`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-endpoint) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.endpoint+++[]<br><br>Sets the OTLP endpoint to send telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_GRPC_BASE_URI`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..endpoint` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>If protocol is `http/protobuf` the version and signal will be appended to the path (e.g. v1/traces or v1/metrics) and the default port will be `OtlpExporterRuntimeConfig#DEFAULT_HTTP_BASE_URI`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_ENDPOINT+++[] | string | `http://localhost:4317/` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-headers"></a> [`quarkus.otel.exporter.otlp.logs.headers`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-headers) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.headers+++[]<br><br>Key-value pairs to be used as headers associated with exporter requests. The format is similar to the `OTEL_EXPORTER_OTLP_HEADERS` environment variable, a list of key-value pairs separated by the "=" character. i.e.: key1=value1,key2=value2<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..headers` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_HEADERS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-compression"></a> [`quarkus.otel.exporter.otlp.logs.compression`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-compression) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.compression+++[]<br><br>Sets the method used to compress payloads. If unset, compression is disabled. Currently supported compression methods include `gzip` and `none`.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..compression` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_COMPRESSION+++[] | `gzip`, `none` |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-timeout"></a> [`quarkus.otel.exporter.otlp.logs.timeout`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-timeout) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.timeout+++[]<br><br>Sets the maximum time to wait for the collector to process an exported batch of telemetry data. If unset, defaults to `OtlpExporterRuntimeConfig#DEFAULT_TIMEOUT_SECS`s.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..timeout` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-opentelemetry_quarkus-otel) | `10s` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-protocol"></a> [`quarkus.otel.exporter.otlp.logs.protocol`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-protocol) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.protocol+++[]<br><br>OTLP defines the encoding of telemetry data and the protocol used to exchange data between the client and the server. Depending on the exporter, the available protocols will be different.<br><br>Currently, only `grpc` and `http/protobuf` are allowed.<br><br>Please mind that changing the protocol requires changing the port in the endpoint as well.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..protocol` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_PROTOCOL+++[] | string | `grpc` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-key-cert-keys"></a> [`quarkus.otel.exporter.otlp.logs.key-cert.keys`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-key-cert-keys) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.key-cert.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_KEY_CERT_KEYS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-key-cert-certs"></a> [`quarkus.otel.exporter.otlp.logs.key-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-key-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.key-cert.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_KEY_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-trust-cert-certs"></a> [`quarkus.otel.exporter.otlp.logs.trust-cert.certs`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-trust-cert-certs) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.trust-cert.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_TRUST_CERT_CERTS+++[] | list of string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-tls-configuration-name"></a> [`quarkus.otel.exporter.otlp.logs.tls-configuration-name`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-tls-configuration-name) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If not set and the default TLS configuration is configured (`quarkus.tls.*`) then that will be used. If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..tls-configuration-name` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-enabled"></a> [`quarkus.otel.exporter.otlp.logs.proxy-options.enabled`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-enabled) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.proxy-options.enabled+++[]<br><br>If proxy connection must be used.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.enabled` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_PROXY_OPTIONS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-username"></a> [`quarkus.otel.exporter.otlp.logs.proxy-options.username`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-username) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.proxy-options.username+++[]<br><br>Set proxy username.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.username` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_PROXY_OPTIONS_USERNAME+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-password"></a> [`quarkus.otel.exporter.otlp.logs.proxy-options.password`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-password) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.proxy-options.password+++[]<br><br>Set proxy password.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.password` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_PROXY_OPTIONS_PASSWORD+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-port"></a> [`quarkus.otel.exporter.otlp.logs.proxy-options.port`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-port) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.proxy-options.port+++[]<br><br>Set proxy port.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.port` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_PROXY_OPTIONS_PORT+++[] | int |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-host"></a> [`quarkus.otel.exporter.otlp.logs.proxy-options.host`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-logs-proxy-options-host) config_property_copy_button:+++quarkus.otel.exporter.otlp.logs.proxy-options.host+++[]<br><br>Set proxy host.<br><br>There is a generic property, that will apply to all signals and a signal specific one, following the pattern: `quarkus.otel.exporter.otlp..proxy-options.host` where  is one of the supported signal types, like `traces` or `metrics`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_LOGS_PROXY_OPTIONS_HOST+++[] | string |  |
| <a id="quarkus-opentelemetry_quarkus-otel-exporter-otlp-memory-mode"></a> [`quarkus.otel.exporter.otlp.memory-mode`](#quarkus-opentelemetry_quarkus-otel-exporter-otlp-memory-mode) config_property_copy_button:+++quarkus.otel.exporter.otlp.memory-mode+++[]<br><br>The memory mode of the OTLP exporter.<br><br>When `reusable-data`, the exporter will reuse marshaler objects to reduce allocations and GC pressure. When `immutable-data`, the exporter will create new marshaler objects for each export operation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OTEL_EXPORTER_OTLP_MEMORY_MODE+++[] | `reusable-data`, `immutable-data` | `immutable-data` |

<a id="duration-note-anchor-quarkus-opentelemetry_quarkus-otel"></a>

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
