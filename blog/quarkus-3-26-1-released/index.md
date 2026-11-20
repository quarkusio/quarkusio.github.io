# Quarkus 3.26.1, 3.20.2.2 and 3.15.6.2 - Emergency releases

Today, we released Quarkus 3.26.1, 3.20.2.2 and 3.15.6.2 to fix an important regression introduced in Vert.x 4.5.18.

A regression in Vert.x introduced in 4.5.18 can lead to a pool HTTP client connection that does not have a correct state and stop making progress when receiving bytes, so the application will not observe the entirety of the HTTP response and therefore hang when receiving the data. It also means that clients of the library might not obtain a connection in a timely manner.

It has been [fixed](https://github.com/eclipse-vertx/vert.x/pull/5683) in Vert.x 4.5.20 and we decided to push emergency releases for our current and LTS streams to promptly address the issue.

We recommend upgrading to these releases without delay if you are using a Quarkus version based on Vert.x 4.5.18 (i.e. 3.26.0, 3.25.4, 3.20.2.1, and 3.15.6.1).

Quarkus 3.26.1 also contains some other unrelated fixes.

## Update

To update to Quarkus 3.26, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.26.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.26 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.26).

## Full changelog

You can get the full changelog of [3.26.1](https://github.com/quarkusio/quarkus/releases/tag/3.26.1) on GitHub.

3.20.2.2 and 3.15.6.2 only contain the Vert.x update to 4.5.20.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
