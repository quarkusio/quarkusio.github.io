# Quarkus Security overview

Quarkus Security is a framework that provides the architecture, multiple authentication and authorization mechanisms, and other tools to build secure and production-quality Java applications.

Before building security into your Quarkus applications, learn about the [Quarkus Security architecture](/guides/security-architecture/index.md) and the different authentication mechanisms and features you can use.

## Key features of Quarkus Security

The Quarkus Security framework provides built-in security authentication mechanisms for Basic, Form-based, and mutual TLS (mTLS) authentication. You can also use other well-known [authentication mechanisms](/guides/security-authentication-mechanisms/index.md#other-supported-authentication-mechanisms), such as OpenID Connect (OIDC) and WebAuthn. Authentication mechanisms depend on [Identity providers](/guides/security-identity-providers/index.md) to verify the authentication credentials and map them to a `SecurityIdentity` instance with the username, roles, original authentication credentials, and other attributes.

Quarkus also includes built-in security to allow for role-based access control (RBAC) based on the common security annotations `@RolesAllowed`, `@DenyAll`, `@PermitAll` on REST endpoints, and Contexts and Dependency Injection (CDI) beans. For more information, see the Quarkus [Authorization of web endpoints](/guides/security-authorize-web-endpoints-reference/index.md) guide.

Quarkus Security also supports the following features:

- [Proactive authentication](/guides/security-proactive-authentication/index.md)
- [Secure connections with SSL/TLS](/guides/http-reference/index.md#ssl)
- [Cross-origin resource sharing](#cross-origin-resource-sharing)
- [Cross-Site Request Forgery (CSRF) prevention](#csrf-prevention)
- [SameSite cookies](#samesite-cookies)
- [Secrets engines](#secrets-engines)
- [Secure auto-generated resources by REST Data with Panache](#rest-data-panache)
- [Secure serialization](#secure-serialization)
- [Security vulnerability detection and National Vulnerability Database (NVD) registration](/guides/security-vulnerability-detection/index.md)

Quarkus Security is also highly customizable. For more information, see the Quarkus [Security tips and tricks](/guides/security-customization/index.md) guide.

## Getting started with Quarkus Security

To get started with security in Quarkus, consider securing your Quarkus application endpoints with the built-in Quarkus [Basic authentication](/guides/security-basic-authentication/index.md) and the Jakarta Persistence identity provider and enabling role-based access control.

Complete the steps in the [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md) tutorial.

After successfully securing your Quarkus application with Basic authentication, you can increase the security further by adding more advanced authentication mechanisms, for example, the Quarkus [OpenID Connect (OIDC) authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md) guide.

## Quarkus Security testing

For guidance on testing Quarkus Security features and ensuring that your Quarkus applications are securely protected, see the [Security testing](/guides/security-testing/index.md) guide.

## More about security features in Quarkus

### WebSockets Next security

The `quarkus-websockets-next` extension provides a modern, efficient implementation of the WebSocket API. It also provides an integration with Quarkus security. For more information, see the [Security](/guides/websockets-next-reference/index.md#websocket-next-security) section of the Quarkus "WebSockets Next reference" guide.

<a id="cross-origin-resource-sharing"></a>
### Cross-origin resource sharing

To make your Quarkus application accessible to another application running on a different domain, you need to configure cross-origin resource sharing (CORS). For more information about the CORS filter Quarkus provides, see the [CORS filter](/guides/security-cors/index.md#cors-filter) section of the Quarkus "Cross-origin resource sharing" guide.

<a id="csrf-prevention"></a>
### Cross-Site Request Forgery (CSRF) prevention

Quarkus Security provides a Quarkus REST (formerly RESTEasy Reactive) filter that can protect your applications against a [Cross-Site Request Forgery](https://owasp.org/www-community/attacks/csrf) attack. For more information, see the Quarkus [Cross-Site Request Forgery Prevention](/guides/security-csrf-prevention/index.md) guide.

<a id="samesite-cookies"></a>
### SameSite cookies

You can add a [SameSite](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite) cookie property to any of the cookies set by a Quarkus endpoint. For more information, see the [SameSite cookies](/guides/http-reference/index.md#same-site-cookie) section of the Quarkus "HTTP reference" guide.

<a id="secrets-engines"></a>
### Secrets engines

You can use secrets engines with Quarkus to store, generate, or encrypt data.

Quarkus provides additional extensions in Quarkiverse for securely storing credentials, for example, [Quarkus and HashiCorp Vault](https://docs.quarkiverse.io/quarkus-vault/dev/index.html).

## Secrets in environment properties

Quarkus provides support to store secrets in environment properties. For more information, see the Quarkus [store secrets in an environment properties file](/guides/config/index.md#secrets-in-environment-properties) guide.

<a id="secure-serialization"></a>
### Secure serialization

If your Quarkus Security architecture includes Quarkus REST (formerly RESTEasy Reactive) and Jackson, Quarkus can limit the fields included in JSON serialization based on the configured security. For more information, see the [JSON serialization](/guides/rest/index.md#secure-serialization) section of the Quarkus “Writing REST services with Quarkus REST (formerly RESTEasy Reactive)” guide.

<a id="rest-data-panache"></a>
### Secure auto-generated resources by REST Data with Panache

If you use the REST Data with Panache extension to auto-generate your resources, you can still use security annotations within the package `jakarta.annotation.security`. For more information, see the [Securing endpoints](/guides/rest-data-panache/index.md#securing-endpoints) section of the Quarkus "Generating Jakarta REST resources with Panache" guide.

## Security vulnerability detection

Most Quarkus tags get reported in the US [National Vulnerability Database (NVD)](https://nvd.nist.gov). For information about security vulnerabilities, see the [Security vulnerability detection and reporting in Quarkus](/guides/security-vulnerability-detection/index.md) guide.

## References

- [Basic authentication](/guides/security-basic-authentication/index.md)
- [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md)
- [Protect a web application by using OIDC authorization code flow](/guides/security-oidc-code-flow-authentication-tutorial/index.md)
- [Protect a service application by using OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication-tutorial/index.md)
