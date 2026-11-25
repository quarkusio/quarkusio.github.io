# Consuming a gRPC service

gRPC clients can be injected in your application code.

> [!IMPORTANT]
> Consuming gRPC services requires the gRPC classes to be generated. Place your `proto` files in `src/main/proto` and run `mvn compile`.

## Stubs and Injection

gRPC generation provides several stubs, providing different ways to consume a gRPC service. You can inject:

- a service interface using the Mutiny API,
- a blocking stub using the gRPC API,
- a reactive stub based on Mutiny,
- the gRPC `io.grpc.Channel`, that lets you create other types of stubs.

```java
import io.quarkus.grpc.GrpcClient;

import hello.Greeter;
import hello.GreeterGrpc.GreeterBlockingStub;
import hello.MutinyGreeterGrpc.MutinyGreeterStub;

class MyBean {

   // A service interface using the Mutiny API
   @GrpcClient("helloService")                   // <1>
   Greeter greeter;

   // A reactive stub based on Mutiny
   @GrpcClient("helloService")
   MutinyGreeterGrpc.MutinyGreeterStub mutiny;

   // A blocking stub using the gRPC API
   @GrpcClient
   GreeterGrpc.GreeterBlockingStub helloService; // <2>

   @GrpcClient("hello-service")
   Channel channel;

}
```

1. A gRPC client injection point must be annotated with the `@GrpcClient` qualifier. This qualifier can be used to specify the name that is used to configure the underlying gRPC client. For example, if you set it to `hello-service`, configuring the host of the service is done using the `quarkus.grpc.clients.**hello-service**.host`.
2. If the name is not specified via the `GrpcClient#value()` then the field name is used instead, e.g. `helloService` in this particular case.

The stub class names are derived from the service name used in your `proto` file. For example, if you use `Greeter` as a service name as in:

```
option java_package = "hello";

service Greeter {
    rpc SayHello (HelloRequest) returns (HelloReply) {}
}
```

Then the service interface name is: `hello.Greeter`, the Mutiny stub name is: `hello.MutinyGreeterGrpc.MutinyGreeterStub` and the blocking stub name is: `hello.GreeterGrpc.GreeterBlockingStub`.

## Examples

### Service Interface

```java
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;

import hello.Greeter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

   @GrpcClient <1>
   Greeter hello;

   @GET
   @Path("/mutiny/{name}")
   public Uni<String> helloMutiny(String name) {
      return hello.sayHello(HelloRequest.newBuilder().setName(name).build())
            .onItem().transform(HelloReply::getMessage);
   }
}
```

1. The service name is derived from the injection point - the field name is used. The `quarkus.grpc.clients.hello.host` property must be set.

### Blocking Stub

```java
import io.quarkus.grpc.GrpcClient;

import hello.GreeterGrpc.GreeterBlockingStub;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

   @GrpcClient("hello") <1>
   GreeterGrpc.GreeterBlockingStub blockingHelloService;

   @GET
   @Path("/blocking/{name}")
   public String helloBlocking(String name) {
      return blockingHelloService.sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage();
   }
}
```

1. The `quarkus.grpc.clients.hello.host` property must be set.

### Handling streams

gRPC allows sending and receiving streams:

```
service Streaming {
    rpc Source(Empty) returns (stream Item) {} // Returns a stream
    rpc Sink(stream Item) returns (Empty) {}   // Reads a stream
    rpc Pipe(stream Item) returns (stream Item) {}  // Reads a streams and return a streams
}
```

Using the Mutiny stub, you can interact with these as follows:

