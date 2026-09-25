# FRD — Authentication & Identity (auth-identity)

**System:** OneCMS / College Management System · **Client:** SKSCON / SKS College Of Nursing · **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Overview

This module implements: (a) backend JWT/OAuth2-resource-server security, (b) a DB-driven permission engine (`@perm.has`/`@perm.hasAny`), (c) server-side token revocation on logout, and (d) the frontend Keycloak session bootstrap, route guards, and the Role Management / Permission Tiers admin UI that manages the DB role/permission data these checks read.

## 2. Actors & Permissions

Permission codes are free-form strings stored in the `permissions` table and checked verbatim in code/annotations. Codes found specifically in this module:

| Code | Grants | Found in |
|---|---|---|
| `ROLE_VIEW` | View roles — the class-level `@PreAuthorize` on `RoleManagementController` gates every endpoint on the controller (so it's a floor, not the whole story: the three mutating endpoints below layer their own method-level check on top) | `RoleManagementController` class-level `@PreAuthorize`, `PermissionController.getAllPermissions/getDelegatablePermissions` |
| `ROLE_CREATE` | Create a new role (`POST /role-management`) | `RoleManagementController.createRole` `@PreAuthorize`, `role-management.component.ts`'s `canCreate` (gates the Add Role button/empty-state action) |
| `ROLE_EDIT` | Reassign a role's dashboard widget layout (`PUT /role-management/{id}/dashboard-widgets`) | `RoleManagementController.updateDashboardWidgets` `@PreAuthorize`, `role-management.component.ts`'s `canEdit` (gates the Dashboard-widgets button in the permission editor) |
| `PERMISSION_ASSIGN` | Change a role's permission set (`PUT /role-management/{id}/permissions`) — tier 1 (DEV_ADMIN-only to hold/delegate, per V241) | `RoleManagementController.updatePermissions` `@PreAuthorize`, `role-management.component.ts`'s `canAssignPermissions` (gates the permission checkboxes and Save Permissions button) |
| `PERMISSION_TIER_MANAGE` | View/change permission tiers, preview tier-change impact | `PermissionController.previewTierImpact/updateTier`, route `/permission-tiers` |
| `DASHBOARD_CUSTOMIZE` | Save/delete a personal dashboard layout (resolved via `/permissions/my`'s widget config; not an auth-identity screen itself but delivered through the same response) | Referenced in BR-40, surfaced through this module's `/permissions/my` payload |

Two seeded, immutable, hierarchy-level-1/2 platform roles: `DEV_ADMIN` (`is_system_role=true`, hierarchy 1, permission set immutable even to itself) and `SUPPORT_ADMIN` (`is_system_role=true`, hierarchy 2, editable by DEV_ADMIN). Go-live baseline additionally seeds `collegeadmin` (non-system role). `UserPermissionService.isSystemRole()` treats only these two as exempt from feature-module gating (`PermSecurityBean`) regardless of deployment configuration.

**Delegation tiers** (`permissions.tier`, 1–4, enforced by `Permission.tierAllowsLevel`):
- Tier 1 → only hierarchy level ≤ 1 (DEV_ADMIN) may hold/delegate.
- Tier 2 & 3 → hierarchy level ≤ 2 (DEV_ADMIN, SUPPORT_ADMIN).
- Tier 4 → anyone who holds the permission may delegate it further, within their own hierarchy constraints.

## 3. Screens & UI Behavior

### 3.1 Role Management (`/role-management`)
- **List:** roles with `hierarchy_level` strictly greater than the caller's own (`findAssignableRoles`) — a caller never sees roles at or above their own level.
- **Create panel:** `name`, `displayName`, `description`, optional initial permission codes and dashboard widget keys. New role's `hierarchy_level` is always `requesterLevel + 1` (placed one level below its creator) — not user-selectable. Reserved names `DEVADMIN`/`SUPPORTADMIN` (case/underscore-insensitive match) are rejected with 403; a duplicate `name` is rejected with 400 (`IllegalArgumentException`).
- **Edit panel — permissions:** a checkbox tree grouped first by nav section then by screen (`groupPermissionsByNav`/`colorForNavGroup` from `menu-order.util.ts`, itself keyed off `nav-config.ts`'s canonical menu order). Only permissions the caller could see via `/permissions/delegatable` are offered. Checkboxes and the Save Permissions button require `PERMISSION_ASSIGN`; a caller without it sees the matrix read-only (disabled checkboxes, no Save button, "Close" instead of "Cancel").
- **Edit panel — dashboard widgets:** a widget picker (`WidgetPickerComponent`) to set the role's default ordered widget layout (key, column span 1–4, row span 1–2). The Dashboard button that opens this picker requires `ROLE_EDIT`.
- Screen ships an in-app guided tour (`ROLE_MANAGEMENT_TOUR`).
- ~~No `uniqueFieldValidator`/`name-exists` async endpoint was found for role name~~ **Fixed 2026-09-24:** added `GET /role-management/name-exists` (gated by the controller's existing `ROLE_VIEW`) and wired it into the Create panel's `name` field. Since this screen uses plain template-driven forms (`[(ngModel)]`), not Reactive Forms, the check is implemented as an RxJS `Subject`+`debounceTime`+`switchMap` pipeline on `(ngModelChange)` rather than the shared `uniqueFieldValidator`/`AsyncValidatorFn` used on Reactive-Forms screens elsewhere — same debounced UX (a "Checking…" hint, then an inline error), just a different plumbing mechanism suited to this screen's form style. Checks the same uppercase/underscore-transformed value the form actually submits. No excludeId — role names have no rename flow.

### 3.2 Permission Tiers (`/permission-tiers`)
- Lists every permission with its current tier, category, and screen label.
- Changing a tier opens a confirm dialog showing exactly which roles (and how many users each) will lose the permission (`POST /permissions/tier-impact`) before the change is committed (`PUT /permissions/{id}/tier`).

### 3.3 Login / Logout
- No custom login screen exists. `authGuard` calls `AuthService.login()`, which redirects the browser to Keycloak's hosted `/realms/cms/protocol/openid-connect/auth` page.
- Logout (`App.logout()` → `AuthService.logout()`) first `fetch()`s `POST /auth/revoke` with the current token (bypassing the app's `HttpClient`/interceptor by design, to avoid a circular DI dependency), then calls Keycloak's own `logout()`.

## 4. Functional Workflows

### 4.1 App bootstrap / login
1. `provideAppInitializer` runs `AuthService.init()` → Keycloak `check-sso`, PKCE S256.
2. If not authenticated → `login()` redirects to Keycloak; initializer returns early (browser navigates away).
3. If authenticated → `Promise.all([PermissionService.load(), ModuleService.load()])` fetches `/permissions/my` and the enabled-modules list in parallel.
4. If `/permissions/my` errors (e.g. no `app_users` row, backend unreachable), the app still renders with zero permissions rather than re-redirecting to Keycloak — route/permission guards then naturally block everything protected.

### 4.2 Per-request token attach + refresh
1. `authInterceptor` calls `AuthService.getValidToken()` before every HTTP request.
2. `getValidToken()` calls Keycloak's `updateToken(30)` (refresh if <30s to expiry); on success the request is cloned with `Authorization: Bearer <token>`; on failure the request proceeds token-less and the `authenticated` signal is set false (re-auth deferred to the next route navigation via `authGuard`, deliberately not triggered inline here).

### 4.3 Backend permission check
1. `@PreAuthorize("@perm.has('CODE')")` (or `hasAny`) on a controller method invokes `PermSecurityBean`.
2. Resolves `preferred_username` from the JWT principal → `UserPermissionService.getPermissions(username)` → `app_users.app_role_id` → `role.getPermissions()` → set of codes, with any `_MANAGE` code expanded to its granular siblings.
3. Each candidate code is additionally checked against `ModuleRegistry`/`ModuleConfig`; if the resolving module is disabled for this deployment (and the caller isn't a system role), a `ModuleNotEnabledException` (403, `MODULE_NOT_ENABLED`) is thrown instead of a plain deny — lets the frontend distinguish "not licensed" from "not permitted."

### 4.4 Logout / revocation
1. Frontend `fetch()`s `POST /auth/revoke` with the current Bearer token.
2. `AuthController.revokeToken` reads `jti`/`expiresAt` straight off the authenticated principal (never from a request body) and calls `TokenRevocationService.revoke` (idempotent).
3. Every subsequent request bearing that `jti` fails `RevocationJwtValidator`, chained into the JWT decoder alongside the standard/issuer validators, with `invalid_token`.
4. An hourly `@Scheduled` job purges `revoked_tokens` rows whose `expires_at` has already passed.

### 4.5 Role permission update (privilege-escalation guard)
1. `PUT /role-management/{id}/permissions` receives the target role id and the full desired permission-code list.
2. `ensureRoleIsEditable`: rejects if the role is `DEVADMIN` (truly immutable) or if `role.hierarchyLevel <= requesterLevel` (can't edit peers/above).
3. Codes newly being added (not already on the role) are diffed out; each must (a) be held by the requester themselves, and (b) satisfy `Permission.tierAllowsLevel(tier, requesterLevel)` — both checked, both 403 on failure.
4. Codes already present on the role pass through even if they'd now fail either check (e.g. a permission's tier was tightened after the grant) — prevents being unable to save unrelated edits.
5. On success: role's permission set is fully replaced, an audit log entry is recorded, `evictAll()` is called (currently a no-op, lookups are always live).

## 5. API Endpoints

See SRS.md §4.2 for the consolidated table; request/response detail:
- `POST /auth/revoke` — no body; uses the authenticated `JwtAuthenticationToken`'s own `jti`/expiry. Returns `200` with empty body.
- `GET /permissions/my` — returns `MyPermissionsResponse(username, roleName, roleDisplayName, hierarchyLevel, permissionCodes[], widgetConfigs[])`.
- `GET /permissions/all` / `GET /permissions/delegatable` — return `List<PermissionDetail(id, code, displayName, category, tier, screenLabel)>`.
- `POST /permissions/tier-impact` — body `List<TierChangeItem(id, tier)>`; returns `List<TierImpactEntry(permissionId, code, displayName, currentTier, newTier, revokedFrom: List<ImpactedRole(roleId, roleName, roleDisplayName, userCount)>)>`, omitting entries with zero impact.
- `PUT /permissions/{id}/tier` — body `{tier: int}` (1–4, else 400); returns updated `PermissionDetail`.
- `POST /role-management` — body `AppRoleRequest(name, displayName, description, permissionCodes[], dashboardWidgets[])`; 201 + `AppRoleResponse`.
- `PUT /role-management/{id}/permissions` — body `List<String>` permission codes; returns `AppRoleResponse`.
- `PUT /role-management/{id}/dashboard-widgets` — body `List<WidgetConfigDto>`; returns `AppRoleResponse`.

## 6. Data Model

- **`app_roles`**(id, name UNIQUE, display_name, hierarchy_level, is_system_role, description, created_at, updated_at) — 1:N `role_dashboard_widget_configs`, M:N `permissions` via `role_permissions`.
- **`permissions`**(id, code UNIQUE, display_name, category, description, screen_label, tier DEFAULT 4, created_at).
- **`role_permissions`**(role_id, permission_id) — composite PK, `ON DELETE CASCADE` both sides.
- **`app_users`**(id, keycloak_username UNIQUE, keycloak_user_id, email UNIQUE, full_name, app_role_id FK, is_active, created_by, created_at, updated_at, plus profile fields and optional `linked_student`/`linked_faculty`/`linked_guardian` 1:1 FKs).
- **`revoked_tokens`**(jti UNIQUE, expires_at, revoked_at) — indexed on `jti` and `expires_at` (V243).

## 7. Edge Cases & Validation Rules

- Creating a role named `DevAdmin`, `dev_admin`, etc. (any case/underscore variant of `DEVADMIN`/`SUPPORTADMIN`) is rejected — normalization strips `_` and upper-cases before comparing.
- Editing `DEVADMIN`'s permissions or dashboard is rejected for every caller, including another DEV_ADMIN-level user.
- Editing a role at or above the caller's own `hierarchy_level` is rejected (peers cannot edit peers; nobody edits upward).
- Granting a permission the caller doesn't themselves hold is rejected, even if the caller's tier would otherwise allow it.
- A tier change that would strand a role at a hierarchy level the new tier disallows auto-revokes that permission from the role immediately (not lazily on next edit) and logs one `PERMISSION_AUTO_REVOKED` audit entry per affected role.
- A revoked token is rejected on every request (not just a "logout" endpoint) for the remainder of its natural lifetime; revocation itself is idempotent (revoking twice is a no-op).
- `preferred_username` missing/blank on the JWT → treated as unauthenticated for permission purposes (`PermSecurityBean.currentUsername()` returns null → every check fails closed).

## 8. Known Gaps / Deferred

- `role-management` and `permission-tiers` routes are each duplicated verbatim in `app.routes.ts` — dead code, not a functional defect (see SRS.md §6).
- No forced/admin logout of another user's session anywhere in the codebase.
- ~~`RoleManagementController`'s `createRole`/`updatePermissions`/`updateDashboardWidgets` are only gated by the class-level `ROLE_VIEW`~~ **Fixed 2026-09-25:** `createRole`/`updateDashboardWidgets`/`updatePermissions` now require `ROLE_CREATE`/`ROLE_EDIT`/`PERMISSION_ASSIGN` respectively — see §2's permission table and `docs/requirements/FINDINGS.md`.
