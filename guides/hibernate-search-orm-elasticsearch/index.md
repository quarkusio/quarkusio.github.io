# Use Hibernate Search with Hibernate ORM and Elasticsearch/OpenSearch

You have a Hibernate ORM-based application? You want to provide a full-featured full-text search to your users? You're at the right place.

With this guide, you'll learn how to synchronize your entities to an Elasticsearch or OpenSearch cluster in a heartbeat with Hibernate Search. We will also explore how you can query your Elasticsearch or OpenSearch cluster using the Hibernate Search API.

> [!IMPORTANT]
> If you want to index entities that are not Hibernate ORM entities, see [this dedicated guide](/guides/hibernate-search-standalone-elasticsearch/index.md) instead.

## Prerequisites

To complete this guide, you need:

- Roughly 20 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

The application described in this guide allows to manage a (simple) library: you manage authors and their books.

The entities are stored in a PostgreSQL database and indexed in an Elasticsearch cluster.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `hibernate-search-orm-elasticsearch-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-search-orm-elasticsearch-quickstart).

> [!NOTE]
> The provided solution contains a few additional elements such as tests and testing infrastructure.

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:hibernate-search-orm-elasticsearch-quickstart \
    --extension='hibernate-orm-panache,jdbc-postgresql,hibernate-search-orm-elasticsearch,rest-jackson' \
    --no-code
cd hibernate-search-orm-elasticsearch-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=hibernate-search-orm-elasticsearch-quickstart \
    -Dextensions='hibernate-orm-panache,jdbc-postgresql,hibernate-search-orm-elasticsearch,rest-jackson' \
    -DnoCode
cd hibernate-search-orm-elasticsearch-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=hibernate-search-orm-elasticsearch-quickstart"`

This command generates a Maven structure importing the following extensions:

\* Hibernate ORM with Panache, \* the PostgreSQL JDBC driver, \* Hibernate Search + Elasticsearch, \* Quarkus REST (formerly RESTEasy Reactive) and Jackson.

If you already have your Quarkus project configured, you can add the `hibernate-search-orm-elasticsearch` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add hibernate-search-orm-elasticsearch
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='hibernate-search-orm-elasticsearch'
```

**Gradle**

```bash
./gradlew addExtension --extensions='hibernate-search-orm-elasticsearch'
```

This will add the following to your `pom.xml`:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-search-orm-elasticsearch</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-hibernate-search-orm-elasticsearch")
```

## Creating the bare entities

First, let's create our Hibernate ORM entities `Book` and `Author` in the `model` subpackage.

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Author extends PanacheEntity { // <1>

    public String firstName;

    public String lastName;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER) // <2>
    public List<Book> books;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Author)) {
            return false;
        }

        Author other = (Author) o;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
```

1. We are using Hibernate ORM with Panache, it is not mandatory.
2. We are loading these elements eagerly so that they are present in the JSON output. In a real world application, you should probably use a DTO approach.

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Book extends PanacheEntity {

    public String title;

    @ManyToOne
    @JsonIgnore <1>
    public Author author;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book)) {
            return false;
        }

        Book other = (Book) o;

        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
```

1. We mark this property with `@JsonIgnore` to avoid infinite loops when serializing with Jackson.

## Initializing the REST service

While everything is not yet set up for our REST service, we can initialize it with the standard CRUD operations we will need.

Create the `org.acme.hibernate.search.elasticsearch.LibraryResource` class:

```java
package org.acme.hibernate.search.elasticsearch;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.acme.hibernate.search.elasticsearch.model.Author;
import org.acme.hibernate.search.elasticsearch.model.Book;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.runtime.StartupEvent;

@Path("/library")
public class LibraryResource {

    @PUT
    @Path("book")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void addBook(@RestForm String title, @RestForm Long authorId) {
        Author author = Author.findById(authorId);
        if (author == null) {
            return;
        }

        Book book = new Book();
        book.title = title;
        book.author = author;
        book.persist();

        author.books.add(book);
        author.persist();
    }

    @DELETE
    @Path("book/{id}")
    @Transactional
    public void deleteBook(Long id) {
        Book book = Book.findById(id);
        if (book != null) {
            book.author.books.remove(book);
            book.delete();
        }
    }

    @PUT
    @Path("author")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void addAuthor(@RestForm String firstName, @RestForm String lastName) {
        Author author = new Author();
        author.firstName = firstName;
        author.lastName = lastName;
        author.persist();
    }

    @POST
    @Path("author/{id}")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updateAuthor(Long id, @RestForm String firstName, @RestForm String lastName) {
        Author author = Author.findById(id);
        if (author == null) {
            return;
        }
        author.firstName = firstName;
        author.lastName = lastName;
        author.persist();
    }

    @DELETE
    @Path("author/{id}")
    @Transactional
    public void deleteAuthor(Long id) {
        Author author = Author.findById(id);
        if (author != null) {
            author.delete();
        }
    }
}
```

Nothing out of the ordinary here: it is just good old Hibernate ORM with Panache operations in a REST service.

In fact, the interesting part is that we will need to add very few elements to make our full text search application working.

## Using Hibernate Search annotations

Let's go back to our entities.

Enabling full text search capabilities for them is as simple as adding a few annotations.

Let's edit the `Book` entity again to include this content:

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Indexed // <1>
public class Book extends PanacheEntity {

    @FullTextField(analyzer = "english") // <2>
    public String title;

    @ManyToOne
    @JsonIgnore
    public Author author;

    // Preexisting equals()/hashCode() methods
}
```

1. First, let's use the `@Indexed` annotation to register our `Book` entity as part of the full text index.
2. The `@FullTextField` annotation declares a field in the index specifically tailored for full text search. In particular, we have to define an analyzer to split and analyze the tokens (\~ words) - more on this later.

Now that our books are indexed, we can do the same for the authors.

Open the `Author` class and include the content below.

Things are quite similar here: we use the `@Indexed`, `@FullTextField` and `@KeywordField` annotations.

There are a few differences/additions though. Let's check them out.

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
@Indexed
public class Author extends PanacheEntity {

    @FullTextField(analyzer = "name") // <1>
    @KeywordField(name = "firstName_sort", sortable = Sortable.YES, normalizer = "sort") // <2>
    public String firstName;

    @FullTextField(analyzer = "name")
    @KeywordField(name = "lastName_sort", sortable = Sortable.YES, normalizer = "sort")
    public String lastName;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @IndexedEmbedded // <3>
    public List<Book> books;

    // Preexisting equals()/hashCode() methods
}
```

1. We use a `@FullTextField` similar to what we did for `Book` but you'll notice that the analyzer is different - more on this later.
2. As you can see, we can define several fields for the same property. Here, we define a `@KeywordField` with a specific name. The main difference is that a keyword field is not tokenized (the string is kept as one single token) but can be normalized (i.e. filtered) - more on this later. This field is marked as sortable as our intention is to use it for sorting our authors.
3. The purpose of `@IndexedEmbedded` is to include the `Book` fields into the `Author` index. In this case, we just use the default configuration: all the fields of the associated `Book` entities are included in the index (i.e. the `title` field). The nice thing with `@IndexedEmbedded` is that it is able to automatically reindex an `Author` if one of its ``Book``s has been updated thanks to the bidirectional relation. `@IndexedEmbedded` also supports nested documents (using the `structure = NESTED` attribute), but we don't need it here. You can also specify the fields you want to embed in your parent index using the `includePaths`/`excludePaths` attributes if you don't want them all.

## Analyzers and normalizers

### Introduction

Analysis is a big part of full text search: it defines how text will be processed when indexing or building search queries.

The role of analyzers is to split the text into tokens (\~ words) and filter them (making it all lowercase and removing accents for instance).

Normalizers are a special type of analyzers that keeps the input as a single token. It is especially useful for sorting or indexing keywords.

There are a lot of bundled analyzers, but you can also develop your own for your own specific purposes.

You can learn more about the Elasticsearch analysis framework in the [Analysis section of the Elasticsearch documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis.html).

### Defining the analyzers used

When we added the Hibernate Search annotations to our entities, we defined the analyzers and normalizers used. Typically:

```java
@FullTextField(analyzer = "english")
```

```java
@FullTextField(analyzer = "name")
```

```java
@KeywordField(name = "lastName_sort", sortable = Sortable.YES, normalizer = "sort")
```

We use:

\* an analyzer called `name` for person names, \* an analyzer called `english` for book titles, \* a normalizer called `sort` for our sort fields

but we haven't set them up yet.

Let's see how you can do it with Hibernate Search.

<a id="analysis-configurer"></a>
### Setting up the analyzers

It is an easy task, we just need to create an implementation of `ElasticsearchAnalysisConfigurer` (and configure Quarkus to use it, more on that later).

To fulfill our requirements, let's create the following implementation:

```java
package org.acme.hibernate.search.elasticsearch.config;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

import io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension;

@SearchExtension // <1>
public class AnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        context.analyzer("name").custom() // <2>
                .tokenizer("standard")
                .tokenFilters("asciifolding", "lowercase");

        context.analyzer("english").custom() // <3>
                .tokenizer("standard")
                .tokenFilters("asciifolding", "lowercase", "porter_stem");

        context.normalizer("sort").custom() // <4>
                .tokenFilters("asciifolding", "lowercase");
    }
}
```

1. Annotate the configurer implementation with the `@SearchExtension` qualifier to tell Quarkus it should be used in the default persistence unit, for all Elasticsearch indexes (by default).

   The annotation can also target a specific persistence unit (`@SearchExtension(persistenceUnit = "nameOfYourPU")`), backend (`@SearchExtension(backend = "nameOfYourBackend")`), index (`@SearchExtension(index = "nameOfYourIndex")`), or a combination of those (`@SearchExtension(persistenceUnit = "nameOfYourPU", backend = "nameOfYourBackend", index = "nameOfYourIndex")`).

2. This is a simple analyzer separating the words on spaces, removing any non-ASCII characters by its ASCII counterpart (and thus removing accents) and putting everything in lowercase. It is used in our examples for the author's names.
3. We are a bit more aggressive with this one and we include some stemming: we will be able to search for `mystery` and get a result even if the indexed input contains `mysteries`. It is definitely too aggressive for person names, but it is perfect for the book titles.
4. Here is the normalizer used for sorting. Very similar to our first analyzer, except we don't tokenize the words as we want one and only one token.

> [!TIP]
> Alternatively, if for some reason you can't or don't want to annotate your analysis configurer with `@SearchExtension`, you can simply annotate it with `@Dependent @Named("myAnalysisConfigurer")` and then reference it from configuration properties:
>
> ```properties
> quarkus.hibernate-search-orm.elasticsearch.analysis.configurer=bean:myAnalysisConfigurer
> ```

For more information about configuring analyzers, see [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-analysis).

## Adding full text capabilities to our REST service

