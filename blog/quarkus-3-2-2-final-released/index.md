# Quarkus 3.2.2.Final released - Maintenance release

Today, we released Quarkus 3.2.2.Final, the second maintenance release of our 3.2 release train.

We released 3.2.1.Final a few days ago but two annoying regressions were spotted before we announced it so we decided to not announce it and release a 3.2.2.Final right away.

These two releases contains mostly bugfixes and documentation improvements.

It should be a safe upgrade for anyone already using 3.2, but if you are using the management network interfance and OpenAPI/Swagger UI, please have look below.

If you are not already using 3.2, please refer to the [Quarkus 3.2 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2).

And if you are not already using 3.0, please refer to [the Quarkus 3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) and in particular the section about the [upgrade path to 3.0](https://quarkus.io/blog/quarkus-3-0-final-released/#upgrading) together with the [3.1 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1) and [Quarkus 3.2 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2).

## OpenAPI, Swagger UI, and the management network interface

We recently introduced the management network interface which allows to serve management endpoints (typically the health endpoint) on a different network interface.

Until now, Open API and Swagger UI were not served on the management network interface when enabled. It was an oversight and we decided to fix it for 3.2.1.Final, even if we don't usually introduce this type of behavioral change in micro releases.

From 3.2.1.Final, when the management network interface is enabled, Open API and Swagger UI endpoints will be served from the management network interface by default.

You can disable this behavior by setting the `quarkus.smallrye-openapi.management.enabled=false` configuration property to `false`.

## Full changelog

You can get [the full changelog of 3.2.1.Final](https://github.com/quarkusio/quarkus/releases/tag/3.2.1.Final) and [the full changelog of 3.2.2.Final](https://github.com/quarkusio/quarkus/releases/tag/3.2.2.Final) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
