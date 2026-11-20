# Quarkus 3.7 - Java 17 baseline, Hibernate ORM 6.4, @MeterTag...

It is with great pleasure that we are announcing the release of Quarkus 3.7.

With Quarkus 3.7, we begin the journey that will lead to Quarkus 3.8 being the next LTS version of Quarkus. We strongly encourage you to update to this version and provide feedback to make our next LTS version strong and stable.

Major changes are:

- [#37335](https://github.com/quarkusio/quarkus/pull/37335) - Java 17 is the new baseline
- [#38190](https://github.com/quarkusio/quarkus/pull/38190) - Rename RESTEasy Classic client extensions to resteasy-client
- [#36978](https://github.com/quarkusio/quarkus/pull/36978) - Upgrade to Hibernate ORM 6.4, Hibernate Search 7.0, Hibernate Reactive 2.2
- [#35065](https://github.com/quarkusio/quarkus/pull/35065) - Add Hibernate Search management endpoint
- [#36945](https://github.com/quarkusio/quarkus/pull/36945) - Support Micrometer `@MeterTag`
- [#37152](https://github.com/quarkusio/quarkus/pull/37152) - Support token verification with the inlined certificate chain
- [#37269](https://github.com/quarkusio/quarkus/pull/37269) - Support certificate role mappings
- [#37472](https://github.com/quarkusio/quarkus/pull/37472) - Provide a way to observe security events
- [#37730](https://github.com/quarkusio/quarkus/pull/37730) - Introduce LinkedIn OIDC provider
- [#37891](https://github.com/quarkusio/quarkus/pull/37891) - Split OIDC session cookie if its size is more than 4KB
- [#38029](https://github.com/quarkusio/quarkus/pull/38029) - Allow applications using quakus-info to contribute data to the /info using CDI
- [#38066](https://github.com/quarkusio/quarkus/pull/38066) - Drop Okhttp/Okio from BOM

3.7.1 also fixes the following CVEs:

- [CVE-2023-5675](https://access.redhat.com/security/cve/CVE-2023-5675) Authorization flaw in Quarkus RESTEasy Reactive and Classic when `quarkus.security.jaxrs.deny-unannotated-endpoints` or `quarkus.security.jaxrs.default-roles-allowed` properties are used
- [CVE-2023-6267](https://access.redhat.com/security/cve/CVE-2023-6267) JSON payload getting processed prior to security checks when REST resources are used with annotations

We also released 3.6.9 to address these issues in 3.6, in case you encounter problems updating to 3.7.

As usual, this version also comes with bugfixes, performance improvements and documentation improvements.

As mentioned in the previous minor announcement, we currently maintain two version streams in the community:

- 3.7: it is the latest and greatest and it introduces new features
- 3.2: it is our current [LTS release](/blog/lts-releases/)

Quarkus 2.x is not maintained in the community anymore. If you are using the community version, please upgrade to Quarkus 3.x (either 3.2 LTS or 3.7). We recommend using `quarkus update` to do so.

## Update

To update to Quarkus 3.7, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.7.

To migrate from 3.6, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.7).

If you are not already using 3.x, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details. Once you upgraded to 3.0, also have a look at the [3.1](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1), [3.2](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2), [3.3](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.3), [3.4](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.4), [3.5](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.5), and [3.6](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.6) migration guides.

## What's new?

### Java 17 is the new baseline

As was announced at [the end of November](https://quarkus.io/blog/java-17/), you will need a Java 17+ runtime (using Java 21 is encouraged!) to run Quarkus 3.7 applications.

When we released Quarkus 3.0, we announced that Java 11 support was deprecated and would go away at some point. This is the time.

Most of the Java ecosystem is moving to Java 17 as the baseline so it wasn't sustainable for Quarkus to support Java 11 much longer.

If your Java 17 migration is ongoing, you can upgrade to the latest 3.6 but keep in mind that 3.6.9 released today is the latest 3.6. If you are stuck with Java 11 for some time, we recommend using Quarkus 3.2 LTS as it will be maintained longer.

### RESTEasy Classic Client extensions renamed

As part of the renaming of the RESTEasy Reactive extensions that will be spread across several releases, we renamed the `quarkus-rest-client*` extensions (client extensions for RESTEasy Classic) to `quarkus-resteasy-client*`, which makes it clearer that it is the client counterpart of `quarkus-resteasy`.

Relocations have been put in place to not break your applications but we recommend that you adjust your applications already as these particular artifacts (`quarkus-rest-client*`) will be switched to RESTEasy Reactive in Quarkus 3.9.

See <https://github.com/quarkusio/quarkus/blob/main/adr/0002-reactive-rename.adoc> for the full context of this change.

We encourage you to move to the new artifacts listed in the following table:

| Old name | New name |
| --- | --- |
| quarkus-rest-client | quarkus-resteasy-client |
| quarkus-rest-client-jackson | quarkus-resteasy-client-jackson |
| quarkus-rest-client-jaxb | quarkus-resteasy-client-jaxb |
| quarkus-rest-client-jsonb | quarkus-resteasy-client-jsonb |
| quarkus-rest-client-mutiny | quarkus-resteasy-client-mutiny |

### Hibernate ORM 6.4, Hibernate Search 7.0, Hibernate Reactive 2.2

We updated the Hibernate persistence stack to:

- Hibernate ORM 6.4 - see [announcement](https://in.relation.to/2023/11/23/orm-640-final/)
- Hibernate Search 7.0 - see [announcement](https://in.relation.to/2023/12/05/hibernate-search-7-0-0-Final/)
- Hibernate Reactive 2.2 - see [announcement](https://in.relation.to/2023/11/28/hibernate-reactive-2_2_Final/)

We now expose a management endpoint for Hibernate Search, which allows to trigger mass indexing and other maintenance tasks. You can find out more about it in the [updated documentation](https://quarkus.io/guides/hibernate-search-orm-elasticsearch#management).

### Micrometer @MeterTag

Micrometer `@MeterTag` can now be used to dynamically assign tag values to metrics. See the [dedicated doc section](https://quarkus.io/guides/telemetry-micrometer#annotations) for more information.

### Assorted security improvements

Our security extensions got a bunch of new features, improvements:

- [#37152](https://github.com/quarkusio/quarkus/pull/37152) - Support token verification with the inlined certificate chain
- [#37269](https://github.com/quarkusio/quarkus/pull/37269) - Support certificate role mappings
- [#37472](https://github.com/quarkusio/quarkus/pull/37472) - Provide a way to observe security events
- [#37730](https://github.com/quarkusio/quarkus/pull/37730) - Introduce LinkedIn OIDC provider
- [#37891](https://github.com/quarkusio/quarkus/pull/37891) - Split OIDC session cookie if its size is more than 4KB

### Contribute data to /info endpoint

All CDI beans implementing `InfoContributor` will contribute to the `/info` endpoint.

### Okhttp/Okio versions not enforced anymore

As we didn't want to rely on the Kotlin runtime for non-Kotlin-related extensions, we were enforcing a very old version of Okhttp in Quarkus, thus making using newer Okhttp version harder.

For several versions, we have been working on reducing our dependency to Okhttp to be able to avoid enforcing the version in Quarkus.

This is now effective in 3.7.

## Full changelog

You can get the full changelog of [3.7.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.7.0.CR1), [3.7.0](https://github.com/quarkusio/quarkus/releases/tag/3.7.0), and [3.7.1](https://github.com/quarkusio/quarkus/releases/tag/3.7.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [903 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.7 release, thanks to a29340, Abdul Rauf, Ales Justin, Alex Katlein, Alex Martel, Alexei Bratuhin, Alexey Loubyansky, Alexey Kovynev, Andrea Peruffo, Andreas Eberle, Andy Damevin, Anton Vasilev, Auri Munoz, barreiro, Bas Passon, Benedikt Schneppe, Bernhard Schuhmann, Björn Großewinkelmann, Björn Konrad, Bruno Baptista, Bruno Caballero, brunobat, Carles Arnal, Chris Laprun, Christian Thiel, Clement Escoffier, David Andlinger, David M. Lloyd, Davide D'Alto, Debabrata Patnaik, elendis, Eric Deandrea, Erin Schnabel, Falko Modler, Fedor Dudinskiy, Foivos Zakkak, Fortran, Francesco Nigro, Frantisek Havel, Gasper Kojek, George Gastaldi, Georgios Andrianakis, Gero Müller, Guillaume Smet, Håkan Öström, Idryss Bourdier, Ioannis Canellos, Jakub Jedlicka, Jakub Scholz, Jan Martiska, Jerome Prinet, Jiří Locker, Jonathan Kolberg, Jorge Solórzano, Jose Carranza, jtama, Julien Ponge, Justin Lee, Katia Aresti, Ladislav Thon, Leonor Boga, Loïc Mathieu, luca-bassoricci, Luke Morfill, Maciej Lisowski, Marc Nuri, Marco Schaub, Marek Skacelik, Marko Bekhta, Martin Kofoed, Martin Kouba, Marvin B. Lillehaug, Matej Novotny, Matheus Cruz, Max Rydahl Andersen, mert18, Michael Edgar, Michael Musgrove, Michael Rasmussen, Michal Karm Babacek, Michal Maléř, Michal Vavřík, Michelle Purcell, Mickey Maler, Miroslav Vasko, Ozan Gunalp, Pablo Gonzalez Granados, Peter Palaga, Phillip Krüger, Radim Vansa, rmartinc, Roberto Cortez, rob.spoor, Rolfe Dlugy-Hegwer, Rostislav Svoboda, Sanne Grinovero, Sebastian Schuster, Sergey Beryozkin, Severin Gehwolf, shjones, SIMULATAN, Stephan Strate, stianst, Stuart Douglas, Stéphane Épardaud, Tamaro Skaljic, troosan, Vitaliy Baschlykoff, Waldemar Reusch, Welton Rodrigo Torres Nascimento, Wladimir Hofmann, wrongwrong, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
