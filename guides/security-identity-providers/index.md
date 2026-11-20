# Identity providers

In the Quarkus Security framework, identity providers play a crucial role in authentication and authorization by verifying user identities. `IdentityProvider` creates a `SecurityIdentity` instance, which gets used during user authentication to verify and authorize access requests to your Quarkus application.

<a id="identity-providers"></a>
`IdentityProvider` converts the authentication credentials provided by `HttpAuthenticationMechanism` to a `SecurityIdentity` instance.

Some extensions, such as the ones for OIDC and SmallRye JWT, include inline `IdentityProvider` implementations specific to the supported authentication flow. For example, `quarkus-oidc` uses its own `IdentityProvider` to convert a token to a `SecurityIdentity` instance.

If you use Basic or form-based authentication, you must add an `IdentityProvider` instance to convert a username and password to a `SecurityIdentity` instance.

To get started with security in Quarkus, consider combining the Quarkus built-in Basic HTTP authentication with the Jakarta Persistence identity provider to enable role-based access control (RBAC).

For more information about Basic authentication, its mechanisms, and related identity providers, see the following resources:

- [Quarkus Security with Jakarta Persistence](/guides/security-jpa/index.md)
- 
  - [Getting started with Security by using Basic authentication and Jakarta Persistence](/guides/security-getting-started-tutorial/index.md)
- [Form-based authentication](/guides/security-authentication-mechanisms/index.md#form-auth)
- [Using security with JDBC](/guides/security-jdbc/index.md)
- [Using security with an LDAP realm](/guides/security-ldap/index.md)
- [Quarkus Security overview](/guides/security-overview/index.md)
