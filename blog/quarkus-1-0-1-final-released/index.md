# Quarkus 1.0.1.Final released - Important security fix

We just released 1.0.1.Final to fix an important security issue introduced in CR2 and still present in 1.0.0.Final.

Please upgrade to this version as soon as possible if you are using our security layer.

## What's new?

### Security issue fix

If you are using our security annotations (e.g. `@RolesAllowed`) and also other annotations (such as Bean Validation annotations) on the parameters of your secured methods, the security checks would entirely be bypassed.

This issue was originally reported here: <https://github.com/quarkusio/quarkus/issues/5763> .

1.0.1.Final fixes this issue and upgrade is highly recommended.

### Full changelog

You can get [the full changelog of 1.0.1.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/1.0.1.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
