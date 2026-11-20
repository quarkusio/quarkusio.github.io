# Quarkus 1.8.1.Final released - Bugfixes, RESTEasy Multipart extension

Today, we released Quarkus 1.8.1.Final with some bugfixes and documentation improvements.

We also introduced a new RESTEasy Multipart extension to fix an recurring encoding issues with multipart.

It is a safe upgrade for everyone using 1.8.0.Final.

## What's new?

This release is a maintenance release that fixes various bugs found in 1.8.0.Final and provides some documentation updates.

You can get [the full changelog of 1.8.1.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/1.8.1.Final).

### RESTEasy Multipart

The default encoding for multipart requests in RESTEasy is US-ASCII which is not very convenient in 2020.

Thanks to the `quarkus-resteasy-multipart` extension, the default encoding is now UTF-8 and it can be configured if needed. If working with multipart, make sure you use this extension.

More on this in the [Writing JSON REST Services guide](/guides/rest#multipart).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
