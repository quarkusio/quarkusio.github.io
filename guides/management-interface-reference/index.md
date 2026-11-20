# Management interface reference

Various Quarkus extensions contribute non-application endpoints that provide different kinds of information about the application. Examples of such extensions are the [health](/guides/smallrye-health/index.md), [metrics](/guides/telemetry-micrometer/index.md), [OpenAPI](/guides/openapi-swaggerui/index.md) and [info](/guides/info/index.md) extensions.

These non-application endpoints are normally accessible under the `/q` prefix like so:

- `/q/health`
- `/q/metrics`
- `/q/openapi`
- `/q/info`

By default, these management endpoints are served by the same HTTP server as your application endpoints. This document presents how you can use a separate HTTP server (bound to a different network interface and port) for the management endpoints. It avoids exposing these endpoints on the main server and, therefore, prevents undesired accesses.

## Enabling the management interface

To enable the management interface, use the following **build-time** property:

```properties
quarkus.management.enabled=true
```

By default, management endpoints will be exposed on: `http://0.0.0.0:9000/q`. For example, if you have `smallrye-health` installed, the readiness probe will be exposed at `http://0.0.0.0:9000/q/health/ready`.

SmallRye Health Checks, Micrometer and Info endpoints will be declared as management endpoints when the management interface is enabled.

> [!NOTE]
> The management interface is disabled when no extensions relying on it (such as the SmallRye Health or SmallRye OpenAPI extensions) are installed.

## Configure the host, port and scheme

By default, the management interface is exposed on the interface: `0.0.0.0` (all interfaces) and on the port `9000` (`9001` in test mode). It does not use TLS (`https`) by default.

You can configure the host, ports, and TLS configuration name using the following properties:

