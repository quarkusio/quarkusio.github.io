# Redis Cache

By default, Quarkus Cache uses Caffeine as backend. It's possible to use Redis instead.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Redis as cache backend

When using Redis as the backend for Quarkus cache, each cached item will be stored in Redis:

- The backend uses the *\<default>* Redis client (if not configured otherwise), so make sure it's configured (or use the [Redis Dev Service](/guides/redis-dev-services/index.md))
- the Redis key is built as follows: `cache:{cache-name}:{cache-key}`, where `cache-key` is the key the application uses and `cache:{cache-name}` the prefix.
- the value is encoded to JSON if needed

## Use the Redis backend

First, you need to add the `quarkus-redis-cache` extension to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-redis-cache</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-redis-cache")
```

Then, use the `@CacheResult` and others cache annotations as explained in the [Quarkus Cache guide](/guides/cache/index.md):

```java
@GET
@Path("/{keyElement1}/{keyElement2}/{keyElement3}")
@CacheResult(cacheName = "expensiveResourceCache")
public ExpensiveResponse getExpensiveResponse(@PathParam("keyElement1") @CacheKey String keyElement1,
        @PathParam("keyElement2") @CacheKey String keyElement2, @PathParam("keyElement3") @CacheKey String keyElement3,
        @QueryParam("foo") String foo) {
    invocations.incrementAndGet();
    ExpensiveResponse response = new ExpensiveResponse();
    response.setResult(keyElement1 + " " + keyElement2 + " " + keyElement3 + " too!");
    return response;
}

@POST
@CacheInvalidateAll(cacheName = "expensiveResourceCache")
public void invalidateAll() {

}
```

<a id="redis-cache-configuration-reference"></a>
## Configure the Redis backend

The Redis backend uses the `<default>` Redis client. See the [Redis reference](/guides/redis-reference/index.md) to configure the access to Redis.

> [!TIP]
> In dev mode, you can use the [Redis Dev Service](/guides/redis-dev-services/index.md).

If you want to use another Redis for your cache, configure the `client-name` as follows:

```properties
quarkus.cache.redis.client-name=my-redis-for-cache
```

When writing to Redis or reading from Redis, Quarkus needs to know the type. Indeed, the objects need to be serialized and deserialized. For that purpose, you may need to configure type (class names) of the key and value you want to cache. At build time, Quarkus tries to deduce the types from the application code, but that decision can be overridden using:

```properties
# Default configuration
quarkus.cache.redis.key-type=java.lang.String
quarkus.cache.redis.value-type=org.acme.Person

# Configuration for `expensiveResourceCache`
quarkus.cache.redis.expensiveResourceCache.key-type=java.lang.String
quarkus.cache.redis.expensiveResourceCache.value-type=org.acme.Supes
```

You can also configure the time to live of the cached entries:

```properties
# Default configuration
quarkus.cache.redis.expire-after-write=10s

