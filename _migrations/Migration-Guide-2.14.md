---
date: 2022-11-09
---
## GraalVM CE / Mandrel migration from 22.2 to 22.3

Quarkus 2.14.0.Final still defaults to GraalVM CE / Mandrel 22.2 but, starting with 2.14.1.Final, Quarkus will default to using GraalVM CE / Mandrel 22.3 for generating native executables.

### Substitution (and other) annotations

Starting with 22.3, substitution (and other) annotations located under the package `com.oracle.svm.core.annotate` have been moved from `org.graalvm.nativeimage:svm:jar` to `org.graalvm.sdk:graal-sdk:jar`.
If your code relies on any of these annotations, please update your pom files accordingly, i.e., substituting the `svm` dependency with the `graal-sdk` one.

NOTE: Some annotations, like `@AlwaysInline` have been moved from `com.oracle.svm.core.annotate` to `com.oracle.svm.core`, and are not shipped in `org.graalvm.sdk:graal-sdk:jar`.
If your code relies on such annotations, you are strongly advised to reconsider their use as they are not public API.
If, however, they turn out to be mandatory for the correct functionality of your code please make sure to update the `import` statements in your source code and include `org.graalvm.nativeimage:svm:jar` in addition to `org.graalvm.sdk:graal-sdk:jar` in the dependencies of your project.

### Mandrel no longer providing Java 11 based releases

Starting with 22.3 Mandrel no longer provides Java 11 based releases.
As a result, if you used Mandrel builder images with the suffix `-java11` in the past, please be advised that such images will no longer be available, thus you are encouraged to use the `-java17` suffixed ones.
Note, however,  that this doesn't mean that you may no longer produce native executables with Mandrel for Java 11 projects.
You may still compile your Java 11 projects using OpenJDK 11 and produce native executables from the resulting Java 11 bytecode using the `-java17` Mandrel builder images.

## Kubernetes Client upgraded to 6.1

The Kubernetes Client has been upgraded from 5.12 to 6.1.
Please refer to the [Kubernetes Client 6 migration guide](https://github.com/fabric8io/kubernetes-client/blob/master/doc/MIGRATION-v6.md).

## `@ConfigProperties` was removed

The deprecated annotation `io.quarkus.arc.config.ConfigProperties` was removed.
Users are encouraged to use the `@io.smallrye.config.ConfigMapping` instead.
Please read the [Mapping configuration to objects](https://quarkus.io/guides/config-mappings#config-properties) for more information.

## OpenTelemetry exporters moved

The `quarkus-opentelemetry-exporter-jaeger` extension was moved to the [quarkus-opentelemetry-exporter](https://github.com/quarkiverse/quarkus-opentelemetry-exporter) at Quarkiverse.
The `quarkus-opentelemetry-exporter-otlp` extension was removed and the code is now part of `quarkus-opentelemetry`, as the default exporter.
Your project dependencies need to be updated accordingly. The config properties remain the same.

## RESTEasy Reactive multipart changes

The following changes impact multipart support in RESTEasy Reactive:

- BREAKING: Previously, you could catch all file uploads regardless of the parameter name using the syntax: `@RestForm List<FileUpload> all`, but this was ambiguous and unintuitive, so now this form will only fetch parameters named `all` (just like for every other form element of other types) and you have to use this form to catch every parameter regardless of its name: `@RestForm(FileUpload.ALL) List<FileUpload> all`.
- The `@MultipartForm` annotation is now deprecated. It is now equivalent to `@BeanParam` which you may use in its stead. Multipart form parameter support has been added to `@BeanParam`
- The `@BeanParam` is now optional and implicit for any non-annotated method parameter which has fields annotated with any `@Rest*` or `@*Param` annotations.
- Multipart elements are no longer limited to being encapsulated inside `@MultipartForm`-annotated classes: they can be used as method endpoint parameters as well as endpoint class fields.
- Multipart elements now default to the `@PartType(MediaType.TEXT_PLAIN)` mime type, unless they are of type `FileUpload`, `Path`, `File`, `byte[]` or `InputStream`
- Multipart elements of the `MediaType.TEXT_PLAIN` mime type are now deserialised using the regular `ParamConverter` infrastructure (previously they were using `MessageBodyReader`)
- Multipart elements of the `FileUpload`, `Path`, `File`, `byte[]` or `InputStream` types are deserialised specially.
- Multipart elements of other mime types (explicitely set) still use the appropriate `MessageBodyReader` infrastructure.
- Multipart elements can now be wrapped in `List` in order to obtain all values of the part that have the same name.
- Any client call including `@RestForm` or `@FormParam` parameters defaults to the `MediaType.APPLICATION_FORM_URLENCODED` content type, unless they are of the `File`, `Path`, `Buffer`, `Multi<Byte>` or `byte[]` types, in which case it defaults to the `MediaType.MULTIPART_FORM_DATA` content type.

## Hibernate ORM with Panache deprecated methods removed

The following deprecated methods from Hibernate ORM with Panache and Hibernate ORM with Panache in Kotlin was removed:

- `io.quarkus.hibernate.orm.panache.PanacheRepositoryBase#getEntityManager(Class<?> clazz)`
- `io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase#getEntityManager(clazz: KClass<Any>)`

Both can be replaced by `Panache.getEntityManager(Class<?> clazz)`.

## Hibernate Validator

Quarkus doesn't support the manual creation of `ValidatorFactory`: you have to use the `ValidatorFactory` managed by Quarkus that you can inject via CDI.
The main reason for this decision is that a `ValidatorFactory` has to be very carefully crafted to work in native executables.

Until now, you could still do that and handle a `ValidatorFactory` yourself (if you could make it work...). Starting with 2.14, when some component of your application tries to create a `ValidatorFactory`, we actually return the `ValidatorFactory` managed by Quarkus. This is to improve the compatibility with components creating their own `ValidatorFactory`.

## Quartz

If you are storing your Quartz jobs in a database via JDBC, you will have to update your `JOB_DETAILS` table as a class name has changed.

Executing the following query will fix the issue:
```
UPDATE JOB_DETAILS set JOB_CLASS_NAME = 'io.quarkus.quartz.runtime.QuartzSchedulerImpl$InvokerJob' WHERE JOB_CLASS_NAME = 'io.quarkus.quartz.runtime.QuartzScheduler$InvokerJob';
```