---
date: 2020-03-19
---
## Maven

You need to upgrade Maven to 3.6.2+ due to a bug in Maven resolver.

## JUnit

You need JUnit 5.6+. It's included in our BOM but if you specify the version manually, you need to upgrade.

## REST Assured

REST Assured was updated to 4.2.

If you are tuning the Jackson `ObjectMapper`, you need to change:
```java
RestAssured.objectMapper(objectMapper);
```
to:
```java
RestAssured.config().getObjectMapperConfig().defaultObjectMapper(objectMapper);
```
as they removed the shortcut.

## Class Loading

The class loading model for dev and test mode has changed, which should fix a number of issues around class loading that have been reported. Most applications should continue to work as normal with no changes required, however there are some changes that may effect testing. The most visible change is that `@BeforeAll` methods are now always run after Quarkus has started, so they can't be used to start resources Quarkus needs before boot. If you need to start resources before Quarkus has booted you can use the `@QuarkusTestResource` annotation to specify a `QuarkusTestResourceLifecycleManager` that can start and stop the resources.

## Native images and GraalVM

19.3.1 and 20.0.0 are supported. Older versions are not.

Both JDK 8 and JDK 11 should work but keep in mind that GraalVM JDK 11 is a tech preview.

## Hibernate Validator

If you use Hibernate Validator in a multi-module project with constrained beans being in a library module, you need to be sure this library module is included in the bean archive index by adding a `src/main/resources/META-INF/beans.xml` empty file.

Otherwise your constraints will be ignored.

## Hibernate ORM and Hibernate ORM with Panache

Bytecode-enhanced bi-directional association management is no longer activated.
It was not comprehensive and we decided to embrace the principle of least surprise and disable it.
Make sure to update both sides of your relationships explicitly in your code.

[Some info about Bytecode-enhanced bi-directional association management](https://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#BytecodeEnhancement-dirty-tracking-bidirectional)

## Reactive Messaging

The `Emitter` interface has slightly changed. First, the interface has moved to `org.eclipse.microprofile.reactive.messaging.Emitter`, as the interface is now part of the Reactive Messaging specification.

Also, sending `Message` has changed. The Emitter generic parameter is the payload type and cannot be the `Message` type:

```
// Before
@Inject @Channel("channel") Emitter<Message<String>> emitter;

// Now
@Inject @Channel("channel") Emitter<String> emitter;
emitter.send("foo");
emitter.send(Message.of("foo"));
```

Sending a payload returns a `CompletionStage` completed when the message is acknowledged. 

## Reactive Messaging Kafka Message -> Record 

The `KafkaMessage` class has been renamed into `KafkaRecord` to be closer to the Kafka lingo.

The `@Stream` annotation has been replaced by the `@Channel` annotation.

## Mutiny

In 1.3, a new reactive programming API has been introduced. This API named Mutiny (https://smallrye.io/smallrye-mutiny/) replaces the Axle and Reactive Streams Operators models (Reactive Streams and Completion Stage). The previous models are still functional, but deprecated and will be removed in the future. Refer to the various guides to show the differences and use the new API.

## Hibernate Search + Elasticsearch (Preview)

### Search DSL

A few methods in the Search DSL have been renamed:

* `searchSession.search(...).predicate(...)` becomes `searchSession.search(...).where(...)`.
* `searchSession.search(...).asProjection(...)` becomes `searchSession.search(...).select(...)`.
* `searchSession.search(...).asProjections(...)` becomes `searchSession.search(...).select(...)`.
* `searchSession.search(...).asEntity(...)` becomes `searchSession.search(...).selectEntity()`.
* `searchSession.search(...).asEntityReference(...)` becomes `searchSession.search(...).selectEntityReference()`.

The methods with the old name are still present, though deprecated.
They will be removed before Hibernate Search 6.0.0.Final is released.

### Index layout

Elasticsearch indexes are now accessed through aliases only.
Hibernate Search relies on two aliases for each index: one for writes (`<myindex>-write`) and one for reads (`<myindex>-read`).

As a result, you will need to either create the two aliases,
or drop your indexes and have Hibernate Search re-create empty indexes on startup: it will create the aliases.

See [this section of the reference documentation](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#backend-elasticsearch-indexlayout)
for more information about index layout.

### Configuration

The names of synchronization strategies for automatic indexing have changed:

* `queued` => `async`.
* `committed` (the default) => `write-sync` (still the default).
* `searchable` (commonly used for tests) => `sync` (still recommended for tests only).

So if your configuration looked like this:

```
quarkus.hibernate-search.automatic_indexing.synchronization.strategy = queued
```

You should now have this:

```
quarkus.hibernate-search.automatic_indexing.synchronization.strategy = async
```

And if it looked like this:

```
quarkus.hibernate-search.automatic_indexing.synchronization.strategy = searchable
```

You should now have this:

```
quarkus.hibernate-search.automatic_indexing.synchronization.strategy = sync
```

See [this section of the reference documentation](https://docs.jboss.org/hibernate/search/6.0/reference/en-US/html_single/#mapper-orm-indexing-automatic-synchronization) for more information about synchronization strategies.

## Kubernetes

In 1.3, the configuration properties of the Kubernetes extension have changed in the following ways:

* They are now prefixed with `quarkus.`
* Nested property arrays have been converted to maps.


For example the old property `kubernetes.replicas` is now
`quarkus.kubernetes.replicas`. This applies to all properties starting with
`kubernetes`, `openshift` and `knative`. For compatibility reasons the old properties will still work but the new ones will always take precedence.

The other change affects properties that contained nested arrays,
like `env-vars` and `labels`:

For example, labels used to be configured like:

```
kubernetes.labels[0].name=foo
kubernetes.labels[0].value=bar
```

But the new properties are configured like:

```
quarkus.kubernetes.labels.foo=bar
```

The same applies to complex objects too:

```
kubernetes.env-vars[0].name=FOO_
kubernetes.env-vars[0].value=BAR

kubernetes.env-vars[0].name=FOO2
kubernetes.env-vars[0].secret=bar-secret

kubernetes.env-vars[0].name=FOO3
kubernetes.env-vars[0].config-map=bar-cfg
```

They can now be configured:

```
quarkus.kubernetes.env-vars.foo=value
quarkus.kubernetes.env-vars.foo2.secret=bar-secret
quarkus.kubernetes.env-vars.foo3.config-map=bar-cfg
```

## Deprecated classes removed

The following unused and long-time deprecated classes were deleted from the codebase:

* `io.quarkus.test.junit.SubstrateTest`(Replaced with `io.quarkus.test.junit.NativeImageTest`)
* `io.quarkus.test.junit.DisabledOnSubstrate` (Replaced with `io.quarkus.test.junit.DisabledOnNativeImage`)
* `io.quarkus.test.junit.DisabledOnSubstrateCondition` (Replaced with `io.quarkus.test.junit.DisabledOnNativeImageCondition`)
* `io.quarkus.infinispan.client.runtime.Remote`
*  All classes in package `io.quarkus.deployment.builditem.substrate` (Replaced with `io.quarkus.deployment.builditem.nativeimage`)

More information: https://github.com/quarkusio/quarkus/pull/5596