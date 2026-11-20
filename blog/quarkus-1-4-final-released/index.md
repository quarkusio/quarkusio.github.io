# Quarkus 1.4 released - Command mode, HTTP 2, New FaaS framework, Mocking...

Here comes the time of the month when you get your new Quarkus release: 1.4.1.Final is here. No 1.4.0.Final, we had to skip it.

This version is packed with very nice features and includes tons of improvements and bugfixes:

\* Command mode \* HTTP/2 \* New FaaS framework called Funqy \* Improvements around mocking \* Keycloak admin client \* SmallRye Reactive Messaging 2.0 \* MongoDB client 4 \* Amazon Alexa extension \* LDAP authentication \* Improvements to the Vault support

We also made some nice performance improvements so your Quarkus applications should be faster than ever!

## Migration Guide

Eager to migrate your existing applications? Here is [our migration guide for 1.4](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.4).

## What's new?

### Java 8 deprecated

Building Quarkus applications with Java 8 is now deprecated. We recommend using Java 11 from now on.

We plan to drop Java 8 support for Quarkus 1.6 so in roughly two months. If you have a strong case for us keeping Java 8 support longer, please reach out to us on our `quarkus-dev` mailing list.

### Command mode

Until now, Quarkus goodness was reserved to server applications but these times are over!

You can now build command line applications with Quarkus using the new command mode. Find out all about it in [our dedicated reference documentation](/guides/command-mode-reference).

Stay tuned, we will publish a specific blog post shortly.

### HTTP/2

Quarkus now supports HTTP/2. It is enabled by default but can be disabled by setting `quarkus.http.http2` to `false`.

### New FaaS framework

We improved a lot on the function front (namely AWS Lambdas and Azure Functions) with the introduction of a new FaaS framework called Funqy.

A blog post will soon explain everything you need to know about Funky but in the meantime, you can refer to the updated documentation:

\* [Amazon Lambda](/guides/amazon-lambda) \* [Amazon Lambda with RESTEasy, Undertow, or Vert.x Web](/guides/aws-lambda-http) \* [Azure Functions (Serverless) with RESTEasy, Undertow, or Vert.x Web](/guides/azure-functions-http)

### Improvements around mocking

Mocking CDI beans is now easier than ever. We also have a nice integration with Mockito.

For more information, please refer to our [Testing your application guide](/guides/getting-started-testing).

Again, blog post coming soon!

### MongoDB client upgrade

The MongoDB client has been upgraded to version 4. This might break a few applications and might require some code updates.

### Vault

The default version of the Vault KV Secret Engine is now version 2. You can go back to v1 with some configuration.

Support for [the Vault TOTP engine](/guides/vault#totp) was added, as well as two new topic guides:

\* [Working with HashiCorp Vault’s Authentication](/guides/vault-auth) \* [Using HashiCorp Vault with Databases](/guides/vault-datasource)

### New extensions

#### Keycloak Admin Client

We had some issues with the Keycloak Admin Client in previous versions.

We added a dedicated extension called `quarkus-keycloak-admin-client` to ensure a smooth experience.

#### Amazon Alexa

Amazon Alexa is now supported in native thanks to the addition of the `quarkus-amazon-alexa` extension.

#### LDAP authentication

We added LDAP to the list of authentication stores supported out of the box by Quarkus.

### And more...

\* SmallRye Reactive Messaging has been updated to 2.0. \* A lot of component upgrades, bugfixes and small improvements.

### Full changelog

You can get the full changelogs of [1.4.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.4.0.CR1), [1.4.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.4.0.Final) and [1.4.1.Final](https://github.com/quarkusio/quarkus/releases/tag/1.4.1.Final) on GitHub.

## ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

## Contributors

The Quarkus community is growing and has now [269 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 1.4 release, thanks to Aaron Rogers, Adrian, Adrian Fiedler, Akito Kidani, Alex Soto, Alexander Schwartz, Alexey Loubyansky, Andrej Petras, András Péteri, Andy Damevin, Antoine Sabot-Durand, Antonio Goncalves, Aurea Munoz, Bartosz Firyn, Bill Burke, Cem Nura, Clement Escoffier, Daniel Petisme, Daniel Wamara, Daniel Platz, David M. Lloyd, Emmanuel Bernard, Erik Mattheis, Falko Modler, Felix Wong, Galder Zamarreño, Geoffrey De Smet, George Doenlen, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gwenneg Lepage, Gytis Trikleris, Harold Sun, Harsha R, Ioannis Canellos, Jaikiran Pai, Jan Martiska, Jason T. Greene, Jesper Skov, Jiri Ondrusek, Johan Westerlund, John O'Hara, Jonas Scholz, Jordan Moore, Jozef Hartinger, Julian Cui, Justin Holmes, Ken Finnigan, Ladislav Thon, Loïc Mathieu, Luis F Munoz, Manyanda Chitimbo, Marc Boissonneault, Mario Fusco, Marius Ileana, Martin Kouba, Martin Panzer, Matej Novotny, Matej Vasek, Matthias Andreas Benkard, Matthias Harter, Mattia, Max Rydahl Andersen, Mayank Kunwar, Michał Szynkiewicz, oscarfh, Pavol Loffay, Pedro Igor, Peter Palaga, Petr Nehyba, Phillip Kruger, Robbie Gemmell, Robert Marcano, Roberto Cortez, Rohan Maity, Rostislav Svoboda, Sanne Grinovero, Sebastian Daschner, Sebastien Dionne, Sergey Beryozkin, Stuart Douglas, Stéphane Épardaud, Timothy Power, Tobias Liese, Tobias Stadler, Tomaz Cerar, Vincent Sevel, Vincent Sourin, Vinícius Ferraz Campos Florentino, William Burns and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
