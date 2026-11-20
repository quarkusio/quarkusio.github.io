# Quarkus 2.7.1.Final released - Maintenance release

Today, we released Quarkus 2.7.1.Final, our first maintenance release for our 2.7 release train. It contains bugfixes and documentation improvements, but also a couple of new features, filling a few gaps.

It is a safe upgrade for anyone already using 2.7.

If you are not using 2.7 already, please refer to the [2.7 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.7).

## What's new

### Kogito extensions are back

2.7.1.Final reintroduces the Kogito extensions that were disabled for 2.7.0.Final due to an incompatibility. So if you are using Kogito, you can now upgrade your applications to 2.7.

### gRPC

Our gRPC extension only generated code for the application classes. Starting with 2.7.1.Final, it also generates code from the dependencies.

### OIDC token propagation

We introduced a reactive counterpart to the OIDC token propagation extension, that you can use with RESTEasy Reactive.

## Full changelog

You can get [the full changelog of 2.7.1.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.7.1.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
