---
date: 2022-12-14
---

## Hibernate ORM - IN clause parameter padding

The Hibernate ORM extension now enables IN clause parameter padding by default, improving the caching of queries containing IN clauses.

You can disable this behavior by setting `quarkus.hibernate-orm.query.in-clause-parameter-padding` to `false`.

## REST Data with Panache changes

The `quarkus-hibernate-orm-rest-data-panache` and `quarkus-hibernate-reactive-rest-data-panache` extensions now support querying by entity fields and entity-named queries. To see more information about this change, you can go to section [Query parameters to list entities](https://quarkus.io/version/main/guides/rest-data-panache#query-parameters-to-list-entities) of the extension guide.

Before these changes, the auto-generated resources were internally calling the `PanacheRepositoryBase.findAll()` and `PanacheRepositoryBase.findAll(sort)` methods. And after these changes, the auto-generated resources are now calling the `PanacheRepositoryBase.find(query, params)` and `PanacheRepositoryBase.find(query, sort, params)` methods. This is totally transparent for users, so they don't need to migrate anything. However, if you were overriding either the `findAll()` or the `findAll(sort)` methods of the entity repository, these methods won't be called by the auto-generated resources any longer. Instead, you should now use the `PanacheRepositoryBase.findAll()` and `PanacheRepositoryBase.findAll(sort)` methods.

## CORS Filter returns HTTP 403 for failed preflight requests

CORS filter used to return HTTP `200` status code and no `Access-Control-Allow-Origin` header when CORS preflight requests were failing. Now, HTTP `403` will be returned instead of `200` as well.
Please update your tests which expect HTTP `200` status code for the failed preflight requests to check for HTTP `403` status code instead.
