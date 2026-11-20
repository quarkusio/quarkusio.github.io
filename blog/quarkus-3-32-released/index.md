# Quarkus 3.32 - Project Leyden integration, more graceful shutdown, automatic Consul registration and more!

We're pleased to announce the release of Quarkus 3.32.

This is an important milestone: 3.32 marks the feature freeze for our next LTS release, Quarkus 3.33 LTS. The upcoming LTS will be branched from the 3.32 line, making this release the foundation on which 3.33 LTS will be built.

Quarkus 3.32 also delivers several significant improvements and new features:

- [#52376](https://github.com/quarkusio/quarkus/pull/52376) and [#52224](https://github.com/quarkusio/quarkus/pull/52224) - Project Leyden integration
- [#50975](https://github.com/quarkusio/quarkus/pull/50975) - Rework graceful shutdown to be more graceful with respect to HTTP
- [#47997](https://github.com/quarkusio/quarkus/pull/47997) - Automatic Consul Registration for Quarkus Applications
- [#52090](https://github.com/quarkusio/quarkus/pull/52090) - OIDC: Support for custom DPoP nonce providers
- [#52353](https://github.com/quarkusio/quarkus/pull/52353) - OIDC: Add basic support for rich authorization requests
- [#52175](https://github.com/quarkusio/quarkus/pull/52175) - Make built-in authentication mechanism order customizable
- [#52537](https://github.com/quarkusio/quarkus/pull/52537) - Introduce OIDC AuthenticationCompletionAction
- [#52380](https://github.com/quarkusio/quarkus/pull/52380) - Upgrade to Google Cloud Function framework 2.0

We strongly recommend upgrading to 3.32 soon so we can gather feedback and ensure the upcoming 3.33 LTS is as solid as possible.

If you are currently running 3.27 LTS, now is the right time to validate your upgrade path to 3.33 LTS, and let us know if you encounter any issues. Your feedback at this stage is especially valuable.

## Update

To update to Quarkus 3.32, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.32.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.32 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.32).

## What's new?

### Project Leyden integration

Project Leyden brings Ahead-of-Time (AOT) compilation to the JVM. By recording class loading, class linking, and method profiling during a training run, you can generate an AOT cache file that the JVM can use for subsequent runs.

The result is a significant improvement in JVM startup time, with very few constraints or drawbacks.

Project Leyden is available starting with OpenJDK 25 (counting only LTS releases). We'll explore it in detail in a dedicated blog post very soon.

In Quarkus, we've integrated Project Leyden AOT so that:

- The training run can be triggered either at build time (minimal training) or through your integration tests.
- The generated AOT cache file can be included directly in your container image.

While the improvements aren't quite as substantial as with native executables, and the container image size is a bit larger due to the AOT cache file, the results are impressive given the minimal effort required:

- A minimal REST application created with quarkus create app sees startup drop from **370 ms** to **80 ms**.
- A large REST CRUD application with 9,000 classes goes from **3 seconds** to **900 ms**.

You can learn more in the [Quarkus AOT guide](https://quarkus.io/guides/aot), and we'll share a detailed walkthrough in the upcoming blog post.

### Graceful shutdown

Several improvements were made to our HTTP graceful shutdown, thanks to some work from the Keycloak team.

### Stork

Quarkus applications can now automatically register to Consul, using the Stork extension.

This new feature is explained in detail in the [Automatic service registration with SmallRye Stork for Quarkus](https://quarkus.io/guides/stork-registration) guide.

### Security

As usual, this version comes with new security features and enhancements:

- [#52090](https://github.com/quarkusio/quarkus/pull/52090) - OIDC: Support for custom DPoP nonce providers
- [#52353](https://github.com/quarkusio/quarkus/pull/52353) - OIDC: Add basic support for rich authorization requests
- [#52175](https://github.com/quarkusio/quarkus/pull/52175) - Make built-in authentication mechanism order customizable
- [#52537](https://github.com/quarkusio/quarkus/pull/52537) - Introduce OIDC AuthenticationCompletionAction

### Google Cloud Function

The Google Cloud Function framework has been updated to 2.0.

### Platform updates

We updated Camel Quarkus to Camel Quarkus 3.32. For more information, see the [announcement](https://camel.apache.org/blog/2026/02/camel-quarkus-3.32.0/).

Quarkus CXF was also updated to 3.32. Since we forgot to include the 3.31.1 release notes in previous Quarkus announcements, you can now read both sets of notes: [3.31.1](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.31.1.html) and [3.32.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.32.0.html).

## Full changelog

You can get the full changelog of [3.32.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.32.0.CR1), [3.32.0](https://github.com/quarkusio/quarkus/releases/tag/3.32.0), and [3.32.1](https://github.com/quarkusio/quarkus/releases/tag/3.32.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1163 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.32 release, thanks to Ales Justin, Alexander Schwartz, Alexandre Dutra, Alexey Loubyansky, andreatp, Asger Askov Blekinge, Aurea Munoz, Aurélien Pupier, azerr, Bruno Baptista, Carles Arnal, cfitzw, Chris Laprun, Clement Escoffier, Daniel Vergien, David M. Lloyd, Dennis Kniep, Erwin Oegema, Foivos Zakkak, Fouad Almalki, Frank Eichfelder, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Holly Cummins, Jakub Jedlicka, James Netherton, Jerome Prinet, Jiri Ondrusek, jtama, Julien Ponge, Karm Michal Babacek, Katia Aresti, Kristian Rickert, Ladislav Thon, lberrymage, lloydmeta, Loïc Mathieu, Marc Nuri, Marco Belladelli, marco sappe griot, mariofusco, marko-bekhta, Martin Kouba, Martin Panzer, Matej Novotny, Matheus Cruz, melloware, Michael Edgar, Michal Vavřík, Nejc Tomažič, Nicola Concetti, Olivier V, osoohynn, Ozan Gunalp, Patrick Schaub, Peter Levart, Peter Palaga, Phillip Krüger, Robert Stupp, Robert Toyonaga, Roberto Cortez, Sanne Grinovero, Sergey Beryozkin, shjones, sNiXx, Steve Hawkins, Stéphane Épardaud, Teymur Babayev, Victor Dalosto, Vincent Sevel, vincent-duluth, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
