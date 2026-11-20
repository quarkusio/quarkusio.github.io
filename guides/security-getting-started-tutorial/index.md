# Getting started with security by using Basic authentication and Jakarta Persistence

Get started with Quarkus Security by securing your Quarkus application endpoints with the built-in Quarkus [Basic authentication](/guides/security-basic-authentication/index.md) and the Jakarta Persistence identity provider, enabling role-based access control.

The Jakarta Persistence `IdentityProvider` verifies and converts a [Basic authentication](/guides/security-basic-authentication/index.md) user name and password pair to a `SecurityIdentity` instance, which is used to authorize access requests, making your Quarkus application secure.

For more information about Jakarta Persistence, see the [Quarkus Security with Jakarta Persistence](/guides/security-jpa/index.md) guide.

This tutorial prepares you to implement more advanced security mechanisms in Quarkus, for example, how to use the OpenID Connect (OIDC) authentication mechanism.

## Prerequisites

To complete this guide, you need:

- Roughly 15 minutes
- An IDE
- JDK 17+ installed with `JAVA_HOME` configured appropriately
- Apache Maven 3.9.16
- Optionally the [Quarkus CLI](/guides/cli-tooling/index.md) if you want to use it
- Optionally Mandrel or GraalVM installed and [configured appropriately](/guides/building-native-image/index.md#configuring-graalvm) if you want to build a native executable (or Docker if you use a native container build)

## Building your application

This tutorial gives detailed steps for creating an application with endpoints that illustrate various authorization policies:

| Endpoint | Description |
| --- | --- |
| `/api/public` | Accessible without authentication, this endpoint allows anonymous access. |
| `/api/admin` | Secured with role-based access control (RBAC), this endpoint is accessible only to users with the `admin` role. Access is controlled declaratively by using the `@RolesAllowed` annotation. |
| `/api/users/me` | Also secured by RBAC, this endpoint is accessible only to users with the `user` role. It returns the caller's username as a string. |

> [!TIP]
> To examine the completed example, download the [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip) or clone the Git repository:
>
> ```shell
> git clone https://github.com/quarkusio/quarkus-quickstarts.git
> ```
>
> You can find the solution in the `security-jpa-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-jpa-quickstart/).

## Create and verify the Maven project

For Quarkus Security to be able to map your security source to Jakarta Persistence entities, ensure that the Maven project in this tutorial includes the `quarkus-security-jpa` or `quarkus-security-jpa-reactive` extension.

> [!NOTE]
> [Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md) is used to store your user identities, but you can also use [Hibernate ORM](/guides/hibernate-orm/index.md) with the `quarkus-security-jpa` extension.
>
> Both [Hibernate Reactive](/guides/hibernate-reactive/index.md) and [Hibernate Reactive with Panache](/guides/hibernate-reactive-panache/index.md) can be used with the `quarkus-security-jpa-reactive` extension.
>
> You must also add your preferred database connector library. The instructions in this example tutorial use a PostgreSQL database for the identity store.

### Create the Maven project

You can create a new Maven project with the Security Jakarta Persistence extension or add the extension to an existing Maven project. You can use either Hibernate ORM or Hibernate Reactive.

#### Creating new Maven project

- To create a new Maven project with the Jakarta Persistence extension, complete one of the following steps:
  - To create the Maven project with Hibernate ORM, use the following command:

**CLI**

```bash
quarkus create app org.acme:security-jpa-quickstart \
    --extension='security-jpa,jdbc-postgresql,rest,hibernate-orm-panache' \
    --no-code
cd security-jpa-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-jpa-quickstart \
    -Dextensions='security-jpa,jdbc-postgresql,rest,hibernate-orm-panache' \
    -DnoCode
cd security-jpa-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-jpa-quickstart"`

#### Adding Jakarta Persistence extension to existing project

- To add the Jakarta Persistence extension to an existing Maven project, complete one of the following steps:

\*\* To add the Security Jakarta Persistence extension to an existing Maven project with Hibernate ORM, run the following command from your project base directory: +

**CLI**

```bash
quarkus extension add security-jpa
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='security-jpa'
```

**Gradle**

```bash
./gradlew addExtension --extensions='security-jpa'
```

\*\* To add the Security Jakarta Persistence extension to an existing Maven project with Hibernate Reactive, run the following command from your project base directory: +

**CLI**

```bash
quarkus extension add security-jpa-reactive
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='security-jpa-reactive'
```

**Gradle**

```bash
./gradlew addExtension --extensions='security-jpa-reactive'
```

### Verify the quarkus-security-jpa dependency

After you have run either of the preceding commands to create the Maven project, verify that the `quarkus-security-jpa` dependency was added to your project build XML file.

- To verify the `quarkus-security-jpa` extension, check for the following configuration:

  **pom.xml**

  ```xml
  <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-security-jpa</artifactId>
  </dependency>
  ```


**build.gradle**

```gradle
implementation("io.quarkus:quarkus-security-jpa")
```

- To verify the `quarkus-security-jpa-reactive` extension, check for the following configuration:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-security-jpa-reactive</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-security-jpa-reactive")
```

## Write the application

- Secure the API endpoint to determine who can access the application by using one of the following approaches:

\*\* Implement the `/api/public` endpoint to allow all users access to the application. Add a regular Jakarta REST resource to your Java source code, as shown in the following code snippet: +

**`src/main/java/org/acme/security/jpa/PublicResource.java`**

```java
package org.acme.security.jpa;

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

\*\* Implement an /api/admin endpoint that can only be accessed by users who have the admin role. The source code for the `/api/admin` endpoint is similar, but instead, you use a `@RolesAllowed` annotation to ensure that only users granted the `admin` role can access the endpoint. Add a Jakarta REST resource with the following `@RolesAllowed` annotation: +

**`src/main/java/org/acme/security/jpa/AdminResource.java`**

```java
package org.acme.security.jpa;

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
    public String adminResource() {
         return "admin";
    }
}
```

\*\* Implement an `/api/users/me` endpoint that can only be accessed by users who have the `user` role. Use `SecurityContext` to get access to the currently authenticated `Principal` user and to return their username, all of which is retrieved from the database. +

**`src/main/java/org/acme/security/jpa/UserResource.java`**

```java
package org.acme.security.jpa;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;

@Path("/api/users")
public class UserResource {

    @GET
    @RolesAllowed("user")
    @Path("/me")
    public String me(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }
}
```

<a id="define-the-user-entity"></a>
## Define the user entity

Specify how security information is stored in the model by adding the following annotations to the `user` entity:

**`src/main/java/org/acme/security/jpa/User.java`**

```java
package org.acme.security.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@Entity
@Table(name = "test_user")
@UserDefinition <1>
public class User extends PanacheEntity {
    @Username <2>
    public String username;
    @Password <3>
    public String password;
    @Roles <4>
    public String role;

    /**
     * Adds a new user to the database
     * @param username the username
     * @param password the unencrypted password (it is encrypted with bcrypt)
     * @param role the comma-separated roles
     */
    public static void add(String username, String password, String role) { <5>
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = role;
        user.persist();
    }
}
```

1. The `@UserDefinition` annotation must be present on a single entity, either a regular Hibernate ORM entity or a Hibernate ORM with Panache entity.
2. Indicates the field used for the username.
3. Indicates the field used for the password. By default, it uses bcrypt-hashed passwords. You can configure it to use plain text or custom passwords.
4. Indicates the comma-separated list of roles added to the target principal representation attributes.
5. Provides a helper method to add users with bcrypt-hashed passwords. The `BcryptUtil.bcryptHash()` method:
   - Automatically generates a random salt for each password.
   - Hashes the password using bcrypt with 10 iterations (default).
   - Returns the hash in Modular Crypt Format (MCF), which includes the algorithm identifier, cost parameter, salt, and hash.

The `quarkus-security-jpa` extension only initializes if a single entity is annotated with `@UserDefinition`.

**Password hashing best practices:**

- Always pass plain text passwords to `BcryptUtil.bcryptHash()` - never pre-hash them.
- The bcrypt hash includes the salt, so no separate salt storage is needed.
- To verify passwords during authentication, the framework automatically uses `BcryptUtil.matches(plainPassword, hashedPassword)`.
- For custom iterations: `BcryptUtil.bcryptHash(password, iterationCount)` where higher iterations (12-14) provide more security but slower performance.

  - For more information about configuring passwords and roles, see [Configure the application](#configure-the-application).
  - For more information on hashing passwords and available options, see [Password storage and hashing](/guides/security-jpa/index.md#password-storage-and-hashing).

  Remember to set up the Panache and PostgreSQL JDBC driver. For more information, see [Setting up and configuring Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md#setting-up-and-configuring-hibernate-orm-with-panache).

  Hibernate Reactive Panache uses `io.quarkus.hibernate.reactive.panache.PanacheEntity` instead of `io.quarkus.hibernate.orm.panache.PanacheEntity`. For more information, see  [User file](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-jpa-reactive-quickstart/src/main/java/org/acme/elytron/security/jpa/reactive/User.java).

  <a id="configure-the-application"></a>
  ## Configure the application

  1. Enable the built-in Quarkus [Basic authentication](/guides/security-basic-authentication/index.md) mechanism by setting the `quarkus.http.auth.basic` property to `true`:

  `quarkus.http.auth.basic=true`

  When secure access is required, and no other authentication mechanisms are enabled, the built-in [Basic authentication](/guides/security-basic-authentication/index.md) of Quarkus is the fallback authentication mechanism. Therefore, in this tutorial, you do not need to set the property `quarkus.http.auth.basic` to `true`.

  1. Configure at least one data source in the `src/main/resources/application.properties` file so the `quarkus-security-jpa` extension can access your database. For example:

  .src/main/resources/application.properties

  ```properties
  quarkus.http.auth.basic=true

  %prod.quarkus.datasource.db-kind=postgresql
  %prod.quarkus.datasource.username=quarkus
  %prod.quarkus.datasource.password=quarkus
  %prod.quarkus.datasource.jdbc.url=jdbc:postgresql:quarkus

  quarkus.hibernate-orm.schema-management.strategy=drop-and-create
  ```


By adding the `%prod.` profile prefix, you ensure that the data source properties are only observed by an application running in production mode.

\+

1. To initialize the database with users and roles, implement the `Startup` class, as outlined in the following code snippet:

- The URLs of Reactive datasources that are used by the `quarkus-security-jpa-reactive` extension are set with the `quarkus.datasource.reactive.url` configuration property and not the `quarkus.datasource.jdbc.url` configuration property typically used by JDBC datasources.

  .src/main/resources/application.properties

  ```properties
  %prod.quarkus.datasource.reactive.url=vertx-reactive:postgresql://localhost:5431/security_jpa
  ```

- In this tutorial, a PostgreSQL database is used for the identity store. [Hibernate ORM](https://hibernate.org/orm/) automatically creates the database schema on startup. This approach is suitable for development but not for production. Therefore, adjustments are needed in a production environment.

  **`src/main/java/org/acme/security/jpa/Startup.java`**

  ```java
  package org.acme.security.jpa;

  import jakarta.enterprise.event.Observes;
  import jakarta.inject.Singleton;
  import jakarta.transaction.Transactional;

  import io.quarkus.runtime.StartupEvent;

  @Singleton
  public class Startup {
      @Transactional
      public void loadUsers(@Observes StartupEvent evt) {
          // reset and load all test users
          User.deleteAll();
          User.add("admin", "admin", "admin");
          User.add("user", "user", "user");
      }
  }
  ```


The preceding example demonstrates how the application can be protected and identities provided by the specified database.

> [!IMPORTANT]
> In a production environment, do not store plain text passwords. As a result, the `quarkus-security-jpa` defaults to using bcrypt-hashed passwords.

## Test your application in dev mode by using Dev Services for PostgreSQL

Complete the integration testing of your application in JVM and native modes by using [Dev Services for PostgreSQL](/guides/dev-services/index.md#databases) before you run your application in production mode.

Start by adding the following dependencies to your test project:

**pom.xml**

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

**build.gradle**

```gradle
testImplementation("io.rest-assured:rest-assured")
```

To run your application in dev mode:

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

In this scenario, `Dev Services for PostgreSQL` launches and configures a `PostgreSQL` test container. Make sure that either `Podman` or `Docker` is installed on your computer.

To write the integration test, use the following code sample:

**`src/test/java/org/acme/security/jpa/JpaSecurityRealmTest.java`**

```java
package org.acme.security.jpa;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JpaSecurityRealmTest {

    @Test
    void shouldAccessPublicWhenAnonymous() {
        get("/api/public")
                .then()
                .statusCode(HttpStatus.SC_OK);

    }

    @Test
    void shouldNotAccessAdminWhenAnonymous() {
        get("/api/admin")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

    }

    @Test
    void shouldAccessAdminWhenAdminAuthenticated() {
        given()
                .auth().preemptive().basic("admin", "admin")
                .when()
                .get("/api/admin")
                .then()
                .statusCode(HttpStatus.SC_OK);

    }

    @Test
    void shouldNotAccessUserWhenAdminAuthenticated() {
        given()
                .auth().preemptive().basic("admin", "admin")
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    void shouldAccessUserAndGetIdentityWhenUserAuthenticated() {
        given()
                .auth().preemptive().basic("user", "user")
                .when()
                .get("/api/users/me")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("user"));
    }
}
```

As you can see in this code sample, you do not need to start the test container from the test code.

To run these tests, choose `Press [r] to resume testing` option which is shown in the console after you started your application in dev mode.

> [!NOTE]
> When you start your application in dev mode, Dev Services for PostgreSQL launches a PostgreSQL dev mode container so that you can start developing your application. While developing your application, you can add and run tests individually by using the [Continuous Testing](/guides/continuous-testing/index.md) feature. Dev Services for PostgreSQL supports testing while you develop by providing a separate PostgreSQL test container that does not conflict with the dev mode container.

Alternatively, you can run these tests using Maven:

```shell
./mvnw test
```

## Test your application in production mode by using Curl or browser

To test your application using Curl or a browser, start a PostgreSQL server first. Then, compile and run your application in either JVM or native mode.

### Start the PostgreSQL server

```shell
docker run --rm=true --name security-getting-started -e POSTGRES_USER=quarkus \
           -e POSTGRES_PASSWORD=quarkus -e POSTGRES_DB=quarkus \
           -p 5432:5432 docker.io/library/postgres:18
```

### Compile and run the application

- Compile and run your Quarkus application by using one of the following methods:
  - 

    JVM mode

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

    ```shell
    java -jar target/quarkus-app/quarkus-run.jar
    ```

    \*\* Native mode

    1. Compile the application:

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

    1. Run the application:

    ```shell
    ./target/security-jpa-quickstart-1.0.0-SNAPSHOT-runner
    ```

    ### Access and test the application security with Curl

    When your application is running, you can access its endpoints by using one of the following Curl commands.

    - Connect to a protected endpoint anonymously:

    ```shell
    $ curl -i -X GET http://localhost:8080/api/public

    HTTP/1.1 200 OK
    Content-Length: 6
    Content-Type: text/plain;charset=UTF-8

    public
    ```

    - Connect to a protected endpoint anonymously:

    ```shell
    $ curl -i -X GET http://localhost:8080/api/admin

    HTTP/1.1 401 Unauthorized
    WWW-Authenticate: Basic
    ```

    - Connect to a protected endpoint as an authorized user:

    ```shell
    $ curl -i -X GET -u admin:admin http://localhost:8080/api/admin

    HTTP/1.1 200 OK
    Content-Length: 5
    Content-Type: text/plain;charset=UTF-8

    admin
    ```

    You can also access the same endpoint URLs by using a browser.

    ### Access and test the application security with the browser

    If you use a browser to connect to a protected resource anonymously, a Basic authentication form displays, prompting you to enter credentials.

    ### Results

    When you provide the credentials of an authorized user, for example, `admin:admin`, the Jakarta Persistence security extension authenticates and loads the user's roles. The `admin` user is authorized to access the protected resources.

    If a resource is protected with `@RolesAllowed("user")`, the user `admin` is not authorized to access the resource because it is not assigned to the "user" role, as shown in the following example:

    ```shell
    $ curl -i -X GET -u admin:admin http://localhost:8080/api/users/me

    HTTP/1.1 403 Forbidden
    ```

Finally, the user named `user` is authorized, and the security context contains the principal details, for example, the username.

```shell
$ curl -i -X GET -u user:user http://localhost:8080/api/users/me

HTTP/1.1 200 OK
Content-Length: 4
Content-Type: text/plain;charset=UTF-8

user
```

## What's next

You have successfully learned how to create and test a secure Quarkus application. This was achieved by integrating the built-in [Basic authentication](/guides/security-basic-authentication/index.md) in Quarkus with the Jakarta Persistence identity provider.

After completing this tutorial, you can explore more advanced security mechanisms in Quarkus. The following information shows you how to use `OpenID Connect` for secure single sign-on access to your Quarkus endpoints:

- [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md)
- [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md)

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
- [Quarkus Security architecture](/guides/security-architecture/index.md)
- [Other supported authentication mechanisms](/guides/security-authentication-mechanisms/index.md#other-supported-authentication-mechanisms)
- [Identity providers](/guides/security-identity-providers/index.md)
- [OIDC Bearer token authentication](/guides/security-oidc-bearer-token-authentication/index.md)
- [OIDC code flow mechanism for protecting web applications](/guides/security-oidc-code-flow-authentication/index.md)
- [Simplified Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md)
- [Using Hibernate ORM and Jakarta Persistence](/guides/hibernate-orm/index.md)
