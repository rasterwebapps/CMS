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

## Current Screen Inventory (rebuild in Step 0 — placeholder until then)

*To be filled in by Step 0. Do not assume the old TOUR_FLOWMAP checklist's
Tour/FM columns are still accurate for any screen shipped or touched after
2026-08-19.*

## Progress Log

Format: `- YYYY-MM-DD HH:MM | <screen/area> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket> | <note>`

- 2026-08-25 18:4X | setup | DONE | OC-168 | Parent ticket created, this audit doc scaffolded, cron session scheduled for 21:00 tonight via `scripts/screen-audit-autonomous-run.sh`. No screens checked yet — this is the pre-work commit.
