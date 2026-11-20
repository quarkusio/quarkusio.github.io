# Quarkus 2.5.4.Final released - Maintenance release

We just released Quarkus 2.5.4.Final, a maintenance release for our 2.5 release train containing bugfixes and documentation improvements.

It is a safe upgrade for anyone already using 2.5.

If you are not using 2.5 already, please refer to the [2.5 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.5).

## Main changes

### Jackson 2.12.6

We upgraded to Jackson 2.12.6 to fix [a security issue in Jackson Databind](https://github.com/FasterXML/jackson-databind/issues/3328).

### Platform resolution fix

An issue has been fixed regarding how the platforms are resolved in the dev tools. This issue could cause code.quarkus.io do go out of memory when creating projects with high number of extensions.

If you are running your own instance of [code.quarkus.io](https://github.com/quarkusio/code.quarkus.io) internally, you will want to update to this version to avoid issues.

## Full changelog

You can get [the full changelog of 2.5.4.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.5.4.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
