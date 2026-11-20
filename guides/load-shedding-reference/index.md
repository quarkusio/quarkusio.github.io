# Load shedding reference guide

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

Load shedding is the practice of detecting service overload and rejecting requests. By rejecting requests when overloaded, load shedding keeps the application alive. With priority load shedding, the application even keeps working, albeit in a degraded state: only a fraction of requests is handled, others are rejected early. You should consider using it if your application runs in a dynamic environment with a real risk of getting overloaded and is not fronted by another service that sheds load already.

In Quarkus, the `quarkus-load-shedding` extension provides a load shedding mechanism.

## Use the Load Shedding extension

To use the load shedding extension, you need to add the `io.quarkus:quarkus-load-shedding` extension to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-load-shedding</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-load-shedding")
```

When added, load shedding is enabled. It can be disabled by setting `quarkus.load-shedding.enabled` to `false`.

There are 2 variants of load shedding: pure and priority-based. Pure load shedding rejects all requests when overload is detected. Priority load shedding only rejects a fraction of requests even if overload is detected, based on application-defined criteria. By default, priority load shedding is enabled. It can be disabled by setting `quarkus.load-shedding.priority.enabled` to `false.`

There are 2 ways of customizing priority load shedding:

- By implementing `io.quarkus.load.shedding.RequestPrioritizer`, the application can decide which requests can be rejected early on and which requests can only be rejected when there's no other choice.
- By implementing `io.quarkus.load.shedding.RequestClassifier`, the application can classify requests into cohorts which are rejected independently.

Other configuration options are described below, although they should typically be left untouched.

## The load shedding algorithm

The load shedding algorithm has 2 parts:

- overload detection
- priority load shedding (optional)

### Overload detection

To detect whether the current service is overloaded, an adaptation of TCP Vegas is used.

This algorithm starts with a configurable limit of in-flight requests, by default 100. If the current number of concurrent in-flight requests reaches the limit, overload situation is signalled.

That is not very interesting, but the algorithm dynamically adjusts the limit based on the size of the request queue. If the queue is short, more requests can be handled and the limit increases; if the queue is long, fewer requests can be handled and the limit decreases. The limit cannot be increased indefinitely; there's a configurable maximum, by default 1000.

There's no actual queue of requests that we could monitor, though, so the algorithm estimates the current length of a request queue based on previously seen response times. The longer recent requests take, compared to the recent lowest response time, the longer the queue is supposed to be.

### Priority load shedding

If an overload situation is signalled, priority load shedding is invoked.

If priority load shedding is disabled, there's nothing to do and all requests are rejected immediately. However, by default, priority load shedding is enabled, which means a request is only rejected if the current CPU load is high enough.

> [!NOTE]
> Priority load shedding is currently always based on CPU load. Other mechanisms are possible (such as network utilization), but currently not implemented.

To determine whether a request should be rejected, 2 attributes are considered:

- request priority
- request cohort

There are 5 statically defined priorities and 128 cohorts, which amounts to 640 request groups in total.

After both priority and cohort are assigned to a request, a request group number is computed. The group number is smaller for higher priority requests and higher for lower priority requests. If the group number is higher that the current CPU load, the request is rejected, otherwise it is allowed even in an overload situation.

> [!NOTE]
> The group number is actually not compared to the CPU load directly; instead, it is compared to a function of CPU load. The function is `(1 - load^3) * 640`, where `load` is the CPU load as a number between 0.0 and 1.0, and 640 is a number of requests group as mentioned above.

#### Customizing request priority

Priority is assigned by a `io.quarkus.load.shedding.RequestPrioritizer`. There is 5 statically defined priorities in the `io.quarkus.load.shedding.RequestPriority` enum: `CRITICAL`, `IMPORTANT`, `NORMAL`, `BACKGROUND` and `DEGRADED`. By default, if no request prioritizer applies, the priority is assumed to be `NORMAL`.

There is one default prioritizer which assigns the priority of `CRITICAL` to requests to the non-application endpoints. It declares no `@Priority`.

It is possible to define custom implementations of the `RequestPrioritizer` interface. The implementations must be CDI beans, otherwise they are ignored. The CDI rules of typesafe resolution must be followed. That is, if multiple implementations exist with a different `@Priority` value and some of them are ``@Alternative``s, only the alternatives with the highest priority value are retained. If no implementation is an alternative, all implementations are retained and are sorted in descending `@Priority` order (highest priority value comes first).

#### Customizing request cohort

Cohort is assigned by a `io.quarkus.load.shedding.RequestClassifier`. There is 128 statically defined cohorts, with the lowest number being 1 and highest number being 128. The classifier should return a number in this interval; if it does not, the number is adjusted automatically.

There is one default classifier which assigns a cohort based on a hash of the remote IP address and current time, such that an IP address changes its cohort roughly every hour. It declares no `@Priority`.

It is possible to define custom implementations of the `RequestClassifier` interface. The implementations must be CDI beans, otherwise they are ignored. The CDI rules of typesafe resolution must be followed. That is, if multiple implementations exist with a different `@Priority` value and some of them are ``@Alternative``s, only the alternatives with the highest priority value are retained. If no implementation is an alternative, all implementations are retained and are sorted in descending `@Priority` order (highest priority value comes first).

## Limitations

The load shedding extension currently only applies to HTTP requests, and is heavily skewed towards request/response network interactions. This means that gRPC, WebSocket and other kinds of streaming over HTTP are not supported. Other "entrypoints" to Quarkus applications, such as messaging, are not supported either.

Further, the load shedding implementation is currently rather basic and not heavily tested in production. Improvements may be necessary.

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-load-shedding_quarkus-load-shedding-enabled"></a> [`quarkus.load-shedding.enabled`](#quarkus-load-shedding_quarkus-load-shedding-enabled) config_property_copy_button:+++quarkus.load-shedding.enabled+++[]<br><br>Whether load shedding should be enabled. Currently, this only applies to incoming HTTP requests.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-max-limit"></a> [`quarkus.load-shedding.max-limit`](#quarkus-load-shedding_quarkus-load-shedding-max-limit) config_property_copy_button:+++quarkus.load-shedding.max-limit+++[]<br><br>The maximum number of concurrent requests allowed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_MAX_LIMIT+++[] | int | `1000` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-alpha-factor"></a> [`quarkus.load-shedding.alpha-factor`](#quarkus-load-shedding_quarkus-load-shedding-alpha-factor) config_property_copy_button:+++quarkus.load-shedding.alpha-factor+++[]<br><br>The `alpha` factor of the Vegas overload detection algorithm.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_ALPHA_FACTOR+++[] | int | `3` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-beta-factor"></a> [`quarkus.load-shedding.beta-factor`](#quarkus-load-shedding_quarkus-load-shedding-beta-factor) config_property_copy_button:+++quarkus.load-shedding.beta-factor+++[]<br><br>The `beta` factor of the Vegas overload detection algorithm.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_BETA_FACTOR+++[] | int | `6` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-probe-factor"></a> [`quarkus.load-shedding.probe-factor`](#quarkus-load-shedding_quarkus-load-shedding-probe-factor) config_property_copy_button:+++quarkus.load-shedding.probe-factor+++[]<br><br>The probe factor of the Vegas overload detection algorithm.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_PROBE_FACTOR+++[] | double | `30.0` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-initial-limit"></a> [`quarkus.load-shedding.initial-limit`](#quarkus-load-shedding_quarkus-load-shedding-initial-limit) config_property_copy_button:+++quarkus.load-shedding.initial-limit+++[]<br><br>The initial limit of concurrent requests allowed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_INITIAL_LIMIT+++[] | int | `100` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-priority-enabled"></a> [`quarkus.load-shedding.priority.enabled`](#quarkus-load-shedding_quarkus-load-shedding-priority-enabled) config_property_copy_button:+++quarkus.load-shedding.priority.enabled+++[]<br><br>Whether priority load shedding should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_PRIORITY_ENABLED+++[] | boolean | `true` |

## Further reading

Netflix Technology Blog:

- [Performance Under Load](https://netflixtechblog.medium.com/performance-under-load-3e6fa9a60581)
- [Keeping Netflix Reliable Using Prioritized Load Shedding](https://netflixtechblog.com/keeping-netflix-reliable-using-prioritized-load-shedding-6cc827b02f94)
- [Enhancing Netflix Reliability with Service-Level Prioritized Load Shedding](https://netflixtechblog.com/enhancing-netflix-reliability-with-service-level-prioritized-load-shedding-e735e6ce8f7d)

Uber Engineering Blog:

- [Cinnamon: Using Century Old Tech to Build a Mean Load Shedder](https://www.uber.com/blog/cinnamon-using-century-old-tech-to-build-a-mean-load-shedder/)
- [PID Controller for Cinnamon](https://www.uber.com/blog/pid-controller-for-cinnamon/)
- [Cinnamon Auto-Tuner: Adaptive Concurrency in the Wild](https://www.uber.com/blog/cinnamon-auto-tuner-adaptive-concurrency-in-the-wild/)

Amazon Builders' Library:

- [Using load shedding to avoid overload](https://aws.amazon.com/builders-library/using-load-shedding-to-avoid-overload/)

Google Cloud Blog:

- [Using load shedding to survive a success disaster](https://cloud.google.com/blog/products/gcp/using-load-shedding-to-survive-a-success-disaster-cre-life-lessons)

CodeReliant Blog:

- [Load Shedding for High Traffic Systems](https://www.codereliant.io/load-shedding/)

TCP Vegas:

- [Basic concept of TCP-Vegas](https://www.geeksforgeeks.org/computer-networks/basic-concept-of-tcp-vegas/)
- TCP Congestion Control: A Systems Approach, [Chapter 5: Avoidance-Based Algorithms](https://tcpcc.systemsapproach.org/avoidance.html)
