# Stork reference guide

This guide is the companion to the [Stork Getting Started Guide](/guides/stork/index.md). It explains the configuration and usage of SmallRye Stork integration in Quarkus.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Supported clients

The current integration of Stork supports:

- the REST Client
- the gRPC clients (using the Vert.x gRPC client is recommended)

Warning: The gRPC client integration does not support statistic-based load balancers.

## Available service discovery and selection

Check the [SmallRye Stork website](https://smallrye.io/smallrye-stork) to find more about the provided service discovery and selection.

## Using Stork in Kubernetes

Stork provides a service discovery support for Kubernetes, which goes beyond what Kubernetes provides by default. It looks for all the pods backing up a Kubernetes service, but instead of applying a round-robin (as Kubernetes would do), it gives you the option to select the pod using a Stork load-balancer.

To use this feature, add the following dependency to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-service-discovery-kubernetes</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.smallrye.stork:stork-service-discovery-kubernetes")
```

For each service expected to be exposed as a Kubernetes Service, configure the lookup:

```properties
quarkus.stork.my-service.service-discovery.type=kubernetes
quarkus.stork.my-service.service-discovery.k8s-namespace=my-namespace
```

Stork looks for the Kubernetes Service with the given name (`my-service` in the previous example) in the specified namespace. Instead of using the Kubernetes Service IP directly and let Kubernetes handle the selection and balancing, Stork inspects the service and retrieves the list of pods providing the service. Then, it can select the instance.

For a full example of using Stork with Kubernetes, please read the [Using Stork with Kubernetes guide](/guides/stork-kubernetes/index.md).

## Extending Stork

Stork is extensible. You can implement your own service discovery or service selection provider.

To learn about custom service discovery and service selection, check:

- [Implement a custom service discover provider](https://smallrye.io/smallrye-stork/latest/service-discovery/custom-service-discovery/)
- [Implement a custom service selection provider](https://smallrye.io/smallrye-stork/latest/load-balancer/custom-load-balancer/)

## Configure Stork observability

### Enable metrics

Stork metrics are automatically enabled when the application also uses the [`quarkus-micrometer`](/guides/telemetry-micrometer/index.md) extension.

Micrometer collects the metrics of rest/grpc clients using Stork and the client using Stork programmatically.

As an example, if you export the metrics to Prometheus, you will get:

```text
# HELP stork_service_selection_failures_total The number of failures during service selection.
# TYPE stork_service_selection_failures_total counter
stork_service_selection_failures_total{service_name="hello-service",} 0.0
# HELP stork_service_selection_duration_seconds The duration of the selection operation
# TYPE stork_service_selection_duration_seconds summary
stork_service_selection_duration_seconds_count{service_name="hello-service",} 13.0
stork_service_selection_duration_seconds_sum{service_name="hello-service",} 0.001049291
# HELP stork_service_selection_duration_seconds_max The duration of the selection operation
# TYPE stork_service_selection_duration_seconds_max gauge
stork_service_selection_duration_seconds_max{service_name="hello-service",} 0.0
# HELP stork_overall_duration_seconds_max The total duration of the Stork service discovery and selection operations
# TYPE stork_overall_duration_seconds_max gauge
stork_overall_duration_seconds_max{service_name="hello-service",} 0.0
# HELP stork_overall_duration_seconds The total duration of the Stork service discovery and selection operations
# TYPE stork_overall_duration_seconds summary
stork_overall_duration_seconds_count{service_name="hello-service",} 13.0
stork_overall_duration_seconds_sum{service_name="hello-service",} 0.001049291
# HELP stork_service_discovery_failures_total The number of failures during service discovery
# TYPE stork_service_discovery_failures_total counter
stork_service_discovery_failures_total{service_name="hello-service",} 0.0
# HELP stork_service_discovery_duration_seconds_max The duration of the discovery operation
# TYPE stork_service_discovery_duration_seconds_max gauge
stork_service_discovery_duration_seconds_max{service_name="hello-service",} 0.0
# HELP stork_service_discovery_duration_seconds The duration of the discovery operation
# TYPE stork_service_discovery_duration_seconds summary
stork_service_discovery_duration_seconds_count{service_name="hello-service",} 13.0
stork_service_discovery_duration_seconds_sum{service_name="hello-service",} 6.585046209
# HELP stork_service_discovery_instances_count_total The number of service instances discovered
# TYPE stork_service_discovery_instances_count_total counter
stork_service_discovery_instances_count_total{service_name="hello-service",} 26.0
```

The Stork service name can be found in the *tags*.

The metrics contain both the service discovery (`stork_service_discovery_*`) and the metrics about the service selection (`stork_service_selection_*`) such as the number of service instances, failures, and durations.

### Disable metrics

To disable the Stork metrics when `quarkus-micrometer` is used, add the following property to the application configuration:

```properties
quarkus.micrometer.binder.stork.enabled=false
```

<a id="stork-configuration-reference"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-smallrye-stork_section_quarkus-stork"></a> [Configuration for the service](#quarkus-smallrye-stork_section_quarkus-stork) | Type | Default |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-service-discovery-type"></a> [`quarkus.stork."service-name".service-discovery.type`](#quarkus-smallrye-stork_quarkus-stork-service-name-service-discovery-type) config_property_copy_button:+++quarkus.stork."service-name".service-discovery.type+++[]<br><br>Configures the service discovery type, e.g. "consul". ServiceDiscoveryProvider for the type has to be available<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__SERVICE_DISCOVERY_TYPE+++[] | string | required [exclamation circle] |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-service-discovery-params"></a> [`quarkus.stork."service-name".service-discovery."params"`](#quarkus-smallrye-stork_quarkus-stork-service-name-service-discovery-params) config_property_copy_button:+++quarkus.stork."service-name".service-discovery."params"+++[]<br><br>ServiceDiscovery parameters. Check the documentation of the selected service discovery type for available parameters.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__SERVICE_DISCOVERY__PARAMS_+++[] | Map\<String,String> |  |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-load-balancer-type"></a> [`quarkus.stork."service-name".load-balancer.type`](#quarkus-smallrye-stork_quarkus-stork-service-name-load-balancer-type) config_property_copy_button:+++quarkus.stork."service-name".load-balancer.type+++[]<br><br>Configures load balancer type, e.g. "round-robin". A LoadBalancerProvider for the type has to be available<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__LOAD_BALANCER_TYPE+++[] | string | `round-robin` |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-load-balancer-parameters"></a> [`quarkus.stork."service-name".load-balancer."parameters"`](#quarkus-smallrye-stork_quarkus-stork-service-name-load-balancer-parameters) config_property_copy_button:+++quarkus.stork."service-name".load-balancer."parameters"+++[]<br><br>Load Balancer parameters. Check the documentation of the selected load balancer type for available parameters<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__LOAD_BALANCER__PARAMETERS_+++[] | Map\<String,String> |  |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-service-registrar-enabled"></a> [`quarkus.stork."service-name".service-registrar.enabled`](#quarkus-smallrye-stork_quarkus-stork-service-name-service-registrar-enabled) config_property_copy_button:+++quarkus.stork."service-name".service-registrar.enabled+++[]<br><br>Whether automatic registration and deregistration of service instances is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__SERVICE_REGISTRAR_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-service-registrar-type"></a> [`quarkus.stork."service-name".service-registrar.type`](#quarkus-smallrye-stork_quarkus-stork-service-name-service-registrar-type) config_property_copy_button:+++quarkus.stork."service-name".service-registrar.type+++[]<br><br>Configures service registrar type, e.g. "consul". A ServiceRegistrarProvider for the type has to be available<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__SERVICE_REGISTRAR_TYPE+++[] | string |  |
| <a id="quarkus-smallrye-stork_quarkus-stork-service-name-service-registrar-parameters"></a> [`quarkus.stork."service-name".service-registrar."parameters"`](#quarkus-smallrye-stork_quarkus-stork-service-name-service-registrar-parameters) config_property_copy_button:+++quarkus.stork."service-name".service-registrar."parameters"+++[]<br><br>Service Registrar parameters. Check the documentation of the selected registrar type for available parameters<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK__SERVICE_NAME__SERVICE_REGISTRAR__PARAMETERS_+++[] | Map\<String,String> |  |
