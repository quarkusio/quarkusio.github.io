# Quarkus 3.21.2 - Maintenance release

We released Quarkus 3.21.2, the second maintenance release for our 3.21 release train.

## Noticeable changes

### JUnit update

We had to update JUnit from 5.10 to 5.12 to solve an alignment issue with Mockito.

JUnit 5.11 comes with [several changes](https://junit.org/junit5/docs/5.11.1/release-notes/#release-notes-5.11.0-junit-platform-bug-fixes) to how hierarchies of tests are handled so you might have to adjust your tests accordingly.

## Update

To update to Quarkus 3.21, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.21.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.21 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.21).

## Full changelog

You can get the full changelog of [3.21.2](https://github.com/quarkusio/quarkus/releases/tag/3.21.2) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
