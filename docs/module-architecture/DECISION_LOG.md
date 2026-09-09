# Module Architecture — Decision Log

This file is the append-only chronological record of every scope/architecture decision behind the module-configuration system, so it survives independently of whichever doc or code is being edited at any given moment.

## Rules for this file

1. Append only — never edit or delete a past entry. If a decision is later reversed, add a new entry that says so and references the old one.
2. Every entry needs a date, what prompted it, the decision(s) made, and the impact (what doc/code changed as a result).
3. Cross-update `MODULE_REGISTRY.md`/`OPS_CONFIG_GUIDE.md` in the same change whenever a decision here changes the module taxonomy or config shape.

---

## 2026-09-08 — @Partner specialist round: module taxonomy, deployment model, enforcement shape

**Prompted by:** user request to split the app's menus/screens by module and let an org's deployment be configured to only a subset (their own example: "SKSCON will use all modules, another industry uses only infra, inventory and its related masters and configurations").

**Decisions**, by specialist round:
- **Product Owner:** separate per-deployment (not a shared multi-tenant app) — matches the already-confirmed single-tenant architecture (`docs/inventory-management/DECISION_LOG.md`). Driven by a concrete second org, not speculative cleanup. Module = whole top-level nav group. The existing 12 nav groups needed redesigning into a cleaner taxonomy rather than being used as-is.
- **Frontend Architect:** the enabled-module list is backend-served (frontend fetches it at app init, mirroring how permissions already load). Bundle exclusion "if possible" — see the reconciliation entry below. Module config is ops-level only for now, no in-app admin screen.
- **Backend Architect:** the backend enforces module gating on the API, not just the frontend nav. Config lives in `application.yml` for now; LDAP-driven config noted as a possible future direction (not built). The module registry is backend-defined; the frontend fetches it rather than keeping its own copy of which modules exist.
- **DBA:** no migration to normalize `permissions.category` — the module ⇄ permission-code mapping lives in backend Java (`ModuleRegistry.java`), not the DB. Permission codes stay exactly as they are; the user explicitly approved introducing new nav items/regrouping as needed to get a clean taxonomy.
- **QA:** module gate always wins over an individual permission grant (AND logic, not OR) — a disabled module blocks access even for a role that holds the underlying permission. Module dependencies are validated at startup, not just documented (initially proposed as "document only," the user chose the stricter option). Full e2e coverage across module combinations was the QA round's initial choice, but see the "test scope descoped" entry below — no e2e framework existed in this repo to build that on.
- **Security:** the enforcement point was initially decided as "a global interceptor/filter" — see the "enforcement point" reconciliation entry below for why this was changed after investigation. Fail-fast startup validation on a bad module config. A disabled module returns a distinct `MODULE_NOT_ENABLED` code, not a generic 403.
- **Documentation:** a new `docs/module-architecture/` folder (this folder), mirroring the `docs/inventory-management/` convention. A short ops runbook. LDAP-driven config recorded as a forward-looking note only.

**Impact:** became the basis of the implementation plan; `MODULE_REGISTRY.md` and `OPS_CONFIG_GUIDE.md` implement it.

---

## 2026-09-08 — Enforcement point: extend `PermSecurityBean`, not a new interceptor/filter

**Prompted by:** the Security specialist round's initial pick of "a global interceptor/filter" for backend module enforcement, before investigating how this app actually does authorization.

**Finding confirmed in code:** this app has no interceptor/filter infrastructure for authorization at all — every permission check goes through `@PreAuthorize("@perm.has('CODE')")`, a SpEL bean (`backend/src/main/java/com/cms/config/PermSecurityBean.java`), used at 812 call sites. `SecurityConfig`'s `SecurityFilterChain` only enforces authentication, not per-route authorization. The one existing `Filter` in the app (`RateLimitFilter`) is unrelated (IP rate limiting).

**Decision:** extend `PermSecurityBean.has()`/`hasAny()` itself to resolve a permission code's module (via `ModuleRegistry`) and check it's enabled before evaluating the permission, throwing `ModuleNotEnabledException` when every code offered is module-gated off. Zero changes needed to the 812 existing `@PreAuthorize` annotations. A brand-new URL-based filter would have needed its own URL→module registry to independently track 246 routes, duplicating and risking drift from the permission-code-based mapping.

