# Compose Dev Services

Compose Dev Services offer a way to define custom dev services using the Compose specification.

[Quarkus Dev Services](/guides/dev-services/index.md) automatically provisions unconfigured services in development and test mode. Most extensions providing connectivity to services also provide dev services implementation using [Testcontainers](https://www.testcontainers.org/) behind the scenes. Each dev service has some default configuration, such as the container image to use, but is limited in terms of customization.

While this level of automation is great for most use cases, there are times when you need more control and coordination over provisioned services.

**Compose Dev Services** extends the Dev Services concept by allowing you to define custom dev services using the [Compose specification](https://compose-spec.io/). *The Compose Specification is a developer-focused standard for defining cloud and platform agnostic container-based applications*.

Compose is a widely used tool for defining and managing multi-container applications, for development and testing purposes. A YAML description file, typically named `compose.yml`, defines the services, networks, and volumes required for your application. [Docker Compose](https://docs.docker.com/compose/) is the reference implementation and [Podman Desktop](https://podman-desktop.io/docs/compose) also provides out-of-the-box support for the Compose specification.

Quarkus detects Compose files named `compose-devservices.yml` (or [Using specific Compose files](#compose-devservices-compose-files)) in your project and starts the defined services when your application runs in development or test mode. Extensions providing dev services discover these custom services and use them instead of creating default ones. When the dev mode or tests are stopped, the services are automatically stopped and cleaned up.

This integration provides a seamless development experience while giving you full control over your service configurations.

## Prerequisites

To use Compose Dev Services, you need:

1. A working **local** container environment: [Docker](https://docs.docker.com/get-started/get-docker/) or [Podman](https://podman.io/docs/installation)
2. The `docker` or `podman` CLI present in the `$PATH`
3. Compose tooling such as `docker-compose` or `podman-compose` (Note that `docker compose` and `podman compose` commands internally call these binaries):

- For Docker: [Installing Docker Compose](https://docs.docker.com/compose/install/)
- For Podman: [Setting up Compose](https://podman-desktop.io/docs/compose/setting-up-compose)

> [!IMPORTANT]
> Compose Dev Services is only compatible with Compose V2.

## Using Compose Dev Services

Let's see how to use Compose Dev Services with examples ranging from simple to more complex scenarios.

### Basic Example: PostgreSQL Database

In a Quarkus project already configured to use the `quarkus-jdbc-postgresql` extension, you can create a `compose-devservices.yml` file in the root of the project and define a custom service using Compose:

```yaml
services:
  db:
    image: docker.io/library/postgres:18
    healthcheck:
      test: pg_isready -U myuser -d mydb
      interval: 5s
      timeout: 3s
      retries: 3
    ports:
      - '5432'
    environment:
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
      POSTGRES_DB: mydb
```

When you run the application in dev mode or execute tests, Compose Dev Services will automatically start the PostgreSQL service defined in the `compose-devservices.yml` file, instead of using the default dev service provided by the `quarkus-jdbc-postgresql` extension.

As per the above configuration, the PostgreSQL container port `5432` will be exposed to a random host port and the application datasource will be configured by extracting connection information such as *user*, *password*, and the *database name*.

### Multi-Service Example: Custom Network and Dependencies

For more complex scenarios, you can define custom networks and service dependencies:

```yaml
services:
  db:
    image: docker.io/library/postgres:18
    ports:
      - '5432'
    environment:
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
      POSTGRES_DB: mydb
    networks:
      - backend
    volumes:
      - postgres-data:/var/lib/postgresql/data

  cache:
    image: docker.io/library/redis:7
    command: redis-server --save 60 1 --loglevel warning
    ports:
      - '6379'
    networks:
      - backend
    depends_on:
      - db

  message-broker:
    image: docker.io/library/rabbitmq:3.12-management
    ports:
      - '5672'
      - '15672'
    networks:
      - backend
      - frontend
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest

  api-gateway:
    image: nginx:latest
    ports:
      - '8080:80'
    networks:
      - frontend
    depends_on:
      - message-broker
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro

networks:
  backend:
  frontend:

volumes:
  postgres-data:
```

Note that the `compose-devservices.yml` file can be customized as needed with multiple services with dependencies to control the startup order, custom networks to isolate service communication, and volumes for data persistence and configuration.

## Service Discovery

Services started by Compose Dev Services are automatically discovered by extensions providing dev services. Each extension discovers the services it needs and configures the application accordingly.

Extensions rely on container image names and exposed ports to discover services. For example, the PostgreSQL Dev Service looks for a service with the image name **containing** `postgres` and the exposed container port `5432`.

When a match is found, the extension will:

1. Extract connection information from the container (connection URL, credentials, database name, etc.)
2. Configure the application to use the discovered service
3. Skip creating its own default Dev Service container

### Supported Extensions and Discovery Criteria

The following is the list of platform extensions with dev services support:

| Extension | Image Names | Exposed Port |
| --- | --- | --- |
| AMQP | amqp, activemq-artemis, amq-broker, rabbitmq | 5672 |
| Apicurio Registry | apicurio | 8080 |
| DB2 | db2 | 50000 |
| MySQL | mysql | 3306 |
| PostgreSQL | postgres | 5432 |
| MariaDB | mariadb | 3306 |
| Microsoft SQL Server | mssql | 1433 |
| Oracle Database | oracle | 1521 |
| Kafka | kafka, strimzi, redpanda | 9092 |
| Keycloak | keycloak | 8080 |
| Kubernetes | kube-apiserver, k3s, kindest/node | 6443 |
| LRA | lra-coordinator | 8080 |
| MongoDB | mongo | 27017 |
| MQTT | hivemq, eclipse-mosquitto | 1883 |
| RabbitMQ | rabbitmq | 5672 |
| Pulsar | pulsar | 6650 |
| Redis | redis | 6379 |
| Infinispan | infinispan, datagrid | 11222 |
| Elasticsearch | elasticsearch, opensearch | 9200 |
| Observability | lgtm | 16686, 9411, 9090, 3000 |

### Configuring custom images for service discovery

You can customize the image names used for service discovery by setting the appropriate properties in your `application.properties` file. For example, to use a custom Kafka image:

```properties
quarkus.kafka.devservices.image-name=my-custom-kafka:latest
```

Each extension that provides Dev Services has its own configuration properties for customizing the image name. Refer to the specific extension documentation for details.

<a id="compose-devservices-compose-files"></a>
### Using specific Compose files

By default, Compose Dev Services looks for files in the project root named `compose-devservices.[yml|yaml]` or `docker-compose-devservices.[yml|yaml]`.

You can specify custom files to use by setting the `quarkus.compose.devservices.files` property in the `application.properties` file:

```properties
# Use a specific compose file for all modes
quarkus.compose.devservices.files=src/main/docker/compose.yml

# Use different compose files for different config profiles
%dev.quarkus.compose.devservices.files=src/main/docker/dev-compose.yml
%test.quarkus.compose.devservices.files=src/test/resources/test-compose.yml
```

This allows you to use different dev service environments depending on the Quarkus config profiles. You can also specify multiple files by separating them with commas:

```properties
quarkus.compose.devservices.files=src/main/docker/base-compose.yml,src/main/docker/extra-services.yml
```

#### Using Compose profiles

With profiles, Compose files can define a set of active profiles so that started services are adjusted for various usages and environments. You can specify the profiles to activate by setting the `quarkus.compose.devservices.profiles` property in the `application.properties` file:

```yaml
services:
  db:
    image: docker.io/library/postgres:18
    profiles:
      - dev
      - local
    ports:
      - '5432'
    environment:
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
      POSTGRES_DB: mydb
```

```properties
%test.quarkus.compose.devservices.profiles=dev,local
```

### Ignoring Services

You can configure Compose Dev Services to not discover specific services by adding the `io.quarkus.devservices.compose.ignore` label to the service in your Compose file:

```yaml
services:
  db:
    image: docker.io/library/postgres:18
    labels:
      io.quarkus.devservices.compose.ignore: true
    ports:
      - '5432'
    environment:
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
      POSTGRES_DB: mydb
```

When a service has this label set to `true`, it will be excluded from service discovery, and extensions will not use it for Dev Services.

## Service Readiness

Compose Dev Services provides several ways to ensure services are ready before your application tries to use them.

### Health Checks

Compose Dev Services respects the health checks defined in your Compose file. It's recommended to configure health checks for your services to ensure they are ready before your application tries to use them:

```yaml
services:
  db:
    image: docker.io/library/postgres:18
    healthcheck:
      test: pg_isready -U myuser -d mydb
      interval: 5s
      timeout: 3s
      retries: 3
    ports:
      - '5432'
    environment:
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
      POSTGRES_DB: mydb
```

### Wait for Logs

You can configure Compose Dev Services to wait for specific log messages to appear in the container logs before considering a service ready. This is useful for services that don't provide health checks but log a message when they're ready.

To wait for a specific log message, add a label with the prefix `io.quarkus.devservices.compose.wait_for.logs` to your service:

```yaml
services:
  app:
    image: my-application:latest
    ports:
      - '8080'
    labels:
      io.quarkus.devservices.compose.wait_for.logs: .*Server is now running.*
```

You can also specify how many times a log message should appear by setting the numeric suffix value: `io.quarkus.devservices.compose.wait_for.logs.3: .\*Worker started.*`

### Wait for Ports

By default, Compose Dev Services waits for all exposed ports to be available before considering a service ready. This behavior can be customized using labels:

```yaml
services:
  app:
    image: my-application:latest
    ports:
      - '8080'
    labels:
      # Change to `true` to disable waiting for ports
      io.quarkus.devservices.compose.wait_for.ports.disable: false
      # Or set a custom timeout for port waiting
      io.quarkus.devservices.compose.wait_for.ports.timeout: "30s"
```

### Global Timeout

You can configure the global timeout for Dev Services startup using the `quarkus.devservices.timeout` property:

```properties
quarkus.devservices.timeout=120s
```

This property sets the maximum time to wait for all Dev Services to start. The default is 60 seconds.

### Ordering Services

Compose Dev Services starts services in the order they are defined in the Compose file. If you need to start services in a specific order, you can use the `depends_on` attribute:

```yaml
services:
  db:
    image: docker.io/library/postgres:18
    ports:
      - '5432'
    environment:
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: mypassword
      POSTGRES_DB: mydb

  app:
    image: my-application:latest
    ports:
      - '8080'
    depends_on:
      - db
```

## Exposing Service Configuration into the Application

Compose Dev Services automatically exposes the configuration of discovered services to your application. For example, when a database service is discovered with the following compose service description:

```yaml
services:
  db:
    image: docker.io/library/mysql:9.5
    ports:
      - '9906:3306'
    labels:
      io.quarkus.devservices.compose.jdbc.parameters: characterEncoding=UTF-8&useUnicode=true
    environment:
      MYSQL_USER: myuser
      MYSQL_PASSWORD: mypassword
      MYSQL_DATABASE: mydb
```

Following application properties are automatically set:

```properties
quarkus.datasource.username=myuser
quarkus.datasource.password=mypassword
quarkus.datasource.jdbc.url=jdbc:mysql://localhost:9906/mydb?characterEncoding=UTF-8&useUnicode=true
```

The actual values are extracted from the service labels and environment variables. The host is set to the Docker host IP address, and the port is set to the mapped port on the host.

> [!NOTE]
> For database services, if the label `io.quarkus.devservices.compose.jdbc.parameters` is set, the parameters are added to the JDBC URL.

### Custom config mapping using service labels

You can customize how environment variables and exposed ports are mapped to application configuration properties using service labels.

#### Mapping environment variables

To map an environment variable to a specific application configuration property, use the `io.quarkus.devservices.compose.config_map.env.<env-var-name>` label.

Let's take the example of an MQTT broker:

```yaml
services:
  mqtt:
    image: eclipse-mosquitto:2.0.21
    ports:
    - '1883'
    environment:
      MQTT_USER: user
      MQTT_PASSWORD: pass
    labels:
      io.quarkus.devservices.compose.config_map.env.MQTT_USER: mp.messaging.connector.smallrye-mqtt.username
      io.quarkus.devservices.compose.config_map.env.MQTT_PASSWORD: mp.messaging.connector.smallrye-mqtt.password
    volumes:
      - ./conf:/mosquitto/config
```

With the `./conf` containing the `mosquitto.conf` file:

```ini
persistence true
persistence_location /mosquitto/data/
log_dest file /mosquitto/log/mosquitto.log
listener 1883

allow_anonymous false
password_file /mosquitto/config/password.txt
```

and the `password.txt` file containing the user and encrypted password:

```
user:$7$101$EyGShytu3v+...==
```

In this example:

- The `MQTT_USER` environment variable with the value `user` is mapped to the `mp.messaging.connector.smallrye-mqtt.username` property
- The `MQTT_PASSWORD` environment variable with the value `pass` is mapped to the `mp.messaging.connector.smallrye-mqtt.password` property

> [!NOTE]
> With the `password_file` is configured in the `mosquitto.conf`, environment variables are not used to set the username and password but to map them to the application properties.

#### Mapping exposed ports

To map an exposed port to a specific application configuration property, use the `io.quarkus.devservices.compose.config_map.port.<container-port>` label:

```yaml
services:
  my-service:
    image: my-service-image
    ports:
      - '7432:5432'
      - '9080:8080'
    labels:
      # Map port 5432 to quarkus.datasource.jdbc.port
      io.quarkus.devservices.compose.config_map.port.5432: quarkus.datasource.jdbc.port
      # Map port 8080 to quarkus.http.port
      io.quarkus.devservices.compose.config_map.port.8080: quarkus.http.port
```

In the example above:

- Port 7432 is mapped to the `quarkus.datasource.jdbc.port` property
- Port 9080 is mapped to the `quarkus.http.port` property

The mapped properties will contain the host port that is mapped to the container port, which may be different from the container port if you're using dynamic port mapping (e.g., `- '5432'` instead of `- '7432:5432'`).

<a id="exposing-port-mappings-to-running-containers"></a>
#### Exposing port mappings to running containers

In some cases, containers need to know the host ports they're mapped to at runtime. For example, a Kafka broker needs to advertise its externally accessible address to clients.

You can use the `io.quarkus.devservices.compose.exposed_ports` label to specify a file path inside the container where port mappings will be written:

```yaml
services:
  kafka:
    image: apache/kafka-native:3.9.0
    ports:
      - '9092'
    labels:
      io.quarkus.devservices.compose.exposed_ports: /etc/kafka/docker/ports
```

When the container starts, Quarkus will copy a file at the specified path containing environment variable-style port mappings:

```bash
PORT_9092=54321
PORT_8080=12345
# ... other ports as needed
```

Each line follows the format `PORT_<containerPort>=<hostPort>`, where *containerPort* is the port exposed by the container and *hostPort* is the dynamically assigned port on the host. The container command can wait until the file is present, and then source this file to access these mappings as environment variables:

```bash
#!/bin/bash

# Wait for the ports file to be created
while [ ! -f /etc/kafka/docker/ports ]; do
  sleep 0.1;
done;

# Source the file to load PORT_* variables
source /etc/kafka/docker/ports;

# Use the port mappings
export KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:$PORT_9092;
# Run the Kafka broker executable
/etc/kafka/docker/run
```

## Controlling Service Lifecycle with Compose

Compose Dev Services provides several configuration options to control how services are started, stopped and shared between application instances.

When the application starts in development or test mode, Compose Dev Services decides whether to start services or to discover already running services based on the configuration. When the application stops, Compose Dev Services stops the services it started.

### Compose Project Name

Compose uses the project name to identify resources such as services, networks, volumes, etc. in order to namespace and isolate different compose projects. This enables the discovery of existing resources and cleanup resources when the application stops.

Compose Dev Services determines the project name as follows:

1. If the `quarkus.compose.devservices.project-name` property is set, it's used as the project name
2. If the top-level `name` attribute is specified in the Compose file, it's used as the project name
3. Otherwise, the default name `quarkus-devservices-<application-name>` is chosen.

### Discovering already started Compose Services

Once the project name is determined, Compose Dev Services first tries to discover existing services with that project name.

The already started compose project may have been started by another Quarkus application running locally with the same project name, or manually using the `docker compose up` or `podman compose up` commands.

Regardless of how the services were started, Compose Dev Services will configure dev services and injected configuration properties, but won't stop the services when the application is stopped.

> [!IMPORTANT]
> If no compose files are found in the project, and no `quarkus.compose.devservices.project-name` is set, Compose Dev Services won't try to discover any services and will be disabled.

### Compose Dev Services used for tests

For Quarkus tests, a generated project name in the format `quarkus-devservices-<application-name>-<random-suffix>` is used by default to ensure isolation between test runs and running dev mode services. If the top-level name attribute is specified in the Compose file, the project name in the format `<compose-name>-<random-suffix>` is used.

This way, Quarkus tests start a separate copy of the services defined in the compose files. For example, when running continuous testing in development mode, tests will have their own isolated set of services.

You can change this behavior by setting the `quarkus.compose.devservices.reuse-project-for-tests=true` property. This allows tests to reuse services started separately in development mode.

### Using Start/Stop Controls

In the default lifecycle, Compose Dev Services will start and stop services automatically when the application starts and stops. For more fine-grained control, you can use the `start-services` and `stop-services` configuration properties.

When `quarkus.compose.devservices.start-services=false` is set, Compose Dev Services will only try to discover existing services with the determined project name, but won't start any new services, even if compose files are present.

When `quarkus.compose.devservices.stop-services=false` is set, services will continue running after the application stops. This allows services to be reused by other applications or subsequent runs of the same application.

You can also configure the timeout for stopping services with the `quarkus.compose.devservices.stop-timeout` property. After the timeout, Compose Dev Services will forcefully stop the services. The default timeout is deliberately chosen to be short (1 second) for fast cleanup, but you can increase it as needed:

```text
# application.properties
quarkus.compose.devservices.stop-timeout=10s
```

## Relation to existing Dev Services

Compose Dev Services work alongside existing Dev Services implementations provided by Quarkus extensions. The service discovery process follows this order of precedence:

1. Dev Service implementations first try locating services via shared service labels (e.g., services started by other applications)
2. If no shared services are found, they fallback to discovering services started by Compose Dev Services
3. If no Compose services are found, the default Dev Services implementation is used (typically starting a Testcontainers-based service)

When Compose Dev Service creates or discovers a project, regular dev service implementations create containers inside the compose project default network. This ensures that all services can communicate with each other using their service names as hostnames within the same network.

<a id="compose-devservices-configuration-reference"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-enabled"></a> [`quarkus.compose.devservices.enabled`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-enabled) config_property_copy_button:+++quarkus.compose.devservices.enabled+++[]<br><br>Compose dev service enabled or disabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-files"></a> [`quarkus.compose.devservices.files`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-files) config_property_copy_button:+++quarkus.compose.devservices.files+++[]<br><br>List of file paths relative to the project root for Compose dev service configuration, if not provided will look for compose files in the project root<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_FILES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-project-name"></a> [`quarkus.compose.devservices.project-name`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-project-name) config_property_copy_button:+++quarkus.compose.devservices.project-name+++[]<br><br>Name of the compose project, used to discover running containers, if not provided a project name will be generated<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_PROJECT_NAME+++[] | string |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-profiles"></a> [`quarkus.compose.devservices.profiles`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-profiles) config_property_copy_button:+++quarkus.compose.devservices.profiles+++[]<br><br>Compose profiles to activate<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_PROFILES+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-options"></a> [`quarkus.compose.devservices.options`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-options) config_property_copy_button:+++quarkus.compose.devservices.options+++[]<br><br>List of additional options to pass to compose command<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_OPTIONS+++[] | list of string |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-start-services"></a> [`quarkus.compose.devservices.start-services`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-start-services) config_property_copy_button:+++quarkus.compose.devservices.start-services+++[]<br><br>Whether to run compose up and start containers at startup, when disabled, services are discovered by project name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_START_SERVICES+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-stop-services"></a> [`quarkus.compose.devservices.stop-services`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-stop-services) config_property_copy_button:+++quarkus.compose.devservices.stop-services+++[]<br><br>Whether to run compose down and stop containers at shutdown<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_STOP_SERVICES+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-ryuk-enabled"></a> [`quarkus.compose.devservices.ryuk-enabled`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-ryuk-enabled) config_property_copy_button:+++quarkus.compose.devservices.ryuk-enabled+++[]<br><br>Whether to use test containers Ryuk resource reaper to clean up containers<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_RYUK_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-remove-volumes"></a> [`quarkus.compose.devservices.remove-volumes`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-remove-volumes) config_property_copy_button:+++quarkus.compose.devservices.remove-volumes+++[]<br><br>Whether to remove volumes on compose down<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_REMOVE_VOLUMES+++[] | boolean | `true` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-remove-images"></a> [`quarkus.compose.devservices.remove-images`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-remove-images) config_property_copy_button:+++quarkus.compose.devservices.remove-images+++[]<br><br>Which images to remove on compose down<br><br>Locally built images, without custom tags are removed by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_REMOVE_IMAGES+++[] | `all`, `local` |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-env-variables-env-variables"></a> [`quarkus.compose.devservices.env-variables."env-variables"`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-env-variables-env-variables) config_property_copy_button:+++quarkus.compose.devservices.env-variables."env-variables"+++[]<br><br>Env variables to pass to all Compose instances<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_ENV_VARIABLES__ENV_VARIABLES_+++[] | Map\<String,String> |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-scale-scale"></a> [`quarkus.compose.devservices.scale."scale"`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-scale-scale) config_property_copy_button:+++quarkus.compose.devservices.scale."scale"+++[]<br><br>Scale configuration for services: Configure the number of instances for specific services<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_SCALE__SCALE_+++[] | Map\<String,Integer> |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-follow-container-logs"></a> [`quarkus.compose.devservices.follow-container-logs`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-follow-container-logs) config_property_copy_button:+++quarkus.compose.devservices.follow-container-logs+++[]<br><br>Whether to tail container logs to the console<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_FOLLOW_CONTAINER_LOGS+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-build"></a> [`quarkus.compose.devservices.build`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-build) config_property_copy_button:+++quarkus.compose.devservices.build+++[]<br><br>Whether to build images before starting containers.<br><br>When not provided, Compose images are built per-service `pull-policy`. When `true`, forces build of all images before starting containers. When `false`, skips re-building images before starting containers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_BUILD+++[] | boolean |  |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-reuse-project-for-tests"></a> [`quarkus.compose.devservices.reuse-project-for-tests`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-reuse-project-for-tests) config_property_copy_button:+++quarkus.compose.devservices.reuse-project-for-tests+++[]<br><br>Whether to reuse the project for tests, when disabled, a new project is created for each test run<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_REUSE_PROJECT_FOR_TESTS+++[] | boolean | `false` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-stop-timeout"></a> [`quarkus.compose.devservices.stop-timeout`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-stop-timeout) config_property_copy_button:+++quarkus.compose.devservices.stop-timeout+++[]<br><br>Timeout for stopping services, after the timeout the services are forcefully stopped,<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_STOP_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-core_quarkus-compose-devservices) | `1S` |
| [lock] <a id="quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-startup-timeout"></a> [`quarkus.compose.devservices.startup-timeout`](#quarkus-core_quarkus-compose-devservices_quarkus-compose-devservices-startup-timeout) config_property_copy_button:+++quarkus.compose.devservices.startup-timeout+++[]<br><br>Timeout for starting services, used as a global maximum for all wait strategies. If not provided, falls back to the global `quarkus.devservices.timeout`, or 1 minute by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_COMPOSE_DEVSERVICES_STARTUP_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-core_quarkus-compose-devservices) | `60s` |

<a id="duration-note-anchor-quarkus-core_quarkus-compose-devservices"></a>

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