In our existing `LibraryResource`, we just need to inject the `SearchSession`:

```java
    @Inject
    SearchSession searchSession; // <1>
```

1. Inject a Hibernate Search session, which relies on the `EntityManager` under the hood. Applications with multiple persistence units can use the CDI qualifier `@io.quarkus.hibernate.orm.PersistenceUnit` to select the right one: see [CDI integration](#multiple-persistence-units-attaching-cdi).

And then the magic begins. When we added annotations to our entities, we made them available for full text search; we can now query the index using the Hibernate Search DSL, simply by adding the following method (and a few ``import``s):

```java
    @GET
    @Path("author/search")
    @Transactional // <1>
    public List<Author> searchAuthors(@RestQuery String pattern, // <2>
            @RestQuery Optional<Integer> size) {
        return searchSession.search(Author.class) // <3>
                .where(f ->
                    pattern == null || pattern.trim().isEmpty() ?
                        f.matchAll() : // <4>
                        f.simpleQueryString()
                                .fields("firstName", "lastName", "books.title").matching(pattern) // <5>
                )
                .sort(f -> f.field("lastName_sort").then().field("firstName_sort")) // <6>
                .fetchHits(size.orElse(20)); // <7>
    }
```

1. Important point: we need a transactional context for this method.
2. Use the `org.jboss.resteasy.reactive.RestQuery` annotation type to avoid repeating the parameter name.
3. We indicate that we are searching for ``Author``s.
4. We create a predicate: if the pattern is empty, we use a `matchAll()` predicate.
5. If we have a valid pattern, we create a [`simpleQueryString()`](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html) predicate on the `firstName`, `lastName` and `books.title` fields matching our pattern.
6. We define the sort order of our results. Here we sort by last name, then by first name. Note that we use the specific fields we created for sorting.
7. Fetch the `size` top hits, `20` by default. Obviously, paging is also supported.

> [!NOTE]
> The Hibernate Search DSL supports a significant subset of the Elasticsearch predicates (match, range, nested, phrase, spatial...). Feel free to explore the DSL using autocompletion.
>
> When that's not enough, you can always fall back to [defining a predicate using JSON directly](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#search-dsl-predicate-extensions-elasticsearch-from-json).

<a id="automatic-import-script"></a>
## Automatic data initialization

For the purpose of this demonstration, let's import an initial dataset.

Let's create a `src/main/resources/import.sql` file with the following content (we'll reference it in configuration later):

```sql
INSERT INTO author(id, firstname, lastname) VALUES (1, 'John', 'Irving');
INSERT INTO author(id, firstname, lastname) VALUES (2, 'Paul', 'Auster');
ALTER SEQUENCE author_seq RESTART WITH 3;

INSERT INTO book(id, title, author_id) VALUES (1, 'The World According to Garp', 1);
INSERT INTO book(id, title, author_id) VALUES (2, 'The Hotel New Hampshire', 1);
INSERT INTO book(id, title, author_id) VALUES (3, 'The Cider House Rules', 1);
INSERT INTO book(id, title, author_id) VALUES (4, 'A Prayer for Owen Meany', 1);
INSERT INTO book(id, title, author_id) VALUES (5, 'Last Night in Twisted River', 1);
INSERT INTO book(id, title, author_id) VALUES (6, 'In One Person', 1);
INSERT INTO book(id, title, author_id) VALUES (7, 'Avenue of Mysteries', 1);
INSERT INTO book(id, title, author_id) VALUES (8, 'The New York Trilogy', 2);
INSERT INTO book(id, title, author_id) VALUES (9, 'Mr. Vertigo', 2);
INSERT INTO book(id, title, author_id) VALUES (10, 'The Brooklyn Follies', 2);
INSERT INTO book(id, title, author_id) VALUES (11, 'Invisible', 2);
INSERT INTO book(id, title, author_id) VALUES (12, 'Sunset Park', 2);
INSERT INTO book(id, title, author_id) VALUES (13, '4 3 2 1', 2);
ALTER SEQUENCE book_seq RESTART WITH 14;
```

Because this data above will be inserted into the database without Hibernate Search's knowledge, it won't be indexed -- unlike upcoming updates coming through Hibernate ORM operations, which will be synchronized automatically to the full text index.

In our existing `LibraryResource`, let's add the following content (and a few ``import``s) to index that initial data:

