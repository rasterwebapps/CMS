# Purchasing & Suppliers — Verify + Audit (Phase 1 of 4)

You are the FIRST of four unattended, chained sessions running tonight back-to-back (a background
script launches each phase automatically as soon as the previous one exits — you do not need to
wait for a clock time). Nobody is available to answer questions. Do not stop to ask for input at
any point — make the most reasonable call yourself, write it down, and keep working until the stop
condition at the bottom.

## Working directory — read this first

Your working directory is already
`/home/raster/Idea Projects/SKSCMS/.worktrees/purchasing-equipment-overnight` — a git worktree on
branch `purchasing-equipment-overnight-20260922`. **Do not `cd` to
`/home/raster/Idea Projects/SKSCMS` (the main repo root) and do not touch it** — it has a live
`ng serve` (:4200) and `bootRun` (:8080) running that may belong to the user or another session.
`.jira.env` and `backend/local-dev.p12` (both gitignored) are already copied into this worktree —
JIRA and HTTPS boot both work here exactly as in the main checkout.

Local services (Postgres :5435, MinIO :9000-9001, Keycloak :8280-8281) are shared Docker
containers, already running, reachable identically from here. **Before any `bootRun`, check
`ss -ltnp | grep -E ':8080|:8090'`**, pick a free port (`--server.port=NNNN`), and **kill your own
`bootRun` process when you're done with it**.

## Read the plan file first

`docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_PLAN.md` is the single source of truth for
this whole 4-phase run — standing rules, full scope, all four phases, blocked items. Read it in
full before doing anything. This prompt only repeats Phase 1's own slice for convenience.

## JIRA

Continue on **OC-264** (already created and In Progress). `bash scripts/jira.sh comment OC-264
"..."` for progress notes; don't create a duplicate ticket for this scope.

## Your job this phase

**Verify completeness, then audit, the "Purchasing & Suppliers" nav group (13 screens).**
`docs/inventory-management/MILESTONES.md` claims this phase is ✅ Done (last updated 2026-09-15) —
treat that as a claim to verify by reading the actual code, not a fact to trust. Full checklist is
in the plan file's "Phase 1" section — in short:

1. Grep the actual backend controllers/entities and frontend routes for: Suppliers
   (register/approve/manage), Rate Contracts, Tax Rules, Vendor Product Rates, Purchase
   Requisitions, Quotation Requests (RFQ), Wanted List, Purchase Orders (full status lifecycle
   including Force Close), Currency Settings/Exchange Rates, and the Price Comparison / PO Aging /
   PO Cycle-Time reports. Confirm each genuinely exists and is wired end to end (route → component
   → service → controller → repository), not just present in nav config.
2. Run the mandatory CLAUDE.md checkup on all 13 screens (list-screen structural gate, badge/
   status audit, `mlp-*` spacing gate, resizable-column gate if used, operation-wise permission
   mapping) — the full checklist text is in the plan file. Fix what's broken, following the fix
   patterns already established in `docs/inventory-management/DECISION_LOG.md`'s 2026-09-15/16
   entries (read them for the concrete style to match).
3. Log any genuine functional gap (a real missing capability, not a UI defect) in the plan file's
   Phase 3 section as a note, or as `BLOCKED` in its own section if it needs real product input —
   do not silently build new capability tonight, this phase is verify + audit only.

## Hard rules (from CLAUDE.md — already loaded in your context; repeated for emphasis)

- **Local dev only. Production data safety is absolute.** Never touch/deploy to `172.16.7.209` or
  `172.17.1.243`. Never `git push`. Never run a `deploy*.sh` script.
- **Migration column verification (hard gate):** grep existing migrations for real column names
  before any `INSERT`. Never guess.
- **Never edit an already-shipped migration file.** Add a new forward migration instead.
- **Permission migrations** end with the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block.
- **No specialist-review round, no `AskUserQuestion` stops** — waived by the user for this file's
  scope only. All other CLAUDE.md gates still apply in full.
- **Commit style:** `OC-264: type(inventory): summary`, one commit per logical slice, `git add`
  specific files only (never `-A`).
- **Naming:** OneCMS / College Management System · Raster / Raster Images Pvt. Ltd. · SKSCON.
- **No self-run visual verification tonight** — use `npx tsc -p tsconfig.app.json --noEmit` /
  `ng build` / `./gradlew compileJava compileTestJava` + tests as your correctness gate. Note in
  the session log anything that needs a manual light/dark/role click-through.

## Progress tracking

Maintain `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md` (create it — this is
the first phase). Append one line per completed/blocked/skipped item, real timestamp from `date`:
```
- YYYY-MM-DD HH:MM | <screen/item> | DONE|PARTIAL|BLOCKED|SKIPPED | <note, incl. any assumption made>
```
Update the plan file's Phase 1 checkboxes and "Session handoff notes" section (append, don't
overwrite) before you stop.

## Stop condition

Work through Phase 1's full checklist. When it's genuinely done (all 13 screens verified/audited,
compiling, tested, committed), stop cleanly — the next phase (Phase 2: bulk demo data for this
same nav group) starts automatically right after you exit. If you run out of the checklist early,
do not invent extra scope — just stop; the background driver moves on regardless. Leave the
working tree clean (compiling, tested, committed) — never mid-edit.
