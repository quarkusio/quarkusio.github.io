# Reactive Messaging AMQP 1.0 Connector reference documentation

This guide is the companion to the [Getting Started with AMQP 1.0](/guides/amqp/index.md). It explains in more details the configuration and usage of the AMQP connector for reactive messaging.

> [!TIP]
> This documentation does not cover all the details of the connector. Refer to the [SmallRye Reactive Messaging website](https://smallrye.io/smallrye-reactive-messaging) for further details.

The AMQP connector allows Quarkus applications to send and receive messages using the AMQP 1.0 protocol. More details about the protocol can be found in [the AMQP 1.0 specification](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-overview-v1.0-os.html). It's important to note that AMQP 1.0 and AMQP 0.9.1 (implemented by RabbitMQ) are incompatible. Check [Using RabbitMQ](#using-rabbitmq) to get more details.

## AMQP connector extension

To use the connector, you need to add the `quarkus-messaging-amqp` extension.

You can add the extension to your project using:

**CLI**

```bash
quarkus extension add quarkus-messaging-amqp
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-messaging-amqp'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-messaging-amqp'
```

Or just add the following dependency to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-messaging-amqp</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-messaging-amqp")
```

Once added to your project, you can map *channels* to AMQP addresses by configuring the `connector` attribute:

```properties
# Inbound
mp.messaging.incoming.[channel-name].connector=smallrye-amqp

# Outbound
mp.messaging.outgoing.[channel-name].connector=smallrye-amqp
```

> [!TIP]
> **Connector auto-attachment**
>
> If you have a single connector on your classpath, you can omit the `connector` attribute configuration. Quarkus automatically associates *orphan* channels to the (unique) connector found on the classpath. *Orphans* channels are outgoing channels without a downstream consumer or incoming channels without an upstream producer.
>
> This auto-attachment can be disabled using:
>
> ```properties
> quarkus.messaging.auto-connector-attachment=false
> ```

## Configuring the AMQP Broker access

The AMQP connector connects to AMQP 1.0 brokers such as Apache's ActiveMQ Classic or ActiveMQ Artemis. To configure the location and credentials of the broker, add the following properties in the `application.properties`:

```properties
amqp-host=amqp # <1>
amqp-port=5672 # <2>
amqp-username=my-username # <3>
amqp-password=my-password # <4>

mp.messaging.incoming.prices.connector=smallrye-amqp # <5>
```

1. Configures the broker/router host name. You can do it per channel (using the `host` attribute) or globally using `amqp-host`
2. Configures the broker/router port. You can do it per channel (using the `port` attribute) or globally using `amqp-port`. The default is `5672`.
3. Configures the broker/router username if required. You can do it per channel (using the `username` attribute) or globally using `amqp-username`.
4. Configures the broker/router password if required. You can do it per channel (using the `password` attribute) or globally using `amqp-password`.
5. Instructs the prices channel to be managed by the AMQP connector

In dev mode and when running tests, [Dev Services for AMQP](/guides/amqp-dev-services/index.md) automatically starts an AMQP broker.

## Receiving AMQP messages

Let's imagine your application receives `Message<Double>`. You can consume the payload directly:

```java
package inbound;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AmqpPriceConsumer {

    @Incoming("prices")
    public void consume(double price) {
        // process your price.
    }

}
```

Or, you can retrieve the Message\<Double>:

```java
package inbound;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class AmqpPriceMessageConsumer {

    @Incoming("prices")
    public CompletionStage<Void> consume(Message<Double> price) {
        // process your price.

        // Acknowledge the incoming message, marking the AMQP message as `accepted`.
        return price.ack();
    }

}
```

### Inbound Metadata

Messages coming from AMQP contain an instance of `IncomingAmqpMetadata` in the metadata.

```java
Optional<IncomingAmqpMetadata> metadata = incoming.getMetadata(IncomingAmqpMetadata.class);
metadata.ifPresent(meta -> {
    String address = meta.getAddress();
    String subject = meta.getSubject();
    boolean durable = meta.isDurable();
    // Use io.vertx.core.json.JsonObject
    JsonObject properties = meta.getProperties();
    // ...
});
```

### Deserialization

The connector converts incoming AMQP Messages into Reactive Messaging `Message<T>` instances. `T` depends on the *body* of the received AMQP Message.

The [AMQP Type System](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-types-v1.0-os.html) defines the supported types.

| AMQP Body Type | `<T>` |
| --- | --- |
| AMQP Value containing a [AMQP Primitive Type](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-types-v1.0-os.html#section-primitive-type-definitions) | the corresponding Java type |
| AMQP Value using the `Binary` type | `byte[]` |
| AMQP Sequence | `List` |
| AMQP Data (with binary content) and the `content-type` is set to `application/json` | [`JsonObject`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonObject.html) |
| AMQP Data with a different `content-type` | `byte[]` |

If you send objects with this AMQP connector (outbound connector), it gets encoded as JSON and sent as binary. The `content-type` is set to `application/json`. So, you can rebuild the object as follows:

```java
import io.vertx.core.json.JsonObject;
//
@ApplicationScoped
public static class Consumer {

    List<Price> prices = new CopyOnWriteArrayList<>();

    @Incoming("from-amqp") // <1>
    public void consume(JsonObject p) {   // <2>
        Price price = p.mapTo(Price.class);  // <3>
        prices.add(price);
    }

    public List<Price> list() {
        return prices;
    }
}
```

1. The `Price` instances are automatically encoded to JSON by the connector
2. You can receive it using a `JsonObject`
3. Then, you can reconstruct the instance using the `mapTo` method

> [!NOTE]
> The `mapTo` method uses the Quarkus Jackson mapper. Check [this guide](/guides/rest-json/index.md#json) to learn more about the mapper configuration.

### Acknowledgement

When a Reactive Messaging Message associated with an AMQP Message is acknowledged, it informs the broker that the message has been [accepted](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-accepted).

### Failure Management

If a message produced from an AMQP message is *nacked*, a failure strategy is applied. The AMQP connector supports six strategies:

- `fail` - fail the application; no more AMQP messages will be processed (default). The AMQP message is marked as rejected.
- `accept` - this strategy marks the AMQP message as *accepted*. The processing continues, ignoring the failure. Refer to the [accepted delivery state documentation](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-accepted).
- `release` - this strategy marks the AMQP message as *released*. The processing continues with the next message. The broker can redeliver the message. Refer to the [released delivery state documentation](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-released).
- `reject` - this strategy marks the AMQP message as rejected. The processing continues with the next message. Refer to the [rejected delivery state documentation](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-rejected).
- `modified-failed` - this strategy marks the AMQP message as *modified* and indicates that it failed (with the `delivery-failed` attribute). The processing continues with the next message, but the broker may attempt to redeliver the message. Refer to the [modified delivery state documentation](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-modified)
- `modified-failed-undeliverable-here` - this strategy marks the AMQP message as *modified* and indicates that it failed (with the `delivery-failed` attribute). It also indicates that the application cannot process the message, meaning that the broker will not attempt to redeliver the message to this node. The processing continues with the next message. Refer to the [modified delivery state documentation](https://docs.oasis-open.org/amqp/core/v1.0/os/amqp-core-messaging-v1.0-os.html#type-modified)

## Sending AMQP messages

### Serialization

When sending a `Message<T>`, the connector converts the message into an AMQP Message. The payload is converted to the AMQP Message *body*.

| `T` | AMQP Message Body |
| --- | --- |
| primitive types or `String` | AMQP Value with the payload |
| `Instant` or `UUID` | AMQP Value using the corresponding AMQP Type |
| [`JsonObject`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonObject.html) or [`JsonArray`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonArray.html) | AMQP Data using a binary content. The `content-type` is set to `application/json` |
| `io.vertx.mutiny.core.buffer.Buffer` | AMQP Data using a binary content. No `content-type` set |
| Any other class | The payload is converted to JSON (using a Json Mapper). The result is wrapped into AMQP Data using a **binary** content. The `content-type` is set to `application/json` |

If the message payload cannot be serialized to JSON, the message is *nacked*.

### Outbound Metadata

When sending `Messages`, you can add an instance of `OutgoingAmqpMetadata` to influence how the message is going to be sent to AMQP. For example, you can configure the subjects, properties:

```java
 OutgoingAmqpMetadata metadata = OutgoingAmqpMetadata.builder()
    .withDurable(true)
    .withSubject("my-subject")
    .build();

// Create a new message from the `incoming` message
// Add `metadata` to the metadata from the `incoming` message.
return incoming.addMetadata(metadata);
```

### Dynamic address names

Sometimes it is desirable to select the destination of a message dynamically. In this case, you should not configure the address inside your application configuration file, but instead, use the outbound metadata to set the address.

For example, you can send to a dynamic address based on the incoming message:

```java
String addressName = selectAddressFromIncomingMessage(incoming);
OutgoingAmqpMetadata metadata = OutgoingAmqpMetadata.builder()
    .withAddress(addressName)
    .withDurable(true)
    .build();

// Create a new message from the `incoming` message
// Add `metadata` to the metadata from the `incoming` message.
return incoming.addMetadata(metadata);
```

> [!NOTE]
> To be able to set the address per message, the connector is using an *anonymous sender*.

### Acknowledgement

By default, the Reactive Messaging `Message` is acknowledged when the broker acknowledges the message. When using routers, this acknowledgement may not be enabled. In this case, configure the `auto-acknowledgement` attribute to acknowledge the message as soon as it has been sent to the router.

If an AMQP message is rejected/released/modified by the broker (or cannot be sent successfully), the message is nacked.

### Back Pressure and Credits

The back-pressure is handled by AMQP *credits*. The outbound connector only requests the amount of allowed credits. When the amount of credits reaches 0, it waits (in a non-blocking fashion) until the broker grants more credits to the AMQP sender.

## Configuring the AMQP address

You can configure the AMQP address using the `address` attribute:

```properties
mp.messaging.incoming.prices.connector=smallrye-amqp
mp.messaging.incoming.prices.address=my-queue

mp.messaging.outgoing.orders.connector=smallrye-amqp
mp.messaging.outgoing.orders.address=my-order-queue
```

If the `address` attribute is not set, the connector uses the channel name.

To use an existing queue, you need to configure the `address`, `container-id`, and, optionally, the `link-name` attributes. For example, if you have an Apache ActiveMQ Artemis broker configured with:

```xml
<addresses>
    <address name="people">
        <anycast>
            <queue name="people">
                <user>artemis</user>
            </queue>
        </anycast>
    </address>
</addresses>
```

You need the following configuration:

```properties
mp.messaging.outgoing.people.connector=smallrye-amqp
mp.messaging.outgoing.people.durable=true
mp.messaging.outgoing.people.address=people
mp.messaging.outgoing.people.container-id=people
```

You may need to configure the `link-name` attribute, if the queue name is not the channel name:

```properties
mp.messaging.outgoing.people-out.connector=smallrye-amqp
mp.messaging.outgoing.people-out.durable=true
mp.messaging.outgoing.people-out.address=people
mp.messaging.outgoing.people-out.container-id=people
mp.messaging.outgoing.people-out.link-name=people
```

To use a `MULTICAST` queue, you need to provide the *FQQN* (fully-qualified queue name) instead of just the name of the queue:

```properties
mp.messaging.outgoing.people-out.connector=smallrye-amqp
mp.messaging.outgoing.people-out.durable=true
mp.messaging.outgoing.people-out.address=foo
mp.messaging.outgoing.people-out.container-id=foo

mp.messaging.incoming.people-out.connector=smallrye-amqp
mp.messaging.incoming.people-out.durable=true
mp.messaging.incoming.people-out.address=foo::bar # Note the syntax: address-name::queue-name
mp.messaging.incoming.people-out.container-id=bar
mp.messaging.incoming.people-out.link-name=people
```

More details about the AMQP Address model can be found in the [ActiveMQ Artemis documentation](https://activemq.apache.org/components/artemis/documentation/latest/address-model.html).

<a id="blocking-processing"></a>
### Execution model and Blocking processing

Reactive Messaging invokes your method on an I/O thread. See the [Quarkus Reactive Architecture documentation](/guides/quarkus-reactive-architecture/index.md) for further details on this topic. But, you often need to combine Reactive Messaging with blocking processing, such as database interactions. For this, you need to use the `@Blocking` annotation indicating that the processing is *blocking* and should not be run on the caller thread.

For example, The following code illustrates how you can store incoming payloads to a database using Hibernate with Panache:

```java
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PriceStorage {

    @Incoming("prices")
    @Transactional
    public void store(int priceInUsd) {
        Price price = new Price();
        price.value = priceInUsd;
        price.persist();
    }

}
```

> [!NOTE]
> There are 2 `@Blocking` annotations:
>
> 1. `io.smallrye.reactive.messaging.annotations.Blocking`
> 2. `io.smallrye.common.annotation.Blocking`
>
> They have the same effect. Thus, you can use both. The first one provides more fine-grained tuning such as the worker pool to use and whether it preserves the order. The second one, used also with other reactive features of Quarkus, uses the default worker pool and preserves the order.

> [!TIP]
> **@RunOnVirtualThread**
>
> For running the blocking processing on Java *virtual threads*, see the [Quarkus Virtual Thread support with Reactive Messaging documentation](/guides/messaging-virtual-threads/index.md).

> [!TIP]
> **@Transactional**
>
> If your method is annotated with `@Transactional`, it will be considered *blocking* automatically, even if the method is not annotated with `@Blocking`.

## Customizing the underlying AMQP client

The connector uses the Vert.x AMQP client underneath. More details about this client can be found in the [Vert.x website](https://vertx.io/docs/vertx-amqp-client/java/).

You can customize the underlying client configuration by producing an instance of `AmqpClientOptions` as follows:

```java
@Produces
@Identifier("my-named-options")
public AmqpClientOptions getNamedOptions() {
  // You can use the produced options to configure the TLS connection
  PemKeyCertOptions keycert = new PemKeyCertOptions()
    .addCertPath("./tls/tls.crt")
    .addKeyPath("./tls/tls.key");
  PemTrustOptions trust = new PemTrustOptions().addCertPath("./tls/ca.crt");
  return new AmqpClientOptions()
        .setSsl(true)
        .setPemKeyCertOptions(keycert)
        .setPemTrustOptions(trust)
        .addEnabledSaslMechanism("EXTERNAL")
        .setHostnameVerificationAlgorithm("") // Disables the hostname verification. Defaults is "HTTPS"
        .setConnectTimeout(30000)
        .setReconnectInterval(5000)
        .setContainerId("my-container");
}
```

This instance is retrieved and used to configure the client used by the connector. You need to indicate the name of the client using the `client-options-name` attribute:

```properties
mp.messaging.incoming.prices.client-options-name=my-named-options
```

If you experience frequent disconnections from the broker, the `AmqpClientOptions` can also be used to set a heartbeat if you need to keep the AMQP connection permanently. Some brokers might terminate the AMQP connection after a certain idle timeout. You can provide a heartbeat value which will be used by the Vert.x proton client to advertise the idle timeout when opening transport to a remote peer.

```java
@Produces
@Identifier("my-named-options")
public AmqpClientOptions getNamedOptions() {
  // set a heartbeat of 30s (in milliseconds)
  return new AmqpClientOptions()
        .setHeartbeat(30000);
}
```

## TLS Configuration

AMQP 1.0 Messaging extension integrates with the [Quarkus TLS registry](/guides/tls-registry-reference/index.md) to configure the Vert.x AMQP client.

To configure the TLS for an AMQP 1.0 channel, you need to provide a named TLS configuration in the `application.properties`:

```properties
quarkus.tls.your-tls-config.trust-store.pem.certs=ca.crt,ca2.pem
# ...
mp.messaging.incoming.prices.tls-configuration-name=your-tls-config
```

## Health reporting

If you use the AMQP connector with the `quarkus-smallrye-health` extension, it contributes to the readiness and liveness probes. The AMQP connector reports the readiness and liveness of each channel managed by the connector. At the moment, the AMQP connector uses the same logic for the readiness and liveness checks.

To disable health reporting, set the `health-enabled` attribute for the channel to false. On the inbound side (receiving messages from AMQP), the check verifies that the receiver is attached to the broker. On the outbound side (sending records to AMQP), the check verifies that the sender is attached to the broker.

Note that a message processing failures nacks the message, which is then handled by the `failure-strategy`. It is the responsibility of the `failure-strategy` to report the failure and influence the outcome of the checks. The `fail` failure strategy reports the failure, and so the check will report the fault.

<a id="using-rabbitmq"></a>
## Using RabbitMQ

This connector is for AMQP 1.0. RabbitMQ implements AMQP 0.9.1. RabbitMQ does not provide AMQP 1.0 by default, but there is a plugin for it. To use RabbitMQ with this connector, enable and configure the AMQP 1.0 plugin.

Despite the existence of the plugin, a few AMQP 1.0 features won’t work with RabbitMQ. Thus, we recommend the following configurations.

To receive messages from RabbitMQ:

- Set durable to false

```properties
mp.messaging.incoming.prices.connector=smallrye-amqp
mp.messaging.incoming.prices.durable=false
```

To send messages to RabbitMQ:

- set the destination address (anonymous sender are not supported)
- set `use-anonymous-sender` to false

```properties
mp.messaging.outgoing.generated-price.connector=smallrye-amqp
mp.messaging.outgoing.generated-price.address=prices
mp.messaging.outgoing.generated-price.use-anonymous-sender=false
```

As a consequence, it’s not possible to change the destination dynamically (using message metadata) when using RabbitMQ.

## Receiving Cloud Events

The AMQP connector supports [Cloud Events](https://cloudevents.io/). When the connector detects a *structured* or *binary* Cloud Events, it adds a  `IncomingCloudEventMetadata<T>` into the metadata of the `Message`. `IncomingCloudEventMetadata` contains accessors to the mandatory and optional Cloud Event attributes.

If the connector cannot extract the Cloud Event metadata, it sends the Message without the metadata.

For more information on receiving Cloud Events, see [Receiving Cloud Events](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/3.12/amqp/amqp.html#_receiving_cloud_events) in SmallRye Reactive Messaging documentation.

### Sending Cloud Events

The AMQP connector supports [Cloud Events](https://cloudevents.io/). The connector sends the outbound record as Cloud Events if:

- the message metadata contains an `io.smallrye.reactive.messaging.ce.OutgoingCloudEventMetadata` instance,
- the channel configuration defines the `cloud-events-type` and `cloud-events-source` attributes.

For more information on sending Cloud Events, see [Sending Cloud Events](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/3.12/amqp/amqp.html#_sending_cloud_events) in SmallRye Reactive Messaging documentation.

<a id="configuration-reference"></a>
## AMQP Connector Configuration Reference

### Quarkus specific configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-messaging-amqp_section_quarkus-amqp-devservices"></a> [Dev Services](#quarkus-messaging-amqp_section_quarkus-amqp-devservices) | Type | Default |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-enabled"></a> [`quarkus.amqp.devservices.enabled`](#quarkus-messaging-amqp_quarkus-amqp-devservices-enabled) config_property_copy_button:+++quarkus.amqp.devservices.enabled+++[]<br><br>If Dev Services for AMQP has been explicitly enabled or disabled. Dev Services are generally enabled by default, unless there is an existing configuration present. For AMQP, Dev Services starts a broker unless `amqp-host` or `amqp-port` are set or if all the Reactive Messaging AMQP channel are configured with `host` or `port`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-port"></a> [`quarkus.amqp.devservices.port`](#quarkus-messaging-amqp_quarkus-amqp-devservices-port) config_property_copy_button:+++quarkus.amqp.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-image-name"></a> [`quarkus.amqp.devservices.image-name`](#quarkus-messaging-amqp_quarkus-amqp-devservices-image-name) config_property_copy_button:+++quarkus.amqp.devservices.image-name+++[]<br><br>The image to use. Note that only ActiveMQ Artemis images are supported. Specifically, the image repository must end with `artemiscloud/activemq-artemis-broker`.<br><br>Check the [activemq-artemis-broker on Quay page](https://quay.io/repository/artemiscloud/activemq-artemis-broker) to find the available versions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_IMAGE_NAME+++[] | string | `quay.io/arkmq-org/arkmq-org-broker:artemis.2.55.0` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-extra-args"></a> [`quarkus.amqp.devservices.extra-args`](#quarkus-messaging-amqp_quarkus-amqp-devservices-extra-args) config_property_copy_button:+++quarkus.amqp.devservices.extra-args+++[]<br><br>The value of the `AMQ_EXTRA_ARGS` environment variable to pass to the container. For ActiveMQ Artemis Broker \<= 1.0.21, set this property to `--no-autotune --mapped --no-fsync --relax-jolokia --http-host 0.0.0.0`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_EXTRA_ARGS+++[] | string | `--no-autotune --mapped --no-fsync --relax-jolokia` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-shared"></a> [`quarkus.amqp.devservices.shared`](#quarkus-messaging-amqp_quarkus-amqp-devservices-shared) config_property_copy_button:+++quarkus.amqp.devservices.shared+++[]<br><br>Indicates if the AMQP broker managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for AMQP starts a new container.<br><br>The discovery uses the `quarkus-dev-service-amqp` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-service-name"></a> [`quarkus.amqp.devservices.service-name`](#quarkus-messaging-amqp_quarkus-amqp-devservices-service-name) config_property_copy_button:+++quarkus.amqp.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-aqmp` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for AMQP looks for a container with the `quarkus-dev-service-amqp` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-amqp` label set to the specified value.<br><br>This property is used when you need multiple shared AMQP brokers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_SERVICE_NAME+++[] | string | `amqp` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices-container-env-environment-variable-name"></a> [`quarkus.amqp.devservices.container-env."environment-variable-name"`](#quarkus-messaging-amqp_quarkus-amqp-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.amqp.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |

### Incoming channel configuration

| Attribute (*alias*) | Description | Mandatory | Default |
| --- | --- | --- | --- |
| **<mark>address</mark>** | The AMQP address. If not set, the channel name is used<br><br>Type: *string* | false |  |
| **<mark>auto-acknowledgement</mark>** | Whether the received AMQP messages must be acknowledged when received<br><br>Type: *boolean* | false | `false` |
| **<mark>broadcast</mark>** | Whether the received AMQP messages must be dispatched to multiple *subscribers*<br><br>Type: *boolean* | false | `false` |
| **<mark>capabilities</mark>** | A comma-separated list of capabilities proposed by the sender or receiver client.<br><br>Type: *string* | false |  |
| **<mark>client-options-name</mark>***<mark>(amqp-client-options-name)</mark>* | The name of the AMQP Client Option bean used to customize the AMQP client configuration<br><br>Type: *string* | false |  |
| **<mark>cloud-events</mark>** | Enables (default) or disables the Cloud Event support. If enabled on an *incoming* channel, the connector analyzes the incoming records and try to create Cloud Event metadata. If enabled on an *outgoing*, the connector sends the outgoing messages as Cloud Event if the message includes Cloud Event Metadata.<br><br>Type: *boolean* | false | `true` |
| **<mark>connect-timeout</mark>***<mark>(amqp-connect-timeout)</mark>* | The connection timeout in milliseconds<br><br>Type: *int* | false | `1000` |
| **<mark>container-id</mark>** | The AMQP container id<br><br>Type: *string* | false |  |
| **<mark>durable</mark>** | Whether AMQP subscription is durable<br><br>Type: *boolean* | false | `false` |
| **<mark>failure-strategy</mark>** | Specify the failure strategy to apply when a message produced from an AMQP message is nacked. Accepted values are `fail` (default), `accept`, `release`, `reject`, `modified-failed`, `modified-failed-undeliverable-here`<br><br>Type: *string* | false | `fail` |
| **<mark>health-timeout</mark>** | The max number of seconds to wait to determine if the connection with the broker is still established for the readiness check. After that threshold, the check is considered as failed.<br><br>Type: *int* | false | `3` |
| **<mark>host</mark>***<mark>(amqp-host)</mark>* | The broker hostname<br><br>Type: *string* | false | `localhost` |
| **<mark>link-name</mark>** | The name of the link. If not set, the channel name is used.<br><br>Type: *string* | false |  |
| **<mark>password</mark>***<mark>(amqp-password)</mark>* | The password used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>port</mark>***<mark>(amqp-port)</mark>* | The broker port<br><br>Type: *int* | false | `5672` |
| **<mark>reconnect-attempts</mark>***<mark>(amqp-reconnect-attempts)</mark>* | The number of reconnection attempts<br><br>Type: *int* | false | `100` |
| **<mark>reconnect-interval</mark>***<mark>(amqp-reconnect-interval)</mark>* | The interval in second between two reconnection attempts<br><br>Type: *int* | false | `10` |
| **<mark>sni-server-name</mark>***<mark>(amqp-sni-server-name)</mark>* | If set, explicitly override the hostname to use for the TLS SNI server name<br><br>Type: *string* | false |  |
| **<mark>selector</mark>** | Sets a message selector. This attribute is used to define an `apache.org:selector-filter:string` filter on the source terminus, using SQL-based syntax to request the server filters which messages are delivered to the receiver (if supported by the server in question). Precise functionality supported and syntax needed can vary depending on the server.<br><br>Type: *string* | false |  |
| **<mark>tracing-enabled</mark>** | Whether tracing is enabled (default) or disabled<br><br>Type: *boolean* | false | `true` |
| **<mark>use-ssl</mark>***<mark>(amqp-use-ssl)</mark>* | Whether the AMQP connection uses SSL/TLS<br><br>Type: *boolean* | false | `false` |
| **<mark>username</mark>***<mark>(amqp-username)</mark>* | The username used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>virtual-host</mark>***<mark>(amqp-virtual-host)</mark>* | If set, configure the hostname value used for the connection AMQP Open frame and TLS SNI server name (if TLS is in use)<br><br>Type: *string* | false |  |

### Outgoing channel configuration

| Attribute (*alias*) | Description | Mandatory | Default |
| --- | --- | --- | --- |
| **<mark>address</mark>** | The AMQP address. If not set, the channel name is used<br><br>Type: *string* | false |  |
| **<mark>capabilities</mark>** | A comma-separated list of capabilities proposed by the sender or receiver client.<br><br>Type: *string* | false |  |
| **<mark>client-options-name</mark>***<mark>(amqp-client-options-name)</mark>* | The name of the AMQP Client Option bean used to customize the AMQP client configuration<br><br>Type: *string* | false |  |
| **<mark>cloud-events</mark>** | Enables (default) or disables the Cloud Event support. If enabled on an *incoming* channel, the connector analyzes the incoming records and try to create Cloud Event metadata. If enabled on an *outgoing*, the connector sends the outgoing messages as Cloud Event if the message includes Cloud Event Metadata.<br><br>Type: *boolean* | false | `true` |
| **<mark>cloud-events-data-content-type</mark>***<mark>(cloud-events-default-data-content-type)</mark>* | Configure the default `datacontenttype` attribute of the outgoing Cloud Event. Requires `cloud-events` to be set to `true`. This value is used if the message does not configure the `datacontenttype` attribute itself<br><br>Type: *string* | false |  |
| **<mark>cloud-events-data-schema</mark>***<mark>(cloud-events-default-data-schema)</mark>* | Configure the default `dataschema` attribute of the outgoing Cloud Event. Requires `cloud-events` to be set to `true`. This value is used if the message does not configure the `dataschema` attribute itself<br><br>Type: *string* | false |  |
| **<mark>cloud-events-insert-timestamp</mark>***<mark>(cloud-events-default-timestamp)</mark>* | Whether the connector should insert automatically the `time` attribute into the outgoing Cloud Event. Requires `cloud-events` to be set to `true`. This value is used if the message does not configure the `time` attribute itself<br><br>Type: *boolean* | false | `true` |
| **<mark>cloud-events-mode</mark>** | The Cloud Event mode (`structured` or `binary` (default)). Indicates how the cloud events are written in the outgoing record<br><br>Type: *string* | false | `binary` |
| **<mark>cloud-events-source</mark>***<mark>(cloud-events-default-source)</mark>* | Configure the default `source` attribute of the outgoing Cloud Event. Requires `cloud-events` to be set to `true`. This value is used if the message does not configure the `source` attribute itself<br><br>Type: *string* | false |  |
| **<mark>cloud-events-subject</mark>***<mark>(cloud-events-default-subject)</mark>* | Configure the default `subject` attribute of the outgoing Cloud Event. Requires `cloud-events` to be set to `true`. This value is used if the message does not configure the `subject` attribute itself<br><br>Type: *string* | false |  |
| **<mark>cloud-events-type</mark>***<mark>(cloud-events-default-type)</mark>* | Configure the default `type` attribute of the outgoing Cloud Event. Requires `cloud-events` to be set to `true`. This value is used if the message does not configure the `type` attribute itself<br><br>Type: *string* | false |  |
| **<mark>connect-timeout</mark>***<mark>(amqp-connect-timeout)</mark>* | The connection timeout in milliseconds<br><br>Type: *int* | false | `1000` |
| **<mark>container-id</mark>** | The AMQP container id<br><br>Type: *string* | false |  |
| **<mark>credit-retrieval-period</mark>** | The period (in milliseconds) between two attempts to retrieve the credits granted by the broker. This time is used when the sender run out of credits.<br><br>Type: *int* | false | `2000` |
| **<mark>durable</mark>** | Whether sent AMQP messages are marked durable<br><br>Type: *boolean* | false | `false` |
| **<mark>health-timeout</mark>** | The max number of seconds to wait to determine if the connection with the broker is still established for the readiness check. After that threshold, the check is considered as failed.<br><br>Type: *int* | false | `3` |
| **<mark>host</mark>***<mark>(amqp-host)</mark>* | The broker hostname<br><br>Type: *string* | false | `localhost` |
| **<mark>link-name</mark>** | The name of the link. If not set, the channel name is used.<br><br>Type: *string* | false |  |
| **<mark>merge</mark>** | Whether the connector should allow multiple upstreams<br><br>Type: *boolean* | false | `false` |
| **<mark>password</mark>***<mark>(amqp-password)</mark>* | The password used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>port</mark>***<mark>(amqp-port)</mark>* | The broker port<br><br>Type: *int* | false | `5672` |
| **<mark>reconnect-attempts</mark>***<mark>(amqp-reconnect-attempts)</mark>* | The number of reconnection attempts<br><br>Type: *int* | false | `100` |
| **<mark>reconnect-interval</mark>***<mark>(amqp-reconnect-interval)</mark>* | The interval in second between two reconnection attempts<br><br>Type: *int* | false | `10` |
| **<mark>sni-server-name</mark>***<mark>(amqp-sni-server-name)</mark>* | If set, explicitly override the hostname to use for the TLS SNI server name<br><br>Type: *string* | false |  |
| **<mark>tracing-enabled</mark>** | Whether tracing is enabled (default) or disabled<br><br>Type: *boolean* | false | `true` |
| **<mark>ttl</mark>** | The time-to-live of the sent AMQP messages. 0 to disable the TTL<br><br>Type: *long* | false | `0` |
| **<mark>use-anonymous-sender</mark>** | Whether the connector should use an anonymous sender. The default value is `true` if the broker supports it, `false` otherwise. If not supported, it is not possible to dynamically change the destination address.<br><br>Type: *boolean* | false |  |
| **<mark>use-ssl</mark>***<mark>(amqp-use-ssl)</mark>* | Whether the AMQP connection uses SSL/TLS<br><br>Type: *boolean* | false | `false` |
| **<mark>username</mark>***<mark>(amqp-username)</mark>* | The username used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>virtual-host</mark>***<mark>(amqp-virtual-host)</mark>* | If set, configure the hostname value used for the connection AMQP Open frame and TLS SNI server name (if TLS is in use)<br><br>Type: *string* | false |  |

### Conditionally configure channels

You can configure the channels using a specific profile. Thus, the channels are only configured (and added to the application) when the specified profile is enabled.

To achieve this, you need:

1. Prefix the `mp.messaging.[incoming|outgoing].$channel` entries with `%my-profile` such as `%my-profile.mp.messaging.[incoming|outgoing].$channel.key=value`
2. Use the `@IfBuildProfile("my-profile")` on the CDI beans containing `@Incoming(channel)` and `@Outgoing(channel)` annotations that need only to be enabled when the profile is enabled.

Note that reactive messaging verifies that the graph is complete. So, when using such a conditional configuration, ensure the application works with and without the profile enabled.

Note that this approach can also be used to change the channel configuration based on a profile.
