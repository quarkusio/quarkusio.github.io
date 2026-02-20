# Developmental Edit: guides-landing-page-plan.md

This review covers structure, audience, argument, scope, and internal consistency. It does not cover line-level copyediting.

---

## 1. The document serves two audiences but declares neither

The plan opens with strategic persuasion ("Why JTBD," "Why a Sidebar") aimed at decision-makers at the March F2F, then pivots to implementation specs (Liquid templates, SCSS class names, grid column widths) aimed at whoever builds it. These are different documents for different readers.

**Recommendation**: Add an explicit audience statement at the top. Then separate the two concerns:

- **Part I: The Proposal** — Why, What, and the domain taxonomy. This is what you present at the F2F.
- **Part II: The Implementation Spec** — How. This is what a developer uses to build it.

The domain tables live at the boundary — they're both proposal (showing the reorganization) and spec (providing the exact guide mappings). Accept that they serve double duty, but make Part I self-contained so a reader can stop after it and still understand the plan.

---

## 2. No executive summary

The document is 757 lines. A reader has to absorb "Why JTBD," "Why a Sidebar," 15 domain tables, a changelog, a gap analysis, a checklist, and a phased roadmap before they have the full picture. A decision-maker at the F2F won't do this.

**Recommendation**: Add a 5-8 line summary at the top, before "Why JTBD," that answers:

