# Manual Test Cases: Custom Keycloak Login Theme

## Module: Authentication / Login UI

---

## TC-LOGIN-001: Custom login page loads with split-screen layout

**Preconditions:**
- Docker Compose is running (`docker compose up -d`)
- Keycloak container started successfully (check `docker logs cms-keycloak`)
- Browser opened in incognito/private mode

**Steps:**
1. Navigate to the CMS frontend URL (e.g., `https://dev.raster.in:212` or `http://localhost:80`)
2. Observe the page — Keycloak should redirect to the login screen

**Expected Result:**
- Login page shows a **split-screen layout**: left branding panel + right form panel
- Left panel: deep indigo-purple gradient background, `CMS` acronym, "College Management System" label, graduation cap icon, tagline "Empowering education, one campus at a time.", 3 feature pills (Student & Admissions, Faculty & Academics, Fees & Finance)
- Right panel: white background, "Welcome back" heading, username + password fields with icons
- No Keycloak default PatternFly styling is visible (no blue header bar, no Keycloak logo)

**Status:** NOT TESTED

---

## TC-LOGIN-002: Successful sign-in with valid credentials

**Preconditions:**
- Custom login page loads (TC-LOGIN-001 passes)
- A valid user account exists in the `cms` realm (e.g., admin / admin)

**Steps:**
1. On the custom login page, enter a valid username
2. Enter the correct password
3. Click **Sign In**

**Expected Result:**
- Button shows a spinner while authenticating
- User is redirected to the CMS dashboard/home screen
- No errors displayed

**Status:** NOT TESTED

---

## TC-LOGIN-003: Invalid credentials shows inline error

**Preconditions:**
- Custom login page loads

**Steps:**
1. Enter `wronguser` as username
2. Enter `wrongpassword` as password
3. Click **Sign In**

**Expected Result:**
- Error alert banner appears below the CMS logo mark on the right panel
- Alert has a red left-border (error styling) with a message like "Invalid username or password."
- Fields stay populated (username is retained, password is cleared)
- No page crash or raw Keycloak error page

**Status:** NOT TESTED

---

## TC-LOGIN-004: Password show/hide toggle works

**Preconditions:**
- Custom login page loads

**Steps:**
1. Click the password field and type any text
2. Click the eye icon button to the right of the password field
3. Click the eye icon again

**Expected Result:**
- Step 2: password becomes visible (`type="text"`), eye icon changes to a strikethrough eye
- Step 3: password is hidden again (`type="password"`), original eye icon shown
- No JavaScript console errors

**Status:** NOT TESTED

---

## TC-LOGIN-005: "Forgot password?" link navigates to reset page

**Preconditions:**
- Custom login page loads
- Realm has "Forgot Password" feature enabled in Keycloak admin

**Steps:**
1. Click the **Forgot password?** link on the right panel
2. On reset page, click **← Back to sign in**

**Expected Result:**
- Step 1: browser navigates to the custom **Reset Password** page
- Reset page shows the same split-screen layout with left branding panel
- Right panel shows: "Reset password" heading, "Enter your account email…" subtitle, a single email/username input field, a **Submit** button, and a **← Back to sign in** ghost button
- Step 2: browser navigates to a fresh sign-in screen (no stale-cookie loop)

**Status:** NOT TESTED

---

## TC-LOGIN-006: Reset password form submission

**Preconditions:**
- Custom reset password page loads (TC-LOGIN-005 passes)

**Steps:**
1. Enter a registered email/username in the input field
2. Click **Submit**

**Expected Result:**
- Keycloak processes the request and shows the `info.ftl` page with a confirmation message (e.g., "You should receive an email shortly with further instructions.")
- Page shows the custom info card with blue styling (info variant), not the default Keycloak layout

**Status:** NOT TESTED

---

## TC-LOGIN-007: Error page uses custom theme

**Preconditions:**
- Custom theme is active

**Steps:**
1. Manually navigate to a Keycloak URL with an invalid `client_id` parameter, e.g.:
   `http://localhost:8280/realms/cms/protocol/openid-connect/auth?client_id=INVALID&redirect_uri=http://localhost&response_type=code`

**Expected Result:**
- Keycloak renders the custom `error.ftl` page
- Page shows: "Something went wrong" heading, red error card with an `×` icon, the error message text, and a **Back to sign in** button
- Same split-screen layout as the main login page

**Status:** NOT TESTED

---

## TC-LOGIN-008: Mobile responsive layout

**Preconditions:**
- Custom login page loads

**Steps:**
1. Open browser DevTools → toggle device toolbar → select **iPhone 12** (or any ≤ 768px viewport)
2. Reload the login page

**Expected Result:**
- Layout stacks vertically: branding panel on top (compact, no feature pills), form below
- Left panel shows only the icon, acronym `CMS`, and tagline — feature list and footer are hidden
- Form is full-width with comfortable padding
- All form elements remain usable (tap targets ≥ 44px)

**Status:** NOT TESTED

---

## TC-LOGIN-009: Theme persists after container restart

**Preconditions:**
- Docker Compose is running with the themes volume mount

**Steps:**
1. Run `docker compose restart keycloak`
2. Wait for Keycloak to start (check `docker logs -f cms-keycloak`)
3. Navigate to the login page

**Expected Result:**
- Custom `cms` theme is still active after restart
- No fallback to the default Keycloak theme

**Status:** NOT TESTED

---

## TC-LOGIN-010: Realm import activates theme on fresh start

**Preconditions:**
- No existing `cms` realm (fresh environment or volume deleted: `docker compose down -v`)

**Steps:**
1. Run `docker compose up -d`
2. Wait for Keycloak to fully start and import the realm
3. Navigate to the login page

**Expected Result:**
- Realm is imported with `loginTheme: cms` set
- Custom login page is shown immediately without manual admin configuration
- Verify in Keycloak Admin Console (`http://localhost:8280`): Realm Settings → Themes → Login theme shows `cms`

**Status:** NOT TESTED

---

## TC-LOGIN-011: Error/info "Back to sign in" restarts login flow

**Preconditions:**
- Custom theme is active
- Frontend login flow is reachable

**Steps:**
1. Trigger a "cookie not found" auth error by opening an expired or tampered login action URL in a new tab
2. On the error page, click **Back to sign in**
3. Trigger an `info.ftl` screen (e.g., submit forgot-password form), then click **Back to sign in** when shown

**Expected Result:**
- Both clicks navigate to a fresh sign-in screen
- No loop back to the same error screen
- New login attempt can be started normally

**Status:** NOT TESTED
