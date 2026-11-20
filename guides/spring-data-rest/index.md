# Extension for Spring Data REST

While users are encouraged to use REST Data with Panache for the REST data access endpoints generation, Quarkus provides a compatibility layer for Spring Data REST in the form of the `spring-data-rest` extension.

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

The solution is located in the `spring-data-rest-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/spring-data-rest-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:spring-data-rest-quickstart \
    --extension='spring-data-rest,rest-jackson,quarkus-jdbc-postgresql' \
    --no-code
cd spring-data-rest-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=spring-data-rest-quickstart \
    -Dextensions='spring-data-rest,rest-jackson,quarkus-jdbc-postgresql' \
    -DnoCode
cd spring-data-rest-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=spring-data-rest-quickstart"`

This command generates a project with the `spring-data-rest` extension.

If you already have your Quarkus project configured, you can add the `spring-data-rest` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add spring-data-rest
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='spring-data-rest'
```

**Gradle**

```bash
./gradlew addExtension --extensions='spring-data-rest'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-spring-data-rest</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-spring-data-rest")
```

Furthermore, the following dependency needs to be added

For the tests you will also need REST Assured. Add it to the build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.rest-assured:rest-assured")
```

Note: both `resteasy-jackson` and `resteasy-jsonb` are supported and can be interchanged.

## Define the Entity

Throughout the course of this guide, the following JPA Entity will be used:

```java
package org.acme.spring.data.rest;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Fruit {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String color;


    public Fruit() {
    }

    public Fruit(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
```

## Configure database access properties

Add the following properties to `application.properties` to configure access to a database.

```properties
quarkus.datasource.db-kind=postgresql <1>

%prod.quarkus.datasource.username=quarkus_test
%prod.quarkus.datasource.password=quarkus_test
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql:quarkus_test
%prod.quarkus.hibernate-orm.schema-management.strategy=create <2>
```

