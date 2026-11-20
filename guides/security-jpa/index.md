# Quarkus Security with Jakarta Persistence

You can configure your application to use Jakarta Persistence to store users' identities.

Quarkus provides a Jakarta Persistence identity provider, similar to the [JDBC identity provider](/guides/security-jdbc/index.md). Jakarta Persistence is suitable for use with the [Basic](/guides/security-basic-authentication/index.md) and [Form-based](/guides/security-authentication-mechanisms/index.md#form-auth) Quarkus Security mechanisms, which require username and password credentials.

> [!NOTE]
> The identity provider is available for both persistence stacks:
>
> - `quarkus-security-jpa` for applications that use Hibernate ORM.
> - `quarkus-security-jpa-reactive` for applications that use Hibernate Reactive.
>
> The entity annotations and the configuration described in this guide are the same for both extensions.

The Jakarta Persistence `IdentityProvider` creates a `SecurityIdentity` instance. During user authentication, this instance is used to verify and authorize access requests.

For a practical example, see the [Getting started with Security using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md) tutorial.

## Jakarta Persistence entity specification

Quarkus security offers a Jakarta Persistence integration to collect usernames, passwords, and roles and store them into Jakarta Persistence database entities.

The following Jakarta Persistence entity specification demonstrates how users' information needs to be stored in a Jakarta Persistence entity and correctly mapped so that Quarkus can retrieve this information from a database.

- The `@UserDefinition` annotation must be present on a Jakarta Persistence entity, regardless of whether [simplified Hibernate ORM with Panache](/guides/hibernate-orm-panache/index.md) is used or not.

- The `@Username` and `@Password` field types are always `String`.

- The `@Roles` field must either be `String`, `Collection<String>`, or a `Collection<X>`, where `X` is an entity class with a single `String` field annotated as `@RolesValue`.

- Each `String` role element type is parsed as a comma-separated list of roles.

The following example demonstrates storing security information by adding annotations to the `user` entity:

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
@Table(name = "test_user") <1>
@UserDefinition <2>
public class User extends PanacheEntity {
    @Username <3>
    public String username;
    @Password <4>
    public String password;
    @Roles <5>
    public String role;

    /**
     * Adds a new user to the database
     * @param username the username
     * @param password the unencrypted password (it is encrypted with bcrypt)
     * @param role the comma-separated roles
     */
    public static void add(String username, String password, String role) { <6>
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = role;
        user.persist();
    }
}
```

1. Table name. Do not define it as `user`, which is a restricted keyword in most databases.
2. The `@UserDefinition` annotation must be present on a single entity, either a regular Hibernate ORM entity or a Hibernate ORM with Panache entity.
3. Indicates the field used for the username.
4. Indicates the field used for the password. By default, `quarkus-security-jpa` uses bcrypt-hashed passwords, or you can configure plain text or custom passwords instead.
5. This indicates the comma-separated list of roles added to the target principal representation attributes.
6. This method lets you add users while hashing passwords with the proper `bcrypt` hash.

The `quarkus-security-jpa` extension initializes only if a single entity is annotated with `@UserDefinition`.

## Jakarta Persistence entity as storage of roles

Use the following example to store roles inside another Jakarta Persistence entity:

```java
@UserDefinition
@Table(name = "test_user")
@Entity
public class User extends PanacheEntity {
    @Username
    public String name;

    @Password
    public String pass;

    @ManyToMany
    @Roles
    public List<Role> roles = new ArrayList<>();
}

@Entity
public class Role extends PanacheEntity {

    @ManyToMany(mappedBy = "roles")
    public List<User> users;

    @RolesValue
    public String role;
}
```

> [!NOTE]
> This example demonstrates storing and accessing roles. To update an existing user or create a new one, annotate `public List<Role> roles` with `@Cascade(CascadeType.ALL)` or choose a specific `CascadeType`.

<a id="password-storage-and-hashing"></a>
## Password storage and hashing

When developing applications with Quarkus, you can decide how to manage password storage and hashing. You can keep the default password and hashing settings of Quarkus, or you can hash passwords manually.

With the default option, passwords are stored and hashed with [bcrypt](https://en.wikipedia.org/wiki/Bcrypt) under the [Modular Crypt Format](https://en.wikipedia.org/wiki/Crypt_\(C)) (MCF). While using MCF, the hashing algorithm, iteration count, and salt are stored as a part of the hashed value. As such, we do not need dedicated columns to keep them.

> [!NOTE]
> In cryptography, a salt is a name for random data used as an additional input to a one-way function that hashes data, a password, or a passphrase.

To represent passwords stored in the database that were hashed by different algorithms, create a class that implements `org.wildfly.security.password.PasswordProvider` as shown in the following example.

The following snippet shows how to set a custom password provider that represents a password that was hashed with the SHA256 hashing algorithm.

```java
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.PasswordType;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

@UserDefinition
@Table(name = "test_user")
@Entity
public class CustomPasswordUserEntity {
    @Id
    @GeneratedValue
    public Long id;

    @Column(name = "username")
    @Username
    public String name;

    @Column(name = "password")
    @Password(value = PasswordType.CUSTOM, provider = CustomPasswordProvider.class)
    public String pass;

    @Roles
    public String role;
}
```

```java
import jakarta.xml.bind.DatatypeConverter;

import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.SimpleDigestPassword;

import io.quarkus.security.jpa.PasswordProvider;

public class CustomPasswordProvider implements PasswordProvider {
    @Override
    public Password getPassword(String passwordInDatabase) {
        byte[] digest = DatatypeConverter.parseHexBinary(passwordInDatabase);

        // Let the security runtime know that this passwordInDatabase is hashed by using the SHA256 hashing algorithm
        return SimpleDigestPassword.createRaw(SimpleDigestPassword.ALGORITHM_SIMPLE_DIGEST_SHA_256, digest);
    }
}
```

> [!TIP]
> To quickly create a hashed password, use `String BcryptUtil.bcryptHash(String password)`, which defaults to creating a random salt and hashing in ten iterations. This method also allows specifying the number of iterations and salt used.

> [!WARNING]
> For applications running in a production environment, do not store passwords as plain text.
>
> However, it is possible to store passwords as plain text with the `@Password(PasswordType.CLEAR)` annotation when operating in a test environment.

> [!TIP]
> The [Hibernate Multitenancy](/guides/hibernate-orm/index.md#multitenancy) is supported, and you can store the user entity in a persistence unit with enabled multitenancy. However, if your `io.quarkus.hibernate.orm.runtime.tenant.TenantResolver` must access the `io.vertx.ext.web.RoutingContext` to resolve request details, you must disable proactive authentication. For more information about proactive authentication, see the Quarkus [Proactive authentication](/guides/security-proactive-authentication/index.md) guide.

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-security-jpa_quarkus-security-jpa-persistence-unit-name"></a> [`quarkus.security-jpa.persistence-unit-name`](#quarkus-security-jpa_quarkus-security-jpa-persistence-unit-name) config_property_copy_button:+++quarkus.security-jpa.persistence-unit-name+++[]<br><br>Selects the Hibernate ORM persistence unit. Default persistence unit is used when no value is specified.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_SECURITY_JPA_PERSISTENCE_UNIT_NAME+++[] | string | `<default>` |

## References

- [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md)
- [Identity providers](/guides/security-identity-providers/index.md)
- [Quarkus Security overview](/guides/security-overview/index.md)
