# Quarkus 2.14.1.Final released - Maintenance release

Today, we released Quarkus 2.14.1.Final with some bugfixes and documentation improvements on top of our 2.14.0.Final release.

It is a recommended upgrade for anyone already using 2.14.

If you are not already using 2.14, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.14).

## GraalVM/Mandrel upgrade

We don't usually upgrade GraalVM/Mandrel in a micro release but GraalVM/Mandrel 22.3 is a LTS release and we decided to move to it early, rather than wait for Quarkus 2.15.

We don't think it will cause problems for applications but you might have to adjust your extensions if you make use of GraalVM substitutions. Make sure to have a look at [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.14) if it is the case.

Also, GraalVM/Mandrel 22.2 is now the minimal version to use to build native executables.

### Mandrel by default

For in-container builds, Quarkus now uses Mandrel by default to build native executables.

## Full changelog

You can get [the full changelog of 2.14.1.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.14.1.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
