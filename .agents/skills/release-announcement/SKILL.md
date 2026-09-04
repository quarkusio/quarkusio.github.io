# Quarkus Minor Release Announcement

Step-by-step guide for creating a Quarkus minor release announcement blog
post.

## Overview

A minor release announcement requires:

1. A blog post in `content/posts/`
2. An update to `_data/versions.yaml`

**Note:** The first release of a new minor series is not always `.0` — it
can be `.1` or higher (e.g. 3.39.1). The distinction between a minor and
micro announcement is whether this is the **first release of a new minor
series**, not whether the micro version is `0`. When in doubt, the user
will clarify.

## Step 1 — Gather Information

Before writing anything, collect all the data. Ask the user for:

- **The version number** (e.g. `3.39`)
- **The notable features** — the user will provide these, typically as
  a list of PR numbers and descriptions
- **The contributor count** from the GitHub UI (see below)

### Contributor count

The count displayed on https://github.com/quarkusio/quarkus (Contributors
section in the right sidebar) **cannot be reliably obtained via the GitHub
API**:

- The REST API `GET /repos/quarkusio/quarkus/contributors` is capped at
  500 results.
- The GraphQL `mentionableUsers` field includes all users with repo
  access, not just contributors.

**It must be obtained manually** by the user visiting
https://github.com/quarkusio/quarkus and reading the number from the
"Contributors" link in the right sidebar.

### Contributor list

Run the following in the `../quarkus/` repository, adjusting the version
range:

```bash
git -C ../quarkus fetch upstream --tags
git -C ../quarkus shortlog -s '<previous>.0'..'<current>.<latest_micro>' \
  | cut -d$'\t' -f 2 \
  | grep -v dependabot \
  | grep -v quarkusbot \
  | sort -d -f -i \
  | paste -sd ',' - \
  | sed 's/,/, /g'
```

- The **start** of the range is always the `.0` of the **previous** minor
  (e.g. `3.38.0` when announcing 3.39).
- The **end** of the range is the **latest released micro** of the current
  minor at the time of the announcement (e.g. `3.39.0`, or `3.39.2` if
  micro releases have already been published). Check the tags to find the
  latest micro.

**Post-processing the contributor list:**

1. **Deduplicate names with diacritics/accents**: some contributors have
   commits under both an ASCII and an accented version of their name
   (e.g. `Phillip Kruger` / `Phillip Krüger`, or `brunobat` / `Bruno
   Baptista`). Keep the version with diacritics/accents, or the real
   name over a username when both exist.
2. **Add `and ` before the last name** in the list.

### Platform component updates

Compare the previous and current minor branches of the
`quarkusio/quarkus-platform` repository:

```bash
gh api repos/quarkusio/quarkus-platform/compare/<previous>...<current> \
  --jq '.commits[].commit.message'
```

Filter for commits starting with `Update` or `Upgrade` (ignoring Quarkus
core upgrades, dependabot, and maven-release-plugin). For components not
covered by these commits (e.g. CXF, Flow), check the platform `pom.xml`
on the current branch to see if their versions changed:

```bash
gh api repos/quarkusio/quarkus-platform/contents/pom.xml?ref=<current> \
  --jq '.content' | base64 -d | grep -iE "cxf|flow"
```

When listing components, link to release notes when available:

- **Camel Quarkus**: `https://camel.apache.org/releases/q-<version>/`
  — these are **not available** at Quarkus announcement time; they are
  published later. Do not check or link them; just list the version.
- **Quarkus CXF**: `https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/<version>.html`
  (e.g. `https://docs.quarkiverse.io/quarkus-cxf/dev/release-notes/3.38.0.html`)
  — these are typically available; link when they exist. If CXF went
  through multiple versions (visible in the platform commit history),
  check and link release notes for each intermediate version. Use the
  term "release notes" (not "changelog") in link text to match the CXF
  documentation terminology.

## Step 2 — Create the Blog Post

### File location and naming

Create the file at:

```
content/posts/YYYY-MM-DD-quarkus-<major>-<minor>-released.adoc
```

For example: `content/posts/2026-09-30-quarkus-3-39-released.adoc`


### Front matter

```yaml
---
layout: post
title: 'Quarkus <version> - <headline features summary>'
tags: release
synopsis: 'We released Quarkus <version>, which comes with <brief feature list>.'
author: gsmet
---
```

- `layout`: always `post`
- `tags`: always `release` for release announcements
- `author`: must match a key in `_data/authors.yaml`
- `title`: include the version and a comma-separated summary of the
  most notable features (2-4 items)
- `synopsis`: one sentence summarizing what the release brings

### Blog post structure

Follow this exact structure (see recent posts in `content/posts/` for
reference):

