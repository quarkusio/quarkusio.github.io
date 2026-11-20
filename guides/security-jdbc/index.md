# Using security with JDBC

This guide demonstrates how your Quarkus application can use a database to store your user identities.

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

The `/api/public` endpoint can be accessed anonymously. The `/api/admin` endpoint is protected with RBAC (Role-Based Access Control) where only users granted with the `admin` role can access. At this endpoint, we use the `@RolesAllowed` annotation to declaratively enforce the access constraint. The `/api/users/me` endpoint is also protected with RBAC (Role-Based Access Control) where only users granted with the `user` role can access. As a response, it returns a JSON document with details about the user.

## Solution

We recommend that you follow the instructions in the next sections and create the application step by step. However, you can go right to the completed example.

Clone the Git repository: `git clone https://github.com/quarkusio/quarkus-quickstarts.git`, or download an [archive](https://github.com/quarkusio/quarkus-quickstarts/archive/main.zip).

The solution is located in the `security-jdbc-quickstart` [directory](https://github.com/quarkusio/quarkus-quickstarts/tree/main/security-jdbc-quickstart).

## Creating the Maven Project

First, we need a new project. Create a new project with the following command:

**CLI**

```bash
quarkus create app org.acme:security-jdbc-quickstart \
    --extension='elytron-security-jdbc,jdbc-postgresql,rest' \
    --no-code
cd security-jdbc-quickstart
```

To create a Gradle project, add the `--gradle` or `--gradle-kotlin-dsl` option.

For more information about how to install and use the Quarkus CLI, see the [Quarkus CLI](/guides/cli-tooling/index.md) guide.

**Maven**

```bash
mvn io.quarkus.platform:quarkus-maven-plugin:3.40.1.2:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=security-jdbc-quickstart \
    -Dextensions='elytron-security-jdbc,jdbc-postgresql,rest' \
    -DnoCode
cd security-jdbc-quickstart
```

To create a Gradle project, add the `-DbuildTool=gradle` or `-DbuildTool=gradle-kotlin-dsl` option.

For Windows users:

- If using Powershell, wrap `-D` parameters in double quotes e.g. `"-DprojectArtifactId=security-jdbc-quickstart"`

> [!NOTE]
> Don't forget to add the database connector library of choice. Here we are using PostgreSQL as identity store.

This command generates a new project, importing the `elytron-security-jdbc` extension which is an [`wildfly-elytron-realm-jdbc`](https://docs.wildfly.org/17/WildFly_Elytron_Security.html#jdbc-security-realm) adapter for Quarkus applications.

If you already have your Quarkus project configured, you can add the `elytron-security-jdbc` extension to your project by running the following command in your project base directory:

**CLI**

```bash
quarkus extension add elytron-security-jdbc
```

**Maven**

```bash
./mvnw quarkus:add-extension -Dextensions='elytron-security-jdbc'
```

**Gradle**

```bash
./gradlew addExtension --extensions='elytron-security-jdbc'
```

This will add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elytron-security-jdbc</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-elytron-security-jdbc")
```

## Writing the application

Let's start by implementing the `/api/public` endpoint. As you can see from the source code below, it is just a regular Jakarta REST resource:

```java
package org.acme.security.jdbc;

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

The source code for the `/api/admin` endpoint is also very simple. The main difference here is that we are using a `@RolesAllowed` annotation to make sure that only users granted with the `admin` role can access the endpoint:

```java
package org.acme.security.jdbc;

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

Finally, let's consider the `/api/users/me` endpoint. As you can see from the source code below, we are trusting only users with the `user` role. We are using `SecurityContext` to get access to the current authenticated Principal, and we return the user's name. This information is loaded from the database.

```java
package org.acme.security.jdbc;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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

### Configuring the Application

The `elytron-security-jdbc` extension requires at least one datasource to access to your database.

```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=quarkus
quarkus.datasource.password=quarkus
quarkus.datasource.jdbc.url=jdbc:postgresql:elytron-security-jdbc
```

In our context, we are using PostgreSQL as identity store, and we initialize the database with users and roles. We will use the salted and hashed version of `password` as a password in this example. We can use the `BcryptUtil` class to generate passwords in the Modular Crypt Format (MCF).

```sql
CREATE TABLE test_user (
  id INT,
  username VARCHAR(255),
  password VARCHAR(255),
  role VARCHAR(255)
);

INSERT INTO test_user (id, username, password, role) VALUES (1, 'admin', '$2a$10$Uc.SZ0hvGJQlYdsAp7be1.lFjmOnc7aAr4L0YY3/VN3oK.F8zJHRG', 'admin');
INSERT INTO test_user (id, username, password, role) VALUES (2, 'user','$2a$10$Uc.SZ0hvGJQlYdsAp7be1.lFjmOnc7aAr4L0YY3/VN3oK.F8zJHRG', 'user');
```

When signing up new users, we can encrypt their password as follows:

```java
package org.acme.security.jdbc;

import io.quarkus.elytron.security.common.BcryptUtil;

public class AccountService {

    public void signupUser(String username, String password) {
        String encryptedPassword = BcryptUtil.bcryptHash(password);

        // store user with the encrypted password in the database
    }
}
```

We can now configure the Elytron JDBC Realm.

```properties
quarkus.security.jdbc.enabled=true
quarkus.security.jdbc.principal-query.sql=SELECT u.password, u.role FROM test_user u WHERE u.username=? <1>
quarkus.security.jdbc.principal-query.bcrypt-password-mapper.enabled=true <2>
quarkus.security.jdbc.principal-query.bcrypt-password-mapper.password-index=1
quarkus.security.jdbc.principal-query.attribute-mappings.0.index=2 <3>
quarkus.security.jdbc.principal-query.attribute-mappings.0.to=groups
```

1. We define a parameterized SQL statement (with exactly 1 parameter) which should return the user's password plus any additional information you want to load.
2. The password mapper is configured with the position of the password field in the `SELECT` fields. The hash is stored in the Modular Crypt Format (MCF) because the salt and iteration count indexes are set to `-1` by default. You can override them in order to decompose each element into three separate columns.
3. We use `attribute-mappings` to bind the `SELECT` projection fields (i.e. `u.role` here) to the target Principal representation attributes.

The `elytron-security-jdbc` extension requires at least one principal query to authenticate the user and its identity.

> [!NOTE]
> In the `principal-query` configuration all the `index` properties start at 1 (rather than 0).

## Testing the Application

The application is now protected and the identities are provided by our database. The very first thing to check is to ensure the anonymous access works.

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
$ curl -i -X GET -u admin:password http://localhost:8080/api/admin
HTTP/1.1 200 OK
Content-Length: 5
Content-Type: text/plain;charset=UTF-8

admin%
```

By providing the `admin:password` credentials, the extension authenticated the user and loaded their roles. The `admin` user is authorized to access to the protected resources.

The user `admin` should be forbidden to access a resource protected with `@RolesAllowed("user")` because it doesn't have this role.

```shell
$ curl -i -X GET -u admin:password http://localhost:8080/api/users/me
HTTP/1.1 403 Forbidden
Content-Length: 34
Content-Type: text/html;charset=UTF-8

Forbidden%
```

Finally, using the user `user` works and the security context contains the principal details (username for instance).

```shell
$ curl -i -X GET -u user:password http://localhost:8080/api/users/me
HTTP/1.1 200 OK
Content-Length: 4
Content-Type: text/plain;charset=UTF-8

user%
```

## Advanced Configuration

This guide only covered an easy use case, the extension offers multiple datasources, multiple principal queries configuration as well as a bcrypt password mapper.

quarkus.datasource.db-kind=postgresql quarkus.datasource.username=quarkus quarkus.datasource.password=quarkus quarkus.datasource.jdbc.url=jdbc:postgresql:multiple-data-sources-users

quarkus.datasource.permissions.db-kind=postgresql quarkus.datasource.permissions.username=quarkus quarkus.datasource.permissions.password=quarkus quarkus.datasource.permissions.jdbc.url=jdbc:postgresql:multiple-data-sources-permissions

quarkus.security.jdbc.enabled=true quarkus.security.jdbc.principal-query.sql=SELECT u.password FROM test_user u WHERE u.username=? quarkus.security.jdbc.principal-query.bcrypt-password-mapper.enabled=true quarkus.security.jdbc.principal-query.bcrypt-password-mapper.password-index=1

quarkus.security.jdbc.principal-query.roles.sql=SELECT r.role_name FROM test_role r, test_user_role ur WHERE ur.username=? AND ur.role_id = r.id quarkus.security.jdbc.principal-query.roles.datasource=permissions quarkus.security.jdbc.principal-query.roles.attribute-mappings.0.index=1 quarkus.security.jdbc.principal-query.roles.attribute-mappings.0.to=groups

<a id="configuration-reference"></a>
## Configuration Reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-realm-name"></a> [`quarkus.security.jdbc.realm-name`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-realm-name) config_property_copy_button:+++quarkus.security.jdbc.realm-name+++[]<br><br>The realm name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_REALM_NAME+++[] | string | `Quarkus` |
| [lock] <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-enabled"></a> [`quarkus.security.jdbc.enabled`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-enabled) config_property_copy_button:+++quarkus.security.jdbc.enabled+++[]<br><br>If the properties store is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-sql"></a> [`quarkus.security.jdbc.principal-query.sql`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-sql) config_property_copy_button:+++quarkus.security.jdbc.principal-query.sql+++[]<br><br>The sql query to find the password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_SQL+++[] | string |  |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-datasource"></a> [`quarkus.security.jdbc.principal-query.datasource`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-datasource) config_property_copy_button:+++quarkus.security.jdbc.principal-query.datasource+++[]<br><br>The data source to use<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_DATASOURCE+++[] | string |  |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-clear-password-mapper-enabled"></a> [`quarkus.security.jdbc.principal-query.clear-password-mapper.enabled`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-clear-password-mapper-enabled) config_property_copy_button:+++quarkus.security.jdbc.principal-query.clear-password-mapper.enabled+++[]<br><br>If the clear-password-mapper is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_CLEAR_PASSWORD_MAPPER_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-clear-password-mapper-password-index"></a> [`quarkus.security.jdbc.principal-query.clear-password-mapper.password-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-clear-password-mapper-password-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query.clear-password-mapper.password-index+++[]<br><br>The index (1 based numbering) of the column containing the clear password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_CLEAR_PASSWORD_MAPPER_PASSWORD_INDEX+++[] | int | `1` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-enabled"></a> [`quarkus.security.jdbc.principal-query.bcrypt-password-mapper.enabled`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-enabled) config_property_copy_button:+++quarkus.security.jdbc.principal-query.bcrypt-password-mapper.enabled+++[]<br><br>If the bcrypt-password-mapper is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_BCRYPT_PASSWORD_MAPPER_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-password-index"></a> [`quarkus.security.jdbc.principal-query.bcrypt-password-mapper.password-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-password-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query.bcrypt-password-mapper.password-index+++[]<br><br>The index (1 based numbering) of the column containing the password hash<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_BCRYPT_PASSWORD_MAPPER_PASSWORD_INDEX+++[] | int | `0` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-hash-encoding"></a> [`quarkus.security.jdbc.principal-query.bcrypt-password-mapper.hash-encoding`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-hash-encoding) config_property_copy_button:+++quarkus.security.jdbc.principal-query.bcrypt-password-mapper.hash-encoding+++[]<br><br>A string referencing the password hash encoding ("BASE64" or "HEX")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_BCRYPT_PASSWORD_MAPPER_HASH_ENCODING+++[] | `base64`, `hex` | `base64` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-salt-index"></a> [`quarkus.security.jdbc.principal-query.bcrypt-password-mapper.salt-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-salt-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query.bcrypt-password-mapper.salt-index+++[]<br><br>The index (1 based numbering) of the column containing the Bcrypt salt. The default value of `-1` implies that the salt is stored in the password column using the Modular Crypt Format (MCF) standard.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_BCRYPT_PASSWORD_MAPPER_SALT_INDEX+++[] | int | `-1` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-salt-encoding"></a> [`quarkus.security.jdbc.principal-query.bcrypt-password-mapper.salt-encoding`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-salt-encoding) config_property_copy_button:+++quarkus.security.jdbc.principal-query.bcrypt-password-mapper.salt-encoding+++[]<br><br>A string referencing the salt encoding ("BASE64" or "HEX")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_BCRYPT_PASSWORD_MAPPER_SALT_ENCODING+++[] | `base64`, `hex` | `base64` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-iteration-count-index"></a> [`quarkus.security.jdbc.principal-query.bcrypt-password-mapper.iteration-count-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-bcrypt-password-mapper-iteration-count-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query.bcrypt-password-mapper.iteration-count-index+++[]<br><br>The index (1 based numbering) of the column containing the Bcrypt iteration count. The default value of `-1` implies that the iteration count is stored in the password column using the Modular Crypt Format (MCF) standard.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_BCRYPT_PASSWORD_MAPPER_ITERATION_COUNT_INDEX+++[] | int | `-1` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-attribute-mappings-attribute-mappings-index"></a> [`quarkus.security.jdbc.principal-query.attribute-mappings."attribute-mappings".index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-attribute-mappings-attribute-mappings-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query.attribute-mappings."attribute-mappings".index+++[]<br><br>The index (1 based numbering) of column to map<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__INDEX+++[] | int | `0` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-attribute-mappings-attribute-mappings-to"></a> [`quarkus.security.jdbc.principal-query.attribute-mappings."attribute-mappings".to`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-attribute-mappings-attribute-mappings-to) config_property_copy_button:+++quarkus.security.jdbc.principal-query.attribute-mappings."attribute-mappings".to+++[]<br><br>The target attribute name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY_ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__TO+++[] | string | required [exclamation circle] |
| <a id="quarkus-elytron-security-jdbc_section_quarkus-security-jdbc-principal-query"></a> [Named queries](#quarkus-elytron-security-jdbc_section_quarkus-security-jdbc-principal-query) | Type | Default |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-sql"></a> [`quarkus.security.jdbc.principal-query."query-name".sql`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-sql) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".sql+++[]<br><br>The sql query to find the password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__SQL+++[] | string |  |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-datasource"></a> [`quarkus.security.jdbc.principal-query."query-name".datasource`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-datasource) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".datasource+++[]<br><br>The data source to use<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__DATASOURCE+++[] | string |  |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-attribute-mappings-attribute-mappings-index"></a> [`quarkus.security.jdbc.principal-query."query-name".attribute-mappings."attribute-mappings".index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-attribute-mappings-attribute-mappings-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".attribute-mappings."attribute-mappings".index+++[]<br><br>The index (1 based numbering) of column to map<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__INDEX+++[] | int | `0` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-attribute-mappings-attribute-mappings-to"></a> [`quarkus.security.jdbc.principal-query."query-name".attribute-mappings."attribute-mappings".to`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-attribute-mappings-attribute-mappings-to) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".attribute-mappings."attribute-mappings".to+++[]<br><br>The target attribute name<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__ATTRIBUTE_MAPPINGS__ATTRIBUTE_MAPPINGS__TO+++[] | string | required [exclamation circle] |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-clear-password-mapper-enabled"></a> [`quarkus.security.jdbc.principal-query."query-name".clear-password-mapper.enabled`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-clear-password-mapper-enabled) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".clear-password-mapper.enabled+++[]<br><br>If the clear-password-mapper is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__CLEAR_PASSWORD_MAPPER_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-clear-password-mapper-password-index"></a> [`quarkus.security.jdbc.principal-query."query-name".clear-password-mapper.password-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-clear-password-mapper-password-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".clear-password-mapper.password-index+++[]<br><br>The index (1 based numbering) of the column containing the clear password<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__CLEAR_PASSWORD_MAPPER_PASSWORD_INDEX+++[] | int | `1` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-enabled"></a> [`quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.enabled`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-enabled) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.enabled+++[]<br><br>If the bcrypt-password-mapper is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__BCRYPT_PASSWORD_MAPPER_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-password-index"></a> [`quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.password-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-password-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.password-index+++[]<br><br>The index (1 based numbering) of the column containing the password hash<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__BCRYPT_PASSWORD_MAPPER_PASSWORD_INDEX+++[] | int | `0` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-hash-encoding"></a> [`quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.hash-encoding`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-hash-encoding) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.hash-encoding+++[]<br><br>A string referencing the password hash encoding ("BASE64" or "HEX")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__BCRYPT_PASSWORD_MAPPER_HASH_ENCODING+++[] | `base64`, `hex` | `base64` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-salt-index"></a> [`quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.salt-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-salt-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.salt-index+++[]<br><br>The index (1 based numbering) of the column containing the Bcrypt salt. The default value of `-1` implies that the salt is stored in the password column using the Modular Crypt Format (MCF) standard.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__BCRYPT_PASSWORD_MAPPER_SALT_INDEX+++[] | int | `-1` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-salt-encoding"></a> [`quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.salt-encoding`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-salt-encoding) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.salt-encoding+++[]<br><br>A string referencing the salt encoding ("BASE64" or "HEX")<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__BCRYPT_PASSWORD_MAPPER_SALT_ENCODING+++[] | `base64`, `hex` | `base64` |
| <a id="quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-iteration-count-index"></a> [`quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.iteration-count-index`](#quarkus-elytron-security-jdbc_quarkus-security-jdbc-principal-query-query-name-bcrypt-password-mapper-iteration-count-index) config_property_copy_button:+++quarkus.security.jdbc.principal-query."query-name".bcrypt-password-mapper.iteration-count-index+++[]<br><br>The index (1 based numbering) of the column containing the Bcrypt iteration count. The default value of `-1` implies that the iteration count is stored in the password column using the Modular Crypt Format (MCF) standard.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__QUERY_NAME__BCRYPT_PASSWORD_MAPPER_ITERATION_COUNT_INDEX+++[] | int | `-1` |

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
