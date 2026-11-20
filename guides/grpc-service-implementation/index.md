# Implementing a gRPC service

gRPC service implementations exposed as CDI beans are automatically registered and served by quarkus-grpc.

> [!IMPORTANT]
> Implementing a gRPC service requires the gRPC classes to be generated. Place your `proto` files in `src/main/proto` and run `mvn compile`.

## Generated Code

Quarkus generates a few implementation classes for services declared in the `proto` file:

1. A *service interface* using the Mutiny API - the class name is `${JAVA_PACKAGE}.${NAME_OF_THE_SERVICE}`
2. An *implementation base* class using the gRPC API - the class name is structured as follows: `${JAVA_PACKAGE}.${NAME_OF_THE_SERVICE}Grpc.${NAME_OF_THE_SERVICE}ImplBase`

For example, if you use the following `proto` file snippet:

```
option java_package = "hello"; <1>

service Greeter { <2>
    rpc SayHello (HelloRequest) returns (HelloReply) {}
}
```

1. `hello` is the java package for the generated classes.
2. `Greeter` is the service name.

Then the service interface is `hello.Greeter` and the implementation base is the abstract static nested class: `hello.GreeterGrpc.GreeterImplBase`.

> [!IMPORTANT]
> You'll need to implement the *service interface* or extend the *base class* with your service implementation bean as described in the following sections.

## Implementing a Service with the Mutiny API

To implement a gRPC service using the Mutiny API, create a class that implements the service interface. Then, implement the methods defined in the service interface. If you don't want to implement a service method just throw a `java.lang.UnsupportedOperationException` from the method body (the exception will be automatically converted to the appropriate gRPC exception). Finally, implement the service and add the `@GrpcService` annotation:

```java
import io.quarkus.grpc.GrpcService;
import hello.Greeter;

@GrpcService <1>
public class HelloService implements Greeter { <2>

    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(() ->
                HelloReply.newBuilder().setMessage("Hello " + request.getName()).build()
        );
    }
}
```

1. A gRPC service implementation bean must be annotated with the `@GrpcService` annotation and should not declare any other CDI qualifier. All gRPC services have the `jakarta.inject.Singleton` scope. Additionally, the request context is always active during a service call.
2. `hello.Greeter` is the generated service interface.

> [!NOTE]
> The service implementation bean can also extend the Mutiny implementation base, where the class name is structured as follows: `Mutiny${NAME_OF_THE_SERVICE}Grpc.${NAME_OF_THE_SERVICE}ImplBase`.

## Implementing a Service with the default gRPC API

To implement a gRPC service using the default gRPC API, create a class that extends the default implementation base. Then, override the methods defined in the service interface. Finally, implement the service and add the `@GrpcService` annotation:

```java
import io.quarkus.grpc.GrpcService;

@GrpcService
public class HelloService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        String message = "Hello " + name;
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}
```

## Blocking Service Implementation

By default, all the methods from a gRPC service run on the event loop. As a consequence, you must **not** block. If your service logic must block, annotate the method with `io.smallrye.common.annotation.Blocking`:

```java
@Override
@Blocking
public Uni<HelloReply> sayHelloBlocking(HelloRequest request) {
    // Do something blocking before returning the Uni
}
```

## Handling Streams

gRPC allows receiving and returning streams:

```
service Streaming {
    rpc Source(Empty) returns (stream Item) {} // Returns a stream
    rpc Sink(stream Item) returns (Empty) {}   // Reads a stream
    rpc Pipe(stream Item) returns (stream Item) {}  // Reads a streams and return a streams
}
```

Using Mutiny, you can implement these as follows:

