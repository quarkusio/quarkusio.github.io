---
date: 2021-03-20
---
## Configuration

All `application.properties` files found in the classpath are loaded in the Quarkus configuration. Files in the current application will have priority and files in dependencies will use the classloader order. This means, that your Quarkus application may pick up additional configuration that was not picked up previously and possible change the behaviour of your application.

## Jackson

`SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` is now **disabled** by default which means your temporal types will be serialized as strings (e.g. `1988-11-17T00:00:00Z`) starting 1.13, instead of numeric values.

You can easily go back to the previous behavior by adding `quarkus.jackson.write-dates-as-timestamps=true` to your `application.properties`.

## Live Reload Instrumentation

`quarkus.dev.instrumentation` has been renamed to `quarkus.live-reload.instrumentation` to be more consistent with other configuration properties related to live reload.

## Removal of the `native-image` Maven goal

The goal `native-image` of `quarkus-maven-plugin` had long been deprecated and the plugin had been logging a warning since `1.11.1.Final`.

It has now finally been removed in 1.13. Please remove the plugin execution from your `pom.xml` and simply add the following property to your native profile:

```xml
<properties>
    <quarkus.package.type>native</quarkus.package.type>
</properties>
```

In case you have been setting non-default configuration parameters like `<enableHttpsUrlHandler>true</enableHttpsUrlHandler>`, replace those with the respective [documented properties](https://quarkus.io/guides/building-native-image#configuration-reference), e.g. `<quarkus.native.enable-https-url-handler>true</quarkus.native.enable-https-url-handler>`.

## Removal of the Maven `uberJar` and Gradle `--uber-jar` parameters

Both the Maven `uberJar` and the Gradle `--uber-jar` parameters had been deprecated since `1.11.2.Final`/`1.12.0.CR1` and have now been removed in 1.13.

As a replacement, add `quarkus.package.type=uber-jar` to your `application.properties` (or e.g. `pom.xml`).

## New methods signatures in `AuthenticationRequest`

New methods signatures have been added to the `AuthenticationRequest` interface to allow transportation of additional context information with the request such as context path, HTTP header or query parameter values. If you were implementing this interface before, you can now extend `BaseAuthenticationRequest` in 1.13 without having to implement the new methods.

## Kafka

Kafka `group.id` now defaults to application name (as configured by `quarkus.application.name`).
Only if the application name isn't set, a random string is used.

Automatic generation of a random string is merely a development convenience.
You shouldn't rely on it in production.

If you require application instances to have a unique Kafka `group.id`, you should ensure that explicitly.
For example, you can set the `KAFKA_GROUP_ID` environment variable.