```java
package io.quarkus.grpc.example.streaming;

import io.grpc.examples.streaming.Empty;
import io.grpc.examples.streaming.Item;
import io.grpc.examples.streaming.MutinyStreamingGrpc;
import io.quarkus.grpc.GrpcClient;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/streaming")
@Produces(MediaType.APPLICATION_JSON)
public class StreamingEndpoint {

    @GrpcClient
    MutinyStreamingGrpc.MutinyStreamingStub streaming;

    @GET
    public Multi<String> invokeSource() {
        // Retrieve a stream
        return streaming.source(Empty.newBuilder().build())
                .onItem().transform(Item::getValue);
    }

    @GET
    @Path("sink/{max}")
    public Uni<Void> invokeSink(int max) {
        // Send a stream and wait for completion
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .map(i -> Item.newBuilder().setValue(i).build());
        return streaming.sink(inputs).onItem().ignore().andContinueWithNull();
    }

    @GET
    @Path("/{max}")
    public Multi<String> invokePipe(int max) {
        // Send a stream and retrieve a stream
        Multi<Item> inputs = Multi.createFrom().range(0, max)
                .map(i -> Integer.toString(i))
                .map(i -> Item.newBuilder().setValue(i).build());
        return streaming.pipe(inputs).onItem().transform(Item::getValue);
    }

}

```

## Client configuration

For each gRPC service you inject in your application, you can configure the following attributes:

### Global configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-grpc_quarkus-grpc-client-stork-proactive-connections"></a> [`quarkus.grpc-client.stork-proactive-connections`](#quarkus-grpc_quarkus-grpc-client-stork-proactive-connections) config_property_copy_button:+++quarkus.grpc-client.stork-proactive-connections+++[]<br><br>If set to true, and a Stork load balancer is used, connections with all available service instances will be requested proactively. This means better load balancing at the cost of having multiple active connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENT_STORK_PROACTIVE_CONNECTIONS+++[] | boolean | `true` |

