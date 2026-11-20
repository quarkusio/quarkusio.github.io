# Using the REST Client

This guide explains how to use the REST Client in order to interact with REST APIs. REST Client is the REST Client implementation compatible with Quarkus REST (formerly RESTEasy Reactive).

If your application uses a client and exposes REST endpoints, please use [Quarkus REST](/guides/rest/index.md) for the server part.

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

The solution is located in the `rest-client-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/rest-client-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:rest-client-quickstart \
    --extension='rest-jackson,rest-client-jackson' \
    --no-code
cd rest-client-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=rest-client-quickstart \
    -Dextensions='rest-jackson,rest-client-jackson' \
    -DnoCode
cd rest-client-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=rest-client-quickstart"`

This command generates the Maven project with a REST endpoint and imports:

- the `rest-jackson` extension for the REST server support. Use `rest` instead if you do not wish to use Jackson;
- the `rest-client-jackson` extension for the REST client support. Use `rest-client` instead if you do not wish to use Jackson

If you already have your Quarkus project configured, you can add the `rest-client-jackson` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add rest-client-jackson
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='rest-client-jackson'
```

**Gradle**

```bash
./gradlew addExtension --extensions='rest-client-jackson'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-rest-client-jackson")
```

## Setting up the model

In this guide we will be demonstrating how to consume part of the REST API supplied by the [stage.code.quarkus.io](https://stage.code.quarkus.io) service. Our first order of business is to set up the model we will be using, in the form of a `Extension` POJO.

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

Using the REST Client is as simple as creating an interface using the proper Jakarta REST and MicroProfile annotations. In our case the interface should be created at `src/main/java/org/acme/rest/client/ExtensionsService.java` and have the following content:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);
}
```

The `getById` method gives our code the ability to get an extension by id from the Code Quarkus API. The client will handle all the networking and marshalling leaving our code clean of such technical details.

The purpose of the annotations in the code above is the following:

- `@RegisterRestClient` allows Quarkus to know that this interface is meant to be available for CDI injection as a REST Client
- `@Path`, `@GET` and `@QueryParam` are the standard Jakarta REST annotations used to define how to access the service

> [!NOTE]
> When the `quarkus-rest-client-jackson` extension is installed, Quarkus will use the `application/json` media type by default for most return values, unless the media type is explicitly set via `@Produces` or `@Consumes` annotations.
>
> If you don't rely on the JSON default, it is heavily recommended to annotate your endpoints with the `@Produces` and `@Consumes` annotations to define precisely the expected content-types. It will allow to narrow down the number of Jakarta REST providers (which can be seen as converters) included in the native executable.

> [!WARNING]
> The `getById` method above is a blocking call. It should not be invoked on the event loop. The [Async Support](#async-support) section describes how to make non-blocking calls.

### Query Parameters

The easiest way to specify a query parameter is to annotate a client method parameter with the `@QueryParam` or the `@RestQuery`. The `@RestQuery` is equivalent of the `@QueryParam`, but with optional name. Additionally, it can be also used to pass query parameters as a `Map`, which is convenient if parameters are not known in advance.

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

@Path("/extensions")
@RegisterRestClient(configKey = "extensions-api")
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @GET
    Set<Extension> getByName(@RestQuery String name); <1>

    @GET
    Set<Extension> getByOptionalName(@RestQuery Optional<String> name);

    @GET
    Set<Extension> getByFilter(@RestQuery Map<String, String> filter); <2>

    @GET
    Set<Extension> getByFilters(@RestQuery MultivaluedMap<String, String> filters); <3>

}
```

1. @RestQuery will include parameter with key `name`
2. Each `Map` entry represents exactly one query parameter
3. `MultivaluedMap` allows you to send array values

#### Using @ClientQueryParam

Another way to add query parameters to a request is to use `@io.quarkus.rest.client.reactive.ClientQueryParam` on either the REST client interface or a specific method of the interface. The annotation can specify the query parameter name while the value can either be a constant, a configuration property or it can be determined by invoking a method.

The following example shows the various possible usages:

```java
@ClientQueryParam(name = "my-param", value = "${my.property-value}") // <1>
public interface Client {
    @GET
    String getWithParam();

    @GET
    @ClientQueryParam(name = "some-other-param", value = "other") // <2>
    String getWithOtherParam();

    @GET
    @ClientQueryParam(name = "param-from-method", value = "{with-param}") // <3>
    String getFromMethod();

    default String withParam(String name) {
        if ("param-from-method".equals(name)) {
            return "test";
        }
        throw new IllegalArgumentException();
    }
}
```

1. By placing `@ClientQueryParam` on the interface, we ensure that `my-param` will be added to all requests of the client. Because we used the `${...}` syntax, the actual value of the parameter will be obtained using the `my.property-value` configuration property.
2. When `getWithOtherParam` is called, in addition to the `my-param` query parameter, `some-other-param` with the value of `other` will also be added.
3. when `getFromMethod` is called, in addition to the `my-param` query parameter, `param-from-method` with the value of `test` (because that's what the `withParam` method returns when invoked with `param-from-method`) will also be added.

> [!IMPORTANT]
> Note that if an interface method contains an argument annotated with `@QueryParam`, that argument will take priority over anything specified in any `@ClientQueryParam` annotation.

More information about this annotation can be found on the javadoc of [`@ClientQueryParam`](https://javadoc.io/doc/io.quarkus/quarkus-rest-client/latest/io/quarkus/rest/client/reactive/ClientQueryParam.html).

### Form Parameters

Form parameters can be specified using `@RestForm` (or `@FormParam`) annotations:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestForm;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Map;
import java.util.Set;

@Path("/extensions")
@RegisterRestClient(configKey = "extensions-api")
public interface ExtensionsService {

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postId(@FormParam("id") String id);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postName(@RestForm String name);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postFilter(@RestForm Map<String, String> filter);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Set<Extension> postFilters(@RestForm MultivaluedMap<String, String> filters);

}
```

#### Using @ClientFormParam

Form parameters can also be specified using `@ClientFormParam`, similar to `@ClientQueryParam`:

```java
@ClientFormParam(name = "my-param", value = "${my.property-value}")
public interface Client {
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    String postWithParam();

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ClientFormParam(name = "some-other-param", value = "other")
    String postWithOtherParam();

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ClientFormParam(name = "param-from-method", value = "{with-param}")
    String postFromMethod();

    default String withParam(String name) {
        if ("param-from-method".equals(name)) {
            return "test";
        }
        throw new IllegalArgumentException();
    }
}
```

More information about this annotation can be found on the javadoc of [`@ClientFormParam`](https://javadoc.io/doc/io.quarkus/quarkus-rest-client/latest/io/quarkus/rest/client/reactive/ClientFormParam.html).

### Path Parameters

If the GET request requires path parameters you can leverage the `@PathParam("parameter-name")` annotation instead of (or in addition to) the `@QueryParam`. Path and query parameters can be combined, as required, as illustrated in the example below.

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    @Path("/stream/{stream}")
    Set<Extension> getByStream(@PathParam("stream") String stream, @QueryParam("id") String id);
}
```

### Matrix Parameters

Matrix parameters can be specified with `@MatrixParam` or `@RestMatrix`. The `@RestMatrix` annotation is equivalent to `@MatrixParam`, but the name is optional — when omitted, the parameter name is used.

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestMatrix;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.Path;

@Path("/greet")
@RegisterRestClient
public interface GreetingService {

    @GET
    String greet(@MatrixParam("name") String name);

    @GET
    String greetRest(@RestMatrix String name);
}
```

Calling `greet("world")` sends a request to `/greet;name=world`.

### Dynamic base URLs

The REST client allows for a per invocation override of the base URL using the `io.quarkus.rest.client.reactive.Url` annotation.

Here is a simple example:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

