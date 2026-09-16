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

## Rollout plan (OC-250)

`docs/manual-test-cases/` already has 1,184 documented test cases across 138
files — written at feature-completion time but 99% still marked
`NOT TESTED`. That gap (documented, never executed as a gate) is the root
cause behind live support-call failures. The plan is to keep adding Tier-A
specs seeded directly from that catalogue, prioritized by risk:

1. ~~Role & User Management~~ (done — matches the reported failure)
2. Master screens sharing the `uniqueFieldValidator` pattern
   (`master-entry-uniqueness-constraints.md`, `master-lifecycle-status-management.md`)
3. High-traffic transaction screens: fee collection/finalization, admission,
   enquiry, examination results, attendance
4. Everything else in `docs/manual-test-cases/`, worked through in file order

As each spec goes green against a real 243 deploy, flip the corresponding
`**Status:**` line in the matching `docs/manual-test-cases/*.md` file from
`NOT TESTED` to `PASS`, so that catalogue becomes a live, trustworthy source
of truth instead of a write-once artifact.

## Adding a new spec

Pick the next file in `docs/manual-test-cases/`, automate its TC- cases as
Playwright steps against real selectors (read the component's `.html` first —
don't guess), and land it as its own commit/OC ticket so the rollout stays
trackable.
