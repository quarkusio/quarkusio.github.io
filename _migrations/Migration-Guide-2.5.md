---
date: 2021-11-24
---
## Flyway

We upgraded the Flyway dependency from 7.15 to version 8.0. Please refer to the [Flyway release notes](https://flywaydb.org/documentation/learnmore/releaseNotes#8.0.1) to see all the changes. 

One important breaking change is that Flyway deprecated the Community support for database versions older than 5 years, for example: MySQL 5.7 and PostgreSQL 9.6. Therefore, users using the Flyway community and an unsupported database will have a failure at startup similar to:

```
Caused by: org.flywaydb.core.internal.license.FlywayEditionUpgradeRequiredException: Flyway Teams Edition or MySQL upgrade required: 
MySQL 5.7 is no longer supported by Flyway Community Edition, but still supported by Flyway Teams Edition.
```

## Hibernate with Panache split packages

To avoid split packages, the following classes was deprecated in 2.1 and are now removed:

* `io.quarkus.hibernate.orm.panache.ProjectedFieldName` has been removed in favor of  `io.quarkus.hibernate.orm.panache.common.ProjectedFieldName`.

## MongoDB with Panache split packages

To avoid split packages, the following classes was deprecated in 2.1 and are now removed:

* `io.quarkus.mongodb.panache.MongoEntity` has been removed in favor of `io.quarkus.mongodb.panache.common.MongoEntity`.
* `io.quarkus.mongodb.panache.ProjectionFor` has been removed in favor of `io.quarkus.mongodb.panache.common.ProjectionFor`.
* `io.quarkus.mongodb.panache.PanacheUpdate` has been removed in favor of `io.quarkus.mongodb.panache.common.PanacheUpdate`.
* `io.quarkus.mongodb.panache.reactive.ReactivePanacheUpdate` has been removed in favor of `io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate`.

## Vert.x

The `quarkus-vertx` extension no longer depends on the `quarkus-jackson` extension.
As a result, if your application depends on `quarkus-vertx` and makes use of the `io.vertx.core.json.JsonObject.mapFrom()` or the `io.vertx.core.json.JsonObject.mapTo()` methods then the `quarkus-jackson` extension should be added (either manually or as a transient dependency) to the project dependencies.


Quarkus 2.5 uses Vert.x 4.2.1. This release contains a few breaking changes. While some of them have been mitigated, you may use removed APIs:

#### Incompatible changes in vertx-core

| Element | Classification | Description |
| ------- | :------------- | :---------- |
| `Uni<MessageConsumer<T>> MessageConsumer<T>::registration()` | BREAKING | Method was removed.|
| `MessageConsumer<T> MessageConsumer<T>::registrationAndAwait()` | BREAKING | Method was removed.|
| `MessageConsumer<T> MessageConsumer<T>::registrationAndForget()` | BREAKING | Method was removed.|


#### Incompatible changes in vertx-json-schema

| Element | Classification | Description |
| ------- | :------------- | :---------- |
| `SchemaRouter SchemaRouter::create(HttpClient, FileSystem, SchemaRouterOptions)` | BREAKING | The number of parameters of the method have changed. |


#### Incompatible changes in vertx-redis-client

If you use the Quarkus Redis extension, these changes have been mitigated. However, the underlying Redis client contains the following breaking changes:

| Element | Classification | Description |
| ------- | :------------- | :---------- |
| `Uni<Response> RedisAPI::lpop(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `Response RedisAPI::lpopAndAwait(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `void RedisAPI::lpopAndForget(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `Uni<Response> RedisAPI::psync(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `Response RedisAPI::psyncAndAwait(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `void RedisAPI::psyncAndForget(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `Uni<Response> RedisAPI::rpop(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `Response RedisAPI::rpopAndAwait(===String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `void RedisAPI::rpopAndForget(===java.lang.String===)` |  BREAKING | The type of the parameter changed from 'String' to 'List<String>'. |
| `void RedisConnection::close()` | BREAKING | The return type changed from 'void' to  `Uni<Void>`. |


#### Incompatible changes in vertx-auth-oauth2

| Element | Classification | Description |
| ------- | :------------- | :---------- |
| class `AccessToken` | BREAKING | The class has been removed |
| class `KeycloakHelper` | BREAKING | The class has been removed |
| class `OAuth2RBAC` | BREAKING | The class has been removed |
| class `OAuth2Response` | BREAKING | The class has been removed |
| class `KeycloakRBAC` | BREAKING | The class has been removed |
| class `MicroProfileRBAC` | BREAKING | The class has been removed |
| `OAuth2FlowType OAuth2Auth::getFlowType()` | BREAKING | Method was removed |
| `OAuth2FlowType OAuth2Auth::introspectToken(String)` | BREAKING | Method was removed |
| `Uni<AccessToken> OAuth2Auth::introspectToken(String, String)` | BREAKING | Method was removed |
| `Uni<AccessToken> OAuth2Auth::introspectToken(String)` | BREAKING | Method was removed |
| `AccessToken OAuth2Auth::introspectTokenAndAwait(String, String)` | BREAKING | Method was removed |
| `AccessToken OAuth2Auth::introspectTokenAndAwait(String)` | BREAKING | Method was removed |
| `OAuth2Auth OAuth2Auth::introspectTokenAndForget(String, String)` | BREAKING | Method was removed |
| `OAuth2Auth OAuth2Auth::introspectTokenAndForget(String)` | BREAKING | Method was removed |
| `Uni<Void> OAuth2Auth::loadJWK(String)` | BREAKING | Method was removed |
| `void OAuth2Auth::loadJWKAndAwait(String)` | BREAKING | Method was removed |
| `OAuth2Auth OAuth2Auth::loadJWKAndForget(String)` | BREAKING | Method was removed |
| `OAuth2Auth OAuth2Auth::rbacHandler(OAuth2RBAC)` | BREAKING | Method was removed |


#### Incompatible changes in vertx-web-validation

| Element | Classification | Description |
| ------- | :------------- | :---------- |
| `ValidationHandlerBuilder ValidationHandler::builder(SchemaParser)` | BREAKING | The return type changed from 'io.vertx.ext.web.validation.builder.ValidationHandlerBuilder' to  'io.vertx.mutiny.ext.web.validation.builder.ValidationHandlerBuilder'.|


## Qute

### Scanning

The `templates` directories are also scanned in all application archives and in extension runtime modules.
Previously, only the `templates` directory from the application root was considered.
Note that it's possible to exclude any file from any `templates` directory via the `quarkus.qute.template-path-exclude` configuration property. 
Excluded files are neither parsed nor validated during build and are not automatically available at runtime.

### Iteration Metadata Prefix

The keys used to access the iteration metadata inside a loop cannot be used directly anymore.
Instead, a prefix is used to avoid possible collisions with variables from the outer scope.
By default, the alias of an iterated element suffixed with an underscore is used as a prefix.
For example, the `hasNext` key must be prefixed with `it_` inside an `{#each}` section: `{it_hasNext}`.
And must be used in a form of `{item_hasNext}` inside a `{#for}` section with the `item` element alias.

The prefix is configurable either via `EngineBuilder.iterationMetadataPrefix()` for standalone Qute or via the `quarkus.qute.iteration-metadata-prefix` configuration property in a Quarkus application. Three special constants can be used: 

1. `<alias_>` - the alias of an iterated element suffixed with an underscore is used (default); e.g. `{item_hasNext}`
2. `<alias?>` - the alias of an iterated element suffixed with a question mark is used; e.g. `{item?hasNext}`
3. `<none>` - no prefix is used - the default behavior in previous versions; e.g. `{hasNext}`


## REST Data Panache

`quarkus-hibernate-orm-rest-data-panache`, `quarkus-spring-data-rest` and `quarkus-mongodb-rest-data-panache` can now work with both RESTEasy Classic (i.e. `quarkus-resteasy`) and RESTEasy Reactive (i.e. `quarkus-resteasy-reactive`) and this no longer have a hard dependency on `quarkus-resteasy`.
Users of these dependencies will now have to explicitly add `quarkus-resteasy` or `quarkus-resteasy-reactive` to their applications.

## Spring Web

`quarkus-spring-web` now works with both RESTEasy Classic (i.e. `quarkus-resteasy`) and RESTEasy Reactive (i.e. `quarkus-resteasy-reactive`) and this no longer have a hard dependency on `quarkus-resteasy-jackson`.
Users of `quarkus-spring-web` will now have to explicitly add `quarkus-resteasy-jackson` or `quarkus-resteasy-reactive-jackson` to their applications.