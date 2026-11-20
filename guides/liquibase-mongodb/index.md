# Using Liquibase MongoDB

[Liquibase](https://www.liquibase.org/) is an open source tool for database schema change management, it allows managing MongoDB databases via its [MongoDB Extension](https://github.com/liquibase/liquibase-mongodb).

Quarkus provides first class support for using Liquibase MongoDB Extension as will be explained in this guide.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `liquibase-mongodb-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/liquibase-mongodb-quickstart).

## Setting up support for Liquibase

To start using the Liquibase MongoDB Extension with your project, you just need to:

- add your changeLog to the `src/main/resources/db/changeLog.xml` file as you usually do with Liquibase
- activate the `migrate-at-start` option to migrate the schema automatically or inject the `Liquibase` object and run your migration as you normally do.

You can add the `liquibase-mongodb` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add liquibase-mongodb
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='liquibase-mongodb'
```

**Gradle**

```bash
./gradlew addExtension --extensions='liquibase-mongodb'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-liquibase-mongodb</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-liquibase-mongodb")
```

The Liquibase MongoDB extension support relies on the Quarkus MongoDB client config. You need to add the MongoDB config to the `application.properties` file in order to allow Liquibase to manage the schema.

The following is an example for the `application.properties` file:

```properties
# configure MongoDB
quarkus.mongodb.connection-string = mongodb://localhost:27017/mydatabase

# Liquibase MongoDB minimal config properties
quarkus.liquibase-mongodb.migrate-at-start=true

# Liquibase MongoDB optional config properties
# quarkus.liquibase-mongodb.change-log=db/changeLog.xml
# quarkus.liquibase-mongodb.validate-on-migrate=true
# quarkus.liquibase-mongodb.clean-at-start=false
# quarkus.liquibase-mongodb.contexts=Context1,Context2
# quarkus.liquibase-mongodb.labels=Label1,Label2
# quarkus.liquibase-mongodb.default-catalog-name=DefaultCatalog
# quarkus.liquibase-mongodb.default-schema-name=DefaultSchema
```

> [!NOTE]
> Liquibase needs a database either in the connection string or with the `quarkus.mongodb.database` property.

> [!NOTE]
> By default, Liquibase MongoDB is configured to use the default MongoDB client Quarkus creates, but you can configure the extension to use a named client by setting `quarkus.liquibase-mongodb.mongo-client-name`.

Add a changeLog file to the default folder following the Liquibase naming conventions: `src/main/resources/db/changeLog.xml` YAML, JSON and XML formats are supported for the changeLog.

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd
        http://www.liquibase.org/xml/ns/dbchangelog-ext https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd">

    <changeSet id="1" author="loic">
        <ext:createCollection collectionName="Fruit"/>

        <ext:createIndex collectionName="Fruit">
            <ext:keys>{color: 1}</ext:keys>
            <ext:options>{name: "colorIdx"}</ext:options>
        </ext:createIndex>

        <ext:insertOne collectionName="Fruit">
            <ext:document>{"name":"orange", "color": "orange"}</ext:document>
        </ext:insertOne>
    </changeSet>

</databaseChangeLog>
```

Now you can start your application and Quarkus will run the Liquibase's update method according to your config.

## Multiple Clients

Liquibase MongoDB can be configured to support multiple clients. The properties are prefixed exactly the same way as named MongoDB clients. For example:

```properties
quarkus.liquibase-mongodb.users.change-log=db/users.xml
quarkus.liquibase-mongodb.users.migrate-at-start=true

quarkus.mongodb.users.database=users
quarkus.mongodb.users.hosts=localhost:27017

quarkus.liquibase-mongodb.inventory.change-log=db/inventory.xml
quarkus.liquibase-mongodb.inventory.migrate-at-start=false

quarkus.mongodb.inventory.connection-string=mongodb://localhost:27017/inventory
```

> [!NOTE]
> The property `quarkus.liquibase-mongodb.[MongoDB client name].change-log` is the minimal configuration required for the extension to detect a new client setup.

## Using the Liquibase object

In case you are interested in using the `Liquibase` object directly, you can inject it as follows:

> [!NOTE]
> If you enabled the `quarkus.liquibase.migrate-at-start` property, by the time you use the Liquibase instance, Quarkus will already have run the migrate operation.

```java
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.mongodb.runtime.LiquibaseMongodbClient;

@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    LiquibaseMongodbFactory liquibaseMongodbFactory; <1>

    @Inject
    @LiquibaseMongodbClient("inventory") <2>
    LiquibaseMongodbFactory liquibaseMongodbFactoryForInventory;

    public void checkMigration() {
        // Use the liquibase instance manually
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.dropAll(); <3>
            liquibase.validate();
            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
            // Get the list of liquibase change set statuses
            List<ChangeSetStatus> status = liquibase.getChangeSetStatuses(liquibaseFactory.createContexts(), liquibaseFactory.createLabels()); <4>
        }
    }
}
```

1. Inject the LiquibaseFactory object
2. Inject Liquibase MongoDB for named clients using the Quarkus `LiquibaseMongodbClient` qualifier
3. Use the Liquibase instance directly
4. List of applied or not applied liquibase ChangeSets

## Liquibase MongoDB on Kubernetes

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
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-enabled"></a> [`quarkus.liquibase-mongodb.enabled`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-enabled) config_property_copy_button:+++quarkus.liquibase-mongodb.enabled+++[]<br><br>Flag to enable / disable Liquibase.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-liquibase-mongodb_section_quarkus-liquibase-mongodb"></a> [The liquibase configuration config by client name](#quarkus-liquibase-mongodb_section_quarkus-liquibase-mongodb) | Type | Default |
| [lock] <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-change-log"></a> [`quarkus.liquibase-mongodb.change-log`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-change-log) config_property_copy_button:+++quarkus.liquibase-mongodb.change-log+++[]<br><br>`quarkus.liquibase-mongodb."client-name".change-log` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".change-log+++[]<br><br>The change log file<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_CHANGE_LOG+++[] | string | `db/changeLog.xml` |
| [lock] <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-search-path"></a> [`quarkus.liquibase-mongodb.search-path`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-search-path) config_property_copy_button:+++quarkus.liquibase-mongodb.search-path+++[]<br><br>`quarkus.liquibase-mongodb."client-name".search-path` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".search-path+++[]<br><br>The search path for DirectoryResourceAccessor<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_SEARCH_PATH+++[] | list of string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-mongo-client-name"></a> [`quarkus.liquibase-mongodb.mongo-client-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-mongo-client-name) config_property_copy_button:+++quarkus.liquibase-mongodb.mongo-client-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".mongo-client-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".mongo-client-name+++[]<br><br>Mongodb client name to use to connect to database, defaults to the default mongodb client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_MONGO_CLIENT_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-migrate-at-start"></a> [`quarkus.liquibase-mongodb.migrate-at-start`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-migrate-at-start) config_property_copy_button:+++quarkus.liquibase-mongodb.migrate-at-start+++[]<br><br>`quarkus.liquibase-mongodb."client-name".migrate-at-start` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".migrate-at-start+++[]<br><br>The migrate at start flag<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_MIGRATE_AT_START+++[] | boolean | `false` |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-validate-on-migrate"></a> [`quarkus.liquibase-mongodb.validate-on-migrate`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-validate-on-migrate) config_property_copy_button:+++quarkus.liquibase-mongodb.validate-on-migrate+++[]<br><br>`quarkus.liquibase-mongodb."client-name".validate-on-migrate` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".validate-on-migrate+++[]<br><br>The validate on update flag<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_VALIDATE_ON_MIGRATE+++[] | boolean | `true` |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-clean-at-start"></a> [`quarkus.liquibase-mongodb.clean-at-start`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-clean-at-start) config_property_copy_button:+++quarkus.liquibase-mongodb.clean-at-start+++[]<br><br>`quarkus.liquibase-mongodb."client-name".clean-at-start` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".clean-at-start+++[]<br><br>The clean at start flag<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_CLEAN_AT_START+++[] | boolean | `false` |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-change-log-parameters-change-log-parameters"></a> [`quarkus.liquibase-mongodb.change-log-parameters."change-log-parameters"`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-change-log-parameters-change-log-parameters) config_property_copy_button:+++quarkus.liquibase-mongodb.change-log-parameters."change-log-parameters"+++[]<br><br>`quarkus.liquibase-mongodb."client-name".change-log-parameters."change-log-parameters"` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".change-log-parameters."change-log-parameters"+++[]<br><br>The parameters to be passed to the changelog. Defined as key value pairs.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_CHANGE_LOG_PARAMETERS__CHANGE_LOG_PARAMETERS_+++[] | Map\<String,String> |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-database-change-log-lock-table-name"></a> [`quarkus.liquibase-mongodb.database-change-log-lock-table-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-database-change-log-lock-table-name) config_property_copy_button:+++quarkus.liquibase-mongodb.database-change-log-lock-table-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".database-change-log-lock-table-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".database-change-log-lock-table-name+++[]<br><br>The liquibase change log lock table name. Name of table to use for tracking concurrent Liquibase usage.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_DATABASE_CHANGE_LOG_LOCK_TABLE_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-database-change-log-table-name"></a> [`quarkus.liquibase-mongodb.database-change-log-table-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-database-change-log-table-name) config_property_copy_button:+++quarkus.liquibase-mongodb.database-change-log-table-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".database-change-log-table-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".database-change-log-table-name+++[]<br><br>The liquibase change log table name. Name of table to use for tracking change history.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_DATABASE_CHANGE_LOG_TABLE_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-contexts"></a> [`quarkus.liquibase-mongodb.contexts`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-contexts) config_property_copy_button:+++quarkus.liquibase-mongodb.contexts+++[]<br><br>`quarkus.liquibase-mongodb."client-name".contexts` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".contexts+++[]<br><br>The list of contexts<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_CONTEXTS+++[] | list of string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-labels"></a> [`quarkus.liquibase-mongodb.labels`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-labels) config_property_copy_button:+++quarkus.liquibase-mongodb.labels+++[]<br><br>`quarkus.liquibase-mongodb."client-name".labels` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".labels+++[]<br><br>The list of labels<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_LABELS+++[] | list of string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-default-catalog-name"></a> [`quarkus.liquibase-mongodb.default-catalog-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-default-catalog-name) config_property_copy_button:+++quarkus.liquibase-mongodb.default-catalog-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".default-catalog-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".default-catalog-name+++[]<br><br>The default catalog name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_DEFAULT_CATALOG_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-default-schema-name"></a> [`quarkus.liquibase-mongodb.default-schema-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-default-schema-name) config_property_copy_button:+++quarkus.liquibase-mongodb.default-schema-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".default-schema-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".default-schema-name+++[]<br><br>The default schema name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_DEFAULT_SCHEMA_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-liquibase-catalog-name"></a> [`quarkus.liquibase-mongodb.liquibase-catalog-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-liquibase-catalog-name) config_property_copy_button:+++quarkus.liquibase-mongodb.liquibase-catalog-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".liquibase-catalog-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".liquibase-catalog-name+++[]<br><br>The liquibase tables catalog name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_LIQUIBASE_CATALOG_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-liquibase-schema-name"></a> [`quarkus.liquibase-mongodb.liquibase-schema-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-liquibase-schema-name) config_property_copy_button:+++quarkus.liquibase-mongodb.liquibase-schema-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".liquibase-schema-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".liquibase-schema-name+++[]<br><br>The liquibase tables schema name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_LIQUIBASE_SCHEMA_NAME+++[] | string |  |
| <a id="quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-liquibase-tablespace-name"></a> [`quarkus.liquibase-mongodb.liquibase-tablespace-name`](#quarkus-liquibase-mongodb_quarkus-liquibase-mongodb-liquibase-tablespace-name) config_property_copy_button:+++quarkus.liquibase-mongodb.liquibase-tablespace-name+++[]<br><br>`quarkus.liquibase-mongodb."client-name".liquibase-tablespace-name` config_property_copy_button:+++quarkus.liquibase-mongodb."client-name".liquibase-tablespace-name+++[]<br><br>The liquibase tables tablespace name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LIQUIBASE_MONGODB_LIQUIBASE_TABLESPACE_NAME+++[] | string |  |
