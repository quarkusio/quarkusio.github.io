# Component Categories Sync

Keeps `_data/componentcategories.yaml` current: for every component of the Quarkus platform (each platform extension,
and every artifact one of them brings in), which **Quarkus platform extension categories** it belongs to, and whether it
is one of Quarkus's own artifacts or a third-party dependency.

## What it does

1. Asks the [Quarkus registry](https://registry.quarkus.io) for the recommended platform release, as `quarkus create`
   does, unless a version is given.
2. Reads that release's **platform descriptor** (`io.quarkus.platform:quarkus-bom-quarkus-platform-descriptor`) with
   Quarkus's own registry client (`PlatformDescriptor.java`), as the Quarkus tooling and code.quarkus.io do. The
   descriptor has only the platform BOM's own extensions (not those of member BOMs such as Camel), and the platform's
   categories with their display names, in the platform's order.
3. Resolves the Maven dependency graph of each extension's runtime artifact (`DependencyGraph.java`, with the same
   resolver Quarkus uses: compile and runtime scopes, optional dependencies not followed). The first run downloads
   many POMs.
4. Assigns categories (`CategoryAssignment.java`):
   - a component that is itself a platform extension has that extension's categories;
   - anything else (a library, or a Quarkus module that is not an extension) has the categories of the platform
     extensions whose dependencies include it;
   - a library brought in by extensions of **3 or more categories** is used all over the platform, so it is `core`;
   - a component with no categories (an extension without any, or a library only such extensions bring in) is
     `miscellaneous`.
5. Writes the file, if it changed. Components are in alphabetical order and nothing in it changes between runs unless
   something real did.

## The data

```yaml
platform: 3.40.1.2             # the platform version it was worked out from
categories:                    # every category some component has, in the platform's order, with the platform's names
  - id: web
    name: Web
components:
  org.postgresql:postgresql:   # artifacts in group io.quarkus are named without it
    origin: third-party        # quarkus (group io.quarkus or under it, such as io.quarkus.arc) | third-party
    categories: [data]
```

The extensions that bring each component in (the evidence for its categories) are not written, since nothing uses them
and they made up most of the file. `CategoryAssignment` works them out, so they could come back if needed.

The file is typed with the website's own class, `ComponentCategories` (a Roq data mapping, so the site reads it as a
bean), which the script compiles with `//SOURCES ../../src/main/java/io/quarkusio/categories/ComponentCategories.java`.
`DataMapping.java` is a compile-only stand-in for Roq's `@DataMapping` annotation; it goes away with
https://github.com/quarkusio/quarkusio.github.io/pull/3251.

`ComponentCategoriesDataTest` checks the file as the site reads it.

## Tests

The category rules and the descriptor reader are tested without network access. The descriptor test reads
`test/platform-descriptor.json`, a **made-up** descriptor in the platform's shape (fake extensions, fake version 99.0.0):

```bash
cd tools/componentcategories-sync
jbang CategoryAssignmentTest.java
jbang PlatformDescriptorTest.java
```

## Running locally

```bash
jbang tools/componentcategories-sync/main.java
git diff _data/componentcategories.yaml
```

## CI

`.github/workflows/sync-componentcategories.yml` runs weekly and on demand, and commits the file if it changed. On PRs
that touch the tool, `ComponentCategories` or the workflow, it runs the tests and the sync, and shows what would change
without committing.

## Options and environment variables

- `--output` / `COMPONENTCATEGORIES_OUTPUT` - the file to write (default `_data/componentcategories.yaml`)
- `--platform` / `PLATFORM_VERSION` - the platform version (default the registry's recommended release)
