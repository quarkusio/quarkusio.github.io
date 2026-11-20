# Quarkus 3.0.0.Alpha1 released - First iteration of our Jakarta EE 10 stream

Last week, Max Andersen [explained our plans for Quarkus 3 and Jakarta EE 10](https://quarkus.io/blog/road-to-quarkus-3/).

I published yesterday a more detailed blog post explaining how we are building the Quarkus 3 stream.

It is now time to announce Quarkus 3.0.0.Alpha1 which is the first iteration of our Quarkus 3 stream.

A few key points about Quarkus 3.0.0.Alpha1:

- It is based on Quarkus 2.13.3.Final so it has all the features of this version.
- It targets Jakarta EE 10, except for JPA, where we are still aiming at EE 9. Thus we are still using Hibernate ORM 5.6.

From the key targets we have for Quarkus 3, some are not there yet:

- Eclipse MicroProfile 6 - it is not even released so no surprise there
- Switch to the Flow API instead of Reactive Streams - it is a work in progress
- As mentioned previously, Hibernate ORM 6

A few things are not working yet, due to upstream issues:

- REST Assured only works with Jackson, not with JSON-B, as there is no versions of REST Assured supporting the `jakarta.*` packages.

## Alpha?

We named this version Alpha1 because it is still a work in progress but it doesn't contain any experimental stuff.

It is a very stable `2.13.3.Final` converted to Jakarta EE 10 with some minor additions.

As a result, it should already be very usable.

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
