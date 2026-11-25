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

## Use the load shedding extension

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

> [!WARNING]
> Pure load shedding rejects *all* requests while the application is overloaded, including requests to the non-application endpoints. The only exceptions are the health probes (liveness, readiness and startup) and the metrics endpoint, which are never shed: an orchestrator that probes them would otherwise restart an application that is merely overloaded, or cut it off and push its load onto the remaining replicas. Request priority, described below, is the only mechanism that can exempt other requests. If you disable priority load shedding, consider also exposing the management endpoints on the [separate management interface](/guides/management-interface-reference/index.md), which is not subject to load shedding.

There are 2 ways of customizing priority load shedding:

- By implementing `io.quarkus.load.shedding.RequestPrioritizer`, the application can decide which requests can be rejected early on and which requests can only be rejected when there's no other choice.
- By implementing `io.quarkus.load.shedding.RequestClassifier`, the application can classify requests into cohorts which are rejected independently.

Other configuration options are described below, although they should typically be left untouched.

## The load shedding algorithm

The load shedding algorithm has 2 parts:

- overload detection
- prioritization (optional)

### Overload detection

To detect whether the current service is overloaded, an adaptation of the Vegas algorithm (known from TCP) is used.

This algorithm starts with a configurable limit of in-flight requests, by default 100. If the current number of concurrent in-flight requests reaches the limit, overload situation is signalled.

That is not very interesting, but the algorithm dynamically adjusts the limit based on the size of the request queue. If the queue is short, more requests can be handled and the limit increases; if the queue is long, fewer requests can be handled and the limit decreases. The limit cannot increase or decrease indefinitely; there's a configurable minimum, by default 20, and configurable maximum, by default 1000.

There's no actual queue of requests that we could monitor, though, so the algorithm estimates the current length of a request queue based on previously seen response times. The longer recent requests take, compared to the recent lowest response time, the longer the queue is supposed to be.

> [!CAUTION]
> We know the Vegas algorithm is not the best choice in environments where response times vary. We are working on replacing it with another algorithm, or maybe supporting a small set of algorithms you can select from.

### Prioritization

If an overload situation is signalled, prioritization is invoked.

If prioritization is disabled, all requests beyond the current limit are rejected immediately. However, by default, prioritization is enabled, which means a request is only rejected if the overload is deep enough.

The overload depth is expressed as a number between 0.0 and 1.0. It is 0.0 when the number of current requests is at the current limit. When the number of current requests exceeds the current limit, the result scales smoothly using the right half of a sigmoid curve (hyperbolic tangent). This provides maximum initial steepness to aggressively shed lower-priority requests immediately upon crossing the limit, while smoothly flattening out near 1.0 to provide a broad runway for higher-priority traffic. When the limit is below the maximum limit, the overload depth reaches 1.0 when the number of current requests reaches the maximum limit. When the limit is at the maximum limit, the overload depth reaches 1.0 when the number of current requests reaches 130% of the maximum limit, leaving a 30% safety buffer for the most important requests.

The more requests are admitted past the limit, the deeper the overload and the more requests are rejected, until the application settles at a level of concurrency it can sustain.

To determine whether a request should be rejected, 2 attributes are considered:

- request priority
- request cohort

There are 5 statically defined priorities and 128 cohorts, which amounts to 640 request groups in total.

After both priority and cohort are assigned to a request, a request group number is computed. The group number is smaller for higher priority requests and higher for lower priority requests. If the group number is lower than a threshold derived from the overload depth, the request is accepted, otherwise it is rejected.

The threshold is derived as `(1 - overload^3) * 640`, where `overload` is the overload depth (described above) and 640 is the number of request groups (also described above). The cubic curve is chosen deliberately, because it is flat near 0, so that a slight excess over the concurrency limit only rejects the least important requests.

#### Customizing request priority

Priority is assigned by a `io.quarkus.load.shedding.RequestPrioritizer`. There is 5 statically defined priorities in the `io.quarkus.load.shedding.RequestPriority` enum: `CRITICAL`, `IMPORTANT`, `NORMAL`, `BACKGROUND` and `DEGRADED`. By default, if no request prioritizer applies, the priority is assumed to be `NORMAL`. Further, there's a special priority level called `NEVER`, which is exempt from load shedding entirely.

There is one default prioritizer which assigns the priority of `NEVER` to requests to the non-application endpoints. It declares no `@Priority`.

Independently of priority load shedding, the health probes (liveness, readiness and startup) and the metrics endpoint are never shed, even if priority load shedding is disabled. Extensions declare such paths with the `io.quarkus.load.shedding.deployment.spi.LoadSheddingExemptPathBuildItem` build item from the `quarkus-load-shedding-deployment-spi` artifact.

