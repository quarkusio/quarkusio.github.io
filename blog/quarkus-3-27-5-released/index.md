# Quarkus 3.27.5 released - LTS maintenance release

Today, we released Quarkus 3.27.5, our next maintenance release for the 3.27 LTS stream.

This release contains bugfixes, documentation updates, and security fixes.

It should be a safe upgrade for anyone already using 3.27.

## Security fixes

This release fixes the following CVEs:

### Quarkus and direct dependencies

- [CVE-2026-15075](https://nvd.nist.gov/vuln/detail/CVE-2026-15075) - Eclipse Vert.x: `DefaultRedirectHandler` cross-origin header propagation
- [CVE-2026-15076](https://nvd.nist.gov/vuln/detail/CVE-2026-15076) - Eclipse Vert.x: `WebClientSession` cross-domain cookie injection
- [CVE-2026-53712](https://nvd.nist.gov/vuln/detail/CVE-2026-53712) - OnGres SCRAM client: Authentication downgrade
- [CVE-2026-59888](https://nvd.nist.gov/vuln/detail/CVE-2026-59888) - Jackson-databind: `@JsonIgnore` bypass with `PropertyNamingStrategy` on Java Records
- [CVE-2026-59889](https://nvd.nist.gov/vuln/detail/CVE-2026-59889) - Jackson-databind: `@JsonView` bypass for `@JsonUnwrapped` properties during deserialization
- [CVE-2026-8484](https://nvd.nist.gov/vuln/detail/CVE-2026-8484) - Jansi: Heap-based buffer overflow in JNI ioctl() wrapper
- [CVE-2026-55405](https://nvd.nist.gov/vuln/detail/CVE-2026-55405) - LangChain4j: SQL injection in embedding store metadata filter

### Netty

This release upgrades Netty to 4.1.136.Final, which fixes numerous security vulnerabilities including:

- [CVE-2026-55833](https://nvd.nist.gov/vuln/detail/CVE-2026-55833) - Zip bomb vulnerability in `netty-codec-http`
- [CVE-2026-59921](https://nvd.nist.gov/vuln/detail/CVE-2026-59921) - Improper CR/LF neutralization in `netty-codec-http` (multipart)
- [CVE-2026-59919](https://nvd.nist.gov/vuln/detail/CVE-2026-59919) - Improper CR/LF neutralization in `netty-codec-haproxy`
- [CVE-2026-55851](https://nvd.nist.gov/vuln/detail/CVE-2026-55851) - Memory exhaustion in `netty-codec-haproxy`
- [CVE-2026-56745](https://nvd.nist.gov/vuln/detail/CVE-2026-56745) - Memory exhaustion in `netty-codec-http`
- [CVE-2026-59899](https://nvd.nist.gov/vuln/detail/CVE-2026-59899) - Memory exhaustion in `netty-codec-http`
- [CVE-2026-55831](https://nvd.nist.gov/vuln/detail/CVE-2026-55831) - Resource exhaustion/DoS in `netty-codec-http`
- [CVE-2026-56819](https://nvd.nist.gov/vuln/detail/CVE-2026-56819) - Memory leak in `netty-codec-http2`
- [CVE-2026-59900](https://nvd.nist.gov/vuln/detail/CVE-2026-59900) - Improper header neutralization in `netty-codec-http2`
- [CVE-2026-59898](https://nvd.nist.gov/vuln/detail/CVE-2026-59898) - Protocol version confusion in `netty-codec-http` (websocket)
- [CVE-2026-56746](https://nvd.nist.gov/vuln/detail/CVE-2026-56746) - Improper access control in `netty-codec-http` (CORS)

For the full list, see the [Netty 4.1.136.Final release announcement](https://netty.io/news/2026/07/09/4-1-136-Final.html).

## Update

To update to Quarkus 3.27, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update --stream=3.27
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.27.

## Full changelog

You can get [the full changelog of 3.27.5 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.27.5).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

- provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues);
- craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls);
- discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev);
- ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
