# Quarkus Website (quarkusio.github.io)

## Skills

When performing specific tasks, read the relevant skill file for detailed
guidance before starting work:

- **Building blog** — Read `.agents/skills/building-blog/SKILL.md`
  when previewing, testing, or troubleshooting blog post changes locally.

## Project Structure

- `_posts/` — Blog posts (`.adoc`, `.asciidoc`, or `.md` with `YYYY-MM-DD-slug` naming)
- `_guides/` — Guides (maintained in the main Quarkus repo, do not edit here)
- `_layouts/` — Jekyll layouts
- `_includes/` — Jekyll includes and partials
- `_data/` — YAML data files (`authors.yaml`, events, books)
- `_sass/` — SCSS stylesheets
- `_config.yml` — Main Jekyll configuration
- `_config_dev.yml` — Dev overrides (staging search cluster)
- `_noguides_config.yml` — Excludes guides for fast builds
- `jekyll-container/` — Dockerfile and entrypoint for containerized Jekyll
- `assets/` — Static assets (images, JavaScript, CSS)

## Blog Post Conventions

- Filename: `YYYY-MM-DD-slug.adoc` (date must match front matter `date`)
- Front matter: `layout: post`, `title`, `date`, `tags`, `synopsis`, `author`
- Author must be defined in `_data/authors.yaml`
- Tags: lowercase, space-separated
- Permalink pattern: `/blog/:title/`
- Images: store in `assets/images/posts/<slug>/`, reference with `:imagesdir:`
- Preview locally: `just blog-preview` (auto-detects changes, opens browser)
