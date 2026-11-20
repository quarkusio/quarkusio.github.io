# Using Apache Kafka with Schema Registry and Avro

This guide shows how your Quarkus application can use Apache Kafka, [Avro](https://avro.apache.org/docs/current/) serialized records, and connect to a schema registry (such as the [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html) or [Apicurio Registry](https://www.apicur.io/registry/)).

If you are not familiar with Kafka and Kafka in Quarkus in particular, consider first going through the [Using Apache Kafka with Reactive Messaging](/guides/kafka/index.md) guide.

## Prerequisites

To complete this guide, you need:

- Roughly 30 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Docker and Docker Compose or [Podman](/guides/podman/index.md), and Docker Compose
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this guide we are going to implement a REST resource, namely `MovieResource`, that will consume movie DTOs and put them in a Kafka topic.

Then, we will implement a consumer that will consume and collect messages from the same topic. The collected messages will be then exposed by another resource, `ConsumedMovieResource`, via [Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events).

The *Movies* will be serialized and deserialized using Avro. The schema, describing the *Movie*, is stored in Apicurio Registry. The same concept applies if you are using the Confluent Avro *serde* and Confluent Schema Registry.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `kafka-avro-schema-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/kafka-avro-schema-quickstart).

## Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:kafka-avro-schema-quickstart \
    --extension='rest-jackson,messaging-kafka,apicurio-registry-avro' \
    --no-code
cd kafka-avro-schema-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=kafka-avro-schema-quickstart \
    -Dextensions='rest-jackson,messaging-kafka,apicurio-registry-avro' \
    -DnoCode
cd kafka-avro-schema-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=kafka-avro-schema-quickstart"`

> [!TIP]
> If you use Confluent Schema Registry, you don't need the `quarkus-apicurio-registry-avro` extension. Instead, you need the `quarkus-confluent-registry-avro` extension and a few more dependencies. See [Using the Confluent Schema Registry](#confluent) for details.

## Avro schema

Apache Avro is a data serialization system. Data structures are described using schemas. The first thing we need to do is to create a schema describing the `Movie` structure. Create a file called `src/main/avro/movie.avsc` with the schema for our record (Kafka message):

```json
{
  "namespace": "org.acme.kafka.quarkus",
  "type": "record",
  "name": "Movie",
  "fields": [
    {
      "name": "title",
      "type": "string"
    },
    {
      "name": "year",
      "type": "int"
    }
  ]
}
```

If you build the project with:

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

the `movies.avsc` will get compiled to a `Movie.java` file placed in the `target/generated-sources/avsc` directory.

Take a look at the [Avro specification](https://avro.apache.org/docs/current/specification/#schemas) to learn more about the Avro syntax and supported types.

> [!TIP]
> With Quarkus, there's no need to use a specific Maven plugin to process the Avro schema, this is all done for you by the `quarkus-avro` extension!

If you run the project with:

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

the changes you do to the schema file will be automatically applied to the generated Java files.

## The `Movie` producer

Having defined the schema, we can now jump to implementing the `MovieResource`.

Let's open the `MovieResource`, inject an [`Emitter`](https://quarkus.io/blog/reactive-messaging-emitter/) of `Movie` DTO and implement a `@POST` method that consumes `Movie` and sends it through the `Emitter`:

```java
package org.acme.kafka;

import org.acme.kafka.quarkus.Movie;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/movies")
public class MovieResource {
    private static final Logger LOGGER = Logger.getLogger(MovieResource.class);

    @Channel("movies")
    Emitter<Movie> emitter;

    @POST
    public Response enqueueMovie(Movie movie) {
        LOGGER.infof("Sending movie %s to Kafka", movie.getTitle());
        emitter.send(movie);
        return Response.accepted().build();
    }

}
```

Now, we need to *map* the `movies` channel (the `Emitter` emits to this channel) to a Kafka topic. To achieve this, edit the `application.properties` file, and add the following content:

```properties
# set the connector for the outgoing channel to `smallrye-kafka`
mp.messaging.outgoing.movies.connector=smallrye-kafka

# set the topic name for the channel to `movies`
mp.messaging.outgoing.movies.topic=movies

# automatically register the schema with the registry, if not present
mp.messaging.outgoing.movies.apicurio.registry.auto-register=true
```

> [!TIP]
> You might have noticed that we didn't define the `value.serializer`. That's because Quarkus can [autodetect](/guides/kafka/index.md#serialization-autodetection) that `io.apicurio.registry.serde.avro.AvroKafkaSerializer` is appropriate here, based on the `@Channel` declaration, structure of the `Movie` type, and presence of the Apicurio Registry libraries. We still have to define the `apicurio.registry.auto-register` property.
>
> If you use Confluent Schema Registry, you don't have to configure `value.serializer` either. It is also detected automatically. The Confluent Schema Registry analogue of `apicurio.registry.auto-register` is called `auto.register.schemas`. It defaults to `true`, so it doesn't have to be configured in this example. It can be explicitly set to `false` if you want to disable automatic schema registration.

## The `Movie` consumer

So, we can write records into Kafka containing our `Movie` data. That data is serialized using Avro. Now, it's time to implement a consumer for them.

Let's create `ConsumedMovieResource` that will consume `Movie` messages from the `movies-from-kafka` channel and will expose it via Server-Sent Events:

```java
package org.acme.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.kafka.quarkus.Movie;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.resteasy.reactive.RestStreamElementType;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
@Path("/consumed-movies")
public class ConsumedMovieResource {

    @Channel("movies-from-kafka")
    Multi<Movie> movies;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Multi<String> stream() {
        return movies.map(movie -> String.format("'%s' from %s", movie.getTitle(), movie.getYear()));
    }
}
```

The last bit of the application's code is the configuration of the `movies-from-kafka` channel in `application.properties`:

```properties
# set the connector for the incoming channel to `smallrye-kafka`
mp.messaging.incoming.movies-from-kafka.connector=smallrye-kafka

# set the topic name for the channel to `movies`
mp.messaging.incoming.movies-from-kafka.topic=movies

# disable auto-commit, Reactive Messaging handles it itself
mp.messaging.incoming.movies-from-kafka.enable.auto.commit=false

mp.messaging.incoming.movies-from-kafka.auto.offset.reset=earliest
```

> [!TIP]
> You might have noticed that we didn't define the `value.deserializer`. That's because Quarkus can [autodetect](/guides/kafka/index.md#serialization-autodetection) that `io.apicurio.registry.serde.avro.AvroKafkaDeserializer` is appropriate here, based on the `@Channel` declaration, structure of the `Movie` type, and presence of the Apicurio Registry libraries. We don't have to define the `apicurio.registry.use-specific-avro-reader` property either, that is also configured automatically.
>
> If you use Confluent Schema Registry, you don't have to configure `value.deserializer` or `specific.avro.reader` either. They are both detected automatically.

## Running the application

Start the application in dev mode:

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

Kafka broker and Apicurio Registry instance are started automatically thanks to Dev Services. See [Dev Services for Kafka](/guides/kafka-dev-services/index.md) and [Dev Services for Apicurio Registry](/guides/apicurio-registry-dev-services/index.md) for more details.

> [!TIP]
> You might have noticed that we didn't configure the schema registry URL anywhere. This is because Dev Services for Apicurio Registry configures all Kafka channels in Quarkus Messaging to use the automatically started registry instance.
>
> Apicurio Registry, in addition to its native API, also exposes an endpoint that is API-compatible with Confluent Schema Registry. Therefore, this automatic configuration works both for Apicurio Registry serde and Confluent Schema Registry serde.
>
> However, note that there's no Dev Services support for running Confluent Schema Registry itself. If you want to use a running instance of Confluent Schema Registry, configure its URL, together with the URL of a Kafka broker:
>
> ```properties
> kafka.bootstrap.servers=PLAINTEXT://localhost:9092
> mp.messaging.connector.smallrye-kafka.schema.registry.url=http://localhost:8081
> ```

In the second terminal, query the `ConsumedMovieResource` resource with `curl`:

```bash
curl -N http://localhost:8080/consumed-movies
```

In the third one, post a few movies:

```bash
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"title":"The Shawshank Redemption","year":1994}' \
  http://localhost:8080/movies

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"title":"The Godfather","year":1972}' \
  http://localhost:8080/movies

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"title":"The Dark Knight","year":2008}' \
  http://localhost:8080/movies

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"title":"12 Angry Men","year":1957}' \
  http://localhost:8080/movies
```

Observe what is printed in the second terminal. You should see something along the lines of:

```
data:'The Shawshank Redemption' from 1994

data:'The Godfather' from 1972

data:'The Dark Knight' from 2008

data:'12 Angry Men' from 1957
```

## Running in JVM or Native mode

When not running in dev or test mode, you will need to start your own Kafka broker and Apicurio Registry. The easiest way to get them running is to use `docker-compose` to start the appropriate containers.

> [!TIP]
> If you use Confluent Schema Registry, you already have a Kafka broker and Confluent Schema Registry instance running and configured. You can ignore the `docker-compose` instructions here, as well as the Apicurio Registry configuration.

Create a `docker-compose.yaml` file at the root of the project with the following content:

```yaml
services:

  kafka:
    image: quay.io/strimzi/kafka:latest-kafka-4.1.0
    command: [
      "sh", "-c",
      "./bin/kafka-storage.sh format --standalone -t $$(./bin/kafka-storage.sh random-uuid) -c ./config/server.properties && ./bin/kafka-server-start.sh ./config/server.properties"
    ]
    ports:
      - "9092:9092"
    environment:
      LOG_DIR: "/tmp/logs"

  schema-registry:
    image: quay.io/apicurio/apicurio-registry:3.1.7
    ports:
      - 8081:8080
    depends_on:
      - kafka
    environment:
      QUARKUS_PROFILE: prod
```

Before starting the application, let's first start the Kafka broker and Apicurio Registry:

```bash
docker-compose up
```

> [!NOTE]
> To stop the containers, use `docker-compose down`. You can also clean up the containers with `docker-compose rm`

You can build the application with:

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

And run it in JVM mode with:

```bash
java -Dmp.messaging.connector.smallrye-kafka.apicurio.registry.url=http://localhost:8081/apis/registry/v3 -jar target/quarkus-app/quarkus-run.jar
```

> [!NOTE]
> By default, the application tries to connect to a Kafka broker listening at `localhost:9092`. You can configure the bootstrap server using: `java -Dkafka.bootstrap.servers=\... -jar target/quarkus-app/quarkus-run.jar`

Specifying the registry URL on the command line is not very convenient, so you can add a configuration property only for the `prod` profile:

```properties
%prod.mp.messaging.connector.smallrye-kafka.apicurio.registry.url=http://localhost:8081/apis/registry/v3
```

You can build a native executable with:

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

and run it with:

```bash
./target/kafka-avro-schema-quickstart-1.0.0-SNAPSHOT-runner -Dkafka.bootstrap.servers=localhost:9092
```

## Testing the application

As mentioned above, Dev Services for Kafka and Apicurio Registry automatically start and configure a Kafka broker and Apicurio Registry instance in dev mode and for tests. Hence, we don't have to set up Kafka and Apicurio Registry ourselves. We can just focus on writing the test.

First, let's add test dependencies on REST Client and Awaitility to the build file:

**pom.xml**

```xml
<!-- we'll use Jakarta REST Client for talking to the SSE endpoint -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-rest-client")
testImplementation("org.awaitility:awaitility")
```

In the test, we will send movies in a loop and check if the `ConsumedMovieResource` returns what we send.

```java
package org.acme.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
public class MovieResourceTest {

    @TestHTTPResource("/consumed-movies")
    URI consumedMovies;

    @Test
    public void testHelloEndpoint() throws InterruptedException {
        // create a client for `ConsumedMovieResource` and collect the consumed resources in a list
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(consumedMovies);

        List<String> received = new CopyOnWriteArrayList<>();

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> received.add(inboundSseEvent.readData()));

        // in a separate thread, feed the `MovieResource`
        ExecutorService movieSender = startSendingMovies();

        source.open();

        // check if, after at most 5 seconds, we have at least 2 items collected, and they are what we expect
        await().atMost(5, SECONDS).until(() -> received.size() >= 2);
        assertThat(received, Matchers.hasItems("'The Shawshank Redemption' from 1994",
                "'12 Angry Men' from 1957"));
        source.close();

        // shutdown the executor that is feeding the `MovieResource`
        movieSender.shutdownNow();
        movieSender.awaitTermination(5, SECONDS);
    }

    private ExecutorService startSendingMovies() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            while (true) {
                given()
                        .contentType(ContentType.JSON)
                        .body("{\"title\":\"The Shawshank Redemption\",\"year\":1994}")
                .when()
                        .post("/movies")
                .then()
                        .statusCode(202);

                given()
                        .contentType(ContentType.JSON)
                        .body("{\"title\":\"12 Angry Men\",\"year\":1957}")
                .when()
                        .post("/movies")
                .then()
                        .statusCode(202);

                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        return executorService;
    }

}
```

> [!NOTE]
> We modified the `MovieResourceTest` that was generated together with the project. This test class has a subclass, `NativeMovieResourceIT`, that runs the same test against the native executable. To run it, execute:

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

### Manual setup

If we couldn't use Dev Services and wanted to start a Kafka broker and Apicurio Registry instance manually, we would define a [QuarkusTestResourceLifecycleManager](/guides/getting-started-testing/index.md#quarkus-test-resource).

**pom.xml**

```xml
<dependency>
    <groupId>io.strimzi</groupId>
    <artifactId>strimzi-test-container</artifactId>
    <version>0.112.0</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.strimzi:strimzi-test-container:0.112.0") {
    exclude group: "org.apache.logging.log4j", module: "log4j-core"
}
```

```java
package org.acme.kafka;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaCluster;

public class KafkaAndSchemaRegistryTestResource implements QuarkusTestResourceLifecycleManager {

    private final StrimziKafkaCluster kafka = new StrimziKafkaCluster.StrimziKafkaClusterBuilder().build();

    private GenericContainer<?> registry;

    @Override
    public Map<String, String> start() {
        kafka.start();
        registry = new GenericContainer<>("apicurio/apicurio-registry:3.0.7")
                .withExposedPorts(8080)
                .withEnv("QUARKUS_PROFILE", "prod");
        registry.start();
        Map<String, String> properties = new HashMap<>();
        properties.put("mp.messaging.connector.smallrye-kafka.apicurio.registry.url",
                "http://" + registry.getHost() + ":" + registry.getMappedPort(8080) + "/apis/registry/v3");
        properties.put("kafka.bootstrap.servers", kafka.getBootstrapServers());
        return properties;
    }

    @Override
    public void stop() {
        registry.stop();
        kafka.stop();
    }
}
```

```java
@QuarkusTest
@QuarkusTestResource(KafkaAndSchemaRegistryTestResource.class)
public class MovieResourceTest {
    ...
}
```

## Migrating from Apicurio Registry 2.x to 3.x

> [!IMPORTANT]
> Apicurio Registry 3.x introduces a **breaking change in schema ID format** from 8-byte (long) to 4-byte (int) identifiers. This affects message compatibility between v2 and v3 producers/consumers.

### Schema ID Format Change

Apicurio Registry 3.x changed the schema ID format from **8-byte (long)** to **4-byte (int)** identifiers. This means messages produced with v2 cannot be consumed by v3 applications (and vice versa) without explicit configuration.

### Migration Scenarios

**New applications (no existing v2 messages):** No configuration needed. The v3 defaults will be used automatically.

**Consuming existing v2 messages:** Configure the `Legacy8ByteIdHandler` for channels that need to read v2-produced messages:

```properties
# Per-channel configuration for consuming v2 messages
mp.messaging.incoming.my-channel.apicurio.registry.id-handler=io.apicurio.registry.serde.Legacy8ByteIdHandler

# Or configure globally for all channels
mp.messaging.connector.smallrye-kafka.apicurio.registry.id-handler=io.apicurio.registry.serde.Legacy8ByteIdHandler
```

**Producing v2-compatible messages:** If downstream consumers still use v2, configure the producer:

```properties
mp.messaging.outgoing.my-channel.apicurio.registry.id-handler=io.apicurio.registry.serde.Legacy8ByteIdHandler
```

### Standard ID Handlers: Fixed Format

Both v2 (8-byte) and v3 (4-byte) formats use the same magic byte. The standard ID handlers (`Legacy8ByteIdHandler` and `Default4ByteIdHandler`) read a fixed number of bytes based on configuration—there is no per-message auto-detection.

> [!WARNING]
> If a single topic contains mixed messages (some with 8-byte IDs, some with 4-byte IDs), consumers using standard ID handlers will fail. A consumer configured with `Legacy8ByteIdHandler` always reads 8 bytes; one with `Default4ByteIdHandler` always reads 4 bytes. Mismatches cause corruption or deserialization errors.

**With standard ID handlers, gradual migration only works when:**

- Each topic has messages in ONE format (either v2 or v3, not both)
- Per-channel configuration routes each topic to the correct ID handler

You can have producers and consumers using both formats in the same Apicurio Registry instance, but not mixing them in the same topic.

> [!TIP]
> For topics with mixed v2/v3 messages, consider using the `OptimisticFallbackIdHandler` described in the [\[Optimistic Fallback ID Handler\]](<#Optimistic Fallback ID Handler>) section.

### Migration Paths for Topics with Existing v2 Messages

For topics that already contain v2 messages, valid migration approaches are:

1. **Stop and drain:** Stop producers → drain topic completely → upgrade all services → restart with v3 configuration
2. **New topic migration:** Create a new v3 topic and migrate traffic to it
3. **Accept transient failures:** Accept transient failures during the switchover period
4. **Optimistic fallback handler:** Use the `OptimisticFallbackIdHandler` for gradual migration (see below)

### Optimistic Fallback ID Handler

Apicurio Registry provides an `OptimisticFallbackIdHandler` that can help with gradual migration from v2 to v3. This handler:

- **Writes** new messages with 4-byte (v3) IDs
- **Reads** both 4-byte (v3) and 8-byte (v2) IDs

```properties
# Configure the optimistic fallback handler for migration
mp.messaging.connector.smallrye-kafka.apicurio.registry.id-handler=io.apicurio.registry.serde.OptimisticFallbackIdHandler

# Or per-channel
mp.messaging.incoming.my-channel.apicurio.registry.id-handler=io.apicurio.registry.serde.OptimisticFallbackIdHandler
```

> [!IMPORTANT]
> The `OptimisticFallbackIdHandler` makes the following assumption to distinguish between 4-byte and 8-byte IDs: schema IDs are greater than 0 and smaller than the maximum integer value. This is typically true for most use cases, but you should verify your schema IDs meet this constraint before using this handler.

This approach enables a gradual migration where:

1. Upgrade consumers first with `OptimisticFallbackIdHandler` (they can read both v2 and v3 messages)
2. Then upgrade producers (they start writing v3 format)
3. Once all v2 messages are consumed, optionally switch to `Default4ByteIdHandler`

### Compatibility with Apicurio Registry 2.x Server

The Apicurio Registry 3.x client libraries continue to work with Apicurio Registry 2.x servers. However, note that Apicurio Registry 2.x is no longer actively maintained.

### Additional Resources

- [Apicurio Registry 3.x Documentation](https://www.apicur.io/registry/docs/apicurio-registry/3.0.x/index.html)
- [Apicurio Registry Deployment Migration Guide](https://www.apicur.io/registry/docs/apicurio-registry/3.1.x/getting-started/assembly-migrating-registry-v2-v3.html)
- [Official Apicurio Migration Guide](https://www.apicur.io/blog/2025/03/30/migrate-registry-2-to-3)
- [Apicurio SerDes Evolution Blog Post](https://www.apicur.io/blog/2025/04/03/evolving-serialization)

<a id="confluent"></a>
## Using the Confluent Schema Registry

If you want to use the Confluent Schema Registry, you need the `quarkus-confluent-registry-avro` extension, instead of the `quarkus-apicurio-registry-avro` extension. Also, you need to add a few dependencies and a custom Maven repository to your `pom.xml` / `build.gradle` file:

**pom.xml**

```xml
<dependencies>
    ...
    <!-- the extension -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-confluent-registry-avro</artifactId>
    </dependency>
    <!-- Confluent registry libraries use Jakarta REST client -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest-client</artifactId>
    </dependency>
    <dependency>
        <groupId>io.confluent</groupId>
        <artifactId>kafka-avro-serializer</artifactId>
        <version>7.2.0</version>
        <exclusions>
            <exclusion>
                <groupId>jakarta.ws.rs</groupId>
                <artifactId>jakarta.ws.rs-api</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>

<repositories>
    <!-- io.confluent:kafka-avro-serializer is only available from this repository: -->
    <repository>
        <id>confluent</id>
        <url>https://packages.confluent.io/maven/</url>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
    </repository>
</repositories>
```

**build.gradle**

```gradle
repositories {
    ...

    maven {
        url "https://packages.confluent.io/maven/"
    }
}

dependencies {
    ...

    implementation("io.quarkus:quarkus-confluent-registry-avro")

    // Confluent registry libraries use Jakarta REST client
    implementation("io.quarkus:quarkus-rest-client")

    implementation("io.confluent:kafka-avro-serializer:7.2.0") {
        exclude group: "jakarta.ws.rs", module: "jakarta.ws.rs-api"
    }
}
```

In JVM mode, any version of `io.confluent:kafka-avro-serializer` can be used. In native mode, Quarkus supports the following versions: `6.2.x`, `7.0.x`, `7.1.x`, `7.2.x`, `7.3.x`.

For versions `7.4.x` and later, due to an issue with the Confluent Schema Serializer, you need to add another dependency:

**pom.xml**

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-csv</artifactId>
</dependency>
```

**build.gradle**

```gradle
dependencies {
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
}
```

For any other versions, the native configuration may need to be adjusted.

## Avro code generation details

In this guide we used the Quarkus code generation mechanism to generate Java files from Avro schema.

Under the hood, the mechanism uses `org.apache.avro:avro-compiler`.

You can use the following configuration properties to alter how it works:

- `avro.codegen.[avsc|avdl|avpr].imports` - a list of files or directories that should be compiled first thus making them importable by subsequently compiled schemas. Note that imported files should not reference each other. All paths should be relative to the `src/[main|test]/avro` directory, or `avro` sub-directory in any source directory configured by the build system. Passed as a comma-separated list.
- `avro.codegen.stringType` - the Java type to use for Avro strings. May be one of `CharSequence`, `String` or `Utf8`. Defaults to `String`
- `avro.codegen.createOptionalGetters` - enables generating the `getOptional...` methods that return an Optional of the requested type. Defaults to `false`
- `avro.codegen.enableDecimalLogicalType` - determines whether to use Java classes for decimal types, defaults to `false`
- `avro.codegen.createSetters` - determines whether to create setters for the fields of the record. Defaults to `false`
- `avro.codegen.gettersReturnOptional` - enables generating `get...` methods that return an Optional of the requested type. Defaults to `false`
- `avro.codegen.optionalGettersForNullableFieldsOnly`, works in conjunction with `gettersReturnOptional` option. If it is set, `Optional` getters will be generated only for fields that are nullable. If the field is mandatory, regular getter will be generated. Defaults to `false`

## Avro deserialization and class security

Starting with Apache Avro 1.12.2, Avro validates every class against a global security validator before it is deserialized. If a class is not trusted, deserialization fails with a `java.lang.SecurityException`.

The `quarkus-avro` extension (pulled in transitively by `quarkus-apicurio-registry-avro` and `quarkus-confluent-registry-avro`) automatically installs a validator that trusts:

- all Avro-generated classes (those annotated with `@org.apache.avro.specific.AvroGenerated`),
- classes that other extensions register as trusted — for example the Protobuf-generated messages contributed by the `quarkus-grpc` and `quarkus-messaging-pulsar` extensions, which may be deserialized through Avro, and
- Avro's built-in trusted types (such as the boxed primitives, `String`, `BigDecimal` and `BigInteger`).

Because generated classes are trusted out of the box, no configuration is required for the typical use case shown in this guide. If you deserialize additional, non-generated classes, you can trust them explicitly:

```properties
# Trust individual classes by their fully qualified name
quarkus.avro.trusted-classes=org.acme.MyClass,org.acme.OtherClass

# Trust whole packages (and their sub-packages)
quarkus.avro.trusted-packages=org.acme.model,org.acme.dto
```

> [!NOTE]
> Package matching respects package boundaries. Configuring `org.acme` trusts `org.acme.Foo` and classes in any sub-package such as `org.acme.sub.Bar`, but does not trust `org.acme2.Foo`.

> [!NOTE]
> `quarkus.avro.trusted-classes` matches classes by their exact name. A nested class must therefore be listed by its binary name using `$` (for example `org.acme.Outer$Inner`); trusting its enclosing class does not trust the nested class. Trusting the class's package, on the other hand, does cover its nested classes.

Both properties are build-time properties, so changing them requires rebuilding the application. Avro's own `org.apache.avro.SERIALIZABLE_CLASSES` and `org.apache.avro.SERIALIZABLE_PACKAGES` system properties remain available as an additional runtime escape hatch.

> [!TIP]
> If you are developing an extension that generates or handles classes deserialized through Avro, add a dependency on `quarkus-avro-spi` and produce an `io.quarkus.avro.spi.AvroTrustedClassBuildItem` for those classes from a build step. They will then be trusted automatically whenever the `quarkus-avro` extension is present, without your extension having to depend on it directly.

## Further reading

- [SmallRye Reactive Messaging Kafka](https://smallrye.io/smallrye-reactive-messaging/smallrye-reactive-messaging/3.4/kafka/kafka.html) documentation
- [How to Use Kafka, Schema Registry and Avro with Quarkus](https://quarkus.io/blog/kafka-avro/) - a blog post on which the guide is based. It gives a good introduction to Avro and the concept of schema registry
