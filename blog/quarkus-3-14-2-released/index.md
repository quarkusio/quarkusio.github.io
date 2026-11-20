# Quarkus 3.14.2 - Maintenance release and Micrometer 1.13

We released Quarkus 3.14.2, a maintenance release for our 3.14 release train.

It contains several important bugfixes so we recommend the upgrade for anyone already using 3.14.

While our maintenance releases usually don't include important changes, we had to make an exception for the upgrade to Micrometer 1.13.

Our main issue was that Micrometer 1.12 would reach EOL in a few months, which wasn't in line with the upcoming 3.15 LTS release. As previously explained, 3.15 will be based the continuation of the 3.14 branch, thus why we included this update in 3.14.

The [3.14 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.14#micrometer) has been updated to reflect that.

## Update

To update to Quarkus 3.14, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.14.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.14 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.14).

## Full changelog

You can get the full changelog of [3.14.2](https://github.com/quarkusio/quarkus/releases/tag/3.14.2) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