```java
import io.quarkus.grpc.GrpcService;

@GrpcService
public class StreamingService implements Streaming {

    @Override
    public Multi<Item> source(Empty request) {
        // Just returns a stream emitting an item every 2ms and stopping after 10 items.
        return Multi.createFrom().ticks().every(Duration.ofMillis(2))
                .select().first(10)
                .map(l -> Item.newBuilder().setValue(Long.toString(l)).build());
    }

    @Override
    public Uni<Empty> sink(Multi<Item> request) {
        // Reads the incoming streams, consume all the items.
        return request
                .map(Item::getValue)
                .map(Long::parseLong)
                .collect().last()
                .map(l -> Empty.newBuilder().build());
    }

    @Override
    public Multi<Item> pipe(Multi<Item> request) {
        // Reads the incoming stream, compute a sum and return the cumulative results
        // in the outbound stream.
        return request
                .map(Item::getValue)
                .map(Long::parseLong)
                .onItem().scan(() -> 0L, Long::sum)
                .onItem().transform(l -> Item.newBuilder().setValue(Long.toString(l)).build());
    }
}
```

<a id="health"></a>
## Health Check

For the implemented services, Quarkus gRPC exposes health information in the following format:

```protobuf
syntax = "proto3";

package grpc.health.v1;

message HealthCheckRequest {
  string service = 1;
}

message HealthCheckResponse {
  enum ServingStatus {
    UNKNOWN = 0;
    SERVING = 1;
    NOT_SERVING = 2;
  }
  ServingStatus status = 1;
}

service Health {
  rpc Check(HealthCheckRequest) returns (HealthCheckResponse);

  rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
}
```

Clients can specify the fully qualified service name to get the health status of a specific service or skip specifying the service name to get the general status of the gRPC server.

