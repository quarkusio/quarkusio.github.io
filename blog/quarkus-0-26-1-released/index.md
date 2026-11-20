# Quarkus 0.26.1 released - Stabilizing things and adding Vault support

Keeping the rapid pace of one release per week, here comes Quarkus 0.26.1.

Thanks a lot to the community at large for the useful bug reports, that helps a lot in stabilizing the new HTTP and security layers. Keep them coming.

Apart from bugfixes, the new big thing in 0.26.1 is an extension adding support for [HashiCorp Vault](https://www.vaultproject.io/).

> [!NOTE]
> The recommended version of GraalVM is still 19.2.0.1 as the 19.2.1 artifacts are still not published on Central.

## What's new?

### Vault support

The latest addition in Quarkus extension universe is a Vault extension. No more plain text database credentials in your configuration files: you can now store them securely in [Vault](https://www.vaultproject.io/).

The extension comes with a MicroProfile config source so you can directly inject secured properties coming from Vault in your application.

More on that in [our extensive guide](https://quarkus.io/guides/vault).

As usual for new extensions, feedback is highly welcome, be it positive feedback or bug reports.

### Gradle plugin published on Gradle portal

The Gradle plugin is now published [on the Gradle portal](https://plugins.gradle.org/plugin/io.quarkus) for each new version as part of our release process.

### Live reload issue on Windows

This time, [the infamous `InvalidPathException` on live reload issue](https://github.com/quarkusio/quarkus/issues/3592) should be fully solved. There was one remaining problem with JDK 11+ due to a JDK bug.

### Full changelog

We also fixed a few bugs and usability issues: get [the full changelog of 0.26.0 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/0.26.0) - and, if you're really curious, [the issues we fixed in 0.26.1](https://github.com/quarkusio/quarkus/releases/tag/0.26.1).

## Contributors

Quarkus has now [166 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for this release, thanks to Alexey Loubyansky, Andrew Guibert, Andy Damevin, cknoblauch, Clement Escoffier, Daniel Petisme, Dusan Odalovic, Gary Brown, George Gastaldi, Georgios Andrianakis, Guillaume Nodet, Guillaume Smet, Gwenneg Lepage, Jaikiran Pai, Jan Martiska, John OHara, Ken Finnigan, Maciej Swiderski, Manyanda Chitimbo, Martin Kouba, Max Rydahl Andersen, Minto van der Sluis, Nelson Graça, Pedro Igor, Sanne Grinovero, Sergey Beryozkin, Stuart Douglas, Stéphane Épardaud, Tom Jenkinson and Vincent Sevel.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
