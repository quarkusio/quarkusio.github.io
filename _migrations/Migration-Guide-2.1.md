---
date: 2021-07-29
---
## Hibernate with Panache split packages

To avoid split packages:

* `ProjectedFieldName` has been moved from `io.quarkus.hibernate.orm.panache` to `io.quarkus.hibernate.orm.panache.common`.

The original, now deprecated, classes haven been kept in 2.1 and will be removed in 2.2.

## MongoDB with Panache split packages

To avoid split packages:

* `MongoEntity` has been moved from `io.quarkus.mongodb.panache` to `io.quarkus.mongodb.panache.common`.
* `ProjectionFor` has been moved from `io.quarkus.mongodb.panache` to `io.quarkus.mongodb.panache.common`.
* `PanacheUpdate` has been moved from `io.quarkus.mongodb.panache` to `io.quarkus.mongodb.panache.common`.
* `ReactivePanacheUpdate` has been moved from `io.quarkus.mongodb.panache.reactive` to `io.quarkus.mongodb.panache.common.reactive`.

The original, now deprecated, classes haven been kept in 2.1 and will be removed in 2.2.

## Config
The `@WithName` annotation for `@ConfigMapping` now correctly used the name as is to map the configuration name and not a transformed version. Example:

```java
@ConfigMapping
interface Server {
 @WithName("theHost")
 String server();
}
```

In Quarkus `2.x`, the `theHost` name would map to the `the-host` configuration. Starting with Quarkus `2.1` it now maps with the exact name of `theHost`.

## Hibernate Reactive

Hibernate Reactive **must** run on a Vert.x event loop thread. Running it on other threads has never been supported and would likely lead to difficult to identify concurrency related problems. Prior to Quarkus 2.1, despite this fact, there was no actual check in place to enforce the rule.

Since Quarkus 2.1 there is an active guard protecting users from this mistake, so you'll see a runtime exception if you attempt to use it from the wrong thread.

We strongly recommend trying to design Reactive applications to stick to the pure event loop as that is the most efficient and best performing solution for a reactive app. If this doesn't apply to you, please consider using the classic version of Hibernate ORM which is more suited for applications which are not running on the event loop.

## OpenShift

### Java command and arguments mapping

When deploying a Quarkus application into OpenShift, by default the container configuration to run the application looked like:

```
command: java
args:
    - '-Dquarkus.http.host=0.0.0.0'
    - '-Djava.util.logging.manager=org.jboss.logmanager.LogManager'
    - '-jar'
    - /deployments/quarkus-run.jar
```

Both `command` and `args` fields can be overriden via the `quarkus.openshift.command` and `quarkus.openshift.arguments` properties. 

However, users can't simple append their custom Java arguments, for example: `java -jar /deployments/quarkus-run.jar param1 param2`. The only workaround was to copy and paste the existing content in the default `args` field and append the custom arguments `param1` and `param2`.

This has been fixed in 2.0 by moving all the Java related parameters into the `command` field:

```
command: 
    - 'java'
    - '-Dquarkus.http.host=0.0.0.0'
    - '-Djava.util.logging.manager=org.jboss.logmanager.LogManager'
    - '-jar'
    - /deployments/quarkus-run.jar
```

Now, users can use the `quarkus.openshift.arguments` property to append their custom Java arguments, for example: `quarkus.openshift.arguments=param1,param2`.


## Kubernetes Client

The Kubernetes Client has been upgraded to 5.6, which contains some minor breaking API changes.

Please refer to the release notes for versions [5.6](https://github.com/fabric8io/kubernetes-client/releases/tag/v5.6.0) and [5.5](https://github.com/fabric8io/kubernetes-client/releases/tag/v5.5.0) for more details.

## SmallRye Fault Tolerance

Quarkus 2.1 updates SmallRye Fault Tolerance to version 5.2, which introduces a special mode, **not compatible** with the MicroProfile Fault Tolerance specification. In this mode, methods with fault tolerance annotations are automatically treated as non-blocking if they have an asynchronous return type (`CompletionStage` or `Uni`), even if they don't have any asynchronous annotation (`@Asynchronous`, `@Blocking`, `@NonBlocking`). If an annotation is present, it is of course still honored.

Quarkus enables this non-compatible mode by default. If you need strict compatibility with MicroProfile Fault Tolerance, you can set `smallrye.faulttolerance.mp-compatibility=true`. For more information, see https://smallrye.io/docs/smallrye-fault-tolerance/5.2.1/usage/extra.html#_non_compatible_mode

## Quarkus Universe BOM is deprecated

In the Quarkus 2.1.0.Final release the `io.quarkus:quarkus-universe-bom` typically imported by Quarkus applications is deprecated in favor of the Quarkus platform member-specific BOMs:

* io.quarkus.platform:quarkus-bom:2.1.0.Final
* io.quarkus.platform:quarkus-optaplanner-bom:2.1.0.Final
* io.quarkus.platform:quarkus-kogito-bom:2.1.0.Final
* io.quarkus.platform:quarkus-qpid-jms-bom:2.1.0.Final
* io.quarkus.platform:quarkus-cassandra-bom:2.1.0.Final
* io.quarkus.platform:quarkus-camel-bom:2.1.0.Final
* io.quarkus.platform:quarkus-hazelcast-client:2.1.0.Final
* io.quarkus.platform:quarkus-debezium-bom:2.1.0.Final
* io.quarkus.platform:quarkus-blaze-persistence-bom:2.1.0.Final
* io.quarkus.platform:quarkus-google-cloud-services-bom:2.1.0.Final

The platform member-specific BOMs above are actually fragments of the legacy `io.quarkus:quarkus-universe-bom` and so it doesn't matter in which order they are imported in applications. The main advantage is that instead of enforcing all the dependency version constraints from the Quarkus "universe" BOM, applications may import only those constraints that are actually relevant to them. You can read more about this change in [the blog post introducing this change](https://quarkus.io/blog/quarkus-2x-platform-quarkiverse-registry/).

NOTE: that the Quarkus dev tools, including [code.quarkus.io](https://code.quarkus.io), will now be generating projects importing the BOMs that are relevant to the extensions selected for the project to be created.

The `io.quarkus:quarkus-universe-bom` is still going to be released in the upcoming versions of the Quarkus platform in addition to the member-specific BOMs to give time for the existing applications to migrate to the new BOMs.

## Quartz

The name of a job trigger stored in the database does not have the `_trigger` suffix anymore. As a result you'll need to either clear the Quartz trigger tables or append the `_trigger` suffix to the relevant `@Scheduler#identity()` value when migrating an application running on previous versions of Quarkus. No change is required if the `ram` job store is used, i.e. `quarkus.quartz.store-type=ram`.
