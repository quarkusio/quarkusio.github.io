# Quarkus 3.27.5.1 released - LTS emergency release

Today, we released Quarkus 3.27.5.1, an emergency release for the 3.27 LTS stream.

This release fixes the following CVE:

- [CVE-2026-16308](https://nvd.nist.gov/vuln/detail/CVE-2026-16308) - Quarkus REST denial of service via unbounded multipart MIME part-header accumulation

It also fixes a native image regression introduced by the Netty upgrade in 3.27.5 and upgrades the PostgreSQL JDBC driver to 42.7.13.

It should be a safe upgrade for anyone already using 3.27.

## Update

To update to Quarkus 3.27, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update --stream=3.27
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.27.

## Full changelog

You can get [the full changelog of 3.27.5.1 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.27.5.1).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

- provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues);
- craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls);
- discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev);
- ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
