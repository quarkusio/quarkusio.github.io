---
date: 2020-09-15
---
## Datasource configuration

This is the last call to migrate your deprecated datasource configuration to the new one before it gets removed in Quarkus 1.9 - the new datasource configuration was introduced in Quarkus 1.3.

So if you are still using `quarkus.datasource.url` or `quarkus.datasource.driver`, please refer to [the datasource guide](https://quarkus.io/guides/datasource) to migrate your configuration.

## Hibernate ORM

If you are using a `persistence.xml` to define your persistence units and want to enable entity class scanning, you need to add:
```
<property name="hibernate.archive.autodetection" value="class" />
```
to your `persistence.xml`.

## Quartz

The configuration for Quartz extension points (job listeners, trigger listeners, plugins) has changed.

If you are using this advanced feature, please refer to the [updated Quarz configuration reference](https://quarkus.io/guides/quartz#quartz-configuration-reference) to migrate your existing configuration.

## Hibernate Search + Elasticsearch (Preview)

* Configuration properties for index defaults can no longer include the `.index-defaults` component. For example,  `quarkus.hibernate-search.elasticsearch.index-defaults.schema-management.required-status` should now be written simply as `quarkus.hibernate-search.elasticsearch.schema-management.required-status`.
* `query.explain()` now expects to be passed an entity ID instead of a document ID. In short, if your entity has an ID of type `Long`, you need to pass a `Long` (previously you had to pass a `String`).
* `@IndexedEmbedded.storage`/`ObjectFieldStorage` have been renamed to `@IndexedEmbedded.structure`/`ObjectStructure`. The older syntax is deprecated and will eventually be removed.
* `quarkus.hibernate-search.elasticsearch.default-backend` no longer exists. You can either configure a default backend (`quarkus.hibernate-search.elasticsearch.someProperty someValue`) or named backends (`quarkus.hibernate-search.elasticsearch.backends."backend-name".someProperty someValue`), but a named backend can no longer be considered as the default backend.

## Kubernetes Config

* When setting `quarkus.kubernetes-config.secrets.enabled=true`, it is now no longer necessary to also set `quarkus.kubernetes-config.enabled=true` in order to read Secrets.

## Jib

* The quarkus-container-image-jib extension no longer sets the `quarkus.http.host` System Property to `0.0.0.0` because Quarkus defaults to `0.0.0.0` (which has been the case for a very long time). This change means that if `quarkus.http.host` is set in `application.properties`, then the container image created by Jib **will** honor the value - as opposed the previous behavior, which was that value being overriden by the `quarkus.http.host` System Property.
