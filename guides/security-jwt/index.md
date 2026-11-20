# Using JWT RBAC

This guide explains how to integrate [SmallRye JWT](https://github.com/smallrye/smallrye-jwt/) into your Quarkus application to implement [JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519) security in compliance with the MicroProfile JWT specification. You’ll learn how to verify JWTs, represent them as MicroProfile JWT `org.eclipse.microprofile.jwt.JsonWebToken`, and secure Quarkus HTTP endpoints using bearer token authorization and [Role-Based Access Control](https://en.wikipedia.org/wiki/Role-based_access_control).

> [!NOTE]
> The Quarkus OpenID Connect (`quarkus-oidc`) extension also supports bearer token authorization and uses `smallrye-jwt` to represent bearer tokens as `JsonWebToken`. For details, see the [OIDC Bearer Token Authentication](/guides/security-oidc-bearer-token-authentication/index.md) guide.
>
> If your Quarkus application needs to authenticate users using the OIDC Authorization Code Flow, you must use the OpenID Connect extension. For more information, refer to the [OIDC Code Flow Mechanism for Protecting Web Applications](/guides/security-oidc-code-flow-authentication/index.md).

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Quickstart

### Solution

We recommend following the instructions in the upcoming sections to create the application step by step. If you prefer, you can skip ahead to the completed example.

To access the example, either clone the Git repository or download an archive:

- Clone the repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`.
- Download the [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The completed solution is located in the `security-jwt-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-jwt-quickstart).

### Creating the Maven project

First, create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-jwt-quickstart \
    --extension='rest-jackson,smallrye-jwt,smallrye-jwt-build' \
    --no-code
cd security-jwt-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-jwt-quickstart \
    -Dextensions='rest-jackson,smallrye-jwt,smallrye-jwt-build' \
    -DnoCode
cd security-jwt-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-jwt-quickstart"`

This command generates the Maven project and imports the `smallrye-jwt` extension, which includes the MicroProfile JWT RBAC support.

If you already have your Quarkus project configured, you can add the `smallrye-jwt` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add smallrye-jwt,smallrye-jwt-build
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='smallrye-jwt,smallrye-jwt-build'
```

**Gradle**

```bash
./gradlew addExtension --extensions='smallrye-jwt,smallrye-jwt-build'
```

This command adds the following dependencies to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt-build</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-smallrye-jwt")
implementation("io.quarkus:quarkus-smallrye-jwt-build")
```

### Examine the Jakarta REST resource

Create a REST endpoint in `src/main/java/org/acme/security/jwt/TokenSecuredResource.java` with the following content:

**REST endpoint V1**

```java
package org.acme.security.jwt;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/secured")
public class TokenSecuredResource {

    @Inject
    JsonWebToken jwt; // <1>

    @GET
    @Path("permit-all")
    @PermitAll // <2>
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context SecurityContext ctx) {
        return getResponseString(ctx); // <3>
    }

    private String getResponseString(SecurityContext ctx) {
        String name;
        if (ctx.getUserPrincipal() == null) { // <4>
            name = "anonymous";
        } else if (!ctx.getUserPrincipal().getName().equals(jwt.getName())) { // <5>
            throw new InternalServerErrorException("Principal and JsonWebToken names do not match");
        } else {
            name = ctx.getUserPrincipal().getName(); // <6>
        }
        return String.format("hello %s,"
            + " isHttps: %s,"
            + " authScheme: %s,"
            + " hasJWT: %s",
            name, ctx.isSecure(), ctx.getAuthenticationScheme(), hasJwt()); // <7>
    }

    private boolean hasJwt() {
        return jwt.getClaimNames() != null;
    }
}
```

1. The `JsonWebToken` interface is injected, providing access to claims associated with the current authenticated token. This interface extends `java.security.Principal`.
2. The `@PermitAll` is a standard Jakarta security annotation. It indicates that the given endpoint is accessible by all callers, whether authenticated or not.
3. The Jakarta REST `SecurityContext` is injected to inspect the security state of the request. The `getResponseString()` function generates the response.
4. Checks if the call is insecure by checking if the request user/caller `Principal` against null.
5. Ensures the names in the `Principal` and `JsonWebToken` match because the `JsonWebToken` represents the current `Principal`.
6. Retrieves the name of the `Principal`.
7. Builds a response containing the caller's name, the `isSecure()` and `getAuthenticationScheme()` states of the request `SecurityContext`, and whether a non-null `JsonWebToken` was injected.

<a id="run-application"></a>
### Run the application in dev mode

Now, you are ready to run the application in dev mode by using one of the following commands:

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

Then, you should see output similar to the following example:

**`quarkus:dev` output**

```shell
[INFO] Scanning for projects...
[INFO]
[INFO] ----------------------< org.acme:security-jwt-quickstart >-----------------------
[INFO] Building security-jwt-quickstart 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
...
Listening for transport dt_socket at address: 5005
2020-07-15 16:09:50,883 INFO  [io.quarkus] (Quarkus Main Thread) security-jwt-quickstart 1.0.0-SNAPSHOT on JVM (powered by Quarkus 999-SNAPSHOT) started in 1.073s. Listening on: http://0.0.0.0:8080
2020-07-15 16:09:50,885 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2020-07-15 16:09:50,885 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [cdi, mutiny, rest, rest-jackson, security, smallrye-context-propagation, smallrye-jwt, vertx, vertx-web]
```

Now that the REST endpoint is running, you can access it by using a command line tool such as curl:

**`curl` command for `/secured/permit-all`**

```shell
$ curl http://127.0.0.1:8080/secured/permit-all; echo
```

This command returns the following response:

```shell
hello anonymous, isHttps: false, authScheme: null, hasJWT: false
```

You have not provided any JWT in our request, so you would not expect the endpoint to see any security state, and the response is consistent with that:

- `username` is anonymous.
- `isHttps` is `false` because `https` is not used.
- `authScheme` is `null`.
- `hasJWT` is `false`.

Use Ctrl-C to stop the Quarkus server.

So now let's actually secure something. Take a look at the new endpoint method `helloRolesAllowed` in the following:

**REST endpoint V2**

```java
package org.acme.security.jwt;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/secured")
public class TokenSecuredResource {

    @Inject
    JsonWebToken jwt; // <1>

    @GET
    @Path("permit-all")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context SecurityContext ctx) {
        return getResponseString(ctx);
    }

    @GET
    @Path("roles-allowed") // <2>
    @RolesAllowed({ "User", "Admin" }) // <3>
    @Produces(MediaType.TEXT_PLAIN)
    public String helloRolesAllowed(@Context SecurityContext ctx) {
        return getResponseString(ctx) + ", birthdate: " + jwt.getClaim("birthdate").toString(); // <4>
    }

    private String getResponseString(SecurityContext ctx) {
        String name;
        if (ctx.getUserPrincipal() == null) {
            name = "anonymous";
        } else if (!ctx.getUserPrincipal().getName().equals(jwt.getName())) {
            throw new InternalServerErrorException("Principal and JsonWebToken names do not match");
        } else {
            name = ctx.getUserPrincipal().getName();
        }
        return String.format("hello %s,"
            + " isHttps: %s,"
            + " authScheme: %s,"
            + " hasJWT: %s",
            name, ctx.isSecure(), ctx.getAuthenticationScheme(), hasJwt());
    }

    private boolean hasJwt() {
        return jwt.getClaimNames() != null;
    }
}
```

