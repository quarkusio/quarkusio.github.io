---
date: 2021-01-20
---
## Log min-level

If you are using TRACE (or DEBUG too if using 1.11.0.Final or 1.11.1.Final - changed in 1.11.2.Final) log level, we made an important change in our logging layer: a new build-time `min-level` configuration property was introduced that sets the minimum log level you will be able to use at runtime.

So if you are logging at TRACE level for some loggers, setting `min-level` to TRACE is required.

## Non-application endpoints moved to their own namespace

[Path resolution for configurable endpoints](https://quarkus.io/blog/path-resolution-in-quarkus/) changed in this release. This transition was a little rough. There are some differences in behavior between 1.11.0.Final, when this was introduced, and 1.11.5.Final, when issues were resolved. 

By default, non-application endpoints, like health and metrics, are now grouped under `/q`.

Convenience redirects from previous URLs to new namespaced URLs can be enabled and disabled by setting:  
`quarkus.http.redirect-to-non-application-root-path=false`

Disable the Quarkus non-application endpoints by setting the non-application endpoint root to be the same as the http root: 
`quarkus.http.non-application-root-path=${quarkus.http.root-path}`

You can customize the root used for non-application endpoints by setting `quarkus.http.non-application-root-path` to an alternative path. 

As of 1.11.5.Final, leading slashes in configured paths are significant. Please see [Path resolution in Quarkus](https://quarkus.io/blog/path-resolution-in-quarkus/) for more details and examples.

## Jackson

The default `ObjectMapper` obtained via CDI and consumed by the Quarkus extensions now ignores unknown properties (by disabling the `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` feature).

See https://quarkus.io/guides/rest-json#jackson for more details about how to go back to the previous behavior.

## Kubernetes Client

We upgraded the Kubernetes Client to version 5. Please refer to the [Kubernetes Client migration guide](https://github.com/fabric8io/kubernetes-client/blob/master/doc/MIGRATION-v5.md) for more information.

## Hibernate Search ORM + Elasticsearch

* The default required status for Elasticsearch indexes is now `yellow`. If you have specific requirements and need to wait for indexes to be `green` on startup, set `quarkus.hibernate-search.elasticsearch.schema-management.required-status` to `green`.
* [Queries](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#troubleshooting-logging-query)
and [requests](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#troubleshooting-logging-elasticsearch-request)
are now logged at the `TRACE` level instead of the `DEBUG` level.

## MongoDB Panache

* A recent change was made to MongoDB panache to bring it in to conformity with the Hibernate Panache behavior.  Public field accesses on `MongoEntity` and `ReactiveMongoEntity` types are now wrapped with the appropriate `get` or `set` methods.  In general, you will like not see any difference in your application.  However, if you have written a custom `get` or `set` method you may notice a change in behavior if those custom methods deviate from the standard `get`/`set` paradigm.  See [this issue](https://github.com/quarkusio/quarkus-quickstarts/pull/726) for an example of something you might run in to.

## quarkus-smallrye-jwt-build

A new `quarkus-smallrye-jwt-build` extension has been introduced allowing users to generate JWT tokens without having to depend on `quarkus-smallrye-jwt` extension which is used for verifying JWT tokens.

## GraalVM 20.3

We upgraded the base container image to build native executables to GraalVM 20.3.

However, we hit a regression in the ImageIO support so if you are using ImageIO and seeing errors such as `UnsatisfiedLinkError: no awt in java.library.path`. This regression is specific to GraalVM 20.3.0 and will be fixed in GraalVM 20.3.1. Your options are:

* Go back to GraalVM 20.2 until we upgrade to GraalVM 20.3.1: `quarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-native-image:20.2.0-java11`
* Use [Mandrel](https://github.com/graalvm/mandrel/releases) in which we backported some additional ImageIO support from GraalVM master: `quarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:20.3-java11`