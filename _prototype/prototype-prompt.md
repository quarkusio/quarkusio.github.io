# Prototype Prompt: Quarkus Guides Navigation Redesign

Use the following prompt to build a working prototype of the redesigned guides navigation. The prototype implements Steps 1–4 of the plan in [guides-landing-page-plan.md](guides-landing-page-plan.md), using the domain taxonomy in [guides-landing-page-domains.md](guides-landing-page-domains.md).

---

## Prompt

You are building a working prototype for a Quarkus guides navigation redesign on the `quarkusio.github.io` Jekyll site (local clone at `/home/rdlugyhe/quarkus/quarkusio.github.io/`). The goal: replace the current Diataxis-type-grouped card grid with 15 JTBD (Jobs to Be Done) domain categories and add a persistent left sidebar on every guide page.

### Scope

**In scope (this prototype):**
- Step 1: Generate `_data/domains.yaml` from existing data
- Step 2: Build `_includes/guide-sidebar.html` (persistent sidebar on guide pages)
- Step 3: Build `_includes/index-docs-v2.html` (domain-grouped landing page)
- Step 4: Add prev/next navigation at the bottom of each guide page
- SCSS additions for the sidebar

**Out of scope:**
- Step 5 (retitling `.adoc` files) — editorial work, not a code change
- The `guides-configuration-reference` layout — config reference pages do not get a sidebar in this prototype
- Old-format versions (e.g., `2-16`, `3-2`) that use the `index-guides.html` fallback — the `{% else %}` branch in `_layouts/documentation.html` is unchanged
- Changes to the `search.quarkus.io` web components (Phase 2)

---

### Step 1: Generate `_data/domains.yaml`

Write a Python script (`_scripts/generate-domains-yaml.py`) that builds `_data/domains.yaml` programmatically. This avoids error-prone manual title matching.

**How it works:**

1. Parse `_data/versioned/latest/index/quarkus.yaml` to build a **title → {url, type, summary, keywords, categories, origin}** lookup. Iterate all five type keys (`reference`, `concepts`, `howto`, `tutorial`, `guide`) under `types:`.
2. Parse the 15 domain sections in `guides-landing-page-domains.md`. Each section has a markdown table whose first column is "Current Title." Match each current title against the quarkus.yaml lookup to resolve its URL and metadata. Flag any unmatched titles as warnings on stderr.
3. Output `_data/domains.yaml` in this structure:

```yaml
domains:
  - id: get-started
    title: "Get Started"
    job: "When I'm evaluating Quarkus or starting a new project, I want to create a working application quickly, so I can understand the developer experience and decide if Quarkus fits my needs."
    order: 1
    guides:
      - url: /guides/getting-started
      - url: /guides/getting-started-reactive
      # ... ordered list from the domain table
  - id: backend-apis
    title: "Build Backend APIs"
    job: "When I need to expose HTTP endpoints..."
    order: 2
    guides:
      - url: /guides/rest
      # ...
  # ... remaining 13 domains
```

Each domain entry has:
- `id`: kebab-case identifier (e.g., `get-started`, `backend-apis`, `access-manage-data`)
- `title`: display name (e.g., "Get Started", "Build Backend APIs")
- `job`: the JTBD job statement (the `>` blockquote text from each domain in `guides-landing-page-domains.md`)
- `order`: integer 1–15
- `guides`: ordered list of `- url: /guides/...` entries, in the same order as the domain table

For the 3 cross-referenced guides, add `also-in: true` on the secondary entry:
```yaml
      - url: /guides/cli-tooling
        also-in: true
```

Cross-referenced guides appear in **both** domains — e.g., "Use the Quarkus CLI" appears in both "Get Started" and "Use Build Tools." The `also-in` flag is for editorial tracking only; the sidebar and landing page render all entries identically.

**IMPORTANT — include `title` and `type` in each guide entry.** The original design stored only URLs and resolved titles at build time via inner loops against `quarkus.yaml`. This caused O(n²) Liquid iteration (272 guides × 268 quarkus.yaml entries × every page) that stalled builds at 8+ minutes and 3.7 GB RAM. Instead, the script must write `title` and `type` alongside each `url`:

```yaml
    guides:
      - url: /guides/getting-started
        title: "Creating Your First Application"
        type: tutorial
```

This lets Liquid templates read titles directly from `domains.yaml` with O(n) iteration per page.

