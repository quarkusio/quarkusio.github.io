# WebSockets Next reference guide

The `quarkus-websockets-next` extension provides a modern declarative API to define WebSocket server and client endpoints.

## The WebSocket protocol

The *WebSocket* protocol, documented in the [RFC6455](https://datatracker.ietf.org/doc/html/rfc6455), establishes a standardized method for creating a bidirectional communication channel between a client and a server through a single TCP connection. Unlike HTTP, WebSocket operates as a distinct TCP protocol but is designed to function seamlessly alongside HTTP. For example, it reuses the same ports and is compatible with the same security mechanisms.

The interaction using WebSocket initiates with an HTTP request employing the 'Upgrade' header to transition to the WebSocket protocol. Instead of a `200 OK` response, the server replies with a `101 Switching Protocols` response to upgrade the HTTP connection to a WebSocket connection. Following this successful handshake, the TCP socket utilized in the initial HTTP upgrade request remains open, allowing both client and server to exchange messages in both directions continually.

## HTTP and WebSocket architecture styles

Despite WebSocket's compatibility with HTTP and its initiation through an HTTP request, it's crucial to recognize that the two protocols lead to distinctly different architectures and programming models.

With HTTP/REST, applications are structured around resources/endpoints that handle various HTTP methods and paths. Client interaction occurs through emitting HTTP requests with appropriate methods and paths, following a request-response pattern. The server routes incoming requests to corresponding handlers based on path, method, and headers and then replies with a well-defined response.

Conversely, WebSocket typically involves a single endpoint for the initial HTTP connection, after which all messages utilize the same TCP connection. It introduces an entirely different interaction model: asynchronous and message-driven.

WebSocket is a low-level transport protocol, in contrast to HTTP. Message formats, routing, or processing require prior agreement between the client and server regarding message semantics.

For WebSocket clients and servers, the `Sec-WebSocket-Protocol` header in the HTTP handshake request allows negotiation of a higher-level messaging protocol. In its absence, the server and client must establish their own conventions.

## Quarkus WebSockets vs. Quarkus WebSockets Next

This guide utilizes the `quarkus-websockets-next` extension, an implementation of the WebSocket API boasting enhanced efficiency and usability compared to the legacy `quarkus-websockets` extension. The original `quarkus-websockets` extension remains accessible, will receive ongoing support, but it's unlikely to receive feature development.

Unlike `quarkus-websockets`, the `quarkus-websockets-next` extension does **not** implement the Jakarta WebSocket specification. Instead, it introduces a modern API, prioritizing simplicity of use. Additionally, it's tailored to integrate with Quarkus' reactive architecture and networking layer seamlessly.

The annotations utilized by the Quarkus WebSockets next extension differ from those in JSR 356 despite, sometimes, sharing the same name. The JSR annotations carry a semantic that the Quarkus WebSockets Next extension does not follow.

## Project setup

To use the `websockets-next` extension, you need to add the `io.quarkus:quarkus-websockets-next` dependency to your project.

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-websockets-next</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-websockets-next")
```

## Endpoints

Both the [Server API](#server-api) and [Client API](#client-api) define *endpoints* that are used to consume and send messages. The endpoints are implemented as CDI beans and support injection. Endpoints declare [_callback methods_](#callback-methods) annotated with `@OnTextMessage`, `@OnBinaryMessage`, `@OnPingMessage`, `@OnPongMessage`, `@OnOpen`, `@OnClose` and `@OnError`. These methods are used to handle various WebSocket events. Typically, a method annotated with `@OnTextMessage` is called when the connected client sends a message to the server and vice versa.

> [!NOTE]
> The client API also includes [connectors](#client-connectors) that are used to configure and create new WebSocket connections.

<a id="server-endpoints"></a>
### Server endpoints

Server endpoints are classes annotated with `@io.quarkus.websockets.next.WebSocket`. The value of `WebSocket#path()` is used to define the path of the endpoint.

```java
package org.acme.websockets;

import io.quarkus.websockets.next.WebSocket;
import jakarta.inject.Inject;

@WebSocket(path = "/chat/{username}")
public class ChatWebSocket {

}
```

Thus, a client can connect to this web socket endpoint using `ws://localhost:8080/chat/your-name`. If TLS is used, the URL is `wss://localhost:8443/chat/your-name`.

> [!NOTE]
> The endpoint path is relative to the [root context](/guides/http-reference/index.md#context-path) configured by the `quarkus.http.root-path` (which is `/` by default). For example, if you add `quarkus.http.root-path=/api` to your `application.properties` then a client can connect to this endpoint using `http://localhost:8080/api/chat/the-name`.

<a id="client-endpoints"></a>
### Client endpoints

Client endpoints are classes annotated with `@io.quarkus.websockets.next.WebSocketClient`. The value of `WebSocketClient#path()` is used to define the path of the endpoint this client will be connected to.

```java
package org.acme.websockets;

import io.quarkus.websockets.next.WebSocketClient;
import jakarta.inject.Inject;

@WebSocketClient(path = "/chat/{username}")
public class ChatWebSocket {

}
```

> [!NOTE]
> Client endpoints are used to consume and send messages. You'll need the [connectors API](#client-connectors) to configure and open new WebSocket connections.

### Path parameters

The path of a WebSocket endpoint can contain path parameters. The syntax is the same as for JAX-RS resources: `{parameterName}`.

You can access the path parameter values using the `io.quarkus.websockets.next.WebSocketConnection#pathParam(String)` method, or `io.quarkus.websockets.next.WebSocketClientConnection#pathParam(String)` respectively. Alternatively, an endpoint callback method parameter annotated with `@io.quarkus.websockets.next.PathParam` is injected automatically.

**`WebSocketConnection#pathParam(String)` example**

```java
@Inject io.quarkus.websockets.next.WebSocketConnection connection;
// ...
String value = connection.pathParam("parameterName");
```

Path parameter values are always strings. If the path parameter is not present in the path, the `WebSocketConnection#pathParam(String)`/`WebSocketClientConnection#pathParam(String)` method returns `null`. If there is an endpoint callback method parameter annotated with `@PathParam` and the parameter name is not defined in the endpoint path, then the build fails.

> [!NOTE]
> Query parameters are not supported. However, you can access the query using `WebSocketConnection#handshakeRequest().query()`

### CDI scopes

Endpoints are managed as CDI beans. By default, the `@Singleton` scope is used. However, developers can specify alternative scopes to fit their specific requirements.

`@Singleton` and `@ApplicationScoped` endpoints are shared across all WebSocket connections. Therefore, implementations should be either stateless or thread-safe.

#### Session context

If an endpoint is annotated with `@SessionScoped`, or depends directly or indirectly on a `@SessionScoped` bean, then each WebSocket connection is associated with its own *session context*. The session context is active during endpoint callback invocation. Subsequent invocations of [Callback methods](#callback-methods) within the same connection utilize the same session context. The session context remains active until the connection is closed (usually when the `@OnClose` method completes execution), at which point it is terminated.

> [!TIP]
> It is also possible to set the `quarkus.websockets-next.server.activate-session-context` config property to `always`. In this case, the session context is always activated, no matter if a `@SessionScoped` bean participates in the dependency tree.

**`@SessionScoped` Endpoint**

```java
import jakarta.enterprise.context.SessionScoped;

@WebSocket(path = "/ws")
@SessionScoped <1>
public class MyWebSocket {

}
```

1. This server endpoint is not shared and is scoped to the session/connection.

#### Request context

Each WebSocket endpoint callback method execution is associated with a new CDI *request context* if the endpoint is:

- Annotated with the `@RequestScoped` annotation.
- Has a method annotated with a security annotation such as `@RolesAllowed`.
- Depends directly or indirectly on a `@RequestScoped` bean.
- Depends directly or indirectly on a CDI bean secured with a standard security annotation.

> [!TIP]
> It is also possible to set the `quarkus.websockets-next.server.activate-request-context` config property to `always`. In this case, the request context is always activated when an endpoint callback is invoked.

**`@RequestScoped` Endpoint**

```java
import jakarta.enterprise.context.RequestScoped;

@WebSocket(path = "/ws")
@RequestScoped <1>
public class MyWebSocket {

}
```

1. This server endpoint is instantiated for each callback method execution.

<a id="callback-methods"></a>
### Callback methods

A WebSocket endpoint may declare:

- At most one `@OnTextMessage` method: Handles the text messages from the connected client/server.
- At most one `@OnBinaryMessage` method: Handles the binary messages from the connected client/server.
- At most one `@OnPingMessage` method: Handles the ping messages from the connected client/server.
- At most one `@OnPongMessage` method: Handles the pong messages from the connected client/server.
- At most one `@OnOpen` method: Invoked when a connection is opened.
- At most one `@OnClose` method: Executed when the connection is closed.
- Any number of `@OnError` methods: Invoked when an error occurs; that is when an endpoint callback throws a runtime error, or when a conversion error occurs, or when a returned `io.smallrye.mutiny.Uni`/`io.smallrye.mutiny.Multi` receives a failure.

Only some endpoints need to include all methods. However, it must contain at least `@On[Text|Binary]Message` or `@OnOpen`.

An error is thrown at build time if any endpoint violates these rules. The static nested classes representing sub-websockets adhere to the same guidelines.

> [!IMPORTANT]
> Any methods annotated with `@OnTextMessage`, `@OnBinaryMessage`, `@OnOpen`, and `@OnClose` outside a WebSocket endpoint are considered erroneous and will result in the build failing with an appropriate error message.

### Processing messages

Methods receiving messages from the client are annotated with `@OnTextMessage` or `@OnBinaryMessage`.

`OnTextMessage` are invoked for every *text* message received from the client. `OnBinaryMessage` are invoked for every *binary* message the client receives.

#### Invocation rules

When invoking the callback methods, the *session* scope linked to the WebSocket connection remains active. In addition, the request scope is active until the completion of the method (or until it produces its result for async and reactive methods).

WebSocket Next supports *blocking* and *non-blocking* logic, akin to Quarkus REST, determined from the return type of the method and additional annotations such as `@Blocking` and `@NonBlocking`.

Here are the rules governing execution:

- Methods annotated with `@RunOnVirtualThread`, `@Blocking` or `@Transactional` are considered blocking.
- Methods declared in a class annotated with `@RunOnVirtualThread` are considered blocking.
- Methods annotated with `@NonBlocking` are considered non-blocking.
- Methods declared in a class annotated with `@Transactional` are considered blocking unless annotated with `@NonBlocking`.
- If the method does not declare any of the annotations listed above the execution model is derived from the return type:
  - Methods returning `Uni` and `Multi` are considered non-blocking.
  - Methods returning `void` or any other type are considered blocking.
- Kotlin `suspend` functions are always considered non-blocking and may not be annotated with `@Blocking`, `@NonBlocking` or `@RunOnVirtualThread` and may not be in a class annotated with `@RunOnVirtualThread`.
- Non-blocking methods must execute on the connection's event loop thread.
- Blocking methods must execute on a worker thread unless annotated with `@RunOnVirtualThread` or in a class annotated with `@RunOnVirtualThread`.
- Methods annotated with `@RunOnVirtualThread` or declared in class annotated with `@RunOnVirtualThread` must execute on a virtual thread, each invocation spawns a new virtual thread.

#### Method parameters

The method must accept exactly one message parameter:

- The message object (of any type).
- A `Multi<X>` with X as the message type.

However, it may also accept the following parameters:

\* `WebSocketConnection`/`WebSocketClientConnection` \* `HandshakeRequest` \* `String` parameters annotated with `@PathParam`

The message object represents the data sent and can be accessed as either raw content (`String`, `JsonObject`, `JsonArray`, `Buffer` or `byte[]`) or deserialized high-level objects, which is the recommended approach.

When receiving a `Multi`, the method is invoked once per connection, and the provided `Multi` receives the items transmitted by this connection. If the method returns a `Multi` (constructed from the received one), Quarkus will automatically subscribe to it and write the emitted items until completion, failure, or cancellation. However, if your method does not return a `Multi`, you must subscribe to the incoming `Multi` to consume the data.

Here are two examples:

```java
// No need to subscribe to the incoming Multi as the method returns a Multi derived from the incoming one
@OnTextMessage
public Multi<ChatMessage> stream(Multi<ChatMessage> incoming) {
    return incoming.log();
}

// ...

// Must subscribe to the incoming Multi as the method does not return a Multi, otherwise no data will be consumed
@OnTextMessage
public void stream(Multi<ChatMessage> incoming) {
    incoming.subscribe().with(item -> log(item));
}
```

See [When to subscribe to a Uni or Multi](#subscribe-or-not-subscribe) to learn more about subscribing to the incoming `Multi`.

#### Supported return types

Methods annotated with `@OnTextMessage` or `@OnBinaryMessage` can return various types to handle WebSocket communication efficiently:

- `void`: Indicates a blocking method where no explicit response is sent back to the client.
- `Uni<Void>`: Denotes a non-blocking method where the completion of the returned `Uni` signifies the end of processing. No explicit response is sent back to the client.
- An object of type `X` represents a blocking method in which the returned object is serialized and sent back to the client as a response.
- `Uni<X>`: Specifies a non-blocking method where the item emitted by the non-null `Uni` is sent to the client as a response.
- `Multi<X>`: Indicates a non-blocking method where the items emitted by the non-null `Multi` are sequentially sent to the client until completion or cancellation.
- Kotlin `suspend` function returning `Unit`: Denotes a non-blocking method where no explicit response is sent back to the client.
- Kotlin `suspend` function returning `X`: Specifies a non-blocking method where the returned item is sent to the client as a response.

Here are some examples of these methods:

```java
@OnTextMessage
void consume(Message m) {
// Process the incoming message. The method is called on an executor thread for each incoming message.
}

@OnTextMessage
Uni<Void> consumeAsync(Message m) {
// Process the incoming message. The method is called on an event loop thread for each incoming message.
// The method completes when the returned Uni emits its item.
}

@OnTextMessage
ResponseMessage process(Message m) {
// Process the incoming message and send a response to the client.
// The method is called for each incoming message.
// Note that if the method returns `null`, no response will be sent to the client.
}

@OnTextMessage
Uni<ResponseMessage> processAsync(Message m) {
// Process the incoming message and send a response to the client.
// The method is called for each incoming message.
// Note that if the method returns `null`, no response will be sent to the client. The method completes when the returned Uni emits its item.
}

@OnTextMessage
Multi<ResponseMessage> stream(Message m) {
// Process the incoming message and send multiple responses to the client.
// The method is called for each incoming message.
// The method completes when the returned Multi emits its completion signal.
// The method cannot return `null` (but an empty multi if no response must be sent)
}
```

Methods returning `Uni` and `Multi` are considered non-blocking. In addition, Quarkus automatically subscribes to the returned `Multi` / `Uni` and writes the emitted items until completion, failure, or cancellation. Failure or cancellation terminates the connection.

#### Streams

In addition to individual messages, WebSocket endpoints can handle streams of messages. In this case, the method receives a `Multi<X>` as a parameter. Each instance of `X` is deserialized using the same rules listed above.

The method receiving the `Multi` can either return another `Multi` or `void`. If the method returns a `Multi`, it does not have to subscribe to the incoming `multi`:

```java
@OnTextMessage
public Multi<ChatMessage> stream(Multi<ChatMessage> incoming) {
    return incoming.log();
}
```

This approach allows bi-directional streaming.

When the method returns `void`, and so does not return a `Multi`, the code must subscribe to the incoming `Multi`. Otherwise, no data will be consumed, and the connection will not be closed:

```java
@OnTextMessage
public void stream(Multi<ChatMessage> incoming) {
    incoming.subscribe().with(item -> log(item));
}
```

Also note that the `stream` method will complete before the `Multi` completes.

See [When to subscribe to a Uni or Multi](#subscribe-or-not-subscribe) to learn more about subscribing to the incoming `Multi`.

#### Skipping reply

When a method is intended to produce a message written to the client, it can emit `null`. Emitting `null` signifies no response to be sent to the client, allowing for skipping a response when needed.

#### JsonObject and JsonArray

Vert.x `JsonObject` and `JsonArray` instances bypass the serialization and deserialization mechanisms. Messages are sent as text messages.

#### OnOpen and OnClose methods

The WebSocket endpoint can also be notified when a client connects or disconnects.

This is done by annotating a method with `@OnOpen` or `@OnClose`:

```java
@OnOpen(broadcast = true)
public ChatMessage onOpen() {
    return new ChatMessage(MessageType.USER_JOINED, connection.pathParam("username"), null);
}

@Inject WebSocketConnection connection;

@OnClose
public void onClose() {
    ChatMessage departure = new ChatMessage(MessageType.USER_LEFT, connection.pathParam("username"), null);
    connection.broadcast().sendTextAndAwait(departure);
}
```

`@OnOpen` is triggered upon client connection, while `@OnClose` is invoked upon disconnection.

These methods have access to the *session-scoped* `WebSocketConnection` bean.

#### Parameters

Methods annotated with `@OnOpen` and `@OnClose` may accept the following parameters:

\* `WebSocketConnection`/`WebSocketClientConnection` \* `HandshakeRequest` \* `String` parameters annotated with `@PathParam`

An endpoint method annotated with `@OnClose` may also accept the `io.quarkus.websockets.next.CloseReason` parameter that may indicate a reason for closing a connection.

#### Supported return types

`@OnOpen` and `@OnClose` methods support different returned types.

For `@OnOpen` methods, the same rules as `@On[Text|Binary]Message` apply. Thus, a method annotated with `@OnOpen` can send messages to the client immediately after connecting. The supported return types for `@OnOpen` methods are:

- `void`: Indicates a blocking method where no explicit message is sent back to the connected client.
- `Uni<Void>`: Denotes a non-blocking method where the completion of the returned `Uni` signifies the end of processing. No message is sent back to the client.
- An object of type `X`: Represents a blocking method where the returned object is serialized and sent back to the client.
- `Uni<X>`: Specifies a non-blocking method where the item emitted by the non-null `Uni` is sent to the client.
- `Multi<X>`: Indicates a non-blocking method where the items emitted by the non-null `Multi` are sequentially sent to the client until completion or cancellation.
- Kotlin `suspend` function returning `Unit`: Denotes a non-blocking method where no explicit message is sent back to the client.
- Kotlin `suspend` function returning `X`: Specifies a non-blocking method where the returned item is sent to the client.

Items sent to the client are [serialized](#serialization) except for the `String`, `io.vertx.core.json.JsonObject`, `io.vertx.core.json.JsonArray`, `io.vertx.core.buffer.Buffer`, and `byte[]` types. In the case of `Multi`, Quarkus subscribes to the returned `Multi` and writes the items to the `WebSocket` as they are emitted. `String`, `JsonObject` and `JsonArray` are sent as text messages. `Buffers` and byte arrays are sent as binary messages.

For `@OnClose` methods, the supported return types include:

- `void`: The method is considered blocking.
- `Uni<Void>`: The method is considered non-blocking.
- Kotlin `suspend` function returning `Unit`: The method is considered non-blocking.

> [!NOTE]
> `@OnClose` methods declared on a server endpoint cannot send items to the connected client by returning objects. They can only send messages to the other clients by using the `WebSocketConnection` object.

<a id="error-handling"></a>
### Error handling

WebSocket endpoints can also be notified when an error occurs. A WebSocket endpoint method annotated with `@io.quarkus.websockets.next.OnError` is invoked when an endpoint callback throws a runtime error, or when a conversion error occurs, or when a returned `io.smallrye.mutiny.Uni`/`io.smallrye.mutiny.Multi` receives a failure.

The method must accept exactly one *error* parameter, i.e. a parameter that is assignable from `java.lang.Throwable`. The method may also accept the following parameters:

- `WebSocketConnection`/`WebSocketClientConnection`
- `HandshakeRequest`
- `String` parameters annotated with `@PathParam`

An endpoint may declare multiple methods annotated with `@io.quarkus.websockets.next.OnError`. However, each method must declare a different error parameter. The method that declares a most-specific supertype of the actual exception is selected.

> [!NOTE]
> The `@io.quarkus.websockets.next.OnError` annotation can be also used to declare a global error handler, i.e. a method that is not declared on a WebSocket endpoint. Such a method may not accept `@PathParam` parameters. Error handlers declared on an endpoint take precedence over the global error handlers.

When an error occurs but no error handler can handle the failure, Quarkus uses the strategy specified by `quarkus.websockets-next.server.unhandled-failure-strategy`. For server endpoints, the error message is logged and the connection is closed by default. For client endpoints, the error message is logged by default.

<a id="serialization"></a>
### Serialization and deserialization

The WebSocket Next extension supports automatic serialization and deserialization of messages.

Objects of type `String`, `JsonObject`, `JsonArray`, `Buffer`, and `byte[]` are sent as-is and bypass the serialization and deserialization. When no codec is provided, the serialization and deserialization convert the message from/to JSON automatically.

When you need to customize the serialization and deserialization, you can provide a custom codec.

#### Custom codec

To implement a custom codec, you must provide a CDI bean implementing:

- `io.quarkus.websockets.next.BinaryMessageCodec` for binary messages
- `io.quarkus.websockets.next.TextMessageCodec` for text messages

The following example shows how to implement a custom codec for a `Item` class:

```java
@Singleton
public class ItemBinaryMessageCodec implements BinaryMessageCodec<Item> {

    @Override
    public boolean supports(Type type) {
        // Allows selecting the right codec for the right type
        return type.equals(Item.class);
    }

    @Override
    public Buffer encode(Item value) {
        // Serialization
        return Buffer.buffer(value.toString());
    }

    @Override
    public Item decode(Type type, Buffer value) {
        // Deserialization
        return new Item(value.toString());
    }
}
```

`OnTextMessage` and `OnBinaryMessage` methods can also specify which codec should be used explicitly:

```java
@OnTextMessage(codec = MyInputCodec.class) // <1>
Item find(Item item) {
        //....
}
```

1. Specify the codec to use for both the deserialization and serialization of the message

When the serialization and deserialization must use a different codec, you can specify the codec to use for the serialization and deserialization separately:

```java
@OnTextMessage(
        codec = MyInputCodec.class, // <1>
        outputCodec = MyOutputCodec.class // <2>
Item find(Item item) {
        //....
}
```

1. Specify the codec to use for the deserialization of the incoming message
2. Specify the codec to use for the serialization of the outgoing message

### Ping/Pong messages

A [ping message](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.2) may serve as a keepalive or to verify the remote endpoint. A [pong message](https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.3) is sent in response to a ping message and it must have an identical payload.

#### Sending ping messages

Ping messages are optional and not sent by default. However, server and client endpoints can be configured to automatically send ping messages on an interval.

```properties
quarkus.websockets-next.server.auto-ping-interval=2 <1>
quarkus.websockets-next.client.auto-ping-interval=10 <2>
```

1. Sends a ping message from the server to each connected client every 2 seconds.
2. Sends a ping message from all connected client instances to their remote servers every 10 seconds.

Servers and clients can send ping messages programmatically at any time using `WebSocketConnection` or `WebSocketClientConnection`. There is a non-blocking variant: `Sender#sendPing(Buffer)` and a blocking variant: `Sender#sendPingAndAwait(Buffer)`.

#### Sending pong messages

Server and client endpoints will always respond to a ping message sent from the remote party with a corresponding pong message, using the application data from the ping message. This behavior is built-in and requires no additional code or configuration.

Servers and clients can send unsolicited pong messages that may serve as a unidirectional heartbeat using `WebSocketConnection` or `WebSocketClientConnection`. There is a non-blocking variant: `Sender#sendPong(Buffer)` and a blocking variant: `Sender#sendPongAndAwait(Buffer)`.

#### Handling ping/pong messages

Because ping messages are handled automatically and pong messages require no response, it is not necessary to write handlers for these messages to comply with the WebSocket protocol. However, it is sometimes useful to know when ping or pong messages are received by an endpoint.

The `@OnPingMessage` and `@OnPongMessage` annotations can be used to define callbacks that consume ping or pong messages sent from the remote party. An endpoint may declare at most one `@OnPingMessage` callback and at most  one `@OnPongMessage` callback. The callback method must return either `void` or `Uni<Void>` (or be a Kotlin `suspend` function returning `Unit`), and it must accept a single parameter of type `Buffer`.

```java
@OnPingMessage
void ping(Buffer data) {
    // an incoming ping that will automatically receive a pong
}

@OnPongMessage
void pong(Buffer data) {
    // an incoming pong in response to the last ping sent
}
```

<a id="inbound-processing-mode"></a>
### Inbound processing mode

WebSocket endpoints can define the mode used to process incoming events for a specific connection using the `@WebSocket#inboundProcessingMode()`, and `@WebSocketClient.inboundProcessingMode()` respectively. An incoming event can represent a message (text, binary, pong), opening connection and closing connection. By default, events are processed serially and ordering is guaranteed. This means that if an endpoint receives events `A` and `B` (in this particular order) then callback for event `B` will be invoked after the callback for event `A` completed. However, in some situations it is preferable to process events concurrently, i.e. with no ordering guarantees but also with no concurrency limits. For this cases, the `InboundProcessingMode#CONCURRENT` should be used.

<a id="max-pending-messages"></a>
#### Limiting the number of pending messages

A peer that sends messages faster than the application can process them may cause an unbounded number of messages to be buffered in memory; this is especially relevant when `InboundProcessingMode#CONCURRENT` is used. To prevent this, Quarkus bounds the number of *pending* (in-flight) messages, i.e. messages that a connection has received but has not yet fully processed, for a single connection. The limit is controlled by the following configuration property:

- `quarkus.websockets-next.server.max-pending-messages` for server endpoints, and
- `quarkus.websockets-next.client.max-pending-messages` for client endpoints.

When the limit is reached, the underlying WebSocket read stream is paused and no more messages are received until the pending messages are processed.

The default value is `256`. Set the property to `0` or a negative value to disable this back-pressure completely, in which case there is no limit on the number of in-flight messages.

```properties
quarkus.websockets-next.server.max-pending-messages=32
quarkus.websockets-next.client.max-pending-messages=32
```

> [!WARNING]
> Pausing the read stream also pauses the delivery of ping/pong control frames for the affected connection. Under sustained saturation, i.e. when the number of pending messages stays at the limit, automatic pong replies and `@OnPingMessage`/`@OnPongMessage` callbacks may be delayed.

<a id="server-api"></a>
## Server API

### HTTP server configuration

This extension reuses the *main* HTTP server.

Thus, the configuration of the WebSocket server is done in the `quarkus.http.` configuration section.

WebSocket paths configured within the application are concatenated with the root path defined by `quarkus.http.root` (which defaults to `/`). This concatenation ensures that WebSocket endpoints are appropriately positioned within the application's URL structure.

Refer to the [HTTP guide](/guides/http-reference/index.md) for more details.

### Sub-websockets endpoints

A `@WebSocket` endpoint can encapsulate static nested classes, which are also annotated with `@WebSocket` and represent *sub-websockets*. The resulting path of these sub-websockets concatenates the path from the enclosing class and the nested class. The resulting path is normalized, following the HTTP URL rules.

Sub-websockets inherit access to the path parameters declared in the `@WebSocket` annotation of both the enclosing and nested classes. The `consumePrimary` method within the enclosing class can access the `version` parameter in the following example. Meanwhile, the `consumeNested` method within the nested class can access both `version` and `id` parameters:

```java
@WebSocket(path = "/ws/v{version}")
public class MyPrimaryWebSocket {

    @OnTextMessage
    void consumePrimary(String s)    { ... }

    @WebSocket(path = "/products/{id}")
    public static class MyNestedWebSocket {

      @OnTextMessage
      void consumeNested(String s)    { ... }

    }
}
```

<a id="ws-connection"></a>
### WebSocket connection

The `io.quarkus.websockets.next.WebSocketConnection` object represents the WebSocket connection. Quarkus provides a `@SessionScoped` CDI bean that implements this interface and can be injected in a `WebSocket` endpoint and used to interact with the connected client.

Methods annotated with `@OnOpen`, `@OnTextMessage`, `@OnBinaryMessage`, and `@OnClose` can access the injected `WebSocketConnection` object:

```java
@Inject WebSocketConnection connection;
```

> [!NOTE]
> Note that outside of these methods, the `WebSocketConnection` object is not available. However, it is possible to [list all open connections](#list-open-connections).

The connection can be used to send messages to the client, access the path parameters, broadcast messages to all connected clients, etc.

```java
// Send a message:
connection.sendTextAndAwait("Hello!");

// Broadcast messages:
connection.broadcast().sendTextAndAwait(departure);

// Access path parameters:
String param = connection.pathParam("foo");
```

The `WebSocketConnection` provides both a blocking and a non-blocking method variants to send messages:

- `sendTextAndAwait(String message)`: Sends a text message to the client and waits for the message to be sent. It's blocking and should only be called from an executor thread.
- `sendText(String message)`: Sends a text message to the client. It returns a `Uni`. It's non-blocking. Make sure you or Quarkus subscribes to the returned `Uni` to send the message. If you return the `Uni` from a method invoked by Quarkus (like with Quarkus REST, Quarkus WebSocket Next or Quarkus Messaging), it will subscribe to it and send the message. For example:

```java
@POST
public Uni<Void> send() {
    return connection.sendText("Hello!"); // Quarkus automatically subscribes to the returned Uni and sends the message.
}
```

See [When to subscribe to a Uni or Multi](#subscribe-or-not-subscribe) to learn more about subscribing to the `Uni`.

<a id="list-open-connections"></a>
#### List open connections

It is also possible to list all open connections. Quarkus provides a CDI bean of type `io.quarkus.websockets.next.OpenConnections` that declares convenient methods to access the connections.

```java
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OpenConnections;

class MyBean {

  @Inject
  OpenConnections connections;

  void logAllOpenConnections() {
     Log.infof("Open connections: %s", connections.listAll()); // <1>
  }
}
```

1. `OpenConnections#listAll()` returns an immutable snapshot of all open connections at the given time.

There are also other convenient methods. For example, `OpenConnections#findByEndpointId(String)` makes it easy to find connections for a specific endpoint.

#### User data

It is also possible to associate arbitrary user data with a specific connection. The `io.quarkus.websockets.next.UserData` object obtained by the `WebSocketConnection#userData()` method represents mutable user data associated with a connection.

```java
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.UserData.TypedKey;

@WebSocket(path = "/endpoint/{username}")
class MyEndpoint {

  @Inject
  CoolService service;

  @OnOpen
  void open(WebSocketConnection connection) {
     connection.userData().put(TypedKey.forBoolean("isCool"), service.isCool(connection.pathParam("username"))); <1>
  }

  @OnTextMessage
  String process(String message) {
     if (connection.userData().get(TypedKey.forBoolean("isCool"))) { <2>
        return "Cool message processed!";
     } else {
        return "Message processed!";
     }
  }
}
```

1. `CoolService#isCool()` returns `Boolean` that is associated with the current connection.
2. The `TypedKey.forBoolean("isCool")` is the key used to obtain the data stored when the connection was created.

<a id="server-cdi-events"></a>
#### CDI events

Quarkus fires a CDI event of type `io.quarkus.websockets.next.WebSocketConnection` with qualifier `@io.quarkus.websockets.next.Open` asynchronously when a new connection is opened. Moreover, a CDI event of type `WebSocketConnection` with qualifier `@io.quarkus.websockets.next.Closed` is fired asynchronously when a connection is closed.

```java
import jakarta.enterprise.event.ObservesAsync;
import io.quarkus.websockets.next.Open;
import io.quarkus.websockets.next.WebSocketConnection;

class MyBean {

  void connectionOpened(@ObservesAsync @Open WebSocketConnection connection) { <1>
     // This observer method is called when a connection is opened...
  }
}
```

1. An asynchronous observer method is executed using the default blocking executor service.

<a id="websocket-next-security"></a>
### Security

Security capabilities are provided by the Quarkus Security extension. Any [Identity provider](/guides/security-identity-providers/index.md) can be used to convert authentication credentials on the initial HTTP request into a `SecurityIdentity` instance. The `SecurityIdentity` is then associated with the websocket connection. Authorization options are demonstrated in following sections.

> [!NOTE]
> When an OpenID Connect extension, `quarkus-oidc`, is used and token expires, Quarkus automatically closes connection.

<a id="secure-http-upgrade"></a>
#### Secure HTTP upgrade

An HTTP upgrade is secured when a standard security annotation is placed on an endpoint class or an HTTP Security policy is defined. The advantage of securing HTTP upgrade is less processing, the authorization is performed early and only once. You should always prefer HTTP upgrade security unless you need to perform an action on error (see [Secure WebSocket endpoint callback methods](#secure-callback-methods)) or a security check based on the payload (see [Secure server endpoints with permission checkers](#secure-endpoints-with-permission-checkers)).

**Use standard security annotation to secure an HTTP upgrade**

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@Authenticated <1>
@WebSocket(path = "/end")
public class Endpoint {

    @Inject
    SecurityIdentity currentIdentity;

    @OnOpen
    String open() {
        return "ready";
    }

    @OnTextMessage
    String echo(String message) {
        return message;
    }
}
```

1. Initial HTTP handshake ends with the 401 status for anonymous users. You can also redirect the handshake request on authorization failure with the `quarkus.websockets-next.server.security.auth-failure-redirect-url` configuration property.

> [!IMPORTANT]
> HTTP upgrade is only secured when a security annotation is declared on an endpoint class next to the `@WebSocket` annotation. Placing a security annotation on an endpoint bean will not secure bean methods, only the HTTP upgrade. You must always verify that your endpoint is secured as intended.

**Use HTTP Security policy to secure an HTTP upgrade**

```properties
quarkus.http.auth.permission.http-upgrade.paths=/end
quarkus.http.auth.permission.http-upgrade.policy=authenticated
```

Security annotations used during authentication must be placed on an endpoint class as well, for the `SecurityIdentity` is created before the websocket connection is opened.

**Select Bearer token authentication mechanism**

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.oidc.BearerTokenAuthentication;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@BearerTokenAuthentication <1>
@WebSocket(path = "/end")
public class Endpoint {

    @OnTextMessage
    String echo(String message) {
        return message;
    }

}
```

1. Require that an opening WebSocket handshake request is authenticated using the bearer token authentication. See the [Authentication mechanisms in Quarkus](/guides/security-authentication-mechanisms/index.md#use-annotations-for-path-based-auth) guide for more information about selecting authentication mechanisms with annotations.

```properties
quarkus.http.auth.proactive=false <1>
```

1. Start authenticating an opening WebSocket handshake request only when the `io.quarkus.oidc.BearerTokenAuthentication` annotation is detected.

#### Secure HTTP upgrade with the `@AuthorizationPolicy` annotation

You can bind custom named `HttpSecurityPolicy` to a WebSocket endpoint with the `@AuthorizationPolicy` security annotation. The `@AuthorizationPolicy` security annotation can only be used to secure the HTTP upgrade, thus placing this annotation on WebSocket endpoint methods fails the build during the validation.

**Example WebSocket endpoint with secured HTTP upgrade**

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.vertx.http.security.AuthorizationPolicy;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@AuthorizationPolicy(name = "custom") <1>
@WebSocket(path = "/end")
public class Endpoint {

    @OnTextMessage
    String echo(String message) {
        return message;
    }

}
```

1. Secure the HTTP upgrade with the `HttpSecurityPolicy` named `custom`.

**Example custom `HttpSecurityPolicy`**

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomHttpSecurityPolicy implements HttpSecurityPolicy {

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext routingContext, Uni<SecurityIdentity> identity,
                                            AuthorizationRequestContext requestContext) {
        if (customRequestAuthorization(routingContext)) {
            return CheckResult.permit();
        }
        return CheckResult.deny();
    }

    @Override
    public String name() {
        return "custom"; <1>
    }

    private static boolean customRequestAuthorization(RoutingContext routingContext) {
        // here comes your custom authorization check
        // for example, you can check headers:
        String authorization = routingContext.request().getHeader("Authorization");
        return verifyAuthorization(authorization);
    }

}
```

1. The `custom` name must match the name specified with the `io.quarkus.vertx.http.security.AuthorizationPolicy#name` attribute.

See also the [Custom HttpSecurityPolicy](/guides/security-authorize-web-endpoints-reference/index.md#custom-http-security-policy) section of the Authorization of web endpoints guide for more information about custom policies.

<a id="secure-callback-methods"></a>
#### Secure WebSocket endpoint callback methods

WebSocket endpoint callback methods can be secured with security annotations such as `io.quarkus.security.Authenticated`, `jakarta.annotation.security.RolesAllowed` and other annotations listed in the [Supported security annotations](/guides/security-authorize-web-endpoints-reference/index.md#standard-security-annotations) documentation.

For example:

```java
package io.quarkus.websockets.next.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/end")
public class Endpoint {

    @Inject
    SecurityIdentity currentIdentity;

    @OnOpen
    String open() {
        return "ready";
    }

    @RolesAllowed("admin")
    @OnTextMessage
    String echo(String message) { <1>
        return message;
    }

    @OnError
    String error(ForbiddenException t) { <2>
        return "forbidden:" + currentIdentity.getPrincipal().getName();
    }
}
```

1. The echo callback method can only be invoked if the current security identity has an `admin` role.
2. The error handler is invoked in case of the authorization failure.

<a id="secure-endpoints-with-permission-checkers"></a>
#### Secure server endpoints with permission checkers

WebSocket endpoints can be secured with the [permission checkers](/guides/security-authorize-web-endpoints-reference/index.md#permission-checker). We recommend to [Secure HTTP upgrade](#secure-http-upgrade) rather than individual endpoint methods. For example:

**Example of a WebSocket endpoint with secured HTTP upgrade**

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@PermissionsAllowed("product:premium")
@WebSocket(path = "/product/premium")
public class PremiumProductEndpoint {

    @OnTextMessage
    PremiumProduct getPremiumProduct(int productId) {
        return new PremiumProduct(productId);
    }

}
```

**Example of a permission checker authorizing the HTTP upgrade**

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.PermissionChecker;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PermissionChecker {

    @PermissionChecker("product:premium")
    public boolean canGetPremiumProduct(SecurityIdentity securityIdentity) { <1>
        String username = securityIdentity.getPrincipal().getName();

        RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(securityIdentity);
        String initialHttpUpgradePath = routingContext == null ? null : routingContext.normalizedPath();
        if (!isUserAllowedToAccessPath(initialHttpUpgradePath, username)) {
            return false;
        }

        return isPremiumCustomer(username);
    }

}
```

1. A permission checker authorizing an HTTP upgrade must declare exactly one method parameter, the `SecurityIdentity`.

It is also possible to run security checks on every message. For example, a message payload can be accessed like this:

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import jakarta.inject.Inject;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;

@WebSocket(path = "/product")
public class ProductEndpoint {

    private record Product(int id, String name) {}

    @Inject
    SecurityIdentity currentIdentity;

    @PermissionsAllowed("product:get")
    @OnTextMessage
    Product getProduct(int productId) { <1>
        return new Product(productId, "Product " + productId);
    }

    @OnError
    String error(ForbiddenException t) { <2>
        return "forbidden:" + currentIdentity.getPrincipal().getName();
    }

    @PermissionChecker("product:get")
    boolean canGetProduct(int productId) {
        String username = currentIdentity.getPrincipal().getName();
        return currentIdentity.hasRole("admin") || canUserGetProduct(productId, username);
    }
}
```

1. The `getProduct` callback method can only be invoked if the current security identity has an `admin` role or the user is allowed to get the product detail.
2. The error handler is invoked in case of the authorization failure.

More information about permission checkers can be found on the JavaDoc of [`@PermissionChecker`](https://javadoc.io/doc/io.quarkus.security/quarkus-security/latest/io.quarkus.security.api/io/quarkus/security/PermissionChecker.html).

#### Bearer token authentication

The [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md) expects that the bearer token is passed in the `Authorization` header during the initial HTTP handshake. Java WebSocket clients such as [WebSockets Next Client](#client-api) and [Vert.x WebSocketClient](https://vertx.io/docs/vertx-core/java/#_websockets_on_the_client) support adding custom headers to the WebSocket opening handshake. However, JavaScript clients that follow the [WebSockets API](https://websockets.spec.whatwg.org/#the-websocket-interface) do not support adding custom headers. Therefore, passing a bearer access token using a custom `Authorization` header is impossible with JavaScript-based WebSocket clients. The JavaScript WebSocket client only allows to configure the HTTP [Sec-WebSocket-Protocol](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Sec-WebSocket-Protocol) request header for negotiating a sub-protocol. If absolutely necessary, the `Sec-WebSocket-Protocol` header might be used as a carrier for custom headers, to provide a workaround the [WebSockets API](https://websockets.spec.whatwg.org/#the-websocket-interface) restrictions. Here is an example of a JavaScript client propagating the `Authorization` header as a sub-protocol value:

```javascript
const token = getBearerToken()
const quarkusHeaderProtocol = encodeURIComponent("quarkus-http-upgrade#Authorization#Bearer " + token) <1>
const socket = new WebSocket("wss://" + location.host + "/chat/" + username, ["bearer-token-carrier", quarkusHeaderProtocol]) <2>
```

1. Expected format for the Quarkus Header sub-protocol is `quarkus-http-upgrade#header-name#header-value`. Do not forget to encode the sub-protocol value as a URI component to avoid encoding issues.
2. Indicate 2 sub-protocols supported by the client, the sub-protocol of your choice and the Quarkus HTTP upgrade sub-protocol.

For the WebSocket server to accept the `Authorization` passed as a sub-protocol, we must:

- Configure our WebSocket server with the supported sub-protocols. When the WebSocket client provides a list of supported sub-protocols in the HTTP `Sec-WebSocket-Protocol` request header, the WebSocket server must agree to serve content with one of them.
- Enable Quarkus HTTP upgrade sub-protocol mapping to the opening WebSocket handshake request headers.

```properties
quarkus.websockets-next.server.supported-subprotocols=bearer-token-carrier
quarkus.websockets-next.server.propagate-subprotocol-headers=true
```

> [!WARNING]
> WebSocket security model is origin-based and is not designed for the client-side authentication with headers or cookies. For example, web browsers do not enforce the Same-origin policy for the opening WebSocket handshake request. When you plan to use bearer access tokens during the opening WebSocket handshake request, we strongly recommend to follow the additional security measures listed below to minimize the security risks:
>
> - Restrict supported Origins to trusted Origins only with the [CORS filter](/guides/security-cors/index.md#cors-filter).
> - Use the `wss` protocol to enforce encrypted HTTP connection via TLS.
> - Use a custom WebSocket ticket system which supplies a random token with the HTML page which hosts the JavaScript WebSockets client which must provide this token during the initial handshake request as a query parameter.

Before the bearer access token sent on the initial HTTP request expires, you can send a new bearer access token as part of a message and update current `SecurityIdentity` attached to the WebSocket server connection:

```java
package io.quarkus.websockets.next.test.security;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketSecurity;
import jakarta.inject.Inject;

@Authenticated
@WebSocket(path = "/end")
public class Endpoint {

    record Metadata(String token) {}
    record RequestDto(Metadata metadata, String message) {}

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    WebSocketSecurity webSocketSecurity;

    @OnTextMessage
    String echo(RequestDto request) {
        if (request.metadata != null && request.metadata.token != null) {
            webSocketSecurity.updateSecurityIdentity(request.metadata.token); <1>
        }
        String principalName = securityIdentity.getPrincipal().getName(); <2>
        return request.message + " " + principalName;
    }

}
```

1. Asynchronously update the `SecurityIdentity` attached to the WebSocket server connection.
2. The current `SecurityIdentity` instance is still available and can be used during the `SecurityIdentity` update.

The [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md) mechanism has builtin support for the `SecurityIdentity` update. If you use other authentication mechanisms, you must implement the `io.quarkus.security.identity.IdentityProvider` provider that supports the `io.quarkus.websockets.next.runtime.spi.security.WebSocketIdentityUpdateRequest` authentication request.

> [!IMPORTANT]
> The new bearer access token must have the same `sub` claim value as the token used during the initial HTTP request. Please also make sure the `SecurityIdentity` is only updated when necessary and the WebSocket message with credentials do not appear in your application logs. Always use the `wss` protocol to enforce encrypted HTTP connection via TLS when sending credentials as part of the WebSocket message.

WebSocket client applications have to send a new access token before previous one expires:

```html
<script type="module">
    import Keycloak from 'https://cdn.jsdelivr.net/npm/keycloak-js@26.2.0/lib/keycloak.js'
    const keycloak = new Keycloak({
        url: 'http://localhost:39245',
        realm: 'quarkus',
        clientId: 'websockets-js-client'
    });
    function getToken() {
        return keycloak.token
    }

    await keycloak
        .init({onLoad: 'login-required'})
        .then(() => console.log('User is now authenticated.'))
        .catch(err => console.log('User is NOT authenticated.', err));

    // open Web socket - reduced for brevity
    let connectionOpened = true;
    const subprotocols = [ "quarkus", encodeURI("quarkus-http-upgrade" + "#Authorization#Bearer " + getToken()) ]
    const socket = new WebSocket("wss://" + location.host + "/chat/username", subprotocols);

    setInterval(() => {
        keycloak
            .updateToken(15)
            .then(result => {
                if (result && connectionOpened) {
                    console.log('Token updated, sending new token to the server')
                    socket.send(JSON.stringify({
                        metadata: {
                            token: `${getToken()}`
                        }
                    }));
                }
            })
            .catch(err => console.error(err))
    }, 10000);
</script>
```

Complete example is located in the `security-openid-connect-websockets-next-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-websockets-next-quickstart).

### Inspect and/or reject HTTP upgrade

To inspect an HTTP upgrade, you must provide a CDI bean implementing the `io.quarkus.websockets.next.HttpUpgradeCheck` interface. Quarkus calls the `HttpUpgradeCheck#perform` method on every HTTP request that should be upgraded to a WebSocket connection. Inside this method, you can perform any business logic and/or reject the HTTP upgrade.

**Example HttpUpgradeCheck**

```java
package io.quarkus.websockets.next.test;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped <1>
public class ExampleHttpUpgradeCheck implements HttpUpgradeCheck {

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext ctx) {
        if (rejectUpgrade(ctx)) {
            return CheckResult.rejectUpgrade(400); <2>
        }
        return CheckResult.permitUpgrade();
    }

    private boolean rejectUpgrade(HttpUpgradeContext ctx) {
        var headers = ctx.httpRequest().headers();
        // implement your business logic in here
    }
}
```

1. The CDI beans implementing `HttpUpgradeCheck` interface can be either `@ApplicationScoped`, `@Singleton` or `@Dependent` beans, but never the `@RequestScoped` beans.
2. Reject the HTTP upgrade. Initial HTTP handshake ends with the 400 Bad Request response status code.

> [!TIP]
> You can choose WebSocket endpoints to which the `HttpUpgradeCheck` is applied with the `HttpUpgradeCheck#appliesTo` method.

<a id="limiting-number-of-connections"></a>
#### Limiting the number of connections

There is no built-in configuration property to cap the number of open connections. However, a stateful `HttpUpgradeCheck` can reject the HTTP upgrade once a limit is reached. The [`OpenConnections`](#list-open-connections) bean provides a live snapshot of the open connections and can be consulted to count the connections of a specific endpoint.

**Example HttpUpgradeCheck limiting the number of connections per endpoint**

```java
package io.quarkus.websockets.next.test;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.websockets.next.HttpUpgradeCheck;
import io.quarkus.websockets.next.OpenConnections;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConnectionLimitCheck implements HttpUpgradeCheck {

    private static final String ENDPOINT_ID = "foo-endpoint-id";

    @ConfigProperty(name = "foo-max-connections")
    int fooMaxConnections;

    @Inject
    OpenConnections connections;

    @Override
    public boolean appliesTo(String endpointId) {
        return ENDPOINT_ID.equals(endpointId); <1>
    }

    @Override
    public Uni<CheckResult> perform(HttpUpgradeContext context) {
        long current = connections.findByEndpointId(ENDPOINT_ID).size(); <2>
        if (current >= fooMaxConnections) {
            return CheckResult.rejectUpgrade(503); <3>
        }
        return CheckResult.permitUpgrade();
    }
}
```

1. Only apply this check to the `foo-endpoint-id` endpoint; the `perform` method is not invoked for HTTP upgrades of other endpoints.
2. The endpoint id defaults to the simple name of the endpoint class but can be set with `@WebSocket#endpointId()`.
3. Reject the HTTP upgrade with the 503 Service Unavailable response status code once the limit is reached.

> [!WARNING]
> The check runs *before* the HTTP upgrade completes, and a connection only appears in `OpenConnections` after it has been established. Therefore, the snapshot returned by `OpenConnections` may not account for connections that are being established concurrently, and the limit is not strictly enforced under high concurrency. If you need a strict cap, maintain your own counter (e.g. an `AtomicInteger` incremented in the check and decremented in an `@OnClose` callback).

### TLS

As a direct consequence of the fact this extension reuses the *main* HTTP server, all the relevant server configurations apply. See Refer to the [HTTP guide](/guides/http-reference/index.md#ssl) for more details.

### Hibernate multitenancy

The `RoutingContext` is not available after the HTTP upgrade. However, it is possible to inject the `WebSocketConnection` and access the headers of the initial HTTP request.

If a custom `TenantResolver` is used and you would like to combine REST/HTTP and WebSockets, the code may look like this:

```java
@RequestScoped
@PersistenceUnitExtension
public class CustomTenantResolver implements TenantResolver {

    @Inject
    RoutingContext context;
    @Inject
    WebSocketConnection connection;

    @Override
    public String getDefaultTenantId() {
        return "public";
    }

    @Override
    public String resolveTenantId() {
        String schema;
        try {
            //Handle WebSocket
            schema = connection.handshakeRequest().header("schema");
        } catch ( ContextNotActiveException e) {
            // Handle REST/HTTP
            schema = context.request().getHeader( "schema" );
        }

        if ( schema == null || schema.equalsIgnoreCase( "public" ) ) {
            return "public";
        }

        return schema;
    }
}
```

For more information on Hibernate multitenancy, refer to the [hibernate documentation](https://quarkus.io/guides/hibernate-orm#multitenancy).

<a id="client-api"></a>
## Client API

<a id="client-connectors"></a>
### Client connectors

A connector can be used to configure and open a new client connection backed by a client endpoint that is used to consume and send messages. Quarkus provides a CDI bean with bean type `io.quarkus.websockets.next.WebSocketConnector<CLIENT>` and default qualifier that can be injected in other beans. The actual type argument of an injection point is used to determine the client endpoint. The type is validated during build - if it does not represent a client endpoint the build fails.

Let’s consider the following client endpoint:

**Client endpoint**

```java
@WebSocketClient(path = "/endpoint/{name}")
public class ClientEndpoint {

    @OnTextMessage
    void onMessage(@PathParam String name, String message, WebSocketClientConnection connection) {
        // ...
    }
}
```

The connector for this client endpoint is used as follows:

**Connector**

```java
@Singleton
public class MyBean {

    @ConfigProperty(name = "endpoint.uri")
    URI myUri;

    @Inject
    WebSocketConnector<ClientEndpoint> connector; <1>

    void openAndSendMessage() {
        WebSocketClientConnection connection = connector
            .baseUri(uri) <2>
            .pathParam("name", "Roxanne") <3>
            .customizeOptions((connectOptions, clientOptions) -> { <4>
               // ...
            })
            .connectAndAwait();
        connection.sendTextAndAwait("Hi!"); <5>
    }
}
```

1. Inject the connector for `ClientEndpoint`.
2. If the base `URI` is not supplied we attempt to obtain the value from the config. The key consists of the client id and the `.base-uri` suffix.
3. Set the path param value. Throws `IllegalArgumentException` if the client endpoint path does not contain a parameter with the given name.
4. Optionally, customize client and connection options not configurable with the connector API or Quarkus configuration.
5. Use the connection to send messages, if needed.

> [!NOTE]
> If an application attempts to inject a connector for a missing endpoint, an error is thrown.

Connectors are not thread-safe and should not be used concurrently. Connectors should also not be reused. If you need to create multiple connections in a row you'll need to obtain a new connector instance programmatically using `Instance#get()`:

```java
import jakarta.enterprise.inject.Instance;

@Singleton
public class MyBean {

    @Inject
    Instance<WebSocketConnector<MyEndpoint>> connector;

    void connect() {
        var connection1 = connector.get().baseUri(uri)
                .addHeader("Foo", "alpha")
                .connectAndAwait();
        var connection2 = connector.get().baseUri(uri)
                .addHeader("Foo", "bravo")
                .connectAndAwait();
    }
}
```

#### Basic connector

In the case where the application developer does not need the combination of the client endpoint and the connector, a *basic connector* can be used. The basic connector is a simple way to create a connection and consume/send messages without defining a client endpoint.

**Basic connector**

```java
@Singleton
public class MyBean {

    @Inject
    BasicWebSocketConnector connector; <1>

    void openAndConsume() {
        WebSocketClientConnection connection = connector
            .baseUri(uri) <2>
            .path("/ws") <3>
            .executionModel(ExecutionModel.NON_BLOCKING) <4>
            .onTextMessage((c, m) -> { <5>
               // ...
            })
            .customizeOptions((connectOptions, clientOptions) -> { <6>
               // ...
            })
            .connectAndAwait();
    }
}
```

1. Inject the connector.
2. The base URI must be always set.
3. The additional path that should be appended to the base URI.
4. Set the execution model for callback handlers. By default, the callback may block the current thread. However in this case, the callback is executed on the event loop and may not block the current thread.
5. The lambda will be called for every text message sent from the server.
6. Optionally, customize client and connection options not configurable with the connector API or Quarkus configuration.

The basic connector is closer to a low-level API and is reserved for advanced users. However, unlike other low-level WebSocket clients, it is still a CDI bean and can be injected in other beans. It also provides a way to configure the execution model of the callbacks, ensuring optimal integration with the rest of Quarkus.

Connectors are not thread-safe and should not be used concurrently. Connectors should also not be reused. If you need to create multiple connections in a row you'll need to obtain a new connector instance programmatically using `Instance#get()`:

```java
import jakarta.enterprise.inject.Instance;

@Singleton
public class MyBean {

    @Inject
    Instance<BasicWebSocketConnector> connector;

    void connect() {
        var connection1 = connector.get().baseUri(uri)
                .addHeader("Foo", "alpha")
                .connectAndAwait();
        var connection2 = connector.get().baseUri(uri)
                .addHeader("Foo", "bravo")
                .connectAndAwait();
    }
}
```

<a id="ws-client-connection"></a>
### WebSocket client connection

The `io.quarkus.websockets.next.WebSocketClientConnection` object represents the WebSocket connection. Quarkus provides a `@SessionScoped` CDI bean that implements this interface and can be injected in a `WebSocketClient` endpoint and used to interact with the connected server.

Methods annotated with `@OnOpen`, `@OnTextMessage`, `@OnBinaryMessage`, and `@OnClose` can access the injected `WebSocketClientConnection` object:

```java
@Inject WebSocketClientConnection connection;
```

> [!NOTE]
> Note that outside of these methods, the `WebSocketClientConnection` object is not available. However, it is possible to [list all open client connections](#list-open-client-connections).

The connection can be used to send messages to the client, access the path parameters, etc.

```java
// Send a message:
connection.sendTextAndAwait("Hello!");

// Broadcast messages:
connection.broadcast().sendTextAndAwait(departure);

// Access path parameters:
String param = connection.pathParam("foo");
```

The `WebSocketClientConnection` provides both a blocking and a non-blocking method variants to send messages:

- `sendTextAndAwait(String message)`: Sends a text message to the client and waits for the message to be sent. It's blocking and should only be called from an executor thread.
- `sendText(String message)`: Sends a text message to the client. It returns a `Uni`. It's non-blocking. Make sure you or Quarkus subscribes to the returned `Uni` to send the message. If you return the `Uni` from a method invoked by Quarkus (like with Quarkus REST, Quarkus WebSocket Next or Quarkus Messaging), it will subscribe to it and send the message. For example:

```java
@POST
public Uni<Void> send() {
    return connection.sendText("Hello!"); // Quarkus automatically subscribes to the returned Uni and sends the message.
}
```

<a id="list-open-client-connections"></a>
#### List open client connections

It is also possible to list all open connections. Quarkus provides a CDI bean of type `io.quarkus.websockets.next.OpenClientConnections` that declares convenient methods to access the connections.

```java
import io.quarkus.logging.Log;
import io.quarkus.websockets.next.OpenClientConnections;

class MyBean {

  @Inject
  OpenClientConnections connections;

  void logAllOpenClinetConnections() {
     Log.infof("Open client connections: %s", connections.listAll()); <1>
  }
}
```

1. `OpenClientConnections#listAll()` returns an immutable snapshot of all open connections at the given time.

There are also other convenient methods. For example, `OpenClientConnections#findByClientId(String)` makes it easy to find connections for a specific endpoint.

#### User data

It is also possible to associate arbitrary user data with a specific connection. The `io.quarkus.websockets.next.UserData` object obtained by the `WebSocketClientConnection#userData()` method represents mutable user data associated with a connection.

```java
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.UserData.TypedKey;

@WebSocketClient(path = "/endpoint/{username}")
class MyEndpoint {

  @Inject
  CoolService service;

  @OnOpen
  void open(WebSocketClientConnection connection) {
     connection.userData().put(TypedKey.forBoolean("isCool"), service.isCool(connection.pathParam("username"))); <1>
  }

  @OnTextMessage
  String process(String message) {
     if (connection.userData().get(TypedKey.forBoolean("isCool"))) { <2>
        return "Cool message processed!";
     } else {
        return "Message processed!";
     }
  }
}
```

1. `CoolService#isCool()` returns `Boolean` that is associated with the current connection.
2. The `TypedKey.forBoolean("isCool")` is the key used to obtain the data stored when the connection was created.

##### Specify in connector

In some scenarios you may wish to associate user data with a connection to be created by a [connector](#client-connectors). In this case, you can set values on the connector instance prior to obtaining the connection. This is particularly useful if you need to do something when the connection is opened and the necessary context cannot be otherwise inferred.

**Connector**

```java
@Singleton
public class MyBean {

    @Inject
    MyService service;

    @Inject
    Instance<WebSocketConnector<MyEndpoint>> connectorInstance;

    public void openAndSendMessage(String internalId, String message) {
        var externalId = service.getExternalId(internalId);
        var connection = connectorInstance.get()
            .pathParam("externalId", externalId)
            .userData(TypedKey.forString("internalId"), internalId)
            .connectAndAwait();
        connection.sendTextAndAwait(message);
    }
}
```

**Endpoint**

```java
@WebSocketClient(path = "/endpoint/{externalId}")
class MyEndpoint {

    @Inject
    MyService service;

    @OnOpen
    void open(WebSocketClientConnection connection) {
        var internalId = connection.userData().get(TypedKey.forString("internalId"));
        service.doSomething(internalId);
    }
}
```

<a id="client-cdi-events"></a>
#### CDI events

Quarkus fires a CDI event of type `io.quarkus.websockets.next.WebSocketClientConnection` with qualifier `@io.quarkus.websockets.next.Open` asynchronously when a new connection is opened. Moreover, a CDI event of type `WebSocketClientConnection` with qualifier `@io.quarkus.websockets.next.Closed` is fired asynchronously when a connection is closed.

```java
import jakarta.enterprise.event.ObservesAsync;
import io.quarkus.websockets.next.Open;
import io.quarkus.websockets.next.WebSocketClientConnection;

class MyBean {

  void connectionOpened(@ObservesAsync @Open WebSocketClientConnection connection) { <1>
     // This observer method is called when a connection is opened...
  }
}
```

1. An asynchronous observer method is executed using the default blocking executor service.

### Configuring SSL/TLS

To establish a TLS connection, you need to configure a *named* configuration using the [TLS registry](/guides/tls-registry-reference/index.md). This is typically done via configuration:

```properties
quarkus.tls.my-ws-client.trust-store.p12.path=server-truststore.p12
quarkus.tls.my-ws-client.trust-store.p12.password=secret
```

With a *named* TLS configuration established, you can then configure the client to use it:

```properties
quarkus.websockets-next.client.tls-configuration-name=my-ws-client
```

Alternatively, you can supply the configuration name using the [connector](#client-connectors):

```java
@Singleton
public class MyBean {

    @Inject
    WebSocketConnector<MyEndpoint> connector;

    public void connect() {
        connector
            .tlsConfigurationName("my-ws-client")
            .connectAndAwait();
    }
}
```

A name supplied to the connector will override any statically configured name. This can be useful for establishing a default configuration which can be overridden at runtime as necessary.

> [!WARNING]
> When using the WebSocket client, using a *named* configuration is required to avoid conflicts with other TLS configurations. The client will not use the default TLS configuration.

When you configure a *named* TLS configuration, TLS is enabled by default.

<a id="traffic-logging"></a>
## Traffic logging

Quarkus can log the messages sent and received for debugging purposes. To enable traffic logging for the server, set the `quarkus.websockets-next.server.traffic-logging.enabled` configuration property to `true`. To enable traffic logging for the client, set the `quarkus.websockets-next.client.traffic-logging.enabled` configuration property to `true`. The payload of text messages is logged as well. However, the number of logged characters is limited. The default limit is 100, but you can change this limit with the `quarkus.websockets-next.server.traffic-logging.text-payload-limit` and `quarkus.websockets-next.client.traffic-logging.text-payload-limit` configuration property, respectively.

> [!TIP]
> The messages are only logged if the `DEBUG` level is enabled for the logger `io.quarkus.websockets.next.traffic`.

**Example server configuration**

```properties
quarkus.websockets-next.server.traffic-logging.enabled=true <1>
quarkus.websockets-next.server.traffic-logging.text-payload-limit=50 <2>

quarkus.log.category."io.quarkus.websockets.next.traffic".level=DEBUG <3>
```

1. Enables traffic logging.
2. Set the number of characters of a text message payload which will be logged.
3. Enable `DEBUG` level is for the logger `io.quarkus.websockets.next.traffic`.

<a id="subscribe-or-not-subscribe"></a>
## When to subscribe to a `Uni` or `Multi`

`Uni` and `Multi` are lazy types, which means that they do not start processing until they are subscribed to.

When you get (from a parameter or from a method you called) a `Uni` or a `Multi`, whether you should subscribe to it depends on the context:

- if you return the `Uni` or `Multi` in a method invoked by Quarkus (like with Quarkus REST, Quarkus WebSocket Next or Quarkus Messaging), Quarkus subscribes to it and processes the items emitted by the `Multi` or the item emitted by the `Uni`:

```java
@Incoming("...")
@Outgoing("...")
public Multi<String> process(Multi<String> input) {
    // No need to subscribe to the input Multi, the `process` method is called by Quarkus (Messaging).
    return input.map(String::toUpperCase);
}
```

When a `Uni` or `Multi` is returned from a method annotated with `@OnOpen`, `@OnTextMessage`, `@OnBinaryMessage`, or `@OnClose`, Quarkus subscribes to it automatically.

- if you do not return the `Uni` or `Multi` in a method invoked by Quarkus, you should subscribe to it:

```java
@Incoming("...")
@Outgoing("...")
public void process(Multi<String> input) {
    input.map(String::toUpperCase)
        .subscribe().with(s -> log(s));
}
```

<a id="telemetry"></a>
## Telemetry

When the OpenTelemetry extension is present, traces for opened and closed WebSocket connections are collected by default. If you do not require WebSocket traces, you can disable collecting of traces like in the example below:

```properties
quarkus.websockets-next.server.traces.enabled=false
quarkus.websockets-next.client.traces.enabled=false
```

When the Micrometer extension is present, Quarkus can collect metrics for messages, errors and bytes transferred. If you require WebSocket metrics, you can enable the metrics like in the example below:

```properties
quarkus.websockets-next.server.metrics.enabled=true
quarkus.websockets-next.client.metrics.enabled=true
```

> [!NOTE]
> Telemetry for the `BasicWebSocketConnector` is currently not supported.

<a id="websocket-next-configuration-reference"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-websockets-next_quarkus-websockets-next-server-activate-request-context"></a> [`quarkus.websockets-next.server.activate-request-context`](#quarkus-websockets-next_quarkus-websockets-next-server-activate-request-context) config_property_copy_button:+++quarkus.websockets-next.server.activate-request-context+++[]<br><br>Specifies the activation strategy for the CDI request context during endpoint callback invocation. By default, the request context is only activated if needed, i.e. if there is a bean with the given scope, or a bean annotated with a security annotation (such as `@RolesAllowed`), in the dependency tree of the endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_ACTIVATE_REQUEST_CONTEXT+++[] | tooltip:auto[The context is only activated if needed.], tooltip:always[The context is always activated.] | tooltip:auto[The context is only activated if needed.] |
| [lock] <a id="quarkus-websockets-next_quarkus-websockets-next-server-activate-session-context"></a> [`quarkus.websockets-next.server.activate-session-context`](#quarkus-websockets-next_quarkus-websockets-next-server-activate-session-context) config_property_copy_button:+++quarkus.websockets-next.server.activate-session-context+++[]<br><br>Specifies the activation strategy for the CDI session context during endpoint callback invocation. By default, the session context is only activated if needed, i.e. if there is a bean with the given scope in the dependency tree of the endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_ACTIVATE_SESSION_CONTEXT+++[] | tooltip:auto[The context is only activated if needed.], tooltip:always[The context is always activated.] | tooltip:auto[The context is only activated if needed.] |
| [lock] <a id="quarkus-websockets-next_quarkus-websockets-next-server-propagate-subprotocol-headers"></a> [`quarkus.websockets-next.server.propagate-subprotocol-headers`](#quarkus-websockets-next_quarkus-websockets-next-server-propagate-subprotocol-headers) config_property_copy_button:+++quarkus.websockets-next.server.propagate-subprotocol-headers+++[]<br><br>If enabled, the WebSocket opening handshake headers are enhanced with the 'Sec-WebSocket-Protocol' sub-protocol that match format 'quarkus-http-upgrade++<mark>++header-name++</mark>++header-value'. If the WebSocket client interface does not support setting headers to the WebSocket opening handshake, this is a way how to set authorization header required to authenticate user. The 'quarkus-http-upgrade' sub-protocol is removed and server selects from the sub-protocol one that is supported (don't forget to configure the 'quarkus.websockets-next.server.supported-subprotocols' property). **IMPORTANT: We strongly recommend to only enable this feature if the HTTP connection is encrypted via TLS, CORS origin check is enabled and custom WebSocket ticket system is in place. Please see the Quarkus WebSockets Next reference for more information.**<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_PROPAGATE_SUBPROTOCOL_HEADERS+++[] | boolean | `false` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-offer-per-message-compression"></a> [`quarkus.websockets-next.client.offer-per-message-compression`](#quarkus-websockets-next_quarkus-websockets-next-client-offer-per-message-compression) config_property_copy_button:+++quarkus.websockets-next.client.offer-per-message-compression+++[]<br><br>Compression Extensions for WebSocket are supported by default.<br><br>See also [RFC 7692](https://datatracker.ietf.org/doc/html/rfc7692)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_OFFER_PER_MESSAGE_COMPRESSION+++[] | boolean | `false` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-compression-level"></a> [`quarkus.websockets-next.client.compression-level`](#quarkus-websockets-next_quarkus-websockets-next-client-compression-level) config_property_copy_button:+++quarkus.websockets-next.client.compression-level+++[]<br><br>The compression level must be a value between 0 and 9. The default value is `io.vertx.core.http.HttpClientOptions#DEFAULT_WEBSOCKET_COMPRESSION_LEVEL`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_COMPRESSION_LEVEL+++[] | int |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-max-message-size"></a> [`quarkus.websockets-next.client.max-message-size`](#quarkus-websockets-next_quarkus-websockets-next-client-max-message-size) config_property_copy_button:+++quarkus.websockets-next.client.max-message-size+++[]<br><br>The maximum size of a message in bytes. The default values is `io.vertx.core.http.HttpClientOptions#DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_MAX_MESSAGE_SIZE+++[] | int |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-max-frame-size"></a> [`quarkus.websockets-next.client.max-frame-size`](#quarkus-websockets-next_quarkus-websockets-next-client-max-frame-size) config_property_copy_button:+++quarkus.websockets-next.client.max-frame-size+++[]<br><br>The maximum size of a frame in bytes. The default values is `io.vertx.core.http.HttpClientOptions#DEFAULT_MAX_WEBSOCKET_FRAME_SIZE`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_MAX_FRAME_SIZE+++[] | int |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-max-pending-messages"></a> [`quarkus.websockets-next.client.max-pending-messages`](#quarkus-websockets-next_quarkus-websockets-next-client-max-pending-messages) config_property_copy_button:+++quarkus.websockets-next.client.max-pending-messages+++[]<br><br>The maximum number of pending (in-flight) messages processed concurrently for a single connection.<br><br>When set to a positive value, the connection applies back-pressure so that no more than this number of messages are received but not yet fully processed, bounding the memory a connection can retain. Set to `0` or a negative value to disable back-pressure completely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_MAX_PENDING_MESSAGES+++[] | int | `256` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-auto-ping-interval"></a> [`quarkus.websockets-next.client.auto-ping-interval`](#quarkus-websockets-next_quarkus-websockets-next-client-auto-ping-interval) config_property_copy_button:+++quarkus.websockets-next.client.auto-ping-interval+++[]<br><br>The interval after which, when set, the client sends a ping message to a connected server automatically.<br><br>Ping messages are not sent automatically by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_AUTO_PING_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-websockets-next_quarkus-websockets-next) |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-connection-idle-timeout"></a> [`quarkus.websockets-next.client.connection-idle-timeout`](#quarkus-websockets-next_quarkus-websockets-next-client-connection-idle-timeout) config_property_copy_button:+++quarkus.websockets-next.client.connection-idle-timeout+++[]<br><br>If set then a connection will be closed if no data is received nor sent within the given timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_CONNECTION_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-websockets-next_quarkus-websockets-next) |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-connection-closing-timeout"></a> [`quarkus.websockets-next.client.connection-closing-timeout`](#quarkus-websockets-next_quarkus-websockets-next-client-connection-closing-timeout) config_property_copy_button:+++quarkus.websockets-next.client.connection-closing-timeout+++[]<br><br>The amount of time a client will wait until it closes the TCP connection after sending a close frame. Any value will be `Duration#toSeconds() converted to seconds` and limited to `Integer#MAX_VALUE`. The default value is `io.vertx.core.http.HttpClientOptions#DEFAULT_WEBSOCKET_CLOSING_TIMEOUT`s<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_CONNECTION_CLOSING_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-websockets-next_quarkus-websockets-next) |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-unhandled-failure-strategy"></a> [`quarkus.websockets-next.client.unhandled-failure-strategy`](#quarkus-websockets-next_quarkus-websockets-next-client-unhandled-failure-strategy) config_property_copy_button:+++quarkus.websockets-next.client.unhandled-failure-strategy+++[]<br><br>The strategy used when an error occurs but no error handler can handle the failure.<br><br>By default, the error message is logged when an unhandled failure occurs.<br><br>Note that clients should not close the WebSocket connection arbitrarily. See also RFC-6455 [section 7.3](https://datatracker.ietf.org/doc/html/rfc6455#section-7.3).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_UNHANDLED_FAILURE_STRATEGY+++[] | tooltip:log-and-close[Log the error message and close the connection.], tooltip:close[Close the connection silently.], tooltip:log[Log the error message.], tooltip:noop[No operation.] | tooltip:log[Log the error message.] |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-tls-configuration-name"></a> [`quarkus.websockets-next.client.tls-configuration-name`](#quarkus-websockets-next_quarkus-websockets-next-client-tls-configuration-name) config_property_copy_button:+++quarkus.websockets-next.client.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-traffic-logging-enabled"></a> [`quarkus.websockets-next.client.traffic-logging.enabled`](#quarkus-websockets-next_quarkus-websockets-next-client-traffic-logging-enabled) config_property_copy_button:+++quarkus.websockets-next.client.traffic-logging.enabled+++[]<br><br>If set to true then binary/text messages received/sent are logged if the `DEBUG` level is enabled for the logger `io.quarkus.websockets.next.traffic`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_TRAFFIC_LOGGING_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-traffic-logging-text-payload-limit"></a> [`quarkus.websockets-next.client.traffic-logging.text-payload-limit`](#quarkus-websockets-next_quarkus-websockets-next-client-traffic-logging-text-payload-limit) config_property_copy_button:+++quarkus.websockets-next.client.traffic-logging.text-payload-limit+++[]<br><br>The number of characters of a text message which will be logged if traffic logging is enabled. The payload of a binary message is never logged.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_TRAFFIC_LOGGING_TEXT_PAYLOAD_LIMIT+++[] | int | `100` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-traces-enabled"></a> [`quarkus.websockets-next.client.traces.enabled`](#quarkus-websockets-next_quarkus-websockets-next-client-traces-enabled) config_property_copy_button:+++quarkus.websockets-next.client.traces.enabled+++[]<br><br>If collection of WebSocket traces is enabled. Only applicable when the OpenTelemetry extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_TRACES_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-client-metrics-enabled"></a> [`quarkus.websockets-next.client.metrics.enabled`](#quarkus-websockets-next_quarkus-websockets-next-client-metrics-enabled) config_property_copy_button:+++quarkus.websockets-next.client.metrics.enabled+++[]<br><br>If collection of WebSocket metrics is enabled. Only applicable when the Micrometer extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_CLIENT_METRICS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-supported-subprotocols"></a> [`quarkus.websockets-next.server.supported-subprotocols`](#quarkus-websockets-next_quarkus-websockets-next-server-supported-subprotocols) config_property_copy_button:+++quarkus.websockets-next.server.supported-subprotocols+++[]<br><br>See [The WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455#page-12)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_SUPPORTED_SUBPROTOCOLS+++[] | list of string |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-per-message-compression-supported"></a> [`quarkus.websockets-next.server.per-message-compression-supported`](#quarkus-websockets-next_quarkus-websockets-next-server-per-message-compression-supported) config_property_copy_button:+++quarkus.websockets-next.server.per-message-compression-supported+++[]<br><br>Compression Extensions for WebSocket are supported by default.<br><br>See also [RFC 7692](https://datatracker.ietf.org/doc/html/rfc7692)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_PER_MESSAGE_COMPRESSION_SUPPORTED+++[] | boolean | `true` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-compression-level"></a> [`quarkus.websockets-next.server.compression-level`](#quarkus-websockets-next_quarkus-websockets-next-server-compression-level) config_property_copy_button:+++quarkus.websockets-next.server.compression-level+++[]<br><br>The compression level must be a value between 0 and 9. The default value is `io.vertx.core.http.HttpServerOptions#DEFAULT_WEBSOCKET_COMPRESSION_LEVEL`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_COMPRESSION_LEVEL+++[] | int |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-max-message-size"></a> [`quarkus.websockets-next.server.max-message-size`](#quarkus-websockets-next_quarkus-websockets-next-server-max-message-size) config_property_copy_button:+++quarkus.websockets-next.server.max-message-size+++[]<br><br>The maximum size of a message in bytes. The default values is `io.vertx.core.http.HttpServerOptions#DEFAULT_MAX_WEBSOCKET_MESSAGE_SIZE`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_MAX_MESSAGE_SIZE+++[] | int |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-max-frame-size"></a> [`quarkus.websockets-next.server.max-frame-size`](#quarkus-websockets-next_quarkus-websockets-next-server-max-frame-size) config_property_copy_button:+++quarkus.websockets-next.server.max-frame-size+++[]<br><br>The maximum size of a frame in bytes. The default values is `io.vertx.core.http.HttpServerOptions#DEFAULT_MAX_WEBSOCKET_FRAME_SIZE`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_MAX_FRAME_SIZE+++[] | int |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-max-pending-messages"></a> [`quarkus.websockets-next.server.max-pending-messages`](#quarkus-websockets-next_quarkus-websockets-next-server-max-pending-messages) config_property_copy_button:+++quarkus.websockets-next.server.max-pending-messages+++[]<br><br>The maximum number of pending (in-flight) messages processed concurrently for a single connection.<br><br>When set to a positive value, the connection applies back-pressure so that no more than this number of messages are received but not yet fully processed, bounding the memory a connection can retain. Set to `0` or a negative value to disable back-pressure completely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_MAX_PENDING_MESSAGES+++[] | int | `256` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-auto-ping-interval"></a> [`quarkus.websockets-next.server.auto-ping-interval`](#quarkus-websockets-next_quarkus-websockets-next-server-auto-ping-interval) config_property_copy_button:+++quarkus.websockets-next.server.auto-ping-interval+++[]<br><br>The interval after which, when set, the server sends a ping message to a connected client automatically.<br><br>Ping messages are not sent automatically by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_AUTO_PING_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-websockets-next_quarkus-websockets-next) |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-unhandled-failure-strategy"></a> [`quarkus.websockets-next.server.unhandled-failure-strategy`](#quarkus-websockets-next_quarkus-websockets-next-server-unhandled-failure-strategy) config_property_copy_button:+++quarkus.websockets-next.server.unhandled-failure-strategy+++[]<br><br>The strategy used when an error occurs but no error handler can handle the failure.<br><br>By default, the error message is logged and the connection is closed when an unhandled failure occurs.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_UNHANDLED_FAILURE_STRATEGY+++[] | tooltip:log-and-close[Log the error message and close the connection.], tooltip:close[Close the connection silently.], tooltip:log[Log the error message.], tooltip:noop[No operation.] | tooltip:log-and-close[Log the error message and close the connection.] |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-security-auth-failure-redirect-url"></a> [`quarkus.websockets-next.server.security.auth-failure-redirect-url`](#quarkus-websockets-next_quarkus-websockets-next-server-security-auth-failure-redirect-url) config_property_copy_button:+++quarkus.websockets-next.server.security.auth-failure-redirect-url+++[]<br><br>Quarkus redirects HTTP handshake request to this URL if an HTTP upgrade is rejected due to the authorization failure. This configuration property takes effect when you secure endpoint with a standard security annotation. For example, the HTTP upgrade is secured if an endpoint class is annotated with the `@RolesAllowed` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_SECURITY_AUTH_FAILURE_REDIRECT_URL+++[] | string |  |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-dev-mode-connection-messages-limit"></a> [`quarkus.websockets-next.server.dev-mode.connection-messages-limit`](#quarkus-websockets-next_quarkus-websockets-next-server-dev-mode-connection-messages-limit) config_property_copy_button:+++quarkus.websockets-next.server.dev-mode.connection-messages-limit+++[]<br><br>The limit of messages kept for a Dev UI connection. If less than zero then no messages are stored and sent to the Dev UI view.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_DEV_MODE_CONNECTION_MESSAGES_LIMIT+++[] | long | `1000` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-traffic-logging-enabled"></a> [`quarkus.websockets-next.server.traffic-logging.enabled`](#quarkus-websockets-next_quarkus-websockets-next-server-traffic-logging-enabled) config_property_copy_button:+++quarkus.websockets-next.server.traffic-logging.enabled+++[]<br><br>If set to true then binary/text messages received/sent are logged if the `DEBUG` level is enabled for the logger `io.quarkus.websockets.next.traffic`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_TRAFFIC_LOGGING_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-traffic-logging-text-payload-limit"></a> [`quarkus.websockets-next.server.traffic-logging.text-payload-limit`](#quarkus-websockets-next_quarkus-websockets-next-server-traffic-logging-text-payload-limit) config_property_copy_button:+++quarkus.websockets-next.server.traffic-logging.text-payload-limit+++[]<br><br>The number of characters of a text message which will be logged if traffic logging is enabled. The payload of a binary message is never logged.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_TRAFFIC_LOGGING_TEXT_PAYLOAD_LIMIT+++[] | int | `100` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-traces-enabled"></a> [`quarkus.websockets-next.server.traces.enabled`](#quarkus-websockets-next_quarkus-websockets-next-server-traces-enabled) config_property_copy_button:+++quarkus.websockets-next.server.traces.enabled+++[]<br><br>If collection of WebSocket traces is enabled. Only applicable when the OpenTelemetry extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_TRACES_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-websockets-next_quarkus-websockets-next-server-metrics-enabled"></a> [`quarkus.websockets-next.server.metrics.enabled`](#quarkus-websockets-next_quarkus-websockets-next-server-metrics-enabled) config_property_copy_button:+++quarkus.websockets-next.server.metrics.enabled+++[]<br><br>If collection of WebSocket metrics is enabled. Only applicable when the Micrometer extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_WEBSOCKETS_NEXT_SERVER_METRICS_ENABLED+++[] | boolean | `false` |

<a id="duration-note-anchor-quarkus-websockets-next_quarkus-websockets-next"></a>

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
