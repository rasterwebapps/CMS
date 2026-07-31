# R2 Autonomous Session — Night Kickoff (2026-07-28 21:15 IST)

You are running fully autonomously and unattended overnight. Nobody is available to
answer questions tonight. Do not stop to ask for input at any point — make the most
reasonable call yourself, write down what you assumed, and keep working.

## Scope and priority order

Source of truth for what to build is `docs/RELEASE_2_MILESTONES.md` — read it first.
Work top to bottom in this order, completing one milestone before starting the next
wherever practical:

1. **R2-M4 Hostel Management leftovers** (already specialist-scoped 2026-07-22, most
   detailed spec of anything on this list):
   - R2-M4.3 Mess Management (R2-4.3a)
   - R2-M4.4 Hostel Attendance & Leave (R2-4.3b, R2-4.3c)
   - R2-M4.5 Frontend (R2-4.4, all the sub-bullets under it)
   - R2-M4.6 Tests & Docs (R2-4.5, R2-4.6)
   - **Explicitly SKIP R2-4.0.2 and R2-4.2** (hostel-fee mid-course conversion
     recalculation). These are blocked pending a human decision on which billing
     mechanism to target — see the "Finding 2026-07-22" callout in the milestone
     doc under R2-M4.0. Do not attempt a workaround or guess at a mechanism. Leave
     their checkboxes unchecked and log them as `BLOCKED` (not `SKIPPED`) in the
     session log — this is a real open decision, not deprioritized work.
2. **R2-M1: Lab Safety & Compliance**
3. **R2-M2: Communication & Portals** — note R2-2.5 (Parent Portal) implies a new
   `ROLE_PARENT`. Per CLAUDE.md, role *assignment* is DB-only — you don't create or
   hardcode role rows, you only reference the role name in `@PreAuthorize`/route
   guards the same way existing roles are referenced in code.
4. **R2-M5 through R2-M12**, in that numeric order, as far as you get.

This is a large amount of work — realistically more than one overnight window can
finish. That's expected. Work the priority list top to bottom and leave the repo in
a clean, committed, building state at whatever point you stop.

## Hard rules (from this repo's CLAUDE.md — already loaded in your context; the
ones most likely to bite tonight, repeated for emphasis)

- **Local dev only.** Never touch, deploy to, or run migrations against the production
  servers (172.16.7.209 / 172.17.1.243). Never run `git push`. Never open a PR. Commit
  locally to `main` only.
- **Migration column verification (hard gate):** grep existing migrations for the
  exact target table's real column names before writing any `INSERT`. Never guess.
- **Never edit an already-shipped migration file.** If something from tonight needs
  correcting after it's committed, add a new forward migration instead.
- **Permission migration pattern:** every migration inserting permissions ends with
  the DEV_ADMIN/SUPPORT_ADMIN catch-all sync block (see V129/V172 for reference).
- **Operation-wise permission mapping:** every distinct button/operation gets its own
  dedicated permission, named `<MODULE>_<SCREEN>_<OPERATION>`, matching existing
  naming for that module. Never share one permission across two operations.
- **Master screen uniqueness validation:** every new master form gets the real-time
  async uniqueness check (`uniqueFieldValidator` + a `/name-exists` endpoint).
- **List-screen structural gate:** paginator inside `.content-card.mlp-table-card`,
  right after an inner `.table-wrapper`; `matSort` fully bound
  (`matSortActive`/`matSortDirection`/`(matSortChange)`); sort keys must be real,
  directly-queryable entity fields, not DTO-only/joined ones.
- **Component Touch Rule** applies to anything you modify — **except** the live
  browser visual check: nobody is here to look at a screen tonight. Use
  `./gradlew compileJava compileTestJava` plus the relevant unit/controller tests,
  and `ng build` / `npx tsc -p tsconfig.app.json --noEmit`, as your correctness gate
  instead. For anything you can't visually confirm, note it in the session log as
  "needs manual light/dark + role check."
- **JIRA workflow:** for each distinct feature slice, run
  `bash scripts/jira.sh create "Title" "Description" "onecms"` then
  `bash scripts/jira.sh start OC-XXX`. When that slice is functionally complete and
  committed, run `bash scripts/jira.sh review OC-XXX "one-line summary"`. Don't batch
  unrelated features under one ticket — mirror how OC-87/88/89/90 were scoped.
- **Commit style:** `OC-XXX: type(scope): summary`, matching `git log` history. One
  commit per logical slice — don't squash the whole night into a single commit.
- **Naming conventions:** App = OneCMS / College Management System. Company = Raster
  / Raster Images Pvt. Ltd. Client = SKSCON / SKS College Of Nursing.

## Specialist-review substitute

R2-M1, R2-M2, and R2-M5 through R2-M12 have never been through CLAUDE.md's mandatory
Partner Mode specialist round — only the one-paragraph milestone bullets exist as
spec. Since nobody is here to answer specialist questions tonight, don't skip making
the decisions — just make the most reasonable one yourself (favoring reuse of
existing patterns: Faculty/DesignationMaster instead of new Staff entities, the
existing Finance pipeline instead of parallel fee systems, the existing permission-tier
system, etc.) and **write the decision and its rationale into the session log** so it
can be reviewed and reversed in the morning if wrong. Treat the milestone bullets as
directionally correct but not gospel — if a bullet is genuinely ambiguous, pick the
interpretation closest to an existing shipped pattern elsewhere in the app.

## Progress tracking — do this continuously, not just at the end

Maintain `docs/R2_AUTONOMOUS_SESSION_LOG.md` (create it if this is the first run).
Every time you complete, partially complete, block, or explicitly skip a task, append
one line, using a real timestamp from `date`:

```
- YYYY-MM-DD HH:MM | <milestone item id, e.g. R2-4.3a> | DONE|PARTIAL|BLOCKED|SKIPPED | <ticket ID> | <one-line note, incl. any assumption made>
```

Also tick the actual checkboxes in `docs/RELEASE_2_MILESTONES.md` as items complete,
in the same style already used there. Include the log-file update in the same commit
as the feature it describes wherever possible.

## Stop condition

Keep working until either you run out of well-defined next steps in the priority
list above, or you hit a genuine hard blocker you can't resolve safely (missing
infra, a requirement with no reasonable default). In that case, log it clearly in
the session log with a `BLOCKED` entry and stop cleanly — don't leave the working
tree mid-edit, uncompiled, or with a half-finished migration.
