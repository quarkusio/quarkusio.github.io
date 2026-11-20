# Quarkus 3.2.7.Final released - Maintenance release

Today, we released Quarkus 3.2.7.Final, the seventh maintenance release of our 3.2 LTS release train.

This release fixes several CVEs:

- [CVE-2023-43642](https://nvd.nist.gov/vuln/detail/CVE-2023-43642), [CVE-2023-34454](https://nvd.nist.gov/vuln/detail/CVE-2023-34454) upgrading Snappy Java library version to 1.1.10.5
- [CVE-2023-39410](https://nvd.nist.gov/vuln/detail/CVE-2023-39410) upgrading Apache Avro version to 1.11.3
- [CVE-2023-44487](https://nvd.nist.gov/vuln/detail/CVE-2023-44487) upgrading Netty version to Netty to 4.1.100

And one bug:

- [RESTEasy Reactive fails to handle collections of parameterized types as parameter](https://github.com/quarkusio/quarkus/issues/35774)

It should be a safe upgrade for anyone already using 3.2.

If you are not already using 3.2, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2).

## Full changelog

You can get [the full changelog of 3.2.7.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.2.7.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
