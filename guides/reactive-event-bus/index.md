# Using the event bus

Quarkus allows different beans to interact using asynchronous events, thus promoting loose-coupling. The messages are sent to *virtual addresses*. It offers 3 types of delivery mechanism:

- point-to-point - send the message, one consumer receives it. If several consumers listen to the address, a round-robin is applied;
- publish/subscribe - publish a message, all the consumers listening to the address are receiving the message;
- request/reply - send the message and expect a response. The receiver can respond to the message in an asynchronous-fashion

All these delivery mechanisms are non-blocking, and are providing one of the fundamental brick to build reactive applications.

> [!NOTE]
> The asynchronous message passing feature allows replying to messages which is not supported by Reactive Messaging. However, it is limited to single-event behavior (no stream) and to local messages.

## Installing

This mechanism uses the Vert.x EventBus, so you need to enable the `vertx` extension to use this feature. If you are creating a new project, set the `extensions` parameter as follows:

**CLI**

```bash
quarkus create app org.acme:vertx-quickstart \
    --extension='vertx,rest' \
    --no-code
cd vertx-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=vertx-quickstart \
    -Dextensions='vertx,rest' \
    -DnoCode
cd vertx-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=vertx-quickstart"`

If you have an already created project, the `vertx` extension can be added to an existing Quarkus project with the `add-extension` command:

**CLI**

```bash
quarkus extension add vertx
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='vertx'
```

**Gradle**

```bash
./gradlew addExtension --extensions='vertx'
```

Otherwise, you can manually add this to the dependencies section of your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-vertx</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-vertx")
```

## Consuming events

To consume events, use the `io.quarkus.vertx.ConsumeEvent` annotation:

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

1. If not set, the address is the fully qualified name of the bean, for instance, in this snippet it's `org.acme.vertx.GreetingService`.
2. The method parameter is the message body. If the method returns *something* it's the message response.

> [!IMPORTANT]
> By default, the code consuming the event must be *non-blocking*, as it's called on the Vert.x event loop. If your processing is blocking, use the `blocking` attribute:
>
> ```java
> @ConsumeEvent(value = "blocking-consumer", blocking = true)
> void consumeBlocking(String message) {
>     // Something blocking
> }
> ```
>
> Alternatively, you can annotate your method with `@io.smallrye.common.annotation.Blocking`:
>
> ```java
> @ConsumeEvent(value = "blocking-consumer")
> @Blocking
> void consumeBlocking(String message) {
>     // Something blocking
> }
> ```
>
> When using `@Blocking`, it ignores the value of the `blocking` attribute of `@ConsumeEvent`. See the [Quarkus Reactive Architecture documentation](/guides/quarkus-reactive-architecture/index.md) for further details on this topic.

Asynchronous processing is also possible by returning either an `io.smallrye.mutiny.Uni` or a `java.util.concurrent.CompletionStage`:

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
    public CompletionStage<String> consume(String name) {
        // return a CompletionStage completed when the processing is finished.
        // You can also fail the CompletionStage explicitly
    }

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

### Configuring the address

The `@ConsumeEvent` annotation can be configured to set the address:

```java
@ConsumeEvent("greeting")               // <1>
public String consume(String name) {
    return name.toUpperCase();
}
```

1. Receive the messages sent to the `greeting` address

### Replying

The *return* value of a method annotated with `@ConsumeEvent` is used as response to the incoming message. For instance, in the following snippet, the returned `String` is the response.

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
> ```java
> @Inject ManagedExecutor executor;
> ```
>
> Alternatively, you can use the default Quarkus worker pool using:
>
> ```java
> Executor executor = Infrastructure.getDefaultWorkerPool();
> ```

### Implementing fire and forget interactions

You don't have to reply to received messages. Typically, for a *fire and forget* interaction, the messages are consumed and the sender does not need to know about it. To implement this, your consumer method just returns `void`

```java
@ConsumeEvent("greeting")
public void consume(String event) {
    // Do something with the event
}
```

### Dealing with messages

As said above, this mechanism is based on the Vert.x event bus. So, you can also use `Message` directly:

```java
@ConsumeEvent("greeting")
public void consume(Message<String> msg) {
    System.out.println(msg.address());
    System.out.println(msg.body());
}
```

### Handling Failures

If a method annotated with `@ConsumeEvent` throws an exception then:

- if a reply handler is set then the failure is propagated back to the sender via an `io.vertx.core.eventbus.ReplyException` with code `ConsumeEvent#FAILURE_CODE` and the exception message,
- if no reply handler is set then the exception is rethrown (and wrapped in a `RuntimeException` if necessary) and can be handled by the default exception handler, i.e. `io.vertx.core.Vertx#exceptionHandler()`.

## Sending messages

Ok, we have seen how to receive messages, let's now switch to the *other side*: the sender. Sending and publishing messages use the Vert.x event bus:

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
    EventBus bus;                                       // <1>

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
3. `send` a message and expect reply asynchronously
4. `send` a message and expect reply in a blocking manner

```java
// Case 1
bus.<String>requestAndForget("greeting", name);
// Case 2
bus.publish("greeting", name);
// Case 3
Uni<String> response = bus.<String>request("address", "hello, how are you?")
        .onItem().transform(Message::body);
// Case 4
String response = bus.<String>requestAndAwait("greeting", name).body();
```

## Putting things together - bridging HTTP and messages

Let's revisit a greeting HTTP endpoint and use asynchronous message passing to delegate the call to a separated bean. It uses the request/reply dispatching mechanism. Instead of implementing the business logic inside the Jakarta REST endpoint, we are sending a message. This message is consumed by another bean and the response is sent using the *reply* mechanism.

First create a new project using:

**CLI**

```bash
quarkus create app org.acme:vertx-http-quickstart \
    --extension='vertx,rest' \
    --no-code
cd vertx-http-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=vertx-http-quickstart \
    -Dextensions='vertx,rest' \
    -DnoCode
cd vertx-http-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=vertx-http-quickstart"`

You can already start the application in *dev mode* using:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

Then, creates a new Jakarta REST resource with the following content:

**src/main/java/org/acme/vertx/EventResource.java**

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

If you call this endpoint, you will wait and get a timeout. Indeed, no one is listening. So, we need a consumer listening on the `greeting` address. Create a `GreetingService` bean with the following content:

**src/main/java/org/acme/vertx/GreetingService.java**

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

This bean receives the name, and returns the greeting message.

Now, open your browser to <http://localhost:8080/async/Quarkus>, and you should see:

```text
Hello Quarkus
```

To better understand, let's detail how the HTTP request/response has been handled:

1. The request is received by the `hello` method
2. a message containing the *name* is sent to the event bus
3. Another bean receives this message and computes the response
4. This response is sent back using the reply mechanism
5. Once the reply is received by the sender, the content is written to the HTTP response

This application can be packaged using:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

You can also compile it as a native executable with:

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

## Using codecs

The [Vert.x Event Bus](https://vertx.io/docs/vertx-core/java/#event_bus) uses codecs to *serialize* and *deserialize* objects. Quarkus provides a default codec for local delivery. So you can exchange objects as follows:

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

If you want to use a specific codec, you need to explicitly set it on both ends:

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