**Title aliases:** Three guide titles in `guides-landing-page-domains.md` don't exactly match `quarkus.yaml`. The script needs a `TITLE_ALIASES` dict to map them:
- "Writing REST Services with Quarkus REST" → "Writing REST Services with Quarkus REST (formerly RESTEasy Reactive)"
- "Migrating to Quarkus REST" → "Migrating to Quarkus REST (formerly RESTEasy Reactive)"
- "Config Reference Guide" → "Configuration Reference Guide"

Run the script and commit the generated `_data/domains.yaml`. The script is a dev tool, not part of the Jekyll build.

---

### Step 2: Build `_includes/guide-sidebar.html`

Create a Liquid include that renders a persistent `<nav>` sidebar from `_data/domains.yaml`.

**Reuse existing patterns:**
- **Sticky positioning**: clone the `.toc` styles from `_sass/layouts/guide.scss` lines 30–56 (`position: sticky; top: 1rem; max-height: 90vh; overflow-y: auto; align-self: start;`)
- **Mobile hide**: use the existing `.hide-mobile` class from `_sass/quarkus.scss` (already used on the TOC at `_layouts/guides.html` line 68)
- **Dark mode**: use `var(--sec-background-color)`, `var(--link-color)`, `var(--main-text-color)` from `_sass/colormode.scss`
- **Version-aware URL construction**: copy the pattern from `_layouts/guides.html` lines 79–86

**Complete Liquid template:**

**CRITICAL — no inner loops.** Do NOT use nested `{% for g in all_guides %}` loops to look up titles by URL. This causes O(n²) build time that stalls Jekyll for 8+ minutes. Instead, read `title` and `type` directly from `domains.yaml` entries (see Step 1).

```liquid
{% comment %}
  Version detection — same pattern used in _layouts/guides.html lines 4-9.
{% endcomment %}
{% assign versioned_page = page.url | startswith: '/version/' %}
{% if versioned_page %}
  {% assign docversion = page.url | replace_regex: '^/version/([^/]+)/.*', '\1' %}
{% else %}
  {% assign docversion = 'latest' %}
{% endif %}
{% assign docversion_index = docversion | replace: '.', '-' %}

{% comment %}
  Detect which domain the current page belongs to,
  so we can open that <details> section by default.
{% endcomment %}
{% assign current_domain = "" %}
{% for domain in site.data.domains.domains %}
  {% for entry in domain.guides %}
    {% if page.url == entry.url %}
      {% assign current_domain = domain.id %}
    {% endif %}
  {% endfor %}
{% endfor %}

<nav class="guide-sidebar" aria-label="Guide navigation">
  {% for domain in site.data.domains.domains %}
  {% if domain.guides.size > 0 %}
  <details {% if current_domain == domain.id %}open{% endif %}>
    <summary>{{ domain.title }}</summary>
    <ul>
      {% for entry in domain.guides %}
        <li {% if page.url == entry.url %}class="active"{% endif %}>
          <a href="{% if docversion == 'latest' %}{{ site.baseurl }}{{ entry.url }}{% else %}{{ site.baseurl }}/version/{{ docversion }}{{ entry.url }}{% endif %}"
             {% if page.url == entry.url %}aria-current="page"{% endif %}>
            {{ entry.title }}
            {% if entry.type == 'tutorial' %}<span class="type-badge">[T]</span>
            {% elsif entry.type == 'howto' %}<span class="type-badge">[H]</span>
            {% elsif entry.type == 'reference' %}<span class="type-badge">[R]</span>
            {% elsif entry.type == 'concepts' %}<span class="type-badge">[C]</span>
            {% endif %}
          </a>
        </li>
      {% endfor %}
    </ul>
  </details>
  {% endif %}
  {% endfor %}
</nav>
```

**Key behaviors:**
- Only the current page's domain opens by default; all others are collapsed
- `<details>/<summary>` provides collapse/expand with zero JavaScript
- Cross-referenced guides (`also-in: true`) render identically in the sidebar — no special treatment needed here

---

### Step 3: Modify layouts and build the landing page

#### 3a. `_layouts/guides.html` — add sidebar column

Change the grid from `8/12 + 4/12` to `3/12 + 6/12 + 3/12`:

