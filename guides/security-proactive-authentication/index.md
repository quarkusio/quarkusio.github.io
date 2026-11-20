# Proactive authentication

Learn how to manage proactive authentication in Quarkus, including customizing settings and handling exceptions. Gain practical insights and strategies for various application scenarios.

Proactive authentication is enabled in Quarkus by default. It ensures that all incoming requests with credentials are authenticated, even if the target page does not require authentication. As a result, requests with invalid credentials are rejected, even if the target page is public. Requests without credentials are not rejected, because anonymous requests are allowed.

You can turn off this default behavior if you want to authenticate only when the target page requires it. To turn off proactive authentication so that authentication occurs only when the target page requires it, modify the `application.properties` configuration file as follows:

```xml
quarkus.http.auth.proactive=false
```

If you turn off proactive authentication, the authentication process runs only when an identity is requested. An identity can be requested because of security rules that require the user to authenticate or because programmatic access to the current identity is required.

If proactive authentication is not used, accessing `SecurityIdentity` is a blocking operation. This is because authentication might have yet to happen, and accessing `SecurityIdentity` might require calls to external systems, such as databases, that might block the operation. For blocking applications, this is not an issue. However, if you have disabled authentication in a reactive application, this fails because you cannot do blocking operations on the I/O thread. To work around this, you need to `@Inject` an instance of `io.quarkus.security.identity.CurrentIdentityAssociation` and call the `Uni<SecurityIdentity> getDeferredIdentity();` method. Then, you can subscribe to the resulting `Uni` to be notified when authentication is complete and the identity is available.

> [!NOTE]
> You can still access `SecurityIdentity` synchronously with `public SecurityIdentity getIdentity()` in [Quarkus REST (formerly RESTEasy Reactive)](/guides/rest/index.md) from endpoints that are annotated with `@RolesAllowed`, `@Authenticated`, or with respective configuration authorization checks because authentication has already happened. The same is also valid for [Reactive routes](/guides/reactive-routes/index.md) if a route response is synchronous.

When proactive authentication is disabled, [standard security annotations](/guides/security-authorize-web-endpoints-reference/index.md#standard-security-annotations) used on CDI beans do not function on an I/O thread if a secured method that is not void synchronously returns a value. This limitation arises from the necessity for these methods to access `SecurityIdentity`.

The following example defines `HelloResource` and `HelloService`. Any GET request to `/hello` runs on the I/O thread and throws a `BlockingOperationNotAllowedException` exception.

There is more than one way to fix the example:

- Switch to a worker thread by annotating the `hello` endpoint with `@Blocking`.
- Change the `sayHello` method return type by using a reactive or asynchronous data type.
- Move the `@RolesAllowed` annotation to the endpoint. This could be one of the safest ways because accessing `SecurityIdentity` from endpoint methods is never the blocking operation.

```java
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.smallrye.mutiny.Uni;

@Path("/hello")
@PermitAll
public class HelloResource {

    @Inject
    HelloService helloService;

    @GET
    public Uni<String> hello() {
        return Uni.createFrom().item(helloService.sayHello());
    }

}
```

```java
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloService {

    @RolesAllowed("admin")
    public String sayHello() {
        return "Hello";
    }

}
```

<a id="cdi-request-context-activation"></a>
## Activating the CDI request context

You may need to inject `@RequestScoped` beans during authentication and authorization. A good example of this is accessing a database during a `SecurityIdentity` augmentation, which is described in the [Security Identity Customization](/guides/security-customization/index.md#security-identity-customization) section of the "Security Tips and Tricks" guide. If authentication or authorization fails with the `jakarta.enterprise.context.ContextNotActiveException`, disabling proactive authentication is most often the best solution. Users can also activate [CDI request context](/guides/cdi-reference/index.md#request-context-lifecycle), for example, by using the `@ActivateRequestContext` annotation. However, some CDI beans may not be ready for use.

One exception to this solution is a situation when application endpoints are secured with the [Authorization using configuration](/guides/security-authorize-web-endpoints-reference/index.md#authorization-using-configuration). For more information, see the [Inject RequestScoped beans into HttpSecurityPolicy](/guides/security-authorize-web-endpoints-reference/index.md#policy-active-cdi-request-context) section of the "Authorization of Web endpoints" guide for more information.

<a id="customize-auth-exception-responses"></a>
## Customize authentication exception responses

You can use Jakarta REST `ExceptionMapper` to capture Quarkus Security authentication exceptions such as `io.quarkus.security.AuthenticationFailedException`. For example:

```java
package io.quarkus.it.keycloak;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationFailedException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        return Response.status(401).header("WWW-Authenticate", "Basic realm=\"Quarkus\"").build();
    }
}
```

> [!CAUTION]
> Some HTTP authentication mechanisms must handle authentication exceptions themselves to create a correct authentication challenge. For example, `io.quarkus.oidc.runtime.CodeAuthenticationMechanism`, which manages OpenID Connect (OIDC) authorization code flow authentication, must build a correct redirect URL and set a state cookie. Therefore, avoid using custom exception mappers to customize authentication exceptions thrown by such mechanisms. Instead, a safer approach is to ensure that proactive authentication is enabled and to use Vert.x HTTP route failure handlers. This is because events come to the handler with the correct response status and headers. Then, you must only customize the response; for example:

```java
package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.security.AuthenticationFailedException;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class AuthenticationFailedExceptionHandler {

    public void init(@Observes Router router) {
        router.route().failureHandler(new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (event.failure() instanceof AuthenticationFailedException) {
                    event.response().end("CUSTOMIZED_RESPONSE");
                } else {
                    event.next();
                }
            }
        });
    }
}
```

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
- [Quarkus Security architecture](/guides/security-architecture/index.md)
- [Authentication mechanisms in Quarkus](/guides/security-authentication-mechanisms/index.md)
- [Identity providers](/guides/security-identity-providers/index.md)
