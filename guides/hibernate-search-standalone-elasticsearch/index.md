# Use Hibernate Search in Standalone mode with Elasticsearch/OpenSearch

You have a Quarkus application? You want to provide a full-featured full-text search to your users? You're at the right place.

With this guide, you'll learn how to index entities into an Elasticsearch or OpenSearch cluster in a heartbeat with Hibernate Search. We will also explore how you can query your Elasticsearch or OpenSearch cluster using the Hibernate Search API.

> [!IMPORTANT]
> If you want to index Hibernate ORM entities, see [this dedicated guide](/guides/hibernate-search-orm-elasticsearch/index.md) instead.

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

The entities are stored and indexed in an Elasticsearch cluster.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `hibernate-search-standalone-elasticsearch-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-search-standalone-elasticsearch-quickstart).

> [!NOTE]
> The provided solution contains a few additional elements such as tests and testing infrastructure.

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:hibernate-search-standalone-elasticsearch-quickstart \
    --extension='hibernate-search-standalone-elasticsearch,rest-jackson' \
    --no-code
cd hibernate-search-standalone-elasticsearch-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=hibernate-search-standalone-elasticsearch-quickstart \
    -Dextensions='hibernate-search-standalone-elasticsearch,rest-jackson' \
    -DnoCode
cd hibernate-search-standalone-elasticsearch-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=hibernate-search-standalone-elasticsearch-quickstart"`

This command generates a Maven structure importing the following extensions:

\* Hibernate Search Standalone + Elasticsearch, \* Quarkus REST (formerly RESTEasy Reactive) and Jackson.

If you already have your Quarkus project configured, you can add the `hibernate-search-standalone-elasticsearch` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add hibernate-search-standalone-elasticsearch
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='hibernate-search-standalone-elasticsearch'
```

**Gradle**

```bash
./gradlew addExtension --extensions='hibernate-search-standalone-elasticsearch'
```

This will add the following to your `pom.xml`:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-search-standalone-elasticsearch</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-hibernate-search-standalone-elasticsearch")
```

## Creating the bare classes

First, let's create our `Book` and `Author` classes in the `model` subpackage.

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.List;
import java.util.Objects;

public class Author {

    public UUID id; // <1>

    public String firstName;

    public String lastName;

    public List<Book> books;

    public Author(UUID id, String firstName, String lastName, List<Book> books) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.books = books;
    }
}
```

1. We're using public fields here, because it's shorter and there is no expectation of encapsulation on what is essentially a data class.

   However, if you prefer using private fields with getters/setters, that's totally fine and will work perfectly as long as the getters/setters follow the JavaBeans naming convention (`getSomething()`/`isSomething()`/`setSomething(...)`).


```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.Objects;

public class Book {

    public UUID id;

    public String title;

    public Book(UUID id, String title) {
        this.id = id;
        this.title = title;
    }
}
```

## Using Hibernate Search annotations

Enabling full text search capabilities for our classes is as simple as adding a few annotations.

Let's edit the `Author` entity to include this content:

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity // <1>
@Indexed // <2>
public class Author {

    @DocumentId // <3>
    public UUID id;

    @FullTextField(analyzer = "name") // <4>
    @KeywordField(name = "firstName_sort", sortable = Sortable.YES, normalizer = "sort") // <5>
    public String firstName;

    @FullTextField(analyzer = "name")
    @KeywordField(name = "lastName_sort", sortable = Sortable.YES, normalizer = "sort")
    public String lastName;

    @IndexedEmbedded // <6>
    public List<Book> books = new ArrayList<>();

    public Author(UUID id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @ProjectionConstructor // <7>
    public Author(@IdProjection UUID id, String firstName, String lastName, List<Book> books) {
        this( id, firstName, lastName );
        this.books = books;
    }
}
```

1. First, let's mark the `Author` type as an [entity type](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#concepts-entity). In short, this implies the `Author` type it has its own, distinct lifecycle (not tied to another type), and that every \`BookAuthor instance carries an immutable, unique identifier.
2. Then, let's use the `@Indexed` annotation to register our `Author` entity as part of the full text index.
3. And let's end the mandatory configuration by defining a document identifier.
4. The `@FullTextField` annotation declares a field in the index specifically tailored for full text search. In particular, we have to define an analyzer to split and analyze the tokens (\~ words) - more on this later.
5. As you can see, we can define several fields for the same property. Here, we define a `@KeywordField` with a specific name. The main difference is that a keyword field is not tokenized (the string is kept as one single token) but can be normalized (i.e. filtered) - more on this later. This field is marked as sortable as our intention is to use it for sorting our authors.
6. The purpose of `@IndexedEmbedded` is to include the `Book` fields into the `Author` index. In this case, we just use the default configuration: all the fields of the associated `Book` instances are included in the index (i.e. the `title` field). `@IndexedEmbedded` also supports nested documents (using the `structure = NESTED` attribute), but we don't need it here. You can also specify the fields you want to embed in your parent index using the `includePaths`/`excludePaths` attributes if you don't want them all.
7. We mark a (single) constructor as a `@ProjectionConstructor`, so that an `Author` instance can be reconstructed from the content of the index.

Now that our authors are indexed, we will want to map books, so that this `@IndexedEmbedded` annotation actually embeds *something*.

Open the `Book` class and include the content below.

```java
package org.acme.hibernate.search.elasticsearch.model;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

@SearchEntity // <1>
public class Book {

    @KeywordField // <2>
    public UUID id;

    @FullTextField(analyzer = "english") // <3>
    public String title;

    @ProjectionConstructor // <4>
    public Book(UUID id, String title) {
        this.id = id;
        this.title = title;
    }
}
```

