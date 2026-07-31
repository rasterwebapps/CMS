# R2 Autonomous Session — Morning Follow-up (2026-07-29 04:00 IST)

A previous autonomous session started last night at 21:15 IST working through
Release 2 (R2) modules. You are its continuation, still fully autonomous and
unattended — nobody is available to answer questions this early either. Do not
stop to ask for input; make the best reasonable call and keep working.

## First: figure out exactly what's already done

Before writing any new code:
1. Read `docs/R2_AUTONOMOUS_SESSION_LOG.md` in full — it's the timestamped log of
   everything the night session did, blocked on, or skipped.
2. Run `git log --oneline -40` and `git status` to see the actual committed state
   and whether anything was left uncommitted or mid-edit.
3. Re-read the checkbox state in `docs/RELEASE_2_MILESTONES.md` — don't trust the
   log alone, cross-check against the actual checkboxes and the actual code.

If the working tree has uncommitted or half-finished changes, finish that slice
first (get it compiling/tested and committed) before moving on to the next item.

## Then: continue the same priority order

Same scope and same priority order as the night session:

1. R2-M4 leftovers (Mess Management, Hostel Attendance & Leave, Frontend, Tests &
   Docs) — **except R2-4.0.2 and R2-4.2, which stay explicitly skipped/blocked**,
   same reason as before (hostel-fee recalculation needs a human decision on the
   billing engine — see the "Finding 2026-07-22" note in
   `docs/RELEASE_2_MILESTONES.md` under R2-M4.0). Do not attempt this.
2. R2-M1: Lab Safety & Compliance
3. R2-M2: Communication & Portals
4. R2-M5 through R2-M12, in numeric order

Pick up wherever the night session's log says it stopped. If a whole earlier
milestone in the list is already fully checked off in `RELEASE_2_MILESTONES.md`,
skip straight to the next incomplete one.

## Same hard rules apply as last night (from this repo's CLAUDE.md)

- Local dev only — never touch/deploy to 172.16.7.209 / 172.17.1.243, never
  `git push`, never open a PR, commit locally to `main` only.
- Migration column verification hard gate: grep existing migrations for real
  column names before any `INSERT`. Never guess.
- Never edit an already-shipped migration file — add a new forward migration.
- Permission migrations end with the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block.
- Operation-wise permission mapping: one dedicated permission per button/operation.
- New masters get the `uniqueFieldValidator` + `/name-exists` pattern.
- List-screen structural gate (paginator placement, matSort binding, real sort
  keys) on every list screen you touch or add.
- Component Touch Rule minus live browser check (nobody's watching a screen) —
  use `./gradlew compileJava compileTestJava` + relevant tests, and `ng build` /
  `npx tsc -p tsconfig.app.json --noEmit`, as the correctness gate. Note anything
  needing a manual light/dark + role check in the session log.
- JIRA workflow: `bash scripts/jira.sh create/start/review`, one ticket per
  feature slice, commit style `OC-XXX: type(scope): summary`.
- Naming: OneCMS / College Management System, Raster / Raster Images Pvt. Ltd.,
  SKSCON / SKS College Of Nursing.
- For any of R2-M1/M2/M5-M12 decisions that would normally need a specialist
  round: make the call yourself favoring existing shipped patterns, and write
  the decision + rationale into the session log for review later.

## Progress tracking — continue the same log

Keep appending to `docs/R2_AUTONOMOUS_SESSION_LOG.md` in the same format:

```
- YYYY-MM-DD HH:MM | <milestone item id> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket ID> | <one-line note>
```

Add a line at the very start of this session marking the handoff, e.g.:
`- YYYY-MM-DD 04:00 | — | RESUMED | — | picking up from night session, see above`

Keep ticking `docs/RELEASE_2_MILESTONES.md` checkboxes as items complete.

## Stop condition

This is the last scheduled session for this run — there is no further follow-up
after this one. Work until you run out of well-defined next steps or hit a real
blocker, then stop cleanly with the working tree compiling/tested and committed,
and a clear final summary appended to the session log covering: what got done
across both sessions, what's still open, what's blocked, and what needs a human
decision or manual visual check in the morning.
