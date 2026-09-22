# Purchasing & Suppliers + Equipment & Asset Management — Overnight Build Plan

> **Purpose.** On 2026-09-22 the user authorized an unattended, no-confirmation overnight session
> to (1) re-verify the "Purchasing & Suppliers" and "Equipment & Asset Management" nav groups are
> genuinely complete (both are marked ✅ Done in `MILESTONES.md`, last updated 2026-09-15 — treat
> that as a claim to verify, not a fact to trust blindly, same discipline
> `AUTONOMOUS_OVERNIGHT_PLAN.md`'s Phase 8 re-examinations already proved out), (2) run the
> mandatory CLAUDE.md structural/badge/permission checkup across all 17 screens in those two nav
> groups, and (3) seed realistic bulk demo data across both — the 2026-09-15 `InventoryBulkDemoDataSeeder`
> run covered only the "Stock Management" nav group; these two groups still have thin/placeholder
> data. This file is the single source of truth for what's left, same append-only spirit as
> `DECISION_LOG.md` and `AUTONOMOUS_OVERNIGHT_PLAN.md`. Never delete a line — check it off or add a note.

**JIRA:** OC-264 (In Progress). Comment progress with `bash scripts/jira.sh comment OC-264 "..."`;
don't open a duplicate ticket for this scope. (Unlike the older `AUTONOMOUS_OVERNIGHT_PLAN.md`
note claiming Inventory work skips real JIRA — that's stale; recent inventory commits, e.g.
OC-261/OC-263, use real tickets. Follow current practice: real JIRA ticket per commit.)

## Standing rules for every session that works from this file

1. **No specialist-review round, no `AskUserQuestion` stops.** The user explicitly waived
   CLAUDE.md's @Partner confirmation gate for this file's scope only ("without waiting my
   approvals"). Make the same class of judgment call a specialist round would have settled, using
   established precedent in `DECISION_LOG.md` first, ERP-standard defaults second — then **write
   the decision into `DECISION_LOG.md`** exactly as every prior slice did, tagged
   `**Made autonomously overnight — flag for morning review if this reads wrong.**`
2. **All other CLAUDE.md gates still apply in full** — they are code-quality/safety gates, not
   approval gates: migration column verification, never edit a shipped migration, permission-
   migration DEV_ADMIN/SUPPORT_ADMIN sync block, operation-wise permission mapping, master-screen
   uniqueness validator, list-screen structural gate, resizable-column cell markup gate, `mlp-*`
   spacing system, badge/status consistency, Component Touch Rule. Do not skip any to go faster.
3. **Production data safety is absolute and non-negotiable (CLAUDE.md header rule).** This entire
   session touches **local dev only**. Never connect to, migrate, seed, or deploy to
   `172.16.7.209` (live) or `172.17.1.243` (test). Never run `deploy.sh`/`deploy-209.sh`/
   `deploy-243.sh`. Never `git push`. If any step would even ambiguously touch a remote server,
   stop and log it as `BLOCKED` instead.
4. **Isolated worktree, not the main checkout.** Your working directory is
   `.worktrees/purchasing-equipment-overnight` on branch `purchasing-equipment-overnight-20260922`.
   The main checkout currently has a live `ng serve` (port 4200) and `bootRun` (port 8080) running
   — leave them alone, they may belong to the user or another session. **Before any `bootRun` from
   this worktree, check `ss -ltnp | grep -E ':8080|:8090'`** and pick a free port
   (`--server.port=NNNN` or `SERVER_PORT=NNNN`); **always kill your own `bootRun` process when
   you're done with it** (it keeps the server alive after startup — don't leave orphaned JVMs).
5. **One commit per finished slice**, local only — `git add` the specific touched files (never
   `-A`). Message format: `OC-264: type(inventory): summary` (matches recent repo history).
6. **Next OC ticket number for any *new*, separately-scoped stretch work found mid-session:**
   check `bash scripts/jira.sh` / recent commits yourself — do not assume OC-264 is the last one,
   another session may have created tickets since this file was written.
