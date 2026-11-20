# Quarkus 1.3.0.Final released - New class loader infrastructure, GraalVM 20 support and much much more

In case you haven't noticed, it has been a bit less than two months since our last major Quarkus release as we went through some extensive testing phases to validate our new class loader infrastructure.

A big thanks to all the people who tested our Alphas/CRs and provided feedback.

Two months? That's a lot in the Quarkus world so expect a whole lot of new things.

Enough introduction, let's see what 1.3 brings to the world.

## Migration Guide

Eager to migrate your existing applications? Here is [our migration guide for 1.3.0.Final](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.3).

## What's new?

### New class loader infrastructure

Due to the innovative nature of Quarkus (especially in our test infrastructure and dev mode), we had a few recurring class loading issues.

Quarkus 1.3 comes with a brand new class loading infrastructure, fixing the structural issues we had. The new class loading infrastructure is described at length in the [Class Loading Reference guide](https://quarkus.io/guides/class-loading-reference).

We ironed out this new infrastructure during the Alpha and CR phases so it is pretty solid now. If you encounter weird class loading issues, report them with a reproducer, we will include all those fixes in the upcoming 1.3.1.

### One Vert.x to rule them all

A significant amount of work went into having only one Vert.x instance to handle everything in Quarkus instead of two. The idea behind this work is to reduce the resource consumption, hopefully without changing anything for you.

### Banner

This is probably the most visible change: by default, an ASCII-art banner is now displayed when starting Quarkus.

You can of course:

\* customize it, \* disable it for a given project, \* disable it globally for all your projects.

Please refer to [the documentation](https://quarkus.io/guides/getting-started#banner) for more details.

### Mutiny arising

A new reactive programming API has been introduced. This API named [Mutiny](https://smallrye.io/smallrye-mutiny/) replaces the Axle and Reactive Streams Operators models (Reactive Streams and CompletionStage). The previous models are still functional, but deprecated and will be removed in the future.

If you would like to know more, please refer to the [Getting Started with Reactive guide](https://quarkus.io/guides/getting-started-reactive).

### Easy deployment to Kubernetes or OpenShift

The Kubernetes extension has been overhauled and now gives users the ability to deploy their Quarkus applications to Kubernetes or OpenShift with almost no effort. Essentially the extension now also takes care of generating a container image (see [Container images](#container-images) for more details) and applying the generated Kubernetes manifests to a target cluster, after the container image has been generated.

Please refer to [the documentation](https://quarkus.io/guides/kubernetes) for more details.

### Eclipse MicroProfile 3.3 specifications

The SmallRye extensions were updated to implement Eclipse MicroProfile 3.3.

A blog post with more details is coming soon.

### Default HTTP read timeout

We have added the ability to configure the HTTP read timeout (via `quarkus.http.read-timeout`) and it comes with a default value of 1 minute.

### New extensions

<a id="container-images"></a>
#### Container images

A massive effort has been put in facilitating the build of container images with Quarkus.

We now have a set of extensions building the images as part of the Quarkus build based on the Quarkus configuration, either using Jib, Docker or S2I.

More information in the [Container Image guide](https://quarkus.io/guides/container-image).

#### OptaPlanner

OptaPlanner is an AI constraint solver. It optimizes planning and scheduling problems.

The OptaPlanner Quarkus extension brings all the features of OptaPlanner to Quarkus, even in native executables. If you want to discover how OptaPlanner can help, please have a look at [Timefold](https://docs.timefold.ai/timefold-platform/latest/introduction) (OptaPlanner's successor).

#### Security JPA

Want your users in a database managed by Hibernate ORM? Just add a couple of annotations and [the Security JPA extension](https://quarkus.io/guides/security-jpa) gets you covered.

#### Spring Cloud Config Client

We extended our Spring compatibility layer with a Spring Cloud Config Client.

You can now [get your configuration from a Spring Cloud Config server](https://quarkus.io/guides/spring-cloud-config-client).

#### AWS X-Ray

Support for AWS X-Ray was added so you can use it with your AWS lambdas, in JVM mode and native mode.

#### Qpid JMS

We now recommend the usage of the Qpid JMS extension instead of the Artemis JMS one. Features are the same, it just adds more flexibility.

The Artemis JMS extension is still there.

More information in [the JMS guide](https://quarkus.io/guides/jms).

#### Liquibase

We had a Flyway extension for a while, you can now also use Liquibase to handle your database schema migrations.

More on this in [the Liquibase guide](https://quarkus.io/guides/liquibase).

#### Reactive MongoDB with Panache

Love reactive? Love MongoDB? Love simple code?

Here comes the Reactive MongoDB with Panache extension. More details in [the MongoDB with Panache documentation](https://quarkus.io/guides/mongodb-panache).

### GraalVM 20.0 support

1.3.0.Final supports four flavors of GraalVM:

\* GraalVM 19.3.1 - JDK 8 \* GraalVM 19.3.1 - JDK 11 \* GraalVM 20.0.0 - JDK 8 \* GraalVM 20.0.0 - JDK 11

> [!NOTE]
> To summarize, for Quarkus, you can use:
>
> \* JDK 8, 11, 12, 13 or 14 in JVM mode \* JDK 8 or 11 with GraalVM 19.3.1 or 20.0.0 to build native executables

> [!TIP]
> JDK 11 support in GraalVM is still in tech preview. On our side, we did not encounter any specific issues except for the compilation to be \~20% slower.

### Hibernate ORM

Automatic association management via bytecode enhancement is disabled from now on.

It is possible to disable the second level caching by setting the `quarkus.hibernate-orm.second-level-caching-enabled` configuration property to `false`.

### Spring compatibility layer

The Spring compatibility layer is now based on trimmed down artifacts only including the necessary APIs and annotations instead of the full Spring jars.

Less dependencies, less code and you only have the supported features around.

### And more...

\* Several improvements in our Gradle and Kotlin support \* The `<enableJni>` option of the native executable building has been deprecated and marked for removal as JNI is always enabled with GraalVM 19.3+ \* The Flyway extension now supports an `out-of-order` configuration property \* Our Qute templating engine is maturing and got its share of bugfixes and improvements \* We support Keycloak 9 \* Hibernate Search + Elasticsearch was upgraded to 6.0.0.Beta5 \* You now can inject the underlying Quartz scheduler of the Quartz extension for more flexibility

### Full changelog

You can get the full changelogs of [1.3.0.Alpha1](https://github.com/quarkusio/quarkus/releases/tag/1.3.0.Alpha1), [1.3.0.Alpha2](https://github.com/quarkusio/quarkus/releases/tag/1.3.0.Alpha2), [1.3.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.3.0.CR1), [1.3.0.CR2](https://github.com/quarkusio/quarkus/releases/tag/1.3.0.CR2) and [1.3.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.3.0.Final) on GitHub.

## ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

## Contributors

The Quarkus community is growing and has now [246 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 1.3.0 release, thanks to Akito Kidani, Alex Soto, Alexey Loubyansky, Alvaro Serrano, Andreas Eberle, Andrej Petras, Andrew Guibert, Andy Damevin, Aurea Munoz, Bill Burke, Clement Escoffier, cstdev, Darran Lofthouse, David M. Lloyd, Emmanuel Bernard, Erik Mattheis, Erin Schnabel, Felix Wong, Geoffrey De Smet, George Gastaldi, Georgios Andrianakis, Grzegorz Piwowarek, Guillaume Smet, Gunnar Morling, Gwenneg Lepage, Harald Reinmueller, Harold Sun, Ioannis Canellos, Irena Kezic, Jaikiran Pai, Jakubo96, Jan Martiska, Jason T. Greene, Jesper Skov, Joachim Segala, Johannes, John O'Hara, Jonas Scholz, Jordi Sola, Josejulio Martínez, Justin Lee, Kamesh Sampath, Katia Aresti, Kaxil Naik, Ken Finnigan, Kuzuha Shinoda, Ladislav Thon, Loïc Mathieu, Luca Burgazzoli, Luis F Munoz, Luram Archanjo, Maciej Swiderski, Manyanda Chitimbo, Marc Häbich, Martin Kouba, Martin Panzer, Matej Novotny, Max Rydahl Andersen, Michal Karm Babacek, Michał Szynkiewicz, Miroslav Stary, Nicolas Martignole, Olivier Dufour, Ondrej Machala, Paulo Casaes, Pavol Loffay, Pedro Igor, Peter Palaga, Phillip Kruger, Pierre PLR, Raffael Hertle, Ramakrishna Pattnaik, Rinaldo Pitzer Júnior, Robbie Gemmell, Roberto Cortez, Rohan Maity, Rostislav Svoboda, Sanne Grinovero, Sebastian Wastl, Sergey Beryozkin, Stefnotch, Stuart Douglas, Stéphane Épardaud, testfixer, Thomas Segismont, Timothy Power, Tobias Liese, Tomaz Cerar, Vincent Sevel, Vincent Sourin, Vinícius Ferraz Campos Florentino and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
