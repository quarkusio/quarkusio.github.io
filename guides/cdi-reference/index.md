# Contexts and Dependency Injection

Quarkus DI solution (also called ArC) is based on the [Jakarta Contexts and Dependency Injection 4.1](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html) specification. It implements the CDI Lite specification, with selected improvements on top, and passes the CDI Lite TCK. It does not implement CDI Full. See also [the list of supported features and limitations](#supported_features_and_limitations). Most of the existing CDI code should work just fine but there are some small differences which follow from the Quarkus architecture and goals.

> [!TIP]
> If you're new to CDI we recommend you to read the [Introduction to CDI](/guides/cdi/index.md) first.

> [!TIP]
> The [CDI integration guide](/guides/cdi-integration/index.md) has more detail on common CDI-related integration use cases, and example code for solutions.

<a id="bean_discovery"></a>
## Bean Discovery

Bean discovery in CDI is a complex process which involves legacy deployment structures and accessibility requirements of the underlying module architecture. However, Quarkus is using a **simplified bean discovery**. There is only single bean archive with the [bean discovery mode `annotated`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#default_bean_discovery) and no visibility boundaries.

The bean archive is synthesized from:

- the application classes,
- dependencies that contain a `beans.xml` descriptor (content is ignored),
- dependencies that contain a Jandex index - `META-INF/jandex.idx`,
- dependencies referenced by `quarkus.index-dependency` in `application.properties`,
- and Quarkus integration code.

Bean classes that don't have a [bean defining annotation](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#bean_defining_annotations) are not discovered. This behavior is defined by CDI. But producer methods and fields and observer methods are discovered even if the declaring class is not annotated with a bean defining annotation (this behavior is different to what is defined in CDI). In fact, the declaring bean classes are considered annotated with `@Dependent`.

> [!NOTE]
> Quarkus extensions may declare additional discovery rules. For example, `@Scheduled` business methods are registered even if the declaring class is not annotated with a bean defining annotation.

### How to Generate a Jandex Index

A dependency with a Jandex index is automatically scanned for beans. To generate the index, just add the following plugin to your build file:

**Maven**

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.smallrye</groupId>
      <artifactId>jandex-maven-plugin</artifactId>
      <version>3.6.0</version>
      <executions>
        <execution>
          <id>make-index</id>
          <goals>
            <goal>jandex</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**Gradle (Groovy DSL)**

```groovy
plugins {
    id 'org.kordamp.gradle.jandex' version '2.3.0'
}
```

You can find the latest plugin version in the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.kordamp.gradle.jandex)

**Gradle (Kotlin DSL)**

```kotlin
plugins {
    id("org.kordamp.gradle.jandex") version "2.3.0"
}
```

You can find the latest plugin version in the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.kordamp.gradle.jandex)

If you can't modify the dependency, you can still index it by adding `quarkus.index-dependency` entries to your `application.properties`:

```properties
quarkus.index-dependency.<name>.group-id=
quarkus.index-dependency.<name>.artifact-id=(this one is optional)
quarkus.index-dependency.<name>.classifier=(this one is optional)
```

> [!TIP]
> If no `artifact-id` is specified then all dependencies with the specified `group-id` are indexed.

For example, the following entries ensure that the `org.acme:acme-api` dependency is indexed:

**Example application.properties**

```properties
quarkus.index-dependency.acme.group-id=org.acme <1>
quarkus.index-dependency.acme.artifact-id=acme-api <2>
```

1. Value is a group id for a dependency identified by name `acme`.
2. Value is an artifact id for a dependency identified by name `acme`.

### How To Exclude Types and Dependencies from Discovery

It may happen that some beans from third-party libraries do not work correctly in Quarkus. A typical example is a bean injecting a portable extension. In such case, it's possible to exclude types and dependencies from the bean discovery. The `quarkus.arc.exclude-types` property accepts a list of string values that are used to match classes that should be excluded.

**Value Examples**

| Value | Description |
| --- | --- |
| `org.acme.Foo` | Match the fully qualified name of the class |
| `org.acme.*` | Match classes with package `org.acme` |
| `org.acme.**` | Match classes where the package starts with `org.acme` |
| `Bar` | Match the simple name of the class |

**Example application.properties**

```properties
quarkus.arc.exclude-types=org.acme.Foo,org.acme.*,Bar <1><2><3>
```

1. Exclude the type `org.acme.Foo`.
2. Exclude all types from the `org.acme` package.
3. Exclude all types whose simple name is `Bar`

It is also possible to exclude a dependency artifact that would be otherwise scanned for beans. For example, because it contains a `beans.xml` descriptor.

**Example application.properties**

```properties
quarkus.arc.exclude-dependency.acme.group-id=org.acme <1>
quarkus.arc.exclude-dependency.acme.artifact-id=acme-services <2>
```

1. Value is a group id for a dependency identified by name `acme`.
2. Value is an artifact id for a dependency identified by name `acme`.

<a id="string-based-qualifiers"></a>
## String-Based Qualifiers

The `@Named` qualifier, which you might be familiar with, is a *string-based qualifier*. That is, it's the string value of the qualifier annotation who determines whether the qualifier matches or not. This is not type-safe and should not be the norm in CDI applications. Specific qualifier types should be preferred.

However, sometimes string-based qualifiers are necessary. In that case, avoid the `@Named` qualifier, because in CDI, it works differently to all other qualifiers.

Specifically: if the only qualifier a bean has is `@Named`, it also automatically gets `@Default`. This means that if multiple beans of the same type exist, one of them without qualifiers and the others with `@Named`, they *all* get the `@Default` qualifier and bean resolution will error with ambiguity. For example:

```java
@ApplicationScoped
public class Producers {
    @Produces
    MyBean produce() {
        ...
    }

    @Produces
    @Named("foo")
    MyBean produceFoo() {
        ...
    }
}

@ApplicationScoped
public class Consumer {
    @Inject
    MyBean bean;
}
```

In this case, the `Consumer#bean` injection point will cause ambiguity error, because both `MyBean` producers will have the `@Default` qualifier.

Instead of `@Named`, use `@io.smallrye.common.annotation.Identifier`. This is a regular qualifier that works like all others. So if we rewrite the example to use `@Identifier`:

```java
@ApplicationScoped
public class Producers {
    @Produces
    MyBean produce() {
        ...
    }

    @Produces
    @Identifier("foo")
    MyBean produceFoo() {
        ...
    }
}

@ApplicationScoped
public class Consumer {
    @Inject
    MyBean bean;
}
```

Only the first producer will get the `@Default` qualifier, the second will not. Hence, there will be no error, and everything will work as expected.

### When To Use `@Named`?

There is one case where `@Named` is the right thing to use: specifying an external identifier for a different language that doesn't support dependency injection directly.

For example:

```java
@ApplicationScoped
@Named("myBean")
public class MyBean {
    public String getValue() {
        ...
    }
}

@ApplicationScoped
public class Consumer {
    @Inject
    MyBean bean;
}
```

As you can see, in the application code, the bean is injected without a qualifier. The bean name is only used to refer to the bean in the other language.

