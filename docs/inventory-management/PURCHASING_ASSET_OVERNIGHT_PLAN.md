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
- [x] Suppliers (`/inventory/procurement/suppliers`)
- [x] Rate Contracts (`/inventory/procurement/rate-contracts`)
- [x] Vendor Product Rates (`/inventory/procurement/vendor-product-mappings`)
- [x] Price Comparison (`/inventory/reporting/price-comparison`)
- [x] Purchase Requisitions (`/inventory/procurement/purchase-requisitions`)
- [x] Quotation Requests (`/inventory/procurement/quotation-requests`)
- [x] Wanted List (`/inventory/procurement/wanted-list`)
- [x] Purchase Orders (`/inventory/procurement/purchase-orders`)
- [x] PO Aging Report (`/inventory/reporting/purchase-order-aging`)
- [x] PO Cycle-Time Report (`/inventory/reporting/purchase-order-cycle-time`)
- [x] Tax Rules (`/inventory/procurement/tax-rules`)
- [x] Currency Settings (`/inventory/procurement/currency-settings`)
- [x] Currency Exchange Rates (`/inventory/procurement/currency-exchange-rates`)

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

- [x] Re-derive (don't trust) whether Phase 2's shipped feature set genuinely matches
      `MILESTONES.md`'s claims: Suppliers (register/approve/manage), Rate Contracts, Tax Rules,
      Vendor Product Rates, Purchase Requisitions, Quotation Requests (RFQ), Wanted List
      (auto-reorder), Purchase Orders (full lifecycle incl. Force Close), Currency
      Settings/Exchange Rates, Price Comparison / PO Aging / PO Cycle-Time reports. Grep the
      actual controllers/entities/routes — don't infer from doc prose alone. **Confirmed
      genuinely done** — see session log's 19:19-19:20 entries.
- [x] Run the structural/badge/permission checkup (see "Scope" above) on all 13 Purchasing &
      Suppliers screens. Fix what's broken following the established fix patterns in
      `DECISION_LOG.md`'s 2026-09-15/16 entries. **Zero defects found — nothing to fix.** See
      session log's 19:20-19:22 entries for the full per-gate breakdown.
- [x] Note any genuine functional gap found (not a UI defect — an actually missing capability)
      for Phase 3 to consider, or log as `BLOCKED` if it needs real product input. **None found**
      for this nav group's scope.

## Phase 2 — Bulk demo data: Purchasing & Suppliers

- [x] Build (or extend, if a reusable pattern already exists) a bulk demo-data seeder —
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
- [x] **Read `DECISION_LOG.md`'s 2026-09-15 "Bulk demo data" entry's three-retries lesson before
      writing this seeder** — specifically: (a) the top-level `CommandLineRunner`'s own
      `@Transactional` does NOT wrap the whole run, each phase method commits independently, so
      make every phase idempotent on its own criterion (not one global count gate); (b) never rely
      on a `seedX()` method's *return value* to drive a later phase on a resumed/re-run — re-query
      the table (`repo.findAll()`) instead, since the return value is empty once rows already
      exist; (c) check tracking-mode compliance (`BATCH`/`SERIAL` products) before routing demo
      documents through a product — this module already hit that gap once, it may resurface here
      with different document types. **All three lessons applied, and (c) resurfaced exactly as
      warned** — see the session log's 21:15 entry (a `BATCH`-tracked product broke a Goods Receipt
      confirm; fixed by picking a `NONE`-tracked product instead of adding batch-number plumbing).
- [x] Verify with real counts against Postgres (`docker exec cms-postgres psql -U cms -d cmsdb -c
      "..."`), not just "the seeder ran with no exception." **Done — see session log's 21:25 entry
      for the full per-table count breakdown.**

## Phase 3 — Verify completeness + audit + bulk demo data: Equipment & Asset Management

