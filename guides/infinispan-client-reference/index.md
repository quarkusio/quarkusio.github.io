# Infinispan Client extension reference guide

Infinispan is a distributed, in-memory key/value store that provides Quarkus applications with a highly configurable and independently scalable data layer. This extension gives you client functionality that connects applications running on Quarkus with remote Infinispan clusters. To get started with Infinispan, we recommend:

1. Following the [Get Started Tutorial](https://infinispan.org/get-started/) (5 minutes).
2. Running the **remote cache** [simple code tutorials](https://infinispan.org/tutorials/).

Learn more in the [Infinispan documentation](https://infinispan.org/documentation).

## Installation

Run the following command in the base directory of your Quarkus project to add the `infinispan-client` extension:

**CLI**

```bash
quarkus extension add infinispan-client
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='infinispan-client'
```

**Gradle**

```bash
./gradlew addExtension --extensions='infinispan-client'
```

This command adds the following dependency to your build file:

**Maven**

```bash
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-infinispan-client</artifactId>
</dependency>
```

**Gradle**

```gradle
implementation 'io.quarkus:quarkus-infinispan-client'
annotationProcessor 'org.infinispan.protostream:protostream-processor:{infinispan-protostream-version}' <1>
```

1. Mandatory in the Gradle build to enable the generation of the files in the annotation based serialization

> [!IMPORTANT]
> You must include the Protostream annotations processor in your build file. Starting with Java 24, schema generation will not work unless this annotation processor is explicitly added.
>
> **Maven**
>
> ```bash
> <plugin>
>     <artifactId>maven-compiler-plugin</artifactId>
>     <version>${compiler-plugin.version}</version>
>     <configuration>
>         <annotationProcessorPaths>
>             <path>
>                 <groupId>org.infinispan.protostream</groupId>
>                 <artifactId>protostream-processor</artifactId>
>             </path>
>         </annotationProcessorPaths>
>     </configuration>
> </plugin>
> ```

## Connection to Infinispan

### Running the server

You need at least one running instance of the Infinispan Server.

If you are running a Docker instance, you can use [Infinispan Dev Services](/guides/infinispan-dev-services/index.md) and connect without configuration.

If you want to run the server yourself using Docker, check out the 5-minute [Getting stated with Infinispan](https://infinispan.org/get-started/) tutorial to run Infinispan Server

You can also [download](https://infinispan.org/download/) ${infinispan.version} Server bare metal distribution and run the following command from the distribution folder.

```bash
$ ./bin/server.sh
```

Infinispan Server **enables authentication and security authorization by default**, so you need to create a user with permissions.

- If you run the **Infinispan Server image**, pass the `USER="admin"` and `PASS="password"` parameters.
- If you run the **bare metal distribution**, use the Command Line Interface (CLI) as follows:

  ```bash
  $ ./bin/cli.sh user create admin -p password
  ```


In Kubernetes, we recommend the [Infinispan Operator](https://infinispan.org/docs/infinispan-operator/main/operator.html). Additionally, grab a look to the [Cross Site Replication](https://infinispan.org/tutorials/simple/simple_tutorials.html#cross-site-replication_remote-cache-tutorials) tutorial. You will learn how to run two separate Infinispan Clusters with docker compose (for local dev) and the Operator.

### Configuring the connection

If you are running an Infinispan Server, add the following properties to connect in the `application.properties` file in the `src/main/resources` directory.

```properties
quarkus.infinispan-client.hosts=localhost:11222 <1>

quarkus.infinispan-client.username=admin <2>
quarkus.infinispan-client.password=password <3>
```

1. Sets Infinispan Server address list, separated with semicolons
2. Sets the authentication username
3. Sets the authentication password

Alternatively, you can use uri connection by providing a single connection property

```properties
quarkus.infinispan-client.uri=hotrod://admin:password@localhost:11222 <1>
```

1. Sets Infinispan URI connection. The following properties will be ignored: hosts, username and password.

> [!TIP]
> Use Infinispan Dev Services to run a server and connect without configuration.

### Configuring backup clusters in Cross-Site Replication

In High Availability production deployments, it is common to have multiple Infinispan Clusters that are distributed across various Data Centers worldwide. Infinispan offers the capability to connect these clusters and configure backups between them. This enables seamless switching between clusters through both automated and manual methods using a single connection. To achieve this, it is necessary to configure the client to direct to the backup clusters.

```properties
quarkus.infinispan-client.hosts=host1:11222;host2:3122 <1>
quarkus.infinispan-client.username=admin
quarkus.infinispan-client.password=password
quarkus.infinispan-client.backup-cluster.nyc-site.hosts=nyc1:11222;nyc2:21222;nyc3:31222 <2>
quarkus.infinispan-client.backup-cluster.lon-site.hosts=lon1:11222;lon2:21222;lon3:31222 <3>
```

1. Sets Infinispan Server address list, separated with semicolons. This is the default cluster.
2. Configures a backup site 'nyc-site' with the provided address list
3. Configures an additional backup site 'lon-site' with the provided address list

Based on the provided configuration, in the event of the default cluster becoming unavailable, the client will seamlessly transition to one of the accessible backup clusters. Additionally, there is also the option to manually switch the client to an alternate cluster:

```java
@ApplicationScoped
public class InfinispanExample {
    @Inject
    RemoteCacheManager cacheManager;

    public void doSomething() {
       cacheManager.switchToCluster("nyc-site"); // <1>
       cacheManager.switchToCluster("lon-site"); // <2>
       cacheManager.switchToDefaultCluster(); // <3>
    }
}
```

1. The client connects to the 'nyc-site'.
2. The client connects to the 'lon-site'.
3. The client connects to the default site.

By default, Protobuf Schemas will also be uploaded to the backup clusters. However, it might be required to handle the registration manually as a schema may evolve over time when used in production, so you can disable this from occurring in each backup site by configuring the `quarkus.infinispan-client.backup-cluster.YOUR_SITE_NAME.use-schema-registration` to `false`. The value of this property will be ignored if the `use-schema-registration` global property is `false`.

> [!NOTE]
> Cross-site replication is a powerful feature offered by Infinispan that facilitates data backup between clusters situated in geographically diverse data centers, even spanning across various cloud providers. Learn more in the [Infinispan documentation](https://infinispan.org/docs/stable/titles/xsite/xsite.html).

### Default and named connections

This extension lets you configure a *default* Infinispan client connections and *named* ones. Named connections are essential to connect to multiple Infinispan clusters.

The default connection is configured using the `quarkus.infinispan-client.*` properties as seen above. When using the default connection, you can inject using a *plain* `@Inject`:

*Named* clients are configured using the `quarkus.infinispan-client.<name>.*` properties:

```properties
quarkus.infinispan-client.site-lon.hosts=localhost:11222
quarkus.infinispan-client.site-lon.username=admin
quarkus.infinispan-client.site-lon.password=password

quarkus.infinispan-client.site-nyc.hosts=localhost:31222
quarkus.infinispan-client.site-nyc.username=admin
quarkus.infinispan-client.site-nyc.password=password
```

Use the `@InfinispanClientName` qualifier with dependency injection:

```java
@ApplicationScoped
public class InfinispanExample {
    @Inject
    @InfinispanClientName("site-lon")
    RemoteCacheManager rcmLon;

    @Inject
    @InfinispanClientName("site-nyc")
    RemoteCacheManager rmcNyc;
}
```

### Infinispan Health Check

If you are using the quarkus-smallrye-health extension, the Infinispan client extensions will automatically add a readiness health check to validate the connection.

When you access the `/q/health/ready` endpoint of your application you will have information about the server connection and available caches.

This behavior can be disabled via the property `quarkus.infinispan-client.health.enabled`.

### Tracing with OpenTelemetry

Infinispan supports instrumentation of the server via OpenTelemetry. Having the `quarkus-opentelemetry` extension will propagate the traces from the Infinispan Client to the Server. This behavior can be disabled via the property `quarkus.infinispan-client.tracing.propagation.enabled`

### Creating caches from the client

When a cache is accessed from the client, if the cache does not exist in the Infinispan Server and you want to create it on first access, use one of the following properties:

```properties
quarkus.infinispan-client.cache.magazine.configuration=<distributed-cache><encoding media-type="application/x-protostream"/></distributed-cache> <1>
quarkus.infinispan-client.cache.books.configuration-resource=booksDistributedCache.json <2>
quarkus.infinispan-client.cache.authors.configuration-uri=/file/authorsIndexedCache.yaml <3>
```

1. The configuration in xml of the 'magazine' (yaml and json are also supported)
2. The file name located under the `resources` folder that contains the configuration of the 'books' cache
3. A provided file URI. The file URI can also be a file under resources

If  `configuration-resource`, `configuration` and `configuration-uri` are configured for the same cache with the same Quarkus profile, `configuration-uri` gets the highest preference, over `configuration-resource` and `configuration`. `configuration-resource` gets preference over `configuration`.

> [!WARNING]
> The `configuration-resource` is build time property and the file will be included in the native build automatically. `configuration-uri` can also point to a file under the `resources` folder. However, the file won't be automatically included in the native executable, unless you configure the property `quarkus.native.resources.includes`.

> [!TIP]
> Cache configuration can be provided in XML, JSON or YAML. Use the Infinispan Console and the cache configuration Wizard to learn more about Infinispan Caches and create guided configurations.

If nothing is configured for a particular cache, it will be created with the following basic configuration:

**XML**

```xml
<distributed-cache>
    <encoding media-type="application/x-protostream"/>
</distributed-cache>
```

**JSON**

```json
{
    "distributed-cache": {
        "encoding": {
            "media-type": "application/x-protostream"
        }
    }
}
```

**YAML**

```yaml
distributedCache:
  encoding:
    mediaType: "application/x-protostream"
```

### Authentication mechanisms

You can use the following authentication mechanisms with the Infinispan client:

- DIGEST-SHA-512 (default)
- DIGEST-MD5
- PLAIN (recommended only in combination with TLS encryption)
- EXTERNAL

Other authentication mechanisms, such as SCRAM and GSSAPI, are not yet verified with the Infinispan client.

You can find more information on configuring authentication in [Hot Rod Endpoint Authentication Mechanisms](https://infinispan.org/docs/stable/titles/hotrod_java/hotrod_java.html#hotrod_endpoint_auth-hotrod-client-configuration).

> [!NOTE]
> You must configure authentication in the `hotrod-client.properties` file if you use Dependency Injection.

## Serialization (Key Value types support)

By default, the client will support keys and values of the following types: byte\[\], primitive wrappers (e.g. Integer, Long, Double), String, Date and Instant. User types require some additional steps that are detailed here. Let's say we have the following user classes:

**Author.java**

```java
public record Author(String name, String surname) {
}
```

**Book.java**

```java
public record Book(String title,
                   String description,
                   int publicationYear,
                   Set<Author> authors,
                   Type bookType,
                   BigDecimal price) {
}
```

Serialization of user types uses a library based on protobuf, called [Protostream](https://github.com/infinispan/protostream).

> [!TIP]
> Infinispan caches can store keys and values in different encodings, but recommend using [Protocol Buffers (Protobuf)](https://developers.google.com/protocol-buffers).
>
> For more information see our [Cache Encoding and Marshalling](https://infinispan.org/docs/stable/titles/encoding/encoding.html) guide.

### Annotation based Serialization

This can be done automatically by adding protostream annotations to your user classes. In addition, a single Initializer annotated interface is required which controls how the supporting classes are generated.

Here is an example of how the preceding classes should be changed:

**Author.java**

```java
@Proto // <1>
public record Author(String name, String surname) { // <2>
}
```

1. Since Protostream 5.0, a single annotation is needed to generate a default mapping
2. Since Protostream 5.0, records are supported

**Type.java**

```java
@Proto
public enum Type { // <1>
    FANTASY,
    PROGRAMMING
}
```

1. Enums are supported

**Book.java**

```java
@Proto
@Indexed // <1>
public record Book(@Text String title, // <2>
                   @Keyword(projectable = true, sortable = true, normalizer = "lowercase", indexNullAs = "unnamed", norms = false) // <3>
                   String description,
                   int publicationYear,
                   Set<Author> authors, // <4>
                   Type bookType,
                   BigDecimal price) { // <5>
}
```

1. Indicates that the entity will be indexed. Necessary to perform distributed full-text query operations.
2. Indicates the `title` should be indexed as text
3. Indicates the `description` field field should be indexed as a Keyword.
4. Collections are supported
5. Protostream provides default Protobuf mappers for commonly used types as `BigDecimal`, included in the `org.infinispan.protostream.types` package.

Then all that is required is a very simple `GeneratedSchema` interface with an annotation on it to specify configuration settings

**BooksSchema.java**

```java
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.types.java.math.BigDecimalAdapter;

@ProtoSchema(includeClasses = { Book.class, Author.class, BigDecimalAdapter.class }, schemaPackageName = "book_sample")
interface BookStoreSchema extends GeneratedSchema {
}
```

> [!TIP]
> You can use the `basePackages` property to scan a full package containing your classes. You can override the default marshalling by using the `@Protofield` annotation.

So in this case we will automatically generate the marshaller and schemas for the included classes and place them in the schema package automatically. The package does not have to be provided, but if you use Infinispan search capabilities, you must know the generated package.

> [!NOTE]
> In Quarkus the `schemaFileName` and `schemaFilePath` attributes should NOT be set on the `ProtoSchema` annotation. Setting either attributes causes native runtime errors.

### Custom serialization

The previous method is suggested for any case when the user can annotate their classes. Unfortunately the user may not be able to annotate all classes they will put in the cache. In this case you must define your schema and create your own Marshaller(s) yourself.

**Protobuf schema**

  You can supply a protobuf schema through either one of two ways.

  1. Proto File

     You can put the `.proto` file in the `META-INF` directory of the project. These files will automatically be picked up at initialization time.

     .library.proto

     ```
     package book_sample;

     message Book {
       required string title = 1;
       required string description = 2;
       required int32 publicationYear = 3; // no native Date type available in Protobuf
       repeated Author authors = 4;
       requited double price = 5; // no native BigDecimal type available in Protobuf but you can use the adapter
     }

     message Author {
       required string name = 1;
       required string surname = 2;
     }
     ```

  2. In Code

     Or you can define the proto schema directly in user code by defining a produced bean of type `org.infinispan.protostream.schema.Schema`.

     ```java
        @Produces
        Schema bookSchema() {
             return new Schema.Builder("book.proto")
                     .packageName("book_sample")
                     .addMessage("Author")
                          .addField(Type.Scalar.STRING, "name", 1)
                          .addField(Type.Scalar.STRING, "surname", 2)
                     .addMessage("Book")
                          .addField(Type.Scalar.STRING, "title", 1)
                          .addField(Type.Scalar.STRING, "description", 2)
                          .addField(Type.Scalar.INT32, "publicationYear", 3)
                          .addRepeatedField(Type.create("Author"), "author", 4)
                          .addField(Type.Scalar.DOUBLE, "price", 5)
                     .build();
         }
     ```

**User Marshaller**

  The last thing to do is to provide a `org.infinispan.protostream.MessageMarshaller` implementation for each user class defined in the proto schema. This class is then provided via `@Produces` in a similar fashion to the code based proto schema definition above. + Here is the Marshaller class for our Author & Book classes. +

  > [!NOTE]
  > The type name must match the `<protobuf package>.<protobuf message>` exactly! + .AuthorMarshaller.java

  @Override public String getTypeName() { return "book_sample.Author"; }   @Override public Class\<? extends Author> getJavaClass() { return Author.class; }   @Override public void writeTo(ProtoStreamWriter writer, Author author) throws IOException { writer.writeString("name", author.getName()); writer.writeString("surname", author.getSurname()); }   @Override public Author readFrom(ProtoStreamReader reader) throws IOException { String name = reader.readString("name"); String surname = reader.readString("surname"); return new Author(name, surname); } }

  ```
  +
  .BookMarshaller.java
  [source,java]
  ```

  public class BookMarshaller implements MessageMarshaller\<Book> {   @Override public String getTypeName() { return "book_sample.Book"; }   @Override public Class\<? extends Book> getJavaClass() { return Book.class; }   @Override public void writeTo(ProtoStreamWriter writer, Book book) throws IOException { writer.writeString("title", book.getTitle()); writer.writeString("description", book.getDescription()); writer.writeInt("publicationYear", book.getPublicationYear()); writer.writeCollection("authors", book.getAuthors(), Author.class); writer.writeDouble("price", book.getPrice().doubleValue()); }   @Override public Book readFrom(ProtoStreamReader reader) throws IOException { String title = reader.readString("title"); String description = reader.readString("description"); int publicationYear = reader.readInt("publicationYear"); Set\<Author> authors = reader.readCollection("authors", new HashSet\<>(), Author.class); BigDecimal price = BigDecimal.valueOf(reader.readDouble("price")); return new Book(title, description, publicationYear, authors, price); } }

  ```
  +
  And you pass the marshaller by defining the following:
  +
  [source,java]
  ```

  @Produces MessageMarshaller authorMarshaller() { return new AuthorMarshaller(); }   @Produces MessageMarshaller bookMarshaller() { return new BookMarshaller(); }

  ```
  NOTE: The above produced Marshaller method MUST return `MessageMarshaller` without types or else it will not be found.
  ```

## Dependency Injection

As you saw above we support the user injecting Marshaller configuration. You can do the inverse with the Infinispan client extension providing injection for `RemoteCacheManager` and `RemoteCache` objects. There is one global `RemoteCacheManager` that takes all the configuration parameters setup in the above sections.

It is very simple to inject these components. All you need to do is to add the `@Inject` annotation to the field, constructor or method. In the below code we utilize field and constructor injection.

**SomeClass.java**

```java
    @Inject
    SomeClass(RemoteCacheManager remoteCacheManager) {
       this.remoteCacheManager = remoteCacheManager;
    }

    @Inject
    @Remote("myCache")
    RemoteCache<String, Book> cache;

    RemoteCacheManager remoteCacheManager;
```

If you notice the `RemoteCache` declaration has an additional annotation named `Remote`. This is a **qualifier** annotation allowing you to specify which named cache that will be injected. This annotation is not required and if it is not supplied, the default cache will be injected. The RemoteCacheManager and RemoteCache bean scope is `@ApplicationScoped`.

For non default connections, combine the qualifier `@InfinispanClientName` and `@Remote`.

**SomeClass.java**

```java
    @Inject
    @InfinispanClientName("lon-site")
    @Remote("books")
    RemoteCache<String, Book> lonBooks;

    @Inject
    @InfinispanClientName("nyc-site")
    @Remote("books")
    RemoteCache<String, Book> nycBooks;
```

> [!NOTE]
> Other types may be supported for injection, please see other sections for more information

### Mock Support

Quarkus supports the use of mock objects using two different approaches. You can either use CDI alternatives to mock out a bean for all test classes, or use `QuarkusMock` to mock out beans on a per test basis. Check the [Getting started with testing guide](/guides/getting-started-testing/index.md) for more information.

RemoteCacheManager and RemoteCache can be mocked.

**BookService.java**

```java
@ApplicationScoped
public class BookService {

   @Inject
   @Remote("books")
   RemoteCache<String, Book> books; // <1>

   public String getBookDescriptionById(String id) {
      Book book = books.get(id);
      if (book == null) {
         return "default";
      }

      return book.getDescription();
   }
}
```

1. Use dependency injection to connect to the books cache

In the test class, the RemoteCache can be mocked.

**BookServiceTest.java**

```java
@QuarkusTest
public class BookServiceTest {

    @Inject
    BookService bookService;

    @InjectMock // <1>
    @Remote("books")
    RemoteCache<String, Book> bookRemoteCache;

    @Test
    public void mockRemoteCache() {
        Mockito.when(bookRemoteCache.get("harry_potter")).thenReturn(new Book(... "Best saga ever");// <2>

        Assertions.assertThat(bookService.getBookDescriptionById("harry_potter")).isEqualTo("Best saga ever");// <3>
    }
}
```

1. Inject a mock instead of the RemoteCache bean
2. Use Mockito to mock the call of the RemoteCache
3. Assert the service call

### Registering Protobuf Schemas with Infinispan Server

You need to register the generated Protobuf schemas with Infinispan Server to perform queries or convert from `Protobuf` to other media types such as `JSON`.

> [!TIP]
> You can check the schemas that exist under the `Schemas` tab by logging into Infinispan Console at `http://SERVER_HOST:SERVER_PORT` (for example `http://localhost:11222`).
>
> Check the [Infinispan Dev Services Guide](/guides/infinispan-dev-services/index.md) to connect to the Infinispan Dev Services server.

By default, Protobuf schemas generated this way will be registered by this extension when the client first connects. However, it might be required to handle the registration manually as a schema may evolve over time when used in production, so you can disable this from occurring by configuring the `quarkus.infinispan-client.use-schema-registration` to `false`.

To configure the schema manually please use [Infinispan Operator](https://infinispan.org/docs/infinispan-operator/main/operator.html) for Kubernetes deployments, Infinispan Console, [REST API](https://infinispan.org/docs/stable/titles/rest/rest.html#rest_v2_protobuf_schemas) or the [Hot Rod Java Client](https://infinispan.org/docs/stable/titles/encoding/encoding.html#registering-sci-remote-caches_marshalling).

<a id="infinispan-annotations-api"></a>
### Caching using annotations

> [!IMPORTANT]
> Infinispan Caching annotations are deprecated **in this extension** and will be removed. Use or replace your annotations by using the [Infinispan Cache extension](/guides/cache-infinispan-reference/index.md). Update your import statements to use the annotations from `io.quarkus.cache` package instead of `io.quarkus.infinispan.client`.

The Infinispan Client extension offers a set of annotations that can be used in a CDI managed bean to enable caching abilities with Infinispan.

> [!WARNING]
> Caching annotations are not allowed on private methods. They will work fine with any other access modifier including package-private (no explicit modifier).

### @CacheResult

Loads a method result from the cache without executing the method body whenever possible.

When a method annotated with `@CacheResult` is invoked, Quarkus will use the method argument as the cache key and check in the cache whether the method has been already invoked. Methods with multiple parameters are not allowed. For composite keys, define a Protobuf schema that will hold multiple values. If a value is found in the cache, it is returned and the annotated method is never actually executed. If no value is found, the annotated method is invoked and the returned value is stored in the cache using the computed key. This annotation cannot be used on a method returning `void`.

> [!NOTE]
> Infinispan Client extension is not able yet to cache `null` values unlike the Quarkus-Cache extension.

### @CacheInvalidate

Removes an entry from the cache.

When a method annotated with `@CacheInvalidate` is invoked, Infinispan will use the method argument as a cache key to try to remove an existing entry from the cache. If the key does not identify any cache entry, nothing will happen.

### @CacheInvalidateAll

When a method annotated with `@CacheInvalidateAll` is invoked, Infinispan will remove all entries from the cache.

## Querying

The Infinispan client supports both indexed and non-indexed search as long as the `ProtoStreamMarshaller` is configured above. This allows the user to query on **keys** or **values** based on the properties of the proto schema. **Indexed queries are preferred for performance reasons**.

**XML**

```xml
<distributed-cache name="books" statistics="true">
    <!-- other configuration -->
	<indexing enabled="true" storage="filesystem" startup-mode="PURGE">
		<indexed-entities>
			<indexed-entity>book_sample.Book</indexed-entity>
		</indexed-entities>
	</indexing>
</distributed-cache>
```

**JSON**

```json
{
  "books": {
    "distributed-cache": {
      ...
      "indexing": {
        "enabled": true,
        "storage": "filesystem",
        "startupMode": "PURGE",
        "indexed-entities": [
          "book_sample.Book"
        ]
      }
    }
  }
}
```

**YAML**

```yaml
distributedCache:
  # other configuration
  indexing:
    enabled: "true"
    storage: "filesystem"
    startupMode: "PURGE"
    indexedEntities:
      - "book_sample.Book"
```

Query builds upon the proto definitions you can configure when setting up the `ProtoStreamMarshaller`. Either method of Serialization above will automatically register the schema with the server at startup, meaning that you will automatically gain the ability to query objects stored in the remote Infinispan Server.

**Book.java**

```java
@Indexed <1>
public class Book {

    @ProtoFactory
    public Book(String title, String description, int publicationYear, Set<Author> authors) {
      ...
    }

    @ProtoField(number = 1)
    @Text <2>
    public String getTitle() {
        return title;
    }

    @ProtoField(number = 2)
    @Keyword(projectable = true, sortable = true, normalizer = "lowercase", indexNullAs = "unnamed", norms = false) <3>
    public String getDescription() {
        return description;
    }
    ...
```

1. `@Indexed` annotation makes the POJO indexable
2. `@Basic` annotation is used for indexed fields without any special transformation
3. `@Keyword` annotation is used to apply a normalizer to a text field

You can use either the Query DSL or the Ickle Query language with the Quarkus Infinispan client extension.

**Query.java**

```java
@Inject
@Remote("books")
RemoteCache<String, Book> booksCache; // <1>

Query<Book> query = booksCache.query("from book_sample.Book b where b.authors.name like '%" + name + "%'"); // <2>
List<Book> list = query.execute().list();
```

1. Inject the books cache
2. Perform a full text query on books author name

> [!NOTE]
> You can read more about [querying](https://infinispan.org/docs/stable/titles/query/query.html) in the Infinispan documentation.

> [!IMPORTANT]
> Prior to Quarkus 3.9 and the Infinispan 15 integration, queries were executed by calling the following code: .Query.java
>
> ```java
> QueryFactory queryFactory = Search.getQueryFactory(booksCache); <1>
> Query query = queryFactory.create("from book_sample.Book");
> List<Book> list = query.execute().list();
> ```
>
> 1. Breaking change in 3.9
>
> This code won't work anymore since `RemoteCache` is now an `@ApplicationScoped` proxy bean. `Search.getQueryFactory` will raise a ClassCastException. Remove the unecessary indirection by using the `query` method in the `RemoteCache` API as follows.
>
> ```java
> Query<Book> query = booksCache.<Book>query("from book_sample.Book");
> List<Book> list = query.execute().list();
> ```

## Counters

Infinispan also has a notion of counters and the Quarkus Infinispan client supports them out of the box.

The Quarkus Infinispan client extension allows for Dependency Injection of the `CounterManager` directly. All you need to do is annotate your field, constructor or method, and you get it with no fuss. You can then use counters as you would normally.

```java
@Inject
CounterManager counterManager;
```

You can read more about [clustered counters](https://infinispan.org/docs/stable/titles/developing/developing.html#clustered_counters) in the Infinispan documentation.

## Near Caching

Near caching is disabled by default, but you can enable it on a per cache basic by configuring the following properties:

```properties
quarkus.infinispan-client.cache.books.near-cache-mode=INVALIDATED <1>
quarkus.infinispan-client.cache.books.near-cache-max-entries=200 <2>
quarkus.infinispan-client.cache.books.near-cache-use-bloom-filter=true <3>
```

1. Enables near caching for the 'books' cache by setting the mode to `INVALIDATED`
2. Sets the maximum number of entries that the near cache of the 'books' cache can hold before eviction occurs
3. Enables bloom filter for the 'books' cache

### Bounded near caching

You should always use bounded near caches by specifying the maximum number of entries they can contain.

### Bloom filters

If you need to optimize the performance for write operations by reducing the total number of invalidation messages, enable bloom filter. Bloom filters reside on Infinispan Server and keep track of the entries that the client has requested. They cannot be used with unbounded near cache: maximum number of entries must be defined when enabling bloom filters.

## Encryption

Encryption at this point requires additional steps to get working.

The first step is to configure the `application.properties` file to point to your truststore and/or keystore. This is further detailed [here](https://infinispan.org/docs/stable/titles/hotrod_java/hotrod_java.html#hotrod_encryption).

The Infinispan Client extension enables SSL/TLS by default. You can read more about this at [Using SSL With Native Executables](/guides/native-and-ssl/index.md).

## SSL Host Name Validation

To prevent MITM attacks, when SSL is enabled, SSL host name validation is enabled by default in Infinispan. In this case, configuring the SNI host name is mandatory to start the client.

```properties
quarkus.infinispan-client.sni-host-name=localhost <1>
```

1. Sets the SNI host name

This behaviour can be changed by disabling the validation.

```properties
quarkus.infinispan-client.ssl-host-name-validation=false <1>
```

1. Disables ssl host name validation

## Additional Features

The Infinispan Client has additional features that were not mentioned here. This means this feature was not tested in a Quarkus environment, and they may or may not work. Please let us know if you need these added!

<a id="dev-services"></a>
## Dev Services for Infinispan

When you use the infinispan-client extension in dev mode or in test, Quarkus automatically starts an Infinispan server and configure your application.

### Enabling / Disabling Dev Services for Infinispan

> [!NOTE]
> Learn more in the [Infinispan Dev Services guide](/guides/infinispan-dev-services/index.md).

## Shared server

Quarkus will share the Infinispan broker if you have multiple applications running in dev mode. Dev Services for Infinispan implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single broker.

> [!NOTE]
> Dev Services for Infinispan starts the container with the `quarkus-dev-service-infinispan` label which is used to identify the container.

If you need multiple (shared) Infinispan server, you can configure the `quarkus.infinispan-client.devservices.service-name` attribute and indicate the server name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `infinispan`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.infinispan-client.devservices.shared=false`.

## Setting the port

By default, Dev Services for Infinispan picks a random port and configures the application. You can set the port by configuring the `quarkus.infinispan-client.devservices.port` property.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-marshaller-class"></a> [`quarkus.infinispan-client.marshaller-class`](#quarkus-infinispan-client_quarkus-infinispan-client-marshaller-class) config_property_copy_button:+++quarkus.infinispan-client.marshaller-class+++[]<br><br>Sets the marshallerClass. Default is ProtoStreamMarshaller<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_MARSHALLER_CLASS+++[] | string |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-transport-factory"></a> [`quarkus.infinispan-client.transport-factory`](#quarkus-infinispan-client_quarkus-infinispan-client-transport-factory) config_property_copy_button:+++quarkus.infinispan-client.transport-factory+++[]<br><br>Sets the transport Factory. Default is QuarkusTransportFactory<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_TRANSPORT_FACTORY+++[] | string | `io.quarkus.infinispan.client.runtime.QuarkusTransportFactory` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-health-enabled"></a> [`quarkus.infinispan-client.health.enabled`](#quarkus-infinispan-client_quarkus-infinispan-client-health-enabled) config_property_copy_button:+++quarkus.infinispan-client.health.enabled+++[]<br><br>Whether or not a health check is published in case the smallrye-health extension is present.<br><br>This is a global setting and is not specific to an Infinispan Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_HEALTH_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-uri"></a> [`quarkus.infinispan-client.uri`](#quarkus-infinispan-client_quarkus-infinispan-client-uri) config_property_copy_button:+++quarkus.infinispan-client.uri+++[]<br><br>Sets the URI of the running Infinispan server to connect to. hotrod://localhost:11222@admin:password If provided `hosts`, `username` and `password` will be ignored.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_URI+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-hosts"></a> [`quarkus.infinispan-client.hosts`](#quarkus-infinispan-client_quarkus-infinispan-client-hosts) config_property_copy_button:+++quarkus.infinispan-client.hosts+++[]<br><br>Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_HOSTS+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-intelligence"></a> [`quarkus.infinispan-client.client-intelligence`](#quarkus-infinispan-client_quarkus-infinispan-client-client-intelligence) config_property_copy_button:+++quarkus.infinispan-client.client-intelligence+++[]<br><br>Sets client intelligence used by authentication Available values: ++**++ `BASIC` - Means that the client doesn't handle server topology changes and therefore will only use the list of servers supplied at configuration time. ++**++ `TOPOLOGY_AWARE` - Use this provider if you don't want the client to present any certificates to the remote TLS host. ++\*++ `HASH_DISTRIBUTION_AWARE` - Like `TOPOLOGY_AWARE` but with the additional advantage that each request involving keys will be routed to the server who is the primary owner which improves performance greatly. This is the default mode in Infinispan.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CLIENT_INTELLIGENCE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-use-auth"></a> [`quarkus.infinispan-client.use-auth`](#quarkus-infinispan-client_quarkus-infinispan-client-use-auth) config_property_copy_button:+++quarkus.infinispan-client.use-auth+++[]<br><br>Enables or disables authentication. Set it to false when connecting to an Infinispan Server without authentication. deployments. Default is 'true'.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_USE_AUTH+++[] | boolean | `true` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-username"></a> [`quarkus.infinispan-client.username`](#quarkus-infinispan-client_quarkus-infinispan-client-username) config_property_copy_button:+++quarkus.infinispan-client.username+++[]<br><br>Sets username used by authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_USERNAME+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-password"></a> [`quarkus.infinispan-client.password`](#quarkus-infinispan-client_quarkus-infinispan-client-password) config_property_copy_button:+++quarkus.infinispan-client.password+++[]<br><br>Sets password used by authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-auth-realm"></a> [`quarkus.infinispan-client.auth-realm`](#quarkus-infinispan-client_quarkus-infinispan-client-auth-realm) config_property_copy_button:+++quarkus.infinispan-client.auth-realm+++[]<br><br>Sets realm used by authentication<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_AUTH_REALM+++[] | string | `default` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-auth-server-name"></a> [`quarkus.infinispan-client.auth-server-name`](#quarkus-infinispan-client_quarkus-infinispan-client-auth-server-name) config_property_copy_button:+++quarkus.infinispan-client.auth-server-name+++[]<br><br>Sets server name used by authentication<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_AUTH_SERVER_NAME+++[] | string | `infinispan` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-sasl-mechanism"></a> [`quarkus.infinispan-client.sasl-mechanism`](#quarkus-infinispan-client_quarkus-infinispan-client-sasl-mechanism) config_property_copy_button:+++quarkus.infinispan-client.sasl-mechanism+++[]<br><br>Sets SASL mechanism used by authentication. Available values: ++**++ `DIGEST-SHA-512` - Uses the SHA-512 hashing algorithm to securely hash and verify credentials. This is the default. ++**++ `DIGEST-MD5` - Uses the MD5 hashing algorithm in addition to nonces to encrypt credentials. ++**++ `EXTERNAL` - Uses client certificates to provide valid identities to Infinispan Server and enable encryption. ++**++ `PLAIN` - Sends credentials in plain text (unencrypted) over the wire in a way that is similar to HTTP BASIC authentication. You should use `PLAIN` authentication only in combination with TLS encryption.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SASL_MECHANISM+++[] | string | `DIGEST-SHA-512` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-key-store"></a> [`quarkus.infinispan-client.key-store`](#quarkus-infinispan-client_quarkus-infinispan-client-key-store) config_property_copy_button:+++quarkus.infinispan-client.key-store+++[]<br><br>Specifies the filename of a keystore to use to create the `SSLContext`. You also need to specify a keyStorePassword. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_KEY_STORE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-key-store-password"></a> [`quarkus.infinispan-client.key-store-password`](#quarkus-infinispan-client_quarkus-infinispan-client-key-store-password) config_property_copy_button:+++quarkus.infinispan-client.key-store-password+++[]<br><br>Specifies the password needed to open the keystore. You also need to specify a keyStore. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-key-store-type"></a> [`quarkus.infinispan-client.key-store-type`](#quarkus-infinispan-client_quarkus-infinispan-client-key-store-type) config_property_copy_button:+++quarkus.infinispan-client.key-store-type+++[]<br><br>Specifies the type of the keyStore, such as PKCS12.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_KEY_STORE_TYPE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-key-alias"></a> [`quarkus.infinispan-client.key-alias`](#quarkus-infinispan-client_quarkus-infinispan-client-key-alias) config_property_copy_button:+++quarkus.infinispan-client.key-alias+++[]<br><br>Sets the unique name used to identify a specific key pair in a keystore for secure connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_KEY_ALIAS+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-trust-store"></a> [`quarkus.infinispan-client.trust-store`](#quarkus-infinispan-client_quarkus-infinispan-client-trust-store) config_property_copy_button:+++quarkus.infinispan-client.trust-store+++[]<br><br>Specifies the filename of a truststore to use to create the `SSLContext`. You also need to specify a trustStorePassword. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_TRUST_STORE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-trust-store-password"></a> [`quarkus.infinispan-client.trust-store-password`](#quarkus-infinispan-client_quarkus-infinispan-client-trust-store-password) config_property_copy_button:+++quarkus.infinispan-client.trust-store-password+++[]<br><br>Specifies the password needed to open the truststore You also need to specify a trustStore. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-trust-store-type"></a> [`quarkus.infinispan-client.trust-store-type`](#quarkus-infinispan-client_quarkus-infinispan-client-trust-store-type) config_property_copy_button:+++quarkus.infinispan-client.trust-store-type+++[]<br><br>Specifies the type of the truststore, such as JKS or JCEKS. Defaults to JKS if trustStore is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_TRUST_STORE_TYPE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-ssl-protocol"></a> [`quarkus.infinispan-client.ssl-protocol`](#quarkus-infinispan-client_quarkus-infinispan-client-ssl-protocol) config_property_copy_button:+++quarkus.infinispan-client.ssl-protocol+++[]<br><br>Configures the secure socket protocol. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SSL_PROTOCOL+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-ssl-provider"></a> [`quarkus.infinispan-client.ssl-provider`](#quarkus-infinispan-client_quarkus-infinispan-client-ssl-provider) config_property_copy_button:+++quarkus.infinispan-client.ssl-provider+++[]<br><br>Sets the ssl provider. For example BCFIPS Setting this implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SSL_PROVIDER+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-ssl-ciphers"></a> [`quarkus.infinispan-client.ssl-ciphers`](#quarkus-infinispan-client_quarkus-infinispan-client-ssl-ciphers) config_property_copy_button:+++quarkus.infinispan-client.ssl-ciphers+++[]<br><br>Configures the ciphers. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SSL_CIPHERS+++[] | list of string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-ssl-host-name-validation"></a> [`quarkus.infinispan-client.ssl-host-name-validation`](#quarkus-infinispan-client_quarkus-infinispan-client-ssl-host-name-validation) config_property_copy_button:+++quarkus.infinispan-client.ssl-host-name-validation+++[]<br><br>Do SSL hostname validation. Defaults to true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SSL_HOST_NAME_VALIDATION+++[] | boolean |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-sni-host-name"></a> [`quarkus.infinispan-client.sni-host-name`](#quarkus-infinispan-client_quarkus-infinispan-client-sni-host-name) config_property_copy_button:+++quarkus.infinispan-client.sni-host-name+++[]<br><br>SNI host name. Mandatory when SSL is enabled and host name validation is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SNI_HOST_NAME+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-socket-timeout"></a> [`quarkus.infinispan-client.socket-timeout`](#quarkus-infinispan-client_quarkus-infinispan-client-socket-timeout) config_property_copy_button:+++quarkus.infinispan-client.socket-timeout+++[]<br><br>Configures the socket timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SOCKET_TIMEOUT+++[] | int |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-tracing-propagation-enabled"></a> [`quarkus.infinispan-client.tracing.propagation.enabled`](#quarkus-infinispan-client_quarkus-infinispan-client-tracing-propagation-enabled) config_property_copy_button:+++quarkus.infinispan-client.tracing.propagation.enabled+++[]<br><br>Whether a tracing propagation is enabled in case the Opentelemetry extension is present. By default the propagation of the context is propagated from the client to the Infinispan Server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_TRACING_PROPAGATION_ENABLED+++[] | boolean |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-use-schema-registration"></a> [`quarkus.infinispan-client.use-schema-registration`](#quarkus-infinispan-client_quarkus-infinispan-client-use-schema-registration) config_property_copy_button:+++quarkus.infinispan-client.use-schema-registration+++[]<br><br>Enables or disables Protobuf generated schemas upload to the server. Set it to 'false' when you need to handle the lifecycle of the Protobuf Schemas on Server side yourself. Default is 'true'. This is a global setting and is not specific to a Infinispan Client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_USE_SCHEMA_REGISTRATION+++[] | boolean | `true` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-start-client"></a> [`quarkus.infinispan-client.start-client`](#quarkus-infinispan-client_quarkus-infinispan-client-start-client) config_property_copy_button:+++quarkus.infinispan-client.start-client+++[]<br><br>Starts the client and connects to the server. If set to false, you'll need to start it yourself.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_START_CLIENT+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-configuration-resource"></a> [`quarkus.infinispan-client.cache."cache".configuration-resource`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-configuration-resource) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".configuration-resource+++[]<br><br>Cache configuration file in XML, JSON or YAML is defined in build time to create the cache on first access. An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder: quarkus.infinispan-client.cache.bookscache.configuration-resource=cacheConfig.xml<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__CONFIGURATION_RESOURCE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-configuration"></a> [`quarkus.infinispan-client.cache."cache".configuration`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-configuration) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".configuration+++[]<br><br>Cache configuration in inlined XML to create the cache on first access. Will be ignored if the configuration-uri is provided for the same cache name. An example of the user defined property: quarkus.infinispan-client.cache.bookscache.configuration=<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__CONFIGURATION+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-configuration-uri"></a> [`quarkus.infinispan-client.cache."cache".configuration-uri`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-configuration-uri) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".configuration-uri+++[]<br><br>Cache configuration file in XML, Json or YAML whose path will be converted to URI to create the cache on first access. An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder: quarkus.infinispan-client.cache.bookscache.configuration-uri=cacheConfig.xml<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__CONFIGURATION_URI+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-near-cache-max-entries"></a> [`quarkus.infinispan-client.cache."cache".near-cache-max-entries`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-near-cache-max-entries) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".near-cache-max-entries+++[]<br><br>The maximum number of entries to keep locally for the specified cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__NEAR_CACHE_MAX_ENTRIES+++[] | int |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-near-cache-mode"></a> [`quarkus.infinispan-client.cache."cache".near-cache-mode`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-near-cache-mode) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".near-cache-mode+++[]<br><br>Sets near cache mode used by the Infinispan Client Available values: ++**++ `DISABLED` - Means that near caching is disabled. This is the default value. ++**++ `INVALIDATED` - Means is near caching is invalidated, so when entries are updated or removed server-side, invalidation messages will be sent to clients to remove them from the near cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__NEAR_CACHE_MODE+++[] | `disabled`, `invalidated` |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-near-cache-use-bloom-filter"></a> [`quarkus.infinispan-client.cache."cache".near-cache-use-bloom-filter`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-near-cache-use-bloom-filter) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".near-cache-use-bloom-filter+++[]<br><br>Enables bloom filter for near caching. Bloom filters optimize performance for write operations by reducing the total number of invalidation messages.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__NEAR_CACHE_USE_BLOOM_FILTER+++[] | boolean |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-transaction-mode"></a> [`quarkus.infinispan-client.cache."cache".transaction-mode`](#quarkus-infinispan-client_quarkus-infinispan-client-cache-cache-transaction-mode) config_property_copy_button:+++quarkus.infinispan-client.cache."cache".transaction-mode+++[]<br><br>Enables transaction mode in the client side, for transactional caches.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_CACHE__CACHE__TRANSACTION_MODE+++[] | `none`, `non-xa`, `non-durable-xa`, `full-xa` |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-backup-cluster-backup-cluster-hosts"></a> [`quarkus.infinispan-client.backup-cluster."backup-cluster".hosts`](#quarkus-infinispan-client_quarkus-infinispan-client-backup-cluster-backup-cluster-hosts) config_property_copy_button:+++quarkus.infinispan-client.backup-cluster."backup-cluster".hosts+++[]<br><br>Sets the host name/port to connect to. Each one is separated by a semicolon (eg. hostA:11222;hostB:11222).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_BACKUP_CLUSTER__BACKUP_CLUSTER__HOSTS+++[] | string | required [exclamation circle] |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-backup-cluster-backup-cluster-client-intelligence"></a> [`quarkus.infinispan-client.backup-cluster."backup-cluster".client-intelligence`](#quarkus-infinispan-client_quarkus-infinispan-client-backup-cluster-backup-cluster-client-intelligence) config_property_copy_button:+++quarkus.infinispan-client.backup-cluster."backup-cluster".client-intelligence+++[]<br><br>Sets client intelligence used by authentication Available values: ++**++ `BASIC` - Means that the client doesn't handle server topology changes and therefore will only use the list of servers supplied at configuration time. ++**++ `TOPOLOGY_AWARE` - Use this provider if you don't want the client to present any certificates to the remote TLS host. ++\*++ `HASH_DISTRIBUTION_AWARE` - Like `TOPOLOGY_AWARE` but with the additional advantage that each request involving keys will be routed to the server who is the primary owner which improves performance greatly. This is the default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_BACKUP_CLUSTER__BACKUP_CLUSTER__CLIENT_INTELLIGENCE+++[] | `basic`, `topology-aware`, `hash-distribution-aware` | `hash-distribution-aware` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-backup-cluster-backup-cluster-use-schema-registration"></a> [`quarkus.infinispan-client.backup-cluster."backup-cluster".use-schema-registration`](#quarkus-infinispan-client_quarkus-infinispan-client-backup-cluster-backup-cluster-use-schema-registration) config_property_copy_button:+++quarkus.infinispan-client.backup-cluster."backup-cluster".use-schema-registration+++[]<br><br>Enables or disables Protobuf generated schemas upload to the backup. Set it to 'false' when you need to handle the lifecycle of the Protobuf Schemas on Server side yourself. Default is 'true'. This setting will be ignored if the Global Setting is set up to false.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_BACKUP_CLUSTER__BACKUP_CLUSTER__USE_SCHEMA_REGISTRATION+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-near-cache-max-entries"></a> <mark>link:#quarkus-infinispan-client_quarkus-infinispan-client-near-cache-max-entries\[`quarkus.infinispan-client.near-cache-max-entries`\]</mark> config_property_copy_button:+++quarkus.infinispan-client.near-cache-max-entries+++[]<br><br>*This property is deprecated: use per cache configuration for near cache max entries.*<br><br>Sets the bounded entry count for near cache. If this value is 0 or less near cache is disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_NEAR_CACHE_MAX_ENTRIES+++[] | int | `0` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-server-list"></a> <mark>link:#quarkus-infinispan-client_quarkus-infinispan-client-server-list\[`quarkus.infinispan-client.server-list`\]</mark> config_property_copy_button:+++quarkus.infinispan-client.server-list+++[]<br><br>*This property is deprecated: `hosts` should be used to configure the list or uri for an uri connection string.*<br><br>Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_SERVER_LIST+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-auth-username"></a> <mark>link:#quarkus-infinispan-client_quarkus-infinispan-client-auth-username\[`quarkus.infinispan-client.auth-username`\]</mark> config_property_copy_button:+++quarkus.infinispan-client.auth-username+++[]<br><br>*This property is deprecated: `username` should be used to configure the credentials username.*<br><br>Sets username used by authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_AUTH_USERNAME+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-auth-password"></a> <mark>link:#quarkus-infinispan-client_quarkus-infinispan-client-auth-password\[`quarkus.infinispan-client.auth-password`\]</mark> config_property_copy_button:+++quarkus.infinispan-client.auth-password+++[]<br><br>*This property is deprecated: `password` should be used to configure the credentials password.*<br><br>Sets password used by authentication<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_AUTH_PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_section_quarkus-infinispan-client-devservices"></a> [Dev Services](#quarkus-infinispan-client_section_quarkus-infinispan-client-devservices) | Type | Default |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-enabled"></a> [`quarkus.infinispan-client.devservices.enabled`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-enabled) config_property_copy_button:+++quarkus.infinispan-client.devservices.enabled+++[]<br><br>If DevServices has been explicitly enabled or disabled. DevServices is generally enabled by default, unless there is an existing configuration present.<br><br>When DevServices is enabled Quarkus will attempt to automatically configure and start a database when running in Dev or Test mode and when Docker is running.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-create-default-client"></a> [`quarkus.infinispan-client.devservices.create-default-client`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-create-default-client) config_property_copy_button:+++quarkus.infinispan-client.devservices.create-default-client+++[]<br><br>When the configuration is empty, an Infinispan default client is automatically created to connect to the running dev service. However, there are scenarios where creating this client is unnecessary, yet we still need to spin up an Infinispan Server. In such cases, this property serves to determine whether the client should be created by default or not by the extension.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_CREATE_DEFAULT_CLIENT+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-port"></a> [`quarkus.infinispan-client.devservices.port`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-port) config_property_copy_button:+++quarkus.infinispan-client.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-shared"></a> [`quarkus.infinispan-client.devservices.shared`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-shared) config_property_copy_button:+++quarkus.infinispan-client.devservices.shared+++[]<br><br>Indicates if the Infinispan server managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Infinispan starts a new container.<br><br>The discovery uses the `quarkus-dev-service-infinispan` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-service-name"></a> [`quarkus.infinispan-client.devservices.service-name`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-service-name) config_property_copy_button:+++quarkus.infinispan-client.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-infinispan` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Infinispan looks for a container with the `quarkus-dev-service-infinispan` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-infinispan` label set to the specified value.<br><br>This property is used when you need multiple shared Infinispan servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_SERVICE_NAME+++[] | string | `infinispan` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-image-name"></a> [`quarkus.infinispan-client.devservices.image-name`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-image-name) config_property_copy_button:+++quarkus.infinispan-client.devservices.image-name+++[]<br><br>The image to use. Note that only official Infinispan images are supported.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_IMAGE_NAME+++[] | string | `quay.io/infinispan/server:latest` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-artifacts"></a> [`quarkus.infinispan-client.devservices.artifacts`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-artifacts) config_property_copy_button:+++quarkus.infinispan-client.devservices.artifacts+++[]<br><br>List of the artifacts to automatically download and add to the Infinispan server libraries.<br><br>For example a Maven coordinate (org.postgresql:postgresql:42.3.1) or a dependency location url.<br><br>If an invalid value is passed, the Infinispan server will throw an error when trying to start.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_ARTIFACTS+++[] | list of string |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-site"></a> [`quarkus.infinispan-client.devservices.site`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-site) config_property_copy_button:+++quarkus.infinispan-client.devservices.site+++[]<br><br>Add a site name to start the Infinispan Server Container with Cross Site Replication enabled (ex. lon). Cross Site Replication is the capability to connect two separate Infinispan Server Clusters that might run in different Data Centers, and configure backup caches to copy the data across the clusters with active-active or active-passive replication. See more about Cross Site Replication in the Infinispan Documentation <https://infinispan.org/docs/stable/titles/xsite/xsite.html> Configure `mcast-port` to avoid forming a cluster with any other running Infinispan Server container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_SITE+++[] | string |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-mcast-port"></a> [`quarkus.infinispan-client.devservices.mcast-port`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-mcast-port) config_property_copy_button:+++quarkus.infinispan-client.devservices.mcast-port+++[]<br><br>If you are running an Infinispan Server already in docker, if the containers use the same mcastPort they will form a cluster. Set a different mcastPort to create a separate cluster in Docker (e. 46656). A common use case in a local Docker development mode, is the need of having two different Infinispan Clusters with Cross Site Replication enabled. see <https://github.com/infinispan/infinispan-simple-tutorials/blob/main/infinispan-remote/cross-site-replication/docker-compose/><br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_MCAST_PORT+++[] | int |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-container-env-environment-variable-name"></a> [`quarkus.infinispan-client.devservices.container-env."environment-variable-name"`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.infinispan-client.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-devservices-config-files"></a> [`quarkus.infinispan-client.devservices.config-files`](#quarkus-infinispan-client_quarkus-infinispan-client-devservices-config-files) config_property_copy_button:+++quarkus.infinispan-client.devservices.config-files+++[]<br><br>Infinispan Server configuration chunks to be passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT_DEVSERVICES_CONFIG_FILES+++[] | list of string |  |
| <a id="quarkus-infinispan-client_section_quarkus-infinispan-client"></a> [Named clients](#quarkus-infinispan-client_section_quarkus-infinispan-client) | Type | Default |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-marshaller-class"></a> [`quarkus.infinispan-client."client-name".marshaller-class`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-marshaller-class) config_property_copy_button:+++quarkus.infinispan-client."client-name".marshaller-class+++[]<br><br>Sets the marshallerClass. Default is ProtoStreamMarshaller<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__MARSHALLER_CLASS+++[] | string |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-transport-factory"></a> [`quarkus.infinispan-client."client-name".transport-factory`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-transport-factory) config_property_copy_button:+++quarkus.infinispan-client."client-name".transport-factory+++[]<br><br>Sets the transport Factory. Default is QuarkusTransportFactory<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__TRANSPORT_FACTORY+++[] | string | `io.quarkus.infinispan.client.runtime.QuarkusTransportFactory` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-configuration-resource"></a> [`quarkus.infinispan-client."client-name".cache."cache".configuration-resource`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-configuration-resource) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".configuration-resource+++[]<br><br>Cache configuration file in XML, JSON or YAML is defined in build time to create the cache on first access. An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder: quarkus.infinispan-client.cache.bookscache.configuration-resource=cacheConfig.xml<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__CONFIGURATION_RESOURCE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-uri"></a> [`quarkus.infinispan-client."client-name".uri`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-uri) config_property_copy_button:+++quarkus.infinispan-client."client-name".uri+++[]<br><br>Sets the URI of the running Infinispan server to connect to. hotrod://localhost:11222@admin:password If provided `hosts`, `username` and `password` will be ignored.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__URI+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-hosts"></a> [`quarkus.infinispan-client."client-name".hosts`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-hosts) config_property_copy_button:+++quarkus.infinispan-client."client-name".hosts+++[]<br><br>Sets the host name/port to connect to. Each one is separated by a semicolon (eg. host1:11222;host2:11222).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__HOSTS+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-client-intelligence"></a> [`quarkus.infinispan-client."client-name".client-intelligence`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-client-intelligence) config_property_copy_button:+++quarkus.infinispan-client."client-name".client-intelligence+++[]<br><br>Sets client intelligence used by authentication Available values: ++**++ `BASIC` - Means that the client doesn't handle server topology changes and therefore will only use the list of servers supplied at configuration time. ++**++ `TOPOLOGY_AWARE` - Use this provider if you don't want the client to present any certificates to the remote TLS host. ++\*++ `HASH_DISTRIBUTION_AWARE` - Like `TOPOLOGY_AWARE` but with the additional advantage that each request involving keys will be routed to the server who is the primary owner which improves performance greatly. This is the default mode in Infinispan.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CLIENT_INTELLIGENCE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-use-auth"></a> [`quarkus.infinispan-client."client-name".use-auth`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-use-auth) config_property_copy_button:+++quarkus.infinispan-client."client-name".use-auth+++[]<br><br>Enables or disables authentication. Set it to false when connecting to an Infinispan Server without authentication. deployments. Default is 'true'.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__USE_AUTH+++[] | boolean | `true` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-username"></a> [`quarkus.infinispan-client."client-name".username`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-username) config_property_copy_button:+++quarkus.infinispan-client."client-name".username+++[]<br><br>Sets username used by authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__USERNAME+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-password"></a> [`quarkus.infinispan-client."client-name".password`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-password) config_property_copy_button:+++quarkus.infinispan-client."client-name".password+++[]<br><br>Sets password used by authentication.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-auth-realm"></a> [`quarkus.infinispan-client."client-name".auth-realm`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-auth-realm) config_property_copy_button:+++quarkus.infinispan-client."client-name".auth-realm+++[]<br><br>Sets realm used by authentication<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__AUTH_REALM+++[] | string | `default` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-auth-server-name"></a> [`quarkus.infinispan-client."client-name".auth-server-name`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-auth-server-name) config_property_copy_button:+++quarkus.infinispan-client."client-name".auth-server-name+++[]<br><br>Sets server name used by authentication<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__AUTH_SERVER_NAME+++[] | string | `infinispan` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-sasl-mechanism"></a> [`quarkus.infinispan-client."client-name".sasl-mechanism`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-sasl-mechanism) config_property_copy_button:+++quarkus.infinispan-client."client-name".sasl-mechanism+++[]<br><br>Sets SASL mechanism used by authentication. Available values: ++**++ `DIGEST-SHA-512` - Uses the SHA-512 hashing algorithm to securely hash and verify credentials. This is the default. ++**++ `DIGEST-MD5` - Uses the MD5 hashing algorithm in addition to nonces to encrypt credentials. ++**++ `EXTERNAL` - Uses client certificates to provide valid identities to Infinispan Server and enable encryption. ++**++ `PLAIN` - Sends credentials in plain text (unencrypted) over the wire in a way that is similar to HTTP BASIC authentication. You should use `PLAIN` authentication only in combination with TLS encryption.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SASL_MECHANISM+++[] | string | `DIGEST-SHA-512` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-store"></a> [`quarkus.infinispan-client."client-name".key-store`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-store) config_property_copy_button:+++quarkus.infinispan-client."client-name".key-store+++[]<br><br>Specifies the filename of a keystore to use to create the `SSLContext`. You also need to specify a keyStorePassword. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__KEY_STORE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-store-password"></a> [`quarkus.infinispan-client."client-name".key-store-password`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-store-password) config_property_copy_button:+++quarkus.infinispan-client."client-name".key-store-password+++[]<br><br>Specifies the password needed to open the keystore. You also need to specify a keyStore. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-store-type"></a> [`quarkus.infinispan-client."client-name".key-store-type`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-store-type) config_property_copy_button:+++quarkus.infinispan-client."client-name".key-store-type+++[]<br><br>Specifies the type of the keyStore, such as PKCS12.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__KEY_STORE_TYPE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-alias"></a> [`quarkus.infinispan-client."client-name".key-alias`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-key-alias) config_property_copy_button:+++quarkus.infinispan-client."client-name".key-alias+++[]<br><br>Sets the unique name used to identify a specific key pair in a keystore for secure connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__KEY_ALIAS+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-trust-store"></a> [`quarkus.infinispan-client."client-name".trust-store`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-trust-store) config_property_copy_button:+++quarkus.infinispan-client."client-name".trust-store+++[]<br><br>Specifies the filename of a truststore to use to create the `SSLContext`. You also need to specify a trustStorePassword. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__TRUST_STORE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-trust-store-password"></a> [`quarkus.infinispan-client."client-name".trust-store-password`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-trust-store-password) config_property_copy_button:+++quarkus.infinispan-client."client-name".trust-store-password+++[]<br><br>Specifies the password needed to open the truststore You also need to specify a trustStore. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-trust-store-type"></a> [`quarkus.infinispan-client."client-name".trust-store-type`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-trust-store-type) config_property_copy_button:+++quarkus.infinispan-client."client-name".trust-store-type+++[]<br><br>Specifies the type of the truststore, such as JKS or JCEKS. Defaults to JKS if trustStore is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__TRUST_STORE_TYPE+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-protocol"></a> [`quarkus.infinispan-client."client-name".ssl-protocol`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-protocol) config_property_copy_button:+++quarkus.infinispan-client."client-name".ssl-protocol+++[]<br><br>Configures the secure socket protocol. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SSL_PROTOCOL+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-provider"></a> [`quarkus.infinispan-client."client-name".ssl-provider`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-provider) config_property_copy_button:+++quarkus.infinispan-client."client-name".ssl-provider+++[]<br><br>Sets the ssl provider. For example BCFIPS Setting this implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SSL_PROVIDER+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-ciphers"></a> [`quarkus.infinispan-client."client-name".ssl-ciphers`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-ciphers) config_property_copy_button:+++quarkus.infinispan-client."client-name".ssl-ciphers+++[]<br><br>Configures the ciphers. Setting this property implicitly enables SSL/TLS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SSL_CIPHERS+++[] | list of string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-host-name-validation"></a> [`quarkus.infinispan-client."client-name".ssl-host-name-validation`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-ssl-host-name-validation) config_property_copy_button:+++quarkus.infinispan-client."client-name".ssl-host-name-validation+++[]<br><br>Do SSL hostname validation. Defaults to true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SSL_HOST_NAME_VALIDATION+++[] | boolean |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-sni-host-name"></a> [`quarkus.infinispan-client."client-name".sni-host-name`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-sni-host-name) config_property_copy_button:+++quarkus.infinispan-client."client-name".sni-host-name+++[]<br><br>SNI host name. Mandatory when SSL is enabled and host name validation is true.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SNI_HOST_NAME+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-socket-timeout"></a> [`quarkus.infinispan-client."client-name".socket-timeout`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-socket-timeout) config_property_copy_button:+++quarkus.infinispan-client."client-name".socket-timeout+++[]<br><br>Configures the socket timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__SOCKET_TIMEOUT+++[] | int |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-tracing-propagation-enabled"></a> [`quarkus.infinispan-client."client-name".tracing.propagation.enabled`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-tracing-propagation-enabled) config_property_copy_button:+++quarkus.infinispan-client."client-name".tracing.propagation.enabled+++[]<br><br>Whether a tracing propagation is enabled in case the Opentelemetry extension is present. By default the propagation of the context is propagated from the client to the Infinispan Server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__TRACING_PROPAGATION_ENABLED+++[] | boolean |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-configuration"></a> [`quarkus.infinispan-client."client-name".cache."cache".configuration`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-configuration) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".configuration+++[]<br><br>Cache configuration in inlined XML to create the cache on first access. Will be ignored if the configuration-uri is provided for the same cache name. An example of the user defined property: quarkus.infinispan-client.cache.bookscache.configuration=<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__CONFIGURATION+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-configuration-uri"></a> [`quarkus.infinispan-client."client-name".cache."cache".configuration-uri`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-configuration-uri) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".configuration-uri+++[]<br><br>Cache configuration file in XML, Json or YAML whose path will be converted to URI to create the cache on first access. An example of the user defined property. cacheConfig.xml file is located in the 'resources' folder: quarkus.infinispan-client.cache.bookscache.configuration-uri=cacheConfig.xml<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__CONFIGURATION_URI+++[] | string |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-near-cache-max-entries"></a> [`quarkus.infinispan-client."client-name".cache."cache".near-cache-max-entries`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-near-cache-max-entries) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".near-cache-max-entries+++[]<br><br>The maximum number of entries to keep locally for the specified cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__NEAR_CACHE_MAX_ENTRIES+++[] | int |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-near-cache-mode"></a> [`quarkus.infinispan-client."client-name".cache."cache".near-cache-mode`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-near-cache-mode) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".near-cache-mode+++[]<br><br>Sets near cache mode used by the Infinispan Client Available values: ++**++ `DISABLED` - Means that near caching is disabled. This is the default value. ++**++ `INVALIDATED` - Means is near caching is invalidated, so when entries are updated or removed server-side, invalidation messages will be sent to clients to remove them from the near cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__NEAR_CACHE_MODE+++[] | `disabled`, `invalidated` |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-near-cache-use-bloom-filter"></a> [`quarkus.infinispan-client."client-name".cache."cache".near-cache-use-bloom-filter`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-near-cache-use-bloom-filter) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".near-cache-use-bloom-filter+++[]<br><br>Enables bloom filter for near caching. Bloom filters optimize performance for write operations by reducing the total number of invalidation messages.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__NEAR_CACHE_USE_BLOOM_FILTER+++[] | boolean |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-transaction-mode"></a> [`quarkus.infinispan-client."client-name".cache."cache".transaction-mode`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-cache-cache-transaction-mode) config_property_copy_button:+++quarkus.infinispan-client."client-name".cache."cache".transaction-mode+++[]<br><br>Enables transaction mode in the client side, for transactional caches.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__CACHE__CACHE__TRANSACTION_MODE+++[] | `none`, `non-xa`, `non-durable-xa`, `full-xa` |  |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-backup-cluster-backup-cluster-hosts"></a> [`quarkus.infinispan-client."client-name".backup-cluster."backup-cluster".hosts`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-backup-cluster-backup-cluster-hosts) config_property_copy_button:+++quarkus.infinispan-client."client-name".backup-cluster."backup-cluster".hosts+++[]<br><br>Sets the host name/port to connect to. Each one is separated by a semicolon (eg. hostA:11222;hostB:11222).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__BACKUP_CLUSTER__BACKUP_CLUSTER__HOSTS+++[] | string | required [exclamation circle] |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-backup-cluster-backup-cluster-client-intelligence"></a> [`quarkus.infinispan-client."client-name".backup-cluster."backup-cluster".client-intelligence`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-backup-cluster-backup-cluster-client-intelligence) config_property_copy_button:+++quarkus.infinispan-client."client-name".backup-cluster."backup-cluster".client-intelligence+++[]<br><br>Sets client intelligence used by authentication Available values: ++**++ `BASIC` - Means that the client doesn't handle server topology changes and therefore will only use the list of servers supplied at configuration time. ++**++ `TOPOLOGY_AWARE` - Use this provider if you don't want the client to present any certificates to the remote TLS host. ++\*++ `HASH_DISTRIBUTION_AWARE` - Like `TOPOLOGY_AWARE` but with the additional advantage that each request involving keys will be routed to the server who is the primary owner which improves performance greatly. This is the default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__BACKUP_CLUSTER__BACKUP_CLUSTER__CLIENT_INTELLIGENCE+++[] | `basic`, `topology-aware`, `hash-distribution-aware` | `hash-distribution-aware` |
| <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-backup-cluster-backup-cluster-use-schema-registration"></a> [`quarkus.infinispan-client."client-name".backup-cluster."backup-cluster".use-schema-registration`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-backup-cluster-backup-cluster-use-schema-registration) config_property_copy_button:+++quarkus.infinispan-client."client-name".backup-cluster."backup-cluster".use-schema-registration+++[]<br><br>Enables or disables Protobuf generated schemas upload to the backup. Set it to 'false' when you need to handle the lifecycle of the Protobuf Schemas on Server side yourself. Default is 'true'. This setting will be ignored if the Global Setting is set up to false.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__BACKUP_CLUSTER__BACKUP_CLUSTER__USE_SCHEMA_REGISTRATION+++[] | boolean | `true` |
| <a id="quarkus-infinispan-client_section_quarkus-infinispan-client-client-name-devservices"></a> [Dev Services](#quarkus-infinispan-client_section_quarkus-infinispan-client-client-name-devservices) | Type | Default |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-enabled"></a> [`quarkus.infinispan-client."client-name".devservices.enabled`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-enabled) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.enabled+++[]<br><br>If DevServices has been explicitly enabled or disabled. DevServices is generally enabled by default, unless there is an existing configuration present.<br><br>When DevServices is enabled Quarkus will attempt to automatically configure and start a database when running in Dev or Test mode and when Docker is running.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-create-default-client"></a> [`quarkus.infinispan-client."client-name".devservices.create-default-client`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-create-default-client) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.create-default-client+++[]<br><br>When the configuration is empty, an Infinispan default client is automatically created to connect to the running dev service. However, there are scenarios where creating this client is unnecessary, yet we still need to spin up an Infinispan Server. In such cases, this property serves to determine whether the client should be created by default or not by the extension.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_CREATE_DEFAULT_CLIENT+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-port"></a> [`quarkus.infinispan-client."client-name".devservices.port`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-port) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-shared"></a> [`quarkus.infinispan-client."client-name".devservices.shared`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-shared) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.shared+++[]<br><br>Indicates if the Infinispan server managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Infinispan starts a new container.<br><br>The discovery uses the `quarkus-dev-service-infinispan` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-service-name"></a> [`quarkus.infinispan-client."client-name".devservices.service-name`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-service-name) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-infinispan` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Infinispan looks for a container with the `quarkus-dev-service-infinispan` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-infinispan` label set to the specified value.<br><br>This property is used when you need multiple shared Infinispan servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_SERVICE_NAME+++[] | string | `infinispan` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-image-name"></a> [`quarkus.infinispan-client."client-name".devservices.image-name`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-image-name) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.image-name+++[]<br><br>The image to use. Note that only official Infinispan images are supported.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_IMAGE_NAME+++[] | string | `quay.io/infinispan/server:latest` |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-artifacts"></a> [`quarkus.infinispan-client."client-name".devservices.artifacts`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-artifacts) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.artifacts+++[]<br><br>List of the artifacts to automatically download and add to the Infinispan server libraries.<br><br>For example a Maven coordinate (org.postgresql:postgresql:42.3.1) or a dependency location url.<br><br>If an invalid value is passed, the Infinispan server will throw an error when trying to start.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_ARTIFACTS+++[] | list of string |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-site"></a> [`quarkus.infinispan-client."client-name".devservices.site`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-site) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.site+++[]<br><br>Add a site name to start the Infinispan Server Container with Cross Site Replication enabled (ex. lon). Cross Site Replication is the capability to connect two separate Infinispan Server Clusters that might run in different Data Centers, and configure backup caches to copy the data across the clusters with active-active or active-passive replication. See more about Cross Site Replication in the Infinispan Documentation <https://infinispan.org/docs/stable/titles/xsite/xsite.html> Configure `mcast-port` to avoid forming a cluster with any other running Infinispan Server container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_SITE+++[] | string |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-mcast-port"></a> [`quarkus.infinispan-client."client-name".devservices.mcast-port`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-mcast-port) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.mcast-port+++[]<br><br>If you are running an Infinispan Server already in docker, if the containers use the same mcastPort they will form a cluster. Set a different mcastPort to create a separate cluster in Docker (e. 46656). A common use case in a local Docker development mode, is the need of having two different Infinispan Clusters with Cross Site Replication enabled. see <https://github.com/infinispan/infinispan-simple-tutorials/blob/main/infinispan-remote/cross-site-replication/docker-compose/><br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_MCAST_PORT+++[] | int |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-container-env-environment-variable-name"></a> [`quarkus.infinispan-client."client-name".devservices.container-env."environment-variable-name"`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-config-files"></a> [`quarkus.infinispan-client."client-name".devservices.config-files`](#quarkus-infinispan-client_quarkus-infinispan-client-client-name-devservices-config-files) config_property_copy_button:+++quarkus.infinispan-client."client-name".devservices.config-files+++[]<br><br>Infinispan Server configuration chunks to be passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_INFINISPAN_CLIENT__CLIENT_NAME__DEVSERVICES_CONFIG_FILES+++[] | list of string |  |
