# Observability in Quarkus

Observability can be defined as the capability to allow a human to ask and answer questions about a system.

Over the time, many extensions have been developed to provide observability capabilities to Quarkus applications. This guide will provide an overview of the observability extensions available in Quarkus and which can be used to provide observability according to the needs of your Quarkus application.

## Telemetry

Telemetry contains data about the internal state of the system and can be split into categories or **signals**:

- **Logging**, classical local or centralised logs
- **Metrics**, metrics calculated in each Quarkus application instance
- **Tracing**, distributed tracing across applications
- **Profiling**, to analyze and monitor the application's performance, resource usage, and runtime behavior
- **Events**, the most generic signal. It's a representation of something significant happening during the execution of a program.

## Guidelines

There is only one important guideline in Quarkus observability:

- The [OpenTelemetry OTLP protocol](https://opentelemetry.io/docs/specs/otel/protocol/) is the recommended way to send telemetry out of a Quarkus application. This provides a unified output for the application's telemetry.

## Overview

Some extensions have overlapping functionality and there are recommended extensions for each type of signal.

The following table provides an overview of the observability extensions available in Quarkus, the signals they provide and which is the recommended extension for each signal.

| Extension | Logging | Metrics | Tracing | Profiling | Health Check | Events |
| --- | --- | --- | --- | --- | --- | --- |
| [quarkus-opentelemetry](/guides/opentelemetry/index.md) | O, R | O, R | R |  |  | X |
| [quarkus-micrometer](/guides/telemetry-micrometer/index.md) |  | R |  |  |  |  |
| [quarkus-micrometer-opentelemetry](/guides/telemetry-micrometer-to-opentelemetry/index.md) | X | R | R |  |  | X |
| [quarkus-jfr](/guides/jfr/index.md) |  |  |  | R |  | X |
| [Logging in Quarkus](/guides/logging/index.md) | X |  |  |  |  |  |
| [quarkus-logging-json](/guides/logging/index.md#json-logging) | X |  |  |  |  |  |
| [quarkus-logging-gelf](/guides/centralized-log-management/index.md) | D |  |  |  |  |  |

- **R** - recommended
- **X** - supports
- **D** - deprecated
- **O** - off by default

## The signals

### Logging and events

We can say a log line is a type of event that includes a severity classification. The OpenTelemetry project recognizes this approach, resulting in an OpenTelemetry API for logs and events that is pretty much the same.

Log is a much older concept and is still widely used in the industry, that's why the overview table from above has different columns for logs and events.

In the future we'll see a convergence of these two concepts, as people start adopting OpenTelemetry Logs.

#### Quarkus logging extensions

Quarkus uses the **JBoss Log Manager** logging backend to publish application and framework logs either in the console or files. These logs can also be forwarded to a centralized logging system, either by using the legacy [`quarkus-logging-gelf`](/guides/centralized-log-management/index.md) extension or the recommended [`quarkus-opentelemetry`](/guides/opentelemetry/index.md) or [`quarkus-micrometer-opentelemetry`](/guides/telemetry-micrometer-to-opentelemetry/index.md) extensions.

OpenTelemetry logs are disabled by default in the `quarkus-opentelemetry` extension, but enabled by default in the `quarkus-micrometer-opentelemetry` extension.

#### Other events

There is no recommended way to generate observability events in Quarkus because the OpenTelemetry Events API is still under development and its use is not recommended, yet.

The [`quarkus-jfr`](/guides/jfr/index.md) extension can generate observability related events.

### Metrics

Quarkus has been using Micrometer to collect metrics from the application for a long time. Almost all the out-of-the-box metrics instrumentation in Quarkus are implemented with the [Micrometer](/guides/telemetry-micrometer/index.md) extension.

More recently, OpenTelemetry Metrics has become available in the [`quarkus-opentelemetry`](/guides/opentelemetry/index.md) extension, but it's disabled by default because metrics semantic conventions are not stable yet.

The solution is to use the [`quarkus-micrometer-opentelemetry`](/guides/telemetry-micrometer-to-opentelemetry/index.md) extension to allow using **Micrometer metrics** and **OpenTelemetry logs and traces** at the same time with a unified output using the OTLP protocol. Manual OpenTelemetry metrics can also be created with this extension and all signals are enabled by default, for convenience.

### Tracing

Quarkus uses OpenTelemetry Tracing to provide tracing capabilities to the application. The [`quarkus-opentelemetry`](/guides/opentelemetry/index.md) and the [`quarkus-micrometer-opentelemetry`](/guides/telemetry-micrometer-to-opentelemetry/index.md) extensions are the recommended way to use for tracing.

### Profiling

Quarkus uses the Java Flight Recorder (JFR) to provide profiling capabilities to the application. The [`quarkus-jfr`](/guides/jfr/index.md) extension is the recommended way to generate the events necessary to profile the application.

The OpenTelemetry profiling signal, still under development, might be available in the future.

## See telemetry

The [Grafana LGTM Dev Service](/guides/observability-devservices-lgtm/index.md) extension is available to visualize the telemetry data for logs, metrics and traces in Grafana.