```html
<!-- CURRENT (lines 59-69): -->
<div class="guide">
  <div class="grid-wrapper">
    <div class="grid__item width-8-12 width-12-12-m">
      ...content...
    </div>
    <div class="grid__item width-4-12 width-12-12-m tocwrapper">
      ...toc...
    </div>
  </div>

<!-- REPLACE WITH: -->
<div class="guide">
  <div class="grid-wrapper">
    <div class="grid__item width-3-12 hide-mobile guide-sidebar-wrapper">
      {% include guide-sidebar.html %}
    </div>
    <div class="grid__item width-6-12 width-12-12-m">
      ...content (keep everything inside unchanged)...
    </div>
    <div class="grid__item width-3-12 hide-mobile tocwrapper">
      ...toc (keep unchanged)...
    </div>
  </div>
```

Use the existing `hide-mobile` class on the sidebar wrapper — do **not** create a new mobile-hide class. On mobile, both sidebar and TOC hide; content goes full-width via `width-12-12-m`.

#### 3b. `_includes/index-docs-v2.html` — domain-grouped landing page

Create a new include that replaces the type-grouped layout. **Reuse `_includes/index-doc-item.html`** to render each guide entry (it already wraps guides in `<qs-guide>` with all required attributes).

**CRITICAL — no inner loops.** The original design used nested `{% for g in all_guides %}` loops to look up titles. This caused O(n²) iteration that stalled builds. Read titles directly from `domains.yaml` entries instead. The `index-doc-item.html` include only needs `type`, `title`, `url`, and `docversion` — pass empty strings for `summary`, `keywords`, `categories`.

Structure:

```liquid
{% assign docversion = include.docversion %}
{% assign docversion_index = docversion | replace: '.', '-' %}

<div id="guides-app">
  {% comment %}
    Keep the existing <qs-form> filter bar exactly as-is from index-docs.html
    lines 7-45 — copy it verbatim. EXCEPT: remove the "By Category" dropdown
    since the new domain grouping replaces category filtering.
  {% endcomment %}
  <qs-form server="{{ site.search.host }}" ...>
    <section class="full-width-version-bg flexfilterbar">
      <div class="flexcontainer">
        <div class="flexlabel"><label>By Version</label></div>
        <div class="pulldown version">...version dropdown (unchanged)...</div>
        <div class="search">...search input (unchanged)...</div>
      </div>
    </section>
  </qs-form>

  <div><h1 class="title">{{ page.title }}</h1></div>

  <qs-target origins-with-relative-urls="quarkus">
    <div class="grid-wrapper guides" id="all-docs">

      <div class="grid__item width-3-12 hide-mobile guide-sidebar-wrapper">
        {% comment %} Sidebar with ALL domains expanded on landing page {% endcomment %}
        <nav class="guide-sidebar guide-sidebar--landing" aria-label="Guide navigation">
          {% for domain in site.data.domains.domains %}
          {% if domain.guides.size > 0 %}
          <details open>
            <summary>{{ domain.title }}</summary>
            <ul>
              {% for entry in domain.guides %}
              <li><a href="#{{ domain.id }}">{{ entry.title }}</a></li>
              {% endfor %}
            </ul>
          </details>
          {% endif %}
          {% endfor %}
        </nav>
      </div>

      <div class="grid__item width-9-12 width-12-12-m">
        {% for domain in site.data.domains.domains %}
        {% if domain.guides.size > 0 %}
        <section id="{{ domain.id }}" class="domain-section">
          <h2>{{ domain.title }}</h2>
          <p class="domain-job">{{ domain.job }}</p>
          <div class="grid-wrapper">
          {% for entry in domain.guides %}
            {% include index-doc-item.html
               type=entry.type docversion=docversion
               title=entry.title url=entry.url summary=""
               keywords="" categories="" origin="quarkus" %}
          {% endfor %}
          </div>
        </section>
        {% endif %}
        {% endfor %}
      </div>

    </div>
  </qs-target>
</div>
```

**Search compatibility — confirmed safe.** Each guide is rendered via `index-doc-item.html`, which outputs `<qs-guide>` elements inside `<qs-target>`. The `<section>` wrappers per domain add nesting depth, but this is safe because:

- **Local search** (fallback) uses `document.querySelectorAll("qs-target qs-guide")` — a CSS descendant selector that matches at any depth. `<section>` wrappers are transparent to it.
- **Remote search** (primary) replaces the `<qs-target>` slot content entirely with server-rendered results. The static DOM structure (including `<section>` wrappers) is not involved in the search rendering path — it only appears when no search is active, via `<slot></slot>`.

