---
name: building-blog
description: >
  How to preview blog posts locally: one-command container-based
  Jekyll serve with auto-detection of changed posts and deep-linking.
---

# Building Blog Posts

## Supported Platforms

This workflow is supported on Linux, macOS, and Windows through WSL2.
Native Windows shells (PowerShell, CMD, Git Bash) are not supported.

## Quick Start

Run the full preview pipeline in one command:

```bash
just blog-preview
```

Or directly:

```bash
bash blog-preview.sh
```

## How It Works

The script uses marker files to track state across runs:

* `.blog-preview-last-run` — timestamp of the last preview run.
  Used to detect recently changed posts and open the right URL.
* `.blog-preview.lock` — directory-based lock to prevent concurrent runs.

Both files are gitignored. Delete `.blog-preview-last-run` to reset
change detection.

## What the Script Does

### Step 1 — Environment Detection

Detects the container runtime (`podman` or `docker`), SELinux state
(sets `:z` volume flag if needed), and browser command (`xdg-open`,
`open`, or `wslview`).

### Step 2 — Container Management

Builds a Jekyll image from `jekyll-container/Dockerfile` in this repo
and starts a preview container. If a healthy container is already
running from a previous invocation, it is reused without restart.

The container runs with:
* `--future` — always included so posts with future dates are visible
* `--livereload` — browser auto-refreshes on file changes (port 35729)
* `--incremental` — only rebuilds changed pages
* `_noguides_config.yml` — excludes guides for fast builds (~10s)
* Named volume `quarkus-blog-jekyll-bundles` — persists gems across restarts

### Step 3 — Detect and Open

The script auto-detects what you were working on and opens the right page:

| Changed posts | Preview URLs |
|---|---|
| 1 post | `/blog/` (listing) + `/blog/<slug>/` (deep-link) |
| 2–4 posts | `/blog/` (listing) + a tab for each post |
| 5+ posts | `/blog/` (listing only) |
| No changes | `/blog/` (listing only) |

The slug is derived from the filename: strip the `YYYY-MM-DD-` prefix
and the file extension (`.adoc`, `.asciidoc`, or `.md`).

## Iteration Loop

```
Edit .adoc → save → Jekyll auto-rebuilds → browser auto-refreshes
```

The container stays running. LiveReload on port 35729 triggers the
browser refresh automatically — no manual reload needed.

## The `--future` Flag

The `--future` flag is always included because blog contributors
typically work on posts with today's or a future publication date.
Without this flag, Jekyll silently excludes future-dated posts from
the generated site.

## Troubleshooting

**Port 4000 or 35729 already in use** — Another process is using the
preview ports. Stop the existing container:
`podman rm -f quarkus-blog-preview` (or `docker`).

**`Failed to delete .cache/` or permission errors** — Rootless Podman
UID mapping. Fix: `podman unshare rm -rf .cache/`. With Docker:
`rm -rf .cache/`.

**Volume mount errors on macOS/Ubuntu** — SELinux `:z` flag applied
on a system without SELinux. The script detects this automatically;
if it persists, check `getenforce` output.

**Preview shows stale content** — Jekyll's `--incremental` mode can
sometimes miss dependency changes. Restart the container and re-run:
```bash
podman rm -f quarkus-blog-preview
just blog-preview
```

**Container image build fails** — Check the build log printed by the
script. To force a rebuild of the local image:
`podman rmi quarkus-blog-jekyll:local` (or `docker`).

**Post not appearing** — Check that the file is in `_posts/` with the
correct `YYYY-MM-DD-slug.adoc` naming and that the front matter `date`
matches the filename date.
