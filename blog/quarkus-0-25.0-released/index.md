# Quarkus 0.25.0 released - Security layer updated

Just a few days after 0.24.0, and to stay in line with our planning, we release 0.25.0.

This release fixes a couple of issues, mostly related to the new HTTP layer and brings some updates to the currently being rewritten security layer.

## What's new?

### Security layer changes

HTTP based permissions can now be defined in the `application.properties`.

This new feature is fully documented in the [Security guide](/guides/security#securing-web-endpoints).

### Windows and dev mode issue

We had a [long-standing issue](https://github.com/quarkusio/quarkus/issues/3592) with dev mode on Windows and, hopefully, this time, it is fixed.

Please give it another chance and report back if it still does not work.

### Hibernate Search 6.0.0.Beta1

We updated Hibernate Search to 6.0.0.Beta1.

A few changes has been made to the APIs and please consider getting rid of the potential calls to deprecated methods in your code because they are going away in the next Hibernate Search release.

Some configuration properties have also been adjusted:

\* `quarkus.hibernate-search.elasticsearch.automatic-indexing.synchronization-strategy` becomes `quarkus.hibernate-search.elasticsearch.automatic-indexing.synchronization.strategy` \* `quarkus.hibernate-search.elasticsearch.analysis-configurer` becomes `quarkus.hibernate-search.elasticsearch.analysis.configurer`

[The documentation](/guides/hibernate-search-elasticsearch) has been updated to reflect these changes.

### Debugging, suspend and dev mode

Dev mode doesn't suspend the execution by default anymore when you use `-Ddebug=true` or `-Ddebug=<port>`. You need to explicitly use `-Dsuspend=true`.

### Apache Derby driver

We now have an extension for the Apache Derby JDBC driver: it will work out of the box even with a native executable.

### REST Client multipart guide

We now have [a guide explaining how to do multipart with our REST client](/guides/rest-client-multipart). This allows you for instance to upload documents to a REST endpoint.

### Full changelog

We also fixed a few bugs and usability issues: get [the full changelog of 0.25.0 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/0.25.0).

## Contributors

Quarkus has now [163 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for this release, thanks to Alexey Loubyansky, Andrew Guibert, Aurea Munoz, Clement Escoffier, Daniel Petisme, Don Naro, Dusan Odalovic, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Gwenneg Lepage, Harald Reinmüller, Jaikiran Pai, Jan Martiska, John O'Hara, Ken Finnigan, Luca Burgazzoli, Maciej Swiderski, Manyanda Chitimbo, Martin Kouba, Matej Novotny, Matthias Andreas Benkard, Peter Palaga, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Simon Bengtsson, Stéphane Epardaud, Stuart Douglas, Timothy Power, William Burns and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
