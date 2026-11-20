# Quarkus 3.28 - More security features, custom Grafana dashboards, support for multiple clients in Liquibase MongoDB, and more

Today, we released both Quarkus 3.27 LTS and Quarkus 3.28. While Quarkus 3.27 is a Long Term Support (LTS) release, branched from Quarkus 3.26 and maintained for 12 months, Quarkus 3.28 comes with some new features.

Quarkus 3.28 introduces the following notable changes:

- [#49618](https://github.com/quarkusio/quarkus/pull/49618) - Provide a fluent API for CSRF programmatic set up
- [#49122](https://github.com/quarkusio/quarkus/pull/49122) - Support flow-specific OIDC request and response filters
- [#49151](https://github.com/quarkusio/quarkus/pull/49151) - Allow for custom Grafana dashboards
- [#49805](https://github.com/quarkusio/quarkus/pull/49805) - Add support for multiple clients in Liquibase MongoDB extension

Building Quarkus applications should also be faster with this release, thanks to an effort to reduce memory allocations during the build, parallelize some more tasks such as Hibernate ORM proxy generation or building the Jar file, and various other improvements.

## Update

To update to Quarkus 3.28, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.28.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.28 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.28).

## What's new?

### Security

It is now possible to configure CSRF protection programmatically using a [fluent API](https://quarkus.io/guides/security-csrf-prevention#csrf-prevention-programmatic-set-up).

You can restrict an `OidcRequestFilter` or an `OidcResponseFilter` to a specific authentication flow by using the `@BearerTokenAuthentication` and `@AuthorizationCodeFlow` annotations. Find out more about this new feature in [OpenID Connect (OIDC) Bearer token authentication](https://quarkus.io/guides/security-oidc-bearer-token-authentication#restrict-oidc-filter-to-bearer-auth-flow) and [OpenID Connect authorization code flow mechanism for protecting web applications](https://quarkus.io/guides/security-oidc-code-flow-authentication#restrict-oidc-filter-to-code-flow) guides.

### Custom Grafana dashboards

You can now provide custom Grafana dashboards by placing JSON descriptors in the `META-INF/grafana/` directory of your application. You can find more information in the [Observability Dev Services with Grafana OTel LGTM](https://quarkus.io/guides/observability-devservices-lgtm#custom-dashboards) guide.

### Liquibase MongoDB

The MongoDB extension have supported multiple clients for a very long time. The Liquibase MongoDB extension was only supporting a single client so far, but it now [supports multiple clients](https://quarkus.io/guides/liquibase-mongodb#multiple-clients) as well.

## Full changelog

You can get the full changelog of [3.28.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.28.0.CR1), [3.28.0](https://github.com/quarkusio/quarkus/releases/tag/3.28.0), and [3.28.1](https://github.com/quarkusio/quarkus/releases/tag/3.28.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [1122](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.28 release, thanks to Akulov S V, Ales Justin, Alex Martel, Alexey Loubyansky, andreatp, Andy Damevin, Bhawna, Bruno Baptista, Chris Laprun, Christian Beikov, Clement Escoffier, Diego Fernandez Aceves, Eli Barbosa, Eric Deandrea, Florent Fourcade, Foivos Zakkak, Fouad Almalki, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Holly Cummins, Jakub Jedlicka, Jan Martiska, Jan Scheidegger, Jonas Rutishauser, Joseph Zhang, Julien Ponge, Justin Bertram, Karm Michal Babacek, Katia Aresti, Ladislav Thon, Lars Andringa, Luca Molteni, Luke Morfill, Marc Nuri, Marco Belladelli, marko-bekhta, Martin Bartoš, Martin Kouba, Matej Novotny, Melloware, Michael Edgar, Michal Maléř, Michal Vavřík, Mikhail Polivakha, Ozan Gunalp, Paulo Casaes, Peter Palaga, Phillip Krüger, Roberto Cortez, Rolfe Dlugy-Hegwer, Rostislav Svoboda, Rúben Garcia, Saee Saadat, Sanne Grinovero, Sergey Beryozkin, Stéphane Épardaud, Teymur Babayev, Unai Valle, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
