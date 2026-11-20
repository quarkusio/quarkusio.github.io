# Using transactions in Quarkus

The `quarkus-narayana-jta` extension provides a Transaction Manager that coordinates and exposes transactions to your applications as described in the [Jakarta Transactions](https://jakarta.ee/specifications/transactions/) specification, formerly known as Java Transaction API (JTA).

When discussing Quarkus transactions, this guide refers to Jakarta Transactions transaction style and uses only the term *transaction* to address them.

Also, Quarkus does not support distributed transactions. This means that models that propagate transaction context, such as [Java Transaction Service](https://download.oracle.com/otndocs/jcp/7309-jts-1.0-spec-oth-JSpec/) (JTS), REST-AT, WS-Atomic Transaction, and others, are not supported by the `narayana-jta` extension.

## Setting it up

You don't need to worry about setting it up most of the time as extensions needing it will simply add it as a dependency. Hibernate ORM for example will include the transaction manager and set it up properly.

You might need to add it as a dependency explicitly if you are using transactions directly without Hibernate ORM for example. Add the following to your build file:

**pom.xml**

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-narayana-jta</artifactId>
</dependency>
```

**build.gradle**

```gradle
implementation("io.quarkus:quarkus-narayana-jta")
```

## Starting and stopping transactions: defining your boundaries

You can define your transaction boundaries either declaratively with `@Transactional` or programmatically with `QuarkusTransaction`. You can also use the JTA `UserTransaction` API directly, however this is less user-friendly than `QuarkusTransaction`.

### Declarative approach

The easiest way to define your transaction boundaries is to use the `@Transactional` annotation on your entry method (`jakarta.transaction.Transactional`).

```java
@ApplicationScoped
public class SantaClausService {

    @Inject ChildDAO childDAO;
    @Inject SantaClausDAO santaDAO;

    @Transactional // <1>
    public void getAGiftFromSanta(Child child, String giftDescription) {
        // some transaction work
        Gift gift = childDAO.addToGiftList(child, giftDescription);
        if (gift == null) {
            throw new OMGGiftNotRecognizedException(); // <2>
        }
        else {
            santaDAO.addToSantaTodoList(gift);
        }
    }
}
```

1. This annotation defines your transaction boundaries and will wrap this call within a transaction.
2. A `RuntimeException` crossing the transaction boundaries will roll back the transaction.

`@Transactional` can be used to control transaction boundaries on any CDI bean at the method level or at the class level to ensure every method is transactional. That includes REST endpoints.

You can control whether and how the transaction is started with parameters on `@Transactional`:

- `@Transactional(REQUIRED)` (default): starts a transaction if none was started, stays with the existing one otherwise.
- `@Transactional(REQUIRES_NEW)`: starts a transaction if none was started ; if an existing one was started, suspends it and starts a new one for the boundary of that method.
- `@Transactional(MANDATORY)`: fails if no transaction was started ; works within the existing transaction otherwise.
- `@Transactional(SUPPORTS)`: if a transaction was started, joins it ; otherwise works with no transaction.
- `@Transactional(NOT_SUPPORTED)`: if a transaction was started, suspends it and works with no transaction for the boundary of the method ; otherwise works with no transaction.
- `@Transactional(NEVER)`: if a transaction was started, raises an exception ; otherwise works with no transaction.

`REQUIRED` or `NOT_SUPPORTED` are probably the most useful ones. This is how you decide whether a method is to be running within or outside a transaction. Make sure to check the JavaDoc for the precise semantic.

The transaction context is propagated to all calls nested in the `@Transactional` method as you would expect (in this example `childDAO.addToGiftList()` and `santaDAO.addToSantaTodoList()`). The transaction will commit unless a runtime exception crosses the method boundary. You can override whether an exception forces the rollback or not by using `@Transactional(dontRollbackOn=SomeException.class)` (or `rollbackOn`).

You can also programmatically ask for a transaction to be marked for rollback. Inject a `TransactionManager` for this.

```java
@ApplicationScoped
public class SantaClausService {

    @Inject TransactionManager tm; // <1>
    @Inject ChildDAO childDAO;
    @Inject SantaClausDAO santaDAO;

    @Transactional
    public void getAGiftFromSanta(Child child, String giftDescription) {
        // some transaction work
        Gift gift = childDAO.addToGiftList(child, giftDescription);
        if (gift == null) {
            tm.setRollbackOnly(); // <2>
        }
        else {
            santaDAO.addToSantaTodoList(gift);
        }
    }
}
```

1. Inject the `TransactionManager` to be able to activate `setRollbackOnly` semantic.
2. Programmatically decide to set the transaction for rollback.

### Transaction configuration

Advanced configuration of the transaction is possible with the use of the `@TransactionConfiguration` annotation that is set in addition to the standard `@Transactional` annotation on your entry method or at the class level.

The `@TransactionConfiguration` annotation allows to set a timeout property, in seconds, that applies to transactions created within the annotated method.

This annotation may only be placed on the top level method delineating the transaction. Annotated nested methods once a transaction has started will throw an exception.

If defined on a class, it is equivalent to defining it on all the methods of the class marked with `@Transactional`. The configuration defined on a method takes precedence over the configuration defined on a class.

### Reactive extensions

If your `@Transactional`-annotated method returns a reactive value, such as:

- `CompletionStage` (from the JDK)
- `Publisher` (from Reactive-Streams)
- Any type that can be converted to one of the two previous types using Reactive Type Converters

then the behaviour is a bit different, because the transaction will not be terminated until the returned reactive value is terminated. In effect, the returned reactive value will be listened to and if it terminates exceptionally the transaction will be marked for rollback, and will be committed or rolled-back only at termination of the reactive value.

This allows your reactive methods to keep on working on the transaction asynchronously until their work is really done, and not just until the reactive method returns.

If you need to propagate your transaction context across your reactive pipeline, please see the [Context Propagation guide](/guides/context-propagation/index.md).

### Programmatic approach

You can use static methods on `QuarkusTransaction` to define transaction boundaries. This provides two different options, a functional approach that allows you to run a lambda within the scope of a transaction, or by using explicit `begin`, `commit` and `rollback` methods.

```java
import io.quarkus.narayana.jta.QuarkusTransaction;

public class TransactionExample {

    public void beginExample() {
        QuarkusTransaction.begin();
        //do work
        QuarkusTransaction.commit();

        QuarkusTransaction.begin(QuarkusTransaction.beginOptions()
                .timeout(10));
        //do work
        QuarkusTransaction.rollback();
    }

    public void runnerExample() {
        QuarkusTransaction.requiringNew().run(() -> {
            //do work
        });
        QuarkusTransaction.joiningExisting().run(() -> {
            //do work
        });

        int result = QuarkusTransaction.requiringNew()
                .timeout(10)
                .exceptionHandler((throwable) -> {
                    if (throwable instanceof SomeException) {
                        return TransactionExceptionResult.COMMIT;
                    }
                    return TransactionExceptionResult.ROLLBACK;
                })
                .call(() -> {
                    //do work
                    return 0;
                });
    }
}
```

The above example shows a few different ways the API can be used.

The first method simply calls begin, does some work and commits it. This created transaction is tied to the CDI request scope, so if it is still active when the request scope is destroyed then it will be automatically rolled back. This removes the need to explicitly catch exceptions and call `rollback`, and acts as a safety net against inadvertent transaction leaks, however it does mean that this can only be used when the request scope is active. The second example in the method calls begin with a timeout option, and then rolls back the transaction.

The second method shows the use of lambda scoped transactions with `QuarkusTransaction.runner(...)`; the first example just runs a `Runnable` within a new transaction, the second does the same but joining the existing transaction (if any), and the third calls a `Callable` with some specific options. In particular the `exceptionHandler` method can be used to control if the transaction is rolled back or not on exception.

The following semantics are supported:

**`QuarkusTransaction.disallowingExisting()`/`DISALLOW_EXISTING`**



If a transaction is already associated with the current thread a `QuarkusTransactionException` will be thrown, otherwise a new transaction is started, and follows all the normal lifecycle rules.

**`QuarkusTransaction.joiningExisting()`/`JOIN_EXISTING`**



If no transaction is active then a new transaction will be started, and committed when the method ends. If an exception is thrown the exception handler registered by `#exceptionHandler(Function)` will be called to decide if the TX should be committed or rolled back. If an existing transaction is active then the method is run in the context of the existing transaction. If an exception is thrown the exception handler will be called, however a result of `ExceptionResult#ROLLBACK` will result in the TX marked as rollback only, while a result of `ExceptionResult#COMMIT` will result in no action being taken.

**`QuarkusTransaction.requiringNew()`/`REQUIRE_NEW`**



If an existing transaction is already associated with the current thread then the transaction is suspended, then a new transaction is started which follows all the normal lifecycle rules, and when it's complete the original transaction is resumed. Otherwise, a new transaction is started, and follows all the normal lifecycle rules.

**`QuarkusTransaction.suspendingExisting()`/`SUSPEND_EXISTING`**



If no transaction is active then these semantics are basically a no-op. If a transaction is active then it is suspended, and resumed after the task is run. The exception handler will never be consulted when these semantics are in use, specifying both an exception handler and these semantics are considered an error. These semantics allows for code to easily be run outside the scope of a transaction.

### Legacy API approach

The less easy way is to inject a `UserTransaction` and use the various transaction demarcation methods.

```java
@ApplicationScoped
public class SantaClausService {

    @Inject ChildDAO childDAO;
    @Inject SantaClausDAO santaDAO;
    @Inject UserTransaction transaction;

    public void getAGiftFromSanta(Child child, String giftDescription) {
        // some transaction work
        try {
            transaction.begin();
            Gift gift = childDAO.addToGiftList(child, giftDescription);
            santaDAO.addToSantaTodoList(gift);
            transaction.commit();
        }
        catch(SomeException e) {
            // do something on Tx failure
            transaction.rollback();
        }
    }
}
```

> [!NOTE]
> You cannot use `UserTransaction` in a method having a transaction started by a `@Transactional` call.

## Configuring the transaction timeout

You can configure the default transaction timeout, the timeout that applies to all transactions managed by the transaction manager, via the property [`quarkus.transaction-manager.default-transaction-timeout`](#quarkus-narayana-jta_quarkus-transaction-manager-default-transaction-timeout), specified as a duration.

> [!NOTE]
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
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

The default value is 60 seconds.

## Configuring the transaction reaper

The transaction reaper is a background thread that monitors running transactions and cancels those that exceed their timeout. It periodically scans for timed-out transactions (every `check-period`, default 120 seconds). When a transaction times out, the reaper follows this sequence:

1. Schedules the transaction for cancellation
2. A cancel worker attempts to roll back the transaction
3. After `cancel-wait-period` (default 500ms), the reaper interrupts the cancel worker if it has not finished
4. After `cancel-fail-wait-period` (default 500ms), the reaper declares the transaction a zombie if the worker still has not finished
5. Once the number of zombies exceeds `zombie-max` (default 8), the reaper escalates to ERROR-level logging

In CPU-constrained environments (e.g., containers with strict CPU limits), the default cancel wait periods may be too aggressive — the reaper can declare a zombie before the application thread gets a chance to respond to the cancellation. When this happens, you will see warnings like:

```
ARJUNA012117: TransactionReaper::check timeout for TX ... in state CANCEL
ARJUNA012120: cancel for ... returned, but worker is not done
ARJUNA012378: ReaperElement appears to be wedged
ARJUNA012108: CheckedAction::check - atomic action ... aborting with N threads active
```

If you see these messages, increase the cancel wait periods to give the reaper more time.

For CPU-constrained environments, increase the cancel wait periods:

```properties
# Time before interrupting a cancel worker (default: 500ms)
quarkus.transaction-manager.reaper.cancel-wait-period=30s
# Time after interrupting before declaring a zombie (default: 500ms)
quarkus.transaction-manager.reaper.cancel-fail-wait-period=30s
```

Other reaper settings can also be tuned if needed:

```properties
# How often the reaper scans for timed-out transactions (default: 120s)
quarkus.transaction-manager.reaper.check-period=120s
# Zombie count threshold before ERROR-level logging (default: 8)
quarkus.transaction-manager.reaper.zombie-max=8
```

> [!NOTE]
> To write duration values, use the standard `java.time.Duration` format. See the [Duration#parse() javadoc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html#parse(java.lang.CharSequence)) for more information.
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

## Configuring transaction node name identifier

Narayana, as the underlying transaction manager, has a concept of a unique node identifier. This is important if you consider using XA transactions that involve multiple resources.

The node name identifier plays a crucial part in the identification of a transaction. The node name identifier is forged into the transaction id when the transaction is created. Based on the node name identifier, the transaction manager is capable of recognizing the XA transaction counterparts created in database or JMS broker. The identifier makes possible for the transaction manager to roll back the transaction counterparts during recovery.

The node name identifier needs to be unique per transaction manager deployment. And the node identifier needs to be stable over the transaction manager restarts.

The node name identifier may be configured via the property [`quarkus.transaction-manager.node-name`](#quarkus-narayana-jta_quarkus-transaction-manager-node-name).

> [!NOTE]
> The node name cannot be longer than 28 bytes. To automatically shorten names longer than 28 bytes, set [`quarkus.transaction-manager.shorten-node-name-if-necessary`](#quarkus-narayana-jta_quarkus-transaction-manager-shorten-node-name-if-necessary) to `true`.
>
> Shortening is implemented by hashing the node name, encoding the hash to Base64 and then truncating the result. As with all hashes, the resulting shortened node name could potentially conflict with another shortened node name, but it is [very unlikely](https://github.com/quarkusio/quarkus/issues/30491#issuecomment-1537247764).

<a id="transaction-scope"></a>
## Using `@TransactionScoped` to bind CDI beans to the transaction lifecycle

You can define beans that live for as long as a transaction, and through CDI lifecycle events perform actions when a transaction starts and ends.

Just assign the transaction [scope](/guides/cdi/index.md#bean-scope) to such beans using the `@TransactionScoped` annotation:

```java
@TransactionScoped
public class MyTransactionScopedBean {

    // The bean's state is bound to a specific transaction,
    // and restored even after suspending then resuming the transaction.
    int myData;

    @PostConstruct
    void onBeginTransaction() {
        // This gets invoked after a transaction begins.
    }

    @PreDestroy
    void onBeforeEndTransaction() {
        // This gets invoked before a transaction ends (commit or rollback).
    }
}
```

Alternatively, if you don't necessarily need to hold state during the transaction, and just want to react to transaction start/end events, you can simply declare event listeners in a differently scoped bean:

```java
@ApplicationScoped
public class MyTransactionEventListeningBean {

    void onBeginTransaction(@Observes @Initialized(TransactionScoped.class) Object event) {
        // This gets invoked when a transaction begins.
    }

    void onBeforeEndTransaction(@Observes @BeforeDestroyed(TransactionScoped.class) Object event) {
        // This gets invoked before a transaction ends (commit or rollback).
    }

    void onAfterEndTransaction(@Observes @Destroyed(TransactionScoped.class) Object event) {
        // This gets invoked after a transaction ends (commit or rollback).
    }
}
```

> [!NOTE]
> The `event` object represents the transaction ID, and defines `toString()`/`equals()`/`hashCode()` accordingly.

> [!TIP]
> In listener methods, you can access more information about the transaction in progress by accessing the `TransactionManager`, which is a CDI bean and can be ``@Inject``ed.

<a id="jdbcstore"></a>
## Configure storing of Quarkus transaction logs in a database

In cloud environments where persistent storage is not available, such as when application containers are unable to use persistent volumes, you can configure the transaction management to store transaction logs in a database by using a Java Database Connectivity (JDBC) datasource.

However, in cloud-native apps, using a database to store transaction logs has additional requirements. The `narayana-jta` extension, which manages these transactions, requires stable storage, a unique reusable node identifier, and a steady IP address to work correctly. While the JDBC object store provides a stable storage, users must still plan how to meet the other two requirements.

Quarkus, after you evaluate whether using a database to store transaction logs is right for you, allows the following JDBC-specific configuration of the object store included in `quarkus.transaction-manager.object-store._<property>_` properties, where *\<property>* can be:

- [`type`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-type) (*string*): Configure this property to `jdbc` to enable usage of a Quarkus JDBC datasource for storing transaction logs. The default value is `file-system`.

- [`datasource`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-datasource) (*string*): Specify the name of the datasource for the transaction log storage. If no value is provided for the `datasource` property, Quarkus uses the [default datasource](/guides/datasource/index.md#configure-datasources).

- [`create-table`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-create-table) (*boolean*): When set to `true`, the transaction log table gets automatically created if it does not already exist. The default value is `false`.

- [`drop-table`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-drop-table) (*boolean*): When set to `true`, the tables are dropped on startup if they already exist. The default value is `false`.

- [`table-prefix`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-table-prefix) (string): Specify the prefix for a related table name. The default value is `quarkus_`.

For more configuration information, see the **Narayana JTA - Transaction manager** section of the Quarkus [All configuration options](/guides/all-config/index.md) reference.

**Additional information:**

- Create the transaction log table during the initial setup by setting the `create-table` property to `true`.

- JDBC datasources and ActiveMQ Artemis allow the enlistment and automatically register the `XAResourceRecovery`.

\*\* JDBC datasources is part of `quarkus-agroal`, and it needs to use `quarkus.datasource.jdbc.transactions=XA`. \*\* ActiveMQ Artemis is part of `quarkus-pooled-jms`, and it needs to use `quarkus.pooled-jms.transaction=XA`.

- The transaction recovery service is automatically enabled when XA JDBC datasources are detected (i.e. `quarkus.datasource.jdbc.transactions=XA`). For other XA resource providers such as `quarkus-pooled-jms`, set [`quarkus.transaction-manager.enable-recovery=true`](#quarkus-narayana-jta_quarkus-transaction-manager-enable-recovery) to enable recovery. You can also set it to `false` to explicitly disable recovery even when XA datasources are present.

> [!NOTE]
> To work around the current known issue of [Agroal having a different view on running transaction checks](https://issues.redhat.com/browse/AG-209), set the datasource transaction type for the datasource responsible for writing the transaction logs to `disabled`:
>
> ```
> quarkus.datasource.TX_LOG.jdbc.transactions=disabled
> ```
>
> This example uses TX_LOG as the datasource name.

## Why always having a transaction manager?

**Does it work everywhere I want to?**



Yep, it works in your Quarkus application, in your IDE, in your tests, because all of these are Quarkus applications. JTA has some bad press for some people. I don't know why. Let's just say that this is not your grandpa's JTA implementation. What we have is perfectly embeddable and lean.

**Does it do 2 Phase Commit and slow down my app?**



No, this is an old folk tale. Let's assume it essentially comes for free and let you scale to more complex cases involving several datasources as needed.

**I don't need transaction when I do read only operations, it's faster.**



Wrong.\
First off, just disable the transaction by marking your transaction boundary with `@Transactional(NOT_SUPPORTED)` (or `NEVER` or `SUPPORTS` depending on the semantic you want).\
Second, it's again fairy tale that not using transaction is faster. The answer is, it depends on your DB and how many SQL SELECTs you are making.\
No transaction means the DB does have a single operation transaction context anyway. + Third, when you do several SELECTs, it's better to wrap them in a single transaction because they will all be consistent with one another. Say your DB represents your car dashboard, you can see the number of kilometers remaining and the fuel gauge level. By reading it in one transaction, they will be consistent. If you read one and the other from two different transactions, then they can be inconsistent. It can be more dramatic if you read data related to rights and access management for example.

**Why do you prefer JTA vs Hibernate's transaction management API**



Managing the transactions manually via `entityManager.getTransaction().begin()` and friends lead to a butt ugly code with tons of try catch finally that people get wrong. Transactions are also about JMS and other database access, so one API makes more sense.

**It's a mess because I don't know if my Jakarta Persistence persistence unit is using `JTA` or `Resource-level` Transaction**



It's not a mess in Quarkus :) Resource-level was introduced to support Jakarta Persistence in a non-managed environment. But Quarkus is both lean and a managed environment, so we can safely always assume we are in JTA mode. The end result is that the difficulties of running Hibernate ORM + CDI + a transaction manager in Java SE mode are solved by Quarkus.

<a id="configuration-reference"></a>
## Configuration Reference for Transactions

[lock] Configuration property fixed at build time - All other configuration properties are overridable at runtime

|  |  |  |
| --- | --- | --- |
| <mark>Configuration property</mark> | Type | Default |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-node-name"></a> [`quarkus.transaction-manager.node-name`](#quarkus-narayana-jta_quarkus-transaction-manager-node-name) config_property_copy_button:+++quarkus.transaction-manager.node-name+++[]<br><br>The node name used by the transaction manager. Must not exceed a length of 28 bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_NODE_NAME+++[] | string | `quarkus` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-shorten-node-name-if-necessary"></a> [`quarkus.transaction-manager.shorten-node-name-if-necessary`](#quarkus-narayana-jta_quarkus-transaction-manager-shorten-node-name-if-necessary) config_property_copy_button:+++quarkus.transaction-manager.shorten-node-name-if-necessary+++[]<br><br>Whether the node name should be shortened if necessary. The node name must not exceed a length of 28 bytes. If this property is set to `true`, and the node name exceeds 28 bytes, the node name is shortened by calculating the [SHA-224](https://en.wikipedia.org/wiki/SHA-2) hash, which has a length of 28 bytes, encoded to Base64 format and then shortened to 28 bytes.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_SHORTEN_NODE_NAME_IF_NECESSARY+++[] | boolean | `false` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-default-transaction-timeout"></a> [`quarkus.transaction-manager.default-transaction-timeout`](#quarkus-narayana-jta_quarkus-transaction-manager-default-transaction-timeout) config_property_copy_button:+++quarkus.transaction-manager.default-transaction-timeout+++[]<br><br>The default transaction timeout.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_DEFAULT_TRANSACTION_TIMEOUT+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-narayana-jta_quarkus-transaction-manager) | `60S` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-enable-recovery"></a> [`quarkus.transaction-manager.enable-recovery`](#quarkus-narayana-jta_quarkus-transaction-manager-enable-recovery) config_property_copy_button:+++quarkus.transaction-manager.enable-recovery+++[]<br><br>Start the recovery service on startup.<br><br>If not set, the recovery service will be started automatically if XA datasources are configured. Set to `true` to always enable recovery, or `false` to explicitly disable it.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_ENABLE_RECOVERY+++[] | boolean | `` `true` if XA datasources are configured, `false` otherwise `` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-recovery-modules"></a> [`quarkus.transaction-manager.recovery-modules`](#quarkus-narayana-jta_quarkus-transaction-manager-recovery-modules) config_property_copy_button:+++quarkus.transaction-manager.recovery-modules+++[]<br><br>The list of recovery modules.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_RECOVERY_MODULES+++[] | list of string | `com.arjuna.ats.internal.arjuna.recovery.AtomicActionRecoveryModule`, `com.arjuna.ats.internal.jta.recovery.arjunacore.XARecoveryModule` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-expiry-scanners"></a> [`quarkus.transaction-manager.expiry-scanners`](#quarkus-narayana-jta_quarkus-transaction-manager-expiry-scanners) config_property_copy_button:+++quarkus.transaction-manager.expiry-scanners+++[]<br><br>The list of expiry scanners.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_EXPIRY_SCANNERS+++[] | list of string | `com.arjuna.ats.internal.arjuna.recovery.ExpiredTransactionStatusManagerScanner` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-xa-resource-orphan-filters"></a> [`quarkus.transaction-manager.xa-resource-orphan-filters`](#quarkus-narayana-jta_quarkus-transaction-manager-xa-resource-orphan-filters) config_property_copy_button:+++quarkus.transaction-manager.xa-resource-orphan-filters+++[]<br><br>The list of orphan filters.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_XA_RESOURCE_ORPHAN_FILTERS+++[] | list of string | `com.arjuna.ats.internal.jta.recovery.arjunacore.JTATransactionLogXAResourceOrphanFilter`, `com.arjuna.ats.internal.jta.recovery.arjunacore.JTANodeNameXAResourceOrphanFilter`, `com.arjuna.ats.internal.jta.recovery.arjunacore.JTAActionStatusServiceXAResourceOrphanFilter` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-reaper-check-period"></a> [`quarkus.transaction-manager.reaper.check-period`](#quarkus-narayana-jta_quarkus-transaction-manager-reaper-check-period) config_property_copy_button:+++quarkus.transaction-manager.reaper.check-period+++[]<br><br>The interval at which the reaper checks for timed-out transactions.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_REAPER_CHECK_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-narayana-jta_quarkus-transaction-manager) | `120S` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-reaper-cancel-wait-period"></a> [`quarkus.transaction-manager.reaper.cancel-wait-period`](#quarkus-narayana-jta_quarkus-transaction-manager-reaper-cancel-wait-period) config_property_copy_button:+++quarkus.transaction-manager.reaper.cancel-wait-period+++[]<br><br>The time the reaper waits before interrupting a cancel worker that is rolling back a timed-out transaction.<br><br>When a transaction exceeds its timeout, the reaper schedules a cancel and waits this period before interrupting the cancel worker. In CPU-constrained environments (e.g., containers with strict CPU limits), the default of 500ms may be too aggressive, causing premature zombie transaction declarations.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_REAPER_CANCEL_WAIT_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-narayana-jta_quarkus-transaction-manager) | `500MS` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-reaper-cancel-fail-wait-period"></a> [`quarkus.transaction-manager.reaper.cancel-fail-wait-period`](#quarkus-narayana-jta_quarkus-transaction-manager-reaper-cancel-fail-wait-period) config_property_copy_button:+++quarkus.transaction-manager.reaper.cancel-fail-wait-period+++[]<br><br>The time the reaper waits after interrupting a cancel worker before declaring the transaction a zombie.<br><br>After interrupting a cancel worker, the reaper waits this period. If the worker has not completed by then, the transaction is marked as a zombie. In CPU-constrained environments, increasing this value gives the application thread more time to respond to the cancellation.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_REAPER_CANCEL_FAIL_WAIT_PERIOD+++[] | [Duration](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/Duration.html) [[question circle]](#duration-note-anchor-quarkus-narayana-jta_quarkus-transaction-manager) | `500MS` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-reaper-zombie-max"></a> [`quarkus.transaction-manager.reaper.zombie-max`](#quarkus-narayana-jta_quarkus-transaction-manager-reaper-zombie-max) config_property_copy_button:+++quarkus.transaction-manager.reaper.zombie-max+++[]<br><br>The maximum number of zombie transactions before the reaper escalates to ERROR-level logging.<br><br>A zombie transaction is one where the cancel worker was unable to roll it back within the configured wait periods. Once this threshold is reached, subsequent zombies are logged at ERROR level instead of WARN.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_REAPER_ZOMBIE_MAX+++[] | int | `8` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-object-store-directory"></a> [`quarkus.transaction-manager.object-store.directory`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-directory) config_property_copy_button:+++quarkus.transaction-manager.object-store.directory+++[]<br><br>The name of the directory where the transaction logs will be stored when using the `file-system` object store. If the value is not absolute then the directory is relative to the *user.dir* system property.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_OBJECT_STORE_DIRECTORY+++[] | string | `ObjectStore` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-object-store-type"></a> [`quarkus.transaction-manager.object-store.type`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-type) config_property_copy_button:+++quarkus.transaction-manager.object-store.type+++[]<br><br>The type of object store.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_OBJECT_STORE_TYPE+++[] | `file-system`, `jdbc` | `file-system` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-object-store-datasource"></a> [`quarkus.transaction-manager.object-store.datasource`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-datasource) config_property_copy_button:+++quarkus.transaction-manager.object-store.datasource+++[]<br><br>The name of the datasource where the transaction logs will be stored when using the `jdbc` object store.<br><br>If undefined, it will use the default datasource.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_OBJECT_STORE_DATASOURCE+++[] | string |  |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-object-store-create-table"></a> [`quarkus.transaction-manager.object-store.create-table`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-create-table) config_property_copy_button:+++quarkus.transaction-manager.object-store.create-table+++[]<br><br>Whether to create the table if it does not exist.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_OBJECT_STORE_CREATE_TABLE+++[] | boolean | `false` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-object-store-drop-table"></a> [`quarkus.transaction-manager.object-store.drop-table`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-drop-table) config_property_copy_button:+++quarkus.transaction-manager.object-store.drop-table+++[]<br><br>Whether to drop the table on startup.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_OBJECT_STORE_DROP_TABLE+++[] | boolean | `false` |
| <a id="quarkus-narayana-jta_quarkus-transaction-manager-object-store-table-prefix"></a> [`quarkus.transaction-manager.object-store.table-prefix`](#quarkus-narayana-jta_quarkus-transaction-manager-object-store-table-prefix) config_property_copy_button:+++quarkus.transaction-manager.object-store.table-prefix+++[]<br><br>The prefix to apply to the table.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_OBJECT_STORE_TABLE_PREFIX+++[] | string | `quarkus_` |
| [lock] <a id="quarkus-narayana-jta_quarkus-transaction-manager-unsafe-multiple-last-resources"></a> <mark>link:#quarkus-narayana-jta_quarkus-transaction-manager-unsafe-multiple-last-resources\[`quarkus.transaction-manager.unsafe-multiple-last-resources`\]</mark> config_property_copy_button:+++quarkus.transaction-manager.unsafe-multiple-last-resources+++[]<br><br>*This property is deprecated: This property is planned for removal in a future version.*<br><br>Define the behavior when using multiple XA unaware resources in the same transactional demarcation.<br><br>Defaults to `fail`. `warn-each`, `warn-first`, and `allow` are UNSAFE and should only be used for compatibility. Either use XA for all resources if you want consistency, or split the code into separate methods with separate transactions.<br><br>Note that using a single XA unaware resource together with XA aware resources, known as the Last Resource Commit Optimization (LRCO), is different from using multiple XA unaware resources. Although LRCO allows most transactions to complete normally, some errors can cause an inconsistent transaction outcome. Using multiple XA unaware resources is not recommended since the probability of inconsistent outcomes is significantly higher and much harder to recover from than LRCO. For this reason, use LRCO as a last resort.<br><br>We do not recommend using this configuration property, and we plan to remove it in the future, so you should plan fixing your application accordingly. If you think your use case of this feature is valid and this option should be kept around, open an issue in our tracker explaining why.<br><br>Environment variable: env_var_with_copy_button:+++QUARKUS_TRANSACTION_MANAGER_UNSAFE_MULTIPLE_LAST_RESOURCES+++[] | tooltip:allow[Allow using multiple XA unaware resources in the same transactional demarcation.  This will log a warning once on application startup,opts=but not on each use of multiple XA unaware resources in the same transactional demarcation.], tooltip:warn-first[Allow using multiple XA unaware resources in the same transactional demarcation,opts=but log a warning on the first occurrence.], tooltip:warn-each[Allow using multiple XA unaware resources in the same transactional demarcation,opts=but log a warning on each occurrence.], tooltip:fail[Allow using multiple XA unaware resources in the same transactional demarcation,opts=but log a warning on each occurrence.] | tooltip:fail[Allow using multiple XA unaware resources in the same transactional demarcation,opts=but log a warning on each occurrence.] |

<a id="duration-note-anchor-quarkus-narayana-jta_quarkus-transaction-manager"></a>

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