import io.quarkus.rest.client.reactive.Url;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    @Path("/stream/{stream}")
    Set<Extension> getByStream(@Url String url, @PathParam("stream") String stream, @QueryParam("id") String id);
}
```

When the `url` parameter is non-null, it will override the base URL that is configured for the client (the default base URL configuration is still mandatory).

### Sending large payloads

The REST Client is capable of sending arbitrarily large HTTP bodies without buffering the contents in memory, if one of the following types is used:

- `InputStream`
- `Multi<io.vertx.mutiny.core.buffer.Buffer>`

Furthermore, the client can also send arbitrarily large files if one of the following types is used:

- `File`
- `Path`

### Getting other response properties

#### Using RestResponse

If you need to get more properties of the HTTP response than just the body, such as the status code or headers, you can make your method return `org.jboss.resteasy.reactive.RestResponse` from a method. An example of this could look like:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.Set;

@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    RestResponse<Set<Extension>> getByIdResponseProperties(@RestQuery String id);
}
```

> [!CAUTION]
> When configuring the REST client to use `RestResponse` as the return type, you need to disable the [Quarkus REST client default exception mapper](/guides/rest-client/index.md#disabling-the-default-mapper), so that no `WebApplicationException` will be thrown when the response status code is not in the successful range (2xx).

> [!NOTE]
> You can also use the Jakarta REST type [`Response`](https://javadoc.io/doc/jakarta.ws.rs/jakarta.ws.rs-api/3.1.0/jakarta.ws.rs/jakarta/ws/rs/core/Response.html) but it is not strongly typed to your entity.

## Create the Jakarta REST resource

Create the `src/main/java/org/acme/rest/client/ExtensionsResource.java` file with the following content:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Set;

@Path("/extension")
public class ExtensionsResource {

    @RestClient // <1>
    ExtensionsService extensionsService;


    @GET
    @Path("/id/{id}")
    public Set<Extension> id(String id) {
        return extensionsService.getById(id);
    }

    @GET
    @Path("/properties")
    public RestResponse<Set<Extension>> responseProperties(@RestQuery String id) {
        RestResponse<Set<Extension>> clientResponse = extensionsService.getByIdResponseProperties(id); // <2>
        String contentType = clientResponse.getHeaderString("Content-Type");
        int status = clientResponse.getStatus();
        String setCookie = clientResponse.getHeaderString("Set-Cookie");
        Date lastModified = clientResponse.getLastModified();

        Log.infof("content-Type: %s status: %s Last-Modified: %s Set-Cookie: %s", contentType, status, lastModified,
                setCookie);

        return RestResponse.fromResponse(clientResponse);
    }
}
```

1. the client stub is injected with the `@RestClient` annotation instead of the usual CDI `@Inject`
2. `org.jboss.resteasy.reactive.RestResponse` used as effective way of getting response properties via RestResponse directly from RestClient, as described in [\[Using RestResponse\]](<#Using RestResponse>)

## Create the configuration

In order to determine the base URL to which REST calls will be made, the REST Client uses configuration from `application.properties`. The name of the property needs to follow a certain convention which is best displayed in the following code:

```properties
# Your configuration properties
quarkus.rest-client."org.acme.rest.client.ExtensionsService".url=https://stage.code.quarkus.io/api // <1>
```

1. Having this configuration means that all requests performed using `org.acme.rest.client.ExtensionsService` will use `https://stage.code.quarkus.io/api` as the base URL. Using the configuration above, calling the `getById` method of `ExtensionsService` with a value of `io.quarkus:quarkus-rest-client` would result in an HTTP GET request being made to `https://stage.code.quarkus.io/api/extensions?id=io.quarkus:quarkus-rest-client`.

Note that `org.acme.rest.client.ExtensionsService` *must* match the fully qualified name of the `ExtensionsService` interface we created in the previous section.

To facilitate the configuration, you can use the `@RegisterRestClient` `configKey` property that allows to use different configuration root than the fully qualified name of your interface.

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

> [!IMPORTANT]
> Setting the base URL of the client is **mandatory**, however the REST Client supports per-invocation overrides of the base URL using the `@io.quarkus.rest.client.reactive.Url` annotation.

### Trusting all certificates and Disabling SSL hostname verification

> [!WARNING]
> This properties set should not be used in production.

You can configure TLS connection of specific REST client to trust all certificates and disable the hostname verification using tls extension. First of all, you should configure tls configuration bucket.

To trust all certificates:

```properties
quarkus.tls.tls-disabled.trust-all=true
```

To disable SSL hostname verification:

```properties
quarkus.tls.tls-disabled.hostname-verification-algorithm=NONE
```

Finally, lets configure our REST client with apropriate tls configuration name:

```properties
quarkus.rest-client.extensions-api.tls-configuration-name=tls-disabled
```

### HTTP/2 Support

HTTP/2 is disabled by default in REST Client. If you want to enable it, you can set:

```properties
// for all REST Clients:
quarkus.rest-client.http2=true
// or for a single REST Client:
quarkus.rest-client.extensions-api.http2=true
```

Alternatively, you can enable the Application-Layer Protocol Negotiation (alpn) TLS extension and the client will negotiate which HTTP version to use over the ones compatible by the server. By default, it will try to use HTTP/2 first and if it's not enabled, it will use HTTP/1.1. If you want to enable it, you can set:

```properties
quarkus.rest-client.alpn=true
// or for a single REST Client:
quarkus.rest-client.extensions-api.alpn=true
```

## Programmatic client creation with QuarkusRestClientBuilder

Instead of annotating the client with `@RegisterRestClient`, and injecting a client with `@RestClient`, you can also create REST Client programmatically. You do that with the `QuarkusRestClientBuilder`.

With this approach the client interface could look as follows:

```java
package org.acme.rest.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

@Path("/extensions")
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);
}
```

And the service as follows:

```java
package org.acme.rest.client;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.net.URI;
import java.util.Set;

@Path("/extension")
public class ExtensionsResource {

    private final ExtensionsService extensionsService;

    public ExtensionsResource() {
        extensionsService = QuarkusRestClientBuilder.newBuilder()
            .baseUri(URI.create("https://stage.code.quarkus.io/api"))
            .build(ExtensionsService.class);
    }

    @GET
    @Path("/id/{id}")
    public Set<Extension> id(String id) {
        return extensionsService.getById(id);
    }
}
```

> [!TIP]
> The `QuarkusRestClientBuilder` interface is a Quarkus-specific API to programmatically create clients with additional configuration options. Otherwise, you can also use the `RestClientBuilder` interface from the Microprofile API:
>
> ```java
> package org.acme.rest.client;
>
> import org.eclipse.microprofile.rest.client.RestClientBuilder;
>
> import jakarta.ws.rs.GET;
> import jakarta.ws.rs.Path;
> import java.net.URI;
> import java.util.Set;
>
> @Path("/extension")
> public class ExtensionsResource {
>
>     private final ExtensionsService extensionsService;
>
>     public ExtensionsResource() {
>         extensionsService = RestClientBuilder.newBuilder()
>             .baseUri(URI.create("https://stage.code.quarkus.io/api"))
>             .build(ExtensionsService.class);
>     }
>
>     // ...
> }
> ```

## Use Custom HTTP Options

The REST Client internally uses [the Vert.x HTTP Client](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClient.html) to make the network connections. The REST Client extensions allows configuring some settings via properties, for example:

- `quarkus.rest-client.client-prefix.connect-timeout` to configure the connect timeout in milliseconds.
- `quarkus.rest-client.client-prefix.max-redirects` to limit the number of redirects.

However, there are many more options within the Vert.x HTTP Client to configure the connections. See all the options in the Vert.x HTTP Client Options API in [this link](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpClientOptions.html).

To fully customize the Vert.x HTTP Client instance that the REST Client is internally using, you can provide your custom HTTP Client Options instance via CDI or when programmatically creating your client.

Let's see an example about how to provide the HTTP Client Options via CDI:

```java
package org.acme.rest.client;

import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.ext.ContextResolver;

import io.vertx.core.http.HttpClientOptions;
import io.quarkus.arc.Unremovable;

@Provider
public class CustomHttpClientOptions implements ContextResolver<HttpClientOptions> {

    @Override
    public HttpClientOptions getContext(Class<?> aClass) {
        HttpClientOptions options = new HttpClientOptions();
        // ...
        return options;
    }
}
```

Now, all the REST Clients will be using your custom HTTP Client Options.

Another approach is to provide the custom HTTP Client options when creating the client programmatically:

```java
package org.acme.rest.client;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.net.URI;
import java.util.Set;

import io.vertx.core.http.HttpClientOptions;

@Path("/extension")
public class ExtensionsResource {

    private final ExtensionsService extensionsService;

    public ExtensionsResource() {
        HttpClientOptions options = new HttpClientOptions();
        // ...

        extensionsService = QuarkusRestClientBuilder.newBuilder()
            .baseUri(URI.create("https://stage.code.quarkus.io/api"))
            .httpClientOptions(options) <1>
            .build(ExtensionsService.class);
    }

    // ...
}
```

1. the client will use the registered HTTP Client options over the HTTP Client options provided via CDI if any.

<a id="redirection"></a>
## Redirection

A HTTP server can redirect a response to another location by sending a response with a status code that starts with "3" and a HTTP header "Location" holding the URL to be redirected to. When the REST Client receives a redirection response from a HTTP server, it won't automatically perform another request to the new location. We can enable the automatic redirection in REST Client by adding the "follow-redirects" property:

- `quarkus.rest-client.follow-redirects` to enable redirection for all REST clients.
- `quarkus.rest-client.<client-prefix>.follow-redirects` to enable redirection for a specific REST client.

If this property is true, then REST Client will perform a new request that it receives a redirection response from the HTTP server.

Additionally, we can limit the number of redirections using the property "max-redirects".

One important note is that according to the [RFC2616](https://www.rfc-editor.org/rfc/rfc2616#section-10.3.8) specs, by default the redirection will only happen for GET or HEAD methods. However, in REST Client, you can provide your custom redirect handler to enable redirection on POST or PUT methods, or to follow a more complex logic, via either using the `@ClientRedirectHandler` annotation, CDI or programmatically when creating your client.

Let's see an example about how to register your own custom redirect handler using the `@ClientRedirectHandler` annotation:

```java
import jakarta.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.ClientRedirectHandler;

@RegisterRestClient(configKey="extensions-api")
public interface ExtensionsService {
    @ClientRedirectHandler
    static URI alwaysRedirect(Response response) {
        if (Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.REDIRECTION) {
            return response.getLocation();
        }

        return null;
    }
}
```

The "alwaysRedirect" redirect handler will only be used by the specified REST Client which in this example is the "ExtensionsService" client.

Alternatively, you can also provide a custom redirect handler for all your REST Clients via CDI:

```java
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.handlers.RedirectHandler;

@Provider
public class AlwaysRedirectHandler implements ContextResolver<RedirectHandler> {

    @Override
    public RedirectHandler getContext(Class<?> aClass) {
        return response -> {
            if (Response.Status.Family.familyOf(response.getStatus()) == Response.Status.Family.REDIRECTION) {
                return response.getLocation();
            }
            // no redirect
            return null;
        };
    }
}
```

Now, all the REST Clients will be using your custom redirect handler.

Another approach is to provide it programmatically when creating the client:

```java
@Path("/extension")
public class ExtensionsResource {

    private final ExtensionsService extensionsService;

    public ExtensionsResource() {
        extensionsService = QuarkusRestClientBuilder.newBuilder()
            .baseUri(URI.create("https://stage.code.quarkus.io/api"))
            .register(AlwaysRedirectHandler.class) <1>
            .build(ExtensionsService.class);
    }

    // ...
}
```

1. the client will use the registered redirect handler over the redirect handler provided via CDI if any.

## Update the test

Next, we need to update the functional test to reflect the changes made to the endpoint. Edit the `src/test/java/org/acme/rest/client/ExtensionsResourceTest.java` file and change the content of the test to:

```java
package org.acme.rest.client;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
public class ExtensionsResourceTest {

    @Test
    public void testExtensionsIdEndpoint() {
        given()
            .when().get("/extension/id/io.quarkus:quarkus-rest-client")
            .then()
            .statusCode(200)
            .body("$.size()", is(1),
                "[0].id", is("io.quarkus:quarkus-rest-client"),
                "[0].name", is("REST Client"),
                "[0].keywords.size()", greaterThan(1),
                "[0].keywords", hasItem("rest-client"));
    }
}
```

The code above uses [REST Assured](https://rest-assured.io/)'s [json-path](https://github.com/rest-assured/rest-assured/wiki/GettingStarted#jsonpath) capabilities.

<a id="async-support"></a>
## Async Support

To get the full power of the reactive nature of the client, you can use the non-blocking flavor of REST Client extension, which comes with support for `CompletionStage` and `Uni`. Let's see it in action by adding a `getByIdAsync` method in our `ExtensionsService` REST interface. The code should look like:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;
import java.util.concurrent.CompletionStage;

@Path("/extensions")
@RegisterRestClient(configKey = "extensions-api")
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam("id") String id);
}
```

Open the `src/main/java/org/acme/rest/client/ExtensionsResource.java` file and update it with the following content:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Set;
import java.util.concurrent.CompletionStage;

@Path("/extension")
public class ExtensionsResource {

    @RestClient
    ExtensionsService extensionsService;


    @GET
    @Path("/id/{id}")
    public Set<Extension> id(String id) {
        return extensionsService.getById(id);
    }

    @GET
    @Path("/id-async/{id}")
    public CompletionStage<Set<Extension>> idAsync(String id) {
        return extensionsService.getByIdAsync(id);
    }
}
```

Please note that since the invocation is now non-blocking, the `idAsync` method will be invoked on the event loop, i.e. will not get offloaded to a worker pool thread and thus reducing hardware resource utilization. See [Quarkus REST execution model](/guides/rest/index.md#execution-model) for more details.

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
            "[0].name", is("REST Client"),
            "[0].keywords.size()", greaterThan(1),
            "[0].keywords", hasItem("rest-client"));
}
```

The `Uni` version is very similar:

```java
package org.acme.rest.client;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;

@Path("/extensions")
@RegisterRestClient(configKey = "extensions-api")
public interface ExtensionsService {

    // ...

    @GET
    Uni<Set<Extension>> getByIdAsUni(@QueryParam("id") String id);
}
```

The `ExtensionsResource` becomes:

```java
package org.acme.rest.client;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Set;

@Path("/extension")
public class ExtensionsResource {

    @RestClient
    ExtensionsService extensionsService;


    // ...

    @GET
    @Path("/id-uni/{id}")
    public Uni<Set<Extension>> idUni(String id) {
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

@RestClient ExtensionsService extensionsService;

// ...

extensionsService.getByIdAsUni(id)
    .onFailure().retry().atMost(10);
```

If you use a `CompletionStage`, you would need to call the service's method to retry. This difference comes from the laziness aspect of Mutiny and its subscription protocol. More details about this can be found in [the Mutiny documentation](https://smallrye.io/smallrye-mutiny/latest/reference/uni-and-multi/).

### Server-Sent Event (SSE) support

Consuming SSE events is possible simply by declaring the result type as a `io.smallrye.mutiny.Multi`.

The simplest example is:

```java
package org.acme.rest.client;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/sse")
@RegisterRestClient(configKey = "some-api")
public interface SseClient {
     @GET
     @Produces(MediaType.SERVER_SENT_EVENTS)
     Multi<String> get();
}
```

> [!NOTE]
> All the IO involved in streaming the SSE results is done in a non-blocking manner.

Results are not limited to strings - for example when the server returns JSON payload for each event, Quarkus automatically deserializes it into the generic type used in the `Multi`.

> [!TIP]
> Users can also access the entire SSE event by using the `org.jboss.resteasy.reactive.client.SseEvent` type.
>
> A simple example where the event payloads are `Long` values is the following:
>
> ```java
> package org.acme.rest.client;
>
> import io.smallrye.mutiny.Uni;
> import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
> import org.jboss.resteasy.reactive.client.SseEvent;
>
> import jakarta.ws.rs.GET;
> import jakarta.ws.rs.Path;
> import jakarta.ws.rs.QueryParam;
>
> @Path("/sse")
> @RegisterRestClient(configKey = "some-api")
> public interface SseClient {
>      @GET
>      @Produces(MediaType.SERVER_SENT_EVENTS)
>      Multi<SseEvent<Long>> get();
> }
> ```

#### Accessing response metadata from a stream

When consuming a streaming response (SSE, newline-delimited JSON, or chunked), you may need access to the HTTP status code or response headers alongside the streamed items. Using `RestMultiResponse<T>` instead of `Multi<T>` as the return type gives you non-blocking access to this metadata via the `response()` method, which returns a `Uni<BasicRestResponse>`.

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.BasicRestResponse;
import org.jboss.resteasy.reactive.client.RestMultiResponse;
import org.jboss.resteasy.reactive.client.SseEvent;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/sse")
@RegisterRestClient(configKey = "some-api")
public interface SseClient {
     @GET
     @Produces(MediaType.SERVER_SENT_EVENTS)
     RestMultiResponse<SseEvent<Long>> get();
}
```

The `BasicRestResponse` is available as soon as the HTTP response headers arrive, before any stream items are emitted:

```java
RestMultiResponse<SseEvent<Long>> stream = sseClient.get();

// non-blocking access to response metadata
stream.response().subscribe().with(response -> {
    int status = response.status();
    String correlationId = response.headers().getFirst("X-Correlation-Id");
});

// consume the stream items
stream.subscribe().with(event -> {
    // process each event
});
```

`RestMultiResponse<T>` extends `Multi<T>`, so it can be used anywhere a `Multi` is expected. It works with any streaming media type, not just SSE.

#### Filtering out events

On occasion, the stream of SSE events may contain some events that should not be returned by the client - an example of this is having the server send heartbeat events in order to keep the underlying TCP connection open. The REST Client supports filtering out such events by providing the `@org.jboss.resteasy.reactive.client.SseEventFilter`.

Here is an example of filtering out heartbeat events:

```java
package org.acme.rest.client;

import io.smallrye.mutiny.Uni;
import java.util.function.Predicate;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/sse")
@RegisterRestClient(configKey = "some-api")
public interface SseClient {

     @GET
     @Produces(MediaType.SERVER_SENT_EVENTS)
     @SseEventFilter(HeartbeatFilter.class)
     Multi<SseEvent<Long>> get();


     class HeartbeatFilter implements Predicate<SseEvent<String>> {

        @Override
        public boolean test(SseEvent<String> event) {
            return !"heartbeat".equals(event.id());
        }
     }
}
```

## Custom headers support

There are a few ways in which you can specify custom headers for your REST calls:

- by registering a `ClientHeadersFactory` or a `ReactiveClientHeadersFactory` with the `@RegisterClientHeaders` annotation
- by programmatically registering a `ClientHeadersFactory` or a `ReactiveClientHeadersFactory` with the `QuarkusRestClientBuilder.clientHeadersFactory(factory)` method
- by specifying the value of the header with `@ClientHeaderParam`
- by specifying the value of the header by `@HeaderParam`

The code below demonstrates how to use each of these techniques:

```java
package org.acme.rest.client;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.Set;
import io.quarkus.rest.client.reactive.NotBody;

@Path("/extensions")
@RegisterRestClient
@RegisterClientHeaders(RequestUUIDHeaderFactory.class) // <1>
@ClientHeaderParam(name = "my-header", value = "constant-header-value") // <2>
@ClientHeaderParam(name = "computed-header", value = "{org.acme.rest.client.Util.computeHeader}") // <3>
public interface ExtensionsService {

    @GET
    @ClientHeaderParam(name = "header-from-properties", value = "${header.value}") // <4>
    @ClientHeaderParam(name = "header-from-method-param", value = "Bearer {token}") // <5>
    Set<Extension> getById(@QueryParam("id") String id, @HeaderParam("jaxrs-style-header") String headerValue, @NotBody String token); // <6>
}
```

1. There can be only one `ClientHeadersFactory` per class. With it, you can not only add custom headers, but you can also transform existing ones. See the `RequestUUIDHeaderFactory` class below for an example of the factory.
2. `@ClientHeaderParam` can be used on the client interface and on methods. It can specify a constant header value...
3. ... and a name of a method that should compute the value of the header. It can either be a static method or a default method in this interface. The method can take either no parameters, a single String parameter or a single `io.quarkus.rest.client.reactive.ComputedParamContext` parameter (which is very useful for code that needs to compute headers based on method parameters and naturally complements `@io.quarkus.rest.client.reactive.NotBody`).
4. ... as well as a value from your application's configuration
5. ... or even any mixture of verbatim text, method parameters (referenced by name), a configuration value (as mentioned previously) and method invocations (as mentioned before)
6. ... or as a normal Jakarta REST `@HeaderParam` annotated argument

> [!NOTE]
> When using Kotlin, if default methods are going to be leveraged, then the Kotlin compiler needs to be configured to use Java's default interface capabilities. See [this](https://kotlinlang.org/docs/java-to-kotlin-interop.html#default-methods-in-interfaces) for more details.

A `ClientHeadersFactory` can look as follows:

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

To specify a value for `${header.value}`, simply put the following in your `application.properties`:

```properties
header.value=value of the header
```

Also, there is a reactive flavor of `ClientHeadersFactory` that allows doing blocking operations. For example:

```java
package org.acme.rest.client;

import io.smallrye.mutiny.Uni;

import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.UUID;

@ApplicationScoped
public class GetTokenReactiveClientHeadersFactory extends ReactiveClientHeadersFactory {

    @Inject
    Service service;

    @Override
    public Uni<MultivaluedMap<String, String>> getHeaders(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        return Uni.createFrom().item(() -> {
            MultivaluedHashMap<String, String> newHeaders = new MultivaluedHashMap<>();
            // perform blocking call
            newHeaders.add(HEADER_NAME, service.getToken());
            return newHeaders;
        });
    }
}
```

> [!TIP]
> When using HTTP Basic Auth, the `@io.quarkus.rest.client.reactive.ClientBasicAuth` annotation provides a much simpler way of configuring the necessary `Authorization` header.
>
> A very simple example is:
>
> ```java
> @ClientBasicAuth(username = "${service.username}", password = "${service.password}")
> public interface SomeClient {
>
> }
> ```
>
> where `service.username` and `service.password` are configuration properties that must be set at runtime to the username and password that allow access to the service being called.

### Default header factory

The `@RegisterClientHeaders` annotation can also be used without any custom factory specified. In that case the `DefaultClientHeadersFactoryImpl` factory will be used. If you make a REST client call from a REST resource, this factory will propagate all the headers listed in `org.eclipse.microprofile.rest.client.propagateHeaders` configuration property from the resource request to the client request. Individual header names are comma-separated.

```java
@Path("/extensions")
@RegisterRestClient
@RegisterClientHeaders
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam("id") String id);
}
```

```properties
org.eclipse.microprofile.rest.client.propagateHeaders=Authorization,Proxy-Authorization
```

## Multiple `@Consumes` media types

Some REST Client interface methods may have a `@Consumes` annotation with multiple media type values, for example:

```java
@Path("/orders")
@RegisterRestClient
public interface OrderService {

    record OrderReference(String orderReference) {
    };

    @POST
    @Consumes({"application/json", "application/yaml"}) <1>
    Response addOrderReference(OrderReference orderReference);
}
```

1. REST server accepts OrderReference representations in either JSON or YAML formats.

REST Client must set a `Content-Type` for the `POST /orders` requests. When it sees `@Consumes` with multiple media type values, it sorts them and chooses the first media type as a `Content-Type` value. It must not impact the client code that makes calls such as `OrderService#addOrderReference` as all the listed `@Consumes` values are expected to be supported by the REST server.

Register a custom `ClientRequestFiler` as explained in the [Customizing the request](#customize-rest-client-request) section if you prefer to set a different `Content-Type` such as `application/yaml` which is also supported according to the `OrderService` interface definition:

```java
@Provider
public class RestClientContentTypeRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext rc) throws IOException {
        String contentType = rc.getHeaderString("Content-Type"));
        if ("application/json".equals(contentType)) {
            rc.getHeaders().putSingle("Content-Type", "application/yaml");
        }
    }

}
```

<a id="customize-rest-client-request"></a>
## Customizing the request

The REST Client supports further customization of the final request to be sent to the server via filters. The filters must implement either the interface `ClientRequestFilter` or `ResteasyReactiveClientRequestFilter`.

A simple example of customizing the request would be to add a custom header:

```java
@Provider
public class TestClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().add("my_header", "value");
    }
}
```

Next, you can register your filter using the `@RegisterProvider` annotation:

```java
@Path("/extensions")
@RegisterProvider(TestClientRequestFilter.class)
public interface ExtensionsService {

    // ...
}
```

Or programmatically using the `.register()` method:

```java
QuarkusRestClientBuilder.newBuilder()
    .register(TestClientRequestFilter.class)
    .build(ExtensionsService.class)
```

### Injecting the `jakarta.ws.rs.ext.Providers` instance in filters

The `jakarta.ws.rs.ext.Providers` is useful when we need to lookup the provider instances of the current client.

We can get the `Providers` instance in our filters from the request context as follows:

```java
@Provider
public class TestClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        Providers providers = ((ResteasyReactiveClientRequestContext) requestContext).getProviders();
        // ...
    }
}
```

Alternatively, you can implement the `ResteasyReactiveClientRequestFilter` interface instead of the `ClientRequestFilter` interface that will directly provide the `ResteasyReactiveClientRequestContext` context:

```java
@Provider
public class TestClientRequestFilter implements ResteasyReactiveClientRequestFilter {

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        Providers providers = requestContext.getProviders();
        // ...
    }
}
```

## Jackson-specific features

### Customizing the ObjectMapper in REST Client Jackson

The REST Client supports adding a custom ObjectMapper to be used only the Client using the annotation `@ClientObjectMapper`.

A simple example is to provide a custom ObjectMapper to the REST Client Jackson extension by doing:

```java
@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @ClientObjectMapper <1>
    static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) { <2>
        return defaultObjectMapper.copy() <3>
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
    }
}
```

1. The method must be annotated with `@ClientObjectMapper`.
2. It's must be a static method. Also, the parameter `defaultObjectMapper` will be resolved via CDI. If not found, it will throw an exception at runtime.
3. In this example, we're creating a copy of the default object mapper. You should **NEVER** modify the default object mapper, but create a copy instead.

### @JsonView support

Jakarta REST methods can be annotated with [@JsonView](https://fasterxml.github.io/jackson-annotations/javadoc/2.10/com/fasterxml/jackson/annotation/JsonView.html) in order to customize the serialization of the returned POJO, on a per method-basis. This is best explained with an example.

A typical use of `@JsonView` is to hide certain fields on certain methods. In that vein, let's define two views:

```java
public class Views {

    public static class Public {
    }

    public static class Private extends Public {
    }
}
```

Let's assume we have the `User` POJO on which we want to hide some field during serialization. A simple example of this is:

```java
public class User {

    @JsonView(Views.Private.class)
    public int id;

    @JsonView(Views.Public.class)
    public String name;
}
```

The REST Client supports `@JsonView` both for sending content to the REST API and for retrieving data from it:

```java
    @Path("/users")
    @RegisterRestClient
    public interface UserClient {
        @GET
        @Path("/{id}")
        @Produces(MediaType.APPLICATION_JSON)
        @JsonView(Views.Public.class)
        User get(@RestPath String id);

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        Response create(@JsonView(Views.Public.class) User user);
    }
```

In the preceding code, the `get` method would return a `User` whose `id` is always `null` while the `create` method would never include `id` in the JSON it sends to the REST API.

## Exception handling

The MicroProfile REST Client specification introduces the `org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper` whose purpose is to convert an HTTP response to an exception.

A simple example of implementing such a `ResponseExceptionMapper` for the `ExtensionsService` discussed above, could be:

```java
public class MyResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public RuntimeException toThrowable(Response response) {
        if (response.getStatus() == 500) {
            return new RuntimeException("The remote service responded with HTTP 500");
        }
        return null;
    }
}
```

`ResponseExceptionMapper` also defines the `getPriority` method which is used in order to determine the priority with which `ResponseExceptionMapper` implementations will be called (implementations with a lower value for `getPriority` will be invoked first). If `toThrowable` returns an exception, then that exception will be thrown. If `null` is returned, the next implementation of `ResponseExceptionMapper` in the chain will be called (if there is any).

The class as written above, would not be automatically used by any REST Client. To make it available to every REST Client of the application, the class needs to be annotated with `@Provider` (as long as `quarkus.rest-client.provider-autodiscovery` is not set to `false`). Alternatively, if the exception handling class should only apply to specific REST Client interfaces, you can either annotate the interfaces with `@RegisterProvider(MyResponseExceptionMapper.class)`, or register it using configuration using the `providers` property of the proper `quarkus.rest-client` configuration group.

### Using @ClientExceptionMapper

A simpler way to convert HTTP response codes of 400 or above is to use the `@ClientExceptionMapper` annotation.

For the `ExtensionsService` REST Client interface defined above, an example use of `@ClientExceptionMapper` would be:

```java
@Path("/extensions")
@RegisterRestClient
public interface ExtensionsService {

    @GET
    Set<Extension> getById(@QueryParam("id") String id);

    @GET
    CompletionStage<Set<Extension>> getByIdAsync(@QueryParam("id") String id);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 500) {
            return new RuntimeException("The remote service responded with HTTP 500");
        }
        return null;
    }
}
```

Naturally this handling is per REST Client. `@ClientExceptionMapper` uses the default priority if the `priority` attribute is not set and the normal rules of invoking all handlers in turn apply.

> [!NOTE]
> Methods annotated with `@ClientExceptionMapper` can also take a `java.lang.reflect.Method` parameter which is useful if the exception mapping code needs to know the REST Client method that was invoked and caused the exception mapping code to engage.

### Using @Blocking annotation in exception mappers

In cases that warrant using `InputStream` as the return type of REST Client method (such as when large amounts of data need to be read):

```java
@Path("/echo")
@RegisterRestClient
public interface EchoClient {

    @GET
    InputStream get();
}
```

This will work as expected, but if you try to read this InputStream object in a custom exception mapper, you will receive a `BlockingNotAllowedException` exception. This is because `ResponseExceptionMapper` classes are run on the Event Loop thread executor by default - which does not allow to perform IO operations.

To make your exception mapper blocking, you can annotate the exception mapper with the `@Blocking` annotation:

```java
@Provider
@Blocking <1>
public class MyResponseExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public RuntimeException toThrowable(Response response) {
        if (response.getStatus() == 500) {
            response.readEntity(String.class); <2>
            return new RuntimeException("The remote service responded with HTTP 500");
        }
        return null;
    }
}
```

1. With the `@Blocking` annotation, the MyResponseExceptionMapper exception mapper will be executed in the worker thread pool.
2. Reading the entity is now allowed because we're executing the mapper on the worker thread pool.

Note that you can also use the `@Blocking` annotation when using @ClientExceptionMapper:

```java
@Path("/echo")
@RegisterRestClient
public interface EchoClient {

    @GET
    InputStream get();

    @ClientExceptionMapper
    @Blocking
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 500) {
            response.readEntity(String.class);
            return new RuntimeException("The remote service responded with HTTP 500");
        }
        return null;
    }
}
```

### Disabling the default mapper

As mandated by the REST Client specification, a default exception mapper is included, that throws an exception when HTTP status code is higher than 400. While this behavior is fine when the client returns a regular object, it is however very unintuitive when the client needs to return a `jakarta.ws.rs.core.Response` (with the intention of allowing the caller to decide how to handle the HTTP status code).

For this reason, the REST Client includes a property named `disable-default-mapper` which can be used to disable the default mapper when using a REST client in a declarative manner.

For example, with a client like so:

```java
    @Path("foo")
    @RegisterRestClient(configKey = "bar")
    public interface Client {
        @GET
        Response get();
    }
```

The default exception mapper can be disabled by setting `quarkus.rest-client.bar.disable-default-mapper=true` to disable the exception mapper for the REST Client configured with the key `bar`.

> [!NOTE]
> When using the programmatic approach for creating a REST Client, `QuarkusRestClientBuilder` provides a method named `disableDefaultMapper` that provides the same feature.

<a id="multipart"></a>
## Multipart Form support

### Sending Multipart messages

REST Client allows sending data as multipart forms. This way you can for example send files efficiently.

To send data as a multipart form, you can just use the regular `@RestForm` (or `@FormParam`) annotations:

```java
    @POST
    @Path("/binary")
    String sendMultipart(@RestForm File file, @RestForm String otherField);
```

Parameters specified as `File`, `Path`, `byte[]`, `Buffer` or `FileUpload` are sent as files and default to the `application/octet-stream` MIME type. Other `@RestForm` parameter types default to the `text/plain` MIME type. You can override these defaults with the `@PartType` annotation.

Naturally, you can also group these parameters into a containing class:

```java
    public static class Parameters {
        @RestForm
        File file;

        @RestForm
        String otherField;
    }

    @POST
    @Path("/binary")
    String sendMultipart(Parameters parameters);
```

Any `@RestForm` parameter of the type `File`, `Path`, `byte[]`, `Buffer` or `FileUpload`, as well as any annotated with `@PartType` automatically imply a `@Consumes(MediaType.MULTIPART_FORM_DATA)` on the method if there is no `@Consumes` present.

> [!NOTE]
> If there are `@RestForm` parameters that are not multipart-implying, then `@Consumes(MediaType.APPLICATION_FORM_URLENCODED)` is implied.

There are a few modes in which the form data can be encoded. By default, REST Client uses RFC1738. You can override it by specifying the mode either on the client level, by setting `io.quarkus.rest.client.multipart-post-encoder-mode` RestBuilder property to the selected value of `HttpPostRequestEncoder.EncoderMode` or by specifying `quarkus.rest-client.multipart-post-encoder-mode` in your `application.properties`. Please note that the latter works only for clients created with the `@RegisterRestClient` annotation. All the available modes are described in the [Netty documentation](https://netty.io/4.1/api/io/netty/handler/codec/http/multipart/HttpPostRequestEncoder.EncoderMode.html)

You can also send JSON multiparts by specifying the `@PartType` annotation:

```java
    public static class Person {
        public String firstName;
        public String lastName;
    }

    @POST
    @Path("/json")
    String sendMultipart(@RestForm @PartType(MediaType.APPLICATION_JSON) Person person);
```

#### Programmatically creating the Multipart form

In cases where the multipart content needs to be built up programmatically, the REST Client provides `ClientMultipartForm` which can be used in the REST Client like so:

```java
public interface MultipartService {

  @POST
  @Path("/multipart")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  Map<String, String> multipart(ClientMultipartForm dataParts);
}
```

More information about this class and supported methods can be found on the javadoc of [`ClientMultipartForm`](https://javadoc.io/doc/io.quarkus.resteasy.reactive/resteasy-reactive-client/latest/org/jboss/resteasy/reactive/client/api/ClientMultipartForm.html).

##### Converting a received multipart object into a client request

A good example of creating `ClientMultipartForm` is one where it is created from the server's `MultipartFormDataInput` (which represents a multipart request received by [Quarkus REST](/guides/rest/index.md#multipart)) - the purpose being to propagate the request downstream while allowing for arbitrary modifications:

```java
public ClientMultipartForm buildClientMultipartForm(MultipartFormDataInput inputForm) // <1>
    throws IOException {
  ClientMultipartForm multiPartForm = ClientMultipartForm.create(); // <2>
  for (Entry<String, Collection<FormValue>> attribute : inputForm.getValues().entrySet()) {
    for (FormValue fv : attribute.getValue()) {
      if (fv.isFileItem()) {
        final FileItem fi = fv.getFileItem();
        String mediaType = Objects.toString(fv.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE),
            MediaType.APPLICATION_OCTET_STREAM);
        if (fi.isInMemory()) {
          multiPartForm.binaryFileUpload(attribute.getKey(), fv.getFileName(),
              Buffer.buffer(IOUtils.toByteArray(fi.getInputStream())), mediaType); // <3>
        } else {
          multiPartForm.binaryFileUpload(attribute.getKey(), fv.getFileName(),
              fi.getFile().toString(), mediaType); // <4>
        }
      } else {
        multiPartForm.attribute(attribute.getKey(), fv.getValue(), fv.getFileName()); // <5>
      }
    }
  }
  return multiPartForm;
}
```

1. `MultipartFormDataInput` is a Quarkus REST (Server) type representing a received multipart request.
2. A `ClientMultipartForm` is created.
3. `FileItem` attribute is created for the request attribute that represented an in memory file attribute
4. `FileItem` attribute is created for the request attribute that represented a file attribute saved on the file system
5. Non-file attributes added directly to `ClientMultipartForm` if not `FileItem`.

In a similar fashion if the received server multipart request is known and looks something like:

```java
public class Request {

  @RestForm("files")
  @PartType(MediaType.APPLICATION_OCTET_STREAM)
  List<FileUpload> files;

  @RestForm("jsonPayload")
  @PartType(MediaType.TEXT_PLAIN)
  String jsonPayload;
}
```

the `ClientMultipartForm` can be created easily as follows:

```java
public ClientMultipartForm buildClientMultipartForm(Request request) { // <1>
  ClientMultipartForm multiPartForm = ClientMultipartForm.create();
  multiPartForm.attribute("jsonPayload", request.getJsonPayload(), "jsonPayload"); // <2>
  request.getFiles().forEach(fu -> {
    multiPartForm.fileUpload(fu); // <3>
  });
  return multiPartForm;
}
```

1. `Request` representing the request the server parts accepts
2. A `jsonPayload` attribute is added directly to `ClientMultipartForm`
3. A `fileUpload` is created from the request's `FileUpload`

> [!NOTE]
> When sending multipart data that uses the same name, problems can arise if the client and server do not use the same multipart encoder mode. By default, the REST Client uses `RFC1738`, but depending on the situation, clients may need to be configured with `HTML5` or `RFC3986` mode.
>
> This configuration can be achieved via the `quarkus.rest-client.multipart-post-encoder-mode` property.

### Receiving Multipart Messages

REST Client also supports receiving multipart messages. As with sending, to parse a multipart response, you need to create a class that describes the response data, e.g.

```java
public class FormDto {
    @RestForm // <1>
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @FormParam("otherField") // <2>
    @PartType(MediaType.TEXT_PLAIN)
    public String textProperty;
}
```

1. uses the shorthand `@RestForm` annotation to make a field as a part of a multipart form
2. the standard `@FormParam` can also be used. It allows to override the name of the multipart part.

Then, create an interface method that corresponds to the call and make it return the `FormDto`:

```java
    @GET
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Path("/get-file")
    FormDto data receiveMultipart();
```

At the moment, multipart response support is subject to the following limitations:

- files sent in multipart responses can only be parsed to `File`, `Path` and `FileDownload`
- each field of the response type has to be annotated with `@PartType` - fields without this annotation are ignored

REST Client needs to know the classes used as multipart return types upfront. If you have an interface method that produces `multipart/form-data`, the return type will be discovered automatically. However, if you intend to use the `ClientBuilder` API to parse a response as multipart, you need to annotate your DTO class with `@MultipartForm`.

> [!WARNING]
> The files you download are not automatically removed and can take up a lot of disk space. Consider removing the files when you are done working with them.

### Multipart mixed / OData usage

It is not uncommon that an application has to interact with enterprise systems (like CRM systems) using a special protocol called [OData](https://www.odata.org/documentation/odata-version-3-0/batch-processing/). This protocol essentially uses a custom HTTP `Content-Type` which needs some glue code to work with the REST Client (creating the body is entirely up to the application - the REST Client can't do much to help).

An example looks like the following:

```java
@Path("/crm")
@RegisterRestClient
public interface CRMService {

    @POST
    @ClientHeaderParam(name = "Content-Type", value = "{calculateContentType}")  // <1>
    String performBatch(@HeaderParam("Authorization") String accessToken, @NotBody String batchId, String body); // <2>

    default String calculateContentType(ComputedParamContext context) {
        return "multipart/mixed;boundary=batch_" + context.methodParameters().get(1).value(); // <3>
    }
}
```

1. `@ClientHeaderParam(name = "Content-Type", value = "{calculateContentType}")` which ensures that the `Content-Type` header is created by calling the interface's `calculateContentType` default method.
2. The aforementioned parameter needs to be annotated with `@NotBody` because it is only used to aid the construction of HTTP headers.
3. `context.methodParameters().get(1).value()` which allows the `calculateContentType` method to obtain the proper method parameter passed to the REST Client method.

As previously mentioned, the body parameter needs to be properly crafted by the application code to conform to the service's requirements.

### Receiving compressed messages

REST Client also supports receiving compressed messages using GZIP and can be enabled via configuration. When this feature is enabled and a server returns a response that includes the header `Content-Encoding: gzip`, REST Client will automatically decode the content and proceed with the message handling.

An example configuration could be:

```properties
# global configuration is used for all clients
quarkus.rest-client.enable-compression=true

# per-client configuration overrides the global settings for a specific client
quarkus.rest-client.my-client.enable-compression=true
```

The REST Client falls back onto the Quarkus wide `quarkus.http.enable-compression` configuration property (which defaults to `false`) if no REST Client specific property is set.

## Proxy support

REST Client supports sending requests through a proxy. You can still rely on JVM proxy settings, but REST Client allows you to define a default proxy and any number of named proxy configurations that clients can reference.

Proxy configuration is centralized in a Proxy Registry; configure proxies under the `quarkus.proxy` configuration root.

### Global proxy configuration

You can define a default proxy configuration that will be used by all REST Clients that do not explicitly reference a named proxy configuration.

```properties
quarkus.proxy.host=localhost
quarkus.proxy.port=8182
quarkus.proxy.username=username
quarkus.proxy.password=password
quarkus.proxy.non-proxy-hosts=my.company.com

quarkus.rest-client.my-client.url=http://example.com/api
```

This configuration will cause all REST Clients, including `my-client`, to use the proxy at `localhost:8182` with the provided credentials.

### Named proxy configuration

You can define named proxy configurations and have specific REST Clients use them.

```properties
quarkus.proxy.my-proxy.host=localhost
quarkus.proxy.my-proxy.port=8183
quarkus.proxy.my-proxy.username=username
quarkus.proxy.my-proxy.password=password

quarkus.rest-client.client1.proxy-configuration-name=my-proxy <1>
quarkus.rest-client.client1.url=http://c1.example.com/api

quarkus.rest-client.client2.url=http://c2.example.com/api <2>
```

1. `client1` is configured to use the `my-proxy` proxy configuration.
2. `client2` does not specify a proxy configuration name, so it will not use any proxy unless a default proxy configuration is defined.

### Using Credentials Provider

In addition to the `quarkus.proxy` settings, REST Client can obtain proxy credentials from the Quarkus Credentials Provider. See [Credentials Provider](https://quarkus.io/guides/credentials-provider) for more details.

```properties
quarkus.proxy.baby-proxy.host=localhost <1>
quarkus.proxy.baby-proxy.port=3838

quarkus.proxy.credentials-provider.name=credential <2>
quarkus.proxy.credentials-provider.bean-name=credentialBean
quarkus.proxy.credentials-provider.username-key=userKey
quarkus.proxy.credentials-provider.password-key=passwordKey
```

1. Define a named proxy configuration `baby-proxy` without username and password.
2. Configure the Credentials Provider to obtain the username and password for the proxy.

> [!NOTE]
> The Credentials Provider is not used when `quarkus.proxy.password` (for global) or `quarkus.proxy.<named>.password` (for named) is not set.

### Local proxy for dev mode

When using the REST Client in dev mode, Quarkus has the ability to stand up a pass-through proxy which can be used as a target for Wireshark (or similar tools) in order to capture all the traffic originating from the REST Client (this really makes sense when the REST Client is used against HTTPS services)

To enable this feature, all that needs to be done is set the `enable-local-proxy` configuration option for the configKey corresponding to the client for which proxying is desired. For example:

```properties
quarkus.rest-client.my-client.enable-local-proxy=true
```

When a REST Client does not use a config key (for example when it is created programmatically via `QuarkusRestClientBuilder`) then the class name can be used instead. For example:

```properties
quarkus.rest-client."org.acme.SomeClient".enable-local-proxy=true
```

The port the proxy is listening can be found in startup logs. An example entry is:

```
Started HTTP proxy server on http://localhost:38227 for REST Client 'org.acme.SomeClient'
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

You should see a JSON object containing some basic information about this extension.

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

## Logging traffic

REST Client can log the requests it sends and the responses it receives. To enable logging, add the `quarkus.rest-client.logging.scope` property to your `application.properties` and set it to:

- `request-response` to log the request and response contents, or
- `all` to also enable low level logging of the underlying libraries.

As HTTP messages can have large bodies, we limit the amount of body characters logged. The default limit is `100`, you can change it by specifying `quarkus.rest-client.logging.body-limit`.

Sensitive request and response header values can be masked using `quarkus.rest-client.logging.masked-headers`. The value of any configured headers will be replaced with `<hidden>` in the logs.

> [!IMPORTANT]
> **Default masked headers**
>
> The default value of `quarkus.rest-client.logging.masked-headers` (`Authorization` and `Cookie`) will be replaced when you explicitly configure a value. You **must** explicitly include them if you still need them masked.

These configuration properties work globally for all clients injected by CDI. If you want configure logging for a specific declarative client, you should do it by specifying named "client" properties, also known as `quarkus.rest-client."client".logging.*` properties.

An example logging configuration:

```properties
quarkus.rest-client.logging.scope=request-response
quarkus.rest-client.logging.body-limit=50
quarkus.rest-client.logging.masked-headers=Authorization,Cookie,x-super-secret

quarkus.rest-client.extensions-api.logging.scope=all
```

> [!TIP]
> REST Client uses a default `ClientLogger` implementation, which can be swapped out for a custom implementation.
>
> When setting up the client programmatically using the `QuarkusRestClientBuilder`, the `ClientLogger` is set via the `clientLogger` method.
>
> For declarative clients using `@RegisterRestClient`, simply providing a CDI bean that implements `ClientLogger` is enough for that logger to be used by said clients.

## Metrics

All declarative REST Client instances produce metrics using the `http.clients` prefix. Furthermore, the metrics contain a tag named `clientName` which corresponds to the config key of the client (as specified by the `configKey` property of the `@RegisterRestClient` annotation).

To enable metrics for programmatically created REST Clients, the following snippet can be used:

```java
var builder = QuarkusRestClientBuilder.newBuilder();

// use the builder to configure the client

// now configure a customizer that sets the metrics name
builder.httpClientOptionsCustomizer(new Consumer<>() {
    @Override
    public void accept(HttpClientOptions httpClientOptions) {
        String metricsName = httpClientOptions.getMetricsName();
        if (metricsName == null || metricsName.isEmpty()) {
            httpClientOptions.setMetricsName("rest-client|" + "someName"); // the 'rest-client|' prefix is absolutely necessary here
        }
    }
});
```

## Mocking the client for tests

If you use a client injected with the `@RestClient` annotation, you can easily mock it for tests. You can do it with Mockito's `@InjectMock` or with `QuarkusMock`.

This section shows how to replace your client with a mock. If you would like to get a more in-depth understanding of how mocking works in Quarkus, see the blog post on [Mocking CDI beans](https://quarkus.io/blog/mocking/).

> [!NOTE]
> Mocking does not work when using `@QuarkusIntegrationTest`.

Let's assume you have the following client:

```java
package io.quarkus.it.rest.client.main;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@Path("/")
@RegisterRestClient
public interface Client {
    @GET
    String get();
}
```

### Mocking with InjectMock

The simplest approach to mock a client for tests is to use Mockito and `@InjectMock`.

First, add the following dependency to your application:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-junit-mockito")
```

Then, in your test you can simply use `@InjectMock` to create and inject a mock:

```java
package io.quarkus.it.rest.client.main;

import static org.mockito.Mockito.when;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class InjectMockTest {

    @InjectMock
    @RestClient
    Client mock;

    @BeforeEach
    public void setUp() {
        when(mock.get()).thenReturn("MockAnswer");
    }

    @Test
    void doTest() {
        // ...
    }
}
```

### Mocking with QuarkusMock

If Mockito doesn't meet your needs, you can create a mock programmatically using `QuarkusMock`, e.g.:

```java
package io.quarkus.it.rest.client.main;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusMockTest {

    @BeforeEach
    public void setUp() {
        Client customMock = new Client() { // <1>
            @Override
            public String get() {
                return "MockAnswer";
            }
        };
        QuarkusMock.installMockForType(customMock, Client.class, RestClient.LITERAL); // <2>
    }
    @Test
    void doTest() {
        // ...
    }
}
```

1. here we use a manually created implementation of the client interface to replace the actual Client
2. note that `RestClient.LITERAL` has to be passed as the last argument of the `installMockForType` method

<a id="using-a-mock-http-server-for-tests"></a>
## Using a Mock HTTP Server for tests

Setting up a mock HTTP server, against which tests are run, is a common testing pattern. Examples of such servers are [Wiremock](https://wiremock.org/) and [Hoverfly](https://docs.hoverfly.io/projects/hoverfly-java/en/latest/index.html). In this section we'll demonstrate how Wiremock can be leveraged for testing the `ExtensionsService` which was developed above.

First, Wiremock needs to be added as a test dependency. For a Maven project that would happen like so:

**pom.xml**

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <scope>test</scope>
    <version>${wiremock.version}</version> <1>
</dependency>
```

1. Use a proper Wiremock version. All available versions can be found [here](https://search.maven.org/artifact/org.wiremock/wiremock).

**build.gradle**

```gradle
testImplementation("org.wiremock:wiremock:$wiremockVersion") <1>
```

1. Use a proper Wiremock version. All available versions can be found [here](https://search.maven.org/artifact/org.wiremock/wiremock).

In Quarkus tests when some service needs to be started before the Quarkus tests are ran, we utilize the `@io.quarkus.test.common.QuarkusTestResource` annotation to specify a `io.quarkus.test.common.QuarkusTestResourceLifecycleManager` which can start the service and supply configuration values that Quarkus will use.

> [!NOTE]
> For more details about `@QuarkusTestResource` refer to  [this part of the documentation](/guides/getting-started-testing/index.md#quarkus-test-resource).

Let's create an implementation of `QuarkusTestResourceLifecycleManager` called `WiremockExtensions` like so:

```java
package org.acme.rest.client;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.*; // <1>

public class WireMockExtensions implements QuarkusTestResourceLifecycleManager {  // <2>

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start(); // <3>

        wireMockServer.stubFor(get(urlEqualTo("/extensions?id=io.quarkus:quarkus-rest-client"))   // <4>
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            "[{" +
                            "\"id\": \"io.quarkus:quarkus-rest-client\"," +
                            "\"name\": \"REST Client\"" +
                            "}]"
                        )));

        wireMockServer.stubFor(get(urlMatching(".*")).atPriority(10).willReturn(aResponse().proxiedFrom("https://stage.code.quarkus.io/api")));   // <5>

        return Map.of("quarkus.rest-client.\"org.acme.rest.client.ExtensionsService\".url", wireMockServer.baseUrl()); // <6>
    }

    @Override
    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();  // <7>
        }
    }
}
```

1. Statically importing the methods in the Wiremock package makes it easier to read the test.
2. The `start` method is invoked by Quarkus before any test is run and returns a `Map` of configuration properties that apply during the test execution.
3. Launch Wiremock.
4. Configure Wiremock to stub the calls to `/extensions?id=io.quarkus:quarkus-rest-client` by returning a specific canned response.
5. All HTTP calls that have not been stubbed are handled by calling the real service. This is done for demonstration purposes, as it is not something that would usually happen in a real test.
6. As the `start` method returns configuration that applies for tests, we set the rest-client property that controls the base URL which is used by the implementation of `ExtensionsService` to the base URL where Wiremock is listening for incoming requests.
7. When all tests have finished, shutdown Wiremock.

The `ExtensionsResourceTest` test class needs to be annotated like so:

```java
@QuarkusTest
@QuarkusTestResource(WireMockExtensions.class)
public class ExtensionsResourceTest {

}
```

> [!WARNING]
> `@QuarkusTestResource` applies to all tests, not just `ExtensionsResourceTest`.

## Known limitations

While the REST Client extension aims to be a drop-in replacement for the RESTEasy Client extension, there are some differences and limitations:

- the default scope of the client for the new extension is `@ApplicationScoped` while the `quarkus-resteasy-client` defaults to `@Dependent` To change this behavior, set the `quarkus.rest-client.scope` property to the fully qualified scope name.
- it is not possible to set `SSLContext`
- a few things that don't make sense for a non-blocking implementations, such as setting the `ExecutorService`, don't work

## Import things to be aware of

### Concurrent requests

The REST Client uses the Vert.x HTTP connection pool with a default size of `50`  in order to minimize the necessary HTTP connections being used against the target REST services. While this is a reasonable default, it might be too limiting for specific scenarios. In such cases, the `quarkus.rest-client."some-client".connection-pool-size` configuration property can be used.

## Further reading

\* [MicroProfile Rest Client specification](https://download.eclipse.org/microprofile/microprofile-rest-client-4.0/microprofile-rest-client-spec-4.0.html)

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-rest-client_quarkus-rest-client-disable-smart-produces"></a> [`quarkus.rest-client.disable-smart-produces`](#quarkus-rest-client_quarkus-rest-client-disable-smart-produces) config_property_copy_button:+++quarkus.rest-client.disable-smart-produces+++[]<br><br>By default, RESTEasy Reactive uses text/plain content type for String values and application/json for everything else.<br><br>MicroProfile Rest Client spec requires the implementations to always default to application/json. This build item disables the "smart" behavior of RESTEasy Reactive to comply to the spec<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_DISABLE_SMART_PRODUCES+++[] | boolean | `false` |
| [lock] <a id="quarkus-rest-client_quarkus-rest-client-provider-autodiscovery"></a> [`quarkus.rest-client.provider-autodiscovery`](#quarkus-rest-client_quarkus-rest-client-provider-autodiscovery) config_property_copy_button:+++quarkus.rest-client.provider-autodiscovery+++[]<br><br>Whether providers (filters, etc.) annotated with `jakarta.ws.rs.ext.Provider` should be automatically registered for all the clients in the application.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_PROVIDER_AUTODISCOVERY+++[] | boolean | `true` |

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
