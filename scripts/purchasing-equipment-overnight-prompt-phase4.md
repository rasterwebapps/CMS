# Final Checkup, Cross-Report Verification, Wrap-Up (Phase 4 of 4)

You are the FOURTH and LAST of four unattended, chained sessions running tonight. Nobody is
available to answer questions. Do not stop to ask for input — make the most reasonable call, write
it down, and finish up per the stop condition at the bottom.

## Working directory — read this first

`/home/raster/Idea Projects/SKSCMS/.worktrees/purchasing-equipment-overnight` — git worktree,
branch `purchasing-equipment-overnight-20260922`. Do not touch the main repo root. Before any
`bootRun`, check `ss -ltnp | grep -E ':8080|:8090'`, pick a free port, kill your own process when
done.

## Read the plan + all prior phases' work first

1. `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_PLAN.md` — single source of truth.
2. `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md` and the plan file's
   "Session handoff notes" — see what Phases 1-3 actually did. **Do a fresh, independent
   verification, do not just trust their own claims** — this is the exact discipline
   `AUTONOMOUS_OVERNIGHT_PLAN.md`'s Phase 8 re-examinations proved valuable (a prior session's own
   "done"/"blocked" note is not automatically correct).

## JIRA

Continue on **OC-264**. `bash scripts/jira.sh comment OC-264 "..."` with your final summary at the
end — leave the ticket **In Progress** (not resolved), so the user reviews and resolves it
themselves in the morning.

## Your job this phase

1. **Re-check both PO reports (Aging, Cycle-Time) and Price Comparison** render real, varied
   output against Phase 2's seeded Purchase Orders / Vendor Product Rates — hit the endpoints
   directly (`curl`) against your own `bootRun`, since nobody can click through the UI tonight.
2. **Full top-to-bottom re-read of the plan file.** Verify every checkbox in Phases 1-3 actually
   reflects reality — grep the code and spot-check Postgres yourself, don't trust an earlier
   phase's checkmark blindly. Correct any checkbox that's wrong (checked-but-not-actually-done, or
   vice versa) and note the correction.
3. `./gradlew compileJava compileTestJava` and the relevant `com.cms.inventory.*` test classes
   (procurement/receiving/asset/reporting packages at minimum) must be green.
   `npx tsc -p tsconfig.app.json --noEmit` must be clean.
4. **Final real-count verification against Postgres** for every table seeded across Phases 2-3 —
   record the actual counts in the session log and in the plan file's "Session handoff notes".
5. **Optional but nice, do it if time allows:** publish a progress Artifact dashboard summarizing
   tonight's run (screens audited, bugs fixed, row counts seeded) — follow the visual pattern
   `AUTONOMOUS_OVERNIGHT_PLAN.md`'s 2026-09-15 sibling run used (a published dashboard the user can
   open at their desk). If you publish one, record its URL in the plan file's handoff notes.
6. Write a clear, complete final entry in the plan file's "Session handoff notes": what shipped,
   what's still open (if anything from Phase 1/3's BLOCKED items remains genuinely unresolved),
   exact row counts seeded, and anything that needs the user's manual light/dark/role click-through
   before they'd consider this "done" in the UI sense (compile/test-clean is not the same as
   visually verified — say so explicitly, don't imply otherwise).
7. `bash scripts/jira.sh comment OC-264 "<final summary>"`.

## Hard rules (from CLAUDE.md — repeated for emphasis)

- **Local dev only. Production data safety is absolute.** Never touch `172.16.7.209` /
  `172.17.1.243`. Never `git push`. Never run a `deploy*.sh` script. Never merge this branch into
  `main` — leave integration to the user.
- **Never edit an already-shipped migration file.**
- **No specialist-review / `AskUserQuestion` stops** — waived for this file's scope only, tonight
  only.
- **Commit style:** `OC-264: type(inventory): summary`, one commit per logical slice.

## Progress tracking

Final append to `docs/inventory-management/PURCHASING_ASSET_OVERNIGHT_SESSION_LOG.md`.

## Stop condition

This is the last phase — no further phase follows. Once steps 1-7 above are complete (or you hit a
genuine hard blocker you've clearly logged), leave the working tree clean (compiling, tested,
committed, nothing mid-edit) and stop. The user picks this up live in the morning.
