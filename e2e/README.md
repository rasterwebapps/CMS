# SKSCMS e2e regression suite

Local-only Playwright suite — no GitHub Actions, no cloud CI. Run it via
`scripts/regression-gate.sh <branch>` from the repo root, or directly here
once `.env` is filled in and a build is already deployed to 243.

## One-time setup

```bash
cd e2e
npm install
npx playwright install --with-deps chromium
cp .env.example .env   # fill in real Keycloak test-account creds for 243
```

## Structure

- `tests/route-crawler.spec.ts` — **Tier B**: every static route in
  `frontend/src/app/app.routes.ts`, visited as ADMIN, asserting no 5xx / no
  uncaught JS error / no error string rendered. Extracted live from the route
  table so it can't go stale. This is the "whole application" smoke floor —
  cheap, and it catches the exact bug class that's shipped repeatedly
  (OC-242, OC-237, OC-245: a screen that loads to a visibly broken state).
- `tests/user-role-management.spec.ts` — **Tier A**: a hand-authored deep spec
  automating `docs/manual-test-cases/role-and-user-management.md`
  (TC-RBAC-004/005/010) — the exact reported production failure
  ("unable to create users, roles").
- `tests/masters.spec.ts` — **Tier A**: one parameterized spec covering every
  CLAUDE.md-mandated master screen's create round-trip + real-time
  `uniqueFieldValidator` check (`master-entry-uniqueness-constraints.md`).
  Currently covers Blood Group, Speciality, Community, Referral Type,
  Designation, Institution, Program, Agent, Course (9 masters, 18 tests) —
  add a `MasterConfig` entry to extend to the rest (`codeFieldId` is
  optional for a name-only master like Agent; `fillExtra` fills any one-off
  extra required field, e.g. Program's durationYears or Course's
  rollNumberCode + programId). Scholarship Type, Countries/States/Districts,
  and the rest of Preferences are still open.
- `tests/masters-lifecycle.spec.ts` — **Tier A**: activate/deactivate round-trip
  from the list screen (`master-lifecycle-status-management.md`,
  TC-MASTER-LIFE-001 pattern), via the shared `ConfirmDialogComponent`.
- `tests/filter-gated-lists.spec.ts` — **Tier A**: parameterized regression spec
  for list screens gated behind a mandatory filter dropdown — the exact shape
  of two real shipped bugs (OC-242 Attendance List always 400s, OC-237 Exam
  Results 500s) that the route crawler alone can't catch since it never picks
  a filter value. Covers Attendance and Exam Results; add an entry to extend.
- `tests/enquiry.spec.ts` — **Tier A**: creates a real enquiry end to end.
  Enquiry is the front door of admissions (largest manual-test-case doc in the
  repo) and its form gates submission on a live fee-structure lookup
  (`feeNotFound()`/`totalFees() <= 0` in enquiry-form.component.ts) — the spec
  tries a few programs and fails with a clear message if none resolve a fee
  structure, which is itself a real finding, not just a broken selector.
- `tests/fee-collection.spec.ts` — **Tier A**: verifies the list loads and the
  "Collect" action opens the payment view without erroring. Deliberately does
  NOT submit a real payment — 243 is shared test-server state, not an
  isolated per-run fixture, and a blind financial write there is a mutation
  this spec shouldn't make (see file header for the full reasoning).
- `tests/admission.spec.ts` — **Tier A**: verifies "Complete" on a
  DOCUMENTS_VERIFIED enquiry in the admission-completion queue reaches Create
  Admission cleanly, skipping if none exist. `admission-from-enquiry.md` is
  **stale** — it describes a `/admissions/new` screen that no longer exists
  in `app.routes.ts`; the real path is enquiry -> fee finalization -> document
  submission -> document verification -> admission-completion queue ->
  `/enquiries/:id/convert` (an 800+ line form). Driving an enquiry through
  that whole pipeline with real uploads/approvals to test the actual convert
  submission is deliberately left for a dedicated follow-up — see next.
- `tests/fee-explorer.spec.ts` — **Tier A**: dataset-correctness regression
  spec for `/student-fees` (Fee Explorer), written for the pagination bug
  fixed on the `oc-253-student-fee-self-service` branch (filter dropdowns
  and the "N students" count were derived from the loaded page, not the
  full server-paginated dataset). Grabs a real Program value from the
  actual last page and asserts it survives the round trip — the route
  crawler would NOT have caught this class of bug (screen renders fine,
  200s, just silently shows wrong/incomplete data). **Not yet fully
  red/green-verified** — see its file header and the rollout note below.
- `tests/student.spec.ts`, `tests/faculty.spec.ts` — **Tier A**: real create
  round-trips. Both list screens sort by an auto-generated field unrelated
  to creation order (244 real students on this environment) — a freshly
  created row isn't reliably on page 1, so both search for their new row by
  a field the spec set explicitly (roll number / employee code) rather than
  assuming placement, the way a real user would actually check their save.

