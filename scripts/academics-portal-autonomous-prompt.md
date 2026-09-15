# Academics + Portals Autonomous Session

You are running fully autonomously and unattended overnight (2026-09-15 ~21:00 IST
through 2026-09-16 10:00 IST). The user returns to the office at 10:00 IST sharp and
picks up in a brand-new interactive session — nobody is available to answer
questions before then. Do not stop to ask for input at any point: make the most
reasonable call yourself, write down what you assumed, and keep working.

This prompt fires multiple times tonight (21:00 already ran live in the interactive
session instead of waiting for cron — see the log; 00:00, 02:30, 05:00, 07:30, 09:30
remain scheduled, each a completely separate process). A prior overnight run on this
project hit a session/usage limit only ~28 minutes in and stopped abruptly with no
warning. **Never assume you have hours left to work with** — commit and update the
log/report after every single completed unit of work, not just at natural stopping
points, so a sudden cutoff loses at most a few minutes, not the whole session.

## Status as of the 21:00 session (read the log for full detail before doing anything)

Items 1-6 and 8-14 are done/verified with real evidence. **Item 7 (Special Classes) is
genuinely BLOCKED**, not just unstarted: its self-service request path requires a real
Keycloak login for a faculty member, and none exists in this local environment (only
backend `Faculty` entity rows, no Keycloak users) — provisioning one is real auth
plumbing, not a same-session seeding task. **Do not re-attempt this blindly** on a
later checkpoint hoping it resolves itself; if you have a genuinely new idea for how
to unblock it without creating Keycloak users (e.g. a documented, safe way to
authenticate as a faculty identity), try it once and log the outcome either way, then
stop trying it again on later checkpoints regardless of outcome — repeatedly
re-attempting a known blocker wastes a checkpoint that could do something useful.

Per this file's own portal-gating rule below ("only once every item 1-14 is
genuinely Verified"), **item 7 staying blocked means the portals (15/16) stay out of
scope tonight** — don't start them just because time remains. If you reach a
checkpoint with nothing left to do (7 still blocked, 1-6/8-14 already verified, no
new bugs found on a fresh look), the right move is to say so plainly in the log and
stop cleanly rather than manufacture busywork or lower your own bar for "Verified."

## Step 0 — always do this first, every firing

1. Read `docs/ACADEMICS_PORTAL_AUTONOMOUS_SESSION_LOG.md` in full — the timestamped
   record of everything done, blocked, or skipped so far.
2. Run `git log --oneline -30` and `git status` — confirm what's actually committed.
   If the working tree has uncommitted or half-finished changes, finish that slice
   (get it compiling, tested, committed) before starting anything new.
3. Run `date` to know how close you are to 10:00 IST. If it's this firing's job to
   be the last one before 10:00 (check: is this the 09:30 firing, or did an earlier
   firing not happen?), stop starting new large slices by ~09:40 and spend the rest
   of the time finalizing — see "Final firing" below.

## Scope and priority order

Work top to bottom. Every item in this list **already exists and is routed** —
this is a completeness/QA/data-population pass, not greenfield build, for items 1-14.
Do not skip straight to "looks fine" without actually reading the service layer and
trying real flows through it.

1. Skeleton Builder — `frontend/.../timetable/skeleton-builder`
2. Lab Schedules — `frontend/.../lab-schedule` (list/new/edit)
3. Timetable Draft Review — `frontend/.../timetable/timetable-draft-review`
4. Time Table — `frontend/.../timetable/timetable-view`
5. Resource Timetable — `frontend/.../timetable/resource-timetable-grid`
6. Faculty Absence Marking — `frontend/.../timetable/faculty-absence`
7. Special Classes Scheduling & Approvals — `frontend/.../timetable/special-classes/{my-requests,approval-queue}`
8. Swap Staff Sessions — `frontend/.../timetable/staff-session-swap`
9. Escort Duties — `frontend/.../escort-rotation/my-escort-duties`
10. Attendance — `frontend/.../attendance` (+ mark)
11. Progress Report — `frontend/.../timetable/progress-report`
12. Manage Exams + Exams Scheduling — `frontend/.../examination/{examination-list,examination-form}`
13. Results — `frontend/.../examination/exam-result-list`
14. Student Promotion — `frontend/.../student-promotion`

Then, **only once every item 1-14 is genuinely `Verified` (not just started)**:

15. Student Portal — greenfield. No `ROLE_STUDENT`-scoped routes exist today.
16. Parent Portal — greenfield. No `ROLE_PARENT` exists today. R2-M2 scoped a
    Parent Portal once (see `docs/RELEASE_2_MILESTONES.md`) but it was never built.

