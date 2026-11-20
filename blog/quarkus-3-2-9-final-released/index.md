# Quarkus 3.2.9.Final released - Maintenance LTS release

Quarkus 3.2.9.Final, the ninth maintenance release of the 3.2 LTS release train has been released.

This release fixes the following regressions reported by the community:

- [Gradle plugin: quarkus.container-image.push=true not working in 3.2.8](https://github.com/quarkusio/quarkus/issues/36992)
- [Regression with ForwardedParser setting an empty host header](https://github.com/quarkusio/quarkus/issues/37045)

And the following critical bug fixes:

- [Handle duplicated context in the CacheResultInterceptor](https://github.com/quarkusio/quarkus/pull/37077)
- [GraphQL authenticated subscription](https://github.com/quarkusio/quarkus/issues/20092)

It should be a safe upgrade for anyone already using a 3.2 release.

If you are not already using a 3.2 release, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2).

## Full changelog

You can get [the full changelog of 3.2.9.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.2.9.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
