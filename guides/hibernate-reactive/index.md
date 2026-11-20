# Using Hibernate Reactive

[Hibernate Reactive](https://hibernate.org/reactive/) is a reactive API for Hibernate ORM, supporting non-blocking database drivers and a reactive style of interaction with the database.

> [!NOTE]
> Hibernate Reactive is not a replacement for [Hibernate ORM](/guides/hibernate-orm/index.md) or the future of Hibernate ORM. It is a different stack tailored for reactive use cases where you need high-concurrency.
>
> Also, using Quarkus REST (formerly RESTEasy Reactive), our default REST layer, does not require the use of Hibernate Reactive. It is perfectly valid to use Quarkus REST with Hibernate ORM, and if you do not need high-concurrency, or are not accustomed to the reactive paradigm, it is recommended to use Hibernate ORM.

> [!NOTE]
> Hibernate Reactive works with the same annotations and most of the configuration described in the [Hibernate ORM guide](/guides/hibernate-orm/index.md). This guide will only focus on what's specific for Hibernate Reactive.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `hibernate-reactive-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-reactive-quickstart).

<a id="hr-getting-started"></a>
## Setting up and configuring Hibernate Reactive

When using Hibernate Reactive in Quarkus, you need to:

- add your configuration settings in `application.properties`
- annotate your entities with `@Entity` and any other mapping annotations as usual

Other configuration needs have been automated: Quarkus will make some opinionated choices and educated guesses.

Add the following dependencies to your project:

- the Hibernate Reactive extension: `io.quarkus:quarkus-hibernate-reactive`
- the [Reactive SQL client extension](/guides/reactive-sql-clients/index.md) for the database of your choice; the following options are available: - `quarkus-reactive-pg-client`: [the client for PostgreSQL or CockroachDB](https://vertx.io/docs/vertx-pg-client/java)  - `quarkus-reactive-mysql-client`: [the client MySQL or MariaDB](https://vertx.io/docs/vertx-mysql-client/java)  - `quarkus-reactive-mssql-client`: [the client for Microsoft SQL Server](https://vertx.io/docs/vertx-mssql-client/java)  - `quarkus-reactive-db2-client`: [the client for IBM Db2](https://vertx.io/docs/vertx-db2-client/java)  - `quarkus-reactive-oracle-client`: [the client for Oracle](https://vertx.io/docs/vertx-oracle-client/java)

For instance:

**pom.xml**

```xml
<!-- Hibernate Reactive dependency -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-reactive</artifactId>
</dependency>

<!-- Reactive SQL client for PostgreSQL -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-reactive-pg-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
// Hibernate Reactive dependency
implementation("io.quarkus:quarkus-hibernate-reactive")

Reactive SQL client for PostgreSQL
implementation("io.quarkus:quarkus-reactive-pg-client")
```

Annotate your persistent objects with `@Entity`, then add the relevant configuration properties in `application.properties`:

**Example `application.properties`**

```properties
quarkus.datasource.db-kind = postgresql <1>

%prod.quarkus.datasource.username = hibernate
%prod.quarkus.datasource.password = hibernate
%prod.quarkus.datasource.reactive.url = vertx-reactive:postgresql://localhost/quarkus_test <2>
%prod.quarkus.hibernate-orm.schema-management.strategy=create <3>
```

1. [Configure the datasource](/guides/datasource/index.md) for production, relying on [Dev Services](/guides/datasource/index.md#dev-services) for connection information in tests / dev mode.
2. The only different property from a Hibernate ORM configuration
3. Configure Hibernate ORM to create the schema on startup in production, which is useful for experimentation, but rely on [convenient defaults in tests / dev mode](/guides/hibernate-orm/index.md#dev-mode).

Note that these configuration properties are not the same ones as in your typical Hibernate Reactive configuration file. They will often map to Hibernate Reactive configuration properties but could have different names and don't necessarily map 1:1 to each other.

Blocking (non-reactive) and reactive configuration [can be mixed together in the same project](#orm-and-reactive-extension-simultaneously).

> [!WARNING]
> Configuring Hibernate Reactive using the standard `persistence.xml` configuration file is not supported.

See section [Configuration Reference for Hibernate Reactive](#configuration-reference) for the list of properties you can set in `application.properties`.

A `Mutiny.SessionFactory` will be created based on the Quarkus `datasource` configuration as long as the Hibernate Reactive extension is listed among your project dependencies.

The dialect will be selected based on the Reactive SQL client - unless you set one explicitly.

> [!NOTE]
> For more information on dialect selection and database versions, see [the corresponding section of the Hibernate ORM guide](/guides/hibernate-orm/index.md#hibernate-dialect).

You can then happily inject your `Mutiny.SessionFactory`:

**Example application bean using Hibernate Reactive**

```java
@ApplicationScoped
public class SantaClausService {
    @Inject
    Mutiny.SessionFactory sf; <1>

    public Uni<Void> createGift(String giftDescription) {
	Gift gift = new Gift();
        gift.setName(giftDescription);
	return sf.withTransaction(session -> session.persist(gift)) <2>
    }
}
```

1. Inject your session factory and have fun
2. `.withTransaction()` will automatically flush at commit

> [!WARNING]
> Make sure to wrap methods modifying your database (e.g. `session.persist(entity)`) within a transaction.

**Example of an Entity**

```java
@Entity
public class Gift {
    private Long id;
    private String name;

    @Id
    @SequenceGenerator(name = "giftSeq", sequenceName = "gift_id_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(generator = "giftSeq")
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
}
```

To load SQL statements when Hibernate Reactive starts, add an `import.sql` file in your `src/main/resources/` directory. This script can contain any SQL DML statements. Make sure to terminate each statement with a semicolon.

This is useful to have a data set ready for your tests or demos.

### Using `@Transactional` with Hibernate Reactive

You can use the standard `jakarta.transaction.Transactional` annotation with Hibernate Reactive. This provides a more familiar programming model for developers coming from Hibernate ORM.

While defining transaction handling using the annotation, you should inject `Mutiny.Session` or `Mutiny.StatelessSession` using CDI and use them in methods annotated with `@Transactional`.

Here's an example showing how to use `@Transactional` in a REST resource:

**Example REST resource using @Transactional**

```java
@Path("/fruits")
public class FruitResource {

    @Inject
    Mutiny.Session session; <1>

    @GET
    @Path("/{id}")
    public Uni<Fruit> getFruit(Long id) {
        return session.find(Fruit.class, id);
    }

    @POST
    @Transactional <2>
    public Uni<Fruit> createFruit(Fruit fruit) {
        return session.persist(fruit)
                .chain(() -> session.find(Fruit.class, fruit.getId()));
    }

    @PUT
    @Path("/{id}")
    @Transactional <3>
    public Uni<Fruit> updateFruit(Long id, @QueryParam("name") String newName) {
        return session.find(Fruit.class, id)
                .map(fruit -> {
                    fruit.setName(newName);
                    return fruit;
                });
    }

}
```

1. Inject the reactive session directly
2. Use `@Transactional` for persist operations - creates new entities in the database
3. Use `@Transactional` for update operations - modifies existing entities

The `@Transactional` interceptor will:

- Lazily open a Hibernate Reactive session when first accessed
- Begin a transaction automatically
- Commit the transaction when the `Uni` completes successfully
- Roll back the transaction if an exception is thrown or the `Uni` is cancelled
- Close the session and release the database connection when the reactive chain completes

> [!IMPORTANT]
> There are some limitations when using `@Transactional` with Hibernate Reactive, in particular not being able to use a specific `TxType`, not being able to use multiple persistence units / datasources, or non-functional `Uni.combine()`/`Uni.joining()`.
>
> See [Limitations and other things you should know](#hr-limitations) for more information.

<a id="hr-configuration-properties"></a>
### Hibernate Reactive configuration properties

There are various optional properties useful to refine your session factory or guide Quarkus' guesses.

When no properties are set, Quarkus can typically infer everything it needs to set up Hibernate Reactive and will have it use the default datasource.

The configuration properties listed here allow you to override such defaults, and customize and tune various aspects.

Hibernate Reactive uses the same properties you would use for Hibernate ORM: see [Configuration Reference for Hibernate Reactive](#configuration-reference).

<a id="orm-and-reactive-extension-simultaneously"></a>
### Hibernate ORM and Reactive extensions simultaneously

If you add both Hibernate ORM and Hibernate Reactive extensions to your Quarkus app, they can be mixed together in the same project.

This is useful if your app normally uses Hibernate ORM (which is blocking), but you want to try Hibernate Reactive to see if it works better for your case.

By adding the second extension, you can use the reactive API in another part of your code - without needing to create a separate app.

> [!NOTE]
> Hibernate ORM and Hibernate Reactive won't share the same persistence context, so it's recommended you stick to one or the other in a given method. For example use Hibernate ORM in blocking REST endpoints, and use Hibernate Reactive in reactive REST endpoints.

- To use the both extension simultaneously, add both extension to the `pom.xml` file:

  ```xml
          <!-- Hibernate reactive -->
          <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-hibernate-reactive</artifactId>
          </dependency>
          <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-reactive-pg-client</artifactId>
          </dependency>

          <!-- Hibernate ORM -->
          <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-jdbc-postgresql</artifactId>
          </dependency>
          <dependency>
              <groupId>io.quarkus</groupId>
              <artifactId>quarkus-hibernate-orm</artifactId>
          </dependency>
  ```


- Also update the `applications.properties` file:

  ```properties
  %prod.quarkus.datasource.reactive.url=postgresql:///your_database
  %prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/hibernate_orm_test
  ```


- The presence of the JDBC driver will enable Hibernate ORM. If you want to disable it, and only use Hibernate Reactive, you can use:

  ```properties
  quarkus.hibernate-orm.jdbc.enabled=false
  ```


Quarkus will set many Hibernate Reactive configuration settings automatically, and will often use more modern defaults.

<a id="hr-cdi-integration"></a>
#### CDI integration

If you are familiar with using Hibernate Reactive in Quarkus, you probably already have injected the `Mutiny.SessionFactory` using CDI:

```java
@Inject
Mutiny.SessionFactory sessionFactory;
```

This will inject the `Mutiny.SessionFactory` of the default persistence unit.

> [!NOTE]
> Prior to Quarkus 3.0 it was also possible to inject a `@RequestScoped` bean for `Mutiny.Session`. However, the lifecycle of a reactive session does not fit the lifecycle of the CDI request context. Therefore, this bean is removed in Quarkus 3.0.

<a id="persistence-unit-active"></a>
### Activate/deactivate persistence units

When a persistence unit is configured at build time, and it is assigned entity types or an [active datasource](/guides/datasource/index.md#datasource-active), the persistence unit is active by default. Quarkus starts the corresponding Hibernate Reactive `SessionFactory` when the application starts.

To deactivate a persistence unit at runtime, see [the corresponding section of the Hibernate ORM guide](/guides/hibernate-orm/index.md#persistence-unit-active).

> [!NOTE]
> If you decide to follow the example from the Hibernate ORM guide to declare a custom CDI bean for the active persistence unit, but you use Hibernate Reactive, make sure to work with the `Mutiny.SessionFactory` type instead of `Session`: this is one way Hibernate Reactive's API entrypoint differs from Hibernate ORM's.
>
> See [CDI integration](#hr-cdi-integration) for details.

<a id="flyway"></a>
## Automatically transitioning to Flyway to Manage Schemas

Hibernate Reactive can be used in the same application as Flyway. See [this section of the Flyway extension documentation](/guides/flyway/index.md#reactive-datasources) for details regarding configuration of Flyway in a reactive application.

> [!TIP]
> If you have the [Flyway extension](/guides/flyway/index.md) installed when running in development mode, Quarkus provides a simple way to initialize your Flyway configuration using the schema generated automatically by Hibernate Reactive.
>
> See [the Hibernate ORM guide](/guides/hibernate-orm/index.md#flyway) for more details.

<a id="testing"></a>
### Testing

Using Hibernate Reactive in a `@QuarkusTest` is slightly more involved than using Hibernate ORM due to the asynchronous nature of the APIs and the fact that all operations need to run on a Vert.x Event Loop.

Two components are necessary to write these tests:

- The use of `@io.quarkus.test.vertx.RunOnVertxContext` or `@io.quarkus.test.TestReactiveTransaction` on the test methods
- The use of `io.quarkus.test.vertx.UniAsserter` as a test method parameter.

> [!IMPORTANT]
> These classes are provided by the `quarkus-test-vertx` dependency.

A very simple example usage looks like:

```java
@QuarkusTest
public class SomeTest {

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void testQuery(UniAsserter asserter) {
        asserter.assertThat(() -> sessionFactory.withSession(s -> s.createQuery(
                "from Gift g where g.name = :name").setParameter("name", "Lego").getResultList()),
                list -> org.junit.jupiter.api.Assertions.assertEquals(list.size(), 1));
    }

}
```

> [!NOTE]
> See the Javadoc of `UniAsserter` for a full description of the various methods that can be used for creating assertions.

> [!TIP]
> You can also extend the `io.quarkus.test.vertx.UniAsserterInterceptor` to wrap the injected `UniAsserter` and customize the default behavior. For example, the interceptor can be used to execute the assert methods within a separate database transaction.:
>
> ```java
> @QuarkusTest
> public class SomeTest {
>
>    @Test
>    @RunOnVertxContext
>    public void testEntity(UniAsserter asserter) {
>       asserter = new UniAsserterInterceptor(asserter) {
>          @Override
>          protected <T> Supplier<Uni<T>> transformUni(Supplier<Uni<T>> uniSupplier) {
>             return () -> Panache.withTransaction(uniSupplier);
>          }
>       };
>       asserter.execute(() -> new MyEntity().persist());
>       asserter.assertEquals(() -> MyEntity.count(), 1l);
>       asserter.execute(() -> MyEntity.deleteAll());
>    }
> }
> ```

<a id="multiple-persistence-units"></a>
### Multiple persistence units

#### Setting up multiple persistence units

In a similar fashion to Hibernate ORM, Hibernate Reactive supports multiple persistence units.

You can define multiple persistence units and datasources, and they can mix blocking and reactive datasources. To ensure that a datasource supports reactive, you need to set the `reactive` property to `true`.

**Example `application.properties`**

```properties
quarkus.datasource."users".reactive.url=vertx-reactive:postgresql://localhost/users <1>
quarkus.datasource."users".db-kind=postgresql
%prod.quarkus.datasource."users".username=hibernate_orm_test
%prod.quarkus.datasource."users".password=hibernate_orm_test

quarkus.datasource."inventory".reactive.url=vertx-reactive:postgresql://localhost/inventory <2>
quarkus.datasource."inventory".db-kind=postgresql
%prod.quarkus.datasource."inventory".username=hibernate_orm_test
%prod.quarkus.datasource."inventory".password=hibernate_orm_test

quarkus.hibernate-orm."users".datasource=users <3>
quarkus.hibernate-orm."users".packages=io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.user

quarkus.hibernate-orm."inventory".datasource=inventory <4>
quarkus.hibernate-orm."inventory".packages=io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory
```

1. Define a reactive datasource named `users`.
2. Define a reactive datasource named `inventory`.
3. Define a persistence unit named `users` and specify the datasource.
4. Define a persistence unit named `inventory` and specify the datasource.

When using named persistence units, you must set the `datasource` property to the name of the corresponding datasource.

<a id="hr-limitations"></a>
## Limitations and other things you should know

Quarkus does not modify the libraries it uses; this rule applies to Hibernate Reactive as well: when using this extension you will mostly have the same experience as using the original library.

But while they share the same code, Quarkus does configure some components automatically and inject custom implementations for some extension points; this should be transparent and useful but if you're an expert of Hibernate Reactive you might want to know what is being done.

### General limitations

- Hibernate Reactive is not configurable via a `persistence.xml` file.
- This extension only considers the default persistence unit at the moment: it's not possible to configure multiple persistence units, or even a single named persistence unit.
- This extension does not support [database-based multitenancy](/guides/hibernate-orm/index.md#database-approach) or [schema-based multitenancy](/guides/hibernate-orm/index.md#schema-approach) at the moment. [Discriminator-based multitenancy](/guides/hibernate-orm/index.md#discriminator-approach), on the other hand, is expected to work correctly. See <https://github.com/quarkusio/quarkus/issues/15959>.
- Integration with the Envers extension is not supported.

### Transaction management: choosing between `@Transactional` and Panache annotations

With the introduction of the `quarkus-reactive-transactions` extension, you now have two options for managing transactions in Hibernate Reactive applications:

1. Use `jakarta.transaction.Transactional` for declarative transaction management
2. Use Panache's transaction annotations (`@WithTransaction`, `@WithSession`, `@WithSessionOnDemand`, or the programmatic `Panache.withTransaction()`)

You must choose one approach and use it consistently throughout your application. Mixing both transaction management styles in the same reactive pipeline is not supported and will result in an `UnsupportedOperationException`. In the future, we'll deprecate the previous annotations provided by Panache and and support only `@Transactional`.

You can inject either `Mutiny.Session` or `Mutiny.StatelessSession`. Mixing both session types in the same transaction should work, but should be reserved for exotic use cases implemented by advanced users, as the (stateful) session will not be aware of changes operated through the stateless session, which could thus conflict or be silently erased by (stateful) session writes.

<a id="transactional-different-pipelines"></a>
### Using Declarative Transaction Management in different reactive pipelines

When using declarative transaction management with Vert.x context-based interceptors (`@Transactional`, `@WithTransaction`, `@WithSession`, `@WithSessionOnDemand`) in multiple methods and combining such methods with either `Uni.combine().all().unis()` or `Uni.join().all()` the same transaction might be shared by different reactive pipelines causing unpredictable behavior.

For this reason, **avoid using declarative transactional management with those methods**.

<a id="transactional-others"></a>
### Other Declarative Transactional Management limitations

Reactive transactions does not use `TransactionManager`, thus they are local only and do not support XA transactions. Every parameter defined on the `TransactionManager` (such as `quarkus.transaction-manager.default-transaction-timeout`) will be ignored. Only a single datasource can participate in a transaction.

Reactive transactions also work exclusively within the reactive pipeline. If blocking code is executed (for example on a worker thread) as part of that pipeline, it will not be able to participate in the transaction.

Declarative reactive transactions with `@Transactional` can only be applied to methods returning `Uni`, not `Multi`, `CompletionStage`, or `CompletableFuture`.

Currently, only `Transactional.TxType.REQUIRED` is supported with reactive transactions. Other transaction types (`REQUIRES_NEW`, `MANDATORY`, etc.) will throw an `UnsupportedOperationException`.

## Simplifying Hibernate Reactive with Panache

The [Hibernate Reactive with Panache](/guides/hibernate-reactive-panache/index.md) extension facilitates the usage of Hibernate Reactive by providing active record style entities (and repositories) and focuses on making your entities trivial and fun to write in Quarkus.

## Validation modes and Hibernate Validator integration

To find out more on how the [`quarkus.hibernate-orm.validation.mode` configuration property](#quarkus-hibernate-orm_quarkus-hibernate-orm-validation-mode). influence your Hibernate Reactive application see the [corresponding Hibernate ORM guide](/guides/hibernate-orm/index.md#validator_integration), as these modes work the same in both cases.

<a id="configuration-reference"></a>
## Configuration Reference for Hibernate Reactive

> [!TIP]
> You will notice that some properties contain "jdbc" in their name: this is because Hibernate ORM refers to its "data access" layer as "JDBC" for historical reasons. Hibernate Reactive uses Vert.x Reactive SQL clients for its data access layer rather than JDBC.
>
> Regardless of their name, these properties still make sense for Hibernate Reactive.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-enabled"></a> [`quarkus.hibernate-orm.enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-enabled) config_property_copy_button:+++quarkus.hibernate-orm.enabled+++[]<br><br>Whether Hibernate ORM is enabled **during the build**.<br><br>If Hibernate ORM is disabled during the build, all processing related to Hibernate ORM will be skipped, but it will not be possible to activate Hibernate ORM at runtime: `quarkus.hibernate-orm.active` will default to `false` and setting it to `true` will lead to an error.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-persistence-xml-ignore"></a> [`quarkus.hibernate-orm.persistence-xml.ignore`](#quarkus-hibernate-orm_quarkus-hibernate-orm-persistence-xml-ignore) config_property_copy_button:+++quarkus.hibernate-orm.persistence-xml.ignore+++[]<br><br>If `true`, Quarkus will ignore any `persistence.xml` file in the classpath and rely exclusively on the Quarkus configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_PERSISTENCE_XML_IGNORE+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-statistics"></a> [`quarkus.hibernate-orm.statistics`](#quarkus-hibernate-orm_quarkus-hibernate-orm-statistics) config_property_copy_button:+++quarkus.hibernate-orm.statistics+++[]<br><br>Whether statistics collection is enabled. If 'metrics.enabled' is true, then the default here is considered true, otherwise the default is false.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_STATISTICS+++[] | boolean |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-session-metrics"></a> [`quarkus.hibernate-orm.log-session-metrics`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-session-metrics) config_property_copy_button:+++quarkus.hibernate-orm.log-session-metrics+++[]<br><br>Whether session metrics should be appended into the server log for each Hibernate session. This only has effect if statistics are enabled (`quarkus.hibernate-orm.statistics`). The default is false (which means both `statistics` and `log-session-metrics` need to be enabled for the session metrics to appear in the log).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_SESSION_METRICS+++[] | boolean |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-metrics-enabled"></a> [`quarkus.hibernate-orm.metrics.enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-metrics-enabled) config_property_copy_button:+++quarkus.hibernate-orm.metrics.enabled+++[]<br><br>Whether metrics are published if a metrics extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_METRICS_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dev-ui-allow-hql"></a> [`quarkus.hibernate-orm.dev-ui.allow-hql`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dev-ui-allow-hql) config_property_copy_button:+++quarkus.hibernate-orm.dev-ui.allow-hql+++[]<br><br>Allow hql queries in the Dev UI page<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DEV_UI_ALLOW_HQL+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-request-scoped-enabled"></a> [`quarkus.hibernate-orm.request-scoped.enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-request-scoped-enabled) config_property_copy_button:+++quarkus.hibernate-orm.request-scoped.enabled+++[]<br><br>Enable or disable access to a Hibernate ORM `EntityManager`/`Session`/`StatelessSession` ++**++when no transaction is active++**++ but a request scope is. When enabled, the corresponding sessions will be read-only. Defaults to enabled for backwards compatibility, but disabling this is recommended, to avoid inconsistent resulsts caused by queries running outside of transactions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_REQUEST_SCOPED_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-datasource"></a> [`quarkus.hibernate-orm.datasource`](#quarkus-hibernate-orm_quarkus-hibernate-orm-datasource) config_property_copy_button:+++quarkus.hibernate-orm.datasource+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".datasource` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".datasource+++[]<br><br>The name of the datasource which this persistence unit uses.<br><br>If undefined, it will use the default datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATASOURCE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-packages"></a> [`quarkus.hibernate-orm.packages`](#quarkus-hibernate-orm_quarkus-hibernate-orm-packages) config_property_copy_button:+++quarkus.hibernate-orm.packages+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".packages` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".packages+++[]<br><br>The packages in which the entities affected to this persistence unit are located.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_PACKAGES+++[] | list of string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-sql-load-script"></a> [`quarkus.hibernate-orm.sql-load-script`](#quarkus-hibernate-orm_quarkus-hibernate-orm-sql-load-script) config_property_copy_button:+++quarkus.hibernate-orm.sql-load-script+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".sql-load-script` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".sql-load-script+++[]<br><br>Paths to files containing the SQL statements to execute when Hibernate ORM starts.<br><br>The files are retrieved from the classpath resources, so they must be located in the resources directory (e.g. `src/main/resources`).<br><br>The default value for this setting differs depending on the Quarkus launch mode:<br><br>- In dev and test modes, it defaults to `import.sql`. Simply add an `import.sql` file in the root of your resources directory and it will be picked up without having to set this property. Pass `no-file` to force Hibernate ORM to ignore the SQL import file.<br>- In production mode, it defaults to `no-file`. It means Hibernate ORM won't try to execute any SQL import file by default. Pass an explicit value to force Hibernate ORM to execute the SQL import file.<br><br>If you need different SQL statements between dev mode, test (`@QuarkusTest`) and in production, use Quarkus [configuration profiles facility](https://quarkus.io/guides/config#configuration-profiles).<br><br>**application.properties**<br><br>```property<br>%dev.quarkus.hibernate-orm.sql-load-script = import-dev.sql<br>%test.quarkus.hibernate-orm.sql-load-script = import-test.sql<br>%prod.quarkus.hibernate-orm.sql-load-script = no-file<br>```<br><br>> [!NOTE]<br>> Quarkus supports files with SQL statements or comments spread over multiple lines, or `.zip` files containing those files. Each SQL statement must be terminated by a semicolon.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SQL_LOAD_SCRIPT+++[] | list of string | `import.sql in dev and test modes ; no-file otherwise` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-physical-naming-strategy"></a> [`quarkus.hibernate-orm.physical-naming-strategy`](#quarkus-hibernate-orm_quarkus-hibernate-orm-physical-naming-strategy) config_property_copy_button:+++quarkus.hibernate-orm.physical-naming-strategy+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".physical-naming-strategy` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".physical-naming-strategy+++[]<br><br>Pluggable strategy contract for applying physical naming rules for database object names. Class name of the Hibernate PhysicalNamingStrategy implementation<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_PHYSICAL_NAMING_STRATEGY+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-implicit-naming-strategy"></a> [`quarkus.hibernate-orm.implicit-naming-strategy`](#quarkus-hibernate-orm_quarkus-hibernate-orm-implicit-naming-strategy) config_property_copy_button:+++quarkus.hibernate-orm.implicit-naming-strategy+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".implicit-naming-strategy` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".implicit-naming-strategy+++[]<br><br>Pluggable strategy for applying implicit naming rules when an explicit name is not given. Class name of the Hibernate ImplicitNamingStrategy implementation<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_IMPLICIT_NAMING_STRATEGY+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-files"></a> [`quarkus.hibernate-orm.mapping-files`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-files) config_property_copy_button:+++quarkus.hibernate-orm.mapping-files+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping-files` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping-files+++[]<br><br>XML files to configure the entity mapping, e.g. `META-INF/my-orm.xml`.<br><br>Defaults to `META-INF/orm.xml` if it exists. Pass `no-file` to force Hibernate ORM to ignore `META-INF/orm.xml`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_FILES+++[] | list of string | `META-INF/orm.xml if it exists; no-file otherwise` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-quote-identifiers-strategy"></a> [`quarkus.hibernate-orm.quote-identifiers.strategy`](#quarkus-hibernate-orm_quarkus-hibernate-orm-quote-identifiers-strategy) config_property_copy_button:+++quarkus.hibernate-orm.quote-identifiers.strategy+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".quote-identifiers.strategy` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".quote-identifiers.strategy+++[]<br><br>Identifiers can be quoted using one of the available strategies.<br><br>Set to `none` by default, meaning no identifiers will be quoted. If set to `all`, all identifiers and column definitions will be quoted. Additionally, setting it to `all-except-column-definitions` will skip the column definitions, which can usually be required when they exist, or else use the option `only-keywords` to quote only identifiers deemed SQL keywords by the Hibernate ORM dialect.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_QUOTE_IDENTIFIERS_STRATEGY+++[] | tooltip:none[Identifiers are not quoted.], tooltip:all[All identifiers are quoted.], tooltip:all-except-column-definitions[All identifiers,opts=except column definitions,are quoted.], tooltip:only-keywords[Only keywords will be quoted.] | tooltip:none[Identifiers are not quoted.] |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-second-level-caching-enabled"></a> [`quarkus.hibernate-orm.second-level-caching-enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-second-level-caching-enabled) config_property_copy_button:+++quarkus.hibernate-orm.second-level-caching-enabled+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".second-level-caching-enabled` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".second-level-caching-enabled+++[]<br><br>The default in Quarkus is for 2nd level caching to be enabled, and a good implementation is already integrated for you.<br><br>Just cherry-pick which entities should be using the cache.<br><br>Set this to false to disable all 2nd level caches.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SECOND_LEVEL_CACHING_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-validation-mode"></a> [`quarkus.hibernate-orm.validation.mode`](#quarkus-hibernate-orm_quarkus-hibernate-orm-validation-mode) config_property_copy_button:+++quarkus.hibernate-orm.validation.mode+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".validation.mode` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".validation.mode+++[]<br><br>Defines how the Bean Validation integration behaves.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_VALIDATION_MODE+++[] | list of tooltip:auto[If a Bean Validation provider is present then behaves as if both `ValidationMode#CALLBACK` and `ValidationMode#DDL` modes are configured. Otherwise,opts=same as `ValidationMode#NONE`.], tooltip:callback[Bean Validation will perform the lifecycle event validation.], tooltip:ddl[Bean Validation constraints will be considered for the DDL operations.], tooltip:none[Bean Validation integration will be disabled.] | `auto` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-multitenant"></a> [`quarkus.hibernate-orm.multitenant`](#quarkus-hibernate-orm_quarkus-hibernate-orm-multitenant) config_property_copy_button:+++quarkus.hibernate-orm.multitenant+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".multitenant` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".multitenant+++[]<br><br>Defines the method for multi-tenancy (DATABASE, NONE, SCHEMA). The complete list of allowed values is available in the [https://javadoc.io/doc/org.hibernate/hibernate-core/5.6.10.Final/org/hibernate/MultiTenancyStrategy.html\[Hibernate](https://javadoc.io/doc/org.hibernate/hibernate-core/5.6.10.Final/org/hibernate/MultiTenancyStrategy.html[Hibernate) ORM JavaDoc\]. The type DISCRIMINATOR is currently not supported. The default value is NONE (no multi-tenancy).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MULTITENANT+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-validate-in-dev-mode"></a> [`quarkus.hibernate-orm.validate-in-dev-mode`](#quarkus-hibernate-orm_quarkus-hibernate-orm-validate-in-dev-mode) config_property_copy_button:+++quarkus.hibernate-orm.validate-in-dev-mode+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".validate-in-dev-mode` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".validate-in-dev-mode+++[]<br><br>If hibernate is not auto generating the schema, and Quarkus is running in development mode then Quarkus will attempt to validate the database after startup and print a log message if there are any problems.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_VALIDATE_IN_DEV_MODE+++[] | boolean | `true` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-active"></a> [`quarkus.hibernate-orm.active`](#quarkus-hibernate-orm_quarkus-hibernate-orm-active) config_property_copy_button:+++quarkus.hibernate-orm.active+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".active` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".active+++[]<br><br>Whether this persistence unit should be active at runtime.<br><br>See [this section of the documentation](/guides/hibernate-orm/index.md#persistence-unit-active).<br><br>Note that if Hibernate ORM is disabled (i.e. `quarkus.hibernate-orm.enabled` is set to `false`), all persistence units are deactivated, and setting this property to `true` will fail.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_ACTIVE+++[] | boolean | `` `true` if Hibernate ORM is enabled and there are entity types or an active datasource assigned to the persistence unit; `false` otherwise `` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-unsupported-properties-full-property-key"></a> [`quarkus.hibernate-orm.unsupported-properties."full-property-key"`](#quarkus-hibernate-orm_quarkus-hibernate-orm-unsupported-properties-full-property-key) config_property_copy_button:+++quarkus.hibernate-orm.unsupported-properties."full-property-key"+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".unsupported-properties."full-property-key"` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".unsupported-properties."full-property-key"+++[]<br><br>Properties that should be passed on directly to Hibernate ORM. Use the full configuration property key here, for instance `quarkus.hibernate-orm.unsupported-properties."hibernate.order_inserts" = true`.<br><br>> [!WARNING]<br>> Properties set here are completely unsupported: as Quarkus doesn't generally know about these properties and their purpose, there is absolutely no guarantee that they will work correctly, and even if they do, that may change when upgrading to a newer version of Quarkus (even just a micro/patch version).<br><br>Consider using a supported configuration property before falling back to unsupported ones. If none exists, make sure to file a feature request so that a supported configuration property can be added to Quarkus, and more importantly so that the configuration property is tested regularly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_UNSUPPORTED_PROPERTIES__FULL_PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-blocking"></a> <mark>link:#quarkus-hibernate-orm_quarkus-hibernate-orm-blocking\[`quarkus.hibernate-orm.blocking`\]</mark> config_property_copy_button:+++quarkus.hibernate-orm.blocking+++[]<br><br>*This property is deprecated since `3.39`: Use `quarkus.hibernate-orm.jdbc.enabled` for the default persistence unit, and `quarkus.hibernate-orm."<persistence-unit-name>".jdbc.enabled` for named persistence units, to configure this on a per-persistence-unit basis instead.*<br><br>Whether Hibernate ORM is working in blocking mode.<br><br>Hibernate ORM's blocking `EntityManager`/`Session`/`SessionFactory` are normally disabled by default if no JDBC datasource is found. You can set this property to `false` if you want to disable them despite having a JDBC datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_BLOCKING+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-batch-fetch-size"></a> <mark>link:#quarkus-hibernate-orm_quarkus-hibernate-orm-batch-fetch-size\[`quarkus.hibernate-orm.batch-fetch-size`\]</mark> config_property_copy_button:+++quarkus.hibernate-orm.batch-fetch-size+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".batch-fetch-size` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".batch-fetch-size+++[]<br><br>*This property is deprecated: Use `quarkus.hibernate-orm.fetch.batch-size` to configure the batch fetch size.*<br><br>The size of the batches used when loading entities and collections.<br><br>`-1` means batch loading is disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_BATCH_FETCH_SIZE+++[] | int | `16` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-max-fetch-depth"></a> <mark>link:#quarkus-hibernate-orm_quarkus-hibernate-orm-max-fetch-depth\[`quarkus.hibernate-orm.max-fetch-depth`\]</mark> config_property_copy_button:+++quarkus.hibernate-orm.max-fetch-depth+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".max-fetch-depth` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".max-fetch-depth+++[]<br><br>*This property is deprecated: Use `quarkus.hibernate-orm.fetch.max-depth` to configure the maximum fetch depth.*<br><br>The maximum depth of outer join fetch tree for single-ended associations (one-to-one, many-to-one).<br><br>A `0` disables default outer join fetching.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAX_FETCH_DEPTH+++[] | int |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-metadata-builder-contributor"></a> <mark>link:#quarkus-hibernate-orm_quarkus-hibernate-orm-metadata-builder-contributor\[`quarkus.hibernate-orm.metadata-builder-contributor`\]</mark> config_property_copy_button:+++quarkus.hibernate-orm.metadata-builder-contributor+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".metadata-builder-contributor` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".metadata-builder-contributor+++[]<br><br>*This property is deprecated: Use `TypeContributor`, `FunctionContributor`, or `AdditionalMappingContributor` instead.*<br><br>Class name of a custom [`org.hibernate.boot.spi.MetadataBuilderContributor`](https://docs.hibernate.org/stable/orm/javadocs/org/hibernate/boot/spi/MetadataBuilderContributor.html) implementation.<br><br>> [!NOTE]<br>> Not all customization options exposed by [`org.hibernate.boot.MetadataBuilder`](https://docs.hibernate.org/stable/orm/javadocs/org/hibernate/boot/MetadataBuilder.html) will work correctly. Stay clear of options related to classpath scanning in particular.<br>><br>> This setting is exposed mainly to allow registration of types, converters and SQL functions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_METADATA_BUILDER_CONTRIBUTOR+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-validation-enabled"></a> <mark>link:#quarkus-hibernate-orm_quarkus-hibernate-orm-validation-enabled\[`quarkus.hibernate-orm.validation.enabled`\]</mark> config_property_copy_button:+++quarkus.hibernate-orm.validation.enabled+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".validation.enabled` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".validation.enabled+++[]<br><br>*This property is deprecated since `3.19`: Use `quarkus.hibernate-orm.validation.mode` instead.*<br><br>Enables the Bean Validation integration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_VALIDATION_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-multitenant-schema-datasource"></a> <mark>link:#quarkus-hibernate-orm_quarkus-hibernate-orm-multitenant-schema-datasource\[`quarkus.hibernate-orm.multitenant-schema-datasource`\]</mark> config_property_copy_button:+++quarkus.hibernate-orm.multitenant-schema-datasource+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".multitenant-schema-datasource` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".multitenant-schema-datasource+++[]<br><br>*This property is deprecated: Use `quarkus.hibernate-orm.datasource` to configure the datasource for the persistence unit.*<br><br>Defines the name of the datasource to use in case of SCHEMA approach. The datasource of the persistence unit will be used if not set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MULTITENANT_SCHEMA_DATASOURCE+++[] | string |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-database"></a> [Database related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-database) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-database-orm-compatibility-version"></a> [`quarkus.hibernate-orm.database.orm-compatibility.version`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-orm-compatibility-version) config_property_copy_button:+++quarkus.hibernate-orm.database.orm-compatibility.version+++[]<br><br>When set, attempts to exchange data with the database as the given version of Hibernate ORM would have, **on a best-effort basis**.<br><br>Please note:<br><br>- schema validation may still fail in some cases: this attempts to make Hibernate ORM 6+ behave correctly at runtime, but it may still expect a different (but runtime-compatible) schema.<br>- robust test suites are still useful and recommended: you should still check that your application behaves as intended with your legacy schema.<br>- this feature is inherently unstable: some aspects of it may stop working in future versions of Quarkus, and older versions will be dropped as Hibernate ORM changes pile up and support for those older versions becomes too unreliable.<br>- you should still plan a migration of your schema to a newer version of Hibernate ORM. For help with migration, refer to link:[https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration\[the](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration[the) Quarkus 3 migration guide from Hibernate ORM 5 to 6\].<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATABASE_ORM_COMPATIBILITY_VERSION+++[] | `5.6`, `latest` | `latest` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-database-charset"></a> [`quarkus.hibernate-orm.database.charset`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-charset) config_property_copy_button:+++quarkus.hibernate-orm.database.charset+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".database.charset` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".database.charset+++[]<br><br>The charset of the database.<br><br>Used for DDL generation and also for the SQL import scripts.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATABASE_CHARSET+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) | `UTF-8` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-database-default-catalog"></a> [`quarkus.hibernate-orm.database.default-catalog`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-default-catalog) config_property_copy_button:+++quarkus.hibernate-orm.database.default-catalog+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".database.default-catalog` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".database.default-catalog+++[]<br><br>The default catalog to use for the database objects.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATABASE_DEFAULT_CATALOG+++[] | string |  |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-database-default-schema"></a> [`quarkus.hibernate-orm.database.default-schema`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-default-schema) config_property_copy_button:+++quarkus.hibernate-orm.database.default-schema+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".database.default-schema` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".database.default-schema+++[]<br><br>The default schema to use for the database objects.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATABASE_DEFAULT_SCHEMA+++[] | string |  |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-database-version-check-enabled"></a> [`quarkus.hibernate-orm.database.version-check.enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-version-check-enabled) config_property_copy_button:+++quarkus.hibernate-orm.database.version-check.enabled+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".database.version-check.enabled` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".database.version-check.enabled+++[]<br><br>Whether Hibernate ORM should check on startup that the version of the database matches the version configured on the dialect (either the default version, or the one set through `quarkus.datasource.db-version`).<br><br>This should be set to `false` if the database is not available on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATABASE_VERSION_CHECK_ENABLED+++[] | boolean | `` `false` if starting offline (see `start-offline`), `true` otherwise `` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-database-start-offline"></a> [`quarkus.hibernate-orm.database.start-offline`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-start-offline) config_property_copy_button:+++quarkus.hibernate-orm.database.start-offline+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".database.start-offline` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".database.start-offline+++[]<br><br>Instructs Hibernate ORM to avoid connecting to the database on startup.<br><br>When starting offline:<br><br>- Hibernate ORM will not attempt to create a schema automatically, so it must already be created when the application hits the database for the first time.<br>- Quarkus will not check that the database version matches the one configured at build time.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DATABASE_START_OFFLINE+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-mapping"></a> [JSON/XML mapping related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-mapping) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-format-global"></a> [`quarkus.hibernate-orm.mapping.format.global`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-format-global) config_property_copy_button:+++quarkus.hibernate-orm.mapping.format.global+++[]<br><br>How the default JSON/XML format mappers are configured. Only available to mitigate migration from the current Quarkus-preconfigured format mappers (that will be removed in the future version).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_FORMAT_GLOBAL+++[] | `ignore`, `warn`, `fail` | `fail` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-timezone-default-storage"></a> [`quarkus.hibernate-orm.mapping.timezone.default-storage`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-timezone-default-storage) config_property_copy_button:+++quarkus.hibernate-orm.mapping.timezone.default-storage+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping.timezone.default-storage` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping.timezone.default-storage+++[]<br><br>How to store timezones in the database by default for properties of type `OffsetDateTime` and `ZonedDateTime`. This default may be overridden on a per-property basis using `@TimeZoneStorage`. NOTE: Properties of type `OffsetTime` are [++not affected by this setting++](https://hibernate.atlassian.net/browse/HHH-16287++)++.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_TIMEZONE_DEFAULT_STORAGE+++[] | tooltip:native[Stores the timestamp and timezone in a column of type `timestamp with time zone`. Only available on some databases/dialects; if not supported,opts=an exception will be thrown during static initialization.], tooltip:normalize[Does not store the timezone,opts=and loses timezone information upon persisting. Instead,normalizes the value\: * upon persisting to the database,to a timestamp in the JDBC timezone set through `quarkus.hibernate-orm.jdbc.timezone`,or the JVM default timezone if not set. * upon reading back from the database,to the JVM default timezone. Use this to get the legacy behavior of Quarkus 2 / Hibernate ORM 5 or older.], tooltip:normalize-utc[Does not store the timezone,opts=and loses timezone information upon persisting. Instead,normalizes the value to a timestamp in the UTC timezone.], tooltip:column[Stores the timezone in a separate column next to the timestamp column. Use `@TimeZoneColumn` on the relevant entity property to customize the timezone column.], tooltip:auto[Equivalent to `native` if supported,opts=`column` otherwise.], tooltip:default[Equivalent to `native` if supported,opts=`normalize-utc` otherwise.] | tooltip:default[Equivalent to `native` if supported,opts=`normalize-utc` otherwise.] |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-id-optimizer-default"></a> [`quarkus.hibernate-orm.mapping.id.optimizer.default`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-id-optimizer-default) config_property_copy_button:+++quarkus.hibernate-orm.mapping.id.optimizer.default+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping.id.optimizer.default` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping.id.optimizer.default+++[]<br><br>The optimizer to apply to identifier generators whose optimizer is not configured explicitly.<br><br>Only relevant for table- and sequence-based identifier generators. Other generators, such as UUID-based generators, will ignore this setting.<br><br>The optimizer is responsible for pooling new identifier values, in order to reduce the frequency of database calls to retrieve those values and thereby improve performance.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_ID_OPTIMIZER_DEFAULT+++[] | tooltip:pooled-lo[Assumes the value retrieved from the table/sequence is the lower end of the pool.  Upon retrieving value `N`,opts=the new pool of identifiers will go from `N` to `N + <allocation size> - 1`,inclusive.], tooltip:pooled[Assumes the value retrieved from the table/sequence is the higher end of the pool.  Upon retrieving value `N`,opts=the new pool of identifiers will go from `N - <allocation size>` to `N + <allocation size> - 1`,inclusive.  The first value,`1`,is handled differently to avoid negative identifiers.  Use this to get the legacy behavior of Quarkus 2 / Hibernate ORM 5 or older.], tooltip:none[No optimizer,opts=resulting in a database call each and every time an identifier value is needed from the generator.  Not recommended in production environments\: may result in degraded performance and/or frequent gaps in identifier values.] | tooltip:pooled-lo[Assumes the value retrieved from the table/sequence is the lower end of the pool.  Upon retrieving value `N`,opts=the new pool of identifiers will go from `N` to `N + <allocation size> - 1`,inclusive.] |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-duration-preferred-jdbc-type"></a> [`quarkus.hibernate-orm.mapping.duration.preferred-jdbc-type`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-duration-preferred-jdbc-type) config_property_copy_button:+++quarkus.hibernate-orm.mapping.duration.preferred-jdbc-type+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping.duration.preferred-jdbc-type` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping.duration.preferred-jdbc-type+++[]<br><br>The preferred JDBC type to use for storing {@link java.time.Duration} values. \<p> Can be overridden locally using `@JdbcType`, `@JdbcTypeCode`, and similar annotations. \<p> Can also specify the name of the SqlTypes constant field, for example, `quarkus.hibernate-orm.mapping.type.preferred_jdbc_type=INTERVAL_SECOND`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_DURATION_PREFERRED_JDBC_TYPE+++[] | string | `INTERVAL_SECOND` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-instant-preferred-jdbc-type"></a> [`quarkus.hibernate-orm.mapping.instant.preferred-jdbc-type`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-instant-preferred-jdbc-type) config_property_copy_button:+++quarkus.hibernate-orm.mapping.instant.preferred-jdbc-type+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping.instant.preferred-jdbc-type` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping.instant.preferred-jdbc-type+++[]<br><br>The preferred JDBC type to use for storing {@link java.time.Instant} values. \<p> Can be overridden locally using `@JdbcType`, `@JdbcTypeCode`, and similar annotations. \<p> Can also specify the name of the SqlTypes constant field, for example, `quarkus.hibernate-orm.mapping.instant.preferred-jdbc-type=TIMESTAMP` or `quarkus.hibernate-orm.mapping.instant.preferred-jdbc-type=INSTANT`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_INSTANT_PREFERRED_JDBC_TYPE+++[] | string | `TIMESTAMP` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-boolean-preferred-jdbc-type"></a> [`quarkus.hibernate-orm.mapping.boolean.preferred-jdbc-type`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-boolean-preferred-jdbc-type) config_property_copy_button:+++quarkus.hibernate-orm.mapping.boolean.preferred-jdbc-type+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping.boolean.preferred-jdbc-type` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping.boolean.preferred-jdbc-type+++[]<br><br>The preferred JDBC type to use for storing boolean values. \<p> Can be overridden locally using `@JdbcType`, `@JdbcTypeCode`, and similar annotations. \<p> Can also specify the name of the SqlTypes constant field, for example, `quarkus.hibernate-orm.mapping.boolean.preferred-jdbc-type=BIT`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_BOOLEAN_PREFERRED_JDBC_TYPE+++[] | string | `BOOLEAN` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-uuid-preferred-jdbc-type"></a> [`quarkus.hibernate-orm.mapping.uuid.preferred-jdbc-type`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-uuid-preferred-jdbc-type) config_property_copy_button:+++quarkus.hibernate-orm.mapping.uuid.preferred-jdbc-type+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".mapping.uuid.preferred-jdbc-type` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".mapping.uuid.preferred-jdbc-type+++[]<br><br>The preferred JDBC type to use for storing {@link java.util.UUID} values. \<p> Can be overridden locally using `@JdbcType`, `@JdbcTypeCode`, and similar annotations. \<p> Can also specify the name of the SqlTypes constant field, for example, `quarkus.hibernate-orm.mapping.uuid.preferred-jdbc-type=CHAR`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_MAPPING_UUID_PREFERRED_JDBC_TYPE+++[] | string | `UUID` |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-dialect"></a> [Dialect related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-dialect) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect"></a> [`quarkus.hibernate-orm.dialect`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect) config_property_copy_button:+++quarkus.hibernate-orm.dialect+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect+++[]<br><br>Name of the Hibernate ORM dialect.<br><br>For [supported databases](/guides/datasource/index.md#extensions-and-database-drivers-reference), this property does not need to be set explicitly: it is selected automatically based on the datasource, and configured using the xref:datasource.adoc#quarkus-datasource_quarkus.datasource.db-version\[DB version set on the datasource\] to benefit from the best performance and latest features.<br><br>If your database does not have a corresponding Quarkus extension, you **will** need to set this property explicitly. In that case, keep in mind that the JDBC driver and Hibernate ORM dialect may not work properly in GraalVM native executables.<br><br>For built-in dialects, the expected value is one of the names in the [official list of dialects](https://docs.hibernate.org/orm/7.4/dialect/dialect.html), **without** the `Dialect` suffix, for example `Cockroach` for `CockroachDialect`.<br><br>For third-party dialects, the expected value is the fully-qualified class name, for example `com.acme.hibernate.AcmeDbDialect`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT+++[] | string | `selected automatically for most popular databases` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mariadb-bytes-per-character"></a> [`quarkus.hibernate-orm.dialect.mariadb.bytes-per-character`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mariadb-bytes-per-character) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mariadb.bytes-per-character+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mariadb.bytes-per-character` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mariadb.bytes-per-character+++[]<br><br>Specifies the bytes per character to use based on the database's configured [charset](https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MARIADB_BYTES_PER_CHARACTER+++[] | int | `4` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mariadb-no-backslash-escapes"></a> [`quarkus.hibernate-orm.dialect.mariadb.no-backslash-escapes`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mariadb-no-backslash-escapes) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mariadb.no-backslash-escapes+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mariadb.no-backslash-escapes` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mariadb.no-backslash-escapes+++[]<br><br>Specifies whether the `NO_BACKSLASH_ESCAPES` sql mode is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MARIADB_NO_BACKSLASH_ESCAPES+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mariadb-storage-engine"></a> [`quarkus.hibernate-orm.dialect.mariadb.storage-engine`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mariadb-storage-engine) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mariadb.storage-engine+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mariadb.storage-engine` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mariadb.storage-engine+++[]<br><br>The storage engine to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MARIADB_STORAGE_ENGINE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mysql-bytes-per-character"></a> [`quarkus.hibernate-orm.dialect.mysql.bytes-per-character`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mysql-bytes-per-character) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mysql.bytes-per-character+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mysql.bytes-per-character` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mysql.bytes-per-character+++[]<br><br>Specifies the bytes per character to use based on the database's configured [charset](https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MYSQL_BYTES_PER_CHARACTER+++[] | int | `4` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mysql-no-backslash-escapes"></a> [`quarkus.hibernate-orm.dialect.mysql.no-backslash-escapes`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mysql-no-backslash-escapes) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mysql.no-backslash-escapes+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mysql.no-backslash-escapes` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mysql.no-backslash-escapes+++[]<br><br>Specifies whether the `NO_BACKSLASH_ESCAPES` sql mode is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MYSQL_NO_BACKSLASH_ESCAPES+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mysql-storage-engine"></a> [`quarkus.hibernate-orm.dialect.mysql.storage-engine`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mysql-storage-engine) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mysql.storage-engine+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mysql.storage-engine` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mysql.storage-engine+++[]<br><br>The storage engine to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MYSQL_STORAGE_ENGINE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-oracle-extended"></a> [`quarkus.hibernate-orm.dialect.oracle.extended`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-oracle-extended) config_property_copy_button:+++quarkus.hibernate-orm.dialect.oracle.extended+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.oracle.extended` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.oracle.extended+++[]<br><br>Support for Oracle's MAX_STRING_SIZE = EXTENDED.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_ORACLE_EXTENDED+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-oracle-autonomous"></a> [`quarkus.hibernate-orm.dialect.oracle.autonomous`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-oracle-autonomous) config_property_copy_button:+++quarkus.hibernate-orm.dialect.oracle.autonomous+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.oracle.autonomous` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.oracle.autonomous+++[]<br><br>Specifies whether this database is running on an Autonomous Database Cloud Service.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_ORACLE_AUTONOMOUS+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-oracle-application-continuity"></a> [`quarkus.hibernate-orm.dialect.oracle.application-continuity`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-oracle-application-continuity) config_property_copy_button:+++quarkus.hibernate-orm.dialect.oracle.application-continuity+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.oracle.application-continuity` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.oracle.application-continuity+++[]<br><br>Specifies whether this database is accessed using a database service protected by Application Continuity.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_ORACLE_APPLICATION_CONTINUITY+++[] | boolean | `false` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mssql-compatibility-level"></a> [`quarkus.hibernate-orm.dialect.mssql.compatibility-level`](#quarkus-hibernate-orm_quarkus-hibernate-orm-dialect-mssql-compatibility-level) config_property_copy_button:+++quarkus.hibernate-orm.dialect.mssql.compatibility-level+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".dialect.mssql.compatibility-level` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".dialect.mssql.compatibility-level+++[]<br><br>The `compatibility_level` as defined in `sys.databases`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DIALECT_MSSQL_COMPATIBILITY_LEVEL+++[] | string |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-query"></a> [Query related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-query) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-query-query-plan-cache-max-size"></a> [`quarkus.hibernate-orm.query.query-plan-cache-max-size`](#quarkus-hibernate-orm_quarkus-hibernate-orm-query-query-plan-cache-max-size) config_property_copy_button:+++quarkus.hibernate-orm.query.query-plan-cache-max-size+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".query.query-plan-cache-max-size` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".query.query-plan-cache-max-size+++[]<br><br>The maximum size of the query plan cache. see ++<mark>++\`org.hibernate.cfg.AvailableSettings++</mark>++QUERY_PLAN_CACHE_MAX_SIZE\`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_QUERY_QUERY_PLAN_CACHE_MAX_SIZE+++[] | int | `2048` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-query-default-null-ordering"></a> [`quarkus.hibernate-orm.query.default-null-ordering`](#quarkus-hibernate-orm_quarkus-hibernate-orm-query-default-null-ordering) config_property_copy_button:+++quarkus.hibernate-orm.query.default-null-ordering+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".query.default-null-ordering` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".query.default-null-ordering+++[]<br><br>Default precedence of null values in `ORDER BY` clauses.<br><br>Valid values are: `none`, `first`, `last`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_QUERY_DEFAULT_NULL_ORDERING+++[] | tooltip:none[Null precedence not specified.], tooltip:first[Null values occur at the beginning of the ORDER BY clause.], tooltip:last[Null values occur at the end of the ORDER BY clause.] | tooltip:none[Null precedence not specified.] |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-query-in-clause-parameter-padding"></a> [`quarkus.hibernate-orm.query.in-clause-parameter-padding`](#quarkus-hibernate-orm_quarkus-hibernate-orm-query-in-clause-parameter-padding) config_property_copy_button:+++quarkus.hibernate-orm.query.in-clause-parameter-padding+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".query.in-clause-parameter-padding` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".query.in-clause-parameter-padding+++[]<br><br>Enables IN clause parameter padding which improves statement caching.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_QUERY_IN_CLAUSE_PARAMETER_PADDING+++[] | boolean | `true` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-query-fail-on-pagination-over-collection-fetch"></a> [`quarkus.hibernate-orm.query.fail-on-pagination-over-collection-fetch`](#quarkus-hibernate-orm_quarkus-hibernate-orm-query-fail-on-pagination-over-collection-fetch) config_property_copy_button:+++quarkus.hibernate-orm.query.fail-on-pagination-over-collection-fetch+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".query.fail-on-pagination-over-collection-fetch` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".query.fail-on-pagination-over-collection-fetch+++[]<br><br>When limits cannot be applied on the database side, trigger an exception instead of attempting badly-performing in-memory result set limits.<br><br>When pagination is used in combination with a fetch join applied to a collection or many-valued association, the limit must be applied in-memory instead of on the database. This should be avoided as it typically has terrible performance characteristics.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_QUERY_FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-jdbc"></a> [JDBC related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-jdbc) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-enabled"></a> [`quarkus.hibernate-orm.jdbc.enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-enabled) config_property_copy_button:+++quarkus.hibernate-orm.jdbc.enabled+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".jdbc.enabled` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".jdbc.enabled+++[]<br><br>Whether to bootstrap a blocking (JDBC) Hibernate ORM instance for this persistence unit. \<p> Use {@code quarkus.hibernate-orm.jdbc.enabled} for the default persistence unit and {@code quarkus.hibernate-orm."<persistence-unit-name>".jdbc.enabled} for named persistence units. This is the per-persistence-unit replacement for the deprecated {@code quarkus.hibernate-orm.blocking}. \<p> If not set, this is inferred from whether a JDBC datasource is available for this persistence unit, as well as the global (deprecated) `quarkus.hibernate-orm.blocking` setting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_JDBC_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-timezone"></a> [`quarkus.hibernate-orm.jdbc.timezone`](#quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-timezone) config_property_copy_button:+++quarkus.hibernate-orm.jdbc.timezone+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".jdbc.timezone` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".jdbc.timezone+++[]<br><br>The time zone pushed to the JDBC driver. See `quarkus.hibernate-orm.mapping.timezone.default-storage`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_JDBC_TIMEZONE+++[] | string |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-statement-fetch-size"></a> [`quarkus.hibernate-orm.jdbc.statement-fetch-size`](#quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-statement-fetch-size) config_property_copy_button:+++quarkus.hibernate-orm.jdbc.statement-fetch-size+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".jdbc.statement-fetch-size` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".jdbc.statement-fetch-size+++[]<br><br>How many rows are fetched at a time by the JDBC driver.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_JDBC_STATEMENT_FETCH_SIZE+++[] | int |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-statement-batch-size"></a> [`quarkus.hibernate-orm.jdbc.statement-batch-size`](#quarkus-hibernate-orm_quarkus-hibernate-orm-jdbc-statement-batch-size) config_property_copy_button:+++quarkus.hibernate-orm.jdbc.statement-batch-size+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".jdbc.statement-batch-size` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".jdbc.statement-batch-size+++[]<br><br>The number of updates (inserts, updates and deletes) that are sent by the JDBC driver at one time for execution.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_JDBC_STATEMENT_BATCH_SIZE+++[] | int |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-reactive"></a> [Reactive related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-reactive) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-reactive-enabled"></a> [`quarkus.hibernate-orm.reactive.enabled`](#quarkus-hibernate-orm_quarkus-hibernate-orm-reactive-enabled) config_property_copy_button:+++quarkus.hibernate-orm.reactive.enabled+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".reactive.enabled` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".reactive.enabled+++[]<br><br>Whether to bootstrap a reactive Hibernate Reactive instance for this persistence unit. \<p> Use {@code quarkus.hibernate-orm.reactive.enabled} for the default persistence unit and {@code quarkus.hibernate-orm."<persistence-unit-name>".reactive.enabled} for named persistence units. \<p> If not set, this is inferred from whether a reactive datasource is available for this persistence unit.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_REACTIVE_ENABLED+++[] | boolean |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-fetch"></a> [Fetching logic configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-fetch) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-fetch-batch-size"></a> [`quarkus.hibernate-orm.fetch.batch-size`](#quarkus-hibernate-orm_quarkus-hibernate-orm-fetch-batch-size) config_property_copy_button:+++quarkus.hibernate-orm.fetch.batch-size+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".fetch.batch-size` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".fetch.batch-size+++[]<br><br>The size of the batches used when loading entities and collections.<br><br>`-1` means batch loading is disabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_FETCH_BATCH_SIZE+++[] | int | `16` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-fetch-max-depth"></a> [`quarkus.hibernate-orm.fetch.max-depth`](#quarkus-hibernate-orm_quarkus-hibernate-orm-fetch-max-depth) config_property_copy_button:+++quarkus.hibernate-orm.fetch.max-depth+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".fetch.max-depth` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".fetch.max-depth+++[]<br><br>The maximum depth of outer join fetch tree for single-ended associations (one-to-one, many-to-one).<br><br>A `0` disables default outer join fetching.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_FETCH_MAX_DEPTH+++[] | int |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-cache"></a> [Caching configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-cache) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-expiration-max-idle"></a> [`quarkus.hibernate-orm.cache."cache".expiration.max-idle`](#quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-expiration-max-idle) config_property_copy_button:+++quarkus.hibernate-orm.cache."cache".expiration.max-idle+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".cache."cache".expiration.max-idle` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".cache."cache".expiration.max-idle+++[]<br><br>The maximum time before an object of the cache is considered expired.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_CACHE__CACHE__EXPIRATION_MAX_IDLE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-hibernate-orm_quarkus-hibernate-orm) | `100s` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-memory-object-count"></a> [`quarkus.hibernate-orm.cache."cache".memory.object-count`](#quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-memory-object-count) config_property_copy_button:+++quarkus.hibernate-orm.cache."cache".memory.object-count+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".cache."cache".memory.object-count` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".cache."cache".memory.object-count+++[]<br><br>The maximum number of objects kept in memory in the cache.<br><br>Mutually exclusive with `maximum-weight`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_CACHE__CACHE__MEMORY_OBJECT_COUNT+++[] | long | `10000` |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-memory-maximum-weight"></a> [`quarkus.hibernate-orm.cache."cache".memory.maximum-weight`](#quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-memory-maximum-weight) config_property_copy_button:+++quarkus.hibernate-orm.cache."cache".memory.maximum-weight+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".cache."cache".memory.maximum-weight` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".cache."cache".memory.maximum-weight+++[]<br><br>The maximum total weight of objects kept in memory in the cache.<br><br>When set, eviction is based on the total weight of cached entries rather than their count. This is useful for entities with highly variable sizes (e.g., JSON blobs).<br><br>Mutually exclusive with `object-count`. Requires a `weigher-class` to assign weights to entries; without one, each entry has a default weight of 1.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_CACHE__CACHE__MEMORY_MAXIMUM_WEIGHT+++[] | long |  |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-memory-weigher-class"></a> [`quarkus.hibernate-orm.cache."cache".memory.weigher-class`](#quarkus-hibernate-orm_quarkus-hibernate-orm-cache-cache-memory-weigher-class) config_property_copy_button:+++quarkus.hibernate-orm.cache."cache".memory.weigher-class+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".cache."cache".memory.weigher-class` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".cache."cache".memory.weigher-class+++[]<br><br>The fully qualified class name of a `com.github.benmanes.caffeine.cache.Weigher` implementation used to assign weights to cache entries.<br><br>Only used when `maximum-weight` is set. The class must have a public no-arg constructor.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_CACHE__CACHE__MEMORY_WEIGHER_CLASS+++[] | string |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-discriminator"></a> [Discriminator related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-discriminator) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-discriminator-ignore-explicit-for-joined"></a> [`quarkus.hibernate-orm.discriminator.ignore-explicit-for-joined`](#quarkus-hibernate-orm_quarkus-hibernate-orm-discriminator-ignore-explicit-for-joined) config_property_copy_button:+++quarkus.hibernate-orm.discriminator.ignore-explicit-for-joined+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".discriminator.ignore-explicit-for-joined` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".discriminator.ignore-explicit-for-joined+++[]<br><br>Existing applications rely (implicitly or explicitly) on Hibernate ignoring any DiscriminatorColumn declarations on joined inheritance hierarchies. This setting allows these applications to maintain the legacy behavior of DiscriminatorColumn annotations being ignored when paired with joined inheritance.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_DISCRIMINATOR_IGNORE_EXPLICIT_FOR_JOINED+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-log"></a> [Logging configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-log) | Type | Default |
| [lock] <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-bind-parameters"></a> [`quarkus.hibernate-orm.log.bind-parameters`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-bind-parameters) config_property_copy_button:+++quarkus.hibernate-orm.log.bind-parameters+++[]<br><br>Logs SQL bind parameters.<br><br>Setting it to true is obviously not recommended in production.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_BIND_PARAMETERS+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-sql"></a> [`quarkus.hibernate-orm.log.sql`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-sql) config_property_copy_button:+++quarkus.hibernate-orm.log.sql+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".log.sql` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".log.sql+++[]<br><br>Show SQL logs and format them nicely.<br><br>Setting it to true is obviously not recommended in production.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_SQL+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-format-sql"></a> [`quarkus.hibernate-orm.log.format-sql`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-format-sql) config_property_copy_button:+++quarkus.hibernate-orm.log.format-sql+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".log.format-sql` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".log.format-sql+++[]<br><br>Format the SQL logs if SQL log is enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_FORMAT_SQL+++[] | boolean | `true` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-highlight-sql"></a> [`quarkus.hibernate-orm.log.highlight-sql`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-highlight-sql) config_property_copy_button:+++quarkus.hibernate-orm.log.highlight-sql+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".log.highlight-sql` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".log.highlight-sql+++[]<br><br>Highlight the SQL logs if SQL log is enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_HIGHLIGHT_SQL+++[] | boolean | `true` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-jdbc-warnings"></a> [`quarkus.hibernate-orm.log.jdbc-warnings`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-jdbc-warnings) config_property_copy_button:+++quarkus.hibernate-orm.log.jdbc-warnings+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".log.jdbc-warnings` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".log.jdbc-warnings+++[]<br><br>Whether JDBC warnings should be collected and logged.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_JDBC_WARNINGS+++[] | boolean | `depends on dialect` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-log-queries-slower-than-ms"></a> [`quarkus.hibernate-orm.log.queries-slower-than-ms`](#quarkus-hibernate-orm_quarkus-hibernate-orm-log-queries-slower-than-ms) config_property_copy_button:+++quarkus.hibernate-orm.log.queries-slower-than-ms+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".log.queries-slower-than-ms` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".log.queries-slower-than-ms+++[]<br><br>If set, Hibernate will log queries that took more than specified number of milliseconds to execute.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_LOG_QUERIES_SLOWER_THAN_MS+++[] | long |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-schema-management"></a> [Schema management configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-schema-management) | Type | Default |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-strategy"></a> [`quarkus.hibernate-orm.schema-management.strategy`](#quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-strategy) config_property_copy_button:+++quarkus.hibernate-orm.schema-management.strategy+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".schema-management.strategy` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".schema-management.strategy+++[]<br><br>Select whether the database schema is generated or not.<br><br>`drop-and-create` is awesome in development mode.<br><br>This defaults to 'none'.<br><br>However if Dev Services is in use and no other extensions that manage the schema are present the value will be automatically overridden to 'drop-and-create'.<br><br>Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY+++[] | tooltip:none[No schema action.], tooltip:create[Create the schema.], tooltip:drop-and-create[Drop and then recreate the schema.], tooltip:drop[Drop the schema.], tooltip:update[Update (alter) the database schema.], tooltip:validate[Validate the database schema.] | tooltip:none[No schema action.] |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-create-schemas"></a> [`quarkus.hibernate-orm.schema-management.create-schemas`](#quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-create-schemas) config_property_copy_button:+++quarkus.hibernate-orm.schema-management.create-schemas+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".schema-management.create-schemas` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".schema-management.create-schemas+++[]<br><br>If Hibernate ORM should create the schemas automatically (for databases supporting them).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_CREATE_SCHEMAS+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-halt-on-error"></a> [`quarkus.hibernate-orm.schema-management.halt-on-error`](#quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-halt-on-error) config_property_copy_button:+++quarkus.hibernate-orm.schema-management.halt-on-error+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".schema-management.halt-on-error` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".schema-management.halt-on-error+++[]<br><br>Whether we should stop on the first error when applying the schema.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_HALT_ON_ERROR+++[] | boolean | `false` |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-extra-physical-table-types"></a> [`quarkus.hibernate-orm.schema-management.extra-physical-table-types`](#quarkus-hibernate-orm_quarkus-hibernate-orm-schema-management-extra-physical-table-types) config_property_copy_button:+++quarkus.hibernate-orm.schema-management.extra-physical-table-types+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".schema-management.extra-physical-table-types` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".schema-management.extra-physical-table-types+++[]<br><br>Additional database object types to include in schema management operations.<br><br>By default, Hibernate ORM only considers tables and sequences when performing schema management operations. This setting allows you to specify additional database object types that should be included, such as "MATERIALIZED VIEW", "VIEW", or other database-specific object types.<br><br>The exact supported values depend on the underlying database and dialect.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_EXTRA_PHYSICAL_TABLE_TYPES+++[] | string |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-scripts"></a> [Database scripts related configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-scripts) | Type | Default |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-scripts-generation"></a> [`quarkus.hibernate-orm.scripts.generation`](#quarkus-hibernate-orm_quarkus-hibernate-orm-scripts-generation) config_property_copy_button:+++quarkus.hibernate-orm.scripts.generation+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".scripts.generation` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".scripts.generation+++[]<br><br>Select whether the database schema DDL files are generated or not. Accepted values: `none`, `create`, `drop-and-create`, `drop`, `update`, `validate`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCRIPTS_GENERATION+++[] | tooltip:none[No schema action.], tooltip:create[Create the schema.], tooltip:drop-and-create[Drop and then recreate the schema.], tooltip:drop[Drop the schema.], tooltip:update[Update (alter) the database schema.], tooltip:validate[Validate the database schema.] | tooltip:none[No schema action.] |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-scripts-generation-create-target"></a> [`quarkus.hibernate-orm.scripts.generation.create-target`](#quarkus-hibernate-orm_quarkus-hibernate-orm-scripts-generation-create-target) config_property_copy_button:+++quarkus.hibernate-orm.scripts.generation.create-target+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".scripts.generation.create-target` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".scripts.generation.create-target+++[]<br><br>Filename or URL where the database create DDL file should be generated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCRIPTS_GENERATION_CREATE_TARGET+++[] | string |  |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-scripts-generation-drop-target"></a> [`quarkus.hibernate-orm.scripts.generation.drop-target`](#quarkus-hibernate-orm_quarkus-hibernate-orm-scripts-generation-drop-target) config_property_copy_button:+++quarkus.hibernate-orm.scripts.generation.drop-target+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".scripts.generation.drop-target` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".scripts.generation.drop-target+++[]<br><br>Filename or URL where the database drop DDL file should be generated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_SCRIPTS_GENERATION_DROP_TARGET+++[] | string |  |
| <a id="quarkus-hibernate-orm_section_quarkus-hibernate-orm-flush"></a> [Flush configuration](#quarkus-hibernate-orm_section_quarkus-hibernate-orm-flush) | Type | Default |
| <a id="quarkus-hibernate-orm_quarkus-hibernate-orm-flush-mode"></a> [`quarkus.hibernate-orm.flush.mode`](#quarkus-hibernate-orm_quarkus-hibernate-orm-flush-mode) config_property_copy_button:+++quarkus.hibernate-orm.flush.mode+++[]<br><br>`quarkus.hibernate-orm."persistence-unit-name".flush.mode` config_property_copy_button:+++quarkus.hibernate-orm."persistence-unit-name".flush.mode+++[]<br><br>The default flushing strategy, or when to flush entities to the database in a Hibernate session: before every query, on commit, ...<br><br>This default can be overridden on a per-session basis with `Session#setHibernateFlushMode()` or on a per-query basis with the hint `HibernateHints#HINT_FLUSH_MODE`.<br><br>See the javadoc of `org.hibernate.FlushMode` for details.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HIBERNATE_ORM_FLUSH_MODE+++[] | tooltip:manual[The `org.hibernate.Session` is only flushed when `org.hibernate.Session#flush()` is called explicitly. This mode is very efficient for read-only transactions.], tooltip:commit[The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()` is called. It is never automatically flushed before query execution.], tooltip:auto[The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()` is called,opts=and is sometimes flushed before query execution in order to ensure that queries never return stale state. This is the default flush mode.], tooltip:always[The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()` is called and before every query. This is usually unnecessary and inefficient.] | tooltip:auto[The `org.hibernate.Session` is flushed when `org.hibernate.Transaction#commit()` is called,opts=and is sometimes flushed before query execution in order to ensure that queries never return stale state. This is the default flush mode.] |

<a id="duration-note-anchor-quarkus-hibernate-orm_quarkus-hibernate-orm"></a>

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
