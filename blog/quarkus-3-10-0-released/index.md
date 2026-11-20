# Quarkus 3.10 - Hibernate Search standalone POJO mapper, Flyway 10, security enhancements

After some big changes in Quarkus 3.9, we have the pleasure to announce Quarkus 3.10.

Quarkus 3.10 is for developers who want the latest features, if you are looking for an extended support cycle, you are encouraged to stay on 3.8 LTS.

Here are the main changes for 3.10:

- [#39415](https://github.com/quarkusio/quarkus/pull/39415) - Extension for the Hibernate Search Standalone POJO Mapper with Elasticsearch
- [#39509](https://github.com/quarkusio/quarkus/pull/39509) - Update Flyway to 10.10.0
- [#39295](https://github.com/quarkusio/quarkus/pull/39295) - Move package config to an interface
- [#36504](https://github.com/quarkusio/quarkus/pull/36504) - Allow authentication mechanism selection for a REST endpoint with annotation
- [#37816](https://github.com/quarkusio/quarkus/pull/37816) - Optional support for the OIDC session cookie dir encryption
- [#39793](https://github.com/quarkusio/quarkus/pull/39793) - Support for verifying OIDC JWT claims with custom Jose4j `Validator`
- [#39812](https://github.com/quarkusio/quarkus/pull/39812) - Support resolving of static OIDC tenants based on token issuers
- [#40056](https://github.com/quarkusio/quarkus/pull/40056) - Add OIDC TokenCertificateValidator

## Update

To update to Quarkus 3.10, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.10.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.10 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.10).

If you are not already using 3.x, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details. Once you upgraded to 3.0, also have a look at the [3.1](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1), [3.2](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2), [3.3](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.3), [3.4](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.4), [3.5](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.5), and [3.6](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.6), [3.7](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.7), [3.8](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.8), [3.9](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9), and [3.10](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.10) migration guides.

## What's new?

### Hibernate Search standalone POJO Mapper

Hibernate Search has been part of the Quarkus ecosystem for a long time but until now we only supported the Hibernate ORM integration.

Quarkus 3.10 adds a brand new extension to add support for the Hibernate Search standalone POJO Mapper, which allows to index directly arbitrary POJOs.

If you want to index structured data coming from files, MongoDB entities, ... this new extension should make you happy.

Interested in this extension? We have a [dedicated guide](https://quarkus.io/guides/hibernate-search-standalone-elasticsearch) for you, which covers the Quarkus integration and the basics.

For more advanced information about the Hibernate Search standalone POJO mapper, please consult the [Hibernate Search reference documentation](https://docs.jboss.org/hibernate/stable/search/reference/en-US/html_single/#mapper-pojo-standalone) and the [Getting started](https://docs.jboss.org/hibernate/stable/search/getting-started/standalone/en-US/html_single/index.html#mapper-pojo-standalone-getting-started) guide.

### Flyway 10

We are a bit late to the party as we encountered some incompatibility with native executables and the new Flyway 10 but Quarkus 3.10 comes with an upgrade to Flyway 10, more precisely 10.10.

If you are not using `quarkus update`, have a look at the [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.10) as they split some more database supports out of Flyway core.

### Changes to `quarkus.package.*` config

The `quarkus.package.*` config was refactored.

The configuration changes are handled by `quarkus update` but if you are upgrading manually, have a closer look at the dedicated section of the [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.10#packaging-configuration-gear-white_check_mark).

Note that when using the previous configuration properties are deprecated but should still work so you don't have to upgrade right away. They will be dropped in a (relatively distant) future version.

### Security enhancements

This new version comes with several improvements related to security:

- [#36504](https://github.com/quarkusio/quarkus/pull/36504) - Allow authentication mechanism selection for a REST endpoint with annotation
- [#37816](https://github.com/quarkusio/quarkus/pull/37816) - Optional support for the OIDC session cookie dir encryption
- [#39793](https://github.com/quarkusio/quarkus/pull/39793) - Support for verifying OIDC JWT claims with custom Jose4j `Validator`
- [#39812](https://github.com/quarkusio/quarkus/pull/39812) - Support resolving of static OIDC tenants based on token issuers
- [#40056](https://github.com/quarkusio/quarkus/pull/40056) - Add OIDC TokenCertificateValidator

## Quarkus CXF

Quarkus CXF 3.10.0 was released and is now available in [Quarkus Platform 3.10](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check [Quarkus CXF 3.10.0 release notes](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.10.0.html) for more information about what is new in this release.

## Full changelog

You can get the full changelog of [3.10.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.10.0.CR1) and [3.10.0](https://github.com/quarkusio/quarkus/releases/tag/3.10.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [940 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.10 release, thanks to Ales Justin, Alexander Schwartz, Alexey Loubyansky, Andy Damevin, Antonio Musarra, asjervanasten, avivmu, Bas Passon, Bruno Baptista, Clement Escoffier, Damiano Renfer, David M. Lloyd, ennishol, Eric Deandrea, Erin Schnabel, fdlane, Foivos Zakkak, Fouad Almalki, Francesco Nigro, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Gwenneg Lepage, Holly Cummins, Ioannis Canellos, Jakub Jedlicka, James Netherton, Jan Martiska, Jason T. Greene, Jean Bisutti, Jerome Prinet, Jonas Jensen, Juan Jose Garcia, Julien Ponge, Jérémie Bresson, Katia Aresti, Klaus Nguetsa, Ladislav Thon, Laurent Broudoux, luneo7, Marc Nuri, Marco Sappé Griot, Marco Schaub, Marek Skacelik, marko-bekhta, Martin Kouba, Matej Novotny, Max Rydahl Andersen, Michal Maléř, Michal Vavřík, Michiel Thomassen, Monhemius,  B. (Bart), Ozan Gunalp, Peter Palaga, Phillip Krüger, Pierre Adam, Robbie Gemmell, Roberto Cortez, Sanne Grinovero, Sebastian Davids, Sergey Beryozkin, Stéphane Épardaud, Thomas Canava, Thomas Segismont, Vinicius A. Santos, xstefank, Yoann Rodière, Yoshikazu Nojima, Yukihiro Okada, and Žan Horvat.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
