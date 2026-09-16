# Inventory / Stock Management — Overnight Midpoint (~2026-09-16 02:00 IST, 2 of 3)

You are the SECOND of three scheduled unattended sessions tonight (kickoff ~21:00, **you are the
02:00 midpoint**, final ~07:00 wrapping up by 10:00 when the user arrives). Nobody is available to
answer questions. Do not stop to ask for input — make the most reasonable call yourself, write down
what you assumed, and keep working until the stop condition at the bottom.

## Working directory — read this first

Your working directory is already `/home/raster/Idea Projects/SKSCMS/.worktrees/inventory-overnight`
— a git worktree on branch `inventory-overnight-2026-09-15`. **Do not `cd` to
`/home/raster/Idea Projects/SKSCMS` (the main repo root) and do not touch it** — a different
concurrent Claude Code session may still be active there. `.jira.env` and `backend/local-dev.p12`
are already in this worktree. Before any `bootRun`, check `ss -ltnp | grep -E ':8080|:8090'` and
pick a free port; **kill your own bootRun process when done with it**.

## First: figure out exactly what's already done

Before writing any new code:
1. Read `docs/inventory-management/INVENTORY_OVERNIGHT_SESSION_LOG.md` in full — the kickoff
   session's timestamped log of what it did, blocked on, or skipped, and its handoff note at the
   end.
2. Run `git log --oneline -20` and `git status` in this worktree to see the actual committed
   state and whether anything was left uncommitted or mid-edit.
3. Read `docs/inventory-management/DECISION_LOG.md`'s 2026-09-15 entries for full context on the
   location-role gate, the zero-cost Internal Return fix, and the bulk demo-data seeder (incl. the
   real gap found: Issue Request/Transfer lines can't carry a batch/serial number).

If the working tree has uncommitted or half-finished changes, finish that slice first (compiling/
tested/committed) before moving to the next screen.

Add a handoff line at the start of your own session log entries:
`- YYYY-MM-DD 02:00 | — | RESUMED | — | picking up from kickoff session, see above`

## Scope — same priority order as kickoff

**1. (mandatory floor) Continue the Stock Management screen checkup.** 16 screens total, 3 done
before kickoff started tonight (Stock Issue Requests, Loanable Item Issues, Stock Transfers). Check
the session log for which of the remaining 13 the kickoff session finished, and continue with
whatever's left:

Dashboard · Products · Categories · Units of Measure · Brands · UOM Conversion Templates ·
Locations · Storage Racks · Stock Balance · Cycle Counts · Stock Valuation · Goods Receipts ·
Supplier Returns

Same gates as before (already loaded in your CLAUDE.md context, and spelled out in full in the
kickoff prompt if you want the exact checklist — `scripts/inventory-overnight-prompt-kickoff.md`
in this same worktree): list-screen structural gate, badge/status/enum audit, mlp-page spacing
gate, resizable-column gate where applicable, operation-wise permission mapping. Correctness gate
is compile/test only (`npx tsc -p tsconfig.app.json --noEmit`, `ng build` for SCSS fixes,
`./gradlew compileJava compileTestJava` + relevant tests) — nobody can visually confirm tonight.

**2. (stretch, only once the 13-screen floor is fully done) Quotation Request → Approval → PO →
Purchase-against-PO → stock update.** See the kickoff prompt's full instructions on this — check
the session log first for whether kickoff already investigated/started it (and if so, what it
found/decided) before re-investigating from scratch.

## Explicitly BLOCKED — unchanged

Same three items as kickoff (auto-restocking policy, Library→Inventory migration, stand-alone
packaging) — still off-limits, still logged `BLOCKED` not `SKIPPED` if they come up again.

## Hard rules — same as kickoff

Local dev only, never touch 172.16.7.209/172.17.1.243, never push/PR, commit only to
`inventory-overnight-2026-09-15` (don't merge to `main`), migration column verification, never
edit a shipped migration, permission migrations end with the catch-all sync block, Component Touch
Rule minus the live check, commit style `OC-231: type(scope): summary`.

## Progress tracking

Keep appending to `docs/inventory-management/INVENTORY_OVERNIGHT_SESSION_LOG.md` in the same
format. Include the log update in the same commit as the fix it describes wherever practical.

## Update the dashboard before you stop

`Artifact({ action: "read", url: "https://claude.ai/artifact/V7nq1aNW5FciuVpp7N7Vx2" })` to get the
current HTML (kickoff's version), then edit and republish with the same `url` — update the screen
checklist, append your session's timeline events, advance the progress ring/percentage and "as of"
timestamp, move the phase chip for "02:00 midpoint" from upcoming to done and "07:00 final" stays
upcoming. Keep the same visual design language; don't redesign it.

## Stop condition

You are the middle session — not expected to finish everything, but should meaningfully advance
past where kickoff left off. Work until a natural stopping point (compiling/tested/committed) or a
genuine hard blocker. Leave a clear handoff note at the end of the session log for the final
(~07:00) session: what's done, what's in progress, what's left, anything it needs to know to wrap
up cleanly by 10:00.
