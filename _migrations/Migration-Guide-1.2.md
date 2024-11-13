---
date: 2020-01-22
---
## Hibernate ORM

In Quarkus, the bytecode enhancement of Hibernate ORM is always on.
We used to enable the automatic association management but there are cases where it doesn't work well.

It is now disabled. You have to manage both sides of an association manually from now on (as with a classic configuration of Hibernate ORM).

## MongoDB

We switched from `mongo-java-driver` to `mongodb-driver-sync` which means that the legacy `com.mongodb.MongoClient` is not there anymore and should be replaced by `com.mongodb.client.MongoClient`.

## Native images and GraalVM

You can either use:

 * GraalVM 19.2.1
 * or GraalVM 19.3.1 with JDK 8 or JDK 11

GraalVM 19.3.0.x is not supported anymore.

## Hibernate Search + Elasticsearch (Preview)

The protocol used to contact Elasticsearch is now specified in a separate configuration property, `quarkus.hibernate-search.elasticsearch.protocol`. This property defaults to `http`. 

As a result, the property `quarkus.hibernate-search.elasticsearch.discovery.default_scheme` was removed.

So if your configuration looked like this:
```
quarkus.hibernate-search.elasticsearch.hosts = http://es1.mycompany.com
```

You should now have this:
```
quarkus.hibernate-search.elasticsearch.hosts = es1.mycompany.com
```

And if it looked like this:
```
quarkus.hibernate-search.elasticsearch.hosts = https://es1.mycompany.com
quarkus.hibernate-search.elasticsearch.discovery.enabled = true
quarkus.hibernate-search.elasticsearch.discovery.default_scheme = https
```

You should now have this:
```
quarkus.hibernate-search.elasticsearch.hosts = es1.mycompany.com
quarkus.hibernate-search.elasticsearch.protocol = https
quarkus.hibernate-search.elasticsearch.discovery.enabled = true
```

## @ConfigProperties

In previous versions of Quarkus, `@ConfigProperties` made the assumption that the name of the class fields (or method names when talking about interfaces) would exactly match to the property name.
For example:

```
@ConfigProperties
public class SomeConfig {
    public String fooBar;
}
```

meant that setting `fooBar` would require the `some.fooBar` property.

This behavior is unintuitive however, so we have changed it to use kebab-case properties instead, which means that you now need to set the `some.foo-bar` property.

If keeping the old behavior is desired, there are two options:

* Set `@ConfigProperties(namingStrategy=NamingStrategy.VERBATIM)` for each class that uses `ConfigProperties` and for which the old behavior is desired
* Set the `quarkus.arc.config-properties-default-naming-strategy=verbatim` property. This makes the old behavior the default for `@ConfigProperties` classes.

## Test framework

The `quarkus-vault-test` artifact has been renamed to `quarkus-test-vault` to be more consistent with our other test framework artifacts.