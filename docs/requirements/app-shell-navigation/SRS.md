# SRS — Application Shell & Navigation (app-shell-navigation)

**System:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source milestone:** Release 1, Milestone 1 — R1-M1.3 (Application Shell & Navigation). Substantially extended by a later, undocumented-in-BR "Phase 5" navigation/shell redesign (see `docs/manual-test-cases/navigation-shell.md`) — this SRS documents the shell as it actually ships today, noting which parts are the original M1.3 baseline vs. later additions.

---

## 1. Introduction

### 1.1 Purpose
Documents the Angular application shell — the persistent chrome (sidenav, toolbar, routing frame, theming) every authenticated screen in OneCMS renders inside — and the declarative navigation-menu system that drives it.

### 1.2 Scope
Covers the root `App` component (`app.ts`/`app.html`), `app.routes.ts`'s routing/guard composition, `nav-config.ts` (the single source of truth for menu structure), the dashboard landing page as the shell's default destination, and shell-level cross-cutting concerns (theme, responsive/mobile behavior, keyboard shortcuts, breadcrumbs, global search, notifications). Does not cover the internal business logic of any individual feature screen reached through the shell, nor the DB role/permission engine itself (see `auth-identity`) — only how the shell *consumes* that engine to decide what to show.

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, lines 129–142 (R1-M1.3)
- `docs/manual-test-cases/app-shell.md` (original M1.3 test cases), `docs/manual-test-cases/navigation-shell.md` ("Phase 5" redesign test cases)
- `frontend/src/app/app.ts`, `frontend/src/app/app.routes.ts`, `frontend/src/app/core/nav/nav-config.ts`

---

## 2. Overall Description

### 2.1 Product Perspective
The shell is the outermost authenticated layout every module (admissions, students, fees, library, timetable, inventory, etc.) mounts into via `<router-outlet>`. It owns global chrome only — no feature-specific business logic — and is the single place permission- and module-gating are applied to the *menu* (routes are independently guarded per-route in `app.routes.ts`).

### 2.2 User Classes / Actors
Every authenticated user class in the system (DEV_ADMIN, SUPPORT_ADMIN, collegeadmin, faculty, front office, cashier, student, parent, etc.) sees the same shell component; the menu items and dashboard widgets rendered inside it differ per the caller's DB permissions and role, resolved via `PermissionService`/`ModuleService`.

### 2.3 Operating Environment
Client-side rendered Angular SPA (no SSR — deliberately, per code comment in `app.config.ts`), Angular Material components, served responsively at desktop (≥1024px), tablet (~900px), and mobile (~390px) breakpoints.

### 2.4 Constraints / Assumptions
- The shell only ever renders for an authenticated session — `authGuard` runs before any shell-nested route resolves.
- Persisted UI state (collapsed/pinned sidenav, dark/light theme, expanded nav groups) lives in `localStorage`, keyed `cms_sidenav_collapsed`, `cms_dark_theme`, `cms_nav_expanded_groups` — per-browser, not synced across devices or persisted server-side.
- `nav-config.ts` is also the canonical ordering source for how permissions are grouped on the Role Management / Permission Tiers screens (`menu-order.util.ts`), so navigation structure and permission-admin UI structure are coupled by design, not coincidence.

