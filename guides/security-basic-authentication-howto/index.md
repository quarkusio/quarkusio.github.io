# Enable Basic authentication

Enable Basic authentication to require a username and password before users can access your Quarkus application. For more information, see [Basic authentication](/guides/security-basic-authentication/index.md).

## Prerequisites

- You have installed at least one extension that provides an `IdentityProvider` to verify username and password credentials and map them to a `SecurityIdentity`. For example:

\*\* [Quarkus Security Jakarta Persistence extensions (`quarkus-security-jpa` or `quarkus-security-jpa-reactive`)](/guides/security-jpa/index.md) \*\* [Elytron security properties file extension `(quarkus-elytron-security-properties-file)`](/guides/security-properties/index.md) \*\* [Elytron security JDBC extension `(quarkus-elytron-security-jdbc)`](/guides/security-jdbc/index.md)

## Procedure

1. In the `application.properties` file, set the `quarkus.http.auth.basic` property to `true`.

   ```properties
   quarkus.http.auth.basic=true
   ```


1. Configure an identity provider to verify the username and password against your user store. For a production application, configure one of the identity provider extensions listed in the Prerequisites, for example, by storing users in a database with the Jakarta Persistence extension. For more information, see the [Identity providers](/guides/security-identity-providers/index.md) guide.

1. **For testing only:** In a non-production environment, you can use the embedded realm provided by the `quarkus-elytron-security-properties-file` extension instead of a production identity provider.
   1. To enable authentication for the embedded realm, set the `quarkus.security.users.embedded.enabled` property to `true`.

      ```properties
      quarkus.security.users.embedded.enabled=true
      ```

.. You can also configure the required user credentials: user name, password, and roles. For example: +

```properties
quarkus.http.auth.basic=true
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.alice=alice
quarkus.security.users.embedded.users.bob=bob
quarkus.security.users.embedded.roles.alice=admin
quarkus.security.users.embedded.roles.bob=user
```

where:

- The user, `alice`, has `alice` as their password and `admin` as their role.
- The user, `bob`, has `bob` as their password and `user` as their role.

## Verification

1. Start your application.

1. Send a request to a secured endpoint *without* credentials and confirm that the application returns a `401 Unauthorized` response:

   ```shell
   $ curl -i -X GET http://localhost:8080/<your_secured_endpoint>

   HTTP/1.1 401 Unauthorized
   WWW-Authenticate: Basic
   ```


1. Send the same request *with* a valid username and password and confirm that the application returns a `200 OK` response:

   ```shell
   $ curl -i -X GET -u <username>:<password> http://localhost:8080/<your_secured_endpoint>

   HTTP/1.1 200 OK
   ```


## Next steps

For a more detailed walk-through that shows you how to configure Basic authentication together with Jakarta Persistence for storing user credentials in a database, see the [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md) guide.

## References

- [Quarkus Security overview](/guides/security-overview/index.md)
- [Identity Providers](/guides/security-identity-providers/index.md)
- [Configuring User Information in application.properties](/guides/security-testing/index.md#configuring-user-information)
- [Basic authentication](/guides/security-basic-authentication/index.md)
