# OpenID Connect (OIDC) and OAuth2 client and filters

You can use Quarkus extensions for OpenID Connect and OAuth 2.0 access token management, focusing on acquiring, refreshing, and propagating tokens.

This includes the following:

- Using `quarkus-oidc-client`, `quarkus-rest-client-oidc-filter` and `quarkus-resteasy-client-oidc-filter` extensions to acquire and refresh access tokens from OpenID Connect and OAuth 2.0 compliant Authorization Servers such as [Keycloak](https://www.keycloak.org).

- Using `quarkus-rest-client-oidc-token-propagation` and `quarkus-resteasy-client-oidc-token-propagation` extensions to propagate the current `Bearer` or `Authorization Code Flow` access tokens.

The access tokens managed by these extensions can be used as HTTP Authorization Bearer tokens to access the remote services.

Also see [OpenID Connect client and token propagation quickstart](/guides/security-openid-connect-client/index.md).

## OidcClient

Add the following dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc-client</artifactId>
</dependency>
```

The `quarkus-oidc-client` extension provides a reactive `io.quarkus.oidc.client.OidcClient`, which can be used to acquire and refresh tokens by using SmallRye Mutiny `Uni` and `Vert.x WebClient`.

`OidcClient` is initialized at build time with the IDP token endpoint URL, which can be auto-discovered or manually configured. `OidcClient` uses this endpoint to acquire access tokens with token grants such as `client_credentials` or `password`, and to refresh tokens with a `refresh_token` grant.

### Token endpoint configuration

By default, the token endpoint address is discovered by adding a `/.well-known/openid-configuration` path to the configured `quarkus.oidc-client.auth-server-url`.

For example, given this Keycloak URL:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus
```

`OidcClient` will discover that the token endpoint URL is `http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/tokens`.

If the OIDC provider requires authentication to access the discovery endpoint, you can configure `OidcClient` to send credentials with the discovery request, as described in [Authenticated access to the discovery endpoint](#oidc-client-authenticated-discovery-endpoint).

If the discovery endpoint is unavailable or you want to avoid an extra round-trip to it, you can disable discovery and configure the token endpoint address with a relative path.

For example:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus
quarkus.oidc-client.discovery-enabled=false
# Token endpoint: http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/tokens
quarkus.oidc-client.token-path=/protocol/openid-connect/tokens
```

A more compact way to configure the token endpoint URL without the discovery is to set `quarkus.oidc-client.token-path` to an absolute URL:

```properties
quarkus.oidc-client.token-path=http://localhost:8180/auth/realms/quarkus/protocol/openid-connect/tokens
```

Setting `quarkus.oidc-client.auth-server-url` and `quarkus.oidc-client.discovery-enabled` is not required in this case.

### Supported token grants

The main token grants that `OidcClient` can use to acquire the tokens are the `client_credentials` (default) and `password` grants.

#### Client credentials grant

Here is how `OidcClient` can be configured to use the `client_credentials` grant:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
```

The `client_credentials` grant allows setting extra parameters for the token request by using `quarkus.oidc-client.grant-options.client.<param-name>=<value>`.

Here is how to set the intended token recipient by using the `audience` parameter:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
# 'client' is a shortcut for `client_credentials`
quarkus.oidc-client.grant.type=client
quarkus.oidc-client.grant-options.client.audience=https://example.com/api
```

#### Password grant

Here is how `OidcClient` can be configured to use the `password` grant:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
quarkus.oidc-client.grant.type=password
quarkus.oidc-client.grant-options.password.username=alice
quarkus.oidc-client.grant-options.password.password=alice
```

It can be further customized with the `quarkus.oidc-client.grant-options.password` configuration prefix, similar to the client credentials grant.

#### Other grants

`OidcClient` can also help acquire tokens with grants that require additional input parameters that cannot be captured in the configuration.

These grants are `refresh_token` (with the external refresh token), `authorization_code`, and two grants which can be used to exchange the current access token, namely:

- `urn:ietf:params:oauth:grant-type:token-exchange`
- `urn:ietf:params:oauth:grant-type:jwt-bearer`.

If you need to acquire an access token and have posted an existing refresh token to the current Quarkus endpoint, you must use the `refresh_token` grant. This grant uses an out-of-band refresh token to obtain a new token set.

In this case, configure `OidcClient` as follows:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
quarkus.oidc-client.grant.type=refresh
```

Then you can use the `OidcClient.refreshTokens` method with a provided refresh token to get the access token.

Using the `urn:ietf:params:oauth:grant-type:token-exchange` or `urn:ietf:params:oauth:grant-type:jwt-bearer` grants might be required if you are building a complex microservices application and want to avoid the same `Bearer` token being propagated to and used by more than one service.

For more information, see [Token Propagation for Quarkus REST](#token-propagation-rest) and [Token Propagation for RESTEasy Classic](#token-propagation-resteasy).

Using `OidcClient` to support the `authorization code` grant might be required if you cannot use the [Quarkus OIDC extension](/guides/security-oidc-code-flow-authentication/index.md) to support Authorization Code Flow.

If there is a very good reason for you to implement Authorization Code Flow, then you can configure `OidcClient` as follows:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
quarkus.oidc-client.grant.type=code
```

Then, you can use the `OidcClient.accessTokens` method to accept a Map of extra properties and pass the current `code` and `redirect_uri` parameters to exchange the authorization code for the tokens.

`OidcClient` also supports the `urn:openid:params:grant-type:ciba` grant:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
quarkus.oidc-client.grant.type=ciba
```

Then, you can use the `OidcClient.accessTokens` method to accept a Map of extra properties and pass the `auth_req_id` parameter to exchange the token authorization code.

#### Grant scopes

You might need to request that a specific set of scopes be associated with an issued access token. Use a dedicated `quarkus.oidc-client.scopes` list property, for example: `quarkus.oidc-client.scopes=email,phone`

<a id="use-oidc-client-directly"></a>
### Use OidcClient directly

You can use `OidcClient` directly to acquire access tokens and set them in an HTTP `Authorization` header as a `Bearer` scheme value.

For example, assume that the Quarkus endpoint must access a microservice that returns a user name.

1. Create a REST client:

   ```java
   package org.acme.security.openid.connect.client;

   import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

   import io.smallrye.mutiny.Uni;
   import jakarta.ws.rs.GET;
   import jakarta.ws.rs.HeaderParam;
   import jakarta.ws.rs.Path;
   import jakarta.ws.rs.Produces;

   @RegisterRestClient
   @Path("/")
   public interface RestClientWithTokenHeaderParam {

       @GET
       @Produces("text/plain")
       @Path("userName")
       Uni<String> getUserName(@HeaderParam("Authorization") String authorization);
   }
   ```


1. Use `OidcClient` to acquire the tokens and propagate them:

   ```java
   package org.acme.security.openid.connect.client;

   import org.eclipse.microprofile.rest.client.inject.RestClient;

   import io.quarkus.oidc.client.runtime.TokensHelper;
   import io.quarkus.oidc.client.OidcClient;

   import io.smallrye.mutiny.Uni;
   import jakarta.inject.Inject;
   import jakarta.ws.rs.GET;
   import jakarta.ws.rs.Path;
   import jakarta.ws.rs.Produces;

   @Path("/service")
   public class OidcClientResource {

       @Inject
       OidcClient client;
       TokensHelper tokenHelper = new TokensHelper(); <1>

       @Inject
       @RestClient
       RestClientWithTokenHeaderParam restClient;

       @GET
       @Path("user-name")
       @Produces("text/plain")
       public Uni<String> getUserName() {
       	return tokenHelper.getTokens(client).onItem()
           		.transformToUni(tokens -> restClient.getUserName("Bearer " + tokens.getAccessToken()));
       }
   }
   ```

   1. `io.quarkus.oidc.client.runtime.TokensHelper` manages the access token acquisition and refresh.


### Inject tokens

You can inject `Tokens` that use `OidcClient` internally. `Tokens` can be used to acquire the access tokens and refresh them if necessary:

```java
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.oidc.client.Tokens;

@Path("/service")
public class OidcClientResource {

    @Inject Tokens tokens;

    @GET
    public String getResponse() {
        //  Get the access token, which might have been refreshed.
        String accessToken = tokens.getAccessToken();
        // Use the access token to configure MP RestClient Authorization header/etc
    }
}
```

<a id="use-oidc-clients"></a>
### Use OidcClients

`io.quarkus.oidc.client.OidcClients` is a container of `OidcClient` instances.

It includes a default `OidcClient` and named clients that can be configured like this:

```properties
quarkus.oidc-client.client-enabled=false

quarkus.oidc-client.jwt-secret.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.jwt-secret.client-id=quarkus-app
quarkus.oidc-client.jwt-secret.credentials.jwt.secret=AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow
```

In this case, the default client is disabled with a `client-enabled=false` property. The `jwt-secret` client can be accessed like this:

```java
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import io.smallrye.mutiny.Uni;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.TokensHelper;

@Path("/clients")
public class OidcClientResource {

    @Inject
    OidcClients clients;
    TokensHelper tokenHelper = new TokensHelper();

    @Inject
    @RestClient
    RestClientWithTokenHeaderParam restClient; <1>

    @GET
    @Path("user-name")
    @Produces("text/plain")
    public Uni<String> getUserName() {
    	OidcClient client = clients.getClient("jwt-secret");
    	return tokenHelper.getTokens(client).onItem()
        		.transformToUni(tokens -> restClient.getUserName("Bearer " + tokens.getAccessToken()));
    }
}
```

1. See the `RestClientWithTokenHeaderParam` declaration in the [Use OidcClient directly](#use-oidc-client-directly) section.

> [!NOTE]
> If you also use [OIDC multitenancy](/guides/security-openid-connect-multitenancy/index.md), and each OIDC tenant has its own associated `OidcClient`, you can use a Vert.x `RoutingContext` `tenant-id` attribute.
>
> For example:
>
> ```java
> import jakarta.inject.Inject;
> import jakarta.ws.rs.GET;
> import jakarta.ws.rs.Path;
>
> import io.quarkus.oidc.client.OidcClient;
> import io.quarkus.oidc.client.OidcClients;
> import io.vertx.ext.web.RoutingContext;
>
> @Path("/clients")
> public class OidcClientResource {
>
>     @Inject
>     OidcClients clients;
>     @Inject
>     RoutingContext context;
>
>     @GET
>     public String getResponse() {
>         String tenantId = context.get("tenant-id");
>         // named OIDC tenant and client configurations use the same key:
>         OidcClient client = clients.getClient(tenantId);
>         //Use this client to get the token
>     }
> }
> ```

You can also create a new `OidcClient` programmatically. For example, assume that you must create it at startup time:

```java
package org.acme.security.openid.connect.client;

import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant.Type;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class OidcClientCreator {

    @Inject
    OidcClients oidcClients;
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcProviderAddress;

    private volatile OidcClient oidcClient;

    public void startup(@Observes StartupEvent event) {
    	createOidcClient().subscribe().with(client -> {oidcClient = client;});
    }

    public OidcClient getOidcClient() {
        return oidcClient;
    }

    private Uni<OidcClient> createOidcClient() {
        OidcClientConfig cfg = OidcClientConfig
            .authServerUrl(oidcProviderAddress)
            .id("myclient")
            .clientId("backend-service")
            .credentials("secret")
            .grant(Type.PASSWORD)
            .grantOptions("password", Map.of("username", "alice", "password", "alice"))
            .build();
        return oidcClients.newClient(cfg);
    }
}
```

Now, you can use this client like this:

```java
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import io.smallrye.mutiny.Uni;
import io.quarkus.oidc.client.runtime.TokensHelper;

@Path("/clients")
public class OidcClientResource {

    @Inject
    OidcClientCreator oidcClientCreator;
    TokensHelper tokenHelper = new TokensHelper();

    @Inject
    @RestClient
    RestClientWithTokenHeaderParam restClient; <1>

    @GET
    @Path("user-name")
    @Produces("text/plain")
    public Uni<String> getUserName() {
    	return tokenHelper.getTokens(oidcClientCreator.getOidcClient()).onItem()
        		.transformToUni(tokens -> restClient.getUserName("Bearer " + tokens.getAccessToken()));
    }
}
```

1. See the `RestClientWithTokenHeaderParam` declaration in the [Use OidcClient directly](#use-oidc-client-directly) section.

<a id="named-oidc-clients"></a>
### Inject named OidcClient and tokens

If multiple `OidcClient` objects are configured, you can specify the `OidcClient` injection target with the `@NamedOidcClient` qualifier instead of working with `OidcClients`:

```java
package org.acme.security.openid.connect.client;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import io.smallrye.mutiny.Uni;
import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.runtime.TokensHelper;

@Path("/clients")
public class OidcClientResource {

    @Inject
    @NamedOidcClient("jwt-secret")
    OidcClient client;

    TokensHelper tokenHelper = new TokensHelper();

    @Inject
    @RestClient
    RestClientWithTokenHeaderParam restClient; <1>

    @GET
    @Path("user-name")
    @Produces("text/plain")
    public Uni<String> getUserName() {
    	return tokenHelper.getTokens(client).onItem()
        		.transformToUni(tokens -> restClient.getUserName("Bearer " + tokens.getAccessToken()));
    }
}
```

1. See the `RestClientWithTokenHeaderParam` declaration in the [Use OidcClient directly](#use-oidc-client-directly) section.

The same qualifier can be used to specify the `OidcClient` used for a `Tokens` injection:

```java
import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.Tokens;

@Provider
@Priority(Priorities.AUTHENTICATION)
@RequestScoped
public class OidcClientRequestCustomFilter implements ClientRequestFilter {

    @Inject
    @NamedOidcClient("jwt-secret")
    Tokens tokens;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
    }
}
```

Additionally, if you want to do a programmatic lookup of an `OidcClient`, use the `NamedOidcClient.Literal` qualifier to select it instead of relying on a static `@NamedOidcClient` injection point:

```java
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.OidcClient;
import io.smallrye.mutiny.Uni;

@Path("/clients")
public class OidcClientResource {

    @Inject
    @Any
    Instance<OidcClient> clients; // <1>

    @GET
    @Path("{name}/user-name")
    public Uni<String> getUserName(@PathParam("name") String name) {
        OidcClient client = clients.select(NamedOidcClient.Literal.of(name)).get();
        return client.getTokens().onItem().transform(tokens -> tokens.getAccessToken());
    }

    @GET
    @Path("{name}/access-token")
    public Uni<String> getAccessToken(@PathParam("name") String name) {
        // Alternatively, look the OidcClient up directly from the CDI container
        OidcClient client = Arc.container().instance(OidcClient.class, NamedOidcClient.Literal.of(name)).get(); // <2>
        return client.getTokens().onItem().transform(tokens -> tokens.getAccessToken());
    }
}
```

1. Inject with `@Any` so that all the named `OidcClient` beans are available for selection by the `NamedOidcClient.Literal` qualifier.
2. `Arc.container()` resolves the bean directly without an injected `Instance`, passing the `NamedOidcClient.Literal` qualifier to select the wanted `OidcClient`.

> [!NOTE]
> Prefer the static `@NamedOidcClient` injection point whenever the client name is known in advance.

<a id="rest-client-oidc-filter"></a>
### Use OidcClient in RestClient Reactive ClientFilter

Add the following Maven dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-oidc-filter</artifactId>
</dependency>
```

> [!NOTE]
> It will also bring `io.quarkus:quarkus-oidc-client`.

`quarkus-rest-client-oidc-filter` extension provides `io.quarkus.oidc.client.filter.OidcClientRequestReactiveFilter`.

It works similarly to `OidcClientRequestFilter`. See [Use OidcClient in MicroProfile RestClient client filter](#resteasy-client-oidc-filter). It uses `OidcClient` to acquire the access token, refresh it if needed, and set it as an HTTP `Authorization Bearer` scheme value.

The difference is that it works with [Reactive RestClient](/guides/rest-client/index.md) and implements a non-blocking client filter that does not block the current IO thread when acquiring or refreshing the tokens.

`OidcClientRequestReactiveFilter` delays an initial token acquisition until it is executed to avoid blocking an IO thread.

To add an OIDC client access token to requests from all methods, register `OidcClientRequestReactiveFilter` for the REST client interface with `io.quarkus.oidc.client.filter.OidcClientFilter` or `org.eclipse.microprofile.rest.client.annotation.RegisterProvider`:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@OidcClientFilter
@Path("/")
public interface ProtectedResourceService {

    @GET
    Uni<String> getUserName();
}
```

To add the token only to requests from selected methods, apply `@OidcClientFilter` to those methods instead of the REST client interface.

You cannot combine class-level and method-level `@OidcClientFilter` annotations on the same REST client, regardless of the OIDC client name. To use different OIDC clients for different methods, leave the interface unannotated and annotate every method that requires a token, specifying the client name where needed.

For example, a single REST client interface can call both public endpoints and endpoints that require an access token:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@Path("/")
public interface InformationService {

    @OidcClientFilter
    @GET
    Uni<String> getUserName();

    @Path("/public")
    @GET
    Uni<String> getPublicInformation();
}
```

or

```java
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@Path("/")
public interface ProtectedResourceService {

    @GET
    Uni<String> getUserName();
}
```

`OidcClientRequestReactiveFilter` uses a default `OidcClient` by default. A named `OidcClient` can be selected with the `quarkus.rest-client-oidc-filter.client-name` configuration property. You can also select `OidcClient` by setting the `value` attribute of the `@OidcClientFilter` annotation.

The client name set through annotation has higher priority than the `quarkus.rest-client-oidc-filter.client-name` configuration property.

For example, given [this](#use-oidc-clients) `jwt-secret` named OIDC client declaration, you can refer to this client like this:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@OidcClientFilter("jwt-secret")
@Path("/")
public interface ProtectedResourceService {

    @GET
    Uni<String> getUserName();
}
```

If you also want to refresh the token every time the `ProtectedResourceService#getUserName` call results in a 401 Unauthorized error, use the `quarkus.rest-client-oidc-filter.refresh-on-unauthorized` configuration property, as in the following example:

```properties
quarkus.rest-client-oidc-filter.refresh-on-unauthorized=true
```

Alternatively, if you only need to enable this feature for individual endpoints, create a custom filter, as in the following example:

```java
package io.quarkus.it.keycloak;

import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter extends AbstractOidcClientRequestReactiveFilter {

    @Override
    protected boolean refreshOnUnauthorized() {
        return true;
    }
}
```

<a id="resteasy-client-oidc-filter"></a>
### Use OidcClient in RestClient ClientFilter

Add the following Maven dependency:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-client-oidc-filter</artifactId>
</dependency>
```

> [!NOTE]
> It will also bring `io.quarkus:quarkus-oidc-client`.

`quarkus-resteasy-client-oidc-filter` extension provides `io.quarkus.oidc.client.resteasy.filter.OidcClientRequestFilter`, a Jakarta REST `ClientRequestFilter` that uses `OidcClient` to acquire the access token, refresh it if needed, and set it as an HTTP `Authorization Bearer` scheme value.

By default, this filter uses `OidcClient` to acquire the first pair of access and refresh tokens at initialization time. If the access tokens are short-lived and refresh tokens are unavailable, then the token acquisition should be delayed with `quarkus.oidc-client.early-tokens-acquisition=false`.

To add an OIDC client access token to requests from all methods, register `OidcClientRequestFilter` for the REST client interface with `io.quarkus.oidc.client.filter.OidcClientFilter` or `org.eclipse.microprofile.rest.client.annotation.RegisterProvider`:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@OidcClientFilter
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

To add the token only to requests from selected methods, apply `@OidcClientFilter` to those methods instead of the REST client interface.

You cannot combine class-level and method-level `@OidcClientFilter` annotations on the same REST client, regardless of the OIDC client name. To use different OIDC clients for different methods, leave the interface unannotated and annotate every method that requires a token, specifying the client name where needed.

For example, a single REST client interface can call both public endpoints and endpoints that require an access token:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@Path("/")
public interface InformationService {

    @OidcClientFilter
    @GET
    String getUserName();

    @Path("/public")
    @GET
    String getPublicInformation();
}
```

or

```java
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.resteasy.filter.OidcClientRequestFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@RegisterProvider(OidcClientRequestFilter.class)
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

Alternatively, `OidcClientRequestFilter` can be registered automatically with all MP Rest or Jakarta REST clients if the `quarkus.resteasy-client-oidc-filter.register-filter=true` property is set.

`OidcClientRequestFilter` uses a default `OidcClient` by default. A named `OidcClient` can be selected with the `quarkus.resteasy-client-oidc-filter.client-name` configuration property. You can also select `OidcClient` by setting the `value` attribute of the `@OidcClientFilter` annotation.

The client name set through annotation has higher priority than the `quarkus.resteasy-client-oidc-filter.client-name` configuration property.

For example, given [this](#use-oidc-clients) `jwt-secret` named OIDC client declaration, you can refer to this client like this:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.client.filter.OidcClientFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@OidcClientFilter("jwt-secret")
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

If you also want to refresh the token every time the `ProtectedResourceService#getUserName` call results in a 401 Unauthorized error, use the `quarkus.resteasy-client-oidc-filter.refresh-on-unauthorized` configuration property, as in the following example:

```properties
quarkus.resteasy-client-oidc-filter.refresh-on-unauthorized=true
```

Alternatively, if you only need to enable this feature for individual endpoints, create a custom filter, as in the following example:

```java
package io.quarkus.it.keycloak;

import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter extends AbstractOidcClientRequestFilter {

    @Override
    protected boolean refreshOnUnauthorized() {
        return true;
    }
}
```

### Use a custom RestClient ClientFilter

If you prefer, you can use your own custom filter and inject `Tokens`:

```java
import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import io.quarkus.oidc.client.Tokens;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter implements ClientRequestFilter {

    @Inject
    Tokens tokens;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken());
    }
}
```

The `Tokens` producer will acquire and refresh the tokens, and the custom filter will decide how and when to use the token.

You can also inject named `Tokens`, see [Inject named OidcClient and Tokens](#named-oidc-clients)

<a id="refresh-access-tokens"></a>
### Refreshing access tokens

`OidcClientRequestReactiveFilter`, `OidcClientRequestFilter`, and `Tokens` producers refresh the current expired access token if a refresh token is available.

Additionally, you can use the `quarkus.oidc-client.refresh-token-time-skew` property for preemptive access token refresh to avoid sending nearly expired access tokens that might cause HTTP 401 errors.

For example, if this property is set to `3S` and the access token will expire in less than 3 seconds, the token is refreshed automatically.

By default, the OIDC client refreshes the token during the current request when it detects that the token has expired, or is nearly expired if the [refresh token time skew](#quarkus-oidc-client_quarkus-oidc-client-refresh-token-time-skew) is configured. Performance-critical applications might want to avoid waiting for a possible token refresh during incoming requests and configure an asynchronous token refresh instead.

For example:

```properties
quarkus.oidc-client.refresh-interval=1m <1>
```

1. Check every minute whether the current access token is expired and must be refreshed.

If the access token needs to be refreshed but no refresh token is available, an attempt is made to acquire a new token with a configured grant, such as `client_credentials`.

Some OpenID Connect providers do not return a refresh token in a `client_credentials` grant response. For example, starting with Keycloak 12, a refresh token is not returned by default for `client_credentials`. Providers might also restrict how many times a refresh token can be used.

<a id="revoke-access-tokens"></a>
### Revoking access tokens

If your OpenID Connect provider, such as Keycloak, supports a token revocation endpoint, you can use `OidcClient#revokeAccessToken` to revoke the current access token. The revocation endpoint URL is discovered alongside the token request URI or can be configured with `quarkus.oidc-client.revoke-path`.

You might want to revoke the access token if a REST client call that uses this token fails with an HTTP `401` status code, or if the token has been used for a long time and you want to refresh it.

You can do this by requesting a token refresh with a refresh token. However, if the refresh token is unavailable, you can revoke the current access token first and then request a new access token.

<a id="oidc-client-authentication"></a>
### OidcClient authentication

`OidcClient` must authenticate to the OpenID Connect provider for the `client_credentials` and other grant requests to succeed. All the [OIDC Client Authentication](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication) options are supported.

For example:

`client_secret_basic`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=mysecret
```

or

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.client-secret.value=mysecret
```

Or with the secret retrieved from a [CredentialsProvider](/guides/credentials-provider/index.md):

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app

# This key is used to retrieve a secret from the map of credentials returned from CredentialsProvider
quarkus.oidc-client.credentials.client-secret.provider.key=mysecret-key
# This is the keyring provided to the CredentialsProvider when looking up the secret, set only if required by the CredentialsProvider implementation
quarkus.oidc.credentials.client-secret.provider.keyring-name=oidc
# Set it only if more than one CredentialsProvider can be registered
quarkus.oidc-client.credentials.client-secret.provider.name=oidc-credentials-provider
```

`client_secret_post`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.client-secret.value=mysecret
quarkus.oidc-client.credentials.client-secret.method=post
```

`client_secret_jwt`, signature algorithm is `HS256`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.secret=AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow
```

Or with the secret retrieved from a [CredentialsProvider](/guides/credentials-provider/index.md), signature algorithm is `HS256`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app

# This is a key that will be used to retrieve a secret from the map of credentials returned from CredentialsProvider
quarkus.oidc-client.credentials.jwt.secret-provider.key=mysecret-key
# This is the keyring provided to the CredentialsProvider when looking up the secret, set only if required by the CredentialsProvider implementation
quarkus.oidc.credentials.client-secret.provider.keyring-name=oidc
# Set it only if more than one CredentialsProvider can be registered
quarkus.oidc-client.credentials.jwt.secret-provider.name=oidc-credentials-provider
```

`private_key_jwt` with the PEM key inlined in application.properties, where the signature algorithm is `RS256`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.key=Base64-encoded private key representation
```

`private_key_jwt` with the PEM key file, signature algorithm is `RS256`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.key-file=privateKey.pem
```

`private_key_jwt` with the keystore file, signature algorithm is `RS256`:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.key-store-file=keystore.pkcs12
quarkus.oidc-client.credentials.jwt.key-store-password=mypassword
quarkus.oidc-client.credentials.jwt.key-password=mykeypassword

# Private key alias inside the keystore
quarkus.oidc-client.credentials.jwt.key-id=mykeyAlias
```

Using `client_secret_jwt` or `private_key_jwt` authentication methods ensures that no client secret goes over the wire.

#### Additional JWT authentication options

If either `client_secret_jwt` or `private_key_jwt` authentication method is used, you can customize the JWT signature algorithm, key identifier, audience, subject, and issuer.

For example:

```properties
# private_key_jwt client authentication

quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus/
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.key-file=privateKey.pem

# This is a token key identifier 'kid' header - set it if your OpenID Connect provider requires it.
# Note that if the key is represented in a JSON Web Key (JWK) format with a `kid` property, then
# using 'quarkus.oidc-client.credentials.jwt.token-key-id' is unnecessary.
quarkus.oidc-client.credentials.jwt.token-key-id=mykey

# Use the RS512 signature algorithm instead of the default RS256
quarkus.oidc-client.credentials.jwt.signature-algorithm=RS512

# The token endpoint URL is the default audience value; use the base address URL instead:
quarkus.oidc-client.credentials.jwt.audience=${quarkus.oidc-client.auth-server-url}

# custom subject instead of the client ID:
quarkus.oidc-client.credentials.jwt.subject=custom-subject

# custom issuer instead of the client ID:
quarkus.oidc-client.credentials.jwt.issuer=custom-issuer
```

<a id="oidc-client-jwt-bearer"></a>
#### JWT Bearer

[RFC7523](https://www.rfc-editor.org/rfc/rfc7523) explains how JWT Bearer tokens can be used to authenticate clients. For more information, see the [Using JWTs for Client Authentication](https://www.rfc-editor.org/rfc/rfc7523#section-2.2) section.

Enable it as follows:

```properties
quarkus.oidc-client.auth-server-url=${auth-server-url}
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.source=bearer
```

Next, provide the JWT bearer token as a `client_assertion` parameter to the OIDC client.

Quarkus can load the JWT bearer token from the file system. For example, in Kubernetes, a service account token projection can be mounted to a `/var/run/secrets/tokens` path. Then configure the JWT bearer token path as follows:

```properties
quarkus.oidc-client.credentials.jwt.token-path=/var/run/secrets/tokens <1>
```

1. Path to a JWT bearer token. Quarkus loads a new token from the file system and reloads it when the token expires.

Your other option is to use `OidcClient` methods for acquiring or refreshing tokens that accept additional grant parameters, for example, `oidcClient.getTokens(Map.of("client_assertion", "ey..."))`.

If you use the OIDC client filters, you must register a custom filter that provides this assertion.

Here is an example of the Quarkus REST (formerly RESTEasy Reactive) custom filter:

```java
package io.quarkus.it.keycloak;

import java.util.Map;

import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter extends AbstractOidcClientRequestReactiveFilter {

    @Override
    protected Map<String, String> additionalParameters() {
        return Map.of(OidcConstants.CLIENT_ASSERTION, "ey...");
    }
}
```

Here is an example of the RESTEasy Classic custom filter:

```java
package io.quarkus.it.keycloak;

import java.util.Map;

import io.quarkus.oidc.client.filter.runtime.AbstractOidcClientRequestFilter;
import io.quarkus.oidc.common.runtime.OidcConstants;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;

@Priority(Priorities.AUTHENTICATION)
public class OidcClientRequestCustomFilter extends AbstractOidcClientRequestFilter {

    @Override
    protected Map<String, String> additionalParameters() {
        return Map.of(OidcConstants.CLIENT_ASSERTION, "ey...");
    }
}
```

#### SPIFFE JWT-SVID

[SPIFFE](https://spiffe.io) (Secure Production Identity Framework for Everyone) JWT-SVID tokens can be used as client assertions for client authentication. When the JWT source is set to `spiffe`, the `client_assertion_type` parameter is set to `urn:ietf:params:oauth:client-assertion-type:jwt-spiffe` instead of the JWT bearer `urn:ietf:params:oauth:client-assertion-type:jwt-bearer` value.

Enable it as follows:

```properties
quarkus.oidc-client.auth-server-url=${auth-server-url}
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.jwt.source=spiffe-jwt
```

Quarkus can load the SPIFFE JWT-SVID token from the file system. Configure the token path as follows:

```properties
quarkus.oidc-client.credentials.jwt.token-path=/var/run/secrets/tokens <1>
```

1. Path to a SPIFFE JWT-SVID token. Quarkus loads a new token from the file system and reloads it when the token expires. The token's `sub` claim is verified to start with the `spiffe://` scheme.

#### Apple POST JWT

Apple OpenID Connect provider uses the `client_secret_post` method, where the secret is a JWT produced with the `private_key_jwt` authentication method, but with Apple account-specific issuer and subject properties.

`quarkus-oidc-client` supports a non-standard `client_secret_post_jwt` authentication method, which can be configured as follows:

```properties
quarkus.oidc-client.auth-server-url=${apple.url}
quarkus.oidc-client.client-id=${apple.client-id}
quarkus.oidc-client.credentials.client-secret.method=post-jwt

quarkus.oidc-client.credentials.jwt.key-file=ecPrivateKey.pem
quarkus.oidc-client.credentials.jwt.signature-algorithm=ES256
quarkus.oidc-client.credentials.jwt.subject=${apple.subject}
quarkus.oidc-client.credentials.jwt.issuer=${apple.issuer}
```

#### Mutual TLS

Some OpenID Connect providers require that a client be authenticated as part of the mutual TLS (`mTLS`) authentication process.

`quarkus-oidc-client` can be configured as follows to support `mTLS`:

```properties
quarkus.oidc-client.tls.tls-configuration-name=oidc-client

# configure hostname verification if necessary
#quarkus.tls.oidc-client.hostname-verification-algorithm=NONE

# Keystore configuration
quarkus.tls.oidc-client.key-store.p12.path=client-keystore.p12
quarkus.tls.oidc-client.key-store.p12.password=${key-store-password}

# Add more keystore properties if needed:
#quarkus.tls.oidc-client.key-store.p12.alias=keyAlias
#quarkus.tls.oidc-client.key-store.p12.alias-password=keyAliasPassword

# Truststore configuration
quarkus.tls.oidc-client.trust-store.p12.path=client-truststore.p12
quarkus.tls.oidc-client.trust-store.p12.password=${trust-store-password}
# Add more truststore properties if needed:
#quarkus.tls.oidc-client.trust-store.p12.alias=certAlias
```

<a id="oidc-client-authenticated-discovery-endpoint"></a>
#### Authenticated access to the discovery endpoint

The OIDC discovery endpoint is typically public. Some OIDC providers may enforce authenticated access to this endpoint. For example, the Kubernetes API server acts as an OIDC provider for service account tokens and requires a bearer access token to access its discovery endpoint.

To send client credentials to the discovery endpoint, set `quarkus.oidc-client.credentials.for-all-endpoints=true`. Since the discovery endpoint is accessed with an HTTP GET request, only authentication methods that use the HTTP `Authorization` header are effective, such as `client_secret_basic` and [JWT bearer](#oidc-client-jwt-bearer). Alternatively, you can use [OidcRequestFilter](#oidc-client-ref-oidc-request-filters) to customize the authentication.

### OIDC Client SPI

When your custom extension must acquire OIDC tokens by using one of the token grants supported by the OIDC client, the extension can depend only on the OIDC Client SPI and let the OIDC client itself acquire and refresh access tokens as needed.

1. Add the following dependency:

   ```xml
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-oidc-client-spi</artifactId>
   </dependency>
   ```


1. Update your extension to use the `io.quarkus.oidc.client.spi.TokenProvider` CDI bean as required. For example:

   ```java
   package org.acme.extension;

   import jakarta.inject.Inject;
   import io.quarkus.oidc.client.spi.TokenProvider;

   public class ExtensionOAuth2Support {

      @Inject
      TokenProvider tokenProvider;

      public Uni<String> getAccessToken() {
          return tokenProvider.getAccessToken();
      }
   }
   ```

   Currently, `io.quarkus.oidc.client.spi.TokenProvider` is available only for default OIDC clients because custom extensions are unlikely to be aware of multiple named OIDC clients.


<a id="integration-testing-oidc-client"></a>
### Testing

Start by adding the following dependencies to your test project:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

<a id="oidc-client-ref-integration-testing-wiremock"></a>
#### Wiremock

1. Add the following dependencies to your test project:

   ```xml
   <dependency>
       <groupId>org.wiremock</groupId>
       <artifactId>wiremock</artifactId>
       <scope>test</scope>
       <version>${wiremock.version}</version> // <1>
   </dependency>
   ```

   1. Use a proper Wiremock version. View all [available WireMock versions](https://central.sonatype.com/artifact/org.wiremock/wiremock?smo=true) on Maven Central.


1. Write a Wiremock-based `QuarkusTestResourceLifecycleManager`, for example:

   ```properties
   package io.quarkus.it.keycloak;

   import static com.github.tomakehurst.wiremock.client.WireMock.matching;
   import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

   import java.util.HashMap;
   import java.util.Map;

   import com.github.tomakehurst.wiremock.WireMockServer;
   import com.github.tomakehurst.wiremock.client.WireMock;
   import com.github.tomakehurst.wiremock.core.Options.ChunkedEncodingPolicy;

   import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

   public class KeycloakRealmResourceManager implements QuarkusTestResourceLifecycleManager {
       private WireMockServer server;

       @Override
       public Map<String, String> start() {

           server = new WireMockServer(wireMockConfig().dynamicPort().useChunkedTransferEncoding(ChunkedEncodingPolicy.NEVER));
           server.start();

           server.stubFor(WireMock.post("/tokens")
                   .withRequestBody(matching("grant_type=password&username=alice&password=alice"))
                   .willReturn(WireMock
                           .aResponse()
                           .withHeader("Content-Type", "application/json")
                           .withBody(
                                   "{\"access_token\":\"access_token_1\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}")));
           server.stubFor(WireMock.post("/tokens")
                   .withRequestBody(matching("grant_type=refresh_token&refresh_token=refresh_token_1"))
                   .willReturn(WireMock
                           .aResponse()
                           .withHeader("Content-Type", "application/json")
                           .withBody(
                                   "{\"access_token\":\"access_token_2\", \"expires_in\":4, \"refresh_token\":\"refresh_token_1\"}")));


           Map<String, String> conf = new HashMap<>();
           conf.put("keycloak.url", server.baseUrl());
           return conf;
       }

       @Override
       public synchronized void stop() {
           if (server != null) {
               server.stop();
               server = null;
           }
       }
   }
   ```


1. Prepare the REST test endpoints.

   You can have the test front-end endpoint, which uses the injected MP REST client with a registered OidcClient filter, call the downstream endpoint. This endpoint echoes the token back. For example, see the `integration-tests/oidc-client-wiremock` in the `main` Quarkus repository.


1. Set `application.properties`, for example:

   ```properties
   # Use the 'keycloak.url' property set by the test KeycloakRealmResourceManager
   quarkus.oidc-client.auth-server-url=${keycloak.url:replaced-by-test-resource}
   quarkus.oidc-client.discovery-enabled=false
   quarkus.oidc-client.token-path=/tokens
   quarkus.oidc-client.client-id=quarkus-service-app
   quarkus.oidc-client.credentials.secret=secret
   quarkus.oidc-client.grant.type=password
   quarkus.oidc-client.grant-options.password.username=alice
   quarkus.oidc-client.grant-options.password.password=alice
   ```


1. Write the test code.

   Given the Wiremock-based resource above, the first test invocation should return the `access_token_1` access token, which will expire in 4 seconds. Use `awaitility` to wait for about 5 seconds. The next test invocation should return the `access_token_2` access token, which confirms the expired `access_token_1` access token has been refreshed.


#### Keycloak

If you work with Keycloak, you can use the same approach described in the [OpenID Connect Bearer Token Integration testing](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing-keycloak) Keycloak section.

### How to check the errors in the logs

1. Enable `io.quarkus.oidc.client.runtime.OidcClientImpl` `TRACE` level logging to see more details about the token acquisition and refresh errors:

   ```properties
   quarkus.log.category."io.quarkus.oidc.client.runtime.OidcClientImpl".level=TRACE
   quarkus.log.category."io.quarkus.oidc.client.runtime.OidcClientImpl".min-level=TRACE
   ```


1. Enable `io.quarkus.oidc.client.runtime.OidcClientRecorder` `TRACE` level logging to see more details about the OidcClient initialization errors:

   ```properties
   quarkus.log.category."io.quarkus.oidc.client.runtime.OidcClientRecorder".level=TRACE
   quarkus.log.category."io.quarkus.oidc.client.runtime.OidcClientRecorder".min-level=TRACE
   ```


<a id="oidc-client-ref-oidc-request-filters"></a>
## OIDC request filters

You can filter OIDC requests made by the OIDC client to the OIDC provider by registering one or more `OidcRequestFilter` implementations, which can update or add new request headers, customize or analyze the request body.

You can have a single filter intercept requests to all OIDC provider endpoints, or use an `@OidcEndpoint` annotation to apply this filter to requests to specific endpoints only.

For example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;

@ApplicationScoped
@OidcEndpoint(value = Type.TOKEN)
@Unremovable
public class OidcRequestCustomizer implements OidcRequestFilter {

    @Override
    public Uni<Void> filter(OidcRequestFilterContext requestContext) {
        HttpMethod method = requestContext.request().method();
        String uri = requestContext.request().uri();
        if (method == HttpMethod.POST && uri.endsWith("/token") && requestContext.requestBody() != null) {
            requestContext.request().putHeader("Digest", calculateDigest(requestContext.requestBody().toString()));
        }
        return Uni.createFrom().voidItem();
    }

    private String calculateDigest(String bodyString) {
        // Apply the required digest algorithm to the body string.
        // Implementation details are omitted for brevity.
    }
}
```

`OidcRequestContextProperties` can be used to access request properties. Currently, you can use a `client_id` key to access the client tenant ID and a `grant_type` key to access the grant type that the OIDC client uses to acquire tokens.

`OidcRequestFilter` can customize the request body by updating it with a `String` or by preparing an instance of `io.vertx.mutiny.core.buffer.Buffer` and setting it on a request context, for example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class TokenRequestFilter implements OidcRequestFilter {

    @Override
    public Uni<Void> filter(OidcRequestFilterContext rc) {
        // Add more required properties to the token request
        rc.requestBody(rc.requestBody().toString() + "&opaque_token_param=opaque_token_value"));
        return Uni.createFrom().voidItem();
    }
}
```

<a id="oidc-client-ref-oidc-response-filters"></a>
## OIDC response filters

You can filter responses to OIDC client requests by registering one or more `OidcResponseFilter` implementations that can check the response status, headers, and body to log them or perform other actions.

You can have a single filter intercept responses to all OIDC client requests, or use an `@OidcEndpoint` annotation to apply this filter to responses to specific OIDC client requests only.

For example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.smallrye.mutiny.Uni;
import io.quarkus.oidc.common.runtime.OidcConstants;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN) <1>
public class TokenEndpointResponseFilter implements OidcResponseFilter {
    private static final Logger LOG = Logger.getLogger(TokenEndpointResponseFilter.class);

    @Override
    public Uni<Void> filter(OidcResponseFilterContext rc) {
        String contentType = rc.responseHeaders().get("Content-Type"); <2>
        if (contentType.equals("application/json")
                && "refresh_token".equals(rc.requestProperties().get(OidcConstants.GRANT_TYPE)) <3>
                && rc.responseBody().toJsonObject().containsKey("refresh_token")) { <4>
            LOG.debug("Tokens have been refreshed");
        }
        return Uni.createFrom().voidItem();
    }

}
```

1. Restrict this filter to requests targeting the OIDC token endpoint only.
2. Check the response `Content-Type` header.
3. Use `OidcRequestContextProperties` request properties to confirm it is a `refresh_grant` token grant response.
4. Confirm the response JSON contains a `refresh_token` property.

`OidcResponseFilter` can customize the response body by preparing an instance of `io.vertx.mutiny.core.buffer.Buffer` and setting it as a property on a response context, for example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcEndpoint.Type;
import io.quarkus.oidc.common.OidcRequestContextProperties;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;

@ApplicationScoped
@Unremovable
@OidcEndpoint(value = Type.TOKEN)
public class TokenResponseFilter implements OidcResponseFilter {

    @Override
    public Uni<Void> filter(OidcResponseFilterContext rc) {
        JsonObject body = rc.responseBody().toJsonObject();
        // JSON `scope` property has multiple values separated by a comma character.
        // It must be replaced with a space character.
        String scope = body.getString("scope");
        body.put("scope", scope.replace(",", " "));
        rc.responseBody(Buffer.buffer(body.toString()));
        return Uni.createFrom().voidItem();
    }
}
```

<a id="token-propagation-rest"></a>
## Token Propagation for Quarkus REST

The `quarkus-rest-client-oidc-token-propagation` extension provides a REST Client filter, `io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter`, that simplifies the propagation of authentication information.

This filter propagates the [bearer token](/guides/security-oidc-bearer-token-authentication/index.md) present in the currently active request or the token acquired from the [authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md) as the HTTP `Authorization` header's `Bearer` scheme value.

You can selectively register `AccessTokenRequestReactiveFilter` by using either the `io.quarkus.oidc.token.propagation.common.AccessToken` annotation or the `org.eclipse.microprofile.rest.client.annotation.RegisterProvider` annotation, for example:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@AccessToken
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

Alternatively, apply `@AccessToken` to individual methods to propagate access tokens for requests made by those methods. This lets a single REST client interface call both public endpoints and endpoints that require an access token:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@Path("/")
public interface InformationService {

    @AccessToken
    @GET
    String getUserName();

    @Path("/public")
    @GET
    String getPublicInformation();
}
```

or

```java
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.reactive.AccessTokenRequestReactiveFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@RegisterProvider(AccessTokenRequestReactiveFilter.class)
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

You can combine class-level and method-level `@AccessToken` annotations on the same REST client. When you do, Quarkus uses the class-level annotation for unannotated methods and the method-level annotation, including its `exchangeTokenClient` attribute, for annotated methods.

Additionally, `AccessTokenRequestReactiveFilter` can support a complex application that needs to exchange tokens before propagating them.

If you work with [Keycloak](https://www.keycloak.org/securing-apps/token-exchange) or another OIDC provider that supports a [Token Exchange](https://tools.ietf.org/html/rfc8693) token grant, then you can configure `AccessTokenRequestReactiveFilter` to exchange the token like this:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
quarkus.oidc-client.grant.type=exchange
quarkus.oidc-client.grant-options.exchange.audience=quarkus-app-exchange

quarkus.rest-client-oidc-token-propagation.exchange-token=true <1>
```

1. Please note that the `exchange-token` configuration property is ignored when the OidcClient name is set with the `io.quarkus.oidc.token.propagation.common.AccessToken#exchangeTokenClient` annotation attribute.

> [!NOTE]
> `AccessTokenRequestReactiveFilter` will use `OidcClient` to exchange the current token, and you can use `quarkus.oidc-client.grant-options.exchange` to set the additional exchange properties expected by your OpenID Connect provider.

If you work with providers such as `Azure` that [require using](https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-on-behalf-of-flow#example) [JWT bearer token grant](https://www.rfc-editor.org/rfc/rfc7523#section-2.1) to exchange the current token, then you can configure `AccessTokenRequestReactiveFilter` to exchange the token like this:

```properties
quarkus.oidc-client.auth-server-url=${azure.provider.url}
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret

quarkus.oidc-client.grant.type=jwt
quarkus.oidc-client.grant-options.jwt.requested_token_use=on_behalf_of
quarkus.oidc-client.scopes=https://graph.microsoft.com/user.read,offline_access

quarkus.resteasy-client-oidc-token-propagation.exchange-token=true
```

`AccessTokenRequestReactiveFilter` uses a default `OidcClient` by default. A named `OidcClient` can be selected with a `quarkus.rest-client-oidc-token-propagation.client-name` configuration property or with the `io.quarkus.oidc.token.propagation.common.AccessToken#exchangeTokenClient` annotation attribute.

<a id="token-propagation-resteasy"></a>
## Token Propagation for RESTEasy Classic

The `quarkus-resteasy-client-oidc-token-propagation` extension provides two Jakarta REST `jakarta.ws.rs.client.ClientRequestFilter` class implementations that simplify the propagation of authentication information. `io.quarkus.oidc.token.propagation.AccessTokenRequestFilter` propagates the [Bearer token](/guides/security-oidc-bearer-token-authentication/index.md) present in the current active request or the token acquired from the [Authorization code flow mechanism](/guides/security-oidc-code-flow-authentication/index.md), as the HTTP `Authorization` header's `Bearer` scheme value.

The `io.quarkus.oidc.token.propagation.JsonWebTokenRequestFilter` provides the same functionality and, in addition, supports JWT tokens.

When you need to propagate the current Authorization Code Flow access token, immediate token propagation works well because the code flow access tokens, as opposed to ID tokens, are meant to be propagated for the current Quarkus endpoint to access the remote services on behalf of the currently authenticated user.

However, direct end-to-end Bearer token propagation should be avoided. For example, `Client -> Service A -> Service B`, where `Service B` receives a token sent by `Client` to `Service A`. In such cases, `Service B` cannot distinguish whether the token came from `Service A` or directly from `Client`. For `Service B` to verify that the token originated from `Service A`, it should be able to assert new issuer and audience claims.

Additionally, a complex application might need to exchange or update the tokens before propagating them. For example, the access context might differ when `Service A` accesses `Service B`. In this case, `Service A` might be granted a narrow or completely different set of scopes to access `Service B`.

The following sections show how `AccessTokenRequestFilter` and `JsonWebTokenRequestFilter` can help.

### RestClient AccessTokenRequestFilter

`AccessTokenRequestFilter` treats all tokens as strings and, as such, can work with both JWT and opaque tokens.

You can selectively register `AccessTokenRequestFilter` by using either `io.quarkus.oidc.token.propagation.common.AccessToken` or `org.eclipse.microprofile.rest.client.annotation.RegisterProvider`, for example:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@AccessToken
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

Alternatively, apply `@AccessToken` to individual methods to propagate access tokens for requests made by those methods. This lets a single REST client interface call both public endpoints and endpoints that require an access token:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.common.AccessToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@Path("/")
public interface InformationService {

    @AccessToken
    @GET
    String getUserName();

    @Path("/public")
    @GET
    String getPublicInformation();
}
```

or

```java
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.AccessTokenRequestFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@RegisterProvider(AccessTokenRequestFilter.class)
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

You can combine class-level and method-level `@AccessToken` annotations on the same REST client. When you do, Quarkus uses the class-level annotation for unannotated methods and the method-level annotation, including its `exchangeTokenClient` attribute, for annotated methods.

Alternatively, `AccessTokenRequestFilter` can be registered automatically with all MP Rest or Jakarta REST clients if the `quarkus.resteasy-client-oidc-token-propagation.register-filter` property is set to `true` and the `quarkus.resteasy-client-oidc-token-propagation.json-web-token` property is set to `false`, which is the default value.

#### Exchange token before propagation

If the current access token needs to be exchanged before propagation and you work with [Keycloak](https://www.keycloak.org/securing-apps/token-exchange) or another OpenID Connect provider that supports a [Token Exchange](https://tools.ietf.org/html/rfc8693) token grant, you can configure `AccessTokenRequestFilter` like this:

```properties
quarkus.oidc-client.auth-server-url=http://localhost:8180/auth/realms/quarkus
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret
quarkus.oidc-client.grant.type=exchange
quarkus.oidc-client.grant-options.exchange.audience=quarkus-app-exchange

quarkus.resteasy-client-oidc-token-propagation.exchange-token=true
```

If you work with providers such as `Azure` that [require using](https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-on-behalf-of-flow#example) [JWT bearer token grant](https://www.rfc-editor.org/rfc/rfc7523#section-2.1) to exchange the current token, you can configure `AccessTokenRequestFilter` to exchange the token like this:

```properties
quarkus.oidc-client.auth-server-url=${azure.provider.url}
quarkus.oidc-client.client-id=quarkus-app
quarkus.oidc-client.credentials.secret=secret

quarkus.oidc-client.grant.type=jwt
quarkus.oidc-client.grant-options.jwt.requested_token_use=on_behalf_of
quarkus.oidc-client.scopes=https://graph.microsoft.com/user.read,offline_access

quarkus.resteasy-client-oidc-token-propagation.exchange-token=true
```

> [!NOTE]
> `AccessTokenRequestFilter` will use `OidcClient` to exchange the current token, and you can use `quarkus.oidc-client.grant-options.exchange` to set the additional exchange properties expected by your OpenID Connect provider.

`AccessTokenRequestFilter` uses a default `OidcClient` by default. A named `OidcClient` can be selected with the `quarkus.resteasy-client-oidc-token-propagation.client-name` configuration property.

### RestClient JsonWebTokenRequestFilter

Using `JsonWebTokenRequestFilter` is recommended if you work with Bearer JWT tokens whose claims, such as `issuer` and `audience`, can be modified and the updated tokens re-secured, for example, re-signed.

It expects an injected `org.eclipse.microprofile.jwt.JsonWebToken` and therefore will not work with opaque tokens. Also, if your OpenID Connect provider supports a Token Exchange protocol, it is recommended to use `AccessTokenRequestFilter` instead, because both JWT and opaque bearer tokens can be securely exchanged with `AccessTokenRequestFilter`.

`JsonWebTokenRequestFilter` makes it easy for `Service A` implementations to update the injected `org.eclipse.microprofile.jwt.JsonWebToken` with the new `issuer` and `audience` claim values and secure the updated token again with a new signature. The only difficult step is ensuring that `Service A` has a signing key, which should be provisioned from a secure file system or remote secure storage such as Vault.

You can selectively register `JsonWebTokenRequestFilter` by using either `io.quarkus.oidc.token.propagation.JsonWebToken` or `org.eclipse.microprofile.rest.client.annotation.RegisterProvider`, for example:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.JsonWebToken;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@JsonWebToken
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

or

```java
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.oidc.token.propagation.JsonWebTokenRequestFilter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterRestClient
@RegisterProvider(JsonWebTokenRequestFilter.class)
@Path("/")
public interface ProtectedResourceService {

    @GET
    String getUserName();
}
```

Alternatively, `JsonWebTokenRequestFilter` can be registered automatically with all MicroProfile REST or Jakarta REST clients if both the `quarkus.resteasy-client-oidc-token-propagation.register-filter` and `quarkus.resteasy-client-oidc-token-propagation.json-web-token` properties are set to `true`.

#### Update token before propagation

If the injected token needs to have its `iss` (issuer) or `aud` (audience) claims updated and secured again with a new signature, then you can configure `JsonWebTokenRequestFilter` like this:

```properties
quarkus.resteasy-client-oidc-token-propagation.secure-json-web-token=true
smallrye.jwt.sign.key.location=/privateKey.pem
# Set a new issuer
smallrye.jwt.new-token.issuer=http://frontend-resource
# Set a new audience
smallrye.jwt.new-token.audience=http://downstream-resource
# Override the existing token issuer and audience claims if they are already set
smallrye.jwt.new-token.override-matching-claims=true
```

As mentioned, use `AccessTokenRequestFilter` if you work with Keycloak or an OpenID Connect provider that supports a Token Exchange protocol.

<a id="integration-testing-token-propagation"></a>
### Testing

Typically, you must prepare two REST test endpoints. The first endpoint uses the injected MP REST client with a registered token propagation filter to call the second endpoint.

To learn how it can be done, follow the [OpenID Connect client and token propagation](/guides/security-openid-connect-client/index.md) quickstart, and its [Testing](/guides/security-openid-connect-client/index.md#testing) section in particular.

<a id="quarkus-oidc-client-graphql"></a>
## GraphQL client integration

The `quarkus-oidc-client-graphql` extension provides a way to integrate an OIDC client into [GraphQL clients](/guides/smallrye-graphql-client/index.md), paralleling the approach used with REST clients.

When this extension is active, any GraphQL client configured through properties, rather than programmatically by the builder, uses the OIDC client to acquire an access token, which it then sets as the `Authorization` header value. The OIDC client also refreshes expired access tokens.

To configure which OIDC client the GraphQL client should use, select one of the configured OIDC clients with the `quarkus.oidc-client-graphql.client-name` property, for example:

```properties
quarkus.oidc-client-graphql.client-name=oidc-client-for-graphql

# example declaration of the OIDC client itself
quarkus.oidc-client.oidc-client-for-graphql.auth-server-url=${keycloak.url}
quarkus.oidc-client.oidc-client-for-graphql.grant.type=password
quarkus.oidc-client.oidc-client-for-graphql.grant-options.password.username=${username}
quarkus.oidc-client.oidc-client-for-graphql.grant-options.password.password=${password}
quarkus.oidc-client.oidc-client-for-graphql.client-id=${quarkus.oidc.client-id}
quarkus.oidc-client.oidc-client-for-graphql.credentials.client-secret.value=${keycloak.credentials.secret}
quarkus.oidc-client.oidc-client-for-graphql.credentials.client-secret.method=POST
```

> [!NOTE]
> If you do not specify the `quarkus.oidc-client-graphql.client-name` property, GraphQL clients will use the default OIDC client without an explicit name.

Specifically for type-safe GraphQL clients, you can override this on a per-client basis by annotating the `GraphQLClientApi` interface with `@io.quarkus.oidc.client.filter.OidcClientFilter`.

For example:

```java
@GraphQLClientApi(configKey = "order-client")
@OidcClientFilter("oidc-client-for-graphql")
public interface OrdersGraphQLClient {
    // Queries, mutations, and subscriptions go here.
}
```

To use this with a programmatically created GraphQL client, both builders, `VertxDynamicGraphQLClientBuilder` and `VertxTypesafeGraphQLClientBuilder`, contain a `dynamicHeader(String, Uni<String>)` method that lets you plug in a header that might change for every request.

To plug an OIDC client into it, use:

```java
@Inject
OidcClients oidcClients;

VertxDynamicGraphQLClientBuilder builder = ....;
Uni<String> tokenUni = oidcClients.getClient("OIDC_CLIENT_NAME")
    .getTokens().map(t -> "Bearer " + t.getAccessToken());
builder.dynamicHeader("Authorization", tokenUni);
VertxDynamicGraphQLClient client = builder.build();
```

For dynamic GraphQL clients, you can override the `quarkus.oidc-client-graphql.client-name` configuration property on a per-client basis with the `quarkus.oidc-client-graphql."graphql-client".client-name` configuration property.

**Example `application.properties`**

```properties
quarkus.oidc-client-graphql."dynamic-graphql-client-2".client-name=oidc-client-for-graphql-2

# example declaration of the OIDC client itself
quarkus.oidc-client.oidc-client-for-graphql-2.auth-server-url=${keycloak.url}
quarkus.oidc-client.oidc-client-for-graphql-2.grant.type=password
quarkus.oidc-client.oidc-client-for-graphql-2.grant-options.password.username=${username}
quarkus.oidc-client.oidc-client-for-graphql-2.grant-options.password.password=${password}
quarkus.oidc-client.oidc-client-for-graphql-2.client-id=${quarkus.oidc.client-id}
quarkus.oidc-client.oidc-client-for-graphql-2.credentials.client-secret.value=${keycloak.credentials.secret}
quarkus.oidc-client.oidc-client-for-graphql-2.credentials.client-secret.method=POST
```

**An example of dynamic GraphQL client**

```java
@Inject
@GraphQLClient("dynamic-graphql-client-2")
DynamicGraphQLClient dynamicClient;

String dynamicClientAdmin() throws ExecutionException, InterruptedException {
    return dynamicClient.executeSync("query { principalName }").getData().getString("principalName"); <1>
}
```

1. The `dynamic-graphql-client-2` GraphQL client calls have the `Authorization` header with an access token acquired by the `oidc-client-for-graphql-2` OIDC client.

<a id="health-check"></a>
## Health Check

When you use the `quarkus-smallrye-health` extension, Quarkus can expose health checks for OIDC clients.

The health check uses OIDC endpoint discovery to validate the connection. Therefore, discovery must be enabled.

To activate the health check, set `quarkus.oidc-client.health.enabled` to true and ensure discovery is not disabled:

```properties
quarkus.oidc-client.health.enabled=true
```

<a id="configuration-reference"></a>
## Configuration reference

### OIDC client

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-oidc-client_quarkus-oidc-client-enabled"></a> [`quarkus.oidc-client.enabled`](#quarkus-oidc-client_quarkus-oidc-client-enabled) config_property_copy_button:+++quarkus.oidc-client.enabled+++[]<br><br>If the OIDC client extension is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-oidc-client_quarkus-oidc-client-health-enabled"></a> [`quarkus.oidc-client.health.enabled`](#quarkus-oidc-client_quarkus-oidc-client-health-enabled) config_property_copy_button:+++quarkus.oidc-client.health.enabled+++[]<br><br>Whether the OIDC Client extension should automatically register a health check for OIDC clients when a Health Check capability is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_HEALTH_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-auth-server-url"></a> [`quarkus.oidc-client.auth-server-url`](#quarkus-oidc-client_quarkus-oidc-client-auth-server-url) config_property_copy_button:+++quarkus.oidc-client.auth-server-url+++[]<br><br>`quarkus.oidc-client."id".auth-server-url` config_property_copy_button:+++quarkus.oidc-client."id".auth-server-url+++[]<br><br>The base URL of an OpenID Connect (OIDC) server, for example, `https://host:port/auth`. Do not set this property if you use the public key verification (`public-key`) or certificate chain verification only (`certificate-chain`).<br><br>By default, when an OIDC configuration metadata discovery is enabled with the `discovery-enabled()` property, it is retrieved from a well known provider endpoint with its URL calculated by appending a value of the `discovery-path()` path such as `.well-known/openid-configuration` to this URL.<br><br>For Keycloak, use `https://host:port/realms/{realm}`, replacing `{realm}` with the Keycloak realm name.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_AUTH_SERVER_URL+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-discovery-enabled"></a> [`quarkus.oidc-client.discovery-enabled`](#quarkus-oidc-client_quarkus-oidc-client-discovery-enabled) config_property_copy_button:+++quarkus.oidc-client.discovery-enabled+++[]<br><br>`quarkus.oidc-client."id".discovery-enabled` config_property_copy_button:+++quarkus.oidc-client."id".discovery-enabled+++[]<br><br>Enable discovery of the OIDC endpoints. If not enabled, you must configure the OIDC endpoint URLs individually.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_DISCOVERY_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-discovery-path"></a> [`quarkus.oidc-client.discovery-path`](#quarkus-oidc-client_quarkus-oidc-client-discovery-path) config_property_copy_button:+++quarkus.oidc-client.discovery-path+++[]<br><br>`quarkus.oidc-client."id".discovery-path` config_property_copy_button:+++quarkus.oidc-client."id".discovery-path+++[]<br><br>The relative path of the OIDC discovery endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_DISCOVERY_PATH+++[] | string | `.well-known/openid-configuration` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-registration-path"></a> [`quarkus.oidc-client.registration-path`](#quarkus-oidc-client_quarkus-oidc-client-registration-path) config_property_copy_button:+++quarkus.oidc-client.registration-path+++[]<br><br>`quarkus.oidc-client."id".registration-path` config_property_copy_button:+++quarkus.oidc-client."id".registration-path+++[]<br><br>The relative path or absolute URL of the OIDC dynamic client registration endpoint. Set if `discovery-enabled` is `false` or a discovered token endpoint path must be customized.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REGISTRATION_PATH+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-connection-delay"></a> [`quarkus.oidc-client.connection-delay`](#quarkus-oidc-client_quarkus-oidc-client-connection-delay) config_property_copy_button:+++quarkus.oidc-client.connection-delay+++[]<br><br>`quarkus.oidc-client."id".connection-delay` config_property_copy_button:+++quarkus.oidc-client."id".connection-delay+++[]<br><br>The duration to attempt the initial connection to an OIDC server. For example, setting the duration to `20S` allows 10 retries, each 2 seconds apart. This property is only effective when the initial OIDC connection is created. For dropped connections, use the `connection-retry-count` property instead.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CONNECTION_DELAY+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client) |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-connection-retry-count"></a> [`quarkus.oidc-client.connection-retry-count`](#quarkus-oidc-client_quarkus-oidc-client-connection-retry-count) config_property_copy_button:+++quarkus.oidc-client.connection-retry-count+++[]<br><br>`quarkus.oidc-client."id".connection-retry-count` config_property_copy_button:+++quarkus.oidc-client."id".connection-retry-count+++[]<br><br>The number of times to retry re-establishing an existing OIDC connection if it is temporarily lost. Different from `connection-delay`, which applies only to initial connection attempts. For instance, if a request to the OIDC token endpoint fails due to a connection issue, it will be retried as per this setting.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CONNECTION_RETRY_COUNT+++[] | int | `3` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-connection-timeout"></a> [`quarkus.oidc-client.connection-timeout`](#quarkus-oidc-client_quarkus-oidc-client-connection-timeout) config_property_copy_button:+++quarkus.oidc-client.connection-timeout+++[]<br><br>`quarkus.oidc-client."id".connection-timeout` config_property_copy_button:+++quarkus.oidc-client."id".connection-timeout+++[]<br><br>The number of seconds after which the current OIDC connection request times out.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client) | `10S` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-use-blocking-dns-lookup"></a> [`quarkus.oidc-client.use-blocking-dns-lookup`](#quarkus-oidc-client_quarkus-oidc-client-use-blocking-dns-lookup) config_property_copy_button:+++quarkus.oidc-client.use-blocking-dns-lookup+++[]<br><br>`quarkus.oidc-client."id".use-blocking-dns-lookup` config_property_copy_button:+++quarkus.oidc-client."id".use-blocking-dns-lookup+++[]<br><br>Whether DNS lookup should be performed on the worker thread. Use this option when you can see logged warnings about blocked Vert.x event loop by HTTP requests to OIDC server.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_USE_BLOCKING_DNS_LOOKUP+++[] | boolean | `false` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-max-pool-size"></a> [`quarkus.oidc-client.max-pool-size`](#quarkus-oidc-client_quarkus-oidc-client-max-pool-size) config_property_copy_button:+++quarkus.oidc-client.max-pool-size+++[]<br><br>`quarkus.oidc-client."id".max-pool-size` config_property_copy_button:+++quarkus.oidc-client."id".max-pool-size+++[]<br><br>The maximum size of the connection pool used by the WebClient.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_MAX_POOL_SIZE+++[] | int |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-follow-redirects"></a> [`quarkus.oidc-client.follow-redirects`](#quarkus-oidc-client_quarkus-oidc-client-follow-redirects) config_property_copy_button:+++quarkus.oidc-client.follow-redirects+++[]<br><br>`quarkus.oidc-client."id".follow-redirects` config_property_copy_button:+++quarkus.oidc-client."id".follow-redirects+++[]<br><br>Follow redirects automatically when WebClient gets HTTP 302. When this property is disabled only a single redirect to exactly the same original URI is allowed but only if one or more cookies were set during the redirect request.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_FOLLOW_REDIRECTS+++[] | boolean | `true` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-token-path"></a> [`quarkus.oidc-client.token-path`](#quarkus-oidc-client_quarkus-oidc-client-token-path) config_property_copy_button:+++quarkus.oidc-client.token-path+++[]<br><br>`quarkus.oidc-client."id".token-path` config_property_copy_button:+++quarkus.oidc-client."id".token-path+++[]<br><br>The OIDC token endpoint that issues access and refresh tokens; specified as a relative path or absolute URL. Set if `discovery-enabled` is `false` or a discovered token endpoint path must be customized.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_TOKEN_PATH+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-revoke-path"></a> [`quarkus.oidc-client.revoke-path`](#quarkus-oidc-client_quarkus-oidc-client-revoke-path) config_property_copy_button:+++quarkus.oidc-client.revoke-path+++[]<br><br>`quarkus.oidc-client."id".revoke-path` config_property_copy_button:+++quarkus.oidc-client."id".revoke-path+++[]<br><br>The relative path or absolute URL of the OIDC token revocation endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REVOKE_PATH+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-client-id"></a> [`quarkus.oidc-client.client-id`](#quarkus-oidc-client_quarkus-oidc-client-client-id) config_property_copy_button:+++quarkus.oidc-client.client-id+++[]<br><br>`quarkus.oidc-client."id".client-id` config_property_copy_button:+++quarkus.oidc-client."id".client-id+++[]<br><br>The client id of the application. Each application has a client id that is used to identify the application. Setting the client id is not required if `application-type` is `service` and no token introspection is required.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CLIENT_ID+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-client-name"></a> [`quarkus.oidc-client.client-name`](#quarkus-oidc-client_quarkus-oidc-client-client-name) config_property_copy_button:+++quarkus.oidc-client.client-name+++[]<br><br>`quarkus.oidc-client."id".client-name` config_property_copy_button:+++quarkus.oidc-client."id".client-name+++[]<br><br>The client name of the application. It is meant to represent a human readable description of the application which you may provide when an application (client) is registered in an OpenId Connect provider's dashboard. For example, you can set this property to have more informative log messages which record an activity of the given client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CLIENT_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-id"></a> [`quarkus.oidc-client.id`](#quarkus-oidc-client_quarkus-oidc-client-id) config_property_copy_button:+++quarkus.oidc-client.id+++[]<br><br>`quarkus.oidc-client."id".id` config_property_copy_button:+++quarkus.oidc-client."id".id+++[]<br><br>A unique OIDC client identifier. It must be set when OIDC clients are created dynamically and is optional in all other cases.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_ID+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-client-enabled"></a> [`quarkus.oidc-client.client-enabled`](#quarkus-oidc-client_quarkus-oidc-client-client-enabled) config_property_copy_button:+++quarkus.oidc-client.client-enabled+++[]<br><br>`quarkus.oidc-client."id".client-enabled` config_property_copy_button:+++quarkus.oidc-client."id".client-enabled+++[]<br><br>If this client configuration is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CLIENT_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-scopes"></a> [`quarkus.oidc-client.scopes`](#quarkus-oidc-client_quarkus-oidc-client-scopes) config_property_copy_button:+++quarkus.oidc-client.scopes+++[]<br><br>`quarkus.oidc-client."id".scopes` config_property_copy_button:+++quarkus.oidc-client."id".scopes+++[]<br><br>List of access token scopes<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_SCOPES+++[] | list of string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-audience"></a> [`quarkus.oidc-client.audience`](#quarkus-oidc-client_quarkus-oidc-client-audience) config_property_copy_button:+++quarkus.oidc-client.audience+++[]<br><br>`quarkus.oidc-client."id".audience` config_property_copy_button:+++quarkus.oidc-client."id".audience+++[]<br><br>List of access token audiences<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_AUDIENCE+++[] | list of string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-refresh-token-time-skew"></a> [`quarkus.oidc-client.refresh-token-time-skew`](#quarkus-oidc-client_quarkus-oidc-client-refresh-token-time-skew) config_property_copy_button:+++quarkus.oidc-client.refresh-token-time-skew+++[]<br><br>`quarkus.oidc-client."id".refresh-token-time-skew` config_property_copy_button:+++quarkus.oidc-client."id".refresh-token-time-skew+++[]<br><br>Refresh token time skew. If this property is enabled then the configured duration is converted to seconds and is added to the current time when checking whether the access token should be refreshed. If the sum is greater than this access token's expiration time then a refresh is going to happen.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REFRESH_TOKEN_TIME_SKEW+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client) |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-access-token-expires-in"></a> [`quarkus.oidc-client.access-token-expires-in`](#quarkus-oidc-client_quarkus-oidc-client-access-token-expires-in) config_property_copy_button:+++quarkus.oidc-client.access-token-expires-in+++[]<br><br>`quarkus.oidc-client."id".access-token-expires-in` config_property_copy_button:+++quarkus.oidc-client."id".access-token-expires-in+++[]<br><br>Access token expiration period relative to the current time. This property is only checked when an access token grant response does not include an access token expiration property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_ACCESS_TOKEN_EXPIRES_IN+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client) |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-access-token-expiry-skew"></a> [`quarkus.oidc-client.access-token-expiry-skew`](#quarkus-oidc-client_quarkus-oidc-client-access-token-expiry-skew) config_property_copy_button:+++quarkus.oidc-client.access-token-expiry-skew+++[]<br><br>`quarkus.oidc-client."id".access-token-expiry-skew` config_property_copy_button:+++quarkus.oidc-client."id".access-token-expiry-skew+++[]<br><br>Access token expiry time skew that can be added to the calculated token expiry time.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_ACCESS_TOKEN_EXPIRY_SKEW+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client) |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-absolute-expires-in"></a> [`quarkus.oidc-client.absolute-expires-in`](#quarkus-oidc-client_quarkus-oidc-client-absolute-expires-in) config_property_copy_button:+++quarkus.oidc-client.absolute-expires-in+++[]<br><br>`quarkus.oidc-client."id".absolute-expires-in` config_property_copy_button:+++quarkus.oidc-client."id".absolute-expires-in+++[]<br><br>If the access token 'expires_in' property should be checked as an absolute time value as opposed to a duration relative to the current time.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_ABSOLUTE_EXPIRES_IN+++[] | boolean | `false` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-grant-type"></a> [`quarkus.oidc-client.grant.type`](#quarkus-oidc-client_quarkus-oidc-client-grant-type) config_property_copy_button:+++quarkus.oidc-client.grant.type+++[]<br><br>`quarkus.oidc-client."id".grant.type` config_property_copy_button:+++quarkus.oidc-client."id".grant.type+++[]<br><br>Grant type<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_GRANT_TYPE+++[] | tooltip:client['client_credentials' grant requiring an OIDC client authentication only], tooltip:password['password' grant requiring both OIDC client and user ('username' and 'password') authentications], tooltip:code['authorization_code' grant requiring an OIDC client authentication as well as at least 'code' and 'redirect_uri' parameters which must be passed to OidcClient at the token request time.], tooltip:exchange['urn\:ietf\:params\:oauth\:grant-type\:token-exchange' grant requiring an OIDC client authentication as well as at least 'subject_token' parameter which must be passed to OidcClient at the token request time.], tooltip:jwt['urn\:ietf\:params\:oauth\:grant-type\:jwt-bearer' grant requiring an OIDC client authentication as well as at least an 'assertion' parameter which must be passed to OidcClient at the token request time.], tooltip:refresh['refresh_token' grant requiring an OIDC client authentication and a refresh token. Note,opts=OidcClient supports this grant by default if an access token acquisition response contained a refresh token. However,in some cases,the refresh token is provided out of band,for example,it can be shared between several of the confidential client's services,etc. If 'quarkus.oidc-client.grant-type' is set to 'refresh' then `OidcClient` will only support refreshing the tokens.], tooltip:ciba['urn\:openid\:params\:grant-type\:ciba' grant requiring an OIDC client authentication as well as 'auth_req_id' parameter which must be passed to OidcClient at the token request time.], tooltip:device['urn\:ietf\:params\:oauth\:grant-type\:device_code' grant requiring an OIDC client authentication as well as 'device_code' parameter which must be passed to OidcClient at the token request time.], tooltip:preauthorized-code['urn\:ietf\:params\:oauth\:grant-type\:pre-authorized_code' grant requiring an OIDC client authentication as well as at least a 'pre-authorized_code' parameter which must be passed to OidcClient at the token request time.] | tooltip:client['client_credentials' grant requiring an OIDC client authentication only] |
| <a id="quarkus-oidc-client_quarkus-oidc-client-grant-access-token-property"></a> [`quarkus.oidc-client.grant.access-token-property`](#quarkus-oidc-client_quarkus-oidc-client-grant-access-token-property) config_property_copy_button:+++quarkus.oidc-client.grant.access-token-property+++[]<br><br>`quarkus.oidc-client."id".grant.access-token-property` config_property_copy_button:+++quarkus.oidc-client."id".grant.access-token-property+++[]<br><br>Access token property name in a token grant response<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_GRANT_ACCESS_TOKEN_PROPERTY+++[] | string | `access_token` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-grant-refresh-token-property"></a> [`quarkus.oidc-client.grant.refresh-token-property`](#quarkus-oidc-client_quarkus-oidc-client-grant-refresh-token-property) config_property_copy_button:+++quarkus.oidc-client.grant.refresh-token-property+++[]<br><br>`quarkus.oidc-client."id".grant.refresh-token-property` config_property_copy_button:+++quarkus.oidc-client."id".grant.refresh-token-property+++[]<br><br>Refresh token property name in a token grant response<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_GRANT_REFRESH_TOKEN_PROPERTY+++[] | string | `refresh_token` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-grant-expires-in-property"></a> [`quarkus.oidc-client.grant.expires-in-property`](#quarkus-oidc-client_quarkus-oidc-client-grant-expires-in-property) config_property_copy_button:+++quarkus.oidc-client.grant.expires-in-property+++[]<br><br>`quarkus.oidc-client."id".grant.expires-in-property` config_property_copy_button:+++quarkus.oidc-client."id".grant.expires-in-property+++[]<br><br>Access token expiry property name in a token grant response<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_GRANT_EXPIRES_IN_PROPERTY+++[] | string | `expires_in` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-grant-refresh-expires-in-property"></a> [`quarkus.oidc-client.grant.refresh-expires-in-property`](#quarkus-oidc-client_quarkus-oidc-client-grant-refresh-expires-in-property) config_property_copy_button:+++quarkus.oidc-client.grant.refresh-expires-in-property+++[]<br><br>`quarkus.oidc-client."id".grant.refresh-expires-in-property` config_property_copy_button:+++quarkus.oidc-client."id".grant.refresh-expires-in-property+++[]<br><br>Refresh token expiry property name in a token grant response<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_GRANT_REFRESH_EXPIRES_IN_PROPERTY+++[] | string | `refresh_expires_in` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-grant-options-grant-name"></a> [`quarkus.oidc-client.grant-options."grant-name"`](#quarkus-oidc-client_quarkus-oidc-client-grant-options-grant-name) config_property_copy_button:+++quarkus.oidc-client.grant-options."grant-name"+++[]<br><br>`quarkus.oidc-client."id".grant-options."grant-name"` config_property_copy_button:+++quarkus.oidc-client."id".grant-options."grant-name"+++[]<br><br>Grant options<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_GRANT_OPTIONS__GRANT_NAME_+++[] | Map\<String,Map\<String,String>> |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-early-tokens-acquisition"></a> [`quarkus.oidc-client.early-tokens-acquisition`](#quarkus-oidc-client_quarkus-oidc-client-early-tokens-acquisition) config_property_copy_button:+++quarkus.oidc-client.early-tokens-acquisition+++[]<br><br>`quarkus.oidc-client."id".early-tokens-acquisition` config_property_copy_button:+++quarkus.oidc-client."id".early-tokens-acquisition+++[]<br><br>Requires that all filters which use 'OidcClient' acquire the tokens at the post-construct initialization time, possibly long before these tokens are used. This property should be disabled if the access token may expire before it is used for the first time and no refresh token is available.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_EARLY_TOKENS_ACQUISITION+++[] | boolean | `true` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-headers-headers"></a> [`quarkus.oidc-client.headers."headers"`](#quarkus-oidc-client_quarkus-oidc-client-headers-headers) config_property_copy_button:+++quarkus.oidc-client.headers."headers"+++[]<br><br>`quarkus.oidc-client."id".headers."headers"` config_property_copy_button:+++quarkus.oidc-client."id".headers."headers"+++[]<br><br>Custom HTTP headers which have to be sent to the token endpoint<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_HEADERS__HEADERS_+++[] | Map\<String,String> |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-refresh-interval"></a> [`quarkus.oidc-client.refresh-interval`](#quarkus-oidc-client_quarkus-oidc-client-refresh-interval) config_property_copy_button:+++quarkus.oidc-client.refresh-interval+++[]<br><br>`quarkus.oidc-client."id".refresh-interval` config_property_copy_button:+++quarkus.oidc-client."id".refresh-interval+++[]<br><br>Token refresh interval. By default, OIDC client refreshes the token during the current request, when it detects that it has expired, or nearly expired if the `refresh-token-time-skew()` is configured. But, when this property is configured, OIDC client can refresh the token asynchronously in the configured interval. This property is only effective with OIDC client filters and other `AbstractTokensProducer` extensions, but not when you use the `OidcClient#getTokens()` API directly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_REFRESH_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client) |  |
| <a id="quarkus-oidc-client_section_quarkus-oidc-client-proxy"></a> [HTTP proxy configuration](#quarkus-oidc-client_section_quarkus-oidc-client-proxy) | Type | Default |
| <a id="quarkus-oidc-client_quarkus-oidc-client-proxy-proxy-configuration-name"></a> [`quarkus.oidc-client.proxy.proxy-configuration-name`](#quarkus-oidc-client_quarkus-oidc-client-proxy-proxy-configuration-name) config_property_copy_button:+++quarkus.oidc-client.proxy.proxy-configuration-name+++[]<br><br>`quarkus.oidc-client."id".proxy.proxy-configuration-name` config_property_copy_button:+++quarkus.oidc-client."id".proxy.proxy-configuration-name+++[]<br><br>The name of the proxy configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.proxy.<name>.*`. Please note that the 'non-proxy-hosts' option is currently not supported. If a name is configured, but no proxy configuration is found with that name then an error will be thrown.<br><br>The default proxy configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_PROXY_PROXY_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_section_quarkus-oidc-client-tls"></a> [TLS configuration](#quarkus-oidc-client_section_quarkus-oidc-client-tls) | Type | Default |
| <a id="quarkus-oidc-client_quarkus-oidc-client-tls-tls-configuration-name"></a> [`quarkus.oidc-client.tls.tls-configuration-name`](#quarkus-oidc-client_quarkus-oidc-client-tls-tls-configuration-name) config_property_copy_button:+++quarkus.oidc-client.tls.tls-configuration-name+++[]<br><br>`quarkus.oidc-client."id".tls.tls-configuration-name` config_property_copy_button:+++quarkus.oidc-client."id".tls.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_TLS_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_section_quarkus-oidc-client-credentials"></a> [Different authentication options for OIDC client to access OIDC token and other secured endpoints](#quarkus-oidc-client_section_quarkus-oidc-client-credentials) | Type | Default |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-secret"></a> [`quarkus.oidc-client.credentials.secret`](#quarkus-oidc-client_quarkus-oidc-client-credentials-secret) config_property_copy_button:+++quarkus.oidc-client.credentials.secret+++[]<br><br>`quarkus.oidc-client."id".credentials.secret` config_property_copy_button:+++quarkus.oidc-client."id".credentials.secret+++[]<br><br>The client secret used by the `client_secret_basic` authentication method. Must be set unless a secret is set in `client-secret` or `jwt` client authentication is required. You can use `client-secret.value` instead, but both properties are mutually exclusive.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_SECRET+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-value"></a> [`quarkus.oidc-client.credentials.client-secret.value`](#quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-value) config_property_copy_button:+++quarkus.oidc-client.credentials.client-secret.value+++[]<br><br>`quarkus.oidc-client."id".credentials.client-secret.value` config_property_copy_button:+++quarkus.oidc-client."id".credentials.client-secret.value+++[]<br><br>The client secret value. This value is ignored if `credentials.secret` is set. Must be set unless a secret is set in `client-secret` or `jwt` client authentication is required.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_CLIENT_SECRET_VALUE+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-provider-name"></a> [`quarkus.oidc-client.credentials.client-secret.provider.name`](#quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-provider-name) config_property_copy_button:+++quarkus.oidc-client.credentials.client-secret.provider.name+++[]<br><br>`quarkus.oidc-client."id".credentials.client-secret.provider.name` config_property_copy_button:+++quarkus.oidc-client."id".credentials.client-secret.provider.name+++[]<br><br>The CredentialsProvider bean name, which should only be set if more than one CredentialsProvider is registered<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_CLIENT_SECRET_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-provider-keyring-name"></a> [`quarkus.oidc-client.credentials.client-secret.provider.keyring-name`](#quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-provider-keyring-name) config_property_copy_button:+++quarkus.oidc-client.credentials.client-secret.provider.keyring-name+++[]<br><br>`quarkus.oidc-client."id".credentials.client-secret.provider.keyring-name` config_property_copy_button:+++quarkus.oidc-client."id".credentials.client-secret.provider.keyring-name+++[]<br><br>The CredentialsProvider keyring name. The keyring name is only required when the CredentialsProvider being used requires the keyring name to look up the secret, which is often the case when a CredentialsProvider is shared by multiple extensions to retrieve credentials from a more dynamic source like a vault instance or secret manager<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_CLIENT_SECRET_PROVIDER_KEYRING_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-provider-key"></a> [`quarkus.oidc-client.credentials.client-secret.provider.key`](#quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-provider-key) config_property_copy_button:+++quarkus.oidc-client.credentials.client-secret.provider.key+++[]<br><br>`quarkus.oidc-client."id".credentials.client-secret.provider.key` config_property_copy_button:+++quarkus.oidc-client."id".credentials.client-secret.provider.key+++[]<br><br>The CredentialsProvider client secret key<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_CLIENT_SECRET_PROVIDER_KEY+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-method"></a> [`quarkus.oidc-client.credentials.client-secret.method`](#quarkus-oidc-client_quarkus-oidc-client-credentials-client-secret-method) config_property_copy_button:+++quarkus.oidc-client.credentials.client-secret.method+++[]<br><br>`quarkus.oidc-client."id".credentials.client-secret.method` config_property_copy_button:+++quarkus.oidc-client."id".credentials.client-secret.method+++[]<br><br>The authentication method. If the `clientSecret.value` secret is set, this method is `basic` by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_CLIENT_SECRET_METHOD+++[] | tooltip:basic[`client_secret_basic` (default)\: The client id and secret are submitted with the HTTP Authorization Basic scheme.], tooltip:post[`client_secret_post`\: The client id and secret are submitted as the `client_id` and `client_secret` form parameters.], tooltip:post-jwt[`client_secret_jwt`\: The client id and generated JWT secret are submitted as the `client_id` and `client_secret` form parameters.], tooltip:query[client id and secret are submitted as HTTP query parameters. This option is only supported by the OIDC extension.] |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-source"></a> [`quarkus.oidc-client.credentials.jwt.source`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-source) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.source+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.source` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.source+++[]<br><br>JWT token source: token is generated by the OIDC provider client or is an existing JWT bearer or SPIFFE SVID token.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SOURCE+++[] | tooltip:client[JWT token is generated by the OIDC provider client to support `client_secret_jwt` and `private_key_jwt` authentication methods.], tooltip:bearer[JWT bearer token is used as a client assertion\: https\://www.rfc-editor.org/rfc/rfc7523++#++section-2.2.], tooltip:spiffe-jwt[SPIFFE SVID token is used as a client assertion\: urn\:ietf\:params\:oauth\:client-assertion-type\:jwt-spiffe] | tooltip:client[JWT token is generated by the OIDC provider client to support `client_secret_jwt` and `private_key_jwt` authentication methods.] |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-token-path"></a> [`quarkus.oidc-client.credentials.jwt.token-path`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-token-path) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.token-path+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.token-path` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.token-path+++[]<br><br>Path to a file with a JWT bearer token that should be used as a client assertion. This path can only be set when JWT source (`source()`) is set to `Source#BEARER`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_TOKEN_PATH+++[] | path |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret"></a> [`quarkus.oidc-client.credentials.jwt.secret`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.secret+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.secret` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.secret+++[]<br><br>If provided, indicates that JWT is signed using a secret key. It is mutually exclusive with `key`, `key-file` and `key-store` properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SECRET+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret-provider-name"></a> [`quarkus.oidc-client.credentials.jwt.secret-provider.name`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret-provider-name) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.secret-provider.name+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.secret-provider.name` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.secret-provider.name+++[]<br><br>The CredentialsProvider bean name, which should only be set if more than one CredentialsProvider is registered<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SECRET_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret-provider-keyring-name"></a> [`quarkus.oidc-client.credentials.jwt.secret-provider.keyring-name`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret-provider-keyring-name) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.secret-provider.keyring-name+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.secret-provider.keyring-name` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.secret-provider.keyring-name+++[]<br><br>The CredentialsProvider keyring name. The keyring name is only required when the CredentialsProvider being used requires the keyring name to look up the secret, which is often the case when a CredentialsProvider is shared by multiple extensions to retrieve credentials from a more dynamic source like a vault instance or secret manager<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SECRET_PROVIDER_KEYRING_NAME+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret-provider-key"></a> [`quarkus.oidc-client.credentials.jwt.secret-provider.key`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-secret-provider-key) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.secret-provider.key+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.secret-provider.key` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.secret-provider.key+++[]<br><br>The CredentialsProvider client secret key<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SECRET_PROVIDER_KEY+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key"></a> [`quarkus.oidc-client.credentials.jwt.key`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.key+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.key` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.key+++[]<br><br>String representation of a private key. If provided, indicates that JWT is signed using a private key in PEM or JWK format. It is mutually exclusive with `secret`, `key-file` and `key-store` properties. You can use the `signature-algorithm` property to override the default key algorithm, `RS256`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEY+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-file"></a> [`quarkus.oidc-client.credentials.jwt.key-file`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-file) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.key-file+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.key-file` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.key-file+++[]<br><br>If provided, indicates that JWT is signed using a private key in PEM or JWK format. It is mutually exclusive with `secret`, `key` and `key-store` properties. You can use the `signature-algorithm` property to override the default key algorithm, `RS256`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEY_FILE+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-store-file"></a> [`quarkus.oidc-client.credentials.jwt.key-store-file`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-store-file) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.key-store-file+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.key-store-file` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.key-store-file+++[]<br><br>If provided, indicates that JWT is signed using a private key from a keystore. It is mutually exclusive with `secret`, `key` and `key-file` properties.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEY_STORE_FILE+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-store-password"></a> [`quarkus.oidc-client.credentials.jwt.key-store-password`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-store-password) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.key-store-password+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.key-store-password` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.key-store-password+++[]<br><br>A parameter to specify the password of the keystore file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEY_STORE_PASSWORD+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-id"></a> [`quarkus.oidc-client.credentials.jwt.key-id`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-id) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.key-id+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.key-id` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.key-id+++[]<br><br>The private key id or alias.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEY_ID+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-password"></a> [`quarkus.oidc-client.credentials.jwt.key-password`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-key-password) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.key-password+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.key-password` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.key-password+++[]<br><br>The private key password.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEY_PASSWORD+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-audience"></a> [`quarkus.oidc-client.credentials.jwt.audience`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-audience) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.audience+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.audience` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.audience+++[]<br><br>The JWT audience (`aud`) claim value. By default, the audience is set to the address of the OpenId Connect Provider's token endpoint.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_AUDIENCE+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-keep-audience-trailing-slash"></a> [`quarkus.oidc-client.credentials.jwt.keep-audience-trailing-slash`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-keep-audience-trailing-slash) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.keep-audience-trailing-slash+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.keep-audience-trailing-slash` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.keep-audience-trailing-slash+++[]<br><br>Whether to keep a trailing slash `/` in the `audience()` value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_KEEP_AUDIENCE_TRAILING_SLASH+++[] | boolean | `false` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-token-key-id"></a> [`quarkus.oidc-client.credentials.jwt.token-key-id`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-token-key-id) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.token-key-id+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.token-key-id` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.token-key-id+++[]<br><br>The key identifier of the signing key added as a JWT `kid` header.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_TOKEN_KEY_ID+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-issuer"></a> [`quarkus.oidc-client.credentials.jwt.issuer`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-issuer) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.issuer+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.issuer` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.issuer+++[]<br><br>The issuer of the signing key added as a JWT `iss` claim. The default value is the client id.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_ISSUER+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-subject"></a> [`quarkus.oidc-client.credentials.jwt.subject`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-subject) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.subject+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.subject` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.subject+++[]<br><br>Subject of the signing key added as a JWT `sub` claim The default value is the client id.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SUBJECT+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-claims-claim-name"></a> [`quarkus.oidc-client.credentials.jwt.claims."claim-name"`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-claims-claim-name) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.claims."claim-name"+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.claims."claim-name"` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.claims."claim-name"+++[]<br><br>Additional claims.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_CLAIMS__CLAIM_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-signature-algorithm"></a> [`quarkus.oidc-client.credentials.jwt.signature-algorithm`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-signature-algorithm) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.signature-algorithm+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.signature-algorithm` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.signature-algorithm+++[]<br><br>The signature algorithm used for the `key-file` property. Supported values: `RS256` (default), `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, `ES256`, `ES384`, `ES512`, `HS256`, `HS384`, `HS512`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_SIGNATURE_ALGORITHM+++[] | string |  |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-lifespan"></a> [`quarkus.oidc-client.credentials.jwt.lifespan`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-lifespan) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.lifespan+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.lifespan` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.lifespan+++[]<br><br>The JWT lifespan in seconds. This value is added to the time at which the JWT was issued to calculate the expiration time.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_LIFESPAN+++[] | int | `10` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-assertion"></a> [`quarkus.oidc-client.credentials.jwt.assertion`](#quarkus-oidc-client_quarkus-oidc-client-credentials-jwt-assertion) config_property_copy_button:+++quarkus.oidc-client.credentials.jwt.assertion+++[]<br><br>`quarkus.oidc-client."id".credentials.jwt.assertion` config_property_copy_button:+++quarkus.oidc-client."id".credentials.jwt.assertion+++[]<br><br>If true then the client authentication token is a JWT bearer grant assertion. Instead of producing 'client_assertion' and 'client_assertion_type' form properties, only 'assertion' is produced. This option is only supported by the OIDC client extension.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_JWT_ASSERTION+++[] | boolean | `false` |
| <a id="quarkus-oidc-client_quarkus-oidc-client-credentials-for-all-endpoints"></a> [`quarkus.oidc-client.credentials.for-all-endpoints`](#quarkus-oidc-client_quarkus-oidc-client-credentials-for-all-endpoints) config_property_copy_button:+++quarkus.oidc-client.credentials.for-all-endpoints+++[]<br><br>`quarkus.oidc-client."id".credentials.for-all-endpoints` config_property_copy_button:+++quarkus.oidc-client."id".credentials.for-all-endpoints+++[]<br><br>If true, client credentials are also sent to OIDC discovery and JWKS endpoints, which normally do not require authentication. Enable this when the OIDC provider enforces authenticated access to these endpoints.<br><br>One of the following authentication methods must be used when this property is enabled:<br><br>\* `client_secret_basic` — `Authorization: Basic` header \* JWT bearer (`jwt.source=bearer`) — `Authorization: Bearer` header<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_OIDC_CLIENT_CREDENTIALS_FOR_ALL_ENDPOINTS+++[] | boolean | `false` |

<a id="duration-note-anchor-quarkus-oidc-client_quarkus-oidc-client"></a>

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

### OIDC token propagation

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-enabled"></a> [`quarkus.rest-client-oidc-token-propagation.enabled`](#quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-enabled) config_property_copy_button:+++quarkus.rest-client-oidc-token-propagation.enabled+++[]<br><br>If the OIDC Token Reactive Propagation is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_OIDC_TOKEN_PROPAGATION_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-enabled-during-authentication"></a> [`quarkus.rest-client-oidc-token-propagation.enabled-during-authentication`](#quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-enabled-during-authentication) config_property_copy_button:+++quarkus.rest-client-oidc-token-propagation.enabled-during-authentication+++[]<br><br>Whether the token propagation is enabled during the `SecurityIdentity` augmentation.<br><br>For example, you may need to use a REST client from `SecurityIdentityAugmentor` to propagate the current token to acquire additional roles for the `SecurityIdentity`.<br><br>Note, this feature relies on a duplicated context. More information about Vert.x duplicated context can be found in [this guide](/guides/duplicated-context/index.md).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_OIDC_TOKEN_PROPAGATION_ENABLED_DURING_AUTHENTICATION+++[] | boolean | `false` |
| [lock] <a id="quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-exchange-token"></a> [`quarkus.rest-client-oidc-token-propagation.exchange-token`](#quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-exchange-token) config_property_copy_button:+++quarkus.rest-client-oidc-token-propagation.exchange-token+++[]<br><br>Exchange the current token with OpenId Connect Provider for a new token using either "urn:ietf:params:oauth:grant-type:token-exchange" or "urn:ietf:params:oauth:grant-type:jwt-bearer" token grant before propagating it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_OIDC_TOKEN_PROPAGATION_EXCHANGE_TOKEN+++[] | boolean | `false` |
| [lock] <a id="quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-client-name"></a> [`quarkus.rest-client-oidc-token-propagation.client-name`](#quarkus-rest-client-oidc-token-propagation_quarkus-rest-client-oidc-token-propagation-client-name) config_property_copy_button:+++quarkus.rest-client-oidc-token-propagation.client-name+++[]<br><br>Name of the configured OidcClient. Note this property is only used if the `exchangeToken` property is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REST_CLIENT_OIDC_TOKEN_PROPAGATION_CLIENT_NAME+++[] | string |  |

## References

- [OpenID Connect client and token propagation quickstart](/guides/security-openid-connect-client/index.md).
- [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md)
- [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md)
- [Quarkus Security overview](/guides/security-overview/index.md)
