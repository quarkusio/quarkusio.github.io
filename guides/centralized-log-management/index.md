# Centralized log management (Graylog, Logstash, Fluentd)

This guide explains how you can send your logs to a centralized log management system like Graylog, Logstash (inside the Elastic Stack or ELK - Elasticsearch, Logstash, Kibana) or Fluentd (inside EFK - Elasticsearch, Fluentd, Kibana).

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

There are a lot of different ways to centralize your logs (if you are using Kubernetes, the simplest way is to log to the console and ask your cluster administrator to integrate a central log manager inside your cluster). In this guide, we will expose how to send them to an external tool using supported Quarkus extensions in supported standard formats like Graylog Extended Log Format (GELF), Elastic Common Schema (ECS) or the OpenTelemetry Log signal.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Docker and Docker Compose or [Podman](/guides/podman/index.md), and Docker Compose
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Example application

The following examples will all be based on the same example application that you can create with the following steps.

Create an application with the REST extension. You can use the following command to create it:

**CLI**

```bash
quarkus create app org.acme:centralized-logging \
    --extension='rest' \
    --no-code
cd centralized-logging
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=centralized-logging \
    -Dextensions='rest' \
    -DnoCode
cd centralized-logging
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=centralized-logging"`

For demonstration purposes, we create an endpoint that does nothing but log a sentence. You don't need to do this inside your application.

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;

@Path("/logging")
@ApplicationScoped
public class LoggingResource {
    private static final Logger LOG = Logger.getLogger(LoggingResource.class);

    @GET
    public void log() {
        LOG.info("Some useful log message");
    }

}
```

## Send logs to the Elastic Stack (ELK) in the ECS (Elastic Common Schema) format with the Socket handler

You can send your logs to Logstash using a TCP input in the [ECS](https://www.elastic.co/guide/en/ecs-logging/overview/current/intro.html) format. To achieve this, we will use the `quarkus-logging-json` extension to format the logs in JSON format and the socket handler to send them to Logstash.

Create the following file  in `$HOME/pipelines/ecs.conf`:

```
input {
  tcp {
    port => 4560
    codec => json
  }
}

filter {
  if ![span][id] and [mdc][spanId] {
    mutate { rename => { "[mdc][spanId]" => "[span][id]" } }
  }
  if ![trace][id] and [mdc][traceId] {
    mutate { rename => {"[mdc][traceId]" => "[trace][id]"} }
  }
}

output {
  stdout {}
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
  }
}
```

Then configure your application to log in JSON format

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-logging-json</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-logging-json")
```

and specify the host and port of your Logstash endpoint. To be ECS compliant, specify the log format.

```properties
# to keep the logs in the usual format in the console
quarkus.log.console.json.enabled=false

quarkus.log.socket.enable=true
quarkus.log.socket.json.enabled=true
quarkus.log.socket.endpoint=localhost:4560

# to have the exception serialized into a single text element
quarkus.log.socket.json.exception-output-type=formatted

# specify the format of the produced JSON log
quarkus.log.socket.json.log-format=ECS
```

Finally, launch the components that compose the Elastic Stack:

- Elasticsearch
- Logstash
- Kibana

You can do this via the following `docker-compose.yml` file that you can launch via `docker-compose up -d`:

```yaml
# Launch Elasticsearch
version: '3.2'

services:
  elasticsearch:
    image: docker.io/elastic/elasticsearch:9.4.1
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      discovery.type: "single-node"
      cluster.routing.allocation.disk.threshold_enabled: false
    networks:
      - elk

  logstash:
    image: docker.io/elastic/logstash:9.4.1
    volumes:
      - source: $HOME/pipelines
        target: /usr/share/logstash/pipeline
        type: bind
    ports:
      - "12201:12201/udp"
      - "5000:5000"
      - "9600:9600"
    networks:
      - elk
    depends_on:
      - elasticsearch

  kibana:
    image: docker.io/elastic/kibana:9.4.1
    ports:
      - "5601:5601"
    networks:
      - elk
    depends_on:
      - elasticsearch

networks:
  elk:
    driver: bridge

```

