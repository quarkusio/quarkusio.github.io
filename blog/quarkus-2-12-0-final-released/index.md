# Quarkus 2.12.0.Final released - GraalVM/Mandrel 22.2, Kotlin 1.7

Quarkus 2.12 is a relatively small release, with most development happening in August and all.

But it still comes with a lot of documentation improvements, fixes and enhancements, most notably:

- Upgrade to GraalVM/Mandrel 22.2
- Upgrade to Kotlin 1.7
- SmallRye Config SecretKeys support
- Microsoft SQL Server JDBC driver update

## Migration Guide

To migrate from 2.11, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.12).

## What's new?

### GraalVM/Mandrel 22.2

It was already possible to use it with Quarkus 2.11 but GraalVM/Mandrel 22.2 is now the default version for building native executables.

### Kotlin 1.7

Our Kotlin extensions got upgraded to Kotlin 1.7.

### SmallRye Config SecretKeys

Support for [SmallRye Config SecretKeys](https://smallrye.io/smallrye-config/Main/config/secret-keys/) has been added.

### Microsoft SQL Server JDBC driver update

We were using a very old version of the Microsoft SQL Server JDBC driver, due to some issues when compiling to native executables. We were finally able to upgrade to version 11.2.0.jre11 in Quarkus 2.12.0.Final.

## Full changelog

You can get the full changelog of [2.12.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/2.12.0.CR1) and [2.12.0.Final](https://github.com/quarkusio/quarkus/releases/tag/2.12.0.Final) on GitHub.

## Contributors

The Quarkus community is growing and has now [691 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 2.12 release, thanks to Alexander Schwartz, Alexey Loubyansky, Andy Damevin, Baiju Muthukadan, Bernhard Wolkerstorfer, Christopher Chianelli, Clement Escoffier, Craig Day, Davide D'Alto, Dheeraj Nalluri, DiogoCarleto, Erin Schnabel, Falko Modler, Florin Botis, Foivos Zakkak, Fouad Almalki, Galder Zamarreño, Gavin Ray, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gwenneg Lepage, Heiko W. Rupp, hmanwani-rh, Holly Cummins, Ignasi Camacho Redó, Ioannis Canellos, Ivan Senic, James Netherton, Jan Martiska, Jose, Julien Ponge, Justin Lee, Katia Aresti, Kevin Wooten, Knut Wannheden, Konstantin Gribov, Ladislav Thon, Loïc Mathieu, Luc Thuot, Lukas Lowinger, Manyanda Chitimbo, Marc Nuri, Marc Wrobel, Marcelo Pereira, Martin Kouba, Matej Novotny, Michael Edgar, Michael Musgrove, Michal Maléř, Michal Vavřík, Michał Szynkiewicz, Michelle Purcell, Mike Weber, mun711, Ola Sæter Heitmann, Ozan Gunalp, Pablo Gonzalez Granados, Pavol Liška, Phillip Krüger, Richard Gomez, Rinaldo Pitzer Júnior, Robert Stupp, Roberto Cortez, robp94, Rostislav Svoboda, Sanne Grinovero, Sebastian Schuster, Sergey Beryozkin, Severin Gehwolf, Stefan Sitani, Stuart Douglas, Theodor Mihalache, xstefank, Yoann Rodière and Yoshikazu Nojima.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
