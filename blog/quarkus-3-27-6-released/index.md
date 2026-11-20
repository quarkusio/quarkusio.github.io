# Quarkus 3.27.6 released - LTS maintenance release

Today, we released Quarkus 3.27.6, our next maintenance release for the 3.27 LTS stream.

This release contains bugfixes, documentation updates, and security fixes.

It should be a safe upgrade for anyone already using 3.27.

Please note that this is planned to be the last update to 3.27. Anybody still on 3.27 is advised to look into upgrading to 3.33 or 3.40 (the next LTS) soon.

## Security fixes

This release fixes the following CVEs:

### Quarkus and direct dependencies

- [CVE-2026-89407](https://nvd.nist.gov/vuln/detail/CVE-2026-89407) - FasterXML jackson-core: Denial of Service via quadratic regex backtracking in number validation
- [CVE-2026-89425](https://nvd.nist.gov/vuln/detail/CVE-2026-89425) - FasterXML jackson-core: Denial of Service via unbounded error-token accumulation in `UTF8DataInputJsonParser`
- [CVE-2026-91777](https://nvd.nist.gov/vuln/detail/CVE-2026-91777) - FasterXML jackson-databind: Denial of Service via quadratic-time forward-reference resolution for `@JsonIdentityInfo`
- [CVE-2026-91776](https://nvd.nist.gov/vuln/detail/CVE-2026-91776) - FasterXML jackson-databind: Denial of Service via unbounded deserializer cache growth for unrecognized polymorphic type IDs
- [CVE-2026-84939](https://nvd.nist.gov/vuln/detail/CVE-2026-84939) - Apache FreeMarker: Path traversal via malformed locale identifier
- [CVE-2026-94449](https://nvd.nist.gov/vuln/detail/CVE-2026-94449) - Quarkus SmallRye Fault Tolerance: Memory leak in `@ApplyGuard` leads to Denial of Service
- [CVE-2026-82617](https://nvd.nist.gov/vuln/detail/CVE-2026-82617) - Apache OpenNLP: Denial of Service via super-linear regex backtracking in built-in name finders
- [CVE-2026-68497](https://nvd.nist.gov/vuln/detail/CVE-2026-68497) - Jackson-databind: Denial of Service via unbounded number parsing for `Duration`/`XMLGregorianCalendar`
- [CVE-2026-89059](https://nvd.nist.gov/vuln/detail/CVE-2026-89059) - RESTEasy: Denial of Service via unbounded memory allocation in `IIOImageProvider`
- [CVE-2026-93432](https://nvd.nist.gov/vuln/detail/CVE-2026-93432) - Quarkus Qute: Cross-Site Scripting via missing content-type propagation in `{#eval}` sub-templates
- [CVE-2026-59949](https://nvd.nist.gov/vuln/detail/CVE-2026-59949) - LZ4 Java: Denial of Service via native XXHash crash on invalid byte array ranges
- [CVE-2026-61700](https://nvd.nist.gov/vuln/detail/CVE-2026-61700) - MariaDB Connector/J: `allowLocalInfile=false` bypass via server-initiated LOCAL INFILE request
- [CVE-2026-55856](https://nvd.nist.gov/vuln/detail/CVE-2026-55856) - MariaDB Connector/J: Cleartext password disclosure via MITM on the initial handshake
- [CVE-2026-55857](https://nvd.nist.gov/vuln/detail/CVE-2026-55857) - MariaDB Connector/J: Cleartext transmission of credentials during PAM authentication
- [CVE-2026-55858](https://nvd.nist.gov/vuln/detail/CVE-2026-55858) - MariaDB Connector/J: Data corruption via incorrect character set handling after a mid-session charset change

## Platform updates

This release includes [Quarkus CXF 3.27.2](https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.27.2.html).

## Update

To update to Quarkus 3.27, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update --stream=3.27
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.27.

## Full changelog

You can get [the full changelog of 3.27.6 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.27.6).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

- provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues);
- craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls);
- discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev);
- ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
