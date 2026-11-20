# Quarkus 3.13 - OpenTelemetry Metrics, OpenTelemetry 1.39, TLS registry improvements and more...

Today, we released Quarkus 3.13.

While the summer is here, Quarkus is making great progress and this release contains several exciting new features and improvements:

- [#39032](https://github.com/quarkusio/quarkus/pull/39032) - OpenTelemetry Metrics support
- [#41521](https://github.com/quarkusio/quarkus/pull/41521) - Bump OpenTelemetry to 1.39. and Instrumentation to 2.5.0
- [#41418](https://github.com/quarkusio/quarkus/pull/41418) - Add TLS registry CLI commands
- [#41501](https://github.com/quarkusio/quarkus/pull/41501) - Cert-Manager support and TLS periodic reload
- [#41804](https://github.com/quarkusio/quarkus/pull/41804) - Introduce the ability to automatically stand up an HTTP proxy for the REST Client
- [#41760](https://github.com/quarkusio/quarkus/pull/41760) - WebSockets Next: add support for Kotlin suspend functions
- [#41258](https://github.com/quarkusio/quarkus/pull/41258) - ArC: support interception of producers and synthetic beans
- [#41368](https://github.com/quarkusio/quarkus/pull/41368) - Add new `@WithTestResource` annotation and deprecate `@QuarkusTestResource`
- [#41457](https://github.com/quarkusio/quarkus/pull/41457) - Provide a configuration option for disabling live-reload

## Update

To update to Quarkus 3.13, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.13.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.13 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.13).

## What's new?

### OpenTelemetry

A couple of months ago, we started some major work on the OpenTelemetry extension and you might have noticed that we skipped a few OpenTelemetry releases.

It is our pleasure to announce that this extensive work has landed in Quarkus 3.13 with these major changes:

- Update to OpenTelemetry 1.39 and Instrumentation 2.5.0
- Introduce OpenTelemetry Metrics support

The documentation has been updated:

- [OpenTelemetry Tracing](https://quarkus.io/guides/opentelemetry-tracing) guide
- [OpenTelemetry Metrics](https://quarkus.io/guides/opentelemetry-metrics) guide

### TLS registry

In Quarkus 3.12, we introduced the TLS registry to simplify managing the TLS configuration and certificates.

This release introduces several additional improvements:

- You can automatically reload certificates
- You can use Kubernetes secrets or Cert-Manager to store your certificates

These new features are documented in the [TLS registry](https://quarkus.io/guides/tls-registry-reference) guide.

We added several `quarkus tls` commands to the CLI to help with this new feature.

### HTTP proxy for REST Client

When working with REST Clients, you might want to have a closer look at the requests sent to the server. You often end up having to use a pass-through proxy, especially when dealing with HTTPS connections.

Quarkus 3.13 comes with a new feature that starts a pass-through proxy in dev mode. This proxy can be used as target for Wireshark to get the content of the requests sent by the client.

It can be easily enabled by setting `quarkus.rest-client."client name".enable-local-proxy` to `true`. When starting Quarkus, a log message will indicate you the port of the proxy.

This feature is only available with the Quarkus REST Client (the RESTEasy client doesn't support it).

### WebSockets Next

WebSockets Next, our next-generation WebSockets extension continues to be improved in each release.

One major improvement in this release for our Kotlin users is the support of Kotlin suspend functions.

### ArC - Support interception of producers and synthetic beans

ArC is the CDI implementation of Quarkus. It is at the core of Quarkus and is a critical piece of the Quarkus architecture, and is regularly improved to support more use cases or simplify existing ones.

Starting with Quarkus 3.13, it will be possible to use interceptors on producers and synthetic beans - which are very often used by extensions.

### @WithTestResource replaces @QuarkusTestResource

`WithTestResource` replaces `QuarkusTestResource` that is now deprecated.

The only change is the default behavior of the annotation: by default, it will restrict the resources to the annotated class. You can easily go back to the previous behavior by adding `restrictToAnnotatedClass = false` to your `@WithTestResource` annotation.

`quarkus update` will take care of this change for you and make sure it keeps the current behavior.

### Disable live reload

In some cases, people wanted to use dev mode for the Dev Services feature but wanted to disable live reload.

You can now easily do it by setting the `quarkus.live-reload.enabled` configuration property to `false`.

### Quarkus CXF

Quarkus CXF 3.13 was released and is now available in [Quarkus Platform 3.13](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check [Quarkus CXF 3.13.0](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.13.0.html) and [Quarkus CXF 3.13.1](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.13.1.html) release notes for more information about what is new in this release.

## Full changelog

You can get the full changelog of [3.13.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.13.0.CR1) and [3.13.0](https://github.com/quarkusio/quarkus/releases/tag/3.13.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [973 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.13 release, thanks to Ales Justin, Alex Martel, Alexey Loubyansky, Andre F de Miranda, Andy Damevin, Auri Munoz, Bruno Alves, Bruno Baptista, chrischiedo, Christian Navolskyi, Christian Schmidt, Clement Escoffier, Cristiano Nicolai, Daniel Santos, David M. Lloyd, Dmitry Kryukov, Eric Deandrea, Foivos Zakkak, Fouad Almalki, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Giancarlo Calderón Cárdenas, Gonçalo Montalvão Marques, Guillaume Smet, Gunther C. Wenda, Gwenneg Lepage, Harald Albers, harlequin516, Holly Cummins, Ioannis Canellos, Jakub Jedlicka, James Cobb, James Netherton, Jan Hendriks, Jan Martiska, Jerome Prinet, João Daniel Simões, Juan Jose Garcia, Julien Ponge, Katia Aresti, Ladislav Thon, Luke Morfill, Maciej Lisowski, Marc Nuri, Marco Bungart, Marco Sappé Griot, Marek Skacelik, mariofusco, Mark Dijkstra, marko-bekhta, Martin Bartoš, Martin Kouba, Matej Novotny, Matheus Cruz, Matteo Franci a.k.a. Fugerit, Max Rydahl Andersen, melloware, Michael Edgar, Michael Hamburger, Michal Karm Babacek, Michal Vavřík, Michel Käser, mkrueger92, Nikolas Schmidt-Voigt, Ozan Gunalp, Peter Palaga, Phillip Krüger, renanmachad, Roberto Balarezo, Roberto Cortez, Rolfe Dlugy-Hegwer, Ryan Dens, Sanne Grinovero, Sebastian Schuster, Sergey Beryozkin, Shivansh, Simon Bradette, Stéphane Épardaud, Thomas Canava, vsevel, xstefank, YeonguChoe, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
