# Infinispan Cache

By default, Quarkus Cache uses Caffeine as backend. It's possible to use Infinispan instead.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered preview.
>
> In *preview*, backward compatibility and presence in the ecosystem is not guaranteed. Specific improvements might require changing configuration or APIs, and plans to become *stable* are under way. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

## Infinispan as cache backend

When using Infinispan as the backend for Quarkus cache, each cached item will be stored in Infinispan:

- The backend uses the *\<default>* Infinispan client (unless configured differently), so ensure its configuration is set up accordingly (or use the [Infinispan Dev Service](/guides/infinispan-dev-services/index.md))
- Both the key and the value are marshalled using Protobuf with Protostream.

## Use the Infinispan backend

First, add the `quarkus-infinispan-cache` extension to your project:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-infinispan-cache</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-infinispan-cache")
```

Then, use the `@CacheResult` and other cache annotations as detailed in the [Quarkus Cache guide](/guides/cache/index.md):

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

<a id="infinispan-cache-configuration-reference"></a>
## Configure the Infinispan backend

The Infinispan backend uses the `<default>` Infinispan client. Refer to the [Infinispan reference](/guides/infinispan-client-reference/index.md) for configuring the access to Infinispan.

> [!TIP]
> In dev mode, you can use the [Infinispan Dev Service](/guides/infinispan-dev-services/index.md).

If you want to use another Infinispan for your cache, configure the `client-name` as follows:

```properties
quarkus.cache.infinispan.client-name=another
```

## Marshalling

When interacting with Infinispan in Quarkus, you can easily serialize and deserialize Java primitive types when storing or retrieving data from the cache. No additional marshalling configuration is required for Infinispan.

```java
@CacheResult(cacheName = "weather-cache") // <1>
public String getDailyForecast(String dayOfWeek, int dayOfMonth, String city) { // <2>
    return dayOfWeek + " will be " + getDailyResult(dayOfMonth % 4) + " in " + city;
}
```

1. Ask this method execution to be cached in the 'weather-cache'
2. The key combines `String` dayOfWeek, `int` dayOfMonth and `String` city. The associated value is of type `String`.

Quarkus uses Protobuf for data serialization in Infinispan by default. Infinispan recommends using Protobuf as the preferred way to structure data. Therefore, when working with Plain Old Java Objects (POJOs), users need to supply the schema for marshalling in Infinispan.

### Marshalling Java types

Let's say we want a composite Key using `java.time.LocalDate`.

```java
@CacheResult(cacheName = "weather-cache") // <1>
public String getDailyForecast(LocalDate date, String city) { // <2>
    return date.getDayOfWeek() + " will be " + getDailyResult(date.getDayOfMonth() % 4) + " in " + city;
}
```

1. Request that the response of this method execution be cached in 'weather-cache'
2. The key combines a `java.util.LocalDate` date and a `String` city. The associated value is of type 'String'.

Since Infinispan serializes data by default using Protobuf in Quarkus, executing the code will result in the following error:

```bash
java.lang.IllegalArgumentException:
No marshaller registered for object of Java type java.time.LocalDate
```

Protobuf does not inherently know how to marshal `java.time.LocalDate`. Fortunately, Protostream provides a straightforward solution to this problem using `@ProtoAdapter` and `@ProtoSchema`.

```java
@ProtoAdapter(LocalDate.class)
public class LocalDateAdapter {
    @ProtoFactory
    LocalDate create(String localDate) {
        return LocalDate.parse(localDate);
    }

    @ProtoField(1)
    String getLocalDate(LocalDate localDate) {
        return localDate.toString();
    }
}

@ProtoSchema(includeClasses = LocalDateAdapter.class, schemaPackageName = "quarkus")
public interface Schema extends GeneratedSchema {
}
```

### Marshalling your POJOs

Just like with Java types, when using your POJOs as keys or values, you can follow this approach:

```java
@CacheResult(cacheName = "my-cache") // <1>
public ExpensiveResponse requestApi(String id) { // <2>
    // very expensive call

    return new ExpensiveResponse(...);
}
```

1. Request that the response of this method execution be cached in 'my-cache'
2. The key is a `String`. The associated value is of type `org.acme.ExpensiveResponse`.

In this case, you'll need to define the schema for your Java type using `@Proto` and `@ProtoSchema`. This schema can encompass all types and adapters used in your Quarkus application.

```java
@Proto
public record ExpensiveResponse(String result) {
}

