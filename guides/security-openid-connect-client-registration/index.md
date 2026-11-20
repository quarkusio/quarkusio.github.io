# OpenID Connect (OIDC) and OAuth2 dynamic client registration

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

Typically, you have to register an OIDC client (application) manually in your OIDC provider's dashboard. During this process, you specify the human readable application name, allowed redirect and post logout URLs, and other properties. After the registration has been completed, you copy the generated client id and secret to your Quarkus OIDC application properties.

OpenID Connect and OAuth2 dynamic client registration allows you to register OIDC clients dynamically, and manage individual client registrations. You can read more about it in the [OIDC client registration](https://openid.net/specs/openid-connect-registration-1_0.html) and [OAuth2 Dynamic Client Registration Management Protocol](https://datatracker.ietf.org/doc/html/rfc7592) specification documents.

You can use Quarkus `quarkus-oidc-client-registration` extension to register one or more clients using OIDC client registration configurations and read, update and delete metadata of the registered clients.

[OIDC TenantConfigResolver](/guides/security-openid-connect-multitenancy/index.md#tenant-config-resolver) can be used to create OIDC tenant configurations using the metadata of the registered clients.

> [!IMPORTANT]
> Currently, Quarkus `quarkus-oidc-client-registration` extension has an `experimental` status. Dynamic client registration API provided by this extension may change while this extension has an experiemental status.

## OIDC Client Registration

Add the following dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc-client-registration</artifactId>
</dependency>
```

The `quarkus-oidc-client-registration` extension allows register one or more clients using OIDC client registration configurations, either on start-up or on demand, and read, update and delete metadata of the registered clients.

You can register and manage client registrations from the custom [OIDC TenantConfigResolver](/guides/security-openid-connect-multitenancy/index.md#tenant-config-resolver).

Alternatively, you can register clients without even using OIDC. For example, it can be a command line tool which registers clients and passes metadata of the registered clients to Quarkus services which require them.

Each OIDC client registration configuration represents an OIDC client registration endpoint which can accept many individual client registrations.

<a id="register-clients-on-startup"></a>
### Register clients on start-up

You start with declaring one or more OIDC client registration configurations, for example:

```properties
# Default OIDC client registration which auto-discovers a standard client registration endpoint.
# It does not require an initial registration token.

quarkus.oidc-client-registration.auth-server-url=${quarkus.oidc.auth-server-url}
quarkus.oidc-client-registration.metadata.client-name=Default Client
quarkus.oidc-client-registration.metadata.redirect-uri=http://localhost:8081/protected

# Named OIDC client registration which configures a registration endpoint path:
# It require an initial registration token for a client registration to succeed.

quarkus.oidc-client-registration.tenant-client.registration-path=${quarkus.oidc.auth-server-url}/clients-registrations/openid-connect
quarkus.oidc-client-registration.tenant-client.metadata.client-name=Tenant Client
quarkus.oidc-client-registration.tenant-client.metadata.redirect-uri=http://localhost:8081/protected/tenant
quarkus.oidc-client-registration.initial-token=${initial-registration-token}
```

The above configuration will lead to two new client registrations created in your OIDC provider.

You or may not need to acquire an initial registration access token. If you don't, then you will have to enable one or more client registration policies in your OIDC provider's dashboard. For example, see [Keycloak client registration policies](https://www.keycloak.org/securing-apps/client-registration).

The next step is to inject either `quarkus.oidc.client.registration.OidcClientRegistration` if only a single default client registration is done, or `quarkus.oidc.client.registration.OidcClientRegistrations` if more than one registration is configured, and use metadata of the clients registered with these configurations.

For example:

```java
package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcClientRegistration clientReg;

    @Inject
    OidcClientRegistrations clientRegs;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {

        if (routingContext.request().path().endsWith("/protected")) {
            // Use the registered client created from the default OIDC client registration
            return clientReg.registeredClient().onItem().transform(client -> createTenantConfig("registered-client", client));
        } else if (routingContext.request().path().endsWith("/protected/tenant")) {
            // Use the registered client created from the named 'tenant-client' OIDC client registration
            OidcClientRegistration tenantClientReg = clientRegs.getClientRegistration("tenant-client");
            return tenantClientReg.registeredClient().onItem().transform(client -> createTenantConfig("registered-client-tenant", client));
        }
        return null;
    }

    // Convert metadata of registered clients to OidcTenantConfig
    private OidcTenantConfig createTenantConfig(String tenantId, RegisteredClient client) {
        ClientMetadata metadata = client.getMetadata();

        String redirectPath = URI.create(metadata.getRedirectUris().get(0)).getPath();
        OidcTenantConfig oidcConfig = OidcTenantConfig
                .authServerUrl(authServerUrl)
                .tenantId(tenantId)
                .applicationType(ApplicationType.WEB_APP)
                .clientName(metadata.getClientName())
                .clientId(metadata.getClientId())
                .credentials(metadata.getClientSecret())
                .authentication().redirectPath(redirectPath).end()
                .build();
        return oidcConfig;
    }
}
```

<a id="register-clients-on-demand"></a>
### Register clients on demand

You can register new clients on demand. You can add new clients to the existing, already configured `OidcClientConfiguration` or to a newly created `OidcClientConfiguration`.

Start from configuring one or more OIDC client registrations:

```properties
quarkus.oidc-client-registration.auth-server-url=${quarkus.oidc.auth-server-url}
```

The above configuration is sufficient for registering new clients using this configuration. For example:

```java
package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcClientRegistration clientReg;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;


    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        if (routingContext.request().path().endsWith("/protected/oidc-client-reg-existing-config")) {
            // New client registration done dynamically at the request time using the configured client registration
            ClientMetadata metadata = createMetadata("http://localhost:8081/protected/dynamic-tenant",
                    "Dynamic Tenant Client");

            return clientReg.registerClient(metadata).onItem().transform(r ->
                  createTenantConfig("registered-client-dynamically", r));
        }
        return null;
    }

    // Create metadata of registered clients to OidcTenantConfig
    private OidcTenantConfig createTenantConfig(String tenantId, ClientMetadata metadata) {
        String redirectPath = URI.create(metadata.getRedirectUris().get(0)).getPath();
        OidcTenantConfig oidcConfig = OidcTenantConfig
                .authServerUrl(authServerUrl)
                .tenantId(tenantId)
                .applicationType(ApplicationType.WEB_APP)
                .clientName(metadata.getClientName())
                .clientId(metadata.getClientId())
                .credentials(metadata.getClientSecret())
                .authentication().redirectPath(redirectPath).end()
                .build();
        return oidcConfig;
    }

    protected static ClientMetadata createMetadata(String redirectUri, String clientName) {
        return ClientMetadata.builder()
                .setRedirectUri(redirectUri)
                .setClientName(clientName)
                .build();
    }
}
```

Alternatively, you can use `OidcClientRegistrations` to prepare a new `OidcClientRegistration` and use `OidcClientRegistration` to register a client. For example:

```java
package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcClientRegistrations clientRegs;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        if (routingContext.request().path().endsWith("/protected/new-oidc-client-reg")) {
            // New client registration done dynamically at the request time

            OidcClientRegistrationConfig clientRegConfig = OidcClientRegistrationConfig
                    .authServerUrl(authServerUrl)
                    .metadata("Dynamic Client", "http://localhost:8081/protected/new-oidc-client-reg")
                    .build();

            return clientRegs.newClientRegistration(clientRegConfig)
                    .onItem().transform(reg ->
                    createTenantConfig("registered-client-dynamically", reg.registeredClient());
        }

        return null;
    }

    // Create metadata of registered clients to OidcTenantConfig
    private OidcTenantConfig createTenantConfig(String tenantId, ClientMetadata metadata) {
        String redirectPath = URI.create(metadata.getRedirectUris().get(0)).getPath();
        OidcTenantConfig oidcConfig = OidcTenantConfig
                .authServerUrl(authServerUrl)
                .tenantId(tenantId)
                .applicationType(ApplicationType.WEB_APP)
                .clientName(metadata.getClientName())
                .clientId(metadata.getClientId())
                .credentials(metadata.getClientSecret())
                .authentication().redirectPath(redirectPath).end()
                .build();
        return oidcConfig;
    }

    protected static ClientMetadata createMetadata(String redirectUri, String clientName) {
        return ClientMetadata.builder()
                .setRedirectUri(redirectUri)
                .setClientName(clientName)
                .build();
    }
}
```

<a id="managing-registered-clients"></a>
### Managing registered clients

`io.quarkus.oidc.client.registration.RegisteredClient` represents a registered client and can be used to read and update its metadata. It can also be used to delete this client.

For example:

```java
package io.quarkus.it.keycloak;