For more details, check out the [gRPC documentation](https://github.com/grpc/grpc/blob/v1.28.1/doc/health-checking.md)

Additionally, if Quarkus SmallRye Health is added to the application, a readiness check for the state of the gRPC services will be added to the MicroProfile Health endpoint response, that is `/q/health`.

## Reflection Service

Quarkus gRPC Server implements the [reflection service](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md). This service allows tools like [grpcurl](https://github.com/fullstorydev/grpcurl) or [grpcox](https://github.com/gusaul/grpcox) to interact with your services.

The reflection service is enabled by default in *dev* mode. In test or production mode, you need to enable it explicitly by setting `quarkus.grpc.server.enable-reflection-service` to `true`.

> [!NOTE]
> Quarkus exposes both the reflection service `v1` and `v1alpha`.

## Scaling

By default, quarkus-grpc starts a single gRPC server running on a single event loop.

If you wish to scale your server, you can set the number of server instances by setting `quarkus.grpc.server.instances`.

## Server Configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-use-separate-server"></a> [`quarkus.grpc.server.use-separate-server`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-use-separate-server) config_property_copy_button:+++quarkus.grpc.server.use-separate-server+++[]<br><br>Do we use separate HTTP server to serve gRPC requests. Set this to false if you want to use new Vert.x gRPC support, which uses existing Vert.x HTTP server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_USE_SEPARATE_SERVER+++[] | boolean | `true` |
| <a id="quarkus-grpc_quarkus-grpc-server_section_quarkus-grpc-server-xds"></a> [Configure XDS usage, if enabled](#quarkus-grpc_quarkus-grpc-server_section_quarkus-grpc-server-xds) | Type | Default |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-xds-enabled"></a> [`quarkus.grpc.server.xds.enabled`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-xds-enabled) config_property_copy_button:+++quarkus.grpc.server.xds.enabled+++[]<br><br>Explicitly enable use of XDS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_XDS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-xds-secure"></a> [`quarkus.grpc.server.xds.secure`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-xds-secure) config_property_copy_button:+++quarkus.grpc.server.xds.secure+++[]<br><br>Use secure credentials.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_XDS_SECURE+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-in-process-enabled"></a> [`quarkus.grpc.server.in-process.enabled`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-in-process-enabled) config_property_copy_button:+++quarkus.grpc.server.in-process.enabled+++[]<br><br>Explicitly enable use of in-process.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_IN_PROCESS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-in-process-name"></a> [`quarkus.grpc.server.in-process.name`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-in-process-name) config_property_copy_button:+++quarkus.grpc.server.in-process.name+++[]<br><br>Set in-process name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_IN_PROCESS_NAME+++[] | string | `quarkus-grpc` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-port"></a> [`quarkus.grpc.server.port`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-port) config_property_copy_button:+++quarkus.grpc.server.port+++[]<br><br>The gRPC Server port.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_PORT+++[] | int | `9000` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-test-port"></a> [`quarkus.grpc.server.test-port`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-test-port) config_property_copy_button:+++quarkus.grpc.server.test-port+++[]<br><br>The gRPC Server port used for tests.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_TEST_PORT+++[] | int | `9001` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-host"></a> [`quarkus.grpc.server.host`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-host) config_property_copy_button:+++quarkus.grpc.server.host+++[]<br><br>The gRPC server host.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_HOST+++[] | string | `0.0.0.0` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-handshake-timeout"></a> [`quarkus.grpc.server.handshake-timeout`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-handshake-timeout) config_property_copy_button:+++quarkus.grpc.server.handshake-timeout+++[]<br><br>The gRPC handshake timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_HANDSHAKE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-grpc_quarkus-grpc-server) |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-max-inbound-message-size"></a> [`quarkus.grpc.server.max-inbound-message-size`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-max-inbound-message-size) config_property_copy_button:+++quarkus.grpc.server.max-inbound-message-size+++[]<br><br>The max inbound message size in bytes.<br><br>When using a single server (using `quarkus.grpc.server.use-separate-server=false`), the default value is 256KB. When using a separate server (using `quarkus.grpc.server.use-separate-server=true`), the default value is 4MB.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_MAX_INBOUND_MESSAGE_SIZE+++[] | int |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-max-inbound-metadata-size"></a> [`quarkus.grpc.server.max-inbound-metadata-size`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-max-inbound-metadata-size) config_property_copy_button:+++quarkus.grpc.server.max-inbound-metadata-size+++[]<br><br>The max inbound metadata size in bytes<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_MAX_INBOUND_METADATA_SIZE+++[] | int |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-certificate"></a> [`quarkus.grpc.server.ssl.certificate`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-certificate) config_property_copy_button:+++quarkus.grpc.server.ssl.certificate+++[]<br><br>The classpath path or file path to a server certificate or certificate chain in PEM format.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_CERTIFICATE+++[] | path |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key"></a> [`quarkus.grpc.server.ssl.key`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key) config_property_copy_button:+++quarkus.grpc.server.ssl.key+++[]<br><br>The classpath path or file path to the corresponding certificate private key file in PEM format.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_KEY+++[] | path |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store"></a> [`quarkus.grpc.server.ssl.key-store`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store) config_property_copy_button:+++quarkus.grpc.server.ssl.key-store+++[]<br><br>An optional keystore that holds the certificate information instead of specifying separate files. The keystore can be either on classpath or an external file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_KEY_STORE+++[] | path |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-type"></a> [`quarkus.grpc.server.ssl.key-store-type`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-type) config_property_copy_button:+++quarkus.grpc.server.ssl.key-store-type+++[]<br><br>An optional parameter to specify the type of the keystore file. If not given, the type is automatically detected based on the file name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_KEY_STORE_TYPE+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-password"></a> [`quarkus.grpc.server.ssl.key-store-password`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-password) config_property_copy_button:+++quarkus.grpc.server.ssl.key-store-password+++[]<br><br>A parameter to specify the password of the keystore file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-alias"></a> [`quarkus.grpc.server.ssl.key-store-alias`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-alias) config_property_copy_button:+++quarkus.grpc.server.ssl.key-store-alias+++[]<br><br>A parameter to specify the alias of the keystore file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_KEY_STORE_ALIAS+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-alias-password"></a> [`quarkus.grpc.server.ssl.key-store-alias-password`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-key-store-alias-password) config_property_copy_button:+++quarkus.grpc.server.ssl.key-store-alias-password+++[]<br><br>A parameter to specify the alias password of the keystore file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_KEY_STORE_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-trust-store"></a> [`quarkus.grpc.server.ssl.trust-store`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-trust-store) config_property_copy_button:+++quarkus.grpc.server.ssl.trust-store+++[]<br><br>An optional trust store which holds the certificate information of the certificates to trust<br><br>The trust store can be either on classpath or an external file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_TRUST_STORE+++[] | path |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-trust-store-type"></a> [`quarkus.grpc.server.ssl.trust-store-type`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-trust-store-type) config_property_copy_button:+++quarkus.grpc.server.ssl.trust-store-type+++[]<br><br>An optional parameter to specify type of the trust store file. If not given, the type is automatically detected based on the file name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_TRUST_STORE_TYPE+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-trust-store-password"></a> [`quarkus.grpc.server.ssl.trust-store-password`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-trust-store-password) config_property_copy_button:+++quarkus.grpc.server.ssl.trust-store-password+++[]<br><br>A parameter to specify the password of the trust store file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-cipher-suites"></a> [`quarkus.grpc.server.ssl.cipher-suites`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-cipher-suites) config_property_copy_button:+++quarkus.grpc.server.ssl.cipher-suites+++[]<br><br>The cipher suites to use. If none is given, a reasonable default is selected.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_CIPHER_SUITES+++[] | list of string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-protocols"></a> [`quarkus.grpc.server.ssl.protocols`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-protocols) config_property_copy_button:+++quarkus.grpc.server.ssl.protocols+++[]<br><br>Sets the ordered list of enabled SSL/TLS protocols.<br><br>If not set, it defaults to `"TLSv1.3, TLSv1.2"`. The following list of protocols are supported: `TLSv1, TLSv1.1, TLSv1.2, TLSv1.3`. To only enable `TLSv1.3`, set the value to `to "TLSv1.3"`.<br><br>Note that setting an empty list, and enabling SSL/TLS is invalid. You must at least have one protocol.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_PROTOCOLS+++[] | list of string | `TLSv1.3`, `TLSv1.2` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-client-auth"></a> [`quarkus.grpc.server.ssl.client-auth`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-ssl-client-auth) config_property_copy_button:+++quarkus.grpc.server.ssl.client-auth+++[]<br><br>Configures the engine to require/request client authentication. NONE, REQUEST, REQUIRED<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_SSL_CLIENT_AUTH+++[] | `none`, `request`, `required` | `none` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-plain-text"></a> [`quarkus.grpc.server.plain-text`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-plain-text) config_property_copy_button:+++quarkus.grpc.server.plain-text+++[]<br><br>Disables SSL, and uses plain text instead. If disabled, configure the ssl configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_PLAIN_TEXT+++[] | boolean | `true` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-alpn"></a> [`quarkus.grpc.server.alpn`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-alpn) config_property_copy_button:+++quarkus.grpc.server.alpn+++[]<br><br>Whether ALPN should be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_ALPN+++[] | boolean | `true` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-transport-security-certificate"></a> [`quarkus.grpc.server.transport-security.certificate`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-transport-security-certificate) config_property_copy_button:+++quarkus.grpc.server.transport-security.certificate+++[]<br><br>The path to the certificate file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_TRANSPORT_SECURITY_CERTIFICATE+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-transport-security-key"></a> [`quarkus.grpc.server.transport-security.key`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-transport-security-key) config_property_copy_button:+++quarkus.grpc.server.transport-security.key+++[]<br><br>The path to the private key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_TRANSPORT_SECURITY_KEY+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-enable-reflection-service"></a> [`quarkus.grpc.server.enable-reflection-service`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-enable-reflection-service) config_property_copy_button:+++quarkus.grpc.server.enable-reflection-service+++[]<br><br>Enables the gRPC Reflection Service. By default, the reflection service is only exposed in `dev` mode. This setting allows overriding this choice and enable the reflection service every time.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_ENABLE_REFLECTION_SERVICE+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-instances"></a> [`quarkus.grpc.server.instances`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-instances) config_property_copy_button:+++quarkus.grpc.server.instances+++[]<br><br>Number of gRPC server verticle instances. This is useful for scaling easily across multiple cores. The number should not exceed the amount of event loops.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_INSTANCES+++[] | int | `1` |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-netty-keep-alive-time"></a> [`quarkus.grpc.server.netty.keep-alive-time`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-netty-keep-alive-time) config_property_copy_button:+++quarkus.grpc.server.netty.keep-alive-time+++[]<br><br>Sets a custom keep-alive duration. This configures the time before sending a `keepalive` ping when there is no read activity.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_NETTY_KEEP_ALIVE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-grpc_quarkus-grpc-server) |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-netty-permit-keep-alive-time"></a> [`quarkus.grpc.server.netty.permit-keep-alive-time`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-netty-permit-keep-alive-time) config_property_copy_button:+++quarkus.grpc.server.netty.permit-keep-alive-time+++[]<br><br>Sets a custom permit-keep-alive duration. This configures the most aggressive keep-alive time clients are permitted to configure. The server will try to detect clients exceeding this rate and when detected will forcefully close the connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_NETTY_PERMIT_KEEP_ALIVE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-grpc_quarkus-grpc-server) |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-netty-permit-keep-alive-without-calls"></a> [`quarkus.grpc.server.netty.permit-keep-alive-without-calls`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-netty-permit-keep-alive-without-calls) config_property_copy_button:+++quarkus.grpc.server.netty.permit-keep-alive-without-calls+++[]<br><br>Sets whether to allow clients to send keep-alive HTTP/2 PINGs even if there are no outstanding RPCs on the connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_NETTY_PERMIT_KEEP_ALIVE_WITHOUT_CALLS+++[] | boolean |  |
| <a id="quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-compression"></a> [`quarkus.grpc.server.compression`](#quarkus-grpc_quarkus-grpc-server_quarkus-grpc-server-compression) config_property_copy_button:+++quarkus.grpc.server.compression+++[]<br><br>gRPC compression, e.g. "gzip"<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_COMPRESSION+++[] | string |  |

<a id="duration-note-anchor-quarkus-grpc_quarkus-grpc-server"></a>

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

> [!IMPORTANT]
> When you disable `quarkus.grpc.server.use-separate-server`, you are then using the new Vert.x gRPC server implementation which uses the existing HTTP server. Which means that the server port is now `8080` (or the port configured with `quarkus.http.port`). Also, most of the other configuration properties are no longer applied, since it's the HTTP server that should already be properly configured.

> [!IMPORTANT]
> When you enable `quarkus.grpc.server.xds.enabled`, it's the xDS that should handle most of the configuration above.

## Example of Configuration

### Enabling TLS

To enable TLS, use the following configuration.

Note that all paths in the configuration may either specify a resource on the classpath (typically from `src/main/resources` or its subfolder) or an external file.

```properties
quarkus.grpc.server.ssl.certificate=tls/server.pem
quarkus.grpc.server.ssl.key=tls/server.key
```

> [!NOTE]
> When SSL/TLS is configured, `plain-text` is automatically disabled.

<a id="tls-with-mutual-auth"></a>
### TLS with Mutual Auth

To use TLS with mutual authentication, use the following configuration:

```properties
quarkus.grpc.server.ssl.certificate=tls/server.pem
quarkus.grpc.server.ssl.key=tls/server.key
quarkus.grpc.server.ssl.trust-store=tls/ca.jks
quarkus.grpc.server.ssl.trust-store-password=*****
quarkus.grpc.server.ssl.client-auth=REQUIRED
```

### Custom server building

When Quarkus builds a gRPC server instance, users can apply their own Server(Builder) customizers. The customizers are applied by `priority`, the higher the number the later customizer is applied. The customizers are applied before Quarkus applies user's server configuration; e.g. ideal for some initial defaults.

There are two `customize` methods, the first one uses gRPC's `ServerBuilder` as a parameter - to be used with Quarkus' legacy gRPC support, where the other uses `GrpcServerOptions` - to be used with the new Vert.x gRPC support. User should implement the right `customize` method per gRPC support type usage, or both if the customizer is gRPC type neutral.

```java
public interface ServerBuilderCustomizer<T extends ServerBuilder<T>> {

    /**
     * Customize a ServerBuilder instance.
     *
     * @param config server's configuration
     * @param builder Server builder instance
     */
    default void customize(GrpcServerConfiguration config, T builder) {
    }

    /**
     * Customize a GrpcServerOptions instance.
     *
     * @param config server's configuration
     * @param options GrpcServerOptions instance
     */
    default void customize(GrpcServerConfiguration config, GrpcServerOptions options) {
    }

    /**
     * Priority by which the customizers are applied.
     * Higher priority is applied later.
     *
     * @return the priority
     */
    default int priority() {
        return 0;
    }
}
```

## Server Interceptors

gRPC server interceptors let you perform logic, such as authentication, before your service is invoked.

You can implement a gRPC server interceptor by creating an `@ApplicationScoped` bean implementing `io.grpc.ServerInterceptor`:

```java
@ApplicationScoped
// add @GlobalInterceptor for interceptors meant to be invoked for every service
public class MyInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        // ...
    }
}
```

It's also possible to annotate a producer method as a global interceptor:

```java
import io.quarkus.grpc.GlobalInterceptor;

import jakarta.enterprise.inject.Produces;

public class MyProducer {
    @GlobalInterceptor
    @Produces
    public MyInterceptor myInterceptor() {
        return new MyInterceptor();
    }
}
```

> [!TIP]
> Check the [ServerInterceptor JavaDoc](https://grpc.github.io/grpc-java/javadoc/io/grpc/ServerInterceptor.html) to properly implement your interceptor.

To apply an interceptor to all exposed services, annotate it with `@io.quarkus.grpc.GlobalInterceptor`. To apply an interceptor to a single service, register it on the service with `@io.quarkus.grpc.RegisterInterceptor`:

```java
import io.quarkus.grpc.GrpcService;
import io.quarkus.grpc.RegisterInterceptor;

@GrpcService
@RegisterInterceptor(MyInterceptor.class)
public class StreamingService implements Streaming {
    // ...
}
```

When you have multiple server interceptors, you can order them by implementing the `jakarta.enterprise.inject.spi.Prioritized` interface. Please note that all the global interceptors are invoked before the service-specific interceptors.

```java
@ApplicationScoped
public class MyInterceptor implements ServerInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
            Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        // ...
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
```

Interceptors with the highest priority are called first. The default priority, used if the interceptor does not implement the `Prioritized` interface, is `0`.

There is also a support to inject Vert.x `RoutingContext` instance into your gRPC service, if / when needed. Quarkus doesn't do that by default, you will need to add `RoutingContextGrpcInterceptor` to your gRPC service.

```java
@GrpcService
@RegisterInterceptor(RoutingContextGrpcInterceptor.class)
public class HelloWorldService extends GreeterGrpc.GreeterImplBase {

    @Inject
    RoutingContext context;

    // ...
}
```

## Testing your services

The easiest way to test a gRPC service is to use a gRPC client as described in [Consuming a gRPC Service](/guides/grpc-service-consumption/index.md).

Please note that in the case of using a client to test an exposed service that does not use TLS, there is no need to provide any configuration. E.g. to test the `HelloService` defined above, one could create the following test:

```java
public class HelloServiceTest implements Greeter {

    @GrpcClient
    Greeter client;

    @Test
    void shouldReturnHello() {
        CompletableFuture<String> message = new CompletableFuture<>();
        client.sayHello(HelloRequest.newBuilder().setName("Quarkus").build())
                .subscribe().with(reply -> message.complete(reply.getMessage()));
        assertThat(message.get(5, TimeUnit.SECONDS)).isEqualTo("Hello Quarkus");
    }
}
```

## Trying out your services manually

In the dev mode, you can try out your gRPC services in the Quarkus Dev UI. Just go to <http://localhost:8080/q/dev-ui> and click on *Services* under the gRPC tile.

Please note that your application needs to expose the "normal" HTTP port for the Dev UI to be accessible. If your application does not expose any HTTP endpoints, you can create a dedicated profile with a dependency on `quarkus-vertx-http`:

```xml
    <profiles>
        <profile>
            <id>development</id>
            <dependencies>
                <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-vertx-http</artifactId>
                </dependency>
            </dependencies>
        </profile>
    </profiles>
```

Having it, you can run the dev mode with: `mvn quarkus:dev -Pdevelopment`.

If you use Gradle, you can simply add a dependency for the `quarkusDev` task:

```groovy
dependencies {
    quarkusDev 'io.quarkus:quarkus-vertx-http'
}
```

## gRPC Server metrics

### Enabling metrics collection

gRPC server metrics are automatically enabled when the application also uses the [`quarkus-micrometer`](/guides/telemetry-micrometer/index.md) extension. Micrometer collects the metrics of all the gRPC services implemented by the application.

As an example, if you export the metrics to Prometheus, you will get:

```text
# HELP grpc_server_responses_sent_messages_total The total number of responses sent
# TYPE grpc_server_responses_sent_messages_total counter
grpc_server_responses_sent_messages_total{method="SayHello",methodType="UNARY",service="helloworld.Greeter",} 6.0
# HELP grpc_server_processing_duration_seconds The total time taken for the server to complete the call
# TYPE grpc_server_processing_duration_seconds summary
grpc_server_processing_duration_seconds_count{method="SayHello",methodType="UNARY",service="helloworld.Greeter",statusCode="OK",} 6.0
grpc_server_processing_duration_seconds_sum{method="SayHello",methodType="UNARY",service="helloworld.Greeter",statusCode="OK",} 0.016216771
# HELP grpc_server_processing_duration_seconds_max The total time taken for the server to complete the call
# TYPE grpc_server_processing_duration_seconds_max gauge
grpc_server_processing_duration_seconds_max{method="SayHello",methodType="UNARY",service="helloworld.Greeter",statusCode="OK",} 0.007985236
# HELP grpc_server_requests_received_messages_total The total number of requests received
# TYPE grpc_server_requests_received_messages_total counter
grpc_server_requests_received_messages_total{method="SayHello",methodType="UNARY",service="helloworld.Greeter",} 6.0
```

The service name, method and type can be found in the *tags*.

### Disabling metrics collection

To disable the gRPC server metrics when `quarkus-micrometer` is used, add the following property to the application configuration:

```properties
quarkus.micrometer.binder.grpc-server.enabled=false
```

### Use virtual threads

To use virtual threads in your gRPC service implementation, check the dedicated [guide](/guides/grpc-virtual-threads/index.md).

## gRPC Server authorization

Quarkus includes built-in security to allow [authorization using annotations](/guides/security-authorize-web-endpoints-reference/index.md#standard-security-annotations) when the Vert.x gRPC support, which uses existing Vert.x HTTP server, is enabled.

### Add the Quarkus Security extension

Security capabilities are provided by the Quarkus Security extension, therefore make sure your `pom.xml` file contains following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-security</artifactId>
</dependency>
```

To add the Quarkus Security extension to an existing Maven project, run the following command from your project base directory:

**CLI**

```bash
quarkus extension add security
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='security'
```

**Gradle**

```bash
./gradlew addExtension --extensions='security'
```

### Overview of supported authentication mechanisms

Some supported authentication mechanisms are built into Quarkus, while others require you to add an extension. The following table maps specific authentication requirements to a supported mechanism that you can use in Quarkus:

**Authentication requirements and mechanisms**

| Authentication requirement | Authentication mechanism |
| --- | --- |
| Username and password | [Basic authentication](#basic-auth-mechanism) |
| Client certificate | [Mutual TLS authentication](#mutual-tls-auth-mechanism) |
| Custom requirements | [Custom authentication](#custom-auth-mechanism) |
| Bearer access token | [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md), [JWT](/guides/security-jwt/index.md), [OAuth2](/guides/security-oauth2/index.md) |

Do not forget to install at least one extension that provides an `IdentityProvider` based on selected authentication requirements. Please refer to the [Basic authentication guide](/guides/security-basic-authentication-howto/index.md) for example how to provide the `IdentityProvider` based on username and password.

> [!TIP]
> If you use a separate HTTP server to serve gRPC requests, [Custom authentication](#custom-auth-mechanism) is your only option. Set the `quarkus.grpc.server.use-separate-server` configuration property to `false` so that you can use other mechanisms.

### Secure gRPC service

The gRPC services can be secured with the [standard security annotations](/guides/security-authorize-web-endpoints-reference/index.md#standard-security-annotations) like in the example below:

```java
package org.acme.grpc.auth;

import hello.Greeter;
import io.quarkus.grpc.GrpcService;
import jakarta.annotation.security.RolesAllowed;

@GrpcService
public class HelloService implements Greeter {

    @RolesAllowed("admin")
    @Override
    public Uni<HelloReply> sayHello(HelloRequest request) {
        return Uni.createFrom().item(() ->
                HelloReply.newBuilder().setMessage("Hello " + request.getName()).build()
        );
    }
}
```

Most of the examples of the supported mechanisms send authentication headers, please refer to the [gRPC Headers](/guides/grpc-service-consumption/index.md) section of the Consuming a gRPC Service guide for more information about the gRPC headers.

<a id="basic-auth-mechanism"></a>
### Basic authentication

Quarkus Security provides built-in authentication support for the [Basic authentication](/guides/security-basic-authentication/index.md).

```properties
quarkus.grpc.server.use-separate-server=false
quarkus.http.auth.basic=true <1>
```

1. Enable the Basic authentication.

```java
package org.acme.grpc.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.acme.proto.Greeter;
import org.acme.proto.HelloRequest;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class GreeterServiceTest {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @GrpcClient
    Greeter greeterClient;

    @Test
    void shouldReturnHello() throws ExecutionException, InterruptedException, TimeoutException {
        Metadata headers = new Metadata();

        // Set the headers - Basic auth for testing
        headers.put(AUTHORIZATION, "Basic YWxpY2U6YWxpY2U="); // alice:alice with "admin" role
        var client = GrpcClientUtils.attachHeaders(greeterClient, headers);

        // Call the client
        CompletableFuture<String> message = new CompletableFuture<>();
        client.sayHello(HelloRequest.newBuilder().setName("Quarkus").build())
                .subscribe().with(reply -> message.complete(reply.getMessage()));

        // Get the values
        String theValue = message.get(5, TimeUnit.SECONDS);

        // Assert
        assertThat(theValue, is("Hello Quarkus"));
    }
}
```

<a id="mutual-tls-auth-mechanism"></a>
### Mutual TLS authentication

Quarkus provides mutual TLS (mTLS) authentication so that you can authenticate users based on their X.509 certificates. The simplest way to enforce authentication for all your gRPC services is described in the [TLS with Mutual Auth](#tls-with-mutual-auth) section of this guide. However, the Quarkus Security supports role mapping that you can use to perform even more fine-grained access control.

```properties
quarkus.grpc.server.use-separate-server=false
quarkus.http.insecure-requests=disabled
quarkus.http.ssl.certificate.files=tls/server.pem
quarkus.http.ssl.certificate.key-files=tls/server.key
quarkus.http.ssl.certificate.trust-store-file=tls/ca.jks
quarkus.http.ssl.certificate.trust-store-password=**********
quarkus.http.ssl.client-auth=required
quarkus.http.auth.certificate-role-properties=role-mappings.txt <1>
quarkus.native.additional-build-args=-H:IncludeResources=.*\\.txt
```

1. Adds certificate role mapping.

**Example of the role mapping file**

```properties
testclient=admin <1>
```

1. Map the `testclient` certificate CN (Common Name) to the `SecurityIdentity` role `admin`.

<a id="custom-auth-mechanism"></a>
### Custom authentication

You can always implement one or more `GrpcSecurityMechanism` beans if above-mentioned mechanisms provided by Quarkus do no meet your needs.

**Example of custom `GrpcSecurityMechanism`**

```java
package org.acme.grpc.auth;

import jakarta.inject.Singleton;

import io.grpc.Metadata;
import io.quarkus.security.credential.PasswordCredential;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;

@Singleton
public class CustomGrpcSecurityMechanism implements GrpcSecurityMechanism {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public boolean handles(Metadata metadata) {
        String authString = metadata.get(AUTHORIZATION);
        return authString != null && authString.startsWith("Custom ");
    }

    @Override
    public AuthenticationRequest createAuthenticationRequest(Metadata metadata) {
        final String authString = metadata.get(AUTHORIZATION);
        final String userName;
        final String password;
        // here comes your application logic that transforms 'authString' to user name and password
        return new UsernamePasswordAuthenticationRequest(userName, new PasswordCredential(password));
    }
}
```
