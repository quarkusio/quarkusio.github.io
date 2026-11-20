# Quarkus 3.19 - UBI 9 images, Micrometer to OpenTelemetry bridge, JEP 483 new AOT cache...

Today, we released Quarkus 3.19. Quarkus 3.19 is our first step towards the release of our new 3.20 LTS, as 3.20 LTS will be based on the 3.19 branch.

You can consider that the next LTS is feature complete and we encourage you to adopt 3.19 and share your feedback.

It comes with a lot of enhancements and the following new features:

- [#45446](https://github.com/quarkusio/quarkus/issues/45446) - Migrate core extensions to @ConfigMapping
- [#45973](https://github.com/quarkusio/quarkus/pull/45973) - Switch to UBI 9 by default
- [#43831](https://github.com/quarkusio/quarkus/pull/43831) - Micrometer to OpenTelemetry bridge
- [#46169](https://github.com/quarkusio/quarkus/pull/46169) - Introduce support for JEP 483's new AOT cache
- [#45809](https://github.com/quarkusio/quarkus/pull/45809) - WebSockets Next: Allow to send authorization headers from web browsers using JavaScript clients
- [#45704](https://github.com/quarkusio/quarkus/pull/45704) - WebSockets Next: Support permission checkers
- [#45891](https://github.com/quarkusio/quarkus/pull/45891) - Support for OAuth2 Demonstrating Proof of Possession

## Update

To update to Quarkus 3.19, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.19.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.19 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.19).

## What's new?

### Migration to @ConfigMapping

In 2022, we introduced in Quarkus a new configuration infrastructure for extensions based on interfaces annotated with `@ConfigMapping`. They were a replacement for the legacy config classes that were used in Quarkus extensions, which came with several issues and were specific to extensions (you couldn't use them in applications).

The new `@ConfigMapping` infrastructure unifies extension and application configuration on the same infrastructure.

In Quarkus 3.19, we moved all the core extensions to this new infrastructure (except for some classes that were kept for compatibility purposes). This change might impact you if you were consuming the config classes and, if so, we recommend having a look at the [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.19).

If you are an extension developer, we encourage you to move to this new infrastructure as, at some point in the future, we will sunset the legacy ones (we will announce a sunsetting plan soon, it will offer you ample time to upgrade).

### UBI 9 as default

We updated the default images across Quarkus to UBI 9 (both to build your applications and run your applications).

The [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.19#ubi-9) will give you ample information on the impact and what you might need to tweak.

### Micrometer to OpenTelemetry bridge

You can now push your Micrometer metrics to OpenTelemetry using a bridge.

Learn more about it in the [dedicated guide](https://quarkus.io/guides/telemetry-micrometer-to-opentelemetry).

### JEP 483's AOT cache

We have support for AppCDS in Quarkus for quite a while and Quarkus 3.19 generalizes it to also support the AOT cache introduced in JEP 483.

It can further improve the startup time, if you generate an AOT cache.

### WebSockets Next

Two new features for WebSockets Next:

- The ability to send Authorization headers from web browsers using JavaScript clients - see the [documentation](https://quarkus.io/guides/websockets-next-reference#bearer-token-authentication).
- The support for permission checkers, which were already available for your REST endpoints - see the [documentation](https://quarkus.io/guides/websockets-next-reference#secure-endpoints-with-permission-checkers).

### Security

Our OIDC extension now supports [proof of possession](https://datatracker.ietf.org/doc/html/rfc9449) for OAuth 2 tokens.

You can find more information about it in our [documentation](https://quarkus.io/guides/security-oidc-bearer-token-authentication#demonstrating-proof-of-possession-dpop).

### Platform component upgrades

#### Quarkus CXF

Quarkus CXF 3.19 was released and is now available in [Quarkus Platform 3.19](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check the [Quarkus CXF 3.19.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.19.0.html) release notes for more information about what is new in this release.

#### Camel Quarkus

Camel Quarkus has been upgraded to 3.19.0. You can consult the [release notes](https://camel.apache.org/releases/q-3.19.0/) for more information.

## Full changelog

You can get the full changelog of [3.19.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.19.0.CR1), [3.19.0](https://github.com/quarkusio/quarkus/releases/tag/3.19.0), and [3.19.1](https://github.com/quarkusio/quarkus/releases/tag/3.19.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1058 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.19 release, thanks to Akulov S V, Ales Justin, Alex Martel, Alex Rovner, Alexey Loubyansky, Andrea Boriero, André Pantaleão, Andy Damevin, Bruno Baptista, Chris Laprun, Clement Escoffier, cmoulliard, Damien Clément d'Huart, David Me, Davide D'Alto, dc1248, Eric Deandrea, Erik Mattheis, Felix König, Foivos Zakkak, franz1981, George Gastaldi, Georgios Andrianakis, Gerhard Flothow, Guillaume Smet, Hannah Arndt, Harald Albers, HerrDerb, Holly Cummins, Ivan Béthus, Ivan Petkov, Jakub Gardo, Jakub Jedlicka, Jan Martiska, Jorge Pinto, Julien Ponge, Katia Aresti, Ladislav Thon, Lars Andringa, Loïc Mathieu, Luis Rubiera, Marc Nuri, Marco Bungart, Marco Sappé Griot, Marek Skacelik, mariofusco, marko-bekhta, Martin Kouba, Martin Panzer, Matej Novotny, Matheus Cruz, Matthias Schorsch, Max Rydahl Andersen, Maximilian Zellhofer, melloware, Michael Edgar, Michal Maléř, Michal Vavřík, Ozan Gunalp, pedro_Simoes, Peter Palaga, Phillip Krüger, Roberto Cortez, Rolfe Dlugy-Hegwer, Romain QUINIO, Rostislav Svoboda, Ryan Dens, Sanne Grinovero, Sergey Beryozkin, Severin Gehwolf, Stephan Strate, Stuart Douglas, Stéphane Épardaud, Tobias Haindl, Vincent Potucek, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
