---
date: 2023-01-25
---

## Config

Configuration property names set in `.env` files are now converted to their expected name, following the Environment Variables naming rules detailed in https://quarkus.io/version/2.16/guides/config-reference#environment-variables.

## Kafka Client Dev services

Kafka Client extension introduces `quarkus.kafka.devservices.provider` build time property for configuring the dev service image provider. 
Before 2.16 strimzi images could be used by configuring the `quarkus.kafka.devservices.image-name` property to a value containing `strimzi` text. After 2.16 developers have the option to configure `quarkus.kafka.devservices.provider` to values `redpanda` (default), `strimzi` and `kafka-native` with each option having a default container image. `quarkus.kafka.devservices.image-name` can still be used to change the default image name.

## No wildcard Origin support by default for CORS Filter

CORS filter no longer supports all Origins by default when it is enabled and it has to be configured to allow all Origins.
For example, if, after a careful consideration, you decide that all Origins must be supported, then you can do it like this:
```
quarkus.http.cors=true
quarkus.http.cors.origins=/.*/
```

Please note that the same-site Origin requests will be supported without `quarkus.http.cors.origins` having to be configured.
Therefore configuring `quarkus.http.cors.origins` will only be required when the trusted 3rd party Origin requests should be allowed in which case allowing all Origins will be risky and unnecessary.

## Micrometer

By default, the metrics are now exported using the Prometheus format `application/openmetrics-text`,
you can revert to the former format by specifying the `Accept` request header to `plain/text` (`curl -H "Accept: text/plain" localhost:8080/q/metrics/`).

## <a id="quarkus-transaction">`QuarkusTransaction.run`/`QuarkusTransaction.call` are deprecated

`QuarkusTransaction.run`/`QuarkusTransaction.call` are deprecated in favor of newly introduced, more explicit methods. Code relying on these methods should be updated as follows:

```
// Before
QuarkusTransaction.run(() -> { ... });
QuarkusTransaction.call(() -> { ... });
// After
QuarkusTransaction.requiringNew().run(() -> { ... });
QuarkusTransaction.requiringNew().call(() -> { ... });


// Before
QuarkusTransaction.run(QuarkusTransaction.runOptions()
        .semantic(RunOptions.Semantic.REQUIRED),
        () -> { ... });
QuarkusTransaction.call(QuarkusTransaction.runOptions()
        .semantic(RunOptions.Semantic.REQUIRED),
        () -> { ... });
// After
QuarkusTransaction.joiningExisting().run(() -> { ... });
QuarkusTransaction.joiningExisting().call(() -> { ... });


// Before
QuarkusTransaction.run(QuarkusTransaction.runOptions()
        .timeout(10)
        .exceptionHandler((throwable) -> {
            if (throwable instanceof SomeException) {
               return RunOptions.ExceptionResult.COMMIT;
            }
            return RunOptions.ExceptionResult.ROLLBACK;
        }),
        () -> { ... });
QuarkusTransaction.call(QuarkusTransaction.runOptions()
        .timeout(10)
        .exceptionHandler((throwable) -> {
            if (throwable instanceof SomeException) {
               return RunOptions.ExceptionResult.COMMIT;
            }
            return RunOptions.ExceptionResult.ROLLBACK;
        }),
        () -> { ... });
// After
QuarkusTransaction.requiringNew()
        .timeout(10)
        .exceptionHandler((throwable) -> {
            if (throwable instanceof SomeException) {
               return RunOptions.ExceptionResult.COMMIT;
            }
            return RunOptions.ExceptionResult.ROLLBACK;
        })
        .run(() -> { ... });
QuarkusTransaction.requiringNew()
        .timeout(10)
        .exceptionHandler((throwable) -> {
            if (throwable instanceof SomeException) {
               return RunOptions.ExceptionResult.COMMIT;
            }
            return RunOptions.ExceptionResult.ROLLBACK;
        })
        .call(() -> { ... });
```

See the javadoc of `QuarkusTransaction` and the [documentation of programmatic transactions](https://quarkus.io/guides/transaction#programmatic-approach) for more information.