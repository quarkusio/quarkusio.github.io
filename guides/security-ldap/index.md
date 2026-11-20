# Using security with an LDAP realm

This guide demonstrates how your Quarkus application can use an LDAP server to authenticate and authorize your user identities.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Architecture

In this example, we build a very simple microservice which offers three endpoints:

- `/api/public`
- `/api/users/me`
- `/api/admin`

The `/api/public` endpoint can be accessed anonymously. The `/api/admin` endpoint is protected with RBAC (Role-Based Access Control) where only users granted with the `adminRole` role can access. At this endpoint, we use the `@RolesAllowed` annotation to declaratively enforce the access constraint. The `/api/users/me` endpoint is also protected with RBAC (Role-Based Access Control) where only users granted with the `standardRole` role can access. As a response, it returns a JSON document with details about the user.

> [!WARNING]
> By default, Quarkus will restrict the use of JNDI within an application, as a precaution to try and mitigate any future vulnerabilities similar to Log4Shell. Because LDAP based auth requires JNDI this protection will be automatically disabled.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `security-ldap-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-ldap-quickstart).

## Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-ldap-quickstart \
    --extension='elytron-security-ldap,rest' \
    --no-code
cd security-ldap-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-ldap-quickstart \
    -Dextensions='elytron-security-ldap,rest' \
    -DnoCode
cd security-ldap-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-ldap-quickstart"`

This command generates a project, importing the `elytron-security-ldap` extension which is a `wildfly-elytron-realm-ldap` adapter for Quarkus applications.

If you already have your Quarkus project configured, you can add the `elytron-security-ldap` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add elytron-security-ldap
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='elytron-security-ldap'
```

**Gradle**

```bash
./gradlew addExtension --extensions='elytron-security-ldap'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elytron-security-ldap</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-elytron-security-ldap")
```

## Writing the application

Let's start by implementing the `/api/public` endpoint. As you can see from the source code below, it is just a regular Jakarta REST resource:

```java
package org.acme.elytron.security.ldap;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/public")
public class PublicResource {

    @GET
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String publicResource() {
        return "public";
   }
}
```

The source code for the `/api/admin` endpoint is also very simple. The main difference here is that we are using a `@RolesAllowed` annotation to make sure that only users granted with the `adminRole` role can access the endpoint:

```java
package org.acme.elytron.security.ldap;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/admin")
public class AdminResource {

    @GET
    @RolesAllowed("adminRole")
    @Produces(MediaType.TEXT_PLAIN)
    public String adminResource() {
         return "admin";
    }
}
```

Finally, let's consider the `/api/users/me` endpoint. As you can see from the source code below, we are trusting only users with the `standardRole` role. We are using `SecurityContext` to get access to the current authenticated Principal, and we return the user's name. This information is loaded from the LDAP server.

```java
package org.acme.elytron.security.ldap;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/users")
public class UserResource {

    @GET
    @RolesAllowed("standardRole")
    @Path("/me")
    public String me(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }
}
```

### Configuring the Application

```properties
quarkus.security.ldap.enabled=true

quarkus.security.ldap.dir-context.principal=uid=admin,ou=system
quarkus.security.ldap.dir-context.url=ldaps://ldap.server.local <1>
%test.quarkus.security.ldap.dir-context.url=ldap://127.0.0.1:10389 <2>
quarkus.security.ldap.dir-context.password=secret

quarkus.security.ldap.identity-mapping.rdn-identifier=uid
quarkus.security.ldap.identity-mapping.search-base-dn=ou=Users,dc=quarkus,dc=io

quarkus.security.ldap.identity-mapping.attribute-mappings."cn".from=cn
quarkus.security.ldap.identity-mapping.attribute-mappings."cn".filter=(member=uid={0},ou=Users,dc=quarkus,dc=io) <3>
quarkus.security.ldap.identity-mapping.attribute-mappings."cn".filter-base-dn=ou=Roles,dc=quarkus,dc=io
```

1. You need to provide the URL to an LDAP server. This example requires the LDAP server to have imported [this LDIF file](https://github.com/quarkusio/quarkus/blob/main/test-framework/ldap/src/main/resources/quarkus-io.ldif).
2. The URL used by our test resource. Tests may leverage `LdapServerTestResource` provided by Quarkus as [we do](https://github.com/quarkusio/quarkus-quickstarts/blob/main/security-ldap-quickstart/src/test/java/org/acme/elytron/security/ldap/ElytronLdapExtensionTestResources.java) in the test coverage of the example application.
3. `{0}` is substituted by the `uid`.

The `quarkus-elytron-security-ldap` extension requires a dir-context and an identity-mapping with at least one attribute-mapping to authenticate the user and its identity.

> [!TIP]
> By default, Quarkus doesn't cache the credentials obtained from the LDAP directory. Every request to your service will cause an additional roundtrip to the LDAP server.
>
> It is a common practice to cache these results to improve performance, but the tradeoff is that there will be a delay before the changes in the LDAP get effective in your service.
>
> To enable the cache, set `quarkus.security.ldap.cache.enabled=true` in your configuration file.
>
> The default cache max-age is `60s`. It can be configured by setting `quarkus.security.ldap.cache.max-age`.
>
> The number of cache entries is limited by `quarkus.security.ldap.cache.size`, which defaults to `100`.

### Map LDAP groups to `SecurityIdentity` roles

Previously described application configuration showed how to map `CN` attribute of the LDAP Distinguished Name group to a Quarkus `SecurityIdentity` role. More specifically, the `standardRole` CN was mapped to a `SecurityIdentity` role and thus allowed access to the `UserResource#me` endpoint. However, required `SecurityIdentity` roles may differ between applications and you may need to map LDAP groups to local `SecurityIdentity` roles like in the example below:

