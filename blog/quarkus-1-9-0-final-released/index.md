# Quarkus 1.9 released - Micrometer extension maturing, multiple Redis clients, Quarkiverse

Quarkus 1.9 comes with a lot of improvements on top of our existing feature set:

- The Micrometer extension is maturing
- Kafka now has metrics
- Multiple Redis clients are supported (as well as Sentinel connections)
- Bean Validation is supported by Reactive Routes
- SmallRye Reactive Messaging upgraded to 2.4.0 and Mutiny to 0.9.0

As usual, it also comes with its fair share of bugfixes and usability and documentation improvements.

Last but not least, we also started [the Quarkiverse initiative](#quarkiverse).

## Migration Guide

Eager to migrate your existing applications? Here is [our migration guide for 1.9](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.9).

The biggest change is that the old datasource (JDBC and reactive) configuration that was deprecated in 1.3 is now gone. So if your datasources don't work anymore, that's probably because you haven't migrated to the new configuration yet.

GraalVM 20.2 in its Java 11 flavor is the recommended GraalVM version.

## What's new?

### Micrometer extension maturing

1.8 introduced a new Micrometer extension. During the 1.9 development cycle, it matured a lot and is now the recommended way to collect metrics in Quarkus.

For instance, you now get metrics for Kafka.

### Bean Validation and Reactive Routes

Bean Validation constraints are now taken into account for parameters and return values (`Uni` is supported but not `Multi`) of Reactive Routes methods.

### More Reactive Changes

SmallRye Reactive Messaging was upgraded to 2.4.0 which includes several improvements - including Cloud Event support for Kafka - and bugfixes and Mutiny to 0.9.0.

The [pull request description](https://github.com/quarkusio/quarkus/pull/12470) lists the main changes.

It is now easier than ever to use Mutiny with the REST Client thanks to the `quarkus-rest-client-mutiny` extension.

### @TestTransaction annotation

This annotation is our response to a recurring request from the community: having the ability to rollback changes made in a test.

Instead of annotating your test with `@Transactional` (which will persist the changes), annotate your test with `@TestTransaction` and the changes will be automatically rolled back at the end of the test.

### Multiple Redis Clients

Until now, you could only connect to one Redis instance.

Starting with 1.9, you can connect to multiple instances. Also Sentinel is now supported.

### Other Changes

\* We now have a separate JAXP extension. Until now, it was intertwined with the JAXB extension. \* Hibernate ORM has been upgraded to 5.4.22.Final and Hibernate Reactive to 1.0.0.Alpha10

### Full Changelog

You can get the full changelogs of [1.9.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.9.0.CR1) and [1.9.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.9.0.Final) on GitHub.

<a id="quarkiverse"></a>
## Quarkiverse

Quarkus wouldn't be as successful if it weren't for its extension ecosystem. In the beginning, we were adding all extensions to the core Quarkus repository, but eventually it became too much of an overhead or just not always sensible to include an extension into the core. Some have since then created their own repositories external to Quarkus, but others just never happened because not everyone wants the overhead of maintaining a build setup while in other cases we actually had an idea for an extension but not solidified enough yet to be included in core (and consequently failed to find other community members interested and eager to help).

Meet Quarkiverse!

Quarkiverse is a separate GitHub organization (<https://github.com/quarkiverse>) that offers:

- Automated and secured publishing of your Maven releases to Maven Central
- Automated cross-testing of your extension with Quarkus builds/releases (see <https://github.com/quarkusio/quarkus-ecosystem-ci>)
- Inclusion in registry used by Quarkus tooling to browse extensions (i.e. code.quarkus.io, command line and IDE tools).
- Consistent formatting and release of projects
- Bigger Community outreach and a great place to collaborate

If you want to lead the development of an extension, see if it wasn't already [proposed on GitHub](https://github.com/quarkusio/quarkus/issues?q=is%3Aopen+is%3Aissue+label%3Akind%2Fextension-proposal) or [create an Extension Proposal issue](https://github.com/quarkusio/quarkus/issues/new/choose).

A blog post is in the works and should give more details. In the meantime, check out [the extensions that are already there](https://github.com/search?q=topic%3Aquarkus-extension+org%3Aquarkiverse&type=Repositories).

## ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

## Contributors

The Quarkus community is growing and has now [378 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 1.9 release, thanks to Abel Salgado Romero, Adam Bien, Alex Soto, Alexey Loubyansky, AlexeyAnufriev, Andrea Cosentino, Andy Damevin, Anh Le (Andy), Armel Soro, Arne Mejlholm, asodja, Auri Munoz, Bill Burke, Bobby Philip, Centro de Computos, Chris Laprun, Clement Escoffier, Daniel Badawi, Daniel Platz, David M. Lloyd, Dillon Fearns, Dmitry Shohov, Driss Amri, Dušan Križan, Emanuel Alves, Enercido Alcantara, Erin Schnabel, Fabian Stäber, Fabio Iwakoshi, Falko Modler, Foivos Zakkak, Galder Zamarreño, Garima Monga, Gautier de Saint Martin Lacaze, Geoffrey De Smet, George Gastaldi, Georgios Andrianakis, ghokun, Guillaume Le Floch, Guillaume Smet, Gunnar Morling, Gytis Trikleris, Harsh Madhani, Harsha R, Ioannis Canellos, Jack Hu, Jaikiran Pai, Jakub Scholz, Jan Martiška, João Lucas Scharf, Julien Ponge, Justin Lee, kdnakt, Ken Finnigan, Kevin Viet, Knut Wannheden, Ladislav Thon, Laurent Broudoux, lburgazzoli, Loïc Mathieu, Maciej Swiderski, Manyanda Chitimbo, Marcin Czeczko, Markus Heberling, Martin Kouba, Matej Vasek, Max Rydahl Andersen, Michal Krzywanski Michał Górniewski, Michał Szynkiewicz, Miguel Serra, Nathan Erwin, Oliver Libutzki, Paul Kunysch, Pedro Igor, Peter Palaga, Phillip Krüger, Roberto Cortez, Rohan Maity, Ron Sigal, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Simon Bengtsson, Stephane Epardaud, Stuart Douglas, Thomas Pfaff, Vincent Sevel, Vincent Sourin, Vinícius Ferraz Campos Florentino, William Burns, xstefank, Xumk and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
