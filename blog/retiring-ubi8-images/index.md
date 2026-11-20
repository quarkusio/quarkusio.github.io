# Retiring community UBI 8 images for Quarkus

As you may know, Quarkus provides container images based on Red Hat Universal Base Image (UBI) both for building applications and for running them.

Since Quarkus 3.19, Quarkus is using UBI 9 as the default base image for container images. Given the oldest version we maintain in the community is 3.20 LTS, we have decided to retire the community UBI 8 images for Quarkus in March 2026.

We won't drop the images from our community Quay repository but they won't be updated anymore. Which means that CVE issues affecting UBI 8 won't be fixed in the community Quarkus UBI 8 images anymore.

If you are still using UBI 8 images for building or running your Quarkus applications, we strongly recommend you to migrate to the UBI 9-based images as soon as possible. If you have a very specific need to stay on UBI 8, please reach out to us.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

- provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues);
- craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls);
- discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev);
- ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
