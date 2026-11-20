# Quarkus 3.29 - Multiple cache backends and Qute DAP debugger support

Today, we released Quarkus 3.29.

Quarkus 3.29 introduces the following notable changes:

- [#50007](https://github.com/quarkusio/quarkus/pull/50007) - Add support for multiple cache backends
- [#48962](https://github.com/quarkusio/quarkus/pull/48962) - Provide Qute DAP debugger support
- [#48732](https://github.com/quarkusio/quarkus/pull/48732) - Keep the Hibernate ORM and Reactive extensions enabled even when no entity is defined

As usual, this release also includes many bug fixes and improvements.

## Update

To update to Quarkus 3.29, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.29.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.29 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.29).

## What's new?

### Cache

Until now, the Cache extension only supported using one caching backend at a time. For instance, it wasn't possible to use Caffeine for a given cache and Redis for another one.

With this release, you can now define multiple cache backends and associate each cache to a specific backend. You can find more information in the [Configuring the underlying caching provider](https://quarkus.io/guides/cache#configuring-the-underlying-caching-provider) section of the Cache guide.

### Qute DAP debugger support

The Qute extension now provides support for the Debug Adapter Protocol (DAP), which means that you can easily debug your Qute templates in your favorite IDE!

> [!TIP]
> You can read more about this new feature in the README file of the [qute-debug project](https://github.com/quarkusio/quarkus/tree/main/independent-projects/qute/debug).

### Hibernate ORM and Reactive

Both Hibernate ORM and Hibernate Reactive extensions are now enabled even when no entity is defined in your application.

## Full changelog

You can get the full changelog of [3.29.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.29.0.CR1) and [3.29.0](https://github.com/quarkusio/quarkus/releases/tag/3.29.0) on GitHub.

## Contributors

The Quarkus community is growing and has now [1131](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.29 release, thanks to Akulov S V, Ales Justin, Alex Martel, Alexey Loubyansky, Alon Gamliel, Andy Damevin, Aryant, Aurea Munoz, azerr, Bhawna, Bruno Baptista, Chris Laprun, Clement Escoffier, Daniel Cunha, Darko Janković, David M. Lloyd, Eli Barbosa, Eric Deandrea, Fedor Dudinsky, Foivos Zakkak, Fouad Almalki, Francesco Nigro, George Gastaldi, Georgios Andrianakis, Guillaume Smet, Holly Cummins, Houssam El Mansouri, Ioannis Canellos, Jakub Jedlicka, Jan Martiska, Jan Pohlmeyer, joonseolee, Julien Ponge, Juliusz Ćwiąkalski, Karm Michal Babacek, Katia Aresti, Ladislav Thon, Lars Andringa, Luca Molteni, Maciej Swiderski, Manuel Müller, Marco Belladelli, marko-bekhta, Martin Kouba, Matej Novotny, Matheus Cruz, Maximilian Zellhofer, Melloware, Michael Edgar, Michal Jurc, Michal Vavřík, Nathan Erwin, Neil J. L. Benn, Ozan Gunalp, Peter Palaga, Phillip Krüger, Pierre Beitz, Piotr Płaczek, Robert Kühne, Robert Stupp, Roberto Cortez, Roman Lovakov, Rostislav Svoboda, Sanne Grinovero, Sergey Beryozkin, Severin Gehwolf, Steve Hawkins, Stéphane Épardaud, Thomas Segismont, Tomas Hofman, xstefank, Yoann Rodière, Yukihiro Okada, yuokada, and zg-riband.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
