---
date: 2021-06-30
---
## JDK 11

The minimal JDK version supported by Quarkus 2.0 is JDK 11.

JDK 8 is not supported anymore.

## Maven 3.8

Maven 3.8.1 is the new minimum Maven version.
It fixes [an important CVE](https://maven.apache.org/docs/3.8.1/release-notes.html) so, in any case, it is highly recommended to upgrade to it.

## Jandex upgrade

We fixed some issues related to the Jandex indexer.
Jandex itself is part of our BOM so will be upgraded automatically but if you use some Jandex build plugins, you need to upgrade them yourself. 

* If you are using the Jandex Maven plugin, please upgrade it to 1.1.0.
* If you are using the Jandex Gradle plugin (`org.kordamp.gradle.jandex`), please upgrade it to 0.11.0.

## SmallRye Config
Quarkus `@ConfigProperties` has been deprecated in favour of SmallRye Config `@ConfigMapping`. Please, refer to the [Mapping Configuration to Objects](https://quarkus.io/version/main/guides/config-mappings).

## Mapping of YAML Configuration to complex objects

Mapping of YAML Configuration to complex objects has been removed from `io.quarkus.arc.config.ConfigProperties`. Please use `io.smallrye.config.ConfigMapping` instead which is a safer alternative.

## SmallRye JWT

`smallrye.jwt.sign.key-location` has been renamed to `smallrye.jwt.sign.key.location` and `smallrye.jwt.encrypt.key-location` - to `smallrye.jwt.encrypt.key.location`.

## Avro
Avro has been integrated with the Quarkus code generation mechanism. To use it with Maven, make sure you have the `generate-code` 
goal enabled for the `quarkus-maven-plugin`, it should be enabled by default if you created your project fairly recently.
For Gradle no specific task is required.

This replaces the need to use Avro plugin, such as the `avro-maven-plugin`, for projects that use Avro.

## Apicurio Registry + Avro
A new extension for Apicurio Registry 2.x client libraries for Avro is present: `quarkus-apicurio-registry-avro`.
If you use Apicurio Registry 2.x client libraries for Avro (`io.apicurio:apicurio-registry-serdes-avro-serde`), you have to use this extension now (otherwise application build will fail).

Support for Apicurio Registry 1.x client libraries for Avro, as well as Confluent Schema Registry libraries for Avro, is not affected.

## Kubernetes / Openshift

### VCS annotations

The value `app.quarkus.io/vcs-url` is no longer converted to an `http` url and will now match the url of the `origin` remote. Users that need the prefer the use of the `http` protocol over `git`, will have to configure that on git level.

### Service port mapping

Services that are exposing http ports are now automatically mapped to port `80`. The change makes the generated `Service` easier to consume by external tools, or services as knowledge of the actual container port is no longer needed by consumers.
This may affect services that were using the service dns name and container port combination to communicate with each other.

## Kubernetes Client

The Kubernetes Client has been upgraded to 5.4, which contains some breaking changes.

Please refer to [the release notes](https://github.com/fabric8io/kubernetes-client/releases/tag/v5.4.0) for more details.

## gRPC

The `io.quarkus.grpc.runtime.annotations.GrpcService` annotation was renamed to `io.quarkus.grpc.GrpcClient`. Furthermore, the `@GrpcClient.value()` is optional and the service name can be derived from the annotated element.

gRPC service classes now have to be annotated with `@io.quarkus.grpc.GrpcService` instead of `@javax.inject.Singleton`.

## Qute

The deprecated annotations from the `io.quarkus.qute.api` package were removed. All occurrences of `@io.quarkus.qute.api.CheckedTemplate` should be replaced with `@io.quarkus.qute.CheckedTemplate` and occurrences of `@io.quarkus.qute.api.ResourcePath` should be replaced with `@io.quarkus.qute.Location`.

Checked templates require type-safe expressions by default, i.e. expressions that can be validated at build time. It's possible to use `@CheckedTemplate(requireTypeSafeExpressions = false)` to relax this requirement.

Multiple ``NamespaceResolver`` instances can be registered for the same namespace provided that each declares a different priority.

## Quartz

The deprecated property `quarkus.quartz.force-start` was removed. Use `quarkus.quartz.start-mode=forced` instead.

The deprecated config value `StoreType.DB` was removed. `quarkus.quartz.store-type=db` should be replaced with `quarkus.quartz.store-type=jdbc-cmt`.

## Keycloak Authorization

Anonymous requests to public resources with no matching Keycloak Authorization policies will now correctly return HTTP `200` status as opposed to `401`. Adding `DISABLED` Keycloak Authorization policies in such cases is no longer necessary.

In 2.0, we removed some features that were deprecated for a while.

## Legacy Redirect for non-application endpoints such as `/health`

When we introduced the new `/q` mounting point for `/health`, `/metrics`, and other non-application endpoints,
the `quarkus.http.redirect-to-non-application-root-path` configuration property was provided to revert back to the previous behavior of not prefixing them with `/q`.

The property was deprecated and is now removed.

It is possible to achieve the previous behavior by explicitly setting endpoints to be absolute instead of relative. For instance, the Health endpoint can be forced to be available at `/health` instead of `/q/health` by setting `quarkus.smallrye-health.root-path=/health`.

### Impact on other services

Services using non-application endpoints, such as Prometheus ServiceMonitor, should be adjusted accordingly.

#### Prometheus ServiceMonitor

In order to collect statistics for your service, the service monitor YAML file should declare `path: /q/metrics` under
the `endpoints` declaration.

## Container Image JIB

As of Quarkus 2.0, when the `quarkus.container-image.username` and `quarkus.container-image.password` properties are set, credentials from other locations (like the docker config on the file system) will be ignored

## Databases

### Reactive Datasources

The configuration property `datasource.reactive.max-size` is now applied to the whole pool. In previous versions there was a Pool for each thread, leading to applying a separate max limit to each pool in each thread. There is a single Pool shared among all threads now, which is more intuitive to use and configure.

## Vert.x

Quarkus 2.0 uses Vert.x 4.0. If your application relies on Vert.x APIs, check the [Vert.x migration guide](https://vert-x3.github.io/vertx-4-migration-guide/index.html).

### Reactive Routes

The enum of the HTTP _verb_ to configure the `methods` attribute of a `@Route` has been moved to `io.quarkus.vertx.web.Route.HttpMethod`.

### Vert.x JSON support

In Quarkus 2.0, Vert.x uses the Jackson Mapper managed by Quarkus. Thus, instead of configuring the JSON object mappers on the `Json` class, customize the Jackson Mapper following the instructions from https://quarkus.io/guides/rest-json#configuring-json-support. 

## Neo4j

The `quarkus.neo4j.pool.metrics-enabled` property that enables Neo4j metrics was renamed to `quarkus.neo4j.pool.metrics.enabled` to make the naming more consistent with similar properties in other extensions.

## Kafka


### Accessing the configuration

Accessing the default configuration of the Kafka broker must use the `@Identifier` annotation, instead of `@Named`:

```java
@ApplicationScoped
public class KafkaProviders {

    @Inject
    @Identifier("default-kafka-broker") // was @Named before
    Map<String, Object> config;
}
```

The `KafkaClientService.getProducer()` method no longer returns `org.apache.kafka.clients.producer.Producer`.
Instead, it returns `io.smallrye.reactive.messaging.kafka.KafkaProducer`, which exposes the most common producer methods with a `Uni`-based interface.
The underlying reason is that the Kafka connector in SmallRye Reactive Messaging now sends all records to the Kafka broker on an extra thread, to guarantee fully non-blocking behavior.

### Dev Mode - Connecting to localhost:9092

By default, the Kafka client was connecting to localhost:9092 if the broker location was not configured. 
In 2.0, Dev Services will start a broker and connect to this one. If you need to connect to a broker running on localhost:9092, you need to set `kafka.bootstrap.servers=localhost:9092`.

## MongoDB

The old `io.quarkus.mongodb.runtime.MongoClientName` annotation that has been deprecated five release ago is now removed, please use `io.quarkus.mongodb.MongoClientName` instead.

## Reactive MongoDB with Panache

The `persist()`, `update()` and `persistOrUpdate()` methods now return an `Uni<T extends PanacheMongoEntityBase>` instead of an `Uni<Void>` to allow chainning the methods. The same has been done for the Kotlin variant as well.

## Hibernate ORM with Panache

The `getEntityManager()` and the `flush()` methods of `PanacheEntityBase` are now static methods. The same has been done for the Kotlin variant, those methods are now in the PanacheEntityBase companion object.

## Hibernate reactive with Panache

The `persist()` and `persistAndFlush()` methods now return an `Uni<T extends PanacheEntityBase>` instead of an `Uni<Void>` to allow chainning the methods.

## OpenTracing JDBC instrumentation

In Quarkus 2.0, we updated the version of the OpenTracing JDBC driver to a version that uses dynamic proxies, so not compatible by default with native image.

So, if you're using the OpenTracing JDBC driver to instrument JDBC calls, **and deploys your application as a native image**, the easiest way to make it work is to downgrade to version 0.2.4 inside your `pom.xml`:

```xml
<dependency>
      <groupId>io.opentracing.contrib</groupId>
     <artifactId>opentracing-jdbc</artifactId>
     <version>0.2.4</version>
</dependency>
```

You can also try to register the dynamic proxy usage for native image, see the details in the following issue: https://github.com/quarkusio/quarkus/issues/18033