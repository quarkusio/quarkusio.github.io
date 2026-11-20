# Quarkus 3.33 LTS - new LTS version

It is our pleasure to announce the release of Quarkus 3.33, which is our new LTS (Long Term Support) version.

This version is built on the top of Quarkus 3.32. New features landed in [Quarkus 3.34](https://quarkus.io/blog/quarkus-3-34-released/), which was also released today.

If you want to know more about our LTS policy, the [LTS announcement](https://quarkus.io/blog/lts-releases/) is a must read.

LTS releases are supported for 12 months.

If you are coming from the previous LTS, Quarkus 3.27 LTS, there are a lot of exciting new features and we recommend reading the following announcements:

- [Quarkus 3.28 - More security features, custom Grafana dashboards, support for multiple clients in Liquibase MongoDB, and more](https://quarkus.io/blog/quarkus-3-28-released/)
- [Quarkus 3.29 - Multiple cache backends and Qute DAP debugger support](https://quarkus.io/blog/quarkus-3-29-released/)
- [Quarkus 3.30 - JsonView on REST Client, Hibernate Validator 9.1, CLI decrypt command, and more](https://quarkus.io/blog/quarkus-3-30-released/)
- [Quarkus 3.31 - Full Java 25 support, Quarkus Maven packaging, Panache Next, and more!](https://quarkus.io/blog/quarkus-3-31-released/)
- [Quarkus 3.32 - Project Leyden integration, more graceful shutdown, automatic Consul registration and more!](https://quarkus.io/blog/quarkus-3-32-released/)

## Update

To update to Quarkus 3.33 LTS, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.33 LTS.

If you are upgrading from 3.32, there's nothing to do as 3.33 LTS is the direct continuation of 3.32.

If you are upgrading from the previous LTS, Quarkus 3.27 LTS, please refer to the following migration guides:

- [Migration guide for 3.28](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.28)
- [Migration guide for 3.29](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.29)
- [Migration guide for 3.30](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.30)
- [Migration guide for 3.31](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.31)
- [Migration guide for 3.32](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.32)
- [Migration guide for 3.33](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.33) - this one is empty as 3.33 is the continuation of 3.32

Note that `quarkus update` should handle most of the heavy lifting for you, but there are still cases that should be handled manually and we recommend reading these migration guides carefully.

## What's new?

### Platform component upgrades

Various Platform components were upgraded including:

- Quarkus CXF to 3.33.0 - see [release notes](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.33.0.html)
- Camel Quarkus to 3.33.0
- Quarkus Operator SDK to 7.7.3
- Quarkus Vault to 4.7.0
- Quarkus Qpid JMS to 2.12.0

## Full changelog

The core part of Quarkus 3.33 LTS is based on Quarkus 3.32 with some additional fixes included in 3.33.1.

You can get the full changelog of [3.33.1](https://github.com/quarkusio/quarkus/releases/tag/3.33.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1173 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.33 release, thanks to Ales Justin, Alexey Loubyansky, Andy Damevin, Asger Askov Blekinge, Aurea Munoz, Aurélien Pupier, brunobat, Clement Escoffier, Clément de Tastes, David M. Lloyd, Dmitri Bourlatchkov, Erwin Oegema, Faisal Dilawar, Foivos Zakkak, Gaelle Fournier, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Holly Cummins, Jakub Jedlicka, jcarranzan, Jens Teglhus Møller, Julien Ponge, Katia Aresti, Kristian Rickert, Ladislav Thon, Luca Molteni, Martin Kouba, Martin Panzer, María Arias de Reyna Domínguez, Matej Novotny, Michal Maléř, Michal Vavřík, Nico Hinrichs, Ozan Gunalp, Patrick Schaub, Phillip Kruger, Roberto Cortez, Sanne Grinovero, Sergey Beryozkin, shjones, Stéphane Épardaud, Thomas McWork, tiwari91, Tom Schindl, and Yoann Rodière.

The list is a bit smaller than usual as 3.33 LTS only contains bugfixes on top of 3.32.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
