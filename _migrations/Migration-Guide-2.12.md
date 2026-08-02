---
date: 2022-08-31
---
## `quarkus.hibernate-search-orm.enabled` was renamed to `quarkus.hibernate-search-orm.active`

If you were previously using the configuration property `quarkus.hibernate-search-orm.enabled` to enable/disable Hibernate Search at runtime (by setting the config in deployment configuration files, environment variables or commandline parameters), then you should use the configuration property `quarkus.hibernate-search-orm.active` instead.

`quarkus.hibernate-search-orm.enabled` is now aligned with Quarkus conventions and disables the extension at *build time*, which cannot be reverted at runtime.

## Gradle 7.5.1

Quarkus now requires Gradle 7.5.1. If your project is using Gradle Wrapper, make sure to [update it](https://docs.gradle.org/7.5.1/userguide/gradle_wrapper.html#sec:upgrading_wrapper).
This upgrade should solve build issues such as out of memory and non-existent tasks that have been seen with previous Gradle versions.

## Path specific authentication for OIDC `web-app` applications

If your application combines multiple authentication mechanisms including OIDC then please use `quarkus.http.auth.permission.<policy-name>.auth-mechanism=code` instead of `quarkus.http.auth.permission.<policy-name>.auth-mechanism=bearer` if you work with OIDC `web-app` applications and would like to configure an HTTP security policy to use only the OIDC authorization code flow mechanism, for example:

```
quarkus.http.auth.permission.bearer.paths=/web-app
quarkus.http.auth.permission.bearer.policy=authenticated
quarkus.http.auth.permission.bearer.auth-mechanism=code
```