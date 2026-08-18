# Tour + Flow Map Rollout — Night Kickoff (2026-08-18 21:00 IST)

You are running fully autonomously and unattended overnight. Nobody is available to
answer questions tonight. Do not stop to ask for input at any point — make the most
reasonable call yourself, write it down, and keep working.

## The task

Roll out "Take a Tour" (Guided Tour) + "Flow Map" — the pattern built for the
Collect Payment screen (OC-136/137/138) — to every other screen in the app.

**Read these two files in full before writing any code:**
1. `frontend/src/app/shared/tour/README.md` — the exact per-screen recipe
   (shared components to reuse untouched, `TourFlowMap` data shape, wiring
   steps, verification steps). Follow it precisely; don't improvise a
   different shape.
2. `docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md` — the screen checklist (97
   screens across 12 modules) and progress log. The Tour/FM columns in that
   file are a **heuristic pre-survey, not verified** — always `grep
   tourService.register` / `registerFlowMap` on the actual target component
   before trusting the table for that screen, and correct the table if it's
   wrong.

## First: commit the setup scaffold

Before starting any screen, run `git status`. You should see 5 new
uncommitted files created during setup: `frontend/src/app/shared/tour/README.md`,
`docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md`,
`scripts/tour-flowmap-autonomous-prompt-night.md`,
`scripts/tour-flowmap-autonomous-prompt-morning.md`,
`scripts/tour-flowmap-autonomous-run.sh`. Commit these as their own first
commit (`OC-139: docs(tour): add rollout README, checklist, autonomous
session scaffold`) before starting Phase 1 feature work, so there's a clean
checkpoint to diff against.

## Priority order

**Phase 1 first, across every module:** for every screen already marked
`Tour:Y` in the checklist, add a `TourFlowMap` + `registerFlowMap()` call only
(no new Guided Tour needed). This is the fast, low-risk win — do all of these
before starting Phase 2, so a partial night still leaves broad, real value
shipped rather than a few screens fully done and the rest untouched.

**Phase 2 second:** for every screen marked `Tour:N`, build both the Guided
Tour (driver.js steps + `id="tour-*"` anchors) and the Flow Map from scratch,
per the README. Work module by module in the order listed in the checklist
(Overview → Admission → Student → Finance → Academics → Library → Core
Infrastructure → Inventory → Hostel → Reports → Preferences → User
Management), screen by screen top to bottom within each module.

Within both phases, batch sensibly: author content for 3-6 screens in a
module's shared `*.tours.ts` file, wire each screen's component + template,
then run one `npx tsc -p tsconfig.app.json --noEmit`, then one commit per
module (or per screen if a module is large/slow), then move on. Don't let a
single giant uncommitted working tree accumulate — commit as you go so a
context cutoff mid-task doesn't lose finished work.

For `Overview` module screens (Dashboard, My Profile, My Timetable) — these
are lower value/more ambiguous (highly role-variable content, not a typical
list/form screen). Use judgment on whether a Guided Tour adds real value; if
you skip one, log it as `SKIPPED` with the reasoning, don't silently drop it.

## Hard rules (from this repo's CLAUDE.md — already loaded in your context; the
ones most likely to bite tonight, repeated for emphasis)

- **Local dev only.** Never touch, deploy to, or run migrations against the
  production servers (172.16.7.209 / 172.17.1.243). Never run `git push`.
  Never open a PR. Commit locally to `main` only.
- **No schema/migration work is expected for this task** — it's frontend-only
  (Angular components + shared tour service data). If you find yourself
  writing a migration, stop and reconsider — that's a sign of scope creep.
- **Component Touch Rule** applies to every component you touch — **except**
  the live-browser visual check: nobody is here to look at a screen tonight.
  Use `npx tsc -p tsconfig.app.json --noEmit` and `ng build` as your
  correctness gate instead. Note in the progress log which screens still
  "need manual light/dark + role check" — don't skip logging this.
- **Never invent a new shared component or restructure `tour.service.ts` /
  `tour-button.component.ts` / `tour-panel.component.ts`.** They're generic
  and already correct for every screen. Only add to the small `FlowMapIcon`
  union in `tour.service.ts` if a genuinely new icon shape is needed, and
  check `tour-panel.component.ts`'s icon switch first per the README.
- **JIRA workflow:** parent ticket is **OC-139** (already In Progress). Create
  one sub-ticket per module (not per screen — that's too granular) via
  `bash scripts/jira.sh create "Tour+FlowMap: <Module name>" "..." "onecms"`
  then `bash scripts/jira.sh start OC-XXX`, and `bash scripts/jira.sh review
  OC-XXX "one-line summary"` once that module's screens are committed.
- **Commit style:** `OC-XXX: feat(tour): <module> — flow map / guided tour for
  <screens>`, matching `git log` history (see cf216866, 9f0a401b for shape).
  One commit per module (or per screen if large), not one giant commit for
  the whole night.
- **Naming conventions:** App = OneCMS / College Management System. Company =
  Raster / Raster Images Pvt. Ltd. Client = SKSCON / SKS College Of Nursing.

## Progress tracking — do this continuously, not just at the end

Every time you complete, partially complete, block, or skip a screen, append
one line to `docs/TOUR_FLOWMAP_AUTONOMOUS_SESSION_LOG.md`'s Progress Log
section, using a real timestamp from `date`:

```
- YYYY-MM-DD HH:MM | <screen name> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket ID> | <one-line note>
```

Also tick the checkbox for that screen in the **Screen Checklist** section
(and update its Tour/FM markers to `Y` once shipped) as you finish it. Include
the log-file update in the same commit as the screen(s) it describes wherever
practical.

## Stop condition

Keep working until you either run out of well-defined next steps (every
screen in the checklist is DONE/SKIPPED/BLOCKED) or you're genuinely at risk
of running out of context/tokens for this session. In the latter case, finish
whatever screen you're mid-way through to a clean committed state (don't
leave a half-wired screen or a failing `tsc`), write a clear final Progress
Log line summarizing exactly where you stopped and what the next screen in
priority order is, and stop. A second unattended session picks this up at
3 AM using the same checklist and log — it will read this file first to know
where to resume, so precision here matters more than a long narrative.
