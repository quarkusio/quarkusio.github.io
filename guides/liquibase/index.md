# Using Liquibase

[Liquibase](https://www.liquibase.org/) is an open source tool for database schema change management.

Quarkus provides first class support for using Liquibase as will be explained in this guide.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `liquibase-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/liquibase-quickstart).

## Setting up support for Liquibase

To start using Liquibase with your project, you just need to:

- add your changeLog to the `src/main/resources/db/changeLog.xml` file as you usually do with Liquibase
- activate the `migrate-at-start` option to migrate the schema automatically or inject the `Liquibase` object and run your migration as you normally do.

In your `pom.xml`, add the following dependencies:

- the Liquibase extension
- your JDBC driver extension (`quarkus-jdbc-postgresql`, `quarkus-jdbc-h2`, `quarkus-jdbc-mariadb`, ...)

**pom.xml**

```xml
<!-- Liquibase specific dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-liquibase</artifactId>
</dependency>

<!-- JDBC driver dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
```

**build.gradle**

```gradle
// Liquibase specific dependencies
implementation("io.quarkus:quarkus-liquibase")

// JDBC driver dependencies
implementation("io.quarkus:quarkus-jdbc-postgresql")
```

Liquibase support relies on the Quarkus datasource config. It can be customized for the default datasource as well as for every [named datasource](#multiple-datasources). First, you need to add the datasource config to the `application.properties` file in order to allow Liquibase to manage the schema.

The following is an example for the `application.properties` file:

```properties
# configure your datasource
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=sarah
quarkus.datasource.password=connor
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydatabase

# Liquibase minimal config properties
quarkus.liquibase.migrate-at-start=true

# Liquibase optional config properties
# quarkus.liquibase.change-log=db/changeLog.xml
# quarkus.liquibase.validate-on-migrate=true
# quarkus.liquibase.clean-at-start=false
# quarkus.liquibase.database-change-log-lock-table-name=DATABASECHANGELOGLOCK
# quarkus.liquibase.database-change-log-table-name=DATABASECHANGELOG
# quarkus.liquibase.contexts=Context1,Context2
# quarkus.liquibase.labels=Label1,Label2
# quarkus.liquibase.default-catalog-name=DefaultCatalog
# quarkus.liquibase.default-schema-name=DefaultSchema
# quarkus.liquibase.liquibase-catalog-name=liquibaseCatalog
# quarkus.liquibase.liquibase-schema-name=liquibaseSchema
# quarkus.liquibase.liquibase-tablespace-name=liquibaseSpace
```

Add a changeLog file to the default folder following the Liquibase naming conventions: `src/main/resources/db/changeLog.xml` The yaml, json, xml and sql changeLog file formats are also supported.

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog-ext
    https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd
    http://www.liquibase.org/xml/ns/dbchangelog
    https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet author="quarkus" id="1">
        <createTable tableName="quarkus">
            <column name="ID" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="NAME" type="VARCHAR(255)"/>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

Now you can start your application and Quarkus will run the Liquibase's update method according to your config:

```java
import io.quarkus.liquibase.LiquibaseFactory; <1>

@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    LiquibaseFactory liquibaseFactory; <2>

    public void checkMigration() {
        // Get the list of liquibase change set statuses
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
        }
    }
}
```

1. The Quarkus extension provides a factory to initialize a Liquibase instance
2. Inject the Quarkus liquibase factory if you want to use the liquibase methods directly

<a id="multiple-datasources"></a>
## Multiple datasources

Liquibase can be configured for multiple datasources. The Liquibase properties are prefixed exactly the same way as the named datasources, for example:

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=username-default
quarkus.datasource.jdbc.url=jdbc:h2:tcp://localhost/mem:default
quarkus.datasource.jdbc.max-size=13

quarkus.datasource.users.db-kind=h2
quarkus.datasource.users.username=username1
quarkus.datasource.users.jdbc.url=jdbc:h2:tcp://localhost/mem:users
quarkus.datasource.users.jdbc.max-size=11

quarkus.datasource.inventory.db-kind=h2
quarkus.datasource.inventory.username=username2
quarkus.datasource.inventory.jdbc.url=jdbc:h2:tcp://localhost/mem:inventory
quarkus.datasource.inventory.jdbc.max-size=12

# Liquibase configuration for the default datasource
quarkus.liquibase.schemas=DEFAULT_TEST_SCHEMA
quarkus.liquibase.change-log=db/changeLog.xml
quarkus.liquibase.migrate-at-start=true

# Liquibase configuration for the "users" datasource
quarkus.liquibase.users.schemas=USERS_TEST_SCHEMA
quarkus.liquibase.users.change-log=db/users.xml
quarkus.liquibase.users.migrate-at-start=true

# Liquibase configuration for the "inventory" datasource
quarkus.liquibase.inventory.schemas=INVENTORY_TEST_SCHEMA
quarkus.liquibase.inventory.change-log=db/inventory.xml
quarkus.liquibase.inventory.migrate-at-start=true
```

