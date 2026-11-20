# Using Hibernate ORM and Jakarta Persistence

Hibernate ORM is the de facto standard Jakarta Persistence (formerly known as JPA) implementation and offers you the full breadth of an Object Relational Mapper. It works beautifully in Quarkus.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `hibernate-orm-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-quickstart).

## Setting up and configuring Hibernate ORM

When using Hibernate ORM in Quarkus, you don't need to have a `persistence.xml` resource to configure it.

Using such a classic configuration file is an option, but unnecessary unless you have specific advanced needs; so we'll see first how Hibernate ORM can be configured without a `persistence.xml` resource.

In Quarkus, you only need to:

- add your configuration settings in `application.properties`
- annotate your entities with `@Entity` and any other mapping annotation as usual

Other configuration needs have been automated: Quarkus will make some opinionated choices and educated guesses.

Add the following dependencies to your project:

- the Hibernate ORM extension: `io.quarkus:quarkus-hibernate-orm`
- your JDBC driver extension; the following options are available: - `quarkus-jdbc-db2` for [IBM DB2](https://www.ibm.com/products/db2-database)  - `quarkus-jdbc-h2` for [H2](https://www.h2database.com/html/main.html)  - `quarkus-jdbc-mariadb` for [MariaDB](https://mariadb.com/)  - `quarkus-jdbc-mssql` for [Microsoft SQL Server](https://www.microsoft.com/en-gb/sql-server/)  - `quarkus-jdbc-mysql` for [MySQL](https://www.mysql.com/)  - `quarkus-jdbc-oracle` for [Oracle Database](https://www.oracle.com/database/)  - `quarkus-jdbc-postgresql` for [PostgreSQL](https://www.postgresql.org/)

For instance:

**pom.xml**

```xml
<!-- Hibernate ORM specific dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm</artifactId>
</dependency>

<!-- JDBC driver dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
```

**build.gradle**

```gradle
// Hibernate ORM specific dependencies
implementation("io.quarkus:quarkus-hibernate-orm")

// JDBC driver dependencies
implementation("io.quarkus:quarkus-jdbc-postgresql")
```

Annotate your persistent objects with `@Entity`, then add the relevant configuration properties in `application.properties`.

**Example `application.properties`**

```properties
quarkus.datasource.db-kind = postgresql <1>

%prod.quarkus.datasource.username = hibernate
%prod.quarkus.datasource.password = hibernate
%prod.quarkus.datasource.jdbc.url = jdbc:postgresql://localhost:5432/hibernate_db
%prod.quarkus.hibernate-orm.schema-management.strategy=create <2>
```

1. [Configure the datasource](/guides/datasource/index.md) for production, relying on [Dev Services](/guides/datasource/index.md#dev-services) for connection information in tests / dev mode.
2. Configure Hibernate ORM to create the schema on startup in production, which is useful for experimentation, but rely on [convenient defaults in tests / dev mode](#dev-mode).

Note that configuration properties for the Hibernate ORM Quarkus extension are not the same ones as in your typical Hibernate ORM configuration file. They will often map to Hibernate ORM configuration properties but could have different names and don't necessarily map 1:1 to each other.

Also, Quarkus will set many Hibernate ORM configuration settings automatically, and will often use more modern defaults.

For a list of the items that you can set in `application.properties`, see [Configuration Reference for Hibernate ORM](#configuration-reference).

An `EntityManagerFactory` will be created based on the Quarkus `datasource` configuration as long as the Hibernate ORM extension is listed among your project dependencies.

The dialect will be selected and configured automatically based on your datasource; you may want to [configure it to more precisely match your database](#hibernate-dialect).

You can then happily inject your `EntityManager`:

**Example application bean using Hibernate**

```java
@ApplicationScoped
public class SantaClausService {
    @Inject
    EntityManager em; <1>

    @Transactional <2>
    public void createGift(String giftDescription) {
        Gift gift = new Gift();
        gift.setName(giftDescription);
        em.persist(gift);
    }
}
```

1. Inject your entity manager and have fun
2. Mark your CDI bean method as `@Transactional` and the `EntityManager` will enlist and flush at commit.

**Example Entity**

```java
@Entity
public class Gift {
    private Long id;
    private String name;