## Rollout plan (OC-250)

`docs/manual-test-cases/` already has 1,184 documented test cases across 138
files — written at feature-completion time but 99% still marked
`NOT TESTED`. That gap (documented, never executed as a gate) is the root
cause behind live support-call failures. The plan is to keep adding Tier-A
specs seeded directly from that catalogue, prioritized by risk:

1. ~~Role & User Management~~ (done — matches the reported failure)
2. ~~Master screens sharing the `uniqueFieldValidator` pattern~~ — 9 of them
   done (see `masters.spec.ts` note above); Scholarship Type,
   Countries/States/Districts, and the rest of Preferences still open
3. High-traffic transaction screens — **done, at entry-point depth**:
   - ~~Attendance / Exam Results~~ (`filter-gated-lists.spec.ts`)
   - ~~Enquiry creation~~ (`enquiry.spec.ts`, full real create)
   - ~~Fee Collection~~ (`fee-collection.spec.ts`, scoped to opening the
     payment view — see its file header)
   - ~~Admission~~ (`admission.spec.ts`, scoped to reaching Create Admission
     from the queue — see its file header)
   - **Still open — the full multi-screen pipelines, each its own dedicated
     pass:** a real enquiry -> fee finalization -> document submission ->
     document verification -> admission conversion round trip end to end; a
     real Fee Collection payment submit-and-verify-receipt spec. Both need an
     isolated test fixture rather than writing against shared 243 state, and
     each touches 3-4 large components that deserve their own careful trace.
4. Everything else in `docs/manual-test-cases/`, worked through in file order
   — **in progress.** Done so far: the 9 masters above, Student (create),
   Faculty (create), Fee Explorer (dataset-correctness). **Not started:**
   Library Management, Inventory (~30 files — the largest single chunk),
   Subject/Curriculum Management, Scholarship, Fee Structures,
   Country/Location Master, plus a long tail of lower-value UI-polish docs
   (dynamic theming, column visibility, table sorting alignment, etc.) —
   deprioritize those relative to functional-module coverage.

**Status as of this session:** the whole suite has run for real against 243
(`e2e/.env` filled in with the `devadmin`/`collegeadmin` bootstrap accounts
from `infrastructure/keycloak/cms-realm.json` — no cashier/faculty/student
Keycloak accounts are seeded there, so those role-gated specs stay skipped
until real creds are added). That first real run found 243 itself was stale
— missing OC-242 entirely — and a genuine app bug, OC-252 (fixed:
`/lab-schedules/new` 500'd on every load, `lab-schedule-form.component.ts`
calling `GET /term-instances` with no `academicYearId`, which the backend
has never supported unfiltered). Every spec above **except
`fee-explorer.spec.ts`** is green against a freshly redeployed 243 as of
this session. Still true: only flip a `**Status:**` line in
`docs/manual-test-cases/*.md` from `NOT TESTED` to `PASS` after
re-confirming green against a *specific* run — 243's state can drift again
the same way it did before, and did again mid-session (see next).

**Picking this back up — read this first:**
1. **`fee-explorer.spec.ts` needs a real run once the fee-explorer fix
   ships.** That fix (filter dropdowns/count reading the loaded page
   instead of the full dataset) lives on branch
   `oc-253-student-fee-self-service`, not yet merged to `main` or deployed
   anywhere. On 243 right now (pre-fix) the deep test **skips** rather than
   fails — the bug itself makes the footer always show exactly "25
   students" (the page size), which fools the test's own `total > 25`
   precondition. Once that branch merges and 243 is redeployed, re-run this
   spec specifically to get a real result.
2. **This repo has multiple concurrent Claude Code sessions sharing one
   working directory.** Twice this session another session's branch
   silently became `HEAD` mid-turn and a commit landed on it by mistake
   (cherry-picked onto `main` both times, nothing lost). **Always
   `git branch --show-current` before editing or committing any `e2e/`
   file**, and `git checkout main` first if it's not already there — a
   stale branch can also make a just-edited file look reverted when you
   re-read it (harmless; the real content is on `main`).
3. Run `scripts/regression-gate.sh <branch>` or `cd e2e && npx playwright
   test` directly once `.env` is filled in — see "One-time setup" above.
   `dev.raster.in:212` (the documented URL) wasn't reachable from this
   agent's network; `https://172.17.1.243:8443` (direct IP) worked fine as
   a substitute — try the documented URL first from a machine with real
   LAN/VPN access, since it's the intended path.

## Adding a new spec

Pick the next file in `docs/manual-test-cases/`, automate its TC- cases as
Playwright steps against real selectors (read the component's `.html` first —
don't guess), and land it as its own commit/OC ticket so the rollout stays
trackable.