import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcClientRegistration clientReg;

    RegisteredClient registeredClient;

    void onStartup(@Observes StartupEvent event) {

        // Default OIDC client registration, client has already been registered at startup, `await()` will return immediately.
        registeredClient = clientReg.registeredClient().await().indefinitely();

        // Read the latest client metadata
        registeredClient = registeredClient.read().await().indefinitely();
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {

        if (routingContext.request().path().endsWith("/protected")) {
            // Use the registered client created from the default OIDC client registration

            return createTenantConfig("registered-client", registeredClient));
        }
        return null;
    }

    // Convert metadata of registered clients to OidcTenantConfig
    private OidcTenantConfig createTenantConfig(String tenantId, RegisteredClient client) {
        ClientMetadata metadata = client.getMetadata();

        String redirectPath = URI.create(metadata.getRedirectUris().get(0)).getPath();
        OidcTenantConfig oidcConfig = OidcTenantConfig
                .authServerUrl(authServerUrl)
                .tenantId(tenantId)
                .applicationType(ApplicationType.WEB_APP)
                .clientName(metadata.getClientName())
                .clientId(metadata.getClientId())
                .credentials(metadata.getClientSecret())
                .authentication().redirectPath(redirectPath).end()
                .build();
        return oidcConfig;
    }
}
```

<a id="avoiding-duplicate-registrations"></a>
### Avoiding duplicate registrations

When you register clients in startup, as described in the [Register clients on start-up](#register-clients-on-startup) section, you will most likely want to avoid creating duplicate registrations after a restart.

In this case, you should configure OIDC client registration to perform the registration at the request time, as opposed to at the startup time:

```properties
quarkus.oidc-client-registration.register-early=false
```

The next thing you should do is to persist the already registered client's registration URI and registration token at the shutdown time, you can get them from the `io.quarkus.oidc.client.registration.RegisteredClient` instance.

Finally, at the startup time, you should restore the already registered client instead of registering it again.

For example:

```java
package io.quarkus.it.keycloak;


