# Quarkus 3.2.0.Final released - New security features, @QuarkusComponentTest

It is our pleasure to announce the release of Quarkus 3.2.0.Final.

> [!NOTE]
> Quarkus 3.2 is the first of the [now announced Long Term Support releases of Quarkus](/blog/lts-releases/).

Even if [we will maintain 2.16 for a few months still](/blog/quarkus-2-16-8-final-released/), we encourage all our users to start their migration journey to Quarkus 3.

Major changes are:

\* Various new security features \* The ability to test CDI components with `@QuarkusComponentTest`  \* Build time analytics

Together with improvements in a lot of areas.

## Migration Guide

To migrate from 3.1, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2).

If you are not already using 3.0, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details. Once you upgraded to 3.0, also have a look at the [3.1 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1)

## What's new?

### Build time analytics

To be able to better understand how Quarkus is used, we have added a new build time analytics feature. It is anonymous and opt-in thus fully voluntary.

On first startup of `quarkus dev` you will be asked if you want to share build time analytics with the Quarkus community. If you answer yes, then on `quarkus dev` and builds a small package is sent with information about Java, Maven/Gradle, Quarkus versions, public extensions used, build time, etc. This will help us to better understand how Quarkus is used and what we can improve. If you answer no, then no data is sent.

You can read more on how this works in <https://quarkus.io/usage> and find more technical details in this [quarkus-dev](https://groups.google.com/g/quarkus-dev/c/Iw17La9pgX) post and this [pull request](https://github.com/quarkusio/quarkus/pull/33555).

### Security

Several new features were added to our security layer making it better than ever:

- Custom claim types are now supported in `quarkus-test-security-jwt` and `quarkus-test-security-oidc`.
- It is now possible to customize the OIDC verification, which gives you more flexibility when OIDC providers are doing funky stuff.
- We provide a default static tenant resolver which will reduce the boilerplate in simple multi-tenant configurations.
- The OIDC extension has been integrated in the new Dev UI introduced with Quarkus 3.

### Platform - Quarkus CXF

[Quarkus CXF](https://docs.quarkiverse.io/quarkus-cxf/dev/index.html) got a part of the previous Quarkus Platform release 3.1.0.Final already, but we forgot to announce it at that time (sorry!). Quarkus CXF ports [Apache CXF](https://cxf.apache.org/) to Quarkus. It can be used for writing SOAP [WebServices](https://docs.quarkiverse.io/quarkus-cxf/dev/user-guide/first-soap-web-service.html) and [Clients](https://docs.quarkiverse.io/quarkus-cxf/dev/user-guide/first-soap-client.html). You can now create project stubs via [code.quarkus.io](https://code.quarkus.io/?e=io.quarkiverse.cxf%3Aquarkus-cxf&extension-search=origin:platform%20quarkus%20cxf).

With Quarkus Platform 3.2.0.Final, the status of the [Quarkus CXF Metrics Feature](https://docs.quarkiverse.io/quarkus-cxf/dev/reference/extensions/quarkus-cxf-rt-features-metrics.html) changed from experimental to stable. The extension is now properly tested both in JVM and native modes.

### QuarkusComponentTest

Quarkus 3.2 introduces an experimental feature to ease the testing of CDI components and mocking of their dependencies.

Stay tuned, this feature will be presented in details in a future blog post.

In the meantime, have a look at [this new section of our Testing guide](/guides/getting-started-testing#testing-components).

### MongoDB

Programmatic transaction support has been added to MongoDB Reactive with Panache.

You can find more information about this new feature in the [MongoDB with Panache](/guides/mongodb-panache#reactive-transactions) guide.

### Reactive datasources

Support for ``CredentialProvider``s changing values throughout the life of the application has been added.

### Reactive REST Client

Support for `@Encoded` for path and query parameters has been added.

### Developer experience

- The `x` command in dev mode used to open the IDE when you had an exception raised. It now opens the IDE even if no exceptions have been raised.
- JUnit tag expressions are now fully supported in the `quarkus test` command.

### io_uring

If you are interested in using io_uring with Quarkus, please have a look at the [new documentation that has been added](/guides/vertx-reference#use-io_uring).

## Full changelog

You can get the full changelog of [3.2.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.2.0.CR1) and [3.2.0.Final](https://github.com/quarkusio/quarkus/releases/tag/3.2.0.Final) on GitHub.

## Contributors

The Quarkus community is growing and has now [818 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.2 release, thanks to Ales Justin, Alex Martel, Alexey Loubyansky, Andrea Peruffo, Andy Damevin, Avinash Gupta, Bill Burke, Bruno Oliveira da Silva, brunobat, Chris Laprun, Clement Escoffier, dagrammy, Daryl Koh, Dave Maughan, David M. Lloyd, David Voit, Davide D'Alto, Emmanuel Bernard, Eric Deandrea, Erin Schnabel, Falko Modler, Foivos Zakkak, Fouad Almalki, franz1981, Gareth Healy, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Harald Albers, Holly Cummins, Ioannis Canellos, Jan Martiska, Jonas Kleinebecker, Jose Carvajal, Julien Ponge, Jérôme Tama, Kai Suchomel, Katia Aresti, kdnakt, Kevin Dubois, Ladislav Thon, Loïc Mathieu, Manyanda Chitimbo, Marc Nuri, Marco Schaub, Marko Bekhta, Martin Kouba, Martin Panzer, Matej Novotny, Matej Vasek, Max Rydahl Andersen, Maximilian Zellhofer, Michael Edgar, Michael Kroll, Michal Karm Babacek, Michal Maléř, Michal Vavřík, Michelle Purcell, Ozan Gunalp, Peter Palaga, Phillip Krüger, Rhuan Rocha, rjtmahinay, Robert Stupp, Roberto Cortez, Rolfe Dlugy-Hegwer, Romain Pelisse, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Severin Gehwolf, Stefan, Stuart Douglas, Stéphane Épardaud, Sébastien Crocquesel, Thomas Segismont, xstefank, Yoann Rodière, Yoshikazu Nojima, Zheng Feng, and Zineb Bendhiba.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
