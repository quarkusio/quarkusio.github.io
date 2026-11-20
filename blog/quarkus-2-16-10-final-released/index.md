# Quarkus 2.16.10.Final released - Maintenance release

As mentioned in previous blog posts, we encourage all our community users to upgrade to Quarkus 3. Most of the heavy lifting can be done with [`quarkus update`](https://quarkus.io/guides/update-quarkus) but be aware that some components were updated to new major versions and that migrating might require some time and careful testing if you are using these components.

We will continue to maintain 2.16.x until the end of October so we recommend that you start your migration process soon. Today, we released Quarkus 2.16.10.Final, the tenth maintenance release of our 2.16 release train.

The only change is the upgrade of Snappy to fix:

- [CVE-2023-34453](https://nvd.nist.gov/vuln/detail/CVE-2023-34453)
- [CVE-2023-34454](https://nvd.nist.gov/vuln/detail/CVE-2023-34454)
- [CVE-2023-34455](https://nvd.nist.gov/vuln/detail/CVE-2023-34455)

It should be a safe upgrade for anyone already using 2.16.

If you are not already using 2.16, please refer to [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-2.16).

## Full changelog

You can get [the full changelog of 2.16.10.Final on GitHub](https://github.com/quarkusio/quarkus/releases/tag/2.16.10.Final).

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