```properties
quarkus.http.auth.roles-mapping."standardRole"=user <1>
```

1. Map the `standardRole` role to the application-specific `SecurityIdentity` role `user`.

## Testing the Application

The application is now protected and the identities are provided by our LDAP server. Let's start the application in dev mode:

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

The very first thing to check is to ensure the anonymous access works.

```shell
$ curl -i -X GET http://localhost:8080/api/public
HTTP/1.1 200 OK
Content-Length: 6
Content-Type: text/plain;charset=UTF-8

public%
```

Now, let's try to hit a protected resource anonymously.

```shell
$ curl -i -X GET http://localhost:8080/api/admin
HTTP/1.1 401 Unauthorized
Content-Length: 14
Content-Type: text/html;charset=UTF-8

Not authorized%
```

So far so good, now let's try with an allowed user.

```shell
$ curl -i -X GET -u adminUser:adminUserPassword http://localhost:8080/api/admin
HTTP/1.1 200 OK
Content-Length: 5
Content-Type: text/plain;charset=UTF-8

admin%
```

By providing the `adminUser:adminUserPassword` credentials, the extension authenticated the user and loaded their roles. The `adminUser` user is authorized to access to the protected resources.

The user `adminUser` should be forbidden to access a resource protected with `@RolesAllowed("standardRole")` because it doesn't have this role.

```shell
$ curl -i -X GET -u adminUser:adminUserPassword http://localhost:8080/api/users/me
HTTP/1.1 403 Forbidden
Content-Length: 34
Content-Type: text/html;charset=UTF-8

Forbidden%
```

Finally, using the user `standardUser` works and the security context contains the principal details (username for instance).

```shell
$ curl -i -X GET -u standardUser:standardUserPassword http://localhost:8080/api/users/me
HTTP/1.1 200 OK
Content-Length: 4
Content-Type: text/plain;charset=UTF-8

user%
```

