# Quarkus 3.25 - Virtual threads for GraphQL, Micrometer update and various new security-related features

Today, we released Quarkus 3.25 with the following significant new features:

- [#47802](https://github.com/quarkusio/quarkus/pull/47802) - Virtual threads support for SmallRye GraphQL
- [#48482](https://github.com/quarkusio/quarkus/pull/48482) - Security - Provide a fluent API to set up path-specific authorization programmatically
- [#48296](https://github.com/quarkusio/quarkus/pull/48296) - OIDC Client: Add periodic asynchronous tokens refresh for performance critical applications
- [#48734](https://github.com/quarkusio/quarkus/pull/48734) - Support for OAuth2 Protected Resource Metadata

We are now preparing 3.26, which will be the base for our new LTS, 3.27 LTS. The feature freeze for the next LTS is on August 12th.

## Update

To update to Quarkus 3.25, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.25.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.25 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.25).

## What's new?

### Virtual threads support for SmallRye GraphQL

We have had a Quarkus virtual threads story for a long time: Quarkus 3.6 introduced virtual threads support for Quarkus REST.

Since then, a lot more features got support for virtual threads added e.g. Quarkus Messaging, gRPC.

In Quarkus 3.25, we introduce the support of virtual threads in the [SmallRye GraphQL extension](https://quarkus.io/guides/smallrye-graphql#runonvirtualthread).

### Security

#### Fluent API to set up path-specific authorization

To defined path-specific authorization, you previously had to use configuration properties such as:

```properties
quarkus.http.auth.permission.permit1.paths=/public/*
quarkus.http.auth.permission.permit1.policy=permit
quarkus.http.auth.permission.permit1.methods=GET

quarkus.http.auth.permission.deny1.paths=/forbidden
quarkus.http.auth.permission.deny1.policy=deny

quarkus.http.auth.permission.roles1.paths=/roles-secured/*,/other/*,/api/*
quarkus.http.auth.permission.roles1.policy=role-policy1
quarkus.http.auth.policy.role-policy1.roles-allowed=user,admin
```

It is still a possibility but you now also have the option to use a programmatic API if it is more convenient for you. For instance:

```java
public class HttpSecurityConfiguration {
    void configure(@Observes HttpSecurity httpSecurity) {
        httpSecurity
                .get("/public/*").permit()
                .path("/roles-secured/*", "/other/*", "/api/*").roles("admin", "user")
                .path("/forbidden").authorization().deny();
    }
}
```

#### Periodic asynchronous tokens refresh

The OIDC Client currently refreshes the token during the current request execution, which might be impractical for high performance applications.

Quarkus 3.25 introduces the ability to refresh the tokens in the background by using the `quarkus.oidc-client.refresh-interval` configuration property.

#### Support for OAuth2 Protected Resource Metadata

Quarkus 3.25 adds initial support for [RFC 9728's OAuth 2.0 Protected Resource Metadata](https://datatracker.ietf.org/doc/rfc9728/).

See our [documentation](https://quarkus.io/guides/security-oidc-expanded-configuration#resource-metadata-properties) for all the details about this new feature.

#### Legacy config classes

We have started sunsetting the legacy config classes support and, with 3.25, you can't build an extension using legacy config classes anymore.

In 3.26, the support for legacy config classes will be entirely dropped from Quarkus.

### Platform component upgrades

#### Quarkus LangChain4j

Quarkus LangChain4j is now part of the Quarkus Platform.

The first version of Quarkus LangChain4j we included is `1.1.0`.

#### Camel Quarkus

Camel Quarkus has been upgraded to 3.25.0.

## Full changelog

You can get the full changelog of [3.25.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.25.0.CR1) and [3.25.0](https://github.com/quarkusio/quarkus/releases/tag/3.25.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [1103 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.25 release, thanks to Alex Martel, Alexey Loubyansky, Anis Ikram, Antonio Macrì, Blaz Mrak, Bruno Baptista, Chris Laprun, Christian, Christian Beikov, Clement Escoffier, comrt, David M. Lloyd, Erik Mattheis, Foivos Zakkak, Fouad Almalki, Francesco Nigro, George Gastaldi, Georgios Andrianakis, Guillaume LECLERC, Guillaume Smet, Holly Cummins, Ilya Korennoy, Inaki Villar, Ivan Petkov, Izziizzi-ux, James Netherton, Jan Martiska, Jonathan Dowland, Julien Ponge, Katia Aresti, Kevin Wooten, Ladislav Thon, Lars Andringa, Lorenzo Vannucchi, Marco Belladelli, Marco Bungart, marko-bekhta, Martin Bartoš, Martin Kouba, Matej Novotny, Matej Vašek, Matheus Oliveira da Silva, Max Rydahl Andersen, melloware, Michal Vavřík, Nicola Concetti, Ozan Gunalp, Paulo Casaes, Peter Palaga, Phillip Krüger, Pierre Beitz, Ramon Boss, Robert Pospisil, Roberto Cortez, Rostislav Svoboda, Sebastian Zieja, Sergey Beryozkin, Severin Gehwolf, Sopka, Stéphane Épardaud, Teymur Babayev, Thomas Canava, Vincent Sevel, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
