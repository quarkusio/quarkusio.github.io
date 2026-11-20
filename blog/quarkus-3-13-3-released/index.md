# Quarkus 3.13.3 released - Maintenance release

Today, we released Quarkus 3.13.3, our third maintenance release for the 3.13 release train.

This release contains bugfixes and documentation improvements. It should be a safe upgrade for anyone already using 3.13.

One important change is that we downgraded Apache Commons Lang from 3.16.0 to 3.14.0, because of a change in behavior as to the default source of entropy. It is causing some severe issues (including the application completely stalling), in particular with [Liquibase](https://github.com/liquibase/liquibase/issues/6178).

You can find more information about this issue [here](https://issues.apache.org/jira/browse/LANG-1748). We will wait for the Java ecosystem to adapt to this change before upgrading again.

## Update

To update to Quarkus 3.13, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.13.

If you are not already using 3.13, please refer to the [3.13](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.13) migration guide.

## Full changelog

You can get [the full changelog of 3.13.3 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.13.3).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
