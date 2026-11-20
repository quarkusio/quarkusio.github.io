# Dev Services for Apicurio Registry

If an extension for schema registry, such as `quarkus-apicurio-registry-avro` or `quarkus-confluent-registry-avro`, is present, Dev Services for Apicurio Registry automatically starts an Apicurio Registry instance in dev mode and when running tests. Also, all Kafka channels in SmallRye Reactive Messaging are automatically configured to use this registry. This automatic configuration only applies to serializers and deserializers from Apicurio Registry serde libraries and Confluent Schema Registry serde libraries, because their properties are set:

```properties
# for Apicurio Registry serde
mp.messaging.connector.smallrye-kafka.apicurio.registry.url=http://localhost:8081/apis/registry/v3
# for Confluent Schema Registry serde
mp.messaging.connector.smallrye-kafka.schema.registry.url=http://localhost:8081/apis/ccompat/v7
```

## Enabling / Disabling Dev Services for Apicurio Registry

Dev Services for Apicurio Registry is automatically enabled unless:

- `quarkus.apicurio-registry.devservices.enabled` is set to `false`
- `mp.messaging.connector.smallrye-kafka.apicurio.registry.url` is configured
- `mp.messaging.connector.smallrye-kafka.schema.registry.url` is configured
- all the Reactive Messaging Kafka channels have either the `apicurio.registry.url` attribute or the `schema.registry.url` attribute set

Dev Services for Apicurio Registry relies on Docker to start the registry. If your environment does not support Docker, you will need to start the registry manually, or use an already running registry. In such case, you can configure the registry URL for all Kafka channels in SmallRye Reactive Messaging with a single property. For Apicurio Registry serde, that is:

```properties
mp.messaging.connector.smallrye-kafka.apicurio.registry.url=... your Apicurio Registry URL...
```

For Confluent Schema Registry serde, that is:

```properties
mp.messaging.connector.smallrye-kafka.schema.registry.url=... your Confluent Schema Registry URL...
```

## Shared registry

Most of the time, you need to share the registry between applications. Dev Services for Apicurio Registry implements a *service discovery* mechanism for your multiple Quarkus applications running in *dev* mode to share a single registry.

> [!NOTE]
> Dev Services for Apicurio Registry starts the container with the `quarkus-dev-service-apicurio-registry` label, which is used to identify the container.

If you need multiple (shared) registries, you can configure the `quarkus.apicurio-registry.devservices.service-name` attribute and indicate the registry name. It looks for a container with the same value, or starts a new one if none can be found. The default service name is `apicurio-registry`.

Sharing is enabled by default in dev mode, but disabled in test mode. You can disable the sharing with `quarkus.apicurio-registry.devservices.shared=false`.

## Setting the port

By default, Dev Services for Apicurio Registry picks a random port and configures the application. You can set the port by configuring the `quarkus.apicurio-registry.devservices.port` property.

Note that the Kafka channels in SmallRye Reactive messaging are automatically configured with the chosen port.

## Configuring the image

Dev Services for Apicurio Registry uses `apicurio/apicurio-registry` images. These images use an in-memory h2 database by default. You can select any 3.x version from <https://hub.docker.com/r/apicurio/apicurio-registry>:

```properties
quarkus.apicurio-registry.devservices.image-name=apicurio/apicurio-registry:latest-snapshot
```

<a id="Compose"></a>
## Compose

The Apicurio Dev Services supports [Compose Dev Services](/guides/compose-dev-services/index.md). It relies on a `compose-devservices.yml`, such as:

```yaml
name: <application name>
services:
  apicurio:
    image: quay.io/apicurio/apicurio-registry:3.1.7
    ports:
      - "8080"
```
