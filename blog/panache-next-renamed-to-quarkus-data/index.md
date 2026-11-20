# Quarkus Hibernate with Panache Next renamed to Quarkus Data Hibernate

## Goodbye Hibernate with Panache Next… welcome Quarkus Data Hibernate 🥳

It's already been a while since we introduced [Quarkus Hibernate with Panache Next](/blog/hibernate-panache-next), and since then, we've decided to move closer to the [Jakarta Data](https://jakarta.ee/specifications/data/1.1/jakarta-data-1.1.html) standard, as well as rename the module to [Quarkus Data Hibernate](https://quarkus.io/version/main/guides/quarkus-data-hibernate).

I'll repeat the original disclaimer: this is a new extension, which is experimental, which means everything about it can (and probably will) change: the extension name, the package names, the class names and even the API. We are releasing it because we feel it's in a good shape to be tested and discussed by the community, and we hope to get better feedback before we commit to anything like names or API.

Please report feedback either on [Zulip](https://quarkusio.zulipchat.com/#narrow/channel/187038-dev/topic/WG.20-.20Quarkus.20Data/with/598039033) or via [GitHub issues](https://github.com/orgs/quarkusio/projects/50/views/1).

## What changed since last time?

- Pivoted from the Panache 1 [`Sort`](https://javadoc.io/doc/io.quarkus/quarkus-panache-common/latest/io/quarkus/panache/common/Sort.html) type to the Jakarta Data [`Order`](https://jakarta.ee/specifications/data/1.1/api/apidocs/jakarta.data/jakarta/data/Order.html)  type for sorting: <https://github.com/quarkusio/quarkus/issues/53429>
- Supported [`@Transactional`](https://quarkus.io/version/main/guides/hibernate-reactive-panache#transactions)  for Hibernate Reactive transactions: <https://github.com/quarkusio/quarkus/issues/47698>
- Support non-blocking [`@Startup`](https://quarkus.io/guides/lifecycle#startup_annotation) methods: <https://github.com/quarkusio/quarkus/issues/50175>
- The [`@Repository`](https://jakarta.ee/specifications/data/1.1/api/apidocs/jakarta.data/jakarta/data/repository/Repository.html)  annotation is now optional on nested repositories: <https://github.com/quarkusio/quarkus/issues/50178>
- Added a codestart: <https://github.com/quarkusio/quarkus/issues/51565>
- Make sure the [Hibernate Reactive](https://quarkus.io/guides/hibernate-reactive) dependency is optional: <https://github.com/quarkusio/quarkus/pull/52265>
- Deprecated the [`Parameters`](https://javadoc.io/doc/io.quarkus/quarkus-panache-common/latest/io/quarkus/panache/common/Parameters.html)  class in favour of [`Map.of`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Map.html#of()): <https://github.com/quarkusio/quarkus/pull/52583>
- Added [`upsert`](https://javadoc.io/doc/io.quarkus/quarkus-hibernate-panache-next/latest/io/quarkus/hibernate/panache/stateless/PanacheStatelessEntityOperations.html#upsert())  operation: <https://github.com/quarkusio/quarkus/pull/52574>
- Make `StatelessSession` injectable for [Hibernate Reactive](https://quarkus.io/guides/hibernate-reactive)  <https://github.com/quarkusio/quarkus/issues/47462>
- Support [security annotations](https://quarkus.io/guides/security-overview#key-features-of-quarkus-security)  on repositories: <https://github.com/quarkusio/quarkus/issues/53623>
- Support [`@InjectMock`](https://quarkus.io/guides/getting-started-testing#further-simplification-with-injectmock)  on repositories: <https://github.com/quarkusio/quarkus/issues/53873>

And many other bug fixes.

## Need feedback

We're currently working on various issues, but we could especially use feedback on the following topics:

### Paging

We're switching to Jakarta Data `Page` and `PageRequest` for paging, including using cursors. This one will introduce a new API on `PanacheQuery` for paging: <https://github.com/quarkusio/quarkus/issues/53431>

``\`java @Transactional void offsetPage() { PanacheBlockingQuery\<MyEntity> query = MyEntity\_.managedBlocking().findAll();

List\<MyEntity> list = query.paging().offset(0, 10).list(); while (query.paging().hasNext()) { list = query.paging().next().list(); } }

@Transactional void cursorPage() { PanacheBlockingQuery\<MyEntity> query = MyEntity\_.managedBlocking().findAll();

List\<MyEntity> list = query.paging().cursored(0, 10).list(); while (query.paging().hasNext()) { list = query.paging().next().list(); } }

@Transactional void limitPage() { PanacheBlockingQuery\<MyEntity> query = MyEntity\_.managedBlocking().findAll();

List\<MyEntity> list = query.limiting().limit(10).list(); } ``\`

### Names

In <https://github.com/quarkusio/quarkus/issues/53145> we are considering moving:

- Our package name from `io.quarkus.hibernate.panache` to `io.quarkus.data.hibernate` (this one is pretty obvious)
- The `PanacheEntity` name to `ManagedEntity` (for entities backed by a stateful session), and `RecordEntity` or `ActiveRecord` (for entities backed by a stateless session).
- The `PanacheRepository` name to… something that would go along with whatever names we pick for our entities 😬

Please comment on the issue if you like this or think of anything better. But please read all the discussion first :)

### Automatic configuration of required annotation processor

On <https://github.com/quarkusio/quarkus/pull/53901> we have a proof of concept that lets our Maven and Gradle builds automatically configure the required Hibernate Processor annotation processor, so that you can't forget to add them to your builds.

This raises questions about magic and compatibility with IDEs, so chime in if you have opinions about those.

## Let us know

We're still looking for feedback, and testers.

Give it a try, [read the documentation](https://quarkus.io/version/main/guides/quarkus-data-hibernate), and give us feedback either on [Zulip](https://quarkusio.zulipchat.com/#narrow/channel/187038-dev/topic/WG.20-.20Quarkus.20Data/with/598039033) or via [GitHub issues](https://github.com/orgs/quarkusio/projects/50/views/1).
