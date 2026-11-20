# Quarkus 2.5.0.Final released - GraalVM/Mandrel 21.3 and usability improvements all over the place

Today, we announce the availability of Quarkus 2.5.0.Final which comes with the following main improvements:

- Upgrade to GraalVM/Mandrel 21.3
- Support for JPA entity listeners for Hibernate ORM in native mode
- Ability to add HTTP headers to responses
- Various usability improvements in extensions and our dev mode/testing infrastructure

## Migration Guide

To migrate from 2.4, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.5).

## What's new?

### GraalVM/Mandrel 21.3

GraalVM/Mandrel 21.3 is the recommended version for Quarkus 2.5.

### Hibernate ORM

Quarkus 2.5 adds support for JPA entity listeners, including in native mode.

Until now, they were not really supported and were working in JVM mode only.

### Adding headers with quarkus.http.header

With `quarkus.http.header`, you can easily add headers to your HTTP responses:

```properties
quarkus.http.header."My-Header".value=Header value
```

See the [HTTP reference documentation](http://quarkus.io/guides/http-reference#additional-http-headers) for more information.

### Security

Some well known providers such as `GitHub` or `LinkedIn` are not `OpenId Connect` but `OAuth2` providers which support the `authorization code flow`, for example, [GitHub OAuth2](https://docs.github.com/en/developers/apps/building-oauth-apps/authorizing-oauth-apps) and [LinkedIn OAuth2](https://docs.microsoft.com/en-us/linkedin/shared/authentication/authorization-code-flow).

Quarkus 2.5 supports these providers as explained in the [Using OpenID Connect (OIDC) to Protect Web Applications using Authorization Code Flow](/guides/security-openid-connect-web-authentication#integration-with-github-and-other-oauth2-providers) guide.

Using RESTEasy Reactive, it is now possible to filter the JSON output based on the role of the user using the `@SecureField` annotation:

```java
public class Person {

    private String firstName;

    @SecureField(rolesAllowed = "admin")
    private String lastName;
}
```

In the example above, the `lastName` field will be included in the JSON output only if the user has the `admin` role.

### Flyway

Flyway was updated to 8.0.

It is now possible to generate initial migration scripts from the Hibernate ORM generated schema from the Dev UI.

### Reactive Microsoft SQL Server client

The Reactive Microsoft SQL Server client now supports encryption.

### Infinispan

The Infinispan client extension has been upgraded to 13.0.0.Final.

The development/test infrastructure for Infinispan got major improvements in the form of a test resource and a dev service: you can automatically start Infinispan in dev mode and in your tests.

### Oracle

A Dev Service for Oracle has been added: start your Oracle database automatically in dev and test mode.

### Camel 3.13.0

Camel Quarkus extensions have been upgraded to Camel 3.13.0 version, that brings a list of improvements and fixes. Check the [Camel 3.13.0 release notes](https://camel.apache.org/releases/release-3.13.0/).

### Kubernetes extensions

It is now possible to define service bindings using Quarkus configuration as explained in the [Deploying to Kubernetes](/guides/deploying-to-kubernetes#service_binding) guide.

### Quarkus Maven plugin - Go offline

The default Maven feature to go offline doesn't download the full set of Quarkus artifacts as some artifacts (the deployment/build ones) are resolved by the Quarkus Maven plugin and are not part of the regular dependencies.

Quarkus 2.5 introduces the `quarkus:go-offline` goal to cover this requirement.

Note that the same feature will come to the Gradle plugin very soon.

### Testing

When testing command line applications using `@QuarkusMain`, you can define the command line parameters in a `QuarkusTestProfile` by implementing `commandLineParameters()`.

## Contributors

The Quarkus community is growing and has now [560 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 2.5 release, thanks to acevedor, Alexey Loubyansky, Andy Damevin, Arne Mejlholm, Bill Burke, Carles Arnal, Chris Laprun, Clement Escoffier, Daniel Petisme, Davide, Dheeraj Nalluri, Dmitrii K, Emelia Smith, Eric Deandrea, Erin Schnabel, Falko Modler, fhavel, Foivos Zakkak, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Gilvan Filho, Guillaume Le Floch, Guillaume Smet, Ioannis Canellos, Jan Martiška, Jose, Juan Zuriaga, Justin Lee, Kannan, Katia Aresti, Ken Finnigan, Kevin Wooten, Knut Wannheden, Ladislav Thon, lducamp, Loïc Mathieu, Marc Nuri, Marcin Czeczko, Martin Kouba, Michael Anstis, Michael Schnitzler, Michael Simons, Michal Karm Babacek, Michał Szynkiewicz, Nathan Erwin, NetoDevel, Omar Al-Safi, orekyuu, Ozan Gunalp, Phillip Krüger, Richard Gomez, Robbie Gemmell, Robert Stupp, Roberto Cortez, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, SIX Douglas, Stuart Douglas, tarilabs, Thomas Haines, Thomas Segismont, Vincent Sevel, Vincent van Dam, Wippermueller, Frank, xstefank and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
