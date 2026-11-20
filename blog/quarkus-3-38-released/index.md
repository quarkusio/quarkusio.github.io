# Quarkus 3.38 - Weight-based eviction for Hibernate second level Caffeine cache, and more

We're pleased to announce the release of Quarkus 3.38.

As most of the development effort for new features and major upgrades is now focused on Quarkus 4, this release is lighter on features than usual. It still comes with the following notable features:

- [#55284](https://github.com/quarkusio/quarkus/pull/55284) - Support weight-based eviction for Hibernate 2LC Caffeine cache regions
- Quarkus HTTP Problem added to the Platform

## Update

To update to Quarkus 3.38, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.38.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.38 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.38).

## What's new?

### Weight-based eviction for Hibernate second level Caffeine cache regions

Quarkus 3.38 introduces support for weight-based (memory) eviction for Hibernate second level cache Caffeine cache regions.

Previously, only count-based eviction was available, meaning you could only configure the maximum number of entries per cache region. With weight-based eviction, you can now configure eviction based on the estimated memory footprint of cached entities, providing more precise control over cache memory usage.

### Quarkus Platform

#### Quarkus HTTP Problem

[Quarkus HTTP Problem](https://github.com/quarkiverse/quarkus-http-problem) has been added to the Quarkus Platform.

This extension implements the [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) (Problem Details for HTTP APIs) standard, automatically mapping exceptions to standardized `application/problem+json` HTTP responses.

It provides built-in exception mappers for common exceptions, prevents leaking stack traces and implementation details, and integrates with OpenAPI for auto-generated error documentation.

#### Component upgrades

Various Platform components were upgraded including:

- Camel Quarkus to 3.38.0
- Quarkus Amazon Services to 3.21.1
- Quarkus Blaze-Persistence to 1.6.19
- Quarkus Debezium to 3.6.0.Final
- Quarkus Flow to 0.14.0
- Quarkus Google Cloud Services to 2.23.1
- Quarkus LangChain4j to 1.12.1
- Quarkus MCP Server to 1.13.1
- Quarkus Vault to 4.9.0

## Full changelog

You can get the full changelog of [3.38.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.38.0.CR1) and [3.38.0](https://github.com/quarkusio/quarkus/releases/tag/3.38.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [1213](https://github.com/quarkusio/quarkus/graphs/contributors) contributors. Many many thanks to each and everyone of them.

In particular for the 3.38 release, thanks to Ahmed El amraouiyine, Alexander Schwartz, Alexey Loubyansky, amirdeljouyi, Andreas Maechler, Andy Damevin, Arthur Navarro, Ashish Thakur, Atharv Hatwar, Aurea Munoz, Ben Rose, Bruno Baptista, Clement Escoffier, David Kjerrumgaard, David M. Lloyd, Davide D'Alto, Domenico Briganti, Foivos Zakkak, Fouad Almalki, Gaelle Fournier, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Holly Cummins, Jan Martiska, Julien Ponge, Kamil Krzywanski, Katia Aresti, Kristian Rickert, Ladislav Thon, Luca Molteni, Lukas Lowinger, luneo7, Marco Belladelli, Marek Skacelik, mariofusco, marko-bekhta, Martin Kouba, Matheus Cruz, MdTanwer, Michal Vavřík, Ozan Gunalp, Phillip Krüger, Robert Stupp, Roberto Cortez, Rolfe Dlugy-Hegwer, Sergey Beryozkin, Ståle Pedersen, Stéphane Épardaud, Teymur Babayev, Thomas Segismont, Vojin Jovanovic, Willem Jan Glerum, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
