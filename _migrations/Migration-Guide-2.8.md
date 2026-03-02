---
date: 2022-04-12
---
## RESTEasy Reactive by default

RESTEasy Reactive is now our default REST layer.
It supports equally well reactive and traditional blocking workloads.

Note that RESTEasy Classic is still around so you don't have to migrate your projects to RESTEasy Reactive if you are not inclined to do so.

One other important change is that we don't force a REST layer on you when creating apps: prior to 2.8, we were automatically adding the RESTEasy extension when creating new projects in a lot of cases.
This is not the case anymore:

- If you create an application with no extensions (and with code examples enabled, which is the default), we will add RESTEasy Reactive automatically to show what a REST Quarkus application looks like.
- If you have defined at least one extension, we will consider you want to specify the extensions explicitly and we won't force RESTEasy Reactive on you.

If you want a REST layer in your applications, add `resteasy-reactive` to the set of extensions you select, or most probably `resteasy-reactive-jackson`.

To learn more about RESTEasy Reactive, see:

- [Our introduction to writing REST services](https://quarkus.io/guides/rest-json)
- [The RESTEasy Reactive reference guide](https://quarkus.io/guides/resteasy-reactive)
- [The REST Client Reactive guide](https://quarkus.io/guides/rest-client-reactive)
- [The RESTEasy Reactive migration guide](https://quarkus.io/guides/resteasy-reactive-migration) - again, you can pursue with RESTEasy Classic if you like it!

## `quarkus-resteasy-mutiny` deprecated

RESTEasy Reactive supports reactive programming via Mutiny in far more comprehensive manner that classic RESTEasy, so users of `quarkus-resteasy-mutiny` are encouraged to switch to RESTEasy Reactive.

## `quarkus-undertow-websockets` is gone

The long time deprecated `quarkus-undertow-websockets` legacy extension is now removed. Use `quarkus-websockets` instead.

## Assertj moved out of the BOM

We had frequent problems with Assertj binary compatibility (running tests compiled with an older version didn't work well with the version enforced in the BOM) so we decided to move Assertj outside of the Quarkus BOM.

That means you will have to define the version of Assertj in your own POM:

```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.22.0</version>
</dependency>
```

## quarkus.datasource.devservices removed

Long time deprecated `quarkus.datasource.devservices` config property has been removed.
Use `quarkus.datasource.devservices.enabled` instead.

## Hibernate ORM MariaDB Dialect updated to 10.6

Hibernate ORM is now using `org.hibernate.dialect.MariaDB106Dialect` by default.
If you are working with MariaDB 10.3, 10.4 or 10.5 you should configure the following:

```
quarkus.hibernate-orm.dialect=org.hibernate.dialect.MariaDB103Dialect
```

## OpenTracing 

Some OpenTracing libraries code was moved to SmallRye OpenTracing due to lack of maintenance in the upstream projects:

- [opentracing-jaxrs2](https://github.com/opentracing-contrib/java-jaxrs) 
- [opentracing-concurrent](https://github.com/opentracing-contrib/java-concurrent)
- [opentracing-web-servlet-filter](https://github.com/opentracing-contrib/java-web-servlet-filter)
- [opentracing-tracerresolver](https://github.com/opentracing-contrib/java-tracerresolver)

These are now removed from the Quarkus BOM and available in `io.smallrye:smallrye-opentracing-contrib` (included transitively with the Quarkus OpenTracing Extension).

## OpenTelemetry

- Static resources are not traced automatically anymore. To revert to the old behavior, please use the configuration `quarkus.opentelemetry.tracer.include-static-resources=true`.
- HTTP Span names now include a leading slash. This aligns with the [OpenTelemetry Semantic conventions for HTTP spans](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md) and the `http.route` tag.

## Qute

A `null` parameter of a loop section is treated as an empty iterable, smilarly as a `null` value is handled by an output expression. Previously a `null` parameter resulted in a `TemplateException` at runtime.

```html
{#for item in items} <1>
 {item.name}
{/for}
```
<1> If `items` is resolved to `null` then nothing is rendered. Previously, a `TemplateException` was thrown at runtime.


## REST Client Reactive `ReactiveClientHeadersFactory` method change

To align with `ClientHeadersFactory`, the `getHeaders` method now accepts `clientOutgoingHeaders` map as the second parameter. This parameter is a read-only map of header parameters specified on the client interface. 
After the change the signature of the method is:
```java
public abstract Uni<MultivaluedMap<String, String>> getHeaders(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders);
```

## Scheduler

Subclasses do not inherit the metadata of a `@Scheduled` method declared on a superclass anymore. The old behavior was undocumented and inconsistent; e.g. if `Scheduled#identity()` was used and the method was inherited then the build failed. 

In the following example, the `everySecond()` method is only invoked upon the instance of `Jobs`.
```java
class Jobs {
   @Scheduled(every = "1s")
   void everySecond() {
     // ..do something 
   }
}
@Singleton
class MyJobs extends Jobs {
}
```

## Stork extension properties config moved to the `quarkus` namespace

Stork extension configuration is now aligned with the rest of extensions. Properties are now configured under the `quarkus` namespace.
For configuring service discovery and load balancer strategies in the Stork extension, you can now use:

```properties
quarkus.stork.my-service.service-discovery.type=consul
quarkus.stork.my-service.service-discovery.consul-host=localhost
quarkus.stork.my-service.service-discovery.consul-port=8500

quarkus.stork.my-service.load-balancer.type=least-response-time
quarkus.stork.my-service.load-balancer.use-secure-random=true
```

If a service discovery and load balancer providers want to use the same _type_ name, the implementation must be in different packages.

## DevServices for Keycloak use Keycloak Quarkus distribution by default

`DevServices for Keycloak` now use a `Quarkus-based` `Keycloak` distribution by default. The default image name is currently `quay.io/keycloak/keycloak:17.0.1`. If you'd like to use a `WildFly-based` `Keycloak` distribution then use a `quay.io/keycloak/keycloak:17.0.1-legacy` image name. Please note the version such as `17.0.1` will keep changing going forward.

If you have to use an older `WildFly-based` `Keycloak` distributions then please set `quarkus.keycloak.devservices.keycloak-x-image=false`