Verify after building: type a query in the search bar, confirm matching guides appear and non-matching guides are hidden.

**Category dropdown decision:** Remove the "By Category" dropdown from the filter bar. The old categories (alt-languages, architecture, cloud, data, etc.) are orthogonal to the new 15 domains. Keeping both would confuse users. The search input and version dropdown remain.

#### 3c. `_layouts/documentation.html` — switch to new include

Change line 17 from:
```liquid
  {% include index-docs.html docversion=docversion %}
```
to:
```liquid
  {% include index-docs-v2.html docversion=docversion %}
```

Keep `_includes/index-docs.html` as-is (fallback). The `{% else %}` branch for old-format versions (`index-guides.html`) is unchanged.

---

### Step 4: Add prev/next navigation

At the bottom of each guide page (in `_layouts/guides.html`), before the existing related content section, add prev/next links driven by `domains.yaml` ordering.

**CRITICAL — no inner loops.** Read titles directly from `domains.yaml` entries (which include `title`). Do NOT use nested `{% for g in all_guides %}` loops.

```liquid
{% comment %} Prev/next navigation within the current domain — reads titles from domains.yaml directly {% endcomment %}
{% assign current_domain_id = "" %}
{% for domain in site.data.domains.domains %}
  {% for entry in domain.guides %}
    {% if page.url == entry.url %}
      {% assign current_domain_id = domain.id %}
    {% endif %}
  {% endfor %}
{% endfor %}
{% assign prev_guide = nil %}
{% assign next_guide = nil %}
{% assign found_current = false %}
{% for domain in site.data.domains.domains %}
  {% if domain.id == current_domain_id %}
    {% for entry in domain.guides %}
      {% if found_current and next_guide == nil %}
        {% assign next_guide = entry %}
      {% endif %}
      {% if entry.url == page.url %}
        {% assign found_current = true %}
      {% endif %}
      {% if found_current == false %}
        {% assign prev_guide = entry %}
      {% endif %}
    {% endfor %}
  {% endif %}
{% endfor %}

{% if prev_guide or next_guide %}
<nav class="guide-prevnext" aria-label="Previous and next guides">
  {% if prev_guide %}
  <a class="guide-prevnext__prev" href="{{ site.baseurl }}{{ prev_guide.url }}">
    &#9666; {{ prev_guide.title }}
  </a>
  {% endif %}
  {% if next_guide %}
  <a class="guide-prevnext__next" href="{{ site.baseurl }}{{ next_guide.url }}">
    {{ next_guide.title }} &#9656;
  </a>
  {% endif %}
</nav>
{% endif %}
```

This gives each guide a natural reading order within its domain without any new data source.

---

### SCSS additions

