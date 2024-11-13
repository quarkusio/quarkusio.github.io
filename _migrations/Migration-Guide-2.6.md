---
date: 2021-12-21
---
## Extensions moved to the Quarkiverse Hub

Once upon a time, Quarkus was a young project and we didn't have a lot of infrastructure around it to host extensions.
Thus all the extensions created then ended up in the main repository (called Core).

Since then, we have invested a lot in setting up the Quarkiverse Hub infrastructure which allows easy collaboration on extensions that live outside of the Core repository.

The Core repository is very large now and we have started moving extensions outside of it to make it smaller (easier to import in IDEs, faster local builds, shorter CI runs...) and also to lower the barrier to entry to contribute to these extensions: they are in small independent projects, far easier to build and contribute to.

For all these extensions, we have put in place relocations so migration should be seamless but in any case you need to adjust your project for the future. If you are using one of these moved out extensions, you will see a warning when building your project instructing you how to fix the issue.

If you are a user of these extensions, have a look at their respective project and don't hesitate to contribute to them and provide feedback.

These moved out extensions are not handled the same and there are three categories.

### Extensions moved out of the Quarkus Platform

These extensions are not in the Quarkus Platform BOM anymore and you need to define the version yourself in your build file:

| Component  | New groupId | Current version | Repository |
| -----------| ------------| ----------------| ------------|
| Amazon Alexa | `io.quarkiverse.amazonalexa` | `1.0.1` | https://github.com/quarkiverse/quarkus-amazon-alexa |
| Artemis | `io.quarkiverse.artemis` | `1.0.3` | https://github.com/quarkiverse/quarkus-artemis |
| AWS support for Hibernate Search ORM + Elasticsearch | `io.quarkiverse.hibernatesearchextras` | `1.0.1` | https://github.com/quarkiverse/quarkus-hibernate-search-extras |
| JGit | `io.quarkiverse.jgit` | `1.1.0` | https://github.com/quarkiverse/quarkus-jgit |
| JSch | `io.quarkiverse.jsch` | `1.0.0` | https://github.com/quarkiverse/quarkus-jsch |
| Logging Sentry | `io.quarkiverse.loggingsentry` | `1.0.1` | https://github.com/quarkiverse/quarkus-logging-sentry |
| Neo4j | `io.quarkiverse.neo4j` | `1.0.2` | https://github.com/quarkiverse/quarkus-neo4j |
| Reactive Messaging HTTP | `io.quarkiverse.reactivemessaging.http` | `1.0.0` | https://github.com/quarkiverse/quarkus-reactive-messaging-http |
| Tika | `io.quarkiverse.tika` | `1.0.3` | https://github.com/quarkiverse/quarkus-tika |


> :warning: if you are using the `org.apache.activemq:artemis-server` or `org.apache.activemq:artemis-amqp-protocol` dependencies for your Artemis testing without defining a version, you will have build errors (their versions used to be defined in the Quarkus BOM but they have been removed from there). To solve this issue, you can import `io.quarkiverse.artemis:quarkus-artemis-bom:1.0.2` as a BOM in your project. It will define the versions of these dependencies.

### Extensions included inside the Platform BOM

These extensions have been included into the Platform BOM so if you are using the `io.quarkus.platform:quarkus-bom` BOM, you don't need to set the version but you still need to change the groupId.

| Component  | New groupId | New artifactId | Current version | Repository |
| -----------| ------------| ---------------| ----------------| ------------|
| Consul Config | `io.quarkiverse.config` | `quarkus-config-consul` | `1.0.1` | https://github.com/quarkiverse/quarkus-config-extensions |
| Vault | `io.quarkiverse.vault` | _same_ | `1.0.1` | https://github.com/quarkiverse/quarkus-vault |

> :warning: The artifactId for the Consul Config extension has changed from `quarkus-consul-config` to `quarkus-config-consul` to be more consistent with the other Config extensions.

### Extensions included inside the Platform with their own BOM

These extensions have their own specific BOM in the Quarkus Platform. If you want to use them, you need to add it to your project.

| Component  | New groupId | BOM | Repository |
| -----------| ------------| ----| -----------|
| Amazon Services | `io.quarkiverse.amazonservices` | `io.quarkus.platform:quarkus-amazon-services-bom`:`your quarkus version` | https://github.com/quarkiverse/quarkus-amazon-services |

> :information_source: The Amazon Services extensions are the extensions connecting to AWS services such as DynamoDB, IAM, S3...

## Flyway

