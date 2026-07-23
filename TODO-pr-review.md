# PR #2784 Review Comments — TODO

## Simple text fixes (apply to current file first, before split)

- [x] 1. Remove hardcoded version 3.37.0.CR1 from create-app
  - [FroMage L38](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474324243)
- [x] 2. Remove `target/generated-sources/annotations/` path — not accurate for Gradle
  - [Yoan L57](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472630195)
- [x] 3. Add link to datasource guide listing available JDBC extensions
  - [Yoan L52](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472619481)
- [x] 4. Replace @SQL/@NativeQuery user-facing note with a TODO comment
  - [Yoan L174](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472750545)
  - [FroMage L174](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474539795)
- [x] 5. Remove "The annotation processor generates the implementation" from callout <5>, already said in <1>
  - [FroMage L175](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474544870)
- [ ] 6. Add footnote for the intentional `proce` typo — typo exercise was for the conference, will be removed in the split tutorials
  - [FroMage L167](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474555076)
- [x] 7. Fix @Query callout: "avoids the need to write what you select"
  - [FroMage L302](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474655464)
- [x] 8. Fix "more powerful" → shorthand for WithId
  - [Yoan L460](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472908527)
- [x] 9. Fix dirty-checking explanation: focus on behavior, not implementation
  - [Yoan L674](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3475210272)
  - [FroMage L674](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474796936)
  - [FroMage L674](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3475367735)
- [x] 10. Rewrite managed-vs-stateless summary: remove N+1 mention, clarify
  - [Yoan L677](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472991574)
  - [FroMage L677](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474806993)
- [x] 11. Fix "Notice how the persistence concerns..." sentence
  - [FroMage L560](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474767275)
- [x] 12. Dev Services TIP: move right after `quarkus test`
  - [Yoan L145](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472735988)

## Structural changes (apply to current file, then carry into split)

- [x] 13. Remove "About entity identifiers" section — replaced with one-line link in callout <2>
  - [Yoan L467](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472901485)
  - [FroMage L467](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474730433)
- [x] 14. Remove "managed entities / detached / persisted" sentence from stateless NOTE box
  - [Yoan L456](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472894888)
  - [FroMage L456](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474718343)
- [x] 15. Start entities section with PanacheEntity.Stateless from the beginning
  - [Yoan L367](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472887675)
  - [FroMage L360](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474673920)
- [ ] 16. Shorten the stateless→managed comparison
  - [FroMage L602](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474781973)
- [ ] 17. Fix lazy to-one association — will address after the split
  - [Yoan L520](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472952664)
  - [Yoan L556](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472927616)
  - [FroMage L520](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474745276)
  - [FroMage L547](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474763787)
- [ ] 18. Remove the "lecture" paragraph about why SQL is limited — will address in the split
  - [Yoan L261](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472821807)
  - [FroMage L261](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474578603)

## Deferred / not actionable in tutorial (track for later)

- [ ] 19. [DEFERRED] Auto-add annotation processor via `quarkus ext add` — needs Quarkus changes
  - [Yoan L59](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472657083)
  - [FroMage L59](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474452646)
  - [FroMage L59](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474454085)
- [ ] 20. [DEFERRED] Infer @RunOnVertxContext from UniAsserter — needs Quarkus changes
  - [Yoan L789](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3473021174)
  - [FroMage L789](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474824829)
  - [FroMage L789](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3475376459)
- [x] 21. FroMage's type renames — applied: RecordEntity, RecordRepository, ManagedEntity, ManagedRepository, package io.quarkus.data.hibernate
  - [FroMage L283](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474636027)
  - [FroMage L288](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474637158)
  - [FroMage L372](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474682846)
  - [FroMage L377](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474685419)
  - [FroMage L389](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474690814)
  - [FroMage L486](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474735863)
  - [FroMage L491](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474737701)
  - [FroMage L501](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474739268)
  - [FroMage L508](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474740903)
  - [FroMage L609](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474783500)
  - [FroMage L616](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474784584)
  - [FroMage L703](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474816893)
  - [FroMage L710](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474817698)
  - [FroMage L721](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474819406)

## Final: Split into two tutorials

- [x] 22. Split current tutorial into:
  - **quarkus-data-hibernate-getting-started.adoc** — Entity mapping, repositories, stateless vs managed, reactive
  - **quarkus-data-hibernate-sql.adoc** — Pure SQL queries with @SQL, records, no entities
- [x] 23. Add cross-links between the two tutorials
  - [FroMage L261](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474578603)
- [ ] 24. Close PR #2784, open new PR(s) with the split tutorials
- [ ] 25. Add a section on how to navigate to generated files, for both Maven and Gradle
  - [Yoan L57](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472630195)
  - [FroMage L57](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474434063)

## PR-level review comment

- Yoan's main review: the tutorial conflates 3 things — (1) entry point for Quarkus Data docs, (2) getting-started with entities/repos, (3) getting-started with SQL. Should be split. The "start with SQL → build up" approach doesn't work well for a tutorial.
  - [FroMage L143](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3474484704) — agrees, SQL-first schema is "scary as hell", split it off
  - [Yoan L143](https://github.com/quarkusio/quarkusio.github.io/pull/2784#discussion_r3472729375) — detailed reasoning for the split
