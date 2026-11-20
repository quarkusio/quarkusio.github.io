# Quarkus virtual thread support for gRPC services

This guide explains how to benefit from Java virtual threads when implementing a gRPC service.

> [!TIP]
> This guide focuses on using virtual threads with the gRPC extensions. Please refer to [Writing simpler reactive REST services with Quarkus Virtual Thread support](/guides/virtual-threads/index.md) to read more about Java virtual threads in general and the Quarkus Virtual Thread support.

By default, the Quarkus gRPC extension invokes service methods on an event-loop thread. See the [Quarkus Reactive Architecture documentation](/guides/quarkus-reactive-architecture/index.md) for further details on this topic. But, you can also use the [@Blocking](https://javadoc.io/doc/io.smallrye.reactive/smallrye-reactive-messaging-api/latest/io/smallrye/reactive/messaging/annotations/Blocking.html) annotation to indicate that the service is *blocking* and should be run on a worker thread.

The idea behind Quarkus Virtual Thread support for gRPC services is to offload the service method invocation on virtual threads, instead of running it on an event-loop thread or a worker thread.

To enable virtual thread support on a service method, simply add the [@RunOnVirtualThread](https://javadoc.io/doc/io.smallrye.common/smallrye-common-annotation/latest/io/smallrye/common/annotation/RunOnVirtualThread.html) annotation to the method. If the JDK is compatible (Java 19 or later versions - we recommend 21+) then the invocation will be offloaded to a new virtual thread. It will then be possible to perform blocking operations without blocking the platform thread upon which the virtual thread is mounted.

## Configuring gRPC services to use virtual threads

Let's see an example of how to implement a gRPC service using virtual threads. First, make sure to have the gRPC extension dependency in your build file:

**pom.xml**

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-grpc</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-grpc")
```

You also need to make sure that you are using Java 19 or later (we recommend 21+), this can be enforced in your `pom.xml` file with the following:

**pom.xml**

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

Run your application with:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

or to use the Quarkus Dev mode, insert the following to the `quarkus-maven-plugin` configuration:

**pom.xml**

```xml
<plugin>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <version>${quarkus.version}</version>
    <configuration>
      <source>21</source>
      <target>21</target>
    </configuration>
</plugin>
```

Then you can start using the annotation `@RunOnVirtualThread` in your service implementation:

```java
package io.quarkus.grpc.example.streaming;

import com.google.protobuf.ByteString;
import com.google.protobuf.EmptyProtos;

import io.grpc.testing.integration.Messages;
import io.grpc.testing.integration.TestService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class TestServiceImpl implements TestService {

    @RunOnVirtualThread
    @Override
    public Uni<EmptyProtos.Empty> emptyCall(EmptyProtos.Empty request) {
        return Uni.createFrom().item(EmptyProtos.Empty.newBuilder().build());
    }

    @RunOnVirtualThread
    @Override
    public Uni<Messages.SimpleResponse> unaryCall(Messages.SimpleRequest request) {
        var value = request.getPayload().getBody().toStringUtf8();
        var resp = Messages.SimpleResponse.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(value.toUpperCase())).build())
                .build();
        return Uni.createFrom().item(resp);
    }

    @Override
    @RunOnVirtualThread
    public Multi<Messages.StreamingOutputCallResponse> streamingOutputCall(Messages.StreamingOutputCallRequest request) {
        var value = request.getPayload().getBody().toStringUtf8();
        return Multi.createFrom().<String> emitter(emitter -> {
            emitter.emit(value.toUpperCase());
            emitter.emit(value.toUpperCase());
            emitter.emit(value.toUpperCase());
            emitter.complete();
        }).map(v -> Messages.StreamingOutputCallResponse.newBuilder()
                .setPayload(Messages.Payload.newBuilder().setBody(ByteString.copyFromUtf8(v)).build())
                .build());
    }
}

```

> [!IMPORTANT]
> **Limitations**
>
> The gRPC methods receiving *streams*, such as a `Multi` cannot use `@RunOnVirtualThread`, as the method must not be blocking and produce its result (`Multi` or `Uni`) immediately.
