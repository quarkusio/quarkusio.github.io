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

### Set Globally

You can configure the global settings by editing the `io.quarkus.analytics.localconfig` file in the `.redhat` folder of your user’s home directory.

#### To active collection: 

```
{"disabled":false}
```
#### To de-active collection: 

```
{"disabled":true}
```

### Set Per Project

You can set this by project using the system property `quarkus.analytics.disabled=true`

Example using maven:

```
mvn clean install -Dquarkus.analytics.disabled=true
```

Similarly, you can set it to `quarkus.analytics.disabled=true` if you want to enable it for a single run.
