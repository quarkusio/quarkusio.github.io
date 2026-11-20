# Using OpenID Connect (OIDC) multitenancy

This guide demonstrates how your OpenID Connect (OIDC) application can support multitenancy to serve multiple tenants from a single application. These tenants can be distinct realms or security domains within the same OIDC provider or even distinct OIDC providers.

Each customer functions as a distinct tenant when serving multiple customers from the same application, such as in a SaaS environment. By enabling multitenancy support to your applications, you can support distinct authentication policies for each tenant, even authenticating against different OIDC providers, such as Keycloak and Google.

To authorize a tenant by using Bearer Token Authorization, see the [OpenID Connect (OIDC) Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md) guide.

To authenticate and authorize a tenant by using the OIDC authorization code flow, read the [OpenID Connect authorization code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md) guide.

Also, see the [OpenID Connect (OIDC) configuration properties](/guides/security-oidc-configuration-properties-reference/index.md) reference guide.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- [jq tool](https://stedolan.github.io/jq/)

## Architecture

In this example, we build a very simple application that supports two resource methods:

- `/{tenant}`

This resource returns information obtained from the ID token issued by the OIDC provider about the authenticated user and the current tenant.

- `/{tenant}/bearer`

This resource returns information obtained from the Access Token issued by the OIDC provider about the authenticated user and the current tenant.

## Solution

For a thorough understanding, we recommend you build the application by following the upcoming step-by-step instructions.

Alternatively, if you prefer to start with the completed example, clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `security-openid-connect-multi-tenancy-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-multi-tenancy-quickstart).

## Creating the Maven project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-openid-connect-multi-tenancy-quickstart \
    --extension='oidc,rest-jackson' \
    --no-code
cd security-openid-connect-multi-tenancy-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-openid-connect-multi-tenancy-quickstart \
    -Dextensions='oidc,rest-jackson' \
    -DnoCode
cd security-openid-connect-multi-tenancy-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-openid-connect-multi-tenancy-quickstart"`

If you already have your Quarkus project configured, add the `oidc` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add oidc
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='oidc'
```

**Gradle**

```bash
./gradlew addExtension --extensions='oidc'
```

This adds the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-oidc")
```

## Writing the application

Start by implementing the `/{tenant}` endpoint. As you can see from the source code below, it is just a regular Jakarta REST resource:

```java
package org.acme.quickstart.oidc;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;

@Path("/{tenant}")
public class HomeResource {
    /**
     * Injection point for the ID Token issued by the OIDC provider.
     */
    @Inject
    @IdToken
    JsonWebToken idToken;

    /**
     * Injection point for the Access Token issued by the OIDC provider.
     */
    @Inject
    JsonWebToken accessToken;

    /**
     * Returns the ID Token info.
     * This endpoint exists only for demonstration purposes.
     * Do not expose this token in a real application.
     *
     * @return ID Token info
     */
    @GET
    @Produces("text/html")
    public String getIdTokenInfo() {
        StringBuilder response = new StringBuilder().append("<html>")
                .append("<body>");

        response.append("<h2>Welcome, ").append(this.idToken.getClaim("email").toString()).append("</h2>\n");
        response.append("<h3>You are accessing the application within tenant <b>").append(idToken.getIssuer()).append(" boundaries</b></h3>");

        return response.append("</body>").append("</html>").toString();
    }

    /**
     * Returns the Access Token info.
     * This endpoint exists only for demonstration purposes.
     * Do not expose this token in a real application.
     *
     * @return Access Token info
     */
    @GET
    @Produces("text/html")
    @Path("bearer")
    public String getAccessTokenInfo() {
        StringBuilder response = new StringBuilder().append("<html>")
                .append("<body>");

        response.append("<h2>Welcome, ").append(this.accessToken.getClaim("email").toString()).append("</h2>\n");
        response.append("<h3>You are accessing the application within tenant <b>").append(accessToken.getIssuer()).append(" boundaries</b></h3>");

        return response.append("</body>").append("</html>").toString();
    }
}

```

To resolve the tenant from incoming requests and map it to a specific `quarkus-oidc` tenant configuration in `application.properties`, create an implementation for the `io.quarkus.oidc.TenantConfigResolver` interface, which can dynamically resolve tenant configurations:

```java
package org.acme.quickstart.oidc;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantConfigResolver {

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        String path = context.request().path();

        if (path.startsWith("/tenant-a")) {
           String keycloakUrl = ConfigProvider.getConfig().getValue("keycloak.url", String.class);

            OidcTenantConfig config = OidcTenantConfig
                    .authServerUrl(keycloakUrl + "/realms/tenant-a")
                    .tenantId("tenant-a")
                    .clientId("multi-tenant-client")
                    .credentials("secret")
                    .applicationType(ApplicationType.HYBRID)
                    .build();
            return Uni.createFrom().item(config);
        } else {
            // resolve to default tenant config
            return Uni.createFrom().nullItem();
        }
    }
}
```

In the preceding implementation, tenants are resolved from the request path. If no tenant can be inferred, `null` is returned to indicate that the default tenant configuration should be used.

The `tenant-a` application type is `hybrid`; it can accept HTTP bearer tokens if provided. Otherwise, it initiates an authorization code flow when authentication is required.

## Configuring the application

```properties
# Default tenant configuration
%prod.quarkus.oidc.auth-server-url=http://localhost:8180/realms/quarkus
quarkus.oidc.client-id=multi-tenant-client
quarkus.oidc.credentials.secret=secret
quarkus.oidc.application-type=web-app

# Tenant A configuration is created dynamically in CustomTenantConfigResolver

# HTTP security configuration
quarkus.http.auth.permission.authenticated.paths=/*
quarkus.http.auth.permission.authenticated.policy=authenticated
```

The first configuration is the default tenant configuration that should be used when the tenant cannot be inferred from the request. Be aware that a `%prod` profile prefix is used with `quarkus.oidc.auth-server-url` to support testing a multitenant application with Dev Services For Keycloak. This configuration uses a Keycloak instance to authenticate users.

The second configuration, provided by `TenantConfigResolver`, is used when an incoming request is mapped to the `tenant-a` tenant.

Both configurations map to the same Keycloak server instance while using distinct `realms`.

Alternatively, you can configure the tenant `tenant-a` directly in `application.properties`:

```properties
# Default tenant configuration
%prod.quarkus.oidc.auth-server-url=http://localhost:8180/realms/quarkus
quarkus.oidc.client-id=multi-tenant-client
quarkus.oidc.credentials.secret=secret
quarkus.oidc.application-type=web-app

# Tenant A configuration
quarkus.oidc.tenant-a.auth-server-url=http://localhost:8180/realms/tenant-a
quarkus.oidc.tenant-a.client-id=multi-tenant-client
quarkus.oidc.tenant-a.credentials.secret=secret
quarkus.oidc.tenant-a.application-type=web-app

# HTTP security configuration
quarkus.http.auth.permission.authenticated.paths=/*
quarkus.http.auth.permission.authenticated.policy=authenticated
```

In that case, also use a custom `TenantConfigResolver` to resolve it:

```java
package org.acme.quickstart.oidc;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.request().path();
        String[] parts = path.split("/");

        if (parts.length == 0) {
            //Resolve to default tenant configuration
            return null;
        }

        return parts[1];
    }
}
```

You can define multiple tenants in your configuration file. To map them correctly when resolving a tenant from your `TenantResolver` implementation, ensure each has a unique alias.

However, using a static tenant resolution, which involves configuring tenants in `application.properties` and resolving them with `TenantResolver`, does not work for testing endpoints with Dev Services for Keycloak because it does not know how the requests are be mapped to individual tenants, and cannot dynamically provide tenant-specific `quarkus.oidc.<tenant-id>.auth-server-url` values. Therefore, using `%prod` prefixes with tenant-specific URLs within `application.properties` does not work in both test and development modes.

> [!NOTE]
> When a current tenant represents an OIDC `web-app` application, the current `io.vertx.ext.web.RoutingContext` contains a `tenant-id` attribute by the time the custom tenant resolver has been called for all the requests completing the code authentication flow and the already authenticated requests, when either a tenant-specific state or session cookie already exists. Therefore, when working with multiple OIDC providers, you only need a path-specific check to resolve a tenant id if the `RoutingContext` does not have the `tenant-id` attribute set, for example:
>
> ```java
> package org.acme.quickstart.oidc;
>
> import jakarta.enterprise.context.ApplicationScoped;
>
> import io.quarkus.oidc.TenantResolver;
> import io.vertx.ext.web.RoutingContext;
>
> @ApplicationScoped
> public class CustomTenantResolver implements TenantResolver {
>
>     @Override
>     public String resolve(RoutingContext context) {
>         String tenantId = context.get("tenant-id");
>         if (tenantId != null) {
>             return tenantId;
>         } else {
>             // Initial login request
>             String path = context.request().path();
>             String[] parts = path.split("/");
>
>             if (parts.length == 0) {
>                 //Resolve to default tenant configuration
>                 return null;
>             }
>             return parts[1];
>         }
>     }
> }
> ```
>
> This is how Quarkus OIDC resolves static custom tenants if no custom `TenantResolver` is registered.
>
> A similar technique can be used with `TenantConfigResolver`, where a `tenant-id` provided in the context can return `OidcTenantConfig` already prepared with the previous request.

> [!NOTE]
> If you also use Hibernate ORM multitenancy or MongoDB with Panache multitenancy and both tenant ids are the same, you can get the tenant id from the `RoutingContext` attribute with `tenant-id`. You can find more information here:
>
> - [Hibernate ORM multitenancy](/guides/hibernate-orm/index.md#multitenancy)
> - [MongoDB with Panache multitenancy](/guides/mongodb-panache/index.md#multitenancy)

## Starting and configuring the Keycloak server

To start a Keycloak server, you can use Docker and run the following command:

```bash
docker run --name keycloak -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -p 8180:8080 quay.io/keycloak/keycloak:26.7.4 start-dev
```

Access your Keycloak server at [localhost:8180](http://localhost:8180).

Log in as the `admin` user to access the Keycloak administration console. The username and password are both `admin`.

Now, import the realms for the two tenants:

- Import the [default-tenant-realm.json](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-multi-tenancy-quickstart/config/default-tenant-realm.json) to create the default realm.
- Import the [tenant-a-realm.json](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-multi-tenancy-quickstart/config/tenant-a-realm.json) to create the realm for the tenant `tenant-a`.

For more information, see the Keycloak documentation about how to [create a new realm](https://www.keycloak.org/docs/latest/server_admin/index.html#_create-realm).

## Running and using the application

### Running in developer mode

To run the microservice in dev mode, use:

**CLI**

```bash
quarkus dev
```

**Maven**

```bash
./mvnw quarkus:dev
```

**Gradle**

```bash
./gradlew --console=plain quarkusDev
```

### Running in JVM mode

After exploring the application in dev mode, you can run it as a standard Java application.

First, compile it:

**CLI**

```bash
quarkus build
```

**Maven**

```bash
./mvnw install
```

**Gradle**

```bash
./gradlew build
```

Then run it:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### Running in native mode

This same demo can be compiled into native code; no modifications are required.

This implies that you no longer need to install a JVM on your production environment, as the runtime technology is included in the produced binary, and optimized to run with minimal resources.

Compilation takes a bit longer, so this step is turned off by default; let's build again by enabling the native build:

**CLI**

```bash
quarkus build --native
```

**Maven**

```bash
./mvnw install -Dnative
```

**Gradle**

```bash
./gradlew build -Dquarkus.native.enabled=true
```

After a little while, you can run this binary directly:

```bash
./target/security-openid-connect-multi-tenancy-quickstart-runner
```

## Test the application

### Use Dev Services for Keycloak

[Dev Services for Keycloak](/guides/security-openid-connect-dev-services/index.md) is recommended for the integration testing against Keycloak. Dev Services for Keycloak launches and initializes a test container: it imports configured realms and sets a base Keycloak URL for the `CustomTenantResolver` to calculate a realm-specific URL.

First, add the following dependencies:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-keycloak-server</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.htmlunit</groupId>
    <artifactId>htmlunit</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-keycloak-server")
testImplementation("io.rest-assured:rest-assured")
testImplementation("org.htmlunit:htmlunit")
```

`quarkus-test-keycloak-server` provides a utility class `io.quarkus.test.keycloak.client.KeycloakTestClient` for acquiring the realm specific access tokens and which you can use with `RestAssured` for testing the `/{tenant}/bearer` endpoint expecting bearer access tokens. `HtmlUnit` tests the `/{tenant}` endpoint and the authorization code flow.

Next, configure the required realms:

```properties
# Default tenant configuration
%prod.quarkus.oidc.auth-server-url=http://localhost:8180/realms/quarkus
quarkus.oidc.client-id=multi-tenant-client
quarkus.oidc.application-type=web-app

# Tenant A configuration is created dynamically in CustomTenantConfigResolver

# HTTP security configuration
quarkus.http.auth.permission.authenticated.paths=/*
quarkus.http.auth.permission.authenticated.policy=authenticated

quarkus.keycloak.devservices.realm-path=default-tenant-realm.json,tenant-a-realm.json
```

Finally, write your test, which runs in JVM mode:

```java
package org.acme.quickstart.oidc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.htmlunit.SilentCssErrorHandler;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.restassured.RestAssured;

@QuarkusTest
public class CodeFlowTest {

    KeycloakTestClient keycloakClient = new KeycloakTestClient();

    @Test
    public void testLogInDefaultTenant() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/default");

            assertEquals("Sign in to quarkus", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertTrue(page.asNormalizedText().contains("tenant"));
        }
    }

    @Test
    public void testLogInTenantAWebApp() throws IOException {
        try (final WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage("http://localhost:8081/tenant-a");

            assertEquals("Sign in to tenant-a", page.getTitleText());

            HtmlForm loginForm = page.getForms().get(0);

            loginForm.getInputByName("username").setValueAttribute("alice");
            loginForm.getInputByName("password").setValueAttribute("alice");

            page = loginForm.getButtonByName("login").click();

            assertTrue(page.asNormalizedText().contains("alice@tenant-a.org"));
        }
    }

    @Test
    public void testLogInTenantABearerToken() throws IOException {
        RestAssured.given().auth().oauth2(getAccessToken()).when()
            .get("/tenant-a/bearer").then().body(containsString("alice@tenant-a.org"));
    }

    private String getAccessToken() {
        return keycloakClient.getRealmAccessToken("tenant-a", "alice", "alice", "multi-tenant-client", "secret");
    }

    private WebClient createWebClient() {
        WebClient webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        return webClient;
    }
}
```

In native mode:

```java
package org.acme.quickstart.oidc;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class CodeFlowIT extends CodeFlowTest {
}
```

For more information about how it is initialized and configured, see [Dev Services for Keycloak](/guides/security-openid-connect-dev-services/index.md).

### Use the browser

To test the application, open your browser and access the following URL:

- <http://localhost:8080/default>

If everything works as expected, you are redirected to the Keycloak server to authenticate. Be aware that the requested path defines a `default` tenant, which we don't have mapped in the configuration file. In this case, the default configuration is used.

To authenticate to the application, enter the following credentials in the Keycloak login page:

- Username: `alice`
- Password: `alice`

After clicking the **Login** button, you are redirected back to the application.

If you try now to access the application at the following URL:

- <http://localhost:8080/tenant-a>

You are redirected again to the Keycloak login page. However, this time, you are going to authenticate by using a different realm.

In both cases, the landing page shows the user's name and email if the user is successfully authenticated. Although `alice` exists in both tenants, the application treats them as distinct users in separate realms.

## Tenant resolution

<a id="tenant-resolution-order"></a>
### Tenant resolution order

OIDC tenants are resolved in the following order:

1. `io.quarkus.oidc.Tenant` annotation is checked first if the proactive authentication is disabled.

1. Dynamic tenant resolution using a custom `TenantConfigResolver`.

1. Static tenant resolution using one of these options: custom `TenantResolver`, configured tenant paths, and defaulting to the last request path segment as a tenant id.

Finally, the default OIDC tenant is selected if a tenant id has not been resolved after the preceding steps.

See the following sections for more information:

- [Resolve with annotations](#annotations-tenant-resolver)
- [Dynamic tenant configuration resolution](#tenant-config-resolver)
- [Static tenant configuration resolution](#static-tenant-resolution)

Additionally, for the OIDC `web-app` applications, the state and session cookies also provide a hint about the tenant resolved with one of the above mentioned options at the time when the authorization code flow started. See the [Tenant resolution for OIDC web-app applications](#tenant-resolution-for-web-app) section for more information.

<a id="annotations-tenant-resolver"></a>
### Resolve with annotations

You can use the `io.quarkus.oidc.Tenant` annotation for resolving the tenant identifiers as an alternative to using `io.quarkus.oidc.TenantResolver`.

> [!NOTE]
> Proactive HTTP authentication must be disabled (`quarkus.http.auth.proactive=false`) for this to work. For more information, see the [Proactive authentication](/guides/security-proactive-authentication/index.md) guide.

Assuming your application supports two OIDC tenants, the `hr` and default tenants, all resource methods and classes carrying `@Tenant("hr")` are authenticated by using the OIDC provider configured by `quarkus.oidc.hr.auth-server-url`. In contrast, all other classes and methods are still authenticated by using the default OIDC provider.

```java
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.Tenant;
import io.quarkus.security.Authenticated;

@Authenticated
@Path("/api/hello")
public class HelloResource {

    @Tenant("hr") <1>
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello() {
        return "Hello!";
    }
}
```

1. The `io.quarkus.oidc.Tenant` annotation must be placed on either the resource class or resource method.

> [!TIP]
> In the example above, authentication of the `sayHello` endpoint is enforced with the `@Authenticated` annotation.
>
> Alternatively, if you use an the [HTTP Security policy](/guides/security-authorize-web-endpoints-reference/index.md#authorization-using-configuration) to secure the endpoint, then, for the `@Tenant` annotation be effective, you must delay this policy's permission check as shown in the following example:
>
> ```properties
> quarkus.http.auth.permission.authenticated.paths=/api/hello
> quarkus.http.auth.permission.authenticated.methods=GET
> quarkus.http.auth.permission.authenticated.policy=authenticated
> quarkus.http.auth.permission.authenticated.applies-to=JAXRS <1>
> ```
>
> 1. Tell Quarkus to run the HTTP permission check after the tenant has been selected with the `@Tenant` annotation.

> [!NOTE]
> The `io.quarkus.oidc.Tenant` annotation can be used to select tenant for a WebSockets Next server endpoint. The annotation must be placed on the endpoint class, because the `SecurityIdentity` is created before the HTTP connection is upgraded to a WebSocket connection. For more information about the HTTP upgrade security, see the [Secure HTTP upgrade](/guides/websockets-next-reference/index.md#secure-http-upgrade) section of the Quarkus "WebSockets Next reference" guide.

<a id="tenant-config-resolver"></a>
### Dynamic tenant configuration resolution

If you need a more dynamic configuration for the different tenants you want to support and don't want to end up with multiple entries in your configuration file, you can use the `io.quarkus.oidc.TenantConfigResolver`.

This interface allows you to dynamically create tenant configurations at runtime:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        String path = context.request().path();
        String[] parts = path.split("/");

        if (parts.length == 0) {
            //Resolve to default tenant configuration
            return null;
        }

        if ("tenant-c".equals(parts[1])) {
            // Do 'return requestContext.runBlocking(createTenantConfig());'
            // if a blocking call is required to create a tenant config,
            return Uni.createFrom().item(createTenantConfig());
        }

        //Resolve to default tenant configuration
        return null;
    }

    private Supplier<OidcTenantConfig> createTenantConfig() {
        final OidcTenantConfig config = OidcTenantConfig
                .authServerUrl("http://localhost:8180/realms/tenant-c")
                .tenantId("tenant-c")
                .clientId("multi-tenant-client")
                .credentials("my-secret")
                .build();

        // Any other setting supported by the quarkus-oidc extension

        return () -> config;
    }
}
```

The `OidcTenantConfig` returned by this method is the same one used to parse the `oidc` namespace configuration from the `application.properties`. You can populate it by using any settings supported by the `quarkus-oidc` extension.

If the dynamic tenant resolver returns `null`, a [Static tenant configuration resolution](#static-tenant-resolution) is attempted next.

#### Update resolved dynamic tenant configuration

It may be necessary to update the already resolved tenant configuration. For example, a client secret may have to be updated following a client secret update in the registered OIDC application.

To update the configuration, use `OidcTenantConfigBuilder` to create a new instance of `OidcTenantConfig` and modify it as required before returning it:

```java
package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    TenantConfigBean tenantConfigBean;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {

        // Check request path or request context `tenant-id` property to determine the tenant id.

        var currentTenantConfig = tenantConfigBean.getDynamicTenant("some-dynamic-tenant-id").getOidcTenantConfig(); <1>
        if (currentTenantConfig != null
            && "name".equals(currentTenantConfig.token().principalClaim().get())) { <2>
            // This is an original configuration, update it now:
            OidcTenantConfig updatedConfig = OidcTenantConfig.builder(currentTenantConfig) <3>
                                .token().principalClaim("email").end()
                                .build();

            return Uni.createFrom().item(updatedConfig);
        }
        // create an initial configuration for the tenant
        OidcTenantConfig config = OidcTenantConfig.builder() <4>
                                .token().principalClaim("name").end()
                                // set other properties
                                .build();
        return Uni.createFrom().item(config);
    }
}
```

1. Use `io.quarkus.oidc.runtime.TenantConfigBean` to get the already resolved tenant configuration. Alternatively, you can use the tenant `OidcTenantConfig` cached in your resolver.
2. You may want to check if this configuration has already been updated, to avoid multiple redundant updates, for example, due to multliple redirects.
3. Use the resolved configuration to create a builder and update it as required.
4. Create an initial configuration if no configuration already exists.

This is all you have to do update the already resolved dynamic tenantr configuration, without having to reconnect to the provider.

If reconnecting is necessary, for example, the `UserInfo` endpoint address may have changed for the tenant to rediscover it, then simply set a `RoutingContext` `replace-tenant-configuration-context` property to `true`:

```java
@Override
public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
    OidcTenantConfig updatedConfig = OidcTenantConfig.builder(currentTenantConfig)
                                    .token().principalClaim("email").end()
                                    .build();
    context.put("replace-tenant-configuration-context", "true"); <1>

    return Uni.createFrom().item(updatedConfig);
}
```

1. Replace the resolved tenant configuration and re-connect to the provider

Finally, if you decide to update the resoved configuration while the existing OIDC session is still active, you may to have the session cookie removed and the user re-authenticated to align with the latest tenant configuration requirements. Set a `RoutingContext` `remove-session-cookie` property to `true` if it is necessary:

```java
@Override
public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
    OidcTenantConfig updatedConfig = OidcTenantConfig.builder(currentTenantConfig)
                                    .token().principalClaim("email").end()
                                    .build();
    context.put("remove-session-cookie", "true"); <1>

    return Uni.createFrom().item(updatedConfig);
}
```

1. Update the tenant configuration, remove the session cookie and triger the user re-authentication. If possible, prefer to get the user log-out first, instead of triggering the re-authentication at the tenant resolution time.

<a id="static-tenant-resolution"></a>
### Static tenant configuration resolution

When you set multiple tenant configurations in the `application.properties` file, you only need to specify how the tenant identifier gets resolved. To configure the resolution of the tenant identifier, use one of the following options:

- [Resolve with TenantResolver](#tenant-resolver)
- [Configure tenant paths](#configure-tenant-paths)
- [Resolve tenants with a token issuer claim](#issuer-based-tenant-resolver)
- [Resolve tenants with a token header name](#header-based-tenant-resolver)
- [Use request path segments to find tenant id](#default-tenant-resolver)

These tenant resolution options are tried in the order they are listed until the tenant id gets resolved. If the tenant id remains unresolved (`null`), the default (unnamed) tenant configuration is selected.

<a id="tenant-resolver"></a>
#### Resolve with `TenantResolver`

The following `application.properties` example shows how you can resolve the tenant identifier of two tenants named `a` and `b` by using the `TenantResolver` method:

```properties
# Tenant 'a' configuration
quarkus.oidc.a.auth-server-url=http://localhost:8180/realms/quarkus-a
quarkus.oidc.a.client-id=client-a
quarkus.oidc.a.credentials.secret=client-a-secret

# Tenant 'b' configuration
quarkus.oidc.b.auth-server-url=http://localhost:8180/realms/quarkus-b
quarkus.oidc.b.client-id=client-b
quarkus.oidc.b.credentials.secret=client-b-secret
```

You can return the tenant id of either `a` or `b` from `io.quarkus.oidc.TenantResolver`:

```java
import io.quarkus.oidc.TenantResolver;
import io.vertx.ext.web.RoutingContext;

public class CustomTenantResolver implements TenantResolver {

    @Override
    public String resolve(RoutingContext context) {
        String path = context.request().path();
        if (path.endsWith("a")) {
            return "a";
        } else if (path.endsWith("b")) {
            return "b";
        } else {
            // default tenant
            return null;
        }
    }
}
```

In this example, the value of the last request path segment is a tenant id, but if required, you can implement a more complex tenant identifier resolution logic.

<a id="configure-tenant-paths"></a>
#### Configure tenant paths

You can use the `quarkus.oidc.tenant-paths` configuration property for resolving the tenant identifier as an alternative to using `io.quarkus.oidc.TenantResolver`. Here is how you can select the `hr` tenant for the `sayHello` endpoint of the `HelloResource` resource used in the previous example:

```properties
quarkus.oidc.hr.tenant-paths=/api/hello <1>
quarkus.oidc.a.tenant-paths=/api/* <2>
quarkus.oidc.b.tenant-paths=/*/hello <3>
```

1. Same path-matching rules apply as for the `quarkus.http.auth.permission.authenticated.paths=/api/hello` configuration property from the previous example.
2. The wildcard placed at the end of the path represents any number of path segments. However the path is less specific than the `/api/hello`, therefore the `hr` tenant will be used to secure the `sayHello` endpoint.
3. The wildcard in the `/*/hello` represents exactly one path segment. Nevertheless, the wildcard is less specific than the `api`, therefore the `hr` tenant will be used.

> [!TIP]
> Path-matching mechanism works exactly same as in the [Authorization using configuration](/guides/security-authorize-web-endpoints-reference/index.md#authorization-using-configuration).

<a id="issuer-based-tenant-resolver"></a>
#### Resolve tenants with a token issuer claim

OIDC tenants which support Bearer token authentication can be resolved using the access token's issuer. The following conditions must be met for the issuer-based resolution to work:

- The access token must be in the JWT format and contain an issuer (`iss`) token claim.
- Only OIDC tenants with the application type `service` or `hybrid` are considered. These tenants must have a token issuer discovered or configured.

The issuer-based resolution is enabled with the `quarkus.oidc.resolve-tenants-with-issuer` property. For example:

```properties
quarkus.oidc.resolve-tenants-with-issuer=true <1>

quarkus.oidc.tenant-a.auth-server-url=${tenant-a-oidc-provider} <2>
quarkus.oidc.tenant-a.client-id=${tenant-a-client-id}
quarkus.oidc.tenant-a.credentials.secret=${tenant-a-client-secret}

quarkus.oidc.tenant-b.auth-server-url=${tenant-b-oidc-provider} <3>
quarkus.oidc.tenant-b.discover-enabled=false
quarkus.oidc.tenant-b.token.issuer=${tenant-b-oidc-provider}/issuer
quarkus.oidc.tenant-b.jwks-path=/jwks
quarkus.oidc.tenant-b.token-path=/tokens
quarkus.oidc.tenant-b.client-id=${tenant-b-client-id}
quarkus.oidc.tenant-b.credentials.secret=${tenant-b-client-secret}
```

1. Tenants `tenant-a` and `tenant-b` are resolved using a JWT access token's issuer `iss` claim value.
2. Tenant `tenant-a` discovers the `issuer` from the OIDC provider's well-known configuration endpoint.
3. Tenant `tenant-b` configures the `issuer` because its OIDC provider does not support the discovery.

<a id="header-based-tenant-resolver"></a>
#### Resolve tenants with a token header name

OIDC tenants that support Bearer token authentication can be resolved using the custom HTTP header that contains the token. Header-based resolution is enabled when the `quarkus.oidc.token.header` property is set to a value other than `Authorization`. For example:

```properties
quarkus.oidc.tenant-a.token.header=Custom-Authorization-1 <1>
quarkus.oidc.tenant-b.token.header=Custom-Authorization-2 <2>
```

1. Tenant `tenant-a` is resolved when the incoming HTTP request contains the `Custom-Authorization-1` header.
2. Tenant `tenant-b` is resolved if the request contains the `Custom-Authorization-2` header.

<a id="default-tenant-resolver"></a>
#### Use request path segments to find tenant id

The default resolution for a tenant identifier is convention based, whereby the authentication request must include the tenant identifier in one of the path segments of the request path.

The following `application.properties` example shows how you can configure two tenants named `google` and `github`:

```properties
# Tenant 'google' configuration
quarkus.oidc.google.provider=google
quarkus.oidc.google.client-id=${google-client-id}
quarkus.oidc.google.credentials.secret=${google-client-secret}
quarkus.oidc.google.authentication.redirect-path=/signed-in

# Tenant 'github' configuration
quarkus.oidc.github.provider=github
quarkus.oidc.github.client-id=${github-client-id}
quarkus.oidc.github.credentials.secret=${github-client-secret}
quarkus.oidc.github.authentication.redirect-path=/signed-in
```

In the provided example, both tenants configure OIDC `web-app` applications to use an authorization code flow to authenticate users and require session cookies to be generated after authentication. After Google or GitHub authenticates the current user, the user gets returned to the `/signed-in` area for authenticated users, such as a secured resource path on the JAX-RS endpoint.

Finally, to complete the default tenant resolution, set the following configuration property:

```properties
quarkus.http.auth.permission.login.paths=/google,/github
quarkus.http.auth.permission.login.policy=authenticated
```

If the endpoint is running on `http://localhost:8080`, you can also provide UI options for users to log in to either `http://localhost:8080/google` or `http://localhost:8080/github`, without having to add specific `/google` or `/github` JAX-RS resource paths.

Tenant identifiers are also recorded in the session cookie names after the authentication is completed. Therefore, authenticated users can access the secured application area without requiring either the `google` or `github` path values to be included in the secured URL.

Default resolution can also work for Bearer token authentication. Still, it might be less practical because a tenant identifier must always be set as the last path segment value.

<a id="restrict-oidc-filter-to-tenants"></a>
#### Restricting OIDC request and response filters to specific tenants

Both the `io.quarkus.oidc.common.OidcRequestFilter` and the `io.quarkus.oidc.common.OidcResponseFilter` filters can be restricted to specific tenants like in the example below:

```java
package io.quarkus.it.oidc;

import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@TenantFeature({ "tenant-one", "tenant-two" }) <1>
public class CustomOidcRequestFilter implements OidcRequestFilter {

    @Override
    public Uni<Void> filter(OidcRequestFilterContext requestContext) {
        requestContext.request().putHeader("custom-header-name", "custom-header-value");
        return Uni.createFrom().voidItem();
    }
}
```

1. Restrict the `CustomOidcRequestFilter` filter to OIDC tenants `tenant-one` and `tenant-two`.

An OIDC response filter can be restricted to the specific OIDC endpoint or endpoints with the `quarkus.oidc.common.OidcEndpoint` annotation.

<a id="tenant-resolution-for-web-app"></a>
### Tenant resolution for OIDC web-app applications

Tenant resolution for the OIDC `web-app` applications must be done at least 3 times during an authorization code flow, when the OIDC tenant-specific configuration affects how each of the following steps is run.

When an unauthenticated user accesses a secured path, the user is redirected to the OIDC provider to authenticate and the tenant configuration is used to build the redirect URI.

All the static and dynamic tenant resolution options listed in the [Static tenant configuration resolution](#static-tenant-resolution) and [Dynamic tenant configuration resolution](#tenant-config-resolver) sections can be used to resolve a tenant.

After the provider authentication, the user is redirected back to the Quarkus endpoint and the tenant configuration is used to complete the authorization code flow.

All the static and dynamic tenant resolution options listed in the [Static tenant configuration resolution](#static-tenant-resolution) and [Dynamic tenant configuration resolution](#tenant-config-resolver) sections can be used to resolve a tenant. Before the tenant resolution begins, the authorization code flow `state cookie` is used to set the already resolved tenant configuration id as a RoutingContext `tenant-id` attribute: both custom dynamic `TenantConfigResolver` and static `TenantResolver` tenant resolvers can check it.

The tenant configuration determines how the session cookie is verified and refreshed. Before the tenant resolution begins, the authorization code flow `session cookie` is used to set the already resolved tenant configuration id as a RoutingContext `tenant-id` attribute: both custom dynamic `TenantConfigResolver` and static `TenantResolver` tenant resolvers can check it.

For example, here is how a custom `TenantConfigResolver` can avoid creating the already resolved tenant configuration, that may otherwise require blocking reads from the database or other remote sources:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        String resolvedTenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (resolvedTenantId != null) { <1>
            return null;
        }

        String path = context.request().path(); <2>
        if (path.endsWith("tenant-a")) {
            return Uni.createFrom().item(createTenantConfig("tenant-a", "client-a", "secret-a"));
        } else if (path.endsWith("tenant-b")) {
            return Uni.createFrom().item(createTenantConfig("tenant-b", "client-b", "secret-b"));
        }

        // Default tenant id
        return null;
    }

    private OidcTenantConfig createTenantConfig(String tenantId, String clientId, String secret) {
        final OidcTenantConfig config = OidcTenantConfig
                .authServerUrl("http://localhost:8180/realms/"  + tenantId)
                .tenantId(tenantId)
                .clientId(clientId)
                .credentials(secret)
                .applicationType(ApplicationType.WEB_APP)
                .build();
        return config;
    }
}
```

1. Let Quarkus use the already resolved tenant configuration if it has been resolved earlier.
2. Check the request path to create tenant configurations.

The default configuration may look like this:

```properties
quarkus.oidc.auth-server-url=http://localhost:8180/realms/default
quarkus.oidc.client-id=client-default
quarkus.oidc.credentials.secret=secret-default
quarkus.oidc.application-type=web-app
```

The preceding example assumes that the `tenant-a`, `tenant-b` and default tenants are all used to protect the same endpoint paths. In other words, after the user has authenticated with the `tenant-a` configuration, this user will not be able to choose to authenticate with the `tenant-b` or default configuration before this user logs out and has a session cookie cleared or expired.

The situation where multiple OIDC `web-app` tenants protect the tenant-specific paths is less typical and also requires an extra care. When multiple OIDC `web-app` tenants such as `tenant-a`, `tenant-b` and default tenants are used to control access to the tenant specific paths, the users authenticated with one OIDC provider must not be able to access the paths requiring an authentication with another provider, otherwise the results can be unpredictable, most likely causing unexpected authentication failures. For example, if the `tenant-a` authentication requires a Keycloak authentication and the `tenant-b` authentication requires an Auth0 authentication, then, if the `tenant-a` authenticated user attempts to access a path secured by the `tenant-b` configuration, then the session cookie will not be verified, since the Auth0 public verification keys can not be used to verify the tokens signed by Keycloak. An easy, recommended way to avoid multiple `web-app` tenants conflicting with each other is to set the tenant specific session path as shown in the following example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        String resolvedTenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (resolvedTenantId != null) { <1>
            return null;
        }

        String path = context.request().path(); <2>
        if (path.endsWith("tenant-a")) {
            return Uni.createFrom().item(createTenantConfig("tenant-a", "/tenant-a", "client-a", "secret-a"));
        } else if (path.endsWith("tenant-b")) {
            return Uni.createFrom().item(createTenantConfig("tenant-b", "/tenant-b", "client-b", "secret-b"));
        }

        // Default tenant id
        return null;
    }

    private OidcTenantConfig createTenantConfig(String tenantId, String cookiePath, String clientId, String secret) {
        final OidcTenantConfig config = OidcTenantConfig
                .authServerUrl("http://localhost:8180/realms/" + tenantId)
                .tenantId(tenantId)
                .clientId(clientId)
                .credentials(secret)
                .applicationType(ApplicationType.WEB_APP)
                .authentication().cookiePath(cookiePath).end() <3>
                .build();
        return config;
    }
}
```

1. Let Quarkus use the already resolved tenant configuration if it has been resolved earlier.
2. Check the request path to create tenant configurations.
3. Set the tenant-specific cookie paths which makes sure the session cookie is only visible to the tenant which created it.

The default tenant configuration should be adjusted like this:

```properties
quarkus.oidc.auth-server-url=http://localhost:8180/realms/default
quarkus.oidc.client-id=client-default
quarkus.oidc.credentials.secret=secret-default
quarkus.oidc.authentication.cookie-path=/default
quarkus.oidc.application-type=web-app
```

Having the same session cookie path when multiple OIDC `web-app` tenants protect the tenant-specific paths is not recommended and should be avoided as it requires even more care from the custom resolvers, for example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {

        String path = context.request().path(); <1>
        if (path.endsWith("tenant-a")) {
            String resolvedTenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
	    if (resolvedTenantId != null) {
	        if ("tenant-a".equals(resolvedTenantId)) { <2>
	            return null;
	        } else {
	           // Require a "tenant-a" authentication
                   context.remove(OidcUtils.TENANT_ID_ATTRIBUTE); <3>
	        }
            }
            return Uni.createFrom().item(createTenantConfig("tenant-a", "client-a", "secret-a"));
        } else if (path.endsWith("tenant-b")) {
            String resolvedTenantId = context.get(OidcUtils.TENANT_ID_ATTRIBUTE);
	    if (resolvedTenantId != null) {
	        if ("tenant-b".equals(resolvedTenantId)) { <2>
	            return null;
	        } else {
	            // Require a "tenant-b" authentication
                   context.remove(OidcUtils.TENANT_ID_ATTRIBUTE); <3>
	        }
            }
            return Uni.createFrom().item(createTenantConfig("tenant-b", "client-b", "secret-b"));
        }

        // Set default tenant id
        context.put(OidcUtils.TENANT_ID_ATTRIBUTE, OidcUtils.DEFAULT_TENANT_ID); <4>
        return null;
    }

    private OidcTenantConfig createTenantConfig(String tenantId, String clientId, String secret) {
        final OidcTenantConfig config = OidcTenantConfig
            .authServerUrl("http://localhost:8180/realms/"  + tenantId)
            .tenantId(tenantId)
            .clientId(clientId)
            .credentials(secret)
            .applicationType(ApplicationType.WEB_APP)
            .build();
        return config;
    }
}
```

1. Check the request path to create tenant configurations.
2. Let Quarkus use the already resolved tenant configuration if the already resolved tenant is expected for the current path.
3. Remove the `tenant-id` attribute if the already resolved tenant configuration is not expected for the current path.
4. Use the default tenant for all other paths. It is equivalent to removing the `tenant-id` attribute.

<a id="disable-tenant"></a>
## Disabling tenant configurations

Custom `TenantResolver` and `TenantConfigResolver` implementations might return `null` if no tenant can be inferred from the current request and a fallback to the default tenant configuration is required.

If you expect the custom resolvers always to resolve a tenant, you do not need to configure the default tenant resolution.

- To turn off the default tenant configuration, set  `quarkus.oidc.tenant-enabled=false`.

> [!NOTE]
> The default tenant configuration is automatically disabled when `quarkus.oidc.auth-server-url` is not configured, but either custom tenant configurations are available or `TenantConfigResolver` is registered.

Be aware that tenant-specific configurations can also be disabled, for example: `quarkus.oidc.tenant-a.tenant-enabled=false`.

<a id="programmatic-startup"></a>
## Programmatic OIDC start-up for multiple tenants

Static OIDC tenants can be created programmatically like in the example below:

```java
package io.quarkus.it.oidc;

import io.quarkus.oidc.Oidc;
import io.quarkus.oidc.OidcTenantConfig;
import jakarta.enterprise.event.Observes;

public class OidcStartup {

    void observe(@Observes Oidc oidc) { <1>
        oidc.create(OidcTenantConfig.authServerUrl("http://localhost:8180/realms/tenant-one").tenantId("tenant-one").build()); <2>
        oidc.create(OidcTenantConfig.authServerUrl("http://localhost:8180/realms/tenant-two").tenantId("tenant-two").build()); <3>
    }

}
```

1. Observe OIDC event.
2. Create OIDC tenant 'tenant-one'.
3. Create OIDC tenant 'tenant-two'.

The code above is a programmatic equivalent to the following configuration in the `application.properties` file:

```properties
quarkus.oidc.tenant-one.auth-server-url=http://localhost:8180/realms/tenant-one
quarkus.oidc.tenant-two.auth-server-url=http://localhost:8180/realms/tenant-two
```

## References

- [OIDC configuration properties](/guides/security-oidc-configuration-properties-reference/index.md)
- [Keycloak Documentation](https://www.keycloak.org/documentation.html)
- [OpenID Connect](https://openid.net/connect/)
- [JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [Google OpenID Connect](https://developers.google.com/identity/protocols/OpenIDConnect)
- [Quarkus Security overview](/guides/security-overview/index.md)
