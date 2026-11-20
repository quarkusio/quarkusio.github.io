# Quarkus 3.3.0 released - OpenTelemetry improvements, Reactive Messaging Pulsar extension

It is our pleasure to announce the release of Quarkus 3.3.0.

The first thing you will notice in this release is that we dropped the `.Final` suffix. This suffix was introduced to make sure final releases were sorted properly compared to alphas, beta and candidate releases, at a time where the Java tooling had troubles sorting versions properly. These days are long gone so it is time for us to drop the suffix and simplify our version scheme.

We currently maintain 3 version streams in the community:

- 3.3: it is the latest and greatest and it introduces new features
- 3.2: it is our current [LTS release](/blog/lts-releases/)
- 2.16: we plan to release a few more releases but we recommend you to migrate your applications before the end of October, as there is little chance we will release new 2.16 releases after this date

Major changes are:

\* A lot of improvements to the OpenTelemetry extension \* Reactive Messaging Pulsar extension \* Ability to customize the ObjectMapper in REST Client Reactive Jackson \* Micrometer Introduce a way to completely customize MeterRegistry \* Drop elasticsearch-high-level-rest-client extension and upgrade Dev Services and tests to Elasticsearch 8 \* Various security-related enhancements

## Update

To update to Quarkus 3.3, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

To migrate from 3.2, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.3).

If you are not already using 3.x, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details. Once you upgraded to 3.0, also have a look at the [3.1](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1) and [3.2](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2) migration guides.

## What's new?

### OpenTelemetry

This version brings extensive improvements to the OpenTelemetry extension:

- OTel 1.28 is now supported.
- The default OTel exporter has been replaced by a Quarkus implementation on top of Vert.x. This allows us not to depend on the OkHttp library. The exporter continues to be automatically wired with CDI, that's why the `quarkus.otel.traces.exporter` property defaults to `cdi`.
- The new Quarkus Vert.x exporter now supports `grpc` (default) and `http/protobuf`. Please change the protocol with this property: `quarkus.otel.exporter.otlp.traces.protocol`.
- The Quarkus OTel exporter will now have TLS support.
- When setting `quarkus.tls.trust-all=true`, it will also disable all SSL verifications on the Quarkus OTel exporter.
- We added a way to customize the propagation header. This can be achieved by implementing the `TextMapPropagatorCustomizer` interface. This can be used, as an example, to restrict propagation of OpenTelemetry trace headers and prevent potentially sensitive data to be sent to third party systems
- By setting `quarkus.otel.traces.eusp.enabled=true`; you can add information about the user related to each span. The user's ID and roles will be added to the span attributes, if available.
- We now properly report the `http.route` attribute.
- Swagger UI endpoints will not be tracked anymore.
- Spans of failed requests will now contain stack traces.
- OTel instantiation has been improved to prevent racing condition at startup.

### Reactive Messaging

A Pulsar extension for Reactive Messaging has been added to Quarkus. And it comes with [Dev Services](https://quarkus.io/guides/pulsar-dev-services)!

Please have a look at the [dedicated guide](https://quarkus.io/guides/pulsar).

### Security

- We added support for OIDC authorization code flow nonce.
- We provide easy configuration for a lot of common OIDC provider and we added support for a new one: Twitch.
- OIDC JavaRequest checks can be customized.
- The OIDC `@Tenant` annotation is used to resolved tenants.
- OIDC token propagation is supported during `SecurityIdentity`\` augmentation.

### Elasticsearch

The deprecated `quarkus-elasticsearch-high-level-rest-client` extension has been dropped.

It was relying on the previous high level client that was deprecated by Elastic and was not Open Source anymore in the recent versions (we were still using an old version).

This extension is replaced by the [Elasticsearch Java Client extension](https://quarkus.io/guides/elasticsearch#using-the-elasticsearch-java-client). It is not a drop in replacement so the migration will require some work.

### REST Client Reactive

You can now customize the `ObjectMapper` when using REST Client Reactive Jackson.

### Micrometer

It is now possible to customize the `MeterRegistry` as [described in our documentation](https://quarkus.io/guides/telemetry-micrometer#use-meterregistrycustomizer-for-arbitrary-customizations-to-meter-registries).

Netty metrics were added.

### GraalVM

We are now relying consistently on the `org.graalvm.sdk:graal-sdk` artifact (we previously used the non-API `svm` artifact).

The dependencies to this artifact are marked as `provided` in Quarkus so they are not transitive: if you want to include GraalVM substitutions in your applications, please add it as a dependency yourself from now on.

It is part of the BOM so you don't have to define the version.

## Full changelog

You can get the full changelog of [3.3.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.3.0.CR1) and [3.3.0](https://github.com/quarkusio/quarkus/releases/tag/3.3.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [834 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.3 release, thanks to Ales Justin, Alexey Loubyansky, A.Moscatelli, Andrea Peruffo, Andy Damevin, Anthony T. Lannutti, Auri Munoz, biswassri, Bruno Baptista, Chris Laprun, Clement Escoffier, Daniel Cunha, Daryl Koh, Dave Maughan, Davide D'Alto, Emile de Weerd, Erin Schnabel, Falko Modler, Foivos Zakkak, Fouad Almalki, franz1981, Galder Zamarreño, George Gastaldi, Georgios Andrianakis, Giancarlo Calderón Cárdenas, Guillaume Smet, Holly Cummins, humberto, imperatorx, Ioannis Canellos, James Netherton, Jan Martiska, Jerome Prinet, Joan Ruget, Jose Carvajal, Josef Smrcka, Julien Ponge, Katia Aresti, Kenneth Bøgedal, Kevin Dubois, Kevin Howell, Kevin Wooten, kpapakyriakos, Ladislav Thon, Loïc Mathieu, Manyanda Chitimbo, Marek Skacelik, Marko Bekhta, Martin Kouba, Martin Ocenas, Martin Panzer, Matej Novotny, Max Rydahl Andersen, melloware, Michael Edgar, Michael Musgrove, Michal Maléř, Michal Vavřík, Michelle Purcell, Nicolas Filotto, Ozan Gunalp, Paul Carter-Brown, Paulo Casaes, Pedro Igor, Peter Fortuin, Peter Palaga, Phillip Krüger, rjtmahinay, Robert Kühne, Roberto Cortez, Rolfe Dlugy-Hegwer, Romain Pelisse, Rostislav Svoboda, Samet Karakaya, Sanne Grinovero, Sebastian Schuster, Sergey Beryozkin, Severin Gehwolf, Siva_M7, suchwerk, The-Huginn, Thomas Segismont, Vincent Sevel, xstefank, ygyg70, Yoann Rodière, Yoshikazu Nojima, yyang.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
