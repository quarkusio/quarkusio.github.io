# Dev Services for databases

When testing or running in dev mode Quarkus can provide you with a zero-config database out of the box, a feature we refer to as Dev Services. Depending on your database type you may need Docker installed in order to use this feature. Dev Services is supported for the following databases:

- DB2 (container) (requires [license acceptance](#license_acceptance))
- H2 (in-process)
- MariaDB (container)
- Microsoft SQL Server (container) (requires [license acceptance](#license_acceptance))
- MySQL (container)
- Oracle Express Edition (container)
- PostgreSQL (container)

If you want to use Dev Services then all you need to do is include the relevant extension for the type of database you want (either reactive or JDBC, or both). Don't configure a database URL, username and password - Quarkus will provide the database and you can just start coding without worrying about config.

Production databases need to be configured as normal, so if you want to include a production database config in your `application.properties` and continue to use Dev Services we recommend that you use the `%prod.` profile to define your database settings.

## Enabling / Disabling Dev Services for Database

Dev Services for databases automatically starts a database server in dev mode and when running tests. So, you don't have to start a server manually. The application is configured automatically.

You can disable the automatic database start in `application.properties` via:

```properties
quarkus.devservices.enabled=false
# OR
quarkus.datasource.devservices.enabled=false
```

Dev Services for databases relies on Docker to start the server (except for H2 which is run in process). If your environment does not support Docker, you will need to start the server manually, or connect to an already running server.

<a id="license_acceptance"></a>
### Proprietary Databases - License Acceptance

If you are using a proprietary database such as DB2 or MSSQL you will need to accept the license agreement. To do this create a `src/main/resources/container-license-acceptance.txt` files in your project and add a line with the image name and tag of the database. By default, Quarkus uses the default image for the current version of Testcontainers, if you attempt to start Quarkus the resulting failure will tell you the exact image name in use for you to add to the file.

An example file is shown below:

**src/main/resources/container-license-acceptance.txt**

```
icr.io/db2_community/db2:12.1.5.0
mcr.microsoft.com/mssql/server:2025-latest
```

Alternatively, add these entries to your `application.properties`:

```properties
# For mssql
quarkus.datasource.devservices.container-env.ACCEPT_EULA=Y

# For db2
quarkus.datasource.devservices.container-env.LICENSE=accept
```

### Capturing Logs

By default, logs of the underlying database are not exposed. By capturing the logs, they become visible amongst other log statements:

```properties
quarkus.datasource.devservices.show-logs=true
```

## Automatic Image Selection

The container image used by Dev Services is determined by the database kind of your [datasource](/guides/datasource/index.md), which is implicit if you include only one kind of JDBC or Reactive SQL client extension in your project.

For certain databases, Quarkus can further refine the image selection based on specific database features required by your application. For example, with PostgreSQL:

- If you use Hibernate Spatial, Quarkus will automatically use a PostGIS-enabled image instead of the standard PostgreSQL image
- If you use Hibernate ORM vector types, Quarkus will automatically use a pgvector-enabled image

This automatic feature-based selection is transparent and requires no configuration. Extensions that require specific database capabilities will declare their requirements, and Dev Services will select the most appropriate image automatically.

Explicitly setting the image name always takes precedence over automatic selection:

```properties
quarkus.datasource.devservices.image-name=<custom-image>
```

<a id="reuse"></a>
## Reusing Dev Services

<a id="reuse-within-execution"></a>
### General case

Within a dev mode session or test suite execution, Quarkus will always reuse database Dev Services as long as their configuration (username, password, environment, port bindings, ...) did not change.

When the configuration of any database Dev Services changes, Quarkus will always restart all database Dev Services.

When a dev mode session or test suite execution ends, Quarkus will (by default) stop all database Dev Services.

<a id="reuse-across-executions"></a>
### Reusing Dev Service containers across runs

Assuming you rely on Dev Services based on containers (unlike H2), if you want to keep Dev Service containers running **after a dev mode session or test suite execution** to reuse them in the next dev mode session or test suite execution, this is possible as well. Just enable [TestContainers reuse](https://java.testcontainers.org/features/reuse/) by inserting this line in one of your [TestContainers configuration file](https://java.testcontainers.org/features/configuration/) (generally `~/.testcontainers.properties` or `C:/Users/myuser/.testcontainers.properties`):

```properties
testcontainers.reuse.enable=true
```

> [!NOTE]
> Even with container reuse enabled, containers will only be reused if their startup command did not change: same environment variables (username/password in particular), same port bindings, same volume mounts, ...

> [!WARNING]
> Reusing containers implies reusing their internal state, including the database schema and the content of tables.
>
> If that's not what you want -- and if your tests write to the database, that's probably not what you want -- consider [configuring Hibernate ORM appropriately](/guides/hibernate-orm/index.md#dev-mode), or using [Flyway](/guides/flyway/index.md) or [Liquibase](/guides/liquibase/index.md).

> [!WARNING]
> With container reuse enabled, old containers (especially with obsolete configuration) might be left running indefinitely, even after starting a new Quarkus dev mode session or test suite execution.
>
> In that case, you will need to stop and remove these containers manually.

If you want to reuse containers for some Quarkus applications but not all of them, or some Dev Services but not all of them, you can disable this feature for a specific Dev Service by setting the configuration property [`quarkus.datasource.devservices.reuse`/`quarkus.datasource."datasource-name".devservices.reuse`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-reuse) to `false`.

## Mapping volumes into Dev Services for Database

Mapping volumes from the Docker host's filesystem to the containers is handy to provide files like scripts or configuration, but also to preserve database data and reuse it after an application restart.

> [!NOTE]
> Mapping volumes will only work in Dev Services with a container-based database like PostgreSQL.

Dev Services volumes can be mapped to the filesystem or the classpath:

```properties
# Using a filesystem volume:
quarkus.datasource.devservices.volumes."/path/from"=/container/to <1>
# Using a classpath volume:
quarkus.datasource.devservices.volumes."classpath\:./file"=/container/to <2>
```

1. The file or folder "/path/from" from the local machine will be accessible at "/container/to" in the container.
2. When using classpath volumes, the location has to start with "classpath:". The file or folder "./file" from the project's classpath will be accessible at "/container/to" in the container.

> [!CAUTION]
> The colon character `:` needs to be escaped in `.properties` files as `\:`, or it will be interpreted as a key/value separator.

> [!IMPORTANT]
> when using a classpath volume, the container will only be granted read permission. On the other hand, when using a filesystem volume, the container will be granted read and write permission.

### Example of mapping volumes to persist the database data

Let's see an example using PostgreSQL where we'll map a file system volume to keep the database data permanently and use it:

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.devservices.volumes."/local/test/data"=/var/lib/postgresql/data
```

The appropriate in-container location varies depending on the database vendor. For PostgreSQL is "/var/lib/postgresql/data", but for MySQL, you would need this configuration instead:

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.devservices.volumes."/local/test/data"=/var/lib/mysql
```

When starting Dev Services (for example, in tests or in dev mode), you will see that the folder "/local/test/data" will be created at your file system and that will contain all the database data. When rerunning again the same Dev Services, this data will contain all the data you might have created beforehand.

> [!IMPORTANT]
> When using Dev Services with Hibernate ORM, by default Quarkus will wipe out the database on application startup, which will wipe out the database data on your Docker host's filesystem. Configure `quarkus.hibernate-orm.schema-management.strategy=none` or `quarkus.hibernate-orm.schema-management.strategy=validate` to avoid this behavior.
>
> Also, using Flyway to migrate your schema when starting the application will modify the database data on your Docker hosts's file system.

## Database Vendor Specific Configuration

All services based on containers are run using Testcontainers but Quarkus is not using the Testcontainers JDBC driver. Thus, even though extra JDBC URL properties can be set in your `application.properties` file, specific properties supported by the Testcontainers JDBC driver such as `TC_INITSCRIPT`, `TC_INITFUNCTION`, `TC_DAEMON`, `TC_TMPFS` are not supported.

Quarkus can support **specific** properties sent to the container itself though, e.g. this is the case for `TC_MY_CNF` which allows to override the MariaDB/MySQL configuration file.

Overriding the MariaDB/MySQL configuration would be done as follows:

```properties
quarkus.datasource.devservices.container-properties.TC_MY_CNF=testcontainers/mysql-conf
```

This support is database specific and needs to be implemented in each Dev Service specifically.

## Connect To Database Run as a Dev Service

You can connect to a database running as a Dev Service as you would do with any database running inside a Docker container.

Login credentials are the same for most databases, except when the database requirements don't allow it:

| Database | Username | Password | Database name |
| --- | --- | --- | --- |
| PostgreSQL, MariaDB, MySQL, IBM Db2, H2 | `quarkus` | `quarkus` | `quarkus` for the default datasource or name of the datasource |
| Microsoft SQL Server | `sa` | `Quarkus123` |  |

> [!NOTE]
> The Microsoft SQL Server Testcontainer doesn't support defining the username or database name. It also requires a strong password.

> [!TIP]
> For databases supporting it (i.e. all of them except Microsoft SQL Server for which it is only possible to override the password), you can override the database name, username and password used by the Dev Service.
>
> See [Configuration Reference](#configuration-reference) for more information.

Keep in mind that, except if configured otherwise (see below), a Dev Service runs on a random port. For instance, when you run PostgreSQL as a Dev Service and have `psql` installed on the host, you can connect via:

```bash
psql -h localhost -p <random port> -U quarkus
```

The random port can be found with `docker ps`

```bash
docker ps

# returns something like this:

CONTAINER ID   IMAGE           [..]    PORTS                                         [..]
b826e3a168c4   docker.io/library/postgres:18   [..]    0.0.0.0:49174->5432/tcp, :::49174->5432/tcp   [..] <1>
```

1. The random port is `49174`.

You can require a fixed port for a database Dev Service using:

```properties
quarkus.datasource.devservices.port=<your fixed port> <1>

quarkus.datasource."datasource-name".devservices.port=<your fixed port> <2>
```

1. Fixed port for the default datasource.
2. Fixed port for a named datasource.

> [!TIP]
> `docker ps` allows for more advanced retrieval of container information using the `--format` argument. For example, to get the running container ID, the image, the labels and the ports, the following command can be used:
>
> ```bash
> docker ps --format "table {{.ID}}\t{{.Image}}\t{{.Labels}}\t{{.Ports}}
> ```
>
> An example output using Dev Services for PostgreSQL is the following:
>
> ```bash
> CONTAINER ID   IMAGE          LABELS                                                                        PORTS
> a7034c91a392   docker.io/library/postgres:18     org.testcontainers.sessionId=xyz,datasource=default,org.testcontainers=true   0.0.0.0:49154->5432/tcp, :::49154->5432/tcp
> ```
>
> In the labels tab, we see that Quarkus added the datasource label, which can be very useful in differentiating containers when multiple Dev Services have been started.

<a id="Compose"></a>
## Compose

The Database Dev Services supports [Compose Dev Services](/guides/compose-dev-services/index.md). It relies on a `compose-devservices.yml`, such as:

```yaml
name: <application name>
services:
  postgresql:
    image: docker.io/library/postgres:18
    ports:
      - "5432"
    environment:
      POSTGRES_USER: quarkus
      POSTGRES_PASSWORD: quarkus
      POSTGRES_DB: quarkus
  oracle:
    image: docker.io/gvenzl/oracle-free:23-slim-faststart
    ports:
      - "1521"
    environment:
      ORACLE_PASSWORD: quarkus
      ORACLE_DATABASE: quarkus
      APP_USER: quarkus
      APP_USER_PASSWORD: quarkus
    labels:
      io.quarkus.devservices.compose.wait_for.logs: .*DATABASE IS READY TO USE.*
  mssql:
    image: mcr.microsoft.com/mssql/server:2025-latest
    ports:
      - "1433"
    environment:
      ACCEPT_EULA: "Y"
      MSSQL_SA_PASSWORD: Quarkus123
    labels:
      io.quarkus.devservices.compose.jdbc.parameters: trustServerCertificate=true
```

<a id="configuration-reference"></a>
## Configuration Reference

Dev Services for Databases support the following configuration options:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-enabled"></a> [`quarkus.datasource.devservices.enabled`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-enabled) config_property_copy_button:+++quarkus.datasource.devservices.enabled+++[]<br><br>`quarkus.datasource."datasource-name".devservices.enabled` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.enabled+++[]<br><br>Whether this Dev Service should start with the application in dev mode or tests.<br><br>Dev Services are enabled by default unless connection configuration (e.g. the JDBC URL or reactive client URL) is set explicitly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-image-name"></a> [`quarkus.datasource.devservices.image-name`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-image-name) config_property_copy_button:+++quarkus.datasource.devservices.image-name+++[]<br><br>`quarkus.datasource."datasource-name".devservices.image-name` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.image-name+++[]<br><br>The container image name for container-based Dev Service providers.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Defaults depend on the configured `datasource`:<br><br>- DB2: `icr.io/db2_community/db2:12.1.5.0`<br>- MariaDB: `docker.io/library/mariadb:12.1`<br>- Microsoft SQL Server: `{mssql-image}`\<br>On ARM (aarch64),`mcr.microsoft.com/azure-sql-edge:latest` is used instead, because the standard MS SQL Server image does not run on ARM.<br>- MySQL: `docker.io/library/mysql:9.5`<br>- Oracle Express Edition: `docker.io/gvenzl/oracle-free:23-slim-faststart`<br>- PostgreSQL: `docker.io/library/postgres:18`<br><br>For certain databases, the default image may be automatically adjusted based on required database features (e.g., spatial or vector support). Explicitly setting this property overrides the automatic image selection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-container-env-environment-variable-name"></a> [`quarkus.datasource.devservices.container-env."environment-variable-name"`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.datasource.devservices.container-env."environment-variable-name"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.container-env."environment-variable-name"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-container-properties-property-key"></a> [`quarkus.datasource.devservices.container-properties."property-key"`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-container-properties-property-key) config_property_copy_button:+++quarkus.datasource.devservices.container-properties."property-key"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.container-properties."property-key"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.container-properties."property-key"+++[]<br><br>Generic properties that are passed for additional container configuration.<br><br>Properties defined here are database-specific and are interpreted specifically in each database dev service implementation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_CONTAINER_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-properties-property-key"></a> [`quarkus.datasource.devservices.properties."property-key"`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-properties-property-key) config_property_copy_button:+++quarkus.datasource.devservices.properties."property-key"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.properties."property-key"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.properties."property-key"+++[]<br><br>Generic properties that are added to the database connection URL.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-port"></a> [`quarkus.datasource.devservices.port`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-port) config_property_copy_button:+++quarkus.datasource.devservices.port+++[]<br><br>`quarkus.datasource."datasource-name".devservices.port` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-command"></a> [`quarkus.datasource.devservices.command`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-command) config_property_copy_button:+++quarkus.datasource.devservices.command+++[]<br><br>`quarkus.datasource."datasource-name".devservices.command` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.command+++[]<br><br>The container start command to use for container-based Dev Service providers.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_COMMAND+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-db-name"></a> [`quarkus.datasource.devservices.db-name`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-db-name) config_property_copy_button:+++quarkus.datasource.devservices.db-name+++[]<br><br>`quarkus.datasource."datasource-name".devservices.db-name` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.db-name+++[]<br><br>The database name to use if this Dev Service supports overriding it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_DB_NAME+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-username"></a> [`quarkus.datasource.devservices.username`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-username) config_property_copy_button:+++quarkus.datasource.devservices.username+++[]<br><br>`quarkus.datasource."datasource-name".devservices.username` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.username+++[]<br><br>The username to use if this Dev Service supports overriding it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_USERNAME+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-password"></a> [`quarkus.datasource.devservices.password`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-password) config_property_copy_button:+++quarkus.datasource.devservices.password+++[]<br><br>`quarkus.datasource."datasource-name".devservices.password` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.password+++[]<br><br>The password to use if this Dev Service supports overriding it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_PASSWORD+++[] | string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-init-script-path"></a> [`quarkus.datasource.devservices.init-script-path`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-init-script-path) config_property_copy_button:+++quarkus.datasource.devservices.init-script-path+++[]<br><br>`quarkus.datasource."datasource-name".devservices.init-script-path` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.init-script-path+++[]<br><br>The paths to SQL scripts to be loaded from the classpath and applied to the Dev Service database.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_INIT_SCRIPT_PATH+++[] | list of string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-init-privileged-script-path"></a> [`quarkus.datasource.devservices.init-privileged-script-path`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-init-privileged-script-path) config_property_copy_button:+++quarkus.datasource.devservices.init-privileged-script-path+++[]<br><br>`quarkus.datasource."datasource-name".devservices.init-privileged-script-path` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.init-privileged-script-path+++[]<br><br>The paths to SQL scripts to be loaded from the classpath and applied to the Dev Service database using the SYS privileged user. Not all databases provide a privileged user. In these cases, the property is ignored. This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_INIT_PRIVILEGED_SCRIPT_PATH+++[] | list of string |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-volumes-host-path"></a> [`quarkus.datasource.devservices.volumes."host-path"`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-volumes-host-path) config_property_copy_button:+++quarkus.datasource.devservices.volumes."host-path"+++[]<br><br>`quarkus.datasource."datasource-name".devservices.volumes."host-path"` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.volumes."host-path"+++[]<br><br>The volumes to be mapped to the container.<br><br>The map key corresponds to the host location; the map value is the container location. If the host location starts with "classpath:", the mapping loads the resource from the classpath with read-only permission.<br><br>When using a file system location, the volume will be generated with read-write permission, potentially leading to data loss or modification in your file system.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_VOLUMES__HOST_PATH_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-reuse"></a> [`quarkus.datasource.devservices.reuse`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-reuse) config_property_copy_button:+++quarkus.datasource.devservices.reuse+++[]<br><br>`quarkus.datasource."datasource-name".devservices.reuse` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.reuse+++[]<br><br>Whether to keep Dev Service containers running **after a dev mode session or test suite execution** to reuse them in the next dev mode session or test suite execution.<br><br>Within a dev mode session or test suite execution, Quarkus will always reuse Dev Services as long as their configuration (username, password, environment, port bindings, ...) did not change. This feature is specifically about keeping containers running **when Quarkus is not running** to reuse them across runs.<br><br>> [!WARNING]<br>> This feature needs to be enabled explicitly in `testcontainers.properties`, may require changes to how you configure data initialization in dev mode and tests, and may leave containers running indefinitely, forcing you to stop and remove them manually. See [this section of the documentation](/guides/databases-dev-services/index.md#reuse) for more information.<br><br>This configuration property is set to `true` by default, so it is mostly useful to **disable** reuse, if you enabled it in `testcontainers.properties` but only want to use it for some of your Quarkus applications or datasources.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_REUSE+++[] | boolean | `true` |
| [lock] <a id="quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-show-logs"></a> [`quarkus.datasource.devservices.show-logs`](#quarkus-datasource_quarkus-datasource-devservices_quarkus-datasource-devservices-show-logs) config_property_copy_button:+++quarkus.datasource.devservices.show-logs+++[]<br><br>`quarkus.datasource."datasource-name".devservices.show-logs` config_property_copy_button:+++quarkus.datasource."datasource-name".devservices.show-logs+++[]<br><br>Whether the logs should be consumed by the JBoss logger.<br><br>This has no effect if the provider is not a container-based database, such as H2 or Derby.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_DATASOURCE_DEVSERVICES_SHOW_LOGS+++[] | boolean | `false` |
