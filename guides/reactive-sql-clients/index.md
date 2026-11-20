# Reactive SQL clients

The Reactive SQL Clients have a straightforward API focusing on scalability and low-overhead. Currently, the following database servers are supported:

- IBM Db2
- PostgreSQL
- MariaDB/MySQL
- Microsoft SQL Server
- Oracle

In this guide, you will learn how to implement a simple CRUD application exposing data stored in **PostgreSQL** over a RESTful API.

> [!NOTE]
> Extension and connection pool class names for each client can be found at the bottom of this document.

> [!IMPORTANT]
> If you are not familiar with the Quarkus Vert.x extension, consider reading the [Using Eclipse Vert.x](/guides/vertx/index.md) guide first.

The application shall manage fruit entities:

**src/main/java/org/acme/reactive/crud/Fruit.java**

```java
package org.acme.reactive.crud;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

public class Fruit {

    public Long id;

    public String name;

    public Fruit() {
        // default constructor.
    }

    public Fruit(String name) {
        this.name = name;
    }

    public Fruit(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

> [!TIP]
> If you start the application in dev mode, Quarkus provides you with a [zero-config database](https://quarkus.io/guides/databases-dev-services) out of the box.
>
> You might also start a database up front:
>
> ```bash
> docker run -it --rm=true --name quarkus_test -e POSTGRES_USER=quarkus_test -e POSTGRES_PASSWORD=quarkus_test -e POSTGRES_DB=quarkus_test -p 5432:5432 docker.io/library/postgres:18
> ```

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `getting-started-reactive-crud` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/getting-started-reactive-crud).

## Installing

### Reactive PostgreSQL Client extension

First, make sure your project has the `quarkus-reactive-pg-client` extension enabled. If you are creating a new project, use the following command:

**CLI**

```bash
quarkus create app org.acme:reactive-pg-client-quickstart \
    --extension='rest,reactive-pg-client' \
    --no-code
cd reactive-pg-client-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=reactive-pg-client-quickstart \
    -Dextensions='rest,reactive-pg-client' \
    -DnoCode
cd reactive-pg-client-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=reactive-pg-client-quickstart"`

If you have an already created project, the `reactive-pg-client` extension can be added to an existing Quarkus project with the `add-extension` command:

**CLI**

```bash
quarkus extension add reactive-pg-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='reactive-pg-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='reactive-pg-client'
```

Otherwise, you can manually add the dependency to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-reactive-pg-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-reactive-pg-client")
```

### Mutiny

Quarkus REST (formerly RESTEasy Reactive) includes supports for Mutiny types (e.g. `Uni` and `Multi`) out of the box.

> [!TIP]
> In this guide, we will use the Mutiny API of the Reactive PostgreSQL Client. If you are not familiar with Mutiny, check [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

### JSON Binding

We will expose `Fruit` instances over HTTP in the JSON format. Consequently, you must also add the `quarkus-rest-jackson` extension:

**CLI**

```bash
quarkus extension add rest-jackson
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='rest-jackson'
```

**Gradle**

```bash
./gradlew addExtension --extensions='rest-jackson'
```

If you prefer not to use the command line, manually add the dependency to your build file:

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

Of course, this is only a requirement for this guide, not any application using the Reactive PostgreSQL Client.

## Configuring

The Reactive PostgreSQL Client can be configured with standard Quarkus datasource properties and a reactive URL:

**src/main/resources/application.properties**

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=quarkus_test
quarkus.datasource.password=quarkus_test
quarkus.datasource.reactive.url=postgresql://localhost:5432/quarkus_test
```

With that you can create your `FruitResource` skeleton and inject a `io.vertx.mutiny.sqlclient.Pool` instance:

**src/main/java/org/acme/vertx/FruitResource.java**

```java
package org.acme.reactive.crud;

import java.net.URI;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

@Path("fruits")
public class FruitResource {

    private final Pool client;

    public FruitResource(Pool client) {
        this.client = client;
    }
}
```

## Database schema and seed data

Before we implement the REST endpoint and data management code, we must set up the database schema. It would also be convenient to have some data inserted up front.

For production, we would recommend to use something like the [Flyway database migration tool](/guides/flyway/index.md). But for development we can simply drop and create the tables on startup, and then insert a few fruits.

**/src/main/java/org/acme/reactive/crud/DBInit.java**

```java
package org.acme.reactive.crud;

import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.sqlclient.Pool;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class DBInit {

    private final Pool client;
    private final boolean schemaCreate;

    public DBInit(Pool client, @ConfigProperty(name = "myapp.schema.create", defaultValue = "true") boolean schemaCreate) {
        this.client = client;
        this.schemaCreate = schemaCreate;
    }

    void onStart(@Observes StartupEvent ev) {
        if (schemaCreate) {
            initdb();
        }
    }

    private void initdb() {
        // TODO
    }
}
```

> [!TIP]
> You might override the default value of the `myapp.schema.create` property in the `application.properties` file.

Almost ready! To initialize the DB in development mode, we will use the client simple `query` method. It returns a `Uni` and thus can be composed to execute queries sequentially:

```java
client.query("DROP TABLE IF EXISTS fruits").execute()
    .flatMap(r -> client.query("CREATE TABLE fruits (id SERIAL PRIMARY KEY, name TEXT NOT NULL)").execute())
    .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Kiwi')").execute())
    .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Durian')").execute())
    .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Pomelo')").execute())
    .flatMap(r -> client.query("INSERT INTO fruits (name) VALUES ('Lychee')").execute())
    .await().indefinitely();
```

> [!NOTE]
> Wondering why we must block until the latest query is completed? This code is part of a method that `@Observes` the `StartupEvent` and Quarkus invokes it synchronously. As a consequence, returning prematurely could lead to serving requests while the database is not ready yet.

That's it! So far we have seen how to configure a pooled client and execute simple queries. We are now ready to develop the data management code and implement our RESTful endpoint.

## Using

### Query results traversal

In development mode, the database is set up with a few rows in the `fruits` table. To retrieve all the data, we will use the `query` method again:

**/src/main/java/org/acme/reactive/crud/Fruit.java**

```java
    public static Multi<Fruit> findAll(Pool client) {
        return client.query("SELECT id, name FROM fruits ORDER BY name ASC").execute()
                .onItem().transformToMulti(set -> Multi.createFrom().iterable(set)) // <1>
                .onItem().transform(Fruit::from); // <2>
    }

    private static Fruit from(Row row) {
        return new Fruit(row.getLong("id"), row.getString("name"));
    }
```

1. Transform the `io.vertx.mutiny.sqlclient.RowSet` to a `Multi<Row>`.
2. Convert each `io.vertx.mutiny.sqlclient.Row` to a `Fruit`.

The `Fruit#from` method converts a `Row` instance to a `Fruit` instance. It is extracted as a convenience for the implementation of the other data management methods.

Then, add the endpoint to get all fruits from the backend:

**src/main/java/org/acme/vertx/FruitResource.java**

```java
@GET
public Multi<Fruit> get() {
    return Fruit.findAll(client);
}
```

Now start Quarkus in dev mode with:

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

Lastly, open your browser and navigate to <http://localhost:8080/fruits>, you should see:

