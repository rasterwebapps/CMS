# Role-Based Access Control (RBAC) Updates

This document summarizes the role-based access control updates implemented for the College Management System to restrict access based on user roles.

## Updated Roles and Permissions

### 1. College Admin (ROLE_COLLEGE_ADMIN)

**Dashboard:**
- College Admin Dashboard

**Preferences:**
- Departments (Create, Read, Update, Delete)
- Programs (Create, Read, Update, Delete)
- Courses (Create, Read, Update, Delete)
- Academic Years (Create, Read, Update, Delete)
- Semesters (Create, Read, Update, Delete)
- Fee Structures (Create, Read, Update, Delete)
- Faculty (Create, Read, Update, Delete)
- Agents (Create, Read, Update, Delete)
- Referral Types (Create, Read, Update, Delete)

**Admission Management:**
- Enquiries (Read-only)
- Document Submission (Read/Write)
- Admission Completion (Read/Write)
- Admissions (Read/Write)
- Students (Read/Write)

**Finance:**
- Fee Finalization (Create, Read, Update, Delete)
- Fee Payments (Create, Read, Update, Delete)

**Reports:**
- All Reports (Read-only)

### 2. Front Office (ROLE_FRONT_OFFICE)

**Dashboard:**
- Front Office Dashboard

**Admission Management:**
- Enquiries (Read-only)
- Document Submission (Read/Write)
- Admission Completion (Read/Write)
- Admissions (Read/Write)
- Students (Read/Write)

**Reports:**
- All Reports (Read-only)

### 3. Cashier (ROLE_CASHIER)

**Dashboard:**
- Cashier Dashboard

**Admission Management:**
- Enquiries (Read-only)

**Finance:**
- Fee Payments (Create, Read, Update [for existing payments only], Delete [for existing payments only])

**Reports:**
- All Reports (Read-only)

## Backend API Authorization Updates

### Fee Payment Controller (`/fee-payments`)
- **POST** (Create Payment): `ROLE_ADMIN` | `ROLE_COLLEGE_ADMIN` | `ROLE_CASHIER`

### Student Fee Controller (`/student-fees`)
- **POST `/finalize`** (Finalize Student Fees): `ROLE_ADMIN` | `ROLE_COLLEGE_ADMIN`
- **POST `/{studentId}/collect`** (Collect Payment): `ROLE_ADMIN` | `ROLE_COLLEGE_ADMIN` | `ROLE_CASHIER`

### Enquiry Controller (`/enquiries`)
- **GET** (List/Read Enquiries): No role restriction (all authenticated users)
- **GET `/{id}`** (View Enquiry): No role restriction (all authenticated users)
- **POST** (Create Enquiry): `ROLE_ADMIN` | `ROLE_COLLEGE_ADMIN` | `ROLE_FRONT_OFFICE`
- **PUT `/{id}/convert`** (Convert to Student): `ROLE_ADMIN` | `ROLE_COLLEGE_ADMIN` | `ROLE_FRONT_OFFICE`

## Frontend Navigation Menu Updates

Updated navigation menu filtering in `/frontend/src/app/app.ts`:

1. Added `roles` property to `NavGroup` interface to support group-level role restrictions
2. Updated `filteredNavEntries` computed signal to:
   - Check group-level role restrictions before filtering items
   - Ensure groups are hidden if user doesn't have access to any items
3. Added `ROLE_COLLEGE_ADMIN` and `ROLE_CASHIER` to:
   - `CMS_ROLE_NAMES` mapping
   - `primaryRole` priority list
4. Updated navigation items with role restrictions:
   - Dashboard: All authenticated roles
   - Preferences: Admin, College Admin only
   - Admission Management: Admin, College Admin, Front Office, Cashier (read-only for Cashier)
   - Finance: Admin, College Admin, Cashier
   - Curriculum & Academics: Admin only
   - Examinations: Admin only
   - Lab & Infrastructure: Admin only
   - Reports: Admin, College Admin, Front Office, Cashier

## Manual Test Cases

### TC-RBAC-001: College Admin Menu Access

**Preconditions:**
- User is logged in with ROLE_COLLEGE_ADMIN
- Backend is running

**Steps:**
1. Navigate to the application
2. Verify the sidenav navigation menu is displayed
3. Verify the following menu items are visible:
   - Dashboard
   - Preferences (with sub-items: Departments, Programs, Courses, Academic Years, Semesters, Fee Structures, Faculty, Agents, Referral Types)
   - Admission Management (with sub-items: Enquiries, Document Submission, Admission Completion, Admissions, Students)
   - Finance (with sub-items: Student Fees, Fee Payments, Fee Finalization)
   - Reports
4. Verify the following menu items are NOT visible:
   - Curriculum & Academics
   - Examinations
   - Lab & Infrastructure

**Expected Result:**
- College Admin sees only the allowed menu items.
- Unauthorized menu items are hidden.

**Status:** NOT TESTED

---

### TC-RBAC-002: Front Office Menu Access

**Preconditions:**
- User is logged in with ROLE_FRONT_OFFICE
- Backend is running

**Steps:**
1. Navigate to the application
2. Verify the sidenav navigation menu is displayed
3. Verify the following menu items are visible:
   - Dashboard
   - Admission Management (with sub-items: Enquiries, Document Submission, Admission Completion, Admissions, Students)
   - Reports
