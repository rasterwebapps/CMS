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
  Currently covers Blood Group, Speciality, Community, Referral Type — add a
  `MasterConfig` entry to extend to the rest.
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

## Rollout plan (OC-250)

`docs/manual-test-cases/` already has 1,184 documented test cases across 138
files — written at feature-completion time but 99% still marked
`NOT TESTED`. That gap (documented, never executed as a gate) is the root
cause behind live support-call failures. The plan is to keep adding Tier-A
specs seeded directly from that catalogue, prioritized by risk:

1. ~~Role & User Management~~ (done — matches the reported failure)
2. ~~Master screens sharing the `uniqueFieldValidator` pattern~~ (done — Blood
   Group/Speciality/Community/Referral Type; extend `MasterConfig` for the rest
   of the module's masters as time allows)
3. High-traffic transaction screens — **partially done**:
   - ~~Attendance / Exam Results~~ (done — `filter-gated-lists.spec.ts`)
   - ~~Enquiry creation~~ (done — `enquiry.spec.ts`)
   - ~~Fee Collection~~ (done, scoped to opening the payment view — see its
     file header for why a real payment submit is deliberately out of scope
     for now)
   - **Still open:** Admission (enquiry → admission conversion,
     `admission-from-enquiry.md`/`admission-completion.md` — another large
     multi-step wizard, not yet traced); a real Fee Collection payment
     submit-and-verify-receipt spec once there's an isolated fee fixture to
     run it against safely
4. Everything else in `docs/manual-test-cases/`, worked through in file order

None of this has run against a real 243 deploy yet (blocked on
`e2e/.env` test credentials — see repo root). Only flip a `**Status:**` line
in `docs/manual-test-cases/*.md` from `NOT TESTED` to `PASS` after it has
actually gone green there — not on "the spec exists and parses."

## Adding a new spec

Pick the next file in `docs/manual-test-cases/`, automate its TC- cases as
Playwright steps against real selectors (read the component's `.html` first —
don't guess), and land it as its own commit/OC ticket so the rollout stays
trackable.
