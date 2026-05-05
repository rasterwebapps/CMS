# Role & User Management — Manual Test Cases

Verifies the new database-driven RBAC system: roles, permissions, role-permission grants,
plus the User Management and Role Management screens. Hierarchy enforcement (a user can
only assign roles strictly below their own level) is the core invariant.

**Hierarchy (lower = more powerful):**

| Level | Role | Notes |
|------:|------|-------|
| 1 | `DEV_ADMIN` | System role; cannot be created/assigned via UI |
| 2 | `SUPPORT_ADMIN` | System role; cannot be assigned by anyone except `DEV_ADMIN` |
| 3 | `ADMIN` | Highest client-side role |
| 4 | `COLLEGE_ADMIN` | Day-to-day college operations |
| 5 | `FRONT_OFFICE`, `CASHIER`, `FACULTY`, `LAB_INCHARGE`, `TECHNICIAN` | Operational |
| 6 | `STUDENT`, `PARENT` | Read-only |

---

## TC-RBAC-001: RBAC seed data populates on local profile

**Preconditions:**
- Backend started with default profile (`local`, H2 in-memory)

**Steps:**
1. Start the backend: `cd backend && ./gradlew bootRun`
2. Watch the logs for `RBAC seed: complete — 11 roles, … permissions, … grants.`
3. Open the H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:cmsdb`)
4. Run: `SELECT name, hierarchy_level, is_system_role FROM app_roles ORDER BY hierarchy_level;`

**Expected Result:**
- Log line confirms seed completed with > 0 roles, permissions, and grants
- `app_roles` returns 11 rows: `DEV_ADMIN`(1), `SUPPORT_ADMIN`(2), `ADMIN`(3), `COLLEGE_ADMIN`(4), then `FRONT_OFFICE`/`CASHIER`/`FACULTY`/`LAB_INCHARGE`/`TECHNICIAN`(5), then `STUDENT`/`PARENT`(6)
- `DEV_ADMIN` and `SUPPORT_ADMIN` have `is_system_role = TRUE`

**Status:** NOT TESTED

---

## TC-RBAC-002: Seeder is idempotent (skips on restart)

**Preconditions:**
- TC-RBAC-001 completed (data already seeded in current JVM)

**Steps:**
1. With H2 in-memory the data persists only for the JVM lifetime, so restart the backend
2. After the restart, the seeder runs again from empty
3. Now stop the app, change the `app-roles` count check or run the same `SELECT` again

**Expected Result:**
- On the very first run after JVM start, log shows `RBAC seed: replaying …`
- On profile-aware restarts that preserve the DB (PostgreSQL / `prod`), the log shows `app_roles already populated (N rows) — skipping.`
- No duplicate rows ever appear

**Status:** NOT TESTED

---

## TC-RBAC-003: Permission API returns user's permissions

**Preconditions:**
- Logged in via Keycloak as a user mapped to `ROLE_ADMIN` (which links to the `ADMIN` `AppRole`)

**Steps:**
1. With a valid bearer token, call `GET /api/v1/permissions/my`

**Expected Result:**
- HTTP 200 with body `{ username, roleName: "ADMIN", roleDisplayName: "Admin", hierarchyLevel: 3, permissions: [ … ] }`
- `permissions` array contains all the permission codes seeded for `ADMIN` (e.g., `USER_VIEW`, `ROLE_VIEW`, `ENQUIRY_VIEW`, etc.)

**Status:** NOT TESTED

---

## TC-RBAC-004: User Management screen visible only to ADMIN+

**Preconditions:**
- Two browser sessions: one as `admin`, one as `cashier`

**Steps:**
1. In the admin session, expand the sidebar; under **Administration** there should be **User Management** and **Roles & Permissions**
2. In the cashier session, the **Administration** group must be hidden entirely
3. As cashier, manually navigate to `/user-management` in the URL bar

**Expected Result:**
- Admin: menu group visible, both items reachable
- Cashier: menu group hidden; manual URL navigation either redirects to dashboard or shows "Forbidden" (depending on guard wiring)

**Status:** NOT TESTED

---

## TC-RBAC-005: Hierarchy enforcement — assignable roles dropdown

**Preconditions:**
- Logged in as `ADMIN` (level 3) on the User Management screen

**Steps:**
1. Click **Add User** (or open the create form)
2. Open the **Role** dropdown

**Expected Result:**
- The dropdown lists ONLY roles with hierarchy level **strictly greater than 3**: `COLLEGE_ADMIN`, `FRONT_OFFICE`, `CASHIER`, `FACULTY`, `LAB_INCHARGE`, `TECHNICIAN`, `STUDENT`, `PARENT`
- `DEV_ADMIN`, `SUPPORT_ADMIN`, and `ADMIN` itself must NOT appear

**Status:** NOT TESTED

---

## TC-RBAC-006: SUPPORT_ADMIN cannot create another SUPPORT_ADMIN

**Preconditions:**
- Logged in as a `SUPPORT_ADMIN` user

**Steps:**
1. Open Roles & Permissions screen
2. Open the role assignment dropdown when creating a new user

**Expected Result:**
- `SUPPORT_ADMIN` does NOT appear in the dropdown — the rule "strictly greater than your own level" excludes it
- Only `ADMIN` (level 3) and below are selectable

**Status:** NOT TESTED

---

## TC-RBAC-007: System roles are read-only in Role Management UI

**Preconditions:**
- Logged in as `ADMIN`

**Steps:**
1. Open Roles & Permissions screen
2. Locate `DEV_ADMIN` and `SUPPORT_ADMIN` rows (if visible at all)

**Expected Result:**
- Either: rows are completely hidden from `ADMIN`'s view, OR
- They appear but with no Edit / Delete actions (read-only) — and a `SYSTEM` tag is shown
- Attempting to PUT `/api/v1/role-management/{id}/permissions` with a system role's id returns 403

**Status:** NOT TESTED

---

## TC-RBAC-008: Backend rejects API attempts to escalate privilege

**Preconditions:**
- Logged in as `ADMIN` (level 3) — token in hand

**Steps:**
1. Try to create a user with `roleId` pointing to `SUPPORT_ADMIN`:
   ```
   POST /api/v1/user-management
   { "fullName":"x","email":"x@e.in","keycloakUsername":"x","roleId":<SUPPORT_ADMIN id> }
   ```

**Expected Result:**
- HTTP 403 (or 400 with explanatory message)
- No user row created in `app_users`

**Status:** NOT TESTED

---

## TC-RBAC-009: Permission grant — only permissions you hold can be granted

**Preconditions:**
- Logged in as `ADMIN`

**Steps:**
1. Open Roles & Permissions screen, select an editable role (e.g., `FACULTY`)
2. View the permission matrix
3. Try to grant a permission that `ADMIN` itself does NOT hold (this is rare — `ADMIN` holds all permissions in seed data, so this is mostly a `COLLEGE_ADMIN`-level test)

**Expected Result:**
- The matrix should disable / hide permission codes that the actor lacks
- A direct API call attempting to grant such a permission returns 403

**Status:** NOT TESTED

---

## TC-RBAC-010: Roundtrip — create user, login as them, verify permissions

**Preconditions:**
- Logged in as `ADMIN`; Keycloak admin access also available (to set the new user's password)

**Steps:**
1. Create a new user with role `FRONT_OFFICE` via the User Management screen
2. In Keycloak, set a password for the new user
3. Log out, log back in as the new user
4. Call `GET /api/v1/permissions/my`

**Expected Result:**
- The response shows `roleName: "FRONT_OFFICE"` and the corresponding seeded permission set (admission/enquiry permissions, no master-data manage permissions)
- Sidebar reflects only the screens the user is permitted to view

**Status:** NOT TESTED

