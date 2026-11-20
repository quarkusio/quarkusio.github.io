# Using the Redis Client

This guide demonstrates how your Quarkus application can connect to a Redis server using the Redis Client extension.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- A working Docker environment

## Architecture

In this guide, we are going to expose a simple Rest API to increment numbers by using the [`INCRBY`](https://redis.io/commands/incrby) command. Along the way, we'll see how to use other Redis commands like `GET`, `SET` (from the string group), `DEL` and `KEYS` (from the key group).

We'll be using the Quarkus Redis extension to connect to interact with Redis.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `redis-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/redis-quickstart).

## Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:redis-quickstart \
    --extension='redis-client,rest-jackson' \
    --no-code
cd redis-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=redis-quickstart \
    -Dextensions='redis-client,rest-jackson' \
    -DnoCode
cd redis-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=redis-quickstart"`

This command generates a new project, importing the Redis extension.

If you already have your Quarkus project configured, you can add the `redis-client` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add redis-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='redis-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='redis-client'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-redis-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-redis-client")
```

## Creating the Increment POJO

We are going to model our increments using the `Increment` POJO. Create the `src/main/java/org/acme/redis/Increment.java` file, with the following content:

```java
package org.acme.redis;

public class Increment {
    public String key; // <1>
    public long value; // <2>

    public Increment(String key, long value) {
        this.key = key;
        this.value = value;
    }

    public Increment() {
    }
}
```

1. The key that will be used as the Redis key
2. The value held by the Redis key

## Creating the Increment Service

We are going to create an `IncrementService` class which will play the role of a Redis client. With this class, we'll be able to perform the `SET`, `GET` , `DEL`, `KEYS` and `INCRBY` Redis commands.

Create the `src/main/java/org/acme/redis/IncrementService.java` file, with the following content:

```java
package org.acme.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.string.StringCommands;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class IncrementService {

    // This quickstart demonstrates both the imperative
    // and reactive Redis data sources
    // Regular applications will pick one of them.

    private ReactiveKeyCommands<String> keyCommands; // <1>
    private ValueCommands<String, Long> countCommands; // <2>

    public IncrementService(RedisDataSource ds, ReactiveRedisDataSource reactive) { // <3>
        countCommands = ds.value(Long.class); // <4>
        keyCommands = reactive.key();  // <5>

    }


    long get(String key) {
        Long value = countCommands.get(key); // <6>
        if (value == null) {
            return 0L;
        }
        return value;
    }

    void set(String key, Long value) {
        countCommands.set(key, value); // <7>
    }

    void increment(String key, Long incrementBy) {
        countCommands.incrby(key, incrementBy); // <8>
    }

    Uni<Void> del(String key) {
        return keyCommands.del(key) // <9>
            .replaceWithVoid();
    }

    Uni<List<String>> keys() {
        return keyCommands.keys("*"); // <10>
    }
}
```

1. The field use to manipulate keys
2. The field use to manipulate the counter
3. Inject both the imperative and reactive data sources
4. Retrieve the commands to manipulate the counters
5. Retrieve the commands to manipulate the keys
6. Retrieve the value associated with the given key. It `null`, returns 0.
7. Set the value associated with the given key
8. Increment the value associated with the given key
9. Delete a key (and its associated value)
10. List all the keys

## Creating the Increment Resource

Create the `src/main/java/org/acme/redis/IncrementResource.java` file, with the following content:

```java
package org.acme.redis;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import java.util.List;

import io.smallrye.mutiny.Uni;

@Path("/increments")
public class IncrementResource {

    @Inject
    IncrementService service;

    @GET
    public Uni<List<String>> keys() {
        return service.keys();
    }

    @POST
    public Increment create(Increment increment) {
        service.set(increment.key, increment.value);
        return increment;
    }

    @GET
    @Path("/{key}")
    public Increment get(String key) {
        return new Increment(key, service.get(key));
    }

    @PUT
    @Path("/{key}")
    public void increment(String key, long value) {
        service.increment(key, value);
    }

    @DELETE
    @Path("/{key}")
    public Uni<Void> delete(String key) {
        return service.del(key);
    }
}
```

## Creating the test class

Edit the `pom.xml` file to add the following dependency:

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

Create the `src/test/java/org/acme/redis/IncrementResourceTest.java` file with the following content:

```java
package org.acme.redis;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;

@QuarkusTest
public class IncrementResourceTest {

    @Test
    public void testRedisOperations() {
        // verify that we have nothing
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/increments")
                .then()
                .statusCode(200)
                .body("size()", is(0));

        // create a first increment key with an initial value of 0
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"key\":\"first-key\",\"value\":0}")
                .when()
                .post("/increments")
                .then()
                .statusCode(200)
                .body("key", is("first-key"))
                .body("value", is(0));

        // create a second increment key with an initial value of 10
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{\"key\":\"second-key\",\"value\":10}")
                .when()
                .post("/increments")
                .then()
                .statusCode(200)
                .body("key", is("second-key"))
                .body("value", is(10));

        // increment first key by 1
        given()
                .contentType(ContentType.JSON)
                .body("1")
                .when()
                .put("/increments/first-key")
                .then()
                .statusCode(204);

        // verify that key has been incremented
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/increments/first-key")
                .then()
                .statusCode(200)
                .body("key", is("first-key"))
                .body("value", is(1));

        // increment second key by 1000
        given()
                .contentType(ContentType.JSON)
                .body("1000")
                .when()
                .put("/increments/second-key")
                .then()
                .statusCode(204);

        // verify that key has been incremented
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/increments/second-key")
                .then()
                .statusCode(200)
                .body("key", is("second-key"))
                .body("value", is(1010));

        // verify that we have two keys in registered
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/increments")
                .then()
                .statusCode(200)
                .body("size()", is(2));

        // delete first key
        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/increments/first-key")
                .then()
                .statusCode(204);

        // verify that we have one key left after deletion
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/increments")
                .then()
                .statusCode(200)
                .body("size()", is(1));

        // delete second key
        given()
                .accept(ContentType.JSON)
                .when()
                .delete("/increments/second-key")
                .then()
                .statusCode(204);

        // verify that there is no key left
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/increments")
                .then()
                .statusCode(200)
                .body("size()", is(0));
    }
}
```

## Get it running

If you followed the instructions, you should have the Redis server running. Then, you just need to run the application using:

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

Open another terminal and run the `curl http://localhost:8080/increments` command.