@ProtoSchema(includeClasses = { ExpensiveResponse.class })
interface Schema extends GeneratedSchema {
}
```

Read more about it in the [Infinispan reference](/guides/infinispan-client-reference/index.md) in the Annotation based serialization section.

## Expiration

You have the option to configure two properties for data expiration: **lifespan** and **max-idle**.

### Lifespan

In Infinispan, **lifespan** refers to a configuration parameter that determines the maximum time an entry (or an object) can remain in the cache since it was created or last accessed before it is considered expired and removed from the cache.

When you configure the **lifespan** parameter for entries in an Infinispan cache, you specify a time duration. After an entry has been added to the cache or accessed (read or written), it starts its lifespan countdown. If the time since the entry was created or last accessed exceeds the specified "lifespan" duration, the entry is considered expired and becomes eligible for eviction from the cache.

```properties
quarkus.cache.infinispan.my-cache.lifespan=10s
```

### Max Idle

When you configure the **max-idle** parameter for entries in an Infinispan cache, you specify a time duration. After an entry has been accessed (read or written) in the cache, if there are no subsequent accesses to that entry within the specified duration, it is considered idle. Once the idle time exceeds the **max-idle** duration, the entry is considered expired and eligible for eviction from the cache.

```properties
quarkus.cache.infinispan.my-cache.max-idle=100s
```

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| [lock] <a id="quarkus-infinispan-cache_quarkus-cache-infinispan-client-name"></a> [`quarkus.cache.infinispan.client-name`](#quarkus-infinispan-cache_quarkus-cache-infinispan-client-name) config_property_copy_button:+++quarkus.cache.infinispan.client-name+++[]<br><br>The name of the named Infinispan client to be used for communicating with Infinispan. If not set, use the default Infinispan client.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_INFINISPAN_CLIENT_NAME+++[] | string |  |
| <a id="quarkus-infinispan-cache_quarkus-cache-infinispan-lifespan"></a> [`quarkus.cache.infinispan.lifespan`](#quarkus-infinispan-cache_quarkus-cache-infinispan-lifespan) config_property_copy_button:+++quarkus.cache.infinispan.lifespan+++[]<br><br>The default lifespan of the item stored in the cache<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_INFINISPAN_LIFESPAN+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-infinispan-cache_quarkus-cache) |  |
| <a id="quarkus-infinispan-cache_quarkus-cache-infinispan-max-idle"></a> [`quarkus.cache.infinispan.max-idle`](#quarkus-infinispan-cache_quarkus-cache-infinispan-max-idle) config_property_copy_button:+++quarkus.cache.infinispan.max-idle+++[]<br><br>The default max-idle of the item stored in the cache<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_INFINISPAN_MAX_IDLE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-infinispan-cache_quarkus-cache) |  |
| <a id="quarkus-infinispan-cache_section_quarkus-cache-infinispan"></a> [Additional configuration applied to a specific Infinispan cache (highest precedence)](#quarkus-infinispan-cache_section_quarkus-cache-infinispan) | Type | Default |
| <a id="quarkus-infinispan-cache_quarkus-cache-infinispan-cache-name-lifespan"></a> [`quarkus.cache.infinispan."cache-name".lifespan`](#quarkus-infinispan-cache_quarkus-cache-infinispan-cache-name-lifespan) config_property_copy_button:+++quarkus.cache.infinispan."cache-name".lifespan+++[]<br><br>The default lifespan of the item stored in the cache<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_INFINISPAN__CACHE_NAME__LIFESPAN+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-infinispan-cache_quarkus-cache) |  |
| <a id="quarkus-infinispan-cache_quarkus-cache-infinispan-cache-name-max-idle"></a> [`quarkus.cache.infinispan."cache-name".max-idle`](#quarkus-infinispan-cache_quarkus-cache-infinispan-cache-name-max-idle) config_property_copy_button:+++quarkus.cache.infinispan."cache-name".max-idle+++[]<br><br>The default max-idle of the item stored in the cache<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_CACHE_INFINISPAN__CACHE_NAME__MAX_IDLE+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-infinispan-cache_quarkus-cache) |  |

<a id="duration-note-anchor-quarkus-infinispan-cache_quarkus-cache"></a>

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
