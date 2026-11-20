# Using Flyway

[Flyway](https://flywaydb.org/) is a popular database migration tool that is commonly used in JVM environments.

Quarkus provides first class support for using Flyway as will be explained in this guide.

## Setting up support for Flyway

As shown in the [Developing with Flyway](#developing-with-flyway) section, to start using Flyway with your project, you just need to:

- add your migrations to the `src/main/resources/db/migration` folder as you usually do with Flyway
- activate the `migrate-at-start` option to migrate the schema automatically or inject the `Flyway` object and run your migration as you normally do

In your build file, add the following dependencies:

- the Flyway extension
- your JDBC driver extension (`quarkus-jdbc-postgresql`, `quarkus-jdbc-h2`, `quarkus-jdbc-mariadb`, ...)
- unless you're using in-memory or file databases (such as H2 or SQLite), you need to add a flyway module dependency corresponding to the database you're using. ([for more details](https://github.com/flyway/flyway/issues/3780))

**pom.xml**

```xml
<!-- Flyway specific dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-flyway</artifactId>
</dependency>

<!-- JDBC driver dependencies -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>

<!-- Flyway SQL Server specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-sqlserver</artifactId>
</dependency>

<!-- Flyway MariaDB/MySQL specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>

<!-- Flyway Oracle specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-oracle</artifactId>
</dependency>

<!-- Flyway PostgreSQL specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>

<!-- Flyway DB2 specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-db2</artifactId>
</dependency>

<!-- HSQLDB specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-hsqldb</artifactId>
</dependency>

<!-- Informix specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-informix</artifactId>
</dependency>

<!-- Redshift specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-redshift</artifactId>
</dependency>

<!-- Saphana specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-saphana</artifactId>
</dependency>

<!-- Snowflake specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-snowflake</artifactId>
</dependency>

<!-- Sybasease specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-sybasease</artifactId>
</dependency>

<!-- Firebird specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-firebird</artifactId>
</dependency>

<!-- BigQuery specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-gcp-bigquery</artifactId>
</dependency>

<!-- Spanner specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-gcp-spanner</artifactId>
</dependency>

<!-- Singlestore specific dependencies -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-singlestore</artifactId>
</dependency>

```

**build.gradle**

```gradle
// Flyway specific dependencies
implementation("io.quarkus:quarkus-flyway")
// JDBC driver dependencies
implementation("io.quarkus:quarkus-jdbc-postgresql")
// Flyway SQL Server specific dependencies
implementation("org.flywaydb:flyway-sqlserver")
// Flyway MariaDB/MySQL specific dependencies
implementation("org.flywaydb:flyway-mysql")
// Flyway Oracle specific dependencies
implementation("org.flywaydb:flyway-database-oracle")
// Flyway PostgreSQL specific dependencies
implementation("org.flywaydb:flyway-database-postgresql")
// Flyway DB2 specific dependencies
implementation("org.flywaydb:flyway-database-db2")
// HSQLDB specific dependencies
implementation("org.flywaydb:flyway-database-hsqldb")
// Informix specific dependencies
implementation("org.flywaydb:flyway-database-informix")
// Redshift specific dependencies
implementation("org.flywaydb:flyway-database-redshift")
// Saphana specific dependencies
implementation("org.flywaydb:flyway-database-saphana")
// Snowflake specific dependencies
implementation("org.flywaydb:flyway-database-snowflake")
// Sybasease specific dependencies
implementation("org.flywaydb:flyway-database-sybasease")
// Firebird specific dependencies
implementation("org.flywaydb:flyway-firebird")
// BigQuery specific dependencies
implementation("org.flywaydb:flyway-gcp-bigquery")
// Spanner specific dependencies
implementation("org.flywaydb:flyway-gcp-spanner")
// Singlestore specific dependencies
implementation("org.flywaydb:flyway-singlestore:10.15.0")

```

Flyway support relies on the Quarkus datasource config. It can be customized for the default datasource as well as for every [named datasource](#multiple-datasources). First, you need to add the datasource config to the `application.properties` file in order to allow Flyway to manage the schema. Also, you can customize the Flyway behaviour by using the following properties:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-flyway_quarkus-flyway-enabled"></a> [`quarkus.flyway.enabled`](#quarkus-flyway_quarkus-flyway-enabled) config_property_copy_button:+++quarkus.flyway.enabled+++[]<br><br>Whether Flyway is enabled **during the build**.<br><br>If Flyway is disabled, the Flyway beans won't be created and Flyway won't be usable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-flyway_section_quarkus-flyway"></a> [Datasources](#quarkus-flyway_section_quarkus-flyway) | Type | Default |
| [lock] <a id="quarkus-flyway_quarkus-flyway-locations"></a> [`quarkus.flyway.locations`](#quarkus-flyway_quarkus-flyway-locations) config_property_copy_button:+++quarkus.flyway.locations+++[]<br><br>`quarkus.flyway."datasource-name".locations` config_property_copy_button:+++quarkus.flyway."datasource-name".locations+++[]<br><br>Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.<br><br>Unprefixed locations or locations starting with classpath: point to a package on the classpath and may contain both SQL and Java-based migrations.<br><br>Locations starting with filesystem: point to a directory on the filesystem, may only contain SQL migrations and are only scanned recursively down non-hidden directories.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_LOCATIONS+++[] | list of string | `db/migration` |
| [lock] <a id="quarkus-flyway_quarkus-flyway-callbacks"></a> [`quarkus.flyway.callbacks`](#quarkus-flyway_quarkus-flyway-callbacks) config_property_copy_button:+++quarkus.flyway.callbacks+++[]<br><br>`quarkus.flyway."datasource-name".callbacks` config_property_copy_button:+++quarkus.flyway."datasource-name".callbacks+++[]<br><br>Comma-separated list of fully qualified class names of Callback implementations to use to hook into the Flyway lifecycle. The `org.flywaydb.core.api.callback.Callback` subclass must have a no-args constructor and must not be abstract. These classes must also not have any fields that hold state (unless that state is initialized in the constructor).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_CALLBACKS+++[] | list of string |  |
| <a id="quarkus-flyway_quarkus-flyway-active"></a> [`quarkus.flyway.active`](#quarkus-flyway_quarkus-flyway-active) config_property_copy_button:+++quarkus.flyway.active+++[]<br><br>`quarkus.flyway."datasource-name".active` config_property_copy_button:+++quarkus.flyway."datasource-name".active+++[]<br><br>Flag to activate/deactivate Flyway for a specific datasource at runtime.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_ACTIVE+++[] | boolean | `'true' if the datasource is active; 'false' otherwise` |
| <a id="quarkus-flyway_quarkus-flyway-connect-retries"></a> [`quarkus.flyway.connect-retries`](#quarkus-flyway_quarkus-flyway-connect-retries) config_property_copy_button:+++quarkus.flyway.connect-retries+++[]<br><br>`quarkus.flyway."datasource-name".connect-retries` config_property_copy_button:+++quarkus.flyway."datasource-name".connect-retries+++[]<br><br>The maximum number of retries when attempting to connect to the database.<br><br>After each failed attempt, Flyway will wait up to the configured `connect-retries-interval` duration before attempting to connect again, up to the maximum number of times specified by connectRetries.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_CONNECT_RETRIES+++[] | int |  |
| <a id="quarkus-flyway_quarkus-flyway-connect-retries-interval"></a> [`quarkus.flyway.connect-retries-interval`](#quarkus-flyway_quarkus-flyway-connect-retries-interval) config_property_copy_button:+++quarkus.flyway.connect-retries-interval+++[]<br><br>`quarkus.flyway."datasource-name".connect-retries-interval` config_property_copy_button:+++quarkus.flyway."datasource-name".connect-retries-interval+++[]<br><br>The maximum time between retries when attempting to connect to the database.<br><br>This will cap the interval between connect retries to the value provided.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_CONNECT_RETRIES_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-flyway_quarkus-flyway) | `120 seconds` |
| <a id="quarkus-flyway_quarkus-flyway-default-schema"></a> [`quarkus.flyway.default-schema`](#quarkus-flyway_quarkus-flyway-default-schema) config_property_copy_button:+++quarkus.flyway.default-schema+++[]<br><br>`quarkus.flyway."datasource-name".default-schema` config_property_copy_button:+++quarkus.flyway."datasource-name".default-schema+++[]<br><br>Sets the default schema managed by Flyway. This schema name is case-sensitive. If not specified, but *schemas* is, Flyway uses the first schema in that list. If that is also not specified, Flyway uses the default schema for the database connection.<br><br>Consequences:<br><br>\* This schema will be the one containing the schema history table. \* This schema will be the default for the database connection (provided the database supports this concept).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_DEFAULT_SCHEMA+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-jdbc-url"></a> [`quarkus.flyway.jdbc-url`](#quarkus-flyway_quarkus-flyway-jdbc-url) config_property_copy_button:+++quarkus.flyway.jdbc-url+++[]<br><br>`quarkus.flyway."datasource-name".jdbc-url` config_property_copy_button:+++quarkus.flyway."datasource-name".jdbc-url+++[]<br><br>The JDBC URL that Flyway uses to connect to the database. Falls back to the datasource URL if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_JDBC_URL+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-username"></a> [`quarkus.flyway.username`](#quarkus-flyway_quarkus-flyway-username) config_property_copy_button:+++quarkus.flyway.username+++[]<br><br>`quarkus.flyway."datasource-name".username` config_property_copy_button:+++quarkus.flyway."datasource-name".username+++[]<br><br>The username that Flyway uses to connect to the database. If no specific JDBC URL is configured, falls back to the datasource username if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_USERNAME+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-password"></a> [`quarkus.flyway.password`](#quarkus-flyway_quarkus-flyway-password) config_property_copy_button:+++quarkus.flyway.password+++[]<br><br>`quarkus.flyway."datasource-name".password` config_property_copy_button:+++quarkus.flyway."datasource-name".password+++[]<br><br>The password that Flyway uses to connect to the database. If no specific JDBC URL is configured, falls back to the datasource password if not specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_PASSWORD+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-schemas"></a> [`quarkus.flyway.schemas`](#quarkus-flyway_quarkus-flyway-schemas) config_property_copy_button:+++quarkus.flyway.schemas+++[]<br><br>`quarkus.flyway."datasource-name".schemas` config_property_copy_button:+++quarkus.flyway."datasource-name".schemas+++[]<br><br>Comma-separated case-sensitive list of schemas managed by Flyway. The first schema in the list will be automatically set as the default one during the migration. It will also be the one containing the schema history table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_SCHEMAS+++[] | list of string |  |
| <a id="quarkus-flyway_quarkus-flyway-table"></a> [`quarkus.flyway.table`](#quarkus-flyway_quarkus-flyway-table) config_property_copy_button:+++quarkus.flyway.table+++[]<br><br>`quarkus.flyway."datasource-name".table` config_property_copy_button:+++quarkus.flyway."datasource-name".table+++[]<br><br>The name of Flyway's schema history table. By default (single-schema mode), the schema history table is placed in the default schema for the connection provided by the datasource. When the flyway.schemas property is set (multi-schema mode), the schema history table is placed in the first schema of the list.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_TABLE+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-sql-migration-prefix"></a> [`quarkus.flyway.sql-migration-prefix`](#quarkus-flyway_quarkus-flyway-sql-migration-prefix) config_property_copy_button:+++quarkus.flyway.sql-migration-prefix+++[]<br><br>`quarkus.flyway."datasource-name".sql-migration-prefix` config_property_copy_button:+++quarkus.flyway."datasource-name".sql-migration-prefix+++[]<br><br>The file name prefix for versioned SQL migrations. Versioned SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix , which using the defaults translates to V1.1__My_description.sql<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_SQL_MIGRATION_PREFIX+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-repeatable-sql-migration-prefix"></a> [`quarkus.flyway.repeatable-sql-migration-prefix`](#quarkus-flyway_quarkus-flyway-repeatable-sql-migration-prefix) config_property_copy_button:+++quarkus.flyway.repeatable-sql-migration-prefix+++[]<br><br>`quarkus.flyway."datasource-name".repeatable-sql-migration-prefix` config_property_copy_button:+++quarkus.flyway."datasource-name".repeatable-sql-migration-prefix+++[]<br><br>The file name prefix for repeatable SQL migrations. Repeatable SQL migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix , which using the defaults translates to R__My_description.sql<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_REPEATABLE_SQL_MIGRATION_PREFIX+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-clean-at-start"></a> [`quarkus.flyway.clean-at-start`](#quarkus-flyway_quarkus-flyway-clean-at-start) config_property_copy_button:+++quarkus.flyway.clean-at-start+++[]<br><br>`quarkus.flyway."datasource-name".clean-at-start` config_property_copy_button:+++quarkus.flyway."datasource-name".clean-at-start+++[]<br><br>true to execute Flyway clean command automatically when the application starts, false otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_CLEAN_AT_START+++[] | boolean | `` `false` `` |
| <a id="quarkus-flyway_quarkus-flyway-clean-disabled"></a> [`quarkus.flyway.clean-disabled`](#quarkus-flyway_quarkus-flyway-clean-disabled) config_property_copy_button:+++quarkus.flyway.clean-disabled+++[]<br><br>`quarkus.flyway."datasource-name".clean-disabled` config_property_copy_button:+++quarkus.flyway."datasource-name".clean-disabled+++[]<br><br>true to prevent Flyway clean operations, false otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_CLEAN_DISABLED+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-migrate-at-start"></a> [`quarkus.flyway.migrate-at-start`](#quarkus-flyway_quarkus-flyway-migrate-at-start) config_property_copy_button:+++quarkus.flyway.migrate-at-start+++[]<br><br>`quarkus.flyway."datasource-name".migrate-at-start` config_property_copy_button:+++quarkus.flyway."datasource-name".migrate-at-start+++[]<br><br>true to execute Flyway automatically when the application starts, false otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_MIGRATE_AT_START+++[] | boolean | `` `false` `` |
| <a id="quarkus-flyway_quarkus-flyway-repair-at-start"></a> [`quarkus.flyway.repair-at-start`](#quarkus-flyway_quarkus-flyway-repair-at-start) config_property_copy_button:+++quarkus.flyway.repair-at-start+++[]<br><br>`quarkus.flyway."datasource-name".repair-at-start` config_property_copy_button:+++quarkus.flyway."datasource-name".repair-at-start+++[]<br><br>true to execute a Flyway repair command when the application starts, false otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_REPAIR_AT_START+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-validate-at-start"></a> [`quarkus.flyway.validate-at-start`](#quarkus-flyway_quarkus-flyway-validate-at-start) config_property_copy_button:+++quarkus.flyway.validate-at-start+++[]<br><br>`quarkus.flyway."datasource-name".validate-at-start` config_property_copy_button:+++quarkus.flyway."datasource-name".validate-at-start+++[]<br><br>true to execute a Flyway validate command when the application starts, false otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_VALIDATE_AT_START+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-validate-at-start-clean-on-validation-error"></a> [`quarkus.flyway.validate-at-start.clean-on-validation-error`](#quarkus-flyway_quarkus-flyway-validate-at-start-clean-on-validation-error) config_property_copy_button:+++quarkus.flyway.validate-at-start.clean-on-validation-error+++[]<br><br>`quarkus.flyway."datasource-name".validate-at-start.clean-on-validation-error` config_property_copy_button:+++quarkus.flyway."datasource-name".validate-at-start.clean-on-validation-error+++[]<br><br>true to automatically execute a Flyway clean command when a validation error occurs at start, false otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_VALIDATE_AT_START_CLEAN_ON_VALIDATION_ERROR+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-baseline-on-migrate"></a> [`quarkus.flyway.baseline-on-migrate`](#quarkus-flyway_quarkus-flyway-baseline-on-migrate) config_property_copy_button:+++quarkus.flyway.baseline-on-migrate+++[]<br><br>`quarkus.flyway."datasource-name".baseline-on-migrate` config_property_copy_button:+++quarkus.flyway."datasource-name".baseline-on-migrate+++[]<br><br>true to execute Flyway baseline before migrations This flag is ignored if the flyway_schema_history table exists in the current schema or if the current schema is empty. Note that this will not automatically call migrate, you must either enable baselineAtStart or programmatically call flyway.migrate().<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_BASELINE_ON_MIGRATE+++[] | boolean | `` `false` `` |
| <a id="quarkus-flyway_quarkus-flyway-baseline-at-start"></a> [`quarkus.flyway.baseline-at-start`](#quarkus-flyway_quarkus-flyway-baseline-at-start) config_property_copy_button:+++quarkus.flyway.baseline-at-start+++[]<br><br>`quarkus.flyway."datasource-name".baseline-at-start` config_property_copy_button:+++quarkus.flyway."datasource-name".baseline-at-start+++[]<br><br>true to execute Flyway baseline automatically when the application starts. This flag is ignored if the flyway_schema_history table exists in the current schema. This will work even if the current schema is empty.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_BASELINE_AT_START+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-baseline-version"></a> [`quarkus.flyway.baseline-version`](#quarkus-flyway_quarkus-flyway-baseline-version) config_property_copy_button:+++quarkus.flyway.baseline-version+++[]<br><br>`quarkus.flyway."datasource-name".baseline-version` config_property_copy_button:+++quarkus.flyway."datasource-name".baseline-version+++[]<br><br>The initial baseline version.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_BASELINE_VERSION+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-baseline-description"></a> [`quarkus.flyway.baseline-description`](#quarkus-flyway_quarkus-flyway-baseline-description) config_property_copy_button:+++quarkus.flyway.baseline-description+++[]<br><br>`quarkus.flyway."datasource-name".baseline-description` config_property_copy_button:+++quarkus.flyway."datasource-name".baseline-description+++[]<br><br>The description to tag an existing schema with when executing baseline.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_BASELINE_DESCRIPTION+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-validate-on-migrate"></a> [`quarkus.flyway.validate-on-migrate`](#quarkus-flyway_quarkus-flyway-validate-on-migrate) config_property_copy_button:+++quarkus.flyway.validate-on-migrate+++[]<br><br>`quarkus.flyway."datasource-name".validate-on-migrate` config_property_copy_button:+++quarkus.flyway."datasource-name".validate-on-migrate+++[]<br><br>Whether to automatically call validate when performing a migration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_VALIDATE_ON_MIGRATE+++[] | boolean | `true` |
| <a id="quarkus-flyway_quarkus-flyway-out-of-order"></a> [`quarkus.flyway.out-of-order`](#quarkus-flyway_quarkus-flyway-out-of-order) config_property_copy_button:+++quarkus.flyway.out-of-order+++[]<br><br>`quarkus.flyway."datasource-name".out-of-order` config_property_copy_button:+++quarkus.flyway."datasource-name".out-of-order+++[]<br><br>Allows migrations to be run "out of order".<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_OUT_OF_ORDER+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-ignore-missing-migrations"></a> [`quarkus.flyway.ignore-missing-migrations`](#quarkus-flyway_quarkus-flyway-ignore-missing-migrations) config_property_copy_button:+++quarkus.flyway.ignore-missing-migrations+++[]<br><br>`quarkus.flyway."datasource-name".ignore-missing-migrations` config_property_copy_button:+++quarkus.flyway."datasource-name".ignore-missing-migrations+++[]<br><br>Ignore missing migrations when reading the history table. When set to true migrations from older versions present in the history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default) the validation step will fail.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_IGNORE_MISSING_MIGRATIONS+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-ignore-future-migrations"></a> [`quarkus.flyway.ignore-future-migrations`](#quarkus-flyway_quarkus-flyway-ignore-future-migrations) config_property_copy_button:+++quarkus.flyway.ignore-future-migrations+++[]<br><br>`quarkus.flyway."datasource-name".ignore-future-migrations` config_property_copy_button:+++quarkus.flyway."datasource-name".ignore-future-migrations+++[]<br><br>Ignore future migrations when reading the history table. When set to true migrations from newer versions present in the history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default) the validation step will fail.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_IGNORE_FUTURE_MIGRATIONS+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-placeholders-placeholder-key"></a> [`quarkus.flyway.placeholders."placeholder-key"`](#quarkus-flyway_quarkus-flyway-placeholders-placeholder-key) config_property_copy_button:+++quarkus.flyway.placeholders."placeholder-key"+++[]<br><br>`quarkus.flyway."datasource-name".placeholders."placeholder-key"` config_property_copy_button:+++quarkus.flyway."datasource-name".placeholders."placeholder-key"+++[]<br><br>Sets the placeholders to replace in SQL migration scripts.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_PLACEHOLDERS__PLACEHOLDER_KEY_+++[] | Map\<String,String> |  |
| <a id="quarkus-flyway_quarkus-flyway-create-schemas"></a> [`quarkus.flyway.create-schemas`](#quarkus-flyway_quarkus-flyway-create-schemas) config_property_copy_button:+++quarkus.flyway.create-schemas+++[]<br><br>`quarkus.flyway."datasource-name".create-schemas` config_property_copy_button:+++quarkus.flyway."datasource-name".create-schemas+++[]<br><br>Whether Flyway should attempt to create the schemas specified in the schemas property<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_CREATE_SCHEMAS+++[] | boolean | `true` |
| <a id="quarkus-flyway_quarkus-flyway-placeholder-prefix"></a> [`quarkus.flyway.placeholder-prefix`](#quarkus-flyway_quarkus-flyway-placeholder-prefix) config_property_copy_button:+++quarkus.flyway.placeholder-prefix+++[]<br><br>`quarkus.flyway."datasource-name".placeholder-prefix` config_property_copy_button:+++quarkus.flyway."datasource-name".placeholder-prefix+++[]<br><br>Prefix of every placeholder (default: $++{++ )<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_PLACEHOLDER_PREFIX+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-placeholder-suffix"></a> [`quarkus.flyway.placeholder-suffix`](#quarkus-flyway_quarkus-flyway-placeholder-suffix) config_property_copy_button:+++quarkus.flyway.placeholder-suffix+++[]<br><br>`quarkus.flyway."datasource-name".placeholder-suffix` config_property_copy_button:+++quarkus.flyway."datasource-name".placeholder-suffix+++[]<br><br>Suffix of every placeholder (default: ++}++ )<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_PLACEHOLDER_SUFFIX+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-init-sql"></a> [`quarkus.flyway.init-sql`](#quarkus-flyway_quarkus-flyway-init-sql) config_property_copy_button:+++quarkus.flyway.init-sql+++[]<br><br>`quarkus.flyway."datasource-name".init-sql` config_property_copy_button:+++quarkus.flyway."datasource-name".init-sql+++[]<br><br>The SQL statements to run to initialize a new database connection immediately after opening it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_INIT_SQL+++[] | string |  |
| <a id="quarkus-flyway_quarkus-flyway-validate-migration-naming"></a> [`quarkus.flyway.validate-migration-naming`](#quarkus-flyway_quarkus-flyway-validate-migration-naming) config_property_copy_button:+++quarkus.flyway.validate-migration-naming+++[]<br><br>`quarkus.flyway."datasource-name".validate-migration-naming` config_property_copy_button:+++quarkus.flyway."datasource-name".validate-migration-naming+++[]<br><br>Whether to validate migrations and callbacks whose scripts do not obey the correct naming convention. A failure can be useful to check that errors such as case sensitivity in migration prefixes have been corrected.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_VALIDATE_MIGRATION_NAMING+++[] | boolean | `false` |
| <a id="quarkus-flyway_quarkus-flyway-ignore-migration-patterns"></a> [`quarkus.flyway.ignore-migration-patterns`](#quarkus-flyway_quarkus-flyway-ignore-migration-patterns) config_property_copy_button:+++quarkus.flyway.ignore-migration-patterns+++[]<br><br>`quarkus.flyway."datasource-name".ignore-migration-patterns` config_property_copy_button:+++quarkus.flyway."datasource-name".ignore-migration-patterns+++[]<br><br>Ignore migrations during validate and repair according to a given list of patterns (see <https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns> for more information). When this configuration is set, the ignoreFutureMigrations and ignoreMissingMigrations settings are ignored. Patterns are comma separated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FLYWAY_IGNORE_MIGRATION_PATTERNS+++[] | list of string |  |

<a id="duration-note-anchor-quarkus-flyway_quarkus-flyway"></a>

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

<a id="developing-with-flyway"></a>
## Developing with Flyway

The following is an example for the `application.properties` file:

```properties
# configure your datasource
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=sarah
quarkus.datasource.password=connor
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mydatabase

# Run Flyway migrations automatically
quarkus.flyway.migrate-at-start=true

# More Flyway configuration options
# quarkus.flyway.baseline-on-migrate=true
# quarkus.flyway.baseline-version=1.0.0
# quarkus.flyway.baseline-description=Initial version
# quarkus.flyway.connect-retries=10
# quarkus.flyway.schemas=TEST_SCHEMA
# quarkus.flyway.table=flyway_quarkus_history
# quarkus.flyway.locations=db/location1,db/location2
# quarkus.flyway.sql-migration-prefix=X
# quarkus.flyway.repeatable-sql-migration-prefix=K
```

Add a SQL migration to the default folder following the Flyway naming conventions: `src/main/resources/db/migration/V1.0.0__Quarkus.sql`

```sql
CREATE TABLE quarkus
(
  id   INT,
  name VARCHAR(20)
);
INSERT INTO quarkus(id, name)
VALUES (1, 'QUARKED');
```

Now you can start your application and Quarkus will run the Flyway's migrate method according to your config.

> [!NOTE]
> With `quarkus.flyway.migrate-at-start=true`, as in the example above, Quarkus will execute the Flyway migration as part of the [application startup](/guides/lifecycle/index.md).

```java
@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    Flyway flyway; <1>

    public void checkMigration() {
        // This will print 1.0.0
        System.out.println(flyway.info().current().getVersion().toString());
    }
}
```

1. Inject the Flyway object if you want to use it directly

> [!TIP]
> In dev mode Quarkus will automatically restart the application if any of the existing migration scripts get modified. If you want to take advantage of this while developing and testing new migration scripts, you will want to set `%dev.quarkus.flyway.clean-at-start=true`, so that Flyway actually runs the modified migration.

### Repairing the Flyway schema history table

There are different scenarios which may require *repairing* the Flyway schema history table. One such scenario is when a migration fails in a database which doesn't support transactional DDL statements.

In such situations the [Flyway repair command](https://flywaydb.org/documentation/command/repair) comes in handy. In Quarkus this can either be executed automatically before the migration by setting `quarkus.flyway.repair-at-start=true` or manually by injecting the `Flyway` object and calling `Flyway#repair()`.

<a id="multiple-datasources"></a>
## Multiple datasources

Flyway can be configured for multiple datasources. The Flyway properties are prefixed exactly the same way as the named datasources, for example:

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

# Flyway configuration for the default datasource
quarkus.flyway.schemas=DEFAULT_TEST_SCHEMA
quarkus.flyway.locations=db/default/location1,db/default/location2
quarkus.flyway.migrate-at-start=true

# Flyway configuration for the "users" datasource
quarkus.flyway.users.schemas=USERS_TEST_SCHEMA
quarkus.flyway.users.locations=db/users/location1,db/users/location2
quarkus.flyway.users.migrate-at-start=true

# Flyway configuration for the "inventory" datasource
quarkus.flyway.inventory.schemas=INVENTORY_TEST_SCHEMA
quarkus.flyway.inventory.locations=db/inventory/location1,db/inventory/location2
quarkus.flyway.inventory.migrate-at-start=true
```

Notice there's an extra bit in the key. The syntax is as follows: `quarkus.flyway.[optional name.][datasource property]`.

> [!NOTE]
> Without configuration, Flyway is set up for every datasource using the default settings.

## Customizing Flyway

In cases where Flyway needs to be configured in addition to the configuration options that Quarkus provides, the `io.quarkus.flyway.FlywayConfigurationCustomizer` class comes in handy.

To customize Flyway for the default datasource, simply add a bean like so:

```java
@Singleton
public static class MyCustomizer implements FlywayConfigurationCustomizer {

    @Override
    public void customize(FluentConfiguration configuration) {
        // do something with configuration
    }
}
```

When named datasources are used, the `@FlywayDataSource` annotation can be used to specify the datasource to which the customizer applies. For example, if there are multiple datasources one of which is called `users` and customization of Flyway is needed for only that datasource, then the following code can be used:

```java
@Singleton
@FlywayDataSource("users")
public static class UsersCustomizer implements FlywayConfigurationCustomizer {

    @Override
    public void customize(FluentConfiguration configuration) {
        // do something with configuration
    }
}
```

## Using the Flyway object

In case you are interested in using the `Flyway` object directly, you can inject it as follows:

```java
@ApplicationScoped
public class MigrationService {
    // You can Inject the object if you want to use it manually
    @Inject
    Flyway flyway; <1>

    @Inject
    @FlywayDataSource("inventory") <2>
    Flyway flywayForInventory;

    @Inject
    @Named("flyway_users") <3>
    Flyway flywayForUsers;

    public void checkMigration() {
        // Use the flyway instance manually
        flyway.clean(); <4>
        flyway.migrate();
        // This will print 1.0.0
        System.out.println(flyway.info().current().getVersion().toString());
    }
}
```

1. Inject the Flyway object if you want to use it directly
2. Inject Flyway for named datasources using the Quarkus `FlywayDataSource` qualifier
3. Inject Flyway for named datasources
4. Use the Flyway instance directly

## Flyway and Hibernate ORM

When using Flyway together with Hibernate ORM, you can use the Dev UI to generate the initial schema creation script.

You can find more information about this feature in the [Hibernate ORM guide](/guides/hibernate-orm/index.md#flyway).

<a id="reactive-datasources"></a>
## Flyway and Reactive datasources

Flyway internally relies on a JDBC datasource, whereas reactive use cases will rely on [reactive SQL clients](/guides/reactive-sql-clients/index.md), either directly or through [Hibernate Reactive](/guides/hibernate-reactive/index.md). This is not a problem in Quarkus, because [a single configured datasource can be made available both through reactive clients and JDBC](/guides/datasource/index.md#jdbc-and-reactive-datasources-simultaneously).

To use Flyway on a datasource you otherwise access reactively, simply make sure to configure that datasource both as [JDBC](/guides/datasource/index.md#jdbc-datasource) and [reactive](/guides/datasource/index.md#reactive-datasource). This involves in particular adding dependencies to Quarkus extensions for both the JDBC driver and the reactive client, for instance `quarkus-jdbc-postgresql` **and** `quarkus-reactive-pg-client`.

## Flyway on Kubernetes

Sometimes, it's helpful not to execute Flyway initialization on each application startup. One such example is when deploying

on Kubernetes, where it doesn't make sense to execute Flyway on every single replica. Instead it's desirable to execute it once and then start the actual application without Flyway. To support this use case, when generating manifests for Kubernetes the generated manifests contain a Kubernetes initialization `Job` for Flyway. The `Job` performs initialization and the actual `Pod`, will starts once the `Job` is successfully completed.

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

## Known Issues in Flyway

### Oracle: Multiple schemas in Dev Services

When having multiple schemas in Oracle, you can use the `quarkus.flyway.schemas` property to specify the schemas that Flyway should manage. However, because this is executed in the DB as the `quarkus` user, you need to either give DBA privileges to the user that is executing the migration (`quarkus`) or perform the necessary DDLs before the Dev Service starts. This is done with the `quarkus.datasource.devservices.init-privileged-script-path` configuration.

#### Giving DBA privileges to the user

- In your application, create a SQL file named `001-devservice-init.sql` (for ordering purposes, it can be any name really) in your `src/main/resources` with the following content:

```sql
ALTER SESSION SET CONTAINER=quarkus;
GRANT DBA TO quarkus;
```

- Add the following configuration to your `application.properties`:

```properties
quarkus.datasource.devservices.init-privileged-script-path=001-devservice-init.sql
```
