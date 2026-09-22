# Purchasing & Suppliers — Bulk Demo Data (Phase 2 of 4)

You are the SECOND of four unattended, chained sessions running tonight back-to-back. Nobody is
available to answer questions. Do not stop to ask for input — make the most reasonable call, write
it down, and keep working until the stop condition at the bottom.

## Working directory — read this first

`/home/raster/Idea Projects/SKSCMS/.worktrees/purchasing-equipment-overnight` — git worktree,
branch `purchasing-equipment-overnight-20260922`. Do not touch the main repo root (live `ng serve`
:4200 / `bootRun` :8080 there). Before any `bootRun`, check `ss -ltnp | grep -E ':8080|:8090'`,
pick a free port, and kill your own process when done.

## Read the plan + prior phase's work first

1. `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_PLAN.md` — the single source of truth
   (standing rules, full scope, all four phases). This prompt only repeats Phase 2's own slice.
2. `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md` and the plan file's
   "Session handoff notes" — see what Phase 1 actually did (don't assume, verify with `git log`
   on this branch and a spot-check against Postgres).
3. `docs/inventory-management/DECISION_LOG.md`'s **2026-09-15 "Bulk demo data for Stock
   Management" entry — read this in full before writing any seeder code.** It documents three real
   retries and the exact lessons from each; do not rediscover them the hard way:
   - The top-level `CommandLineRunner`'s own `@Transactional` does **not** wrap the whole run —
     each phase method commits independently. Make every phase idempotent on **its own**
     criterion (e.g. "does this table already have rows"), not one global count gate.
   - Never rely on a `seedX()` method's **return value** to drive a later phase on a resumed run —
     it's empty once the rows already exist. Re-query instead (`repo.findAll()`).
   - Check tracking-mode compliance (`BATCH`/`SERIAL` products) before routing a demo document
     through a product if the same `StockMovementService.requireTrackingModeCompliance` gate
     applies to whatever document type you're seeding.

## JIRA

Continue on **OC-264**. `bash scripts/jira.sh comment OC-264 "..."` for progress notes.

## Your job this phase

Build (or extend, if Phase 1 already started one) a bulk demo-data seeder for the Purchasing &
Suppliers screens — e.g. `PurchasingAssetBulkDemoDataSeeder`, gated behind its own opt-in property
(`cms.seed.bulk-purchasing-asset-demo=true`, following `InventoryBulkDemoDataSeeder`'s exact
convention in `com.cms.inventory.stock.config` — never fires on a normal boot). Full target list is
in the plan file's Phase 2 section:

- 8-12 Suppliers, mixed approval states (a few pending, most approved, one rejected/inactive),
  realistic nursing-college vendor names.
- Tax Rules (a handful of named rates).
- Currency Settings + Currency Exchange Rates (base INR + 1-2 foreign currencies) — first check
  `DECISION_LOG.md`'s multi-currency FX entry (OC-214) for what actually shipped before assuming
  the shape.
- Rate Contracts (a few active, one expired, one upcoming) against approved suppliers.
- Vendor Product Rates mapping suppliers to **existing** Stock Management products — reuse the 112
  products the 2026-09-15 seeder created (`productRepo.findAll()`), do not recreate products.
- Purchase Requisitions across states (pending, partially approved, rejected line, fully ordered).
- Quotation Requests (RFQ) across lifecycle states, if OC-231 shipped real lifecycle states —
  verify first by reading the entity/enum.
- Wanted List entries (at least one genuinely auto-flagged via a real reorder-level breach, plus
  one manually triggered run).
- Purchase Orders spanning the full status lifecycle (`PENDING`, `ORDERED`, `IN_PROGRESS`,
  `PARTIALLY_COMPLETED`, `COMPLETED`, `FORCE_CLOSED`) with enough of each that the list screen and
  both PO reports (Aging, Cycle-Time) show real, varied output.

Run the seeder against the local dev DB (via your own `bootRun` on a free port, or a Boot test/
runner — whatever this repo's existing seeder invocation convention is, check how
`InventoryBulkDemoDataSeeder` gets triggered) and **verify with real counts against Postgres**:
```
docker exec cms-postgres psql -U cms -d cmsdb -c "select 'suppliers', count(*) from suppliers union all select 'purchase_orders', count(*) from purchase_orders ..."
```
Don't just trust "the seeder ran with no exception" — confirm the rows actually landed, same
discipline the 2026-09-15 entry's final "Verified" paragraph used.

## Hard rules (from CLAUDE.md — repeated for emphasis)

- **Local dev only. Production data safety is absolute.** Never touch `172.16.7.209` /
  `172.17.1.243`. Never `git push`. Never run a `deploy*.sh` script.
- **Migration column verification:** grep existing migrations for real column names before any
  `INSERT` you write directly (not through JPA). Never guess.
- **Never edit an already-shipped migration file.**
- **No specialist-review / `AskUserQuestion` stops** — waived for this file's scope only.
- **Commit style:** `OC-264: type(inventory): summary`, one commit per logical slice, specific
  `git add` only.
- Purely fabricated demo data doesn't need new manual-test-case files — only note in the session
  log what was seeded and roughly how many rows, for the user's morning review.

## Progress tracking

Append to `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md` (same format as
Phase 1). Update the plan file's Phase 2 checkboxes and "Session handoff notes" (append, don't
overwrite) before you stop.

## Stop condition

Work through Phase 2's full target list. When genuinely done (seeder built, run, data verified by
real count, compiling, tested, committed), stop cleanly — Phase 3 (Equipment & Asset Management:
verify + audit + seed) starts automatically. If a real blocker forces you to stop early, leave the
working tree clean and the handoff note explicit about exactly what's seeded vs. not.
