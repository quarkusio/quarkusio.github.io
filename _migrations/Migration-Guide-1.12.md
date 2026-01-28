---
date: 2021-02-23
---
## Fast-jar as default

Fast-jar is a new Quarkus application packaging format that is faster to boot, compared to our legacy jar packaging. It was introduced several versions ago and it brings many improvements that made us make it the new default.

### Starting the application

The biggest change here is that to start your Quarkus application, you should now use:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

(instead of using the versioned `-runner` jar)

This change will concern all your applications as soon as you have upgraded them to 1.12.

When deploying your application, make sure to deploy the entire `quarkus-app` directory.

For those who want to stick to the legacy jar packaging, they can go back to the previous behavior by adding the following property to the `application.properties`:

```properties
quarkus.package.type=legacy-jar
```

### Dockerfiles

For existing applications, you have two Dockerfiles:

- `Dockerfile.jvm`: this is the one for the `legacy-jar` packaging
- `Dockerfile.fast-jar`: this is the one for `fast-jar` packaging (so the new default)

For newly generated applications, the situation is a bit different:

- `Dockerfile.jvm`: this is the one for the `fast-jar` packaging (so the new default)
- `Dockerfile.legacy-jar`: this is the one for `legacy-jar`

Note that if you want all your applications to be consistent, you can just update the Dockerfiles of your existing applications with the ones of a newly generated project.

You can find an example of the new Fast jar Dockerfile [here](https://github.com/quarkusio/quarkus-quickstarts/blob/master/validation-quickstart/src/main/docker/Dockerfile.jvm).

## Quarkus Maven Plugin

We cleaned up a few things in the Quarkus Maven Plugin. Make sure the `quarkus-maven-plugin` section of the `pom.xml` of your project looks like:

```xml
<plugin>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-maven-plugin</artifactId>
  <version>${quarkus-plugin.version}</version>
  <extensions>true</extensions>
  <executions>
    <execution>
      <goals>
        <goal>build</goal>
        <goal>generate-code</goal>
        <goal>generate-code-tests</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## Mutiny 

Mutiny has deprecated a few APIs. The deprecated APIs are still available and would work, but are planned for removal. 

Changed APIs are:

* `multi.collectItems()` -> `multi.collect()`
* `multi.groupItems()` -> `multi.group()`
* `multi.transform().byTakingFirstItems(long)/byTestingItemsWith()/byFilteringItemsWith()` -> `multi.select().first(long)`, `multi.select().when(Function<T, Uni<?>>)`, `multi.select().where(Predicate<T>)`
* `multi.transform().toHotStream()` -> `multi.toHotStream()`
* `multi.transform().bySkippingFirstItems(long)` -> `multi.skip().first(long)`

Mutiny removes two methods (deprecated for 11 months):

* `uni.subscribeOn` -> `uni.runSubscriptionOn()`
* `multi.subscribeOn` -> `multi.runSubscriptionOn()`


## Mailer

* The `MailTemplateInstance` now returns a `Uni<Void>` instead of `CompletionStage<Void>`. To convert a `Uni` to a `CompletionStage`, call `.subscribeAsCompletionStage()`.

## Vert.x

* The Axle and RX Java API from Vert.x are no more exposed, use the Mutiny API instead. These features were deprecated since February 2020. 

## HTTP 

A couple of the default file upload settings have changed.

`quarkus.http.body.delete-uploaded-files-on-end` now defaults to `true` (the reason being that no uploaded files should be left over on the server by default).
`quarkus.http.body.uploads-directory` now defaults to `${java.io.tmpdir}/uploads` (the reason being that some application might not have the permissions to write to the current working directory)


[Path resolution for configurable endpoints](https://quarkus.io/blog/path-resolution-in-quarkus/) changed in this release.  

By default, non-application endpoints, like health and metrics, are now grouped under `/q`.

Convenience redirects from previous URLs to new namespaced URLs can be enabled and disabled by setting:  
`quarkus.http.redirect-to-non-application-root-path=false`

Disable the Quarkus non-application endpoints by setting the non-application endpoint root to be the same as the http root: 
`quarkus.http.non-application-root-path=${quarkus.http.root-path}`

You can customize the root used for non-application endpoints by setting `quarkus.http.non-application-root-path` to an alternative path. 

As of 1.12.1.Final, leading slashes in configured paths are significant. Please see [Path resolution in Quarkus](https://quarkus.io/blog/path-resolution-in-quarkus/) for more details and examples.

## Rest Client Exceptions

If you are using `quarkus-rest-client` then please be aware that MP REST Client or JAX-RS Client invocation exceptions will no longer have JAX-RS `Response` available by default to avoid leaking some potentially sensitive downstream endpoint data such as cookies. You can set `resteasy.original.webapplicationexception.behavior=true` in `application.properties` if you need to access the exception `Response`.
Please see https://docs.jboss.org/resteasy/docs/4.5.9.Final/userguide/html/ExceptionHandling.html#ResteasyWebApplicationException for more information.

## Version of distroless image

If you are using the distroless image, please note that it now has an immutable version so you might have to update your Dockerfiles to use: `quay.io/quarkus/quarkus-distroless-image:1.0` instead of `quay.io/quarkus/quarkus-distroless-image:20.3-java11`.


## Kafka Health Check

Readiness health checks for _incoming_ Kafka channel may report _DOWN_ if they are consumed lazily (like using SSE). To avoid this, disable the readiness health check:

```
mp.messaging.incoming.your-topic.health-readiness-enabled=false
```