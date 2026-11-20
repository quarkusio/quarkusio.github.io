# Quarkus 2.6.0.Final released - SmallRye Reactive Messaging 3.13 and Kafka 3, programmatic API for caching, Kotlin 1.6 and a lot more!

It is my great pleasure to announce the release of Quarkus 2.6.0.Final which comes with some nice changes:

- Some extensions moved to Quarkiverse Hub
- SmallRye Reactive Messaging 3.13 and Kafka 3
- Programmatic API for caching
- Smaller image for native executables
- Built-in UPX compression
- AWT extension
- Kotlin 1.6
- Camel 3.14

> [!NOTE]
> Due to the way we schedule our releases, Quarkus 2.6.0.Final doesn't include [the fixes from Quarkus 2.5.4.Final](https://quarkus.io/blog/quarkus-2-5-4-final-released/). We will release Quarkus 2.6.1.Final on Friday with all these fixes and more.

## Migration Guide

To migrate from 2.5, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.6).

## What's new?

### Extensions moved to Quarkiverse Hub

At the beginning of the Quarkus project, we didn't have anything to properly host extensions outside of the Quarkus project, nor did we have infrastructure to, for instance, display external extensions on [code.quarkus.io](https://code.quarkus.io).

Since then, we have made a lot of progress and everything is in place to support external extensions, be they hosted in the [Quarkiverse Hub](https://github.com/quarkiverse/) or in other organizations/repositories.

The fact is that the Core had become very big (1000+ Maven modules) and it was the barrier of entry was high, be it to contribute to the Core itself or to one of the smaller extensions inside it.

We decided to move some of the extensions to the Quarkiverse Hub so that they are easier to contribute to and to allow us to reduce the size of the Core repository.

The extensions that have been moved are:

- [Amazon Alexa](https://github.com/quarkiverse/quarkus-amazon-alexa)
- [Amazon Services](https://github.com/quarkiverse/quarkus-amazon-services) (DynamoDB, S3...)
- [Artemis](https://github.com/quarkiverse/quarkus-artemis)
- [Consul Config](https://github.com/quarkiverse/quarkus-config-extensions)
- [AWS support for Hibernate Search ORM + Elasticsearch](https://github.com/quarkiverse/quarkus-hibernate-search-extras)
- [JGit](https://github.com/quarkiverse/quarkus-jgit)
- [JSch](https://github.com/quarkiverse/quarkus-jsch)
- [Logging Sentry](https://github.com/quarkiverse/quarkus-logging-sentry)
- [Neo4j](https://github.com/quarkiverse/quarkus-neo4j)
- [Reactive Messaging HTTP](https://github.com/quarkiverse/quarkus-reactive-messaging-http)
- [Tika](https://github.com/quarkiverse/quarkus-tika)
- [Vault](https://github.com/quarkiverse/quarkus-vault)

We tried our best to make sure the change would be seamless by putting together Maven relocations. So your applications should still work and you will see a warning asking you to move to the new extensions. If you have any issue with the migration, please report it and we will have a look.

You can find additional information on how to migrate to the new extensions in [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.6).

If you are using these extensions, you are very welcome to contribute to them, now that the contribution is easier and doesn't require building the whole Quarkus!

We keep track of their compatibility with the latest versions of Quarkus thanks to the Ecosystem CI and [status.quarkus.io](https://status.quarkus.io/#quarkiverse).

> [!NOTE]
> The Quarkiverse Hub is a home for community-maintained extensions. Among other things, it provides a nice infrastructure for building and releasing extensions and also for hosting documentation.
>
> If you want to share Open Source Quarkus extensions with others, it is a nice home for them. If you want to contribute to Quarkus, contributing to extensions hosted on the Quarkiverse Hub for which you have an interest is a good way to start.

### SmallRye Reactive Messaging 3.13

SmallRye Reactive Messaging was upgraded to 3.13 and the Kafka client has been upgraded to 3.0.0.

### Programmatic API for caching

It took a while to get it in but, together with the annotation support for caching we had since a long time, there is now a programmatic API for caching coming with the Cache extension.

You can learn more about it in [the dedicated section of the guide](http://quarkus.io/guides/cache#programmatic-api).

### Smaller image for native executables

Our Dockerfile for native executables is now using `quay.io/quarkus/quarkus-micro-image:1.0` as the base image. This image is based on [UBI Micro](https://www.redhat.com/en/blog/introduction-ubi-micro) and reduces the size of the native image.

### UPX compression

Quarkus 2.6 adds support for built-in UPX compression of native executables. While it has some advantages, be aware it comes at a cost. Clément explained it at length in [his dedicated blog post](https://quarkus.io/blog/upx/).

### AWT extension

We improved a lot on the AWT support in native executables and you now have a dedicated extension for that `quarkus-awt`. Add it to your application if you are doing AWT work in native.

### Java 17 in generated projects

Until now, we generated all projects with Java 11 as the compilation source/target. We now define the compilation source/target as Java 17 for projects generated using Java 17.

### Kotlin 1.6

We upgraded Kotlin to 1.6.

### Camel 3.14

Camel 3.14 is the new LTS version of Camel. Compared to the previous LTS version (3.11), a lot of work has been done to bring new components, CamelJbang and Java 17 support. Improvements have been made to Core performance, Route configuration, and many components such as Kamelets and Kafka. For more details, check [Apache Camel 3.14 What’s New](https://camel.apache.org/blog/2021/12/camel314-whatsnew/).

#### Camel JFR Native support

Camel JFR extension allows diagnosing Camel applications with Java Flight Recorder. The extension has been introduced in Camel Quarkus 1.7.0 with JVM support only. The extension is now fully supported in JVM mode and Native mode.

## Contributors

The Quarkus community is growing and has now [577 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 2.6 release, thanks to Alexey Loubyansky, Andy Damevin, Bill Burke, Brian Wyka, Carles Arnal, Chris Cranford, Clara Fang, Clement Escoffier, Corentin Arnaud, Cristiano Nicolai, David Andlinger, Davide, Domagoj Tršan, Eric Deandrea, Erin Schnabel, Falko Modler, Florian Heubeck, Foivos Zakkak, Freeman Fang, Fu Cheng, Galder Zamarreño, Geoffrey GREBERT, George Gastaldi, Georgios Andrianakis, Gopal, Guillaume Le Floch, Guillaume Smet, Gwenneg Lepage, Ioannis Canellos, Jacopo Rota, James Netherton, Jan Martiška, Jorge Solórzano, Jose, Julien Ponge, Justin Lee, Kevin Wooten, Knut Wannheden, Ladislav Thon, Loïc Mathieu, Maciej Swiderski, Marc Nuri, Marcin Czeczko, Martin Kouba, Martin Panzer, Matej Novotny, Max Rydahl Andersen, Michael Anstis, Michael Simons, Michal Karm Babacek, Michal Maléř, Michał Szynkiewicz, NetoDevel, Ozan Gunalp, Pedro Igor, Phillip Krüger, Ramy, Ricardo Rodrigues, Richard Gomez, Robbie Gemmell, Roberto Cortez, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Steve Hawkins, Stuart Douglas, Stéphane Épardaud, Thomas Haines, Tomas Hofman, Victor Gallet, Victor Hugo de Oliveira Molinar, Vincent Sevel, Vincent van Dam, Wippermueller, Frank, xstefank, Yoann Rodière, Yoshikazu Nojima, Zaheed Beita, Шумов Игорь Юрьевич.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
