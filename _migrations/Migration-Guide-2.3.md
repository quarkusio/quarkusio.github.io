---
date: 2021-10-06
---
## Dev Mode Working Directory

The working directory for dev mode has been changed from the build directory (`target/`) to the root of the project.
This makes the execution of the tests with continuous testing consistent with the tests executed via Surefire.

To restore the previous behavior, you can define `<workingDir>${project.build.directory}</workingDir>` in the Quarkus Maven plugin configuration.

## gRPC Server Interceptors
Global gRPC server interceptors have to be annotated with `@io.quarkus.grpc.GlobalInterceptor` since 2.3.

Prior to Quarkus 2.3, all the CDI beans implementing `io.grpc.ServerInterceptor` where considered global interceptors, i.e. applied to all gRPC services. 
Since 2.3, it is possible to make a service-specific interceptor, by adding `@io.quarkus.grpc.RegisterInterceptor(MyInterceptor.class)` on a gRPC service implementation. To easily distinguish between per-service interceptors and global ones, the aforementioned `@GlobalInterceptor` annotation has been introduced.

## CDI `@PreDestroy` and `@PostConstruct` methods on bean classes
Fixed a bug where `@PreDestroy` and `@PostConstruct` methods declared on bean classes were considered business methods which violated [CDI specification rules](https://jakarta.ee/specifications/cdi/2.0/cdi-spec-2.0.html#biz_method). Therefore, starting with 2.3, an invocation of these methods will no longer be intercepted by any `@AroundInvoke` interceptors declared on the bean class and/or on these lifecycle methods.

This means that if you previously leveraged this scenario (for example to apply `@Transactional` to post construct callback), you will need to change your code. Here is an example of an approach that will still work:

```
@Dependent
class MyBean {
   @PostConstruct
   void init() {
     doInit(); // Interception works for self-invocation
   }
   
   @Transactional
   void doInit() {
     // ...e.g. save something in DB
   }
}
```

## Deprecated methods in OIDC TokenStateManager

`io.quarkus.oidc.TokenStateManager` has had its `String createTokenState(RoutingContext, OidcTenantConfig, AuthorizationCodeTokens)`, `AuthorizationCodeTokens getTokens(RoutingContext, OidcTenantConfig, String)` and `void deleteTokens(RoutingContext, OidcTenantConfig, String)` methods deprecated and these methods will be removed soon.

Please use `Uni<String> createTokenState(RoutingContext, OidcTenantConfig, AuthorizationCodeTokens, CreateTokenStateRequestContext)`, `Uni<AuthorizationCodeTokens> getTokens(RoutingContext, OidcTenantConfig, String, GetTokensRequestContext)` and `Uni<Void> deleteTokens(RoutingContext, OidcTenantConfig, String, DeleteTokensRequestContext)` instead, where the request contexts can be used to run the blocking tasks.

## Reactive Routes

The extension artifact id was changed from `quarkus-vertx-web` to `quarkus-reactive-routes`, i.e. you should add the following dependency in your `pom.xml`:

```
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-reactive-routes</artifactId>
</dependency>
```

NOTE: The original artifact `io.quarkus:quarkus-vertx-web` is automatically relocated to this new dependency. However, users are encouraged to update the dependency as soon as possible.

## New `@PersistenceUnitExtension` qualifier for `TenantResolver`/`TenantConnectionResolver`

Multi-tenancy support in Hibernate ORM relies on user-provided `TenantResolver` beans, and optionally `TenantConnectionResolver` beans.

Starting with Quarkus 2.3, you should always use the `@PersistenceUnitExtension` CDI qualifier for such bean definitions, even when targeting the default persistence unit, and you should never use the `@PersistenceUnit` CDI qualifier for such bean definitions.

In short, add a `@PersistenceUnitExtension` annotation for `TenantResolver` or `TenantConnectionResolver` beans targeting the default persistence unit:

```diff
+ @PersistenceUnitExtension
  @RequestScoped
  public class CustomTenantResolver implements TenantResolver {
      @Override
      public String resolveTenantId() {
          // ...
      }
  }
```

And replace the `@PersistenceUnit` annotation with a `@PersistenceUnitExtension` annotation for `TenantResolver` or `TenantConnectionResolver` beans targeting a named persistence unit:

```diff
- @PersistenceUnit("nameOfMyPersistenceUnit")
+ @PersistenceUnitExtension("nameOfMyPersistenceUnit")
  @RequestScoped
  public class CustomTenantResolver implements TenantResolver {
      @Override
      public String resolveTenantId() {
          // ...
      }
  }
```

Old bean definitions with the old qualifiers will continue to work for now, but such support will be removed in a later version of Quarkus.

Note that `@PersistenceUnit` is still a valid annotation for injecting `EntityManager`/`EntityManagerFactory` in your components; it just shouldn't be used as a CDI qualifier for `TenantResolver`/`TenantConnectionResolver` beans.

## Change of Default Key Encryption Algorithm for quarkus-smallrye-jwt-build

Please note that the default key encryption algorithm for `quarkus-smallrye-jwt-build` has been changed from `RSA-OAEP-256` to `RSA-OAEP` to align with the `MP JWT 1.2` specification.

If you need to continue using `RSA-OAEP-256` for encrypting the key encryption keys then set it like this:

```java
// Encrypt the token
Jwt.upn("some identifier").jwe().keyAlgorithm(KeyEncryptionAlgorithm.RSA_OAEP_256).encrypt();
```

or

```java
// Inner Sign and Encrypt the token
Jwt.upn("some identifier").innerSign().keyAlgorithm(KeyEncryptionAlgorithm.RSA_OAEP_256).encrypt();
```