**Verification:** the design's one real risk — whether an exception thrown from inside a `@PreAuthorize` SpEL evaluation actually reaches `GlobalExceptionHandler` as itself, rather than being swallowed into a generic `AccessDeniedException` by Spring Security's method-security machinery — was verified with a full-context integration test (`backend/src/test/java/com/cms/config/ModuleGatingIntegrationTest.java`) against the real `SecurityConfig`, a real controller endpoint, and a real disabled-module scenario. It passes: the `MODULE_NOT_ENABLED` response is returned exactly as designed.

**Impact:** `PermSecurityBean.java`, `GlobalExceptionHandler.java`, `ModuleNotEnabledException.java`, `ModuleNotEnabledResponse.java`.

---

## 2026-09-08 — Bundle exclusion vs. backend-served config: reconciled, Phase 3 deferred

**Prompted by:** the Frontend Architect round's two answers were in tension — "backend-served config" (one universal frontend build, module list fetched at runtime) and "exclude from compiled bundle if possible" (requires the build itself to differ per org) can't both be fully true of the same build artifact.

**Decision:** the backend module registry stays the single source of truth for both. Runtime gating (nav hidden + routes blocked + API blocked) is what actually ships now and is fully functional on its own, in one universal frontend build. True build-time bundle exclusion (splitting `app.routes.ts` into per-module files, per-org `angular.json` build configurations via `fileReplacements` — mirroring the existing `environment.prod.ts` swap) remains a documented, buildable design (see `OPS_CONFIG_GUIDE.md`'s "Future direction" section) but was **not implemented** in this pass — there is no real second org yet that specifically needs a leaner build artifact rather than just the runtime-hidden universal build, so the extra build-pipeline complexity was deferred until one does.

**Impact:** `app.routes.ts` was NOT split into per-module files. `ModuleService`, `module-route-index.ts`, and `module.guard.ts` implement runtime-only gating.

---

## 2026-09-08 — Preferences stays one nav group, clustered by module internally

**Prompted by:** user correction after the taxonomy was first drafted: "lets keep all the masters into the separate Preferences menu - grouped by the modules" — the original draft had proposed distributing Preferences' 26 master-data items out into each owning module's own nav group (e.g. Fee Structures moving into the Finance group).

**Decision:** Preferences remains ONE top-level nav group, exactly as it is today. Its items are internally clustered by owning module (with section comments in `nav-config.ts`) and each item carries its own `modules` tag for filtering, so a disabled module's masters still correctly disappear from Preferences even though the group itself is never module-gated. Four items with no natural single owning module (Designations, Location Master, Number Sequences, Settings) carry no `modules` tag and stay always-visible (core).

**Impact:** `frontend/src/app/core/nav/nav-config.ts`'s Preferences group.

---

## 2026-09-08 — Test scope descoped: no new e2e framework, no new frontend unit-test runner

**Prompted by:** investigation before writing Phase 4 tests found the repo has no e2e testing framework at all (frontend has only `ng test` wired in `package.json`, no Cypress/Playwright/Protractor anywhere), and separately, `ng test` itself doesn't actually run — there is no `test` target in `angular.json` and no karma/jest config, so there is no working frontend unit-test runner in this repo at all, not just a lack of test files.

**Decisions** (both put to the user explicitly, since introducing either piece of infrastructure is a scope decision of its own, not implied by "write tests for this feature"):
1. No new e2e framework was introduced for this feature — QA's original "full e2e coverage across module combinations" was scaled back to backend unit/integration tests only.
2. No frontend unit-test runner was set up either — `ModuleService`/nav module-gating filtering logic has no automated frontend test coverage. Verified manually instead: `npx tsc -p tsconfig.app.json --noEmit` and `ng build` both pass clean.

**Impact:** Test coverage for this feature is backend-only: `PermSecurityBeanTest.java`, `ModuleConfigTest.java`, `ModuleRegistryTest.java`, `ModuleGatingIntegrationTest.java` (14 + 2 tests, all passing). Revisiting either testing gap is a separate, standalone initiative — not scoped to this feature.

---

## 2026-09-08 — Forward-looking notes recorded, not built

**Prompted by:** two things raised during the review that are future directions, not current scope:
1. LDAP-driven module configuration (an alternative to editing `application.yml` per deployment) — raised during the Backend Architect round.
2. This module boundary as a plausible seam for a future OneCMS ⇄ OneBook integration — per the user-supplied `architecture_and_workflow_specifications.md`, which describes OneCMS as one of several decoupled "Operational Apps" (alongside OneStore, OneHIS, OnePharmacy, OneLab) sitting behind a centralized OneBook financial core. The Inventory/Purchasing module's vendor and PO flows are the most likely first candidate if this is ever pursued, since they already have the closest conceptual overlap with OneBook's vendor-master/PO/GRN workflow.

**Decision:** record both here as known future directions. Neither is built, referenced by any code, or implied by the current module registry.

**Impact:** none to code. This entry is the only record of the discussion.

---

## 2026-09-08 — Platform system roles (DEV_ADMIN, SUPPORT_ADMIN) are exempt from module gating

**Prompted by:** user request, "provide permission rights to all modules for devadmin user."

**Finding confirmed in code:** the app already has a mandatory, established pattern (CLAUDE.md's "Permission migration pattern") guaranteeing DEV_ADMIN and SUPPORT_ADMIN hold every permission that exists, regardless of which migration added it — enforced via a DB-level catch-all sync block every permission-adding migration ends with (canonical example: `V129__ensure_devadmin_full_access.sql`). `AppRole.isSystemRole` (`is_system_role` column) is the exact existing flag for these two roles — set `TRUE` only for `DEV_ADMIN`/`SUPPORT_ADMIN` as of `V125__rbac_identity_only_final_pass.sql`'s "final pass," `FALSE` for every other role including `LIBRARIAN` and `collegeadmin`. Module gating, added in this initiative, is a new layer that sits above permission possession and — without an explicit exemption — would have silently broken that "these two roles always have every capability" guarantee for the first time, on any deployment where a module is disabled.

**Decision:** `PermSecurityBean.hasAny()` now resolves the current user's `isSystemRole` (via a new `UserPermissionService.isSystemRole(username)` method) once per call and skips the module-disabled check entirely for a system-role user — they still need the underlying permission itself (which the existing catch-all guarantees they have), just never see `MODULE_NOT_ENABLED`. This is not treated as a new deviation needing a fresh @Partner round: it's a direct, faithful continuation of an already-mandatory, already-documented pattern (extending it to cover a new layer this initiative introduced), not a new product decision.

The frontend mirrors this exemption too, so a system-role user actually *sees* every module in nav (not just retains hidden API access behind a menu that never shows it): `app.ts`'s `moduleAllows()` and `core/modules/module.guard.ts`'s `requiresEnabledModule()` both short-circuit to allow when `PermissionService.isDevAdmin()`/`isSupportAdmin()` — the same computed signals the frontend already exposes for other DEV_ADMIN/SUPPORT_ADMIN-specific behavior.

**Impact:** `UserPermissionService.java` (new `isSystemRole` method), `PermSecurityBean.java` (exemption logic + updated Javadoc), `frontend/src/app/app.ts` (`moduleAllows()`), `frontend/src/app/core/modules/module.guard.ts` (`requiresEnabledModule()`). Three new backend tests: `PermSecurityBeanTest` (`systemRoleUserBypassesModuleGateEvenWhenModuleDisabled`, `systemRoleUserStillNeedsThePermissionItselfEvenIfExemptFromModuleGating`) and `ModuleGatingIntegrationTest` (`systemRoleUserReachesDisabledModuleEndpointSuccessfully`) — all passing; no frontend automated test coverage, per the earlier "test scope descoped" entry (verified via `tsc --noEmit` instead).

---

*Next entry goes here — do not insert above this line.*
