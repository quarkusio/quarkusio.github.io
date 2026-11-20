# Cross-Site Request Forgery prevention

[Cross-Site Request Forgery (CSRF)](https://owasp.org/www-community/attacks/csrf) is an attack that forces an end user to execute unwanted actions on a web application in which they are currently authenticated.

Quarkus Security provides a CSRF prevention feature which implements [Double Submit Cookie](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html#double-submit-cookie) and [CSRF Request Header](#csrf-request-header) techniques.

`Double Submit Cookie` technique requires that the CSRF token sent as `HTTPOnly`, optionally signed, cookie to the client, and directly embedded in a hidden form input of server-side rendered HTML forms, or submitted as a request header value.

> [!NOTE]
> If you are looking for stateless CSRF prevention that does not involve the server creating a cookie, have a look at the [CORS filter](/guides/security-cors/index.md#cors-filter). Despite its name it also offers CSRF prevention by checking whether a request's `Origin` either matches the target `Host` or is in a list of allowed origins server-side.

The extension consists of a [Quarkus REST (formerly RESTEasy Reactive)](/guides/rest/index.md) server filter which creates and verifies CSRF tokens in `application/x-www-form-urlencoded` and `multipart/form-data` forms and a Qute HTML form parameter provider which supports the [injection of CSRF tokens in Qute templates](/guides/qute-reference/index.md#injecting-beans-directly-in-templates).

The CSRF prevention filter applies to requests using HTTP `POST`, `PUT`, `PATCH`, `DELETE` and other methods that can change the REST application state.

## Creating the Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-csrf-prevention \
    --extension='rest-csrf' \
    --no-code
cd security-csrf-prevention
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-csrf-prevention \
    -Dextensions='rest-csrf' \
    -DnoCode
cd security-csrf-prevention
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-csrf-prevention"`

This command generates a project which imports the `rest-csrf` extension.

If you already have your Quarkus project configured, you can add the `rest-csrf` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add rest-csrf
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='rest-csrf'
```

**Gradle**

```bash
./gradlew addExtension --extensions='rest-csrf'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-csrf</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-rest-csrf")
```

Next, let's add a `csrfToken.html` Qute template producing an HTML form in the `src/main/resources/templates` folder:

```html
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>User Name Input</title>
</head>
<body>
    <h1>User Name Input</h1>

    <form action="/service/csrfTokenForm" method="post">
    	<input type="hidden" name="{inject:csrf.parameterName}" value="{inject:csrf.token}" /> <1>

    	<p>Your Name: <input type="text" name="name" /></p>
    	<p><input type="submit" name="submit"/></p>
    </form>
</body>
</html>
```

1. This expression is used to inject a CSRF token into a hidden form field. This token will be verified by the CSRF filter against a CSRF cookie.

Now let's create a resource class which returns an HTML form and handles form POST requests:

```java
package io.quarkus.it.csrf;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/service")
public class UserNameResource {

    @Inject
    Template csrfToken; <1>

    @GET
    @Path("/csrfTokenForm")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenForm() {
        return csrfToken.instance(); <2>
    }

    @POST
    @Path("/csrfTokenForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenForm(@FormParam("name") String userName) {
        return userName; <3>
    }
}
```

1. Inject the `csrfToken.html` as a `Template`.
2. Return the HTML form with a hidden form field containing a CSRF token created by the CSRF filter.
3. Handle the POST form request, this method can only be invoked if the CSRF filter has successfully verified the token.

The form POST request will fail with HTTP status `400` if the filter finds the hidden CSRF form field is missing, the CSRF cookie is missing, or if the CSRF form field and CSRF cookie values do not match.

At this stage no additional configuration is needed - by default the CSRF form field and cookie name will be set to `csrf-token`, and the filter will verify the token. But you can change these names if you would like:

```properties
quarkus.rest-csrf.form-field-name=csrftoken
quarkus.rest-csrf.cookie-name=csrftoken
```

## Sign CSRF token

You can get `HMAC` signatures created for the generated CSRF tokens and have these `HMAC` values stored as CSRF token cookies if you would like to avoid the risk of the attackers recreating the CSRF cookie token. All you need to do is to configure a token signature secret which must be at least 32 characters long:

```properties
quarkus.rest-csrf.token-signature-key=AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow
```

<a id="csrf-request-header"></a>
## CSRF Request Header

If HTML `form` tags are not used and you need to pass CSRF token as a header, then inject the header name and token, for example, into HTMX:

```html
<body hx-headers='{"{inject:csrf.headerName}":"{inject:csrf.token}"}'> <1>
</body>
```

1. This expression is used to inject a CSRF token header and token. This token will be verified by the CSRF filter against a CSRF cookie.

Default header name is `X-CSRF-TOKEN`, you can customize it with `quarkus.rest-csrf.token-header-name`, for example:

```properties
quarkus.rest-csrf.token-header-name=CUSTOM-X-CSRF-TOKEN
```

If you need to access the CSRF cookie from JavaScript in order to pass its value as a header, use `{inject:csrf.cookieName}` and `{inject:csrf.headerName}` to inject the cookie name which has to be read as a CSRF header value and allow accessing this cookie:

```properties
quarkus.rest-csrf.cookie-http-only=false
```

## Cross-origin resource sharing

> [!NOTE]
> If you would like to enforce CSRF prevention in a Cross-origin environment, please avoid supporting all Origins.
>
> Restrict supported Origins to trusted Origins only, see [CORS filter](/guides/security-cors/index.md#cors-filter) section of the "Cross-origin resource sharing" guide for more information.

## Restrict CSRF token verification

Your Jakarta REST endpoint may accept not only HTTP POST requests with `application/x-www-form-urlencoded` or `multipart/form-data` payloads but also payloads with other media types, either on the same or different URL paths, and therefore you would like to avoid verifying the CSRF token in such cases, for example:

```java
package io.quarkus.it.csrf;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/service")
public class UserNameResource {

    @Inject
    Template csrfToken;

    @GET
    @Path("/user")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenForm() {
        return csrfToken.instance();
    }

 <1>
    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenForm(@FormParam("name") String userName) {
        return userName;
    }

 <2>
    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String postJson(User user) {
        return user.name;
    }

 <3>
    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String postJson(User user) {
        return user.name;
    }

    public static class User {
        private String name;
        public String getName() {
            return this.name;
        }
        public void setName(String name) {
            this.name = name;
        }
    }
}
```

1. POST form request to `/user`, CSRF token verification is enforced by the CSRF filter
2. POST json request to `/user`, CSRF token verification is not needed
3. POST json request to `/users`, CSRF token verification is not needed

As you can see a CSRF token verification will be required at the `/service/user` path accepting the `application/x-www-form-urlencoded` payload, but `User` JSON representation posted to both `/service/user` and `/service/users` method will have no CSRF token and therefore the token verification has to be skipped in these cases by restricting it to the specific `/service/user` request path but also allowing not only `application/x-www-form-urlencoded` on this path:

```properties
# Verify CSRF token only for the `/service/user` path, ignore other paths such as `/service/users`
quarkus.rest-csrf.create-token-path=/service/user

# If `/service/user` path accepts not only `application/x-www-form-urlencoded` payloads but also other ones such as JSON then allow them
# Setting this property is not necessary when the token is submitted as a header value
quarkus.rest-csrf.require-form-url-encoded=false
```

## Verify CSRF token in the application code

If you prefer to compare the CSRF form field and cookie values in the application code then you can do it as follows:

```java
package io.quarkus.it.csrf;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/service")
public class UserNameResource {

    @Inject
    Template csrfToken;

    @GET
    @Path("/csrfTokenForm")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getCsrfTokenForm() {
        return csrfToken.instance();
    }

    @POST
    @Path("/csrfTokenForm")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public String postCsrfTokenForm(@CookieParam("csrf-token") Cookie csrfCookie, @FormParam("csrf-token") String formCsrfToken, @FormParam("name") String userName) {
        if (!csrfCookie.getValue().equals(formCsrfToken)) { <1>
            throw new BadRequestException();
        }
        return userName;
    }
}
```

1. Compare the CSRF form field and cookie values and fail with HTTP status `400` if they don't match.

Also disable the token verification in the filter:

```properties
quarkus.rest-csrf.verify-token=false
```

<a id="csrf-reactive-configuration-reference"></a>
## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-rest-csrf_quarkus-rest-csrf-enabled"></a> [`quarkus.rest-csrf.enabled`](#quarkus-rest-csrf_quarkus-rest-csrf-enabled) config_property_copy_button:+++quarkus.rest-csrf.enabled+++[]<br><br>If filter is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-form-field-name"></a> [`quarkus.rest-csrf.form-field-name`](#quarkus-rest-csrf_quarkus-rest-csrf-form-field-name) config_property_copy_button:+++quarkus.rest-csrf.form-field-name+++[]<br><br>Form field name which keeps a CSRF token.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_FORM_FIELD_NAME+++[] | string | `csrf-token` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-token-header-name"></a> [`quarkus.rest-csrf.token-header-name`](#quarkus-rest-csrf_quarkus-rest-csrf-token-header-name) config_property_copy_button:+++quarkus.rest-csrf.token-header-name+++[]<br><br>Token header which can provide a CSRF token.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_TOKEN_HEADER_NAME+++[] | string | `X-CSRF-TOKEN` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-cookie-name"></a> [`quarkus.rest-csrf.cookie-name`](#quarkus-rest-csrf_quarkus-rest-csrf-cookie-name) config_property_copy_button:+++quarkus.rest-csrf.cookie-name+++[]<br><br>CSRF cookie name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_COOKIE_NAME+++[] | string | `csrf-token` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-cookie-max-age"></a> [`quarkus.rest-csrf.cookie-max-age`](#quarkus-rest-csrf_quarkus-rest-csrf-cookie-max-age) config_property_copy_button:+++quarkus.rest-csrf.cookie-max-age+++[]<br><br>CSRF cookie max age.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_COOKIE_MAX_AGE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-rest-csrf_quarkus-rest-csrf) | `2H` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-cookie-path"></a> [`quarkus.rest-csrf.cookie-path`](#quarkus-rest-csrf_quarkus-rest-csrf-cookie-path) config_property_copy_button:+++quarkus.rest-csrf.cookie-path+++[]<br><br>CSRF cookie path.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_COOKIE_PATH+++[] | string | `/` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-cookie-domain"></a> [`quarkus.rest-csrf.cookie-domain`](#quarkus-rest-csrf_quarkus-rest-csrf-cookie-domain) config_property_copy_button:+++quarkus.rest-csrf.cookie-domain+++[]<br><br>CSRF cookie domain.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_COOKIE_DOMAIN+++[] | string |  |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-cookie-force-secure"></a> [`quarkus.rest-csrf.cookie-force-secure`](#quarkus-rest-csrf_quarkus-rest-csrf-cookie-force-secure) config_property_copy_button:+++quarkus.rest-csrf.cookie-force-secure+++[]<br><br>If enabled the CSRF cookie will have its 'secure' parameter set to 'true' when HTTP is used. It may be necessary when running behind an SSL terminating reverse proxy. The cookie will always be secure if HTTPS is used even if this property is set to false.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_COOKIE_FORCE_SECURE+++[] | boolean | `false` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-cookie-http-only"></a> [`quarkus.rest-csrf.cookie-http-only`](#quarkus-rest-csrf_quarkus-rest-csrf-cookie-http-only) config_property_copy_button:+++quarkus.rest-csrf.cookie-http-only+++[]<br><br>Set the HttpOnly attribute to prevent access to the cookie via JavaScript.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_COOKIE_HTTP_ONLY+++[] | boolean | `true` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-create-token-path"></a> [`quarkus.rest-csrf.create-token-path`](#quarkus-rest-csrf_quarkus-rest-csrf-create-token-path) config_property_copy_button:+++quarkus.rest-csrf.create-token-path+++[]<br><br>Create CSRF token only if the HTTP GET relative request path matches one of the paths configured with this property. Use a comma to separate multiple path values.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_CREATE_TOKEN_PATH+++[] | list of string |  |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-token-size"></a> [`quarkus.rest-csrf.token-size`](#quarkus-rest-csrf_quarkus-rest-csrf-token-size) config_property_copy_button:+++quarkus.rest-csrf.token-size+++[]<br><br>Random CSRF token size in bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_TOKEN_SIZE+++[] | int | `16` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-token-signature-key"></a> [`quarkus.rest-csrf.token-signature-key`](#quarkus-rest-csrf_quarkus-rest-csrf-token-signature-key) config_property_copy_button:+++quarkus.rest-csrf.token-signature-key+++[]<br><br>CSRF token HMAC signature key, if this key is set then it must be at least 32 characters long.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_TOKEN_SIGNATURE_KEY+++[] | string |  |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-verify-token"></a> [`quarkus.rest-csrf.verify-token`](#quarkus-rest-csrf_quarkus-rest-csrf-verify-token) config_property_copy_button:+++quarkus.rest-csrf.verify-token+++[]<br><br>Verify CSRF token in the CSRF filter. If you prefer then you can disable this property and compare CSRF form and cookie parameters in the application code using JAX-RS jakarta.ws.rs.FormParam which refers to the `form-field-name` form property and jakarta.ws.rs.CookieParam which refers to the `RestCsrfConfig#cookieName` cookie. Note that even if the CSRF token verification in the CSRF filter is disabled, the filter will still perform checks to ensure the token is available, has the correct `token-size` in bytes and that the Content-Type HTTP header is either 'application/x-www-form-urlencoded' or 'multipart/form-data'.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_VERIFY_TOKEN+++[] | boolean | `true` |
| <a id="quarkus-rest-csrf_quarkus-rest-csrf-require-form-url-encoded"></a> [`quarkus.rest-csrf.require-form-url-encoded`](#quarkus-rest-csrf_quarkus-rest-csrf-require-form-url-encoded) config_property_copy_button:+++quarkus.rest-csrf.require-form-url-encoded+++[]<br><br>Require that only 'application/x-www-form-urlencoded' or 'multipart/form-data' body is accepted for the token verification to proceed. Disable this property for the CSRF filter to avoid verifying the token for POST requests with other content types. This property is only effective if `verify-token` property is enabled and `token-header-name` is not configured.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CSRF_REQUIRE_FORM_URL_ENCODED+++[] | boolean | `true` |

<a id="duration-note-anchor-quarkus-rest-csrf_quarkus-rest-csrf"></a>

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

<a id="csrf-prevention-programmatic-set-up"></a>
## Configuring the CSRF prevention programmatically

When the `quarkus-rest-csrf` extension is used together with the `quarkus-security` extension, the `io.quarkus.vertx.http.security.HttpSecurity` CDI event allows you to customize the CSRF prevention programmatically:

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.CSRF;
import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class CsrfProgrammaticConfig {
    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.csrf(CSRF.builder() <1>
                .formFieldName("my-csrf-token")
                .tokenSize(32)
                .build());
    }
}
```

1. Creates the CSRF prevention configuration builder provided by the `quarkus-rest-csrf` extension. If this extension is missing, call to the `CSRF.builder()` will fail.

## References

- [OWASP Cross-Site Request Forgery](https://owasp.org/www-community/attacks/csrf)
- [Quarkus REST](/guides/rest/index.md)
- [Qute Reference](/guides/qute-reference/index.md)
- [Cross-origin resource sharing](/guides/security-cors/index.md#cors-filter)
- [Quarkus Security overview](/guides/security-overview/index.md)
