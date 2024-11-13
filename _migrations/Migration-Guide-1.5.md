---
date: 2020-07-03
---

## Qute

`io.quarkus.qute.api.VariantTemplate` was removed from the API. The functionality is now provided by an injected `io.quarkus.qute.Template`. Injected templates do not support the `getExpressions()`, `getGeneratedId()` and `getVariant()` methods.

## Hibernate Search + Elasticsearch (Preview)

`ElasticsearchSearchQuery#explain(String, String)` now expects the name of the mapped type to be passed as the first argument, instead of the index name.

## Testcontainers

The `org.testcontainers` artifacts are no longer in our BOM.
We still use them under the hood but we don't want to enforce the version of this test dependency on our users.

The main consequence of this change is that you will have to define the Testcontainers artifacts versions in your projects.