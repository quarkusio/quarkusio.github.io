# Using the legacy REST Client

> [!WARNING]
> This guide is about the REST Client compatible with [RESTEasy Classic](https://resteasy.dev) which used to be the default Jakarta REST (formerly known as JAX-RS) implementation until Quarkus 2.8.
>
> It is now recommended to use Quarkus REST (formerly RESTEasy Reactive), which supports equally well traditional blocking workloads and reactive workloads. For more information about Quarkus REST, please see the [REST Client guide](/guides/rest-client/index.md) and, for the server side, the [introductory REST JSON guide](/guides/rest-json/index.md) or the more detailed [Quarkus REST guide](/guides/rest/index.md).

This guide explains how to use the RESTEasy REST Client in order to interact with REST APIs with very little effort.

> [!TIP]
> there is another guide if you need to write server [JSON REST APIs](/guides/rest-json/index.md).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `resteasy-client-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/resteasy-client-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:resteasy-client-quickstart \
    --extension='resteasy,resteasy-jackson,resteasy-client,resteasy-client-jackson' \
    --no-code
cd resteasy-client-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=resteasy-client-quickstart \
    -Dextensions='resteasy,resteasy-jackson,resteasy-client,resteasy-client-jackson' \
    -DnoCode
cd resteasy-client-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=resteasy-client-quickstart"`

This command generates the Maven project with a REST endpoint and imports:

- the `resteasy` and `resteasy-jackson` extensions for the REST server support;
- the `resteasy-client` and `resteasy-client-jackson` extensions for the REST client support.

If you already have your Quarkus project configured, you can add the `resteasy-client` and the `resteasy-client-jackson` extensions to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add resteasy-client,resteasy-client-jackson
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='resteasy-client,resteasy-client-jackson'
```

**Gradle**

```bash
./gradlew addExtension --extensions='resteasy-client,resteasy-client-jackson'
```

This will add the following to your `pom.xml`:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-client</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-client-jackson</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-resteasy-client")
implementation("io.quarkus:quarkus-resteasy-client-jackson")
```

## Setting up the model

In this guide we will be demonstrating how to consume part of the REST API supplied by the [stage.code.quarkus.io](https://stage.code.quarkus.io) service. Our first order of business is to set up the model we will be using, in the form of an `Extension` POJO.

Create a `src/main/java/org/acme/rest/client/Extension.java` file and set the following content:

```java
package org.acme.rest.client;

import java.util.List;

public class Extension {

    public String id;
    public String name;
    public String shortName;
    public List<String> keywords;

}
```

The model above is only a subset of the fields provided by the service, but it suffices for the purposes of this guide.

## Create the interface

Using the RESTEasy REST Client is as simple as creating an interface using the proper Jakarta REST and MicroProfile annotations. In our case the interface should be created at `src/main/java/org/acme/rest/client/ExtensionsService.java` and have the following content:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam String id);
}
```

The `getById` method gives our code the ability to get an extension by id from the Code Quarkus API. The client will handle all the networking and marshalling leaving our code clean of such technical details.

The purpose of the annotations in the code above is the following:

- `@RegisterRestClient` allows Quarkus to know that this interface is meant to be available for CDI injection as a REST Client
- `@Path`, `@GET` and `@QueryParam` are the standard Jakarta REST annotations used to define how to access the service

> [!NOTE]
> When a JSON extension is installed such as `quarkus-resteasy-client-jackson` or `quarkus-resteasy-client-jsonb`, Quarkus will use the `application/json` media type by default for most return values, unless the media type is explicitly set via `@Produces` or `@Consumes` annotations (there are some exceptions for well known types, such as `String` and `File`, which default to `text/plain` and `application/octet-stream` respectively).
>
> If you don't want JSON by default you can set `quarkus.resteasy-json.default-json=false` and the default will change back to being auto-negotiated. If you set this you will need to add `@Produces(MediaType.APPLICATION_JSON)` and `@Consumes(MediaType.APPLICATION_JSON)` to your endpoints in order to use JSON.
>
> If you don't rely on the JSON default, it is heavily recommended to annotate your endpoints with the `@Produces` and `@Consumes` annotations to define precisely the expected content-types. It will allow to narrow down the number of Jakarta REST providers (which can be seen as converters) included in the native executable.

### Path Parameters

If the GET request requires path parameters you can leverage the `@PathParam("parameter-name")` annotation instead of (or in addition to) the `@QueryParam`. Path and query parameters can be combined, as required, as illustrated in a mock example below.

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Set;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    @Path("/stream/{stream}")
    Set<Extension> getByStream(@PathParam String stream, @QueryParam("id") String id);
}
```

## Create the configuration

In order to determine the base URL to which REST calls will be made, the REST Client uses configuration from `application.properties`. The name of the property needs to follow a certain convention which is best displayed in the following code:

```properties
# Your configuration properties
quarkus.rest-client."org.acme.rest.client.ExtensionsService".url=https://stage.code.quarkus.io/api # // <1>
quarkus.rest-client."org.acme.rest.client.ExtensionsService".scope=jakarta.inject.Singleton # // <2>
```

1. Having this configuration means that all requests performed using `ExtensionsService` will use `https://stage.code.quarkus.io` as the base URL. Using the configuration above, calling the `getById` method of `ExtensionsService` with a value of `io.quarkus:quarkus-resteasy-client` would result in an HTTP GET request being made to `https://stage.code.quarkus.io/api/extensions?id=io.quarkus:quarkus-rest-client`.
2. Having this configuration means that the default scope of `ExtensionsService` will be `@Singleton`. Supported scope values are `@Singleton`, `@Dependent`, `@ApplicationScoped` and `@RequestScoped`. The default scope is `@Dependent`. The default scope can also be defined on the interface.

Note that `org.acme.rest.client.ExtensionsService` *must* match the fully qualified name of the `ExtensionsService` interface we created in the previous section.

> [!NOTE]
> The standard MicroProfile Rest Client properties notation can also be used to configure the client:
>
> ```properties
> org.acme.rest.client.ExtensionsService/mp-rest/url=https://stage.code.quarkus.io/api
> org.acme.rest.client.ExtensionsService/mp-rest/scope=jakarta.inject.Singleton
> ```
>
> If a property is specified via both the Quarkus notation and the MicroProfile notation, the Quarkus notation takes a precedence.

To facilitate the configuration, you can use the `@RegisterRestClient` `configKey` property that allows to use another configuration root than the fully qualified name of your interface.

```java

@RegisterRestClient(configKey="extensions-api")
public interface ExtensionsService {
    [...]
}
```

```properties
# Your configuration properties
quarkus.rest-client.extensions-api.url=https://stage.code.quarkus.io/api
quarkus.rest-client.extensions-api.scope=jakarta.inject.Singleton
```

### Disabling Hostname Verification

To disable the SSL hostname verification for a specific REST client, add the following property to your configuration:

```properties
quarkus.rest-client.extensions-api.verify-host=false
```

> [!WARNING]
> This setting should not be used in production as it will disable the SSL hostname verification.

Moreover, you can configure a REST client to use your custom hostname verify strategy. All you need to do is to provide a class that implements the interface `javax.net.ssl.HostnameVerifier` and add the following property to your configuration:

