# Quarkus 1.3 and Eclipse MicroProfile 3.3

With the announcement that Quarkus 1.3 is now available, we’re pleased to additionally announce that Quarkus 1.3 passes the TCKs of all [Eclipse MicroProfile 3.3](https://projects.eclipse.org/projects/technology.microprofile/releases/microprofile-3.3) specifications.

[MicroProfile 3.3](https://projects.eclipse.org/projects/technology.microprofile/releases/microprofile-3.3) includes the following specification updates:

- Config 1.4
- Fault Tolerance 2.1
- Health 2.2
- Metrics 2.3
- REST Client 1.4

## [Config](https://quarkus.io/guides/config)

The Config update includes fixes to prevent the incorrect caching of `ConfigProviderResolver` and classloader issues related to it as well.

It also adjusted the priority of implicit converters and added converters for `byte`, `short`, and `char`. The priority change means that implicit conversion will utilize any static methods before attempting to use a constructor.

## [Fault Tolerance](https://quarkus.io/guides/microprofile-fault-tolerance)

`CircuitBreaker` and `Fallback` both added the `skipOn` property and `Fallback` added the `applyOn` property so that it’s now possible to define exceptions that should trigger the behavior or pass-through to the caller. Why is this important?

Real-world use cases were identified where triggering a fault tolerance strategy for certain exceptions is required, and also being able to treat other exceptions as *normal* to prevent a strategy from being applied.

## [Health](https://quarkus.io/guides/microprofile-health)

It contains a few minor updates that include adding Annotation literals to all qualifiers to ease programmatic lookup and switching `HealthCheckResponse` to be a concrete class, enabling its use on the consuming end.

## [Metrics](https://quarkus.io/guides/microprofile-metrics)

Added `@SimplyTimed` which doesn’t perform mean and distribution calculations. It only calculates hit count and time spent on a method. `@SimplyTimed` is a “simple” version of `@Timed` that reduces calculation time, as these calculations can be performed by Prometheus with the raw data from `@SimplyTimed`.

The specification now also defines optional metrics specific to JAX-RS invocations. For each JAX-RS method, there is a Simple Timer, so we measure the total count of requests and the total time spent in that method.

The specification also adds one more optional base metric, ProcessCpuTime, which tracks the CPU time occupied by the JVM. It is optional because its value is not available from a JDK interface, only an internal Sun one.

## [REST Client](https://quarkus.io/guides/rest-client)

Now defines that supporting CDI injection into `ClientHeadersFactory` is a requirement of the specification. This resolves a gap where previously it was not possible to `@Inject` into the class.

The `@RestClient` qualifier for injection points now has `@Target` specified, enabling its usage with Kotlin. There are many Quarkus users using Kotlin that were bitten by this problem in previous releases.

## Standalone

In addition to the specifications within the MicroProfile platform, [Quarkus](https://quarkus.io/) also includes implementations of Reactive Streams Operators, [Reactive Messaging](https://quarkus.io/guides/kafka), and [Context Propagation](https://quarkus.io/guides/context-propagation). Whether it’s Imperative or Reactive we’ve got what you need in [Quarkus](https://quarkus.io/)!

In the near future, [Quarkus](https://quarkus.io/) will also begin including implementations for the new standalone specifications for Long-Running Actions and GraphQL.

## Summary

The release of Quarkus 1.3 continues our commitment to implementing the [MicroProfile](https://microprofile.io/) specifications in [SmallRye](https://smallrye.io/) and utilizing them in [Quarkus](https://quarkus.io/). Please provide feedback on the [MicroProfile](https://microprofile.io/) features to ensure we’re providing what developers need and want. If we’re missing key functionality, please let us know!
