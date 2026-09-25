# BRD — Authentication & Identity (auth-identity)

**System:** OneCMS / College Management System · **Client:** SKSCON / SKS College Of Nursing · **Built by:** Raster / Raster Images Pvt. Ltd.

---

## 1. Executive Summary / Business Objective

OneCMS needs a single, trustworthy gate for "who is this person" (identity) that is decoupled from "what can they do" (authorization), because the college's operational roles (front office, cashier, faculty, college admin, etc.) change far more often, and far more granularly, than anyone should be redeploying the application or touching Keycloak to support. The business decision (BR-24) was to keep Keycloak strictly as an SSO/identity provider and move all authorization into the application's own database, so role and permission changes are a same-day admin-screen operation, never a code or infrastructure change.

## 2. Stakeholders

- **DEV_ADMIN (Raster engineering)** — owns the platform's immutable top-level role; only role whose own permissions cannot be edited by anyone.
- **SUPPORT_ADMIN (Raster support)** — second platform tier; can adjust its own access and delegate.
- **collegeadmin (SKSCON operations)** — the college's actual day-to-day administrative user for admissions/fees.
- **Faculty, Front Office, Cashier, Student, Parent** — downstream role holders whose screens and API access are entirely determined by whatever permissions their role is granted.
- **End users generally** — never interact with Keycloak's own UI; login/logout is presented entirely inside OneCMS (redirect to Keycloak's hosted login page is the one unavoidable exception).

## 3. Business Rules

| ID | Rule | Rationale |
|----|------|-----------|
| BR-AUTH-1 | Application authorization is controlled by database role-permission mappings only; Keycloak realm roles must never grant application access. | Keeps role changes out of infrastructure/deploys; single source of truth. (BR-24) |
| BR-AUTH-2 | Backend access checks use DB permission codes exclusively (`@perm.has`); no hardcoded role-name checks (`ROLE_ADMIN`, etc.) anywhere in controllers/services. | Prevents two parallel, driftable authorization systems. (BR-24) |
| BR-AUTH-3 | Immutable platform roles are limited to `DEV_ADMIN` and `SUPPORT_ADMIN`; they cannot be edited through Role Management. | Guarantees a support escape hatch that can never be accidentally locked out. (BR-24) |
| BR-AUTH-4 | The go-live baseline contains only three DB roles: `DEV_ADMIN`, `SUPPORT_ADMIN`, `collegeadmin`; other operational roles are removed by the go-live wipe process. | Minimizes attack surface / unused role sprawl at launch. (BR-24) |
| BR-AUTH-5 | `collegeadmin` must not see or assign the `DEV_ADMIN`/`SUPPORT_ADMIN` platform roles. | Prevents the client-side operational admin from ever elevating to platform-support privileges. (BR-24) |
| BR-AUTH-6 | Every permission carries a delegation tier; a role can only be granted a permission within the tier its own hierarchy level allows, even by a user who otherwise holds `ROLE_VIEW`. | Stops privilege escalation through the Role Management UI itself. (BR-39) |
| BR-AUTH-7 | A broad legacy `_MANAGE` permission implicitly grants the newer granular `_VIEW/_CREATE/_EDIT/_DELETE/_EXPORT` codes at read time. | Lets granular, operation-wise permissions roll out screen-by-screen without breaking roles still holding the old broad code. (BR-39) |
| BR-AUTH-8 | Logging out must invalidate the bearer token server-side immediately (denylist by `jti`), not just discard it client-side. | Bearer JWTs are otherwise valid until natural expiry even after logout — closes that window. (BR-48) |
| BR-AUTH-9 | Token revocation is always self-service (the caller revokes their own current token only) — there is no admin-forced logout of someone else's session. | Explicit scope decision; a different capability with its own security review, not bundled into logout. (BR-48) |

## 4. Business Process / Workflow

1. A user opens OneCMS; the SPA silently checks for an existing Keycloak session (`check-sso`). If none, the user is redirected to Keycloak's hosted login page (PKCE flow) and authenticates with their Keycloak credentials.
2. On successful login, the SPA loads the user's resolved permission set and dashboard widgets from `GET /permissions/my`, which the backend resolves from `app_users` → `app_roles` → `permissions` (never from the Keycloak token's own claims).
3. Every subsequent API call carries a fresh Bearer token (refreshed proactively if near expiry); the backend validates signature, issuer, and that the token is not on the revocation denylist before evaluating any `@perm.has(...)` check.
4. When the user logs out, the frontend calls `POST /auth/revoke` (denylisting the current token server-side) before ending the Keycloak browser session.
5. A `ROLE_VIEW` holder can create new operational roles and shape their permission set/dashboard through Role Management, always bounded by what they themselves hold and their own hierarchy level — no code change or deployment required for a new role.

## 5. Success Criteria

Not formally defined with a numeric KPI anywhere in project docs — inferred from feature completeness: (a) zero hardcoded role checks in the codebase (BR-24's stated goal, and the standing "Role management is DB-only" mandatory pattern in this project's engineering instructions), (b) a logged-out token is rejected immediately rather than remaining valid until natural expiry, (c) a new operational role can be stood up entirely from the UI.

## 6. Assumptions & Constraints

- Keycloak availability is a hard dependency for login; there is no local/offline authentication fallback.
- A Keycloak-authenticated user with no matching `app_users` row is treated as permission-less, not as an error state requiring re-login (deliberate, to avoid a PKCE redirect-loop bug documented in code comments).
- The three go-live roles (`DEV_ADMIN`, `SUPPORT_ADMIN`, `collegeadmin`) are the only ones assumed present at first deployment; any other role is created post-go-live via Role Management.

## 7. Known Gaps / Deferred

- No self-service "forgot password" or account-recovery flow is documented in this module — that surface, if it exists, is Keycloak's own hosted UI, which per project standing guidance (`never-expose-keycloak-to-users`) should eventually be replaced by an in-app flow; nothing in the current code builds one.
- No admin-triggered forced logout of another user's active session (BR-48, explicit out-of-scope).
