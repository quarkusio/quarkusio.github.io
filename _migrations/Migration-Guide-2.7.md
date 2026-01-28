---
date: 2022-02-02
---
## JNDI disabled by default

Except if one of the extensions you are using in your application requires it, JNDI is now disabled by default.

You can enable it with `quarkus.naming.enable-jndi=true`.

## Kubernetes Client / OpenShift Client

The Kubernetes Client and OpenShift Client dependency has been updated to version 5.11.0. This version of the Fabric8 client includes some minor breaking changes.

These are the most notable breakages:

- Watchers now default to request bookmarks, improving 410 exceptions (if supported by API server).
  If you are using the mock framework with explicit URIs, you may need to update your expected watch endpoints to include the parameter `allowWatchBookmarks=true` in your path expectations.
- `ExecListener` no longer passes the `okhttp3.Response` to `onOpen`.
  `onFailure` will pass a simplified `ExecListener.Response` when possible.

You can check the rest on the [Kubernetes Client 5.11.0 release notes](https://github.com/fabric8io/kubernetes-client/blob/master/CHANGELOG.md#note-breaking-changes-in-the-api-1).

## Flyway

The MariaDB/MySQL integration was moved to a separate dependency. See https://flywaydb.org/documentation/database/mysql#java-usage. MariaDB/MySQL users need to add the following dependency from now on:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

## Jib

The `quarkus-container-image-jib` extension now uses a new base image when building a container image for JVM mode.
Where previously `fabric8/java-alpine-openjdk11-jre` was being used, now if the application targets Java 17, `registry.access.redhat.com/ubi8/openjdk-17-runtime:1.11` is used as the base image, otherwise `registry.access.redhat.com/ubi8/openjdk-11-runtime:1.11` is used.

It's also worth noting that the new images do **not** run the application with the _root_ user.

Furthermore, the working directory was changed to `/home/jboss`.

## Mutiny

Some 1+ year old deprecated methods have been removed in Mutiny 1.3.0.

- `Multi.groupItems()`: use `Multi.group()` instead.
- `Multi.transform()`: use `Multi.select()` and `Multi.skip()` instead.
- `Multi.collectItems()`: use `Multi.collect()` instead.
- `Multi.onOverflow().drop(Consumer)`: use `Multi.onOverflow().invoke(Consumer).drop()` instead.
- `Uni.cache()` use `Uni.memoize()`: instead.

## Supported Maven versions

2.7.0.Final and 2.7.1.Final releases require Maven version 3.8.1 or later. 2.7.2.Final release reverts this requirement back to Maven 3.6.2 and later.

## Testing

Various changes where made to what used to the `quarkus-junit5-vertx` module.
- First of all, the name was changed to `quarkus-test-vertx`, as this module now also provides support for internal Quarkus tests (i.e the `quarkus-junit5-internal` module). 
- Furthermore, the dependency on `quarkus-junit5` was removed.
- Finally, the API changed packages (from `io.quarkus.test.junit.vertx` to `io.quarkus.test.vertx`).