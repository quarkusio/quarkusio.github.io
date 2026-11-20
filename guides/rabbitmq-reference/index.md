# Reactive Messaging RabbitMQ Connector reference documentation

This guide is the companion to the [Getting Started with RabbitMQ](/guides/rabbitmq/index.md). It explains in more details the configuration and usage of the RabbitMQ connector for reactive messaging.

> [!TIP]
> This documentation does not cover all the details of the connector. Refer to the [SmallRye Reactive Messaging website](https://smallrye.io/smallrye-reactive-messaging) for further details.

The RabbitMQ connector allows Quarkus applications to send and receive messages using the AMQP 0.9.1 protocol. More details about the protocol can be found in [the AMQP 0.9.1 specification](https://www.rabbitmq.com/amqp-0-9-1-reference.html#queue.bind.routing-key).

> [!IMPORTANT]
> The RabbitMQ connector is specifically designed for AMQP 0-9-1. While RabbitMQ 4 now supports the AMQP 1.0 protocol, the two protocols are fundamentally different. If you want to communicate with RabbitMQ using AMQP 1.0, we recommend using the [AMQP 1.0 connector](/guides/amqp-reference/index.md). Please note that using AMQP 1.0 with RabbitMQ may still have some limitations or reduced functionality compared to native 0-9-1 usage.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## RabbitMQ connector extension

To use the connector, you need to add the `quarkus-messaging-rabbitmq` extension.

You can add the extension to your project using:

```bash
> ./mvnw quarkus:add-extensions -Dextensions="quarkus-messaging-rabbitmq"
```

Or just add the following dependency to your project:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-messaging-rabbitmq</artifactId>
</dependency>
```

Once added to your project, you can map *channels* to RabbitMQ exchanges or queues by configuring the `connector` attribute:

```properties
# Inbound
mp.messaging.incoming.[channel-name].connector=smallrye-rabbitmq

# Outbound
mp.messaging.outgoing.[channel-name].connector=smallrye-rabbitmq
```

`outgoing` channels are mapped to RabbitMQ exchanges and `incoming` channels are mapped to RabbitMQ queues as required by the broker.

## Configuring the RabbitMQ Broker access

The RabbitMQ connector connects to RabbitMQ brokers. To configure the location and credentials of the broker, add the following properties in the `application.properties`:

```properties
rabbitmq-host=amqp # <1>
rabbitmq-port=5672 # <2>
rabbitmq-username=my-username # <3>
rabbitmq-password=my-password # <4>

mp.messaging.incoming.prices.connector=smallrye-rabbitmq # <5>
```

1. Configures the broker host name. You can do it per channel (using the `host` attribute) or globally using `rabbitmq-host`
2. Configures the broker port. You can do it per channel (using the `port` attribute) or globally using `rabbitmq-port`. The default is `5672`.
3. Configures the broker username if required. You can do it per channel (using the `username` attribute) or globally using `rabbitmq-username`.
4. Configures the broker password if required. You can do it per channel (using the `password` attribute) or globally using `rabbitmq-password`.
5. Instructs the prices channel to be managed by the RabbitMQ connector

In dev mode and when running tests, [Dev Services for RabbitMQ](/guides/rabbitmq-dev-services/index.md) automatically starts a RabbitMQ broker.

## Receiving RabbitMQ messages

Let's imagine your application receives `Message<Double>`. You can consume the payload directly:

```java
package inbound;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RabbitMQPriceConsumer {

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
public class RabbitMQPriceMessageConsumer {

    @Incoming("prices")
    public CompletionStage<Void> consume(Message<Double> price) {
        // process your price.

        // Acknowledge the incoming message, marking the RabbitMQ message as `accepted`.
        return price.ack();
    }

}
```

### Inbound Metadata

Messages coming from RabbitMQ contain an instance of `IncomingRabbitMQMetadata` in the metadata.

```java
Optional<IncomingRabbitMQMetadata> metadata = incoming.getMetadata(IncomingRabbitMQMetadata.class);
metadata.ifPresent(meta -> {
    final Optional<String> contentEncoding = meta.getContentEncoding();
    final Optional<String> contentType = meta.getContentType();
    final Optional<String> correlationId = meta.getCorrelationId();
    final Optional<ZonedDateTime> creationTime = meta.getCreationTime(ZoneId.systemDefault());
    final Optional<Integer> priority = meta.getPriority();
    final Optional<String> replyTo = meta.getReplyTo();
    final Optional<String> userId = meta.getUserId();

    // Access a single String-valued header
    final Optional<String> stringHeader = meta.getHeader("my-header", String.class);

    // Access all headers
    final Map<String,Object> headers = meta.getHeaders();
    // ...
});
```

### Deserialization

The connector converts incoming RabbitMQ Messages into Reactive Messaging `Message<T>` instances. The payload type `T` depends on the value of the RabbitMQ received message Envelope `content_type` and `content_encoding` properties.

| content_encoding | content_type | T |
| --- | --- | --- |
| *Value present* | *n/a* | `byte[]` |
| *No value* | `text/plain` | `String` |
| *No value* | `application/json` | a JSON element which can be a [`JsonArray`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonArray.html), [`JsonObject`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonObject.html), `String`, ...etc if the buffer contains an array, object, string, ...etc |
| *No value* | *Anything else* | `byte[]` |

If you send objects with this RabbitMQ connector (outbound connector), they are encoded as JSON and sent with `content_type` set to `application/json`. You can receive this payload using (Vert.x) JSON Objects, and then map it to the object class you want:

```java
@ApplicationScoped
public static class Generator {

    @Outgoing("to-rabbitmq")
    public Multi<Price> prices() { // <1>
        AtomicInteger count = new AtomicInteger();
        return Multi.createFrom().ticks().every(Duration.ofMillis(1000))
                .map(l -> new Price().setPrice(count.incrementAndGet()))
                .onOverflow().drop();
    }

}

@ApplicationScoped
public static class Consumer {

    List<Price> prices = new CopyOnWriteArrayList<>();

    @Incoming("from-rabbitmq")
    public void consume(JsonObject p) { // <2>
        Price price = p.mapTo(Price.class); // <3>
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

When a Reactive Messaging Message associated with a RabbitMQ Message is acknowledged, it informs the broker that the message has been *accepted*.

Whether you need to explicitly acknowledge the message depends on the `auto-acknowledgement` setting for the channel; if that is set to true then your message will be automatically acknowledged on receipt.

### Failure Management

If a message produced from a RabbitMQ message is nacked, a failure strategy is applied. The RabbitMQ connector supports three strategies, controlled by the failure-strategy channel setting:

- `fail` - fail the application; no more RabbitMQ messages will be processed. The RabbitMQ message is marked as rejected.
- `accept` - this strategy marks the RabbitMQ message as *accepted*. The processing continues, ignoring the failure.
- `reject` - this strategy marks the RabbitMQ message as rejected (default). The processing continues with the next message.

## Sending RabbitMQ messages

### Serialization

When sending a `Message<T>`, the connector converts the message into a RabbitMQ Message. The payload is converted to the RabbitMQ Message body.

| T | RabbitMQ Message Body |
| --- | --- |
| primitive types or `UUID`/`String` | String value with `content_type` set to `text/plain` |
| [`JsonObject`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonObject.html) or [`JsonArray`](https://vertx.io/docs/apidocs/io/vertx/core/json/JsonArray.html) | Serialized String payload with `content_type` set to `application/json` |
| `io.vertx.mutiny.core.buffer.Buffer` | Binary content, with `content_type` set to `application/octet-stream` |
| `byte[]` | Binary content, with `content_type` set to `application/octet-stream` |
| Any other class | The payload is converted to JSON (using a Json Mapper) then serialized with `content_type` set to `application/json` |

If the message payload cannot be serialized to JSON, the message is *nacked*.

### Outbound Metadata

When sending `Messages`, you can add an instance of `OutgoingRabbitMQMetadata` to influence how the message is handled by RabbitMQ. For example, you can configure the routing key, timestamp and headers:

```java
final OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
        .withHeader("my-header", "xyzzy")
        .withRoutingKey("urgent")
        .withTimestamp(ZonedDateTime.now())
        .build();

// Add `metadata` to the metadata of the outgoing message.
return Message.of("Hello", Metadata.of(metadata));
```

### Acknowledgement

By default, the Reactive Messaging `Message` is acknowledged when the broker acknowledges the message.

## Configuring the RabbitMQ Exchange/Queue

You can configure the RabbitMQ exchange or queue associated with a channel using properties on the channel configuration. `incoming` channels are mapped to RabbitMQ `queues` and `outgoing` channels are mapped to `RabbitMQ` exchanges. For example:

```properties
mp.messaging.incoming.prices.connector=smallrye-rabbitmq
mp.messaging.incoming.prices.queue.name=my-queue

mp.messaging.outgoing.orders.connector=smallrye-rabbitmq
mp.messaging.outgoing.orders.exchange.name=my-order-queue
```

If the `exchange.name` or `queue.name` attribute is not set, the connector uses the channel name.

To use an existing queue, you need to configure the `name` and set the exchange's or queue's `declare` property to `false`. For example, if you have a RabbitMQ broker configured with a `people` exchange and queue, you need the following configuration:

```properties
mp.messaging.incoming.people.connector=smallrye-rabbitmq
mp.messaging.incoming.people.queue.name=people
mp.messaging.incoming.people.queue.declare=false

mp.messaging.outgoing.people.connector=smallrye-rabbitmq
mp.messaging.outgoing.people.exchange.name=people
mp.messaging.outgoing.people.exchange.declare=false
```

<a id="blocking-processing"></a>
### Execution model and Blocking processing

Reactive Messaging invokes your method on an I/O thread. See the [Quarkus Reactive Architecture documentation](/guides/quarkus-reactive-architecture/index.md) for further details on this topic. But, you often need to combine Reactive Messaging with blocking processing such as database interactions. For this, you need to use the `@Blocking` annotation indicating that the processing is *blocking* and should not be run on the caller thread.

For example, The following code illustrates how you can store incoming payloads to a database using Hibernate with Panache:

```java
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PriceStorage {

    @Incoming("prices")
    @Blocking
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

## Customizing the underlying RabbitMQ client

The connector uses the Vert.x RabbitMQ client underneath. More details about this client can be found in the [Vert.x website](https://vertx.io/docs/vertx-rabbitmq-client/java/).

You can customize the underlying client configuration by producing an instance of `RabbitMQOptions` as follows:

```java
@Produces
@Identifier("my-named-options")
public RabbitMQOptions getNamedOptions() {
  PemKeyCertOptions keycert = new PemKeyCertOptions()
        .addCertPath("./tls/tls.crt")
        .addKeyPath("./tls/tls.key");
  PemTrustOptions trust = new PemTrustOptions().addCertPath("./tlc/ca.crt");
  // You can use the produced options to configure the TLS connection
  return new RabbitMQOptions()
        .setSsl(true)
        .setPemKeyCertOptions(keycert)
        .setPemTrustOptions(trust)
        .setUser("user1")
        .setPassword("password1")
        .setHost("localhost")
        .setPort(5672)
        .setVirtualHost("vhost1")
        .setConnectionTimeout(6000) // in milliseconds
        .setRequestedHeartbeat(60) // in seconds
        .setHandshakeTimeout(6000) // in milliseconds
        .setRequestedChannelMax(5)
        .setNetworkRecoveryInterval(500) // in milliseconds
        .setAutomaticRecoveryEnabled(true);
}
```

This instance is retrieved and used to configure the client used by the connector. You need to indicate the name of the client using the `client-options-name` attribute:

```properties
mp.messaging.incoming.prices.client-options-name=my-named-options
```

## TLS Configuration

RabbitMQ Messaging extension integrates with the [Quarkus TLS registry](/guides/tls-registry-reference/index.md) to configure the Vert.x RabbitMQ client.

To configure the TLS for a channel, you need to provide a named TLS configuration in the `application.properties`:

```properties
quarkus.tls.your-tls-config.trust-store.pem.certs=ca.crt,ca2.pem
# ...
mp.messaging.incoming.prices.tls-configuration-name=your-tls-config
```

## Health reporting

If you use the RabbitMQ connector with the `quarkus-smallrye-health` extension, it contributes to the readiness and liveness probes. The RabbitMQ connector reports the readiness and liveness of each channel managed by the connector.

To disable health reporting, set the `health-enabled` attribute for the channel to false.

On the inbound side (receiving messages from RabbitMQ), the check verifies that the receiver is connected to the broker.

On the outbound side (sending records to RabbitMQ), the check verifies that the sender is not disconnected from the broker; the sender *may* still be in an initialised state (connection not yet attempted), but this is regarded as live/ready.

Note that a message processing failure nacks the message, which is then handled by the `failure-strategy`. It's the responsibility of the `failure-strategy` to report the failure and influence the outcome of the checks. The `fail` failure strategy reports the failure, and so the check will report the fault.

<a id="dynamic-credentials"></a>
## Dynamic Credentials

Quarkus and the RabbitMQ connector support [Vault's RabbitMQ secrets engine](https://www.vaultproject.io/docs/secrets/rabbitmq) for generating short-lived dynamic credentials. This allows Vault to create and retire RabbitMQ credentials on a regular basis.

First we need to enable Vault's `rabbitmq` secret engine, configure it with RabbitMQ's connection and authentication information, and create a Vault role `my-role` (replace `10.0.0.3` by the actual host that is running the RabbitMQ container):

```bash
vault secrets enable rabbitmq

vault write rabbitmq/config/connection \
    connection_uri=http://10.0.0.3:15672 \
    username=guest \
    password=guest

vault write rabbitmq/roles/my-role \
    vhosts='{"/":{"write": ".*", "read": ".*"}}'
```

> [!NOTE]
> For this use case, user `guest` configured above needs to be a RabbitMQ admin user with the capability to create credentials.

Then we need to give a read capability to the Quarkus application on path `rabbitmq/creds/my-role`.

```bash
cat <<EOF | vault policy write vault-rabbitmq-policy -
path "secret/data/myapps/vault-rabbitmq-test/*" {
  capabilities = ["read"]
}
path "rabbitmq/creds/my-role" {
  capabilities = [ "read" ]
}
EOF
```

Now that Vault knows how to create users in RabbitMQ, we need to configure Quarkus to use a credentials-provider for RabbitMQ.

First we tell Quarkus to request dynamic credentials using a credentials-provider named `rabbitmq`.

```
quarkus.rabbitmq.credentials-provider = rabbitmq
```

Next we configure the `rabbitmq` credentials provider. The `credentials-role` option must be set to the name of the role we created in Vault, in our case `my-role`. The `credentials-mount` option must be set to `rabbitmq`.

```properties
quarkus.vault.credentials-provider.rabbitmq.credentials-role=my-role
quarkus.vault.credentials-provider.rabbitmq.credentials-mount=rabbitmq
```

> [!NOTE]
> The `credentials-mount` is used directly as the mount of the secret engine in Vault. Here we are using the default mount path of `rabbitmq`. If the RabbitMQ secret engine was mounted at a custom path, the `credentials-mount` option must be set to that path instead.

<a id="configuration-reference"></a>
## RabbitMQ Connector Configuration Reference

### Incoming channel configuration

| Attribute (*alias*) | Description | Mandatory | Default |
| --- | --- | --- | --- |
| **<mark>username</mark>***<mark>(rabbitmq-username)</mark>* | The username used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>password</mark>***<mark>(rabbitmq-password)</mark>* | The password used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>host</mark>***<mark>(rabbitmq-host)</mark>* | The broker hostname<br><br>Type: *string* | false | `localhost` |
| **<mark>port</mark>***<mark>(rabbitmq-port)</mark>* | The broker port<br><br>Type: *int* | false | `5672` |
| **<mark>ssl</mark>***<mark>(rabbitmq-ssl)</mark>* | Whether the connection should use SSL<br><br>Type: *boolean* | false | `false` |
| **<mark>trust-all</mark>***<mark>(rabbitmq-trust-all)</mark>* | Whether to skip trust certificate verification<br><br>Type: *boolean* | false | `false` |
| **<mark>trust-store-path</mark>***<mark>(rabbitmq-trust-store-path)</mark>* | The path to a JKS trust store<br><br>Type: *string* | false |  |
| **<mark>trust-store-password</mark>***<mark>(rabbitmq-trust-store-password)</mark>* | The password of the JKS trust store<br><br>Type: *string* | false |  |
| **<mark>credentials-provider-name</mark>***<mark>(rabbitmq-credentials-provider-name)</mark>* | The name of the RabbitMQ Credentials Provider bean used to provide dynamic credentials to the RabbitMQ client<br><br>Type: *string* | false |  |
| **<mark>connection-timeout</mark>** | The TCP connection timeout (ms); 0 is interpreted as no timeout<br><br>Type: *int* | false | `60000` |
| **<mark>handshake-timeout</mark>** | The AMQP 0-9-1 protocol handshake timeout (ms)<br><br>Type: *int* | false | `10000` |
| **<mark>automatic-recovery-enabled</mark>** | Whether automatic connection recovery is enabled<br><br>Type: *boolean* | false | `false` |
| **<mark>automatic-recovery-on-initial-connection</mark>** | Whether automatic recovery on initial connections is enabled<br><br>Type: *boolean* | false | `true` |
| **<mark>reconnect-attempts</mark>***<mark>(rabbitmq-reconnect-attempts)</mark>* | The number of reconnection attempts<br><br>Type: *int* | false | `100` |
| **<mark>reconnect-interval</mark>***<mark>(rabbitmq-reconnect-interval)</mark>* | The interval (in seconds) between two reconnection attempts<br><br>Type: *int* | false | `10` |
| **<mark>network-recovery-interval</mark>** | How long (ms) will automatic recovery wait before attempting to reconnect<br><br>Type: *int* | false | `5000` |
| **<mark>user</mark>** | The AMQP username to use when connecting to the broker<br><br>Type: *string* | false | `guest` |
| **<mark>include-properties</mark>** | Whether to include properties when a broker message is passed on the event bus<br><br>Type: *boolean* | false | `false` |
| **<mark>requested-channel-max</mark>** | The initially requested maximum channel number<br><br>Type: *int* | false | `2047` |
| **<mark>requested-heartbeat</mark>** | The initially requested heartbeat interval (seconds), zero for none<br><br>Type: *int* | false | `60` |
| **<mark>use-nio</mark>** | Whether usage of NIO Sockets is enabled<br><br>Type: *boolean* | false | `false` |
| **<mark>virtual-host</mark>***<mark>(rabbitmq-virtual-host)</mark>* | The virtual host to use when connecting to the broker<br><br>Type: *string* | false | `/` |
| **<mark>exchange.name</mark>** | The exchange that messages are published to or consumed from. If not set, the channel name is used. If set to `""`, the default exchange is used<br><br>Type: *string* | false |  |
| **<mark>exchange.durable</mark>** | Whether the exchange is durable<br><br>Type: *boolean* | false | `true` |
| **<mark>exchange.auto-delete</mark>** | Whether the exchange should be deleted after use<br><br>Type: *boolean* | false | `false` |
| **<mark>exchange.type</mark>** | The exchange type: direct, fanout, headers or topic (default)<br><br>Type: *string* | false | `topic` |
| **<mark>exchange.declare</mark>** | Whether to declare the exchange; set to false if the exchange is expected to be set up independently<br><br>Type: *boolean* | false | `true` |
| **<mark>tracing.enabled</mark>** | Whether tracing is enabled (default) or disabled<br><br>Type: *boolean* | false | `true` |
| **<mark>tracing.attribute-headers</mark>** | A comma-separated list of headers that should be recorded as span attributes. Relevant only if tracing.enabled=true<br><br>Type: *string* | false | `` |
| **<mark>queue.name</mark>** | The queue from which messages are consumed.<br><br>Type: *string* | true |  |
| **<mark>queue.durable</mark>** | Whether the queue is durable<br><br>Type: *boolean* | false | `true` |
| **<mark>queue.exclusive</mark>** | Whether the queue is for exclusive use<br><br>Type: *boolean* | false | `false` |
| **<mark>queue.auto-delete</mark>** | Whether the queue should be deleted after use<br><br>Type: *boolean* | false | `false` |
| **<mark>queue.declare</mark>** | Whether to declare the queue and binding; set to false if these are expected to be set up independently<br><br>Type: *boolean* | false | `true` |
| **<mark>queue.ttl</mark>** | If specified, the time (ms) for which a message can remain in the queue undelivered before it is dead<br><br>Type: *long* | false |  |
| **<mark>queue.single-active-consumer</mark>** | If set to true, only one consumer can actively consume messages<br><br>Type: *boolean* | false | `false` |
| **<mark>queue.x-queue-type</mark>** | If automatically declare queue, we can choose different types of queue \[quorum, classic, stream\]<br><br>Type: *string* | false | `classic` |
| **<mark>queue.x-queue-mode</mark>** | If automatically declare queue, we can choose different modes of queue \[lazy, default\]<br><br>Type: *string* | false | `default` |
| **<mark>max-incoming-internal-queue-size</mark>** | The maximum size of the incoming internal queue<br><br>Type: *int* | false |  |
| **<mark>connection-count</mark>** | The number of RabbitMQ connections to create for consuming from this queue. This might be necessary to consume from a sharded queue with a single client.<br><br>Type: *int* | false | `1` |
| **<mark>auto-bind-dlq</mark>** | Whether to automatically declare the DLQ and bind it to the binder DLX<br><br>Type: *boolean* | false | `false` |
| **<mark>dead-letter-queue-name</mark>** | The name of the DLQ; if not supplied will default to the queue name with '.dlq' appended<br><br>Type: *string* | false |  |
| **<mark>dead-letter-exchange</mark>** | A DLX to assign to the queue. Relevant only if auto-bind-dlq is true<br><br>Type: *string* | false | `DLX` |
| **<mark>dead-letter-exchange-type</mark>** | The type of the DLX to assign to the queue. Relevant only if auto-bind-dlq is true<br><br>Type: *string* | false | `direct` |
| **<mark>dead-letter-routing-key</mark>** | A dead letter routing key to assign to the queue; if not supplied will default to the queue name<br><br>Type: *string* | false |  |
| **<mark>dlx.declare</mark>** | Whether to declare the dead letter exchange binding. Relevant only if auto-bind-dlq is true; set to false if these are expected to be set up independently<br><br>Type: *boolean* | false | `false` |
| **<mark>dead-letter-queue-type</mark>** | If automatically declare DLQ, we can choose different types of DLQ \[quorum, classic, stream\]<br><br>Type: *string* | false | `classic` |
| **<mark>dead-letter-queue-mode</mark>** | If automatically declare DLQ, we can choose different modes of DLQ \[lazy, default\]<br><br>Type: *string* | false | `default` |
| **<mark>failure-strategy</mark>** | The failure strategy to apply when a RabbitMQ message is nacked. Accepted values are `fail`, `accept`, `reject` (default)<br><br>Type: *string* | false | `reject` |
| **<mark>broadcast</mark>** | Whether the received RabbitMQ messages must be dispatched to multiple *subscribers*<br><br>Type: *boolean* | false | `false` |
| **<mark>auto-acknowledgement</mark>** | Whether the received RabbitMQ messages must be acknowledged when received; if true then delivery constitutes acknowledgement<br><br>Type: *boolean* | false | `false` |
| **<mark>keep-most-recent</mark>** | Whether to discard old messages instead of recent ones<br><br>Type: *boolean* | false | `false` |
| **<mark>routing-keys</mark>** | A comma-separated list of routing keys to bind the queue to the exchange<br><br>Type: *string* | false | `#` |
| **<mark>content-type-override</mark>** | Override the content_type attribute of the incoming message, should be a valid MIME type<br><br>Type: *string* | false |  |
| **<mark>max-outstanding-messages</mark>** | The maximum number of outstanding/unacknowledged messages being processed by the connector at a time; must be a positive number<br><br>Type: *int* | false |  |

### Outgoing channel configuration

| Attribute (*alias*) | Description | Mandatory | Default |
| --- | --- | --- | --- |
| **<mark>automatic-recovery-enabled</mark>** | Whether automatic connection recovery is enabled<br><br>Type: *boolean* | false | `false` |
| **<mark>automatic-recovery-on-initial-connection</mark>** | Whether automatic recovery on initial connections is enabled<br><br>Type: *boolean* | false | `true` |
| **<mark>connection-timeout</mark>** | The TCP connection timeout (ms); 0 is interpreted as no timeout<br><br>Type: *int* | false | `60000` |
| **<mark>default-routing-key</mark>** | The default routing key to use when sending messages to the exchange<br><br>Type: *string* | false | `` |
| **<mark>default-ttl</mark>** | If specified, the time (ms) sent messages can remain in queues undelivered before they are dead<br><br>Type: *long* | false |  |
| **<mark>exchange.auto-delete</mark>** | Whether the exchange should be deleted after use<br><br>Type: *boolean* | false | `false` |
| **<mark>exchange.declare</mark>** | Whether to declare the exchange; set to false if the exchange is expected to be set up independently<br><br>Type: *boolean* | false | `true` |
| **<mark>exchange.durable</mark>** | Whether the exchange is durable<br><br>Type: *boolean* | false | `true` |
| **<mark>exchange.name</mark>** | The exchange that messages are published to or consumed from. If not set, the channel name is used. If set to `""`, the default exchange is used<br><br>Type: *string* | false |  |
| **<mark>exchange.type</mark>** | The exchange type: direct, fanout, headers or topic (default)<br><br>Type: *string* | false | `topic` |
| **<mark>handshake-timeout</mark>** | The AMQP 0-9-1 protocol handshake timeout (ms)<br><br>Type: *int* | false | `10000` |
| **<mark>host</mark>***<mark>(rabbitmq-host)</mark>* | The broker hostname<br><br>Type: *string* | false | `localhost` |
| **<mark>include-properties</mark>** | Whether to include properties when a broker message is passed on the event bus<br><br>Type: *boolean* | false | `false` |
| **<mark>max-inflight-messages</mark>** | The maximum number of messages to be written to RabbitMQ concurrently; must be a positive number<br><br>Type: *long* | false | `1024` |
| **<mark>max-outgoing-internal-queue-size</mark>** | The maximum size of the outgoing internal queue<br><br>Type: *int* | false |  |
| **<mark>network-recovery-interval</mark>** | How long (ms) will automatic recovery wait before attempting to reconnect<br><br>Type: *int* | false | `5000` |
| **<mark>password</mark>***<mark>(rabbitmq-password)</mark>* | The password used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>port</mark>***<mark>(rabbitmq-port)</mark>* | The broker port<br><br>Type: *int* | false | `5672` |
| **<mark>reconnect-attempts</mark>***<mark>(rabbitmq-reconnect-attempts)</mark>* | The number of reconnection attempts<br><br>Type: *int* | false | `100` |
| **<mark>reconnect-interval</mark>***<mark>(rabbitmq-reconnect-interval)</mark>* | The interval (in seconds) between two reconnection attempts<br><br>Type: *int* | false | `10` |
| **<mark>requested-channel-max</mark>** | The initially requested maximum channel number<br><br>Type: *int* | false | `2047` |
| **<mark>requested-heartbeat</mark>** | The initially requested heartbeat interval (seconds), zero for none<br><br>Type: *int* | false | `60` |
| **<mark>ssl</mark>***<mark>(rabbitmq-ssl)</mark>* | Whether the connection should use SSL<br><br>Type: *boolean* | false | `false` |
| **<mark>tracing.attribute-headers</mark>** | A comma-separated list of headers that should be recorded as span attributes. Relevant only if tracing.enabled=true<br><br>Type: *string* | false | `` |
| **<mark>tracing.enabled</mark>** | Whether tracing is enabled (default) or disabled<br><br>Type: *boolean* | false | `true` |
| **<mark>trust-all</mark>***<mark>(rabbitmq-trust-all)</mark>* | Whether to skip trust certificate verification<br><br>Type: *boolean* | false | `false` |
| **<mark>trust-store-password</mark>***<mark>(rabbitmq-trust-store-password)</mark>* | The password of the JKS trust store<br><br>Type: *string* | false |  |
| **<mark>trust-store-path</mark>***<mark>(rabbitmq-trust-store-path)</mark>* | The path to a JKS trust store<br><br>Type: *string* | false |  |
| **<mark>credentials-provider-name</mark>***<mark>(rabbitmq-credentials-provider-name)</mark>* | The name of the RabbitMQ Credentials Provider bean used to provide dynamic credentials to the RabbitMQ client<br><br>Type: *string* | false |  |
| **<mark>use-nio</mark>** | Whether usage of NIO Sockets is enabled<br><br>Type: *boolean* | false | `false` |
| **<mark>user</mark>** | The AMQP username to use when connecting to the broker<br><br>Type: *string* | false | `guest` |
| **<mark>username</mark>***<mark>(rabbitmq-username)</mark>* | The username used to authenticate to the broker<br><br>Type: *string* | false |  |
| **<mark>virtual-host</mark>***<mark>(rabbitmq-virtual-host)</mark>* | The virtual host to use when connecting to the broker<br><br>Type: *string* | false | `/` |
