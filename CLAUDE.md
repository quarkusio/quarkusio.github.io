# Quarkus Website (quarkusio.github.io)

## Skills

When performing specific tasks, read the relevant skill file for detailed
guidance before starting work:

- **Building blog** — Read `.agents/skills/building-blog/SKILL.md`
  when previewing, testing, or troubleshooting blog post changes locally.
- **Release announcement** — Read `.agents/skills/release-announcement/SKILL.md`
  when creating a Quarkus minor or micro release announcement blog post.

## Project Structure

The site is built with [Roq](https://pages.quarkiverse.io/quarkus-roq/)
(a Quarkus-based static site generator), not Jekyll.

- `content/posts/` — Blog posts (`.adoc`, `.asciidoc`, or `.md` with `YYYY-MM-DD-slug` naming)
- `content/guides/` — Guides (maintained in the main Quarkus repo, do not edit here)
- `content/versions/` — Versioned guide snapshots
- `content/redirects/` — Redirect pages
- `config/` — Roq/Quarkus configuration (`application.properties`)
- `_data/` — YAML data files (`authors.yaml`, `versions.yaml`, events, books)
- `assets/` — Static assets (images, JavaScript, CSS)
- `pom.xml` — Maven build (Quarkus/Roq)

## Blog Post Conventions

- Location: `content/posts/YYYY-MM-DD-slug.adoc` (date must match front matter `date`)
- Front matter: `layout: post`, `title`, `date`, `tags`, `synopsis`, `author`
- Author must be defined in `_data/authors.yaml`
- Tags: lowercase, space-separated
- Permalink pattern: `/blog/:Name/` (configured in `config/application.properties`)
- Images: store in `assets/images/posts/<slug>/`, reference with `:imagesdir:`
