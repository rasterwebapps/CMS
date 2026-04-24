# Manual Test Cases — Role-Based Access Control

Four operational roles have been defined and provisioned in this system.

## User Credentials

| Role | Username | Password | Description |
|------|----------|----------|-------------|
| Developer Admin | `devadmin` | `DevAdmin@123` | Full access to all screens and features |
| Developer Admin (legacy) | `admin` | `admin123` | Full access to all screens and features |
| College Admin | `collegeadmin` | `College@123` | Academic setup, admissions, finance, reports |
| Front Office | `frontoffice` | `FrontOffice@123` | Admission management screens + Front Office Dashboard |
| Cashier | `cashier` | `Cashier@123` | Finance screens + Cashier Dashboard |

---

## TC-RBAC-001: Developer Admin — Full Access

**Preconditions:**
- Application is running
- Keycloak is running with the `cms` realm loaded

**Steps:**
1. Log in as `devadmin` / `DevAdmin@123`
2. Verify the sidebar shows all menu groups: Preferences, Admission Management, Curriculum & Academics, Examinations, Finance, Lab & Infrastructure, Reports
3. Verify all Preferences sub-items are visible including Settings, Labs, Equipment, Academic Calendar
4. Verify Dashboard shows the Admin Dashboard
5. Navigate to `/departments` — verify access
6. Navigate to `/reports` — verify access
7. Navigate to `/labs` — verify access

**Expected Result:**
- All navigation items are visible
- All screens are accessible
- Dashboard shows Admin Dashboard with full KPI and trend data

**Status:** NOT TESTED

---

## TC-RBAC-002: College Admin — Academic + Admissions + Finance Access

**Preconditions:**
- Application is running
- Keycloak is running with the `cms` realm loaded

**Steps:**
1. Log in as `collegeadmin` / `College@123`
2. Verify the sidebar shows: Preferences, Admission Management, Finance, Reports
3. Verify Preferences shows: Departments, Programs, Courses, Academic Years, Semesters, Fee Structures, Faculty, Agents, Referral Types
4. Verify Preferences does NOT show: Settings, Labs, Equipment, Academic Calendar
5. Verify Admission Management shows all items: Enquiries, Document Submission, Admission Completion, Admissions, Students, Roll Number Assignment
6. Verify Finance shows: Student Fees, Fee Payments, Fee Finalization
7. Verify Curriculum & Academics is NOT visible
8. Verify Examinations is NOT visible
9. Verify Lab & Infrastructure is NOT visible
10. Verify Dashboard shows Admin Dashboard (same as Developer Admin)
11. Navigate to `/reports` — verify access

**Expected Result:**
- Only the allowed navigation items are visible
- Dashboard shows Admin Dashboard panel
- All permitted screens are accessible without 403 errors

**Status:** NOT TESTED

---

## TC-RBAC-003: College Admin — Backend Write Access

**Preconditions:**
- Application is running with backend
- Valid JWT token for `collegeadmin` obtained from Keycloak

**Steps:**
1. Send `POST /api/v1/departments` with College Admin JWT — verify `201 Created`
2. Send `PUT /api/v1/departments/{id}` with College Admin JWT — verify `200 OK`
3. Send `POST /api/v1/programs` with College Admin JWT — verify `201 Created`
4. Send `POST /api/v1/faculty` with College Admin JWT — verify `201 Created`
5. Send `POST /api/v1/agents` with College Admin JWT — verify `201 Created`
6. Send `POST /api/v1/enquiries` with College Admin JWT — verify `201 Created`
7. Send `POST /api/v1/admissions` with College Admin JWT — verify `201 Created`
8. Send `POST /api/v1/fee-payments` with College Admin JWT — verify `201 Created`
9. Send `GET /api/v1/reports/lab-utilization` with College Admin JWT — verify `200 OK`
10. Send `POST /api/v1/settings` with College Admin JWT — verify `403 Forbidden` (Settings is Admin-only)

**Expected Result:**
- All permitted write operations return success status codes
- Settings endpoint correctly returns 403 for College Admin

**Status:** NOT TESTED

---

## TC-RBAC-004: Front Office — Admission Management Access

**Preconditions:**
- Application is running
- Keycloak is running with the `cms` realm loaded

**Steps:**
1. Log in as `frontoffice` / `FrontOffice@123`
2. Verify the sidebar shows ONLY: Dashboard, Admission Management, Fee Payments (under Finance)
3. Verify Admission Management shows: Enquiries, Document Submission, Admission Completion, Admissions, Students
4. Verify Admission Management does NOT show: Roll Number Assignment
5. Verify Preferences is NOT visible
6. Verify Curriculum & Academics is NOT visible
7. Verify Examinations is NOT visible
8. Verify Lab & Infrastructure is NOT visible
9. Verify Reports is NOT visible
10. Verify Dashboard shows Front Office Dashboard

**Expected Result:**
- Only Admission Management items, Fee Payments, and Dashboard are in the sidebar
- Dashboard shows the Front Office Dashboard with enquiry KPIs and funnel

**Status:** NOT TESTED

---

## TC-RBAC-005: Front Office — Backend Write Access

