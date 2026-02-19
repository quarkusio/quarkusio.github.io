# Developmental Edit (Round 2): guides-landing-page-plan.md

This second review covers issues that remain after the first 15 recommendations were implemented. It focuses on internal contradictions, underspecified mechanisms, missing edge cases, and argument gaps that a reviewer at the F2F will likely probe.

---

## 1. The Summary never states the ask

The Summary (lines 3-12) says what is being proposed, why, and how — but never says what the reader is being asked to *do*. A decision-maker reading this at the F2F needs to know: "Am I approving the taxonomy? Approving the implementation? Approving a timeline? Allocating headcount?" The audience statement says this document is "for the Quarkus documentation team and stakeholders at the March 2026 F2F," but the Summary treats the reader as a passive consumer, not someone who needs to act.

**Recommendation**: Add a final sentence to the Summary: "We are asking the documentation team to approve the 15-domain taxonomy and the Phase 1 implementation plan, and to commit engineering time for Steps 1-6." Adjust the specifics to match what you actually need from the F2F.

**Decision**: Add a final sentence to the Summary: (please improve it, too) "We are asking the stakeholders to approve the the Phase 1 implementation plan (Steps 1-6). Please note that the taxonomy presented here is a only starting point for discussions and will be reworked during Phase 1. Topic clusters will be significantly improved during this process." 

---

## 2. The `:domain:` attribute and `domains.yaml` are dual sources of truth

Step 2 proposes two things:

1. A `_data/domains.yaml` file that lists which guides belong to which domain
2. A `:domain:` attribute in each `.adoc` front matter that assigns the guide's primary domain

These are two sources of truth for the same fact. If someone adds a guide to `domains.yaml` but forgets the `:domain:` attribute (or vice versa), the data diverges silently. The plan doesn't state:

- Which source is authoritative
- Whether one is generated from the other
- What happens when they disagree
- What the `:domain:` attribute is *used for* at build time (the sidebar template reads `domains.yaml`, not `:domain:`)

**Recommendation**: State the relationship explicitly. Two options:

- **Option A (yaml-authoritative)**: `domains.yaml` is the source of truth. Drop the `:domain:` front-matter attribute entirely — the sidebar and landing page both read from `domains.yaml`, so `:domain:` is unused. If the guide page needs to know its own domain (e.g., to open the right sidebar section), look it up from `domains.yaml` at build time, the same way titles are looked up.
- **Option B (adoc-authoritative)**: `:domain:` is the source of truth. Generate `domains.yaml` from `.adoc` front matter at build time (a simple script or Jekyll plugin). This keeps guide metadata co-located with the guide content.

Option A is simpler for Phase 1. Option B is more maintainable long-term. Either way, state which one you intend.

**Decision** state that Option A is for Phase 1 and Option B for Phase 2.

---

## 3. The Liquid template caching claim is incorrect

Step 3 (line 787) says:

> "The linear scan is acceptable because it runs once per build, not per page — Jekyll caches the rendered include."

This is wrong. The sidebar include uses `page.url` to determine the active page:

```html
<li {% if page.url == entry.url %}class="active"{% endif %}>
```

Because the output differs for every page, Jekyll cannot cache the rendered include. The template runs **268 times** — once per guide page. Each run iterates the full `guide_index` array for each domain entry.

The performance may still be acceptable in practice (Liquid is fast enough for this scale), but the stated justification is factually incorrect and a technically savvy reviewer will catch it.

**Recommendation**: Replace the caching claim with an honest performance note: "The template runs once per guide page (268 times per build). The title lookup is a linear scan per entry, but the total iteration count is bounded — roughly `268 pages × 18 entries per domain × 268 index entries` in the worst case. In practice, Jekyll builds complete in [X] seconds with this include. If build time becomes a concern, a Jekyll plugin could precompute a URL-to-title hash."

Better yet: measure it. Add `guide-sidebar.html` to a test build and report the actual build time delta.

**Decision**: Implement measure it. Add `guide-sidebar.html` to a test build and report the actual build time delta.

---

## 4. Domain table ordering doesn't follow the stated principle

Step 2 (line 743) states: "The ordering within each domain is editorial: tutorials and concepts first, then how-to guides, then reference."

The domain tables in Part I don't follow this order. Examples:

- **Build Backend APIs** (domain 2): Starts with 11 how-to guides, then a concept, then a tutorial, then 8 more how-to guides, then 8 references, then 2 more how-to guides, then a reference. The concept ("Understand gRPC in Quarkus") and tutorial ("Get started with gRPC") are buried in the middle.
- **Observe Your Application** (domain 8): Starts with a reference, then 2 tutorials, then alternates between how-to and reference.
- **Understand the Runtime** (domain 9): Starts with a concept, then a reference, then a how-to, then alternates between all three types.

