# Implementing a custom Stork service registrar

This guide explains how to implement a custom service registrar for [SmallRye Stork](https://smallrye.io/smallrye-stork) and use it to programmatically register service instances at startup.

If you are new to Stork, please read the [Stork Getting Started Guide](/guides/stork/index.md).

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this guide, we will build an application that:

- Implements a custom service registrar by extending Stork's SPI
- Programmatically registers a service instance at startup
- Configures the registrar via `application.properties`

Beyond service discovery and load balancing, it also supports [_service registration_](/guides/stork-registration/index.md). Service registration is the process of announcing a service instance to a registry so that other services can discover it. While Stork provides built-in registrars for Consul, Eureka, and others, you can implement your own by using the Stork SPI.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `stork-programmatic-custom-registration-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/stork-programmatic-custom-registration-quickstart).

## Bootstrapping the project

Create a Quarkus project importing the quarkus-rest, quarkus-smallrye-stork, and quarkus-rest-client-jackson extensions using your favorite approach:

**CLI**

```bash
quarkus create app org.acme:stork-programmatic-custom-registration-quickstart \
    --extension='quarkus-rest,quarkus-smallrye-stork,quarkus-rest-client-jackson' \
    --no-code
cd stork-programmatic-custom-registration-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=stork-programmatic-custom-registration-quickstart \
    -Dextensions='quarkus-rest,quarkus-smallrye-stork,quarkus-rest-client-jackson' \
    -DnoCode
cd stork-programmatic-custom-registration-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=stork-programmatic-custom-registration-quickstart"`

In the generated project, also add the following dependencies:

**pom.xml**

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-core</artifactId>
</dependency>
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-configuration-generator</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.smallrye.stork:stork-core")
implementation("io.smallrye.stork:stork-configuration-generator")
```

`stork-core` provides the Stork API and SPI needed to implement a custom registrar. `stork-configuration-generator` generates configuration classes at build time based on annotations on your registrar provider.

## The Custom Service Registrar Provider

Stork uses a provider model. To create a custom registrar, you need two classes:

1. A **provider** implementing `ServiceRegistrarProvider` — the factory that creates registrar instances.
2. A **registrar** implementing `ServiceRegistrar` — the actual registration logic.

Let's start with the provider. Create the `src/main/java/org/acme/services/CustomServiceRegistrarProvider.java` file with the following content:

```java
package org.acme.services;

import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;
import io.smallrye.stork.api.config.ServiceRegistrarAttribute;
import io.smallrye.stork.api.config.ServiceRegistrarType;
import io.smallrye.stork.spi.ServiceRegistrarProvider;
import io.smallrye.stork.spi.StorkInfrastructure;
import jakarta.enterprise.context.ApplicationScoped;

@ServiceRegistrarType(value = "custom", metadataKey = Metadata.DefaultMetadataKey.class)
@ServiceRegistrarAttribute(name = "host",
        description = "Host name of the service registration server.", required = true)
@ServiceRegistrarAttribute(name = "port",
        description = "Port of the service registration server.", required = false)
@ApplicationScoped
public class CustomServiceRegistrarProvider
        implements ServiceRegistrarProvider<CustomRegistrarConfiguration, Metadata.DefaultMetadataKey> {

    @Override
    public ServiceRegistrar createServiceRegistrar(
            CustomRegistrarConfiguration config,
            String serviceName,
            StorkInfrastructure storkInfrastructure) {
        return new CustomServiceRegistrar(config);
    }

}
```

There are a few important things to note:

- The `@ServiceRegistrarType` annotation declares the registrar *type*. This is the value used in the `quarkus.stork.<service-name>.service-registrar.type` property. Here, it is `custom`.
- The `@ServiceRegistrarAttribute` annotations declare the configuration attributes for this registrar. Stork's annotation processor (provided by `stork-configuration-generator`) uses these annotations to generate the `CustomRegistrarConfiguration` class at compile time. You do not need to write this class yourself.
- The `@ApplicationScoped` annotation makes this provider a CDI bean, so Stork can discover it automatically.
- The `createServiceRegistrar` method is the factory that creates a `CustomServiceRegistrar` from the generated configuration.

## The Custom Service Registrar

Now let's implement the registrar itself. Create the `src/main/java/org/acme/services/CustomServiceRegistrar.java` file with the following content:

```java
package org.acme.services;

import io.smallrye.mutiny.Uni;
import io.smallrye.stork.api.Metadata;
import io.smallrye.stork.api.ServiceRegistrar;

import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CustomServiceRegistrar implements ServiceRegistrar {

    private static final Logger LOGGER = Logger.getLogger(CustomServiceRegistrar.class.getName());

    private final String backendHost;
    private final int backendPort;
    private final Map<String, String> registeredInstances = new ConcurrentHashMap<>();

    public CustomServiceRegistrar(CustomRegistrarConfiguration configuration) {
        this.backendHost = configuration.getHost();
        this.backendPort = Integer.parseInt(configuration.getPort()!=null?configuration.getPort():"8080");
    }


    @Override
    public Uni<Void> registerServiceInstance(String serviceName, Metadata metadata, String ipAddress, int defaultPort) {
        String address = ipAddress + ":" + defaultPort;
        LOGGER.info("Registering service: " + serviceName + " with ipAddress: " + ipAddress + " and port: " + defaultPort);
        registeredInstances.put(serviceName, address);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> deregisterServiceInstance(String serviceName) {
        LOGGER.infof("Deregistering service '%s' from backend %s:%d", serviceName, backendHost, backendPort);
        registeredInstances.remove(serviceName);
        return Uni.createFrom().voidItem();
    }
}
```

This registrar:

- Receives its configuration (host and port) from the generated `CustomRegistrarConfiguration` class.
- Stores registered instances in a `ConcurrentHashMap`.
- Implements `registerServiceInstance` to add a service instance to the map.
- Implements `deregisterServiceInstance` to remove a service instance on shutdown.
- Both methods return `Uni<Void>`, since Stork uses Mutiny for reactive support.

## Programmatic Service Registration at Startup

With the registrar in place, we can now register service instances programmatically. Create the `src/main/java/org/acme/services/Registration.java` file with the following content:

```java
package org.acme.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.stork.Stork;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class Registration {

    /**
     * Register our two services using custom registrar.
     *
     * Note: this method is called on a worker thread, and so it is allowed to block.
     */
    public void init(@Observes StartupEvent ev, Vertx vertx) {
        Stork.getInstance().getService("my-service").registerInstance("my-service", "localhost",
                9000);
    }
}
```

When the application starts, this CDI bean observes the `StartupEvent` and uses the Stork API to register a service instance. The `Stork.getInstance().getService("my-service")` call retrieves the Stork service named `my-service`, and `registerInstance` delegates to the custom registrar we created earlier.

## Stork configuration

Now we need to configure Stork to use our custom registrar.

In the `src/main/resources/application.properties`, add:

```properties
quarkus.stork.my-service.service-registrar.type=custom
quarkus.stork.my-service.service-registrar.host=localhost
```

The `quarkus.stork.my-service.service-registrar.type` property tells Stork to use the `custom` registrar type. This matches the value declared in the `@ServiceRegistrarType` annotation on `CustomServiceRegistrarProvider`.

The `quarkus.stork.my-service.service-registrar.host` property maps to the `host` attribute declared with `@ServiceRegistrarAttribute` on the provider. This is a required attribute.

You can optionally set `quarkus.stork.my-service.service-registrar.port` as well, since the provider declares an optional `port` attribute. If omitted, the registrar defaults to port `8080`.

## Running the application

Package the application:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

And run it:

```shell script
> java -jar target/quarkus-app/quarkus-run.jar
```

You should see the following log message in the output, confirming that the custom registrar is invoked:

```text
INFO  [org.acm.ser.CustomServiceRegistrar] Registering service: my-service with ipAddress: localhost and port: 9000
```

You can compile this application into a native executable:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

## Summary

In this guide, we have:

1. Implemented a custom `ServiceRegistrarProvider` using the `@ServiceRegistrarType` and `@ServiceRegistrarAttribute` annotations.
2. Implemented a custom `ServiceRegistrar` with `registerServiceInstance` and `deregisterServiceInstance` methods.
3. Used the Stork API to programmatically register a service instance at startup.
4. Configured the custom registrar in `application.properties`.

This pattern is useful when you need to integrate Stork with a service registry that is not supported out of the box.

## Going further

This guide has shown how to extend SmallRye Stork with a custom service registrar. You can find more about Stork in:

- the [Stork reference guide](/guides/stork-reference/index.md),
- the [Stork with Consul getting started guide](/guides/stork/index.md),
- the [Stork with Kubernetes guide](/guides/stork-kubernetes/index.md),
- the [SmallRye Stork website](https://smallrye.io/smallrye-stork).
