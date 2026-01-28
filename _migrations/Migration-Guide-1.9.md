---
date: 2020-10-21
---
## Datasource Configuration

In 1.3, we introduced a new much more flexible datasource configuration supporting both JDBC and reactive datasources in a unified way.

Until 1.9, we still supported the deprecated configuration properties but this leads to some confusion for the users mixing both old and new configuration properties.

In 1.9, the deprecated configuration properties are gone so you need to move to the new ones: https://quarkus.io/guides/datasource .

Quarkus will warn you about any use of the old deprecated configuration properties.

## Redis Client Configuration

The following configuration properties have been deprecated:
- `quarkus.redis.ssl`,
- `quarkus.redis.database`,
- `quarkus.redis.password`

And they are all configurable via the `quarkus.redis.hosts` property using a connection string in the following format:
`redis://[username:password@][host][:port][/database]`.

## Spring Boot @ConfigProperties

The verbatim naming strategy previously used with Spring Boot properties extension is now replaced by the one defined in quarkus.arc.config-properties-default-naming-strategy property (with `kebab` being the default).

To keep your application operating as before, perform one of the following changes:

* Set `quarkus.arc.config-properties-default-naming-strategy=verbatim` in the `application.(yml|properties)`
* Or replace the properties from verbatim style to kebab (default) or the one defined in quarkus.arc.config-properties-default-naming-strategy property.

## Reactive Messaging - Kafka

* Kafka auto-commit is now disabled by default - you can re-enabled it using: `mp.messaging.incoming.my-channel.enable.auto.commit=true`. Enabling auto-commit sets the commit-strategy to _ignore_ (auto-commit will do the work, not the connector) 
* The default commit-strategy is `latest` - you may want to switch to `throttled` to improve the consumption rate. Set it with: `mp.messaging.incoming.my-channel.commit-strategy=throttled`

## Mutiny logging about dropped exceptions

You may start seeing logs with:
```
2020-10-30 17:33:39,400 ERROR [io.qua.mut.run.MutinyInfrastructure] (vert.x-eventloop-thread-X) Mutiny had to drop the following exception: ....
```

This log happens when Mutiny receives a failure but cannot dispatch it to a downstream subscriber as it would violate the Reactive Streams protocol. Before Quarkus 1.9, these exceptions were swallowed.  