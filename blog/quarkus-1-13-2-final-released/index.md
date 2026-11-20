# Quarkus 1.13.2.Final released - Oracle JDBC driver extension, bugfixes

We just released 1.13.2.Final, a new maintenance release for the 1.13 release train.

As usual, this release fixes bugs and includes some documentation improvements but it also introduces a new extension for the Oracle JDBC driver.

1.13.2.Final is a safe upgrade for everyone using Quarkus 1.13.

If you are not using 1.13 already, please refer to the [1.13 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.13).

## What's new?

### Oracle JDBC driver extension

While 1.13.2.Final is a maintenance release, we had some users asking for a backport of the brand new Oracle JDBC driver extension.

Given this work is self contained, it was judged safe enough to be included in this release.

To use the Oracle Database with Quarkus, in JVM mode and native mode, you can now include the following snippet in your `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-oracle</artifactId>
</dependency>
```

and use our standard datasource configuration infrastructure.

### Full changelog

You can get [the full changelog of 1.13.2.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/1.13.2.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