This matters because the domain tables serve as the specification for `domains.yaml` ordering. If a developer builds `domains.yaml` from these tables, the stated ordering principle and the actual table order will conflict.

**Recommendation**: Either reorder every domain table to match the stated principle (tutorials → concepts → how-to → reference), or revise the stated principle to match the actual ordering intent (e.g., "ordered by topic cluster, with tutorials leading each cluster").

**Decision**: Revise the stated principle to match the actual ordering intent (e.g., "ordered by topic cluster, with tutorials leading, followed by concepts, how-to, and reference")

---

## 5. The Security learning path is unique but unjustified

Domain 5 (Secure Your Application) has a blockquote prescribing a specific reading order: "Security overview → Security architecture → Authentication mechanisms → ..." No other domain has this. At 40 guides, Security is the largest domain, but Data (35) and Deploy (38) are comparable in size and have no learning path.

A reviewer will ask: "If Security needs a learning path, why don't Data and Deploy? Is this a one-off editorial choice or a pattern that applies to all domains above N guides?"

**Recommendation**: Either:

- **Generalize**: State a policy — "Domains with more than 25 guides include a recommended learning path on the domain page." Then add learning paths to Data and Deploy as well.
- **Justify the exception**: Explain why Security specifically needs a learning path (e.g., "Security concepts build on each other — you can't configure OIDC multitenancy without first understanding OIDC authentication. Other large domains have more independent guides that can be read in any order.").

**Decision**: Generalize: State a policy — "Domains with more than 25 guides include a recommended learning path on the domain page." Then add learning paths to Data and Deploy as well.

---

## 6. Phase 2 commitment level is unstated

The Summary names Phase 2 alongside Phase 1. The Risks section references it ("Phase 2 replaces static HTML with a dynamic component"). But Phase 2 gets 30 lines, no deliverables table, no effort estimate, no success criteria, no risks, and no timeline.

A reviewer will ask: "Is Phase 2 committed work, aspirational, or contingent on Phase 1 results?" If it's aspirational, the Summary overstates it by presenting it as part of the plan. If it's committed, it needs the same rigor as Phase 1.

**Recommendation**: Add a one-paragraph commitment statement at the top of the Phase 2 section. For example: "Phase 2 is contingent on Phase 1 adoption. It will be scoped as a separate proposal after Phase 1 has been deployed for at least one release cycle and community feedback has been collected." This lets Phase 2 exist in the document as a vision without implying it's already approved.

**Decision**: Implement this recommendation.

---

## 7. Wireframes show features absent from the implementation steps

The guide page wireframe (lines 59-83) shows:

- A header bar: `◂ Back to Guides    Version: 3.19 ▾    Edit ✎` — not described in any implementation step
- A type legend: `[T] = tutorial  [H] = howto  [R] = reference  [C] = concept` — described for the landing page (Step 4) but not for guide pages
- A "Related" line in the footer: `Related: Use Panache · Configure Flyway migrations` — described in Step 5

If the header bar and type legend already exist in the current layout, the wireframe is accurate but misleading — it implies these are new. If they're new, they need implementation steps.

**Recommendation**: Add a brief annotation below each wireframe distinguishing new elements from existing ones. For example: "Elements in the wireframe that already exist in the current layout: version selector, edit link, related guides. New elements introduced by this plan: sidebar, prev/next navigation, type legend on guide pages."

**Decision**: Implement this recommendation.

---

## 8. Sidebar template is broken on versioned pages

The sidebar template (Step 3) hardcodes `site.data.versioned.latest.index.quarkus.types` for the title lookup. This is a bug, not an ambiguity.

The quarkusio.github.io site uses a **single-build architecture**: one Jekyll build renders all versions simultaneously. Guide sources live in `_guides/` (latest) and `_versions/3.27/guides/`, `_versions/main/guides/`, etc. Version-specific data lives in `_data/versioned/{VERSION}/index/quarkus.yaml`. The existing `_layouts/guides.html` already handles this correctly:

```liquid
{% assign versioned_page = page.url | startswith: '/version/' %}
{% if versioned_page %}
  {% assign docversion = page.url | replace_regex: '^/version/([^/]+)/.*', '\1' %}
{% else %}
  {% assign docversion = 'latest' %}
{% endif %}
{% assign docversion_index = docversion | replace: '.', '-' %}
{% assign relations = site.data.versioned[docversion_index].index.relations %}
```