---

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|----|-------------|----------|---------------|
| FR-SHELL-1 | The shell must render a persistent top toolbar (app title/logo, hamburger toggle, notification bell, theme toggle, user menu) and a side navigation for every authenticated route. | Must | auth-identity |
| FR-SHELL-2 | The side navigation must be declared in a single config (`NAV_ENTRIES`) of flat items and/or labeled groups, each optionally gated by permission code(s) and/or feature-module code(s). | Must | — |
| FR-SHELL-3 | A nav entry (item or whole group) must be hidden if the user holds none of its listed permission codes, or if none of its listed modules are enabled for the deployment — module gate is evaluated first and always wins even if the permission check would pass. | Must | auth-identity, module registry |
| FR-SHELL-4 | DEV_ADMIN/SUPPORT_ADMIN must see every module-gated nav entry regardless of deployment module configuration (mirrors the backend's system-role exemption). | Must | FR-SHELL-3 |
| FR-SHELL-5 | The sidenav must support a collapsed "icon rail" mode (68px) with a hover-triggered "peek" second tray, in addition to a fully pinned-expanded mode; the collapsed/pinned state must persist across sessions. | Should | — |
| FR-SHELL-6 | On mobile viewports, the sidenav must render as an overlay drawer (not push page content) and auto-close on navigation. | Must | FR-SHELL-5 |
| FR-SHELL-7 | The sidenav must auto-expand the group owning the current route and collapse all other groups on navigation, and must persist the expanded-group state across reloads. | Should | — |
| FR-SHELL-8 | The shell must provide an in-menu text search (`/` keyboard shortcut to focus, `Esc` to clear) that filters both groups and items by label. | Should | — |
| FR-SHELL-9 | The shell must provide a light/dark theme toggle, defaulting to the OS `prefers-color-scheme` when no explicit user choice has been persisted. | Must | — |
| FR-SHELL-10 | Unmatched (`''`, `'**'`) route paths must redirect to a role-appropriate landing page — `/student/my-dashboard` for the `student` role, `/dashboard` for everyone else — rather than a single fixed default. | Must | auth-identity |
| FR-SHELL-11 | Every application route must be reachable only via lazy-loaded (`loadComponent`) standalone components, composed through a small set of shared guard combinators (`withAuth`, `withPermission(...codes)`) rather than bespoke per-route guard wiring. | Must | auth-identity |
| FR-SHELL-12 | The shell must surface a badge/count indicator on select admission-workflow nav items (Enquiries, Finalize Fee, Collect Payment, Submit/Verify Documents, Complete Admission) for users holding `ENQUIRY_VIEW`, sourced from a dashboard summary endpoint. | Could | auth-identity |
| FR-SHELL-13 | The shell must expose global keyboard shortcuts (a "g" leader for navigation, a `?` cheat-sheet dialog) and a first-run onboarding tour that can be replayed on demand. | Could | — |

---

## 4. External Interface Requirements

### 4.1 Screens / Chrome Elements
Root shell (`App`/`app.html`): toolbar (logo, hamburger, global search, ward switcher, notification bell, theme toggle, keyboard-shortcuts button, user menu with initials avatar), icon-rail + second-tray sidenav (desktop) / overlay drawer (mobile), breadcrumb bar, toast host, main `<router-outlet>`. Landing page: `DashboardComponent` at `/dashboard` (see §4.3).

### 4.2 API Endpoints (shell-level)
| Method & Path | Purpose |
|---|---|
| `GET /dashboard/summary` | Enquiry-funnel counts used for nav badge counters (only called if caller holds `ENQUIRY_VIEW`) |

(Feature-module APIs reached *through* the shell are documented in their own module FRDs.)

### 4.3 Key Frontend Constructs
`NAV_ENTRIES: NavEntry[]` (`nav-config.ts`) — the sole source of menu structure; `NavItem`/`NavGroup` interfaces carry `label`, `icon`, `route`, optional `permissions[]`, optional `modules[]`. `App.filteredNavEntries` — computed signal applying module+permission+search filtering. `routes: Routes` (`app.routes.ts`) — ~250 lazy-loaded route entries composed via `withAuth`/`withPermission(...)`.

---

## 5. Non-Functional Requirements

- **Responsiveness:** three explicitly tested breakpoints — desktop ≥1024px, tablet ~900px, mobile ~390px (`docs/manual-test-cases/navigation-shell.md`).
- **Consistency:** nav structure doubles as the permission-grouping taxonomy for the Role Management/Permission Tiers screens — a menu reorganization is also, implicitly, a permission-admin UI reorganization.
- **Resilience:** dashboard-summary badge fetch and notification-feed load both silently swallow errors (empty catch) rather than breaking shell render.
- **No SSR:** the app is deployed as a plain static SPA behind Nginx; `provideClientHydration()` is deliberately omitted to avoid hydration/stale-state issues (documented code comment).

---

## 6. Known Gaps / Not Yet Implemented

- The dashboard landing page's original M1.3 scope (four placeholder metric cards + two placeholder widgets, per `docs/manual-test-cases/app-shell.md` TC-SHELL-003) has been superseded in the current codebase by a permission-gated, role/user-configurable widget grid (BR-40) — that system is a distinct, larger feature and is not re-documented here; this SRS covers only the shell frame that hosts it.
- `WardSwitcherComponent` appears in the shell toolbar but its business purpose (ward/hostel context switching) is outside this module's scope and not investigated here.