```asciidoc
We're pleased to announce the release of Quarkus <version>.

This release brings several notable features:

* https://github.com/quarkusio/quarkus/pull/<number>[#<number>] - <description>
* ...

== Update

To update to Quarkus <version>, we recommend updating to the latest version of the Quarkus CLI and run:

[source,bash]
----
quarkus update
----

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus <version>.

For more information about the adjustments you need to make to your applications, please refer to the https://github.com/quarkusio/quarkus/wiki/Migration-Guide-<version>[Quarkus <version> migration guide].

== What's new?

=== <Feature 1 title>

<Description of feature 1>

=== <Feature 2 title>

<Description of feature 2>

...

=== Platform updates

Various Platform components were upgraded including:

- <Component> to <version>
- ...

== Full changelog

You can get the full changelog of https://github.com/quarkusio/quarkus/releases/tag/<version>.0.CR1[<version>.0.CR1] and https://github.com/quarkusio/quarkus/releases/tag/<version>.0[<version>.0] on GitHub.

== Contributors

The Quarkus community is growing and has now https://github.com/quarkusio/quarkus/graphs/contributors[<count>] contributors.
Many many thanks to each and everyone of them.

In particular for the <version> release, thanks to <contributor list>.

== Come Join Us

We value your feedback a lot so please report bugs, ask for improvements... Let's build something great together!

If you are a Quarkus user or just curious, don't be shy and join our welcoming community:

 * provide feedback on https://github.com/quarkusio/quarkus/issues[GitHub];
 * craft some code and https://github.com/quarkusio/quarkus/pulls[push a PR];
 * discuss with us on https://quarkusio.zulipchat.com/[Zulip] and on the https://groups.google.com/d/forum/quarkus-dev[mailing list];
 * ask your questions on https://stackoverflow.com/questions/tagged/quarkus[Stack Overflow].
```

### Notes on the structure

- The introductory paragraph can vary: if it's a lighter release (most
  features going to the next major), say so. See recent posts for tone.
- The bullet list at the top links PRs by number. Each item is a GitHub
  PR link followed by a short description.
- Each feature in "What's new?" gets its own `===` subsection.
- Platform updates go under their own `=== Platform updates` subsection,
  with an optional `==== Component upgrades` sub-subsection, or an
  `==== <Component name>` sub-subsection if a component deserves a
  paragraph of description (e.g. a new addition to the platform).
- When a platform component went through multiple version bumps (e.g.
  CXF 3.39.0 → 3.39.1), list the **final version** as the upgrade
  target, then link each version's release notes in parentheses:
  `Quarkus CXF to 3.39.1 (link[3.39.0 release notes], link[3.39.1 release notes])`

## Step 3 — Update `_data/versions.yaml`

Set `quarkus.version` to the new version and `quarkus.announce` to the
blog post path:

```yaml
quarkus:
  version: <version>.0
  announce: /blog/quarkus-<major>-<minor>-released/
```

For example:

```yaml
quarkus:
  version: 3.39.0
  announce: /blog/quarkus-3-39-released/
```

Only update `quarkus.version` and `quarkus.announce`. Do not touch
the other fields (`graalvm`, `jdk`, `maven`, `documentation`) unless
the user specifically asks.

## Step 4 — Preview

After creating the post and updating versions, suggest the user preview
locally. The blog preview skill (`.agents/skills/building-blog/SKILL.md`)
has full instructions. If `just blog-preview` or `blog-preview.sh` are
available, use them. Otherwise, the Roq dev mode can be started with:

```bash
./mvnw quarkus:dev
```

## Micro Release Announcements

Micro releases (e.g. 3.38.3) are much simpler. The structure is:

```
content/posts/YYYY-MM-DD-quarkus-<major>-<minor>-<micro>-released.adoc
```

Front matter:

```yaml
---
layout: post
title: 'Quarkus <version> - Maintenance release'
date: YYYY-MM-DD
tags: release
synopsis: 'We released Quarkus <version>, a regular maintenance release.'
author: gsmet
---
```

Body (much shorter, no feature descriptions or contributor list):

```asciidoc
Today, we released Quarkus <version>, a maintenance release for our <minor> release train.

This release contains bugfixes and documentation improvements.
It should be a safe upgrade for anyone already using <minor>.

== Update

To update to Quarkus <minor>, we recommend updating to the latest version of the Quarkus CLI and run:

[source,bash]
----
quarkus update
----

Note that `quarkus update` can update your applications from any version of Quarkus (including 2.x) to Quarkus <minor>.

For more information about the adjustments you need to make to your applications, please refer to the https://github.com/quarkusio/quarkus/wiki/Migration-Guide-<minor>[Quarkus <minor> migration guide].

== Full changelog

You can get the full changelog of https://github.com/quarkusio/quarkus/releases/tag/<version>[<version>] on GitHub.
```

For micro releases, also update `_data/versions.yaml` with the new
micro version number and set the announce path accordingly.
