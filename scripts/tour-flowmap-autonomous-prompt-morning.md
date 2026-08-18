# Tour + Flow Map Rollout — Morning Continuation (2026-08-19 03:00 IST)

You are running fully autonomously and unattended in the middle of the night.
Nobody is available to answer questions. Do not stop to ask for input at any
point — make the most reasonable call yourself, write it down, and keep
working. This is a **continuation** of a session that ran at 21:00 on
2026-08-18 — it may or may not have finished the whole task.

## First: figure out where the night session left off

1. Read `docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md` in full — the Progress
   Log section at the bottom has the night session's last entries, and the
   Screen Checklist shows which boxes are already ticked.
2. Run `git log --oneline -30` to see what actually got committed overnight —
   cross-check against the log; if the log claims something DONE that isn't
   in git history, treat it as not done and redo/verify it.
3. Read `frontend/src/app/shared/tour/README.md` — same recipe as last night,
   still the only spec you need for the per-screen mechanics.

## Then: resume exactly where it stopped

Same priority order as the night session: **finish all of Phase 1 (Flow Map
only, for every screen already marked `Tour:Y`) across every module before
touching Phase 2 (build Guided Tour + Flow Map from scratch for `Tour:N`
screens)** if Phase 1 isn't fully done yet. If Phase 1 is complete, continue
Phase 2 module-by-module in checklist order from wherever it stopped.

All the same hard rules apply as the night session (repeated from CLAUDE.md,
most relevant to this task):

- **Local dev only** — never touch/deploy to 172.16.7.209 or 172.17.1.243,
  never `git push`, never open a PR. Commit locally to `main` only.
- Reuse `tour.service.ts` / `tour-button.component.ts` /
  `tour-panel.component.ts` as-is — never restructure them.
- `npx tsc -p tsconfig.app.json --noEmit` (+ `ng build` if you touched the
  shared `FlowMapIcon` union) as your correctness gate — nobody is here for a
  live-browser check; note screens that still need one in the log.
- JIRA: parent is **OC-139**. Continue using per-module sub-tickets
  (`bash scripts/jira.sh create/start/review`) — check `git log` / JIRA for
  tickets the night session already opened before creating duplicates for a
  module already in progress.
- Commit style `OC-XXX: feat(tour): <module> — ...`, one commit per module
  (or per screen if large).
- Keep updating `docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md` continuously —
  checkboxes + a timestamped Progress Log line per screen, not just at the
  end.

## Stop condition — this is the last currently-scheduled autonomous session

Keep working until every screen in the checklist is DONE/SKIPPED/BLOCKED, or
you're at genuine risk of running out of context/tokens. If you finish
everything: write a final Progress Log summary line noting full completion,
double-check `npx tsc -p tsconfig.app.json --noEmit` is clean on the final
state, and stop — nothing further to schedule.

If you **don't** finish everything by the time you must stop: this is fine,
it was a large amount of work (up to ~85 screens) for two overnight windows.
Leave the repo in a clean, committed, building state at whatever point you
stop (same rule as always — no half-wired screen, no failing `tsc`). Write a
clear, precise final Progress Log line stating exactly which screens remain
and in what order, so a human (or a newly-scheduled session) can pick the
checklist back up without re-deriving anything. Do not attempt to schedule
another cron session yourself — that decision is for the user to make when
they're back, since it involves the crontab file which Claude cannot modify
in an unattended run.