```properties
quarkus.rest-client.extensions-api.hostname-verifier=<full qualified custom hostname verifier class name>
```

> [!NOTE]
> Quarkus REST client provides an embedded hostname verifier strategy to disable the hostname verification called `io.quarkus.restclient.NoopHostnameVerifier`.

### Disabling SSL verifications

To disable all SSL verifications, add the following property to your configuration:

```properties
quarkus.tls.trust-all=true
```

> [!WARNING]
> This setting should not be used in production as it will disable any kind of SSL verification.

## Create the Jakarta REST resource

Create the `src/main/java/org/acme/rest/client/ExtensionsResource.java` file with the following content:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Set;

@Path("/extension")
public class ExtensionsResource {

    @Inject
    @RestClient
    ExtensionsService extensionsService;

    @GET
    @Path("/id/{id}")
    public Set<Extension> id(@PathParam String id) {
        return extensionsService.getById(id);
    }
}
```

Note that in addition to the standard CDI `@Inject` annotation, we also need to use the MicroProfile `@RestClient` annotation to inject `ExtensionsService`.

## Update the test

We also need to update the functional test to reflect the changes made to the endpoint. Edit the `src/test/java/org/acme/rest/client/ExtensionsResourceTest.java` file and change the content of the `testExtensionIdEndpoint` method to:

```java
package org.acme.rest.client;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

import org.acme.rest.client.resources.WireMockExtensionsResource;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WireMockExtensionsResource.class)
public class ExtensionsResourceTest {

    @Test
    public void testExtensionsIdEndpoint() {
        given()
            .when().get("/extension/id/io.quarkus:quarkus-rest-client")
            .then()
            .statusCode(200)
            .body("$.size()", is(1),
                "[0].id", is("io.quarkus:quarkus-rest-client"),
                "[0].name", is("REST Client Classic"),
                "[0].keywords.size()", greaterThan(1),
                "[0].keywords", hasItem("rest-client"));
    }
}
```

The code above uses [REST Assured](https://rest-assured.io/)'s [json-path](https://github.com/rest-assured/rest-assured/wiki/GettingStarted#jsonpath) capabilities.

<a id="redirection"></a>
## Redirection

A HTTP server can redirect a response to another location by sending a response with a status code that starts with "3" and a HTTP header "Location" holding the URL to be redirected to. When the REST Client receives a redirection response from a HTTP server, it won't automatically perform another request to the new location. However, you can enable the automatic redirection by enabling the "follow-redirects" property:

- `quarkus.rest-client.follow-redirects` to enable redirection for all REST clients.
- `quarkus.rest-client.<client-prefix>.follow-redirects` to enable redirection for a specific REST client.

If this property is true, then REST Client will perform a new request that it receives a redirection response from the HTTP server.

Additionally, we can limit the number of redirections using the property "max-redirects".

One important note is that according to the [RFC2616](https://www.rfc-editor.org/rfc/rfc2616#section-10.3.8) specs, by default the redirection will only happen for GET or HEAD methods.

## Async Support

The rest client supports asynchronous rest calls. Async support comes in 2 flavors: you can return a `CompletionStage` or a `Uni` (requires the `quarkus-resteasy-client-mutiny` extension). Let's see it in action by adding a `getByIdAsync` method in our `ExtensionsService` REST interface. The code should look like:

```java
package org.acme.rest.client;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam String id);

    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam String id);

}
```

Open the `src/main/java/org/acme/rest/client/ExtensionsResource.java` file and update it with the following content:

```java
package org.acme.rest.client;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/extension")
public class ExtensionsResource {

    @Inject
    @RestClient
    ExtensionsService extensionsService;

    @GET
    @Path("/id/{id}")
    public Set<Extension> id(@PathParam String id) {
        return extensionsService.getById(id);
    }

    @GET
    @Path("/id-async/{id}")
    public CompletionStage<Set<Extension>> idAsync(@PathParam String id) {
        return extensionsService.getByIdAsync(id);
    }

}
```

To test asynchronous methods, add the test method below in `ExtensionsResourceTest`:

```java
@Test
public void testExtensionIdAsyncEndpoint() {
    given()
        .when().get("/extension/id-async/io.quarkus:quarkus-rest-client")
        .then()
        .statusCode(200)
        .body("$.size()", is(1),
            "[0].id", is("io.quarkus:quarkus-rest-client"),
            "[0].name", is("REST Client Classic"),
            "[0].keywords.size()", greaterThan(1),
            "[0].keywords", hasItem("rest-client"));
}
```

The `Uni` version is very similar:

```java
package org.acme.rest.client;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import io.smallrye.mutiny.Uni;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    // ...

    @GET
    Uni<Set<Extension>> getByIdAsUni(@QueryParam String id);
}
```

The `ExtensionsResource` becomes:

```java
package org.acme.rest.client;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.smallrye.mutiny.Uni;

@Path("/extension")
public class ExtensionsResource {

    @Inject
    @RestClient
    ExtensionsService extensionsService;


    // ...

    @GET
    @Path("/id-uni/{id}")
    public Uni<Set<Extension>> idMutiny(@PathParam String id) {
        return extensionsService.getByIdAsUni(id);
    }
}
```

> [!TIP]
> **Mutiny**
>
> The previous snippet uses Mutiny reactive types. If you are not familiar with Mutiny, check [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

When returning a `Uni`, every *subscription* invokes the remote service. It means you can re-send the request by re-subscribing on the `Uni`, or use a `retry` as follows:

```java

@Inject @RestClient ExtensionsService extensionsService;

// ...

extensionsService.getByIdAsUni(id)
    .onFailure().retry().atMost(10);
```

If you use a `CompletionStage`, you would need to call the service's method to retry. This difference comes from the laziness aspect of Mutiny and its subscription protocol. More details about this can be found in [the Mutiny documentation](https://smallrye.io/smallrye-mutiny/latest/reference/uni-and-multi/).

## Custom headers support

The MicroProfile REST client allows amending request headers by registering a `ClientHeadersFactory` with the `@RegisterClientHeaders` annotation.

Let's see it in action by adding a `@RegisterClientHeaders` annotation pointing to a `RequestUUIDHeaderFactory` class in our `ExtensionsService` REST interface:

```java
package org.acme.rest.client;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.QueryParam;

import io.smallrye.mutiny.Uni;

@Path("/extensions")
@RegisterRestClient
@RegisterClientHeaders(RequestUUIDHeaderFactory.class)
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam String id);

    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam String id);

    @GET
    Uni<Set<Extension>> getByIdAsUni(@QueryParam String id);
}
```

And the `RequestUUIDHeaderFactory` would look like:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.UUID;

@ApplicationScoped
public class RequestUUIDHeaderFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("X-request-uuid", UUID.randomUUID().toString());
        return result;
    }
}
```

As you see in the example above, you can make your `ClientHeadersFactory` implementation a CDI bean by annotating it with a scope-defining annotation, such as `@Singleton`, `@ApplicationScoped`, etc.

### Default header factory

You can also use `@RegisterClientHeaders` annotation without any custom factory specified. In that case the `DefaultClientHeadersFactoryImpl` factory will be used and all headers listed in `org.eclipse.microprofile.rest.client.propagateHeaders` configuration property will be amended. Individual header names are comma-separated.

