# SRS — Authentication & Identity (auth-identity)

**System:** OneCMS / College Management System
**Client:** SKSCON / SKS College Of Nursing
**Built by:** Raster / Raster Images Pvt. Ltd.
**Source milestone:** Release 1, Milestone 1 — R1-M1.1 (Backend Security Configuration), R1-M1.2 (Frontend Authentication). Extended in later releases by BR-24, BR-39, BR-48 (see BRD.md).

---

## 1. Introduction

### 1.1 Purpose
Documents the shipped authentication and authorization subsystem of OneCMS: Keycloak-based identity/SSO, JWT validation and revocation on the Spring Boot backend, and a fully database-driven role/permission (RBAC) system that governs both API access and Angular frontend navigation.

### 1.2 Scope
Covers: backend JWT validation and OAuth2 resource-server configuration; the DB-driven permission-check mechanism (`@perm.has(...)`); token revocation (logout denylist); the Role Management and Permission Tiers admin screens; frontend Keycloak session bootstrap, route guards, and the HTTP auth interceptor. Does not cover individual feature-module permissions themselves (each feature module documents its own permission codes), nor the application shell/navigation chrome (see the `app-shell-navigation` module docs).

### 1.3 References
- `docs/RELEASE_1_MILESTONES.md`, lines 100–128 (R1-M1.1, R1-M1.2)
- `docs/BUSINESS_REQUIREMENTS.md` — BR-24 (DB-Driven RBAC & Identity-Only Keycloak), BR-39 (Permission Model V2), BR-48 (JWT Revoked-Token Tracking)
- `docs/manual-test-cases/security-config.md`, `docs/manual-test-cases/authentication.md`

---

## 2. Overall Description

### 2.1 Product Perspective
Auth-identity is the foundation layer every other OneCMS module depends on. Keycloak is the identity provider (authentication only); it issues JWTs consumed by the Spring Boot resource server. Application-level authorization (who can do what) is entirely separate from Keycloak and lives in PostgreSQL (`app_roles`, `permissions`, `role_permissions`, `app_users`). No Keycloak realm role is ever used to gate application behavior.

### 2.2 User Classes / Actors
- **Any authenticated user** — has exactly one DB role (`app_users.app_role_id`) that resolves to a permission set.
- **DEV_ADMIN / SUPPORT_ADMIN** — immutable, seeded "platform system roles" (`app_roles.is_system_role = true`) guaranteed every permission that exists (via the catalogued DEV_ADMIN/SUPPORT_ADMIN migration sync block) and exempt from feature-module gating.
- **collegeadmin** — the sole non-platform go-live baseline role; scoped to admission-setup masters and the enquiry → admission → fee workflow.
- **Custom/delegated roles** — created via Role Management by any user holding `ROLE_VIEW`, placed one hierarchy level below the creator.

### 2.3 Operating Environment
Angular 18+ SPA (served over HTTPS via Nginx in production) talking to a stateless Spring Boot REST API. Keycloak realm `cms`, clients `cms-frontend` (public/PKCE) and `cms-backend`. PostgreSQL for both identity linkage (`app_users`) and RBAC tables.

### 2.4 Constraints & Assumptions
- CSRF is disabled by design — the API is stateless/Bearer-token only, no cookies.
- Keycloak realm roles/`realm_access.roles` are read by `AuthService` for display purposes only (`_roles` signal) but are **not** used for any access-control decision anywhere in the codebase — `JwtRoleConverter` deliberately grants an authenticated principal zero Spring Security authorities from the token.
- A user must have a matching row in `app_users` (by `keycloak_username`) or `/permissions/my` 404s and the frontend proceeds with zero permissions rather than looping back to Keycloak login (documented race-condition avoidance in `app.config.ts`).
- Token revocation is self-service only; there is no admin-triggered forced logout of another user's session (explicit out-of-scope per BR-48).

---

## 3. Functional Requirements

