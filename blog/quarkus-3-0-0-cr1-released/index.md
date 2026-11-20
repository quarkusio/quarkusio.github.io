# Quarkus 3.0.0.CR1 released

It is our pleasure to announce the availability of Quarkus 3.0.0.CR1.

We are working full steam on polishing Quarkus 3.0 so please try it with your applications and report any problem to us by [creating a GitHub issue](https://github.com/quarkusio/quarkus/issues).

To upgrade your application to Quarkus 3.0, see [the instructions below](#upgrading).

Among a lot of bugfixes and small enhancements, the highlights of this release are:

- The introduction of the `/q/info` endpoint exposing information about your application
- A `quarkus update` command based on our OpenRewrite upgrade recipes
- A new Redis backend for the Cache extension
- The support for Maven 3.9
- CLI plugins
- Support for multiple mailers

## What's new

### /q/info

To expose information about your application (such as the git hash), add the `quarkus-info` extension to your project.

The endpoint is available on `/q/info` and will be exposed on the management network interface if you enable it.

### quarkus update

Until now, the migration of your project had to be done via a JBang script but the update is now embedded in the Quarkus tooling (using the same OpenRewrite recipes).

See the [Automated migration](#automated-migration) section below for more information.

### Redis backend for Cache extension

It is now possible to use a Redis backend with the Cache extension.

More information in the [dedicated guide](https://quarkus.io/version/main/guides/cache-redis-reference).

### Support for Maven 3.9

Maven 3.9 came with some breaking changes and the integration with Quarkus was not working properly. It is now fixed and you can use Maven 3.9 to build your Quarkus 3.0.0.CR1 applications.

### CLI plugins

The Quarkus CLI was made extensible with plugins. Expect a lot of new features in the Quarkus CLI in the form of plugins in the near future.

### Multiple mailers

Sending emails via several SMTP servers is supported in Quarkus 3.0.0.CR1.

Have a look at the [updated documentation](https://quarkus.io/version/main/guides/mailer-reference#multiple-mailer-configurations).

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

If you are using Maven and the Quarkus CLI, update your Quarkus CLI to 3.0.0.CR1 and run the following command:

```bash
quarkus update --stream=3.0
```

If you are not using the CLI, you can use a Maven command directly:

```bash
./mvnw io.quarkus.platform:quarkus-maven-plugin:3.0.0.CR1:update -N -Dstream=3.0
```

If you are using Gradle, the migration is not yet embedded in the Quarkus tooling but we have a JBang script for you:

If you have JBang already installed, run:

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
