---
name: building-blog
description: >
  How to preview blog posts locally using the Roq-based dev server:
  start the site with serve-noguides.sh for fast iteration.
---

# Building Blog Posts

## Supported Platforms

This workflow is supported on Linux, macOS, and Windows through WSL2.
Java 21+ and Maven (or the included `./mvnw` wrapper) are required.

## Quick Start

Run the full preview pipeline in one command — starts the dev server, detects which
posts you changed, and opens the right URLs in your browser:

```bash
just blog-preview
```

Or directly:

```bash
bash blog-preview.sh
```

To just start the dev server without the auto-open behaviour:

```bash
./serve-noguides.sh
```

Then browse to [http://localhost:8080/blog/](http://localhost:8080/blog/).

## How It Works

The site is built with [Quarkus Roq](https://docs.quarkiverse.io/quarkus-roq/dev/index.html),
a Quarkus-based static site generator. Running `mvn quarkus:dev` starts a live-reload
dev server — edits to content files are picked up automatically without a server restart.

`blog-preview.sh` starts the dev server in the background, waits for it to be ready,
then detects recently changed posts and opens the right URLs in your browser before
bringing the server to the foreground. Press Ctrl-C to stop.

### Detect and Open

The script auto-detects what you were working on and opens the right page:

| Changed posts | Preview URLs |
|---|---|
| 1 post | `/blog/` (listing) + `/blog/<slug>/` (deep-link) |
| 2–4 posts | `/blog/` (listing) + a tab for each post |
| 5+ posts | `/blog/` (listing only), unless git narrows it |
| No changes | `/blog/` (listing only) |

The slug is derived from the filename: strip the `YYYY-MM-DD-` prefix and the file extension.

Change detection uses a `.blog-preview-last-run` timestamp file on repeat runs, and
falls back to `git diff`/`git log` on first run. Delete `.blog-preview-last-run` to reset.

### Profiles

| Script | Profile | What it includes |
|---|---|---|
| `./serve.sh` | (default) | Full site with all guides |
| `./serve-noguides.sh` | `noguides` | Site without guides (fast) |
| `./serve-only-latest-guides.sh` | `only-latest-guides` | Site with latest + main guides |

For blog-only work, `serve-noguides.sh` is the fastest option.

## Blog Post File Conventions

- Location: `content/posts/YYYY-MM-DD-slug.adoc` (or `.asciidoc`, `.md`)
- Front matter fields: `layout: post`, `title`, `date`, `tags`, `synopsis`, `author`
- `author` must match a key in `_data/authors.yaml`
- Tags: lowercase, space-separated — e.g. `tags: extension kafka`
- Images: store in `public/assets/images/posts/<slug>/`, reference with `:imagesdir: /assets/images/posts/<slug>`

## Future-Dated Posts

Posts with a `date` value in the future are served normally by the local dev server.
No special flag or workaround is needed — just run `./serve-noguides.sh` and the post will appear.

## Iteration Loop

```
Edit content/posts/YYYY-MM-DD-slug.adoc → save → Roq rebuilds → browser refreshes
```

Quarkus dev mode watches for file changes and triggers an incremental rebuild automatically.

## Troubleshooting

**Port 8080 already in use** — Another process is occupying the default port.
Pass a different port: `./serve-noguides.sh -Dquarkus.http.port=8081`.

**Changes not appearing** — Quarkus dev mode watches source files. If a change is not picked
up, press `s` in the terminal running the dev server to force a restart, or stop and re-run
`./serve-noguides.sh`.

**Post not appearing** — Check that the file is in `content/posts/` with the correct
`YYYY-MM-DD-slug.adoc` naming and that the front matter `layout: post` and `author` fields
are present and valid.

**Slow first start** — The first run downloads Maven dependencies. Subsequent starts are fast.
