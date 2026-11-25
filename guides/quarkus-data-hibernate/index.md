# Quarkus Data Hibernate reference guide

Quarkus Data Hibernate is the perfect solution if you want to get started using Hibernate in Quarkus.

If you are new to Quarkus Data Hibernate, start with the [Quarkus Data Hibernate getting started tutorial](/guides/quarkus-data-hibernate-getting-started/index.md).

To work with native SQL only, without entities, see the [Quarkus Data native SQL tutorial](/guides/quarkus-data-hibernate-sql/index.md).

> [!NOTE]
> This guide does not go into the specifics of using either [Hibernate ORM](/guides/hibernate-orm/index.md), or its underlying [Jakarta Persistence specification](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-M4.html). Refer to their documentation for more in-depth knowledge.

<a id="extension-status-note"></a>

> [!NOTE]
> This technology is considered experimental.
>
> In *experimental* mode, early feedback is requested to mature the idea. There is no guarantee of stability nor long term presence in the platform until the solution matures. Feedback is welcome on our [mailing list](https://groups.google.com/d/forum/quarkus-dev) or as issues in our [GitHub issue tracker](https://github.com/quarkusio/quarkus/issues).
>
> For a full list of possible statuses, check our [FAQ entry](https://quarkus.io/faq/#what-are-the-extension-statuses).

> [!WARNING]
> This extension is currently experimental, so its API may change, in particular names of classes or packages may change, even the module name. We are in the process of making it a public preview in order to obtain public feedback, so please report feedback either on [Zulip](https://quarkusio.zulipchat.com/#narrow/channel/187038-dev/topic/WG.20-.20Panache.2ENext/with/562916276) or via [GitHub issues](https://github.com/orgs/quarkusio/projects/50/views/1).

## Installation

Quarkus Data Hibernate uses an annotation processor to inspect your entities and repository interfaces at build time. It validates repository methods and queries, then generates the repository implementations that Quarkus makes available as CDI beans. This catches many mapping and query errors during the build instead of at runtime.

Enable the annotation processor in your build configuration:

**pom.xml**

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPathsUseDepMgmt>true</annotationProcessorPathsUseDepMgmt>
        <annotationProcessorPaths>
            <path>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-data-processor</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**build.gradle**

```gradle
annotationProcessor enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")
annotationProcessor 'io.quarkus:quarkus-data-processor'
```

For step-by-step instructions, including the required extensions, database driver, and datasource configuration, see the [Quarkus Data Hibernate getting started tutorial](/guides/quarkus-data-hibernate-getting-started/index.md).

## Entities

An entity is a Java class that represents data stored in a database. Typically, the class maps to a table, each entity instance maps to a row, and its fields map to columns. Quarkus Data Hibernate works with entities defined using Jakarta Persistence annotations.

### Defining an entity

To map a Java class to a database table:

- Write a class.
- Annotate it with [`@Entity`](https://jakarta.ee/specifications/persistence/4.0/apidocs/jakarta.persistence/jakarta/persistence/Entity.html).
- Optionally, make it extend `ManagedEntity` or `RecordEntity` (see [Session and access models](#session-and-access-models)).
- Make its fields `public`. [Getters and setters are not necessary thanks to bytecode enhancement](#entity-field-access).

```java
import io.quarkus.data.hibernate.ManagedEntity;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
public class Cat extends ManagedEntity {
    public String name;
    public LocalDate birth;
    public Breed breed;

    public enum Breed {
        CUTE, HAIRLESS;
    }
}
```

Instances of `ManagedEntity` are managed by Hibernate ORM. After an instance is persisted, changes to its fields are automatically sent to the database without an explicit `update` instruction.

<a id="entity-field-access"></a>
### Entity field access

Quarkus enhances entity bytecode at build time, so public fields can be accessed directly without writing getters and setters. Missing getters and setters are generated, and public-field access is rewritten to invoke the corresponding accessor methods. For example, reading `cat.name` invokes `getName()`, and assigning `cat.name` invokes `setName()`. You can write these accessors yourself to add custom logic; callers can continue using the public field without changes. Enhancement also tracks changes to managed entities. See [Automatic build time enhancement](/guides/hibernate-orm/index.md#automatic-build-time-enhancement). Public fields are a convenience, not a requirement: private fields with accessors and Jakarta Persistence property access are also supported.

<a id="about-entity-identifiers"></a>
### Entity identifiers

`ManagedEntity` and `RecordEntity` come with a default generated database identifier because they extend `WithId.AutoLong`, which provides a generated database identifier of type `Long`.

You can choose to extend `WithId.AutoString` for a `String` identifier, `WithId.AutoUUID` for a `UUID` identifier, or `WithId.AutoUUIDv7` for a time-sortable `UUID` identifier. The following table lists the identifier supertypes:

| ID type | Supertype | Generation |
| --- | --- | --- |
| `Long` | `WithId.AutoLong` | `@GeneratedValue` |
| `String` | `WithId.AutoString` | `@UuidGenerator` |
| `UUID` | `WithId.AutoUUID` | `@UuidGenerator` |
| `UUID` | `WithId.AutoUUIDv7` | `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` |
| `Id` | `WithId<Id>` | Assigned by the application |

> [!NOTE]
> `WithId<Id>` provides an inherited `@Id` field without an identifier generator. You must assign its value before persisting the entity.

For example, extend `WithId.AutoUUIDv7` to use generated UUIDv7 identifiers:

```java
import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.WithId;
import jakarta.persistence.Entity;

@Entity
public class CatWithUUID extends WithId.AutoUUIDv7 implements ManagedEntity.CustomId {
    public String name;
}
```

The entity inherits an `id` field of type `java.util.UUID`. When the entity is persisted, Hibernate automatically generates a time-sortable version 7 UUID for this field. You do not need to declare or assign the identifier yourself. Implementing `ManagedEntity.CustomId` also provides managed entity operations such as `persist()` and `delete()`.

You can also provide your own database identifier explicitly and implement the `ManagedEntity.CustomId` interface in your entity, as well as use the `ManagedRepository.CustomId` interface for your repository, to specify the database identifier type:

```java
import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import jakarta.data.repository.Find;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CatWithId implements ManagedEntity.CustomId {
    @Id
    public String id;
    public String name;

    public interface Repo extends ManagedRepository.CustomId<CatWithId, String> {
        @Find
        CatWithId findByName(String name);
    }
}
```

> [!WARNING]
> The custom identifier in this example has no generator, so you must set its value before persisting the entity.

You do not have to extend an identifier supertype if you define your own identifier. To combine a generated identifier superclass with managed or record operations, implement the corresponding `CustomId` entity interface and extend the corresponding `CustomId<Entity, Id>` repository interface; see [the type matrix](#entity-and-repository-type-matrix).

## Repositories

A Quarkus Data Hibernate repository is an interface containing annotated finder, query, or lifecycle methods. The annotation processor generates its implementation. A repository may also declare unannotated `default` methods for custom operations.

A repository does not need to extend another interface, but Quarkus Data provides a few repository supertypes worth considering for convenience. The usual conventions are to nest the repository in its entity and extend `ManagedRepository<Entity>` for managed entities, or `RecordRepository<Entity>` for record entities. These supertypes provide ready-made persistence and query operations in addition to your declared methods.

**Managed entity and repository**

```java
import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.persistence.Entity;

import java.time.LocalDate;
import java.util.List;

@Entity
public class Cat extends ManagedEntity {
    public String name;
    public LocalDate birth;
    public Breed breed;

    public enum Breed {
        CUTE, HAIRLESS;
    }

    public interface Repo extends ManagedRepository<Cat> {
        @Find
        Cat findByName(String name);

        @Query("where breed = CUTE")
        List<Cat> findCute();

        @Query("delete from Cat where name = :name")
        long deleteByName(String name);

        @Query("delete from Cat where breed = HAIRLESS")
        long deleteHairless();
    }
}
```

**Record entity and repository**

```java
import io.quarkus.data.hibernate.RecordEntity;
import io.quarkus.data.hibernate.RecordRepository;
import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

@Entity
public class CatRecord extends RecordEntity {
    public String name;

    public interface Repo extends RecordRepository<CatRecord> {
        @Find
        CatRecord findByName(String name);
    }
}
```

The examples use `jakarta.data.repository.Find` and `jakarta.data.repository.Query`. For the behavioral differences between managed and record entities, see [Session and access models](#session-and-access-models). For standalone repositories and entities without Quarkus Data supertypes, see [Advanced usage](#alternative-repository-definitions).

> [!TIP]
> For queries without mapped entities, see the [native SQL guide](/guides/quarkus-data-hibernate-sql/index.md).

### Generated finder and query methods

You can use `@Find` methods to find single instances or collections of entities by using the parameters of the method to build a query. The Hibernate documentation contains the complete [generated finder method reference](https://docs.hibernate.org/orm/8.0/introduction/html_single/#generated-finder-methods).

**A few common examples**

```java
import jakarta.data.repository.Find;
import org.acme.Cat.Breed;

import java.util.List;

@Find
List<Cat> findAllCats(); <1>

@Find
long countAllCats(); <2>

@Find
Cat findCatByNameAndBreed(String name, Breed breed); <3>
```

1. Finder methods may return collections of entities
2. Or generate `COUNT` queries
3. Or single entities, and any number of parameters which must all match the queried entities.

Use `@Delete` instead of `@Find` to delete entities matching the method parameters. The method can return the number of deleted entities:

```java
import jakarta.data.repository.Delete;

@Delete
long deleteByName(String name);
```

Alternatively, use Jakarta Data's `@Query` for entity queries or Jakarta Persistence's `@jakarta.persistence.query.NativeQuery` for native SQL. Jakarta Data's `@Query` supports JPQL and its portable Jakarta Data Query Language subset (JDQL). Hibernate also accepts HQL extensions in `@Query`. The Hibernate documentation contains the complete [generated query method reference](https://docs.hibernate.org/orm/8.0/introduction/html_single/#generated-query-methods).

**Examples of `@Query` and `@NativeQuery` usage**

```java
import jakarta.data.repository.Query;
import jakarta.persistence.query.NativeQuery;
import org.acme.Cat.Breed;

import java.time.LocalDate;
import java.util.List;

@Query("from Cat")
List<Cat> findAllCats(); <1>

@Query("select min(birth) from Cat")
LocalDate oldestCat(); <2>

@Query("where name = :name and breed = :breed")
Cat findCatByNameAndBreed(String name, Breed breed); <3>

@NativeQuery("select name from Cat where name = :name")
List<String> findNames(String name); <4>
```

1. Query methods may return collections of entities
2. Or projections of a single column, even multiple columns with `Object[]` and `List<Object[]>` types
3. Queries can of course refer to method parameters.
4. Native queries refer to database tables and columns instead of entity names and fields.

`@Query` also supports bulk `UPDATE` and `DELETE` queries. These methods can return the number of affected entities:

```java
import jakarta.data.repository.Query;

@Query("update Cat set name = :newName where name = :name")
long renameCats(String name, String newName);

@Query("delete from Cat where name = :name")
long deleteByName(String name);
```

Generated finder and HQL query methods are type-checked at build time. They are validated for entity names, fields, HQL syntax, and parameter names. Native SQL is validated by the database when the query is executed; see the [native SQL tutorial](/guides/quarkus-data-hibernate-sql/index.md).

### Obtaining and using a repository

Repositories are CDI beans and can be injected where you intend to use them:

```java
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Inject
Cat.Repo repo;

@Transactional
public Cat findCat(String name) {
    return repo.findByName(name);
}
```

You can also access a nested repository by using a generated static method on the [generated metamodel](https://docs.hibernate.org/orm/8.0/introduction/html_single/#static-metamodel) of your entity:

```java
Cat cat = Cat_.repo().findByName("Lucky");
```

All repositories defined as nested interfaces of an entity are available under their generated metamodel class through accessor methods with the same name as the repository. This is equivalent to injecting the repository type and uses CDI under the hood to look up the repository.

### The `ManagedRepository` supertype

The `ManagedRepository` type provides most of the operations needed to work with an entity, such as `persist()`, `delete()`, `isPersistent()`, and `listAll()`.

Here is an example of how it can be used:

```java
import io.quarkus.data.hibernate.blocking.BlockingDataQuery;
import jakarta.data.Sort;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.hibernate.Session;

import java.util.List;
import java.util.Optional;

@Inject
Cat.Repo repo;

@Transactional
public void repositoryOperations(Cat cat) {
    // entity operations
    repo.persist(cat);
    repo.delete(cat);
    boolean isPersistent = repo.isPersistent(cat);
    // operations on all entities
    long count = repo.count();
    long deleted = repo.deleteAll();
    List<Cat> allCats = repo.listAll();
    repo.streamAll().forEach(kitty -> kitty.name = kitty.name.toUpperCase());
    BlockingDataQuery<Cat> catQuery = repo.findAll();
    List<Cat> sortedCats = repo.findAll().sort(Sort.asc("name")).list();
    // operations on the Hibernate session
    repo.flush();
    Session session = repo.getSession();
    // ID-related operations
    boolean wasDeleted = repo.deleteById(cat.id);
    Cat foundCat = repo.findById(cat.id);
    Optional<Cat> optionalCat = repo.findByIdOptional(cat.id);
}
```

<a id="generated-metamodels"></a>
### Generated metamodels

The annotation processor generates two different metamodels for `Cat`:

- `_Cat`, with a leading underscore, is the Jakarta Data metamodel. Its typed attributes build restrictions and sort criteria.
- `Cat_`, with a trailing underscore, is the Jakarta Persistence metamodel. Its string attribute constants can be used in repository queries.

Both are generated in the same package as `Cat`; the examples below assume `org.acme`.

### Dynamically built queries

When defining finder and query operations on a repository, queries are validated and generated at build time. For dynamic filtering and sorting, prefer type-safe restrictions and ordering before using query strings.

#### Type-safe restrictions and ordering

Jakarta Data 1.1 provides `Restriction` parameters for finder and query methods. For example, add this method to `Cat.Repo`:

```java
import jakarta.data.Order;
import jakarta.data.restrict.Restriction;
import jakarta.data.repository.Find;

import java.util.List;

@Find
List<Cat> search(Restriction<Cat> restriction, Order<Cat> order);
```

Use the [generated Jakarta Data metamodel](#generated-metamodels) `_Cat` for type-safe restrictions and ordering:

```java
import jakarta.data.Order;
import jakarta.data.restrict.Restrict;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@Inject
Cat.Repo repo;

@Transactional
public List<Cat> typeSafeQueries() {
    return repo.search(
            Restrict.all(_Cat.name.startsWith("Luck"), _Cat.breed.equalTo(Cat.Breed.CUTE)),
            Order.by(_Cat.name.asc(), _Cat.birth.desc()));
}
```

The attribute types determine which comparisons are available and which values they accept. Restrictions can be combined conditionally to build a query from user-supplied search criteria. See the Hibernate documentation on [restrictions and ordering](https://docs.hibernate.org/orm/8.0/introduction/html_single/#restrictions-and-ordering) and the [Jakarta Data specification](https://jakarta.ee/specifications/data/1.1/jakarta-data-1.1.html) for the full APIs.

#### Queries using metamodel attribute constants

The query methods provided by `ManagedRepository` and `RecordRepository` can use attribute constants from the generated Jakarta Persistence metamodel `Cat_`. These constants contain attribute names as strings; unlike the typed attributes of `_Cat`, they do not make query parameters or expressions type-safe.

```java
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@Inject
Cat.Repo repo;

@Transactional
public void metamodelAttributeQueries() {
    // Find a cat by name
    Cat cat = repo.find(Cat_.NAME, "Lucky").singleResult();
    // All cute cats
    List<Cat> cuteCats = repo.list(Cat_.BREED, Cat.Breed.CUTE);
    // Cats with no known birth date
    repo.stream(Cat_.BIRTH + " is null").forEach(kitty -> System.err.println(kitty));
    // Get rid of non-cute cats
    long deleted = repo.delete(Cat_.BREED, Cat.Breed.HAIRLESS);
}
```

#### String-based queries

For queries that cannot be expressed with restrictions, use the query methods provided by `ManagedRepository` or `RecordRepository`. These methods accept query strings, so they are not type-safe.

```java
import jakarta.data.Sort;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@Inject
Cat.Repo repo;

@Transactional
public void nonTypeSafeQueries() {
    // Count ugly cats with no name
    repo.count("breed = ?1 and name is null", Cat.Breed.HAIRLESS);
    // Make every cat cute
    repo.update("breed = CUTE");
    // Sort results using BlockingDataQuery
    List<Cat> sortedByName = repo.findAll().sort(Sort.asc("name")).list();
    List<Cat> sortedMatches = repo.find("breed = ?1", Cat.Breed.CUTE)
            .sort(Sort.desc("name"))
            .list();
}
```

Sorting is applied on the `BlockingDataQuery` returned by `find()` and `findAll()`, not on the shortcut `list()`, `listAll()`, `stream()`, or `streamAll()` methods. For a single criterion, pass a `Sort` directly. Use `Order` only when you need multiple criteria, or when receiving an `Order` from a REST endpoint:

```java
import jakarta.data.Order;
import jakarta.data.Sort;

import java.util.List;

List<Cat> byName = repo.findAll().sort(Sort.asc("name")).list();
List<Cat> byNameThenBirth = repo.findAll()
        .sort(Order.by(Sort.asc("name"), Sort.desc("birth")))
        .list();
```

<a id="session-and-access-models"></a>
## Session and access models

Quarkus Data Hibernate supports managed and stateless sessions with blocking and reactive APIs.

<a id="use-stateful-sessions"></a>
### Managed entities and stateful sessions

Use `ManagedEntity` and `ManagedRepository` for Hibernate ORM *stateful sessions*. Entities persisted or obtained through a stateful session are managed by Hibernate ORM, and changes to their fields are automatically sent to the database without requiring an explicit `update` operation. The session type is `Session`.

<a id="use-stateless-sessions"></a>
### Record entities and stateless sessions

To make every update operation explicit, use a Hibernate ORM *stateless session*. Extend `RecordEntity` for the entity and `RecordRepository` for its repository. Compared with a managed entity:

- Changes to entity fields must be explicitly pushed to the database by calling `update()` on the entity or repository.
- Call `insert()` instead of `persist()` to insert the entity.
- The session type is `StatelessSession` instead of `Session`.
- Accessing an unfetched lazy association does not trigger automatic loading. Fetch the associations you need as part of a repository query, using a `join fetch` clause in `@Query`. Alternatively, fetch an association explicitly with `repo.getSession().fetch(association)`. Managed blocking sessions can load lazy associations automatically while the session is open.

Queries and the way you obtain entities remain the same. Stateless sessions can also use [Jakarta Data repositories](#jakarta-data).

> [!NOTE]
> Whether an entity is managed depends on the session used to persist or obtain it, not on its Java superclass. For example, a `ManagedEntity` obtained through a `RecordRepository` is not managed, because that repository uses a stateless session. See [Switching between managed and record models](#switching-between-managed-and-record-models) for alternative entity and repository APIs.

<a id="lets-get-reactive"></a>
### Reactive access

#### Reactive managed entities

For reactive access:

- The entity extends `ManagedEntity.Reactive`.
- The repository extends `ManagedRepository.Reactive`.
- Operations return `Uni<T>` instead of `T`, which is the standard Mutiny reactive type.
- Use `jakarta.transaction.Transactional` for transaction boundaries and compose operations as usual with Mutiny.

```java
import io.quarkus.data.hibernate.ManagedEntity;
import io.quarkus.data.hibernate.ManagedRepository;
import io.smallrye.mutiny.Uni;
import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

@Entity
public class ReactiveCat extends ManagedEntity.Reactive {
    public String name;

    public interface Repo extends ManagedRepository.Reactive<ReactiveCat> {
        @Find
        Uni<ReactiveCat> findByName(String name);
    }
}
```

With an injected `ReactiveCat.Repo repo`, operations are composed in a transaction:

```java
import io.smallrye.mutiny.Uni;
import jakarta.transaction.Transactional;

@Transactional
public Uni<Void> renameCat(String name, String newName) {
    return repo.findByName(name)
            .invoke(cat -> cat.name = newName)
            .replaceWithVoid();
}
```

Changes to the managed entity are automatically flushed before the transaction commits. In reactive sessions, lazy associations must be fetched explicitly, even for managed entities; see [Hibernate Reactive](/guides/hibernate-reactive/index.md).

<a id="reactive-and-stateless"></a>
#### Reactive record entities and stateless sessions

Use `RecordEntity.Reactive` and `RecordRepository.Reactive` for reactive stateless sessions. Changes must be explicitly pushed by calling `update()`, and insertion uses `insert()` instead of `persist()`.

```java
import io.quarkus.data.hibernate.RecordEntity;
import io.quarkus.data.hibernate.RecordRepository;
import io.smallrye.mutiny.Uni;
import jakarta.data.repository.Find;
import jakarta.persistence.Entity;

@Entity
public class ReactiveCatRecord extends RecordEntity.Reactive {
    public String name;

    public interface Repo extends RecordRepository.Reactive<ReactiveCatRecord> {
        @Find
        Uni<ReactiveCatRecord> findByName(String name);
    }
}
```

With an injected `ReactiveCatRecord.Repo repo`, explicitly update the modified entity:

```java
import io.smallrye.mutiny.Uni;
import jakarta.transaction.Transactional;

@Transactional
public Uni<Void> renameCat(String name, String newName) {
    return repo.findByName(name)
            .chain(cat -> {
                cat.name = newName;
                return cat.update();
            });
}
```

The [getting started tutorial](/guides/quarkus-data-hibernate-getting-started/index.md#reactive) describes the required dependencies and reactive datasource configuration. You can use any of the [supported reactive drivers](/guides/hibernate-reactive/index.md#hr-getting-started).

<a id="entity-and-repository-type-matrix"></a>
### Entity and repository type matrix

The following table lists the entity and repository supertypes for each operation model and identifier type. You do not have to extend `WithId<Id>` if you define your own entity identifier field:

| Session type | ID type | Entity superclass | Entity superinterface | Repository supertype |
| --- | --- | --- | --- | --- |
| `Session` (managed, blocking) | `Long` | `ManagedEntity` |  | `ManagedRepository<Entity>` |
| `Session` (managed, blocking) | `Id` | `WithId<Id>` | `ManagedEntity.CustomId` | `ManagedRepository.CustomId<Entity, Id>` |
| `StatelessSession` (stateless, blocking) | `Long` | `RecordEntity` |  | `RecordRepository<Entity>` |
| `StatelessSession` (stateless, blocking) | `Id` | `WithId<Id>` | `RecordEntity.CustomId` | `RecordRepository.CustomId<Entity, Id>` |
| `Mutiny.Session` (managed, reactive) | `Long` | `ManagedEntity.Reactive` |  | `ManagedRepository.Reactive<Entity>` |
| `Mutiny.Session` (managed, reactive) | `Id` | `WithId<Id>` | `ManagedEntity.Reactive.CustomId` | `ManagedRepository.Reactive.CustomId<Entity, Id>` |
| `Mutiny.StatelessSession` (stateless, reactive) | `Long` | `RecordEntity.Reactive` |  | `RecordRepository.Reactive<Entity>` |
| `Mutiny.StatelessSession` (stateless, reactive) | `Id` | `WithId<Id>` | `RecordEntity.Reactive.CustomId` | `RecordRepository.Reactive.CustomId<Entity, Id>` |

## Startup code

For startup code, use `jakarta.transaction.Transactional` for both blocking and reactive operations. See the [application lifecycle guide](/guides/lifecycle/index.md#startup_annotation) for information about `@Startup`.

**Blocking startup**

```java
import io.quarkus.runtime.Startup;
import jakarta.transaction.Transactional;

public class OnStart {
    @Startup
    @Transactional
    public void startupMethod() {
        Cat_.repo().deleteAll();
    }
}
```

**Reactive startup**

```java
import jakarta.transaction.Transactional;
import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;

public class OnStartReactive {
    @Startup
    @Transactional
    public Uni<Void> startupMethod() {
        return ReactiveCat_.repo().deleteAll().replaceWithVoid();
    }
}
```

<a id="jakarta-data"></a>
## Jakarta Data

Quarkus Data Hibernate builds on the [Jakarta Data](https://jakarta.ee/specifications/data/1.1/jakarta-data-1.1.html) repository model and supports its annotations directly. Entities can extend a variant of `ManagedEntity` or `RecordEntity`, or neither. The Jakarta Data API is provided by the `quarkus-data-hibernate` extension, so no additional dependency is required.

For portable repository methods, use the Jakarta Data `@Find`, `@Query`, and `@Delete` annotations. Quarkus Data Hibernate also supports the Jakarta Persistence-specific `@NativeQuery` annotation, which can be used alongside Jakarta Data annotations in the same repository. The following example uses only Jakarta Data annotations:

```java
import io.quarkus.data.hibernate.RecordRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.util.List;

@Entity
public class Cat {
    @Id
    @GeneratedValue
    public Long id;
    public String name;
    public LocalDate birth;
    public Breed breed;

    public enum Breed {
        CUTE, HAIRLESS;
    }

    public interface Repo extends RecordRepository<Cat> {
        @Find <1>
        Cat findByName(String name);

        @Query("where breed = CUTE") <2>
        List<Cat> findCute();

        @Delete <3>
        long deleteByName(String name);

        @Query("delete from Cat where breed = HAIRLESS") <4>
        long deleteHairless();
    }
}
```

1. `@Find` defines a finder method from its parameters.
2. `@Query` defines a query using the Jakarta Data Query Language.
3. `@Delete` defines a finder-style delete method and can return the number of deleted entities.
4. A delete operation can also be written explicitly with `@Query`, which also supports update queries.

Repositories are CDI beans and are also available through generated metamodel accessors such as `Cat_.repo()`.

Refer to the [Hibernate Data Repositories](https://hibernate.org/repositories/) and [Jakarta Data](https://jakarta.ee/specifications/data/1.1/jakarta-data-1.1.html) documentation for the complete repository API.

## REST integration

When your application uses both `quarkus-data-hibernate` and `quarkus-rest-jackson`, Quarkus automatically adds REST support for common Jakarta Data parameter types. You can declare these types directly as REST endpoint parameters, and they are populated from HTTP query parameters without manual parsing.

### Supported parameter types

The following Jakarta Data types are automatically mapped from query parameters:

| Jakarta Data type | Query parameters | Defaults | Example URL |
| --- | --- | --- | --- |
| `PageRequest` | `page`, `size`, `requestTotal` | `page=1`, `size=10`, `requestTotal=true` | `?page=2&size=25` |
| `Sort<Cat>` | `sort` | `null` | `?sort=name` (ascending) or `?sort=-name` (descending) |
| `Order<Cat>` | `sort` (repeating) | empty `Order.by()` | `?sort=name&sort=-salary` |
| `Limit` | `limit`, `startAt`, `endAt` | `null` (no limit), `startAt=1` | `?limit=10` or `?limit=10&startAt=51` or `?startAt=51&endAt=60` |
| `Direction` | `direction` | `null` | `?direction=ASC` or `?direction=desc` |

For `Sort` and `Order`, prefix the property name with `-` to indicate descending order.

When binding sort parameters from REST endpoints to `DataQuery`, prefer `Order<Cat>` over `Sort<Cat>`. A missing `sort` query parameter yields `null` for `Sort<Cat>` but an empty `Order.by()` for `Order<Cat>`. If the `Sort<Cat>` parameter is `null`, `.sort(null)` is a no-op. `Order` is preferred when multiple columns can be requested.

> [!NOTE]
> Cursor-based pagination (`PageRequest.Mode.CURSOR_NEXT` and `CURSOR_PREVIOUS`) is not currently supported by this REST integration. Only offset-based pagination is available through query parameters.

### Page serialization

When a REST endpoint returns a `jakarta.data.page.Page`, it is serialized as a JSON object with pagination metadata, rather than as a plain array:

```json
{
  "content": [ ... ],
  "hasNext": true,
  "hasPrevious": false,
  "totalElements": 100,
  "totalPages": 34
}
```

The `totalElements` and `totalPages` fields are only included when the `PageRequest` was created with `requestTotal=true`.

### Example endpoint

Here is an example of a REST endpoint that uses `PageRequest` and `Order` to return a paginated, sorted list of cats. Paging and sorting are configured on the `PanacheQuery` returned by `findAll()`:

```java
import jakarta.data.Order;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("cats")
public class CatResource {

    @Inject
    Cat.Repo repo;

    @GET
    public List<Cat> list(PageRequest pageRequest, Order<Cat> order) { // <1>
        return repo.findAll().pages().request(pageRequest).sort(order).list();
    }
}
```

1. `PageRequest` is populated from `page`, `size`, and `requestTotal` query parameters. `Order` is populated from repeating `sort` query parameters.

A client can then call:

```
GET /cats?page=1&size=20&sort=name&sort=-birth
```

This produces a `PageRequest.ofPage(1, 20, true)` and `Order.by(Sort.asc("name"), Sort.desc("birth"))`.

## Query languages

There are multiple technologies to express queries in Quarkus Data Hibernate. They are related languages rather than separate query engines: each standard defines a different level of functionality or portability.

- The [Hibernate Query Language](https://docs.hibernate.org/orm/8.0/querylanguage/html_single/)  (HQL) is Hibernate's full query language. Hibernate ORM accepts HQL in Jakarta Data's `@Query`, including features beyond the standard query languages.
- The [Jakarta Persistence Query Language](https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-M4.html#a4665) (JPQL) is the standard query language defined by Jakarta Persistence and is a subset of HQL. A valid JPQL query is also a valid HQL query. This language is evolving into the standalone [Jakarta Query specification](https://jakarta.ee/specifications/query/1.0/).
- The [Jakarta Data Query Language](https://jakarta.ee/specifications/data/1.1/jakarta-data-1.1.html#_jakarta_common_query_language) (JDQL) is the portable subset of JPQL used by Jakarta Data's `@Query` annotation. Restrict queries to JDQL when the repository should remain independent of Hibernate-specific query features.
- Panache Query Language adds concise forms for common HQL queries. It is accepted by query methods such as `find()`, `list()`, `update()`, and `delete()` on `ManagedRepository` and `RecordRepository`, and is expanded to HQL before execution.

### Panache Query Language

Most HQL select queries have the form `from EntityName [where ...] [order by ...]`, with optional elements at the end.

If a select query does not start with `from`, `select`, or `with`, the following additional forms are supported:

- `order by ...` expands to `from EntityName order by ...`.
- `<singleAttribute>` with a single parameter expands to `from EntityName where <singleAttribute> = ?`.
- `where <query>` expands to `from EntityName where <query>`.
- `<query>` expands to `from EntityName where <query>`.

If an update query does not start with `update`, the following additional forms are supported:

- `from EntityName ...` expands to `update EntityName ...`.
- `set? <singleAttribute>` with a single parameter expands to `update EntityName set <singleAttribute> = ?`.
- `set? <update-query>` expands to `update EntityName set <update-query>`.

If a delete query does not start with `delete`, the following additional forms are supported:

- `from EntityName ...` expands to `delete from EntityName ...`.
- `<singleAttribute>` with a single parameter expands to `delete from EntityName where <singleAttribute> = ?`.
- `<query>` expands to `delete from EntityName where <query>`.

## Repository security

Quarkus Security provides initial support for securing repositories with security annotations. Security annotations can be placed on methods directly declared by a repository.

```java
import io.quarkus.security.PermissionsAllowed;
import jakarta.data.repository.Find;

import java.util.List;

@PermissionsAllowed("cat:list")
@PermissionsAllowed("cat:read")
@Find
List<Cat> findByName(String name);
```

You can also place a security annotation on the repository interface:

```java
import io.quarkus.data.hibernate.ManagedRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.data.repository.Find;

import java.util.List;

@RolesAllowed("admin")
public interface SecuredCatRepo extends ManagedRepository<Cat> {
    @Find
    List<Cat> findByName(String name);
}
```

> [!WARNING]
> Only methods directly declared on an annotated repository interface are secured. Methods inherited from `ManagedRepository`, such as `findById`, `persist`, or `deleteAll`, are not secured because type-level annotations only affect the type they annotate.
>
> If you need security in the repository layer, consider [plain repository classes](#alternative-repository-definitions) that do not extend Quarkus Data ones, or roll out your own repository super interfaces, adding security annotations wherever relevant for your security model.

Generic interface methods using type variables or wildcards cannot currently be secured reliably with standard security annotations. Apply the security annotation to the calling method, such as a REST endpoint, or redeclare the repository method with a concrete type.

See the [Authorize access to web endpoints](/guides/security-authorize-web-endpoints-reference/index.md) guide for information about Quarkus Security annotations.

## Advanced usage

<a id="alternative-repository-definitions"></a>
### Standalone repositories and entities without supertypes

Neither an entity nor its repository has to extend a Quarkus Data supertype. This is useful when an entity already extends an application-specific superclass, or when the repository should expose only explicitly declared operations:

```java
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class PlainCat {
    @Id
    @GeneratedValue
    public Long id;
    public String name;
}
```

A repository can also be a top-level interface. Annotate standalone repositories with `jakarta.data.repository.Repository`:

```java
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

@Repository
public interface PlainCatRepository {
    @Find
    PlainCat findByName(String name);
}
```

The generated repository remains a CDI bean and can be injected as `PlainCatRepository`. It only exposes the declared finder, because it does not inherit persistence or query operations. A standalone repository can also extend `ManagedRepository<PlainCat>` or `RecordRepository<PlainCat>` to inherit those operations. Conversely, a nested repository can omit the supertype; nesting and inheritance are independent choices.

<a id="switching-between-managed-and-record-models"></a>
### Switching between managed and record models

You can access an entity using any supported session model, regardless of the entity supertype. Pick the supertype that represents the majority of your use cases and use the following accessors for alternative operations:

| Session type | Entity accessor | Equivalent entity type |
| --- | --- | --- |
| `Session` | `.managed()` | `ManagedEntity` |
| `StatelessSession` | `.record()` | `RecordEntity` |
| `Mutiny.Session` | `.managedReactive()` | `ManagedEntity.Reactive` |
| `Mutiny.StatelessSession` | `.recordReactive()` | `RecordEntity.Reactive` |

For repository operations, obtain alternative repositories from the generated metamodel accessors, or inject them with `@Inject`:

| Session type | Metamodel accessor | Repository type |
| --- | --- | --- |
| `Session` | `Cat_.managed()` | `ManagedRepository<Cat>` |
| `StatelessSession` | `Cat_.record()` | `RecordRepository<Cat>` |
| `Mutiny.Session` | `Cat_.managedReactive()` | `ManagedRepository.Reactive<Cat>` |
| `Mutiny.StatelessSession` | `Cat_.recordReactive()` | `RecordRepository.Reactive<Cat>` |

You can also define any number of repositories for custom operations within an entity. All nested repositories are available through generated metamodel accessors, such as `Cat_.repo()` and `Cat_.theOtherRepo()`.
