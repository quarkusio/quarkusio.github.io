# Micrometer and OpenTelemetry extension

This extension provides support for both Micrometer and OpenTelemetry in Quarkus applications. It streamlines integration by incorporating both extensions along with a bridge that enables sending Micrometer metrics via OpenTelemetry.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

> [!NOTE]
> - This extension is available since Quarkus version 3.19.
> - The [Micrometer Guide](/guides/telemetry-micrometer/index.md) is available for detailed information about the Micrometer extension.
> - The [OpenTelemetry Guide](/guides/opentelemetry/index.md) provides information about the OpenTelemetry extension.

The extension allows the normal use of the Micrometer API, but have the metrics handled by the OpenTelemetry extension.

As an example, the `@Timed` annotation from Micrometer is used to measure the execution time of a method:

```java
import io.micrometer.core.annotation.Timed;
//...
@Timed(name = "timer_metric")
public String timer() {
    return "OK";
}
```

The output telemetry data is handled by the OpenTelemetry SDK and sent by the `quarkus-opentelemetry` extension exporter using the OTLP protocol.

This reduces the overhead of having an independent Micrometer registry plus the OpenTelemetry SDK in memory for the same application when both `quarkus-micrometer` and `quarkus-opentelemetry` extensions are used independently.

**The OpenTelemetry SDK will handle all metrics.** Either Micrometer metrics (manual or automatic) and OpenTelemetry Metrics can be used. All are available with this single extension.

All the configurations from the OpenTelemetry and Micrometer extensions are available with `quarkus-micrometer-opentelemetry`.

The bridge is more than the simple OTLP registry found in Quarkiverse. In this extension, the OpenTelemetry SDK provides a Micrometer registry implementation based on the [`micrometer/micrometer-1.5`](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/micrometer/micrometer-1.5/library) OpenTelemetry instrumentation library.

## Usage

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

## Configuration

When the extension is present, Micrometer is enabled by default as are OpenTelemetry tracing, metrics and logs.

OpenTelemetry metrics auto-instrumentation for HTTP server and JVM metrics are disabled by default because those metrics can be  collected by Micrometer.

Specific automatic Micrometer metrics are all disabled by default and can be enabled by setting, for example in the case of JVM metrics:

```properties
quarkus.micrometer.binder.jvm=true
```

in the `application.properties` file.

For this and other properties you can use with the extension, Please refer to:

