# Getting Started with Quarkus Data Hibernate

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Overview

Database access is something many applications need, whether it's persisting data or querying tables. In this guide we explain how Quarkus Data provides database access with entity mapping, compile-time validated repositories, and reactive database access.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 21+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it

## Introduction

You're working on a physical bookstore website, in which you want users to be able to browse books with titles. In this tutorial, we won't deal with the frontend and the user interface, we just want to connect to the database.

We'll start this tutorial by creating a new empty Quarkus application:

**CLI**

```bash
quarkus create app org.acme:quarkus-data-tutorial \
    --no-code
cd quarkus-data-tutorial
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:4.0.1:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=quarkus-data-tutorial \
    -DnoCode
cd quarkus-data-tutorial
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=quarkus-data-tutorial"`

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

The first dependency, `quarkus-data-hibernate`, is the Quarkus extension that provides everything you need for database access. The second dependency, `quarkus-jdbc-postgresql`, is the JDBC driver that tells Quarkus how to talk to a PostgreSQL database. If you're using a different database (MySQL, MariaDB, H2, etc.), pick the corresponding `quarkus-jdbc-*` driver instead (see [Configure a JDBC datasource](/guides/datasource/index.md#configure-a-jdbc-datasource)). Quarkus Data Hibernate cannot work without a JDBC driver [or a reactive SQL client](#reactive).

> [!WARNING]
> Quarkus Data Hibernate (Panache Next) and the classic Panache (Panache 1) extensions cannot be used together. Adding both `quarkus-data-hibernate` and any classic Panache extension (such as `quarkus-hibernate-orm-panache`, `quarkus-hibernate-orm-panache-kotlin`, `quarkus-hibernate-reactive-panache` or `quarkus-hibernate-reactive-panache-kotlin`) to the same application makes the build fail with an explicit error. These two data access layers are mutually exclusive, so pick only one of them.

Quarkus Data Hibernate uses the Quarkus Data annotation processor, a key component that reads your repository interfaces at compile time, validates queries, and generates implementation classes. These generated classes are compiled together with your code and wired up automatically by Quarkus. They are also simple to read, in case you want to understand how it works internally.

To enable it, add the annotation processor configuration to your build file:

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

Now run Quarkus continuous testing in a separate tab of your terminal:

**Maven**

```bash
./mvnw test
```

**Gradle**

```bash
./gradlew test
```

You don't need to restart Quarkus testing, it will re-run tests continuously after every change.

> [!TIP]
> Database connections are called "Data Sources" in Quarkus. Even if you don't configure a datasource, Quarkus will automatically start a database container for you via [Dev Services](/guides/dev-services/index.md). This means you can run `quarkus dev` and start coding without installing or configuring a database. See the [Datasource guide](/guides/datasource/index.md) for more details.

## Entities and repositories

We start by mapping our database table to a Java class and defining a repository for it. An *entity* is a Java class that maps to database tables.

Create a file called `Book.java` in the package `org.acme`:

```java
package org.acme;

import io.quarkus.data.hibernate.RecordEntity;
import io.quarkus.data.hibernate.RecordRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.persistence.Entity;

import java.util.List;

@Entity // <1>
public class Book extends RecordEntity { // <2>
    public String title; // <3>
    public String isbn;
    public BigDecimal currentPrice;

    public interface Repo extends RecordRepository<Book> { // <4>
        @Find
        List<Book> findByTitle(String title); // <5>

        @Query("where currentPrice < :maxPrice order by title")
        List<Book> cheaperThan(BigDecimal maxPrice); // <6>
    }
}
```

