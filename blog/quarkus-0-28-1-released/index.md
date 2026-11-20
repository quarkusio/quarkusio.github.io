# Quarkus 0.28.1 released - Some more bugfixes, help us squash 404s

This release is a bugfix/usability improvements release so the upgrade from 0.27.0 should be safe.

One new item is that we now have a notion of platform to refer to a set of extensions in the Quarkus ecosystem. Details below on how to use/try it out.

## What's new?

### New website theme

We just pushed a new theme for the website. We are still working on polishing it but, if you see something weird, please report it in [our tracker](https://github.com/quarkusio/quarkusio.github.io/issues).

### Guides and quickstarts URLs changes

Our guides URLs and our quickstarts directories were not entirely consistent until now. We changed them for more consistency.

We put redirections in place for the guides but we can't do it for the quickstarts directories so if you encounter any broken links on the website, please report them and we'll fix them right away.

Thanks!

### Enablement of platform in new and existing projects

Quarkus ecosystem is more than just the extensions released with Quarkus core. That is visible on [code.quarkus.io](https://code.quarkus.io) where you can find additional Quarkus extensions such as Apache Camel.

In your projects you refer to a part of the ecosystem as a platform - for now there is just one platform outside the `core` fondly named `universe`.

If you use [code.quarkus.io](https://code.quarkus.io) you will get it automatically.

If you create new projects you get it by passing `platformArtifactId=quarkus-universe-bom` and `platformVersion=0.28.1` to the project create:

```shell
mvn io.quarkus:quarkus-maven-plugin:0.28.1:create \
    -DprojectGroupId=my-groupId \
    -DprojectArtifactId=my-artifactId \
    -DprojectVersion=my-version \
    -DplatformArtifactId=quarkus-universe-bom \
    -DplatformVersion=0.28.1 \
    -DclassName="org.my.group.MyResource"
```

In existing projects, you for now have to manually update your Quarkus version as usual and then update the BOM reference from `<artifactId>quarkus-bom</artifactid>` to `<artifactId>quarkus-universe-bom></artifactId>`.

### Full changelog

You can get [the full changelog of 0.28.0 on GitHub](https://github.com/quarkusio/quarkus/releases/tag/0.28.0) and [the one from 0.28.1](https://github.com/quarkusio/quarkus/releases/tag/0.28.1).

## Contributors

Quarkus has now [177 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for this release, thanks to Alex Soto, Alexey Loubyansky, Aurea Munoz, Bill Burke, Clement Escoffier, Deven Phillips, Emmanuel Bernard, Georgios Andrianakis, Guillaume Smet, Gwenneg Lepage, Hector Ventura, Ioannis Canellos, Jaikiran Pai, Jan Martiska, Jason T. Greene, Ken Finnigan, Luca Burgazzoli, Logan Hauspie, Loïc Mathieu, Maciej Swiderski, Manyanda Chitimbo, Martin Kouba, Martin Stefanko, Matej Novotny, Max Rydahl Andersen, Michael Edgar, Pedro Igor, Ronak Patel, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Stuart Douglas, Syed M Shaaf, Tako Schotanus and Timothy Power.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
