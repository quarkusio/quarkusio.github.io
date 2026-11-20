# Quarkus 3.37 - Extension-based modularity, Hibernate ORM 7.4, Jackson reflection-free serializers, and more

We're pleased to announce the release of Quarkus 3.37.

This release brings several notable features:

- [#53140](https://github.com/quarkusio/quarkus/pull/53140) - Extension-based modularity
- [#54083](https://github.com/quarkusio/quarkus/pull/54083) - Bump Hibernate ORM to 7.4.0.Final, Reactive to 3.4.0.Final, Search to 8.4.0.Final
- [#54347](https://github.com/quarkusio/quarkus/pull/54347) - Enable Jackson reflection-free serializers by default
- [#54533](https://github.com/quarkusio/quarkus/pull/54533) - Introduce ability to get response metadata in streamed response
- [#54631](https://github.com/quarkusio/quarkus/pull/54631) - Add `quarkus-rest-data-hibernate-types` extension

## Update

To update to Quarkus 3.37, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.37.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.37 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.37).

## What's new?

### Extension-based modularity

Quarkus 3.37 introduces the new `quarkus-jlink` extension, which produces a jlink-ed modular application using [jlink](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html).

By leveraging the Java module system, `jlink` creates a custom runtime image that includes only the JDK modules your application actually needs, stripping out everything else. This results in significantly smaller deployment artifacts compared to shipping a full JDK, which is particularly beneficial for container-based deployments where image size matters.

When this extension is present, default JAR packaging is disabled.

This extension is experimental. Container builds, cross-architecture builds, AOT with jlink, and modular testing are not yet supported.

Have a look at the [jlink guide](/guides/jlink) to get started. As this is still experimental, we welcome your feedback!

### Hibernate ORM 7.4, Hibernate Reactive 3.4, Hibernate Search 8.4

Quarkus 3.37 upgrades Hibernate ORM from 7.3 to 7.4, Hibernate Reactive from 3.3 to 3.4, and Hibernate Search from 8.3 to 8.4.

Hibernate ORM 7.4 includes several behavioral and DDL changes, for instance pagination limits are now processed in SQL, and new NOT NULL constraints are added on timestamp columns. PostgreSQL minimum version is also bumped to 14. Please refer to the [Hibernate ORM 7.4 migration guide](https://docs.hibernate.org/orm/7.4/migration-guide/) for the full list of changes.

Hibernate Search 8.4 is fully backwards-compatible. See the [Hibernate Search 8.4 migration guide](https://docs.hibernate.org/search/8.4/migration/html_single/) for more details.

Also, Elasticsearch Dev Services now defaults to Elasticsearch 9.4 and OpenSearch 3.6.

### Jackson reflection-free serializers enabled by default

Jackson's reflection-free serializers are now enabled by default in Quarkus. This improves serialization performance by avoiding reflection-based serialization.

If you run into any issues, you can disable this optimization by setting:

```properties
quarkus.rest.jackson.optimization.enable-reflection-free-serializers=false
```

and please [report the problem](https://github.com/quarkusio/quarkus/issues) so that we can fix it.

### Streamed response metadata in REST Client

The REST Client introduces `RestMultiResponse`, which allows extraction of status codes and HTTP headers from streamed responses. Previously, getting response metadata from a streamed response required falling back to the Vert.x HTTP Client directly.

### New `quarkus-rest-data-hibernate-types` extension

A new `quarkus-rest-data-hibernate-types` extension has been added. It is automatically included as a conditional dependency when both `quarkus-rest-jackson` and `quarkus-data-hibernate` are part of the application, providing better integration between the REST layer and Hibernate ORM data types.

### Platform updates

Various Platform components were upgraded including:

- Quarkus CXF to 3.37.0
- Quarkus Google Cloud Services to 2.22.0
- Quarkus LangChain4j to 1.11.2
- Quarkus MCP Server to 1.13.0
- Quarkus Vault to 4.8.0

## Full changelog

You can get the full changelog of [3.37.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.37.0.CR1) and [3.37.0](https://github.com/quarkusio/quarkus/releases/tag/3.37.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [1203](https://github.com/quarkusio/quarkus/graphs/contributors) contributors. Many many thanks to each and everyone of them.

In particular for the 3.37 release, thanks to Alex Martel, Alexey Loubyansky, Andreas Maechler, Arend von Reinersdorff, Arthur Navarro, Ashish Thakur, Atharv Hatwar, Aurea Munoz, Beutlin, Bruno Baptista, Carles Arnal, Chris Laprun, Clement Escoffier, David M. Lloyd, Dorothy Cheng, Enoque Duarte, Faisal Dilawar, Foivos Zakkak, Fouad Almalki, George Gastaldi, Georgios Andrianakis, Gorre Surya, Guillaume Smet, Holly Cummins, Inaki Villar, Jan Martiska, jcarranzan, Jesse White, Jiri Ondrusek, Julien Ponge, Katia Aresti, Kristian Rickert, Ladislav Thon, Laurent Goujon, lloydmeta, loiclefevre, lu1tr0n, Luca Molteni, Marco Belladelli, Marco Sappe Griot, Marek Skacelik, mariofusco, marko-bekhta, Martin Kouba, Matej Novotny, Matheus Cruz, Max Rydahl Andersen, MdTanwer, Michael Edgar, Michael Hertel, Michal Maléř, Michal Vavřík, Nikolas Schmidt-Voigt, Ozan Gunalp, Pantazis Vouzaxakis, Phillip Krüger, Roberto Cortez, Rolfe Dlugy-Hegwer, Sergey Beryozkin, Shivam Srivastav, Stéphane Épardaud, Sven Zbinden, Tamas Cservenak, Teymur Babayev, Willem Jan Glerum, xstefank, Yoann Rodière, and Zahanturel.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
