# Quarkus 3.0.0.Alpha2 released - Second iteration of our Jakarta EE 10 stream

We already presented our Quarkus 3 effort [here](https://quarkus.io/blog/road-to-quarkus-3/), [here](https://quarkus.io/blog/our-bumpy-road-to-jakarta-ee-10/) and [here](https://quarkus.io/blog/quarkus-3-0-0-alpha1-released/).

Quarkus 3.0.0.Alpha2 is the continuation of this effort.

On the Jakarta EE 10 front, it doesn't bring anything new, several things are in the works to be integrated in the next Alpha, but it is based on 2.14.3.Final (the previous Alpha was based on 2.13.3.Final) so you get all the new features and bugfixes from Quarkus 2.14.

For more details, you can refer to the announcements for Quarkus [2.14.0.Final](https://quarkus.io/blog/quarkus-2-14-0-final-released/), [2.14.1.Final](https://quarkus.io/blog/quarkus-2-14-1-final-released/), [2.14.2.Final](https://quarkus.io/blog/quarkus-2-14-2-final-released/), and [2.14.3.Final](https://quarkus.io/blog/quarkus-2-14-3-final-released/).

The next Alpha will be released in about a month and will be based on Quarkus 2.15.3.Final.

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
