# Application Shell — Manual Test Cases

## TC-SHELL-001: Sidenav layout displays correctly

**Preconditions:**
- User is logged in to the application

**Steps:**
1. Verify the application displays a Material sidenav layout
2. Verify the left side navigation panel is visible with navigation items
3. Verify the top toolbar displays the app title "College Management System"
4. Verify the toolbar has a menu toggle button, theme toggle, and user menu

**Expected Result:**
- Application shell renders with sidenav, toolbar, and main content area

**Status:** NOT TESTED

---

## TC-SHELL-002: Sidenav toggle

**Preconditions:**
- User is logged in

**Steps:**
1. Click the hamburger menu icon in the toolbar
2. Verify the sidenav collapses/hides
3. Click the menu icon again
4. Verify the sidenav expands/shows

**Expected Result:**
- Sidenav toggles open and closed when the menu button is clicked

**Status:** NOT TESTED

---

## TC-SHELL-003: Navigation to Dashboard

**Preconditions:**
- User is logged in

**Steps:**
1. Click "Dashboard" in the sidenav navigation
2. Verify the URL changes to `/dashboard`
3. Verify the dashboard component is displayed with placeholder cards
4. Verify the "Dashboard" nav item is highlighted as active

**Expected Result:**
- Dashboard route loads correctly with placeholder metric cards

**Status:** NOT TESTED

---

## TC-SHELL-004: Dashboard placeholder cards

**Preconditions:**
- User is logged in and on the dashboard page

**Steps:**
1. Verify the dashboard displays a welcome message with the user's name
2. Verify four placeholder cards are displayed: Students, Faculty, Departments, Lab Utilization
3. Verify each card has an icon, title, subtitle, and placeholder value "—"

**Expected Result:**
- Dashboard displays all placeholder metric cards correctly

**Status:** NOT TESTED

---

## TC-SHELL-005: Light/dark theme toggle

**Preconditions:**
- User is logged in

**Steps:**
1. Note the current theme (light or dark)
2. Click the theme toggle button (sun/moon icon) in the toolbar
3. Verify the theme switches (e.g., light → dark)
4. Verify Material components update their colors (toolbar, cards, sidenav)
5. Click the theme toggle button again
6. Verify the theme switches back

**Expected Result:**
- Theme toggle switches between light and dark mode, updating all Material component colors

**Status:** NOT TESTED

---

## TC-SHELL-006: User menu displays username and logout option

**Preconditions:**
- User is logged in as `admin`

**Steps:**
1. Click the account icon in the toolbar
2. Verify a dropdown menu appears
3. Verify the menu shows the username "admin"
4. Verify the menu has a "Logout" option

**Expected Result:**
- User menu displays the current user's name and a logout button

**Status:** NOT TESTED

---

## TC-SHELL-007: Default route redirects to dashboard

**Preconditions:**
- User is logged in

**Steps:**
1. Navigate to `http://localhost:4200/`
2. Verify automatic redirect to `http://localhost:4200/dashboard`
3. Navigate to `http://localhost:4200/non-existent-page`
4. Verify redirect to `http://localhost:4200/dashboard`

**Expected Result:**
- Root path and unknown paths redirect to the dashboard

**Status:** NOT TESTED

---

## TC-SHELL-008: Responsive layout

**Preconditions:**
- User is logged in

**Steps:**
1. Resize the browser window to a narrow width (< 768px)
2. Verify the sidenav can be toggled to save screen space
3. Verify the dashboard cards reflow to a single column layout
4. Resize back to a wide width
5. Verify the layout returns to multi-column

**Expected Result:**
- Layout adapts responsively to different screen sizes

**Status:** NOT TESTED
