# Dev Services for MongoDB

Quarkus supports a feature called Dev Services that allows you to create various datasources without any config. In the case of MongoDB this support extends to the default MongoDB connection. What that means practically, is that if you have not configured `quarkus.mongodb.connection-string` nor `quarkus.mongodb.hosts`, Quarkus will automatically start a MongoDB container when running tests or in dev mode, and automatically configure the connection.

MongoDB Dev Services is based on [Testcontainers MongoDB module](https://www.testcontainers.org/modules/databases/mongodb/) that will start a single node replicaset.

When running the production version of the application, the MongoDB connection need to be configured as normal, so if you want to include a production database config in your `application.properties` and continue to use Dev Services we recommend that you use the `%prod.` profile to define your MongoDB settings.

## Shared server

Most of the time you need to share the server between applications. Dev Services for MongoDB implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single server.

> [!NOTE]
> Dev Services for MongoDB starts the container with the `quarkus-dev-service-mongodb` label which is used to identify the container.

If you need multiple (shared) servers, you can configure the `quarkus.mongodb.devservices.service-name` attribute and indicate the server name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `mongodb`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.mongodb.devservices.shared=false`.

<a id="Compose"></a>
## Compose

The MongoDB Dev Services supports [Compose Dev Services](/guides/compose-dev-services/index.md). It relies on a `compose-devservices.yml`, such as:

```yaml
name: <application name>
services:
  mongo:
    image: docker.io/library/mongo:7.0
    ports:
      - "27017"
```

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-enabled"></a> [`quarkus.mongodb.devservices.enabled`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-enabled) config_property_copy_button:+++quarkus.mongodb.devservices.enabled+++[]<br><br>If DevServices has been explicitly enabled or disabled. DevServices is generally enabled by default, unless there is an existing configuration present.<br><br>When DevServices is enabled Quarkus will attempt to automatically configure and start a database when running in Dev or Test mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-image-name"></a> [`quarkus.mongodb.devservices.image-name`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-image-name) config_property_copy_button:+++quarkus.mongodb.devservices.image-name+++[]<br><br>The container image name to use, for container based DevServices providers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_IMAGE_NAME+++[] | string | `docker.io/library/mongo:7.0` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-port"></a> [`quarkus.mongodb.devservices.port`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-port) config_property_copy_button:+++quarkus.mongodb.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-properties-property-key"></a> [`quarkus.mongodb.devservices.properties."property-key"`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-properties-property-key) config_property_copy_button:+++quarkus.mongodb.devservices.properties."property-key"+++[]<br><br>Generic properties that are added to the connection URL.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_PROPERTIES__PROPERTY_KEY_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-container-env-environment-variable-name"></a> [`quarkus.mongodb.devservices.container-env."environment-variable-name"`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.mongodb.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-shared"></a> [`quarkus.mongodb.devservices.shared`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-shared) config_property_copy_button:+++quarkus.mongodb.devservices.shared+++[]<br><br>Indicates if the MongoDB server managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for MongoDB starts a new container.<br><br>The discovery uses the `quarkus-dev-service-mongodb` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-service-name"></a> [`quarkus.mongodb.devservices.service-name`](#quarkus-mongodb-client_quarkus-mongodb-devservices_quarkus-mongodb-devservices-service-name) config_property_copy_button:+++quarkus.mongodb.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-mongodb` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for MongoDB looks for a container with the `quarkus-dev-service-mongodb` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise it starts a new container with the `quarkus-dev-service-mongodb` label set to the specified value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MONGODB_DEVSERVICES_SERVICE_NAME+++[] | string | `mongodb` |
