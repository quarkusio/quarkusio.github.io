# Quarkus 2.6.2.Final released - Maintenance release

We just released Quarkus 2.6.2.Final, our second maintenance release for our 2.6 release train.

It is a safe upgrade for anyone already using 2.6 and contains small bugfixes, tweaks and documentation improvements. It also brings back a working Kogito stack. Due to some infrastructure problems, the Kogito team wasn't able to deliver a Kogito version working with Quarkus 2.6 before the holiday break but that's ancient history.

If you are not using 2.6 already, please refer to the [2.6 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.6).

## Kogito

Working Kogito and OptaPlanner are back in Quarkus 2.6.2.Final.

There is one change though: if you aren't doing so already, you will need to explicitly add the `quarkus-resteasy-jackson` extension to your Kogito and OptaPlanner applications.

We are working on supporting both RESTEasy Classic and RESTEasy Reactive with Kogito and OptaPlanner, this is the first step in this direction and this task will come to completion with Quarkus 2.7.

## Full changelog

You can get [the full changelog of 2.6.2.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.6.2.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
