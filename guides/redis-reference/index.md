# Redis extension reference guide

Redis is an in-memory data store used as a database, cache, streaming engine, and message broker. The Quarkus Redis extension allows integrating Quarkus applications with Redis.

To use this extension, the user must be familiar with Redis, especially understanding the mechanism of commands and how they are organized. Typically, we recommend:

1. The [interactive tutorial](https://redis.io/try/sandbox/) introducing Redis.
2. The [command references](https://redis.io/commands/) explains Redis commands and contains links to reference documentation.

This extension provides imperative and reactive APIs and low-level and high-level (type-safe) clients.

## Use the Redis Client

If you want to use this extension, you need to add the `io.quarkus:quarkus-redis` extension first. In your `pom.xml` file, add:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-redis-client</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-redis-client")
```

With this dependency, you can then inject Redis clients or *datasource* (high-level, type-safe API), such as:

```java
import io.quarkus.redis.datasource.RedisDataSource;

// ...
@Inject RedisAPI lowLevelClient;
@Inject RedisDataSource highLevelApi;
```

More details about the various APIs offered by the quarkus-redis extension are available in the [One extension, multiple APIs](#apis) section.

> [!NOTE]
> To use Redis as a cache backend, refer to the [Redis Cache Backend reference](/guides/cache-redis-reference/index.md).

<a id="apis"></a>
## One extension, multiple APIs

This extension provides multiple ways to interact with Redis:

- the *low-level* Vert.x client: it's a fully reactive, non-blocking, and asynchronous client. More details on [the Vert.x Redis Client documentation](https://vertx.io/docs/vertx-redis-client/java/). Two APIs are exposed: `io.vertx.redis.client.Redis`, and `io.vertx.redis.client.RedisAPI`. You will generally use the latter, except if you need to manage connections yourself.
- the *low-level* Mutiny variant of the Vert.x API: Unlike the previous one, it exposes a Mutiny API and provides both reactive and imperative methods (suffixed with `andAwait()`). Two APIs are exposed: `io.vertx.mutiny.redis.client.Redis` and `io.vertx.mutiny.redis.client.RedisAPI`. You will generally use the latter, except if you need to manage connections yourself.
- a *high-level* reactive data source: A type-safe, high-level API to interact with Redis. This API is fully reactive and asynchronous. It exposes a Mutiny API. It exposes the `io.quarkus.redis.datasource.ReactiveRedisDataSource` interface.
- a *high-level* imperative data source: A type-safe, high-level API to interact with Redis. It is the imperative variant of the reactive data source. It exposes the `io.quarkus.redis.datasource.RedisDataSource` interface.

To help you select the suitable API for you, here are some recommendations:

- If you are building an imperative (*classic*) Quarkus application integrating with Redis: use `io.quarkus.redis.datasource.RedisDataSource`.
- If you are building a reactive Quarkus application integrating with Redis: use `io.quarkus.redis.datasource.ReactiveRedisDataSource`.
- If you need fine-grain control, or execute commands in a generic way: use `io.vertx.mutiny.redis.client.RedisAPI`
- If you have existing Vert.x code, use `io.vertx.redis.client.RedisAPI`
- If you need to emit custom commands, you can either use the data sources (reactive or imperative) or the `io.vertx.mutiny.redis.client.Redis`.

<a id="default-and-named-clients"></a>
## <a id="inject-the-default-and-named-clients"></a> Inject the default and named clients

This extension lets you configure a *default* Redis client/data sources or *named* ones. The latter is essential when you need to connect to multiple Redis instances.

The default connection is configured using the `quarkus.redis.*` properties. For example, to configure the default Redis client, use:

```properties
quarkus.redis.hosts=redis://localhost/
```

When using the default connection, you can inject the various APIS using a *plain* `@Inject`:

```java
@ApplicationScoped
public class RedisExample {
    @Inject ReactiveRedisDataSource reactiveDataSource;
    @Inject RedisDataSource redisDataSource;
    @Inject RedisAPI redisAPI;
    // ...
}
```

> [!TIP]
> In general, you inject a single one; the previous snippet is just an example.

*Named* clients are configured using the `quarkus.redis.<name>.*` properties:

```properties
quarkus.redis.my-redis-1.hosts=redis://localhost/
quarkus.redis.my-redis-2.hosts=redis://my-other-redis:6379
```

To access the APIs, you need to use the `@RedisClientName` qualifier:

```java
@ApplicationScoped
public class RedisExample {
    @Inject @RedisClientName("my-redis-1") ReactiveRedisDataSource reactiveDataSource;
    @Inject @RedisClientName("my-redis-2") RedisDataSource redisDataSource;
    // ...
}
```

> [!TIP]
> You can omit the `@Inject` annotation when using `@RedisClientName`.

### Activate or deactivate Redis Clients

When a Redis Clients is configured at build time, and its URL is set at runtime, it is active by default. Quarkus starts the corresponding Redis Client when the application starts.

To deactivate a Redis Client at runtime, either:

- Do not set `quarkus.redis[.optional name].hosts` or `quarkus.redis[.optional name].hosts-provider-name`.
- Set `quarkus.redis[.optional name].active` to `false`.

If a Redis Client is not active:

- The Redis Client does not attempt to connect to Redis during application startup.
- The Redis Client does not contribute a [health check](#redis-health-check).
- Static CDI injection points involving the Redis Client, such as `@Inject ReactiveRedisDataSource redis` or `@Inject RedisDataSource redis`, cause application startup to fail.
- Dynamic retrieval of the Redis Client, such as through `CDI.getBeanContainer()`, `Arc.instance()`, or an injected `Instance<ReactiveRedisDataSource>`, causes an exception to be thrown.
- Other Quarkus extensions that consume the Redis Client may cause application startup to fail.

This feature is especially useful when the application must dynamically select a Redis Client from a predefined set at runtime.

**An example of configuring multiple Redis Clients for runtime selection:**

```properties
quarkus.redis.one.active=false
quarkus.redis.one.hosts=redis://localhost:64251

quarkus.redis.two.active=false
quarkus.redis.two.hosts=redis://localhost:64251
```

```java
import io.quarkus.arc.InjectableInstance;

@ApplicationScoped
public class MyConsumer {
    @Inject
    @RedisClientName("one")
    InjectableInstance<ReactiveRedisDataSource> one;
    @Inject
    @RedisClientName("two")
    InjectableInstance<ReactiveRedisDataSource> two;

    public void doSomething() {
        ReactiveRedisDataSource redis = one.getActive();
        // ...
    }
}
```

Setting `quarkus.redis.one.active=true` [at runtime](/guides/config-reference/index.md#configuration-sources) makes only the Redis Client `one` available. Setting `quarkus.redis.two.active=true` at runtime makes only the Redis Client `two` available.

> [!IMPORTANT]
> A Redis Client (either default or named) must always be discoverable at build-time to be considered for runtime injection. This can be done by injecting the Redis Client name with `@RedisClientName`. If the Redis Client name may be active or inactive, it needs to use the wrapper `InjectableInstance<>`, or else Quarkus will throw an exception at startup time if the Redis Client is inactive. Alternatively, Redis Clients may also be discovered via configuration.

## Connect to the Redis server

The Redis extension can operate in 4 distinct modes:

- Simple client (probably what most users need).
- Sentinel (when working with Redis in High Availability mode).
- Cluster (when working with Redis in Clustered mode).
- Replication (single shard, one node writes, multiple read).

The connection url is configured with the `quarkus.redis.hosts` (or `quarkus.redis.<name>.hosts`) as follows:

```properties
quarkus.redis.hosts=redis://[:password@]host[:port][/db-number]
```

### Use Unix Socket

When using unix-socket, you need:

```properties
quarkus.redis.hosts=unix://[:password@]/domain/docker.sock[?select=db-number]
```

### Use the Sentinel Mode

When using Sentinel, you need to pass multiple *host urls* and configure the client type to `sentinel`:

```properties
quarkus.redis.hosts=redis://localhost:5000,redis://localhost:5001,redis://localhost:5002
quarkus.redis.client-type=sentinel

# Optional
quarkus.redis.master-name=my-sentinel # Default is mymaster
quarkus.redis.role=master # master is the default
```

The host URLs here must be the sentinel servers. The client will obtain the URLs of actual Redis servers (master or replicas, depending on `role`) from one of the sentinels, using the `master-name` as an identifier of the "master set".

Note that you practically never want to configure `quarkus.redis.role=sentinel`. This setting means that the Redis client will execute commands directly on one of the sentinel servers, instead of an actual Redis server guarded by the sentinels.

#### Automatic Failover

In the sentinel mode, it is possible to configure automatic failover of *master* connections:

```properties
quarkus.redis.auto-failover=true
```

If enabled, the sentinel client will additionally create a connection to one sentinel node and watch for failover events. When new master is elected, all connections to the old master are automatically closed and new connections to the new master are created. Automatic failover makes sense for connections executing regular commands, but not for connections used to subscribe to Redis pub/sub channels.

Note that there is a brief period of time between the old master failing and the new master being elected when the existing connections will temporarily fail all operations. After the new master is elected, the connections will automatically fail over and start working again.

### Use the Cluster Mode

When using Redis in cluster mode, you need to pass multiple *host urls*, configure the client type to `cluster` and configure the `replicas` mode:

```properties
quarkus.redis.hosts=redis://localhost:7000,redis://localhost:7001,redis://localhost:7002
quarkus.redis.client-type=cluster

# Optional
quarkus.redis.replicas=share # defaults to "never"
```

The host URLs here must be some of the cluster members. Not all cluster members need to be configured, as the client will obtain a full cluster topology from one of the known servers. However, it is advisable to configure at least 2 or 3 nodes, not just 1.

By default, all commands are sent to a master node (if the command has a key, it is the master node of the shard that owns the key, otherwise it is a random master node). It is possible to configure the Redis client to send read-only commands (queries) to replica nodes. Set the `quarkus.redis.replicas` configuration property to:

- `never`: never use replica nodes, always send queries to master nodes (this is the default)
- `always`: always use replica nodes (if there's more than one replica in the shard, it is selected randomly), never send queries to master nodes
- `share`: use master and replica nodes to execute queries (the specific node for each query is selected randomly)

Note that replication in Redis is asynchronous, so replica nodes may be lagging behind their master nodes.

### Use the Replication Mode

When using the replication mode, you need to pass a single host url and configure the type to be `replication`:

```properties
quarkus.redis.hosts=redis://localhost:7000
quarkus.redis.client-type=replication

# Optional
quarkus.redis.replicas=share # defaults to "never"
```

By default, all commands are sent to the master node. It is possible to configure the Redis client to send read-only commands (queries) to replica nodes. Set the `quarkus.redis.replicas` configuration property to:

- `never`: never use replica nodes, always send queries to the master node (this is the default)
- `always`: always use replica nodes (if there's more than one replica, it is selected randomly), never send queries to the master node
- `share`: use master and replica nodes to execute queries (the specific node for each query is selected randomly)

Note that replication in Redis is asynchronous, so replica nodes may be lagging behind the master node.

#### Static Topology

In the replication mode, it is possible to reconfigure the Redis client to skip automatic discovery of the topology:

```properties
quarkus.redis.topology=static
```

With static topology, the first node in the configuration is assumed to be a *master* node, while the remaining nodes are assumed to be *replicas*. The nodes are not verified; it is a responsibility of the application developer to ensure that the static configuration is correct.

Note that automatic discovery of the topology is usually the preferred choice. Static configuration should only be used when necessary. One such case is *Amazon Elasticache for Redis (Cluster Mode Disabled)*, where:

- master node should be set to the *primary endpoint*, and
- one replica node should be set to the *reader endpoint*.

> [!WARNING]
> Note that the reader endpoint of Elasticache for Redis (Cluster Mode Disabled) is a domain name which resolves to a CNAME record that points to one of the replicas. The CNAME record to which the reader endpoint resolves changes over time. This form of DNS-based load balancing does not work well with DNS resolution caching and connection pooling. As a result, some replicas are likely to be underutilized.

### Connect to Redis Cloud

To connect to redis cloud, you need the following properties:

```properties
quarkus.redis.hosts=<the redis cloud url such as redis://redis-12436.c14.us-east-1-3.ec2.cloud.redislabs.com:12436>
quarkus.redis.password=<the password>
```

### Use TLS

To use TLS, you need to:

1. Set the `quarkus.redis.tls.enabled=true` property or use the  [TLS registry](/guides/tls-registry-reference/index.md) (recommended)
2. Make sure that your URL starts with `rediss://` (with two `s`)

When using the TLS registry, you need to use a named configuration to avoid conflicts with other TLS configurations:

```properties
quarkus.tls.redis.trust-store.p12.path=client.p12
quarkus.tls.redis.trust-store.p12.password=secret

quarkus.redis.tls-configuration-name=redis # Reference the named configuration
```

> [!IMPORTANT]
> The default hostname verifier is set to `NONE`, meaning it does not verify the host name. You can change this behavior by setting the `quarkus.redis.tls.hostname-verification-algorithm` property, to `HTTPS` for example.

### Configure the authentication

The Redis password can be set in the `redis://` URL or with the `quarkus.redis.password` property. We recommend the latter, and if possible, using secrets or an environment variable to configure the password.

The associated environment variable is `QUARKUS_REDIS_PASSWORD`, or `QUARKUS_REDIS_<NAME>_PASSWORD` for named clients.

### Connection pooling

Connections to Redis are always pooled. By default, maximum number of connections in the pool is 6. This can be configured using `quarkus.redis.max-pool-size`.

When the connection pool is depleted, attempts to obtain a connection are put into a queue. By default, maximum number of attempts waiting in the queue to obtain a Redis connection is 24. This can be configured using `quarkus.redis.max-pool-waiting`.

Executing certain commands modifies the server-side state and the behavior of the connection. Such connections cannot be reused and when closed, they are not put back into the pool; instead, they are truly closed. The commands that cause this behavior are:

- subscription commands (`SUBSCRIBE`, `UNSUBSCRIBE` etc.)
- `SELECT`
- `AUTH`

## Use Redis data sources

Quarkus exposes a high-level API on top of Redis. This API is type-safe and structured around the notion of *group*, inherited from the [Redis command organization](https://redis.io/commands/command-docs/). This API lets you execute Redis commands more conveniently and safely.

### Inject data sources

For each configured Redis client, two Redis data sources are exposed:

- `io.quarkus.redis.datasource.RedisDataSource` - an imperative (blocking) Redis data source. Each operation blocks until a response is received or a timeout is reached
- `io.quarkus.redis.datasource.ReactiveRedisDataSource` - a reactive Redis data source returning `Uni<X>` or `Multi<X>`.

If you configured the *default* Redis client, you could inject the data sources using:

```java
@Inject RedisDataSource defaultRedisDataSource;
@Inject ReactiveRedisDataSource defaultReactiveRedisDataSource;
```

If you configured a *named* Redis client, you need to use the `io.quarkus.redis.RedisClientName` qualifier to select the right client:

```java
@RedisClientName("my-redis") RedisDataSource myRedisDataSource;
@RedisClientName("my-redis") ReactiveRedisDataSource myReactiveRedisDataSource;
```

When using the *blocking* variant, you can configure the default timeout with:

```properties
quarkus.redis.timeout=5s
quarkus.redis.my-redis.timeout=5s
```

The default timeout is configured to 10s.

> [!NOTE]
> **All about delegation**
>
> The blocking data source (`io.quarkus.redis.datasource.RedisDataSource`) is implemented on top of the reactive one (`io.quarkus.redis.datasource.ReactiveRedisDataSource`). The `ReactiveRedisDataSource` is implemented on top of the `io.vertx.mutiny.redis.Redis` API.

#### Data Source groups

As mentioned above, the API is divided into groups:

- bitmap - `.bitmap()`
- key (generic) - `.key()`
- geo - `.geo(memberType)`
- hash - `.hash(`valueType)\`
- hyperloglog - `.hyperloglog(memberType)`
- list - `.list(memberType)`
- pubsub - `pubsub()`
- set - `.set(memberType)`
- sorted-set - `.sortedSet(memberType)`
- string - `.value(valueType)`
- stream - `.stream(`valueType\`)
- transactions - `withTransaction`
- json - `.json()` (requires the [RedisJSON](https://redis.io/docs/latest/develop/data-types/json/) module on the server side)
- bloom - `.bloom()` (requires the [RedisBloom](https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/) module on the server side)
- cuckoo - `.cuckoo()` (requires the [RedisBloom](https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/) module on the server side, which also provides the cuckoo filter commands)
- count-min - `.countmin()` (requires the [RedisBloom](https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/) module on the server side, which also provides the count-min filter commands)
- top-k - `.topk()` (requires the [RedisBloom](https://redis.io/docs/latest/develop/data-types/probabilistic/bloom-filter/) module on the server side, which also provides the top-k filter commands)
- graph - `.graph()` (requires the [RedisGraph](https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/deprecated-features/graph/) module on the server side). These commands are marked as experimental. Also the module has been declared *end of life* by [Redis](https://redis.io/blog/redisgraph-eol/).
- search - `.search()` (requires the [RedisSearch](https://redis.io/docs/latest/develop/interact/search-and-query/) module on the server side).
- auto-suggest - `.autosuggest()` (requires the [RedisSearch](https://redis.io/docs/latest/develop/interact/search-and-query/) module on the server side).
- time-series - `.timeseries()` (requires the [Redis Time Series](https://redis.io/docs/latest/develop/data-types/timeseries/) module on the server side).

These commands are marked as experimental, as we would need feedback before making them stable.

Each of these methods returns an object that lets you execute the commands related to the group. The following snippet demonstrates how to use the *hash* group:

```java
@ApplicationScoped
public class MyRedisService {

    private static final String MY_KEY = "my-key";

    private final HashCommands<String, String, Person> commands;

    public MyRedisService(RedisDataSource ds) { // <1>
        commands = ds.hash(Person.class); // <2>
    }

    public void set(String field, Person value) {
        commands.hset(MY_KEY, field, value);  // <3>
    }

    public Person get(String field) {
        return commands.hget(MY_KEY, field);  // <4>
    }
}
```

1. Inject the `RedisDataSource` in the constructor
2. Creates the `HashCommands` object. This object has three type parameters: the type of the key, the type of the field, and the type of the member
3. Use the created `commands` to associate the field `field` with the value `value`
4. Use the created `commands` to retrieve the field `field` value.

### Serializing and Deserializing data

The data source APIs handle the serialization and deserialization automatically. By default, non-standard types are serialized into JSON and deserialized from JSON. In this case, `quarkus-jackson` is used.

### Binary

To store or retrieve binary data, use `byte[]`.

### Custom codec

You can register custom codec by providing a CDI *bean* implementing the `io.quarkus.redis.datasource.codecs.Codec` interface:

```java
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.redis.datasource.codecs.Codec;

@ApplicationScoped
public class PersonCodec implements Codec {
    @Override
    public boolean canHandle(Type clazz) {
        return clazz.equals(Person.class);
    }

    @Override
    public byte[] encode(Object item) {
        var p = (Person) item;
        return (p.firstName + ";" + p.lastName.toUpperCase()).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Object decode(byte[] item) {
        var value = new String(item, StandardCharsets.UTF_8);
        var segments = value.split(";");
        return new Person(segments[0], segments[1]);
    }
}
```

The `canHandle` method is called to check if the codec can handle a specific type. The parameter received in the `encode` method matches that type. The object returned by the `decode` method must also match that type.

### Use type reference

Each group can be configured with `Class`, or with `TypeReference` objects. `TypeReference` are useful when dealing with Java generics:

```java
@ApplicationScoped
public class MyRedisService {

    private static final String MY_KEY = "my-key";

    private final HashCommands<String, String, List<Person>> commands;

    public MyRedisService(RedisDataSource ds) {
        commands = ds.hash(new TypeReference<List<Person>>(){});
    }

    public void set(String field, List<Person> value) {
        commands.hset(MY_KEY, field, value);
    }

    public List<Person> get(String field) {
        return commands.hget(MY_KEY, field);
    }
}
```

> [!IMPORTANT]
> You cannot use type references when using transaction. This is a known limitation.

### Manipulate cached and binary data with the `value` group

The `value` group is used to manipulate [Redis Strings](https://redis.io/docs/manual/data-types/#strings). Thus, this group is not limited to Java Strings but can be used for integers (like a counter) or binary content (like images).

#### Work with cached values

You can use Redis as a cache using the `setex` command, which stores a given value to a given key for a given duration. The following snippet shows how such a command can be used to store `BusinessObject` for 1 second.

```java
@ApplicationScoped
public static class MyRedisCache {

    private final ValueCommands<String, BusinessObject> commands;

    public MyRedisCache(RedisDataSource ds) {
        commands = ds.value(BusinessObject.class);
    }

    public BusinessObject get(String key) {
        return commands.get(key);
    }

    public void set(String key, BusinessObject bo) {
        commands.setex(key, 1, bo); // Expires after 1 second
    }
}
```

You can use the `setnx` method only to set the value if no value has been stored for the given key.

> [!TIP]
> The `key` group provides more fine-grain control on expiration and ttl of each key.

> [!NOTE]
> The `set` method can also receive a `SetArgs` argument that modify the behavior:
>
> - `ex(seconds)` - Set the specified expire time, in seconds.
> - `px(milliseconds)` - Set the specified expire time, in milliseconds.
> - `exat(timestamp-seconds)` - Set the specified Unix time at which the key will expire, in seconds.
> - `pxat(timestamp-milliseconds)` - Set the specified Unix time at which the key will expire, in milliseconds.
> - `nx()` - Only set the key if it does not already exist.
> - `xx()` - Only set the key if it already exists.
> - `keepttl()` - Retain the time to live associated with the key.

#### Store binary data

Redis *strings* can be used to store binary data, such as images. In this case, we will use `byte[]` as value type:

```java
@ApplicationScoped
public static class MyBinaryRepository {

    private final ValueCommands<String, byte[]> commands;

    public MyBinaryRepository(RedisDataSource ds) {
        commands = ds.value(byte[].class);
    }

    public byte[] get(String key) {
        byte[] bytes = commands.get(key);
        if (bytes == null) {
            throw new NoSuchElementException("`" + key + "` not found");
        }
        return bytes;
    }

    public void add(String key, byte[] bytes) {
        commands.set(key, bytes);
    }

    public void addIfAbsent(String key, byte[] bytes) {
        commands.setnx(key, bytes);
    }
}
```

#### Store a counter

You can store counters in Redis as demonstrated below:

```java
@ApplicationScoped
public static class MyRedisCounter {

    private final ValueCommands<String, Long> commands;

    public MyRedisCounter(RedisDataSource ds) {
        commands = ds.value(Long.class); // <1>
    }

    public long get(String key) {
        Long l = commands.get(key);  // <2>
        if (l == null) {
            return 0L;
        }
        return l;
    }

    public void incr(String key) {
        commands.incr(key);  // <3>
    }

}
```

1. Retrieve the commands. This time we will manipulate `Long` values
2. Retrieve the counter associated with the given `key`. Return `0L` when no counter is stored.
3. Increment the value. If there are no counter stored for the key, the `incr` command considers 0 as value (so `incr` sets the value to 1).

There are other methods that can be useful to manipulate counters, such as:

- `incrby` - allows setting the increment value (positive or negative)
- `incrbyfloat` - allows setting the increment value as a float/ double (the stored value will be a double)
- `set` - to set an initial value if needed
- `decr` and `decrby` - allows decrementing the stored value

### Communicate with pub/sub

Redis allows sending *messages* to channels and listening for these messages. These features are available from the `pubsub` group.

The following snippets shows how a *cache* can emit a `Notification` after every `set`, and how a subscriber can receive the notification.

```java
public static final class Notification {
    public String key;
    public BusinessObject bo;

    public Notification() {

    }

    public Notification(String key, BusinessObject bo) {
        this.key = key;
        this.bo = bo;
    }
}

@ApplicationScoped
@Startup // We want to create the bean instance on startup to subscribe to the channel.
public static class MySubscriber implements Consumer<Notification> {
    private final PubSubCommands<Notification> pub;
    private final PubSubCommands.RedisSubscriber subscriber;

    public MySubscriber(RedisDataSource ds) {
        pub = ds.pubsub(Notification.class);
        subscriber = pub.subscribe("notifications", this);
    }

    @Override
    public void accept(Notification notification) {
        // Receive the notification
    }

    @PreDestroy
    public void terminate() {
        subscriber.unsubscribe(); // Unsubscribe from all subscribed channels
    }
}

@ApplicationScoped
public static class MyCache {

    private final ValueCommands<String, BusinessObject> commands;
    private final PubSubCommands<Notification> pub;

    public MyCache(RedisDataSource ds) {
        commands = ds.value(BusinessObject.class);
        pub = ds.pubsub(Notification.class);
    }

    public BusinessObject get(String key) {
        return commands.get(key);
    }

    public void set(String key, BusinessObject bo) {
        commands.set(key, bo);
        pub.publish("notifications", new Notification(key, bo));
    }
}
```

### Use Redis transactions

Redis transactions are slightly different from relational database transactions. Redis transactions are a batch of commands executed altogether.

A Redis transaction can *watch* a set of keys, which would *discard* the transaction is one of these keys are updated during the transaction execution.

Commands enqueued in a transaction are not executed before the whole transaction is executed. It means that you cannot retrieve a result during the transaction. Results are accumulated in a `TransactionResult` object you will access after the completion of the transaction. This object contains whether the transaction succeeded or was discarded, and in the former case the result of each command (indexed by the command order).

To start a transaction, you use the `withTransaction` method. This method receives a `Consumer<TransactionalRedisDataSource>`, which follows the same API as the regular `RedisDataSource` except that the commands return `void` (`Uni<Void>` for the reactive variant). When that consumer returns, the transaction is *executed*.

The following snippet shows how to create a transaction executing two related *writes*:

```java
@Inject RedisDataSource ds;

// ...

TransactionResult result = ds.withTransaction(tx -> {
        TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
        hash.hset(KEY, "field-1", "hello");
        hash.hset(KEY, "field-2", "hello");
    });
```

The received `tx` object can also be used to *discard* the transaction, using `tx.discard()`. The returned `TransactionResult` lets you retrieve the result of each command.

When using the reactive variant of the data source, the passed callback is a `Function<ReactiveTransactionalRedisDataSource, Uni<Void>>`:

```java
@Inject ReactiveRedisDataSource ds;

// ...

Uni<TransactionResult> result = ds.withTransaction(tx -> {
        ReactiveTransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
        return hash.hset(KEY, "field-1", "hello")
            .chain(() -> hash.hset(KEY, "field-2", "hello"));
});
```

Transaction execution can be conditioned by *keys*. When a passed key gets modified during the execution of a transaction, the transaction is discarded. The keys are passed as ``String``s to the second, vararg parameter of the `withTransaction` method:

```java
TransactionResult result = ds.withTransaction(tx -> {
    TransactionalHashCommands<String, String, String> hash = tx.hash(String.class);
    hash.hset(KEY, "field-1", "hello");
    hash.hset(KEY, "field-2", "hello");
}, KEY);
```

> [!IMPORTANT]
> You cannot use the pub/sub feature from within a transaction.

#### Implement the optimistic locking pattern

To use optimistic locking, you need to use a variant of the `withTransaction` method allowing execution of code before the transaction starts. In other words, it will be executed as follows:

```
WATCH key

// Pre-transaction block
// ...
// Produce a result

MULTI

// In transaction code, receive the result produced by the pre-transaction block.

EXEC
```

For example, if you need to update a value in a hash only if the field exists, you will use the following API:

```java
OptimisticLockingTransactionResult<Boolean> result = blocking.withTransaction(ds -> {
    // The pre-transaction block:
    HashCommands<String, String, String> hashCommands = ds.hash(String.class);
    return hashCommands.hexists(key, "field"); // Produce a result (boolean in this case)
},
(exists, tx) -> {
    // The transactional block, receives the result and the transactional data source
    if (exists) {
        tx.hash(String.class).hset(key, "field", "new value");
    } else {
        tx.discard();
    }
},
// The watched key
key);
```

If one of the watched keys is touched before or during the execution of the pre-transaction or transactional blocks, the transaction is aborted. The pre-transactional block produces a result that the transactional block can use. This construct is necessary because, within a transaction, the commands do not produce a result. Results can only be retrieved after the execution of the transaction.

The pre-transaction and transactional blocks are invoked on the same Redis connection. Consequently, the pre-transaction block must use the passed data source to execute commands. Thus, the commands are emitted from that connection. These commands must not modify the watched keys.

#### Error handling

If the transaction block returns successfully (or completes with an item in case of the reactive API), the transaction is executed. Note that in this case, some transaction commands may succeed and some may fail; this is standard Redis behavior. The `TransactionResult.hasErrors()` method indicates whether at least one command failed. Results of failed commands in the `TransactionResult` are represented as ``Throwable``s. If the transaction block throws an exception (or completes with a failure in case of the reactive API), the transaction is discarded automatically and the exception is rethrown (or the resulting `Uni` completes with the same failure in case of the reactive API).

In case of the optimistic locking API, if the pre-transaction block returns successfully (or completes with an item in case of the reactive API), the transaction is started and execution proceeds to the transaction block. If the pre-transaction block throws an exception (or completes with a failure in case of the reactive API), all watched keys are ``UNWATCH``ed automatically and the exception is rethrown (or the resulting `Uni` completes with the same failure in case of the reactive API). In this case, the transaction is not started and the transaction block is not executed.

#### Transactions in Redis Cluster

In the cluster mode, transaction commands (`MULTI`, `EXEC`, `DISCARD`, `WATCH`, `UNWATCH`) are handled specially.

By default, transactions in cluster are disabled and transaction commands immediately fail.

It is however possible to enable single-node transactions by setting `quarkus.redis.cluster-transactions` to `single-node`. Note that this setting only makes sense in the cluster mode and is ignored otherwise.

In this mode, the `MULTI` command is queued and is only issued when the next command is executed. This next command binds the connection to the corresponding node of the Redis cluster (so it should have keys, otherwise the target node is random). All subsequent commands are targeted to that node. If some of the subsequent commands have a key that belongs to another node, the command fails on the server side. If `WATCH` is used before `MULTI`, its key(s) determine to which node the connection is bound and the subsequent `MULTI` is not queued. If `WATCH` keys belong to multiple nodes, the command fails on the client side.

### Execute custom commands

To execute a custom command, or a command not supported by the API, use the following approach:

```java
@Inject ReactiveRedisDataSource ds;

// ...

Response response = ds.execute("my-command", param1, param2, param3);
```

The `execute` method sends the command to Redis and retrieves the `Response`. The command name is passed as first parameters. You can add an arbitrary number of **String** parameters to your command. The result is wrapped into a `Response` object.

The reactive variant returns a `Uni<Response>`.

> [!NOTE]
> You can also execute custom command in a transaction.

## Preload data into Redis

On startup, you can configure the Redis client to preload data into the Redis database.

### Configure the load scripts

Specify the *load script* you want to load using:

```properties
quarkus.redis.load-script=import.redis # import.redis is the default in dev mode, no-file is the default in production mode
quarkus.redis.my-redis.load-script=actors.redis, movies.redis
```

> [!IMPORTANT]
> `load-script` is a build time property than cannot be overridden at runtime.

Note that each client can have a different script, even a list of scripts. In the case of a list, the data is imported in the list order (for example, first `actors.redis`, then `movies.redis` for the `my-redis` client).

### Write load scripts

The `.redis` file follows a *one command per line* format:

```text
# Line starting with # and -- are ignored, as well as empty lines

-- One command per line:
HSET foo field1 abc field2 123

-- Parameters with spaces must be wrapped into single or double quotes
HSET bar field1 "abc def" field2 '123 456 '

-- Parameters with double quotes must be wrapped into single quotes and the opposite
SET key1 'A value using "double-quotes"'
SET key2 "A value using 'single-quotes'"
```

Quarkus batches all the commands from a single file and sends all the commands. The loading process fails if there is any error, but the previous instructions may have been executed. To avoid that, you can wrap your command into a Redis *transaction*:

```text
-- Run inside a transaction
MULTI
SET key value
SET space:key 'another value'
INCR counter
EXEC
```

### Configure the pre-loading

The data is loaded when the application starts. By default, it drops the whole database before importing. You can prevent this using `quarkus.redis.flush-before-load=false`.

Also, the import process only runs if the database is empty (no key). You can force to import even if there is data using the `quarkus.redis.load-only-if-empty=false`

### Distinguish dev/test vs. prod when pre-loading

As mentioned above, in dev and test modes, Quarkus tries to import data by looking for the `src/main/resources/import.redis`. This behavior is disabled in *prod* mode, and if you want to import even in production, add:

```properties
%prod.quarkus.redis.load-script=import.redis
```

Before importing in *prod* mode, make sure you configured `quarkus.redis.flush-before-load` accordingly.

> [!IMPORTANT]
> In dev mode, to reload the content of the `.redis` load scripts, you need to add: `%dev.quarkus.vertx.caching=false`

## Use the Vert.x redis client

In addition to the high-level API, you can use the Vertx Redis clients directly in your code. The documentation of the Vert.x Redis Client is available on the [Vert.x Web Site](https://vertx.io/docs/vertx-redis-client/java/).

## Configure Redis hosts programmatically

The `RedisHostsProvider` programmatically provides Redis hosts. This allows for configuration of properties like Redis connection password coming from other sources.

> [!NOTE]
> This is useful as it removes the need to store sensitive data in application.properties.

```java
@ApplicationScoped
@Identifier("hosts-provider") // the name of the host provider
public class ExampleRedisHostProvider implements RedisHostsProvider {
    @Override
    public Set<URI> getHosts() {
        // do stuff to get the host
        String host = "redis://localhost:6379/3";
        return Collections.singleton(URI.create(host));
    }
}
```

The host provider can be used to configure the redis client like shown below

```properties
quarkus.redis.hosts-provider-name=hosts-provider
```

## Customize the Redis options programmatically

You can expose a bean implementing the `io.quarkus.redis.client.RedisOptionsCustomizer` interface to customize the Redis client options. The bean is called for each configured Redis client:

```java
@ApplicationScoped
public static class MyExampleCustomizer implements RedisOptionsCustomizer {

    @Override
    public void customize(String clientName, RedisOptions options) {
        if (clientName.equalsIgnoreCase("my-redis")
                || clientName.equalsIgnoreCase(RedisConfig.DEFAULT_CLIENT_NAME)) {
            // modify the given options
        } else {
            throw new IllegalStateException("Unknown client name: " + clientName);
        }
    }
}
```

### Use the Redis Dev Services

See [Redis Dev Service](/guides/redis-dev-services/index.md).

## Configure Redis observability

<a id="redis-health-check"></a>
### Enable the health checks

If you are using the `quarkus-smallrye-health` extension, `quarkus-redis` will automatically add a readiness health check to validate the connection to the Redis server.

So when you access the `/q/health/ready` endpoint of your application you will have information about the connection validation status.

This behavior can be disabled by setting the `quarkus.redis.health.enabled` property to `false` in your `application.properties`.

### Enable metrics

Redis client metrics are automatically enabled when the application also uses the [`quarkus-micrometer`](/guides/telemetry-micrometer/index.md) extension. Micrometer collects the metrics of all the Redis clients implemented by the application.

As an example, if you export the metrics to Prometheus, you will get:

```text
# HELP redis_commands_duration_seconds The duration of the operations (commands of batches
# TYPE redis_commands_duration_seconds summary
redis_commands_duration_seconds_count{client_name="<default>",} 3.0
redis_commands_duration_seconds_sum{client_name="<default>",} 0.047500042
# HELP redis_commands_duration_seconds_max The duration of the operations (commands of batches
# TYPE redis_commands_duration_seconds_max gauge
redis_commands_duration_seconds_max{client_name="<default>",} 0.033273167
# HELP redis_pool_active The number of resources from the pool currently used
# TYPE redis_pool_active gauge
redis_pool_active{pool_name="<default>",pool_type="redis",} 0.0
# HELP redis_pool_ratio Pool usage ratio
# TYPE redis_pool_ratio gauge
redis_pool_ratio{pool_name="<default>",pool_type="redis",} 0.0
# HELP redis_pool_queue_size Number of pending elements in the waiting queue
# TYPE redis_pool_queue_size gauge
redis_pool_queue_size{pool_name="<default>",pool_type="redis",} 0.0
# HELP redis_commands_failure_total The number of operations (commands or batches) that have been failed
# TYPE redis_commands_failure_total counter
redis_commands_failure_total{client_name="<default>",} 0.0
# HELP redis_commands_success_total The number of operations (commands or batches) that have been executed successfully
# TYPE redis_commands_success_total counter
redis_commands_success_total{client_name="<default>",} 3.0
# HELP redis_pool_idle The number of resources from the pool currently used
# TYPE redis_pool_idle gauge
redis_pool_idle{pool_name="<default>",pool_type="redis",} 6.0
# HELP redis_pool_completed_total Number of times resources from the pool have been acquired
# TYPE redis_pool_completed_total counter
redis_pool_completed_total{pool_name="<default>",pool_type="redis",} 3.0
# HELP redis_commands_count_total The number of operations (commands or batches) executed
# TYPE redis_commands_count_total counter
redis_commands_count_total{client_name="<default>",} 3.0
# HELP redis_pool_usage_seconds Time spent using resources from the pool
# TYPE redis_pool_usage_seconds summary
redis_pool_usage_seconds_count{pool_name="<default>",pool_type="redis",} 3.0
redis_pool_usage_seconds_sum{pool_name="<default>",pool_type="redis",} 0.024381375
# HELP redis_pool_usage_seconds_max Time spent using resources from the pool
# TYPE redis_pool_usage_seconds_max gauge
redis_pool_usage_seconds_max{pool_name="<default>",pool_type="redis",} 0.010671542
# HELP redis_pool_queue_delay_seconds Time spent in the waiting queue before being processed
# TYPE redis_pool_queue_delay_seconds summary
redis_pool_queue_delay_seconds_count{pool_name="<default>",pool_type="redis",} 3.0
redis_pool_queue_delay_seconds_sum{pool_name="<default>",pool_type="redis",} 0.022341249
# HELP redis_pool_queue_delay_seconds_max Time spent in the waiting queue before being processed
# TYPE redis_pool_queue_delay_seconds_max gauge
redis_pool_queue_delay_seconds_max{pool_name="<default>",pool_type="redis",} 0.021926083
```

The Redis client name can be found in the *tags*.

The metrics contain both the Redis connection pool metrics (`redis_pool_*`) and the metrics about the command execution (`redis_commands_*`) such as the number of command, successes, failures, and durations.

### Disable metrics

To disable the Redis client metrics when `quarkus-micrometer` is used, add the following property to the application configuration:

```properties
quarkus.micrometer.binder.redis.enabled=false
```

<a id="redis-configuration-reference"></a>
## Configuration reference

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-redis-client_quarkus-redis-health-enabled"></a> [`quarkus.redis.health.enabled`](#quarkus-redis-client_quarkus-redis-health-enabled) config_property_copy_button:+++quarkus.redis.health.enabled+++[]<br><br>Whether a health check is published in case the smallrye-health extension is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_HEALTH_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-load-script"></a> [`quarkus.redis.load-script`](#quarkus-redis-client_quarkus-redis-load-script) config_property_copy_button:+++quarkus.redis.load-script+++[]<br><br>`quarkus.redis."redis-client-name".load-script` config_property_copy_button:+++quarkus.redis."redis-client-name".load-script+++[]<br><br>A list of files allowing to pre-load data into the Redis server. The file is formatted as follows:<br><br>\* One instruction per line \* Each instruction is a Redis command and its parameter such as `HSET foo field value` \* Parameters can be wrapped into double-quotes if they include spaces \* Parameters can be wrapped into single-quote if they include spaces \* Parameters including double-quotes must be wrapped into single-quotes<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_LOAD_SCRIPT+++[] | list of string | `import.redis in DEV`, ` TEST ; no-file otherwise` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-flush-before-load"></a> [`quarkus.redis.flush-before-load`](#quarkus-redis-client_quarkus-redis-flush-before-load) config_property_copy_button:+++quarkus.redis.flush-before-load+++[]<br><br>`quarkus.redis."redis-client-name".flush-before-load` config_property_copy_button:+++quarkus.redis."redis-client-name".flush-before-load+++[]<br><br>When using `redisLoadScript`, indicates if the Redis database must be flushed (erased) before importing.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_FLUSH_BEFORE_LOAD+++[] | boolean | `true` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-load-only-if-empty"></a> [`quarkus.redis.load-only-if-empty`](#quarkus-redis-client_quarkus-redis-load-only-if-empty) config_property_copy_button:+++quarkus.redis.load-only-if-empty+++[]<br><br>`quarkus.redis."redis-client-name".load-only-if-empty` config_property_copy_button:+++quarkus.redis."redis-client-name".load-only-if-empty+++[]<br><br>When using `redisLoadScript`, indicates if the import should only happen if the database is empty (no keys).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_LOAD_ONLY_IF_EMPTY+++[] | boolean | `true` |
| <a id="quarkus-redis-client_quarkus-redis-active"></a> [`quarkus.redis.active`](#quarkus-redis-client_quarkus-redis-active) config_property_copy_button:+++quarkus.redis.active+++[]<br><br>`quarkus.redis."redis-client-name".active` config_property_copy_button:+++quarkus.redis."redis-client-name".active+++[]<br><br>Whether this Redis Client should be active at runtime.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_ACTIVE+++[] | boolean | `true` |
| <a id="quarkus-redis-client_quarkus-redis-hosts"></a> [`quarkus.redis.hosts`](#quarkus-redis-client_quarkus-redis-hosts) config_property_copy_button:+++quarkus.redis.hosts+++[]<br><br>`quarkus.redis."redis-client-name".hosts` config_property_copy_button:+++quarkus.redis."redis-client-name".hosts+++[]<br><br>The Redis hosts to use while connecting to the Redis server. Only the cluster and sentinel modes will consider more than 1 element.<br><br>The URI provided uses the following schema `redis://[username:password@][host][:port][/database]` Use `quarkus.redis.hosts-provider-name` to provide the hosts programmatically.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_HOSTS+++[] | list of [URI](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URI.html) |  |
| <a id="quarkus-redis-client_quarkus-redis-hosts-provider-name"></a> [`quarkus.redis.hosts-provider-name`](#quarkus-redis-client_quarkus-redis-hosts-provider-name) config_property_copy_button:+++quarkus.redis.hosts-provider-name+++[]<br><br>`quarkus.redis."redis-client-name".hosts-provider-name` config_property_copy_button:+++quarkus.redis."redis-client-name".hosts-provider-name+++[]<br><br>The hosts provider bean name.<br><br>It is the `&#64;Named` value of the hosts provider bean. It is used to discriminate if multiple `io.quarkus.redis.client.RedisHostsProvider` beans are available.<br><br>Used when `quarkus.redis.hosts` is not set.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_HOSTS_PROVIDER_NAME+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-timeout"></a> [`quarkus.redis.timeout`](#quarkus-redis-client_quarkus-redis-timeout) config_property_copy_button:+++quarkus.redis.timeout+++[]<br><br>`quarkus.redis."redis-client-name".timeout` config_property_copy_button:+++quarkus.redis."redis-client-name".timeout+++[]<br><br>The maximum delay to wait before a blocking command to Redis server times out<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) | `10S` |
| <a id="quarkus-redis-client_quarkus-redis-client-type"></a> [`quarkus.redis.client-type`](#quarkus-redis-client_quarkus-redis-client-type) config_property_copy_button:+++quarkus.redis.client-type+++[]<br><br>`quarkus.redis."redis-client-name".client-type` config_property_copy_button:+++quarkus.redis."redis-client-name".client-type+++[]<br><br>The Redis client type. Accepted values are: `STANDALONE` (default), `CLUSTER`, `REPLICATION`, `SENTINEL`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_CLIENT_TYPE+++[] | `standalone`, `sentinel`, `cluster`, `replication` | `standalone` |
| <a id="quarkus-redis-client_quarkus-redis-master-name"></a> [`quarkus.redis.master-name`](#quarkus-redis-client_quarkus-redis-master-name) config_property_copy_button:+++quarkus.redis.master-name+++[]<br><br>`quarkus.redis."redis-client-name".master-name` config_property_copy_button:+++quarkus.redis."redis-client-name".master-name+++[]<br><br>The master name (only considered in the Sentinel mode).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_MASTER_NAME+++[] | string | `mymaster` |
| <a id="quarkus-redis-client_quarkus-redis-role"></a> [`quarkus.redis.role`](#quarkus-redis-client_quarkus-redis-role) config_property_copy_button:+++quarkus.redis.role+++[]<br><br>`quarkus.redis."redis-client-name".role` config_property_copy_button:+++quarkus.redis."redis-client-name".role+++[]<br><br>The role name (only considered in the Sentinel mode). Accepted values are: `MASTER`, `REPLICA`, `SENTINEL`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_ROLE+++[] | `master`, `replica`, `sentinel` | `master` |
| <a id="quarkus-redis-client_quarkus-redis-replicas"></a> [`quarkus.redis.replicas`](#quarkus-redis-client_quarkus-redis-replicas) config_property_copy_button:+++quarkus.redis.replicas+++[]<br><br>`quarkus.redis."redis-client-name".replicas` config_property_copy_button:+++quarkus.redis."redis-client-name".replicas+++[]<br><br>Whether to use replicas nodes (only considered in Cluster mode and Replication mode). Accepted values are: `ALWAYS`, `NEVER`, `SHARE`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_REPLICAS+++[] | `never`, `share`, `always` | `never` |
| <a id="quarkus-redis-client_quarkus-redis-password"></a> [`quarkus.redis.password`](#quarkus-redis-client_quarkus-redis-password) config_property_copy_button:+++quarkus.redis.password+++[]<br><br>`quarkus.redis."redis-client-name".password` config_property_copy_button:+++quarkus.redis."redis-client-name".password+++[]<br><br>The default password for Redis connections.<br><br>If not set, it will try to extract the value from the `hosts`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_PASSWORD+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-max-pool-size"></a> [`quarkus.redis.max-pool-size`](#quarkus-redis-client_quarkus-redis-max-pool-size) config_property_copy_button:+++quarkus.redis.max-pool-size+++[]<br><br>`quarkus.redis."redis-client-name".max-pool-size` config_property_copy_button:+++quarkus.redis."redis-client-name".max-pool-size+++[]<br><br>The maximum size of the connection pool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_MAX_POOL_SIZE+++[] | int | `6` |
| <a id="quarkus-redis-client_quarkus-redis-max-pool-waiting"></a> [`quarkus.redis.max-pool-waiting`](#quarkus-redis-client_quarkus-redis-max-pool-waiting) config_property_copy_button:+++quarkus.redis.max-pool-waiting+++[]<br><br>`quarkus.redis."redis-client-name".max-pool-waiting` config_property_copy_button:+++quarkus.redis."redis-client-name".max-pool-waiting+++[]<br><br>The maximum waiting requests for a connection from the pool.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_MAX_POOL_WAITING+++[] | int | `24` |
| <a id="quarkus-redis-client_quarkus-redis-pool-cleaner-interval"></a> [`quarkus.redis.pool-cleaner-interval`](#quarkus-redis-client_quarkus-redis-pool-cleaner-interval) config_property_copy_button:+++quarkus.redis.pool-cleaner-interval+++[]<br><br>`quarkus.redis."redis-client-name".pool-cleaner-interval` config_property_copy_button:+++quarkus.redis."redis-client-name".pool-cleaner-interval+++[]<br><br>The duration indicating how often should the connection pool cleaner execute.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_POOL_CLEANER_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) | `30s` |
| <a id="quarkus-redis-client_quarkus-redis-pool-recycle-timeout"></a> [`quarkus.redis.pool-recycle-timeout`](#quarkus-redis-client_quarkus-redis-pool-recycle-timeout) config_property_copy_button:+++quarkus.redis.pool-recycle-timeout+++[]<br><br>`quarkus.redis."redis-client-name".pool-recycle-timeout` config_property_copy_button:+++quarkus.redis."redis-client-name".pool-recycle-timeout+++[]<br><br>The timeout for unused connection recycling.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_POOL_RECYCLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) | `3M` |
| <a id="quarkus-redis-client_quarkus-redis-max-waiting-handlers"></a> [`quarkus.redis.max-waiting-handlers`](#quarkus-redis-client_quarkus-redis-max-waiting-handlers) config_property_copy_button:+++quarkus.redis.max-waiting-handlers+++[]<br><br>`quarkus.redis."redis-client-name".max-waiting-handlers` config_property_copy_button:+++quarkus.redis."redis-client-name".max-waiting-handlers+++[]<br><br>Sets how many handlers is the client willing to queue.<br><br>The client will always work on pipeline mode, this means that messages can start queueing. Using this configuration option, you can control how much backlog you're willing to accept.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_MAX_WAITING_HANDLERS+++[] | int | `2048` |
| <a id="quarkus-redis-client_quarkus-redis-max-nested-arrays"></a> [`quarkus.redis.max-nested-arrays`](#quarkus-redis-client_quarkus-redis-max-nested-arrays) config_property_copy_button:+++quarkus.redis.max-nested-arrays+++[]<br><br>`quarkus.redis."redis-client-name".max-nested-arrays` config_property_copy_button:+++quarkus.redis."redis-client-name".max-nested-arrays+++[]<br><br>Tune how much nested arrays are allowed on a Redis response. This affects the parser performance.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_MAX_NESTED_ARRAYS+++[] | int | `32` |
| <a id="quarkus-redis-client_quarkus-redis-reconnect-attempts"></a> [`quarkus.redis.reconnect-attempts`](#quarkus-redis-client_quarkus-redis-reconnect-attempts) config_property_copy_button:+++quarkus.redis.reconnect-attempts+++[]<br><br>`quarkus.redis."redis-client-name".reconnect-attempts` config_property_copy_button:+++quarkus.redis."redis-client-name".reconnect-attempts+++[]<br><br>The number of reconnection attempts when a pooled connection cannot be established on first try.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_RECONNECT_ATTEMPTS+++[] | int | `0` |
| <a id="quarkus-redis-client_quarkus-redis-reconnect-interval"></a> [`quarkus.redis.reconnect-interval`](#quarkus-redis-client_quarkus-redis-reconnect-interval) config_property_copy_button:+++quarkus.redis.reconnect-interval+++[]<br><br>`quarkus.redis."redis-client-name".reconnect-interval` config_property_copy_button:+++quarkus.redis."redis-client-name".reconnect-interval+++[]<br><br>The interval between reconnection attempts when a pooled connection cannot be established on first try.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_RECONNECT_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) | `1S` |
| <a id="quarkus-redis-client_quarkus-redis-protocol-negotiation"></a> [`quarkus.redis.protocol-negotiation`](#quarkus-redis-client_quarkus-redis-protocol-negotiation) config_property_copy_button:+++quarkus.redis.protocol-negotiation+++[]<br><br>`quarkus.redis."redis-client-name".protocol-negotiation` config_property_copy_button:+++quarkus.redis."redis-client-name".protocol-negotiation+++[]<br><br>Should the client perform `RESP` protocol negotiation during the connection handshake.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_PROTOCOL_NEGOTIATION+++[] | boolean | `true` |
| <a id="quarkus-redis-client_quarkus-redis-preferred-protocol-version"></a> [`quarkus.redis.preferred-protocol-version`](#quarkus-redis-client_quarkus-redis-preferred-protocol-version) config_property_copy_button:+++quarkus.redis.preferred-protocol-version+++[]<br><br>`quarkus.redis."redis-client-name".preferred-protocol-version` config_property_copy_button:+++quarkus.redis."redis-client-name".preferred-protocol-version+++[]<br><br>The preferred protocol version to be used during protocol negotiation. When not set, defaults to RESP 3. When protocol negotiation is disabled, this setting has no effect.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_PREFERRED_PROTOCOL_VERSION+++[] | `resp2`, `resp3` | `resp3` |
| <a id="quarkus-redis-client_quarkus-redis-topology-cache-ttl"></a> [`quarkus.redis.topology-cache-ttl`](#quarkus-redis-client_quarkus-redis-topology-cache-ttl) config_property_copy_button:+++quarkus.redis.topology-cache-ttl+++[]<br><br>`quarkus.redis."redis-client-name".topology-cache-ttl` config_property_copy_button:+++quarkus.redis."redis-client-name".topology-cache-ttl+++[]<br><br>The TTL of the topology cache. A topology cache is used by a clustered Redis client and a sentinel Redis client to prevent constantly sending topology discovery commands (`CLUSTER SLOTS` or `SENTINEL ...`).<br><br>This setting is only meaningful in case of a clustered Redis client and a sentinel Redis client and has no effect otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TOPOLOGY_CACHE_TTL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) | `1S` |
| <a id="quarkus-redis-client_quarkus-redis-auto-failover"></a> [`quarkus.redis.auto-failover`](#quarkus-redis-client_quarkus-redis-auto-failover) config_property_copy_button:+++quarkus.redis.auto-failover+++[]<br><br>`quarkus.redis."redis-client-name".auto-failover` config_property_copy_button:+++quarkus.redis."redis-client-name".auto-failover+++[]<br><br>Whether automatic failover is enabled. This only makes sense for sentinel clients with role of `MASTER` and is ignored otherwise.<br><br>If enabled, the sentinel client will additionally create a connection to one sentinel node and watch for failover events. When new master is elected, all connections to the old master are automatically closed and new connections to the new master are created. Automatic failover makes sense for connections executing regular commands, but not for connections used to subscribe to Redis pub/sub channels.<br><br>Note that there is a brief period of time between the old master failing and the new master being elected when the existing connections will temporarily fail all operations. After the new master is elected, the connections will automatically fail over and start working again.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_AUTO_FAILOVER+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-topology"></a> [`quarkus.redis.topology`](#quarkus-redis-client_quarkus-redis-topology) config_property_copy_button:+++quarkus.redis.topology+++[]<br><br>`quarkus.redis."redis-client-name".topology` config_property_copy_button:+++quarkus.redis."redis-client-name".topology+++[]<br><br>How the Redis topology is obtained. By default, the topology is discovered automatically. This is the only mode for the clustered and sentinel client. For replication client, topology may be set *statically*.<br><br>In case of a static topology for replication Redis client, the first node in the list is considered a *master* and the remaining nodes in the list are considered *replicas*.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TOPOLOGY+++[] | `discover`, `static` | `discover` |
| <a id="quarkus-redis-client_quarkus-redis-cluster-transactions"></a> [`quarkus.redis.cluster-transactions`](#quarkus-redis-client_quarkus-redis-cluster-transactions) config_property_copy_button:+++quarkus.redis.cluster-transactions+++[]<br><br>`quarkus.redis."redis-client-name".cluster-transactions` config_property_copy_button:+++quarkus.redis."redis-client-name".cluster-transactions+++[]<br><br>How transactions are handled in a cluster. This only makes sense for cluster clients and is ignored otherwise.<br><br>By default, transactions in cluster are disabled and transaction commands fail.<br><br>When set to `single-node`, Redis transactions are supported in the cluster, but only when they target a single node. The `MULTI` command is queued and is only issued when the next command is executed. This next command binds the connection to the corresponding node of the Redis cluster (so it should have keys, otherwise the target node is random). All subsequent commands are targeted to that node. If some of the subsequent commands have a key that belongs to another node, the command will fail on the server side. If `WATCH` is used before `MULTI`, its key(s) determine to which node the connection is bound and the subsequent `MULTI` is not queued. If `WATCH` keys belong to multiple nodes, the command fails on the client side.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_CLUSTER_TRANSACTIONS+++[] | `disabled`, `single-node` | `disabled` |
| <a id="quarkus-redis-client_quarkus-redis-client-name"></a> [`quarkus.redis.client-name`](#quarkus-redis-client_quarkus-redis-client-name) config_property_copy_button:+++quarkus.redis.client-name+++[]<br><br>`quarkus.redis."redis-client-name".client-name` config_property_copy_button:+++quarkus.redis."redis-client-name".client-name+++[]<br><br>The client name used to identify the connection.<br><br>If the `RedisClientConfig#configureClientName()` is enabled, and this property is not set it will attempt to extract the value from the `RedisClientName#value()` annotation.<br><br>If the `RedisClientConfig#configureClientName()` is enabled, both this property and the `RedisClientName#value()` must adhere to the pattern '++\[++a-zA-Z0-9++ \\++-\_.\~++\]\*++'; if not, this may result in an incorrect client name after URI encoding.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_CLIENT_NAME+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-configure-client-name"></a> [`quarkus.redis.configure-client-name`](#quarkus-redis-client_quarkus-redis-configure-client-name) config_property_copy_button:+++quarkus.redis.configure-client-name+++[]<br><br>`quarkus.redis."redis-client-name".configure-client-name` config_property_copy_button:+++quarkus.redis."redis-client-name".configure-client-name+++[]<br><br>Whether it should set the client name while connecting with Redis.<br><br>This is necessary because Redis only accepts `client=my-client-name` query parameter in version 6{plus}.<br><br>This property can be used with `RedisClientConfig#clientName()` configuration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_CONFIGURE_CLIENT_NAME+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-configuration-name"></a> [`quarkus.redis.tls-configuration-name`](#quarkus-redis-client_quarkus-redis-tls-configuration-name) config_property_copy_button:+++quarkus.redis.tls-configuration-name+++[]<br><br>`quarkus.redis."redis-client-name".tls-configuration-name` config_property_copy_button:+++quarkus.redis."redis-client-name".tls-configuration-name+++[]<br><br>The name of the TLS configuration to use.<br><br>If a name is configured, it uses the configuration from `quarkus.tls.<name>.*` If a name is configured, but no TLS configuration is found with that name then an error will be thrown.<br><br>If no TLS configuration name is set then, `quarkus.redis.$client-name.tls` will be used.<br><br>The default TLS configuration is **not** used by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-hash-slot-cache-ttl"></a> <mark>link:#quarkus-redis-client_quarkus-redis-hash-slot-cache-ttl\[`quarkus.redis.hash-slot-cache-ttl`\]</mark> config_property_copy_button:+++quarkus.redis.hash-slot-cache-ttl+++[]<br><br>`quarkus.redis."redis-client-name".hash-slot-cache-ttl` config_property_copy_button:+++quarkus.redis."redis-client-name".hash-slot-cache-ttl+++[]<br><br>*This property is deprecated since `3.30`: use `quarkus.redis.topology-cache-ttl`.*<br><br>The TTL of the topology cache. A topology cache is used by a clustered Redis client and a sentinel Redis client to prevent constantly sending topology discovery commands (`CLUSTER SLOTS` or `SENTINEL ...`).<br><br>This setting is only meaningful in case of a clustered Redis client and a sentinel Redis client and has no effect otherwise.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_HASH_SLOT_CACHE_TTL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) | `1S` |
| <a id="quarkus-redis-client_section_quarkus-redis-devservices"></a> [Dev Services allows Quarkus to automatically start Redis in dev and test mode](#quarkus-redis-client_section_quarkus-redis-devservices) | Type | Default |
| [lock] <a id="quarkus-redis-client_quarkus-redis-devservices-enabled"></a> [`quarkus.redis.devservices.enabled`](#quarkus-redis-client_quarkus-redis-devservices-enabled) config_property_copy_button:+++quarkus.redis.devservices.enabled+++[]<br><br>`quarkus.redis."redis-client-name".devservices.enabled` config_property_copy_button:+++quarkus.redis."redis-client-name".devservices.enabled+++[]<br><br>If DevServices has been explicitly enabled or disabled. DevServices is generally enabled by default, unless there is an existing configuration present.<br><br>When DevServices is enabled Quarkus will attempt to automatically configure and start a database when running in Dev or Test mode and when Docker is running.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_DEVSERVICES_ENABLED+++[] | boolean | `true` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-devservices-image-name"></a> [`quarkus.redis.devservices.image-name`](#quarkus-redis-client_quarkus-redis-devservices-image-name) config_property_copy_button:+++quarkus.redis.devservices.image-name+++[]<br><br>`quarkus.redis."redis-client-name".devservices.image-name` config_property_copy_button:+++quarkus.redis."redis-client-name".devservices.image-name+++[]<br><br>The container image name to use, for container based DevServices providers. If you want to use Redis Stack modules (bloom, graph, search...), use: `redis/redis-stack:latest`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_DEVSERVICES_IMAGE_NAME+++[] | string | `docker.io/library/redis:7` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-devservices-port"></a> [`quarkus.redis.devservices.port`](#quarkus-redis-client_quarkus-redis-devservices-port) config_property_copy_button:+++quarkus.redis.devservices.port+++[]<br><br>`quarkus.redis."redis-client-name".devservices.port` config_property_copy_button:+++quarkus.redis."redis-client-name".devservices.port+++[]<br><br>Optional fixed port the dev service will listen to.<br><br>If not defined, the port will be chosen randomly.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_DEVSERVICES_PORT+++[] | int |  |
| [lock] <a id="quarkus-redis-client_quarkus-redis-devservices-shared"></a> [`quarkus.redis.devservices.shared`](#quarkus-redis-client_quarkus-redis-devservices-shared) config_property_copy_button:+++quarkus.redis.devservices.shared+++[]<br><br>`quarkus.redis."redis-client-name".devservices.shared` config_property_copy_button:+++quarkus.redis."redis-client-name".devservices.shared+++[]<br><br>Indicates if the Redis server managed by Quarkus Dev Services is shared. When shared, Quarkus looks for running containers using label-based service discovery. If a matching container is found, it is used, and so a second one is not started. Otherwise, Dev Services for Redis starts a new container.<br><br>The discovery uses the `quarkus-dev-service-redis` label. The value is configured using the `service-name` property.<br><br>Container sharing is only used in dev mode.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_DEVSERVICES_SHARED+++[] | boolean | `true` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-devservices-service-name"></a> [`quarkus.redis.devservices.service-name`](#quarkus-redis-client_quarkus-redis-devservices-service-name) config_property_copy_button:+++quarkus.redis.devservices.service-name+++[]<br><br>`quarkus.redis."redis-client-name".devservices.service-name` config_property_copy_button:+++quarkus.redis."redis-client-name".devservices.service-name+++[]<br><br>The value of the `quarkus-dev-service-redis` label attached to the started container. This property is used when `shared` is set to `true`. In this case, before starting a container, Dev Services for Redis looks for a container with the `quarkus-dev-service-redis` label set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it starts a new container with the `quarkus-dev-service-redis` label set to the specified value.<br><br>This property is used when you need multiple shared Redis servers.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_DEVSERVICES_SERVICE_NAME+++[] | string | `redis` |
| [lock] <a id="quarkus-redis-client_quarkus-redis-devservices-container-env-environment-variable-name"></a> [`quarkus.redis.devservices.container-env."environment-variable-name"`](#quarkus-redis-client_quarkus-redis-devservices-container-env-environment-variable-name) config_property_copy_button:+++quarkus.redis.devservices.container-env."environment-variable-name"+++[]<br><br>`quarkus.redis."redis-client-name".devservices.container-env."environment-variable-name"` config_property_copy_button:+++quarkus.redis."redis-client-name".devservices.container-env."environment-variable-name"+++[]<br><br>Environment variables that are passed to the container.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_DEVSERVICES_CONTAINER_ENV__ENVIRONMENT_VARIABLE_NAME_+++[] | Map\<String,String> |  |
| <a id="quarkus-redis-client_section_quarkus-redis-tcp"></a> [TCP config](#quarkus-redis-client_section_quarkus-redis-tcp) | Type | Default |
| <a id="quarkus-redis-client_quarkus-redis-tcp-alpn"></a> [`quarkus.redis.tcp.alpn`](#quarkus-redis-client_quarkus-redis-tcp-alpn) config_property_copy_button:+++quarkus.redis.tcp.alpn+++[]<br><br>`quarkus.redis."redis-client-name".tcp.alpn` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.alpn+++[]<br><br>Set the ALPN usage.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_ALPN+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-application-layer-protocols"></a> [`quarkus.redis.tcp.application-layer-protocols`](#quarkus-redis-client_quarkus-redis-tcp-application-layer-protocols) config_property_copy_button:+++quarkus.redis.tcp.application-layer-protocols+++[]<br><br>`quarkus.redis."redis-client-name".tcp.application-layer-protocols` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.application-layer-protocols+++[]<br><br>Sets the list of application-layer protocols to provide to the server during the `Application-Layer Protocol Negotiation`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_APPLICATION_LAYER_PROTOCOLS+++[] | list of string |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-secure-transport-protocols"></a> [`quarkus.redis.tcp.secure-transport-protocols`](#quarkus-redis-client_quarkus-redis-tcp-secure-transport-protocols) config_property_copy_button:+++quarkus.redis.tcp.secure-transport-protocols+++[]<br><br>`quarkus.redis."redis-client-name".tcp.secure-transport-protocols` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.secure-transport-protocols+++[]<br><br>Sets the list of enabled SSL/TLS protocols.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_SECURE_TRANSPORT_PROTOCOLS+++[] | list of string |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-idle-timeout"></a> [`quarkus.redis.tcp.idle-timeout`](#quarkus-redis-client_quarkus-redis-tcp-idle-timeout) config_property_copy_button:+++quarkus.redis.tcp.idle-timeout+++[]<br><br>`quarkus.redis."redis-client-name".tcp.idle-timeout` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.idle-timeout+++[]<br><br>Set the idle timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-connection-timeout"></a> [`quarkus.redis.tcp.connection-timeout`](#quarkus-redis-client_quarkus-redis-tcp-connection-timeout) config_property_copy_button:+++quarkus.redis.tcp.connection-timeout+++[]<br><br>`quarkus.redis."redis-client-name".tcp.connection-timeout` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.connection-timeout+++[]<br><br>Set the connect timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_CONNECTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-proxy-configuration-name"></a> [`quarkus.redis.tcp.proxy-configuration-name`](#quarkus-redis-client_quarkus-redis-tcp-proxy-configuration-name) config_property_copy_button:+++quarkus.redis.tcp.proxy-configuration-name+++[]<br><br>`quarkus.redis."redis-client-name".tcp.proxy-configuration-name` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.proxy-configuration-name+++[]<br><br>The name of the proxy configuration to use.<br><br>If `quarkus.redis.tcp.proxy-options.host` is set, the `quarkus.redis.tcp.proxy-options.*` (and `quarkus.redis.tcp.non-proxy-hosts`) configuration is used and the `quarkus.proxy.*` configuration is ignored.<br><br>Otherwise, if not set and the default proxy configuration is configured (`quarkus.proxy.*`), it is used. If set to `none`, no proxy configuration is used. If set to some other value, the configuration from `quarkus.proxy.<name>.*` is used. If set but no proxy configuration with that name is found, an exception is thrown at runtime.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_PROXY_CONFIGURATION_NAME+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-read-idle-timeout"></a> [`quarkus.redis.tcp.read-idle-timeout`](#quarkus-redis-client_quarkus-redis-tcp-read-idle-timeout) config_property_copy_button:+++quarkus.redis.tcp.read-idle-timeout+++[]<br><br>`quarkus.redis."redis-client-name".tcp.read-idle-timeout` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.read-idle-timeout+++[]<br><br>Set the read idle timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_READ_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-receive-buffer-size"></a> [`quarkus.redis.tcp.receive-buffer-size`](#quarkus-redis-client_quarkus-redis-tcp-receive-buffer-size) config_property_copy_button:+++quarkus.redis.tcp.receive-buffer-size+++[]<br><br>`quarkus.redis."redis-client-name".tcp.receive-buffer-size` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.receive-buffer-size+++[]<br><br>Set the TCP receive buffer size.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_RECEIVE_BUFFER_SIZE+++[] | int |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-reconnect-attempts"></a> [`quarkus.redis.tcp.reconnect-attempts`](#quarkus-redis-client_quarkus-redis-tcp-reconnect-attempts) config_property_copy_button:+++quarkus.redis.tcp.reconnect-attempts+++[]<br><br>`quarkus.redis."redis-client-name".tcp.reconnect-attempts` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.reconnect-attempts+++[]<br><br>Set the value of reconnect attempts.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_RECONNECT_ATTEMPTS+++[] | int |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-reconnect-interval"></a> [`quarkus.redis.tcp.reconnect-interval`](#quarkus-redis-client_quarkus-redis-tcp-reconnect-interval) config_property_copy_button:+++quarkus.redis.tcp.reconnect-interval+++[]<br><br>`quarkus.redis."redis-client-name".tcp.reconnect-interval` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.reconnect-interval+++[]<br><br>Set the reconnect interval.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_RECONNECT_INTERVAL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-reuse-address"></a> [`quarkus.redis.tcp.reuse-address`](#quarkus-redis-client_quarkus-redis-tcp-reuse-address) config_property_copy_button:+++quarkus.redis.tcp.reuse-address+++[]<br><br>`quarkus.redis."redis-client-name".tcp.reuse-address` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.reuse-address+++[]<br><br>Whether to reuse the address.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_REUSE_ADDRESS+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-reuse-port"></a> [`quarkus.redis.tcp.reuse-port`](#quarkus-redis-client_quarkus-redis-tcp-reuse-port) config_property_copy_button:+++quarkus.redis.tcp.reuse-port+++[]<br><br>`quarkus.redis."redis-client-name".tcp.reuse-port` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.reuse-port+++[]<br><br>Whether to reuse the port.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_REUSE_PORT+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-send-buffer-size"></a> [`quarkus.redis.tcp.send-buffer-size`](#quarkus-redis-client_quarkus-redis-tcp-send-buffer-size) config_property_copy_button:+++quarkus.redis.tcp.send-buffer-size+++[]<br><br>`quarkus.redis."redis-client-name".tcp.send-buffer-size` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.send-buffer-size+++[]<br><br>Set the TCP send buffer size.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_SEND_BUFFER_SIZE+++[] | int |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-so-linger"></a> [`quarkus.redis.tcp.so-linger`](#quarkus-redis-client_quarkus-redis-tcp-so-linger) config_property_copy_button:+++quarkus.redis.tcp.so-linger+++[]<br><br>`quarkus.redis."redis-client-name".tcp.so-linger` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.so-linger+++[]<br><br>Set the `SO_linger` keep alive duration.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_SO_LINGER+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-cork"></a> [`quarkus.redis.tcp.cork`](#quarkus-redis-client_quarkus-redis-tcp-cork) config_property_copy_button:+++quarkus.redis.tcp.cork+++[]<br><br>`quarkus.redis."redis-client-name".tcp.cork` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.cork+++[]<br><br>Enable the `TCP_CORK` option - only with linux native transport.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_CORK+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-fast-open"></a> [`quarkus.redis.tcp.fast-open`](#quarkus-redis-client_quarkus-redis-tcp-fast-open) config_property_copy_button:+++quarkus.redis.tcp.fast-open+++[]<br><br>`quarkus.redis."redis-client-name".tcp.fast-open` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.fast-open+++[]<br><br>Enable the `TCP_FASTOPEN` option - only with linux native transport.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_FAST_OPEN+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-keep-alive"></a> [`quarkus.redis.tcp.keep-alive`](#quarkus-redis-client_quarkus-redis-tcp-keep-alive) config_property_copy_button:+++quarkus.redis.tcp.keep-alive+++[]<br><br>`quarkus.redis."redis-client-name".tcp.keep-alive` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.keep-alive+++[]<br><br>Set whether keep alive is enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_KEEP_ALIVE+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-no-delay"></a> [`quarkus.redis.tcp.no-delay`](#quarkus-redis-client_quarkus-redis-tcp-no-delay) config_property_copy_button:+++quarkus.redis.tcp.no-delay+++[]<br><br>`quarkus.redis."redis-client-name".tcp.no-delay` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.no-delay+++[]<br><br>Set whether no delay is enabled<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_NO_DELAY+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-quick-ack"></a> [`quarkus.redis.tcp.quick-ack`](#quarkus-redis-client_quarkus-redis-tcp-quick-ack) config_property_copy_button:+++quarkus.redis.tcp.quick-ack+++[]<br><br>`quarkus.redis."redis-client-name".tcp.quick-ack` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.quick-ack+++[]<br><br>Enable the `TCP_QUICKACK` option - only with linux native transport.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_QUICK_ACK+++[] | boolean |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-traffic-class"></a> [`quarkus.redis.tcp.traffic-class`](#quarkus-redis-client_quarkus-redis-tcp-traffic-class) config_property_copy_button:+++quarkus.redis.tcp.traffic-class+++[]<br><br>`quarkus.redis."redis-client-name".tcp.traffic-class` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.traffic-class+++[]<br><br>Set the value of traffic class.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_TRAFFIC_CLASS+++[] | int |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-write-idle-timeout"></a> [`quarkus.redis.tcp.write-idle-timeout`](#quarkus-redis-client_quarkus-redis-tcp-write-idle-timeout) config_property_copy_button:+++quarkus.redis.tcp.write-idle-timeout+++[]<br><br>`quarkus.redis."redis-client-name".tcp.write-idle-timeout` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.write-idle-timeout+++[]<br><br>Set the write idle timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_WRITE_IDLE_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-client_quarkus-redis) |  |
| <a id="quarkus-redis-client_quarkus-redis-tcp-local-address"></a> [`quarkus.redis.tcp.local-address`](#quarkus-redis-client_quarkus-redis-tcp-local-address) config_property_copy_button:+++quarkus.redis.tcp.local-address+++[]<br><br>`quarkus.redis."redis-client-name".tcp.local-address` config_property_copy_button:+++quarkus.redis."redis-client-name".tcp.local-address+++[]<br><br>Set the local interface to bind for network connections. When the local address is null, it will pick any local address, the default local address is null.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TCP_LOCAL_ADDRESS+++[] | string |  |
| <a id="quarkus-redis-client_section_quarkus-redis-tls"></a> [SSL/TLS config](#quarkus-redis-client_section_quarkus-redis-tls) | Type | Default |
| <a id="quarkus-redis-client_quarkus-redis-tls-enabled"></a> [`quarkus.redis.tls.enabled`](#quarkus-redis-client_quarkus-redis-tls-enabled) config_property_copy_button:+++quarkus.redis.tls.enabled+++[]<br><br>`quarkus.redis."redis-client-name".tls.enabled` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.enabled+++[]<br><br>Whether SSL/TLS is enabled.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_ENABLED+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-all"></a> [`quarkus.redis.tls.trust-all`](#quarkus-redis-client_quarkus-redis-tls-trust-all) config_property_copy_button:+++quarkus.redis.tls.trust-all+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-all` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-all+++[]<br><br>Enable trusting all certificates. Disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_ALL+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-pem"></a> [`quarkus.redis.tls.trust-certificate-pem`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-pem) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-pem+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-pem` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-pem+++[]<br><br>PEM Trust config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_PEM+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-pem-certs"></a> [`quarkus.redis.tls.trust-certificate-pem.certs`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-pem-certs) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-pem.certs+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-pem.certs` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-pem.certs+++[]<br><br>Comma-separated list of the trust certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_PEM_CERTS+++[] | list of string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-jks"></a> [`quarkus.redis.tls.trust-certificate-jks`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-jks) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-jks+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-jks` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-jks+++[]<br><br>JKS config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_JKS+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-jks-path"></a> [`quarkus.redis.tls.trust-certificate-jks.path`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-jks-path) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-jks.path+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-jks.path` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-jks.path+++[]<br><br>Path of the key file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_JKS_PATH+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-jks-password"></a> [`quarkus.redis.tls.trust-certificate-jks.password`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-jks-password) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-jks.password+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-jks.password` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-jks.password+++[]<br><br>Password of the key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-pfx"></a> [`quarkus.redis.tls.trust-certificate-pfx`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-pfx) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-pfx+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-pfx` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-pfx+++[]<br><br>PFX config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_PFX+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-pfx-path"></a> [`quarkus.redis.tls.trust-certificate-pfx.path`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-pfx-path) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-pfx.path+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-pfx.path` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-pfx.path+++[]<br><br>Path to the key file (PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_PFX_PATH+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-trust-certificate-pfx-password"></a> [`quarkus.redis.tls.trust-certificate-pfx.password`](#quarkus-redis-client_quarkus-redis-tls-trust-certificate-pfx-password) config_property_copy_button:+++quarkus.redis.tls.trust-certificate-pfx.password+++[]<br><br>`quarkus.redis."redis-client-name".tls.trust-certificate-pfx.password` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.trust-certificate-pfx.password+++[]<br><br>Password of the key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_TRUST_CERTIFICATE_PFX_PASSWORD+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-pem"></a> [`quarkus.redis.tls.key-certificate-pem`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-pem) config_property_copy_button:+++quarkus.redis.tls.key-certificate-pem+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-pem` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-pem+++[]<br><br>PEM Key/cert config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_PEM+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-pem-keys"></a> [`quarkus.redis.tls.key-certificate-pem.keys`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-pem-keys) config_property_copy_button:+++quarkus.redis.tls.key-certificate-pem.keys+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-pem.keys` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-pem.keys+++[]<br><br>Comma-separated list of the path to the key files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_PEM_KEYS+++[] | list of string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-pem-certs"></a> [`quarkus.redis.tls.key-certificate-pem.certs`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-pem-certs) config_property_copy_button:+++quarkus.redis.tls.key-certificate-pem.certs+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-pem.certs` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-pem.certs+++[]<br><br>Comma-separated list of the path to the certificate files (Pem format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_PEM_CERTS+++[] | list of string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-jks"></a> [`quarkus.redis.tls.key-certificate-jks`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-jks) config_property_copy_button:+++quarkus.redis.tls.key-certificate-jks+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-jks` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-jks+++[]<br><br>JKS config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_JKS+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-jks-path"></a> [`quarkus.redis.tls.key-certificate-jks.path`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-jks-path) config_property_copy_button:+++quarkus.redis.tls.key-certificate-jks.path+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-jks.path` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-jks.path+++[]<br><br>Path of the key file (JKS format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_JKS_PATH+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-jks-password"></a> [`quarkus.redis.tls.key-certificate-jks.password`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-jks-password) config_property_copy_button:+++quarkus.redis.tls.key-certificate-jks.password+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-jks.password` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-jks.password+++[]<br><br>Password of the key file.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_JKS_PASSWORD+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-pfx"></a> [`quarkus.redis.tls.key-certificate-pfx`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-pfx) config_property_copy_button:+++quarkus.redis.tls.key-certificate-pfx+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-pfx` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-pfx+++[]<br><br>PFX config is disabled by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_PFX+++[] | boolean | `false` |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-pfx-path"></a> [`quarkus.redis.tls.key-certificate-pfx.path`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-pfx-path) config_property_copy_button:+++quarkus.redis.tls.key-certificate-pfx.path+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-pfx.path` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-pfx.path+++[]<br><br>Path to the key file (PFX format).<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_PFX_PATH+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-key-certificate-pfx-password"></a> [`quarkus.redis.tls.key-certificate-pfx.password`](#quarkus-redis-client_quarkus-redis-tls-key-certificate-pfx-password) config_property_copy_button:+++quarkus.redis.tls.key-certificate-pfx.password+++[]<br><br>`quarkus.redis."redis-client-name".tls.key-certificate-pfx.password` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.key-certificate-pfx.password+++[]<br><br>Password of the key.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_KEY_CERTIFICATE_PFX_PASSWORD+++[] | string |  |
| <a id="quarkus-redis-client_quarkus-redis-tls-hostname-verification-algorithm"></a> [`quarkus.redis.tls.hostname-verification-algorithm`](#quarkus-redis-client_quarkus-redis-tls-hostname-verification-algorithm) config_property_copy_button:+++quarkus.redis.tls.hostname-verification-algorithm+++[]<br><br>`quarkus.redis."redis-client-name".tls.hostname-verification-algorithm` config_property_copy_button:+++quarkus.redis."redis-client-name".tls.hostname-verification-algorithm+++[]<br><br>The hostname verification algorithm to use in case the server's identity should be checked. Should be `HTTPS`, `LDAPS` or `NONE` (default).<br><br>If set to `NONE`, it does not verify the hostname.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_REDIS_TLS_HOSTNAME_VERIFICATION_ALGORITHM+++[] | string | `NONE` |

<a id="duration-note-anchor-quarkus-redis-client_quarkus-redis"></a>

> [!NOTE]
> **About the Duration format**
>
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() Java API documentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
>
> You can also use a simplified format, starting with a number:
>
> - If the value is only a number, it represents time in seconds.
> - If the value is a number followed by `ms`, it represents time in milliseconds.
>
> In other cases, the simplified format is translated to the `java.time.Duration` format for parsing:
>
> - If the value is a number followed by `h`, `m`, or `s`, it is prefixed with `PT`.
> - If the value is a number followed by `d`, it is prefixed with `P`.
