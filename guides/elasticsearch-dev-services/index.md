# Dev Services for Elasticsearch

If any Elasticsearch-related extension is present (e.g. `quarkus-elasticsearch-rest-client` or `quarkus-hibernate-search-orm-elasticsearch`), Dev Services for Elasticsearch automatically starts an Elasticsearch server in dev mode and when running tests. So, you don't have to start a server manually. The application is configured automatically.

## Enabling / Disabling Dev Services for Elasticsearch

Dev Services for Elasticsearch is automatically enabled unless:

- `quarkus.devservices.enabled` is set to `false`.
- `quarkus.elasticsearch.devservices.enabled` is set to `false`
- the hosts property is configured, depending on the extension used it can be:
- `quarkus.elasticsearch.hosts`
- `quarkus.hibernate-search-orm.elasticsearch.hosts`

Dev Services for Elasticsearch relies on Docker to start the server. If your environment does not support Docker, you will need to start the server manually, or connect to an already running server.

## Shared Elasticsearch

Sometimes you need to share the server between applications. Dev Services for Elasticsearch implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single server.

> [!NOTE]
> Dev Services for Elasticsearch starts the container with the `quarkus-dev-service-elasticsearch` label which is used to identify the container.

If you need multiple (shared) servers, you can configure the `quarkus.elasticsearch.devservices.service-name` attribute and indicate the server name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `elasticsearch`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.elasticsearch.devservices.shared=false`.

## Setting the port

By default, Dev Services for Elasticsearch picks a random port and configures the application. You can set the port by configuring the `quarkus.elasticsearch.devservices.port` property.

Note that the Elasticsearch hosts property is automatically configured with the chosen port.

## Configuring the image

Dev Services for Elasticsearch support distributions based on both Elasticsearch and OpenSearch images.

When using [Hibernate Search](/guides/hibernate-search-orm-elasticsearch/index.md), Dev Services will default to Elasticsearch or OpenSearch based on Hibernate Search configuration.

Otherwise, Dev Services will default to Elasticsearch. To use OpenSearch, configure the distribution explicitly:

```properties
quarkus.elasticsearch.devservices.distribution=opensearch
```

If you need to use a different Elasticsearch or OpenSearch image than the default one you can configure it via:

```properties
quarkus.elasticsearch.devservices.image-name=docker.io/elastic/elasticsearch:9.4.1
```

For exotic image names, Quarkus might be unable to infer the distribution (`elastic` or `opensearch`). In these cases starting the Dev Services will fail, and you will need to configure the distribution explicitly:

```properties
quarkus.elasticsearch.devservices.image-name=my-custom-image-with-no-clue-about-the-distribution:1.0
quarkus.elasticsearch.devservices.distribution=elasticsearch
```

<a id="reuse"></a>
## Reusing Dev Services

<a id="reuse-within-execution"></a>
### General case

Within a dev mode session or test suite execution, Quarkus will always reuse Elasticsearch Dev Services as long as their configuration (username, password, environment, port bindings, ...) did not change.

When the configuration of Elasticsearch Dev Services changes, Quarkus will always restart the corresponding containers.

When a dev mode session or test suite execution ends, Quarkus will (by default) stop Elasticsearch Dev Services.

<a id="reuse-across-executions"></a>
### Reusing Dev Service containers across runs

If you want to keep Dev Service containers running **after a dev mode session or test suite execution** to reuse them in the next dev mode session or test suite execution, this is possible as well. Just enable [TestContainers reuse](https://java.testcontainers.org/features/reuse/) by inserting this line in one of your [TestContainers configuration file](https://java.testcontainers.org/features/configuration/) (generally `~/.testcontainers.properties` or `C:/Users/myuser/.testcontainers.properties`):

```properties
testcontainers.reuse.enable=true
```

> [!NOTE]
> Even with container reuse enabled, containers will only be reused if their startup command did not change: same environment variables (username/password in particular), same port bindings, ...

> [!WARNING]
> Reusing containers implies reusing their internal state, including the Elasticsearch schema and the content of indexes.
>
> If that's not what you want -- and if your tests write to the indexes, that's probably not what you want -- consider reinitializing your schema and data on application startup. If you use Hibernate Search, [Hibernate Search's schema management](/guides/hibernate-search-orm-elasticsearch/index.md#quarkus-hibernate-search-orm-elasticsearch_quarkus-hibernate-search-orm-schema-management-strategy) may help with that.

> [!WARNING]
> With container reuse enabled, old containers (especially with obsolete configuration) might be left running indefinitely, even after starting a new Quarkus dev mode session or test suite execution.
>
> In that case, you will need to stop and remove these containers manually.

If you want to reuse containers for some Quarkus applications but not all of them, or some Dev Services but not all of them, you can disable this feature for a specific Dev Service by setting the configuration property [`quarkus.elasticsearch.devservices.reuse`](/guides/elasticsearch-dev-services/index.md#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-reuse) to `false`.

## Current limitations

Currently, only the default backend for Hibernate Search Elasticsearch is supported, because Dev Services for Elasticsearch can only start one Elasticsearch container.

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-enabled"></a> [`quarkus.elasticsearch.devservices.enabled`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-enabled) config_property_copy_button:+++quarkus.elasticsearch.devservices.enabled+++[]<br><br>Whether this Dev Service should start with the application in dev mode or tests.<br><br>Dev Services are enabled by default unless connection configuration (e.g. `quarkus.elasticsearch.hosts`) is set explicitly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-port"></a> [`quarkus.elasticsearch.devservices.port`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-port) config_property_copy_button:+++quarkus.elasticsearch.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-distribution"></a> [`quarkus.elasticsearch.devservices.distribution`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-distribution) config_property_copy_button:+++quarkus.elasticsearch.devservices.distribution+++[]<br><br>The Elasticsearch distribution to use.<br><br>Defaults to a distribution inferred from the explicitly configured `image-name` (if any), or by default to the distribution configured in depending extensions (e.g. Hibernate Search), or by default to `elastic`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_DISTRIBUTION+++[] | `elastic`, `opensearch` |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-image-name"></a> [`quarkus.elasticsearch.devservices.image-name`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-image-name) config_property_copy_button:+++quarkus.elasticsearch.devservices.image-name+++[]<br><br>The Elasticsearch container image to use.<br><br>Defaults depend on the configured `distribution`:<br><br>- For the `elastic` distribution: `docker.io/elastic/elasticsearch:9.4.1`<br>- For the `opensearch` distribution: `docker.io/opensearchproject/opensearch:3.6.0`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_IMAGE_NAME+++[] | string |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-java-opts"></a> [`quarkus.elasticsearch.devservices.java-opts`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-java-opts) config_property_copy_button:+++quarkus.elasticsearch.devservices.java-opts+++[]<br><br>The value for the ES_JAVA_OPTS env variable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_JAVA_OPTS+++[] | string | `-Xms512m -Xmx1g` |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-shared"></a> [`quarkus.elasticsearch.devservices.shared`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-shared) config_property_copy_button:+++quarkus.elasticsearch.devservices.shared+++[]<br><br>Whether the Elasticsearch server managed by Quarkus Dev Services is shared.<br><br>When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Elasticsearch starts a new container.<br><br>The discovery uses the `quarkus-dev-service-elasticsearch` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-service-name"></a> [`quarkus.elasticsearch.devservices.service-name`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-service-name) config_property_copy_button:+++quarkus.elasticsearch.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-elasticsearch` label attached to the started container.<br><br>This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Elasticsearch looks for a container with the `quarkus-dev-service-elasticsearch` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it starts a new container with the `quarkus-dev-service-elasticsearch` label set to the specified value.<br><br>This property is used when you need multiple shared Elasticsearch servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_SERVICE_NAME+++[] | string | `elasticsearch` |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-container-env-environment-variable-name"></a> [`quarkus.elasticsearch.devservices.container-env."environment-variable-name"`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.elasticsearch.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-reuse"></a> [`quarkus.elasticsearch.devservices.reuse`](#quarkus-elasticsearch-rest-client_quarkus-elasticsearch-devservices_quarkus-elasticsearch-devservices-reuse) config_property_copy_button:+++quarkus.elasticsearch.devservices.reuse+++[]<br><br>Whether to keep Dev Service containers running **after a dev mode session or test suite execution** to reuse them in the next dev mode session or test suite execution.<br><br>Within a dev mode session or test suite execution, Quarkus will always reuse Dev Services as long as their configuration (username, password, environment, port bindings, ...) did not change. This feature is specifically about keeping containers running **when Quarkus is not running** to reuse them across runs.<br><br>> [!WARNING]<br>> This feature needs to be enabled explicitly in `testcontainers.properties`, may require changes to how you configure data initialization in dev mode and tests, and may leave containers running indefinitely, forcing you to stop and remove them manually. See [this section of the documentation](/guides/elasticsearch-dev-services/index.md#reuse) for more information.<br><br>This configuration property is set to `true` by default, so it is mostly useful to **disable** reuse, if you enabled it in `testcontainers.properties` but only want to use it for some of your Quarkus applications.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ELASTICSEARCH_DEVSERVICES_REUSE+++[] | boolean | `true` |
