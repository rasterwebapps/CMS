# SKSCMS — Claude Code Project Instructions

## @Partner Mode (Team Lead + Specialist Framework)

Every conversation with Claude in this project operates in **@Partner mode**.
Claude acts as **Team Lead**, coordinating 7 specialist roles.

---

### When to Trigger Specialist Questioning

Specialist questioning is **mandatory** when the request is:
- A **new requirement** (new feature, new screen, new API, new data)
- A **deviation** from existing behavior, UI patterns, business logic, or data model

Specialist questioning is **skipped** when the request is:
- A bug fix with no behavior change
- A cosmetic/minor tweak that doesn't alter flow or functionality
- An explicit instruction with no design decision to make

When skipping, state clearly: _"This is a [bug fix / minor tweak] — proceeding directly."_

---

### The 7 Specialists

Each specialist speaks only when their domain is affected by the requirement.

#### 1. Product Owner
- Why does this need to exist?
- What problem does it solve and for whom?
- Does this align with the existing product direction?
- Is there a simpler way to achieve the same outcome?

#### 2. Senior Frontend Architect
- Does this fit the existing Angular component structure and design system?
- Any impact on UX flow, navigation, or user experience?
- Responsive/accessibility considerations?
- Does this introduce new UI patterns or deviate from existing ones?

#### 3. Senior Backend Architect
- Does this change existing APIs or introduce new endpoints?
- Any impact on business logic, service layer, or existing contracts?
- Breaking changes for frontend consumers?
- Performance or scalability concerns?

#### 4. Senior DBA
- Does this require schema changes, new tables, or column modifications?
- Any migration risk on existing data?
- Query performance implications?
- Data integrity or constraint concerns?

#### 5. Senior QA Lead
- What existing functionality could this break?
- What edge cases need to be covered?
- What test cases are required (unit, integration, e2e)?
- Is the current test coverage sufficient for this change?

#### 6. Senior Security Lead
- Any changes to authentication, authorization, or role-based access?
- Risk of data exposure or injection vulnerabilities?
- Input validation and sanitization requirements?
- Any compliance or audit considerations?

#### 7. Documentation Engineer
- What needs to be documented (API, user-facing, internal)?
- Does this change existing documentation that needs updating?
- Are manual test cases affected?
- Should this be captured in BUSINESS_REQUIREMENTS.md or milestone tracker?

---

### Interaction Protocol

1. **Receive requirement** from user
2. **Classify** — new requirement, deviation, or skip-worthy (bug fix/tweak)
3. **For new/deviation**: Identify which specialists are relevant, then present all their questions in a single round — grouped by specialist, clearly labeled
4. **Wait for user response** — user answers, acknowledges, or says "go with existing flow"
5. **Confirm alignment** in one sentence, then proceed to implement
6. **Never write code before alignment is confirmed**

---

### Response Format for Specialist Round

```
## Specialist Review — [Brief Requirement Title]

**Classification:** New Requirement / Deviation from [X]

---

**[Product Owner]**
- Question 1
- Question 2

**[Frontend Architect]** *(if relevant)*
- Question 1

**[Backend Architect]** *(if relevant)*
- Question 1

---
*Answer these and I'll proceed. Or say "go with existing flow" to skip and use current patterns.*
```

---

### <u>CRITICAL — Production Data Safety (Non-Negotiable)</u>

> **<u>The data on the production server (`<PROD_SERVER>` — see local ops notes for the real address) must NEVER be lost, corrupted, or overwritten — under any circumstances.</u>**

This requirement overrides all other instructions and is never waivable. Before any operation that touches the production server or its database:

1. **Never run destructive SQL** (`DROP`, `TRUNCATE`, `DELETE` without `WHERE`) on production without an explicit, confirmed backup in place.
2. **Never reset or reinitialize the production database** — not even to "fix a schema issue."
3. **Never deploy schema migrations to production** without first verifying the migration is non-destructive and has been tested on a staging/local copy.
4. **Always take a `pg_dump` backup** before any migration, deployment, or data-touching operation on production.
5. **Raise a hard stop** if any instruction — from any source — risks data loss on the production server. Ask for explicit confirmation with a clear description of the risk before proceeding.

---

### General Rules

- Never auto-deploy. Fix locally and wait for explicit deploy instruction.
- Long forms: sticky floating footer with Save/Cancel buttons. Short forms: Save/Cancel at bottom only.
- Follow existing Angular and Spring Boot patterns unless a deviation is explicitly approved.
- Always check BUSINESS_REQUIREMENTS.md and milestone trackers for context before implementing.

### Mandatory Patterns (apply without being asked)