- [Micrometer metrics configuration properties](/guides/telemetry-micrometer/index.md#configuration-reference)
- [OpenTelemetry configuration properties](/guides/opentelemetry/index.md#configuration-reference)

## Metric differences between Micrometer and OpenTelemetry

### API differences

The metrics produced with each framework follow different APIs and the mapping is not 1:1.

One fundamental API difference is that Micrometer uses a [Timer](https://docs.micrometer.io/micrometer/reference/concepts/timers.html) and OpenTelemetry uses a [Histogram](https://opentelemetry.io/docs/specs/otel/metrics/data-model/#histogram) to record latency (execution time) metrics and the frequency of the events.

When using the `@Timed` annotation with Micrometer, 2 different metrics are [created on the OpenTelemetry side](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/324fdbdd452ddffaf2da2c5bf004d8bb3fdfa1dd/instrumentation/micrometer/micrometer-1.5/library/src/main/java/io/opentelemetry/instrumentation/micrometer/v1_5/OpenTelemetryTimer.java#L31), one `Gauge` for the `max` value and one `Histogram`.

The `DistributionSummary` from Micrometer is transformed into a `Histogram` and a `DoubleGauge` for the `max` value. If service level objectives (slo) are set to `true` when creating a `DistributionSummary`, an additional histogram is created for them.

This table shows the differences between the two frameworks:

| Micrometer | OpenTelemetry |
| --- | --- |
| DistributionSummary | `<Metric name>` (Histogram), `<Metric name>.max` (DoubleGauge) |
| DistributionSummary with SLOs | `<Metric name>` (Histogram), `<Metric name>.max` (DoubleGauge), `<Metric name>.histogram` (DoubleGauge) |
| LongTaskTimer | `<Metric name>.active` (ObservableLongUpDownCounter), `<Metric name>.duration` (ObservableDoubleUpDownCounter) |
| Timer | `<Metric name>` (Histogram), `<Metric name>.max` (ObservableDoubleGauge) |

### Semantic convention differences

The 2 frameworks follow different semantic conventions. The OpenTelemetry Metrics are based on the [OpenTelemetry Semantic Conventions](https://opentelemetry.io/docs/concepts/semantic-conventions/) and are still under active development (early 2025). Micrometer metrics convention format is around for a long time and has not changed much.

When these 2 configurations are set in the `application.properties` file:

```properties
quarkus.micrometer.binder.jvm=true
quarkus.micrometer.binder.http-server.enabled=true
```

The JVM and HTTP server metrics are collected by Micrometer.

Next, are examples of the metrics collected by Micrometer compared with what would be the `quarkus-micrometer-registry-prometheus` endpoint output (`/q/metrics`) vs the OTLP protocol output on this bridge.

A link to the equivalent OpenTelemetry Semantic Convention is also provided for reference and is not currently used by the bridge.

**Micrometer metrics output comparison. Prometheus registry vs. OpenTelemetry bridge**

| Micrometer Meter Java definition | Quarkus Micrometer Prometheus output (as seen at `/q/metrics/`) | This bridge OpenTelemetry output name (as seen in the OTLP output) | Related OpenTelemetry Semantic Convention (not applied) |
| --- | --- | --- | --- |
| Using the [@Timed](/guides/telemetry-micrometer/index.md#create-a-timer) interceptor. |  | method.timed ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)), method.timed.max ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | NA |
| Using the [@Counted](/guides/telemetry-micrometer/index.md#counters) interceptor. |  | method.counted ([DoubleSum](https://opentelemetry.io/docs/specs/otel/metrics/sdk/#sum-aggregation)) | NA |
| `http.server.active.requests` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `http_server_active_requests` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `http.server.active.requests` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`http.server.active_requests`](https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpserveractive_requests) ([UpDownCounter](/guides/opentelemetry-metrics/index.md#counters)) |
| `http.server.requests` (Timer) | `http_server_requests_seconds_count`, `http_server_requests_seconds_sum`, `http_server_requests_seconds_max` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `http.server.requests` ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)), `http.server.requests.max` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`http.server.request.duration`](https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpserverrequestduration) ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)) |
| `http.server.bytes.read` ([DistributionSummary](/guides/telemetry-micrometer/index.md#create-a-distribution-summary)) | `http_server_bytes_read_count`, `http_server_bytes_read_sum` , `http_server_bytes_read_max` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `http.server.bytes.read` ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)), `http.server.bytes.read.max` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`http.server.request.body.size`](https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpserverrequestbodysize) ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)) |
| `http.server.bytes.write` ([DistributionSummary](/guides/telemetry-micrometer/index.md#create-a-distribution-summary)) | `http_server_bytes_write_count`, `http_server_bytes_write_sum` , `http_server_bytes_write_max` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `http.server.bytes.write` ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)), `http.server.bytes.write.max` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`http.server.response.body.size`](https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpserverresponsebodysize) ([Histogram](/guides/opentelemetry-metrics/index.md#histograms)) |
| `http.server.connections` ([LongTaskTimer](https://docs.micrometer.io/micrometer/reference/concepts/long-task-timers.html)) | `http_server_connections_seconds_active_count`, `http_server_connections_seconds_duration_sum` `http_server_connections_seconds_max` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `http.server.connections.active` ([LongSum](https://opentelemetry.io/docs/specs/otel/metrics/sdk/#sum-aggregation)), `http.server.connections.duration` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | N/A |
| `jvm.threads.live` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm_threads_live_threads` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm.threads.live` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`jvm.threads.live`](https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/#metric-jvmthreadcount) ([UpDownCounter](/guides/opentelemetry-metrics/index.md#counters)) |
| `jvm.threads.started` ([FunctionCounter](https://docs.micrometer.io/micrometer/reference/concepts/counters.html#_function_tracking_counters)) | `jvm_threads_started_threads_total` ([Counter](/guides/telemetry-micrometer/index.md#counters)) | `jvm.threads.started` ([DoubleSum](https://opentelemetry.io/docs/specs/otel/metrics/sdk/#sum-aggregation)) | [`jvm.threads.live`](https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/#metric-jvmthreadcount) ([UpDownCounter](/guides/opentelemetry-metrics/index.md#counters)) |
| `jvm.threads.daemon` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm_threads_daemon_threads` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm.threads.daemon` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`jvm.threads.live`](https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/#metric-jvmthreadcount) ([UpDownCounter](/guides/opentelemetry-metrics/index.md#counters)) |
| `jvm.threads.peak` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm_threads_peak_threads` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm.threads.peak` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | N/A |
| `jvm.threads.states` ([Gauge](/guides/telemetry-micrometer/index.md#gauges) per state) | `jvm_threads_states_threads` ([Gauge](/guides/telemetry-micrometer/index.md#gauges)) | `jvm.threads.states` ([DoubleGauge](/guides/opentelemetry-metrics/index.md#gauges)) | [`jvm.threads.live`](https://opentelemetry.io/docs/specs/semconv/runtime/jvm-metrics/#metric-jvmthreadcount) ([UpDownCounter](/guides/opentelemetry-metrics/index.md#counters)) |

> [!NOTE]
> Some metrics might be missing from the output if they contain no data.

## See the output

### Grafana-OTel-LGTM Dev Service

You can use the [Grafana-OTel-LGTM](/guides/observability-devservices-lgtm/index.md) Dev Service.

This Dev Service includes Grafana for visualizing data, Loki to store logs, Tempo to store traces and Prometheus to store metrics. It also provides an OTel collector to receive the data

### Logging exporter

You can output all metrics to the console by setting the exporter to `logging` in the `application.properties` file:

```properties
quarkus.otel.metrics.exporter=logging <1>
quarkus.otel.metric.export.interval=10000ms <2>
```

1. Set the exporter to `logging`. Normally you don't need to set this. The default is `cdi`.
2. Set the interval to export the metrics. The default is `1m`, which is too long for debugging.

Also add this dependency to your project:

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-logging</artifactId>
</dependency>
```
