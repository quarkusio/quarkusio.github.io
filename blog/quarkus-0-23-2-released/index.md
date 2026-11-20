# Quarkus 0.23.2 released - Back on track

Last week, we released [Quarkus 0.23.1](/blog/quarkus-0-23-1-released/) which paved the way towards our new Vert.x based HTTP layer and brought a lot of new features (see [the original announcement post](/blog/quarkus-0-23-1-released/) for all the details). We discovered a memory usage regression shortly after the release: it was not a memory leak or anything that bad but an increased memory usage due to Netty getting a bit too comfortable.

Today, we release 0.23.2, which contains all the great features of 0.23 and has better intentions on the memory side.

> [!NOTE]
> The recommended version of GraalVM is now 19.2.0.1 so [please upgrade](https://www.graalvm.org/downloads/)!

## What's new?

### Better Netty tuning

0.23 comes with the foundations of a brand new HTTP layer based on Vert.x and Netty. By default, Netty has a tendency to initialize a significant amount of memory at startup for performance reasons. This is obviously not ideal in highly constrained environments.

Another issue we had is that some Netty classes were initialized at build time and the hardware on which you built your application was used as the reference for Netty autotuning. This doesn't fly in a container world.

Both issues were fixed in 0.23.2.

### Full changelog

We also fixed a few bugs and usability issues: get [the full changelog of 0.23.2 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/0.23.2).

## Contributors

Quarkus has now [152 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for this release, thanks to Adam Bien, Andrej Petras, Clement Escoffier, George Gastaldi, Guillaume Smet, Jaikiran Pai, Jason T. Greene, Radim Vansa and Stuart Douglas.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
