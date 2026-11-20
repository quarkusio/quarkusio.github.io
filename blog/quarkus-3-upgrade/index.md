# Migration to Quarkus 3.0 is a breeze

Quarkus 3 is out and we encourage our users to upgrade their applications to experience the best Quarkus ever! The team worked hard to deliver a lot of new features and performance improvements. But of course, there are also some breaking changes. The [migration guide](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0) describes all the important changes in the core extensions.

Nevertheless, there is also some tedious work that needs to be done. Quarkus 3 has updated to the latest Jakarta EE 10 APIs, which include the infamous change of the `javax.\*` to the `jakarta.*` packages. If your application makes use of any Jakarta API, and there's a good chance it does (think about JAX-RS, CDI, JPA, etc.), you'll need to modify your code. Furthermore, if there's an explicit Jakarta API dependency in the project, you'll need to update this dependency as well.

Of course, you can migrate the application manually. But since Quarkus embraces the developer joy there is the Quarkus Update Tool to the rescue!

> [!TIP]
> This blog post was written just after the Quarkus 3.0 release. Things have evolved a bit since then.
>
> For more information about how to update to the latest Quarkus, have a look at the [dedicated guide](https://quarkus.io/guides/update-quarkus).
>
> You can also find all the migration guides in [our wiki](https://github.com/quarkusio/quarkus/wiki/Migration-Guides).

## Quarkus Update Tool - TL;DR

1. Install the [Quarkus CLI](https://quarkus.io/guides/cli-tooling) or make sure the latest version is installed
2. Run `quarkus update`
3. Check the changes made by the tool
4. Profit!

## Quarkus Update Tool - The Full Story

The goal of the update tool is to help developers to migrate an application to Quarkus 3 smoothly. The update process can be triggered from the Quarkus CLI, and directly via Maven or Gradle commands. The tool itself is basically a set of [OpenRewrite transformation rules](https://docs.openrewrite.org/). Most of the rules are defined in the [Quarkus Update Recipes](https://github.com/quarkusio/quarkus-updates) project.

What exacty does the update tool do?

- Updates the Quarkus version
- Transforms the source code to use the `jakarta.*` packages
- Upgrades the Quarkiverse extensions to the versions compatible with Quarkus 3.0
- Updates the Java EE API dependencies to the Jakarta EE API versions
- Adjusts some common dependencies (such as Jackson and Hibernate)
- Transforms the configuration files for some configuration properties (for example `quarkus.kubernetes.host` to `quarkus.kubernetes.ingress.host`)
- Renames some common service providers (for example `/META-INF/services/javax.ws.rs.ext.Providers` to `/META-INF/services/jakarta.ws.rs.ext.Providers`)

> [!TIP]
> Maven multi-module projects with standard layout are also supported if the parent project imports a Quarkus Platform BOM.

Let's get down to business. To run the update:

**CLI**

```
quarkus update
```

**Maven**

```
./mvnw io.quarkus.platform:quarkus-maven-plugin:3.9.3.Final:update -N
```

**Gradle**

```
./gradlew -PquarkusPluginVersion=3.9.3.Final quarkusUpdate
```

> [!IMPORTANT]
> If using [Quarkus CLI](https://quarkus.io/guides/cli-tooling) then always make sure the latest version is used. The `quarkus version` command should output version 3+.

Keep in mind that there is no dry-run mode. In other words, once you trigger the update, the changes will be immediately written to the file system. However, you can leverage the source code management tool to see the transformations applied. For example, if using Git then run the update tool and execute the `git diff` command afterwards to see the changes. And if something went wrong then just use `git checkout .` to revert the changes.

> [!TIP]
> If your application depends on Hibernate ORM, the dedicated [Hibernate ORM 5 to 6](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration) migration guide will come in handy.

That's it. Your application should be ready now. If there's something wrong please, ping us on Zulip or file a [new issue](https://github.com/quarkusio/quarkus/issues).

## Summary

In this article, we discussed the possibilities of the Quarkus Update Tool that will make your migration to Quarkus 3.0 a breeze. Don't hesitate and go for it!

> [!NOTE]
> The Quarkus Update Tool is not a one-time thing. The plan is to use this tool to provide easy updates for the future versions of Quarkus as well.

## More resources

- [Migration Guide 3.0](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0)
- [Migration Guide 3.0 - Hibernate ORM 5 to 6](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.0:-Hibernate-ORM-5-to-6-migration)
- [Quarkus Update Recipes](https://github.com/quarkusio/quarkus-updates)
- [Quarkus CLI](https://quarkus.io/guides/cli-tooling)
