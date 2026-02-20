# Guides Navigation Prototype: Maintainer Guide

This document is for the developer who needs to understand, debug, or extend the prototype code.

## File inventory

| File | Purpose | Generated? |
|------|---------|------------|
| `_data/domains.yaml` | 15 task-based domains with guide URLs, titles, types, and nav-titles | Initially generated; now manually maintained |
| `_includes/guide-sidebar.html` | Per-guide sidebar partial (collapsible domains, active page highlight) | No, hand-authored |
| `_includes/index-docs-v2.html` | Landing page: sidebar + domain-grouped link lists | No, hand-authored |
| `_layouts/guides.html` | Guide page layout (modified to add sidebar column + prev/next nav) | No, edited in place |
| `_layouts/documentation.html` | Guides index layout (modified to conditionally include `index-docs-v2.html`) | No, edited in place |
| `_sass/layouts/guide-sidebar.scss` | All sidebar, prev/next, and domain-section styles. Note: no underscore prefix — this project uses `guide-sidebar.scss`, not `_guide-sidebar.scss` | No, hand-authored |
| `assets/css/main.scss` | Main SCSS entry point (modified to import `guide-sidebar`) | No, edited in place |
| `_scripts/generate-domains-yaml.py` | Generates `domains.yaml` from `quarkus.yaml` and `_prototype/guides-landing-page-domains.md` | N/A (tool) |
| `_scripts/add-nav-titles.py` | Adds action-oriented `nav-title` fields to `domains.yaml` | N/A (tool) |

## Maintaining domains.yaml

Both the sidebar (`guide-sidebar.html`) and the landing page (`index-docs-v2.html`) read domain membership, guide ordering, and navigation titles from this file.

### Full regeneration (when quarkus.yaml changes)

```bash
cd quarkusio.github.io
python3 _scripts/generate-domains-yaml.py
```

This reads `_data/versioned/latest/index/quarkus.yaml` to build a title-to-URL lookup, parses domain sections from `_prototype/guides-landing-page-domains.md` (headings and markdown tables), and writes `_data/domains.yaml` with `url`, `title`, and `type` for each guide.

**Warning**: Full regeneration drops all `nav-title` fields. The sidebar falls back to displaying the full original titles until nav-titles are re-applied:

```bash
python3 _scripts/add-nav-titles.py
```

This reads `domains.yaml`, matches each `title` against the `OVERRIDES` dict in the script, and adds `nav-title` fields where the action-oriented title differs from the original. To add title mappings for new guides, edit the `OVERRIDES` dict in `_scripts/add-nav-titles.py`.

Only use full regeneration if the guide list has changed so extensively that manual edits are impractical.

### Manual edits (preferred)

For incremental changes (adding a new guide, removing a deleted guide, reordering, moving between domains), edit `_data/domains.yaml` directly. This preserves all existing nav-titles. See the [Author Guide](AUTHOR-GUIDE.md) for the entry format.

## Version-conditional layout

`_layouts/documentation.html` conditionally includes the new landing page:

```liquid
{% if site.data.versioned[docversion_index].index %}
  {% include index-docs-v2.html docversion=docversion %}
{% else %}
  {% include index-guides.html source=data_source docversion=docversion %}
{% endif %}
```

If `site.data.versioned[docversion_index].index` is falsy (no index data for that version), the layout falls back to the original `index-guides.html` template. If the new layout doesn't appear for a specific version, check that `_data/versioned/<version>/index/` contains the expected data files.

**Maintenance hazard**: `guide-sidebar.html` has its own version detection logic (lines 5-10) that mirrors `documentation.html`. If these fall out of sync, versioned guide URLs in the sidebar will break.

## Architecture decisions

### Why titles are stored in domains.yaml (not looked up from quarkus.yaml)

The original plan resolved titles at build time by scanning `quarkus.yaml`. This created an O(n^2) inner loop in Liquid (268 guides x 268 index entries per page) that would have been slow. Storing titles directly in `domains.yaml` makes templates simple: `{{ entry.title }}` or `{{ entry.nav-title | default: entry.title }}`.