1. We also mark the Book type as an [entity type](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#concepts-entity), but we don't use `@Indexed`, because we decided we don't need a dedicated index for books.
2. We index the book's ID, so it can be projected (see below).
3. We use a `@FullTextField` similar to what we did for `Author` but you'll notice that the analyzer is different - more on this later.
4. Like `Author`, we mark a constructor as a `@ProjectionConstructor`, so that a `Book` instance can be reconstructed from the content of the index.

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

import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;

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

1. Annotate the configurer implementation with the `@SearchExtension` qualifier to tell Quarkus it should be used in Hibernate Search Standalone, for all Elasticsearch indexes (by default).

   The annotation can also target a specific persistence unit (`@SearchExtension(persistenceUnit = "nameOfYourPU")`), backend (`@SearchExtension(backend = "nameOfYourBackend")`), index (`@SearchExtension(index = "nameOfYourIndex")`), or a combination of those (`@SearchExtension(persistenceUnit = "nameOfYourPU", backend = "nameOfYourBackend", index = "nameOfYourIndex")`).

2. This is a simple analyzer separating the words on spaces, removing any non-ASCII characters by its ASCII counterpart (and thus removing accents) and putting everything in lowercase. It is used in our examples for the author's names.
3. We are a bit more aggressive with this one and we include some stemming: we will be able to search for `mystery` and get a result even if the indexed input contains `mysteries`. It is definitely too aggressive for person names, but it is perfect for the book titles.
4. Here is the normalizer used for sorting. Very similar to our first analyzer, except we don't tokenize the words as we want one and only one token.

For more information about configuring analyzers, see [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-analysis).

## Implementing the REST service

Create the `org.acme.hibernate.search.elasticsearch.LibraryResource` class:

```java
package org.acme.hibernate.search.elasticsearch;

import java.util.ArrayList;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.acme.hibernate.search.elasticsearch.model.Author;
import org.acme.hibernate.search.elasticsearch.model.Book;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

@Path("/library")
public class LibraryResource {

    @Inject
    SearchMapping searchMapping; // <1>

    @PUT
    @Path("author")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void addAuthor(@RestForm String firstName, @RestForm String lastName) {
        try (var searchSession = searchMapping.createSession()) { // <2>
            Author author = new Author(UUID.randomUUID(), firstName, lastName, new ArrayList<>());
            searchSession.indexingPlan().add(author); // <3>
        }
    }

    @GET
    @Path("author/{id}")
    public Author getAuthor(@RestPath UUID id) {
        try (var searchSession = searchMapping.createSession()) {
            return getAuthor(searchSession, id);
        }
    }

    private Author getAuthor(SearchSession searchSession, UUID id) {
        return searchSession.search(Author.class) // <4>
                .where(f -> f.id().matching(id))
                .fetchSingleHit()
                .orElseThrow(NotFoundException::new);
    }

    @POST
    @Path("author/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void updateAuthor(@RestPath UUID id, @RestForm String firstName, @RestForm String lastName) {
        try (var searchSession = searchMapping.createSession()) {
            Author author = getAuthor(searchSession, id); // <5>
            author.firstName = firstName;
            author.lastName = lastName;
            searchSession.indexingPlan().addOrUpdate(author); // <5>
        }
    }

    @DELETE
    @Path("author/{id}")
    public void deleteAuthor(@RestPath UUID id) {
        try (var searchSession = searchMapping.createSession()) {
            searchSession.indexingPlan().purge(Author.class, id, null); // <6>
        }
    }

    @PUT
    @Path("author/{authorId}/book/")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void addBook(@RestPath UUID authorId, @RestForm String title) {
        try (var searchSession = searchMapping.createSession()) {
            Author author = getAuthor(searchSession, authorId); // <7>
            author.books.add(new Book(authorId, title));
            searchSession.indexingPlan().addOrUpdate(author);
        }
    }

    @DELETE
    @Path("author/{authorId}/book/{bookId}")
    public void deleteBook(@RestPath UUID authorId, @RestPath UUID bookId) {
        try (var searchSession = searchMapping.createSession()) {
            Author author = getAuthor(searchSession, authorId); // <7>
            author.books.removeIf(book -> book.id.equals(bookId));
            searchSession.indexingPlan().addOrUpdate(author);
        }
    }
}
```

1. Inject a Hibernate Search mapping, the main entry point to Hibernate Search APIs.
2. Create a Hibernate Search session, which allows executing operations on the indexes.
3. To index a new Author, retrieve the session's indexing plan and call `add`, passing the author instance in argument.
4. To retrieve an Author from the index, execute a simple search -- more on search later -- by identifier.
5. To update an Author, retrieve it from the index, apply changes, retrieve the session's indexing plan and call `addOrUpdate`, passing the author instance in argument.
6. To delete an Author by identifier, retrieve the session's indexing plan and call `purge`, passing the author class and identifier in argument.
7. Since books are "owned" by authors (they are duplicated for each author and their lifecycle is bound to their author's), adding/deleting a book is simply an update to the author.

Nothing groundbreaking here: just a few CRUD operations in a REST service, using Hibernate Search APIs.

The interesting part comes with the addition of a search endpoint. In our `LibraryResource`, we just need to add the following method (and a few ``import``s):

```java
    @GET
    @Path("author/search")
    public List<Author> searchAuthors(@RestQuery String pattern, // <1>
            @RestQuery Optional<Integer> size) {
        try (var searchSession = searchMapping.createSession()) { // <2>
            return searchSession.search(Author.class) // <3>
                    .where(f -> pattern == null || pattern.isBlank()
                            ? f.matchAll() // <4>
                            : f.simpleQueryString()
                                    .fields("firstName", "lastName", "books.title").matching(pattern)) // <5>
                    .sort(f -> f.field("lastName_sort").then().field("firstName_sort")) // <6>
                    .fetchHits(size.orElse(20)); // <7>
        }
    }
```

1. Use the `org.jboss.resteasy.reactive.RestQuery` annotation type to avoid repeating the parameter name.
2. Create a Hibernate Search session, which allows executing operations on the indexes.
3. We indicate that we are searching for ``Author``s.
4. We create a predicate: if the pattern is empty, we use a `matchAll()` predicate.
5. If we have a valid pattern, we create a [`simpleQueryString()`](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html) predicate on the `firstName`, `lastName` and `books.title` fields matching our pattern.
6. We define the sort order of our results. Here we sort by last name, then by first name. Note that we use the specific fields we created for sorting.
7. Fetch the `size` top hits, `20` by default. Obviously, paging is also supported.

> [!NOTE]
> The Hibernate Search DSL supports a significant subset of the Elasticsearch predicates (match, range, nested, phrase, spatial...). Feel free to explore the DSL using autocompletion.
>
> When that's not enough, you can always fall back to [defining a predicate using JSON directly](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#search-dsl-predicate-extensions-elasticsearch-from-json).

## Automatic data initialization

For the purpose of this demonstration, let's import an initial dataset.

Let's add a few methods in `LibraryResource`:

```java
    void onStart(@Observes StartupEvent ev) { // <1>
        // Index some test data if nothing exists
        try (var searchSession = searchMapping.createSession()) {
            if (0 < searchSession.search(Author.class) // <2>
                    .where(f -> f.matchAll())
                    .fetchTotalHitCount()) {
                return;
            }
            for (Author author : initialDataSet()) { // <3>
                searchSession.indexingPlan().add(author); // <4>
            }
        }
    }

    private List<Author> initialDataSet() {
        return List.of(
                new Author(UUID.randomUUID(), "John", "Irving",
                        List.of(
                                new Book(UUID.randomUUID(), "The World According to Garp"),
                                new Book(UUID.randomUUID(), "The Hotel New Hampshire"),
                                new Book(UUID.randomUUID(), "The Cider House Rules"),
                                new Book(UUID.randomUUID(), "A Prayer for Owen Meany"),
                                new Book(UUID.randomUUID(), "Last Night in Twisted River"),
                                new Book(UUID.randomUUID(), "In One Person"),
                                new Book(UUID.randomUUID(), "Avenue of Mysteries"))),
                new Author(UUID.randomUUID(), "Paul", "Auster",
                        List.of(
                                new Book(UUID.randomUUID(), "The New York Trilogy"),
                                new Book(UUID.randomUUID(), "Mr. Vertigo"),
                                new Book(UUID.randomUUID(), "The Brooklyn Follies"),
                                new Book(UUID.randomUUID(), "Invisible"),
                                new Book(UUID.randomUUID(), "Sunset Park"),
                                new Book(UUID.randomUUID(), "4 3 2 1"))));
    }
```

1. Add a method that will get executed on application startup.
2. Check whether there already is data in the index -- if not, bail out.
3. Generate the initial dataset.
4. For each author, add it to the index.

## Configuring the application

As usual, we can configure everything in the Quarkus configuration file, `application.properties`.

Edit `src/main/resources/application.properties` and inject the following configuration:

```properties
quarkus.hibernate-search-standalone.mapping.structure=document <1>
quarkus.hibernate-search-standalone.elasticsearch.version=9 <2>
quarkus.hibernate-search-standalone.indexing.plan.synchronization.strategy=sync <3>

%prod.quarkus.hibernate-search-standalone.elasticsearch.hosts=localhost:9200 <4>
```

1. We need to tell Hibernate Search about the structure of our entities.

   In this application we consider an indexed entity (the author) is the root of a "document": the author "owns" books it references through associations, which **cannot** be updated independently of the author.

   See [`quarkus.hibernate-search-standalone.mapping.structure`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-mapping-structure) for other options and more details.

2. We need to tell Hibernate Search about the version of Elasticsearch we will use.

   It is important because there are significant differences between Elasticsearch mapping syntax depending on the version. Since the mapping is created at build time to reduce startup time, Hibernate Search cannot connect to the cluster to automatically detect the version. Note that, for OpenSearch, you need to prefix the version with `opensearch:`; see [OpenSearch compatibility](#opensearch).

3. This means that we wait for the entities to be searchable before considering a write complete. On a production setup, the `write-sync` default will provide better performance. Using `sync` is especially important when testing as you need the entities to be searchable immediately.
4. For development and tests, we rely on [Dev Services](#dev-services), which means Quarkus will start an Elasticsearch cluster automatically. In production mode, however, we will want to start an Elasticsearch cluster manually, which is why we provide Quarkus with this connection info in the `prod` profile (`%prod.` prefix).

> [!NOTE]
> Because we rely on [Dev Services](#dev-services), the Elasticsearch schema will automatically be dropped and re-created on each application startup in tests and dev mode (unless [`quarkus.hibernate-search-standalone.schema-management.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-schema-management-strategy) is set explicitly).
>
> If for some reason you cannot use Dev Services, you will have to set the following properties to get similar behavior:
>
> ```properties
> %dev,test.quarkus.hibernate-search-standalone.schema-management.strategy=drop-and-create
> ```
>
> See also [`quarkus.hibernate-search-standalone.schema-management.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-schema-management-strategy).

> [!TIP]
> For more information about configuration of the Hibernate Search Standalone extension, refer to the [Configuration Reference](#configuration-reference).

## Creating a frontend

Now let's add a simple web page to interact with our `LibraryResource`. Quarkus automatically serves static resources located under the `META-INF/resources` directory. In the `src/main/resources/META-INF/resources` directory, overwrite the existing `index.html` file with the content from this [index.html](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-search-standalone-elasticsearch-quickstart/src/main/resources/META-INF/resources/index.html) file.

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

Running it is as simple as executing `./target/hibernate-search-standalone-elasticsearch-quickstart-1.0.0-SNAPSHOT-runner`.

You can then point your browser to `http://localhost:8080/` and use your application.

> [!NOTE]
> The startup is a bit slower than usual: it is mostly due to us dropping and recreating the Elasticsearch mapping every time at startup. We also index some initial data.
>
> In a real life application, it is obviously something you won't do on every startup.

<a id="dev-services"></a>
### Dev Services (Configuration Free Datastores)

Quarkus supports a feature called [Dev Services](/guides/dev-services/index.md) that allows you to start various containers without any config.

In the case of Elasticsearch this support extends to the default Elasticsearch connection. What that means practically, is that if you have not configured `quarkus.hibernate-search-standalone.elasticsearch.hosts`, Quarkus will automatically start an Elasticsearch container when running tests or in [dev mode](/guides/dev-mode-differences/index.md), and automatically configure the connection.

When running the production version of the application, the Elasticsearch connection needs to be configured as normal, so if you want to include a production database config in your `application.properties` and continue to use Dev Services we recommend that you use the `%prod.` profile to define your Elasticsearch settings.

> [!NOTE]
> Dev Services for Elasticsearch is currently unable to start multiple clusters concurrently, so it only works with the default backend of the default persistence unit: named persistence units or named backends won't be able to take advantage of Dev Services for Elasticsearch.

For more information you can read the [Dev Services for Elasticsearch guide](/guides/elasticsearch-dev-services/index.md).

<a id="programmatic-mapping"></a>
## Programmatic mapping

If, for some reason, adding Hibernate Search annotations to entities is not possible, mapping can be applied programmatically instead. Programmatic mapping is configured through the `ProgrammaticMappingConfigurationContext` that is exposed via a mapping configurer (`HibernateOrmSearchMappingConfigurer`).

> [!NOTE]
> A mapping configurer (`StandalonePojoMappingConfigurer`) allows much more than just programmatic mapping capabilities. It also allows [configuring annotation mapping, bridges, and more](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapper-pojo-standalone-mapping-configurer).

Below is an example of a mapping configurer that applies programmatic mapping:

```java
package org.acme.hibernate.search.elasticsearch.config;

import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;

@SearchExtension // <1>
public class CustomMappingConfigurer implements StandalonePojoMappingConfigurer {

	@Override
    public void configure(StandalonePojoMappingConfigurationContext context) {
        TypeMappingStep type = context.programmaticMapping()    // <2>
                .type(SomeIndexedEntity.class);                 // <3>
        type.searchEntity();                                    // <4>
        type.indexed()                                          // <5>
                .index(SomeIndexedEntity.INDEX_NAME);           // <6>
        type.property("id").documentId();                       // <7>
        type.property("text").fullTextField();                  // <8>
    }
}
```

1. Annotate the configurer implementation with the `@SearchExtension` qualifier to tell Quarkus it should be used by Hibernate Search Standalone.
2. Access the programmatic mapping context.
3. Create mapping step for the `SomeIndexedEntity` type.
4. Define `SomeIndexedEntity` as an entity type for Hibernate Search.
5. Define the `SomeIndexedEntity` entity as indexed.
6. Provide an index name to be used for the `SomeIndexedEntity` entity.
7. Define the document id property.
8. Define a full-text search field for the `text` property.

<a id="opensearch"></a>
## OpenSearch compatibility

Hibernate Search is compatible with both [Elasticsearch](https://www.elastic.co/elasticsearch) and [OpenSearch](https://www.opensearch.org/), but it assumes it is working with an Elasticsearch cluster by default.

To have Hibernate Search work with an OpenSearch cluster instead, [prefix the configured version with `opensearch:`](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-configuration-version), as shown below.

```properties
quarkus.hibernate-search-standalone.elasticsearch.version=opensearch:3.5
```

All other configuration options and APIs are exactly the same as with Elasticsearch.

You can find more information about compatible distributions and versions of Elasticsearch in [this section of Hibernate Search's reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#compatibility).

<a id="cdi"></a>
## CDI integration

<a id="injecting-entry-points"></a>
### Injecting entry points

You can inject Hibernate Search's main entry point, `SearchMapping`, using CDI:

```java
@Inject
SearchMapping searchMapping;
```

<a id="plugging-in-custom-components"></a>
### Plugging in custom components

The Quarkus extension for Hibernate Search Standalone will automatically inject components annotated with `@SearchExtension` into Hibernate Search.

The annotation can optionally target a specific backend (`@SearchExtension(backend = "nameOfYourBackend")`), index (`@SearchExtension(index = "nameOfYourIndex")`), or a combination of those (`@SearchExtension(backend = "nameOfYourBackend", index = "nameOfYourIndex")`), when it makes sense for the type of the component being injected.

This feature is available for the following component types:

**`org.hibernate.search.engine.reporting.FailureHandler`**\
A component that should be notified of any failure occurring in a background process (mainly index operations). + Scope: one per application. + See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#configuration-background-failure-handling) for more information.

**`org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer`**\
A component used to configure the Hibernate Search mapping, in particular programmatically. + Scope: one or more per persistence unit. + See [this section of this guide](#programmatic-mapping) for more information.

**`org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy`**\
A component used to configure how to synchronize between application threads and indexing. + Scope: one per application. + Can also be set to built-in implementations through [`quarkus.hibernate-search-standalone.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-indexing-plan-synchronization-strategy). + See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#indexing-plan-synchronization) for more information.

**`org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer`**\
A component used to configure full text analysis (e.g. analyzers, normalizers). + Scope: one or more per backend. + See [this section of this guide](#analysis-configurer) for more information.

**`org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy`**\
A component used to configure the Elasticsearch layout: index names, index aliases, ... + Scope: one per backend. + Can also be set to built-in implementations through [`quarkus.hibernate-search-standalone.elasticsearch.layout.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-layout-strategy). + See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-indexlayout) for more information.

<a id="offline-startup"></a>
## Offline startup

By default, Hibernate Search sends a few requests to the Elasticsearch cluster on startup. If the Elasticsearch cluster is not necessarily up and running when Hibernate Search starts, this could cause a startup failure.

To address this, you can configure Hibernate Search to not send any request on startup:

- Disable Elasticsearch version checks on startup by setting the configuration property [`quarkus.hibernate-search-standalone.elasticsearch.version-check.enabled`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-version-check-enabled)  to `false`.
- Disable schema management on startup by setting the configuration property [`quarkus.hibernate-search-standalone.schema-management.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-schema-management-strategy)  to `none`.

Of course, even with this configuration, Hibernate Search still won't be able to index anything or run search queries until the Elasticsearch cluster becomes accessible.

> [!IMPORTANT]
> If you disable automatic schema creation by setting `quarkus.hibernate-search-standalone.schema-management.strategy` to `none`, you will have to create the schema manually at some point before your application starts persisting/updating entities and executing search requests.
>
> See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#schema-management-manager) for more information.

<a id="loading"></a>
## Loading

As an alternative to using Elasticsearch as a primary datastore, this extension can also be used to index entities coming from another datastore.

> [!TIP]
> In such a scenario, you will need to set [`quarkus.hibernate-search-standalone.mapping.structure`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-mapping-structure) to a value matching the structure of the primary datastore.

In order to do this, entities need to be loaded from that other datastore, and such loading must be implemented explicitly.

You can refer to Hibernate Search's reference documentation for more information about configuring loading:

- To load entities from an external datasource in order to reindex them, see [Mass loading strategy](https://docs.hibernate.org/stable/search/reference/en-US/html_single/#mapping-entitydefinition-loading-mass).
- To load entities from an external datasource when returning search hits, see [Selection loading strategy](https://docs.hibernate.org/stable/search/reference/en-US/html_single/#mapping-entitydefinition-loading-selection).

> [!NOTE]
> In Quarkus, the entity loader mentioned in Hibernate Search's reference documentation can be defined as a [CDI bean](#plugging-in-custom-components), but will still need to be attached to particular entities using `@SearchEntity(loadingBinder = ...)`.

<a id="management"></a>
## Management endpoint

> [!CAUTION]
> Hibernate Search's management endpoint is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, or even storage formats, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).

The Hibernate Search extension provides an HTTP endpoint to reindex your data through the [management interface](/guides/management-interface-reference/index.md). By default, this endpoint is not available. It can be enabled through configuration properties as shown below.

```properties
quarkus.management.enabled=true <1>
quarkus.hibernate-search-standalone.management.enabled=true <2>
```

1. Enable the [management interface](/guides/management-interface-reference/index.md).
2. Enable Hibernate Search Standalone specific management endpoints.

Once the management endpoints are enabled, data can be re-indexed via `/q/hibernate-search/standalone/reindex`, where `/q` is the default management root path and `/hibernate-search/standalone/` is the default Hibernate Search root management path. It (`/hibernate-search/standalone/`) can be changed via configuration property as shown below.

```properties
quarkus.hibernate-search-standalone.management.root-path=custom-root-path <1>
```

1. Use a custom `custom-root-path` path for Hibernate Search's management endpoint. If the default management root path is used then the reindex path becomes `/q/custom-root-path/reindex`.

This endpoint accepts `POST` requests with `application/json` content type only. All indexed entities will be re-indexed if an empty request body is submitted.

> [!NOTE]
> In order to reindex an entity type, it needs to be [configured for loading from an external source](#loading).
>
> Without that configuration, reindexing through the management endpoint (or through any other API) will fail.

If only a subset of entities must be re-indexed or if there is a need to have a custom configuration of the underlying mass indexer then this information can be passed through the request body as shown below.

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

<a id="limitations"></a>
## Limitations

- The Hibernate Search Standalone extension cannot be used in the same application as the [Hibernate Search extension with Hibernate ORM](/guides/hibernate-search-orm-elasticsearch/index.md)

  See [#39517](https://github.com/quarkusio/quarkus/issues/39517) to track progress.

- AWS request signing is not available at the moment, unlike in the [Hibernate Search extension with Hibernate ORM](/guides/hibernate-search-orm-elasticsearch/index.md#aws-request-signing)

  See [#26991](https://github.com/quarkusio/quarkus/issues/26991) to track progress.

- Optimistic concurrency control is not available at the moment.

  See [HSEARCH-5105](https://hibernate.atlassian.net/browse/HSEARCH-5105) to track progress.

- Elasticsearch/OpenSearch do not support transactions, so multi-document updates may fail partially and leave the index in an inconsistent state.

  This cannot be avoided like in the [Hibernate Search extension with Hibernate ORM](/guides/hibernate-search-orm-elasticsearch/index.md) with [coordination through outbox polling](/guides/hibernate-search-orm-elasticsearch/index.md#coordination), because that coordination requires Hibernate ORM and relies on the fact that data is derived from that of a (transactional) relational database.


## Further reading

If you are interested in learning more about Hibernate Search, the Hibernate team publishes [an extensive reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/), as well as a page listing [other relevant resources](https://hibernate.org/search/more-resources/).

## FAQ

### Why Elasticsearch only?

Hibernate Search supports both a Lucene backend and an Elasticsearch backend.

In the context of Quarkus and to build scalable applications, we thought the latter would make more sense. Thus, we focused our efforts on it.

We don't have plans to support the Lucene backend in Quarkus for now, though there is an issue tracking progress on such an implementation in the Quarkiverse: [quarkiverse/quarkus-hibernate-search-extras#180](https://github.com/quarkiverse/quarkus-hibernate-search-extras/issues/180).

<a id="configuration-reference"></a>
## Configuration Reference for Hibernate Search Standalone

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-enabled"></a> [`quarkus.hibernate-search-standalone.enabled`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-enabled) config_property_copy_button:+++quarkus.hibernate-search-standalone.enabled+++[]<br><br>Whether Hibernate Search Standalone is enabled **during the build**.<br><br>If Hibernate Search is disabled during the build, all processing related to Hibernate Search will be skipped, but it will not be possible to activate Hibernate Search at runtime: `quarkus.hibernate-search-standalone.active` will default to `false` and setting it to `true` will lead to an error.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-background-failure-handler"></a> [`quarkus.hibernate-search-standalone.background-failure-handler`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-background-failure-handler) config_property_copy_button:+++quarkus.hibernate-search-standalone.background-failure-handler+++[]<br><br>A [bean reference](/guides/hibernate-search-standalone-elasticsearch/index.md#bean-reference-note-anchor) to a component that should be notified of any failure occurring in a background process (mainly index operations).<br><br>The referenced bean must implement `FailureHandler`.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#configuration-background-failure-handling) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `FailureHandler` implementation with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](/guides/hibernate-search-standalone-elasticsearch/index.md#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_BACKGROUND_FAILURE_HANDLER+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-mapping-configurer"></a> [`quarkus.hibernate-search-standalone.mapping.configurer`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-mapping-configurer) config_property_copy_button:+++quarkus.hibernate-search-standalone.mapping.configurer+++[]<br><br>One or more [bean references](/guides/hibernate-search-standalone-elasticsearch/index.md#bean-reference-note-anchor) to the component(s) used to configure the Hibernate Search mapping, in particular programmatically.<br><br>The referenced beans must implement `StandalonePojoMappingConfigurer`.<br><br>See [Programmatic mapping](/guides/hibernate-search-standalone-elasticsearch/index.md#programmatic-mapping) for an example on how mapping configurers can be used to apply programmatic mappings.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `StandalonePojoMappingConfigurer` implementations with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](/guides/hibernate-search-standalone-elasticsearch/index.md#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_MAPPING_CONFIGURER+++[] | list of string |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-mapping-structure"></a> [`quarkus.hibernate-search-standalone.mapping.structure`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-mapping-structure) config_property_copy_button:+++quarkus.hibernate-search-standalone.mapping.structure+++[]<br><br>The structure of the Hibernate Search entity mapping.<br><br>This must match the structure of the application model being indexed with Hibernate Search:<br><br>**`graph` (default)**\<br>Entities indexed through Hibernate Search are nodes in an entity graph, i.e. an indexed entity is independent of other entities it references through associations, which **can** be updated independently of the indexed entity. + Associations between entities must be bi-directional: specifying the inverse side of associations through `@AssociationInverseSide` **is required**, unless reindexing is disabled for that association through `@IndexingDependency(reindexOnUpdate = ...)`.<br><br>**`document`**\<br>Entities indexed through Hibernate Search are the root of a document, i.e. an indexed entity "owns" other entities it references through associations, which **cannot** be updated independently of the indexed entity. + Associations between entities can be uni-directional: specifying the inverse side of associations through `@AssociationInverseSide` **is not required**.<br><br>See also [`@AssociationInverseSide`](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-associationinverseside) [`@IndexingDependency(reindexOnUpdate = ...)`](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-reindexonupdate).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_MAPPING_STRUCTURE+++[] | tooltip:graph[Entities indexed through Hibernate Search are nodes in an entity graph.  With this structure\: * An indexed entity is independent of other entities it references through associations,opts=which *can* be updated independently of the indexed entity; in particular they may be passed to {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}. * Therefore,when an entity changes,Hibernate Search may need to resolve other entities to reindex,which means in particular that associations between entities must be bi-directional\: specifying the inverse side of associations through `@AssociationInverseSide` *is required*,unless reindexing is disabled for that association through `@IndexingDependency(reindexOnUpdate = ...)`.  See also link\:https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-associationinverseside\\]`@AssociationInverseSide`\] link\:https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-reindexonupdate\\]`@IndexingDependency(reindexOnUpdate = ...)`\].], tooltip:document[Entities indexed through Hibernate Search are the root of a document.  With this structure\: * An indexed entity "owns" other entities it references through associations,opts=which *cannot* be updated independently of the indexed entity; in particular they cannot be passed to {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}. * Therefore,when an entity changes,Hibernate Search doesn't need to resolve other entities to reindex,which means in particular that associations between entities can be uni-directional\: specifying the inverse side of associations through `@AssociationInverseSide` *is not required*.  See also link\:https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-associationinverseside\\]`@AssociationInverseSide`\].] | tooltip:graph[Entities indexed through Hibernate Search are nodes in an entity graph.  With this structure\: * An indexed entity is independent of other entities it references through associations,opts=which *can* be updated independently of the indexed entity; in particular they may be passed to {@link org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan#addOrUpdate(Object)}. * Therefore,when an entity changes,Hibernate Search may need to resolve other entities to reindex,which means in particular that associations between entities must be bi-directional\: specifying the inverse side of associations through `@AssociationInverseSide` *is required*,unless reindexing is disabled for that association through `@IndexingDependency(reindexOnUpdate = ...)`.  See also link\:https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-associationinverseside\\]`@AssociationInverseSide`\] link\:https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#mapping-reindexing-reindexonupdate\\]`@IndexingDependency(reindexOnUpdate = ...)`\].] |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-active"></a> [`quarkus.hibernate-search-standalone.active`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-active) config_property_copy_button:+++quarkus.hibernate-search-standalone.active+++[]<br><br>Whether Hibernate Search Standalone should be active at runtime.<br><br>If Hibernate Search Standalone is not active, it won't start with the application, and accessing the SearchMapping for search or other operations will not be possible.<br><br>Note that if Hibernate Search Standalone is disabled (i.e. `quarkus.hibernate-search-standalone.enabled` is set to `false`), it won't be active, and setting this property to `true` will fail.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ACTIVE+++[] | boolean | `'true' if Hibernate Search Standalone is enabled; 'false' otherwise` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-schema-management-strategy"></a> [`quarkus.hibernate-search-standalone.schema-management.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-schema-management-strategy) config_property_copy_button:+++quarkus.hibernate-search-standalone.schema-management.strategy+++[]<br><br>The schema management strategy, controlling how indexes and their schema are created, updated, validated or dropped on startup and shutdown.<br><br>Available values:<br><br>!=== h!Strategy h!Definition<br><br>!none !Do nothing: assume that indexes already exist and that their schema matches Hibernate Search's expectations.<br><br>!validate !Validate that indexes exist and that their schema matches Hibernate Search's expectations.<br><br>If it does not, throw an exception, but make no attempt to fix the problem.<br><br>!create !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, do nothing: assume that their schema matches Hibernate Search's expectations.<br><br>!create-or-validate (**default** unless using Dev Services) !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, validate that their schema matches Hibernate Search's expectations.<br><br>If it does not, throw an exception, but make no attempt to fix the problem.<br><br>!create-or-update !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, validate that their schema matches Hibernate Search's expectations; if it does not match expectations, try to update it.<br><br>**This strategy is unfit for production environments**, due to several important limitations, but can be useful when developing.<br><br>!drop-and-create !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, drop them, then create them along with their schema.<br><br>!drop-and-create-and-drop (**default** when using Dev Services) !For indexes that do not exist, create them along with their schema.<br><br>For indexes that already exist, drop them, then create them along with their schema.<br><br>Also, drop indexes and their schema on shutdown. !===<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#schema-management-strategy) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_SCHEMA_MANAGEMENT_STRATEGY+++[] | `none`, `validate`, `create`, `create-or-validate`, `create-or-update`, `drop-and-create`, `drop-and-create-and-drop` | `drop-and-create-and-drop when using Dev Services; create-or-validate otherwise` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-indexing-plan-synchronization-strategy"></a> [`quarkus.hibernate-search-standalone.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-indexing-plan-synchronization-strategy) config_property_copy_button:+++quarkus.hibernate-search-standalone.indexing.plan.synchronization.strategy+++[]<br><br>How to synchronize between application threads and indexing, in particular when relying on (implicit) listener-triggered indexing on entity change, but also when using a `SearchIndexingPlan` explicitly.<br><br>Defines how complete indexing should be before resuming the application thread after a `SearchSession` is closed.<br><br>Available values:<br><br>!=== .2+h!Strategy .2+h!Throughput 3+^h!Guarantees when the application thread resumes<br><br>h!Changes applied h!Changes safe from crash/power loss h!Changes visible on search<br><br>!async !Best ^![times] ^![times] ^![times]<br><br>!write-sync (**default**) !Medium ^![check] ^![check] ^![times]<br><br>!read-sync !Medium to worst ^![check] ^![times] ^![check]<br><br>!sync !Worst ^![check] ^![check] ^![check] !===<br><br>This property also accepts a [bean reference](/guides/hibernate-search-orm-elasticsearch/index.md#bean-reference-note-anchor) to a custom implementations of `IndexingPlanSynchronizationStrategy`.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#indexing-plan-synchronization) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `IndexingPlanSynchronizationStrategy` implementation with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_INDEXING_PLAN_SYNCHRONIZATION_STRATEGY+++[] | string | `write-sync` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_section_quarkus-hibernate-search-standalone-elasticsearch"></a> [Configuration for Elasticsearch/OpenSearch backends](#quarkus-hibernate-search-standalone-elasticsearch_section_quarkus-hibernate-search-standalone-elasticsearch) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-version"></a> [`quarkus.hibernate-search-standalone.elasticsearch.version`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-version) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.version+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".version` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".version+++[]<br><br>The version of Elasticsearch used in the cluster.<br><br>As the schema is generated without a connection to the server, this item is mandatory.<br><br>It doesn't have to be the exact version (it can be `7` or `7.1` for instance) but it has to be sufficiently precise to choose a model dialect (the one used to generate the schema) compatible with the protocol dialect (the one used to communicate with Elasticsearch).<br><br>There's no rule of thumb here as it depends on the schema incompatibilities introduced by Elasticsearch versions. In any case, if there is a problem, you will have an error when Hibernate Search tries to connect to the cluster.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_VERSION+++[] | ElasticsearchVersion |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-settings-file"></a> [`quarkus.hibernate-search-standalone.elasticsearch.schema-management.settings-file`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-settings-file) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.settings-file+++[]<br><br>Path to a file in the classpath holding custom index settings to be included in the index definition when creating an Elasticsearch index.<br><br>The provided settings will be merged with those generated by Hibernate Search, including analyzer definitions. When analysis is configured both through an analysis configurer and these custom settings, the behavior is undefined; it should not be relied upon.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-configuration-index-settings) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_SCHEMA_MANAGEMENT_SETTINGS_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-mapping-file"></a> [`quarkus.hibernate-search-standalone.elasticsearch.schema-management.mapping-file`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-mapping-file) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.mapping-file+++[]<br><br>Path to a file in the classpath holding a custom index mapping to be included in the index definition when creating an Elasticsearch index.<br><br>The file does not need to (and generally shouldn't) contain the full mapping: Hibernate Search will automatically inject missing properties (index fields) in the given mapping.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-mapping-custom) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_SCHEMA_MANAGEMENT_MAPPING_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-analysis-configurer"></a> [`quarkus.hibernate-search-standalone.elasticsearch.analysis.configurer`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-analysis-configurer) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.analysis.configurer+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".analysis.configurer+++[]<br><br>One or more [bean references](#bean-reference-note-anchor) to the component(s) used to configure full text analysis (e.g. analyzers, normalizers).<br><br>The referenced beans must implement `ElasticsearchAnalysisConfigurer`.<br><br>See [Setting up the analyzers](#analysis-configurer) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `ElasticsearchAnalysisConfigurer` implementations with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_ANALYSIS_CONFIGURER+++[] | list of string |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-hosts"></a> [`quarkus.hibernate-search-standalone.elasticsearch.hosts`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-hosts) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.hosts+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".hosts` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".hosts+++[]<br><br>The list of hosts of the Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_HOSTS+++[] | list of string | `localhost:9200` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-protocol"></a> [`quarkus.hibernate-search-standalone.elasticsearch.protocol`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-protocol) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.protocol+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".protocol` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".protocol+++[]<br><br>The protocol to use when contacting Elasticsearch servers. Set to "https" to enable SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_PROTOCOL+++[] | tooltip:http[Use clear-text HTTP,opts=with SSL/TLS disabled.], tooltip:https[Use HTTPS,opts=with SSL/TLS enabled.] | tooltip:http[Use clear-text HTTP,opts=with SSL/TLS disabled.] |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-username"></a> [`quarkus.hibernate-search-standalone.elasticsearch.username`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-username) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.username+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".username` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".username+++[]<br><br>The username used for authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_USERNAME+++[] | string |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-password"></a> [`quarkus.hibernate-search-standalone.elasticsearch.password`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-password) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.password+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".password` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".password+++[]<br><br>The password used for authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_PASSWORD+++[] | string |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-connection-timeout"></a> [`quarkus.hibernate-search-standalone.elasticsearch.connection-timeout`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-connection-timeout) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.connection-timeout+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".connection-timeout` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".connection-timeout+++[]<br><br>The timeout when establishing a connection to an Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone) | `1S` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-read-timeout"></a> [`quarkus.hibernate-search-standalone.elasticsearch.read-timeout`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-read-timeout) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.read-timeout+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".read-timeout` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".read-timeout+++[]<br><br>The timeout when reading responses from an Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone) | `30S` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-request-timeout"></a> [`quarkus.hibernate-search-standalone.elasticsearch.request-timeout`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-request-timeout) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.request-timeout+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".request-timeout` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".request-timeout+++[]<br><br>The timeout when executing a request to an Elasticsearch server.<br><br>This includes the time needed to wait for a connection to be available, send the request and read the response.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_REQUEST_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone) |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-max-connections"></a> [`quarkus.hibernate-search-standalone.elasticsearch.max-connections`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-max-connections) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.max-connections+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".max-connections` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".max-connections+++[]<br><br>The maximum number of connections to all the Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_MAX_CONNECTIONS+++[] | int | `40` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-max-connections-per-route"></a> [`quarkus.hibernate-search-standalone.elasticsearch.max-connections-per-route`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-max-connections-per-route) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.max-connections-per-route+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".max-connections-per-route` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".max-connections-per-route+++[]<br><br>The maximum number of connections per Elasticsearch server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_MAX_CONNECTIONS_PER_ROUTE+++[] | int | `20` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-discovery-enabled"></a> [`quarkus.hibernate-search-standalone.elasticsearch.discovery.enabled`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-discovery-enabled) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.discovery.enabled+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".discovery.enabled` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".discovery.enabled+++[]<br><br>Defines if automatic discovery is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_DISCOVERY_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-discovery-refresh-interval"></a> [`quarkus.hibernate-search-standalone.elasticsearch.discovery.refresh-interval`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-discovery-refresh-interval) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.discovery.refresh-interval+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".discovery.refresh-interval` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".discovery.refresh-interval+++[]<br><br>Refresh interval of the node list.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_DISCOVERY_REFRESH_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone) | `10S` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-thread-pool-size"></a> [`quarkus.hibernate-search-standalone.elasticsearch.thread-pool.size`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-thread-pool-size) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.thread-pool.size+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".thread-pool.size` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".thread-pool.size+++[]<br><br>The size of the thread pool assigned to the backend.<br><br>Note that number is **per backend**, not per index. Adding more indexes will not add more threads.<br><br>As all operations happening in this thread-pool are non-blocking, raising its size above the number of processor cores available to the JVM will not bring noticeable performance benefit. The only reason to alter this setting would be to reduce the number of threads; for example, in an application with a single index with a single indexing queue, running on a machine with 64 processor cores, you might want to bring down the number of threads.<br><br>Defaults to the number of processor cores available to the JVM on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_THREAD_POOL_SIZE+++[] | int |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-query-shard-failure-ignore"></a> [`quarkus.hibernate-search-standalone.elasticsearch.query.shard-failure.ignore`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-query-shard-failure-ignore) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.query.shard-failure.ignore+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".query.shard-failure.ignore` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".query.shard-failure.ignore+++[]<br><br>Whether partial shard failures are ignored (`true`) or lead to Hibernate Search throwing an exception (`false`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_QUERY_SHARD_FAILURE_IGNORE+++[] | boolean | `false` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-version-check-enabled"></a> [`quarkus.hibernate-search-standalone.elasticsearch.version-check.enabled`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-version-check-enabled) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.version-check.enabled+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".version-check.enabled` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".version-check.enabled+++[]<br><br>Whether Hibernate Search should check the version of the Elasticsearch cluster on startup.<br><br>Set to `false` if the Elasticsearch cluster may not be available on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_VERSION_CHECK_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-required-status"></a> [`quarkus.hibernate-search-standalone.elasticsearch.schema-management.required-status`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-required-status) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.required-status+++[]<br><br>The minimal [https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html\[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html[Elasticsearch) cluster status\] required on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_SCHEMA_MANAGEMENT_REQUIRED_STATUS+++[] | `green`, `yellow`, `red` | `yellow` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-required-status-wait-timeout"></a> [`quarkus.hibernate-search-standalone.elasticsearch.schema-management.required-status-wait-timeout`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-schema-management-required-status-wait-timeout) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".schema-management.required-status-wait-timeout+++[]<br><br>How long we should wait for the status before failing the bootstrap.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_SCHEMA_MANAGEMENT_REQUIRED_STATUS_WAIT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone) | `10S` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexing-queue-count"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexing.queue-count`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexing-queue-count) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexing.queue-count+++[]<br><br>The number of indexing queues assigned to each index.<br><br>Higher values will lead to more connections being used in parallel, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXING_QUEUE_COUNT+++[] | int | `10` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexing-queue-size"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexing.queue-size`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexing-queue-size) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexing.queue-size+++[]<br><br>The size of indexing queues.<br><br>Lower values may lead to lower memory usage, especially if there are many queues, but values that are too low will reduce the likeliness of reaching the max bulk size and increase the likeliness of application threads blocking because the queue is full, which may lead to lower indexing throughput.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXING_QUEUE_SIZE+++[] | int | `1000` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexing-max-bulk-size"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexing.max-bulk-size`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexing-max-bulk-size) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexing.max-bulk-size+++[]<br><br>The maximum size of bulk requests created when processing indexing queues.<br><br>Higher values will lead to more documents being sent in each HTTP request sent to Elasticsearch, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Note that raising this number above the queue size has no effect, as bulks cannot include more requests than are contained in the queue.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXING_MAX_BULK_SIZE+++[] | int | `100` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-layout-strategy"></a> [`quarkus.hibernate-search-standalone.elasticsearch.layout.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-layout-strategy) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.layout.strategy+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".layout.strategy` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".layout.strategy+++[]<br><br>A [bean reference](#bean-reference-note-anchor) to the component used to configure the Elasticsearch layout: index names, index aliases, ...<br><br>The referenced bean must implement `IndexLayoutStrategy`.<br><br>Available built-in implementations:<br><br>**`simple`**\<br>The default, future-proof strategy: if the index name in Hibernate Search is `myIndex`, this strategy will create an index named `myindex-000001`, an alias for write operations named `myindex-write`, and an alias for read operations named `myindex-read`.<br><br>**`no-alias`**\<br>A strategy without index aliases, mostly useful on legacy clusters: if the index name in Hibernate Search is `myIndex`, this strategy will create an index named `myindex`, and will not use any alias.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-indexlayout) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `IndexLayoutStrategy` implementation with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_LAYOUT_STRATEGY+++[] | string |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_section_quarkus-hibernate-search-standalone-elasticsearch-indexes"></a> [Per-index configuration overrides](#quarkus-hibernate-search-standalone-elasticsearch_section_quarkus-hibernate-search-standalone-elasticsearch-indexes) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-settings-file"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.settings-file`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-settings-file) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.settings-file+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.settings-file` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.settings-file+++[]<br><br>Path to a file in the classpath holding custom index settings to be included in the index definition when creating an Elasticsearch index.<br><br>The provided settings will be merged with those generated by Hibernate Search, including analyzer definitions. When analysis is configured both through an analysis configurer and these custom settings, the behavior is undefined; it should not be relied upon.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-configuration-index-settings) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_SETTINGS_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-mapping-file"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.mapping-file`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-mapping-file) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.mapping-file+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.mapping-file` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.mapping-file+++[]<br><br>Path to a file in the classpath holding a custom index mapping to be included in the index definition when creating an Elasticsearch index.<br><br>The file does not need to (and generally shouldn't) contain the full mapping: Hibernate Search will automatically inject missing properties (index fields) in the given mapping.<br><br>See [this section of the reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#backend-elasticsearch-mapping-custom) for more information.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_MAPPING_FILE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-analysis-configurer"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".analysis.configurer`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-analysis-configurer) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".analysis.configurer+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".analysis.configurer` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".analysis.configurer+++[]<br><br>One or more [bean references](#bean-reference-note-anchor) to the component(s) used to configure full text analysis (e.g. analyzers, normalizers).<br><br>The referenced beans must implement `ElasticsearchAnalysisConfigurer`.<br><br>See [Setting up the analyzers](#analysis-configurer) for more information.<br><br>> [!NOTE]<br>> Instead of setting this configuration property, you can simply annotate your custom `ElasticsearchAnalysisConfigurer` implementations with `@SearchExtension` and leave the configuration property unset: Hibernate Search will use the annotated implementation automatically. See [this section](#plugging-in-custom-components) for more information.<br>><br>> If this configuration property is set, it takes precedence over any `@SearchExtension` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__ANALYSIS_CONFIGURER+++[] | list of string |  |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-required-status"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.required-status`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-required-status) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.required-status+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.required-status` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.required-status+++[]<br><br>The minimal [https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html\[Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/cluster-health.html[Elasticsearch) cluster status\] required on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_REQUIRED_STATUS+++[] | `green`, `yellow`, `red` | `yellow` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-required-status-wait-timeout"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.required-status-wait-timeout`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-schema-management-required-status-wait-timeout) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".schema-management.required-status-wait-timeout+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.required-status-wait-timeout` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".schema-management.required-status-wait-timeout+++[]<br><br>How long we should wait for the status before failing the bootstrap.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__SCHEMA_MANAGEMENT_REQUIRED_STATUS_WAIT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone) | `10S` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-indexing-queue-count"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".indexing.queue-count`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-indexing-queue-count) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".indexing.queue-count+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".indexing.queue-count` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".indexing.queue-count+++[]<br><br>The number of indexing queues assigned to each index.<br><br>Higher values will lead to more connections being used in parallel, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__INDEXING_QUEUE_COUNT+++[] | int | `10` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-indexing-queue-size"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".indexing.queue-size`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-indexing-queue-size) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".indexing.queue-size+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".indexing.queue-size` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".indexing.queue-size+++[]<br><br>The size of indexing queues.<br><br>Lower values may lead to lower memory usage, especially if there are many queues, but values that are too low will reduce the likeliness of reaching the max bulk size and increase the likeliness of application threads blocking because the queue is full, which may lead to lower indexing throughput.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__INDEXING_QUEUE_SIZE+++[] | int | `1000` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-indexing-max-bulk-size"></a> [`quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".indexing.max-bulk-size`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-elasticsearch-indexes-index-name-indexing-max-bulk-size) config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch.indexes."index-name".indexing.max-bulk-size+++[]<br><br>`quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".indexing.max-bulk-size` config_property_copy_button:+++quarkus.hibernate-search-standalone.elasticsearch."backend-name".indexes."index-name".indexing.max-bulk-size+++[]<br><br>The maximum size of bulk requests created when processing indexing queues.<br><br>Higher values will lead to more documents being sent in each HTTP request sent to Elasticsearch, which may lead to higher indexing throughput, but incurs a risk of overloading Elasticsearch, i.e. of overflowing its HTTP request buffers and tripping [circuit breakers](https://www.elastic.co/guide/en/elasticsearch/reference/7.9/circuit-breaker.html), leading to Elasticsearch giving up on some request and resulting in indexing failures.<br><br>Note that raising this number above the queue size has no effect, as bulks cannot include more requests than are contained in the queue.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_ELASTICSEARCH_INDEXES__INDEX_NAME__INDEXING_MAX_BULK_SIZE+++[] | int | `100` |
| <a id="quarkus-hibernate-search-standalone-elasticsearch_section_quarkus-hibernate-search-standalone-management"></a> [Management interface](#quarkus-hibernate-search-standalone-elasticsearch_section_quarkus-hibernate-search-standalone-management) | Type | Default |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-management-root-path"></a> [`quarkus.hibernate-search-standalone.management.root-path`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-management-root-path) config_property_copy_button:+++quarkus.hibernate-search-standalone.management.root-path+++[]<br><br>Root path for reindexing endpoints. This value will be resolved as a path relative to `${quarkus.management.root-path}`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_MANAGEMENT_ROOT_PATH+++[] | string | `hibernate-search/standalone/` |
| [lock] <a id="quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-management-enabled"></a> [`quarkus.hibernate-search-standalone.management.enabled`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-management-enabled) config_property_copy_button:+++quarkus.hibernate-search-standalone.management.enabled+++[]<br><br>If management interface is turned on the reindexing endpoints will be published under the management interface. This property allows to enable this functionality by setting it to ``true\`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_SEARCH_STANDALONE_MANAGEMENT_ENABLED+++[] | boolean | `false` |

<a id="duration-note-anchor-quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone"></a>

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
> - An arbitrary string referencing a built-in implementation. Available values are detailed in the documentation of each configuration property, such as `async`/`read-sync`/`write-sync`/`sync` for [`quarkus.hibernate-search-standalone.indexing.plan.synchronization.strategy`](#quarkus-hibernate-search-standalone-elasticsearch_quarkus-hibernate-search-standalone-indexing-plan-synchronization-strategy).
>
> Other formats are also accepted, but are only useful for advanced use cases. See [this section of Hibernate Search's reference documentation](https://docs.hibernate.org/search/8.4/reference/en-US/html_single/#configuration-bean-reference-parsing) for more information.