- What is being proposed (reorganize 268 guides into 15 JTBD domains; replace card grid with persistent sidebar)
- Why (issue #418 feedback, Diataxis type-first grouping fails at scale, no persistent navigation)
- How (Phase 1: pure Jekyll, 6 steps; Phase 2: integrate with search service)
- What the reader is being asked to decide

---

## 3. The domain tables dominate at the expense of the argument

Lines 48-496 (~450 lines, 60% of the document) are guide mapping tables. This is essential reference material, but it buries the strategic argument. A reviewer who opens the file sees one section of "why," then immediately drowns in 15 tables of 268 guide renames.

**Recommendation**: Two options, choose one:

- **Option A**: Move the 15 domain tables into a separate file (`guides-domain-mappings.md`) and reference it. The main plan file keeps only the domain names, job statements, and guide counts — roughly the content of the "Guide Distribution Summary" table but with the job statements inlined.
- **Option B**: Keep the tables but add a clear section heading like "Domain Catalog (reference)" and an explicit note: "Reviewers: you can skip to [Implementation Roadmap](#implementation-roadmap-phase-1-approach-b) — the tables below are implementation reference."

---

## 4. The two "Why" sections don't connect to each other

"Why JTBD" argues for domain-based grouping. "Why a Sidebar" argues for sidebar navigation. Both are sound, but they read as independent arguments. The document never makes explicit that these are **two orthogonal improvements** that compound:

- JTBD fixes the **grouping problem** (what goes where)
- The sidebar fixes the **navigation problem** (how users move between guides)

You could apply JTBD domains without a sidebar (just regroup the card grid). You could add a sidebar without JTBD (keep the Diataxis type sections). The plan does both, and the reader should understand why both are necessary.

**Recommendation**: Add a bridging paragraph between the two "Why" sections — or merge them into a single "Problem Statement" section that identifies both problems (grouping + navigation) and then presents the two-part solution.

---

## 5. The "Task-Flow Hub" reference is unexplained

Line 11 mentions the "Task-Flow Hub" approach in quotes. This was a named option from the Phase 1-3 analysis, but this standalone plan doesn't explain it. A new reader encounters a term that sounds like jargon without context.

**Recommendation**: Either drop the label (the concept is clear without it) or add a one-sentence definition: "The Task-Flow Hub organizes documentation by what users need to do, not by what the documentation is."

---

## 6. "Changes from v1 to v2" is insider revision history

This section (lines 520-532) documents the plan's own editing history. It explains why gRPC was merged into Backend APIs, why Core was split, etc. A first-time reader never saw v1, so this reads as noise. Worse, it sits between the domain catalog and the content gaps section, breaking the flow.

**Recommendation**: Either remove it entirely (the current domain structure should stand on its own merits) or move it to an appendix / collapsed section. If the rationale for individual domain boundaries matters to reviewers, fold those justifications into the domain descriptions themselves — for example, add a note under "Build Backend APIs" explaining why gRPC lives there.

---

## 7. Cross-referenced guides are mentioned but not resolved

The summary table notes "some guides cross-referenced from 2 domains" (line 518), and at least three guides appear in two domains:

- "Use the Quarkus CLI" → Get Started + Use Build Tools
- "Secrets in Configuration" → Secure Your Application + Understand the Runtime
- "Tips for writing native applications" → Deploy to the Cloud + Write Extensions

The plan never states:

- How many guides are cross-referenced in total
- Which domain "owns" the guide (determines where it appears in the sidebar)
- How cross-referencing is represented in `domains.yaml` and the sidebar (does the guide appear twice? does it appear in one domain with a cross-reference link in the other?)

**Recommendation**: Add a short policy statement. For example: "Each guide has one primary domain (its home in the sidebar). Cross-referenced guides appear as 'See also' links in secondary domains. The `domains.yaml` file uses an `also-in` field to generate these cross-references."

---

## 8. Kotlin is miscategorized

"Using Kotlin" (line 431) is placed in domain 12, "Use Spring APIs." Kotlin is not a Spring API. It appears there because the legacy categories grouped it near Spring, but JTBD should break that coupling.

**Recommendation**: Move "Use Kotlin" to either "Get Started" (if Kotlin is an alternative language onramp) or "Understand the Runtime" (if it's about how Kotlin interacts with CDI, reactive, etc.). Alternatively, consider whether a small "Use Alternative Languages" domain is warranted (Kotlin + any future Scala/Groovy guides), or whether language guides distribute naturally across existing domains.

---

## 9. The sidebar data model has a gap

The Liquid template in Step 3 references `entry.title`:

```html
<a href="{{ entry.url }}">{{ entry.title }}</a>
```

But the `domains.yaml` schema in Step 2 only has `url`:

```yaml
guides:
  - url: /guides/getting-started
```

Where does the title come from? Either:

- **Option A**: Add `title` to each entry in `domains.yaml` (duplicates data from `quarkus.yaml`)
- **Option B**: Look up the title from `quarkus.yaml` at build time (requires a Liquid hash lookup, which is feasible but needs to be documented)

**Recommendation**: Resolve this in the plan. Option B avoids duplication but adds build complexity. Option A is simpler but creates a maintenance burden (titles must be updated in two places). State which approach you intend and why.

**Decision**: Implement Option B

---

## 10. No wireframe or mockup

The plan describes a visual redesign entirely in prose. The sidebar grid layout, the landing page structure, the domain sections, the type badges — all described with words and a small HTML snippet. For a change this visual, a reviewer needs to *see* what they're approving.

**Recommendation**: Add an ASCII wireframe or a screenshot mockup for both:

1. The landing page (sidebar + domain-grouped list)
2. A guide page (sidebar + content + TOC)

Even a crude ASCII sketch like the one below communicates more than a paragraph of description:

```
┌──────────────────────────────────────────────────────┐
│  [Search bar]                                        │
├─────────────┬────────────────────────┬───────────────┤
│  SIDEBAR    │  GUIDE CONTENT         │  ON THIS PAGE │
│             │                        │               │
│ ▸ Get Start │  # Configure data...   │  • Section 1  │
│ ▾ Data      │                        │  • Section 2  │
│   • Config  │  Lorem ipsum dolor     │  • Section 3  │
│   • Hibern  │  sit amet...           │               │
│   ★ Panache │                        │               │
│   • MongoDB │                        │               │
│ ▸ Security  │                        │               │
│ ▸ Deploy    │                        │               │
└─────────────┴────────────────────────┴───────────────┘
```

**Decision**: Implement an ASCII wireframe for both.
---

## 11. Step 1 is undersold

Step 1 ("Reclassify Guides") asks someone to make judgment calls about 185 guides' Diataxis types. This is the most contentious step — it requires editorial authority, community consensus on borderline cases, and coordination with extension maintainers. The plan calls it a "prerequisite" and gives it less text than any other step.

**Recommendation**: Expand Step 1 to acknowledge the editorial challenge. Add:

- Who has the authority to reclassify (the docs team? extension maintainers? a review process?)
- What the borderline criteria are (when is something a "howto" vs. a "tutorial"?)
- Whether this can be done incrementally (reclassify one domain at a time) or must be done all at once
- How to handle disagreements

**Decision**: Acknowledge this as an engineering team process. Details will be worked out if the plan is adopted. 

---

## 12. No success criteria or metrics

The plan proposes significant changes but never states how to measure whether they worked. The F2F audience will want to know: what does success look like?

**Recommendation**: Add a short "Success Criteria" section. Possible measures:

- Issue #418 closed or receives positive feedback from @alinnert and @nimo23
- Reduced average number of "Back to Guides" navigations per session (analytics)
- Qualitative feedback from the Quarkus community (Zulip, GitHub Discussions)
- All 268 guides reachable from the sidebar within 2 clicks of the landing page

---

## 13. No risk section

What could go wrong?

- **Build time**: Static sidebar HTML on every guide page adds weight. With 268 guides, each including a sidebar with 268 `<li>` entries, total build output increases. Is this material?
- **Search degradation**: Replacing `<qs-guide>` elements with plain `<li>` links on the landing page may break the search web component's filtering. The plan says it "degrades gracefully" but doesn't verify this.
- **Sidebar length**: 268 guides in 15 collapsible sections. Even collapsed, 15 `<summary>` elements plus the open section's guide list could be long. Has the vertical space been estimated?
- **Content freeze risk**: Steps 1 and 6 (reclassify guides, update titles) touch every `.adoc` file in the main Quarkus repo. This creates merge conflicts with any concurrent guide work.

**Recommendation**: Add a "Risks and Mitigations" section, even a brief one.

---

## 14. Content Gaps section is disconnected from the roadmap

The "Content Gaps" section (lines 536-555) lists 14 missing guides, but the roadmap doesn't reference them. Are these Phase 1 deliverables? Future work? Aspirational? The reader doesn't know what to do with this information.

**Recommendation**: Either connect the gaps to a specific step (e.g., "After Step 6, file GitHub issues for the following content gaps") or explicitly label the section as out-of-scope for this plan: "These gaps are documented for future prioritization and are not part of the Phase 1 deliverables."

**Decision**: explicitly label the section as out-of-scope for this plan: "These gaps are documented for future prioritization and are not part of the Phase 1 deliverables."

---

## 15. Validation Checklist checks the plan against itself

The checklist (lines 559-570) confirms the plan follows its own JTBD principles. This is useful for the author during drafting but doesn't help a reviewer. A reviewer wants to verify claims independently.

**Recommendation**: Reframe the checklist as reviewer-facing verification criteria. For example:

- "Pick any 3 guides at random. Can you locate each one in the domain taxonomy within 10 seconds?"
- "Read any domain's job statement. Does it describe a real task you'd arrive at the docs to accomplish?"
- "Find a guide that spans two domains. Is it clear which domain owns it?"

---

## Summary of Recommendations

| # | Issue | Effort | Impact |
|---|-------|--------|--------|
| 1 | Declare audience; separate proposal from spec | Medium | High |
| 2 | Add executive summary | Low | High |
| 3 | Extract domain tables or add navigation aid | Medium | High |
| 4 | Bridge the two "Why" sections | Low | Medium |
| 5 | Drop or define "Task-Flow Hub" | Low | Low |
| 6 | Remove or relocate v1→v2 changelog | Low | Medium |
| 7 | State cross-reference policy | Low | High |
| 8 | Move Kotlin out of Spring APIs | Low | Low |
| 9 | Resolve sidebar title data model | Low | High |
| 10 | Add wireframe mockup | Medium | High |
| 11 | Expand Step 1 (reclassify) | Low | Medium |
| 12 | Add success criteria | Low | High |
| 13 | Add risk section | Low | Medium |
| 14 | Connect content gaps to roadmap | Low | Medium |
| 15 | Reframe checklist for reviewers | Low | Low |
