# Cross-Origin Resource Sharing (CORS)

Enable and configure CORS in Quarkus to specify allowed origins, methods, and headers, guiding browsers in handling cross-origin requests safely.

Cross-Origin Resource Sharing (CORS) uses HTTP headers to manage browser requests for resources from external origins securely. By specifying permitted origins, methods, and headers, Quarkus servers can use the CORS filter to enable browsers to request resources across domains while maintaining controlled access. This mechanism enhances security and supports legitimate cross-origin requests. For more on origin definitions, see the [Web Origin Concept](https://datatracker.ietf.org/doc/html/rfc6454).

<a id="cors-filter"></a>
## Enabling the CORS filter

To enforce CORS policies in your application, enable the Quarkus CORS filter by adding the following line to the `src/main/resources/application.properties` file:

```properties
quarkus.http.cors.enabled=true
```

The filter intercepts all incoming HTTP requests to identify cross-origin requests and applies the configured policy. The filter then adds CORS headers to the HTTP response, informing browsers about allowed origins and access parameters. For preflight requests, the filter returns an HTTP response immediately. For regular CORS requests, the filter denies access with an HTTP 403 status if the request violates the configured policy; otherwise, the filter forwards the request to the destination if the policy allows it.

> [!NOTE]
> Despite its name, the CORS filter can also prevent CSRF attacks based on [Origin verification](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#using-standard-headers-to-verify-origin). Therefore, since the browser is expected to set an [Origin header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Origin) for cross-origin JavaScript and HTML form requests, you might want to consider using the CORS filter instead of the [REST CSRF filter]({quarkusio-guides}/security-csrf-prevention).
>
> You must confirm that the browser sets an `Origin` header for cross-origin requests when accessing your application, especially with HTML forms, before using the CORS filter to prevent CSRF through [Origin verification](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#using-standard-headers-to-verify-origin).

For detailed configuration options, see the following Configuration Properties section.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-enabled"></a> [`quarkus.http.cors.enabled`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-enabled) config_property_copy_button:+++quarkus.http.cors.enabled+++[]<br><br>Enable the CORS filter.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-origins"></a> [`quarkus.http.cors.origins`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-origins) config_property_copy_button:+++quarkus.http.cors.origins+++[]<br><br>The origins allowed for CORS.<br><br>A comma-separated list of valid URLs, such as `http://www.quarkus.io,http://localhost:3000`. URLs enclosed in forward slashes are interpreted as regular expressions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_ORIGINS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-methods"></a> [`quarkus.http.cors.methods`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-methods) config_property_copy_button:+++quarkus.http.cors.methods+++[]<br><br>The HTTP methods allowed for CORS requests.<br><br>A comma-separated list of valid HTTP methods, such as `GET,PUT,POST`. If not set, the filter allows any HTTP method by default.<br><br>This property is not used to control same-origin HTTP request methods.<br><br>Default: Any HTTP request method is allowed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-headers"></a> [`quarkus.http.cors.headers`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-headers) config_property_copy_button:+++quarkus.http.cors.headers+++[]<br><br>The HTTP headers allowed for CORS requests.<br><br>A comma-separated list of valid headers, such as `X-Custom,Content-Disposition`. If not set, the filter allows any header by default.<br><br>Default: Any HTTP request header is allowed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_HEADERS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-exposed-headers"></a> [`quarkus.http.cors.exposed-headers`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-exposed-headers) config_property_copy_button:+++quarkus.http.cors.exposed-headers+++[]<br><br>The HTTP headers exposed in CORS responses.<br><br>A comma-separated list of headers to expose, such as `X-Custom,Content-Disposition`.<br><br>Default: No headers are exposed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_EXPOSED_HEADERS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-access-control-max-age"></a> [`quarkus.http.cors.access-control-max-age`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-access-control-max-age) config_property_copy_button:+++quarkus.http.cors.access-control-max-age+++[]<br><br>The `Access-Control-Max-Age` response header value in `java.time.Duration` format.<br><br>Informs the browser how long it can cache the results of a preflight request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_ACCESS_CONTROL_MAX_AGE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-cors) |  |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-access-control-allow-credentials"></a> [`quarkus.http.cors.access-control-allow-credentials`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-access-control-allow-credentials) config_property_copy_button:+++quarkus.http.cors.access-control-allow-credentials+++[]<br><br>The `Access-Control-Allow-Credentials` response header.<br><br>Tells browsers if front-end JavaScript can be allowed to access credentials when the request's credentials mode, `Request.credentials`, is set to `include`.<br><br>Default: `true` if the `quarkus.http.cors.origins` property is set and matches the precise `Origin` header value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_ACCESS_CONTROL_ALLOW_CREDENTIALS+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-return-exact-origins"></a> [`quarkus.http.cors.return-exact-origins`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-return-exact-origins) config_property_copy_button:+++quarkus.http.cors.return-exact-origins+++[]<br><br>Whether to return the exact request origin in the `Access-Control-Allow-Origin` response header.<br><br>When set to `true` (the default), the filter echoes back the request's `Origin` header value. When set to `false` and `origins` is configured as `*`, the response will contain the literal `Access-Control-Allow-Origin: *` instead of echoing the request origin. This is useful for public APIs where HTTP caching is important, as echoing the origin requires `Vary: Origin` and defeats shared caches.<br><br>Note: When `*` is returned as the origin, `Access-Control-Allow-Credentials` is forced to `false` per the CORS specification.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_RETURN_EXACT_ORIGINS+++[] | boolean | `true` |
| <a id="quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-vary-origin"></a> [`quarkus.http.cors.vary-origin`](#quarkus-vertx-http_quarkus-http-cors_quarkus-http-cors-vary-origin) config_property_copy_button:+++quarkus.http.cors.vary-origin+++[]<br><br>Whether to add the `Vary: Origin` response header to CORS responses when the origin is not a wildcard.<br><br>The `Vary: Origin` header tells HTTP caches that the response depends on the request's `Origin` header, preventing a cached response for one origin from being served to a different origin.<br><br>This should remain enabled (the default) in most setups, especially when a reverse proxy or CDN may cache responses.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_CORS_VARY_ORIGIN+++[] | boolean | `true` |

<a id="duration-note-anchor-quarkus-vertx-http_quarkus-http-cors"></a>

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

## Example CORS configuration

The following example shows a complete CORS filter configuration, including a regular expression to define one of the origins.

```properties
quarkus.http.cors.enabled=true <1>
quarkus.http.cors.origins=http://example.com,http://www.example.io,/https://([a-z0-9\\-_]+)\\\\.app\\\\.mydomain\\\\.com/ <2>
quarkus.http.cors.methods=GET,PUT,POST <3>
quarkus.http.cors.headers=X-Custom <4>
quarkus.http.cors.exposed-headers=Content-Disposition <5>
quarkus.http.cors.access-control-max-age=24H <6>
quarkus.http.cors.access-control-allow-credentials=true <7>
```

1. Enables the CORS filter.
2. Specifies allowed origins, including a regular expression. When not specified, CORS is not permitted, and only same-origin requests are allowed.
3. Lists allowed HTTP methods for cross-origin requests.
4. Declares custom headers that clients can include in requests.
5. Identifies response headers that clients can access.
6. Sets how long preflight request results are cached.
7. Allows cookies or credentials in cross-origin requests.

When using regular expressions in an `application.properties` file, escape special characters with four backward slashes (`\\\\`) to ensure proper behavior. For example:

- `\\\\.` matches a literal `.` character.
- `\\.` matches any single character as a regular expression metadata character.

> [!IMPORTANT]
> Incorrectly escaped patterns can lead to unintended behavior or security vulnerabilities. Always verify regular expression syntax before deployment.

## When to accept all origins

> [!WARNING]
> Allowing unrestricted origins with `quarkus.http.cors.origins=*` in production environments poses severe security risks, such as unauthorized data access or resource abuse. For production, define explicit origins in the `quarkus.http.cors.origins` property.

The only exception where accepting all origins might be appropriate in production is for read-only public API applications that have no side effects of any kind, such as managing cookies, saving request data on the disk, or similar operations. In such cases, to improve the HTTP cache performance, you might want to return a literal `*` origin by setting `quarkus.http.cors.return-exact-origins=false` to avoid the `Vary: Origin` response header.

Also, since configuring origins during development can be challenging, consider allowing all origins in development mode:

```properties
quarkus.http.cors.enabled=true
%dev.quarkus.http.cors.origins=/.*/
```

<a id="cors-filter-programmatic-set-up"></a>
## Configuring the CORS filter programmatically

> [!NOTE]
> Configure the CORS filter by using [configuration properties](#cors-filter) or programmatically, but not both.

To enforce CORS policies in your application, enable the Quarkus CORS filter with the `io.quarkus.vertx.http.security.HttpSecurity` CDI event:

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class CorsProgrammaticConfig {
    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.cors("https://example.com");
    }
}
```

Use the `io.quarkus.vertx.http.security.CORS` builder to create a complete CORS configuration:

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.CORS;
import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class CorsProgrammaticConfig {
    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.cors(CORS.builder()
                .origin("https://example.com")
                .method("POST")
                .build());
    }
}
```

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
- [Quarkus HTTP Reference](/guides/http-reference/index.md)
- [Mozilla HTTP CORS documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS)