### Per-client configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-threads"></a> [`quarkus.grpc.clients."client-name".stork.threads`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-threads) config_property_copy_button:+++quarkus.grpc.clients."client-name".stork.threads+++[]<br><br>Number of threads on a delayed gRPC ClientCall<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__STORK_THREADS+++[] | int | `10` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-deadline"></a> [`quarkus.grpc.clients."client-name".stork.deadline`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-deadline) config_property_copy_button:+++quarkus.grpc.clients."client-name".stork.deadline+++[]<br><br>Deadline in milliseconds of delayed gRPC call<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__STORK_DEADLINE+++[] | long | `5000` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-retries"></a> [`quarkus.grpc.clients."client-name".stork.retries`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-retries) config_property_copy_button:+++quarkus.grpc.clients."client-name".stork.retries+++[]<br><br>Number of retries on a gRPC ClientCall<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__STORK_RETRIES+++[] | int | `3` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-delay"></a> [`quarkus.grpc.clients."client-name".stork.delay`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-delay) config_property_copy_button:+++quarkus.grpc.clients."client-name".stork.delay+++[]<br><br>Initial delay in seconds on refresh check<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__STORK_DELAY+++[] | long | `60` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-period"></a> [`quarkus.grpc.clients."client-name".stork.period`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-stork-period) config_property_copy_button:+++quarkus.grpc.clients."client-name".stork.period+++[]<br><br>Refresh period in seconds<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__STORK_PERIOD+++[] | long | `120` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-port"></a> [`quarkus.grpc.clients."client-name".port`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-port) config_property_copy_button:+++quarkus.grpc.clients."client-name".port+++[]<br><br>The gRPC service port.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__PORT+++[] | int | `8080` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-test-port"></a> [`quarkus.grpc.clients."client-name".test-port`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-test-port) config_property_copy_button:+++quarkus.grpc.clients."client-name".test-port+++[]<br><br>The gRPC service test port. If not set, uses 8081 for plain text and 8444 when TLS is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TEST_PORT+++[] | int |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-host"></a> [`quarkus.grpc.clients."client-name".host`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-host) config_property_copy_button:+++quarkus.grpc.clients."client-name".host+++[]<br><br>The host name / IP on which the service is exposed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__HOST+++[] | string | `localhost` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-domain-socket"></a> [`quarkus.grpc.clients."client-name".domain-socket`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-domain-socket) config_property_copy_button:+++quarkus.grpc.clients."client-name".domain-socket+++[]<br><br>The path to a Unix domain socket. When set, the client connects to the gRPC server using a Unix domain socket instead of a TCP connection. The `host` and `port` properties are ignored in this case.<br><br>Unix domain sockets are not available on Windows.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__DOMAIN_SOCKET+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-configuration-name"></a> [`quarkus.grpc.clients."client-name".tls-configuration-name`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-configuration-name) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If not set and the default TLS configuration is configured (`quarkus.tls.*`) then that will be used. If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>If no TLS configuration is set, and `quarkus.tls.*` is not configured, then, `quarkus.grpc.clients.$client-name.tls` will be used.<br><br>Important: This is only supported when using the Quarkus (Vert.x-based) gRPC client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-enabled"></a> [`quarkus.grpc.clients."client-name".tls.enabled`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-enabled) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.enabled+++[]<br><br>Whether SSL/TLS is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-all"></a> [`quarkus.grpc.clients."client-name".tls.trust-all`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-all) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.trust-all+++[]<br><br>Enable trusting all certificates. Disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_TRUST_ALL+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-pem-certs"></a> [`quarkus.grpc.clients."client-name".tls.trust-certificate-pem.certs`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-pem-certs) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.trust-certificate-pem.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_TRUST_CERTIFICATE_PEM_CERTS+++[] | list of string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-jks-path"></a> [`quarkus.grpc.clients."client-name".tls.trust-certificate-jks.path`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-jks-path) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.trust-certificate-jks.path+++[]<br><br>Path of the key file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_TRUST_CERTIFICATE_JKS_PATH+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-jks-password"></a> [`quarkus.grpc.clients."client-name".tls.trust-certificate-jks.password`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-jks-password) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.trust-certificate-jks.password+++[]<br><br>Password of the key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_TRUST_CERTIFICATE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-p12-path"></a> [`quarkus.grpc.clients."client-name".tls.trust-certificate-p12.path`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-p12-path) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.trust-certificate-p12.path+++[]<br><br>Path to the key file (PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_TRUST_CERTIFICATE_P12_PATH+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-p12-password"></a> [`quarkus.grpc.clients."client-name".tls.trust-certificate-p12.password`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-trust-certificate-p12-password) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.trust-certificate-p12.password+++[]<br><br>Password of the key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_TRUST_CERTIFICATE_P12_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-pem-keys"></a> [`quarkus.grpc.clients."client-name".tls.key-certificate-pem.keys`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-pem-keys) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.key-certificate-pem.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_KEY_CERTIFICATE_PEM_KEYS+++[] | list of string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-pem-certs"></a> [`quarkus.grpc.clients."client-name".tls.key-certificate-pem.certs`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-pem-certs) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.key-certificate-pem.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_KEY_CERTIFICATE_PEM_CERTS+++[] | list of string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-jks-path"></a> [`quarkus.grpc.clients."client-name".tls.key-certificate-jks.path`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-jks-path) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.key-certificate-jks.path+++[]<br><br>Path of the key file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_KEY_CERTIFICATE_JKS_PATH+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-jks-password"></a> [`quarkus.grpc.clients."client-name".tls.key-certificate-jks.password`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-jks-password) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.key-certificate-jks.password+++[]<br><br>Password of the key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_KEY_CERTIFICATE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-p12-path"></a> [`quarkus.grpc.clients."client-name".tls.key-certificate-p12.path`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-p12-path) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.key-certificate-p12.path+++[]<br><br>Path to the key file (PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_KEY_CERTIFICATE_P12_PATH+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-p12-password"></a> [`quarkus.grpc.clients."client-name".tls.key-certificate-p12.password`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-key-certificate-p12-password) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.key-certificate-p12.password+++[]<br><br>Password of the key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_KEY_CERTIFICATE_P12_PASSWORD+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-verify-hostname"></a> [`quarkus.grpc.clients."client-name".tls.verify-hostname`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-tls-verify-hostname) config_property_copy_button:+++quarkus.grpc.clients."client-name".tls.verify-hostname+++[]<br><br>Whether hostname should be verified in the SSL/TLS handshake.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__TLS_VERIFY_HOSTNAME+++[] | boolean | `true` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-name-resolver"></a> [`quarkus.grpc.clients."client-name".name-resolver`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-name-resolver) config_property_copy_button:+++quarkus.grpc.clients."client-name".name-resolver+++[]<br><br>Use a name resolver. Defaults to dns. If set to "stork", host will be treated as SmallRye Stork service name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__NAME_RESOLVER+++[] | string | `dns` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-plain-text"></a> [`quarkus.grpc.clients."client-name".plain-text`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-plain-text) config_property_copy_button:+++quarkus.grpc.clients."client-name".plain-text+++[]<br><br>Whether `plain-text` should be used instead of `TLS`. Enabled by default, except if TLS/SSL is configured. In this case, `plain-text` is disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__PLAIN_TEXT+++[] | boolean |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-idle-timeout"></a> [`quarkus.grpc.clients."client-name".idle-timeout`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-idle-timeout) config_property_copy_button:+++quarkus.grpc.clients."client-name".idle-timeout+++[]<br><br>The duration without ongoing RPCs before going to idle mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-grpc_quarkus-grpc-clients) |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-keep-alive-timeout"></a> [`quarkus.grpc.clients."client-name".keep-alive-timeout`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-keep-alive-timeout) config_property_copy_button:+++quarkus.grpc.clients."client-name".keep-alive-timeout+++[]<br><br>The amount of time the sender of a keep alive ping waits for an acknowledgement.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__KEEP_ALIVE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-grpc_quarkus-grpc-clients) |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-keep-alive-without-calls"></a> [`quarkus.grpc.clients."client-name".keep-alive-without-calls`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-keep-alive-without-calls) config_property_copy_button:+++quarkus.grpc.clients."client-name".keep-alive-without-calls+++[]<br><br>Whether keep-alive will be performed when there are no outstanding RPC on a connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__KEEP_ALIVE_WITHOUT_CALLS+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-max-inbound-message-size"></a> [`quarkus.grpc.clients."client-name".max-inbound-message-size`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-max-inbound-message-size) config_property_copy_button:+++quarkus.grpc.clients."client-name".max-inbound-message-size+++[]<br><br>The maximum message size allowed for a single gRPC frame (in bytes). Default is 4 MiB.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__MAX_INBOUND_MESSAGE_SIZE+++[] | int |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-compression"></a> [`quarkus.grpc.clients."client-name".compression`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-compression) config_property_copy_button:+++quarkus.grpc.clients."client-name".compression+++[]<br><br>The compression to use for each call. The accepted values are `gzip` and `identity`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__COMPRESSION+++[] | string |  |
| <a id="quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-deadline"></a> [`quarkus.grpc.clients."client-name".deadline`](#quarkus-grpc_quarkus-grpc-clients_quarkus-grpc-clients-client-name-deadline) config_property_copy_button:+++quarkus.grpc.clients."client-name".deadline+++[]<br><br>The deadline used for each call.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__DEADLINE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-grpc_quarkus-grpc-clients) |  |

<a id="duration-note-anchor-quarkus-grpc_quarkus-grpc-clients"></a>

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

The `client-name` is the name set in the `@GrpcClient` or derived from the injection point if not explicitly defined.

The following examples use *hello* as the client name. Don't forget to replace it with the name you used in the `@GrpcClient` annotation.

> [!IMPORTANT]
> When you enable `quarkus.grpc.clients."client-name".xds.enabled`, it's the xDS that should handle most of the configuration above.

### Domain Socket

To connect a gRPC client to a server listening on a Unix domain socket, set the `domain-socket` property to the socket path:

```properties
quarkus.grpc.clients.hello.domain-socket=/var/run/grpc.sock
```

When `domain-socket` is set, the `host` and `port` properties are ignored. The client connects directly to the specified socket file.

Here is a full example with both the server and client configured to use the same domain socket:

```properties
# Server: listen on domain socket only
quarkus.http.domain-socket-enabled=true
quarkus.http.domain-socket=/var/run/grpc.sock
quarkus.http.host-enabled=false

# Client: connect via domain socket
quarkus.grpc.clients.hello.domain-socket=/var/run/grpc.sock
```

> [!IMPORTANT]
> The Stork name resolver is incompatible with domain sockets. Setting both `domain-socket` and `name-resolver=stork` will result in a startup error.

> [!NOTE]
> Unix domain sockets require JDK 16+ and are not available on Windows.

### Custom Channel building

When Quarkus builds a gRPC Channel instance (the way gRPC clients communicate with gRPC services on a lower network level), users can apply their own Channel(Builder) customizers. The customizers are applied by `priority`, the higher the number the later customizer is applied. The customizers are applied before Quarkus applies user's client configuration; e.g. ideal for some initial defaults per all clients.

There are two `customize` methods, the first one uses gRPC's `ManagedChannelBuilder` as a parameter - to be used with Quarkus' legacy gRPC support, where the other uses `GrpcClientOptions` - to be used with the new Vert.x gRPC support. User should implement the right `customize` method per gRPC support type usage, or both if the customizer is gRPC type neutral.

```java
public interface ChannelBuilderCustomizer<T extends ManagedChannelBuilder<T>> {

    /**
     * Customize a ManagedChannelBuilder instance.
     *
     * @param name gRPC client name
     * @param config client's configuration
     * @param builder Channel builder instance
     * @return map of config properties to be used as default service config against the builder
     */
    default Map<String, Object> customize(String name, GrpcClientConfiguration config, T builder) {
        return Map.of();
    }

    /**
     * Customize a GrpcClientOptions instance.
     *
     * @param name gRPC client name
     * @param config client's configuration
     * @param options GrpcClientOptions instance
     */
    default void customize(String name, GrpcClientConfiguration config, GrpcClientOptions options) {
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

### Enabling TLS

To enable TLS, use the following configuration. Note that all paths in the configuration may either specify a resource on the classpath (typically from `src/main/resources` or its subfolder) or an external file.

```properties
quarkus.grpc.clients.hello.host=localhost

# either a path to a classpath resource or to a file:
quarkus.grpc.clients.hello.ssl.trust-store=tls/ca.pem
```

> [!NOTE]
> When SSL/TLS is configured, `plain-text` is automatically disabled.

### TLS with Mutual Auth

To use TLS with mutual authentication, use the following configuration:

```properties
quarkus.grpc.clients.hello.host=localhost
quarkus.grpc.clients.hello.plain-text=false

# all the following may use either a path to a classpath resource or to a file:
quarkus.grpc.clients.hello.ssl.certificate=tls/client.pem
quarkus.grpc.clients.hello.ssl.key=tls/client.key
quarkus.grpc.clients.hello.ssl.trust-store=tls/ca.pem
```

### Client Stub Deadlines

If you need to configure a deadline for a gRPC stub, i.e. to specify a duration of time after which the stub will always return the status error `DEADLINE_EXCEEDED`. You can specify the deadline via the `quarkus.grpc.clients."service-name".deadline` configuration property, e.g.:

```properties
quarkus.grpc.clients.hello.host=localhost
quarkus.grpc.clients.hello.deadline=2s <1>
```

1. Set the  deadline for all injected stubs.

> [!IMPORTANT]
> Do not use this feature to implement an RPC timeout. To implement an RPC timeout, either use Mutiny `call.ifNoItem().after(...)` or Fault Tolerance `@Timeout`.

<a id="grpc-headers"></a>
## gRPC Headers

Similarly to HTTP, alongside the message, gRPC calls can carry headers. Headers can be useful e.g. for authentication.

To set headers for a gRPC call, create a client with headers attached and then perform the call on this client:

```java
import jakarta.enterprise.context.ApplicationScoped;

import examples.Greeter;
import examples.HelloReply;
import examples.HelloRequest;
import io.grpc.Metadata;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcClientUtils;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyService {
    @GrpcClient
    Greeter client;

    public Uni<HelloReply> doTheCall() {
        Metadata extraHeaders = new Metadata();
        if (headers) {
            extraHeaders.put("my-header", "my-interface-value");
        }

        Greeter alteredClient = GrpcClientUtils.attachHeaders(client, extraHeaders); // <1>

        return alteredClient.sayHello(HelloRequest.newBuilder().setName(name).build()); // <2>
    }
}
```

1. Alter the client to make calls with the `extraHeaders` attached
2. Perform the call with the altered client. The original client remains unmodified

`GrpcClientUtils` works with all flavors of clients.

## Client Interceptors

A gRPC client interceptor can be implemented by a CDI bean that also implements the `io.grpc.ClientInterceptor` interface. You can annotate an injected client with `@io.quarkus.grpc.RegisterClientInterceptor` to register the specified interceptor for the particular client instance. The `@RegisterClientInterceptor` annotation is repeatable. Alternatively, if you want to apply the interceptor to any injected client then annotate the interceptor bean with `@io.quarkus.grpc.GlobalInterceptor`.

**Global Client Interceptor Example**

```java
import io.quarkus.grpc.GlobalInterceptor;

import io.grpc.ClientInterceptor;

@GlobalInterceptor <1>
@ApplicationScoped
public class MyInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
       // ...
    }
}
```

1. This interceptor is applied to all injected gRPC clients.

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
> Check the [ClientInterceptor JavaDoc](https://grpc.github.io/grpc-java/javadoc/io/grpc/ClientInterceptor.html) to properly implement your interceptor.

**`@RegisterClientInterceptor` Example**

```java
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.RegisterClientInterceptor;

