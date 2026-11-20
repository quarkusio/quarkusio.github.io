# Dev Services for Kafka

If any Kafka-related extension is present (e.g. `quarkus-messaging-kafka`), Dev Services for Kafka automatically starts a Kafka broker in dev mode and when running tests. So, you don't have to start a broker manually. The application is configured automatically.

> [!IMPORTANT]
> By default, Dev Services for Kafka uses the [Apache Kafka native](https://hub.docker.com/r/apache/kafka-native) image which starts in \~1 second.

## Enabling / Disabling Dev Services for Kafka

Dev Services for Kafka is automatically enabled unless:

- `quarkus.kafka.devservices.enabled` is set to `false`
- the `kafka.bootstrap.servers` is configured
- all the Reactive Messaging Kafka channels have the `bootstrap.servers` attribute set

Dev Services for Kafka relies on Docker to start the broker. If your environment does not support Docker, you will need to start the broker manually, or connect to an already running broker. You can configure the broker address using `kafka.bootstrap.servers`.

## Shared broker

Most of the time you need to share the broker between applications. Dev Services for Kafka implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single broker.

> [!NOTE]
> Dev Services for Kafka starts the container with the `quarkus-dev-service-kafka` label which is used to identify the container.

If you need multiple (shared) brokers, you can configure the `quarkus.kafka.devservices.service-name` attribute and indicate the broker name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `kafka`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.kafka.devservices.shared=false`.

## Setting the port

By default, Dev Services for Kafka picks a random port and configures the application. You can set the port by configuring the `quarkus.kafka.devservices.port` property.

Note that the Kafka advertised address is automatically configured with the chosen port.

<a id="configuring-the-image"></a>
## Configuring the image

Dev Services for Kafka supports the following container providers:

**upstream-kafka-native** (default) uses the official [Apache Kafka native](https://hub.docker.com/r/apache/kafka-native) image, compiled with GraalVM for fast startup.

**upstream-kafka** uses the official [Apache Kafka](https://hub.docker.com/r/apache/kafka) JVM image.

```properties
quarkus.kafka.devservices.provider=upstream-kafka
```

**Redpanda** is a Kafka compatible event streaming platform. You can select any version from <https://hub.docker.com/r/redpandadata/redpanda>.

```properties
quarkus.kafka.devservices.provider=redpanda
```

**Strimzi** provides container images and Operators for running Apache Kafka on Kubernetes. While Strimzi is optimized for Kubernetes, the images work perfectly in classic container environments. Strimzi container images run a Kafka broker on JVM, which is slower to start.

```properties
quarkus.kafka.devservices.provider=strimzi
```

For Strimzi, you can select any image with a Kafka version which has Kraft support (2.8.1 and higher) from <https://quay.io/repository/strimzi-test-container/test-container?tab=tags>

```properties
quarkus.kafka.devservices.image-name=quay.io/strimzi-test-container/test-container:0.115.0-kafka-4.2.0
```

## Configuring Kafka topics

You can configure the Dev Services for Kafka to create topics once the broker is started. Topics are created with given number of partitions and 1 replica. The syntax is the following:

```properties
quarkus.kafka.devservices.topic-partitions.<topic-name>=<number-of-partitions>
```

The following example creates a topic named `test` with three partitions, and a second topic named `messages` with two partitions.

```properties
quarkus.kafka.devservices.topic-partitions.test=3
quarkus.kafka.devservices.topic-partitions.messages=2
```

If a topic already exists with the given name, the creation is skipped, without trying to re-partition the existing topic to a different number of partitions.

You can configure timeout for Kafka admin client calls used in topic creation using `quarkus.kafka.devservices.topic-partitions-timeout`, it defaults to 2 seconds.

<a id="redpanda-transactions"></a>
## Transactional and Idempotent producers support

By default, the Redpanda broker is configured to enable transactions and idempotence features. You can disable those using:

```properties
quarkus.kafka.devservices.redpanda.transaction-enabled=false
```

> [!NOTE]
> Redpanda transactions does not support exactly-once processing.

<a id="Compose"></a>
## Compose

The Kafka Dev Services supports [Compose Dev Services](/guides/compose-dev-services/index.md). It relies on a `compose-devservices.yml`, such as:

```yaml
name: <application name>
services:
  kafka:
    image: apache/kafka-native:4.2.0
    restart: "no"
    ports:
      - '9092'
    labels:
      io.quarkus.devservices.compose.exposed_ports: /etc/kafka/docker/ports
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_NUM_PARTITIONS: 3
    command: "/kafka.sh"
    volumes:
      - './kafka.sh:/kafka.sh'
```

For the broker to advertise its externally accessible address to clients, it requires an additional file `kafka.sh` as described in [Exposing port mappings to running containers](/guides/compose-dev-services/index.md#exposing-port-mappings-to-running-containers).

<a id="configuration-reference-devservices"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-enabled"></a> [`quarkus.kafka.devservices.enabled`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-enabled) config_property_copy_button:+++quarkus.kafka.devservices.enabled+++[]<br><br>If Dev Services for Kafka has been explicitly enabled or disabled. Dev Services are generally enabled by default, unless there is an existing configuration present. For Kafka, Dev Services starts a broker unless `kafka.bootstrap.servers` is set or if all the Reactive Messaging Kafka channel are configured with a `bootstrap.servers`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-port"></a> [`quarkus.kafka.devservices.port`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-port) config_property_copy_button:+++quarkus.kafka.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-provider"></a> [`quarkus.kafka.devservices.provider`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-provider) config_property_copy_button:+++quarkus.kafka.devservices.provider+++[]<br><br>Kafka dev service container type.<br><br>Redpanda, Strimzi, kafka-native and upstream kafka container providers are supported. Default is upstream-kafka-native.<br><br>For Redpanda: See <https://docs.redpanda.com/current/get-started/quick-start/> and <https://hub.docker.com/r/redpandadata/redpanda><br><br>For Strimzi: See <https://github.com/strimzi/test-container> and <https://quay.io/repository/strimzi-test-container/test-container><br><br>For Kafka Native: See <https://github.com/ozangunalp/kafka-native> and <https://quay.io/repository/ogunalp/kafka-native><br><br>For Upstream Kafka: See <https://hub.docker.com/r/apache/kafka> and <https://hub.docker.com/r/apache/kafka-native><br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_PROVIDER+++[] | `redpanda`, `strimzi`, `kafka-native`, `upstream-kafka`, `upstream-kafka-native` | `upstream-kafka-native` |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-image-name"></a> [`quarkus.kafka.devservices.image-name`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-image-name) config_property_copy_button:+++quarkus.kafka.devservices.image-name+++[]<br><br>The Kafka container image to use.<br><br>Dependent on the provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-shared"></a> [`quarkus.kafka.devservices.shared`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-shared) config_property_copy_button:+++quarkus.kafka.devservices.shared+++[]<br><br>Indicates if the Kafka broker managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Kafka starts a new container.<br><br>The discovery uses the `quarkus-dev-service-kafka` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-service-name"></a> [`quarkus.kafka.devservices.service-name`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-service-name) config_property_copy_button:+++quarkus.kafka.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-kafka` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Kafka looks for a container with the `quarkus-dev-service-kafka` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-kafka` label set to the specified value.<br><br>This property is used when you need multiple shared Kafka brokers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_SERVICE_NAME+++[] | string | `kafka` |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-topic-partitions-topic-name"></a> [`quarkus.kafka.devservices.topic-partitions."topic-name"`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-topic-partitions-topic-name) config_property_copy_button:+++quarkus.kafka.devservices.topic-partitions."topic-name"+++[]<br><br>The topic-partition pairs to create in the Dev Services Kafka broker. After the broker is started, given topics with partitions are created, skipping already existing topics. For example, `quarkus.kafka.devservices.topic-partitions.my-topic=2` will create a topic named `test` with 2 partitions.<br><br>The topic creation will not try to re-partition existing topics with different number of partitions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_TOPIC_PARTITIONS__TOPIC_NAME_+++[] | Map\<String,Integer> |  |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-topic-partitions-timeout"></a> [`quarkus.kafka.devservices.topic-partitions-timeout`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-topic-partitions-timeout) config_property_copy_button:+++quarkus.kafka.devservices.topic-partitions-timeout+++[]<br><br>Timeout for admin client calls used in topic creation.<br><br>Defaults to 2 seconds.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_TOPIC_PARTITIONS_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-kafka-client_quarkus-kafka-devservices) | `2S` |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-container-env-environment-variable-name"></a> [`quarkus.kafka.devservices.container-env."environment-variable-name"`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.kafka.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-redpanda-transaction-enabled"></a> [`quarkus.kafka.devservices.redpanda.transaction-enabled`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-redpanda-transaction-enabled) config_property_copy_button:+++quarkus.kafka.devservices.redpanda.transaction-enabled+++[]<br><br>Enables transaction support. Also enables the producer idempotence. Find more info about Redpanda transaction support on <https://vectorized.io/blog/fast-transactions/>. Notice that [KIP-447 (producer scalability for exactly once semantic)](https://cwiki.apache.org/confluence/display/KAFKA/KIP-447%3A+Producer+scalability+for+exactly+once+semantics) and [KIP-360 (Improve reliability of idempotent/transactional producer)](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=89068820) are *not* supported.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_REDPANDA_TRANSACTION_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-redpanda-proxy-port"></a> [`quarkus.kafka.devservices.redpanda.proxy-port`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-redpanda-proxy-port) config_property_copy_button:+++quarkus.kafka.devservices.redpanda.proxy-port+++[]<br><br>Port to access the Redpanda HTTP Proxy ([pandaproxy](https://docs.redpanda.com/current/develop/http-proxy/)).<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_REDPANDA_PROXY_PORT+++[] | int |  |
| [lock] <a id="quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-strimzi-server-configs-server-configs"></a> [`quarkus.kafka.devservices.strimzi.server-configs."server-configs"`](#quarkus-kafka-client_quarkus-kafka-devservices_quarkus-kafka-devservices-strimzi-server-configs-server-configs) config_property_copy_button:+++quarkus.kafka.devservices.strimzi.server-configs."server-configs"+++[]<br><br>Configuration properties that will be added to the server properties file provided to the Kafka broker by the Strimzi provider.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KAFKA_DEVSERVICES_STRIMZI_SERVER_CONFIGS__SERVER_CONFIGS_+++[] | Map\<String,String> |  |

<a id="duration-note-anchor-quarkus-kafka-client_quarkus-kafka-devservices"></a>

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
