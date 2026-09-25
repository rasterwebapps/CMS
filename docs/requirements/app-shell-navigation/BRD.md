# BRD — Application Shell & Navigation (app-shell-navigation)

**System:** OneCMS / College Management System · **Client:** SKSCON / SKS College Of Nursing · **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Executive Summary / Business Objective

OneCMS serves many operationally distinct users (front office, cashier, faculty, hostel warden, librarian, college admin, students, parents) from one application. The shell exists to give every one of them a single consistent frame — the same toolbar, the same navigation mechanics — while showing each person *only* the modules and actions their role actually grants, without a separate build or deployment per role. This keeps onboarding a new user class (or a new module) a configuration change (nav-config entry + DB permissions), not a new application.

## 2. Stakeholders

- **All end users** — every role interacts with the shell as their constant frame of reference across the whole system.
- **College operations leadership (SKSCON)** — cares that staff only see what's relevant to their job, reducing training overhead and accidental misuse of screens they don't own.
- **Raster engineering** — cares that adding the Nth feature module doesn't mean touching N different navigation/guard implementations.

## 3. Business Rules

No `BUSINESS_REQUIREMENTS.md` entries were found that are specific to the shell/navigation chrome itself (grepped the table of contents; none of BR-1…BR-61 target navigation/shell structure directly). The following are derived from the actual shipped code/config rather than an explicit BR:

| ID | Rule | Rationale (inferred) |
|----|------|----------------------|
| BR-SHELL-1 | Menu visibility is permission- and module-gated, never role-name-gated. | Mirrors the DB-only RBAC principle (BR-24) at the UI layer — a menu item checks `permissions[]` codes, never `if (role === 'faculty')`. |
| BR-SHELL-2 | A module gate always wins over a permission a user holds. | A deployment that hasn't licensed/enabled a feature module must hide it completely, even from someone who happens to hold the underlying permission (e.g. carried over from another role). |
| BR-SHELL-3 | Platform system roles (DEV_ADMIN, SUPPORT_ADMIN) always see every module-gated menu entry. | Support/engineering must never be locked out of diagnosing a disabled-module deployment through the UI itself. |
| BR-SHELL-4 | A student's default landing page is their own dashboard (`/student/my-dashboard`), not the generic admin dashboard. | Documented in code: the generic `/dashboard`'s universal widget defaults included widgets a student has no permission for, silently 403ing in the background — the redirect fix is a genuine, shipped business rule now, not just a bug patch. |
| BR-SHELL-5 | Sidenav collapse/expand, pinned state, and theme choice are personal, device-local preferences — never centrally administered or synced. | No backend persistence exists for any of these; all three are `localStorage`-only. |

## 4. Business Process / Workflow

1. A user logs in and lands in the shell; the sidenav and toolbar render immediately, but every menu entry is filtered live against their resolved permission set and their deployment's enabled modules before it's ever shown.
2. The user navigates via the sidenav (desktop: pinned or hover-peek icon rail; mobile: overlay drawer) or the in-menu search; the currently active section auto-expands and all siblings collapse, keeping the menu a clear "you are here" map rather than a list the user has to re-scan every time.
3. Route-level guards independently re-verify permission on every navigation (not just menu visibility) — a user cannot navigate to a screen by typing its URL even if it's hidden from their menu, and vice versa a visible menu entry is guaranteed to be a reachable, authorized route.
4. Selected admission-workflow menu items surface a live count badge (e.g. how many enquiries are pending), giving front-office/admin staff an at-a-glance workload signal without opening each screen.

## 5. Success Criteria

Not formally defined — inferred from feature completeness: every one of the ~250 routed screens in the application is reachable through this single shell/nav mechanism with no bespoke per-module navigation frame; the same mechanism has scaled from the original M1.3 milestone (a handful of placeholder routes) to the full multi-module system without a rewrite.

## 6. Assumptions & Constraints

- The shell assumes exactly one `app_role` per user (no multi-role sessions) — nav filtering and dashboard defaults are all computed off a single resolved permission/role response.
- Nav structure (`nav-config.ts`) is treated as a single shared source of truth deliberately reused by the Role Management/Permission Tiers screens' permission grouping — changing menu labels/order has a side effect on how permissions are grouped there too.
- No server-side record of a user's UI layout preference (collapse state, theme, expanded groups) exists; switching browsers/devices resets these to default.

## 7. Known Gaps / Deferred

- The dashboard's original placeholder-card landing page has been functionally replaced by the configurable widget system (BR-40); no gap here, just noting the shell's "default destination" content has evolved past its original M1.3 shape (see SRS.md §6).
- No documented business rule governs `WardSwitcherComponent`'s presence in the shell toolbar — out of scope for this module's BR mapping.
