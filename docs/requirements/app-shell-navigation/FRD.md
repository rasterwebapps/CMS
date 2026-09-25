# FRD — Application Shell & Navigation (app-shell-navigation)

**System:** OneCMS / College Management System · **Client:** SKSCON / SKS College Of Nursing · **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Overview

The shell is a single root standalone component (`App`, `frontend/src/app/app.ts` + `app.html`) that renders the Material sidenav/toolbar chrome and hosts every other screen via `<router-outlet>`. Menu structure is declared once in `nav-config.ts` and consumed both by the shell (to render/filter the sidenav) and by the permission-admin screens (to group permissions the same way). Routing and route-level guarding live in `app.routes.ts`.

## 2. Actors & Permissions

The shell itself defines no permission codes of its own — it *consumes* whatever codes each `NavItem`/`NavGroup` lists (each belonging to that item's own feature module) and whatever module codes exist in the module registry. Two shell-relevant checks are hardcoded by role name rather than permission code (an intentional, narrow exception — see Edge Cases):
- `permissionService.isDevAdmin()` / `isSupportAdmin()` — derived from `normalizedRoleName()`, used only for the module-gating bypass (mirrors an identical bypass in the backend's `PermSecurityBean`).
- `permissionService.isRole('student')` — used only by `homeRedirectGuard` to choose the default landing route.

## 3. Screens & UI Behavior

### 3.1 Root Shell (`App`)
- **Toolbar:** app logo/title (falls back to text if the logo image errors — `toolbarLogoError` signal), hamburger (`toggleSidenav`), global search component, ward switcher component, notification bell with unread count (`NotificationService.feed()`), keyboard-shortcuts button, theme toggle, user menu (initials avatar derived from the Keycloak username — first letters of the first two whitespace/`_`/`-`/`.`-delimited parts, or first two characters if only one part).
- **Sidenav — desktop:** two-tier — a fixed icon rail (always visible) plus a second "tray" showing the active group's items. Collapsed rail state (`sidenavCollapsed`) persists to `localStorage['cms_sidenav_collapsed']`. Hovering the rail while collapsed opens the tray as a peek overlay after a 120ms delay; leaving closes it after a 220ms delay (`onRailEnter`/`onRailLeave`) — page content never reflows during a peek.
- **Sidenav — mobile:** `isMobile()` (from `ResponsiveService`) switches the drawer to `over` mode; it auto-closes on every `NavigationEnd`.
- **Group expand/collapse:** `expandedGroups` signal persisted to `localStorage['cms_nav_expanded_groups']`. On every navigation, `syncExpandedGroupToRoute` finds the group owning the new URL, expands only that group, and collapses all others; a small excluded-routes set (`NAV_PREFIX_MATCH_EXCLUDED_ROUTES`, currently `/timetable/capacity-planner`) prevents a route that is a path-prefix of an unrelated group's exact route from wrongly claiming that group.
- **Menu search:** `menuSearch` signal; `/` keyboard shortcut focuses it (suppressed while typing in any input/textarea/select/contenteditable, or while the sidenav is collapsed and not currently peeking); `Esc` clears it. Filtering matches group label OR item label, case-insensitive substring.
- **Theme toggle:** `darkTheme` signal persisted to `localStorage['cms_dark_theme']`; falls back to `window.matchMedia('(prefers-color-scheme: dark)')` when never explicitly set. Toggling adds/removes `dark-theme`/`light-theme` classes on `<html>` and calls `ThemeService.refreshForColorScheme()`.
- **Nav badge counts:** if the caller holds `ENQUIRY_VIEW`, `ngOnInit` fetches `GET /dashboard/summary` and maps its `enquiryFunnel`/`enquiryCollectPaymentEligibleCount` fields onto six specific route badges (Enquiries, Finalize Fee, Collect Payment, Submit/Verify Documents, Complete Admission); fetch errors are silently ignored.
- **Onboarding tour:** registered post-view-init with a ~900ms delay (to let `getBoundingClientRect()` return real values); `TourService.maybeAutoStart('onboarding')` respects a "don't show again" preference; a manual "Start tour" action always replays it regardless of that preference.

### 3.2 Dashboard (landing page, `/dashboard`)
Documented M1.3 baseline (per `docs/manual-test-cases/app-shell.md`): four placeholder metric cards (Total Students, Total Faculty, Specialities, Active Courses, showing "—") plus placeholder Lab Utilization and Recent Activity widgets. Current shipped behavior renders a permission-gated, per-role/per-user configurable widget grid instead (see BR-40 in `BUSINESS_REQUIREMENTS.md`) — that widget system's own screens/endpoints are out of scope for this FRD; only its role as the shell's default destination is noted here.

## 4. Functional Workflows

### 4.1 Nav entry visibility resolution (`App.filteredNavEntries`)
For each top-level `NavEntry`:
1. If it's a `NavGroup`: check `moduleAllows(entry.modules)` first — if none of the group's tagged modules are enabled (and caller isn't DEV_ADMIN/SUPPORT_ADMIN), the entire group is dropped, full stop.
2. Else check the group's own `permissions[]` (if any) via `permissionService.hasAny(...)` — group dropped if none held.
3. Filter the group's `items[]` the same way (module gate, then permission gate) individually.
4. If a search term is active, keep only items whose label matches (or all items, if the group label itself matches).
5. Drop the group entirely if it ends up with zero visible items.
6. A flat `NavItem` (not in a group) goes through the same module→permission→search checks directly.

### 4.2 Route navigation & guarding (`app.routes.ts`)
1. Every protected route composes `withAuth = [authGuard, requiresEnabledModule()]`, or `withPermission(...codes) = [authGuard, requiresEnabledModule(), requiresPermission(...codes)]`.
2. `requiresEnabledModule()` resolves the URL's owning module(s) via `resolveModulesForUrl` (itself derived from `nav-config.ts`'s `modules` tags — the module→route mapping is not duplicated by hand); a URL with no module mapping is always allowed; DEV_ADMIN/SUPPORT_ADMIN bypass entirely.
3. `requiresPermission(...codes)` denies (redirects to `/dashboard`) until `PermissionService.loaded()` is true, then checks `hasAny(...codes)`.
4. The catch-all (`''`, `'**'`) routes mount a permanently-empty `HomeRedirectComponent` guarded solely by `homeRedirectGuard`, which always returns a `UrlTree` (`/student/my-dashboard` for role `student`, else `/dashboard`) — the guard is guaranteed to redirect before the empty component ever renders; it exists only because Angular requires *some* component on a route with `canActivate` (a bare `redirectTo` cannot combine with `canActivate`).

### 4.3 Group active-state / prefix matching
`isGroupActive`/`isNavItemActive`/`syncExpandedGroupToRoute` all compare the current URL (query string and fragment stripped) against each nav entry's `route`. An entry matches on exact equality, or — only when no *other* nav entry has an exact match for that URL — on path-prefix (`url.startsWith(item.route + '/')`), so a detail/sub-page with no menu entry of its own still highlights its logical parent, without a genuinely separate screen ever stealing another screen's highlight.

## 5. API Endpoints

| Method & Path | Purpose | Permission |
|---|---|---|
| `GET /dashboard/summary` | Feeds nav badge counts (enquiry funnel + collect-payment-eligible count) | `ENQUIRY_VIEW` (client-side gate before the call is even made; the endpoint itself belongs to the dashboard/enquiry module, not this one) |

No shell-specific backend controller exists — the shell is a pure frontend composition layer over the `auth-identity` permission API (`/permissions/my`) and each feature module's own APIs.

## 6. Data Model

The shell holds no server-side data model of its own. Its structural "data" is the static `NAV_ENTRIES: NavEntry[]` array in `nav-config.ts` (TypeScript source, not DB-backed) and three `localStorage` keys (`cms_sidenav_collapsed`, `cms_dark_theme`, `cms_nav_expanded_groups`) — all client-local, never synced to the backend.

## 7. Edge Cases & Validation Rules

- A nav item/group with no `permissions` and no `modules` is treated as core and always visible to any authenticated user.
- A module-gated item hidden by module gating is hidden even for a user who holds its underlying permission — module gate is checked strictly before, and independent of, the permission gate.
- `/` shortcut to focus menu search is suppressed when focus is already in a form control or the sidenav is collapsed and not currently peeking (avoids stealing focus/opening search behind a hidden panel).
- Prefix-based route highlighting is explicitly excluded for `/timetable/capacity-planner` (no nav entry of its own) so it doesn't fall back to highlighting the unrelated `/timetable` "Timetable" calendar screen, which shares the same first path segment.
- `isDevAdmin`/`isSupportAdmin` role checks in the shell are role-name string comparisons (via `normalizedRoleName`), not permission codes — a deliberate, narrow exception mirroring an equivalent exemption already made explicit on the backend (`PermSecurityBean`/`UserPermissionService.isSystemRole`), not a violation of the DB-only-permission-check rule for ordinary feature access.

## 8. Known Gaps / Deferred

- No shell-level automated/unit test files were located for `App` itself beyond the manual test-case documents (`app-shell.md`, `navigation-shell.md`); guard/service-level spec files exist elsewhere (e.g. `week-navigator.component.spec.ts` etc. are unrelated feature specs, not shell specs).
- Sidenav/theme/expanded-group preferences are per-browser only (see BRD.md §6) — not a defect, but worth flagging if cross-device preference sync is ever expected.
