# Quarkus Guides Navigation Redesign — One-Pager

**Author**: Romain De Lugyhe | **Date**: February 2026 | **Status**: Proposal for March 2026 F2F

---

## The problem

The guides landing page organizes 268 guides by Diataxis content type, but 69% fall into a catch-all "General Guides" section. Users arrive thinking *"I need to connect to a database"* — not *"I need a how-to guide."* Once inside a guide, there is no persistent navigation: the only way to find another is to go back and re-scan the page. Contributors report keeping 12+ browser tabs open as a workaround ([issue #418](https://github.com/quarkusio/quarkusio.github.io/issues/418)).

## The proposal

Two orthogonal changes, delivered using pure Jekyll — no external service changes:

- **15 JTBD domains**: Group guides by user task ("Access and Manage Data," "Deploy to the Cloud") instead of content type. Diataxis types become secondary badges.
- **Persistent left sidebar**: A collapsible domain tree on every guide page, plus prev/next navigation between guides.

## Implementation steps

| Step | Description | Depends on |
|---|---|---|
| 1. Define domain taxonomy | Create `domains.yaml` with 15 domains and guide assignments. Review with SMEs. | — |
| 2. Build guide sidebar | `<nav>` sidebar using `<details>/<summary>` — pure HTML/CSS, no JS. | Step 1 |
| 3. Redesign landing page | Replace card grid with domain-grouped compact list. Retain search components. | Step 1 |
| 4. Cross-guide navigation | Prev/next links at the bottom of each guide, driven by `domains.yaml` ordering. | Step 2 |
| 5. Update guide titles | JTBD-compliant titles ("Configure data sources" not "Configuring Data Sources"). | Step 1 |

Steps 2, 3, 5 run in parallel. Critical path: **Step 1 → Step 2 → Step 4**. Each step is independently revertible.

## The 15 domains (preliminary — refined with SMEs during Step 1)

Get Started (13) · Build Backend APIs (31) · Build Web UIs (6) · Access and Manage Data (35) · Secure Your Application (40) · Send and Receive Messages (21) · Deploy to the Cloud (38) · Observe Your Application (16) · Understand the Runtime (22) · Automate and Integrate (9) · Use Build Tools (13) · Use Spring APIs (9) · Write Extensions (15) · Contribute to Quarkus Docs (4) · Build AI Applications (placeholder)

Full guide mapping tables: [guides-landing-page-domains.md](guides-landing-page-domains.md)

## Key design decisions

- **Topic clusters, not subdomains**: Large domains (Security: 40, Deploy: 38) use visual groupings within a flat list, not nested collapsible sections. Fewer clicks, coherent learning paths, lower editorial overhead.
- **Search compatibility**: `<qs-guide>` elements are restyled via CSS, not replaced — Vue 3 search components work without modification.
- **Version-aware**: Sidebar resolves titles from the version-appropriate `quarkus.yaml`; guides absent from older versions are silently omitted.
- **Phase 2 (future)**: Makes the sidebar dynamic with search/filtering via `search.quarkus.io` web components. Not part of this request.

## The ask

**Approve the Phase 1 plan (Steps 1–5) and commit engineering time to execute it.**

Full plan: [guides-landing-page-plan.md](guides-landing-page-plan.md) · Domain taxonomy: [guides-landing-page-domains.md](guides-landing-page-domains.md)
