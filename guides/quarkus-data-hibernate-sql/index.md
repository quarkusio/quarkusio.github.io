# Native SQL queries with Quarkus Data

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

Sometimes you need full control over the SQL sent to the database, or need to use database-specific SQL syntax that is not portable across databases. This guide shows you how to write native SQL queries in Quarkus using `@NativeQuery` annotations and Java records, with compile-time generated implementations. The examples work directly with query results and do not require entity classes.

You can also use native SQL with mapped entities. See the [Quarkus Data Hibernate reference guide](/guides/quarkus-data-hibernate/index.md#repositories) for repository queries with entities, or the [Getting Started with Quarkus Data Hibernate](/guides/quarkus-data-hibernate-getting-started/index.md) guide to learn about entity mapping.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 21+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it

## Introduction

You're working on a physical bookstore website, in which you want users to be able to browse books with titles. In this tutorial, we won't deal with the frontend and the user interface, we just want to connect to the database.

We'll start this tutorial by creating a new empty Quarkus application:

**CLI**

```bash
quarkus create app org.acme:quarkus-data-sql-tutorial \
    --no-code
cd quarkus-data-sql-tutorial
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:4.0.1:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=quarkus-data-sql-tutorial \
    -DnoCode
cd quarkus-data-sql-tutorial
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=quarkus-data-sql-tutorial"`

Then let's import the extensions:

**CLI**

```bash
quarkus extension add quarkus-data-hibernate,quarkus-jdbc-postgresql
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-data-hibernate,quarkus-jdbc-postgresql'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-data-hibernate,quarkus-jdbc-postgresql'
```

The first dependency, `quarkus-data-hibernate`, provides Quarkus Data Hibernate's database access APIs and integration with Quarkus. The second dependency, `quarkus-jdbc-postgresql`, is the JDBC driver that tells Quarkus how to talk to a PostgreSQL database. This tutorial uses blocking database access, which requires a JDBC driver. For other databases, choose the corresponding `quarkus-jdbc-*` driver (see [Configure a JDBC datasource](/guides/datasource/index.md#configure-a-jdbc-datasource)) and adapt the SQL initialization script below to that database's syntax.

## Configuration

Quarkus Data Hibernate uses the Quarkus Data annotation processor, which reads your repository interfaces at compile time and generates implementation classes. These generated classes are compiled together with your code and wired up automatically by Quarkus. They are also simple to read, in case you want to understand how it works internally.

To enable it, add the annotation processor configuration to your build file. For Maven, merge the configuration into the existing `maven-compiler-plugin` entry:

**pom.xml**

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPathsUseDepMgmt>true</annotationProcessorPathsUseDepMgmt>
        <annotationProcessorPaths>
            <path>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-data-processor</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**build.gradle**

```gradle
annotationProcessor enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
annotationProcessor 'io.quarkus:quarkus-data-processor'
```

Now run [Quarkus continuous testing](/guides/continuous-testing/index.md) from the project directory in a separate tab of your terminal:

```bash
quarkus test
```

If you are not using the Quarkus CLI, run `./mvnw quarkus:test` with Maven or `./gradlew quarkusTest` with Gradle.

You don't need to restart Quarkus testing, it will re-run tests continuously after every change.

> [!TIP]
> A *datasource* provides connections to your database. In dev and test modes, if you don't configure a database connection, Quarkus can automatically start a PostgreSQL container for you via [Dev Services](/guides/dev-services/index.md). See the [Datasource guide](/guides/datasource/index.md) for more details.

## Database initialization

Since we're writing raw SQL without entity mapping, we'll create the schema and sample data ourselves. Create a `schema.sql` file in `src/main/resources/`. Hibernate ORM automatically runs this script during schema initialization in dev and test modes.

```sql
CREATE SEQUENCE IF NOT EXISTS Publisher_SEQ;

CREATE TABLE IF NOT EXISTS Publisher (
    id BIGINT PRIMARY KEY DEFAULT nextval('Publisher_SEQ'),
    name VARCHAR(255),
    country VARCHAR(10),
    allowsDiscounts BOOLEAN
);

CREATE SEQUENCE IF NOT EXISTS Book_SEQ;

CREATE TABLE IF NOT EXISTS Book (
    id BIGINT PRIMARY KEY DEFAULT nextval('Book_SEQ'),
    title VARCHAR(255),
    isbn VARCHAR(20),
    price NUMERIC(10,2),
    publisher_id BIGINT REFERENCES Publisher(id)
);
```

This SQL script creates sequences for ID generation and the `Publisher` and `Book` tables.

Next, create a `data.sql` file in `src/main/resources/` to load the sample data. Hibernate ORM automatically runs this script after schema initialization in dev and test modes.

```sql
DELETE FROM Book;
DELETE FROM Publisher;

INSERT INTO Publisher (id, name, country, allowsDiscounts) VALUES (1, 'Addison-Wesley', 'US', true);
INSERT INTO Publisher (id, name, country, allowsDiscounts) VALUES (2, 'Prentice Hall', 'US', false);
INSERT INTO Publisher (id, name, country, allowsDiscounts) VALUES (3, 'O''Reilly', 'UK', true);
INSERT INTO Publisher (id, name, country, allowsDiscounts) VALUES (4, 'Manning', 'UK', false);

INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (1, 'Effective Java', '978-0134685991', 45.00, 1);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (2, 'Clean Code', '978-0132350884', 40.00, 1);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (3, 'Domain-Driven Design', '978-0321125217', 55.00, 2);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (4, 'Refactoring', '978-0134757599', 50.00, 2);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (5, 'The Pragmatic Programmer', '978-0135957059', 42.00, 3);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (6, 'Head First Design Patterns', '978-0596007126', 38.00, 3);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (7, 'Java Concurrency in Practice', '978-0321349606', 48.00, 4);
INSERT INTO Book (id, title, isbn, price, publisher_id) VALUES (8, 'Kotlin in Action', '978-1617293290', 44.00, 4);

-- Set each sequence to the largest imported ID so the next generated ID does not collide.
SELECT setval('Publisher_SEQ', (SELECT MAX(id) FROM Publisher));
SELECT setval('Book_SEQ', (SELECT MAX(id) FROM Book));
```

This SQL script inserts sample data. Each book is linked to a publisher. The final statements set each sequence to the largest imported ID, so the next generated ID follows the sample data even if you add more rows to the script.

See [SQL init scripts](/guides/hibernate-orm/index.md#sql-init-scripts) for more details about schema and data initialization.

## Querying data

Create a file called `Library.java` in the package `org.acme` to query books by title:

```java
package org.acme;

import jakarta.persistence.query.NativeQuery;

import java.math.BigDecimal;
import java.util.List;

public interface Library { // <1>

    record Book(Long id, String title, String isbn, BigDecimal price) {} // <2>

    @NativeQuery("SELECT id, title, isbn, price FROM Book WHERE title = :title") // <3>
    List<Book> findByTitle(String title); // <4>
}
```

1. A repository interface that groups all your query methods for books. The annotation processor generates the implementation for you.
2. A Java record that maps to the columns returned by the SQL query. The selected columns match the record components in name and type.
3. `@NativeQuery` sends raw SQL to the database. The `:title` syntax is a named parameter, it avoids SQL injection attacks.
4. The method signature defines the parameters.

> [!WARNING]
> Native SQL syntax and database column names are checked by the database when a statement executes, rather than by the annotation processor at build time. Make sure every query is covered by a test, or you might miss typos.

We can now create a test in `src/test/java/org/acme/LibraryTest.java` that will check everything is working correctly:

```java
package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest // <1>
public class LibraryTest {

    @Inject
    Library library; // <2>

    @Test
    void shouldFindBookByTitle() {
        List<Library.Book> books = library.findByTitle("Effective Java"); // <3>
        assertEquals(1, books.size());
        assertEquals("Effective Java", books.get(0).title());
    }

    @Test
    void shouldReturnEmptyListForUnknownTitle() {
        List<Library.Book> books = library.findByTitle("Unknown Book");
        assertTrue(books.isEmpty());
    }
}
```

1. `@QuarkusTest` starts the full Quarkus application for the test, including the database and Dev Services. The data from `data.sql` is loaded automatically.
2. We inject our `Library` just like we would in application code.
3. We query for a book we know exists in `data.sql` and verify it's returned.

If you have continuous testing running in another terminal, these tests will run automatically as soon as you save the file.

## Updating data

Native SQL can also modify rows without mapped entities. Add an `updatePrice` method to `Library`:

```java
    @NativeQuery("UPDATE Book SET price = :price WHERE isbn = :isbn")
    int updatePrice(String isbn, BigDecimal price);
```

The method returns the number of updated rows. Both arguments are bound as query parameters, just as in the title query.

Write operations require a transaction. In application code, invoke the repository from a CDI bean method annotated with `jakarta.transaction.Transactional`; see [Using transactions in Quarkus](/guides/transaction/index.md).

For the test, use `@TestTransaction` to run the update and verification in a transaction that is rolled back after the test. This keeps the sample data unchanged for other tests and subsequent continuous-testing runs. Add this import to `LibraryTest`:

```java
import io.quarkus.test.TestTransaction;
```

Then add the following test method:

```java
    @Test
    @TestTransaction
    void shouldUpdateBookPrice() {
        BigDecimal newPrice = new BigDecimal("39.00");
        int updated = library.updatePrice("978-0134685991", newPrice);
        assertEquals(1, updated);

        List<Library.Book> books = library.findByTitle("Effective Java");
        assertEquals(1, books.size());
        assertEquals(newPrice, books.get(0).price());
    }
```

The tests now cover reading rows, handling an empty result, and updating a row using native SQL.
