# Quarkus 3.16.2 - Maintenance release

We released Quarkus 3.16.2, a maintenance release for our 3.16 release train.

In this version, we upgraded the PostgreSQL Dev Services to PostgreSQL 17, as older versions had issues on Mac Mx architecture. If it is a problem for your applications, you can go back to a previous image by setting the `quarkus.datasource.devservices.image-name=...` configuration property (or `quarkus.datasource."datasource-name".devservices.image-name=...` for a named datasource).

## Update

To update to Quarkus 3.16, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.16.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.16 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.16).

## Full changelog

You can get the full changelog of [3.16.2](https://github.com/quarkusio/quarkus/releases/tag/3.16.2) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
