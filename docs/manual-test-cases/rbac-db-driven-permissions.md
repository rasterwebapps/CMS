# RBAC – DB-Driven Permissions Manual Test Cases

These test cases cover the four deliverables completed in this RBAC sprint:

1. **V89** – Fills permission gaps for FACULTY, LAB_INCHARGE, TECHNICIAN, FRONT_OFFICE, CASHIER roles.
2. **V90 + AuditLog** – `audit_log` table; every user/role/permission mutation is recorded.
3. **Backend controllers** – All `@PreAuthorize` expressions replaced with `@perm.has('CODE')` (DB-driven).
4. **Frontend nav** – `app.ts` navEntries now use `PermissionService.hasAny()` instead of hardcoded Keycloak roles.

---

## TC-RBAC-001: ADMIN sees all navigation groups

**Preconditions:**
- User logged in with `ROLE_ADMIN` Keycloak role
- User record exists in `app_users` with the `ADMIN` app role
- Application running on local profile

**Steps:**
1. Open the application in a browser.
2. Observe the sidenav.

**Expected Result:**
- All nav groups are visible: Dashboard, Preferences, Admission Management, Curriculum & Academics, Examinations, Finance, Lab & Infrastructure, Reports, Fee Reports, Administration.
- All items within each group are visible.

**Status:** NOT TESTED

---

## TC-RBAC-002: COLLEGE_ADMIN sees limited navigation (no admin-only items)

**Preconditions:**
- User logged in with `ROLE_COLLEGE_ADMIN` Keycloak role
- User record assigned `COLLEGE_ADMIN` app role in `app_users`

**Steps:**
1. Log in as a COLLEGE_ADMIN user.
2. Observe which nav groups and items are visible.

**Expected Result:**
- **Visible**: Preferences (minus Labs, Equipment, Academic Calendar, Settings), Admission Management (minus Roll Number Assignment), Curriculum & Academics, Examinations, Finance, Reports, Fee Reports.
- **Hidden**: Administration group (User Management, Roles & Permissions), Labs, Equipment, Settings.

**Status:** NOT TESTED

---

## TC-RBAC-003: FRONT_OFFICE sees only admission-related navigation

**Preconditions:**
- User logged in with `ROLE_FRONT_OFFICE` Keycloak role
- User record assigned `FRONT_OFFICE` app role

**Steps:**
1. Log in as a FRONT_OFFICE user.
2. Observe the sidenav.

**Expected Result:**
- **Visible**: Dashboard, Admission Management (Enquiries, Submit Documents, Complete Admission, Admissions, Students), Reports.
- **Hidden**: Preferences, Curriculum, Examinations, Finance, Lab & Infrastructure, Administration.

**Status:** NOT TESTED

---

## TC-RBAC-004: CASHIER sees finance and enquiry navigation

**Preconditions:**
- User logged in with `ROLE_CASHIER` Keycloak role
- User record assigned `CASHIER` app role

**Steps:**
1. Log in as a CASHIER user.
2. Observe the sidenav.

**Expected Result:**
- **Visible**: Dashboard, Admission Management (Enquiries only), Finance (Fee Collection only), Fee Reports.
- **Hidden**: Preferences, Curriculum, Examinations, Administration.

**Status:** NOT TESTED

---

## TC-RBAC-005: FACULTY sees curriculum and lab schedule navigation

**Preconditions:**
- User logged in with `ROLE_FACULTY` Keycloak role
- User record assigned `FACULTY` app role (V89 permissions applied)

**Steps:**
1. Log in as a FACULTY user.
2. Observe the sidenav.

**Expected Result:**
- **Visible**: Dashboard, Curriculum & Academics (all items), Examinations, Students (in Admission Management).
- **Hidden**: Preferences, Finance, Administration.

**Status:** NOT TESTED

---

## TC-RBAC-006: LAB_INCHARGE sees inventory and maintenance navigation

**Preconditions:**
- User logged in with `ROLE_LAB_INCHARGE`
- User record assigned `LAB_INCHARGE` app role (V89 permissions applied)

**Steps:**
1. Log in as a LAB_INCHARGE user.
2. Observe the sidenav.

**Expected Result:**
- **Visible**: Dashboard, Lab & Infrastructure (Inventory, Maintenance), Curriculum & Academics (Lab Schedules, Attendance).
- **Hidden**: Preferences, Finance, Administration.

**Status:** NOT TESTED

---

## TC-RBAC-007: Backend endpoint enforces DB-driven permission (not Keycloak role)

**Preconditions:**
- A valid JWT with `ROLE_ADMIN` Keycloak role
- The corresponding `app_users` record has `COLLEGE_ADMIN` app role (mismatch scenario)
- `COLLEGE_ADMIN` does NOT have `DEPT_MANAGE` permission

**Steps:**
1. Send `POST /api/v1/specialities` with a valid JWT (which has Keycloak `ROLE_ADMIN` claim).
2. Observe the response status.

**Expected Result:**
- Response is `403 Forbidden` because the DB-assigned role (`COLLEGE_ADMIN`) does not have `DEPT_MANAGE` permission, regardless of the Keycloak role claim.

**Status:** NOT TESTED

---

## TC-RBAC-008: Audit log records role creation

**Preconditions:**
- User logged in as ADMIN
- H2 console accessible at `http://localhost:8080/h2-console`

**Steps:**
1. Send `POST /role-management` to create a new custom role.
2. Open H2 console and run: `SELECT * FROM audit_log ORDER BY occurred_at DESC LIMIT 5;`

**Expected Result:**
- A new row appears in `audit_log` with:
  - `action = 'ROLE_CREATED'`
  - `entity_type = 'AppRole'`
  - `actor = <admin's username>`
  - `occurred_at` is recent

**Status:** NOT TESTED

---

## TC-RBAC-009: Audit log records user deactivation

**Preconditions:**
- ADMIN user session active
- A target user exists with ID `X`

**Steps:**
1. Send `PUT /user-management/{X}/deactivate` with ADMIN JWT.
2. Query `audit_log` in H2 console.

**Expected Result:**
- Row with `action = 'USER_DEACTIVATED'`, `entity_type = 'AppUser'`, `entity_id = 'X'`, `actor = <admin>`.

**Status:** NOT TESTED

---

## TC-RBAC-010: Audit log records permission assignment

**Preconditions:**
- ADMIN user session active
- A role exists with ID `R`

**Steps:**
1. Send `PUT /role-management/{R}/permissions` with a list of permission codes.
2. Query `audit_log`.

**Expected Result:**
- Row with `action = 'PERMISSIONS_UPDATED'`, `entity_type = 'AppRole'`, `entity_id = 'R'`.

**Status:** NOT TESTED

---

## TC-RBAC-011: PermissionController GET /permissions/my returns correct permissions

**Preconditions:**
- User logged in with `ROLE_COLLEGE_ADMIN` and DB app role = `COLLEGE_ADMIN`

**Steps:**
1. Send `GET /permissions/my` with the user's JWT.

**Expected Result:**
- Response JSON includes `roleName: "COLLEGE_ADMIN"`.
- `permissions` array contains `ENQUIRY_VIEW`, `ADMISSION_VIEW`, `STUDENT_VIEW`, `DEPT_VIEW` etc.
- `permissions` does NOT contain `DEPT_MANAGE`, `USER_VIEW`, `ROLE_VIEW`.

**Status:** NOT TESTED

