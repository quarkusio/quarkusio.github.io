# Configure data sources in Quarkus

Use a unified configuration model to define data sources for Java Database Connectivity (JDBC) and Reactive drivers.

Applications use datasources to access relational databases. Quarkus provides a unified configuration model to define datasources for Java Database Connectivity (JDBC) and Reactive database drivers.

Quarkus uses [Agroal](https://agroal.github.io/) and [Vert.x](https://vertx.io/) to provide high-performance, scalable datasource connection pooling for JDBC and reactive drivers. The `quarkus-jdbc-\*` and `quarkus-reactive-*-client` extensions provide build-time optimizations and integrate configured datasources with Quarkus features such as security, health checks, and metrics.

For more information about consuming and using a reactive datasource, see the Quarkus [Reactive SQL clients](/guides/reactive-sql-clients/index.md) guide.

Additionally, refer to the Quarkus [Hibernate ORM](/guides/hibernate-orm/index.md) guide for information about consuming and using a JDBC datasource.

## Get started with configuring `datasources` in Quarkus

For users familiar with the fundamentals, this section provides an overview and code samples to set up datasources quickly.

For more advanced configuration with examples, see [References](#datasource-reference).

<a id="dev-services"></a>
### Zero-config setup in development mode

Quarkus simplifies database configuration by offering the Dev Services feature, enabling zero-config database setup for testing or running in development (dev) mode. In dev mode, the suggested approach is to use Dev Services and let Quarkus handle the database for you, whereas for production mode, you provide explicit database configuration details pointing to a database managed outside of Quarkus.

To use Dev Services, add the appropriate driver extension, such as `jdbc-postgresql`, for your desired database type to the `pom.xml` file. In dev mode, if you do not provide any explicit database connection details, Quarkus automatically handles the database setup and provides the wiring between the application and the database.

When you provide user credentials, Quarkus configures the database to use them. This helps when you connect to the database with an external tool.

To use this feature, ensure a Docker or Podman container runtime is installed, depending on the database type. Certain databases, such as H2, operate in in-memory mode and do not require a container runtime.

> [!TIP]
> Prefix the actual connection details for prod mode with `%prod.` to ensure they are not applied in dev mode. For more information, see the [Profiles](/guides/config-reference/index.md#profiles) section of the "Configuration reference" guide.

For more information about Dev Services, see [Dev Services overview](/guides/dev-services/index.md).

For more details and optional configurations, see [Dev Services for databases](/guides/databases-dev-services/index.md).

### Configure a JDBC datasource

1. Add the correct JDBC extension for the database of your choice.

- `quarkus-jdbc-db2`
- `quarkus-jdbc-h2`
- `quarkus-jdbc-mariadb`
- `quarkus-jdbc-mssql`
- `quarkus-jdbc-mysql`
- `quarkus-jdbc-oracle`
- `quarkus-jdbc-postgresql`

1. Configure your JDBC datasource:

   ```properties
   quarkus.datasource.db-kind=postgresql <1>
   quarkus.datasource.username=<your username>
   quarkus.datasource.password=<your password>

   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/hibernate_orm_test
   quarkus.datasource.jdbc.max-size=16
   ```

   1. This configuration value is only required if there is more than one database extension on the classpath.


If only one viable extension is available, Quarkus assumes this is the correct one. When you add a driver to the test scope, Quarkus automatically includes it in testing.

#### JDBC connection pool size adjustment

To protect your database from overload during load peaks, size the pool appropriately to throttle the database load. The optimal pool size depends on many factors, such as the number of parallel application users or the nature of the workload.

Be aware that setting the pool size too low might cause some requests to time out while waiting for a connection.

For more information about pool size adjustment properties, see the [JDBC configuration reference](#jdbc-configuration) section.

#### Read-only JDBC pools

You can mark a JDBC pool as read-only so that connections are created with [`Connection.setReadOnly(true)`](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/Connection.html#setReadOnly(boolean)). The JDBC driver is responsible for enforcing read-only mode.

This is useful for secondary pools that target read replicas.

```properties
quarkus.datasource.jdbc.read-only=true
```

For a named datasource:

```properties
quarkus.datasource."readonly".db-kind=postgresql
quarkus.datasource."readonly".jdbc.url=jdbc:postgresql://replica:5432/mydb
quarkus.datasource."readonly".jdbc.read-only=true
```

When the property is unset, Quarkus does not configure read-only mode and Agroal's default applies (connections are not read-only).

### Configure a reactive datasource

1. Add the correct reactive extension for the database of your choice.

- `quarkus-reactive-db2-client`
- `quarkus-reactive-mssql-client`
- `quarkus-reactive-mysql-client`
- `quarkus-reactive-oracle-client`
- `quarkus-reactive-pg-client`

1. Configure your reactive datasource:

   ```properties
   quarkus.datasource.db-kind=postgresql <1>
   quarkus.datasource.username=<your username>
   quarkus.datasource.password=<your password>

   quarkus.datasource.reactive.url=postgresql:///your_database
   quarkus.datasource.reactive.max-size=20
   ```

   1. This configuration value is only required if there is more than one Reactive driver extension on the classpath.


<a id="configure-datasources"></a>
## Configure datasources

The following section describes the configuration for single or multiple datasources. For simplicity, we will reference a single datasource as the default (unnamed) datasource.

<a id="configure-a-single-datasource"></a>
### Configure a single datasource

A datasource can be JDBC, reactive, or both. This depends on the configuration and the selection of project extensions.

1. Define a datasource with the following configuration property, where `db-kind` defines which database platform to connect to, for example, `h2`:

   ```properties
   quarkus.datasource.db-kind=h2
   ```

   Quarkus derives the JDBC driver class from the `db-kind` database platform value.

   > [!NOTE]
   > This step is required only if your application depends on multiple database drivers. If the application uses a single driver, Quarkus detects that driver automatically.

   Quarkus currently includes the following built-in database kinds:


- DB2: `db2`
- H2: `h2`
- MariaDB: `mariadb`
- Microsoft SQL Server: `mssql`
- MySQL: `mysql`
- Oracle: `oracle`
- PostgreSQL: `postgresql`, `pgsql` or `pg`
- To use a database kind that is not built-in, use `other` and define the JDBC driver explicitly

  > [!NOTE]
  > You can use any JDBC driver in a Quarkus app in JVM mode as described in [Custom databases and drivers](#other-databases). However, using a non-built-in database kind is unlikely to work when compiling your application to a native executable.
  >
  > For native executable builds, use the available JDBC Quarkus extensions, or contribute a custom extension for your specific driver.


1. Configure the following properties to define credentials:

   ```properties
   quarkus.datasource.username=<your username>
   quarkus.datasource.password=<your password>
   ```

   You can also retrieve the password from Vault by [using a credential provider](https://docs.quarkiverse.io/quarkus-vault/dev/vault-datasource.html) for your datasource.


Until now, the configuration has been the same regardless of whether you are using a JDBC or a reactive driver. After you define the database kind and credentials, the rest depends on the driver you are using. It is possible to use JDBC and a reactive driver simultaneously.

<a id="jdbc-datasource"></a>
#### JDBC datasource

JDBC is the most common database connection pattern, typically needed when used in combination with non-reactive Hibernate ORM.

1. To use a JDBC datasource, start by adding the necessary dependencies:

.. For use with a built-in JDBC driver, choose and add the Quarkus extension for your relational database driver from the list below: +

- H2 - `quarkus-jdbc-h2`

  > [!NOTE]
  > You can configure H2 databases to run in "embedded mode"
  >
  > For suggestions regarding integration testing, see [Testing with in-memory databases](#in-memory-databases).

- DB2 - `quarkus-jdbc-db2`
- MariaDB - `quarkus-jdbc-mariadb`
- Microsoft SQL Server - `quarkus-jdbc-mssql`
- MySQL - `quarkus-jdbc-mysql`
- Oracle - `quarkus-jdbc-oracle`
- PostgreSQL - `quarkus-jdbc-postgresql`
- Other JDBC extensions, such as [SQLite](https://github.com/quarkiverse/quarkus-jdbc-sqlite) and its [documentation](https://docs.quarkiverse.io/quarkus-jdbc-sqlite/dev/index.html), can be found in the [Quarkiverse](https://github.com/quarkiverse).

  For example, to add the PostgreSQL driver dependency:

  ```shell
  ./mvnw quarkus:add-extension -Dextensions="jdbc-postgresql"
  ```

  > [!NOTE]
  > When you add a built-in JDBC driver extension, Quarkus also adds the `quarkus-agroal` extension. Agroal provides the JDBC connection pool for built-in and custom JDBC drivers. When you use a custom JDBC driver, add `quarkus-agroal` explicitly.


.. For use with a custom JDBC driver, add the `quarkus-agroal` dependency to your project alongside the extension for your relational database driver: +

```shell
./mvnw quarkus:add-extension -Dextensions="agroal"
```

\+ To use a JDBC driver for another database, [use a database with no built-in extension or with a different driver](#other-databases).

1. Configure the JDBC connection by defining the JDBC URL property:

   ```properties
   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/hibernate_orm_test
   ```

   > [!NOTE]
   > Note the `jdbc` prefix in the property name. All JDBC-specific configuration properties have the `jdbc` prefix. For reactive datasources, the prefix is `reactive`.


For more information about configuring JDBC, see [JDBC URL format reference](#jdbc-url) and [Quarkus extensions and database drivers reference](#extensions-and-database-drivers-reference).

<a id="other-databases"></a>
##### Custom databases and drivers

If Quarkus does not provide a JDBC extension for your database, or you need to use a different JDBC driver, such as one for OpenTelemetry, you can configure the JDBC driver explicitly.

Without an extension, JDBC drivers are expected to work correctly in JVM mode. However, they are unlikely to function when compiling your application into a native executable. To build a native executable, use an existing Quarkus JDBC extension or contribute a new extension for your driver.

**An example for defining access to a database with no built-in support in JVM mode:**

```properties
quarkus.datasource.db-kind=other
quarkus.datasource.jdbc.driver=oracle.jdbc.driver.OracleDriver
quarkus.datasource.jdbc.url=jdbc:oracle:thin:@192.168.1.12:1521/ORCL_SVC
quarkus.datasource.username=scott
quarkus.datasource.password=tiger
```

For details about JDBC configuration options and configuring other aspects, such as the connection pool size, refer to the [JDBC configuration reference](#jdbc-configuration) section.

##### Consuming the datasource

With Hibernate ORM, the Hibernate layer automatically picks up the datasource and uses it.

For the in-code access to the datasource, obtain it as any other bean as follows:

```java
@Inject
AgroalDataSource defaultDataSource;
```

In the above example, the type is `AgroalDataSource`, a `javax.sql.DataSource` subtype. Because of this, you can also use `javax.sql.DataSource` as the injected type.

##### Oracle considerations

As documented in [issue #36265](https://github.com/quarkusio/quarkus/issues/36265), Oracle unexpectedly commits uncommitted transactions when closing a connection. This means that when stopping Quarkus, in-progress transactions might be committed even if they are incomplete.

Because this behavior is unexpected and can lead to data loss, an interceptor rolls back any unfinished transactions when closing a connection. However, if you use XA transactions, the transaction manager handles the rollback.

If the behavior introduced in 3.18 causes issues for your workload, deactivate it by setting the `-Dquarkus-oracle-no-automatic-rollback-on-connection-close` system property to `true`. Make sure to report your use case in the [issue tracker](https://github.com/quarkusio/quarkus/issues) so we can adjust this behavior if needed, for example, with more permanent settings.

<a id="reactive-datasource"></a>
#### Reactive datasource

Quarkus offers several reactive clients for use with a reactive datasource.

1. Add the corresponding extension to your application:
   - DB2: `quarkus-reactive-db2-client`
   - MariaDB/MySQL: `quarkus-reactive-mysql-client`
   - Microsoft SQL Server: `quarkus-reactive-mssql-client`
   - Oracle: `quarkus-reactive-oracle-client`
   - PostgreSQL: `quarkus-reactive-pg-client`

   The installed extension must be consistent with the `quarkus.datasource.db-kind` you define in your datasource configuration.


1. After adding the driver, configure the connection URL and define a proper size for your connection pool.

   ```properties
   quarkus.datasource.reactive.url=postgresql:///your_database
   quarkus.datasource.reactive.max-size=20
   ```


##### Reactive connection pool size adjustment

To protect your database from overload during load peaks, set the pool size to throttle database load. The correct pool size depends on factors such as the number of concurrent users and the workload type.

Be aware that setting the pool size too low might cause some requests to time out while waiting for a connection.

For more information about pool size adjustment properties, see the [Reactive datasource configuration reference](#reactive-configuration) section.

<a id="jdbc-and-reactive-datasources-simultaneously"></a>
#### JDBC and reactive datasources simultaneously

When you add both a JDBC extension and a reactive datasource extension for the same `db-kind`, Quarkus creates both JDBC and reactive datasources by default.

- To use the [JDBC](#jdbc-datasource) and [reactive](#reactive-datasource) datasources simultaneously:

  ```properties
  %prod.quarkus.datasource.reactive.url=postgresql:///your_database
  %prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/hibernate_orm_test
  ```


If you do not want to have both a JDBC datasource and a reactive datasource created, use the following configuration.

- To disable the JDBC datasource explicitly:

  ```properties
  quarkus.datasource.jdbc=false
  ```


- To disable the reactive datasource explicitly:

  ```properties
  quarkus.datasource.reactive=false
  ```

  > [!TIP]
  > In most cases, the configuration above will be optional as either a JDBC driver or a reactive datasource extension will be present, not both.


### Configure multiple datasources

> [!NOTE]
> The Hibernate ORM extension supports defining [persistence units](/guides/hibernate-orm/index.md#multiple-persistence-units) by using configuration properties. For each persistence unit, point to the datasource of your choice.

Defining multiple datasources works like defining a single datasource, with one important change - you have to specify a name (configuration property) for each datasource.

The following example provides three different datasources:

- the default one
- a datasource named `users`
- a datasource named `inventory`

Each with its configuration:

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=username-default
quarkus.datasource.jdbc.url=jdbc:h2:mem:default
quarkus.datasource.jdbc.max-size=13

quarkus.datasource.users.db-kind=h2
quarkus.datasource.users.username=username1
quarkus.datasource.users.jdbc.url=jdbc:h2:mem:users
quarkus.datasource.users.jdbc.max-size=11

quarkus.datasource.inventory.db-kind=h2
quarkus.datasource.inventory.username=username2
quarkus.datasource.inventory.jdbc.url=jdbc:h2:mem:inventory
quarkus.datasource.inventory.jdbc.max-size=12
```

Notice there is an extra section in the configuration property. The syntax is as follows: `quarkus.datasource.[optional name.][datasource property]`.

> [!NOTE]
> Even when you install only one database extension, named datasources must set at least one build-time property so Quarkus can detect them. In most cases, set `db-kind`.
>
> You can also set Dev Services properties to create named datasources as described in the [Dev Services for Databases](/guides/databases-dev-services/index.md) guide.

#### Named datasource injection

When you configure multiple datasources, each `DataSource` also has the `io.quarkus.agroal.DataSource` qualifier with the datasource name as the value.

After you configure three datasources as described in the previous section, inject each datasource as follows:

```java
@Inject
AgroalDataSource defaultDataSource;

@Inject
@DataSource("users")
AgroalDataSource usersDataSource;

@Inject
@DataSource("inventory")
AgroalDataSource inventoryDataSource;
```

<a id="datasource-active"></a>
### Activate or deactivate datasources

When a datasource is configured at build time, and its URL is set at runtime, it is active by default. Quarkus starts the corresponding JDBC connection pool or reactive client when the application starts.

To deactivate a datasource at runtime, either:

- Do not set `quarkus.datasource[.optional name].jdbc.url` or `quarkus.datasource[.optional name].reactive.url`.
- Set `quarkus.datasource[.optional name].active` to `false`.

If a datasource is not active:

- The datasource does not attempt to connect to the database during application startup.
- The datasource does not contribute a [health check](#datasource-health-check).
- Static CDI injection points involving the datasource, such as `@Inject DataSource ds` or `@Inject Pool pool`, cause application startup to fail.
- Dynamic retrieval of the datasource, such as through `CDI.getBeanContainer()`, `Arc.instance()`, or an injected `Instance<DataSource>`, causes an exception to be thrown.
- Other Quarkus extensions that consume the datasource can cause application startup to fail.

  In this case, you must also deactivate those other extensions. To see an example of this scenario, refer to the [Activate/deactivate persistence units](/guides/hibernate-orm/index.md#persistence-unit-active) section of the Hibernate ORM guide. For Hibernate ORM, Quarkus deactivates the persistence unit when the datasource is inactive.


This feature is especially useful when the application must select one datasource from a predefined set at runtime.

**An example of configuring multiple datasources for runtime selection:**

```properties
quarkus.datasource."pg".db-kind=postgres
quarkus.datasource."pg".active=false
quarkus.datasource."pg".jdbc.url=jdbc:postgresql:///your_database

quarkus.datasource."oracle".db-kind=oracle
quarkus.datasource."oracle".active=false
quarkus.datasource."oracle".jdbc.url=jdbc:oracle:thin:@localhost:1521/your_database
```

Setting `quarkus.datasource."pg".active=true` [at runtime](/guides/config-reference/index.md#configuration-sources) makes only the PostgreSQL datasource available. Setting `quarkus.datasource."oracle".active=true` at runtime makes only the Oracle datasource available.

> [!TIP]
> [Custom configuration profiles](/guides/config-reference/index.md#custom-profiles) simplify this setup. By appending the following profile-specific configuration to the one above, you can select a persistence unit or datasource at runtime by [setting `quarkus.profile`](/guides/config-reference/index.md#multiple-profiles). For example, use `quarkus.profile=prod,pg` or `quarkus.profile=prod,oracle`.
>
> ```properties
> %pg.quarkus.hibernate-orm."pg".active=true
> %pg.quarkus.datasource."pg".active=true
> # Add any PostgreSQL-related runtime configuration here, prefixed with "%pg."
>
> %oracle.quarkus.hibernate-orm."oracle".active=true
> %oracle.quarkus.datasource."oracle".active=true
> # Add any Oracle-related runtime configuration here, prefixed with "%oracle."
> ```

With this setup, ensure that only the *active* datasource is accessed. To achieve this, inject an `InjectableInstance<DataSource>` or `InjectableInstance<Pool>` with an `@Any` qualifier and call [`getActive()`](/guides/cdi-integration/index.md#inactive-synthetic-beans).

```java
import io.quarkus.arc.InjectableInstance;
@ApplicationScoped
public class MyConsumer {
    @Inject
    @Any
    InjectableInstance<DataSource> dataSource;

    public void doSomething() {
        DataSource activeDataSource = dataSource.getActive();
        // ...
    }
}
```

Alternatively, define a [CDI bean producer](/guides/cdi/index.md#ok-you-said-that-there-are-several-kinds-of-beans) for the default datasource. The bean producer redirects to the active named datasource. The application can then inject the default datasource directly, as shown below:

```java
public class MyProducer {
    @Inject
    @DataSource("pg")
    InjectableInstance<DataSource> pgDataSourceBean; // <1>

    @Inject
    @DataSource("oracle")
    InjectableInstance<DataSource> oracleDataSourceBean;

    @Produces // <2>
    @ApplicationScoped
    public DataSource dataSource() {
        if (pgDataSourceBean.getHandle().getBean().isActive()) { // <3>
            return pgDataSourceBean.get();
        } else if (oracleDataSourceBean.getHandle().getBean().isActive()) { // <3>
            return oracleDataSourceBean.get();
        } else {
            throw new RuntimeException("No active datasource!");
        }
    }
}

@ApplicationScoped
public class MyConsumer {
    @Inject
    DataSource dataSource; // <4>

    public void doSomething() {
        // .. just use the injected datasource ...
    }
}
```

1. Do not inject a `DataSource` or `AgroalDataSource` directly. Injecting inactive beans causes a startup failure. Instead, inject `InjectableInstance<DataSource>` or `InjectableInstance<AgroalDataSource>`.
2. Declare a CDI producer method to define the default datasource. It selects either PostgreSQL or Oracle, depending on which one is active.
3. Check if a bean is active before retrieving it.
4. Injects the only active datasource.

<a id="datasource-multiple-single-transaction"></a>
### Use multiple datasources in a single transaction

By default, XA support on datasources is disabled. Therefore, a transaction can include no more than one datasource. Attempting to access multiple non-XA datasources in the same transaction results in an exception similar to the following:

```text
...
Caused by: java.sql.SQLException: Exception in association of connection to existing transaction
        at io.agroal.narayana.NarayanaTransactionIntegration.associate(NarayanaTransactionIntegration.java:130)
        ...
Caused by: java.sql.SQLException: Failed to enlist. Check if a connection from another datasource is already enlisted to the same transaction
        at io.agroal.narayana.NarayanaTransactionIntegration.associate(NarayanaTransactionIntegration.java:121)
        ...
```

To allow using multiple JDBC datasources in the same transaction:

1. Make sure your JDBC driver supports XA. All [supported JDBC drivers do](#extensions-and-database-drivers-reference), but [other JDBC drivers](#other-databases) might not.
2. Make sure your database server is configured to enable XA.
3. Enable XA support explicitly for each relevant datasource by setting [`quarkus.datasource[.optional name].jdbc.transactions`](#quarkus-agroal_quarkus-datasource-jdbc-transactions) to `xa`.

Using XA, a rollback in one datasource will trigger a rollback in every other datasource enrolled in the transaction.

> [!NOTE]
> XA transactions on reactive datasources are not supported at the moment.

> [!NOTE]
> If your transaction involves non-datasource resources, be aware that they might not support XA transactions or might require additional configuration.

If XA cannot be enabled for one of your datasources:

- Be aware that enabling XA for all datasources *except one* (and only one) is still supported through [Last Resource Commit Optimization (LRCO)](https://www.narayana.io/docs/project/index.html#_last_resource_commit_optimization_lrco).
- If you do not need a rollback for one datasource to trigger a rollback for other datasources, consider splitting your code into multiple transactions. To do so, use [`QuarkusTransaction.requiringNew()`](/guides/transaction/index.md#programmatic-approach)/[`@Transactional(REQUIRES_NEW)`](/guides/transaction/index.md#declarative-approach) (preferably) or [`UserTransaction`](/guides/transaction/index.md#legacy-api-approach) (for more complex use cases).

> [!CAUTION]
> If no other solution works and compatibility with Quarkus 3.8 or earlier is required, set `quarkus.transaction-manager.unsafe-multiple-last-resources` to `allow` to enable unsafe transaction handling across multiple non-XA datasources.
>
> With this property set to `allow`, a transaction rollback might only apply to the last non-XA datasource, while other non-XA datasources may have already committed their changes. This can leave the system in an inconsistent state.
>
> Alternatively, allow the same unsafe behavior but with warnings when it occurs:
>
> - Setting the property to `warn-each` logs a warning for **each** offending transaction.
> - Setting the property to `warn-first` logs a warning for the **first** offending transaction.
>
> We do not recommend using this configuration property and plan to remove it in the future. You should update your application accordingly. If you believe your use case justifies keeping this option, open an issue in the [Quarkus tracker](https://github.com/quarkusio/quarkus/issues/new?assignees=&labels=kind%2Fenhancement&projects=&template=feature_request.yml) explaining why.

## Datasource integrations

<a id="datasource-health-check"></a>
### Datasource health check

If you use the [`quarkus-smallrye-health`](https://quarkus.io/extensions/io.quarkus/quarkus-smallrye-health) extension, the `quarkus-agroal` and reactive client extensions automatically add a readiness health check to validate the datasource.

When you access your application’s health readiness endpoint, `/q/health/ready` by default, you receive information about the datasource validation status. If you have multiple datasources, all datasources are checked, and if a single datasource validation failure occurs, the status changes to `DOWN`.

You can disable this behavior by setting `quarkus.datasource.health.enabled=false`.

To exclude only a particular datasource from the health check:

```properties
quarkus.datasource."datasource-name".health-exclude=true
```

### Datasource metrics

When you add the [`quarkus-micrometer`](/guides/telemetry-micrometer/index.md) extension, `quarkus-agroal` can publish datasource metrics to the metrics registry. To enable these metrics, set `quarkus.datasource.metrics.enabled=true`.

For the published metrics to report values, Agroal must enable its internal metrics collection. By default, Agroal enables internal metrics collection for all datasources when a metrics extension is present and `quarkus.datasource.metrics.enabled` is set to `true`.

To disable metrics for a specific datasource, set `quarkus.datasource.jdbc.metrics.enabled=false`. For a named datasource, set `quarkus.datasource.<datasource name>.jdbc.metrics.enabled=false`. This setting stops internal metrics collection and stops exposing datasource metrics at the `/q/metrics` endpoint.

To enable internal metrics collection explicitly for the default data source, set `quarkus.datasource.jdbc.metrics.enabled=true`. To enable internal metrics collection explicitly for a named data source, set `quarkus.datasource.<datasource name>.jdbc.metrics.enabled=true`. This enables internal metrics collection even when you do not add a metrics extension. You can access the collected metrics programmatically by calling `dataSource.getMetrics()` on an injected `AgroalDataSource` instance.

When metrics collection is disabled for a data source, all metric values are zero.

<a id="datasource-tracing"></a>
### Datasource tracing

To use tracing with a datasource, you need to add the [`quarkus-opentelemetry`](/guides/opentelemetry-tracing/index.md) extension to your project.

You do not need to declare a different driver to enable tracing. If you use a JDBC driver, you need to follow [the instructions in the OpenTelemetry extension](/guides/opentelemetry-tracing/index.md#jdbc).

Even with all the tracing infrastructure in place, the datasource tracing is not enabled by default, and you need to enable it by setting this property:

```properties
quarkus.datasource.jdbc.telemetry=true
```

By default, only SQL statement executions are traced. Connection acquisition from the datasource (`getConnection()` calls) is not traced. To also trace connection acquisition, enable it explicitly:

```properties
quarkus.datasource.jdbc.telemetry.trace-connection=true
```

For a named datasource:

```properties
quarkus.datasource."myds".jdbc.telemetry=true
quarkus.datasource."myds".jdbc.telemetry.trace-connection=true
```

### Narayana transaction manager integration

Integration is automatic if the Narayana JTA extension is also available.

You can override this by setting the `transactions` configuration property:

- `quarkus.datasource.jdbc.transactions` for default unnamed datasource
- `quarkus.datasource._<datasource-name>_.jdbc.transactions` for named datasource

When a datasource is configured for XA transactions by setting `quarkus.datasource[.optional name].jdbc.transactions=xa` and the transaction recovery system is enabled by using `quarkus.transaction-manager.enable-recovery=true`, the datasource is automatically registered for recovery. This is the preferred and safe default. You can override this behavior for individual datasources by setting `quarkus.datasource.jdbc.enable-recovery=false` or `quarkus.datasource."datasource-name".jdbc.enable-recovery=false`.

> [!IMPORTANT]
> Change this setting only in advanced use cases and only if you are certain recovery is not required. Incorrect configuration can lead to data loss, data unavailability, or both, due to resources remaining locked indefinitely.

For more information, see the [Configuration reference](#configuration-reference) section below. To facilitate the storage of transaction logs in a database by using JDBC, see the [Configuring transaction logs to be stored in a datasource](/guides/transaction/index.md#jdbcstore) section of the [Using transactions in Quarkus](/guides/transaction/index.md) guide.

<a id="in-memory-databases"></a>
### Testing with in-memory databases

The recommended approach for testing against databases is to use the same database as production to get test results that match production behavior as closely as possible. [Dev Services](/guides/databases-dev-services/index.md) simplifies this approach by requiring no configuration and starting quickly.

For scenarios where neither Dev Services nor a custom database setup is possible, you can use a JVM based database such as H2 in embedded mode.

#### Support and limitations

Embedded databases such as H2 work in JVM mode.

In native mode, do not embed H2 in the native image. Use a remote connection to a separate database, or use Dev Services to run the database outside the native image.

#### Run an integration test

1. Add a dependency on the artifacts providing the additional tools that are under the following Maven coordinates:
   - `io.quarkus:quarkus-test-h2` for H2

   You can test your application even when it is compiled into a native executable, while the database will run as a JVM process.


1. Add the following annotation to any class in your integration tests to run the tests on both the JVM and native executables:
   - `@QuarkusTestResource(H2DatabaseTestResource.class)`

   This ensures that the test suite starts and terminates the managed database in a separate process as required for test execution.

   .H2 example

   ```java
   package my.app.integrationtests.db;

   import io.quarkus.test.common.QuarkusTestResource;
   import io.quarkus.test.h2.H2DatabaseTestResource;

   @QuarkusTestResource(H2DatabaseTestResource.class)
   public class TestResources {
   }
   ```


1. Configure the connection to the managed database:

   ```properties
   quarkus.datasource.db-kind=h2
   quarkus.datasource.jdbc.url=jdbc:h2:tcp://localhost/mem:test
   ```


<a id="datasource-reference"></a>
## References

<a id="configuration-reference"></a>
### Common datasource configuration reference

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

<a id="jdbc-configuration"></a>
### JDBC configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-agroal_quarkus-datasource-dev-ui-enabled"></a> [`quarkus.datasource.dev-ui.enabled`](#quarkus-agroal_quarkus-datasource-dev-ui-enabled) config_property_copy_button:+++quarkus.datasource.dev-ui.enabled+++[]<br><br>Activate or disable the dev ui page.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEV_UI_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-agroal_quarkus-datasource-dev-ui-allow-sql"></a> [`quarkus.datasource.dev-ui.allow-sql`](#quarkus-agroal_quarkus-datasource-dev-ui-allow-sql) config_property_copy_button:+++quarkus.datasource.dev-ui.allow-sql+++[]<br><br>Allow sql queries in the Dev UI page<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEV_UI_ALLOW_SQL+++[] | boolean | `false` |
| [lock] <a id="quarkus-agroal_quarkus-datasource-dev-ui-append-to-default-select"></a> [`quarkus.datasource.dev-ui.append-to-default-select`](#quarkus-agroal_quarkus-datasource-dev-ui-append-to-default-select) config_property_copy_button:+++quarkus.datasource.dev-ui.append-to-default-select+++[]<br><br>Append this to the select done to fetch the table values. eg: LIMIT 100 or TOP 100<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEV_UI_APPEND_TO_DEFAULT_SELECT+++[] | string |  |
| [lock] <a id="quarkus-agroal_quarkus-datasource-dev-ui-allowed-db-host"></a> [`quarkus.datasource.dev-ui.allowed-db-host`](#quarkus-agroal_quarkus-datasource-dev-ui-allowed-db-host) config_property_copy_button:+++quarkus.datasource.dev-ui.allowed-db-host+++[]<br><br>Allowed database host. By default, only localhost is allowed. Any provided host here will also be allowed. You can use the special value `*` to allow any DB host.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEV_UI_ALLOWED_DB_HOST+++[] | string |  |
| [lock] <a id="quarkus-agroal_quarkus-datasource-jdbc"></a> [`quarkus.datasource.jdbc`](#quarkus-agroal_quarkus-datasource-jdbc) config_property_copy_button:+++quarkus.datasource.jdbc+++[]<br><br>`quarkus.datasource."datasource-name".jdbc` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc+++[]<br><br>If we create a JDBC datasource for this datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC+++[] | boolean | `true` |
| [lock] <a id="quarkus-agroal_quarkus-datasource-jdbc-driver"></a> [`quarkus.datasource.jdbc.driver`](#quarkus-agroal_quarkus-datasource-jdbc-driver) config_property_copy_button:+++quarkus.datasource.jdbc.driver+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.driver` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.driver+++[]<br><br>The datasource driver class name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_DRIVER+++[] | string |  |
| [lock] <a id="quarkus-agroal_quarkus-datasource-jdbc-transactions"></a> [`quarkus.datasource.jdbc.transactions`](#quarkus-agroal_quarkus-datasource-jdbc-transactions) config_property_copy_button:+++quarkus.datasource.jdbc.transactions+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.transactions` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.transactions+++[]<br><br>Whether we want to use regular JDBC transactions, XA, or disable all transactional capabilities.<br><br>When enabling XA you will need a driver implementing `javax.sql.XADataSource`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_TRANSACTIONS+++[] | tooltip:enabled[Integrate the JDBC Datasource with the JTA TransactionManager of Quarkus. This is the default.], tooltip:xa[Similarly to `enabled`,opts=also enables integration with the JTA TransactionManager of Quarkus,but enabling XA transactions as well. Requires a JDBC driver implementing `javax.sql.XADataSource`], tooltip:disabled[Disables the Agroal integration with the Narayana TransactionManager. This is typically a bad idea,opts=and is only useful in special cases\: make sure to not use this without having a deep understanding of the implications.] | tooltip:enabled[Integrate the JDBC Datasource with the JTA TransactionManager of Quarkus. This is the default.] |
| [lock] <a id="quarkus-agroal_quarkus-datasource-jdbc-telemetry"></a> [`quarkus.datasource.jdbc.telemetry`](#quarkus-agroal_quarkus-datasource-jdbc-telemetry) config_property_copy_button:+++quarkus.datasource.jdbc.telemetry+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.telemetry` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.telemetry+++[]<br><br>Enable OpenTelemetry JDBC instrumentation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_TELEMETRY+++[] | boolean | `false` |
| [lock] <a id="quarkus-agroal_quarkus-datasource-jdbc-metrics-enabled"></a> [`quarkus.datasource.jdbc.metrics.enabled`](#quarkus-agroal_quarkus-datasource-jdbc-metrics-enabled) config_property_copy_button:+++quarkus.datasource.jdbc.metrics.enabled+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.metrics.enabled` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.metrics.enabled+++[]<br><br>Enable metrics collection for this datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_METRICS_ENABLED+++[] | boolean |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-url"></a> [`quarkus.datasource.jdbc.url`](#quarkus-agroal_quarkus-datasource-jdbc-url) config_property_copy_button:+++quarkus.datasource.jdbc.url+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.url` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.url+++[]<br><br>The datasource URL<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_URL+++[] | string |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-initial-size"></a> [`quarkus.datasource.jdbc.initial-size`](#quarkus-agroal_quarkus-datasource-jdbc-initial-size) config_property_copy_button:+++quarkus.datasource.jdbc.initial-size+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.initial-size` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.initial-size+++[]<br><br>The initial size of the pool. Usually you will want to set the initial size to match at least the minimal size, but this is not enforced so to allow for architectures which prefer a lazy initialization of the connections on boot, while being able to sustain a minimal pool size after boot.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_INITIAL_SIZE+++[] | int |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-min-size"></a> [`quarkus.datasource.jdbc.min-size`](#quarkus-agroal_quarkus-datasource-jdbc-min-size) config_property_copy_button:+++quarkus.datasource.jdbc.min-size+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.min-size` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.min-size+++[]<br><br>The datasource pool minimum size<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_MIN_SIZE+++[] | int | `0` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-max-size"></a> [`quarkus.datasource.jdbc.max-size`](#quarkus-agroal_quarkus-datasource-jdbc-max-size) config_property_copy_button:+++quarkus.datasource.jdbc.max-size+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.max-size` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.max-size+++[]<br><br>The datasource pool maximum size<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_MAX_SIZE+++[] | int | `50` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-background-validation-interval"></a> [`quarkus.datasource.jdbc.background-validation-interval`](#quarkus-agroal_quarkus-datasource-jdbc-background-validation-interval) config_property_copy_button:+++quarkus.datasource.jdbc.background-validation-interval+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.background-validation-interval` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.background-validation-interval+++[]<br><br>The interval at which we validate idle connections in the background.<br><br>Set to `0` to disable background validation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_BACKGROUND_VALIDATION_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) | `2M` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-foreground-validation-interval"></a> [`quarkus.datasource.jdbc.foreground-validation-interval`](#quarkus-agroal_quarkus-datasource-jdbc-foreground-validation-interval) config_property_copy_button:+++quarkus.datasource.jdbc.foreground-validation-interval+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.foreground-validation-interval` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.foreground-validation-interval+++[]<br><br>Perform foreground validation on connections that have been idle for longer than the specified interval.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_FOREGROUND_VALIDATION_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-login-timeout"></a> [`quarkus.datasource.jdbc.login-timeout`](#quarkus-agroal_quarkus-datasource-jdbc-login-timeout) config_property_copy_button:+++quarkus.datasource.jdbc.login-timeout+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.login-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.login-timeout+++[]<br><br>Maximum time to wait while attempting to connect to a database. The actual implementation of login timeout could be different in various drivers. (e.g. driver could only cover the authentication and not the entire connection creation phase)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_LOGIN_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) | `By default, there is no login timeout set.` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-acquisition-timeout"></a> [`quarkus.datasource.jdbc.acquisition-timeout`](#quarkus-agroal_quarkus-datasource-jdbc-acquisition-timeout) config_property_copy_button:+++quarkus.datasource.jdbc.acquisition-timeout+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.acquisition-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.acquisition-timeout+++[]<br><br>The timeout before cancelling the acquisition of a new connection<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_ACQUISITION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) | `5S` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-leak-detection-interval"></a> [`quarkus.datasource.jdbc.leak-detection-interval`](#quarkus-agroal_quarkus-datasource-jdbc-leak-detection-interval) config_property_copy_button:+++quarkus.datasource.jdbc.leak-detection-interval+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.leak-detection-interval` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.leak-detection-interval+++[]<br><br>The interval at which we check for connection leaks.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_LEAK_DETECTION_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) | `This feature is disabled by default.` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-idle-removal-interval"></a> [`quarkus.datasource.jdbc.idle-removal-interval`](#quarkus-agroal_quarkus-datasource-jdbc-idle-removal-interval) config_property_copy_button:+++quarkus.datasource.jdbc.idle-removal-interval+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.idle-removal-interval` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.idle-removal-interval+++[]<br><br>The interval at which we try to remove idle connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_IDLE_REMOVAL_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) | `5M` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-max-lifetime"></a> [`quarkus.datasource.jdbc.max-lifetime`](#quarkus-agroal_quarkus-datasource-jdbc-max-lifetime) config_property_copy_button:+++quarkus.datasource.jdbc.max-lifetime+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.max-lifetime` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.max-lifetime+++[]<br><br>The max lifetime of a connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_MAX_LIFETIME+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) | `By default, there is no restriction on the lifespan of a connection.` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-transaction-isolation-level"></a> [`quarkus.datasource.jdbc.transaction-isolation-level`](#quarkus-agroal_quarkus-datasource-jdbc-transaction-isolation-level) config_property_copy_button:+++quarkus.datasource.jdbc.transaction-isolation-level+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.transaction-isolation-level` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.transaction-isolation-level+++[]<br><br>The transaction isolation level.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_TRANSACTION_ISOLATION_LEVEL+++[] | `undefined`, `none`, `read-uncommitted`, `read-committed`, `repeatable-read`, `serializable` |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-read-only"></a> [`quarkus.datasource.jdbc.read-only`](#quarkus-agroal_quarkus-datasource-jdbc-read-only) config_property_copy_button:+++quarkus.datasource.jdbc.read-only+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.read-only` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.read-only+++[]<br><br>Whether connections from this pool should be created in read-only mode, according to `java.sql.Connection#setReadOnly(boolean)`. The JDBC driver is responsible for enforcing read-only mode.<br><br>This is useful for secondary pools that target read replicas.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_READ_ONLY+++[] | boolean | `By default, the read-only mode of connections is not configured.` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-extended-leak-report"></a> [`quarkus.datasource.jdbc.extended-leak-report`](#quarkus-agroal_quarkus-datasource-jdbc-extended-leak-report) config_property_copy_button:+++quarkus.datasource.jdbc.extended-leak-report+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.extended-leak-report` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.extended-leak-report+++[]<br><br>Collect and display extra troubleshooting info on leaked connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_EXTENDED_LEAK_REPORT+++[] | boolean | `false` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-flush-on-close"></a> [`quarkus.datasource.jdbc.flush-on-close`](#quarkus-agroal_quarkus-datasource-jdbc-flush-on-close) config_property_copy_button:+++quarkus.datasource.jdbc.flush-on-close+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.flush-on-close` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.flush-on-close+++[]<br><br>Allows connections to be flushed upon return to the pool. It's not enabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_FLUSH_ON_CLOSE+++[] | boolean | `false` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-detect-statement-leaks"></a> [`quarkus.datasource.jdbc.detect-statement-leaks`](#quarkus-agroal_quarkus-datasource-jdbc-detect-statement-leaks) config_property_copy_button:+++quarkus.datasource.jdbc.detect-statement-leaks+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.detect-statement-leaks` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.detect-statement-leaks+++[]<br><br>When enabled, Agroal will be able to produce a warning when a connection is returned to the pool without the application having closed all open statements. This is unrelated with tracking of open connections. Disable for peak performance, but only when there's high confidence that no leaks are happening.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_DETECT_STATEMENT_LEAKS+++[] | boolean | `true` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-new-connection-sql"></a> [`quarkus.datasource.jdbc.new-connection-sql`](#quarkus-agroal_quarkus-datasource-jdbc-new-connection-sql) config_property_copy_button:+++quarkus.datasource.jdbc.new-connection-sql+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.new-connection-sql` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.new-connection-sql+++[]<br><br>Query executed when first using a connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_NEW_CONNECTION_SQL+++[] | string |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-validation-query-sql"></a> [`quarkus.datasource.jdbc.validation-query-sql`](#quarkus-agroal_quarkus-datasource-jdbc-validation-query-sql) config_property_copy_button:+++quarkus.datasource.jdbc.validation-query-sql+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.validation-query-sql` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.validation-query-sql+++[]<br><br>Query executed to validate a connection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_VALIDATION_QUERY_SQL+++[] | string |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-validation-query-timeout"></a> [`quarkus.datasource.jdbc.validation-query-timeout`](#quarkus-agroal_quarkus-datasource-jdbc-validation-query-timeout) config_property_copy_button:+++quarkus.datasource.jdbc.validation-query-timeout+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.validation-query-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.validation-query-timeout+++[]<br><br>The timeout for the connection validation query<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_VALIDATION_QUERY_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-validate-on-borrow"></a> [`quarkus.datasource.jdbc.validate-on-borrow`](#quarkus-agroal_quarkus-datasource-jdbc-validate-on-borrow) config_property_copy_button:+++quarkus.datasource.jdbc.validate-on-borrow+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.validate-on-borrow` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.validate-on-borrow+++[]<br><br>Forces connection validation prior to acquisition (foreground validation) regardless of the idle status.<br><br>Because of the overhead of performing validation on every call, it’s recommended to rely on default idle validation instead, and to leave this to `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_VALIDATE_ON_BORROW+++[] | boolean | `false` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-pooling-enabled"></a> [`quarkus.datasource.jdbc.pooling-enabled`](#quarkus-agroal_quarkus-datasource-jdbc-pooling-enabled) config_property_copy_button:+++quarkus.datasource.jdbc.pooling-enabled+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.pooling-enabled` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.pooling-enabled+++[]<br><br>Disable pooling to prevent reuse of Connections. Use this when an external pool manages the life-cycle of Connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_POOLING_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-enable-recovery"></a> [`quarkus.datasource.jdbc.enable-recovery`](#quarkus-agroal_quarkus-datasource-jdbc-enable-recovery) config_property_copy_button:+++quarkus.datasource.jdbc.enable-recovery+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.enable-recovery` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.enable-recovery+++[]<br><br>Whether to enable recovery for this datasource.<br><br>Normally a transaction manager will call xa_recover () on an XA connection during recovery to obtain a list of transaction branches that are currently in a prepared or heuristically completed state. However, it can happen that multiple XA connections connect to the same datasource which would all return the same set of branches and for reasons of improved performance only one should be used for recover() calls. The default value for this configuration property is true because when there is only one connection it is vital for data consistency that the connection is able to report its list of prepared or heuristically completed branches.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_ENABLE_RECOVERY+++[] | boolean | `true` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-transaction-requirement"></a> [`quarkus.datasource.jdbc.transaction-requirement`](#quarkus-agroal_quarkus-datasource-jdbc-transaction-requirement) config_property_copy_button:+++quarkus.datasource.jdbc.transaction-requirement+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.transaction-requirement` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.transaction-requirement+++[]<br><br>Require an active transaction when acquiring a connection. Recommended for production. WARNING: Some extensions acquire connections without holding a transaction for things like schema updates and schema validation. Setting this setting to STRICT may lead to failures in those cases.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_TRANSACTION_REQUIREMENT+++[] | `off`, `warn`, `strict` |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-multiple-acquisition"></a> [`quarkus.datasource.jdbc.multiple-acquisition`](#quarkus-agroal_quarkus-datasource-jdbc-multiple-acquisition) config_property_copy_button:+++quarkus.datasource.jdbc.multiple-acquisition+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.multiple-acquisition` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.multiple-acquisition+++[]<br><br>Defines the behavior when a thread tries to acquire multiple connections from the pool.<br><br>`lenient` (the default) means no restriction. `warn` will log a warning when a thread already holds a connection and tries to acquire another one. `strict` will throw an exception in that situation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_MULTIPLE_ACQUISITION+++[] | `lenient`, `off`, `warn`, `strict` | `By default, multiple acquisition is allowed without any restriction.` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-additional-jdbc-properties-property-key"></a> [`quarkus.datasource.jdbc.additional-jdbc-properties."property-key"`](#quarkus-agroal_quarkus-datasource-jdbc-additional-jdbc-properties-property-key) config_property_copy_button:+++quarkus.datasource.jdbc.additional-jdbc-properties."property-key"+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.additional-jdbc-properties."property-key"` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.additional-jdbc-properties."property-key"+++[]<br><br>Other unspecified properties to be passed to the JDBC driver when creating new connections.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_ADDITIONAL_JDBC_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-telemetry-enabled"></a> [`quarkus.datasource.jdbc.telemetry.enabled`](#quarkus-agroal_quarkus-datasource-jdbc-telemetry-enabled) config_property_copy_button:+++quarkus.datasource.jdbc.telemetry.enabled+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.telemetry.enabled` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.telemetry.enabled+++[]<br><br>Enable OpenTelemetry JDBC instrumentation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_TELEMETRY_ENABLED+++[] | boolean | `false if quarkus.datasource.jdbc.telemetry=false and true if quarkus.datasource.jdbc.telemetry=true` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-telemetry-trace-connection"></a> [`quarkus.datasource.jdbc.telemetry.trace-connection`](#quarkus-agroal_quarkus-datasource-jdbc-telemetry-trace-connection) config_property_copy_button:+++quarkus.datasource.jdbc.telemetry.trace-connection+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.telemetry.trace-connection` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.telemetry.trace-connection+++[]<br><br>Enable tracing of the connection acquisition from the datasource. When enabled, a span is created for each `getConnection()` call.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_TELEMETRY_TRACE_CONNECTION+++[] | boolean | `false` |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-enable-keep-alive"></a> [`quarkus.datasource.jdbc.enable-keep-alive`](#quarkus-agroal_quarkus-datasource-jdbc-enable-keep-alive) config_property_copy_button:+++quarkus.datasource.jdbc.enable-keep-alive+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.enable-keep-alive` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.enable-keep-alive+++[]<br><br>Enable KeepAlive for this datasource. When enabled, the datasource will attempt to keep connections alive by sending periodic keep-alive messages to the database. Each JDBC driver has its own implementation of keep-alive, and the actual behavior may vary depending on the driver and database being used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_ENABLE_KEEP_ALIVE+++[] | boolean |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-read-timeout"></a> [`quarkus.datasource.jdbc.read-timeout`](#quarkus-agroal_quarkus-datasource-jdbc-read-timeout) config_property_copy_button:+++quarkus.datasource.jdbc.read-timeout+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.read-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.read-timeout+++[]<br><br>The timeout value used for socket read operations. If reading from the server takes longer than this value, the connection is closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) |  |
| <a id="quarkus-agroal_quarkus-datasource-jdbc-network-timeout"></a> [`quarkus.datasource.jdbc.network-timeout`](#quarkus-agroal_quarkus-datasource-jdbc-network-timeout) config_property_copy_button:+++quarkus.datasource.jdbc.network-timeout+++[]<br><br>`quarkus.datasource."datasource-name".jdbc.network-timeout` config_property_copy_button:+++quarkus.datasource."datasource-name".jdbc.network-timeout+++[]<br><br>Sets the maximum duration a connection will wait for the database to reply to any one request, using the standard JDBC 4.1 `java.sql.Connection#setNetworkTimeout` API.<br><br>Unlike `readTimeout`, which operates at the socket/driver level for individual read operations, this timeout is managed by the JDBC `java.sql.Connection` object and applies to any pending database request on the connection, making it the more portable option.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_JDBC_NETWORK_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-agroal_quarkus-datasource) |  |

<a id="duration-note-anchor-quarkus-agroal_quarkus-datasource"></a>

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

<a id="jdbc-url"></a>
### JDBC URL reference

Each supported database has its own JDBC URL configuration options. The following sections describe each JDBC URL and provide a link to the official documentation.

#### DB2

`jdbc:db2://<serverName>[:<portNumber>]/<databaseName>[:<key1>=<value>;[<key2>=<value2>;]]`

**Example**\
`jdbc:db2://localhost:50000/MYDB:user=dbadm;password=dbadm;`

For more information on URL syntax and additional supported options, see the [official documentation](https://www.ibm.com/docs/en/db2/12.1.x?topic=cdsudidsdjs-url-format-data-server-driver-jdbc-sqlj-type-4-connectivity).

#### H2

`jdbc:h2:{ {.|mem:}[name] | [file:]fileName | {tcp|ssl}:[//]server[:port][,server2[:port]]/name }[;key=value...]`

**Example**\
`jdbc:h2:tcp://localhost/~/test`, `jdbc:h2:mem:myDB`

H2 is a database that can run in embedded or server mode. It can use a file storage or run entirely in memory. All of these options are available as listed above.

For more information, see the [official documentation](https://h2database.com/html/features.html#database_url).

#### MariaDB

`jdbc:mariadb:[replication:|failover:|sequential:|aurora:]//<hostDescription>[,<hostDescription>...]/[database][?<key1>=<value1>[&<key2>=<value2>]]`

**hostDescription**\
`<host>[:<portnumber>] or address=(host=<host>)[(port=<portnumber>)][(type=(master|slave))]`

**Example**\
`jdbc:mariadb://localhost:3306/test`

For more information, see the [official documentation](https://mariadb.com/docs/connectors/mariadb-connector-j/).

#### Microsoft SQL server

`jdbc:sqlserver://[serverName[\instanceName][:portNumber]][;property=value[;property=value]]`

**Example**\
`jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks`

For more information, see the [official documentation](https://learn.microsoft.com/en-us/sql/connect/jdbc/connecting-to-sql-server-with-the-jdbc-driver?view=sql-server-ver16).

#### MySQL

`jdbc:mysql:[replication:|failover:|sequential:|aurora:]//<hostDescription>[,<hostDescription>...]/[database][?<key1>=<value1>[&<key2>=<value2>]]`

**hostDescription**\
`<host>[:<portnumber>] or address=(host=<host>)[(port=<portnumber>)][(type=(master|slave))]`

**Example**\
`jdbc:mysql://localhost:3306/test`

For more information, see the [official documentation](https://dev.mysql.com/doc/connector-j/en/).

##### MySQL limitations

When you compile an application into a native image, the following limitations apply to MySQL Connector/J integrations:

- Quarkus disables Java Management Extensions (JMX) support because it does not work in native mode.
- Quarkus disables Oracle Cloud Infrastructure (OCI) integration because it does not work in native mode.
- Quarkus disables the MySQL Connector/J OpenTelemetry integration and relies on the Agroal integration instead.

  If you need the MySQL Connector/J OpenTelemetry integration, enable it explicitly by setting `quarkus.datasource.jdbc.additional-jdbc-properties.openTelemetry=PREFERRED`. For more information, see [MySQL Connector/J documentation](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-debugging-profiling.html#cj-conn-prop_openTelemetry).

  When you enable this option and also add the `quarkus-opentelemetry` extension, the application can fail to start and throw a `java.lang.IllegalStateException: GlobalOpenTelemetry.set has already been called.` error.


#### Oracle

`jdbc:oracle:driver_type:@database_specifier`

**Example**\
`jdbc:oracle:thin:@localhost:1521/ORCL_SVC`

For more information, see the [official documentation](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/data-sources-and-URLs.html#GUID-AEA8E228-1B21-4111-AF4C-B1F33744CA08).

#### PostgreSQL

`jdbc:postgresql:[//][host][:port][/database][?key=value...]`

**Example**\
`jdbc:postgresql://localhost/test`

The defaults for the different parts are as follows:

**`host`**\
localhost

**`port`**\
5432

**`database`**\
same name as the username

For more information about additional parameters, see the [official documentation](https://jdbc.postgresql.org/documentation/head/connect.html).

<a id="extensions-and-database-drivers-reference"></a>
### Quarkus extensions and database drivers reference

The following tables list the built-in `db-kind` values, the corresponding Quarkus extensions, and the JDBC drivers used by those extensions.

When using one of the built-in datasource kinds, the JDBC and Reactive drivers are resolved automatically to match the values from these tables.

**Database platform kind to JDBC driver mapping**

| Database kind | Quarkus extension | Drivers |
| --- | --- | --- |
| `db2` | `quarkus-jdbc-db2` | - JDBC: `com.ibm.db2.jcc.DB2Driver`<br><br>- XA: `com.ibm.db2.jcc.DB2XADataSource` |
| `h2` | `quarkus-jdbc-h2` | - JDBC: `org.h2.Driver`<br><br>- XA: `org.h2.jdbcx.JdbcDataSource` |
| `mariadb` | `quarkus-jdbc-mariadb` | - JDBC: `org.mariadb.jdbc.Driver`<br><br>- XA: `org.mariadb.jdbc.MariaDbDataSource` |
| `mssql` | `quarkus-jdbc-mssql` | - JDBC: `com.microsoft.sqlserver.jdbc.SQLServerDriver`<br><br>- XA: `com.microsoft.sqlserver.jdbc.SQLServerXADataSource` |
| `mysql` | `quarkus-jdbc-mysql` | - JDBC: `com.mysql.cj.jdbc.Driver`<br><br>- XA: `com.mysql.cj.jdbc.MysqlXADataSource` |
| `oracle` | `quarkus-jdbc-oracle` | - JDBC: `oracle.jdbc.driver.OracleDriver`<br><br>- XA: `oracle.jdbc.xa.client.OracleXADataSource` |
| `postgresql` | `quarkus-jdbc-postgresql` | - JDBC: `org.postgresql.Driver`<br><br>- XA: `org.postgresql.xa.PGXADataSource` |

**Database kind to Reactive driver mapping**

| Database kind | Quarkus extension | Driver |
| --- | --- | --- |
| `oracle` | `reactive-oracle-client` | `io.vertx.oracleclient.spi.OracleDriver` |
| `mysql` | `reactive-mysql-client` | `io.vertx.mysqlclient.spi.MySQLDriver` |
| `mssql` | `reactive-mssql-client` | `io.vertx.mssqlclient.spi.MSSQLDriver` |
| `postgresql` | `reactive-pg-client` | `io.vertx.pgclient.spi.PgDriver` |
| `db2` | `reactive-db2-client` | `io.vertx.db2client.spi.DB2Driver` |

> [!TIP]
> This automatic resolution is applicable in most cases so that driver configuration is not needed.

<a id="reactive-configuration"></a>
### Reactive datasource configuration reference

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

#### Reactive DB2 configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-db2-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-db2-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-db2-client_quarkus-datasource-reactive-db2-ssl"></a> [`quarkus.datasource.reactive.db2.ssl`](#quarkus-reactive-db2-client_quarkus-datasource-reactive-db2-ssl) config_property_copy_button:+++quarkus.datasource.reactive.db2.ssl+++[]<br><br>`quarkus.datasource."datasource-name".reactive.db2.ssl` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.db2.ssl+++[]<br><br>Whether SSL/TLS is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_DB2_SSL+++[] | boolean | `false` |

#### Reactive MariaDB/MySQL specific configuration

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

#### Reactive Microsoft SQL server-specific configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-mssql-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-mssql-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-packet-size"></a> [`quarkus.datasource.reactive.mssql.packet-size`](#quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-packet-size) config_property_copy_button:+++quarkus.datasource.reactive.mssql.packet-size+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mssql.packet-size` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mssql.packet-size+++[]<br><br>The desired size (in bytes) for TDS packets.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MSSQL_PACKET_SIZE+++[] | int |  |
| <a id="quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-ssl"></a> [`quarkus.datasource.reactive.mssql.ssl`](#quarkus-reactive-mssql-client_quarkus-datasource-reactive-mssql-ssl) config_property_copy_button:+++quarkus.datasource.reactive.mssql.ssl+++[]<br><br>`quarkus.datasource."datasource-name".reactive.mssql.ssl` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.mssql.ssl+++[]<br><br>Whether SSL/TLS is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_MSSQL_SSL+++[] | boolean | `false` |

#### Reactive Oracle-specific configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-oracle-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-oracle-client_section_quarkus-datasource) | Type | Default |

#### Reactive PostgreSQL-specific configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-reactive-pg-client_section_quarkus-datasource"></a> [Datasources](#quarkus-reactive-pg-client_section_quarkus-datasource) | Type | Default |
| <a id="quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-pipelining-limit"></a> [`quarkus.datasource.reactive.postgresql.pipelining-limit`](#quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-pipelining-limit) config_property_copy_button:+++quarkus.datasource.reactive.postgresql.pipelining-limit+++[]<br><br>`quarkus.datasource."datasource-name".reactive.postgresql.pipelining-limit` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.postgresql.pipelining-limit+++[]<br><br>The maximum number of inflight database commands that can be pipelined.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_POSTGRESQL_PIPELINING_LIMIT+++[] | int |  |
| <a id="quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-ssl-mode"></a> [`quarkus.datasource.reactive.postgresql.ssl-mode`](#quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-ssl-mode) config_property_copy_button:+++quarkus.datasource.reactive.postgresql.ssl-mode+++[]<br><br>`quarkus.datasource."datasource-name".reactive.postgresql.ssl-mode` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.postgresql.ssl-mode+++[]<br><br>SSL operating mode of the client.<br><br>See [Protection Provided in Different Modes](https://www.postgresql.org/docs/current/libpq-ssl.html#LIBPQ-SSL-PROTECTION).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_POSTGRESQL_SSL_MODE+++[] | `disable`, `allow`, `prefer`, `require`, `verify-ca`, `verify-full` | `disable` |
| <a id="quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-use-layer7-proxy"></a> [`quarkus.datasource.reactive.postgresql.use-layer7-proxy`](#quarkus-reactive-pg-client_quarkus-datasource-reactive-postgresql-use-layer7-proxy) config_property_copy_button:+++quarkus.datasource.reactive.postgresql.use-layer7-proxy+++[]<br><br>`quarkus.datasource."datasource-name".reactive.postgresql.use-layer7-proxy` config_property_copy_button:+++quarkus.datasource."datasource-name".reactive.postgresql.use-layer7-proxy+++[]<br><br>Level 7 proxies can load balance queries on several connections to the actual database. When it happens, the client can be confused by the lack of session affinity and unwanted errors can happen like ERROR: unnamed prepared statement does not exist (26000). See [Using a level 7 proxy](https://vertx.io/docs/vertx-pg-client/java/#_using_a_level_7_proxy)<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_REACTIVE_POSTGRESQL_USE_LAYER7_PROXY+++[] | boolean | `false` |

<a id="reactive-url"></a>
### Reactive datasource URL reference

#### DB2

`db2://[user[:[password]]@]host[:port][/database][?<key1>=<value1>[&<key2>=<value2>]]`

**Example**\
`db2://dbuser:secretpassword@database.server.com:50000/mydb`

Currently, the client supports the following parameter keys:

- `host`
- `port`
- `user`
- `password`
- `database`

> [!NOTE]
> Configuring parameters in the connection URL overrides the default properties.

#### Microsoft SQL server

`sqlserver://[user[:[password]]@]host[:port][/database][?<key1>=<value1>[&<key2>=<value2>]]`

**Example**\
`sqlserver://dbuser:secretpassword@database.server.com:1433/mydb`

Currently, the client supports the following parameter keys:

- `host`
- `port`
- `user`
- `password`
- `database`

> [!NOTE]
> Configuring parameters in the connection URL overrides the default properties.

#### MySQL / MariaDB

`mysql://[user[:[password]]@]host[:port][/database][?<key1>=<value1>[&<key2>=<value2>]]`

**Example**\
`mysql://dbuser:secretpassword@database.server.com:3211/mydb`

Currently, the client supports the following parameter keys (case-insensitive):

- `host`
- `port`
- `user`
- `password`
- `schema`
- `socket`
- `useAffectedRows`

> [!NOTE]
> Configuring parameters in the connection URL overrides the default properties.

#### Oracle

##### EZConnect format

`oracle:thin:@[[protocol:]//]host[:port][/service_name][:server_mode][/instance_name][?connection properties]`

**Example**\
`oracle:thin:@mydbhost1:5521/mydbservice?connect_timeout=10sec`

##### TNS alias format

`oracle:thin:@<alias_name>[?connection properties]`

**Example**\
`oracle:thin:@prod_db?TNS_ADMIN=/work/tns/`

#### PostgreSQL

`postgresql://[user[:[password]]@]host[:port][/database][?<key1>=<value1>[&<key2>=<value2>]]`

**Example**\
`postgresql://dbuser:secretpassword@database.server.com:5432/mydb`

Currently, the client supports:

- Following parameter keys:
  - `host`
  - `port`
  - `user`
  - `password`
  - `dbname`
  - `sslmode`

- Additional properties, such as:
  - `application_name`
  - `fallback_application_name`
  - `search_path`
  - `options`

> [!NOTE]
> Configuring parameters in the connection URL overrides the default properties.
