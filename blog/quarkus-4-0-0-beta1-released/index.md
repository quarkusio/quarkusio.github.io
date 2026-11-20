# Quarkus 4.0.0.Beta1 released

It is our pleasure to announce the availability of Quarkus 4.0.0.Beta1, the first Beta of the next major version of Quarkus.

Quarkus 4 is a major new version and comes with a lot of changes. The final release is planned for the end of November. We need extension developers to migrate their extensions and end users to test their applications with it.

Your feedback at this stage is essential to make Quarkus 4 a great release, please report any issues you encounter on [GitHub](https://github.com/quarkusio/quarkus/issues).

## Highlights

Quarkus 4 comes with significant upgrades across the board, and more are coming:

**Java 21**\
New base Java version, making virtual threads more central.

**Netty 4.2 and Vert.x 5.x**\
New foundation for the networking layer.

**QUIC support**\
QUIC is becoming the new foundation of distributed systems: HTTP/3, MQTT over QUIC, WebTransport (WebSocket over QUIC)...

**HTTP/3**\
Opt-in support for HTTP/3 with automatic certificate generation in dev and test modes.

**Native transports**\
Improve the concurrency of your application with epoll or io_uring support. Also works in native executables.

**Observation API**\
`@Observed` or programmatic API to generate metrics and traces. Works with Micrometer and OpenTelemetry.

**Dev UI Observability dashboard**\
Built-in Observability section in the Dev UI, available out of the box in dev mode with no wiring needed.

**Jakarta REST 4**\
JSON Merge Patch, new multipart API.

**Hibernate ORM 8**\
Integration of the new major version of Hibernate with Jakarta Persistence 4.0 and Jakarta Data 1.1.

**Jackson 3**\
New major version - everything has changed except for the annotations.

**CDI 5.0**\
New annotations such as `@Reserve` and `@Eager`.

**New JMS extension**\
Dedicated JMS extension with compatibility with Spring JMS, pooled JMS, and IronJacamar.

**@ExactlyOnce**\
Improved Kafka transaction support.

## Upgrading to Quarkus 4

### Migration guide

We wrote a [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-4.0). As expected for a new major version, it is more dense than for our usual minors.

## For extension developers

If you are maintaining a Quarkus extension, now is the time to start migrating it to Quarkus 4. The [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-4.0) contains all the information you need.

Please reach out to us on [Zulip](https://quarkusio.zulipchat.com/) if you have any questions or need help with the migration.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