Notice there's an extra bit in the key. The syntax is as follows: `quarkus.liquibase.[optional name.][datasource property]`.

> [!NOTE]
> Without configuration, Liquibase is set up for every datasource using the default settings.

## Using the Liquibase object

In case you are interested in using the `Liquibase` object directly, you can inject it as follows:

> [!NOTE]
> If you enabled the `quarkus.liquibase.migrate-at-start` property, by the time you use the Liquibase instance, Quarkus will already have run the migrate operation.

```java
import io.quarkus.liquibase.LiquibaseFactory;

@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    LiquibaseFactory liquibaseFactory; <1>

    @Inject
    @LiquibaseDataSource("inventory") <2>
    LiquibaseFactory liquibaseFactoryForInventory;

    @Inject
    @Named("liquibase_users") <3>
    LiquibaseFactory liquibaseFactoryForUsers;

    public void checkMigration() {
        // Use the liquibase instance manually
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.dropAll(); <4>
            liquibase.validate();
            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
            // Get the list of liquibase change set statuses
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(), liquibaseFactory.createLabels()); <5>
        }
    }
}
```

1. Inject the LiquibaseFactory object
2. Inject Liquibase for named datasources using the Quarkus `LiquibaseDataSource` qualifier
3. Inject Liquibase for named datasources
4. Use the Liquibase instance directly
5. List of applied or not applied liquibase ChangeSets

## Liquibase on Kubernetes

Sometimes, it's helpful not to execute Liquibase initialization on each application startup. One such example is when deploying on Kubernetes, where it doesn't make sense to execute Liquibase on every single replica. Instead it's desirable to execute it once and then start the actual application without Liquibase. To support this use case, when generating manifests for Kubernetes the generated manifests contain a Kubernetes initialization `Job` for Liquibase. The `Job` performs initialization and the actual `Pod`, will starts once the `Job` is successfully completed.

### Disabling

The feature is enabled by default and can be globally disabled, using:

```properties
quarkus.kubernetes.init-task-defaults.enabled=false
```

or on OpenShift:

```properties
quarkus.openshift.init-task-defaults.enabled=false
```

### Using a custom image that controls waiting for the Job

To change the `wait-for` image which by default is `groundnuty/k8s-wait-for:no-root-v1.7` you can use:

```properties
quarkus.kubernetes.init-task-defaults.wait-for-container.image=my/wait-for-image:1.0
```

or on OpenShift:

```properties
quarkus.openshift.init-task-defaults.wait-for-container.image=my/wait-for-image:1.0
```

**Note**: In this context globally means `for all extensions that support init task externalization`.

## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-liquibase_quarkus-liquibase-enabled"></a> [`quarkus.liquibase.enabled`](#quarkus-liquibase_quarkus-liquibase-enabled) config_property_copy_button:+++quarkus.liquibase.enabled+++[]<br><br>Flag to enable / disable Liquibase.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-liquibase_section_quarkus-liquibase"></a> [Datasources](#quarkus-liquibase_section_quarkus-liquibase) | Type | Default |
| [lock] <a id="quarkus-liquibase_quarkus-liquibase-change-log"></a> [`quarkus.liquibase.change-log`](#quarkus-liquibase_quarkus-liquibase-change-log) config_property_copy_button:+++quarkus.liquibase.change-log+++[]<br><br>`quarkus.liquibase."datasource-name".change-log` config_property_copy_button:+++quarkus.liquibase."datasource-name".change-log+++[]<br><br>The liquibase change log file. All included change log files in this file are scanned and add to the projects.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_CHANGE_LOG+++[] | string | `db/changeLog.xml` |
| [lock] <a id="quarkus-liquibase_quarkus-liquibase-search-path"></a> [`quarkus.liquibase.search-path`](#quarkus-liquibase_quarkus-liquibase-search-path) config_property_copy_button:+++quarkus.liquibase.search-path+++[]<br><br>`quarkus.liquibase."datasource-name".search-path` config_property_copy_button:+++quarkus.liquibase."datasource-name".search-path+++[]<br><br>The search path for DirectoryResourceAccessor<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_SEARCH_PATH+++[] | list of string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-migrate-at-start"></a> [`quarkus.liquibase.migrate-at-start`](#quarkus-liquibase_quarkus-liquibase-migrate-at-start) config_property_copy_button:+++quarkus.liquibase.migrate-at-start+++[]<br><br>`quarkus.liquibase."datasource-name".migrate-at-start` config_property_copy_button:+++quarkus.liquibase."datasource-name".migrate-at-start+++[]<br><br>`true` to execute Liquibase automatically when the application starts, `false` otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MIGRATE_AT_START+++[] | boolean | `false` |
| <a id="quarkus-liquibase_quarkus-liquibase-validate-on-migrate"></a> [`quarkus.liquibase.validate-on-migrate`](#quarkus-liquibase_quarkus-liquibase-validate-on-migrate) config_property_copy_button:+++quarkus.liquibase.validate-on-migrate+++[]<br><br>`quarkus.liquibase."datasource-name".validate-on-migrate` config_property_copy_button:+++quarkus.liquibase."datasource-name".validate-on-migrate+++[]<br><br>`true` to validate the applied changes against the available ones, `false` otherwise. It is only used if `migration-at-start` is `true`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_VALIDATE_ON_MIGRATE+++[] | boolean | `true` |
| <a id="quarkus-liquibase_quarkus-liquibase-clean-at-start"></a> [`quarkus.liquibase.clean-at-start`](#quarkus-liquibase_quarkus-liquibase-clean-at-start) config_property_copy_button:+++quarkus.liquibase.clean-at-start+++[]<br><br>`quarkus.liquibase."datasource-name".clean-at-start` config_property_copy_button:+++quarkus.liquibase."datasource-name".clean-at-start+++[]<br><br>`true` to execute Liquibase clean command automatically when the application starts, `false` otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_CLEAN_AT_START+++[] | boolean | `false` |
| <a id="quarkus-liquibase_quarkus-liquibase-contexts"></a> [`quarkus.liquibase.contexts`](#quarkus-liquibase_quarkus-liquibase-contexts) config_property_copy_button:+++quarkus.liquibase.contexts+++[]<br><br>`quarkus.liquibase."datasource-name".contexts` config_property_copy_button:+++quarkus.liquibase."datasource-name".contexts+++[]<br><br>Comma-separated case-sensitive list of ChangeSet contexts to execute for liquibase.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_CONTEXTS+++[] | list of string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-labels"></a> [`quarkus.liquibase.labels`](#quarkus-liquibase_quarkus-liquibase-labels) config_property_copy_button:+++quarkus.liquibase.labels+++[]<br><br>`quarkus.liquibase."datasource-name".labels` config_property_copy_button:+++quarkus.liquibase."datasource-name".labels+++[]<br><br>Comma-separated case-sensitive list of expressions defining labeled ChangeSet to execute for liquibase.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_LABELS+++[] | list of string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-change-log-parameters-parameter-name"></a> [`quarkus.liquibase.change-log-parameters."parameter-name"`](#quarkus-liquibase_quarkus-liquibase-change-log-parameters-parameter-name) config_property_copy_button:+++quarkus.liquibase.change-log-parameters."parameter-name"+++[]<br><br>`quarkus.liquibase."datasource-name".change-log-parameters."parameter-name"` config_property_copy_button:+++quarkus.liquibase."datasource-name".change-log-parameters."parameter-name"+++[]<br><br>Map of parameters that can be used inside Liquibase changeLog files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_CHANGE_LOG_PARAMETERS__PARAMETER_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-liquibase_quarkus-liquibase-database-change-log-lock-table-name"></a> [`quarkus.liquibase.database-change-log-lock-table-name`](#quarkus-liquibase_quarkus-liquibase-database-change-log-lock-table-name) config_property_copy_button:+++quarkus.liquibase.database-change-log-lock-table-name+++[]<br><br>`quarkus.liquibase."datasource-name".database-change-log-lock-table-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".database-change-log-lock-table-name+++[]<br><br>The liquibase change log lock table name. Name of table to use for tracking concurrent Liquibase usage.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_DATABASE_CHANGE_LOG_LOCK_TABLE_NAME+++[] | string | `DATABASECHANGELOGLOCK` |
| <a id="quarkus-liquibase_quarkus-liquibase-database-change-log-table-name"></a> [`quarkus.liquibase.database-change-log-table-name`](#quarkus-liquibase_quarkus-liquibase-database-change-log-table-name) config_property_copy_button:+++quarkus.liquibase.database-change-log-table-name+++[]<br><br>`quarkus.liquibase."datasource-name".database-change-log-table-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".database-change-log-table-name+++[]<br><br>The liquibase change log table name. Name of table to use for tracking change history.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_DATABASE_CHANGE_LOG_TABLE_NAME+++[] | string | `DATABASECHANGELOG` |
| <a id="quarkus-liquibase_quarkus-liquibase-default-catalog-name"></a> [`quarkus.liquibase.default-catalog-name`](#quarkus-liquibase_quarkus-liquibase-default-catalog-name) config_property_copy_button:+++quarkus.liquibase.default-catalog-name+++[]<br><br>`quarkus.liquibase."datasource-name".default-catalog-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".default-catalog-name+++[]<br><br>The name of Liquibase's default catalog.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_DEFAULT_CATALOG_NAME+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-default-schema-name"></a> [`quarkus.liquibase.default-schema-name`](#quarkus-liquibase_quarkus-liquibase-default-schema-name) config_property_copy_button:+++quarkus.liquibase.default-schema-name+++[]<br><br>`quarkus.liquibase."datasource-name".default-schema-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".default-schema-name+++[]<br><br>The name of Liquibase's default schema. Overwrites the default schema name (returned by the RDBMS) with a different database schema.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_DEFAULT_SCHEMA_NAME+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-username"></a> [`quarkus.liquibase.username`](#quarkus-liquibase_quarkus-liquibase-username) config_property_copy_button:+++quarkus.liquibase.username+++[]<br><br>`quarkus.liquibase."datasource-name".username` config_property_copy_button:+++quarkus.liquibase."datasource-name".username+++[]<br><br>The username that Liquibase uses to connect to the database. If no specific username is configured, falls back to the datasource username and password.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_USERNAME+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-password"></a> [`quarkus.liquibase.password`](#quarkus-liquibase_quarkus-liquibase-password) config_property_copy_button:+++quarkus.liquibase.password+++[]<br><br>`quarkus.liquibase."datasource-name".password` config_property_copy_button:+++quarkus.liquibase."datasource-name".password+++[]<br><br>The password that Liquibase uses to connect to the database. If no specific password is configured, falls back to the datasource username and password.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_PASSWORD+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-liquibase-catalog-name"></a> [`quarkus.liquibase.liquibase-catalog-name`](#quarkus-liquibase_quarkus-liquibase-liquibase-catalog-name) config_property_copy_button:+++quarkus.liquibase.liquibase-catalog-name+++[]<br><br>`quarkus.liquibase."datasource-name".liquibase-catalog-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".liquibase-catalog-name+++[]<br><br>The name of the catalog with the liquibase tables.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_LIQUIBASE_CATALOG_NAME+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-liquibase-schema-name"></a> [`quarkus.liquibase.liquibase-schema-name`](#quarkus-liquibase_quarkus-liquibase-liquibase-schema-name) config_property_copy_button:+++quarkus.liquibase.liquibase-schema-name+++[]<br><br>`quarkus.liquibase."datasource-name".liquibase-schema-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".liquibase-schema-name+++[]<br><br>The name of the schema with the liquibase tables.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_LIQUIBASE_SCHEMA_NAME+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-liquibase-tablespace-name"></a> [`quarkus.liquibase.liquibase-tablespace-name`](#quarkus-liquibase_quarkus-liquibase-liquibase-tablespace-name) config_property_copy_button:+++quarkus.liquibase.liquibase-tablespace-name+++[]<br><br>`quarkus.liquibase."datasource-name".liquibase-tablespace-name` config_property_copy_button:+++quarkus.liquibase."datasource-name".liquibase-tablespace-name+++[]<br><br>The name of the tablespace where the -LOG and -LOCK tables will be created (if they do not exist yet).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_LIQUIBASE_TABLESPACE_NAME+++[] | string |  |
| <a id="quarkus-liquibase_quarkus-liquibase-allow-duplicated-changeset-identifiers"></a> [`quarkus.liquibase.allow-duplicated-changeset-identifiers`](#quarkus-liquibase_quarkus-liquibase-allow-duplicated-changeset-identifiers) config_property_copy_button:+++quarkus.liquibase.allow-duplicated-changeset-identifiers+++[]<br><br>`quarkus.liquibase."datasource-name".allow-duplicated-changeset-identifiers` config_property_copy_button:+++quarkus.liquibase."datasource-name".allow-duplicated-changeset-identifiers+++[]<br><br>Allows duplicated changeset identifiers without failing Liquibase execution.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_ALLOW_DUPLICATED_CHANGESET_IDENTIFIERS+++[] | boolean |  |
| <a id="quarkus-liquibase_quarkus-liquibase-secure-parsing"></a> [`quarkus.liquibase.secure-parsing`](#quarkus-liquibase_quarkus-liquibase-secure-parsing) config_property_copy_button:+++quarkus.liquibase.secure-parsing+++[]<br><br>`quarkus.liquibase."datasource-name".secure-parsing` config_property_copy_button:+++quarkus.liquibase."datasource-name".secure-parsing+++[]<br><br>Whether Liquibase should enforce secure parsing.<br><br>If secure parsing is enforced, insecure files may not be parsed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_SECURE_PARSING+++[] | boolean | `true` |
