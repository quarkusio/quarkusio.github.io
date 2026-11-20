# A Shot of Quarkus with a Cassandra Chaser: Time for Faster Microservices

With the new [Quarkus extension for Apache Cassandra](https://quarkus.io/guides/cassandra)® released in Quarkus 1.6, all it takes is a few lines of code to connect to and access your data stored in Cassandra and you get all of the benefits of Quarkus (fast-startup, low resource utilization, reactive programming).

To enable the extension, add `cassandra-quarkus-client` to your application. You can do this by including the following dependency or by selecting Cassandra client in the [project generator](https://code.quarkus.io/).

```xml
# the version at time of writing is 1.0.0-alpha3
<dependency>
  <groupId>com.datastax.oss.quarkus</groupId>
  <artifactId>cassandra-quarkus-client</artifactId>
  <version>${cassandra-quarkus.version}</version>
</dependency>
```

The most common database connection settings can be configured directly in an `application.properties` file on the classpath, see below for an example. All of the settings and features of the [DataStax Java driver for Cassandra](https://docs.datastax.com/en/developer/java-driver/latest) are available to you and the [advanced driver settings](https://docs.datastax.com/en/developer/java-driver/latest/manual/core/configuration/reference/) can be placed in an `application.conf` file on the classpath.

```properties
# Connecting to DataStax Astra astra.datastax.com
# if specified, contact-points and local-datacenter are not needed
quarkus.cassandra.cloud.secure-connect-bundle=/path/to/secure-connect-bundle.zip

# Connection details
quarkus.cassandra.contact-points=127.0.0.1:9042
quarkus.cassandra.local-datacenter=dc1
quarkus.cassandra.keyspace=<your keyspace>

# Authentication
quarkus.cassandra.auth.username=<your username>
quarkus.cassandra.auth.password=<your password>
```

Once the database connection is configured, simply inject the [`QuarkusCqlSession`](https://www.javadoc.io/static/com.datastax.oss.quarkus/cassandra-quarkus-client/1.0.0-alpha1/com/datastax/oss/quarkus/runtime/api/session/QuarkusCqlSession.html) in your application to get started. For seamless integration with [reactive applications](https://quarkus.io/guides/getting-started-reactive), there is an `executeReactive` method on the `QuarkusCqlSession` that exposes Mutiny-compatible reactive execution methods and direct retrieval of Mutiny types.

```java
@ApplicationScoped
public class ProductService {
  
  @Inject
  private QuarkusCqlSession session;

  public Multi<Product> getAllProducts() {
    Multi<ReactiveRow> products = session.executeReactive("SELECT * from products");
    return products.on().item().apply(Product::new);
  }
}
```

Mutiny types are also available when using the Cassandra Java driver's [object mapper](https://docs.datastax.com/en/developer/java-driver/4.7/manual/mapper/). With the Quarkus Cassandra extension, you can easily define your mapped entities and DAOs and then inject your DAOs in your reactive services in just a few lines of code!

For example, suppose that you have a `Product` entity, a `ProductDao` and a `ProductMapper` with standard CRUD operations, you can expose your DAO as an injectable bean:

```java
public class ProductDaoProducer {

  private final ProductDao dao;

  @Inject
  public ProductDaoProducer(QuarkusCqlSession cqlSession) {
    ProductMapper mapper = new ProductMapperBuilder(cqlSession).build();
    this.dao = mapper.productDao();
  }

  @Produces @ApplicationScoped
  public ProductDao produceProductDao() { return dao; }
}
```

… then inject it in a `ProductService` component, as follows:

```java
@ApplicationScoped
public class ProductService {

  @Inject
  private ProductDao dao;

  public Uni<Product> findProduct(String id) { return dao.findById(id); }

  public Multi<Product> getAllProducts() { return dao.findAll(); }
}
```

You can retrieve this example and many others in [the quickstart guide](https://github.com/datastax/cassandra-quarkus/blob/main/documentation/src/main/asciidoc/cassandraclient.adoc), which includes how to use the Quarkus Cassandra extension in [native mode](https://quarkus.io/guides/building-native-image). For users that are looking to get up and running in minutes with Cassandra, try out the free-tier in [DataStax Astra](https://astra.datastax.com/).

We value your feedback so please don't hesitate to open feature requests and bug reports in the [extensions GitHub repository](https://github.com/datastax/cassandra-quarkus), ask questions at [community.datastax.com](https://community.datastax.com/), or join the [Cassandra](https://cassandra.apache.org/community/) and [Quarkus](https://quarkus.io/community/) communities.

Happy subatomic coding!