1. Tells Hibernate that this class maps to a database table. Hibernate will use the class name as the table name and the field names as column names.
2. `RecordEntity` gives the entity a generated `Long` id field and convenient methods that will be used later. You can use Hibernate's `@Id` and `@GeneratedValue` directly if you prefer, or if you need a custom ID strategy (see the [reference guide](/guides/quarkus-data-hibernate/index.md#about-entity-identifiers)).
3. These public fields map to columns in the `Book` table. Hibernate reads and writes these fields directly.
4. Defines a repository: a place to put all your query methods for `Book`. By nesting it inside the entity, the queries and the data they operate on live together. You can name this interface whatever you want: `Queries`, `Repo`, `BookQueries`, etc.
5. `@Find` tells Quarkus Data Hibernate to generate a query based on the method's parameter names. `findByTitle(String title)` becomes a query that matches the `title` field on `Book`. Parameters are inferred from the method signature.
6. `@Query` lets you write an [HQL](https://docs.hibernate.org/orm/8.0/querylanguage/html_single/#and-sqk) fragment when you need more control. The `select` and `from` parts are optional: they can be inferred from the return type `List<Book>`.

If you make a typo in a parameter name, the build fails. Try changing the parameter name to:

```java
        @Find
        List<Book> findByTitle(String tilte);
```

You should see a compilation error like:

```
error: no matching field named 'tilte' in entity class 'org.acme.Book'
```

> [!NOTE]
> The same compile-time checks apply to the query string passed to `@Query`. If you reference a field that doesn't exist or make a syntax error, the build fails. The query language is HQL (Hibernate Query Language); see the [HQL and SQL guide](https://docs.hibernate.org/orm/8.0/querylanguage/html_single/#and-sqk) for the full syntax.

Get the parameter back to the correct name so that we can proceed further.

## Creating new entities

Let's write a service to create a new book:

```java
@ApplicationScoped
public class LibraryService {

    @Transactional // <1>
    public void contributeNewBook(String title, String isbn, BigDecimal price) {
        Book book = new Book();
        book.title = title;
        book.isbn = isbn;
        book.currentPrice = price;
        book.insert(); // <2>
    }
}
```

1. Wraps the method in a database transaction. This is required to define the scope of your operations, so that related operations are committed or rolled back together.
2. `insert()` immediately executes an `INSERT` statement against the database. It comes from `RecordEntity` that we extended on the entity earlier, together with other lifecycle methods such as `update()` or `delete()`.

Let's write a test to verify that inserting a new book actually works. Create `src/test/java/org/acme/LibraryServiceTest.java`:

```java
package org.acme;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class LibraryServiceTest {

    @Inject
    LibraryService libraryService;

    @Inject
    Book.Repo bookRepo;

    @Test
    @TestTransaction // <1>
    void shouldInsertAndQueryBooks() {
        libraryService.contributeNewBook("Effective Java", "978-0134685991", BigDecimal.valueOf(45.00));
        libraryService.contributeNewBook("Clean Code", "978-0132350884", BigDecimal.valueOf(40.00));

        assertEquals(1, bookRepo.findByTitle("Effective Java").size());
        assertEquals(1, bookRepo.cheaperThan(BigDecimal.valueOf(42)).size());
        assertTrue(bookRepo.findByTitle("Unknown Book").isEmpty());
    }
}
```

1. `@TestTransaction` rolls back all database changes when the test method ends. Without it, the inserted book would stay in the database and accumulate on every re-run, causing assertions on counts or prices to fail. Use it on any test that writes data.

> [!NOTE]
> If you're an experienced Hibernate user, you'll find this part very different from what you're used to. When you call `insert()`, `update()`, or `delete()`, an operation is instantly triggered against the database, instead of being delayed until the next flush or commit. This is intentional: it's a simpler model that uses `StatelessSession` under the covers. In this mode, every change to an object requires an explicit save operation. If you're not interested in this style and want the full power of managed entities, skip ahead to [Managed session](#managed-session).

## Creating a Discount Service

In our bookstore we want to run a promotional sale: apply a discount to all books from a given publisher, but only if the publisher allows it. Some publishers offer discounts while others don't.

Let's create a `Publisher` entity with a one-to-many relationship to `Book`.

```java
package org.acme;

import io.quarkus.data.hibernate.RecordEntity;
import io.quarkus.data.hibernate.RecordRepository;
import jakarta.data.repository.Find;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class Publisher extends RecordEntity {
    public String name;
    public String country;
    public boolean allowsDiscounts;
    public boolean allowsTranslation;
    @OneToMany(mappedBy = Book_.PUBLISHER) // <1>
    public List<Book> books;

    public interface Repo extends RecordRepository<Publisher> {
        @Find
        List<Publisher> findByCountry(String country);
    }
}
```

1. A publisher has many books. `mappedBy` needs to point to the equivalent `@ManyToOne` association. You can either use the name of the field as a string or use a typesafe constant generated by the annotation processor.

And add a `publisher` field to `Book`. We'll also add a `listPrice` field to preserve the original price when discounts are applied:

```java
@Entity
public class Book extends RecordEntity {
    public String title;
    public String isbn;
    public BigDecimal listPrice;
    public BigDecimal currentPrice;
    @ManyToOne // <1>
    public Publisher publisher;

    public interface Repo extends RecordRepository<Book> {
        @Find
        List<Book> findByTitle(String title);

        @Find
        List<Book> findByPublisher(Publisher publisher);

        @Query("where currentPrice < :maxPrice order by title")
        List<Book> cheaperThan(BigDecimal maxPrice);
    }
}
```

1. Each book belongs to one publisher. This is the field referenced by `Book_.PUBLISHER` in `mappedBy` above.

Now let's implement our discount service:

```java
@ApplicationScoped
public class DiscountService {

    @Inject
    Publisher.Repo publisherRepo;

    @Inject
    Book.Repo bookRepo;

    @Transactional
    public void applyCountrySale(String country, BigDecimal discountPercent) {
        List<Publisher> publishers = publisherRepo.findByCountry(country);
        BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100)));
        Set<Book> modified = new HashSet<>(); // <1>
        for (Publisher publisher : publishers) {
            List<Book> books = bookRepo.findByPublisher(publisher); // <2>
            for (Book book : books) {
                if (publisher.allowsDiscounts) {
                    book.currentPrice = book.listPrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                    modified.add(book); // <3>
                }
                if (publisher.allowsTranslation && translateTitle(book)) {
                    modified.add(book);
                }
            }
        }
        for (Book book : modified) {
            book.update(); // <4>
        }
    }

    private boolean translateTitle(Book book) {
        // Translate the book title to the local language (translation fields omitted for brevity)
        return false;
    }
}
```

1. We collect the books we modify so we can update them all at the end.
2. In a stateless session, to-many associations are not fetched automatically. Accessing `publisher.books` directly would throw an exception. We load the books explicitly via a repository query. In Hibernate, these are called "lazy associations".
3. We add the book to our list of modified objects.
4. We update once at the end, after all modifications are done.

Notice how the persistence concerns, tracking which objects changed, loading associations explicitly, calling `update()`, are mixed in with the business logic. This method is becoming complex.

Let's write a test to verify it works:

```java
package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class DiscountServiceTest {

    @Inject
    DiscountService discountService;

    @Inject
    Book.Repo bookRepo;

    @BeforeEach
    @Transactional
    void setUp() {
        Publisher addisonWesley = new Publisher();
        addisonWesley.name = "Addison-Wesley";
        addisonWesley.country = "US";
        addisonWesley.allowsDiscounts = true;
        addisonWesley.insert();

        Publisher prenticeHall = new Publisher();
        prenticeHall.name = "Prentice Hall";
        prenticeHall.country = "US";
        prenticeHall.allowsDiscounts = false;
        prenticeHall.insert();

        Book effectiveJava = new Book();
        effectiveJava.title = "Effective Java";
        effectiveJava.isbn = "978-0134685991";
        effectiveJava.listPrice = new BigDecimal("45.00");
        effectiveJava.currentPrice = new BigDecimal("45.00");
        effectiveJava.publisher = addisonWesley;
        effectiveJava.insert();

        Book cleanCode = new Book();
        cleanCode.title = "Clean Code";
        cleanCode.isbn = "978-0132350884";
        cleanCode.listPrice = new BigDecimal("40.00");
        cleanCode.currentPrice = new BigDecimal("40.00");
        cleanCode.publisher = addisonWesley;
        cleanCode.insert();

        Book ddd = new Book();
        ddd.title = "Domain-Driven Design";
        ddd.isbn = "978-0321125217";
        ddd.listPrice = new BigDecimal("55.00");
        ddd.currentPrice = new BigDecimal("55.00");
        ddd.publisher = prenticeHall;
        ddd.insert();

        Book refactoring = new Book();
        refactoring.title = "Refactoring";
        refactoring.isbn = "978-0134757599";
        refactoring.listPrice = new BigDecimal("50.00");
        refactoring.currentPrice = new BigDecimal("50.00");
        refactoring.publisher = prenticeHall;
        refactoring.insert();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        bookRepo.deleteAll();
    }

    @Test
    @Transactional
    void shouldApplySaleOnlyToPublishersThatAllowDiscounts() {
        discountService.applyCountrySale("US", BigDecimal.valueOf(10));

        // Addison-Wesley (US, allows discounts): currentPrice reduced by 10%, listPrice unchanged
        Book ej = bookRepo.findByTitle("Effective Java").getFirst();
        assertEquals(new BigDecimal("45.00"), ej.listPrice);
        assertEquals(new BigDecimal("40.50"), ej.currentPrice);

        Book cc = bookRepo.findByTitle("Clean Code").getFirst();
        assertEquals(new BigDecimal("40.00"), cc.listPrice);
        assertEquals(new BigDecimal("36.00"), cc.currentPrice);

        // Prentice Hall (US, no discounts): prices unchanged
        assertEquals(new BigDecimal("55.00"), bookRepo.findByTitle("Domain-Driven Design").getFirst().currentPrice);
        assertEquals(new BigDecimal("50.00"), bookRepo.findByTitle("Refactoring").getFirst().currentPrice);
    }
}
```

<a id="managed-session"></a>
## Managed session

The previous example of a Discount Service works, but it can be simpler. Quarkus Data Hibernate supports a "managed persistence context" that tracks objects for you: Hibernate monitors every entity it loads, detects which fields changed, and generates the minimal SQL needed to keep entities synchronized with the database at commit time. You don't need to track dirty objects, and lazy associations are fetched automatically when you access them.

Switch `Book` and `Publisher` to use `ManagedEntity` and managed repositories:

```java
@Entity
public class Publisher extends ManagedEntity { // <1>
    public String name;
    public String country;
    public boolean allowsDiscounts;
    public boolean allowsTranslation;
    @OneToMany(mappedBy = Book_.PUBLISHER)
    public List<Book> books;

    public interface Repo extends ManagedRepository<Publisher> { // <2>
        @Find
        List<Publisher> findByCountry(String country);
    }
}
```

1. `ManagedEntity` replaces `RecordEntity`. This tells Hibernate to track this entity in its persistence context.
2. The repository uses `ManagedRepository` instead of `RecordRepository`.

```java
@Entity
public class Book extends ManagedEntity { // <1>
    public String title;
    public String isbn;
    public BigDecimal listPrice;
    public BigDecimal currentPrice;
    @ManyToOne
    public Publisher publisher;

    public interface Repo extends ManagedRepository<Book> { // <2>
        @Find
        List<Book> findByTitle(String title);

        @Query("where currentPrice < :maxPrice order by title")
        List<Book> cheaperThan(BigDecimal maxPrice);
    }
}
```

1. `ManagedEntity` replaces `RecordEntity`. This tells Hibernate to track this entity in its persistence context.
2. The repository uses `ManagedRepository` instead of `RecordRepository`.

Since the entities are now managed, `insert()` is no longer available. Use `persist()` instead: unlike `insert()`, it doesn't execute SQL immediately. Hibernate will flush the changes automatically, so you don't need to worry about when SQL is sent to the database. Any changes you make to the entity after calling `persist()` will also be sent to the database at flush time.

```java
@ApplicationScoped
public class LibraryService {

    @Transactional
    public void contributeNewBook(String title, String isbn, BigDecimal price) {
        Book book = new Book();
        book.title = title;
        book.isbn = isbn;
        book.listPrice = price;
        book.currentPrice = price;
        book.persist(); // <1>
    }
}
```

1. `insert()` was replaced with `persist()`

Now the service becomes pure business logic:

```java
@ApplicationScoped
public class DiscountService {

    @Inject
    Publisher.Repo publisherRepo;

    @Transactional
    public void applyCountrySale(String country, BigDecimal discountPercent) {
        BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100)));
        List<Publisher> publishers = publisherRepo.findByCountry(country);
        for (Publisher publisher : publishers) {
            for (Book book : publisher.books) { // <1>
                if (publisher.allowsDiscounts) {
                    book.currentPrice = book.listPrice.multiply(factor).setScale(2, RoundingMode.HALF_UP); // <2>
                }
                if (publisher.allowsTranslation) {
                    translateTitle(book);
                }
            }
        }
    } // <3>

    private void translateTitle(Book book) {
        // Translate the book title to the local language (translation fields omitted for brevity)
    }
}
```

1. Accessing `publisher.books` triggers a lazy fetch: Hibernate loads the books from the database automatically.
2. We just set the field. No need to call `update()`.
3. When the `@Transactional` method returns, Hibernate automatically generates the SQL needed to keep the database in sync with the objects in memory. Only the books that were actually modified get an `UPDATE` statement.

The test is exactly the same: the behavior hasn't changed, the implementation only got simpler.

It's up to you to decide whether to use a Managed Entity or a Record one. The Managed Repository is more expressive, but reasoning about it is more complex and might require knowing some details about how Hibernate handles objects. If you're an experienced Hibernate developer, you expect that changing an object's field doesn't immediately hit the database. Some users prefer a more explicit way to control the database. Either way is fine, and Quarkus Data Hibernate makes it easy to use either.

<a id="reactive"></a>
## Reactive

If you're already using reactive APIs in your application (Quarkus Messaging, reactive REST clients, Vert.x), you want your database access to compose naturally with them without blocking the event loop. Quarkus Data Hibernate lets you pick blocking or reactive to match your use case. The same entity model works unchanged with a reactive driver.

First, add the reactive dependencies:

**CLI**

```bash
quarkus extension add quarkus-hibernate-reactive,quarkus-hibernate-reactive-panache-common,quarkus-reactive-pg-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='quarkus-hibernate-reactive,quarkus-hibernate-reactive-panache-common,quarkus-reactive-pg-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='quarkus-hibernate-reactive,quarkus-hibernate-reactive-panache-common,quarkus-reactive-pg-client'
```

> [!NOTE]
> `quarkus-hibernate-reactive-panache-common` is needed because `quarkus-hibernate-reactive` does not yet transitively include the classes required by the annotation processor to generate reactive repository implementations (see [#53871](https://github.com/quarkusio/quarkus/issues/53871)).

`quarkus-hibernate-reactive` adds the reactive Hibernate engine, which uses non-blocking I/O under the covers instead of JDBC. `quarkus-reactive-pg-client` is the reactive equivalent to `quarkus-jdbc-postgresql`. It provides a non-blocking PostgreSQL driver built on Vert.x. As its blocking counterpart, it's mandatory for Quarkus Data Hibernate to work.

Now let's change only the minimal part of the code you need to try reactive. Let's add a second nested repository for reactive operations:

```java
@Entity
public class Book extends ManagedEntity {
    public String title;
    public String isbn;
    public BigDecimal listPrice;
    public BigDecimal currentPrice;
    @ManyToOne
    public Publisher publisher;

    public interface Repo extends ManagedRepository<Book> { // <1>
        @Find
        List<Book> findByTitle(String title);

        @Query("where currentPrice < :maxPrice order by title")
        List<Book> cheaperThan(BigDecimal maxPrice);
    }

    public interface ReactiveRepo extends ManagedRepository.Reactive<Book> { // <2>
        @Find
        Uni<List<Book>> findByTitle(String title); // <3>
    }
}
```

1. The blocking repository stays unchanged.
2. A new reactive repository for the same entity. Both repositories can coexist.
3. The return type is `Uni<T>` instead of `T`. `Uni` is a Mutiny reactive type that represents a value that will be available in the future. Instead of blocking the thread, the method returns immediately and emits the result when the query completes.

To test the reactive repository, you need `quarkus-test-vertx` which provides `@RunOnVertxContext` and `UniAsserter` for testing reactive code. Add it to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-vertx</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-vertx")
```

Now create `src/test/java/org/acme/ReactiveBookRepositoryTest.java`:

```java
package org.acme;

import io.quarkus.test.TestReactiveTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ReactiveBookRepositoryTest {

    @Inject
    Book.ReactiveRepo reactiveBookRepo;

    @Test
    @RunOnVertxContext // <1>
    @TestReactiveTransaction // <2>
    void shouldPersistAndFindReactively(UniAsserter asserter) {
        Book book = new Book();
        book.title = "Effective Java";
        book.isbn = "978-0134685991";
        book.listPrice = BigDecimal.valueOf(45.00);
        book.currentPrice = BigDecimal.valueOf(45.00);

        asserter.execute(() -> reactiveBookRepo.persist(book)); // <3>
        asserter.assertThat( // <4>
            () -> reactiveBookRepo.findByTitle("Effective Java"),
            books -> {
                assertEquals(1, books.size());
                assertEquals("Effective Java", books.getFirst().title);
            }
        );
    }
}
```

1. `@RunOnVertxContext` ensures the test runs on the Vert.x event loop, which is required for reactive database access.
2. `@TestReactiveTransaction` opens a reactive session and transaction for the test, and rolls back at the end.
3. `UniAsserter.execute()` is a convenient method to execute and wait for the reactive method to complete.
4. `UniAsserter.assertThat()` lets you test reactive code by providing an assertion as the second parameter.

For the full details on reactive sessions, stateless reactive, and combining blocking and reactive code, see the [reference guide](/guides/quarkus-data-hibernate/index.md#lets-get-reactive).

## What you have built

In this guide you went from an empty project to a working database application:

- You introduced entities and repositories with compile-time validation via the Quarkus Data annotation processor.
- You used `RecordEntity` for explicit insert/update/delete operations and saw how persistence logic gets mixed in with business logic.
- You switched to `ManagedEntity` for automatic dirty tracking and lazy loading, letting you write pure business logic while Hibernate handles the persistence.
- You added a reactive repository that returns `Uni` instead of blocking, using the same entity model.

## Going further

Everything you've seen in this guide is built on top of [Jakarta Data](https://jakarta.ee/specifications/data/) and [Jakarta Persistence](https://jakarta.ee/specifications/persistence/). You can use these raw components directly for more advanced scenarios: for instance, you could define repositories as standalone interfaces outside of entities, and the annotation processor will still generate the query implementations for you. You can also inject the `Session` or `StatelessSession` at any time and do things manually, as you would with the Hibernate ORM extension directly. The [reference guide](/guides/quarkus-data-hibernate/index.md) covers the full API surface.