Historically, the most common external language that used bean names was JSF. In Quarkus, we have [Qute](/guides/qute-reference/index.md#injecting-beans-directly-in-templates). In a Qute template, one would refer to the bean using its name:

```
The current value is {inject:myBean.value}.
```

Outside of this use-case, just use `@Identifier`.

## Native Executables and Private Members

Quarkus is using GraalVM to build a native executable. One of the limitations of GraalVM is the usage of [Reflection](https://www.graalvm.org/jdk21/reference-manual/native-image/dynamic-features/Reflection/). Reflective operations are supported, but all relevant members must be registered for reflection explicitly. Those registrations result in a bigger native executable.

And if Quarkus DI needs to access a private member it, **has to use reflection**. That's why Quarkus users are encouraged *not to use private members* in their beans. This involves injection fields, constructors and initializers, observer methods, producer methods and fields, disposers, and interceptor methods.

How to avoid using private members? You can use package-private modifiers:

```java
@ApplicationScoped
public class CounterBean {

    @Inject
    CounterService counterService; <1>

    void onMessage(@Observes Event msg) { <2>
    }
}
```

1. A package-private injection field.
2. A package-private observer method.

Or constructor injection:

```java
@ApplicationScoped
public class CounterBean {

    private CounterService service;

    CounterBean(CounterService service) { <1>
      this.service = service;
    }
}
```

1. A package-private constructor injection. `@Inject` is optional in this particular case.

<a id="supported_features_and_limitations"></a>
## <a id="supported_features"></a><a id="limitations"></a> Supported Features and Limitations

The CDI Lite specification is fully supported. The following features from CDI Full are also supported:

- Decorators
  - Decoration of built-in beans, such as `Event`, is not supported
- `BeanManager`
- 
  - In addition to the `BeanContainer` methods, the following methods are supported: `getInjectableReference()`, `resolveDecorators()`
- `@SessionScoped`
- 
  - Only with the Undertow extension; see [here](/guides/cdi/index.md#bean-scope-available) for details

The *method invokers* implementation supports asynchronous methods. The following methods are considered asynchronous and `@Dependent` instances are only destroyed when the asynchronous action completes:

- methods that declare a return type of `CompletionStage`, `Uni`, or `Multi`

> [!NOTE]
> These additional features are not covered by the CDI Lite TCK.

<a id="nonstandard_features"></a>
## Non-standard Features

### Eager Instantiation of Beans

<a id="lazy_by_default"></a>
#### Lazy By Default

By default, CDI beans are created lazily, when needed. What exactly "needed" means depends on the scope of a bean.

- A **normal scoped bean** (`@ApplicationScoped`, `@RequestScoped`, etc.) is needed when a method is invoked upon an injected instance (contextual reference per the specification).

  In other words, injecting a normal scoped bean will not suffice because a *client proxy* is injected instead of a contextual instance of the bean.


- A **bean with a pseudo-scope** (`@Dependent` and `@Singleton` ) is created when injected.

**Lazy Instantiation Example**

```java
@Singleton // => pseudo-scope
class AmazingService {
  String ping() {
    return "amazing";
  }
}

@ApplicationScoped // => normal scope
class CoolService {
  String ping() {
    return "cool";
  }
}

@Path("/ping")
public class PingResource {

  @Inject
  AmazingService s1; <1>

  @Inject
  CoolService s2; <2>

  @GET
  public String ping() {
    return s1.ping() + s2.ping(); <3>
  }
}
```

1. Injection triggers the instantiation of `AmazingService`.
2. Injection itself does not result in the instantiation of `CoolService`. A client proxy is injected.
3. The first invocation upon the injected proxy triggers the instantiation of `CoolService`.

<a id="startup_event"></a>
#### Startup Event

However, if you really need to instantiate a bean eagerly, you can:

- Declare an observer of the `StartupEvent` - the scope of the bean does not matter in this case:

  ```java
  @ApplicationScoped
  class CoolService {
    void startup(@Observes StartupEvent event) { <1>
    }
  }
  ```

  1. A `CoolService` is created during startup to service the observer method invocation.


- Use the bean in an observer of the `StartupEvent` - normal scoped beans must be used as described in [Lazy By Default](#lazy_by_default):

  ```java
  @Dependent
  class MyBeanStarter {

    void startup(@Observes StartupEvent event, AmazingService amazing, CoolService cool) { <1>
      cool.toString(); <2>
    }
  }
  ```

  1. The `AmazingService` is created during injection.
  2. The `CoolService` is a normal scoped bean, so we have to invoke a method upon the injected proxy to force the instantiation.


- Annotate the bean with `@io.quarkus.runtime.Startup` as described in [Startup annotation](/guides/lifecycle/index.md#startup_annotation):

  ```java
  @Startup // <1>
  @ApplicationScoped
  public class EagerAppBean {

     private final String name;

     EagerAppBean(NameGenerator generator) { // <2>
       this.name = generator.createName();
     }
  }
  ```

  1. For each bean annotated with `@Startup` a synthetic observer of `StartupEvent` is generated. The default priority is used.
  2. The bean constructor is called when the application starts and the resulting contextual instance is stored in the application context.


> [!NOTE]
> Quarkus users are encouraged to always prefer the `@Observes StartupEvent` to `@Initialized(ApplicationScoped.class)` as explained in the [Application Initialization and Termination](/guides/lifecycle/index.md) guide.

<a id="request-context-lifecycle"></a>
### Request Context Lifecycle

The request context is also active:

- during notification of a synchronous observer method.

The request context is destroyed:

- after the observer notification completes for an event, if it was not already active when the notification started.

> [!NOTE]
> An event with qualifier `@Initialized(RequestScoped.class)` is fired when the request context is initialized for an observer notification. Moreover, the events with qualifiers `@BeforeDestroyed(RequestScoped.class)` and `@Destroyed(RequestScoped.class)` are fired when the request context is destroyed.

#### How to Enable Trace Logging for Request Context Activation

You can set the `TRACE` level for the logger `io.quarkus.arc.requestContext` and try to analyze the log output afterwards.

**`application.properties` Example**

```properties
quarkus.log.category."io.quarkus.arc.requestContext".min-level=TRACE <1>
quarkus.log.category."io.quarkus.arc.requestContext".level=TRACE
```

1. You also need to adjust the minimum log level for the relevant category.

### Qualified Injected Fields

In CDI, if you declare a field injection point you need to use `@Inject` and optionally a set of qualifiers.

```java
  @Inject
  @ConfigProperty(name = "cool")
  String coolProperty;
```

In Quarkus, you can skip the `@Inject` annotation completely if the injected field declares at least one qualifier.

```java
  @ConfigProperty(name = "cool")
  String coolProperty;
```

> [!NOTE]
> With the notable exception of one special case discussed below, `@Inject` is still required for constructor and method injection.

### Simplified Constructor Injection

In CDI, a normal scoped bean must always declare a no-args constructor (this constructor is normally generated by the compiler unless you declare any other constructor). However, this requirement complicates constructor injection - you need to provide a dummy no-args constructor to make things work in CDI.

```java
@ApplicationScoped
public class MyCoolService {

  private SimpleProcessor processor;

  MyCoolService() { // dummy constructor needed
  }

  @Inject // constructor injection
  MyCoolService(SimpleProcessor processor) {
    this.processor = processor;
  }
}
```

There is no need to declare dummy constructors for normal scoped bean in Quarkus - they are generated automatically. Also, if there's only one constructor there is no need for `@Inject`.

```java
@ApplicationScoped
public class MyCoolService {

  private SimpleProcessor processor;

  MyCoolService(SimpleProcessor processor) {
    this.processor = processor;
  }
}
```

> [!NOTE]
> We don't generate a no-args constructor automatically if a bean class extends a class that does not declare a no-args constructor.

<a id="remove_unused_beans"></a>
### Removing Unused Beans

The container attempts to remove all unused beans, interceptors and decorators during build by default. This optimization helps to minimize the amount of generated classes, thus conserving memory. However, Quarkus can't detect the programmatic lookup performed via the `CDI.current()` static method. Therefore, it is possible that a removal results in a false positive error, i.e. a bean is removed although it's actually used. In such cases, you'll notice a big warning in the log. Users and extension authors have several options [how to eliminate false positives](#eliminate_false_positives).

The optimization can be disabled by setting `quarkus.arc.remove-unused-beans` to `none` or `false`. Quarkus also provides a middle ground where application beans are never removed, whether or not they are unused, while the optimization proceeds normally for non application classes. To use this mode, set `quarkus.arc.remove-unused-beans` to `fwk` or `framework`.

#### What's Removed?

Quarkus first identifies so-called *unremovable* beans that form the roots in the dependency tree. A good example is a Jakarta REST resource class or a bean which declares a `@Scheduled` method.

An *unremovable* bean:

- is [excluded from removal](#eliminate_false_positives), or
- has a name designated via `@Named`, or
- declares an observer method.

An *unused* bean:

- is not *unremovable*, and
- is not eligible for injection to any injection point in the dependency tree of *unremovable* beans, and
- does not declare any producer which is eligible for injection to any injection point in the dependency tree, and
- is not eligible for injection into any `jakarta.enterprise.inject.Instance` or `jakarta.inject.Provider` injection point, and
- is not eligible for injection into any [`@Inject @All List<>`](#injecting-multiple-bean-instances-intuitively) injection point.

Unused interceptors and decorators are not associated with any bean.

> [!TIP]
> When using the dev mode (running `./mvnw quarkus:dev`), you can see more information about which beans are being removed:
>
> 1. In the console - just enable the DEBUG level in your `application.properties`, i.e. `quarkus.log.category."io.quarkus.arc.processor".level=DEBUG`
> 2. In the relevant Dev UI page

<a id="eliminate_false_positives"></a>
#### How To Eliminate False Positives

Users can instruct the container to not remove any of their specific beans (even if they satisfy all the rules specified above) by annotating them with `@io.quarkus.arc.Unremovable`. This annotation can be declared on a class, a producer method or field.

Since this is not always possible, there is an option to achieve the same via `application.properties`. The `quarkus.arc.unremovable-types` property accepts a list of string values that are used to match beans based on their name or package.

**Value Examples**

| Value | Description |
| --- | --- |
| `org.acme.Foo` | Match the fully qualified name of the bean class |
| `org.acme.*` | Match beans where the package of the bean class is `org.acme` |
| `org.acme.**` | Match beans where the package of the bean class starts with `org.acme` |
| `Bar` | Match the simple name of the bean class |

**Example application.properties**

```properties
quarkus.arc.unremovable-types=org.acme.Foo,org.acme.*,Bar
```

Furthermore, extensions can eliminate false positives by producing an `UnremovableBeanBuildItem`.

<a id="default_beans"></a>
### Default Beans

Quarkus adds a capability that CDI currently does not support which is to conditionally declare a bean if no other bean with equal types and qualifiers was declared by any available means (bean class, producer, synthetic bean, ...) This is done using the `@io.quarkus.arc.DefaultBean` annotation and is best explained with an example.

Say there is a Quarkus extension that, among other things, declares a few CDI beans like the following code does:

```java
@Dependent
public class TracerConfiguration {

    @Produces
    public Tracer tracer(Reporter reporter, Configuration configuration) {
        return new Tracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Configuration configuration() {
        // create a Configuration
    }

    @Produces
    @DefaultBean
    public Reporter reporter(){
        // create a Reporter
    }
}
```

The idea is that the extension autoconfigures things for the user, eliminating a lot of boilerplate - we can just `@Inject` a `Tracer` wherever it is needed. Now imagine that in our application we would like to utilize the configured `Tracer`, but we need to customize it a little, for example by providing a custom `Reporter`. The only thing that would be needed in our application would be something like the following:

```java
@Dependent
public class CustomTracerConfiguration {

    @Produces
    public Reporter reporter(){
        // create a custom Reporter
    }
}
```

`@DefaultBean` allows extensions (or any other code for that matter) to provide defaults while backing off if beans of that type are supplied in any way Quarkus supports.

Default beans can optionally declare `@jakarta.annotation.Priority`. If there is no priority defined, `@Priority(0)` is assumed. Priority value is used for bean ordering and during typesafe resolution to disambiguate multiple matching default beans.

```java
@Dependent
public class CustomizedDefaultConfiguration {

    @Produces
    @DefaultBean
    @Priority(100)
    public Configuration customizedConfiguration(){
        // create a customized default Configuration
        // this will have priority over previously defined default bean
    }
}
```

<a id="enable_build_profile"></a>
### Enabling Beans for Quarkus Build Profile

Quarkus adds a capability that CDI currently does not support, which is to conditionally enable a bean when a Quarkus build time profile is enabled, via the `@io.quarkus.arc.profile.IfBuildProfile` and `@io.quarkus.arc.profile.UnlessBuildProfile` annotations. When used in conjunction with `@io.quarkus.arc.DefaultBean`, these annotations allow for the creation of different bean configurations for different build profiles.

Imagine, for instance that an application contains a bean named `Tracer`, which needs to do nothing when in tests or in dev mode, but works in its normal capacity for the production artifact. An elegant way to create such beans is the following:

```java
@Dependent
public class TracerConfiguration {

    @Produces
    @IfBuildProfile("prod")
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

If instead, it is required that the `Tracer` bean also works in dev mode and only default to doing nothing for tests, then `@UnlessBuildProfile` would be ideal. The code would look like:

```java
@Dependent
public class TracerConfiguration {

    @Produces
    @UnlessBuildProfile("test") // this will be enabled for both prod and dev build time profiles
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

> [!NOTE]
> The runtime profile has absolutely no effect on the bean resolution using `@IfBuildProfile` and `@UnlessBuildProfile`.

> [!TIP]
> `@IfBuildProfile` and `@UnlessBuildProfile` may be put on a stereotype. A bean will only be enabled if **all** the conditions defined by these annotations are satisfied.

<a id="enable_build_properties"></a>
### Enabling Beans for Quarkus Build Properties

Quarkus adds a capability that CDI currently does not support which is to conditionally enable a bean when a Quarkus build time property has or does not have a specific value, via the `@io.quarkus.arc.properties.IfBuildProperty` and `@io.quarkus.arc.properties.UnlessBuildProperty` annotations. When used in conjunction with `@io.quarkus.arc.DefaultBean`, these annotations allow for the creation of different bean configurations for different build properties.

The scenario we mentioned above with `Tracer` could also be implemented in the following way:

```java
@Dependent
public class TracerConfiguration {

    @Produces
    @IfBuildProperty(name = "some.tracer.enabled", stringValue = "true")
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

> [!TIP]
> `@IfBuildProperty` and `@UnlessBuildProperty` are repeatable annotations and may be put on a stereotype. A bean will only be enabled if **all** the conditions defined by these annotations are satisfied.

If instead, it is required that the `RealTracer` bean is only used if the `some.tracer.enabled` property is not `false`, then `@UnlessBuildProperty` would be ideal. The code would look like:

```java
@Dependent
public class TracerConfiguration {

    @Produces
    @UnlessBuildProperty(name = "some.tracer.enabled", stringValue = "false")
    public Tracer realTracer(Reporter reporter, Configuration configuration) {
        return new RealTracer(reporter, configuration);
    }

    @Produces
    @DefaultBean
    public Tracer noopTracer() {
        return new NoopTracer();
    }
}
```

> [!NOTE]
> Properties set at runtime have absolutely no effect on the bean resolution using `@IfBuildProperty` / `@UnlessBuildProperty`.

### Declaring Selected Alternatives

In CDI, an alternative bean may be selected either globally for an application by means of `@Priority`, or for a bean archive using a `beans.xml` descriptor. Quarkus has a simplified bean discovery, and the content of `beans.xml` is ignored.

However, it is also possible to select alternatives for an application using the unified configuration. The `quarkus.arc.selected-alternatives` property accepts a list of string values that are used to match alternative beans. If any value matches, then the priority of `Integer#MAX_VALUE` is used for the relevant bean. The priority declared via `@Priority` or inherited from a stereotype is overridden.

**Value Examples**

| Value | Description |
| --- | --- |
| `org.acme.Foo` | Match the fully qualified name of the bean class or the bean class of the bean that declares the producer |
| `org.acme.*` | Match beans where the package of the bean class is `org.acme` |
| `org.acme.**` | Match beans where the package of the bean class starts with `org.acme` |
| `Bar` | Match the simple name of the bean class or the bean class of the bean that declares the producer |

**Example application.properties**

```properties
quarkus.arc.selected-alternatives=org.acme.Foo,org.acme.*,Bar
```

### Simplified Producer Method Declaration

In CDI, a producer method must be always annotated with `@Produces`.

```java
class Producers {

  @Inject
  @ConfigProperty(name = "cool")
  String coolProperty;

  @Produces
  @ApplicationScoped
  MyService produceService() {
    return new MyService(coolProperty);
  }
}
```

In Quarkus, you can skip the `@Produces` annotation completely if the producer method is annotated with a scope annotation, a stereotype or a qualifier.

```java
class Producers {

  @ConfigProperty(name = "cool")
  String coolProperty;

  @ApplicationScoped
  MyService produceService() {
    return new MyService(coolProperty);
  }
}
```

<a id="interception_of_static_methods"></a>
### Interception of Static Methods

The Interceptors specification is clear that *around-invoke* methods must not be declared static. However, this restriction was driven mostly by technical limitations. And since Quarkus is a build-time oriented stack that allows for additional class transformations, those limitations don't apply anymore. It's possible to annotate a non-private static method with an interceptor binding:

```java
class Services {

  @Logged <1>
  static BigDecimal computePrice(long amount) { <2>
    BigDecimal price;
    // Perform computations...
    return price;
  }
}
```

1. `Logged` is an interceptor binding.
2. Each method invocation is intercepted if there is an interceptor associated with `Logged`.

#### Limitations

- Only **method-level bindings** are considered for backward compatibility reasons (otherwise static methods of bean classes that declare class-level bindings would be suddenly intercepted)
- Private static methods are never intercepted
- `InvocationContext#getTarget()` returns `null` for obvious reasons; therefore, not all existing interceptors may behave correctly when intercepting static methods

  > [!NOTE]
  > Interceptors can use `InvocationContext.getMethod()` to detect static methods and adjust the behavior accordingly.


<a id="unproxyable_classes_transformation"></a>
### Ability to handle 'final' classes and methods

In normal CDI, classes that are marked as `final` and / or have `final` methods are not eligible for proxy creation, which in turn means that interceptors and normal scoped beans don't work properly. This situation is very common when trying to use CDI with alternative JVM languages like Kotlin, where classes and methods are `final` by default.

Quarkus however, can overcome these limitations when `quarkus.arc.transform-unproxyable-classes` is set to `true` (which is the default value).

### Container-managed Concurrency

There is no standard concurrency control mechanism for CDI beans. Nevertheless, a bean instance can be shared and accessed concurrently from multiple threads. In that case, it should be thread-safe. You can use standard Java constructs (`volatile`, `synchronized`, `ReadWriteLock`, etc.) or let the container control the concurrent access. Quarkus provides `@io.quarkus.arc.Lock` and a built-in interceptor for this interceptor binding. Each interceptor instance associated with a contextual instance of an intercepted bean holds a separate `ReadWriteLock` with non-fair ordering policy.

> [!TIP]
> `io.quarkus.arc.Lock` is a regular interceptor binding and as such can be used for any bean with any scope. However, it is especially useful for "shared" scopes, e.g. `@Singleton` and `@ApplicationScoped`.

**Container-managed Concurrency Example**

```java
import io.quarkus.arc.Lock;

@Lock <1>
@ApplicationScoped
class SharedService {

  void addAmount(BigDecimal amount) {
    // ...changes some internal state of the bean
  }

  @Lock(value = Lock.Type.READ, time = 1, unit = TimeUnit.SECONDS) <2><3>
  BigDecimal getAmount() {
    // ...it is safe to read the value concurrently
  }
}
```

1. `@Lock` (which maps to `@Lock(Lock.Type.WRITE)`) declared on the class instructs the container to lock the bean instance for any invocation of any business method, i.e. the client has "exclusive access" and no concurrent invocations will be allowed.
2. `@Lock(Lock.Type.READ)` overrides the value specified at class level. It means that any number of clients can invoke the method concurrently, unless the bean instance is locked by `@Lock(Lock.Type.WRITE)`.
3. You can also specify the "wait time". If it's not possible to acquire the lock in the given time a `LockException` is thrown.

### Repeatable interceptor bindings

Quarkus has limited support for `@Repeatable` interceptor binding annotations.

When binding an interceptor to a component, you can declare multiple `@Repeatable` annotations on methods. Repeatable interceptor bindings declared on classes and stereotypes are not supported, because there are some open questions around interactions with the Interceptors specification. This might be added in the future.

As an example, suppose we have an interceptor that clears a cache. The corresponding interceptor binding would be called `@CacheInvalidateAll` and would be declared as `@Repeatable`. If we wanted to clear two caches at the same time, we would add `@CacheInvalidateAll` twice:

```java
@ApplicationScoped
class CachingService {
  @CacheInvalidateAll(cacheName = "foo")
  @CacheInvalidateAll(cacheName = "bar")
  void heavyComputation() {
    // ...
    // some computation that updates a lot of data
    // and requires 2 caches to be invalidated
    // ...
  }
}
```

This is how interceptors are used. What about creating an interceptor?

When declaring interceptor bindings of an interceptor, you can add multiple `@Repeatable` annotations to the interceptor class as usual. This is useless when the annotation members are `@Nonbinding`, as would be the case for the `@Cached` annotation, but is important otherwise.

For example, suppose we have an interceptor that can automatically log method invocations to certain targets. The interceptor binding annotation `@Logged` would have a member called `target`, which specifies where to store the log. Our implementation could be restricted to console logging and file logging:

```java
@Interceptor
@Logged(target = "console")
@Logged(target = "file")
class NaiveLoggingInterceptor {
  // ...
}
```

Other interceptors could be provided to log method invocations to different targets.

### Caching the Result of Programmatic Lookup

In certain situations, it is practical to obtain a bean instance programmatically via an injected `jakarta.enterprise.inject.Instance` and `Instance.get()`. However, according to the specification, the `get()` method must identify the matching bean and obtain a contextual reference. As a consequence, a new instance of a  `@Dependent` bean is returned from each invocation of `get()`. Moreover, this instance is a dependent object of the injected `Instance`. This behavior is well-defined, but it may lead to unexpected errors and memory leaks. Therefore, Quarkus comes with the `io.quarkus.arc.WithCaching` annotation. An injected `Instance` annotated with this annotation will cache the result of the `Instance#get()` operation. The result is computed on the first call, and the same value is returned for all subsequent calls, even for `@Dependent` beans.

```java
class Producer {

  AtomicLong nextLong = new AtomicLong();
  AtomicInteger nextInt = new AtomicInteger();

   @Dependent
   @Produces
   Integer produceInt() {
     return nextInt.incrementAndGet();
   }

   @Dependent
   @Produces
   Long produceLong() {
     return nextLong.incrementAndGet();
   }
}

class Consumer {

  @Inject
  Instance<Long> longInstance;

  @Inject
  @WithCaching
  Instance<Integer> intInstance;

  // this method should always return true
  // Producer#produceInt() is only called once
  boolean pingInt() {
    return intInstance.get().equals(intInstance.get());
  }

  // this method should always return false
  // Producer#produceLong() is called twice per each pingLong() invocation
  boolean pingLong() {
    return longInstance.get().equals(longInstance.get());
  }
}
```

> [!TIP]
> It is also possible to clear the cached value via `io.quarkus.arc.InjectableInstance.clearCache()`. In this case, you'll need to inject the Quarkus-specific `io.quarkus.arc.InjectableInstance` instead of `jakarta.enterprise.inject.Instance`.

### Declaratively Choose Beans That Can Be Obtained by Programmatic Lookup

It is sometimes useful to narrow down the set of beans that can be obtained by programmatic lookup via `jakarta.enterprise.inject.Instance`. Typically, a user needs to choose the appropriate implementation of an interface based on a runtime configuration property.

Imagine that we have two beans implementing the interface `org.acme.Service`. You can't inject the `org.acme.Service` directly unless your implementations declare a CDI qualifier. However, you can inject the `Instance<Service>` instead, then iterate over all implementations and choose the correct one manually. Alternatively, you can use the `@LookupIfProperty` and `@LookupUnlessProperty` annotations. `@LookupIfProperty` indicates that a bean should only be obtained if a runtime configuration property matches the provided value. `@LookupUnlessProperty`, on the other hand, indicates that a bean should only be obtained if a runtime configuration property does not match the provided value.

**`@LookupIfProperty` Example**

```java
 interface Service {
    String name();
 }

 @LookupIfProperty(name = "service.foo.enabled", stringValue = "true")
 @ApplicationScoped
 class ServiceFoo implements Service {

    public String name() {
       return "foo";
    }
 }

 @ApplicationScoped
 class ServiceBar implements Service {

    public String name() {
       return "bar";
    }
 }

 @ApplicationScoped
 class Client {

    @Inject
    Instance<Service> service;

    void printServiceName() {
       // This will print "bar" if the property "service.foo.enabled" is NOT set to "true"
       // If "service.foo.enabled" is set to "true" then service.get() would result in an AmbiguousResolutionException
       System.out.println(service.get().name());
    }
 }
```

> [!TIP]
> `@LookupIfProperty` and `@LookupUnlessProperty` are repeatable annotations and may be put on a stereotype. A bean will only be obtained if **all** the conditions defined by these annotations are satisfied.

### Sorting beans obtained with programmatic lookup

If there is more than one bean that matches the required type and qualifiers and is eligible for injection, it is possible to iterate (or stream) available bean instances. Beans returned by both stream and iterator methods are sorted by priority as defined by `io.quarkus.arc.InjectableBean#getPriority()`. Higher priority goes first. If no priority is explicitly declared, 0 is assumed.

```java
interface Service {

}

@Priority(100)
@ApplicationScoped
class FirstService implements Service {

}

@Priority(10)
@ApplicationScoped
class SecondService implements Service {

}

@ApplicationScoped
class ThirdService implements Service {

}

@ApplicationScoped
class Client {

   @Inject
   Instance<Service> serviceInstance;

   void printServiceName() {
       if(service.isAmbiguous()){
           for (Service service : serviceInstance) {
                // FirstService, SecondService, ThirdService
           }
       }
   }
}
```

<a id="injecting-multiple-bean-instances-intuitively"></a>
### Injecting Multiple Bean Instances Intuitively

In CDI, it's possible to inject multiple bean instances (aka contextual references) via the `jakarta.enterprise.inject.Instance` which implements `java.lang.Iterable`. However, it's not exactly intuitive. Therefore, a new way was introduced in Quarkus - you can inject a `java.util.List` annotated with the `io.quarkus.arc.All` qualifier. The type of elements in the list is used as the required type when performing the lookup.

```java
@ApplicationScoped
public class Processor {

     @Inject
     @All
     List<Service> services; <1><2>
}
```

1. The injected instance is an *immutable list* of the contextual references of the *disambiguated* beans.
2. For this injection point the required type is `Service` and no additional qualifiers are declared.

> [!TIP]
> The list is sorted by priority as defined by `io.quarkus.arc.InjectableBean#getPriority()`. Higher priority goes first. In general, the `@jakarta.annotation.Priority` annotation can be used to assign the priority to a class bean, producer method or producer field.

If an injection point declares no other qualifier than `@All` then `@Any` is used, i.e. the behavior is equivalent to `@Inject @Any Instance<Service>`.

You can also inject a list of bean instances wrapped in `io.quarkus.arc.InstanceHandle`. This can be useful if you need to inspect the related bean metadata.

```java
@ApplicationScoped
public class Processor {

     @Inject
     @All
     List<InstanceHandle<Service>> services;

     public void doSomething() {
       for (InstanceHandle<Service> handle : services) {
         if (handle.getBean().getScope().equals(Dependent.class)) {
           handle.get().process();
           break;
         }
       }
     }
}
```

> [!NOTE]
> Neither a type variable nor a wildcard is a legal type parameter for an `@All List<>` injection point, i.e. `@Inject @All List<?> all` is not supported and results in a deployment error.

> [!TIP]
> It is also possible to obtain the list of all bean instance handles programmatically via the `Arc.container().listAll()` methods.

### Ignoring Class-Level Interceptor Bindings for Methods and Constructors

If a managed bean declares interceptor binding annotations on the class level, the corresponding `@AroundInvoke` interceptors will apply to all business methods. Similarly, the corresponding `@AroundConstruct` interceptors will apply to the bean constructor.

For example, suppose we have a logging interceptor with the `@Logged` binding annotation and a tracing interceptor with the `@Traced` binding annotation:

```java
@ApplicationScoped
@Logged
public class MyService {
    public void doSomething() {
        ...
    }

    @Traced
    public void doSomethingElse() {
        ...
    }
}
```

In this example, both `doSomething` and `doSomethingElse` will be intercepted by the hypothetical logging interceptor. Additionally, the `doSomethingElse` method will be intercepted by the hypothetical tracing interceptor.

Now, if that `@Traced` interceptor also performed all the necessary logging, we'd like to skip the `@Logged` interceptor for this method, but keep it for all other methods. To achieve that, you can annotate the method with `@NoClassInterceptors`:

```java
@Traced
@NoClassInterceptors
public void doSomethingElse() {
    ...
}
```

The `@NoClassInterceptors` annotation may be put on methods and constructors, and means that all class-level interceptors are ignored for these methods and constructors. In other words, if a method/constructor is annotated `@NoClassInterceptors`, then the only interceptors that will apply to this method/constructor are interceptors declared directly on the method/constructor.

This annotation affects only business method interceptors (`@AroundInvoke`) and constructor lifecycle callback interceptors (`@AroundConstruct`).

### Exceptions Thrown By An Asynchronous Observer Method

If an exception is thrown by an asynchronous observer then the `CompletionStage` returned by the `fireAsync()` method completes exceptionally so that the event producer may react appropriately. However, if the event producer does not care then the exception is ignored silently. Therefore, Quarkus logs an error message by default. It is also possible to implement a custom `AsyncObserverExceptionHandler`. A bean that implements this interface should be `@jakarta.inject.Singleton` or `@jakarta.enterprise.context.ApplicationScoped`.

**`NoopAsyncObserverExceptionHandler`**

```java
@Singleton
public class NoopAsyncObserverExceptionHandler implements AsyncObserverExceptionHandler {

  void handle(Throwable throwable, ObserverMethod<?> observerMethod, EventContext<?> eventContext) {
    // do nothing
  }

}
```

### Intercepted self-invocation

Quarkus supports what is known as intercepted self-invocation or just self-interception - a scenario where CDI bean invokes its own intercepted method from within another method while triggering any associated interceptors. This is a non-standard feature as CDI specification doesn't define whether self-interception should work or not.

Suppose we have a CDI bean with two methods, one of which has the `@Transactional` interceptor binding associated with it:

```java
@ApplicationScoped
public class MyService {

  @Transactional <1>
  void doSomething() {
    // some application logic
  }

  void doSomethingElse() {
    doSomething(); <2>
  }

}
```

1. One or more interceptor bindings; `@Transactional` is just an example.
2. Non-intercepted method invoking another method from the same bean that has associated binding(s); this will trigger interception.

In the above example, any code calling the `doSomething()` method triggers interception - in this case, the method becomes transactional. This is regardless of whether the invocation originated directly from the `MyService` bean (such as `MyService#doSomethingElse`) or from some other bean.

### Intercepting Producer Methods and Synthetic Beans

By default, interception is only supported for managed beans (also known as class-based beans). To support interception of producer methods and synthetic beans, the CDI specification includes an `InterceptionFactory`, which is a runtime oriented concept and therefore cannot be supported in Quarkus.

Instead, Quarkus has its own API: `InterceptionProxy` and `@BindingsSource`. The `InterceptionProxy` is very similar to `InterceptionFactory`: it creates a proxy that applies `@AroundInvoke` interceptors before forwarding the method call to the target instance. The `@BindingsSource` annotation allows setting interceptor bindings in case the intercepted class is external and cannot be changed.

```java
import io.quarkus.arc.InterceptionProxy;

@ApplicationScoped
class MyProducer {
    @Produces
    MyClass produce(InterceptionProxy<MyClass> proxy) { // <1>
        return proxy.create(new MyClass()); // <2>
    }
}
```

1. Declares an injection point of type `InterceptionProxy<MyClass>`. This means that at build time, a subclass of `MyClass` is generated that does the interception and forwarding. Note that the type argument must be identical to the return type of the producer method.
2. Creates an instance of the interception proxy for the given instance of `MyClass`. The method calls will be forwarded to this target instance after all interceptors run.

In this example, interceptor bindings are read from the `MyClass` class.

> [!IMPORTANT]
> Note that `InterceptionProxy` only supports `@AroundInvoke` interceptors declared on interceptor classes. Other kinds of interception, as well as `@AroundInvoke` interceptors declared on the target class and its superclasses, are not supported.

The intercepted class should be [proxyable](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1#unproxyable) and therefore should not be `final`, should not have non-private `final` methods, and should have a non-private zero-parameter constructor. If it isn't, a bytecode transformation will attempt to fix it if [enabled](#unproxyable_classes_transformation), but note that adding a zero-parameter constructor is not always possible.

It is often the case that the produced classes come from external libraries and don't contain interceptor binding annotations at all. To support such cases, the `@BindingsSource` annotation may be declared on the `InterceptionProxy` parameter:

```java
import io.quarkus.arc.BindingsSource;
import io.quarkus.arc.InterceptionProxy;

abstract class MyClassBindings { // <1>
    @MyInterceptorBinding
    abstract String doSomething();
}

@ApplicationScoped
class MyProducer {
    @Produces
    MyClass produce(@BindingsSource(MyClassBindings.class) InterceptionProxy<MyClass> proxy) { // <2>
        return proxy.create(new MyClass());
    }
}
```

1. A class that mirrors the `MyClass` structure and contains interceptor bindings.
2. The `@BindingsSource` annotation says that interceptor bindings for `MyClass` should be read from `MyClassBindings`.

The concept of *bindings source* is a build-time friendly equivalent of `InterceptionFactory.configure()`.

> [!NOTE]
> Producer method interception and synthetic bean interception only works for instance methods. [Interception of Static Methods](#interception_of_static_methods) is not supported for producer methods and synthetic beans.

#### Declaring `@BindingsSource`

The `@BindingsSource` annotation specifies a class that mirrors the structure of the intercepted class. Interceptor bindings are then read from that class and treated as if they were declared on the intercepted class.

Specifically: class-level interceptor bindings declared on the bindings source class are treated as class-level bindings of the intercepted class. Method-level interceptor bindings declared on the bindings source class are treated as method-level bindings of a method with the same name, return type, parameter types and `static` flag of the intercepted class.

It is common to make the bindings source class and methods `abstract` so that you don't have to write method bodies:

```java
abstract class MyClassBindings {
    @MyInterceptorBinding
    abstract String doSomething();
}
```

Since this class is never instantiated and its method are never invoked, this is okay, but it's also possible to create a non-`abstract` class:

```java
class MyClassBindings {
    @MyInterceptorBinding
    String doSomething() {
        return null; // <1>
    }
}
```

1. The method body does not matter.

Note that for generic classes, the type variable names must also be identical. For example, for the following class:

```java
class MyClass<T> {
    T doSomething() {
        ...
    }

    void doSomethingElse(T param) {
        ...
    }
}
```

The bindings source class must also use `T` as the name of the type variable:

```java
abstract class MyClassBindings<T> {
    @MyInterceptorBinding
    abstract T doSomething();
}
```

You don't need to declare methods that are not annotated simply because they exist on the intercepted class. If you want to add method-level bindings to a subset of methods, you only have to declare the methods that are supposed to have an interceptor binding. If you only want to add class-level bindings, you don't have to declare any methods at all.

These annotations can be present on a bindings source class:

- *interceptor bindings*: on the class and on the methods
- *stereotypes*: on the class
- `@NoClassInterceptors`: on the methods

Any other annotation present on a bindings source class is ignored.

#### Synthetic Beans

Using `InterceptionProxy` in synthetic beans is similar.

First, you have to declare that your synthetic bean injects the `InterceptionProxy`:

```java
public void register(RegistrationContext context) {
    context.configure(MyClass.class)
            .types(MyClass.class)
            .injectInterceptionProxy() // <1>
            .creator(MyClassCreator.class)
            .done();
}
```

1. Once again, this means that at build time, a subclass of `MyClass` is generated that does the interception and forwarding.

Second, you have to obtain the `InterceptionProxy` from the `SyntheticCreationalContext` in the `BeanCreator` and use it:

```java
public MyClass create(SyntheticCreationalContext<MyClass> context) {
    InterceptionProxy<MyClass> proxy = context.getInterceptionProxy(); // <1>
    return proxy.create(new MyClass());
}
```

1. Obtains the `InterceptionProxy` for `MyClass`, as declared above. It would also be possible to use the `getInjectedReference()` method, passing a `TypeLiteral`, but `getInterceptionProxy()` is easier.

There's also an equivalent of `@BindingsSource`. The `injectInterceptionProxy()` method has an overload with a parameter:

```java
public void register(RegistrationContext context) {
    context.configure(MyClass.class)
            .types(MyClass.class)
            .injectInterceptionProxy(MyClassBindings.class) // <1>
            .creator(MyClassCreator.class)
            .done();
}
```

1. The argument is the bindings source class.

### `Instance.Handle.close()` Behavior

Per the CDI specification, the `Instance.Handle.close()` method always delegates to `destroy()`. In ArC, this is only true in the [Strict Mode](#strict_mode).

In the default mode, the `close()` method only delegates to `destroy()` when the bean is `@Dependent` (or when the instance handle does not represent a CDI contextual object). When the instance handle represents a bean of any other scope, the `close()` method does nothing; the bean is left as is and will be destroyed whenever its context is destroyed.

This is to make the following code behave as one would naively expect:

```java
Instance<T> instance = ...;
try (Instance.Handle<T> handle : instance.getHandle()) {
   T value = handle.get();
   ... use value ...
}
```

The `@Dependent` beans are destroyed immediately, while other beans are not destroyed at all. This is important when multiple beans of different scopes might be returned by the `Instance`.

<a id="reactive_pitfalls"></a>
## Pitfalls with Reactive Programming

CDI is a purely synchronous framework. Its notion of asynchrony is very limited and based solely on thread pools and thread offloading. Therefore, there is a number of pitfalls when using CDI together with reactive programming.

### Detecting When Blocking Is Allowed

The `io.quarkus.runtime.BlockingOperationControl#isBlockingAllowed()` method can be used to detect whether blocking is allowed on the current thread. When it is not, and you need to perform a blocking operation, you have to offload it to another thread. The easiest way is to use the `Vertx.executeBlocking()` method:

```java
import io.quarkus.runtime.BlockingOperationControl;

@ApplicationScoped
public class MyBean {
    @Inject
    Vertx vertx;

    @PostConstruct
    void init() {
        if (BlockingOperationControl.isBlockingAllowed()) {
            somethingThatBlocks();
        } else {
            vertx.executeBlocking(() -> {
                somethingThatBlocks();
                return null;
            });
        }
    }

    void somethingThatBlocks() {
        // use the file system or JDBC, call a REST service, etc.
        Thread.sleep(5000);
    }
}
```

### Asynchronous Observers

CDI asynchronous observers (`@ObservesAsync`) are not aware of reactive programming and are not meant to be used as part of reactive pipelines. The observer methods are meant to be synchronous, they are just offloaded to a thread pool.

The `Event.fireAsync()` method returns a `CompletionStage` that completes when all observers are notified. If all observers were notified successfully, the `CompletionStage` completes with the event payload. If some observers have thrown an exception, the `CompletionStage` completes exceptionally with a `CompletionException`.

The return type of the observer *does not matter*. The return value of the observer is *ignored*.

You may declare an observer method that has a return type of `CompletionStage<>` or `Uni<>`, but neither the return type nor the actual return value affects the result of `Event.fireAsync()`. Further, if the observer declares a return type of `Uni<>`, the returned `Uni` will not be subscribed to, so it is quite possible that part of the observer logic will not even execute.

Therefore, it is recommended that observer methods, both synchronous and asynchronous, are always declared `void`.

### Reactive startup methods

As documented in [the `@Startup` method documentation](/guides/lifecycle/index.md#startup_annotation), you may define startup methods that return a `Uni`, in which case they will be invoked on an event thread instead of a blocking thread, and their returned `Uni` will be subscribed to and awaited.

<a id="build_time_apis"></a>
## Build Time Extensions

Quarkus incorporates build-time optimizations in order to provide instant startup and low memory footprint. The downside of this approach is that CDI Portable Extensions cannot be supported. Nevertheless, most of the functionality can be achieved using Quarkus [extensions](/guides/writing-extensions/index.md). See the [integration guide](/guides/cdi-integration/index.md) for more information.

<a id="dev_mode"></a>
## <a id="development-mode"></a> Dev mode

In dev mode, two special endpoints are registered automatically to provide some basic debug info in the JSON format:

- HTTP GET `/q/arc` - returns the summary; number of beans, config properties, etc.
- HTTP GET `/q/arc/beans` - returns the list of all beans
  - You can use query params to filter the output:
    - `scope` - include beans with scope that ends with the given value, i.e. `http://localhost:8080/q/arc/beans?scope=ApplicationScoped`
    - `beanClass` - include beans with bean class that starts with the given value, i.e. `http://localhost:8080/q/arc/beans?beanClass=org.acme.Foo`
    - `kind` - include beans of the specified kind (`CLASS`, `PRODUCER_FIELD`, `PRODUCER_METHOD`, `INTERCEPTOR` or `SYNTHETIC`), i.e. `http://localhost:8080/q/arc/beans?kind=PRODUCER_METHOD`
- HTTP GET `/q/arc/removed-beans` - returns the list of unused beans removed during build
- HTTP GET `/q/arc/observers` - returns the list of all observer methods

> [!NOTE]
> These endpoints are only available in dev mode, i.e. when you run your application via `mvn quarkus:dev` (or `./gradlew quarkusDev`).

### Monitoring Business Method Invocations and Events

In dev mode, it is also possible to enable monitoring of business method invocations and fired events. Simply set the `quarkus.arc.dev-mode.monitoring-enabled` configuration property to `true` and explore the relevant Dev UI pages.

<a id="strict_mode"></a>
## Strict Mode

By default, ArC does not perform all validations required by the CDI specification. It also improves CDI usability in many ways, some of them being directly against the specification.

To pass the CDI Lite TCK, ArC also has a *strict* mode. This mode enables additional validations and disables certain improvements that conflict with the specification.

To enable the strict mode, use the following configuration:

```properties
quarkus.arc.strict-compatibility=true
```

Some other features affect specification compatibility as well:

- [Transformation of unproxyable classes](#unproxyable_classes_transformation)
- [Unused beans removal](#remove_unused_beans)

To get a behavior closer to the specification, these features should be disabled.

Applications are recommended to use the default, non-strict mode, which makes CDI more convenient to use. The "strictness" of the strict mode (the set of additional validations and the set of disabled improvements on top of the CDI specification) may change over time.

<a id="arc-configuration-reference"></a>
## ArC Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-arc_quarkus-arc-remove-unused-beans"></a> [`quarkus.arc.remove-unused-beans`](#quarkus-arc_quarkus-arc-remove-unused-beans) config_property_copy_button:+++quarkus.arc.remove-unused-beans+++[]<br><br>- If set to `all` (or `true`) the container will attempt to remove all unused beans.<br>- If set to `none` (or `false`) no beans will ever be removed even if they are unused (according to the criteria set out below)<br>- If set to `fwk`, then all unused beans will be removed, except the unused beans whose classes are declared in the application code<br><br>An unused bean:<br><br>\* is not a built-in bean or interceptor, \* is not eligible for injection to any injection point, \* is not excluded by any extension, \* does not have a name, \* does not declare an observer, \* does not declare any producer which is eligible for injection to any injection point, \* is not directly eligible for injection into any `jakarta.enterprise.inject.Instance` injection point<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_REMOVE_UNUSED_BEANS+++[] | string | `all` |
| [lock] <a id="quarkus-arc_quarkus-arc-auto-inject-fields"></a> [`quarkus.arc.auto-inject-fields`](#quarkus-arc_quarkus-arc-auto-inject-fields) config_property_copy_button:+++quarkus.arc.auto-inject-fields+++[]<br><br>If set to true `@Inject` is automatically added to all non-static non-final fields that are annotated with one of the annotations defined by `AutoInjectAnnotationBuildItem`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_AUTO_INJECT_FIELDS+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-transform-unproxyable-classes"></a> [`quarkus.arc.transform-unproxyable-classes`](#quarkus-arc_quarkus-arc-transform-unproxyable-classes) config_property_copy_button:+++quarkus.arc.transform-unproxyable-classes+++[]<br><br>If set to true, the bytecode of unproxyable beans will be transformed. This ensures that a proxy/subclass can be created properly. If the value is set to false, then an exception is thrown at build time indicating that a subclass/proxy could not be created. Quarkus performs the following transformations when this setting is enabled:<br><br>\* Remove 'final' modifier from classes and methods when a proxy is required. \* Create a no-args constructor if needed. \* Makes private no-args constructors package-private if necessary.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_TRANSFORM_UNPROXYABLE_CLASSES+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-transform-private-injected-fields"></a> [`quarkus.arc.transform-private-injected-fields`](#quarkus-arc_quarkus-arc-transform-private-injected-fields) config_property_copy_button:+++quarkus.arc.transform-private-injected-fields+++[]<br><br>If set to true, the bytecode of private fields that are injection points will be transformed to package private. This ensures that field injection can be performed completely reflection-free. If the value is set to false, then a reflection fallback is used to perform the injection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_TRANSFORM_PRIVATE_INJECTED_FIELDS+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-fail-on-intercepted-private-method"></a> [`quarkus.arc.fail-on-intercepted-private-method`](#quarkus-arc_quarkus-arc-fail-on-intercepted-private-method) config_property_copy_button:+++quarkus.arc.fail-on-intercepted-private-method+++[]<br><br>If set to true (the default), the build fails if a private method that is neither an observer nor a producer, is annotated with an interceptor binding. An example of this is the use of `Transactional` on a private method of a bean. If set to false, Quarkus simply logs a warning that the annotation will be ignored.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_FAIL_ON_INTERCEPTED_PRIVATE_METHOD+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-selected-alternatives"></a> [`quarkus.arc.selected-alternatives`](#quarkus-arc_quarkus-arc-selected-alternatives) config_property_copy_button:+++quarkus.arc.selected-alternatives+++[]<br><br>The list of selected alternatives for an application.<br><br>An element value can be:<br><br>\* a fully qualified class name, i.e. `org.acme.Foo` \* a simple class name as defined by `Class#getSimpleName()`, i.e. `Foo` \* a package name with suffix `.*`, i.e. `org.acme.*`, matches a package \* a package name with suffix `.**`, i.e. `org.acme.**`, matches a package that starts with the value<br><br>Each element value is used to match an alternative bean class, an alternative stereotype annotation type or a bean class that declares an alternative producer. If any value matches then the priority of `Integer#MAX_VALUE` is used for the relevant bean. The priority declared via `jakarta.annotation.Priority` is overridden.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_SELECTED_ALTERNATIVES+++[] | list of string |  |
| [lock] <a id="quarkus-arc_quarkus-arc-auto-producer-methods"></a> [`quarkus.arc.auto-producer-methods`](#quarkus-arc_quarkus-arc-auto-producer-methods) config_property_copy_button:+++quarkus.arc.auto-producer-methods+++[]<br><br>If set to true then `jakarta.enterprise.inject.Produces` is automatically added to all non-void methods that are annotated with a scope annotation, a stereotype or a qualifier, and are not annotated with `Inject` or `Produces`, and no parameter is annotated with `Disposes`, `Observes` or `ObservesAsync`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_AUTO_PRODUCER_METHODS+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-exclude-types"></a> [`quarkus.arc.exclude-types`](#quarkus-arc_quarkus-arc-exclude-types) config_property_copy_button:+++quarkus.arc.exclude-types+++[]<br><br>The list of types that should be excluded from discovery.<br><br>An element value can be:<br><br>\* a fully qualified class name, i.e. `org.acme.Foo` \* a simple class name as defined by `Class#getSimpleName()`, i.e. `Foo` \* a package name with suffix `.*`, i.e. `org.acme.*`, matches a package \* a package name with suffix `.**`, i.e. `org.acme.**`, matches a package that starts with the value<br><br>If any element value matches a discovered type then the type is excluded from discovery, i.e. no beans and observer methods are created from this type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_EXCLUDE_TYPES+++[] | list of string |  |
| [lock] <a id="quarkus-arc_quarkus-arc-unremovable-types"></a> [`quarkus.arc.unremovable-types`](#quarkus-arc_quarkus-arc-unremovable-types) config_property_copy_button:+++quarkus.arc.unremovable-types+++[]<br><br>List of types that should be considered unremovable regardless of whether they are directly used or not. This is a configuration option equivalent to using `io.quarkus.arc.Unremovable` annotation.<br><br>An element value can be:<br><br>\* a fully qualified class name, i.e. `org.acme.Foo` \* a simple class name as defined by `Class#getSimpleName()`, i.e. `Foo` \* a package name with suffix `.*`, i.e. `org.acme.*`, matches a package \* a package name with suffix `.**`, i.e. `org.acme.**`, matches a package that starts with the value<br><br>If any element value matches a discovered bean, then such a bean is considered unremovable.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_UNREMOVABLE_TYPES+++[] | list of string |  |
| <a id="quarkus-arc_section_quarkus-arc-exclude-dependency"></a> [Artifacts that should be excluded from discovery](#quarkus-arc_section_quarkus-arc-exclude-dependency) | Type | Default |
| [lock] <a id="quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-group-id"></a> [`quarkus.arc.exclude-dependency."dependency-name".group-id`](#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-group-id) config_property_copy_button:+++quarkus.arc.exclude-dependency."dependency-name".group-id+++[]<br><br>The maven groupId of the artifact.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__GROUP_ID+++[] | string | required [exclamation circle] |
| [lock] <a id="quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-artifact-id"></a> [`quarkus.arc.exclude-dependency."dependency-name".artifact-id`](#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-artifact-id) config_property_copy_button:+++quarkus.arc.exclude-dependency."dependency-name".artifact-id+++[]<br><br>The maven artifactId of the artifact (optional).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__ARTIFACT_ID+++[] | string |  |
| [lock] <a id="quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-classifier"></a> [`quarkus.arc.exclude-dependency."dependency-name".classifier`](#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-classifier) config_property_copy_button:+++quarkus.arc.exclude-dependency."dependency-name".classifier+++[]<br><br>The maven classifier of the artifact (optional).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__CLASSIFIER+++[] | string |  |
| [lock] <a id="quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-type"></a> [`quarkus.arc.exclude-dependency."dependency-name".type`](#quarkus-arc_quarkus-arc-exclude-dependency-dependency-name-type) config_property_copy_button:+++quarkus.arc.exclude-dependency."dependency-name".type+++[]<br><br>The maven type of the artifact.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_EXCLUDE_DEPENDENCY__DEPENDENCY_NAME__TYPE+++[] | string | `jar` |
| [lock] <a id="quarkus-arc_quarkus-arc-detect-unused-false-positives"></a> [`quarkus.arc.detect-unused-false-positives`](#quarkus-arc_quarkus-arc-detect-unused-false-positives) config_property_copy_button:+++quarkus.arc.detect-unused-false-positives+++[]<br><br>If set to true then the container attempts to detect "unused removed beans" false positives during programmatic lookup at runtime. You can disable this feature to conserve some memory when running your application in production.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_DETECT_UNUSED_FALSE_POSITIVES+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-detect-wrong-annotations"></a> [`quarkus.arc.detect-wrong-annotations`](#quarkus-arc_quarkus-arc-detect-wrong-annotations) config_property_copy_button:+++quarkus.arc.detect-wrong-annotations+++[]<br><br>If set to true then the container attempts to detect *wrong* usages of annotations and eventually fails the build to prevent unexpected behavior of a Quarkus application.<br><br>A typical example is `@jakarta.ejb.Singleton` which is often confused with `@jakarta.inject.Singleton`. As a result a component annotated with `@jakarta.ejb.Singleton` would be completely ignored. Another example is an inner class annotated with a scope annotation - this component would be again completely ignored.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_DETECT_WRONG_ANNOTATIONS+++[] | boolean | `true` |
| [lock] <a id="quarkus-arc_quarkus-arc-strict-compatibility"></a> [`quarkus.arc.strict-compatibility`](#quarkus-arc_quarkus-arc-strict-compatibility) config_property_copy_button:+++quarkus.arc.strict-compatibility+++[]<br><br>If set to `true`, the container will perform additional validations mandated by the CDI specification. Some improvements on top of the CDI specification may be disabled. Applications that work as expected in the strict mode should work without a change in the default, non-strict mode.<br><br>The strict mode is mainly introduced to allow passing the CDI Lite TCK. Applications are recommended to use the default, non-strict mode, which makes CDI more convenient to use. The "strictness" of the strict mode (the set of additional validations and the set of disabled improvements on top of the CDI specification) may change over time.<br><br>Note that `transform-unproxyable-classes` and `remove-unused-beans` also has effect on specification compatibility. You may want to disable these features to get behavior closer to the specification.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_STRICT_COMPATIBILITY+++[] | boolean | `false` |
| [lock] <a id="quarkus-arc_quarkus-arc-dev-mode-monitoring-enabled"></a> [`quarkus.arc.dev-mode.monitoring-enabled`](#quarkus-arc_quarkus-arc-dev-mode-monitoring-enabled) config_property_copy_button:+++quarkus.arc.dev-mode.monitoring-enabled+++[]<br><br>If set to true then the container monitors business method invocations and fired events during the development mode.<br><br>> [!NOTE]<br>> This config property should not be changed in the development mode as it requires a full rebuild of the application<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_DEV_MODE_MONITORING_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-arc_quarkus-arc-dev-mode-generate-dependency-graphs"></a> [`quarkus.arc.dev-mode.generate-dependency-graphs`](#quarkus-arc_quarkus-arc-dev-mode-generate-dependency-graphs) config_property_copy_button:+++quarkus.arc.dev-mode.generate-dependency-graphs+++[]<br><br>If set to `true` then the dependency graphs are generated and available in the Dev UI. If set to `auto` then the dependency graphs are generated if there's less than 1000 beans in the application. If set to `false` the dependency graphs are not generated.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_DEV_MODE_GENERATE_DEPENDENCY_GRAPHS+++[] | `true`, `false`, `auto` | `auto` |
| [lock] <a id="quarkus-arc_quarkus-arc-test-disable-application-lifecycle-observers"></a> [`quarkus.arc.test.disable-application-lifecycle-observers`](#quarkus-arc_quarkus-arc-test-disable-application-lifecycle-observers) config_property_copy_button:+++quarkus.arc.test.disable-application-lifecycle-observers+++[]<br><br>If set to true then disable `StartupEvent` and `ShutdownEvent` observers declared on application bean classes during the tests.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_TEST_DISABLE_APPLICATION_LIFECYCLE_OBSERVERS+++[] | boolean | `false` |
| [lock] <a id="quarkus-arc_quarkus-arc-ignored-split-packages"></a> [`quarkus.arc.ignored-split-packages`](#quarkus-arc_quarkus-arc-ignored-split-packages) config_property_copy_button:+++quarkus.arc.ignored-split-packages+++[]<br><br>The list of packages that will not be checked for split package issues.<br><br>A package string representation can be:<br><br>\* a full name of the package, i.e. `org.acme.foo` \* a package name with suffix `.*`, i.e. `org.acme.*`, which matches a package that starts with provided value<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_IGNORED_SPLIT_PACKAGES+++[] | list of string |  |
| [lock] <a id="quarkus-arc_quarkus-arc-context-propagation-enabled"></a> [`quarkus.arc.context-propagation.enabled`](#quarkus-arc_quarkus-arc-context-propagation-enabled) config_property_copy_button:+++quarkus.arc.context-propagation.enabled+++[]<br><br>If set to true and the SmallRye Context Propagation extension is present then the CDI contexts will be propagated by means of the MicroProfile Context Propagation API. Specifically, a `org.eclipse.microprofile.context.spi.ThreadContextProvider` implementation is registered. On the other hand, if set to false then the MicroProfile Context Propagation API will never be used to propagate the CDI contexts. Note that the CDI contexts may be propagated in a different way though. For example with the Vertx duplicated context.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_ARC_CONTEXT_PROPAGATION_ENABLED+++[] | boolean | `true` |
