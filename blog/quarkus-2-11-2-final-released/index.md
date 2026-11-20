# Quarkus 2.11.2.Final released - CVE-2022-2466 is still ongoing

We thought we got to the bottom of CVE-2022-2466, a security issue we have with GraphQL services since 2.10 was released, but this one keeps on giving.

This issue is only of importance to you if you are exposing GraphQL services using the `quarkus-smallrye-graphql` extension. Consuming GraphQL services is fine.

If you are in this case, we recommend to stay on the latest 2.9 for the time being, which is 2.9.2.Final.

If you are not using `quarkus-smallrye-graphql`, you are safe to go with the latest and greatest Quarkus that is 2.11.2.Final.

We are working hard to fully circumvent CVE-2022-2466 and will hopefully release a 2.11.3.Final soon that fully fixes the issue.

## Migration Guide

If you are not already using 2.11, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.11).

## Full changelog

You can get [the full changelog of 2.11.2.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.11.2.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