4. Verify the following menu items are NOT visible:
   - Preferences
   - Finance
   - Curriculum & Academics
   - Examinations
   - Lab & Infrastructure

**Expected Result:**
- Front Office sees only the allowed menu items.
- Unauthorized menu items are hidden.

**Status:** NOT TESTED

---

### TC-RBAC-003: Cashier Menu Access

**Preconditions:**
- User is logged in with ROLE_CASHIER
- Backend is running

**Steps:**
1. Navigate to the application
2. Verify the sidenav navigation menu is displayed
3. Verify the following menu items are visible:
   - Dashboard
   - Admission Management (sub-item: Enquiries)
   - Finance (sub-item: Fee Payments)
   - Reports
4. Verify the following menu items are NOT visible:
   - Preferences
   - Document Submission (under Admission Management)
   - Admission Completion (under Admission Management)
   - Admissions (under Admission Management)
   - Students (under Admission Management)
   - Fee Finalization (under Finance)
   - Curriculum & Academics
   - Examinations
   - Lab & Infrastructure

**Expected Result:**
- Cashier sees only the allowed menu items.
- Unauthorized menu items are hidden.

**Status:** NOT TESTED

---

### TC-RBAC-004: College Admin Fee Payment Access

**Preconditions:**
- User is logged in with ROLE_COLLEGE_ADMIN
- Backend is running
- A student with finalized fees is in the system

**Steps:**
1. Navigate to `/fee-payments`
2. Click "Create New Payment"
3. Fill in the payment form with valid data
4. Click "Submit"
5. Verify the payment is created successfully

**Expected Result:**
- Payment is created successfully with HTTP 201 response.
- Payment appears in the fee payments list.

**Status:** NOT TESTED

---

### TC-RBAC-005: Cashier Fee Payment Access

**Preconditions:**
- User is logged in with ROLE_CASHIER
- Backend is running
- A student with finalized fees is in the system

**Steps:**
1. Navigate to `/fee-payments`
2. Click "Create New Payment"
3. Fill in the payment form with valid data
4. Click "Submit"
5. Verify the payment is created successfully

**Expected Result:**
- Payment is created successfully with HTTP 201 response.
- Payment appears in the fee payments list.

**Status:** NOT TESTED

---

### TC-RBAC-006: Cashier Cannot Access Fee Finalization

**Preconditions:**
- User is logged in with ROLE_CASHIER
- Backend is running

**Steps:**
1. Try to navigate to `/student-fees/finalize`
2. Verify access is denied

**Expected Result:**
- HTTP 403 Forbidden response is returned.
- User is redirected to an access denied page.

**Status:** NOT TESTED

---

### TC-RBAC-007: Enquiry Read-Only Access for Cashier

**Preconditions:**
- User is logged in with ROLE_CASHIER
- Backend is running
- Enquiries exist in the system

**Steps:**
1. Navigate to `/enquiries`
2. Verify the list of enquiries is displayed
3. Click on an enquiry to view details
4. Verify enquiry details are displayed
5. Verify the "Edit" button is NOT visible for Cashier

**Expected Result:**
- Cashier can read enquiries.
- Cashier cannot modify enquiries.
- Edit/Delete buttons are hidden or disabled.

**Status:** NOT TESTED

---

### TC-RBAC-008: Admin Access Unrestricted

**Preconditions:**
- User is logged in with ROLE_ADMIN
- Backend is running

**Steps:**
1. Navigate to the application
2. Verify all menu items are visible:
   - Dashboard
   - Preferences (with all sub-items)
   - Admission Management (with all sub-items)
   - Curriculum & Academics
   - Examinations
   - Finance (with all sub-items)
   - Lab & Infrastructure
   - Reports

**Expected Result:**
- Admin has access to all menu items.
- All sub-items are visible.

**Status:** NOT TESTED

---

## Implementation Files Modified

### Frontend
- `/frontend/src/app/app.ts`:
  - Updated `NavGroup` interface to include optional `roles` property
  - Added `ROLE_COLLEGE_ADMIN` and `ROLE_CASHIER` to `CMS_ROLE_NAMES`
  - Updated `primaryRole` computed signal priority list
  - Updated `filteredNavEntries` computed signal to check group-level role restrictions
  - Updated navigation entries with appropriate role restrictions

### Backend
- `/backend/src/main/java/com/cms/controller/FeePaymentController.java`:
  - Updated POST `/fee-payments` (create) to include `ROLE_CASHIER`

- `/backend/src/main/java/com/cms/controller/StudentFeeController.java`:
  - Updated POST `/finalize` to exclude `ROLE_CASHIER` (College Admin and Admin only)
  - Updated POST `/{studentId}/collect` to include `ROLE_COLLEGE_ADMIN`

## Verification Steps

### Frontend Verification
1. Build: `npm run build` ✓
2. Run dev server: `npm run start`
3. Test navigation menu filtering for each role (see manual test cases)

### Backend Verification
1. Compile: `./gradlew compileJava` ✓
2. Run tests: `./gradlew test`
3. Run application: `./gradlew bootRun`
4. Test API endpoints with different roles using Postman or similar tool

## Notes

- All GET endpoints for read-only access (Enquiries, Reports) have no role restrictions for authenticated users
- Role filtering is applied at both frontend (navigation menu) and backend (API) levels
- Frontend role-based menu filtering provides user experience improvements
- Backend role-based authorization provides security enforcement

