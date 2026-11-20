# Quarkus 1.1.0.Final released - Template engine, YAML configuration, and more

Just in time for the holiday season, Quarkus 1.1.0.Final is here with all its extensions (Camel Quarkus included) ready for you.

The inception of this version had a few twists and turns but that makes for a good story. It is packed with new features most notably the Qute template engine, support for YAML configuration, Gradle improvements, new logging extensions, a Quartz extension...

> [!WARNING]
> As explained in [this blog post](https://quarkus.io/blog/why-graalvm-19-2/), we reverted the GraalVM 19.3 upgrade in 1.1.0.Final. The version of GraalVM we support to build native executables is **19.2.1**.
>
> We are actively working with the GraalVM team to fix the issues blocking our 19.3 upgrade.

## What's new?

### Qute template engine

This is something we had in mind from the very beginning of Quarkus: offer a template engine that will be tailored to our approach, do everything we can at build time.

With 1.1 comes the very first version of Qute, our build time template engine. It is currently marked as experimental, so play with it and give us some feedback on [our mailing list](https://groups.google.com/forum/#!forum/quarkus-dev) or [our issue tracker](https://github.com/quarkusio/quarkus/issues)!

We have prepared [an introduction guide](https://quarkus.io/guides/qute) to get you started and [a more comprehensive reference guide](https://quarkus.io/guides/qute-reference).

### Configuration framework changes

The updated configuration framework fixes a number of long-standing configuration issues, especially when dealing with environment variables.

1.1 also comes with YAML support for the Quarkus configuration: add the `config-yaml` extension and Quarkus will consume your `application.yaml` file. More information in [the Configuration guide](https://quarkus.io/guides/config#yaml).

### Quarkus extension for Spring Security API

We continue our journey towards providing Spring APIs support and 1.1 adds Spring Security API to the mix. More information in the [Quarkus extension for Spring Security API guide](https://quarkus.io/guides/spring-security).

### Gradle changes

If you are using Quarkus with Gradle, be prepared for some big changes: we have upgraded to Gradle 6 and you need to change how you declare the Gradle plugin.

More information in [our Migration Guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.1#gradle-plugin).

Also we no longer trigger the build of a native executable by default. You need an explicit `./gradlew buildNative` to build a native executable.

### Logging improvements

1.1 comes with several major improvements to our logging layer:

\* we now support the Graylog Extended Log Format (GELF) via the `logging-gelf` extension: you can push your logs to an ELK or EFK stack. Go explore this new possibility in the [Centralized log management guide](https://quarkus.io/guides/centralized-log-management). \* we added a `logging-json` extension so that you can format your log as JSON. More info in [our Logging documentation](https://quarkus.io/guides/logging#json-logging). \* a `logging-sentry` extension was added to allow for application exception monitoring. Note that, [as stated in our documentation](https://quarkus.io/guides/logging-sentry), Sentry is not open source anymore.

### Health Checks

We are continuously improving the observability of Quarkus and one important element is having default health checks and metrics. Future versions of Quarkus will bundle them for all the extensions for which it makes sense.

In 1.1, health checks were added for Kafka, Kafka Streams, MongoDB, Neo4j and Artemis in addition to the preexisting Agroal datasource check.

### Kogito 0.6

Kogito has been upgraded to 0.6. It comes with features to handle time based activities that can be externalized to dedicated service (jobs service). It fully utilizes Quarkus and reactive non-blocking API to effectively deal with scheduled jobs (using Vert.x timers). In addition, GraphQL interface of data index service has been enhanced to allow typed searches over domain information for maximum flexibility to deal with data produced by Kogito runtime services (processes).

If you want to learn more about the jobs and data index services, visit [the Kogito wiki](https://github.com/kiegroup/kogito-runtimes/wiki).

### Quartz extension

From day one, Quarkus had a `scheduler` extension but it didn't expose the underlying Quartz specifics which prevented us from supporting clustered jobs.

As it was a popular requirement, in 1.1, a `quartz` extension has been added to provide support for clustered jobs and it has [its own guide and quickstart](https://quarkus.io/guides/quartz).

### ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

### And more...

\* Keycloak 8 is now supported. \* `Optional` support for Hibernate ORM with Panache and MongoDB with Panache. Lock support for Hibernate ORM with Panache. \* Hibernate Search + Elasticsearch (Preview): we have upgraded to Beta3. There are a few incompatible changes, have a look at [our Migration Guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.1#hibernate-search--elasticsearch-preview). \* Neo4j (Preview): the upgrade to the final version of 4.0 comes with [a few breaking changes](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.1#neo4j-preview). \* You can now generate Knative manifests with our Kubernetes extension. \* Our Amazon Lambda archetype now provide SAM templates. \* `quarkus.flyway.clean-at-start` configuration knob has been added to the Flyway extension.

### Full changelog

You can get the full changelogs of [1.1.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.1.0.CR1) and [1.1.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.1.0.Final) on GitHub.

## Contributors

Quarkus has now [206 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Yes, we are past 200 contributors and we are looking forward to counting you as a contributor soon. Many many thanks to each and everyone of them.

In particular for the 1.1.0 release, thanks to Alessio Soldano, Alex Kochnev, Alex Soto, Alexander Zimmermann, Alexey Loubyansky, Anamarija Talijanac, Andrew Guibert, Andy Damevin, Aurea Munoz, Claus Ibsen, Clement Escoffier, Cristiano Nicolai, CSTDev, Danilo Magalhães, David M. Lloyd, David Walluck, Dennis Baerten, Elegie, Emmanuel Bernard, Eric Wittmann, Erik Jan de Wit, Erin Schnabel, Fabian Martinez, Fabio Heer, Filippe Spolti, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Gunnar Morling, Gwenneg Lepage, Gytis Trikleris, Hantsy Bai, Hebert Coelho, Ioannis Canellos, Irena Kezic, Jacob Middag, Jaikiran Pai, Jan Martiska, Johannes, John O'Hara, John Oliver, Jonas Eriksson, Jonas Scholz, Jorge Solórzano, Jose Quaresma, Justin Holmes, Justin Lee, Ken Finnigan, Kevin Viet, Logan Hauspie, Loïc Mathieu, Luca Masini, Maciej Swiderski, Manyanda Chitimbo, Marcin Czeczko, Martin Kouba, Martin Stefanko, Matej Novotny, Max Rydahl Andersen, Michael Musgrove, Michael Simons, Michal Karm Babacek, Michał Szynkiewicz, Moncef Aoudia, Ondra Chaloupka, Paul Robinson, Pedro Igor, Rohan Maity, Rostislav Svoboda, Sanne Grinovero, Sebastien Blanc, Sergey Beryozkin, Simon Bengtsson, soberich, Stuart Douglas, Stéphane Épardaud, Tako Schotanus, Tiago Dolphine, Timothy Power, Vincent Sevel, Vinicius Ferraz, William Burns and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
