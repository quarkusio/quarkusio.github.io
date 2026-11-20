# Logging configuration

Read about the use of logging API in Quarkus, configuring logging output, and using logging adapters to unify the output from other logging APIs.

This document is part of the [Observability in Quarkus reference guide](/guides/observability/index.md) which features this and other observability related components.

Quarkus uses the JBoss Log Manager logging backend for publishing application and framework logs. Quarkus supports the JBoss Logging API and multiple other logging APIs, seamlessly integrated with JBoss Log Manager.

This means you can use your favorite logging API with Quarkus, provided you add the [appropriate adapter](#logging-apis) to your application:

| Logging API | Adapter |
| --- | --- |
| [JBoss Logging](https://github.com/jboss-logging/jboss-logging) | Built-in |
| [JDK `java.util.logging` (JUL)](https://docs.oracle.com/en/java/javase/17/docs/api/java.logging/java/util/logging/package-summary.html) | Built-in |
| [SLF4J](https://www.slf4j.org/) | [SLF4J adapter](#logging-adapters-slf4j) |
| [Apache Commons Logging](https://commons.apache.org/proper/commons-logging/) | [Commons Logging adapter](#logging-adapters-commons-logging) |
| [Apache Log4j 2](https://logging.apache.org/log4j/2.x/) | [Log4j 2 adapter](#logging-adapters-log4j2) |

<a id="jboss-logging"></a>
## Use JBoss Logging for application logging

When using the JBoss Logging API, your application requires no additional dependencies, as Quarkus provides it automatically.

**An example of using the JBoss Logging API to log a message:**

```java
import org.jboss.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

    private static final Logger LOG = Logger.getLogger(ExampleResource.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        LOG.info("Hello");
        return "hello";
    }
}
```

> [!NOTE]
> While JBoss Logging routes log messages directly to the JBoss Log Manager, one of your libraries might rely on a different logging API. In such cases, you need to use a [logging adapter](#logging-apis) to ensure that its log messages are routed to JBoss Log Manager as well.

## Get an application logger

To get an application logger in Quarkus, select one of the following approaches.

- [Declaring a logger field](#declaring-a-logger-field)
- [Simplified logging](#simplified-logging)
- [Injecting a configured logger](#injection-of-a-configured-logger)

<a id="declaring-a-logger-field"></a>
### Declaring a logger field

With this classic approach, you use a specific API to obtain a logger instance, store it in a static field of a class, and call logging operations upon this instance.

The same flow can be applied with any of the [supported logging APIs](#logging-apis).

**An example of storing a logger instance into a static field by using the JBoss Logging API:**

```java
package com.example;

import org.jboss.logging.Logger;

public class MyService {
    private static final Logger log = Logger.getLogger(MyService.class); <1>

    public void doSomething() {
        log.info("It works!"); <2>
    }
}
```

1. Define the logger field.
2. Invoke the desired logging methods on the `log` object.

> [!WARNING]
> When you create a `Logger` instance, Quarkus keeps metadata associated with the `Logger` name in memory for the full lifecycle of the application.
>
> Do not use an unbounded dynamic name for loggers, for example, `for (int i ...) { Logger.getLogger("logger-name." + i).info("my log") }`.

<a id="simplified-logging"></a>
### Simplified logging

Quarkus simplifies logging by automatically adding logger fields to classes that use `io.quarkus.logging.Log`. This eliminates the need for repetitive boilerplate code and enhances the convenience of logging setup.

**An example of simplified logging with static method calls:**

```java
package com.example;

import io.quarkus.logging.Log; // <1>

class MyService { // <2>
    public void doSomething() {
        Log.info("Simple!"); // <3>
    }
}
```

1. The io.quarkus.logging.Log class provides the same methods as JBoss Logging, but as static methods.
2. The class does not declare a logger field. During the application build, Quarkus creates a `private static final org.jboss.logging.Logger` field in each class that uses the `Log` API. Quarkus uses the fully qualified name of the calling class as the logger name. In this example, the logger name is `com.example.MyService`.
3. During the application build, Quarkus rewrites calls to `Log` methods into JBoss Logging calls on the generated logger field.

<a id="log-api-extension-warning"></a>

> [!WARNING]
> Use the `Log` API only in application classes, not in external dependencies. `Log` method calls not processed by Quarkus at build time will result in an exception.
>
> While the `Log` API simplifies logging in application classes, do not use it in extension modules or external dependencies.
>
> The following considerations apply:
>
> - `io.quarkus.logging.Log` depends on Quarkus bytecode transformations that occur at build time.
>
> - In extension modules, `Log` works only if the module includes a Jandex index. However, this behavior is unsupported and can lead to unreliable logging.
>
>   For extension development, use `org.jboss.logging.Logger.getLogger(String)` instead of `io.quarkus.logging.Log`.

<a id="injection-of-a-configured-logger"></a>
### Injecting a configured logger

Instead of declaring a logger field, you can use an alternative approach and inject a configured `org.jboss.logging.Logger` instance by using `@Inject`. This approach applies only to CDI beans.

To control the logger name, choose one of the following injection patterns:

- Use `@Inject Logger log` when you want Quarkus to name the logger after the class it is injected into.
- Use `@LoggerName("...") Logger log` when you want to set the logger name explicitly.

> [!NOTE]
> `@Inject` is not required if you already annotated your `Logger` with `@LoggerName("...")`.

Once injected, you can use the `log` object to invoke logging methods.

**An example of two different types of logger injection:**

```java
package com.example;

import org.jboss.logging.Logger;

@ApplicationScoped
class SimpleBean {

   @Inject
   Logger log; <1>

   @LoggerName("foo")
   Logger fooLog; <2>

   public void ping() {
     log.info("Simple!");
     fooLog.info("Goes to _foo_ logger!");
   }
}
```

1. The logger is created with `org.jboss.logging.Logger.getLogger(SimpleBean.class)`.
2. In this case, the name "foo" is used as a logger name. The logger is created with `org.jboss.logging.Logger.getLogger("foo")`.

> [!NOTE]
> Quarkus caches logger instances internally. When you inject a logger into a `@RequestScoped` bean, Quarkus shares that logger across bean instances to avoid performance penalties from repeated logger instantiation.

## Use log levels

Quarkus provides multiple log levels to control the amount of information logged based on event severity.

**Available log levels:**

**OFF**\
A special level used in configuration to turn off logging.

**FATAL**\
A critical service failure or total inability to handle any requests.

**ERROR**\
A major issue in processing or an inability to complete a request.

**WARN**\
A non-critical service error or problem that might not require immediate correction.

**INFO**\
Service lifecycle events or other important infrequent information.

**DEBUG**\
Additional information about lifecycle events or events not tied to specific requests, useful for debugging.

**TRACE**\
Detailed per-request debugging information, potentially at a very high frequency.

**ALL**\
A special level to turn on logging for all messages, including custom levels.

You can also configure the following levels for applications and libraries that use [`java.util.logging`](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Level.html):

**SEVERE**\
Same as **ERROR**.

**WARNING**\
Same as **WARN**.

**CONFIG**\
Service configuration information.

**FINE**\
Same as **DEBUG**.

**FINER**\
Same as **TRACE**.

**FINEST**\
Increased debug output compared to `TRACE`, which might have a higher frequency.

**The mapping between the levels**

| Numerical level value | Standard level name | Equivalent `java.util.logging` (JUL) level name |
| --- | --- | --- |
| 1100 | FATAL | Not applicable |
| 1000 | ERROR | SEVERE |
| 900 | WARN | WARNING |
| 800 | INFO | INFO |
| 700 | Not applicable | CONFIG |
| 500 | DEBUG | FINE |
| 400 | TRACE | FINER |
| 300 | Not applicable | FINEST |

## Configure the log level, category, and format

JBoss Logging, integrated into Quarkus, provides a unified configuration for all [supported logging APIs](#logging-apis) through a single configuration file that sets up all available extensions.

To adjust runtime logging, modify the `application.properties` file.

**An example of how you can set the default log level to `INFO` logging and include Hibernate `DEBUG` logs:**

```properties
quarkus.log.level=INFO
quarkus.log.category."org.hibernate".level=DEBUG
```

Assuming the [Quarkus YAML extension](https://quarkus.io/guides/config-yaml) is included, the equivalent YAML configuration is:

```yaml
quarkus:
  log:
    level: INFO
    category:
      org.hibernate: # Using org.hibernate as a single key is required
        level: DEBUG
```

When you set the log level to below `DEBUG`, for example `TRACE`, you must also adjust the minimum log level.

Set the minimum log level globally with the `quarkus.log.min-level` configuration property, or set it per category:

```properties
quarkus.log.category."org.hibernate".min-level=TRACE
```

The minimum log level is a build-time configuration option that sets the floor level for which Quarkus generates supporting code. This enables further build-time optimizations, such as dead code elimination when building a native image.

Setting `INFO` as the minimum log level forces checks for lower levels, such as `isTraceEnabled()`, always to return `false`.

In turn, code such as `if (logger.isDebugEnabled()) callMethod();` is identified as never executed, also known as dead code.

> [!WARNING]
> If you add these properties on the command line, ensure the `"` character is escaped with a backslash:
>
> ```properties
> -Dquarkus.log.category.\"org.hibernate\".level=TRACE
> ```

All potential properties are listed in the [logging configuration reference](#loggingConfigurationReference) section.

### Logging categories

Logging is configured per category. Configuration for a category applies recursively to all subcategories unless a more specific subcategory configuration exists.

The parent of all logging categories is called the "root category." As the ultimate parent, this category might contain a configuration that applies globally to all other categories. This includes the globally configured handlers and formatters.

**An example of a global configuration that applies to all categories:**

```properties
quarkus.log.handlers=con,mylog

quarkus.log.handler.console.con.enable=true
quarkus.log.handler.file.mylog.enable=true
```

In this example, the root category is configured to use two named handlers: `con` and `mylog`.

**An example of a per-category configuration:**

```properties
quarkus.log.category."org.apache.kafka.clients".level=INFO
quarkus.log.category."org.apache.kafka.common.utils".level=INFO
```

This example shows how to configure the minimal log level for the categories `org.apache.kafka.clients` and `org.apache.kafka.common.utils`.

For more information, see [Logging configuration reference](#loggingConfigurationReference).

If you want to configure something extra for a specific category, create a named handler like `quarkus.log.handler.[console|file|syslog].<your-handler-name>.*` and set it up for that category by using `quarkus.log.category.<my-category>.handlers`.

An example use case can be a desire to use a different timestamp format for log messages that are saved to a file than the format used for other handlers.

For further demonstration, see the outputs of the [Attaching named handlers to a category](#category-named-handlers-example) example.

| `Property Name` | `Default` | Description |
| --- | --- | --- |
| `quarkus.log.category."<category-name>".level` | `INFO footnote:[Some extensions might define customized default log levels for certain categories to reduce log noise by default.<br>Setting the log level in configuration will override any extension-defined log levels.]` | The level to use to configure the category named `<category-name>`. The quotes are necessary. |
| `quarkus.log.category."<category-name>".min-level` | `DEBUG` | The minimum logging level to use to configure the category named `<category-name>`. The quotes are necessary. |
| `quarkus.log.category."<category-name>".use-parent-handlers` | `true` | Specify whether this logger should send its output to its parent logger. |
| `quarkus.log.category."<category-name>".handlers=[<handler>]` | `empty footnote:[By default, the configured category gets the same handlers attached as the one on the root logger.]` | The names of the handlers that you want to attach to a specific category. |

> [!NOTE]
> The `.` symbol separates the specific parts in the configuration property. The quotes in the property name are used as a required escape to keep category specifications, such as `quarkus.log.category."io.quarkus.smallrye.jwt".level=TRACE`, intact.

### Root logger configuration

The root logger category is handled separately, and is configured by using the following properties:

| `Property Name` | `Default` | Description |
| --- | --- | --- |
| `quarkus.log.level` | `INFO` | The default log level for every log category. |
| `quarkus.log.min-level` | `DEBUG` | The default minimum log level for every log category. |

- The parent category is examined if no level configuration exists for a given logger category.
- The root logger configuration is used if no specific configurations are provided for the category and any of its parent categories.

> [!NOTE]
> Although the root logger's handlers are usually configured directly via `quarkus.log.console`, `quarkus.log.file` and `quarkus.log.syslog`, it can nonetheless have additional named handlers attached to it using the `quarkus.log.handlers` property.

## Logging format

### Human-readable text

Quarkus uses a pattern-based logging formatter that generates human-readable text logs by default, but you can also configure the format for each log handler by using a dedicated property.

For the console handler, the property is `quarkus.log.console.format`.

The logging format string supports the following symbols:

| `Symbol` | Summary | Description |
| --- | --- | --- |
| `%%` | `%` | Renders a simple `%` character. |
| `%c` | Category | Renders the category name. |
| `%C` | Source class | Renders the source class name.[^calc] |
| `%d{xxx}` | Date | Renders a date with the given date format string, which uses the syntax defined by `java.text.SimpleDateFormat`. |
| `%e` | Exception | Renders the thrown exception, if any. |
| `%F` | Source file | Renders the source file name.[^calc] |
| `%h` | Host name | Renders the system simple host name. |
| `%H` | Qualified host name | Renders the system's fully qualified host name, which might be the same as the simple host name, depending on operating system configuration. |
| `%i` | Process ID | Render the current process PID. |
| `%l` | Source location | Renders the source location information, which includes source file name, line number, class name, and method name.[^calc] |
| `%L` | Source line | Renders the source line number.[^calc] |
| `%m` | Full Message | Renders the log message plus exception (if any). |
| `%M` | Source method | Renders the source method name.[^calc] |
| `%n` | Newline | Renders the platform-specific line separator string. |
| `%N` | Process name | Render the name of the current process. |
| `%p` | Level | Render the log level of the message. |
| `%r` | Relative time | Render the time in milliseconds since the start of the application log. |
| `%s` | Simple message | Renders just the log message, with no exception trace. |
| `%t` | Thread name | Render the thread name. |
| `%t{id}` | Thread ID | Render the thread ID. |
| `%z{<zone name>}` | Time zone | Set the time zone of the output to `<zone name>`. |
| `%X{<MDC property name>}` | Mapped Diagnostic Context Value | Renders the value from Mapped Diagnostic Context. |
| `%X` | Mapped Diagnostic Context Values | Renders all the values from Mapped Diagnostic Context in format `{property.key=property.value}`. |
| `%x` | Nested Diagnostics context values | Renders all the values from Nested Diagnostics Context in format `{value1.value2}`. |

<a id="json-logging"></a>
### JSON logging format

Add the `quarkus-logging-json` extension to enable the JSON logging format and its related configuration.

1. Add this extension to your build file as the following snippet illustrates:

   .pom.xml

   ```
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-logging-json</artifactId>
   </dependency>
   ```

   .build.gradle

   ```
   implementation("io.quarkus:quarkus-logging-json")
   ```

   By default, this extension overrides the console output format configuration, and any format string or color settings are ignored. Other console configuration items continue to apply, including items that control asynchronous logging and the log level.

   If you prefer human-readable, unstructured logging in dev mode and JSON-structured logging in production mode, configure this by using profiles, as shown in the following configuration.

2. Disable JSON logging in application.properties for dev and test mode:

   ```properties
   %dev.quarkus.log.console.json.enabled=false
   %test.quarkus.log.console.json.enabled=false
   ```

3. Choose the JSON console log format by setting the configuration property:

   ```properties
   quarkus.log.console.json.log-format
   ```

   This property sets the JSON log format for the console.

   The possible values are:


- **default**: Generates structured logs based on the key-value pairs present in the log record. Mapped Diagnostic Context (MDC) and Nested Diagnostic Context (NDC) data are included under the `mdc` and `ndc` fields.

  Set `quarkus.log.console.json.mdc.flat-fields=true` to write MDC entries as root-level JSON fields instead of nesting them under `mdc`. This is useful when a log aggregator (such as OpenSearch or Elasticsearch) needs MDC fields to be queryable as top-level document fields.

  .Example: nested MDC (default, `flat-fields=false`)

  ```json
  {"timestamp":"...","message":"Handling request","mdc":{"requestId":"abc123","userId":"42"}}
  ```

  .Example: flat MDC (`flat-fields=true`)

  ```json
  {"timestamp":"...","message":"Handling request","requestId":"abc123","userId":"42"}
  ```

  The option is available for all handler types:

  - console: `quarkus.log.console.json.mdc.flat-fields`
  - file: `quarkus.log.file.json.mdc.flat-fields`
  - syslog: `quarkus.log.syslog.json.mdc.flat-fields`
  - socket: `quarkus.log.socket.json.mdc.flat-fields`
  - Named handlers, such as `quarkus.log.handler.console.<name>.json.mdc.flat-fields`
- **ecs**: Uses the [Elastic Common Schema (ECS)](https://www.elastic.co/docs/reference/ecs/ecs-field-reference) format. This format modifies some default field names and adds other fields to align with ECS.

  Fields include `@timestamp`, `log.logger`, `log.level`, `process.pid`, `process.name`, `process.thread.name`, `process.thread.id`, `host.hostname`, `event.sequence`, `error.message`, `error.stack_trace`, `ecs.version`, `data_stream.type`, `service.name`, `service.version`, and `service.environment`.

  MDC data is kept in the `mdc` field, which is not part of the ECS schema. Set `mdc.flat-fields` to write those entries as root-level fields instead, or add `mdc` to `excluded-keys` to leave them out entirely.

- **gcp**: Uses the [Google Cloud](https://cloud.google.com/logging/docs/structured-logging#structured_logging_special_fields) format. This format follows the **default** format.

  When you use [OpenTelemetry](/guides/opentelemetry-tracing/index.md), Quarkus flattens tracing data present in the `mdc` field and copies it to `spanId`, `traceSampled`, and `trace`, with the `trace` value including a prefix.

  Google Cloud requires the `trace` field to follow the `"projects/<my-trace-project>/traces/12345"` format. `<my-trace-project>` uses the value of the `quarkus.application.name` configuration property.


<a id="json-provider"></a>
#### Custom JSON fields with JsonProvider

The `JsonProvider` SPI allows you to dynamically inject additional JSON fields into every log record at formatting time. Each registered provider's `writeTo` method is called once per log record, receiving a `JsonLogGenerator` and the `ExtLogRecord` being formatted.

Providers can inspect the log record (level, logger name, MDC values, message, etc.) to decide which fields, if any, to add. Any keys listed in `quarkus.log.*.json.excluded-keys` are still filtered, even when written by a provider.

##### CDI registration

In a Quarkus application, the preferred way to register a `JsonProvider` is as a CDI bean:

```java
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.logging.json.runtime.JsonFormatter.JsonLogGenerator;

@ApplicationScoped
public class MyJsonProvider implements JsonProvider {

    @Override
    public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
        // Add a static field to every log record
        generator.add("environment", "production");

        // Conditionally add a field based on the log level
        if (record.getLevel().intValue() >= Level.ERROR.intValue()) {
            generator.add("alert", "true");
        }

        // Add an MDC value as a top-level field
        String requestId = record.getMDC("requestId");
        if (requestId != null) {
            generator.add("requestId", requestId);
        }

        // Add a nested JSON object
        generator.startObject("metadata")
                .add("version", "1.0")
                .add("region", "us-east-1")
                .endObject();
    }
}
```

##### ServiceLoader registration

For providers that must work without CDI (for example, in libraries or during early startup), register them via the Java `ServiceLoader` mechanism. Create a file named `META-INF/services/io.quarkus.logging.json.runtime.JsonProvider` containing the fully qualified class name of each implementation:

```text
com.example.MyJsonProvider
```

The implementation does not need to be a CDI bean:

```java
import io.quarkus.logging.json.runtime.JsonProvider;
import io.quarkus.logging.json.runtime.JsonFormatter.JsonLogGenerator;
import org.jboss.logmanager.ExtLogRecord;

public class MyJsonProvider implements JsonProvider {

    @Override
    public void writeTo(JsonLogGenerator generator, ExtLogRecord record) throws Exception {
        generator.add("source", "my-library");
    }
}
```

> [!NOTE]
> When both CDI beans and ServiceLoader providers are present, all of them are called for each log record. ServiceLoader providers are called first, in the order they appear in the `META-INF/services/` configuration file. CDI beans are called afterwards, but their relative order is not guaranteed.

#### Configuration

Configure the JSON logging extension using supported properties to customize its behavior.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-level"></a> [`quarkus.log.level`](#quarkus-logging-json_quarkus-log-level) config_property_copy_button:+++quarkus.log.level+++[]<br><br>The log level of the root category, which is used as the default log level for all categories.<br><br>JBoss Logging supports Apache-style log levels:<br><br>\* `org.jboss.logmanager.Level#FATAL` \* `org.jboss.logmanager.Level#ERROR` \* `org.jboss.logmanager.Level#WARN` \* `org.jboss.logmanager.Level#INFO` \* `org.jboss.logmanager.Level#DEBUG` \* `org.jboss.logmanager.Level#TRACE`<br><br>In addition, it also supports the standard JDK log levels.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `INFO` |
| <a id="quarkus-logging-json_section_quarkus-log-console"></a> [Console logging](#quarkus-logging-json_section_quarkus-log-console) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-console-enabled"></a> [`quarkus.log.console.enabled`](#quarkus-logging-json_quarkus-log-console-enabled) config_property_copy_button:+++quarkus.log.console.enabled+++[]<br><br>If console logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-console-stderr"></a> [`quarkus.log.console.stderr`](#quarkus-logging-json_quarkus-log-console-stderr) config_property_copy_button:+++quarkus.log.console.stderr+++[]<br><br>If console logging should go to `System#err` instead of `System#out`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_STDERR+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-console-format"></a> [`quarkus.log.console.format`](#quarkus-logging-json_quarkus-log-console-format) config_property_copy_button:+++quarkus.log.console.format+++[]<br><br>The log format. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-console-level"></a> [`quarkus.log.console.level`](#quarkus-logging-json_quarkus-log-console-level) config_property_copy_button:+++quarkus.log.console.level+++[]<br><br>The console log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-console-darken"></a> [`quarkus.log.console.darken`](#quarkus-logging-json_quarkus-log-console-darken) config_property_copy_button:+++quarkus.log.console.darken+++[]<br><br>Specify how much the colors should be darkened. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_DARKEN+++[] | int | `0` |
| <a id="quarkus-logging-json_quarkus-log-console-filter"></a> [`quarkus.log.console.filter`](#quarkus-logging-json_quarkus-log-console-filter) config_property_copy_button:+++quarkus.log.console.filter+++[]<br><br>The name of the filter to link to the console handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-console-async-enabled"></a> [`quarkus.log.console.async.enabled`](#quarkus-logging-json_quarkus-log-console-async-enabled) config_property_copy_button:+++quarkus.log.console.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-console-async-queue-length"></a> [`quarkus.log.console.async.queue-length`](#quarkus-logging-json_quarkus-log-console-async-queue-length) config_property_copy_button:+++quarkus.log.console.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-console-async-overflow"></a> [`quarkus.log.console.async.overflow`](#quarkus-logging-json_quarkus-log-console-async-overflow) config_property_copy_button:+++quarkus.log.console.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_section_quarkus-log-file"></a> [File logging](#quarkus-logging-json_section_quarkus-log-file) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-file-enabled"></a> [`quarkus.log.file.enabled`](#quarkus-logging-json_quarkus-log-file-enabled) config_property_copy_button:+++quarkus.log.file.enabled+++[]<br><br>If file logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-file-format"></a> [`quarkus.log.file.format`](#quarkus-logging-json_quarkus-log-file-format) config_property_copy_button:+++quarkus.log.file.format+++[]<br><br>The log format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-file-level"></a> [`quarkus.log.file.level`](#quarkus-logging-json_quarkus-log-file-level) config_property_copy_button:+++quarkus.log.file.level+++[]<br><br>The level of logs to be written into the file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-file-path"></a> [`quarkus.log.file.path`](#quarkus-logging-json_quarkus-log-file-path) config_property_copy_button:+++quarkus.log.file.path+++[]<br><br>The name of the file in which logs will be written.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_PATH+++[] | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | `quarkus.log` |
| <a id="quarkus-logging-json_quarkus-log-file-filter"></a> [`quarkus.log.file.filter`](#quarkus-logging-json_quarkus-log-file-filter) config_property_copy_button:+++quarkus.log.file.filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-file-encoding"></a> [`quarkus.log.file.encoding`](#quarkus-logging-json_quarkus-log-file-encoding) config_property_copy_button:+++quarkus.log.file.encoding+++[]<br><br>The character encoding used<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ENCODING+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) |  |
| <a id="quarkus-logging-json_quarkus-log-file-async-enabled"></a> [`quarkus.log.file.async.enabled`](#quarkus-logging-json_quarkus-log-file-async-enabled) config_property_copy_button:+++quarkus.log.file.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-file-async-queue-length"></a> [`quarkus.log.file.async.queue-length`](#quarkus-logging-json_quarkus-log-file-async-queue-length) config_property_copy_button:+++quarkus.log.file.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-file-async-overflow"></a> [`quarkus.log.file.async.overflow`](#quarkus-logging-json_quarkus-log-file-async-overflow) config_property_copy_button:+++quarkus.log.file.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_quarkus-log-file-rotation-enabled"></a> [`quarkus.log.file.rotation.enabled`](#quarkus-logging-json_quarkus-log-file-rotation-enabled) config_property_copy_button:+++quarkus.log.file.rotation.enabled+++[]<br><br>Whether log rotation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-file-rotation-max-file-size"></a> [`quarkus.log.file.rotation.max-file-size`](#quarkus-logging-json_quarkus-log-file-rotation-max-file-size) config_property_copy_button:+++quarkus.log.file.rotation.max-file-size+++[]<br><br>The maximum log file size, after which a rotation is executed, up to `Long.MAX_VALUE` bytes. Note that the file is rotated *after* the log record is written. Thus, this isn't a hard maximum on the file size; rather, it's a hard *minimum* on the size of the file before it is rotated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-logging-json_quarkus-log) | `10M` |
| <a id="quarkus-logging-json_quarkus-log-file-rotation-max-backup-index"></a> [`quarkus.log.file.rotation.max-backup-index`](#quarkus-logging-json_quarkus-log-file-rotation-max-backup-index) config_property_copy_button:+++quarkus.log.file.rotation.max-backup-index+++[]<br><br>The maximum number of backups to keep.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX+++[] | int | `5` |
| <a id="quarkus-logging-json_quarkus-log-file-rotation-file-suffix"></a> [`quarkus.log.file.rotation.file-suffix`](#quarkus-logging-json_quarkus-log-file-rotation-file-suffix) config_property_copy_button:+++quarkus.log.file.rotation.file-suffix+++[]<br><br>The file handler rotation file suffix. When used, the file will be rotated based on its suffix.<br><br>The suffix must be in a date-time format that is understood by `DateTimeFormatter`.<br><br>Example fileSuffix: .yyyy-MM-dd<br><br>Note: If the suffix ends with .zip or .gz, the rotation file will also be compressed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_FILE_SUFFIX+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-file-rotation-rotate-on-boot"></a> [`quarkus.log.file.rotation.rotate-on-boot`](#quarkus-logging-json_quarkus-log-file-rotation-rotate-on-boot) config_property_copy_button:+++quarkus.log.file.rotation.rotate-on-boot+++[]<br><br>Indicates whether to rotate log files on server initialization.<br><br>You need to either set a `max-file-size` or configure a `file-suffix` for it to work.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_ROTATE_ON_BOOT+++[] | boolean | `true` |
| <a id="quarkus-logging-json_section_quarkus-log-syslog"></a> [Syslog logging](#quarkus-logging-json_section_quarkus-log-syslog) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-syslog-enabled"></a> [`quarkus.log.syslog.enabled`](#quarkus-logging-json_quarkus-log-syslog-enabled) config_property_copy_button:+++quarkus.log.syslog.enabled+++[]<br><br>If syslog logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-syslog-endpoint"></a> [`quarkus.log.syslog.endpoint`](#quarkus-logging-json_quarkus-log-syslog-endpoint) config_property_copy_button:+++quarkus.log.syslog.endpoint+++[]<br><br>The IP address and port of the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ENDPOINT+++[] | host:port | `localhost:514` |
| <a id="quarkus-logging-json_quarkus-log-syslog-app-name"></a> [`quarkus.log.syslog.app-name`](#quarkus-logging-json_quarkus-log-syslog-app-name) config_property_copy_button:+++quarkus.log.syslog.app-name+++[]<br><br>The app name used when formatting the message in RFC5424 format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_APP_NAME+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-hostname"></a> [`quarkus.log.syslog.hostname`](#quarkus-logging-json_quarkus-log-syslog-hostname) config_property_copy_button:+++quarkus.log.syslog.hostname+++[]<br><br>The name of the host the messages are being sent from<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_HOSTNAME+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-facility"></a> [`quarkus.log.syslog.facility`](#quarkus-logging-json_quarkus-log-syslog-facility) config_property_copy_button:+++quarkus.log.syslog.facility+++[]<br><br>Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_FACILITY+++[] | `kernel`, `user-level`, `mail-system`, `system-daemons`, `security`, `syslogd`, `line-printer`, `network-news`, `uucp`, `clock-daemon`, `security2`, `ftp-daemon`, `ntp`, `log-audit`, `log-alert`, `clock-daemon2`, `local-use-0`, `local-use-1`, `local-use-2`, `local-use-3`, `local-use-4`, `local-use-5`, `local-use-6`, `local-use-7` | `user-level` |
| <a id="quarkus-logging-json_quarkus-log-syslog-syslog-type"></a> [`quarkus.log.syslog.syslog-type`](#quarkus-logging-json_quarkus-log-syslog-syslog-type) config_property_copy_button:+++quarkus.log.syslog.syslog-type+++[]<br><br>Set the `SyslogType syslog type` this handler should use to format the message sent<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_SYSLOG_TYPE+++[] | `rfc5424`, `rfc3164` | `rfc5424` |
| <a id="quarkus-logging-json_quarkus-log-syslog-protocol"></a> [`quarkus.log.syslog.protocol`](#quarkus-logging-json_quarkus-log-syslog-protocol) config_property_copy_button:+++quarkus.log.syslog.protocol+++[]<br><br>Sets the protocol used to connect to the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-logging-json_quarkus-log-syslog-use-counting-framing"></a> [`quarkus.log.syslog.use-counting-framing`](#quarkus-logging-json_quarkus-log-syslog-use-counting-framing) config_property_copy_button:+++quarkus.log.syslog.use-counting-framing+++[]<br><br>If enabled, the message being sent is prefixed with the size of the message<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_USE_COUNTING_FRAMING+++[] | `true`, `false`, `protocol-dependent` | `protocol-dependent` |
| <a id="quarkus-logging-json_quarkus-log-syslog-truncate"></a> [`quarkus.log.syslog.truncate`](#quarkus-logging-json_quarkus-log-syslog-truncate) config_property_copy_button:+++quarkus.log.syslog.truncate+++[]<br><br>Set to `true` to truncate the message if it exceeds maximum length<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_TRUNCATE+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-syslog-block-on-reconnect"></a> [`quarkus.log.syslog.block-on-reconnect`](#quarkus-logging-json_quarkus-log-syslog-block-on-reconnect) config_property_copy_button:+++quarkus.log.syslog.block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-syslog-format"></a> [`quarkus.log.syslog.format`](#quarkus-logging-json_quarkus-log-syslog-format) config_property_copy_button:+++quarkus.log.syslog.format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-syslog-level"></a> [`quarkus.log.syslog.level`](#quarkus-logging-json_quarkus-log-syslog-level) config_property_copy_button:+++quarkus.log.syslog.level+++[]<br><br>The log level specifying what message levels will be logged by the Syslog logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-syslog-filter"></a> [`quarkus.log.syslog.filter`](#quarkus-logging-json_quarkus-log-syslog-filter) config_property_copy_button:+++quarkus.log.syslog.filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-max-length"></a> [`quarkus.log.syslog.max-length`](#quarkus-logging-json_quarkus-log-syslog-max-length) config_property_copy_button:+++quarkus.log.syslog.max-length+++[]<br><br>The maximum length, in bytes, of the message allowed to be sent, up to `Integer.MAX_VALUE` bytes. The length includes the header and the message.<br><br>If not set, the default value is `2048` when `sys-log-type` is `rfc5424` (which is the default) and `1024` when `sys-log-type` is `rfc3164`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_MAX_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-logging-json_quarkus-log) |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-async-enabled"></a> [`quarkus.log.syslog.async.enabled`](#quarkus-logging-json_quarkus-log-syslog-async-enabled) config_property_copy_button:+++quarkus.log.syslog.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-syslog-async-queue-length"></a> [`quarkus.log.syslog.async.queue-length`](#quarkus-logging-json_quarkus-log-syslog-async-queue-length) config_property_copy_button:+++quarkus.log.syslog.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-syslog-async-overflow"></a> [`quarkus.log.syslog.async.overflow`](#quarkus-logging-json_quarkus-log-syslog-async-overflow) config_property_copy_button:+++quarkus.log.syslog.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_section_quarkus-log-socket"></a> [Socket logging](#quarkus-logging-json_section_quarkus-log-socket) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-socket-enabled"></a> [`quarkus.log.socket.enabled`](#quarkus-logging-json_quarkus-log-socket-enabled) config_property_copy_button:+++quarkus.log.socket.enabled+++[]<br><br>If socket logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-socket-endpoint"></a> [`quarkus.log.socket.endpoint`](#quarkus-logging-json_quarkus-log-socket-endpoint) config_property_copy_button:+++quarkus.log.socket.endpoint+++[]<br><br>The IP address and port of the server receiving the logs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ENDPOINT+++[] | host:port | `localhost:4560` |
| <a id="quarkus-logging-json_quarkus-log-socket-protocol"></a> [`quarkus.log.socket.protocol`](#quarkus-logging-json_quarkus-log-socket-protocol) config_property_copy_button:+++quarkus.log.socket.protocol+++[]<br><br>Sets the protocol used to connect to the syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-logging-json_quarkus-log-socket-block-on-reconnect"></a> [`quarkus.log.socket.block-on-reconnect`](#quarkus-logging-json_quarkus-log-socket-block-on-reconnect) config_property_copy_button:+++quarkus.log.socket.block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-socket-format"></a> [`quarkus.log.socket.format`](#quarkus-logging-json_quarkus-log-socket-format) config_property_copy_button:+++quarkus.log.socket.format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-socket-level"></a> [`quarkus.log.socket.level`](#quarkus-logging-json_quarkus-log-socket-level) config_property_copy_button:+++quarkus.log.socket.level+++[]<br><br>The log level specifying, which message levels will be logged by socket logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-socket-filter"></a> [`quarkus.log.socket.filter`](#quarkus-logging-json_quarkus-log-socket-filter) config_property_copy_button:+++quarkus.log.socket.filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-socket-async-enabled"></a> [`quarkus.log.socket.async.enabled`](#quarkus-logging-json_quarkus-log-socket-async-enabled) config_property_copy_button:+++quarkus.log.socket.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-socket-async-queue-length"></a> [`quarkus.log.socket.async.queue-length`](#quarkus-logging-json_quarkus-log-socket-async-queue-length) config_property_copy_button:+++quarkus.log.socket.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-socket-async-overflow"></a> [`quarkus.log.socket.async.overflow`](#quarkus-logging-json_quarkus-log-socket-async-overflow) config_property_copy_button:+++quarkus.log.socket.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_section_quarkus-log-category"></a> [Logging categories](#quarkus-logging-json_section_quarkus-log-category) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-category-categories-level"></a> [`quarkus.log.category."categories".level`](#quarkus-logging-json_quarkus-log-category-categories-level) config_property_copy_button:+++quarkus.log.category."categories".level+++[]<br><br>The log level for this category.<br><br>Note that to get log levels below `INFO`, the minimum level build-time configuration option also needs to be adjusted.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__LEVEL+++[] | InheritableLevel | `inherit` |
| <a id="quarkus-logging-json_quarkus-log-category-categories-handlers"></a> [`quarkus.log.category."categories".handlers`](#quarkus-logging-json_quarkus-log-category-categories-handlers) config_property_copy_button:+++quarkus.log.category."categories".handlers+++[]<br><br>The names of the handlers to link to this category.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__HANDLERS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-category-categories-use-parent-handlers"></a> [`quarkus.log.category."categories".use-parent-handlers`](#quarkus-logging-json_quarkus-log-category-categories-use-parent-handlers) config_property_copy_button:+++quarkus.log.category."categories".use-parent-handlers+++[]<br><br>Specify whether this logger should send its output to its parent Logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__USE_PARENT_HANDLERS+++[] | boolean | `true` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-console"></a> [Console handlers](#quarkus-logging-json_section_quarkus-log-handler-console) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-enabled"></a> [`quarkus.log.handler.console."console-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".enabled+++[]<br><br>If console logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ENABLED+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-stderr"></a> [`quarkus.log.handler.console."console-handlers".stderr`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-stderr) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".stderr+++[]<br><br>If console logging should go to `System#err` instead of `System#out`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__STDERR+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-format"></a> [`quarkus.log.handler.console."console-handlers".format`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-format) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".format+++[]<br><br>The log format. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-level"></a> [`quarkus.log.handler.console."console-handlers".level`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-level) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".level+++[]<br><br>The console log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-darken"></a> [`quarkus.log.handler.console."console-handlers".darken`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-darken) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".darken+++[]<br><br>Specify how much the colors should be darkened. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__DARKEN+++[] | int | `0` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-filter"></a> [`quarkus.log.handler.console."console-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-filter) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".filter+++[]<br><br>The name of the filter to link to the console handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-async-enabled"></a> [`quarkus.log.handler.console."console-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-async-queue-length"></a> [`quarkus.log.handler.console."console-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-console-handlers-async-overflow"></a> [`quarkus.log.handler.console."console-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-console-console-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-enabled"></a> [`quarkus.log.handler.console."json-console-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".enabled+++[]<br><br>If console logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__ENABLED+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-stderr"></a> [`quarkus.log.handler.console."json-console-handlers".stderr`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-stderr) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".stderr+++[]<br><br>If console logging should go to `System#err` instead of `System#out`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__STDERR+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-format"></a> [`quarkus.log.handler.console."json-console-handlers".format`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-format) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".format+++[]<br><br>The log format. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-level"></a> [`quarkus.log.handler.console."json-console-handlers".level`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-level) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".level+++[]<br><br>The console log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-darken"></a> [`quarkus.log.handler.console."json-console-handlers".darken`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-darken) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".darken+++[]<br><br>Specify how much the colors should be darkened. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__DARKEN+++[] | int | `0` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-filter"></a> [`quarkus.log.handler.console."json-console-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-filter) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".filter+++[]<br><br>The name of the filter to link to the console handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-async-enabled"></a> [`quarkus.log.handler.console."json-console-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-async-queue-length"></a> [`quarkus.log.handler.console."json-console-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-async-overflow"></a> [`quarkus.log.handler.console."json-console-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-console-json-console-handlers-json"></a> [JSON logging configuration](#quarkus-logging-json_section_quarkus-log-handler-console-json-console-handlers-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-enabled"></a> [`quarkus.log.handler.console."json-console-handlers".json.enabled`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-enabled) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-pretty-print"></a> [`quarkus.log.handler.console."json-console-handlers".json.pretty-print`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-pretty-print) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-date-format"></a> [`quarkus.log.handler.console."json-console-handlers".json.date-format`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-date-format) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-record-delimiter"></a> [`quarkus.log.handler.console."json-console-handlers".json.record-delimiter`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-record-delimiter) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-zone-id"></a> [`quarkus.log.handler.console."json-console-handlers".json.zone-id`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-zone-id) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-exception-output-type"></a> [`quarkus.log.handler.console."json-console-handlers".json.exception-output-type`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-exception-output-type) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-print-details"></a> [`quarkus.log.handler.console."json-console-handlers".json.print-details`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-print-details) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-key-overrides"></a> [`quarkus.log.handler.console."json-console-handlers".json.key-overrides`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-key-overrides) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-excluded-keys"></a> [`quarkus.log.handler.console."json-console-handlers".json.excluded-keys`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-excluded-keys) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-additional-field-field-name-value"></a> [`quarkus.log.handler.console."json-console-handlers".json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-additional-field-field-name-type"></a> [`quarkus.log.handler.console."json-console-handlers".json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-log-format"></a> [`quarkus.log.handler.console."json-console-handlers".json.log-format`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-log-format) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-mdc-flat-fields"></a> [`quarkus.log.handler.console."json-console-handlers".json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-handler-console-json-console-handlers-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.handler.console."json-console-handlers".json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__JSON_CONSOLE_HANDLERS__JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-file"></a> [File handlers](#quarkus-logging-json_section_quarkus-log-handler-file) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-enabled"></a> [`quarkus.log.handler.file."file-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".enabled+++[]<br><br>If file logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-format"></a> [`quarkus.log.handler.file."file-handlers".format`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-format) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".format+++[]<br><br>The log format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-level"></a> [`quarkus.log.handler.file."file-handlers".level`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-level) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".level+++[]<br><br>The level of logs to be written into the file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-path"></a> [`quarkus.log.handler.file."file-handlers".path`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-path) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".path+++[]<br><br>The name of the file in which logs will be written.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__PATH+++[] | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | `quarkus.log` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-filter"></a> [`quarkus.log.handler.file."file-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-filter) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-encoding"></a> [`quarkus.log.handler.file."file-handlers".encoding`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-encoding) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".encoding+++[]<br><br>The character encoding used<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ENCODING+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-async-enabled"></a> [`quarkus.log.handler.file."file-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-async-queue-length"></a> [`quarkus.log.handler.file."file-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-async-overflow"></a> [`quarkus.log.handler.file."file-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-enabled"></a> [`quarkus.log.handler.file."file-handlers".rotation.enabled`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-enabled) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.enabled+++[]<br><br>Whether log rotation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-max-file-size"></a> [`quarkus.log.handler.file."file-handlers".rotation.max-file-size`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-max-file-size) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.max-file-size+++[]<br><br>The maximum log file size, after which a rotation is executed, up to `Long.MAX_VALUE` bytes. Note that the file is rotated *after* the log record is written. Thus, this isn't a hard maximum on the file size; rather, it's a hard *minimum* on the size of the file before it is rotated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_MAX_FILE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-logging-json_quarkus-log) | `10M` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-max-backup-index"></a> [`quarkus.log.handler.file."file-handlers".rotation.max-backup-index`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-max-backup-index) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.max-backup-index+++[]<br><br>The maximum number of backups to keep.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_MAX_BACKUP_INDEX+++[] | int | `5` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-file-suffix"></a> [`quarkus.log.handler.file."file-handlers".rotation.file-suffix`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-file-suffix) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.file-suffix+++[]<br><br>The file handler rotation file suffix. When used, the file will be rotated based on its suffix.<br><br>The suffix must be in a date-time format that is understood by `DateTimeFormatter`.<br><br>Example fileSuffix: .yyyy-MM-dd<br><br>Note: If the suffix ends with .zip or .gz, the rotation file will also be compressed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_FILE_SUFFIX+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-rotate-on-boot"></a> [`quarkus.log.handler.file."file-handlers".rotation.rotate-on-boot`](#quarkus-logging-json_quarkus-log-handler-file-file-handlers-rotation-rotate-on-boot) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.rotate-on-boot+++[]<br><br>Indicates whether to rotate log files on server initialization.<br><br>You need to either set a `max-file-size` or configure a `file-suffix` for it to work.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_ROTATE_ON_BOOT+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-enabled"></a> [`quarkus.log.handler.file."json-file-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".enabled+++[]<br><br>If file logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-format"></a> [`quarkus.log.handler.file."json-file-handlers".format`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-format) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".format+++[]<br><br>The log format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-level"></a> [`quarkus.log.handler.file."json-file-handlers".level`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-level) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".level+++[]<br><br>The level of logs to be written into the file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-path"></a> [`quarkus.log.handler.file."json-file-handlers".path`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-path) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".path+++[]<br><br>The name of the file in which logs will be written.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__PATH+++[] | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | `quarkus.log` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-filter"></a> [`quarkus.log.handler.file."json-file-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-filter) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-encoding"></a> [`quarkus.log.handler.file."json-file-handlers".encoding`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-encoding) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".encoding+++[]<br><br>The character encoding used<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ENCODING+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-async-enabled"></a> [`quarkus.log.handler.file."json-file-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-async-queue-length"></a> [`quarkus.log.handler.file."json-file-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-async-overflow"></a> [`quarkus.log.handler.file."json-file-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-enabled"></a> [`quarkus.log.handler.file."json-file-handlers".rotation.enabled`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-enabled) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".rotation.enabled+++[]<br><br>Whether log rotation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ROTATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-max-file-size"></a> [`quarkus.log.handler.file."json-file-handlers".rotation.max-file-size`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-max-file-size) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".rotation.max-file-size+++[]<br><br>The maximum log file size, after which a rotation is executed, up to `Long.MAX_VALUE` bytes. Note that the file is rotated *after* the log record is written. Thus, this isn't a hard maximum on the file size; rather, it's a hard *minimum* on the size of the file before it is rotated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ROTATION_MAX_FILE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-logging-json_quarkus-log) | `10M` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-max-backup-index"></a> [`quarkus.log.handler.file."json-file-handlers".rotation.max-backup-index`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-max-backup-index) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".rotation.max-backup-index+++[]<br><br>The maximum number of backups to keep.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ROTATION_MAX_BACKUP_INDEX+++[] | int | `5` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-file-suffix"></a> [`quarkus.log.handler.file."json-file-handlers".rotation.file-suffix`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-file-suffix) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".rotation.file-suffix+++[]<br><br>The file handler rotation file suffix. When used, the file will be rotated based on its suffix.<br><br>The suffix must be in a date-time format that is understood by `DateTimeFormatter`.<br><br>Example fileSuffix: .yyyy-MM-dd<br><br>Note: If the suffix ends with .zip or .gz, the rotation file will also be compressed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ROTATION_FILE_SUFFIX+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-rotate-on-boot"></a> [`quarkus.log.handler.file."json-file-handlers".rotation.rotate-on-boot`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-rotation-rotate-on-boot) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".rotation.rotate-on-boot+++[]<br><br>Indicates whether to rotate log files on server initialization.<br><br>You need to either set a `max-file-size` or configure a `file-suffix` for it to work.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__ROTATION_ROTATE_ON_BOOT+++[] | boolean | `true` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-file-json-file-handlers-json"></a> [JSON logging configuration](#quarkus-logging-json_section_quarkus-log-handler-file-json-file-handlers-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-enabled"></a> [`quarkus.log.handler.file."json-file-handlers".json.enabled`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-enabled) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-pretty-print"></a> [`quarkus.log.handler.file."json-file-handlers".json.pretty-print`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-pretty-print) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-date-format"></a> [`quarkus.log.handler.file."json-file-handlers".json.date-format`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-date-format) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-record-delimiter"></a> [`quarkus.log.handler.file."json-file-handlers".json.record-delimiter`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-record-delimiter) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-zone-id"></a> [`quarkus.log.handler.file."json-file-handlers".json.zone-id`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-zone-id) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-exception-output-type"></a> [`quarkus.log.handler.file."json-file-handlers".json.exception-output-type`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-exception-output-type) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-print-details"></a> [`quarkus.log.handler.file."json-file-handlers".json.print-details`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-print-details) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-key-overrides"></a> [`quarkus.log.handler.file."json-file-handlers".json.key-overrides`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-key-overrides) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-excluded-keys"></a> [`quarkus.log.handler.file."json-file-handlers".json.excluded-keys`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-excluded-keys) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-additional-field-field-name-value"></a> [`quarkus.log.handler.file."json-file-handlers".json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-additional-field-field-name-type"></a> [`quarkus.log.handler.file."json-file-handlers".json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-log-format"></a> [`quarkus.log.handler.file."json-file-handlers".json.log-format`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-log-format) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-mdc-flat-fields"></a> [`quarkus.log.handler.file."json-file-handlers".json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-handler-file-json-file-handlers-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.handler.file."json-file-handlers".json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__JSON_FILE_HANDLERS__JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-syslog"></a> [Syslog handlers](#quarkus-logging-json_section_quarkus-log-handler-syslog) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-enabled"></a> [`quarkus.log.handler.syslog."syslog-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".enabled+++[]<br><br>If syslog logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-endpoint"></a> [`quarkus.log.handler.syslog."syslog-handlers".endpoint`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-endpoint) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".endpoint+++[]<br><br>The IP address and port of the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ENDPOINT+++[] | host:port | `localhost:514` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-app-name"></a> [`quarkus.log.handler.syslog."syslog-handlers".app-name`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-app-name) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".app-name+++[]<br><br>The app name used when formatting the message in RFC5424 format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__APP_NAME+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-hostname"></a> [`quarkus.log.handler.syslog."syslog-handlers".hostname`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-hostname) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".hostname+++[]<br><br>The name of the host the messages are being sent from<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__HOSTNAME+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-facility"></a> [`quarkus.log.handler.syslog."syslog-handlers".facility`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-facility) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".facility+++[]<br><br>Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__FACILITY+++[] | `kernel`, `user-level`, `mail-system`, `system-daemons`, `security`, `syslogd`, `line-printer`, `network-news`, `uucp`, `clock-daemon`, `security2`, `ftp-daemon`, `ntp`, `log-audit`, `log-alert`, `clock-daemon2`, `local-use-0`, `local-use-1`, `local-use-2`, `local-use-3`, `local-use-4`, `local-use-5`, `local-use-6`, `local-use-7` | `user-level` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-syslog-type"></a> [`quarkus.log.handler.syslog."syslog-handlers".syslog-type`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-syslog-type) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".syslog-type+++[]<br><br>Set the `SyslogType syslog type` this handler should use to format the message sent<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__SYSLOG_TYPE+++[] | `rfc5424`, `rfc3164` | `rfc5424` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-protocol"></a> [`quarkus.log.handler.syslog."syslog-handlers".protocol`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-protocol) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".protocol+++[]<br><br>Sets the protocol used to connect to the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-use-counting-framing"></a> [`quarkus.log.handler.syslog."syslog-handlers".use-counting-framing`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-use-counting-framing) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".use-counting-framing+++[]<br><br>If enabled, the message being sent is prefixed with the size of the message<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__USE_COUNTING_FRAMING+++[] | `true`, `false`, `protocol-dependent` | `protocol-dependent` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-truncate"></a> [`quarkus.log.handler.syslog."syslog-handlers".truncate`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-truncate) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".truncate+++[]<br><br>Set to `true` to truncate the message if it exceeds maximum length<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__TRUNCATE+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-block-on-reconnect"></a> [`quarkus.log.handler.syslog."syslog-handlers".block-on-reconnect`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-block-on-reconnect) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-format"></a> [`quarkus.log.handler.syslog."syslog-handlers".format`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-format) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-level"></a> [`quarkus.log.handler.syslog."syslog-handlers".level`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-level) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".level+++[]<br><br>The log level specifying what message levels will be logged by the Syslog logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-filter"></a> [`quarkus.log.handler.syslog."syslog-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-filter) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-max-length"></a> [`quarkus.log.handler.syslog."syslog-handlers".max-length`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-max-length) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".max-length+++[]<br><br>The maximum length, in bytes, of the message allowed to be sent, up to `Integer.MAX_VALUE` bytes. The length includes the header and the message.<br><br>If not set, the default value is `2048` when `sys-log-type` is `rfc5424` (which is the default) and `1024` when `sys-log-type` is `rfc3164`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__MAX_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-logging-json_quarkus-log) |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-async-enabled"></a> [`quarkus.log.handler.syslog."syslog-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-async-queue-length"></a> [`quarkus.log.handler.syslog."syslog-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-async-overflow"></a> [`quarkus.log.handler.syslog."syslog-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-syslog-syslog-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-enabled"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".enabled+++[]<br><br>If syslog logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-endpoint"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".endpoint`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-endpoint) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".endpoint+++[]<br><br>The IP address and port of the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__ENDPOINT+++[] | host:port | `localhost:514` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-app-name"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".app-name`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-app-name) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".app-name+++[]<br><br>The app name used when formatting the message in RFC5424 format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__APP_NAME+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-hostname"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".hostname`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-hostname) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".hostname+++[]<br><br>The name of the host the messages are being sent from<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__HOSTNAME+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-facility"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".facility`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-facility) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".facility+++[]<br><br>Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__FACILITY+++[] | `kernel`, `user-level`, `mail-system`, `system-daemons`, `security`, `syslogd`, `line-printer`, `network-news`, `uucp`, `clock-daemon`, `security2`, `ftp-daemon`, `ntp`, `log-audit`, `log-alert`, `clock-daemon2`, `local-use-0`, `local-use-1`, `local-use-2`, `local-use-3`, `local-use-4`, `local-use-5`, `local-use-6`, `local-use-7` | `user-level` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-syslog-type"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".syslog-type`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-syslog-type) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".syslog-type+++[]<br><br>Set the `SyslogType syslog type` this handler should use to format the message sent<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__SYSLOG_TYPE+++[] | `rfc5424`, `rfc3164` | `rfc5424` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-protocol"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".protocol`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-protocol) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".protocol+++[]<br><br>Sets the protocol used to connect to the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-use-counting-framing"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".use-counting-framing`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-use-counting-framing) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".use-counting-framing+++[]<br><br>If enabled, the message being sent is prefixed with the size of the message<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__USE_COUNTING_FRAMING+++[] | `true`, `false`, `protocol-dependent` | `protocol-dependent` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-truncate"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".truncate`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-truncate) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".truncate+++[]<br><br>Set to `true` to truncate the message if it exceeds maximum length<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__TRUNCATE+++[] | boolean | `true` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-block-on-reconnect"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".block-on-reconnect`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-block-on-reconnect) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-format"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".format`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-format) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-level"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".level`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-level) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".level+++[]<br><br>The log level specifying what message levels will be logged by the Syslog logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-filter"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-filter) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-max-length"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".max-length`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-max-length) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".max-length+++[]<br><br>The maximum length, in bytes, of the message allowed to be sent, up to `Integer.MAX_VALUE` bytes. The length includes the header and the message.<br><br>If not set, the default value is `2048` when `sys-log-type` is `rfc5424` (which is the default) and `1024` when `sys-log-type` is `rfc3164`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__MAX_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-logging-json_quarkus-log) |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-async-enabled"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-async-queue-length"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-async-overflow"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-syslog-json-syslog-handlers-json"></a> [JSON logging configuration](#quarkus-logging-json_section_quarkus-log-handler-syslog-json-syslog-handlers-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-enabled"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.enabled`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-pretty-print"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.pretty-print`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-pretty-print) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-date-format"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.date-format`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-date-format) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-record-delimiter"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.record-delimiter`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-record-delimiter) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-zone-id"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.zone-id`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-zone-id) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-exception-output-type"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.exception-output-type`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-exception-output-type) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-print-details"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.print-details`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-print-details) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-key-overrides"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.key-overrides`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-key-overrides) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-excluded-keys"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.excluded-keys`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-excluded-keys) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-additional-field-field-name-value"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-additional-field-field-name-type"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-log-format"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.log-format`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-log-format) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-mdc-flat-fields"></a> [`quarkus.log.handler.syslog."json-syslog-handlers".json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-handler-syslog-json-syslog-handlers-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.handler.syslog."json-syslog-handlers".json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__JSON_SYSLOG_HANDLERS__JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-socket"></a> [Socket handlers](#quarkus-logging-json_section_quarkus-log-handler-socket) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-enabled"></a> [`quarkus.log.handler.socket."socket-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".enabled+++[]<br><br>If socket logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-endpoint"></a> [`quarkus.log.handler.socket."socket-handlers".endpoint`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-endpoint) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".endpoint+++[]<br><br>The IP address and port of the server receiving the logs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ENDPOINT+++[] | host:port | `localhost:4560` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-protocol"></a> [`quarkus.log.handler.socket."socket-handlers".protocol`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-protocol) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".protocol+++[]<br><br>Sets the protocol used to connect to the syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-block-on-reconnect"></a> [`quarkus.log.handler.socket."socket-handlers".block-on-reconnect`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-block-on-reconnect) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-format"></a> [`quarkus.log.handler.socket."socket-handlers".format`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-format) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-level"></a> [`quarkus.log.handler.socket."socket-handlers".level`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-level) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".level+++[]<br><br>The log level specifying, which message levels will be logged by socket logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-filter"></a> [`quarkus.log.handler.socket."socket-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-filter) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-async-enabled"></a> [`quarkus.log.handler.socket."socket-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-async-queue-length"></a> [`quarkus.log.handler.socket."socket-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-async-overflow"></a> [`quarkus.log.handler.socket."socket-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-socket-socket-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-enabled"></a> [`quarkus.log.handler.socket."json-socket-handlers".enabled`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".enabled+++[]<br><br>If socket logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-endpoint"></a> [`quarkus.log.handler.socket."json-socket-handlers".endpoint`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-endpoint) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".endpoint+++[]<br><br>The IP address and port of the server receiving the logs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__ENDPOINT+++[] | host:port | `localhost:4560` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-protocol"></a> [`quarkus.log.handler.socket."json-socket-handlers".protocol`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-protocol) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".protocol+++[]<br><br>Sets the protocol used to connect to the syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-block-on-reconnect"></a> [`quarkus.log.handler.socket."json-socket-handlers".block-on-reconnect`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-block-on-reconnect) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-format"></a> [`quarkus.log.handler.socket."json-socket-handlers".format`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-format) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-level"></a> [`quarkus.log.handler.socket."json-socket-handlers".level`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-level) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".level+++[]<br><br>The log level specifying, which message levels will be logged by socket logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-filter"></a> [`quarkus.log.handler.socket."json-socket-handlers".filter`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-filter) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-async-enabled"></a> [`quarkus.log.handler.socket."json-socket-handlers".async.enabled`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-async-queue-length"></a> [`quarkus.log.handler.socket."json-socket-handlers".async.queue-length`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-async-overflow"></a> [`quarkus.log.handler.socket."json-socket-handlers".async.overflow`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-logging-json_section_quarkus-log-handler-socket-json-socket-handlers-json"></a> [JSON logging configuration](#quarkus-logging-json_section_quarkus-log-handler-socket-json-socket-handlers-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-enabled"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.enabled`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-enabled) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-pretty-print"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.pretty-print`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-pretty-print) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-date-format"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.date-format`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-date-format) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-record-delimiter"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.record-delimiter`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-record-delimiter) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-zone-id"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.zone-id`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-zone-id) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-exception-output-type"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.exception-output-type`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-exception-output-type) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-print-details"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.print-details`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-print-details) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-key-overrides"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.key-overrides`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-key-overrides) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-excluded-keys"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.excluded-keys`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-excluded-keys) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-additional-field-field-name-value"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-additional-field-field-name-type"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-log-format"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.log-format`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-log-format) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-mdc-flat-fields"></a> [`quarkus.log.handler.socket."json-socket-handlers".json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-handler-socket-json-socket-handlers-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.handler.socket."json-socket-handlers".json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__JSON_SOCKET_HANDLERS__JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-filter"></a> [Log cleanup filters - internal use](#quarkus-logging-json_section_quarkus-log-filter) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-filter-filters-if-starts-with"></a> [`quarkus.log.filter."filters".if-starts-with`](#quarkus-logging-json_quarkus-log-filter-filters-if-starts-with) config_property_copy_button:+++quarkus.log.filter."filters".if-starts-with+++[]<br><br>The message prefix to match<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILTER__FILTERS__IF_STARTS_WITH+++[] | list of string | `inherit` |
| <a id="quarkus-logging-json_quarkus-log-filter-filters-target-level"></a> [`quarkus.log.filter."filters".target-level`](#quarkus-logging-json_quarkus-log-filter-filters-target-level) config_property_copy_button:+++quarkus.log.filter."filters".target-level+++[]<br><br>The new log level for the filtered message. Defaults to DEBUG.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILTER__FILTERS__TARGET_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `DEBUG` |
| <a id="quarkus-logging-json_quarkus-log-handlers"></a> [`quarkus.log.handlers`](#quarkus-logging-json_quarkus-log-handlers) config_property_copy_button:+++quarkus.log.handlers+++[]<br><br>The names of additional handlers to link to the root category. These handlers are defined in consoleHandlers, fileHandlers, or syslogHandlers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLERS+++[] | list of string |  |
| <a id="quarkus-logging-json_section_quarkus-log-console-json"></a> [Console logging](#quarkus-logging-json_section_quarkus-log-console-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-console-json-enabled"></a> [`quarkus.log.console.json.enabled`](#quarkus-logging-json_quarkus-log-console-json-enabled) config_property_copy_button:+++quarkus.log.console.json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-console-json-pretty-print"></a> [`quarkus.log.console.json.pretty-print`](#quarkus-logging-json_quarkus-log-console-json-pretty-print) config_property_copy_button:+++quarkus.log.console.json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-console-json-date-format"></a> [`quarkus.log.console.json.date-format`](#quarkus-logging-json_quarkus-log-console-json-date-format) config_property_copy_button:+++quarkus.log.console.json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-console-json-record-delimiter"></a> [`quarkus.log.console.json.record-delimiter`](#quarkus-logging-json_quarkus-log-console-json-record-delimiter) config_property_copy_button:+++quarkus.log.console.json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-console-json-zone-id"></a> [`quarkus.log.console.json.zone-id`](#quarkus-logging-json_quarkus-log-console-json-zone-id) config_property_copy_button:+++quarkus.log.console.json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-console-json-exception-output-type"></a> [`quarkus.log.console.json.exception-output-type`](#quarkus-logging-json_quarkus-log-console-json-exception-output-type) config_property_copy_button:+++quarkus.log.console.json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-console-json-print-details"></a> [`quarkus.log.console.json.print-details`](#quarkus-logging-json_quarkus-log-console-json-print-details) config_property_copy_button:+++quarkus.log.console.json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-console-json-key-overrides"></a> [`quarkus.log.console.json.key-overrides`](#quarkus-logging-json_quarkus-log-console-json-key-overrides) config_property_copy_button:+++quarkus.log.console.json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-console-json-excluded-keys"></a> [`quarkus.log.console.json.excluded-keys`](#quarkus-logging-json_quarkus-log-console-json-excluded-keys) config_property_copy_button:+++quarkus.log.console.json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-console-json-additional-field-field-name-value"></a> [`quarkus.log.console.json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-console-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.console.json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-console-json-additional-field-field-name-type"></a> [`quarkus.log.console.json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-console-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.console.json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-console-json-log-format"></a> [`quarkus.log.console.json.log-format`](#quarkus-logging-json_quarkus-log-console-json-log-format) config_property_copy_button:+++quarkus.log.console.json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-console-json-mdc-flat-fields"></a> [`quarkus.log.console.json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-console-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.console.json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-file-json"></a> [File logging](#quarkus-logging-json_section_quarkus-log-file-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-file-json-enabled"></a> [`quarkus.log.file.json.enabled`](#quarkus-logging-json_quarkus-log-file-json-enabled) config_property_copy_button:+++quarkus.log.file.json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-file-json-pretty-print"></a> [`quarkus.log.file.json.pretty-print`](#quarkus-logging-json_quarkus-log-file-json-pretty-print) config_property_copy_button:+++quarkus.log.file.json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-file-json-date-format"></a> [`quarkus.log.file.json.date-format`](#quarkus-logging-json_quarkus-log-file-json-date-format) config_property_copy_button:+++quarkus.log.file.json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-file-json-record-delimiter"></a> [`quarkus.log.file.json.record-delimiter`](#quarkus-logging-json_quarkus-log-file-json-record-delimiter) config_property_copy_button:+++quarkus.log.file.json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-file-json-zone-id"></a> [`quarkus.log.file.json.zone-id`](#quarkus-logging-json_quarkus-log-file-json-zone-id) config_property_copy_button:+++quarkus.log.file.json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-file-json-exception-output-type"></a> [`quarkus.log.file.json.exception-output-type`](#quarkus-logging-json_quarkus-log-file-json-exception-output-type) config_property_copy_button:+++quarkus.log.file.json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-file-json-print-details"></a> [`quarkus.log.file.json.print-details`](#quarkus-logging-json_quarkus-log-file-json-print-details) config_property_copy_button:+++quarkus.log.file.json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-file-json-key-overrides"></a> [`quarkus.log.file.json.key-overrides`](#quarkus-logging-json_quarkus-log-file-json-key-overrides) config_property_copy_button:+++quarkus.log.file.json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-file-json-excluded-keys"></a> [`quarkus.log.file.json.excluded-keys`](#quarkus-logging-json_quarkus-log-file-json-excluded-keys) config_property_copy_button:+++quarkus.log.file.json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-file-json-additional-field-field-name-value"></a> [`quarkus.log.file.json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-file-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.file.json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-file-json-additional-field-field-name-type"></a> [`quarkus.log.file.json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-file-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.file.json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-file-json-log-format"></a> [`quarkus.log.file.json.log-format`](#quarkus-logging-json_quarkus-log-file-json-log-format) config_property_copy_button:+++quarkus.log.file.json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-file-json-mdc-flat-fields"></a> [`quarkus.log.file.json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-file-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.file.json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-syslog-json"></a> [Syslog logging](#quarkus-logging-json_section_quarkus-log-syslog-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-enabled"></a> [`quarkus.log.syslog.json.enabled`](#quarkus-logging-json_quarkus-log-syslog-json-enabled) config_property_copy_button:+++quarkus.log.syslog.json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-pretty-print"></a> [`quarkus.log.syslog.json.pretty-print`](#quarkus-logging-json_quarkus-log-syslog-json-pretty-print) config_property_copy_button:+++quarkus.log.syslog.json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-date-format"></a> [`quarkus.log.syslog.json.date-format`](#quarkus-logging-json_quarkus-log-syslog-json-date-format) config_property_copy_button:+++quarkus.log.syslog.json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-record-delimiter"></a> [`quarkus.log.syslog.json.record-delimiter`](#quarkus-logging-json_quarkus-log-syslog-json-record-delimiter) config_property_copy_button:+++quarkus.log.syslog.json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-zone-id"></a> [`quarkus.log.syslog.json.zone-id`](#quarkus-logging-json_quarkus-log-syslog-json-zone-id) config_property_copy_button:+++quarkus.log.syslog.json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-exception-output-type"></a> [`quarkus.log.syslog.json.exception-output-type`](#quarkus-logging-json_quarkus-log-syslog-json-exception-output-type) config_property_copy_button:+++quarkus.log.syslog.json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-print-details"></a> [`quarkus.log.syslog.json.print-details`](#quarkus-logging-json_quarkus-log-syslog-json-print-details) config_property_copy_button:+++quarkus.log.syslog.json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-key-overrides"></a> [`quarkus.log.syslog.json.key-overrides`](#quarkus-logging-json_quarkus-log-syslog-json-key-overrides) config_property_copy_button:+++quarkus.log.syslog.json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-excluded-keys"></a> [`quarkus.log.syslog.json.excluded-keys`](#quarkus-logging-json_quarkus-log-syslog-json-excluded-keys) config_property_copy_button:+++quarkus.log.syslog.json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-additional-field-field-name-value"></a> [`quarkus.log.syslog.json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-syslog-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.syslog.json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-additional-field-field-name-type"></a> [`quarkus.log.syslog.json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-syslog-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.syslog.json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-log-format"></a> [`quarkus.log.syslog.json.log-format`](#quarkus-logging-json_quarkus-log-syslog-json-log-format) config_property_copy_button:+++quarkus.log.syslog.json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-syslog-json-mdc-flat-fields"></a> [`quarkus.log.syslog.json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-syslog-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.syslog.json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_section_quarkus-log-socket-json"></a> [Socket logging](#quarkus-logging-json_section_quarkus-log-socket-json) | Type | Default |
| <a id="quarkus-logging-json_quarkus-log-socket-json-enabled"></a> [`quarkus.log.socket.json.enabled`](#quarkus-logging-json_quarkus-log-socket-json-enabled) config_property_copy_button:+++quarkus.log.socket.json.enabled+++[]<br><br>Determine whether to enable the JSON console formatting extension, which disables "normal" console formatting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_ENABLED+++[] | boolean |  |
| <a id="quarkus-logging-json_quarkus-log-socket-json-pretty-print"></a> [`quarkus.log.socket.json.pretty-print`](#quarkus-logging-json_quarkus-log-socket-json-pretty-print) config_property_copy_button:+++quarkus.log.socket.json.pretty-print+++[]<br><br>Enable "pretty printing" of the JSON record. Note that some JSON parsers will fail to read the pretty printed output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_PRETTY_PRINT+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-date-format"></a> [`quarkus.log.socket.json.date-format`](#quarkus-logging-json_quarkus-log-socket-json-date-format) config_property_copy_button:+++quarkus.log.socket.json.date-format+++[]<br><br>The date format to use. The special string "default" indicates that the default format should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_DATE_FORMAT+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-record-delimiter"></a> [`quarkus.log.socket.json.record-delimiter`](#quarkus-logging-json_quarkus-log-socket-json-record-delimiter) config_property_copy_button:+++quarkus.log.socket.json.record-delimiter+++[]<br><br>The special end-of-record delimiter to be used. By default, newline is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_RECORD_DELIMITER+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-socket-json-zone-id"></a> [`quarkus.log.socket.json.zone-id`](#quarkus-logging-json_quarkus-log-socket-json-zone-id) config_property_copy_button:+++quarkus.log.socket.json.zone-id+++[]<br><br>The zone ID to use. The special string "default" indicates that the default zone should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_ZONE_ID+++[] | string | `default` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-exception-output-type"></a> [`quarkus.log.socket.json.exception-output-type`](#quarkus-logging-json_quarkus-log-socket-json-exception-output-type) config_property_copy_button:+++quarkus.log.socket.json.exception-output-type+++[]<br><br>The exception output type to specify.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_EXCEPTION_OUTPUT_TYPE+++[] | `detailed`, `formatted`, `detailed-and-formatted` | `detailed` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-print-details"></a> [`quarkus.log.socket.json.print-details`](#quarkus-logging-json_quarkus-log-socket-json-print-details) config_property_copy_button:+++quarkus.log.socket.json.print-details+++[]<br><br>Enable printing of more details in the log.<br><br>Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_PRINT_DETAILS+++[] | boolean | `false` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-key-overrides"></a> [`quarkus.log.socket.json.key-overrides`](#quarkus-logging-json_quarkus-log-socket-json-key-overrides) config_property_copy_button:+++quarkus.log.socket.json.key-overrides+++[]<br><br>Override keys with custom values. Omitting this value indicates that no key overrides will be applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_KEY_OVERRIDES+++[] | string |  |
| <a id="quarkus-logging-json_quarkus-log-socket-json-excluded-keys"></a> [`quarkus.log.socket.json.excluded-keys`](#quarkus-logging-json_quarkus-log-socket-json-excluded-keys) config_property_copy_button:+++quarkus.log.socket.json.excluded-keys+++[]<br><br>Keys to be excluded from the JSON output.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_EXCLUDED_KEYS+++[] | list of string |  |
| <a id="quarkus-logging-json_quarkus-log-socket-json-additional-field-field-name-value"></a> [`quarkus.log.socket.json.additional-field."field-name".value`](#quarkus-logging-json_quarkus-log-socket-json-additional-field-field-name-value) config_property_copy_button:+++quarkus.log.socket.json.additional-field."field-name".value+++[]<br><br>Additional field value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_ADDITIONAL_FIELD__FIELD_NAME__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-logging-json_quarkus-log-socket-json-additional-field-field-name-type"></a> [`quarkus.log.socket.json.additional-field."field-name".type`](#quarkus-logging-json_quarkus-log-socket-json-additional-field-field-name-type) config_property_copy_button:+++quarkus.log.socket.json.additional-field."field-name".type+++[]<br><br>Additional field type specification. Supported types: `string`, `int`, and `long`. String is the default if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_ADDITIONAL_FIELD__FIELD_NAME__TYPE+++[] | `string`, `int`, `long` | `string` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-log-format"></a> [`quarkus.log.socket.json.log-format`](#quarkus-logging-json_quarkus-log-socket-json-log-format) config_property_copy_button:+++quarkus.log.socket.json.log-format+++[]<br><br>Specify the format of the produced JSON<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_LOG_FORMAT+++[] | `default`, `ecs`, `gcp` | `default` |
| <a id="quarkus-logging-json_quarkus-log-socket-json-mdc-flat-fields"></a> [`quarkus.log.socket.json.mdc.flat-fields`](#quarkus-logging-json_quarkus-log-socket-json-mdc-flat-fields) config_property_copy_button:+++quarkus.log.socket.json.mdc.flat-fields+++[]<br><br>When true, MDC entries are written as root-level JSON fields instead of being grouped under a nested `"mdc"` object.<br><br>This is useful when log aggregators (such as OpenSearch or Elasticsearch) expect MDC fields to be queryable as top-level document fields rather than nested properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_JSON_MDC_FLAT_FIELDS+++[] | boolean | `false` |

<a id="memory-size-note-anchor-quarkus-logging-json_quarkus-log"></a>

> [!NOTE]
> **About the MemorySize format**
>
> A size configuration option recognizes strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.
>
> If no suffix is given, assume bytes.

> [!WARNING]
> Enabling pretty printing might cause certain processors and JSON parsers to fail.

> [!NOTE]
> Printing the details can be expensive as the values are retrieved from the caller. The details include the source class name, source file name, source method name, and source line number.

## Log handlers

A log handler is a logging component that emits log events to a recipient. Quarkus includes several different log handlers: **console**, **file**, and **syslog**.

The featured examples use `com.example` as a logging category.

### Console log handler

The console log handler is enabled by default, and it directs all log events to the application's console, usually the system's `stdout`.

- A global configuration example:

  ```properties
  quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss} %-5p [%c] (%t) %s%e%n
  ```


- A per-category configuration example:

  ```properties
  quarkus.log.handler.console.my-console-handler.format=%d{yyyy-MM-dd HH:mm:ss} [com.example] %s%e%n

  quarkus.log.category."com.example".handlers=my-console-handler
  quarkus.log.category."com.example".use-parent-handlers=false
  ```


For details about its configuration, see the [console logging configuration](#quarkus-core_section_quarkus-log-console) reference.

### File log handler

To log events to a file on the application's host, use the Quarkus file log handler. The file log handler is disabled by default, so you must enable it first.

The Quarkus file log handler supports log file rotation.

Log file rotation ensures efficient log management by preserving a specified number of backup files while keeping the primary log file up to date and manageable in size.

- A global configuration example:

  ```properties
  quarkus.log.file.enabled=true
  quarkus.log.file.path=application.log
  quarkus.log.file.format=%d{yyyy-MM-dd HH:mm:ss} %-5p [%c] (%t) %s%e%n
  ```


- A per-category configuration example:

  ```properties
  quarkus.log.handler.file.my-file-handler.enabled=true
  quarkus.log.handler.file.my-file-handler.path=application.log
  quarkus.log.handler.file.my-file-handler.format=%d{yyyy-MM-dd HH:mm:ss} [com.example] %s%e%n

  quarkus.log.category."com.example".handlers=my-file-handler
  quarkus.log.category."com.example".use-parent-handlers=false
  ```


For details about its configuration, see the [file logging configuration](#quarkus-core_section_quarkus-log-file) reference.

### Syslog log handler

The syslog handler in Quarkus follows the [Syslog](https://en.wikipedia.org/wiki/Syslog) protocol, which is used to send log messages on UNIX-like systems. It uses the protocol defined in [RFC 5424](https://tools.ietf.org/html/rfc5424).

By default, the syslog handler is disabled. When enabled, it sends all log events to a syslog server, typically the local syslog server for the application.

- A global configuration example:

  ```properties
  quarkus.log.syslog.enabled=true
  quarkus.log.syslog.app-name=my-application
  quarkus.log.syslog.format=%d{yyyy-MM-dd HH:mm:ss} %-5p [%c] (%t) %s%e%n
  ```


- A per-category configuration example:

  ```properties
  quarkus.log.handler.syslog.my-syslog-handler.enabled=true
  quarkus.log.handler.syslog.my-syslog-handler.app-name=my-application
  quarkus.log.handler.syslog.my-syslog-handler.format=%d{yyyy-MM-dd HH:mm:ss} [com.example] %s%e%n

  quarkus.log.category."com.example".handlers=my-syslog-handler
  quarkus.log.category."com.example".use-parent-handlers=false
  ```


For details about its configuration, see the [Syslog logging configuration](#quarkus-core_section_quarkus-log-syslog) reference.

### Socket log handler

This handler sends logs to a socket. The socket log handler is disabled by default; enable it in your configuration. When enabled, it sends all log events to a socket, such as a Logstash server.

- A global configuration example:

  ```properties
  quarkus.log.socket.enabled=true
  quarkus.log.socket.endpoint=localhost:4560
  ```


Typically, this handler is used with the `quarkus-logging-json` extension to send logs in ECS format to an Elasticsearch instance. For an example configuration, see the [Centralized log management](/guides/centralized-log-management/index.md) guide.

## Add a logging filter to your log handler

You can attach a [filter](https://docs.oracle.com/en/java/javase/17/docs/api/java.logging/java/util/logging/Filter.html) to a log handler such as the console handler. The filter determines whether the handler logs a given log record.

To register a logging filter:

1. Annotate a `final` class that implements `java.util.logging.Filter` with `@io.quarkus.logging.LoggingFilter`, and set the `name` property:

   .An example of writing a filter:

   ```java
   package com.example;

   import io.quarkus.logging.LoggingFilter;
   import java.util.logging.Filter;
   import java.util.logging.LogRecord;

   @LoggingFilter(name = "my-filter")
   public final class TestFilter implements Filter {

       private final String part;

       public TestFilter(@ConfigProperty(name = "my-filter.part") String part) {
           this.part = part;
       }

       @Override
       public boolean isLoggable(LogRecord record) {
           return !record.getMessage().contains(part);
       }
   }
   ```

   In this example, the filter excludes log records containing specific text from console logs. The specific text to filter on is not hard-coded; instead, it is read from the `my-filter.part` configuration property.

   .An example of Configuring the filter in `application.properties`:

   ```properties
   my-filter.part=TEST
   ```


1. Attach the filter to the corresponding handler using the `filter` configuration property, located in `application.properties`:

   ```properties
   quarkus.log.console.filter=my-filter
   ```


## Examples of logging configurations

The following examples show how to configure logging in Quarkus:

**Console DEBUG logging except for Quarkus logs (INFO), no color, shortened time, shortened category prefixes**

```properties
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
quarkus.log.console.level=DEBUG
quarkus.console.color=false

quarkus.log.category."io.quarkus".level=INFO
```

> [!NOTE]
> If you add these properties on the command line, ensure the `"` character is escaped with a backslash. For example, `-Dquarkus.log.category.\"io.quarkus\".level=DEBUG`.

<a id="category-example"></a>

**File TRACE logging configuration**

```properties
quarkus.log.file.enabled=true
# Send output to a trace.log file under the /tmp directory
quarkus.log.file.path=/tmp/trace.log
quarkus.log.file.level=TRACE
quarkus.log.file.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
# Set 2 categories (io.quarkus.smallrye.jwt, io.undertow.request.security) to TRACE level
quarkus.log.min-level=TRACE
quarkus.log.category."io.quarkus.smallrye.jwt".level=TRACE
quarkus.log.category."io.undertow.request.security".level=TRACE
```

> [!NOTE]
> Because this example does not change the root logger, the console log contains only `INFO` or higher level logs.

<a id="category-named-handlers-example"></a>

**Named handlers attached to a category**

```properties
# Send output to a trace.log file under the /tmp directory
quarkus.log.file.path=/tmp/trace.log
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
# Configure a named handler that logs to console
quarkus.log.handler.console."STRUCTURED_LOGGING".format=%e%n
# Configure a named handler that logs to file
quarkus.log.handler.file."STRUCTURED_LOGGING_FILE".enabled=true
quarkus.log.handler.file."STRUCTURED_LOGGING_FILE".format=%e%n
# Configure the category and link the two named handlers to it
quarkus.log.category."io.quarkus.category".level=INFO
quarkus.log.category."io.quarkus.category".handlers=STRUCTURED_LOGGING,STRUCTURED_LOGGING_FILE
```

<a id="root-category-named-handlers-example"></a>

**Named handlers attached to the root logger**

```properties
# configure a named file handler that sends the output to 'quarkus.log'
quarkus.log.handler.file.CONSOLE_MIRROR.enabled=true
quarkus.log.handler.file.CONSOLE_MIRROR.path=quarkus.log
# attach the handler to the root logger
quarkus.log.handlers=CONSOLE_MIRROR
```

## Centralized log management

Use a centralized location to efficiently collect, store, and analyze log data from various components and application instances.

To send logs to a centralized tool such as Graylog, Logstash, or Fluentd, see the Quarkus [Centralized log management](/guides/centralized-log-management/index.md) guide.

### OpenTelemetry logging

You can enable sending log entries from all appenders to OpenTelemetry Logging.

For details, see the Quarkus [OpenTelemetry Logging](/guides/opentelemetry-logging/index.md) guide.

## Configure logging for `@QuarkusTest`

To enable logging for `@QuarkusTest`, it is necessary to set the `java.util.logging.manager` system property to `org.jboss.logmanager.LogManager` early in the startup process, or logging will not work as expected.

Configure it in your build tool as follows:

- For Maven:

  Set the property in the Maven Surefire plugin configuration.

  .Setting `java.util.logging.manager` in the Maven Surefire plugin configuration:

  ```xml
  <build>
    <plugins>
      <plugin>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>${surefire-plugin.version}</version>
        <configuration>
          <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager> <1>
            <quarkus.log.level>DEBUG</quarkus.log.level> <2>
            <maven.home>${maven.home}</maven.home>
          </systemPropertyVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
  ```

  1. Make sure the `org.jboss.logmanager.LogManager` is used.
  2. Enable debug logging for all logging categories.


- For Gradle:

  Set the property in the `build.gradle` file.

  ```groovy
  test {
      systemProperty "java.util.logging.manager", "org.jboss.logmanager.LogManager"
  }
  ```


For more information, see the [Running `@QuarkusTest` from an IDE](/guides/getting-started-testing/index.md#test-from-ide) section of the Testing your Application guide.

<a id="logging-apis"></a>
## Use other logging APIs

Quarkus relies on the JBoss Logging library for logging. If your dependencies use other logging APIs, such as Apache Commons Logging, Log4j, or SLF4J, exclude those libraries and use a JBoss Logging adapter. This step is especially important for native executables, because the native build can fail with errors such as the following:

```text
Caused by java.lang.ClassNotFoundException: org.apache.commons.logging.impl.LogFactoryImpl
```

The native executable does not include the logging implementation for those APIs. Use JBoss Logging adapters to resolve this issue.

The next section describes adapters for common open-source logging APIs.

<a id="logging-adapters"></a>
### Add a logging adapter to your application

For each logging API that is not JBoss Logging, add a logging adapter library to ensure that messages logged through these APIs are routed to the JBoss Log Manager backend. You can then verify whether the logs generated by the added library adhere to the same format as the other Quarkus logs.

> [!NOTE]
> This step is unnecessary for libraries that are dependencies of a Quarkus extension where the extension handles it automatically.

<a id="logging-adapters-slf4j"></a>
#### SLF4J

The SLF4J adapter is provided by the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>org.jboss.slf4j</groupId>
    <artifactId>slf4j-jboss-logmanager</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("org.jboss.slf4j:slf4j-jboss-logmanager")
```

<a id="logging-adapters-commons-logging"></a>
#### Apache Commons Logging

The Commons Logging adapter is provided by the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>org.jboss.logging</groupId>
    <artifactId>commons-logging-jboss-logging</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("org.jboss.logging:commons-logging-jboss-logging")
```

<a id="logging-adapters-log4j2"></a>
#### Log4j 2

The Log4j 2 adapter is provided by the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>org.jboss.logmanager</groupId>
    <artifactId>log4j2-jboss-logmanager</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("org.jboss.logmanager:log4j2-jboss-logmanager")
```

> [!NOTE]
> Do not include any Log4j dependencies, as the `log4j2-jboss-logmanager` library contains everything needed to use Log4j as a logging implementation.

### Use MDC to add contextual log information

Quarkus overrides the logging Mapped Diagnostic Context (MDC) to improve compatibility with its reactive core.

#### Add and read MDC data

To add data to the MDC and extract it in your log output:

1. Use the `MDC` class to set the data.
   1. Add `import org.jboss.logmanager.MDC;`
   2. Set `MDC.put(...)` as shown in the example below:

      .An example with JBoss Logging and `io.quarkus.logging.Log`

      ```
      package me.sample;

      import io.quarkus.logging.Log;
      import jakarta.ws.rs.GET;
      import jakarta.ws.rs.Path;
      import org.jboss.logmanager.MDC;

      import java.util.UUID;

      @Path("/hello/jboss")
      public class GreetingResourceJbossLogging {

          @GET
          @Path("/test")
          public String greeting() {
              MDC.put("request.id", UUID.randomUUID().toString());
              MDC.put("request.path", "/hello/test");
              Log.info("request received");
              return "hello world!";
          }
      }
      ```

1. Configure the log format to use `%X{mdc-key}`:

   ```properties
   quarkus.log.console.format=%d{HH:mm:ss} %-5p request.id=%X{request.id} request.path=%X{request.path} [%c{2.}] (%t) %s%n
   ```

   The resulting message contains the MDC data:

   ```text
   08:48:13 INFO request.id=c37a3a36-b7f6-4492-83a1-de41dbc26fe2 request.path=/hello/test [me.sa.GreetingResourceJbossLogging] (executor-thread-1) request received
   ```


#### MDC and supported logging APIs

Based on your logging API, use one of the following MDC classes:

- Log4j 2 - `org.apache.logging.log4j.ThreadContext.put(key, value)`
- SLF4J - `org.slf4j.MDC.put(key, value)`

<a id="mdc-propagation"></a>
#### MDC propagation

In Quarkus, the MDC provider has a specific implementation for handling the reactive context, ensuring that MDC data is propagated during reactive and asynchronous processing.

As a result, you can still access the MDC data in various scenarios:

- After asynchronous calls, for example, when a REST client returns a Uni.
- In code submitted to `org.eclipse.microprofile.context.ManagedExecutor`.
- In code executed with `vertx.executeBlocking()`.

> [!NOTE]
> If applicable, Quarkus stores MDC data in a Vert.x duplicated context, which isolates processing for a single task or request.

<a id="loggingConfigurationReference"></a>
## Logging configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-core_quarkus-log-metrics-enabled"></a> [`quarkus.log.metrics.enabled`](#quarkus-core_quarkus-log-metrics-enabled) config_property_copy_button:+++quarkus.log.metrics.enabled+++[]<br><br>If enabled and a metrics extension is present, logging metrics are published.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_METRICS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-log-min-level"></a> [`quarkus.log.min-level`](#quarkus-core_quarkus-log-min-level) config_property_copy_button:+++quarkus.log.min-level+++[]<br><br>The default minimum log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_MIN_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `DEBUG` |
| [lock] <a id="quarkus-core_quarkus-log-decorate-stacktraces"></a> [`quarkus.log.decorate-stacktraces`](#quarkus-core_quarkus-log-decorate-stacktraces) config_property_copy_button:+++quarkus.log.decorate-stacktraces+++[]<br><br>This will decorate the stacktrace in dev mode to show the line in the code that cause the exception<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_DECORATE_STACKTRACES+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-level"></a> [`quarkus.log.level`](#quarkus-core_quarkus-log-level) config_property_copy_button:+++quarkus.log.level+++[]<br><br>The log level of the root category, which is used as the default log level for all categories.<br><br>JBoss Logging supports Apache-style log levels:<br><br>\* `org.jboss.logmanager.Level#FATAL` \* `org.jboss.logmanager.Level#ERROR` \* `org.jboss.logmanager.Level#WARN` \* `org.jboss.logmanager.Level#INFO` \* `org.jboss.logmanager.Level#DEBUG` \* `org.jboss.logmanager.Level#TRACE`<br><br>In addition, it also supports the standard JDK log levels.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `INFO` |
| <a id="quarkus-core_quarkus-log-handlers"></a> [`quarkus.log.handlers`](#quarkus-core_quarkus-log-handlers) config_property_copy_button:+++quarkus.log.handlers+++[]<br><br>The names of additional handlers to link to the root category. These handlers are defined in consoleHandlers, fileHandlers, or syslogHandlers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLERS+++[] | list of string |  |
| <a id="quarkus-core_section_quarkus-log-category"></a> [Minimum logging categories](#quarkus-core_section_quarkus-log-category) | Type | Default |
| [lock] <a id="quarkus-core_quarkus-log-category-categories-min-level"></a> [`quarkus.log.category."categories".min-level`](#quarkus-core_quarkus-log-category-categories-min-level) config_property_copy_button:+++quarkus.log.category."categories".min-level+++[]<br><br>The minimum log level for this category. By default, all categories are configured with `DEBUG` minimum level.<br><br>To get runtime logging below `DEBUG`, e.g., `TRACE`, adjust the minimum level at build time. The right log level needs to be provided at runtime.<br><br>As an example, to get `TRACE` logging, minimum level needs to be at `TRACE`, and the runtime log level needs to match that.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__MIN_LEVEL+++[] | InheritableLevel | `inherit` |
| <a id="quarkus-core_quarkus-log-category-categories-level"></a> [`quarkus.log.category."categories".level`](#quarkus-core_quarkus-log-category-categories-level) config_property_copy_button:+++quarkus.log.category."categories".level+++[]<br><br>The log level for this category.<br><br>Note that to get log levels below `INFO`, the minimum level build-time configuration option also needs to be adjusted.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__LEVEL+++[] | InheritableLevel | `inherit` |
| <a id="quarkus-core_quarkus-log-category-categories-handlers"></a> [`quarkus.log.category."categories".handlers`](#quarkus-core_quarkus-log-category-categories-handlers) config_property_copy_button:+++quarkus.log.category."categories".handlers+++[]<br><br>The names of the handlers to link to this category.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__HANDLERS+++[] | list of string |  |
| <a id="quarkus-core_quarkus-log-category-categories-use-parent-handlers"></a> [`quarkus.log.category."categories".use-parent-handlers`](#quarkus-core_quarkus-log-category-categories-use-parent-handlers) config_property_copy_button:+++quarkus.log.category."categories".use-parent-handlers+++[]<br><br>Specify whether this logger should send its output to its parent Logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CATEGORY__CATEGORIES__USE_PARENT_HANDLERS+++[] | boolean | `true` |
| <a id="quarkus-core_section_quarkus-log-console"></a> [Console logging](#quarkus-core_section_quarkus-log-console) | Type | Default |
| <a id="quarkus-core_quarkus-log-console-enabled"></a> [`quarkus.log.console.enabled`](#quarkus-core_quarkus-log-console-enabled) config_property_copy_button:+++quarkus.log.console.enabled+++[]<br><br>If console logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-console-stderr"></a> [`quarkus.log.console.stderr`](#quarkus-core_quarkus-log-console-stderr) config_property_copy_button:+++quarkus.log.console.stderr+++[]<br><br>If console logging should go to `System#err` instead of `System#out`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_STDERR+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-console-format"></a> [`quarkus.log.console.format`](#quarkus-core_quarkus-log-console-format) config_property_copy_button:+++quarkus.log.console.format+++[]<br><br>The log format. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-console-level"></a> [`quarkus.log.console.level`](#quarkus-core_quarkus-log-console-level) config_property_copy_button:+++quarkus.log.console.level+++[]<br><br>The console log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-console-darken"></a> [`quarkus.log.console.darken`](#quarkus-core_quarkus-log-console-darken) config_property_copy_button:+++quarkus.log.console.darken+++[]<br><br>Specify how much the colors should be darkened. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_DARKEN+++[] | int | `0` |
| <a id="quarkus-core_quarkus-log-console-filter"></a> [`quarkus.log.console.filter`](#quarkus-core_quarkus-log-console-filter) config_property_copy_button:+++quarkus.log.console.filter+++[]<br><br>The name of the filter to link to the console handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-console-async-enabled"></a> [`quarkus.log.console.async.enabled`](#quarkus-core_quarkus-log-console-async-enabled) config_property_copy_button:+++quarkus.log.console.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-console-async-queue-length"></a> [`quarkus.log.console.async.queue-length`](#quarkus-core_quarkus-log-console-async-queue-length) config_property_copy_button:+++quarkus.log.console.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-console-async-overflow"></a> [`quarkus.log.console.async.overflow`](#quarkus-core_quarkus-log-console-async-overflow) config_property_copy_button:+++quarkus.log.console.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_CONSOLE_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_section_quarkus-log-file"></a> [File logging](#quarkus-core_section_quarkus-log-file) | Type | Default |
| <a id="quarkus-core_quarkus-log-file-enabled"></a> [`quarkus.log.file.enabled`](#quarkus-core_quarkus-log-file-enabled) config_property_copy_button:+++quarkus.log.file.enabled+++[]<br><br>If file logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-file-format"></a> [`quarkus.log.file.format`](#quarkus-core_quarkus-log-file-format) config_property_copy_button:+++quarkus.log.file.format+++[]<br><br>The log format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-file-level"></a> [`quarkus.log.file.level`](#quarkus-core_quarkus-log-file-level) config_property_copy_button:+++quarkus.log.file.level+++[]<br><br>The level of logs to be written into the file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-file-path"></a> [`quarkus.log.file.path`](#quarkus-core_quarkus-log-file-path) config_property_copy_button:+++quarkus.log.file.path+++[]<br><br>The name of the file in which logs will be written.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_PATH+++[] | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | `quarkus.log` |
| <a id="quarkus-core_quarkus-log-file-filter"></a> [`quarkus.log.file.filter`](#quarkus-core_quarkus-log-file-filter) config_property_copy_button:+++quarkus.log.file.filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-file-encoding"></a> [`quarkus.log.file.encoding`](#quarkus-core_quarkus-log-file-encoding) config_property_copy_button:+++quarkus.log.file.encoding+++[]<br><br>The character encoding used<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ENCODING+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) |  |
| <a id="quarkus-core_quarkus-log-file-async-enabled"></a> [`quarkus.log.file.async.enabled`](#quarkus-core_quarkus-log-file-async-enabled) config_property_copy_button:+++quarkus.log.file.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-file-async-queue-length"></a> [`quarkus.log.file.async.queue-length`](#quarkus-core_quarkus-log-file-async-queue-length) config_property_copy_button:+++quarkus.log.file.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-file-async-overflow"></a> [`quarkus.log.file.async.overflow`](#quarkus-core_quarkus-log-file-async-overflow) config_property_copy_button:+++quarkus.log.file.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_quarkus-log-file-rotation-enabled"></a> [`quarkus.log.file.rotation.enabled`](#quarkus-core_quarkus-log-file-rotation-enabled) config_property_copy_button:+++quarkus.log.file.rotation.enabled+++[]<br><br>Whether log rotation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-file-rotation-max-file-size"></a> [`quarkus.log.file.rotation.max-file-size`](#quarkus-core_quarkus-log-file-rotation-max-file-size) config_property_copy_button:+++quarkus.log.file.rotation.max-file-size+++[]<br><br>The maximum log file size, after which a rotation is executed, up to `Long.MAX_VALUE` bytes. Note that the file is rotated *after* the log record is written. Thus, this isn't a hard maximum on the file size; rather, it's a hard *minimum* on the size of the file before it is rotated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_MAX_FILE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-core_quarkus-log) | `10M` |
| <a id="quarkus-core_quarkus-log-file-rotation-max-backup-index"></a> [`quarkus.log.file.rotation.max-backup-index`](#quarkus-core_quarkus-log-file-rotation-max-backup-index) config_property_copy_button:+++quarkus.log.file.rotation.max-backup-index+++[]<br><br>The maximum number of backups to keep.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_MAX_BACKUP_INDEX+++[] | int | `5` |
| <a id="quarkus-core_quarkus-log-file-rotation-file-suffix"></a> [`quarkus.log.file.rotation.file-suffix`](#quarkus-core_quarkus-log-file-rotation-file-suffix) config_property_copy_button:+++quarkus.log.file.rotation.file-suffix+++[]<br><br>The file handler rotation file suffix. When used, the file will be rotated based on its suffix.<br><br>The suffix must be in a date-time format that is understood by `DateTimeFormatter`.<br><br>Example fileSuffix: .yyyy-MM-dd<br><br>Note: If the suffix ends with .zip or .gz, the rotation file will also be compressed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_FILE_SUFFIX+++[] | string |  |
| <a id="quarkus-core_quarkus-log-file-rotation-rotate-on-boot"></a> [`quarkus.log.file.rotation.rotate-on-boot`](#quarkus-core_quarkus-log-file-rotation-rotate-on-boot) config_property_copy_button:+++quarkus.log.file.rotation.rotate-on-boot+++[]<br><br>Indicates whether to rotate log files on server initialization.<br><br>You need to either set a `max-file-size` or configure a `file-suffix` for it to work.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILE_ROTATION_ROTATE_ON_BOOT+++[] | boolean | `true` |
| <a id="quarkus-core_section_quarkus-log-syslog"></a> [Syslog logging](#quarkus-core_section_quarkus-log-syslog) | Type | Default |
| <a id="quarkus-core_quarkus-log-syslog-enabled"></a> [`quarkus.log.syslog.enabled`](#quarkus-core_quarkus-log-syslog-enabled) config_property_copy_button:+++quarkus.log.syslog.enabled+++[]<br><br>If syslog logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-syslog-endpoint"></a> [`quarkus.log.syslog.endpoint`](#quarkus-core_quarkus-log-syslog-endpoint) config_property_copy_button:+++quarkus.log.syslog.endpoint+++[]<br><br>The IP address and port of the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ENDPOINT+++[] | host:port | `localhost:514` |
| <a id="quarkus-core_quarkus-log-syslog-app-name"></a> [`quarkus.log.syslog.app-name`](#quarkus-core_quarkus-log-syslog-app-name) config_property_copy_button:+++quarkus.log.syslog.app-name+++[]<br><br>The app name used when formatting the message in RFC5424 format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_APP_NAME+++[] | string |  |
| <a id="quarkus-core_quarkus-log-syslog-hostname"></a> [`quarkus.log.syslog.hostname`](#quarkus-core_quarkus-log-syslog-hostname) config_property_copy_button:+++quarkus.log.syslog.hostname+++[]<br><br>The name of the host the messages are being sent from<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_HOSTNAME+++[] | string |  |
| <a id="quarkus-core_quarkus-log-syslog-facility"></a> [`quarkus.log.syslog.facility`](#quarkus-core_quarkus-log-syslog-facility) config_property_copy_button:+++quarkus.log.syslog.facility+++[]<br><br>Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_FACILITY+++[] | `kernel`, `user-level`, `mail-system`, `system-daemons`, `security`, `syslogd`, `line-printer`, `network-news`, `uucp`, `clock-daemon`, `security2`, `ftp-daemon`, `ntp`, `log-audit`, `log-alert`, `clock-daemon2`, `local-use-0`, `local-use-1`, `local-use-2`, `local-use-3`, `local-use-4`, `local-use-5`, `local-use-6`, `local-use-7` | `user-level` |
| <a id="quarkus-core_quarkus-log-syslog-syslog-type"></a> [`quarkus.log.syslog.syslog-type`](#quarkus-core_quarkus-log-syslog-syslog-type) config_property_copy_button:+++quarkus.log.syslog.syslog-type+++[]<br><br>Set the `SyslogType syslog type` this handler should use to format the message sent<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_SYSLOG_TYPE+++[] | `rfc5424`, `rfc3164` | `rfc5424` |
| <a id="quarkus-core_quarkus-log-syslog-protocol"></a> [`quarkus.log.syslog.protocol`](#quarkus-core_quarkus-log-syslog-protocol) config_property_copy_button:+++quarkus.log.syslog.protocol+++[]<br><br>Sets the protocol used to connect to the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-core_quarkus-log-syslog-use-counting-framing"></a> [`quarkus.log.syslog.use-counting-framing`](#quarkus-core_quarkus-log-syslog-use-counting-framing) config_property_copy_button:+++quarkus.log.syslog.use-counting-framing+++[]<br><br>If enabled, the message being sent is prefixed with the size of the message<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_USE_COUNTING_FRAMING+++[] | `true`, `false`, `protocol-dependent` | `protocol-dependent` |
| <a id="quarkus-core_quarkus-log-syslog-truncate"></a> [`quarkus.log.syslog.truncate`](#quarkus-core_quarkus-log-syslog-truncate) config_property_copy_button:+++quarkus.log.syslog.truncate+++[]<br><br>Set to `true` to truncate the message if it exceeds maximum length<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_TRUNCATE+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-syslog-block-on-reconnect"></a> [`quarkus.log.syslog.block-on-reconnect`](#quarkus-core_quarkus-log-syslog-block-on-reconnect) config_property_copy_button:+++quarkus.log.syslog.block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-syslog-format"></a> [`quarkus.log.syslog.format`](#quarkus-core_quarkus-log-syslog-format) config_property_copy_button:+++quarkus.log.syslog.format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-syslog-level"></a> [`quarkus.log.syslog.level`](#quarkus-core_quarkus-log-syslog-level) config_property_copy_button:+++quarkus.log.syslog.level+++[]<br><br>The log level specifying what message levels will be logged by the Syslog logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-syslog-filter"></a> [`quarkus.log.syslog.filter`](#quarkus-core_quarkus-log-syslog-filter) config_property_copy_button:+++quarkus.log.syslog.filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-syslog-max-length"></a> [`quarkus.log.syslog.max-length`](#quarkus-core_quarkus-log-syslog-max-length) config_property_copy_button:+++quarkus.log.syslog.max-length+++[]<br><br>The maximum length, in bytes, of the message allowed to be sent, up to `Integer.MAX_VALUE` bytes. The length includes the header and the message.<br><br>If not set, the default value is `2048` when `sys-log-type` is `rfc5424` (which is the default) and `1024` when `sys-log-type` is `rfc3164`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_MAX_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-core_quarkus-log) |  |
| <a id="quarkus-core_quarkus-log-syslog-async-enabled"></a> [`quarkus.log.syslog.async.enabled`](#quarkus-core_quarkus-log-syslog-async-enabled) config_property_copy_button:+++quarkus.log.syslog.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-syslog-async-queue-length"></a> [`quarkus.log.syslog.async.queue-length`](#quarkus-core_quarkus-log-syslog-async-queue-length) config_property_copy_button:+++quarkus.log.syslog.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-syslog-async-overflow"></a> [`quarkus.log.syslog.async.overflow`](#quarkus-core_quarkus-log-syslog-async-overflow) config_property_copy_button:+++quarkus.log.syslog.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SYSLOG_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_section_quarkus-log-socket"></a> [Socket logging](#quarkus-core_section_quarkus-log-socket) | Type | Default |
| <a id="quarkus-core_quarkus-log-socket-enabled"></a> [`quarkus.log.socket.enabled`](#quarkus-core_quarkus-log-socket-enabled) config_property_copy_button:+++quarkus.log.socket.enabled+++[]<br><br>If socket logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-socket-endpoint"></a> [`quarkus.log.socket.endpoint`](#quarkus-core_quarkus-log-socket-endpoint) config_property_copy_button:+++quarkus.log.socket.endpoint+++[]<br><br>The IP address and port of the server receiving the logs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ENDPOINT+++[] | host:port | `localhost:4560` |
| <a id="quarkus-core_quarkus-log-socket-protocol"></a> [`quarkus.log.socket.protocol`](#quarkus-core_quarkus-log-socket-protocol) config_property_copy_button:+++quarkus.log.socket.protocol+++[]<br><br>Sets the protocol used to connect to the syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-core_quarkus-log-socket-block-on-reconnect"></a> [`quarkus.log.socket.block-on-reconnect`](#quarkus-core_quarkus-log-socket-block-on-reconnect) config_property_copy_button:+++quarkus.log.socket.block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-socket-format"></a> [`quarkus.log.socket.format`](#quarkus-core_quarkus-log-socket-format) config_property_copy_button:+++quarkus.log.socket.format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-socket-level"></a> [`quarkus.log.socket.level`](#quarkus-core_quarkus-log-socket-level) config_property_copy_button:+++quarkus.log.socket.level+++[]<br><br>The log level specifying, which message levels will be logged by socket logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-socket-filter"></a> [`quarkus.log.socket.filter`](#quarkus-core_quarkus-log-socket-filter) config_property_copy_button:+++quarkus.log.socket.filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-socket-async-enabled"></a> [`quarkus.log.socket.async.enabled`](#quarkus-core_quarkus-log-socket-async-enabled) config_property_copy_button:+++quarkus.log.socket.async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-socket-async-queue-length"></a> [`quarkus.log.socket.async.queue-length`](#quarkus-core_quarkus-log-socket-async-queue-length) config_property_copy_button:+++quarkus.log.socket.async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-socket-async-overflow"></a> [`quarkus.log.socket.async.overflow`](#quarkus-core_quarkus-log-socket-async-overflow) config_property_copy_button:+++quarkus.log.socket.async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_SOCKET_ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_section_quarkus-log-handler-console"></a> [Console handlers](#quarkus-core_section_quarkus-log-handler-console) | Type | Default |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-enabled"></a> [`quarkus.log.handler.console."console-handlers".enabled`](#quarkus-core_quarkus-log-handler-console-console-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".enabled+++[]<br><br>If console logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ENABLED+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-stderr"></a> [`quarkus.log.handler.console."console-handlers".stderr`](#quarkus-core_quarkus-log-handler-console-console-handlers-stderr) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".stderr+++[]<br><br>If console logging should go to `System#err` instead of `System#out`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__STDERR+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-format"></a> [`quarkus.log.handler.console."console-handlers".format`](#quarkus-core_quarkus-log-handler-console-console-handlers-format) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".format+++[]<br><br>The log format. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-level"></a> [`quarkus.log.handler.console."console-handlers".level`](#quarkus-core_quarkus-log-handler-console-console-handlers-level) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".level+++[]<br><br>The console log level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-darken"></a> [`quarkus.log.handler.console."console-handlers".darken`](#quarkus-core_quarkus-log-handler-console-console-handlers-darken) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".darken+++[]<br><br>Specify how much the colors should be darkened. Note that this value is ignored if an extension is present that takes control of console formatting (e.g., an XML or JSON-format extension).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__DARKEN+++[] | int | `0` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-filter"></a> [`quarkus.log.handler.console."console-handlers".filter`](#quarkus-core_quarkus-log-handler-console-console-handlers-filter) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".filter+++[]<br><br>The name of the filter to link to the console handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-async-enabled"></a> [`quarkus.log.handler.console."console-handlers".async.enabled`](#quarkus-core_quarkus-log-handler-console-console-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-async-queue-length"></a> [`quarkus.log.handler.console."console-handlers".async.queue-length`](#quarkus-core_quarkus-log-handler-console-console-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-handler-console-console-handlers-async-overflow"></a> [`quarkus.log.handler.console."console-handlers".async.overflow`](#quarkus-core_quarkus-log-handler-console-console-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.console."console-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_CONSOLE__CONSOLE_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_section_quarkus-log-handler-file"></a> [File handlers](#quarkus-core_section_quarkus-log-handler-file) | Type | Default |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-enabled"></a> [`quarkus.log.handler.file."file-handlers".enabled`](#quarkus-core_quarkus-log-handler-file-file-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".enabled+++[]<br><br>If file logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-format"></a> [`quarkus.log.handler.file."file-handlers".format`](#quarkus-core_quarkus-log-handler-file-file-handlers-format) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".format+++[]<br><br>The log format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{3.}] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-level"></a> [`quarkus.log.handler.file."file-handlers".level`](#quarkus-core_quarkus-log-handler-file-file-handlers-level) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".level+++[]<br><br>The level of logs to be written into the file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-path"></a> [`quarkus.log.handler.file."file-handlers".path`](#quarkus-core_quarkus-log-handler-file-file-handlers-path) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".path+++[]<br><br>The name of the file in which logs will be written.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__PATH+++[] | [File](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/File.html) | `quarkus.log` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-filter"></a> [`quarkus.log.handler.file."file-handlers".filter`](#quarkus-core_quarkus-log-handler-file-file-handlers-filter) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-encoding"></a> [`quarkus.log.handler.file."file-handlers".encoding`](#quarkus-core_quarkus-log-handler-file-file-handlers-encoding) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".encoding+++[]<br><br>The character encoding used<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ENCODING+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) |  |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-async-enabled"></a> [`quarkus.log.handler.file."file-handlers".async.enabled`](#quarkus-core_quarkus-log-handler-file-file-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-async-queue-length"></a> [`quarkus.log.handler.file."file-handlers".async.queue-length`](#quarkus-core_quarkus-log-handler-file-file-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-async-overflow"></a> [`quarkus.log.handler.file."file-handlers".async.overflow`](#quarkus-core_quarkus-log-handler-file-file-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-rotation-enabled"></a> [`quarkus.log.handler.file."file-handlers".rotation.enabled`](#quarkus-core_quarkus-log-handler-file-file-handlers-rotation-enabled) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.enabled+++[]<br><br>Whether log rotation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-rotation-max-file-size"></a> [`quarkus.log.handler.file."file-handlers".rotation.max-file-size`](#quarkus-core_quarkus-log-handler-file-file-handlers-rotation-max-file-size) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.max-file-size+++[]<br><br>The maximum log file size, after which a rotation is executed, up to `Long.MAX_VALUE` bytes. Note that the file is rotated *after* the log record is written. Thus, this isn't a hard maximum on the file size; rather, it's a hard *minimum* on the size of the file before it is rotated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_MAX_FILE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-core_quarkus-log) | `10M` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-rotation-max-backup-index"></a> [`quarkus.log.handler.file."file-handlers".rotation.max-backup-index`](#quarkus-core_quarkus-log-handler-file-file-handlers-rotation-max-backup-index) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.max-backup-index+++[]<br><br>The maximum number of backups to keep.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_MAX_BACKUP_INDEX+++[] | int | `5` |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-rotation-file-suffix"></a> [`quarkus.log.handler.file."file-handlers".rotation.file-suffix`](#quarkus-core_quarkus-log-handler-file-file-handlers-rotation-file-suffix) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.file-suffix+++[]<br><br>The file handler rotation file suffix. When used, the file will be rotated based on its suffix.<br><br>The suffix must be in a date-time format that is understood by `DateTimeFormatter`.<br><br>Example fileSuffix: .yyyy-MM-dd<br><br>Note: If the suffix ends with .zip or .gz, the rotation file will also be compressed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_FILE_SUFFIX+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-file-file-handlers-rotation-rotate-on-boot"></a> [`quarkus.log.handler.file."file-handlers".rotation.rotate-on-boot`](#quarkus-core_quarkus-log-handler-file-file-handlers-rotation-rotate-on-boot) config_property_copy_button:+++quarkus.log.handler.file."file-handlers".rotation.rotate-on-boot+++[]<br><br>Indicates whether to rotate log files on server initialization.<br><br>You need to either set a `max-file-size` or configure a `file-suffix` for it to work.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_FILE__FILE_HANDLERS__ROTATION_ROTATE_ON_BOOT+++[] | boolean | `true` |
| <a id="quarkus-core_section_quarkus-log-handler-syslog"></a> [Syslog handlers](#quarkus-core_section_quarkus-log-handler-syslog) | Type | Default |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-enabled"></a> [`quarkus.log.handler.syslog."syslog-handlers".enabled`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".enabled+++[]<br><br>If syslog logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-endpoint"></a> [`quarkus.log.handler.syslog."syslog-handlers".endpoint`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-endpoint) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".endpoint+++[]<br><br>The IP address and port of the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ENDPOINT+++[] | host:port | `localhost:514` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-app-name"></a> [`quarkus.log.handler.syslog."syslog-handlers".app-name`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-app-name) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".app-name+++[]<br><br>The app name used when formatting the message in RFC5424 format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__APP_NAME+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-hostname"></a> [`quarkus.log.handler.syslog."syslog-handlers".hostname`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-hostname) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".hostname+++[]<br><br>The name of the host the messages are being sent from<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__HOSTNAME+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-facility"></a> [`quarkus.log.handler.syslog."syslog-handlers".facility`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-facility) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".facility+++[]<br><br>Sets the facility used when calculating the priority of the message as defined by RFC-5424 and RFC-3164<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__FACILITY+++[] | `kernel`, `user-level`, `mail-system`, `system-daemons`, `security`, `syslogd`, `line-printer`, `network-news`, `uucp`, `clock-daemon`, `security2`, `ftp-daemon`, `ntp`, `log-audit`, `log-alert`, `clock-daemon2`, `local-use-0`, `local-use-1`, `local-use-2`, `local-use-3`, `local-use-4`, `local-use-5`, `local-use-6`, `local-use-7` | `user-level` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-syslog-type"></a> [`quarkus.log.handler.syslog."syslog-handlers".syslog-type`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-syslog-type) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".syslog-type+++[]<br><br>Set the `SyslogType syslog type` this handler should use to format the message sent<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__SYSLOG_TYPE+++[] | `rfc5424`, `rfc3164` | `rfc5424` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-protocol"></a> [`quarkus.log.handler.syslog."syslog-handlers".protocol`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-protocol) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".protocol+++[]<br><br>Sets the protocol used to connect to the Syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-use-counting-framing"></a> [`quarkus.log.handler.syslog."syslog-handlers".use-counting-framing`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-use-counting-framing) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".use-counting-framing+++[]<br><br>If enabled, the message being sent is prefixed with the size of the message<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__USE_COUNTING_FRAMING+++[] | `true`, `false`, `protocol-dependent` | `protocol-dependent` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-truncate"></a> [`quarkus.log.handler.syslog."syslog-handlers".truncate`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-truncate) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".truncate+++[]<br><br>Set to `true` to truncate the message if it exceeds maximum length<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__TRUNCATE+++[] | boolean | `true` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-block-on-reconnect"></a> [`quarkus.log.handler.syslog."syslog-handlers".block-on-reconnect`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-block-on-reconnect) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-format"></a> [`quarkus.log.handler.syslog."syslog-handlers".format`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-format) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-level"></a> [`quarkus.log.handler.syslog."syslog-handlers".level`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-level) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".level+++[]<br><br>The log level specifying what message levels will be logged by the Syslog logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-filter"></a> [`quarkus.log.handler.syslog."syslog-handlers".filter`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-filter) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-max-length"></a> [`quarkus.log.handler.syslog."syslog-handlers".max-length`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-max-length) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".max-length+++[]<br><br>The maximum length, in bytes, of the message allowed to be sent, up to `Integer.MAX_VALUE` bytes. The length includes the header and the message.<br><br>If not set, the default value is `2048` when `sys-log-type` is `rfc5424` (which is the default) and `1024` when `sys-log-type` is `rfc3164`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__MAX_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-core_quarkus-log) |  |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-async-enabled"></a> [`quarkus.log.handler.syslog."syslog-handlers".async.enabled`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-async-queue-length"></a> [`quarkus.log.handler.syslog."syslog-handlers".async.queue-length`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-handler-syslog-syslog-handlers-async-overflow"></a> [`quarkus.log.handler.syslog."syslog-handlers".async.overflow`](#quarkus-core_quarkus-log-handler-syslog-syslog-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.syslog."syslog-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SYSLOG__SYSLOG_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_section_quarkus-log-handler-socket"></a> [Socket handlers](#quarkus-core_section_quarkus-log-handler-socket) | Type | Default |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-enabled"></a> [`quarkus.log.handler.socket."socket-handlers".enabled`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-enabled) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".enabled+++[]<br><br>If socket logging should be enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-endpoint"></a> [`quarkus.log.handler.socket."socket-handlers".endpoint`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-endpoint) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".endpoint+++[]<br><br>The IP address and port of the server receiving the logs<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ENDPOINT+++[] | host:port | `localhost:4560` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-protocol"></a> [`quarkus.log.handler.socket."socket-handlers".protocol`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-protocol) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".protocol+++[]<br><br>Sets the protocol used to connect to the syslog server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__PROTOCOL+++[] | `tcp`, `udp`, `ssl-tcp` | `tcp` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-block-on-reconnect"></a> [`quarkus.log.handler.socket."socket-handlers".block-on-reconnect`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-block-on-reconnect) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".block-on-reconnect+++[]<br><br>Enables or disables blocking when attempting to reconnect a `Protocol#TCP TCP` or `Protocol#SSL_TCP SSL TCP` protocol<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__BLOCK_ON_RECONNECT+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-format"></a> [`quarkus.log.handler.socket."socket-handlers".format`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-format) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".format+++[]<br><br>The log message format<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__FORMAT+++[] | string | `%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-level"></a> [`quarkus.log.handler.socket."socket-handlers".level`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-level) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".level+++[]<br><br>The log level specifying, which message levels will be logged by socket logger<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `ALL` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-filter"></a> [`quarkus.log.handler.socket."socket-handlers".filter`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-filter) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".filter+++[]<br><br>The name of the filter to link to the file handler.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__FILTER+++[] | string |  |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-async-enabled"></a> [`quarkus.log.handler.socket."socket-handlers".async.enabled`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-async-enabled) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".async.enabled+++[]<br><br>Whether to log asynchronously<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ASYNC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-async-queue-length"></a> [`quarkus.log.handler.socket."socket-handlers".async.queue-length`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-async-queue-length) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".async.queue-length+++[]<br><br>The queue length to use before flushing writing<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ASYNC_QUEUE_LENGTH+++[] | int | `512` |
| <a id="quarkus-core_quarkus-log-handler-socket-socket-handlers-async-overflow"></a> [`quarkus.log.handler.socket."socket-handlers".async.overflow`](#quarkus-core_quarkus-log-handler-socket-socket-handlers-async-overflow) config_property_copy_button:+++quarkus.log.handler.socket."socket-handlers".async.overflow+++[]<br><br>Determine whether to block the publisher (rather than drop the message) when the queue is full<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_HANDLER_SOCKET__SOCKET_HANDLERS__ASYNC_OVERFLOW+++[] | `block`, `discard` | `block` |
| <a id="quarkus-core_section_quarkus-log-filter"></a> [Log cleanup filters - internal use](#quarkus-core_section_quarkus-log-filter) | Type | Default |
| <a id="quarkus-core_quarkus-log-filter-filters-if-starts-with"></a> [`quarkus.log.filter."filters".if-starts-with`](#quarkus-core_quarkus-log-filter-filters-if-starts-with) config_property_copy_button:+++quarkus.log.filter."filters".if-starts-with+++[]<br><br>The message prefix to match<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILTER__FILTERS__IF_STARTS_WITH+++[] | list of string | `inherit` |
| <a id="quarkus-core_quarkus-log-filter-filters-target-level"></a> [`quarkus.log.filter."filters".target-level`](#quarkus-core_quarkus-log-filter-filters-target-level) config_property_copy_button:+++quarkus.log.filter."filters".target-level+++[]<br><br>The new log level for the filtered message. Defaults to DEBUG.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOG_FILTER__FILTERS__TARGET_LEVEL+++[] | [Level](https://javadoc.io/doc/org.jboss.logmanager/jboss-logmanager/latest/org/jboss/logmanager/Level.html) | `DEBUG` |

<a id="memory-size-note-anchor-quarkus-core_quarkus-log"></a>

> [!NOTE]
> **About the MemorySize format**
>
> A size configuration option recognizes strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.
>
> If no suffix is given, assume bytes.

[^calc]: Format sequences which examine caller information might affect performance
