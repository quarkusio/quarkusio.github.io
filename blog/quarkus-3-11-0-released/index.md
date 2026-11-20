# Quarkus 3.11 - Dev Services for Observability, progress on WebSockets.next, Infinispan Cache extension and more

It is the end of the month, a lot of us are in Slovenia for JCon (if you are there, come talk to us!) and it is time for a Quarkus release: here comes Quarkus 3.11!

Here are the main changes for this release:

- [#38448](https://github.com/quarkusio/quarkus/pull/38448) - Observability extensions - Dev Services, Dev Resources, LGTM
- [#39836](https://github.com/quarkusio/quarkus/pull/39836) - Infinispan Cache Extension
- [#40309](https://github.com/quarkusio/quarkus/pull/40309) - WebSockets Next: client endpoints
- [#40534](https://github.com/quarkusio/quarkus/pull/40534) - WebSockets Next: initial version of security integration
- [#40273](https://github.com/quarkusio/quarkus/pull/40273) - Allow quarkus:run to launch Dev Services
- [#40539](https://github.com/quarkusio/quarkus/pull/40539) - Support for OIDC session expired page
- [#40600](https://github.com/quarkusio/quarkus/pull/40600) - Introduce `OidcRedirectFilter`

## Update

To update to Quarkus 3.11, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.11.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.11 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.11).

If you are not already using 3.x, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details. Once you upgraded to 3.0, also have a look at the [3.1](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1), [3.2](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2), [3.3](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.3), [3.4](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.4), [3.5](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.5), and [3.6](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.6), [3.7](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.7), [3.8](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.8), [3.9](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9), [3.10](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.10), and [3.11](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.11) migration guides.

## What's new?

### Dev extensions for observability

A lot of efforts have been put into simplifying the setup of dev resources for observability.

This includes Dev Services for:

- Grafana
- Jaeger
- OTel
- VictoriaMetrics DB
- VictoriaMetrics Agent / scraper

Actually, they can be used in 3 different contexts:

- Re-usable Dev Services (default)
- Simple start/stop Dev Services
- Single / plain per QuarkusTestResource dev resource

You can find more information about this new feature in this [entry point to more detailed documentation](https://quarkus.io/guides/observability-devservices).

### Infinispan Cache extension

Quarkus has had a cache extension for a long time. This cache extension is backed by Caffeine by default.

We also had an Infinispan Client extension.

With the new Infinispan Cache extension, you can now use the cache extension approach and use Infinispan as a backend.

And, you know us by now, we baked a [dedicated guide](https://quarkus.io/guides/cache-infinispan-reference) just for you.

### WebSockets.next

WebSockets.next is our... next (tadam!)-gen WebSockets extension (both client and server).

It is experimental at the moment and we are making steady progress on it with each release.

In Quarkus 3.11, we added:

- The ability to easily define WebSockets clients.
- An initial version of the security integration (we will add more features in future releases).

### quarkus:run and Dev Services

`./mvnw quarkus:run` is a convenient way to start your Quarkus application. It is now able to start Dev Services so that your favorite service can be started automatically as a container.

### Security

As for each release, this release comes with several new security features including:

- Support for OIDC session expired page
- The introduction of `OidcRedirectFilter`, which allows to customize OIDC redirects

### Quarkus CXF

Quarkus CXF 3.11.0 was released and is now available in [Quarkus Platform 3.11](https://code.quarkus.io/?extension-search=origin:platform%20quarkus-cxf). Check [Quarkus CXF 3.11.0 release notes](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.11.0.html) for more information about what is new in this release.

## Full changelog

You can get the full changelog of [3.11.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.11.0.CR1) and [3.11.0](https://github.com/quarkusio/quarkus/releases/tag/3.11.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [946 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.11 release, thanks to Alasdair Preston, Aleksandr Nichiporuk, Ales Justin, Alexey Loubyansky, Andy Damevin, Antonio Musarra, antonwiens, Arcane418, asjervanasten, Bruno Baptista, Chris Laprun, cknoblauch, Clement Escoffier, Danny Gräf, David M. Lloyd, emile, Eric Deandrea, Foivos Zakkak, Fouad Almalki, George Gastaldi, Georgios Andrianakis, glefloch, Guillaume Smet, Gwenneg Lepage, Holly Cummins, Ivan Puntev, Jakub Jedlicka, Jan Martiska, Juan Zuriaga, Katia Aresti, Ladislav Thon, Lin Gao, Lorenzo Vannucchi, Loïc Mathieu, Marc Nuri, Marek Skacelik, mariofusco, Martin Kouba, Matej Novotny, Max Rydahl Andersen, Michal Maléř, Michal Vavřík, Nathan Erwin, Nithanim, Ozan Gunalp, Phillip Krüger, punkepa, Roberto Cortez, Rostislav Svoboda, Sanne Grinovero, Sauli Ketola, Selim, Sergey Beryozkin, Stuart Douglas, Stéphane Épardaud, Thomas Segismont, Vincent Sourin, Vinicius A. Santos, vkn, Yoann Rodière, and Zheng Feng.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
