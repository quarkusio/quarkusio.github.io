# CDI integration guide

ArC, the CDI container in Quarkus, is bootstrapped at build time. To integrate with the container, [CDI Build Compatible Extensions](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#spi_lite) can be used, as well as a Quarkus-specific extension API. CDI Portable Extensions are not and cannot be supported. This guide focuses on the Quarkus-specific extensions API.

The container is bootstrapped in multiple phases. From a high level perspective these phases go as follows:

1. Initialization
2. Bean discovery
3. Registration of synthetic components
4. Validation

In the *initialization* phase, the preparatory work is being carried out, and custom contexts are registered. *Bean discovery* is then the process where the container analyzes all application classes, identifies beans and wires them all together based on the provided metadata. Subsequently, the extensions can register *synthetic components*. Attributes of these components are fully controlled by the extensions, i.e. are not derived from an existing class. Finally, the *deployment is validated*. For example, the container validates every injection point in the application and fails the build if there is no bean that satisfies the given required type and qualifiers.

> [!TIP]
> You can see more information about the bootstrap by enabling additional logging. Simply run the Maven build with `-X` or `--debug` and grep the lines that contain `io.quarkus.arc`. In [dev mode](/guides/cdi-reference/index.md#dev_mode), you can use `quarkus.log.category."io.quarkus.arc.processor".level=DEBUG` and two special endpoints are also registered automatically to provide some basic debug info in the JSON format.

Quarkus build steps can produce and consume various build items and hook into each phase. In the following sections, we will describe all the relevant build items and common scenarios.

## Metadata Sources

Classes and annotations are the primary source of bean-level metadata. The initial metadata are read from the *bean archive index*, an immutable [Jandex index](https://github.com/smallrye/jandex) which is built from various sources during [bean discovery](/guides/cdi-reference/index.md#bean_discovery). However, extensions can add, remove or transform the metadata at certain stages of the bootstrap. Moreover, extensions can also register [synthetic components](#synthetic_beans). This is an important aspect to realize when integrating CDI components in Quarkus.

This way, extensions can turn classes, that would be otherwise ignored, into beans and vice versa. For example, a class that declares a `@Scheduled` method is always registered as a bean even if it is not annotated with a bean defining annotation and would be normally ignored.

## Use Case - My Class Is Not Recognized as a Bean

An `UnsatisfiedResolutionException` indicates a problem during [typesafe resolution](/guides/cdi/index.md#typesafe_resolution). Sometimes an injection point cannot be satisfied even if there is a class on the classpath that appears to be eligible for injection. There are several reasons why a class is not recognized and also several ways to fix it. In the first step, we should identify the *reason*.

<a id="additional_bean_build_item"></a>
### *Reason 1*: Class Is Not discovered

Quarkus has a [simplified discovery](/guides/cdi-reference/index.md#bean_discovery). It might happen that the class is not part of the application index. For example, classes from the *runtime module* of a Quarkus extension are not indexed automatically.

*Solution*: Use the `AdditionalBeanBuildItem`. This build item can be used to specify one or more additional classes to be analyzed during the discovery. Additional bean classes are transparently added to the application index processed by the container.

> [!IMPORTANT]
> It is not possible to conditionally enable/disable additional beans via the `@IfBuildProfile`, `@UnlessBuildProfile`, `@IfBuildProperty` and `@UnlessBuildProperty` annotations as described in [enable_build_profile](/guides/cdi-reference/index.md#enable_build_profile) and [enable_build_properties](/guides/cdi-reference/index.md#enable_build_properties). Extensions should inspect the configuration or the current profile and only produce an `AdditionalBeanBuildItem` if really needed.

**`AdditionalBeanBuildItem` Example**

```java
@BuildStep
AdditionalBeanBuildItem additionalBeans() {
     return new AdditionalBeanBuildItem(SmallRyeHealthReporter.class, HealthServlet.class); <1>
}
```

1. `AdditionalBeanBuildItem.Builder` can be used for more complex use cases.

Bean classes added via `AdditionalBeanBuildItem` are *removable* by default. If the container considers them [unused](/guides/cdi-reference/index.md#remove_unused_beans), they are just ignored. However, you can use `AdditionalBeanBuildItem.Builder.setUnremovable()` method to instruct the container to never remove bean classes registered via this build item. See also [Removing Unused Beans](/guides/cdi-reference/index.md#remove_unused_beans) and [Reason 3: Class Was Discovered and Has a Bean Defining Annotation but Was Removed](#unremovable_builditem) for more details.

It is also possible to set the default scope via `AdditionalBeanBuildItem.Builder#setDefaultScope()`. The default scope is only used if there is no scope declared on the bean class.

> [!NOTE]
> If no default scope is specified the `@Dependent` pseudo-scope is used.

### *Reason 2*: Class Is Discovered but Has No Bean Defining Annotation

In Quarkus, the application is represented by a single bean archive with the [bean discovery mode `annotated`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#default_bean_discovery). Therefore, bean classes that don't have a [bean defining annotation](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#bean_defining_annotations) are ignored. Bean defining annotations are declared on the class-level and include scopes, stereotypes and `@Interceptor`.

*Solution 1*: Use the `AutoAddScopeBuildItem`. This build item can be used to add a scope to a class that meets certain conditions.

**`AutoAddScopeBuildItem` Example**

```java
@BuildStep
AutoAddScopeBuildItem autoAddScope() {
   return AutoAddScopeBuildItem.builder().containsAnnotations(SCHEDULED_NAME, SCHEDULES_NAME) <1>
      .defaultScope(BuiltinScope.SINGLETON) <2>
      .build();
}
```

1. Find all classes annotated with `@Scheduled`.
2. Add `@Singleton` as default scope. Classes already annotated with a scope are skipped automatically.

*Solution 2*: If you need to process classes annotated with a specific annotation, then it's possible to extend the set of bean defining annotations via the `BeanDefiningAnnotationBuildItem`.

**`BeanDefiningAnnotationBuildItem` Example**

```java
@BuildStep
BeanDefiningAnnotationBuildItem additionalBeanDefiningAnnotation() {
   return new BeanDefiningAnnotationBuildItem(Annotations.GRAPHQL_API); <1>
}
```

1. Add `org.eclipse.microprofile.graphql.GraphQLApi` to the set of bean defining annotations.

Bean classes added via `BeanDefiningAnnotationBuildItem` are *not removable* by default, i.e. the resulting beans must not be removed even if they are considered unused. However, you can change the default behaviour. See also [Removing Unused Beans](/guides/cdi-reference/index.md#remove_unused_beans) and [Reason 3: Class Was Discovered and Has a Bean Defining Annotation but Was Removed](#unremovable_builditem) for more details.

It is also possible to specify the default scope. The default scope is only used if there is no scope declared on the bean class.

> [!NOTE]
> If no default scope is specified the `@Dependent` pseudo-scope is used.

<a id="unremovable_builditem"></a>
### *Reason 3*: Class Was Discovered and Has a Bean Defining Annotation but Was Removed

The container attempts to [remove all unused beans](/guides/cdi-reference/index.md#remove_unused_beans) during the build by default. This optimization allows for *framework-level dead code elimination*. In a few special cases, it's not possible to correctly identify an unused bean. In particular, Quarkus is not able to detect the usage of the `CDI.current()` static method yet. Extensions can eliminate possible false positives by producing an `UnremovableBeanBuildItem`.

**`UnremovableBeanBuildItem` Example**

```java
@BuildStep
UnremovableBeanBuildItem unremovableBeans() {
   return UnremovableBeanBuildItem.targetWithAnnotation(STARTUP_NAME); <1>
}
```

1. Make all classes annotated with `@Startup` unremovable.

## Use Case - My Annotation Is Not Recognized as a Qualifier or an Interceptor Binding

It is likely that the annotation class is not part of the application index. For example, classes from the *runtime module* of a Quarkus extension are not indexed automatically.

*Solution*: Use the `AdditionalBeanBuildItem` as described in [Reason 1: Class Is Not discovered](#additional_bean_build_item).

<a id="annotations_transformer_build_item"></a>
## Use Case - I Need To Transform Annotation Metadata

In some cases, it's useful to be able to modify the annotation metadata. Quarkus provides a powerful alternative to [`jakarta.enterprise.inject.spi.ProcessAnnotatedType`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#process_annotated_type) and [`jakarta.enterprise.inject.build.compatible.spi.Enhancement`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#bce_enhancement). With an `AnnotationsTransformerBuildItem` it's possible to override the annotations that exist on bean classes.

> [!NOTE]
> Keep in mind that annotation transformers must be produced *before* the bean discovery starts.

For example, you might want to add an interceptor binding to a specific bean class. You can use a convenient builder API to create a transformation instance:

**Builder Example**

```java
@BuildStep
AnnotationsTransformerBuildItem transform() {
    return new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses() // <1>
        .whenClass(DotName.createSimple("org.acme.Bar")) // <2>
        .transform(t -> t.add(MyInterceptorBinding.class))); // <3>
}
```

1. The transformer is only applied to classes.
2. Only apply the transformation if the class is `org.acme.Bar`.
3. Add the `@MyInterceptorBinding` annotation.

The example above can be rewritten with an anonymous class:

**`AnnotationsTransformerBuildItem` Example**

```java
@BuildStep
AnnotationsTransformerBuildItem transform() {
    return new AnnotationsTransformerBuildItem(new AnnotationTransformation() {
        public boolean supports(AnnotationTarget.Kind kind) {
            return kind == AnnotationTarget.Kind.CLASS; // <1>
        }

        public void apply(TransformationContext context) {
            if (context.declaration().asClass().name().toString().equals("org.acme.Bar")) {
                context.add(MyInterceptorBinding.class); // <2>
            }
        }
    });
}
```

1. The transformer is only applied to classes.
2. If the class name equals to `org.acme.Bar` then add `@MyInterceptorBinding`.

> [!NOTE]
> The previous `AnnotationsTransformer` API from ArC is still supported, but the new `AnnotationTransformation` API from Jandex is preferred.

Build steps can query the transformed annotations for a given annotation target via the `TransformedAnnotationsBuildItem`.

**`TransformedAnnotationsBuildItem` Example**

```java
@BuildStep
void queryAnnotations(TransformedAnnotationsBuildItem transformedAnnotations,
        BuildProducer<MyBuildItem> myBuildItem) {
    ClassInfo myClazz = ...;
    if (transformedAnnotations.getAnnotations(myClazz).isEmpty()) { // <1>
        myBuildItem.produce(new MyBuildItem());
    }
}
```

1. `TransformedAnnotationsBuildItem.getAnnotations()` will return a possibly transformed set of annotations.

> [!NOTE]
> There are other build items specialized in transformation: [Use Case - Additional Interceptor Bindings](#additional_interceptor_bindings) and [Use Case - Injection Point Transformation](#injection_point_transformation).

### How to Enable Trace Logging for Annotation Transformers

You can set the `TRACE` level for the category `io.quarkus.arc.processor` and try to analyze the log output afterwards.

**`application.properties` Example**

```properties
quarkus.log.category."io.quarkus.arc.processor".min-level=TRACE <1>
quarkus.log.category."io.quarkus.arc.processor".level=TRACE
```

1. You also need to adjust the minimum log level for the relevant category.

<a id="inspect_beans"></a>
## Use Case - Inspect Beans, Observers and Injection Points

### *Solution 1*: `BeanDiscoveryFinishedBuildItem`

Consumers of `BeanDiscoveryFinishedBuildItem` can easily inspect all class-based beans, observers and injection points registered in the application. However, synthetic beans and observers are *not included* because this build item is produced *before* the synthetic components are registered.

Additionally, the bean resolver returned from `BeanDiscoveryFinishedBuildItem#getBeanResolver()` can be used to apply the type-safe resolution rules, e.g. to find out whether there is a bean that would satisfy certain combination of required type and qualifiers.

**`BeanDiscoveryFinishedBuildItem` Example**

```java
@BuildStep
void doSomethingWithNamedBeans(BeanDiscoveryFinishedBuildItem beanDiscovery, BuildProducer<NamedBeansBuildItem> namedBeans) {
   List<BeanInfo> namedBeans = beanDiscovery.beanStream().withName().collect(toList())); <1>
   namedBeans.produce(new NamedBeansBuildItem(namedBeans));
}
```

1. The resulting list will not contain `@Named` synthetic beans.

### *Solution 2*: `SynthesisFinishedBuildItem`

Consumers of `SynthesisFinishedBuildItem` can easily inspect all beans, observers and injection points registered in the application. Synthetic beans and observers are included because this build item is produced *after* the synthetic components are registered.

Additionally, the bean resolver returned from `SynthesisFinishedBuildItem#getBeanResolver()` can be used to apply the type-safe resolution rules, e.g. to find out whether there is a bean that would satisfy certain combination of required type and qualifiers.

**`SynthesisFinishedBuildItem` Example**

```java
@BuildStep
void doSomethingWithNamedBeans(SynthesisFinishedBuildItem synthesisFinished, BuildProducer<NamedBeansBuildItem> namedBeans) {
   List<BeanInfo> namedBeans = synthesisFinished.beanStream().withName().collect(toList())); <1>
   namedBeans.produce(new NamedBeansBuildItem(namedBeans));
}
```

1. The resulting list will contain `@Named` synthetic beans.

<a id="synthetic_beans"></a>
## Use Case - The Need for Synthetic Beans

Sometimes it is practical to be able to register a *synthetic bean*. Bean attributes of a synthetic bean are not derived from a Java class, method or field. Instead, all the attributes are defined by an extension. In regular CDI, this could be achieved using the [`AfterBeanDiscovery.addBean()`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#after_bean_discovery) and [`SyntheticComponents.addBean()`](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html#bce_synthesis) methods.

*Solution*: If you need to register a synthetic bean then use the `SyntheticBeanBuildItem`.

**`SyntheticBeanBuildItem` Example 1**

```java
@BuildStep
SyntheticBeanBuildItem syntheticBean() {
   return SyntheticBeanBuildItem.configure(String.class)
             .qualifiers(AnnotationInstance.builder(MyQualifier.class).build())
             .creator(mc -> mc.returnValue(mc.load("foo"))) <1>
             .done();
}
```

1. Generate the bytecode of the `jakarta.enterprise.context.spi.Contextual#create(CreationalContext<T>)` implementation.

The output of a bean configurator is recorded as bytecode. Therefore, there are some limitations in how a synthetic bean instance is created at runtime. You can:

1. Generate the bytecode of the `Contextual#create(CreationalContext<T>)` method directly via `ExtendedBeanConfigurator.creator(Consumer<MethodCreator>)`.
2. Pass a subclass of `io.quarkus.arc.BeanCreator` via `ExtendedBeanConfigurator#creator(Class<? extends BeanCreator<U>>)`, and possibly specify some build-time parameters via `ExtendedBeanConfigurator#param()` and synthetic injection points via `ExtendedBeanConfigurator#addInjectionPoint()`.
3. Produce the runtime instance through a proxy returned from a [`@Recorder` method](/guides/writing-extensions/index.md#bytecode-recording) and set it via `ExtendedBeanConfigurator#runtimeValue(RuntimeValue<?>)`, `ExtendedBeanConfigurator#runtimeProxy(Object)`,  `ExtendedBeanConfigurator#supplier(Supplier<?>)` or `ExtendedBeanConfigurator#createWith(Function<SyntheticCreationalContext<?>, <?>)`.

**`SyntheticBeanBuildItem` Example 2**

```java
@BuildStep
@Record(STATIC_INIT) <1>
SyntheticBeanBuildItem syntheticBean(TestRecorder recorder) {
   return SyntheticBeanBuildItem.configure(Foo.class).scope(Singleton.class)
                .runtimeValue(recorder.createFoo()) <2>
                .done();
}
```

1. By default, a synthetic bean is initialized during `STATIC_INIT`.
2. The bean instance is supplied by a value returned from a recorder method.

It is also possible to create a generic synthetic bean `Foo<Bar>`.

**`SyntheticBeanBuildItem` Example 3**

```java
@BuildStep
@Record(STATIC_INIT)
SyntheticBeanBuildItem syntheticBean(TestRecorder recorder) {
   return SyntheticBeanBuildItem.configure(Foo.class)
                .types(ParameterizedType.create(Foo.class, ClassType.create(Bar.class)))) <1>
                .scope(Singleton.class)
                .runtimeValue(recorder.createFooBar())
                .done();
}
```

1. `types()` or `addType()` must be used to specify the generic type.

It is possible to mark a synthetic bean to be initialized during `RUNTIME_INIT`. See the [Three Phases of Bootstrap and Quarkus Philosophy](/guides/writing-extensions/index.md#bootstrap-three-phases) for more information about the difference between `STATIC_INIT` and `RUNTIME_INIT`.

**`RUNTIME_INIT` `SyntheticBeanBuildItem` Example**

```java
@BuildStep
@Record(RUNTIME_INIT) <1>
SyntheticBeanBuildItem syntheticBean(TestRecorder recorder) {
   return SyntheticBeanBuildItem.configure(Foo.class).scope(Singleton.class)
                .setRuntimeInit() <2>
                .runtimeValue(recorder.createFoo())
                .done();
}
```

1. The recorder must be executed in the `ExecutionTime.RUNTIME_INIT` phase.
2. The bean instance is initialized during `RUNTIME_INIT`.

> [!IMPORTANT]
> Synthetic beans initialized during `RUNTIME_INIT` must not be accessed during `STATIC_INIT`. `RUNTIME_INIT` build steps that access a runtime-init synthetic bean should consume the `SyntheticBeansRuntimeInitBuildItem`:
>
> ```java
> @BuildStep
> @Record(RUNTIME_INIT)
> @Consume(SyntheticBeansRuntimeInitBuildItem.class) <1>
> void accessFoo(TestRecorder recorder) {
>    recorder.foo(); <2>
> }
> ```
>
> 1. This build step must be executed after `syntheticBean()` completes.
> 2. This recorder method results in an invocation upon the `Foo` bean instance and thus we need to make sure that the build step is executed after all synthetic beans are initialized.

> [!NOTE]
> It is also possible to use the `BeanRegistrationPhaseBuildItem` to register a synthetic bean. However, we recommend extension authors to stick with `SyntheticBeanBuildItem` which is more idiomatic for Quarkus.

### Synthetic Injection Points

A synthetic bean may register a synthetic injection point via the `ExtendedBeanConfigurator#addInjectionPoint()` method. This injection point is validated at build time and considered when [detecting unused beans](/guides/cdi-reference/index.md#remove_unused_beans). The injected reference is accessible through the `SyntheticCreationalContext#getInjectedReference()` methods at runtime.

**Synthetic Injection Point - Build Step Example**

```java
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

@BuildStep
@Record(RUNTIME_INIT) <1>
SyntheticBeanBuildItem syntheticBean(TestRecorder recorder) {
   return SyntheticBeanBuildItem.configure(Foo.class)
                .scope(Singleton.class)
                .addInjectionPoint(ClassType.create(DotName.createSimple(Bar.class))) <2>
                .createWith(recorder.createFoo()) <3>
                .done();
}
```

1. The bean instance is initialized during `RUNTIME_INIT`.
2. A synthetic injection point with required type `Bar` was added; this is an equivalent of `@Inject Bar`.
3. The bean instance is created with a function returned from a recorder method.

**Synthetic Injection Point - Recorder Example**

```java
@Recorder
public class TestRecorder {

   public Function<SyntheticCreationalContext<Foo>, Foo> createFoo() {
     return (context) -> {
        return new Foo(context.getInjectedReference(Bar.class)); <1>
     };
   }
}
```

1. Pass a contextual reference of `Bar` to the constructor of `Foo`.

<a id="inactive-synthetic-beans"></a>
### Inactive Synthetic Beans

In the case when one needs to register multiple synthetic beans at build time but only wants a subset of them active at runtime, it is useful to be able to mark a synthetic bean as *inactive*. This is done by configuring a "check active" procedure, which should be a `Supplier<ActiveResult>` obtained from a recorder:

**Inactive Synthetic Bean - Build Step Example**

```java
@BuildStep
@Record(RUNTIME_INIT)
SyntheticBeanBuildItem syntheticBean(TestRecorder recorder) {
    return SyntheticBeanBuildItem.configure(Foo.class)
            .scope(Singleton.class)
            .startup() // <1>
            .checkActive(recorder.isFooActive()) // <2>
            .createWith(recorder.createFoo())
            .done();
}
```

1. A bean that might be inactive is typically initialized eagerly, to make sure that an error is thrown at application startup. If the bean is in fact inactive, but is not injected into an always-active bean, eager initialization is skipped and no error is thrown.
2. Configures the "check active" procedure.

**Inactive Synthetic Bean - Recorder Example**

```java
@Recorder
public class TestRecorder {
    public Supplier<ActiveResult> isFooActive() {
        return () -> {
            if (... should not be active ...) { // <1>
                return ActiveResult.inactive("explanation"); // <2>
            }
            return ActiveResult.active();
        };
    }

    public Function<SyntheticCreationalContext<Foo>, Foo> createFoo() {
        return (context) -> {
            return new Foo();
        };
    }
}
```

1. The condition when the synthetic bean should be inactive.
2. Proper explanation of why the bean is inactive. Another inactive `ActiveResult` may also be provided as a cause, if this bean's inactivity stems from another bean's inactivity.

If an inactive bean is injected somewhere, or is dynamically looked up, an `InactiveBeanException` is thrown. The error message contains the reason (from the `ActiveResult`), the cause chain (also from the `ActiveResult`), and possibly also a list of all injection points that resolve to this bean.

If you want to handle the inactive case gracefully, you should always inject possibly inactive beans using `Instance<>`. You also need to check before obtaining the actual instance:

```java
import io.quarkus.arc.InjectableInstance;

@Inject
InjectableInstance<Foo> foo;

if (foo.getHandle().getBean().isActive()) {
    Foo foo = foo.get();
    ...
}
```

If you want to consume only active beans, you can inject an `InjectableInstance<>` and call `getActive()` to get the single instance or `listActive()` to get all instances:

```java
import io.quarkus.arc.InjectableInstance;

@Inject
@Any
InjectableInstance<Foo> foos;

for (Foo foo : foos.listActive())
    ...
}
```

<a id="synthetic_observers"></a>
## Use Case - Synthetic Observers

Similar to [synthetic beans](#synthetic_beans), the attributes of a synthetic observer method are not derived from a Java method. Instead, all the attributes are defined by an extension.

*Solution*: If you need to register a synthetic observer, use the `ObserverRegistrationPhaseBuildItem`.

> [!IMPORTANT]
> A build step that consumes the `ObserverRegistrationPhaseBuildItem` should always produce an `ObserverConfiguratorBuildItem` or at least inject a `BuildProducer` for this build item, otherwise it could be ignored or processed at the wrong time (e.g. after the correct CDI bootstrap phase).

**`ObserverRegistrationPhaseBuildItem` Example**

```java
@BuildStep
void syntheticObserver(ObserverRegistrationPhaseBuildItem observerRegistrationPhase,
            BuildProducer<MyBuildItem> myBuildItem,
            BuildProducer<ObserverConfiguratorBuildItem> observerConfigurationRegistry) {
   observerConfigurationRegistry.produce(new ObserverConfiguratorBuildItem(observerRegistrationPhase.getContext()
       .configure()
       .beanClass(DotName.createSimple(MyBuildStep.class.getName()))
       .observedType(String.class)
       .notify(mc -> {
           // do some gizmo bytecode generation...
       })));
   myBuildItem.produce(new MyBuildItem());
}
```

The output of an `ObserverConfigurator` is recorded as bytecode. Therefore, there are some limitations in how a synthetic observer is invoked at runtime. Currently, you must generate the bytecode of the method body directly.

<a id="generated_beans"></a>
## Use Case - I Have a Generated Bean Class

No problem. You can generate the bytecode of a bean class manually, and then all you need to do is to produce a `GeneratedBeanBuildItem` instead of `GeneratedClassBuildItem`.

**`GeneratedBeanBuildItem` Example**

```java
@BuildStep
void generatedBean(BuildProducer<GeneratedBeanBuildItem> generatedBeans) {
    ClassOutput beansClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans); <1>
    ClassCreator beanClassCreator = ClassCreator.builder().classOutput(beansClassOutput)
                .className("org.acme.MyBean")
                .build();
    beanClassCreator.addAnnotation(Singleton.class);
    beanClassCreator.close(); <2>
}
```

1. `io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor` makes it easy to produce ``GeneratedBeanBuildItem``s from Gizmo constructs.
2. The resulting bean class is something like `public class @Singleton MyBean { }`.

## Use Case - I Need to Validate the Deployment

Sometimes extensions need to inspect the beans, observers and injection points, then perform additional validations and fail the build if something is wrong.

*Solution*: If an extension needs to validate the deployment, it should use the `ValidationPhaseBuildItem`.

> [!IMPORTANT]
> A build step that consumes the `ValidationPhaseBuildItem` should always produce a `ValidationErrorBuildItem` or at least inject a `BuildProducer` for this build item, otherwise it could be ignored or processed at the wrong time (e.g. after the correct CDI bootstrap phase).

```java
@BuildStep
void validate(ValidationPhaseBuildItem validationPhase,
            BuildProducer<MyBuildItem> myBuildItem,
            BuildProducer<ValidationErrorBuildItem> errors) {
   if (someCondition) {
     errors.produce(new ValidationErrorBuildItem(new IllegalStateException()));
     myBuildItem.produce(new MyBuildItem());
   }
}
```

> [!TIP]
> You can easily filter all registered beans via the convenient `BeanStream` returned from the `ValidationPhaseBuildItem.getContext().beans()` method.

<a id="custom_context"></a>
## Use Case - Register a Custom CDI Context

Sometimes extensions need to extend the set of built-in CDI contexts.

*Solution*: If you need to register a custom context, use the `ContextRegistrationPhaseBuildItem`.

> [!IMPORTANT]
> A build step that consumes the `ContextRegistrationPhaseBuildItem` should always produce a `ContextConfiguratorBuildItem` or at least inject a `BuildProducer` for this build item, otherwise it could be ignored or processed at the wrong time (e.g. after the correct CDI bootstrap phase).

`ContextRegistrationPhaseBuildItem` Example

```java
@BuildStep
ContextConfiguratorBuildItem registerContext(ContextRegistrationPhaseBuildItem phase) {
      return new ContextConfiguratorBuildItem(phase.getContext().configure(TransactionScoped.class).normal().contextClass(TransactionContext.class));
}
```

Additionally, each extension that registers a custom CDI context via `ContextRegistrationPhaseBuildItem` should also produce the `CustomScopeBuildItem` in order to contribute the custom scope annotation name to the set of bean defining annotations.

`CustomScopeBuildItem` Example

```java
@BuildStep
CustomScopeBuildItem customScope() {
   return new CustomScopeBuildItem(DotName.createSimple(TransactionScoped.class.getName()));
}
```

### What if I Need to Know All the Scopes Used in the Application?

*Solution*: You can inject the `CustomScopeAnnotationsBuildItem` in a build step and use the convenient methods such as `CustomScopeAnnotationsBuildItem.isScopeDeclaredOn()`.

<a id="additional_interceptor_bindings"></a>
## Use Case - Additional Interceptor Bindings

In rare cases, it might be handy to programmatically register an existing annotation that is not annotated with `@jakarta.interceptor.InterceptorBinding` as an interceptor binding. This is similar to what CDI achieves through `BeforeBeanDiscovery#addInterceptorBinding()`. We are going to use `InterceptorBindingRegistrarBuildItem` to get it done.

**`InterceptorBindingRegistrarBuildItem` Example**

```java
@BuildStep
InterceptorBindingRegistrarBuildItem addInterceptorBindings() {
    return new InterceptorBindingRegistrarBuildItem(new InterceptorBindingRegistrar() {
        @Override
        public List<InterceptorBinding> getAdditionalBindings() {
            return List.of(InterceptorBinding.of(NotAnInterceptorBinding.class));
        }
    });
}
```

## Use Case - Additional Qualifiers

Sometimes it might be useful to register an existing annotation that is not annotated with `@jakarta.inject.Qualifier` as a CDI qualifier. This is similar to what CDI achieves through `BeforeBeanDiscovery#addQualifier()`. We are going to use `QualifierRegistrarBuildItem` to get it done.

**`QualifierRegistrarBuildItem` Example**

```java
@BuildStep
QualifierRegistrarBuildItem addQualifiers() {
    return new QualifierRegistrarBuildItem(new QualifierRegistrar() {
        @Override
        public Map<DotName, Set<String>> getAdditionalQualifiers() {
            return Collections.singletonMap(DotName.createSimple(NotAQualifier.class.getName()),
                                        Collections.emptySet());
        }
    });
}
```

## Use Case - Additional Stereotypes

It is sometimes useful to register an existing annotation that is not annotated with `@jakarta.enterprise.inject.Stereotype` as a CDI stereotype. This is similar to what CDI achieves through `BeforeBeanDiscovery#addStereotype()`. We are going to use `StereotypeRegistrarBuildItem` to get it done.

**`StereotypeRegistrarBuildItem` Example**

```java
@BuildStep
StereotypeRegistrarBuildItem addStereotypes() {
    return new StereotypeRegistrarBuildItem(new StereotypeRegistrar() {
        @Override
        public Set<DotName> getAdditionalStereotypes() {
            return Collections.singleton(DotName.createSimple(NotAStereotype.class.getName()));
        }
    });
}
```

If the newly registered stereotype annotation doesn't have the appropriate meta-annotations, such as scope or interceptor bindings, use an [annotation transformation](#annotations_transformer_build_item) to add them.

<a id="injection_point_transformation"></a>
## Use Case - Injection Point Transformation

Every now and then, it is handy to be able to change the qualifiers of an injection point programmatically. You can do just that with `InjectionPointTransformerBuildItem`. The following sample shows how to apply a transformation to injection points with type `Foo` that contain a qualifier `MyQualifier`:

**`InjectionPointTransformerBuildItem` Example**

```java
@BuildStep
InjectionPointTransformerBuildItem transformer() {
    return new InjectionPointTransformerBuildItem(new InjectionPointsTransformer() {

        public boolean appliesTo(Type requiredType) {
            return requiredType.name().equals(DotName.createSimple(Foo.class.getName()));
        }

        public void transform(TransformationContext context) {
            if (context.getQualifiers().stream()
                    .anyMatch(a -> a.name().equals(DotName.createSimple(MyQualifier.class.getName())))) {
                context.transform()
                        .removeAll()
                        .add(DotName.createSimple(MyOtherQualifier.class.getName()))
                        .done();
            }
        }
    });
}
```

> [!NOTE]
> In theory, you can use [an `AnnotationsTransformer`](#annotations_transformer_build_item) to achieve the same goal. However, there are few differences that make `InjectionPointsTransformer` more suitable for this particular task: (1) annotation transformers are applied to all classes during bean discovery, whereas `InjectionPointsTransformer` is only applied to discovered injection points after bean discovery; (2) with `InjectionPointsTransformer` you don't need to handle various types of injection points (field, parameters of initializer methods, etc.).

## Use Case - Resource Annotations and Injection

The `ResourceAnnotationBuildItem` can be used to specify resource annotations that make it possible to resolve non-CDI injection points, such as Jakarta EE resources. An integrator must also provide a corresponding `io.quarkus.arc.ResourceReferenceProvider` service provider implementation.

**`ResourceAnnotationBuildItem` Example**

```java
@BuildStep
void setupResourceInjection(BuildProducer<ResourceAnnotationBuildItem> resourceAnnotations,
        BuildProducer<GeneratedServiceProviderBuildItem> serviceProviders) {
    serviceProviders.produce(new GeneratedServiceProviderBuildItem(
        "io.quarkus.arc.ResourceReferenceProvider",
        MyResourceReferenceProvider.class.getName()));
    resourceAnnotations.produce(new ResourceAnnotationBuildItem(DotName.createSimple(MyAnnotation.class.getName())));
}
```

<a id="build_metadata"></a>
## Available Build Time Metadata

Any of the above extensions that operates with `BuildExtension.BuildContext` can leverage certain build time metadata that are generated during build. The built-in keys located in `io.quarkus.arc.processor.BuildExtension.Key` are:

**ANNOTATION_STORE**\
Contains an `AnnotationStore` that keeps information about all `AnnotationTarget` annotations after application of annotation transformers

**INJECTION_POINTS**\
`Collection<InjectionPointInfo>` containing all injection points

**BEANS**\
`Collection<BeanInfo>` containing all beans

**REMOVED_BEANS**\
`Collection<BeanInfo>` containing all the removed beans; see [Removing unused beans](/guides/cdi-reference/index.md#remove_unused_beans) for more information

**OBSERVERS**\
`Collection<ObserverInfo>` containing all observers

**SCOPES**\
`Collection<ScopeInfo>` containing all scopes, including custom ones

**QUALIFIERS**\
`Map<DotName, ClassInfo>` containing all qualifiers

**INTERCEPTOR_BINDINGS**\
`Map<DotName, ClassInfo>` containing all interceptor bindings

**STEREOTYPES**\
`Map<DotName, StereotypeInfo>` containing all stereotypes

To get hold of these, simply query the extension context object for given key. Note that these metadata are made available as build proceeds, which means that extensions can only leverage metadata that were built before the extensions are invoked. If your extension attempts to retrieve metadata that wasn't yet produced, `null` will be returned. Here is a summary of which extensions can access which metadata:

**AnnotationsTransformer**\
Shouldn't rely on any metadata as it could be used at any time in any phase of the bootstrap

**ContextRegistrar**\
Has access to `ANNOTATION_STORE`, `QUALIFIERS`, `INTERCEPTOR_BINDINGS`, `STEREOTYPES`

**InjectionPointsTransformer**\
Has access to `ANNOTATION_STORE`, `QUALIFIERS`, `INTERCEPTOR_BINDINGS`, `STEREOTYPES`

**ObserverTransformer**\
Has access to `ANNOTATION_STORE`, `QUALIFIERS`, `INTERCEPTOR_BINDINGS`, `STEREOTYPES`

**BeanRegistrar**\
Has access to `ANNOTATION_STORE`, `QUALIFIERS`, `INTERCEPTOR_BINDINGS`, `STEREOTYPES`, `BEANS` (class-based beans only), `OBSERVERS` (class-based observers only), `INJECTION_POINTS`

**ObserverRegistrar**\
Has access to `ANNOTATION_STORE`, `QUALIFIERS`, `INTERCEPTOR_BINDINGS`, `STEREOTYPES`, `BEANS`, `OBSERVERS` (class-based observers only), `INJECTION_POINTS`

**BeanDeploymentValidator**\
Has access to all build metadata