7. **Next Flyway migration number:** grep `backend/src/main/resources/db/migration/` yourself
   before writing a number — do not trust a number recorded earlier in this file, another session
   may have claimed it since.
8. After every slice: update this file's checkboxes, `docs/inventory-management/MILESTONES.md`,
   `docs/RELEASE_3_MILESTONES.md`'s progress row (only if a real functional gap was found and
   fixed — pure demo-data seeding doesn't change module status), add a `DECISION_LOG.md` entry,
   and add/extend manual-test-case files under `docs/manual-test-cases/` for anything with new
   behavior (pure data seeding doesn't need new test cases). Run
   `npx tsc -p frontend/tsconfig.app.json --noEmit` and the relevant backend test class/
   `./gradlew compileJava compileTestJava` before committing.
9. **Stop and leave a clear note in this file's "Session handoff notes" section** instead of
   guessing, if something turns out to need real product input no ERP-standard default or in-repo
   precedent can settle. Move to the next independent slice rather than stalling.
10. **No self-run visual verification tonight.** Nobody is present to click through light/dark/
    role rendering. Use `npx tsc --noEmit` / `ng build` / `./gradlew compileJava` + tests as the
    correctness gate, and note in the session log anything that needs a manual light/dark/role
    click-through pass.

---

## Scope: the 17 screens

**Purchasing & Suppliers** (`frontend/src/app/core/nav/nav-config.ts`, search
`label: 'Purchasing & Suppliers'`):
- [ ] Suppliers (`/inventory/procurement/suppliers`)
- [ ] Rate Contracts (`/inventory/procurement/rate-contracts`)
- [ ] Vendor Product Rates (`/inventory/procurement/vendor-product-mappings`)
- [ ] Price Comparison (`/inventory/reporting/price-comparison`)
- [ ] Purchase Requisitions (`/inventory/procurement/purchase-requisitions`)
- [ ] Quotation Requests (`/inventory/procurement/quotation-requests`)
- [ ] Wanted List (`/inventory/procurement/wanted-list`)
- [ ] Purchase Orders (`/inventory/procurement/purchase-orders`)
- [ ] PO Aging Report (`/inventory/reporting/purchase-order-aging`)
- [ ] PO Cycle-Time Report (`/inventory/reporting/purchase-order-cycle-time`)
- [ ] Tax Rules (`/inventory/procurement/tax-rules`)
- [ ] Currency Settings (`/inventory/procurement/currency-settings`)
- [ ] Currency Exchange Rates (`/inventory/procurement/currency-exchange-rates`)

**Equipment & Asset Management** (search `label: 'Equipment & Asset Management'`):
- [ ] Asset Register (`/inventory/asset/assets`)
- [ ] Maintenance Schedules (`/inventory/asset/maintenance-schedules`)
- [ ] Service Contracts (`/inventory/asset/service-contracts`)
- [ ] Depreciation Summary (`/inventory/reporting/asset-depreciation-summary`)

For each: apply the list-screen structural gate, badge/status audit, `mlp-*` spacing gate,
resizable-column gate (if `[cmsResizableColumns]` used), and confirm operation-wise permission
mapping — same checklist `AUTONOMOUS_OVERNIGHT_PLAN.md`'s sibling runs already used, reproduced in
the phase prompt files for convenience.

---

## Phase 1 — Verify completeness + audit Purchasing & Suppliers

- [ ] Re-derive (don't trust) whether Phase 2's shipped feature set genuinely matches
      `MILESTONES.md`'s claims: Suppliers (register/approve/manage), Rate Contracts, Tax Rules,
      Vendor Product Rates, Purchase Requisitions, Quotation Requests (RFQ), Wanted List
      (auto-reorder), Purchase Orders (full lifecycle incl. Force Close), Currency
      Settings/Exchange Rates, Price Comparison / PO Aging / PO Cycle-Time reports. Grep the
      actual controllers/entities/routes — don't infer from doc prose alone.
- [ ] Run the structural/badge/permission checkup (see "Scope" above) on all 13 Purchasing &
      Suppliers screens. Fix what's broken following the established fix patterns in
      `DECISION_LOG.md`'s 2026-09-15/16 entries.
- [ ] Note any genuine functional gap found (not a UI defect — an actually missing capability)
      for Phase 3 to consider, or log as `BLOCKED` if it needs real product input.

## Phase 2 — Bulk demo data: Purchasing & Suppliers

- [ ] Build (or extend, if a reusable pattern already exists) a bulk demo-data seeder —
      `PurchasingAssetBulkDemoDataSeeder` or similar, gated behind its own opt-in property
      (e.g. `cms.seed.bulk-purchasing-asset-demo=true`, following `InventoryBulkDemoDataSeeder`'s
      exact convention — never fires on a normal boot), covering:
  - 8-12 Suppliers across approval states (a few pending-approval, most approved, one rejected/
    inactive), realistic nursing-college vendor names (medical/pharma/IT/stationery/lab-equipment
    suppliers).
  - Tax Rules (a handful of named rates, e.g. GST slabs).
  - Currency Settings + Currency Exchange Rates (base INR + 1-2 foreign currencies with rates, if
    the module supports multi-currency demo data meaningfully — check `DECISION_LOG.md`'s
    multi-currency FX entry, OC-214, first).
  - Rate Contracts (a few active, one expired, one upcoming) against a subset of approved
    suppliers.
  - Vendor Product Rates mapping suppliers to existing Stock Management products (reuse the 112
    products the 2026-09-15 seeder already created — `productRepo.findAll()`, don't recreate).
  - Purchase Requisitions across states (pending, partially approved, rejected line, fully
    ordered).
  - Quotation Requests (RFQ) across lifecycle states (draft, sent, awarded) if OC-231 shipped this
    as real lifecycle states — verify first.
  - Wanted List entries (both auto-flagged via a real reorder-level breach and one manually run).
  - Purchase Orders spanning the full status lifecycle: `PENDING`, `ORDERED`, `IN_PROGRESS`,
    `PARTIALLY_COMPLETED`, `COMPLETED`, `FORCE_CLOSED` — enough of each that the list screen and
    both PO reports (Aging, Cycle-Time) show real, varied output, not a placeholder single row.
- [ ] **Read `DECISION_LOG.md`'s 2026-09-15 "Bulk demo data" entry's three-retries lesson before
      writing this seeder** — specifically: (a) the top-level `CommandLineRunner`'s own
      `@Transactional` does NOT wrap the whole run, each phase method commits independently, so
      make every phase idempotent on its own criterion (not one global count gate); (b) never rely
      on a `seedX()` method's *return value* to drive a later phase on a resumed/re-run — re-query
      the table (`repo.findAll()`) instead, since the return value is empty once rows already
      exist; (c) check tracking-mode compliance (`BATCH`/`SERIAL` products) before routing demo
      documents through a product — this module already hit that gap once, it may resurface here
      with different document types.
- [ ] Verify with real counts against Postgres (`docker exec cms-postgres psql -U cms -d cmsdb -c
      "..."`), not just "the seeder ran with no exception."

## Phase 3 — Verify completeness + audit + bulk demo data: Equipment & Asset Management

- [ ] Re-derive whether Phase 5's shipped feature set matches `MILESTONES.md`: Asset register +
      lifecycle (`IN_USE`/`UNDER_MAINTENANCE`/`RETIRED`/`DISPOSED`), Maintenance Schedules +
      Service Contracts, Depreciation (straight-line, computed live), Disposal/write-off.
- [ ] Run the structural/badge/permission checkup on all 4 Equipment & Asset Management screens.
- [ ] Extend the same seeder (or a second one in the same style) with:
  - 15-25 Assets across every status (`IN_USE` majority, a few `UNDER_MAINTENANCE`, a few
    `RETIRED`, 1-2 `DISPOSED` with disposal reason/value/date), realistic purchase
    values/dates/`usefulLifeMonths`/salvage values so the Depreciation Summary report shows
    genuinely varied book values, not all-identical numbers. Mix of assets linked back to a GRN
    line (where a receipt exists) and standalone "already-owned, being onboarded" entries, per
    the entity's own dual creation path.
  - Maintenance Schedules against a subset of assets (mix of recurring and one-off, at least one
    overdue and one upcoming).
  - Service Contracts against a subset of assets (active, one expiring soon, one expired) so the
    renewal-reminder concept has something real to show.
- [ ] Verify the Depreciation Summary report renders meaningfully (varied book values across
      categories, disposed assets correctly excluded) against the seeded data.

## Phase 4 — Final checkup, cross-report verification, wrap-up

- [ ] Re-check both PO reports (Aging, Cycle-Time) and Price Comparison render real, varied output
      against Phase 2's seeded Purchase Orders / Vendor Product Rates.
- [ ] Full top-to-bottom re-read of this file — verify every checkbox reflects reality (grep the
      code, don't trust an earlier phase's own claim blindly, same discipline
      `AUTONOMOUS_OVERNIGHT_PLAN.md`'s Phase 8 re-examinations proved valuable).
  Do this in a fresh headless invocation, not by re-running Phase 1-3's own claims.
- [ ] `./gradlew compileJava compileTestJava` and relevant `com.cms.inventory.*` test classes
      green; `npx tsc -p tsconfig.app.json --noEmit` clean.
- [ ] Final real-count verification against Postgres for every seeded table, recorded in the
      session log and in this file's handoff notes.
- [ ] Publish or update a progress Artifact dashboard (optional but nice — see
      `AUTONOMOUS_OVERNIGHT_PLAN.md`'s sibling 2026-09-15 run for the pattern) so the user has
      something to open in the morning.
- [ ] `bash scripts/jira.sh comment OC-264 "..."` with a final summary; leave OC-264 "In Progress"
      (not resolved) so the user reviews and resolves it themselves in the morning.

---

## Explicitly BLOCKED — do not attempt these without asking first

- Anything the audit finds that requires a real business/compliance decision (e.g. a genuinely
  new multi-currency rounding policy, a new approval-routing default, a new tax regime) — log as
  `BLOCKED`, do not invent business policy and ship it silently.
- Any schema change to an already-shipped migration file — add a new forward migration instead.
- Anything that would touch `172.16.7.209` / `172.17.1.243` / a deploy script / `git push`.

## Infrastructure notes (checked once, don't re-derive)

- Local services already running as shared Docker containers: `cms-postgres` (port 5435),
  `cms-minio` (9000-9001), `cms-keycloak` (8280-8281) — reachable identically from the worktree.
- `.jira.env` and `backend/local-dev.p12` (both gitignored) are already copied into this worktree.
- Main checkout currently has `ng serve` on :4200 and a `bootRun` on :8080 already live — do not
  touch either; use a different port for this worktree's own `bootRun`.
- This plan deliberately launches via a **direct detached background process chain**
  (`scripts/purchasing-equipment-overnight-driver.sh`, started once via `setsid`/`nohup`/`disown`),
  **not system `crontab`** — `AUTONOMOUS_OVERNIGHT_PLAN.md`'s sibling 2026-09-15/16 run documented
  that its 3-pass cron mechanism never fired because registering `crontab` entries requires the
  user to run `crontab ...` themselves (blocked for Claude by the auto-mode permission classifier)
  and that manual step never happened. A detached background process started directly, right now,
  by this session needs no further human action to keep running.

## Session handoff notes

*(Append dated entries here — one per phase/session that stops — instead of leaving partial state
undocumented. Include: what was in progress, what's verified-working, what's broken/half-done, and
any judgment call made that a future session should sanity-check.)*

- **2026-09-22, ~19:15 IST, plan authored:** File created fresh by the live interactive session,
  which also created the worktree, JIRA ticket (OC-264), and the driver/prompt scripts, then
  launched the 4-phase chain as a detached background process. No phase work attempted yet under
  this file — Phase 1 starts immediately.