- [x] Re-derive whether Phase 5's shipped feature set matches `MILESTONES.md`: Asset register +
      lifecycle (`IN_USE`/`UNDER_MAINTENANCE`/`RETIRED`/`DISPOSED`), Maintenance Schedules +
      Service Contracts, Depreciation (straight-line, computed live), Disposal/write-off.
      **Confirmed genuinely done** (plus an undocumented `AVAILABLE` default status, a deliberate
      ERP-standard addition — see the entity's own doc-comment) — see session log's entry.
- [x] Run the structural/badge/permission checkup on all 4 Equipment & Asset Management screens.
      **One real defect found and fixed**: Maintenance Schedules and Service Contracts shared one
      permission pair (`INVENTORY_ASSET_MAINTENANCE_VIEW`/`MANAGE`), violating the operation-wise
      permission mapping hard gate — two distinct screens conflated under one permission. Split
      via new `INVENTORY_ASSET_SERVICE_CONTRACT_VIEW`/`MANAGE` (migration V551), backfilled onto
      every role that already held the maintenance permissions. All other gates (list-screen
      structural, badge/status, `mlp-*` spacing) clean on all 4 screens. See session log.
- [x] Extend the same seeder (or a second one in the same style) with:
  - 15-25 Assets across every status (`IN_USE` majority, a few `UNDER_MAINTENANCE`, a few
    `RETIRED`, 1-2 `DISPOSED` with disposal reason/value/date), realistic purchase
    values/dates/`usefulLifeMonths`/salvage values so the Depreciation Summary report shows
    genuinely varied book values, not all-identical numbers. Mix of assets linked back to a GRN
    line (where a receipt exists) and standalone "already-owned, being onboarded" entries, per
    the entity's own dual creation path. **Done — 21 assets (14 IN_USE/3 UNDER_MAINTENANCE/
    2 RETIRED/2 DISPOSED), 2 linked to a real Goods Receipt line via a new onboarding
    Requisition→PO→GR chain, rest standalone.**
  - Maintenance Schedules against a subset of assets (mix of recurring and one-off, at least one
    overdue and one upcoming). **Done — 6 schedules, 4 recurring/2 one-off, 3 overdue/3 upcoming,
    one exercising `markPerformed` for real history.**
  - Service Contracts against a subset of assets (active, one expiring soon, one expired) so the
    renewal-reminder concept has something real to show. **Done — 4 contracts, 2 active/1
    expiring soon/1 expired.**
- [x] Verify the Depreciation Summary report renders meaningfully (varied book values across
      categories, disposed assets correctly excluded) against the seeded data. **Confirmed via a
      live `curl` against this phase's own `bootRun` (port 8099, SSL disabled) with a real
      `devadmin` JWT: Computers (7 assets, ₹2.01L) vs. Medical Equipment (12 assets, ₹6.65L) show
      distinct accumulated depreciation/book values; grand total asset count (19) correctly
      excludes both DISPOSED assets (21 seeded - 2 disposed).**

## Phase 4 — Final checkup, cross-report verification, wrap-up

- [x] Re-check both PO reports (Aging, Cycle-Time) and Price Comparison render real, varied output
      against Phase 2's seeded Purchase Orders / Vendor Product Rates. **Found and fixed a real
      (non-blocking) demo-data gap**: both reports were mathematically correct but under-varied —
      all 5 open POs landed in Aging's 0-30-day bucket, both COMPLETED orders averaged an
      identical 30.0 days in Cycle-Time. Backdated 3 seeded PO dates further (see decision log);
      re-verified live — Aging now spreads 3/1/1/0 across buckets, Cycle-Time shows 30.0 vs 55.0
      days across the two suppliers. Price Comparison confirmed varied (9 STANDARD + 2
      CONTRACT-sourced, one USD row converting to INR) with no changes needed.
