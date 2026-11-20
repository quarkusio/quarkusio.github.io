# Quarkus 3.17 - Observability improvements, programmatic permission checkers, MicroProfile REST Client 4.0

Quarkus 3.17 is a smaller release than 3.16 but we polished and improved a lot of areas.

Notable changes are:

- [#41956](https://github.com/quarkusio/quarkus/pull/41956) - Integrate OpenTelemetry to the WebSockets Next extension
- [#43983](https://github.com/quarkusio/quarkus/pull/43983) - OpenTelemetry - Add SimpleSpanProcessor support
- [#43959](https://github.com/quarkusio/quarkus/pull/43959) - Update MicroProfile REST Client to 4.0
- [#43846](https://github.com/quarkusio/quarkus/pull/43846) - Add annotation to allow using custom CDI bean methods as permission checkers
- [#41929](https://github.com/quarkusio/quarkus/pull/41929) - Use ArC features in datasource extensions for eager startup and active/inactive
- [#43448](https://github.com/quarkusio/quarkus/pull/43448) - Adapt locales support for GraalVM >= 24.2

There won't be any new minor releases in December but we will release new micros of 3.17.

Next minor 3.18 will be released at the end of January.

## Update

To update to Quarkus 3.17, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.17.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.17 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.17).

## What's new?

### Observability

In relation to observability, we would like to highlight new features:

- Support for [OpenTelemetry](https://opentelemetry.io) in the [WebSockets Next](https://quarkus.io/guides/websockets-next-tutorial) extension.
- OpenTelemetry Simple Processors for traces and logs on Quarkus. This is important for lambda functions and other short-lived processes because it allows sending telemetry faster, not batching it, therefore minoring the risk of losing data.
- MicroProfile Telemetry 2.0 support by adding automatic JVM and HTTP request metrics to the OpenTelemetry extension.

### MicroProfile REST Client 4.0

The Quarkus REST Clients have been updated to MicroProfile REST Client 4.0.

### Security

We added more flexibility to how you define your permissions by allowing to point to CDI bean methods for permission checking.

This solution is the Quarkus equivalent to the Expression Language support you can find in some other frameworks. Expression Language expressions are brittle and hard to debug and robust permission checking is critical, thus why we prefer you write your permission checking in plain Java.

You can find more detailed information about this new feature in the [documentation](https://quarkus.io/guides/security-authorize-web-endpoints-reference#permission-checker).

### Inactive datasources

A lot of work went into ArC, our CDI implementation, and our datasource support to make sure we would avoid a lot of corner cases when a datasource is inactive.

For instance:

- We make sure inactive datasources don't contribute to health checks.
- We fail fast if some code tries to inject an inactive datasource.

You can find more information about this work in the [Quarkus 3.17 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.17#datasources).

### Locale support

Our Mandrel team is working tirelessly to make sure Quarkus stays compatible with the future versions of GraalVM/Mandrel.

We adapted our locale support to be compatible with GraalVM >= 24.2 and you can find all the gory details in the corresponding [pull request](https://github.com/quarkusio/quarkus/pull/43448).

### Platform component upgrades

#### Quarkus CXF

Quarkus CXF 3.17 was released and is now available in [Quarkus Platform 3.17](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check the [Quarkus CXF 3.17.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.17.0.html) release notes for more information about what is new in this release.

## Full changelog

You can get the full changelog of [3.17.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.17.0.CR1) and [3.17.0](https://github.com/quarkusio/quarkus/releases/tag/3.17.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [1035 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.17 release, thanks to Ales Justin, Alexei Bratuhin, Alexey Loubyansky, Amit Prasad, Andy Damevin, Antoine de Troostembergh, Benjamin Raimondi, Bruno Baptista, Bruno Marvin, chengehe, Christian Ivanov, Clement Escoffier, Cristian Burlacu, Daniel Bobbert, Daniel Cunha, Daniel Ezihe, Dannier Leonides Galicia Chinchilla, David M. Lloyd, Douglas Monteiro, Emmanuel Ferdman, Foivos Zakkak, Fouad Almalki, Francesco Nigro, Frantisek Havel, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Gunnar Morling, Holly Cummins, Inaki Villar, Ioannis Canellos, Jakub Gardo, Jakub Jedlicka, Jan Martiska, jcarvaja, Jono, Jorge Solórzano, Jose, Julien Ponge, Jérémie Bresson, Katia Aresti, koplas, Ladislav Thon, Loïc Hermann, Loïc Mathieu, luneo7, Marco Belladelli, Marco Sappé Griot, Marek Skacelik, mariofusco, Martin Bartoš, Martin Kouba, Matej Novotny, Matheus Cruz, Max Rydahl Andersen, Melloware, Michael Edgar, Michal Karm Babacek, Michal Maléř, Michal Vavřík, Murray Hopkins, Ozan Gunalp, Peter Palaga, Phillip Krüger, Ralf Ueberfuhr, Raphael Tholl, rghara, Robert Stupp, Robert Toyonaga, Roberto Cortez, Robin De Mol, RobinDM, Rod Cheater, Rolfe Dlugy-Hegwer, Rostislav Svoboda, Ruwen Reddig, Sergey Beryozkin, sNiXx, Stef, Stéphane Épardaud, Thomas Canava, Trấn Nguyễn, Vincent Sourin, vsevel, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