import hello.Greeter;

@ApplicationScoped
class MyBean {

    @RegisterClientInterceptor(MySpecialInterceptor.class) <1>
    @GrpcClient("helloService")
    Greeter greeter;
}
```

1. Registers the `MySpecialInterceptor` for this particular client.

When you have multiple client interceptors, you can order them by implementing the `jakarta.enterprise.inject.spi.Prioritized` interface:

```java
@ApplicationScoped
public class MyInterceptor implements ClientInterceptor, Prioritized {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
       // ...
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
```

Interceptors with the highest priority are called first. The default priority, used if the interceptor does not implement the `Prioritized` interface, is `0`.

## gRPC Client metrics

### Enabling metrics collection

gRPC client metrics are automatically enabled when the application also uses the [`quarkus-micrometer`](/guides/telemetry-micrometer/index.md) extension. Micrometer collects the metrics of all the gRPC clients used by the application.

As an example, if you export the metrics to Prometheus, you will get:

```text
# HELP grpc_client_responses_received_messages_total The total number of responses received
# TYPE grpc_client_responses_received_messages_total counter
grpc_client_responses_received_messages_total{method="SayHello",methodType="UNARY",service="helloworld.Greeter",} 6.0
# HELP grpc_client_requests_sent_messages_total The total number of requests sent
# TYPE grpc_client_requests_sent_messages_total counter
grpc_client_requests_sent_messages_total{method="SayHello",methodType="UNARY",service="helloworld.Greeter",} 6.0
# HELP grpc_client_processing_duration_seconds The total time taken for the client to complete the call, including network delay
# TYPE grpc_client_processing_duration_seconds summary
grpc_client_processing_duration_seconds_count{method="SayHello",methodType="UNARY",service="helloworld.Greeter",statusCode="OK",} 6.0
grpc_client_processing_duration_seconds_sum{method="SayHello",methodType="UNARY",service="helloworld.Greeter",statusCode="OK",} 0.167411625
# HELP grpc_client_processing_duration_seconds_max The total time taken for the client to complete the call, including network delay
# TYPE grpc_client_processing_duration_seconds_max gauge
grpc_client_processing_duration_seconds_max{method="SayHello",methodType="UNARY",service="helloworld.Greeter",statusCode="OK",} 0.136478028
```

The service name, method and type can be found in the *tags*.

By default, processing duration is exported as a Prometheus summary (`_count`, `_sum`, `_max`). To publish aggregatable histogram buckets (for example for `histogram_quantile`), enable:

```properties
quarkus.micrometer.binder.grpc-client.histogram=true
```

This uses Micrometer's default percentile histogram buckets (about `1ms` to `30s`). Optionally add extra SLO boundaries and/or clamp the published bucket range to reduce cardinality:

```properties
quarkus.micrometer.binder.grpc-client.slos=5ms,10ms,25ms,50ms,100ms,1s
quarkus.micrometer.binder.grpc-client.minimum-expected-value=1ms
quarkus.micrometer.binder.grpc-client.maximum-expected-value=10s
```

Histograms are disabled by default because they increase memory usage and metric cardinality.

### Disabling metrics collection

To disable the gRPC client metrics when `quarkus-micrometer` is used, add the following property to the application configuration:

```properties
quarkus.micrometer.binder.grpc-client.enabled=false
```

## Custom exception handling

If any of the gRPC services or server interceptors throw an (custom) exception, you can add your own [ExceptionHandlerProvider](https://github.com/quarkusio/quarkus/blob/main/extensions/grpc/api/src/main/java/io/quarkus/grpc/ExceptionHandlerProvider.java) as a CDI bean in your application, to provide a custom handling of those exceptions.

e.g.

```java
@ApplicationScoped
public class HelloExceptionHandlerProvider implements ExceptionHandlerProvider {
    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        return new HelloExceptionHandler<>(listener, serverCall, metadata);
    }

