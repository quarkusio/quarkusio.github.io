# Quarkus Website (quarkusio.github.io)

## Skills

When performing specific tasks, read the relevant skill file for detailed
guidance before starting work:

- **Building blog** — Read `.agents/skills/building-blog/SKILL.md`
  when previewing, testing, or troubleshooting blog post changes locally.

## Project Structure

- `content/posts/` — Blog posts (`.adoc`, `.asciidoc`, or `.md` with `YYYY-MM-DD-slug` naming)
- `content/` — Site pages (`.md` or `.adoc`)
- `content/_generated-doc/` — Guides synced from the main Quarkus repo (do not edit here)
- `templates/` — Qute layout templates (`layouts/`) and partials (`partials/`)
- `_data/` — YAML data files (`authors.yaml`, `siteConfig.yml`, events, books)
- `web/` — SCSS stylesheets and static assets
- `public/` — Static assets (images, JavaScript, fonts)
- `config/application.properties` — Main Roq/Quarkus configuration
- `config/application-noguides.properties` — Profile that excludes guides for fast builds
- `config/application-only-latest-guides.properties` — Profile with only latest guides
- `src/` — Java source (Roq extensions, site configuration beans) and tests
- `pom.xml` — Maven build descriptor

## Running Locally

```sh
./serve.sh                     # full site (includes guides)
./serve-noguides.sh            # fast startup, no guides
./serve-only-latest-guides.sh  # only latest + main guides
```

Requires Java 21+. Site is served at http://localhost:8080.

## Running Tests

```sh
./mvnw test
```

## Blog Post Conventions

- Filename: `YYYY-MM-DD-slug.adoc` (or `.asciidoc`, `.md`; date must match front matter `date`) in `content/posts/`
- Front matter fields: `layout: post`, `title`, `date`, `tags`, `synopsis`, `author`
- Author must be defined in `_data/authors.yaml`
- Tags: lowercase, space-separated (e.g. `tags: extension kafka`)
- Permalink pattern: `/blog/:name/`
- Images: store in `public/assets/images/posts/<slug>/`, reference with `:imagesdir:`
- Posts with future `date` values are included in the local dev server — no special flag needed
