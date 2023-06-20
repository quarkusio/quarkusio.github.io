---
layout: usage
title: Quarkus Usage Analytics
subtitle: 
permalink: /usage/
---

Starting from Quarkus 3.2 when you run Quarkus the first time in dev mode (`quarkus dev`, `mvn quarkus:dev`, etc.) you get asked if you agree to contribute anonymous build data to the Quarkus community.

![/assets/images/usage-prompt.png](/assets/images/usage-prompt.png)

If you answer Yes then, when you perform a quarkus build, anonymized data is sent
to gather usage statistics on how Quarkus is being used and adopted.

This service is provided by Red Hat and the details can be found on [usage policy](/usage/policy) page.

If you answer No, you will not be asked again and no usage data is sent.

This information is invaluable for the Quarkus team and contributors to 
gauge which operating systems, java version, build systems, extensions and more are 
used. 

We will share what is learned from these anonymous usage data and plan on integrate things like usage and adoption 
into sites like [extensions.quarkus.io](extensions.quarkus.io) and [code.quarkus.io](code.quarkus.io).

## Enabling and Disabling Telemetry Collection

Build time analytics is not active by default. If you have opted in and would like to disable build time analytics, you can do so in two ways:

Globally, by editing the `com.redhat.devtools.quarkus.localconfig` file in the `.redhat` folder of your user’s home directory. Update the file as follows:

```
{"active":false}
```

Per project, by using the system property`quarkus.analytics.disabled=true`

Example using maven:</p>

```
mvn clean install -Dquarkus.analytics.disabled=true
```

Similar you can set it to `quarkus.analytics.disabled=true` if you want to enable it for a single run.
