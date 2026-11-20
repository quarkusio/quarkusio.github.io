# Using the MongoDB Client

MongoDB is a well known NoSQL Database that is widely used.

In this guide, we see how you can get your REST services to use the MongoDB database.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- MongoDB installed or Docker installed

## Architecture

The application built in this guide is quite simple: the user can add elements in a list using a form and the list is updated.

All the information between the browser and the server is formatted as JSON.

The elements are stored in MongoDB.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `mongodb-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/mongodb-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:mongodb-quickstart \
    --extension='rest-jackson,mongodb-client' \
    --no-code
cd mongodb-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=mongodb-quickstart \
    -Dextensions='rest-jackson,mongodb-client' \
    -DnoCode
cd mongodb-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=mongodb-quickstart"`

This command generates a Maven structure importing the Quarkus REST (formerly RESTEasy Reactive) Jackson and MongoDB Client extensions. After this, the `quarkus-mongodb-client` extension has been added to your build file.

If you already have your Quarkus project configured, you can add the `mongodb-client` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add mongodb-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='mongodb-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='mongodb-client'
```

This will add the following to your `pom.xml`:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-mongodb-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-mongodb-client")
```

## Creating your first JSON REST service

In this example, we will create an application to manage a list of fruits.

First, let's create the `Fruit` bean as follows:

```java
package org.acme.mongodb;

import java.util.Objects;

public class Fruit {

    private String name;
    private String description;
    private String id;

    public Fruit() {
    }

    public Fruit(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Fruit)) {
            return false;
        }

        Fruit other = (Fruit) obj;

        return Objects.equals(other.name, this.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
```

Nothing fancy. One important thing to note is that having a default constructor is required by the JSON serialization layer.

Now create a `org.acme.mongodb.FruitService` that will be the business layer of our application and store/load the fruits from the mongoDB database.

```java
package org.acme.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class FruitService {

    @Inject MongoClient mongoClient;

    public List<Fruit> list(){
        List<Fruit> list = new ArrayList<>();
        MongoCursor<Document> cursor = getCollection().find().iterator();

        try {
            while (cursor.hasNext()) {
                Document document = cursor.next();
                Fruit fruit = new Fruit();
                fruit.setName(document.getString("name"));
                fruit.setDescription(document.getString("description"));
                list.add(fruit);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public void add(Fruit fruit){
        Document document = new Document()
                .append("name", fruit.getName())
                .append("description", fruit.getDescription());
        getCollection().insertOne(document);
    }

    private MongoCollection getCollection(){
        return mongoClient.getDatabase("fruit").getCollection("fruit");
    }
}
```

Now, create the `org.acme.mongodb.FruitResource` class as follows:

```java
@Path("/fruits")
public class FruitResource {

    @Inject FruitService fruitService;

    @GET
    public List<Fruit> list() {
        return fruitService.list();
    }

    @POST
    public List<Fruit> add(Fruit fruit) {
        fruitService.add(fruit);
        return list();
    }
}
```

The implementation is pretty straightforward, and you just need to define your endpoints using the Jakarta REST annotations and use the `FruitService` to list/add new fruits.

## Configuring the MongoDB database

The main property to configure is the URL to access the MongoDB instance. Almost all configuration can be included in the connection URI, so we advise you to do so. You can find more information in the MongoDB documentation: <https://docs.mongodb.com/manual/reference/connection-string/>

A sample configuration should look like this:

```properties
# configure the mongoDB client for a replica set of two nodes
quarkus.mongodb.connection-string = mongodb://mongo1:27017,mongo2:27017
```

In this example, we are using a single instance running on localhost:

```properties
# configure the mongoDB client for a single instance on localhost
quarkus.mongodb.connection-string = mongodb://localhost:27017
```

### Resolving mongo+srv:// URLs

When using `mongo+srv://` URLs, the MongoDB driver resolves DNS SRV records to retrieve the list of hosts to connect to. By default, it relies on a DNS client implemented using JNDI.

For security reasons, Quarkus restricts JNDI usage within applications to help mitigate potential vulnerabilities like Log4Shell. To support `mongo+srv://`, Quarkus provides an alternative DNS client.

However, in some environments, this alternative DNS client may be unable to resolve `SRV` records. If you encounter such issues, you can revert to the MongoDB driver’s default DNS client by setting the following properties:

```properties
quarkus.naming.enable-jndi=true
quarkus.class-loading.removed-resources."io.quarkus\:quarkus-mongodb-client"=META-INF/services/com.mongodb.spi.dns.DnsClientProvider
```

This restores the default JNDI-based resolution mechanism used by the MongoDB driver.

<a id="dev-services"></a>
### Use the MongoDB Dev Services

See [MongoDB Dev Services](/guides/mongodb-dev-services/index.md).

## Multiple MongoDB Clients

MongoDB allows you to configure multiple clients. Using several clients works the same way as having a single client.

```properties
quarkus.mongodb.connection-string = mongodb://login:pass@mongo1:27017/database

quarkus.mongodb.users.connection-string = mongodb://mongo2:27017/userdb
quarkus.mongodb.inventory.connection-string = mongodb://mongo3:27017/invdb,mongo4:27017/invdb
```

Notice there's an extra bit in the key (the `users` and `inventory` segments). The syntax is as follows: `quarkus.mongodb.[optional name.][mongo connection property]`. If the name is omitted, it configures the default client.

> [!NOTE]
> The use of multiple MongoDB clients enables multi-tenancy for MongoDB by allowing to connect to multiple MongoDB clusters.\
> If you want to connect to multiple databases inside the same cluster, multiple clients are **not** necessary as a single client is able to access all databases in the same cluster (as a JDBC connection is able to access to multiple schemas inside the same database).

### Named Mongo client Injection

When using multiple clients, each `MongoClient`, you can select the client to inject using the `io.quarkus.mongodb.MongoClientName` qualifier. Using the above properties to configure three different clients, you can also inject each one as follows:

```java
@Inject
MongoClient defaultMongoClient;

@Inject
@MongoClientName("users")
MongoClient mongoClient1;

@Inject
@MongoClientName("inventory")
ReactiveMongoClient mongoClient2;
```

### Activate or deactivate Mongo Clients

When a Mongo Client is configured at build time, and its URL is set at runtime, it is active by default. Quarkus starts the corresponding Mongo Client when the application starts.

To deactivate a Mongo Client at runtime, either:

