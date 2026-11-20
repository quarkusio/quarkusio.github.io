# Quarkus 2.4.0.Final released - Hibernate Reactive 1.0.0, Kafka Streams DevUI, Multi module continuous testing, AWT image resize via new AWT extension and much more

Today, we release Quarkus 2.4.0.Final which includes a lot of refinements and improvements and some new features:

- Hibernate Reactive 1.0.0.Final
- Introducing Kafka Streams DevUI
- Support continuous testing for multi module projects
- Support AWT image resize via new AWT extension

## Migration Guide

To migrate from 2.3, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.4).

## What's new?

### Hibernate Reactive 1.0.0.Final

Among other fixes and improvements, in this release all methods which "open a session" have been made reactive.

Please, refer to the [getting started documentation](https://hibernate.org/reactive/documentation/1.0/reference/html_single/#getting-started) to find out more about the release.

### Kafka Streams DevUI

The Quarkus DevUI now includes support for the topology visualizer, which looks like [this example](https://user-images.githubusercontent.com/593564/136768958-9a66c25b-c3fa-4a6c-9211-8cedd845eb55.png).

### Continuous testing for multi module projects

Continuous testing will now run tests from all the project modules, with the option to configure the old behaviour. The new config options include:

- quarkus.test.only-test-application-module (default value is false) - if false then tests from all dependency modules will be run as well;
- quarkus.test.include-module-pattern - a regular expression matched against the module groupId:artifactId, tests from the matched modules will be included in the continuous testing;
- quarkus.test.exclude-module-pattern (ignored if `include-module-pattern` has been set) - a regular expression and is matched against the module groupId:artifactId, tests from the matched modules will be excluded from the continuous testing.

### AWT image resize via new AWT extension

A new `io.quarkus:quarkus-awt` extension for AWT and Java2D usage was added to enable various AWT use-cases, such as image resizing.

## Contributors

The Quarkus community is growing and has now [549 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 2.4 release, thanks to A. N. M. Bazlur Rahman (Rokon), Alexey Loubyansky, Andy Damevin, Arne Mejlholm, Bill Burke, Chris Laprun, Clement Escoffier, Daniel Petisme, Davide, Dheeraj Nalluri, Dmitrii K, Eduard Tudenhoefner, Erin Schnabel, Falko Modler, Foivos Zakkak, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gwenneg Lepage, hbelmiro, Jaikiran, Jaikiran Pai, Jakub Scholz, Jan Martiška, Javier Toja, Jose, Julien Ponge, kdnakt, Knut Wannheden, Ladislav Thon, Loïc Mathieu, Manyanda Chitimbo, Marc Nuri, Martin Kouba, Michael Schnitzler, Michael Simons, Michał Szynkiewicz, Nathan Erwin, NetoDevel, Nicolas Filotto, Ozan Gunalp, Phillip Kruger, Phillip Krüger, Robbie Gemmell, Roberto Cortez, Rostislav Svoboda, Sanne Grinovero, Sauli Anto, sberyozkin, seregamorph, Sergey Beryozkin, Stuart Douglas, Tomas Hofman, Vincent Sevel, Wayne Ellis, xstefank, Yannick Reifschneider, Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
