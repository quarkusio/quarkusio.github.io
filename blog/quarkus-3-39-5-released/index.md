# Quarkus 3.39.5 - Maintenance release

Today, we released Quarkus 3.39.5, a maintenance release for our 3.39 release train.

This release contains bugfixes and documentation improvements. It should be a safe upgrade for anyone already using 3.39.

This release fixes the following CVEs:

- [CVE-2026-77874](https://nvd.nist.gov/vuln/detail/CVE-2026-77874) - Hibernate ORM: SQL Injection via unescaped JSON path segment allows data exfiltration and authorization bypass
- [CVE-2026-19611](https://nvd.nist.gov/vuln/detail/CVE-2026-19611) - WildFly Elytron: Password keyspace reduction via NFKC fullwidth folding
- [CVE-2026-81829](https://nvd.nist.gov/vuln/detail/CVE-2026-81829) - SmallRye JWT: Unauthenticated same-origin SSRF via unsanitized JWT kid header in AwsAlbKeyResolver
- [CVE-2026-87742](https://nvd.nist.gov/vuln/detail/CVE-2026-87742) - Quarkus WebSockets Next: Denial of Service (OOM) via unbounded message buffering
- [CVE-2026-87743](https://nvd.nist.gov/vuln/detail/CVE-2026-87743) - Quarkus Vert.x HTTP: Authorization bypass via path normalization discrepancy in Quarkus HTTP security
- [CVE-2026-87825](https://nvd.nist.gov/vuln/detail/CVE-2026-87825) - zstd-jni: Data corruption or denial of service via use-after-free vulnerability
- [CVE-2026-87877](https://nvd.nist.gov/vuln/detail/CVE-2026-87877) - zstd-jni: Use-after-free vulnerability allows memory corruption and denial of service
- [CVE-2026-87795](https://nvd.nist.gov/vuln/detail/CVE-2026-87795) - zstd-jni: Out-of-bounds read in ZstdDictCompress constructor leads to denial of service
- [CVE-2026-87824](https://nvd.nist.gov/vuln/detail/CVE-2026-87824) - zstd-jni: Denial of Service via out-of-bounds read in Zstd.trainFromBufferDirect
- [CVE-2026-89046](https://nvd.nist.gov/vuln/detail/CVE-2026-89046) - zstd-jni: Information disclosure or denial of service via out-of-bounds read
- [CVE-2026-87823](https://nvd.nist.gov/vuln/detail/CVE-2026-87823) - zstd-jni: Denial of Service or information disclosure via out-of-bounds read
- [CVE-2026-90560](https://nvd.nist.gov/vuln/detail/CVE-2026-90560) - zstd-jni: Denial of Service via out-of-bounds read in ZstdDictDecompress
- [CVE-2026-90852](https://nvd.nist.gov/vuln/detail/CVE-2026-90852) - zstd-jni: Remote use-after-free vulnerability in dictionary sharing
- [CVE-2026-89045](https://nvd.nist.gov/vuln/detail/CVE-2026-89045) - zstd-jni: Denial of Service via negative length parameter
- [CVE-2026-84939](https://nvd.nist.gov/vuln/detail/CVE-2026-84939) - Apache FreeMarker: Path traversal via malformed locale identifier
- [CVE-2026-94449](https://nvd.nist.gov/vuln/detail/CVE-2026-94449) - SmallRye Fault Tolerance: Memory leak in @ApplyGuard leads to Denial of Service
- [CVE-2026-93491](https://nvd.nist.gov/vuln/detail/CVE-2026-93491) - Netty: Denial of Service via unbounded HttpServerCodec HTTP/1.1 pipeline queue
- [CVE-2026-93558](https://nvd.nist.gov/vuln/detail/CVE-2026-93558) - Netty: Unbounded per-connection queue growth in WebSocketServerExtensionHandler leads to Denial of Service
- [CVE-2026-93564](https://nvd.nist.gov/vuln/detail/CVE-2026-93564) - Netty: HAProxy PROXY-v2 nested-TLV grandchild ByteBuf reference-count leak
- [CVE-2026-93565](https://nvd.nist.gov/vuln/detail/CVE-2026-93565) - Netty: RtspDecoder method-token smuggling via trailing control byte
- [CVE-2026-93567](https://nvd.nist.gov/vuln/detail/CVE-2026-93567) - Netty: HTTP/1 authority-form CONNECT is translated to malformed HTTP/2 CONNECT with Host-controlled :authority
- [CVE-2026-93568](https://nvd.nist.gov/vuln/detail/CVE-2026-93568) - Netty: HTTP/2 and HTTP/3 Extended CONNECT requests are downgraded as regular CONNECT requests
- [CVE-2026-93569](https://nvd.nist.gov/vuln/detail/CVE-2026-93569) - Netty: HTTP/1 absolute-form Host mismatch is translated to HTTP/2 :authority, overriding the request-target authority
- [CVE-2026-93492](https://nvd.nist.gov/vuln/detail/CVE-2026-93492) - Netty: HTTP/2 HpackEncoder Denial of Service with large table size
- [CVE-2026-93562](https://nvd.nist.gov/vuln/detail/CVE-2026-93562) - Netty: Incomplete validation of malformed Transfer-Encoding allows HTTP request smuggling
- [CVE-2026-93566](https://nvd.nist.gov/vuln/detail/CVE-2026-93566) - Netty: HTTP request smuggling due to control characters in the chunk-size line
- [CVE-2026-93573](https://nvd.nist.gov/vuln/detail/CVE-2026-93573) - Netty: Split Transfer-Encoding fields bypass final-chunked validation and enable request smuggling
- [CVE-2026-93574](https://nvd.nist.gov/vuln/detail/CVE-2026-93574) - Netty: HTTP request smuggling via post-digit whitespace in chunk-size parsing
- [CVE-2026-93579](https://nvd.nist.gov/vuln/detail/CVE-2026-93579) - Netty: HTTP/2 header field values are not validated by default (CR/LF/NUL passthrough)
- [CVE-2026-89058](https://nvd.nist.gov/vuln/detail/CVE-2026-89058) - RESTEasy: CorsFilter Reflects Arbitrary Origin with Credentials under Wildcard Config
- [CVE-2026-89059](https://nvd.nist.gov/vuln/detail/CVE-2026-89059) - RESTEasy: IIOImageProvider Unbounded Image Decode (Decompression-Bomb DoS)
- [CVE-2026-93432](https://nvd.nist.gov/vuln/detail/CVE-2026-93432) - Qute: Cross-Site Scripting (XSS) and JSON Injection via Qute {#eval} Section in Quarkus

Yes, that's a lot of them, AI discovers a lot of CVEs these days, and Quarkus aggregates a lot of components.

## Update

To update to Quarkus 3.39, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.39.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.39 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.39).

## Full changelog

You can get the full changelog of [3.39.5](https://github.com/quarkusio/quarkus/releases/tag/3.39.5) on GitHub.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
