# Quarkus 2.10.3.Final released - Fixes CVE-2022-2466

2.10.0.CR1 introduced a major security issue known as CVE-2022-2466 in the SmallRye GraphQL server extension and all the 2.10.x releases are affected (together with 2.11.0.CR1). 2.10.3.Final fixes it and the fix will also be included in the upcoming 2.11.0.Final. You are only affected by this issue if you are exposing GraphQL services.

The context of the requests was not properly terminated and, for a given thread, all further requests would use the context of the first request the thread treated. The context includes authentication if your GraphQL services require authentication.

This is an extremely serious issue so we urge anyone who has already upgraded to 2.10.x and is exposing GraphQL services to upgrade to 2.10.3.Final.

Note that 2.9 and earlier are not affected by the issue.

This version also contains some minor additional fixes.

If you are not using 2.10 already, please refer to the [2.10 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.10).

## Full changelog

You can get [the full changelog of 2.10.3.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.10.3.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