## Interacting with the application

As we have seen above, the API exposes five Rest endpoints. In this section we are going to see how to initialise an increment, see the list of current increments, incrementing a value given its key, retrieving the current value of an increment, and finally deleting a key.

### Creating a new increment

```bash
curl -X POST -H "Content-Type: application/json" -d '{"key":"first","value":10}' http://localhost:8080/increments <1>
```

1. We create the first increment, with the key `first` and an initial value of `10`.

Running the above command should return the result below:

---

{ "key": "first", "value": 10 }

---

### See current increments keys

To see the list of current increments keys, run the following command:

```bash
curl http://localhost:8080/increments
```

The above command should return  `["first"]` indicating that we have only one increment thus far.

### Retrieve a new increment

To retrieve an increment using its key, we will have to run the below command:

```bash
curl http://localhost:8080/increments/first <1>
```

1. Running this command, should return the following result:

```json
{
  "key": "first",
  "value": 10
}
```

### Increment a value given its key

To increment a value, run the following command:

```bash
curl -X PUT -H "Content-Type: application/json" -d '27' http://localhost:8080/increments/first <1>
```

1. Increment the `first` value by 27.

Now, running the command `curl http://localhost:8080/increments/first` should return the following result:

```json
{
  "key": "first",
  "value": 37 <1>
}
```

1. We see that the value of the `first` key is now `37` which is exactly the result of `10 + 27`, quick maths.

### Deleting a key

Use the command below, to delete an increment given its key.

```bash
curl -X DELETE  http://localhost:8080/increments/first <1>
```

1. Delete the `first` increment.

Now, running the command `curl http://localhost:8080/increments` should return an empty list `[]`

## Configuring for production

At this point, Quarkus uses the Redis Dev Service to run a Redis server and configure the application. However, in production, you will run your own Redis (or used a Cloud offering).

Let's start a Redis server on the port 6379 using:

```shell
docker run --ulimit memlock=-1:-1 -it --rm=true --memory-swappiness=0 --name redis_quarkus_test -p 6379:6379 docker.io/library/redis:7
```

Then, open the `src/main/resources/application.properties` file and add:

```properties
%prod.quarkus.redis.hosts=redis://localhost:6379
```

## Packaging and running in JVM mode

You can run the application as a conventional jar file.

First, we will need to package it:

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

> [!NOTE]
> This command will start a Redis instance to execute the tests.

Then run it:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Running Native

You can also create a native executable from this application without making any source code changes. A native executable removes the dependency on the JVM: everything needed to run the application on the target platform is included in the executable, allowing the application to run with minimal resource overhead.

Compiling a native executable takes a bit longer, as GraalVM performs additional steps to remove unnecessary codepaths. Use the  `native` profile to compile a native executable:

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

Once the build is finished, you can run the executable with:

```bash
./target/redis-quickstart-1.0.0-SNAPSHOT-runner
```

## Going further

To learn more about the Quarkus Redis extension, check [the Redis extension reference guide](/guides/redis-reference/index.md).
