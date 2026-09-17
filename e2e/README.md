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
  200s, just silently shows wrong/incomplete data). **Confirmed green**
  against 243 post-fix. First real run (post-merge) failed on a *test*
  bug, not an app bug: `waitForLoadState('networkidle')` after changing
  the Program filter resolved before the filtered `/explorer` response
  actually replaced the table (this app polls in the background, so
  `networkidle` is unreliable as a "the filter took effect" signal) —
  the assertion caught genuinely stale page-1 rows still in the DOM.
  Verified via direct `curl` against `/student-fees/explorer` that the
  backend filter itself was correct throughout. Fixed by waiting on the
  actual `page.waitForResponse(...)` for the filtered request instead;
  green across 3 repeated runs after the fix.
- `tests/student.spec.ts`, `tests/faculty.spec.ts` — **Tier A**: real create
  round-trips. Both list screens sort by an auto-generated field unrelated
  to creation order (244 real students on this environment) — a freshly
  created row isn't reliably on page 1, so both search for their new row by
  a field the spec set explicitly (roll number / employee code) rather than
  assuming placement, the way a real user would actually check their save.
- `tests/library.spec.ts` — **Tier A**: Book Catalogue (full create ->
  accession-uniqueness-check -> edit -> delete round trip, plus search/status
  filter), Issue Desk (not-found lookup error, then a full issue -> return
  round trip), Journals & Periodicals (full create -> edit -> delete round
  trip), and read-only entry-point checks for Fines / Overdue Books /
  Library Settings / Book Import / RBAC direct-URL access. `library-management.md`
  is stale in several places found while writing this — see the spec's file
  header: real nav labels differ from the doc ("Book Explorer" not "Book
  Catalogue", etc.), the summary-card row (Total/Available/Issued) described
  for every list screen doesn't exist in any of the current templates, and
  the whole 4-tab "Reports" section (Overdue/Fines Summary/Issue
  History/Accession Register) describes a feature that isn't there —
  `/library/reports` is a single-purpose Overdue Books list, scoped to that.
  One real backend rule surfaced and is now asserted directly (not a bug):
  `LibraryBookService.delete()` permanently refuses to delete any book with
  issue history at all, even long after it's been returned — the frontend's
  Delete button only disables while status is literally ISSUED, so the
  Issue Desk round-trip test's throwaway book is left behind in the
  catalogue forever by design (no legitimate app flow removes it).
- `tests/inventory-masters.spec.ts` — **Tier A**, first Inventory slice.
  Item Categories (top-level + nested create, parent-scoped uniqueness in
  both directions, deactivate/reactivate), Units of Measure (live
  auto-uppercase/strip-spaces on the code field, code+name global
  uniqueness, deactivate/reactivate), Products (create with
  category/UOM, code global uniqueness, name unique per-category but not
  globally), Tax Rules (create, name uniqueness), Suppliers (create starts
  unapproved, code uniqueness, approve is a separate step), Rate Contracts
  (create against a supplier, end-date-before-start-date blocked). Found and
  fixed two real bugs this uncovered — see OC-257: Tax Rule and Supplier
  creation both 400'd on every single submission (missing required
  `taxTypeId`/`state` fields no frontend form collected at all). A direct
  check against 243 also found the *entire* Inventory module had zero real
  data despite having shipped (categories/uoms/products/suppliers/locations
  all empty) — every test in this spec is self-contained by necessity.
  **Deliberately deferred, not started this pass** (see the spec's file
  header for full reasoning): the Purchase Requisition -> Purchase Order ->
  Goods Receipt chain (blocked on zero Inventory Locations existing on 243,
  which itself needs a Core Infrastructure Zone/Room first); Stock
  Balance/Transfers/Issue Requests/Cycle Counts; all of Equipment & Asset
  Management, Budgets & Approvals, and Gate Pass & Service Requests; every
  reporting/analytics screen; Category custom attributes and the
  parent-picker cycle-prevention check.

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
   Faculty (create), Fee Explorer (dataset-correctness), Library Management
   (`library.spec.ts`), Inventory catalog + Purchasing masters
   (`inventory-masters.spec.ts` — see its note above; the PR->PO->GRN chain
   and most of the other 4 Inventory nav groups are still open within this
   same module, not just "not started"). **Not started at all:**
   Subject/Curriculum Management, Scholarship, Fee Structures,
   Country/Location Master, plus a long tail of lower-value UI-polish docs
   (dynamic theming, column visibility, table sorting alignment, etc.) —
   deprioritize those relative to functional-module coverage.

