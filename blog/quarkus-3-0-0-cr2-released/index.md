# Quarkus 3.0.0.CR2 released

Today, we released Quarkus 3.0.0.CR2, our last step before building the 3.0.0.Final bits.

Please try it with your applications, the update is easy in a lot of cases, and report any problem to us by [creating a GitHub issue](https://github.com/quarkusio/quarkus/issues).

To upgrade your application to Quarkus 3.0, see [the instructions below](#upgrading).

Among a lot of bugfixes and small enhancements, the highlights of this release are:

- Dev UI 2 is the default Dev UI
- Gradle project update

## What's new

### Dev UI 2 by default

Dev UI 2 is our brand new shiny Dev UI. It is now the default you obtain when going to `/q/dev` (but it's actually hosted at `/q/dev-ui`).

The old Dev UI is still accessible at `/q/dev-v1` and will be retired once we have ported all its features to Dev UI 2.

Yes, Dev UI 2 is not complete yet but it's already awesome so go test it and [report back](https://github.com/quarkusio/quarkus/issues).

### Gradle project update

The `quarkus update` command now supports the update of Gradle projects to Quarkus 3.

Have a Gradle project around? Give it a try and tell us [how it works](https://github.com/quarkusio/quarkus/issues).

## Trying out Quarkus 3

For now the easiest way to get started is using [code.quarkus.io](https://code.quarkus.io/?S=io.quarkus.platform%3A3.0) or use the Quarkus CLI:

```bash
quarkus create app --stream=3.0
```

Mind you that at this stage, while all the core extensions are available, only parts of the Quarkus platform is present. Not all extensions have yet migrated to Jakarta packages.

<a id="upgrading"></a>
## Upgrading to Quarkus 3

### Migration guide

As usual, we wrote a [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0). As expected for a new major, it is more dense than for our usual minors.

> [!TIP]
> The first step in a migration to Quarkus 3 is to run the migration script presented below.
>
> It doesn't take care of everything but it should take care of most of the heavy-lifting.

If you are using Hibernate ORM or Hibernate Reactive, please make sure you have a look to the [dedicated migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration).

<a id="automated-migration"></a>
### Automated migration

For existing applications where all extensions are available, we have an OpenRewrite recipe that you can try.

If you are the Quarkus CLI (which we recommend), update your Quarkus CLI to 3.0.0.CR2 and run the following command:

```bash
quarkus update --stream=3.0
```

This command will update your Maven and Gradle projects to the latest Quarkus 3.0.

If you are not using the CLI, you can update your Maven projects with a Maven command directly:

```bash
./mvnw io.quarkus.platform:quarkus-maven-plugin:3.0.0.CR2:update -N -Dstream=3.0
```

If the `update` command is not working for you, please [open a bug report](https://github.com/quarkusio/quarkus/issues).

And if you can't wait for the fix, you can try the following JBang script.

If you have JBang installed:

```bash
jbang --fresh upgrade-to-quarkus3@quarkusio
```

If not, for Linux and macOS:

```bash
curl -Ls https://sh.jbang.dev | bash -s - --fresh upgrade-to-quarkus3@quarkusio
```

and for Windows:

```bash
iex "& { $(iwr https://ps.jbang.dev) } --fresh upgrade-to-quarkus3@quarkusio"
```

Once this has been run your project should have its dependencies, source code and documentation updated to use Quarkus 3.

If it does not work for you, it could be we missed something or you are using extensions not yet supporting Quarkus 3.

[Let us know](https://github.com/quarkusio/quarkus/issues) in either case so we can together improve the migration script.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
