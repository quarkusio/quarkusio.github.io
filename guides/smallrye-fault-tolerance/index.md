# SmallRye Fault Tolerance

One of the challenges brought by the distributed nature of microservices is that communication with external systems is inherently unreliable. This increases demand on resiliency of applications. To simplify making more resilient applications, Quarkus provides [SmallRye Fault Tolerance](https://github.com/smallrye/smallrye-fault-tolerance/), an implementation of the [MicroProfile Fault Tolerance](https://github.com/eclipse/microprofile-fault-tolerance/) specification.

In this guide, we demonstrate usage of MicroProfile Fault Tolerance annotations such as `@Timeout`, `@Fallback`, `@Retry`, `@CircuitBreaker`, and `@RateLimit`.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## The Scenario

The application built in this guide simulates a simple backend for a gourmet coffee e-shop. It implements a REST endpoint providing information about coffee samples we have on store.

Let's imagine, although it's not implemented as such, that some methods in our endpoint require communication to external services like a database or an external microservice, which introduces a factor of unreliability.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `microprofile-fault-tolerance-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/microprofile-fault-tolerance-quickstart).

## Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:microprofile-fault-tolerance-quickstart \
    --extension='smallrye-fault-tolerance,rest-jackson' \
    --no-code
cd microprofile-fault-tolerance-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=microprofile-fault-tolerance-quickstart \
    -Dextensions='smallrye-fault-tolerance,rest-jackson' \
    -DnoCode
cd microprofile-fault-tolerance-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=microprofile-fault-tolerance-quickstart"`

This command generates a project, importing the extensions for Quarkus REST (formerly RESTEasy Reactive)/Jakarta REST and SmallRye Fault Tolerance.

If you already have your Quarkus project configured, you can add the `smallrye-fault-tolerance` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add smallrye-fault-tolerance
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='smallrye-fault-tolerance'
```

**Gradle**

```bash
./gradlew addExtension --extensions='smallrye-fault-tolerance'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
```

## Preparing an Application: REST Endpoint and CDI Bean

In this section we create a skeleton of our application, so that we have something that we can extend and to which we can add fault tolerance features later on.

First, create a simple entity representing a coffee sample in our store:

```java
package org.acme.microprofile.faulttolerance;

public class Coffee {

    public Integer id;
    public String name;
    public String countryOfOrigin;
    public Integer price;

    public Coffee() {
    }

    public Coffee(Integer id, String name, String countryOfOrigin, Integer price) {
        this.id = id;
        this.name = name;
        this.countryOfOrigin = countryOfOrigin;
        this.price = price;
    }
}
```

Let's continue with a simple CDI bean, that would work as a repository of our coffee samples.

```java
package org.acme.microprofile.faulttolerance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CoffeeRepositoryService {

    private Map<Integer, Coffee> coffeeList = new HashMap<>();

    public CoffeeRepositoryService() {
        coffeeList.put(1, new Coffee(1, "Fernandez Espresso", "Colombia", 23));
        coffeeList.put(2, new Coffee(2, "La Scala Whole Beans", "Bolivia", 18));
        coffeeList.put(3, new Coffee(3, "Dak Lak Filter", "Vietnam", 25));
    }

    public List<Coffee> getAllCoffees() {
        return new ArrayList<>(coffeeList.values());
    }

    public Coffee getCoffeeById(Integer id) {
        return coffeeList.get(id);
    }

    public List<Coffee> getRecommendations(Integer id) {
        if (id == null) {
            return Collections.emptyList();
        }
        return coffeeList.values().stream()
                .filter(coffee -> !id.equals(coffee.id))
                .limit(2)
                .collect(Collectors.toList());
    }
}
```

Finally, create the `org.acme.microprofile.faulttolerance.CoffeeResource` class as follows:

```java
package org.acme.microprofile.faulttolerance;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;

@Path("/coffee")
public class CoffeeResource {

    private static final Logger LOGGER = Logger.getLogger(CoffeeResource.class);

    @Inject
    CoffeeRepositoryService coffeeRepository;

    private AtomicLong counter = new AtomicLong(0);

    @GET
    public List<Coffee> coffees() {
        final Long invocationNumber = counter.getAndIncrement();

        maybeFail(String.format("CoffeeResource#coffees() invocation #%d failed", invocationNumber));

        LOGGER.infof("CoffeeResource#coffees() invocation #%d returning successfully", invocationNumber);
        return coffeeRepository.getAllCoffees();
    }

    private void maybeFail(String failureLogMessage) {
        if (new Random().nextBoolean()) {
            LOGGER.error(failureLogMessage);
            throw new RuntimeException("Resource failure.");
        }
    }
}
```

At this point, we expose a single REST method that will show a list of coffee samples in a JSON format. Note that we introduced some fault making code in our `CoffeeResource#maybeFail()` method, which is going to cause failures in the `CoffeeResource#coffees()` endpoint method in about 50 % of requests.

Why not check that our application works? Run the Quarkus development server with:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

and open `http://localhost:8080/coffee` in your browser. Make a couple of requests (remember, some of them are expected to fail). At least some requests should show us the list of our coffee samples in JSON, the rest will fail with a `RuntimeException` thrown in `CoffeeResource#maybeFail()`.

Congratulations, you've just made a working (although somewhat unreliable) Quarkus application!

## Adding Resiliency: Retries

Let the Quarkus development server running and in your IDE add the `@Retry` annotation to the `CoffeeResource#coffees()` method as follows and save the file:

```java
import org.eclipse.microprofile.faulttolerance.Retry;
...

public class CoffeeResource {
    ...
    @GET
    @Retry(maxRetries = 4)
    public List<Coffee> coffees() {
        ...
    }
    ...
}
```

Hit refresh in your browser. The Quarkus development server will automatically detect the changes and recompile the app for you, so there's no need to restart it.

You can hit refresh couple more times. Practically all requests should now be succeeding. The `CoffeeResource#coffees()` method is still in fact failing in about 50 % of time, but every time it happens, the platform will automatically retry the call!

To see that the failures still happen, check the output of the development server. The log messages should be similar to these:

```
2019-03-06 12:17:41,725 INFO  [org.acm.fau.CoffeeResource] (XNIO-1 task-1) CoffeeResource#coffees() invocation #5 returning successfully
2019-03-06 12:17:44,187 INFO  [org.acm.fau.CoffeeResource] (XNIO-1 task-1) CoffeeResource#coffees() invocation #6 returning successfully
2019-03-06 12:17:45,166 ERROR [org.acm.fau.CoffeeResource] (XNIO-1 task-1) CoffeeResource#coffees() invocation #7 failed
2019-03-06 12:17:45,172 ERROR [org.acm.fau.CoffeeResource] (XNIO-1 task-1) CoffeeResource#coffees() invocation #8 failed
2019-03-06 12:17:45,176 INFO  [org.acm.fau.CoffeeResource] (XNIO-1 task-1) CoffeeResource#coffees() invocation #9 returning successfully
```

You can see that every time an invocation fails, it's immediately followed by another invocation, until one succeeds. Since we allowed 4 retries, it would require 5 invocations to fail in a row, in order for the user to be actually exposed to a failure. Which is fairly unlikely to happen.

## Adding Resiliency: Timeouts

So what else have we got in MicroProfile Fault Tolerance? Let's look into timeouts.

Add following two methods to our `CoffeeResource` endpoint. Again, no need to restart the server, just paste the code and save the file.

```java
import org.eclipse.microprofile.faulttolerance.Timeout;
...
public class CoffeeResource {
    ...
    @GET
    @Path("/{id}/recommendations")
    @Timeout(250)
    public List<Coffee> recommendations(int id) {
        long started = System.currentTimeMillis();
        final long invocationNumber = counter.getAndIncrement();

        try {
            randomDelay();
            LOGGER.infof("CoffeeResource#recommendations() invocation #%d returning successfully", invocationNumber);
            return coffeeRepository.getRecommendations(id);
        } catch (InterruptedException e) {
            LOGGER.errorf("CoffeeResource#recommendations() invocation #%d timed out after %d ms",
                    invocationNumber, System.currentTimeMillis() - started);
            return null;
        }
    }

    private void randomDelay() throws InterruptedException {
        Thread.sleep(new Random().nextInt(500));
    }
}
```

We added some new functionality. We want to be able to recommend some related coffees based on a coffee that a user is currently looking at. It's not a critical functionality, it's a nice-to-have. When the system is overloaded and the logic behind obtaining recommendations takes too long to execute, we would rather time out and render the UI without recommendations.

Note that the timeout was configured to 250 ms, and a random artificial delay between 0 and 500 ms was introduced into the `CoffeeResource#recommendations()` method.

In your browser, go to `http://localhost:8080/coffee/2/recommendations` and hit refresh a couple of times.

You should see some requests time out with `org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException`. Requests that do not time out should show two recommended coffee samples in JSON.

## Adding Resiliency: Fallbacks

Let's make our recommendations feature even better by providing a fallback (and presumably faster) way of getting related coffees.

Add a fallback method to `CoffeeResource` and a `@Fallback` annotation to `CoffeeResource#recommendations()` method as follows:

```java
import java.util.Collections;
import org.eclipse.microprofile.faulttolerance.Fallback;

...
public class CoffeeResource {
    ...
    @Fallback(fallbackMethod = "fallbackRecommendations")
    public List<Coffee> recommendations(int id) {
        ...
    }

    public List<Coffee> fallbackRecommendations(int id) {
        LOGGER.info("Falling back to RecommendationResource#fallbackRecommendations()");
        // safe bet, return something that everybody likes
        return Collections.singletonList(coffeeRepository.getCoffeeById(1));
    }
    ...
}
```

Hit refresh several times on `http://localhost:8080/coffee/2/recommendations`. The `TimeoutException` should not appear anymore. Instead, in case of a timeout, the page will display a single recommendation that we hardcoded in our fallback method `fallbackRecommendations()`, rather than two recommendations returned by the original method.

Check the server output to see that fallback is really happening:

```
2020-01-09 13:21:34,250 INFO  [org.acm.fau.CoffeeResource] (executor-thread-1) CoffeeResource#recommendations() invocation #1 returning successfully
2020-01-09 13:21:36,354 ERROR [org.acm.fau.CoffeeResource] (executor-thread-1) CoffeeResource#recommendations() invocation #2 timed out after 250 ms
2020-01-09 13:21:36,355 INFO  [org.acm.fau.CoffeeResource] (executor-thread-1) Falling back to RecommendationResource#fallbackRecommendations()
```

> [!NOTE]
> The fallback method is required to have the same parameters as the original method.

## Adding Resiliency: Circuit Breaker

A circuit breaker is useful for limiting number of failures happening in the system, when part of the system becomes temporarily unstable. The circuit breaker records successful and failed invocations of a method, and when the ratio of failed invocations reaches the specified threshold, the circuit breaker *opens* and blocks all further invocations of that method for a given time.

Add the following code into the `CoffeeRepositoryService` bean, so that we can demonstrate a circuit breaker in action:

```java
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
...

public class CoffeeRepositoryService {
    ...

    private AtomicLong counter = new AtomicLong(0);

    @CircuitBreaker(requestVolumeThreshold = 4)
    public Integer getAvailability(Coffee coffee) {
        maybeFail();
        return new Random().nextInt(30);
    }

    private void maybeFail() {
        // introduce some artificial failures
        final Long invocationNumber = counter.getAndIncrement();
        if (invocationNumber % 4 > 1) { // alternate 2 successful and 2 failing invocations
            throw new RuntimeException("Service failed.");
        }
    }
}
```

And inject the code below into the `CoffeeResource` endpoint:

```java
public class CoffeeResource {
    ...
    @Path("/{id}/availability")
    @GET
    public Response availability(int id) {
        final Long invocationNumber = counter.getAndIncrement();

        Coffee coffee = coffeeRepository.getCoffeeById(id);
        // check that coffee with given id exists, return 404 if not
        if (coffee == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            Integer availability = coffeeRepository.getAvailability(coffee);
            LOGGER.infof("CoffeeResource#availability() invocation #%d returning successfully", invocationNumber);
            return Response.ok(availability).build();
        } catch (RuntimeException e) {
            String message = e.getClass().getSimpleName() + ": " + e.getMessage();
            LOGGER.errorf("CoffeeResource#availability() invocation #%d failed: %s", invocationNumber, message);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .type(MediaType.TEXT_PLAIN_TYPE)
                    .build();
        }
    }
    ...
}
```

We added another functionality - the application can return the amount of remaining packages of given coffee on our store (just a random number).

This time an artificial failure was introduced in the CDI bean: the `CoffeeRepositoryService#getAvailability()` method is going to alternate between two successful and two failed invocations.

We also added a `@CircuitBreaker` annotation with `requestVolumeThreshold = 4`. `CircuitBreaker.failureRatio` is by default 0.5, and `CircuitBreaker.delay` is by default 5 seconds. That means that a circuit breaker will open when 2 of the last 4 invocations failed, and it will stay open for 5 seconds.

To test this out, do the following:

1. Go to `http://localhost:8080/coffee/2/availability` in your browser. You should see a number being returned.
2. Hit refresh, this second request should again be successful and return a number.
3. Refresh two more times. Both times you should see text "RuntimeException: Service failed.", which is the exception thrown by `CoffeeRepositoryService#getAvailability()`.
4. Refresh a couple more times. Unless you waited too long, you should again see exception, but this time it's "CircuitBreakerOpenException: getAvailability". This exception indicates that the circuit breaker opened and the `CoffeeRepositoryService#getAvailability()` method is not being called anymore.
5. Give it 5 seconds during which circuit breaker should close, and you should be able to make two successful requests again.

## Adding Resiliency: Rate Limits

> [!IMPORTANT]
> This is an additional feature of [SmallRye Fault Tolerance](https://github.com/smallrye/smallrye-fault-tolerance/) and is not specified by MicroProfile Fault Tolerance.

It is possible to prevent an operation from being executed too often using a *rate limit*. Rate limits enforce a maximum number of permitted invocations in a time window of some length. For example, with a rate limit, one can make sure that a method may only be called 50 times per minute. Invocations that would exceed the limit are rejected with an exception of type `RateLimitException`.

Additionally, it is possible to define minimum spacing between invocations. For example, with a minimum spacing of 1 second, if a second invocation happens 500 millis after the first, it is rejected even if the limit would not be exceeded yet.

Rate limit is superficially similar to a bulkhead (concurrency limit), but is in fact quite different. Bulkhead limits the number of executions happening concurrently at any point in time. Rate limit limits the number of executions in a time window of some length, without considering concurrency.

Rate limits need to maintain some state between invocations: the number of recent invocations, the time stamp of last invocation, and so on. This state is a singleton, irrespective of the lifecycle of the bean that uses the `@RateLimit` annotation.

More specifically, the rate limit state is uniquely identified by the combination of the bean class (`java.lang.Class`) and the method object (`java.lang.reflect.Method`) representing the guarded method.

Let the Quarkus development mode run and in your IDE add the `@RateLimit` annotation to the `CoffeeResource#coffees()` method as follows and save the file:

```java
import java.time.temporal.ChronoUnit;
import io.smallrye.faulttolerance.api.RateLimit;

...
public class CoffeeResource {
    ...
    @GET
    @RateLimit(value = 2, window = 10, windowUnit = ChronoUnit.SECONDS)
    public List<Coffee> coffees() {
        ...
    }
    ...
}
```

Hit refresh in your browser. The Quarkus development server will automatically detect the changes and recompile the app for you, so there’s no need to restart it.

You can hit refresh a couple more times. After 2 requests within a 10 second interval you should start seeing errors in the logs, similar to these:

```
INFO  [org.acm.mic.fau.CoffeeResource] (executor-thread-1) CoffeeResource#coffees() invocation #1 returning successfully
ERROR [io.qua.ver.htt.run.QuarkusErrorHandler] (executor-thread-1) HTTP Request to /coffee failed, error id: d3e59090-fd45-4c67-844e-80a8f7fa6ee0-4: io.smallrye.faulttolerance.api.RateLimitException: org.acme.microprofile.faulttolerance.CoffeeResource#coffees rate limit exceeded
        at io.smallrye.faulttolerance.core.rate.limit.RateLimit.doApply(RateLimit.java:58)
        at io.smallrye.faulttolerance.core.rate.limit.RateLimit.apply(RateLimit.java:44)
        at io.smallrye.faulttolerance.FaultToleranceInterceptor.syncFlow(FaultToleranceInterceptor.java:255)
        at io.smallrye.faulttolerance.FaultToleranceInterceptor.intercept(FaultToleranceInterceptor.java:182)
        at io.smallrye.faulttolerance.FaultToleranceInterceptor_Bean.intercept(Unknown Source)
        at io.quarkus.arc.impl.InterceptorInvocation.invoke(InterceptorInvocation.java:42)
        at io.quarkus.arc.impl.AroundInvokeInvocationContext.perform(AroundInvokeInvocationContext.java:30)
        at io.quarkus.arc.impl.InvocationContexts.performAroundInvoke(InvocationContexts.java:27)
        at org.acme.microprofile.faulttolerance.CoffeeResource_Subclass.coffees(Unknown Source)
        at org.acme.microprofile.faulttolerance.CoffeeResource$quarkusrestinvoker$coffees_73d7590ab944adfa130e4ad57c30282f825b2d18.invoke(Unknown Source)
        at org.jboss.resteasy.reactive.server.handlers.InvocationHandler.handle(InvocationHandler.java:29)
        at io.quarkus.resteasy.reactive.server.runtime.QuarkusResteasyReactiveRequestContext.invokeHandler(QuarkusResteasyReactiveRequestContext.java:141)
        at org.jboss.resteasy.reactive.common.core.AbstractResteasyReactiveContext.run(AbstractResteasyReactiveContext.java:147)
        at io.quarkus.vertx.core.runtime.VertxCoreRecorder$14.runWith(VertxCoreRecorder.java:599)
        at org.jboss.threads.EnhancedQueueExecutor$Task.doRunWith(EnhancedQueueExecutor.java:2516)
        at org.jboss.threads.EnhancedQueueExecutor$Task.run(EnhancedQueueExecutor.java:2495)
        at org.jboss.threads.EnhancedQueueExecutor$ThreadBody.run(EnhancedQueueExecutor.java:1521)
        at org.jboss.threads.DelegatingRunnable.run(DelegatingRunnable.java:11)
        at org.jboss.threads.ThreadLocalResettingRunnable.run(ThreadLocalResettingRunnable.java:11)
        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        at java.base/java.lang.Thread.run(Thread.java:1583)
```

If `@Fallback` is used with `@RateLimit`, the fallback method or handler may be invoked if a `RateLimitException` is thrown, depending on the fallback configuration.

If `@Retry` is used with `@RateLimit`, each retry attempt is processed by the rate limit as an independent invocation. If `RateLimitException` is thrown, the execution may be retried, depending on how the retry is configured.

If `@CircuitBreaker` is used with `@RateLimit`, the circuit breaker is checked before enforcing the rate limit. If rate limiting results in `RateLimitException`, this may be counted as a failure, depending on how the circuit breaker is configured.

## Runtime configuration

You can override the annotations parameters at runtime inside your `application.properties` file.

If we take the retry example that we already saw:

```java
package org.acme;

import org.eclipse.microprofile.faulttolerance.Retry;
...

public class CoffeeResource {
    ...
    @GET
    @Retry(maxRetries = 4)
    public List<Coffee> coffees() {
        ...
    }
    ...
}
```

We can override the `maxRetries` parameter with 6 retries instead of 4 by the following configuration item:

```properties
quarkus.fault-tolerance."org.acme.CoffeeResource/coffees".retry.max-retries=6

# alternatively, a specification-defined property can be used
org.acme.CoffeeResource/coffees/Retry/maxRetries=6
```

The full set of Quarkus-native configuration properties is detailed in the [Configuration reference](#configuration-reference). The specification-defined configuration properties are still supported, but the Quarkus-native configuration takes priority.

## Conclusion

SmallRye Fault Tolerance allows to improve resiliency of your application, without having an impact on the complexity of our business logic.

All that is needed to enable the fault tolerance features in Quarkus is:

- adding the `smallrye-fault-tolerance` Quarkus extension to your project using the `quarkus-maven-plugin`:

  **CLI**

  ```bash
  quarkus extension add smallrye-fault-tolerance
  ```

  \+

  **Maven**

  ```bash
  ./mvnw quarkus:add-extension -Dextensions='smallrye-fault-tolerance'
  ```

  \+

  **Gradle**

  ```bash
  ./gradlew addExtension --extensions='smallrye-fault-tolerance'
  ```

- or simply adding the following Maven dependency:

  .pom.xml

  ```
  <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
  </dependency>
  ```

  .build.gradle

  ```
  implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
  ```


## Additional resources

SmallRye Fault Tolerance has more features than shown here. Please check the [SmallRye Fault Tolerance documentation](https://smallrye.io/docs/smallrye-fault-tolerance/6.11.4/index.html) to learn about them.

In Quarkus, you can use the SmallRye Fault Tolerance optional features out of the box.

Support for Mutiny is present, so your asynchronous methods can return `Uni` in addition to `CompletionStage`.

MicroProfile Context Propagation is integrated with Fault Tolerance, so existing contexts are automatically propagated to your asynchronous methods.

> [!NOTE]
> This also applies to the CDI request context: if it is active on the original thread, it is propagated to the new thread, but if it's not, then the new thread won't have it either. This is contrary to MicroProfile Fault Tolerance specification, which states that the request context must be active during the `@Asynchronous` method invocation.
>
> We believe that in presence of MicroProfile Context Propagation, this requirement should not apply. The entire point of context propagation is to make sure the new thread has the same contexts as the original thread.

Non-compatible mode is enabled by default. This means that methods that return `CompletionStage` (or `Uni`) have asynchronous fault tolerance applied without the `@Asynchronous` or `@AsynchronousNonBlocking` annotation. It also means that circuit breaker, fallback and retry automatically inspect the exception cause chain if the exception itself is insufficient to decide what should happen.

> [!NOTE]
> This mode is not compatible with the MicroProfile Fault Tolerance specification, albeit the incompatibility is very small. To restore full compatibility, add this configuration property:
>
> ```properties
> quarkus.fault-tolerance.mp-compatibility=true
> ```

The [programmatic API](https://smallrye.io/docs/smallrye-fault-tolerance/6.11.4/reference/programmatic-api.html) is present and integrated with the declarative, annotation-based API. You can use the `Guard`, `TypedGuard` and `@ApplyGuard` APIs out of the box.

Support for Kotlin is present (assuming you use the Quarkus extension for Kotlin), so you can guard your `suspend` functions with fault tolerance annotations.

Metrics are automatically discovered and integrated. If your application uses the Quarkus extension for Micrometer, SmallRye Fault Tolerance will emit metrics to Micrometer. Otherwise, SmallRye Fault Tolerance will emit metrics to OpenTelemetry Metrics if OpenTelemetry Metrics is available. If no Quarkus metrics extension is present, SmallRye Fault Tolerance metrics will be disabled.

<a id="configuration-reference"></a>
## Configuration reference

The `"<identifier>"` below is:

- `"<classname>/<methodname>"` for per method configuration
- `"<classname>"` for per class configuration
- `global` for global configuration
- It may also be an `@Identifier` value for configuring a programmatically constructed `Guard` or `TypedGuard` that is used declaratively through `@ApplyGuard`. Note that in this case, the `Guard`/`TypedGuard` should **NEVER** be used programmatically, because configuration is applied on creation, which happens lazily, on the first use. This first use must be through `@ApplyGuard`, otherwise configuration would be ignored.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-enabled"></a> [`quarkus.fault-tolerance.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-enabled) config_property_copy_button:+++quarkus.fault-tolerance.enabled+++[]<br><br>Whether fault tolerance strategies are enabled. Note that `@Fallback` is always enabled, this applies to all other strategies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-metrics-enabled"></a> [`quarkus.fault-tolerance.metrics.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-metrics-enabled) config_property_copy_button:+++quarkus.fault-tolerance.metrics.enabled+++[]<br><br>Whether fault tolerance metrics are enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE_METRICS_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-mp-compatibility"></a> [`quarkus.fault-tolerance.mp-compatibility`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-mp-compatibility) config_property_copy_button:+++quarkus.fault-tolerance.mp-compatibility+++[]<br><br>Whether SmallRye Fault Tolerance should be compatible with the MicroProfile Fault Tolerance specification.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE_MP_COMPATIBILITY+++[] | boolean | `false` |
| [lock] <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-before-retry-method-name"></a> [`quarkus.fault-tolerance."<identifier>".before-retry.method-name`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-before-retry-method-name) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".before-retry.method-name+++[]<br><br>The name of the method to call before retrying. The method belongs to the same class as the guarded method. The method must have no parameters and return `void`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___BEFORE_RETRY_METHOD_NAME+++[] | string |  |
| [lock] <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-fallback-method"></a> [`quarkus.fault-tolerance."<identifier>".fallback.fallback-method`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-fallback-method) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fallback.fallback-method+++[]<br><br>The name of the method to call on fallback. The method belongs to the same class as the guarded method. The method must have a signature matching the signature of the guarded method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FALLBACK_FALLBACK_METHOD+++[] | string |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-apply-guard-enabled"></a> [`quarkus.fault-tolerance."<identifier>".apply-guard.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-apply-guard-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".apply-guard.enabled+++[]<br><br>Whether the `@ApplyGuard` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___APPLY_GUARD_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-apply-guard-value"></a> [`quarkus.fault-tolerance."<identifier>".apply-guard.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-apply-guard-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".apply-guard.value+++[]<br><br>The `io.smallrye.common.annotation.Identifier @Identifier` of the `io.smallrye.faulttolerance.api.Guard Guard` or `io.smallrye.faulttolerance.api.TypedGuard TypedGuard` to use on the annotated method.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___APPLY_GUARD_VALUE+++[] | string |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-asynchronous-enabled"></a> [`quarkus.fault-tolerance."<identifier>".asynchronous.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-asynchronous-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".asynchronous.enabled+++[]<br><br>Whether the `@Asynchronous` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___ASYNCHRONOUS_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-asynchronous-non-blocking-enabled"></a> [`quarkus.fault-tolerance."<identifier>".asynchronous-non-blocking.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-asynchronous-non-blocking-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".asynchronous-non-blocking.enabled+++[]<br><br>Whether the `@AsynchronousNonBlocking` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___ASYNCHRONOUS_NON_BLOCKING_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-before-retry-enabled"></a> [`quarkus.fault-tolerance."<identifier>".before-retry.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-before-retry-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".before-retry.enabled+++[]<br><br>Whether the `@BeforeRetry` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___BEFORE_RETRY_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-before-retry-value"></a> [`quarkus.fault-tolerance."<identifier>".before-retry.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-before-retry-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".before-retry.value+++[]<br><br>The class of the `BeforeRetryHandler` to call before retrying.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___BEFORE_RETRY_VALUE+++[] | [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-bulkhead-enabled"></a> [`quarkus.fault-tolerance."<identifier>".bulkhead.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-bulkhead-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".bulkhead.enabled+++[]<br><br>Whether the `@Bulkhead` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___BULKHEAD_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-bulkhead-value"></a> [`quarkus.fault-tolerance."<identifier>".bulkhead.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-bulkhead-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".bulkhead.value+++[]<br><br>The maximum number of concurrent invocations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___BULKHEAD_VALUE+++[] | int | `10` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-bulkhead-waiting-task-queue"></a> [`quarkus.fault-tolerance."<identifier>".bulkhead.waiting-task-queue`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-bulkhead-waiting-task-queue) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".bulkhead.waiting-task-queue+++[]<br><br>The maximum number of queued asynchronous invocations. Asynchronous invocations are queued when the number of concurrent invocations in progress has already reached the maximum. Synchronous invocations are not queued at all and are rejected immediately.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___BULKHEAD_WAITING_TASK_QUEUE+++[] | int | `10` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-enabled"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.enabled+++[]<br><br>Whether the `@CircuitBreaker` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-delay"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.delay`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-delay) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.delay+++[]<br><br>The delay after which an open circuit breaker will move to half-open.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_DELAY+++[] | long | `5 seconds` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-delay-unit"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.delay-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-delay-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.delay-unit+++[]<br><br>The unit for `delay()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_DELAY_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-fail-on"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.fail-on`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-fail-on) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.fail-on+++[]<br><br>The exception types that are considered failures.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_FAIL_ON+++[] | list of [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `Throwable (all exceptions)` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-failure-ratio"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.failure-ratio`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-failure-ratio) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.failure-ratio+++[]<br><br>The ratio of failures within the rolling window that will move a closed circuit breaker to open.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_FAILURE_RATIO+++[] | double | `0.5` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-request-volume-threshold"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.request-volume-threshold`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-request-volume-threshold) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.request-volume-threshold+++[]<br><br>The size of the circuit breaker rolling window.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD+++[] | int | `20` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-skip-on"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.skip-on`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-skip-on) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.skip-on+++[]<br><br>The exception types that are not considered failures. Takes priority over `fail-on()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_SKIP_ON+++[] | list of [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `<empty set>` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-success-threshold"></a> [`quarkus.fault-tolerance."<identifier>".circuit-breaker.success-threshold`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-circuit-breaker-success-threshold) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".circuit-breaker.success-threshold+++[]<br><br>The number of successful executions that move a half-open circuit breaker to closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CIRCUIT_BREAKER_SUCCESS_THRESHOLD+++[] | int | `1` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-custom-backoff-enabled"></a> [`quarkus.fault-tolerance."<identifier>".custom-backoff.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-custom-backoff-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".custom-backoff.enabled+++[]<br><br>Whether the `@CustomBackoff` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CUSTOM_BACKOFF_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-custom-backoff-value"></a> [`quarkus.fault-tolerance."<identifier>".custom-backoff.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-custom-backoff-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".custom-backoff.value+++[]<br><br>The class of the `CustomBackoffStrategy` that will be used to compute retry delays.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___CUSTOM_BACKOFF_VALUE+++[] | [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-enabled"></a> [`quarkus.fault-tolerance."<identifier>".exponential-backoff.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".exponential-backoff.enabled+++[]<br><br>Whether the `@ExponentialBackoff` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___EXPONENTIAL_BACKOFF_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-factor"></a> [`quarkus.fault-tolerance."<identifier>".exponential-backoff.factor`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-factor) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".exponential-backoff.factor+++[]<br><br>The multiplicative factor used when determining a delay between two retries. A delay is computed as `factor * previousDelay`, resulting in an exponential growth.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___EXPONENTIAL_BACKOFF_FACTOR+++[] | int | `2` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-max-delay"></a> [`quarkus.fault-tolerance."<identifier>".exponential-backoff.max-delay`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-max-delay) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".exponential-backoff.max-delay+++[]<br><br>The maximum delay between retries.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___EXPONENTIAL_BACKOFF_MAX_DELAY+++[] | long | `1 minute` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-max-delay-unit"></a> [`quarkus.fault-tolerance."<identifier>".exponential-backoff.max-delay-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-exponential-backoff-max-delay-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".exponential-backoff.max-delay-unit+++[]<br><br>The unit for `max-delay()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___EXPONENTIAL_BACKOFF_MAX_DELAY_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-enabled"></a> [`quarkus.fault-tolerance."<identifier>".fallback.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fallback.enabled+++[]<br><br>Whether the `@Fallback` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FALLBACK_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-apply-on"></a> [`quarkus.fault-tolerance."<identifier>".fallback.apply-on`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-apply-on) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fallback.apply-on+++[]<br><br>The exception types that are considered failures and hence should trigger fallback.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FALLBACK_APPLY_ON+++[] | list of [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `Throwable (all exceptions)` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-skip-on"></a> [`quarkus.fault-tolerance."<identifier>".fallback.skip-on`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-skip-on) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fallback.skip-on+++[]<br><br>The exception types that are not considered failures and hence should not trigger fallback. Takes priority over `apply-on()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FALLBACK_SKIP_ON+++[] | list of [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `<empty set>` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-value"></a> [`quarkus.fault-tolerance."<identifier>".fallback.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fallback-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fallback.value+++[]<br><br>The class of the `FallbackHandler` to call on fallback.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FALLBACK_VALUE+++[] | [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fibonacci-backoff-enabled"></a> [`quarkus.fault-tolerance."<identifier>".fibonacci-backoff.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fibonacci-backoff-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fibonacci-backoff.enabled+++[]<br><br>Whether the `@FibonacciBackoff` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FIBONACCI_BACKOFF_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fibonacci-backoff-max-delay"></a> [`quarkus.fault-tolerance."<identifier>".fibonacci-backoff.max-delay`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fibonacci-backoff-max-delay) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fibonacci-backoff.max-delay+++[]<br><br>The maximum delay between retries.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FIBONACCI_BACKOFF_MAX_DELAY+++[] | long | `1 minute` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fibonacci-backoff-max-delay-unit"></a> [`quarkus.fault-tolerance."<identifier>".fibonacci-backoff.max-delay-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-fibonacci-backoff-max-delay-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".fibonacci-backoff.max-delay-unit+++[]<br><br>The unit for `max-delay()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___FIBONACCI_BACKOFF_MAX_DELAY_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-enabled"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.enabled+++[]<br><br>Whether the `@RateLimit` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-min-spacing"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.min-spacing`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-min-spacing) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.min-spacing+++[]<br><br>Minimum time between two consecutive invocations. If the time between two consecutive invocations is shorter, the second invocation is rejected.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_MIN_SPACING+++[] | long | `0` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-min-spacing-unit"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.min-spacing-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-min-spacing-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.min-spacing-unit+++[]<br><br>The unit for `min-spacing()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_MIN_SPACING_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-type"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.type`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-type) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.type+++[]<br><br>The type of type windows used for rate limiting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_TYPE+++[] | `fixed`, `rolling`, `smooth` | `fixed` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-value"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.value+++[]<br><br>The maximum number of invocations in a time window.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_VALUE+++[] | int | `100` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-window"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.window`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-window) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.window+++[]<br><br>The time window length.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_WINDOW+++[] | long | `1 second` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-window-unit"></a> [`quarkus.fault-tolerance."<identifier>".rate-limit.window-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-rate-limit-window-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".rate-limit.window-unit+++[]<br><br>The unit for `window()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RATE_LIMIT_WINDOW_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-enabled"></a> [`quarkus.fault-tolerance."<identifier>".retry.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.enabled+++[]<br><br>Whether the `@Retry` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-abort-on"></a> [`quarkus.fault-tolerance."<identifier>".retry.abort-on`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-abort-on) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.abort-on+++[]<br><br>The exception types that are not considered failures and hence should not be retried. Takes priority over `retry-on()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_ABORT_ON+++[] | list of [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `<empty set>` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-delay"></a> [`quarkus.fault-tolerance."<identifier>".retry.delay`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-delay) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.delay+++[]<br><br>The delay between retry attempts.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_DELAY+++[] | long | `0` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-delay-unit"></a> [`quarkus.fault-tolerance."<identifier>".retry.delay-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-delay-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.delay-unit+++[]<br><br>The unit for `delay()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_DELAY_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-jitter"></a> [`quarkus.fault-tolerance."<identifier>".retry.jitter`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-jitter) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.jitter+++[]<br><br>The maximum jitter to apply for the delay between retry attempts. The actual delay will be in the interval `[delay - jitter, delay {plus} jitter]`, but will not be negative.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_JITTER+++[] | long | `200 millis` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-jitter-unit"></a> [`quarkus.fault-tolerance."<identifier>".retry.jitter-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-jitter-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.jitter-unit+++[]<br><br>The unit for `jitter()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_JITTER_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-max-duration"></a> [`quarkus.fault-tolerance."<identifier>".retry.max-duration`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-max-duration) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.max-duration+++[]<br><br>The maximum duration for which to retry.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_MAX_DURATION+++[] | long | `3 minutes` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-max-duration-unit"></a> [`quarkus.fault-tolerance."<identifier>".retry.max-duration-unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-max-duration-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.max-duration-unit+++[]<br><br>The unit for `max-duration()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_MAX_DURATION_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-max-retries"></a> [`quarkus.fault-tolerance."<identifier>".retry.max-retries`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-max-retries) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.max-retries+++[]<br><br>The maximum number of retry attempts.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_MAX_RETRIES+++[] | int | `3` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-retry-on"></a> [`quarkus.fault-tolerance."<identifier>".retry.retry-on`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-retry-on) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry.retry-on+++[]<br><br>The exception types that are considered failures and hence should be retried.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_RETRY_ON+++[] | list of [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `Exception (all exceptions)` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-when-enabled"></a> [`quarkus.fault-tolerance."<identifier>".retry-when.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-when-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry-when.enabled+++[]<br><br>Whether the `@RetryWhen` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_WHEN_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-when-exception"></a> [`quarkus.fault-tolerance."<identifier>".retry-when.exception`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-when-exception) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry-when.exception+++[]<br><br>Class of the predicate that will be used to determine whether the invocation should be retried if the guarded method has thrown an exception.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_WHEN_EXCEPTION+++[] | [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `AlwaysOnException` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-when-result"></a> [`quarkus.fault-tolerance."<identifier>".retry-when.result`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-retry-when-result) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".retry-when.result+++[]<br><br>Class of the predicate that will be used to determine whether the invocation should be retried if the guarded method has returned a result.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___RETRY_WHEN_RESULT+++[] | [Class](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Class.html) | `NeverOnResult` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-timeout-enabled"></a> [`quarkus.fault-tolerance."<identifier>".timeout.enabled`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-timeout-enabled) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".timeout.enabled+++[]<br><br>Whether the `@Timeout` strategy is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___TIMEOUT_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-timeout-unit"></a> [`quarkus.fault-tolerance."<identifier>".timeout.unit`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-timeout-unit) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".timeout.unit+++[]<br><br>The unit for `value()`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___TIMEOUT_UNIT+++[] | `nanos`, `micros`, `millis`, `seconds`, `minutes`, `hours`, `half-days`, `days`, `weeks`, `months`, `years`, `decades`, `centuries`, `millennia`, `eras`, `forever` |  |
| <a id="quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-timeout-value"></a> [`quarkus.fault-tolerance."<identifier>".timeout.value`](#quarkus-smallrye-fault-tolerance_quarkus-fault-tolerance-identifier-timeout-value) config_property_copy_button:+++quarkus.fault-tolerance."<identifier>".timeout.value+++[]<br><br>The timeout to enforce.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_FAULT_TOLERANCE___IDENTIFIER___TIMEOUT_VALUE+++[] | long | `1 second` |

This is how the specification-defined properties are mapped to the Quarkus-native configuration:

| Specification-defined config property | Quarkus-native config property |
| --- | --- |
| `<classname>/<methodname>/<annotation>/<member>` | `quarkus.fault-tolerance."<classname>/<methodname>".<annotation>.<member>` |
| `<classname>/<annotation>/<member>` | `quarkus.fault-tolerance."<classname>".<annotation>.<member>` |
| `<annotation>/<member>` | `quarkus.fault-tolerance.global.<annotation>.<member>` |
| `MP_Fault_Tolerance_NonFallback_Enabled` | `quarkus.fault-tolerance.enabled` |
| `MP_Fault_Tolerance_Metrics_Enabled` | `quarkus.fault-tolerance.metrics.enabled` |

All the `<annotation>` and `<member>` parts are changed from camel case (`BeforeRetry`, `methodName`) to kebab case (`before-retry`, `method-name`). Two annotation members are special cased to improve consistency:

- `Retry/durationUnit` moves to `retry.max-duration-unit`
- `Retry/jitterDelayUnit` moves to `retry.jitter-unit`
