# Quarkus 0.23.1 released - Paving the way to our new HTTP layer

> [!WARNING]
> DO NOT USE 0.23.1 - PLEASE USE 0.23.2.
>
> We found a major memory regression in 0.23.1, we urge you to not use it and use 0.23.2 instead.

We just released Quarkus 0.23.1. This version is an important milestone for us as it materializes one of our major objectives: relying on [Vert.x](https://vertx.io/) for our entire HTTP layer. It also comes with a lot of improvements and bugfixes contributed by our vibrant community.

> [!NOTE]
> The recommended version of GraalVM is now 19.2.0.1 so [please upgrade](https://www.graalvm.org/downloads/)!

## What's new?

### New HTTP layer

We are making progress on basing our entire HTTP layer (meaning Undertow, our servlet implementation, and RESTEasy, our JAX-RS implementation) on top of Vert.x.

0.23 is the first step of this major change: our servlet implementation is now making use of Vert.x under the hood. Note that this is only the first change of many to come as, in our next version, RESTEasy will be able to avoid servlets entirely and leverage Vert.x directly.

A more comprehensive blog post explaining everything you have to know about it will be published soon so stay tuned.

### JDK 12+ support

Until now, Quarkus only supported JDK 8 and 11 (the so-called LTS JDKs). We now support JDK 12 and 13.

Note that if you want to build GraalVM native executables, you are still limited to JDK 8 for the time being.

### New extensions

#### Azure functions

Slightly related to our new HTTP layer, we now have an extension for Azure functions support and [a guide](https://quarkus.io/guides/azure-functions-http) explaining all you need to know to deploy Azure functions based on Quarkus.

#### MySQL, here we come

We have been a bit shy on our MySQL support as we prioritized the support of MariaDB.

As good news never come alone, it's not one extension we added but two:

\* `quarkus-jdbc-mysql` - An extension for the official MySQL JDBC driver you can use with Hibernate ORM \* `quarkus-reactive-mysql-client` - A reactive client for MySQL

#### ActiveMQ Artemis

If you are into messaging, we have one more option for you, [ActiveMQ Artemis](https://activemq.apache.org/components/artemis/), in the form of the `quarkus-artemis-core` and `quarkus-artemis-jms` extensions.

#### JGit

We have added an extension for JGit: say hello to `quarkus-jgit`. More information in the [JGit extension guide](https://quarkus.io/guides/jgit).

### Various extension improvements

**Amazon DynamoDB**\
The Amazon DynamoDB extension is now fully "quarked": you can configure it fully via our centralized `application.properties` and inject the appropriate beans in your applications. We also have \[a brand new guide\].

**Apache Tika**\
You can now define the parsers you want to use: only these parsers are included in the native image, greatly reducing its size. See [the Apache Tika guide](https://quarkus.io/guides/tika) for more information.

**Hibernate ORM**\
The default value of `quarkus.hibernate-orm.sql-load-script` has been changed from `import.sql` to `no-file` by default **when the `prod` profile is enabled**. That means that if you want to load data in `prod` mode, you need to explicitly point to your import file.

**Jaeger**\
The metrics were wrongly added to the `base` metrics: they are now added to the `vendor` metrics.

**Kafka Streams**\
You can now leverage the awesomeness of Live Coding when developing Kafka Streams applications.

### Scala support

Together with Java and Kotlin, we now also support Scala. Just add the `quarkus-scala` extension when creating a new project (using [code.quarkus.io](https://code.quarkus.io/) or the command line) and you will get a Scala project.

### Deprecations

The `quarkus-junit4` extension has been retired. You may use the `quarkus-junit5` extension and move your tests to JUnit 5.

### Full changelog

We also fixed a few bugs and usability issues: get [the full changelog of 0.23.0 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/0.23.0) - and, if you're really curious, [the issue we fixed in 0.23.1](https://github.com/quarkusio/quarkus/releases/tag/0.23.1).

## Contributors

Quarkus has now [149 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for this release, thanks to Alessio Soldano, Alex Soto, Alexey Loubyansky, Andy Damevin, Andy Muir, Arne Mejlholm, Aurea Munoz, Bill Burke, chaminnk, Clement Escoffier, Cristiano Nicolai, Damien, Daniel Ribeiro, David M. Lloyd, Dennis Baerten, Emmanuel Bernard, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Guillaume Dufour, Guillaume Smet, Gunnar Morling, Gwenneg Lepage, Jacob Middag, Jaikiran Pai, Jan Martiska, Jason T. Greene, Justin Lee, Ken Finnigan, Libor Krzyzanek, Loïc Mathieu, Maarten Mulders, Maciej Swiderski, Manyanda Chitimbo, Marcin Czeczko, Martin Kouba, Matej Novotny, Matteo Mortari, Max Rydahl Andersen, Michal Szynkiewicz, Minto van der Sluis, Mitesh Aghera, Moncef Aoudia, Paweł Żalejko, Peter Palaga, Roberto Gamarra, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, soberich, Stuart Douglas, Stéphane Épardaud, Thomas Segismont, Vincent Sevel, William Burns and William Siqueira.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
