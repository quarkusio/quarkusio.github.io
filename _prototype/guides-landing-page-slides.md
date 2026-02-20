---
marp: true
theme: default
paginate: true
style: |
  section {
    font-family: 'Noto Sans', sans-serif;
    font-size: 28px;
  }
  h1 { color: #4695EB; }
  h2 { color: #333; }
  blockquote { border-left: 4px solid #4695EB; padding-left: 16px; color: #555; font-style: italic; }
  table { font-size: 22px; }
  code { font-size: 22px; }
  .columns { display: flex; gap: 40px; }
  .columns > div { flex: 1; }
  em { color: #888; }
---

# Redesigning Quarkus Guides Navigation

**From content types to user tasks**

Romain De Lugyhe
March 2026 F2F

---

# The story so far

**268 guides.** One landing page. Five years of growth.

The guides page was designed when Quarkus had dozens of guides.
It now has hundreds — and the design hasn't kept up.

> "Every time I need to look something up, I feel like I have to start from scratch."
> — GitHub issue #418 (open since 2020)

---

# Problem 1: How guides are grouped

The landing page organizes guides by **Diataxis content type**:
Tutorials · How-to Guides · Concepts · References

**But 185 of 268 guides (69%) are in a catch-all "General Guides" section.**

Nobody arrives at docs thinking *"I need a how-to guide."*

They think: *"I need to connect my app to a database"*
or *"I need to deploy to Kubernetes."*

---

# Problem 2: How users navigate

Once you click into a guide, the landing page **disappears**.

The only way back: the "Back to Guides" link.

Contributor @alinnert reports keeping **12 browser tabs open** to work around this — and built a **personal Obsidian vault** to recreate the navigation Quarkus lacks.

> "I don't think you can eliminate the need to jump between different guides...
> But that's why a quick way to navigate between pages is so important."

---

# What every exemplar site does

Every documentation site cited in issue #418 uses
a **persistent left sidebar** — not card grids.

- Laravel
- VitePress
- Docusaurus
- Stripe
- Vert.x

**Zero** use card grids for guide indexes.

---

# The proposal: two orthogonal fixes

| Problem | Fix |
|---|---|
| **Grouping** | Reorganize 268 guides into **15 task-based domains** |
| **Navigation** | Add a **persistent left sidebar** on every guide page |

The two changes are independent — you could ship one without the other.

This plan does both because issue #418 points to both problems.

**Pure Jekyll. No external service changes.**

---

# Fix 1: Organize by what users need to do

Group guides by **Jobs to Be Done** — what the user is trying to accomplish.

Each domain has a job statement:

> "When I need to store, retrieve, or manage persistent data,
> I want to connect to databases, use ORM frameworks, and manage schemas,
> so I can build data-driven applications reliably."

Diataxis types (Tutorial, How-to, Reference, Concept)
become **secondary badges** `[T]` `[H]` `[R]` `[C]` — not the grouping axis.

---

# The 15 domains

| Domain | Guides | | Domain | Guides |
|---|---:|---|---|---:|
| Get Started | 13 | | Observe Your Application | 16 |
| Build Backend APIs | 31 | | Understand the Runtime | 22 |
| Build Web UIs | 6 | | Automate and Integrate | 9 |
| Access and Manage Data | 35 | | Use Build Tools | 13 |
| Secure Your Application | 40 | | Use Spring APIs | 9 |
| Send and Receive Messages | 21 | | Write Extensions | 15 |
| Deploy to the Cloud | 38 | | Contribute to Docs | 4 |
| | | | Build AI Applications | *placeholder* |

*These are preliminary — refined with SMEs during implementation.*

---

# Fix 2: Persistent sidebar navigation

```
┌──────────────────────────────────────────────────────────┐
│  ◂ Back to Guides        Version: 3.19 ▾    Edit ✎      │
├──────────────┬────────────────────────────┬──────────────┤
│  SIDEBAR     │  GUIDE CONTENT             │ ON THIS PAGE │
│              │                            │              │
│ ▸ Get Start  │  # Configure data sources  │ • Datasource │
│ ▾ Data       │                            │ • Hibernate  │
│   · Configure│  Configure your datasource │ • Panache    │
│   · Hibern.. │  in application.properties │ • Flyway     │
│   ★ Panache  │                            │              │
│   · MongoDB  │  quarkus.datasource.db-kind│              │
│   · Flyway   │    = postgresql            │              │
│ ▸ Security   │                            │              │
│ ▸ Deploy     │                            │              │
├──────────────┴────────────────────────────┴──────────────┤
│  ◂ Previous: Use Hibernate ORM   Next: Use MongoDB ▸     │
└──────────────────────────────────────────────────────────┘
```

---

# What this changes for a developer

**Today**: Open guide → read → click "Back to Guides" → scroll → find next guide → repeat

**After**: Open guide → see sidebar → click next guide directly

A developer following REST → CDI → Persistence → Security
never leaves the navigation context.

Prev/next links at the bottom of each guide
provide a natural reading order within each domain.

---

# Five implementation steps

| Step | What | Depends on |
|---|---|---|
| **1. Define taxonomy** | Create `domains.yaml` — 15 domains, guide assignments. Review with SMEs. | — |
| **2. Build sidebar** | `<details>/<summary>` nav — pure HTML/CSS, no JS | Step 1 |
| **3. Redesign landing** | Domain-grouped compact list. Keep existing search. | Step 1 |
| **4. Prev/next nav** | Cross-guide links driven by `domains.yaml` ordering | Step 2 |
| **5. Update titles** | JTBD-compliant: "Configure data sources" not "Configuring Data Sources" | Step 1 |

Steps 2, 3, 5 run **in parallel**. Critical path: **1 → 2 → 4**

---

# What stays the same

- **URLs don't change** — no broken bookmarks or external links
- **Search keeps working** — `<qs-guide>` elements are restyled, not replaced
- **Diataxis types are preserved** — they become badges, not the primary axis
- **Version detection works** — sidebar resolves titles per version
- **Each step is revertible** — if the sidebar gets negative feedback, remove it without unwinding the taxonomy

---

# How we measure success

| Criterion | Target |
|---|---|
| Issue #418 resolved | Close with positive feedback from reporters |
| Sidebar reachability | 268/268 guides reachable within 2 clicks |
| Reduced tab-hopping | Fewer "Back to Guides" navigations per session |
| Community reception | Net positive sentiment on Zulip and GitHub |
| No domain confusion | 9/10 random guides locatable in sidebar within 10 seconds |

---

# Phase 2 vision (not part of this request)

After Phase 1 ships and community feedback is collected:

- **Search within the sidebar** — filter the domain tree in real time
- **Dynamic type/domain filtering** — sidebar respects the filter bar
- **Version-aware updates** — sidebar changes when switching doc versions
- **Synchronized highlighting** — search highlights guides in the sidebar

Requires changes to `search.quarkus.io` web components.
Phase 1 provides the editorial structure that Phase 2 makes interactive.

---

# The ask

**Approve the Phase 1 plan (Steps 1–5)
and commit engineering time to execute it.**

The domain taxonomy is a starting point —
guide assignments will be refined collaboratively
with extension maintainers and stakeholders.

Full plan + domain taxonomy available for review.

---

<!--
_class: lead
_paginate: false
-->

# Questions?
