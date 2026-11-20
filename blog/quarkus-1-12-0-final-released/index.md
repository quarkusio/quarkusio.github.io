# Quarkus 1.12 released - Fast jar as default, Multipart support for RESTEasy Reactive

It is my pleasure to announce the release of Quarkus 1.12.0.Final.

It comes with some significant changes:

- Fast jar is now the default packaging when you build a jar.
- RESTEasy Reactive keeps improving and it now has support for multipart.
- Vert.x Axle and RX Java managed instances have been dropped.
- We upgraded to GraalVM 21.0.

## Migration Guide

Eager to migrate your existing applications? Here is [our migration guide for 1.12](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.12).

## What's new?

### Fast jar as default

Quarkus has always offered several types of jar packaging. Historically, we had either a standard jar packaging (the one with the `-runner.jar` and the `lib/` directory) or an uber jar. In Quarkus 1.5, we introduced a new packaging type called Fast jar, which main objective was to further improve our boot time.

In Quarkus time, 1.5 means a century ago and we think Fast jar is now ready for prime time and brings enough improvements to justify the change. Thus the decision to make it the default starting with 1.12.

This change will have consequences on your workflow as the produced artifacts are now under a `quarkus-app` directory.

So instead of starting your application with:

```bash
java -jar target/my-application-runner-1.0.0-SNAPSHOT.jar
```

You will now start it with:

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

When deploying your application, you need to deploy the whole `quarkus-app` directory.

Your Dockerfiles will also need to be adjusted to take this into account.

You can find more details in our [Migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-1.12#fast-jar-as-default).

> [!TIP]
> If it's not the right time for you to change your workflow, you can easily go back to the previous packaging by adding the following property to your `application.properties`:
>
> ```properties
> quarkus.package.type=legacy-jar
> ```

### RESTEasy Reactive

We continue improving RESTEasy Reactive at a steady pace, and this time, we introduced Multipart support.

It is very similar to the Multipart support of RESTEasy and looks like:

```java
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FormData {

    @RestForm
    @PartType(MediaType.TEXT_PLAIN)
    public String description;

    @RestForm("image")
    public FileUpload file;
}
```

You can find more information about it in the updated [RESTEasy Reactive guide](https://quarkus.io/guides/resteasy-reactive#handling-multipart-form-data).

### GraalVM 21

The recommended version of GraalVM for Quarkus 1.12 is GraalVM 21.0.

### Vert.x Axle and RX Java managed instances gone

They had been deprecated for quite a while, they are now gone.

Either use the raw Vert.x instance of the Mutiny one.

### Full Changelog

You can get the full changelogs of [1.12.0.CR1](https://github.com/quarkusio/quarkus/releases/tag/1.12.0.CR1), and [1.12.0.Final](https://github.com/quarkusio/quarkus/releases/tag/1.12.0.Final) on GitHub.

Edit: We discovered some issues with the `/q` namespace introduced in this release that are described [here](/blog/path-resolution-in-quarkus/). These issues were resolved in `1.12.1.Final`.

## ADOPTERS.md

To share stories about Quarkus usage, we added [an `ADOPTERS.md` file](https://github.com/quarkusio/quarkus/blob/master/ADOPTERS.md) at the root of the repository.

If you are using Quarkus and would like to be interviewed on our blog or simply added to this file, please contact us and we will be happy to oblige.

## Contributors

The Quarkus community is growing and has now [445 contributors](https://github.com/quarkusio/quarkus/graphs/contributors). Many many thanks to each and everyone of them.

In particular for the 1.12 release, thanks to Alex Soto, Alexey Loubyansky, Andy Damevin, artsemstones, Auri Munoz, Bernhard Lutzmann, Bill Burke, Clement Escoffier, Erin Schnabel, Ertunc Aybars, essobedo, Falko Modler, Florian Heubeck, Foivos Zakkak, Galder Zamarreño, Gaétan Collaud, George Gastaldi, Georgios Andrianakis, Guillaume Le Floch, Guillaume Smet, Gwenneg Lepage, Henrique Prange, Ioannis Canellos, Jaikiran Pai, James Netherton, Jan Martiška, JiriOndrusek, Johnnes Souza, Joost Baas, Jose, Jose Ignacio Gil Jaldo, Josef Smrcka, Julien Ponge, Katia Aresti, Ken Finnigan, Knut Wannheden, kstrempel, Ladislav Thon, Loïc Mathieu, Lucas Reeh, Maciej Walkowiak, Mahieddine Cherif, Maik Möwes, manusa, Manyanda Chitimbo, Martin Kouba, Martin Panzer, Matej Novotny, Matej Vasek, Maxime DAVID, Mayank Kunwar, Michał Szynkiewicz, Moritz Becker, mrizzi, Nuno Alves, Oscar, PAPADOPOULOS Nikolaos, Peter Palaga, Petr Široký, Phillip Krüger, René Grob, Robert Stupp, Roberto Cortez, Rostislav Svoboda, Sanne Grinovero, SaumyaSingh1, sebthom, Sergey Beryozkin, S.H, Stuart Douglas, Stéphane Épardaud, Suleimenov Yelzhas, Ted Won, Thomas McWork, Thomas Pfaff, Thomas Segismont, Timothy Power, Tobias Stadler, Vasilis Andritsoudis, Vincent Sevel, xstefank, and Yoann Rodière.

## Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

\* provide feedback on [GitHub](https://github.com/quarkusio/quarkus/issues); \* craft some code and [push a PR](https://github.com/quarkusio/quarkus/pulls); \* discuss with us on [Zulip](https://quarkusio.zulipchat.com/) and on the [mailing list](https://groups.google.com/d/forum/quarkus-dev); \* ask your questions on [Stack Overflow](https://stackoverflow.com/questions/tagged/quarkus).