**Status as of this session:** the whole suite has run for real against 243
(`e2e/.env` filled in with the `devadmin`/`collegeadmin` bootstrap accounts
from `infrastructure/keycloak/cms-realm.json` — no cashier/faculty/student
Keycloak accounts are seeded there, so those role-gated specs stay skipped
until real creds are added). An earlier run found 243 itself was stale
— missing OC-242 entirely — and a genuine app bug, OC-252 (fixed:
`/lab-schedules/new` 500'd on every load, `lab-schedule-form.component.ts`
calling `GET /term-instances` with no `academicYearId`, which the backend
has never supported unfiltered). `oc-253-student-fee-self-service` (the
Fee Explorer pagination fix) has since merged to `main` and been deployed
to 243; `fee-explorer.spec.ts` is now also confirmed green (see its note
above — the one failure on the post-merge run was a test-timing bug, not
an app regression). `library.spec.ts` (prior session) is green across 2 full
repeated runs. `inventory-masters.spec.ts` (this session) is green across 2
full repeated runs, after fixing two real bugs it found (OC-257: Tax Rule
and Supplier creation both 400'd on every submit — see its README note
above) and redeploying the frontend to 243. **Every spec in the suite is
green against a freshly redeployed 243 as of this session.** Still true:
only flip a `**Status:**` line in `docs/manual-test-cases/*.md` from
`NOT TESTED` to `PASS` after re-confirming green against a *specific* run —
243's state can drift again the same way it did before. Note: 243's Book
Catalogue permanently carries one throwaway "E2E Issue Test Book …" row from
`library.spec.ts`'s Issue Desk round trip — see that spec's file header for
why it can never be deleted (a real backend rule, not leftover mess to clean
up) — and the Inventory module now permanently carries the throwaway
categories/UOMs/products/suppliers/tax rules/rate contracts
`inventory-masters.spec.ts` created (no delete affordance exists for most of
these master types yet, matching the masters convention elsewhere in the
app).

**Picking this back up — read this first:**
1. **This repo has multiple concurrent Claude Code sessions sharing one
   working directory.** Twice in an earlier session another session's
   branch silently became `HEAD` mid-turn and a commit landed on it by
   mistake (cherry-picked onto `main` both times, nothing lost). **Always
   `git branch --show-current` before editing or committing any `e2e/`
   file**, and `git checkout main` first if it's not already there — a
   stale branch can also make a just-edited file look reverted when you
   re-read it (harmless; the real content is on `main`). This also bit a
   `deploy-243.sh` run directly: another session's in-progress, uncommitted,
   broken `document-verification-list.component.ts` (an unrelated
   `filteredCourses`/`filterCourseId` rename mid-flight) got swept into a
   full-directory rsync and failed the Docker build. Fix: `git stash push --
   <their file(s)>` (not a full `git stash`, which would also grab your own
   uncommitted work), deploy, then `git stash pop` to restore it immediately
   after — never just delete/overwrite someone else's in-progress file to
   unblock a deploy.
2. Run `scripts/regression-gate.sh <branch>` or `cd e2e && npx playwright
   test` directly once `.env` is filled in — see "One-time setup" above.
   `dev.raster.in:212` (the documented URL) wasn't reachable from this
   agent's network; `https://172.17.1.243:8443` (direct IP) worked fine as
   a substitute — try the documented URL first from a machine with real
   LAN/VPN access, since it's the intended path.
3. Next up per the rollout plan: finish Inventory (PR->PO->GRN chain needs a
   Location on 243 first — see `inventory-masters.spec.ts`'s file header for
   exactly what that needs) or move to the next module — see item 4 above.

## Adding a new spec

Pick the next file in `docs/manual-test-cases/`, automate its TC- cases as
Playwright steps against real selectors (read the component's `.html` first —
don't guess), and land it as its own commit/OC ticket so the rollout stays
trackable.
