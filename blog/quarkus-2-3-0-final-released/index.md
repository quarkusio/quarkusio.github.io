# Quarkus 2.3.0.Final released - Dev Service for Neo4J, Logging with Panache, Testing CLI applications and much more

Today, we release Quarkus 2.3.0.Final which includes a lot of refinements and improvements and some new features:

- Dev Service for Neo4J
- Logging with Panache
- Testing support for CLI applications
- MongoDB Liquibase extension
- Support for Hibernate ORM interceptors

## Migration Guide

To migrate from 2.2, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.3).

## What's new?

### Dev Service for Neo4J

Dev Services start the required containers automatically when you are developing or testing your application.

A lot of Quarkus extensions are already covered and, in 2.3, we added [a Dev Service for Neo4J](/guides/neo4j).

### Logging with Panache

Logging in Quarkus now has a Panache flavor which allows you to eliminate logger related boilerplate code. See the [logging guide](/guides/logging) for details.

### Testing support for CLI applications

Quarkus now provides `@QuarkusMainTest` and `@QuarkusMainIntegrationTest` to facilitate testing of CLI applications. Check out the [command mode guide](/guides/command-mode-reference) for details.

### MongoDB Liquibase extension

The new `quarkus-liquibase-mongodb` introduces support for Liquibase migrations to Quarkus applications that use MongoDB.

### Support for Hibernate Interceptors

[Hibernate Interceptors](https://docs.jboss.org/hibernate/orm/5.6/userguide/html_single/Hibernate_User_Guide.html#events) are now supported in Quarkus. The [Hibernate guide](/guides/hibernate-orm) provides all the necessary details.

## Contributors

The Quarkus community is growing and has now [542 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 2.3 release, thanks to Alexey Loubyansky, Andrej Vano, Andy Damevin, asamal, barreiro, Bill Burke, Chris Laprun, Clement Escoffier, Daniele Zonca, Dmitri Bourlatchkov, Eduard Tudenhoefner, Eric Deandrea, Eric Wittmann, Erin Schnabel, Evan Shortiss, Falko Modler, Fedor Dudinskiy, Felipe Carvalho dos Anjos Formentin, Foivos Zakkak, Galder Zamarreño, Geoffrey GREBERT, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gwenneg Lepage, Hugo Guerrero, insectengine, Ioannis Canellos, Jaikiran Pai, Jan Martiška, Jan Martiska, Jose, Josef Smrcka, Josh V, Julien Dubois, Keita Fujii, Ken Finnigan, Kevin Wooten, Knut Wannheden, Ladislav Thon, Loïc Mathieu, luneo7, Mahaswami Software, Manaswini Das, Manyanda Chitimbo, Marc Nuri, Martin Kouba, Martin Muzikar, Matej Novotny, Matthias Cullmann, Michael Edgar, Michael Simons, Michal Karm Babacek, Michał Szynkiewicz, olivier dufour, Ozan Gunalp, Paul Robinson, Phillip Krüger, Phillip Kruger, Robbie Gemmell, Roberto Cortez, Robert Stupp, Ruggero D'Alò, Sandip Gahlot, Sanne Grinovero, Sauli Anto, sberyozkin, Sergey Beryozkin, Stephane Epardaud, Stephen Nimmo, Stéphane Épardaud, Stuart Douglas, Thomas Segismont, Vincent Sevel, Vincent van Dam, Wayne Ellis, wojciech.stryjewski, Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
