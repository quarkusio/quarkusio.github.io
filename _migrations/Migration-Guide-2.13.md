---
date: 2022-09-28
---

## GraalVM CE / Mandrel migration from 22.2 to 22.3

Starting with 2.13.4.Final, Quarkus defaults to using GraalVM CE / Mandrel 22.3 for generating native executables.

### Substitution (and other) annotations

Starting with 22.3, substitution (and other) annotations located under the package `com.oracle.svm.core.annotate` have been moved from `org.graalvm.nativeimage:svm:jar` to `org.graalvm.sdk:graal-sdk:jar`.
If your code relies on any of these annotations, please update your pom files accordingly, i.e., substituting the `svm` dependency with the `graal-sdk` one.

NOTE: If your code relies on such annotations and you want to be able to compile it with both Quarkus < 2.13.4 and >= 2.13.4, then please include both dependencies in your project, i.e.:

```xml
...
<dependency>
    <groupId>org.graalvm.sdk</groupId>
    <artifactId>graal-sdk</artifactId>
</dependency>
<dependency>
    <groupId>org.graalvm.nativeimage</groupId>
    <artifactId>svm</artifactId>
</dependency>
...
```

NOTE: Some annotations, like `@AlwaysInline` have been moved from `com.oracle.svm.core.annotate` to `com.oracle.svm.core`, and are not shipped in `org.graalvm.sdk:graal-sdk:jar`.
If your code relies on such annotations, you are strongly advised to reconsider their use as they are not public API.
If, however, they turn out to be mandatory for the correct functionality of your code please make sure to update the `import` statements in your source code and include `org.graalvm.nativeimage:svm:jar` in addition to `org.graalvm.sdk:graal-sdk:jar` in the dependencies of your project.

### Mandrel no longer providing Java 11 based releases

Starting with 22.3, Mandrel no longer provides Java 11 based releases.
As a result, if you used Mandrel builder images with the suffix `-java11` in the past, please be advised that such images will no longer be available, thus you are encouraged to use the `-java17` suffixed ones.
Note, however,  that this doesn't mean that you may no longer produce native executables with Mandrel for Java 11 projects.
You may still compile your Java 11 projects using OpenJDK 11 and produce native executables from the resulting Java 11 bytecode using the `-java17` Mandrel builder images.

## Flyway

`quarkus.flyway.validate-on-migrate` is now enabled by default, which will cause the deployment to fail if you have modified migrations after they have been applied.
You can either disable it, or enable `quarkus.flyway.repair-at-start` to align checksums in the history table with their current values.

## SmallRye Reactive Messaging

Prior to Quarkus 2.13, the Reactive Messaging consuming methods were called with an active CDI request context, inadvertently propagated, and were never terminated. Quarkus corrects this behaviour and makes sure the request context is not activated unnecessarily on message consuming methods. Code relying on the presence of the `RequestScoped` beans might need to start a request scope explicitly; for example, using `@ActivateRequestContext` annotation on the message consuming method. 

Note that using `@ActivateRequestContext` on a method creates a request context if the method is not already called on an existing request context. If a new context was created for the method, at the end of the method execution (e.g. the completion of the returned `Uni` or `CompletionStage`) the context will be destroyed, effectively disposing request scoped beans bound to it.

## @TestHTTPResource URI

When using @TestHTTPResource as described [here](https://github.com/quarkusio/quarkus/blob/2.13.0.CR1/docs/src/main/asciidoc/getting-started-testing.adoc#43-injecting-a-uri), the injected URI now also contains the any path that was specified in the `quarkus.http.root-path` configuration property.

## @InjectMock

The `quarkus-junit5-mockito` extension is internally using the `javax.enterprise.inject.spi.BeanManager#getBeans()` method to get the set of beans eligible for an `@InjectMock` injection point.
Unfortunately, the behavior of `BeanManager#getBeans()` was broken - if no qualifier was specified then any bean that matching the required type was eligible for injection.
However, the CDI specification mandates that the container must assume the `@Default` qualifier instead.
As a result, a test that injects a mock of a bean with non-`@Default` qualifier and does not specify the qualifier explicitly will fail.
A typical example is the injection of a Reactive REST Client - an `@InjectMock` injection point needs to be annotated with `@org.eclipse.microprofile.rest.client.inject.RestClient`.

## OpenTelemetryClientFilter
The OpenTelemetryClientFilter is not required to be registered manually anymore. 
Before, when creating a programatic `javax.ws.rs.client.Client` with `javax.ws.rs.client.ClientBuilder.newClient()`, this filter was required to enable OpenTelemetry Tracing to create Spans for that client.
The underlying Vert.x client, used by the reactive REST client, now adds the OpenTelemetry instrumentation, simplifying this client's creation.

## CORS Filter returns HTTP 403 for failed preflight requests since 2.7.7.Final

CORS filter used to return HTTP 200 status code and no Access-Control-Allow-Origin header when CORS preflight requests were failing. Now, HTTP 403 will be returned instead of 200 as well. Please update your tests which expect HTTP 200 status code for the failed preflight requests to check for HTTP 403 status code instead.