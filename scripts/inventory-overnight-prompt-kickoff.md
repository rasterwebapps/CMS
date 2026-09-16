# Inventory / Stock Management — Overnight Kickoff (2026-09-15 21:00 IST, 1 of 3)

You are the FIRST of three scheduled unattended sessions tonight (kickoff ~21:00, midpoint
~02:00, final ~07:00, wrapping up by 10:00 when the user arrives). Nobody is available to answer
questions. Do not stop to ask for input at any point — make the most reasonable call yourself,
write down what you assumed, and keep working until the stop condition at the bottom.

## Working directory — read this first

Your working directory is already `/home/raster/Idea Projects/SKSCMS/.worktrees/inventory-overnight`
— a git worktree on branch `inventory-overnight-2026-09-15`. **Do not `cd` to
`/home/raster/Idea Projects/SKSCMS` (the main repo root) and do not touch it.** A *different*
concurrent Claude Code session was found actively branch-switching in that shared main checkout
this evening (unrelated timetable ticket OC-227). Working in this worktree means you never collide
with whatever that or any other concurrent session is doing there. `.jira.env` and
`backend/local-dev.p12` (both gitignored, machine-local) are already copied into this worktree —
JIRA and HTTPS boot both work here exactly as in the main checkout.

Local services (Postgres :5435, MinIO, Keycloak) are shared docker containers, already running,
reachable from here exactly as from the main checkout. **Before any `bootRun`, check
`ss -ltnp | grep -E ':8080|:8090'`** — pick a free port (`--server.port=NNNN`), and **kill your own
bootRun process when you're done with it** (`bootRun` keeps the server alive after startup — three
retries tonight burned time on stale processes still holding the port from a previous attempt).

## Context: what's already done (by the live session, just before you started)

- Fixed two real bugs in Stock Issue Request / Stock Transfer: a zero-cost Internal Return
  valuation bug, and a missing location-role gate (sister-to-sister requests/transfers were
  previously unrestricted). Committed `1b0d4e7b` on `inventory-location-role-gate`, which this
  worktree's branch is based on.
- Audited and fixed Stock Issue Requests, Loanable Item Issues, and Stock Transfers (3 of the 16
  Stock Management screens) — structurally clean, badge/permission-compliant. Commit `14153cef`.
- Built and ran `InventoryBulkDemoDataSeeder` (`backend/src/main/java/com/cms/inventory/stock/
  config/InventoryBulkDemoDataSeeder.java`) and **confirmed the data landed**: 112 products, 9
  locations (mix of STORE/REQUESTING_POINT), 11 categories, 10 brands, 21 batch/expiry-tracked
  lots, 6 Stock Issue Requests across every lifecycle state, 4 Stock Transfers, 3 Loanable Item
  Issues. Full story (including 3 real retries and what each one taught) in
  `docs/inventory-management/DECISION_LOG.md`'s 2026-09-15 "Bulk demo data" entry — read it before
  touching the seeder again.
- A live progress dashboard is published at **https://claude.ai/artifact/V7nq1aNW5FciuVpp7N7Vx2**
  — republish it at the end of your session (see "Update the dashboard" below).

**First thing you do:** a quick sanity spot-check that the data is still there (someone/something
could have touched it), don't just trust this note:
```
docker exec cms-postgres psql -U cms -d cmsdb -c "
select 'products', count(*) from products union all select 'stock_transfers', count(*) from stock_transfers
union all select 'loanable_item_issues', count(*) from loanable_item_issues;"
```
If products < 100, re-run the seeder yourself (see the DECISION_LOG entry for the exact command
and the gotchas already hit) before moving on.

## JIRA

Continue on **OC-231** (already created and In Progress). `bash scripts/jira.sh comment OC-231
"..."` for a progress note is fine; don't create a duplicate ticket for this same overnight scope.

## Scope and priority order

**1. (mandatory floor) Complete checkup of every screen under the "Stock Management" nav group**
(`frontend/src/app/core/nav/nav-config.ts`, search `label: 'Stock Management'`). 3 of 16 are
already done (above). Work through as many of the other 13 as you can tonight — you will not
finish all 13 in one session; that's expected, the midpoint and final sessions continue where you
stop:

- Dashboard (`/inventory/dashboard`)
- Products (`/inventory/products`)
- Categories (`/inventory/categories`)
- Units of Measure (`/inventory/uoms`)
- Brands (`/inventory/brands`)
- UOM Conversion Templates (`/inventory/uom-conversion-templates`)
- Locations (`/inventory/locations`)
- Storage Racks (`/inventory/racks`)
- Stock Balance (`/inventory/stock/balances`)
- Cycle Counts (`/inventory/stock/cycle-counts`)
- Stock Valuation (`/inventory/reporting/stock-valuation`)
- Goods Receipts (`/inventory/receiving/goods-receipts`)
- Supplier Returns (`/inventory/receiving/supplier-returns`)

For each screen, per CLAUDE.md's mandatory gates (already loaded in your context — repeated for
emphasis, the ones most likely to matter):
- **List-screen structural gate**: `mat-paginator` nested inside `.content-card.mlp-table-card`,
  right after an inner `.table-wrapper`; `matSort` (if present) fully bound
  (`matSortActive`/`matSortDirection`/`(matSortChange)`); every `mat-sort-header` sort key a real,
  directly-queryable entity field, not DTO-only/joined.
