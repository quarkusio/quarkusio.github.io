---
layout: usage
title: Quarkus Usage Analytics
subtitle: Help us make Quarkus even better, anonymously.
permalink: /usage/
---

## Why does Quarkus want to gather usage analytics?

Usage analytics (telemetry collection) is invaluable for the Quarkus team and contributors to gauge which operating systems, java version, build systems, extensions and more are used. This service is provided by Red Hat and the details can be found on [usage policy](/usage/policy) page.

## How will this work?

In order to get this information, beginning in Quarkus 3.2, when you run Quarkus the first time in dev mode (`quarkus dev`, `mvn quarkus:dev`, etc.) you get asked if you agree to contribute anonymous build data to the Quarkus community.

![/assets/images/usage-prompt-arrows.png](/assets/images/usage-prompt-arrows.png)

By answering **"Yes"**, when you perform a quarkus build, anonymized data is sent to gather usage statistics on how Quarkus is being used and adopted.

However if you anser **"No"**, then no usage data is sent and you will not be asked again.

We will share what is learned from these anonymous usage data and plan on integrate things like usage and adoption into sites like [extensions.quarkus.io](extensions.quarkus.io) and [code.quarkus.io](code.quarkus.io).

## How can I enable and disable ?

Build time analytics is not active by default. If you have opted in and would like to disable build time analytics or would like to later enable collection, you can do so in two ways:

### Set globally

You can manually configure the global settings by editing the `io.quarkus.analytics.localconfig` file in the `.redhat` folder of your user’s home directory.

#### To enable analytics collection

```
{"disabled":false}
```

#### To disable analytics collection

```
{"disabled":true}
```

### Set per build

You can configure it for a given build by using the `quarkus.analytics.disabled` system property:

- To disable analytics collection, set `quarkus.analytics.disabled` to `true`.
- To enable analytics collection, set `quarkus.analytics.disabled` to `false`.

For instance, when using Maven, you can disable analytics collection for a single run with:

```
./mvnw clean install -Dquarkus.analytics.disabled=true
```
