# Dev Services for AMQP

Dev Services for AMQP automatically starts an AMQP 1.0 broker in dev mode and when running tests. So, you don't have to start a broker manually. The application is configured automatically.

## Enabling / Disabling Dev Services for AMQP

Dev Services for AMQP is automatically enabled unless:

- `quarkus.amqp.devservices.enabled` is set to `false`
- the `amqp-host` or `amqp-port` are configured
- all the Reactive Messaging AMQP channels have the `host` or `port` attributes set

Dev Services for AMQP relies on Docker to start the broker. If your environment does not support Docker, you will need to start the broker manually, or connect to an already running broker. You can configure the broker access using the `amqp-host`, `amqp-port`, `amqp-user` and `amqp-password` properties.

## Shared broker

Most of the time, you need to share the broker between applications. Dev Services for AMQP implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single broker.

> [!NOTE]
> Dev Services for AMQP starts the container with the `quarkus-dev-service-amqp` label which is used to identify the container.

If you need multiple (shared) brokers, you can configure the `quarkus.amqp.devservices.service-name` attribute and indicate the broker name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `amqp`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.amqp.devservices.shared=false`.

## Setting the port

By default, Dev Services for AMQP picks a random port and configures the application. You can set the port by configuring the `quarkus.amqp.devservices.port` property.

## Configuring the image

Dev Services for AMQP uses [activemq-artemis-broker](https://quay.io/repository/artemiscloud/activemq-artemis-broker) images. You can configure the image and version using the `quarkus.amqp.devservices.image-name` property:

```properties
quarkus.amqp.devservices.image-name=quay.io/arkmq-org/arkmq-org-broker:artemis.2.55.0
```

> [!IMPORTANT]
> The configured image must be *compatible* with the `activemq-artemis-broker` one. The container is launched with the `AMQ_USER`, `AMQ_PASSWORD` and `AMQ_EXTRA_ARGS` environment variables. The ports 5672 and 8161 (web console) are exposed.

<a id="Compose"></a>
## Compose

Dev Services for AMQP supports [Compose Dev Services](/guides/compose-dev-services/index.md). It relies on a `compose-devservices.yml`, such as:

```yaml
name: <application name>
services:
  artemis:
    image: quay.io/arkmq-org/arkmq-org-broker:artemis.2.55.0
    ports:
      - "5672"
      - "8161"
    environment:
      AMQ_USER: quarkus
      AMQ_PASSWORD: quarkus
      AMQ_EXTRA_ARGS: --no-autotune --mapped --no-fsync --relax-jolokia
```

<a id="configuration-reference-devservices"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-enabled"></a> [`quarkus.amqp.devservices.enabled`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-enabled) config_property_copy_button:+++quarkus.amqp.devservices.enabled+++[]<br><br>If Dev Services for AMQP has been explicitly enabled or disabled. Dev Services are generally enabled by default, unless there is an existing configuration present. For AMQP, Dev Services starts a broker unless `amqp-host` or `amqp-port` are set or if all the Reactive Messaging AMQP channel are configured with `host` or `port`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-port"></a> [`quarkus.amqp.devservices.port`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-port) config_property_copy_button:+++quarkus.amqp.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-image-name"></a> [`quarkus.amqp.devservices.image-name`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-image-name) config_property_copy_button:+++quarkus.amqp.devservices.image-name+++[]<br><br>The image to use. Note that only ActiveMQ Artemis images are supported. Specifically, the image repository must end with `artemiscloud/activemq-artemis-broker`.<br><br>Check the [activemq-artemis-broker on Quay page](https://quay.io/repository/artemiscloud/activemq-artemis-broker) to find the available versions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_IMAGE_NAME+++[] | string | `quay.io/arkmq-org/arkmq-org-broker:artemis.2.55.0` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-extra-args"></a> [`quarkus.amqp.devservices.extra-args`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-extra-args) config_property_copy_button:+++quarkus.amqp.devservices.extra-args+++[]<br><br>The value of the `AMQ_EXTRA_ARGS` environment variable to pass to the container. For ActiveMQ Artemis Broker \<= 1.0.21, set this property to `--no-autotune --mapped --no-fsync --relax-jolokia --http-host 0.0.0.0`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_EXTRA_ARGS+++[] | string | `--no-autotune --mapped --no-fsync --relax-jolokia` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-shared"></a> [`quarkus.amqp.devservices.shared`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-shared) config_property_copy_button:+++quarkus.amqp.devservices.shared+++[]<br><br>Indicates if the AMQP broker managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for AMQP starts a new container.<br><br>The discovery uses the `quarkus-dev-service-amqp` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-service-name"></a> [`quarkus.amqp.devservices.service-name`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-service-name) config_property_copy_button:+++quarkus.amqp.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-aqmp` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for AMQP looks for a container with the `quarkus-dev-service-amqp` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-amqp` label set to the specified value.<br><br>This property is used when you need multiple shared AMQP brokers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_SERVICE_NAME+++[] | string | `amqp` |
| [lock] <a id="quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-container-env-environment-variable-name"></a> [`quarkus.amqp.devservices.container-env."environment-variable-name"`](#quarkus-messaging-amqp_quarkus-amqp-devservices_quarkus-amqp-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.amqp.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_AMQP_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