- **Badge/status/enum/flag audit**: grep `cms-badge`, `status-badge`, `cms-status-badge`,
  dynamically-interpolated class names; confirm every modifier class is actually defined (globally
  in `styles.scss` or locally in that component's own `.scss`); if using `<cms-status-badge>`,
  check every real enum value against `resolveClass()`'s switch in `frontend/src/app/shared/
  status-badge/status-badge.component.ts`.
- **mlp-page spacing gate**: no local `*-hdr` class declaring `padding-bottom`; `.mlp-table-card`
  always paired with `.content-card`/`.mlp-table-wrap`; never reference an undefined `mlp-*` class.
- **Resizable-column cell markup gate** (only if `[cmsResizableColumns]` is used): flex-wrapped
  stacked-text cells need `min-width: 0`; each text line its own leaf element.
- **Operation-wise permission mapping**: verify each distinct button/operation already has its own
  dedicated permission — you're auditing tonight, not adding new operations.
- Nobody can visually confirm light/dark/role rendering tonight — use
  `npx tsc -p tsconfig.app.json --noEmit` / `ng build` (SCSS-variable fixes) and
  `./gradlew compileJava compileTestJava` + relevant tests as your correctness gate instead. Note
  in the session log anything that still needs a manual light/dark/role click-through.

Fix what's broken, following the same reasoning/fix pattern already applied tonight to the three
screens above — see the 2026-09-15 DECISION_LOG entries for the concrete style to match.

**2. (stretch, only once the 13-screen floor is fully done, lowest priority) Quotation Request →
Quotation Approval → Purchase Order → Purchase-against-PO → stock update.** The user asked for
this mid-session. **Investigate before building:** Phase 2 (Purchasing & Suppliers) is already
shipped — Purchase Requisition, Purchase Order, Wanted List, Price Comparison, Rate Contracts all
exist (`docs/inventory-management/MILESTONES.md`'s Phase 2). "Purchase against PO + stock update"
is very likely already Goods Receipt (Phase 3, shipped). Read the existing Purchase Requisition →
Purchase Order → Goods Receipt code path fully first. If "Quotation Request/Approval" turns out to
just be the user's own name for the existing Purchase Requisition (+ Price Comparison as the
quote-compare step), **don't build a duplicate module** — say so clearly in the session log and
move on to auditing those existing screens with the same rigor as item 1, treating them as
additional Stock Management-adjacent screens worth the same checkup. If it's genuinely a new
concept (an explicit multi-supplier RFQ step before a Requisition becomes eligible for a PO), this
is real new-feature work with no time for a human specialist round tonight — make the most
reasonable call favoring the existing Purchase Requisition/PO code shape (same DRAFT→SUBMITTED→
APPROVED lifecycle, same header/line split, same permission-migration pattern), document every
design decision and its rationale in the session log exactly like the R2 autonomous runs did, and
only take this on if you're confident you can leave it in a complete, compiling, tested state —
a half-built new entity/migration/screen left for the user to find in the morning is worse than not
starting it. If you start it, use the JIRA workflow to open its own ticket (don't fold it into
OC-231) and note that ticket number in the session log.

## Explicitly BLOCKED — do not attempt these

Genuinely gated on decisions nobody but the user can make. Log as `BLOCKED` (not `SKIPPED`), same
as this repo's R2-4.0.2/R2-4.2 precedent — do not invent business policy and ship it silently:

1. **Auto-restocking when items run low** — needs real deployment-policy input (trigger rule,
   approver, auto-create a Purchase Requisition?) before it can be designed properly.
2. **Bringing the existing Library feature onto this Inventory system** — deliberately deferred,
   large cross-module migration.
3. **Packaging a stand-alone version of the Inventory module** — architecture/business decision.

## Hard rules (from CLAUDE.md — already loaded; repeated for emphasis)

- **Local dev only.** Never touch/deploy to 172.16.7.209 / 172.17.1.243. Never `git push`. Never
  open a PR. Commit locally only, to `inventory-overnight-2026-09-15` — **do not merge to `main`**;
  another concurrent session may have unmerged work of its own, leave integration to the user.
- **Migration column verification (hard gate):** grep existing migrations for real column names
  before any `INSERT`. Never guess.
- **Never edit an already-shipped migration file.** Add a new forward migration instead.
- **Permission migrations** end with the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block.
- **Component Touch Rule** minus the live browser check — use the compile/test gates above, note
  what needs a manual check in the session log.
- **Commit style:** `OC-231: type(scope): summary` (or the new ticket's number if you opened one
  for the stretch goal), matching `git log` history. One commit per logical slice.
- **Naming:** OneCMS / College Management System · Raster / Raster Images Pvt. Ltd. · SKSCON.

## Progress tracking

Maintain `docs/inventory-management/INVENTORY_OVERNIGHT_SESSION_LOG.md` (create it — this is the
first run). Append one line per completed/blocked/skipped item, real timestamp from `date`:
```
- YYYY-MM-DD HH:MM | <screen/item> | DONE|PARTIAL|BLOCKED|SKIPPED | <note, incl. any assumption made>
```
Include the log update in the same commit as the fix it describes wherever practical.

## Update the dashboard before you stop

Republish the progress artifact so it reflects tonight's actual state, not last session's:
`Artifact({ action: "read", url: "https://claude.ai/artifact/V7nq1aNW5FciuVpp7N7Vx2" })` to get the
current HTML, then edit and republish with the same `url` — update the screen checklist (mark
whatever you finished), the timeline (append your session's events), the progress ring/percentage,
and the "as of" timestamp. Keep the same visual design language already established; don't
redesign it.

## Stop condition

You are the FIRST of three sessions — you are not expected to finish everything. Work until either
you've made solid, real progress on the 13-screen floor and are at a natural stopping point (a full
screen done, compiling/tested/committed), or you hit a genuine hard blocker. Leave the working tree
clean (compiling, tested, committed) — never mid-edit. Leave a clear handoff note at the end of the
session log: what's done, what's in progress, what the midpoint session should pick up next.