    @Id
    @GeneratedValue
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

To load SQL statements when Hibernate ORM starts, add an `import.sql` file to the root of your `resources` directory. This script can contain any SQL DML statements. Make sure to terminate each statement with a semicolon.

This is useful to have a data set ready for your tests or demos.

> [!WARNING]
> Make sure to wrap methods modifying your database (e.g. `entity.persist()`) within a transaction. Marking a CDI bean method `@Transactional` will do that for you and make that method a transaction boundary. We recommend doing so at your application entry point boundaries like your REST endpoint controllers.

<a id="hibernate-dialect"></a>
### Dialect

<a id="hibernate-dialect-supported-databases"></a>
#### Supported databases

For [supported databases](/guides/datasource/index.md#extensions-and-database-drivers-reference), the [Hibernate ORM dialect](https://docs.hibernate.org/orm/7.4/userguide/html_single/#database-dialect) does not need to be set explicitly: it is selected automatically based on the datasource.

By default, the dialect is configured to target the minimum supported version of the database.

In order for Hibernate ORM to generate more efficient SQL, to avoid workarounds and to take advantage of more database features, you can set the database version explicitly:

**`application.properties` with an explicit `db-version`**

```properties
quarkus.datasource.db-kind = postgresql
quarkus.datasource.db-version = 18.1 <1>

%prod.quarkus.datasource.username = hibernate
%prod.quarkus.datasource.password = hibernate
%prod.quarkus.datasource.jdbc.url = jdbc:postgresql://localhost:5432/hibernate_db
```

1. Set the database version. The Hibernate ORM dialect will target that version.

As a rule, the version set here should be as high as possible, but must be lower than or equal to the version of any database your application will connect to.

> [!NOTE]
> As described above, the version can either be preconfigured explicitly via a `quarkus.datasource.db-version` configuration property, or implicitly set by the Quarkus build process to a minimum supported version of the database. Quarkus will try to check this preconfigured version against the actual database version on startup, leading to a startup failure when the actual version is lower.
>
> This is a safeguard: for versions of the database older than what is configured, Hibernate ORM may generate SQL that is invalid which would lead to runtime exceptions.
>
> If the database cannot be reached, a warning will be logged but startup will proceed. If you know the database won't be reachable on startup, consider configuring [offline startup](#offline), which will automatically disable the check and warning.
>
> As a last resort, you can disable the version check completely using [`quarkus.hibernate-orm.database.version-check.enabled=false`](#quarkus-hibernate-orm_quarkus-hibernate-orm-database-version-check-enabled).

<a id="hibernate-dialect-other-databases"></a>
#### Other databases

If [your database does not have a corresponding Quarkus extension](/guides/datasource/index.md#other-databases), or if the defaults do not match your needs for some reason, you will need to set the [Hibernate ORM dialect](https://docs.hibernate.org/orm/7.4/userguide/html_single/#database-dialect) explicitly:

**`application.properties` with an explicit `dialect`**

```properties
quarkus.datasource.db-kind = postgresql

quarkus.hibernate-orm.dialect=Cockroach <1>

%prod.quarkus.datasource.username = hibernate
%prod.quarkus.datasource.password = hibernate
%prod.quarkus.datasource.jdbc.url = jdbc:postgresql://localhost:26257/hibernate_db
```

1. Set the Hibernate ORM dialect.

   For built-in dialects, the expected value is one of the names in the [official list of dialects](https://docs.hibernate.org/orm/7.4/dialect/dialect.html), **without** the `Dialect` suffix, for example `Cockroach` for `CockroachDialect`.

   For third-party dialects, the expected value is the fully-qualified class name, for example `com.acme.hibernate.AcmeDbDialect`.


> [!WARNING]
> In that case, keep in mind that the JDBC driver or Hibernate ORM dialect may not work properly in GraalVM native executables.

As with [supported databases](#hibernate-dialect-supported-databases), you can configure the DB version explicitly to get the most out of Hibernate ORM:

**`application.properties` with an explicit `dialect` and `db-version`**

```properties
quarkus.datasource.db-kind = postgresql
quarkus.datasource.db-version = 25.4 <1>

quarkus.hibernate-orm.dialect=Cockroach <2>

%prod.quarkus.datasource.username = hibernate
%prod.quarkus.datasource.password = hibernate
%prod.quarkus.datasource.jdbc.url = jdbc:postgresql://localhost:26257/hibernate_db
```

1. Set the database version. The Hibernate ORM dialect will target that version. Since we're targeting CockroachDB here, we're passing the CockroachDB version, not the PostgreSQL version.
2. Set the Hibernate ORM dialect.

<a id="hibernate-dialect-varying-database"></a>
#### Varying database

When enabling [database multi-tenancy](#database-approach), Hibernate ORM will use multiple datasources at runtime for the same persistence unit, and by default Quarkus cannot tell which datasource is going to be used, so it will not be able to detect a dialect to use in Hibernate ORM.

For that reason, when enabling [database multi-tenancy](#database-approach), it is recommended to explicitly point the Hibernate ORM configuration to one datasource among those that will be used at runtime, e.g. with `quarkus.hibernate-orm.datasource=base` (`base` being the name of a datasource).

When doing so, Quarkus will infer the database version and (if possible) dialect from that datasource. For unsupported databases, you may still need to set the Hibernate ORM dialect explicitly, as explained in [this section](#hibernate-dialect-other-databases).

<a id="hibernate-configuration-properties"></a>
### Hibernate ORM configuration properties

There are various optional properties useful to refine your `EntityManagerFactory` or guide guesses of Quarkus.

There are no required properties, as long as a default datasource is configured.

When no property is set, Quarkus can typically infer everything it needs to set up Hibernate ORM and will have it use the default datasource.

The configuration properties listed in [Configuration Reference for Hibernate ORM](#configuration-reference) allow you to override such defaults, and customize and tune various aspects.

> [!NOTE]
> Do not mix [`persistence.xml`](#persistence-xml) and `quarkus.hibernate-orm.*` properties in `application.properties`. Quarkus will raise an exception. Make up your mind on which approach you want to use.
>
> If your classpath contains a `persistence.xml` that you want to ignore, set the following configuration property:
>
> ```properties
> quarkus.hibernate-orm.persistence-xml.ignore=true
> ```

<a id="multiple-persistence-units"></a>
### Multiple persistence units

#### Setting up multiple persistence units

It is possible to define multiple persistence units using the Quarkus configuration properties.

The properties at the root of the `quarkus.hibernate-orm.` namespace define the default persistence unit. For instance, the following snippet defines a default datasource and a default persistence unit:

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:default;DB_CLOSE_DELAY=-1

quarkus.hibernate-orm.schema-management.strategy=validate
```

Using a map based approach, it is possible to define named persistence units:

```properties
quarkus.datasource."users".db-kind=h2 <1>
quarkus.datasource."users".jdbc.url=jdbc:h2:mem:users;DB_CLOSE_DELAY=-1

quarkus.datasource."inventory".db-kind=h2 <2>
quarkus.datasource."inventory".jdbc.url=jdbc:h2:mem:inventory;DB_CLOSE_DELAY=-1

quarkus.hibernate-orm."users".datasource=users <3>
quarkus.hibernate-orm."users".packages=org.acme.model.user <4>

quarkus.hibernate-orm."inventory".datasource=inventory <5>
quarkus.hibernate-orm."inventory".packages=org.acme.model.inventory
```

1. Define a datasource named `users`.
2. Define a datasource named `inventory`.
3. Define a persistence unit called `users` pointing to the `users` datasource.
4. This configuration property is important, but we will discuss it a bit later.
5. Define a persistence unit called `inventory` pointing to the `inventory` datasource.

> [!NOTE]
> You can mix the default datasource and named datasources or only have one or the other.

> [!NOTE]
> The default persistence unit points to the default datasource by default. For named persistence units, the `datasource` property is mandatory. You can point your persistence unit to the default datasource by setting it to `<default>` (which is the internal name of the default datasource).
>
> It is perfectly valid to have several persistence units pointing to the same datasource.

<a id="multiple-persistence-units-attaching-model-classes"></a>
#### Attaching model classes to persistence units

There are two ways to attach model classes to persistence units, and they should not be mixed:

- Via the `packages` configuration property;
- Via the `@io.quarkus.hibernate.orm.PersistenceUnit` package-level annotation.

If both are mixed, the annotations are ignored and only the `packages` configuration properties are taken into account.

Using the `packages` configuration property is simple:

```properties
quarkus.hibernate-orm.packages=org.acme.model.defaultpu

quarkus.hibernate-orm."users".datasource=users
quarkus.hibernate-orm."users".packages=org.acme.model.user
```

This configuration snippet will create two persistence units:

- The default one which will contain all the model classes under the `org.acme.model.defaultpu` package, subpackages included.
- A named `users` persistence unit which will contain all the model classes under the `org.acme.model.user` package, subpackages included.

You can attach several packages to a persistence unit:

```properties
quarkus.hibernate-orm."users".packages=org.acme.model.shared,org.acme.model.user
```

All the model classes under the `org.acme.model.shared` and `org.acme.model.user` packages will be attached to the `users` persistence unit.

It is also supported to attach a given model class to several persistence units.

> [!NOTE]
> Model classes need to be consistently added to a given persistence unit. That meant that all dependent model classes of a given entity (mapped super classes, embeddables...) are required to be attached to the persistence unit. As we are dealing with the persistence unit at the package level, it should be simple enough.

> [!WARNING]
> Panache entities can be attached to only one persistence unit.
>
> For entities attached to several persistence units, you cannot use Panache. You can mix the two approaches though and mix Panache entities and traditional entities where multiple persistence units are required.
>
> If you have a use case for that and clever ideas about how to implement it without cluttering the simplified Panache approach, contact us on the [quarkus-dev mailing list](https://groups.google.com/d/forum/quarkus-dev).

The second approach to attach model classes to a persistence unit is to use package-level `@io.quarkus.hibernate.orm.PersistenceUnit` annotations. Again, the two approaches cannot be mixed.

To obtain a configuration similar to the one above with the `packages` configuration property, create a `package-info.java` file with the following content:

```java
@PersistenceUnit("users") <1>
package org.acme.model.user;

import io.quarkus.hibernate.orm.PersistenceUnit;
```

1. Be careful, use the `@io.quarkus.hibernate.orm.PersistenceUnit` annotation, not the Jakarta Persistence one.

> [!CAUTION]
> We only support defining the `@PersistenceUnit` for model classes at the package level, using the `@PersistenceUnit` annotation at the class level is not supported in this case.

Note that, similarly to what we do with the configuration property, we take into account the annotated package but also all its subpackages.

### CDI integration

<a id="injecting-entry-points"></a>
#### Injecting entry points

If you are familiar with using Hibernate ORM in Quarkus, you probably already have injected the `EntityManager` using CDI:

```java
@Inject
EntityManager entityManager;
```

This will inject the `EntityManager` of the default persistence unit.

Injecting the `EntityManager` of a named persistence unit (`users` in our example) is as simple as:

```java
@Inject
@PersistenceUnit("users") <1>
EntityManager entityManager;
```

1. Here again, we use the same `@io.quarkus.hibernate.orm.PersistenceUnit` annotation.

> [!NOTE]
> The injected `EntityManager` or `Session` instance is a proxy that requires an active transaction for interaction.
>
> By default it is also possible to use it for read-only operations without a transaction when in a request scope, but this can be disabled by setting `quarkus.hibernate-orm.request-scoped.enabled` to `false`.

You can inject the `EntityManagerFactory` of a named persistence unit using the exact same mechanism:

```java
@Inject
@PersistenceUnit("users")
EntityManagerFactory entityManagerFactory;
```

In addition to `EntityManager` and `EntityManagerFactory`, Quarkus also supports injecting the following JPA/Hibernate components:

```java
@Inject
CriteriaBuilder criteriaBuilder;

@Inject
HibernateCriteriaBuilder hibernateCriteriaBuilder;

@Inject
Metamodel metamodel;

@Inject
jakarta.persistence.Cache cache;

@Inject
org.hibernate.Cache cache;

@Inject
jakarta.persistence.PersistenceUnitUtil persistenceUnitUtil;

@Inject
jakarta.persistence.SchemaManager schemaManager;

@Inject
org.hibernate.relational.SchemaManager schemaManager;
```

These components can also be injected with a specific persistence unit qualifier:

```java
@Inject
@PersistenceUnit("users")
CriteriaBuilder criteriaBuilder;
```

<a id="plugging-in-converters-entity-listeners"></a>
#### Plugging in converters and entity listeners

The Quarkus extension for Hibernate ORM supports injecting [attribute converters](https://docs.hibernate.org/orm/7.4/userguide/html_single/#basic-jpa-convert) and [entity listeners](https://docs.hibernate.org/orm/7.4/userguide/html_single/#events-jpa-callbacks) into Hibernate ORM.

Just use them as you would in vanilla Hibernate ORM (see documentation linked above), and rely on CDI features (`@Inject`, `@PostConstruct`, `@PreDestroy`, ...) in the converter/listener implementation as needed.

When no CDI scope is specified, the converter/listener will behave as if it was `@Dependent` and will be instantiated once per persistence unit it's used in.

You can force a CDI scope by annotating the converter/listener class with for example `@ApplicationScoped`.

In very exotic scenarios, if you need to prevent usage of CDI in a converter/listener even though it uses CDI annotations (`@Inject`, ...), annotate it with `@Vetoed`. The class will then be instantiated by Hibernate ORM through its default constructor.

<a id="plugging-in-other-custom-components"></a>
#### Plugging in other custom components

The Quarkus extension for Hibernate ORM will automatically inject components annotated with `@PersistenceUnitExtension` into Hibernate Search.

The annotation can optionally target a specific persistence unit with `@PersistenceUnitExtension(name = "nameOfYourPU")`.

This feature is available for the following component types:

**`org.hibernate.Interceptor`**\
See [Interceptors](#interceptors).

**`org.hibernate.resource.jdbc.spi.StatementInspector`**\
See [Statement Inspectors](#statement_inspectors).

**`org.hibernate.type.format.FormatMapper`**\
See [Customizing JSON/XML serialization/deserialization](#json_xml_serialization_deserialization).

**`io.quarkus.hibernate.orm.runtime.tenant.TenantResolver`**\
See [Multitenancy](#multitenancy).

**`io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver`**\
See [Programmatically Resolving Tenants Connections](#programmatically-resolving-tenants-connections).

**`org.hibernate.boot.model.FunctionContributor`**\
See [Custom functions, types and mappings](#custom-functions-and-types).

**`org.hibernate.boot.model.TypeContributor`**\
See [Custom functions, types and mappings](#custom-functions-and-types).

<a id="persistence-unit-active"></a>
### Activate/deactivate persistence units

When a persistence unit is configured at build time, and it is assigned entity types or an [active datasource](/guides/datasource/index.md#datasource-active), the persistence unit is active by default. Quarkus starts the corresponding Hibernate ORM `SessionFactory` when the application starts.

To deactivate a persistence unit at runtime, set `quarkus.hibernate-orm[.optional name].active` to `false`.

If a persistence unit is not active:

- The `SessionFactory` does not start during application startup.
- Static CDI injection points involving the persistence unit, such as `@Inject SessionFactory sf` or `@Inject Session session`, cause application startup to fail.
- Dynamic retrieval of the persistence unit, such as through `CDI.getBeanContainer()`, `Arc.instance()`, or an injected `Instance<Session>`, causes an exception to be thrown.
- Other Quarkus extensions that consume the persistence unit may cause application startup to fail.

  In this case, you must also deactivate those other extensions.


This is in particular useful when you want an application to be able to [use one of a pre-determined set of datasources at runtime](/guides/datasource/index.md#datasource-active).

For example, with the following configuration:

```properties
quarkus.hibernate-orm."pg".packages=org.acme.model.shared
quarkus.hibernate-orm."pg".datasource=pg
quarkus.hibernate-orm."pg".active=false
quarkus.datasource."pg".db-kind=h2
quarkus.datasource."pg".active=false
%prod.quarkus.datasource."pg".jdbc.url=jdbc:postgresql:///your_database

quarkus.hibernate-orm."oracle".packages=org.acme.model.shared
quarkus.hibernate-orm."oracle".datasource=oracle
quarkus.hibernate-orm."oracle".active=false
quarkus.datasource."oracle".db-kind=oracle
quarkus.datasource."oracle".active=false
%prod.quarkus.datasource."oracle".jdbc.url=jdbc:oracle:///your_database
```

[Setting](/guides/config-reference/index.md#configuration-sources) `quarkus.hibernate-orm."pg".active=true` and `quarkus.datasource."pg".active=true` at runtime will make only the PostgreSQL persistence unit and datasource available, and setting `quarkus.hibernate-orm."oracle".active=true` and `quarkus.datasource."oracle".active=true` at runtime will make only the Oracle persistence unit and datasource available.

> [!TIP]
> [Custom configuration profiles](/guides/config-reference/index.md#custom-profiles) can help simplify such a setup. By appending the following profile-specific configuration to the one above, you can select a persistence unit/datasource at runtime simply by [setting `quarkus.profile`](/guides/config-reference/index.md#multiple-profiles): `quarkus.profile=prod,pg` or `quarkus.profile=prod,oracle`.
>
> ```properties
> %pg.quarkus.hibernate-orm."pg".active=true
> %pg.quarkus.datasource."pg".active=true
> # Add any pg-related runtime configuration here, prefixed with "%pg."
>
> %oracle.quarkus.hibernate-orm."oracle".active=true
> %oracle.quarkus.datasource."oracle".active=true
> # Add any oracle-related runtime configuration here, prefixed with "%oracle."
> ```

With this setup, ensure that only the *active* persistence unit is accessed. To achieve this, inject an `InjectableInstance<SessionFactory>` or `InjectableInstance<Session>` with an `@Any` qualifier and call [`getActive()`](/guides/cdi-integration/index.md#inactive-synthetic-beans).

```java
import io.quarkus.arc.InjectableInstance;
@ApplicationScoped
public class MyConsumer {
    @Inject
    @Any
    InjectableInstance<Session> session;

    public void doSomething() {
        Session sessionForActivePersistenceUnit = session.getActive();
        // ...
    }
}
```

Alternatively, you can define a [CDI bean producer](/guides/cdi/index.md#ok-you-said-that-there-are-several-kinds-of-beans) for the beans of the default persistence unit. This bean producer redirects to the currently active named persistence unit. This allows beans to be injected directly, as shown below:

```java
public class MyProducer {
    @Inject
    @PersistenceUnit("pg")
    InjectableInstance<Session> pgSessionBean; // <1>

    @Inject
    @PersistenceUnit("oracle")
    InjectableInstance<Session> oracleSessionBean;

    @Produces // <2>
    @ApplicationScoped
    public Session session() {
        if (pgSessionBean.getHandle().getBean().isActive()) { // <3>
            return pgSessionBean.get();
        } else if (oracleSessionBean.getHandle().getBean().isActive()) { // <3>
            return oracleSessionBean.get();
        } else {
            throw new RuntimeException("No active persistence unit!");
        }
    }
}

@ApplicationScoped
public class MyConsumer {
    @Inject
    Session session; // <4>

    public void doSomething() {
        // .. just use the injected session ...
    }
}
```

1. Do not inject a `Session` directly. Injecting inactive beans causes a startup failure. Instead, inject `InjectableInstance<Session>`.
2. Declare a CDI producer method to define the default session. It selects either PostgreSQL or Oracle, depending on which persistence unit is active.
3. Check if a bean is active before retrieving it.
4. Injects the only active persistence unit.

<a id="persistence-xml"></a>
## Setting up and configuring Hibernate ORM with a `persistence.xml`

To set up and configure Hibernate ORM, [using `application.properties`](#hibernate-configuration-properties) is recommended, but you can alternatively use a `META-INF/persistence.xml` file. This is mainly useful for migrating existing code to Quarkus.

> [!WARNING]
> Using a `persistence.xml` file implies a few constraints:
>
> - Persistence units defined in `persistence.xml` always use the [default datasource](/guides/datasource/index.md#configure-a-single-datasource).
> - Persistence units defined in `persistence.xml` must be configured explicitly: Quarkus will keep injection of environment-related configuration to a minimum.
>
>   In particular, Quarkus will not configure the dialect or database version automatically based on the datasource, so if the default configuration of Hibernate ORM doesn't suit your needs, you will need to include in `persistence.xml` configuration such as [`hibernate.dialect`](https://docs.hibernate.org/orm/7.4/userguide/html_single/#settings-hibernate.dialect)/[`jakarta.persistence.database-product-name`](https://docs.hibernate.org/orm/7.4/userguide/html_single/#settings-jakarta.persistence.database-product-name) and possibly [`jakarta.persistence.database-product-version`](https://docs.hibernate.org/orm/7.4/userguide/html_single/#settings-jakarta.persistence.database-product-version).
>
> - Using `persistence.xml` is incompatible with using `quarkus.hibernate-orm.*` properties  in `application.properties`: if you mix them, Quarkus will raise an exception.
> - Developer experience may be impacted negatively when using `persistence.xml` compared to when [using `application.properties`](#hibernate-configuration-properties), due to unavailable features, limited guidance in the Quarkus documentation, and error messages providing resolution hints that cannot be applied (e.g. using `quarkus.hibernate-orm.*` properties).
>
> If your classpath contains a `persistence.xml` that you want to ignore, set the following configuration property:
>
> ```properties
> quarkus.hibernate-orm.persistence-xml.ignore=true
> ```

Your `pom.xml` dependencies as well as your Java code would be identical to the precedent example. The only difference is that you would specify your Hibernate ORM configuration in `META-INF/persistence.xml`:

**Example persistence.xml resource**

```xml
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
             http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd"
             version="2.1">

    <persistence-unit name="CustomerPU" transaction-type="JTA">

        <description>My customer entities</description>

        <properties>
            <!-- Connection specific -->
            <property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQLDialect"/>

            <property name="hibernate.show_sql" value="true"/>
            <property name="hibernate.format_sql" value="true"/>

            <!-- Schema generation on startup -->
            <property name="jakarta.persistence.schema-generation.database.action" value="create"/>

            <property name="jakarta.persistence.validation.mode" value="NONE"/>
        </properties>

    </persistence-unit>
</persistence>
```

When using the `persistence.xml` configuration you are configuring Hibernate ORM directly, so in this case the appropriate reference is the [documentation on hibernate.org](https://docs.hibernate.org/orm/7.4/userguide/html_single/#configurations).

Please remember these are not the same property names as the ones used in the Quarkus `application.properties`, nor will the same defaults be applied.

<a id="xml-mapping"></a>
## XML mapping

Hibernate ORM in Quarkus supports XML mapping. You can add mapping files following the [`orm.xml` format (Jakarta Persistence)](https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a16944) or the [`hbm.xml` format (specific to Hibernate ORM, deprecated)](http://hibernate.org/dtd/hibernate-mapping-3.0.dtd):

- in `application.properties` through the (build-time) [`quarkus.hibernate-orm.mapping-files`](#quarkus-hibernate-orm_quarkus-hibernate-orm-mapping-files) property.
- in [`persistence.xml`](#persistence-xml) through the `<mapping-file>` element.

XML mapping files are parsed at build time.

> [!IMPORTANT]
> The file `META-INF/orm.xml` will always be included by default, if it exists in the classpath.
>
> If that is not what you want, use `quarkus.hibernate-orm.mapping-files = no-file` or `<mapping-file>no-file</mapping-file>`.

## Defining entities in external projects or jars

Hibernate ORM in Quarkus relies on compile-time bytecode enhancements to your entities. If you define your entities in the same project where you build your Quarkus application, everything will work fine.

If the entities come from external projects or jars, you can make sure that your jar is treated like a Quarkus application library by adding an empty `META-INF/beans.xml` file.

This will allow Quarkus to index and enhance your entities as if they were inside the current project.

<a id="dev-mode"></a>
## Hibernate ORM in development mode

During tests and in [development mode](/guides/dev-mode-differences/index.md), Hibernate ORM benefits from [datasource Dev Services](/guides/datasource/index.md#dev-services), making configuration of database connections unnecessary.

The main remaining question is how to initialize that database.

1. The first choice is to use `quarkus.hibernate-orm.schema-management.strategy=drop-and-create` in conjunction with `import.sql`.

   That way for every change to your app and in particular to your entities, the database schema will be properly recreated and your data fixture (stored in `import.sql`) will be used to repopulate it from scratch.

   This is best to perfectly control your environment and works magic with Quarkus live reload mode: your entity changes or any change to your `import.sql` is immediately picked up and the schema updated without restarting the application! In fact, this is the default when using [Dev Services](/guides/dev-services/index.md).

   > [!TIP]
   > You can use a file with a different name than `import.sql` by setting the property `quarkus.hibernate-orm.sql-load-script` in `application.properties`. You can also provide a `.zip` file in the same way which should contain only the files containing the SQL statements to be executed.


1. The second approach is to use `quarkus.hibernate-orm.schema-management.strategy=update`.

   This approach is best when you do many entity changes but still need to work on a copy of the production data or if you want to reproduce a bug that is based on specific database entries. `update` is a best effort from Hibernate ORM and will fail in specific situations including altering your database structure which could lead to data loss. For example if you change structures which violate a foreign key constraint, Hibernate ORM might have to bail out. But for development, these limitations are acceptable.


1. The third approach is to use `quarkus.hibernate-orm.schema-management.strategy=none`.

   This approach is best when you are working on a copy of the production data but want to fully control the schema evolution. Or if you use a database schema migration tool like [Flyway](/guides/flyway/index.md) or [Liquibase](/guides/liquibase/index.md).

   With this approach when making changes to an entity, make sure to adapt the database schema accordingly; you could also use `validate` to have Hibernate verify the schema matches its expectations.


> [!WARNING]
> Do not set `quarkus.hibernate-orm.schema-management.strategy` to `drop-and-create` or `update` in your production environment.

These approaches become really powerful when combined with Quarkus configuration profiles. You can define different [configuration profiles](/guides/config-reference/index.md#profiles) to select different behaviors depending on your environment. This is great because you can define different combinations of Hibernate ORM properties matching the development style you currently need.

**application.properties**

```properties
%dev.quarkus.hibernate-orm.schema-management.strategy = drop-and-create
%dev.quarkus.hibernate-orm.sql-load-script = import-dev.sql

%dev-with-data.quarkus.hibernate-orm.schema-management.strategy = update
%dev-with-data.quarkus.hibernate-orm.sql-load-script = no-file

%prod.quarkus.hibernate-orm.schema-management.strategy = none
%prod.quarkus.hibernate-orm.sql-load-script = no-file
```

You can start dev mode using a custom profile:

**CLI**

```bash
quarkus dev -Dquarkus.profile=dev-with-data
```

**Maven**

```bash
./mvnw quarkus:dev -Dquarkus.profile=dev-with-data
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev -Dquarkus.profile=dev-with-data
```

## Hibernate ORM in production mode

Quarkus comes with default profiles (`dev`, `test` and `prod`). And you can add your own custom profiles to describe various environments (`staging`, `prod-us`, etc).

The Hibernate ORM Quarkus extension sets some default configurations differently in dev and test modes than in other environments.

- `quarkus.hibernate-orm.sql-load-script` is set to `no-file` for all profiles except the `dev` and `test` ones.

You can override it in your `application.properties` explicitly (e.g. `%prod.quarkus.hibernate-orm.sql-load-script = import.sql`) but we wanted you to avoid overriding your database by accident in prod :)

Speaking of, make sure to not drop your database schema in production! Add the following in your properties file.

**application.properties**

```properties
%prod.quarkus.hibernate-orm.schema-management.strategy = none
%prod.quarkus.hibernate-orm.sql-load-script = no-file
```

<a id="flyway"></a>
## Flyway integration

<a id="flyway-transition-from-entities"></a>
### Automatically transitioning to Flyway to Manage Schemas

If you have the [Flyway extension](/guides/flyway/index.md) installed when running in development mode, Quarkus provides a simple way to initialize your Flyway configuration using the schema generated automatically by Hibernate ORM. This is intended to ease the move from the early development phase, where Hibernate can be used to quickly set up the schema, to the production phase, where Flyway is used to manage schema changes.

To use this feature simply open the Dev UI when the `quarkus-flyway` extension is installed and click in the `Datasources` link in the Flyway pane. Hit the `Create Initial Migration` button and the following will happen:

- A `db/migration/V1.0.0__{appname}.sql` file will be created, containing the SQL Hibernate is running to generate the schema
- `quarkus.flyway.baseline-on-migrate` will be set, telling Flyway to automatically create its baseline tables
- `quarkus.flyway.migrate-at-start` will be set, telling Flyway to automatically apply migrations on application startup
- `%dev.quarkus.flyway.clean-at-start` and `%test.quarkus.flyway.clean-at-start` will be set, to clean the DB after reload in dev/test mode

> [!WARNING]
> This button is simply a convenience to quickly get you started with Flyway, it is up to you to determine how you want to manage your database schemas in production. In particular the `migrate-at-start` setting may not be right for all environments.

<a id="flyway-incremental-from-entities"></a>
### Incremental migrations from entity changes

After the first migration is created, Quarkus can derive a draft migration from your updated entity model, so you don’t need to hand‑write a baseline for each change. To create a new migration, click the `Generate Migration File` button in the Dev UI Flyway pane.

> [!WARNING]
> Always review the suggested script. While Quarkus infers schema changes from your entities, domain‑specific data movements, concurrency considerations, and advanced index strategies still require human judgment.

Generated migration files follow the following pattern:

- Major version is extracted from the last existing migration in your project.
- Minor version is the current timestamp at generation time.

<a id="offline"></a>
## Offline startup

By default, Hibernate attempts to connect to the database at startup to fetch metadata. This is useful, for example, to validate the schema or create some temporary tables, making the startup process smoother and more user-friendly.

However, in certain environments, such as when running a Quarkus application in a container within a Kubernetes cluster, this connection might not be possible. For example, if the application runs in one pod and the database in another, the database may not be reachable at startup time. To address this, Quarkus provides an *offline startup* mode, which allows Hibernate to skip connecting to the database during application startup.

When using offline startup, it's important to ensure that the database schema has already been created correctly before the application starts.

You can rely on [Flyway](/guides/flyway/index.md), [Liquibase](/guides/liquibase/index.md) or custom setups to create/migrate your database schema, though obviously at a time where the database *is* accessible -- Flyway's `migrate-at-start` option in particular will just fail at application startup if the database is not reachable.

To enable offline startup, set the following configuration property:

**application.properties**

```properties
quarkus.hibernate-orm.database.start-offline=true
```

You can also fine-tune dialect behavior for specific databases using additional properties, such as:

**application.properties**

```properties
quarkus.hibernate-orm."offline".dialect.mariadb.bytes-per-character=1
quarkus.hibernate-orm."offline".dialect.mariadb.no-backslash-escapes=true
quarkus.hibernate-orm."offline".dialect.mariadb.storage-engine=InnoDB <1>
quarkus.hibernate-orm."inventory".dialect.mysql.storage-engine=MyISAM <2>
```

1. Set the storage engine for the MariaDB dialect of the `offline` persistence unit.
2. Different persistence units can use different storage engines.

Refer to the [Configuration Reference for Hibernate ORM](#configuration-reference) section for more details on the available properties.

<a id="caching"></a>
## Caching

Applications that frequently read the same entities can see their performance improved when the Hibernate ORM second-level cache is enabled.

### Caching of entities

To enable second-level cache, mark the entities that you want cached with `@jakarta.persistence.Cacheable`:

```java
@Entity
@Cacheable
public class Country {
    int dialInCode;
    // ...
}
```

When an entity is annotated with `@Cacheable`, all its field values are cached except for collections and relations to other entities.

This means the entity can be loaded without querying the database, but be careful as it implies the loaded entity might not reflect recent changes in the database.

### Caching of collections and relations

Collections and relations need to be individually annotated to be cached; in this case the Hibernate specific `@org.hibernate.annotations.Cache` should be used, which requires also to specify the `CacheConcurrencyStrategy`:

```java
package org.acme;

@Entity
@Cacheable
public class Country {
    // ...

    @OneToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
    List<City> cities;

    // ...
}
```

### Caching of queries

Queries can also benefit from second-level caching. Cached query results can be returned immediately to the caller, avoiding to run the query on the database.

Be careful as this implies the results might not reflect recent changes.

To cache a query, mark it as cacheable on the `Query` instance:

```java
Query query = ...
query.setHint("org.hibernate.cacheable", Boolean.TRUE);
```

If you have a `NamedQuery` then you can enable caching directly on its definition, which will usually be on an entity:

```java
@Entity
@NamedQuery(name = "Fruits.findAll",
      query = "SELECT f FROM Fruit f ORDER BY f.name",
      hints = @QueryHint(name = "org.hibernate.cacheable", value = "true") )
public class Fruit {
   ...
```

That's all! Caching technology is already integrated and enabled by default in Quarkus, so it's enough to set which ones are safe to be cached.

### Tuning of Cache Regions

Caches store the data in separate regions to isolate different portions of data; such regions are assigned a name, which is useful for configuring each region independently, or to monitor their statistics.

By default, entities are cached in regions named after their fully qualified name, e.g. `org.acme.Country`.

Collections are cached in regions named after the fully qualified name of their owner entity and collection field name, separated by `#` character, e.g. `org.acme.Country#cities`.

All cached queries are by default kept in a single region dedicated to them called `default-query-results-region`.

All regions are bounded by size and time by default. The defaults are `10000` max entries, and `100` seconds as maximum idle time.

The size of each region can be customized via the `quarkus.hibernate-orm.cache."<region_name>".memory.object-count` property (Replace *\<region_name>* with the actual region name).

To set the maximum idle time, provide the duration (see note on duration's format below) via the `quarkus.hibernate-orm.cache."<region_name>".expiration.max-idle` property (Replace *\<region_name>* with the actual region name).

> [!NOTE]
> The double quotes are mandatory if your region name contains a dot. For instance:
>
> ```properties
> quarkus.hibernate-orm.cache."org.acme.MyEntity".memory.object-count=1000
> ```

#### Weight-based eviction

For entities with highly variable sizes (e.g., storing JSON data that ranges from a few bytes to megabytes), count-based eviction cannot reliably bound memory usage. In these cases, use weight-based eviction instead:

```properties
quarkus.hibernate-orm.cache."org.acme.MyEntity".memory.maximum-weight=104857600
quarkus.hibernate-orm.cache."org.acme.MyEntity".memory.weigher-class=org.acme.MyEntityWeigher
```

The `maximum-weight` property sets the total weight limit for the cache region. The `weigher-class` property specifies a `com.github.benmanes.caffeine.cache.Weigher` implementation that assigns a weight to each cache entry:

```java
import com.github.benmanes.caffeine.cache.Weigher;

public class MyEntityWeigher implements Weigher<Object, Object> {
    @Override
    public int weigh(Object key, Object value) {
        // Estimate memory based on entity data size
        return 100; // default weight
    }
}
```

> [!NOTE]
> `object-count` and `maximum-weight` are mutually exclusive per cache region. If `maximum-weight` is set without `weigher-class`, each entry has a default weight of 1.

> [!NOTE]
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
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

### Limitations of Caching

The caching technology provided within Quarkus is currently quite rudimentary and limited.

The team thought it was better to have *some* caching capability to start with, than having nothing; you can expect better caching solution to be integrated in future releases, and any help and feedback in this area is very welcome.

> [!NOTE]
> These caches are kept locally, so they are not invalidated or updated when changes are made to the persistent store by other applications.
>
> Also, when running multiple copies of the same application (in a cluster, for example on Kubernetes/OpenShift), caches in separate copies of the application aren't synchronized.
>
> For these reasons, enabling caching is only suitable when certain assumptions can be made: we strongly recommend that only entities, collections and queries which never change are cached. Or at most, that when indeed such an entity is mutated and allowed to be read out of date (stale) this has no impact on the expectations of the application.
>
> Following this advice guarantees applications get the best performance out of the second-level cache and yet avoid unexpected behaviour.
>
> On top of immutable data, in certain contexts it might be acceptable to enable caching also on mutable data; this could be a necessary tradeoff on selected entities which are read frequently and for which some degree of staleness is acceptable; this " acceptable degree of staleness" can be tuned by setting eviction properties. This is however not recommended and should be done with extreme care, as it might produce unexpected and unforeseen effects on the data.
>
> Rather than enabling caching on mutable data, ideally a better solution would be to use a clustered cache; however at this time Quarkus doesn't provide any such implementation: feel free to get in touch and let this need known so that the team can take this into account.

Finally, the second-level cache can be disabled globally by setting `hibernate.cache.use_second_level_cache` to `false`; this is a setting that needs to be specified in the `persistence.xml` configuration file.

When second-level cache is disabled, all cache annotations are ignored and all queries are run ignoring caches; this is generally useful only to diagnose issues.

<a id="envers"></a>
## Hibernate Envers

The Envers extension to Hibernate ORM aims to provide an easy auditing / versioning solution for entity classes.

In Quarkus, Envers has a dedicated Quarkus Extension `io.quarkus:quarkus-hibernate-envers`; you just need to add this to your project to start using it.

**Additional dependency to enable Hibernate Envers**

```xml
    <!-- Add the Hibernate Envers extension -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-hibernate-envers</artifactId>
    </dependency>
```

At this point the extension does not expose additional configuration properties.

For more information about Hibernate Envers, see [hibernate.org/orm/envers/](https://hibernate.org/orm/envers/).

<a id="spatial"></a>
## Hibernate Spatial

The Spatial extension to Hibernate ORM provides a standardized, cross-database interface to geographic data storage and query functions.

To use Hibernate Spatial, you simply need to add the dependency to your project:

**Additional dependency to enable Hibernate Spatial**

```xml
    <!-- Add the Hibernate Spatial module -->
    <dependency>
        <groupId>org.hibernate.orm</groupId>
        <artifactId>hibernate-spatial</artifactId>
    </dependency>
```

The dependency version is managed by Quarkus, so you don't need to specify it explicitly.

You can find all information about Spatial in the Hibernate user guide: [Hibernate ORM Spatial](https://docs.hibernate.org/orm/7.4/userguide/html_single/#spatial).

<a id="vector"></a>
## Hibernate Vector

The Vector extension to Hibernate ORM provides support for mathematical vector types, functions, and vector similarity search. It enables storage and querying of vector embeddings (arrays of bytes, floats, or doubles) in databases that support native vector types or arrays. This is commonly used in AI/ML applications to persist and query embeddings generated by machine learning models.

To use Hibernate Vector, add the dependency to your project:

**Additional dependency to enable Hibernate Vector**

```xml
    <!-- Add the Hibernate Vector module -->
    <dependency>
        <groupId>org.hibernate.orm</groupId>
        <artifactId>hibernate-vector</artifactId>
    </dependency>
```

The dependency version is managed by Quarkus, so you don't need to specify it explicitly.

To map a vector column, annotate a persistent attribute with one of the various vector `@JdbcTypeCode`s (for example, `SqlTypes.VECTOR` for a `float[]` embedding) and specify the vector length with `@Array(length = …​)`.

**Example entity with a vector**

```java
@Entity
public class Document {

    @Id
    private Long id;

    @Column(name = "the_vector")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    private float[] theVector;
}
```

You can find all information about Vector in the Hibernate user guide: [Hibernate ORM Vector](https://docs.hibernate.org/orm/7.4/userguide/html_single/#vector).

<a id="metrics"></a>
## Metrics

[Micrometer](/guides/telemetry-micrometer/index.md) is capable of exposing metrics that Hibernate ORM collects at runtime. To enable exposure of Hibernate metrics on the `/q/metrics` endpoint, make sure your project depends on a metrics extension and set the configuration property `quarkus.hibernate-orm.metrics.enabled` to `true`.

## Limitations and other things you should know

Quarkus does not modify the libraries it uses; this rule applies to Hibernate ORM as well: when using this extension you will mostly have the same experience as using the original library.

But while they share the same code, Quarkus does configure some components automatically and injects custom implementations for some extension points; this should be transparent and useful but if you're an expert of Hibernate you might want to know what is being done.

### Automatic build time enhancement

Hibernate ORM can use build time enhanced entities; normally this is not mandatory, but it's useful and will have your applications perform better.

Typically, you would need to adapt your build scripts to include the Hibernate Enhancement plugins; in Quarkus this is not necessary as the enhancement step is integrated in the build and analysis of the Quarkus application.

> [!WARNING]
> Due to the usage of enhancement, using the `clone()` method on entities is currently not supported as it will also clone some enhancement-specific fields that are specific to the entity.
>
> This limitation might be removed in the future.

<a id="automatic-integration"></a>
### Automatic integration

**Transaction Manager integration**\
You don't need to set this up, Quarkus automatically injects the reference to the Narayana Transaction Manager. The dependency is included automatically as a transitive dependency of the Hibernate ORM extension. All configuration is optional; for more details see [Using Transactions in Quarkus](/guides/transaction/index.md).

**Connection pool**\
Don't need to choose one either. Quarkus automatically includes the Agroal connection pool; configure your datasource as in the above examples and it will set up Hibernate ORM to use Agroal. More details about this connection pool can be found in [Quarkus - Datasources](/guides/datasource/index.md).

**Second Level Cache**\
As explained earlier in the [Caching section](#caching), you don't need to pick an implementation. A suitable implementation based on [JCache](https://jcp.org/en/jsr/detail?id=107) and [Caffeine](https://github.com/ben-manes/caffeine) is included as a transitive dependency of the Hibernate ORM extension, and automatically integrated during the build.

### Limitations

**XML mapping with duplicate files in the classpath**\
[XML mapping](#xml-mapping) files are expected to have a unique path. + In practice, it's only possible to have duplicate XML mapping files in the classpath in very specific scenarios. For example, if two JARs include a `META-INF/orm.xml` file (with the exact same path but in different JARs), then the mapping file path `META-INF/orm.xml` can only be referenced from a `persistence.xml` **in the same JAR as the `META-INF/orm.xml` file**.

**JMX**\
Management beans are not working in GraalVM native images; therefore, Hibernate's capability to register statistics and management operations with the JMX bean is disabled when compiling into a native image. This limitation is likely permanent, as it's not a goal for native images to implement support for JMX. All such metrics can be accessed in other ways.

**JACC Integration**\
Hibernate ORM's capability to integrate with JACC is disabled when building GraalVM native images, as JACC is not available - nor useful - in native mode.

**Binding the Session to ThreadLocal context**\
It is impossible to use the `ThreadLocalSessionContext` helper of Hibernate ORM as support for it is not implemented. Since Quarkus provides out-of-the-box CDI support, injection or programmatic CDI lookup is a better approach. This feature also didn't integrate well with reactive components and more modern context propagation techniques, making us believe this legacy feature has no future. If you badly need to bind it to a ThreadLocal, it should be trivial to implement in your own code.

**JNDI**\
The JNDI technology is commonly used in other runtimes to integrate different components. A common use case is Java Enterprise servers to bind the TransactionManager and the Datasource components to a name and then have Hibernate ORM configured to look these components up by name. But in Quarkus, this use case doesn't apply as components are injected directly, making JNDI support an unnecessary legacy. To avoid unexpected use of JNDI, full support for JNDI has been disabled in the Hibernate ORM extension for Quarkus. This is both a security precaution and an optimization.

### Other notable differences

**Format of `import.sql`**\
When importing a `import.sql` to set up your database, keep in mind that Quarkus reconfigures Hibernate ORM so to require a semicolon (`;`) to terminate each statement. The default in Hibernate is to have a statement per line, without requiring a terminator other than newline: remember to convert your scripts to use the `;` terminator character if you're reusing existing scripts. This is useful so to allow multi-line statements and human friendly formatting.

## Simplifying Hibernate ORM with Panache

The [Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md) extension facilitates the usage of Hibernate ORM by providing active record style entities (and repositories) and focuses on making your entities trivial and fun to write in Quarkus.

## Configure your datasource

Datasource configuration is extremely simple, but is covered in a different guide as technically it's implemented by the Agroal connection pool extension for Quarkus.

Jump over to [Quarkus - Datasources](/guides/datasource/index.md) for all details.

<a id="multitenancy"></a>
## Multitenancy

"The term multitenancy, in general, is applied to software development to indicate an architecture in which a single running instance of an application simultaneously serves multiple clients (tenants). This is highly common in SaaS solutions. Isolating information (data, customizations, etc.) pertaining to the various tenants is a particular challenge in these systems. This includes the data owned by each tenant stored in the database" ([Hibernate User Guide](https://docs.hibernate.org/orm/7.4/userguide/html_single/#multitenacy)).

Quarkus currently supports the [separate database](https://docs.hibernate.org/orm/7.4/userguide/html_single/#multitenacy-separate-database) approach, the [separate schema](https://docs.hibernate.org/orm/7.4/userguide/html_single/#multitenacy-separate-schema) approach and the [discriminator](https://docs.hibernate.org/orm/7.4/userguide/html_single/#multitenacy-discriminator) approach.

To see multitenancy in action, you can check out the [hibernate-orm-multi-tenancy-schema-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-multi-tenancy-schema-quickstart) or [hibernate-orm-multi-tenancy-database-quickstart](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-multi-tenancy-database-quickstart).

### Writing the application

Let's start by implementing the `/{tenant}` endpoint. As you can see from the source code below it is just a regular Jakarta REST resource:

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("/{tenant}")
public class FruitResource {

    @Inject
    EntityManager entityManager;

    @GET
    @Path("fruits")
    public Fruit[] getFruits() {
        return entityManager.createNamedQuery("Fruits.findAll", Fruit.class)
                .getResultList().toArray(new Fruit[0]);
    }

}
```

In order to resolve the tenant from incoming requests and map it to a specific tenant configuration, you need to create an implementation for the `io.quarkus.hibernate.orm.runtime.tenant.TenantResolver` interface.

```java
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@PersistenceUnitExtension // <1>
@RequestScoped // <2>
public class CustomTenantResolver implements TenantResolver {

    @Inject
    RoutingContext context;

    @Override
    public String getDefaultTenantId() {
        return "base";
    }

    @Override
    public String resolveTenantId() {
        String path = context.request().path();
        String[] parts = path.split("/");

        if (parts.length == 0) {
            // resolve to default tenant config
            return getDefaultTenantId();
        }

        return parts[1];
    }

}
```

1. Annotate the TenantResolver implementation with the `@PersistenceUnitExtension` qualifier to tell Quarkus it should be used in the default persistence unit.

   For [named persistence units](#multiple-persistence-units), use `@PersistenceUnitExtension("nameOfYourPU")`.

2. The bean is made `@RequestScoped` as the tenant resolution depends on the incoming request.

From the implementation above, tenants are resolved from the request path so that in case no tenant could be inferred, the default tenant identifier is returned.

> [!NOTE]
> If you also use [OIDC multitenancy](/guides/security-openid-connect-multitenancy/index.md) and both OIDC and Hibernate ORM tenant IDs are the same, you can get the OIDC tenant id from a `RoutingContext` attribute like in the example below:
>
> ```java
> import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
> import io.quarkus.oidc.runtime.OidcUtils;
> import io.vertx.ext.web.RoutingContext;
>
> @PersistenceUnitExtension
> @RequestScoped
> public class CustomTenantResolver implements TenantResolver {
>
>     @Inject
>     RoutingContext context; <1>
>     ...
>     @Override
>     public String resolveTenantId() {
>         // OIDC has saved the tenant id as the RoutingContext attribute:
>         return context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
>     }
> }
> ```
>
> 1. If the Hibernate `TenantResolver` also resolves tenants when there is no active HTTP request, for example when you query database from a scheduler, the `RoutingContext` CDI bean will not be available. Please inject the `io.quarkus.vertx.http.runtime.CurrentVertxRequest` CDI bean instead and get the `RoutingContext` from this bean.

### Configuring the application

In general, it is not possible to use the Hibernate ORM database generation feature in conjunction with a multitenancy setup. Therefore, you have to disable it, and you need to make sure that the tables are created per schema. The following setup will use the [Flyway](/guides/flyway/index.md) extension to achieve this goal.

<a id="schema-approach"></a>
#### SCHEMA approach

The same data source will be used for all tenants and a schema has to be created for every tenant inside that data source.

> [!CAUTION]
> Some databases like MariaDB/MySQL do not support database schemas by default. In these cases you can either:
>
> 1. Configure the JDBC driver to support schemas. Use `quarkus.datasource.jdbc.additional-jdbc-properties."databaseTerm"=SCHEMA` or `quarkus.datasource."datasource-name".jdbc.additional-jdbc-properties."databaseTerm"=SCHEMA` for [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-connection.html#cj-conn-prop_databaseTerm). Use `quarkus.datasource.jdbc.additional-jdbc-properties."useCatalogTerm"=SCHEMA` or `quarkus.datasource."datasource-name".jdbc.additional-jdbc-properties."useCatalogTerm"=SCHEMA` for [MariaDB Connector/J](https://mariadb.com/docs/connectors/mariadb-connector-j/about-mariadb-connector-j).

and

1. Fall back to the [database approach](#database-approach).

```properties
quarkus.hibernate-orm.schema-management.strategy=none <1>

quarkus.hibernate-orm.multitenant=SCHEMA <2>

quarkus.datasource.db-kind=postgresql <3>

quarkus.flyway.schemas=base,mycompany <4>
quarkus.flyway.locations=classpath:schema
quarkus.flyway.migrate-at-start=true

%prod.quarkus.datasource.username=quarkus_test
%prod.quarkus.datasource.password=quarkus_test
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus_test
```

1. Disable schema generation, because it is not supported by Hibernate ORM for schema multi-tenancy. We'll use Flyway instead, see further down.
2. Enable schema multi-tenancy.

   We use the default datasource here, but could use a named datasource if we wanted to, by following instructions [there](#multiple-persistence-units).

3. [Configure the datasource](/guides/datasource/index.md).
4. Configure [Flyway](/guides/flyway/index.md) for database initialization, because schema generation by Hibernate ORM is not supported in this case.

Here is an example of the Flyway SQL (`V1.0.0__create_fruits.sql`) to be created in the configured folder `src/main/resources/schema`.

```sql
CREATE SEQUENCE base.fruit_seq INCREMENT BY 50; -- 50 is quarkus default
CREATE TABLE base.fruit
(
    id   INT,
    name VARCHAR(40)
);
INSERT INTO base.fruit(id, name) VALUES (1, 'Cherry');
INSERT INTO base.fruit(id, name) VALUES (2, 'Apple');
INSERT INTO base.fruit(id, name) VALUES (3, 'Banana');
ALTER SEQUENCE base.fruit_seq RESTART WITH 4;

CREATE SEQUENCE mycompany.fruit_seq INCREMENT BY 50; -- 50 is quarkus default
CREATE TABLE mycompany.fruit
(
  id   INT,
  name VARCHAR(40)
);
INSERT INTO mycompany.fruit(id, name) VALUES (1, 'Avocado');
INSERT INTO mycompany.fruit(id, name) VALUES (2, 'Apricots');
INSERT INTO mycompany.fruit(id, name) VALUES (3, 'Blackberries');
ALTER SEQUENCE mycompany.fruit_seq RESTART WITH 4;
```

<a id="database-approach"></a>
#### DATABASE approach

For every tenant you need to create a named data source with the same identifier that is returned by the `TenantResolver`.

> [!CAUTION]
> With this approach, all datasources used by the same persistence unit are assumed to point to a database of the same vendor (same `db-kind`) and version.
>
> Mismatches will not be detected, and may result in unpredictable behavior.
>
> The list of datasources is defined at build time, so with this approach the **list** of tenants is **fixed at build time**. If the list of tenants needs to change at runtime, you must [resolve the tenant connections programmatically](#programmatically-resolving-tenants-connections).

```properties
quarkus.hibernate-orm.schema-management.strategy=none <1>

quarkus.hibernate-orm.multitenant=DATABASE <2>
quarkus.hibernate-orm.datasource=base <3>

# Default tenant 'base'
quarkus.datasource.base.db-kind=postgresql <4>
quarkus.flyway.base.locations=classpath:database/base <5>
quarkus.flyway.base.migrate-at-start=true
%prod.quarkus.datasource.base.username=base
%prod.quarkus.datasource.base.password=base
%prod.quarkus.datasource.base.jdbc.url=jdbc:postgresql://localhost:5432/base

# Tenant 'mycompany'
quarkus.datasource.mycompany.db-kind=postgresql <6>
quarkus.flyway.mycompany.locations=classpath:database/mycompany <7>
quarkus.flyway.mycompany.migrate-at-start=true
%prod.quarkus.datasource.mycompany.username=mycompany
%prod.quarkus.datasource.mycompany.password=mycompany
%prod.quarkus.datasource.mycompany.jdbc.url=jdbc:postgresql://localhost:5433/mycompany
```

1. Disable schema generation, because it is not supported by Hibernate ORM for database multi-tenancy. We'll use Flyway instead, see further down.
2. Enable database multi-tenancy.
3. Select a datasource for the persistence unit.

   This is only to allow Quarkus to determine the Hibernate ORM dialect to use; see [this section](#hibernate-dialect-varying-database) for details.

4. [Configure the datasource](/guides/datasource/index.md) for one tenant, `base`.
5. Configure [Flyway](/guides/flyway/index.md) for database initialization for tenant `base`, because schema generation by Hibernate ORM is not supported in this case.
6. [Configure the datasource](/guides/datasource/index.md) for another tenant.

   There could be more tenants, but here we're stopping at two.

7. Configure [Flyway](/guides/flyway/index.md) for database initialization for tenant `mycompany`, because schema generation by Hibernate ORM is not supported in this case.

Following are examples of the Flyway SQL files to be created in the configured folder `src/main/resources/database`.

Schema for tenant `base` (`src/main/resources/database/base/V1.0.0__create_fruits.sql`):

```sql
CREATE SEQUENCE fruit_seq INCREMENT BY 50; -- 50 is quarkus default
CREATE TABLE fruit
(
    id   INT,
    name VARCHAR(40)
);
INSERT INTO fruit(id, name) VALUES (1, 'Cherry');
INSERT INTO fruit(id, name) VALUES (2, 'Apple');
INSERT INTO fruit(id, name) VALUES (3, 'Banana');
ALTER SEQUENCE fruit_seq RESTART WITH 4;
```

Schema for tenant `mycompany` (`src/main/resources/database/mycompany/V1.0.0__create_fruits.sql`):

```sql
CREATE SEQUENCE fruit_seq INCREMENT BY 50; -- 50 is quarkus default
CREATE TABLE fruit
(
  id   INT,
  name VARCHAR(40)
);
INSERT INTO fruit(id, name) VALUES (1, 'Avocado');
INSERT INTO fruit(id, name) VALUES (2, 'Apricots');
INSERT INTO fruit(id, name) VALUES (3, 'Blackberries');
ALTER SEQUENCE fruit_seq RESTART WITH 4;
```

<a id="discriminator-approach"></a>
#### DISCRIMINATOR approach

The default data source will be used for all tenants. All entities defining a field annotated with `@TenantId` will have that field populated automatically, and will get filtered automatically in queries.

```properties
quarkus.hibernate-orm.multitenant=DISCRIMINATOR <1>

quarkus.datasource.db-kind=postgresql <2>

%prod.quarkus.datasource.username=quarkus_test
%prod.quarkus.datasource.password=quarkus_test
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/quarkus_test
```

1. Enable discriminator multi-tenancy.
2. [Configure the datasource](/guides/datasource/index.md).

<a id="programmatically-resolving-tenants-connections"></a>
### Programmatically Resolving Tenants Connections

If you need a more dynamic configuration for the different tenants you want to support and don't want to end up with multiple entries in your configuration file, you can use the `io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver` interface to implement your own logic for retrieving a connection. Creating an application-scoped bean that implements this interface and annotating it with `@PersistenceUnitExtension` (or `@PersistenceUnitExtension("nameOfYourPU")` for a [named persistence unit](#multiple-persistence-units)) will replace the current Quarkus default implementation `io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver`. Your custom connection resolver would allow for example to read tenant information from a database and create a connection per tenant at runtime based on it.

> [!CAUTION]
> [Automatic integrations](#automatic-integration) will **not** work with programmatically-created `ConnectionProvider`. This implies all these integrations, including those from other Quarkus modules (e.g. [SmallRye Health](/guides/smallrye-health/index.md)) must be done manually.

Here is an implementation example of a `TenantConnectionResolver` implementation using standard Quarkus technologies – Agroal for pooling and Narayana for transactions:

```java
@ApplicationScoped
@PersistenceUnitExtension
public class ExampleTenantConnectionResolver implements TenantConnectionResolver {

    private final jakarta.transaction.TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public ExampleTenantConnectionResolver(
            TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    }

    @Override
    public ConnectionProvider resolve(String tenantId) {
        // Use your own ConnectionProvider implementation here
        return new YourOwnCustomConnectionProviderImpl(createDatasource(tenantId));
    }

    private AgroalDataSource createDatasource(String tenantId) {
        try {
            final var txIntegration = new NarayanaTransactionIntegration(
                    transactionManager, transactionSynchronizationRegistry, null, false, null);
            // Fetch JDBC URL, username, password & other values from a per-tenant dynamic source
            final var dataSourceConfig = new AgroalDataSourceConfigurationSupplier()
                    .connectionPoolConfiguration(pc -> pc.initialSize(2)
                            .maxSize(10)
                            .minSize(2)
                            .maxLifetime(Duration.of(5, ChronoUnit.MINUTES))
                            .acquisitionTimeout(Duration.of(30, ChronoUnit.SECONDS))
                            .transactionIntegration(txIntegration)
                            .connectionFactoryConfiguration(
                                    cf -> cf.jdbcUrl("jdbc:postgresql://postgres:5432/" + tenantId)
                                            .credential(new NamePrincipal(username))
                                            .credential(new SimplePassword(password))));
            return AgroalDataSource.from(dataSourceConfig.get());
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Failed to create a new data source based on the existing datasource configuration", ex);
        }
    }
}
```

<a id="custom-functions-and-types"></a>
### Custom functions, types and mappings

To register custom SQL functions or types, in Hibernate ORM, you can implement the standard Hibernate interfaces:

- `org.hibernate.boot.model.FunctionContributor`
- `org.hibernate.boot.model.TypeContributor`

Creating an application-scoped bean that implements one of these interfaces and annotating it with `@PersistenceUnitExtension` (or `@PersistenceUnitExtension("nameOfYourPU")` for a [named persistence unit](#multiple-persistence-units)) will automatically register it with the corresponding persistence unit.

Here is an example of a custom function contributor:

```java
import java.util.List;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ReturnableType;
import org.hibernate.sql.ast.tree.expression.SqlAstNodeRenderingMode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PersistenceUnitExtension
public class CustomFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().register(
                "addHardcodedSuffix",
                new HardcodedSuffixFunction(
                        functionContributions.getTypeConfiguration(), "_some_suffix"));
    }

    private static final class HardcodedSuffixFunction extends AbstractSqmSelfRenderingFunctionDescriptor {
        private final String suffix;

        private HardcodedSuffixFunction(TypeConfiguration typeConfiguration, String suffix) {
            super("addHardcodedSuffix",
                    StandardArgumentsValidators.exactly(1),
                    StandardFunctionReturnTypeResolvers.invariant(
                            typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)),
                    StandardFunctionArgumentTypeResolvers.impliedOrInvariant(typeConfiguration, StandardBasicTypes.STRING)
            );
            this.suffix = suffix;
        }

        @Override
        public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType,
                           SqlAstTranslator<?> walker) {
            sqlAppender.appendSql('(');
            walker.render(sqlAstArguments.get(0), SqlAstNodeRenderingMode.DEFAULT);
            sqlAppender.appendSql(" || '" + suffix + "')");
        }
    }
}

```

And here is an example of a custom type contributor that registers a custom `UserType` (e.g. mapping a Boolean to "Y"/"N"):

```java
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PersistenceUnitExtension
public class CustomTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        // Registers the custom type so it can be used via @Type(value = BooleanYesNoType.class) on your entity property
        typeContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .register(new BooleanYesNoType(), "boolean_yes_no");
    }

    public static final class BooleanYesNoType implements UserType<Boolean> {
        @Override
        public int getSqlType() {
            return SqlTypes.VARCHAR;
        }

        @Override
        public Class<Boolean> returnedClass() {
            return Boolean.class;
        }

        @Override
        public Boolean nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
            String value = rs.getString(position);

            if (value == null) {
                return null;
            }

            return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
        }

        @Override
        public void nullSafeSet(PreparedStatement st, Boolean value, int position, WrapperOptions options) throws SQLException {
            if (value == null) {
                st.setNull(position, SqlTypes.VARCHAR);
            } else {
                st.setString(position, value ? "Y" : "N");
            }
        }

        @Override
        public Boolean deepCopy(Boolean value) {
            return value;
        }

        @Override
        public boolean isMutable() {
            return false;
        }
    }
}
```

<a id="interceptors"></a>
## Interceptors

You can assign an [`org.hibernate.Interceptor`](https://docs.hibernate.org/orm/7.4/userguide/html_single/#events-interceptors) to your `SessionFactory` by simply defining a CDI bean with the appropriate qualifier:

```java
@PersistenceUnitExtension // <1>
public static class MyInterceptor implements Interceptor, Serializable { // <2>
    @Override
    public boolean onLoad(Object entity, Object id, Object[] state, // <3>
            String[] propertyNames, Type[] types) {
        // ...
        return false;
    }
}
```

1. Annotate the interceptor implementation with the `@PersistenceUnitExtension` qualifier to tell Quarkus it should be used in the default persistence unit.

   For [named persistence units](#multiple-persistence-units), use `@PersistenceUnitExtension("nameOfYourPU")`

2. Implement `org.hibernate.Interceptor`. All its methods have default implementations, so you only need to override the ones you use.
3. For example, override `onLoad`, which Hibernate ORM calls just before it initializes an entity instance.

> [!TIP]
> By default, interceptor beans annotated with `@PersistenceUnitExtension` are application-scoped, which means only one interceptor instance will be created per application and reused across all entity managers. For this reason, the interceptor implementation must be thread-safe.
>
> In order to create one interceptor instance per entity manager instead, annotate your bean with `@Dependent`. In that case, the interceptor implementation doesn't need to be thread-safe.

> [!NOTE]
> Due to a limitation in Hibernate ORM itself, `@PreDestroy` methods on `@Dependent`-scoped interceptors will never get called.

<a id="statement_inspectors"></a>
## Statement Inspectors

You can assign a `org.hibernate.engine.jdbc.spi.StatementInspector` to your `SessionFactory` by simply defining a CDI bean with the appropriate qualifier:

```java
@PersistenceUnitExtension // <1>
public class MyStatementInspector implements StatementInspector { // <2>
    @Override
    public String inspect(String sql) {
        // ...
        return sql;
    }
}
```

1. Annotate the statement inspector implementation with the `@PersistenceUnitExtension` qualifier to tell Quarkus it should be used in the default persistence unit.

   For [named persistence units](#multiple-persistence-units), use `@PersistenceUnitExtension("nameOfYourPU")`

2. Implement `org.hibernate.engine.jdbc.spi.StatementInspector`.

<a id="json_xml_serialization_deserialization"></a>
## Customizing JSON/XML serialization/deserialization

By default, Quarkus will try to automatically configure format mappers depending on available extensions. Globally configured `ObjectMapper` (or `Jsonb`) will be used for serialization/deserialization operations when Jackson (or JSON-B) is available. Jackson will take precedence if both Jackson and JSON-B are available at the same time.

JSON and XML serialization/deserialization in Hibernate ORM can be customized by implementing a `org.hibernate.type.format.FormatMapper` and annotating the implementation with the appropriate qualifiers:

```java
import io.quarkus.hibernate.orm.JsonFormat;
import org.hibernate.type.format.FormatMapper;

@JsonFormat // <1>
@PersistenceUnitExtension // <2>
public class MyJsonFormatMapper implements FormatMapper { // <3>
    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        // ...
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
       // ...
    }
}
```

1. Annotate the format mapper implementation with the `@JsonFormat` qualifier to tell Quarkus that this mapper is specific to JSON serialization/deserialization.

   > [!WARNING]
   > Make sure the Quarkus-specific `@io.quarkus.hibernate.orm.JsonFormat` annotation is used and not the one from Jackson.

2. Annotate the format mapper implementation with the `@PersistenceUnitExtension` qualifier to tell Quarkus it should be used in the default persistence unit.

   For [named persistence units](#multiple-persistence-units), use `@PersistenceUnitExtension("nameOfYourPU")`

3. Implement `org.hibernate.type.format.FormatMapper`.

In case of a custom XML format mapper, a different CDI qualifier must be applied:

```java
import io.quarkus.hibernate.orm.XmlFormat;
import org.hibernate.type.format.FormatMapper;

@XmlFormat // <1>
@PersistenceUnitExtension // <2>
public class MyJsonFormatMapper implements FormatMapper { // <3>
    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        // ...
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
       // ...
    }
}
```

1. Annotate the format mapper implementation with the `@XmlFormat` qualifier to tell Quarkus that this mapper is specific to XML serialization/deserialization.
2. Annotate the format mapper implementation with the `@PersistenceUnitExtension` qualifier to tell Quarkus it should be used in the default persistence unit.

   For [named persistence units](#multiple-persistence-units), use `@PersistenceUnitExtension("nameOfYourPU")`

3. Implement `org.hibernate.type.format.FormatMapper`.

> [!NOTE]
> Format mappers **must** have both `@PersistenceUnitExtension` and either `@JsonFormat` or `@XmlFormat` CDI qualifiers applied.
>
> Having multiple JSON (or XML) format mappers registered for the same persistence unit will result in an exception, because of the ambiguity.

<a id="validator_integration"></a>
## Validation modes and Hibernate Validator integration

Hibernate Validator integration into Hibernate ORM opens up the following capabilities:

- performing entity validation on lifecycle events
- applying constraint information from entities to DDL

From Quarkus's perspective, this is controlled by the [`quarkus.hibernate-orm.validation.mode` configuration property](#quarkus-hibernate-orm_quarkus-hibernate-orm-validation-mode). The available validation modes are:

- `auto` -- the default option; works the same as `callback` and `ddl` enabled simultaneously when the application uses `quarkus-hibernate-validator`, and as `none` otherwise.
- `callback` -- Hibernate Validator will perform the lifecycle event validation.
- `ddl` -- Hibernate Validator constraints will be considered for the [DDL operations](#dev-mode)
- `none` -- Hibernate Validator integration will be disabled.

While all constraints with corresponding Jakarta Validation rules will apply during the `callback` validation, in the `ddl` mode, only a subset of constraints is used to influence the DDL. In the Hibernate Validator documentation, you can find [the list of available constraints](https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints) and their impact on the DDL generation.

Let's consider having a simple entity with some constraints applied to its properties:

```java
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class MyEntity {
    @Id
    @GeneratedValue
    public long id;

    @NotNull
    @NotEmpty
    @Size(max = 50)
    public String name;

    public String value;
}
```

With the `ddl` mode enabled, the resulting schema would be expected to have the following constraints:

```sql
create table myentity
(
    id    bigint      not null  primary key,
    name  varchar(50) not null, // <1>
    value varchar(255),         // <2>
);
```

1. The `name` column has a `not null` constraint because of the `@NotNull` constraint, and the length of the values is limited to `50` because of the `@Size(max=50)` constraint.
2. Since the `value` property in the entity has no constraints, there are no additional constraints in the DDL, and the `255` length limit comes from the default `jakarta.persistence.Column#length()`.

With the `callback` mode, expect getting a `jakarta.validation.ConstraintViolationException` thrown:

```java
try {
    MyEntity entity = new MyEntity();
    entity.setName(veryLongName);
    em.persist(entity);
    em.flush();
} catch (ConstraintViolationException exception) {
    // handle the constraint violations somehow
}
```

Since Quarkus has built-in exception mappers for `jakarta.validation.ConstraintViolationException`, explicitly handling these exceptions might be redundant. See the [REST end point validation](/guides/validation/index.md#rest-end-point-validation) section of the Hibernate Validator guide for more details.

<a id="jakarta-data"></a>
## Static metamodel and Jakarta Data

Both static metamodel and Jakarta Data capabilities of Hibernate ORM are available in Quarkus through the `quarkus-data-processor` annotation processor. Since it is an annotation processor, you must configure it accordingly in your build tool:

**pom.xml**

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <!-- This setting is required for the annotation processor dependencies to be managed by Quarkus.
             More information is available in Maven compiler plugin documentation:
             https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPathsUseDepMgmt -->
        <annotationProcessorPathsUseDepMgmt>true</annotationProcessorPathsUseDepMgmt>
        <annotationProcessorPaths>
            <path>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-data-processor</artifactId>
                <!-- Note, no artifact version is required, it's managed by Quarkus.  -->
            </path>
            <!-- other processors that may be required by your app -->
        </annotationProcessorPaths>
        <!-- Other compiler plugin configuration options -->
    </configuration>
</plugin>
```

**build.gradle**

```gradle
// Enforce the version management of your annotation processor dependencies,
// so that there's no need to define an explicit version of the quarkus-data-processor
annotationProcessor enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
annotationProcessor 'io.quarkus:quarkus-data-processor'
```

### Static metamodel

The generated static metamodel allows for building queries in a type-safe manner. Let's consider having a simple entity:

```java
@Entity
public class MyEntity {
    @Id
    @GeneratedValue
    public Integer id;

    @Column(unique = true)
    public String name;
}
```

A query created with the help of static metamodel may look as:

```java
var builder = session.getCriteriaBuilder();
var criteria = builder.createQuery(MyEntity.class);
var e = criteria.from(MyEntity_.class);
criteria.where(e.get(MyEntity_.name).equalTo(name));
var query = session.createQuery(criteria);
var result = query.list();
```

For a more detailed overview of static metamodel, please refer to the [Jakarta Persistence specification](https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a6072).

### Jakarta Data

Jakarta Data requires, besides having the `quarkus-data-processor` annotation processor in place, one extra dependency to be added:

**pom.xml**

```xml
<dependency>
    <groupId>jakarta.data</groupId>
    <artifactId>jakarta.data-api</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation 'jakarta.data:jakarta.data-api'
```

With this dependency, and the annotation processor in place you could simply create your repositories as follows:

```java
@Repository
public interface MyRepository extends CrudRepository<MyEntity, Integer> { // <1>

    @Query("select e from MyEntity e where e.name like :name") // <2>
    List<MyEntity> findByName(String name);

    @Delete // <3>
    void delete(String name);

}
```

1. To skip the boilerplate definition of CRUD operations, we can use one of the available interfaces (e.g. `CrudRepository` or `BasicRepository`).
2. Adding custom queries with parameters is as easy as providing your query string to the `@Query` annotation.
3. If the basic CRUD operations from the Jakarta Data interfaces are not enough, we can always add a custom one, in this case a delete operation that removes `MyEntity`s by name.

And then the repository can be used as any other bean:

```java
public class MyEntityResource {

    @Inject
    MyRepository repository;

    @POST
    @Transactional
    public void create(MyEntity entity) {
        repository.insert(entity);
    }

    // ...

}
```

> [!NOTE]
> When working with non-default persistence units, remember to specify the persistence unit name the repository is targeting in the `dataStore` attribute of the repository annotation:
>
> ```java
> @Repository(dataStore = "other") // <1>
> public interface MyNonDefaultPURepository {
>     // ...
> }
> ```
>
> 1. Pass the name of the non-default persistence unit to the repository annotation (`other` in this example).
>
> Review the [Javadoc of the `@Repository` annotation](https://jakarta.ee/specifications/data/1.0/apidocs/jakarta.data/jakarta/data/repository/repository#dataStore()) to learn more about the `dataStore` attribute.

Please refer to the corresponding [Hibernate Data Repositories](https://hibernate.org/repositories/) and [Jakarta Data](https://jakarta.ee/specifications/data/1.0/jakarta-data-1.0) guides to learn what else they have to offer.

#### Secure Jakarta Data repositories

Quarkus Security provides an initial support for securing Jakarta Data Repositories with security annotations.

**Example Jakarta Data repository with method-level security annotation**

```java
@Repository
public interface MyRepository extends CrudRepository<MyEntity, Integer> {

    @RolesAllowed("admin")
    @Delete
    void delete(String name);

}
```

**Example Jakarta Data repository with class-level security annotation**

```java
@Authenticated
@Repository
public interface MyRepository extends CrudRepository<MyEntity, Integer> {

    @Delete
    void delete(String name);

}
```

> [!WARNING]
> In the example above, only methods directly declared on the `MyRepository` interface are secured. Methods inherited from `CrudRepository`, such as `insert`, are not secured because type-level annotations only affect the type they annotate.

> [!IMPORTANT]
> Currently, generic interface methods using type variables or wildcards cannot be secured reliably with standard security annotations. Therefore, you must not attempt to secure such methods and instead use two alternatives described below.
>
> **Example repository with a generic method using a type variable**
>
> ```java
> public interface MyParentRepository<T extends MyEntity> {
>
>     @Find
>     Stream<T> findAll(Order<T> order);
>
> }
>
> @Repository
> public interface MyRepository extends MyParentRepository<MyEntity> {
> }
> ```
>
> Alternative 1: Apply a security annotation to the calling method in your REST layer:
>
> ```java
> @Path("my-entity")
> public class MyEntityResource {
>
>     @Inject
>     MyRepository myRepository;
>
>     @PermissionsAllowed("find-all")
>     @GET
>     public List<MyEntity> findAll() {
>         return myRepository.findAll(Order.by(Sort.asc(MyEntity_.NAME))).toList();
>     }
>
> }
> ```
>
> Alternative 2: Replace the type variable `T` with a concrete type in the repository interface:
>
> ```java
> @Repository
> public interface MyRepository {
>
>     @PermissionsAllowed("find-all")
>     @Find
>     Stream<MyEntity> findAll(Order<MyEntity> order);
>
> }
> ```

<a id="configuration-reference"></a>
## Configuration Reference for Hibernate ORM

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
