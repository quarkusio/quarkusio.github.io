# Writing JSON REST services

JSON is now the *lingua franca* between microservices.

In this guide, we see how you can get your REST services to consume and produce JSON payloads.

> [!TIP]
> there is another guide if you need a [REST client](/guides/rest-client/index.md) (including support for JSON).

> [!TIP]
> This is an introduction to writing JSON REST services with Quarkus. A more detailed guide about Quarkus REST (formerly RESTEasy Reactive) is available [here](/guides/rest/index.md).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

The application built in this guide is quite simple: the user can add elements in a list using a form and the list is updated.

All the information between the browser and the server are formatted as JSON.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `rest-json-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/rest-json-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:rest-json-quickstart \
    --extension='rest-jackson' \
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
    -Dextensions='rest-jackson' \
    -DnoCode
cd rest-json-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=rest-json-quickstart"`

This command generates a new project importing the Quarkus REST/Jakarta REST and [Jackson](https://github.com/FasterXML/jackson) extensions, and in particular adds the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-rest-jackson")
```

> [!NOTE]
> To improve user experience, Quarkus registers the three Jackson [Java 8 modules](https://github.com/FasterXML/jackson-modules-java8) so you don't need to do it manually.

Quarkus also supports [JSON-B](https://jakarta.ee/specifications/jsonb/) so, if you prefer JSON-B over Jackson, you can create a project relying on the Quarkus REST JSON-B extension instead:

**CLI**

```bash
quarkus create app org.acme:rest-json-quickstart \
    --extension='rest-jsonb' \
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
    -Dextensions='rest-jsonb' \
    -DnoCode
cd rest-json-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=rest-json-quickstart"`

This command generates a new project importing the Quarkus REST/Jakarta REST and [JSON-B](https://jakarta.ee/specifications/jsonb/) extensions, and in particular adds the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jsonb</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-rest-jsonb")
```

> [!WARNING]
> If you use JSON-B and JSON-P, make sure you don't use the shortcut methods offered by `jakarta.json.Json` such as `Json.createValue(...)`.
>
> At the moment, any single call to these methods will [initialize a new `JsonProvider`](https://github.com/jakartaee/jsonp-api/issues/154), which is extremely slow. Quarkus provides a shared `JsonProvider` instance via the `JsonProviderHolder` class of the `quarkus-jsonp` extension.
>
> You can import it as a static import to simplify your code:
>
> ```java
> import static io.quarkus.jsonp.JsonProviderHolder.jsonProvider;
>
> [...]
>
>     public void method() {
>         jsonProvider().createValue("value");
>     }
>
> [...]
> ```

> [!NOTE]
> For more information about Quarkus REST, please refer to the [dedicated guide](/guides/rest/index.md).

## Creating your first JSON REST service

In this example, we will create an application to manage a list of fruits.

First, let's create the `Fruit` bean as follows:

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

The `Fruit` objects will be automatically serialized/deserialized by [JSON-B](https://jakarta.ee/specifications/jsonb/) or [Jackson](https://github.com/FasterXML/jackson), depending on the extension you chose when initializing the project.

> [!NOTE]
> When a JSON extension is installed such as `quarkus-rest-jackson` or `quarkus-rest-jsonb`, Quarkus will use the `application/json` media type by default for most return values, unless the media type is explicitly set via `@Produces` or `@Consumes` annotations (there are some exceptions for well known types, such as `String` and `File`, which default to `text/plain` and `application/octet-stream` respectively).

<a id="json"></a>
### Configuring JSON support

#### Jackson

In Quarkus, the default Jackson `ObjectMapper` obtained via CDI (and consumed by the Quarkus extensions) is configured to ignore unknown properties (by disabling the `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` feature).

You can restore the default behavior of Jackson by setting `quarkus.jackson.fail-on-unknown-properties=true` in your `application.properties` or on a per-class basis via `@JsonIgnoreProperties(ignoreUnknown = false)`.

Furthermore, the `ObjectMapper` is configured to format dates and time in ISO-8601 (by disabling the `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` feature).

The default behaviour of Jackson can be restored by setting `quarkus.jackson.write-dates-as-timestamps=true` in your `application.properties`. If you want to change the format for a single field, you can use the `@JsonFormat` annotation.

Also, Quarkus makes it very easy to configure various Jackson settings via CDI beans. The simplest (and suggested) approach is to define a CDI bean of type `io.quarkus.jackson.ObjectMapperCustomizer` inside of which any Jackson configuration can be applied.

An example where a custom module needs to be registered would look like so:

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

Users can even provide their own `ObjectMapper` bean if they so choose. If this is done, it is very important to manually inject and apply all `io.quarkus.jackson.ObjectMapperCustomizer` beans in the CDI producer that produces `ObjectMapper`. Failure to do so will prevent Jackson specific customizations provided by various extensions from being applied.

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.All;
import io.quarkus.jackson.ObjectMapperCustomizer;
import java.util.List;
import jakarta.inject.Singleton;

public class CustomObjectMapper {

    // Replaces the CDI producer for ObjectMapper built into Quarkus
    @Singleton
    @Produces
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

##### Mixin support

Quarkus automates the registration of Jackson's Mixin support, via the `io.quarkus.jackson.JacksonMixin` annotation. This annotation can be placed on classes that are meant to be used as Jackson mixins while the classes they are meant to customize are defined as the value of the annotation.

#### JSON-B

As stated above, Quarkus provides the option of using JSON-B instead of Jackson via the use of the `quarkus-resteasy-jsonb` extension.

Following the same approach as described in the previous section, JSON-B can be configured using a `io.quarkus.jsonb.JsonbConfigCustomizer` bean.

If for example a custom serializer named `FooSerializer` for type `com.example.Foo` needs to be registered with JSON-B, the addition of a bean like the following would suffice:

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

A more advanced option would be to directly provide a bean of `jakarta.json.bind.JsonbConfig` (with a `Dependent` scope) or in the extreme case to provide a bean of type `jakarta.json.bind.Jsonb` (with a `Singleton` scope). If the latter approach is leveraged it is very important to manually inject and apply all `io.quarkus.jsonb.JsonbConfigCustomizer` beans in the CDI producer that produces `jakarta.json.bind.Jsonb`. Failure to do so will prevent JSON-B specific customizations provided by various extensions from being applied.

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

## Creating a frontend

Now let's add a simple web page to interact with our `FruitResource`. Quarkus automatically serves static resources located under the `META-INF/resources` directory. In the `src/main/resources/META-INF/resources` directory, add a `fruits.html` file with the content from this [fruits.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/rest-json-quickstart/src/main/resources/META-INF/resources/fruits.html) file in it.

You can now interact with your REST service:

\* start Quarkus with: +

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

\* open a browser to `http://localhost:8080/fruits.html` \* add new fruits to the list via the form

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

When using native executables with GraalVM, all classes that will be used with reflection need to be registered. The good news is that Quarkus does that work for you most of the time. So far, we haven't registered any class, not even `Fruit`, for reflection usage and everything is working fine.

Quarkus performs some magic when it is capable of inferring the serialized types from the REST methods. When you have the following REST method, Quarkus determines that `Fruit` will be serialized:

```JAVA
@GET
public List<Fruit> list() {
    // ...
}
```

Quarkus does that for you automatically by analyzing the REST methods at build time and that's why we didn't need any reflection registration in the first part of this guide.

Another common pattern in the Jakarta REST world is to use the `Response` object. `Response` comes with some nice perks:

\* you can return different entity types depending on what happens in your method (a `Legume` or an `Error` for instance); \* you can set the attributes of the `Response` (the status comes to mind in the case of an error).

Your REST method then looks like this:

```JAVA
@GET
public Response list() {
    // ...
}
```

It is not possible for Quarkus to determine at build time the type included in the `Response` as the information is not available. In this case, Quarkus won't be able to automatically register for reflection the required classes.

This leads us to our next section.

## Using Response

Let's create the `Legume` class which will be serialized as JSON, following the same model as for our `Fruit` class:

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

Now let's create a `LegumeResource` REST service with only one method which returns the list of legumes.

This method returns a `Response` and not a list of `Legume`.

```JAVA
package org.acme.rest.json;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/legumes")
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

Now let's add a simple web page to display our list of legumes. In the `src/main/resources/META-INF/resources` directory, add a `legumes.html` file with the content from this [legumes.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/rest-json-quickstart/src/main/resources/META-INF/resources/legumes.html) file in it.

Open a browser to <http://localhost:8080/legumes.html>, and you will see our list of legumes.

The interesting part starts when running the application as a native executable:

\* create the native executable with: +

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

\* execute it with `./target/rest-json-quickstart-1.0.0-SNAPSHOT-runner` \* open a browser and go to <http://localhost:8080/legumes.html>

No legumes there.

As mentioned above, the issue is that Quarkus was not able to determine the `Legume` class will require some reflection by analyzing the REST endpoints. The JSON serialization library tries to get the list of fields of `Legume` and gets an empty list, so it does not serialize the fields' data.

> [!NOTE]
> At the moment, when JSON-B or Jackson tries to get the list of fields of a class, if the class is not registered for reflection, no exception will be thrown. GraalVM will simply return an empty list of fields.
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

Let's do that and follow the same steps as before:

\* hit `Ctrl+C` to stop the application \* create the native executable with: +

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

\* execute it with `./target/rest-json-quickstart-1.0.0-SNAPSHOT-runner` \* open a browser and go to <http://localhost:8080/legumes.html>

This time, you can see our list of legumes.

<a id="reactive"></a>
## Being reactive

You can return *reactive types* to handle asynchronous processing. Quarkus recommends the usage of [Mutiny](https://smallrye.io/smallrye-mutiny) to write reactive and asynchronous code.

Quarkus REST is naturally integrated with Mutiny.

Your endpoints can return `Uni` or `Multi` instances:

```java

@GET
@Path("/{name}")
public Uni<Fruit> getOne(String name) {
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

## Conclusion

Creating JSON REST services with Quarkus is easy as it relies on proven and well known technologies.

As usual, Quarkus further simplifies things under the hood when running your application as a native executable.

There is only one thing to remember: if you use `Response` and Quarkus can't determine the beans that are serialized, you need to annotate them with `@RegisterForReflection`.

> [!TIP]
> As an alternative, you can use `org.jboss.resteasy.reactive.RestResponse<T>` instead of `Response`. Because `RestResponse` is strongly typed, Quarkus can determine the serialized type at build time and automatically registers it for reflection, eliminating the need for `@RegisterForReflection`.
