# Quarkus 1.7 released - Elasticsearch and Redis clients, Reactive routes and Funqy improvements

As usual, this new Quarkus version comes with some exciting new features but we put the focus this month on fixing issues and improving things all over the place, documentation included.

Quarkus 1.7, that's more than 300 pull requests merged.

The most prominent new features are:

\* New extensions for the low level and high level Elasticsearch REST clients \* An extension for the Vert.x Redis client \* An Hibernate Envers extension \* Support for the JDBC Db2 driver \* A lot of improvements to the Reactive routes feature \* The Funqy serverless framework got some interesting new features

We also reworked [our Guides index page](/guides/) recently with a new design and a search engine.

## Migration Guide

Eager to migrate your existing applications? Here is [our migration guide for 1.7](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.7).

One important point is that we don't support GraalVM 19.3 anymore: GraalVM 20.1 in its Java 11 flavor is the recommended GraalVM version.

## What's new?

### Elasticsearch REST Clients

Quarkus has support for the low level Elasticsearch REST client for a while but it wasn't publicly exposed. It was just used under the hoods by the Hibernate Search + Elasticsearch extension.

In 1.7, both the low level and high level Elasticsearch REST clients are exposed via extensions with proper Quarkus configuration. Thus you can connect to your Elasticsearch clusters directly.

More information about how to use these clients can be found in [the new Elasticsearch guide](/guides/elasticsearch).

### Vert.x Redis Client

Quarkus 1.7 introduces a new extension to connect to the Redis datastore. It is based on the Eclipse Vert.x Redis client.

Everything you need to know about it is in [the Redis guide](/guides/redis).

### Hibernate Envers

Hibernate Envers is an Hibernate ORM module bringing versioning/auditing features.

It used to work with Quarkus in JVM mode but not in native mode. The new Hibernate Envers extension fixes that.

You can find more information about it in [the Hibernate Envers section](/guides/hibernate-orm#envers) of the Hibernate ORM guide.

### Funqy Improvements

Funqy is our serverless framework.

Two new features were added to Funqy in 1.7:

\* The ability to inject query parameters in the Funqy HTTP bindings \* The support for [Google Cloud Platform](/guides/funqy-gcp-functions)

### Reactive Routes Improvements

While most Quarkus applications are developed with RESTEasy and JAX-RS, you can go with an alternative approach called [Reactive Routes](/guides/reactive-routes) where you declare and chain routes.

Reactive Routes got several improvements in 1.7:

\* The `@Param`, `@Header` and `@Body` annotations were added to inject elements from the request in the route handling method. \* Mutiny types are now fully supported. \* If the route handling method doesn't return `void`, you can now simplify things by not passing the `RoutingExchange` as a parameter.

### Db2 JDBC driver

In 1.6, we included support for the reactive Db2 driver. 1.7 brings a new extension for the Db2 JDBC driver.

### Security Testing Infrastructure

When testing, it might be handy to disable security entirely or use a particular identity.

Quarkus 1.7 introduces the `@TestSecurity` annotation which allows all that, as explained in [the Security Testing guide](/guides/security-testing#testing-security).

In passing the whole Security documentation got a massive reorganization to make it more approachable.

### Extension Registry

This feature paves the way for the future: we now support multiple extension registries, meaning you could host your own extension registry and get it integrated in the Quarkus tools.

We are still making progress on this front but expect to see some real use cases for it soon.

### S2I image

The default S2I image is now a Java 11 image based on UBI: `registry.access.redhat.com/ubi8/openjdk-11`.

### Full Changelog

You can get the full changelogs of [1.7.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.7.0.CR1) and [1.7.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.7.0.Final) on GitHub.

## ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

## Contributors

The Quarkus community is growing and has now [341 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 1.7 release, thanks to Aaron Coburn, Abel Salgado Romero, Adrian, Alex Soto, Alexander Schwartz, Alexey Loubyansky, Andrea Boriero, Andrea Cosentino, Andreas Eberle, Andrej Petras, Andrew Guibert, Andy Damevin, Arne Mejlholm, Aurea Munoz, Bill Burke, Boosey, Bret McGuire, Carlos Eduardo Panarello, Charlie Powicki, Clement Escoffier, Daniel Newport, Daniel Zwicker, David Horvat, Davide D'Alto, Dirk Van Haerenborgh, Dmitriy Chaban, Dominika Vagnerova, ekans, Emanuel Alves, Emmanuel Bernard, Erin Schnabel, Falko Modler, Foivos Zakkak, Francesco Bellentani, Galder Zamarreño, Garima Monga, Geoffrey De Smet, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gunnar Morling, Gwenneg Lepage, Gytis Trikleris, Harald Reinmueller, Heiko W. Rupp, Helber Belmiro, Hugh Lunnon, Ioannis Canellos, Ivan Greene, J Lannoy, Jaikiran Pai, Jan Martiška, Jason T. Greene, Jens Reimann, Joe White, Katia Aresti, kdnakt, Ken Finnigan, Ladislav Thon, Loïc Hermann, Loïc Mathieu, Machareder, Manyanda Chitimbo, Marcelo Rubim, Mario Fusco, Martin Kouba, Matthias Harter, Max Rydahl Andersen, Michael Simons, Michał Szynkiewicz, Miguel Serra, Moritz Becker, Nebrass Lamouchi, Paul Carter-Brown, Paul Robinson, Paulo Casaes, Pedro Igor, Peter Palaga, Phillip Krüger, Rafael T. C. Soares, Robbie Gemmell, Roberto Cortez, Rohan Maity, Romain Quinio, Rostislav Svoboda, Sanne Grinovero, Sebastian Daschner, Sergey Beryozkin, Stuart Douglas, Stéphane Épardaud, Thomas Segismont, tibordigana, Timothy Power, TNM Technologies, Vincent Sevel, Yoann Rodière, Zed Spencer-Milnes, Zhiyuan Zheng and Ивашин Алексей Витальевич.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