The sidebar template needs to follow this same pattern. Without it:

- A guide added in 3.21 would appear in the 3.20 sidebar with a broken link
- A guide renamed in `latest` would show the wrong title on versioned pages
- Title lookups would silently fail for guides that don't exist in `latest`

Additionally, `domains.yaml` is not versioned — it lives at `_data/domains.yaml`, not under `_data/versioned/`. This means the sidebar will show the same domain structure across all versions. If a guide is removed or added between versions, the sidebar's guide list and the version's actual content will diverge.

**Recommendation**: Fix the template in Step 3 to use the existing version-detection pattern (`docversion_index`) instead of hardcoding `latest`. Add a note that `domains.yaml` is version-independent in Phase 1 — the domain structure reflects the `latest` release, and guides that don't exist in older versions are silently omitted from the sidebar (the title lookup fails gracefully). If per-version domain files are needed, that's a Phase 2 concern.

**Decision**: Implement this recommendation.

---

## 9. No accessibility intent statement

The plan introduces a significant navigation redesign and never uses the word "accessibility." The HTML choices are actually good — `<details>/<summary>` for collapse/expand has native keyboard support (Enter/Space to toggle, Tab to traverse), `<nav>` with `aria-label` is correct, and the `class="active"` pattern is standard. But the plan never states that accessibility has been considered, which invites a reviewer to ask.

Specific ARIA attributes (`aria-current="page"`, badge labeling) are implementation details that don't belong in a plan document — the Liquid template in Step 3 is illustrative, not production code. The one plan-level concern worth noting is the mobile fallback: the sidebar hides below 768px and is replaced by a "Browse all guides" link. This is standard practice (every documentation site cited in issue #418 does the same), but the plan could clarify whether a hamburger/drawer menu is planned for Phase 2 or whether the link is the intended long-term mobile experience.

**Recommendation**: Add one sentence to Step 3's design decisions: "The sidebar uses semantic HTML (`<nav>`, `<details>/<summary>`) for native keyboard and screen reader support. Implementation will add `aria-current='page'` to the active link and accessible labels to type badges." This signals intent without over-specifying.

**Decision**: Implement this recommendation.

---

## 10. One job statement's outcome clause is jargon

Job statements should be testable: can you imagine a real person saying this? Most pass this test. One doesn't:

- **Automate and Integrate**: *"...handle cross-cutting operational concerns without custom plumbing."* The situation clause is fine ("schedule tasks, send email, apply business rules, or integrate with external systems beyond HTTP and messaging") — it lists concrete user needs. But the outcome clause uses jargon no developer thinks in. Fix the outcome, keep the situation.

The **Understand the Runtime** statement (*"understand how Quarkus works under the hood — dependency injection, the reactive engine, virtual threads..."*) reads like jargon at first glance, but its target audience actually uses this vocabulary. A developer choosing between reactive and imperative, or deciding whether to adopt virtual threads, arrives at the docs thinking exactly these terms. Reframing this as a debugging scenario ("when something isn't working") would narrow the job to only one of its use cases — many developers read architecture docs proactively when making design decisions, not only when things break.

**Recommendation**: Fix only the Automate and Integrate outcome clause. For example: *"When I need to schedule recurring tasks, send email, apply business rules, or integrate with external systems beyond HTTP and messaging, I want purpose-built extensions, so I can add these capabilities without writing integration code from scratch."* Do not name specific technologies (Camel, Drools) in the job statement — the job is the user's need, not the tool.

**Decision**: Implement this recommendation.


---

## 11. No reversibility statement

The plan has success criteria but never states what happens if the changes don't land well. At the F2F, someone will ask "what if this doesn't work?" — a one-sentence answer is better than improvising.

Step 4 already partially addresses this by preserving `index-docs.html` as a fallback. The missing piece is making explicit that the six steps are independently revertible — you can revert the sidebar (Step 3) without reverting the taxonomy (Step 2), or revert titles (Step 6) without reverting the landing page (Step 4). That's the actually useful information for a decision-maker, not "everything is in git."

**Recommendation**: Add one sentence to the Phase 1 Estimated Effort section: "Each step produces an independent, revertible change. If community feedback is negative on the sidebar but positive on the domain grouping, the sidebar can be removed without unwinding the taxonomy."

**Decision**: Implement this recommendation.


---

## 12. Replacing `<qs-guide>` with `<li>` will break landing page search

Step 4 says "`<qs-guide>` elements are replaced with plain `<li>` links" and claims that wrapping content in `<qs-target>` will let search continue to work. This is wrong.

