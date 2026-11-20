# Dev Services for RabbitMQ

Dev Services for RabbitMQ automatically starts a RabbitMQ broker in dev mode and when running tests. So, you don't have to start a broker manually. The application is configured automatically.

## Enabling / disabling Dev Services for RabbitMQ

Dev Services for RabbitMQ is automatically enabled unless:

- `quarkus.rabbitmq.devservices.enabled` is set to `false`
- the `rabbitmq-host` or `rabbitmq-port` is configured
- all the Reactive Messaging RabbitMQ channels have the `host` or `port` attributes set

Dev Services for RabbitMQ relies on Docker to start the broker. If your environment does not support Docker, you must start the broker manually, or connect to an already running broker. You can configure the broker access by using the `rabbitmq-host`, `rabbitmq-port`, `rabbitmq-username` and `rabbitmq-password` properties.

## Shared broker

Most of the time you want to share the broker between applications. Dev Services for RabbitMQ implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single broker.

> [!NOTE]
> Dev Services for RabbitMQ starts the container with the `quarkus-dev-service-rabbitmq` label, which is used to identify the container.

If you need multiple (shared) brokers, you can configure the `quarkus.rabbitmq.devservices.service-name` attribute and indicate the broker name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `rabbitmq`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.rabbitmq.devservices.shared=false`.

<a id="setting_port"></a>
## Setting the port

By default, Dev Services for RabbitMQ picks a random port and configures the application. You can set the port by configuring the `quarkus.rabbitmq.devservices.port` property.

## Configuring the image

Dev Services for RabbitMQ uses official images available at [https://hub.docker.com/\_/rabbitmq](https://hub.docker.com/_/rabbitmq). You can configure the image and version with the `quarkus.rabbitmq.devservices.image-name` property:

```properties
quarkus.rabbitmq.devservices.image-name=docker.io/library/rabbitmq:3.12-management
```

## Access the management UI

By default, Dev Services for RabbitMQ use the official image with the `management` tag. This means you have the [management plugin](https://github.com/docker-library/docs/tree/master/rabbitmq#management-plugin) available. You can use the [Dev UI](/guides/dev-ui/index.md) to find the HTTP port randomly affected or configure a static one by using `quarkus.rabbitmq.devservices.http-port`.

## Predefined topology

Dev Services for RabbitMQ supports defining topology upon broker start. You can define Virtual Hosts, Exchanges, Queues, and Bindings through standard Quarkus configuration.

### Defining virtual hosts

RabbitMQ uses a default virtual host of `/`. To define additional RabbitMQ virtual hosts, provide the names of the virtual hosts in the `quarkus.rabbitmq.devservices.vhosts` key:

```properties
quarkus.rabbitmq.devservices.vhosts=my-vhost-1,my-vhost-2
```

### Defining exchanges

To define a RabbitMQ exchange you provide the exchange's name after the `quarkus.rabbitmq.devservices.exchanges` key, followed by one (or more) of the exchange's properties:

```properties
quarkus.rabbitmq.devservices.exchanges.my-exchange.type=topic            # defaults to 'direct'
quarkus.rabbitmq.devservices.exchanges.my-exchange.auto-delete=false     # defaults to 'false'
quarkus.rabbitmq.devservices.exchanges.my-exchange.durable=true          # defaults to 'false'
quarkus.rabbitmq.devservices.exchanges.my-exchange.vhost=my-vhost        # defaults to '/'
```

Additionally, any additional arguments can be provided to the exchange's definition by using the `arguments` key:

```properties
quarkus.rabbitmq.devservices.exchanges.my-exchange.arguments.alternate-exchange=another-exchange
```

### Defining queues

To define a RabbitMQ queue you provide the queue's name after the `quarkus.rabbitmq.devservices.queues` key, followed by one (or more) of the queue's properties:

```properties
quarkus.rabbitmq.devservices.queues.my-queue.auto-delete=false          # defaults to 'false'
quarkus.rabbitmq.devservices.queues.my-queue.durable=true               # defaults to 'false'
quarkus.rabbitmq.devservices.queues.my-queue.vhost=my-vhost             # defaults to '/'
```

Additionally, any additional arguments can be provided to the queue's definition by using the `arguments` key:

```properties
quarkus.rabbitmq.devservices.queues.my-queue.arguments.x-dead-letter-exchange=another-exchange
```

### Defining bindings

To define a RabbitMQ binding you provide the binding's name after the `quarkus.rabbitmq.devservices.bindings` key, followed by one (or more) of the binding's properties:

```properties
quarkus.rabbitmq.devservices.bindings.a-binding.source=my-exchange      # defaults to name of binding
quarkus.rabbitmq.devservices.bindings.a-binding.routing-key=some-key    # defaults to '#'
quarkus.rabbitmq.devservices.bindings.a-binding.destination=my-queue    # defaults to name of binding
quarkus.rabbitmq.devservices.bindings.a-binding.destination-type=queue  # defaults to 'queue'
quarkus.rabbitmq.devservices.bindings.a-binding.vhost=my-vhost          # defaults to '/'
```

> [!NOTE]
> The name of the binding is only used for the purposes of the Dev Services configuration and is not part of the binding defined in RabbitMQ.

Additionally, any additional arguments can be provided to the binding's definition by using the `arguments` key:

```properties
quarkus.rabbitmq.devservices.bindings.a-binding.arguments.non-std-option=value
```

<a id="configuration-reference-devservices"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-enabled"></a> [`quarkus.rabbitmq.devservices.enabled`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-enabled) config_property_copy_button:+++quarkus.rabbitmq.devservices.enabled+++[]<br><br>If Dev Services for RabbitMQ has been explicitly enabled or disabled. Dev Services are generally enabled by default, unless there is an existing configuration present. For RabbitMQ, Dev Services starts a broker unless `rabbitmq-host` or `rabbitmq-port` are set or if all the Reactive Messaging RabbitMQ channel are configured with `host` or `port`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_ENABLED+++[] | boolean |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-port"></a> [`quarkus.rabbitmq.devservices.port`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-port) config_property_copy_button:+++quarkus.rabbitmq.devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-http-port"></a> [`quarkus.rabbitmq.devservices.http-port`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-http-port) config_property_copy_button:+++quarkus.rabbitmq.devservices.http-port+++[]<br><br>Optional fixed port for the RabbitMQ management plugin.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_HTTP_PORT+++[] | int |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-image-name"></a> [`quarkus.rabbitmq.devservices.image-name`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-image-name) config_property_copy_button:+++quarkus.rabbitmq.devservices.image-name+++[]<br><br>The image to use. Note that only official RabbitMQ images are supported. Specifically, the image repository must end with `rabbitmq`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_IMAGE_NAME+++[] | string | `docker.io/library/rabbitmq:3.12-management` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-shared"></a> [`quarkus.rabbitmq.devservices.shared`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-shared) config_property_copy_button:+++quarkus.rabbitmq.devservices.shared+++[]<br><br>Indicates if the RabbitMQ broker managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for RabbitMQ starts a new container.<br><br>The discovery uses the `quarkus-dev-service-rabbitmq` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-service-name"></a> [`quarkus.rabbitmq.devservices.service-name`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-service-name) config_property_copy_button:+++quarkus.rabbitmq.devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-rabbitmq` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for RabbitMQ looks for a container with the `quarkus-dev-service-rabbitmq` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-rabbitmq` label set to the specified value.<br><br>This property is used when you need multiple shared RabbitMQ brokers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_SERVICE_NAME+++[] | string | `rabbitmq` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-type"></a> [`quarkus.rabbitmq.devservices.exchanges."exchange-name".type`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-type) config_property_copy_button:+++quarkus.rabbitmq.devservices.exchanges."exchange-name".type+++[]<br><br>Type of exchange: direct, topic, headers, fanout, etc.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_EXCHANGES__EXCHANGE_NAME__TYPE+++[] | string | `direct` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-auto-delete"></a> [`quarkus.rabbitmq.devservices.exchanges."exchange-name".auto-delete`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-auto-delete) config_property_copy_button:+++quarkus.rabbitmq.devservices.exchanges."exchange-name".auto-delete+++[]<br><br>Should the exchange be deleted when all queues are finished using it?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_EXCHANGES__EXCHANGE_NAME__AUTO_DELETE+++[] | boolean | `false` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-durable"></a> [`quarkus.rabbitmq.devservices.exchanges."exchange-name".durable`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-durable) config_property_copy_button:+++quarkus.rabbitmq.devservices.exchanges."exchange-name".durable+++[]<br><br>Should the exchange remain after restarts?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_EXCHANGES__EXCHANGE_NAME__DURABLE+++[] | boolean | `false` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-vhost"></a> [`quarkus.rabbitmq.devservices.exchanges."exchange-name".vhost`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-vhost) config_property_copy_button:+++quarkus.rabbitmq.devservices.exchanges."exchange-name".vhost+++[]<br><br>What virtual host should the exchange be associated with?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_EXCHANGES__EXCHANGE_NAME__VHOST+++[] | string | `/` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-arguments-argument-name"></a> [`quarkus.rabbitmq.devservices.exchanges."exchange-name".arguments."argument-name"`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-exchanges-exchange-name-arguments-argument-name) config_property_copy_button:+++quarkus.rabbitmq.devservices.exchanges."exchange-name".arguments."argument-name"+++[]<br><br>Extra arguments for the exchange definition.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_EXCHANGES__EXCHANGE_NAME__ARGUMENTS__ARGUMENT_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-auto-delete"></a> [`quarkus.rabbitmq.devservices.queues."queue-name".auto-delete`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-auto-delete) config_property_copy_button:+++quarkus.rabbitmq.devservices.queues."queue-name".auto-delete+++[]<br><br>Should the queue be deleted when all consumers are finished using it?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_QUEUES__QUEUE_NAME__AUTO_DELETE+++[] | boolean | `false` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-durable"></a> [`quarkus.rabbitmq.devservices.queues."queue-name".durable`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-durable) config_property_copy_button:+++quarkus.rabbitmq.devservices.queues."queue-name".durable+++[]<br><br>Should the queue remain after restarts?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_QUEUES__QUEUE_NAME__DURABLE+++[] | boolean | `false` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-vhost"></a> [`quarkus.rabbitmq.devservices.queues."queue-name".vhost`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-vhost) config_property_copy_button:+++quarkus.rabbitmq.devservices.queues."queue-name".vhost+++[]<br><br>What virtual host should the queue be associated with?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_QUEUES__QUEUE_NAME__VHOST+++[] | string | `/` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-arguments-argument-name"></a> [`quarkus.rabbitmq.devservices.queues."queue-name".arguments."argument-name"`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-queues-queue-name-arguments-argument-name) config_property_copy_button:+++quarkus.rabbitmq.devservices.queues."queue-name".arguments."argument-name"+++[]<br><br>Extra arguments for the queue definition.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_QUEUES__QUEUE_NAME__ARGUMENTS__ARGUMENT_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-source"></a> [`quarkus.rabbitmq.devservices.bindings."binding-name".source`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-source) config_property_copy_button:+++quarkus.rabbitmq.devservices.bindings."binding-name".source+++[]<br><br>Source exchange to bind to. Defaults to name of binding instance.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_BINDINGS__BINDING_NAME__SOURCE+++[] | string |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-routing-key"></a> [`quarkus.rabbitmq.devservices.bindings."binding-name".routing-key`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-routing-key) config_property_copy_button:+++quarkus.rabbitmq.devservices.bindings."binding-name".routing-key+++[]<br><br>Routing key specification for the source exchange.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_BINDINGS__BINDING_NAME__ROUTING_KEY+++[] | string | `#` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-destination"></a> [`quarkus.rabbitmq.devservices.bindings."binding-name".destination`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-destination) config_property_copy_button:+++quarkus.rabbitmq.devservices.bindings."binding-name".destination+++[]<br><br>Destination exchange or queue to bind to. Defaults to name of binding instance.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_BINDINGS__BINDING_NAME__DESTINATION+++[] | string |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-destination-type"></a> [`quarkus.rabbitmq.devservices.bindings."binding-name".destination-type`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-destination-type) config_property_copy_button:+++quarkus.rabbitmq.devservices.bindings."binding-name".destination-type+++[]<br><br>Destination type for binding: queue, exchange, etc.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_BINDINGS__BINDING_NAME__DESTINATION_TYPE+++[] | string | `queue` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-vhost"></a> [`quarkus.rabbitmq.devservices.bindings."binding-name".vhost`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-vhost) config_property_copy_button:+++quarkus.rabbitmq.devservices.bindings."binding-name".vhost+++[]<br><br>What virtual host should the binding be associated with?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_BINDINGS__BINDING_NAME__VHOST+++[] | string | `/` |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-arguments-argument-name"></a> [`quarkus.rabbitmq.devservices.bindings."binding-name".arguments."argument-name"`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-bindings-binding-name-arguments-argument-name) config_property_copy_button:+++quarkus.rabbitmq.devservices.bindings."binding-name".arguments."argument-name"+++[]<br><br>Extra arguments for the binding definition.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_BINDINGS__BINDING_NAME__ARGUMENTS__ARGUMENT_NAME_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-vhosts"></a> [`quarkus.rabbitmq.devservices.vhosts`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-vhosts) config_property_copy_button:+++quarkus.rabbitmq.devservices.vhosts+++[]<br><br>Virtual hosts that should be predefined after starting the RabbitMQ broker.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_VHOSTS+++[] | list of string |  |
| [lock] <a id="quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-container-env-environment-variable-name"></a> [`quarkus.rabbitmq.devservices.container-env."environment-variable-name"`](#quarkus-messaging-rabbitmq_quarkus-rabbitmq-devservices_quarkus-rabbitmq-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.rabbitmq.devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_RABBITMQ_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
