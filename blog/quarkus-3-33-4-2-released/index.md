# Quarkus 3.33.4.2 released - LTS emergency release

Today, we released Quarkus 3.33.4.2, an emergency release for the 3.33 LTS stream.

This release fixes the following CVEs:

- [CVE-2026-107912](https://nvd.nist.gov/vuln/detail/CVE-2026-107912) - Quarkus Vert.x HTTP: Authorization bypass in the management interface due to incorrect combination of the management root path with permission rules
- [CVE-2026-110879](https://nvd.nist.gov/vuln/detail/CVE-2026-110879) - Jackson Databind: PolymorphicTypeValidator bypass via generic type parameters and array element types of type ids (incomplete fix for CVE-2026-54512)
- [CVE-2026-111497](https://nvd.nist.gov/vuln/detail/CVE-2026-111497) - Brotli (via Brotli4J): Heap out-of-bounds write in the Brotli decoder leading to remote code execution
- Everything else resolved by [Jackson 2.21.8](https://github.com/FasterXML/jackson/wiki/Jackson-Release-2.21.8) (CVEs in the Avro, CBOR, Protobuf and Smile dataformat modules, which are not part of Quarkus itself but are managed by the Quarkus BOM)

It should be a safe upgrade for anyone already using 3.33.

## Update

To update to Quarkus 3.33, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update --stream=3.33
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.33.

## Full changelog

You can get [the full changelog of 3.33.4.2 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/3.33.4.2).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

- provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues);
- craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls);
- discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev);
- ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