Launch your application, you should see your logs arriving inside the Elastic Stack; you can use Kibana available at <http://localhost:5601/> to access them.

## Send logs to Fluentd with the Syslog handler

You can send your logs to Fluentd using a Syslog input. As opposed to the GELF input, the Syslog input will not render multiline logs in one event.

First, you need to create a Fluentd image with the Elasticsearch plugin. You can use the following Dockerfile that should be created inside a `fluentd` directory.

```dockerfile
FROM fluent/fluentd:v1.3-debian
RUN ["gem", "install", "fluent-plugin-elasticsearch", "--version", "3.7.0"]
```

Then, you need to create a Fluentd configuration file inside `$HOME/fluentd/fluent.conf`

```
<source>
  @type syslog
  port 5140
  bind 0.0.0.0
  message_format rfc5424
  tag system
</source>

<match **>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
</match>
```

Then, launch the components that compose the EFK Stack:

- Elasticsearch
- Fluentd
- Kibana

You can do this via the following `docker-compose.yml` file that you can launch via `docker-compose up -d`:

```yaml
version: '3.2'

services:
  elasticsearch:
    image: docker.io/elastic/elasticsearch:9.4.1
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      discovery.type: "single-node"
      cluster.routing.allocation.disk.threshold_enabled: false
    networks:
      - efk

  fluentd:
    build: fluentd
    ports:
      - "5140:5140/udp"
    volumes:
      - source: $HOME/fluentd
        target: /fluentd/etc
        type: bind
    networks:
      - efk
    depends_on:
      - elasticsearch

  kibana:
    image: docker.io/elastic/kibana:9.4.1
    ports:
      - "5601:5601"
    networks:
      - efk
    depends_on:
      - elasticsearch

networks:
  efk:
    driver: bridge
```

Finally, configure your application to send logs to EFK using Syslog:

```properties
quarkus.log.syslog.enable=true
quarkus.log.syslog.endpoint=localhost:5140
quarkus.log.syslog.protocol=udp
quarkus.log.syslog.app-name=quarkus
quarkus.log.syslog.hostname=quarkus-test
```

Launch your application, you should see your logs arriving inside EFK: you can use Kibana available at <http://localhost:5601/> to access them.

## Send logs with OpenTelemetry Logging

OpenTelemetry Logging is able to send logs to a compatible OpenTelemetry collector. Its usage is described in the guide [Using OpenTelemetry Logging](/guides/opentelemetry-logging/index.md).

## Send logs with the `logging-gelf` extension

> [!WARNING]
> This extension is deprecated, we advise considering the alternatives described above in this guide.

The `quarkus-logging-gelf` extension will add a GELF log handler to the underlying logging backend that Quarkus uses (jboss-logmanager). By default, it is disabled, if you enable it but still use another handler (by default the console handler is enabled), your logs will be sent to both handlers.

You can add the `logging-gelf` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add logging-gelf
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='logging-gelf'
```

**Gradle**

```bash
./gradlew addExtension --extensions='logging-gelf'
```

This will add the following dependency to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-logging-gelf</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-logging-gelf")
```

Configure the GELF log handler to send logs to an external UDP endpoint on port 12201:

```properties
quarkus.log.handler.gelf.enabled=true
quarkus.log.handler.gelf.host=localhost
quarkus.log.handler.gelf.port=12201
```

### Send logs to Graylog

> [!NOTE]
> It is advised to use the Syslog handler instead.

To send logs to Graylog, you first need to launch the components that compose the Graylog stack:

- MongoDB
- Elasticsearch
- Graylog

You can do this via the following `docker-compose.yml` file that you can launch via `docker-compose up -d`:

