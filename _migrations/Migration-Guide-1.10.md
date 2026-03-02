---
date: 2020-10-25
---
## Hibernate ORM

* `quarkus.hibernate-orm.log.bind-param` is deprecated and has been renamed `quarkus.hibernate-orm.log.bind-parameters`. The former will be removed at a later stage.

## Hibernate Search ORM + Elasticsearch (Preview)

* You should update your Maven/Gradle dependencies: replace any occurrence of the artifact ID `hibernate-search-elasticsearch` with `hibernate-search-orm-elasticsearch`
* You should update your configuration: replace any occurrence of the prefix `quarkus.hibernate-search.` with `quarkus.hibernate-search-orm.
* Many deprecated methods and classes were removed. For more information: https://in.relation.to/2020/11/04/hibernate-search-6-0-0-CR1/#breaking_changes
* Async/reactive methods now return `CompletionStage` instead of `CompletableFuture`. To convert a `CompletionStage` to a `Future`, call `.toCompletableFuture()`.`

## MongoDB

* The name of the default client is now `<default>` instead of the previous `__default__` to be more consistent with the rest of the code base. It shouldn't have too many consequences but typically the health checks now expose the new name.