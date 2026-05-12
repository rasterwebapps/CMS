# Production Login Permissions Manual Test Cases

## TC-PROD-LOGIN-001: College admin can load permissions after login

**Preconditions:**
- Production application is running on `https://172.16.7.209`
- Keycloak user `collegeadmin` exists and is enabled
- `app_users` contains `collegeadmin` mapped to `COLLEGE_ADMIN`

**Steps:**
1. Open `https://172.16.7.209` in a browser.
2. Log in with username `collegeadmin`.
3. Verify the app returns from Keycloak to the CMS frontend.
4. Open browser developer tools and check the request to `/api/v1/permissions/my`.

**Expected Result:**
- The page does not blink or repeatedly redirect to login.
- `/api/v1/permissions/my` returns `200 OK`.
- Response contains `roleName` as `COLLEGE_ADMIN` and a non-empty `permissions` list.

**Status:** NOT TESTED

## TC-PROD-LOGIN-002: Seeded Keycloak demo users have matching app user rows

**Preconditions:**
- Production database is accessible.
- Keycloak realm `cms` contains seeded demo users.

**Steps:**
1. Verify these Keycloak usernames exist: `collegeadmin`, `frontoffice`, `cashier`, `faculty1`, `labincharge1`, `student1`, `parent1`.
2. Verify each username has a matching `app_users.keycloak_username` row.
3. Verify each matching app user is active and mapped to the expected app role.

**Expected Result:**
- Every seeded Keycloak demo user has a corresponding active `app_users` row.
- Users can call `/api/v1/permissions/my` without a 404 caused by missing app user mapping.

**Status:** NOT TESTED