```json
[{"id":2,"name":"Durian"},{"id":1,"name":"Kiwi"},{"id":4,"name":"Lychee"},{"id":3,"name":"Pomelo"}]
```

### Prepared queries

The Reactive PostgreSQL Client can also prepare queries and take parameters that are replaced in the SQL statement at execution time:

```java
client.preparedQuery("SELECT id, name FROM fruits WHERE id = $1").execute(Tuple.of(id))
```

> [!TIP]
> For PostgreSQL, the SQL string can refer to parameters by position, using `$1`, `$2`, ...etc. Please refer to the [Database Clients details](#reactive-sql-clients-details) section for other databases.

Similar to the simple `query` method, `preparedQuery` returns an instance of `PreparedQuery<RowSet<Row>>`. Equipped with this tooling, we are able to safely use an `id` provided by the user to get the details of a particular fruit:

**src/main/java/org/acme/vertx/Fruit.java**

```java
public static Uni<Fruit> findById(Pool client, Long id) {
    return client.preparedQuery("SELECT id, name FROM fruits WHERE id = $1").execute(Tuple.of(id)) // <1>
            .onItem().transform(RowSet::iterator) // <2>
            .onItem().transform(iterator -> iterator.hasNext() ? from(iterator.next()) : null); // <3>
}
```

1. Create a `Tuple` to hold the prepared query parameters.
2. Get an `Iterator` for the `RowSet` result.
3. Create a `Fruit` instance from the `Row` if an entity was found.

And in the Jakarta REST resource:

**src/main/java/org/acme/vertx/FruitResource.java**

```java
@GET
@Path("{id}")
public Uni<Response> getSingle(Long id) {
    return Fruit.findById(client, id)
            .onItem().transform(fruit -> fruit != null ? Response.ok(fruit) : Response.status(Status.NOT_FOUND)) // <1>
            .onItem().transform(ResponseBuilder::build); // <2>
}
```

1. Prepare a Jakarta REST response with  either the `Fruit` instance if found or the `404` status code.
2. Build and send the response.

The same logic applies when saving a `Fruit`:

**src/main/java/org/acme/vertx/Fruit.java**

```java
public Uni<Long> save(Pool client) {
    return client.preparedQuery("INSERT INTO fruits (name) VALUES ($1) RETURNING id").execute(Tuple.of(name))
            .onItem().transform(pgRowSet -> pgRowSet.iterator().next().getLong("id"));
}
```

And in the web resource we handle the `POST` request:

**src/main/java/org/acme/vertx/FruitResource.java**

```java
@POST
public Uni<Response> create(Fruit fruit) {
    return fruit.save(client)
            .onItem().transform(id -> URI.create("/fruits/" + id))
            .onItem().transform(uri -> Response.created(uri).build());
}
```

### Result metadata

A `RowSet` does not only hold your data in memory, it also gives you some information about the data itself, such as:

- the number of rows affected by the query (inserted/deleted/updated/retrieved depending on the query type),
- the column names.

Let's use this to support removal of fruits in the database:

**src/main/java/org/acme/vertx/Fruit.java**

```java
public static Uni<Boolean> delete(Pool client, Long id) {
    return client.preparedQuery("DELETE FROM fruits WHERE id = $1").execute(Tuple.of(id))
            .onItem().transform(pgRowSet -> pgRowSet.rowCount() == 1); // <1>
}
```

1. Inspect metadata to determine if a fruit has been actually deleted.

And to handle the HTTP `DELETE` method in the web resource:

**src/main/java/org/acme/vertx/FruitResource.java**

```java
@DELETE
@Path("{id}")
public Uni<Response> delete(Long id) {
    return Fruit.delete(client, id)
            .onItem().transform(deleted -> deleted ? Status.NO_CONTENT : Status.NOT_FOUND)
            .onItem().transform(status -> Response.status(status).build());
}
```

With `GET`, `POST` and `DELETE` methods implemented, we can now create a minimal web page to try the RESTful application out. We will use [jQuery](https://jquery.com/) to simplify interactions with the backend:

**/src/main/resources/META-INF/resources/fruits.html**

```html
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <title>Reactive REST - Quarkus</title>
    <script src="https://code.jquery.com/jquery-3.3.1.min.js"
            integrity="sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" crossorigin="anonymous"></script>
    <script type="application/javascript" src="fruits.js"></script>
</head>
<body>

<h1>Fruits API Testing</h1>

<h2>All fruits</h2>
<div id="all-fruits"></div>

<h2>Create Fruit</h2>
<input id="fruit-name" type="text">
<button id="create-fruit-button" type="button">Create</button>
<div id="create-fruit"></div>

</body>
</html>
```

> [!TIP]
> Quarkus automatically serves static resources located under the `META-INF/resources` directory.

In the JavaScript code, we need a function to refresh the list of fruits when:

- the page is loaded, or
- a fruit is added, or
- a fruit is deleted.

**/src/main/resources/META-INF/resources/fruits.js**

```javascript
function refresh() {
    $.get('/fruits', function (fruits) {
        var list = '';
        (fruits || []).forEach(function (fruit) { // <1>
            list = list
                + '<tr>'
                + '<td>' + fruit.id + '</td>'
                + '<td>' + fruit.name + '</td>'
                + '<td><a href="#" onclick="deleteFruit(' + fruit.id + ')">Delete</a></td>'
                + '</tr>'
        });
        if (list.length > 0) {
            list = ''
                + '<table><thead><th>Id</th><th>Name</th><th></th></thead>'
                + list
                + '</table>';
        } else {
            list = "No fruits in database"
        }
        $('#all-fruits').html(list);
    });
}

function deleteFruit(id) {
    $.ajax('/fruits/' + id, {method: 'DELETE'}).then(refresh);
}

$(document).ready(function () {

    $('#create-fruit-button').click(function () {
        var fruitName = $('#fruit-name').val();
        $.post({
            url: '/fruits',
            contentType: 'application/json',
            data: JSON.stringify({name: fruitName})
        }).then(refresh);
    });

    refresh();
});
```

1. The `fruits` parameter is not defined when the database is empty.

All done! Navigate to <http://localhost:8080/fruits.html> and read/create/delete some fruits.

<a id="reactive-sql-clients-details"></a>
## Database Clients details

| Database | Extension name | Placeholders |
| --- | --- | --- |
| IBM Db2 | `quarkus-reactive-db2-client` | `?` |
| MariaDB/MySQL | `quarkus-reactive-mysql-client` | `?` |
| Microsoft SQL Server | `quarkus-reactive-mssql-client` | `@p1`, `@p2`, etc. |
| Oracle | `quarkus-reactive-oracle-client` | `?` |
| PostgreSQL | `quarkus-reactive-pg-client` | `$1`, `$2`, etc. |

## Transactions

The reactive SQL clients support transactions. A transaction is started with `io.vertx.mutiny.sqlclient.SqlConnection#begin` and terminated with either `io.vertx.mutiny.sqlclient.Transaction#commit` or `io.vertx.mutiny.sqlclient.Transaction#rollback`. All these operations are asynchronous:

- `connection.begin()` returns a `Uni<Transaction>`
- `transaction.commit()` and `transaction.rollback()` return `Uni<Void>`

Managing transactions in the reactive programming world can be cumbersome. Instead of writing repetitive and complex (thus error-prone!) code, you can use the `io.vertx.mutiny.sqlclient.Pool#withTransaction` helper method.

The following snippet shows how to run 2 insertions in the same transaction:

```java
public static Uni<Void> insertTwoFruits(Pool client, Fruit fruit1, Fruit fruit2) {
    return client.withTransaction(conn -> {
        Uni<RowSet<Row>> insertOne = conn.preparedQuery("INSERT INTO fruits (name) VALUES ($1) RETURNING id")
                .execute(Tuple.of(fruit1.name));
        Uni<RowSet<Row>> insertTwo = conn.preparedQuery("INSERT INTO fruits (name) VALUES ($1) RETURNING id")
                .execute(Tuple.of(fruit2.name));

        return Uni.combine().all().unis(insertOne, insertTwo)
                // Ignore the results (the two ids)
                .discardItems();
    });
}
```

In this example, the transaction is automatically committed on success or rolled back on failure.

You can also create dependent actions as follows:

```java
return client.withTransaction(conn -> conn

        .preparedQuery("INSERT INTO person (firstname,lastname) VALUES ($1,$2) RETURNING id")
        .execute(Tuple.of(person.getFirstName(), person.getLastName()))

        .onItem().transformToUni(id -> conn.preparedQuery("INSERT INTO addr (person_id,addrline1) VALUES ($1,$2)")
                .execute(Tuple.of(id.iterator().next().getLong("id"), person.getLastName())))

        .onItem().ignore().andContinueWithNull());
```

## Working with batch query results

When executing batch queries, reactive SQL clients return a `RowSet` that corresponds to the results of the first element in the batch. To get the results of the following batch elements, you must invoke the `RowSet#next` method until it returns `null`.

Let's say you want to update some rows and compute the total number of affected rows. You must inspect each `RowSet`:

```java
PreparedQuery<RowSet<Row>> preparedQuery = client.preparedQuery("UPDATE fruits SET name = $1 WHERE id = $2");

Uni<RowSet<Row>> rowSet = preparedQuery.executeBatch(Arrays.asList(
        Tuple.of("Orange", 1),
        Tuple.of("Pear", 2),
        Tuple.of("Apple", 3)));

Uni<Integer> totalAffected = rowSet.onItem().transform(res -> {
    int total = 0;
    do {
        total += res.rowCount(); // <1>
    } while ((res = res.next()) != null); // <2>
    return total;
});
```

1. Compute the sum of `RowSet#rowCount`.
2. Invoke `RowSet#next` until it returns `null`.

As another example, if you want to load all the rows you just inserted, you must concatenate the contents of each `RowSet`:

```java
PreparedQuery<RowSet<Row>> preparedQuery = client.preparedQuery("INSERT INTO fruits (name) VALUES ($1) RETURNING *");

Uni<RowSet<Row>> rowSet = preparedQuery.executeBatch(Arrays.asList(
        Tuple.of("Orange"),
        Tuple.of("Pear"),
        Tuple.of("Apple")));

// Generate a Multi of RowSet items
Multi<RowSet<Row>> rowSets = rowSet.onItem().transformToMulti(res -> {
    return Multi.createFrom().generator(() -> res, (rs, emitter) -> {
        RowSet<Row> next = null;
        if (rs != null) {
            emitter.emit(rs);
            next = rs.next();
        }
        if (next == null) {
            emitter.complete();
        }
        return next;
    });
});

// Transform each RowSet into Multi of Row items and Concatenate
Multi<Row> rows = rowSets.onItem().transformToMultiAndConcatenate(Multi.createFrom()::iterable);
```

## Multiple Datasources

The reactive SQL clients support defining several datasources.

A typical configuration with several datasources would look like:

```properties
quarkus.datasource.db-kind=postgresql <1>
quarkus.datasource.username=user-default
quarkus.datasource.password=password-default
quarkus.datasource.reactive.url=postgresql://localhost:5432/default

quarkus.datasource."additional1".db-kind=postgresql <2>
quarkus.datasource."additional1".username=user-additional1
quarkus.datasource."additional1".password=password-additional1
quarkus.datasource."additional1".reactive.url=postgresql://localhost:5432/additional1

quarkus.datasource."additional2".db-kind=mysql <3>
quarkus.datasource."additional2".username=user-additional2
quarkus.datasource."additional2".password=password-additional2
quarkus.datasource."additional2".reactive.url=mysql://localhost:3306/additional2
```

1. The default datasource - using PostgreSQL.
2. A named datasource called `additional1` - using PostgreSQL.
3. A named datasource called `additional2` - using MySQL.

You can then inject the clients as follows:

```java
@Inject <1>
Pool defaultClient;

@Inject
@ReactiveDataSource("additional1") <2>
Pool additional1Client;

@Inject
@ReactiveDataSource("additional2")
MySQLPool additional2Client;
```

1. Injecting the client for the default datasource does not require anything special.
2. For a named datasource, you use the `@ReactiveDataSource` CDI qualifier with the datasource name as its value.

## UNIX Domain Socket connections

The PostgreSQL and MariaDB/MySQL clients can be configured to connect to the server through a UNIX domain socket.

First make sure that [native transport support](/guides/vertx-reference/index.md#native-transport) is enabled.

Then configure the database connection url. This step depends on the database type.

### PostgreSQL

PostgreSQL domain socket paths have the following form: `<directory>/.s.PGSQL.<port>`

The database connection url must be configured so that:

- the `host` is the `directory` in the socket path
- the `port` is the `port` in the socket path

Consider the following socket path: `/var/run/postgresql/.s.PGSQL.5432`.

In `application.properties` add:

```properties
quarkus.datasource.reactive.url=postgresql://:5432/quarkus_test?host=/var/run/postgresql
```

### MariaDB/MySQL

The database connection url must be configured so that the `host` is the socket path.

Consider the following socket path: `/var/run/mysqld/mysqld.sock`.

In `application.properties` add:

```properties
quarkus.datasource.reactive.url=mysql:///quarkus_test?host=/var/run/mysqld/mysqld.sock
```

## Load-balancing connections

The reactive PostgreSQL and MariaDB/MySQL clients support defining several connections.

A typical configuration with several connections would look like:

```properties
quarkus.datasource.reactive.url=postgresql://host1:5432/default,postgresql://host2:5432/default,postgresql://host3:5432/default
```

This can also be written with indexed property syntax:

```properties
quarkus.datasource.reactive.url[0]=postgresql://host1:5432/default
quarkus.datasource.reactive.url[1]=postgresql://host2:5432/default
quarkus.datasource.reactive.url[2]=postgresql://host3:5432/default
```

## Pooled connection `idle-timeout`

Reactive datasources can be configured with an `idle-timeout`. It is the maximum time a connection remains unused in the pool before it is closed.

> [!NOTE]
> The `idle-timeout` is disabled by default.

For example, you could expire idle connections after 60 minutes:

```properties
quarkus.datasource.reactive.idle-timeout=PT60M
```

## Pooled Connection `max-lifetime`

In addition to `idle-timeout`, reactive datasources can also be configured with a `max-lifetime`. It is the maximum time a connection remains in the pool before it is closed and replaced as needed. The `max-lifetime` allows ensuring the pool has fresh connections with up-to-date configuration.

> [!NOTE]
> The `max-lifetime` is disabled by default but is an important configuration when using a credentials provider that provides time limited credentials, like the [Vault credentials provider](/guides/credentials-provider/index.md).

For example, you could ensure connections are recycled after 60 minutes:

```properties
quarkus.datasource.reactive.max-lifetime=PT60M
```

## Customizing pool creation

Sometimes, the database connection pool cannot be configured only by declaration.

For example, you might have to read a specific file only present in production, or retrieve configuration data from a proprietary configuration server.

In this case, you can customize pool creation by creating a class implementing an interface which depends on the target database:

| Database | Pool creator class name |
| --- | --- |
| IBM Db2 | `io.quarkus.reactive.db2.client.DB2PoolCreator` |
| MariaDB/MySQL | `io.quarkus.reactive.mysql.client.MySQLPoolCreator` |
| Microsoft SQL Server | `io.quarkus.reactive.mssql.client.MSSQLPoolCreator` |
| Oracle | `io.quarkus.reactive.oracle.client.OraclePoolCreator` |
| PostgreSQL | `io.quarkus.reactive.pg.client.PgPoolCreator` |

Here's an example for PostgreSQL:

```java
import jakarta.inject.Singleton;

import io.quarkus.reactive.pg.client.PgPoolCreator;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

@Singleton
public class CustomPgPoolCreator implements PgPoolCreator {

    @Override
    public Pool create(Input input) {
        PgConnectOptions connectOptions = input.pgConnectOptions();
        PoolOptions poolOptions = input.poolOptions();
        // Customize connectOptions, poolOptions or both, as required
        return Pool.pool(input.vertx(), connectOptions, poolOptions);
    }
}
```

## Pipelining

The PostgreSQL and MariaDB/MySQL clients support pipelining of queries at the connection level. The feature consists in sending multiple queries on the same database connection without waiting for the corresponding responses.

In some use cases, query pipelining can improve database access performance.

Here's an example for PostgreSQL:

```java
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;

public class PipeliningExample {

    @Inject
    Pool client;

    public Uni<String> favoriteFruitAndVegetable() {
        // Explicitly acquire a connection
        return client.withConnection(conn -> {
            Uni<String> favoriteFruit = conn.query("SELECT name FROM fruits WHERE preferred IS TRUE").execute()
                    .onItem().transform(rows -> rows.iterator().next().getString("name"));
            Uni<String> favoriteVegetable = conn.query("SELECT name FROM vegetables WHERE preferred IS TRUE").execute()
                    .onItem().transform(rows -> rows.iterator().next().getString("name"));
            // favoriteFruit and favoriteVegetable unis will be subscribed at the same time
            return Uni.combine().all().unis(favoriteFruit, favoriteVegetable)
                    .combinedWith(PipeliningExample::formatMessage);
        });
    }

    private static String formatMessage(String fruit, String vegetable) {
        return String.format("The favorite fruit is %s and the favorite vegetable is %s", fruit, vegetable);
    }
}
```

The maximum number of pipelined queries is configured with the `pipelining-limit` property:

```properties
# For PostgreSQL
quarkus.datasource.reactive.postgresql.pipelining-limit=256
# For MariaDB/MySQL
quarkus.datasource.reactive.mysql.pipelining-limit=256
```

By default, `pipelining-limit` is set to 256.

## Configuration Reference

### Common Datasource

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-datasource_quarkus-datasource-health-enabled"></a> [`quarkus.datasource.health.enabled`](#quarkus-datasource_quarkus-datasource-health-enabled) config_property_copy_button:+++quarkus.datasource.health.enabled+++[]<br><br>Whether or not a health check is published in case the smallrye-health extension is present.<br><br>This is a global setting and is not specific to a datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_HEALTH_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-datasource_quarkus-datasource-metrics-enabled"></a> [`quarkus.datasource.metrics.enabled`](#quarkus-datasource_quarkus-datasource-metrics-enabled) config_property_copy_button:+++quarkus.datasource.metrics.enabled+++[]<br><br>Whether datasource metrics are published in case a metrics extension is present.<br><br>This is a global setting and is not specific to a datasource.<br><br>> [!NOTE]<br>> This is different from the `quarkus.datasource."datasource name".jdbc.metrics.enabled` property that needs to be set on the JDBC datasource level to enable collection of metrics for that datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_METRICS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-datasource_quarkus-datasource-db-kind"></a> [`quarkus.datasource.db-kind`](#quarkus-datasource_quarkus-datasource-db-kind) config_property_copy_button:+++quarkus.datasource.db-kind+++[]<br><br>`quarkus.datasource."datasource-name".db-kind` config_property_copy_button:+++quarkus.datasource."datasource-name".db-kind+++[]<br><br>The kind of database we will connect to (e.g. h2, postgresql...).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DB_KIND+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-db-version"></a> [`quarkus.datasource.db-version`](#quarkus-datasource_quarkus-datasource-db-version) config_property_copy_button:+++quarkus.datasource.db-version+++[]<br><br>`quarkus.datasource."datasource-name".db-version` config_property_copy_button:+++quarkus.datasource."datasource-name".db-version+++[]<br><br>The version of the database we will connect to (e.g. '10.0').<br><br>> [!CAUTION]<br>> The version number set here should follow the same numbering scheme as the string returned by `java.sql.DatabaseMetaData#getDatabaseProductVersion()` for your database's JDBC driver. This numbering scheme may be different from the most popular one for your database; for example Microsoft SQL Server 2016 would be version `13`, and Oracle Database 26ai would be version `23.26`.<br><br>As a rule, the version set here should be as high as possible, but must be lower than or equal to the version of any database your application will connect to.<br><br>A high version will allow better performance and using more features (e.g. Hibernate ORM may generate more efficient SQL, avoid workarounds and take advantage of more database features), but if it is higher than the version of the database you want to connect to, it may lead to runtime exceptions (e.g. Hibernate ORM may generate invalid SQL that your database will reject).<br><br>Some extensions (like the Hibernate ORM extension) will try to check this version against the actual database version on startup, leading to a startup failure when the actual version is lower or simply a warning in case the database cannot be reached.<br><br>The default for this property is specific to each extension; the Hibernate ORM extension will default to the oldest version it supports.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DB_VERSION+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-health-exclude"></a> [`quarkus.datasource.health-exclude`](#quarkus-datasource_quarkus-datasource-health-exclude) config_property_copy_button:+++quarkus.datasource.health-exclude+++[]<br><br>`quarkus.datasource."datasource-name".health-exclude` config_property_copy_button:+++quarkus.datasource."datasource-name".health-exclude+++[]<br><br>Whether this particular data source should be excluded from the health check if the general health check for data sources is enabled.<br><br>By default, the health check includes all configured data sources (if it is enabled).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_HEALTH_EXCLUDE+++[] | boolean | `false` |
| <a id="quarkus-datasource_quarkus-datasource-active"></a> [`quarkus.datasource.active`](#quarkus-datasource_quarkus-datasource-active) config_property_copy_button:+++quarkus.datasource.active+++[]<br><br>`quarkus.datasource."datasource-name".active` config_property_copy_button:+++quarkus.datasource."datasource-name".active+++[]<br><br>Whether this datasource should be active at runtime.<br><br>See [this section of the documentation](/guides/datasource/index.md#datasource-active).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_ACTIVE+++[] | boolean | `` `true` if the URL is set, `false` otherwise `` |
| <a id="quarkus-datasource_quarkus-datasource-username"></a> [`quarkus.datasource.username`](#quarkus-datasource_quarkus-datasource-username) config_property_copy_button:+++quarkus.datasource.username+++[]<br><br>`quarkus.datasource."datasource-name".username` config_property_copy_button:+++quarkus.datasource."datasource-name".username+++[]<br><br>The datasource username<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_USERNAME+++[] | string |  |
| <a id="quarkus-datasource_quarkus-datasource-password"></a> [`quarkus.datasource.password`](#quarkus-datasource_quarkus-datasource-password) config_property_copy_button:+++quarkus.datasource.password+++[]<br><br>`quarkus.datasource."datasource-name".password` config_property_copy_button:+++quarkus.datasource."datasource-name".password+++[]<br><br>The datasource password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_PASSWORD+++[] | string |  |
| <a id="quarkus-datasource_quarkus-datasource-credentials-provider"></a> [`quarkus.datasource.credentials-provider`](#quarkus-datasource_quarkus-datasource-credentials-provider) config_property_copy_button:+++quarkus.datasource.credentials-provider+++[]<br><br>`quarkus.datasource."datasource-name".credentials-provider` config_property_copy_button:+++quarkus.datasource."datasource-name".credentials-provider+++[]<br><br>The credentials provider name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_CREDENTIALS_PROVIDER+++[] | string |  |
| <a id="quarkus-datasource_quarkus-datasource-credentials-provider-name"></a> [`quarkus.datasource.credentials-provider-name`](#quarkus-datasource_quarkus-datasource-credentials-provider-name) config_property_copy_button:+++quarkus.datasource.credentials-provider-name+++[]<br><br>`quarkus.datasource."datasource-name".credentials-provider-name` config_property_copy_button:+++quarkus.datasource."datasource-name".credentials-provider-name+++[]<br><br>The credentials provider bean name.<br><br>This is a bean name (as in `@Named`) of a bean that implements `CredentialsProvider`. It is used to select the credentials provider bean when multiple exist. This is unnecessary when there is only one credentials provider available.<br><br>For Vault, the credentials provider bean name is `vault-credentials-provider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-datasource_section_quarkus-datasource-devservices"></a> [Dev Services](#quarkus-datasource_section_quarkus-datasource-devservices) | Type | Default |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-enabled"></a> [`quarkus.datasource.devservices.enabled`](#quarkus-datasource_quarkus-datasource-devservices-enabled) config_property_copy_button:+++quarkus.datasource.devservices.enabled+++[]<br><br>`quarkus.datasource."datasource-name".devservices.enabled` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.enabled+++[]<br><br>Whether this Dev Service should start with the application in dev mode or tests.<br><br>Dev Services are enabled by default unless connection configuration (e.g. the JDBC URL or reactive client URL) is set explicitly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-image-name"></a> [`quarkus.datasource.devservices.image-name`](#quarkus-datasource_quarkus-datasource-devservices-image-name) config_property_copy_button:+++quarkus.datasource.devservices.image-name+++[]<br><br>`quarkus.datasource."datasource-name".devservices.image-name` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.image-name+++[]<br><br>The container image name for container-based Dev Service providers.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Defaults depend on the configured `datasource`:<br><br>- DB2: `icr.io/db2_community/db2:12.1.5.0`<br>- MariaDB: `docker.io/library/mariadb:12.1`<br>- Microsoft SQL Server: `{mssql-image}`\<br>On ARM (aarch64),`mcr.microsoft.com/azure-sql-edge:latest` is used instead, because the standard MS SQL Server image does not run on ARM.<br>- MySQL: `docker.io/library/mysql:9.5`<br>- Oracle Express Edition: `docker.io/gvenzl/oracle-free:23-slim-faststart`<br>- PostgreSQL: `docker.io/library/postgres:18`<br><br>For certain databases, the default image may be automatically adjusted based on required database features (e.g., spatial or vector support). Explicitly setting this property overrides the automatic image selection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-container-env-environment-variable-name"></a> [`quarkus.datasource.devservices.container-env."environment-variable-name"`](#quarkus-datasource_quarkus-datasource-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.datasource.devservices.container-env."environment-variable-name"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.container-env."environment-variable-name"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-container-properties-property-key"></a> [`quarkus.datasource.devservices.container-properties."property-key"`](#quarkus-datasource_quarkus-datasource-devservices-container-properties-property-key) config_property_copy_button:+++quarkus.datasource.devservices.container-properties."property-key"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.container-properties."property-key"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.container-properties."property-key"+++[]<br><br>Generic properties that are passed for additional container configuration.<br><br>Properties defined here are database-specific and are interpreted specifically in each database dev service implementation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_CONTAINER_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-properties-property-key"></a> [`quarkus.datasource.devservices.properties."property-key"`](#quarkus-datasource_quarkus-datasource-devservices-properties-property-key) config_property_copy_button:+++quarkus.datasource.devservices.properties."property-key"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.properties."property-key"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.properties."property-key"+++[]<br><br>Generic properties that are added to the database connection URL.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-port"></a> [`quarkus.datasource.devservices.port`](#quarkus-datasource_quarkus-datasource-devservices-port) config_property_copy_button:+++quarkus.datasource.devservices.port+++[]<br><br>`quarkus.datasource."datasource-name".devservices.port` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-command"></a> [`quarkus.datasource.devservices.command`](#quarkus-datasource_quarkus-datasource-devservices-command) config_property_copy_button:+++quarkus.datasource.devservices.command+++[]<br><br>`quarkus.datasource."datasource-name".devservices.command` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.command+++[]<br><br>The container start command to use for container-based Dev Service providers.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_COMMAND+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-db-name"></a> [`quarkus.datasource.devservices.db-name`](#quarkus-datasource_quarkus-datasource-devservices-db-name) config_property_copy_button:+++quarkus.datasource.devservices.db-name+++[]<br><br>`quarkus.datasource."datasource-name".devservices.db-name` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.db-name+++[]<br><br>The database name to use if this Dev Service supports overriding it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_DB_NAME+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-username"></a> [`quarkus.datasource.devservices.username`](#quarkus-datasource_quarkus-datasource-devservices-username) config_property_copy_button:+++quarkus.datasource.devservices.username+++[]<br><br>`quarkus.datasource."datasource-name".devservices.username` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.username+++[]<br><br>The username to use if this Dev Service supports overriding it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_USERNAME+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-password"></a> [`quarkus.datasource.devservices.password`](#quarkus-datasource_quarkus-datasource-devservices-password) config_property_copy_button:+++quarkus.datasource.devservices.password+++[]<br><br>`quarkus.datasource."datasource-name".devservices.password` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.password+++[]<br><br>The password to use if this Dev Service supports overriding it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_PASSWORD+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-init-script-path"></a> [`quarkus.datasource.devservices.init-script-path`](#quarkus-datasource_quarkus-datasource-devservices-init-script-path) config_property_copy_button:+++quarkus.datasource.devservices.init-script-path+++[]<br><br>`quarkus.datasource."datasource-name".devservices.init-script-path` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.init-script-path+++[]<br><br>The paths to SQL scripts to be loaded from the classpath and applied to the Dev Service database.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_INIT_SCRIPT_PATH+++[] | list of string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-init-privileged-script-path"></a> [`quarkus.datasource.devservices.init-privileged-script-path`](#quarkus-datasource_quarkus-datasource-devservices-init-privileged-script-path) config_property_copy_button:+++quarkus.datasource.devservices.init-privileged-script-path+++[]<br><br>`quarkus.datasource."datasource-name".devservices.init-privileged-script-path` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.init-privileged-script-path+++[]<br><br>The paths to SQL scripts to be loaded from the classpath and applied to the Dev Service database using the SYS privileged user. Not all databases provide a privileged user. In these cases, the property is ignored. This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_INIT_PRIVILEGED_SCRIPT_PATH+++[] | list of string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-volumes-host-path"></a> [`quarkus.datasource.devservices.volumes."host-path"`](#quarkus-datasource_quarkus-datasource-devservices-volumes-host-path) config_property_copy_button:+++quarkus.datasource.devservices.volumes."host-path"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.volumes."host-path"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.volumes."host-path"+++[]<br><br>The volumes to be mapped to the container.<br><br>The map key corresponds to the host location; the map value is the container location. If the host location starts with "classpath:", the mapping loads the resource from the classpath with read-only permission.<br><br>When using a file system location, the volume will be generated with read-write permission, potentially leading to data loss or modification in your file system.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_VOLUMES__HOST_PATH_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-reuse"></a> [`quarkus.datasource.devservices.reuse`](#quarkus-datasource_quarkus-datasource-devservices-reuse) config_property_copy_button:+++quarkus.datasource.devservices.reuse+++[]<br><br>`quarkus.datasource."datasource-name".devservices.reuse` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.reuse+++[]<br><br>Whether to keep Dev Service containers running **after a dev mode session or test suite execution** to reuse them in the next dev mode session or test suite execution.<br><br>Within a dev mode session or test suite execution, Quarkus will always reuse Dev Services as long as their configuration (username, password, environment, port bindings, ...) did not change. This feature is specifically about keeping containers running **when Quarkus is not running** to reuse them across runs.<br><br>> [!WARNING]<br>> This feature needs to be enabled explicitly in `testcontainers.properties`, may require changes to how you configure data initialization in dev mode and tests, and may leave containers running indefinitely, forcing you to stop and remove them manually. See [this section of the documentation](/guides/databases-dev-services/index.md#reuse) for more information.<br><br>This configuration property is set to `true` by default, so it is mostly useful to **disable** reuse, if you enabled it in `testcontainers.properties` but only want to use it for some of your Quarkus applications or datasources.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_REUSE+++[] | boolean | `true` |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices-show-logs"></a> [`quarkus.datasource.devservices.show-logs`](#quarkus-datasource_quarkus-datasource-devservices-show-logs) config_property_copy_button:+++quarkus.datasource.devservices.show-logs+++[]<br><br>`quarkus.datasource."datasource-name".devservices.show-logs` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.show-logs+++[]<br><br>Whether the logs should be consumed by the JBoss logger.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_SHOW_LOGS+++[] | boolean | `false` |

### Reactive Datasource

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-reactive-datasource_quarkus-datasource-reactive"></a> [`quarkus.datasource.reactive`](#quarkus-reactive-datasource_quarkus-datasource-reactive) config_property_copy_button:+++quarkus.datasource.reactive+++[]<br><br>`quarkus.datasource."datasource-name".reactive` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive+++[]<br><br>If we create a Reactive datasource for this datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE+++[] | boolean | `true` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-cache-prepared-statements"></a> [`quarkus.datasource.reactive.cache-prepared-statements`](#quarkus-reactive-datasource_quarkus-datasource-reactive-cache-prepared-statements) config_property_copy_button:+++quarkus.datasource.reactive.cache-prepared-statements+++[]<br><br>`quarkus.datasource."datasource-name".reactive.cache-prepared-statements` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.cache-prepared-statements+++[]<br><br>Whether prepared statements should be cached on the client side.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_CACHE_PREPARED_STATEMENTS+++[] | boolean | `true for PostgreSQL/MySQL/MariaDB/Db2, false otherwise` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-url"></a> [`quarkus.datasource.reactive.url`](#quarkus-reactive-datasource_quarkus-datasource-reactive-url) config_property_copy_button:+++quarkus.datasource.reactive.url+++[]<br><br>`quarkus.datasource."datasource-name".reactive.url` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.url+++[]<br><br>The datasource URLs.<br><br>If multiple values are set, this datasource will create a pool with a list of servers instead of a single server. The pool uses round-robin load balancing for server selection during connection establishment. Note that certain drivers might not accommodate multiple values in this context.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_URL+++[] | list of string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-max-size"></a> [`quarkus.datasource.reactive.max-size`](#quarkus-reactive-datasource_quarkus-datasource-reactive-max-size) config_property_copy_button:+++quarkus.datasource.reactive.max-size+++[]<br><br>`quarkus.datasource."datasource-name".reactive.max-size` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.max-size+++[]<br><br>The datasource pool maximum size.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE+++[] | int | `20` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-event-loop-size"></a> [`quarkus.datasource.reactive.event-loop-size`](#quarkus-reactive-datasource_quarkus-datasource-reactive-event-loop-size) config_property_copy_button:+++quarkus.datasource.reactive.event-loop-size+++[]<br><br>`quarkus.datasource."datasource-name".reactive.event-loop-size` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.event-loop-size+++[]<br><br>When a new connection object is created, the pool assigns it an event loop.<br><br>When `#event-loop-size` is set to a strictly positive value, the pool assigns as many event loops as specified, in a round-robin fashion. By default, the number of event loops configured or calculated by Quarkus is used. If `#event-loop-size` is set to zero or a negative value, the pool assigns the current event loop to the new connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_EVENT_LOOP_SIZE+++[] | int |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-all"></a> [`quarkus.datasource.reactive.trust-all`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-all) config_property_copy_button:+++quarkus.datasource.reactive.trust-all+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-all` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-all+++[]<br><br>Whether all server certificates should be trusted.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_ALL+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pem"></a> [`quarkus.datasource.reactive.trust-certificate-pem`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pem) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-pem+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-pem` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-pem+++[]<br><br>PEM Trust config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PEM+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pem-certs"></a> [`quarkus.datasource.reactive.trust-certificate-pem.certs`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pem-certs) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-pem.certs+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-pem.certs` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-pem.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PEM_CERTS+++[] | list of string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-jks"></a> [`quarkus.datasource.reactive.trust-certificate-jks`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-jks) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-jks+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-jks` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-jks+++[]<br><br>JKS config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_JKS+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-jks-path"></a> [`quarkus.datasource.reactive.trust-certificate-jks.path`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-jks-path) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-jks.path+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-jks.path` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-jks.path+++[]<br><br>Path of the key file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_JKS_PATH+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-jks-password"></a> [`quarkus.datasource.reactive.trust-certificate-jks.password`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-jks-password) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-jks.password+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-jks.password` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-jks.password+++[]<br><br>Password of the key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pfx"></a> [`quarkus.datasource.reactive.trust-certificate-pfx`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pfx) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-pfx+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-pfx` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-pfx+++[]<br><br>PFX config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PFX+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pfx-path"></a> [`quarkus.datasource.reactive.trust-certificate-pfx.path`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pfx-path) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-pfx.path+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-pfx.path` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-pfx.path+++[]<br><br>Path to the key file (PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PFX_PATH+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pfx-password"></a> [`quarkus.datasource.reactive.trust-certificate-pfx.password`](#quarkus-reactive-datasource_quarkus-datasource-reactive-trust-certificate-pfx-password) config_property_copy_button:+++quarkus.datasource.reactive.trust-certificate-pfx.password+++[]<br><br>`quarkus.datasource."datasource-name".reactive.trust-certificate-pfx.password` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.trust-certificate-pfx.password+++[]<br><br>Password of the key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_TRUST_CERTIFICATE_PFX_PASSWORD+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pem"></a> [`quarkus.datasource.reactive.key-certificate-pem`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pem) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-pem+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-pem` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-pem+++[]<br><br>PEM Key/cert config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_PEM+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pem-keys"></a> [`quarkus.datasource.reactive.key-certificate-pem.keys`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pem-keys) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-pem.keys+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-pem.keys` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-pem.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_PEM_KEYS+++[] | list of string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pem-certs"></a> [`quarkus.datasource.reactive.key-certificate-pem.certs`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pem-certs) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-pem.certs+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-pem.certs` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-pem.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_PEM_CERTS+++[] | list of string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-jks"></a> [`quarkus.datasource.reactive.key-certificate-jks`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-jks) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-jks+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-jks` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-jks+++[]<br><br>JKS config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_JKS+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-jks-path"></a> [`quarkus.datasource.reactive.key-certificate-jks.path`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-jks-path) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-jks.path+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-jks.path` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-jks.path+++[]<br><br>Path of the key file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_JKS_PATH+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-jks-password"></a> [`quarkus.datasource.reactive.key-certificate-jks.password`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-jks-password) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-jks.password+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-jks.password` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-jks.password+++[]<br><br>Password of the key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pfx"></a> [`quarkus.datasource.reactive.key-certificate-pfx`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pfx) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-pfx+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-pfx` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-pfx+++[]<br><br>PFX config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_PFX+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pfx-path"></a> [`quarkus.datasource.reactive.key-certificate-pfx.path`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pfx-path) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-pfx.path+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-pfx.path` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-pfx.path+++[]<br><br>Path to the key file (PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_PFX_PATH+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pfx-password"></a> [`quarkus.datasource.reactive.key-certificate-pfx.password`](#quarkus-reactive-datasource_quarkus-datasource-reactive-key-certificate-pfx-password) config_property_copy_button:+++quarkus.datasource.reactive.key-certificate-pfx.password+++[]<br><br>`quarkus.datasource."datasource-name".reactive.key-certificate-pfx.password` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.key-certificate-pfx.password+++[]<br><br>Password of the key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_KEY_CERTIFICATE_PFX_PASSWORD+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-reconnect-attempts"></a> [`quarkus.datasource.reactive.reconnect-attempts`](#quarkus-reactive-datasource_quarkus-datasource-reactive-reconnect-attempts) config_property_copy_button:+++quarkus.datasource.reactive.reconnect-attempts+++[]<br><br>`quarkus.datasource."datasource-name".reactive.reconnect-attempts` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.reconnect-attempts+++[]<br><br>The number of reconnection attempts when a pooled connection cannot be established on first try.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_RECONNECT_ATTEMPTS+++[] | int | `0` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-reconnect-interval"></a> [`quarkus.datasource.reactive.reconnect-interval`](#quarkus-reactive-datasource_quarkus-datasource-reactive-reconnect-interval) config_property_copy_button:+++quarkus.datasource.reactive.reconnect-interval+++[]<br><br>`quarkus.datasource."datasource-name".reactive.reconnect-interval` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.reconnect-interval+++[]<br><br>The interval between reconnection attempts when a pooled connection cannot be established on first try.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_RECONNECT_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-reactive-datasource_quarkus-datasource) | `1S` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-hostname-verification-algorithm"></a> [`quarkus.datasource.reactive.hostname-verification-algorithm`](#quarkus-reactive-datasource_quarkus-datasource-reactive-hostname-verification-algorithm) config_property_copy_button:+++quarkus.datasource.reactive.hostname-verification-algorithm+++[]<br><br>`quarkus.datasource."datasource-name".reactive.hostname-verification-algorithm` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.hostname-verification-algorithm+++[]<br><br>The hostname verification algorithm to use in case the server's identity should be checked. Should be `HTTPS`, `LDAPS` or `NONE`. `NONE` is the default value and disables the verification.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_HOSTNAME_VERIFICATION_ALGORITHM+++[] | string | `NONE` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-idle-timeout"></a> [`quarkus.datasource.reactive.idle-timeout`](#quarkus-reactive-datasource_quarkus-datasource-reactive-idle-timeout) config_property_copy_button:+++quarkus.datasource.reactive.idle-timeout+++[]<br><br>`quarkus.datasource."datasource-name".reactive.idle-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.idle-timeout+++[]<br><br>The maximum time a connection remains unused in the pool before it is closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-reactive-datasource_quarkus-datasource) | `no timeout` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-max-lifetime"></a> [`quarkus.datasource.reactive.max-lifetime`](#quarkus-reactive-datasource_quarkus-datasource-reactive-max-lifetime) config_property_copy_button:+++quarkus.datasource.reactive.max-lifetime+++[]<br><br>`quarkus.datasource."datasource-name".reactive.max-lifetime` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.max-lifetime+++[]<br><br>The maximum time a connection remains in the pool, after which it will be closed upon return and replaced as necessary.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MAX_LIFETIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-reactive-datasource_quarkus-datasource) | `no timeout` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-shared"></a> [`quarkus.datasource.reactive.shared`](#quarkus-reactive-datasource_quarkus-datasource-reactive-shared) config_property_copy_button:+++quarkus.datasource.reactive.shared+++[]<br><br>`quarkus.datasource."datasource-name".reactive.shared` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.shared+++[]<br><br>Set to true to share the pool among datasources. There can be multiple shared pools distinguished by name, when no specific name is set, the `__vertx.DEFAULT` name is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_SHARED+++[] | boolean | `false` |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-name"></a> [`quarkus.datasource.reactive.name`](#quarkus-reactive-datasource_quarkus-datasource-reactive-name) config_property_copy_button:+++quarkus.datasource.reactive.name+++[]<br><br>`quarkus.datasource."datasource-name".reactive.name` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.name+++[]<br><br>Set the pool name, used when the pool is shared among datasources, otherwise ignored.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_NAME+++[] | string |  |
| <a id="quarkus-reactive-datasource_quarkus-datasource-reactive-additional-properties-property-key"></a> [`quarkus.datasource.reactive.additional-properties."property-key"`](#quarkus-reactive-datasource_quarkus-datasource-reactive-additional-properties-property-key) config_property_copy_button:+++quarkus.datasource.reactive.additional-properties."property-key"+++[]<br><br>`quarkus.datasource."datasource-name".reactive.additional-properties."property-key"` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.additional-properties."property-key"+++[]<br><br>Other unspecified properties to be passed through the Reactive SQL Client directly to the database when new connections are initiated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_ADDITIONAL_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |

<a id="duration-note-anchor-quarkus-reactive-datasource_quarkus-datasource"></a>

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

### IBM Db2

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-db2-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-db2-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-db2-client_quarkus-datasource-reactive-db2-ssl"></a> [`quarkus.datasource.reactive.db2.ssl`](#quarkus-reactive-db2-client_quarkus-datasource-reactive-db2-ssl) config_property_copy_button:+++quarkus.datasource.reactive.db2.ssl+++[]<br><br>`quarkus.datasource."datasource-name".reactive.db2.ssl` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.db2.ssl+++[]<br><br>Whether SSL/TLS is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_DB2_SSL+++[] | boolean | `false` |

### MariaDB/MySQL

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-mysql-client_section_quarkus-datasource"></a> [Additional named datasources](#quarkus-reactive-mysql-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-charset"></a> [`quarkus.datasource.reactive.mysql.charset`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-charset) config_property_copy_button:+++quarkus.datasource.reactive.mysql.charset+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.charset` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.charset+++[]<br><br>Charset for connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_CHARSET+++[] | string |  |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-collation"></a> [`quarkus.datasource.reactive.mysql.collation`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-collation) config_property_copy_button:+++quarkus.datasource.reactive.mysql.collation+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.collation` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.collation+++[]<br><br>Collation for connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_COLLATION+++[] | string |  |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-ssl-mode"></a> [`quarkus.datasource.reactive.mysql.ssl-mode`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-ssl-mode) config_property_copy_button:+++quarkus.datasource.reactive.mysql.ssl-mode+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.ssl-mode` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.ssl-mode+++[]<br><br>Desired security state of the connection to the server.<br><br>See [MySQL Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/connection-options.html#option_general_ssl-mode).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_SSL_MODE+++[] | `disabled`, `preferred`, `required`, `verify-ca`, `verify-identity` | `disabled` |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-connection-timeout"></a> [`quarkus.datasource.reactive.mysql.connection-timeout`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-connection-timeout) config_property_copy_button:+++quarkus.datasource.reactive.mysql.connection-timeout+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.connection-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.connection-timeout+++[]<br><br>Connection timeout in seconds<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_CONNECTION_TIMEOUT+++[] | int |  |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-authentication-plugin"></a> [`quarkus.datasource.reactive.mysql.authentication-plugin`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-authentication-plugin) config_property_copy_button:+++quarkus.datasource.reactive.mysql.authentication-plugin+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.authentication-plugin` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.authentication-plugin+++[]<br><br>The authentication plugin the client should use. By default, it uses the plugin name specified by the server in the initial handshake packet.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_AUTHENTICATION_PLUGIN+++[] | `default`, `mysql-clear-password`, `mysql-native-password`, `sha256-password`, `caching-sha2-password` | `default` |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-pipelining-limit"></a> [`quarkus.datasource.reactive.mysql.pipelining-limit`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-pipelining-limit) config_property_copy_button:+++quarkus.datasource.reactive.mysql.pipelining-limit+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.pipelining-limit` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.pipelining-limit+++[]<br><br>The maximum number of inflight database commands that can be pipelined. By default, pipelining is disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_PIPELINING_LIMIT+++[] | int |  |
| <a id="quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-use-affected-rows"></a> [`quarkus.datasource.reactive.mysql.use-affected-rows`](#quarkus-reactive-mysql-client_quarkus-datasource-reactive-mysql-use-affected-rows) config_property_copy_button:+++quarkus.datasource.reactive.mysql.use-affected-rows+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mysql.use-affected-rows` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mysql.use-affected-rows+++[]<br><br>Whether to return the number of rows matched by the *WHERE* clause in *UPDATE* statements, instead of the number of rows actually changed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MYSQL_USE_AFFECTED_ROWS+++[] | boolean | `false` |

### Microsoft SQL Server

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-mssql-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-mssql-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-packet-size"></a> [`quarkus.datasource.reactive.mssql.packet-size`](#quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-packet-size) config_property_copy_button:+++quarkus.datasource.reactive.mssql.packet-size+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mssql.packet-size` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mssql.packet-size+++[]<br><br>The desired size (in bytes) for TDS packets.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MSSQL_PACKET_SIZE+++[] | int |  |
| <a id="quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-ssl"></a> [`quarkus.datasource.reactive.mssql.ssl`](#quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-ssl) config_property_copy_button:+++quarkus.datasource.reactive.mssql.ssl+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mssql.ssl` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mssql.ssl+++[]<br><br>Whether SSL/TLS is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MSSQL_SSL+++[] | boolean | `false` |

### Oracle

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-oracle-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-oracle-client_section_quarkus-datasource) | Type | Default |

### PostgreSQL

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-pg-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-pg-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-pipelining-limit"></a> [`quarkus.datasource.reactive.postgresql.pipelining-limit`](#quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-pipelining-limit) config_property_copy_button:+++quarkus.datasource.reactive.postgresql.pipelining-limit+++[]<br><br>`quarkus.datasource."datasource-name".reactive.postgresql.pipelining-limit` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.postgresql.pipelining-limit+++[]<br><br>The maximum number of inflight database commands that can be pipelined.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_POSTGRESQL_PIPELINING_LIMIT+++[] | int |  |
| <a id="quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-ssl-mode"></a> [`quarkus.datasource.reactive.postgresql.ssl-mode`](#quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-ssl-mode) config_property_copy_button:+++quarkus.datasource.reactive.postgresql.ssl-mode+++[]<br><br>`quarkus.datasource."datasource-name".reactive.postgresql.ssl-mode` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.postgresql.ssl-mode+++[]<br><br>SSL operating mode of the client.<br><br>See [Protection Provided in Different Modes](https://www.postgresql.org/docs/current/libpq-ssl.html#LIBPQ-SSL-PROTECTION).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_POSTGRESQL_SSL_MODE+++[] | `disable`, `allow`, `prefer`, `require`, `verify-ca`, `verify-full` | `disable` |
| <a id="quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-use-layer7-proxy"></a> [`quarkus.datasource.reactive.postgresql.use-layer7-proxy`](#quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-use-layer7-proxy) config_property_copy_button:+++quarkus.datasource.reactive.postgresql.use-layer7-proxy+++[]<br><br>`quarkus.datasource."datasource-name".reactive.postgresql.use-layer7-proxy` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.postgresql.use-layer7-proxy+++[]<br><br>Level 7 proxies can load balance queries on several connections to the actual database. When it happens, the client can be confused by the lack of session affinity and unwanted errors can happen like ERROR: unnamed prepared statement does not exist (26000). See [Using a level 7 proxy](https://vertx.io/docs/vertx-pg-client/java/#_using_a_level_7_proxy)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_POSTGRESQL_USE_LAYER7_PROXY+++[] | boolean | `false` |