# Configuration for `expensiveResourceCache`
quarkus.cache.redis.expensiveResourceCache.expire-after-write=1h
```

If the `expire-after-write` is not configured, the entry won't be evicted. You would need to invalidate the values using the `@CacheInvalidateAll` or `@CacheInvalidate` annotations.

The following table lists the supported properties:

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-redis-cache_quarkus-cache-redis-client-name"></a> [`quarkus.cache.redis.client-name`](#quarkus-redis-cache_quarkus-cache-redis-client-name) config_property_copy_button:+++quarkus.cache.redis.client-name+++[]<br><br>The name of the named Redis client to be used for communicating with Redis. If not set, use the default Redis client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_CLIENT_NAME+++[] | string |  |
| [lock] <a id="quarkus-redis-cache_quarkus-cache-redis-value-type"></a> [`quarkus.cache.redis.value-type`](#quarkus-redis-cache_quarkus-cache-redis-value-type) config_property_copy_button:+++quarkus.cache.redis.value-type+++[]<br><br>The default type of the value stored in the cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_VALUE_TYPE+++[] | string |  |
| [lock] <a id="quarkus-redis-cache_quarkus-cache-redis-key-type"></a> [`quarkus.cache.redis.key-type`](#quarkus-redis-cache_quarkus-cache-redis-key-type) config_property_copy_button:+++quarkus.cache.redis.key-type+++[]<br><br>The key type, `String` by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_KEY_TYPE+++[] | string |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-expire-after-write"></a> [`quarkus.cache.redis.expire-after-write`](#quarkus-redis-cache_quarkus-cache-redis-expire-after-write) config_property_copy_button:+++quarkus.cache.redis.expire-after-write+++[]<br><br>Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the entry's creation, or the most recent replacement of its value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_EXPIRE_AFTER_WRITE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-cache_quarkus-cache) |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-expire-after-access"></a> [`quarkus.cache.redis.expire-after-access`](#quarkus-redis-cache_quarkus-cache-redis-expire-after-access) config_property_copy_button:+++quarkus.cache.redis.expire-after-access+++[]<br><br>Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the last access of its value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_EXPIRE_AFTER_ACCESS+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-cache_quarkus-cache) |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-prefix"></a> [`quarkus.cache.redis.prefix`](#quarkus-redis-cache_quarkus-cache-redis-prefix) config_property_copy_button:+++quarkus.cache.redis.prefix+++[]<br><br>The key prefix allowing to identify the keys belonging to the cache. If not set, the value "`cache:{cache-name`++}++" will be used. The variable "`{cache-name`++}++" is resolved from the value set in the cache annotations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_PREFIX+++[] | string |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-use-optimistic-locking"></a> [`quarkus.cache.redis.use-optimistic-locking`](#quarkus-redis-cache_quarkus-cache-redis-use-optimistic-locking) config_property_copy_button:+++quarkus.cache.redis.use-optimistic-locking+++[]<br><br>Whether the access to the cache should be using optimistic locking. See [Redis Optimistic Locking](https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set) for details. Default is `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_USE_OPTIMISTIC_LOCKING+++[] | boolean |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-invalidation-scan-size"></a> [`quarkus.cache.redis.invalidation-scan-size`](#quarkus-redis-cache_quarkus-cache-redis-invalidation-scan-size) config_property_copy_button:+++quarkus.cache.redis.invalidation-scan-size+++[]<br><br>If set, the `SCAN` command (used to implement invalidation) will have the `COUNT` argument with given value. If not set (default), no `COUNT` argument is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_INVALIDATION_SCAN_SIZE+++[] | int |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-ttl"></a> <mark>link:#quarkus-redis-cache_quarkus-cache-redis-ttl\[`quarkus.cache.redis.ttl`\]</mark> config_property_copy_button:+++quarkus.cache.redis.ttl+++[]<br><br>*This property is deprecated since `3.3.0`: Use `expire-after-write` instead. This will be removed after Quarkus 3.20.*<br><br>The default time to live of the item stored in the cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS_TTL+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-cache_quarkus-cache) |  |
| <a id="quarkus-redis-cache_section_quarkus-cache-redis"></a> [Additional configuration applied to a specific Redis cache (highest precedence)](#quarkus-redis-cache_section_quarkus-cache-redis) | Type | Default |
| [lock] <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-value-type"></a> [`quarkus.cache.redis."cache-name".value-type`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-value-type) config_property_copy_button:+++quarkus.cache.redis."cache-name".value-type+++[]<br><br>The default type of the value stored in the cache.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__VALUE_TYPE+++[] | string |  |
| [lock] <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-key-type"></a> [`quarkus.cache.redis."cache-name".key-type`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-key-type) config_property_copy_button:+++quarkus.cache.redis."cache-name".key-type+++[]<br><br>The key type, `String` by default.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__KEY_TYPE+++[] | string |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-expire-after-write"></a> [`quarkus.cache.redis."cache-name".expire-after-write`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-expire-after-write) config_property_copy_button:+++quarkus.cache.redis."cache-name".expire-after-write+++[]<br><br>Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the entry's creation, or the most recent replacement of its value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__EXPIRE_AFTER_WRITE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-cache_quarkus-cache) |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-expire-after-access"></a> [`quarkus.cache.redis."cache-name".expire-after-access`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-expire-after-access) config_property_copy_button:+++quarkus.cache.redis."cache-name".expire-after-access+++[]<br><br>Specifies that each entry should be automatically removed from the cache once a fixed duration has elapsed after the last access of its value.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__EXPIRE_AFTER_ACCESS+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-redis-cache_quarkus-cache) |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-prefix"></a> [`quarkus.cache.redis."cache-name".prefix`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-prefix) config_property_copy_button:+++quarkus.cache.redis."cache-name".prefix+++[]<br><br>The key prefix allowing to identify the keys belonging to the cache. If not set, the value "`cache:{cache-name`++}++" will be used. The variable "`{cache-name`++}++" is resolved from the value set in the cache annotations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__PREFIX+++[] | string |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-use-optimistic-locking"></a> [`quarkus.cache.redis."cache-name".use-optimistic-locking`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-use-optimistic-locking) config_property_copy_button:+++quarkus.cache.redis."cache-name".use-optimistic-locking+++[]<br><br>Whether the access to the cache should be using optimistic locking. See [Redis Optimistic Locking](https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set) for details. Default is `false`.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__USE_OPTIMISTIC_LOCKING+++[] | boolean |  |
| <a id="quarkus-redis-cache_quarkus-cache-redis-cache-name-invalidation-scan-size"></a> [`quarkus.cache.redis."cache-name".invalidation-scan-size`](#quarkus-redis-cache_quarkus-cache-redis-cache-name-invalidation-scan-size) config_property_copy_button:+++quarkus.cache.redis."cache-name".invalidation-scan-size+++[]<br><br>If set, the `SCAN` command (used to implement invalidation) will have the `COUNT` argument with given value. If not set (default), no `COUNT` argument is present.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_REDIS__CACHE_NAME__INVALIDATION_SCAN_SIZE+++[] | int |  |

<a id="duration-note-anchor-quarkus-redis-cache_quarkus-cache"></a>

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

## Configure the Redis key

By default, the Redis backend stores the entry using the following keys pattern: `cache:{cache-name}:{cache-key}`, where `cache-key` is the key the application uses and `cache:{cache-name}` the prefix. The variable `{cache-name}` is resolved from the value set in the cache annotations. So, you can find all the entries for a single cache using the Redis `KEYS` command: `KEYS cache:{cache-name}:*`

The prefix can be configured by using the `prefix` property:

```properties
# Default configuration
quarkus.cache.redis.prefix=my-cache

# Configuration for `expensiveResourceCache`
quarkus.cache.redis.expensiveResourceCache.prefix=my-expensive-cache
```

In these cases, you can find all the keys managed by the default cache using `KEYS my-cache:*`, and all the keys managed by the `expensiveResourceCache` cache using: `KEYS my-expensive-cache:*`.

```
# Default configuration
# The variable "{cache-name}" is resolved from the value set in the cache annotations.
quarkus.cache.redis.prefix=my-cache-{cache-name}
```

In this latest example, you can find all the keys managed by the default cache using `KEYS my-cache-{cache-name}:*`.

## Enable optimistic locking

The access to the cache can be *direct* or use [optimistic locking](https://redis.io/docs/manual/transactions/#optimistic-locking-using-check-and-set). By default, optimistic locking is disabled.

You can enable optimistic locking using:

```properties
# Default configuration
quarkus.cache.redis.use-optimistic-locking=true

# Configuration for `expensiveResourceCache`
quarkus.cache.redis.expensiveResourceCache.use-optimistic-locking=true
```

When used, the key is *watched* and the *SET* command is executed in a transaction (`MULTI/EXEC`).