import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcClientRegistration clientReg;

    RegisteredClient registeredClient;

    void onStartup(@Observes StartupEvent event) {

        String registrationUri = readRegistrationUriFromDatabase("Registered Client");
        String registrationToken = readRegistrationTokenFromDatabase("Registered Client");

        if (registrationUri != null && registrationToken != null) {
            // Read an already registered client
            registeredClient = clientReg.readClient(registrationUri, registrationToken).await().indefinitely();
        } else {
            // Register a new client
            registeredClient = clientReg.registeredClient().await().indefinitely();
        }

    }

    void onShutdown(@Observes ShutdownEvent event) {

        saveRegistrationUriToDatabase("Registered Client", registeredClient.registrationUri());
        saveRegistrationTokenToDatabase("Registered Client", registeredClient.registrationToken());

    }

    String readRegistrationUriFromDatabase(String clientName) {
        // implementation is not shown for brevity
    }
    String readRegistrationTokenFromDatabase(String clientName) {
        // implementation is not shown for brevity
    }
    void saveRegistrationUriToDatabase(String clientName, String registrationUri) {
        // implementation is not shown for brevity
    }
    void saveRegistrationTokenToDatabase(String clientName, String registrationToken) {
        // implementation is not shown for brevity
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {

        if (routingContext.request().path().endsWith("/protected")) {
            // Use the registered client created from the default OIDC client registration

            return createTenantConfig("registered-client", registeredClient));
        }
        return null;
    }

    // Convert metadata of registered clients to OidcTenantConfig
    private OidcTenantConfig createTenantConfig(String tenantId, RegisteredClient client) {
        ClientMetadata metadata = client.getMetadata();

        String redirectPath = URI.create(metadata.getRedirectUris().get(0)).getPath();
        OidcTenantConfig oidcConfig = OidcTenantConfig
                .authServerUrl(authServerUrl)
                .tenantId(tenantId)
                .applicationType(ApplicationType.WEB_APP)
                .clientName(metadata.getClientName())
                .clientId(metadata.getClientId())
                .credentials(metadata.getClientSecret())
                .authentication().redirectPath(redirectPath).end()
                .build();
        return oidcConfig;
    }
}
```

If you register clients dynamically, on demand, as described in the [Register clients on demand](#register-clients-on-demand) section, the problem of the duplicate client registration should not arise. You can persist the already registered client's registration URI and registration token if necessary though and check them too to avoid any duplicate reservation risk.

<a id="oidc-client-registration-oidc-request-filters"></a>
## OIDC request filters

You can filter OIDC client registration and registered client requests registering one or more `OidcRequestFilter` implementations, which can update or add new request headers, as well as customize a request body.

For example, a filter can analyze the request body and add its digest as a new header value:

You can have a single filter intercepting all the OIDC registration and registered client requests, or use an `@OidcEndpoint` annotation to apply this filter to either OIDC registration or registered client endpoint responses only. For example:

```java
package io.quarkus.it.keycloak;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.CLIENT_REGISTRATION) <1>
public class ClientRegistrationRequestFilter implements OidcRequestFilter {
    private static final Logger LOG = Logger.getLogger(ClientRegistrationRequestFilter.class);