1. The `JsonWebToken` is injected to access claims from the JWT.
2. This endpoint is exposed at `/secured/roles-allowed`.
3. The `@RolesAllowed` annotation restricts access to users with either the "User" or "Admin" role.
4. The response is constructed similarly to the `hello` method, with the addition of the `birthdate` claim retrieved directly from the injected `JsonWebToken`.

After you make this addition to your `TokenSecuredResource`, rerun the `./mvnw quarkus:dev` command, and then try `curl -v http://127.0.0.1:8080/secured/roles-allowed; echo` to attempt to access the new endpoint.

Your output should be as follows:

**`curl` command for `/secured/roles-allowed`**

```shell
$ curl -v http://127.0.0.1:8080/secured/roles-allowed; echo
```

This command returns the following response:

```shell
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to 127.0.0.1 (127.0.0.1) port 8080 (#0)
> GET /secured/roles-allowed HTTP/1.1
> Host: 127.0.0.1:8080
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 401 Unauthorized
< Connection: keep-alive
< Content-Type: text/html;charset=UTF-8
< Content-Length: 14
< Date: Sun, 03 Mar 2019 16:32:34 GMT
<
* Connection #0 to host 127.0.0.1 left intact
```

Excellent. You did not provide a JWT in the request, so access to the endpoint was correctly denied. Instead, you received an HTTP 401 Unauthorized error.

To access the endpoint, you must obtain and include a valid JWT in your request. This involves two steps:

1. Configuring the SmallRye JWT extension with the necessary information to validate a JWT.
2. Generating a JWT with the appropriate claims to match the configuration.

### Configuring the SmallRye JWT extension security information

Create a `security-jwt-quickstart/src/main/resources/application.properties` with the following content:

**Application properties for `TokenSecuredResource`**

```properties
mp.jwt.verify.publickey.location=publicKey.pem # <1>
mp.jwt.verify.issuer=https://example.com/issuer # <2>

quarkus.native.resources.includes=publicKey.pem # <3>
```

