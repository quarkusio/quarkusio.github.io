# HTTP reference

This document clarifies different HTTP functionalities available in Quarkus.

Eclipse Vert.x supplies the fundamental HTTP layer. For Servlet support, Quarkus employs a customized Undertow version that operates on top of Vert.x, while Jakarta REST support is delivered through RESTEasy.

When Undertow is present, RESTEasy functions as a Servlet filter. In its absence, RESTEasy operates directly on Vert.x without involving Servlets.

## Serving static resources

If you are looking to use Quarkus for a web application, look at the [Quarkus for the Web](/guides/web/index.md) guide.

### From the application jar

To serve static resources from the application jar, you must place them in the `META-INF/resources` directory of your application. This location was chosen as it is the standard location for resources in `jar` files as defined by the Servlet spec. Even though Quarkus can be used without Servlet, following this convention allows existing code that places its resources in this location to function correctly.

### From web dependencies like webjars or mvnpm

Look at the [Web dependency locator](/guides/web-dependency-locator/index.md) guide for details on how to use [WebJars](https://www.webjars.org), [mvnpm](https://mvnpm.org) and [importmaps](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script/type/importmap)

### From a local directory

Static resources can be served from a local directory by installing an additional route in the Vert.x router.

For instance, to serve resources from the `static/` directory relative to the current path at <http://localhost:8080/static/>, you can install the following route:

```java
package org.acme;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import jakarta.enterprise.event.Observes;


public class StaticResources {

    void installRoute(@Observes StartupEvent startupEvent, Router router) {
        router.route()
                .path("/static/*")
                .handler(StaticHandler.create("static/"));
    }
}
```

### Local filesystem static resources

> [!NOTE]
> Serving static resources from a local filesystem directory is enabled when `quarkus.http.static-dir.path` is set. The `quarkus.http.static-dir.enabled` property can be used to explicitly disable this feature.

Quarkus can serve static files from a local filesystem directory, outside of the application JAR and Java resources. This is configured via the `quarkus.http.static-dir.*` properties.

To expose the contents of a `static/` directory:

```properties
quarkus.http.static-dir.path=static
```

By default, static files are exposed under the `/static` endpoint.

With this configuration, a file located at `static/picture.jpg` will be available at: `http://localhost:8080/static/picture.jpg`.

To change the default endpoint:

```properties
quarkus.http.static-dir.endpoint=/
```

With this configuration, a file located at `static/picture.jpg` will be available at: `http://localhost:8080/picture.jpg`.

The `quarkus.http.static-dir.endpoint` value must not contain `*` or `..`, and it will be normalized to avoid trailing slashes. Similarly, `quarkus.http.static-dir.path` must be a relative path without `..`. Quarkus validates these values at build time.

This feature can be used in both development and production environments.

### HTTP Compression

The response body of a static resource is not compressed by default. You can enable the HTTP compression support by means of `quarkus.http.enable-compression=true`. If compression support is enabled then the response body is compressed if the `Content-Type` header derived from the file name of a resource is a compressed media type as configured via `quarkus.http.compress-media-types`.

> [!TIP]
> By default, the following list of media types is compressed: `text/html`, `text/plain`, `text/xml`, `text/css`, `text/javascript`, `application/javascript`, `application/json`, `application/graphql+json` and `application/xhtml+xml`.

> [!NOTE]
> If the client does not indicate its support for HTTP compression in a request header, e.g. `Accept-Encoding: deflate, gzip, br`, then the response body is not compressed.

> [!TIP]
> Brotli compression is not available by default. You can enable it by setting `quarkus.http.compressors=deflate,gzip,br`. In case of building native image, it adds around 1MB to your executable size.

<a id="static-resources-config"></a>
### Other Configurations

Additionally, the index page for static resources can be changed from default `index.html`, the hidden files (e.g. dot files) can be indicated as not served, the range requests can be disabled, and the caching support (e.g. caching headers and file properties cache) can be configured.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-index-page"></a> [`quarkus.http.static-resources.index-page`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-index-page) config_property_copy_button:+++quarkus.http.static-resources.index-page+++[]<br><br>Set the index page when serving static resources.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_INDEX_PAGE+++[] | string | `index.html` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-include-hidden"></a> [`quarkus.http.static-resources.include-hidden`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-include-hidden) config_property_copy_button:+++quarkus.http.static-resources.include-hidden+++[]<br><br>Set whether hidden files should be served.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_INCLUDE_HIDDEN+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-enable-range-support"></a> [`quarkus.http.static-resources.enable-range-support`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-enable-range-support) config_property_copy_button:+++quarkus.http.static-resources.enable-range-support+++[]<br><br>Set whether range requests (resumable downloads; media streaming) should be enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_ENABLE_RANGE_SUPPORT+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-caching-enabled"></a> [`quarkus.http.static-resources.caching-enabled`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-caching-enabled) config_property_copy_button:+++quarkus.http.static-resources.caching-enabled+++[]<br><br>Set whether cache handling is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_CACHING_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-cache-entry-timeout"></a> [`quarkus.http.static-resources.cache-entry-timeout`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-cache-entry-timeout) config_property_copy_button:+++quarkus.http.static-resources.cache-entry-timeout+++[]<br><br>Set the cache entry timeout. The default is `30` seconds.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_CACHE_ENTRY_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-static-resources) | `30S` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-max-age"></a> [`quarkus.http.static-resources.max-age`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-max-age) config_property_copy_button:+++quarkus.http.static-resources.max-age+++[]<br><br>Set value for max age in caching headers. The default is `24` hours.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_MAX_AGE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-static-resources) | `24H` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-max-cache-size"></a> [`quarkus.http.static-resources.max-cache-size`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-max-cache-size) config_property_copy_button:+++quarkus.http.static-resources.max-cache-size+++[]<br><br>Set the max cache size.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_MAX_CACHE_SIZE+++[] | int | `10000` |
| <a id="quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-content-encoding"></a> [`quarkus.http.static-resources.content-encoding`](#quarkus-vertx-http_quarkus-http-static-resources_quarkus-http-static-resources-content-encoding) config_property_copy_button:+++quarkus.http.static-resources.content-encoding+++[]<br><br>Content encoding for text related files<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_STATIC_RESOURCES_CONTENT_ENCODING+++[] | [Charset](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/charset/Charset.html) | `UTF-8` |

<a id="duration-note-anchor-quarkus-vertx-http_quarkus-http-static-resources"></a>

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

<a id="context-path"></a>
## Configuring the Context path

By default, Quarkus will serve content from under the root context. If you want to change this you can use the `quarkus.http.root-path` config key to set the context path.

If you are using Servlet you can control the Servlet context path via `quarkus.servlet.context-path`. This item is relative to the http root above, and will only affect Servlet and things that run on top of Servlet. Most applications will want to use the HTTP root as this affects everything that Quarkus serves.

If both are specified then all non-Servlet web endpoints will be relative to `quarkus.http.root-path`, while Servlet's will be served relative to `{quarkus.http.root-path}/{quarkus.servlet.context-path}`.

If REST Assured is used for testing and `quarkus.http.root-path` is set then Quarkus will automatically configure the base URL for use in Quarkus tests, so test URL's should not include the root path.

In general, path configurations for web content are interpreted relative to `quarkus.http.root-path` (which is / by default).

- To specify paths within this context root, use a relative path that does not begin with a forward slash.

- If you want to specify the URI explicitly, so it is always the same regardless of the value of `quarkus.http.root-path`, use an absolute path that begins with a forward slash.

As an example, if an extension configures a `service` path, that endpoint will be served from `${quarkus.http.root-path}/service`. If you change the configuration of that path to `/service`, that endpoint will be served from `/service`.

The [Path Resolution in Quarkus](https://quarkus.io/blog/path-resolution-in-quarkus/) blog post further explains how path resolution works for both user and extension defined paths.

> [!IMPORTANT]
> **Management Interface**
>
> `quarkus.http.root-path` is only used for the main HTTP server. If you enabled the management interface (using the `quarkus.management.enabled=true` property), you can configure the root path of the management interface using: `quarkus.management.root-path`.
>
> Refer to the [management interface reference](/guides/management-interface-reference/index.md) for more information.

<a id="ssl"></a>
## Supporting secure connections with TLS/SSL

To have Quarkus support secure connections, you must either provide a certificate and associated key file, or supply a keystore.

In both cases, a password must be provided. See the designated paragraph for a detailed description of how to provide it.

> [!TIP]
> To enable TLS/SSL support with native executables, please refer to our [Using SSL With Native Executables guide](/guides/native-and-ssl/index.md).

### Using the TLS centralized configuration

Quarkus provides a [TLS centralized configuration](/guides/tls-registry-reference/index.md) that can be used to configure the server's TLS context. It is the recommended approach to configure HTTPS.

To configure the HTTP server to use HTTPS, you can use the following configuration:

```properties
quarkus.tls.key-store.pem.0.cert=server.crt
quarkus.tls.key-store.pem.0.key=server.key
quarkus.http.insecure-requests=disabled # Reject HTTP requests
```

To use a `p12` (PKCS12) key store, apply the following configuration:

```properties
quarkus.tls.key-store.p12.path=server-keystore.p12
quarkus.tls.key-store.p12.password=secret
quarkus.http.insecure-requests=disabled # Reject HTTP requests
```

Instead of the default configuration, you can use a named configuration:

```properties
quarkus.tls.https.key-store.p12.path=server-keystore.p12
quarkus.tls.https.key-store.p12.password=secret
quarkus.http.insecure-requests=disabled
quarkus.http.tls-configuration-name=https
```

### Configuring the HTTP server directly

If you do not want to use the TLS registry, you can configure the HTTP server directly.

If the certificate has not been loaded into a keystore, it can be provided directly using the properties listed below. Quarkus will first try to load the given files as resources, and uses the filesystem as a fallback. The certificate / key pair will be loaded into a newly created keystore on startup.

Your `application.properties` would then look like this:

```properties
quarkus.http.ssl.certificate.files=/path/to/certificate
quarkus.http.ssl.certificate.key-files=/path/to/key
```

An alternate solution is to directly provide a keystore which already contains a default entry with a certificate. You will need to at least provide the file and a password.

As with the certificate/key file combination, Quarkus will first try to resolve the given path as a resource, before attempting to read it from the filesystem.

Add the following property to your `application.properties`:

```properties
quarkus.http.ssl.certificate.key-store-file=/path/to/keystore
```

As an optional hint, the type of keystore can be provided as one of the options listed. If the type is not provided, Quarkus will try to deduce it from the file extensions.

```properties
quarkus.http.ssl.certificate.key-store-file-type=[one of JKS, JCEKS, P12, PKCS12, PFX]
```

In both aforementioned scenarios, a password needs to be provided to create/load the keystore with. The password can be set in your `application.properties` (in plain-text) using the following property:

```properties
quarkus.http.ssl.certificate.key-store-password=your-password
```

However, instead of providing the password as plain-text in the configuration file (which is considered bad practice), it can instead be supplied (using [MicroProfile Config](https://github.com/eclipse/microprofile-config)) as the environment variable `QUARKUS_HTTP_SSL_CERTIFICATE_KEY_STORE_PASSWORD`. This will also work in tandem with [Kubernetes secrets](https://kubernetes.io/docs/concepts/configuration/secret/#using-secrets-as-environment-variables).

*Note: To remain compatible with earlier versions of Quarkus (before 0.16) the default password is set to "password". It is therefore not a mandatory parameter!*

### Configure the HTTPS port

By default, Quarkus listens to port 8443 for SSL secured connections and 8444 when running tests.

These ports can be configured in your `application.properties` with the properties `quarkus.http.ssl-port` and `quarkus.http.test-ssl-port`.

### Disable the HTTP port

It is possible to disable the HTTP port and only support secure requests. This is done via the `quarkus.http.insecure-requests` property in `application.properties`. There are three possible values:

**`enabled`**\
The default, HTTP works as normal

**`redirect`**\
HTTP requests will be redirected to the HTTPS port

**`disabled`**\
The HTTP port will not be opened.

> [!NOTE]
> if you use `redirect` or `disabled` and have not added an SSL certificate or keystore, your server will not start!

### Reloading the certificates

Key store, trust store and certificate files can be reloaded periodically. Configure the `quarkus.http.ssl.certificate.reload-period` property to specify the interval at which the certificates should be reloaded:

```properties
quarkus.http.ssl.certificate.files=/mount/certs/tls.crt
quarkus.http.ssl.certificate.key-files=/mount/certs/tls.key
quarkus.http.ssl.certificate.reload-period=1h
```

The files are reloaded from the same location as they were initially loaded from. If there is no content change, the reloading is a no-op. It the reloading fails, the server will continue to use the previous certificates.

### Preventing client renegotiation

See [Preventing client renegotiation](/guides/tls-registry-reference/index.md#client-renegotiation) for more information.

## Additional HTTP Headers

To enable HTTP headers to be sent on every response, add the following properties:

```properties
quarkus.http.header."X-Content-Type-Options".value=nosniff
```

This will include the `X-Content-Type-Options: nosniff` HTTP Header on responses for requests performed on any resource in the application.

You can also specify a `path` pattern and the HTTP `methods` the header needs to be applied:

```properties
quarkus.http.header.Pragma.value=no-cache
quarkus.http.header.Pragma.path=/headers/pragma
quarkus.http.header.Pragma.methods=GET,HEAD
```

This will apply the `Pragma` header only when the `/headers/pragma` resource is called with a `GET` or a `HEAD` method

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-header_quarkus-http-header-header-path"></a> [`quarkus.http.header."header".path`](#quarkus-vertx-http_quarkus-http-header_quarkus-http-header-header-path) config_property_copy_button:+++quarkus.http.header."header".path+++[]<br><br>The path this header should be applied<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_HEADER__HEADER__PATH+++[] | string | `/*` |
| <a id="quarkus-vertx-http_quarkus-http-header_quarkus-http-header-header-value"></a> [`quarkus.http.header."header".value`](#quarkus-vertx-http_quarkus-http-header_quarkus-http-header-header-value) config_property_copy_button:+++quarkus.http.header."header".value+++[]<br><br>The value for this header configuration<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_HEADER__HEADER__VALUE+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-http-header_quarkus-http-header-header-methods"></a> [`quarkus.http.header."header".methods`](#quarkus-vertx-http_quarkus-http-header_quarkus-http-header-header-methods) config_property_copy_button:+++quarkus.http.header."header".methods+++[]<br><br>The HTTP methods for this header configuration.<br><br>If no HTTP methods are specified, the header will be always added.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_HEADER__HEADER__METHODS+++[] | list of string |  |

### Additional HTTP Headers per path

If you need different header values depending on the path, you can use the following configuration:

```properties
quarkus.http.filter.index.header."Cache-Control"=none
quarkus.http.filter.index.matches=/index.html
```

This will set the `Cache-Control` header to `none` when `/index.html` is called.

> [!IMPORTANT]
> The `index` after `quarkus.http.filter` in the key is used for grouping and (as an example) can be named as you like.

You can use a regular expression in the path and also specify the HTTP methods where the HTTP header will be set:

```properties
quarkus.http.filter.static.header."Cache-Control"=max-age=31536000
quarkus.http.filter.static.methods=GET,HEAD
quarkus.http.filter.static.matches=/static/.*
```

In case of overlapping paths in the configuration, you can specify an order (higher values take precedence). For example, having the following configuration:

```properties
quarkus.http.filter.just-order.order=10
quarkus.http.filter.just-order.header."Cache-Control"=max-age=5000
quarkus.http.filter.just-order.matches=/paths/order

quarkus.http.filter.any-order.order=11
quarkus.http.filter.any-order.header."Cache-Control"=max-age=1
quarkus.http.filter.any-order.matches=/paths/order.*
```

Will include the `Cache-Control: max-age=1` header when `/paths/order` is requested.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-matches"></a> [`quarkus.http.filter."filter".matches`](#quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-matches) config_property_copy_button:+++quarkus.http.filter."filter".matches+++[]<br><br>A regular expression for the paths matching this configuration<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_FILTER__FILTER__MATCHES+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-header-header-name"></a> [`quarkus.http.filter."filter".header."header-name"`](#quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-header-header-name) config_property_copy_button:+++quarkus.http.filter."filter".header."header-name"+++[]<br><br>Additional HTTP Headers always sent in the response<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_FILTER__FILTER__HEADER__HEADER_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-methods"></a> [`quarkus.http.filter."filter".methods`](#quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-methods) config_property_copy_button:+++quarkus.http.filter."filter".methods+++[]<br><br>The HTTP methods for this path configuration<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_FILTER__FILTER__METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-order"></a> [`quarkus.http.filter."filter".order`](#quarkus-vertx-http_quarkus-http-filter_quarkus-http-filter-filter-order) config_property_copy_button:+++quarkus.http.filter."filter".order+++[]<br><br>Order in which this path config is applied. Higher priority takes precedence<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_FILTER__FILTER__ORDER+++[] | int |  |

## Support 100-Continue in vert.x

To support `100-continue`, the `quarkus.http.handle-100-continue-automatically` option needs to be enabled explicitly For additional information check [100-continue](https://datatracker.ietf.org/doc/html/rfc7231#section-5.1.1) and the related [Vert.x documentation](https://vertx.io/docs/apidocs/io/vertx/core/http/HttpServerOptions.html#DEFAULT_HANDLE_100_CONTINE_AUTOMATICALLY).

```properties
quarkus.http.handle-100-continue-automatically=true
```

## HTTP/2 Support

HTTP/2 is enabled by default, and will be used by browsers if SSL is in use. Even if SSL is not in use HTTP/2 via cleartext upgrade is supported, and may be used by non-browser clients.

If you want to disable HTTP/2 you can set:

```properties
quarkus.http.http2=false
```

Note that some configuration attributes are specific to HTTP/2. For example, to configure the max header list size (\~ header), you need to configure the `quarkus.http.limits.max-header-list-size` attribute. You can also enable or disable HTTP/2 push using `quarkus.http.http2-push-enabled`.

## Listening on a Random Port

If you don't want to specify a port you can set `quarkus.http.port=0` or `quarkus.http.test-port=0`. A random open port will be picked by the OS, and a log message printed in the console.

To retrieve the real port, use the `io.quarkus.vertx.http.HttpServer` API, which can be injected into the application code as a CDI Bean:

```java
import jakarta.inject.Singleton;
import io.quarkus.vertx.http.HttpServer;

@Singleton
public class MyService  {
    @Inject
    HttpServer httpServer;

    public void connectToServer() {
        int port = httpServer.getPort();
    }
}
```

Or into any kind of test, either as a field or test parameter:

```java
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.http.HttpServer;

@QuarkusTest
class HttpServerTest {
    HttpServer httpServer;

    @Test
    void connectToServer(HttpServer httpServer) {
        int port = httpServer.getPort();
    }
}
```

To retrieve the `URI` that Quarkus is serving on, use `HttpServer#getLocalBaseUri()`. In tests, the URI can be directly injected with `@TestHTTPResource`:

```java
@TestHTTPResource
URL testUrl;
```

Every Quarkus component that requires the actual http port, like REST Assured, is automatically updated with the real port.

## CORS filter

To make your Quarkus application accessible to another application running on a different domain, you need to configure cross-origin resource sharing (CORS). For more information about the CORS filter that Quarkus provides, see the Quarkus [CORS filter](/guides/security-cors/index.md#cors-filter) section of the "Cross-origin resource sharing" guide.

<a id="host-header-validation"></a>
## HTTP Host header validation

Quarkus can validate the HTTP `Host` header on incoming requests to protect against DNS rebinding attacks and host header poisoning.

When `quarkus.http.host` is set to a localhost name (`localhost`, `127.0.0.1`, or `[::1]`), host validation is automatically enabled in **dev** and **production** modes. Only requests whose `Host` header matches one of the known localhost names are accepted; all others receive a `400 Bad Request` response.

> [!WARNING]
> This automatic behavior is a change from previous Quarkus versions, where no `Host` header validation was performed. If your application runs behind a reverse proxy that rewrites the `Host` header, you may need to either:
>
> - configure the allowed hosts explicitly with `quarkus.http.host-validation.allowed-hosts`, or
> - disable the automatic validation with `quarkus.http.host-validation.require-localhost=false`.

### Configuring allowed hosts

To restrict requests to a specific set of host names, use `allowed-hosts`:

```properties
quarkus.http.host-validation.allowed-hosts=example.com,api.example.com
```

The check is case-insensitive and only validates the host name, not the port.

### Requiring localhost only

To explicitly require that only localhost names are accepted:

```properties
quarkus.http.host-validation.require-localhost=true
```

To disable automatic localhost validation when the server is bound to a localhost address:

```properties
quarkus.http.host-validation.require-localhost=false
```

> [!NOTE]
> `allowed-hosts` and `require-localhost` are mutually exclusive. Setting both results in a configuration error.

### Reverse proxy considerations

When running behind a reverse proxy, enable proxy header forwarding so that Quarkus validates the forwarded host instead of the proxy's internal host name:

```properties
quarkus.http.proxy.proxy-address-forwarding=true
quarkus.http.proxy.allow-forwarded=true
quarkus.http.host-validation.allowed-hosts=public-api.example.com
```

This works with both the standard `Forwarded` header and the `X-Forwarded-Host` header.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-host-validation_quarkus-http-host-validation-require-localhost"></a> [`quarkus.http.host-validation.require-localhost`](#quarkus-vertx-http_quarkus-http-host-validation_quarkus-http-host-validation-require-localhost) config_property_copy_button:+++quarkus.http.host-validation.require-localhost+++[]<br><br>Require that HTTP Host authority can only contain valid localhost names such as "localhost", "127.0.0.1" or "++\[++::1++\]++".<br><br>This requirement is enforced in production and dev modes, when neither this nor the `allowed-hosts()` properties are configured and the "quarkus.http.host" property has a valid localhost name. Set this property to `false` if it is not required.<br><br>Note this property is mutually exclusive with the `allowed-hosts()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_HOST_VALIDATION_REQUIRE_LOCALHOST+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-http-host-validation_quarkus-http-host-validation-allowed-hosts"></a> [`quarkus.http.host-validation.allowed-hosts`](#quarkus-vertx-http_quarkus-http-host-validation_quarkus-http-host-validation-allowed-hosts) config_property_copy_button:+++quarkus.http.host-validation.allowed-hosts+++[]<br><br>Allowed hosts.<br><br>A comma-separated set of hosts, for example: "localhost", "quarkus.io".<br><br>Note this property is mutually exclusive with the `require-localhost()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_HOST_VALIDATION_ALLOWED_HOSTS+++[] | list of string |  |

## HTTP Limits Configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-header-size"></a> [`quarkus.http.limits.max-header-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-header-size) config_property_copy_button:+++quarkus.http.limits.max-header-size+++[]<br><br>The maximum length of all headers, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_HEADER_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits) | `20K` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-body-size"></a> [`quarkus.http.limits.max-body-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-body-size) config_property_copy_button:+++quarkus.http.limits.max-body-size+++[]<br><br>The maximum size of a request body, up to `Long.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits) | `10240K` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-chunk-size"></a> [`quarkus.http.limits.max-chunk-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-chunk-size) config_property_copy_button:+++quarkus.http.limits.max-chunk-size+++[]<br><br>The max HTTP chunk size, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_CHUNK_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits) | `8192` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-initial-line-length"></a> [`quarkus.http.limits.max-initial-line-length`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-initial-line-length) config_property_copy_button:+++quarkus.http.limits.max-initial-line-length+++[]<br><br>The maximum length of the initial line (e.g. `"GET / HTTP/1.0"`).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_INITIAL_LINE_LENGTH+++[] | int | `4096` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-form-attribute-size"></a> [`quarkus.http.limits.max-form-attribute-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-form-attribute-size) config_property_copy_button:+++quarkus.http.limits.max-form-attribute-size+++[]<br><br>The maximum length of a form attribute, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_FORM_ATTRIBUTE_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits) | `2048` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-form-fields"></a> [`quarkus.http.limits.max-form-fields`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-form-fields) config_property_copy_button:+++quarkus.http.limits.max-form-fields+++[]<br><br>Set the maximum number of fields of a form. Set to `-1` to allow unlimited number of attributes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_FORM_FIELDS+++[] | int | `256` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-form-buffered-bytes"></a> [`quarkus.http.limits.max-form-buffered-bytes`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-form-buffered-bytes) config_property_copy_button:+++quarkus.http.limits.max-form-buffered-bytes+++[]<br><br>Set the maximum number of bytes a server can buffer when decoding a form. Set to `-1` to allow unlimited length, up to `Integer.MAX_VALUE` bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_FORM_BUFFERED_BYTES+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits) | `1K` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-parameters"></a> [`quarkus.http.limits.max-parameters`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-parameters) config_property_copy_button:+++quarkus.http.limits.max-parameters+++[]<br><br>The maximum number of HTTP request parameters permitted for incoming requests.<br><br>If a client sends more than this number of parameters in a request, the connection is closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_PARAMETERS+++[] | int | `1000` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-multipart-part-header-size"></a> [`quarkus.http.limits.max-multipart-part-header-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-multipart-part-header-size) config_property_copy_button:+++quarkus.http.limits.max-multipart-part-header-size+++[]<br><br>The maximum size of the headers section within a single MIME part of a `multipart/form-data` request. This limits the combined size of all header names and values for each individual part (e.g., the Content-Disposition and Content-Type headers), not the HTTP request headers.<br><br>If a part's headers exceed this limit, the request is rejected with HTTP 413.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_MULTIPART_PART_HEADER_SIZE+++[] | MemorySize [[question circle]](#memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits) | `32K` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-multipart-header-count"></a> [`quarkus.http.limits.max-multipart-header-count`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-multipart-header-count) config_property_copy_button:+++quarkus.http.limits.max-multipart-header-count+++[]<br><br>The maximum number of headers allowed within a single MIME part of a `multipart/form-data` request.<br><br>If a part contains more headers than this limit, the request is rejected with HTTP 413.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_MULTIPART_HEADER_COUNT+++[] | int | `40` |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-connections"></a> [`quarkus.http.limits.max-connections`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-connections) config_property_copy_button:+++quarkus.http.limits.max-connections+++[]<br><br>The maximum number of connections that are allowed at any one time. If this is set it is recommended to set a short idle timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_CONNECTIONS+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-header-table-size"></a> [`quarkus.http.limits.header-table-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-header-table-size) config_property_copy_button:+++quarkus.http.limits.header-table-size+++[]<br><br>Set the SETTINGS_HEADER_TABLE_SIZE HTTP/2 setting.<br><br>Allows the sender to inform the remote endpoint of the maximum size of the header compression table used to decode header blocks, in octets. The encoder can select any size equal to or less than this value by using signaling specific to the header compression format inside a header block. The initial value is `4,096` octets.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_HEADER_TABLE_SIZE+++[] | long |  |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-concurrent-streams"></a> [`quarkus.http.limits.max-concurrent-streams`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-concurrent-streams) config_property_copy_button:+++quarkus.http.limits.max-concurrent-streams+++[]<br><br>Set SETTINGS_MAX_CONCURRENT_STREAMS HTTP/2 setting.<br><br>Indicates the maximum number of concurrent streams that the sender will allow. This limit is directional: it applies to the number of streams that the sender permits the receiver to create. Initially, there is no limit to this value. It is recommended that this value be no smaller than 100, to not unnecessarily limit parallelism.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_CONCURRENT_STREAMS+++[] | long |  |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-frame-size"></a> [`quarkus.http.limits.max-frame-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-frame-size) config_property_copy_button:+++quarkus.http.limits.max-frame-size+++[]<br><br>Set the SETTINGS_MAX_FRAME_SIZE HTTP/2 setting. Indicates the size of the largest frame payload that the sender is willing to receive, in octets. The initial value is `2^14` (16,384) octets.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_FRAME_SIZE+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-header-list-size"></a> [`quarkus.http.limits.max-header-list-size`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-max-header-list-size) config_property_copy_button:+++quarkus.http.limits.max-header-list-size+++[]<br><br>Set the SETTINGS_MAX_HEADER_LIST_SIZE HTTP/2 setting. This advisory setting informs a peer of the maximum size of header list that the sender is prepared to accept, in octets. The value is based on the uncompressed size of header fields, including the length of the name and value in octets plus an overhead of 32 octets for each header field. The default value is `8192`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_MAX_HEADER_LIST_SIZE+++[] | long |  |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-rst-flood-max-rst-frame-per-window"></a> [`quarkus.http.limits.rst-flood-max-rst-frame-per-window`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-rst-flood-max-rst-frame-per-window) config_property_copy_button:+++quarkus.http.limits.rst-flood-max-rst-frame-per-window+++[]<br><br>Set the max number of RST frame allowed per time window, this is used to prevent [HTTP/2 RST frame flood DDOS attacks](https://github.com/netty/netty/security/advisories/GHSA-xpw8-rcwv-8f8p). The default value is `200`, setting zero or a negative value, disables flood protection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_RST_FLOOD_MAX_RST_FRAME_PER_WINDOW+++[] | int |  |
| <a id="quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-rst-flood-window-duration"></a> [`quarkus.http.limits.rst-flood-window-duration`](#quarkus-vertx-http_quarkus-http-limits_quarkus-http-limits-rst-flood-window-duration) config_property_copy_button:+++quarkus.http.limits.rst-flood-window-duration+++[]<br><br>Set the duration of the time window when checking the max number of RST frames, this is used to prevent [HTTP/2 RST frame flood DDOS attacks](https://github.com/netty/netty/security/advisories/GHSA-xpw8-rcwv-8f8p).. The default value is `30 s`, setting zero or a negative value, disables flood protection.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_LIMITS_RST_FLOOD_WINDOW_DURATION+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-limits) |  |

<a id="duration-note-anchor-quarkus-vertx-http_quarkus-http-limits"></a>

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

<a id="memory-size-note-anchor-quarkus-vertx-http_quarkus-http-limits"></a>

> [!NOTE]
> **About the MemorySize format**
>
> A size configuration option recognizes strings in this format (shown as a regular expression): `[0-9]+[KkMmGgTtPpEeZzYy]?`.
>
> If no suffix is given, assume bytes.

## Configure traffic shaping

Traffic shaping allows you to limit the bandwidth across all channels (i.e. connections), regardless of the number of open channels. This can be useful when you want to control the overall network traffic to prevent congestion or prioritize certain types of traffic.

To enable traffic shaping, add the following property in your application configuration:

```properties
quarkus.http.traffic-shaping.enabled=true # Required to enable traffic shaping
```

The traffic shaping allows you to configure various parameters, such as write and read limitations (in bytes per  second), check interval (the delay between two computations of the bandwidth), and maximum time to wait:

```properties
quarkus.http.traffic-shaping.enabled=true # Required to enable traffic shaping
quarkus.http.traffic-shaping.check-interval=30s
quarkus.http.traffic-shaping.outbound-global-bandwidth=1M
quarkus.http.traffic-shaping.inbound-global-bandwidth=1M
quarkus.http.traffic-shaping.max-delay=10s
```

The check interval represents the period at which the traffic is computed, and a higher interval may result in less precise traffic shaping. Despite 0 being accepted (no accounting), it is recommended to set a positive value for the check interval, even if it is high since the precision of the traffic shaping depends on the period where the traffic is computed. In this case, a suggested value is something close to 5 or 10 minutes.

The `outbound-global-bandwidth` and `inbound-global-bandwidth` parameters represent the maximum number of bytes per second for write and read operations, respectively. You shall also consider to have object size in read or write operations relatively adapted to the bandwidth you required. For instance having 10 MB objects for 10KB/s will lead to burst effect, while having 100 KB objects for 1 MB/s should be smoothly handle by the traffic shaping.

Additionally, you can set the maximum time to wait (`max-delay`), which specifies an upper bound for time shaping. By default, it is set to 15 seconds. It must be less than the HTTP timeout. When one of the threshold is reached, no write happens for that period of time.

## Configuring HTTP Access Logs

You can add HTTP request logging by configuring it in `application.properties`. There are two options for logging, either logging to the standard JBoss logging output, or logging to a dedicated file.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-enabled"></a> [`quarkus.http.access-log.enabled`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-enabled) config_property_copy_button:+++quarkus.http.access-log.enabled+++[]<br><br>If access logging is enabled. By default this will log via the standard logging facility<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-exclude-pattern"></a> [`quarkus.http.access-log.exclude-pattern`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-exclude-pattern) config_property_copy_button:+++quarkus.http.access-log.exclude-pattern+++[]<br><br>A regular expression that can be used to exclude some paths from logging.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_EXCLUDE_PATTERN+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-pattern"></a> [`quarkus.http.access-log.pattern`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-pattern) config_property_copy_button:+++quarkus.http.access-log.pattern+++[]<br><br>The access log pattern.<br><br>If this is the string `common`, `combined` or `long` then this will use one of the specified named formats:<br><br>- common: `%h %l %u %t "%r" %s %b` - combined: `%h %l %u %t "%r" %s %b "%{i,Referer}" "%{i,User-Agent}"` - long: `%r\n%{ALL_REQUEST_HEADERS}`<br><br>Otherwise, consult the Quarkus documentation for the full list of variables that can be used. Note that enabling the `%{ALL_REQUEST_HEADERS}` attribute directly or with a `long` named format introduces a risk of sensitive header values being logged.<br><br>HTTP `Authorization` header value is always masked. Use the `masked-headers()` property to mask other sensitive headers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_PATTERN+++[] | string | `common` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-masked-headers"></a> [`quarkus.http.access-log.masked-headers`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-masked-headers) config_property_copy_button:+++quarkus.http.access-log.masked-headers+++[]<br><br>Set of HTTP headers whose values must be masked when the `%{ALL_REQUEST_HEADERS}` attribute is enabled with the `pattern()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_MASKED_HEADERS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-masked-cookies"></a> [`quarkus.http.access-log.masked-cookies`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-masked-cookies) config_property_copy_button:+++quarkus.http.access-log.masked-cookies+++[]<br><br>Set of HTTP Cookie headers whose values must be masked when the `%{ALL_REQUEST_HEADERS}` attribute is enabled with the `pattern()` property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_MASKED_COOKIES+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-log-to-file"></a> [`quarkus.http.access-log.log-to-file`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-log-to-file) config_property_copy_button:+++quarkus.http.access-log.log-to-file+++[]<br><br>If logging should be done to a separate file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_LOG_TO_FILE+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-base-file-name"></a> [`quarkus.http.access-log.base-file-name`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-base-file-name) config_property_copy_button:+++quarkus.http.access-log.base-file-name+++[]<br><br>The access log file base name, defaults to 'quarkus' which will give a log file name of 'quarkus.log'.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_BASE_FILE_NAME+++[] | string | `quarkus` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-log-directory"></a> [`quarkus.http.access-log.log-directory`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-log-directory) config_property_copy_button:+++quarkus.http.access-log.log-directory+++[]<br><br>The log directory to use when logging access to a file<br><br>If this is not set then the current working directory is used.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_LOG_DIRECTORY+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-log-suffix"></a> [`quarkus.http.access-log.log-suffix`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-log-suffix) config_property_copy_button:+++quarkus.http.access-log.log-suffix+++[]<br><br>The log file suffix<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_LOG_SUFFIX+++[] | string | `.log` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-category"></a> [`quarkus.http.access-log.category`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-category) config_property_copy_button:+++quarkus.http.access-log.category+++[]<br><br>The log category to use if logging is being done via the standard log mechanism (i.e. if base-file-name is empty).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_CATEGORY+++[] | string | `io.quarkus.http.access-log` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-rotate"></a> [`quarkus.http.access-log.rotate`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-rotate) config_property_copy_button:+++quarkus.http.access-log.rotate+++[]<br><br>If the log should be rotated daily<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_ROTATE+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-consolidate-rerouted-requests"></a> [`quarkus.http.access-log.consolidate-rerouted-requests`](#quarkus-vertx-http_quarkus-http-access-log_quarkus-http-access-log-consolidate-rerouted-requests) config_property_copy_button:+++quarkus.http.access-log.consolidate-rerouted-requests+++[]<br><br>If rerouted requests should be consolidated into one log entry<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_ACCESS_LOG_CONSOLIDATE_REROUTED_REQUESTS+++[] | boolean | `false` |

| Attribute | Short Form | Long Form |
| --- | --- | --- |
| Remote IP address | `%a` | `%{REMOTE_IP}` |
| Local IP address | `%A` | `%{LOCAL_IP}` |
| Bytes sent, excluding HTTP headers, or '-' if no bytes were sent | `%b` |  |
| Bytes sent, excluding HTTP headers | `%B` | `%{BYTES_SENT}` |
| Remote host name | `%h` | `%{REMOTE_HOST}` |
| Remote logical username from identd (always returns '-') | `%l` |  |
| Request protocol | `%H` | `%{PROTOCOL}` |
| Request method | `%m` | `%{METHOD}` |
| Local port | `%p` | `%{LOCAL_PORT}` |
| Query string (prepended with a '?' if it exists, otherwise an empty string) | `%q` | `%{QUERY_STRING}` |
| First line of the request | `%r` | `%{REQUEST_LINE}` |
| HTTP status code of the response | `%s` | `%{RESPONSE_CODE}` |
| Date and time, in Common Log Format format | `%t` | `%{DATE_TIME}` |
| Date and time as defined by a DateTimeFormatter compliant string |  | `%{time,date_fime_formatter_string}` |
| Remote user that was authenticated | `%u` | `%{REMOTE_USER}` |
| Requested URL path | `%U` | `%{REQUEST_URL}` |
| Request relative path | `%R` | `%{REQUEST_PATH}` |
| Local server name | `%v` | `%{LOCAL_SERVER_NAME}` |
| Time taken to process the request, in millis | `%D` | `%{RESPONSE_TIME}` |
| Time taken to process the request, in seconds | `%T` |  |
| Time taken to process the request, in micros |  | `%{RESPONSE_TIME_MICROS}` |
| Time taken to process the request, in nanos |  | `%{RESPONSE_TIME_NANOS}` |
| Current request thread name | `%I` | `%{THREAD_NAME}` |
| SSL cypher |  | `%{SSL_CIPHER}` |
| SSL client certificate |  | `%{SSL_CLIENT_CERT}` |
| SSL session id |  | `%{SSL_SESSION_ID}` |
| All request headers |  | `%{ALL_REQUEST_HEADERS}` |
| Cookie value |  | `%{c,cookie_name}` |
| Query parameter |  | `%{q,query_param_name}` |
| Request header |  | `%{i,request_header_name}` |
| Response header |  | `%{o,response_header_name}` |
| Vert.x Routing Context Internal Data |  | `%{d,map_key}` |
| Vert.x MDC data (e.g. 'traceId' for OpenTelemetry) |  | `%{X,mdc-key}` |

Set `quarkus.http.access-log.consolidate-rerouted-requests=true` to enable support for the modifier `<`. This modifier can be used for requests that have been internally redirected to consult the original request. The following attributes support this modifier:

| Attribute | Short Form | Long Form |
| --- | --- | --- |
| First line of the request | `%<r` | `%{<REQUEST_LINE}` |
| Request method | `%<m` | `%{<METHOD}` |
| Request relative path | `%<R` | `%{<REQUEST_PATH}` |
| Requested URL path | `%<U` | `%{<REQUEST_URL}` |
| Query string (prepended with a '?' if it exists, otherwise an empty string) | `%<q` | `%{<QUERY_STRING}` |
| Query parameter |  | `%{<q,query_param_name}` |

> [!NOTE]
> Set `quarkus.http.record-request-start-time=true` to enable recording request start times when using any of the attributes related to logging request processing times.

> [!NOTE]
> Assuming security has been set up for the application (see our [guide](/guides/security-overview/index.md) for more details), logging attribute `Remote user that was authenticated` is set to the value of the `io.quarkus.security.identity.SecurityIdentity` principal. If your application use custom [Jakarta REST SecurityContext](/guides/security-customization/index.md#jaxrs-security-context), the context principal is used instead. Please refer to the [Logging guide](/guides/logging/index.md) for options how to add contextual log information yourself.

> [!TIP]
> Use `quarkus.http.access-log.exclude-pattern=/some/path/.*` to exclude all entries concerning the path `/some/path/...` (*including subsequent paths*) from the log.

## Arbitrary customizations

Quarkus allows users to arbitrarily customize the options of HTTP servers started by Quarkus via the use of `io.quarkus.vertx.http.HttpServerOptionsCustomizer`. For example, if the HTTP port needs to be set programmatically, then the following code could be used:

```java
import jakarta.inject.Singleton;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;

@Singleton <1>
public class MyCustomizer implements HttpServerOptionsCustomizer {

    @Override
    public void customizeHttpServer(HttpServerOptions options) { <2>
        options.setPort(9998);
    }
}
```

1. By making the class a managed bean, Quarkus will take the customizer into account when it starts the Vert.x servers
2. In this case, we only care about customizing the HTTP server, so we just override the `customizeHttpServer` method, but users should be aware that `HttpServerOptionsCustomizer` allows configuring the HTTPS and Domain Socket servers as well

## How to execute logic when HTTP server started

In order to execute some custom action when the HTTP server is started you'll need to declare an *asynchronous* CDI observer method. Quarkus *asynchronously* fires CDI events of types `io.quarkus.vertx.http.HttpServerStart`, `io.quarkus.vertx.http.HttpsServerStart` and `io.quarkus.vertx.http.DomainSocketServerStart` when the corresponding HTTP server starts listening on the configured host and port.

**`HttpServerStart` example**

```java
@ApplicationScoped
public class MyListener {

   void httpStarted(@ObservesAsync HttpServerStart start) { <1>
      // ...notified when the HTTP server starts listening
   }
}
```

1. An asynchronous `HttpServerStart` observer method may be declared by annotating an `HttpServerStart` parameter with `@jakarta.enterprise.event.ObservesAsync`.

> [!NOTE]
> It's not possible to use the `StartupEvent` for this particular use case because this CDI event is fired before the HTTP server is started.

<a id="reverse-proxy"></a>
## Running behind a reverse proxy

Quarkus could be accessed through proxies that additionally generate headers (e.g. `X-Forwarded-Host`) to keep information from the client-facing side of the proxy servers that is altered or lost when they are involved. In those scenarios, Quarkus can be configured to automatically update information like protocol, host, port and URI reflecting the values in these headers.

> [!IMPORTANT]
> Activating this feature leaves the server exposed to several security issues (i.e. information spoofing). Consider activate it only when running behind a reverse proxy.

To set up this feature, please include the following lines in `src/main/resources/application.properties`:

```properties
quarkus.http.proxy.proxy-address-forwarding=true
```

To consider only de-facto standard header (`Forwarded` header), please include the following lines in `src/main/resources/application.properties`:

```properties
quarkus.http.proxy.allow-forwarded=true
```

To consider only non-standard headers, please include the following lines instead in `src/main/resources/application.properties`:

```properties
quarkus.http.proxy.proxy-address-forwarding=true
quarkus.http.proxy.allow-x-forwarded=true
quarkus.http.proxy.enable-forwarded-host=true
quarkus.http.proxy.enable-forwarded-prefix=true
quarkus.http.proxy.trusted-proxies=127.0.0.1 <1>
```

1. Configure trusted proxy with the IP address `127.0.0.1`. Request headers from any other address are going to be ignored.

Alternatively, in mTLS environments where proxy IP addresses are not stable (e.g., Kubernetes), you can trust a proxy by its TLS client certificate Subject DN. The proxy is trusted when its certificate's Subject DN contains all components specified in any configured value. For example, `CN=my-proxy,O=MyOrg` matches a certificate with Subject DN `CN=my-proxy,O=MyOrg,C=US`. When no configured DN matches, forwarded headers are ignored and the original connection values are used:

```properties
quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=my-proxy <1><2><3>
quarkus.http.proxy.trusted-proxy[1].subject-dn=CN=envoy-client,O=MyOrg
```

1. DNs must be in RFC 2253 format.
2. This option cannot be combined with `quarkus.http.proxy.trusted-proxies`.
3. TLS client authentication must be enabled via `quarkus.http.ssl.client-auth` set to `request` or `required`.

> [!NOTE]
> Configure enough DN components to uniquely identify your proxy. Using only `CN=my-proxy` will match any certificate with that CN, regardless of issuing organization.

When multiple CAs are trusted, different CAs could issue certificates with the same Subject DN. To also verify the trust anchor, reference a certificate alias in the HTTP server's truststore. Both conditions must match — the Subject DN and the trust anchor. The following example uses the [TLS centralized configuration](/guides/tls-registry-reference/index.md) to set up mTLS and proxy trust:

```properties
quarkus.tls.key-store.p12.path=server-keystore.p12
quarkus.tls.key-store.p12.password=secret
quarkus.tls.trust-store.p12.path=server-truststore.p12 <1>
quarkus.tls.trust-store.p12.password=secret
quarkus.http.ssl.client-auth=request

# Proxy trust
quarkus.http.proxy.proxy-address-forwarding=true
quarkus.http.proxy.allow-forwarded=true
quarkus.http.proxy.trusted-proxy[0].subject-dn=CN=my-proxy,O=MyOrg
quarkus.http.proxy.trusted-proxy[0].truststore-alias=my-proxy-ca <2>
```

1. The truststore must be configured via the TLS registry and must contain a certificate entry with alias `my-proxy-ca`.
2. The alias is resolved from `server-truststore.p12`.

When the truststore is configured with PEM certificates, aliases are assigned automatically as `cert-0`, `cert-1`, and so on, based on the order in the configuration:

```properties
quarkus.tls.trust-store.pem.certs=proxy-ca.crt,other-ca.crt <1>
quarkus.http.proxy.trusted-proxy[0].truststore-alias=cert-0 <2>
```

1. `proxy-ca.crt` becomes `cert-0`, `other-ca.crt` becomes `cert-1`.
2. References `proxy-ca.crt`.

Since PEM aliases depend on ordering, we recommend using a [PKCS12 truststore](/guides/tls-registry-reference/index.md#pkcs12_truststores) to avoid certificate list ordering issues.

> [!WARNING]
> The configured Subject DN is trusted to be unique only among clients signed by the CA referenced by the truststore alias. Only reference a CA that you control or trust not to issue another certificate with the same Subject DN.

Both configurations related to standard and non-standard headers can be combined, although the standard headers configuration will have precedence. However, combining them has security implications as clients can forge requests with a forwarded header that is not overwritten by the proxy. Therefore, proxies should strip unexpected `Forwarded` or `X-Forwarded-*` headers from the client.

By default, the `proto` values from `Forwarded` and `X-Forwarded-Proto` headers are validated against the [URI scheme syntax](https://datatracker.ietf.org/doc/html/rfc3986#section-3.1). Requests with an invalid proto value receive a `400 Bad Request` response. To disable this validation, set the `quarkus.http.proxy.forwarded-proto-validation` configuration property to `none`.

Supported forwarding address headers are:

- `Forwarded`
- `X-Forwarded-Proto`
- `X-Forwarded-Host`
- `X-Forwarded-Port`
- `X-Forwarded-Ssl`
- `X-Forwarded-Prefix`

<a id="same-site-cookie"></a>
## SameSite cookies

One can easily add a [SameSite](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite) cookie property to any of the cookies set by a Quarkus endpoint by listing a cookie name and a `SameSite` attribute, for example:

```properties
quarkus.http.same-site-cookie.jwt.value=Lax
quarkus.http.same-site-cookie.session.value=Strict
```

Given this configuration, the `jwt` cookie will have a `SameSite=Lax` attribute and the `session` cookie will have a `SameSite=Strict` attribute.

## Servlet Config

To use Servlet you need to explicitly include `quarkus-undertow`:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-undertow</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-undertow")
```

### undertow-handlers.conf

You can make use of the Undertow predicate language using an `undertow-handlers.conf` file. This file should be placed in the `META-INF` directory of your application jar. This file contains handlers defined using the [Undertow predicate language](https://undertow.io/undertow-docs/undertow-docs-2.0.0/index.html#predicates-attributes-and-handlers).

### web.xml

If you are using a `web.xml` file as your configuration file, you can place it in the `src/main/resources/META-INF` directory.

### Built-in route order values

Route order values are the values that are specified via Vert.x route `io.vertx.ext.web.Route.order(int)` function.

Quarkus registers a couple of routes with specific order values. The constants are defined in the `io.quarkus.vertx.http.runtime.RouteConstants` class and listed in the table below. A custom route should define the order of value 20000 or higher so that it does not interfere with the functionality provided by Quarkus and extensions.

Route order constants defined in `io.quarkus.vertx.http.runtime.RouteConstants` and known extensions:

| Route order value | Constant name | Origin |
| --- | --- | --- |
| `Integer.MIN_VALUE` | `ROUTE_ORDER_ACCESS_LOG_HANDLER` | Access-log handler, if enabled in the configuration. |
| `Integer.MIN_VALUE` | `ROUTE_ORDER_RECORD_START_TIME` | Handler adding the start-time, if enabled in the configuration. |
| `Integer.MIN_VALUE` | `ROUTE_ORDER_HOT_REPLACEMENT` | -replacement body handler. |
| `Integer.MIN_VALUE` | `ROUTE_ORDER_HEADERS` | Handlers that add headers specified in the configuration. |
| `Integer.MIN_VALUE + 1` | `ROUTE_ORDER_HOST_VALIDATION_MANAGEMENT` | Host validation handler of the management router. |
| `Integer.MIN_VALUE + 2` | `ROUTE_ORDER_CORS_MANAGEMENT` | CORS-Origin handler of the management router. |
| `Integer.MIN_VALUE + 3` | `ROUTE_ORDER_BODY_HANDLER_MANAGEMENT` | Body handler for the management router. |
| `Integer.MIN_VALUE + 1` | `ROUTE_ORDER_BODY_HANDLER` | Body handler. |
| `-2` | `ROUTE_ORDER_UPLOAD_LIMIT` | Route that enforces the upload body size limit. |
| `0` | `ROUTE_ORDER_COMPRESSION` | Compression handler. |
| `1000` | `ROUTE_ORDER_BEFORE_DEFAULT` | Route with priority over the default route (add an offset from this value). |
| `10000` | `ROUTE_ORDER_DEFAULT` | Default route order (i.e. Static Resources, Servlet). |
| `20000` | `ROUTE_ORDER_AFTER_DEFAULT` | Route without priority over the default route (add an offset from this value) |

### Undertow configuration

The following configuration properties are available when using the Undertow servlet extension.

| Configuration property | Type | Default | Description |
| --- | --- | --- | --- |
| `quarkus.undertow.disallowed-methods` | list of string | `<empty>` | A list of HTTP methods that should be rejected with `405 Method Not Allowed`. Useful for disabling insecure methods such as `TRACE` or `TRACK`.<br><br>Example:<br><br>```<br>quarkus.undertow.disallowed-methods=TRACE,TRACK<br>``` |
| `quarkus.undertow.record-request-start-time` | boolean | false | If enabled, Undertow records the request start timestamp. Useful for timing, logging and request tracing. |
| `quarkus.undertow.max-parameters` | int | 1000 | Maximum number of allowed HTTP parameters. Provides protection against parameter-based DoS attacks. |
