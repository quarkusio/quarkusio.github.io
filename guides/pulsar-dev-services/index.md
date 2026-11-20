# Dev Services for Pulsar

With Quarkus Messaging Pulsar extension (`quarkus-messaging-pulsar`) Dev Services for Pulsar automatically starts a Pulsar broker in dev mode and when running tests. So, you don't have to start a broker manually. The application is configured automatically.

## Enabling / Disabling Dev Services for Pulsar

Dev Services for Pulsar is automatically enabled unless:

- `quarkus.pulsar.devservices.enabled` is set to `false`
- the `pulsar.client.serviceUrl` is configured
- all the Reactive Messaging Pulsar channels have the `serviceUrl` attribute set

Dev Services for Pulsar relies on Docker to start the broker. If your environment does not support Docker, you will need to start the broker manually, or connect to an already running broker. You can configure the broker address using `pulsar.client.`.

## Shared broker

Most of the time you need to share the broker between applications. Dev Services for Pulsar implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single broker.

> [!NOTE]
> Dev Services for Pulsar starts the container with the `quarkus-dev-service-pulsar` label which is used to identify the container.

If you need multiple (shared) brokers, you can configure the `quarkus.pulsar.devservices.service-name` attribute and indicate the broker name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `pulsar`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.pulsar.devservices.shared=false`.

## Setting the port

By default, Dev Services for Pulsar picks a random port and configures the application. You can set the port by configuring the `quarkus.pulsar.devservices.port` property.

Note that the Pulsar advertised address is automatically configured with the chosen port.

<a id="configuring-the-image"></a>
## Configuring the image

Dev Services for Pulsar supports the [official Apache Pulsar image](https://hub.docker.com/r/apachepulsar/pulsar).

A custom image name can be configured as such:

```properties
quarkus.pulsar.devservices.image-name=datastax/lunastreaming-all:2.10_4.7
```

## Configuring the Pulsar broker

You can configure the Dev Services for Pulsar with custom broker configuration.

The following example enables transaction support:

```properties
quarkus.pulsar.devservices.broker-config.transaction-coordinator-enabled=true
quarkus.pulsar.devservices.broker-config.system-topic-enabled=true
```

<a id="configuration-reference-devservices"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-enabled"></a> [`quarkus.pulsar.devservices.enabled`](#quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-enabled) config_property_copy_button:+++quarkus.pulsar.devservices.enabled+++[]<br><br>If Dev Services for Pulsar has been explicitly enabled or disabled. Dev Services are generally enabled by default, unless there is an existing configuration present. For Pulsar, Dev Services starts a broker unless `pulsar.client.serviceUrl` is set or if all the Reactive Messaging Pulsar channel are configured with `serviceUrl`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PULSAR_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-port"></a> [`quarkus.pulsar.devservices.port`](#quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-port) config_property_copy_button:+++quarkus.pulsar.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PULSAR_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-image-name"></a> [`quarkus.pulsar.devservices.image-name`](#quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-image-name) config_property_copy_button:+++quarkus.pulsar.devservices.image-name+++[]<br><br>The image to use. Note that only Apache Pulsar images are supported. Specifically, the image repository must end with `apachepulsar/pulsar`. Check <https://hub.docker.com/r/apachepulsar/pulsar> to find the available versions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PULSAR_DEVSERVICES_IMAGE_NAME+++[] | string | `docker.io/apachepulsar/pulsar:3.2.4` |
| [lock] <a id="quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-shared"></a> [`quarkus.pulsar.devservices.shared`](#quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-shared) config_property_copy_button:+++quarkus.pulsar.devservices.shared+++[]<br><br>Indicates if the Pulsar broker managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Pulsar starts a new container.<br><br>The discovery uses the `quarkus-dev-service-pulsar` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PULSAR_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-service-name"></a> [`quarkus.pulsar.devservices.service-name`](#quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-service-name) config_property_copy_button:+++quarkus.pulsar.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-pulsar` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Pulsar looks for a container with the `quarkus-dev-service-pulsar` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-pulsar` label set to the specified value.<br><br>This property is used when you need multiple shared Pulsar brokers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PULSAR_DEVSERVICES_SERVICE_NAME+++[] | string | `pulsar` |
| [lock] <a id="quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-broker-config-environment-variable-name"></a> [`quarkus.pulsar.devservices.broker-config."environment-variable-name"`](#quarkus-messaging-pulsar_quarkus-pulsar-devservices_quarkus-pulsar-devservices-broker-config-environment-variable-name) config_property_copy_button:+++quarkus.pulsar.devservices.broker-config."environment-variable-name"+++[]<br><br>Broker config to set on the Pulsar instance<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PULSAR_DEVSERVICES_BROKER_CONFIG__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
