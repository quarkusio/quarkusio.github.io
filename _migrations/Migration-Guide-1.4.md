---
date: 2020-04-27
---
## Multi-module projects

If your projects have multiple modules, for a number of extensions (Hibernate Validator for instance), we are not considering the combined index anymore (i.e. the one built of all the Jandex indexes present in your modules) but the bean archives index.

This change allows us to consider generated classes for these extensions.

What that means for you is that instead of generating a Jandex index via the Maven plugin in your secondary modules, add an empty `src/main/resources/META-INF/beans.xml`. This will make your classes part of the bean archives index and considered again by these extensions.

## MongoDB

We upgraded to the MongoDB client version 4.

## Gradle plugin

### JVM Arguments parameter is now a List

If you are specifying extra JVM arguments in `quarkusDev` task, you need to change from:

```gradle
tasks.withType(QuarkusDev) {
    jvmArgs = "-Djavax.net.ssl.keyStore=/path/to/keystores/keystore.jks -Djavax.net.ssl.keyStorePassword=password"
}
``` 

to:
```gradle
tasks.withType(QuarkusDev) {
    jvmArgs = ["-Djavax.net.ssl.keyStore=/path/to/keystores/keystore.jks", "-Djavax.net.ssl.keyStorePassword=password"]
}
```

### Task `buildNative` is now deprecated

We have deprecated the `./gradlew buildNative` task in favor of `./gradlew build -Dquarkus.package.type=native`. If your build still uses `buildNative` you should see the following warning message: 

```
The 'buildNative' task has been deprecated in favor of 'build -Dquarkus.package.type=native'
```

## Kogito

The Kogito extension has been moved out of the Core repository: it is now part of the Quarkus Platform.

The GA has changed so you should use `org.kie.kogito:kogito-quarkus` instead of `io.quarkus:quarkus-kogito`.

## Hibernate Search + Elasticsearch (Preview)

Configuration properties relative to Elasticsearch index lifecycle management have changed.
See [this section of the reference documentation](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-schema-management)
for detailed documentation about schema management.

In short:

* `quarkus.hibernate-search.elasticsearch.index-defaults.lifecycle.strategy`
is now `quarkus.hibernate-search.index-defaults.schema-management.strategy`
and accepts the same values as before: `none`, `create`, `validate`, `update`, ...
* A new schema management strategy on startup was introduced: `create-or-validate`.
It create indexes if they don't exist, validates their schema if they already exist.
* The default schema management strategy on startup is now `create-or-validate`.
*  `quarkus.hibernate-search.elasticsearch.index-defaults.lifecycle.required-status`
is now `quarkus.hibernate-search.elasticsearch.index-defaults.schema-management.required-status`.
*  `quarkus.hibernate-search.elasticsearch.index-defaults.lifecycle.required-status-wait-timeout`
is now `quarkus.hibernate-search.elasticsearch.index-defaults.schema-management.required-status-wait-timeout`.

Also related: you can now use [`SearchSchemaManager` APIs](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-schema-management-manager)
to trigger schema management operations at the time of your choosing (not just on startup).

## OIDC

### OidcTenantConfig package change

`OidcTenantConfig` has been moved from the `io.quarkus.oidc.runtime` package to `io.quarkus.oidc` package because `OidcTenantConfig` is used by `io.quarkus.oidc.TenantConfigResolver`.