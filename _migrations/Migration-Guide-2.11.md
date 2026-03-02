---
date: 2022-07-27
---
## GraphQL endpoints are singleton by default

Previously, the default scope attached to a `@GraphQLApi` endpoint was `@Dependent`. Now, unless you explicitly add a scope annotation to your endpoint, it will act as a `@Singleton` bean. This is thus aligned with the default behavior of JAX-RS endpoints.

## Unification of database dev services configuration

All database Dev Services now use the same database name, username and password:

- Database name: `quarkus`
- Username: `quarkus`
- Password: `quarkus`

Except for Microsoft SQL Server, for which both the database and Testcontainers are less flexible:

- Database name: none
- Username: `SA`
- Password: `Quarkus123`

## `jsoup` removed from `quarkus-bootstrap-bom`

As a result of updating to Maven 3.8.6, `jsoup` was removed from `quarkus-bootstrap-bom` (`quarkus-bom` respectively).
If you are using `jsoup`, you will have to add an explicit version to the respective dependency declaration.

## New Redis API

Quarkus 2.11 provides a new Redis API.
Introducing this API has introduced a few breaking changes:

* Host Providers are now identified using `@Identifier` instead of `@Named`
* It is not possible to create dynamic clients - this was done to support pub/sub, which is now supported by the API

We recommend migrating to this new API. The old one is still available, but has been deprecated and will be removed at some point.

To migrate to the new API, we recommend injecting the `RedisDataSource` (or `ReactiveRedisDataSource`) in a constructor and then, retrieving the _group_ of commands you need in your application:

```java
private final KeyCommands<String> keyCommands;
private final StringCommands<String, Long> countCommands;

public IncrementService(RedisDataSource ds) {
        countCommands = ds.string(Long.class);
        keyCommands = ds.key(); 
}
```

## Qute

In previous versions, `{[` and `]}` could be used to mark the content that should be rendered but not parsed. This syntax is now removed due to common collisions with constructs from other languages. The unparsed character data can only start with the sequence `{|` and it ends with the sequence `|}`.

## Old Liquibase xsd files removed from native image

`dbchangelog-*.xsd` and `liquibase-pro-*.xsd` files older than 4.7 are not added to native image anymore.
If you are still referencing such old xsd files in your changelog xml files then it's time to update these `schemaLocations` to 4.13, e.g.:
```xml
<databaseChangeLog
        ...
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.13.xsd">
```