The SQL Server integration was moved to a separate dependency. See https://flywaydb.org/documentation/database/sqlserver#java-usage. SQL Server users need to add the following dependency from now on:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-sqlserver</artifactId>
</dependency>
```

## Deprecated OIDC `TokenConfigResolver` and `TokenStateManager` methods, `quarkus.oidc.authentication.auto-refresh-timeout` property removed

OIDC `TokenConfigResolver` methods [deprecated in 2.2](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.2#deprecated-method-in-oidc-tenantconfigresolver) and `TokenStateManager` methods [deprecated in 2.3](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.3#deprecated-methods-in-oidc-tokenstatemanager) have now been removed.

It should have a minimum impact if any at all since only `TokenConfigResolver` and `TokenStateManager` methods returning `Uni` can work without blocking the IO thread and thus should be used in the real world applications.

A long time deprecated `quarkus.oidc.authentication.auto-refresh-timeout` property has also been removed - please use a better named `quarkus.oidc.authentication.refresh-token-time-skew` from now on.

## Access to `RoutingContext` in OIDC `SecurityIdentityAugmentor`

The way a `Vert.x` `RoutingContext` can be accessed in the custom OIDC `SecurityIdentityAugmentor`s has changed.
If it is required then please access it as a `SecurityIdentity` attribute which will be more portable:

```
import javax.enterprise.context.ApplicationScoped;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomOidcSecurityIdentityAugmentor implements SecurityIdentityAugmentor {
    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        // Instead of 
        // IdTokenCredential cred = identity.getCredential(IdTokenCredential.class);
        // RoutingContext context = cred.getRoutingContext();
        RoutingContext context = identity.getAttribute(RoutingContext.class.getName());

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
        // Use RoutingContext as required
        return Uni.createFrom().item(builder.build);
    }
}
```

The old way of accessing `RoutingContext` as an OIDC `IdTokenCredential` or `AccessTokenCredential` property prevents the use of OIDC tokens for running the background tasks when no `RoutingContext` is available.


## Reactive Routes 

### _produces_ changes

The `produces` attribute of the `@Route` annotation was only used for content negotiation.
Starting Quarkus 2.6, it is also used to indicate how `Multi` instances need to be serialized in the HTTP response. 

When a route returns a `Multi<T>`, it can:

* send the item one by one, without any modification (_raw_ stream)
* wrap the `Multi` as a JSON Array, where each item is sent one by one
* produce a server-sent-event stream 
* produce JSON (also named ND-JSON) stream

Before Quarkus 2.6, to express the three last possibilities you had to wrap the produced `Multi` using `ReactiveRoutes.asJsonArray`, `ReactiveRoutes.asEventStream` and `ReactiveRoutes.asJsonStream`. Unfortunately, this approach does not work when Quarkus security is enabled. 

To work around that problem, starting Quarkus 2.6, you can indicate the serialization you need using the `produces` attribute of `@Route`. If the first value of the `produces` array is `application/json`, `text/event-stream`, `application/x-ndjson` (or `application/stream+json`), the associated serialization is used.

So, instead of:

1. returning `ReactiveRoutes.asJsonArray(multi)`, return `multi` directly and set `produces="application/json"`
2. returning `ReactiveRoutes.asEventStream(multi)`, return `multi` directly and set `produces="text/event-stream"`
3. returning `ReactiveRoutes.asJsonStream(multi)`, return `multi` directly and set `produces="application/x-ndjson"`

### Bean Validation JSON Format

The format of a JSON response produced when a constraint violation occurs was changed in an incompatible way.

If a parameter of a reactive route method violates the rules defined by Bean Validation then an HTTP 400 response is produced.
And if the request accepts the JSON payload then the response should follow the [Problem](https://opensource.zalando.com/problem/constraint-violation/) format.
However, prior to version 2.6 the produced JSON included an incorrect property: `details`.
The payload now follows the format, i.e. the property is called `detail`.

## Native image builder changes

The native image builders `ubi-quarkus-native-image` and `ubi-quarkus-mandrel` are now based on [ubi-micro:8.5](https://catalog.redhat.com/software/containers/ubi8/ubi-micro/5ff3f50a831939b08d1b832a) (instead of ubi-micro:8.4).
In addition, both images provide the [upx](https://upx.github.io/) tool which can be used to compress native executable.

## gRPC Metrics changes

gRPC metrics are now handled by the `quarkus-micrometer` extension.
gRPC server and clients metrics are automatically enabled when your application depends on the `quarkus-micrometer` extension.
In the case you do not want to collect these metrics, you can disable them using:

```properties
quarkus.micrometer.binder.grpc-client.enabled=false
quarkus.micrometer.binder.grpc-server.enabled=false
```

The `quarkus.grpc.metrics.enabled` property has no effect anymore. 


## gRPC Client Interceptors

Global gRPC client interceptors must be annotated with `@io.quarkus.grpc.GlobalInterceptor` since 2.6.

Prior to Quarkus 2.6, all the CDI beans implementing `io.grpc.ClientInterceptor` were considered global interceptors, i.e. applied to all injected gRPC clients. Since 2.6, it is possible to make a client-specific interceptor, by annotating the injection point of a gRPC client with the `@io.quarkus.grpc.RegisterClientInterceptor` annotation.

## Reactive Messaging _split-package_

To avoid having a split package between the `smallrye-reactive-messaging-api` and `smallrye-reactive-messaging-provider`, the classes from `io.smallrye.reactive.messaging` from the `smallrye-reactive-messasing-provider` have been moved to `io.smallrye.reactive.messaging.provider`. These classes are internal / implementation-specific classes and should not have been used directly. 