```yaml
version: '3.2'

services:
  elasticsearch:
    image: docker.io/elastic/elasticsearch:9.4.1
    ports:
      - "9200:9200"
    environment:
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      discovery.type: "single-node"
      cluster.routing.allocation.disk.threshold_enabled: false
    networks:
      - graylog

  mongo:
    image: mongo:4.0
    networks:
      - graylog

  graylog:
    image: graylog/graylog:4.3.0
    ports:
      - "9000:9000"
      - "12201:12201/udp"
      - "1514:1514"
    environment:
      GRAYLOG_HTTP_EXTERNAL_URI: "http://127.0.0.1:9000/"
      # CHANGE ME (must be at least 16 characters)!
      GRAYLOG_PASSWORD_SECRET: "forpasswordencryption"
      # Password: admin
      GRAYLOG_ROOT_PASSWORD_SHA2: "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"
    networks:
      - graylog
    depends_on:
      - elasticsearch
      - mongo

networks:
  graylog:
    driver: bridge
```

Then, you need to create a UDP input in Graylog. You can do it from the Graylog web console (System -> Input -> Select GELF UDP) available at <http://localhost:9000> or via the API.

This curl example will create a new Input of type GELF UDP, it uses the default login from Graylog (admin/admin).

```bash
curl -H "Content-Type: application/json" -H "Authorization: Basic YWRtaW46YWRtaW4=" -H "X-Requested-By: curl" -X POST -v -d \
'{"title":"udp input","configuration":{"recv_buffer_size":262144,"bind_address":"0.0.0.0","port":12201,"decompress_size_limit":8388608},"type":"org.graylog2.inputs.gelf.udp.GELFUDPInput","global":true}' \
http://localhost:9000/api/system/inputs
```

Launch your application, you should see your logs arriving inside Graylog.

### Send logs to Logstash / the Elastic Stack (ELK)

> [!NOTE]
> It is advised to use [OpenTelemetry Logging](/guides/opentelemetry-logging/index.md) or the Socket handler instead.

Logstash comes by default with an Input plugin that can understand the GELF format, we will first create a pipeline that enables this plugin.

Create the following file  in `$HOME/pipelines/gelf.conf`:

```
input {
  gelf {
    port => 12201
  }
}
output {
  stdout {}
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
  }
}
```

Finally, launch the components that compose the Elastic Stack:

- Elasticsearch
- Logstash
- Kibana

You can do this via the following `docker-compose.yml` file that you can launch via `docker-compose up -d`:

```yaml
# Launch Elasticsearch
version: '3.2'

services:
  elasticsearch:
    image: docker.io/elastic/elasticsearch:9.4.1
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      discovery.type: "single-node"
      cluster.routing.allocation.disk.threshold_enabled: false
    networks:
      - elk

  logstash:
    image: docker.io/elastic/logstash:9.4.1
    volumes:
      - source: $HOME/pipelines
        target: /usr/share/logstash/pipeline
        type: bind
    ports:
      - "12201:12201/udp"
      - "5000:5000"
      - "9600:9600"
    networks:
      - elk
    depends_on:
      - elasticsearch

  kibana:
    image: docker.io/elastic/kibana:9.4.1
    ports:
      - "5601:5601"
    networks:
      - elk
    depends_on:
      - elasticsearch

networks:
  elk:
    driver: bridge

```

Launch your application, you should see your logs arriving inside the Elastic Stack; you can use Kibana available at <http://localhost:5601/> to access them.

### Send logs to Fluentd (EFK)

> [!NOTE]
> It is advised to use [OpenTelemetry Logging](/guides/opentelemetry-logging/index.md) or the Socket handler instead.

First, you need to create a Fluentd image with the needed plugins: elasticsearch and input-gelf. You can use the following Dockerfile that should be created inside a `fluentd` directory.

```dockerfile
FROM fluent/fluentd:v1.3-debian
RUN ["gem", "install", "fluent-plugin-elasticsearch", "--version", "3.7.0"]
RUN ["gem", "install", "fluent-plugin-input-gelf", "--version", "0.3.1"]
```

