# Quarkus 3.14.3 - Maintenance release, SBOM generation

We released Quarkus 3.14.3, a maintenance release for our 3.14 release train.

![quarkus 3.14.3](/assets/images/posts/3.14.3/quarkus-3.14.3.webp)

It contains several important bugfixes so we recommend the upgrade for anyone already using 3.14.

While our maintenance releases usually don't include new features, we made an exception here as the SBOM generation was frequently requested and people wanted it in 3.15 LTS.

Given part of the Quarkus dependencies are build time dependencies not present in the runtime classpath, some work was needed for SBOM generation to provide the complete picture of the Quarkus application dependencies.

You can find more information about SBOM generation in the [dedicated guide](https://quarkus.io/guides/cyclonedx). If you experiment with it, feedback is highly welcome!

With Quarkus 3.14.3, also comes Quarkus 3.15.0.CR1 which is based on the same code. Quarkus 3.15.0 core artifacts will be released next week for everyone to prepare the Platform and extensions. Quarkus 3.15.0 LTS release is planned for September 25th.

## Update

To update to Quarkus 3.14, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.14.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.14 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.14).

## Full changelog

You can get the full changelog of [3.14.3](https://github.com/quarkusio/quarkus/releases/tag/3.14.3) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