**Preconditions:**
- Valid JWT token for `frontoffice` obtained from Keycloak

**Steps:**
1. Send `POST /api/v1/enquiries` with Front Office JWT — verify `201 Created`
2. Send `PUT /api/v1/enquiries/{id}` with Front Office JWT — verify `200 OK`
3. Send `POST /api/v1/admissions` with Front Office JWT — verify `201 Created`
4. Send `GET /api/v1/dashboard/front-office` with Front Office JWT — verify `200 OK`
5. Send `POST /api/v1/departments` with Front Office JWT — verify `403 Forbidden`
6. Send `GET /api/v1/reports/lab-utilization` with Front Office JWT — verify `403 Forbidden`

**Expected Result:**
- Admission management write endpoints accept Front Office JWT
- Restricted endpoints return 403

**Status:** NOT TESTED

---

## TC-RBAC-006: Cashier — Finance Access

**Preconditions:**
- Application is running
- Keycloak is running with the `cms` realm loaded

**Steps:**
1. Log in as `cashier` / `Cashier@123`
2. Verify the sidebar shows ONLY: Dashboard, Finance
3. Verify Finance shows: Student Fees, Fee Payments, Fee Finalization
4. Verify Preferences is NOT visible
5. Verify Admission Management is NOT visible
6. Verify Curriculum & Academics is NOT visible
7. Verify Reports is NOT visible
8. Verify Dashboard shows Cashier Dashboard with finance KPIs

**Expected Result:**
- Only Finance group and Dashboard are visible in the sidebar
- Dashboard shows Cashier Dashboard with fee collected, outstanding, and quick links

**Status:** NOT TESTED

---

## TC-RBAC-007: Cashier — Backend Write Access

**Preconditions:**
- Valid JWT token for `cashier` obtained from Keycloak

**Steps:**
1. Send `POST /api/v1/student-fees/{studentId}/collect` with Cashier JWT — verify `201 Created`
2. Send `POST /api/v1/student-fees/finalize` with Cashier JWT — verify `201 Created`
3. Send `GET /api/v1/dashboard/summary` with Cashier JWT — verify `200 OK`
4. Send `POST /api/v1/departments` with Cashier JWT — verify `403 Forbidden`
5. Send `POST /api/v1/admissions` with Cashier JWT — verify `403 Forbidden`
6. Send `GET /api/v1/dashboard/front-office` with Cashier JWT — verify `403 Forbidden`

**Expected Result:**
- Finance endpoints accept Cashier JWT
- Non-finance endpoints correctly return 403

**Status:** NOT TESTED

---

## TC-RBAC-008: Cashier Dashboard KPI Cards

**Preconditions:**
- Logged in as `cashier` / `Cashier@123`
- Backend is running with seed data

**Steps:**
1. Navigate to `/dashboard`
2. Verify the Cashier Dashboard panel is shown
3. Verify 4 KPI cards are displayed: Fee Collected (This month), Fee Outstanding, Total Payments, Total Students
4. Verify Finance Quick Links section shows 4 links: Student Fee Explorer, Fee Payments History, Fee Finalization, Fee Structures
5. Click "Student Fee Explorer" — verify navigation to `/student-fees`
6. Click "Collect Payment" CTA — verify navigation to `/fee-payments/new`

**Expected Result:**
- Cashier Dashboard loads with correct KPI data
- All quick links navigate to the correct pages

**Status:** NOT TESTED

---

## TC-RBAC-009: Dashboard Role Tabs for Multi-Role Users

**Preconditions:**
- A user in Keycloak has been assigned both `ROLE_ADMIN` and `ROLE_COLLEGE_ADMIN`

**Steps:**
1. Log in as that user
2. Navigate to `/dashboard`
3. Verify two tabs appear: "Developer Admin" and "College Admin"
4. Click "College Admin" tab
5. Verify Admin Dashboard is shown (both ROLE_ADMIN and ROLE_COLLEGE_ADMIN share the same dashboard component)

**Expected Result:**
- Both tabs are shown for multi-role users
- Both tabs render the Admin Dashboard component

**Status:** NOT TESTED

---

## TC-RBAC-010: Keycloak Realm Role Verification

**Preconditions:**
- Keycloak is running with `cms` realm imported from `cms-realm.json`

**Steps:**
1. Log into Keycloak Admin Console at `http://localhost:8180`
2. Navigate to **Realm: cms → Realm roles**
3. Verify these roles exist: `ROLE_ADMIN`, `ROLE_COLLEGE_ADMIN`, `ROLE_FRONT_OFFICE`, `ROLE_CASHIER`, `ROLE_FACULTY`, `ROLE_STUDENT`, `ROLE_LAB_INCHARGE`, `ROLE_TECHNICIAN`, `ROLE_PARENT`
4. Navigate to **Users** and verify these users exist: `devadmin`, `admin`, `collegeadmin`, `frontoffice`, `cashier`
5. Check each user's role assignments match the expected role

**Expected Result:**
- All 9 roles exist in the realm
- All 5 configured users exist with correct role assignments

**Status:** NOT TESTED