```java
@Path("/extensions")
@RegisterRestClient
@RegisterClientHeaders
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam String id);

    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam String id);

    @GET
    Uni<Set<Extension>> getByIdAsUni(@QueryParam String id);
}

```

```properties
org.eclipse.microprofile.rest.client.propagateHeaders=Authorization,Proxy-Authorization
```

## Package and run the application

Run the application with:

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

Open your browser to <http://localhost:8080/extension/id/io.quarkus:quarkus-rest-client>.

You should see a JSON object containing some basic information about the REST Client extension.

As usual, the application can be packaged using:

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

And executed with `java -jar target/quarkus-app/quarkus-run.jar`.

You can also generate the native executable with:

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

## REST Client and RESTEasy interactions

In Quarkus, the REST Client extension and [the RESTEasy extension](/guides/rest-json/index.md) share the same infrastructure. One important consequence of this consideration is that they share the same list of providers (in the Jakarta REST meaning of the word).

For instance, if you declare a `WriterInterceptor`, it will by default intercept both the servers calls and the client calls, which might not be the desired behavior.

However, you can change this default behavior and constrain a provider to:

- only consider **client** calls by adding the `@ConstrainedTo(RuntimeType.CLIENT)` annotation to your provider;
- only consider **server** calls by adding the `@ConstrainedTo(RuntimeType.SERVER)` annotation to your provider.

## Using a Mock HTTP Server for tests

In some cases you may want to mock the remote endpoint - the HTTP server - instead of mocking the client itself. This may be especially useful for native tests, or for programmatically created clients.

