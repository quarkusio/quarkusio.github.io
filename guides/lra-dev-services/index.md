# Dev Services for LRA

If the Narayana LRA extension is present (`quarkus-narayana-lra`), the Dev Services for Narayana LRA coordinator automatically starts the Narayana LRA coordinator in dev mode and when running tests. So, you don't have to start it manually. The application is also configured automatically to connect to the coordinator and to make possible for the coordinator to call back the application.

> [!WARNING]
> Because the LRA coordinator needs to call back your application from the container, we need to expose the application on any host address, not just `localhost`. So the Dev Services for LRA coordinator injects `quarkus.http.host=0.0.0.0` into your application configuration which means that your application is accessible from any machine running in the same network as your development machine. **This is a potential security risk**, so use the LRA Dev Services only if you are sure that it is safe.

## Enabling / Disabling Dev Services for LRA

Dev Services for LRA is automatically enabled unless:

- `quarkus.lra.devservices.enabled` is set to `false`
- the `quarkus.lra.coordinator-url` is configured

Dev Services for LRA relies on Docker or Podman to start the coordinator. If your environment does not support Docker, you will need to start the coordinator manually, or connect to an already running coordinator. You can configure the coordinator URL using `quarkus.lra.coordinator-url`.

## Sharing LRA coordinator

Most likely you need to share the LRA coordinator between applications. The Dev Services for LRA coordinator implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single coordinator.

> [!NOTE]
> Dev Services for LRA starts the container with the `quarkus-dev-service-lra-coordinator` label which is used to identify the container.

If you need multiple (shared) coordinators, you can configure the `quarkus.lra.devservices.service-name` attribute and indicate the coordinator name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `lra-coordinator`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.lra.devservices.shared=false`.

## Setting the port

By default, Dev Services for LRA picks a random port on which to run the LRA coordinator. You can set the port by configuring the `quarkus.lra.devservices.port` property. The connection property `quarkus.lra.coordinator-url` is automatically configured with the chosen port.

<a id="configuring-the-image"></a>
## Configuring the image

The default image for the LRA coordinator is `quay.io/jbosstm/lra-coordinator`. You can configure the image used by Dev Services for LRA coordinator using the `quarkus.lra.devservices.image-name` property. You can browse the available images at <https://quay.io/repository/jbosstm/lra-coordinator?tab=tags>.

<a id="Compose"></a>
## Compose

The LRA Dev Services supports [Compose Dev Services](/guides/compose-dev-services/index.md). It relies on a `compose-devservices.yml`, such as:

```yaml
name: <application name>
services:
  narayana-lra:
    image: quay.io/jbosstm/lra-coordinator:latest
    ports:
      - "8080"
```

<a id="configuration-reference-devservices"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-enabled"></a> [`quarkus.lra.devservices.enabled`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-enabled) config_property_copy_button:+++quarkus.lra.devservices.enabled+++[]<br><br>If Dev Services for the LRA coordinator has been explicitly enabled or disabled. For the LRA coordinator, the Dev Services is disabled if this property is false or if the `quarkus.lra.coordinator-url` configuration property is defined.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-port"></a> [`quarkus.lra.devservices.port`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-port) config_property_copy_button:+++quarkus.lra.devservices.port+++[]<br><br>Optional fixed port the Dev Services will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-image-name"></a> [`quarkus.lra.devservices.image-name`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-image-name) config_property_copy_button:+++quarkus.lra.devservices.image-name+++[]<br><br>Optional override of the LRA coordinator container image to use.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_IMAGE_NAME+++[] | string | `quay.io/jbosstm/lra-coordinator:latest` |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-shared"></a> [`quarkus.lra.devservices.shared`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-shared) config_property_copy_button:+++quarkus.lra.devservices.shared+++[]<br><br>Indicates if the LRA coordinator managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for LRA coordinator starts a new container.<br><br>The discovery uses the `quarkus-dev-service-lra-coordinator` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-service-name"></a> [`quarkus.lra.devservices.service-name`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-service-name) config_property_copy_button:+++quarkus.lra.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-lra-coordinator` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for LRA coordinator looks for a container with the `quarkus-dev-service-lra-coordinator` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-lra-coordinator` label set to the specified value.<br><br>This property is used when you need multiple shared LRA coordinators.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_SERVICE_NAME+++[] | string | `lra-coordinator` |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-container-env-environment-variable-name"></a> [`quarkus.lra.devservices.container-env."environment-variable-name"`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.lra.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-log-warning"></a> [`quarkus.lra.devservices.log-warning`](#quarkus-narayana-lra_quarkus-lra-devservices_quarkus-lra-devservices-log-warning) config_property_copy_button:+++quarkus.lra.devservices.log-warning+++[]<br><br>Whether to log the warning messages about the LRA Dev Services.<br><br>Defaults to `true`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LRA_DEVSERVICES_LOG_WARNING+++[] | boolean | `true` |
