---
date: 2021-08-31
---
## SmallRye Config

SmallRye Config `@ConfigMapping` can now only be used on interfaces. Using it on classes results in a build-time exception.

## Kubernetes Client

The Kubernetes Client has been upgraded to 5.7, which contains some breaking changes.

Please refer to [the release notes](https://github.com/fabric8io/kubernetes-client/releases/tag/v5.7.0) for more details.

## Deprecated method in OIDC TenantConfigResolver

`OidcTenantConfig io.quarkus.oidc.TenantConfigResolver#resolve(RoutingContext)` method has been deprecated and will be removed soon.

Please use `Uni<OidcTenantConfig> io.quarkus.oidc.TenantConfigResolver#resolve(RoutingContext, TenantConfigResolver.TenantConfigRequestContext)` instead, where `TenantConfigResolver.TenantConfigRequestContext` can be used to run the blocking tasks.