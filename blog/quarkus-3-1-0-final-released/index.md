# Quarkus 3.1.0.Final released - Programmatic creation of Reactive REST Clients, Kotlin 1.8.21 and more

It has been a month since we released Quarkus 3.0 and it is our pleasure to announce Quarkus 3.1.0.Final.

As usual, it comes with a lot of improvements all over the place.

Major changes are:

\* Provide new API to programmatically create Reactive REST Clients \* Introduce a way to set headers and status code for streaming responses \* Introduce a Hibernate Reactive variant of the Security Jakarta Persistence extension \* Verify OIDC ID token audience by default \* Bump Kotlin to 1.8.21 \* Upgrade Oracle JDBC driver to 23.2.0.0

## Migration Guide

To migrate from 3.0, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1).

If you are not already using 3.0, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details.

## What's new?

### Programmatic API to create Reactive REST Clients

Until now, you could only create Reactive REST Clients by configuring them in the `application.properties`, which was enough in most cases but could be problematic if you wanted to create dynamic clients.

Starting with 3.1, you can create Reactive REST Clients programmatically via a new API.

Please refer to the [documentation](https://quarkus.io/guides/rest-client-reactive#programmatic-client-creation-with-quarkusrestclientbuilder) for more details.

### More flexibility for streaming responses

RESTEasy Reactive offers more flexiblity for streaming responses: you can now [customize the response headers and status code](https://quarkus.io/guides/resteasy-reactive#customizing-headers-and-status).

### Reactive variant of Security Jakarta Persistence extension

The Security Jakarta Persistence extension, which allows to store users in a database using Jakarta Persistence, has now a reactive variant based on Hibernate Reactive: `quarkus-security-jpa-reactive`.

### Verify OIDC ID token audience by default

Starting with Quarkus 3.1, the OIDC ID token audience is now verified by default.

You can use the `quarkus.oidc.token.audience` configuration property to override the default check.

### Kotlin 1.8.21

We updated Kotlin to 1.8.21.

### Oracle JDBC driver update

The Oracle JDBC driver has been updated to 23.2.0.0.

## Full changelog

You can get the full changelog of [3.1.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.1.0.CR1) and [3.1.0.Final](https://github.com/quarkusio/quarkus/releases/tag/3.1.0.Final) on GitHub.

## Contributors

The Quarkus community is growing and has now [805 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.1 release, thanks to Ales Justin, Alex Martel, Alexey Loubyansky, Andres Almiray, Bill Burke, Bruno Baptista, Bruno Oliveira da Silva, Carles Arnal, Chris Pitman, Clement Escoffier, David Salter, Eric Deandrea, Erin Schnabel, Falko Modler, Felipe Henrique Gross Windmoller, Foivos Zakkak, Fouad Almalki, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Hitesh C, Holly Cummins, humberto, Ioannis Canellos, Jan Martiska, Jose Carvajal, Julien Ponge, Katia Aresti, kdnakt, Kevin Dubois, Konstantin Gribov, Ladislav Thon, Laure Souche, Manyanda Chitimbo, Marc Nuri, Marco Collovati, Marco Zanghì, Marek Skacelik, Marko Bekhta, Martin Kouba, Matej Novotny, Mathias Holzer, Max Rydahl Andersen, Maximilian Zellhofer, Mazen Khalil, Melloware, Michael Edgar, Michael Hamburger, Michael Musgrove, Michal Karm Babacek, Michal Maléř, Michal Vavřík, Michelle Purcell, Moritz Heine, Nelson Osacky, Ozan Gunalp, Pavel.Vervenko, Pavol Liška, pernelkanic, Phillip Krüger, polarctos, Radovan Synek, Robert Stupp, Robert Toyonaga, Roberto Cortez, Rolfe Dlugy-Hegwer, Romain Pelisse, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Severin Gehwolf, Stuart Douglas, Sébastien Crocquesel, xstefank, Yoann Rodière, and Yoshikazu Nojima.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
