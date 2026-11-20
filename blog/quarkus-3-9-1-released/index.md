# Quarkus 3.9 - Big Reactive Rename

It is with great pleasure that we are announcing the release of Quarkus 3.9.1.

This development cycle for this release was a bit longer than usual as it contains all the new features since we branched Quarkus 3.7.

Quarkus 3.9 is for developers who want the latest features, if you are looking for an extended support cycle, you are encouraged to stay on 3.8 LTS.

Here are the main changes for 3.9:

- [#39159](https://github.com/quarkusio/quarkus/pull/39159) and [#39301](https://github.com/quarkusio/quarkus/pull/39301) - Big Reactive Rename
- [#39142](https://github.com/quarkusio/quarkus/pull/39142) - Initial version of the new declarative WebSocket server API
- [#34493](https://github.com/quarkusio/quarkus/pull/34493) - Add CLI command for Config
- [#39389](https://github.com/quarkusio/quarkus/pull/39389) - Update to Infinispan 15
- [#37680](https://github.com/quarkusio/quarkus/pull/37680) - Welcome page now dynamic
- [#38541](https://github.com/quarkusio/quarkus/pull/38541) - Support OIDC Client JWT Bearer authentication
- [#38603](https://github.com/quarkusio/quarkus/pull/38603) - Introduce `@ClientBasicAuth`\` annotation
- [#38608](https://github.com/quarkusio/quarkus/pull/38608) - Allow TLS certificate reloading for the HTTP server
- [#39206](https://github.com/quarkusio/quarkus/pull/39206) - Improve graceful shutdown

## Update

To update to Quarkus 3.9, we recommend updating to the latest version of the Quarkus CLI and run:

```bash
quarkus update
```

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus 3.9.

For more information about the adjustments you need to make to your applications, please refer to the [Quarkus 3.9 migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9).

If you are not already using 3.x, please refer to the [3.0 announcement](https://quarkus.io/blog/quarkus-3-0-final-released/) for all the details. You can also refer to [this blog post](https://quarkus.io/blog/quarkus-3-upgrade/) for additional details. Once you upgraded to 3.0, also have a look at the [3.1](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.1), [3.2](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.2), [3.3](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.3), [3.4](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.4), [3.5](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.5), and [3.6](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.6), [3.7](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.7), [3.8](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.8), and [3.9](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9) migration guides.

## What's new?

### The Big Reactive Rename

Max Andersen already presented this initiative in [this blog post](https://quarkus.io/blog/the-big-rename/).

The idea behind this substantive change is to clarify which extensions are reactive only and which extensions are actually handling both blocking and reactive workloads equally well.

That's why we renamed several extensions, and why RESTEasy Reactive is now branded as Quarkus REST and SmallRye Reactive Messaging as Messaging. Note that this change also concerns the related extension so there are quite a lot of changes.

We didn't do it lightly: the old names were generating a lot of confusion for our users.

The good news is that, in most cases, it should be transparent for you as we put relocations in place, both for artifacts and configuration properties. However we encourage you to move to the new names as soon as you can.

When using `quarkus update`, the update should be transparent for you. If you are not using `quarkus update`, we recommend to follow the dedicated section of [our migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9).

We have a known issue for this change: if you are using `quarkus.resteasy-reactive.path`/`quarkus.rest.path`, you will get a warning saying the property is not recognized. Don't panic, it is actually recognized, the warning is a mistake. This problem will be solved in 3.9.2.

One common issue you might have when updating manually can come from the fact that we reused the `quarkus-rest-client` name for the Quarkus REST implementation, whereas in pre-3.7, it was used for the RESTEasy Classic implementation.

A bit of history might help:

- In Quarkus 3.7, we renamed `quarkus-rest-client`, based on the RESTEasy Classic to `quarkus-resteasy-client` to make it consistent with `quarkus-resteasy` and pave the way for our future changes. We put a relocation in place so that it wouldn't break your applications. You can refer to the [Quarkus 3.7 migration guide for more information](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.7#rest-client).
- In Quarkus 3.9, we renamed `quarkus-rest-client-reactive` to `quarkus-rest-client` as part of our move to the Quarkus REST branding.

So if your application can't start with an error saying there are duplicate implementations related to REST, just check that you are using consistently either the `quarkus-rest*` extensions or the `quarkus-resteasy*` extensions.

### Welcome page

The default welcome page you get when starting a Quarkus application in dev mode is now dynamic (and prettier!).

### WebSocket Next

Quarkus 3.9 comes with a next generation WebSocket extension, called `quarkus-websockets-next` (the name is temporary as you can imagine).

If you are using WebSockets in your applications, we recommend that you give it a try as it is the future of WebSockets support in Quarkus.

You can find more information about it in the [tutorial](https://quarkus.io/guides/websockets-next-tutorial) and the [reference guide](https://quarkus.io/guides/websockets-next-reference).

### CLI command for config

Commands were added to the Quarkus CLI to help managing configuration:

- `quarkus config set` allows to update configuration properties
- `quarkus config encrypt` allows to encrypt values that you want to keep encrypted in your configuration files

### Security enhancements

This new version comes with several improvements related to security:

- [#38541](https://github.com/quarkusio/quarkus/pull/38541) - Support OIDC Client JWT Bearer authentication
- [#38603](https://github.com/quarkusio/quarkus/pull/38603) - Introduce `@ClientBasicAuth`\` annotation
- [#38608](https://github.com/quarkusio/quarkus/pull/38608) - Allow TLS certificate reloading for the HTTP server (see the [documentation](https://quarkus.io/guides/http-reference#reloading-the-certificates) for more details)

### Infinispan 15

We updated to the latest and greatest Infinispan 15.

You might have to apply some changes to your applications, so, if you are using the Infinispan extension, have a look at the [dedicated section of the migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.9#update-to-infinispan-15).

### Graceful shutdown

Some improvements were made to our graceful shutdown support.

## Full changelog

You can get the full changelog of [3.9.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/3.9.0.CR1), [3.9.0.CR2](https://github.com/quarkusio/quarkus/releases/tag/3.9.0.CR2), [3.9.0](https://github.com/quarkusio/quarkus/releases/tag/3.9.0), and [3.9.1](https://github.com/quarkusio/quarkus/releases/tag/3.9.1) on GitHub.

## Contributors

The Quarkus community is growing and has now [925 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 3.9 release, thanks to Ales Justin, Alex Katlein, Alex Martel, Alexander Schwartz, Alexey Loubyansky, Andy Damevin, Anton-Vasilev, arvind-vignesh, Auri Munoz, avivmu, barreiro, Bas Passon, Benedikt Werner, Brahim Raddahi, Bruno Baptista, Bruno Leonardo, Chris Laprun, Christian Thiel, cknoblauch, Clement Escoffier, Daniel Bobbert, David M. Lloyd, Davide D'Alto, Diego Ramp (u804103), Dimitris Kontokostas, dliubars, ennishol, Erin Schnabel, Falko Modler, fdlane, Foivos Zakkak, Francesco Nigro, Galder Zamarreño, Gasper Kojek, George Gastaldi, Georgios Andrianakis, glefloch, Gonçalo Montalvão Marques, Guillaume Smet, Hendrik Schmitz, Holly Cummins, humberto, Idryss Bourdier, Ioannis Canellos, Jakub Jedlicka, James Netherton, Jan Martiska, Jiří Locker, Julien Ponge, Katia Aresti, Ladislav Thon, Leandro Quiroga, Loïc Mathieu, Luke Morfill, luneo7, Maciej Lisowski, Marcel Stör, Marco Bungart, Marco Sappé Griot, Marek Skacelik, mariofusco, marko-bekhta, martin, Martin Kouba, Matej Novotny, Matheus Cruz, Max Rydahl Andersen, Michael Edgar, Michal Vavřík, Michiel Thomassen, nimo23, ObserverOfTime, Oliver Wiebeck, Ozan Gunalp, Paulo Casaes, Peter Palaga, Phillip Krüger, Pierre Adam, Robert Stupp, Roberto Cortez, Rolfe Dlugy-Hegwer, Romain BADINO, Rostislav Svoboda, Sanne Grinovero, Sebastian Schuster, Selim, Sergey Beryozkin, Severin Gehwolf, SpaceFox, Steve Hawkins, stianst, Stuart Douglas, Stéphane Épardaud, Sébastien ALLEMAND, The-Huginn, Thomas Darimont, troosan, ub003, Vitaliy Baschlykoff, w0pp, Waldemar Reusch, Wesley Salimans, Wladimir Hofmann, xstefank, Yassin Hajaj, Yoann Rodière, and Yoshikazu Nojima.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
