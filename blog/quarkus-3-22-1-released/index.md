# Quarkus 3.22 - Compose Dev Services, improved test class loading infrastructure...

Today, we released Quarkus 3.22.

It comes with several important features, together with some infrastructure improvements:

- [#46848](https://github.com/quarkusio/quarkus/pull/46848) - Compose Dev Services
- [#34681](https://github.com/quarkusio/quarkus/pull/34681) - Improved testing class loader infrastructure
- [#44473](https://github.com/quarkusio/quarkus/pull/44473) - Allow Hibernate ORM and Hibernate Reactive to be used in the same application
- [#46398](https://github.com/quarkusio/quarkus/pull/46398) - Apply validation modes to the Hibernate Reactive session factory config / Add tests for Reactive + Validator
- [#46728](https://github.com/quarkusio/quarkus/pull/46728) - Dedicated Dev UI interface to execute HQL (Hibernate ORM) queries
- [#47012](https://github.com/quarkusio/quarkus/pull/47012) - Add Jakarta Data documentation, dependency management and tests
- [#46864](https://github.com/quarkusio/quarkus/pull/46864) - Allow setting Clear-Site-Data on OIDC logout
- [#47254](https://github.com/quarkusio/quarkus/pull/47254) - Add OIDC expanded configuration reference
- [#47127](https://github.com/quarkusio/quarkus/pull/47127) - Support customization of gRPC server building
- [#47435](https://github.com/quarkusio/quarkus/pull/47435) - Make Stork optional for REST Client

## Update

To update to Quarkus 3.22, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.22.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.22 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.22).

## What's new?

### Compose Dev Services

Dev Services is one of the key developer joy feature of Quarkus. It has been around for quite some time and is very handy to get simple containers started in dev and test modes.

Quarkus 3.22 introduces the ability to compose and orchestrate complex container setups as Dev Services, by leveraging the Compose approach offered by Docker and Podman.

You can learn all about it in the [dedicated guide](https://quarkus.io/guides/compose-dev-services).

### Improved testing class loader infrastructure

This one has been in the works for quite some time and we are pleased to announce a new testing class loader infrastructure. It fixes a lot of issues but might also introduce some issues - even if we tried hard to squash all of them.

You can learn (a lot!) more about it in [Holly's blog post presenting this work](https://quarkus.io/blog/test-classloading-rewrite/).

If you encounter any weird class loading issues in your tests, please report back!

### Hibernate ORM and Hibernate Reactive

We used to not be able to mix Hibernate ORM and Hibernate Reactive in the same application. This limitation has now been lifted and you can leverage the strengths of both extensions in the same app.

Also, Hibernate Validator is now properly integrated with Hibernate Reactive.

Finally, Quarkus 3.22 includes a new Dev UI feature: you can execute HQL queries (as in Hibernate Query Language) directly in the Dev UI, allowing you to very easily test your HQL queries and iterate on them.

### Jakarta Data

Jakarta Data has been supported by Quarkus since its inception but it wasn't properly documented. It has now a [dedicated section](https://quarkus.io/guides/hibernate-orm#jakarta-data) in the Hibernate ORM guide.

The Jakarta Data dependencies have been added to our BOM.

### OIDC

Starting with 3.22, on OIDC logout, you have the ability to set `Clear-Site-Data` header by using the `quarkus.oidc.logout.clear-site-data` configuration property.

We also added a [brand new guide](https://quarkus.io/guides/security-oidc-expanded-configuration) with extensive coverage of the OIDC configuration.

### gRPC

It is now possible to customize the gRPC server building by implementing the `ServerBuilderCustomizer` interface.

You can find out more about in the [new section](https://quarkus.io/guides/grpc-service-implementation#custom-server-building) added to the documentation.

### Stork

To avoid having to initialize Stork even if not needed, Stork is no longer a hard dependency of the REST Client.

If you want to leverage Stork with the REST Client, you need to add the Stork extension to your build file.

### Platform component upgrades

#### Quarkus CXF

Quarkus CXF 3.22 was released and is now available in [Quarkus Platform 3.22](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check the [Quarkus CXF 3.22.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.22.0.html) release notes for more information about what is new in this release.

#### Camel Quarkus

Camel Quarkus has been upgraded to 3.22.0.

#### Amazon Services

The Amazon Services extensions have been upgraded to version 3.5.0.

## Full changelog

You can get the full changelog of [3.22.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.22.0.CR1), [3.22.0](https://github.com/quarkusio/quarkus/releases/tag/3.22.0), and [3.22.1](https://github.com/quarkusio/quarkus/releases/tag/3.22.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1075 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.22 release, thanks to Aixi-dev, Ales Justin, Alexey Loubyansky, Andreas Eberle, Andy Damevin, Antonio Musarra, Auri Munoz, Barry LaFond, Bruno Baptista, Clemens Classen, Clement Escoffier, Davide D'Alto, Digvijay Singh, elmodeer, Eric Deandrea, Esben Nedergaard, Fabian, Fabian Senn, feibl, Foivos Zakkak, franz1981, George Gastaldi, Georgios Andrianakis, Giancarlo Calderón Cárdenas, Guillaume Smet, Harry Chan, Holly Cummins, holomekc, Inaki Villar, ineednousername, Jan Martiska, Jeremie Bresson, Johnathan Gilday, Juan Antonio Breña Moral, Julien Ponge, Junes, Karm Michal Babacek, Katia Aresti, Ladislav Thon, Loïc Mathieu, Luca Basso Ricci, Luca Molteni, Marco Belladelli, mariofusco, marko-bekhta, Martin Bartoš, Martin Kouba, Martin Panzer, Matej Novotny, Matheus Cruz, Matheus Oliveira da Silva, Maximilian Zellhofer, Michael Edgar, Michael Musgrove, Michal Vavřík, Michiel Dockx, Mikhail Polivakha, Ozan Gunalp, Ozzy, Paulo Casaes, Peter Palaga, Phillip Krüger, Roberto Balarezo, Roberto Cortez, Rod Cheater, Rostislav Svoboda, Rüdiger zu Dohna, Sergey Beryozkin, shjones, Stuart Douglas, Stéphane Épardaud, Tamas Cservenak, Vardhman, w0pp, Yassine Haouzane, Yoann Rodière, and Yoshikazu Nojima.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