| ID | Description | Priority | Dependencies |
|----|-------------|----------|---------------|
| FR-AUTH-1 | The backend must validate every non-public request's JWT: signature (JWK set), standard claims (`JwtValidators.createDefault()`), and issuer against an allow-list (`keycloak.allowed-issuers`). | Must | Keycloak realm reachable |
| FR-AUTH-2 | `/health` and `/webhooks/**` must be reachable without authentication; every other endpoint requires a valid, non-revoked JWT. | Must | FR-AUTH-1 |
| FR-AUTH-3 | The backend must reject any JWT whose `jti` appears in the `revoked_tokens` table, even if the token has not naturally expired. | Must | FR-AUTH-6 |
| FR-AUTH-4 | Backend authorization decisions must be made exclusively through DB permission codes (`@perm.has`/`@perm.hasAny`), resolved from the caller's `preferred_username` → `app_users` → `app_roles` → `permissions`. No hardcoded role-name checks are permitted. | Must | — |
| FR-AUTH-5 | `X_MANAGE` permission codes stored on a role must be expanded at read time to also imply `X_VIEW`/`X_CREATE`/`X_EDIT`/`X_DELETE`/`X_EXPORT`, for backward compatibility with screens not yet migrated to granular codes. | Should | FR-AUTH-4 |
| FR-AUTH-6 | A user must be able to revoke their own current access token (`POST /auth/revoke`) on logout; the backend records `jti` + expiry and purges expired revocation rows hourly. | Must | — |
| FR-AUTH-7 | The frontend must bootstrap a Keycloak session on app load (`check-sso`, PKCE S256), redirect unauthenticated users to the Keycloak login page, and attach a valid Bearer token to every outgoing HTTP request. | Must | Keycloak client `cms-frontend` |
| FR-AUTH-8 | The frontend must refresh the access token proactively (within 30s of expiry) on each request; a refresh failure must not itself force a login redirect from inside the HTTP layer (avoids a PKCE `code_verifier` race) — re-authentication is deferred to route navigation. | Must | FR-AUTH-7 |
| FR-AUTH-9 | Route access must be gate-able by permission code(s) (`requiresPermission`) or by maximum hierarchy level (`requiresLevel`); an unauthenticated or unauthorized navigation must redirect to `/dashboard` (or to Keycloak login if unauthenticated). | Must | FR-AUTH-7 |
| FR-AUTH-10 | An authorized user (holding `ROLE_VIEW`) must be able to view all assignable roles, create a new non-reserved role, and view/replace the full permission set and dashboard-widget layout of an editable role, via the Role Management screen and its backing API. | Must | FR-AUTH-4 |
| FR-AUTH-11 | Role editing must enforce no-privilege-escalation: a requester can only grant permissions they themselves hold, only to roles strictly below their own hierarchy level, and only within the permission's delegation tier for their level. `DEVADMIN`/`SUPPORTADMIN` cannot be created via the UI; `DEVADMIN`'s permission set is completely immutable. | Must | FR-AUTH-10 |
| FR-AUTH-12 | A user holding `PERMISSION_TIER_MANAGE` must be able to view every permission's tier, preview which roles/how many users would lose a permission before a tier change, and apply the change — auto-revoking the permission from any role that no longer qualifies. | Should | FR-AUTH-4 |

---

## 4. External Interface Requirements

### 4.1 Screens
- **Role Management** (`/role-management`, permission `ROLE_VIEW`) — list assignable roles, create role, edit role's permission set (grouped by nav section → screen), edit role's dashboard widget layout.
- **Permission Tiers** (`/permission-tiers`, permission `PERMISSION_TIER_MANAGE`) — list all permissions with current tier; change tier with an impact preview.
- No dedicated "Login" screen — authentication is fully delegated to Keycloak's own hosted login page (redirected to, not embedded).

### 4.2 API Endpoints (high level)
| Method & Path | Purpose | Permission |
|---|---|---|
| `GET /health` | Liveness check | Public |
| `POST /auth/revoke` | Revoke caller's current JWT (logout) | Authenticated (self only) |
| `GET /permissions/my` | Caller's role, permission codes, resolved dashboard widgets | Authenticated |
| `GET /permissions/all` | All permissions with tier/screen-label metadata | `ROLE_VIEW` |
| `GET /permissions/delegatable` | Permissions the caller may assign to sub-roles | `ROLE_VIEW` |
| `POST /permissions/tier-impact` | Preview roles/user counts affected by pending tier changes | `PERMISSION_TIER_MANAGE` |
| `PUT /permissions/{id}/tier` | Change a permission's tier (auto-revokes from now-ineligible roles) | `PERMISSION_TIER_MANAGE` |
| `GET /role-management` | List roles assignable by the caller | `ROLE_VIEW` |
| `GET /role-management/{id}` | Role detail incl. permissions | `ROLE_VIEW` |
| `POST /role-management` | Create a role | `ROLE_VIEW` |
| `PUT /role-management/{id}/permissions` | Replace a role's permission set | `ROLE_VIEW` (+ escalation checks) |
| `PUT /role-management/{id}/dashboard-widgets` | Replace a role's default dashboard layout | `ROLE_VIEW` |

### 4.3 Key DB Entities
`app_roles` (name, display_name, hierarchy_level, is_system_role), `permissions` (code, category, tier, screen_label), `role_permissions` (join), `app_users` (keycloak_username, email, app_role_id, plus profile fields), `revoked_tokens` (jti, expires_at, revoked_at).

---

## 5. Non-Functional Requirements

- **Security:** stateless (`SessionCreationPolicy.STATELESS`), Bearer-token only; CORS is explicitly whitelisted by origin; method-level security (`@EnableMethodSecurity`) enforced on every controller via `@perm.has`. Immutable roles (`DEVADMIN`) and tiered delegation prevent privilege escalation through the Role Management UI itself.
- **Auditability:** every role creation, permission-set replacement, dashboard-widget change, and tier change/auto-revoke is written to the audit log (`AuditLogService`) with actor, action, entity, and a human-readable description.
- **Performance:** permission lookups are computed per-request from PostgreSQL (no cache) — `UserPermissionService.evict*` methods are retained as no-ops for caller compatibility but do nothing, since lookups are always fresh.
- **Resilience:** an hourly scheduled job purges expired `revoked_tokens` rows so the denylist does not grow unbounded.

---

## 6. Known Gaps / Not Yet Implemented

- **No admin-forced logout of another user's session** — revocation is strictly self-service (explicit out-of-scope decision, BR-48).
- **Duplicate route definitions:** `role-management` and `permission-tiers` (and `user-management`) are each declared twice, verbatim, in `frontend/src/app/app.routes.ts` (~line 2016 and ~line 2585). Angular's router resolves the first match, so the second block is inert dead code — functionally harmless but should be cleaned up.
- Keycloak realm role display (`AuthService._roles`) is populated from the token but has no consumer found in the codebase beyond the signal itself — effectively unused, consistent with BR-24's "identity only" design intent.
