# Quarkus 3.0.0.Beta1 released

It is our pleasure to announce the availability of Quarkus 3.0.0.Beta1.

We plan to release Quarkus 3.0.0.CR1 next week so now is a good time for testing the migration of your applications to Quarkus 3 (see [below](#upgrading) for more information about upgrading).

Among a lot of bugfixes and small enhancements, the highlights of this release are:

- Hibernate Reactive and Camel Quarkus are back!
- Technical endpoints can be exposed on a specific management network interface
- Support for SDK autoconfiguration in OpenTelemetry extension

## What's new

### Hibernate Reactive

Hibernate Reactive has been reintroduced into Quarkus. Hibernate Reactive 2.0 is in Alpha and is based on Hibernate ORM 6.2.

### Camel Quarkus

Camel Quarkus is back in the Quarkus Platform, with a new major version compatible with Jakarta EE 10 and Quarkus 3.

Note that, while Quarkus 3 is still compatible with Java 11, the Camel Quarkus extensions require Java 17.

### Management network interface

Until now, all the Quarkus endpoints were exposed on the same network interface.

It is now possible to expose technical endpoints such as the health and metrics ones on a different interface thanks to [a specific management interface](https://quarkus.io/version/main/guides/management-interface-reference).

### OpenTelemetry

The OpenTelemetry extension has been rewritten to support the SDK autoconfiguration.

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

### Automated migration

For existing applications where all extensions are available, we have an OpenRewrite recipe that you can try.

There is a one-liner that attempts to do it automatically using a JBang script.

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
