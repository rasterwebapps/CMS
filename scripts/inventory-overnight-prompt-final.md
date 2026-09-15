# Inventory / Stock Management — Overnight Final (~2026-09-16 07:00 IST, 3 of 3)

You are the LAST of three scheduled unattended sessions tonight (kickoff ~21:00, midpoint ~02:00,
**you are the final ~07:00 session**). The user arrives at the office at 10:00 and will continue
in a brand-new session — everything you leave behind (code, commits, session log, the published
dashboard) is what they see first; there is no further scheduled session after you. Nobody is
available to answer questions. Do not stop to ask for input — make the most reasonable call
yourself, write it down, and keep working until the stop condition at the bottom.

## Working directory — read this first

Your working directory is already `/home/raster/Idea Projects/SKSCMS/.worktrees/inventory-overnight`
— a git worktree on branch `inventory-overnight-2026-09-15`. **Do not `cd` to
`/home/raster/Idea Projects/SKSCMS` (the main repo root) and do not touch it** — a different
concurrent Claude Code session may still be active there; leave the main checkout exactly as you
find it. `.jira.env` and `backend/local-dev.p12` are already in this worktree. Before any
`bootRun`, check `ss -ltnp | grep -E ':8080|:8090'` and pick a free port; **kill your own bootRun
process when done with it** — don't leave a stray server running when you finish.

## First: figure out exactly what's already done

1. Read `docs/inventory-management/INVENTORY_OVERNIGHT_SESSION_LOG.md` in full — kickoff's and
   midpoint's entries, and midpoint's handoff note at the end.
2. Run `git log --oneline -30` and `git status` to see the real committed state.
3. Read `docs/inventory-management/DECISION_LOG.md`'s 2026-09-15 entries for full context.

Finish any uncommitted/half-finished work first before moving on. Add a handoff line:
`- YYYY-MM-DD 07:00 | — | RESUMED | — | picking up from midpoint session, see above`

## Scope — finish the mandatory floor first

**1. (mandatory floor, finish this) The remaining Stock Management screens.** Check the session
log for exactly which of the 13 are still outstanding after kickoff + midpoint, and finish them:

Dashboard · Products · Categories · Units of Measure · Brands · UOM Conversion Templates ·
Locations · Storage Racks · Stock Balance · Cycle Counts · Stock Valuation · Goods Receipts ·
Supplier Returns

Same gates as the earlier two sessions (full checklist in
`scripts/inventory-overnight-prompt-kickoff.md` in this worktree if needed): list-screen structural
gate, badge/status/enum audit, mlp-page spacing gate, resizable-column gate where applicable,
operation-wise permission mapping. Compile/test is your correctness gate
(`npx tsc -p tsconfig.app.json --noEmit`, `ng build` for SCSS fixes,
`./gradlew compileJava compileTestJava` + relevant tests) — note anything still needing a manual
light/dark/role click-through in the session log for the user.

**If you finish all 13 with time to spare**, only then: the Quotation Request/Approval/PO stretch
goal (full instructions in the kickoff prompt) — check the log first for whether an earlier session
already investigated or started it. **This is genuinely optional** — a clean, fully-finished
13-screen audit is worth far more at 10:00 than a half-built Quotation feature. If you start it and
can't finish it completely and safely, stop, leave it uncommitted-but-noted or roll back to a clean
state, and say so plainly in the log — don't hand the user a broken build.

## Explicitly BLOCKED — unchanged

Auto-restocking policy, Library→Inventory migration, stand-alone module packaging. Still off-limits
if they come up. Log `BLOCKED`, not `SKIPPED`, if relevant.

## Hard rules — same as the earlier two sessions

Local dev only, never touch 172.16.7.209/172.17.1.243, never push/PR, commit only to
`inventory-overnight-2026-09-15` (**do not merge to `main`** — leave that decision to the user,
who may want to review before integrating with whatever the other concurrent session produced),
migration column verification, never edit a shipped migration, permission migrations end with the
catch-all sync block, Component Touch Rule minus the live check, commit style
`OC-231: type(scope): summary`.

## Progress tracking

Keep appending to `docs/inventory-management/INVENTORY_OVERNIGHT_SESSION_LOG.md`. This is the last
scheduled session — end it with a clear **final summary** covering: what got done across all three
passes tonight, what's still open, what's blocked and why, what needs a human decision, and what
needs a manual light/dark/role click-through in the morning. Also update JIRA:
`bash scripts/jira.sh comment OC-231 "<summary>"` (move to review only if the full mandatory floor
is genuinely done — `bash scripts/jira.sh review OC-231 "<summary>"`).

## Update the dashboard — make this one count

This is the version the user opens at 10:00. `Artifact({ action: "read", url:
"https://claude.ai/artifact/V7nq1aNW5FciuVpp7N7Vx2" })` to get the current HTML, then produce a
genuinely complete, polished final version with the same `url`:
- Screen checklist: every one of the 16 marked accurately (done/pending — there should be very few
  or zero pending if the floor was met).
- Full timeline across all three sessions tonight, not just yours.
- Progress ring at the real final percentage.
- All three phase chips (21:00/02:00/07:00) marked done.
- A clear, short "what to do next" section for the user arriving at 10:00 — anything blocked,
  anything needing a manual check, whether the Quotation stretch goal was touched at all.
- "As of" timestamp updated to your actual finish time.
Keep the same visual design language already established; don't redesign it — this is the fourth
time this exact page has been touched tonight, consistency matters more than a refresh.

## Stop condition

This is the last scheduled session — there is no further follow-up. Work until the mandatory floor
(all 16 screens) is done or you hit a genuine hard blocker, then stop cleanly with the working tree
compiling, tested, and committed. Do not remove this worktree — leave it in place for the user to
review and merge when they're back at 10:00.