> [!NOTE]
> If you don't import data manually in the database, you don't need this: the mass indexer should then only be used when you change your indexing configuration (adding a new field, changing an analyzer's configuration...) and you want the new configuration to be applied to your existing data.

```java
    @Inject
    SearchMapping searchMapping; // <1>

    void onStart(@Observes StartupEvent ev) throws InterruptedException { // <2>
        // only reindex if we imported some content
        if (Book.count() > 0) {
            searchMapping.scope(Object.class) // <3>
                    .massIndexer() // <4>
                    .startAndWait(); // <5>
        }
    }
```

1. Inject a Hibernate Search `SearchMapping`, which relies on the `EntityManagerFactory` under the hood. Applications with multiple persistence units can use the CDI qualifier `@io.quarkus.hibernate.orm.PersistenceUnit` to select the right one: see [CDI integration](#multiple-persistence-units-attaching-cdi).
2. Add a method that will get executed on application startup.
3. Create a "search scope" targeting all indexed entity types that extend `Object` -- that is, every single indexed entity types (`Author` and `Book`).
4. Create an instance of Hibernate Search's mass indexer, which allows indexing a lot of data efficiently (you can fine tune it for better performance).
5. Start the mass indexer and wait for it to finish.

## Configuring the application

As usual, we can configure everything in the Quarkus configuration file, `application.properties`.

Edit `src/main/resources/application.properties` and inject the following configuration:

```properties
quarkus.datasource.db-kind=postgresql <1>

quarkus.hibernate-orm.sql-load-script=import.sql <2>

quarkus.hibernate-search-orm.elasticsearch.version=9 <3>
quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy=sync <4>

%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost/quarkus_test
%prod.quarkus.datasource.username=quarkus_test
%prod.quarkus.datasource.password=quarkus_test
%prod.quarkus.hibernate-orm.schema-management.strategy=create
%prod.quarkus.hibernate-search-orm.elasticsearch.hosts=localhost:9200 <5>
```

1. Let's create a PostgreSQL datasource.
2. We load some initial data on startup (see [Automatic data initialization](#automatic-import-script)).
3. We need to tell Hibernate Search about the version of Elasticsearch we will use. It is important because there are significant differences between Elasticsearch mapping syntax depending on the version. Since the mapping is created at build time to reduce startup time, Hibernate Search cannot connect to the cluster to automatically detect the version. Note that, for OpenSearch, you need to prefix the version with `opensearch:`; see [OpenSearch compatibility](#opensearch).
4. This means that we wait for the entities to be searchable before considering a write complete. On a production setup, the `write-sync` default will provide better performance. Using `sync` is especially important when testing as you need the entities to be searchable immediately.
5. For development and tests, we rely on [Dev Services](#dev-services), which means Quarkus will start a PostgreSQL database and Elasticsearch cluster automatically. In production mode, however, we will want to start a PostgreSQL database and Elasticsearch cluster manually, which is why we provide Quarkus with this connection info in the `prod` profile (`%prod.` prefix).

> [!NOTE]
> Because we rely on [Dev Services](#dev-services), the database and Elasticsearch schema will automatically be dropped and re-created on each application startup in tests and dev mode (unless [`quarkus.hibernate-search-orm.schema-management.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-schema-management-strategy) is set explicitly).
>
> If for some reason you cannot use Dev Services, you will have to set the following properties to get similar behavior:
>
> ```properties
> %dev,test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
> %dev,test.quarkus.hibernate-search-orm.schema-management.strategy=drop-and-create
> ```
>
> See also [`quarkus.hibernate-search-orm.schema-management.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-schema-management-strategy).

> [!TIP]
> For more information about configuration of the Hibernate Search ORM extension, refer to the [Configuration Reference](#configuration-reference).

## Creating a frontend

Now let's add a simple web page to interact with our `LibraryResource`. Quarkus automatically serves static resources located under the `META-INF/resources` directory. In the `src/main/resources/META-INF/resources` directory, overwrite the existing `index.html` file with the content from this [index.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-search-orm-elasticsearch-quickstart/src/main/resources/META-INF/resources/index.html) file.

## Time to play with your application

You can now interact with your REST service:

\* start your Quarkus application with: +

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

\* open a browser to `http://localhost:8080/` \* search for authors or book titles (we initialized some data for you) \* create new authors and books and search for them too

As you can see, all your updates are automatically synchronized to the Elasticsearch cluster.

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

> [!NOTE]
> As usual with native executable compilation, this operation consumes a lot of memory.
>
> It might be safer to stop the two containers while you are building the native executable and start them again once you are done.

Running the native executable is as simple as executing `./target/hibernate-search-orm-elasticsearch-quickstart-1.0.0-SNAPSHOT-runner`.

You can then point your browser to `http://localhost:8080/` and use your application.

> [!NOTE]
> The startup is a bit slower than usual: it is mostly due to us dropping and recreating the database schema and the Elasticsearch mapping every time at startup. We also inject some data and execute the mass indexer.
>
> In a real life application, it is obviously something you won't do on every startup.

<a id="dev-services"></a>
## Dev Services (Configuration Free Datastores)

Quarkus supports a feature called [Dev Services](/guides/dev-services/index.md) that allows you to start various containers without any config.

In the case of Elasticsearch this support extends to the default Elasticsearch connection. What that means practically, is that if you have not configured `quarkus.hibernate-search-orm.elasticsearch.hosts`, Quarkus will automatically start an Elasticsearch container when running tests or in [dev mode](/guides/dev-mode-differences/index.md), and automatically configure the connection.

When running the production version of the application, the Elasticsearch connection needs to be configured as normal, so if you want to include a production database config in your `application.properties` and continue to use Dev Services we recommend that you use the `%prod.` profile to define your Elasticsearch settings.

> [!NOTE]
> Dev Services for Elasticsearch is currently unable to start multiple clusters concurrently, so it only works with the default backend of the default persistence unit: named persistence units or named backends won't be able to take advantage of Dev Services for Elasticsearch.

For more information you can read the [Dev Services for Elasticsearch guide](/guides/elasticsearch-dev-services/index.md).

<a id="programmatic-mapping"></a>
## Programmatic mapping

If, for some reason, adding Hibernate Search annotations to entities is not possible, mapping can be applied programmatically instead. Programmatic mapping is configured through the `ProgrammaticMappingConfigurationContext` that is exposed via a mapping configurer (`HibernateOrmSearchMappingConfigurer`).

> [!NOTE]
> A mapping configurer (`HibernateOrmSearchMappingConfigurer`) allows much more than just programmatic mapping capabilities. It also allows [configuring annotation mapping, bridges, and more](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapper-orm-mapping-configurer).

Below is an example of a mapping configurer that applies programmatic mapping:

```java
package org.acme.hibernate.search.elasticsearch.config;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension;

@SearchExtension // <1>
public class CustomMappingConfigurer implements HibernateOrmSearchMappingConfigurer {

	@Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        TypeMappingStep type = context.programmaticMapping()    // <2>
            .type(SomeIndexedEntity.class);                     // <3>
        type.indexed()                                          // <4>
            .index(SomeIndexedEntity.INDEX_NAME);               // <5>
        type.property("id").documentId();                       // <6>
        type.property("text").fullTextField();                  // <7>
    }
}
```

1. Annotate the configurer implementation with the `@SearchExtension` qualifier to tell Quarkus it should be used by Hibernate Search in the default persistence unit.

   The annotation can also target a specific persistence unit (`@SearchExtension(persistenceUnit = "nameOfYourPU")`).

2. Access the programmatic mapping context.
3. Create mapping step for the `SomeIndexedEntity` entity.
4. Define the `SomeIndexedEntity` entity as indexed.
5. Provide an index name to be used for the `SomeIndexedEntity` entity.
6. Define the document id property.
7. Define a full-text search field for the `text` property.

> [!TIP]
> Alternatively, if for some reason you can't or don't want to annotate your mapping configurer with `@SearchExtension`, you can simply annotate it with `@Dependent @Named("myMappingConfigurer")` and then reference it from configuration properties:
>
> ```properties
> quarkus.hibernate-search-orm.mapping.configurer=bean:myMappingConfigurer
> ```

<a id="opensearch"></a>
## OpenSearch compatibility

Hibernate Search is compatible with both [Elasticsearch](https://www.elastic.co/elasticsearch) and [OpenSearch](https://www.opensearch.org/), but it assumes it is working with an Elasticsearch cluster by default.

To have Hibernate Search work with an OpenSearch cluster instead, [prefix the configured version with `opensearch:`](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-configuration-version), as shown below.

```properties
quarkus.hibernate-search-orm.elasticsearch.version=opensearch:3.5
```

All other configuration options and APIs are exactly the same as with Elasticsearch.

You can find more information about compatible distributions and versions of Elasticsearch in [this section of Hibernate Search's reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#compatibility).

<a id="multiple-persistence-units"></a>
## Multiple persistence units

### Configuring multiple persistence units

With the Hibernate ORM extension, [you can set up multiple persistence units](/guides/hibernate-orm/index.md#multiple-persistence-units), each with its own datasource and configuration.

If you do declare multiple persistence units, you will also configure Hibernate Search separately for each persistence unit.

The properties at the root of the `quarkus.hibernate-search-orm.` namespace define the default persistence unit. For instance, the following snippet defines a default datasource and a default persistence unit, and sets the Elasticsearch host for that persistence unit to `es1.mycompany.com:9200`.

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:default;DB_CLOSE_DELAY=-1

quarkus.hibernate-search-orm.elasticsearch.hosts=es1.mycompany.com:9200
quarkus.hibernate-search-orm.elasticsearch.version=9
```

Using a map based approach, it is also possible to configure named persistence units:

```properties
quarkus.datasource."users".db-kind=h2 <1>
quarkus.datasource."users".jdbc.url=jdbc:h2:mem:users;DB_CLOSE_DELAY=-1

quarkus.datasource."inventory".db-kind=h2 <2>
quarkus.datasource."inventory".jdbc.url=jdbc:h2:mem:inventory;DB_CLOSE_DELAY=-1

quarkus.hibernate-orm."users".datasource=users <3>
quarkus.hibernate-orm."users".packages=org.acme.model.user

quarkus.hibernate-orm."inventory".datasource=inventory <4>
quarkus.hibernate-orm."inventory".packages=org.acme.model.inventory

quarkus.hibernate-search-orm."users".elasticsearch.hosts=es1.mycompany.com:9200 <5>
quarkus.hibernate-search-orm."users".elasticsearch.version=9

quarkus.hibernate-search-orm."inventory".elasticsearch.hosts=es2.mycompany.com:9200 <6>
quarkus.hibernate-search-orm."inventory".elasticsearch.version=9
```

1. Define a datasource named `users`.
2. Define a datasource named `inventory`.
3. Define a persistence unit called `users` pointing to the `users` datasource.
4. Define a persistence unit called `inventory` pointing to the `inventory` datasource.
5. Configure Hibernate Search for the `users` persistence unit, setting the Elasticsearch host for that persistence unit to `es1.mycompany.com:9200`.
6. Configure Hibernate Search for the `inventory` persistence unit, setting the Elasticsearch host for that persistence unit to `es2.mycompany.com:9200`.

<a id="multiple-persistence-units-attaching-model-classes"></a>
### Attaching model classes to persistence units

For each persistence unit, Hibernate Search will only consider indexed entities that are attached to that persistence unit. Entities are attached to a persistence unit by [configuring the Hibernate ORM extension](/guides/hibernate-orm/index.md#multiple-persistence-units-attaching-model-classes).

<a id="multiple-persistence-units-attaching-cdi"></a>
## CDI integration

<a id="injecting-entry-points"></a>
### Injecting entry points

You can inject Hibernate Search's main entry points, `SearchSession` and `SearchMapping`, using CDI:

```java
@Inject
SearchSession searchSession;
```

This will inject the `SearchSession` of the default persistence unit.

To inject the `SearchSession` of a named persistence unit (`users` in our example), just add a qualifier:

```java
@Inject
@PersistenceUnit("users") <1>
SearchSession searchSession;
```

1. This is the `@io.quarkus.hibernate.orm.PersistenceUnit` annotation.

You can inject the `SearchMapping` of a named persistence unit using the exact same mechanism:

```java
@Inject
@PersistenceUnit("users")
SearchMapping searchMapping;
```

<a id="plugging-in-custom-components"></a>
### Plugging in custom components

The Quarkus extension for Hibernate Search with Hibernate ORM will automatically inject components annotated with `@SearchExtension` into Hibernate Search.

The annotation can optionally target a specific persistence unit (`@SearchExtension(persistenceUnit = "nameOfYourPU")`), backend (`@SearchExtension(backend = "nameOfYourBackend")`), index (`@SearchExtension(index = "nameOfYourIndex")`), or a combination of those (`@SearchExtension(persistenceUnit = "nameOfYourPU", backend = "nameOfYourBackend", index = "nameOfYourIndex")`), when it makes sense for the type of the component being injected.

This feature is available for the following component types:

**`org.hibernate.search.engine.reporting.FailureHandler`**\
A component that should be notified of any failure occurring in a background process (mainly index operations). + Scope: one per persistence unit. + See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#configuration-background-failure-handling) for more information.

**`org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer`**\
A component used to configure the Hibernate Search mapping, in particular programmatically. + Scope: one or more per persistence unit. + See [this section of this guide](#programmatic-mapping) for more information.

**`org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy`**\
A component used to configure how to synchronize between application threads and indexing. + Scope: one per persistence unit. + Can also be set to built-in implementations through [`quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-indexing-plan-synchronization-strategy). + See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#indexing-plan-synchronization) for more information.

**`org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer`**\
A component used to configure full text analysis (e.g. analyzers, normalizers). + Scope: one or more per backend. + See [this section of this guide](#analysis-configurer) for more information.

**`org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy`**\
A component used to configure the Elasticsearch layout: index names, index aliases, ... + Scope: one per backend. + Can also be set to built-in implementations through [`quarkus.hibernate-search-orm.elasticsearch.layout.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-layout-strategy). + See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-indexlayout) for more information.

<a id="mapping-active"></a>
### Activate/deactivate Hibernate Search

When a persistence unit is configured for Hibernate Search indexing at build time, Hibernate Search is active in that persistence unit by default. Quarkus starts Hibernate Search when the application starts.

To deactivate Hibernate Search in a given persistence unit at runtime, set `quarkus.hibernate-search-orm-elasticsearch[.optional name].active` to `false`. This works similarly to [the corresponding feature of the Hibernate ORM extension](/guides/hibernate-orm/index.md#persistence-unit-active), but for [Hibernate Search beans](#injecting-entry-points).

<a id="offline-startup"></a>
## Offline startup

By default, Hibernate Search sends a few requests to the Elasticsearch cluster on startup. If the Elasticsearch cluster is not necessarily up and running when Hibernate Search starts, this could cause a startup failure.

To address this, you can configure Hibernate Search to not send any request on startup:

- Disable Elasticsearch version checks on startup by setting the configuration property [`quarkus.hibernate-search-orm.elasticsearch.version-check.enabled`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-version-check-enabled)  to `false`.
- Disable schema management on startup by setting the configuration property [`quarkus.hibernate-search-orm.schema-management.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-schema-management-strategy)  to `none`.

Of course, even with this configuration, Hibernate Search still won't be able to index anything or run search queries until the Elasticsearch cluster becomes accessible.

> [!IMPORTANT]
> If you disable automatic schema creation by setting `quarkus.hibernate-search-orm.schema-management.strategy` to `none`, you will have to create the schema manually at some point before your application starts persisting/updating entities and executing search requests.
>
> See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#schema-management-manager) for more information.

<a id="coordination"></a>
## Coordination through outbox polling

> [!CAUTION]
> Coordination through outbox polling is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, or even storage formats, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).

While it’s technically possible to use Hibernate Search and Elasticsearch in distributed applications, by default they suffer from [a few limitations](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#architecture-examples-no-coordination-elasticsearch-pros-and-cons).

These limitations are the result of Hibernate Search not coordinating between threads or application nodes by default.

In order to get rid of these limitations, you can [use the `outbox-polling` coordination strategy](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#architecture-examples-outbox-polling-elasticsearch). This strategy creates an outbox table in the database to push entity change events to, and relies on a background processor to consume these events and perform indexing.

To enable the `outbox-polling` coordination strategy, an additional extension is required:

**CLI**

```bash
quarkus extension add hibernate-search-orm-outbox-polling
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='hibernate-search-orm-outbox-polling'
```

**Gradle**

```bash
./gradlew addExtension --extensions='hibernate-search-orm-outbox-polling'
```

Once the extension is there, you will need to explicitly select the `outbox-polling` strategy by setting [`quarkus.hibernate-search-orm.coordination.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-coordination-strategy) to `outbox-polling`.

Finally, you will need to make sure that the Hibernate ORM entities added by Hibernate Search (to represent the outbox and agents) have corresponding tables/sequences in your database:

- If you are just starting with your application and intend to [let Hibernate ORM generate your database schema](/guides/hibernate-orm/index.md#dev-mode), then no worries: the entities required by Hibernate Search will be included in the generated schema.
- Otherwise, you must [manually alter your schema to add the necessary tables/sequences](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-schema).

> [!NOTE]
> The database schema Hibernate Search will expect for outbox-polling coordination may be customized through the following configuration properties:
>
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.catalog`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-catalog)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.schema`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-schema)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.table`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-table)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.uuid-gen-strategy`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-uuid-gen-strategy)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.uuid-type`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-uuid-type)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.catalog`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-catalog)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.schema`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-schema)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.table`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-table)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.uuid-gen-strategy`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-uuid-gen-strategy)
> - [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.uuid-type`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-uuid-type)

Once you are done with the above, you're ready to use Hibernate Search with an outbox. Don't change any code, and just start your application: it will automatically detect when multiple applications are connected to the same database, and coordinate the index updates accordingly.

> [!NOTE]
> Hibernate Search mostly behaves the same when using the `outbox-polling` coordination strategy as when not using it: application code (persisting entities, searching, etc.) should not require any change.
>
> However, there is one key difference: index updates are necessarily asynchronous; they are guaranteed to happen *eventually*, but not immediately.
>
> This means in particular that the configuration property [`quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-indexing-plan-synchronization-strategy) cannot be set when using the `outbox-polling` coordination strategy: Hibernate Search will always behave as if this property was set to `write-sync` (the default).
>
> This behavior is consistent with Elasticsearch's [near-real-time search](https://www.elastic.co/guide/en/elasticsearch/reference/current/near-real-time.html) and the recommended way of using Hibernate Search even when coordination is disabled.

For more information about coordination in Hibernate Search, see [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination).

For more information about configuration options related to coordination, see [Configuration of coordination with outbox polling](#configuration-reference-coordination-outbox-polling).

<a id="aws-request-signing"></a>
## <a id="configuration-reference-aws"></a> AWS request signing

If you need to use [Amazon’s managed Elasticsearch service](https://docs.aws.amazon.com/elasticsearch-service/), you will find it requires a proprietary authentication method involving request signing.

You can enable AWS request signing in Hibernate Search by adding a dedicated extension to your project and configuring it.

See [the documentation for the Hibernate Search ORM + Elasticsearch AWS extension](https://docs.quarkiverse.io/quarkus-hibernate-search-extras/dev/index.html#aws-configuration-reference) for more information.

<a id="management"></a>
## Management endpoint

> [!CAUTION]
> Hibernate Search's management endpoint is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, or even storage formats, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).

The Hibernate Search extension provides an HTTP endpoint to reindex your data through the [management interface](/guides/management-interface-reference/index.md). By default, this endpoint is not available. It can be enabled through configuration properties as shown below.

```properties
quarkus.management.enabled=true <1>
quarkus.hibernate-search-orm.management.enabled=true <2>
```

1. Enable the [management interface](/guides/management-interface-reference/index.md).
2. Enable Hibernate Search specific management endpoints.

Once the management endpoints are enabled, data can be re-indexed via `/q/hibernate-search/reindex`, where `/q` is the default management root path and `/hibernate-search` is the default Hibernate Search root management path. It (`/hibernate-search`) can be changed via configuration property as shown below.

```properties
quarkus.hibernate-search-orm.management.root-path=custom-root-path <1>
```

1. Use a custom `custom-root-path` path for Hibernate Search's management endpoint. If the default management root path is used then the reindex path becomes `/q/custom-root-path/reindex`.

This endpoint accepts `POST` requests with `application/json` content type only. All indexed entities will be re-indexed if an empty request body is submitted. If only a subset of entities must be re-indexed or if there is a need to have a custom configuration of the underlying mass indexer then this information can be passed through the request body as shown below.

```json
{
  "filter": {
    "types": ["EntityName1", "EntityName2", "EntityName3", ...], <1>
  },
  "massIndexer":{
    "typesToIndexInParallel": 1, <2>
  }
}
```

1. An array of entity names that should be re-indexed. If unspecified or empty, all entity types will be re-indexed.
2. Sets the number of entity types to be indexed in parallel.

The full list of possible filters and available mass indexer configurations is presented in the example below.

```json
{
  "filter": { <1>
    "types": ["EntityName1", "EntityName2", "EntityName3", ...], <2>
    "tenants": ["tenant1", "tenant2", ...] <3>
  },
  "massIndexer":{ <4>
    "typesToIndexInParallel": 1, <5>
    "threadsToLoadObjects": 6, <6>
    "batchSizeToLoadObjects": 10, <7>
    "cacheMode": "IGNORE", <8>
    "mergeSegmentsOnFinish": false, <9>
    "mergeSegmentsAfterPurge": true, <10>
    "dropAndCreateSchemaOnStart": false, <11>
    "purgeAllOnStart": true, <12>
    "idFetchSize": 100, <13>
    "transactionTimeout": 100000, <14>
  }
}
```

1. Filter object that allows to limit the scope of reindexing.
2. An array of entity names that should be re-indexed. If unspecified or empty, all entity types will be re-indexed.
3. An array of tenant ids, in case of multi-tenancy. If unspecified or empty, all tenants will be re-indexed.
4. Mass indexer configuration object.
5. Sets the number of entity types to be indexed in parallel.
6. Sets the number of threads to be used to load the root entities.
7. Sets the batch size used to load the root entities.
8. Sets the cache interaction mode for the data loading tasks.
9. Whether each index is merged into a single segment after indexing.
10. Whether each index is merged into a single segment after the initial index purge, just before indexing.
11. Whether the indexes and their schema (if they exist) should be dropped and re-created before indexing.
12. Whether all entities are removed from the indexes before indexing.
13. Specifies the fetch size to be used when loading primary keys if objects to be indexed.
14. Specifies the timeout of transactions for loading ids and entities to be re-indexed.

    Note all the properties in the JSON are optional, and only those that are needed should be used.


For more detailed information on mass indexer configuration see the [corresponding section of the Hibernate Search reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#indexing-massindexer-parameters).

Submitting the reindexing request will trigger indexing in the background. Mass indexing progress will appear in the application logs. For testing purposes, it might be useful to know when the indexing finished. Adding `wait_for=finished` query parameter to the URL will result in the management endpoint returning a chunked response that will report when the indexing starts and then when it is finished.

When working with multiple persistence units, the name of the persistence unit to reindex can be supplied through the `persistence_unit` query parameter: `/q/hibernate-search/reindex?persistence_unit=non-default-persistence-unit`.

## Further reading

If you are interested in learning more about Hibernate Search, the Hibernate team publishes [an extensive reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/), as well as a page listing [other relevant resources](https://hibernate.org/search/more-resources/).

## FAQ

### Why Elasticsearch only?

Hibernate Search supports both a Lucene backend and an Elasticsearch backend.

In the context of Quarkus and to build scalable applications, we thought the latter would make more sense. Thus, we focused our efforts on it.

We don't have plans to support the Lucene backend in Quarkus for now, though there is an issue tracking progress on such an implementation in the Quarkiverse: [quarkiverse/quarkus-hibernate-search-extras#179](https://github.com/quarkiverse/quarkus-hibernate-search-extras/issues/179).

<a id="configuration-reference"></a>
## Configuration Reference for Hibernate Search with Hibernate ORM

<a id="configuration-reference-main"></a>
### Main Configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-enabled"></a> [`quarkus.hibernate-search-orm.enabled`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-enabled) config_property_copy_button:+++quarkus.hibernate-search-orm.enabled+++[]<br><br>Whether Hibernate Search is enabled **during the build**.<br><br>If Hibernate Search is disabled during the build, all processing related to Hibernate Search will be skipped, but it will not be possible to activate Hibernate Search at runtime: `quarkus.hibernate-search-orm.active` will default to `false` and setting it to `true` will lead to an error.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-background-failure-handler"></a> [`quarkus.hibernate-search-orm.background-failure-handler`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-background-failure-handler) config_property_copy_button:+++quarkus.hibernate-search-orm.background-failure-handler+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".background-failure-handler` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".background-failure-handler+++[]<br><br>A [bean reference](/guides/hibernate-search-orm-elasticsearch/index.md#bean-reference-note-anchor) to a component that should be notified of any failure occurring in a background process (mainly index operations).<br><br>The referenced bean must implement `FailureHandler`.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#configuration-background-failure-handling) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `FailureHandler` implementation with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](/guides/hibernate-search-orm-elasticsearch/index.md#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_BACKGROUND_FAILURE_HANDLER+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-coordination-strategy"></a> [`quarkus.hibernate-search-orm.coordination.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-coordination-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.strategy+++[]<br><br>The strategy to use for coordinating between threads or even separate instances of the application, in particular in automatic indexing.<br><br>See [coordination](/guides/hibernate-search-orm-elasticsearch/index.md#coordination) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_STRATEGY+++[] | string | `none` |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-mapping-configurer"></a> [`quarkus.hibernate-search-orm.mapping.configurer`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-mapping-configurer) config_property_copy_button:+++quarkus.hibernate-search-orm.mapping.configurer+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".mapping.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".mapping.configurer+++[]<br><br>One or more [bean references](/guides/hibernate-search-orm-elasticsearch/index.md#bean-reference-note-anchor) to the component(s) used to configure the Hibernate Search mapping, in particular programmatically.<br><br>The referenced beans must implement `HibernateOrmSearchMappingConfigurer`.<br><br>See [Programmatic mapping](/guides/hibernate-search-orm-elasticsearch/index.md#programmatic-mapping) for an example on how mapping configurers can be used to apply programmatic mappings.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `HibernateOrmSearchMappingConfigurer` implementations with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](/guides/hibernate-search-orm-elasticsearch/index.md#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_MAPPING_CONFIGURER+++[] | list of string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-active"></a> [`quarkus.hibernate-search-orm.active`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-active) config_property_copy_button:+++quarkus.hibernate-search-orm.active+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".active` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".active+++[]<br><br>Whether Hibernate Search should be active for this persistence unit at runtime.<br><br>If Hibernate Search is not active, it won't index Hibernate ORM entities, and accessing the SearchMapping/SearchSession of the relevant persistence unit for search or other operation will not be possible.<br><br>Note that if Hibernate Search is disabled (i.e. `quarkus.hibernate-search-orm.enabled` is set to `false`), it won't be active for any persistence unit, and setting this property to `true` will fail.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ACTIVE+++[] | boolean | `'true' if Hibernate Search is enabled and the persistence unit is active; 'false' otherwise` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-schema-management-strategy"></a> [`quarkus.hibernate-search-orm.schema-management.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-schema-management-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.schema-management.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".schema-management.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".schema-management.strategy+++[]<br><br>The schema management strategy, controlling how indexes and their schema are created, updated, validated or dropped on startup and shutdown.<br><br>Available values:<br><br>!=== h!Strategy h!Definition<br><br>!none !Do nothing: assume that indexes already exist and that their schema matches Hibernate Search's expectations.<br><br>!validate !Validate that indexes exist and that their schema matches Hibernate Search's expectations.<br><br>If it does not, throw an exception, but make no attempt to fix the problem.<br><br>!create !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, do nothing: assume that their schema matches Hibernate Search's expectations.<br><br>!create-or-validate (**default** unless using Dev Services) !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, validate that their schema matches Hibernate Search's expectations.<br><br>If it does not, throw an exception, but make no attempt to fix the problem.<br><br>!create-or-update !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, validate that their schema matches Hibernate Search's expectations; if it does not match expectations, try to update it.<br><br>**This strategy is unfit for production environments**, due to several important limitations, but can be useful when developing.<br><br>!drop-and-create !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, drop them, then create them along with their schema.<br><br>!drop-and-create-and-drop (**default** when using Dev Services) !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, drop them, then create them along with their schema.<br><br>Also, drop indexes and their schema on shutdown. !===<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapper-orm-schema-management-strategy) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_SCHEMA_MANAGEMENT_STRATEGY+++[] | `none`, `validate`, `create`, `create-or-validate`, `create-or-update`, `drop-and-create`, `drop-and-create-and-drop` | `drop-and-create-and-drop when using Dev Services; create-or-validate otherwise` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-query-loading-cache-lookup-strategy"></a> [`quarkus.hibernate-search-orm.query.loading.cache-lookup.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-query-loading-cache-lookup-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.query.loading.cache-lookup.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".query.loading.cache-lookup.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".query.loading.cache-lookup.strategy+++[]<br><br>The strategy to use when loading entities during the execution of a search query.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_QUERY_LOADING_CACHE_LOOKUP_STRATEGY+++[] | `skip`, `persistence-context`, `persistence-context-then-second-level-cache` | `skip` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-query-loading-fetch-size"></a> [`quarkus.hibernate-search-orm.query.loading.fetch-size`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-query-loading-fetch-size) config_property_copy_button:+++quarkus.hibernate-search-orm.query.loading.fetch-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".query.loading.fetch-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".query.loading.fetch-size+++[]<br><br>The fetch size to use when loading entities during the execution of a search query.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_QUERY_LOADING_FETCH_SIZE+++[] | int | `100` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-indexing-plan-synchronization-strategy"></a> [`quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-indexing-plan-synchronization-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".indexing.plan.synchronization.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".indexing.plan.synchronization.strategy+++[]<br><br>How to synchronize between application threads and indexing, in particular when relying on (implicit) listener-triggered indexing on entity change, but also when using a `SearchIndexingPlan` explicitly.<br><br>Defines how complete indexing should be before resuming the application thread after a database transaction is committed.<br><br>> [!WARNING]<br>> Indexing synchronization is only relevant when coordination is disabled (which is the default).<br>><br>> With the [`outbox-polling` coordination strategy](/guides/hibernate-search-orm-elasticsearch/index.md#coordination), indexing happens in background threads and is always asynchronous; the behavior is equivalent to the `write-sync` synchronization strategy.<br><br>Available values:<br><br>!=== .2+h!Strategy .2+h!Throughput 3+^h!Guarantees when the application thread resumes<br><br>h!Changes applied h!Changes safe from crash/power loss h!Changes visible on search<br><br>!async !Best ^![times] ^![times] ^![times]<br><br>!write-sync (**default**) !Medium ^![check] ^![check] ^![times]<br><br>!read-sync !Medium to worst ^![check] ^![times] ^![check]<br><br>!sync !Worst ^![check] ^![check] ^![check] !===<br><br>This property also accepts a [bean reference](/guides/hibernate-search-orm-elasticsearch/index.md#bean-reference-note-anchor) to a custom implementations of `IndexingPlanSynchronizationStrategy`.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#indexing-plan-synchronization) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `IndexingPlanSynchronizationStrategy` implementation with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](/guides/hibernate-search-orm-elasticsearch/index.md#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_INDEXING_PLAN_SYNCHRONIZATION_STRATEGY+++[] | string | `write-sync` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-multi-tenancy-tenant-ids"></a> [`quarkus.hibernate-search-orm.multi-tenancy.tenant-ids`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-multi-tenancy-tenant-ids) config_property_copy_button:+++quarkus.hibernate-search-orm.multi-tenancy.tenant-ids+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".multi-tenancy.tenant-ids` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".multi-tenancy.tenant-ids+++[]<br><br>An exhaustive list of all tenant identifiers that may be used by the application when multi-tenancy is enabled.<br><br>Mainly useful when using the {@code outbox-polling} coordination strategy, since it involves setting up one background processor per tenant.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_MULTI_TENANCY_TENANT_IDS+++[] | list of string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-automatic-indexing-synchronization-strategy"></a> <mark>link:#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-automatic-indexing-synchronization-strategy\[`quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy`\]</mark> config_property_copy_button:+++quarkus.hibernate-search-orm.automatic-indexing.synchronization.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".automatic-indexing.synchronization.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".automatic-indexing.synchronization.strategy+++[]<br><br>*This property is deprecated: Use `quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy` instead.*<br><br>The synchronization strategy to use when indexing automatically.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY+++[] | string | `write-sync` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-automatic-indexing-enable-dirty-check"></a> <mark>link:#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-automatic-indexing-enable-dirty-check\[`quarkus.hibernate-search-orm.automatic-indexing.enable-dirty-check`\]</mark> config_property_copy_button:+++quarkus.hibernate-search-orm.automatic-indexing.enable-dirty-check+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".automatic-indexing.enable-dirty-check` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".automatic-indexing.enable-dirty-check+++[]<br><br>*This property is deprecated: This property is deprecated with no alternative to replace it. In the future, a dirty check will always be performed when considering whether to trigger reindexing.*<br><br>Whether to check if dirty properties are relevant to indexing before actually reindexing an entity.<br><br>When enabled, re-indexing of an entity is skipped if the only changes are on properties that are not used when indexing.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK+++[] | boolean | `true` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_section_quarkus-hibernate-search-orm-elasticsearch"></a> [Configuration for Elasticsearch/OpenSearch backends](#quarkus-hibernate-search-orm-elasticsearch_section_quarkus-hibernate-search-orm-elasticsearch) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-version"></a> [`quarkus.hibernate-search-orm.elasticsearch.version`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-version) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.version+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".version` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".version+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.version` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.version+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".version` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".version+++[]<br><br>The version of Elasticsearch used in the cluster.<br><br>As the schema is generated without a connection to the server, this item is mandatory.<br><br>It doesn't have to be the exact version (it can be `7` or `7.1` for instance) but it has to be sufficiently precise to choose a model dialect (the one used to generate the schema) compatible with the protocol dialect (the one used to communicate with Elasticsearch).<br><br>There's no rule of thumb here as it depends on the schema incompatibilities introduced by Elasticsearch versions. In any case, if there is a problem, you will have an error when Hibernate Search tries to connect to the cluster.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_VERSION+++[] | ElasticsearchVersion |  |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-settings-file"></a> [`quarkus.hibernate-search-orm.elasticsearch.schema-management.settings-file`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-settings-file) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.settings-file+++[]<br><br>Path to a file in the classpath holding custom index settings to be included in the index definition when creating an Elasticsearch index.<br><br>The provided settings will be merged with those generated by Hibernate Search, including analyzer definitions. When analysis is configured both through an analysis configurer and these custom settings, the behavior is undefined; it should not be relied upon.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-configuration-index-settings) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_SCHEMA_MANAGEMENT_SETTINGS_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-mapping-file"></a> [`quarkus.hibernate-search-orm.elasticsearch.schema-management.mapping-file`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-mapping-file) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.mapping-file+++[]<br><br>Path to a file in the classpath holding a custom index mapping to be included in the index definition when creating an Elasticsearch index.<br><br>The file does not need to (and generally shouldn't) contain the full mapping: Hibernate Search will automatically inject missing properties (index fields) in the given mapping.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-mapping-custom) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_SCHEMA_MANAGEMENT_MAPPING_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-analysis-configurer"></a> [`quarkus.hibernate-search-orm.elasticsearch.analysis.configurer`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-analysis-configurer) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.analysis.configurer+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".analysis.configurer+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.analysis.configurer+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".analysis.configurer+++[]<br><br>One or more [bean references](#bean-reference-note-anchor) to the component(s) used to configure full text analysis (e.g. analyzers, normalizers).<br><br>The referenced beans must implement `ElasticsearchAnalysisConfigurer`.<br><br>See [Setting up the analyzers](#analysis-configurer) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `ElasticsearchAnalysisConfigurer` implementations with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_ANALYSIS_CONFIGURER+++[] | list of string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-hosts"></a> [`quarkus.hibernate-search-orm.elasticsearch.hosts`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-hosts) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.hosts+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".hosts` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".hosts+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.hosts` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.hosts+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".hosts` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".hosts+++[]<br><br>The list of hosts of the Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_HOSTS+++[] | list of string | `localhost:9200` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-protocol"></a> [`quarkus.hibernate-search-orm.elasticsearch.protocol`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-protocol) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.protocol+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".protocol` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".protocol+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.protocol` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.protocol+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".protocol` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".protocol+++[]<br><br>The protocol to use when contacting Elasticsearch servers. Set to "https" to enable SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_PROTOCOL+++[] | tooltip:http[Use clear-text HTTP,opts=with SSL/TLS disabled.], tooltip:https[Use HTTPS,opts=with SSL/TLS enabled.] | tooltip:http[Use clear-text HTTP,opts=with SSL/TLS disabled.] |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-username"></a> [`quarkus.hibernate-search-orm.elasticsearch.username`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-username) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.username+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".username` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".username+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.username` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.username+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".username` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".username+++[]<br><br>The username used for authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_USERNAME+++[] | string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-password"></a> [`quarkus.hibernate-search-orm.elasticsearch.password`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-password) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.password+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".password` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".password+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.password` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.password+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".password` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".password+++[]<br><br>The password used for authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_PASSWORD+++[] | string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-connection-timeout"></a> [`quarkus.hibernate-search-orm.elasticsearch.connection-timeout`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-connection-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.connection-timeout+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".connection-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".connection-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.connection-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.connection-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".connection-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".connection-timeout+++[]<br><br>The timeout when establishing a connection to an Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm) | `1S` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-read-timeout"></a> [`quarkus.hibernate-search-orm.elasticsearch.read-timeout`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-read-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.read-timeout+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".read-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".read-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.read-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.read-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".read-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".read-timeout+++[]<br><br>The timeout when reading responses from an Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm) | `30S` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-request-timeout"></a> [`quarkus.hibernate-search-orm.elasticsearch.request-timeout`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-request-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.request-timeout+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".request-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".request-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.request-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.request-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".request-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".request-timeout+++[]<br><br>The timeout when executing a request to an Elasticsearch server.<br><br>This includes the time needed to wait for a connection to be available, send the request and read the response.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_REQUEST_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm) |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-max-connections"></a> [`quarkus.hibernate-search-orm.elasticsearch.max-connections`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-max-connections) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.max-connections+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".max-connections` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".max-connections+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.max-connections` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.max-connections+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".max-connections` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".max-connections+++[]<br><br>The maximum number of connections to all the Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_MAX_CONNECTIONS+++[] | int | `40` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-max-connections-per-route"></a> [`quarkus.hibernate-search-orm.elasticsearch.max-connections-per-route`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-max-connections-per-route) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.max-connections-per-route+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".max-connections-per-route` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".max-connections-per-route+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.max-connections-per-route` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.max-connections-per-route+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".max-connections-per-route` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".max-connections-per-route+++[]<br><br>The maximum number of connections per Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_MAX_CONNECTIONS_PER_ROUTE+++[] | int | `20` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-discovery-enabled"></a> [`quarkus.hibernate-search-orm.elasticsearch.discovery.enabled`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-discovery-enabled) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.discovery.enabled+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".discovery.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".discovery.enabled+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.discovery.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.discovery.enabled+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".discovery.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".discovery.enabled+++[]<br><br>Defines if automatic discovery is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_DISCOVERY_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-discovery-refresh-interval"></a> [`quarkus.hibernate-search-orm.elasticsearch.discovery.refresh-interval`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-discovery-refresh-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.discovery.refresh-interval+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".discovery.refresh-interval` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".discovery.refresh-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.discovery.refresh-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.discovery.refresh-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".discovery.refresh-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".discovery.refresh-interval+++[]<br><br>Refresh interval of the node list.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_DISCOVERY_REFRESH_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm) | `10S` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-thread-pool-size"></a> [`quarkus.hibernate-search-orm.elasticsearch.thread-pool.size`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-thread-pool-size) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.thread-pool.size+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".thread-pool.size` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".thread-pool.size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.thread-pool.size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.thread-pool.size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".thread-pool.size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".thread-pool.size+++[]<br><br>The size of the thread pool assigned to the backend.<br><br>Note that number is **per backend**, not per index. Adding more indexes will not add more threads.<br><br>As all operations happening in this thread-pool are non-blocking, raising its size above the number of processor cores available to the JVM will not bring noticeable performance benefit. The only reason to alter this setting would be to reduce the number of threads; for example, in an application with a single index with a single indexing queue, running on a machine with 64 processor cores, you might want to bring down the number of threads.<br><br>Defaults to the number of processor cores available to the JVM on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_THREAD_POOL_SIZE+++[] | int |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-query-shard-failure-ignore"></a> [`quarkus.hibernate-search-orm.elasticsearch.query.shard-failure.ignore`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-query-shard-failure-ignore) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.query.shard-failure.ignore+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".query.shard-failure.ignore` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".query.shard-failure.ignore+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.query.shard-failure.ignore` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.query.shard-failure.ignore+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".query.shard-failure.ignore` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".query.shard-failure.ignore+++[]<br><br>Whether partial shard failures are ignored (`true`) or lead to Hibernate Search throwing an exception (`false`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_QUERY_SHARD_FAILURE_IGNORE+++[] | boolean | `false` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-version-check-enabled"></a> [`quarkus.hibernate-search-orm.elasticsearch.version-check.enabled`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-version-check-enabled) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.version-check.enabled+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".version-check.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".version-check.enabled+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.version-check.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.version-check.enabled+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".version-check.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".version-check.enabled+++[]<br><br>Whether Hibernate Search should check the version of the Elasticsearch cluster on startup.<br><br>Set to `false` if the Elasticsearch cluster may not be available on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_VERSION_CHECK_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-required-status"></a> [`quarkus.hibernate-search-orm.elasticsearch.schema-management.required-status`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-required-status) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.required-status+++[]<br><br>The minimal [https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html\[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html[Elasticsearch) cluster status\] required on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_SCHEMA_MANAGEMENT_REQUIRED_STATUS+++[] | `green`, `yellow`, `red` | `yellow` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-required-status-wait-timeout"></a> [`quarkus.hibernate-search-orm.elasticsearch.schema-management.required-status-wait-timeout`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-schema-management-required-status-wait-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".schema-management.required-status-wait-timeout+++[]<br><br>How long we should wait for the status before failing the bootstrap.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_SCHEMA_MANAGEMENT_REQUIRED_STATUS_WAIT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm) | `10S` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexing-queue-count"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexing.queue-count`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexing-queue-count) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexing.queue-count+++[]<br><br>The number of indexing queues assigned to each index.<br><br>Higher values will lead to more connections being used in parallel, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXING_QUEUE_COUNT+++[] | int | `10` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexing-queue-size"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexing.queue-size`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexing-queue-size) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexing.queue-size+++[]<br><br>The size of indexing queues.<br><br>Lower values may lead to lower memory usage, especially if there are many queues, but values that are too low will reduce the likeliness of reaching the max bulk size and increase the likeliness of application threads blocking because the queue is full, which may lead to lower indexing throughput.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXING_QUEUE_SIZE+++[] | int | `1000` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexing-max-bulk-size"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexing.max-bulk-size`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexing-max-bulk-size) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexing.max-bulk-size+++[]<br><br>The maximum size of bulk requests created when processing indexing queues.<br><br>Higher values will lead to more documents being sent in each HTTP request sent to Elasticsearch, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Note that raising this number above the queue size has no effect, as bulks cannot include more requests than are contained in the queue.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXING_MAX_BULK_SIZE+++[] | int | `100` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-layout-strategy"></a> [`quarkus.hibernate-search-orm.elasticsearch.layout.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-layout-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.layout.strategy+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".layout.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".layout.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.layout.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.layout.strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".layout.strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".layout.strategy+++[]<br><br>A [bean reference](#bean-reference-note-anchor) to the component used to configure the Elasticsearch layout: index names, index aliases, ...<br><br>The referenced bean must implement `IndexLayoutStrategy`.<br><br>Available built-in implementations:<br><br>**`simple`**\<br>The default, future-proof strategy: if the index name in Hibernate Search is `myIndex`, this strategy will create an index named `myindex-000001`, an alias for write operations named `myindex-write`, and an alias for read operations named `myindex-read`.<br><br>**`no-alias`**\<br>A strategy without index aliases, mostly useful on legacy clusters: if the index name in Hibernate Search is `myIndex`, this strategy will create an index named `myindex`, and will not use any alias.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-indexlayout) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `IndexLayoutStrategy` implementation with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_LAYOUT_STRATEGY+++[] | string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_section_quarkus-hibernate-search-orm-elasticsearch-indexes"></a> [Per-index configuration overrides](#quarkus-hibernate-search-orm-elasticsearch_section_quarkus-hibernate-search-orm-elasticsearch-indexes) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-settings-file"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.settings-file`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-settings-file) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.settings-file+++[]<br><br>Path to a file in the classpath holding custom index settings to be included in the index definition when creating an Elasticsearch index.<br><br>The provided settings will be merged with those generated by Hibernate Search, including analyzer definitions. When analysis is configured both through an analysis configurer and these custom settings, the behavior is undefined; it should not be relied upon.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-configuration-index-settings) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_SETTINGS_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-mapping-file"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.mapping-file`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-mapping-file) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.mapping-file+++[]<br><br>Path to a file in the classpath holding a custom index mapping to be included in the index definition when creating an Elasticsearch index.<br><br>The file does not need to (and generally shouldn't) contain the full mapping: Hibernate Search will automatically inject missing properties (index fields) in the given mapping.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-mapping-custom) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_MAPPING_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-analysis-configurer"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".analysis.configurer`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-analysis-configurer) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".analysis.configurer+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".analysis.configurer+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".analysis.configurer+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".analysis.configurer+++[]<br><br>One or more [bean references](#bean-reference-note-anchor) to the component(s) used to configure full text analysis (e.g. analyzers, normalizers).<br><br>The referenced beans must implement `ElasticsearchAnalysisConfigurer`.<br><br>See [Setting up the analyzers](#analysis-configurer) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `ElasticsearchAnalysisConfigurer` implementations with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__ANALYSIS_CONFIGURER+++[] | list of string |  |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-required-status"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.required-status`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-required-status) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.required-status+++[]<br><br>The minimal [https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html\[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html[Elasticsearch) cluster status\] required on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_REQUIRED_STATUS+++[] | `green`, `yellow`, `red` | `yellow` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-required-status-wait-timeout"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.required-status-wait-timeout`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-schema-management-required-status-wait-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".schema-management.required-status-wait-timeout+++[]<br><br>How long we should wait for the status before failing the bootstrap.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_REQUIRED_STATUS_WAIT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm) | `10S` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-indexing-queue-count"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".indexing.queue-count`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-indexing-queue-count) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".indexing.queue-count+++[]<br><br>The number of indexing queues assigned to each index.<br><br>Higher values will lead to more connections being used in parallel, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__INDEXING_QUEUE_COUNT+++[] | int | `10` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-indexing-queue-size"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".indexing.queue-size`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-indexing-queue-size) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".indexing.queue-size+++[]<br><br>The size of indexing queues.<br><br>Lower values may lead to lower memory usage, especially if there are many queues, but values that are too low will reduce the likeliness of reaching the max bulk size and increase the likeliness of application threads blocking because the queue is full, which may lead to lower indexing throughput.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__INDEXING_QUEUE_SIZE+++[] | int | `1000` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-indexing-max-bulk-size"></a> [`quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".indexing.max-bulk-size`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-elasticsearch-indexes-index-name-indexing-max-bulk-size) config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch.indexes."index-name".indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-orm.elasticsearch."backend-name".indexes."index-name".indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch.indexes."index-name".indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".elasticsearch."backend-name".indexes."index-name".indexing.max-bulk-size+++[]<br><br>The maximum size of bulk requests created when processing indexing queues.<br><br>Higher values will lead to more documents being sent in each HTTP request sent to Elasticsearch, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Note that raising this number above the queue size has no effect, as bulks cannot include more requests than are contained in the queue.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_ELASTICSEARCH_INDEXES__INDEX_NAME__INDEXING_MAX_BULK_SIZE+++[] | int | `100` |
| <a id="quarkus-hibernate-search-orm-elasticsearch_section_quarkus-hibernate-search-orm-management"></a> [Management interface](#quarkus-hibernate-search-orm-elasticsearch_section_quarkus-hibernate-search-orm-management) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-management-root-path"></a> [`quarkus.hibernate-search-orm.management.root-path`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-management-root-path) config_property_copy_button:+++quarkus.hibernate-search-orm.management.root-path+++[]<br><br>Root path for reindexing endpoints. This value will be resolved as a path relative to `${quarkus.management.root-path}`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_MANAGEMENT_ROOT_PATH+++[] | string | `hibernate-search/` |
| [lock] <a id="quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-management-enabled"></a> [`quarkus.hibernate-search-orm.management.enabled`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-management-enabled) config_property_copy_button:+++quarkus.hibernate-search-orm.management.enabled+++[]<br><br>If management interface is turned on the reindexing endpoints will be published under the management interface. This property allows to enable this functionality by setting it to ``true\`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_MANAGEMENT_ENABLED+++[] | boolean | `false` |

<a id="duration-note-anchor-quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm"></a>

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

<a id="bean-reference-note-anchor"></a>

> [!NOTE]
> **About bean references**
>
> First, be aware that referencing beans in configuration properties is optional and, in fact, discouraged: you can achieve the same results by annotating your beans with `@SearchExtension`. See [this section](#plugging-in-custom-components) for more information.
>
> If you really do want to reference beans using a string value in configuration properties know that string is parsed; here are the most common formats:
>
> - `bean:` followed by the name of a `@Named` CDI bean. For example `bean:myBean`.
> - `class:` followed by the fully-qualified name of a class, to be instantiated through CDI if it's a CDI bean, or through its public, no-argument constructor otherwise. For example `class:com.mycompany.MyClass`.
> - An arbitrary string referencing a built-in implementation. Available values are detailed in the documentation of each configuration property, such as `async`/`read-sync`/`write-sync`/`sync` for [`quarkus.hibernate-search-orm.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-indexing-plan-synchronization-strategy).
>
> Other formats are also accepted, but are only useful for advanced use cases. See [this section of Hibernate Search's reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#configuration-bean-reference-parsing) for more information.

<a id="configuration-reference-coordination-outbox-polling"></a>
### Configuration of coordination with outbox polling

> [!NOTE]
> These configuration properties require an additional extension. See [Coordination through outbox polling](#coordination).

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-enabled"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.enabled`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-enabled) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.enabled+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.enabled+++[]<br><br>Whether the event processor is enabled, i.e. whether events will be processed to perform automatic reindexing on this instance of the application.<br><br>This can be set to `false` to disable event processing on some application nodes, for example to dedicate some nodes to HTTP request processing and other nodes to event processing.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-shards-total-count"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.shards.total-count`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-shards-total-count) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.shards.total-count+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.shards.total-count` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.shards.total-count+++[]<br><br>The total number of shards that will form a partition of the entity change events to process.<br><br>By default, sharding is dynamic and setting this property is not necessary.<br><br>If you want to control explicitly the number and assignment of shards, you must configure static sharding and then setting this property as well as the assigned shards (see `shards.assigned`) is necessary.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor-sharding) for more information about event processor sharding.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_SHARDS_TOTAL_COUNT+++[] | int |  |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-shards-assigned"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.shards.assigned`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-shards-assigned) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.shards.assigned+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.shards.assigned` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.shards.assigned+++[]<br><br>Among shards that will form a partition of the entity change events, the shards that will be processed by this application instance.<br><br>By default, sharding is dynamic and setting this property is not necessary.<br><br>If you want to control explicitly the number and assignment of shards, you must configure static sharding and then setting this property as well as the total shard count is necessary.<br><br>Shards are referred to by an index in the range `[0, total_count - 1]` (see `shards.total-count`). A given application node must be assigned at least one shard but may be assigned multiple shards by setting `shards.assigned` to a comma-separated list, e.g. `0,3`.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor-sharding) for more information about event processor sharding.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_SHARDS_ASSIGNED+++[] | list of int |  |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-polling-interval"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.polling-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-polling-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.polling-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.polling-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.polling-interval+++[]<br><br>How long to wait for another query to the outbox events table after a query didn’t return any event.<br><br>Lower values will reduce the time it takes for a change to be reflected in the index, but will increase the stress on the database when there are no new events.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_POLLING_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `0.100S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-pulse-interval"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.pulse-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-pulse-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.pulse-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.pulse-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.pulse-interval+++[]<br><br>How long the event processor can poll for events before it must perform a "pulse", updating and checking registrations in the agents table.<br><br>The pulse interval must be set to a value between the polling interval and one third (1/3) of the expiration interval.<br><br>Low values (closer to the polling interval) mean less time wasted not processing events when a node joins or leaves the cluster, and reduced risk of wasting time not processing events because an event processor is incorrectly considered disconnected, but more stress on the database because of more frequent checks of the list of agents.<br><br>High values (closer to the expiration interval) mean more time wasted not processing events when a node joins or leaves the cluster, and increased risk of wasting time not processing events because an event processor is incorrectly considered disconnected, but less stress on the database because of less frequent checks of the list of agents.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_PULSE_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `2S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-pulse-expiration"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.pulse-expiration`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-pulse-expiration) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.pulse-expiration+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.pulse-expiration` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.pulse-expiration+++[]<br><br>How long an event processor "pulse" remains valid before considering the processor disconnected and forcibly removing it from the cluster.<br><br>The expiration interval must be set to a value at least 3 times larger than the pulse interval.<br><br>Low values (closer to the pulse interval) mean less time wasted not processing events when a node abruptly leaves the cluster due to a crash or network failure, but increased risk of wasting time not processing events because an event processor is incorrectly considered disconnected.<br><br>High values (much larger than the pulse interval) mean more time wasted not processing events when a node abruptly leaves the cluster due to a crash or network failure, but reduced risk of wasting time not processing events because an event processor is incorrectly considered disconnected.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_PULSE_EXPIRATION+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `30S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-batch-size"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.batch-size`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-batch-size) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.batch-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.batch-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.batch-size+++[]<br><br>How many outbox events, at most, are processed in a single transaction.<br><br>Higher values will reduce the number of transactions opened by the background process and may increase performance thanks to the first-level cache (persistence context), but will increase memory usage and in extreme cases may lead to ``OutOfMemoryError``s.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_BATCH_SIZE+++[] | int | `50` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-transaction-timeout"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.transaction-timeout`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-transaction-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.transaction-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.transaction-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.transaction-timeout+++[]<br><br>The timeout for transactions processing outbox events.<br><br>When this property is not set, Hibernate Search will use whatever default transaction timeout is configured in the JTA transaction manager, which may be too low for batch processing and lead to transaction timeouts when processing batches of events. If this happens, set a higher transaction timeout for event processing using this property.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_TRANSACTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) |  |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-retry-delay"></a> [`quarkus.hibernate-search-orm.coordination.event-processor.retry-delay`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-event-processor-retry-delay) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.event-processor.retry-delay+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.retry-delay` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.event-processor.retry-delay+++[]<br><br>How long the event processor must wait before re-processing an event after its previous processing failed.<br><br>Use the value `0S` to reprocess failed events as soon as possible, with no delay.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_EVENT_PROCESSOR_RETRY_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `30S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-mass-indexer-polling-interval"></a> [`quarkus.hibernate-search-orm.coordination.mass-indexer.polling-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-mass-indexer-polling-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.mass-indexer.polling-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.mass-indexer.polling-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.mass-indexer.polling-interval+++[]<br><br>How long to wait for another query to the agent table when actively waiting for event processors to suspend themselves.<br><br>Low values will reduce the time it takes for the mass indexer agent to detect that event processors finally suspended themselves, but will increase the stress on the database while the mass indexer agent is actively waiting.<br><br>High values will increase the time it takes for the mass indexer agent to detect that event processors finally suspended themselves, but will reduce the stress on the database while the mass indexer agent is actively waiting.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-mass-indexer) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_MASS_INDEXER_POLLING_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `0.100S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-mass-indexer-pulse-interval"></a> [`quarkus.hibernate-search-orm.coordination.mass-indexer.pulse-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-mass-indexer-pulse-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.mass-indexer.pulse-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.mass-indexer.pulse-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.mass-indexer.pulse-interval+++[]<br><br>How long the mass indexer can wait before it must perform a "pulse", updating and checking registrations in the agent table.<br><br>The pulse interval must be set to a value between the polling interval and one third (1/3) of the expiration interval.<br><br>Low values (closer to the polling interval) mean reduced risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected, but more stress on the database because of more frequent updates of the mass indexer agent's entry in the agent table.<br><br>High values (closer to the expiration interval) mean increased risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected, but less stress on the database because of less frequent updates of the mass indexer agent's entry in the agent table.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-mass-indexer) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_MASS_INDEXER_PULSE_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `2S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-mass-indexer-pulse-expiration"></a> [`quarkus.hibernate-search-orm.coordination.mass-indexer.pulse-expiration`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-mass-indexer-pulse-expiration) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.mass-indexer.pulse-expiration+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.mass-indexer.pulse-expiration` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.mass-indexer.pulse-expiration+++[]<br><br>How long an event processor "pulse" remains valid before considering the processor disconnected and forcibly removing it from the cluster.<br><br>The expiration interval must be set to a value at least 3 times larger than the pulse interval.<br><br>Low values (closer to the pulse interval) mean less time wasted with event processors not processing events when a mass indexer agent terminates due to a crash, but increased risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected.<br><br>High values (much larger than the pulse interval) mean more time wasted with event processors not processing events when a mass indexer agent terminates due to a crash, but reduced risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-mass-indexer) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_MASS_INDEXER_PULSE_EXPIRATION+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `30S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_section_quarkus-hibernate-search-orm"></a> [Configuration for persistence units](#quarkus-hibernate-search-orm-outbox-polling_section_quarkus-hibernate-search-orm) | Type | Default |
| <a id="quarkus-hibernate-search-orm-outbox-polling_section_quarkus-hibernate-search-orm-coordination-entity-mapping"></a> [Configuration for the mapping of entities used for outbox-polling coordination](#quarkus-hibernate-search-orm-outbox-polling_section_quarkus-hibernate-search-orm-coordination-entity-mapping) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-catalog"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.catalog`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-catalog) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.agent.catalog+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.catalog` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.catalog+++[]<br><br>The database catalog to use for the agent table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_AGENT_CATALOG+++[] | string | `Default catalog configured in Hibernate ORM` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-schema"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.schema`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-schema) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.agent.schema+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.schema` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.schema+++[]<br><br>The schema catalog to use for the agent table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_AGENT_SCHEMA+++[] | string | `Default catalog configured in Hibernate ORM` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-table"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.table`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-table) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.agent.table+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.table` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.table+++[]<br><br>The name of the agent table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_AGENT_TABLE+++[] | string | `HSEARCH_AGENT` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-uuid-gen-strategy"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.uuid-gen-strategy`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-uuid-gen-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.agent.uuid-gen-strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.uuid-gen-strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.uuid-gen-strategy+++[]<br><br>The UUID generator strategy used for the agent table.<br><br>Available strategies:<br><br>- `auto` (the default) is the same as `random` which uses `UUID#randomUUID()`.<br>- `time` is an IP based strategy consistent with IETF RFC 4122.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_AGENT_UUID_GEN_STRATEGY+++[] | `auto`, `random`, `time` | `auto` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-uuid-type"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.agent.uuid-type`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-agent-uuid-type) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.agent.uuid-type+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.uuid-type` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.agent.uuid-type+++[]<br><br>The name of the Hibernate ORM basic type used for representing an UUID in the outbox event table.<br><br>Refer to [this section of the Hibernate ORM documentation](https://docs.hibernate.org/orm/7.4/userguide/html_single/#basic-uuid) to see the possible UUID representations.<br><br>Defaults to the special value `default`, which will result into one of `char`/`binary` depending on the database kind.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_AGENT_UUID_TYPE+++[] | string | `char/binary depending on the database kind` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-catalog"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.catalog`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-catalog) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.catalog+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.catalog` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.catalog+++[]<br><br>The database catalog to use for the outbox event table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_CATALOG+++[] | string | `Default catalog configured in Hibernate ORM` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-schema"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.schema`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-schema) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.schema+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.schema` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.schema+++[]<br><br>The schema catalog to use for the outbox event table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_SCHEMA+++[] | string | `Default schema configured in Hibernate ORM` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-table"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.table`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-table) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.table+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.table` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.table+++[]<br><br>The name of the outbox event table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_TABLE+++[] | string | `HSEARCH_OUTBOX_EVENT` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-uuid-gen-strategy"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.uuid-gen-strategy`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-uuid-gen-strategy) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.uuid-gen-strategy+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.uuid-gen-strategy` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.uuid-gen-strategy+++[]<br><br>The UUID generator strategy used for the outbox event table.<br><br>Available strategies:<br><br>- `auto` (the default) is the same as `random` which uses `UUID#randomUUID()`.<br>- `time` is an IP based strategy consistent with IETF RFC 4122.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_GEN_STRATEGY+++[] | `auto`, `random`, `time` | `auto` |
| [lock] <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-uuid-type"></a> [`quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.uuid-type`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-entity-mapping-outbox-event-uuid-type) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.entity-mapping.outbox-event.uuid-type+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.uuid-type` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.entity-mapping.outbox-event.uuid-type+++[]<br><br>The name of the Hibernate ORM basic type used for representing an UUID in the outbox event table.<br><br>Refer to [this section of the Hibernate ORM documentation](https://docs.hibernate.org/orm/7.4/userguide/html_single/#basic-uuid) to see the possible UUID representations.<br><br>Defaults to the special value `default`, which will result into one of `char`/`binary` depending on the database kind.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_ENTITY_MAPPING_OUTBOX_EVENT_UUID_TYPE+++[] | string | `char/binary depending on the database kind` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_section_quarkus-hibernate-search-orm-coordination-tenants"></a> [Per-tenant configuration overrides](#quarkus-hibernate-search-orm-outbox-polling_section_quarkus-hibernate-search-orm-coordination-tenants) | Type | Default |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-enabled"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.enabled`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-enabled) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.enabled+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.enabled` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.enabled+++[]<br><br>Whether the event processor is enabled, i.e. whether events will be processed to perform automatic reindexing on this instance of the application.<br><br>This can be set to `false` to disable event processing on some application nodes, for example to dedicate some nodes to HTTP request processing and other nodes to event processing.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-shards-total-count"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.shards.total-count`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-shards-total-count) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.shards.total-count+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.shards.total-count` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.shards.total-count+++[]<br><br>The total number of shards that will form a partition of the entity change events to process.<br><br>By default, sharding is dynamic and setting this property is not necessary.<br><br>If you want to control explicitly the number and assignment of shards, you must configure static sharding and then setting this property as well as the assigned shards (see `shards.assigned`) is necessary.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor-sharding) for more information about event processor sharding.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_SHARDS_TOTAL_COUNT+++[] | int |  |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-shards-assigned"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.shards.assigned`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-shards-assigned) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.shards.assigned+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.shards.assigned` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.shards.assigned+++[]<br><br>Among shards that will form a partition of the entity change events, the shards that will be processed by this application instance.<br><br>By default, sharding is dynamic and setting this property is not necessary.<br><br>If you want to control explicitly the number and assignment of shards, you must configure static sharding and then setting this property as well as the total shard count is necessary.<br><br>Shards are referred to by an index in the range `[0, total_count - 1]` (see `shards.total-count`). A given application node must be assigned at least one shard but may be assigned multiple shards by setting `shards.assigned` to a comma-separated list, e.g. `0,3`.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor-sharding) for more information about event processor sharding.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_SHARDS_ASSIGNED+++[] | list of int |  |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-polling-interval"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.polling-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-polling-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.polling-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.polling-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.polling-interval+++[]<br><br>How long to wait for another query to the outbox events table after a query didn’t return any event.<br><br>Lower values will reduce the time it takes for a change to be reflected in the index, but will increase the stress on the database when there are no new events.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_POLLING_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `0.100S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-pulse-interval"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.pulse-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-pulse-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.pulse-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.pulse-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.pulse-interval+++[]<br><br>How long the event processor can poll for events before it must perform a "pulse", updating and checking registrations in the agents table.<br><br>The pulse interval must be set to a value between the polling interval and one third (1/3) of the expiration interval.<br><br>Low values (closer to the polling interval) mean less time wasted not processing events when a node joins or leaves the cluster, and reduced risk of wasting time not processing events because an event processor is incorrectly considered disconnected, but more stress on the database because of more frequent checks of the list of agents.<br><br>High values (closer to the expiration interval) mean more time wasted not processing events when a node joins or leaves the cluster, and increased risk of wasting time not processing events because an event processor is incorrectly considered disconnected, but less stress on the database because of less frequent checks of the list of agents.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_PULSE_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `2S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-pulse-expiration"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.pulse-expiration`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-pulse-expiration) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.pulse-expiration+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.pulse-expiration` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.pulse-expiration+++[]<br><br>How long an event processor "pulse" remains valid before considering the processor disconnected and forcibly removing it from the cluster.<br><br>The expiration interval must be set to a value at least 3 times larger than the pulse interval.<br><br>Low values (closer to the pulse interval) mean less time wasted not processing events when a node abruptly leaves the cluster due to a crash or network failure, but increased risk of wasting time not processing events because an event processor is incorrectly considered disconnected.<br><br>High values (much larger than the pulse interval) mean more time wasted not processing events when a node abruptly leaves the cluster due to a crash or network failure, but reduced risk of wasting time not processing events because an event processor is incorrectly considered disconnected.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_PULSE_EXPIRATION+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `30S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-batch-size"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.batch-size`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-batch-size) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.batch-size+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.batch-size` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.batch-size+++[]<br><br>How many outbox events, at most, are processed in a single transaction.<br><br>Higher values will reduce the number of transactions opened by the background process and may increase performance thanks to the first-level cache (persistence context), but will increase memory usage and in extreme cases may lead to ``OutOfMemoryError``s.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_BATCH_SIZE+++[] | int | `50` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-transaction-timeout"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.transaction-timeout`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-transaction-timeout) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.transaction-timeout+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.transaction-timeout` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.transaction-timeout+++[]<br><br>The timeout for transactions processing outbox events.<br><br>When this property is not set, Hibernate Search will use whatever default transaction timeout is configured in the JTA transaction manager, which may be too low for batch processing and lead to transaction timeouts when processing batches of events. If this happens, set a higher transaction timeout for event processing using this property.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_TRANSACTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) |  |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-retry-delay"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.retry-delay`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-event-processor-retry-delay) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".event-processor.retry-delay+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.retry-delay` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".event-processor.retry-delay+++[]<br><br>How long the event processor must wait before re-processing an event after its previous processing failed.<br><br>Use the value `0S` to reprocess failed events as soon as possible, with no delay.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-event-processor) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__EVENT_PROCESSOR_RETRY_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `30S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-mass-indexer-polling-interval"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".mass-indexer.polling-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-mass-indexer-polling-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".mass-indexer.polling-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".mass-indexer.polling-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".mass-indexer.polling-interval+++[]<br><br>How long to wait for another query to the agent table when actively waiting for event processors to suspend themselves.<br><br>Low values will reduce the time it takes for the mass indexer agent to detect that event processors finally suspended themselves, but will increase the stress on the database while the mass indexer agent is actively waiting.<br><br>High values will increase the time it takes for the mass indexer agent to detect that event processors finally suspended themselves, but will reduce the stress on the database while the mass indexer agent is actively waiting.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-mass-indexer) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__MASS_INDEXER_POLLING_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `0.100S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-mass-indexer-pulse-interval"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".mass-indexer.pulse-interval`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-mass-indexer-pulse-interval) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".mass-indexer.pulse-interval+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".mass-indexer.pulse-interval` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".mass-indexer.pulse-interval+++[]<br><br>How long the mass indexer can wait before it must perform a "pulse", updating and checking registrations in the agent table.<br><br>The pulse interval must be set to a value between the polling interval and one third (1/3) of the expiration interval.<br><br>Low values (closer to the polling interval) mean reduced risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected, but more stress on the database because of more frequent updates of the mass indexer agent's entry in the agent table.<br><br>High values (closer to the expiration interval) mean increased risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected, but less stress on the database because of less frequent updates of the mass indexer agent's entry in the agent table.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-mass-indexer) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__MASS_INDEXER_PULSE_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `2S` |
| <a id="quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-mass-indexer-pulse-expiration"></a> [`quarkus.hibernate-search-orm.coordination.tenants."tenant-id".mass-indexer.pulse-expiration`](#quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm-coordination-tenants-tenant-id-mass-indexer-pulse-expiration) config_property_copy_button:+++quarkus.hibernate-search-orm.coordination.tenants."tenant-id".mass-indexer.pulse-expiration+++[]<br><br>`quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".mass-indexer.pulse-expiration` config_property_copy_button:+++quarkus.hibernate-search-orm."persistence-unit-name".coordination.tenants."tenant-id".mass-indexer.pulse-expiration+++[]<br><br>How long an event processor "pulse" remains valid before considering the processor disconnected and forcibly removing it from the cluster.<br><br>The expiration interval must be set to a value at least 3 times larger than the pulse interval.<br><br>Low values (closer to the pulse interval) mean less time wasted with event processors not processing events when a mass indexer agent terminates due to a crash, but increased risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected.<br><br>High values (much larger than the pulse interval) mean more time wasted with event processors not processing events when a mass indexer agent terminates due to a crash, but reduced risk of event processors starting to process events again during mass indexing because a mass indexer agent is incorrectly considered disconnected.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#coordination-outbox-polling-mass-indexer) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_ORM_COORDINATION_TENANTS__TENANT_ID__MASS_INDEXER_PULSE_EXPIRATION+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm) | `30S` |

<a id="duration-note-anchor-quarkus-hibernate-search-orm-outbox-polling_quarkus-hibernate-search-orm"></a>

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