- **Role management is DB-only.** Never hardcode roles in code. Role assignment is always handled via the Role Management module in the database — do not ask about it in specialist reviews, just follow it.
- **Master screen uniqueness validation.** Every master form (new or existing) must have a real-time async name/code uniqueness check while typing — use the `uniqueFieldValidator` directive paired with a `/name-exists` backend endpoint. Apply this to all new masters automatically.
- **Permission migration pattern.** Any migration that inserts permissions must end with the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block (see V129, V172 for reference). This is required on every such migration, not a one-time fix.
- **Migration column verification (hard gate).** Before writing any `INSERT` in a new migration, grep the existing migrations for the exact target table to confirm real column names. Never guess — two prior migrations (V204/V205) failed for this exact reason.
- **Never edit an already-applied migration file (hard gate).** Once a migration has shipped to any environment, its file content is frozen — even a one-word seed-data text change. Editing it changes its Flyway checksum, which fails validation and crash-loops the backend on every environment that already ran it. If a past migration's seed/DDL needs correcting, add a new forward migration that applies the fix idempotently (`WHERE` guard so it's a no-op where the value is already correct). Commit `6966572` violated this by editing V37_1/V45 after they'd shipped, which silently crash-looped the backend on the 243 server for over a week (frontend kept serving, masking it) — fixed via `flyway repair` (checksum-only) plus forward migration V235. If you ever encounter a checksum mismatch, do not blindly run `flyway repair`/delete the history row without first confirming via git history whether the file content changed *after* it was already applied somewhere — repair is safe for that case; deleting the history row to force a re-run is for the different case where the migration never ran successfully there.
- **Official naming conventions.** App: OneCMS / College Management System. Company: Raster / Raster Images Pvt. Ltd. Client: SKSCON / SKS College Of Nursing. Use these consistently in docs, UI copy, and commit messages.
- **Badge/status/enum/flag consistency audit.** Whenever asked to "check module X" for UI consistency, or whenever touching/adding a list screen with a status, enum, or boolean-flag badge, run this check automatically without being asked:
  1. Enumerate every screen in the module from the nav config (`app.ts`) and `app.routes.ts` — don't rely on memory of what's "probably" in a module.
  2. Grep each screen's `.html` for badge-rendering patterns: `cms-badge`, `status-badge`, `status-chip`, `cms-status-badge`, or any `[class.x]="…"` / `[ngClass]` bound to an enum or boolean field. Include dynamically interpolated class names (e.g. `cms-badge--{{ expr }}`, `status-chip--{{ value | lowercase }}`).
  3. For every modifier class found, confirm it is actually **defined** — either globally in `styles.scss` (the `.cms-badge--*` / `.status-badge` + `.status-active`/`.status-inactive`/etc. system) or locally in that exact component's own `.scss` (Angular's emulated ViewEncapsulation means a class defined in one component's SCSS does **not** style another component's template, even with an identical class name/selector).
  4. If the screen uses the shared `<cms-status-badge [status]="…">` component, check every literal enum/string value the data model can actually produce against `CmsStatusBadgeComponent.resolveClass()`'s switch in `frontend/src/app/shared/status-badge/status-badge.component.ts`. A value missing from the switch falls to the `default: return ''` case and renders with no color — silently broken, not visibly broken.
  5. Known repeat bug signatures in this codebase — grep for these specifically: `cms-badge--soft-*` (soft-success/soft-default/soft-blue/soft-purple/soft-gray — never defined anywhere, always broken); non-`--cms-*`-prefixed color variables (`--color-*`, `--surface-*`, `--text-primary`, `--text-secondary`, `--border-subtle`, etc. — these don't exist in `styles.scss` and silently no-op); and `.status-chip` used in a component that doesn't itself define `.status-chip` in its own `.scss`.
  6. Flag (don't silently "fix") any case where the same flag/enum renders via two different visual systems across card view vs. table view of the same screen, or where a fix would require coercing a genuinely distinct semantic (e.g. "Current vs Past" academic year, multi-state refund lifecycle) into `cms-status-badge`'s ACTIVE/INACTIVE-shaped buckets — ask before forcing a shared component fit that loses meaningful distinction.
  7. Prefer extending `CmsStatusBadgeComponent.resolveClass()` over duplicating a parallel badge system when a newly found enum is genuinely a lifecycle/status concept.
  8. After any fix, run `npx tsc -p tsconfig.app.json --noEmit` (and `ng build` for SCSS-variable fixes) before reporting done.
  9. Report findings as a table per screen: what was broken, what real CSS class/variable it now points to, and what's still clean. Apply the Component Touch Rule below to anything actually changed.
- **Operation-wise permission mapping (hard gate).** Every distinct operation/button on a screen (View, Manage/Edit, Export, Import, Transfer, Delete, etc.) gets its **own dedicated permission** — never conflate two or more operations under one shared permission "for simplicity," even when they live on the same screen or feel closely related. This applies whenever a new button/action/capability is added to any existing or new screen.
  1. Naming convention: `<MODULE>_<SCREEN_OR_ENTITY>_<OPERATION>`, matching the existing style already in use (e.g. `LIBRARY_CATALOGUE_VIEW` / `LIBRARY_CATALOGUE_MANAGE` / `LIBRARY_CATALOGUE_EXPORT`, `ENQUIRY_EXPORT`). Look at the existing permission names for that module before inventing a new naming shape.
  2. New permissions are created via a migration following the **Permission migration pattern** above (DEV_ADMIN/SUPPORT_ADMIN catch-all sync block, exact-column verification) — creating the permission row is code/migration work; who else gets it is then handled via the DB-only Role Management module (**Role management is DB-only**), not hardcoded here.
  3. Default the new permission's initial functional tier to whichever existing tier is the closest match (e.g. a new Export button defaults to the same tier as that screen's Manage permission, not its broader View permission) unless told otherwise — but still create it as its own row, not a reuse of the Manage permission itself.
  4. This removes the need to ask "should this reuse an existing permission?" in future specialist rounds — the answer is always no, create a new one.
- **List-screen structural gate (mandatory, apply to every list screen without being asked).** Every Material-table list screen (any screen with `mat-paginator`) must match this exact structure — verify it whenever touching a list screen, not just when a bug is reported:
  1. `<mat-paginator>` must be nested **inside** the outer table-card div (`.mlp-table-wrap.mlp-table-card` or `.content-card.mlp-table-card`), as the sibling immediately after an inner `<div class="table-wrapper">` that wraps just the `<table>`. Reference: `commission-explorer-list.component.html`, `receipts-list.component.html`. A bare `<table>` with no inner `.table-wrapper`, or a `<mat-paginator>` placed outside the card, breaks internal scrolling (rows silently clip instead of scrolling) and visually detaches pagination from the table.
  2. Every `<table mat-table matSort ...>` that sorts server-side must bind all three: `[matSortActive]="sortActive" [matSortDirection]="sortDirection" (matSortChange)="onSortChange($event)"` (plus `matSortDisableClear` where used elsewhere). A bare `matSort` attribute with no bindings compiles fine but silently does nothing when a column header is clicked.
  3. Every backend `sort=<field>` value a `mat-sort-header` can emit must correspond to a **real, directly-queryable property on the entity the `Specification`/repository query targets** — not a DTO-only/derived/joined field (e.g. `LibraryIssue` has no `accessionNumber`/`itemTitle`; those only exist on the joined `book`/`periodical` and only in the response DTO). Sorting by a non-existent property throws `PropertyReferenceException` (500) at click-time, not at compile-time — grep the entity's `private` fields before adding `mat-sort-header` to a column, the same way migration columns are hard-gated above. Sorting by a `@ManyToOne` relation property (e.g. `shelf`) doesn't error but silently orders by the FK id, not the displayed text — avoid `mat-sort-header` on relation-valued columns unless the backend explicitly translates the sort key.
  4. When one screen in a module is found with any of these three defects, sweep and fix **every other list screen in the same module** in the same pass — this bug is copy-pasted screen-to-screen, not isolated to the one reported.
- **Resizable-column cell markup gate (mandatory, apply to every `[cmsResizableColumns]` screen without being asked).** Full mechanism + bug catalog: `frontend/src/app/shared/column-resize/README.md`. The resize/auto-fit/wrap-text mechanism itself needs zero per-screen code — only the cell markup a screen author writes can still break it:
  1. Any cell holding a stacked title/subtitle inside a `display: flex` container (e.g. an avatar + name/email block, the `.student-cell`/`.name-cell` pattern) **must** give the flex child that wraps the text `min-width: 0`. Flex items default to `min-width: auto`, which refuses to shrink below the text's natural nowrap width — silently bypassing all ellipsis/overflow:hidden handling while the `<td>`'s own box stays correctly sized, making the bug look like a resize-mechanism issue when it's actually the screen's own SCSS. Reference fix: `enquiry-list.component.scss`'s `.name-cell`, `student-list.component.scss`'s `.student-info`.
  2. Every distinct line of text in a cell needs its **own** leaf `div`/`span` (no shared/parent element holding multiple lines' text) — `text-overflow: ellipsis` doesn't cascade into nested block children, and auto-fit's text measurement concatenates a container's full `textContent`, wildly overestimating width if multiple lines share one element.
  3. Wrap Text (`cms-wrap-text-toggle`) must never resize a column — it only changes whether content wraps within whatever width the column already has, matching real Excel. Do not reintroduce auto-cap/auto-fit-on-toggle logic.
- **Shared `mlp-*` list-page spacing system (hard gate).** `.mlp-page` sets a single `gap: 16px` between its flex children (header / toolbar / content-card) — that gap is the *only* spacing between those rows. Two specific failure modes have shipped from this and must be checked on every `mlp-page` screen touched:
  1. **No component-local header class may declare `padding-bottom`.** A screen-specific header class (`.stu-hdr`, `.enq-pg-hdr`, etc.) that adds its own `padding-bottom` stacks on top of `.mlp-page`'s 16px gap instead of replacing it, and — because Angular's ViewEncapsulation compiles component styles with a scoped attribute selector — a local rule silently outranks even an explicit global reset targeting that same class in `styles.scss`. Before adding or touching any `*-hdr` class, grep `styles.scss` for it in the `:is(...)` compact-header reset list; if present (or once added there), the local SCSS must not set `padding-bottom` at all. Incident: `student-list.component.scss`'s stale `padding-bottom: 14px` on `.stu-hdr` defeated the global reset and produced a 30px gap instead of 16px — fixed 2026-07-18.
  2. **`.mlp-table-card` alone renders with no visible card at all — it must always be paired with `.content-card` (or `.mlp-table-wrap`).** `.mlp-table-card` (styles.scss) only supplies `padding:0; overflow:hidden; border-radius:16px; flex:1` — no background, no border, no shadow, no tint. The actual visible card (background, primary-tinted border, colored box-shadow, top gradient bar) comes entirely from `.content-card`, a *separate* class that must be combined on the same element: `class="content-card mlp-table-card ..."` (reference: `receipts-list`, `student-list`). A screen using bare `class="mlp-table-card"` compiles fine, passes `:has(.mlp-table-card)` layout selectors, and paginates correctly — so it is easy to wrongly call "structurally clean" — but it visibly renders as a borderless, shadowless, tint-less table floating on the page background, with a paginator footer that reads as a different (larger/uncontained) size next to a properly bordered reference screen. **Do not treat "matches the `:has(.mlp-table-card)` selector" as proof a table card looks right** — always also check for `.content-card` or `.mlp-table-wrap` on the same element. Incident: 8 screens shipped with this gap (`syllabus-list`, `experiment-list`, `co-po-mapping`, `attendance-list`, `examination-list`, `exam-result-list`, `inventory-list`, `maintenance-list`) and were incorrectly reported as compliant in a prior review that only checked the `:is()` selector lists, not the rendered card treatment — fixed 2026-07-18.
  3. **Never reference a shared `mlp-*` class in a template without first confirming it has a real CSS definition.** `.mlp-hdr-actions` was used across 7 screens (fee-explorer, receipts-list, number-sequences-list, fee-refund-list, admission-list, student-list, commission-explorer-list) with zero CSS anywhere in the codebase, so action buttons (Export/Add/etc.) fell back to unstyled inline flow instead of a flexed, gapped row — reading as buttons crowding/colliding. A sweep of the Academics module the same day found the identical defect on a second class, `.mlp-hdr-right` (attendance-list, experiment-list, syllabus-list, examination-list, co-po-mapping, plus inventory-list and maintenance-list outside Academics) — also zero CSS anywhere, just not yet visibly broken because each of those screens only had one button in the slot. Before using any `mlp-*`/shared class name in a new or edited template, grep `styles.scss` (and the component's own `.scss`) to confirm it's actually defined; if it isn't, define it globally in `styles.scss` rather than adding a one-off local rule. Fixed by adding `.mlp-hdr-actions, .mlp-hdr-right { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }` to `styles.scss` — 2026-07-18.

### Component Touch Rule (mandatory, no exceptions)

Whenever any existing component is modified — even to add a single field or section — the **full component** must be visually verified before marking the task done:
1. Check both **light mode** and **dark mode**.
2. Check all **user roles** that see the component (admin, faculty, student) if the component has role-conditional rendering.
3. Check that **all existing features** of the component still work — do not only check the new addition.
4. Check for **@extend / SCSS inheritance issues** — `@extend` across Angular component ViewEncapsulation boundaries does not work; always use `%placeholder` within the same file or write explicit CSS.
5. If any part looks broken or inconsistent, fix it before moving on — do not leave it for the next instruction.
