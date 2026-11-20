# Using xDS gRPC

This page explains how to enable xDS gRPC usage in your Quarkus application.

> [!IMPORTANT]
> This Quarkus xDS gRPC integration currently doesn't support building native executables due to the issues with shaded grpc-netty library while running native IT tests.

## Configuring your project

Add the Quarkus gRPC xDS extension to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-grpc-xds</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-grpc-xds")
```

> [!NOTE]
> This transitively adds `io.quarkus:quarkus-grpc` extension dependency.

## Server configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-grpc_quarkus-grpc-server-xds_quarkus-grpc-server-xds-enabled"></a> [`quarkus.grpc.server.xds.enabled`](#quarkus-grpc_quarkus-grpc-server-xds_quarkus-grpc-server-xds-enabled) config_property_copy_button:+++quarkus.grpc.server.xds.enabled+++[]<br><br>Explicitly enable use of XDS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_XDS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-server-xds_quarkus-grpc-server-xds-secure"></a> [`quarkus.grpc.server.xds.secure`](#quarkus-grpc_quarkus-grpc-server-xds_quarkus-grpc-server-xds-secure) config_property_copy_button:+++quarkus.grpc.server.xds.secure+++[]<br><br>Use secure credentials.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_SERVER_XDS_SECURE+++[] | boolean | `false` |

## Server configuration example

To enable server xDS, use the following configuration.

xDS must be explicitly enabled on the server, then verify you use it on the right xDS server port (default is 9000). If you want to use `XdsServerCredentials` set `xds.secure` to `true`.

```properties
quarkus.grpc.server.xds.enabled=true
#quarkus.grpc.server.xds.secure=true
quarkus.grpc.server.port=30051
```

> [!NOTE]
> When xDS is configured, `plain-text` is automatically disabled.

## Client configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-grpc_quarkus-grpc-clients-client-name-xds_quarkus-grpc-clients-client-name-xds-enabled"></a> [`quarkus.grpc.clients."client-name".xds.enabled`](#quarkus-grpc_quarkus-grpc-clients-client-name-xds_quarkus-grpc-clients-client-name-xds-enabled) config_property_copy_button:+++quarkus.grpc.clients."client-name".xds.enabled+++[]<br><br>Explicitly enable use of XDS.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__XDS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-clients-client-name-xds_quarkus-grpc-clients-client-name-xds-secure"></a> [`quarkus.grpc.clients."client-name".xds.secure`](#quarkus-grpc_quarkus-grpc-clients-client-name-xds_quarkus-grpc-clients-client-name-xds-secure) config_property_copy_button:+++quarkus.grpc.clients."client-name".xds.secure+++[]<br><br>Use secure credentials.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__XDS_SECURE+++[] | boolean | `false` |
| <a id="quarkus-grpc_quarkus-grpc-clients-client-name-xds_quarkus-grpc-clients-client-name-xds-target"></a> [`quarkus.grpc.clients."client-name".xds.target`](#quarkus-grpc_quarkus-grpc-clients-client-name-xds_quarkus-grpc-clients-client-name-xds-target) config_property_copy_button:+++quarkus.grpc.clients."client-name".xds.target+++[]<br><br>Optional explicit target.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_GRPC_CLIENTS__CLIENT_NAME__XDS_TARGET+++[] | string |  |

> [!NOTE]
> When xDS target property is used, name resolver, host, and port are not used

## Client configuration example

To enable client xDS, use the following configuration.

You can either explicitly enable xDS or you use `xds` name resolver, and make sure you point it to the right xDS server port (default is 9000). If you want to use `XdsChannelCredentials` set `xds.secure` to `true`.

```properties
#quarkus.grpc.clients.<client-name>.xds.enabled=true
#quarkus.grpc.clients.<client-name>.xds.secure=true
quarkus.grpc.clients.<client-name>.name-resolver=xds
quarkus.grpc.clients.<client-name>.port=30051
```

> [!NOTE]
> When xDS is configured, `plain-text` is automatically disabled.

## Kubernetes configuration example

Below is an example of (required) additional configuration when using xDS gRPC with the Istio Service Mesh in Kubernetes.

```properties
quarkus.kubernetes.ports.grpc.container-port=30051
quarkus.kubernetes.annotations."inject.istio.io/templates"=grpc-agent
quarkus.kubernetes.annotations."proxy.istio.io/config"={"holdApplicationUntilProxyStarts": true}
```