- `quarkus.management.host` - the interface / host
- `quarkus.management.port` - the port
- `quarkus.management.test-port` - the port to use in test mode
- `quarkus.management.tls-configuration-name` - the TLS configuration name, [same as for the main HTTP server](/guides/http-reference/index.md#using-the-tls-centralized-configuration).

Here is a configuration example exposing the management interface on *<https://localhost:9002>*:

```properties
quarkus.management.enabled=true
quarkus.management.host=localhost
quarkus.management.port=9002
quarkus.management.tls-configuration-name=management

# Your TLS registry configuration
...
```

With this configuration, TLS is enabled and configured as defined in the `management` configuration of the TLS registry.

You can also configure the management interface with the legacy SSL configuration, as for ([the main HTTP server](/guides/http-reference/index.md#ssl)):

```properties
quarkus.management.enabled=true
quarkus.management.host=localhost
quarkus.management.port=9002
quarkus.management.ssl.certificate.key-store-file=server-keystore.jks
quarkus.management.ssl.certificate.key-store-password=secret
```

Key store, trust store and certificate files can be reloaded periodically. Configure the `quarkus.management.ssl.certificate.reload-period` property to specify the interval at which the certificates should be reloaded:

```properties
quarkus.http.management.certificate.files=/mount/certs/tls.crt
quarkus.http.management.certificate.key-files=/mount/certs/tls.key
quarkus.http.management.certificate.reload-period=1h
```

The files are reloaded from the same location as they were initially loaded from. If there is no content change, the reloading is a no-op. It the reloading fails, the server will continue to use the previous certificates.

> [!IMPORTANT]
> Unlike the main HTTP server, the management interface does not handle *http* and *https* at the same time. If *https* is configured, plain HTTP requests will be rejected.

## Configure the root path

Management endpoints are configured differently than standard HTTP endpoints. They use a unique root path, which is `/q` by default. This management root path can be configured using the `quarkus.management.root-path property`. For example, if you want to expose the management endpoints under `/management` use:

```properties
quarkus.management.enabled=true
quarkus.management.root-path=/management
```

The mounting rules of the management endpoints slightly differ from the ones used when using the main HTTP server:

- Management endpoints configured using a *relative* path (not starting with `/`) will be served from the configured root path. For example, if the endpoint path is `health` and the root path is `management`, the resulting path is `/management/health`
- Management endpoints configured using an *absolute* path (starting with `/`) will be served from the root. For example, if the endpoint path is `/health`, the resulting path is `/health`, regardless of the root path
- The management interface does not use the HTTP root path from the main HTTP server.

> [!IMPORTANT]
> The `quarkus.http.root-path` property is only applied to the main HTTP server and not to the management interface. In addition, the `quarkus.http.non-application-root-path` property is not used for endpoint exposed on the management interface.

<a id="management-endpoint-extension"></a>
## Create a management endpoint in an extension

> [!NOTE]
> To expose an endpoint on the management interface from the code of an application, refer to [the application section](#management-endpoint-application).

SmallRye Health Checks, and Micrometer endpoints will be declared as management endpoints when the management interface is enabled.

> [!NOTE]
> if you do not enable the management interface, these endpoints will be served using the main HTTP server (under `/q` by default).

Extensions can create a management endpoint by defining a *non application* route and calling `management()` method:

```java
@BuildStep
void createManagementRoute(BuildProducer<RouteBuildItem> routes,
        NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
        MyRecorder recorder) {

    routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
        .management() // Must be called BEFORE the routeFunction method
        .routeFunction("my-path", recorder.route())
        .handler(recorder.getHandler())
        .blockingRoute()
        .build());
    //...
}
```

If the management interface is enabled, the endpoint will be exposed on: `http://0.0.0.0:9000/q/my-path`. Otherwise, it will be exposed on: `http://localhost:8080/q/my-path`.

> [!IMPORTANT]
> Management endpoints can only be declared by extensions and not from the application code.

<a id="management-endpoint-application"></a>
## Exposing an endpoint on the management interface (as an application)

You can expose endpoints on the management interface by registering routes on the management router. To access the router use the following code:

```java
public void registerManagementRoutes(@Observes ManagementInterface mi) {
       mi.router().get("/admin").handler(rc ->
            rc.response().end("admin it is")
       );
}
```

The `io.quarkus.vertx.http.ManagementInterface` event is fired when the management interface is initialized. So, if the management interface is not enabled, the method won't be called.

The `router()` method returns a `io.vertx.ext.web.Router` object which can be used to register routes. The paths are relative to `/`. For example, the previous snippet registers a route on `/admin`. This route is accessible on `http://0.0.0.0:9000/admin`, if you use the default host and port.

More details about the `Router` API can be found on [the Vert.x Web documentation](https://vertx.io/docs/vertx-web/java/).

## Management Interface Configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-vertx-http_quarkus-management-enabled"></a> [`quarkus.management.enabled`](#quarkus-vertx-http_quarkus-management-enabled) config_property_copy_button:+++quarkus.management.enabled+++[]<br><br>Enables / Disables the usage of a separate interface/port to expose the management endpoints. If sets to `true`, the management endpoints will be exposed to a different HTTP server. This avoids exposing the management endpoints on a publicly available server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-auth-enabled"></a> [`quarkus.management.auth.enabled`](#quarkus-vertx-http_quarkus-management-auth-enabled) config_property_copy_button:+++quarkus.management.auth.enabled+++[]<br><br>If authentication for the management interface should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_ENABLED+++[] | boolean | `${quarkus.management.auth.basic:false}` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-auth-basic"></a> [`quarkus.management.auth.basic`](#quarkus-vertx-http_quarkus-management-auth-basic) config_property_copy_button:+++quarkus.management.auth.basic+++[]<br><br>If basic auth should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_BASIC+++[] | boolean |  |
| [lock] <a id="quarkus-vertx-http_quarkus-management-auth-proactive"></a> [`quarkus.management.auth.proactive`](#quarkus-vertx-http_quarkus-management-auth-proactive) config_property_copy_button:+++quarkus.management.auth.proactive+++[]<br><br>If this is true and credentials are present then a user will always be authenticated before the request progresses.<br><br>If this is false then an attempt will only be made to authenticate the user if a permission check is performed or the current user is required for some other reason.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PROACTIVE+++[] | boolean | `true` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-ssl-client-auth"></a> [`quarkus.management.ssl.client-auth`](#quarkus-vertx-http_quarkus-management-ssl-client-auth) config_property_copy_button:+++quarkus.management.ssl.client-auth+++[]<br><br>Configures the engine to require/request client authentication. NONE, REQUEST, REQUIRED<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CLIENT_AUTH+++[] | `none`, `request`, `required` | `none` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-root-path"></a> [`quarkus.management.root-path`](#quarkus-vertx-http_quarkus-management-root-path) config_property_copy_button:+++quarkus.management.root-path+++[]<br><br>A common root path for management endpoints. Various extension-provided management endpoints such as metrics and health are deployed under this path by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_ROOT_PATH+++[] | string | `/q` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-enable-compression"></a> [`quarkus.management.enable-compression`](#quarkus-vertx-http_quarkus-management-enable-compression) config_property_copy_button:+++quarkus.management.enable-compression+++[]<br><br>If responses should be compressed.<br><br>Note that this will attempt to compress all responses, to avoid compressing already compressed content (such as images) you need to set the following header:<br><br>Content-Encoding: identity<br><br>Which will tell vert.x not to compress the response.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_ENABLE_COMPRESSION+++[] | boolean | `false` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-enable-decompression"></a> [`quarkus.management.enable-decompression`](#quarkus-vertx-http_quarkus-management-enable-decompression) config_property_copy_button:+++quarkus.management.enable-decompression+++[]<br><br>When enabled, vert.x will decompress the request's body if it's compressed.<br><br>Note that the compression format (e.g., gzip) must be specified in the Content-Encoding header in the request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_ENABLE_DECOMPRESSION+++[] | boolean | `false` |
| [lock] <a id="quarkus-vertx-http_quarkus-management-compression-level"></a> [`quarkus.management.compression-level`](#quarkus-vertx-http_quarkus-management-compression-level) config_property_copy_button:+++quarkus.management.compression-level+++[]<br><br>The compression level used when compression support is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_COMPRESSION_LEVEL+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-roles-mapping-role-name"></a> [`quarkus.management.auth.roles-mapping."role-name"`](#quarkus-vertx-http_quarkus-management-auth-roles-mapping-role-name) config_property_copy_button:+++quarkus.management.auth.roles-mapping."role-name"+++[]<br><br>Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.<br><br>For example, if `SecurityIdentity` has a `user` role and the endpoint is secured with a 'UserRole' role, use this property to map the `user` role to the `UserRole` role, and have `SecurityIdentity` to have both `user` and `UserRole` roles.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_ROLES_MAPPING__ROLE_NAME_+++[] | Map\<String,List\<String>> |  |
| <a id="quarkus-vertx-http_quarkus-management-port"></a> [`quarkus.management.port`](#quarkus-vertx-http_quarkus-management-port) config_property_copy_button:+++quarkus.management.port+++[]<br><br>The HTTP port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PORT+++[] | int | `9000` |
| <a id="quarkus-vertx-http_quarkus-management-test-port"></a> [`quarkus.management.test-port`](#quarkus-vertx-http_quarkus-management-test-port) config_property_copy_button:+++quarkus.management.test-port+++[]<br><br>The HTTP port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_TEST_PORT+++[] | int | `9001` |
| <a id="quarkus-vertx-http_quarkus-management-host"></a> [`quarkus.management.host`](#quarkus-vertx-http_quarkus-management-host) config_property_copy_button:+++quarkus.management.host+++[]<br><br>The HTTP host<br><br>In dev/test mode this defaults to localhost, in prod mode this defaults to 0.0.0.0<br><br>Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it is not suitable for dev/test mode as other people on the network can connect to your development machine.<br><br>As an exception, when running in Windows Subsystem for Linux (WSL), the HTTP host defaults to 0.0.0.0 even in dev/test mode since using localhost makes the application inaccessible.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HOST+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-management-host-enabled"></a> [`quarkus.management.host-enabled`](#quarkus-vertx-http_quarkus-management-host-enabled) config_property_copy_button:+++quarkus.management.host-enabled+++[]<br><br>Enable listening to host:port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HOST_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-credentials-provider"></a> [`quarkus.management.ssl.certificate.credentials-provider`](#quarkus-vertx-http_quarkus-management-ssl-certificate-credentials-provider) config_property_copy_button:+++quarkus.management.ssl.certificate.credentials-provider+++[]<br><br>The `CredentialsProvider`. If this property is configured, then a matching 'CredentialsProvider' will be used to get the keystore, keystore key, and truststore passwords unless these passwords have already been configured.<br><br>Please note that using MicroProfile `ConfigSource` which is directly supported by Quarkus Configuration should be preferred unless using `CredentialsProvider` provides for some additional security and dynamism.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_CREDENTIALS_PROVIDER+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-credentials-provider-name"></a> [`quarkus.management.ssl.certificate.credentials-provider-name`](#quarkus-vertx-http_quarkus-management-ssl-certificate-credentials-provider-name) config_property_copy_button:+++quarkus.management.ssl.certificate.credentials-provider-name+++[]<br><br>The credentials provider bean name.<br><br>This is a bean name (as in `@Named`) of a bean that implements `CredentialsProvider`. It is used to select the credentials provider bean when multiple exist. This is unnecessary when there is only one credentials provider available.<br><br>For Vault, the credentials provider bean name is `vault-credentials-provider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_CREDENTIALS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-files"></a> [`quarkus.management.ssl.certificate.files`](#quarkus-vertx-http_quarkus-management-ssl-certificate-files) config_property_copy_button:+++quarkus.management.ssl.certificate.files+++[]<br><br>The list of path to server certificates using the PEM format. Specifying multiple files requires SNI to be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_FILES+++[] | list of path |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-files"></a> [`quarkus.management.ssl.certificate.key-files`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-files) config_property_copy_button:+++quarkus.management.ssl.certificate.key-files+++[]<br><br>The list of path to server certificates private key files using the PEM format. Specifying multiple files requires SNI to be enabled.<br><br>The order of the key files must match the order of the certificates.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_FILES+++[] | list of path |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-file"></a> [`quarkus.management.ssl.certificate.key-store-file`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-file) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-file+++[]<br><br>An optional keystore that holds the certificate information instead of specifying separate files.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_FILE+++[] | path |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-file-type"></a> [`quarkus.management.ssl.certificate.key-store-file-type`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-file-type) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-file-type+++[]<br><br>An optional parameter to specify the type of the keystore file. If not given, the type is automatically detected based on the file name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_FILE_TYPE+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-provider"></a> [`quarkus.management.ssl.certificate.key-store-provider`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-provider) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-provider+++[]<br><br>An optional parameter to specify a provider of the keystore file. If not given, the provider is automatically detected based on the keystore file type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_PROVIDER+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-password"></a> [`quarkus.management.ssl.certificate.key-store-password`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-password) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-password+++[]<br><br>A parameter to specify the password of the keystore file. If not given, and if it can not be retrieved from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-password-key"></a> [`quarkus.management.ssl.certificate.key-store-password-key`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-password-key) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-password-key+++[]<br><br>A parameter to specify a `CredentialsProvider` property key, which can be used to get the password of the key store file from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_PASSWORD_KEY+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-alias"></a> [`quarkus.management.ssl.certificate.key-store-alias`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-alias) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-alias+++[]<br><br>An optional parameter to select a specific key in the keystore. When SNI is disabled, and the keystore contains multiple keys and no alias is specified; the behavior is undefined.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_ALIAS+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-alias-password"></a> [`quarkus.management.ssl.certificate.key-store-alias-password`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-alias-password) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-alias-password+++[]<br><br>An optional parameter to define the password for the key, in case it is different from `key-store-password` If not given, it might be retrieved from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_ALIAS_PASSWORD+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-alias-password-key"></a> [`quarkus.management.ssl.certificate.key-store-alias-password-key`](#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-alias-password-key) config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-alias-password-key+++[]<br><br>A parameter to specify a `CredentialsProvider` property key, which can be used to get the password for the alias from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_ALIAS_PASSWORD_KEY+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-file"></a> [`quarkus.management.ssl.certificate.trust-store-file`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-file) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-file+++[]<br><br>An optional trust store that holds the certificate information of the trusted certificates.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_FILE+++[] | path |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-files"></a> [`quarkus.management.ssl.certificate.trust-store-files`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-files) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-files+++[]<br><br>An optional list of trusted certificates using the PEM format. If you pass multiple files, you must use the PEM format.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_FILES+++[] | list of path |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-file-type"></a> [`quarkus.management.ssl.certificate.trust-store-file-type`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-file-type) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-file-type+++[]<br><br>An optional parameter to specify the type of the trust store file. If not given, the type is automatically detected based on the file name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_FILE_TYPE+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-provider"></a> [`quarkus.management.ssl.certificate.trust-store-provider`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-provider) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-provider+++[]<br><br>An optional parameter to specify a provider of the trust store file. If not given, the provider is automatically detected based on the trust store file type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_PROVIDER+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-password"></a> [`quarkus.management.ssl.certificate.trust-store-password`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-password) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-password+++[]<br><br>A parameter to specify the password of the trust store file. If not given, it might be retrieved from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-password-key"></a> [`quarkus.management.ssl.certificate.trust-store-password-key`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-password-key) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-password-key+++[]<br><br>A parameter to specify a `CredentialsProvider` property key, which can be used to get the password of the trust store file from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_PASSWORD_KEY+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-cert-alias"></a> [`quarkus.management.ssl.certificate.trust-store-cert-alias`](#quarkus-vertx-http_quarkus-management-ssl-certificate-trust-store-cert-alias) config_property_copy_button:+++quarkus.management.ssl.certificate.trust-store-cert-alias+++[]<br><br>An optional parameter to trust a single certificate from the trust store rather than trusting all certificates in the store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_TRUST_STORE_CERT_ALIAS+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-reload-period"></a> [`quarkus.management.ssl.certificate.reload-period`](#quarkus-vertx-http_quarkus-management-ssl-certificate-reload-period) config_property_copy_button:+++quarkus.management.ssl.certificate.reload-period+++[]<br><br>When set, the configured certificate will be reloaded after the given period. Note that the certificate will be reloaded only if the file has been modified.<br><br>Also, the update can also occur when the TLS certificate is configured using paths (and not in-memory).<br><br>The reload period must be equal or greater than 30 seconds. If not set, the certificate will not be reloaded.<br><br>> [!IMPORTANT]<br>> It's recommended to use the TLS registry to handle the certificate reloading.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_RELOAD_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-management) |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-cipher-suites"></a> [`quarkus.management.ssl.cipher-suites`](#quarkus-vertx-http_quarkus-management-ssl-cipher-suites) config_property_copy_button:+++quarkus.management.ssl.cipher-suites+++[]<br><br>The cipher suites to use. If none is given, a reasonable default is selected.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CIPHER_SUITES+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-protocols"></a> [`quarkus.management.ssl.protocols`](#quarkus-vertx-http_quarkus-management-ssl-protocols) config_property_copy_button:+++quarkus.management.ssl.protocols+++[]<br><br>Sets the ordered list of enabled SSL/TLS protocols.<br><br>If not set, it defaults to `"TLSv1.3, TLSv1.2"`. The following list of protocols are supported: `TLSv1, TLSv1.1, TLSv1.2, TLSv1.3`. To only enable `TLSv1.3`, set the value to `to "TLSv1.3"`.<br><br>Note that setting an empty list, and enabling SSL/TLS is invalid. You must at least have one protocol.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_PROTOCOLS+++[] | list of string | `TLSv1.3`, `TLSv1.2` |
| <a id="quarkus-vertx-http_quarkus-management-ssl-sni"></a> [`quarkus.management.ssl.sni`](#quarkus-vertx-http_quarkus-management-ssl-sni) config_property_copy_button:+++quarkus.management.ssl.sni+++[]<br><br>Enables Server Name Indication (SNI), an TLS extension allowing the server to use multiple certificates. The client indicate the server name during the TLS handshake, allowing the server to select the right certificate.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_SNI+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-tls-configuration-name"></a> [`quarkus.management.tls-configuration-name`](#quarkus-vertx-http_quarkus-management-tls-configuration-name) config_property_copy_button:+++quarkus.management.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If not set and the default TLS configuration is configured (`quarkus.tls.*`) then that will be used. If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>If no TLS configuration is set, and `quarkus.tls.*` is not configured, then, `quarkus.management.ssl` will be used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-handle-100-continue-automatically"></a> [`quarkus.management.handle-100-continue-automatically`](#quarkus-vertx-http_quarkus-management-handle-100-continue-automatically) config_property_copy_button:+++quarkus.management.handle-100-continue-automatically+++[]<br><br>When set to `true`, the HTTP server automatically sends `100 CONTINUE` response when the request expects it (with the `Expect: 100-Continue` header).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HANDLE_100_CONTINUE_AUTOMATICALLY+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-use-semicolon-as-query-param-delimiter"></a> [`quarkus.management.use-semicolon-as-query-param-delimiter`](#quarkus-vertx-http_quarkus-management-use-semicolon-as-query-param-delimiter) config_property_copy_button:+++quarkus.management.use-semicolon-as-query-param-delimiter+++[]<br><br>Whether the HTTP server should treat the semicolon (`;`) as a query parameter delimiter, in addition to the ampersand (`&`).<br><br>When set to `true` (the default), a request like `/path?a=1;b=2` is parsed as two parameters (`a=1` and `b=2`). When set to `false`, the semicolon is treated as a literal character and the request yields a single parameter (`a=1;b=2`).<br><br>The default is `true` to preserve backward compatibility. It will change to `false` in Quarkus 4, as using the semicolon as a query parameter delimiter is uncommon and can cause issues when semicolons appear as part of parameter values.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_USE_SEMICOLON_AS_QUERY_PARAM_DELIMITER+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-header-size"></a> [`quarkus.management.limits.max-header-size`](#quarkus-vertx-http_quarkus-management-limits-max-header-size) config_property_copy_button:+++quarkus.management.limits.max-header-size+++[]<br><br>The maximum length of all headers, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_HEADER_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-management) | `20K` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-body-size"></a> [`quarkus.management.limits.max-body-size`](#quarkus-vertx-http_quarkus-management-limits-max-body-size) config_property_copy_button:+++quarkus.management.limits.max-body-size+++[]<br><br>The maximum size of a request body, up to `Long.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_BODY_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-management) | `10240K` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-chunk-size"></a> [`quarkus.management.limits.max-chunk-size`](#quarkus-vertx-http_quarkus-management-limits-max-chunk-size) config_property_copy_button:+++quarkus.management.limits.max-chunk-size+++[]<br><br>The max HTTP chunk size, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_CHUNK_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-management) | `8192` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-initial-line-length"></a> [`quarkus.management.limits.max-initial-line-length`](#quarkus-vertx-http_quarkus-management-limits-max-initial-line-length) config_property_copy_button:+++quarkus.management.limits.max-initial-line-length+++[]<br><br>The maximum length of the initial line (e.g. `"GET / HTTP/1.0"`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_INITIAL_LINE_LENGTH+++[] | int | `4096` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-form-attribute-size"></a> [`quarkus.management.limits.max-form-attribute-size`](#quarkus-vertx-http_quarkus-management-limits-max-form-attribute-size) config_property_copy_button:+++quarkus.management.limits.max-form-attribute-size+++[]<br><br>The maximum length of a form attribute, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_FORM_ATTRIBUTE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-management) | `2048` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-form-fields"></a> [`quarkus.management.limits.max-form-fields`](#quarkus-vertx-http_quarkus-management-limits-max-form-fields) config_property_copy_button:+++quarkus.management.limits.max-form-fields+++[]<br><br>Set the maximum number of fields of a form. Set to `-1` to allow unlimited number of attributes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_FORM_FIELDS+++[] | int | `256` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-form-buffered-bytes"></a> [`quarkus.management.limits.max-form-buffered-bytes`](#quarkus-vertx-http_quarkus-management-limits-max-form-buffered-bytes) config_property_copy_button:+++quarkus.management.limits.max-form-buffered-bytes+++[]<br><br>Set the maximum number of bytes a server can buffer when decoding a form. Set to `-1` to allow unlimited length, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_FORM_BUFFERED_BYTES+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-management) | `1K` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-parameters"></a> [`quarkus.management.limits.max-parameters`](#quarkus-vertx-http_quarkus-management-limits-max-parameters) config_property_copy_button:+++quarkus.management.limits.max-parameters+++[]<br><br>The maximum number of HTTP request parameters permitted for incoming requests.<br><br>If a client sends more than this number of parameters in a request, the connection is closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_PARAMETERS+++[] | int | `1000` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-multipart-part-header-size"></a> [`quarkus.management.limits.max-multipart-part-header-size`](#quarkus-vertx-http_quarkus-management-limits-max-multipart-part-header-size) config_property_copy_button:+++quarkus.management.limits.max-multipart-part-header-size+++[]<br><br>The maximum size of the headers section within a single MIME part of a `multipart/form-data` request. This limits the combined size of all header names and values for each individual part (e.g., the Content-Disposition and Content-Type headers), not the HTTP request headers.<br><br>If a part's headers exceed this limit, the request is rejected with HTTP 413.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_MULTIPART_PART_HEADER_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-management) | `32K` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-multipart-header-count"></a> [`quarkus.management.limits.max-multipart-header-count`](#quarkus-vertx-http_quarkus-management-limits-max-multipart-header-count) config_property_copy_button:+++quarkus.management.limits.max-multipart-header-count+++[]<br><br>The maximum number of headers allowed within a single MIME part of a `multipart/form-data` request.<br><br>If a part contains more headers than this limit, the request is rejected with HTTP 413.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_MULTIPART_HEADER_COUNT+++[] | int | `40` |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-connections"></a> [`quarkus.management.limits.max-connections`](#quarkus-vertx-http_quarkus-management-limits-max-connections) config_property_copy_button:+++quarkus.management.limits.max-connections+++[]<br><br>The maximum number of connections that are allowed at any one time. If this is set it is recommended to set a short idle timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_CONNECTIONS+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-management-limits-header-table-size"></a> [`quarkus.management.limits.header-table-size`](#quarkus-vertx-http_quarkus-management-limits-header-table-size) config_property_copy_button:+++quarkus.management.limits.header-table-size+++[]<br><br>Set the SETTINGS_HEADER_TABLE_SIZE HTTP/2 setting.<br><br>Allows the sender to inform the remote endpoint of the maximum size of the header compression table used to decode header blocks, in octets. The encoder can select any size equal to or less than this value by using signaling specific to the header compression format inside a header block. The initial value is `4,096` octets.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_HEADER_TABLE_SIZE+++[] | long |  |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-concurrent-streams"></a> [`quarkus.management.limits.max-concurrent-streams`](#quarkus-vertx-http_quarkus-management-limits-max-concurrent-streams) config_property_copy_button:+++quarkus.management.limits.max-concurrent-streams+++[]<br><br>Set SETTINGS_MAX_CONCURRENT_STREAMS HTTP/2 setting.<br><br>Indicates the maximum number of concurrent streams that the sender will allow. This limit is directional: it applies to the number of streams that the sender permits the receiver to create. Initially, there is no limit to this value. It is recommended that this value be no smaller than 100, to not unnecessarily limit parallelism.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_CONCURRENT_STREAMS+++[] | long |  |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-frame-size"></a> [`quarkus.management.limits.max-frame-size`](#quarkus-vertx-http_quarkus-management-limits-max-frame-size) config_property_copy_button:+++quarkus.management.limits.max-frame-size+++[]<br><br>Set the SETTINGS_MAX_FRAME_SIZE HTTP/2 setting. Indicates the size of the largest frame payload that the sender is willing to receive, in octets. The initial value is `2^14` (16,384) octets.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_FRAME_SIZE+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-management-limits-max-header-list-size"></a> [`quarkus.management.limits.max-header-list-size`](#quarkus-vertx-http_quarkus-management-limits-max-header-list-size) config_property_copy_button:+++quarkus.management.limits.max-header-list-size+++[]<br><br>Set the SETTINGS_MAX_HEADER_LIST_SIZE HTTP/2 setting. This advisory setting informs a peer of the maximum size of header list that the sender is prepared to accept, in octets. The value is based on the uncompressed size of header fields, including the length of the name and value in octets plus an overhead of 32 octets for each header field. The default value is `8192`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_MAX_HEADER_LIST_SIZE+++[] | long |  |
| <a id="quarkus-vertx-http_quarkus-management-limits-rst-flood-max-rst-frame-per-window"></a> [`quarkus.management.limits.rst-flood-max-rst-frame-per-window`](#quarkus-vertx-http_quarkus-management-limits-rst-flood-max-rst-frame-per-window) config_property_copy_button:+++quarkus.management.limits.rst-flood-max-rst-frame-per-window+++[]<br><br>Set the max number of RST frame allowed per time window, this is used to prevent [HTTP/2 RST frame flood DDOS attacks](https://github.com/netty/netty/security/advisories/GHSA-xpw8-rcwv-8f8p). The default value is `200`, setting zero or a negative value, disables flood protection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_RST_FLOOD_MAX_RST_FRAME_PER_WINDOW+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-management-limits-rst-flood-window-duration"></a> [`quarkus.management.limits.rst-flood-window-duration`](#quarkus-vertx-http_quarkus-management-limits-rst-flood-window-duration) config_property_copy_button:+++quarkus.management.limits.rst-flood-window-duration+++[]<br><br>Set the duration of the time window when checking the max number of RST frames, this is used to prevent [HTTP/2 RST frame flood DDOS attacks](https://github.com/netty/netty/security/advisories/GHSA-xpw8-rcwv-8f8p).. The default value is `30 s`, setting zero or a negative value, disables flood protection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_LIMITS_RST_FLOOD_WINDOW_DURATION+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-management) |  |
| <a id="quarkus-vertx-http_quarkus-management-idle-timeout"></a> [`quarkus.management.idle-timeout`](#quarkus-vertx-http_quarkus-management-idle-timeout) config_property_copy_button:+++quarkus.management.idle-timeout+++[]<br><br>Http connection idle timeout<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-management) | `30M` |
| <a id="quarkus-vertx-http_quarkus-management-body-handle-file-uploads"></a> [`quarkus.management.body.handle-file-uploads`](#quarkus-vertx-http_quarkus-management-body-handle-file-uploads) config_property_copy_button:+++quarkus.management.body.handle-file-uploads+++[]<br><br>Whether the files sent using `multipart/form-data` will be stored locally.<br><br>If `true`, they will be stored in `quarkus.http.body-handler.uploads-directory` and will be made available via `io.vertx.ext.web.RoutingContext.fileUploads()`. Otherwise, the files sent using `multipart/form-data` will not be stored locally, and `io.vertx.ext.web.RoutingContext.fileUploads()` will always return an empty collection. Note that even with this option being set to `false`, the `multipart/form-data` requests will be accepted.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_BODY_HANDLE_FILE_UPLOADS+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-body-uploads-directory"></a> [`quarkus.management.body.uploads-directory`](#quarkus-vertx-http_quarkus-management-body-uploads-directory) config_property_copy_button:+++quarkus.management.body.uploads-directory+++[]<br><br>The directory where the files sent using `multipart/form-data` should be stored.<br><br>Either an absolute path or a path relative to the current directory of the application process.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_BODY_UPLOADS_DIRECTORY+++[] | string | `${java.io.tmpdir}/uploads` |
| <a id="quarkus-vertx-http_quarkus-management-body-merge-form-attributes"></a> [`quarkus.management.body.merge-form-attributes`](#quarkus-vertx-http_quarkus-management-body-merge-form-attributes) config_property_copy_button:+++quarkus.management.body.merge-form-attributes+++[]<br><br>Whether the form attributes should be added to the request parameters.<br><br>If `true`, the form attributes will be added to the request parameters; otherwise the form parameters will not be added to the request parameters<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_BODY_MERGE_FORM_ATTRIBUTES+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-body-delete-uploaded-files-on-end"></a> [`quarkus.management.body.delete-uploaded-files-on-end`](#quarkus-vertx-http_quarkus-management-body-delete-uploaded-files-on-end) config_property_copy_button:+++quarkus.management.body.delete-uploaded-files-on-end+++[]<br><br>Whether the uploaded files should be removed after serving the request.<br><br>If `true` the uploaded files stored in `quarkus.http.body-handler.uploads-directory` will be removed after handling the request. Otherwise, the files will be left there forever.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_BODY_DELETE_UPLOADED_FILES_ON_END+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-body-preallocate-body-buffer"></a> [`quarkus.management.body.preallocate-body-buffer`](#quarkus-vertx-http_quarkus-management-body-preallocate-body-buffer) config_property_copy_button:+++quarkus.management.body.preallocate-body-buffer+++[]<br><br>Whether the body buffer should pre-allocated based on the `Content-Length` header value.<br><br>If `true` the body buffer is pre-allocated according to the size read from the `Content-Length` header. Otherwise, the body buffer is pre-allocated to 1KB, and is resized dynamically<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_BODY_PREALLOCATE_BODY_BUFFER+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-body-multipart-file-content-types"></a> [`quarkus.management.body.multipart.file-content-types`](#quarkus-vertx-http_quarkus-management-body-multipart-file-content-types) config_property_copy_button:+++quarkus.management.body.multipart.file-content-types+++[]<br><br>A comma-separated list of `ContentType` to indicate whether a given multipart field should be handled as a file part.<br><br>You can use this setting to force HTTP-based extensions to parse a message part as a file based on its content type.<br><br>For now, this setting only works when using RESTEasy Reactive.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_BODY_MULTIPART_FILE_CONTENT_TYPES+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-accept-backlog"></a> [`quarkus.management.accept-backlog`](#quarkus-vertx-http_quarkus-management-accept-backlog) config_property_copy_button:+++quarkus.management.accept-backlog+++[]<br><br>The accept backlog, this is how many connections can be waiting to be accepted before connections start being rejected<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_ACCEPT_BACKLOG+++[] | int | `-1` |
| <a id="quarkus-vertx-http_quarkus-management-domain-socket"></a> [`quarkus.management.domain-socket`](#quarkus-vertx-http_quarkus-management-domain-socket) config_property_copy_button:+++quarkus.management.domain-socket+++[]<br><br>Path to a unix domain socket<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_DOMAIN_SOCKET+++[] | string | `/var/run/io.quarkus.management.socket` |
| <a id="quarkus-vertx-http_quarkus-management-domain-socket-enabled"></a> [`quarkus.management.domain-socket-enabled`](#quarkus-vertx-http_quarkus-management-domain-socket-enabled) config_property_copy_button:+++quarkus.management.domain-socket-enabled+++[]<br><br>Enable listening to host:port<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_DOMAIN_SOCKET_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-use-proxy-protocol"></a> [`quarkus.management.proxy.use-proxy-protocol`](#quarkus-vertx-http_quarkus-management-proxy-use-proxy-protocol) config_property_copy_button:+++quarkus.management.proxy.use-proxy-protocol+++[]<br><br>Set whether the server should use the HA `PROXY` protocol when serving requests from behind a proxy. (see the [PROXY Protocol](https://www.haproxy.org/download/1.8/doc/proxy-protocol.txt)). When set to `true`, the remote address returned will be the one from the actual connecting client. If it is set to `false` (default), the remote address returned will be the one from the proxy.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_USE_PROXY_PROTOCOL+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-proxy-address-forwarding"></a> [`quarkus.management.proxy.proxy-address-forwarding`](#quarkus-vertx-http_quarkus-management-proxy-proxy-address-forwarding) config_property_copy_button:+++quarkus.management.proxy.proxy-address-forwarding+++[]<br><br>If this is true then the address, scheme etc. will be set from headers forwarded by the proxy server, such as `X-Forwarded-For`. This should only be set if you are behind a proxy that sets these headers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_PROXY_ADDRESS_FORWARDING+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-allow-forwarded"></a> [`quarkus.management.proxy.allow-forwarded`](#quarkus-vertx-http_quarkus-management-proxy-allow-forwarded) config_property_copy_button:+++quarkus.management.proxy.allow-forwarded+++[]<br><br>If this is true and proxy address forwarding is enabled then the standard `Forwarded` header will be used. In case the not standard `X-Forwarded-For` header is enabled and detected on HTTP requests, the standard header has the precedence. Activating this together with `quarkus.http.proxy.allow-x-forwarded` has security implications as clients can forge requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected `Forwarded` or `X-Forwarded-*` headers from the client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_ALLOW_FORWARDED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-allow-x-forwarded"></a> [`quarkus.management.proxy.allow-x-forwarded`](#quarkus-vertx-http_quarkus-management-proxy-allow-x-forwarded) config_property_copy_button:+++quarkus.management.proxy.allow-x-forwarded+++[]<br><br>If either this or `allow-forwarded` are true and proxy address forwarding is enabled then the not standard `Forwarded` header will be used. In case the standard `Forwarded` header is enabled and detected on HTTP requests, the standard header has the precedence. Activating this together with `quarkus.http.proxy.allow-forwarded` has security implications as clients can forge requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected `Forwarded` or `X-Forwarded-*` headers from the client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_ALLOW_X_FORWARDED+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-management-proxy-strict-forwarded-control"></a> [`quarkus.management.proxy.strict-forwarded-control`](#quarkus-vertx-http_quarkus-management-proxy-strict-forwarded-control) config_property_copy_button:+++quarkus.management.proxy.strict-forwarded-control+++[]<br><br>When both Forwarded and X-Forwarded headers are enabled with `allow-forwarded` and `allow-x-forwarded` respectively, enforce that the identical headers must have equal values.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_STRICT_FORWARDED_CONTROL+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-forwarded-precedence"></a> [`quarkus.management.proxy.forwarded-precedence`](#quarkus-vertx-http_quarkus-management-proxy-forwarded-precedence) config_property_copy_button:+++quarkus.management.proxy.forwarded-precedence+++[]<br><br>When both Forwarded and X-Forwarded headers are enabled with `allow-forwarded` and `allow-x-forwarded` respectively, and `strict-forwarded-control` enforcing that the identical headers must have equal values is disabled, choose if it is Forwarded or X-Forwarded matching header value that is preferred.<br><br>For example, if Forwarded has a precedence over X-Forwarded, Forwarded scheme is `http` and X-Forwarded scheme is `https`, then the final scheme value is `http`. If X-Forwarded has a precedence, then the final scheme value is 'https'.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_FORWARDED_PRECEDENCE+++[] | `forwarded`, `x-forwarded` | `forwarded` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-enable-forwarded-host"></a> [`quarkus.management.proxy.enable-forwarded-host`](#quarkus-vertx-http_quarkus-management-proxy-enable-forwarded-host) config_property_copy_button:+++quarkus.management.proxy.enable-forwarded-host+++[]<br><br>Enable override the received request's host through a forwarded host header.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_ENABLE_FORWARDED_HOST+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-forwarded-host-header"></a> [`quarkus.management.proxy.forwarded-host-header`](#quarkus-vertx-http_quarkus-management-proxy-forwarded-host-header) config_property_copy_button:+++quarkus.management.proxy.forwarded-host-header+++[]<br><br>Configure the forwarded host header to be used if override enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_FORWARDED_HOST_HEADER+++[] | string | `X-Forwarded-Host` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-enable-forwarded-prefix"></a> [`quarkus.management.proxy.enable-forwarded-prefix`](#quarkus-vertx-http_quarkus-management-proxy-enable-forwarded-prefix) config_property_copy_button:+++quarkus.management.proxy.enable-forwarded-prefix+++[]<br><br>Enable prefix the received request's path with a forwarded prefix header.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_ENABLE_FORWARDED_PREFIX+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-forwarded-prefix-header"></a> [`quarkus.management.proxy.forwarded-prefix-header`](#quarkus-vertx-http_quarkus-management-proxy-forwarded-prefix-header) config_property_copy_button:+++quarkus.management.proxy.forwarded-prefix-header+++[]<br><br>Configure the forwarded prefix header to be used if prefixing enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_FORWARDED_PREFIX_HEADER+++[] | string | `X-Forwarded-Prefix` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-enable-trusted-proxy-header"></a> [`quarkus.management.proxy.enable-trusted-proxy-header`](#quarkus-vertx-http_quarkus-management-proxy-enable-trusted-proxy-header) config_property_copy_button:+++quarkus.management.proxy.enable-trusted-proxy-header+++[]<br><br>Adds the header `X-Forwarded-Trusted-Proxy` if the request is forwarded by a trusted proxy. The value is `true` if the request is forwarded by a trusted proxy, otherwise `null`.<br><br>The forwarded parser detects forgery attempts and if the incoming request contains this header, it will be removed from the request.<br><br>The `X-Forwarded-Trusted-Proxy` header is a custom header, not part of the standard `Forwarded` header.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_ENABLE_TRUSTED_PROXY_HEADER+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-trusted-proxies"></a> [`quarkus.management.proxy.trusted-proxies`](#quarkus-vertx-http_quarkus-management-proxy-trusted-proxies) config_property_copy_button:+++quarkus.management.proxy.trusted-proxies+++[]<br><br>Configure the list of trusted proxy addresses. Received `Forwarded`, `X-Forwarded` or `X-Forwarded-*` headers from any other proxy address will be ignored. The trusted proxy address should be specified as the IP address (IPv4 or IPv6), hostname or Classless Inter-Domain Routing (CIDR) notation. Please note that Quarkus needs to perform DNS lookup for all hostnames during the request. For that reason, using hostnames is not recommended.<br><br>Examples of a socket address in the form of `host` or `host:port`:<br><br>\* `127.0.0.1:8084` \* `[0:0:0:0:0:0:0:1]` \* `[0:0:0:0:0:0:0:1]:8084` \* `[::]` \* `localhost` \* `localhost:8084`<br><br>Examples of a CIDR notation:<br><br>\* `::/128` \* `::/0` \* `127.0.0.0/8`<br><br>Please bear in mind that IPv4 CIDR won't match request sent from the IPv6 address and the other way around.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_TRUSTED_PROXIES+++[] | list of TrustedProxyCheckPart | `All proxy addresses are trusted` |
| <a id="quarkus-vertx-http_quarkus-management-proxy-trusted-proxy-i-subject-dn"></a> [`quarkus.management.proxy.trusted-proxy[i].subject-dn`](#quarkus-vertx-http_quarkus-management-proxy-trusted-proxy-i-subject-dn) config_property_copy_button:+++quarkus.management.proxy.trusted-proxy[i].subject-dn+++\[\]<br><br>The trusted proxy's Subject Distinguished Name (DN) in RFC 2253 format. The proxy is trusted when its TLS client certificate's Subject DN contains all the components specified in this value. For example, configuring `CN=my-proxy,O=MyOrg` matches a certificate with Subject DN `CN=my-proxy,O=MyOrg,C=US`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_TRUSTED_PROXY_I__SUBJECT_DN+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-management-proxy-trusted-proxy-i-truststore-alias"></a> [`quarkus.management.proxy.trusted-proxy[i].truststore-alias`](#quarkus-vertx-http_quarkus-management-proxy-trusted-proxy-i-truststore-alias) config_property_copy_button:+++quarkus.management.proxy.trusted-proxy[i].truststore-alias+++\[\]<br><br>Alias of a certificate entry in the TLS registry truststore used by the HTTP server. When set, the client's certificate chain is validated against the certificate stored under this alias.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_TRUSTED_PROXY_I__TRUSTSTORE_ALIAS+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-proxy-forwarded-proto-validation"></a> [`quarkus.management.proxy.forwarded-proto-validation`](#quarkus-vertx-http_quarkus-management-proxy-forwarded-proto-validation) config_property_copy_button:+++quarkus.management.proxy.forwarded-proto-validation+++[]<br><br>How to handle `Forwarded` proto and `X-Forwarded-Proto` header values that are not valid [URI schemes](https://datatracker.ietf.org/doc/html/rfc3986#section-3.1).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_PROXY_FORWARDED_PROTO_VALIDATION+++[] | tooltip:none[No validation — accept any proto value.], tooltip:reject[Reject proto values that do not conform to the link\:https\://datatracker.ietf.org/doc/html/rfc3986#section-3.1\\]URI scheme\] syntax with a `400 Bad Request` response.] | tooltip:reject[Reject proto values that do not conform to the <a href="https\://datatracker.ietf.org/doc/html/rfc3986#section-3.1">URI scheme</a> syntax with a {@code 400 Bad Request} response.] |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-enabled"></a> [`quarkus.management.auth.permission."permissions".enabled`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-enabled) config_property_copy_button:+++quarkus.management.auth.permission."permissions".enabled+++[]<br><br>Determines whether the entire permission set is enabled, or not.<br><br>By default, if the permission set is defined, it is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__ENABLED+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-policy"></a> [`quarkus.management.auth.permission."permissions".policy`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-policy) config_property_copy_button:+++quarkus.management.auth.permission."permissions".policy+++[]<br><br>The HTTP policy that this permission set is linked to.<br><br>There are three built-in policies: permit, deny and authenticated. Role based policies can be defined, and extensions can add their own policies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__POLICY+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-methods"></a> [`quarkus.management.auth.permission."permissions".methods`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-methods) config_property_copy_button:+++quarkus.management.auth.permission."permissions".methods+++[]<br><br>The methods that this permission set applies to. If this is not set then they apply to all methods.<br><br>Note that if a request matches any path from any permission set, but does not match the constraint due to the method not being listed then the request will be denied.<br><br>Method specific permissions take precedence over matches that do not have any methods set.<br><br>This means that for example if Quarkus is configured to allow GET and POST requests to /admin to and no other permissions are configured PUT requests to /admin will be denied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-paths"></a> [`quarkus.management.auth.permission."permissions".paths`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-paths) config_property_copy_button:+++quarkus.management.auth.permission."permissions".paths+++[]<br><br>The paths that this permission check applies to. If the path ends in /++\*++ then this is treated as a path prefix, otherwise it is treated as an exact match.<br><br>Matches are done on a length basis, so the most specific path match takes precedence.<br><br>If multiple permission sets match the same path then explicit methods matches take precedence over matches without methods set, otherwise the most restrictive permissions are applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__PATHS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-auth-mechanism"></a> [`quarkus.management.auth.permission."permissions".auth-mechanism`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-auth-mechanism) config_property_copy_button:+++quarkus.management.auth.permission."permissions".auth-mechanism+++[]<br><br>One or more path specific authentication mechanisms separated by comma. Every listed value needs to match `io.quarkus.vertx.http.runtime.security.HttpCredentialTransport` authentication scheme such as 'basic', 'bearer', 'form', etc. By default, only one of the matching authentication mechanisms will produce a `SecurityIdentity`, and all matching authentication mechanisms will attempt to authenticate when an inclusive authentication is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__AUTH_MECHANISM+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-shared"></a> [`quarkus.management.auth.permission."permissions".shared`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-shared) config_property_copy_button:+++quarkus.management.auth.permission."permissions".shared+++[]<br><br>Indicates that this policy always applies to the matched paths in addition to the policy with a winning path. Avoid creating more than one shared policy to minimize the performance impact.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__SHARED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-auth-permission-permissions-applies-to"></a> [`quarkus.management.auth.permission."permissions".applies-to`](#quarkus-vertx-http_quarkus-management-auth-permission-permissions-applies-to) config_property_copy_button:+++quarkus.management.auth.permission."permissions".applies-to+++[]<br><br>Whether permission check should be applied on all matching paths, or paths specific for the Jakarta REST resources.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_PERMISSION__PERMISSIONS__APPLIES_TO+++[] | tooltip:all[Apply on all matching paths.], tooltip:jaxrs[Declares that a permission check must only be applied on the Jakarta REST request paths. Use this option to delay the permission check if an authentication mechanism is chosen with an annotation on the matching Jakarta REST endpoint. This option must be set if the following REST endpoint annotations are used\:   * `io.quarkus.oidc.Tenant` annotation which selects an OIDC authentication mechanism with a tenant identifier  * `io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication` which selects the Basic authentication mechanism  * `io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication` which selects the Form-based authentication mechanism  * `io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication` which selects the mTLS authentication mechanism  * `io.quarkus.security.webauthn.WebAuthn` which selects the WebAuth authentication mechanism  * `io.quarkus.oidc.BearerTokenAuthentication` which selects the OpenID Connect Bearer token authentication mechanism  * `io.quarkus.oidc.AuthorizationCodeFlow` which selects the OpenID Connect Code authentication mechanism] | tooltip:all[Apply on all matching paths.] |
| <a id="quarkus-vertx-http_quarkus-management-auth-policy-role-policy-roles-allowed"></a> [`quarkus.management.auth.policy."role-policy".roles-allowed`](#quarkus-vertx-http_quarkus-management-auth-policy-role-policy-roles-allowed) config_property_copy_button:+++quarkus.management.auth.policy."role-policy".roles-allowed+++[]<br><br>The roles that are allowed to access resources protected by this policy. By default, access is allowed to any authenticated user.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_POLICY__ROLE_POLICY__ROLES_ALLOWED+++[] | list of string | `**` |
| <a id="quarkus-vertx-http_quarkus-management-auth-policy-role-policy-roles-role-name"></a> [`quarkus.management.auth.policy."role-policy".roles."role-name"`](#quarkus-vertx-http_quarkus-management-auth-policy-role-policy-roles-role-name) config_property_copy_button:+++quarkus.management.auth.policy."role-policy".roles."role-name"+++[]<br><br>Add roles granted to the `SecurityIdentity` based on the roles that the `SecurityIdentity` already have. For example, the Quarkus OIDC extension can map roles from the verified JWT access token, and you may want to remap them to a deployment specific roles.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_POLICY__ROLE_POLICY__ROLES__ROLE_NAME_+++[] | Map\<String,List\<String>> |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-policy-role-policy-permissions-role-name"></a> [`quarkus.management.auth.policy."role-policy".permissions."role-name"`](#quarkus-vertx-http_quarkus-management-auth-policy-role-policy-permissions-role-name) config_property_copy_button:+++quarkus.management.auth.policy."role-policy".permissions."role-name"+++[]<br><br>Permissions granted to the `SecurityIdentity` if this policy is applied successfully (the policy allows request to proceed) and the authenticated request has required role. For example, you can map permission `perm1` with actions `action1` and `action2` to role `admin` by setting `quarkus.http.auth.policy.role-policy1.permissions.admin=perm1:action1,perm1:action2` configuration property. Granted permissions are used for authorization with the `@PermissionsAllowed` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_POLICY__ROLE_POLICY__PERMISSIONS__ROLE_NAME_+++[] | Map\<String,List\<String>> |  |
| <a id="quarkus-vertx-http_quarkus-management-auth-policy-role-policy-permission-class"></a> [`quarkus.management.auth.policy."role-policy".permission-class`](#quarkus-vertx-http_quarkus-management-auth-policy-role-policy-permission-class) config_property_copy_button:+++quarkus.management.auth.policy."role-policy".permission-class+++[]<br><br>Permissions granted by this policy will be created with a `java.security.Permission` implementation specified by this configuration property. The permission class must declare exactly one constructor that accepts permission name (`String`) or permission name and actions (`String`, `String[]`). Permission class must be registered for reflection if you run your application in a native mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_AUTH_POLICY__ROLE_POLICY__PERMISSION_CLASS+++[] | string | `io.quarkus.security.StringPermission` |
| <a id="quarkus-vertx-http_quarkus-management-header-header-path"></a> [`quarkus.management.header."header".path`](#quarkus-vertx-http_quarkus-management-header-header-path) config_property_copy_button:+++quarkus.management.header."header".path+++[]<br><br>The path this header should be applied<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HEADER__HEADER__PATH+++[] | string | `/*` |
| <a id="quarkus-vertx-http_quarkus-management-header-header-value"></a> [`quarkus.management.header."header".value`](#quarkus-vertx-http_quarkus-management-header-header-value) config_property_copy_button:+++quarkus.management.header."header".value+++[]<br><br>The value for this header configuration<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HEADER__HEADER__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-management-header-header-methods"></a> [`quarkus.management.header."header".methods`](#quarkus-vertx-http_quarkus-management-header-header-methods) config_property_copy_button:+++quarkus.management.header."header".methods+++[]<br><br>The HTTP methods for this header configuration.<br><br>If no HTTP methods are specified, the header will be always added.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HEADER__HEADER__METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-filter-filter-matches"></a> [`quarkus.management.filter."filter".matches`](#quarkus-vertx-http_quarkus-management-filter-filter-matches) config_property_copy_button:+++quarkus.management.filter."filter".matches+++[]<br><br>A regular expression for the paths matching this configuration<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_FILTER__FILTER__MATCHES+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-management-filter-filter-header-header-name"></a> [`quarkus.management.filter."filter".header."header-name"`](#quarkus-vertx-http_quarkus-management-filter-filter-header-header-name) config_property_copy_button:+++quarkus.management.filter."filter".header."header-name"+++[]<br><br>Additional HTTP Headers always sent in the response<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_FILTER__FILTER__HEADER__HEADER_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-vertx-http_quarkus-management-filter-filter-methods"></a> [`quarkus.management.filter."filter".methods`](#quarkus-vertx-http_quarkus-management-filter-filter-methods) config_property_copy_button:+++quarkus.management.filter."filter".methods+++[]<br><br>The HTTP methods for this path configuration<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_FILTER__FILTER__METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-filter-filter-order"></a> [`quarkus.management.filter."filter".order`](#quarkus-vertx-http_quarkus-management-filter-filter-order) config_property_copy_button:+++quarkus.management.filter."filter".order+++[]<br><br>Order in which this path config is applied. Higher priority takes precedence<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_FILTER__FILTER__ORDER+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-key-alias"></a> <mark>link:#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-key-alias\[`quarkus.management.ssl.certificate.key-store-key-alias`\]</mark> config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-key-alias+++[]<br><br>*This property is deprecated: Use `key-store-alias` instead.*<br><br>An optional parameter to select a specific key in the keystore. When SNI is disabled, and the keystore contains multiple keys and no alias is specified; the behavior is undefined.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_KEY_ALIAS+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-key-password"></a> <mark>link:#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-key-password\[`quarkus.management.ssl.certificate.key-store-key-password`\]</mark> config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-key-password+++[]<br><br>*This property is deprecated: Use `key-store-alias-password` instead.*<br><br>An optional parameter to define the password for the key, in case it is different from `key-store-password` If not given, it might be retrieved from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_KEY_PASSWORD+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-key-password-key"></a> <mark>link:#quarkus-vertx-http_quarkus-management-ssl-certificate-key-store-key-password-key\[`quarkus.management.ssl.certificate.key-store-key-password-key`\]</mark> config_property_copy_button:+++quarkus.management.ssl.certificate.key-store-key-password-key+++[]<br><br>*This property is deprecated: Use `key-store-alias-password-key` instead.*<br><br>A parameter to specify a `CredentialsProvider` property key, which can be used to get the password for the alias from `CredentialsProvider`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_SSL_CERTIFICATE_KEY_STORE_KEY_PASSWORD_KEY+++[] | string |  |
| <a id="quarkus-vertx-http_section_quarkus-management-cors"></a> [The CORS config](#quarkus-vertx-http_section_quarkus-management-cors) | Type | Default |
| <a id="quarkus-vertx-http_quarkus-management-cors-enabled"></a> [`quarkus.management.cors.enabled`](#quarkus-vertx-http_quarkus-management-cors-enabled) config_property_copy_button:+++quarkus.management.cors.enabled+++[]<br><br>Enable the CORS filter.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-management-cors-origins"></a> [`quarkus.management.cors.origins`](#quarkus-vertx-http_quarkus-management-cors-origins) config_property_copy_button:+++quarkus.management.cors.origins+++[]<br><br>The origins allowed for CORS.<br><br>A comma-separated list of valid URLs, such as `http://www.quarkus.io,http://localhost:3000`. URLs enclosed in forward slashes are interpreted as regular expressions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_ORIGINS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-cors-methods"></a> [`quarkus.management.cors.methods`](#quarkus-vertx-http_quarkus-management-cors-methods) config_property_copy_button:+++quarkus.management.cors.methods+++[]<br><br>The HTTP methods allowed for CORS requests.<br><br>A comma-separated list of valid HTTP methods, such as `GET,PUT,POST`. If not set, the filter allows any HTTP method by default.<br><br>This property is not used to control same-origin HTTP request methods.<br><br>Default: Any HTTP request method is allowed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-cors-headers"></a> [`quarkus.management.cors.headers`](#quarkus-vertx-http_quarkus-management-cors-headers) config_property_copy_button:+++quarkus.management.cors.headers+++[]<br><br>The HTTP headers allowed for CORS requests.<br><br>A comma-separated list of valid headers, such as `X-Custom,Content-Disposition`. If not set, the filter allows any header by default.<br><br>Default: Any HTTP request header is allowed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_HEADERS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-cors-exposed-headers"></a> [`quarkus.management.cors.exposed-headers`](#quarkus-vertx-http_quarkus-management-cors-exposed-headers) config_property_copy_button:+++quarkus.management.cors.exposed-headers+++[]<br><br>The HTTP headers exposed in CORS responses.<br><br>A comma-separated list of headers to expose, such as `X-Custom,Content-Disposition`.<br><br>Default: No headers are exposed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_EXPOSED_HEADERS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-management-cors-access-control-max-age"></a> [`quarkus.management.cors.access-control-max-age`](#quarkus-vertx-http_quarkus-management-cors-access-control-max-age) config_property_copy_button:+++quarkus.management.cors.access-control-max-age+++[]<br><br>The `Access-Control-Max-Age` response header value in `java.time.Duration` format.<br><br>Informs the browser how long it can cache the results of a preflight request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_ACCESS_CONTROL_MAX_AGE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-management) |  |
| <a id="quarkus-vertx-http_quarkus-management-cors-access-control-allow-credentials"></a> [`quarkus.management.cors.access-control-allow-credentials`](#quarkus-vertx-http_quarkus-management-cors-access-control-allow-credentials) config_property_copy_button:+++quarkus.management.cors.access-control-allow-credentials+++[]<br><br>The `Access-Control-Allow-Credentials` response header.<br><br>Tells browsers if front-end JavaScript can be allowed to access credentials when the request's credentials mode, `Request.credentials`, is set to `include`.<br><br>Default: `true` if the `quarkus.http.cors.origins` property is set and matches the precise `Origin` header value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_ACCESS_CONTROL_ALLOW_CREDENTIALS+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-management-cors-return-exact-origins"></a> [`quarkus.management.cors.return-exact-origins`](#quarkus-vertx-http_quarkus-management-cors-return-exact-origins) config_property_copy_button:+++quarkus.management.cors.return-exact-origins+++[]<br><br>Whether to return the exact request origin in the `Access-Control-Allow-Origin` response header.<br><br>When set to `true` (the default), the filter echoes back the request's `Origin` header value. When set to `false` and `origins` is configured as `*`, the response will contain the literal `Access-Control-Allow-Origin: *` instead of echoing the request origin. This is useful for public APIs where HTTP caching is important, as echoing the origin requires `Vary: Origin` and defeats shared caches.<br><br>Note: When `*` is returned as the origin, `Access-Control-Allow-Credentials` is forced to `false` per the CORS specification.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_RETURN_EXACT_ORIGINS+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-management-cors-vary-origin"></a> [`quarkus.management.cors.vary-origin`](#quarkus-vertx-http_quarkus-management-cors-vary-origin) config_property_copy_button:+++quarkus.management.cors.vary-origin+++[]<br><br>Whether to add the `Vary: Origin` response header to CORS responses when the origin is not a wildcard.<br><br>The `Vary: Origin` header tells HTTP caches that the response depends on the request's `Origin` header, preventing a cached response for one origin from being served to a different origin.<br><br>This should remain enabled (the default) in most setups, especially when a reverse proxy or CDN may cache responses.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_CORS_VARY_ORIGIN+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_section_quarkus-management-host-validation"></a> [The HTTP Host header validation](#quarkus-vertx-http_section_quarkus-management-host-validation) | Type | Default |
| <a id="quarkus-vertx-http_quarkus-management-host-validation-require-localhost"></a> [`quarkus.management.host-validation.require-localhost`](#quarkus-vertx-http_quarkus-management-host-validation-require-localhost) config_property_copy_button:+++quarkus.management.host-validation.require-localhost+++[]<br><br>Require that HTTP Host authority can only contain valid localhost names such as "localhost", "127.0.0.1" or "++\[++::1++\]++".<br><br>This requirement is enforced in production and dev modes, when neither this nor the `allowed-hosts()` properties are configured and the "quarkus.http.host" property has a valid localhost name. Set this property to `false` if it is not required.<br><br>Note this property is mutually exclusive with the `allowed-hosts()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HOST_VALIDATION_REQUIRE_LOCALHOST+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-management-host-validation-allowed-hosts"></a> [`quarkus.management.host-validation.allowed-hosts`](#quarkus-vertx-http_quarkus-management-host-validation-allowed-hosts) config_property_copy_button:+++quarkus.management.host-validation.allowed-hosts+++[]<br><br>Allowed hosts.<br><br>A comma-separated set of hosts, for example: "localhost", "quarkus.io".<br><br>Note this property is mutually exclusive with the `require-localhost()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HOST_VALIDATION_ALLOWED_HOSTS+++[] | list of string |  |

<a id="duration-note-anchor-quarkus-vertx-http_quarkus-management"></a>

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

<a id="memory-size-note-anchor-quarkus-vertx-http_quarkus-management"></a>

> [!NOTE]
> **About the MemorySize format**
>
> A size configuration option recognizes strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.
>
> If no suffix is given, assume bytes.

<a id="reverse-proxy"></a>
## Running behind a reverse proxy

Quarkus can be accessed through proxies that generate headers (e.g. `X-Forwarded-Host`) to preserve information about the original request. Quarkus can be configured to automatically update information like protocol, host, port and URI to use the values from those headers.

> [!IMPORTANT]
> Activating this feature can expose the server to security issues like information spoofing. Activate it only when running behind a reverse proxy.

To set up this feature for the management interface, include the following lines in `src/main/resources/application.properties`:

```properties
quarkus.management.proxy.proxy-address-forwarding=true
```

To constrain this behavior to the standard `Forwarded` header (and ignore `X-Forwarded` variants) by setting `quarkus.management.proxy.allow-forwarded` in `src/main/resources/application.properties`:

```properties
quarkus.management.proxy.allow-forwarded=true
```

Alternatively, you can prefer `X-Forwarded-*` headers using the following configuration in `src/main/resources/application.properties` (note `allow-x-forwarded` instead of `allow-forwarded`):

```properties
quarkus.management.proxy.proxy-address-forwarding=true
quarkus.management.proxy.allow-x-forwarded=true
quarkus.management.proxy.enable-forwarded-host=true
quarkus.management.proxy.enable-forwarded-prefix=true
```

Supported forwarding address headers are:

- `Forwarded`
- `X-Forwarded-Proto`
- `X-Forwarded-Host`
- `X-Forwarded-Port`
- `X-Forwarded-Ssl`
- `X-Forwarded-Prefix`

If both header variants (`Forwarded` and `X-Forwarded-*`) are enabled, the `Forwarded` header will have precedence.

> [!IMPORTANT]
> Using both `Forwarded` and `X-Forwarded` headers can have security implications as it may allow clients to forge requests with a header that is not overwritten by the proxy.

Ensure that your proxy is configured to strip unexpected `Forwarded` or `X-Forwarded-*` headers from the client request.

## Kubernetes

When Quarkus generates the Kubernetes metadata, it checks if the management interface is enabled and configures the probes accordingly. The resulting descriptor defines the main HTTP port (named `http`) and the management port (named `management`). Health probes (using HTTP actions) and Prometheus scrape URLs are configured using the `management` port.

> [!IMPORTANT]
> **KNative**
>
> Until [KNative#8471](https://github.com/knative/serving/issues/8471) is resolved, you cannot use the management interface, as KNative does not support containers will multiple exposed ports.

## CORS filter

To make your Quarkus management interface accessible to another application running on a different domain, you need to configure cross-origin resource sharing (CORS). For more information about the CORS filter that Quarkus provides, see the Quarkus [CORS filter](/guides/security-cors/index.md#cors-filter) section of the "Cross-origin resource sharing" guide.

The management interface uses the same CORS filter mechanism as the main HTTP server, but with separate configuration under `quarkus.management.cors.*`.

For example, to allow requests from a specific origin:

```properties
quarkus.management.enabled=true
quarkus.management.cors.enabled=true
quarkus.management.cors.origins=http://example.com
```

When CORS is enabled without configuring specific origins, only same-origin requests are allowed.

## HTTP Host header validation

The management interface supports the same HTTP `Host` header validation as the main HTTP server, but with separate configuration under `quarkus.management.host-validation.*`.

When `quarkus.management.host` is set to a localhost name (`localhost`, `127.0.0.1`, or `[::1]`), host validation is automatically enabled in **dev** and **production** modes.

> [!WARNING]
> This automatic behavior is a change from previous Quarkus versions. If the management interface is accessed through a reverse proxy or a non-localhost host name, configure the allowed hosts explicitly or disable automatic validation.

For example, to restrict requests to specific host names:

```properties
quarkus.management.enabled=true
quarkus.management.host-validation.allowed-hosts=management.example.com
```

To disable automatic localhost validation:

```properties
quarkus.management.enabled=true
quarkus.management.host-validation.require-localhost=false
```

For more details on the available options and reverse proxy considerations, see the [HTTP Host header validation](/guides/http-reference/index.md#host-header-validation) section in the HTTP reference guide.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-management-host-validation_quarkus-management-host-validation-require-localhost"></a> [`quarkus.management.host-validation.require-localhost`](#quarkus-vertx-http_quarkus-management-host-validation_quarkus-management-host-validation-require-localhost) config_property_copy_button:+++quarkus.management.host-validation.require-localhost+++[]<br><br>Require that HTTP Host authority can only contain valid localhost names such as "localhost", "127.0.0.1" or "++\[++::1++\]++".<br><br>This requirement is enforced in production and dev modes, when neither this nor the `allowed-hosts()` properties are configured and the "quarkus.http.host" property has a valid localhost name. Set this property to `false` if it is not required.<br><br>Note this property is mutually exclusive with the `allowed-hosts()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HOST_VALIDATION_REQUIRE_LOCALHOST+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-management-host-validation_quarkus-management-host-validation-allowed-hosts"></a> [`quarkus.management.host-validation.allowed-hosts`](#quarkus-vertx-http_quarkus-management-host-validation_quarkus-management-host-validation-allowed-hosts) config_property_copy_button:+++quarkus.management.host-validation.allowed-hosts+++[]<br><br>Allowed hosts.<br><br>A comma-separated set of hosts, for example: "localhost", "quarkus.io".<br><br>Note this property is mutually exclusive with the `require-localhost()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_MANAGEMENT_HOST_VALIDATION_ALLOWED_HOSTS+++[] | list of string |  |

## Security

Security for the management endpoints exposed in the separate HTTP server needs to be enabled explicitly like in the example below:

```properties
quarkus.management.enabled=true
quarkus.management.auth.enabled=true
```

Once enabled, you can use same authentication mechanism you have already configured for the main server, or use a different one. All of these mechanisms are detailed in the [Authentication mechanisms in Quarkus](/guides/security-authentication-mechanisms/index.md) guide.

### Use HTTP Security Policy to enable path-based authentication

The following configuration example demonstrates how you can enforce a single selectable authentication mechanism for a given request path:

```properties
quarkus.management.auth.permission.metrics.paths=/q/metrics/*
quarkus.management.auth.permission.metrics.policy=authenticated
quarkus.management.auth.permission.metrics.auth-mechanism=basic <1>

quarkus.management.auth.permission.health.paths=/q/health/*
quarkus.management.auth.permission.health.policy=authenticated
quarkus.management.auth.permission.health.auth-mechanism=bearer <2>
```

1. The metric endpoints will be only accessible with the [Basic authentication](#basic-auth).
2. If the Quarkus OIDC extension is present, the health endpoints will be authenticated by the [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md).

> [!IMPORTANT]
> The `quarkus.management.root-path` property is prepended to the configured policy paths that do not start with a forward slash, in the same way as the `quarkus.http.root-path` property is prepended to the main HTTP server policy paths. For more information, see the [Permission paths and HTTP root path](/guides/security-authorize-web-endpoints-reference/index.md#permission-paths-and-root-paths) section of the Authorization of web endpoints guide.

<a id="basic-auth"></a>
### Basic authentication

You can enable *basic* authentication using the following properties:

```properties
quarkus.management.enabled=true
# Enable basic authentication
quarkus.management.auth.basic=true
# Require all access to /q/* to be authenticated
quarkus.management.auth.permission.all.policy=authenticated
quarkus.management.auth.permission.all.paths=/q/*
```

You can also use different permissions for different paths or use role bindings:

```properties
quarkus.management.enabled=true
# Enable basic authentication
quarkus.management.auth.basic=true
# Configure a management policy if needed, here the policy `management-policy` requires users to have the role `management`.
quarkus.management.auth.policy.management-policy.roles-allowed=management

# For each endpoint you can configure the permissions
# Health used the management-policy (so requires authentication + the `management` role)
quarkus.management.auth.permission.health.paths=/q/health/*
quarkus.management.auth.permission.health.policy=management-policy

# Metrics just requires authentication
quarkus.management.auth.permission.metrics.paths=/q/metrics/*
quarkus.management.auth.permission.metrics.policy=authenticated
```

More details about Basic authentication in Quarkus can be found in the [Basic authentication guide](/guides/security-basic-authentication-howto/index.md).

## Injecting management URL in tests

When testing your application, you can inject the management URL using the `@TestHTTPResource` annotation:

```java
@TestHTTPResource(value="/management", management=true)
URL management;
```

The `management` attribute is set to `true` to indicate that the injected URL is for the management interface. The `context-root` is automatically added. Thus, in the previous example, the injected URL is `http://localhost:9001/q/management`.

`@TestHTTPResource` is particularly useful when setting the management `test-port` to 0, which indicates that the system will assign a random port to the management interface:

----\] quarkus.management.test-port=0

```

```