Create `_sass/layouts/guide-sidebar.scss` (**no underscore prefix** — this project's SCSS partials do not use the `_` prefix convention; all files in `_sass/layouts/` are named without it) and add `@import "layouts/guide-sidebar";` to `assets/css/main.scss` (after the `layouts/guide` import on line 39).

**Reuse the TOC pattern** from `_sass/layouts/guide.scss` lines 30–56 as the starting point.

**CRITICAL — CSS specificity overrides.** The sidebar renders as `<nav>` with `<ul><li><a>` inside containers that have the `.guides` class. Four global rules compete and must be explicitly overridden:
- `nav { float: right }` (header-navigation.scss) — override with `float: none` on `.guide-sidebar`
- `nav ul li { float: left }` (header-navigation.scss) — override with `float: none` on `li` (without this, `<li>` elements float, collapsing `<ul>` height and causing `<details>` to flow inline instead of stacking vertically)
- `nav ul li a { font-size: 1.5rem }` (quarkus.scss) — override with explicit `font-size` on `li a`
- `.guides ul li { margin-bottom: .5rem !important }` (guides.scss) — override with `margin-bottom: 0 !important` on `li`

```scss
/* Sidebar wrapper — uses existing .hide-mobile class for responsive */
.guide-sidebar-wrapper {
  display: flex;
}

/*
 * Sidebar nav — mirrors .toc sticky pattern from guide.scss.
 *
 * SPECIFICITY NOTE: This sidebar renders as <nav> with <ul><li><a> inside
 * containers that have the .guides class. Four global rules compete:
 *   - "nav" (header-navigation.scss) sets float: right on all nav elements
 *   - "nav ul li" (header-navigation.scss) sets float: left on all nav li
 *   - "nav ul li a" (quarkus.scss) sets font-size: 1.5rem on all nav links
 *   - ".guides ul li" (guides.scss) sets margin-bottom: .5rem !important
 * To override these: float: none is set on the nav and li elements,
 * font-size and line-height are set explicitly on "li a", and
 * margin-bottom uses !important on "li".
 */
.guide-sidebar {
  position: sticky;
  top: 1rem;
  padding: 0.5rem 0.5rem 0.5rem 0;
  background-color: var(--sec-background-color);
  word-wrap: break-word;
  max-height: 90vh;
  overflow-y: auto;
  width: 100%;
  align-self: start;
  float: none;
  font-size: 0.8rem;
  line-height: 1.3;

  details {
    display: block;
    width: 100%;
    margin-bottom: 0.1rem;

    summary {
      cursor: pointer;
      font-weight: 600;
      font-size: 0.85rem;
      padding: 0.25rem 0.4rem;
      list-style: none;
      color: var(--main-text-color);

      &::-webkit-details-marker { display: none; }
      &::before {
        content: "\25B8 ";
        display: inline-block;
        width: 1em;
      }
    }

    &[open] > summary::before {
      content: "\25BE ";
    }
  }

  ul {
    list-style: none;
    padding-left: 0.8rem;
    margin: 0;
  }

  li {
    float: none;
    padding: 0.1rem 0.2rem;
    margin-bottom: 0 !important;
    border-left: 2px solid transparent;

    &.active {
      border-left-color: var(--link-color, #4695EB);
      font-weight: 600;
    }

    a {
      color: var(--link-color);
      text-decoration: none;
      display: block;
      font-size: 0.8rem;
      line-height: 1.3;
      &:hover { text-decoration: underline; }
    }
  }

  .type-badge {
    font-size: 0.7rem;
    opacity: 0.6;
    margin-left: 0.3rem;
    font-family: monospace;
  }

  /* Landing page variant: no sticky, no max-height */
  &--landing {
    position: static;
    max-height: none;
    overflow-y: visible;
  }
}

/* Prev/next navigation */
.guide-prevnext {
  display: flex;
  justify-content: space-between;
  padding: 1.5rem 0;
  margin-top: 2rem;
  border-top: 1px solid var(--sec-border-color, #aaa);

  a {
    color: var(--link-color);
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }

  &__prev {
    margin-right: auto;
  }

  &__next {
    margin-left: auto;
  }
}

/* Domain sections on landing page */
.domain-section {
  margin-bottom: 2rem;

  h2 {
    margin-bottom: 0.25rem;
  }

  .domain-job {
    color: var(--main-text-color);
    opacity: 0.7;
    font-style: italic;
    font-size: 0.95rem;
    margin-top: 0;
    margin-bottom: 1rem;
  }
}
```

---

### Current site architecture (reference)

Key files and how they connect:

| File | Role |
|------|------|
| `_layouts/base.html` | Base HTML shell. Body class = `page.layout`. Loads search WC script if `search_wc: true`. |
| `_layouts/documentation.html` | Landing page layout. `layout: base`, `search_wc: true`. Includes `index-docs.html` for versioned data, `index-guides.html` for legacy. |
| `_layouts/guides.html` | Individual guide layout. `layout: base`. 8/12 + 4/12 grid. Version detection. Related content section (lines 71–111). |
| `_includes/index-docs.html` | Landing page content. Groups guides by Diataxis type (tutorial, howto, concepts, reference, guide). Uses `index-docs-merge.html` to iterate and `index-doc-item.html` to render. |
| `_includes/index-doc-item.html` | Renders one `<qs-guide>` element. Takes `type`, `title`, `url`, `summary`, `status`, `keywords`, `categories`, `origin` as include params. Handles versioned URLs and external links. **Reuse this include on the landing page.** |
| `_includes/index-docs-merge.html` | Builds a flat array of guides by type from `quarkus.yaml`. Pattern: `{% for source in index %}{% for item in source[1].types[v_type] %}`. |
| `_data/versioned/{VERSION}/index/quarkus.yaml` | Generated guide index (~3354 lines for latest). Top-level `types:` with keys `reference`, `concepts`, `howto`, `tutorial`, `guide`. Each entry: `{title, url, type, summary, keywords, categories, extensions, topics, filename}`. Bottom: `categories:` list. |
| `_data/versioned/{VERSION}/index/relations.yaml` | Guide relations (sameExtensions, sameTopics) keyed by `/guides/slug`. |
| `_sass/core/grid.scss` | 12-column CSS Grid: `repeat(12, 1fr)`, `grid-gap: 1.5em`. Spans: `.width-N-12`. Mobile: `.width-N-12-m` at `$breakpoint-m: 48em`. |
| `_sass/layouts/guide.scss` | Guide page styles. **Lines 30–56: `.toc` sticky sidebar** — the pattern to clone. Lines 68–100: related content with type-based icon pseudo-elements. |
| `_sass/layouts/guides.scss` | `qs-guide` card styles (span 4/12). Category nav. Search styles. |
| `_sass/layouts/documentation.scss` | Filter bar: `.flexfilterbar`, `.flexcontainer`, version/category dropdowns, search input, return link. |
| `_sass/core/includes/header-navigation.scss` | **Specificity hazard:** `nav { float: right }` and `nav ul li { float: left }` apply to ALL `<nav>` elements globally. Must override with `float: none` in sidebar styles. |
| `_sass/quarkus.scss` | `.hide-mobile { @media screen and (max-width: 768px) { display: none; } }`. Also: `nav ul li a { font-size: 1.5rem }` applies globally — must override on sidebar links. |
| `_sass/colormode.scss` | Full CSS custom property palette. Key vars: `--link-color`, `--sec-background-color`, `--main-text-color`, `--sec-border-color`, `--card-outline`, `--card-background-color-hover`. Dark mode overrides under `html.dark`. |
| `assets/css/main.scss` | SCSS entry point. Imports all partials in order. **Add new import here.** |
| `assets/javascript/guides-version-dropdown.js` | Version dropdown handler — test that it still works with the new layout. |

### Constraints

- **Pure Jekyll**: Liquid templates, SCSS, and one Python script (dev tool). No new npm/gem dependencies.
- **No URL changes**: guide URLs stay the same.
- **Search compatibility**: guide entries are rendered via `index-doc-item.html` inside `<qs-target>`. The `<qs-guide>` elements retain all attributes. Verify search filtering works with the added `<section>` nesting.
- **Incremental**: each new file is independently revertible. `index-docs.html` is kept as a fallback.
- **Dark mode**: use CSS custom properties from `colormode.scss`. Never hardcode colors.

### Testing

A Jekyll dev server is already running in a podman container named `quarkus-jekyll` on port 4000 with `--incremental` mode. It auto-rebuilds when files change.

To restart it if needed:
```bash
cd /home/rdlugyhe/quarkus/quarkusio.github.io
podman stop quarkus-jekyll 2>/dev/null; podman rm quarkus-jekyll 2>/dev/null
podman run --rm --name quarkus-jekyll -d -p 4000:4000 -v .:/site:Z \
  bretfisher/jekyll-serve \
  bundle exec jekyll serve --force_polling --incremental -H 0.0.0.0 -P 4000 \
  --config _config.yml,_config_dev.yml
```

**Note:** Use `http://127.0.0.1:4000` (IPv4), not `localhost` — the container binds to `0.0.0.0` and `localhost` may resolve to IPv6 `::1` which won't connect.

Check build status with `podman logs --tail 20 quarkus-jekyll`. Look for a `done in X seconds` message.

**Incremental vs full rebuilds:**
- Prefer incremental builds (the default with `--incremental`). They pick up SCSS/CSS changes fine (~3-5 minutes).
- Incremental builds do NOT pick up changes to `_layouts/` or `_includes/` files. For those, stop and restart the container (triggers a full rebuild, ~5 minutes).
- After every build completes, reload `http://127.0.0.1:4000/guides/` and verify the expected changes are visible. Use Ctrl+Shift+R for a hard refresh to bypass cached CSS.

Then verify:
1. `http://localhost:4000/guides/` — domain-grouped landing page loads, search filters guides across domain sections
2. Click any guide (e.g., `/guides/getting-started`) — sidebar appears with "Get Started" domain expanded, current guide highlighted
3. Click a guide in a different domain from the sidebar — navigates correctly, that domain expands
4. Prev/next links at the bottom of each guide — navigate within the domain
5. Version dropdown — switching versions still works, sidebar omits version-absent guides
6. Resize to mobile width (<768px) — sidebar and TOC hide, content goes full-width
7. Dark mode toggle — sidebar colors follow the theme
