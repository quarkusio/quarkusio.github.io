# Basic authentication

HTTP Basic authentication is one of the least resource-demanding techniques that enforce access controls to web resources. You can secure your Quarkus application endpoints by using HTTP Basic authentication. Quarkus includes a built-in authentication mechanism for Basic authentication.

Basic authentication uses fields in the HTTP header and does not rely on HTTP cookies, session identifiers, or login pages.

## Authorization header

An HTTP user agent, like a web browser, uses an `Authorization` header to provide a username and password in each HTTP request. The header is specified as `Authorization: Basic <credentials>`, where credentials are the Base64 encoding of the user ID and password, joined by a colon.

If the user name is `Alice` and the password is `secret`, the HTTP authorization header would be `Authorization: Basic QWxpY2U6c2VjcmV0`, where `QWxpY2U6c2VjcmV0` is a Base64 encoded representation of the `Alice:secret` string.

The Basic authentication mechanism does not provide confidentiality protection for the transmitted credentials. The credentials are merely encoded with Base64 when in transit, and not encrypted or hashed in any way. Therefore, to provide confidentiality, use Basic authentication with HTTPS.

Basic authentication is a well-specified, simple challenge and response scheme that all web browsers and most web servers understand.

## Limitations with using Basic authentication

The following table outlines some limitations of using HTTP Basic authentication to secure your Quarkus applications:

**Limitations of HTTP Basic authentication**

| Limitation | Description |
| --- | --- |
| Credentials are sent as plain text | Use HTTPS with Basic authentication to avoid exposing the credentials. The risk of exposing credentials as plain text increases if a load balancer terminates HTTPS because the request is forwarded to Quarkus over HTTP. Furthermore, in multi-hop deployments, the credentials can be exposed if HTTPS is used between the client and the first Quarkus endpoint only, and the credentials are propagated to the next Quarkus endpoint over HTTP. |
| Credentials are sent with each request | In Basic authentication, a username and password must be sent with each request, increasing the risk of exposing credentials. |
| Application complexity increases | The Quarkus application must validate that usernames, passwords, and roles are managed securely. This process, however, can introduce significant complexity to the application. Depending on the use case, other authentication mechanisms that delegate username, password, and role management to specialized services might be more secure. |

## Implementing Basic authentication in Quarkus

For more information about how you can secure your Quarkus applications by using Basic authentication, see the following resources:

- [Enable Basic authentication](/guides/security-basic-authentication-howto/index.md)
- [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md)

## Role-based access control

Quarkus also includes built-in security to allow for role-based access control (RBAC) based on the common security annotations `@RolesAllowed`, `@DenyAll`, `@PermitAll` on REST endpoints and CDI beans. For more information, see the Quarkus [Authorization of web endpoints](/guides/security-authorize-web-endpoints-reference/index.md) guide.

<a id="basic-auth-programmatic-set-up"></a>
## Set up Basic authentication programmatically

The `io.quarkus.vertx.http.security.HttpSecurity` CDI event allows to configure the Basic authentication mechanism programmatically like in the example below:

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class HttpSecurityConfiguration {

    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.basic("QuarkusTestRealm"); <1>
    }

}
```

1. Enable the Basic authentication and use the `QuarkusTestRealm` authentication realm.

> [!NOTE]
> If you generate an OpenAPI schema document with the SmallRye OpenAPI extension, the Basic authentication must be enabled during the build-time with the `quarkus.http.auth.basic` configuration property.

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
- [Quarkus Security architecture](/guides/security-architecture/index.md)
- [Other supported authentication mechanisms](/guides/security-authentication-mechanisms/index.md#other-supported-authentication-mechanisms)
- [Identity providers](/guides/security-identity-providers/index.md)
- [Authorization of web endpoints](/guides/security-authorize-web-endpoints-reference/index.md)
