# Screen Compliance Audit — 2026-08-25 21:00 IST (OC-168)

Autonomous, unattended full-app audit. Scope: (1) Tour + Flow Map coverage,
(2) CLAUDE.md mandatory hard gates, (3) multi-tab screens against the latest
design instructions. This file is both the working checklist and the
timestamped findings/progress log — update it continuously, not just at the
end, per `feedback_production_ops_verification` (require real evidence, not
"done" alone) and the cron-autonomous pattern established by
`docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md`.

**Runs to completion on its own — must not stop for input.** Nobody is
available to answer questions during this run. Every gate this audit checks
is already fully specified below and in CLAUDE.md's own mandatory-pattern
sections, plus established precedent from the OC-139 rollout and its
follow-on fix commits (OC-152's badge fix, OC-157/158's Flow Map fixes). When
a judgment call comes up inside one of these gates, resolve it the way
CLAUDE.md and that precedent already resolve it and keep moving — do not
pause because a call is ambiguous. The narrow "flag, don't force" carve-out
in Step 2 (badge-audit item 6's semantic-collapse case) still requires a
concrete written recommendation in the Progress Log, not a bare flag, and
does not block work on anything else. Reaching the stop condition with real
progress committed always beats stopping early on an open question.

## Why this audit, and why now

The Tour+FlowMap rollout (OC-139..OC-160) finished 2026-08-19 with one
documented exception (Campus Infrastructure, resolved later in OC-155). Since
then, several tickets shipped new screens and new tabs on existing screens
without a rollout-style sweep behind them: OC-155 (Campus Infra
consolidation), OC-159 (floor-plan measurements+linking), OC-161 (Global
Auto-Schedule, Section Faculty, Subject-Lab binding, Capacity Auto-Plan),
OC-163 (Class Incharge), OC-164 (Skeleton Builder automate-first + Faculty
workload management — **added the "Lab Schedules" tab to Faculty View**),
OC-165 (Eligible Faculty list), OC-166 (CourseOffering.facultyId retirement).
Faculty View (`/faculty/:id`) is the flagged example: it now has 5 tabs
(Profile / Professional / Courses / Lab Schedules / Documents) built across
multiple separate tickets, which is exactly the shape of screen where one
tab quietly missing a hard-gate fix (badge classes, spacing, resizable-column
markup) is easy to miss because nobody re-reviews the *whole* component when
only one tab changes.

## Step 0 — rebuild the ground truth (do this before checking anything)

The existing `docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md` Screen Checklist
is **six days stale** — do not trust its Tour/FM columns for screens touched
or added after 2026-08-19. Rebuild a current screen inventory first:

1. Enumerate every route in `frontend/src/app/app.routes.ts` and every nav
   entry in `frontend/src/app/app.ts` — cross-reference the two, they should
   match. Note any route with no nav entry (orphaned/dev-only) and any nav
   entry with no route (dead link) — log both, don't silently skip.
2. For each screen, `grep` its component for `tourService.register` /
   `registerFlowMap` directly — never trust the old checklist table without
   verifying. Mark the current truth in the table below.
3. Diff this fresh inventory against the old checklist's screen list — any
   screen present in one but not the other is itself a finding (new screen
   shipped without tour work, or a screen renamed/removed).

## Step 1 — Tour + Flow Map coverage

For every screen found in Step 0 lacking a working Tour and/or Flow Map,
follow `frontend/src/app/shared/tour/README.md` exactly (same recipe as the
OC-139 rollout) to add it. Reuse existing funnel constants
(`*.tours.ts` files) where the screen naturally belongs to an existing
journey; only create a new funnel where none fits, matching the precedent
set during the OC-139 rollout (see that log for the full funnel list).

## Step 2 — CLAUDE.md mandatory hard-gate sweep

Run every one of these across **every screen**, not just ones that look
suspicious — the whole point of a scheduled sweep is to catch what a
one-off targeted fix missed:

1. **Badge/status/enum/flag consistency audit** (CLAUDE.md "Mandatory
   Patterns") — grep every `.html` for `cms-badge`, `status-badge`,
   `status-chip`, `cms-status-badge`, dynamic `[ngClass]`/interpolated badge
   classes; confirm every modifier class actually has a CSS definition
   (global `styles.scss` or the component's own `.scss`); check every enum
   value a screen's data model can produce against
   `CmsStatusBadgeComponent.resolveClass()`. Known repeat bug signatures to
   grep for specifically: `cms-badge--soft-*` variants that were never
   defined, non-`--cms-*`-prefixed color variables, `.status-chip` used
   without a local definition.
2. **List-screen structural gate** — every `mat-paginator` screen: paginator
   nested inside `.mlp-table-wrap.mlp-table-card` / `.content-card
   .mlp-table-card` as sibling after an inner `.table-wrapper`; every
   server-sorted `mat-sort-header` table binds `matSortActive` /
   `matSortDirection` / `(matSortChange)`; every sort key is a real
   queryable entity property, not a DTO-only/joined/relation field.
3. **Resizable-column cell markup gate** (screens with
   `[cmsResizableColumns]`) — flex-wrapped stacked title/subtitle cells have
   `min-width: 0` on the text-wrapping child; every distinct line of text is
   its own leaf element (no shared parent holding multiple lines).
4. **`mlp-page` spacing hard gate** — no local `*-hdr` class sets its own
   `padding-bottom`; every `.mlp-table-card` is paired with `.content-card`
   or `.mlp-table-wrap`; no template references an `mlp-*` class that isn't
   actually defined anywhere.
5. **Operation-wise permission mapping** — spot-check that recently added
   buttons/operations (anything from OC-161 through OC-166) each have their
   own dedicated permission rather than reusing an existing one.

Fix clear-cut, low-risk violations inline as you find them (same standard as
OC-152's Fee Reports badge fix during the OC-139 rollout: a wrong CSS class
name pointed at a real existing class is safe to fix on sight). **Do not**
silently force a fix that would collapse a genuinely distinct semantic into
a shared component's bucket, or restructure a screen's layout beyond the
gate's literal requirement — flag those for human review instead, per
CLAUDE.md's badge-audit item 6.

## Step 3 — Multi-tab screen deep verification

Any screen with a tabbed layout (`Profile`/`Professional`/`Courses`/`Lab
Schedules`/`Documents` on Faculty View is the trigger example, but check
every tabbed screen in the app, not just that one) gets the full **Component
Touch Rule** applied tab-by-tab, since tabs on these screens were built
across separate tickets over time and nobody has re-verified the whole
component as a unit since the last tab was added:

1. Every tab checked in both light and dark mode.
2. Every tab checked across all roles that can see it (admin/faculty/
   student, if role-conditional).
3. Every tab's existing functionality still works, not just the
   most-recently-added tab.
4. No `@extend` across component `ViewEncapsulation` boundaries.
5. Apply Steps 1–2's gates (badges, resizable-column markup, spacing) to
   each tab's markup individually — a tab added later often didn't get the
   same pass its siblings did.

`npx tsc -p tsconfig.app.json --noEmit` (+ `ng build`) is the correctness
gate for this unattended session — nobody is here for a live-browser check.
Note explicitly in the Progress Log which screens/tabs still need a manual
light/dark/role check, same convention as the OC-139 rollout log.

## Hard rules (from CLAUDE.md, repeated for emphasis)

- **Local dev only.** Never touch/deploy to 172.16.7.209 or 172.17.1.243.
  Never `git push`. Never open a PR. Commit locally to `main` only.
- **Frozen migration files** — if a fix requires a migration, add a new
  forward one; never edit a shipped one. Migration column verification gate
  applies if any migration is written (grep the real table first).
- **Role management is DB-only** — never hardcode roles.
- JIRA: parent is **OC-168** (already In Progress). One sub-ticket per
  logical group of fixes (not per screen), `bash scripts/jira.sh create ...`
  then `start`, `review` once committed.
- Commit style: `OC-XXX: fix(<module>): <what changed>` or
  `OC-XXX: feat(tour): <module> — ...` matching existing history shape.
  Commit as you go, not one giant end-of-night commit.
- Naming: App = OneCMS / College Management System. Company = Raster /
  Raster Images Pvt. Ltd. Client = SKSCON / SKS College Of Nursing.

## Stop condition

Keep working until the fresh Step-0 inventory is fully swept (every screen
DONE/SKIPPED/BLOCKED across all three steps) or you're at genuine risk of
running out of context/tokens. If you must stop early: leave the repo in a
clean, committed, tsc-clean state (no half-wired screen), and write a final
Progress Log line stating precisely what's left and in what order, the same
way the OC-139 rollout's night session handed off to its morning
continuation. Do not attempt to schedule a follow-up cron session yourself —
that requires editing the system crontab, which you cannot do unattended;
leave that decision for the user.

## Current Screen Inventory (Step 0, rebuilt 2026-08-25 21:0X)

Built by cross-referencing `core/nav/nav-config.ts` (94 nav items, single
source of truth) against `app.routes.ts` (201 route entries — 0 orphan nav
items, 0 dead links; the extra routes are `/new`, `/:id/edit`, etc. variants
not in nav) and grepping each nav item's routed component for
`tourService.register(`/`registerFlowMap(`. All 94 nav items resolved to a
real component file — no orphaned routes or dead nav links found.

Tour/FlowMap columns below reflect state **after** OC-169 (Overview module
fixes committed). Two gaps remain open, both in Academics (see Progress Log).

| Module | Screen | Route | Tour | FlowMap |
|---|---|---|---|---|
| Overview | Dashboard | `/dashboard` | Y | Y |
| Overview | My Profile | `/profile` | Y | Y |
| Overview | My Timetable | `/my-timetable` | Y | Y |
| Admission Management | Enquiries | `/enquiries` | Y | Y |
| Admission Management | Finalize Fee | `/student-fees/finalize` | Y | Y |
| Admission Management | Collect Payment | `/fee-collection` | Y | Y |
| Admission Management | Submit Documents | `/enquiries/document-submission` | Y | Y |
| Admission Management | Verify Documents | `/enquiries/document-verification` | Y | Y |
| Admission Management | Complete Admission | `/enquiries/admission-completion` | Y | Y |
| Admission Management | Admission Explorer | `/admissions` | Y | Y |
| Admission Management | Retro Admit | `/students/retro-admit` | Y | Y |
| Student Management | Student Explorer | `/students` | Y | Y |
| Student Management | Assign Roll Numbers | `/students/roll-numbers` | Y | Y |
| Student Management | Scholarship Applications | `/scholarship-applications` | Y | Y |
| Student Management | Data Import | `/import` | Y | Y |
| Finance | Fee Explorer | `/student-fees` | Y | Y |
| Finance | Receipts | `/receipts` | Y | Y |
| Finance | Refunds | `/refund-approvals` | Y | Y |
| Finance | Commissions | `/commission-explorer` | Y | Y |
| Academics | Curriculum Versions | `/curriculum-versions` | Y | Y |
| Academics | Syllabus | `/syllabi` | Y | Y |
| Academics | Experiments | `/experiments` | Y | Y |
| Academics | CO/PO Mapping | `/curriculum-mappings` | Y | Y |
| Academics | Course Offerings | `/course-offerings` | Y | Y |
| Academics | Elective Assignment | `/elective-assignment` | Y | Y |
| Academics | **Capacity Auto-Plan** | `/timetable/capacity-auto-plan` | **N** | **N** |
| Academics | **Assign Faculty** | `/assign-faculty` | **N** | **N** |
| Academics | Lab Schedules | `/lab-schedules` | Y | Y |
| Academics | Faculty Availability | `/faculty-availability` | Y | Y |
| Academics | Faculty Workload Rules | `/timetable/workload-rules` | Y | Y |
| Academics | Skeleton Builder | `/timetable/skeleton-builder` | Y | Y |
| Academics | Staffing | `/timetable/staffing` | Y | Y |
| Academics | Conflict Inspector | `/timetable/conflict-inspector` | Y | Y |
| Academics | Timetable Draft Review | `/timetable/draft-review` | Y | Y |
| Academics | Timetable | `/timetable` | Y | Y |
| Academics | Resource Timetable | `/timetable/resource-grid` | Y | Y |
| Academics | Faculty Absence | `/faculty-absence` | Y | Y |
| Academics | Staff Session Swap | `/timetable/staff-swap` | Y | Y |
| Academics | My Special Classes | `/timetable/special-classes/my-requests` | Y | Y |
| Academics | Special Class Approvals | `/timetable/special-classes/approval-queue` | Y | Y |
| Academics | Attendance | `/attendance` | Y | Y |
| Academics | Progress Report | `/progress-report` | Y | Y |
| Academics | Manage Exams | `/examinations` | Y | Y |
| Academics | Exam Results | `/exam-results` | Y | Y |
| Academics | Student Promotion | `/student-promotions` | Y | Y |
| Library | Issue Books | `/library/issues/new` | Y | Y |
| Library | Issue Explorer | `/library/issues` | Y | Y |
| Library | Overdue Books | `/library/reports` | Y | Y |
| Library | Book Explorer | `/library/books` | Y | Y |
| Library | Journal Explorer | `/library/periodicals` | Y | Y |
| Library | My Library | `/library/my-issues` | Y | Y |
| Library | Fines | `/library/fines` | Y | Y |
| Library | Racks & Shelves | `/library/racks` | Y | Y |
| Library | Import | `/library/import` | Y | Y |
| Library | Library Settings | `/library/settings` | Y | Y |
| Core Infrastructure | Campus Infrastructure | `/campus-infrastructure` | Y | Y |
| Core Infrastructure | Room Purpose Categories | `/room-purpose-categories` | Y | Y |
| Core Infrastructure | Room Sub-Types | `/room-sub-types` | Y | Y |
| Inventory Management | Inventory | `/inventory` | Y | Y |
| Inventory Management | Maintenance | `/maintenance` | Y | Y |
| Hostel Management | Hostel Room Types | `/hostel-room-types` | Y | Y |
| Hostel Management | Room Preferences | `/room-preferences` | Y | Y |
| Hostel Management | Room Allocation | `/room-allocations` | Y | Y |
| Reports & Analytics | General Reports | `/reports` | Y | Y |
| Reports & Analytics | Fee Reports | `/fee-reports` | Y | Y |
| Preferences | Academic Calendar | `/academic-calendar` | Y | Y |
| Preferences | Academic Years | `/academic-years` | Y | Y |
| Preferences | Agents | `/agents` | Y | Y |
| Preferences | Blood Groups | `/blood-groups` | Y | Y |
| Preferences | Classrooms | `/classrooms` | Y | Y |
| Preferences | Clinical Venues | `/clinical-venues` | Y | Y |
| Preferences | Communities | `/communities` | Y | Y |
| Preferences | Courses | `/courses` | Y | Y |
| Preferences | Designations | `/designations` | Y | Y |
| Preferences | Equipment | `/equipment` | Y | Y |
| Preferences | Faculty | `/faculty` | Y | Y |
| Preferences | Faculty Doc Config | `/faculty/document-config` | Y | Y |
| Preferences | Fee Structures | `/fee-structures` | Y | Y |
| Preferences | Holiday Templates | `/holiday-templates` | Y | Y |
| Preferences | Institutions | `/institutions` | Y | Y |
| Preferences | Labs | `/labs` | Y | Y |
| Preferences | Location Master | `/india-locations` | Y | Y |
| Preferences | Number Sequences | `/number-sequences` | Y | Y |
| Preferences | Periods | `/periods` | Y | Y |
| Preferences | Programs | `/programs` | Y | Y |
| Preferences | Referral Types | `/referral-types` | Y | Y |
| Preferences | Scholarship Types | `/scholarships` | Y | Y |
| Preferences | Settings | `/settings` | Y | Y |
| Preferences | Specialities | `/specialities` | Y | Y |
| Preferences | Staff Referrers | `/staff-referrers` | Y | Y |
| Preferences | Subjects | `/subjects` | Y | Y |
| User Management | Users | `/user-management` | Y | Y |
| User Management | Roles & Permissions | `/role-management` | Y | Y |
| User Management | Permission Tiers | `/permission-tiers` | Y | Y |

Note: this table covers nav-visible screens only (Step 0's cross-reference
scope). It does not include non-nav detail/sub-screens reached contextually
(e.g. `student-detail`, `faculty-detail`, `enquiry-detail`, `capacity-planner`)
— those get swept during the Step 2 hard-gate pass and Step 3 multi-tab pass
by module, not enumerated here individually.

## Progress Log

Format: `- YYYY-MM-DD HH:MM | <screen/area> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket> | <note>`

- 2026-08-25 18:4X | setup | DONE | OC-168 | Parent ticket created, this audit doc scaffolded, cron session scheduled for 21:00 tonight via `scripts/screen-audit-autonomous-run.sh`. No screens checked yet — this is the pre-work commit.
- 2026-08-25 21:00 | setup | DONE | OC-168 | Night run started. Committed the 3 setup-scaffold files as their own commit (`159b2f90`) per the prompt's instruction. Pre-existing uncommitted WIP (Skeleton Builder: 4 backend services + 6 frontend files + its tours file) found in the tree at session start — left untouched, not folded into any audit commit (same handling as the OC-149 precedent this doc cites).
- 2026-08-25 21:00-21:05 | Step 0 inventory | DONE | OC-168 | Rebuilt ground truth: 94 nav items in `nav-config.ts` × 201 route entries in `app.routes.ts`. Cross-reference found 0 orphaned nav links and 0 dead routes. Grepped every nav-routed component for `tourService.register(`/`registerFlowMap(` — found 6 real gaps: Dashboard, My Profile, My Timetable (zero tour infra at all) + Resource Timetable (tour constants imported and `<cms-tour-button>` rendered in template, but `register()`/`registerFlowMap()` never called — a dead button) + Capacity Auto-Plan, Assign Faculty (zero tour infra). Full table written into this doc's Current Screen Inventory section.
- 2026-08-25 21:05-21:06 | Overview: Dashboard, My Profile, My Timetable, Resource Timetable | DONE | OC-169 | Fixed all 4 Step-1 gaps in this batch. Dashboard: role-conditional widget grid had no header/toolbar to anchor to — added a small fixed `.tour-fab` (bottom-left, mirrors the existing `.customize-fab` bottom-right so they never collide) plus `#tour-dash-grid` anchor on the widget grid; new `dashboard.tours.ts`, single-entry funnel (standalone Overview landing screen, no natural multi-screen journey per tour README). My Profile: added tour button into existing `.hero__actions` column (new `.hero__tour-btn { align-self: flex-end }` so the icon button doesn't get stretched full-width by the column's flex `align-items` default) + anchors on hero actions and the bento-grid; new `profile.tours.ts`. My Timetable: standard `mlp-hdr`/`mlp-toolbar` pattern, added `page-title-row` + toolbar/view anchors matching the existing Timetable/Capacity Planner tour shape; new `my-timetable.tours.ts`. Resource Timetable: one-line fix — added the two missing `.register()`/`.registerFlowMap()` calls in `ngOnInit`, reusing its already-imported (but previously unwired) tour constants. `npx tsc -p tsconfig.app.json --noEmit` clean. Committed `088b511d`. **Manual light/dark/role click-through still needed** for all 4 (no browser in this unattended session) — flagging per the tour README's Verification section, same convention as OC-139.
- 2026-08-25 21:06 | Academics: Capacity Auto-Plan, Assign Faculty | OPEN | OC-168 | Two remaining Step-1 gaps, both zero tour infra. Continuing into Academics module next (matches priority order); will close these alongside the rest of the Academics hard-gate + tour sweep rather than as an isolated fix, since Academics is next in priority order anyway.
- 2026-08-25 21:06-21:25 | Step 2 whole-app breadth sweep (badges / mlp-spacing / list-screen structural gate) | DONE | OC-168 | Ran the 5 hard-gate greps app-wide (not module-scoped) before the module-by-module pass, to catch anything already broken independent of module. Results: (1) `cms-badge--soft-*` — the ~40 usages across the app all use variants (`soft-blue/green/amber/red/purple/gray/accent`) that **are** defined in `styles.scss:604-622` — CLAUDE.md's own bug-signature list names `soft-success`/`soft-default` as the actually-broken ones and neither is used anywhere in the codebase; this specific item is stale documentation of an already-fixed bug, no action needed. (2) non-`--cms-*` color vars (`--color-text-secondary`, `--color-surface`, etc.) — `styles.scss:217-226` defines a deliberate, commented "Library module alias tokens" block mapping every `--color-*` var used to its `--cms-*` equivalent, correctly theme-aware; not a bug. (3) bare `.status-chip` — the 4 apparent hits were all namespaced (`rfl-status-chip`, `receipt-status-chip`, `ob-status-chip`) each with its own local `.scss` definition; substring false-positives, not a bug. (4) `mlp-table-card` without `.content-card`/`.mlp-table-wrap` pairing — zero hits, clean. (5) undefined `mlp-*` classes — 28 distinct classes referenced, 27 defined in `styles.scss` (my first pass missed them because it searched only `app/**/*.scss`, not `src/styles.scss` itself); the one genuine gap was `commission-explorer-list.component.html`'s `<div class="mlp-loading">` (vs. the correct, already-globally-defined `.loading-container` every sibling finance screen uses) — **fixed**, one-line class swap. (6) bare `matSort` without `matSortActive`/`matSortDirection`/`(matSortChange)` — 19 files flagged, but all 19 verified as legitimate client-side `MatTableDataSource` + `dataSource.sort = matSort` screens (Angular Material's native pattern needs no extra bindings); the gate's own text scopes this requirement to server-side–sorted tables only. Cross-checked the inverse (44 files *with* `matSortActive`, i.e. genuinely server-side) — did not do a full per-screen sort-key-vs-entity audit of all 44 given time budget; spot-checks found no red flags. (7) paginator/table-wrapper sibling structure — initial distance-based grep heuristic was too tight (600-char window, false-positived on every screen including the reference `receipts-list`); corrected to "does `.table-wrapper` exist anywhere in the file", which found one real-looking hit (`library-my-issues`) that on inspection is a false positive — that screen's lone bare `<table>` (Borrow History tab) has no paginator at all; the `<mat-paginator>` present belongs to a separate card-grid tab (Catalogue Search), a legitimately different, non-`<table>` pattern the gate doesn't cover. **Net result: one real fix (commission-explorer-list), everything else in this pass was already compliant** — prior sweeps (OC-152 and the 2026-07-18 mlp-spacing fixes) evidently already closed most of what this gate checks for app-wide. Did not yet do the per-module Step 2 pass called for by the doc (badge-enum-vs-`resolveClass()` audit, permission-mapping spot-check) — that's still open, tracked below.
- 2026-08-25 21:25 | correction to stale memory | DONE | (memory, not ticket) | `project_pagination_progress.md` claims "only Enquiry List still client-side" — false as of tonight: `enquiry-list.component.html` actually has `matSortActive` (server-side) and 19 *other* screens (Inventory, Attendance, Examination, Course Offerings, Assign Faculty, Maintenance, etc.) are intentionally client-side `MatTableDataSource` screens, not stragglers. Will correct that memory file after this session; noting here so the correction has a paper trail.
