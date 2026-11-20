# Using Keycloak Admin Client

The Quarkus Keycloak Admin Client and its reactive twin support Keycloak Admin Client which can be used to configure a running Keycloak server.

This guide demonstrates how you can leverage the [Quarkus ArC](/guides/cdi-reference/index.md) and inject the admin client to your Quarkus application, as well as how to create it directly in the application code.

To learn more about the Keycloak Admin Client, please refer to its [reference guide](https://www.keycloak.org/docs/latest/server_development/#example-using-java).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- [Keycloak](https://www.keycloak.org)

## Creating the Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-keycloak-admin-client \
    --extension='keycloak-admin-rest-client,rest-jackson' \
    --no-code
cd security-keycloak-admin-client
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-keycloak-admin-client \
    -Dextensions='keycloak-admin-rest-client,rest-jackson' \
    -DnoCode
cd security-keycloak-admin-client
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-keycloak-admin-client"`

This command generates a project which imports the `keycloak-admin-rest-client` and `rest-jackson` extensions.

If you already have your Quarkus project configured, you can add the `keycloak-admin-rest-client` and `rest-jackson` extensions to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add keycloak-admin-rest-client,rest-jackson
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='keycloak-admin-rest-client,rest-jackson'
```

**Gradle**

```bash
./gradlew addExtension --extensions='keycloak-admin-rest-client,rest-jackson'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-keycloak-admin-rest-client</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-keycloak-admin-rest-client")
implementation("io.quarkus:quarkus-rest-jackson")
```

We also are going to need a simple resource with a `Keycloak` injected as request scoped CDI bean.

```java
package org.acme.keycloak.admin.client;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

@Path("/api/admin")
public class RolesResource {

        @Inject
        Keycloak keycloak; <1>

        @GET
        @Path("/roles")
        public List<RoleRepresentation> getRoles() {
            return keycloak.realm("quarkus").roles().list();
        }

}
```

1. Create  a default Keycloak Admin Client which can perform Keycloak `master` realm administration tasks as an `admin-cli` client, such as adding new realms, clients and users.

The only configuration which is required to create this Keycloak Admin Client is a Keycloak server URL.

For example:

```properties
# Quarkus based Keycloak distribution
quarkus.keycloak.admin-client.server-url=http://localhost:8081
```

or

```properties
# WildFly based Keycloak distribution
quarkus.keycloak.admin-client.server-url=http://localhost:8081/auth
```

> [!NOTE]
> It is important that `quarkus.keycloak.admin-client.server-url` is configured if you would like to have `Keycloak` injected. The injection will fail if you attempt to inject `Keycloak` without configuring this property.

Injecting Keycloak Admin Client instead of creating it directly in the application code is a simpler and more flexible option but you can create the same admin client manually if necessary:

```java
package org.acme.keycloak.admin.client;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.RoleRepresentation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

@Path("/api/admin")
public class RolesResource {

        Keycloak keycloak;

        @PostConstruct
        public void initKeycloak() {
            keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8081")
                .realm("master")
                .clientId("admin-cli")
                .grantType("password")
                .username("admin")
                .password("admin")
                .build();
        }

        @PreDestroy
        public void closeKeycloak() {
            keycloak.close();
        }

        @GET
        @Path("/roles")
        public List<RoleRepresentation> getRoles() {
            return keycloak.realm("quarkus").roles().list();
        }

}
```

For more details consult [Keycloak Admin REST API documentation](https://www.keycloak.org/docs/latest/server_development/#admin-rest-api).

You can configure Keycloak Admin Client to administer other realms and clients. It can use either a `password` or `client_credentials` grant to acquire an access token to call the Admin REST API which requires authorization.

If you exchange user's credentials for the access token, here is an example configuration for the `password` grant type:

```properties
quarkus.keycloak.admin-client.server-url=http://localhost:8081
quarkus.keycloak.admin-client.realm=quarkus
quarkus.keycloak.admin-client.client-id=quarkus-client
quarkus.keycloak.admin-client.username=alice
quarkus.keycloak.admin-client.password=alice
quarkus.keycloak.admin-client.grant-type=PASSWORD <1>
```

1. Use `password` grant type.

An example using the `client-credentials` grant type needs only a minor adjustments:

```properties
quarkus.keycloak.admin-client.enabled=true
quarkus.keycloak.admin-client.server-url=http://localhost:8081
quarkus.keycloak.admin-client.realm=quarkus
quarkus.keycloak.admin-client.client-id=quarkus-client
quarkus.keycloak.admin-client.client-secret=secret
quarkus.keycloak.admin-client.username= # remove default username
quarkus.keycloak.admin-client.password= # remove default password
quarkus.keycloak.admin-client.grant-type=CLIENT_CREDENTIALS <1>
```

1. Use `client_credentials` grant type.

> [!NOTE]
> Note that the [OidcClient](/guides/security-openid-connect-client/index.md) can also be used to acquire tokens.

## Configuring TLS

To configure a TLS connection for the Keycloak Admin Client, use the TLS Registry extension and point the Keycloak Admin Client to respective TLS configuration. For example, you can configure mutual TLS (mTLS) like this:

```properties
quarkus.keycloak.admin-client.tls-configuration-name=kc-mtls
quarkus.tls.kc-mtls.key-store.p12.path=client-keystore.p12
quarkus.tls.kc-mtls.key-store.p12.password=secret
quarkus.tls.kc-mtls.trust-store.p12.path=client-truststore.p12
quarkus.tls.kc-mtls.trust-store.p12.password=secret
```

## Testing

The preferred approach for testing Keycloak Admin Client against Keycloak is [Dev Services for Keycloak](/guides/security-openid-connect-dev-services/index.md). `Dev Services for Keycloak` will start and initialize a test container. Then, it will create a `quarkus` realm and a `quarkus-app` client (`secret` secret) and add `alice` (`admin` and `user` roles) and `bob` (`user` role) users, where all of these properties can be customized.

For example, by default, a test container will be available at a randomly allocated port but you can make both Keycloak Admin Client and the container use the same port as follows:

```properties
%test.quarkus.keycloak.devservices.port=${kc.admin.port.test:45180} <1>
%test.quarkus.keycloak.admin-client.server-url=http://localhost:${kc.admin.port.test:45180}/ <2>
```

1. Configure the Keycloak container to listen on the `45180` port by default
2. Configure the Keycloak Admin Client to use the same port

<a id="keycloak-admin-client-configuration-reference"></a>
## Quarkus Keycloak Admin Client Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-enabled"></a> [`quarkus.keycloak.admin-client.enabled`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-enabled) config_property_copy_button:+++quarkus.keycloak.admin-client.enabled+++[]<br><br>Set to true if Keycloak Admin Client injection is supported.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_ENABLED+++[] | boolean | `true` |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-server-url"></a> [`quarkus.keycloak.admin-client.server-url`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-server-url) config_property_copy_button:+++quarkus.keycloak.admin-client.server-url+++[]<br><br>Keycloak server URL, for example, `https://host:port`. When the Keycloak Dev Services is started and this property is not configured, Quarkus points the 'quarkus.keycloak.admin-client.server-url' configuration property to started Keycloak container. In other cases, when this property is not set then the Keycloak Admin Client injection will fail - use `org.keycloak.admin.client.KeycloakBuilder` to create the client instead.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_SERVER_URL+++[] | string |  |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-realm"></a> [`quarkus.keycloak.admin-client.realm`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-realm) config_property_copy_button:+++quarkus.keycloak.admin-client.realm+++[]<br><br>Realm.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_REALM+++[] | string | `master` |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-client-id"></a> [`quarkus.keycloak.admin-client.client-id`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-client-id) config_property_copy_button:+++quarkus.keycloak.admin-client.client-id+++[]<br><br>Client id.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_CLIENT_ID+++[] | string | `admin-cli` |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-client-secret"></a> [`quarkus.keycloak.admin-client.client-secret`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-client-secret) config_property_copy_button:+++quarkus.keycloak.admin-client.client-secret+++[]<br><br>Client secret. Required with a `client_credentials` grant type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_CLIENT_SECRET+++[] | string |  |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-username"></a> [`quarkus.keycloak.admin-client.username`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-username) config_property_copy_button:+++quarkus.keycloak.admin-client.username+++[]<br><br>Username. Required with a `password` grant type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_USERNAME+++[] | string | `admin` |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-password"></a> [`quarkus.keycloak.admin-client.password`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-password) config_property_copy_button:+++quarkus.keycloak.admin-client.password+++[]<br><br>Password. Required with a `password` grant type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_PASSWORD+++[] | string | `admin` |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-scope"></a> [`quarkus.keycloak.admin-client.scope`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-scope) config_property_copy_button:+++quarkus.keycloak.admin-client.scope+++[]<br><br>OAuth 2.0 [Access Token Scope](https://datatracker.ietf.org/doc/html/rfc6749#section-3.3).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_SCOPE+++[] | string |  |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-grant-type"></a> [`quarkus.keycloak.admin-client.grant-type`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-grant-type) config_property_copy_button:+++quarkus.keycloak.admin-client.grant-type+++[]<br><br>OAuth Grant Type.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_GRANT_TYPE+++[] | `password`, `client-credentials` | `password` |
| <a id="quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-tls-configuration-name"></a> [`quarkus.keycloak.admin-client.tls-configuration-name`](#quarkus-keycloak-admin-client_quarkus-keycloak-admin-client-tls-configuration-name) config_property_copy_button:+++quarkus.keycloak.admin-client.tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_KEYCLOAK_ADMIN_CLIENT_TLS_CONFIGURATION_NAME+++[] | string |  |

## References

- [Keycloak Documentation](https://www.keycloak.org/documentation.html)
- [Keycloak Authorization extension](/guides/security-keycloak-authorization/index.md)
- [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md)
- [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md)
- [OpenID Connect Client and Token Propagation Quickstart](/guides/security-openid-connect-client/index.md)
- [Quarkus Security overview](/guides/security-overview/index.md)
