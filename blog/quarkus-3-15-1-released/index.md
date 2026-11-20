# Quarkus 3.15 - new LTS version

It is our pleasure to announce the release of Quarkus 3.15, which is our new LTS (Long Term Support) version.

This version is built on the top of Quarkus 3.14 with some additional bugfixes. New features will come with Quarkus 3.16 that we will release at the end of October.

If you want to know more about our LTS policy, the [LTS announcement](https://quarkus.io/blog/lts-releases/) is a must read.

LTS releases are supported for 12 months.

If you are coming from the previous LTS, Quarkus 3.8, there are a lot of exciting new features and we recommend reading the following announcements:

- [Quarkus 3.9 - Big Reactive Rename](https://quarkus.io/blog/quarkus-3-9-1-released/)
- [Quarkus 3.10 - Hibernate Search standalone POJO mapper, Flyway 10, security enhancements](https://quarkus.io/blog/quarkus-3-10-0-released/)
- [Quarkus 3.11 - Dev Services for Observability, progress on WebSockets.next, Infinispan Cache extension and more](https://quarkus.io/blog/quarkus-3-11-0-released/)
- [Quarkus 3.12 - TLS Registry, load shedding, native image agent, Kotlin 2.0 and more](https://quarkus.io/blog/quarkus-3-12-0-released/)
- [Quarkus 3.13 - OpenTelemetry Metrics, OpenTelemetry 1.39, TLS registry improvements and more...](https://quarkus.io/blog/quarkus-3-13-0-released/)
- [Quarkus 3.14 - Hibernate ORM 6.6, Let's encrypt, faster reflection-free Jackson serializers...](https://quarkus.io/blog/quarkus-3-14-1-released/)

## Update

To update to Quarkus 3.15, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.15 LTS.

If you are upgrading from 3.14, there's nothing to do as 3.15 is the direct continuation of 3.14.

If you are upgrading from the previous LTS, Quarkus 3.8, please refer to the following migration guides:

- [Migration guide for 3.9](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9)
- [Migration guide for 3.10](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.10)
- [Migration guide for 3.11](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.11)
- [Migration guide for 3.12](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.12)
- [Migration guide for 3.13](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.13)
- [Migration guide for 3.14](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.14)
- [Migration guide for 3.15](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.15) - this one is empty as 3.15 is the continuation of 3.14

Note that `quarkus update` should handle most of the heavy lifting for you, but there are still cases that should be handled manually and we recommend reading these migration guides carefully.

Keep in mind that we try to reduce the upgrade complexity by making the transition smoother. For instance, when renaming artifacts, we provide relocations and these relocations are kept until the next LTS is released.

`quarkus-resteasy-reactive` was renamed to `quarkus-rest` in Quarkus 3.9, the relocation is still in place in Quarkus 3.15 and has been dropped only for the upcoming Quarkus 3.16, allowing you a smoother transition to 3.15. It is recommended to move to the new artifacts but it is not mandated for 3.15 (again, `quarkus update` will take care of it if you're using it).

## What's new?

### Platform component upgrades

#### Camel Quarkus

Camel Quarkus has been updated to 3.15.0. You can find everything you need to know about it in the [release notes](https://camel.apache.org/releases/q-3.15.0/).

#### Quarkus CXF

Quarkus CXF 3.15 was released and is now available in [Quarkus Platform 3.15](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check the [Quarkus CXF 3.15.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.15.0.html) and [Quarkus CXF 3.15.1](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.15.1.html) release notes for more information about what is new in this release.

## Full changelog

You can get the full changelog of [3.15.0](https://github.com/quarkusio/quarkus/releases/tag/3.15.0) and [3.15.1](https://github.com/quarkusio/quarkus/releases/tag/3.15.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1003 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.15 release, thanks to AB, Ales Justin, Alexey Loubyansky, Auri Munoz, AxiomaticFixedChimpanzee, brunobat, Claudio Miranda, Clement Escoffier, David M. Lloyd, Foivos Zakkak, Francesco Nigro, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Gunther C. Wenda, Holly Cummins, Inaki Villar, Ioannis Canellos, Jakub Jedlicka, Jan Martiska, Jérémie Bresson, Katia Aresti, KERN Christian, Lars, Loic Hermann, luneo7, mariofusco, marko-bekhta, Martin Bartoš, Martin Kouba, Matej Novotny, Matheus Cruz, Matthias Schorsch, Max Rydahl Andersen, Maximilian Rehberger, Michal Maléř, Michal Vavřík, Nicholas Kolatsis, Ozan Gunalp, Peer Bech Hansen, peubouzon, Phillip Krüger, Roberto Cortez, Rolfe Dlugy-Hegwer, Roman Lovakov, Rostislav Svoboda, Sergey Beryozkin, Thomas Canava, xstefank, Yasser Greyeb, Yoann Rodière, and Yurii Dubinka.

The list is a bit smaller than usual as 3.15 only contains bugfixes on top of 3.14.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
