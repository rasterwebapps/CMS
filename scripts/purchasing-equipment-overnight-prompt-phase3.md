# Equipment & Asset Management — Verify + Audit + Bulk Demo Data (Phase 3 of 4)

You are the THIRD of four unattended, chained sessions running tonight back-to-back. Nobody is
available to answer questions. Do not stop to ask for input — make the most reasonable call, write
it down, and keep working until the stop condition at the bottom.

## Working directory — read this first

`/home/raster/Idea Projects/SKSCMS/.worktrees/purchasing-equipment-overnight` — git worktree,
branch `purchasing-equipment-overnight-20260922`. Do not touch the main repo root. Before any
`bootRun`, check `ss -ltnp | grep -E ':8080|:8090'`, pick a free port, kill your own process when
done.

## Read the plan + prior phases' work first

1. `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_PLAN.md` — single source of truth.
2. `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md` + the plan file's
   "Session handoff notes" — verify what Phases 1-2 actually did with `git log` on this branch,
   don't just trust the notes.
3. `docs/inventory-management/DECISION_LOG.md`'s 2026-09-15 "Bulk demo data" entry — same three
   seeder-building lessons apply here (idempotency per-phase not per-global-count, re-query instead
   of trusting a `seedX()` return value on resume, tracking-mode compliance).

## JIRA

Continue on **OC-264**. `bash scripts/jira.sh comment OC-264 "..."` for progress notes.

## Your job this phase (two parts — do both)

**Part A — verify + audit.** `docs/inventory-management/MILESTONES.md` claims Phase 5 (Equipment &
Asset Management) is ✅ Done (last updated 2026-09-15) — verify by reading the actual code, not by
trusting the doc:
1. Confirm Asset register + lifecycle (`IN_USE`/`UNDER_MAINTENANCE`/`RETIRED`/`DISPOSED`),
   Maintenance Schedules + Service Contracts, Depreciation (straight-line, computed live from
   `Asset.purchaseValue`/`purchaseDate`/`usefulLifeMonths`/`salvageValue`), and Disposal/write-off
   genuinely exist end to end.
2. Run the mandatory CLAUDE.md checkup (list-screen structural gate, badge/status audit, `mlp-*`
   spacing gate, resizable-column gate if used, operation-wise permission mapping) on all 4
   screens: Asset Register, Maintenance Schedules, Service Contracts, Depreciation Summary. Fix
   what's broken, matching the fix patterns in `DECISION_LOG.md`'s 2026-09-15/16 entries.

**Part B — bulk demo data.** Extend the same seeder Phase 2 built (or start a sibling one in the
same style if Phase 2 left none reusable) with:
- 15-25 Assets across every status — `IN_USE` majority, a few `UNDER_MAINTENANCE`, a few
  `RETIRED`, 1-2 `DISPOSED` with a real disposal reason/value/date. Realistic purchase
  values/dates/`usefulLifeMonths`/salvage values, varied enough that the Depreciation Summary
  report shows genuinely different book values across categories, not near-identical numbers. Mix
  of assets linked back to a Goods Receipt line (where one exists in seeded/real data) and
  standalone "already-owned, being onboarded" entries — the entity supports both paths, use both.
- Maintenance Schedules against a subset of assets — mix of recurring and one-off, at least one
  overdue (past due date) and one upcoming.
- Service Contracts against a subset of assets — one active, one expiring soon, one already
  expired, so the renewal-reminder concept has real data to show.

Verify with real counts against Postgres:
```
docker exec cms-postgres psql -U cms -d cmsdb -c "select 'assets', count(*) from assets union all select 'asset_maintenance_schedules', count(*) from asset_maintenance_schedules ..."
```
Then confirm the Depreciation Summary report actually renders varied output against this data
(check the controller/service response directly via `curl` against your own `bootRun`, since
nobody can click through the UI tonight).

## Hard rules (from CLAUDE.md — repeated for emphasis)

- **Local dev only. Production data safety is absolute.** Never touch `172.16.7.209` /
  `172.17.1.243`. Never `git push`. Never run a `deploy*.sh` script.
- **Migration column verification:** grep existing migrations for real column names before any
  direct `INSERT`. Never guess. **Never edit an already-shipped migration file.**
- **Permission migrations** end with the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block.
- **No specialist-review / `AskUserQuestion` stops** — waived for this file's scope only.
- **Commit style:** `OC-264: type(inventory): summary`, one commit per logical slice.
- **No self-run visual verification tonight** — use compile/test/`curl` as your correctness gate;
  note anything needing a manual light/dark/role click-through in the session log.

## Progress tracking

Append to `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md`. Update the plan
file's Phase 3 checkboxes and "Session handoff notes" before you stop.

## Stop condition

Work through both Part A and Part B. When genuinely done (audited, seeded, verified by real count
and a real report response, compiling, tested, committed), stop cleanly — Phase 4 (final checkup +
wrap-up) starts automatically. If a real blocker forces an early stop, leave the tree clean and the
handoff note explicit about what's done vs. not.
