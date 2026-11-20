# Authentication mechanisms in Quarkus

The Quarkus Security framework supports multiple authentication mechanisms, which you can use to secure your applications. You can also combine authentication mechanisms.

> [!TIP]
> Before you choose an authentication mechanism for securing your Quarkus applications, review the information provided.

## Overview of supported authentication mechanisms

Some supported authentication mechanisms are built into Quarkus, while others require you to add an extension. All of these mechanisms are detailed in the following sections:

- [Built-in authentication mechanisms](#built-in-authentication-mechanisms)
- [Other supported authentication mechanisms](#other-supported-authentication-mechanisms)

The following table maps specific authentication requirements to a supported mechanism that you can use in Quarkus:

**Authentication requirements and mechanisms**

| Authentication requirement | Authentication mechanism |
| --- | --- |
| Username and password | [Basic](/guides/security-basic-authentication/index.md), [Form-based authentication](#form-auth) |
| Bearer access token | [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md), [JWT](/guides/security-jwt/index.md) , [OAuth2](/guides/security-oauth2/index.md) |
| Single sign-on (SSO) | [OIDC Code Flow](/guides/security-oidc-code-flow-authentication/index.md), [Form-based authentication](#form-auth) |
| Client certificate | [Mutual TLS authentication](#mutual-tls) |
| WebAuthn | [WebAuthn](/guides/security-webauthn/index.md) |
| Kerberos ticket | [Kerberos](https://docs.quarkiverse.io/quarkus-kerberos/dev/index.html) |

For more information, see the following [Token authentication mechanism comparison](#table) table.

<a id="built-in-authentication-mechanisms"></a>
## Built-in authentication mechanisms

Quarkus Security provides the following built-in authentication support:

- [Basic authentication](/guides/security-basic-authentication/index.md)
- [Form-based authentication](#form-auth)
- [Mutual TLS authentication](#mutual-tls)

<a id="basic-auth"></a>
### Basic authentication

You can secure your Quarkus application endpoints with the built-in HTTP Basic authentication mechanism. For more information, see the following documentation:

- [Basic authentication](/guides/security-basic-authentication/index.md)
- 
  - [Enable Basic authentication](/guides/security-basic-authentication-howto/index.md)
- [Quarkus Security with Jakarta Persistence](/guides/security-jpa/index.md)
- 
  - [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md)
- [Identity providers](/guides/security-identity-providers/index.md)

<a id="form-auth"></a>
### Form-based authentication

Quarkus provides form-based authentication that works similarly to traditional Servlet form-based authentication. Unlike traditional form authentication, the authenticated user is not stored in an HTTP session because Quarkus does not support clustered HTTP sessions. Instead, the authentication information is stored in an encrypted cookie, which can be read by all cluster members who share the same encryption key.

To apply encryption, add the `quarkus.http.auth.session.encryption-key` property, and ensure the value you set is at least 16 characters long. The encryption key is hashed by using SHA-256. The resulting digest is used as a key for AES-256 encryption of the cookie value. The cookie contains an expiry time as part of the encrypted value, so all nodes in the cluster must have their clocks synchronized. At one-minute intervals, a new cookie gets generated with an updated expiry time if the session is in use.

To get started with form authentication, you should have similar settings as described in [Enable Basic authentication](/guides/security-basic-authentication-howto/index.md) and property `quarkus.http.auth.form.enabled` must be set to `true`.

Simple `application.properties` with form-base authentication can look similar to this:

```properties
quarkus.http.auth.form.enabled=true

quarkus.http.auth.form.login-page=login.html
quarkus.http.auth.form.landing-page=hello
quarkus.http.auth.form.error-page=

# Define testing user
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.alice=alice
quarkus.security.users.embedded.roles.alice=user
```

> [!IMPORTANT]
> Configuring user names, secrets, and roles in the application.properties file is appropriate only for testing scenarios. For securing a production application, it is crucial to use a database or LDAP to store this information. For more information you can take a look at [Quarkus Security with Jakarta Persistence](/guides/security-jpa/index.md) or other mentioned in [Enable Basic authentication](/guides/security-basic-authentication-howto/index.md).

and application login page will contain HTML form similar to this:

```html
<form action="/j_security_check" method="post">
    <label>Username</label>
    <input type="text" placeholder="Username" name="j_username" required>
    <label>Password</label>
    <input type="password" placeholder="Password" name="j_password" required>
    <button type="submit">Login</button>
</form>
```

With single-page applications (SPA), you typically want to avoid redirects by removing default page paths, as shown in the following example:

```properties
# do not redirect, respond with HTTP 200 OK
quarkus.http.auth.form.landing-page=

# do not redirect, respond with HTTP 401 Unauthorized
quarkus.http.auth.form.login-page=
quarkus.http.auth.form.error-page=

# HttpOnly must be false if you want to log out on the client; it can be true if logging out from the server
quarkus.http.auth.form.http-only-cookie=false
```

Now that you have disabled redirects for the SPA, you must log in and log out programmatically from your client. Below are examples of JavaScript methods for logging into the `j_security_check` endpoint and logging out of the application by destroying the cookie.

```javascript
const login = () => {
    // Create an object to represent the form data
    const formData = new URLSearchParams();
    formData.append("j_username", username);
    formData.append("j_password", password);

    // Make an HTTP POST request using fetch against j_security_check endpoint
    fetch("j_security_check", {
        method: "POST",
        body: formData,
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        },
    })
    .then((response) => {
        if (response.status === 200) {
            // Authentication was successful
            console.log("Authentication successful");
        } else {
            // Authentication failed
            console.error("Invalid credentials");
        }
    })
    .catch((error) => {
        console.error(error);
    });
};
```

To log out of the SPA from the client, the cookie must be set to `quarkus.http.auth.form.http-only-cookie=false` so you can destroy the cookie and possibly redirect back to your main page.

```javascript
const logout= () => {
    // delete the credential cookie, essentially killing the session
    const removeCookie = `quarkus-credential=; Max-Age=0;path=/`;
    document.cookie = removeCookie;

    // perform post-logout actions here, such as redirecting back to your login page
};
```

To log out of the SPA from the server, the cookie can be set to `quarkus.http.auth.form.http-only-cookie=true` and use this example code to destroy the cookie.

```java
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.POST;

@Inject
SecurityIdentity identity;

@POST
public Response logout() {
    if (identity.isAnonymous()) {
        throw new UnauthorizedException("Not authenticated");
    }
    FormAuthenticationMechanism.logout(identity); <1>
    return Response.noContent().build();
}
```

1. Perform the logout by removing the session cookie.

<a id="form-configuration-properties"></a>
#### Form-based authentication configuration reference

The following properties can be used to configure form-based authentication:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-enabled"></a> [`quarkus.http.auth.permission."permissions".enabled`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-enabled) config_property_copy_button:+++quarkus.http.auth.permission."permissions".enabled+++[]<br><br>Determines whether the entire permission set is enabled, or not.<br><br>By default, if the permission set is defined, it is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__ENABLED+++[] | boolean |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-policy"></a> [`quarkus.http.auth.permission."permissions".policy`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-policy) config_property_copy_button:+++quarkus.http.auth.permission."permissions".policy+++[]<br><br>The HTTP policy that this permission set is linked to.<br><br>There are three built-in policies: permit, deny and authenticated. Role based policies can be defined, and extensions can add their own policies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__POLICY+++[] | string | required [exclamation circle] |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-methods"></a> [`quarkus.http.auth.permission."permissions".methods`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-methods) config_property_copy_button:+++quarkus.http.auth.permission."permissions".methods+++[]<br><br>The methods that this permission set applies to. If this is not set then they apply to all methods.<br><br>Note that if a request matches any path from any permission set, but does not match the constraint due to the method not being listed then the request will be denied.<br><br>Method specific permissions take precedence over matches that do not have any methods set.<br><br>This means that for example if Quarkus is configured to allow GET and POST requests to /admin to and no other permissions are configured PUT requests to /admin will be denied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__METHODS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-paths"></a> [`quarkus.http.auth.permission."permissions".paths`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-paths) config_property_copy_button:+++quarkus.http.auth.permission."permissions".paths+++[]<br><br>The paths that this permission check applies to. If the path ends in /++\*++ then this is treated as a path prefix, otherwise it is treated as an exact match.<br><br>Matches are done on a length basis, so the most specific path match takes precedence.<br><br>If multiple permission sets match the same path then explicit methods matches take precedence over matches without methods set, otherwise the most restrictive permissions are applied.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__PATHS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-auth-mechanism"></a> [`quarkus.http.auth.permission."permissions".auth-mechanism`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-auth-mechanism) config_property_copy_button:+++quarkus.http.auth.permission."permissions".auth-mechanism+++[]<br><br>One or more path specific authentication mechanisms separated by comma. Every listed value needs to match `io.quarkus.vertx.http.runtime.security.HttpCredentialTransport` authentication scheme such as 'basic', 'bearer', 'form', etc. By default, only one of the matching authentication mechanisms will produce a `SecurityIdentity`, and all matching authentication mechanisms will attempt to authenticate when an inclusive authentication is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__AUTH_MECHANISM+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-shared"></a> [`quarkus.http.auth.permission."permissions".shared`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-shared) config_property_copy_button:+++quarkus.http.auth.permission."permissions".shared+++[]<br><br>Indicates that this policy always applies to the matched paths in addition to the policy with a winning path. Avoid creating more than one shared policy to minimize the performance impact.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__SHARED+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-applies-to"></a> [`quarkus.http.auth.permission."permissions".applies-to`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-permission-permissions-applies-to) config_property_copy_button:+++quarkus.http.auth.permission."permissions".applies-to+++[]<br><br>Whether permission check should be applied on all matching paths, or paths specific for the Jakarta REST resources.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_PERMISSION__PERMISSIONS__APPLIES_TO+++[] | tooltip:all[Apply on all matching paths.], tooltip:jaxrs[Declares that a permission check must only be applied on the Jakarta REST request paths. Use this option to delay the permission check if an authentication mechanism is chosen with an annotation on the matching Jakarta REST endpoint. This option must be set if the following REST endpoint annotations are used\:   * `io.quarkus.oidc.Tenant` annotation which selects an OIDC authentication mechanism with a tenant identifier  * `io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication` which selects the Basic authentication mechanism  * `io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication` which selects the Form-based authentication mechanism  * `io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication` which selects the mTLS authentication mechanism  * `io.quarkus.security.webauthn.WebAuthn` which selects the WebAuth authentication mechanism  * `io.quarkus.oidc.BearerTokenAuthentication` which selects the OpenID Connect Bearer token authentication mechanism  * `io.quarkus.oidc.AuthorizationCodeFlow` which selects the OpenID Connect Code authentication mechanism] | tooltip:all[Apply on all matching paths.] |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-roles-allowed"></a> [`quarkus.http.auth.policy."role-policy".roles-allowed`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-roles-allowed) config_property_copy_button:+++quarkus.http.auth.policy."role-policy".roles-allowed+++[]<br><br>The roles that are allowed to access resources protected by this policy. By default, access is allowed to any authenticated user.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_POLICY__ROLE_POLICY__ROLES_ALLOWED+++[] | list of string | `**` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-roles-role-name"></a> [`quarkus.http.auth.policy."role-policy".roles."role-name"`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-roles-role-name) config_property_copy_button:+++quarkus.http.auth.policy."role-policy".roles."role-name"+++[]<br><br>Add roles granted to the `SecurityIdentity` based on the roles that the `SecurityIdentity` already have. For example, the Quarkus OIDC extension can map roles from the verified JWT access token, and you may want to remap them to a deployment specific roles.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_POLICY__ROLE_POLICY__ROLES__ROLE_NAME_+++[] | Map\<String,List\<String>> |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-permissions-role-name"></a> [`quarkus.http.auth.policy."role-policy".permissions."role-name"`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-permissions-role-name) config_property_copy_button:+++quarkus.http.auth.policy."role-policy".permissions."role-name"+++[]<br><br>Permissions granted to the `SecurityIdentity` if this policy is applied successfully (the policy allows request to proceed) and the authenticated request has required role. For example, you can map permission `perm1` with actions `action1` and `action2` to role `admin` by setting `quarkus.http.auth.policy.role-policy1.permissions.admin=perm1:action1,perm1:action2` configuration property. Granted permissions are used for authorization with the `@PermissionsAllowed` annotation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_POLICY__ROLE_POLICY__PERMISSIONS__ROLE_NAME_+++[] | Map\<String,List\<String>> |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-permission-class"></a> [`quarkus.http.auth.policy."role-policy".permission-class`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-policy-role-policy-permission-class) config_property_copy_button:+++quarkus.http.auth.policy."role-policy".permission-class+++[]<br><br>Permissions granted by this policy will be created with a `java.security.Permission` implementation specified by this configuration property. The permission class must declare exactly one constructor that accepts permission name (`String`) or permission name and actions (`String`, `String[]`). Permission class must be registered for reflection if you run your application in a native mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_POLICY__ROLE_POLICY__PERMISSION_CLASS+++[] | string | `io.quarkus.security.StringPermission` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-roles-mapping-role-name"></a> [`quarkus.http.auth.roles-mapping."role-name"`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-roles-mapping-role-name) config_property_copy_button:+++quarkus.http.auth.roles-mapping."role-name"+++[]<br><br>Map the `SecurityIdentity` roles to deployment specific roles and add the matching roles to `SecurityIdentity`.<br><br>For example, if `SecurityIdentity` has a `user` role and the endpoint is secured with a 'UserRole' role, use this property to map the `user` role to the `UserRole` role, and have `SecurityIdentity` to have both `user` and `UserRole` roles.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_ROLES_MAPPING__ROLE_NAME_+++[] | Map\<String,List\<String>> |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-certificate-role-attribute"></a> [`quarkus.http.auth.certificate-role-attribute`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-certificate-role-attribute) config_property_copy_button:+++quarkus.http.auth.certificate-role-attribute+++[]<br><br>Client certificate attribute whose values are going to be mapped to the 'SecurityIdentity' roles according to the roles mapping specified in the certificate properties file. The attribute must be either one of the Relative Distinguished Names (RDNs) or Subject Alternative Names (SANs). By default, the Common Name (CN) attribute value is used for roles mapping. Supported values are:<br><br>\* RDN type - Distinguished Name field. For example 'CN' represents Common Name field. Multivalued RNDs and multiple instances of the same attributes are currently not supported. \* 'SAN_RFC822' - Subject Alternative Name field RFC 822 Name. \* 'SAN_URI' - Subject Alternative Name field Uniform Resource Identifier (URI). \* 'SAN_ANY' - Subject Alternative Name field Other Name. Please note that only simple case of UTF8 identifier mapping is supported. For example, you can map 'other-identifier' to the SecurityIdentity roles. If you use 'openssl' tool, supported Other name definition would look like this: `subjectAltName=otherName:1.2.3.4;UTF8:other-identifier`<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_CERTIFICATE_ROLE_ATTRIBUTE+++[] | string | `CN` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-certificate-role-properties"></a> [`quarkus.http.auth.certificate-role-properties`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-certificate-role-properties) config_property_copy_button:+++quarkus.http.auth.certificate-role-properties+++[]<br><br>Properties file containing the client certificate attribute value to role mappings. Use it only if the mTLS authentication mechanism is enabled with either `quarkus.http.ssl.client-auth=required` or `quarkus.http.ssl.client-auth=request`.<br><br>Properties file is expected to have the `CN_VALUE=role1,role,...,roleN` format and should be encoded using UTF-8.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_CERTIFICATE_ROLE_PROPERTIES+++[] | path |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-realm"></a> [`quarkus.http.auth.realm`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-realm) config_property_copy_button:+++quarkus.http.auth.realm+++[]<br><br>The authentication realm used for Basic authentication. Legacy HTTP clients may fail to parse a Basic authentication challenge when this property is not set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_REALM+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-login-page"></a> [`quarkus.http.auth.form.login-page`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-login-page) config_property_copy_button:+++quarkus.http.auth.form.login-page+++[]<br><br>The login page. Redirect to login page can be disabled by setting `quarkus.http.auth.form.login-page=`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_LOGIN_PAGE+++[] | string | `/login.html` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-login-page-query-params"></a> [`quarkus.http.auth.form.login-page-query-params`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-login-page-query-params) config_property_copy_button:+++quarkus.http.auth.form.login-page-query-params+++[]<br><br>List of query parameters that should be passed through when Quarkus redirects requests to the login page. For example, if a request to the `/secured-path?query_param=1&other_query_param=2` is redirected on the authentication failure to the login page and this property is set to the `other_query_param`, then the resulting redirect path will be `/login.html?other_query_param=2`. By default, no request query parameters are passed through.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_LOGIN_PAGE_QUERY_PARAMS+++[] | list of string |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-username-parameter"></a> [`quarkus.http.auth.form.username-parameter`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-username-parameter) config_property_copy_button:+++quarkus.http.auth.form.username-parameter+++[]<br><br>The username field name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_USERNAME_PARAMETER+++[] | string | `j_username` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-password-parameter"></a> [`quarkus.http.auth.form.password-parameter`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-password-parameter) config_property_copy_button:+++quarkus.http.auth.form.password-parameter+++[]<br><br>The password field name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_PASSWORD_PARAMETER+++[] | string | `j_password` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-error-page"></a> [`quarkus.http.auth.form.error-page`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-error-page) config_property_copy_button:+++quarkus.http.auth.form.error-page+++[]<br><br>The error page. Redirect to error page can be disabled by setting `quarkus.http.auth.form.error-page=`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_ERROR_PAGE+++[] | string | `/error.html` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-error-page-query-params"></a> [`quarkus.http.auth.form.error-page-query-params`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-error-page-query-params) config_property_copy_button:+++quarkus.http.auth.form.error-page-query-params+++[]<br><br>List of query parameters that should be passed through when Quarkus redirects requests to the error page. For example, if the POST request to the `/j_security_check?query_param=1&other_query_param=2` is redirected on the authentication failure to the error page and this property is set to the `other_query_param`, then the resulting redirect path will be `/error.html?other_query_param=2`. By default, only query parameters configured with the `quarkus.http.auth.form.login-page-query-params` property are passed through.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_ERROR_PAGE_QUERY_PARAMS+++[] | list of string | `${quarkus.http.auth.form.login-page-query-params:}` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-landing-page"></a> [`quarkus.http.auth.form.landing-page`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-landing-page) config_property_copy_button:+++quarkus.http.auth.form.landing-page+++[]<br><br>The landing page to redirect to if there is no saved page to redirect back to. Redirect to landing page can be disabled by setting `quarkus.http.auth.form.landing-page=`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_LANDING_PAGE+++[] | string | `/index.html` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-landing-page-query-params"></a> [`quarkus.http.auth.form.landing-page-query-params`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-landing-page-query-params) config_property_copy_button:+++quarkus.http.auth.form.landing-page-query-params+++[]<br><br>List of query parameters that should be passed through when Quarkus redirects requests to the landing page. For example, if the POST request to the `/j_security_check?query_param=1&other_query_param=2` is redirected on the authentication success to the landing page and this property is set to the `other_query_param`, then the resulting redirect path will be `/index.html?other_query_param=2`. By default, only query parameters configured with the `quarkus.http.auth.form.login-page-query-params` property are passed through.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_LANDING_PAGE_QUERY_PARAMS+++[] | list of string | `${quarkus.http.auth.form.login-page-query-params:}` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-location-cookie"></a> [`quarkus.http.auth.form.location-cookie`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-location-cookie) config_property_copy_button:+++quarkus.http.auth.form.location-cookie+++[]<br><br>Option to control the name of the cookie used to redirect the user back to the location they want to access.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_LOCATION_COOKIE+++[] | string | `quarkus-redirect-location` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-timeout"></a> [`quarkus.http.auth.form.timeout`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-timeout) config_property_copy_button:+++quarkus.http.auth.form.timeout+++[]<br><br>The inactivity (idle) timeout<br><br>When inactivity timeout is reached, cookie is not renewed and a new login is enforced.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-auth) | `30M` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-new-cookie-interval"></a> [`quarkus.http.auth.form.new-cookie-interval`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-new-cookie-interval) config_property_copy_button:+++quarkus.http.auth.form.new-cookie-interval+++[]<br><br>How old a cookie can get before it will be replaced with a new cookie with an updated timeout, also referred to as "renewal-timeout".<br><br>Note that smaller values will result in slightly more server load (as new encrypted cookies will be generated more often); however, larger values affect the inactivity timeout because the timeout is set when a cookie is generated.<br><br>For example if this is set to 10 minutes, and the inactivity timeout is 30m, if a user's last request is when the cookie is 9m old then the actual timeout will happen 21m after the last request because the timeout is only refreshed when a new cookie is generated.<br><br>That is, no timeout is tracked on the server side; the timestamp is encoded and encrypted in the cookie itself, and it is decrypted and parsed with each request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_NEW_COOKIE_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-auth) | `1M` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-name"></a> [`quarkus.http.auth.form.cookie-name`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-name) config_property_copy_button:+++quarkus.http.auth.form.cookie-name+++[]<br><br>The cookie that is used to store the persistent session<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_COOKIE_NAME+++[] | string | `quarkus-credential` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-path"></a> [`quarkus.http.auth.form.cookie-path`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-path) config_property_copy_button:+++quarkus.http.auth.form.cookie-path+++[]<br><br>The cookie path for the session and location cookies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_COOKIE_PATH+++[] | string | `/` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-domain"></a> [`quarkus.http.auth.form.cookie-domain`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-domain) config_property_copy_button:+++quarkus.http.auth.form.cookie-domain+++[]<br><br>Cookie domain parameter value which, if set, will be used for the session and location cookies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_COOKIE_DOMAIN+++[] | string |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-http-only-cookie"></a> [`quarkus.http.auth.form.http-only-cookie`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-http-only-cookie) config_property_copy_button:+++quarkus.http.auth.form.http-only-cookie+++[]<br><br>Set the HttpOnly attribute to prevent access to the cookie via JavaScript.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_HTTP_ONLY_COOKIE+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-same-site"></a> [`quarkus.http.auth.form.cookie-same-site`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-same-site) config_property_copy_button:+++quarkus.http.auth.form.cookie-same-site+++[]<br><br>SameSite attribute for the session and location cookies.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_COOKIE_SAME_SITE+++[] | `strict`, `lax`, `none` | `strict` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-max-age"></a> [`quarkus.http.auth.form.cookie-max-age`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-cookie-max-age) config_property_copy_button:+++quarkus.http.auth.form.cookie-max-age+++[]<br><br>Max-Age attribute for the session cookie. This is the amount of time the browser will keep the cookie.<br><br>The default value is empty, which means the cookie will be kept until the browser is closed.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_COOKIE_MAX_AGE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-vertx-http_quarkus-http-auth) |  |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-post-location"></a> [`quarkus.http.auth.form.post-location`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-post-location) config_property_copy_button:+++quarkus.http.auth.form.post-location+++[]<br><br>The post location.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_POST_LOCATION+++[] | string | `/j_security_check` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-priority"></a> [`quarkus.http.auth.form.priority`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-form-priority) config_property_copy_button:+++quarkus.http.auth.form.priority+++[]<br><br>Form-based authentication mechanism priority.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_FORM_PRIORITY+++[] | int | `1000` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-inclusive"></a> [`quarkus.http.auth.inclusive`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-inclusive) config_property_copy_button:+++quarkus.http.auth.inclusive+++[]<br><br>Require that all registered HTTP authentication mechanisms must attempt to verify the request credentials.<br><br>By default, when the `inclusive-mode` is strict, every registered authentication mechanism must produce SecurityIdentity, otherwise, a number of mechanisms which produce the identity may be less than a total number of registered mechanisms.<br><br>All produced security identities can be retrieved using the following utility method:<br><br>```<br>`io.quarkus.vertx.http.runtime.security.HttpSecurityUtils++#++getSecurityIdentities(io.quarkus.security.identity.SecurityIdentity)`<br>```<br><br>An injected `SecurityIdentity` represents an identity produced by the first inclusive authentication mechanism. When the `mTLS` authentication is required, the `mTLS` mechanism is always the first mechanism, because its priority is elevated when inclusive authentication<br><br>This property is false by default which means that the authentication process is complete as soon as the first `SecurityIdentity` is created.<br><br>This property will be ignored if the path specific authentication is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_INCLUSIVE+++[] | boolean | `false` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-inclusive-mode"></a> [`quarkus.http.auth.inclusive-mode`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-inclusive-mode) config_property_copy_button:+++quarkus.http.auth.inclusive-mode+++[]<br><br>Inclusive authentication mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_INCLUSIVE_MODE+++[] | tooltip:lax[Authentication succeeds if at least one of the registered HTTP authentication mechanisms creates the identity.], tooltip:strict[Authentication succeeds if all the registered HTTP authentication mechanisms create the identity. Typically,opts=inclusive authentication should be in the strict mode when the credentials are carried over mTLS,when both mTLS and another authentication,for example,OIDC bearer token authentication,must succeed. In such cases,`SecurityIdentity` created by the first mechanism,mTLS,can be injected,identities created by other mechanisms will be available on `SecurityIdentity`.] | tooltip:strict[Authentication succeeds if all the registered HTTP authentication mechanisms create the identity. Typically,opts=inclusive authentication should be in the strict mode when the credentials are carried over mTLS,when both mTLS and another authentication,for example,OIDC bearer token authentication,must succeed. In such cases,`SecurityIdentity` created by the first mechanism,mTLS,can be injected,identities created by other mechanisms will be available on `SecurityIdentity`.] |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-mtls-priority"></a> [`quarkus.http.auth.mtls.priority`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-mtls-priority) config_property_copy_button:+++quarkus.http.auth.mtls.priority+++[]<br><br>Mutual TLS authentication mechanism priority.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_MTLS_PRIORITY+++[] | int | `1000 by default, 3000 when an inclusive authentication is enabled` |
| <a id="quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-basic-priority"></a> [`quarkus.http.auth.basic.priority`](#quarkus-vertx-http_quarkus-http-auth_quarkus-http-auth-basic-priority) config_property_copy_button:+++quarkus.http.auth.basic.priority+++[]<br><br>Basic authentication mechanism priority.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_HTTP_AUTH_BASIC_PRIORITY+++[] | int | `2000` |

<a id="duration-note-anchor-quarkus-vertx-http_quarkus-http-auth"></a>

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

<a id="form-based-auth-programmatic-set-up"></a>
#### Set up Form-based authentication programmatically

In addition to the configuration properties listed in the [Form-based authentication configuration reference](#form-configuration-properties) section, Quarkus supports a programmatic set up during the runtime as in the example below:

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.Form;
import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class FormConfiguration {

    void configure(@Observes HttpSecurity httpSecurity) { <1>
        httpSecurity.mechanism(Form.builder()
                .httpOnlyCookie()
                .loginPage("/my-login.html")
                .errorPage("/my-error.html")
                .build());
    }

}
```

1. Observe the `io.quarkus.vertx.http.security.HttpSecurity` CDI event and configure Form authentication mechanism programmatically.

<a id="mutual-tls"></a>
### Mutual TLS authentication

Quarkus provides mutual TLS (mTLS) authentication so that you can authenticate users based on their X.509 certificates.

To use this authentication method, you must first enable SSL/TLS for your application. For more information, see the [Supporting secure connections with SSL/TLS](/guides/http-reference/index.md#ssl) section of the Quarkus "HTTP reference" guide.

After your application accepts secure connections, the next step is to configure the `quarkus.http.ssl.certificate.trust-store-file` property with the name of the file that holds all the certificates your application trusts. This file also includes information about how your application requests certificates when a client, such as a browser or another service, tries to access one of its protected resources.

Because JKS is no longer the default keystore and truststore format in Quarkus, the framework makes an educated guess based on the file extension:

- `.pem`, `.crt`, and `.key` are read as PEM certificates and keys.
- `.jks`, `.keystore`, and `.truststore` are read as JKS keystores and truststores.
- `.p12`, `.pkcs12`, and `.pfx` are read as PKCS12 keystores and truststores.

If your file does not use one of these extensions, you must set the format by using the following properties:

```properties
quarkus.http.ssl.certificate.key-store-file-type=JKS  # or P12 or PEM
quarkus.http.ssl.certificate.trust-store-file-type=JKS  # or P12 or PEM
```

JKS is becoming less commonly used. Since Java 9, the default keystore format in Java is PKCS12. The most significant difference between JKS and PKCS12 is that JKS is a format specific to Java. In contrast, PKCS12 is a standardized, language-neutral way of storing encrypted private keys and certificates.

Here is an example configuration for enabling mTLS:

```properties
quarkus.http.ssl.certificate.key-store-file=server-keystore.jks <1>
quarkus.http.ssl.certificate.key-store-password=the_key_store_secret
quarkus.http.ssl.certificate.trust-store-file=server-truststore.jks <2>
quarkus.http.ssl.certificate.trust-store-password=the_trust_store_secret
quarkus.http.ssl.client-auth=required <3>
quarkus.http.auth.permission.default.paths=/* <4>
quarkus.http.auth.permission.default.policy=authenticated
quarkus.http.insecure-requests=disabled <5>
```

1. The keystore where the server's private key is located.
2. The truststore from which the trusted certificates are loaded.
3. Setting `quarkus.http.ssl.client-auth` to `required` makes the server demand client certificates. You can set it to `REQUEST` if the server should accept requests without a certificate. This setting is useful when supporting multiple authentication methods besides mTLS.
4. Defines a policy where only authenticated users can access resources from your application.
5. Disables the plain HTTP protocol, requiring all requests to use HTTPS. When you set `quarkus.http.ssl.client-auth` to `required`, `quarkus.http.insecure-requests` is automatically disabled.

When an incoming request matches a valid certificate in the truststore, your application can obtain the subject by injecting a `SecurityIdentity` as follows:

<a id="x509-subject-example"></a>

**Obtaining the subject**

```java
@Inject
SecurityIdentity identity;

@GET
@Produces(MediaType.TEXT_PLAIN)
public String hello() {
    return String.format("Hello, %s", identity.getPrincipal().getName());
}
```

You can also get the certificate by using the code outlined in the following example:

<a id="x509-credential-example"></a>

**Obtaining the certificate**

```java
import java.security.cert.X509Certificate;
import io.quarkus.security.credential.CertificateCredential;

CertificateCredential credential = identity.getCredential(CertificateCredential.class);
X509Certificate certificate = credential.getCertificate();
```

<a id="map-certificate-attributes-to-roles"></a>
#### Mapping certificate attributes to roles

The information from the client certificate can be used to add roles to Quarkus `SecurityIdentity`.

You can add new roles to `SecurityIdentity` after checking a client certificate's common name (CN) attribute. The easiest way to add new roles is to use a certificate attribute to role mapping feature.

For example, you can update the properties shown in the section which introduces [Mutual TLS authentication](#mutual-tls) as follows:

```properties
quarkus.http.ssl.certificate.key-store-file=server-keystore.jks
quarkus.http.ssl.certificate.key-store-password=the_key_store_secret
quarkus.http.ssl.certificate.trust-store-file=server-truststore.jks
quarkus.http.ssl.certificate.trust-store-password=the_trust_store_secret
quarkus.http.ssl.client-auth=required
quarkus.http.insecure-requests=disabled

quarkus.http.auth.certificate-role-properties=cert-role-mappings.properties <1>

quarkus.http.auth.permission.certauthenticated.paths=/* <2>
quarkus.http.auth.permission.certauthenticated.policy=role-policy-cert <2>
quarkus.http.auth.policy.role-policy-cert.roles-allowed=user,admin <2>
```

1. The `cert-role-mappings.properties` classpath resource contains a map of certificate's CN values to roles in the form `CN=role` or `CN=role1,role2`, etc. Let us assume it contains three entries: `alice=user,admin`, `bob=user` and `jdoe=tester`.
2. Use HTTP security policy to require that `SecurityIdentity` must have either `user` or `admin` roles for the requests to be authorized.

Given the preceding configuration, the request is authorized if the client certificate's CN attribute is equal to `alice` or `bob` and forbidden if it is equal to `jdoe`.

<a id="use-cert-attrs-to-augment-identity"></a>
#### Using certificate attributes to augment SecurityIdentity

You can always register `SecurityIdentityAugmentor` if the automatic [Mapping certificate attributes to roles](#map-certificate-attributes-to-roles) option does not suit. Custom `SecurityIdentityAugmentor` can check the values of different client certificate attributes and augment the `SecurityIdentity` accordingly.

For more information about customizing `SecurityIdentity`, see the [Security identity customization](/guides/security-customization/index.md#security-identity-customization) section in the Quarkus "Security tips and tricks" guide.

<a id="mtls-programmatic-set-up"></a>
#### Set up the mutual TLS client authentication programmatically

In the [Mutual TLS authentication](#mutual-tls) section we configured the mutual TLS client authentication in the `application.properties` file like this:

```properties
quarkus.tls.tls-config-1.key-store.p12.path=server-keystore.p12
quarkus.tls.tls-config-1.key-store.p12.password=the_key_store_secret
quarkus.tls.tls-config-1.trust-store.p12.path=server-truststore.p12
quarkus.tls.tls-config-1.trust-store.p12.password=the_trust_store_secret
quarkus.http.ssl.client-auth=required <1>
quarkus.http.tls-configuration-name=tls-config-1 <2>
```

1. Enable and require the mutual TLS client authentication.
2. Use the `tls-config-1` TLS configuration for the HTTP server TLS communication.

The `io.quarkus.vertx.http.security.HttpSecurity` CDI event enable you to configuration the mutual TLS authentication programmatically like in the example below:

**Example TLS configuration**

```java
package org.acme.http.security;

import io.quarkus.tls.BaseTlsConfiguration;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MyTlsConfiguration extends BaseTlsConfiguration {

    @Override
    public KeyCertOptions getKeyStoreOptions() {
        return new KeyStoreOptions()
                .setPath("/tmp/certs/server-keystore.p12")
                .setPassword("the_key_store_secret")
                .setType("PKCS12");
    }

    @Override
    public TrustOptions getTrustStoreOptions() {
        return new KeyStoreOptions()
                .setPath("/tmp/certs/server-truststore.jks")
                .setPassword("the_trust_store_secret")
                .setType("PKCS12");
    }

    @Override
    public SSLOptions getSSLOptions() {
        SSLOptions options = new SSLOptions();
        options.setKeyCertOptions(getKeyStoreOptions());
        options.setTrustOptions(getTrustStoreOptions());
        options.setSslHandshakeTimeoutUnit(TimeUnit.SECONDS);
        options.setSslHandshakeTimeout(10);
        options.setEnabledSecureTransportProtocols(Set.of("TLSv1.3", "TLSv1.2"));
        return options;
    }

}
```

**Example mutual TLS client authentication configuration**

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class MutualTlsClientAuthConfig {

    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.mTLS("tls-config-1", new MyTlsConfiguration()); <1>
    }

}
```

1. Enable and require the mutual TLS client authentication and use the `tls-config-1` TLS configuration for the HTTP server TLS communication. The `tls-config-1` TLS configuration is registered in the TLS registry.

It is also possible to map certificate attributes to roles. Let's consider the example explained in the [Mapping certificate attributes to roles](#map-certificate-attributes-to-roles) section:

```properties
quarkus.tls.tls-config-1.key-store.p12.path=server-keystore.p12
quarkus.tls.tls-config-1.key-store.p12.password=the_key_store_secret
quarkus.tls.tls-config-1.trust-store.p12.path=server-truststore.p12
quarkus.tls.tls-config-1.trust-store.p12.password=the_trust_store_secret
quarkus.http.ssl.client-auth=required
quarkus.http.tls-configuration-name=tls-config-1
quarkus.http.auth.certificate-role-properties=cert-role-mappings.properties
```

The configuration with the programmatic set up would look like this:

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.MTLS;
import io.quarkus.vertx.http.security.HttpSecurity;
import jakarta.enterprise.event.Observes;

public class MutualTlsClientAuthConfig {

    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.mTLS(MTLS.builder()
                .tls("tls-config-1", new MyTlsConfiguration())
                .rolesMapping("CN-value", "user", "admin")
                .build());
    }

}
```

This API also enables you to use certificate to augment `SecurityIdentity`:

```properties
quarkus.http.tls-configuration-name=tls-config-1
quarkus.tls.tls-config-1.key-store.p12.path=server-keystore.p12
quarkus.tls.tls-config-1.key-store.p12.password=the_key_store_secret
quarkus.tls.tls-config-1.trust-store.p12.path=server-truststore.jks
quarkus.tls.tls-config-1.trust-store.p12.password=the_trust_store_secret
```

```java
package org.acme.http.security;

import io.quarkus.vertx.http.security.HttpSecurity;
import io.quarkus.vertx.http.security.MTLS;
import jakarta.enterprise.event.Observes;

import java.util.Set;

public class MutualTlsClientAuthConfig {

    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity.mTLS(MTLS.builder()
                .certificateToRolesMapper(x509Certificate -> { <1>
                    final Set<String> securityIdentityRoles;

                    // replace this logic with your own certificate to roles mapping
                    if (x509Certificate.getIssuerX500Principal() != null
                            && "CN=quarkus.io".equals(x509Certificate.getIssuerX500Principal().getName())) {
                        securityIdentityRoles = Set.of("admin");
                    } else {
                        securityIdentityRoles = Set.of();
                    }
                    return securityIdentityRoles;
                })
                .build());
    }

}
```

1. In the [Using certificate attributes to augment SecurityIdentity](#use-cert-attrs-to-augment-identity) section we have mentioned that the `SecurityIdentityAugmentor` can be used to map the client certificate attribute values to the `SecurityIdentity` roles. The `MTLS` API allows you to define such roles mapping as well.

> [!IMPORTANT]
> If Quarkus starts the HTTP server in DEV mode after a failed start, Quarkus may need to fallback to the configuration provided in the `application.properties` file. This is a known limitation.

<a id="other-supported-authentication-mechanisms"></a>
## Other supported authentication mechanisms

Quarkus Security also supports the following authentication mechanisms through extensions:

- [WebAuthn authentication](#webauthn-authentication)
- [OpenID Connect authentication](#openid-connect-authentication)
- [SmallRye JWT authentication](#smallrye-jwt-authentication)
- [OAuth2 authentication](#oauth2-authentication)

<a id="webauthn-authentication"></a>
### WebAuthn authentication

[WebAuthn](https://webauthn.guide/) is an authentication mechanism that replaces passwords. When you write a service for registering new users, or logging them in, instead of asking for a password, you can use WebAuthn, which replaces the password. For more information, see the [Secure a Quarkus application by using the WebAuthn authentication mechanism](/guides/security-webauthn/index.md) guide.

<a id="openid-connect-authentication"></a>
### OpenID Connect authentication

OpenID Connect (OIDC) is an identity layer that works on top of the OAuth 2.0 protocol. OIDC enables client applications to verify the identity of a user based on the authentication performed by the OIDC provider and retrieve basic information about that user.

The Quarkus `quarkus-oidc` extension provides a reactive, interoperable, multitenant-enabled OIDC adapter that supports Bearer token and Authorization Code Flow authentication mechanisms. The Bearer token authentication mechanism extracts the token from the HTTP Authorization header.

The Authorization Code Flow mechanism redirects the user to an OIDC provider to authenticate the user's identity. After the user is redirected back to Quarkus, the mechanism completes the authentication process by exchanging the provided code that was granted for the ID, access, and refresh tokens.

You can verify ID and access JSON Web Token (JWT) tokens by using the refreshable JSON Web Key (JWK) set or introspect them remotely. However, opaque, also known as binary tokens, can only be introspected remotely.

> [!NOTE]
> Using the Quarkus OIDC extension, both the Bearer token and Authorization Code Flow authentication mechanisms use [SmallRye JWT authentication](#smallrye-jwt-authentication) to represent JWT tokens as MicroProfile JWT `org.eclipse.microprofile.jwt.JsonWebToken`.

#### Additional Quarkus resources for OIDC authentication

For more information about OIDC authentication and authorization methods that you can use to secure your Quarkus applications, see the following resources:

| OIDC topic | Quarkus information resource |
| --- | --- |
| Bearer token authentication mechanism | [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md) |
| Authorization Code Flow authentication mechanism | [OpenID Connect (OIDC) Authorization Code Flow mechanism](/guides/security-oidc-code-flow-authentication/index.md) |
| OIDC and SAML Identity broker | [OpenID Connect (OIDC) Authorization Code Flow and SAML Identity broker](/guides/security-oidc-code-flow-authentication/index.md#oidc-saml-broker) |
| Multiple tenants that can support the Bearer token authentication or Authorization Code Flow mechanisms | [Using OpenID Connect (OIDC) multi-tenancy](/guides/security-openid-connect-multitenancy/index.md) |
| Securing Quarkus with commonly used OpenID Connect providers | [Configuring well-known OpenID Connect providers](/guides/security-openid-connect-providers/index.md) |
| Using Keycloak to centralize authorization | [Using OpenID Connect (OIDC) and Keycloak to centralize authorization](/guides/security-keycloak-authorization/index.md) |
| Configuring Keycloak programmatically | [Using the Keycloak admin client](/guides/security-keycloak-admin-client/index.md) |

> [!NOTE]
> To enable the Quarkus OIDC extension at runtime, set `quarkus.oidc.tenant-enabled=false` at build time. Then, re-enable it at runtime by using a system property.
>
> For more information about managing the individual tenant configurations in multitenant OIDC deployments, see the [Disabling tenant configurations](/guides/security-openid-connect-multitenancy/index.md#disable-tenant) section in the "Using OpenID Connect (OIDC) multi-tenancy" guide.

#### OpenID Connect client and filters

The `quarkus-oidc-client` extension provides `OidcClient` for acquiring and refreshing access tokens from OpenID Connect and OAuth2 providers that support the following token grants:

- `client-credentials`
- `password`
- `refresh_token`

The `quarkus-resteasy-client-oidc-filter` extension requires the `quarkus-oidc-client` extension. It provides JAX-RS RESTful Web Services `OidcClientRequestFilter`, which sets the access token acquired by `OidcClient` as the `Bearer` scheme value of the HTTP `Authorization` header. This filter can be registered with MicroProfile REST client implementations injected into the current Quarkus endpoint, but it is not related to the authentication requirements of this service endpoint. For example, it can be a public endpoint or be protected with mTLS.

> [!IMPORTANT]
> In this scenario, you do not need to protect your Quarkus endpoint by using the Quarkus OpenID Connect adapter.

The `quarkus-resteasy-client-oidc-token-propagation` extension requires the `quarkus-oidc` extension. It provides Jakarta REST `TokenCredentialRequestFilter`, which sets the OpenID Connect Bearer token or Authorization Code Flow access token as the `Bearer` scheme value of the HTTP `Authorization` header. This filter can be registered with MicroProfile REST client implementations injected into the current Quarkus endpoint, which must be protected by using the Quarkus OIDC adapter. This filter can propagate the access token to the downstream services.

For more information, see the [OpenID Connect client and token propagation quickstart](/guides/security-openid-connect-client/index.md) and [OpenID Connect (OIDC) and OAuth2 client and filters reference](/guides/security-openid-connect-client-reference/index.md) guides.

<a id="smallrye-jwt-authentication"></a>
### SmallRye JWT authentication

The `quarkus-smallrye-jwt` extension provides a MicroProfile JSON Web Token (JWT) 2.1 implementation and multiple options to verify signed and encrypted `JWT` tokens. It represents them as `org.eclipse.microprofile.jwt.JsonWebToken`.

`quarkus-smallrye-jwt` is an alternative to the `quarkus-oidc` Bearer token authentication mechanism and verifies only `JWT` tokens by using either Privacy Enhanced Mail (PEM) keys or the refreshable `JWK` key set. `quarkus-smallrye-jwt` also provides the JWT generation API, which you can use to easily create `signed`, `inner-signed`, and `encrypted` `JWT` tokens.

For more information, see the [Using JWT RBAC](/guides/security-jwt/index.md) guide.

<a id="oauth2-authentication"></a>
### OAuth2 authentication

`quarkus-elytron-security-oauth2` provides an alternative to the Quarkus `quarkus-oidc` Bearer token authentication mechanism extension. `quarkus-elytron-security-oauth2` is based on `Elytron` and is primarily intended for introspecting opaque tokens remotely.

For more information, see the Quarkus [Using OAuth2](/guides/security-oauth2/index.md) guide.

<a id="oidc-jwt-oauth2-comparison"></a>
## Choosing between OpenID Connect, SmallRye JWT, and OAuth2 authentication mechanisms

Use the following information to select the appropriate token authentication mechanism to secure your Quarkus applications.

**List of authentication mechanism use cases**

- `quarkus-oidc` requires an OpenID Connect provider such as Keycloak, which can verify the bearer tokens or authenticate the end users with the Authorization Code flow. In both cases, `quarkus-oidc` requires a connection to the specified OpenID Connect provider.

- If the user authentication requires Authorization Code flow, or you need to support multiple tenants, use `quarkus-oidc`. `quarkus-oidc` can also request user information by using both Authorization Code Flow and Bearer access tokens.

- If your bearer tokens must be verified, use `quarkus-oidc`, `quarkus-elytron-security-oauth2`, or `quarkus-smallrye-jwt`.

- If your bearer tokens are in a JSON web token (JWT) format, you can use any extensions in the preceding list. Both `quarkus-oidc` and `quarkus-smallrye-jwt` support refreshing the `JsonWebKey` (JWK) set when the OpenID Connect provider rotates the keys. Therefore, if remote token introspection must be avoided or is unsupported by the providers, use `quarkus-oidc` or `quarkus-smallrye-jwt` to verify JWT tokens.

- To introspect the JWT tokens remotely, you can use `quarkus-oidc` or `quarkus-elytron-security-oauth2` for verifying the opaque or binary tokens by using remote introspection. `quarkus-smallrye-jwt` does not support the remote introspection of both opaque or JWT tokens but instead relies on the locally available keys that are usually retrieved from the OpenID Connect provider.

- `quarkus-oidc` and `quarkus-smallrye-jwt` support the JWT and opaque token injection into the endpoint code. Injected JWT tokens provide more information about the user. All extensions can have the tokens injected as `Principal`.

- `quarkus-smallrye-jwt` supports more key formats than `quarkus-oidc`. `quarkus-oidc` uses only the JWK-formatted keys that are part of a JWK set, whereas `quarkus-smallrye-jwt` supports PEM keys.

- `quarkus-smallrye-jwt` handles locally signed, inner-signed-and-encrypted, and encrypted tokens. In contrast, although `quarkus-oidc` and `quarkus-elytron-security-oauth2` can also verify such tokens, they treat them as opaque tokens and verify them through remote introspection.

- If you need a lightweight library for the remote introspection of opaque or JWT tokens, use `quarkus-elytron-security-oauth2`.

> [!NOTE]
> Architectural considerations drive your decision to use opaque or JSON web token (JWT) token format. Opaque tokens tend to be much shorter than JWT tokens but need most of the token-associated state to be maintained in the provider database. Opaque tokens are effectively database pointers.
>
> JWT tokens are significantly longer than opaque tokens. Nonetheless, the providers effectively delegate most of the token-associated state to the client by storing it as the token claims and either signing or encrypting them.

<a id="table"></a>

**Token authentication mechanism comparison**

| Feature required | Authentication mechanism |  |  |
| --- | --- | --- | --- |
|  | `quarkus-oidc` | `quarkus-smallrye-jwt` | `quarkus-elytron-security-oauth2` |
| **Bearer JWT verification** | Local verification or introspection | Local verification | Introspection |
| **Bearer opaque token verification** | Introspection | No | Introspection |
| **Refreshing** `JsonWebKey` **set to verify JWT tokens** | Yes | Yes | No |
| **Represent token as** `Principal` | Yes | Yes | Yes |
| **Inject JWT as MP JWT** | Yes | Yes | No |
| **Authorization code flow** | Yes | No | No |
| **Multi-tenancy** | Yes | No | No |
| **User information support** | Yes | No | No |
| **PEM key format support** | No | Yes | No |
| **SecretKey support** | No | In JSON Web Key (JWK) format | No |
| **Inner-signed and encrypted or encrypted tokens** | Introspection | Local verification | Introspection |
| **Custom token verification** | No | With injected JWT parser | No |
| **JWT as a cookie support** | No | Yes | Yes |

<a id="combining-authentication-mechanisms"></a>
## Combining authentication mechanisms

If different sources provide the user credentials, you can combine authentication mechanisms. For example, you can combine the built-in Basic and the Quarkus `quarkus-oidc` Bearer token authentication mechanisms.

The authentication process completes as soon as the first `SecurityIdentity` is produced by one of the authentication mechanisms.

<a id="authentication-mechanisms-priority"></a>
### Priority of authentication mechanisms

Quarkus Security uses a priority system to determine which mechanism gets the first chance to produce `SecurityIdentity`. The authentication mechanism priority affects the order of:

- **Credential Verification:** Mechanisms with a higher priority are asked to verify credentials first. The first one to validate credentials wins.
- **Authentication Challenge:** If the request is unauthenticated, mechanisms with a higher priority are asked to issue a challenge first.

The default priorities can be overridden using the configuration properties listed below.

**Priorities of the supported authentication mechanisms**

| Authentication mechanism^ | Configuration property^ | Default priority |
| --- | --- | --- |
| **Basic authentication mechanism** | `quarkus.http.auth.basic.priority` | 2000 |
| **Form-based authentication mechanism** | `quarkus.http.auth.form.priority` | 1000 |
| **Mutual TLS authentication mechanism** | `quarkus.http.auth.mtls.priority` | 1000 by default, 3000 when an inclusive authentication is enabled |
| **WebAuthn authentication mechanism** | `quarkus.webauthn.priority` | 1000 |
| **OIDC authentication mechanism** | `quarkus.oidc.priority` | 1001 |
| **SmallRye JWT authentication mechanism** | `quarkus.smallrye-jwt.priority` | 1000 |

<a id="inclusive-authentication"></a>
### Inclusive Authentication

In some cases it can be required that all registered authentication mechanisms create their `SecurityIdentity`. It can be required when the credentials such as tokens have to be passed over [Mutual TLS authentication](#mutual-tls), for example, when users are authenticating via `Virtual Private Network`, or when the current token has to be bound to the client certificate for the token verification to succeed, guaranteeing that the token was issued exactly to the same client which is currently passing this token to Quarkus alongside its client certificate.

In Quarkus such authentication is called `inclusive` and you can enable it like this:

```properties
quarkus.http.auth.inclusive=true
```

If the authentication is inclusive then `SecurityIdentity` created by the first authentication mechanism can be injected into the application code. For example, if both [Mutual TLS authentication](#mutual-tls) and basic authentication mechanism authentications are required, the [Mutual TLS authentication](#mutual-tls) mechanism will create `SecurityIdentity` first.

> [!NOTE]
> When an inclusive authentication is enabled, the [Mutual TLS authentication](#mutual-tls) mechanism has the highest priority by default, to reflect on the fact that when it is used, it is enforced early at the transport level, and to ensure that an injected `SecurityIdentity` represents a [Mutual TLS authentication](#mutual-tls) authentication. If you need an injected `SecurityIdentity` to represent another authentication mechanism, [configure the mechanism priority](#authentication-mechanisms-priority).

Additional `SecurityIdentity` instances can be accessed as a `quarkus.security.identities` attribute on the first `SecurityIdentity`, however, accessing these extra identities directly may not be necessary, for example, when both [Mutual TLS authentication](#mutual-tls) and [OIDC Bearer authentication](/guides/security-oidc-bearer-token-authentication/index.md) mechanisms have been combined and done their authentications, the authenticated bearer token can be injected as a token credential alongside `SecurityIdentity` created by [Mutual TLS authentication](#mutual-tls). This is exemplified below:

```java
package org.acme.security;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InclusiveAuthExampleBean {

    @Inject
    SecurityIdentity mtlsIdentity; <1>

    @Inject
    AccessTokenCredential accessTokenCredential;

    private AccessTokenCredential getAccessTokenCredential() {
        if (doItHardWay()) {
            var securityIdentities = HttpSecurityUtils.getSecurityIdentities(mtlsIdentity); <2>
            if (securityIdentities != null) {
                SecurityIdentity bearerIdentity = securityIdentities.get(OidcConstants.BEARER_SCHEME);
                if (bearerIdentity != null) {
                    return bearerIdentity.getCredential(AccessTokenCredential.class);
                }
            }
        }
        return accessTokenCredential;
    }

}
```

1. This is the `SecurityIdentity` created by applicable authentication mechanism with the highest priority.
2. Other applicable authentication mechanisms performed authentication and are available on the `SecurityIdentity`.

> [!IMPORTANT]
> You cannot combine the Quarkus `quarkus-oidc` Bearer token and `smallrye-jwt` authentication mechanisms because both mechanisms attempt to verify the token extracted from the HTTP Bearer token authentication scheme.

#### Authorization with multiple identities

Authorization is always performed on the first produced `SecurityIdentity`. For example:

```java
package org.acme.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class DataService {

    @RolesAllowed("admin") <1>
    void deleteEntity(Long entityId) {
        // business logic
    }

}
```

1. The `deleteEntity` method can only be accessed if the first `SecurityIdentity` has the `admin` role. [Configure the mechanism priority](#authentication-mechanisms-priority) to impact which authentication mechanism is used to produce the first `SecurityIdentity`.

If you want to perform authorization on all `SecurityIdentity` instances, you must do it manually. For example, to check if a permission is allowed at least by one of the identities, use a `@PermissionChecker` annotation:

```java
package org.acme.security;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class DataService {

    @PermissionsAllowed("canDeleteEntity")
    void deleteEntity(Long entityId) {
        // business logic
    }

    @PermissionChecker("canDeleteEntity")
    boolean canDeleteEntity(SecurityIdentity firstIdentity) {
        var allSecurityIdentities = HttpSecurityUtils.getSecurityIdentities(firstIdentity);
        if (allSecurityIdentities != null) {
            for (SecurityIdentity identity : allSecurityIdentities.values()) {
                if (identity.hasRole("admin")) {
                    return true;
                }
            }
        }

        return false;
    }

}
```

<a id="path-based-authentication"></a>
### Path-based authentication

#### Use HTTP Security Policy to enable path-based authentication

The following configuration example demonstrates how you can enforce a single selectable authentication mechanism for a given request path:

```properties
quarkus.http.auth.permission.basic.paths=/hello/basic <1>
quarkus.http.auth.permission.basic.policy=authenticated
quarkus.http.auth.permission.basic.auth-mechanism=basic

quarkus.http.auth.permission.bearer.paths=/hello/bearer <2>
quarkus.http.auth.permission.bearer.policy=authenticated
quarkus.http.auth.permission.bearer.auth-mechanism=bearer

quarkus.http.auth.permission.custom.paths=/hello/custom <3>
quarkus.http.auth.permission.custom.policy=authenticated
quarkus.http.auth.permission.custom.auth-mechanism=custom

quarkus.http.auth.permission.basic-bearer.paths=/hello/basic-bearer <4>
quarkus.http.auth.permission.basic-bearer.policy=authenticated
quarkus.http.auth.permission.basic-bearer.auth-mechanism=basic,bearer
```

1. The REST endpoint `/hello/basic` can only ever be accessed by using the [Basic authentication](#basic-auth).
2. The REST endpoint `/hello/bearer` can only ever be accessed by using the [OIDC bearer token authentication mechanism](/guides/security-oidc-bearer-token-authentication/index.md).
3. The REST endpoint `/hello/custom` can only ever be accessed by using a custom `HttpAuthenticationMechanism` with a `custom` authentication scheme.
4. The REST endpoint `/hello/basic-bearer` can be accessed either by using the [OIDC bearer token authentication mechanism](/guides/security-oidc-bearer-token-authentication/index.md), or by using the [Basic authentication](#basic-auth).

> [!IMPORTANT]
> Ensure that the value of the `auth-mechanism` property matches the authentication scheme supported by `HttpAuthenticationMechanism`, for example, `basic`, `bearer` or `custom`. For example, for the `quarkus.http.auth.permission.custom.auth-mechanism=custom` expression to be effective, a custom `HttpAuthenticationMechanism` implementation must set its authentication scheme to `custom` in its `HttpAuthenticationMechanism#getCredentialTransport` method implementation.

#### Use HTTP Security Policy inclusive path-based authentication

When an [inclusive authentication](#inclusive-authentication) is enabled in a `strict` mode, every selected authentication mechanism must provide the `SecurityIdentity`. For example:

```properties
quarkus.http.auth.inclusive=true

quarkus.http.auth.permission.basic-bearer.paths=/hello/basic-bearer
quarkus.http.auth.permission.basic-bearer.policy=authenticated
quarkus.http.auth.permission.basic-bearer.auth-mechanism=basic,bearer <1>
```

1. The REST endpoint `/hello/basic-bearer` can be accessed only if both the [OIDC bearer token authentication mechanism](/guides/security-oidc-bearer-token-authentication/index.md) and the [Basic authentication](#basic-auth) have successfully provided `SecurityIdentity`.

When an [inclusive authentication](#inclusive-authentication) is enabled in a `lax` mode, every selected authentication mechanism will have a chance to provide its own `SecurityIdentity`, with at least one of the mechanisms expected to produce `SecurityIdentity`. Consider the following example:

```properties
quarkus.http.auth.inclusive=true
quarkus.http.auth.inclusive-mode=lax

quarkus.http.auth.permission.basic-bearer.paths=/hello/basic-bearer
quarkus.http.auth.permission.basic-bearer.policy=authenticated
quarkus.http.auth.permission.basic-bearer.auth-mechanism=basic,bearer <1>
```

1. The REST endpoint `/hello/basic-bearer` uses both the [OIDC bearer token authentication mechanism](/guides/security-oidc-bearer-token-authentication/index.md) and the [Basic authentication](#basic-auth). Access is granted if at least one of them successfully provides `SecurityIdentity`.

<a id="use-annotations-for-path-based-auth"></a>
#### Use annotations to enable path-based authentication for Jakarta REST endpoints

It is possible to use annotations to select an authentication mechanism specific to each Jakarta REST endpoint. This feature is only enabled when [Proactive authentication](#proactive-auth) is disabled due to the fact that the annotations can only be used to select authentication mechanisms after a REST endpoint has been matched. Here is how you can select an authentication mechanism per a REST endpoint basis:

```properties
quarkus.http.auth.proactive=false
```

```java
import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("hello")
public class HelloResource {

    @GET
    @BasicAuthentication <1><2>
    @Path("basic")
    public String basicAuthMechanism() {
        return "basic";
    }

    @GET
    @RolesAllowed("admin") <3>
    @AuthorizationCodeFlow <4>
    @Path("code-flow")
    public String codeFlowAuthMechanism() {
        return "code-flow";
    }

    @GET
    @HttpAuthenticationMechanism("custom") <5><6>
    @Path("custom")
    public String customAuthMechanism() {
        return "custom";
    }

    @GET
    @BasicAuthentication
    @AuthorizationCodeFlow
    @Path("basic-or-code-flow")
    public String basicOrCodeFlowAuthMechanism() { <7>
        return "basic-or-code-flow";
    }
}
```

1. The REST endpoint `/hello/basic` can only ever be accessed by using the [Basic authentication](#basic-auth).
2. The `/hello/basic` endpoint requires authentication, because when no standard security annotation accompanies the `@BasicAuthentication` annotation, the `@Authenticated` annotation is added by default.
3. The `@AuthorizationCodeFlow` annotation can be combined with any other standard security annotation like `@RolesAllowed`, `@PermissionsAllowed` and others.
4. The REST endpoint `/hello/code-flow` can only ever be accessed by using the [OIDC authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md).
5. The REST endpoint `/hello/custom` can only ever be accessed by using a custom `HttpAuthenticationMechanism` with a `custom` authentication scheme.
6. The `/hello/custom` endpoint requires authentication, because when no standard security annotation accompanies the `@HttpAuthenticationMechanism` annotation, the `@Authenticated` annotation is added by default.
7. The REST endpoint `/hello/basic-or-code-flow` can be accessed either by using the [OIDC authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md), or by using the [Basic authentication](#basic-auth).

> [!IMPORTANT]
> Ensure that the value of the `@HttpAuthenticationMechanism` annotation matches the authentication scheme supported by `HttpAuthenticationMechanism`. For example, for `@HttpAuthenticationMechanism("custom")` to work, a custom `io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism` implementation must set its authentication scheme to `custom` in its `HttpAuthenticationMechanism#getCredentialTransport` method implementation.

**Supported authentication mechanism annotations**

| Authentication mechanism^ | Annotation |
| --- | --- |
| **Basic authentication mechanism** | `io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication` |
| **Form-based authentication mechanism** | `io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication` |
| **Mutual TLS authentication mechanism** | `io.quarkus.vertx.http.runtime.security.annotation.MTLSAuthentication` |
| **WebAuthn authentication mechanism** | `io.quarkus.security.webauthn.WebAuthn` |
| **Bearer token authentication mechanism** | `io.quarkus.oidc.BearerTokenAuthentication` |
| **OIDC authorization code flow mechanism** | `io.quarkus.oidc.AuthorizationCodeFlow` |
| **SmallRye JWT authentication mechanism** | `io.quarkus.smallrye.jwt.runtime.auth.BearerTokenAuthentication` |

> [!TIP]
> Quarkus automatically secures endpoints annotated with the authentication mechanism annotation. When no standard security annotation is present on the REST endpoint and resource, the `io.quarkus.security.Authenticated` annotation is added for you.

It is also possible to use the `io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism` annotation to select any authentication mechanism based on its scheme. Annotation-based analogy to the `quarkus.http.auth.permission.basic.auth-mechanism=custom` configuration property is the `@HttpAuthenticationMechanism("custom")` annotation.

> [!NOTE]
> For consistency with various Jakarta EE specifications, it is recommended to always repeat annotations instead of relying on annotation inheritance.

#### Use annotations to enable inclusive path-based authentication for Jakarta REST endpoints

When an [inclusive authentication](#inclusive-authentication) is enabled in a `strict` mode, every selected authentication mechanism must provide the `SecurityIdentity`. For example:

```properties
quarkus.http.auth.inclusive=true
quarkus.http.auth.proactive=false
```

```java
package org.acme.http.security;

import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@BasicAuthentication
@AuthorizationCodeFlow
@Path("hello")
public class HelloResource {

    @GET
    @Path("basic-and-code-flow")
    public String basicAndCodeFlowAuthMechanism() { <1>
        return "basic-and-code-flow";
    }
}
```

1. The REST endpoint `/hello/basic-and-code-flow` can be accessed only if both the [OIDC authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md) and the [Basic authentication](#basic-auth) have successfully provided `SecurityIdentity`.

When an [inclusive authentication](#inclusive-authentication) is enabled in a `lax` mode, every selected authentication mechanism will have a chance to provide its own `SecurityIdentity`, with at least one of the mechanisms expected to produce `SecurityIdentity`. Consider the following example:

```properties
quarkus.http.auth.inclusive=true
quarkus.http.auth.inclusive-mode=lax
quarkus.http.auth.proactive=false
```

```java
package org.acme.http.security;

import io.quarkus.oidc.AuthorizationCodeFlow;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@BasicAuthentication
@AuthorizationCodeFlow
@Path("hello")
public class HelloResource {

    @GET
    @Path("basic-and-code-flow")
    public String basicAndCodeFlowAuthMechanism() { <1>
        return "basic-and-code-flow";
    }
}
```

1. The REST endpoint `/hello/basic-and-code-flow` uses both the [OIDC authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md) and the [Basic authentication](#basic-auth). Access is granted if at least one of them successfully provides `SecurityIdentity`.

#### Use inclusive authentication to enable path-based authentication

By default, Quarkus only supports [path-based authentication](#use-annotations-for-path-based-auth) for one authentication mechanism selected with annotation per REST endpoint. If more than one authentication mechanism must be used for the [Path-based authentication](#path-based-authentication), you can write a custom `HttpAuthenticationMechanism` as documented in the [Dealing with more than one HttpAuthenticationMechanism](/guides/security-customization/index.md#dealing-with-more-than-one-http-auth-mechanisms) section of the Security Tips and Tricks guide. Another option is to enable [Inclusive Authentication](#inclusive-authentication) in the lax mode and write a custom `HttpSecurityPolicy` or `PermissionChecker` that verifies that all registered HTTP authentication mechanisms created their mechanism-specific `SecurityIdentity`.

**Enable inclusive authentication in the lax mode**

```properties
quarkus.http.auth.inclusive-mode=lax <1>
quarkus.http.auth.inclusive=true
```

1. By default, inclusive authentication requires that all registered HTTP authentication mechanisms must create the `SecurityIdentity`. However, in the lax mode, the authentication succeeds if at least one registered `HttpAuthenticationMechanism` created the `SecurityIdentity`.

Let's assume that we have 3 registered mechanisms, mTLS, Basic and OIDC and you only require Basic and mTLS authentications to succeed to permit access to the `hello` method. In this case, enabling an inclusive authentication in a lax mode allows to check which mechanisms produced the identity as shown in the example below:

**Example of the HTTP Authentication mechanisms check**

```java
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.Map;

@Path("/hello")
public class HelloResource {

    @PermissionsAllowed("mtls-basic")
    @GET
    public String hello() {
        return "Hello world";
    }

    @PermissionChecker("mtls-basic")
    boolean isMtlsAndBasicAuthentication(SecurityIdentity identity) {
        Map<String, SecurityIdentity> identities = HttpSecurityUtils.getSecurityIdentities(identity);
        if (identities != null) {
            return identities.containsKey("basic") && identities.containsKey("x509"); <1>
        }
        return false;
    }
}
```

1. Permit access to the endpoint only if it is confirmed that both `mTLS` and `Basic` authentication mechanisms have authenticated the current request.

#### How to combine it with HTTP Security Policy

The easiest way to define roles that are allowed to access individual resources is the `@RolesAllowed` annotation. Nevertheless, it is also possible to use the HTTP Security Policy like in the example below:

```properties
quarkus.http.auth.policy.roles1.roles-allowed=user
quarkus.http.auth.permission.roles1.paths=/hello/code-flow
quarkus.http.auth.permission.roles1.applies-to=JAXRS <1>
quarkus.http.auth.permission.roles1.policy=roles1
quarkus.http.auth.permission.roles1.methods=GET <2>
```

1. Delay this policy's permission check after the endpoint-specific authentication mechanism has been selected.
2. Make the `roles1` permission match only the endpoint annotated with the `@AuthorizationCodeFlow` annotation. Unannotated endpoints must avoid the delay caused by the `applies-to=JAXRS` option.

<a id="proactive-auth"></a>
## Proactive authentication

Proactive authentication is enabled in Quarkus by default. This means that if an incoming request has a credential, the request will always be authenticated, even if the target page does not require authentication. For more information, see the Quarkus [Proactive authentication](/guides/security-proactive-authentication/index.md) guide.

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
- [Quarkus Security architecture](/guides/security-architecture/index.md)
- [Identity providers](/guides/security-identity-providers/index.md)
- [Authorization of web endpoints](/guides/security-authorize-web-endpoints-reference/index.md)