You can easily mock an HTTP Server with Wiremock. The [Wiremock section of the Quarkus - Using the REST Client](/guides/rest-client/index.md#using-a-mock-http-server-for-tests) describes how to set it up in detail.

## Further reading

\* [MicroProfile Rest Client specification](https://download.eclipse.org/microprofile/microprofile-rest-client-2.0/microprofile-rest-client-spec-2.0.html)

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-rest-client-config_quarkus-rest-client-scope"></a> [`quarkus.rest-client.scope`](#quarkus-rest-client-config_quarkus-rest-client-scope) config_property_copy_button:+++quarkus.rest-client.scope+++[]<br><br>The CDI scope to use for injection. This property can contain either a fully qualified class name of a CDI scope annotation (such as "jakarta.enterprise.context.ApplicationScoped") or its simple name (such as "ApplicationScoped"). By default, this is not set which means the interface is not registered as a bean unless it is annotated with `RegisterRestClient`. If an interface is not annotated with `RegisterRestClient` and this property is set, then Quarkus will make the interface a bean of the configured scope.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_SCOPE+++[] | string |  |
| [lock] <a id="quarkus-rest-client-config_quarkus-rest-client-removes-trailing-slash"></a> [`quarkus.rest-client.removes-trailing-slash`](#quarkus-rest-client-config_quarkus-rest-client-removes-trailing-slash) config_property_copy_button:+++quarkus.rest-client.removes-trailing-slash+++[]<br><br>If true, the extension will automatically remove the trailing slash in the paths if any. This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_REMOVES_TRAILING_SLASH+++[] | boolean | `true` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-multipart-post-encoder-mode"></a> [`quarkus.rest-client.multipart-post-encoder-mode`](#quarkus-rest-client-config_quarkus-rest-client-multipart-post-encoder-mode) config_property_copy_button:+++quarkus.rest-client.multipart-post-encoder-mode+++[]<br><br>Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`. The modes are described in the [Netty documentation](https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html)<br><br>By default, Rest Client Reactive uses RFC1738.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_MULTIPART_POST_ENCODER_MODE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-proxy-configuration-name"></a> [`quarkus.rest-client.proxy-configuration-name`](#quarkus-rest-client-config_quarkus-rest-client-proxy-configuration-name) config_property_copy_button:+++quarkus.rest-client.proxy-configuration-name+++[]<br><br>The name of the proxy configuration to use; ignored if `quarkus.rest-client.proxy-address` is set.<br><br>If not set and the default proxy configuration is configured (`quarkus.proxy.*`) then that will be used. If the proxy configuration name is set, the configuration from `quarkus.proxy.<name>.*` will be used. If the proxy configuration name is set, but no proxy configuration is found with that name, then an error will be thrown at runtime.<br><br>Can be overwritten by client-specific settings.<br><br>Use the value `none` to disable using the default configuration defined via `quarkus.proxy.*`.<br><br>Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency) does not support this property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROXY_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-connect-timeout"></a> [`quarkus.rest-client.connect-timeout`](#quarkus-rest-client-config_quarkus-rest-client-connect-timeout) config_property_copy_button:+++quarkus.rest-client.connect-timeout+++[]<br><br>A timeout in milliseconds that REST clients should wait to connect to the remote endpoint.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_CONNECT_TIMEOUT+++[] | long | `15000` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-read-timeout"></a> [`quarkus.rest-client.read-timeout`](#quarkus-rest-client-config_quarkus-rest-client-read-timeout) config_property_copy_button:+++quarkus.rest-client.read-timeout+++[]<br><br>A timeout in milliseconds that REST clients should wait for a response from the remote endpoint.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_READ_TIMEOUT+++[] | long | `30000` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-disable-contextual-error-messages"></a> [`quarkus.rest-client.disable-contextual-error-messages`](#quarkus-rest-client-config_quarkus-rest-client-disable-contextual-error-messages) config_property_copy_button:+++quarkus.rest-client.disable-contextual-error-messages+++[]<br><br>If true, the REST clients will not provide additional contextual information (like REST client class and method names) when exception occurs during a client invocation.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_DISABLE_CONTEXTUAL_ERROR_MESSAGES+++[] | boolean | `false` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-user-agent"></a> [`quarkus.rest-client.user-agent`](#quarkus-rest-client-config_quarkus-rest-client-user-agent) config_property_copy_button:+++quarkus.rest-client.user-agent+++[]<br><br>Default configuration for the HTTP user-agent header to use in all REST clients.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_USER_AGENT+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-headers-header-name"></a> [`quarkus.rest-client.headers."header-name"`](#quarkus-rest-client-config_quarkus-rest-client-headers-header-name) config_property_copy_button:+++quarkus.rest-client.headers."header-name"+++[]<br><br>The HTTP headers that should be applied to all requests of the rest client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_HEADERS__HEADER_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-hostname-verifier"></a> [`quarkus.rest-client.hostname-verifier`](#quarkus-rest-client-config_quarkus-rest-client-hostname-verifier) config_property_copy_button:+++quarkus.rest-client.hostname-verifier+++[]<br><br>The class name of the host name verifier. The class must have a public no-argument constructor.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_HOSTNAME_VERIFIER+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-connection-ttl"></a> [`quarkus.rest-client.connection-ttl`](#quarkus-rest-client-config_quarkus-rest-client-connection-ttl) config_property_copy_button:+++quarkus.rest-client.connection-ttl+++[]<br><br>The time in ms for which a connection remains unused in the connection pool before being evicted and closed.<br><br>Although this is set in milliseconds, the underlying client can only be configured in seconds, meaning that for this value to actually take effect, it must be higher than `1000`; any value less than or equal to `999` is converted to 0 seconds.<br><br>A timeout of `0` means there is no timeout.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_CONNECTION_TTL+++[] | int | `60000` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-connection-pool-size"></a> [`quarkus.rest-client.connection-pool-size`](#quarkus-rest-client-config_quarkus-rest-client-connection-pool-size) config_property_copy_button:+++quarkus.rest-client.connection-pool-size+++[]<br><br>The size of the connection pool for this client.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_CONNECTION_POOL_SIZE+++[] | int | `50` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-keep-alive-enabled"></a> [`quarkus.rest-client.keep-alive-enabled`](#quarkus-rest-client-config_quarkus-rest-client-keep-alive-enabled) config_property_copy_button:+++quarkus.rest-client.keep-alive-enabled+++[]<br><br>If set to false disables the keep alive completely.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_KEEP_ALIVE_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-max-redirects"></a> [`quarkus.rest-client.max-redirects`](#quarkus-rest-client-config_quarkus-rest-client-max-redirects) config_property_copy_button:+++quarkus.rest-client.max-redirects+++[]<br><br>The maximum number of redirection a request can follow.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_MAX_REDIRECTS+++[] | int |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-follow-redirects"></a> [`quarkus.rest-client.follow-redirects`](#quarkus-rest-client-config_quarkus-rest-client-follow-redirects) config_property_copy_button:+++quarkus.rest-client.follow-redirects+++[]<br><br>A boolean value used to determine whether the client should follow HTTP redirect responses.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_FOLLOW_REDIRECTS+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-same-origin-redirect-blocked-headers"></a> [`quarkus.rest-client.same-origin-redirect-blocked-headers`](#quarkus-rest-client-config_quarkus-rest-client-same-origin-redirect-blocked-headers) config_property_copy_button:+++quarkus.rest-client.same-origin-redirect-blocked-headers+++[]<br><br>The set of HTTP headers that are removed on same-origin redirect responses.<br><br>When not set, the Vert.x default is used.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_SAME_ORIGIN_REDIRECT_BLOCKED_HEADERS+++[] | list of string | `cookie`, `content-length` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-cross-origin-redirect-blocked-headers"></a> [`quarkus.rest-client.cross-origin-redirect-blocked-headers`](#quarkus-rest-client-config_quarkus-rest-client-cross-origin-redirect-blocked-headers) config_property_copy_button:+++quarkus.rest-client.cross-origin-redirect-blocked-headers+++[]<br><br>The set of HTTP headers that are removed on cross-origin redirect responses.<br><br>When not set, the Vert.x default is used.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_CROSS_ORIGIN_REDIRECT_BLOCKED_HEADERS+++[] | list of string | `authorization`, `cookie`, `proxy-authorization`, `content-length` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-providers"></a> [`quarkus.rest-client.providers`](#quarkus-rest-client-config_quarkus-rest-client-providers) config_property_copy_button:+++quarkus.rest-client.providers+++[]<br><br>Fully-qualified provider classnames to include in the client. The equivalent of the `@RegisterProvider` annotation.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROVIDERS+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-query-param-style"></a> [`quarkus.rest-client.query-param-style`](#quarkus-rest-client-config_quarkus-rest-client-query-param-style) config_property_copy_button:+++quarkus.rest-client.query-param-style+++[]<br><br>An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED", or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_QUERY_PARAM_STYLE+++[] | `multi-pairs`, `comma-separated`, `array-pairs` |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-verify-host"></a> [`quarkus.rest-client.verify-host`](#quarkus-rest-client-config_quarkus-rest-client-verify-host) config_property_copy_button:+++quarkus.rest-client.verify-host+++[]<br><br>Set whether hostname verification is enabled. Default is enabled. This setting should not be disabled in production as it makes the client vulnerable to MITM attacks.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_VERIFY_HOST+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-trust-store"></a> [`quarkus.rest-client.trust-store`](#quarkus-rest-client-config_quarkus-rest-client-trust-store) config_property_copy_button:+++quarkus.rest-client.trust-store+++[]<br><br>The trust store location. Can point to either a classpath resource or a file.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_TRUST_STORE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-trust-store-password"></a> [`quarkus.rest-client.trust-store-password`](#quarkus-rest-client-config_quarkus-rest-client-trust-store-password) config_property_copy_button:+++quarkus.rest-client.trust-store-password+++[]<br><br>The trust store password.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-trust-store-type"></a> [`quarkus.rest-client.trust-store-type`](#quarkus-rest-client-config_quarkus-rest-client-trust-store-type) config_property_copy_button:+++quarkus.rest-client.trust-store-type+++[]<br><br>The type of the trust store. Defaults to "JKS".<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_TRUST_STORE_TYPE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-key-store"></a> [`quarkus.rest-client.key-store`](#quarkus-rest-client-config_quarkus-rest-client-key-store) config_property_copy_button:+++quarkus.rest-client.key-store+++[]<br><br>The key store location. Can point to either a classpath resource or a file.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_KEY_STORE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-key-store-password"></a> [`quarkus.rest-client.key-store-password`](#quarkus-rest-client-config_quarkus-rest-client-key-store-password) config_property_copy_button:+++quarkus.rest-client.key-store-password+++[]<br><br>The key store password.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-key-store-type"></a> [`quarkus.rest-client.key-store-type`](#quarkus-rest-client-config_quarkus-rest-client-key-store-type) config_property_copy_button:+++quarkus.rest-client.key-store-type+++[]<br><br>The type of the key store. Defaults to "JKS".<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_KEY_STORE_TYPE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-tls-configuration-name"></a> [`quarkus.rest-client.tls-configuration-name`](#quarkus-rest-client-config_quarkus-rest-client-tls-configuration-name) config_property_copy_button:+++quarkus.rest-client.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown. The default TLS configuration will be ignored.<br><br>If no named TLS configuration is set, then the key-store, trust-store, etc. properties will be used.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-http2"></a> [`quarkus.rest-client.http2`](#quarkus-rest-client-config_quarkus-rest-client-http2) config_property_copy_button:+++quarkus.rest-client.http2+++[]<br><br>If this is true then HTTP/2 will be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_HTTP2+++[] | boolean | `false` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-http2-upgrade-max-content-length"></a> [`quarkus.rest-client.http2-upgrade-max-content-length`](#quarkus-rest-client-config_quarkus-rest-client-http2-upgrade-max-content-length) config_property_copy_button:+++quarkus.rest-client.http2-upgrade-max-content-length+++[]<br><br>Configures the HTTP/2 upgrade maximum length of the aggregated content in bytes.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_HTTP2_UPGRADE_MAX_CONTENT_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-rest-client-config_quarkus-rest-client) | `64K` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-max-chunk-size"></a> [`quarkus.rest-client.max-chunk-size`](#quarkus-rest-client-config_quarkus-rest-client-max-chunk-size) config_property_copy_button:+++quarkus.rest-client.max-chunk-size+++[]<br><br>Configures two different things:<br><br>\* The max HTTP chunk size, up to `Integer.MAX_VALUE` bytes. \* The size of the chunk to be read when an `InputStream` is being used as an input<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_MAX_CHUNK_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-rest-client-config_quarkus-rest-client) | `8k` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-enable-compression"></a> [`quarkus.rest-client.enable-compression`](#quarkus-rest-client-config_quarkus-rest-client-enable-compression) config_property_copy_button:+++quarkus.rest-client.enable-compression+++[]<br><br>Supports receiving compressed messages using GZIP. When this feature is enabled and a server returns a response that includes the header `Content-Encoding: gzip`, REST Client will automatically decode the content and proceed with the message handling.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_ENABLE_COMPRESSION+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-alpn"></a> [`quarkus.rest-client.alpn`](#quarkus-rest-client-config_quarkus-rest-client-alpn) config_property_copy_button:+++quarkus.rest-client.alpn+++[]<br><br>If the Application-Layer Protocol Negotiation is enabled, the client will negotiate which protocol to use over the protocols exposed by the server. By default, it will try to use HTTP/2 first and if it's not enabled, it will use HTTP/1.1. When the property `http2` is enabled, this flag will be automatically enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_ALPN+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-capture-stacktrace"></a> [`quarkus.rest-client.capture-stacktrace`](#quarkus-rest-client-config_quarkus-rest-client-capture-stacktrace) config_property_copy_button:+++quarkus.rest-client.capture-stacktrace+++[]<br><br>If `true`, the stacktrace of the invocation of the REST Client method is captured. This stacktrace will be used if the invocation throws an exception<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_CAPTURE_STACKTRACE+++[] | boolean | `false` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-logging-scope"></a> [`quarkus.rest-client.logging.scope`](#quarkus-rest-client-config_quarkus-rest-client-logging-scope) config_property_copy_button:+++quarkus.rest-client.logging.scope+++[]<br><br>Scope of logging for the client.<br><br>> [!WARNING]<br>> beware of logging sensitive data The possible values are:<br><br>\<br> \* `request-response` - enables logging request and responses, including redirect responses \* `all` - enables logging requests and responses and lower-level logging \* `none` - no additional logging<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_LOGGING_SCOPE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-logging-body-limit"></a> [`quarkus.rest-client.logging.body-limit`](#quarkus-rest-client-config_quarkus-rest-client-logging-body-limit) config_property_copy_button:+++quarkus.rest-client.logging.body-limit+++[]<br><br>How many characters of the body should be logged. Message body can be large and can easily pollute the logs.<br><br>By default, set to 100.<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_LOGGING_BODY_LIMIT+++[] | int | `100` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-logging-masked-headers"></a> [`quarkus.rest-client.logging.masked-headers`](#quarkus-rest-client-config_quarkus-rest-client-logging-masked-headers) config_property_copy_button:+++quarkus.rest-client.logging.masked-headers+++[]<br><br>Which request and response headers values to mask in logs.<br><br>The value of any matching header will be replaced with `"<hidden>"`. The header name itself remains visible. E.g. `Authorization=<hidden>`<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_LOGGING_MASKED_HEADERS+++[] | list of string | `Authorization`, `Cookie` |
| [lock] <a id="quarkus-rest-client-config_quarkus-rest-client-client-scope"></a> [`quarkus.rest-client."client".scope`](#quarkus-rest-client-config_quarkus-rest-client-client-scope) config_property_copy_button:+++quarkus.rest-client."client".scope+++[]<br><br>The CDI scope to use for injection. This property can contain either a fully qualified class name of a CDI scope annotation (such as "jakarta.enterprise.context.ApplicationScoped") or its simple name (such as "ApplicationScoped"). By default, this is not set which means the interface is not registered as a bean unless it is annotated with `RegisterRestClient`. If an interface is not annotated with `RegisterRestClient` and this property is set, then Quarkus will make the interface a bean of the configured scope.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__SCOPE+++[] | string |  |
| [lock] <a id="quarkus-rest-client-config_quarkus-rest-client-client-enable-local-proxy"></a> [`quarkus.rest-client."client".enable-local-proxy`](#quarkus-rest-client-config_quarkus-rest-client-client-enable-local-proxy) config_property_copy_button:+++quarkus.rest-client."client".enable-local-proxy+++[]<br><br>If set to true, then Quarkus will ensure that all calls from the REST client go through a local proxy server (that is managed by Quarkus). This can be very useful for capturing network traffic to a service that uses HTTPS.<br><br>This property is not applicable to the RESTEasy Client, only the Quarkus REST client (formerly RESTEasy Reactive client).<br><br>This property only applicable to dev and test mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__ENABLE_LOCAL_PROXY+++[] | boolean | `false` |
| [lock] <a id="quarkus-rest-client-config_quarkus-rest-client-client-local-proxy-provider"></a> [`quarkus.rest-client."client".local-proxy-provider`](#quarkus-rest-client-config_quarkus-rest-client-client-local-proxy-provider) config_property_copy_button:+++quarkus.rest-client."client".local-proxy-provider+++[]<br><br>This setting is used to select which proxy provider to use if there are multiple ones. It only applies if `enable-local-proxy` is true.<br><br>The algorithm for picking between multiple provider is the following:<br><br>\* If only the default is around, use it (its name is `default`) \* If there is only one besides the default, use it \* If there are multiple ones, fail<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__LOCAL_PROXY_PROVIDER+++[] | string |  |
| [lock] <a id="quarkus-rest-client-config_quarkus-rest-client-client-removes-trailing-slash"></a> [`quarkus.rest-client."client".removes-trailing-slash`](#quarkus-rest-client-config_quarkus-rest-client-client-removes-trailing-slash) config_property_copy_button:+++quarkus.rest-client."client".removes-trailing-slash+++[]<br><br>If true, the extension will automatically remove the trailing slash in the paths if any. This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__REMOVES_TRAILING_SLASH+++[] | boolean | `true` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-url"></a> [`quarkus.rest-client."client".url`](#quarkus-rest-client-config_quarkus-rest-client-client-url) config_property_copy_button:+++quarkus.rest-client."client".url+++[]<br><br>The base URL to use for this service. This property or the `uri` property is considered required, unless the `baseUri` attribute is configured in the `@RegisterRestClient` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__URL+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-uri"></a> [`quarkus.rest-client."client".uri`](#quarkus-rest-client-config_quarkus-rest-client-client-uri) config_property_copy_button:+++quarkus.rest-client."client".uri+++[]<br><br>The base URI to use for this service. This property or the `url` property is considered required, unless the `baseUri` attribute is configured in the `@RegisterRestClient` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__URI+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-override-uri"></a> [`quarkus.rest-client."client".override-uri`](#quarkus-rest-client-config_quarkus-rest-client-client-override-uri) config_property_copy_button:+++quarkus.rest-client."client".override-uri+++[]<br><br>This property is only meant to be set by advanced configurations to override whatever value was set for the uri or url. The override is done using the REST Client class name configuration syntax.<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__OVERRIDE_URI+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-providers"></a> [`quarkus.rest-client."client".providers`](#quarkus-rest-client-config_quarkus-rest-client-client-providers) config_property_copy_button:+++quarkus.rest-client."client".providers+++[]<br><br>Map where keys are fully-qualified provider classnames to include in the client, and values are their integer priorities. The equivalent of the `@RegisterProvider` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__PROVIDERS+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-connect-timeout"></a> [`quarkus.rest-client."client".connect-timeout`](#quarkus-rest-client-config_quarkus-rest-client-client-connect-timeout) config_property_copy_button:+++quarkus.rest-client."client".connect-timeout+++[]<br><br>Timeout specified in milliseconds to wait to connect to the remote endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__CONNECT_TIMEOUT+++[] | long |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-read-timeout"></a> [`quarkus.rest-client."client".read-timeout`](#quarkus-rest-client-config_quarkus-rest-client-client-read-timeout) config_property_copy_button:+++quarkus.rest-client."client".read-timeout+++[]<br><br>Timeout specified in milliseconds to wait for a response from the remote endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__READ_TIMEOUT+++[] | long |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-follow-redirects"></a> [`quarkus.rest-client."client".follow-redirects`](#quarkus-rest-client-config_quarkus-rest-client-client-follow-redirects) config_property_copy_button:+++quarkus.rest-client."client".follow-redirects+++[]<br><br>A boolean value used to determine whether the client should follow HTTP redirect responses.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__FOLLOW_REDIRECTS+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-multipart-post-encoder-mode"></a> [`quarkus.rest-client."client".multipart-post-encoder-mode`](#quarkus-rest-client-config_quarkus-rest-client-client-multipart-post-encoder-mode) config_property_copy_button:+++quarkus.rest-client."client".multipart-post-encoder-mode+++[]<br><br>Mode in which the form data are encoded. Possible values are `HTML5`, `RFC1738` and `RFC3986`. The modes are described in the [Netty documentation](https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html)<br><br>By default, Rest Client Reactive uses RFC1738.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__MULTIPART_POST_ENCODER_MODE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-proxy-configuration-name"></a> [`quarkus.rest-client."client".proxy-configuration-name`](#quarkus-rest-client-config_quarkus-rest-client-client-proxy-configuration-name) config_property_copy_button:+++quarkus.rest-client."client".proxy-configuration-name+++[]<br><br>The name of the proxy configuration to use; ignored if `quarkus.rest-client."client".proxy-address` is set.<br><br>If not set and `quarkus.rest-client.proxy-configuration-name` or the default proxy configuration (`quarkus.proxy.*`) is set, then the first valid of them will get effective. If the proxy configuration name is set, the configuration from `quarkus.proxy.<name>.*` will be used. If the proxy configuration name is set, but no proxy configuration is found with that name, then an error will be thrown at runtime.<br><br>Use the value `none` to disable using the default configuration defined via `quarkus.rest-client.proxy-configuration-name` or `quarkus.proxy.*`.<br><br>Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency) does not support this property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__PROXY_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-query-param-style"></a> [`quarkus.rest-client."client".query-param-style`](#quarkus-rest-client-config_quarkus-rest-client-client-query-param-style) config_property_copy_button:+++quarkus.rest-client."client".query-param-style+++[]<br><br>An enumerated type string value with possible values of "MULTI_PAIRS" (default), "COMMA_SEPARATED", or "ARRAY_PAIRS" that specifies the format in which multiple values for the same query parameter is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__QUERY_PARAM_STYLE+++[] | `multi-pairs`, `comma-separated`, `array-pairs` |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-verify-host"></a> [`quarkus.rest-client."client".verify-host`](#quarkus-rest-client-config_quarkus-rest-client-client-verify-host) config_property_copy_button:+++quarkus.rest-client."client".verify-host+++[]<br><br>Set whether hostname verification is enabled. Default is enabled. This setting should not be disabled in production as it makes the client vulnerable to MITM attacks.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__VERIFY_HOST+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-trust-store"></a> [`quarkus.rest-client."client".trust-store`](#quarkus-rest-client-config_quarkus-rest-client-client-trust-store) config_property_copy_button:+++quarkus.rest-client."client".trust-store+++[]<br><br>The trust store location. Can point to either a classpath resource or a file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__TRUST_STORE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-trust-store-password"></a> [`quarkus.rest-client."client".trust-store-password`](#quarkus-rest-client-config_quarkus-rest-client-client-trust-store-password) config_property_copy_button:+++quarkus.rest-client."client".trust-store-password+++[]<br><br>The trust store password.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-trust-store-type"></a> [`quarkus.rest-client."client".trust-store-type`](#quarkus-rest-client-config_quarkus-rest-client-client-trust-store-type) config_property_copy_button:+++quarkus.rest-client."client".trust-store-type+++[]<br><br>The type of the trust store. Defaults to "JKS".<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__TRUST_STORE_TYPE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-key-store"></a> [`quarkus.rest-client."client".key-store`](#quarkus-rest-client-config_quarkus-rest-client-client-key-store) config_property_copy_button:+++quarkus.rest-client."client".key-store+++[]<br><br>The key store location. Can point to either a classpath resource or a file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__KEY_STORE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-key-store-password"></a> [`quarkus.rest-client."client".key-store-password`](#quarkus-rest-client-config_quarkus-rest-client-client-key-store-password) config_property_copy_button:+++quarkus.rest-client."client".key-store-password+++[]<br><br>The key store password.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-key-store-type"></a> [`quarkus.rest-client."client".key-store-type`](#quarkus-rest-client-config_quarkus-rest-client-client-key-store-type) config_property_copy_button:+++quarkus.rest-client."client".key-store-type+++[]<br><br>The type of the key store. Defaults to "JKS".<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__KEY_STORE_TYPE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-hostname-verifier"></a> [`quarkus.rest-client."client".hostname-verifier`](#quarkus-rest-client-config_quarkus-rest-client-client-hostname-verifier) config_property_copy_button:+++quarkus.rest-client."client".hostname-verifier+++[]<br><br>The class name of the host name verifier. The class must have a public no-argument constructor.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__HOSTNAME_VERIFIER+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-tls-configuration-name"></a> [`quarkus.rest-client."client".tls-configuration-name`](#quarkus-rest-client-config_quarkus-rest-client-client-tls-configuration-name) config_property_copy_button:+++quarkus.rest-client."client".tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown. The default TLS configuration will be ignored.<br><br>If no named TLS configuration is set, then the key-store, trust-store, etc. properties will be used.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-connection-ttl"></a> [`quarkus.rest-client."client".connection-ttl`](#quarkus-rest-client-config_quarkus-rest-client-client-connection-ttl) config_property_copy_button:+++quarkus.rest-client."client".connection-ttl+++[]<br><br>The time in ms for which a connection remains unused in the connection pool before being evicted and closed.<br><br>Although this is set in milliseconds, the underlying client can only be configured in seconds, meaning that for this value to actually take effect, it must be higher than `1000`; any value less than or equal to `999` is converted to 0 seconds.<br><br>A timeout of `0` means there is no timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__CONNECTION_TTL+++[] | int | `60000` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-connection-pool-size"></a> [`quarkus.rest-client."client".connection-pool-size`](#quarkus-rest-client-config_quarkus-rest-client-client-connection-pool-size) config_property_copy_button:+++quarkus.rest-client."client".connection-pool-size+++[]<br><br>The size of the connection pool for this client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__CONNECTION_POOL_SIZE+++[] | int | `50` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-keep-alive-enabled"></a> [`quarkus.rest-client."client".keep-alive-enabled`](#quarkus-rest-client-config_quarkus-rest-client-client-keep-alive-enabled) config_property_copy_button:+++quarkus.rest-client."client".keep-alive-enabled+++[]<br><br>If set to false disables the keep alive completely.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__KEEP_ALIVE_ENABLED+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-max-redirects"></a> [`quarkus.rest-client."client".max-redirects`](#quarkus-rest-client-config_quarkus-rest-client-client-max-redirects) config_property_copy_button:+++quarkus.rest-client."client".max-redirects+++[]<br><br>The maximum number of redirection a request can follow.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__MAX_REDIRECTS+++[] | int |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-same-origin-redirect-blocked-headers"></a> [`quarkus.rest-client."client".same-origin-redirect-blocked-headers`](#quarkus-rest-client-config_quarkus-rest-client-client-same-origin-redirect-blocked-headers) config_property_copy_button:+++quarkus.rest-client."client".same-origin-redirect-blocked-headers+++[]<br><br>The set of HTTP headers that are removed on same-origin redirect responses.<br><br>When not set, the global setting or the Vert.x default is used.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__SAME_ORIGIN_REDIRECT_BLOCKED_HEADERS+++[] | list of string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-cross-origin-redirect-blocked-headers"></a> [`quarkus.rest-client."client".cross-origin-redirect-blocked-headers`](#quarkus-rest-client-config_quarkus-rest-client-client-cross-origin-redirect-blocked-headers) config_property_copy_button:+++quarkus.rest-client."client".cross-origin-redirect-blocked-headers+++[]<br><br>The set of HTTP headers that are removed on cross-origin redirect responses.<br><br>When not set, the global setting or the Vert.x default is used.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__CROSS_ORIGIN_REDIRECT_BLOCKED_HEADERS+++[] | list of string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-headers-header-name"></a> [`quarkus.rest-client."client".headers."header-name"`](#quarkus-rest-client-config_quarkus-rest-client-client-headers-header-name) config_property_copy_button:+++quarkus.rest-client."client".headers."header-name"+++[]<br><br>The HTTP headers that should be applied to all requests of the rest client.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__HEADERS__HEADER_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-shared"></a> [`quarkus.rest-client."client".shared`](#quarkus-rest-client-config_quarkus-rest-client-client-shared) config_property_copy_button:+++quarkus.rest-client."client".shared+++[]<br><br>Set to true to share the HTTP client between REST clients. There can be multiple shared clients distinguished by *name*, when no specific name is set, the name `__vertx.DEFAULT` is used.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__SHARED+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-name"></a> [`quarkus.rest-client."client".name`](#quarkus-rest-client-config_quarkus-rest-client-client-name) config_property_copy_button:+++quarkus.rest-client."client".name+++[]<br><br>Set the HTTP client name, used when the client is shared, otherwise ignored.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__NAME+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-user-agent"></a> [`quarkus.rest-client."client".user-agent`](#quarkus-rest-client-config_quarkus-rest-client-client-user-agent) config_property_copy_button:+++quarkus.rest-client."client".user-agent+++[]<br><br>Configure the HTTP user-agent header to use.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__USER_AGENT+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-http2"></a> [`quarkus.rest-client."client".http2`](#quarkus-rest-client-config_quarkus-rest-client-client-http2) config_property_copy_button:+++quarkus.rest-client."client".http2+++[]<br><br>If this is true then HTTP/2 will be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__HTTP2+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-http2-upgrade-max-content-length"></a> [`quarkus.rest-client."client".http2-upgrade-max-content-length`](#quarkus-rest-client-config_quarkus-rest-client-client-http2-upgrade-max-content-length) config_property_copy_button:+++quarkus.rest-client."client".http2-upgrade-max-content-length+++[]<br><br>Configures the HTTP/2 upgrade maximum length of the aggregated content in bytes.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__HTTP2_UPGRADE_MAX_CONTENT_LENGTH+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-rest-client-config_quarkus-rest-client) | `64K` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-max-chunk-size"></a> [`quarkus.rest-client."client".max-chunk-size`](#quarkus-rest-client-config_quarkus-rest-client-client-max-chunk-size) config_property_copy_button:+++quarkus.rest-client."client".max-chunk-size+++[]<br><br>Configures two different things:<br><br>\* The max HTTP chunk size, up to `Integer.MAX_VALUE` bytes. \* The size of the chunk to be read when an `InputStream` is being read and sent to the server<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__MAX_CHUNK_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-rest-client-config_quarkus-rest-client) | `8K` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-enable-response-decompression"></a> [`quarkus.rest-client."client".enable-response-decompression`](#quarkus-rest-client-config_quarkus-rest-client-client-enable-response-decompression) config_property_copy_button:+++quarkus.rest-client."client".enable-response-decompression+++[]<br><br>Supports receiving compressed messages using GZIP. When this feature is enabled and a server returns a response that includes the header `Content-Encoding: gzip`, REST Client will automatically decode the content and proceed with the message handling.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__ENABLE_RESPONSE_DECOMPRESSION+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-alpn"></a> [`quarkus.rest-client."client".alpn`](#quarkus-rest-client-config_quarkus-rest-client-client-alpn) config_property_copy_button:+++quarkus.rest-client."client".alpn+++[]<br><br>If the Application-Layer Protocol Negotiation is enabled, the client will negotiate which protocol to use over the protocols exposed by the server. By default, it will try to use HTTP/2 first and if it's not enabled, it will use HTTP/1.1. When the property `http2` is enabled, this flag will be automatically enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__ALPN+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-capture-stacktrace"></a> [`quarkus.rest-client."client".capture-stacktrace`](#quarkus-rest-client-config_quarkus-rest-client-client-capture-stacktrace) config_property_copy_button:+++quarkus.rest-client."client".capture-stacktrace+++[]<br><br>If `true`, the stacktrace of the invocation of the REST Client method is captured. This stacktrace will be used if the invocation throws an exception<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__CAPTURE_STACKTRACE+++[] | boolean |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-disable-default-mapper"></a> [`quarkus.rest-client."client".disable-default-mapper`](#quarkus-rest-client-config_quarkus-rest-client-client-disable-default-mapper) config_property_copy_button:+++quarkus.rest-client."client".disable-default-mapper+++[]<br><br>If set to `true`, then this REST Client will not the default exception mapper which always throws an exception if HTTP response code >= 400.<br><br>This property is only taken into account if the REST Client returns `jakarta.ws.rs.core.Response` or `org.jboss.resteasy.reactive.RestResponse`<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__DISABLE_DEFAULT_MAPPER+++[] | boolean | `false` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-logging-scope"></a> [`quarkus.rest-client."client".logging.scope`](#quarkus-rest-client-config_quarkus-rest-client-client-logging-scope) config_property_copy_button:+++quarkus.rest-client."client".logging.scope+++[]<br><br>Scope of logging for the client.<br><br>> [!WARNING]<br>> beware of logging sensitive data The possible values are:<br><br>\<br> \* `request-response` - enables logging request and responses, including redirect responses \* `all` - enables logging requests and responses and lower-level logging \* `none` - no additional logging<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__LOGGING_SCOPE+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-logging-body-limit"></a> [`quarkus.rest-client."client".logging.body-limit`](#quarkus-rest-client-config_quarkus-rest-client-client-logging-body-limit) config_property_copy_button:+++quarkus.rest-client."client".logging.body-limit+++[]<br><br>How many characters of the body should be logged. Message body can be large and can easily pollute the logs.<br><br>By default, set to 100.<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__LOGGING_BODY_LIMIT+++[] | int | `100` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-logging-masked-headers"></a> [`quarkus.rest-client."client".logging.masked-headers`](#quarkus-rest-client-config_quarkus-rest-client-client-logging-masked-headers) config_property_copy_button:+++quarkus.rest-client."client".logging.masked-headers+++[]<br><br>Which request and response headers values to mask in logs.<br><br>The value of any matching header will be replaced with `"<hidden>"`. The header name itself remains visible. E.g. `Authorization=<hidden>`<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__LOGGING_MASKED_HEADERS+++[] | list of string | `Authorization`, `Cookie` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-proxy-address"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-proxy-address\[`quarkus.rest-client.proxy-address`\]</mark> config_property_copy_button:+++quarkus.rest-client.proxy-address+++[]<br><br>*This property is deprecated: use `quarkus.rest-client.proxy-configuration-name` instead.*<br><br>A string value in the form of `:` that specifies the HTTP proxy server hostname (or IP address) and port for requests of clients to use.<br><br>Can be overwritten by client-specific settings.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROXY_ADDRESS+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-proxy-user"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-proxy-user\[`quarkus.rest-client.proxy-user`\]</mark> config_property_copy_button:+++quarkus.rest-client.proxy-user+++[]<br><br>*This property is deprecated: use `quarkus.rest-client.proxy-configuration-name` instead.*<br><br>Proxy username, equivalent to the http.proxy or https.proxy JVM settings. Honored only if `quarkus.rest-client.proxy-address` is set.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROXY_USER+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-proxy-password"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-proxy-password\[`quarkus.rest-client.proxy-password`\]</mark> config_property_copy_button:+++quarkus.rest-client.proxy-password+++[]<br><br>*This property is deprecated: use `quarkus.rest-client.proxy-configuration-name` instead.*<br><br>Proxy password, equivalent to the http.proxyPassword or https.proxyPassword JVM settings. Honored only if `quarkus.rest-client.proxy-address` is set.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROXY_PASSWORD+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-non-proxy-hosts"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-non-proxy-hosts\[`quarkus.rest-client.non-proxy-hosts`\]</mark> config_property_copy_button:+++quarkus.rest-client.non-proxy-hosts+++[]<br><br>*This property is deprecated: use `quarkus.rest-client.proxy-configuration-name` instead.*<br><br>Hosts to access without proxy, similar to the http.nonProxyHosts or https.nonProxyHosts JVM settings. Please note that unlike the JVM settings, this property is empty by default. Honored only if `quarkus.rest-client.proxy-address` is set.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_NON_PROXY_HOSTS+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-proxy-connect-timeout"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-proxy-connect-timeout\[`quarkus.rest-client.proxy-connect-timeout`\]</mark> config_property_copy_button:+++quarkus.rest-client.proxy-connect-timeout+++[]<br><br>*This property is deprecated: use `quarkus.rest-client.proxy-configuration-name` instead.*<br><br>Proxy connection timeout. Honored only if `quarkus.rest-client.proxy-address` is set.<br><br>Can be overwritten by client-specific settings.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROXY_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-rest-client-config_quarkus-rest-client) | `10s` |
| <a id="quarkus-rest-client-config_quarkus-rest-client-multipart-max-chunk-size"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-multipart-max-chunk-size\[`quarkus.rest-client.multipart.max-chunk-size`\]</mark> config_property_copy_button:+++quarkus.rest-client.multipart.max-chunk-size+++[]<br><br>*This property is deprecated: Use `quarkus.rest-client.max-chunk-size` instead.*<br><br>The max HTTP chunk size (8096 bytes by default).<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_MULTIPART_MAX_CHUNK_SIZE+++[] | int |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-multipart-max-chunk-size"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-client-multipart-max-chunk-size\[`quarkus.rest-client."client".multipart.max-chunk-size`\]</mark> config_property_copy_button:+++quarkus.rest-client."client".multipart.max-chunk-size+++[]<br><br>*This property is deprecated: Use `quarkus.rest-client.max-chunk-size` instead.*<br><br>The max HTTP chunk size (8096 bytes by default).<br><br>This property is not applicable to the Quarkus RESTEasy client (provided by the quarkus-resteasy-client dependency).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__MULTIPART_MAX_CHUNK_SIZE+++[] | int |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-proxy-address"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-client-proxy-address\[`quarkus.rest-client."client".proxy-address`\]</mark> config_property_copy_button:+++quarkus.rest-client."client".proxy-address+++[]<br><br>*This property is deprecated: use `quarkus.rest-client."client".proxy-configuration-name` instead.*<br><br>A string value in the form of `:` that specifies the HTTP proxy server hostname (or IP address) and port for requests of this client to use.<br><br>Use `none` to disable proxy<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__PROXY_ADDRESS+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-proxy-user"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-client-proxy-user\[`quarkus.rest-client."client".proxy-user`\]</mark> config_property_copy_button:+++quarkus.rest-client."client".proxy-user+++[]<br><br>*This property is deprecated: use `quarkus.rest-client."client".proxy-configuration-name` instead.*<br><br>Proxy username. Honored only if `quarkus.rest-client."client".proxy-address` is set.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__PROXY_USER+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-proxy-password"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-client-proxy-password\[`quarkus.rest-client."client".proxy-password`\]</mark> config_property_copy_button:+++quarkus.rest-client."client".proxy-password+++[]<br><br>*This property is deprecated: use `quarkus.rest-client."client".proxy-configuration-name` instead.*<br><br>Proxy password. Honored only if `quarkus.rest-client."client".proxy-address` is set.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__PROXY_PASSWORD+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-non-proxy-hosts"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-client-non-proxy-hosts\[`quarkus.rest-client."client".non-proxy-hosts`\]</mark> config_property_copy_button:+++quarkus.rest-client."client".non-proxy-hosts+++[]<br><br>*This property is deprecated: use `quarkus.rest-client."client".proxy-configuration-name` instead.*<br><br>Hosts to access without proxy. Honored only if `quarkus.rest-client."client".proxy-address` is set.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__NON_PROXY_HOSTS+++[] | string |  |
| <a id="quarkus-rest-client-config_quarkus-rest-client-client-proxy-connect-timeout"></a> <mark>link:#quarkus-rest-client-config_quarkus-rest-client-client-proxy-connect-timeout\[`quarkus.rest-client."client".proxy-connect-timeout`\]</mark> config_property_copy_button:+++quarkus.rest-client."client".proxy-connect-timeout+++[]<br><br>*This property is deprecated: use `quarkus.rest-client."client".proxy-configuration-name` instead.*<br><br>Proxy connection timeout. Honored only if `quarkus.rest-client."client".proxy-address` is set.<br><br>This property is not applicable to the RESTEasy Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT__CLIENT__PROXY_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-rest-client-config_quarkus-rest-client) | `10s` |

<a id="memory-size-note-anchor-quarkus-rest-client-config_quarkus-rest-client"></a>

> [!NOTE]
> **About the MemorySize format**
>
> A size configuration option recognizes strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.
>
> If no suffix is given, assume bytes.