    @Override
    public Throwable transform(Throwable t) {
        if (t instanceof HelloException he) {
            return new StatusRuntimeException(Status.ABORTED.withDescription(he.getName()));
        } else {
            return ExceptionHandlerProvider.toStatusException(t, true);
        }
    }

    private static class HelloExceptionHandler<A, B> extends ExceptionHandler<A, B> {
        public HelloExceptionHandler(ServerCall.Listener<A> listener, ServerCall<A, B> call, Metadata metadata) {
            super(listener, call, metadata);
        }

        @Override
        protected void handleException(Throwable t, ServerCall<A, B> call, Metadata metadata) {
            StatusRuntimeException sre = (StatusRuntimeException) ExceptionHandlerProvider.toStatusException(t, true);
            Metadata trailers = sre.getTrailers() != null ? sre.getTrailers() : metadata;
            call.close(sre.getStatus(), trailers);
        }
    }
}
```

## Dev Mode

By default, when starting the application in dev mode, a gRPC server is started, even if no services are configured. You can configure the gRPC extension's dev mode behavior using the following properties.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-grpc_quarkus-grpc-dev-mode_quarkus-grpc-dev-mode-force-server-start"></a> [`quarkus.grpc.dev-mode.force-server-start`](#quarkus-grpc_quarkus-grpc-dev-mode_quarkus-grpc-dev-mode-force-server-start) config_property_copy_button:+++quarkus.grpc.dev-mode.force-server-start+++[]<br><br>Start gRPC server in dev mode even if no gRPC services are implemented. By default set to `true` to ease incremental development of new services using dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_DEV_MODE_FORCE_SERVER_START+++[] | boolean | `true` |

## Inject mock clients

In your `@QuarkusTest`, you can use `@InjectMock` to inject the Mutiny client of a gRPC service:

```java
@QuarkusTest
public class GrpcMockTest {

    @InjectMock
    @GrpcClient("hello")
    Greeter greeter;

    @Test
    void test1() {
        HelloRequest request = HelloRequest.newBuilder().setName("neo").build();
        Mockito.when(greeter.sayHello(Mockito.any(HelloRequest.class)))
                .thenReturn(Uni.createFrom().item(HelloReply.newBuilder().setMessage("hello neo").build()));
        Assertions.assertEquals(greeter.sayHello(request).await().indefinitely().getMessage(), "hello neo");
    }
}
```

> [!IMPORTANT]
> Only the Mutiny client can be *mocked*, channels, and other stubs cannot be mocked.
