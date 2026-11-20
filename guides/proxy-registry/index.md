# Using Proxy Registry

This guide covers:

- How to configure a global proxy
- How to configure named proxies
- How to integrate with a Credentials Provider

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Introduction

A proxy acts as an intermediary between an application and external services. Instead of connecting directly to a remote host, the application sends requests through a proxy server. The proxy server then forwards the requests to the destination.

In Java applications, proxies are commonly required in environments where direct outbound network access is restricted. This is typical in corporate networks. This is also common in cloud environments with strict security rules. This is often required in production systems that require traffic inspection and auditing.

Proxies are commonly used to:

- Control and restrict outbound network access
- Enforce security and network policies
- Provide authentication for external connections
- Monitor and log outgoing traffic

## Proxy configuration

Many Java applications rely on JVM system properties or client-specific configuration to enable proxy support. This approach can lead to duplicated configuration. This approach can also lead to inconsistent behavior across clients.

Quarkus provides a centralized way to configure proxies using the Proxy Registry. All proxy configuration is defined under the `quarkus.proxy` configuration root.

You can configure:

- A default (global) proxy
- One or more named proxies that extensions can reference

Some Quarkus extensions already integrate with the Proxy Registry, including:

- [quarkus-rest-client](https://quarkus.io/guides/rest-client)
- [quarkus-redis-client](https://quarkus.io/guides/redis)
- [quarkus-smallrye-graphql-client](https://pt.quarkus.io/guides/smallrye-graphql-client)
- [quarkus-cxf](https://docs.quarkiverse.io/quarkus-cxf/dev/reference/extensions/quarkus-cxf.html#quarkus-cxf_quarkus-cxf-client-client-name-proxy-configuration-name) (since version 3.31.0)

## Configuring a default proxy

To configure a default (global) proxy, add the following properties to `application.properties`:

```properties
quarkus.proxy.host=localhost <1>
quarkus.proxy.port=8443 <2>
quarkus.proxy.type=HTTP <3>
```

1. The proxy host
2. The proxy port
3. The proxy type. Possible values are `HTTP` (default), `SOCKS4`, and `SOCKS5`.

Extensions that are aware of the Proxy Registry use the default proxy. This happens unless a named proxy is explicitly referenced in the configuration of the extension.

## Configuring named proxies

Named proxies allow different clients or extensions to use different proxy configurations.

The following example defines two named proxies:

```properties
quarkus.proxy.local.host=localhost
quarkus.proxy.local.port=3333
quarkus.proxy.local.type=SOCKS5

quarkus.proxy.my-proxy.host=localhost
quarkus.proxy.my-proxy.port=8443
quarkus.proxy.my-proxy.type=HTTP
quarkus.proxy.my-proxy.non-proxy-hosts=proxy.mycompany.com,proxy.org.acme <1>
```

1. Hostnames or IP addresses that should bypass the proxy

In this example, two named proxies are configured. The proxies are named `local` and `my-proxy`. Extensions can reference these proxies by name when required.

## Using credentials

Proxy authentication can be configured directly using the `username` and `password` properties.

```properties
quarkus.proxy.my-proxy.host=localhost
quarkus.proxy.my-proxy.port=8443
quarkus.proxy.my-proxy.type=HTTP
quarkus.proxy.my-proxy.non-proxy-hosts=proxy.mycompany.com,proxy.org.acme

quarkus.proxy.my-proxy.username=${PROXY_USERNAME}
quarkus.proxy.my-proxy.password=${PROXY_PASSWORD}
```

Credentials can be externalized using environment variables. Credentials can also be externalized using any other supported Quarkus configuration source.

### Providing credentials via a Credentials Provider

Instead of defining credentials directly in configuration, you can integrate with a Quarkus Credentials Provider. This allows credentials to be retrieved securely from external systems. Examples include vaults or secret managers.

This feature relies on the [Credentials Provider](/guides/credentials-provider/index.md) capability.

To configure a credentials provider for proxy authentication, use the following properties:

```properties
quarkus.proxy.host=localhost
quarkus.proxy.port=3838

quarkus.proxy.credentials-provider.name=credential
quarkus.proxy.credentials-provider.bean-name=credentialBean
quarkus.proxy.credentials-provider.username-key=userKey
quarkus.proxy.credentials-provider.password-key=passwordKey
```

The Proxy Registry automatically retrieves the credentials. The credentials are applied to the proxy configuration.

### How credentials are applied

When a proxy is configured with a Credentials Provider, the Proxy Registry automatically retrieves the credentials at startup. The retrieved credentials are applied to the proxy configuration.

The behavior is as follows:

- If `username` and `password` are explicitly configured, they are used.
- Otherwise, if a Credentials Provider is configured, the credentials are retrieved from the provider.
- If no credentials are configured, the proxy is used without authentication.

If only one of `username` or `password` is defined, the application fails at startup. The application also fails if the credentials cannot be retrieved from the provider. In both cases, a configuration error is reported.

This ensures that proxy configuration is validated early. This validation is consistent across all clients and extensions.

### Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-proxy-registry_quarkus-proxy-host"></a> [`quarkus.proxy.host`](#quarkus-proxy-registry_quarkus-proxy-host) config_property_copy_button:+++quarkus.proxy.host+++[]<br><br>Proxy host.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_HOST+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-port"></a> [`quarkus.proxy.port`](#quarkus-proxy-registry_quarkus-proxy-port) config_property_copy_button:+++quarkus.proxy.port+++[]<br><br>Proxy port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_PORT+++[] | int |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-credentials-provider-name"></a> [`quarkus.proxy.credentials-provider.name`](#quarkus-proxy-registry_quarkus-proxy-credentials-provider-name) config_property_copy_button:+++quarkus.proxy.credentials-provider.name+++[]<br><br>Name of the credential bucket to retrieve from the `CredentialsProvider`. If not set, the credentials provider will not be used.<br><br>A credentials provider offers a way to retrieve the key store password as well as alias password. Note that the credentials provider is only used if the username and password are not set in the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-credentials-provider-bean-name"></a> [`quarkus.proxy.credentials-provider.bean-name`](#quarkus-proxy-registry_quarkus-proxy-credentials-provider-bean-name) config_property_copy_button:+++quarkus.proxy.credentials-provider.bean-name+++[]<br><br>Name of the bean providing the credentials provider.<br><br>The name is used to select the credentials provider to use. The credentials provider must be exposed as a CDI bean and with the `@Named` annotation set to the configured name to be selected.<br><br>If not set, the default credentials provider is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_CREDENTIALS_PROVIDER_BEAN_NAME+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-credentials-provider-username-key"></a> [`quarkus.proxy.credentials-provider.username-key`](#quarkus-proxy-registry_quarkus-proxy-credentials-provider-username-key) config_property_copy_button:+++quarkus.proxy.credentials-provider.username-key+++[]<br><br>The key used to retrieve the username from the credentials provider.<br><br>If username, password or both cannot be retrieved from the credentials provider, an exception is thrown.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_CREDENTIALS_PROVIDER_USERNAME_KEY+++[] | string | `user` |
| <a id="quarkus-proxy-registry_quarkus-proxy-credentials-provider-password-key"></a> [`quarkus.proxy.credentials-provider.password-key`](#quarkus-proxy-registry_quarkus-proxy-credentials-provider-password-key) config_property_copy_button:+++quarkus.proxy.credentials-provider.password-key+++[]<br><br>The key used to retrieve the password from the credentials provider.<br><br>If username, password or both cannot be retrieved from the credentials provider, an exception is thrown.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_CREDENTIALS_PROVIDER_PASSWORD_KEY+++[] | string | `password` |
| <a id="quarkus-proxy-registry_quarkus-proxy-username"></a> [`quarkus.proxy.username`](#quarkus-proxy-registry_quarkus-proxy-username) config_property_copy_button:+++quarkus.proxy.username+++[]<br><br>Proxy username.<br><br>See also `credentials-provider`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_USERNAME+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-password"></a> [`quarkus.proxy.password`](#quarkus-proxy-registry_quarkus-proxy-password) config_property_copy_button:+++quarkus.proxy.password+++[]<br><br>Proxy password<br><br>See also `credentials-provider`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_PASSWORD+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-non-proxy-hosts"></a> [`quarkus.proxy.non-proxy-hosts`](#quarkus-proxy-registry_quarkus-proxy-non-proxy-hosts) config_property_copy_button:+++quarkus.proxy.non-proxy-hosts+++[]<br><br>Hostnames or IP addresses to exclude from proxying<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_NON_PROXY_HOSTS+++[] | list of string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-proxy-connect-timeout"></a> [`quarkus.proxy.proxy-connect-timeout`](#quarkus-proxy-registry_quarkus-proxy-proxy-connect-timeout) config_property_copy_button:+++quarkus.proxy.proxy-connect-timeout+++[]<br><br>Proxy connection timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_PROXY_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-proxy-registry_quarkus-proxy) | `10s` |
| <a id="quarkus-proxy-registry_quarkus-proxy-type"></a> [`quarkus.proxy.type`](#quarkus-proxy-registry_quarkus-proxy-type) config_property_copy_button:+++quarkus.proxy.type+++[]<br><br>The proxy type. Possible values are: `HTTP` (default), `SOCKS4` and `SOCKS5`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY_TYPE+++[] | `http`, `socks4`, `socks5` | `http` |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-host"></a> [`quarkus.proxy."named-proxy-configs".host`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-host) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".host+++[]<br><br>Proxy host.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__HOST+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-port"></a> [`quarkus.proxy."named-proxy-configs".port`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-port) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".port+++[]<br><br>Proxy port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__PORT+++[] | int |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-name"></a> [`quarkus.proxy."named-proxy-configs".credentials-provider.name`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-name) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".credentials-provider.name+++[]<br><br>Name of the credential bucket to retrieve from the `CredentialsProvider`. If not set, the credentials provider will not be used.<br><br>A credentials provider offers a way to retrieve the key store password as well as alias password. Note that the credentials provider is only used if the username and password are not set in the configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-bean-name"></a> [`quarkus.proxy."named-proxy-configs".credentials-provider.bean-name`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-bean-name) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".credentials-provider.bean-name+++[]<br><br>Name of the bean providing the credentials provider.<br><br>The name is used to select the credentials provider to use. The credentials provider must be exposed as a CDI bean and with the `@Named` annotation set to the configured name to be selected.<br><br>If not set, the default credentials provider is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__CREDENTIALS_PROVIDER_BEAN_NAME+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-username-key"></a> [`quarkus.proxy."named-proxy-configs".credentials-provider.username-key`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-username-key) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".credentials-provider.username-key+++[]<br><br>The key used to retrieve the username from the credentials provider.<br><br>If username, password or both cannot be retrieved from the credentials provider, an exception is thrown.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__CREDENTIALS_PROVIDER_USERNAME_KEY+++[] | string | `user` |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-password-key"></a> [`quarkus.proxy."named-proxy-configs".credentials-provider.password-key`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-credentials-provider-password-key) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".credentials-provider.password-key+++[]<br><br>The key used to retrieve the password from the credentials provider.<br><br>If username, password or both cannot be retrieved from the credentials provider, an exception is thrown.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__CREDENTIALS_PROVIDER_PASSWORD_KEY+++[] | string | `password` |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-username"></a> [`quarkus.proxy."named-proxy-configs".username`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-username) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".username+++[]<br><br>Proxy username.<br><br>See also `credentials-provider`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__USERNAME+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-password"></a> [`quarkus.proxy."named-proxy-configs".password`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-password) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".password+++[]<br><br>Proxy password<br><br>See also `credentials-provider`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__PASSWORD+++[] | string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-non-proxy-hosts"></a> [`quarkus.proxy."named-proxy-configs".non-proxy-hosts`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-non-proxy-hosts) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".non-proxy-hosts+++[]<br><br>Hostnames or IP addresses to exclude from proxying<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__NON_PROXY_HOSTS+++[] | list of string |  |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-proxy-connect-timeout"></a> [`quarkus.proxy."named-proxy-configs".proxy-connect-timeout`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-proxy-connect-timeout) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".proxy-connect-timeout+++[]<br><br>Proxy connection timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__PROXY_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-proxy-registry_quarkus-proxy) | `10s` |
| <a id="quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-type"></a> [`quarkus.proxy."named-proxy-configs".type`](#quarkus-proxy-registry_quarkus-proxy-named-proxy-configs-type) config_property_copy_button:+++quarkus.proxy."named-proxy-configs".type+++[]<br><br>The proxy type. Possible values are: `HTTP` (default), `SOCKS4` and `SOCKS5`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_PROXY__NAMED_PROXY_CONFIGS__TYPE+++[] | `http`, `socks4`, `socks5` | `http` |

<a id="duration-note-anchor-quarkus-proxy-registry_quarkus-proxy"></a>

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
