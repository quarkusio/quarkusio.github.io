# Quarkus 3.30 - JsonView on REST Client, Hibernate Validator 9.1, CLI decrypt command, and more

Today, we released Quarkus 3.30.

Quarkus 3.30 introduces the following notable changes:

- [#51043](https://github.com/quarkusio/quarkus/pull/51043) - Introduce support for `@JsonView` on the REST Client
- [#50196](https://github.com/quarkusio/quarkus/pull/50196) - Update to Hibernate Validator 9.1
- [#50332](https://github.com/quarkusio/quarkus/pull/50332) - Add a CLI command to decrypt secrets
- [#50723](https://github.com/quarkusio/quarkus/pull/50723) - OIDC token propagation - allow users to select REST Client methods for which token should be propagated
- [#50806](https://github.com/quarkusio/quarkus/pull/50806) - Support method-level `@OidcClientFilter` bindings
- [#50952](https://github.com/quarkusio/quarkus/pull/50952) - Use `@QuarkusIntegrationTest` tests as a test run for AOT file generation

As usual, this release also includes many bug fixes and improvements.

## Update

To update to Quarkus 3.30, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.30.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.30 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.30).

## What's new?

### REST Client

`@JsonView` support has been added to the REST Client. You can now use this annotation on your REST Client interfaces to control the serialization and deserialization of JSON data based on different views.

This feature used to be only supported on the server side.

### Hibernate Validator

Hibernate Validator has been updated to version 9.1, which comes with some significant performance improvements.

### CLI

A new `config decrypt` command has been added to the Quarkus CLI to decrypt secrets that were encrypted using the `quarkus config encrypt` command.

### OIDC

OIDC token propagation has been enhanced to allow users to select specific REST Client methods for which the token should be propagated.

Additionally, method-level `@OidcClientFilter` bindings are now supported, allowing for more granular control over OIDC client behavior.

### AOT cache

`@QuarkusIntegrationTest` tests are now used as a test run for AOT file generation.

## Full changelog

You can get the full changelog of [3.30.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.30.0.CR1), [3.30.0](https://github.com/quarkusio/quarkus/releases/tag/3.30.0), and [3.30.1](https://github.com/quarkusio/quarkus/releases/tag/3.30.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1136 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.30 release, thanks to Ales Justin, Alexey Loubyansky, Andreas Repp, Andy Damevin, Antonio Musarra, Aurea Munoz, azerr, Bruno Baptista, Christian Pieczewski, Clement Escoffier, David M. Lloyd, Eric Deandrea, eschcam, Foivos Zakkak, Fouad Almalki, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Greg Stewart, Guillaume Smet, Holly Cummins, Jakub Pietrzak, James Netherton, Jan Martiska, jcarranzan, jcjveraa, Johannes Schleger, Jonas Rutishauser, Juan Antonio Breña Moral, Julien Ponge, Junior DUSSOUILLEZ, Karm Michal Babacek, Kevin Ferrare, Ladislav Thon, Loïc Hermann, Luca Molteni, Lucas Reeh, Maciej Swiderski, Manuel Müller, Marco Belladelli, mariofusco, marko-bekhta, Martin Kouba, Matej Novotny, Matej Vašek, Matheus Cruz, Melloware, Michael Edgar, Michal Vavřík, Nicolo Pietro Belcastro, Ozan Gunalp, Peter Palaga, Phillip Krüger, Pierre Beitz, Robert Stupp, Roberto Cortez, Rolfe Dlugy-Hegwer, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, staillebois, Stéphane Épardaud, Teymur Babayev, Thomas Segismont, Tomas Hofman, Willem Jan Glerum, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