### Why the landing page uses plain HTML instead of `<qs-guide>` cards

The `<qs-guide>` web component renders icons, card padding, and description text inside its shadow DOM. External CSS cannot reach into shadow DOM, so there's no way to restyle the cards as compact links. The landing page uses plain `<ul class="domain-guides"><li><a>` elements instead.

**Trade-off**: The `<qs-target>` search filtering no longer works on the domain link lists (it only filters `<qs-guide>` elements). Landing page search filtering is not yet implemented.

### Why nav-title exists as a separate field

Guide pages still show the original `title` from the `.adoc` source. Changing upstream titles requires docs team buy-in and is a separate effort. `nav-title` provides short, action-oriented titles in the sidebar without touching upstream files.

The sidebar and landing-page sidebar use: `{{ entry.nav-title | default: entry.title }}`
The landing-page content area uses: `{{ entry.title }}` (original titles, unchanged)

## CSS specificity hazards

The sidebar renders as `<nav>` with `<ul><li><a>` inside containers that have the `.guides` class. Four global CSS rules affect the sidebar (all paths relative to `_sass/`):

| Rule | File | Effect |
|------|------|--------|
| `nav { float: right }` | `core/includes/header-navigation.scss:23` | Floats all `<nav>` elements right |
| `nav ul li { float: left }` | `core/includes/header-navigation.scss:31` | Floats all nav list items left |
| `nav ul li a { font-size: 1.5rem }` | `core/includes/header-navigation.scss:38` | Makes all nav links large |
| `.guides ul li { margin-bottom: .5rem !important }` | `quarkus.scss:982` | Adds bottom margin to all guide list items |

Overrides in `guide-sidebar.scss`:
- `float: none` on `.guide-sidebar` and `.guide-sidebar li`
- Explicit `font-size: 0.8rem` and `line-height: 1.3` on `li a`
- `margin-bottom: 0 !important` on `li`

If you add new elements to the sidebar, check whether global rules are affecting them. Use browser DevTools to inspect computed styles.

## Grid layout

### Guide pages (`_layouts/guides.html`)

```
3/12 sidebar | 6/12 content | 3/12 TOC
```

- Sidebar: `width-3-12 hide-mobile`
- Content: `width-6-12 width-12-12-m`
- TOC: `width-3-12 hide-mobile`

### Landing page (`_includes/index-docs-v2.html`)

```
3/12 sidebar | 9/12 content
```

- Sidebar: `width-3-12 hide-mobile`
- Content: `width-9-12 width-12-12-m`

On mobile (below 768px), sidebar hides and content takes full width.

## Build workflow

### When to use incremental builds

SCSS changes to existing files are picked up by incremental builds. Use the `--incremental` flag for fast iteration on styles. If you add a new SCSS partial, you also need to add an `@import` in `assets/css/main.scss` — that change is picked up incrementally.

### When you need a full rebuild

Changes to `_data/`, `_includes/`, or `_layouts/` files require a full rebuild. Stop and restart the Jekyll container without `--incremental`:

```bash
podman stop quarkus-jekyll
podman run --rm --name quarkus-jekyll -d -p 4000:4000 -v .:/site:Z \
  bretfisher/jekyll-serve \
  bundle exec jekyll serve --force_polling -H 0.0.0.0 -P 4000 \
  --config _config.yml,_config_dev.yml
```

Full builds take ~5 minutes. Check completion with:

```bash
podman logs --tail 5 quarkus-jekyll
```

Look for "done in X seconds."

### Verifying after a build

Always use `http://127.0.0.1:4000/guides/` (not `localhost` -- IPv6 resolution issues on some systems).

To verify compiled CSS without a browser:

```bash
curl -s http://127.0.0.1:4000/assets/css/main.css | grep -c "guide-sidebar"
```

A non-zero count confirms the SCSS was compiled.