The search system is a Vue 3 app where `<qs-target>` finds `<qs-guide>` descendants at any depth and reads their structured attributes (`type`, `title`, `keywords`, `categories`, `summary`). It then shows/hides them by toggling CSS visibility. Plain `<li>` elements are invisible to this system — `<qs-target>` without `<qs-guide>` children does nothing. Search wouldn't degrade gracefully; it would stop working entirely.

The fix is straightforward: **keep `<qs-guide>` elements, restyle them as compact list items via CSS.** The current site already nests `<qs-guide>` elements 4-5 levels deep inside `<qs-target>` (inside grid wrappers, divs, and section elements). Adding domain `<section>` wrappers around groups of `<qs-guide>` elements won't break the search — it already traverses descendants at any depth. The card-to-list-item change is purely CSS: set `qs-guide { display: list-item; }` instead of the current grid span, and restyle the internal `<h4>` and description.

**Recommendation**: In Step 4, replace "The `<qs-guide>` elements are replaced with plain `<li>` links" with: "The `<qs-guide>` elements are restyled as compact list items via CSS. Each guide retains its `<qs-guide>` wrapper and structured attributes so that `<qs-target>` search filtering continues to work. The visual change is CSS-only — no web component modifications are needed."

**Decision**: Implement this recommendation.

---

## 13. Landing page anchor stability is a minor concern

The plan correctly says Step 6 doesn't change guide URLs. The landing page redesign does change the anchor structure — current type-based sections (`#tutorials`, `#howto`) become domain-based sections (`#backend-apis`, `#access-and-manage-data`).

In practice, this has near-zero impact. The guides landing page is a navigation hub, not a deep-linkable resource — external sites link to specific guides (`quarkus.io/guides/getting-started`), not to index page sections. When a browser navigates to an anchor that doesn't exist, it loads the page at the top — no error, no 404, just no auto-scroll. And the current page's type-section headings may not even have stable `id` attributes since the content is rendered by Vue web components.

Adding invisible anchor targets for `#tutorials` on a domain-grouped page is also awkward: tutorials are distributed across 15 domains, so there's nowhere sensible for the anchor to point.

**Recommendation**: No action needed in the plan. If the team wants belt-and-suspenders, add a comment in the `index-docs-v2.html` template: `<!-- Old anchors: #tutorials, #howto, #concepts, #reference — not preserved; no known external links -->`. This documents the decision without adding code.

---

## 14. The "Proposed Solution" introduction largely restates the Problem Statement

**Original claim**: Lines 44-55 restate information from the Summary and Problem Statement, and only the table is new.

**Analysis**: The redundancy claim is overstated. Of the three items flagged:

- "This plan addresses both problems" — is a structural bridge from Problem Statement to Solution. Without it, the table appears with no introduction. This is a standard document transition, not redundancy.
- "The two changes are orthogonal" — is **new information**. The Problem Statement describes two *problems*; it never says the *solutions* are independent. The orthogonality statement matters because it tells the reader these changes can be adopted separately.
- "Phase 1 (Approach B) ... Phase 2 (Approach C)" — adds the Approach B/C labels and links to the Phase 2 section. The Summary mentions phases but not approach labels.

The entire Proposed Solution introduction is 4 sentences plus a table — already tight. Trimming further would remove either the transition, the orthogonality insight, or the approach labels, all of which serve a purpose.

**Recommendation**: No action needed. The section is concise and each sentence carries information the reader hasn't seen yet.

---

## Summary of Recommendations

| # | Issue | Effort | Impact |
|---|-------|--------|--------|
| 1 | Summary doesn't state the ask | Low | High |
| 2 | `:domain:` attribute and `domains.yaml` are dual sources of truth | Low | High |
| 3 | Liquid template caching claim is factually incorrect | Low | Medium |
| 4 | Domain table ordering contradicts stated principle | Medium | Medium |
| 5 | Security learning path is unique but unjustified | Low | Medium |
| 6 | Phase 2 commitment level is unstated | Low | High |
| 7 | Wireframes show features absent from implementation steps | Low | Medium |
| 8 | Sidebar template is broken on versioned pages | Low | High |
| 9 | No accessibility intent statement | Low | Medium |
| 10 | One job statement's outcome clause is jargon | Low | Medium |
| 11 | No reversibility statement | Low | Low |
| 12 | Replacing `<qs-guide>` with `<li>` will break search | Low | High |
| 13 | Landing page anchor stability is a minor concern | Low | Low |
| 14 | Proposed Solution introduction is not redundant (no action) | — | — |