> [!IMPORTANT]
> The `NEVER` priority must be used sparingly. A request should only be assigned `NEVER` if it is part of the operational control plane (health checks, in-memory metrics scraping, etc.), where failures would trigger cascading failures, AND if it satisfies strict execution bounds. The request should execute in sub-millisecond time, only read pre-aggregated values from memory, perform no disk or network I/O, and require no thread offload.

It is possible to define custom implementations of the `RequestPrioritizer` interface. The implementations must be CDI beans, otherwise they are ignored. The CDI rules of typesafe resolution must be followed. That is, if multiple implementations exist with a different `@Priority` value and some of them are ``@Alternative``s, only the alternatives with the highest priority value are retained. If no implementation is an alternative, all implementations are retained and are sorted in descending `@Priority` order (highest priority value comes first).

#### Customizing request cohort

Cohort is assigned by a `io.quarkus.load.shedding.RequestClassifier`. There are 128 statically defined cohorts, with the lowest number being 0 and the highest number being 127. The classifier should return a number in this interval; if it doesn't, the result is clamped by a simple bit mask. This puts requirements on the returned value that are described in the `RequestClassifier` javadoc.

There is one default classifier which assigns a cohort based on a hash of the remote IP address and current time, such that an IP address changes its cohort roughly every hour. It declares no `@Priority`.

> [!IMPORTANT]
> If the application is deployed behind a reverse proxy, an ingress or a load balancer, the remote IP address is the address of that intermediary and is therefore identical for all clients. All requests then land in a single cohort, the cohort dimension collapses and shedding degenerates to all-or-nothing within each priority level. To avoid this, either set `quarkus.http.proxy.proxy-address-forwarding` to `true`, in which case the remote address is taken from the `Forwarded` or `X-Forwarded-For` header and the default classifier works as intended, or implement a custom classifier. A custom classifier can also use a better key than the IP address if the application has one, such as a tenant or account identifier.

It is possible to define custom implementations of the `RequestClassifier` interface. The implementations must be CDI beans, otherwise they are ignored. The CDI rules of typesafe resolution must be followed. That is, if multiple implementations exist with a different `@Priority` value and some of them are ``@Alternative``s, only the alternatives with the highest priority value are retained. If no implementation is an alternative, all implementations are retained and are sorted in descending `@Priority` order (highest priority value comes first).

## Metrics

If the [Micrometer extension](/guides/telemetry-micrometer/index.md) is present, this extension publishes the following metrics:

| Name | Type | Description |
| --- | --- | --- |
| `load.shedding.shed.requests` | counter | Number of requests rejected by load shedding. |
| `load.shedding.current.requests` | gauge | Number of requests currently in flight, as counted by the overload detector. |
| `load.shedding.current.limit` | gauge | The request limit the overload detector has currently adapted to; reaching it signals an overload. |
| `load.shedding.overload.status` | gauge | Whether the application is currently overloaded: `1` if it is, `0` if it is not. |
| `load.shedding.overload.depth` | gauge | How deep the overload currently is, as a number between 0.0 and 1.0. |
| `load.shedding.overload.duration` | gauge | How long the current overload has lasted, or `0` if the application is not overloaded. |

The overload status is hysteretic, so it cannot be derived from the number of current requests and the current limit; that is why it is published on its own.

Publishing the meters can be turned off by setting `quarkus.load-shedding.metrics.enabled` to `false`. No meters are published if load shedding itself is disabled, because nothing would ever update them.

## Limitations

The load shedding extension currently only applies to HTTP requests, and is heavily skewed towards request/response network interactions. This means that gRPC, WebSocket and other kinds of streaming over HTTP are not supported. Other "entrypoints" to Quarkus applications, such as messaging, are not supported either.

Further, the load shedding implementation is currently rather basic and not heavily tested in production. Improvements may be necessary.

## How rejected requests look

An HTTP request is rejected with the `503 Service Unavailable` status code and a `Retry-After` header.

The connection is deliberately left open. Closing it would force the client through a new TCP and TLS handshake, which is expensive and invisible to this load shedding mechanism.

The `Retry-After` value is derived from how long the overload has already lasted. The application cannot know when an overload will end, but it does know how long the overload has lasted. This is used as a proxy: a brief spike is likely over shortly, while an overload that has been going on for minutes is likely to keep going for a while. A client rejected 2 seconds into an overload is therefore told to come back in a few seconds, while a client rejected 5 minutes in is told to stay away for longer.

The value is capped by `quarkus.load-shedding.max-retry-after` (120 seconds by default) and is never shorter than 1 second, which is the granularity of the header. It is also jittered, so that clients rejected in the same instant are not advised to come back in the same instant.

## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-load-shedding_quarkus-load-shedding-metrics-enabled"></a> [`quarkus.load-shedding.metrics.enabled`](#quarkus-load-shedding_quarkus-load-shedding-metrics-enabled) config_property_copy_button:+++quarkus.load-shedding.metrics.enabled+++[]<br><br>Whether metrics about load shedding should be published.<br><br>Only takes effect if a metrics extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_METRICS_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-enabled"></a> [`quarkus.load-shedding.enabled`](#quarkus-load-shedding_quarkus-load-shedding-enabled) config_property_copy_button:+++quarkus.load-shedding.enabled+++[]<br><br>Whether load shedding should be enabled. Currently, this only applies to incoming HTTP requests.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-min-limit"></a> [`quarkus.load-shedding.min-limit`](#quarkus-load-shedding_quarkus-load-shedding-min-limit) config_property_copy_button:+++quarkus.load-shedding.min-limit+++[]<br><br>The minimum number of concurrent requests allowed.<br><br>Must be at least 1 and less than or equal to `max-limit`; the application fails to start otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_MIN_LIMIT+++[] | int | `20` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-max-limit"></a> [`quarkus.load-shedding.max-limit`](#quarkus-load-shedding_quarkus-load-shedding-max-limit) config_property_copy_button:+++quarkus.load-shedding.max-limit+++[]<br><br>The maximum number of concurrent requests allowed.<br><br>Must be greater than or equal to `min-limit`; the application fails to start otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_MAX_LIMIT+++[] | int | `1000` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-alpha-factor"></a> [`quarkus.load-shedding.alpha-factor`](#quarkus-load-shedding_quarkus-load-shedding-alpha-factor) config_property_copy_button:+++quarkus.load-shedding.alpha-factor+++[]<br><br>The `alpha` factor of the Vegas overload detection algorithm. The algorithm grows the limit while the estimated request queue is shorter than this factor times `log10(current limit) {plus} 1`.<br><br>Must be at least 1; the application fails to start otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_ALPHA_FACTOR+++[] | int | `3` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-beta-factor"></a> [`quarkus.load-shedding.beta-factor`](#quarkus-load-shedding_quarkus-load-shedding-beta-factor) config_property_copy_button:+++quarkus.load-shedding.beta-factor+++[]<br><br>The `beta` factor of the Vegas overload detection algorithm. The algorithm shrinks the limit once the estimated request queue is longer than this factor times `log10(current limit) {plus} 1`, and holds the limit steady in between.<br><br>Must be greater than the `alpha` factor; the application fails to start otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_BETA_FACTOR+++[] | int | `6` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-probe-factor"></a> [`quarkus.load-shedding.probe-factor`](#quarkus-load-shedding_quarkus-load-shedding-probe-factor) config_property_copy_button:+++quarkus.load-shedding.probe-factor+++[]<br><br>The probe factor of the Vegas overload detection algorithm. It controls how often the algorithm discards the lowest request time it has seen and measures against a fresh one; the higher the factor, the rarer that is.<br><br>Must be positive; the application fails to start otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_PROBE_FACTOR+++[] | double | `30.0` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-initial-limit"></a> [`quarkus.load-shedding.initial-limit`](#quarkus-load-shedding_quarkus-load-shedding-initial-limit) config_property_copy_button:+++quarkus.load-shedding.initial-limit+++[]<br><br>The initial limit of concurrent requests allowed.<br><br>Must be greater than or equal to `min-limit` and less than or equal to `max-limit`; the application fails to start otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_INITIAL_LIMIT+++[] | int | `100` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-max-retry-after"></a> [`quarkus.load-shedding.max-retry-after`](#quarkus-load-shedding_quarkus-load-shedding-max-retry-after) config_property_copy_button:+++quarkus.load-shedding.max-retry-after+++[]<br><br>The upper bound on the `Retry-After` response header sent with a rejected request.<br><br>The value actually sent is derived from how long the overload has already lasted, so no lower bound is configurable. This is only a safety measure against telling clients to stay away hours, if the overload has been going on for long.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_MAX_RETRY_AFTER+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-load-shedding_quarkus-load-shedding) | `120S` |
| <a id="quarkus-load-shedding_quarkus-load-shedding-priority-enabled"></a> [`quarkus.load-shedding.priority.enabled`](#quarkus-load-shedding_quarkus-load-shedding-priority-enabled) config_property_copy_button:+++quarkus.load-shedding.priority.enabled+++[]<br><br>Whether priority load shedding should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_LOAD_SHEDDING_PRIORITY_ENABLED+++[] | boolean | `true` |

<a id="duration-note-anchor-quarkus-load-shedding_quarkus-load-shedding"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.

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