1. [Configure the datasource](/guides/datasource/index.md) for production, relying on [Dev Services](/guides/datasource/index.md#dev-services) for connection information in tests / dev mode.
2. Configure Hibernate ORM to create the schema on startup in production, which is useful for experimentation, but rely on [convenient defaults in tests / dev mode](/guides/hibernate-orm/index.md#dev-mode).

## Prepare the data

To make it easier to showcase some capabilities of Spring Data REST on Quarkus, some test data should be inserted into the database by adding the following content to a new file named `src/main/resources/import.sql`:

```sql
INSERT INTO fruit(id, name, color) VALUES (1, 'Cherry', 'Red');
INSERT INTO fruit(id, name, color) VALUES (2, 'Apple', 'Red');
INSERT INTO fruit(id, name, color) VALUES (3, 'Banana', 'Yellow');
INSERT INTO fruit(id, name, color) VALUES (4, 'Avocado', 'Green');
INSERT INTO fruit(id, name, color) VALUES (5, 'Strawberry', 'Red');
```

Hibernate ORM will execute these queries on application startup.

## Define the repository

It is now time to define the repository that will be used to access `Fruit`. In a typical Spring Data fashion, create a repository like so:

```java
package org.acme.spring.data.rest;

import org.springframework.data.repository.CrudRepository;

public interface FruitsRepository extends CrudRepository<Fruit, Long> {
}
```

The `FruitsRepository` above extends Spring Data's `org.springframework.data.repository.CrudRepository` which means that all the latter's methods are available to `FruitsRepository`.

The `spring-data-jpa` extension will generate an implementation for this repository. Then the `spring-data-rest` extension will generate a REST CRUD resource for it.

## Update the test

To test the capabilities of `FruitsRepository` proceed to update the content of `FruitsRepositoryTest` to:

```java
package org.acme.spring.data.rest;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNot.not;

@QuarkusTest
class FruitsRepositoryTest {

    @Test
    void testListAllFruits() {
        //List all, should have all 3 fruits the database has initially:
        given()
                .accept("application/json")
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        containsString("Cherry"),
                        containsString("Apple"),
                        containsString("Banana")
                );

        //Delete the Cherry:
        given()
                .when().delete("/fruits/1")
                .then()
                .statusCode(204);

        //List all, cherry should be missing now:
        given()
                .accept("application/json")
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        not(containsString("Cherry")),
                        containsString("Apple"),
                        containsString("Banana")
                );

        //Create a new Fruit
        given()
                .contentType("application/json")
                .accept("application/json")
                .body("{\"name\": \"Orange\", \"color\": \"Orange\"}")
                .when().post("/fruits")
                .then()
                .statusCode(201)
                .body(containsString("Orange"))
                .body("id", notNullValue())
                .extract().body().jsonPath().getString("id");

        //List all, Orange should be present now:
        given()
                .accept("application/json")
                .when().get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        not(containsString("Cherry")),
                        containsString("Apple"),
                        containsString("Orange")
                );
    }
}

```

The test can be easily run by issuing:

**Maven**

```bash
./mvnw test
```

**Gradle**

```bash
./gradlew test
```

## Package and run the application

Quarkus dev mode works with the defined repositories just like with any other Quarkus extension, greatly enhancing your productivity during the dev cycle. The application can be started in dev mode as usual using:

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

## Run the application as a native binary

You can of course create a native executable following the instructions of the [Building native executables](/guides/building-native-image/index.md) guide.

## Supported Spring Data REST functionalities

Quarkus currently supports a subset of Spring Data REST features, namely the most useful and most commonly used features.

### What is supported

The following sections describe the most important supported features of Spring Data REST.

#### Automatic REST endpoint generation

Interfaces that extend any of the following Spring Data repositories get automatically generated REST endpoints:

- `org.springframework.data.repository.CrudRepository`
- `org.springframework.data.repository.PagingAndSortingRepository`
- `org.springframework.data.jpa.repository.JpaRepository`

Endpoints generated from the above repositories expose five common REST operations:

- `GET /fruits` - lists all entities or returns a page if `PagingAndSortingRepository` or `JpaRepository` is used.
- `GET /fruits/:id` - returns an entity by ID.
- `POST /fruits` - creates a new entity.
- `PUT /fruits/:id` - updates an existing entity or creates a new one with a specified ID (if allowed by the entity definition).
- `DELETE /fruits/:id` - deletes an entity by ID.

There are two supported data types: `application/json` and `application/hal+json`. The former is used by default, but it is highly recommended to specify which one you prefer with an `Accept` header.

#### Exposing many entities

If a database contains many entities, it might not be a great idea to return them all at once. `PagingAndSortingRepository` allows the `spring-data-rest` extension to access data in chunks.

So, you can extend the `PagingAndSortingRepository`:

```java
package org.acme.spring.data.rest;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface FruitsRepository extends CrudRepository<Fruit, Long>, PagingAndSortingRepository<Fruit, Long> {
}
```

Now the `GET /fruits` will accept three new query parameters: `sort`, `page` and `size`.

| Query parameter | Description | Default value | Example values |
| --- | --- | --- | --- |
| `sort` | Sorts the entities that are returned by the list operation | "" | `?sort=name` (ascending name), `?sort=name,-color` (ascending name and descending color) |
| `page` | Zero indexed page number. Invalid value is interpreted as 0. | 0 | 0, 11, 100 |
| `size` | Page size. Minimal accepted value is 1. Any lower value is interpreted as 1. | 20 | 1, 11, 100 |

For paged responses, `spring-data-rest` also returns a set of link headers that can be used to access other pages: first, previous, next and last.

Additionally, rather than extending both `PagingAndSortingRepository` and `CrudRepository`, you can use `JpaRepository`, which is a higher-level abstraction tailored for JPA. Since `JpaRepository` already extends both `PagingAndSortingRepository` and `CrudRepository`, it can replace `CrudRepository` directly.

```java
package org.acme.spring.data.rest;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface FruitsRepository extends JpaRepository<Fruit, Long> {
}
```

#### Fine tuning endpoints generation

This allows user to specify which methods should be exposed and what path should be used to access them. Spring Data REST provides two annotations that can be used: `@RepositoryRestResource` and `@RestResource`. `spring-data-rest` extension supports the `exported`, `path` `collectionResourceRel` attributes of these annotations.

Assume for example that fruits repository should be accessible by a `/my-fruits` path and only allow `GET` operation. In such a case, `FruitsRepository` would look like so:

```java
package org.acme.spring.data.rest;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@RepositoryRestResource(exported = false, path = "/my-fruits")
public interface FruitsRepository extends CrudRepository<Fruit, Long> {

    @RestResource(exported = true)
    Optional<Fruit> findById(Long id);

    @RestResource(exported = true)
    Iterable<Fruit> findAll();
}
```

`spring-data-rest` uses only a subset of the repository methods for data access. It is important to annotate the correct method in order to customize its REST endpoint:

| REST operation | CrudRepository | PagingAndSortingRepository and JpaRepository |
| --- | --- | --- |
| Get by ID | `Optional<T> findById(ID id)` | `Optional<T> findById(ID id)` |
| List | `Iterable<T> findAll()` | `Page<T> findAll(Pageable pageable)` |
| Create | `<S extends T> S save(S entity)` | `<S extends T> S save(S entity)` |
| Update | `<S extends T> S save(S entity)` | `<S extends T> S save(S entity)` |
| Delete | `void deleteById(ID id)` | `void deleteById(ID id)` |

#### Securing endpoints

This extension will automatically use the Security annotations within the package `jakarta.annotation.security` that are defined on your resource interfaces:

```java
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;

@DenyAll
public interface FruitResource extends CrudRepository<Fruit, Long> {
    @RolesAllowed("superuser")
    Iterable<Fruit> findAll();
}
```

Note that this feature is provided by the REST Data with Panache extension that this extension is using under the hood. So, pure Spring Boot applications might not behave the same way.

### What is currently unsupported

- Only the repository methods listed above are supported. No other standard or custom methods are supported.
- Only the `exposed`, `path` and `collectionResourceRel` annotation properties are supported.

## Important Technical Note

Please note that the Spring support in Quarkus does not start a Spring Application Context nor are any Spring infrastructure classes run. Spring classes and annotations are only used for reading metadata and / or are used as user code method return types or parameter types.

## More Spring guides

Quarkus has more Spring compatibility features. See the following guides for more details:

- [Quarkus - Extension for Spring Data JPA](/guides/spring-data-jpa/index.md)
- [Quarkus - Extension for Spring DI](/guides/spring-di/index.md)
- [Quarkus - Extension for Spring Web](/guides/spring-web/index.md)
- [Quarkus - Extension for Spring Security](/guides/spring-security/index.md)
- [Quarkus - Reading properties from Spring Cloud Config Server](/guides/spring-cloud-config-client/index.md)
- [Quarkus - Extension for Spring Boot properties](/guides/spring-boot-properties/index.md)
- [Quarkus - Extension for Spring Cache](/guides/spring-cache/index.md)
- [Quarkus - Extension for Spring Scheduled](/guides/spring-scheduled/index.md)
- [Quarkus - Extension for Spring Transaction](/guides/spring-tx/index.md)
- [Testing Spring-style Applications in Quarkus](/guides/spring-test/index.md)