You can build the image or let docker-compose build it for you.

Then you need to create a fluentd configuration file inside `$HOME/fluentd/fluent.conf`

```
<source>
  type gelf
  tag example.gelf
  bind 0.0.0.0
  port 12201
</source>

<match example.gelf>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
</match>
```

Finally, launch the components that compose the EFK Stack:

- Elasticsearch
- Fluentd
- Kibana

You can do this via the following `docker-compose.yml` file that you can launch via `docker-compose up -d`:

```yaml
version: '3.2'

services:
  elasticsearch:
    image: docker.io/elastic/elasticsearch:9.4.1
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      discovery.type: "single-node"
      cluster.routing.allocation.disk.threshold_enabled: false
    networks:
      - efk

  fluentd:
    build: fluentd
    ports:
      - "12201:12201/udp"
    volumes:
      - source: $HOME/fluentd
        target: /fluentd/etc
        type: bind
    networks:
      - efk
    depends_on:
      - elasticsearch

  kibana:
    image: docker.io/elastic/kibana:9.4.1
    ports:
      - "5601:5601"
    networks:
      - efk
    depends_on:
      - elasticsearch

networks:
  efk:
    driver: bridge
```

Launch your application, you should see your logs arriving inside EFK: you can use Kibana available at <http://localhost:5601/> to access them.

### Elasticsearch indexing consideration

Be careful that, by default, Elasticsearch will automatically map unknown fields (if not disabled in the index settings) by detecting their type. This can become tricky if you use log parameters (which are included by default), or if you enable MDC inclusion (disabled by default), as the first log will define the type of the message parameter (or MDC parameter) field inside the index.

Imagine the following case:

```java
LOG.info("some {} message {} with {} param", 1, 2, 3);
LOG.info("other {} message {} with {} param", true, true, true);
```

With log message parameters enabled, the first log message sent to Elasticsearch will have a `MessageParam0` parameter with an `int` type; this will configure the index with a field of type `integer`. When the second message will arrive to Elasticsearch, it will have a `MessageParam0` parameter with the boolean value `true`, and this will generate an indexing error.

To work around this limitation, you can disable sending log message parameters via `logging-gelf` by configuring `quarkus.log.handler.gelf.include-log-message-parameters=false`, or you can configure your Elasticsearch index to store those fields as text or keyword, Elasticsearch will then automatically make the translation from int/boolean to a String.

