# Quarkus 3.24 - Dev Assistant, Hibernate ORM 7, Hibernate Validator 9...

Today, we released Quarkus 3.24. It comes with major version upgrades to major components and a brand new feature: the Dev Assistant.

- [#47756](https://github.com/quarkusio/quarkus/pull/47756) - Introduce the Assistant
- [#41310](https://github.com/quarkusio/quarkus/pull/41310) - Upgrade to Hibernate ORM 7.0, Hibernate Reactive 3.0, and Hibernate Search 8.0
- [#42292](https://github.com/quarkusio/quarkus/pull/42292) - Upgrade to Hibernate Validator 9.0
- [#47108](https://github.com/quarkusio/quarkus/pull/47108) - Upgrade to Kafka Client 4.0
- [#47830](https://github.com/quarkusio/quarkus/pull/47830) - Add OIDC Health Check

## Update

To update to Quarkus 3.24, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.24.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.24 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.24).

## What's new?

### Dev Assistant

We all love Quarkus' Dev UI and Quarkus 3.24 paves the way for major improvements to the Dev UI through a brand new feature: the Dev Assistant.

The Assistant is a new extension point to provide features assisting you in your daily coding:

- Generate clients from your OpenAPI specification
- Generate additional test data
- ... sky is the limit!

The Assistant can offer features that are backed by AI, but isn't limited to it.

We have been cooking an extension leveraging the Assistant feature for a while: [Quarkus Chappie](https://github.com/quarkiverse/quarkus-chappie). You can already add it to your projects.

But don't see it as the end of things: you can develop your own Assistant features in your extensions.

[Q88NQp_Uul4](Q88NQp_Uul4)

### Hibernate ORM

Hibernate ORM was updated to [7.0](https://hibernate.org/orm/releases/7.0/).

This is a major version and it comes with new features and significant changes that are presented in more details in a [dedicated blog post](https://quarkus.io/blog/hibernate7-on-quarkus/).

Please also have a look to the dedicated section of our [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.24#jakarta-persistence-hibernate-orm).

Hibernate ORM 7.0 is an implementation of [Jakarta Persistence 3.2](https://jakarta.ee/specifications/persistence/3.2/).

### Hibernate Reactive

Hibernate Validator was updated to [3.0](https://hibernate.org/reactive/releases/3.0/).

### Hibernate Search

Hibernate Search was updated to [8.0](https://hibernate.org/search/releases/8.0/).

### Hibernate Validator

Hibernate Validator was updated to [9.0](https://hibernate.org/validator/releases/9.0/).

Hibernate Validator 9.0 is an implementation of [Jakarta Validation 3.1](https://jakarta.ee/specifications/bean-validation/3.1/).

### Kafka Client

The Kafka Client has been updated to 4.0.

### OIDC health check

Quarkus 3.24 offers a health check for OIDC that allows to check your Quarkus application is able to connect to your OIDC server.

### Platform component upgrades

#### Camel Quarkus

Camel Quarkus has been upgraded to 3.24.0.

## Full changelog

You can get the full changelog of [3.24.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.24.0.CR1), [3.24.0](https://github.com/quarkusio/quarkus/releases/tag/3.24.0), and [3.24.1](https://github.com/quarkusio/quarkus/releases/tag/3.24.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1092 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.24 release, thanks to Alexandre Dutra, Alexey Loubyansky, appiepollo14, Ashish Ranjan, Bill Burke, Bruno Baptista, Clement Escoffier, David M. Lloyd, Davide D'Alto, Erik Mattheis, Foivos Zakkak, Fouad Almalki, gbourant, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Holly Cummins, Inaki Villar, João Lopes, Julien Ponge, Katia Aresti, Kevin Wooten, Ladislav Thon, Lars, Lukas Schmitt, Marc Nuri, Marco Belladelli, marko-bekhta, Martin Bartoš, Martin Kouba, Martin Panzer, Matej Novotny, melloware, Michael Edgar, Michal Maléř, Michal Vavřík, Nicholas Hassan, nort3x, Ozan Gunalp, patriot1burke, Peter Palaga, Phillip Krüger, Robert Stupp, Roberto Cortez, Rostislav Svoboda, Sebastian Vogl, Sergey Beryozkin, Stefan Schmöller, Steve Hawkins, Stuart Douglas, Stéphane Épardaud, Teymur Babayev, Tim van der Lippe, Vincent Potucek, Volodymyr, xstefank, Yahya Berbeche, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