These two are a stretch goal, not the priority — realistically item 1-14 alone is
substantial. If you reach them, treat every real design decision (auth model, what
data a student/parent can see, how `ROLE_STUDENT`/`ROLE_PARENT` gets wired) as a
**specialist-review substitute**: make the safest, most conservative call favoring
existing patterns (reuse the existing permission-tier system; default to read-only,
self-scoped views — a student sees their own attendance/results/timetable, nothing
else; a parent sees their linked ward's, nothing else) and log the decision + full
rationale for human review in the morning. This is genuinely higher-stakes than
items 1-14 (new end-user-facing data exposure) — do not improvise around auth/data-
visibility without writing down exactly what you decided and why.

## What "done" means for each of items 1-14

For each screen, in order, do all of the following before moving to the next:

1. **Read the real code** — component, service, backend controller/service, entity,
   and existing tests. Understand what it actually does today, not what the name
   implies.
2. **Fix real bugs you find**, respecting every hard gate in `CLAUDE.md` (migration
   column verification, never edit a shipped migration, permission migration
   pattern, operation-wise permission mapping, master uniqueness validation,
   list-screen structural gate, resizable-column cell markup gate, `mlp-*` spacing
   gate, batches-only-via-Capacity-Auto-Plan). Grep for the exact patterns CLAUDE.md
   describes before writing any fix.
3. **Seed at least 50 realistic records/transactions** for the screen, going through
   the real backend service/API layer (reuse and extend `scripts/seed_demo_data.py`
   where it already has a relevant section — it already seeds a handful of
   examinations/exam-results/attendance rows; extend those to 50+, and add sections
   for the screens it doesn't cover yet) — **not raw SQL inserts**, so the same
   validation/business rules a real user would hit are actually exercised. Data
   must reflect SKS College of Nursing's real domain (INC syllabus subjects,
   B.Sc./M.Sc./GNM programs, real nursing labs/clinical venues) exactly like the
   existing seed data does — no placeholder/lorem content.
4. **Exercise the functionality end to end** — not just "record exists," but the
   actual workflows the screen supports (e.g., for Special Classes: request →
   approve → reject; for Faculty Absence: mark absent → substitute apply; for
   Escort Duties: assign → complete; for Student Promotion: run promotion → verify
   the resulting enrollment/section change). Write or extend backend
   unit/integration tests covering these flows if coverage is thin — this repo has
   a 90% JaCoCo gate, treat that as the bar. Run `./gradlew compileJava
   compileTestJava test` (scoped to the touched modules if the full suite is slow)
   and `npx tsc -p tsconfig.app.json --noEmit` as your correctness gate.
5. **No live browser tonight** — nobody is here to look at a rendered screen. Note
   anything that needs a morning light/dark + role visual check instead of
   pretending it was checked.
6. **Mark the screen's true status** — `Queued` → `In Progress` → `Seeded` (data in,
   not yet fully exercised) → `Verified` (bugs fixed, 50+ real records, workflows
   exercised, tests green) or `Blocked` (a genuine open decision, not a
   deprioritization — same distinction R2's session log used). Never mark
   `Verified` if it isn't true — the user is relying on this report being honest,
   not optimistic.

## JIRA + commit conventions (same as this repo's established pattern)

- `bash scripts/jira.sh create "Title" "Description" "onecms"` then
  `bash scripts/jira.sh start OC-XXX` per distinct feature/fix slice — don't batch
  unrelated screens under one ticket.
- When a slice is functionally complete and committed:
  `bash scripts/jira.sh review OC-XXX "one-line summary"`.
- Commit style: `OC-XXX: type(scope): summary`, one commit per logical slice.
- **Local dev only.** Never `git push`. Never open a PR. Never touch or deploy to
  any remote/production server. Commit locally only, per this repo's CLAUDE.md.
- Naming: OneCMS / College Management System, Raster / Raster Images Pvt. Ltd.,
  SKSCON / SKS College Of Nursing.

## Progress tracking — do this continuously, not just at the end

After every completed unit of work (a fix, a batch of seed data, a test pass),
append one line to `docs/ACADEMICS_PORTAL_AUTONOMOUS_SESSION_LOG.md`:

```
- YYYY-MM-DD HH:MM | <item, e.g. "06 Faculty Absence Marking"> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket ID> | <one-line note, incl. any assumption made, incl. real record count>
```

## Live progress report — redeploy after every checkpoint (and ideally more often)

A dashboard Artifact is already published at:

**https://claude.ai/code/artifact/b9a5c7f4-35ac-4f20-a3cb-d0cd24df94d3**

Before ending this firing (and after any major chunk of work within it):

1. Call `Artifact` with `action: "read"` and that `url` to get the current page's
   HTML content.
2. Edit it in place — update the "Report as of" timestamp, the 4 overview stats
   (screens/portals verified out of 16, total records seeded tonight, bugs found &
   fixed, blocked items), each screen/portal card's status pill (`queued` /
   `progress` / `verified` / `blocked` classes already exist in the CSS — reuse
   them; add a `seeded` state styled like `progress` if useful), each card's
   `0/50` progress bar and note text (one real sentence — what was actually done,
   not "in progress"), the checkpoint rail (`pending`/`done` dot classes on each
   `.tick`, matching which checkpoints have actually fired), and append a new
   `.log-entry` div to the `#log` section (newest last, matching the session log)
   for everything meaningful this firing did.
3. Save the edited HTML to a local file and call `Artifact` again with
   `action: "publish"`, the same `file_path`, and `url` set to the URL above so it
   redeploys in place — **do not** omit `url` or you'll create a second, separate
   artifact and the user's saved link will go stale.
4. Every number on the page must be real — the count of records actually created,
   the actual bug count, actual blocked items. Never round up or mark something
   `Verified`/checked-off there that the session log doesn't also show as done.

## Final firing (09:30, or whichever firing ends up being last before 10:00)

Stop starting new large slices by ~09:40 IST. Spend the remaining time:

1. Get the working tree to a clean, fully compiling, fully committed state —
   never leave a mid-edit or non-building state at 10:00.
2. Write a clear final summary as the last entries in the session log: what got
   done across all checkpoints tonight, what's `Verified` vs `Seeded` vs
   `Blocked`, what needs a human decision or morning visual check, and — if you
   reached items 15/16 — a clear flag that the portal auth/data-visibility
   decisions need a specialist-review pass before anyone treats them as final.
3. Do the Artifact redeploy above one last time, with the rail's last tick marked
   `done` and every number final and accurate — this is what the user opens at
   10:00 walking in, treat it as the actual deliverable of the night, not an
   afterthought.