- Do not set `quarkus.mongodb[.optional name].hosts` or `quarkus.mongodb[.optional name].connection-#string#`.
- Set `quarkus.mongodb[.optional name].active` to `false`.

If a Mongo Client is not active:

- The Mongo Client does not attempt to connect to Mongo during application startup.
- The Mongo Client does not contribute a [health check](#mongo-health-check).
- Static CDI injection points involving the Mongo Client, such as `@Inject ReactiveMongoClient mongoClient` or `@Inject MongoClient mongoClient`, cause application startup to fail.
- Dynamic retrieval of the Mongo Client, such as through `CDI.getBeanContainer()`, `Arc.instance()`, or an injected `Instance<ReactiveMongoClient>`, causes an exception to be thrown.
- Other Quarkus extensions that consume the Mongo Client may cause application startup to fail.

This feature is especially useful when the application must dynamically select a Mongo Client from a predefined set at runtime.

**An example of configuring multiple Mongo Clients for runtime selection:**

```properties
quarkus.mongodb.one.active=false
quarkus.mongodb.one.connection-string=mongodb://127.0.0.1:27018
quarkus.mongodb.two.active=false
quarkus.mongodb.two.connection-string=mongodb://127.0.0.1:27019
```

```java
import io.quarkus.arc.InjectableInstance;

import com.mongodb.client.MongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;

@ApplicationScoped
public class MyConsumer {
    @Inject
    @MongoClientName("one")
    InjectableInstance<ReactiveMongoClient> one;
    @Inject
    @MongoClientName("two")
    InjectableInstance<MongoClient> two;
    public void doSomething() {
        ReactiveMongoClient mongoClient = one.getActive();
        // ...
    }
}
```

Setting `quarkus.mongodb.one.active=true` [at runtime](/guides/config-reference/index.md#configuration-sources) makes only the Mongo Client `one` available. Setting `quarkus.mongodb.two.active=true` at runtime makes only the Mongo Client `two` available.

> [!IMPORTANT]
> A Mongo Client (either default or named) must always be discoverable at build-time to be considered for runtime injection. This can be done by injecting the Mongo Client name with `@MongoClientName`. If the Mongo Client name may be active or inactive, it needs to use the wrapper `InjectableInstance<>`, or else Quarkus will throw an exception at startup time if the Mongo Client is inactive. Alternatively, Mongo Clients may also be discovered via configuration.

## Running a MongoDB Database

As by default, `MongoClient` is configured to access a local MongoDB database on port 27017 (the default MongoDB port), if you have a local running database on this port, there is nothing more to do before being able to test it!

If you want to use Docker to run a MongoDB database, you can use the following command to launch one:

```bash
docker run -ti --rm -p 27017:27017 docker.io/library/mongo:7.0
```

> [!NOTE]
> If you use [Dev Services](#dev-services), launching the container manually is not necessary.

## Creating a frontend

Now let's add a simple web page to interact with our `FruitResource`. Quarkus automatically serves static resources located under the `META-INF/resources` directory. In the `src/main/resources/META-INF/resources` directory, add a `fruits.html` file with the content from this [fruits.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/mongodb-quickstart/src/main/resources/META-INF/resources/fruits.html) file in it.

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

<a id="reactive"></a>
## Reactive MongoDB Client

A reactive MongoDB Client is included in Quarkus. Using it is as easy as using the classic MongoDB Client. You can rewrite the previous example to use it like the following.

> [!TIP]
> **Mutiny**
>
> The MongoDB reactive client uses Mutiny reactive types. If you are not familiar with Mutiny, check [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

```java
package org.acme.mongodb;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;
import org.bson.Document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ReactiveFruitService {

    @Inject
    ReactiveMongoClient mongoClient;

    public Uni<List<Fruit>> list() {
        return getCollection().find()
                .map(doc -> {
                    Fruit fruit = new Fruit();
                    fruit.setName(doc.getString("name"));
                    fruit.setDescription(doc.getString("description"));
                    return fruit;
                }).collect().asList();
    }

    public Uni<Void> add(Fruit fruit) {
        Document document = new Document()
                .append("name", fruit.getName())
                .append("description", fruit.getDescription());
        return getCollection().insertOne(document)
                .onItem().ignore().andContinueWithNull();
    }

    private ReactiveMongoCollection<Document> getCollection() {
        return mongoClient.getDatabase("fruit").getCollection("fruit");
    }
}
```

```java
package org.acme.mongodb;

import io.smallrye.mutiny.Uni;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;

@Path("/reactive_fruits")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactiveFruitResource {

    @Inject
    ReactiveFruitService fruitService;

    @GET
    public Uni<List<Fruit>> list() {
        return fruitService.list();
    }

    @POST
    public Uni<List<Fruit>> add(Fruit fruit) {
        return fruitService.add(fruit)
                .onItem().ignore().andSwitchTo(this::list);
    }
}
```

## Simplifying MongoDB Client usage using BSON codec

By using a Bson `Codec`, the MongoDB Client will take care of the transformation of your domain object to/from a MongoDB `Document` automatically.

First you need to create a Bson `Codec` that will tell Bson how to transform your entity to/from a MongoDB `Document`. Here we use a `CollectibleCodec` as our object is retrievable from the database (it has a MongoDB identifier), if not we would have used a `Codec` instead. More information in the codec documentation: <https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/codecs/>.

```java
package org.acme.mongodb.codec;

import com.mongodb.MongoClientSettings;
import org.acme.mongodb.Fruit;
import org.bson.Document;
import org.bson.BsonWriter;
import org.bson.BsonValue;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.UUID;

public class FruitCodec implements CollectibleCodec<Fruit> {

    private final Codec<Document> documentCodec;

    public FruitCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, Fruit fruit, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("name", fruit.getName());
        doc.put("description", fruit.getDescription());
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<Fruit> getEncoderClass() {
        return Fruit.class;
    }

    @Override
    public Fruit generateIdIfAbsentFromDocument(Fruit document) {
        if (!documentHasId(document)) {
            document.setId(UUID.randomUUID().toString());
        }
        return document;
    }

    @Override
    public boolean documentHasId(Fruit document) {
        return document.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(Fruit document) {
        return new BsonString(document.getId());
    }

    @Override
    public Fruit decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        Fruit fruit = new Fruit();
        if (document.getString("id") != null) {
            fruit.setId(document.getString("id"));
        }
        fruit.setName(document.getString("name"));
        fruit.setDescription(document.getString("description"));
        return fruit;
    }
}
```

Then you need to create a `CodecProvider` to link this `Codec` to the `Fruit` class.

```java
package org.acme.mongodb.codec;

import org.acme.mongodb.Fruit;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class FruitCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(Fruit.class)) {
            return (Codec<T>) new FruitCodec();
        }
        return null;
    }

}
```

Quarkus takes care of registering the `CodecProvider` for you as a CDI bean of `@Singleton` scope.

Finally, when getting the `MongoCollection` from the database you can use directly the `Fruit` class instead of the `Document` one, the codec will automatically map the `Document` to/from your `Fruit` class.

Here is an example of using a `MongoCollection` with the `FruitCodec`.

```java
package org.acme.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CodecFruitService {

    @Inject MongoClient mongoClient;

    public List<Fruit> list(){
        List<Fruit> list = new ArrayList<>();
        MongoCursor<Fruit> cursor = getCollection().find().iterator();

        try {
            while (cursor.hasNext()) {
                list.add(cursor.next());
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public void add(Fruit fruit){
        getCollection().insertOne(fruit);
    }

    private MongoCollection<Fruit> getCollection(){
        return mongoClient.getDatabase("fruit").getCollection("fruit", Fruit.class);
    }
}
```

## The POJO Codec

The [POJO Codec](https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/document-data-format-pojo/) provides a set of annotations that enable the customization of the way a POJO is mapped to a MongoDB collection and this codec is initialized automatically by Quarkus. This codec also supports Java records so you can use them for your POJOs or an attribute of your POJOs.

One of these annotations is the `@BsonDiscriminator` annotation that allows storing multiple Java types in a single MongoDB collection by adding a discriminator field inside the document. It can be useful when working with abstract types or interfaces.

Quarkus will automatically register all the classes annotated with `@BsonDiscriminator` with the POJO codec.

The POJO Codec have enhanced generic support via `PropertyCodecProvider`, Quarkus will automatically register any `PropertyCodecProvider` with the POJO Codec (these classes are automatically made CDI beans of `@Singleton` scope). When building native executables and using generic types, you might need to register the type arguments for reflection.

## Simplifying MongoDB with Panache

The [MongoDB with Panache](/guides/mongodb-panache/index.md) extension facilitates the usage of MongoDB by providing active record style entities (and repositories) like you have in [Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md) and focuses on making your entities trivial and fun to write in Quarkus.

## Schema migration with Liquibase

The [Liquibase MongoDB](/guides/liquibase-mongodb/index.md) extension facilitates the initialization of a MongoDB database including indices and initial data. It implements the same schema migration facilities that Liquibase offers for SQL databases.

<a id="mongo-health-check"></a>
## Connection Health Check

If you are using the `quarkus-smallrye-health` extension, `quarkus-mongodb-client` will automatically add a readiness health check to validate the connection to the cluster.

So when you access the `/q/health/ready` endpoint of your application you will have information about the connection validation status.

This behavior can be disabled by setting the `quarkus.mongodb.health.enabled` property to `false` in your `application.properties`.

## Metrics

If you are using the `quarkus-micrometer`, `quarkus-mongodb-client` can provide metrics about the connection pools. This behavior must first be enabled by setting the `quarkus.mongodb.metrics.enabled` property to `true` in your `application.properties`.

So when you access the `/q/metrics` endpoint of your application you will have information about the connection pool status.

## Tracing

To use tracing with MongoDB, you need to add the [`quarkus-opentelemetry`](/guides/opentelemetry/index.md) extension to your project.

Even with all the tracing infrastructure in place the mongodb tracing is not enabled by default, and you need to enable it by setting this property:

```properties
# enable tracing
quarkus.mongodb.tracing.enabled=true
```

### Command Detail Level

By default, MongoDB command details are not included in traces (`OFF` mode). You can control this behavior with the `command-detail-level` property. When enabled, the command text is stored in the `db.query.text` attribute following OpenTelemetry semantic conventions.

```properties
# Command detail level: OFF (default), SANITIZED, or FULL
quarkus.mongodb.tracing.command-detail-level=SANITIZED
```

**Detail levels:**

- `OFF` (default) - No command details, only operation metadata (operation name, database, collection)
- `SANITIZED` - Command structure with type placeholders instead of actual values
  - Example: `{"find": "<string>", "filter": {"email": "<string>", "age": "<int32>"}}`
  - Preserves command structure for debugging while protecting sensitive data
- `FULL` - Complete command including all data
  - **NOT recommended for production** - may expose sensitive information

> [!WARNING]
> Using `FULL` mode can expose sensitive data (passwords, emails, personal information) in your traces. Only use in development or when you're certain no sensitive data is involved.

## Testing helpers

[Dev Services for MongoDB](#dev-services) is your best option to start a MongoDB database for your unit tests.

But if you can't use it, you can start a MongoDB database using one of the two `QuarkusTestResourceLifecycleManager` that Quarkus provides. They rely on [Flapdoodle embedded MongoDB](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo).

- `io.quarkus.test.mongodb.MongoTestResource` will start a single instance on port 27017.
- `io.quarkus.test.mongodb.MongoReplicaSetTestResource` will start a replicaset with two instances, one on port 27017 and the other on port 27018.

To use them, you need to add the `io.quarkus:quarkus-test-mongodb` dependency to your pom.xml.

For more information about the usage of a `QuarkusTestResourceLifecycleManager` please read [Quarkus test resource](/guides/getting-started-testing/index.md#quarkus-test-resource).

> [!TIP]
> To set the desired port MongoDB will listen to when it is launched, the following code should be used:
>
> ```java
> @QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.PORT, value = "27017"))
> ```
>
> To set the desired MongoDB version that will be launched, the following code should be used:
>
> ```java
> @QuarkusTestResource(value = MongoTestResource.class, initArgs = @ResourceArg(name = MongoTestResource.VERSION, value = "V5_0"))
> ```
>
> The string value used can be any of one of the `de.flapdoodle.embed.mongo.distribution.Version` or `de.flapdoodle.embed.mongo.distribution.Version.Main` enums. If no version is specified, `Version.Main.V4_0` is used by default.

## The legacy client

We don't include the legacy MongoDB client by default. It contains the now retired MongoDB Java API (DB, DBCollection,... ) and the `com.mongodb.MongoClient` that is now superseded by `com.mongodb.client.MongoClient`.

If you want to use the legacy API, you need to add the following dependency to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-legacy</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("org.mongodb:mongodb-driver-legacy")
```

## TLS configuration

If you want to use SSL/TLS encryption, you need to add this property in your `application.properties`:

```properties
quarkus.mongodb.tls=true
```

You can also configure the client to use a tls configuration from the TLS registry, this will enable tls:

```properties
quarkus.tls.mongo.trust-store.pem.certs=certa.pem,certb.pem
quarkus.mongodb.tls-configuration-name=mongo
```

You can configure the client to trust servers with certificates with invalid hostnames:

```properties
quarkus.tls.mongo.hostname-verification-algorithm=NONE
quarkus.mongodb.tls-configuration-name=mongo
```

## Building a native executable

You can use the MongoDB client in a native executable.

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

Running it is as simple as executing `./target/mongodb-quickstart-1.0.0-SNAPSHOT-runner`.

You can then point your browser to `http://localhost:8080/fruits.html` and use your application.

> [!WARNING]
> Currently, Quarkus doesn't support [Client-Side Field Level Encryption](https://docs.mongodb.com/manual/core/security-client-side-encryption/) in native mode.

> [!TIP]
> If you encounter the following error when running your application in native mode:\
> `Failed to encode 'MyObject'. Encoding 'myVariable' errored with: Can't find a codec for class org.acme.MyVariable.`\
> This means that the `org.acme.MyVariable` class is not known to GraalVM, the remedy is to add the `@RegisterForReflection` annotation to your `MyVariable` class. More details about the `@RegisterForReflection` annotation can be found on the [native application tips](/guides/writing-native-applications-tips/index.md#registerForReflection) page.

## Customize the Mongo client configuration programmatically

If you need to customize the Mongo client configuration programmatically, you need to implement the `io.quarkus.mongodb.runtime.MongoClientCustomizer` interface and expose it as a CDI *application scoped* bean:

```java
@ApplicationScoped
public class MyCustomizer implements MongoClientCustomizer {

    @Override
    public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
        return builder.applicationName("my-app");
    }
}
```

The bean can customize a specific client using the `@MongoClientName` qualifier to indicate the client name. When there is no qualifier, it customizes the default client. At most one customizer can be used per client. If multiple customizers targeting the same client are detected, an exception is thrown at build time.

This feature can be used to configure client-side field level encryption (CSFLE). Follows the instructions from [the Mongo web site](https://www.mongodb.com/docs/manual/core/csfle/) to configure CSFLE:

```java
@ApplicationScoped
public class MyCustomizer implements MongoClientCustomizer {
    @Override
    public MongoClientSettings.Builder customize(MongoClientSettings.Builder builder) {
        Map<String, Map<String, Object>> kmsProviders = getKmsProviders();
        String dek = getDataEncryptionKey();
        Map<String, BsonDocument> schema = getSchema(dek);

        Map<String, Object> extraOptions = new HashMap<>();
        extraOptions.put("cryptSharedLibPath", "<path to crypt shared library>");

        return builder.autoEncryptionSettings(AutoEncryptionSettings.builder()
                .keyVaultNamespace(KEY_VAULT_NAMESPACE)
                .kmsProviders(kmsProviders)
                .schemaMap(schemaMap)
                .extraOptions(extraOptions)
                .build());
    }
}
```

> [!IMPORTANT]
> Client-side field level encryption, and feature relying on [Mongo Crypt](https://github.com/mongodb/libmongocrypt) in general, are not supported in native mode.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-health-enabled"></a> [`quarkus.mongodb.health.enabled`](#quarkus-mongodb-client_quarkus-mongodb-health-enabled) config_property_copy_button:+++quarkus.mongodb.health.enabled+++[]<br><br>Whether a health check is published in case the smallrye-health extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_HEALTH_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-metrics-enabled"></a> [`quarkus.mongodb.metrics.enabled`](#quarkus-mongodb-client_quarkus-mongodb-metrics-enabled) config_property_copy_button:+++quarkus.mongodb.metrics.enabled+++[]<br><br>Whether metrics are published in case a metrics extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_METRICS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-force-default-clients"></a> [`quarkus.mongodb.force-default-clients`](#quarkus-mongodb-client_quarkus-mongodb-force-default-clients) config_property_copy_button:+++quarkus.mongodb.force-default-clients+++[]<br><br>If set to true, the default clients will always be created even if there are no injection points that use them<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_FORCE_DEFAULT_CLIENTS+++[] | boolean | `false` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-tracing-enabled"></a> [`quarkus.mongodb.tracing.enabled`](#quarkus-mongodb-client_quarkus-mongodb-tracing-enabled) config_property_copy_button:+++quarkus.mongodb.tracing.enabled+++[]<br><br>Whether tracing spans of driver commands are sent in case the quarkus-opentelemetry extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_TRACING_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-dns-server-host"></a> [`quarkus.mongodb.dns.server-host`](#quarkus-mongodb-client_quarkus-mongodb-dns-server-host) config_property_copy_button:+++quarkus.mongodb.dns.server-host+++[]<br><br>This property configures the DNS server. If the server is not set, it tries to read the first `nameserver` from `/etc /resolv.conf` (if the file exists), otherwise fallback to the default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DNS_SERVER_HOST+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-dns-server-port"></a> [`quarkus.mongodb.dns.server-port`](#quarkus-mongodb-client_quarkus-mongodb-dns-server-port) config_property_copy_button:+++quarkus.mongodb.dns.server-port+++[]<br><br>This property configures the DNS server port.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DNS_SERVER_PORT+++[] | int |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-dns-lookup-timeout"></a> [`quarkus.mongodb.dns.lookup-timeout`](#quarkus-mongodb-client_quarkus-mongodb-dns-lookup-timeout) config_property_copy_button:+++quarkus.mongodb.dns.lookup-timeout+++[]<br><br>If `native.dns.use-vertx-dns-resolver` is set to `true`, this property configures the DNS lookup timeout duration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DNS_LOOKUP_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) | `5S` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-dns-log-activity"></a> [`quarkus.mongodb.dns.log-activity`](#quarkus-mongodb-client_quarkus-mongodb-dns-log-activity) config_property_copy_button:+++quarkus.mongodb.dns.log-activity+++[]<br><br>This property enables the logging ot the DNS lookup. It can be useful to understand why the lookup fails.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DNS_LOG_ACTIVITY+++[] | boolean | `false` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-active"></a> [`quarkus.mongodb.active`](#quarkus-mongodb-client_quarkus-mongodb-active) config_property_copy_button:+++quarkus.mongodb.active+++[]<br><br>`quarkus.mongodb."mongo-client-name".active` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".active+++[]<br><br>Whether this Mongo Client should be active at runtime.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_ACTIVE+++[] | boolean | `true` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-connection-string"></a> [`quarkus.mongodb.connection-string`](#quarkus-mongodb-client_quarkus-mongodb-connection-string) config_property_copy_button:+++quarkus.mongodb.connection-string+++[]<br><br>`quarkus.mongodb."mongo-client-name".connection-string` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".connection-string+++[]<br><br>Configures the connection string. The format is: `mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database.collection][?options]]`<br><br>`mongodb://` is a required prefix to identify that this is a string in the standard connection format.<br><br>`username:password@` are optional. If given, the driver will attempt to log in to a database after connecting to a database server. For some authentication mechanisms, only the username is specified and the password is not, in which case the ":" after the username is left off as well.<br><br>`host1` is the only required part of the connection string. It identifies a server address to connect to.<br><br>`:portX` is optional and defaults to :27017 if not provided.<br><br>`/database` is the name of the database to log in to and thus is only relevant if the `username:password@` syntax is used. If not specified the `admin` database will be used by default.<br><br>`?options` are connection options. Note that if `database` is absent there is still a `/` required between the last host and the `?` introducing the options. Options are name=value pairs and the pairs are separated by "&".<br><br>An alternative format, using the `mongodb{plus}srv` protocol, is:<br><br>```<br>mongodb+srv://[username:password@]host[/[database][?options]]<br>```<br><br>\* `mongodb{plus}srv://` is a required prefix for this format. \* `username:password@` are optional. If given, the driver will attempt to login to a database after connecting to a database server. For some authentication mechanisms, only the username is specified and the password is not, in which case the ":" after the username is left off as well \* `host` is the only required part of the URI. It identifies a single host name for which SRV records are looked up from a Domain Name Server after prefixing the host name with `"_mongodb._tcp"`. The host/port for each SRV record becomes the seed list used to connect, as if each one were provided as host/port pair in a URI using the normal mongodb protocol. \* `/database` is the name of the database to login to and thus is only relevant if the `username:password@` syntax is used. If not specified the "admin" database will be used by default. \* `?options` are connection options. Note that if `database` is absent there is still a `/` required between the last host and the `?` introducing the options. Options are name=value pairs and the pairs are separated by "&". Additionally with the mongodb{plus}srv protocol, TXT records are looked up from a Domain Name Server for the given host, and the text value of each one is prepended to any options on the URI itself. Because the last specified value for any option wins, that means that options provided on the URI will override any that are provided via TXT records.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CONNECTION_STRING+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-hosts"></a> [`quarkus.mongodb.hosts`](#quarkus-mongodb-client_quarkus-mongodb-hosts) config_property_copy_button:+++quarkus.mongodb.hosts+++[]<br><br>`quarkus.mongodb."mongo-client-name".hosts` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".hosts+++[]<br><br>Configures the MongoDB server addresses (one if single mode). The addresses are passed as `host:port`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_HOSTS+++[] | list of string | `127.0.0.1:27017` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-database"></a> [`quarkus.mongodb.database`](#quarkus-mongodb-client_quarkus-mongodb-database) config_property_copy_button:+++quarkus.mongodb.database+++[]<br><br>`quarkus.mongodb."mongo-client-name".database` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".database+++[]<br><br>Configure the database name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DATABASE+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-application-name"></a> [`quarkus.mongodb.application-name`](#quarkus-mongodb-client_quarkus-mongodb-application-name) config_property_copy_button:+++quarkus.mongodb.application-name+++[]<br><br>`quarkus.mongodb."mongo-client-name".application-name` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".application-name+++[]<br><br>Configures the application name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_APPLICATION_NAME+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-max-pool-size"></a> [`quarkus.mongodb.max-pool-size`](#quarkus-mongodb-client_quarkus-mongodb-max-pool-size) config_property_copy_button:+++quarkus.mongodb.max-pool-size+++[]<br><br>`quarkus.mongodb."mongo-client-name".max-pool-size` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".max-pool-size+++[]<br><br>Configures the maximum number of connections in the connection pool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_MAX_POOL_SIZE+++[] | int |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-min-pool-size"></a> [`quarkus.mongodb.min-pool-size`](#quarkus-mongodb-client_quarkus-mongodb-min-pool-size) config_property_copy_button:+++quarkus.mongodb.min-pool-size+++[]<br><br>`quarkus.mongodb."mongo-client-name".min-pool-size` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".min-pool-size+++[]<br><br>Configures the minimum number of connections in the connection pool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_MIN_POOL_SIZE+++[] | int |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-max-connection-idle-time"></a> [`quarkus.mongodb.max-connection-idle-time`](#quarkus-mongodb-client_quarkus-mongodb-max-connection-idle-time) config_property_copy_button:+++quarkus.mongodb.max-connection-idle-time+++[]<br><br>`quarkus.mongodb."mongo-client-name".max-connection-idle-time` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".max-connection-idle-time+++[]<br><br>Maximum idle time of a pooled connection. A connection that exceeds this limit will be closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_MAX_CONNECTION_IDLE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-max-connection-life-time"></a> [`quarkus.mongodb.max-connection-life-time`](#quarkus-mongodb-client_quarkus-mongodb-max-connection-life-time) config_property_copy_button:+++quarkus.mongodb.max-connection-life-time+++[]<br><br>`quarkus.mongodb."mongo-client-name".max-connection-life-time` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".max-connection-life-time+++[]<br><br>Maximum lifetime of a pooled connection. A connection that exceeds this limit will be closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_MAX_CONNECTION_LIFE_TIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-maintenance-frequency"></a> [`quarkus.mongodb.maintenance-frequency`](#quarkus-mongodb-client_quarkus-mongodb-maintenance-frequency) config_property_copy_button:+++quarkus.mongodb.maintenance-frequency+++[]<br><br>`quarkus.mongodb."mongo-client-name".maintenance-frequency` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".maintenance-frequency+++[]<br><br>Configures the time period between runs of the maintenance job.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_MAINTENANCE_FREQUENCY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-maintenance-initial-delay"></a> [`quarkus.mongodb.maintenance-initial-delay`](#quarkus-mongodb-client_quarkus-mongodb-maintenance-initial-delay) config_property_copy_button:+++quarkus.mongodb.maintenance-initial-delay+++[]<br><br>`quarkus.mongodb."mongo-client-name".maintenance-initial-delay` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".maintenance-initial-delay+++[]<br><br>Configures period of time to wait before running the first maintenance job on the connection pool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_MAINTENANCE_INITIAL_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-connect-timeout"></a> [`quarkus.mongodb.connect-timeout`](#quarkus-mongodb-client_quarkus-mongodb-connect-timeout) config_property_copy_button:+++quarkus.mongodb.connect-timeout+++[]<br><br>`quarkus.mongodb."mongo-client-name".connect-timeout` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".connect-timeout+++[]<br><br>How long a connection can take to be opened before timing out.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-read-timeout"></a> [`quarkus.mongodb.read-timeout`](#quarkus-mongodb-client_quarkus-mongodb-read-timeout) config_property_copy_button:+++quarkus.mongodb.read-timeout+++[]<br><br>`quarkus.mongodb."mongo-client-name".read-timeout` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".read-timeout+++[]<br><br>How long a socket read can take before timing out.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-tls"></a> [`quarkus.mongodb.tls`](#quarkus-mongodb-client_quarkus-mongodb-tls) config_property_copy_button:+++quarkus.mongodb.tls+++[]<br><br>`quarkus.mongodb."mongo-client-name".tls` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".tls+++[]<br><br>Whether to connect using TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_TLS+++[] | boolean | `false` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-tls-configuration-name"></a> [`quarkus.mongodb.tls-configuration-name`](#quarkus-mongodb-client_quarkus-mongodb-tls-configuration-name) config_property_copy_button:+++quarkus.mongodb.tls-configuration-name+++[]<br><br>`quarkus.mongodb."mongo-client-name".tls-configuration-name` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-replica-set-name"></a> [`quarkus.mongodb.replica-set-name`](#quarkus-mongodb-client_quarkus-mongodb-replica-set-name) config_property_copy_button:+++quarkus.mongodb.replica-set-name+++[]<br><br>`quarkus.mongodb."mongo-client-name".replica-set-name` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".replica-set-name+++[]<br><br>Implies that the hosts given are a seed list, and the driver will attempt to find all members of the set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_REPLICA_SET_NAME+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-server-selection-timeout"></a> [`quarkus.mongodb.server-selection-timeout`](#quarkus-mongodb-client_quarkus-mongodb-server-selection-timeout) config_property_copy_button:+++quarkus.mongodb.server-selection-timeout+++[]<br><br>`quarkus.mongodb."mongo-client-name".server-selection-timeout` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".server-selection-timeout+++[]<br><br>How long the driver will wait for server selection to succeed before throwing an exception.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_SERVER_SELECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-local-threshold"></a> [`quarkus.mongodb.local-threshold`](#quarkus-mongodb-client_quarkus-mongodb-local-threshold) config_property_copy_button:+++quarkus.mongodb.local-threshold+++[]<br><br>`quarkus.mongodb."mongo-client-name".local-threshold` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".local-threshold+++[]<br><br>When choosing among multiple MongoDB servers to send a request, the driver will only send that request to a server whose ping time is less than or equal to the server with the fastest ping time plus the local threshold.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_LOCAL_THRESHOLD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-heartbeat-frequency"></a> [`quarkus.mongodb.heartbeat-frequency`](#quarkus-mongodb-client_quarkus-mongodb-heartbeat-frequency) config_property_copy_button:+++quarkus.mongodb.heartbeat-frequency+++[]<br><br>`quarkus.mongodb."mongo-client-name".heartbeat-frequency` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".heartbeat-frequency+++[]<br><br>The frequency that the driver will attempt to determine the current state of each server in the cluster.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_HEARTBEAT_FREQUENCY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-read-concern"></a> [`quarkus.mongodb.read-concern`](#quarkus-mongodb-client_quarkus-mongodb-read-concern) config_property_copy_button:+++quarkus.mongodb.read-concern+++[]<br><br>`quarkus.mongodb."mongo-client-name".read-concern` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".read-concern+++[]<br><br>Configures the read concern. Supported values are: `local\|majority\|linearizable\|snapshot\|available`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_READ_CONCERN+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-read-preference"></a> [`quarkus.mongodb.read-preference`](#quarkus-mongodb-client_quarkus-mongodb-read-preference) config_property_copy_button:+++quarkus.mongodb.read-preference+++[]<br><br>`quarkus.mongodb."mongo-client-name".read-preference` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".read-preference+++[]<br><br>Configures the read preference. Supported values are: `primary\|primaryPreferred\|secondary\|secondaryPreferred\|nearest`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_READ_PREFERENCE+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-health-database"></a> [`quarkus.mongodb.health.database`](#quarkus-mongodb-client_quarkus-mongodb-health-database) config_property_copy_button:+++quarkus.mongodb.health.database+++[]<br><br>`quarkus.mongodb."mongo-client-name".health.database` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".health.database+++[]<br><br>The database used during the readiness health checks<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_HEALTH_DATABASE+++[] | string | `admin` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-uuid-representation"></a> [`quarkus.mongodb.uuid-representation`](#quarkus-mongodb-client_quarkus-mongodb-uuid-representation) config_property_copy_button:+++quarkus.mongodb.uuid-representation+++[]<br><br>`quarkus.mongodb."mongo-client-name".uuid-representation` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".uuid-representation+++[]<br><br>Configures the UUID representation to use when encoding instances of `java.util.UUID` and when decoding BSON binary values with subtype of 3.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_UUID_REPRESENTATION+++[] | `unspecified`, `standard`, `c-sharp-legacy`, `java-legacy`, `python-legacy` |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-reactive-transport"></a> [`quarkus.mongodb.reactive-transport`](#quarkus-mongodb-client_quarkus-mongodb-reactive-transport) config_property_copy_button:+++quarkus.mongodb.reactive-transport+++[]<br><br>`quarkus.mongodb."mongo-client-name".reactive-transport` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".reactive-transport+++[]<br><br>Configures the reactive transport.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_REACTIVE_TRANSPORT+++[] | tooltip:netty[Uses a Netty-based transport re-using the existing Netty event loops.], tooltip:mongo[With a reactive driver it uses an async transport backed by a driver-managed thread pool.] | tooltip:netty[Uses a Netty-based transport re-using the existing Netty event loops.] |
| <a id="quarkus-mongodb-client_quarkus-mongodb-native-dns-use-vertx-dns-resolver"></a> <mark>link:#quarkus-mongodb-client_quarkus-mongodb-native-dns-use-vertx-dns-resolver\[`quarkus.mongodb.native.dns.use-vertx-dns-resolver`\]</mark> config_property_copy_button:+++quarkus.mongodb.native.dns.use-vertx-dns-resolver+++[]<br><br>*This property is deprecated: This resolver is always used.*<br><br>The default DNS resolver used to handle `mongo{plus}srv://` urls cannot be used in a native executable. This option enables a fallback to use Vert.x to resolve the server names instead of JNDI. **IMPORTANT:** The resolution may be different in JVM mode using the default (JNDI-based) DNS resolver, and in native mode. This feature is experimental.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_NATIVE_DNS_USE_VERTX_DNS_RESOLVER+++[] | boolean | `false` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-tls-insecure"></a> <mark>link:#quarkus-mongodb-client_quarkus-mongodb-tls-insecure\[`quarkus.mongodb.tls-insecure`\]</mark> config_property_copy_button:+++quarkus.mongodb.tls-insecure+++[]<br><br>`quarkus.mongodb."mongo-client-name".tls-insecure` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".tls-insecure+++[]<br><br>*This property is deprecated since `3.21`: in favor of configuration at the tls registry level. See `tls-configuration-name()` and quarkus tls registry hostname verification configuration `quarkus.tls.hostname-verification-algorithm=NONE`.*<br><br>If connecting with TLS, this option enables insecure TLS connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_TLS_INSECURE+++[] | boolean | `false` |
| <a id="quarkus-mongodb-client_section_quarkus-mongodb-devservices"></a> [Dev Services](#quarkus-mongodb-client_section_quarkus-mongodb-devservices) | Type | Default |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-enabled"></a> [`quarkus.mongodb.devservices.enabled`](#quarkus-mongodb-client_quarkus-mongodb-devservices-enabled) config_property_copy_button:+++quarkus.mongodb.devservices.enabled+++[]<br><br>If DevServices has been explicitly enabled or disabled. DevServices is generally enabled by default, unless there is an existing configuration present.<br><br>When DevServices is enabled Quarkus will attempt to automatically configure and start a database when running in Dev or Test mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-image-name"></a> [`quarkus.mongodb.devservices.image-name`](#quarkus-mongodb-client_quarkus-mongodb-devservices-image-name) config_property_copy_button:+++quarkus.mongodb.devservices.image-name+++[]<br><br>The container image name to use, for container based DevServices providers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_IMAGE_NAME+++[] | string | `docker.io/library/mongo:7.0` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-port"></a> [`quarkus.mongodb.devservices.port`](#quarkus-mongodb-client_quarkus-mongodb-devservices-port) config_property_copy_button:+++quarkus.mongodb.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-properties-property-key"></a> [`quarkus.mongodb.devservices.properties."property-key"`](#quarkus-mongodb-client_quarkus-mongodb-devservices-properties-property-key) config_property_copy_button:+++quarkus.mongodb.devservices.properties."property-key"+++[]<br><br>Generic properties that are added to the connection URL.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-container-env-environment-variable-name"></a> [`quarkus.mongodb.devservices.container-env."environment-variable-name"`](#quarkus-mongodb-client_quarkus-mongodb-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.mongodb.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-shared"></a> [`quarkus.mongodb.devservices.shared`](#quarkus-mongodb-client_quarkus-mongodb-devservices-shared) config_property_copy_button:+++quarkus.mongodb.devservices.shared+++[]<br><br>Indicates if the MongoDB server managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for MongoDB starts a new container.<br><br>The discovery uses the `quarkus-dev-service-mongodb` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices-service-name"></a> [`quarkus.mongodb.devservices.service-name`](#quarkus-mongodb-client_quarkus-mongodb-devservices-service-name) config_property_copy_button:+++quarkus.mongodb.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-mongodb` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for MongoDB looks for a container with the `quarkus-dev-service-mongodb` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it starts a new container with the `quarkus-dev-service-mongodb` label set to the specified value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_SERVICE_NAME+++[] | string | `mongodb` |
| <a id="quarkus-mongodb-client_section_quarkus-mongodb-write-concern"></a> [Write concern](#quarkus-mongodb-client_section_quarkus-mongodb-write-concern) | Type | Default |
| <a id="quarkus-mongodb-client_quarkus-mongodb-write-concern-safe"></a> [`quarkus.mongodb.write-concern.safe`](#quarkus-mongodb-client_quarkus-mongodb-write-concern-safe) config_property_copy_button:+++quarkus.mongodb.write-concern.safe+++[]<br><br>`quarkus.mongodb."mongo-client-name".write-concern.safe` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".write-concern.safe+++[]<br><br>Configures the safety. If set to `true`: the driver ensures that all writes are acknowledged by the MongoDB server, or else throws an exception. (see also `w` and `wtimeoutMS`). If set fo `false`: the driver does not ensure that all writes are acknowledged by the MongoDB server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_WRITE_CONCERN_SAFE+++[] | boolean | `true` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-write-concern-journal"></a> [`quarkus.mongodb.write-concern.journal`](#quarkus-mongodb-client_quarkus-mongodb-write-concern-journal) config_property_copy_button:+++quarkus.mongodb.write-concern.journal+++[]<br><br>`quarkus.mongodb."mongo-client-name".write-concern.journal` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".write-concern.journal+++[]<br><br>Configures the journal writing aspect. If set to `true`: the driver waits for the server to group commit to the journal file on disk. If set to `false`: the driver does not wait for the server to group commit to the journal file on disk.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_WRITE_CONCERN_JOURNAL+++[] | boolean | `true` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-write-concern-w"></a> [`quarkus.mongodb.write-concern.w`](#quarkus-mongodb-client_quarkus-mongodb-write-concern-w) config_property_copy_button:+++quarkus.mongodb.write-concern.w+++[]<br><br>`quarkus.mongodb."mongo-client-name".write-concern.w` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".write-concern.w+++[]<br><br>When set, the driver adds `w: wValue` to all write commands. It requires `safe` to be `true`. The value is typically a number, but can also be the `majority` string.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_WRITE_CONCERN_W+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-write-concern-retry-writes"></a> [`quarkus.mongodb.write-concern.retry-writes`](#quarkus-mongodb-client_quarkus-mongodb-write-concern-retry-writes) config_property_copy_button:+++quarkus.mongodb.write-concern.retry-writes+++[]<br><br>`quarkus.mongodb."mongo-client-name".write-concern.retry-writes` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".write-concern.retry-writes+++[]<br><br>If set to `true`, the driver will retry supported write operations if they fail due to a network error.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_WRITE_CONCERN_RETRY_WRITES+++[] | boolean | `false` |
| <a id="quarkus-mongodb-client_quarkus-mongodb-write-concern-w-timeout"></a> [`quarkus.mongodb.write-concern.w-timeout`](#quarkus-mongodb-client_quarkus-mongodb-write-concern-w-timeout) config_property_copy_button:+++quarkus.mongodb.write-concern.w-timeout+++[]<br><br>`quarkus.mongodb."mongo-client-name".write-concern.w-timeout` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".write-concern.w-timeout+++[]<br><br>When set, the driver adds `wtimeout : ms` to all write commands. It requires `safe` to be `true`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_WRITE_CONCERN_W_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb) |  |
| <a id="quarkus-mongodb-client_section_quarkus-mongodb-credentials"></a> [Credentials and authentication mechanism](#quarkus-mongodb-client_section_quarkus-mongodb-credentials) | Type | Default |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-username"></a> [`quarkus.mongodb.credentials.username`](#quarkus-mongodb-client_quarkus-mongodb-credentials-username) config_property_copy_button:+++quarkus.mongodb.credentials.username+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.username` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.username+++[]<br><br>Configures the username.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_USERNAME+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-password"></a> [`quarkus.mongodb.credentials.password`](#quarkus-mongodb-client_quarkus-mongodb-credentials-password) config_property_copy_button:+++quarkus.mongodb.credentials.password+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.password` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.password+++[]<br><br>Configures the password.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_PASSWORD+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-auth-mechanism"></a> [`quarkus.mongodb.credentials.auth-mechanism`](#quarkus-mongodb-client_quarkus-mongodb-credentials-auth-mechanism) config_property_copy_button:+++quarkus.mongodb.credentials.auth-mechanism+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.auth-mechanism` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.auth-mechanism+++[]<br><br>Configures the authentication mechanism to use if a credential was supplied. The default is unspecified, in which case the client will pick the most secure mechanism available based on the sever version. For the GSSAPI and MONGODB-X509 mechanisms, no password is accepted, only the username. Supported values: null or `GSSAPI\|PLAIN\|MONGODB-X509\|SCRAM_SHA_1\|SCRAM_SHA_256\|MONGODB_AWS`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_AUTH_MECHANISM+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-auth-source"></a> [`quarkus.mongodb.credentials.auth-source`](#quarkus-mongodb-client_quarkus-mongodb-credentials-auth-source) config_property_copy_button:+++quarkus.mongodb.credentials.auth-source+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.auth-source` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.auth-source+++[]<br><br>Configures the source of the authentication credentials. This is typically the database where the credentials have been created. The value defaults to the database specified in the path portion of the connection string or in the 'database' configuration property. If the database is specified in neither place, the default value is `admin`. This option is only respected when using the MONGO-CR mechanism (the default).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_AUTH_SOURCE+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-auth-mechanism-properties-property-key"></a> [`quarkus.mongodb.credentials.auth-mechanism-properties."property-key"`](#quarkus-mongodb-client_quarkus-mongodb-credentials-auth-mechanism-properties-property-key) config_property_copy_button:+++quarkus.mongodb.credentials.auth-mechanism-properties."property-key"+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.auth-mechanism-properties."property-key"` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.auth-mechanism-properties."property-key"+++[]<br><br>Allows passing authentication mechanism properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_AUTH_MECHANISM_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-credentials-provider"></a> [`quarkus.mongodb.credentials.credentials-provider`](#quarkus-mongodb-client_quarkus-mongodb-credentials-credentials-provider) config_property_copy_button:+++quarkus.mongodb.credentials.credentials-provider+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.credentials-provider` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.credentials-provider+++[]<br><br>The credentials provider name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_CREDENTIALS_PROVIDER+++[] | string |  |
| <a id="quarkus-mongodb-client_quarkus-mongodb-credentials-credentials-provider-name"></a> [`quarkus.mongodb.credentials.credentials-provider-name`](#quarkus-mongodb-client_quarkus-mongodb-credentials-credentials-provider-name) config_property_copy_button:+++quarkus.mongodb.credentials.credentials-provider-name+++[]<br><br>`quarkus.mongodb."mongo-client-name".credentials.credentials-provider-name` config_property_copy_button:+++quarkus.mongodb."mongo-client-name".credentials.credentials-provider-name+++[]<br><br>The credentials provider bean name.<br><br>This is a bean name (as in `@Named`) of a bean that implements `CredentialsProvider`. It is used to select the credentials provider bean when multiple exist. This is unnecessary when there is only one credentials provider available.<br><br>For Vault, the credentials provider bean name is `vault-credentials-provider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_CREDENTIALS_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-mongodb-client_section_quarkus-mongodb-tracing"></a> [Tracing configuration](#quarkus-mongodb-client_section_quarkus-mongodb-tracing) | Type | Default |
| <a id="quarkus-mongodb-client_quarkus-mongodb-tracing-command-detail-level"></a> [`quarkus.mongodb.tracing.command-detail-level`](#quarkus-mongodb-client_quarkus-mongodb-tracing-command-detail-level) config_property_copy_button:+++quarkus.mongodb.tracing.command-detail-level+++[]<br><br>Defines what information from MongoDB commands is included in traces.<br><br>\* **OFF** (default): No command information is included (only operation name, database, collection) \* **SANITIZED**: Command structure with BSON type information instead of actual values \* **FULL**: Complete command with all data (NOT recommended for production - may expose sensitive data)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_TRACING_COMMAND_DETAIL_LEVEL+++[] | tooltip:off[No command details,opts=only operation metadata], tooltip:sanitized[Command structure with type information (e.g.,opts=++{++"email"\: "<string>",age,\: "<int32>"++}++)], tooltip:full[Full command including all data - may expose sensitive information] | tooltip:off[No command details,opts=only operation metadata] |

<a id="duration-note-anchor-quarkus-mongodb-client_quarkus-mongodb"></a>

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
