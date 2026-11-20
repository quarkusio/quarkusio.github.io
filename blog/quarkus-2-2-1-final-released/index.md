# Quarkus 2.2.1.Final released - Hardening release

Today we announce the availability of Quarkus 2.2.1.Final, which is the result of our first hardening cycle.

Indeed, for 2.2, we decided to slow down on adding new features and focus this release cycle on hardening Quarkus with 3 main focuses:

- Fix issues
- Improve usability
- Improve documentation

Thus the list of noteworthy new features is a bit short for this release:

- Upgrade to GraalVM 21.2
- Add global flag to disable Dev Services
- Change the default thread model for RESTEasy Reactive
- Introduce support for MongoDB service binding
- Extension for running Narayana LRA participants

but overall, this release comes with a lot of nice things so we strongly recommend you to upgrade.

## Where is 2.2.0.Final?

We only released the Core artifacts of 2.2.0.Final and not the full Platform release because this version suffered from a bug preventing dev mode to work on Windows (that's what you get for trying to fix another bug...). Thus why we announce 2.2.1.Final directly.

## Migration Guide

To migrate from 2.1, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.2).

## What's new?

### GraalVM 21.2

The recommended version of GraalVM to use with Quarkus 2.2 is 21.2.

### Global flag to disable Dev Services

It is now possible to disable all the Dev Services at once with `quarkus.devservices.enabled=false`.

Also we centralized all the information about Dev Services in [one place](/guides/dev-services).

### Enhanced thread model for RESTEasy Reactive

In short, RESTEasy Reactive is now smart enough to choose a blocking or non blocking thread model depending on the prototype of your REST methods, which should makes things easier when you are coming from RESTEasy Classic.

Clément Escoffier wrote a very comprehensive blog post about this enhancement that is very much worth reading: [RESTEasy Reactive - To block or not to block](https://quarkus.io/blog/resteasy-reactive-smart-dispatch/).

### MongoDB service binding

Service bindings got a new component supported: MongoDB.

### Narayana LRA extension

LRA stands for Long Running Actions and the point of it is to provide APIs for services to coordinate activities.

You can learn more about it in [the dedicated blog post](/blog/using-lra).

### Full Changelog

You can get the full changelogs of [2.2.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/2.2.0.CR1), [2.2.0.Final](https://github.com/quarkusio/quarkus/releases/tag/2.2.0.Final) and [2.2.1.Final](https://github.com/quarkusio/quarkus/releases/tag/2.2.1.Final) on GitHub.

## Contributors

The Quarkus community is growing and has now [533 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 2.2 release, thanks to Alexey Loubyansky, Andreas Eberle, Andrej Vano, Andy Damevin, Anze Sodja, asamal, Aykut Bulgu, barreiro, bdevreugd-vialis, Bill Burke, Christoph Kappel, Clement Escoffier, Cyrille Le Clerc, Davide, Dirk Van Haerenborgh, Eduard Tudenhoefner, Erin Schnabel, Falko Modler, Felipe Carvalho dos Anjos Formentin, Foivos Zakkak, Fouad Almalki, Gabriele Cardosi, Galder Zamarreño, Geoffrey De Smet, Georg Leber, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gustavo Luszczynski, hbelmiro, insectengine, Ioannis Canellos, Jaikiran Pai, Jan Martiška, John O'Hara, John Oliver, Jose, Julien Ponge, Katia Aresti, Ken Finnigan, Kjetil Nygård, Kyrylo Shpak, Ladislav Thon, Loïc Mathieu, Luca Molteni, Manyanda Chitimbo, Marc Nuri, Mark McLaughlin, Martin Kouba, Martin Muzikar, Martin Panzer, Matej Novotny, Matthias Cullmann, Michael Musgrove, Michał Szynkiewicz, Ozan Gunalp, Paul Robinson, Phillip Krüger, Raul Valdoleiros, Robert Stupp, Roberto Cortez, Ronald Dehuysser, root, Rostislav Svoboda, Ruggero D'Alò, Sandip Gahlot, Sanne Grinovero, Sergey Beryozkin, Stuart Douglas, Stéphane Épardaud, tarilabs, Tomas Hofman, Willem Jan Glerum, Xavier, Yoann Rodièreand Yubao Liu.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
