# Quarkus 1.8 released - Multiple Persistence Units, Micrometer, jbang, GraalVM 20.2

We have been hard at work during Summer to bake Quarkus 1.8.

It comes with some shiny new features, among them:

- Multiple persistence units support for the Hibernate ORM extension
- A new Micrometer extension
- jbang integration for easy Quarkus-based scripting
- An update to GraalVM 20.2

As usual, it also comes with its fair share of bugfixes and usability and documentation improvements.

## Migration Guide

Eager to migrate your existing applications? Here is [our migration guide for 1.8](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.8).

GraalVM 20.2 in its Java 11 flavor is the recommended GraalVM version.

## What's new?

### Multiple Persistence Units

Until now, with standard Quarkus configuration, the Hibernate ORM extension only supported a single persistence unit. You could work around this limitation by using a `persistence.xml` but it was unpractical at best.

1.8 introduces support for multiple persistence units in the Hibernate ORM extension. The feature is still experimental, not because it's unstable, but because we might tweak how you configure it depending on your feedback. So if you use this feature, please provide your feedback on Zulip or on our `quarkus-dev` mailing list.

For more information about this new feature and how to use it in your applications, please refer to [the Hibernate ORM extension guide](/guides/hibernate-orm).

> [!NOTE]
> While standard Hibernate ORM entities can belong to several persistence units, Panache entities can only belong to one persistence unit.

### Micrometer extension

Micrometer is a widely used library to collect metrics from your application. One big advantage over our existing MicroProfile Metrics implementation is that a lot of dashboards are available out there for your usual monitoring tools.

Quarkus 1.8 introduces a new extension for Micrometer called `quarkus-micrometer`. Please refer to [its guide](/guides/micrometer) to learn more about it.

### Scripting with jbang

Quarkus 1.8 provides integration with [jbang](https://jbang.dev)\] which allows you to write Java scripts/applications requiring no Maven nor Gradle to get running.

Writing quick scripts using the Quarkus stack becomes easy. Learn more about it in [the Scripting with jbang guide](/guides/scripting).

### GraalVM 20.2

If you build native executables from your Quarkus applications, it is time to update to GraalVM 20.2, which is the recommended version from now on.

### SmallRye OpenAPI

SmallRye OpenAPI users often request the ability to save the generated API document.

The new `quarkus.smallrye-openapi.store-schema-directory` property allows you to do exactly that.

### Quartz

It is now possible to configure the Quartz by setting the `quarkus.quartz.instance-name` configuration property.

Starting with 1.8, you can use CDI injection in Quartz jobs.

The extension points (trigger listeners, job listeners, plugins) configuration have changed, so if you are using this feature, please refer to [the updated documentation](/guides/quartz#quartz-configuration-reference) to migrate your configuration.

### Kotlin support for MongoDB with Panache

Some of our extensions requires some additions to properly support Kotlin. This is the case of the MongoDB with Panache extension.

If you are using MongoDB with Panache and Kotlin, please add the `quarkus-mongodb-panache-kotlin` extension to your project.

### Full Changelog

You can get the full changelogs of [1.8.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.8.0.CR1) and [1.8.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.8.0.Final) on GitHub.

## ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

## Contributors

The Quarkus community is growing and has now [356 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 1.8 release, thanks to Aaron Coburn, Alex Soto, Alexey Loubyansky, Andreas Eberle, Andrej Petras, Andy Damevin, Antonio Goncalves, antonwiens, Bill Burke, Bobby Alex Philip, Charlie Powicki, Clement Escoffier, Daniel Platz, Dmitry Shohov, Enercido Alcantara, Erin Schnabel, Fabian Stäber, Falko Modler, Foivos Zakkak, Galder Zamarreño, Garima Monga, Gautier de Saint Martin Lacaze, George Gastaldi, Georgios Andrianakis, ghokun, Guillaume Le Floch, Guillaume Smet, Gunnar Morling, Gwenneg Lepage, Gytis Trikleris, Jaikiran Pai, Jan Martiška, Juri Berlanda, Justin Lee, Katia Aresti, kdnakt, Ken Finnigan, ketoketo, Kevin Viet, Knut Wannheden, Ladislav Thon, Laurent Broudoux, Luca Burgazzoli, Leandro, Loïc Hermann, Loïc Mathieu, Manyanda Chitimbo, MarceloArraes, Martin Kouba, Martin Schoene, Matej Novotny, Matthias Harter, Max Rydahl Andersen, Michael Simons, Michał Górniewski, Michał Szynkiewicz, Moncef, Moritz Becker, noelo, Pasquale Congiusti, Paul Carter-Brown, Phillip Kruger, Radim Vansa, Rafael T. C. Soares (A.K.A Tuelho), Raffael Hertle, Ramiz Ismailov, Roberto Cortez, Rohan Maity, Romain Quinio, Rostislav Svoboda, Sanne Grinovero, Sebastian Daschner, Sergey Beryozkin, Steffen Vinther Sørensen, Stuart Douglas, Stéphane Épardaud, Thomas Segismont, TNM Technologies, tomekl007, Yashar Imanlou and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
