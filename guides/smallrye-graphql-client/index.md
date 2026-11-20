# SmallRye GraphQL Client

This guide demonstrates how your Quarkus application can use the GraphQL client library. The client is implemented by the [SmallRye GraphQL](https://github.com/smallrye/smallrye-graphql/) project. This guide is specifically geared towards the client side, so if you need an introduction to GraphQL in general, first refer to the [SmallRye GraphQL guide](/guides/smallrye-graphql/index.md), which provides an introduction to the GraphQL query language, general concepts and server-side development.

The guide will walk you through developing and running a simple application that uses both supported types of GraphQL clients to retrieve data from a remote resource, that being a database related to Star Wars. It's available at [this webpage](https://graphql.org/swapi-graphql) if you want to experiment with it manually. The web UI allows you to write and execute GraphQL queries against it.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## GraphQL client types introduction

Two types of GraphQL clients are supported.

The **typesafe** client works very much like the MicroProfile REST Client adjusted for calling GraphQL endpoints. A client instance is basically a proxy that you can call like a regular Java object, but under the hood, the call will be translated to a GraphQL operation. It works with domain classes directly. Any input and output objects for the operation will be translated to/from their representations in the GraphQL query language.

The **dynamic** client, on the other hand, works rather like an equivalent of the Jakarta REST client from the `jakarta.ws.rs.client` package. It does not require the domain classes to work, it works with abstract representations of GraphQL documents instead. Documents are built using a domain-specific language (DSL). The exchanged objects are treated as an abstract `JsonObject`, but, when necessary, it is possible to convert them to concrete model objects (if suitable model classes are available).

The typesafe client can be viewed as a rather high-level and more declarative approach designed for ease of use, whereas the dynamic client is lower-level, more imperative, somewhat more verbose to use, but allows finer grained control over operations and responses.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `microprofile-graphql-client-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/microprofile-graphql-client-quickstart).

## Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:microprofile-graphql-client-quickstart \
    --extension='rest-jsonb,graphql-client' \
    --no-code
cd microprofile-graphql-client-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=microprofile-graphql-client-quickstart \
    -Dextensions='rest-jsonb,graphql-client' \
    -DnoCode
cd microprofile-graphql-client-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=microprofile-graphql-client-quickstart"`

This command generates a project, importing the `smallrye-graphql-client` extension, and also the `rest-jsonb` extension, because we will use that too - a REST endpoint will be the entrypoint to allow you to manually trigger the GraphQL client to do its work.

If you already have your Quarkus project configured, you can add the `smallrye-graphql-client` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add graphql-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='graphql-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='graphql-client'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-graphql-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-smallrye-graphql-client")
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

## The application

The application we will build makes use of both types of GraphQL clients. In both cases, they will connect to the Star Wars service at [SWAPI](https://graphql.org/swapi-graphql) and query it for a list of Star Wars films, and, for each film, the names of the planets which appear in that film.

The corresponding GraphQL query looks like this:

```
{
  allFilms {
    films {
      title
      planetConnection {
        planets {
          name
        }
      }
    }
  }
}
```

You may go to [the webpage](https://graphql.org/swapi-graphql) to execute this query manually.

## Using the Typesafe client

To use the typesafe client, we need the corresponding model classes that are compatible with the schema. There are two ways to obtain them. First is to  use the client generator offered by SmallRye GraphQL, which generates classes from the schema document and a list of queries. This generator is considered highly experimental for now, and is not covered in this example. If interested, refer to the [Client Generator](https://github.com/smallrye/smallrye-graphql/tree/main/client/generator) and its documentation.

In this example, we will create a slimmed down version of the model classes manually, with only the fields that we need, and ignore all the stuff that we don't need. We will need the classes for `Film` and `Planet`. But, the service is also using specific wrappers named `FilmConnection` and `PlanetConnection`, which, for our purpose, will serve just to contain the actual list of `Film` and `Planet` instances, respectively.

Let's create all the model classes and put them into the `org.acme.microprofile.graphql.client.model` package:

```java
public class FilmConnection {

    private List<Film> films;

    public List<Film> getFilms() {
        return films;
    }

    public void setFilms(List<Film> films) {
        this.films = films;
    }
}

public class Film {

    private String title;

    private PlanetConnection planetConnection;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PlanetConnection getPlanetConnection() {
        return planetConnection;
    }

    public void setPlanetConnection(PlanetConnection planetConnection) {
        this.planetConnection = planetConnection;
    }
}

public class PlanetConnection {

    private List<Planet> planets;

    public List<Planet> getPlanets() {
        return planets;
    }

    public void setPlanets(List<Planet> planets) {
        this.planets = planets;
    }

}

public class Planet {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

Now that we have the model classes, we can create the interface that represents the actual set of operations we want to call on the remote GraphQL service.

```java
@GraphQLClientApi(configKey = "star-wars-typesafe")
public interface StarWarsClientApi {

    FilmConnection allFilms();

}
```

For simplicity, we're only calling the query named `allFilms`. We named our corresponding method `allFilms` too. If we named the method differently, we would need to annotate it with `@Query(value="allFilms")` to specify the name of the query that should be executed when this method is called.

The client also needs some configuration, namely at least the URL of the remote service. We can either specify that within the `@GraphQLClientApi` annotation (by setting the `endpoint` parameter), or move this over to the configuration file, `application.properties`:

```
quarkus.smallrye-graphql-client.star-wars-typesafe.url=https://swapi-graphql.netlify.app/graphql
```

> [!NOTE]
> During **tests only**, the URL is an optional property, and if it's not specified, Quarkus will assume that the target of the client is the application that is being tested (typically, `http://localhost:8081/graphql`). This is useful if your application contains a GraphQL server-side API as well as a GraphQL client that is used for testing the API.

If you need to add an authorization header, or any other custom HTTP header (in our case it's not required), this can be done with a configuration in the configuration file as well:

```
quarkus.smallrye-graphql-client.star-wars-typesafe.header.HEADER-KEY=HEADER-VALUE
```

`star-wars-typesafe` is the name of the configured client instance, and corresponds to the `configKey` in the `@GraphQLClientApi` annotation. If you don't want to specify a custom name, you can leave out the `configKey`, and then refer to it by using the fully qualified name of the interface.

Now that we have the client instance properly configured, we need a way to have it perform something when we start the application. For that, we will use a REST endpoint that, when called by a user, obtains the client instance and lets it execute the query.

```java
@Path("/")
public class StarWarsResource {
    @Inject
    StarWarsClientApi typesafeClient;

    @GET
    @Path("/typesafe")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public List<Film> getAllFilmsUsingTypesafeClient() {
        return typesafeClient.allFilms().getFilms();
    }
}
```

With this REST endpoint included in your application, you can simply send a GET request to `/typesafe`, and the application will use an injected typesafe client instance to call the remote service, obtain the films and planets, and return the JSON representation of the resulting list.

### Logging

For debugging purpose, it is possible to log the request generated by the typesafe client and the response sent back by the server by changing the log level of the `io.smallrye.graphql.client` category to `TRACE` (see the [Logging guide](/guides/logging/index.md#configure-the-log-level-category-and-format) for more details about how to configure logging).

This can be achieved by adding the following lines to the `application.properties`:

```properties
quarkus.log.category."io.smallrye.graphql.client".level=TRACE
quarkus.log.category."io.smallrye.graphql.client".min-level=TRACE
```

## Using the Dynamic client

For the dynamic client, the model classes are optional, because we can work with abstract representations of the GraphQL types and documents. The client API interface is not needed at all.

We still need to configure the URL for the client, so let's put this into `application.properties`:

```
quarkus.smallrye-graphql-client.star-wars-dynamic.url=https://swapi-graphql.netlify.app/graphql
```

We decided to name the client `star-wars-dynamic`. We will use this name when injecting a dynamic client to properly qualify the injection point.

If you need to add an authorization header, or any other custom HTTP header (in our case it's not required), this can be done by:

```
quarkus.smallrye-graphql-client.star-wars-dynamic.header.HEADER-KEY=HEADER-VALUE
```

Add this to the `StarWarsResource` created earlier:

```java
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Operation.operation;

// ....

@Inject
@GraphQLClient("star-wars-dynamic")    // <1>
DynamicGraphQLClient dynamicClient;

@GET
@Path("/dynamic")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public List<Film> getAllFilmsUsingDynamicClient() throws Exception {
    Document query = document(   // <2>
        operation(
            field("allFilms",
                field("films",
                    field("title"),
                    field("planetConnection",
                        field("planets",
                            field("name")
                        )
                    )
                )
            )
        )
    );
    Response response = dynamicClient.executeSync(query); <3>
    return response.getObject(FilmConnection.class, "allFilms").getFilms(); <4>
}
```

1. Qualifies the injection point so that we know which named client needs to be injected here.
2. Here we build a document representing the GraphQL query, using the provided DSL language. We use static imports to make the code easier to read. The DSL is designed in a way that it looks quite similar to writing a GraphQL query as a string.
3. Execute the query and block while waiting for the response. There is also an asynchronous variant that returns a `Uni<Response>`.
4. Here we did the optional step of converting the response to instances of our model classes, because we have the classes available. If you don't have the classes available or don't want to use them, simply calling `response.getData()` would get you a `JsonObject` representing all the returned data.

## Running the application

Launch the application in dev mode using:

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

To execute the queries, you need to send GET requests to our REST endpoint:

```bash
curl -s http://localhost:8080/dynamic # to use the dynamic client
curl -s http://localhost:8080/typesafe # to use the typesafe client
```

Whether you use dynamic or typesafe, the result should be the same. If the JSON document is hard to read, you might want to run it through a tool that formats it for better readability by humans, for example by piping the output through `jq`.

## Conclusion

This example showed how to use both the dynamic and typesafe GraphQL clients to call an external GraphQL service and explained the difference between the client types.

## References

- [Integrating OIDC clients into GraphQL clients](/guides/security-openid-connect-client-reference/index.md#quarkus-oidc-client-graphql)

- [Upstream SmallRye GraphQL Client documentation](https://smallrye.io/smallrye-graphql/latest/)
