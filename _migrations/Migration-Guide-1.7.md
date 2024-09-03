---
date: 2020-08-13
---
## Infinispan Embedded

The Infinispan Embedded extension has been discontinued with no replacement.

## Native executables

### file.encoding set to UTF-8

Native executables use the `file.encoding` defined at build time. It used to be set to the locale of the build host. It is now hardcoded to UTF-8 at build time to avoid inconsistent behaviors.

It is not configurable right now but if you have a use case for being able to configure it, please speak up on the `quarkus-dev` mailing list.

### GraalVM 19.3 not supported anymore

Starting with 1.7, we only support the JDK 11 flavor of GraalVM 20.1 for native executables building.

## io.quarkus:quarkus-bom-deployment has been removed

`io.quarkus:quarkus-bom-deployment` artifact was deprecated in 1.6.0.Final and now was removed completely in 1.7.0.Final. Its content (the deployment or build time Quarkus extension dependencies) was merged into `io.quarkus:quarkus-bom` artifact. Starting from 1.7.0.Final release, imports of `io.quarkus:quarkus-bom-deployment` should be changed to import `io.quarkus:quarkus-bom` instead.

### S2I image

The default S2I image is now a Java 11 image based on UBI: `registry.access.redhat.com/ubi8/openjdk-11`.

## SmallRye Reactive messaging extension health checks are enabled by default

SmallRye Reactive messaging extensions (eg. `io.quarkus:quarkus-smallrye-reactive-messaging-kafka`) automatically registers a Health Check for their respective messaging server (when `io.quarkus:quarkus-smallrye-health` is present). To disable this behavior, simply add the following to your `application.properties`:

```properties
quarkus.reactive-messaging.health.enabled=false
``` 