# Automatic service registration with SmallRye Stork for Quarkus

This guide explains how to enable automatic registration and deregistration of a Quarkus application using SmallRye Stork and Consul, with minimal or no configuration.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Overview

SmallRye Stork supports service registration backends like Consul and Eureka. Traditionally, developers had to explicitly configure and manually register their service instances using code. With this feature, Quarkus applications can automatically register with a supported registry (e.g., Consul) during startup and deregister during shutdown — without writing boilerplate code.

**IMPORTANT**: Note that automatic registration is possible **only** for built-in registrars such as Consul, Eureka, and Static. If you're using a custom registrar, registration must still be handled programmatically. See the [stork.adoc](https://smallrye.io/smallrye-stork/2.7.10/programmatic-api/)\[Stork API documentation\] for an example of programmatic service registration.

This integration minimizes configuration effort and adheres to Quarkus' philosophy of doing as much as possible at build time.

## How it works

When the application starts and the `stork-service-registration-consul` dependency, besides the `quarkus-smallrye-stork`, is present:

- Quarkus performs a build-time check for registration configuration.
- If valid configuration is detected (or can be inferred), registration metadata is generated.
- **During startup**, the application registers itself automatically using the detected or configured IP address and port.
- During shutdown, the service is automatically deregistered.

The `stork-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/stork-quickstart) demonstrates Stork with Consul service discovery, which can be extended with automatic registration as shown below.

## Use cases/configuration options

The registration system uses sensible defaults but also allows for fine-grained control per service.

### Case 1: implicit configuration

No configuration required at all:

```xml
<dependency>
  <groupId>io.smallrye.stork</groupId>
  <artifactId>stork-service-registration-consul</artifactId>
</dependency>
```

Quarkus automatically uses:

- Application name
- Detected IP address
- HTTP port

The service is registered and unregistered automatically.

### Case 2: minimal explicit configuration

Only override what's necessary:

```properties
quarkus.stork.my-service.service-registrar.ip-address=192.168.0.42
```

No `quarkus.stork.my-service.service-registrar.type` is needed because there's only one registrar configuration in `application.properties`.

### Case 3: multiple services registrars

When multiple services are configured, you must specify the `quarkus.stork.<service-name>.service-registrar.type`:

```properties
quarkus.stork.red-service.service-registrar.type=consul
quarkus.stork.red-service.service-registrar.port=8083

quarkus.stork.blue-service.service-registrar.type=consul
quarkus.stork.blue-service.service-registrar.port=8084
```

### Case 4: disabling registration

Automatic registration is enabled by default. If you want to disable registration, you can do it per service using the following property:

```properties
quarkus.stork.red-service.service-registrar.enabled=false
```

### Case 5: advanced health check configuration

Customize how Consul polls service health:

```properties
quarkus.stork.my-service.service-registrar.parameters.health-check-url=/q/health/live
quarkus.stork.my-service.service-registrar.parameters.health-check-interval=10s
quarkus.stork.my-service.service-registrar.parameters.health-check-deregister-after=1m
```

### Case 6: static IP and port

Useful when behind a proxy or in production with known IPs:

```properties
quarkus.stork.my-service.service-registrar.ip-address=203.0.113.25
quarkus.stork.my-service.service-registrar.port=80
```

## Registration timeout

When the application starts, Quarkus waits for each service instance to be registered, and when it stops, for each service instance to be deregistered. If the service registry does not respond within 10 seconds, a warning is logged and the application continues to start or stop.

You can change this timeout, for example when the service registry takes longer to respond:

```properties
quarkus.stork.registration-timeout=30s
```

## IP detection

By default, Quarkus attempts to determine the IP address using the first available, non-loopback network interface. If this fails, it falls back to `localhost`.

Note that in Docker/Kubernetes environments, this detection may result in internal IPs that are not externally reachable. In such cases, it's recommended to explicitly set the IP and port.

## Summary

- Minimal or zero config to get service registration working
- Seamless integration with Consul via build-time processing
- Per-service override support
- Auto-deregistration on shutdown
- Easily extendable to support other backends

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
| <a id="quarkus-smallrye-stork_quarkus-stork-registration-timeout"></a> [`quarkus.stork.registration-timeout`](#quarkus-smallrye-stork_quarkus-stork-registration-timeout) config_property_copy_button:+++quarkus.stork.registration-timeout+++[]<br><br>The maximum time to wait for a service instance to be registered when the application starts, and deregistered when the application stops.<br><br>If the service registrar does not complete the operation in time, a warning is logged and the application continues to start or stop.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_STORK_REGISTRATION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-smallrye-stork_quarkus-stork) | `10S` |

<a id="duration-note-anchor-quarkus-smallrye-stork_quarkus-stork"></a>

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
