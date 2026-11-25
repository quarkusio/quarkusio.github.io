# Vert.x reference guide

[Vert.x](https://vertx.io) is a toolkit for building reactive applications. As described in the [Quarkus Reactive Architecture](/guides/quarkus-reactive-architecture/index.md), Quarkus uses Vert.x underneath.

This guide is the companion to the [Using Eclipse Vert.x API from a Quarkus Application](/guides/vertx/index.md) guide. It provides more advanced details about the usage and the configuration of the Vert.x instance used by Quarkus.

<a id="vertx-access"></a>
## Access the Vert.x instance

To access the managed Vert.x instance, add the `quarkus-vertx` extension to your project. This dependency might already be available in your project (as a transitive dependency).

With this extension, you can retrieve the managed instance of Vert.x using either field or constructor injection:

```java
@ApplicationScoped
public class MyBean {

    // Field injection
    @Inject Vertx vertx;

    // Constructor injection
    MyBean(Vertx vertx) {
        // ...
    }

}
```

You can inject either the:

- `io.vertx.core.Vertx` instance exposing the *bare* Vert.x API
- `io.vertx.mutiny.core.Vertx` instance exposing the *Mutiny* API

We recommend using the Mutiny variant as it integrates with the other reactive APIs provided by Quarkus.

> [!TIP]
> **Mutiny**
>
> If you are not familiar with Mutiny, check [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

Documentation about the Vert.x Mutiny variant is available on <https://smallrye.io/smallrye-mutiny-vertx-bindings>.

<a id="vertx-config"></a>
## Configure the Vert.x instance

You can configure the Vert.x instance from the `application.properties` file. The following table lists the supported properties:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-vertx_quarkus-vertx-use-async-dns"></a> [`quarkus.vertx.use-async-dns`](#quarkus-vertx_quarkus-vertx-use-async-dns) config_property_copy_button:+++quarkus.vertx.use-async-dns+++[]<br><br>Enables the async DNS resolver. This must be configured at build time. When disabled, native executables exclude the async DNS resolver to reduce their size.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_USE_ASYNC_DNS+++[] | boolean | `false` |
| [lock] <a id="quarkus-vertx_quarkus-vertx-native-transport"></a> [`quarkus.vertx.native-transport`](#quarkus-vertx_quarkus-vertx-native-transport) config_property_copy_button:+++quarkus.vertx.native-transport+++[]<br><br>Whether and how strictly native transport should be used.<br><br>Supported values:<br><br>\* `disabled` - Do not use native transport (default). Vert.x always uses Java NIO. \* `if-available` - Use native transport if available, fall back to Java NIO otherwise. \* `required` - Require native transport. If the transport dependency is missing, the build fails. If the native library cannot load at runtime, the application fails to start.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_NATIVE_TRANSPORT+++[] | tooltip:disabled[Do not use native transport. Quarkus will always use Java NIO.], tooltip:if-available[Use native transport if available,opts=fall back to Java NIO otherwise.], tooltip:required[Require native transport. If the transport dependency is missing from the classpath,opts=the build fails. If the native library cannot be loaded at runtime,the application fails to start.] | tooltip:disabled[Do not use native transport. Quarkus will always use Java NIO.] |
| [lock] <a id="quarkus-vertx_quarkus-vertx-native-transport-type"></a> [`quarkus.vertx.native-transport-type`](#quarkus-vertx_quarkus-vertx-native-transport-type) config_property_copy_button:+++quarkus.vertx.native-transport-type+++[]<br><br>The type of native transport to use.<br><br>When set to a value other than `auto`, the application will attempt to use the specified native transport. If the transport is not available at runtime, the behavior depends on `native-transport()`.<br><br>Setting this to a specific transport type implicitly enables native transport (i.e., `native-transport()` is treated as at least `if-available`).<br><br>Supported values:<br><br>\* `auto` - Let Vert.x pick the best available transport (default) \* `epoll` - Use Linux epoll transport \* `kqueue` - Use macOS kqueue transport \* `io-uring` - Use Linux io_uring transport<br><br>Note that, if not set to `auto`, Quarkus checks at build time that the requested transport type is available in the classpath. At runtime, it checks whether the transport can be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_NATIVE_TRANSPORT_TYPE+++[] | tooltip:auto[Pick the most appropriate one. This is the default.], tooltip:epoll[EPOLL (Linux Only)  link\:https\://man7.org/linux/man-pages/man7/epoll.7.html\\]See epoll man page\].], tooltip:kqueue[KQUEUE (OSX only)  link\:https\://en.wikipedia.org/wiki/Kqueue\\]See KQueue on Wikipedia\]], tooltip:io-uring[IO_URING (Linux only,opts=need a recent Linux kernel (5.10{plus})).  link\:https\://man7.org/linux/man-pages/man7/io_uring.7.html\\]See io_uring man page\].] | tooltip:auto[Pick the most appropriate one. This is the default.] |
| <a id="quarkus-vertx_quarkus-vertx-caching"></a> [`quarkus.vertx.caching`](#quarkus-vertx_quarkus-vertx-caching) config_property_copy_button:+++quarkus.vertx.caching+++[]<br><br>Enables or disables the Vert.x cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_CACHING+++[] | boolean | `true` |
| <a id="quarkus-vertx_quarkus-vertx-cache-directory"></a> [`quarkus.vertx.cache-directory`](#quarkus-vertx_quarkus-vertx-cache-directory) config_property_copy_button:+++quarkus.vertx.cache-directory+++[]<br><br>Configure the file cache directory. When not set, the cache is stored in the system temporary directory (read from the `java.io.tmpdir` system property). If the `java.io.tmpdir` is not set `.` is used.<br><br>Note that this property is ignored if the `vertx.cacheDirBase` system property is set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_CACHE_DIRECTORY+++[] | string |  |
| <a id="quarkus-vertx_quarkus-vertx-classpath-resolving"></a> [`quarkus.vertx.classpath-resolving`](#quarkus-vertx_quarkus-vertx-classpath-resolving) config_property_copy_button:+++quarkus.vertx.classpath-resolving+++[]<br><br>Enables or disabled the Vert.x classpath resource resolver.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_CLASSPATH_RESOLVING+++[] | boolean | `true` |
| <a id="quarkus-vertx_quarkus-vertx-event-loops-pool-size"></a> [`quarkus.vertx.event-loops-pool-size`](#quarkus-vertx_quarkus-vertx-event-loops-pool-size) config_property_copy_button:+++quarkus.vertx.event-loops-pool-size+++[]<br><br>The number of event loops. By default, it matches the number of CPUs detected on the system.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_EVENT_LOOPS_POOL_SIZE+++[] | int |  |
| <a id="quarkus-vertx_quarkus-vertx-max-event-loop-execute-time"></a> [`quarkus.vertx.max-event-loop-execute-time`](#quarkus-vertx_quarkus-vertx-max-event-loop-execute-time) config_property_copy_button:+++quarkus.vertx.max-event-loop-execute-time+++[]<br><br>The maximum amount of time the event loop can be blocked.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_MAX_EVENT_LOOP_EXECUTE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `2S` |
| <a id="quarkus-vertx_quarkus-vertx-warning-exception-time"></a> [`quarkus.vertx.warning-exception-time`](#quarkus-vertx_quarkus-vertx-warning-exception-time) config_property_copy_button:+++quarkus.vertx.warning-exception-time+++[]<br><br>The amount of time before a warning is displayed if the event loop is blocked.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_WARNING_EXCEPTION_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `2S` |
| <a id="quarkus-vertx_quarkus-vertx-max-worker-execute-time"></a> [`quarkus.vertx.max-worker-execute-time`](#quarkus-vertx_quarkus-vertx-max-worker-execute-time) config_property_copy_button:+++quarkus.vertx.max-worker-execute-time+++[]<br><br>The maximum amount of time the worker thread can be blocked.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_MAX_WORKER_EXECUTE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `60S` |
| <a id="quarkus-vertx_quarkus-vertx-internal-blocking-pool-size"></a> [`quarkus.vertx.internal-blocking-pool-size`](#quarkus-vertx_quarkus-vertx-internal-blocking-pool-size) config_property_copy_button:+++quarkus.vertx.internal-blocking-pool-size+++[]<br><br>The size of the internal thread pool (used for the file system).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_INTERNAL_BLOCKING_POOL_SIZE+++[] | int | `20` |
| <a id="quarkus-vertx_quarkus-vertx-queue-size"></a> [`quarkus.vertx.queue-size`](#quarkus-vertx_quarkus-vertx-queue-size) config_property_copy_button:+++quarkus.vertx.queue-size+++[]<br><br>The queue size. For most applications this should be unbounded<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_QUEUE_SIZE+++[] | int |  |
| <a id="quarkus-vertx_quarkus-vertx-growth-resistance"></a> [`quarkus.vertx.growth-resistance`](#quarkus-vertx_quarkus-vertx-growth-resistance) config_property_copy_button:+++quarkus.vertx.growth-resistance+++[]<br><br>The executor growth resistance.<br><br>A resistance factor applied after the core pool is full; values applied here will cause that fraction of submissions to create new threads when no idle thread is available. A value of `0.0f` implies that threads beyond the core size should be created as aggressively as threads within it; a value of `1.0f` implies that threads beyond the core size should never be created.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_GROWTH_RESISTANCE+++[] | float | `0` |
| <a id="quarkus-vertx_quarkus-vertx-keep-alive-time"></a> [`quarkus.vertx.keep-alive-time`](#quarkus-vertx_quarkus-vertx-keep-alive-time) config_property_copy_button:+++quarkus.vertx.keep-alive-time+++[]<br><br>The amount of time a thread will stay alive with no work.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_KEEP_ALIVE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `30S` |
| <a id="quarkus-vertx_quarkus-vertx-prefill"></a> [`quarkus.vertx.prefill`](#quarkus-vertx_quarkus-vertx-prefill) config_property_copy_button:+++quarkus.vertx.prefill+++[]<br><br>Prefill thread pool when creating a new Executor. When `io.vertx.core.spi.ExecutorServiceFactory#createExecutor` is called, initialise with the number of defined threads at startup<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_PREFILL+++[] | boolean | `false` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-cache-max-time-to-live"></a> [`quarkus.vertx.resolver.cache-max-time-to-live`](#quarkus-vertx_quarkus-vertx-resolver-cache-max-time-to-live) config_property_copy_button:+++quarkus.vertx.resolver.cache-max-time-to-live+++[]<br><br>The maximum amount of time that a successfully resolved address will be cached.<br><br>If not set explicitly, resolved addresses may be cached forever.<br><br>The value must be a duration of at least 1 second.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_CACHE_MAX_TIME_TO_LIVE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `2147483647S` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-cache-min-time-to-live"></a> [`quarkus.vertx.resolver.cache-min-time-to-live`](#quarkus-vertx_quarkus-vertx-resolver-cache-min-time-to-live) config_property_copy_button:+++quarkus.vertx.resolver.cache-min-time-to-live+++[]<br><br>The minimum amount of time that a successfully resolved address will be cached.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_CACHE_MIN_TIME_TO_LIVE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `0S` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-cache-negative-time-to-live"></a> [`quarkus.vertx.resolver.cache-negative-time-to-live`](#quarkus-vertx_quarkus-vertx-resolver-cache-negative-time-to-live) config_property_copy_button:+++quarkus.vertx.resolver.cache-negative-time-to-live+++[]<br><br>The amount of time that an unsuccessful attempt to resolve an address will be cached.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_CACHE_NEGATIVE_TIME_TO_LIVE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `0S` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-max-queries"></a> [`quarkus.vertx.resolver.max-queries`](#quarkus-vertx_quarkus-vertx-resolver-max-queries) config_property_copy_button:+++quarkus.vertx.resolver.max-queries+++[]<br><br>The maximum number of queries to be sent during a resolution.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_MAX_QUERIES+++[] | int | `4` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-query-timeout"></a> [`quarkus.vertx.resolver.query-timeout`](#quarkus-vertx_quarkus-vertx-resolver-query-timeout) config_property_copy_button:+++quarkus.vertx.resolver.query-timeout+++[]<br><br>The duration after which a DNS query is considered to be failed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_QUERY_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `5S` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-hosts-path"></a> [`quarkus.vertx.resolver.hosts-path`](#quarkus-vertx_quarkus-vertx-resolver-hosts-path) config_property_copy_button:+++quarkus.vertx.resolver.hosts-path+++[]<br><br>Set the path of an alternate hosts configuration file to use instead of the one provided by the os.<br><br>The default value is `null`, so the operating system hosts config (e.g. `/etc/hosts`) is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_HOSTS_PATH+++[] | string |  |
| <a id="quarkus-vertx_quarkus-vertx-resolver-host-refresh-period"></a> [`quarkus.vertx.resolver.host-refresh-period`](#quarkus-vertx_quarkus-vertx-resolver-host-refresh-period) config_property_copy_button:+++quarkus.vertx.resolver.host-refresh-period+++[]<br><br>The hosts configuration refresh period.<br><br>The resolver caches the hosts configuration (configured using `quarkus.vertx.resolver.hosts-path` after it has read it. When the content of this file can change, setting a positive refresh period will load the configuration file again when necessary.<br><br>A value of `0` (the default) disables the refresh.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_HOST_REFRESH_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `0S` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-servers"></a> [`quarkus.vertx.resolver.servers`](#quarkus-vertx_quarkus-vertx-resolver-servers) config_property_copy_button:+++quarkus.vertx.resolver.servers+++[]<br><br>Set the list of DNS server addresses, an address is the IP of the dns server, followed by an optional colon and a port, e.g `8.8.8.8` or `192.168.0.1:40000`. When the list is empty, the resolver will use the list of the system DNS server addresses from the environment, if that list cannot be retrieved it will use Google's public DNS servers `"8.8.8.8"` and `"8.8.4.4"`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_SERVERS+++[] | list of string |  |
| <a id="quarkus-vertx_quarkus-vertx-resolver-opt-resource-enabled"></a> [`quarkus.vertx.resolver.opt-resource-enabled`](#quarkus-vertx_quarkus-vertx-resolver-opt-resource-enabled) config_property_copy_button:+++quarkus.vertx.resolver.opt-resource-enabled+++[]<br><br>Set to true to enable the automatic inclusion in DNS queries of an optional record that hints the remote DNS server about how much data the resolver can read per response.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_OPT_RESOURCE_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-rd-flag"></a> [`quarkus.vertx.resolver.rd-flag`](#quarkus-vertx_quarkus-vertx-resolver-rd-flag) config_property_copy_button:+++quarkus.vertx.resolver.rd-flag+++[]<br><br>Set the DNS queries *Recursion Desired* flag value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_RD_FLAG+++[] | boolean | `true` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-search-domains"></a> [`quarkus.vertx.resolver.search-domains`](#quarkus-vertx_quarkus-vertx-resolver-search-domains) config_property_copy_button:+++quarkus.vertx.resolver.search-domains+++[]<br><br>Set the lists of DNS search domains.<br><br>When the search domain list is null, the effective search domain list will be populated using the system DNS search domains.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_SEARCH_DOMAINS+++[] | list of string |  |
| <a id="quarkus-vertx_quarkus-vertx-resolver-ndots"></a> [`quarkus.vertx.resolver.ndots`](#quarkus-vertx_quarkus-vertx-resolver-ndots) config_property_copy_button:+++quarkus.vertx.resolver.ndots+++[]<br><br>Set the ndots value used when resolving using search domains, the default value is `-1` which determines the value from the OS on Linux or uses the value `1`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_NDOTS+++[] | int | `-1` |
| <a id="quarkus-vertx_quarkus-vertx-resolver-rotate-servers"></a> [`quarkus.vertx.resolver.rotate-servers`](#quarkus-vertx_quarkus-vertx-resolver-rotate-servers) config_property_copy_button:+++quarkus.vertx.resolver.rotate-servers+++[]<br><br>Set to `true` to enable round-robin selection of the dns server to use. It spreads the query load among the servers and avoids all lookup to hit the first server of the list.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_ROTATE_SERVERS+++[] | boolean |  |
| <a id="quarkus-vertx_quarkus-vertx-resolver-round-robin-inet-address"></a> [`quarkus.vertx.resolver.round-robin-inet-address`](#quarkus-vertx_quarkus-vertx-resolver-round-robin-inet-address) config_property_copy_button:+++quarkus.vertx.resolver.round-robin-inet-address+++[]<br><br>Set to `true` to enable round-robin inet address selection of the ip address to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_RESOLVER_ROUND_ROBIN_INET_ADDRESS+++[] | boolean | `false` |
| <a id="quarkus-vertx_quarkus-vertx-blocked-thread-check-interval"></a> [`quarkus.vertx.blocked-thread-check-interval`](#quarkus-vertx_quarkus-vertx-blocked-thread-check-interval) config_property_copy_button:+++quarkus.vertx.blocked-thread-check-interval+++[]<br><br>The interval of the blocked thread checker. The blocked thread checker periodically checks if any event loop or worker thread has been blocked for longer than the configured thresholds (`max-event-loop-execute-time` and `max-worker-execute-time`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_VERTX_BLOCKED_THREAD_CHECK_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx_quarkus-vertx) | `1S` |

<a id="duration-note-anchor-quarkus-vertx_quarkus-vertx"></a>

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

See [Customize the Vert.x configuration](#customizing-the-vert-x-configuration) to configure the Vert.x instance using a programmatic approach.

<a id="using-vertx-clients"></a>
## Use Vert.x clients

In addition to Vert.x core, you can use most Vert.x ecosystem libraries. Some Quarkus extension already wraps Vert.x libraries.

### Available APIs

The following table lists the **most** used libraries from the Vert.x ecosystem. To access these APIs, add the indicated extension or dependency to your project. Check the associated documentation to learn how to use them.

| API | Extension or Dependency | Documentation |
| --- | --- | --- |
| AMQP Client | `io.quarkus:quarkus-messaging-amqp` (extension) | [Getting Started to Quarkus Messaging with AMQP](/guides/amqp/index.md) |
| Circuit Breaker | `io.smallrye.reactive:smallrye-mutiny-vertx-circuit-breaker` (external dependency) | <https://vertx.io/docs/vertx-circuit-breaker/java/> |
| Consul Client | `io.smallrye.reactive:smallrye-mutiny-vertx-consul-client` (external dependency) | <https://vertx.io/docs/vertx-consul-client/java/> |
| DB2 Client | `io.quarkus:quarkus-reactive-db2-client` (extension) | [Reactive SQL Clients](/guides/reactive-sql-clients/index.md) |
| Kafka Client | `io.quarkus:quarkus-messaging-kafka` (extension) | [Apache Kafka Reference Guide](/guides/kafka/index.md) |
| Mail Client | `io.quarkus:quarkus-mailer` (extension) | [Sending emails using SMTP](/guides/mailer/index.md) |
| MQTT Client | `io.quarkus:quarkus-messaging-mqtt` (extension) | No guide yet |
| MS SQL Client | `io.quarkus:quarkus-reactive-mssql-client` (extension) | [Reactive SQL Clients](/guides/reactive-sql-clients/index.md) |
| MySQL Client | `io.quarkus:quarkus-reactive-mysql-client` (extension) | [Reactive SQL Clients](/guides/reactive-sql-clients/index.md) |
| Oracle Client | `io.quarkus:quarkus-reactive-oracle-client` (extension) | [Reactive SQL Clients](/guides/reactive-sql-clients/index.md) |
| PostgreSQL Client | `io.quarkus:quarkus-reactive-pg-client` (extension) | [Reactive SQL Clients](/guides/reactive-sql-clients/index.md) |
| RabbitMQ Client | `io.smallrye.reactive:smallrye-mutiny-vertx-rabbitmq-client` (external dependency) | <https://vertx.io/docs/vertx-rabbitmq-client/java> |
| Redis Client | `io.quarkus:quarkus-redis-client` (extension) | [Using the Redis Client](/guides/redis/index.md) |
| Web Client | `io.smallrye.reactive:smallrye-mutiny-vertx-web-client` (external dependency) | <https://vertx.io/docs/vertx-web-client/java/> |

To learn more about the usage of the Vert.x Mutiny API, refer to <https://smallrye.io/smallrye-mutiny-vertx-bindings>.

### Use the Vert.x Web Client

This section gives an example using the Vert.x `WebClient` in the context of a Quarkus REST (formerly RESTEasy Reactive) application. As indicated in the table above, add the following dependency to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.smallrye.reactive</groupId>
    <artifactId>smallrye-mutiny-vertx-web-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.smallrye.reactive:smallrye-mutiny-vertx-web-client")
```

Now, in your code, you can create an instance of `WebClient`:

```java
package org.acme.vertx;


import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.smallrye.mutiny.Uni;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;

@Path("/fruit-data")
public class ResourceUsingWebClient {

    private final WebClient client;

    @Inject
    VertxResource(Vertx vertx) {
        this.client = WebClient.create(vertx);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{name}")
    public Uni<JsonObject> getFruitData(String name) {
        return client.getAbs("https://.../api/fruit/" + name)
                .send()
                .onItem().transform(resp -> {
                    if (resp.statusCode() == 200) {
                        return resp.bodyAsJsonObject();
                    } else {
                        return new JsonObject()
                                .put("code", resp.statusCode())
                                .put("message", resp.bodyAsString());
                    }
                });
    }

}

```

This resource creates a `WebClient` and, upon request, uses this client to invoke a remote HTTP API. Depending on the result, the response is forwarded as received, or it creates a JSON object wrapping the error. The `WebClient` is asynchronous (and non-blocking), to the endpoint returns a `Uni`.

The application can also run as a native executable. But, first, we need to instruct Quarkus to enable *ssl* (if the remote API uses HTTPS). Open the `src/main/resources/application.properties` and add:

```properties
quarkus.ssl.native=true
```

Then, create the native executable with:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

<a id="using-vert-x-json"></a>
## Use Vert.x JSON

Vert.x APIs often rely on JSON. Vert.x provides two convenient classes to manipulate JSON document: `io.vertx.core.json.JsonObject` and `io.vertx.core.json.JsonArray`.

`JsonObject` can be used to map an object into its JSON representation and build an object from a JSON document:

```java
// Map an object into JSON
Person person = ...;
JsonObject json = JsonObject.mapFrom(person);

// Build an object from JSON
json = new JsonObject();
person = json.mapTo(Person.class);
```

Note that these features use the mapper managed by the `quarkus-jackson` extension. Refer to [Jackson configuration](/guides/rest-json/index.md#json) to customize the mapping.

JSON Object and JSON Array are both supported as Quarkus HTTP endpoint requests and response bodies (using classic RESTEasy and Quarkus REST). Consider these endpoints:

```java
package org.acme.vertx;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
public class VertxJsonResource {

    @GET
    @Path("{name}/object")
    public JsonObject jsonObject(String name) {
        return new JsonObject().put("Hello", name);
    }

    @GET
    @Path("{name}/array")
    public JsonArray jsonArray(String name) {
        return new JsonArray().add("Hello").add(name);
    }
}
```

<http://localhost:8080/hello/Quarkus/object> returns:

```text
{"Hello":"Quarkus"}
```

<http://localhost:8080/hello/Quarkus/array> returns:

```text
["Hello","Quarkus"]
```

This works equally well when the JSON content is a request body or is wrapped in a `Uni`, `Multi`, `CompletionStage` or `Publisher`.

## Use Verticles

[Verticles](https://vertx.io/docs/vertx-core/java/#_verticles) is "a simple, scalable, actor-like deployment and concurrency model" provided by *Vert.x*. This model does not claim to be a strict actor-model implementation, but it shares similarities, especially concerning concurrency, scaling, and deployment. To use this model, you write and *deploy* verticles, communicating by sending messages on the event bus.

You can deploy *verticles* in Quarkus. It supports:

- *bare* verticle - Java classes extending `io.vertx.core.AbstractVerticle`
- *Mutiny* verticle - Java classes extending `io.smallrye.mutiny.vertx.core.AbstractVerticle`

### Deploy Verticles

To deploy verticles, use the `deployVerticle` method:

```java
@Inject Vertx vertx;

// ...
vertx.deployVerticle(MyVerticle.class.getName(), ar -> { });
vertx.deployVerticle(new MyVerticle(), ar -> { });
```

If you use the Mutiny-variant of Vert.x, be aware that the `deployVerticle` method returns a `Uni`, and you would need to trigger a subscription to make the actual deployment.

> [!NOTE]
> An example explaining how to deploy verticles during the initialization of the application will follow.

### Use @ApplicationScoped beans as Verticle

In general, Vert.x verticles are not CDI beans. And so cannot use injection. However, in Quarkus, you can deploy verticles as beans. Note that in this case, CDI (Arc in Quarkus) is responsible for creating the instance.

The following snippet provides an example:

```java
package io.quarkus.vertx.verticles;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyBeanVerticle extends AbstractVerticle {

    @ConfigProperty(name = "address") String address;

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer(address)
                .handler(m -> m.replyAndForget("hello"))
                .completionHandler();
    }
}
```

You don't have to inject the `vertx` instance; instead, leverage the protected field from `AbstractVerticle`.

Then, deploy the verticle instances with:

```java
package io.quarkus.vertx.verticles;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class VerticleDeployer {

    public void init(@Observes StartupEvent e, Vertx vertx, MyBeanVerticle verticle) {
         vertx.deployVerticle(verticle).await().indefinitely();
    }
}
```

If you want to deploy every exposed `AbstractVerticle`, you can use:

```java
public void init(@Observes StartupEvent e, Vertx vertx, Instance<AbstractVerticle> verticles) {
    for (AbstractVerticle verticle : verticles) {
        vertx.deployVerticle(verticle).await().indefinitely();
    }
}
```

### Create multiple verticles instances

When using `@ApplicationScoped`, you will get a single instance for your verticle. Having multiple instances of verticles can be helpful to share the load among them. Each of them will be associated with a different I/O thread (Vert.x event loop).

To deploy multiple instances of your verticle, use the `@Dependent` scope instead of `@ApplicationScoped`:

```java
package org.acme.verticle;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class MyVerticle extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        return vertx.eventBus().consumer("address")
                .handler(m -> m.reply("Hello from " + this))
                .completionHandler();
    }
}
```

Then, deploy your verticle as follows:

```java
package org.acme.verticle;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.DeploymentOptions;
import io.vertx.mutiny.core.Vertx;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyApp {

    void init(@Observes StartupEvent ev, Vertx vertx, Instance<MyVerticle> verticles) {
        vertx
                .deployVerticle(verticles::get, new DeploymentOptions().setInstances(2))
                .await().indefinitely();
    }
}

```

The `init` method receives an `Instance<MyVerticle>`. Then, you pass a supplier to the `deployVerticle` method. The supplier is just calling the `get()` method. Thanks to the `@Dependent` scope, it returns a new instance on every call. Finally, you pass the desired number of instances to the `DeploymentOptions`, such as two in the previous example. It will call the supplier twice, which will create two instances of your verticle.

<a id="eventbus"></a>
## Use the Event Bus

Vert.x comes with a built-in [event bus](https://vertx.io/docs/vertx-core/java/#event_bus) that you can use from your Quarkus application. So, your application components (CDI beans, resources...) can interact using asynchronous events, thus promoting loose-coupling.

With the event bus, you send *messages* to *virtual addresses*. The event bus offers three types of delivery mechanisms:

- point-to-point - send the message, one consumer receives it. If several consumers listen to the address, a round-robin is applied;
- publish/subscribe - publish a message; all the consumers listening to the address are receiving the message;
- request/reply - send the message and expect a response. The receiver can respond to the message in an asynchronous fashion.

All these delivery mechanisms are non-blocking and are providing one of the fundamental bricks to build reactive applications.

### Consume events

While you can use the Vert.x API to register consumers, Quarkus comes with declarative support. To consume events, use the `io.quarkus.vertx.ConsumeEvent` annotation:

```java
package org.acme.vertx;

import io.quarkus.vertx.ConsumeEvent;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    @ConsumeEvent                           // <1>
    public String consume(String name) {    // <2>
        return name.toUpperCase();
    }
}
```

1. If not set, the address is the fully qualified name of the bean; for instance, in this snippet, it's `org.acme.vertx.GreetingService`.
2. The method parameter is the message body. If the method returns *something*, it's the message response.

### Configure the address

The `@ConsumeEvent` annotation can be configured to set the address:

```java
@ConsumeEvent("greeting")               // <1>
public String consume(String name) {
    return name.toUpperCase();
}
```

1. Receive the messages sent to the `greeting` address

The address value can be a property expression. In this case, the configured value is used instead: `@ConsumeEvent("${my.consumer.address}")`. Additionally, the property expression can specify a default value: `@ConsumeEvent("${my.consumer.address:defaultAddress}")`.

**Config Property Example**

```java
@ConsumeEvent("${my.consumer.address}")   // <1>
public String consume(String name) {
    return name.toLowerCase();
}
```

1. Receive the messages sent to the address configured with the `my.consumer.address` key.

> [!NOTE]
> If no config property with the specified key exists and no default value is set then the application startup fails.

### Process events asynchronously

The previous examples use synchronous processing. Asynchronous processing is also possible by returning either an `io.smallrye.mutiny.Uni` or a `java.util.concurrent.CompletionStage`:

```java
package org.acme.vertx;

import io.quarkus.vertx.ConsumeEvent;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class GreetingService {

    @ConsumeEvent
    public Uni<String> process(String name) {
        // return an Uni completed when the processing is finished.
        // You can also fail the Uni explicitly
    }
}
```

> [!TIP]
> **Mutiny**
>
> The previous example uses Mutiny reactive types. If you are not familiar with Mutiny, check [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

### Blocking processing of events

By default, the code consuming the event must be *non-blocking*, as it's called on an I/O thread. If your processing is blocking, use the `@io.smallrye.common.annotation.Blocking` annotation:

```java
@ConsumeEvent(value = "blocking-consumer")
@Blocking
void consumeBlocking(String message) {
    // Something blocking
}
```

Alternatively, you can use the `blocking` attribute from the `@ConsumeEvent` annotation:

```java
@ConsumeEvent(value = "blocking-consumer", blocking = true)
void consumeBlocking(String message) {
    // Something blocking
}
```

When using `@Blocking`, it ignores the value of the `blocking` attribute of `@ConsumeEvent`.

### Reply to events

The *return* value of a method annotated with `@ConsumeEvent` is used to respond to the incoming message. For instance, in the following snippet, the returned `String` is the response.

```java
@ConsumeEvent("greeting")
public String consume(String name) {
    return name.toUpperCase();
}
```

You can also return a `Uni<T>` or a `CompletionStage<T>` to handle asynchronous reply:

```java
@ConsumeEvent("greeting")
public Uni<String> consume2(String name) {
    return Uni.createFrom().item(() -> name.toUpperCase()).emitOn(executor);
}
```

> [!NOTE]
> You can inject an `executor` if you use the Context Propagation extension:
>
> ```code
> @Inject Executor executor;
> ```

### Implement fire-and-forget interactions

You don't have to reply to received messages. Typically, for a *fire and forget* interaction, the messages are consumed, and the sender does not need to know about it. To implement this pattern, your consumer method returns `void`.

```java
@ConsumeEvent("greeting")
public void consume(String event) {
    // Do something with the event
}
```

### Consume messages (instead of events)

Unlike the previous example using the *payloads* directly, you can also use `Message` directly:

```java
@ConsumeEvent("greeting")
public void consume(Message<String> msg) {
    System.out.println(msg.address());
    System.out.println(msg.body());
}
```

### Handle failures

If a method annotated with `@ConsumeEvent` throws an exception, then:

- if a reply handler is set, then the failure is propagated back to the sender via an `io.vertx.core.eventbus.ReplyException` with code `ConsumeEvent#FAILURE_CODE` and the exception message,
- if no reply handler is set, then the exception is rethrown (and wrapped in a `RuntimeException` if necessary) and can be handled by the default exception handler, *i.e.* `io.vertx.core.Vertx#exceptionHandler()`.

### Send messages

Sending and publishing messages use the Vert.x event bus:

```java
package org.acme.vertx;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/async")
public class EventResource {

    @Inject
    EventBus bus;                                            // <1>

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{name}")
    public Uni<String> greeting(String name) {
        return bus.<String>request("greeting", name)        // <2>
                .onItem().transform(Message::body);
    }
}
```

1. Inject the Event bus
2. Send a message to the address `greeting`. Message payload is `name`

The `EventBus` object provides methods to:

1. `send` a message to a specific address - one single consumer receives the message.
2. `publish` a message to a specific address - all consumers receive the messages.
3. `request` a message and expect a reply

```java
// Case 1
bus.send("greeting", name)
// Case 2
bus.publish("greeting", name)
// Case 3
Uni<String> response = bus.<String>request("address", "hello, how are you?")
        .onItem().transform(Message::body);
```

### Process events on virtual threads

Methods annotated with `@ConsumeEvent` can also be annotated with `@RunOnVirtualThread`. In this case, the method is invoked on a virtual thread. Each event is invoked on a different virtual thread.

To use this feature, make sure:

1. Your Java runtime supports virtual threads.
2. Your method uses a blocking signature.

The second point means only methods returning an object or `void` can use `@RunOnVirtualThread`. Methods returning a `Uni` or a `CompletionStage` **cannot** run on virtual threads.

Read [the virtual thread guide](/guides/virtual-threads/index.md) for more details.

### Use codecs

The [Vert.x Event Bus](++https://vertx.io/docs/vertx-core/java/#event_bus++) uses [codecs](++https://vertx.io/docs/vertx-core/java/#_message_codecs++) to *serialize* and *deserialize* message objects. Quarkus provides a default codec for local delivery. This codec is automatically used for return types and message body parameters of methods annotated with `@ConsumeEvent`.

So that you can exchange the message objects as follows:

```java
@GET
@Produces(MediaType.TEXT_PLAIN)
@Path("{name}")
public Uni<String> greeting(String name) {
    return bus.<String>request("greeting", new MyName(name))
        .onItem().transform(Message::body);
}

@ConsumeEvent(value = "greeting")
Uni<String> greeting(MyName name) {
    return Uni.createFrom().item(() -> "Hello " + name.getName());
}
```

If you want to use a specific codec, you need to set it on both ends explicitly:

```java
@GET
@Produces(MediaType.TEXT_PLAIN)
@Path("{name}")
public Uni<String> greeting(String name) {
    return bus.<String>request("greeting", name,
        new DeliveryOptions().setCodecName(MyNameCodec.class.getName())) // <1>
        .onItem().transform(Message::body);
}

@ConsumeEvent(value = "greeting", codec = MyNameCodec.class)            // <2>
Uni<String> greeting(MyName name) {
    return Uni.createFrom().item(() -> "Hello "+name.getName());
}
```

1. Set the name of the codec to use to send the message
2. Set the codec to use to receive the message

### Combine HTTP and the Event Bus

Let's revisit a greeting HTTP endpoint and use asynchronous message passing to delegate the call to a separated bean. It uses the request/reply dispatching mechanism. Instead of implementing the business logic inside the Jakarta REST endpoint, we are sending a message. Another bean consumes this message, and the response is sent using the *reply* mechanism.

In your HTTP endpoint class, inject the event bus and uses the `request` method to send a message to the event bus and expect a response:

```java
package org.acme.vertx;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/bus")
public class EventResource {

    @Inject
    EventBus bus;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{name}")
    public Uni<String> greeting(String name) {
        return bus.<String>request("greeting", name)            // <1>
                .onItem().transform(Message::body);            // <2>
    }
}
```

1. send the `name` to the `greeting` address and request a response
2. when we get the response, extract the body and send it to the user

> [!NOTE]
> the HTTP method returns a `Uni`. If you are using Quarkus REST, `Uni` support is built-in. If you are using *classic* RESTEasy, you need to add the `quarkus resteasy-mutiny` extension to your project.

We need a consumer listening on the `greeting` address. This consumer can be in the same class or another bean such as:

```java
package org.acme.vertx;

import io.quarkus.vertx.ConsumeEvent;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    @ConsumeEvent("greeting")
    public String greeting(String name) {
        return "Hello " + name;
    }

}
```

This bean receives the name and returns the greeting message.

With this in place, every HTTP request on `/bus/quarkus` sends a message to the event bus, waits for a reply, and when this one arrives, writes the HTTP response:

```text
Hello Quarkus
```

To better understand, let's detail how the HTTP request/response has been handled:

1. The request is received by the `greeting` method
2. a message containing the *name* is sent to the event bus
3. Another bean receives this message and computes the response
4. This response is sent back using the reply mechanism
5. Once the reply is received by the sender, the content is written to the HTTP response

### Bidirectional communication with browsers by using SockJS

The SockJS bridge provided by Vert.x allows browser applications and Quarkus applications to communicate using the event bus. It connects both sides. So, both sides can send messages received on the other side. It supports the three delivery mechanisms.

SockJS negotiates the communication channel between the Quarkus application and the browser. If WebSockets are supported, it uses them; otherwise, it degrades to SSE, long polling, etc.

So use SockJS, you need to configure the bridge, especially the addresses that will be used to communicate:

```java
package org.acme;

import io.vertx.core.Vertx;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class SockJsExample {

    @Inject
    Vertx vertx;

    public void init(@Observes Router router) {
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        Router bridge = sockJSHandler.bridge(new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("ticks")));
        router.route("/eventbus/*").subRouter(bridge);

        AtomicInteger counter = new AtomicInteger();
        vertx.setPeriodic(1000,
                ignored -> vertx.eventBus().publish("ticks", counter.getAndIncrement()));
    }

}
```

This code configures the SockJS bridge to send all the messages targeting the `ticks` address to the connected browsers. More detailed explanations about the configuration can be found on [the Vert.x SockJS Bridge documentation](https://vertx.io/docs/vertx-web/java/#_sockjs_event_bus_bridge).

The browser must use the `vertx-eventbus` JavaScript library to consume the message:

```html
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>SockJS example - Quarkus</title>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"
            integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
    <script type="application/javascript" src="https://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/vertx3-eventbus-client@3.8.5/vertx-eventbus.min.js"></script>
</head>
<body>

<h1>SockJS Examples</h1>

<p><strong>Last Tick:</strong> <span id="tick"></span></p>

</body>
<script>
    var eb = new EventBus('/eventbus');

    eb.onopen = function () {

        eb.registerHandler('ticks', function (error, message) {
            $("#tick").html(message.body);
        });
    }

</script>
</html>
```

<a id="native-transport"></a>
## Native transports

Quarkus supports Netty's native transports (epoll, kqueue, io_uring) for improved I/O performance on specific platforms. See the [Native Transport Reference](/guides/native-transport-reference/index.md) for configuration, dependencies, and platform-specific details.

## Use a Vert.x context-aware scheduler

Some Mutiny operators need to schedule work on an executor thread pool. A good example is `.onItem().delayIt().by(Duration.ofMillis(10))` as it needs such an executor to delay emissions.

The default executor is returned by `io.smallrye.mutiny.infrastructure.Infrastructure` and it is already configured and managed by Quarkus.

That being said, there are cases where you need to make sure that an operation is run on a Vert.x (duplicated) context and not just on any random thread.

The `io.smallrye.mutiny.vertx.core.ContextAwareScheduler` interface offers an API to obtain context-aware schedulers. Such a scheduler is configured with:

1. a delegate `ScheduledExecutorService` of your choice (hint: you can reuse `Infrastructure.getDefaultWorkerPool()`), and
2. a context fetching strategy among: - an explicit `Context`, or - calling `Vertx::getOrCreateContext()` either on the current thread or later when the scheduling request happens, or - calling `Vertx::currentContext()`, which fails if the current thread is not a Vert.x thread.

Here is a sample where `ContextAwareScheduler` is used:

```java
class MyVerticle extends AbstractVerticle {

    @Override
    public Uni<Void> asyncStart() {
        vertx.getOrCreateContext().put("foo", "bar");

        var delegate = Infrastructure.getDefaultWorkerPool();
        var scheduler = ContextAwareScheduler.delegatingTo(delegate)
            .withCurrentContext();

        return Uni.createFrom().voidItem()
                .onItem().delayIt().onExecutor(scheduler).by(Duration.ofMillis(10))
                .onItem().invoke(() -> {
                    // Prints "bar"
                    var ctx = vertx.getOrCreateContext();
                    System.out.println(ctx.get("foo"));
                });
    }
}
```

In this example a scheduler is created by capturing the context of the Vert.x event-loop that calls `asyncStart()`. The `delayIt` operator uses that scheduler, and we can check that the context that we get in `invoke` is a Vert.x duplicated context where the data for key `"foo"` has been propagated.

## Use a Unix Domain Socket

Listening on a Unix Domain Socket allows us to dispense with the overhead of TCP if the connection to the quarkus service is established from the same host. This can happen if access to the service goes through a proxy which is often the case if you're setting up a service mesh with a proxy like Envoy.

Unix Domain Sockets are supported in both JVM and native modes.

> [!NOTE]
> Unix domain sockets require a native transport dependency. See the [Native Transport Reference](/guides/native-transport-reference/index.md) for details.

```
quarkus.http.domain-socket=/var/run/io.quarkus.app.socket
quarkus.http.domain-socket-enabled=true
```

By itself this will not disable the tcp socket which by default will open on `0.0.0.0:8080`. It can be explicitly disabled:

```
quarkus.http.host-enabled=false
```

These properties can be set through Java's `-D` command line parameter or on `application.properties`.

> [!IMPORTANT]
> Make sure your application has the right permissions to write to the socket.

## Deploy on read-only environments

In environments with read only file systems you may receive errors of the form:

```
java.lang.IllegalStateException: Failed to create cache dir
```

Assuming `/tmp/` is writable this can be fixed by setting the `vertx.cacheDirBase` property to point to a directory in `/tmp/` for instance in Kubernetes by creating an environment variable `JAVA_OPTS` with the value `-Dvertx.cacheDirBase=/tmp/vertx`, or setting the `quarkus.vertx.cache-directory` property in `application.properties`:

```properties
quarkus.vertx.cache-directory=/tmp/vertx
```

## DNS resolution

By default, Quarkus uses the blocking DNS resolver. To enable the Vert.x asynchronous DNS resolver, set `quarkus.vertx.use-async-dns=true` **at build time**. This setting is fixed at build time for both JVM applications and native executables; changing it requires rebuilding the application. When asynchronous DNS is disabled, native executables omit the asynchronous resolver code to reduce their size.

<a id="customizing-the-vert-x-configuration"></a>
## Customize the Vert.x configuration

The configuration of the managed Vert.x instance can be provided using the `application.properties` file, but also using *special beans*. CDI beans exposing the `io.quarkus.vertx.VertxOptionsCustomizer` interface can be used to customize the Vert.x configuration. For example, the following customizer change the `tmp` base directory:

```java
@ApplicationScoped
public class MyCustomizer implements VertxOptionsCustomizer {

    @Override
    public void accept(VertxOptions options) {
        options.setFileSystemOptions(new FileSystemOptions().setFileCacheDir("target"));
    }
}
```

The *customizer* beans received the `VertxOptions` (coming from the application configuration), and can modify them.

## Brotli4J and cross-platform support

Brotli4J is a native library that provides support for the Brotli compression algorithm. It is not included by default: see [the HTTP reference guide](/guides/http-reference/index.md#http-compression) to enable it. When added with Maven, the `com.aayushatharva.brotli4j:brotli4j` dependency brings the Brotli native library matching the platform you are running on. But sometimes, you need to include the native library for a different platform, for example when building a native executable in a container.

In this case, you need to explicitly add a dependency to your project. For example, if you need to include the native library for `linux-aarch64`, you can add the following dependency:

```xml
<dependency>
    <groupId>com.aayushatharva.brotli4j</groupId>
    <artifactId>native-linux-aarch64</artifactId>
</dependency>
```

This will include the native library for `linux-aarch64` in your project, in addition to the one matching your machine.

Here is the list of available brotli4j artifacts for the different platforms:

- `native-linux-x86_64`
- `native-linux-s390x`
- `native-linux-ppc64le`
- `native-linux-aarch64`
- `native-linux-armv7`
- `native-linux-riscv64`
- `native-windows-x86_64`
- `native-windows-aarch64`
- `native-osx-x86_64`
- `native-osx-aarch64`