<a id="configuration-reference"></a>
## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-enabled"></a> [`quarkus.security.ldap.enabled`](#quarkus-elytron-security-ldap_quarkus-security-ldap-enabled) config_property_copy_button:+++quarkus.security.ldap.enabled+++[]<br><br>The option to enable the ldap elytron module<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_ENABLED+++[] | boolean | `false` |
| [lock] <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-realm-name"></a> [`quarkus.security.ldap.realm-name`](#quarkus-elytron-security-ldap_quarkus-security-ldap-realm-name) config_property_copy_button:+++quarkus.security.ldap.realm-name+++[]<br><br>The elytron realm name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_REALM_NAME+++[] | string | `Quarkus` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-direct-verification"></a> [`quarkus.security.ldap.direct-verification`](#quarkus-elytron-security-ldap_quarkus-security-ldap-direct-verification) config_property_copy_button:+++quarkus.security.ldap.direct-verification+++[]<br><br>Provided credentials are verified against ldap?<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIRECT_VERIFICATION+++[] | boolean | `true` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-url"></a> [`quarkus.security.ldap.dir-context.url`](#quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-url) config_property_copy_button:+++quarkus.security.ldap.dir-context.url+++[]<br><br>The url of the ldap server<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIR_CONTEXT_URL+++[] | string | required [exclamation circle] |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-principal"></a> [`quarkus.security.ldap.dir-context.principal`](#quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-principal) config_property_copy_button:+++quarkus.security.ldap.dir-context.principal+++[]<br><br>The principal: user which is used to connect to ldap server (also named "bindDn")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIR_CONTEXT_PRINCIPAL+++[] | string |  |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-password"></a> [`quarkus.security.ldap.dir-context.password`](#quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-password) config_property_copy_button:+++quarkus.security.ldap.dir-context.password+++[]<br><br>The password which belongs to the principal (also named "bindCredential")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIR_CONTEXT_PASSWORD+++[] | string |  |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-referral-mode"></a> [`quarkus.security.ldap.dir-context.referral-mode`](#quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-referral-mode) config_property_copy_button:+++quarkus.security.ldap.dir-context.referral-mode+++[]<br><br>how ldap redirects are handled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIR_CONTEXT_REFERRAL_MODE+++[] | `ignore`, `follow`, `throw` | `ignore` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-connect-timeout"></a> [`quarkus.security.ldap.dir-context.connect-timeout`](#quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-connect-timeout) config_property_copy_button:+++quarkus.security.ldap.dir-context.connect-timeout+++[]<br><br>The connect timeout<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIR_CONTEXT_CONNECT_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elytron-security-ldap_quarkus-security) | `5S` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-read-timeout"></a> [`quarkus.security.ldap.dir-context.read-timeout`](#quarkus-elytron-security-ldap_quarkus-security-ldap-dir-context-read-timeout) config_property_copy_button:+++quarkus.security.ldap.dir-context.read-timeout+++[]<br><br>The read timeout<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_DIR_CONTEXT_READ_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elytron-security-ldap_quarkus-security) | `60S` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-cache-enabled"></a> [`quarkus.security.ldap.cache.enabled`](#quarkus-elytron-security-ldap_quarkus-security-ldap-cache-enabled) config_property_copy_button:+++quarkus.security.ldap.cache.enabled+++[]<br><br>If set to true, request to the LDAP server are cached<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_CACHE_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-cache-max-age"></a> [`quarkus.security.ldap.cache.max-age`](#quarkus-elytron-security-ldap_quarkus-security-ldap-cache-max-age) config_property_copy_button:+++quarkus.security.ldap.cache.max-age+++[]<br><br>The duration that an entry can stay in the cache<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_CACHE_MAX_AGE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-elytron-security-ldap_quarkus-security) | `60S` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-cache-size"></a> [`quarkus.security.ldap.cache.size`](#quarkus-elytron-security-ldap_quarkus-security-ldap-cache-size) config_property_copy_button:+++quarkus.security.ldap.cache.size+++[]<br><br>The maximum number of entries to keep in the cache<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_CACHE_SIZE+++[] | int | `100` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-rdn-identifier"></a> [`quarkus.security.ldap.identity-mapping.rdn-identifier`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-rdn-identifier) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.rdn-identifier+++[]<br><br>The identifier which correlates to the provided user (also named "baseFilter")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_RDN_IDENTIFIER+++[] | string | `uid` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-search-base-dn"></a> [`quarkus.security.ldap.identity-mapping.search-base-dn`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-search-base-dn) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.search-base-dn+++[]<br><br>The dn where we look for users<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_SEARCH_BASE_DN+++[] | string | required [exclamation circle] |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-search-recursive"></a> [`quarkus.security.ldap.identity-mapping.search-recursive`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-search-recursive) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.search-recursive+++[]<br><br>If the child nodes are also searched for identities<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_SEARCH_RECURSIVE+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-from"></a> [`quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".from`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-from) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".from+++[]<br><br>The roleAttributeId from which is mapped (e.g. "cn")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__FROM+++[] | string | required [exclamation circle] |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-to"></a> [`quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".to`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-to) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".to+++[]<br><br>The identifier whom the attribute is mapped to (in Quarkus: "groups", in WildFly this is "Roles")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__TO+++[] | string | `groups` |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-filter"></a> [`quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".filter`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-filter) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".filter+++[]<br><br>The filter (also named "roleFilter")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__FILTER+++[] | string | required [exclamation circle] |
| <a id="quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-filter-base-dn"></a> [`quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".filter-base-dn`](#quarkus-elytron-security-ldap_quarkus-security-ldap-identity-mapping-attribute-mappings-attribute-mappings-filter-base-dn) config_property_copy_button:+++quarkus.security.ldap.identity-mapping.attribute-mappings."attribute-mappings".filter-base-dn+++[]<br><br>The filter base dn (also named "rolesContextDn")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_LDAP_IDENTITY_MAPPING_ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__FILTER_BASE_DN+++[] | string | required [exclamation circle] |

<a id="duration-note-anchor-quarkus-elytron-security-ldap_quarkus-security"></a>

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

- [LDAP](https://en.wikipedia.org/wiki/Lightweight_Directory_Access_Protocol)
- [Quarkus Security overview](/guides/security-overview/index.md)