1. Specifies the location of the public key file `publicKey.pem` on the classpath. See [Adding a public key](#add-public-key) for adding this key.
2. Defines the expected issuer as `https://example.com/issuer`.
3. Ensures the `publicKey.pem` file is included as a resource in the native executable.

<a id="add-public-key"></a>
### Adding a public key

The [JWT specification](https://tools.ietf.org/html/rfc7519) defines various levels of security of JWTs that one can use. The MicroProfile JWT RBAC specification requires JWTs signed with the RSA-256 signature algorithm. This in turn requires an RSA public key pair. On the REST endpoint server side, you need to configure the location of the RSA public key to use to verify the JWT sent along with requests. The `mp.jwt.verify.publickey.location=publicKey.pem` setting configured previously expects that the public key is available on the classpath as `publicKey.pem`. To accomplish this, copy the following content to a `security-jwt-quickstart/src/main/resources/publicKey.pem` file.

**RSA public key PEM content**

```text
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlivFI8qB4D0y2jy0CfEq
Fyy46R0o7S8TKpsx5xbHKoU1VWg6QkQm+ntyIv1p4kE1sPEQO73+HY8+Bzs75XwR
TYL1BmR1w8J5hmjVWjc6R2BTBGAYRPFRhor3kpM6ni2SPmNNhurEAHw7TaqszP5e
UF/F9+KEBWkwVta+PZ37bwqSE4sCb1soZFrVz/UT/LF4tYpuVYt3YbqToZ3pZOZ9
AX2o1GCG3xwOjkc4x0W7ezbQZdC9iftPxVHR8irOijJRRjcPDtA6vPKpzLl6CyYn
sIYPd99ltwxTHjr3npfv/3Lw50bAkbT4HeLFxTx4flEoZLKO/g0bAoV2uqBhkA9x
nQIDAQAB
-----END PUBLIC KEY-----
```

### Generating a JWT

Often, one obtains a JWT from an identity manager such as [Keycloak](https://www.keycloak.org/). But for this quickstart, you generate our own by using the JWT generation API provided by `smallrye-jwt`. For more information, see [Generate JWT tokens with SmallRye JWT](/guides/security-jwt-build/index.md).

Take the code from the following listing and place it into `security-jwt-quickstart/src/test/java/org/acme/security/jwt/GenerateToken.java`:

**`GenerateToken` main driver class**

```java
package org.acme.security.jwt;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.microprofile.jwt.Claims;
import io.smallrye.jwt.build.Jwt;

/**
 * A utility class to generate and print a JWT token string to stdout.
 */
public class GenerateToken {

    /**
     * Generates and prints a JWT token.
     */
    public static void main(String[] args) {
        String token = Jwt.issuer("https://example.com/issuer") // <1>
                .upn("jdoe@quarkus.io") // <2>
                .groups(new HashSet<>(Arrays.asList("User", "Admin"))) // <3>
                .claim(Claims.birthdate.name(), "2001-07-13") // <4>
                .sign();

        System.out.println(token);
        System.exit(0);
    }
}
```

1. Sets the `iss` (issuer) claim in the JWT. This value must match the server-side `mp.jwt.verify.issuer` configuration for the token to be considered valid.
2. Specifies the `upn` (User Principal Name) claim, which the MicroProfile JWT RBAC specification defines as the preferred claim for identifying the `Principal` in container security APIs.
3. Defines the `groups` claim, which provides the group memberships and top-level roles assigned to the JWT bearer.
4. Adds a `birthdate` claim. Because this can be considered sensitive information, consider encrypting claims as described in [Generate JWT tokens with SmallRye JWT](/guides/security-jwt-build/index.md).

Note that for this code to work, you need the content of the RSA private key corresponding to the public key you have in the `TokenSecuredResource` application. Take the following PEM content and place it into `security-jwt-quickstart/src/test/resources/privateKey.pem`:

**RSA private key PEM content**

```text
-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCWK8UjyoHgPTLa
PLQJ8SoXLLjpHSjtLxMqmzHnFscqhTVVaDpCRCb6e3Ii/WniQTWw8RA7vf4djz4H
OzvlfBFNgvUGZHXDwnmGaNVaNzpHYFMEYBhE8VGGiveSkzqeLZI+Y02G6sQAfDtN
qqzM/l5QX8X34oQFaTBW1r49nftvCpITiwJvWyhkWtXP9RP8sXi1im5Vi3dhupOh
nelk5n0BfajUYIbfHA6ORzjHRbt7NtBl0L2J+0/FUdHyKs6KMlFGNw8O0Dq88qnM
uXoLJiewhg9332W3DFMeOveel+//cvDnRsCRtPgd4sXFPHh+UShkso7+DRsChXa6
oGGQD3GdAgMBAAECggEAAjfTSZwMHwvIXIDZB+yP+pemg4ryt84iMlbofclQV8hv
6TsI4UGwcbKxFOM5VSYxbNOisb80qasb929gixsyBjsQ8284bhPJR7r0q8h1C+jY
URA6S4pk8d/LmFakXwG9Tz6YPo3pJziuh48lzkFTk0xW2Dp4SLwtAptZY/+ZXyJ6
96QXDrZKSSM99Jh9s7a0ST66WoxSS0UC51ak+Keb0KJ1jz4bIJ2C3r4rYlSu4hHB
Y73GfkWORtQuyUDa9yDOem0/z0nr6pp+pBSXPLHADsqvZiIhxD/O0Xk5I6/zVHB3
zuoQqLERk0WvA8FXz2o8AYwcQRY2g30eX9kU4uDQAQKBgQDmf7KGImUGitsEPepF
KH5yLWYWqghHx6wfV+fdbBxoqn9WlwcQ7JbynIiVx8MX8/1lLCCe8v41ypu/eLtP
iY1ev2IKdrUStvYRSsFigRkuPHUo1ajsGHQd+ucTDf58mn7kRLW1JGMeGxo/t32B
m96Af6AiPWPEJuVfgGV0iwg+HQKBgQCmyPzL9M2rhYZn1AozRUguvlpmJHU2DpqS
34Q+7x2Ghf7MgBUhqE0t3FAOxEC7IYBwHmeYOvFR8ZkVRKNF4gbnF9RtLdz0DMEG
5qsMnvJUSQbNB1yVjUCnDAtElqiFRlQ/k0LgYkjKDY7LfciZl9uJRl0OSYeX/qG2
tRW09tOpgQKBgBSGkpM3RN/MRayfBtmZvYjVWh3yjkI2GbHA1jj1g6IebLB9SnfL
WbXJErCj1U+wvoPf5hfBc7m+jRgD3Eo86YXibQyZfY5pFIh9q7Ll5CQl5hj4zc4Y
b16sFR+xQ1Q9Pcd+BuBWmSz5JOE/qcF869dthgkGhnfVLt/OQzqZluZRAoGAXQ09
nT0TkmKIvlza5Af/YbTqEpq8mlBDhTYXPlWCD4+qvMWpBII1rSSBtftgcgca9XLB
MXmRMbqtQeRtg4u7dishZVh1MeP7vbHsNLppUQT9Ol6lFPsd2xUpJDc6BkFat62d
Xjr3iWNPC9E9nhPPdCNBv7reX7q81obpeXFMXgECgYEAmk2Qlus3OV0tfoNRqNpe
Mb0teduf2+h3xaI1XDIzPVtZF35ELY/RkAHlmWRT4PCdR0zXDidE67L6XdJyecSt
FdOUH8z5qUraVVebRFvJqf/oGsXc4+ex1ZKUTbY0wqY1y9E39yvB3MaTmZFuuqk8
f3cg+fr8aou7pr9SHhJlZCU=
-----END PRIVATE KEY-----
```

Later, you configure the `smallrye.jwt.sign.key.location` property to specify the location of the private signing key.

> [!NOTE]
> **Generating keys with OpenSSL**
>
> It is also possible to generate a public and private key pair by using the OpenSSL command line tool.
>
> **`openssl` commands to generate keys**
>
> ```text
> openssl genrsa -out rsaPrivateKey.pem 2048
> openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
> ```
>
> An additional step is required to generate and convert the private key to the PKCS#8 format, commonly used for secure key storage and transport.
>
> **`openssl` commands to perform the conversion**
>
> ```bash
> openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
> ```
>
> You can use the generated key pair instead of those used in this quickstart.

Ensure the [application is running](#run-application) before generating the JSON Web Token (JWT) for the `TokenSecuredResource` endpoint.

Next, use the following command to generate the JWT:

**Command to generate JWT**

```shell
$ mvn exec:java -Dexec.mainClass=org.acme.security.jwt.GenerateToken -Dexec.classpathScope=test -Dsmallrye.jwt.sign.key.location=privateKey.pem
```

The JWT string is a Base64 URL-encoded string consisting of three parts, separated by `.` characters:

1. The header, which contains metadata about the token, such as the signing algorithm.
2. The payload, also called "claims", which includes the token's claims or data.
3. The signature, which verifies the token's integrity.

### Finally, secured access to `/secured/roles-allowed`

Now, let's use this to make a secured request to the `/secured/roles-allowed` endpoint. Make sure you have the Quarkus server still running in dev mode, and then run the following command, making sure to use your version of the generated JWT from the previous step:

**`curl` command for `/secured/roles-allowed` with JWT**

```shell
$ curl -H "Authorization: Bearer eyJraWQ..." http://127.0.0.1:8080/secured/roles-allowed; echo
```

Make sure to use the generated token as the HTTP Authorization Bearer scheme value.

This command returns the following response:

```shell
hello jdoe@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 2001-07-13
```

Success! You now have the following:

- A non-anonymous caller name: `jdoe@quarkus.io`
- An authentication scheme: `Bearer`
- A non-null `JsonWebToken`
- The `birthdate` claim value

### Using the `JsonWebToken` and claim injection

Now that you can generate a JWT to access our secured REST endpoints, let's see what more you can do with the `JsonWebToken` interface and the JWT claims. The `org.eclipse.microprofile.jwt.JsonWebToken` interface extends the `java.security.Principal` interface, and is the object type returned by the `jakarta.ws.rs.core.SecurityContext#getUserPrincipal()` call you used previously. This means that code that does not use CDI but does have access to the REST container `SecurityContext` can get hold of the caller `JsonWebToken` interface by casting the `SecurityContext#getUserPrincipal()`.

The `JsonWebToken` interface defines methods for accessing claims in the underlying JWT. It provides accessors for common claims that are required by the MicroProfile JWT RBAC specification and arbitrary claims that might exist in the JWT.

All the JWT claims can also be injected. Let's expand our `TokenSecuredResource` with another endpoint `/secured/roles-allowed-admin` which uses the injected `birthdate` claim (as opposed to getting it from `JsonWebToken`):

```java
package org.acme.security.jwt;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/secured")
@RequestScoped <1>
public class TokenSecuredResource {

    @Inject
    JsonWebToken jwt; // <2>
    @Inject
    @Claim(standard = Claims.birthdate)
    String birthdate; // <3>

    @GET
    @Path("permit-all")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context SecurityContext ctx) {
        return getResponseString(ctx);
    }

    @GET
    @Path("roles-allowed")
    @RolesAllowed({ "User", "Admin" })
    @Produces(MediaType.TEXT_PLAIN)
    public String helloRolesAllowed(@Context SecurityContext ctx) {
        return getResponseString(ctx) + ", birthdate: " + jwt.getClaim("birthdate").toString();
    }

    @GET
    @Path("roles-allowed-admin")
    @RolesAllowed("Admin")
    @Produces(MediaType.TEXT_PLAIN)
    public String helloRolesAllowedAdmin(@Context SecurityContext ctx) {
        return getResponseString(ctx) + ", birthdate: " + birthdate; // <4>
    }

    private String getResponseString(SecurityContext ctx) {
        String name;
        if (ctx.getUserPrincipal() == null) {
            name = "anonymous";
        } else if (!ctx.getUserPrincipal().getName().equals(jwt.getName())) {
            throw new InternalServerErrorException("Principal and JsonWebToken names do not match");
        } else {
            name = ctx.getUserPrincipal().getName();
        }
        return String.format("hello %s,"
            + " isHttps: %s,"
            + " authScheme: %s,"
            + " hasJWT: %s",
            name, ctx.isSecure(), ctx.getAuthenticationScheme(), hasJwt());
    }

    private boolean hasJwt() {
        return jwt.getClaimNames() != null;
    }
}
```

1. The `@RequestScoped` scope is required to enable injection of the `birthdate` claim as a `String`.
2. The `JsonWebToken` is injected here, providing access to all claims and JWT-related information.
3. The `birthdate` claim is injected as a `String`. This highlights why the `@RequestScoped` scope is mandatory.
4. The injected `birthdate` claim is directly used to construct the response.

Now generate the token again and run:

```shell
$ curl -H "Authorization: Bearer eyJraWQ..." http://127.0.0.1:8080/secured/roles-allowed-admin; echo
```

Make sure to use the generated token as the HTTP Authorization Bearer scheme value.

This command returns the following response:

```shell
hello jdoe@quarkus.io, isHttps: false, authScheme: Bearer, hasJWT: true, birthdate: 2001-07-13
```

### Run the application in JVM mode

You can run the application as a standard Java application.

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

2. Run the application:

   ```bash
   java -jar target/quarkus-app/quarkus-run.jar
   ```

   ### Run the application in native mode

   You can compile this same demo into native mode without any modifications. This implies that you no longer need to install a JVM on your production environment. The runtime technology is included in the produced binary and optimized to run with minimal resources required.

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

1. Run the following binary directly:

```bash
./target/security-jwt-quickstart-1.0.0-SNAPSHOT-runner
```

### Explore the solution

The `security-jwt-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-jwt-quickstart) repository contains all the versions covered in this quickstart guide, along with additional endpoints that demonstrate subresources using injected `JsonWebToken` tokens and their claims via CDI APIs.

We encourage you to explore the `security-jwt-quickstart` directory and review the quickstart solutions to learn more about the features of the SmallRye JWT extension.

## Reference guide

<a id="supported-injection-scopes"></a>
### Supported injection scopes

`@ApplicationScoped`, `@Singleton` and `@RequestScoped` outer bean injection scopes are all supported when an `org.eclipse.microprofile.jwt.JsonWebToken` is injected, with the `@RequestScoped` scoping for `JsonWebToken` enforced to ensure the current token is represented.

However, `@RequestScoped` must be used when the individual token claims are injected as simple types such as `String`, for example:

```java
package org.acme.security.jwt;

import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;

@Path("/secured")
@RequestScoped
public class TokenSecuredResource {

    @Inject
    @Claim(standard = Claims.birthdate)
    String birthdate;
}
```

Note you can also use the injected `JsonWebToken` to access the individual claims, but setting `@RequestScoped` is unnecessary in this case.

Please see [MP JWT CDI Injection Requirements](https://download.eclipse.org/microprofile/microprofile-jwt-auth-1.2/microprofile-jwt-auth-spec-1.2.html#_cdi_injection_requirements) for more details.

<a id="supported-public-key-formats"></a>
### Supported public key formats

Public keys can be formatted in any of the following formats, specified in order of precedence:

- Public Key Cryptography Standards <mark>8 (PKCS</mark>8) PEM
- JSON Web Key (JWK)
- JSON Web Key Set (JWKS)
- JSON Web Key (JWK) Base64 URL encoded
- JSON Web Key Set (JWKS) Base64 URL encoded

### Dealing with verification keys

If you need to verify the token signature by using the asymmetric RSA or Elliptic Curve (EC) key, use the `mp.jwt.verify.publickey.location` property to refer to the local or remote verification key.

Use `mp.jwt.verify.publickey.algorithm` to customize the verification algorithm (default is `RS256`); for example, set it to `ES256` when working with the EC keys.

If you need to verify the token signature by using the symmetric secret key, then either a `JSON Web Key` (JWK) or `JSON Web Key Set` (JWK Set) format must be used to represent this secret key, for example:

```json
{
 "keys": [
   {
     "kty":"oct",
     "kid":"secretKey",
     "k":"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow"
   }
 ]
}
```

This secret key JWK must also be referred to with `smallrye.jwt.verify.key.location`. `smallrye.jwt.verify.algorithm` should be set to `HS256`/`HS384`/`HS512`.

<a id="jwt-parser"></a>
### Parse and verify `JsonWebToken` with `JWTParser`

If the JWT token can not be injected, for example, if it is embedded in the service request payload or the service endpoint acquires it out of band, then one can use `JWTParser`:

```java
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.jwt.auth.principal.JWTParser;
...
@Inject JWTParser parser;

String token = getTokenFromOidcServer();

// Parse and verify the token
JsonWebToken jwt = parser.parse(token);
```

You can also use it to customize how the token is verified or decrypted. For example, one can supply a local `SecretKey`:

```java
package org.acme.security.jwt;

import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.build.Jwt;

@Path("/secured")
public class SecuredResource {
    private static final String SECRET = "AyM1SysPpbyDfgZld3umj1qzKObwVMko";

    @Inject
    JWTParser parser;

    @GET
    @Produces("text/plain")
    public Response getUserName(@CookieParam("jwt") String jwtCookie) throws ParseException {
        if (jwtCookie == null) {
            // Create a JWT token signed by using the 'HS256' algorithm
            String newJwtCookie = Jwt.upn("Alice").signWithSecret(SECRET);
            // or create a JWT token encrypted by using the 'A256KW' algorithm
            // Jwt.upn("alice").encryptWithSecret(secret);
            return Response.ok("Alice").cookie(new NewCookie("jwt", newJwtCookie)).build();
        } else {
            // All mp.jwt and smallrye.jwt properties are still effective; only the verification key is customized.
            JsonWebToken jwt = parser.verify(jwtCookie, SECRET);
            // or jwt = parser.decrypt(jwtCookie, secret);
            return Response.ok(jwt.getName()).build();
        }
    }
}
```

Please also see the [How to Add SmallRye JWT directly](#add-smallrye-jwt) section about using `JWTParser` without the `HTTP` support provided by `quarkus-smallrye-jwt`.

### Token decryption

If your application needs to accept tokens with encrypted claims or encrypted inner-signed claims, simply set the `smallrye.jwt.decrypt.key.location` property to point to the decryption key.

If this is the only key property set, the incoming token is expected to contain only encrypted claims. If either `mp.jwt.verify.publickey` or `mp.jwt.verify.publickey.location` verification properties are also set, then the incoming token is expected to contain the encrypted inner-signed token.

See [Generate JWT tokens with SmallRye JWT](/guides/security-jwt-build/index.md) and learn how to generate the encrypted or inner-signed and then encrypted tokens quickly.

### Custom factories

The `io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipalFactory` is the default implementation used to parse and verify JWT tokens, converting them into `JsonWebToken` principals. This factory relies on the `MP JWT` and `smallrye-jwt` properties, as described in the `Configuration` section, to validate and customize JWT tokens.

If you need to implement a custom factory—such as to skip re-verifying tokens that have already been validated by a firewall—you can do so in one of the following ways:

- Use the `ServiceLoader` mechanism by creating a `META-INF/services/io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory` resource.
- Provide an `Alternative` CDI bean implementation, like the example below:

```java
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;

@ApplicationScoped
@Alternative
@Priority(1)
public class TestJWTCallerPrincipalFactory extends JWTCallerPrincipalFactory {

    @Override
    public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
        try {
            // Token has already been verified; parse the token claims only
            String json = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
            return new DefaultJWTCallerPrincipal(JwtClaims.parse(json));
        } catch (InvalidJwtException ex) {
            throw new ParseException(ex.getMessage());
        }
    }
}
```

### Blocking calls

`quarkus-smallrye-jwt` extension uses [SmallRye JWT](https://github.com/smallrye/smallrye-jwt) library which is currently not reactive.

What it means from the perspective of `quarkus-smallrye-jwt`, which operates as part of the reactive Quarkus security architecture, is that an IO thread entering the [SmallRye JWT](https://github.com/smallrye/smallrye-jwt) verification or decryption code might block in one of the following cases:

- The default key resolver refreshes the `JsonWebKey` set containing the keys, which involves a remote call to the OIDC endpoint.
- The custom key resolver, such as `AWS Application Load Balancer` (`ALB`) key resolver, resolves the keys against the AWS ALB key endpoint by using the current token's key identifier header value.

In such cases, if connections are slow—for instance, taking more than 3 seconds to respond to the key endpoint—the current event loop thread is likely to become blocked.

To prevent it from blocking, set `quarkus.smallrye-jwt.blocking-authentication=true`.

### Token propagation

Please see the [Token Propagation](/guides/security-openid-connect-client-reference/index.md#token-propagation-rest) section about the Bearer access token propagation to the downstream services.

<a id="integration-testing"></a>
### Testing

<a id="auto-generated-keys"></a>
#### Automatic key generation

This extension generates an asymmetric RSA 2024 bit signing key pair in the dev and test modes if the verification key has not been configured. Once the key pair is generated, the public RSA key is used to configure the `mp.jwt.verify.publickey` property, and the RSA private key is available to tests to sign tokens using `smallrye-jwt-build`, for example:

```java
import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.GET;

@GET
public String token() {
   return Jwt.upn("Alice").sign();
}
```

See [signing claims guide](/guides/security-jwt-build/index.md#sign-claims) for mode details.

You can disable automatic key generation by setting at least one of the following properties:

- `mp.jwt.verify.publickey.location`
- `mp.jwt.verify.publickey`
- `mp.jwt.decrypt.key.location`
- `smallrye.jwt.encrypt.key.location`
- `smallrye.jwt.sign.key.location`
- `smallrye.jwt.sign.key`

In **dev** mode, if you do not explicitly configure the issuer using the `mp.jwt.verify.issuer` property, the SmallRye JWT extension will set a default issuer of `https://quarkus.io/issuer`.

If you prefer to configure the issuer programmatically, set `mp.jwt.verify.issuer` to `NONE` to prevent the default value from being applied.

<a id="integration-testing-wiremock"></a>
#### Wiremock

If you configure `mp.jwt.verify.publickey.location` to point to HTTPS or HTTP-based JsonWebKey (JWK) set, then you can use the same approach as described in the [OpenID Connect Bearer Token Integration testing](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing) `Wiremock` section but only change the `application.properties` to use MP JWT configuration properties instead:

```properties
# keycloak.url is set by OidcWiremockTestResource
mp.jwt.verify.publickey.location=${keycloak.url}/realms/quarkus/protocol/openid-connect/certs
mp.jwt.verify.issuer=${keycloak.url}/realms/quarkus
```

<a id="integration-testing-keycloak"></a>
#### Keycloak

If you work with Keycloak and configure `mp.jwt.verify.publickey.location` to point to HTTPS or HTTP-based JsonWebKey (JWK) set, you can use the same approach as described in the [OpenID Connect Bearer Token Integration testing](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing) Keycloak section but only change the `application.properties` to use MP JWT configuration properties instead:

```properties
# keycloak.url is set by DevServices for Keycloak
mp.jwt.verify.publickey.location=${keycloak.url}/realms/quarkus/protocol/openid-connect/certs
mp.jwt.verify.issuer=${keycloak.url}/realms/quarkus
```

Note that the tokens issued by Keycloak have an `iss` (issuer) claim set to the realm endpoint address.

If your Quarkus application runs in a Docker container, it might share a network interface with a Keycloak container started by DevServices for Keycloak. In this scenario, the Quarkus application and Keycloak communicate through an internal shared Docker network.

In such cases, use the following configuration instead:

```properties
# keycloak.url is set by DevServices for Keycloak,
# Quarkus accesses it through an internal shared docker network interface.
mp.jwt.verify.publickey.location=${keycloak.url}/realms/quarkus/protocol/openid-connect/certs

# Issuer is set to the docker bridge localhost endpoint address represented by the `client.quarkus.oidc.auth-server-url` property
mp.jwt.verify.issuer=${client.quarkus.oidc.auth-server-url}
```

<a id="integration-testing-public-key"></a>
#### Local public key

You can use the same approach as described in the [OpenID Connect Bearer Token Integration testing](/guides/security-oidc-bearer-token-authentication/index.md#bearer-token-integration-testing) `Local public key` section but only change the `application.properties` to use MP JWT configuration properties instead:

```properties
mp.jwt.verify.publickey=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlivFI8qB4D0y2jy0CfEqFyy46R0o7S8TKpsx5xbHKoU1VWg6QkQm+ntyIv1p4kE1sPEQO73+HY8+Bzs75XwRTYL1BmR1w8J5hmjVWjc6R2BTBGAYRPFRhor3kpM6ni2SPmNNhurEAHw7TaqszP5eUF/F9+KEBWkwVta+PZ37bwqSE4sCb1soZFrVz/UT/LF4tYpuVYt3YbqToZ3pZOZ9AX2o1GCG3xwOjkc4x0W7ezbQZdC9iftPxVHR8irOijJRRjcPDtA6vPKpzLl6CyYnsIYPd99ltwxTHjr3npfv/3Lw50bAkbT4HeLFxTx4flEoZLKO/g0bAoV2uqBhkA9xnQIDAQAB
# set it to the issuer value which is used to generate the tokens
mp.jwt.verify.issuer=${keycloak.url}/realms/quarkus

# required to sign the tokens
smallrye.jwt.sign.key.location=privateKey.pem
```

<a id="integration-testing-security-annotation"></a>
#### `TestSecurity` annotation

Add the following dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-security-jwt</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.quarkus:quarkus-test-security-jwt")
```

Then, write test code such as this:

```java
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(ProtectedResource.class)
public class TestSecurityAuthTest {

    @Test
    @TestSecurity(user = "userJwt", roles = "viewer")
    public void testJwt() {
        RestAssured.when().get("test-security-jwt").then()
                .body(is("userJwt:viewer"));
    }

    @Test
    @TestSecurity(user = "userJwt", roles = "viewer")
    @JwtSecurity(claims = {
            @Claim(key = "email", value = "user@gmail.com")
    })
    public void testJwtWithClaims() {
        RestAssured.when().get("test-security-jwt-claims").then()
                .body(is("userJwt:viewer:user@gmail.com"));
    }

}
```

where the `ProtectedResource` class might look like this:

```java
@Path("/web-app")
@Authenticated
public class ProtectedResource {

    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("test-security-jwt")
    public String testSecurityOidc() {
        return accessToken.getName() + ":" + accessToken.getGroups().iterator().next();
    }

    @GET
    @Path("test-security-jwt-claims")
    public String testSecurityOidcUserInfoMetadata() {
        return accessToken.getName() + ":" + accessToken.getGroups().iterator().next()
                + ":" + accessToken.getClaim("email");
    }
}
```

Note that the `@TestSecurity` annotation must always be used, and its `user` property is returned as `JsonWebToken.getName()` and `roles` property - as `JsonWebToken.getGroups()`. `@JwtSecurity` annotation is optional and can be used to set the additional token claims.

`@TestSecurity` and `@JwtSecurity` can be combined in a meta-annotation, as follows:

```java
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    @TestSecurity(user = "userOidc", roles = "viewer")
    @OidcSecurity(introspectionRequired = true,
        introspection = {
            @TokenIntrospection(key = "email", value = "user@gmail.com")
        }
    )
    public @interface TestSecurityMetaAnnotation {

    }
```

This is particularly useful if the same set of security settings needs to be used in multiple test methods.

### How to check the errors in the logs

Please enable `io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator` `TRACE` level logging to see more details about the token verification or decryption errors:

```properties
quarkus.log.category."io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator".level=TRACE
quarkus.log.category."io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator".min-level=TRACE
```

### Proactive authentication

If you'd like to skip the token verification when the public endpoint methods are invoked, disable the [proactive authentication](/guides/security-proactive-authentication/index.md).

Note that you can't access the injected `JsonWebToken` through public methods if token verification has not been done.

<a id="add-smallrye-jwt"></a>
### How to add SmallRye JWT directly

To [parse and verify JsonWebToken with JWTParser](#jwt-parser), use `smallrye-jwt` instead of `quarkus-smallrye-jwt` directly for the following situations:

- You work with Quarkus extensions that do not support `HTTP`, such as `Quarkus GRPC`.
- You provide an extension-specific `HTTP`, the support of which conflicts with the support of those offered by `quarkus-smallrye-jwt` and `Vert.x HTTP`, such as `Quarkus AWS Lambda`.

Start with adding the `smallrye-jwt` dependency:

**pom.xml**

```xml
<dependency>
    <groupId>io.smallrye</groupId>
    <artifactId>smallrye-jwt</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.smallrye:smallrye-jwt")
```

Then, update `application.properties` to get all the CDI producers provided by `smallrye-jwt` included as follows:

```properties
quarkus.index-dependency.smallrye-jwt.group-id=io.smallrye
quarkus.index-dependency.smallrye-jwt.artifact-id=smallrye-jwt
```

<a id="configuration-reference"></a>
## Configuration reference

### Quarkus configuration

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-smallrye-jwt_quarkus-smallrye-jwt-enabled"></a> [`quarkus.smallrye-jwt.enabled`](#quarkus-smallrye-jwt_quarkus-smallrye-jwt-enabled) config_property_copy_button:+++quarkus.smallrye-jwt.enabled+++[]<br><br>The MP-JWT configuration object<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_JWT_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-smallrye-jwt_quarkus-smallrye-jwt-rsa-sig-provider"></a> [`quarkus.smallrye-jwt.rsa-sig-provider`](#quarkus-smallrye-jwt_quarkus-smallrye-jwt-rsa-sig-provider) config_property_copy_button:+++quarkus.smallrye-jwt.rsa-sig-provider+++[]<br><br>The name of the `java.security.Provider` that supports SHA256withRSA signatures<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_JWT_RSA_SIG_PROVIDER+++[] | string | `SunRsaSign` |
| <a id="quarkus-smallrye-jwt_quarkus-smallrye-jwt-blocking-authentication"></a> [`quarkus.smallrye-jwt.blocking-authentication`](#quarkus-smallrye-jwt_quarkus-smallrye-jwt-blocking-authentication) config_property_copy_button:+++quarkus.smallrye-jwt.blocking-authentication+++[]<br><br>Enable this property if fetching the remote keys can be a time-consuming operation. Do not enable it if you use the local keys.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_JWT_BLOCKING_AUTHENTICATION+++[] | boolean | `'true' if 'mp.jwt.decrypt.key.location' or 'mp.jwt.verify.publickey.location' location is set and the location uses the HTTP or HTTPS protocol` |
| <a id="quarkus-smallrye-jwt_quarkus-smallrye-jwt-silent"></a> [`quarkus.smallrye-jwt.silent`](#quarkus-smallrye-jwt_quarkus-smallrye-jwt-silent) config_property_copy_button:+++quarkus.smallrye-jwt.silent+++[]<br><br>Always create HTTP 401 challenge, even for requests containing no authentication credentials. JWT authentication mechanism will return HTTP 401 when an authentication challenge is required. However if it is used alongside one of the interactive authentication mechanisms then returning HTTP 401 to the users accessing the application from a browser may not be desired. If you prefer you can request that JWT authentication mechanism does not create a challenge in such cases by setting this property to 'true'.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_JWT_SILENT+++[] | boolean | `false` |
| <a id="quarkus-smallrye-jwt_quarkus-smallrye-jwt-priority"></a> [`quarkus.smallrye-jwt.priority`](#quarkus-smallrye-jwt_quarkus-smallrye-jwt-priority) config_property_copy_button:+++quarkus.smallrye-jwt.priority+++[]<br><br>JWT authentication mechanism priority.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SMALLRYE_JWT_PRIORITY+++[] | int | `1000` |

### MicroProfile JWT configuration

| `Property Name` | `Default` | Description |
| --- | --- | --- |
| `` `mp.jwt.verify.publickey` `` | `none` | The `mp.jwt.verify.publickey` config property allows the public key text to be supplied as a string. The public key is parsed from the supplied string in the order defined in the [Supported public key formats](#supported-public-key-formats) section. |
| `` `mp.jwt.verify.publickey.location` `` | `none` | Config property allows for a specified external or internal location of the public key. The value can be a relative path or a URL. If the value points to an HTTPS-based JWK set, then, for it to work in native mode, the `quarkus.ssl.native` property must also be set to `true`. See [Using SSL With Native Executables](/guides/native-and-ssl/index.md) for more details. |
| `` `mp.jwt.verify.publickey.algorithm` `` | `` `RS256` `` | List of signature algorithms. Set it to `ES256` to support the Elliptic Curve signature algorithm. |
| `` `mp.jwt.decrypt.key.location` `` | `none` | Config property allows for a specified external or internal location of the Private Decryption Key. |
| `` `mp.jwt.decrypt.key.algorithm` `` | `` `RSA-OAEP`,`RSA-OAEP-256` `` | List of decryption algorithms. Set it to `RSA-OAEP-256` to support RSA-OAEP with SHA-256 only. |
| `` `mp.jwt.verify.issuer` `` | `none` | Config property specifies the value of the `iss` (issuer) claim of the JWT that the server accepts as valid. |
| `` `mp.jwt.verify.audiences` `` | `none` | Comma-separated list of audiences a token `aud` claim might contain. |
| `` `mp.jwt.verify.clock.skew` `` | `` `60` `` | Clock skew in seconds used during the token expiration and age verification. An expired token is accepted if the current time is within the number of seconds specified by this property after the token expiration time. The default value is 60 seconds. |
| `` `mp.jwt.verify.token.age` `` | `` `none` `` | Number of seconds that must not elapse since the token `iat` (issued at) time. |
| `` `mp.jwt.token.header` `` | `` `Authorization` `` | Set this property if another header, such as `Cookie`, is used to pass the token. |
| `` `mp.jwt.token.cookie` `` | `none` | Name of the cookie containing a token. This property is effective only if `mp.jwt.token.header` is set to `Cookie`. |

### Additional SmallRye JWT configuration

SmallRye JWT provides more properties that can be used to customize the token processing:

| `Property Name` | `Default` | Description |
| --- | --- | --- |
| `` `smallrye.jwt.verify.secretkey` `` | `none` | Secret key supplied as a string. |
| `` `smallrye.jwt.verify.key.location` `` | `NONE` | Location of the verification key, which can point to both public and secret keys. Secret keys can only be in the JWK format. Note that 'mp.jwt.verify.publickey.location' is ignored if this property is set. |
| `` `smallrye.jwt.verify.algorithm` `` | `` | Signature algorithm. This property should only be used to set a symmetric algorithm such as `HS256`. It is deprecated for setting asymmetric algorithms such as `ES256` - use 'mp.jwt.verify.publickey.algorithm' instead. |
| `` `smallrye.jwt.verify.key-format` `` | `` `ANY` `` | Set this property to a specific key format such as `PEM_KEY`, `PEM_CERTIFICATE`, `JWK` or `JWK_BASE64URL` to optimize the way the verification key is loaded. |
| `` `smallrye.jwt.verify.key-provider` `` | `` `DEFAULT` `` | By default, PEM, JWK, or JWK key sets can be read from the local file system or fetched from URIs as required by MicroProfile JWT specification. Set this property to `AWS_ALB` to support an AWS Application Load Balancer verification key resolution. |
| `` `smallrye.jwt.verify.relax-key-validation` `` | `false` | Relax the validation of the verification keys; setting this property to `true` allows public RSA keys with a length of less than 2048 bits. |
| `` `smallrye.jwt.verify.certificate-thumbprint` `` | `false` | If this property is enabled, a signed token must contain either 'x5t' or 'x5t#S256' X509Certificate thumbprint headers. Verification keys can only be in JWK or PEM Certificate key formats. JWK keys must have an 'x5c' (Base64-encoded X509Certificate) property set. |
| `` `smallrye.jwt.token.header` `` | `` `Authorization` `` | Set this property if another header, such as `Cookie`, is used to pass the token. This property is deprecated - use 'mp.jwt.token.header'. |
| `` `smallrye.jwt.key-cache-size` `` | `` `100` `` | Key cache size. Use this property and `smallrye.jwt.key-cache-time-to-live` to control the key cache when a key provider such as `AWS_ALB` is configured with `smallrye.jwt.verify.key-provider=AWS_ALB` for resolving the keys dynamically. |
| `` `smallrye.jwt.key-cache-time-to-live` `` | `` `10` `` | Key cache entry time-to-live in minutes. Use this property and `smallrye.jwt.key-cache-size` to control the key cache when a key provider such as `AWS_ALB` is configured with `smallrye.jwt.verify.key-provider=AWS_ALB` for resolving the keys dynamically. |
| `` `smallrye.jwt.token.cookie` `` | `none` | Name of the cookie containing a token. This property is effective only if `smallrye.jwt.token.header` is set to `Cookie`. This property is deprecated - use `mp.jwt.token.cookie`. |
| `` `smallrye.jwt.always-check-authorization` `` | `false` | Set this property to `true` for the `Authorization` header to be checked even if the `smallrye.jwt.token.header` is set to `Cookie` but no cookie with a `smallrye.jwt.token.cookie` name exists. |
| `` `smallrye.jwt.token.schemes` `` | `` `Bearer` `` | Comma-separated list containing alternative single or multiple schemes, such as `DPoP`. |
| `` `smallrye.jwt.token.kid` `` | `none` | Key identifier. The verification JWK key and every JWT token must have a matching `kid` header if it is set. |
| `` `smallrye.jwt.time-to-live` `` | `none` | The maximum number of seconds a JWT can be issued for use. Effectively, the difference between the expiration date of the JWT and the issued at date must not exceed this value. Setting this property to a non-positive value relaxes the requirement for the token to have a valid 'iat' (issued at) claim. |
| `` `smallrye.jwt.require.named-principal` `` | `` `true` `` | If an application relies on `java.security.Principal` returning a name, then a token must have a `upn` or `preferred_username` or `sub` claim set. Setting this property results in SmallRye JWT throwing an exception if none of these claims is available for the application code to deal with a non-null `Principal` name reliably. |
| `` `smallrye.jwt.path.sub` `` | `none` | Path to the claim containing the subject name. It starts from the top-level JSON object and can contain multiple segments where each segment only represents a JSON object name, for example, `realms/subject`. This property can be used if a token has no 'sub' claim but has the subject set in a different claim. Use double quotes with the namespace-qualified claims. |
| `` `smallrye.jwt.claims.sub` `` | `none` | This property can set a default sub claim value when the current token has no standard or custom `sub` claim available. Effectively, this property can be used to customize the `java.security.Principal` name if no `upn` or `preferred_username` or `sub` claim is set. |
| `` `smallrye.jwt.path.groups` `` | `none` | Path to the claim containing the groups. It starts from the top-level JSON object and can contain multiple segments where each segment represents a JSON object name only, for example: `realm/groups`. This property can be used if a token has no 'groups' claim but has the groups set in a different claim. Use double quotes with the namespace-qualified claims. |
| `` `smallrye.jwt.groups-separator` `` | `space` | Separator for splitting a string which might contain multiple group values. It is only used if the `smallrye.jwt.path.groups` property points to a custom claim with a string value. The default value is a single space because a standard OAuth2 `scope` claim might contain a space-separated sequence. |
| `` `smallrye.jwt.claims.groups` `` | `none` | This property can set a default groups claim value when the current token has no standard or custom groups claim available. |
| `` `smallrye.jwt.jwks.refresh-interval` `` | `60` | JWK cache refresh interval in minutes. It is ignored unless the `mp.jwt.verify.publickey.location` points to the HTTP or HTTPS URL-based JWK set and no HTTP `Cache-Control` response header with a positive `max-age` parameter value is returned from a JWK HTTPS endpoint. |
| `` `smallrye.jwt.jwks.forced-refresh-interval` `` | `30` | Forced JWK cache refresh interval in minutes, which is used to restrict the frequency of the forced refresh attempts that might happen when the token verification fails due to the cache having no JWK key with a `kid` property matching the current token's `kid` header. It is ignored unless the `mp.jwt.verify.publickey.location` points to the HTTP or HTTPS URL-based JWK set. |
| `` `smallrye.jwt.expiration.grace` `` | `0` | Expiration grace in seconds. By default, an expired token is still accepted if the current time is no more than 1 min after the token expiry time. This property is deprecated. Use `mp.jwt.verify.clock.skew` instead. |
| `` `smallrye.jwt.verify.aud` `` | `none` | Comma-separated list of audiences a token `aud` claim might contain. This property is deprecated - use `mp.jwt.verify.audiences`. |
| `` `smallrye.jwt.required.claims` `` | `none` | Comma-separated list of the claims a token must contain. |
| `` `smallrye.jwt.decrypt.key.location` `` | `none` | Config property to specify the external or internal location of Private Decryption Key. This property is deprecated - use `mp.jwt.decrypt.key.location`. |
| `` `smallrye.jwt.decrypt.algorithm` `` | `` `RSA_OAEP` `` | Decryption algorithm. |
| `` `smallrye.jwt.decrypt.key` `` | `none` | Decryption key supplied as a string. |
| `` `smallrye.jwt.token.decryption.kid` `` | `none` | Decryption Key identifier. If it is set then the decryption JWK key as well every JWT token must have a matching `kid` header. |
| `` `smallrye.jwt.client.tls.certificate.path` `` | `none` | Path to TLS trusted certificate which might need to be configured if the keys have to be fetched over `HTTPS`. |
| `` `smallrye.jwt.client.tls.trust-all` `` | `false` | Trust all the hostnames. If the keys have to be fetched over `HTTPS` and this property is set to `true` then all the hostnames are trusted by default. |
| `` `smallrye.jwt.client.tls.hosts` `` | `none` | Set of trusted hostnames. If the keys have to be fetched over `HTTPS` and `smallrye.jwt.client.tls.trust-all` is set to `false` then this property can be used to configure the trusted hostnames. |
| `` `smallrye.jwt.http.proxy.host` `` | `none` | HTTP proxy host. |
| `` `smallrye.jwt.http.proxy.port` `` | `80` | HTTP proxy port. |
| `` `smallrye.jwt.keystore.type` `` | `` `JKS` `` | This property can be used to customize a keystore type if either `mp.jwt.verify.publickey.location` or `mp.jwt.decrypt.key.location` points to a `KeyStore` file. If it is not set, the file name is checked to determine the keystore type before defaulting to `JKS`. |
| `` `smallrye.jwt.keystore.provider` `` | `` | This property can be used to customize a `KeyStore` provider if `mp.jwt.verify.publickey.location` or `mp.jwt.decrypt.key.location` points to a `KeyStore` file. |
| `` `smallrye.jwt.keystore.password` `` | `` | Keystore password. If `mp.jwt.verify.publickey.location` or `mp.jwt.decrypt.key.location`, this property must be set. |
| `` `smallrye.jwt.keystore.verify.key.alias` `` | `` | This property has to be set to identify a public verification key which is extracted from `KeyStore` from a matching certificate if `mp.jwt.verify.publickey.location` points to a `KeyStore` file. |
| `` `smallrye.jwt.keystore.decrypt.key.alias` `` | `` | This property has to be set to identify a private decryption key if `mp.jwt.decrypt.key.location` points to a `KeyStore` file. |
| `` `smallrye.jwt.keystore.decrypt.key.password` `` | `` | This property can be set if a private decryption key's password in `KeyStore` is different from `smallrye.jwt.keystore.password` when `mp.jwt.decrypt.key.location` points to a `KeyStore` file. |
| `` `smallrye.jwt.resolve-remote-keys-at-startup` `` | `false` | Set this property to true to resolve the remote keys at the application startup. |

## References

- [MP JWT 1.2 HTML](https://download.eclipse.org/microprofile/microprofile-jwt-auth-1.2/microprofile-jwt-auth-spec-1.2.html)
- [MP JWT 1.2 PDF](https://download.eclipse.org/microprofile/microprofile-jwt-auth-1.2/microprofile-jwt-auth-spec-1.2.pdf)
- [SmallRye JWT](https://github.com/smallrye/smallrye-jwt)
- [JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [JSON Web Signature](https://tools.ietf.org/html/rfc7515)
- [JSON Web Encryption](https://tools.ietf.org/html/rfc7516)
- [JSON Web Algorithms](https://tools.ietf.org/html/rfc7518)
- [Sign and encrypt JWT tokens with SmallRye JWT Build](/guides/security-jwt-build/index.md)
- [Combining authentication mechanisms](/guides/security-authentication-mechanisms/index.md#combining-authentication-mechanisms)
- [Quarkus Security overview](/guides/security-overview/index.md)
