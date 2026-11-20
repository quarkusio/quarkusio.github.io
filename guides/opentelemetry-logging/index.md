# Using OpenTelemetry Logging

This guide explains how your Quarkus application can utilize [OpenTelemetry](https://opentelemetry.io/) (OTel) to provide structured, contextual, vendor-neutral and centralised logging for interactive web applications.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

> [!NOTE]
> - OpenTelemetry Logging is considered *tech preview* and is disabled by default.
> - The [OpenTelemetry Guide](/guides/opentelemetry/index.md) is available with signal independent information about the OpenTelemetry extension.

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

In this guide, we create a straightforward REST application to demonstrate OTel logging, in a similar way to the other OpenTelemetry signal guides.

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

If you have followed the tracing guide, this class will seem familiar. The main difference is that now, the `hello` message logged with `org.jboss.logging.Logger` will end up in the OpenTelemetry logs.

### Create the configuration

The only mandatory configuration for OpenTelemetry Logging is the one enabling it:

```properties
quarkus.otel.logs.enabled=true
```

To change any of the default property values, here is an example on how to configure the default OTLP gRPC Exporter within the application, using the `src/main/resources/application.properties` file:

```properties
quarkus.application.name=myservice // <1>
quarkus.otel.logs.enabled=true // <2>
quarkus.otel.exporter.otlp.logs.endpoint=http://localhost:4317 // <3>
quarkus.otel.exporter.otlp.logs.headers=authorization=Bearer my_secret // <4>
```

1. All logs created from the application will include an OpenTelemetry `Resource` indicating the logs were created by the `myservice` application. If not set, it will default to the artifact id.
2. Enable the OpenTelemetry logging. Must be set at build time.
3. gRPC endpoint to send the logs. If not set, it will default to `http://localhost:4317`.
4. Optional gRPC headers commonly used for authentication.

To configure the connection using the same properties for all signals, please check the base [configuration section of the OpenTelemetry guide](/guides/opentelemetry/index.md#create-the-configuration).

#### Setting the log level

By default all log levels are exported.

If the following configuration is created in the `application.properties` file, only logs with a level of `ERROR` or higher will be exported:

```properties
quarkus.otel.logs.level=ERROR
```

As in other logs in Quarkus, log levels can be set to [these values](/guides/logging/index.md#use-log-levels).

## Run the application

First we need to start a system to visualise the OpenTelemetry data. We have 2 options:

- Start an all-in-one Grafana OTel LGTM system for traces, metrics and logs.

### See the data

#### Grafana OTel LGTM option

A Dev Service will receive your app's telemetry.

The Grafana-OTel-LGTM Dev Service will start automatically on Dev Mode and data will be automatically sent to it.

- Take a look at: [Getting Started with Grafana-OTel-LGTM](/guides/observability-devservices-lgtm/index.md).

This features a Quarkus Dev service including a Grafana for visualizing data, Loki to store logs, Tempo to store traces and Prometheus to store metrics. Also provides and OTel collector to receive the data.

#### Logging exporter

You can output all logs to the console by setting the exporter to `logging` in the `application.properties` file:

```properties
quarkus.otel.logs.exporter=logging <1>
```

1. Set the exporter to `logging`. Normally you don't need to set this. The default is `cdi`.

Also add this dependency to your project:

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-logging</artifactId>
</dependency>
```

<a id="configuration-reference"></a>
## OpenTelemetry Configuration Reference

See the main [OpenTelemetry Guide configuration](/guides/opentelemetry/index.md#configuration-reference) reference.
