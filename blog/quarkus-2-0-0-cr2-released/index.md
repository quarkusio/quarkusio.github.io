# Quarkus 2.0.0.CR2 released - Towards Final

We just started the candidate release cycle for 2.0.0.Final.

Feedback is very welcome so if you have a Quarkus app around, please give it a go.

Make sure you update both the BOM version and the Quarkus Maven Plugin version to 2.0.0.CR2.

Also, we only publish the `quarkus-bom` for non-Final releases so make sure you use this BOM and not the `quarkus-universe-bom` for testing.

## Small issue with IDEs

We noticed a last minute issue with IDEs today related to a new annotation processor we introduced.

The workaround for now is to disable annotation processors in your IDE (that might not work very well if you're using others though).

For instance, for Eclipse users, you can add the `<m2e.apt.activation>disabled</m2e.apt.activation>` property in your POM file.

Anyway, this issue shouldn't prevent you from testing your existing applications and will be fixed in our Final release.

## Migration Guide

We keep the Migration Guide up to date here: <https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.0>.

## Documentation

The documentation for the `main` branch is available [on the website](/version/main/guides/).

### Full Changelog

You can get the full changelogs of [2.0.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/2.0.0.CR1) [2.0.0.CR2](https://github.com/quarkusio/quarkus/releases/tag/2.0.0.CR2) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
