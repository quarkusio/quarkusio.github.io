# Quarkus Micrometer using Prometheus client v1

## Quarkus Micrometer using Prometheus client v1

Micrometer adopted Prometheus Client v1.x starting with version 1.13. However, Quarkus still uses Prometheus Client v0.x by default through the `quarkus-micrometer-registry-prometheus` extension which has been using the legacy `io.prometheus:simpleclient`. This is now considered **deprecated** and will not be part of the future Quarkus 4.x version (still no timeline).

In parallel, we have introduced a new extension in Quarkiverse: [`quarkus-micrometer-registry-prometheus-v1`](https://docs.quarkiverse.io/quarkus-micrometer-registry/dev/micrometer-registry-prometheus-v1.html), which integrates the new v1.x client.

```xml <dependency> <groupId>io.quarkiverse.micrometer.registry</groupId> <artifactId>quarkus-micrometer-registry-prometheus-v1</artifactId> </dependency> ```

Since the new client introduces **breaking changes**, we’ve prepared a migration plan designed to reduce the impact on existing users.

Some of the changes affect the names of some metrics. This requires queries and dashboards to be updated in order to continue working. Users should evaluate the impacts for their projects before moving to the new `quarkus-micrometer-registry-prometheus-v1` extension.

> [!WARNING]
> The old and the new extension should not be used at the same time as the both require the `/q/metrics` endpoint.

### Migrating to the new extension

1. Users can keep current extension as is until Quarkus 4.0 allowing them to migrate at a time of their choice.
2. We are introducing the new extension [`quarkus-micrometer-registry-prometheus-v1`](https://docs.quarkiverse.io/quarkus-micrometer-registry/dev/micrometer-registry-prometheus-v1.html) and users can start migrating their projects to it right now.
3. For Quarkus 4.0 we will introduce the following changes: \*\* The deprecated `quarkus-micrometer-registry-prometheus` extension will be moved from Quarkus core repo to Quarkiverse, allowing an additional migration period. \*\* The new [`quarkus-micrometer-registry-prometheus-v1`](https://docs.quarkiverse.io/quarkus-micrometer-registry/dev/micrometer-registry-prometheus-v1.html) extension will be moved from Quarkiverse to Quarkus core repo, making it the default registry.

### Breaking changes

As mentioned above, the new extension comes with a set of breaking changes.

#### Prometheus client v1

The `quarkus-micrometer-registry-prometheus-v1` extension will be using the Prometheus Client v1.x and includes **breaking changes** expressed in detail on the [Micrometer 1.13 Migration Guide](https://github.com/micrometer-metrics/micrometer/wiki/1.13-Migration-Guide), featuring package, API and semantic convention changes.

These are the main points to consider when migrating to the new Quarkus extension:

- Counters now use Longs instead of Doubles.
  - Before: `"registry=\"prometheus\",status=\"200\",uri=\"/example/prime/{number}\"} 2.0`
  - Now: `"registry=\"prometheus\",status=\"200\",uri=\"/example/prime/{number}\"} 2`
- `io.micrometer.prometheusmetrics.PrometheusMeterRegistry` must be used instead of the old `io.micrometer.prometheus.PrometheusMeterRegistry`.
- `io.prometheus.metrics.tracer.common.SpanContext` must be used instead of the old `io.prometheus.client.exemplars.ExemplarSampler`
- The new Prometheus client uses some reserved words that must not be used to name metrics, therefore some metrics were renamed. Some examples of reserved words and renamed metrics:
  - `info`. Before: `jvm_info_total`. Now: `jvm_total`
  - `duration`. Before: `http_server_requests_duration_seconds`. Now: `http_server_requests_seconds`
- Some metrics would display Tags ending in a comma (`,`). This tailing comma has now been removed. Example: `hibernate_flushes_total{entityManagerFactory=\"<default>\",env=\"test\",env2=\"test\",registry=\"prometheus\",} 1.0`
- It is no longer possible to create a metric generating Service Level Objectives and Percentiles at the same time.

#### Change when registering metrics:

Metrics must be registered always with the same tags. Micrometer will now send a warning if we register the same metric more than once with different Tag names.

#### Implementation changes

Quarkus automatic instrumentation will generate metrics reflecting these changes. If your application defines custom metrics, be sure to update their creation, associated tests, and any dashboard queries to align with the guidelines above.

### About metrics in Quarkus

The [Micrometer extension](https://quarkus.io/guides/telemetry-micrometer) is the recommended approach to generate metrics in Quarkus and this a rare occasions were breaking changes are being introduced.

Micrometer has become the default metrics framework in Quarkus due to its stability, maturity, and widespread adoption on the Java ecosystem — not [OpenTelemetry Metrics](https://quarkus.io/guides/opentelemetry-metrics).

Micrometer integration in Quarkus is provided through the core quarkus-micrometer` extension. Specific registry extensions depend on this core extension and implement their own registries to export telemetry data. The Prometheus registry (`quarkus-micrometer-registry-prometheus\`) is the default and most commonly used, and it is the focus of this announcement.

However, there are alternative ways to export metrics using Micrometer:

- Through the [Quarkiverse Micrometer registries](https://github.com/quarkiverse/quarkus-micrometer-registry/)
- Via OpenTelemetry, using a bridge provided by the [Micrometer and OpenTelemetry](https://quarkus.io/guides/telemetry-micrometer-to-opentelemetry) extension. This setup allows Micrometer metrics to be sent as OpenTelemetry metrics, offering a unified output via the [OpenTelemetry OTLP protocol](https://opentelemetry.io/docs/specs/otlp/).

For more details on Observability in Quarkus please visit [this guide](https://quarkus.io/guides/observability).

### Conclusion

While the existing Prometheus registry remains available for the Quarkus 3.x lifecycle, it is now deprecated, and users are encouraged to begin migrating to the new extension because the old client extension will be removed on Quarkus 4.0 (no timeline at this moment).

This change introduces a number of breaking updates aligned with Micrometer 1.13 and Prometheus Client v1.x, affecting metric types, naming conventions, and APIs.

To support users, a migration plan has been outlined to provide flexibility and minimize disruption.

The prometheus client is not the only option to send out telemetry, there are alternative options such as the Quarkiverse registries or our OpenTelemetry bridge.