    @Override
    public Uni<Void> filter(OidcRequestFilterContext rc) {
        JsonObject body = rc.requestBody().toJsonObject();
        if ("Default Client".equals(body.getString("client_name"))) { <2>
            LOG.debug("'Default Client' registration request");
        }
        return Uni.createFrom().voidItem();
    }

}
```

1. Restrict this filter to requests targeting the OIDC client registration endpoint only.
2. Check the 'client_name' property in the request metadata JSON.

`OidcRequestContextProperties` can be used to access request properties. Currently, you can use a `client_id` key to access the client tenant id and a `grant_type` key to access the grant type which the OIDC client uses to acquire tokens.

`OidcRequestFilter` can customize a request body by preparing an instance of `io.vertx.mutiny.core.buffer.Buffer` and setting it on a request context, for example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.CLIENT_REGISTRATION)
public class ClientRegistrationReRequestFilter implements OidcRequestFilter {

    @Override
    public Uni<Void> filter(OidcRequestFilterContext rc) {
        // Update the client name
        JsonObject body = rc.requestBody().toJsonObject();
        if ("Dynamic Tenant Client".equals(body.getString("client_name"))) {
            body.put("client_name", "Registered Dynamic Tenant Client");
            rc.requestBody(Buffer.buffer(body.toString()));
        }
        return Uni.createFrom().voidItem();
    }
}
```

<a id="oidc-client-registration-oidc-response-filters"></a>
## OIDC response filters

You can filter responses to OIDC client registration and registered client requests by registering one or more `OidcResponseFilter` implementations, which can check the response status, headers and body in order to log them or perform other actions.

You can have a single filter intercepting responses to all the OIDC registration and registered client requests, or use an `@OidcEndpoint` annotation to apply this filter to responses from either OIDC registration or registered client endpoint only. For example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.CLIENT_REGISTRATION) <1>
public class ClientRegistrationResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(ClientRegistrationResponseFilter.class);

    @Override
    public Uni<Void> filter(OidcResponseFilterContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type"); <2>
        JsonObject body = rc.responseBody().toJsonObject();
        if (contentType.startsWith("application/json")
                && "Default Client".equals(body.getString("client_name"))) { <3>
            LOG.debug("'Default Client' has been registered");
        }
        return Uni.createFrom().voidItem();
    }

}
```

1. Restrict this filter to requests targeting the OIDC client registration endpoint only.
2. Check the response `Content-Type` header.
3. Check the 'client_name' property in the response metadata JSON.

or

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.REGISTERED_CLIENT) <1>
public class RegisteredClientResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(RegisteredClientResponseFilter.class);

    @Override
    public Uni<Void> filter(OidcResponseFilterContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type"); <2>
        if (contentType.startsWith("application/json")
                && "Default Client Updated".equals(rc.responseBody().toJsonObject().getString("client_name"))) { <3>
            LOG.debug("Registered 'Default Client' has had its name updated to 'Default Client Updated'");
        }
        return Uni.createFrom().voidItem();
    }

}
```

1. Restrict this filter to requests targeting the registered OIDC client endpoint only.
2. Check the response `Content-Type` header.
3. Confirm the client name property was updated.

