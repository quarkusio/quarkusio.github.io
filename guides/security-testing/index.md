# Security testing

This document describes how to test Quarkus Security.

<a id="configuring-user-information"></a>
## Configuring User Information

You can use [quarkus-elytron-security-properties-file](/guides/security-properties/index.md) for testing security. This supports both embedding user info in `application.properties` and standalone properties files.

For example, the following configuration will allow for configuring the users in both the production where OAuth2 is required and development modes using [Configuration Profiles](/guides/config-reference/index.md#profiles).

```properties
# Configure embedded authentication
%dev.quarkus.security.users.embedded.enabled=true
%dev.quarkus.security.users.embedded.plain-text=true
%dev.quarkus.security.users.embedded.users.scott=reader
%dev.quarkus.security.users.embedded.users.stuart=writer
%dev.quarkus.security.users.embedded.roles.scott=READER
%dev.quarkus.security.users.embedded.roles.stuart=READER,WRITER

# Configure OAuth2
quarkus.oauth2.enabled=true
%dev.quarkus.oauth2.enabled=false
quarkus.oauth2.client-id=client-id
quarkus.oauth2.client-secret=client-secret
quarkus.oauth2.introspection-url=http://host:port/introspect
```

<a id="testing-security"></a>
## Test Security Extension

Quarkus provides explicit support for testing with different users, and with the security subsystem disabled. To use this you must include the `quarkus-test-security` dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-security</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-security")
```

This artifact provides the `io.quarkus.test.security.TestSecurity` annotation, that can be applied to test methods and test classes to control the security context that the test is run with. This allows you to do two things, you can disable authorization so tests can access secured endpoints without needing to be authenticated, and you can specify the identity that you want the tests to run under.

A test that runs with authorization disabled can just set the enabled property to false:

```java
@Test
@TestSecurity(authorizationEnabled = false)
void someTestMethod() {
...
}
```

This will disable all access checks, which allows the test to access secured endpoints without needing to authenticate.

You can also use this to configure the current user that the test will run as:

```java
@Test
@TestSecurity(user = "testUser", roles = {"admin", "user"})
void someTestMethod() {
...
}
```

This will run the test with an identity with the given username and roles. Note that these can be combined, so you can disable authorization while also providing an identity to run the test under, which can be useful if the endpoint expects an identity to be present.

See [OpenID Connect Bearer Token Integration testing](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing-security-annotation), [OpenID Connect Authorization Code Flow Integration testing](/guides/security-oidc-code-flow-authentication/index.md#code-flow-integration-testing-security-annotation) and [SmallRye JWT Integration testing](/guides/security-jwt/index.md#integration-testing-security-annotation) for more details about testing the endpoint code which depends on the injected `JsonWebToken`.

Additionally, you can specify attributes for the identity, perhaps custom items that were added with identity augmentation:

```java
@Inject
SecurityIdentity identity;

@Test
@TestSecurity(
    user = "testUser",
    roles = {"admin", "user"},
    attributes = { @SecurityAttribute(key = "answer", value = "42", type = AttributeType.LONG) })
void someTestMethod() {
    Long answer = identity.<Long>getAttribute("answer");
...
}
```

This will run the test with an identity with an attribute of type `Long` named `answer`.

> [!WARNING]
> The feature is only available for `@QuarkusTest` and will **not** work on a `@QuarkusIntegrationTest`.

> [!TIP]
> `@TestSecurity` can also be used in meta-annotations, for example like so:
>
> ```java
>     @Retention(RetentionPolicy.RUNTIME)
>     @Target({ ElementType.METHOD })
>     @TestSecurity(user = "testUser", roles = {"admin", "user"})
>     public @interface TestSecurityMetaAnnotation {
>
>     }
> ```
>
> This is particularly useful if the same set of security settings needs to be used in multiple test methods.

The `@TestSecurity` annotation also works with the `@PermissionsAllowed` security annotation. Consider following example:

```java
@Test
@TestSecurity(user = "testUser", permissions = "see:detail")
void someTestMethod() {
    ...
}
```

This will run the test with an identity possessing permission `see` and action `detail`. Consequently, call to the `getDetail` method declared in the example below will succeed:

```java
@PermissionsAllowed("see:detail")
public String getDetail() {
    return "detail";
}
```

It is also possible to set custom permissions like in the example below:

```java
@PermissionsAllowed("see", permission = CustomPermission.class)
public String getDetail() {
    return "detail";
}
```

The `CustomPermission` needs to be granted to the `SecurityIdentity` created by the `@TestSecurity` annotation with a `SecurityIdentityAugmentor` CDI bean:

```java
@ApplicationScoped
public class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {
    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        final SecurityIdentity augmentedIdentity;
        if (shouldGrantCustomPermission(securityIdentity) {
            augmentedIdentity = QuarkusSecurityIdentity.builder(securityIdentity)
                                   .addPermission(new CustomPermission("see")).build();
        } else {
            augmentedIdentity = securityIdentity;
        }
        return Uni.createFrom().item(augmentedIdentity);
    }
}
```

Quarkus will only augment the `SecurityIdentity` created with the `@TestSecurity` annotation if you set the `@TestSecurity#augmentors` annotation attribute to the `CustomSecurityIdentityAugmentor.class` like this:

```java
@Test
@TestSecurity(user = "testUser", permissions = "see:detail", augmentors = CustomSecurityIdentityAugmentor.class)
void someTestMethod() {
    ...
}
```

### Mixing security tests

If it becomes necessary to test security features using both `@TestSecurity` and Basic Auth (which is the fallback auth mechanism when none is defined), then Basic Auth needs to be enabled explicitly, for example by setting `quarkus.http.auth.basic=true` or `%test.quarkus.http.auth.basic=true`.

Similarly, if it becomes necessary to test security features using both `@TestSecurity` and Bearer token authentication, you can leverage both like in the example below:

```java
@Test
@TestSecurity(user = "Bob")
public void testSecurityMetaAnnotation {
    RestAssured.given()
            .auth().oauth2(getTokenForUser("Alice")) <1>
            .get("hello")
            .then()
            .statusCode(200)
            .body(Matchers.is("Hello Alice"));
    RestAssured.given()
            .get("hello")
            .then()
            .statusCode(200)
            .body(Matchers.is("Hello Bob")); <2>
}

@Path("hello")
public static class HelloResource {

  @Inject
  SecurityIdentity identity;

  @Authenticated
  @GET
  public String sayHello() {
       return "Hello " + identity.getPrincipal().getName();
  }
}
```

1. Bearer token authentication mechanism is used, because a Bearer access token is sent with the HTTP request.
2. No authorization header is present, therefore the Test Security Extension creates user `Bob`.

### Path-based authentication

`@TestSecurity` can also be used when [authentication mechanisms must be combined](/guides/security-authentication-mechanisms/index.md#combining-authentication-mechanisms). Example below shows how to select authentication mechanism when path-based authentication is enabled.

```java
@Test
@TestSecurity(user = "testUser", roles = {"admin", "user"}, authMechanism = "basic") <1>
void basicTestMethod() {
    ...
}

@Test
@TestSecurity(user = "testUser", roles = {"admin", "user"}, authMechanism = "form") <2>
void formTestMethod() {
    ...
}
```

1. The 'authMechanism' attribute selects Basic authentication.
2. The 'authMechanism' attribute selects Form-based authentication.

In your Quarkus application, it is possible to use annotations to select an authentication mechanism specific to each Jakarta REST endpoint:

```java
package org.acme.security.testing;

import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class TestSecurityResource {

    @BasicAuthentication <1>
    @GET
    @Path("basic-only")
    public String basicOnly() {
        return "basic-only";
    }

    @FormAuthentication <2>
    @GET
    @Path("form-only")
    public String formOnly() {
        return "form-only";
    }
}
```

1. All HTTP requests to the `/basic-only` path from the `basicTestMethod` test are authenticated successfully.
2. Same HTTP requests will fail when invoked from the `formTestMethod` test as Basic authentication is required.

Alternatively, it is possible to select path-specific authentication mechanism with HTTP Security Policy:

```properties
# require basic authentication for the '/basic-only' path
quarkus.http.auth.permission.basic.paths=/basic-only
quarkus.http.auth.permission.basic.policy=authenticated
quarkus.http.auth.permission.basic.auth-mechanism=basic

# require form-based authentication for the '/form-only' path
quarkus.http.auth.permission.form.paths=/form-only
quarkus.http.auth.permission.form.policy=authenticated
quarkus.http.auth.permission.form.auth-mechanism=form
```

## Use Wiremock for Integration Testing

You can also use Wiremock to mock the authorization OAuth2 and OIDC services: See [OAuth2 Integration testing](/guides/security-oauth2/index.md#integration-testing), [OpenID Connect Bearer Token Integration testing](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing-wiremock), [OpenID Connect Authorization Code Flow Integration testing](/guides/security-oidc-code-flow-authentication/index.md#code-flow-integration-testing-wiremock) and [SmallRye JWT Integration testing](/guides/security-jwt/index.md#integration-testing-wiremock) for more details.

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
