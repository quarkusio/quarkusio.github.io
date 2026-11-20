# Quarkus 3.18 - Micrometer for WebSockets Next, Security WebAuthn based on WebAuthn4J, Kubernetes Client 7...

Today, we released Quarkus 3.18, with two months worth of new features and enhancements.

It comes with a lot of enhancements and the following new features:

- [#44379](https://github.com/quarkusio/quarkus/pull/44379) - Integrate Micrometer with WebSockets Next
- [#44105](https://github.com/quarkusio/quarkus/pull/44105) - Reimplement security-webauthn on top of webauthn4j
- [#45131](https://github.com/quarkusio/quarkus/pull/45131) - OIDC and OIDC Client: Support JWT bearer client authentication using client assertion loaded from filesystem
- [#45121](https://github.com/quarkusio/quarkus/pull/45121) - Support for OIDC mTLS binding
- [#44993](https://github.com/quarkusio/quarkus/pull/44993) - Support OidcProviderClient injection and token revocation
- [#44546](https://github.com/quarkusio/quarkus/pull/44546) - Add OIDC Redis Token State Manager extension
- [#45294](https://github.com/quarkusio/quarkus/pull/45294) - Allow to create static OIDC tenants programmatically
- [#35324](https://github.com/quarkusio/quarkus/pull/35324) - OIDC Dev Services and UI changes
- [#45117](https://github.com/quarkusio/quarkus/pull/45117) - TLS - Enable policy configuration for expired or not yet valid certificates
- [#44549](https://github.com/quarkusio/quarkus/pull/44549) - Add support for encrypted PKCS#8
- [#43885](https://github.com/quarkusio/quarkus/pull/43885) - Exclude uri from OpenTelemetry Tracing
- [#45259](https://github.com/quarkusio/quarkus/pull/45259) - Bump kubernetes-client-bom from 6.13.4 to 7.0.1
- [#32447](https://github.com/quarkusio/quarkus/pull/32447) - Introduce `Report an Issue`  menu in DevUI
- [#43618](https://github.com/quarkusio/quarkus/pull/43618) - Add a Dev UI screen for Agroal datasources
- [#44305](https://github.com/quarkusio/quarkus/pull/44305) - Ability to configure extension dev mode JVM options
- [#44424](https://github.com/quarkusio/quarkus/pull/44424) - Support for dev-mode-only conditional dependencies

We are already hard at work preparing 3.19 and the upcoming 3.20 LTS.

## Update

To update to Quarkus 3.18, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.18.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.18 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.18).

## What's new?

### WebSockets.Next

Our next-generation WebSockets extension continues to improve with each version.

In 3.18, it comes with Micrometer integration.

### Security

You are used to it, each version comes with a lot of enhancements to our security layer, offering even more flexibility.

This time, it comes with the complete rewrite of our Security Webauthn extension to leverage the WebAuthn4J library (if you are using this extension, please have a look at the [dedicated section in the migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.18#webauthn) as a lot of things have changed), a lot of new features for OIDC, and improvements to our TLS registry:

- [#44105](https://github.com/quarkusio/quarkus/pull/44105) - Reimplement security-webauthn on top of webauthn4j
- [#45131](https://github.com/quarkusio/quarkus/pull/45131) - OIDC and OIDC Client: Support JWT bearer client authentication using client assertion loaded from filesystem
- [#45121](https://github.com/quarkusio/quarkus/pull/45121) - Support for OIDC mTLS binding
- [#44993](https://github.com/quarkusio/quarkus/pull/44993) - Support OidcProviderClient injection and token revocation
- [#44546](https://github.com/quarkusio/quarkus/pull/44546) - Add OIDC Redis Token State Manager extension
- [#45294](https://github.com/quarkusio/quarkus/pull/45294) - Allow to create static OIDC tenants programmatically
- [#35324](https://github.com/quarkusio/quarkus/pull/35324) - OIDC Dev Services and UI changes
- [#45117](https://github.com/quarkusio/quarkus/pull/45117) - TLS - Enable policy configuration for expired or not yet valid certificates
- [#44549](https://github.com/quarkusio/quarkus/pull/44549) - Add support for encrypted PKCS#8

### OpenTelemetry Tracing

When using OpenTelemetry Tracing, it happens quite often that you don't want to collect any trace for a given URI.

3.18 comes with the `quarkus.otel.traces.suppress-application-uris` that allows to define URIs that are going to be ignored.

You can find more information about this new feature in the [OpenTelemetry Tracing guide](https://quarkus.io/guides/opentelemetry-tracing#disabling-traces-for-app-endpoints).

### Kubernetes Client

The Kubernetes Client was upgraded to a major new version: Kubernetes Client 7.

You can find more information about this upgrade in our [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.18#kubernetes-client-fabric8).

### Dev UI

We polished our Dev UI with two new features:

- A screen to browse the tables exposes by your datasources
- A quick link to report an issue to the Quarkus project on GitHub

### Preparing our next LTS

We are also preparing our next LTS with various initiatives:

- A lot of extensions were migrated to the new `@ConfigMapping`-based configuration infrastructure (and this effort will continue in the next versions).
- We are making enhancements related to startup time and initial memory (RSS) usage.
- We are removing code that was deprecated for a long time.

Expect us to focus on polishing features and fixing issues for the upcoming 3.19 and 3.20.

### Platform component upgrades

#### Quarkus CXF

Quarkus CXF 3.18 was released and is now available in [Quarkus Platform 3.18](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check the [Quarkus CXF 3.18.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.18.0.html) release notes for more information about what is new in this release.

#### Camel Quarkus

Camel Quarkus has been upgraded to 3.18.0.

## Full changelog

You can get the full changelog of [3.18.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.18.0.CR1), [3.18.0](https://github.com/quarkusio/quarkus/releases/tag/3.18.0), and [3.18.1](https://github.com/quarkusio/quarkus/releases/tag/3.18.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1048 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.18 release, thanks to Akulov S V, Ales Justin, Alex Martel, Alexander Pankin, Alexey Loubyansky, André Pantaleão, Andy Damevin, Ankush Saini, Antonio Musarra, Auri Munoz, Bassel Rachid, Blaz Mrak, Bruno Baptista, Bruno Marvin, Chris Laprun, Christian Ivanov, Christian Pieczewski, Clement Escoffier, Cristian Burlacu, Damien Clément d'Huart, Daniel Bobbert, Daniel Strobusch, Danilo Piazzalunga, David M. Lloyd, Davide D'Alto, Eduard Wagner, Emmanuel Ferdman, Eric Deandrea, Erik Mattheis, Fary Hurtado, Foivos Zakkak, Francesco Nigro, George Gastaldi, Georgios Andrianakis, Gianmarco Frangipane, Guillaume Smet, Gurubase.io, Harald Albers, HerrDerb, Holly Cummins, Inaki Villar, Ioannis Canellos, ivan.baricic, Jakub Jedlicka, Jan Martiska, Jeremie Bresson, Jochen Schalanda, Johnathan Gilday, Jorge Pinto, Jose, Julien Ponge, Katia Aresti, Ladislav Thon, Lars Andringa, Loïc Mathieu, luneo7, Maciej Lisowski, Marc Nuri, Marco Belladelli, Marco Bungart, Marco Collovati, Marek Skacelik, mariofusco, Martin Bartoš, Martin Kouba, Martin Panzer, Matej Novotny, Matheus Cruz, Max Rydahl Andersen, Michael Edgar, Michal Maléř, Michal Vavřík, Neon, Nuno Neto, ogomezdi, Ozan Gunalp, Ozzy Osborne, Peter Skopek, Phillip Krüger, rghara, Roberto Balarezo, Roberto Cortez, Rolfe Dlugy-Hegwer, Romain QUINIO, Rostislav Svoboda, row, Scott M Stark, Sergey Beryozkin, sergioruydev, Sola-ris, Stephan Strate, Stuart Douglas, Stéphane Épardaud, Thibault Meyer, Thomas Canava, tom, Trấn Nguyễn, vkn, xstefank, Yoann Rodière, Yoshikazu Nojima, zanmagerl, and Zheng Feng.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