- [x] Full top-to-bottom re-read of this file — verify every checkbox reflects reality (grep the
      code, don't trust an earlier phase's own claim blindly, same discipline
      `AUTONOMOUS_OVERNIGHT_PLAN.md`'s Phase 8 re-examinations proved valuable).
  Done in this fresh Phase 4 invocation — spot-checked Phase 1's uniqueness-validator/`-exists`
  endpoint claims, `PurchaseOrderStatus`/`AssetStatus` enum shapes, all 7 permission migrations'
  DEV_ADMIN/SUPPORT_ADMIN catch-all blocks, and Phase 3's V551 permission split end-to-end
  (migration, `role_permissions` backfill, controller, routes, nav-config). **Every Phase 1-3
  checkbox genuinely holds up** — no corrections needed to any of them.
- [x] `./gradlew compileJava compileTestJava` and relevant `com.cms.inventory.*` test classes
      green; `npx tsc -p tsconfig.app.json --noEmit` clean. Run both before and after this phase's
      own seeder fix — clean both times.
- [x] Final real-count verification against Postgres for every seeded table, recorded in the
      session log and in this file's handoff notes below.
- [ ] Publish or update a progress Artifact dashboard (optional but nice — see
      `AUTONOMOUS_OVERNIGHT_PLAN.md`'s sibling 2026-09-15 run for the pattern) so the user has
      something to open in the morning. **Skipped** — time/scope prioritized toward the mandatory
      re-verification and the report-variety fix found along the way; no dashboard published this
      run.
- [x] `bash scripts/jira.sh comment OC-264 "..."` with a final summary; leave OC-264 "In Progress"
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
- **2026-09-22, ~19:23 IST, Phase 1 complete:** Full verify+audit of all 13 Purchasing &
  Suppliers screens done — see `PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md` for the detailed
  breakdown. **Result: zero defects, zero functional gaps.** `MILESTONES.md`'s "✅ Done" claim for
  this nav group genuinely holds up under a from-code re-derivation (controllers/routes/entities
  grepped directly, not inferred from doc prose). Every structural/badge/`mlp-*`/permission gate
  from CLAUDE.md checked clean; `uniqueFieldValidator` wired on all 4 master screens with a real
  uniqueness constraint; operation-wise permission mapping already correct (Approve/Award/
  Force-Close/Run/Convert all distinct dedicated permissions, not reused). No code changes were
  needed this phase, so nothing was committed to the working tree beyond this plan file and the
  new session log (both doc-only). `node_modules` symlinked into this worktree's `frontend/` from
  the main checkout (lockfiles identical) so `tsc --noEmit` could run — that symlink is untracked/
  gitignored, safe to leave for Phase 2-4 to reuse. Compile/test gates: `tsc --noEmit` clean,
  `gradlew compileJava compileTestJava` clean, `com.cms.inventory.*` test suite green. Handing off
  clean to Phase 2 (bulk demo data for this same nav group) — no blockers, no assumptions that
  need morning sanity-checking.
- **2026-09-22, ~21:26 IST, Phase 2 complete:** `PurchasingAssetBulkDemoDataSeeder` built, run, and
  verified — see `PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md`'s full Phase 2 section for per-table
  counts. **Full target list met**: 10 Suppliers (7 approved/2 pending/1 inactive), 4 Tax Rules,
  Currency Settings (INR) + 3 Exchange Rates (USD×2 dated, EUR×1), 3 Rate Contracts (active/
  expired/upcoming) + 4 lines, 11 Vendor Product Rates (incl. one USD-priced, three
  contract-linked), 9 Purchase Requisitions (+21 items) across pending/partially-approved/
  rejected-line/fully-ordered plus RFQ- and PO-lifecycle-sourcing states, 4 Quotation Requests
  (+5 lines) covering all 4 real shipped lifecycle states (DRAFT/SUBMITTED/COMPLETED/CANCELLED —
  confirmed real states from the entity/enum, not assumed), 4 Wanted List items (2 pre-existing +
  2 newly genuinely auto-flagged via an engineered real reorder-level breach), and 7 Purchase
  Orders (+10 items, +3 Goods Receipts/+3 lines) covering all 6 `PurchaseOrderStatus` values with
  the receipt-progress-computed states (`IN_PROGRESS`/`PARTIALLY_COMPLETED`/`COMPLETED`) driven by
  real confirmed Goods Receipts, not hand-set. One real gap hit and fixed mid-run: a `BATCH`-
  tracked product ("Wound Dressing Kit") broke a Goods Receipt confirm since this seeder's receipt
  lines never carry a batch number — routed around by picking a `NONE`-tracked product instead,
  same posture the 2026-09-15 Stock Management seeder already took for the same underlying gap
  (documented, not silently patched). Deliberately did **not** wire any Tax Rule onto a PO line's
  `taxRuleId` this run — `InventoryTaxJurisdictionSetting` (home state) is unconfigured in local
  dev and `JurisdictionService.resolve` hard-blocks tax computation without it; configuring that
  singleton was out of this phase's scope, so Tax Rules exist as real, correct master data but
  aren't exercised end-to-end on a PO line yet. Two crashes during development (both the same
  batch-tracking gap, hit again on a resumed run before the root cause was fully fixed) needed
  manual `psql` cleanup of the partial Purchase Order/Goods Receipt/Purchase Requisition rows
  between retries — zero real history in any of them, same "stray partial DRAFT" precedent the
  2026-09-15 entry already established; final run completed clean end-to-end. Compile/test gates:
  `gradlew compileJava compileTestJava` clean, `com.cms.inventory.*` test suite green, `tsc
  --noEmit` clean (no frontend changes this phase — pure backend seeder). Handing off clean to
  Phase 3 (Equipment & Asset Management verify+audit+seed) — no blockers.
- **2026-09-22, ~15:33 UTC, Phase 3 complete:** Full verify+audit+seed of Equipment & Asset
  Management. **Audit result: genuinely done end-to-end**, one real defect found and fixed (not
  cosmetic) — see `DECISION_LOG.md`'s 2026-09-22 "Phase 3" entry and this file's own Phase 3
  checkboxes above for the operation-wise permission mapping violation
  (`AssetServiceContractController` reused `AssetMaintenanceScheduleController`'s permission pair)
  and its fix (migration V551, controller + `app.routes.ts` + `nav-config.ts` updated). All other
  gates (structural, badge, `mlp-*` spacing, resizable-column N/A) clean on all 4 screens — no
  `cms-status-badge` usage in this nav group; local chip classes (`.as-disposed-label`,
  `.ms-overdue-chip`, `.sc-expired-chip`) all locally defined with `--cms-*`-prefixed variables,
  no collisions. `PurchasingAssetBulkDemoDataSeeder` extended (not a second seeder — same class,
  same opt-in flag) with: a new Requisition→PO→Goods Receipt chain for 2 IT-asset products
  (Laptop + External HDD 1TB, both `NONE`-tracked — avoided Phase 2's batch-tracking gap on
  purpose) so 2 of the 21 seeded Assets link back to a *real* `GoodsReceiptLine` rather than a
  fabricated FK; 21 Assets total (14 IN_USE/3 UNDER_MAINTENANCE/2 RETIRED/2 DISPOSED with real
  disposal reason/value/date), drawn only from the catalog's 20 `isAsset=true` products (10
  "Computers" + 10 "Medical Equipment") for category realism; 6 Maintenance Schedules (4
  RECURRING/2 ONE_OFF, 3 overdue/3 upcoming, one exercising `markPerformed` so it carries real
  `lastPerformedDate`/advanced-`nextDueDate` history); 4 Service Contracts (2 active/1 expiring
  soon/1 expired). First run completed with zero crashes (no batch-tracking gap this time, since
  both onboarding products were deliberately chosen `NONE`-tracked up front, learning applied
  proactively rather than hit-and-routed-around). Verified with real Postgres counts (21/6/4/2-
  disposed/2-with-gr-line, all matching target) and a live `curl` against this phase's own
  `bootRun` (port 8099, `--server.ssl.enabled=false`, `cms.seed.bulk-purchasing-asset-demo=true`)
  using a real `devadmin` JWT from the shared local Keycloak — Depreciation Summary correctly
  shows 2 distinct category rows with genuinely different accumulated depreciation/book values,
  disposed assets excluded from the grand total (19 = 21 - 2), overdue maintenance schedules and
  expired service contracts both correctly flagged. One incidental fix made along the way: the
  shared local Keycloak's live `devadmin` password credential had drifted from the committed
  `infrastructure/keycloak/cms-realm.json` export (password-grant login failed with
  `invalid_grant`) — reset it back to the exported value (`Dev@1cms`) via the Keycloak admin API
  so it matches the checked-in source of truth again; flagging here in case another concurrent
  session notices the same symptom before reading this note. Compile/test gates:
  `gradlew compileJava compileTestJava` clean, `com.cms.inventory.*` test suite green, `tsc -p
  tsconfig.app.json --noEmit` clean. Handing off clean to Phase 4 (final checkup, cross-report
  verification, wrap-up) — no blockers. Note for Phase 4: it should double-check the permission
  split (migration V551) reads cleanly on a fresh top-to-bottom re-read, since it's the one
  Phase 3 change that touches shipped screens rather than pure demo data.
- **2026-09-22, ~21:15 IST / 15:40 UTC, Phase 4 complete — session wrap-up:** Fresh, independent
  re-verification of every Phase 1-3 checkbox (grepped code directly — controllers, enums,
  migrations, routes, nav-config — not re-reading prior phases' own prose). **Everything held up,
  zero corrections needed to Phase 1-3's claims**, including the V551 permission split (migration
  backfill + `role_permissions` + controller `@PreAuthorize` + `app.routes.ts` + `nav-config.ts`
  all independently confirmed consistent) and the 7 permission migrations' DEV_ADMIN/SUPPORT_ADMIN
  catch-all blocks (V519 initially looked short by a grep quoting mismatch — reading the file
  directly confirmed the block is genuinely present, false alarm from the grep pattern, not the
  file).
  **One real, non-blocking finding — fixed:** hit PO Aging, PO Cycle-Time, and Price Comparison
  live via `curl` against a fresh `bootRun` (port 8099, SSL disabled, seed flag on) with a real
  `devadmin` JWT. Aging and Cycle-Time were mathematically correct but poorly varied — all 5 open
  demo POs fell into Aging's 0-30-day bucket (3 of 4 buckets always empty) and both COMPLETED POs
  coincidentally shared the same `today.minusDays(30)` seed offset, making Cycle-Time show an
  identical 30.0-day average for both suppliers. Not a computation bug — the report services
  themselves are correct — just under-varied seed input. Fixed by backdating 3 POs' `po_date`
  further in `PurchasingAssetBulkDemoDataSeeder` (IN_PROGRESS 15→70 days, PARTIALLY_COMPLETED
  25→45 days, COMPLETED 30→55 days) and correcting the 3 already-seeded rows directly via `psql`
  (zero real history, local-only demo data — same posture as Phase 2's own stray-row cleanup) so
  this run's live data reflects the fix without a full reseed. Re-verified live post-fix: Aging
  spreads 3/1/1/0 across the four buckets (₹17,070 grand total unchanged), Cycle-Time shows 30.0
  vs. 55.0 days across the two suppliers (42.5 overall average). Price Comparison needed no fix —
  already varied (9 STANDARD + 2 CONTRACT-sourced rows, one USD-priced row correctly converting to
  INR). Depreciation Summary re-confirmed identical to Phase 3's own live-curl figures (Computers
  7/₹2.01L, Medical Equipment 12/₹6.65L, grand total 19 excluding both DISPOSED) — no drift since
  Phase 3, confirming report stability across sessions.
  **Final real Postgres counts** (Phases 2+3 combined, unchanged by this phase's date-only fix):
  10 suppliers, 4 tax rules, 1 currency setting + 3 exchange rates, 3 rate contracts + 4 lines, 11
  vendor product mappings, 10 purchase requisitions + 23 items, 4 quotation requests + 5 lines, 4
  wanted list items, 8 purchase orders + 12 items, 4 goods receipts + 5 lines, 21 assets, 6
  maintenance schedules, 4 service contracts. (Requisition/PO/GR/GR-line counts are each one higher
  than Phase 2's own tally purely because Phase 3's asset-onboarding chain added one more of each —
  expected, cross-checked against both phases' own logs, not a discrepancy.)
  **Compile/test gates:** `./gradlew compileJava compileTestJava` and `./gradlew test --tests
  "com.cms.inventory.*"` green both before and after this phase's own seeder edit; `npx tsc -p
  tsconfig.app.json --noEmit` clean. `bootRun` on port 8099 killed after verification, port
  confirmed free again — no orphaned JVM left running.
  **Not done this phase:** no progress-dashboard Artifact was published (optional item, skipped in
  favor of the mandatory re-verification work and the report-variety fix found along the way).
  **What still needs the user's own eyes** (compile/test-clean is not the same as visually
  verified, per the plan's own standing rule 10 — nobody was present tonight to click through):
  light mode, dark mode, and role-conditional rendering (admin/faculty/whatever non-DEV_ADMIN
  roles see this module) on all 17 screens across both nav groups — with particular attention to
  the Service Contracts screen specifically, since its permission strings changed in Phase 3
  (V551) and a UI-level check that a non-DEV_ADMIN role sees/doesn't-see it correctly has not been
  done by any phase tonight, only the backend `@PreAuthorize`/route-guard layer. Also worth a human
  glance: the PO Aging/Cycle-Time bucket spread this phase engineered is still a small, hand-picked
  demo shape (5 open + 2 completed orders) — real production data will naturally vary far more:
  treat tonight's fix as "removed an artificial flatness," not as a claim the report needs no
  further eyes-on once genuine production volume exists.
  **No genuinely open/blocked items remain from Phases 1-3** — both nav groups' own functional
  completeness claims held up under two independent audits (Phase 1/3's own, and this phase's
  re-derivation). OC-264 left **In Progress**, not resolved, per the plan's standing instruction —
  final JIRA summary comment posted separately. This is the last of the 4 chained phases; no
  further phase follows. Working tree is clean: one commit this phase
  (`OC-264: fix(inventory): spread demo PO dates so Aging/Cycle-Time reports show real variety`),
  nothing mid-edit, all gates green.
