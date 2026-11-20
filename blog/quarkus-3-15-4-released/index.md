# Quarkus 3.15.4 released - LTS maintenance release

Today, we released Quarkus 3.15.4, our next maintenance release for the 3.15 LTS stream.

This release contains bugfixes and documentation improvements.

It should be a safe upgrade for anyone already using 3.15. However, as part of fixing a security issue, one small potentially breaking change had to be applied: [#45877](https://github.com/quarkusio/quarkus/pull/45877). This may affect you if you consume Netty allocator metrics.

## Update

To update to Quarkus 3.15, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update --stream=3.15
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.15.

## Full changelog

You can get [the full changelog of 3.15.4 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.15.4).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
