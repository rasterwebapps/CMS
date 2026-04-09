# Authentication — Manual Test Cases

## TC-AUTH-001: Keycloak login redirect

**Preconditions:**
- Frontend application is running (`ng serve`)
- Keycloak is running on port 8180 with `cms` realm and `cms-frontend` client
- Backend is running on port 8080

**Steps:**
1. Open `http://localhost:4200` in a browser
2. Verify the browser is redirected to the Keycloak login page
3. Verify the Keycloak realm is `cms` and the client is `cms-frontend`

**Expected Result:**
- Unauthenticated users are automatically redirected to Keycloak login

**Status:** NOT TESTED

---

## TC-AUTH-002: Successful login with admin user

**Preconditions:**
- Frontend and Keycloak are running
- Test user `admin` / `admin123` exists with ROLE_ADMIN

**Steps:**
1. Open `http://localhost:4200`
2. Enter username: `admin`, password: `admin123` on the Keycloak login page
3. Verify successful redirect back to the application
4. Verify the dashboard is displayed
5. Verify the username "admin" appears in the toolbar user menu

**Expected Result:**
- User is authenticated and the dashboard is displayed with the correct username

**Status:** NOT TESTED

---

## TC-AUTH-003: Successful login with student user

**Preconditions:**
- Frontend and Keycloak are running
- Test user `student1` / `student123` exists with ROLE_STUDENT

**Steps:**
1. Open `http://localhost:4200`
2. Enter username: `student1`, password: `student123`
3. Verify successful authentication and dashboard display

**Expected Result:**
- Student user can log in and see the dashboard

**Status:** NOT TESTED

---

## TC-AUTH-004: Logout functionality

**Preconditions:**
- User is logged in to the application

**Steps:**
1. Click the user menu icon (account_circle) in the toolbar
2. Click "Logout"
3. Verify the user is redirected to the Keycloak login page
4. Verify navigating back to `http://localhost:4200` requires login again

**Expected Result:**
- User is logged out and session is terminated

**Status:** NOT TESTED

---

## TC-AUTH-005: Bearer token attached to API requests

**Preconditions:**
- User is logged in
- Browser developer tools are open (Network tab)

**Steps:**
1. Navigate to a page that makes an API request
2. Inspect the network request in browser developer tools
3. Verify the request includes an `Authorization: Bearer <token>` header
4. Verify the token is a valid JWT

**Expected Result:**
- All API requests to the backend include a valid Bearer token

**Status:** NOT TESTED

---

## TC-AUTH-006: Auth guard protects routes

**Preconditions:**
- Frontend is running

**Steps:**
1. Clear browser session/cookies
2. Navigate directly to `http://localhost:4200/dashboard`
3. Verify redirection to Keycloak login

**Expected Result:**
- Protected routes redirect unauthenticated users to Keycloak login

**Status:** NOT TESTED

---

## TC-AUTH-007: Token refresh

**Preconditions:**
- User is logged in
- Access token has a short expiry (default Keycloak: 5 minutes)

**Steps:**
1. Log in to the application
2. Wait for the access token to expire (or set Keycloak token lifespan to 1 minute for testing)
3. Trigger an API request
4. Verify the token is refreshed automatically without requiring re-login

**Expected Result:**
- Expired tokens are automatically refreshed using the refresh token

**Status:** NOT TESTED
