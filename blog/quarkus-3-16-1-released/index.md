# Quarkus 3.16 - OpenTelemetry Logging, LGTM Quarkus dashboard and too many things to list here

After the 3.15 LTS release comes Quarkus 3.16 and a lot of new features and enhancements.

Quarkus 3.16 is the result of the 2 month worth of work so it's more packed than your usual Quarkus minor.

> [!NOTE]
> We went directly to 3.16.1 due to the inclusion of a last minute fix.

Notable changes are:

- [#43005](https://github.com/quarkusio/quarkus/pull/43005) - Drop the compatibility layer for the Big Reactive Rename
- [#38239](https://github.com/quarkusio/quarkus/pull/38239) - OpenTelemetry Logging support
- [#41264](https://github.com/quarkusio/quarkus/pull/41264) - LGTM Quarkus dashboard
- [#42954](https://github.com/quarkusio/quarkus/pull/42954) - Generate reflection-free Jackson deserializers
- [#42642](https://github.com/quarkusio/quarkus/pull/42642) - Quarkus REST - Support record parameter containers
- [#43331](https://github.com/quarkusio/quarkus/pull/43331) - Introduce per invocation override of REST Client's base URL
- [#41866](https://github.com/quarkusio/quarkus/pull/41866) - Add quarkus-oidc-client-registration extension
- [#42749](https://github.com/quarkusio/quarkus/pull/42749) - Add new AuthorizationPolicy annotation to bind named HttpSecurityPolicy to a Jakarta REST endpoints
- [#42879](https://github.com/quarkusio/quarkus/pull/42879) - Add OIDC Client SPI
- [#43241](https://github.com/quarkusio/quarkus/pull/43241) - Support @PermissionsAllowed defined on meta-annotation
- [#43283](https://github.com/quarkusio/quarkus/pull/43283) - Introduce OidcResponseFilter
- [#42935](https://github.com/quarkusio/quarkus/pull/42935) - Support for two or more authentications for a single request
- [#43609](https://github.com/quarkusio/quarkus/pull/43609) - Support Keycloak Dev Service when OIDC client is used without OIDC extension
- [#42534](https://github.com/quarkusio/quarkus/issues/42534) - Integrate GraphQL clients with the TLS registry extension
- [#43303](https://github.com/quarkusio/quarkus/pull/43303) - Integrate Keycloak Admin Client with TLS registry
- [#43402](https://github.com/quarkusio/quarkus/pull/43402) - Auto log for Dev Services in containers
- [#43441](https://github.com/quarkusio/quarkus/pull/43441) - Add HTTP Access Log to Dev UI
- [#42907](https://github.com/quarkusio/quarkus/pull/42907) - Allow multiple format and themes in the Config Doc generator

Note that we haven't forgotten 3.15 LTS, a 3.15.2 LTS is in the works and will be released in November. We are carefully selecting the fixes we will backport to it.

## Update

To update to Quarkus 3.16, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.16.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.16 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.16).

## What's new?

### Big Reactive Rename compatibility layer dropped

Remember the Big Reactive Rename? It happened in 3.9 with the ultimate goal of avoiding the confusion between extensions that have a reactive core and are supporting equally well reactive and non-reactive workloads and extensions that are purely designed as reactive.

A lot of extensions were renamed and we put in place relocations both for the artifacts and for the configuration.

Compatibility layers have a cost and we decided to drop this one in 3.16, after the 3.15 LTS release.

If you encounter issues with this change, please refer to the [Quarkus 3.9 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9).

If you are using `quarkus update` to update to each new version, the changes were handled for you already.

### OpenTelemetry Logging

Quarkus 3.16 supports distributed logging via OpenTelemetry Logging. This is the natural continuation of the OpenTelemetry work in Quarkus.

This support is part of the already existing OpenTelemetry extension and can be easily enabled via configuration properties.

To learn more about it, have a look at the [dedicated guide](https://quarkus.io/guides/opentelemetry-logging)

### LGTM dashboard

When using the LGTM Dev Services, an out of the box Quarkus dashboard will now be provided.

### Quarkus REST

In Quarkus REST, you can use custom classes as parameters of your REST methods but records were not supported. It is now the case in Quarkus 3.16.

When using the REST Client, providing a URL is mandatory and you usually configure it globally. However, from time to time, you might want to configure it per-invocation. The `@Url` annotation was introduced for that: annotate a parameter of your REST Client method with it and you can dynamically provide a URL.

### Jackson

You might remember that in Quarkus 3.14, we introduced [faster reflection-free serializers](https://quarkus.io/blog/quarkus-3-14-1-released/#faster-reflection-free-jackson-serializers).

And you might have wondered "where are my faster reflection-free **de**serializers"?

They just landed in 3.16!

### Security

As usual, this version comes with several new features and enhancements related to our security layer:

- [#41866](https://github.com/quarkusio/quarkus/pull/41866) - Add quarkus-oidc-client-registration extension - see [documentation](https://quarkus.io/guides/security-openid-connect-client-registration) here
- [#42749](https://github.com/quarkusio/quarkus/pull/42749) - Add new AuthorizationPolicy annotation to bind named HttpSecurityPolicy to a Jakarta REST endpoints - see [documentation](https://quarkus.io/guides/security-authorize-web-endpoints-reference#authorization-policy-example) here
- [#42879](https://github.com/quarkusio/quarkus/pull/42879) - Add OIDC Client SPI - see [documentation](https://quarkus.io/guides/security-openid-connect-client-reference#oidc-client-spi) here
- [#43241](https://github.com/quarkusio/quarkus/pull/43241) - Support `@PermissionsAllowed` defined on meta-annotation - see [documentation](https://quarkus.io/guides/security-authorize-web-endpoints-reference#permission-meta-annotation) here
- [#43283](https://github.com/quarkusio/quarkus/pull/43283) - Introduce `OidcResponseFilter` - see [documentation](https://quarkus.io/guides/security-oidc-code-flow-authentication#code-flow-oidc-response-filters) here
- [#42935](https://github.com/quarkusio/quarkus/pull/42935) - Support for two or more authentications for a single request - described [here](https://quarkus.io/guides/security-authentication-mechanisms)
- [#43609](https://github.com/quarkusio/quarkus/pull/43609) - Support Keycloak Dev Service when OIDC client is used without OIDC extension

### TLS registry everywhere

The TLS registry was introduced in Quarkus a while ago and we are iterating in each version to migrate more extensions to it.

In Quarkus 3.16, two new extensions have been adapted to rely on the centralized TLS registry:

- SmallRye GraphQL Client
- Keycloak Admin Client

### Dev UI

The Dev UI is continuously enhanced but we wanted to highlight a very nice addition: logs from your Dev Services containers and HTTP access logs are now available in the Dev UI.

### Configuration documentation

When developing extensions, it can be handy to publish your configuration documentation.

Until now, it was only possible to publish it in Asciidoc. With 3.16, you can also generate Markdown by passing `<format>markdown</format>` to the configuration of the Config Doc Maven Plugin.

### Platform component upgrades

#### Camel Quarkus

Camel Quarkus has been updated to 3.16.0.

#### Quarkus CXF

Quarkus CXF 3.16 was released and is now available in [Quarkus Platform 3.16](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check the [Quarkus CXF 3.16.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.16.0.html) release notes for more information about what is new in this release.

## Full changelog

You can get the full changelog of [3.16.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.16.0.CR1), [3.16.0](https://github.com/quarkusio/quarkus/releases/tag/3.16.0), and [3.16.1](https://github.com/quarkusio/quarkus/releases/tag/3.16.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1020 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.16 release, thanks to AB, Adriano Moreira, Akulov S V, Ales Justin, Alex Martel, Alexandros Antonakakis, Alexey Loubyansky, Andreas Stangl, Andy Damevin, Auri Munoz, AxiomaticFixedChimpanzee, Bassel Rachid, Bruno Baptista, Chris Cranford, Chris Laprun, Christian Navolskyi, Claudio Miranda, Clement Escoffier, Dale Peakall, Daniel Bobbert, Daniel Cunha, Daniel Ezihe, Dannier Leonides Galicia Chinchilla, David M. Lloyd, Davide D'Alto, Dimitris Polissiou, Domenico Briganti, Falko Modler, Foivos Zakkak, Francesco Nigro, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Gunnar Morling, Gunther C. Wenda, Holly Cummins, Inaki Villar, Ioannis Canellos, Jakub Gardo, Jakub Jedlicka, Jan Martiska, jcarranzan, Jeremy Whiting, Jerome Prinet, Jonathan Kolberg, Jorge Solórzano, Julien Ponge, Jérémie Bresson, Jérémie Panzer, Katia Aresti, KERN Christian, Konrad Durnoga, KS, Ladislav Thon, Lars, Laurent Perez, Loic Hermann, Lorenzo De Francesco, Loïc Hermann, Loïc Mathieu, luneo7, Marc Nuri, Marcel Stör, Marcelo Ataxexe Guimarães, Marek Skacelik, mariofusco, marko-bekhta, Martin Bartoš, Martin Kouba, Matej Novotny, Matheus Cruz, Matthias Schorsch, mauroantonio.depalma, Max Rydahl Andersen, Maximilian Rehberger, Melloware, Michael Edgar, Michal Maléř, Michal Vavřík, Nathan Erwin, Nicholas Kolatsis, Ozan Gunalp, Ozzy Osborne, Paul6552, Paulo Casaes, Peer Bech Hansen, Peter Palaga, peubouzon, PhilKes, Phillip Krüger, polarctos, Ralf Ueberfuhr, rghara, Robert Stupp, Roberto Cortez, RobinDM, Rod Cheater, Rolfe Dlugy-Hegwer, Roman Lovakov, Rostislav Svoboda, Sanne Grinovero, Sebastian Schuster, Sergey Beryozkin, Seto, sku20, Stéphane Épardaud, Thomas Canava, Thomas Segismont, Tiago Bento, tmulle, Vincent Sevel, xstefank, yamada-y0, Yasser Greyeb, Yoann Rodière, Yurii Dubinka, and Žan Ožbot.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
