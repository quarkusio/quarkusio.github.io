# RESTEasy Classic

> [!WARNING]
> This guide is about [RESTEasy Classic](https://resteasy.dev), which used to be the default Jakarta REST (formerly known as JAX-RS) implementation until Quarkus 2.8.
>
> It is now recommended to use Quarkus REST (formerly RESTEasy Reactive), which supports both traditional blocking workloads and reactive workloads equally well.
>
> For more information about Quarkus REST, please see the [introductory REST JSON guide](/guides/rest-json/index.md) or the [Quarkus REST reference documentation](/guides/rest/index.md).

> [!TIP]
> There is another guide if you need a [REST client based on RESTEasy Classic](/guides/resteasy-client/index.md) (including support for JSON).

## Architecture

The application created in this guide is straightforward: users can add elements to a list through a form, and the list gets updated accordingly.

All the information between the browser and the server is formatted as JSON.

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:rest-json-quickstart \
    --extension='resteasy-jackson' \
    --no-code
cd rest-json-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=rest-json-quickstart \
    -Dextensions='resteasy-jackson' \
    -DnoCode
cd rest-json-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=rest-json-quickstart"`

This command generates a new project importing the RESTEasy/Jakarta REST and [Jackson](https://github.com/FasterXML/jackson) extensions, and in particular, adds the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-jackson</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-resteasy-jackson")
```

> [!NOTE]
> To improve user experience, Quarkus registers the three Jackson [Java 8 modules](https://github.com/FasterXML/jackson-modules-java8), so you do not need to do it manually.

Quarkus also supports [JSON-B](https://eclipse-ee4j.github.io/jsonb-api/), so if you prefer JSON-B over Jackson, you can create a project relying on the RESTEasy JSON-B extension instead:

**CLI**

```bash
quarkus create app org.acme:rest-json-quickstart \
    --extension='resteasy-jsonb' \
    --no-code
cd rest-json-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=rest-json-quickstart \
    -Dextensions='resteasy-jsonb' \
    -DnoCode
cd rest-json-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=rest-json-quickstart"`

This command generates a new project importing the RESTEasy/Jakarta REST and [JSON-B](https://eclipse-ee4j.github.io/jsonb-api/) extensions, and in particular, adds the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-jsonb</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-resteasy-jsonb")
```

## Creating the first JSON REST service

In this example, we will create an application to manage a list of fruits.

First, let us create the `Fruit` bean as follows:

```java
package org.acme.rest.json;

public class Fruit {

    public String name;
    public String description;

    public Fruit() {
    }

    public Fruit(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

Nothing fancy. One important thing to note is that having a default constructor is required by the JSON serialization layer.

Now, create the `org.acme.rest.json.FruitResource` class as follows:

```java
package org.acme.rest.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/fruits")
public class FruitResource {

    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    public FruitResource() {
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
    }

    @GET
    public Set<Fruit> list() {
        return fruits;
    }

    @POST
    public Set<Fruit> add(Fruit fruit) {
        fruits.add(fruit);
        return fruits;
    }

    @DELETE
    public Set<Fruit> delete(Fruit fruit) {
        fruits.removeIf(existingFruit -> existingFruit.name.contentEquals(fruit.name));
        return fruits;
    }
}
```

The implementation is pretty straightforward, and you just need to define your endpoints using the Jakarta REST annotations.

The `Fruit` objects will be automatically serialized/deserialized by [JSON-B](https://eclipse-ee4j.github.io/jsonb-api/) or [Jackson](https://github.com/FasterXML/jackson), depending on the extension you chose when initializing the project.

> [!NOTE]
> When a JSON extension like `quarkus-resteasy-jackson` or `quarkus-resteasy-jsonb` is installed, Quarkus defaults to the `application/json` media type for most return values. This can be overridden using `@Produces` or `@Consumes` annotations, except for certain well-known types like `String` (defaulting to `text/plain`) and `File` (defaulting to `application/octet-stream`).
>
> To disable the default JSON behavior, set `quarkus.resteasy-json.default-json=false`, and the default will return to auto-negotiation. In this case, you must include `@Produces(MediaType.APPLICATION_JSON)` and `@Consumes(MediaType.APPLICATION_JSON)` annotations in your endpoints to use JSON.
>
> If you don't depend on the JSON default, it's highly advisable to use `@Produces` and `@Consumes` annotations on your endpoints to specify the expected content types precisely. This helps reduce the number of Jakarta REST providers (essentially converters) included in the native executable.

<a id="json"></a>
### Configuring JSON support

#### Jackson

In Quarkus, the default Jackson `ObjectMapper` obtained via CDI (utilized by Quarkus extensions) is set to ignore unknown properties (by disabling `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES`).

To revert to Jackson's default behavior, set `quarkus.jackson.fail-on-unknown-properties=true` in your `application.properties`, or set it on a per-class basis with `@JsonIgnoreProperties(ignoreUnknown = false)`.

Additionally, the `ObjectMapper` formats dates and times in ISO-8601 (by disabling `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`).

To restore Jackson's default behavior, use `quarkus.jackson.write-dates-as-timestamps=true` in your `application.properties`. For custom date format on a single field, use the `@JsonFormat` annotation.

Quarkus simplifies Jackson configuration via CDI beans. Create a CDI bean of type `io.quarkus.jackson.ObjectMapperCustomizer` to apply various Jackson settings. Here's an example for registering a custom module:

```java @ApplicationScoped public class MyObjectMapperCustomizer implements ObjectMapperCustomizer { @Override public void customize(ObjectMapper objectMapper) { // Add custom Jackson configuration here } } ```

This approach is recommended for configuring Jackson settings.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class RegisterCustomModuleCustomizer implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new CustomModule());
    }
}
```

Users can even provide their own `ObjectMapper` bean if they so choose. If this is done, it is very important to manually inject and apply all `io.quarkus.jackson.ObjectMapperCustomizer` beans in the CDI producer that produces `ObjectMapper`. Failure to do so will prevent Jackson-specific customizations provided by various extensions from being applied.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.All;
import io.quarkus.jackson.ObjectMapperCustomizer;
import java.util.List;
import jakarta.inject.Singleton;

public class CustomObjectMapper {

    // Replaces the CDI producer for ObjectMapper built into Quarkus
    @Singleton
    ObjectMapper objectMapper(@All List<ObjectMapperCustomizer> customizers) {
        ObjectMapper mapper = myObjectMapper(); // Custom `ObjectMapper`

        // Apply all ObjectMapperCustomizer beans (incl. Quarkus)
        for (ObjectMapperCustomizer customizer : customizers) {
            customizer.customize(mapper);
        }

        return mapper;
    }
}
```

#### JSON-B

As stated above, Quarkus provides the option of using JSON-B instead of Jackson via the use of the `quarkus-resteasy-jsonb` extension.

Following the same approach described in the previous section, JSON-B can be configured using an `io.quarkus.jsonb.JsonbConfigCustomizer` bean.

If, for example, a custom serializer named `FooSerializer` for type `com.example.Foo` needs to be registered with JSON-B, the addition of a bean like the following would suffice:

```java
import io.quarkus.jsonb.JsonbConfigCustomizer;
import jakarta.inject.Singleton;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.JsonbSerializer;

@Singleton
public class FooSerializerRegistrationCustomizer implements JsonbConfigCustomizer {

    public void customize(JsonbConfig config) {
        config.withSerializers(new FooSerializer());
    }
}
```

A more advanced option would be to directly provide a bean of `jakarta.json.bind.JsonbConfig` (with a `Dependent` scope) or, in the extreme case, to provide a bean of type `jakarta.json.bind.Jsonb` (with a `Singleton` scope). If the latter approach is leveraged it is very important to manually inject and apply all `io.quarkus.jsonb.JsonbConfigCustomizer` beans in the CDI producer that produces `jakarta.json.bind.Jsonb`. Failure to do so will prevent JSON-B specific customizations provided by various extensions from being applied.

```java
import io.quarkus.jsonb.JsonbConfigCustomizer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.json.bind.JsonbConfig;

public class CustomJsonbConfig {

    // Replaces the CDI producer for JsonbConfig built into Quarkus
    @Dependent
    JsonbConfig jsonConfig(Instance<JsonbConfigCustomizer> customizers) {
        JsonbConfig config = myJsonbConfig(); // Custom `JsonbConfig`

        // Apply all JsonbConfigCustomizer beans (incl. Quarkus)
        for (JsonbConfigCustomizer customizer : customizers) {
            customizer.customize(config);
        }

        return config;
    }
}
```

<a id="links"></a>
### JSON Hypertext Application Language (HAL) support

The [HAL](https://www.ietf.org/archive/id/draft-kelly-json-hal-11.html) standard is a simple format to represent web links.

To enable the HAL support, add the `quarkus-hal` extension to your project. Also, as HAL needs JSON support, you need to add either the `quarkus-resteasy-jsonb` or the `quarkus-resteasy-jackson` extension.

**Table Context object**

| GAV | Usage |
| --- | --- |
| `io.quarkus:quarkus-hal` | [HAL](https://www.ietf.org/archive/id/draft-kelly-json-hal-11.html) |

After adding the extensions, we can now annotate the REST resources to produce the media type `application/hal+json` (or use RestMediaType.APPLICATION_HAL_JSON). For example:

```java
@Path("/records")
public class RecordsResource {

    @GET
    @Produces({ MediaType.APPLICATION_JSON, "application/hal+json" })
    @LinkResource(entityClassName = "org.acme.Record", rel = "list")
    public List<TestRecord> getAll() {
        // ...
    }

    @GET
    @Path("/first")
    @Produces({ MediaType.APPLICATION_JSON, "application/hal+json" })
    @LinkResource(rel = "first")
    public TestRecord getFirst() {
        // ...
    }
}
```

Now, the endpoints `/records` and `/records/first` will accept the media type, both `json` and `hal+json`, to print the records in Hal format.

For example, if we invoke the `/records` endpoint using curl to return a list of records, the HAL format will look like as follows:

```bash
& curl -H "Accept:application/hal+json" -i localhost:8080/records
{
    "_embedded": {
        "items": [
            {
                "id": 1,
                "slug": "first",
                "value": "First value",
                "_links": {
                    "list": {
                        "href": "http://localhost:8081/records"
                    },
                    "first": {
                        "href": "http://localhost:8081/records/first"
                    }
                }
            },
            {
                "id": 2,
                "slug": "second",
                "value": "Second value",
                "_links": {
                    "list": {
                        "href": "http://localhost:8081/records"
                    },
                    "first": {
                        "href": "http://localhost:8081/records/first"
                    }
                }
            }
        ]
    },
    "_links": {
        "list": {
            "href": "http://localhost:8081/records"
        }
    }
}
```

When we call a resource `/records/first` that returns only one instance, then the output is:

```bash
& curl -H "Accept:application/hal+json" -i localhost:8080/records/first
{
    "id": 1,
    "slug": "first",
    "value": "First value",
    "_links": {
        "list": {
            "href": "http://localhost:8081/records"
        },
        "first": {
            "href": "http://localhost:8081/records/first"
        }
    }
}
```

## Creating a frontend

Now let us add a simple web page to interact with our `FruitResource`. Quarkus automatically serves static resources located under the `META-INF/resources` directory. In the `src/main/resources/META-INF/resources` directory, add a `fruits.html` file with the content from this [fruits.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/rest-json-quickstart/src/main/resources/META-INF/resources/fruits.html) file in it.

You can now interact with your REST service:

- start Quarkus with:

  **CLI**

  ```bash
  quarkus dev
  ```

  \+

  **Maven**

  ```bash
  ./mvnw quarkus:dev
  ```

  \+

  **Gradle**

  ```bash
  ./gradlew --console=plain quarkusDev
  ```

- open a browser to `http://localhost:8080/fruits.html`
- add new fruits to the list via the form

## Building a native executable

You can build a native executable with the usual command:

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

Running it is as simple as executing `./target/rest-json-quickstart-1.0.0-SNAPSHOT-runner`.

You can then point your browser to `http://localhost:8080/fruits.html` and use your application.

## About serialization

JSON serialization libraries use Java reflection to get the properties of an object and serialize them.

When using native executables with GraalVM, all classes that will be used with reflection need to be registered. The good news is that Quarkus does that work for you most of the time. So far, we have not registered any class, not even `Fruit`, for reflection usage, and everything is working fine.

Quarkus performs some magic when it is capable of inferring the serialized types from the REST methods. When you have the following REST method, Quarkus determines that `Fruit` will be serialized:

```JAVA
@GET
public List<Fruit> list() {
    // ...
}
```

Quarkus does that for you automatically by analyzing the REST methods at build time, and that is why we did not need any reflection registration in the first part of this guide.

Another common pattern in the Jakarta REST world is to use the `Response` object. `Response` comes with some nice perks:

- You can return different entity types depending on what happens in your method (a `Legume` or an `Error` for instance).
- You can set the attributes of the `Response` (the status comes to mind in the case of an error).

Your REST method then looks like this:

```JAVA
@GET
public Response list() {
    // ...
}
```

Quarkus cannot determine at build time the type included in the `Response` as the information is not available. In this case, Quarkus won't be able to register for reflection in the required classes automatically.

This leads us to our next section.

## Using response

Let us create the `Legume` class, which will be serialized as JSON, following the same model as for our `Fruit` class:

```JAVA
package org.acme.rest.json;

public class Legume {

    public String name;
    public String description;

    public Legume() {
    }

    public Legume(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
```

Now let's create a `LegumeResource` REST service with only one method that returns the list of legumes.

This method returns a `Response` and not a list of `Legume`.

```JAVA
package org.acme.rest.json;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/legumes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LegumeResource {

    private Set<Legume> legumes = Collections.synchronizedSet(new LinkedHashSet<>());

    public LegumeResource() {
        legumes.add(new Legume("Carrot", "Root vegetable, usually orange"));
        legumes.add(new Legume("Zucchini", "Summer squash"));
    }

    @GET
    public Response list() {
        return Response.ok(legumes).build();
    }
}
```

Now, let us add a simple web page to display our list of legumes. In the `src/main/resources/META-INF/resources` directory, add a `legumes.html` file with the content from this [legumes.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/rest-json-quickstart/src/main/resources/META-INF/resources/legumes.html) file in it.

Open a browser to <http://localhost:8080/legumes.html>, and you will see our list of legumes.

The interesting part starts when running the application as a native executable:

- create the native executable with:

  **CLI**

  ```bash
  quarkus build --native
  ```

  \+

  **Maven**

  ```bash
  ./mvnw install -Dnative
  ```

  \+

  **Gradle**

  ```bash
  ./gradlew build -Dquarkus.native.enabled=true
  ```

- execute it with `./target/rest-json-quickstart-1.0.0-SNAPSHOT-runner`
- open a browser and go to <http://localhost:8080/legumes.html>

There are no legumes there.

As mentioned above, the issue is that Quarkus could not determine the `Legume` class, which will require some reflection by analyzing the REST endpoints. The JSON serialization library tries to get the list of fields of `Legume` and gets an empty list, so it does not serialize the fields' data.

> [!NOTE]
> At the moment, when JSON-B or Jackson tries to get the list of fields of a class, if the class is not registered for reflection, no exception will be thrown. GraalVM will return an empty list of fields.
>
> Hopefully, this will change in the future and make the error more obvious.

We can register `Legume` for reflection manually by adding the `@RegisterForReflection` annotation on our `Legume` class:

```JAVA
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Legume {
    // ...
}
```

> [!TIP]
> The `@RegisterForReflection` annotation instructs Quarkus to keep the class and its members during the native compilation. More details about the `@RegisterForReflection` annotation can be found on the [native application tips](/guides/writing-native-applications-tips/index.md#registerForReflection) page.

Let us do that and follow the same steps as before:

- hit `Ctrl+C` to stop the application
- create the native executable with:

  **CLI**

  ```bash
  quarkus build --native
  ```

  \+

  **Maven**

  ```bash
  ./mvnw install -Dnative
  ```

  \+

  **Gradle**

  ```bash
  ./gradlew build -Dquarkus.native.enabled=true
  ```

- execute it with `./target/rest-json-quickstart-1.0.0-SNAPSHOT-runner`
- open a browser and go to <http://localhost:8080/legumes.html>

This time, you can see our list of legumes.

<a id="reactive"></a>
## Being reactive

> [!WARNING]
> For reactive workloads, please always use [Quarkus REST](/guides/rest/index.md).

You can return *reactive types* to handle asynchronous processing. Quarkus recommends the usage of [Mutiny](https://smallrye.io/smallrye-mutiny) to write reactive and asynchronous code.

To integrate Mutiny and RESTEasy, you need to add the `quarkus-resteasy-mutiny` dependency to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-mutiny</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-resteasy-mutiny")
```

Then, your endpoint can return `Uni` or `Multi` instances:

```java

@GET
@Path("/{name}")
public Uni<Fruit> getOne(@PathParam String name) {
    return findByName(name);
}

@GET
public Multi<Fruit> getAll() {
    return findAll();
}
```

Use `Uni` when you have a single result. Use `Multi` when you have multiple items that may be emitted asynchronously.

You can use `Uni` and `Response` to return asynchronous HTTP responses: `Uni<Response>`.

More details about Mutiny can be found in [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

## HTTP filters and interceptors

Both HTTP request and response can be intercepted by providing `ContainerRequestFilter` or `ContainerResponseFilter` implementations respectively. These filters are suitable for processing the metadata associated with a message: HTTP headers, query parameters, media type, and other metadata. They also can abort the request processing, for instance, when the user does not have permission to access the endpoint.

Let's use `ContainerRequestFilter` to add logging capability to our service. We can do that by implementing `ContainerRequestFilter` and annotating it with the `@Provider` annotation:

```java
package org.acme.rest.json;

import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LoggingFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext context) {

        final String method = context.getMethod();
        final String path = info.getPath();
        final String address = request.remoteAddress().toString();

        LOG.infof("Request %s %s from IP %s", method, path, address);
    }
}
```

Now, whenever a REST method is invoked, the request will be logged into the console:

```text
2019-06-05 12:44:26,526 INFO  [org.acm.res.jso.LoggingFilter] (executor-thread-1) Request GET /legumes from IP 127.0.0.1
2019-06-05 12:49:19,623 INFO  [org.acm.res.jso.LoggingFilter] (executor-thread-1) Request GET /fruits from IP 0:0:0:0:0:0:0:1
2019-06-05 12:50:44,019 INFO  [org.acm.res.jso.LoggingFilter] (executor-thread-1) Request POST /fruits from IP 0:0:0:0:0:0:0:1
2019-06-05 12:51:04,485 INFO  [org.acm.res.jso.LoggingFilter] (executor-thread-1) Request GET /fruits from IP 127.0.0.1
```

## CORS filter

[Cross-origin resource sharing](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing) (CORS) is a mechanism that allows restricted resources on a web page to be requested from another domain outside the domain from which the first resource was served.

Quarkus includes a CORS filter at the HTTP layer level. For more information about the CORS filters and their usage, see the [CORS filter](/guides/security-cors/index.md#cors-filter) section of the Quarkus "Cross-origin resource sharing" guide.

## GZip Support

Quarkus comes with GZip support (even though it is not enabled by default). The following configuration knobs allow to configure GZip support.

```properties
quarkus.resteasy.gzip.enabled=true // <1>
quarkus.resteasy.gzip.max-input=10M // <2>
```

1. Enable Gzip support.
2. Configure the upper limit on the deflated request body. This is useful to mitigate potential attacks by limiting their reach. The default value is `10M`. This configuration option would recognize strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`. If no suffix is given, assume bytes.

Once GZip support has been enabled, you can use it on an endpoint by adding the `@org.jboss.resteasy.annotations.GZIP` annotation to your endpoint method.

> [!NOTE]
> There is also the `quarkus.http.enable-compression` configuration property, which enables HTTP response compression globally. If enabled, a response body is compressed if the `Content-Type` HTTP header is set and the value is a compressed media type configured via the `quarkus.http.compress-media-types` configuration property.

## Multipart Support

RESTEasy supports multipart via the [RESTEasy Multipart Provider](https://docs.jboss.org/resteasy/docs/4.5.6.Final/userguide/html/Multipart.html).

Quarkus provides an extension called `quarkus-resteasy-multipart` to make things easier for you.

This extension slightly differs from the RESTEasy default behavior as the default charset (if none is specified in your request) is UTF-8 rather than US-ASCII.

You can configure this behavior with the following configuration properties:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-resteasy-multipart_quarkus-resteasy-multipart-input-part-default-charset"></a> [`quarkus.resteasy.multipart.input-part.default-charset`](#quarkus-resteasy-multipart_quarkus-resteasy-multipart-input-part-default-charset) config_property_copy_button:+++quarkus.resteasy.multipart.input-part.default-charset+++[]<br><br>Default charset.<br><br>Note that the default value is UTF-8 which is different from RESTEasy's default value US-ASCII.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RESTEASY_MULTIPART_INPUT_PART_DEFAULT_CHARSET+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) | `UTF-8` |
| <a id="quarkus-resteasy-multipart_quarkus-resteasy-multipart-input-part-default-content-type"></a> [`quarkus.resteasy.multipart.input-part.default-content-type`](#quarkus-resteasy-multipart_quarkus-resteasy-multipart-input-part-default-content-type) config_property_copy_button:+++quarkus.resteasy.multipart.input-part.default-content-type+++[]<br><br>The default content-type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RESTEASY_MULTIPART_INPUT_PART_DEFAULT_CONTENT_TYPE+++[] | string | `text/plain` |

## Servlet compatibility

In Quarkus, RESTEasy can either run directly on top of the Vert.x HTTP server, or on top of Undertow if you have any servlet dependency.

As a result, certain classes, such as `HttpServletRequest` are not always available for injection. Most use cases for this particular class are covered by Jakarta REST equivalents, except for getting the remote client's IP.

RESTEasy comes with a replacement API that you can inject: [`HttpRequest`](https://docs.jboss.org/resteasy/docs/4.5.6.Final/javadocs/org/jboss/resteasy/spi/HttpRequest.html), which has the methods [`getRemoteAddress()`](https://docs.jboss.org/resteasy/docs/4.5.6.Final/javadocs/org/jboss/resteasy/spi/HttpRequest.html#getRemoteAddress--) and [`getRemoteHost()`](https://docs.jboss.org/resteasy/docs/4.5.6.Final/javadocs/org/jboss/resteasy/spi/HttpRequest.html#getRemoteHost--) to solve this problem.

## RESTEasy and REST Client interactions

In Quarkus, the RESTEasy extension and [the REST Client extension](/guides/resteasy-client/index.md) share the same infrastructure. One important consequence of this consideration is that they share the same list of providers (in the Jakarta REST meaning of the word).

For instance, if you declare a `WriterInterceptor`, it will, by default, intercept both the servers calls and the client calls, which might not be the desired behavior.

However, you can change this default behavior and constrain a provider to:

- only consider **server** calls by adding the `@ConstrainedTo(RuntimeType.SERVER)` annotation to your provider;
- only consider **client** calls by adding the `@ConstrainedTo(RuntimeType.CLIENT)` annotation to your provider.

## What's Different from Jakarta EE Development

### No Need for `Application` Class

Configuration via an application-supplied subclass of `Application` is supported but not required.

### Only a single Jakarta REST application

In contrast to Jakarta REST (and RESTEasy) running in a standard servlet container, Quarkus only supports the deployment of a single Jakarta REST application. If multiple Jakarta REST `Application` classes are defined, the build will fail with the message `Multiple classes have been annotated with @ApplicationPath which is currently not supported`.

If multiple Jakarta REST applications are defined, the property `quarkus.resteasy.ignore-application-classes=true` can be used to ignore all explicit `Application` classes. This makes all resource-classes available via the application-path as defined by `quarkus.resteasy.path` (default: `/`).

### Support limitations of Jakarta REST application

The RESTEasy extension doesn't support the method `getProperties()` of the class `jakarta.ws.rs.core.Application`. Moreover, it only relies on the methods `getClasses()` and `getSingletons()` to filter out the annotated resource, provider, and feature classes. It does not filter out the built-in resource, provider, and feature classes and also the resource, provider, and feature classes registered by the other extensions. Finally, the objects returned by the method `getSingletons()` are ignored, only the classes are taken into account to filter out the resource, provider and feature classes, in other words the method `getSingletons()` is managed the same way as `getClasses()`.

### Lifecycle of Resources

In Quarkus, all Jakarta REST resources are treated as CDI beans. It's possible to inject other beans via `@Inject`, bind interceptors using bindings such as `@Transactional`, define `@PostConstruct` callbacks, etc.

If no scope annotation is declared on the resource class, then the scope is defaulted. The `quarkus.resteasy.singleton-resources` property can control the default scope.

If set to `true` (default), then a **single instance** of a resource class is created to service all requests (as defined by `@jakarta.inject.Singleton`).

If set to `false`, then a **new instance** of the resource class is created per each request.

An explicit CDI scope annotation (`@RequestScoped`, `@ApplicationScoped`, etc.) always overrides the default behavior and specifies the lifecycle of resource instances.

> [!WARNING]
> `@Context` elements are not injected via CDI and, as such, may not be injected via constructor injection. Inject `@Context` elements in fields of your resources instead.

## Include/Exclude Jakarta REST classes with build time conditions

Quarkus enables the inclusion or exclusion of Jakarta REST Resources, Providers and Features directly thanks to build time conditions in the same that it does for CDI beans. Thus, the various Jakarta REST classes can be annotated with profile conditions (`@io.quarkus.arc.profile.IfBuildProfile` or `@io.quarkus.arc.profile.UnlessBuildProfile`) and/or with property conditions (`io.quarkus.arc.properties.IfBuildProperty` or `io.quarkus.arc.properties.UnlessBuildProperty`) to indicate to Quarkus at build time under which conditions these Jakarta REST classes should be included.

In the following example, Quarkus includes the endpoint `sayHello` if and only if the build profile `app1` has been enabled.

```java
@IfBuildProfile("app1")
public class ResourceForApp1Only {

    @GET
    @Path("sayHello")
    public String sayHello() {
        return "hello";
     }
}
```

Please note that if a Jakarta REST Application has been detected and the method `getClasses()` and/or `getSingletons()` has/have been overridden, Quarkus will ignore the build time conditions and consider only what has been defined in the Jakarta REST Application.

## Conclusion

Creating JSON REST services with Quarkus is easy as it relies on proven and well-known technologies.

As usual, Quarkus further simplifies things under the hood when running your application as a native executable.

There is only one thing to remember: if you use `Response` and Quarkus cannot determine the beans that are serialized, you need to annotate them with `@RegisterForReflection`.