See the following documentation for Graylog (but the same issue exists for the other central logging stacks): [Custom Index Mappings](https://docs.graylog.org/en/3.2/pages/configuration/elasticsearch.html#custom-index-mappings).

<a id="configuration-reference"></a>
### Configuration Reference

Configuration is done through the usual `application.properties` file.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-enabled"></a> [`quarkus.log.handler.gelf.enabled`](#quarkus-logging-gelf_quarkus-log-handler-gelf-enabled) config_property_copy_button:+++quarkus.log.handler.gelf.enabled+++[]<br><br>Determine whether to enable the GELF logging handler<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-host"></a> [`quarkus.log.handler.gelf.host`](#quarkus-logging-gelf_quarkus-log-handler-gelf-host) config_property_copy_button:+++quarkus.log.handler.gelf.host+++[]<br><br>Hostname/IP-Address of the Logstash/Graylog Host By default it uses UDP, prepend tcp: to the hostname to switch to TCP, example: "tcp:localhost"<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_HOST+++[] | string | `localhost` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-port"></a> [`quarkus.log.handler.gelf.port`](#quarkus-logging-gelf_quarkus-log-handler-gelf-port) config_property_copy_button:+++quarkus.log.handler.gelf.port+++[]<br><br>The port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_PORT+++[] | int | `12201` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-version"></a> [`quarkus.log.handler.gelf.version`](#quarkus-logging-gelf_quarkus-log-handler-gelf-version) config_property_copy_button:+++quarkus.log.handler.gelf.version+++[]<br><br>GELF version: 1.0 or 1.1<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_VERSION+++[] | string | `1.1` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-extract-stack-trace"></a> [`quarkus.log.handler.gelf.extract-stack-trace`](#quarkus-logging-gelf_quarkus-log-handler-gelf-extract-stack-trace) config_property_copy_button:+++quarkus.log.handler.gelf.extract-stack-trace+++[]<br><br>Whether to post Stack-Trace to StackTrace field.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_EXTRACT_STACK_TRACE+++[] | boolean | `true` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-stack-trace-throwable-reference"></a> [`quarkus.log.handler.gelf.stack-trace-throwable-reference`](#quarkus-logging-gelf_quarkus-log-handler-gelf-stack-trace-throwable-reference) config_property_copy_button:+++quarkus.log.handler.gelf.stack-trace-throwable-reference+++[]<br><br>Only used when `extractStackTrace` is `true`. A value of 0 will extract the whole stack trace. Any positive value will walk the cause chain: 1 corresponds with exception.getCause(), 2 with exception.getCause().getCause(), ... Negative throwable reference walk the exception chain from the root cause side: -1 will extract the root cause, -2 the exception wrapping the root cause, ...<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_STACK_TRACE_THROWABLE_REFERENCE+++[] | int | `0` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-filter-stack-trace"></a> [`quarkus.log.handler.gelf.filter-stack-trace`](#quarkus-logging-gelf_quarkus-log-handler-gelf-filter-stack-trace) config_property_copy_button:+++quarkus.log.handler.gelf.filter-stack-trace+++[]<br><br>Whether to perform Stack-Trace filtering<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_FILTER_STACK_TRACE+++[] | boolean | `false` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-timestamp-pattern"></a> [`quarkus.log.handler.gelf.timestamp-pattern`](#quarkus-logging-gelf_quarkus-log-handler-gelf-timestamp-pattern) config_property_copy_button:+++quarkus.log.handler.gelf.timestamp-pattern+++[]<br><br>Java date pattern, see `java.text.SimpleDateFormat`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_TIMESTAMP_PATTERN+++[] | string | `yyyy-MM-dd HH:mm:ss,SSS` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-level"></a> [`quarkus.log.handler.gelf.level`](#quarkus-logging-gelf_quarkus-log-handler-gelf-level) config_property_copy_button:+++quarkus.log.handler.gelf.level+++[]<br><br>The logging-gelf log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-facility"></a> [`quarkus.log.handler.gelf.facility`](#quarkus-logging-gelf_quarkus-log-handler-gelf-facility) config_property_copy_button:+++quarkus.log.handler.gelf.facility+++[]<br><br>Name of the facility.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_FACILITY+++[] | string | `jboss-logmanager` |
| <a id="quarkus-logging-gelf_section_quarkus-log-handler-gelf-additional-field"></a> [Post additional fields](#quarkus-logging-gelf_section_quarkus-log-handler-gelf-additional-field) | Type | Default |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-additional-field-field-name-value"></a> [`quarkus.log.handler.gelf.additional-field."field-name".value`](#quarkus-logging-gelf_quarkus-log-handler-gelf-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.handler.gelf.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-additional-field-field-name-type"></a> [`quarkus.log.handler.gelf.additional-field."field-name".type`](#quarkus-logging-gelf_quarkus-log-handler-gelf-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.handler.gelf.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: String, long, Long, double, Double and discover. Discover is the default if not specified, it discovers field type based on parseability.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | string | `discover` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-include-full-mdc"></a> [`quarkus.log.handler.gelf.include-full-mdc`](#quarkus-logging-gelf_quarkus-log-handler-gelf-include-full-mdc) config_property_copy_button:+++quarkus.log.handler.gelf.include-full-mdc+++[]<br><br>Whether to include all fields from the MDC.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_INCLUDE_FULL_MDC+++[] | boolean | `false` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-mdc-fields"></a> [`quarkus.log.handler.gelf.mdc-fields`](#quarkus-logging-gelf_quarkus-log-handler-gelf-mdc-fields) config_property_copy_button:+++quarkus.log.handler.gelf.mdc-fields+++[]<br><br>Send additional fields whose values are obtained from MDC. Name of the Fields are comma-separated. Example: mdcFields=Application,Version,SomeOtherFieldName<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_MDC_FIELDS+++[] | string |  |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-dynamic-mdc-fields"></a> [`quarkus.log.handler.gelf.dynamic-mdc-fields`](#quarkus-logging-gelf_quarkus-log-handler-gelf-dynamic-mdc-fields) config_property_copy_button:+++quarkus.log.handler.gelf.dynamic-mdc-fields+++[]<br><br>Dynamic MDC Fields allows you to extract MDC values based on one or more regular expressions. Multiple regexes are comma-separated. The name of the MDC entry is used as GELF field name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_DYNAMIC_MDC_FIELDS+++[] | string |  |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-dynamic-mdc-field-types"></a> [`quarkus.log.handler.gelf.dynamic-mdc-field-types`](#quarkus-logging-gelf_quarkus-log-handler-gelf-dynamic-mdc-field-types) config_property_copy_button:+++quarkus.log.handler.gelf.dynamic-mdc-field-types+++[]<br><br>Pattern-based type specification for additional and MDC fields. Key-value pairs are comma-separated. Example: my_field.++**++=String,business++ ++..++**++.field=double<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_DYNAMIC_MDC_FIELD_TYPES+++[] | string |  |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-maximum-message-size"></a> [`quarkus.log.handler.gelf.maximum-message-size`](#quarkus-logging-gelf_quarkus-log-handler-gelf-maximum-message-size) config_property_copy_button:+++quarkus.log.handler.gelf.maximum-message-size+++[]<br><br>Maximum message size (in bytes). If the message size is exceeded, the appender will submit the message in multiple chunks.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_MAXIMUM_MESSAGE_SIZE+++[] | int | `8192` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-include-log-message-parameters"></a> [`quarkus.log.handler.gelf.include-log-message-parameters`](#quarkus-logging-gelf_quarkus-log-handler-gelf-include-log-message-parameters) config_property_copy_button:+++quarkus.log.handler.gelf.include-log-message-parameters+++[]<br><br>Include message parameters from the log event<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_INCLUDE_LOG_MESSAGE_PARAMETERS+++[] | boolean | `true` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-include-location"></a> [`quarkus.log.handler.gelf.include-location`](#quarkus-logging-gelf_quarkus-log-handler-gelf-include-location) config_property_copy_button:+++quarkus.log.handler.gelf.include-location+++[]<br><br>Include source code location<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_INCLUDE_LOCATION+++[] | boolean | `true` |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-origin-host"></a> [`quarkus.log.handler.gelf.origin-host`](#quarkus-logging-gelf_quarkus-log-handler-gelf-origin-host) config_property_copy_button:+++quarkus.log.handler.gelf.origin-host+++[]<br><br>Origin hostname<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_ORIGIN_HOST+++[] | string |  |
| <a id="quarkus-logging-gelf_quarkus-log-handler-gelf-skip-hostname-resolution"></a> [`quarkus.log.handler.gelf.skip-hostname-resolution`](#quarkus-logging-gelf_quarkus-log-handler-gelf-skip-hostname-resolution) config_property_copy_button:+++quarkus.log.handler.gelf.skip-hostname-resolution+++[]<br><br>Bypass hostname resolution. If you didn't set the `originHost` property, and resolution is disabled, the value “unknown” will be used as hostname<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_GELF_SKIP_HOSTNAME_RESOLUTION+++[] | boolean | `false` |

This extension uses the `logstash-gelf` library that allow more configuration options via system properties, you can access its documentation here: <https://logging.paluch.biz/>.
