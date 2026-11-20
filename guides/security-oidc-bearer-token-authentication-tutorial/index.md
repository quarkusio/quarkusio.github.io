# Protect a service application by using OpenID Connect (OIDC) Bearer token authentication

Use the Quarkus OpenID Connect (OIDC) extension to secure a Jakarta REST application with Bearer token authentication. The bearer tokens are issued by OIDC and OAuth 2.0 compliant authorization servers, such as [Keycloak](https://www.keycloak.org).

For more information about OIDC Bearer token authentication, see the Quarkus [OpenID Connect (OIDC) Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md) guide.

If you want to protect web applications by using OIDC Authorization Code Flow authentication, see the [OpenID Connect authorization code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md) guide.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- A working container runtime (Docker or [Podman](/guides/podman/index.md))
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

- The [jq command-line processor tool](https://stedolan.github.io/jq/)

## Architecture

This example shows how you can build a simple microservice that offers two endpoints:

- `/api/users/me`
- `/api/admin`

These endpoints are protected and can only be accessed if a client sends a bearer token along with the request, which must be valid (for example, signature, expiration, and audience) and trusted by the microservice.

A Keycloak server issues the bearer token and represents the subject for which the token was issued. Because it is an OAuth 2.0 authorization server, the token also references the client acting on the user's behalf.

Any user with a valid token can access the `/api/users/me` endpoint. As a response, it returns a JSON document with user details obtained from the information in the token.

The `/api/admin` endpoint is protected with RBAC (Role-Based Access Control), which only users with the `admin` role can access. At this endpoint, the `@RolesAllowed` annotation is used to enforce the access constraint declaratively.

## Solution

Follow the instructions in the next sections and create the application step by step. You can also go straight to the completed example.

You can clone the Git repository by running the command `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or you can download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `security-openid-connect-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-quickstart).

## Create the Maven project

You can either create a new Maven project with the `oidc` extension or you can add the extension to an existing Maven project. Complete one of the following commands:

To create a new Maven project, use the following command:

**CLI**

```bash
quarkus create app org.acme:security-openid-connect-quickstart \
    --extension='oidc,rest-jackson' \
    --no-code
cd security-openid-connect-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-openid-connect-quickstart \
    -Dextensions='oidc,rest-jackson' \
    -DnoCode
cd security-openid-connect-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-openid-connect-quickstart"`

If you already have your Quarkus project configured, you can add the `oidc` extension to your project by running the following command in your project base directory:

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

This will add the following to your build file:

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

## Write the application

1. Implement the `/api/users/me` endpoint as shown in the following example, which is a regular Jakarta REST resource:

   ```java
   package org.acme.security.openid.connect;

   import jakarta.annotation.security.RolesAllowed;
   import jakarta.inject.Inject;
   import jakarta.ws.rs.GET;
   import jakarta.ws.rs.Path;

   import org.jboss.resteasy.reactive.NoCache;
   import io.quarkus.security.identity.SecurityIdentity;

   @Path("/api/users")
   public class UsersResource {

       @Inject
       SecurityIdentity securityIdentity;

       @GET
       @Path("/me")
       @RolesAllowed("user")
       @NoCache
       public User me() {
           return new User(securityIdentity);
       }

       public static class User {

           private final String userName;

           User(SecurityIdentity securityIdentity) {
               this.userName = securityIdentity.getPrincipal().getName();
           }

           public String getUserName() {
               return userName;
           }
       }
   }
   ```

   1. Implement the `/api/admin` endpoint as shown in the following example:

   ```java
   package org.acme.security.openid.connect;

   import jakarta.annotation.security.RolesAllowed;
   import jakarta.ws.rs.GET;
   import jakarta.ws.rs.Path;
   import jakarta.ws.rs.Produces;
   import jakarta.ws.rs.core.MediaType;

   @Path("/api/admin")
   public class AdminResource {

       @GET
       @RolesAllowed("admin")
       @Produces(MediaType.TEXT_PLAIN)
       public String admin() {
           return "granted";
       }
   }
   ```

   > [!NOTE]
   > The main difference in this example is that the `@RolesAllowed` annotation is used to verify that only users granted the `admin` role can access the endpoint.

   Injection of the `SecurityIdentity` is supported in both `@RequestScoped` and `@ApplicationScoped` contexts.

   ## Configure the application

   - Configure the Quarkus OpenID Connect (OIDC) extension by setting the following configuration properties in the `src/main/resources/application.properties` file.

   ```properties
   %prod.quarkus.oidc.auth-server-url=http://localhost:8180/realms/quarkus
   quarkus.oidc.client-id=backend-service
   quarkus.oidc.credentials.secret=secret

   # Tell Dev Services for Keycloak to import the realm file
   # This property is not effective when running the application in JVM or native modes

   quarkus.keycloak.devservices.realm-path=quarkus-realm.json
   ```

   Where:

   - `%prod.quarkus.oidc.auth-server-url` sets the base URL of the OpenID Connect (OIDC) server. The `%prod.` profile prefix ensures that `Dev Services for Keycloak` launches a container when you run the application in development (dev) mode. For more information, see the [Run the application in dev mode](#bearer-token-tutorial-keycloak-dev-mode) section.

   - `quarkus.oidc.client-id` sets a client id that identifies the application.
   - `quarkus.oidc.credentials.secret` sets the client secret, which is used by the `client_secret_basic` authentication method.

   For more information, see the Quarkus [OpenID Connect (OIDC) configuration properties](/guides/security-oidc-configuration-properties-reference/index.md) guide.

   <a id="bearer-token-tutorial-keycloak-dev-mode"></a>
   ## Run the application in dev mode

   Put the [realm configuration file](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-quickstart/config/quarkus-realm.json) in the `src/main/resources` application folder so that it gets copied to the classpath and imported automatically to Keycloak. You do not need to do this if you have already built a [complete solution](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-quickstart), in which case, this realm file is added to the classpath during the build.

   1. To run the application in dev mode, run the following commands:

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

   - [Dev Services for Keycloak](/guides/security-openid-connect-dev-services/index.md) will start a Keycloak container and import a `quarkus-realm.json`.
     1. Open a [Dev UI](/guides/dev-ui/index.md), which you can find at [/q/dev-ui](http://localhost:8080/q/dev-ui). Then, in an `OpenID Connect` card, click the `Keycloak provider` link .
     2. When prompted to log in to a `Single Page Application` provided by `OpenID Connect Dev UI`, do the following steps:

   \* Log in as `alice` (password: `alice`), who has a `user` role. \*\* Accessing `/api/admin` returns a `403` status code. \*\* Accessing `/api/users/me` returns a `200` status code. \* Log out and log in again as `admin` (password: `admin`), who has both `admin` and `user` roles. \*\* Accessing `/api/admin` returns a `200` status code. \*\* Accessing `/api/users/me` returns a `200` status code.

   ## Start and configure the Keycloak server

   > [!NOTE]
   > Do not start the Keycloak server when you [run the application in dev mode](#bearer-token-tutorial-keycloak-dev-mode); `Dev Services for Keycloak` will start and configure a container. For more information, see the [Run the application in dev mode](#bearer-token-tutorial-keycloak-dev-mode) section.

   1. To start a Keycloak server, you can use Docker to run the following command:

   ```bash
   docker run --name keycloak -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin -p 8180:8080 quay.io/keycloak/keycloak:26.7.4 start-dev
   ```

   1. You can access your Keycloak server at [localhost:8180](http://localhost:8180).
   2. To access the Keycloak Administration console, log in as the `admin` user by using the following login credentials:

   - Username: `admin`
   - Password: `admin`

   1. Import the [realm configuration file](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-openid-connect-quickstart/config/quarkus-realm.json) from the upstream community repository to create a new realm.

   For more information, see the Keycloak documentation about [creating and configuring a new realm](https://www.keycloak.org/docs/latest/server_admin/index.html#configuring-realms).

   > [!NOTE]
   > To configure the Keycloak server from your application by using the Keycloak Admin Client, include one of the following extensions based on your setup:


- **For Quarkus REST**: If you are using `quarkus-rest`, `quarkus-rest-client`, or both, include the `quarkus-keycloak-admin-rest-client` extension.

- **For RESTEasy Classic**: If you are using `quarkus-resteasy`, `quarkus-resteasy-client`, or both, include the `quarkus-keycloak-admin-resteasy-client` extension.

- **If no REST layer is explicitly used**: It is recommended to include the `quarkus-keycloak-admin-rest-client` extension.

These guidelines ensure seamless integration of the Keycloak Admin Client with your REST framework, whether you are working with a REST server, a REST client, or both.

For more information, see the [Quarkus Keycloak Admin Client](/guides/security-keycloak-admin-client/index.md) guide.

## Run the Application in JVM mode

1. Compile the application:

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

1. Run the application:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Run the application in native mode

You can compile this same demo as-is into native mode without any modifications. This implies that you no longer need to install a JVM on your production environment. The runtime technology is included in the produced binary and optimized to run with minimal resources required.

Compilation takes a bit longer, so this step is disabled by default.

1. Build your application again by enabling the `native` profile:

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

1. After waiting a little while, you run the following binary directly:

```bash
./target/security-openid-connect-quickstart-1.0.0-SNAPSHOT-runner
```

## Test the application

For information about testing your application in dev mode, see the preceding [Run the application in dev mode](#bearer-token-tutorial-keycloak-dev-mode) section.

You can test the application launched in JVM or native modes with `curl`.

- Because the application uses Bearer token authentication, you must first obtain an access token from the Keycloak server to access the application resources:

```bash
export access_token=$(\
    curl --insecure -X POST http://localhost:8180/realms/quarkus/protocol/openid-connect/token \
    --user backend-service:secret \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=alice&password=alice&grant_type=password' | jq --raw-output '.access_token' \
 )
```

When the `quarkus.oidc.authentication.user-info-required` property is set to `true` to require that an access token is used to request `UserInfo`, you must add a `scope=openid` query parameter to the token grant request command, for example:

```bash
export access_token=$(\
    curl --insecure -X POST http://localhost:8180/realms/quarkus/protocol/openid-connect/token \
    --user backend-service:secret \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=alice&password=alice&grant_type=password&scope=openid' | jq --raw-output '.access_token' \
 )
```

The preceding example obtains an access token for the user `alice`.

- Any user can access the `http://localhost:8080/api/users/me` endpoint, which returns a JSON payload with details about the user.

```bash
curl -v -X GET \
  http://localhost:8080/api/users/me \
  -H "Authorization: Bearer "$access_token
```

- Only users with the `admin` role can access the `http://localhost:8080/api/admin` endpoint. If you try to access this endpoint with the previously-issued access token, you get a `403` response from the server.

```bash
curl -v -X GET \
   http://localhost:8080/api/admin \
   -H "Authorization: Bearer "$access_token
```

- To access the admin endpoint, obtain a token for the `admin` user:

```bash
export access_token=$(\
    curl --insecure -X POST http://localhost:8180/realms/quarkus/protocol/openid-connect/token \
    --user backend-service:secret \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=admin&password=admin&grant_type=password' | jq --raw-output '.access_token' \
 )
```

For information about writing integration tests that depend on `Dev Services for Keycloak`, see the [Dev Services for Keycloak](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing-keycloak-devservices) section of the "OpenID Connect (OIDC) Bearer token authentication" guide.

## References

- [OIDC configuration properties](/guides/security-oidc-configuration-properties-reference/index.md)
- [OpenID Connect (OIDC) Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md)
- [Keycloak Documentation](https://www.keycloak.org/documentation.html)
- [OpenID Connect](https://openid.net/connect/)
- [JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [OpenID Connect and OAuth2 Client and Filters Reference Guide](/guides/security-openid-connect-client-reference/index.md)
- [Dev Services for Keycloak](/guides/security-openid-connect-dev-services/index.md)
- [Sign and encrypt JWT tokens with SmallRye JWT Build](/guides/security-jwt-build/index.md)
- [Combining authentication mechanisms](/guides/security-authentication-mechanisms/index.md#combining-authentication-mechanisms)
- [Quarkus Security overview](/guides/security-overview/index.md)
- [Quarkus Keycloak Admin Client](/guides/security-keycloak-admin-client/index.md)