`OidcResponseFilter` can customize a response body by preparing an instance of `io.vertx.mutiny.core.buffer.Buffer` and setting it as a property on a response context, for example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.CLIENT_REGISTRATION)
public class ClientRegistrationResponseFilter implements OidcResponseFilter {

    @Override
    public Uni<Void> filter(OidcResponseFilterContext rc) {
        // Update the client name
        JsonObject body = rc.responseBody().toJsonObject();
        if ("Registered Dynamic Tenant Client".equals(body.getString("client_name"))) {
            body.put("client_name", "Registered Dynamically Tenant Client");
            rc.responseContext(Buffer.buffer(body.toString()));
        }
        return Uni.createFrom().voidItem();
    }
}
```

<a id="configuration-reference"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-enabled"></a> [`quarkus.oidc-client-registration.enabled`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-enabled) config_property_copy_button:+++quarkus.oidc-client-registration.enabled+++[]<br><br>If the OIDC client registration extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-auth-server-url"></a> [`quarkus.oidc-client-registration.auth-server-url`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-auth-server-url) config_property_copy_button:+++quarkus.oidc-client-registration.auth-server-url+++[]<br><br>`quarkus.oidc-client-registration."id".auth-server-url` config_property_copy_button:+++quarkus.oidc-client-registration."id".auth-server-url+++[]<br><br>The base URL of an OpenID Connect (OIDC) server, for example, `https://host:port/auth`. Do not set this property if you use the public key verification (`public-key`) or certificate chain verification only (`certificate-chain`).<br><br>By default, when an OIDC configuration metadata discovery is enabled with the `discovery-enabled()` property, it is retrieved from a well known provider endpoint with its URL calculated by appending a value of the `discovery-path()` path such as `.well-known/openid-configuration` to this URL.<br><br>For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_AUTH_SERVER_URL+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-discovery-enabled"></a> [`quarkus.oidc-client-registration.discovery-enabled`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-discovery-enabled) config_property_copy_button:+++quarkus.oidc-client-registration.discovery-enabled+++[]<br><br>`quarkus.oidc-client-registration."id".discovery-enabled` config_property_copy_button:+++quarkus.oidc-client-registration."id".discovery-enabled+++[]<br><br>Enable discovery of the OIDC endpoints. If not enabled, you must configure the OIDC endpoint URLs individually.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_DISCOVERY_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-discovery-path"></a> [`quarkus.oidc-client-registration.discovery-path`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-discovery-path) config_property_copy_button:+++quarkus.oidc-client-registration.discovery-path+++[]<br><br>`quarkus.oidc-client-registration."id".discovery-path` config_property_copy_button:+++quarkus.oidc-client-registration."id".discovery-path+++[]<br><br>The relative path of the OIDC discovery endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_DISCOVERY_PATH+++[] | string | `.well-known/openid-configuration` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-registration-path"></a> [`quarkus.oidc-client-registration.registration-path`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-registration-path) config_property_copy_button:+++quarkus.oidc-client-registration.registration-path+++[]<br><br>`quarkus.oidc-client-registration."id".registration-path` config_property_copy_button:+++quarkus.oidc-client-registration."id".registration-path+++[]<br><br>The relative path or absolute URL of the OIDC dynamic client registration endpoint. Set if `discovery-enabled` is `false` or a discovered token endpoint path must be customized.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_REGISTRATION_PATH+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-connection-delay"></a> [`quarkus.oidc-client-registration.connection-delay`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-connection-delay) config_property_copy_button:+++quarkus.oidc-client-registration.connection-delay+++[]<br><br>`quarkus.oidc-client-registration."id".connection-delay` config_property_copy_button:+++quarkus.oidc-client-registration."id".connection-delay+++[]<br><br>The duration to attempt the initial connection to an OIDC server. For example, setting the duration to `20S` allows 10 retries, each 2 seconds apart. This property is only effective when the initial OIDC connection is created. For dropped connections, use the `connection-retry-count` property instead.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_CONNECTION_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client-registration_quarkus-oidc-client-registration) |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-connection-retry-count"></a> [`quarkus.oidc-client-registration.connection-retry-count`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-connection-retry-count) config_property_copy_button:+++quarkus.oidc-client-registration.connection-retry-count+++[]<br><br>`quarkus.oidc-client-registration."id".connection-retry-count` config_property_copy_button:+++quarkus.oidc-client-registration."id".connection-retry-count+++[]<br><br>The number of times to retry re-establishing an existing OIDC connection if it is temporarily lost. Different from `connection-delay`, which applies only to initial connection attempts. For instance, if a request to the OIDC token endpoint fails due to a connection issue, it will be retried as per this setting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_CONNECTION_RETRY_COUNT+++[] | int | `3` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-connection-timeout"></a> [`quarkus.oidc-client-registration.connection-timeout`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-connection-timeout) config_property_copy_button:+++quarkus.oidc-client-registration.connection-timeout+++[]<br><br>`quarkus.oidc-client-registration."id".connection-timeout` config_property_copy_button:+++quarkus.oidc-client-registration."id".connection-timeout+++[]<br><br>The number of seconds after which the current OIDC connection request times out.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client-registration_quarkus-oidc-client-registration) | `10S` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-use-blocking-dns-lookup"></a> [`quarkus.oidc-client-registration.use-blocking-dns-lookup`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-use-blocking-dns-lookup) config_property_copy_button:+++quarkus.oidc-client-registration.use-blocking-dns-lookup+++[]<br><br>`quarkus.oidc-client-registration."id".use-blocking-dns-lookup` config_property_copy_button:+++quarkus.oidc-client-registration."id".use-blocking-dns-lookup+++[]<br><br>Whether DNS lookup should be performed on the worker thread. Use this option when you can see logged warnings about blocked Vert.x event loop by HTTP requests to OIDC server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_USE_BLOCKING_DNS_LOOKUP+++[] | boolean | `false` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-max-pool-size"></a> [`quarkus.oidc-client-registration.max-pool-size`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-max-pool-size) config_property_copy_button:+++quarkus.oidc-client-registration.max-pool-size+++[]<br><br>`quarkus.oidc-client-registration."id".max-pool-size` config_property_copy_button:+++quarkus.oidc-client-registration."id".max-pool-size+++[]<br><br>The maximum size of the connection pool used by the WebClient.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_MAX_POOL_SIZE+++[] | int |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-follow-redirects"></a> [`quarkus.oidc-client-registration.follow-redirects`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-follow-redirects) config_property_copy_button:+++quarkus.oidc-client-registration.follow-redirects+++[]<br><br>`quarkus.oidc-client-registration."id".follow-redirects` config_property_copy_button:+++quarkus.oidc-client-registration."id".follow-redirects+++[]<br><br>Follow redirects automatically when WebClient gets HTTP 302. When this property is disabled only a single redirect to exactly the same original URI is allowed but only if one or more cookies were set during the redirect request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_FOLLOW_REDIRECTS+++[] | boolean | `true` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-id"></a> [`quarkus.oidc-client-registration.id`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-id) config_property_copy_button:+++quarkus.oidc-client-registration.id+++[]<br><br>`quarkus.oidc-client-registration."id".id` config_property_copy_button:+++quarkus.oidc-client-registration."id".id+++[]<br><br>OIDC Client Registration id<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_ID+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-registration-enabled"></a> [`quarkus.oidc-client-registration.registration-enabled`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-registration-enabled) config_property_copy_button:+++quarkus.oidc-client-registration.registration-enabled+++[]<br><br>`quarkus.oidc-client-registration."id".registration-enabled` config_property_copy_button:+++quarkus.oidc-client-registration."id".registration-enabled+++[]<br><br>If this client registration configuration is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_REGISTRATION_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-register-early"></a> [`quarkus.oidc-client-registration.register-early`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-register-early) config_property_copy_button:+++quarkus.oidc-client-registration.register-early+++[]<br><br>`quarkus.oidc-client-registration."id".register-early` config_property_copy_button:+++quarkus.oidc-client-registration."id".register-early+++[]<br><br>If the client configured with `metadata` must be registered at startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_REGISTER_EARLY+++[] | boolean | `true` |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-initial-token"></a> [`quarkus.oidc-client-registration.initial-token`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-initial-token) config_property_copy_button:+++quarkus.oidc-client-registration.initial-token+++[]<br><br>`quarkus.oidc-client-registration."id".initial-token` config_property_copy_button:+++quarkus.oidc-client-registration."id".initial-token+++[]<br><br>Initial access token<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_INITIAL_TOKEN+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-client-name"></a> [`quarkus.oidc-client-registration.metadata.client-name`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-client-name) config_property_copy_button:+++quarkus.oidc-client-registration.metadata.client-name+++[]<br><br>`quarkus.oidc-client-registration."id".metadata.client-name` config_property_copy_button:+++quarkus.oidc-client-registration."id".metadata.client-name+++[]<br><br>Client name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_METADATA_CLIENT_NAME+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-redirect-uri"></a> [`quarkus.oidc-client-registration.metadata.redirect-uri`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-redirect-uri) config_property_copy_button:+++quarkus.oidc-client-registration.metadata.redirect-uri+++[]<br><br>`quarkus.oidc-client-registration."id".metadata.redirect-uri` config_property_copy_button:+++quarkus.oidc-client-registration."id".metadata.redirect-uri+++[]<br><br>Redirect URI<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_METADATA_REDIRECT_URI+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-post-logout-uri"></a> [`quarkus.oidc-client-registration.metadata.post-logout-uri`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-post-logout-uri) config_property_copy_button:+++quarkus.oidc-client-registration.metadata.post-logout-uri+++[]<br><br>`quarkus.oidc-client-registration."id".metadata.post-logout-uri` config_property_copy_button:+++quarkus.oidc-client-registration."id".metadata.post-logout-uri+++[]<br><br>Post Logout URI<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_METADATA_POST_LOGOUT_URI+++[] | string |  |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-extra-props-extra-props"></a> [`quarkus.oidc-client-registration.metadata.extra-props."extra-props"`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-metadata-extra-props-extra-props) config_property_copy_button:+++quarkus.oidc-client-registration.metadata.extra-props."extra-props"+++[]<br><br>`quarkus.oidc-client-registration."id".metadata.extra-props."extra-props"` config_property_copy_button:+++quarkus.oidc-client-registration."id".metadata.extra-props."extra-props"+++[]<br><br>Additional metadata properties<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_METADATA_EXTRA_PROPS__EXTRA_PROPS_+++[] | Map\<String,String> |  |
| <a id="quarkus-oidc-client-registration_section_quarkus-oidc-client-registration-proxy"></a> [HTTP proxy configuration](#quarkus-oidc-client-registration_section_quarkus-oidc-client-registration-proxy) | Type | Default |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-proxy-proxy-configuration-name"></a> [`quarkus.oidc-client-registration.proxy.proxy-configuration-name`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-proxy-proxy-configuration-name) config_property_copy_button:+++quarkus.oidc-client-registration.proxy.proxy-configuration-name+++[]<br><br>`quarkus.oidc-client-registration."id".proxy.proxy-configuration-name` config_property_copy_button:+++quarkus.oidc-client-registration."id".proxy.proxy-configuration-name+++[]<br><br>The name of the proxy configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.proxy.<name>.*`. Please note that the 'non-proxy-hosts' option is currently not supported. If a name is configured, but no proxy configuration is found with that name then an error will be thrown.<br><br>The default proxy configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_PROXY_PROXY_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-oidc-client-registration_section_quarkus-oidc-client-registration-tls"></a> [TLS configuration](#quarkus-oidc-client-registration_section_quarkus-oidc-client-registration-tls) | Type | Default |
| <a id="quarkus-oidc-client-registration_quarkus-oidc-client-registration-tls-tls-configuration-name"></a> [`quarkus.oidc-client-registration.tls.tls-configuration-name`](#quarkus-oidc-client-registration_quarkus-oidc-client-registration-tls-tls-configuration-name) config_property_copy_button:+++quarkus.oidc-client-registration.tls.tls-configuration-name+++[]<br><br>`quarkus.oidc-client-registration."id".tls.tls-configuration-name` config_property_copy_button:+++quarkus.oidc-client-registration."id".tls.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_TLS_TLS_CONFIGURATION_NAME+++[] | string |  |

<a id="duration-note-anchor-quarkus-oidc-client-registration_quarkus-oidc-client-registration"></a>

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

## References

- [OIDC client registration](https://openid.net/specs/openid-connect-registration-1_0.html)
- [OAuth2 Dynamic Client Registration Management Protocol](https://datatracker.ietf.org/doc/html/rfc7592)
- [Keycloak Dynamic Client Registration Service](https://www.keycloak.org/securing-apps/client-registration)
- [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md)
- [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md)
- [Quarkus Security overview](/guides/security-overview/index.md)
