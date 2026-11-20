# Context propagation in Quarkus

Traditional blocking code uses [`ThreadLocal`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html) variables to store contextual objects in order to avoid passing them as parameters everywhere. Many Quarkus extensions require those contextual objects to operate properly: [Quarkus REST (formerly RESTEasy Reactive)](/guides/rest-json/index.md), [ArC](/guides/cdi-reference/index.md) and [Transaction](/guides/transaction/index.md) for example.

If you write reactive/async code, you have to cut your work into a pipeline of code blocks that get executed "later", and in practice after the method you defined them in has returned. As such, `try/finally` blocks as well as `ThreadLocal` variables stop working, because your reactive code gets executed in another thread, after the caller ran its `finally` block.

[SmallRye Context Propagation](https://github.com/smallrye/smallrye-context-propagation) an implementation of [MicroProfile Context Propagation](https://github.com/eclipse/microprofile-context-propagation) was made to make those Quarkus extensions work properly in reactive/async settings. It works by capturing those contextual values that used to be in thread-locals, and restoring them when your code is called.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `context-propagation-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/context-propagation-quickstart).

## Setting it up

If you are using [Mutiny](https://smallrye.io/smallrye-mutiny) (the `quarkus-mutiny` extension), you just need to add the `quarkus-smallrye-context-propagation` extension to enable context propagation.

In other words, add the following dependencies to your build file:

**pom.xml**

```xml
<!-- Quarkus REST extension if not already included -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
<!-- Context Propagation extension -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-context-propagation</artifactId>
</dependency>
```

**build.gradle**

```gradle
// Quarkus REST extension if not already included
implementation("io.quarkus:quarkus-rest")
// Context Propagation extension
implementation("io.quarkus:quarkus-smallrye-context-propagation")
```

With this, you will get context propagation for ArC, Quarkus REST and transactions, if you are using them.

## Usage example with Mutiny

> [!TIP]
> **Mutiny**
>
> This section uses Mutiny reactive types. If you are not familiar with Mutiny, check [Mutiny - an intuitive reactive programming library](/guides/mutiny-primer/index.md).

Let's write a REST endpoint that reads the next 3 items from a [Kafka topic](/guides/kafka/index.md), stores them in a database using [Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md) (all in the same transaction) before returning them to the client, you can do it like this:

```java
    // Get the prices stream
    @Inject
    @Channel("prices") Publisher<Double> prices;

    @Transactional
    @GET
    @Path("/prices")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Publisher<Double> prices() {
        // get the next three prices from the price stream
        return Multi.createFrom().publisher(prices)
                .select().first(3)
                // The items are received from the event loop, so cannot use Hibernate ORM (classic)
                // Switch to a worker thread, the transaction will be propagated
                .emitOn(Infrastructure.getDefaultExecutor())
                .map(price -> {
                    // store each price before we send them
                    Price priceEntity = new Price();
                    priceEntity.value = price;
                    // here we are all in the same transaction
                    // thanks to context propagation
                    priceEntity.persist();
                    return price;
                    // the transaction is committed once the stream completes
                });
    }
```

Notice that thanks to Mutiny support for context propagation, this works out of the box. The 3 items are persisted using the same transaction and this transaction is committed when the stream completes.

## Usage example for `CompletionStage`

If you are using [`CompletionStage`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletionStage.html) you need manual context propagation. You can do that by injecting a `ThreadContext` or `ManagedExecutor` that will propagate every context. For example, here we use the [Vert.x Web Client](/guides/vertx/index.md) to get the list of Star Wars people, then store them in the database using [Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md) (all in the same transaction) before returning them to the client as JSON using [Jackson or JSON-B](/guides/rest-json/index.md):

```java
    @Inject ThreadContext threadContext;
    @Inject ManagedExecutor managedExecutor;
    @Inject Vertx vertx;

    @Transactional
    @GET
    @Path("/people")
    public CompletionStage<List<Person>> people() throws SystemException {
        // Create a REST client to the Star Wars API
        WebClient client = WebClient.create(vertx,
                         new WebClientOptions()
                          .setDefaultHost("swapi.tech")
                          .setDefaultPort(443)
                          .setSsl(true));
        // get the list of Star Wars people, with context capture
        return threadContext.withContextCapture(client.get("/api/people/").send())
                .thenApplyAsync(response -> {
                    JsonObject json = response.bodyAsJsonObject();
                    List<Person> persons = new ArrayList<>(json.getInteger("count"));
                    // Store them in the DB
                    // Note that we're still in the same transaction as the outer method
                    for (Object element : json.getJsonArray("results")) {
                        Person person = new Person();
                        person.name = ((JsonObject) element).getString("name");
                        person.persist();
                        persons.add(person);
                    }
                    return persons;
                }, managedExecutor);
    }
```

Using `ThreadContext` or `ManagedExecutor` you can wrap most useful functional types and `CompletionStage` in order to get context propagated.

> [!NOTE]
> The injected `ManagedExecutor` uses the Quarkus thread pool.

## Overriding which contexts are propagated

By default, all available contexts are propagated. However, you can override this behaviour in several ways.

### Using configuration

The following configuration properties allow you to specify the default sets of propagated contexts:

| Configuration property | Description | Default Value |
| --- | --- | --- |
| `mp.context.ThreadContext.propagated` | The comma-separated set of propagated contexts | `Remaining` (all non-explicitly list contexts) |
| `mp.context.ThreadContext.cleared` | The comma-separated set of cleared contexts | `None` (no context), unless neither the propagated nor cleared sets contain `Remaining`, in which case the default is `Remaining` (all non-explicitly listed contexts) |
| `mp.context.ThreadContext.unchanged` | The comma-separated set of unchanged contexts | `None` (no context) |

The following contexts are available in Quarkus either out of the box, or depending on whether you include their extensions:

| Context Name | Name Constant | Description |
| --- | --- | --- |
| `None` | [`ThreadContext.NONE`](https://javadoc.io/static/org.eclipse.microprofile.context-propagation/microprofile-context-propagation-api/1.2/org/eclipse/microprofile/context/ThreadContext.html#NONE) | Can be used to specify an empty set of contexts, but setting the value to empty works too |
| `Remaining` | [`ThreadContext.ALL_REMAINING`](https://javadoc.io/static/org.eclipse.microprofile.context-propagation/microprofile-context-propagation-api/1.2/org/eclipse/microprofile/context/ThreadContext.html#ALL_REMAINING) | All the contexts that are not explicitly listed in other sets |
| `Transaction` | [`ThreadContext.TRANSACTION`](https://javadoc.io/static/org.eclipse.microprofile.context-propagation/microprofile-context-propagation-api/1.2/org/eclipse/microprofile/context/ThreadContext.html#TRANSACTION) | The JTA transaction context |
| `CDI` | [`ThreadContext.CDI`](https://javadoc.io/static/org.eclipse.microprofile.context-propagation/microprofile-context-propagation-api/1.2/org/eclipse/microprofile/context/ThreadContext.html#CDI) | The CDI (ArC) context |
| `Servlet` | N/A | The servlet context |
| `Jakarta REST` | N/A | The Quarkus REST or RESTEasy Classic context |
| `Application` | [`ThreadContext.APPLICATION`](https://javadoc.io/static/org.eclipse.microprofile.context-propagation/microprofile-context-propagation-api/1.2/org/eclipse/microprofile/context/ThreadContext.html#APPLICATION) | The current `ThreadContextClassLoader` |

### Overriding the propagated contexts using annotations

In order for automatic context propagation, such as Mutiny uses, to be overridden in specific methods, you can use the [`@CurrentThreadContext`](https://javadoc.io/doc/io.smallrye/smallrye-context-propagation-api/latest/io/smallrye/context/api/CurrentThreadContext.html) annotation:

```java
    // Get the prices stream
    @Inject
    @Channel("prices") Publisher<Double> prices;

    @GET
    @Path("/prices")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    // Get rid of all context propagation, since we don't need it here
    @CurrentThreadContext(propagated = {}, unchanged = ThreadContext.ALL_REMAINING)
    public Publisher<Double> prices() {
        // get the next three prices from the price stream
        return Multi.createFrom().publisher(prices)
                .select().first(3);
    }
```

### Overriding the propagated contexts using CDI injection

You can also inject a custom-built `ThreadContext` using the [`@ThreadContextConfig`](https://javadoc.io/doc/io.smallrye/smallrye-context-propagation-api/latest/io/smallrye/context/api/ThreadContextConfig.html) annotation on your injection point:

```java
    // Get the prices stream
    @Inject
    @Channel("prices") Publisher<Double> prices;
    // Get a ThreadContext that doesn't propagate context
    @Inject
    @ThreadContextConfig(unchanged = ThreadContext.ALL_REMAINING)
    SmallRyeThreadContext threadContext;

    @GET
    @Path("/prices")
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    public Publisher<Double> prices() {
        // Get rid of all context propagation, since we don't need it here
        try(CleanAutoCloseable ac = SmallRyeThreadContext.withThreadContext(threadContext)){
            // get the next three prices from the price stream
            return Multi.createFrom().publisher(prices)
                    .select().first(3);
        }
    }
```

Likewise, there is a similar way to inject a configured instance of `ManagedExecutor` using the [`@ManagedExecutorConfig`](https://javadoc.io/doc/io.smallrye/smallrye-context-propagation-api/latest/io/smallrye/context/api/ManagedExecutorConfig.html) annotation:

```java
    // Custom ManagedExecutor with different async limit, queue and no propagation
    @Inject
    @ManagedExecutorConfig(maxAsync = 2, maxQueued = 3, cleared = ThreadContext.ALL_REMAINING)
    ManagedExecutor configuredCustomExecutor;
```

### Sharing configured CDI instances of ManagedExecutor and ThreadContext

If you need to inject the same `ManagedExecutor` or `ThreadContext` into several places and share its capacity, you can name the instance with [`@NamedInstance`](https://javadoc.io/doc/io.smallrye/smallrye-context-propagation-api/latest/io/smallrye/context/api/NamedInstance.html) annotation. `@NamedInstance` is a CDI qualifier and all injections of the same type and name will therefore share the same underlying instance. If you also need to customize your instance, you can do so using `@ManagedExecutorConfig`/`ThreadContextConfig` annotation on one of its injection points:

```java
    // Custom configured ManagedExecutor with name
    @Inject
    @ManagedExecutorConfig(maxAsync = 2, maxQueued = 3, cleared = ThreadContext.ALL_REMAINING)
    @NamedInstance("myExecutor")
    ManagedExecutor sharedConfiguredExecutor;

    // Since this executor has the same name, it will be the same instance as above
    @Inject
    @NamedInstance("myExecutor")
    ManagedExecutor sameExecutor;

    // Custom ThreadContext with a name
    @Inject
    @ThreadContextConfig(unchanged = ThreadContext.ALL_REMAINING)
    @NamedInstance("myContext")
    ThreadContext sharedConfiguredThreadContext;

    // Given equal value of @NamedInstance, this ThreadContext will be the same as the above one
    @Inject
    @NamedInstance("myContext")
    ThreadContext sameContext;
```

<a id="context-propagation-for-cdi"></a>
## Context Propagation for CDI

In terms of CDI, `@RequestScoped`, `@ApplicationScoped` and `@Singleton` beans get propagated and are available in other threads. `@Dependent` beans as well as any custom scoped beans cannot be automatically propagated via CDI Context Propagation.

`@ApplicationScoped` and `@Singleton` beans are always active scopes and as such are easy to deal with - context propagation tasks can work with those beans so long as the CDI container is running. However, `@RequestScoped` beans are a different story. They are only active for a short period of time which can be bound either to HTTP request or some other request/task when manually activated/deactivated. In this case user must be aware that once the original thread gets to an end of a request, it will terminate the context, calling `@PreDestroy` on those beans and then clearing them from the context. Subsequent attempts to access those beans from other threads can result in unexpected behaviour. It is therefore recommended to make sure all tasks using request scoped beans via context propagation are performed in such a manner that they don't outlive the original request duration.

> [!NOTE]
> Due to the above described behavior, it is recommended to avoid using `@PreDestroy` on `@RequestScoped` beans when working with Context Propagation in CDI.
