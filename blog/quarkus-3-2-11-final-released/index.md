# Quarkus 3.2.11.Final released - Maintenance LTS release

Quarkus 3.2.11.Final, the eleventh maintenance release of the 3.2 LTS release train has been released.

This release includes the following security-related fixes:

- [CVE-2024-25710](https://nvd.nist.gov/vuln/detail/CVE-2024-25710) Denial of service caused by an infinite loop for a corrupted DUMP file
- [CVE-2024-1597](https://nvd.nist.gov/vuln/detail/CVE-2024-1597) PostgreSQL JDBC Driver allows attacker to inject SQL if using PreferQueryMode=SIMPLE
- [CVE-2024-1023](https://access.redhat.com/security/cve/cve-2024-1023) memory leak due to the use of Netty FastThreadLocal data structures in Vertx
- [CVE-2024-1300](https://access.redhat.com/security/cve/CVE-2024-1300) memory leak when a TCP server is configured with TLS and SNI support
- [CVE-2024-1726](https://access.redhat.com/security/cve/CVE-2024-1726) security checks for some inherited endpoints performed after serialization in RESTEasy Reactive may trigger a denial of service

And the following component upgrades:

- Apache Commons Compress 1.25.0 -> 1.26.0
- PostgeSQL JDBC Driver 42.6.0 -> 42.6.1
- SmallRye JWT 4.3.0 -> 4.4.0
- Vert.X 4.4.6 -> 4.4.8

If you are not already using a 3.2 release, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2).

## Known issues include:

It should be a safe upgrade for anyone already using a 3.2.10.Final release.

## Full changelog

You can get [the full changelog of 3.2.11.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.2.11.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
