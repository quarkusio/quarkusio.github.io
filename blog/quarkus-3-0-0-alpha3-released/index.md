# Quarkus 3.0.0.Alpha3 released - Third iteration of our Jakarta EE 10 stream

As you might know by now, we started a Quarkus 3.0 effort last year and we are continuing this effort, which was described [here](https://quarkus.io/blog/road-to-quarkus-3/), [here](https://quarkus.io/blog/our-bumpy-road-to-jakarta-ee-10/), [here](https://quarkus.io/blog/quarkus-3-0-0-alpha1-released/), and [here](https://quarkus.io/blog/quarkus-3-0-0-alpha2-released/).

Quarkus 3.0.0.Alpha3 is the third iteration of this work.

On the Jakarta EE 10 front, it doesn't bring anything new, except for a few bugfixes and upgrades.

But it comes with some significant changes:

- This version is based on our current `main` branch: it contains all the refinements of [2.15.0.Final](https://quarkus.io/blog/quarkus-2-15-0-final-released/), [2.15.1.Final](https://quarkus.io/blog/quarkus-2-15-1-final-released/), [2.15.2.Final](https://quarkus.io/blog/quarkus-2-15-2-final-released/), [2.15.3.Final](https://quarkus.io/blog/quarkus-2-15-3-final-released/), and [2.16.0.Final](https://quarkus.io/blog/quarkus-2-16-0-final-released/).
- It upgrades Quarkus to [Mutiny 2](https://smallrye.io/smallrye-mutiny/2.0.0/reference/migrating-to-mutiny-2/) and the Java Flow API.
- It simplifies how Kotlin is handled by our classloader, which should ease the work on Kotlin-based Quarkus extensions.

We plan to release the next Alpha in about a month. Until then, we encourage you to test it and report your feedback.

## Trying out Quarkus 3

For now the easiest way to get started is using [code.quarkus.io](https://code.quarkus.io/?S=io.quarkus.platform%3A3.0) or use the Quarkus CLI:

```bash
quarkus create app --stream=3.0
```

Mind you that at this stage, while all the core extensions are available, only parts of the Quarkus platform is present. Not all extensions have yet migrated to Jakarta packages (e.g. Camel Quarkus or Kogito are not yet available).

## Upgrading to Quarkus 3

For existing applications where all extensions are available, we have an early OpenRewrite recipe that you can try.

There is a one-liner that attempts to do it automatically using a JBang script:

For Linux:

```bash
curl -Ls https://sh.jbang.dev | bash -s - --fresh upgrade-to-quarkus3@quarkusio
```

For Windows:

```bash
iex "& { $(iwr https://ps.jbang.dev) } --fresh upgrade-to-quarkus3@quarkusio"
```

> [!TIP]
> Using the JBang script also migrates the documentation (in Markdown on AsciiDoc).
>
> It is the preferred method as the one presented below will only migrate the source code.

You can also do it manually by downloading the [OpenRewrite recipe](https://raw.githubusercontent.com/quarkusio/quarkus/main/jakarta/quarkus3.yml) and apply it manually with the following Maven command:

```bash
curl -o quarkus3.yml https://raw.githubusercontent.com/quarkusio/quarkus/main/jakarta/quarkus3.yml
mvn org.openrewrite.maven:rewrite-maven-plugin:4.39.0:run \
   -Drewrite.configLocation=quarkus3.yml \
   -DactiveRecipes=io.quarkus.openrewrite.Quarkus3
```

> [!TIP]
> For multi-module projects, it is recommended to specify an absolute path in the `-Drewrite.configLocation` parameter so that the submodules can find the migration descriptor.

Once this has been run your project should have its dependencies and source code updated to use Quarkus 3.

If it does not work for you, it could be we missed something or you are using extensions not yet supporting Quarkus 3.

Let us know in either case so we can together improve the migration script.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
